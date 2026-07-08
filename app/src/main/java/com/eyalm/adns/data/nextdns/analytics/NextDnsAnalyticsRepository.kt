package com.eyalm.adns.data.nextdns.analytics

import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.nextdns.api.NextDnsApi
import com.eyalm.adns.data.nextdns.api.NextDnsDeviceItem
import com.eyalm.adns.data.nextdns.api.NextDnsStatsGraphResponse
import com.eyalm.adns.data.nextdns.api.nextDnsApiCall
import com.eyalm.adns.data.nextdns.api.toBodyApiResult
import com.eyalm.adns.data.nextdns.api.toJsonApiResult
import com.eyalm.adns.domain.nextdns.ApiResult
import com.google.gson.JsonArray
import java.util.TimeZone

class NextDnsAnalyticsRepository(
    private val api: NextDnsApi = ApiClient.nextDnsApi,
) {
    suspend fun getGraph(
        profileId: String,
        scope: AnalyticsScope,
    ): ApiResult<NextDnsStatsGraphResponse> = nextDnsApiCall {
        api.getStatsGraph(
            profileId = profileId,
            period = scope.period.wireValue,
            alignment = "start",
            timezone = TimeZone.getDefault().id,
            device = scope.deviceId,
        ).toBodyApiResult()
    }

    suspend fun getCardData(
        profileId: String,
        feature: String,
        baseParams: Map<String, String>,
        scope: AnalyticsScope,
    ): ApiResult<JsonArray> = nextDnsApiCall {
        val params = buildMap {
            putAll(baseParams)
            put("from", scope.period.wireValue)
            scope.deviceId?.let { put("device", it) }
        }
        when (
            val result = api
                .getAnalyticsFeature(profileId, feature, params)
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing analytics data")
                    )
                ApiResult.Success(data, result.status)
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun getDevices(profileId: String): ApiResult<List<NextDnsDeviceItem>> = nextDnsApiCall {
        when (
            val result = api
                .getDevices(profileId)
                .toBodyApiResult()
        ) {
            is ApiResult.Success -> ApiResult.Success(result.value.data, result.status)
            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

}
