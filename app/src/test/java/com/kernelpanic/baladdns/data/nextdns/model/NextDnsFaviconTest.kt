package com.kernelpanic.baladdns.data.nextdns.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextDnsFaviconTest {
    @Test
    fun `valid hostname is normalized before building url`() {
        assertEquals(
            "https://favicons.nextdns.io/hex:6578616d706c652e636f6d@3x.png",
            buildNextDnsFaviconUrl("Example.COM.") { it == "example.com" },
        )
    }

    @Test
    fun `ip addresses and arbitrary ids do not request favicons`() {
        assertNull(buildNextDnsFaviconUrl("1.1.1.1") { true })
        assertNull(buildNextDnsFaviconUrl("2001:4860:4860::8888") { true })
        assertNull(buildNextDnsFaviconUrl("nextdns-recommended") { false })
        assertNull(buildNextDnsFaviconUrl("") { false })
    }
}
