package com.kernelpanic.baladdns.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ProviderLoginStep {
    Login,
    Profile,
    Success,
}

data class ProviderLoginFlowState(
    val step: ProviderLoginStep = ProviderLoginStep.Login,
    val selectedProfileId: String? = null,
)

class ProviderLoginFlow {
    private val _state = MutableStateFlow(ProviderLoginFlowState())
    val state: StateFlow<ProviderLoginFlowState> = _state.asStateFlow()

    fun authenticated() {
        _state.value = ProviderLoginFlowState(step = ProviderLoginStep.Profile)
    }

    fun profileSelected(profileId: String) {
        _state.value = ProviderLoginFlowState(
            step = ProviderLoginStep.Success,
            selectedProfileId = profileId,
        )
    }

    fun back() {
        _state.value = when (_state.value.step) {
            ProviderLoginStep.Login -> _state.value
            ProviderLoginStep.Profile -> ProviderLoginFlowState(ProviderLoginStep.Login)
            ProviderLoginStep.Success -> _state.value.copy(step = ProviderLoginStep.Profile)
        }
    }
}

sealed interface ProviderLoginResult {
    data class Completed(
        val providerId: String,
        val profileId: String,
    ) : ProviderLoginResult

    data object Cancelled : ProviderLoginResult
}
