@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.devansh.alarm.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class NextFireCalculatorTest {

    private val zone = TimeZone.of("Asia/Kolkata")

    private fun instantOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant =
        LocalDateTime(year, month, day, hour, minute).toInstant(zone)

    private fun alarm(
        hour: Int,
        minute: Int,
        repeatDays: Set<DayOfWeek> = emptySet(),
    ) = Alarm(id = "a1", hour = hour, minute = minute, repeatDays = repeatDays)

    // --- one-shot alarms ---

    @Test
    fun oneShot_laterToday_firesToday() {
        // Sat 2026-09-20, now 10:00, alarm 18:30
        val now = instantOf(2026, 9, 20, 10, 0)
        val fire = NextFireCalculator.nextFire(alarm(18, 30), now, zone)
        assertEquals(instantOf(2026, 9, 20, 18, 30), fire)
    }

    @Test
    fun oneShot_earlierToday_firesTomorrow() {
        val now = instantOf(2026, 9, 20, 10, 0)
        val fire = NextFireCalculator.nextFire(alarm(7, 0), now, zone)
        assertEquals(instantOf(2026, 9, 21, 7, 0), fire)
    }

    @Test
    fun oneShot_exactlyNow_firesTomorrow() {
        // an alarm at exactly `now` must not fire in the past
        val now = instantOf(2026, 9, 20, 10, 0)
        val fire = NextFireCalculator.nextFire(alarm(10, 0), now, zone)
        assertEquals(instantOf(2026, 9, 21, 10, 0), fire)
    }

    @Test
    fun oneShot_oneMinuteAhead_firesToday() {
        val now = instantOf(2026, 9, 20, 9, 59)
        val fire = NextFireCalculator.nextFire(alarm(10, 0), now, zone)
        assertEquals(instantOf(2026, 9, 20, 10, 0), fire)
    }

    @Test
    fun oneShot_midnightAlarm_firesNextMidnight() {
        val now = instantOf(2026, 9, 20, 0, 1)
        val fire = NextFireCalculator.nextFire(alarm(0, 0), now, zone)
        assertEquals(instantOf(2026, 9, 21, 0, 0), fire)
    }

    // --- repeating alarms ---

    @Test
    fun repeating_todayIsRepeatDay_timeAhead_firesToday() {
        // 2026-09-20 is a Sunday
        val now = instantOf(2026, 9, 20, 6, 0)
        val a = alarm(8, 0, setOf(DayOfWeek.SUNDAY))
        assertEquals(instantOf(2026, 9, 20, 8, 0), NextFireCalculator.nextFire(a, now, zone))
    }

    @Test
    fun repeating_todayIsRepeatDay_timePassed_firesNextWeek() {
        val now = instantOf(2026, 9, 20, 9, 0)
        val a = alarm(8, 0, setOf(DayOfWeek.SUNDAY))
        assertEquals(instantOf(2026, 9, 27, 8, 0), NextFireCalculator.nextFire(a, now, zone))
    }

    @Test
    fun repeating_picksNearestOfMultipleDays() {
        // Sunday now; alarm on Mon+Fri at 07:00 -> Monday 2026-09-21
        val now = instantOf(2026, 9, 20, 12, 0)
        val a = alarm(7, 0, setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        assertEquals(instantOf(2026, 9, 21, 7, 0), NextFireCalculator.nextFire(a, now, zone))
    }

    @Test
    fun repeating_weekWrapsAround() {
        // Sunday 12:00; alarm only on Saturdays 07:00 -> next Saturday 2026-09-26
        val now = instantOf(2026, 9, 20, 12, 0)
        val a = alarm(7, 0, setOf(DayOfWeek.SATURDAY))
        assertEquals(instantOf(2026, 9, 26, 7, 0), NextFireCalculator.nextFire(a, now, zone))
    }

    @Test
    fun repeating_everyDay_behavesLikeDaily() {
        val now = instantOf(2026, 9, 20, 23, 30)
        val a = alarm(23, 0, DayOfWeek.entries.toSet())
        assertEquals(instantOf(2026, 9, 21, 23, 0), NextFireCalculator.nextFire(a, now, zone))
    }

    // --- timezone behavior ---

    @Test
    fun resultIsZoneAware() {
        // Same wall-clock alarm in a different zone yields a different instant
        val nyZone = TimeZone.of("America/New_York")
        val now = LocalDateTime(2026, 9, 20, 10, 0).toInstant(nyZone)
        val fire = NextFireCalculator.nextFire(alarm(18, 30), now, nyZone)
        assertEquals(LocalDateTime(2026, 9, 20, 18, 30).toInstant(nyZone), fire)
        assertEquals(18, fire.toLocalDateTime(nyZone).hour)
    }

    @Test
    fun dstSpringForwardGap_resolvesToValidInstant() {
        // US DST 2026: clocks jump 02:00 -> 03:00 on Sun 2026-03-08 in New_York.
        // An 02:30 alarm on that night must still produce a valid fire time, not crash.
        val nyZone = TimeZone.of("America/New_York")
        val now = LocalDateTime(2026, 3, 8, 1, 0).toInstant(nyZone)
        val fire = NextFireCalculator.nextFire(alarm(2, 30), now, nyZone)
        val local = fire.toLocalDateTime(nyZone)
        // kotlinx-datetime resolves the gap by shifting forward; accept 02:30 or 03:30
        assertEquals(2026, local.year)
        assertEquals(3, local.monthNumber)
        assertEquals(8, local.dayOfMonth)
        assertEquals(30, local.minute)
    }
}
