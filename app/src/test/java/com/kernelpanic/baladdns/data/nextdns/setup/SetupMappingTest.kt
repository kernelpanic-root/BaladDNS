package com.kernelpanic.baladdns.data.nextdns.setup

import com.google.gson.JsonParser
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SetupMappingTest {

    @Test
    fun `setup mapping derives encrypted endpoints and uses returned linked IP servers`() {
        val root = JsonParser.parseString(
            """
            {
              "data": {
                "ipv4": [],
                "ipv6": ["2a07:a8c0::ef:ccb3", "2a07:a8c1::ef:ccb3"],
                "linkedIp": {
                  "servers": ["45.90.28.195", "45.90.30.195"],
                  "ip": "203.0.113.8",
                  "ddns": null,
                  "updateToken": "synthetic-update-token"
                },
                "dnscrypt": "sdns://synthetic"
              }
            }
            """.trimIndent(),
        ).asJsonObject

        val result = root.toSetupLoad("def456")

        assertEquals("def456.dns.nextdns.io", result.content.dnsOverTls)
        assertEquals("https://dns.nextdns.io/def456", result.content.dnsOverHttps)
        assertEquals("sdns://synthetic", result.content.dnscryptStamp)
        assertEquals(
            listOf("45.90.28.195", "45.90.30.195"),
            result.content.linkedIp.servers,
        )
        assertFalse(result.toString().contains("synthetic-update-token"))
    }

    @Test
    fun `setup steps sort numeric object keys and preserve array order`() {
        assertEquals(
            listOf("first", "second", "tenth"),
            normalizeSetupSteps(
                linkedMapOf(
                    "10" to "tenth",
                    "2" to "second",
                    "1" to "first",
                ),
            ),
        )
        assertEquals(
            listOf("first", "second"),
            normalizeSetupSteps(listOf("first", "second")),
        )
    }

    @Test
    fun `link action is hidden only when the current and linked IP match`() {
        org.junit.Assert.assertFalse(shouldShowLinkIp("203.0.113.8", "203.0.113.8"))
        org.junit.Assert.assertTrue(shouldShowLinkIp("203.0.113.8", "203.0.113.9"))
        org.junit.Assert.assertTrue(shouldShowLinkIp("203.0.113.8", null))
    }

    @Test
    fun `DDNS removal preserves an explicit null in the request body`() {
        val payload = ddnsPayload(null)

        assertEquals(1, payload.size())
        assertEquals(true, payload.get("ddns").isJsonNull)
    }

    @Test
    fun `DDNS removal sends an explicit JSON null on the wire`() {
        val body = ddnsRequestBody(null)
        val buffer = Buffer()

        body.writeTo(buffer)

        assertEquals("application", body.contentType()?.type)
        assertEquals("json", body.contentType()?.subtype)
        assertEquals("{\"ddns\":null}", buffer.readUtf8())
    }
}
