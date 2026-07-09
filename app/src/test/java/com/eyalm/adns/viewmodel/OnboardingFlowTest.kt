package com.eyalm.adns.viewmodel

import com.eyalm.adns.data.activation.ActivationMode
import com.eyalm.adns.data.provider.DnsProviderCatalog
import com.eyalm.adns.data.provider.DnsProviderSelection
import com.eyalm.adns.data.provider.ProviderId
import com.eyalm.adns.data.provider.ResolverPresetId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFlowTest {
    @Test
    fun `standard provider with presets routes through preset and privileged activation`() {
        val flow = OnboardingFlow(DnsProviderCatalog.default)

        flow.dispatch(OnboardingIntent.Continue)
        assertEquals(OnboardingStep.Provider, flow.state.value.step)

        flow.dispatch(
            OnboardingIntent.ProviderSelected(
                DnsProviderSelection.Standard(
                    ProviderId("adguard"),
                    ResolverPresetId("default"),
                )
            )
        )
        assertEquals(OnboardingStep.Preset, flow.state.value.step)

        flow.dispatch(
            OnboardingIntent.PresetSelected(
                DnsProviderSelection.Standard(
                    ProviderId("adguard"),
                    ResolverPresetId("family"),
                )
            )
        )
        assertEquals(OnboardingStep.ActivationMethod, flow.state.value.step)
        assertEquals(
            ActivationMode.PrivilegedDnsControl,
            flow.state.value.draft.mode,
        )

        flow.dispatch(OnboardingIntent.ActivationMethodSelected(PrivilegedMethod.Adb))
        assertEquals(OnboardingStep.Adb, flow.state.value.step)
        flow.dispatch(OnboardingIntent.ActivationGranted)
        assertEquals(OnboardingStep.Success, flow.state.value.step)
    }

    @Test
    fun `single-preset and custom providers skip preset choice`() {
        val google = OnboardingFlow(DnsProviderCatalog.default).apply {
            dispatch(OnboardingIntent.Continue)
            dispatch(
                OnboardingIntent.ProviderSelected(
                    DnsProviderSelection.Standard(
                        ProviderId("google"),
                        ResolverPresetId("default"),
                    )
                )
            )
        }
        val custom = OnboardingFlow(DnsProviderCatalog.default).apply {
            dispatch(OnboardingIntent.Continue)
            dispatch(
                OnboardingIntent.ProviderSelected(
                    DnsProviderSelection.Custom("dns.example")
                )
            )
        }

        assertEquals(OnboardingStep.ActivationMethod, google.state.value.step)
        assertEquals(OnboardingStep.ActivationMethod, custom.state.value.step)
    }

    @Test
    fun `invalid custom hostname stays on provider choice`() {
        val flow = OnboardingFlow(DnsProviderCatalog.default)
        flow.dispatch(OnboardingIntent.Continue)

        flow.dispatch(
            OnboardingIntent.ProviderSelected(
                DnsProviderSelection.Custom("https://dns.example")
            )
        )

        assertEquals(OnboardingStep.Provider, flow.state.value.step)
    }

    @Test
    fun `nextdns offers control-only only after profile selection`() {
        val flow = OnboardingFlow(DnsProviderCatalog.default)
        flow.dispatch(OnboardingIntent.Continue)
        flow.dispatch(
            OnboardingIntent.ProviderSelected(
                DnsProviderSelection.Enhanced(ProviderId("nextdns"))
            )
        )

        assertEquals(OnboardingStep.ProviderLogin, flow.state.value.step)
        assertFalse(flow.state.value.controlOnlyEligible)

        flow.dispatch(OnboardingIntent.ProviderLoginCompleted("29a59d"))

        assertEquals(OnboardingStep.ActivationMode, flow.state.value.step)
        assertTrue(flow.state.value.controlOnlyEligible)

        flow.dispatch(OnboardingIntent.ModeSelected(ActivationMode.NextDnsControlOnly))

        assertEquals(OnboardingStep.Success, flow.state.value.step)
        assertEquals(
            ActivationMode.NextDnsControlOnly,
            flow.state.value.draft.mode,
        )
    }

    @Test
    fun `back transitions follow the actual branch`() {
        val flow = OnboardingFlow(DnsProviderCatalog.default)
        flow.dispatch(OnboardingIntent.Continue)
        flow.dispatch(
            OnboardingIntent.ProviderSelected(
                DnsProviderSelection.Enhanced(ProviderId("nextdns"))
            )
        )
        flow.dispatch(OnboardingIntent.ProviderLoginCompleted("29a59d"))
        flow.dispatch(OnboardingIntent.ModeSelected(ActivationMode.PrivilegedDnsControl))

        assertEquals(OnboardingStep.ActivationMethod, flow.state.value.step)
        flow.dispatch(OnboardingIntent.Back)
        assertEquals(OnboardingStep.ActivationMode, flow.state.value.step)
        flow.dispatch(OnboardingIntent.Back)
        assertEquals(OnboardingStep.ProviderLogin, flow.state.value.step)
    }

    @Test
    fun `non-sensitive draft can be restored after process recreation`() {
        val original = OnboardingFlow(DnsProviderCatalog.default)
        original.dispatch(OnboardingIntent.Continue)
        original.dispatch(
            OnboardingIntent.ProviderSelected(
                DnsProviderSelection.Enhanced(ProviderId("nextdns"))
            )
        )
        original.dispatch(OnboardingIntent.ProviderLoginCompleted("29a59d"))

        val restored = OnboardingFlow(
            catalog = DnsProviderCatalog.default,
            initialState = original.state.value,
        )

        assertEquals(original.state.value, restored.state.value)
        assertEquals(OnboardingStep.ActivationMode, restored.state.value.step)
    }
}
