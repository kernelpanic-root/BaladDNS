package com.kernelpanic.baladdns.data.nextdns.settings

import com.google.gson.JsonElement
import com.google.gson.JsonObject

fun JsonObject.valueAt(path: List<String>): JsonElement? =
    path.fold(this as JsonElement?) { current, key ->
        current
            ?.takeIf(JsonElement::isJsonObject)
            ?.asJsonObject
            ?.get(key)
    }

fun nestedPayload(path: List<String>, value: Any): Map<String, Any> {
    require(path.isNotEmpty())
    return path.dropLast(1).asReversed().fold(
        initial = mapOf(path.last() to value),
    ) { child, parent -> mapOf(parent to child) }
}
