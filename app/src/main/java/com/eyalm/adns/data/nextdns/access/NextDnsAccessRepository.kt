package com.eyalm.adns.data.nextdns.access

import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.nextdns.api.NextDnsApi
import com.eyalm.adns.data.nextdns.api.nextDnsApiCall
import com.eyalm.adns.data.nextdns.api.toEmptyApiResult
import com.eyalm.adns.data.nextdns.api.toJsonApiResult
import com.eyalm.adns.data.nextdns.api.toEncodedPathSegment
import com.eyalm.adns.domain.nextdns.ApiResult
import com.google.gson.Gson

class NextDnsAccessRepository(
    private val api: NextDnsApi = ApiClient.nextDnsApi,
) {
    private val gson = Gson()

    suspend fun get(profileId: String): ApiResult<List<AccessEntry>> = nextDnsApiCall {
        when (val result = api.getAccess(profileId).toJsonApiResult()) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing access data")
                    )
                ApiResult.Success(
                    gson.fromJson(data, Array<AccessEntry>::class.java).toList(),
                    result.status,
                )
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun invite(
        profileId: String,
        email: String,
        role: AccessRole,
    ): ApiResult<AccessEntry> = nextDnsApiCall {
        when (
            val result = api.inviteAccess(
                profileId,
                InviteAccessRequest(email, role),
            ).toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonObject("data")
                    ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing invited access entry")
                    )
                ApiResult.Success(gson.fromJson(data, AccessEntry::class.java), result.status)
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun updateRole(
        profileId: String,
        email: String,
        role: AccessRole,
    ): ApiResult<Unit> = nextDnsApiCall {
        api.updateAccessRole(
            profileId,
            email.toEncodedPathSegment(),
            UpdateAccessRoleRequest(role),
        )
            .toEmptyApiResult()
    }

    suspend fun delete(profileId: String, email: String): ApiResult<Unit> =
        nextDnsApiCall {
            api.deleteAccess(profileId, email.toEncodedPathSegment()).toEmptyApiResult()
        }
}
