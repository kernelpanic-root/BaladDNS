package com.kernelpanic.baladdns.domain.nextdns

import java.io.IOException

sealed interface ApiResult<out T> {
    data class Success<T>(
        val value: T,
        val status: Int,
    ) : ApiResult<T>

    data class ServerFailure(
        val status: Int,
        val problems: List<ApiProblem>,
        val requestId: String? = null,
    ) : ApiResult<Nothing>

    data class NetworkFailure(
        val cause: IOException,
    ) : ApiResult<Nothing>

    data class SerializationFailure(
        val cause: Throwable,
    ) : ApiResult<Nothing>
}

data class ApiProblem(
    val code: String,
    val field: String? = null,
)