@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.devansh.alarm.engine

import com.devansh.alarm.domain.Alarm
import com.devansh.alarm.domain.NextFireCalculator
import com.devansh.alarm.domain.ToneRandomizer
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.minutes

/**
 * Pure orchestration between domain logic and the platform [AlarmEngine]:
 * decides *when* each alarm fires and *which tone* it uses, then hands
 * concrete [FireRequest]s to the engine.
 */
class AlarmScheduler(
    private val engine: AlarmEngine,
    private val toneRandomizer: ToneRandomizer,
) {

    /**
     * Drop every scheduled request and reschedule all [alarms] that are enabled.
     * Called after any alarm edit and on app foreground to self-heal drift.
     */
    fun rescheduleAll(
        alarms: List<Alarm>,
        now: Instant,
        zone: TimeZone,
        lastToneId: String? = null,
    ): List<FireRequest> {
        engine.cancelAll()
        return alarms.filter { it.enabled }.map { alarm ->
            val request = FireRequest(
                alarmId = alarm.id,
                fireAt = NextFireCalculator.nextFire(alarm, now, zone),
                label = alarm.label,
                toneFileName = toneRandomizer.resolve(alarm.tone, lastToneId).fileName,
            )
            engine.schedule(request)
            request
        }
    }

    /** Schedule a snooze ring [Alarm.snoozeMinutes] from [now], keeping the same tone. */
    fun snooze(alarm: Alarm, now: Instant, currentToneFileName: String): FireRequest {
        require(alarm.snoozeEnabled) { "snooze disabled for alarm ${alarm.id}" }
        val request = FireRequest(
            alarmId = alarm.id,
            fireAt = now + alarm.snoozeMinutes.minutes,
            label = alarm.label,
            toneFileName = currentToneFileName,
            isSnooze = true,
        )
        engine.schedule(request)
        return request
    }

    /** Cancel a single alarm's pending request (e.g. after disable or delete). */
    fun cancel(alarmId: String) {
        engine.cancel(alarmId)
    }
}
