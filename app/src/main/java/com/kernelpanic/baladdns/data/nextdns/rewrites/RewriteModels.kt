package com.kernelpanic.baladdns.data.nextdns.rewrites

import com.google.gson.annotations.SerializedName

data class CreateRewriteRequest(
    @SerializedName("name") val name: String,
    @SerializedName("content") val content: String,
)

data class Rewrite(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("content") val content: String,
)