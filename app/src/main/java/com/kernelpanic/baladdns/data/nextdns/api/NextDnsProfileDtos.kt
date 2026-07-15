package com.kernelpanic.baladdns.data.nextdns.api

import com.google.gson.annotations.SerializedName

data class NextDnsProfilesResponse(
    @SerializedName("data") val data: List<NextDnsProfile>,
)

data class NextDnsProfile(
    @SerializedName("id") val id: String,
    @SerializedName("fingerprint") val fingerprint: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("name") val name: String,
)

data class NextDnsCreateProfileRequest(
    @SerializedName("name") val name: String,
    @SerializedName("security") val security: Map<String, Boolean> = mapOf(
        "threatIntelligenceFeeds" to true,
        "googleSafeBrowsing" to true,
        "cryptojacking" to true,
        "idnHomographs" to true,
        "typosquatting" to true,
        "dga" to true,
        "csam" to true,
    ),
    @SerializedName("privacy") val privacy: Map<String, Any> = mapOf(
        "blocklists" to listOf(mapOf("id" to "nextdns-recommended")),
        "disguisedTrackers" to true,
    ),
    @SerializedName("settings") val settings: Map<String, Map<String, Boolean>> = mapOf(
        "logs" to mapOf("enabled" to true),
        "performance" to mapOf("ecs" to true),
    ),
) {
    companion object {
        fun withName(name: String) = NextDnsCreateProfileRequest(name = name)
    }
}
