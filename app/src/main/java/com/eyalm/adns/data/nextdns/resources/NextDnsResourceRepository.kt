package com.eyalm.adns.data.nextdns.resources

import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.nextdns.api.NextDnsApi
import com.eyalm.adns.data.nextdns.api.nextDnsApiCall
import com.eyalm.adns.data.nextdns.api.toJsonApiResult
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.data.nextdns.model.nextDnsFaviconUrl
import com.eyalm.adns.domain.nextdns.ApiResult

data class CustomResourceList(
    val activeIds: Set<String>,
    val items: List<NextDnsResourceItem>,
)

class NextDnsResourceRepository(
    private val api: NextDnsApi = ApiClient.nextDnsApi,
) {
    suspend fun getActiveIds(
        profileId: String,
        page: String,
        feature: String,
    ): ApiResult<Set<String>> = nextDnsApiCall {
        when (
            val result = api.getActiveListItems(profileId, page, feature)
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing active resource data")
                    )
                ApiResult.Success(
                    data.mapNotNull { element ->
                        element.takeIf { it.isJsonObject }
                            ?.asJsonObject
                            ?.get("id")
                            ?.takeIf { it.isJsonPrimitive }
                            ?.asString
                    }.toSet(),
                    result.status,
                )
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun getServerCatalog(
        page: String,
        feature: String,
    ): ApiResult<List<NextDnsResourceItem>> = nextDnsApiCall {
        when (
            val result = api.getAvailableCatalog(page, feature)
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing resource catalog data")
                    )
                ApiResult.Success(
                    mapServerResourceItems(feature, data),
                    result.status,
                )
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun getCustomList(
        profileId: String,
        page: String,
    ): ApiResult<CustomResourceList> = nextDnsApiCall {
        when (
            val result = api.getPageSettings(profileId, page)
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@nextDnsApiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing custom list data")
                    )
                val activeIds = mutableSetOf<String>()
                val items = data.mapNotNull { element ->
                    val item = element.takeIf { it.isJsonObject }?.asJsonObject
                        ?: return@mapNotNull null
                    val id = item.get("id")
                        ?.takeIf { it.isJsonPrimitive }
                        ?.asString
                        ?: return@mapNotNull null
                    val active = item.get("active")
                        ?.takeIf { it.isJsonPrimitive }
                        ?.asBoolean
                        ?: true
                    if (active) activeIds += id
                    NextDnsResourceItem(
                        id = id,
                        name = "*.$id",
                        icon = nextDnsFaviconUrl(id)
                            ?.let(ListIcon::Url)
                            ?: ListIcon.None,
                    )
                }
                ApiResult.Success(
                    CustomResourceList(activeIds, items),
                    result.status,
                )
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

}
