package com.kernelpanic.baladdns.data.nextdns.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NextDnsLinkIpApi {
    @GET("{profileId}/{updateToken}")
    suspend fun linkCurrentIp(
        @Path("profileId") profileId: String,
        @Path("updateToken") updateToken: String,
    ): Response<ResponseBody>

    @GET("{profileId}/{updateToken}")
    suspend fun getCurrentIp(
        @Path("profileId") profileId: String,
        @Path("updateToken") updateToken: String,
        @Query("touchOnly") touchOnly: Int = 1,
    ): Response<ResponseBody>
}
