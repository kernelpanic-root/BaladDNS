package com.eyalm.adns.data.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderSelectionSnapshotCodecTest {
    @Test
    fun `codec reads legacy preference keys`() {
        assertEquals(
            LegacyProviderSnapshot(
                migrationVersion = 0,
                selectedProviderId = "nextdns",
                selectedPresetId = null,
                customHostname = "custom.example",
                enhancedHostname = "profile.dns.nextdns.io",
            ),
            ProviderSelectionSnapshotCodec.decode(
                mapOf(
                    "selected_provider_id" to "nextdns",
                    "custom_url" to "custom.example",
                    "enhanced_url" to "profile.dns.nextdns.io",
                )
            )
        )
    }

    @Test
    fun `codec writes typed standard selection and clears stale preset only when needed`() {
        val standard = ProviderSelectionSnapshotCodec.encode(
            ProviderSelectionMigrationResult(
                selection = DnsProviderSelection.Standard(
                    ProviderId("cloudflare"),
                    ResolverPresetId("security"),
                ),
                customHostname = "custom.example",
                enhancedHostname = "profile.dns.nextdns.io",
                migrationVersion = 1,
            )
        )
        val custom = ProviderSelectionSnapshotCodec.encode(
            ProviderSelectionMigrationResult(
                selection = DnsProviderSelection.Custom("custom.example"),
                customHostname = "custom.example",
                enhancedHostname = "profile.dns.nextdns.io",
                migrationVersion = 1,
            )
        )

        assertEquals("cloudflare", standard["selected_provider_id"])
        assertEquals("security", standard["selected_provider_preset_id"])
        assertEquals("custom.example", standard["custom_url"])
        assertEquals("profile.dns.nextdns.io", standard["enhanced_url"])
        assertEquals(1, standard["provider_selection_migration_version"])
        assertEquals("custom", custom["selected_provider_id"])
        assertNull(custom["selected_provider_preset_id"])
    }
}
