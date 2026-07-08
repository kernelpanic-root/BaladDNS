package com.eyalm.adns.data.nextdns.model

import android.util.Patterns
import com.eyalm.adns.data.nextdns.api.toHexId
import java.net.Inet6Address
import java.net.InetAddress
import java.util.Locale

fun nextDnsFaviconUrl(hostname: String): String? = buildNextDnsFaviconUrl(
    hostname = hostname,
    isValidDomain = { Patterns.DOMAIN_NAME.matcher(it).matches() },
)

internal fun buildNextDnsFaviconUrl(
    hostname: String,
    isValidDomain: (String) -> Boolean,
): String? {
    val normalized = hostname
        .trim()
        .trimEnd('.')
        .lowercase(Locale.ROOT)

    if (!isValidDomain(normalized)) return null
    if (normalized.isNumericIpAddress()) return null

    return "https://favicons.nextdns.io/${normalized.toHexId()}@3x.png"
}

private fun String.isNumericIpAddress(): Boolean {
    val ipv4Parts = split('.')
    if (
        ipv4Parts.size == 4 &&
        ipv4Parts.all { part ->
            part.isNotEmpty() &&
                part.all(Char::isDigit) &&
                part.toIntOrNull() in 0..255
        }
    ) {
        return true
    }
    if (':' !in this) return false
    return runCatching { InetAddress.getByName(this) is Inet6Address }.getOrDefault(false)
}
