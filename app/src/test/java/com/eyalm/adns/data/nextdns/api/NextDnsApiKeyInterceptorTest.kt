package com.eyalm.adns.data.nextdns.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextDnsApiKeyInterceptorTest {

    @Test
    fun `returns key only for an allowed host`() {
        val interceptor = NextDnsApiKeyInterceptor(
            apiKeyProvider = { "test-api-key" },
            allowedHosts = setOf("api.nextdns.io", "ipv4.api.nextdns.io"),
        )

        assertEquals("test-api-key", interceptor.apiKeyForHost("api.nextdns.io"))
        assertEquals("test-api-key", interceptor.apiKeyForHost("ipv4.api.nextdns.io"))
        assertNull(interceptor.apiKeyForHost("link-ip.nextdns.io"))
        assertNull(interceptor.apiKeyForHost("example.test"))
    }

    @Test
    fun `returns no key when signed out`() {
        val interceptor = NextDnsApiKeyInterceptor(
            apiKeyProvider = { null },
            allowedHosts = setOf("api.nextdns.io"),
        )

        assertNull(interceptor.apiKeyForHost("api.nextdns.io"))
    }
}
