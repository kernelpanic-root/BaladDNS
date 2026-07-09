package com.eyalm.adns.data.nextdns.profile

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.eyalm.adns.data.AppRuntimeRepositories
import com.eyalm.adns.data.DnsRepository
import com.eyalm.adns.data.TokenManager
import com.eyalm.adns.data.dns.DnsConfigurationResult
import com.eyalm.adns.data.provider.DnsProviderCatalog
import com.eyalm.adns.data.provider.DnsProviderSelection
import com.eyalm.adns.data.provider.ResolverPresetId
import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.nextdns.api.NextDnsApi
import com.eyalm.adns.data.nextdns.api.NextDnsCreateProfileRequest
import com.eyalm.adns.data.nextdns.api.NextDnsProfile
import com.eyalm.adns.data.nextdns.api.nextDnsApiCall
import com.eyalm.adns.data.nextdns.api.toEmptyApiResult
import com.eyalm.adns.data.nextdns.api.toJsonApiResult
import com.eyalm.adns.data.nextdns.api.toServerFailure
import com.eyalm.adns.data.nextdns.auth.NextDnsSessionManager
import com.eyalm.adns.data.nextdns.logs.LogExportResult
import com.eyalm.adns.domain.nextdns.ApiProblem
import com.eyalm.adns.domain.nextdns.ApiResult
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NextDnsProfileRepository(
    context: Context,
    private val api: NextDnsApi = ApiClient.nextDnsApi,
) {
    private val appContext = context.applicationContext
    private val sharedPreferences =
        appContext.getSharedPreferences("adns_settings", Context.MODE_PRIVATE)
    private val dnsRepository = DnsRepository(appContext)
    private val tokenManager = TokenManager.getInstance(appContext)
    private val gson = Gson()

    fun currentProfileId(): String? =
        sharedPreferences.getString("enhanced_url", null)?.let { url ->
            val cleanUrl = url.removeSuffix(".dns.nextdns.io")
            if ('-' in cleanUrl) cleanUrl.substringAfterLast('-') else cleanUrl
        }

    suspend fun email(): String = withContext(Dispatchers.IO) {
        tokenManager.getEmail().orEmpty()
    }

    fun isSignedIn(): Boolean = tokenManager.hasToken()

    suspend fun profiles(): ApiResult<List<NextDnsProfile>> = authenticatedCall {
        when (val result = api.getProfilesRaw().toJsonApiResult()) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@authenticatedCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing profiles data")
                    )
                ApiResult.Success(
                    gson.fromJson(data, Array<NextDnsProfile>::class.java).toList(),
                    result.status,
                )
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun createProfile(name: String): ApiResult<NextDnsProfile> = authenticatedCall {
        when (
            val result = api.createProfile(NextDnsCreateProfileRequest.withName(name.trim()))
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonObject("data")
                    ?: return@authenticatedCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing created profile data")
                    )
                ApiResult.Success(
                    gson.fromJson(data, NextDnsProfile::class.java),
                    result.status,
                )
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun selectProfile(
        profile: NextDnsProfile,
        deviceName: String? = DEFAULT_NEXTDNS_DEVICE_NAME,
    ): DnsConfigurationResult {
        return selectProfileId(profile.id, deviceName)
    }

    suspend fun selectProfileId(
        profileId: String,
        deviceName: String? = DEFAULT_NEXTDNS_DEVICE_NAME,
    ): DnsConfigurationResult = selectProfileHostname(
        nextDnsProfileHostname(profileId, deviceName)
    )

    suspend fun setDeviceName(deviceName: String): DnsConfigurationResult? {
        val profileId = currentProfileId() ?: return null
        return selectProfileHostname(nextDnsProfileHostname(profileId, deviceName))
    }

    fun deviceName(): String {
        val hostname = sharedPreferences.getString("enhanced_url", "").orEmpty()
        val cleanUrl = hostname.removeSuffix(".dns.nextdns.io")
        return if ('-' in cleanUrl) {
            cleanUrl.substringBeforeLast('-').replace("--", " ")
        } else {
            ""
        }
    }

    suspend fun clearSelectedProfile() {
        if (dnsRepository.currentSelection() is DnsProviderSelection.Enhanced) {
            if (!selectFallbackProvider()) return
        }
        dnsRepository.clearEnhancedHostname()
    }

    suspend fun signOut() {
        NextDnsSessionManager.getInstance(appContext).signedOut()
    }

    private suspend fun selectProfileHostname(hostname: String): DnsConfigurationResult {
        val capabilities = AppRuntimeRepositories.capabilities(appContext).current()
        return if (capabilities.canControlPrivateDns) {
            dnsRepository.changeEnhancedSelection(hostname)
        } else {
            when (dnsRepository.stageEnhancedSelection(hostname)) {
                is com.eyalm.adns.data.provider.ProviderSelectionUpdateResult.Saved ->
                    DnsConfigurationResult.Changed(
                        DnsProviderSelection.Enhanced(DnsProviderCatalog.NEXTDNS),
                        appliedToDevice = false,
                    )

                is com.eyalm.adns.data.provider.ProviderSelectionUpdateResult.Invalid ->
                    DnsConfigurationResult.InvalidSelection
            }
        }
    }

    private suspend fun selectFallbackProvider(): Boolean {
        val fallback = DnsProviderSelection.Standard(
            DnsProviderCatalog.ADGUARD,
            ResolverPresetId("default"),
        )
        val result = if (
            AppRuntimeRepositories.capabilities(appContext).current().canControlPrivateDns
        ) {
            dnsRepository.changeSelection(fallback)
        } else {
            return dnsRepository.stageSelection(fallback) is
                com.eyalm.adns.data.provider.ProviderSelectionUpdateResult.Saved
        }
        return result is DnsConfigurationResult.Changed
    }

    suspend fun clearLogs(profileId: String): ApiResult<Unit> = authenticatedCall {
        api.clearLogs(profileId).toEmptyApiResult()
    }

    suspend fun exportLogs(profileId: String, destination: Uri): LogExportResult =
        withContext(Dispatchers.IO) {
            if (!isSignedIn()) {
                return@withContext LogExportResult.ApiFailure(authRequired())
            }
            val response = try {
                api.downloadLogs(profileId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                return@withContext LogExportResult.ApiFailure(ApiResult.NetworkFailure(error))
            } catch (error: Exception) {
                return@withContext LogExportResult.ApiFailure(
                    ApiResult.SerializationFailure(error)
                )
            }

            if (!response.isSuccessful) {
                return@withContext LogExportResult.ApiFailure(response.toServerFailure())
            }
            val body = response.body()
                ?: return@withContext LogExportResult.ApiFailure(
                    ApiResult.SerializationFailure(
                        IllegalStateException("Missing logs download body")
                    )
                )

            try {
                body.byteStream().use { input ->
                    appContext.contentResolver.openOutputStream(destination)?.use { output ->
                        input.copyTo(output)
                    } ?: return@withContext LogExportResult.DestinationFailure(
                        IllegalStateException("Unable to open export destination")
                    )
                }
                LogExportResult.Success
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                LogExportResult.DestinationFailure(error)
            }
        }

    suspend fun duplicateProfile(
        profileId: String,
        newName: String,
    ): ApiResult<NextDnsProfile> {
        val detail = profileDetail(profileId)
        if (detail !is ApiResult.Success) return detail.asFailure()

        return authenticatedCall {
            when (
                val result = api.duplicateProfile(
                    detail.value.toDuplicateProfilePayload(newName.trim())
                ).toJsonApiResult()
            ) {
                is ApiResult.Success -> {
                    val data = result.value.getAsJsonObject("data")
                        ?: return@authenticatedCall ApiResult.SerializationFailure(
                            IllegalStateException("Missing duplicated profile data")
                        )
                    ApiResult.Success(
                        gson.fromJson(data, NextDnsProfile::class.java),
                        result.status,
                    )
                }

                is ApiResult.ServerFailure -> result
                is ApiResult.NetworkFailure -> result
                is ApiResult.SerializationFailure -> result
            }
        }
    }

    suspend fun renameProfile(profileId: String, newName: String): ApiResult<Unit> =
        authenticatedCall {
            api.renameProfile(
                profileId,
                JsonObject().apply { addProperty("name", newName.trim()) },
            ).toEmptyApiResult()
        }

    suspend fun deleteOrLeaveProfile(profileId: String): ApiResult<Unit> =
        authenticatedCall {
            api.deleteOrLeaveProfile(profileId).toEmptyApiResult()
        }

    private suspend fun profileDetail(profileId: String): ApiResult<JsonObject> =
        authenticatedCall {
            when (val result = api.getProfileDetail(profileId).toJsonApiResult()) {
                is ApiResult.Success -> {
                    val data = result.value.getAsJsonObject("data")
                        ?: return@authenticatedCall ApiResult.SerializationFailure(
                            IllegalStateException("Missing profile detail data")
                        )
                    ApiResult.Success(data, result.status)
                }

                is ApiResult.ServerFailure -> result
                is ApiResult.NetworkFailure -> result
                is ApiResult.SerializationFailure -> result
            }
        }

    private suspend fun <T> authenticatedCall(
        block: suspend () -> ApiResult<T>,
    ): ApiResult<T> {
        if (!isSignedIn()) return authRequired()
        return nextDnsApiCall(block)
    }

    private fun authRequired() = ApiResult.ServerFailure(
        status = 401,
        problems = listOf(ApiProblem("authRequired")),
    )
}

const val DEFAULT_NEXTDNS_DEVICE_NAME = "ADNS"

fun nextDnsProfileHostname(
    profileId: String,
    deviceName: String? = DEFAULT_NEXTDNS_DEVICE_NAME,
): String {
    val sanitizedName = deviceName.orEmpty().trim().replace(" ", "--")
    return if (sanitizedName.isEmpty()) {
        "$profileId.dns.nextdns.io"
    } else {
        "$sanitizedName-$profileId.dns.nextdns.io"
    }
}

private fun <T> ApiResult<JsonObject>.asFailure(): ApiResult<T> = when (this) {
    is ApiResult.ServerFailure -> this
    is ApiResult.NetworkFailure -> this
    is ApiResult.SerializationFailure -> this
    is ApiResult.Success -> error("Success cannot be converted to a failure")
}
