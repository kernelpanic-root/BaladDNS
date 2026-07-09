package com.eyalm.adns.ui.screens.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSettingsVisibilityTest {
    @Test
    fun `device name editor is available only with private DNS control`() {
        assertTrue(shouldShowDeviceNameEditor(canControlPrivateDns = true))
        assertFalse(shouldShowDeviceNameEditor(canControlPrivateDns = false))
    }
}
