package com.eyalm.adns.viewmodel.nextdns

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyalm.adns.R
import com.eyalm.adns.data.nextdns.settings.BooleanSettingSpec
import com.eyalm.adns.data.nextdns.settings.IntSelectSettingSpec
import com.eyalm.adns.data.nextdns.settings.NextDnsSettingsRepository
import com.eyalm.adns.data.nextdns.settings.ProfileSettingSpec
import com.eyalm.adns.data.nextdns.settings.SettingId
import com.eyalm.adns.data.nextdns.settings.SettingsPageSpec
import com.eyalm.adns.data.nextdns.settings.StringSelectSettingSpec
import com.eyalm.adns.data.nextdns.settings.valueAt
import com.eyalm.adns.domain.nextdns.ApiResult
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScalarSettingsUiState(
    val page: String? = null,
    val profileId: String? = null,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loaded: Boolean = false,
    val values: Map<SettingId, JsonElement> = emptyMap(),
    val saving: Set<SettingId> = emptySet(),
    val pendingConfirmation: PendingSettingChange? = null,
    val refreshRevision: Long = 0,
)

data class PendingSettingChange(
    val spec: ProfileSettingSpec<*>,
    val encodedValue: Any,
)

class ScalarSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NextDnsSettingsRepository()

    private val _state = MutableStateFlow(ScalarSettingsUiState())
    val state = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    private var loadGeneration = 0L
    private var editable = false

    fun load(
        profileId: String,
        pageSpec: SettingsPageSpec,
        editable: Boolean,
        force: Boolean = false,
    ) {
        this.editable = editable
        val current = _state.value
        if (
            !force &&
            current.page == pageSpec.page &&
            current.profileId == profileId &&
            (current.loading || current.loaded)
        ) return

        val generation = ++loadGeneration
        _state.value = if (
            force &&
            current.page == pageSpec.page &&
            current.profileId == profileId &&
            current.loaded
        ) {
            current.copy(
                refreshing = true,
                refreshRevision = current.refreshRevision + 1,
            )
        } else {
            ScalarSettingsUiState(
                page = pageSpec.page,
                profileId = profileId,
                loading = true,
            )
        }

        viewModelScope.launch {
            when (val result = repository.getScalarSettings(profileId, pageSpec.page)) {
                is ApiResult.Success -> {
                    if (!isCurrent(generation, profileId, pageSpec.page)) return@launch
                    val values = buildMap {
                        pageSpec.settings.forEach { spec ->
                            val raw = result.value.valueAt(spec.api.path)
                            if (raw != null && spec.isValid(raw)) put(spec.id, raw)
                        }
                    }
                    _state.value = ScalarSettingsUiState(
                        page = pageSpec.page,
                        profileId = profileId,
                        loaded = true,
                        values = values,
                        refreshRevision = current.refreshRevision + if (force) 1 else 0,
                    )
                }

                else -> {
                    if (!isCurrent(generation, profileId, pageSpec.page)) return@launch
                    val latest = _state.value
                    _state.value = if (latest.loaded) {
                        latest.copy(refreshing = false)
                    } else {
                        ScalarSettingsUiState(page = pageSpec.page, profileId = profileId)
                    }
                    _messages.emit(
                        getApplication<Application>().getString(
                            R.string.failed_to_load_page_data_check_your_network_connection_and_try_again_later
                        )
                    )
                }
            }
        }
    }

    fun changeBoolean(spec: BooleanSettingSpec, value: Boolean) {
        if (!editable) return
        requestChange(spec, spec.encode(value))
    }

    fun changeInt(spec: IntSelectSettingSpec, value: Int) {
        if (!editable) return
        requestChange(spec, spec.encode(value))
    }

    fun changeString(spec: StringSelectSettingSpec, value: String) {
        if (!editable) return
        requestChange(spec, spec.encode(value))
    }

    fun confirmPendingChange() {
        val pending = _state.value.pendingConfirmation ?: return
        _state.value = _state.value.copy(pendingConfirmation = null)
        persistChange(pending.spec, pending.encodedValue)
    }

    fun cancelPendingChange() {
        _state.value = _state.value.copy(pendingConfirmation = null)
    }

    private fun requestChange(spec: ProfileSettingSpec<*>, encodedValue: Any) {
        val state = _state.value
        if (spec.id in state.saving || state.pendingConfirmation != null) return
        if (state.values[spec.id] == Gson().toJsonTree(encodedValue)) return

        if (spec.confirmation != null) {
            _state.value = state.copy(
                pendingConfirmation = PendingSettingChange(spec, encodedValue)
            )
        } else {
            persistChange(spec, encodedValue)
        }
    }

    private fun persistChange(spec: ProfileSettingSpec<*>, encodedValue: Any) {
        if (!editable) return
        val previousValue = _state.value.values[spec.id] ?: return
        val profileId = _state.value.profileId ?: return
        val encodedJson = Gson().toJsonTree(encodedValue)

        _state.value = _state.value.copy(
            values = _state.value.values + (spec.id to encodedJson),
            saving = _state.value.saving + spec.id,
        )

        viewModelScope.launch {
            val result = repository.patchScalarSetting(profileId, spec.api, encodedValue)
            val current = _state.value
            if (current.page != spec.api.page || current.profileId != profileId) return@launch

            if (result is ApiResult.Success) {
                _state.value = current.copy(saving = current.saving - spec.id)
            } else {
                _state.value = current.copy(
                    values = current.values + (spec.id to previousValue),
                    saving = current.saving - spec.id,
                )
                _messages.emit(
                    getApplication<Application>().getString(
                        R.string.network_error_please_try_again
                    )
                )
            }
        }
    }

    private fun isCurrent(generation: Long, profileId: String, page: String): Boolean =
        generation == loadGeneration &&
            _state.value.profileId == profileId &&
            _state.value.page == page

    private fun ProfileSettingSpec<*>.isValid(raw: JsonElement): Boolean = when (this) {
        is BooleanSettingSpec -> decode(raw) != null
        is IntSelectSettingSpec -> decode(raw) != null
        is StringSelectSettingSpec -> decode(raw) != null
    }
}
