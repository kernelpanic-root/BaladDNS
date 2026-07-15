package com.kernelpanic.baladdns.data.dns

import com.kernelpanic.baladdns.data.PrivateDnsObservation
import com.kernelpanic.baladdns.data.provider.DnsProviderCatalog
import com.kernelpanic.baladdns.data.provider.DnsProviderSelection
import com.kernelpanic.baladdns.data.provider.LegacyProviderSnapshot
import com.kernelpanic.baladdns.data.provider.ProviderId
import com.kernelpanic.baladdns.data.provider.ProviderSelectionMigrationResult
import com.kernelpanic.baladdns.data.provider.ProviderSelectionRepository
import com.kernelpanic.baladdns.data.provider.ProviderSelectionStore
import com.kernelpanic.baladdns.data.provider.ResolverPresetId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsConfigurationRepositoryTest {
    @Test
    fun `active provider change is persisted only after verified device write`() = runTest {
        val selectionRepository = selectionRepository()
        val control = FakePrivateDnsControl(
            observation = PrivateDnsObservation.Hostname("dns.adguard-dns.com")
        )
        val repository = DnsConfigurationRepository(
            selectionRepository = selectionRepository,
            privateDnsControl = control,
            disableBehavior = { DnsDisableBehavior.Off },
        )
        val target = DnsProviderSelection.Standard(
            ProviderId("cloudflare"),
            ResolverPresetId("family"),
        )

        val result = repository.changeSelection(target)

        assertEquals(listOf("enable:family.cloudflare-dns.com"), control.operations)
        assertEquals(target, selectionRepository.selection.value)
        assertEquals(
            DnsConfigurationResult.Changed(target, appliedToDevice = true),
            result,
        )
    }

    @Test
    fun `failed active provider write preserves previous selection`() = runTest {
        val selectionRepository = selectionRepository()
        val control = FakePrivateDnsControl(
            observation = PrivateDnsObservation.Hostname("dns.adguard-dns.com"),
            nextResult = DnsWriteResult.Rejected(PrivateDnsObservation.Off),
        )
        val repository = DnsConfigurationRepository(
            selectionRepository = selectionRepository,
            privateDnsControl = control,
            disableBehavior = { DnsDisableBehavior.Off },
        )

        val result = repository.changeSelection(
            DnsProviderSelection.Standard(
                ProviderId("cloudflare"),
                ResolverPresetId("family"),
            )
        )

        assertTrue(result is DnsConfigurationResult.WriteFailed)
        assertEquals(defaultSelection, selectionRepository.selection.value)
    }

    @Test
    fun `inactive provider change persists without touching Android settings`() = runTest {
        val selectionRepository = selectionRepository()
        val control = FakePrivateDnsControl(PrivateDnsObservation.Off)
        val repository = DnsConfigurationRepository(
            selectionRepository = selectionRepository,
            privateDnsControl = control,
            disableBehavior = { DnsDisableBehavior.Off },
        )
        val target = DnsProviderSelection.Standard(
            ProviderId("google"),
            ResolverPresetId("default"),
        )

        val result = repository.changeSelection(target)

        assertEquals(target, selectionRepository.selection.value)
        assertTrue(control.operations.isEmpty())
        assertEquals(
            DnsConfigurationResult.Changed(target, appliedToDevice = false),
            result,
        )
    }

    @Test
    fun `permission-missing provider change does not pretend to succeed`() = runTest {
        val selectionRepository = selectionRepository()
        val repository = DnsConfigurationRepository(
            selectionRepository = selectionRepository,
            privateDnsControl = FakePrivateDnsControl(
                PrivateDnsObservation.PermissionMissing
            ),
            disableBehavior = { DnsDisableBehavior.Off },
        )

        val result = repository.changeSelection(
            DnsProviderSelection.Standard(
                ProviderId("google"),
                ResolverPresetId("default"),
            )
        )

        assertEquals(DnsConfigurationResult.PermissionMissing, result)
        assertEquals(defaultSelection, selectionRepository.selection.value)
    }

    @Test
    fun `toggle uses selected disable behavior and verified state`() = runTest {
        val selectionRepository = selectionRepository()
        val control = FakePrivateDnsControl(
            PrivateDnsObservation.Hostname("dns.adguard-dns.com")
        )
        val repository = DnsConfigurationRepository(
            selectionRepository = selectionRepository,
            privateDnsControl = control,
            disableBehavior = { DnsDisableBehavior.Automatic },
        )

        val result = repository.toggle()

        assertTrue(result is DnsConfigurationResult.StateChanged)
        assertEquals(listOf("disable:Automatic"), control.operations)
        assertFalse(repository.isSelectedResolverActive())
    }

    @Test
    fun `active nextdns profile change writes new hostname before persisting it`() = runTest {
        val selectionRepository = selectionRepository(
            LegacyProviderSnapshot(
                selectedProviderId = "nextdns",
                enhancedHostname = "old.dns.nextdns.io",
            )
        )
        val control = FakePrivateDnsControl(
            PrivateDnsObservation.Hostname("old.dns.nextdns.io")
        )
        val repository = DnsConfigurationRepository(
            selectionRepository = selectionRepository,
            privateDnsControl = control,
            disableBehavior = { DnsDisableBehavior.Off },
        )

        val result = repository.changeEnhancedSelection(
            providerId = ProviderId("nextdns"),
            hostname = "new.dns.nextdns.io",
        )

        assertEquals(listOf("enable:new.dns.nextdns.io"), control.operations)
        assertEquals("new.dns.nextdns.io", selectionRepository.resolveHostname())
        assertEquals(
            DnsConfigurationResult.Changed(
                DnsProviderSelection.Enhanced(ProviderId("nextdns")),
                appliedToDevice = true,
            ),
            result,
        )
    }

    private fun selectionRepository(
        initial: LegacyProviderSnapshot = LegacyProviderSnapshot(),
    ): ProviderSelectionRepository =
        ProviderSelectionRepository(
            store = object : ProviderSelectionStore {
                private var snapshot = initial

                override fun read(): LegacyProviderSnapshot = snapshot

                override fun write(result: ProviderSelectionMigrationResult) {
                    snapshot = LegacyProviderSnapshot(
                        migrationVersion = result.migrationVersion,
                        selectedProviderId = when (val value = result.selection) {
                            is DnsProviderSelection.Standard -> value.providerId.value
                            is DnsProviderSelection.Enhanced -> value.providerId.value
                            is DnsProviderSelection.Custom -> "custom"
                        },
                        selectedPresetId = (result.selection as? DnsProviderSelection.Standard)
                            ?.presetId
                            ?.value,
                        customHostname = result.customHostname,
                        enhancedHostname = result.enhancedHostname,
                    )
                }
            },
            catalog = DnsProviderCatalog.default,
        )

    private class FakePrivateDnsControl(
        var observation: PrivateDnsObservation,
        private var nextResult: DnsWriteResult? = null,
    ) : PrivateDnsControl {
        val operations = mutableListOf<String>()

        override fun observe(): PrivateDnsObservation = observation

        override suspend fun enable(hostname: String?): DnsWriteResult {
            operations += "enable:$hostname"
            return nextResult ?: DnsWriteResult.Success(
                PrivateDnsObservation.Hostname(requireNotNull(hostname))
            ).also { observation = it.observation }
        }

        override suspend fun disable(behavior: DnsDisableBehavior): DnsWriteResult {
            operations += "disable:$behavior"
            return nextResult ?: DnsWriteResult.Success(
                when (behavior) {
                    DnsDisableBehavior.Automatic -> PrivateDnsObservation.Automatic
                    DnsDisableBehavior.Off -> PrivateDnsObservation.Off
                }
            ).also { observation = it.observation }
        }
    }

    private companion object {
        val defaultSelection = DnsProviderSelection.Standard(
            ProviderId("adguard"),
            ResolverPresetId("default"),
        )
    }
}
