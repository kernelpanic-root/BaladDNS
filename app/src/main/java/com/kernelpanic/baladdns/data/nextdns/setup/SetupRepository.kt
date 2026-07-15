package com.kernelpanic.baladdns.data.nextdns.setup

import android.content.Context
import com.kernelpanic.baladdns.data.TokenManager
import com.kernelpanic.baladdns.data.network.ApiClient
import com.kernelpanic.baladdns.data.nextdns.api.NextDnsApi
import com.kernelpanic.baladdns.data.nextdns.api.NextDnsIpv4Api
import com.kernelpanic.baladdns.data.nextdns.api.NextDnsLinkIpApi
import com.kernelpanic.baladdns.data.nextdns.api.toJsonApiResult
import com.kernelpanic.baladdns.data.nextdns.api.toServerFailure
import com.kernelpanic.baladdns.domain.nextdns.ApiResult
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.InetAddress

class SetupRepository(
    context: Context,
    private val api: NextDnsApi = ApiClient.nextDnsApi,
    private val linkIpApi: NextDnsLinkIpApi = ApiClient.nextDnsLinkIpApi,
    private val ipv4Api: NextDnsIpv4Api = ApiClient.nextDnsIpv4Api,
    private val tokenManager: TokenManager = TokenManager.getInstance(context),
) {

    internal suspend fun getSetup(profileId: String): ApiResult<SetupLoad> = safeCall {
        when (val result = api.getSetup(profileId).toJsonApiResult()) {
            is ApiResult.Success -> {
                try {
                    val load = result.value.toSetupLoad(profileId)
                    ApiResult.Success(load, result.status)
                } catch (error: Exception) {
                    ApiResult.SerializationFailure(error)
                }
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    internal suspend fun linkCurrentIp(capability: LinkIpCapability): ApiResult<String> = safeCall {
        val (profileId, updateToken) = capability.requestParts()
        linkIpApi.linkCurrentIp(profileId, updateToken).toIpApiResult()
    }

    internal suspend fun getCurrentPublicIp(capability: LinkIpCapability): ApiResult<String> = safeCall {
        val (profileId, updateToken) = capability.requestParts()
        linkIpApi.getCurrentIp(profileId, updateToken).toIpApiResult()
    }

    internal suspend fun updateDdns(
        profileId: String,
        hostname: String?,
    ): ApiResult<Unit> = safeCall {
        when (val result = ipv4Api.updateLinkedIp(profileId, ddnsRequestBody(hostname)).toJsonApiResult()) {
            is ApiResult.Success -> ApiResult.Success(Unit, result.status)
            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    private suspend fun <T> safeCall(
        block: suspend () -> ApiResult<T>,
    ): ApiResult<T> = try {
        if (!tokenManager.hasToken()) {
            ApiResult.SerializationFailure(IllegalStateException("Not logged in"))
        } else {
            block()
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: IOException) {
        ApiResult.NetworkFailure(error)
    } catch (error: Exception) {
        ApiResult.SerializationFailure(error)
    }

}

internal fun JsonObject.toSetupLoad(profileId: String): SetupLoad {
    val data = get("data")
        ?.takeIf(JsonElement::isJsonObject)
        ?.asJsonObject
        ?: throw IllegalStateException("Missing setup data")

    val linkedIp = data.get("linkedIp")
        ?.takeIf(JsonElement::isJsonObject)
        ?.asJsonObject
        ?: throw IllegalStateException("Missing linked IP setup data")

    val updateToken = linkedIp.stringOrNull("updateToken")
        ?.takeIf(String::isNotBlank)

    return SetupLoad(
        content = SetupContent(
            profileId = profileId,
            dnsOverTls = "$profileId.dns.nextdns.io",
            dnsOverHttps = "https://dns.nextdns.io/$profileId",
            ipv4 = data.stringList("ipv4"),
            ipv6 = data.stringList("ipv6"),
            dnscryptStamp = data.stringOrNull("dnscrypt"),
            linkedIp = LinkedIpContent(
                servers = linkedIp.stringList("servers"),
                address = linkedIp.stringOrNull("ip"),
                ddnsHostname = linkedIp.stringOrNull("ddns"),
            ),
        ),
        linkIpCapability = updateToken?.let { LinkIpCapability(profileId, it) },
    )
}

private fun JsonObject.stringList(name: String): List<String> = get(name)
    ?.takeIf(JsonElement::isJsonArray)
    ?.asJsonArray
    ?.mapNotNull { item ->
        item.takeIf(JsonElement::isJsonPrimitive)?.asString
    }
    .orEmpty()

private fun JsonObject.stringOrNull(name: String): String? = get(name)
    ?.takeIf(JsonElement::isJsonPrimitive)
    ?.asString

internal fun ddnsPayload(hostname: String?): JsonObject = JsonObject().apply {
    if (hostname == null) {
        add("ddns", JsonNull.INSTANCE)
    } else {
        addProperty("ddns", hostname)
    }
}

internal fun ddnsRequestBody(hostname: String?): RequestBody = ddnsPayload(hostname)
    .toString()
    .toRequestBody(DDNS_JSON_MEDIA_TYPE)

private val DDNS_JSON_MEDIA_TYPE = "application/json".toMediaType()

private fun String.isIpAddress(): Boolean {
    if (isBlank() || (!contains('.') && !contains(':'))) return false
    if (any { character ->
            !(character.isDigit() || character in ".:abcdefABCDEF")
        }
    ) {
        return false
    }
    return runCatching { InetAddress.getByName(this) }.isSuccess
}

private fun retrofit2.Response<okhttp3.ResponseBody>.toIpApiResult(): ApiResult<String> {
    if (!isSuccessful) return toServerFailure()

    val address = body()
        ?.string()
        ?.trim()
        .orEmpty()
    return if (address.isIpAddress()) {
        ApiResult.Success(address, code())
    } else {
        ApiResult.SerializationFailure(
            IllegalStateException("Link IP response did not contain an IP address")
        )
    }
}
