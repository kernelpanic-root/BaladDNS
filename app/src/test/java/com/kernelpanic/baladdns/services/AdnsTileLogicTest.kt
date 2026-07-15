package com.kernelpanic.baladdns.services

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdnsTileLogicTest {
    @Test
    fun `tile keeps its current state until an actual result arrives`() {
        assertFalse(resolveQuickTileState(currentState = false, actualState = null)!!)
        assertTrue(resolveQuickTileState(currentState = false, actualState = true)!!)
        assertFalse(resolveQuickTileState(currentState = true, actualState = false)!!)
    }
}
