package com.eyalm.adns.data.provider

import java.net.IDN
import java.util.Locale

@JvmInline
value class PrivateDnsHostname private constructor(val ascii: String) {
    companion object {
        fun parse(value: String?): PrivateDnsHostname? = parse(
            value = value,
            preserveCase = false,
        )

        fun parsePreservingCase(value: String?): PrivateDnsHostname? = parse(
            value = value,
            preserveCase = true,
        )

        private fun parse(
            value: String?,
            preserveCase: Boolean,
        ): PrivateDnsHostname? {
            val candidate = value
                ?.trim()
                ?.trimEnd('.')
                ?.takeIf(String::isNotEmpty)
                ?: return null

            val ascii = try {
                IDN.toASCII(candidate, IDN.USE_STD3_ASCII_RULES)
                    .let { if (preserveCase) it else it.lowercase(Locale.ROOT) }
            } catch (_: IllegalArgumentException) {
                return null
            }

            if (ascii.length > 253) return null
            val labels = ascii.split('.')
            if (labels.size < 2) return null
            if (labels.any { label ->
                    label.isEmpty() ||
                        label.length > 63 ||
                        label.first() == '-' ||
                        label.last() == '-'
                }
            ) {
                return null
            }
            if (ascii.all { it == '.' || it.isDigit() }) return null

            return PrivateDnsHostname(ascii)
        }
    }
}
