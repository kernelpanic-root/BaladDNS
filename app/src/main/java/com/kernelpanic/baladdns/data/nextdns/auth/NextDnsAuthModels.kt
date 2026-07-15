package com.kernelpanic.baladdns.data.nextdns.auth

import com.kernelpanic.baladdns.data.nextdns.api.NextDnsProfile

enum class NextDnsLoginMode {
    Password,
    ApiKey,
}

enum class NextDnsLoginField {
    Email,
    Password,
    Code,
    ApiKey,
}

sealed interface NextDnsLoginFailure {
    data object Required : NextDnsLoginFailure
    data object InvalidEmail : NextDnsLoginFailure
    data object InvalidCredentials : NextDnsLoginFailure
    data object InvalidTwoFactorCode : NextDnsLoginFailure
    data object InvalidTwoFactorFormat : NextDnsLoginFailure
    data object InvalidApiKey : NextDnsLoginFailure
    data object Offline : NextDnsLoginFailure
    data object ServerUnavailable : NextDnsLoginFailure
    data object Unknown : NextDnsLoginFailure
}

data class NextDnsLoginUiState(
    val mode: NextDnsLoginMode = NextDnsLoginMode.Password,
    val email: String = "",
    val password: String = "",
    val code: String = "",
    val apiKey: String = "",
    val requiresTwoFactor: Boolean = false,
    val submitting: Boolean = false,
    val fieldErrors: Map<NextDnsLoginField, NextDnsLoginFailure> = emptyMap(),
    val generalError: NextDnsLoginFailure? = null,
)

sealed interface NextDnsLoginOutcome {
    data class Authenticated(
        val profiles: List<NextDnsProfile>,
    ) : NextDnsLoginOutcome

    data object RequiresTwoFactor : NextDnsLoginOutcome

    data class Failure(
        val reason: NextDnsLoginFailure,
        val field: NextDnsLoginField? = null,
    ) : NextDnsLoginOutcome
}

internal fun NextDnsLoginOutcome.Failure.fieldErrors(
    mode: NextDnsLoginMode,
): Map<NextDnsLoginField, NextDnsLoginFailure> = when {
    mode == NextDnsLoginMode.Password && reason == NextDnsLoginFailure.InvalidCredentials ->
        mapOf(
            NextDnsLoginField.Email to reason,
            NextDnsLoginField.Password to reason,
        )

    field != null -> mapOf(field to reason)
    else -> emptyMap()
}

internal fun isValidTwoFactorCode(code: String): Boolean =
    code.length == 6 && code.all(Char::isDigit)
