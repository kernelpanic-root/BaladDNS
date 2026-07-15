package com.kernelpanic.baladdns.viewmodel

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kernelpanic.baladdns.BuildConfig
import com.kernelpanic.baladdns.IPrivilegedService
import com.kernelpanic.baladdns.PrivilegedService
import com.kernelpanic.baladdns.data.DnsRepository
import com.kernelpanic.baladdns.data.activation.ActivationRepositories
import com.kernelpanic.baladdns.data.activation.PermissionState
import com.kernelpanic.baladdns.data.nextdns.api.NextDnsProfile
import com.kernelpanic.baladdns.data.nextdns.profile.NextDnsProfileRepository
import com.kernelpanic.baladdns.data.provider.DnsProviderCatalog
import com.kernelpanic.baladdns.data.provider.DnsProviderSelection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

sealed interface PermissionAcquisitionState {
    data object Idle : PermissionAcquisitionState
    data object WaitingForAdb : PermissionAcquisitionState
    data object RequestingShizuku : PermissionAcquisitionState
    data object Granting : PermissionAcquisitionState
    data object Granted : PermissionAcquisitionState
    data object ShizukuUnavailable : PermissionAcquisitionState
    data object Denied : PermissionAcquisitionState
    data class Error(val message: String?) : PermissionAcquisitionState
}

class OnboardingViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val catalog = DnsProviderCatalog.default
    private val activationRepository = ActivationRepositories.getInstance(application)
    private val dnsRepository = DnsRepository(application)
    private val profileRepository = NextDnsProfileRepository(application)
    private val flow = OnboardingFlow(
        catalog = catalog,
        initialState = restoreState(savedStateHandle).let { restored ->
            if (
                restored == OnboardingFlowState() &&
                activationRepository.state.value.needsModeChoice &&
                dnsRepository.currentSelection() is DnsProviderSelection.Enhanced
            ) {
                OnboardingFlowState(
                    step = OnboardingStep.ActivationMode,
                    draft = OnboardingDraft(
                        providerSelection = dnsRepository.currentSelection(),
                        nextDnsProfileId = profileRepository.currentProfileId(),
                    ),
                )
            } else {
                restored
            }
        },
    )

    val state = flow.state
    private val _permissionState = MutableStateFlow<PermissionAcquisitionState>(
        PermissionAcquisitionState.Idle
    )
    val permissionState = _permissionState.asStateFlow()
    private val _completion = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completion = _completion.asSharedFlow()

    private var permissionCheckJob: Job? = null
    private var listenerRegistered = false
    private var userServiceArgs: Shizuku.UserServiceArgs? = null
    private var serviceBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            serviceBound = true
            _permissionState.value = PermissionAcquisitionState.Granting
            val service = IPrivilegedService.Stub.asInterface(binder)
            try {
                service.grantWriteSecureSettings(getApplication<Application>().packageName)
                unbindPrivilegedService()
                startPermissionCheck()
            } catch (error: Exception) {
                _permissionState.value = PermissionAcquisitionState.Error(error.message)
                unbindPrivilegedService()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST) {
                unregisterPermissionListener()
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    bindPrivilegedService()
                } else {
                    _permissionState.value = PermissionAcquisitionState.Denied
                }
            }
        }

    fun dispatch(intent: OnboardingIntent) {
        if (intent == OnboardingIntent.Back) stopPermissionCheck()
        flow.dispatch(intent)
        persistState()
    }

    fun selectNextDnsProfile(profile: NextDnsProfile) {
        dispatch(OnboardingIntent.ProviderLoginCompleted(profile.id))
    }

    fun startPermissionCheck() {
        if (permissionCheckJob?.isActive == true) return
        _permissionState.value = PermissionAcquisitionState.WaitingForAdb
        permissionCheckJob = viewModelScope.launch {
            while (true) {
                activationRepository.refreshPermission()
                if (activationRepository.state.value.permission == PermissionState.Granted) {
                    _permissionState.value = PermissionAcquisitionState.Granted
                    flow.dispatch(OnboardingIntent.ActivationGranted)
                    persistState()
                    return@launch
                }
                delay(PERMISSION_POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPermissionCheck() {
        permissionCheckJob?.cancel()
        permissionCheckJob = null
        if (_permissionState.value == PermissionAcquisitionState.WaitingForAdb) {
            _permissionState.value = PermissionAcquisitionState.Idle
        }
    }

    fun stopPermissionAcquisition() {
        stopPermissionCheck()
        unregisterPermissionListener()
        unbindPrivilegedService()
    }

    fun requestShizukuActivation() {
        if (_permissionState.value == PermissionAcquisitionState.RequestingShizuku) return
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            _permissionState.value = PermissionAcquisitionState.ShizukuUnavailable
            return
        }
        _permissionState.value = PermissionAcquisitionState.RequestingShizuku
        val permission = runCatching { Shizuku.checkSelfPermission() }.getOrElse {
            _permissionState.value = PermissionAcquisitionState.ShizukuUnavailable
            return
        }
        when {
            permission == PackageManager.PERMISSION_GRANTED -> bindPrivilegedService()
            runCatching { Shizuku.shouldShowRequestPermissionRationale() }
                .getOrDefault(false) -> _permissionState.value = PermissionAcquisitionState.Denied

            else -> {
                registerPermissionListener()
                runCatching { Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST) }
                    .onFailure {
                        unregisterPermissionListener()
                        _permissionState.value = PermissionAcquisitionState.ShizukuUnavailable
                    }
            }
        }
    }

    fun finish() {
        val draft = state.value.draft
        val mode = draft.mode ?: return
        val selection = draft.providerSelection ?: return
        if (
            mode == com.kernelpanic.baladdns.data.activation.ActivationMode.PrivilegedDnsControl &&
            activationRepository.state.value.permission != PermissionState.Granted
        ) {
            return
        }
        viewModelScope.launch {
            when (selection) {
                is DnsProviderSelection.Enhanced -> {
                    val profileId = draft.nextDnsProfileId ?: return@launch
                    profileRepository.selectProfileId(profileId)
                }

                is DnsProviderSelection.Standard,
                is DnsProviderSelection.Custom,
                -> dnsRepository.stageSelection(selection)
            }
            activationRepository.completeOnboarding(mode)
            clearSavedState()
            _completion.emit(Unit)
        }
    }

    private fun bindPrivilegedService() {
        if (serviceBound) return
        val context = getApplication<Application>()
        val componentName = ComponentName(context.packageName, PrivilegedService::class.java.name)
        val versionCode = runCatching {
            PackageInfoCompat.getLongVersionCode(
                context.packageManager.getPackageInfo(context.packageName, 0),
            )
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        }.getOrDefault(1)
        val args = Shizuku.UserServiceArgs(componentName)
            .processNameSuffix("service")
            .debuggable(BuildConfig.DEBUG)
            .daemon(false)
            .version(versionCode)
        userServiceArgs = args
        runCatching { Shizuku.bindUserService(args, connection) }
            .onFailure {
                _permissionState.value = PermissionAcquisitionState.ShizukuUnavailable
            }
    }

    private fun unbindPrivilegedService() {
        val args = userServiceArgs ?: return
        runCatching { Shizuku.unbindUserService(args, connection, true) }
        serviceBound = false
        userServiceArgs = null
    }

    private fun registerPermissionListener() {
        if (listenerRegistered) return
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        listenerRegistered = true
    }

    private fun unregisterPermissionListener() {
        if (!listenerRegistered) return
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        listenerRegistered = false
    }

    private fun persistState() {
        OnboardingStateCodec.encode(state.value).forEach { (key, value) ->
            savedStateHandle[key] = value
        }
    }

    private fun clearSavedState() {
        OnboardingStateCodec.encode(OnboardingFlowState()).keys.forEach { key ->
            savedStateHandle.remove<String>(key)
        }
    }

    override fun onCleared() {
        stopPermissionAcquisition()
        super.onCleared()
    }

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST = 1
        private const val PERMISSION_POLL_INTERVAL_MS = 1_000L

        private fun restoreState(handle: SavedStateHandle): OnboardingFlowState {
            val keys = OnboardingStateCodec.encode(OnboardingFlowState()).keys
            val values = keys.associateWith { key -> handle.get<String>(key) }
            return OnboardingStateCodec.decode(values)
        }
    }
}
