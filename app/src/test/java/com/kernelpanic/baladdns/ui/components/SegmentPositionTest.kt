package com.kernelpanic.baladdns.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SegmentPositionTest {
    @Test
    fun `single visible item uses single position`() {
        assertEquals(SegmentPosition.Single, segmentPosition(index = 0, size = 1))
    }

    @Test
    fun `multi item group resolves first middle and last`() {
        assertEquals(SegmentPosition.First, segmentPosition(index = 0, size = 3))
        assertEquals(SegmentPosition.Middle, segmentPosition(index = 1, size = 3))
        assertEquals(SegmentPosition.Last, segmentPosition(index = 2, size = 3))
    }

    @Test
    fun `invalid positions fail fast`() {
        assertThrows(IllegalArgumentException::class.java) {
            segmentPosition(index = 0, size = 0)
        }
        assertThrows(IndexOutOfBoundsException::class.java) {
            segmentPosition(index = 2, size = 2)
        }
    }
}
