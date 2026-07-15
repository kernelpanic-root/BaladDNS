package com.kernelpanic.baladdns.data.nextdns.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface NextDnsCookieAuthApi {
    @POST("accounts/@login")
    suspend fun login(
        @Body request: NextDnsLoginRequest,
    ): Response<ResponseBody>

    @POST("account/apiKeys")
    suspend fun exchangeCookieForApiKey(
        @Header("Cookie") cookie: String,
    ): Response<NextDnsCreateApiKeyResponse>
}
