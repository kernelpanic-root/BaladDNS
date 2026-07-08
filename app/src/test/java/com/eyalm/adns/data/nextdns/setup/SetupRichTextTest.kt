package com.eyalm.adns.data.nextdns.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SetupRichTextTest {

    @Test
    fun `interpolates values and preserves numbered tag emphasis`() {
        val segments = GuideRichText(
            template = "Enter <1>{{endpoint}}</1> then save.",
            values = mapOf("endpoint" to "abc123.dns.nextdns.io"),
        ).segments()

        assertEquals(
            listOf(
                GuideTextSegment("Enter ", emphasized = false),
                GuideTextSegment("abc123.dns.nextdns.io", emphasized = true),
                GuideTextSegment(" then save.", emphasized = false),
            ),
            segments,
        )
    }

    @Test
    fun `keeps an unresolved placeholder visible`() {
        val segments = GuideRichText("Use {{endpoint}}.").segments()

        assertEquals(listOf(GuideTextSegment("Use {{endpoint}}.", false)), segments)
    }

    @Test
    fun `link IP capability redacts its token in string form`() {
        val capability = LinkIpCapability("abc123", "synthetic-token")

        assertFalse(capability.toString().contains("sensitive-token"))
    }
}
