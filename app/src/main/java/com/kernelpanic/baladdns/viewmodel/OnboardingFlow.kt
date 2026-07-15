package com.kernelpanic.baladdns.viewmodel

import com.kernelpanic.baladdns.data.activation.ActivationMode
import com.kernelpanic.baladdns.data.provider.DnsProviderCatalog
import com.kernelpanic.baladdns.data.provider.DnsProviderSelection
import com.kernelpanic.baladdns.data.provider.PrivateDnsHostname
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OnboardingStep {
    Welcome,
    Provider,
    Preset,
    ProviderLogin,
    ActivationMode,
    ActivationMethod,
    Adb,
    Shizuku,
    Success,
}

enum class PrivilegedMethod {
    Adb,
    Shizuku,
}

data class OnboardingDraft(
    val providerSelection: DnsProviderSelection? = null,
    val mode: ActivationMode? = null,
    val privilegedMethod: PrivilegedMethod? = null,
    val nextDnsProfileId: String? = null,
)

data class OnboardingFlowState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val draft: OnboardingDraft = OnboardingDraft(),
) {
    val controlOnlyEligible: Boolean
        get() = draft.providerSelection is DnsProviderSelection.Enhanced &&
            draft.nextDnsProfileId != null
}

sealed interface OnboardingIntent {
    data object Continue : OnboardingIntent
    data class ProviderSelected(val selection: DnsProviderSelection) : OnboardingIntent
    data class PresetSelected(val selection: DnsProviderSelection.Standard) : OnboardingIntent
    data class ProviderLoginCompleted(val profileId: String) : OnboardingIntent
    data class ModeSelected(val mode: ActivationMode) : OnboardingIntent
    data class ActivationMethodSelected(val method: PrivilegedMethod) : OnboardingIntent
    data object ActivationGranted : OnboardingIntent
    data object Back : OnboardingIntent
}

class OnboardingFlow(
    private val catalog: DnsProviderCatalog,
    initialState: OnboardingFlowState = OnboardingFlowState(),
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<OnboardingFlowState> = _state.asStateFlow()

    fun dispatch(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.Continue -> continueFromWelcome()
            is OnboardingIntent.ProviderSelected -> selectProvider(intent.selection)
            is OnboardingIntent.PresetSelected -> selectPreset(intent.selection)
            is OnboardingIntent.ProviderLoginCompleted -> completeProviderLogin(intent.profileId)
            is OnboardingIntent.ModeSelected -> selectMode(intent.mode)
            is OnboardingIntent.ActivationMethodSelected -> selectActivationMethod(intent.method)
            OnboardingIntent.ActivationGranted -> activationGranted()
            OnboardingIntent.Back -> goBack()
        }
    }

    private fun continueFromWelcome() {
        if (_state.value.step == OnboardingStep.Welcome) {
            _state.value = _state.value.copy(step = OnboardingStep.Provider)
        }
    }

    private fun selectProvider(selection: DnsProviderSelection) {
        if (_state.value.step != OnboardingStep.Provider) return
        when (selection) {
            is DnsProviderSelection.Enhanced -> {
                if (catalog.provider(selection) == null) return
                _state.value = OnboardingFlowState(
                    step = OnboardingStep.ProviderLogin,
                    draft = OnboardingDraft(providerSelection = selection),
                )
            }

            is DnsProviderSelection.Standard -> {
                val provider = catalog.standardProvider(selection.providerId) ?: return
                if (provider.presets.none { it.id == selection.presetId }) return
                _state.value = OnboardingFlowState(
                    step = if (provider.presets.size > 1) {
                        OnboardingStep.Preset
                    } else {
                        OnboardingStep.ActivationMethod
                    },
                    draft = OnboardingDraft(
                        providerSelection = selection,
                        mode = ActivationMode.PrivilegedDnsControl,
                    ),
                )
            }

            is DnsProviderSelection.Custom -> {
                val hostname = PrivateDnsHostname.parse(selection.hostname)?.ascii ?: return
                _state.value = OnboardingFlowState(
                    step = OnboardingStep.ActivationMethod,
                    draft = OnboardingDraft(
                        providerSelection = DnsProviderSelection.Custom(hostname),
                        mode = ActivationMode.PrivilegedDnsControl,
                    ),
                )
            }
        }
    }

    private fun selectPreset(selection: DnsProviderSelection.Standard) {
        if (_state.value.step != OnboardingStep.Preset) return
        val draftSelection = _state.value.draft.providerSelection
            as? DnsProviderSelection.Standard
            ?: return
        if (draftSelection.providerId != selection.providerId) return
        val provider = catalog.standardProvider(selection.providerId) ?: return
        if (provider.presets.none { it.id == selection.presetId }) return
        _state.value = _state.value.copy(
            step = OnboardingStep.ActivationMethod,
            draft = _state.value.draft.copy(providerSelection = selection),
        )
    }

    private fun completeProviderLogin(profileId: String) {
        if (
            _state.value.step != OnboardingStep.ProviderLogin ||
            _state.value.draft.providerSelection !is DnsProviderSelection.Enhanced ||
            profileId.isBlank()
        ) {
            return
        }
        _state.value = _state.value.copy(
            step = OnboardingStep.ActivationMode,
            draft = _state.value.draft.copy(nextDnsProfileId = profileId),
        )
    }

    private fun selectMode(mode: ActivationMode) {
        if (_state.value.step != OnboardingStep.ActivationMode) return
        if (mode == ActivationMode.NextDnsControlOnly && !_state.value.controlOnlyEligible) return
        _state.value = _state.value.copy(
            step = if (mode == ActivationMode.NextDnsControlOnly) {
                OnboardingStep.Success
            } else {
                OnboardingStep.ActivationMethod
            },
            draft = _state.value.draft.copy(mode = mode),
        )
    }

    private fun selectActivationMethod(method: PrivilegedMethod) {
        if (
            _state.value.step != OnboardingStep.ActivationMethod ||
            _state.value.draft.mode != ActivationMode.PrivilegedDnsControl
        ) {
            return
        }
        _state.value = _state.value.copy(
            step = when (method) {
                PrivilegedMethod.Adb -> OnboardingStep.Adb
                PrivilegedMethod.Shizuku -> OnboardingStep.Shizuku
            },
            draft = _state.value.draft.copy(privilegedMethod = method),
        )
    }

    private fun activationGranted() {
        if (
            _state.value.step == OnboardingStep.Adb ||
            _state.value.step == OnboardingStep.Shizuku
        ) {
            _state.value = _state.value.copy(step = OnboardingStep.Success)
        }
    }

    private fun goBack() {
        val current = _state.value
        val previous = when (current.step) {
            OnboardingStep.Welcome -> OnboardingStep.Welcome
            OnboardingStep.Provider -> OnboardingStep.Welcome
            OnboardingStep.Preset -> OnboardingStep.Provider
            OnboardingStep.ProviderLogin -> OnboardingStep.Provider
            OnboardingStep.ActivationMode -> OnboardingStep.ProviderLogin
            OnboardingStep.ActivationMethod -> previousBeforeActivation(current.draft)
            OnboardingStep.Adb,
            OnboardingStep.Shizuku,
            -> OnboardingStep.ActivationMethod

            OnboardingStep.Success -> if (
                current.draft.mode == ActivationMode.NextDnsControlOnly
            ) {
                OnboardingStep.ActivationMode
            } else {
                OnboardingStep.ActivationMethod
            }
        }
        _state.value = current.copy(step = previous)
    }

    private fun previousBeforeActivation(draft: OnboardingDraft): OnboardingStep {
        if (draft.providerSelection is DnsProviderSelection.Enhanced) {
            return OnboardingStep.ActivationMode
        }
        val standard = draft.providerSelection as? DnsProviderSelection.Standard
        val hasPresetChoice = standard
            ?.let { catalog.standardProvider(it.providerId) }
            ?.presets
            ?.size
            ?.let { it > 1 }
            ?: false
        return if (hasPresetChoice) OnboardingStep.Preset else OnboardingStep.Provider
    }
}
