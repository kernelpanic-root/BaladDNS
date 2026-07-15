package com.kernelpanic.baladdns.data.nextdns.recreation

import com.kernelpanic.baladdns.data.network.ApiClient
import com.kernelpanic.baladdns.data.nextdns.api.NextDnsApi
import com.kernelpanic.baladdns.data.nextdns.api.nextDnsApiCall
import com.kernelpanic.baladdns.data.nextdns.api.toEmptyApiResult
import com.kernelpanic.baladdns.data.nextdns.api.toJsonApiResult
import com.kernelpanic.baladdns.data.nextdns.api.toHexId
import com.kernelpanic.baladdns.domain.nextdns.ApiResult
import com.google.gson.Gson

class NextDnsRecreationRepository(
    private val api: NextDnsApi = ApiClient.nextDnsApi,
) {
    private val gson = Gson()

    suspend fun get(profileId: String): ApiResult<ParentalRecreationState> =
        nextDnsApiCall {
            when (val result = api.getParentalControl(profileId).toJsonApiResult()) {
                is ApiResult.Success -> {
                    val data = result.value.getAsJsonObject("data")
                        ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                            IllegalStateException("Missing parental control data")
                        )
                    ApiResult.Success(
                        gson.fromJson(data, ParentalRecreationState::class.java),
                        result.status,
                    )
                }

                is ApiResult.ServerFailure -> result
                is ApiResult.NetworkFailure -> result
                is ApiResult.SerializationFailure -> result
            }
        }

    suspend fun updateSchedule(
        profileId: String,
        schedule: RecreationScheduleDto,
    ): ApiResult<Unit> = nextDnsApiCall {
        api.updateRecreationSchedule(
            profileId,
            UpdateRecreationScheduleRequest(schedule),
        ).toEmptyApiResult()
    }

    suspend fun updateItem(
        profileId: String,
        collection: RecreationItemCollection,
        itemId: String,
        recreation: Boolean,
    ): ApiResult<Unit> = nextDnsApiCall {
        api.updateRecreationItem(
            profileId = profileId,
            collection = collection.wireName,
            hexId = itemId.toHexId(),
            request = UpdateRecreationItemRequest(recreation),
        ).toEmptyApiResult()
    }
}
