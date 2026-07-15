package com.kernelpanic.baladdns.data.nextdns.model

fun countryFlag(code: String): String =
    code.uppercase().filter { it in 'A'..'Z' }
        .map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }
        .joinToString("")
