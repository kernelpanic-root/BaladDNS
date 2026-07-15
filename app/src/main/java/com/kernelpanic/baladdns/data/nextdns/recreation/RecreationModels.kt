package com.kernelpanic.baladdns.data.nextdns.recreation

import com.google.gson.annotations.SerializedName

data class RecreationWindowDto(
    @SerializedName("start") val start: String,
    @SerializedName("end") val end: String,
)

data class RecreationScheduleDto(
    @SerializedName("times") val times: Map<String, RecreationWindowDto> = emptyMap(),
    @SerializedName("timezone") val timezone: String? = null,
)

data class ParentalRecreationItem(
    @SerializedName("id") val id: String,
    @SerializedName("active") val active: Boolean,
    @SerializedName("recreation") val recreation: Boolean,
    @SerializedName("website") val website: String? = null,
)

data class ParentalRecreationState(
    @SerializedName("services") val services: List<ParentalRecreationItem> = emptyList(),
    @SerializedName("categories") val categories: List<ParentalRecreationItem> = emptyList(),
    @SerializedName("recreation") val recreation: RecreationScheduleDto = RecreationScheduleDto(),
)

data class UpdateRecreationScheduleRequest(
    @SerializedName("recreation") val recreation: RecreationScheduleDto,
)

data class UpdateRecreationItemRequest(
    @SerializedName("recreation") val recreation: Boolean,
)

enum class RecreationItemCollection(val wireName: String) {
    Services("services"),
    Categories("categories"),
}
