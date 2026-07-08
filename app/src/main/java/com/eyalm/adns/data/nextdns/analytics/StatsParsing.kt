package com.eyalm.adns.data.nextdns.analytics

import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.data.nextdns.model.BuiltInListIcon
import com.eyalm.adns.data.nextdns.model.countryFlag
import com.eyalm.adns.data.nextdns.model.nextDnsFaviconUrl

import android.icu.text.NumberFormat
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.Locale

private fun fmt(n: Int): String = NumberFormat.getNumberInstance(Locale.US).format(n)
private fun JsonArray.objs(): List<JsonObject> = this.map { it.asJsonObject }
private fun JsonObject.str(k: String): String? = get(k)?.takeIf { !it.isJsonNull }?.asString
private fun JsonObject.int(k: String): Int = get(k)?.takeIf { !it.isJsonNull }?.asInt ?: 0

fun fmtPercent(p: Float): String =
    if (p % 1f == 0f) "${p.toInt()}%" else String.format(Locale.US, "%.2f%%", p)


internal fun parseList(card: ListCard, data: JsonArray): List<StatRow> = when (card.kind) {

    ListKind.DOMAINS -> data.objs().map { o ->
        val d = o.str("domain") ?: ""
        StatRow(
            id = d, title = d, highlightDomain = true,
            value = fmt(o.int("queries")),
            icon = nextDnsFaviconUrl(d)?.let(ListIcon::Url) ?: ListIcon.None,
        )
    }

    ListKind.REASONS -> data.objs().map { o ->
        StatRow(
            id = o.str("id") ?: "",
            title = o.str("name") ?: o.str("id") ?: "",
            value = fmt(o.int("queries"))
        )
    }

    ListKind.DEVICES -> data.objs().map { o ->
        val id = o.str("id") ?: ""
        val name = o.str("name") ?: if (id == "__UNIDENTIFIED__")
            Locales.getString("analytics", "devices", "unidentified", "name") else id
        StatRow(
            id = id, title = name, value = fmt(o.int("queries")),
        )
    }

    ListKind.IPS -> data.objs().map { o ->
        val net = o.getAsJsonObject("network")
        val geo = o.getAsJsonObject("geo")
        val isp = net?.str("isp")
        val loc = listOfNotNull(geo?.str("city"), geo?.str("country"))
            .joinToString(", ").ifEmpty { null }
        val cellular = net?.get("cellular")?.takeIf { !it.isJsonNull }?.asBoolean == true
        StatRow(
            id = o.str("ip") ?: "", title = o.str("ip") ?: "",
            subtitle = listOfNotNull(isp, loc).joinToString("  •  ").ifEmpty { null },
            value = fmt(o.int("queries")),
            icon = ListIcon.BuiltIn(
                if (cellular) BuiltInListIcon.SignalCellular
                else BuiltInListIcon.Wifi
            )
        )
    }

    ListKind.COUNTRIES -> data.objs().take(card.limit ?: 8).map { o ->
        val code = o.str("code") ?: ""
        StatRow(
            id = code, title = o.str("name") ?: code,
            subtitle = o.getAsJsonArray("domains")
                ?.take(3)?.joinToString(", ") { it.asString }?.ifEmpty { null },
            value = fmt(o.int("queries")),
            icon = ListIcon.Text(countryFlag(code))
        )
    }
}

internal fun parsePercent(card: PercentCard, data: JsonArray): Float {
    val flag = if (card.kind == PercentKind.ENCRYPTED) "encrypted" else "validated"
    val total = data.objs().sumOf { it.int("queries") }.coerceAtLeast(1)
    val on = data.objs()
        .filter { it.get(flag)?.takeIf { e -> !e.isJsonNull }?.asBoolean == true }
        .sumOf { it.int("queries") }
    return on.toFloat() / total * 100f
}
