package com.kernelpanic.baladdns.viewmodel.nextdns

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kernelpanic.baladdns.R
import com.kernelpanic.baladdns.data.nextdns.recreation.ParentalRecreationItem
import com.kernelpanic.baladdns.data.nextdns.recreation.RecreationItemCollection
import com.kernelpanic.baladdns.data.nextdns.recreation.RecreationScheduleError
import com.kernelpanic.baladdns.data.nextdns.recreation.RecreationScheduleDto
import com.kernelpanic.baladdns.data.nextdns.recreation.RecreationScheduleValidation
import com.kernelpanic.baladdns.data.nextdns.recreation.RecreationTimeDraft
import com.kernelpanic.baladdns.data.nextdns.recreation.NextDnsRecreationRepository
import com.kernelpanic.baladdns.domain.nextdns.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId

data class RecreationUiState(
    val loading: Boolean = false,
    val initialLoadComplete: Boolean = false,
    val services: List<ParentalRecreationItem> = emptyList(),
    val categories: List<ParentalRecreationItem> = emptyList(),
    val schedule: RecreationScheduleDto = RecreationScheduleDto(),
    val editorOpen: Boolean = false,
    val draftTimes: Map<String, RecreationTimeDraft> = emptyMap(),
    val scheduleErrors: Map<String, RecreationScheduleError> = emptyMap(),
    val savingSchedule: Boolean = false,
    val savingItems: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

class RecreationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NextDnsRecreationRepository()
    private var profileId: String? = null
    private var editable = false

    private val _state = MutableStateFlow(RecreationUiState())
    val state = _state.asStateFlow()

    fun load(profileId: String, editable: Boolean) {
        this.profileId = profileId
        this.editable = editable
        if (_state.value.loading) return

        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            when (val result = repository.get(profileId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        initialLoadComplete = true,
                        services = result.value.services,
                        categories = result.value.categories,
                        schedule = result.value.recreation,
                        errorMessage = null,
                    )
                }

                else -> _state.update {
                    it.copy(
                        loading = false,
                        initialLoadComplete = true,
                        errorMessage = requestFailureMessage(),
                    )
                }
            }
        }
    }

    fun openEditor() {
        if (!editable) return
        if (_state.value.savingSchedule) return
        val schedule = _state.value.schedule
        _state.update {
            it.copy(
                editorOpen = true,
                draftTimes = RecreationScheduleValidation.days.associateWith { day ->
                    schedule.times[day]?.let(RecreationScheduleValidation::toDraft)
                        ?: RecreationTimeDraft()
                },
                scheduleErrors = emptyMap(),
                errorMessage = null,
            )
        }
    }

    fun dismissEditor() {
        if (_state.value.savingSchedule) return
        _state.update { it.copy(editorOpen = false, scheduleErrors = emptyMap(), errorMessage = null) }
    }

    fun updateStart(day: String, value: String) {
        updateDraft(day) { it.copy(start = value) }
    }

    fun updateEnd(day: String, value: String) {
        updateDraft(day) { it.copy(end = value) }
    }

    fun setDayEnabled(day: String, enabled: Boolean) {
        updateDraft(day) { draft ->
            if (enabled) {
                draft.copy(enabled = true)
            } else {
                RecreationTimeDraft(enabled = false)
            }
        }
    }

    fun saveSchedule() {
        if (!editable) return
        val current = _state.value
        val profileId = profileId ?: return
        if (current.savingSchedule) return

        val serialized = RecreationScheduleValidation.serialize(current.draftTimes)
        if (serialized.errors.isNotEmpty()) {
            _state.update { it.copy(scheduleErrors = serialized.errors) }
            return
        }

        val schedule = RecreationScheduleDto(
            times = serialized.times,
            timezone = ZoneId.systemDefault().id,
        )
        _state.update { it.copy(savingSchedule = true, scheduleErrors = emptyMap(), errorMessage = null) }
        viewModelScope.launch {
            when (repository.updateSchedule(profileId, schedule)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(savingSchedule = false, editorOpen = false) }
                    load(profileId, editable)
                }

                else -> _state.update {
                    it.copy(savingSchedule = false, errorMessage = requestFailureMessage())
                }
            }
        }
    }

    fun toggleItem(collection: RecreationItemCollection, item: ParentalRecreationItem) {
        if (!editable) return
        val profileId = profileId ?: return
        if (!item.active) return
        val key = itemKey(collection, item.id)
        if (key in _state.value.savingItems) return

        _state.update { it.copy(savingItems = it.savingItems + key, errorMessage = null) }
        viewModelScope.launch {
            when (
                repository.updateItem(
                    profileId,
                    collection,
                    item.id,
                    !item.recreation,
                )
            ) {
                is ApiResult.Success -> {
                    _state.update { it.copy(savingItems = it.savingItems - key) }
                    load(profileId, editable)
                }

                else -> _state.update {
                    it.copy(
                        savingItems = it.savingItems - key,
                        errorMessage = requestFailureMessage(),
                    )
                }
            }
        }
    }

    fun isSavingItem(collection: RecreationItemCollection, itemId: String): Boolean =
        itemKey(collection, itemId) in _state.value.savingItems

    private fun updateDraft(day: String, transform: (RecreationTimeDraft) -> RecreationTimeDraft) {
        _state.update { state ->
            state.copy(
                draftTimes = state.draftTimes + (day to transform(state.draftTimes[day] ?: RecreationTimeDraft())),
                scheduleErrors = state.scheduleErrors - day,
                errorMessage = null,
            )
        }
    }

    private fun requestFailureMessage(): String =
        getApplication<Application>().getString(R.string.network_error_please_try_again)

    private fun itemKey(collection: RecreationItemCollection, itemId: String): String =
        "${collection.wireName}:$itemId"

}
