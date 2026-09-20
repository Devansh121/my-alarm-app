package com.devansh.alarm.ui

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

/** "7:05 AM" style 12-hour clock. */
fun formatTime(hour: Int, minute: Int): String {
    val h12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val mm = minute.toString().padStart(2, '0')
    val suffix = if (hour < 12) "AM" else "PM"
    return "$h12:$mm $suffix"
}

private val WEEKDAYS = setOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
)
private val WEEKENDS = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

private val SHORT_NAMES = mapOf(
    DayOfWeek.MONDAY to "Mon", DayOfWeek.TUESDAY to "Tue", DayOfWeek.WEDNESDAY to "Wed",
    DayOfWeek.THURSDAY to "Thu", DayOfWeek.FRIDAY to "Fri", DayOfWeek.SATURDAY to "Sat",
    DayOfWeek.SUNDAY to "Sun",
)

/** Human summary of repeat days: Once / Every day / Weekdays / Weekends / "Mon, Fri". */
fun repeatSummary(days: Set<DayOfWeek>): String = when {
    days.isEmpty() -> "Once"
    days.size == 7 -> "Every day"
    days == WEEKDAYS -> "Weekdays"
    days == WEEKENDS -> "Weekends"
    else -> days.sortedBy { it.isoDayNumber }.joinToString(", ") { SHORT_NAMES.getValue(it) }
}
