package com.eyalm.adns.viewmodel

import android.app.Application
import android.app.StatusBarManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyalm.adns.R
import com.eyalm.adns.data.DnsRepository
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.data.models.DnsProviders
import com.eyalm.adns.data.nextdns.api.NextDnsProfile
import com.eyalm.adns.data.nextdns.profile.NextDnsProfileRepository
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceSpec
import com.eyalm.adns.data.nextdns.auth.NextDnsManagementSession
import com.eyalm.adns.data.nextdns.auth.NextDnsSessionManager
import com.eyalm.adns.domain.nextdns.ApiResult
import com.eyalm.adns.domain.nextdns.ProfileCapabilities
import com.eyalm.adns.domain.nextdns.ProfileRole
import com.eyalm.adns.domain.nextdns.capabilities
import com.eyalm.adns.domain.nextdns.profileRoleFromWire
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

data class ProfileSessionState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val profiles: List<NextDnsProfile> = emptyList(),
    /** The locally configured profile, available even before the profile list can be refreshed. */
    val selectedProfileId: String? = null,
    val selected: NextDnsProfile? = null,
    val capabilities: ProfileCapabilities = ProfileRole.Unknown.capabilities(),
    val logsRevision: Long = 0,
    val error: ApiResult<*>? = null,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    enum class Page {
        MAIN,
        PROVIDERS,
        ACCOUNT_SETTINGS,
        SETUP,
        SECURITY,
        PRIVACY,
        PARENTAL_CONTROL,
        SETTINGS_PAGE,
        GENERIC_LIST,
        LOGS,
        LANGUAGE,
    }

    private val repository = DnsRepository(application)
    private val profileRepository = NextDnsProfileRepository(application)
    private val nextDnsSessionManager = NextDnsSessionManager.getInstance(application)

    private val _dnsUrl = MutableStateFlow(repository.getDnsUrl())
    val dnsUrl: StateFlow<String?> = _dnsUrl.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(repository.isNotificationEnabled())
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _profileSessionState = MutableStateFlow(
        ProfileSessionState(
            selectedProfileId = profileRepository.currentProfileId(),
        )
    )
    val profileSessionState = _profileSessionState.asStateFlow()
    private var profileLoadGeneration = 0L

    init {
        viewModelScope.launch {
            // Defer the first StateFlow emission until all ViewModel properties are initialized.
            yield()
            nextDnsSessionManager.state.collect { session ->
                if (session == NextDnsManagementSession.Expired) {
                    profileLoadGeneration++
                    invalidateProfileScopedState()
                    email = null
                    setPage(Page.MAIN)
                    val previous = _profileSessionState.value
                    _profileSessionState.value = previous.copy(
                        loading = false,
                        profiles = emptyList(),
                        selected = null,
                        capabilities = ProfileRole.Unknown.capabilities(),
                        error = ApiResult.ServerFailure(
                            status = 401,
                            problems = emptyList(),
                        ),
                    )
                }
            }
        }
    }

    fun refreshProfileSession(force: Boolean = false) {
        if (nextDnsSessionManager.state.value != NextDnsManagementSession.Active) return
        val generation = ++profileLoadGeneration
        val previous = _profileSessionState.value
        val selectedProfileId = profileRepository.currentProfileId()
        val selected = previous.selected?.takeIf { it.id == selectedProfileId }
        val profileChanged = previous.selectedProfileId != selectedProfileId
        if (profileChanged) {
            invalidateProfileScopedState()
            if (_page.value == Page.GENERIC_LIST) {
                setPage(Page.MAIN)
            }
        }
        _profileSessionState.value = previous.copy(
            loading = previous.profiles.isEmpty(),
            refreshing = force && previous.profiles.isNotEmpty(),
            selectedProfileId = selectedProfileId,
            selected = selected,
            capabilities = profileRoleFromWire(selected?.role).capabilities(),
            logsRevision = previous.logsRevision + if (profileChanged) 1 else 0,
            error = null,
        )
        viewModelScope.launch {
            val result = profileRepository.profiles()
            if (generation != profileLoadGeneration) return@launch
            when (result) {
                is ApiResult.Success -> publishProfileSession(result.value)
                else -> {
                    _profileSessionState.value = _profileSessionState.value.copy(
                        loading = false,
                        refreshing = false,
                        error = result,
                    )
                }
            }
        }
    }

    fun onProfileRemoved(
        removedProfileId: String,
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = profileRepository.profiles()
            val remaining = when (result) {
                is ApiResult.Success -> result.value.filterNot { it.id == removedProfileId }
                else -> _profileSessionState.value.profiles.filterNot { it.id == removedProfileId }
            }
            val wasSelected = profileRepository.currentProfileId() == removedProfileId
            if (wasSelected) {
                remaining.firstOrNull()?.let { fallback ->
                    profileRepository.selectProfile(
                        fallback,
                        profileRepository.deviceName(),
                    )
                } ?: profileRepository.clearSelectedProfile()
                refreshProvider()
            }
            invalidateProfileScopedState()
            publishProfileSession(remaining)
            if (result !is ApiResult.Success) {
                _profileSessionState.value = _profileSessionState.value.copy(error = result)
            }
            setPage(Page.MAIN)
            onComplete()
        }
    }

    fun onProfileRenamed() = refreshProfileSession()

    private fun publishProfileSession(availableProfiles: List<NextDnsProfile>) {
        val previous = _profileSessionState.value
        val locallySelectedId = profileRepository.currentProfileId()
        val selected = availableProfiles.firstOrNull { it.id == locallySelectedId }
        val selectedProfileId = selected?.id
        val profileChanged = previous.selectedProfileId != selectedProfileId
        if (profileChanged) {
            invalidateProfileScopedState()
            if (_page.value == Page.GENERIC_LIST) {
                setPage(Page.MAIN)
            }
        }
        _profileSessionState.value = ProfileSessionState(
            loading = false,
            refreshing = false,
            profiles = availableProfiles,
            selectedProfileId = selectedProfileId,
            selected = selected,
            capabilities = profileRoleFromWire(selected?.role).capabilities(),
            logsRevision = previous.logsRevision + if (profileChanged) 1 else 0,
        )
    }

    private fun invalidateProfileScopedState() {
        currentListSetting = null
    }

    fun invalidateLogs() {
        _profileSessionState.value = _profileSessionState.value.copy(
            logsRevision = _profileSessionState.value.logsRevision + 1,
        )
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.setNotificationEnabled(enabled)
        _notificationsEnabled.value = enabled
    }

    private val _page = MutableStateFlow(Page.MAIN)
    val page = _page.asStateFlow()

    fun setDnsUrl(url: String) {
        repository.setCustomUrl(url)
        _dnsUrl.value = url
    }

    fun addQuickTile() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager =
                getApplication<Application>().getSystemService(StatusBarManager::class.java)
            statusBarManager?.requestAddTileService(
                ComponentName(
                    getApplication(),
                    com.eyalm.adns.services.AdnsTileService::class.java
                ),
                getApplication<Application>().getString(R.string.adns_adblock),
                android.graphics.drawable.Icon.createWithResource(
                    getApplication(),
                    R.drawable.ic_launcher_foreground
                ),
                getApplication<Application>().mainExecutor
            ) { result ->
                val message = when (result) {
                    1 -> getApplication<Application>().getString(R.string.tile_already_added)
                    2 -> getApplication<Application>().getString(R.string.tile_added)
                    else -> ""
                }
                if (message.isNotEmpty())
                    Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                getApplication(),
                getApplication<Application>().getString(R.string.feature_not_supported_on_this_version),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun refreshNotification() {
        repository.updateNotification()
        _notificationsEnabled.value = repository.isNotificationEnabled()
    }


    private val _selectedProvider = MutableStateFlow(repository.getSelectedProvider())
    val selectedProvider = _selectedProvider.asStateFlow()

    fun setProvider(providerId: String, url: String? = null) {
        repository.setProvider(providerId, url)
        _selectedProvider.value = repository.getSelectedProvider()
    }

    fun refreshProvider() {
        _selectedProvider.value = repository.getSelectedProvider()
    }

    fun isLoggedIn(provider: DnsProvider): Boolean {
        return when (provider) {
            DnsProviders.NEXTDNS -> profileRepository.isSignedIn()
            else -> false
        }
    }

    suspend fun getEmail(): String {
        return profileRepository.email()
    }

    fun setPage(page: Page): Boolean {
        if (!requestPageAccess(page)) return false
        _page.value = page
        return true
    }

    private fun requestPageAccess(page: Page): Boolean = when (page) {
        Page.ACCOUNT_SETTINGS,
        Page.SETUP,
        Page.SECURITY,
        Page.PRIVACY,
        Page.PARENTAL_CONTROL,
        Page.SETTINGS_PAGE,
        Page.GENERIC_LIST,
        Page.LOGS,
        -> nextDnsSessionManager.requestFeatureAccess()

        Page.MAIN,
        Page.PROVIDERS,
        Page.LANGUAGE,
        -> true
    }



    var email by mutableStateOf<String?>(null)
    var nextDnsDeviceName by mutableStateOf(profileRepository.deviceName())
        private set

    fun setProfile(profile: NextDnsProfile) {
        val currentName = profileRepository.deviceName()
        val nameToSet = currentName.ifEmpty { "ADNS" }
        profileRepository.selectProfile(profile, nameToSet)
        nextDnsDeviceName = profileRepository.deviceName()
        val knownProfiles = _profileSessionState.value.profiles
        val updatedProfiles = if (knownProfiles.any { it.id == profile.id }) {
            knownProfiles.map { if (it.id == profile.id) profile else it }
        } else {
            knownProfiles + profile
        }
        publishProfileSession(updatedProfiles)
        refreshProvider()
        refreshProfileSession()
    }

    fun updateDeviceName(name: String) {
        profileRepository.setDeviceName(name)
        nextDnsDeviceName = profileRepository.deviceName()
        Toast.makeText(getApplication(), getApplication<Application>().getString(R.string.done), Toast.LENGTH_SHORT).show()
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            when (val createResult = profileRepository.createProfile(name)) {
                is ApiResult.Success -> when (val profilesResult = profileRepository.profiles()) {
                    is ApiResult.Success -> publishProfileSession(profilesResult.value)
                    else -> _profileSessionState.value =
                        _profileSessionState.value.copy(error = profilesResult)
                }

                else -> _profileSessionState.value =
                    _profileSessionState.value.copy(error = createResult)
            }
        }
    }

    fun logout() {
        profileLoadGeneration++
        profileRepository.signOut()
        invalidateProfileScopedState()
        _profileSessionState.value = ProfileSessionState(
            loading = false,
            selectedProfileId = profileRepository.currentProfileId(),
        )
        setPage(Page.MAIN)
        refreshProvider()
    }

    var currentListSetting: NextDnsResourceSpec? = null
        private set

    private var listParentPage: Page = Page.MAIN

    fun openListScreen(listSetting: NextDnsResourceSpec) {
        if (!requestPageAccess(Page.GENERIC_LIST)) return
        currentListSetting = listSetting
        listParentPage = when (listSetting.parentPage) {
            NextDnsResourceSpec.ParentPage.SECURITY -> Page.SECURITY
            NextDnsResourceSpec.ParentPage.PRIVACY -> Page.PRIVACY
            NextDnsResourceSpec.ParentPage.PARENTAL_CONTROL -> Page.PARENTAL_CONTROL
            null -> Page.MAIN
        }
        _page.value = Page.GENERIC_LIST
    }

    fun getListParentPage(): Page = listParentPage
}
