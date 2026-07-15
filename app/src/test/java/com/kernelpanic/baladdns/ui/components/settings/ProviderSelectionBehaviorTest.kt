package com.kernelpanic.baladdns.ui.components.settings

import com.kernelpanic.baladdns.data.provider.DnsProviderSelection
import com.kernelpanic.baladdns.data.provider.EnhancedProviderDefinition
import com.kernelpanic.baladdns.data.provider.ProviderId
import com.kernelpanic.baladdns.data.provider.ResolverPreset
import com.kernelpanic.baladdns.data.provider.ResolverPresetId
import com.kernelpanic.baladdns.data.provider.StandardProviderDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderSelectionBehaviorTest {
    @Test
    fun `provider with multiple presets opens a dedicated options page`() {
        val provider = standardProvider(
            presets = listOf(preset("default"), preset("family")),
        )

        val action = resolveProviderSelectionAction(
            provider = provider,
            currentSelection = DnsProviderSelection.Standard(
                provider.id,
                ResolverPresetId("family"),
            ),
        )

        assertEquals(
            ProviderSelectionAction.OpenPresets(
                provider = provider,
                selectedPresetId = ResolverPresetId("family"),
            ),
            action,
        )
    }

    @Test
    fun `options for an inactive provider open without a selected preset`() {
        val provider = standardProvider(
            presets = listOf(preset("default"), preset("family")),
        )

        assertEquals(
            ProviderSelectionAction.OpenPresets(
                provider = provider,
                selectedPresetId = null,
            ),
            resolveProviderSelectionAction(
                provider = provider,
                currentSelection = DnsProviderSelection.Standard(
                    ProviderId("another-provider"),
                    ResolverPresetId("default"),
                ),
            ),
        )
    }

    @Test
    fun `options hint appears only on the selected provider with multiple presets`() {
        val provider = standardProvider(
            presets = listOf(preset("default"), preset("family")),
        )
        val selected = DnsProviderSelection.Standard(
            provider.id,
            ResolverPresetId("family"),
        )

        assertTrue(shouldShowProviderOptionsHint(provider, selected))
        assertTrue(!shouldShowProviderOptionsHint(provider, currentSelection = null))
        assertTrue(
            !shouldShowProviderOptionsHint(
                standardProvider(presets = listOf(preset("only"))),
                selected,
            )
        )
    }

    @Test
    fun `single preset and enhanced providers can be selected directly`() {
        val standard = standardProvider(presets = listOf(preset("only")))
        val enhanced = EnhancedProviderDefinition(
            id = ProviderId("enhanced"),
            titleRes = 1,
            descriptionRes = 2,
        )

        assertEquals(
            ProviderSelectionAction.Apply(
                DnsProviderSelection.Standard(
                    standard.id,
                    ResolverPresetId("only"),
                )
            ),
            resolveProviderSelectionAction(standard, currentSelection = null),
        )
        assertTrue(
            resolveProviderSelectionAction(enhanced, currentSelection = null) is
                ProviderSelectionAction.Apply
        )
    }

    private fun standardProvider(
        presets: List<ResolverPreset>,
    ) = StandardProviderDefinition(
        id = ProviderId("standard"),
        titleRes = 1,
        descriptionRes = 2,
        defaultPresetId = presets.first().id,
        presets = presets,
    )

    private fun preset(id: String) = ResolverPreset(
        id = ResolverPresetId(id),
        hostname = "$id.example.com",
        titleRes = 1,
        descriptionRes = 2,
    )
}
