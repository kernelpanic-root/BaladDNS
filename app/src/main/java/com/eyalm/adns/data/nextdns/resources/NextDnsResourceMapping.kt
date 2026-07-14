package com.eyalm.adns.data.nextdns.resources

import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.nextdns.model.BuiltInListIcon
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.data.nextdns.model.nextDnsFaviconUrl
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.IDN
import java.net.URI
import java.time.Instant
import java.util.Locale

enum class BlocklistSort {
    Popularity,
    Name,
    Entries,
    Recent,
}

fun mapServerResourceItems(
    feature: String,
    data: JsonArray,
): List<NextDnsResourceItem> = data.mapIndexedNotNull { index, element ->
    val objectValue = element.takeIf { it.isJsonObject }?.asJsonObject
        ?: return@mapIndexedNotNull null
    val id = objectValue.stringOrNull("id") ?: return@mapIndexedNotNull null

    when (feature) {
        "tlds" -> NextDnsResourceItem(
            id = id,
            name = ".${IDN.toUnicode(id)}",
            spamhausRank = objectValue.intOrNull("spamhaus")
                ?.coerceAtLeast(0)
                ?: 0,
            sourceIndex = index,
        )

        "blocklists" -> NextDnsResourceItem(
            id = id,
            name = objectValue.stringOrNull("name")
                ?: recommendedBlocklistString(id, "name")
                ?: id,
            description = objectValue.stringOrNull("description")
                ?: recommendedBlocklistString(id, "description"),
            // website = objectValue.stringOrNull("website"),
            entries = objectValue.intOrNull("entries"),
            updatedOn = objectValue.stringOrNull("updatedOn"),
            sourceIndex = index,
        )

        "services" -> {
            val website = objectValue.stringOrNull("website")
            val hostname = website?.let(::hostnameFromUrl)
            NextDnsResourceItem(
                id = id,
                name = Locales.getString(
                    "parentalControl",
                    "services",
                    "services",
                    id,
                ).takeUnless { it.startsWith("[missing:") } ?: id,
                website = website,
                icon = hostname
                    ?.let(::nextDnsFaviconUrl)
                    ?.let(ListIcon::Url)
                    ?: ListIcon.None,
                sourceIndex = index,
            )
        }

        else -> NextDnsResourceItem(
            id = id,
            name = objectValue.stringOrNull("name") ?: id,
            description = objectValue.stringOrNull("description"),
            website = objectValue.stringOrNull("website"),
            icon = ListIcon.BuiltIn(BuiltInListIcon.Shield),
            sourceIndex = index,
        )
    }
}

fun orderResourceItems(
    items: List<NextDnsResourceItem>,
    feature: String,
    blocklistSort: BlocklistSort = BlocklistSort.Popularity,
): List<NextDnsResourceItem> = when (feature) {
    "tlds" -> items.sortedWith(
        compareByDescending<NextDnsResourceItem> { it.spamhausRank }
            .thenBy { it.id.lowercase(Locale.ROOT) }
    )

    "blocklists" -> when (blocklistSort) {
        BlocklistSort.Popularity -> items.sortedBy(NextDnsResourceItem::sourceIndex)
        BlocklistSort.Name -> items.sortedWith(
            compareBy<NextDnsResourceItem> {
                it.name.lowercase(Locale.ROOT)
            }.thenBy { it.id.lowercase(Locale.ROOT) }
        )

        BlocklistSort.Entries -> items.sortedWith(
            compareByDescending<NextDnsResourceItem> { it.entries != null }
                .thenByDescending { it.entries }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )

        BlocklistSort.Recent -> items.sortedWith(
            compareByDescending<NextDnsResourceItem> { it.updatedInstantOrNull() != null }
                .thenByDescending { it.updatedInstantOrNull() }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }

    else -> items
}

fun filterResourceItems(
    items: List<NextDnsResourceItem>,
    query: String,
    enabledOnly: Boolean,
    activeIds: Set<String>,
): List<NextDnsResourceItem> = items.filter { item ->
    (!enabledOnly || item.id in activeIds) &&
        (
            item.name.contains(query, ignoreCase = true) ||
                item.id.contains(query, ignoreCase = true) ||
                item.description?.contains(query, ignoreCase = true) == true
            )
}

internal fun NextDnsResourceItem.updatedInstantOrNull(): Instant? =
    updatedOn?.let { runCatching { Instant.parse(it) }.getOrNull() }

private fun recommendedBlocklistString(id: String, field: String): String? {
    if (id != "nextdns-recommended") return null
    return Locales.getString(
        "privacy",
        "blocklists",
        "blocklists",
        id,
        field,
    ).takeUnless { it.startsWith("[missing:") }
}

private fun hostnameFromUrl(value: String): String? = runCatching {
    URI(value).host
}.getOrNull()

private fun JsonObject.stringOrNull(name: String): String? =
    get(name)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString

private fun JsonObject.intOrNull(name: String): Int? =
    get(name)
        ?.takeIf { !it.isJsonNull && it.isJsonPrimitive }
        ?.let { runCatching { it.asInt }.getOrNull() }
