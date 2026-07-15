package com.kernelpanic.baladdns.data.nextdns.setup

data class SetupContent(
    val profileId: String,
    val dnsOverTls: String,
    val dnsOverHttps: String,
    val ipv4: List<String>,
    val ipv6: List<String>,
    val dnscryptStamp: String?,
    val linkedIp: LinkedIpContent,
)

data class LinkedIpContent(
    val servers: List<String>,
    val address: String?,
    val ddnsHostname: String?,
)


internal class LinkIpCapability(
    private val profileId: String,
    private val updateToken: String,
) {
    fun requestParts(): Pair<String, String> = profileId to updateToken

    fun programmaticUrl(): String =
        "https://link-ip.nextdns.io/$profileId/$updateToken"

    override fun toString(): String = "LinkIpCapability(redacted)"
}

internal data class SetupLoad(
    val content: SetupContent,
    val linkIpCapability: LinkIpCapability?,
)

internal fun shouldShowLinkIp(currentPublicIp: String, linkedIp: String?): Boolean =
    !currentPublicIp.equals(linkedIp, ignoreCase = true)
