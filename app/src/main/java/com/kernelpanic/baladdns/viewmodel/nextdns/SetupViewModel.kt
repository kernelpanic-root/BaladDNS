package com.kernelpanic.baladdns.viewmodel.nextdns

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kernelpanic.baladdns.data.nextdns.setup.LinkIpCapability
import com.kernelpanic.baladdns.data.nextdns.setup.DdnsValidationError
import com.kernelpanic.baladdns.data.nextdns.setup.SetupContent
import com.kernelpanic.baladdns.data.nextdns.setup.SetupRepository
import com.kernelpanic.baladdns.data.nextdns.setup.shouldShowLinkIp
import com.kernelpanic.baladdns.data.nextdns.setup.validateDdnsHostname
import com.kernelpanic.baladdns.domain.nextdns.ApiResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SetupUiState(
    val profileId: String? = null,
    val content: SetupContent? = null,
    val loading: Boolean = false,
    val linkingIp: Boolean = false,
    val checkingLinkedIp: Boolean = false,
    val canManageLinkedIp: Boolean = false,
    val linkIpAvailable: Boolean = false,
    val advancedOptionsVisible: Boolean = false,
    val ddnsDialog: DdnsDialogState? = null,
    val removingDdns: Boolean = false,
    val error: ApiResult<*>? = null,
)

data class DdnsDialogState(
    val hostname: String = "",
    val error: DdnsValidationError? = null,
    val submitting: Boolean = false,
)

sealed interface SetupEffect {
    data class CopyToClipboard(val text: String) : SetupEffect
    data object LinkedIpUpdated : SetupEffect
}

class SetupViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SetupRepository(application)

    private val _state = MutableStateFlow(SetupUiState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<SetupEffect>(extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    private var capability: LinkIpCapability? = null
    private var loadGeneration = 0L

    fun load(profileId: String, force: Boolean = false) {
        val current = _state.value
        if (!force && current.profileId == profileId && (current.loading || current.content != null)) {
            return
        }

        val profileChanged = current.profileId != profileId
        if (profileChanged) {
            capability = null
        }

        val generation = ++loadGeneration
        _state.value = if (profileChanged) {
            SetupUiState(
                profileId = profileId,
                loading = true,
                canManageLinkedIp = current.canManageLinkedIp,
            )
        } else {
            current.copy(
                loading = true,
                linkingIp = false,
                checkingLinkedIp = false,
                linkIpAvailable = false,
                advancedOptionsVisible = false,
                error = null,
            )
        }

        viewModelScope.launch {
            when (val result = repository.getSetup(profileId)) {
                is ApiResult.Success -> {
                    if (!isCurrentRequest(profileId, generation)) return@launch
                    capability = result.value.linkIpCapability
                    _state.value = _state.value.copy(
                        content = result.value.content,
                        loading = false,
                        linkingIp = false,
                        checkingLinkedIp = result.value.linkIpCapability != null,
                        linkIpAvailable = false,
                        error = null,
                    )
                    result.value.linkIpCapability?.let { linkCapability ->
                        checkLinkedIp(
                            profileId = profileId,
                            capability = linkCapability,
                            linkedIp = result.value.content.linkedIp.address,
                            generation = generation,
                        )
                    }
                }

                else -> {
                    if (!isCurrentRequest(profileId, generation)) return@launch
                    _state.value = _state.value.copy(
                        loading = false,
                        linkingIp = false,
                        checkingLinkedIp = false,
                        error = result,
                    )
                }
            }
        }
    }

    fun setCanManageLinkedIp(canManage: Boolean) {
        _state.value = _state.value.copy(
            canManageLinkedIp = canManage,
            advancedOptionsVisible = _state.value.advancedOptionsVisible && canManage,
            ddnsDialog = _state.value.ddnsDialog?.takeIf { canManage },
        )
    }

    fun retry() {
        _state.value.profileId?.let { profileId -> load(profileId, force = true) }
    }

    fun showAdvancedOptions() {
        if (capability != null && _state.value.canManageLinkedIp) {
            _state.value = _state.value.copy(advancedOptionsVisible = true)
        }
    }

    fun copyProgrammaticUpdateUrl() {
        if (!_state.value.canManageLinkedIp || !_state.value.advancedOptionsVisible) return
        capability?.programmaticUrl()?.let { url ->
            _effects.tryEmit(SetupEffect.CopyToClipboard(url))
        }
    }

    fun linkCurrentIp() {
        val currentCapability = capability ?: return
        val profileId = _state.value.profileId ?: return
        if (
            !_state.value.canManageLinkedIp ||
            _state.value.linkingIp ||
            _state.value.loading ||
            !_state.value.linkIpAvailable
        ) {
            return
        }

        _state.value = _state.value.copy(linkingIp = true, error = null)
        viewModelScope.launch {
            when (val result = repository.linkCurrentIp(currentCapability)) {
                is ApiResult.Success -> {
                    if (_state.value.profileId != profileId) return@launch
                    _effects.emit(SetupEffect.LinkedIpUpdated)
                    load(profileId, force = true)
                }

                else -> {
                    if (_state.value.profileId != profileId) return@launch
                    _state.value = _state.value.copy(linkingIp = false, error = result)
                }
            }
        }
    }

    fun openDdnsDialog() {
        if (!_state.value.canManageLinkedIp) return
        _state.value = _state.value.copy(
            ddnsDialog = DdnsDialogState(),
        )
    }

    fun updateDdnsHostname(hostname: String) {
        val dialog = _state.value.ddnsDialog ?: return
        _state.value = _state.value.copy(
            ddnsDialog = dialog.copy(hostname = hostname, error = null),
        )
    }

    fun dismissDdnsDialog() {
        val dialog = _state.value.ddnsDialog ?: return
        if (dialog.submitting) return
        _state.value = _state.value.copy(ddnsDialog = null)
    }

    fun submitDdns() {
        val dialog = _state.value.ddnsDialog ?: return
        val profileId = _state.value.profileId ?: return
        if (!canSubmitDdns()) return

        val hostname = dialog.hostname.trim()
        validateDdnsHostname(hostname)?.let { error ->
            _state.value = _state.value.copy(
                ddnsDialog = dialog.copy(error = error),
            )
            return
        }

        _state.value = _state.value.copy(
            ddnsDialog = dialog.copy(
                hostname = hostname,
                error = null,
                submitting = true,
            ),
        )
        viewModelScope.launch {
            when (val result = repository.updateDdns(profileId, hostname)) {
                is ApiResult.Success -> {
                    if (_state.value.profileId != profileId) return@launch
                    _state.value = _state.value.copy(ddnsDialog = null)
                    load(profileId, force = true)
                }

                is ApiResult.ServerFailure -> {
                    if (_state.value.profileId != profileId) return@launch
                    _state.value = _state.value.copy(
                        ddnsDialog = dialog.copy(
                            hostname = hostname,
                            submitting = false,
                            error = result.problems.firstOrNull()
                                ?.code
                                ?.let(DdnsValidationError::Server)
                                ?: DdnsValidationError.Request,
                        ),
                    )
                }

                else -> {
                    if (_state.value.profileId != profileId) return@launch
                    _state.value = _state.value.copy(
                        ddnsDialog = dialog.copy(
                            hostname = hostname,
                            submitting = false,
                            error = DdnsValidationError.Request,
                        ),
                    )
                }
            }
        }
    }

    fun removeDdns() {
        val profileId = _state.value.profileId ?: return
        if (
            !_state.value.canManageLinkedIp ||
            _state.value.removingDdns ||
            _state.value.content?.linkedIp?.ddnsHostname == null
        ) {
            return
        }

        _state.value = _state.value.copy(removingDdns = true, error = null)
        viewModelScope.launch {
            when (val result = repository.updateDdns(profileId, hostname = null)) {
                is ApiResult.Success -> {
                    if (_state.value.profileId != profileId) return@launch
                    _state.value = _state.value.copy(removingDdns = false)
                    load(profileId, force = true)
                }

                else -> {
                    if (_state.value.profileId != profileId) return@launch
                    _state.value = _state.value.copy(
                        removingDdns = false,
                        error = result,
                    )
                }
            }
        }
    }

    private fun canSubmitDdns(): Boolean =
        _state.value.canManageLinkedIp &&
            _state.value.ddnsDialog?.submitting == false

    private fun checkLinkedIp(
        profileId: String,
        capability: LinkIpCapability,
        linkedIp: String?,
        generation: Long,
    ) {
        viewModelScope.launch {
            val result = repository.getCurrentPublicIp(capability)
            if (!isCurrentRequest(profileId, generation)) return@launch
            _state.value = _state.value.copy(
                checkingLinkedIp = false,
                linkIpAvailable = when (result) {
                    is ApiResult.Success -> shouldShowLinkIp(result.value, linkedIp)
                    else -> true
                },
            )
        }
    }

    private fun isCurrentRequest(profileId: String, generation: Long): Boolean =
        _state.value.profileId == profileId && loadGeneration == generation

    override fun onCleared() {
        capability = null
        super.onCleared()
    }
}
