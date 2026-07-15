package com.kernelpanic.baladdns.data.nextdns.api

import com.google.gson.annotations.SerializedName

data class NextDnsStatsGraphResponse(
    @SerializedName("data") val data: List<NextDnsStatsGraphData>,
    @SerializedName("meta") val meta: Any,
)

data class NextDnsStatsGraphData(
    @SerializedName("queries") val queries: List<Int>,
    @SerializedName("status") val status: String,
)

data class NextDnsDevicesResponse(
    @SerializedName("data") val data: List<NextDnsDeviceItem>,
)

data class NextDnsDeviceItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("queries") val queries: Int?,
)
