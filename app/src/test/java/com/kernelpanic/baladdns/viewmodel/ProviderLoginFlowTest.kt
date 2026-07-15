package com.kernelpanic.baladdns.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderLoginFlowTest {
    @Test
    fun `login flow owns navigation independently of an activity`() {
        val flow = ProviderLoginFlow()

        flow.authenticated()
        assertEquals(ProviderLoginStep.Profile, flow.state.value.step)

        flow.profileSelected("29a59d")
        assertEquals(ProviderLoginStep.Success, flow.state.value.step)
        assertEquals("29a59d", flow.state.value.selectedProfileId)

        flow.back()
        assertEquals(ProviderLoginStep.Profile, flow.state.value.step)
    }
}
