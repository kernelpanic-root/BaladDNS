package com.kernelpanic.baladdns.data.provider

import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderSelectionMigrationTest {
    private val migration = ProviderSelectionMigration(DnsProviderCatalog.default)

    @Test
    fun `legacy preset ids map to typed standard selections`() {
        assertEquals(
            DnsProviderSelection.Standard(
                ProviderId("adguard"),
                ResolverPresetId("default"),
            ),
            migration.migrate(
                LegacyProviderSnapshot(selectedProviderId = "adguard_default")
            ).selection,
        )
        assertEquals(
            DnsProviderSelection.Standard(
                ProviderId("adguard"),
                ResolverPresetId("default"),
            ),
            migration.migrate(
                LegacyProviderSnapshot(customHostname = "dns.adguard-dns.com")
            ).selection,
        )
        assertEquals(
            DnsProviderSelection.Standard(
                ProviderId("google"),
                ResolverPresetId("default"),
            ),
            migration.migrate(
                LegacyProviderSnapshot(selectedProviderId = "google")
            ).selection,
        )
    }

    @Test
    fun `legacy cloudflare hostname is preserved as custom`() {
        assertEquals(
            DnsProviderSelection.Custom("cloudflare-dns.com"),
            migration.migrate(
                LegacyProviderSnapshot(selectedProviderId = "cloudflare")
            ).selection,
        )
    }

    @Test
    fun `unknown provider preserves a valid stored hostname as custom`() {
        assertEquals(
            DnsProviderSelection.Custom("dns.xn--nqv7f"),
            migration.migrate(
                LegacyProviderSnapshot(
                    selectedProviderId = "retired-provider",
                    customHostname = "DNS.机构.",
                )
            ).selection,
        )
    }

    @Test
    fun `nextdns selection and profile hostname remain independent`() {
        val result = migration.migrate(
            LegacyProviderSnapshot(
                selectedProviderId = "nextdns",
                enhancedHostname = "profile.dns.nextdns.io",
            )
        )

        assertEquals(
            DnsProviderSelection.Enhanced(ProviderId("nextdns")),
            result.selection,
        )
        assertEquals("profile.dns.nextdns.io", result.enhancedHostname)
    }

    @Test
    fun `missing or invalid legacy state uses the catalog default`() {
        val expected = DnsProviderSelection.Standard(
            ProviderId("adguard"),
            ResolverPresetId("default"),
        )

        assertEquals(expected, migration.migrate(LegacyProviderSnapshot()).selection)
        assertEquals(
            expected,
            migration.migrate(
                LegacyProviderSnapshot(
                    selectedProviderId = "unknown",
                    customHostname = "not a hostname",
                )
            ).selection,
        )
    }

    @Test
    fun `current typed selection is read without legacy reinterpretation`() {
        val result = migration.migrate(
            LegacyProviderSnapshot(
                migrationVersion = ProviderSelectionMigration.CURRENT_VERSION,
                selectedProviderId = "cloudflare",
                selectedPresetId = "family",
                customHostname = "cloudflare-dns.com",
            )
        )

        assertEquals(
            DnsProviderSelection.Standard(
                ProviderId("cloudflare"),
                ResolverPresetId("family"),
            ),
            result.selection,
        )
        assertEquals(ProviderSelectionMigration.CURRENT_VERSION, result.migrationVersion)
    }
}
