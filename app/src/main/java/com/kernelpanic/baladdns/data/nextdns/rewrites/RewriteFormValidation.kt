package com.kernelpanic.baladdns.data.nextdns.rewrites

import com.kernelpanic.baladdns.domain.nextdns.ApiProblem

enum class RewriteField {
    Name,
    Content,
}

enum class RewriteError {
    Required,
    Ip,
    Invalid,
    Taken,
}

object RewriteFormValidation {
    fun localErrors(name: String, content: String): Map<RewriteField, RewriteError> {
        return buildMap {
            when {
                name.isBlank() -> put(RewriteField.Name, RewriteError.Required)
                isIpLiteral(name) -> put(RewriteField.Name, RewriteError.Ip)
                !isDomainLike(name) -> put(RewriteField.Name, RewriteError.Invalid)
            }

            when {
                content.isBlank() -> put(RewriteField.Content, RewriteError.Required)
                !isAnswerLike(content) -> put(RewriteField.Content, RewriteError.Invalid)
            }
        }
    }

    fun serverErrors(problems: List<ApiProblem>): Map<RewriteField, RewriteError> {
        return buildMap {
            problems.forEach { problem ->
                when (problem.field?.trim('/')?.substringAfterLast('/')) {
                    "name" -> when (problem.code) {
                        "required" -> put(RewriteField.Name, RewriteError.Required)
                        "ip" -> put(RewriteField.Name, RewriteError.Ip)
                        "invalid" -> put(RewriteField.Name, RewriteError.Invalid)
                        "conflict", "taken" -> put(RewriteField.Name, RewriteError.Taken)
                    }

                    "content" -> when (problem.code) {
                        "required" -> put(RewriteField.Content, RewriteError.Required)
                        "invalid" -> put(RewriteField.Content, RewriteError.Invalid)
                    }

                    null -> when (problem.code) {
                        "conflict", "taken", "duplicate" ->
                            put(RewriteField.Name, RewriteError.Taken)
                    }
                }
            }
        }
    }

    private fun isAnswerLike(value: String): Boolean =
        isIpv4(value) || isIpv6Like(value) || isDomainLike(value)

    private fun isIpLiteral(value: String): Boolean =
        isIpv4(value) || isIpv6Like(value)

    private fun isIpv4(value: String): Boolean {
        val parts = value.split('.')
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() && part.all(Char::isDigit) && part.toIntOrNull() in 0..255
        }
    }

    private fun isIpv6Like(value: String): Boolean =
        value.count { it == ':' } >= 2 && value.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it == ':' || it == '.' }

    private fun isDomainLike(value: String): Boolean {
        if (value.isBlank() || value.contains("://") || value.any(Char::isWhitespace)) return false
        if ('/' in value || '\\' in value || ':' in value) return false

        return value.split('.').all { label ->
            label.isNotEmpty() &&
                label.length <= 63 &&
                !label.startsWith('-') &&
                !label.endsWith('-') &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }
    }
}
