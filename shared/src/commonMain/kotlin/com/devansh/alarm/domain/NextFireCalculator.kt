@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.devansh.alarm.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object NextFireCalculator {

    /**
     * The next instant strictly after [now] at which [alarm] should fire in [zone].
     *
     * One-shot alarms (empty repeatDays) fire at the next occurrence of their wall-clock
     * time; repeating alarms fire on the nearest enabled weekday. Wall-clock times that
     * fall in a DST gap resolve to the shifted valid instant.
     */
    fun nextFire(alarm: Alarm, now: Instant, zone: TimeZone): Instant {
        val today = now.toLocalDateTime(zone).date
        val time = LocalTime(alarm.hour, alarm.minute)
        // offset 8 covers "today is the only repeat day but the time already passed"
        for (offset in 0..8) {
            val date = today.plus(DatePeriod(days = offset))
            if (alarm.repeatDays.isNotEmpty() && date.dayOfWeek !in alarm.repeatDays) continue
            val candidate = LocalDateTime(date, time).toInstant(zone)
            if (candidate > now) return candidate
        }
        error("unreachable: no fire time within 8 days for alarm ${alarm.id}")
    }
}
