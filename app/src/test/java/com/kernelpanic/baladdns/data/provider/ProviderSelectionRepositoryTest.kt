package com.kernelpanic.baladdns.data.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderSelectionRepositoryTest {
    @Test
    fun `repository migrates legacy state once and persists typed selection`() {
        val store = FakeProviderSelectionStore(
            LegacyProviderSnapshot(selectedProviderId = "cloudflare")
        )

        val first = ProviderSelectionRepository(store, DnsProviderCatalog.default)
        val second = ProviderSelectionRepository(store, DnsProviderCatalog.default)

        assertEquals(
            DnsProviderSelection.Custom("cloudflare-dns.com"),
            first.selection.value,
        )
        assertEquals(first.selection.value, second.selection.value)
        assertEquals(ProviderSelectionMigration.CURRENT_VERSION, store.snapshot.migrationVersion)
        assertEquals("custom", store.snapshot.selectedProviderId)
        assertEquals("cloudflare-dns.com", store.snapshot.customHostname)
        assertEquals(1, store.writeCount)
    }

    @Test
    fun `repository validates updates and resolves selected hostname`() {
        val store = FakeProviderSelectionStore(LegacyProviderSnapshot())
        val repository = ProviderSelectionRepository(store, DnsProviderCatalog.default)
        val selection = DnsProviderSelection.Standard(
            ProviderId("cloudflare"),
            ResolverPresetId("family"),
        )

        assertTrue(repository.save(selection) is ProviderSelectionUpdateResult.Saved)
        assertEquals(selection, repository.selection.value)
        assertEquals("family.cloudflare-dns.com", repository.resolveHostname())
        assertTrue(
            repository.save(
                DnsProviderSelection.Standard(
                    ProviderId("cloudflare"),
                    ResolverPresetId("missing"),
                )
            ) is ProviderSelectionUpdateResult.Invalid
        )
        assertEquals(selection, repository.selection.value)
    }

    @Test
    fun `enhanced selection resolves separately stored profile hostname`() {
        val store = FakeProviderSelectionStore(
            LegacyProviderSnapshot(
                selectedProviderId = "nextdns",
                enhancedHostname = "profile.dns.nextdns.io",
            )
        )
        val repository = ProviderSelectionRepository(store, DnsProviderCatalog.default)

        assertEquals("profile.dns.nextdns.io", repository.resolveHostname())
        assertEquals("profile.dns.nextdns.io", repository.resolvedHostname.value)
        assertTrue(
            repository.setEnhancedHostname("other.dns.nextdns.io")
                is ProviderSelectionUpdateResult.Saved
        )
        assertEquals("other.dns.nextdns.io", repository.resolveHostname())
        assertEquals("other.dns.nextdns.io", repository.resolvedHostname.value)
    }

    private class FakeProviderSelectionStore(
        initial: LegacyProviderSnapshot,
    ) : ProviderSelectionStore {
        var snapshot = initial
        var writeCount = 0

        override fun read(): LegacyProviderSnapshot = snapshot

        override fun write(result: ProviderSelectionMigrationResult) {
            writeCount++
            snapshot = LegacyProviderSnapshot(
                migrationVersion = result.migrationVersion,
                selectedProviderId = when (val selection = result.selection) {
                    is DnsProviderSelection.Standard -> selection.providerId.value
                    is DnsProviderSelection.Enhanced -> selection.providerId.value
                    is DnsProviderSelection.Custom -> ProviderSelectionMigration.CUSTOM_PROVIDER_ID
                },
                selectedPresetId = (result.selection as? DnsProviderSelection.Standard)
                    ?.presetId
                    ?.value,
                customHostname = result.customHostname,
                enhancedHostname = result.enhancedHostname,
            )
        }
    }
}
