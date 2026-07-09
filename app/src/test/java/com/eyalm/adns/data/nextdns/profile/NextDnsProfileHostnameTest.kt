package com.eyalm.adns.data.nextdns.profile

import com.eyalm.adns.data.provider.PrivateDnsHostname
import org.junit.Assert.assertEquals
import org.junit.Test

class NextDnsProfileHostnameTest {
    @Test
    fun `profile hostname uses ADNS as the default device name`() {
        assertEquals(
            "ADNS-29a59d.dns.nextdns.io",
            nextDnsProfileHostname("29a59d"),
        )
    }

    @Test
    fun `profile hostname encodes device-name spaces without changing profile id`() {
        assertEquals(
            "Living--Room-29a59d.dns.nextdns.io",
            nextDnsProfileHostname("29a59d", " Living Room "),
        )
        assertEquals(
            "29a59d.dns.nextdns.io",
            nextDnsProfileHostname("29a59d", "   "),
        )
    }

    @Test
    fun `private DNS validation preserves NextDNS device-name case`() {
        val hostname = nextDnsProfileHostname("29a59d", "Living Room")

        assertEquals(
            "Living--Room-29a59d.dns.nextdns.io",
            PrivateDnsHostname.parsePreservingCase(hostname)?.ascii,
        )
    }
}
