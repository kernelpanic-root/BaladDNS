package com.eyalm.adns.viewmodel.nextdns

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyalm.adns.R
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.nextdns.model.BuiltInListIcon
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.data.nextdns.model.nextDnsFaviconUrl
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceItem
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceRepository
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceSource
import com.eyalm.adns.data.nextdns.resources.NextDnsResourceSpec
import com.eyalm.adns.data.nextdns.settings.AddCustomDomainResult
import com.eyalm.adns.data.nextdns.settings.NextDnsSettingsRepository
import com.eyalm.adns.domain.nextdns.ApiResult
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ResourceListUiState(
    val profileId: String? = null,
    val spec: NextDnsResourceSpec? = null,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loaded: Boolean = false,
    val activeIds: Set<String> = emptySet(),
    val availableItems: List<NextDnsResourceItem> = emptyList(),
    val error: ApiResult<*>? = null,
)

class ResourceListViewModel(application: Application) : AndroidViewModel(application) {
    private val resourceRepository = NextDnsResourceRepository()
    private val settingsRepository = NextDnsSettingsRepository()

    private val _state = MutableStateFlow(ResourceListUiState())
    val state = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    private var loadGeneration = 0L
    private var editable = false
    private val savingItems = mutableSetOf<String>()

    fun load(
        profileId: String,
        spec: NextDnsResourceSpec,
        editable: Boolean,
        force: Boolean = false,
    ) {
        this.editable = editable
        val current = _state.value
        val sameTarget = current.profileId == profileId && current.spec == spec
        if (!force && sameTarget && (current.loading || current.loaded)) return

        val generation = ++loadGeneration
        _state.value = if (force && sameTarget && current.loaded) {
            current.copy(refreshing = true, error = null)
        } else {
            ResourceListUiState(profileId = profileId, spec = spec, loading = true)
        }

        viewModelScope.launch {
            try {
                val result = loadItems(profileId, spec)
                if (!isCurrent(generation, profileId, spec)) return@launch
                _state.value = when (result) {
                    is ApiResult.Success -> _state.value.copy(
                        loading = false,
                        refreshing = false,
                        loaded = true,
                        activeIds = result.value.first,
                        availableItems = result.value.second,
                        error = null,
                    )

                    else -> _state.value.copy(
                        loading = false,
                        refreshing = false,
                        loaded = true,
                        error = result,
                    )
                }
                if (result !is ApiResult.Success) emitLoadFailure()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (isCurrent(generation, profileId, spec)) {
                    _state.value = _state.value.copy(
                        loading = false,
                        refreshing = false,
                        loaded = true,
                        error = ApiResult.SerializationFailure(
                            IllegalStateException("Unable to load resource list")
                        ),
                    )
                    emitLoadFailure()
                }
            }
        }
    }

    fun refresh() {
        val current = _state.value
        val profileId = current.profileId ?: return
        val spec = current.spec ?: return
        load(profileId, spec, editable, force = true)
    }

    fun toggle(itemId: String) {
        if (!editable) return
        val current = _state.value
        val profileId = current.profileId ?: return
        val spec = current.spec ?: return
        if (current.loading || current.refreshing || !savingItems.add(itemId)) return

        val wasActive = itemId in current.activeIds
        val active = !wasActive
        _state.value = current.copy(
            activeIds = if (active) current.activeIds + itemId else current.activeIds - itemId
        )

        viewModelScope.launch {
            try {
                val result = if (spec.allowsCustomInput) {
                    settingsRepository.setCustomDomainActive(
                        profileId,
                        spec.apiPage,
                        itemId,
                        active,
                    )
                } else if (wasActive) {
                    settingsRepository.removeListItem(
                        profileId,
                        spec.apiPage,
                        spec.apiFeature,
                        itemId,
                    )
                } else {
                    settingsRepository.addListItem(
                        profileId,
                        spec.apiPage,
                        spec.apiFeature,
                        itemId,
                    )
                }

                if (result !is ApiResult.Success && isCurrent(profileId, spec)) {
                    val latest = _state.value
                    _state.value = latest.copy(
                        activeIds = if (wasActive) {
                            latest.activeIds + itemId
                        } else {
                            latest.activeIds - itemId
                        }
                    )
                    _messages.emit(
                        getApplication<Application>().getString(R.string.failed_to_update, itemId)
                    )
                }
            } finally {
                savingItems -= itemId
            }
        }
    }

    fun addCustomDomain(domain: String) {
        if (!editable) return
        val current = _state.value
        val profileId = current.profileId ?: return
        val spec = current.spec?.takeIf(NextDnsResourceSpec::allowsCustomInput) ?: return
        val cleanDomain = domain.trim().lowercase(Locale.ROOT)
        val itemWasPresent = current.availableItems.any { it.id == cleanDomain }
        val wasActive = cleanDomain in current.activeIds
        if (itemWasPresent && wasActive) return
        if (current.loading || current.refreshing || !savingItems.add(cleanDomain)) return

        val newItem = NextDnsResourceItem(
            id = cleanDomain,
            name = "*.$cleanDomain",
            icon = nextDnsFaviconUrl(cleanDomain)?.let(ListIcon::Url) ?: ListIcon.None,
        )
        _state.value = current.copy(
            availableItems = if (itemWasPresent) {
                current.availableItems
            } else {
                listOf(newItem) + current.availableItems
            },
            activeIds = current.activeIds + cleanDomain,
        )

        viewModelScope.launch {
            try {
                val addResult = if (itemWasPresent) {
                    AddCustomDomainResult.AlreadyExists
                } else {
                    settingsRepository.addCustomDomain(
                        profileId,
                        spec.apiPage,
                        cleanDomain,
                    )
                }
                val success = when (addResult) {
                    AddCustomDomainResult.Added -> true
                    AddCustomDomainResult.AlreadyExists ->
                        settingsRepository.setCustomDomainActive(
                            profileId,
                            spec.apiPage,
                            cleanDomain,
                            active = true,
                        ) is ApiResult.Success
                    is AddCustomDomainResult.Failure -> false
                }

                if (!success && isCurrent(profileId, spec)) {
                    val latest = _state.value
                    _state.value = latest.copy(
                        availableItems = if (itemWasPresent) {
                            latest.availableItems
                        } else {
                            latest.availableItems.filterNot { it.id == cleanDomain }
                        },
                        activeIds = if (wasActive) {
                            latest.activeIds + cleanDomain
                        } else {
                            latest.activeIds - cleanDomain
                        },
                    )
                    _messages.emit(
                        getApplication<Application>().getString(
                            R.string.failed_to_add,
                            cleanDomain,
                        )
                    )
                }
            } finally {
                savingItems -= cleanDomain
            }
        }
    }

    fun deleteCustomDomain(domain: String) {
        if (!editable) return
        val current = _state.value
        val profileId = current.profileId ?: return
        val spec = current.spec?.takeIf(NextDnsResourceSpec::allowsCustomInput) ?: return
        val removedItem = current.availableItems.firstOrNull { it.id == domain } ?: return
        if (current.loading || current.refreshing || !savingItems.add(domain)) return
        val wasActive = domain in current.activeIds

        _state.value = current.copy(
            availableItems = current.availableItems.filterNot { it.id == domain },
            activeIds = current.activeIds - domain,
        )

        viewModelScope.launch {
            try {
                val result = settingsRepository.removeCustomDomain(
                    profileId,
                    spec.apiPage,
                    domain,
                )
                if (result !is ApiResult.Success && isCurrent(profileId, spec)) {
                    val latest = _state.value
                    _state.value = latest.copy(
                        availableItems = listOf(removedItem) + latest.availableItems,
                        activeIds = if (wasActive) latest.activeIds + domain else latest.activeIds,
                    )
                    _messages.emit(
                        getApplication<Application>().getString(R.string.failed_to_delete, domain)
                    )
                }
            } finally {
                savingItems -= domain
            }
        }
    }

    private suspend fun loadItems(
        profileId: String,
        spec: NextDnsResourceSpec,
    ): ApiResult<Pair<Set<String>, List<NextDnsResourceItem>>> {
        if (spec.allowsCustomInput) {
            return when (
                val custom = resourceRepository.getCustomList(profileId, spec.apiPage)
            ) {
                is ApiResult.Success -> ApiResult.Success(
                    custom.value.activeIds to custom.value.items,
                    custom.status,
                )
                is ApiResult.ServerFailure -> custom
                is ApiResult.NetworkFailure -> custom
                is ApiResult.SerializationFailure -> custom
            }
        }

        return when (
            val active = resourceRepository.getActiveIds(
                profileId,
                spec.apiPage,
                spec.apiFeature,
            )
        ) {
            is ApiResult.Success -> {
                val catalog = when (spec.source) {
                    NextDnsResourceSource.SERVER ->
                        resourceRepository.getServerCatalog(spec.apiPage, spec.apiFeature)
                    NextDnsResourceSource.LOCALE ->
                        ApiResult.Success(loadLocaleItems(spec), 200)
                }
                when (catalog) {
                    is ApiResult.Success -> ApiResult.Success(
                        active.value to catalog.value,
                        catalog.status,
                    )
                    is ApiResult.ServerFailure -> catalog
                    is ApiResult.NetworkFailure -> catalog
                    is ApiResult.SerializationFailure -> catalog
                }
            }
            is ApiResult.ServerFailure -> active
            is ApiResult.NetworkFailure -> active
            is ApiResult.SerializationFailure -> active
        }
    }

    private fun loadLocaleItems(spec: NextDnsResourceSpec): List<NextDnsResourceItem> {
        val values = Locales.getMap(*spec.localePath.toTypedArray()) ?: return emptyList()
        return values.map { (key, value) ->
            when (spec.apiFeature) {
                "natives" -> {
                    val name = (value as? Map<*, *>)?.get("name") as? String
                    val devices = (value as? Map<*, *>)?.get("devices") as? String
                    val icon = when (key.lowercase(Locale.ROOT)) {
                        "windows", "apple" -> BuiltInListIcon.Computer
                        "xiaomi", "samsung", "huawei" -> BuiltInListIcon.Smartphone
                        "sonos" -> BuiltInListIcon.Speaker
                        else -> BuiltInListIcon.Devices
                    }
                    NextDnsResourceItem(
                        id = key,
                        name = name ?: key,
                        description = devices,
                        icon = ListIcon.BuiltIn(icon),
                    )
                }

                "categories" -> {
                    val name = (value as? Map<*, *>)?.get("name") as? String
                    val description = (value as? Map<*, *>)?.get("description") as? String
                    val icon = when (key.lowercase(Locale.ROOT)) {
                        "porn" -> BuiltInListIcon.Block
                        "dating" -> BuiltInListIcon.Favorite
                        "social" -> BuiltInListIcon.People
                        "video" -> BuiltInListIcon.PlayCircle
                        "games" -> BuiltInListIcon.SportsEsports
                        "gambling" -> BuiltInListIcon.Casino
                        "shopping" -> BuiltInListIcon.ShoppingBag
                        "chat" -> BuiltInListIcon.Chat
                        "music" -> BuiltInListIcon.MusicNote
                        else -> BuiltInListIcon.Folder
                    }
                    NextDnsResourceItem(
                        id = key,
                        name = name ?: key,
                        description = description,
                        icon = ListIcon.BuiltIn(icon),
                    )
                }

                else -> NextDnsResourceItem(
                    id = key,
                    name = value as? String ?: key,
                )
            }
        }
    }

    private fun isCurrent(
        generation: Long,
        profileId: String,
        spec: NextDnsResourceSpec,
    ): Boolean = generation == loadGeneration && isCurrent(profileId, spec)

    private fun isCurrent(profileId: String, spec: NextDnsResourceSpec): Boolean =
        _state.value.profileId == profileId && _state.value.spec == spec

    private suspend fun emitLoadFailure() {
        _messages.emit(
            getApplication<Application>().getString(
                R.string.failed_to_load_list_data_check_your_network_connection_and_try_again_later
            )
        )
    }
}
