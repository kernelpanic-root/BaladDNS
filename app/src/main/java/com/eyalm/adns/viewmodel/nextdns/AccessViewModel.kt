package com.eyalm.adns.viewmodel.nextdns

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyalm.adns.R
import com.eyalm.adns.data.nextdns.access.AccessEntry
import com.eyalm.adns.data.nextdns.access.AccessError
import com.eyalm.adns.data.nextdns.access.AccessField
import com.eyalm.adns.data.nextdns.access.AccessFormValidation
import com.eyalm.adns.data.nextdns.access.AccessRole
import com.eyalm.adns.data.nextdns.access.NextDnsAccessRepository
import com.eyalm.adns.domain.nextdns.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccessUiState(
    val loading: Boolean = false,
    val initialLoadComplete: Boolean = false,
    val items: List<AccessEntry> = emptyList(),
    val inviteDialogOpen: Boolean = false,
    val email: String = "",
    val inviteRole: AccessRole = AccessRole.Editor,
    val fieldErrors: Map<AccessField, AccessError> = emptyMap(),
    val roleTarget: AccessEntry? = null,
    val selectedRole: AccessRole = AccessRole.Editor,
    val deleting: AccessEntry? = null,
    val submitting: Boolean = false,
    val errorMessage: String? = null,
)

class AccessViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NextDnsAccessRepository()
    private var profileId: String? = null
    private var canManage = false

    private val _state = MutableStateFlow(AccessUiState())
    val state = _state.asStateFlow()

    fun load(profileId: String, canManage: Boolean) {
        this.profileId = profileId
        this.canManage = canManage
        if (_state.value.loading) return

        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            when (val result = repository.get(profileId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        initialLoadComplete = true,
                        items = result.value,
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

    fun openInvite() {
        if (!canManage) return
        if (_state.value.submitting) return
        _state.update {
            it.copy(
                inviteDialogOpen = true,
                email = "",
                inviteRole = AccessRole.Editor,
                fieldErrors = emptyMap(),
                errorMessage = null,
            )
        }
    }

    fun dismissInvite() {
        if (_state.value.submitting) return
        _state.update { it.copy(inviteDialogOpen = false, fieldErrors = emptyMap(), errorMessage = null) }
    }

    fun updateEmail(value: String) {
        _state.update {
            it.copy(
                email = value,
                fieldErrors = it.fieldErrors - AccessField.Email,
                errorMessage = null,
            )
        }
    }

    fun updateInviteRole(role: AccessRole) {
        _state.update { it.copy(inviteRole = role, errorMessage = null) }
    }

    fun submitInvite() {
        if (!canManage) return
        val current = _state.value
        val profileId = profileId ?: return
        if (current.submitting) return

        val email = current.email.trim()
        val errors = AccessFormValidation.localErrors(email)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors) }
            return
        }

        _state.update { it.copy(submitting = true, fieldErrors = emptyMap(), errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.invite(profileId, email, current.inviteRole)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            submitting = false,
                            inviteDialogOpen = false,
                            email = "",
                            fieldErrors = emptyMap(),
                        )
                    }
                    load(profileId, canManage)
                }

                is ApiResult.ServerFailure -> {
                    val serverErrors = AccessFormValidation.serverErrors(result.problems)
                    _state.update {
                        it.copy(
                            submitting = false,
                            fieldErrors = serverErrors,
                            errorMessage = if (serverErrors.isEmpty()) requestFailureMessage() else null,
                        )
                    }
                }

                else -> _state.update {
                    it.copy(submitting = false, errorMessage = requestFailureMessage())
                }
            }
        }
    }

    fun requestRoleChange(entry: AccessEntry) {
        if (!canManage) return
        if (_state.value.submitting) return
        _state.update {
            it.copy(roleTarget = entry, selectedRole = entry.role, errorMessage = null)
        }
    }

    fun updateSelectedRole(role: AccessRole) {
        _state.update { it.copy(selectedRole = role, errorMessage = null) }
    }

    fun dismissRoleChange() {
        if (_state.value.submitting) return
        _state.update { it.copy(roleTarget = null, errorMessage = null) }
    }

    fun submitRoleChange() {
        if (!canManage) return
        val current = _state.value
        val profileId = profileId ?: return
        val target = current.roleTarget ?: return
        if (current.submitting) return
        if (target.role == current.selectedRole) {
            dismissRoleChange()
            return
        }

        _state.update { it.copy(submitting = true, errorMessage = null) }
        viewModelScope.launch {
            when (repository.updateRole(profileId, target.email, current.selectedRole)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false, roleTarget = null) }
                    load(profileId, canManage)
                }

                else -> _state.update {
                    it.copy(submitting = false, errorMessage = requestFailureMessage())
                }
            }
        }
    }

    fun requestDelete(entry: AccessEntry) {
        if (!canManage) return
        if (_state.value.submitting) return
        _state.update { it.copy(deleting = entry, errorMessage = null) }
    }

    fun dismissDelete() {
        if (_state.value.submitting) return
        _state.update { it.copy(deleting = null, errorMessage = null) }
    }

    fun confirmDelete() {
        if (!canManage) return
        val target = _state.value.deleting ?: return
        val profileId = profileId ?: return
        if (_state.value.submitting) return

        _state.update { it.copy(submitting = true, errorMessage = null) }
        viewModelScope.launch {
            when (repository.delete(profileId, target.email)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(submitting = false, deleting = null) }
                    load(profileId, canManage)
                }

                else -> _state.update {
                    it.copy(submitting = false, errorMessage = requestFailureMessage())
                }
            }
        }
    }

    private fun requestFailureMessage(): String =
        getApplication<Application>().getString(R.string.network_error_please_try_again)
}
