package com.kernelpanic.baladdns.data.nextdns.recreation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecreationScheduleValidationTest {

    @Test
    fun `serializes valid time windows to the observed wire format`() {
        val result = RecreationScheduleValidation.serialize(
            mapOf("monday" to RecreationTimeDraft(start = "18:00", end = "20:30"))
        )

        assertTrue(result.errors.isEmpty())
        assertEquals(
            RecreationWindowDto(start = "18:00:00", end = "20:30:00"),
            result.times["monday"],
        )
    }

    @Test
    fun `omits empty days and rejects incomplete or overnight windows`() {
        val result = RecreationScheduleValidation.serialize(
            mapOf(
                "monday" to RecreationTimeDraft(),
                "tuesday" to RecreationTimeDraft(start = "18:00"),
                "wednesday" to RecreationTimeDraft(start = "20:30", end = "18:00"),
            )
        )

        assertTrue(result.times.isEmpty())
        assertEquals(RecreationScheduleError.BothTimesRequired, result.errors["tuesday"])
        assertEquals(RecreationScheduleError.EndMustFollowStart, result.errors["wednesday"])
    }

    @Test
    fun `omits a disabled day even when it has a previous time window`() {
        val result = RecreationScheduleValidation.serialize(
            mapOf(
                "monday" to RecreationTimeDraft(
                    start = "18:00",
                    end = "20:30",
                    enabled = false,
                )
            )
        )

        assertTrue(result.times.isEmpty())
        assertTrue(result.errors.isEmpty())
    }
}
