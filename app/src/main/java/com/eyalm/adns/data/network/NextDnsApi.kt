package com.eyalm.adns.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NextDnsApi {

    @POST("accounts/@login")
    suspend fun login(
        @Body request: NextDnsLoginRequest
    ): Response<Unit>

    @GET("accounts/@me?withProfiles=true")
    suspend fun getProfiles(
        @Header("Cookie") cookie: String
    ): NextDnsProfileResponse

    @POST("profiles")
    suspend fun createProfile(
        @Header("Cookie") cookie: String,
        @Body request: NextDnsCreateProfileRequest
    ): Response<NextDnsProfile>

    @GET("profiles/{profileId}/analytics/status")
    suspend fun getAnalytics(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String,
        @Query("from") period: String
    ): NextDnsAnalytics

    @GET("privacy/blocklists")
    suspend fun getBlocklists(
        @Header("Cookie") cookie: String
    ): NextDnsBlocklistResponse

    @GET("profiles/{profileId}/privacy")
    suspend fun getPrivacy(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String
    ): NextDnsPrivacyResponse

    @POST("profiles/{profileId}/privacy/blocklists")
    suspend fun addBlocklist(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String,
        @Body body: NextDnsUpdateBlocklistsRequest
    ): Response<Unit>

    @DELETE("profiles/{profileId}/privacy/blocklists/{blocklistId}")
    suspend fun removeBlocklist(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String,
        @Path("blocklistId") blocklistId: String
    ): Response<Unit>

}