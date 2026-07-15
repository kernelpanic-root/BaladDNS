package com.kernelpanic.baladdns.data.nextdns.api

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun String.toHexId(): String {
    val hex = toByteArray(Charsets.UTF_8).joinToString("") { byte ->
        "%02x".format(byte)
    }
    return "hex:$hex"
}

fun String.toEncodedPathSegment(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.name())
        .replace("+", "%20")
