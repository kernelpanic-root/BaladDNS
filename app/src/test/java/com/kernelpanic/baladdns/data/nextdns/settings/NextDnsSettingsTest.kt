package com.kernelpanic.baladdns.data.nextdns.settings

import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NextDnsSettingsTest {

    @Test
    fun `reads nested values and returns null for a missing path`() {
        val settings = JsonParser.parseString(
            """{ "logs": { "drop": { "ip": false } } }"""
        ).asJsonObject

        assertFalse(settings.valueAt(listOf("logs", "drop", "ip"))!!.asBoolean)
        assertNull(settings.valueAt(listOf("logs", "retention")))
    }

    @Test
    fun `builds canonical nested patch payloads`() {
        assertEquals(
            mapOf("logs" to mapOf("retention" to 63_072_000)),
            nestedPayload(listOf("logs", "retention"), 63_072_000),
        )
        assertEquals(
            mapOf("logs" to mapOf("drop" to mapOf("domain" to true))),
            nestedPayload(listOf("logs", "drop", "domain"), true),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an empty patch path`() {
        nestedPayload(emptyList(), true)
    }

    @Test
    fun `log privacy booleans invert the API drop fields`() {
        val clientIps = NextDnsSettingRegistry.settings.settings
            .filterIsInstance<BooleanSettingSpec>()
            .first { it.id == SettingId("settings.logs.logClientIps") }

        val apiFalse = JsonParser.parseString("false")
        val apiTrue = JsonParser.parseString("true")

        assertTrue(clientIps.decode(apiFalse)!!)
        assertFalse(clientIps.decode(apiTrue)!!)
        assertEquals(false, clientIps.encode(true))
        assertEquals(true, clientIps.encode(false))
    }

    @Test
    fun `retention and location preserve observed values`() {
        val settings = NextDnsSettingRegistry.settings.settings
        val retention = settings.filterIsInstance<IntSelectSettingSpec>().single()
        val location = settings.filterIsInstance<StringSelectSettingSpec>().single()

        assertEquals(
            listOf(3_600, 21_600, 86_400, 604_800, 2_592_000, 7_776_000, 15_552_000, 31_536_000, 63_072_000),
            retention.options.map { it.value },
        )
        assertEquals("settings", retention.api.page)
        assertEquals(listOf("logs", "retention"), retention.api.path)
        assertEquals(listOf("us", "eu", "ch"), location.options.map { it.value })
        assertEquals(listOf("logs", "location"), location.api.path)
        assertTrue(location.confirmation?.destructive == true)
    }

    @Test
    fun `log detail settings are visible only while logging is enabled`() {
        val retention = NextDnsSettingRegistry.settings.settings
            .filterIsInstance<IntSelectSettingSpec>()
            .single()
        val visibility = requireNotNull(retention.visibleWhen)
        val logsEnabled = SettingId("settings.logs.enabled")

        assertFalse(visibility(mapOf(logsEnabled to JsonPrimitive(false))))
        assertTrue(visibility(mapOf(logsEnabled to JsonPrimitive(true))))
        assertFalse(visibility(emptyMap()))
    }
}
