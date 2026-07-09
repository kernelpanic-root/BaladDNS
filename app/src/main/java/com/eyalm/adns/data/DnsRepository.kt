package com.eyalm.adns.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.TileService
import android.util.Log
import com.eyalm.adns.MainActivity
import com.eyalm.adns.R
import com.eyalm.adns.data.dns.AndroidPrivateDnsSettings
import com.eyalm.adns.data.dns.DnsConfigurationRepository
import com.eyalm.adns.data.dns.DnsConfigurationResult
import com.eyalm.adns.data.dns.DnsDisableBehaviorRepositories
import com.eyalm.adns.data.dns.DnsWriteResult
import com.eyalm.adns.data.dns.PrivateDnsController
import com.eyalm.adns.data.activation.ActivationRepositories
import com.eyalm.adns.data.provider.DnsProviderSelection
import com.eyalm.adns.data.provider.DnsProviderCatalog
import com.eyalm.adns.data.provider.ProviderSelectionRepositories
import com.eyalm.adns.data.provider.ProviderSelectionUpdateResult
import com.eyalm.adns.services.AdnsTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DnsRepository(rawContext: Context) {
    private val context: Context = com.eyalm.adns.data.LocaleHelper.onAttach(rawContext)
    private val resolver = context.contentResolver
    private val sharedPrefs = context.getSharedPreferences("adns_settings", Context.MODE_PRIVATE)
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notificationManager = NotificationsManager(context)
    private val activationRepository = ActivationRepositories.getInstance(context)
    private val providerSelectionRepository =
        ProviderSelectionRepositories.getInstance(context)
    private val privateDnsController = PrivateDnsController(
        AndroidPrivateDnsSettings(resolver)
    )
    private val disableBehaviorRepository =
        DnsDisableBehaviorRepositories.getInstance(context)
    private val configurationRepository = DnsConfigurationRepository(
        selectionRepository = providerSelectionRepository,
        privateDnsControl = privateDnsController,
        disableBehavior = { disableBehaviorRepository.behavior.value },
    )

    init {
        // DnsNotificationManager initializes the channel in its constructor
    }

    fun isAdBlockingActive(): Boolean {
        return isSelectedPrivateDnsActive(readPrivateDnsObservation(), getDnsUrl())
    }

    private fun observePrivateDns(): Flow<PrivateDnsObservation> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(readPrivateDnsObservation())
            }
        }

        resolver.registerContentObserver(
            Settings.Global.getUriFor(AndroidPrivateDnsSettings.MODE_KEY),
            false,
            observer,
        )
        resolver.registerContentObserver(
            Settings.Global.getUriFor(AndroidPrivateDnsSettings.SPECIFIER_KEY),
            false,
            observer,
        )

        trySend(readPrivateDnsObservation())

        awaitClose {
            resolver.unregisterContentObserver(observer)
        }

    }.distinctUntilChanged()

    fun getDnsStatusFlow(): Flow<Boolean> = combine(
        observePrivateDns(),
        getDnsUrlFlow(),
    ) { observation, selectedHostname ->
        isSelectedPrivateDnsActive(observation, selectedHostname)
    }.distinctUntilChanged().onEach { isActive ->
        if (!isActive) {
            saveStartTime(0L)
        } else if (getStartTime() == 0L) {
            saveStartTime(System.currentTimeMillis())
        }
        repositoryScope.launch {
            updateShortcuts()
            updateNotification()
        }
    }

    fun readPrivateDnsObservation(): PrivateDnsObservation =
        privateDnsController.observe().also { observation ->
            if (observation == PrivateDnsObservation.PermissionMissing) {
                activationRepository.refreshPermission()
            }
        }

    suspend fun toggle(): DnsConfigurationResult {
        if (!canControlPrivateDns()) return DnsConfigurationResult.PermissionMissing
        return configurationRepository.toggle().also(::onConfigurationResult)
    }

    suspend fun setEnabled(enabled: Boolean): DnsConfigurationResult {
        if (!canControlPrivateDns()) return DnsConfigurationResult.PermissionMissing
        val result = if (enabled) {
            when (val write = privateDnsController.enable(getDnsUrl())) {
                is DnsWriteResult.Success -> DnsConfigurationResult.StateChanged(write.observation)
                DnsWriteResult.PermissionMissing -> DnsConfigurationResult.PermissionMissing
                DnsWriteResult.MissingHostname -> DnsConfigurationResult.MissingHostname
                is DnsWriteResult.Failure,
                is DnsWriteResult.Rejected,
                -> DnsConfigurationResult.WriteFailed(write)
            }
        } else {
            when (
                val write = privateDnsController.disable(disableBehaviorRepository.behavior.value)
            ) {
                is DnsWriteResult.Success -> DnsConfigurationResult.StateChanged(write.observation)
                DnsWriteResult.PermissionMissing -> DnsConfigurationResult.PermissionMissing
                DnsWriteResult.MissingHostname -> DnsConfigurationResult.MissingHostname
                is DnsWriteResult.Failure,
                is DnsWriteResult.Rejected,
                -> DnsConfigurationResult.WriteFailed(write)
            }
        }
        onConfigurationResult(result)
        return result
    }

    suspend fun changeSelection(
        selection: DnsProviderSelection,
    ): DnsConfigurationResult {
        if (!canControlPrivateDns()) return DnsConfigurationResult.PermissionMissing
        return configurationRepository
            .changeSelection(selection)
            .also(::onConfigurationResult)
    }

    suspend fun changeEnhancedSelection(hostname: String): DnsConfigurationResult {
        if (!canControlPrivateDns()) return DnsConfigurationResult.PermissionMissing
        return configurationRepository.changeEnhancedSelection(
            providerId = DnsProviderCatalog.NEXTDNS,
            hostname = hostname,
        ).also(::onConfigurationResult)
    }

    fun stageEnhancedSelection(hostname: String): ProviderSelectionUpdateResult {
        val hostnameResult = providerSelectionRepository.setEnhancedHostname(hostname)
        if (hostnameResult !is ProviderSelectionUpdateResult.Saved) return hostnameResult
        return providerSelectionRepository.save(
            DnsProviderSelection.Enhanced(DnsProviderCatalog.NEXTDNS)
        )
    }

    fun stageSelection(selection: DnsProviderSelection): ProviderSelectionUpdateResult =
        providerSelectionRepository.save(selection)

    fun clearEnhancedHostname(): ProviderSelectionUpdateResult =
        providerSelectionRepository.setEnhancedHostname(null)

    fun currentSelection(): DnsProviderSelection =
        providerSelectionRepository.selection.value

    fun getSelectionFlow() = providerSelectionRepository.selection

    fun getDisableBehaviorFlow() = disableBehaviorRepository.behavior

    fun setDisableBehavior(value: com.eyalm.adns.data.dns.DnsDisableBehavior) {
        disableBehaviorRepository.set(value)
    }

    private fun canControlPrivateDns(): Boolean {
        activationRepository.refreshPermission()
        return activationRepository.state.value.canControlPrivateDns
    }

    fun getDnsUrl(): String? {
        return providerSelectionRepository.resolvedHostname.value
    }

    fun getDnsUrlFlow(): Flow<String> = providerSelectionRepository
        .resolvedHostname
        .filterNotNull()
        .distinctUntilChanged()

    private fun onConfigurationResult(result: DnsConfigurationResult) {
        if (result == DnsConfigurationResult.PermissionMissing) {
            activationRepository.refreshPermission()
            updateNotification()
            updateShortcuts()
            return
        }
        if (
            result is DnsConfigurationResult.StateChanged ||
            result is DnsConfigurationResult.Changed
        ) {
            val active = isAdBlockingActive()
            if (!active) {
                saveStartTime(0L)
            } else if (sharedPrefs.getLong("start_time", 0L) == 0L) {
                saveStartTime(System.currentTimeMillis())
            }
            updateNotification()
            updateShortcuts()
            TileService.requestListeningState(
                context,
                ComponentName(context, AdnsTileService::class.java),
            )
        }
    }

    fun saveStartTime(time: Long) {
        sharedPrefs.edit().putLong("start_time", time).apply()
    }

    fun getStartTime(): Long {
        val startTime = sharedPrefs.getLong("start_time", 0L)
        if (isAdBlockingActive() && startTime == 0L) {
            val now = System.currentTimeMillis()
            saveStartTime(now)
            return now
        }

        return startTime
    }

    fun updateShortcuts() {
        val isActive = isAdBlockingActive()
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        val shortcutId = "toggle_dns"
        if (!AppRuntimeRepositories.capabilities(context).current().canUseDnsToggleSurfaces) {
            runCatching {
                shortcutManager.disableShortcuts(
                    listOf(shortcutId),
                    context.getString(R.string.activation_required),
                )
            }.onFailure {
                Log.e("DnsRepository", "Failed to disable DNS shortcut", it)
            }
            return
        }
        runCatching { shortcutManager.enableShortcuts(listOf(shortcutId)) }

        val toggleShortcut = ShortcutInfo.Builder(context, shortcutId)
            .setShortLabel(if (isActive) context.getString(R.string.disable_blocker) else context.getString(R.string.enable_blocker))
            .setLongLabel(if (isActive) context.getString(R.string.disable_ad_blocker) else context.getString(R.string.enable_ad_blocker))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_monochrome))
            .setIntent(Intent(context, MainActivity::class.java).apply {
                action = "com.eyalm.adns.TOGGLE_ACTION"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
            .build()

        try {
            shortcutManager.dynamicShortcuts = listOf(toggleShortcut)
        } catch (e: Exception) {
            Log.e("DnsRepository", "Failed to update shortcuts")
        }
    }

    fun updateNotification() {
        notificationManager.updateNotification(isAdBlockingActive())
    }

    fun isNotificationEnabled(): Boolean {
        return notificationManager.isNotificationEnabled()
    }

    fun setNotificationEnabled(enabled: Boolean) {
        notificationManager.setNotificationEnabled(enabled, isAdBlockingActive())
    }
}
