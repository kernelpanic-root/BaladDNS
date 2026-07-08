package com.eyalm.adns.data.nextdns.rewrites

import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.nextdns.api.NextDnsApi
import com.eyalm.adns.data.nextdns.api.nextDnsApiCall
import com.eyalm.adns.data.nextdns.api.toEmptyApiResult
import com.eyalm.adns.data.nextdns.api.toJsonApiResult
import com.eyalm.adns.domain.nextdns.ApiResult
import com.google.gson.Gson

class NextDnsRewritesRepository(
    private val api: NextDnsApi = ApiClient.nextDnsApi,
) {
    private val gson = Gson()

    suspend fun get(profileId: String): ApiResult<List<Rewrite>> = nextDnsApiCall {
        when (val result = api.getRewrites(profileId).toJsonApiResult()) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing rewrite data")
                    )
                ApiResult.Success(
                    gson.fromJson(data, Array<Rewrite>::class.java).toList(),
                    result.status,
                )
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun create(
        profileId: String,
        name: String,
        content: String,
    ): ApiResult<Rewrite> = nextDnsApiCall {
        when (
            val result = api.createRewrite(
                profileId,
                CreateRewriteRequest(name, content),
            ).toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonObject("data")
                    ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing rewrite data")
                    )
                ApiResult.Success(gson.fromJson(data, Rewrite::class.java), result.status)
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun delete(profileId: String, rewriteId: String): ApiResult<Unit> =
        nextDnsApiCall {
            api.deleteRewrite(profileId, rewriteId).toEmptyApiResult()
        }
}
