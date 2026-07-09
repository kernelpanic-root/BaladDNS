package com.eyalm.adns.data.nextdns.settings

import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.nextdns.api.NextDnsApi
import com.eyalm.adns.data.nextdns.api.NextDnsErrorParser
import com.eyalm.adns.data.nextdns.api.nextDnsApiCall
import com.eyalm.adns.data.nextdns.api.requestId
import com.eyalm.adns.data.nextdns.api.toEmptyApiResult
import com.eyalm.adns.data.nextdns.api.toServerFailure
import com.eyalm.adns.data.nextdns.api.toHexId
import com.eyalm.adns.domain.nextdns.ApiResult
import com.google.gson.JsonObject

sealed interface AddCustomDomainResult {
    data object Added : AddCustomDomainResult
    data object AlreadyExists : AddCustomDomainResult
    data class Failure(val result: ApiResult<*>) : AddCustomDomainResult
}

class NextDnsSettingsRepository(
    private val api: NextDnsApi = ApiClient.nextDnsApi,
) {
    suspend fun getScalarSettings(
        profileId: String,
        page: String,
    ): ApiResult<JsonObject> = nextDnsApiCall {
        val response = api.getPageSettings(profileId, page)
        if (!response.isSuccessful) return@nextDnsApiCall response.toServerFailure()

        val root = response.body()
            ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                IllegalStateException("Missing $page settings response body")
            )
        val problems = NextDnsErrorParser.parse(root)
        if (problems.isNotEmpty()) {
            return@nextDnsApiCall ApiResult.ServerFailure(
                status = response.code(),
                problems = problems,
                requestId = response.requestId(),
            )
        }
        val data = root.get("data")
        if (data == null || !data.isJsonObject) {
            ApiResult.SerializationFailure(
                IllegalStateException("Missing data object in $page settings response")
            )
        } else {
            ApiResult.Success(data.asJsonObject, response.code())
        }
    }

    suspend fun patchScalarSetting(
        profileId: String,
        binding: ApiBinding,
        encodedValue: Any,
    ): ApiResult<Unit> = nextDnsApiCall {
        api.patchPageSettings(
            profileId = profileId,
            page = binding.page,
            payload = nestedPayload(binding.path, encodedValue),
        ).toEmptyApiResult()
    }

    suspend fun addListItem(
        profileId: String,
        page: String,
        feature: String,
        itemId: String,
    ): ApiResult<Unit> = nextDnsApiCall {
        api.addListItem(profileId, page, feature, mapOf("id" to itemId))
            .toEmptyApiResult()
    }

    suspend fun removeListItem(
        profileId: String,
        page: String,
        feature: String,
        itemId: String,
    ): ApiResult<Unit> = nextDnsApiCall {
        api.removeListItem(profileId, page, feature, itemId.toHexId())
            .toEmptyApiResult()
    }

    suspend fun addCustomDomain(
        profileId: String,
        page: String,
        domain: String,
    ): AddCustomDomainResult = when (
        val result = nextDnsApiCall {
            api.addCustomItem(profileId, page, mapOf("id" to domain)).toEmptyApiResult()
        }
    ) {
        is ApiResult.Success -> AddCustomDomainResult.Added
        is ApiResult.ServerFailure -> if (result.problems.any { it.code == "duplicate" }) {
            AddCustomDomainResult.AlreadyExists
        } else {
            AddCustomDomainResult.Failure(result)
        }
        is ApiResult.NetworkFailure -> AddCustomDomainResult.Failure(result)
        is ApiResult.SerializationFailure -> AddCustomDomainResult.Failure(result)
    }

    suspend fun setCustomDomainActive(
        profileId: String,
        page: String,
        domain: String,
        active: Boolean,
    ): ApiResult<Unit> = nextDnsApiCall {
        api.patchCustomItem(
            profileId,
            page,
            domain.toHexId(),
            mapOf("active" to active),
        ).toEmptyApiResult()
    }

    suspend fun removeCustomDomain(
        profileId: String,
        page: String,
        domain: String,
    ): ApiResult<Unit> = nextDnsApiCall {
        api.removeCustomItem(profileId, page, domain.toHexId()).toEmptyApiResult()
    }
}
