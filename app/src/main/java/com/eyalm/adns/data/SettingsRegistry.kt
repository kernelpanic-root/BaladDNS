package com.eyalm.adns.data

import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.JsonObject

data class ToggleSetting(
    val apiPath: List<String>,
    val localeKey: String,
    val category: String,
    val customTitle: String? = null,
    val customDescription: String? = null
) {
    // for simple (non-nested) toggles

    constructor(
        apiKey: String,
        localeKey: String,
        category: String
    ) : this(
        apiPath = listOf(apiKey),
        localeKey = localeKey,
        category = category
    )

    val stateKey: String get() = apiPath.joinToString(".")

    fun title(): String =
        customTitle ?: Locales.getString(category, localeKey, "name")

    fun description(): String =
        customDescription ?: Locales.getString(category, localeKey, "description")



    // get this toggle's boolean value from a response
    fun readFrom(data: JsonObject): Boolean? {
        var current: JsonObject = data
        for (i in 0 until apiPath.size - 1) {
            current = current.getAsJsonObject(apiPath[i]) ?: return null
        }
        val element = current.get(apiPath.last()) ?: return null
        return if (element.isJsonPrimitive && element.asJsonPrimitive.isBoolean) {
            element.asBoolean
        } else null
    }

    fun buildPatchPayload(value: Boolean): Map<String, Any> {
        var result: Map<String, Any> = mapOf(apiPath.last() to value)
        for (i in apiPath.size - 2 downTo 0) {
            result = mapOf(apiPath[i] to result)
        }
        return result
    }
}



sealed class ListIcon {
    data class Url(val url: String) : ListIcon()
    data class Vector(val imageVector: ImageVector) : ListIcon()
    data class Text(val text: String) : ListIcon()
    object None : ListIcon()
}

data class ListItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: ListIcon = ListIcon.None
)


enum class ListSource {
    SERVER,
    LOCALE
}

data class ListSetting(
    val apiPage: String,         // API path segment: "security", "privacy", "parentalcontrol"
    val apiFeat: String,         // API path segment: "tlds", "blocklists", "natives", etc.
    val localeCategory: String,  // Category in merged.json
    val localeKey: String,       // Key in merged.json
    val source: ListSource,      // Where available items come from
    val localePath: List<String>, // Path in merged.json for locale-sourced lists
    val parentPage: Page? = null,       // Which page to go back to
    val customTitle: String? = null,
    val allowsCustomInput: Boolean = false
) {
    enum class Page { SECURITY, PRIVACY, PARENTAL_CONTROL }

    fun title(): String = customTitle ?: Locales.getString(localeCategory, localeKey, "name")
    fun description(): String = Locales.getString(localeCategory, localeKey, "description")

}


object SecuritySettings {
    val toggles = listOf(
        ToggleSetting("threatIntelligenceFeeds", "feeds",             "security"),
        ToggleSetting("aiThreatDetection",       "ai",                "security"),
        ToggleSetting("googleSafeBrowsing",      "googleSafeBrowsing","security"),
        ToggleSetting("cryptojacking",           "cryptojacking",     "security"),
        ToggleSetting("dnsRebinding",            "dnsRebinding",      "security"),
        ToggleSetting("idnHomographs",           "homograph",         "security"),
        ToggleSetting("typosquatting",           "typosquatting",     "security"),
        ToggleSetting("dga",                     "dga",               "security"),
        ToggleSetting("nrd",                     "nrd",               "security"),
        ToggleSetting("ddns",                    "ddns",              "security"),
        ToggleSetting("parking",                 "parked",            "security"),
        ToggleSetting("csam",                    "csam",              "security"),
    )
    val lists = listOf(
        ListSetting(
            apiPage = "security",
            apiFeat = "tlds",
            localeCategory = "security",
            localeKey = "tld",
            source = ListSource.SERVER,
            localePath = emptyList(),
            parentPage = ListSetting.Page.SECURITY
        )
    )
}


object PrivacySettings {
    val toggles = listOf(
        ToggleSetting("disguisedTrackers", "disguised",  "privacy"),
        ToggleSetting("allowAffiliate",    "affiliate",  "privacy"),
    )
    val lists = listOf(
        ListSetting(
            apiPage = "privacy",
            apiFeat = "blocklists",
            localeCategory = "privacy",
            localeKey = "blocklists",
            source = ListSource.SERVER,
            localePath = emptyList(),
            parentPage = ListSetting.Page.PRIVACY
        ),
        ListSetting(
            apiPage = "privacy",
            apiFeat = "natives",
            localeCategory = "privacy",
            localeKey = "native",
            source = ListSource.LOCALE,
            localePath = listOf("privacy", "native", "systems"),
            parentPage = ListSetting.Page.PRIVACY
        ),
    )
}

object ParentalControlSettings {
    val toggles = listOf(
        ToggleSetting("safeSearch",            "safesearch",         "parentalControl"),
        ToggleSetting("youtubeRestrictedMode", "youtubeRestricted",  "parentalControl"),
        ToggleSetting("blockBypass",           "bypass",             "parentalControl"),
    )
    val lists = listOf(
        ListSetting(
            apiPage = "parentalcontrol",
            apiFeat = "services",
            localeCategory = "parentalControl",
            localeKey = "services",
            source = ListSource.SERVER,
            localePath = listOf("parentalControl", "services", "services"),
            parentPage = ListSetting.Page.PARENTAL_CONTROL
        ),
        ListSetting(
            apiPage = "parentalcontrol",
            apiFeat = "categories",
            localeCategory = "parentalControl",
            localeKey = "categories",
            source = ListSource.LOCALE,
            localePath = listOf("parentalControl", "categories", "categories"),
            parentPage = ListSetting.Page.PARENTAL_CONTROL
        ),
    )
}

object SettingsPageSettings {
    val toggles = listOf(

        ToggleSetting(
            apiPath = listOf("bav"),
            localeKey = "bav", category = "settings",
            customTitle = "Bypass Age Verification",
            customDescription = "Automatically bypass age verification checks used by certain websites, such as adult content sites, to verify a visitor’s age before allowing access."
        ),
        ToggleSetting(
            apiPath = listOf("web3"),
            localeKey = "web3", category = "settings",
            customTitle = "Web3",
            customDescription = "Enable Web3 domain resolution."
        ),

        // toggles under "logs"
        // TODO refer to webUI
        ToggleSetting(
            apiPath = listOf("logs", "enabled"),
            localeKey = "logs", category = "settings",
            customTitle = "Logs",
            customDescription = "Enable or disable query logging."
        ),
        ToggleSetting(
            apiPath = listOf("logs", "drop", "ip"),
            localeKey = "logs", category = "settings",
            customTitle = "Drop IP from Logs",
            customDescription = "Strip client IP addresses from all log entries."
        ),
        ToggleSetting(
            apiPath = listOf("logs", "drop", "domain"),
            localeKey = "logs", category = "settings",
            customTitle = "Drop Domains from Logs",
            customDescription = "Strip domain names from all log entries."
        ),

        // toggles under "blockPage"
        ToggleSetting(
            apiPath = listOf("blockPage", "enabled"),
            localeKey = "blockPage", category = "settings",
            customTitle = "Block Page",
            customDescription = "Show a block page when a domain is blocked."
        ),

        // toggles under "performance"
        ToggleSetting(
            apiPath = listOf("performance", "ecs"),
            localeKey = "performance", category = "settings",
            customTitle = "EDNS Client Subnet",
            customDescription = "Helps CDNs locate you more accurately for faster content delivery."
        ),
        ToggleSetting(
            apiPath = listOf("performance", "cacheBoost"),
            localeKey = "performance", category = "settings",
            customTitle = "Cache Boost",
            customDescription = "Boost DNS performance by increasing cache TTLs."
        ),
        ToggleSetting(
            apiPath = listOf("performance", "cnameFlattening"),
            localeKey = "performance", category = "settings",
            customTitle = "CNAME Flattening",
            customDescription = "Resolve CNAMEs to their final target for enhanced security."
        ),
    )

    val lists = emptyList<ListSetting>()
}

object DenyList {
    val toggles = emptyList<ToggleSetting>()
    val lists = listOf(
        ListSetting(
            apiPage = "denylist",
            apiFeat = "",
            localeCategory = "pages",
            localeKey = "denylist",
            source = ListSource.SERVER,
            localePath = emptyList(),
            customTitle = "Denylist",
            allowsCustomInput = true
        )
    )
}

object Allowlist {
    val toggles = emptyList<ToggleSetting>()
    val lists = listOf(
        ListSetting(
            apiPage = "allowlist",
            apiFeat = "",
            localeCategory = "pages",
            localeKey = "allowlist",
            source = ListSource.SERVER,
            localePath = emptyList(),
            allowsCustomInput = true
        )
    )
}