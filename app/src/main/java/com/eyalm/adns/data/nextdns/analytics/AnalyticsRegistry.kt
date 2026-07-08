package com.eyalm.adns.data.nextdns.analytics

import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.nextdns.model.ListIcon

// [icon] title / subtitle ........ value
data class StatRow(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val value: String,
    val icon: ListIcon = ListIcon.None,
    val highlightDomain: Boolean = false
)

sealed class StatCard {
    abstract val key: String  // e.g. "domains.blocked"
    abstract val feature: String
    abstract val params: Map<String, String>   // base params, WITHOUT "from"
    abstract val localePath: List<String> // -> name/description node
    abstract val emptyPath: List<String>  // -> "empty" string

    fun title()       = Locales.getString(*(localePath + "name").toTypedArray())
    fun description() = Locales.getString(*(localePath + "description").toTypedArray())
    fun emptyText()   = Locales.getString(*emptyPath.toTypedArray())
}

enum class ListKind { DOMAINS, REASONS, DEVICES, IPS, COUNTRIES }

data class ListCard(
    override val key: String,
    override val feature: String,
    override val params: Map<String, String>,
    override val localePath: List<String>,
    override val emptyPath: List<String>,
    val kind: ListKind,
    val limit: Int? = null
) : StatCard()

enum class PercentKind { ENCRYPTED, DNSSEC }

data class PercentCard(
    override val key: String,
    override val feature: String,
    override val params: Map<String, String>,
    override val localePath: List<String>,
    override val emptyPath: List<String>,
    val kind: PercentKind
) : StatCard()


object StatsRegistry {
    val cards: List<StatCard> = listOf(
        ListCard("domains.resolved", "domains",
            params = mapOf("status" to "default,allowed", "limit" to "6"),
            localePath = listOf("analytics", "domains", "resolved"),
            emptyPath  = listOf("analytics", "domains", "empty"),
            kind = ListKind.DOMAINS),

        ListCard("domains.blocked", "domains",
            params = mapOf("status" to "blocked", "limit" to "6"),
            localePath = listOf("analytics", "domains", "blocked"),
            emptyPath  = listOf("analytics", "domains", "empty"),
            kind = ListKind.DOMAINS),

        ListCard("reasons", "reasons",
            params = mapOf("limit" to "6", "lang" to "en"),
            localePath = listOf("analytics", "reasons"),
            emptyPath  = listOf("analytics", "reasons", "empty"),
            kind = ListKind.REASONS),

        ListCard("devices", "devices",
            params = mapOf("limit" to "4"),
            localePath = listOf("analytics", "devices"),
            emptyPath  = listOf("analytics", "devices", "empty"),
            kind = ListKind.DEVICES),

        ListCard("ips", "ips",
            params = mapOf("limit" to "4", "lang" to "en"),
            localePath = listOf("analytics", "ips"),
            emptyPath  = listOf("analytics", "ips", "empty"),
            kind = ListKind.IPS),

        ListCard("domains.root", "domains",
            params = mapOf("root" to "true", "limit" to "6"),
            localePath = listOf("analytics", "domains", "root"),
            emptyPath  = listOf("analytics", "domains", "empty"),
            kind = ListKind.DOMAINS),

        PercentCard("encryption", "encryption",
            params = emptyMap(),
            localePath = listOf("analytics", "encrypted"),
            emptyPath  = listOf("analytics", "encrypted", "empty"),
            kind = PercentKind.ENCRYPTED),

        PercentCard("dnssec", "dnssec",
            params = emptyMap(),
            localePath = listOf("analytics", "dnssec"),
            emptyPath  = listOf("analytics", "dnssec", "empty"),
            kind = PercentKind.DNSSEC),

        ListCard("destinations.countries", "destinations",
            params = mapOf("type" to "countries", "limit" to "20", "lang" to "en"),
            localePath = listOf("analytics", "destination"),
            emptyPath  = listOf("analytics", "destination", "empty"),
            kind = ListKind.COUNTRIES,
            limit = 8),
    )
}
