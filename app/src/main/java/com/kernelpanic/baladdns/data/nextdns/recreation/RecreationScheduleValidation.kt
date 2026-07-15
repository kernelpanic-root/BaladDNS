package com.kernelpanic.baladdns.data.nextdns.recreation

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class RecreationTimeDraft(
    val start: String = "",
    val end: String = "",
    val enabled: Boolean = start.isNotBlank() || end.isNotBlank(),
)

enum class RecreationScheduleError {
    BothTimesRequired,
    InvalidTime,
    EndMustFollowStart,
}

data class RecreationScheduleSerialization(
    val times: Map<String, RecreationWindowDto>,
    val errors: Map<String, RecreationScheduleError>,
)

object RecreationScheduleValidation {
    val days = listOf(
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
    )

    fun serialize(drafts: Map<String, RecreationTimeDraft>): RecreationScheduleSerialization {
        val times = linkedMapOf<String, RecreationWindowDto>()
        val errors = linkedMapOf<String, RecreationScheduleError>()

        days.forEach { day ->
            val draft = drafts[day] ?: RecreationTimeDraft()
            if (!draft.enabled) return@forEach
            if (draft.start.isBlank() && draft.end.isBlank()) return@forEach
            if (draft.start.isBlank() || draft.end.isBlank()) {
                errors[day] = RecreationScheduleError.BothTimesRequired
                return@forEach
            }

            val start = parseTime(draft.start)
            val end = parseTime(draft.end)
            if (start == null || end == null) {
                errors[day] = RecreationScheduleError.InvalidTime
            } else if (start >= end) {
                errors[day] = RecreationScheduleError.EndMustFollowStart
            } else {
                times[day] = RecreationWindowDto(
                    start = start.format(wireTimeFormatter),
                    end = end.format(wireTimeFormatter),
                )
            }
        }
        return RecreationScheduleSerialization(times, errors)
    }

    fun toDraft(window: RecreationWindowDto): RecreationTimeDraft = RecreationTimeDraft(
        start = window.start.take(5),
        end = window.end.take(5),
        enabled = true,
    )

    private fun parseTime(value: String): LocalTime? =
        runCatching { LocalTime.parse(value, inputTimeFormatter) }.getOrNull()

    private val inputTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val wireTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
}
