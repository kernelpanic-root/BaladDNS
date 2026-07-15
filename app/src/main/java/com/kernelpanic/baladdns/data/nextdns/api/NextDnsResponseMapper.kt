package com.kernelpanic.baladdns.data.nextdns.api

import com.kernelpanic.baladdns.domain.nextdns.ApiResult
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import retrofit2.Response

fun Response<JsonObject>.toJsonApiResult(): ApiResult<JsonObject> {
    if (!isSuccessful) return toServerFailure()

    val root = body()
        ?: return ApiResult.SerializationFailure(
            IllegalStateException("Missing response body")
        )

    val problems = NextDnsErrorParser.parse(root)
    return if (problems.isNotEmpty()) {
        ApiResult.ServerFailure(code(), problems, requestId())
    } else {
        ApiResult.Success(root, code())
    }
}

fun <T : Any> Response<T>.toBodyApiResult(): ApiResult<T> {
    if (!isSuccessful) return toServerFailure()
    val value = body()
        ?: return ApiResult.SerializationFailure(
            IllegalStateException("Missing response body")
        )
    return ApiResult.Success(value, code())
}

fun Response<*>.toEmptyApiResult(): ApiResult<Unit> {
    if (!isSuccessful) return toServerFailure()

    val problems = (body() as? JsonObject)?.let(NextDnsErrorParser::parse).orEmpty()
    return if (problems.isNotEmpty()) {
        ApiResult.ServerFailure(code(), problems, requestId())
    } else {
        ApiResult.Success(Unit, code())
    }
}

internal fun Response<*>.toServerFailure(): ApiResult.ServerFailure {
    val body = errorBody()?.string()
    val root = body
        ?.takeIf(String::isNotBlank)
        ?.let { raw -> runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull() }
    return ApiResult.ServerFailure(
        status = code(),
        problems = root?.let(NextDnsErrorParser::parse).orEmpty(),
        requestId = requestId(),
    )
}

internal fun Response<*>.requestId(): String? =
    headers()["X-Request-Id"] ?: headers()["X-Request-ID"]
