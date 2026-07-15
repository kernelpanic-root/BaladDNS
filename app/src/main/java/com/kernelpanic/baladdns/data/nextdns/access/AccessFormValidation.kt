package com.kernelpanic.baladdns.data.nextdns.access

import android.util.Patterns
import com.kernelpanic.baladdns.domain.nextdns.ApiProblem

enum class AccessField {
    Email,
}

enum class AccessError {
    Required,
    Invalid,
    Duplicate,
}

object AccessFormValidation {
    fun localErrors(email: String): Map<AccessField, AccessError> = buildMap {
        when {
            email.isBlank() -> put(AccessField.Email, AccessError.Required)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> put(AccessField.Email, AccessError.Invalid)
        }
    }

    fun serverErrors(problems: List<ApiProblem>): Map<AccessField, AccessError> = buildMap {
        problems.forEach { problem ->
            val field = problem.field?.trim('/')?.substringAfterLast('/')
            if (field == "email" || field == null) {
                when (problem.code) {
                    "required" -> put(AccessField.Email, AccessError.Required)
                    "invalid" -> put(AccessField.Email, AccessError.Invalid)
                    "duplicate", "conflict", "taken" -> put(AccessField.Email, AccessError.Duplicate)
                }
            }
        }
    }
}
