package com.eyalm.adns.viewmodel

import android.app.Application
import android.app.StatusBarManager
import android.content.ComponentName
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyalm.adns.R
import com.eyalm.adns.data.ApiRepository
import com.eyalm.adns.data.Blocklist
import com.eyalm.adns.data.DnsRepository
import com.eyalm.adns.data.ListIcon
import com.eyalm.adns.data.ListItem
import com.eyalm.adns.data.ListSetting
import com.eyalm.adns.data.ListSource
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.ToggleSetting
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.data.models.DnsProviders
import com.eyalm.adns.data.network.NextDnsProfile
import com.eyalm.adns.data.network.toHexId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    enum class Page {
        MAIN,
        PROVIDERS,
        ACCOUNT_SETTINGS,
        SECURITY,
        PRIVACY,
        PARENTAL_CONTROL,
        SETTINGS_PAGE,
        GENERIC_LIST,
        BLOCKLISTS,
    }

    private val repository = DnsRepository(application)
    private val apiRepository = ApiRepository(application)

    private val _dnsUrl = MutableStateFlow(repository.getDnsUrl())
    val dnsUrl: StateFlow<String?> = _dnsUrl.asStateFlow()

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
                "ADNS AdBlock",
                android.graphics.drawable.Icon.createWithResource(
                    getApplication(),
                    R.drawable.ic_launcher_foreground
                ),
                getApplication<Application>().mainExecutor
            ) { result ->
                val message = when (result) {
                    1 -> "Tile already added!"
                    2 -> "Tile added!"
                    else -> ""
                }
                if (message.isNotEmpty())
                    Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                getApplication(),
                "Feature not supported on this version",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun refreshNotification() {
        repository.updateNotification()

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
            DnsProviders.NEXTDNS -> try {
                apiRepository.requireAuth()
                true
            } catch (e: Exception) {
                false
            }
            else -> false
        }
    }

    suspend fun getEmail(): String {
        return apiRepository.getNextDnsEmail()
    }

    fun setPage(page: Page) {
        _page.value = page
    }





    var profiles by mutableStateOf<List<NextDnsProfile>?>(null)
    var email by mutableStateOf<String?>(null)
    var currentProfile by mutableStateOf<NextDnsProfile?>(null)


    suspend fun getProfiles(): List<NextDnsProfile> {
        return apiRepository.getNextDnsProfiles()
    }

    suspend fun getCurrentProfile(): NextDnsProfile? {
        val profileId = apiRepository.getCurrentNextDnsProfileId()
        val currentProfiles = profiles ?: apiRepository.getNextDnsProfiles()
        return currentProfiles.firstOrNull { it.id == profileId }
    }

    fun setProfile(profile: NextDnsProfile) {
        apiRepository.setNextDnsProfile(profile)
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            apiRepository.createNextDnsProfile(name)
            profiles = getProfiles()
        }
    }

    fun logout() {
        apiRepository.nextDnsLogOut()
        setPage(Page.MAIN)
        refreshProvider()
    }

    // new generic methods


    // e.g. "nrd" or "logs.drop.ip"
    private val _pageToggles = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val pageToggles: StateFlow<Map<String, Boolean>> = _pageToggles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


    var currentListSetting: ListSetting? = null
        private set

    // Which page to return to when pressing back from the list screen
    private var listParentPage: Page = Page.MAIN

    private val _activeListIds = MutableStateFlow<Set<String>>(emptySet())
    val activeListIds: StateFlow<Set<String>> = _activeListIds.asStateFlow()

    private val _availableItems = MutableStateFlow<List<ListItem>>(emptyList())
    val availableItems: StateFlow<List<ListItem>> = _availableItems.asStateFlow()

    private val _loadedPageId = MutableStateFlow<String>("")
    val loadedPageId: StateFlow<String> = _loadedPageId.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    fun loadPageSettings(page: String, toggles: List<ToggleSetting>) {

        if (_loadedPageId.value == page && _pageToggles.value.isNotEmpty()) {
            return
        }

        _loadedPageId.value = ""
        _pageToggles.value = emptyMap()

        viewModelScope.launch {
            _isLoading.value = true
            val data = apiRepository.getPageSettings(page)
            if (data != null) {
                val states = mutableMapOf<String, Boolean>()
                for (toggle in toggles) {
                    val value = toggle.readFrom(data)
                    if (value != null) {
                        states[toggle.stateKey] = value
                    }
                }
                _pageToggles.value = states
                _loadedPageId.value = page
            } else {
                _errorMessage.emit("Failed to load page data. Check your network connection and try again later.")
            }
            _isLoading.value = false
        }
    }

    fun updateToggle(page: String, toggle: ToggleSetting, newValue: Boolean) {

        _pageToggles.value = _pageToggles.value.toMutableMap().apply {
            this[toggle.stateKey] = newValue
        }
        viewModelScope.launch {
            val payload = toggle.buildPatchPayload(newValue)
            val success = apiRepository.patchPageSettings(page, payload)
            if (!success) {
                _pageToggles.value = _pageToggles.value.toMutableMap().apply {
                    this[toggle.stateKey] = !newValue
                }
                _errorMessage.emit("Network error. Please try again.")
            }
        }
    }


    fun openListScreen(listSetting: ListSetting) {

        currentListSetting = listSetting

        listParentPage = when (listSetting.parentPage) {
            ListSetting.Page.SECURITY -> Page.SECURITY
            ListSetting.Page.PRIVACY -> Page.PRIVACY
            ListSetting.Page.PARENTAL_CONTROL -> Page.PARENTAL_CONTROL
            else -> Page.MAIN
        }
        _activeListIds.value = emptySet()
        _availableItems.value = emptyList()

        setPage(Page.GENERIC_LIST)
        loadListData(listSetting)
    }

    fun getListParentPage(): Page = listParentPage

    private fun loadListData(listSetting: ListSetting) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                if (listSetting.allowsCustomInput) {
                    val dataArray = apiRepository.getCustomListItems(listSetting.apiPage)

                    val activeIds = mutableSetOf<String>()
                    val items = mutableListOf<ListItem>()

                    dataArray?.forEach { element ->
                        val obj = element.asJsonObject
                        val id = obj.get("id").asString
                        val isActive = if (obj.has("active")) obj.get("active").asBoolean else true

                        items.add(ListItem(id = id, name = "*.$id"))
                        if (isActive) activeIds.add(id)
                    }

                    _activeListIds.value = activeIds
                    _availableItems.value = items
                } else {

                    val activeIds = apiRepository.getActiveListItems(
                        listSetting.apiPage, listSetting.apiFeat
                    )
                    _activeListIds.value = activeIds.toSet()

                    val items: List<ListItem> = when (listSetting.source) {
                        ListSource.SERVER -> loadServerList(listSetting)
                        ListSource.LOCALE -> loadLocaleList(listSetting)
                    }
                    _availableItems.value = items
                    if (_activeListIds.value.isEmpty()) {
                        _errorMessage.emit("Failed to load list data.")
                    }
                }
                _isLoading.value = false

            } catch (e: Exception) {
                _errorMessage.emit("Failed to load list data. Check your network connection and try again later.")
                _isLoading.value = false
            }
        }
    }


    private fun getDomain(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            url.replace("https://", "").replace("http://", "")
        } catch (e: Exception) {
            null
        }
    }


    private suspend fun loadServerList(listSetting: ListSetting): List<ListItem> {
        val catalog = apiRepository.getAvailableCatalog(
            listSetting.apiPage, listSetting.apiFeat
        ) ?: return emptyList()

        val dataArray = catalog.getAsJsonArray("data") ?: return emptyList()

        return dataArray.map { element ->
            val obj = element.asJsonObject
            Log.d("loadServerList", "obj: $obj")
            val id = obj.get("id").asString

            when (listSetting.apiFeat) {
                "tlds" -> ListItem(
                    id = id,
                    name = ".$id",
                )
                "blocklists" -> {
                    ListItem(
                        id = id,
                        name = obj.get("name")?.takeIf { !it.isJsonNull }?.asString ?: "NextDNS Ads & Trackers Blocklist" ,
                        description = obj.get("description")?.takeIf { !it.isJsonNull }?.asString ?: "A comprehensive blocklist to block ads & trackers in all countries. This is the recommended starter blocklist.",
                    )
                }
                "services" -> {
                    val website = obj.get("website")?.takeIf { !it.isJsonNull }?.asString ?: ""

                    val domain = website
                        .removePrefix("https://")
                        .removePrefix("http://")
                        .substringBefore("/")

                    val prettyName = Locales.getString("parentalControl", "services", "services", id)
                        .takeIf { it.isNotEmpty() } ?: id

                    ListItem(
                        id = id,
                        name = prettyName,
                        icon = ListIcon.Url("https://favicons.nextdns.io/${domain.toHexId()}@3x.png")
                    )
                }
                else -> ListItem(
                    id = id,
                    name = obj.get("name")?.takeIf { !it.isJsonNull }?.asString ?: id,
                    description = obj.get("description")?.takeIf { !it.isJsonNull }?.asString,
                    icon = ListIcon.Vector(androidx.compose.material.icons.Icons.Default.Shield)
                )
            }
        }
    }

    private fun loadLocaleList(listSetting: ListSetting): List<ListItem> {
        val map = Locales.getMap(*listSetting.localePath.toTypedArray())
            ?: return emptyList()

        return map.map { (key, value) ->
            when (listSetting.apiFeat) {
                "natives" -> {
                    val nameVal = if (value is Map<*, *>) (value["name"] as? String) else null
                    val devicesVal = if (value is Map<*, *>) (value["devices"] as? String) else null
                    val vector = when (key.lowercase()) {
                        "windows", "apple" -> androidx.compose.material.icons.Icons.Default.Computer
                        "xiaomi", "samsung", "huawei" -> androidx.compose.material.icons.Icons.Default.Smartphone
                        "sonos" -> androidx.compose.material.icons.Icons.Default.Speaker
                        else -> androidx.compose.material.icons.Icons.Default.Devices
                    }
                    ListItem(
                        id = key,
                        name = nameVal ?: key,
                        description = devicesVal,
                        icon = ListIcon.Vector(vector)
                    )
                }
                "categories" -> {
                    val nameVal = if (value is Map<*, *>) (value["name"] as? String) else null
                    val descVal = if (value is Map<*, *>) (value["description"] as? String) else null
                    val vector = when (key.lowercase()) {
                        "porn" -> androidx.compose.material.icons.Icons.Default.Block
                        "dating" -> androidx.compose.material.icons.Icons.Default.Favorite
                        "social" -> androidx.compose.material.icons.Icons.Default.People
                        "video" -> androidx.compose.material.icons.Icons.Default.PlayCircle
                        "games" -> androidx.compose.material.icons.Icons.Default.SportsEsports
                        "gambling" -> androidx.compose.material.icons.Icons.Default.Casino
                        "shopping" -> androidx.compose.material.icons.Icons.Default.ShoppingBag
                        "chat" -> androidx.compose.material.icons.Icons.Default.Chat
                        "music" -> androidx.compose.material.icons.Icons.Default.MusicNote
                        else -> androidx.compose.material.icons.Icons.Default.Folder
                    }
                    ListItem(
                        id = key,
                        name = nameVal ?: key,
                        description = descVal,
                        icon = ListIcon.Vector(vector)
                    )
                }
                else -> ListItem(
                    id = key,
                    name = (value as? String) ?: key,
                    icon = ListIcon.None
                )
            }
        }
    }


    fun toggleListItem(itemId: String) {
        val listSetting = currentListSetting ?: return
        val isCurrentlyActive = _activeListIds.value.contains(itemId)
        val newState = !isCurrentlyActive

        if (newState) _activeListIds.value += itemId else _activeListIds.value -= itemId


        viewModelScope.launch {
            val success = if (listSetting.allowsCustomInput) {
                apiRepository.patchCustomListItem(listSetting.apiPage, itemId, newState)
            } else {
                if (isCurrentlyActive) {
                    apiRepository.removeListItem(listSetting.apiPage, listSetting.apiFeat, itemId)
                } else {
                    apiRepository.addListItem(listSetting.apiPage, listSetting.apiFeat, itemId)
                }
            }
            if (!success) {
                if (isCurrentlyActive) _activeListIds.value += itemId else _activeListIds.value -= itemId
                _errorMessage.emit("Failed to update $itemId")
            }
        }
    }

    fun addCustomDomain(domain: String) {
        val listSetting = currentListSetting ?: return
        if (!listSetting.allowsCustomInput) return

        val cleanDomain = domain.trim().lowercase()

        val newItem = ListItem(id = cleanDomain, name = "*.$cleanDomain")
        _availableItems.value = listOf(newItem) + _availableItems.value
        _activeListIds.value += cleanDomain

        viewModelScope.launch {
            val success = apiRepository.addCustomListItem(listSetting.apiPage, cleanDomain)
            if (!success) {
                _availableItems.value = _availableItems.value.filter { it.id != cleanDomain }
                _activeListIds.value -= cleanDomain
                _errorMessage.emit("Failed to add $cleanDomain")
            }
        }
    }

    fun deleteCustomDomain(domain: String) {
        val listSetting = currentListSetting ?: return
        if (!listSetting.allowsCustomInput) return

        val wasActive = _activeListIds.value.contains(domain)

        _availableItems.value = _availableItems.value.filter { it.id != domain }
        _activeListIds.value -= domain

        viewModelScope.launch {
            val success = apiRepository.removeCustomListItem(listSetting.apiPage, domain)
            if (!success) {
                _availableItems.value = listOf(ListItem(id = domain, name = domain)) + _availableItems.value
                if (wasActive) _activeListIds.value += domain
                _errorMessage.emit("Failed to delete $domain")
            }
        }
    }

    // old methods

    var blocklists: List<Blocklist>? by mutableStateOf(null)

    fun getBlocklists() {
        viewModelScope.launch {
            val blocklistsResponse = apiRepository.getNextDnsBlocklists()
            blocklists = blocklistsResponse
        }
    }


    fun updateBlocklists(blocklistId: String) {
        viewModelScope.launch {
            apiRepository.updateNextDnsBlocklists(blocklistId)
        }
    }

    fun removeBlocklists(blocklistId: String) {
        viewModelScope.launch {
            apiRepository.removeNextDnsBlocklists(blocklistId)
        }
    }

}