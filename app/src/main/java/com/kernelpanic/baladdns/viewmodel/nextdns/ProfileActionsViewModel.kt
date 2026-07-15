package com.kernelpanic.baladdns.viewmodel.nextdns

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kernelpanic.baladdns.R
import com.kernelpanic.baladdns.data.nextdns.logs.LogExportResult
import com.kernelpanic.baladdns.data.nextdns.profile.NextDnsProfileRepository
import com.kernelpanic.baladdns.domain.nextdns.ApiResult
import com.kernelpanic.baladdns.domain.nextdns.ProfileCapabilities
import com.kernelpanic.baladdns.domain.nextdns.ProfileRole
import com.kernelpanic.baladdns.domain.nextdns.capabilities
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProfileActionDialog {
    ClearLogs,
    Duplicate,
    Rename,
    DeleteOrLeave,
}

enum class ProfileAction {
    ClearLogs,
    Duplicate,
    Rename,
    DeleteOrLeave,
    DownloadLogs,
}

enum class ProfileNameError {
    Required,
    Duplicate,
}

data class ProfileActionsUiState(
    val dialog: ProfileActionDialog? = null,
    val inFlight: ProfileAction? = null,
    val duplicateName: String = "",
    val renameName: String = "",
    val duplicateNameError: ProfileNameError? = null,
    val renameNameError: ProfileNameError? = null,
    val errorMessage: String? = null,
)

sealed interface ProfileActionsEffect {
    data class Message(val value: String) : ProfileActionsEffect
    data class ProfileRemoved(val profileId: String) : ProfileActionsEffect
    data object LogsCleared : ProfileActionsEffect
    data object ProfileDuplicated : ProfileActionsEffect
    data object ProfileRenamed : ProfileActionsEffect
}

class ProfileActionsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NextDnsProfileRepository(application)
    private var capabilities = ProfileRole.Unknown.capabilities()

    private val _state = MutableStateFlow(ProfileActionsUiState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ProfileActionsEffect>()
    val effects = _effects.asSharedFlow()

    fun setCapabilities(value: ProfileCapabilities) {
        capabilities = value
    }

    fun openClearLogs() {
        if (!capabilities.canEditSettings) return
        open(ProfileActionDialog.ClearLogs)
    }

    fun openDuplicate() = open(ProfileActionDialog.Duplicate) {
        copy(duplicateName = "", duplicateNameError = null)
    }

    fun openRename(currentName: String) {
        if (!capabilities.canEditSettings) return
        open(ProfileActionDialog.Rename) {
            copy(renameName = currentName, renameNameError = null)
        }
    }

    fun openDeleteOrLeave() {
        if (!capabilities.canDelete && !capabilities.canLeave) return
        open(ProfileActionDialog.DeleteOrLeave)
    }

    fun dismissDialog() {
        if (_state.value.inFlight != null) return
        _state.update {
            it.copy(
                dialog = null,
                duplicateNameError = null,
                renameNameError = null,
                errorMessage = null,
            )
        }
    }

    fun updateDuplicateName(value: String) {
        _state.update { it.copy(duplicateName = value, duplicateNameError = null, errorMessage = null) }
    }

    fun updateRenameName(value: String) {
        _state.update { it.copy(renameName = value, renameNameError = null, errorMessage = null) }
    }

    fun clearLogs(profileId: String) {
        if (!capabilities.canEditSettings) return
        if (!begin(ProfileAction.ClearLogs)) return
        viewModelScope.launch {
            when (val result = repository.clearLogs(profileId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(dialog = null, inFlight = null) }
                    _effects.emit(ProfileActionsEffect.LogsCleared)
                    _effects.emit(ProfileActionsEffect.Message(getString(R.string.logs_cleared)))
                }

                else -> {
                    fail()
                }
            }
        }
    }

    fun exportLogs(profileId: String, destination: Uri) {
        if (!begin(ProfileAction.DownloadLogs)) return
        viewModelScope.launch {
            when (val result = repository.exportLogs(profileId, destination)) {
                LogExportResult.Success -> {
                    _state.update { it.copy(inFlight = null) }
                    _effects.emit(ProfileActionsEffect.Message(getString(R.string.logs_downloaded)))
                }

                is LogExportResult.ApiFailure -> {
                    fail(keepDialog = false)
                    _effects.emit(ProfileActionsEffect.Message(failureMessage()))
                }

                is LogExportResult.DestinationFailure -> {
                    fail(keepDialog = false)
                    _effects.emit(ProfileActionsEffect.Message(failureMessage()))
                }
            }
        }
    }

    fun duplicateProfile(profileId: String) {
        val name = _state.value.duplicateName.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(duplicateNameError = ProfileNameError.Required) }
            return
        }
        if (!begin(ProfileAction.Duplicate)) return

        viewModelScope.launch {
            when (val result = repository.duplicateProfile(profileId, name)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(dialog = null, inFlight = null, duplicateName = "") }
                    _effects.emit(ProfileActionsEffect.ProfileDuplicated)
                    _effects.emit(ProfileActionsEffect.Message(getString(R.string.profile_duplicated)))
                }

                is ApiResult.ServerFailure -> {
                    _state.update {
                        it.copy(
                            inFlight = null,
                            duplicateNameError = if (result.problems.any { problem -> problem.code == "duplicate" }) {
                                ProfileNameError.Duplicate
                            } else {
                                null
                            },
                            errorMessage = if (result.problems.any { problem -> problem.code == "duplicate" }) {
                                null
                            } else {
                                failureMessage()
                            },
                        )
                    }
                }

                else -> {
                    fail()
                }
            }
        }
    }

    fun renameProfile(profileId: String) {
        if (!capabilities.canEditSettings) return
        val name = _state.value.renameName.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(renameNameError = ProfileNameError.Required) }
            return
        }
        if (!begin(ProfileAction.Rename)) return

        viewModelScope.launch {
            when (val result = repository.renameProfile(profileId, name)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(dialog = null, inFlight = null) }
                    _effects.emit(ProfileActionsEffect.ProfileRenamed)
                    _effects.emit(ProfileActionsEffect.Message(getString(R.string.profile_renamed)))
                }

                is ApiResult.ServerFailure -> {
                    _state.update {
                        it.copy(
                            inFlight = null,
                            renameNameError = if (result.problems.any { problem -> problem.code == "duplicate" }) {
                                ProfileNameError.Duplicate
                            } else {
                                null
                            },
                            errorMessage = if (result.problems.any { problem -> problem.code == "duplicate" }) {
                                null
                            } else {
                                failureMessage()
                            },
                        )
                    }
                }

                else -> {
                    fail()
                }
            }
        }
    }

    fun deleteOrLeaveProfile(profileId: String) {
        if (!capabilities.canDelete && !capabilities.canLeave) return
        if (!begin(ProfileAction.DeleteOrLeave)) return
        viewModelScope.launch {
            when (val result = repository.deleteOrLeaveProfile(profileId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(dialog = null, inFlight = null) }
                    _effects.emit(ProfileActionsEffect.ProfileRemoved(profileId))
                }

                else -> {
                    fail()
                }
            }
        }
    }

    private fun open(
        dialog: ProfileActionDialog,
        transform: ProfileActionsUiState.() -> ProfileActionsUiState = { this },
    ) {
        if (_state.value.inFlight != null) return
        _state.update { it.transform().copy(dialog = dialog, errorMessage = null) }
    }

    private fun begin(action: ProfileAction): Boolean {
        if (_state.value.inFlight != null) return false
        _state.update { it.copy(inFlight = action, errorMessage = null) }
        return true
    }

    private fun fail(keepDialog: Boolean = true) {
        _state.update {
            it.copy(
                dialog = if (keepDialog) it.dialog else null,
                inFlight = null,
                errorMessage = failureMessage(),
            )
        }
    }

    private fun failureMessage(): String = getString(R.string.network_error_please_try_again)

    private fun getString(resourceId: Int): String =
        getApplication<Application>().getString(resourceId)
}
