package com.eyalm.adns.viewmodel

import com.eyalm.adns.data.activation.ActivationMode
import com.eyalm.adns.data.provider.DnsProviderSelection
import com.eyalm.adns.data.provider.ProviderId
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingStateCodecTest {
    @Test
    fun `codec round trips non-sensitive onboarding draft`() {
        val state = OnboardingFlowState(
            step = OnboardingStep.ActivationMode,
            draft = OnboardingDraft(
                providerSelection = DnsProviderSelection.Enhanced(ProviderId("nextdns")),
                mode = ActivationMode.NextDnsControlOnly,
                privilegedMethod = null,
                nextDnsProfileId = "29a59d",
            ),
        )

        assertEquals(state, OnboardingStateCodec.decode(OnboardingStateCodec.encode(state)))
    }
}
