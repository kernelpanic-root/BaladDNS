package com.eyalm.adns.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyalm.adns.ProviderLoginActivity
import com.eyalm.adns.data.nextdns.api.NextDnsProfile
import com.eyalm.adns.data.nextdns.auth.NextDnsAuthRepository
import com.eyalm.adns.data.nextdns.auth.NextDnsLoginFailure
import com.eyalm.adns.data.nextdns.auth.NextDnsLoginField
import com.eyalm.adns.data.nextdns.auth.NextDnsLoginMode
import com.eyalm.adns.data.nextdns.auth.NextDnsLoginOutcome
import com.eyalm.adns.data.nextdns.auth.NextDnsLoginUiState
import com.eyalm.adns.data.nextdns.auth.fieldErrors
import com.eyalm.adns.data.nextdns.auth.isValidTwoFactorCode
import com.eyalm.adns.data.nextdns.profile.NextDnsProfileRepository
import com.eyalm.adns.domain.nextdns.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProviderLoginViewModel(application: Application) : AndroidViewModel(application) {
    private val profileRepository = NextDnsProfileRepository(application)
    private val authRepository = NextDnsAuthRepository(application)

    var currentStep by mutableStateOf(ProviderLoginActivity.Step.LOGIN)
        private set

    var profiles by mutableStateOf(emptyList<NextDnsProfile>())
        private set

    private val _state = MutableStateFlow(NextDnsLoginUiState())
    val state = _state.asStateFlow()

    fun submit() {
        val current = _state.value
        if (current.submitting) return

        val validation = validate(current)
        if (validation.isNotEmpty()) {
            _state.value = current.copy(
                fieldErrors = validation,
                generalError = null,
            )
            return
        }

        viewModelScope.launch {
            _state.value = current.copy(
                submitting = true,
                fieldErrors = emptyMap(),
                generalError = null,
            )

            when (val result = authRepository.login(current)) {
                NextDnsLoginOutcome.RequiresTwoFactor -> {
                    _state.update {
                        it.copy(
                            requiresTwoFactor = true,
                            code = "",
                            submitting = false,
                        )
                    }
                }

                is NextDnsLoginOutcome.Authenticated -> {
                    profiles = result.profiles
                    clearSecrets()
                    currentStep = ProviderLoginActivity.Step.PROFILE
                }

                is NextDnsLoginOutcome.Failure -> {
                    _state.update { state ->
                        val fieldErrors = result.fieldErrors(state.mode)
                        state.copy(
                            submitting = false,
                            fieldErrors = fieldErrors,
                            generalError = result.reason.takeIf { fieldErrors.isEmpty() },
                        )
                    }
                }
            }
        }
    }

    internal fun validate(
        state: NextDnsLoginUiState,
    ): Map<NextDnsLoginField, NextDnsLoginFailure> = buildMap {
        when (state.mode) {
            NextDnsLoginMode.Password -> {
                when {
                    state.email.isBlank() -> put(
                        NextDnsLoginField.Email,
                        NextDnsLoginFailure.Required,
                    )

                    !Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches() -> put(
                        NextDnsLoginField.Email,
                        NextDnsLoginFailure.InvalidEmail,
                    )
                }
                if (state.password.isBlank()) {
                    put(NextDnsLoginField.Password, NextDnsLoginFailure.Required)
                }
                if (
                    state.requiresTwoFactor &&
                    !isValidTwoFactorCode(state.code)
                ) {
                    put(
                        NextDnsLoginField.Code,
                        if (state.code.isBlank()) {
                            NextDnsLoginFailure.Required
                        } else {
                            NextDnsLoginFailure.InvalidTwoFactorFormat
                        }
                    )
                }
            }

            NextDnsLoginMode.ApiKey -> {
                if (state.apiKey.isBlank()) {
                    put(NextDnsLoginField.ApiKey, NextDnsLoginFailure.Required)
                }
            }
        }
    }

    fun setMode(mode: NextDnsLoginMode) {
        if (_state.value.submitting || _state.value.mode == mode) return
        authRepository.discardPendingKey()
        _state.update {
            it.copy(
                mode = mode,
                requiresTwoFactor = false,
                code = "",
                fieldErrors = emptyMap(),
                generalError = null,
            )
        }
    }

    fun onEmailChanged(value: String) {
        authRepository.discardPendingKey()
        updateField(NextDnsLoginField.Email) { copy(email = value) }
    }

    fun onPasswordChanged(value: String) {
        authRepository.discardPendingKey()
        updateField(NextDnsLoginField.Password) { copy(password = value) }
    }

    fun onCodeChanged(value: String) {
        updateField(NextDnsLoginField.Code) {
            copy(code = value.filter(Char::isDigit).take(6))
        }
    }

    fun onApiKeyChanged(value: String) {
        updateField(NextDnsLoginField.ApiKey) { copy(apiKey = value) }
    }

    private fun updateField(
        field: NextDnsLoginField,
        update: NextDnsLoginUiState.() -> NextDnsLoginUiState,
    ) {
        if (_state.value.submitting) return
        _state.update { current ->
            val fieldsToClear = if (
                field == NextDnsLoginField.Email || field == NextDnsLoginField.Password
            ) {
                setOf(NextDnsLoginField.Email, NextDnsLoginField.Password)
            } else {
                setOf(field)
            }
            current.update().copy(
                fieldErrors = current.fieldErrors - fieldsToClear,
                generalError = null,
            )
        }
    }

    fun setProfile(profile: NextDnsProfile) {
        profileRepository.selectProfile(profile, "ADNS")
        currentStep = ProviderLoginActivity.Step.SUCCESS
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            when (val result = profileRepository.createProfile(name)) {
                is ApiResult.Success -> profiles = profiles + result.value
                else -> Unit
            }
        }
    }

    private fun clearSecrets() {
        _state.value = NextDnsLoginUiState()
        authRepository.discardPendingKey()
    }

    override fun onCleared() {
        clearSecrets()
        super.onCleared()
    }
}
