package com.kernelpanic.baladdns.data.nextdns.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface NextDnsApiKeyAuthApi {
    @GET("profiles")
    suspend fun verifyApiKey(
        @Header("X-Api-Key") apiKey: String,
    ): Response<NextDnsProfilesResponse>
}
