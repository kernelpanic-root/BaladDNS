package com.eyalm.adns.data

sealed interface PrivateDnsObservation {
    data object Off : PrivateDnsObservation
    data object Automatic : PrivateDnsObservation
    data object PermissionMissing : PrivateDnsObservation
    data class Hostname(val value: String) : PrivateDnsObservation
}

fun isSelectedPrivateDnsActive(
    observation: PrivateDnsObservation,
    selectedHostname: String?,
): Boolean =
    observation is PrivateDnsObservation.Hostname &&
        selectedHostname != null &&
        observation.value.equals(selectedHostname, ignoreCase = true)
