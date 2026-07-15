package com.kernelpanic.baladdns.data.nextdns.setup

import android.util.Patterns

sealed interface DdnsValidationError {
    data object Required : DdnsValidationError
    data object Invalid : DdnsValidationError
    data object IpAddress : DdnsValidationError
    data class Server(val code: String) : DdnsValidationError
    data object Request : DdnsValidationError
}

fun validateDdnsHostname(value: String): DdnsValidationError? {
    if (value.isBlank()) return DdnsValidationError.Required
    if (Patterns.IP_ADDRESS.matcher(value).matches()) return DdnsValidationError.IpAddress
    if (!Patterns.DOMAIN_NAME.matcher(value).matches()) return DdnsValidationError.Invalid
    return null
}
