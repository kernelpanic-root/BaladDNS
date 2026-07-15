package com.kernelpanic.baladdns.data.nextdns.api

import com.google.gson.annotations.SerializedName

data class NextDnsLoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("code") val code: String? = null,
)

data class NextDnsCreateApiKeyResponse(
    @SerializedName("apiKey") val key: String,
)
