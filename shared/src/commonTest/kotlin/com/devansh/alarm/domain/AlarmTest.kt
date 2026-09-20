package com.devansh.alarm.domain

import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
