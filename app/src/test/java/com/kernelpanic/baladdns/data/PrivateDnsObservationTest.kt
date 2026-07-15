package com.kernelpanic.baladdns.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateDnsObservationTest {
    @Test
    fun `hostname is active only when it matches selected resolver`() {
        val observed = PrivateDnsObservation.Hostname("profile.dns.nextdns.io")

        assertTrue(
            isSelectedPrivateDnsActive(observed, "profile.dns.nextdns.io")
        )
        assertFalse(
            isSelectedPrivateDnsActive(observed, "dns.example")
        )
        assertFalse(isSelectedPrivateDnsActive(PrivateDnsObservation.Automatic, null))
        assertFalse(isSelectedPrivateDnsActive(PrivateDnsObservation.Off, null))
        assertFalse(
            isSelectedPrivateDnsActive(PrivateDnsObservation.PermissionMissing, null)
        )
    }

    @Test
    fun `hostname state comparison is case insensitive`() {
        assertTrue(
            isSelectedPrivateDnsActive(
                PrivateDnsObservation.Hostname("living--room-profile.dns.nextdns.io"),
                "Living--Room-profile.dns.nextdns.io",
            )
        )
    }
}
