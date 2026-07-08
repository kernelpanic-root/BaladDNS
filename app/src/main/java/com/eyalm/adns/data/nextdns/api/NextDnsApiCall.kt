package com.eyalm.adns.data.nextdns.api

import com.eyalm.adns.domain.nextdns.ApiResult
import java.io.IOException
import kotlinx.coroutines.CancellationException

internal suspend fun <T> nextDnsApiCall(
    block: suspend () -> ApiResult<T>,
): ApiResult<T> = try {
    block()
} catch (error: CancellationException) {
    throw error
} catch (error: IOException) {
    ApiResult.NetworkFailure(error)
} catch (error: Exception) {
    ApiResult.SerializationFailure(error)
}
