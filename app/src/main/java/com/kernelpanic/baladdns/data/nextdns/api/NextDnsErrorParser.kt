package com.kernelpanic.baladdns.data.nextdns.api

import com.kernelpanic.baladdns.domain.nextdns.ApiProblem
import com.google.gson.JsonElement
import com.google.gson.JsonObject

object NextDnsErrorParser {

    fun parse(root: JsonObject): List<ApiProblem> {
        val errors = root.get("errors") ?: return emptyList()
        return when {
            errors.isJsonArray -> errors.asJsonArray.mapNotNull { element ->
                val item = element.takeIf { it.isJsonObject }?.asJsonObject
                    ?: return@mapNotNull null

                val code = item.stringOrNull("code")
                    ?: return@mapNotNull null

                val field = item.getAsJsonObject("source")
                    ?.stringOrNull("pointer")

                ApiProblem(code = code, field = field)
            }

            errors.isJsonObject -> errors.asJsonObject.entrySet().mapNotNull {
                val code = it.value
                    .takeIf(JsonElement::isJsonPrimitive)
                    ?.asString

                code?.let { value ->
                    ApiProblem(code = value, field = it.key)
                }
            }

            else -> emptyList()
        }
    }

    private fun JsonObject.stringOrNull(name: String): String? =
        get(name)
            ?.takeIf(JsonElement::isJsonPrimitive)
            ?.asString

}