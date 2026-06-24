package com.eyalm.adns.data

import android.R.attr.name
import android.content.Context
import android.util.Log
import android.util.Log.e
import com.eyalm.adns.data.models.DnsProviders
import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.network.NextDnsAnalytics
import com.eyalm.adns.data.network.NextDnsCreateProfileRequest
import com.eyalm.adns.data.network.NextDnsDomainsResponse
import com.eyalm.adns.data.network.NextDnsLoginRequest
import com.eyalm.adns.data.network.NextDnsProfile
import com.eyalm.adns.data.network.NextDnsStatsGraphResponse
import com.eyalm.adns.data.network.NextDnsLoginResponse
import com.eyalm.adns.data.network.toHexId
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Blocklist(
    val id: String,
    val name: String?,
    val website: String?,
    val description: String?,
    val entries: Int,
    val updatedOn: String,
    val isEnabled: Boolean
)

sealed class LoginResult {
    object Success : LoginResult()
    object RequiresTwoFactor : LoginResult()
    data class Error(val message: String) : LoginResult()
}

class ApiRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("adns_settings", Context.MODE_PRIVATE)
    val repository = DnsRepository(context)
    val keyManager = TokenManager(context)


    suspend fun NextDnsLogin(email: String, password: String, code: String? = null): LoginResult {
        return try {
            val loginRequest = NextDnsLoginRequest(email, password, code)
            val response = ApiClient.nextDnsApi.login(loginRequest)
            val responseText = response.body()?.string() ?: response.errorBody()?.string() ?: ""

            if (response.isSuccessful) {
                if (responseText.trim() == "OK") {
                    val cookiesList: List<String> = response.headers().values("Set-Cookie")

                    var fullCookieString = ""
                    for (cookieLine in cookiesList) {
                        val coreCookie = cookieLine.substringBefore(";")
                        fullCookieString += "$coreCookie; "
                    }

                    val apiKey = ApiClient.nextDnsApi.createApiKey(fullCookieString.trim()).body()?.key
                        ?: throw IllegalStateException("Failed to retrieve API key after login")
                    keyManager.saveApiKey(apiKey)
                    keyManager.saveEmail(email)
                    Log.d("ApiRepository", "Login successful, API key saved securely")
                    return LoginResult.Success
                }
            }

            val nextDnsResponse = try {
                Gson().fromJson(responseText, NextDnsLoginResponse::class.java)
            } catch (e: Exception) {
                null
            }

            if (nextDnsResponse?.requiresCode == true) {
                LoginResult.RequiresTwoFactor
            } else {
                val errorMessage = if (responseText.isNotEmpty()) responseText else "Login Failed: ${response.code()}"
                e("ApiRepository", "Login Failed: $errorMessage")
                LoginResult.Error(errorMessage)
            }

        } catch (e: Exception) {
            e("ApiRepository", "Network Error during login", e)
            LoginResult.Error(e.message ?: "Unknown error")
        }
    }

    fun getCurrentNextDnsProfileId(): String? {
        return sharedPrefs.getString("enhanced_url", null)?.let { url ->
            val cleanUrl = url.removeSuffix(".dns.nextdns.io")
            if (cleanUrl.contains("-")) {
                cleanUrl.substringAfterLast('-')
            } else {
                cleanUrl
            }
        }
    }

    suspend fun getNextDnsStats(): NextDnsAnalytics? {
        val profileId = requireAuth()

        return try {
            ApiClient.nextDnsApi.getAnalytics(profileId, "-30d")
        } catch (e: Exception) {
            e("ApiRepository", "Error fetching analytics", e)
            null
        }
    }

    suspend fun getNextDnsEmail(): String = withContext(Dispatchers.IO) {
        keyManager.getEmail() ?: ""
    }



    suspend fun getNextDnsProfiles(): List<NextDnsProfile> {
        if (!keyManager.hasToken()) throw IllegalStateException("Not logged in")
        return try {
            val response = ApiClient.nextDnsApi.getProfiles()
            response.data
        } catch (e: Exception) {
            e("ApiRepository", "Error fetching profiles", e)
            emptyList()
        }
    }

    fun setNextDnsProfile(profile: NextDnsProfile, deviceName: String? = null) {
        val sanitizedName = deviceName?.replace(" ", "--")
        val url = if (sanitizedName.isNullOrEmpty()) {
            "${profile.id}.dns.nextdns.io"
        } else {
            "$sanitizedName-${profile.id}.dns.nextdns.io"
        }
        repository.setProvider(DnsProviders.NEXTDNS.id, url)
    }

    fun setNextDnsDeviceName(deviceName: String) {
        val profileId = getCurrentNextDnsProfileId() ?: return
        val sanitizedName = deviceName.trim().replace(" ", "--")
        val url = if (sanitizedName.isEmpty()) {
            "$profileId.dns.nextdns.io"
        } else {
            "$sanitizedName-$profileId.dns.nextdns.io"
        }
        repository.setProvider(DnsProviders.NEXTDNS.id, url)
    }

    fun getNextDnsDeviceName(): String {
        val url = sharedPrefs.getString("enhanced_url", "") ?: ""
        val cleanUrl = url.removeSuffix(".dns.nextdns.io")
        return if (cleanUrl.contains("-")) {
            cleanUrl.substringBeforeLast("-").replace("--", " ")
        } else {
            ""
        }
    }

    suspend fun createNextDnsProfile(name: String) {
        if (!keyManager.hasToken()) throw IllegalStateException("Not logged in")
        try {
            ApiClient.nextDnsApi.createProfile(NextDnsCreateProfileRequest.withName(name))
        } catch (e: Exception) {
            e("ApiRepository", "Error creating profile", e)
        }

    }



    suspend fun getNextDnsStatsGraph(period: String = "-30d"): NextDnsStatsGraphResponse {
        val profileId = requireAuth()
        return try {
            val tz = java.util.TimeZone.getDefault().id
            ApiClient.nextDnsApi.getStatsGraph(profileId, period, "start", tz)
        } catch (e: Exception) {
            e("ApiRepository", "Error fetching stats graph", e)
            throw e
        }
    }

    suspend fun getNextDnsDomains(status: String, period: String = "-30d", limit: Int = 10): NextDnsDomainsResponse {
        val profileId = requireAuth()
        return try {
            ApiClient.nextDnsApi.getDomains(profileId, status, period, limit)
        } catch (e: Exception) {
            e("ApiRepository", "Error fetching domains ($status)", e)
            throw e
        }
    }

    fun nextDnsLogOut() {
        keyManager.destroyApiKey()
        keyManager.destroyEmail()
        repository.setProvider(DnsProviders.ADGUARD.id)
    }


    // new generic methods

    fun requireAuth(): String {
        if (!keyManager.hasToken()) {
            throw IllegalStateException("Not logged in")
        }
        val profileId = getCurrentNextDnsProfileId()
            ?: throw IllegalStateException("No profile selected")
        return profileId
    }

    suspend fun getPageSettings(page: String): JsonObject? {
        return try {
            val profileId = requireAuth()
            val response = ApiClient.nextDnsApi.getPageSettings(profileId, page)
            response.getAsJsonObject("data")
        } catch (e: Exception) {
            e("ApiRepository", "Error fetching $page settings", e)
            null
        }
    }

    suspend fun patchPageSettings(
        page: String,
        payload: Map<String, Any>
    ): Boolean {
        return try {
            val profileId = requireAuth()
            val response = ApiClient.nextDnsApi.patchPageSettings(
                profileId, page, payload
            )
            response.isSuccessful
        } catch (e: Exception) {
            e("ApiRepository", "Error patching $page", e)
            false
        }
    }

    suspend fun getActiveListItems(page: String, feat: String): List<String> {
        return try {
            val profileId = requireAuth()
            val response = ApiClient.nextDnsApi.getActiveListItems(
                profileId, page, feat
            )
            val dataArray = response.getAsJsonArray("data")
            dataArray.map { it.asJsonObject.get("id").asString }
        } catch (e: Exception) {
            e("ApiRepository", "Error fetching active $page/$feat", e)
            emptyList()
        }
    }

    suspend fun getAvailableCatalog(page: String, feat: String): JsonObject? {
        return try {
            requireAuth()
            ApiClient.nextDnsApi.getAvailableCatalog(page, feat)
        } catch (e: Exception) {
            e("ApiRepository", "Error fetching catalog $page/$feat", e)
            null
        }
    }

    suspend fun addListItem(page: String, feat: String, itemId: String): Boolean {
        return try {
            val profileId = requireAuth()
            val response = ApiClient.nextDnsApi.addListItem(
                profileId, page, feat, mapOf("id" to itemId)
            )
            response.isSuccessful
        } catch (e: Exception) {
            e("ApiRepository", "Error adding $itemId to $page/$feat", e)
            false
        }
    }

    suspend fun removeListItem(page: String, feat: String, itemId: String): Boolean {
        return try {
            val profileId = requireAuth()
            val response = ApiClient.nextDnsApi.removeListItem(
                profileId, page, feat, itemId.toHexId()
            )
            response.isSuccessful
        } catch (e: Exception) {
            e("ApiRepository", "Error removing $itemId from $page/$feat", e)
            false
        }
    }

    suspend fun getCustomListItems(page: String): JsonArray? {
        return try {
            val profileId = requireAuth()
            val response = ApiClient.nextDnsApi.getPageSettings(profileId, page)
            response.getAsJsonArray("data")
        } catch (e: Exception) { null }
    }

    suspend fun addCustomListItem(page: String, domain: String): Boolean {
        return try {
            val profileId = requireAuth()
            val response = ApiClient.nextDnsApi.addCustomItem(profileId, page, mapOf("id" to domain))
            response.isSuccessful
        } catch (e: Exception) { false }
    }

    suspend fun patchCustomListItem(page: String, domain: String, isActive: Boolean): Boolean {
        return try {
            val profileId = requireAuth()
            val response = ApiClient.nextDnsApi.patchCustomItem(profileId, page, domain.toHexId(), mapOf("active" to isActive))
            response.isSuccessful
        } catch (e: Exception) { false }
    }

    suspend fun removeCustomListItem(page: String, domain: String): Boolean {
        return try {
            val profileId = requireAuth()
            val response = ApiClient.nextDnsApi.removeCustomItem(profileId, page, domain.toHexId())
            response.isSuccessful
        } catch (e: Exception) { false }
    }



    // old
    suspend fun getNextDnsBlocklists(): List<Blocklist> {
        throw UnsupportedOperationException("deprecated")
        /**
        val cookie = getNextDnsCookie()
        if (!isLoggedIn(DnsProviders.NEXTDNS) || cookie == null) {
            e("ApiRepository", "No cookie found. User must login first.")
            throw IllegalStateException("User must login first")
        }

        val profileId = getCurrentNextDnsProfileId()
        if (profileId == null) {
            e("ApiRepository", "No profile ID found. User must select a profile first.")
            throw IllegalStateException("User must select a profile first")
        }

        try {
            val blocklistsResponse = ApiClient.nextDnsApi.getBlocklists(cookie)
            Log.d("ApiRepository", "Blocklists: $blocklistsResponse")
            val privacyResponse = ApiClient.nextDnsApi.getPrivacy(cookie, profileId)
            val blocklists = mutableListOf<Blocklist>()
            blocklistsResponse.data.forEach { blocklist: NextDnsBlocklistData ->
                val isEnabled = privacyResponse.data.blocklists.contains(blocklist)
                blocklists.add(
                    Blocklist(
                        id =  blocklist.id,
                        name = if (blocklist.id == "nextdns-recommended") "NextDNS Ads & Trackers Blocklist" else blocklist.name,
                        website = blocklist.website,
                        description = if (blocklist.id == "nextdns-recommended") "A comprehensive blocklist to block ads & trackers in all countries. This is the recommended starter blocklist." else blocklist.description,
                        entries = blocklist.entries,
                        updatedOn = blocklist.updatedOn,
                        isEnabled = isEnabled
                    )
                )
                Log.d("ApiRepository", "Blocklist: $blocklist")
            }

            return blocklists
        } catch (e: Exception) {
            e("ApiRepository", "Error fetching blocklists", e)
            throw e
        }
        */
    }

    suspend fun updateNextDnsBlocklists(blocklistId: String) {
        throw UnsupportedOperationException("deprecated")
         /**
        val cookie = getNextDnsCookie()
        if (!isLoggedIn(DnsProviders.NEXTDNS) || cookie == null) {
            e("ApiRepository", "No cookie found. User must login first.")
            throw IllegalStateException("User must login first")
        }

        val profileId = getCurrentNextDnsProfileId()
        if (profileId == null) {
            e("ApiRepository", "No profile ID found. User must select a profile first.")
            throw IllegalStateException("User must select a profile first")
        }


        try {
            ApiClient.nextDnsApi.addBlocklist(cookie, profileId,
                NextDnsUpdateBlocklistsRequest(blocklistId))
            Log.d("ApiRepository", "Blocklist updated successfully: $blocklistId")


        } catch (e: Exception) {
            e("ApiRepository", "Error updating blocklists", e)
            throw e
        }
            */
    }


    suspend fun removeNextDnsBlocklists(blocklistId: String) {
        throw UnsupportedOperationException("deprecated")
        /**

        val cookie = getNextDnsCookie()
        if (!isLoggedIn(DnsProviders.NEXTDNS) || cookie == null) {
            e("ApiRepository", "No cookie found. User must login first.")
            throw IllegalStateException("User must login first")
        }

        val profileId = getCurrentNextDnsProfileId()
        if (profileId == null) {
            e("ApiRepository", "No profile ID found. User must select a profile first.")
            throw IllegalStateException("User must select a profile first")
        }


        try {
            ApiClient.nextDnsApi.removeBlocklist(cookie, profileId, blocklistId)
            Log.d("ApiRepository", "Blocklist removed successfully: $blocklistId")


        } catch (e: Exception) {
            e("ApiRepository", "Error removing blocklists", e)
            throw e
        }
        */
    }





}