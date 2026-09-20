package com.devansh.alarm.ui

import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals

class AlarmFormattingTest {

    @Test
    fun formatsMorning() = assertEquals("7:05 AM", formatTime(7, 5))

    @Test
    fun formatsAfternoon() = assertEquals("6:30 PM", formatTime(18, 30))

    @Test
    fun midnightIsTwelveAm() = assertEquals("12:00 AM", formatTime(0, 0))

    @Test
    fun noonIsTwelvePm() = assertEquals("12:00 PM", formatTime(12, 0))

    @Test
    fun padsMinutes() = assertEquals("9:07 AM", formatTime(9, 7))

    @Test
    fun emptyIsOnce() = assertEquals("Once", repeatSummary(emptySet()))

    @Test
    fun allSevenIsEveryDay() = assertEquals("Every day", repeatSummary(DayOfWeek.entries.toSet()))

    @Test
    fun weekdaysDetected() = assertEquals(
        "Weekdays",
        repeatSummary(
            setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
            ),
        ),
    )

    @Test
    fun weekendsDetected() = assertEquals(
        "Weekends",
        repeatSummary(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
    )

    @Test
    fun customDaysListedInIsoOrder() = assertEquals(
        "Mon, Fri, Sun",
        repeatSummary(setOf(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY)),
    )
}
