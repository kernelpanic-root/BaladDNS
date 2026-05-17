package com.eyalm.adns.data

import android.content.Context
import android.util.Log
import android.util.Log.e
import com.eyalm.adns.data.models.DnsProvider
import com.eyalm.adns.data.models.DnsProviders
import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.network.NextDnsAnalytics
import com.eyalm.adns.data.network.NextDnsBlocklistData
import com.eyalm.adns.data.network.NextDnsCreateProfileRequest
import com.eyalm.adns.data.network.NextDnsLoginRequest
import com.eyalm.adns.data.network.NextDnsProfile
import com.eyalm.adns.data.network.NextDnsUpdateBlocklistsRequest

data class Blocklist(
    val id: String,
    val name: String?,
    val website: String?,
    val description: String?,
    val entries: Int,
    val updatedOn: String,
    val isEnabled: Boolean
)

class ApiRepository(context: Context) {

    private val sharedPrefs = context.getSharedPreferences("adns_settings", Context.MODE_PRIVATE)
    val repository = DnsRepository(context)
    private companion object {
        const val NEXTDNS_COOKIE_KEY = "nextdns_cookie"
    }

    private fun getNextDnsCookie(): String? {
        return sharedPrefs.getString(NEXTDNS_COOKIE_KEY, null)?.takeIf { it.isNotBlank() }
    }

    suspend fun NextDnsLogin(email: String, password: String): Boolean {
        return try {
            val loginRequest = NextDnsLoginRequest(email, password)
            val response = ApiClient.nextDnsApi.login(loginRequest)

            if (response.isSuccessful) {

                val cookiesList: List<String> = response.headers().values("Set-Cookie")

                var fullCookieString = ""
                for (cookieLine in cookiesList) {
                    val coreCookie = cookieLine.substringBefore(";")
                    fullCookieString += "$coreCookie; "
                }

                sharedPrefs.edit()
                    .putString(NEXTDNS_COOKIE_KEY, fullCookieString.trim())
                    .apply()

                Log.d("ApiRepository", "Login Success! Cookies saved: $fullCookieString")

                true
            } else {
                e("ApiRepository", "Login Failed: ${response.code()} - ${response.message()}")
                false
            }


        } catch (e: Exception) {
            Log.e("ApiRepository", "Network Error during login", e)

            false
        }

    }

    fun getCurrentNextDnsProfileId(): String? {
        val url = sharedPrefs.getString("enhanced_url", null);
        if (url != null) {
            return url.substringBefore(".dns.nextdns.io")
        }
        return null
    }

    suspend fun getNextDnsStats(): NextDnsAnalytics? {
        val profileId = getCurrentNextDnsProfileId()
        val cookie = getNextDnsCookie()

        if (profileId == null || cookie == null) {

            Log.e("ApiRepository", "No profile or cookie available for NextDNS stats")
            e("ApiRepository", "${if (profileId == null) "Profile ID is null. " else ""}${if (cookie == null) "Cookie is null." else ""}")
            return null
        }

        return try {
            ApiClient.nextDnsApi.getAnalytics(cookie, profileId, "-30d")
        } catch (e: Exception) {
            Log.e("ApiRepository", "Error fetching analytics", e)
            null
        }
    }

    suspend fun getNextDnsEmail(): String {

        val cookie = getNextDnsCookie()

        if (!isLoggedIn(DnsProviders.NEXTDNS) || cookie == null) {
            Log.e("ApiRepository", "No cookie found. User must login first.")
            throw IllegalStateException("User must login first")
        }

        return try {
            val response = ApiClient.nextDnsApi.getProfiles(cookie)
            response.email
        } catch (e: Exception) {
            Log.e("ApiRepository", "Error fetching email", e)
            throw e
        }

    }

    fun isLoggedIn(provider: DnsProvider): Boolean {
        return provider is DnsProvider.Enhanced && getNextDnsCookie() != null
    }

    suspend fun getNextDnsProfiles(): List<NextDnsProfile> {

        val cookie = getNextDnsCookie()

        if (!isLoggedIn(DnsProviders.NEXTDNS) || cookie == null) {
            Log.e("ApiRepository", "No cookie found. User must login first.")
            return emptyList()
        }

        return try {
            val response = ApiClient.nextDnsApi.getProfiles(cookie)
            response.profiles
        } catch (e: Exception) {
            Log.e("ApiRepository", "Error fetching profiles", e)
            emptyList()
        }
    }

    fun setNextDnsProfile(profile: NextDnsProfile) {
        repository.setProvider(DnsProviders.NEXTDNS.id, profile.id + ".dns.nextdns.io")
    }

    suspend fun createNextDnsProfile(name: String) {

        val cookie = getNextDnsCookie()

        if (!isLoggedIn(DnsProviders.NEXTDNS) || cookie == null) {
            Log.e("ApiRepository", "No cookie found. User must login first.")
            throw IllegalStateException("User must login first")
        }


        try {
            val response = ApiClient.nextDnsApi.createProfile(cookie, NextDnsCreateProfileRequest.withName(name))
        } catch (e: Exception) {
            Log.e("ApiRepository", "Error creating profile", e)
        }

    }

    suspend fun getNextDnsBlocklists(): List<Blocklist> {
        val cookie = getNextDnsCookie()
        if (!isLoggedIn(DnsProviders.NEXTDNS) || cookie == null) {
            Log.e("ApiRepository", "No cookie found. User must login first.")
            throw IllegalStateException("User must login first")
        }

        val profileId = getCurrentNextDnsProfileId()
        if (profileId == null) {
            Log.e("ApiRepository", "No profile ID found. User must select a profile first.")
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
                        id = blocklist.id,
                        name = blocklist.name,
                        website = blocklist.website,
                        description = blocklist.description,
                        entries = blocklist.entries,
                        updatedOn = blocklist.updatedOn,
                        isEnabled = isEnabled
                    )
                )
                Log.d("ApiRepository", "Blocklist: $blocklist")
            }

            return blocklists
        } catch (e: Exception) {
            Log.e("ApiRepository", "Error fetching blocklists", e)
            throw e
        }
    }

    suspend fun updateNextDnsBlocklists(blocklistId: String) {
        val cookie = getNextDnsCookie()
        if (!isLoggedIn(DnsProviders.NEXTDNS) || cookie == null) {
            Log.e("ApiRepository", "No cookie found. User must login first.")
            throw IllegalStateException("User must login first")
        }

        val profileId = getCurrentNextDnsProfileId()
        if (profileId == null) {
            Log.e("ApiRepository", "No profile ID found. User must select a profile first.")
            throw IllegalStateException("User must select a profile first")
        }


        try {
            ApiClient.nextDnsApi.addBlocklist(cookie, profileId,
                NextDnsUpdateBlocklistsRequest(blocklistId))
            Log.d("ApiRepository", "Blocklist updated successfully: $blocklistId")


        } catch (e: Exception) {
            Log.e("ApiRepository", "Error updating blocklists", e)
            throw e
        }

    }


    suspend fun removeNextDnsBlocklists(blocklistId: String) {

        val cookie = getNextDnsCookie()
        if (!isLoggedIn(DnsProviders.NEXTDNS) || cookie == null) {
            Log.e("ApiRepository", "No cookie found. User must login first.")
            throw IllegalStateException("User must login first")
        }

        val profileId = getCurrentNextDnsProfileId()
        if (profileId == null) {
            Log.e("ApiRepository", "No profile ID found. User must select a profile first.")
            throw IllegalStateException("User must select a profile first")
        }


        try {
            ApiClient.nextDnsApi.removeBlocklist(cookie, profileId, blocklistId)
            Log.d("ApiRepository", "Blocklist removed successfully: $blocklistId")


        } catch (e: Exception) {
            Log.e("ApiRepository", "Error removing blocklists", e)
            throw e
        }

    }

}