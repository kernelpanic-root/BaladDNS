package com.kernelpanic.baladdns.data.nextdns.logs

import com.kernelpanic.baladdns.data.network.ApiClient
import com.kernelpanic.baladdns.data.nextdns.api.NextDnsApi
import com.kernelpanic.baladdns.data.nextdns.api.NextDnsLogsResponse
import com.kernelpanic.baladdns.data.nextdns.api.NextDnsErrorParser
import com.kernelpanic.baladdns.data.nextdns.api.nextDnsApiCall
import com.kernelpanic.baladdns.data.nextdns.api.toBodyApiResult
import com.kernelpanic.baladdns.data.nextdns.api.toEmptyApiResult
import com.kernelpanic.baladdns.data.nextdns.api.toHexId
import com.kernelpanic.baladdns.domain.nextdns.ApiResult
import com.google.gson.JsonParser
import java.io.IOException
import kotlinx.coroutines.CancellationException

data class LogsQuery(
    val search: String = "",
    val blockedOnly: Boolean = false,
    val raw: Boolean = false,
    val deviceId: String? = null,
)

enum class DomainRuleList(val wireName: String) {
    Allow("allowlist"),
    Deny("denylist"),
}

sealed interface DomainRuleResult {
    data object Added : DomainRuleResult
    data object Activated : DomainRuleResult
    data class Failure(val result: ApiResult<Nothing>) : DomainRuleResult
}

class NextDnsLogsRepository(
    private val api: NextDnsApi = ApiClient.nextDnsApi,
) {
    suspend fun getLogs(
        profileId: String,
        query: LogsQuery,
        cursor: String? = null,
    ): ApiResult<NextDnsLogsResponse> = nextDnsApiCall {
        api.getLogs(
            profileId = profileId,
            cursor = cursor,
            device = query.deviceId,
            status = if (query.blockedOnly) "blocked" else null,
            search = query.search.takeIf(String::isNotBlank),
            raw = if (query.raw) 1 else null,
        ).toBodyApiResult()
    }

    suspend fun applyRule(
        profileId: String,
        list: DomainRuleList,
        domain: String,
    ): DomainRuleResult {
        return try {
            val response = api.addCustomItem(
                profileId = profileId,
                page = list.wireName,
                payload = mapOf("id" to domain),
            )
            val problems = response.body()?.let(NextDnsErrorParser::parse)
                ?: response.errorBody()
                    ?.string()
                    ?.takeIf(String::isNotBlank)
                    ?.let { raw ->
                        runCatching { JsonParser.parseString(raw).asJsonObject }
                            .getOrNull()
                            ?.let(NextDnsErrorParser::parse)
                    }
                ?: emptyList()

            if (response.isSuccessful && problems.isEmpty()) return DomainRuleResult.Added
            if (problems.none { it.code == "duplicate" }) {
                return DomainRuleResult.Failure(
                    ApiResult.ServerFailure(
                        status = response.code(),
                        problems = problems,
                    )
                )
            }

            when (
                val activation = api.patchCustomItem(
                    profileId = profileId,
                    page = list.wireName,
                    hexId = domain.toHexId(),
                    payload = mapOf("active" to true),
                ).toEmptyApiResult()
            ) {
                is ApiResult.Success -> DomainRuleResult.Activated
                is ApiResult.ServerFailure -> DomainRuleResult.Failure(activation)
                is ApiResult.NetworkFailure -> DomainRuleResult.Failure(activation)
                is ApiResult.SerializationFailure -> DomainRuleResult.Failure(activation)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            DomainRuleResult.Failure(ApiResult.NetworkFailure(error))
        } catch (error: Exception) {
            DomainRuleResult.Failure(ApiResult.SerializationFailure(error))
        }
    }

}
