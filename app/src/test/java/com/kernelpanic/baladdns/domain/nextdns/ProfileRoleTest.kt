package com.kernelpanic.baladdns.domain.nextdns

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRoleTest {

    @Test
    fun `owner can manage its profile`() {
        val capabilities = ProfileRole.Owner.capabilities()

        assertTrue(capabilities.canEditSettings)
        assertTrue(capabilities.canManageAccess)
        assertTrue(capabilities.canDelete)
        assertFalse(capabilities.canLeave)
    }

    @Test
    fun `editor and viewer receive the observed restricted capabilities`() {
        val editor = ProfileRole.Editor.capabilities()
        val viewer = ProfileRole.Viewer.capabilities()

        assertTrue(editor.canEditSettings)
        assertTrue(editor.canLeave)
        assertFalse(editor.canManageAccess)
        assertFalse(editor.canDelete)

        assertFalse(viewer.canEditSettings)
        assertTrue(viewer.canLeave)
        assertFalse(viewer.canManageAccess)
    }

    @Test
    fun `unknown role does not enable mutation`() {
        val capabilities = profileRoleFromWire("new-role").capabilities()

        assertFalse(capabilities.canEditSettings)
        assertFalse(capabilities.canDelete)
        assertFalse(capabilities.canLeave)
    }
}
