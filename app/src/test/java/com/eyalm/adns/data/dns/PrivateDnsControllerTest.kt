package com.eyalm.adns.data.dns

import com.eyalm.adns.data.PrivateDnsObservation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateDnsControllerTest {
    @Test
    fun `enable writes hostname then mode and verifies readback`() = runTest {
        val settings = FakePrivateDnsSettings()
        val controller = PrivateDnsController(settings)

        val result = controller.enable("DNS.Example.")

        assertEquals(
            listOf(
                "specifier=DNS.Example",
                "mode=hostname",
            ),
            settings.writes,
        )
        assertEquals(
            DnsWriteResult.Success(PrivateDnsObservation.Hostname("DNS.Example")),
            result,
        )
    }

    @Test
    fun `disable maps explicit behavior to Android mode`() = runTest {
        val settings = FakePrivateDnsSettings(
            mode = "hostname",
            specifier = "dns.example",
        )
        val controller = PrivateDnsController(settings)

        assertEquals(
            DnsWriteResult.Success(PrivateDnsObservation.Off),
            controller.disable(DnsDisableBehavior.Off),
        )
        assertEquals("off", settings.mode)

        assertEquals(
            DnsWriteResult.Success(PrivateDnsObservation.Automatic),
            controller.disable(DnsDisableBehavior.Automatic),
        )
        assertEquals("opportunistic", settings.mode)
    }

    @Test
    fun `invalid hostname is rejected before Android settings are touched`() = runTest {
        val settings = FakePrivateDnsSettings()

        val result = PrivateDnsController(settings).enable("https://dns.example")

        assertEquals(DnsWriteResult.MissingHostname, result)
        assertTrue(settings.writes.isEmpty())
    }

    @Test
    fun `security exception returns permission missing`() = runTest {
        val settings = FakePrivateDnsSettings(throwSecurityException = true)

        val result = PrivateDnsController(settings).enable("dns.example")

        assertEquals(DnsWriteResult.PermissionMissing, result)
    }

    @Test
    fun `failed write or mismatched readback is rejected`() = runTest {
        val rejectedWrite = PrivateDnsController(
            FakePrivateDnsSettings(acceptWrites = false)
        ).enable("dns.example")
        val mismatchedReadback = PrivateDnsController(
            FakePrivateDnsSettings(ignoreModeWrites = true)
        ).enable("dns.example")

        assertTrue(rejectedWrite is DnsWriteResult.Rejected)
        assertTrue(mismatchedReadback is DnsWriteResult.Rejected)
    }

    private class FakePrivateDnsSettings(
        var mode: String? = "off",
        var specifier: String? = null,
        private val acceptWrites: Boolean = true,
        private val ignoreModeWrites: Boolean = false,
        private val throwSecurityException: Boolean = false,
    ) : PrivateDnsSettings {
        val writes = mutableListOf<String>()

        override fun readMode(): String? = mode

        override fun readSpecifier(): String? = specifier

        override fun writeMode(value: String): Boolean {
            if (throwSecurityException) throw SecurityException("missing")
            writes += "mode=$value"
            if (acceptWrites && !ignoreModeWrites) mode = value
            return acceptWrites
        }

        override fun writeSpecifier(value: String): Boolean {
            if (throwSecurityException) throw SecurityException("missing")
            writes += "specifier=$value"
            if (acceptWrites) specifier = value
            return acceptWrites
        }
    }
}
