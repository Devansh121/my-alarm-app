package com.devansh.alarm.domain

import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AlarmTest {
    @Test fun defaultsMatchSpec() {
        val a = Alarm(id = "a1", hour = 7, minute = 30)
        assertEquals(emptySet<DayOfWeek>(), a.repeatDays)
        assertEquals("Alarm", a.label)
        assertEquals(ToneSelection.Random, a.tone)
        assertEquals(true, a.snoozeEnabled)
        assertEquals(9, a.snoozeMinutes)
        assertEquals(true, a.enabled)
    }
    @Test fun repeatDaysRoundTripsThroughCsv() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        assertEquals(days, repeatDaysFromCsv(repeatDaysToCsv(days)))
        assertEquals(emptySet<DayOfWeek>(), repeatDaysFromCsv(""))
    }

    @Test fun repeatDaysFromCsvIgnoresTrailingEmptyToken() {
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), repeatDaysFromCsv("1,3,"))
    }
    @Test fun repeatDaysFromCsvTrimsWhitespace() {
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), repeatDaysFromCsv(" 1, 5"))
    }
    @Test fun repeatDaysFromCsvIgnoresNonNumericTokens() {
        assertEquals(emptySet<DayOfWeek>(), repeatDaysFromCsv("abc"))
    }
    @Test fun repeatDaysFromCsvIgnoresOutOfRangeTokens() {
        assertEquals(emptySet<DayOfWeek>(), repeatDaysFromCsv("0,8"))
    }
    @Test fun repeatDaysFromCsvBlankIsEmpty() {
        assertEquals(emptySet<DayOfWeek>(), repeatDaysFromCsv(""))
        assertEquals(emptySet<DayOfWeek>(), repeatDaysFromCsv("   "))
    }

    @Test fun rejectsHour24() {
        assertFailsWith<IllegalArgumentException> { Alarm(id = "a1", hour = 24, minute = 0) }
    }
    @Test fun rejectsHourNegative() {
        assertFailsWith<IllegalArgumentException> { Alarm(id = "a1", hour = -1, minute = 0) }
    }
    @Test fun rejectsMinute60() {
        assertFailsWith<IllegalArgumentException> { Alarm(id = "a1", hour = 0, minute = 60) }
    }
    @Test fun rejectsSnoozeMinutesZero() {
        assertFailsWith<IllegalArgumentException> { Alarm(id = "a1", hour = 0, minute = 0, snoozeMinutes = 0) }
    }
    @Test fun rejectsSnoozeMinutes31() {
        assertFailsWith<IllegalArgumentException> { Alarm(id = "a1", hour = 0, minute = 0, snoozeMinutes = 31) }
    }
}
