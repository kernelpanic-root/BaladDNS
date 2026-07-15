package com.kernelpanic.baladdns.data.nextdns.api

import com.google.gson.JsonObject
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.Path

interface NextDnsIpv4Api {
    @PATCH("profiles/{profileId}/setup/linkedIp")
    suspend fun updateLinkedIp(
        @Path("profileId") profileId: String,
        @Body request: RequestBody,
    ): Response<JsonObject>
}
