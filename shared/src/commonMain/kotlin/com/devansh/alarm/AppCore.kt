@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.devansh.alarm

import androidx.compose.runtime.mutableStateOf
import com.devansh.alarm.data.AlarmRepository
import com.devansh.alarm.domain.Alarm
import com.devansh.alarm.domain.BUNDLED_TONES
import com.devansh.alarm.domain.ToneRandomizer
import com.devansh.alarm.engine.AlarmEngine
import com.devansh.alarm.engine.AlarmScheduler
import com.devansh.alarm.engine.FireRequest
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

/** Platform audio boundary: Swift's AlarmRinger implements this. */
interface RingerControl {
    fun start(toneFileName: String)
    fun stop()
}

object NoopRinger : RingerControl {
    override fun start(toneFileName: String) {}
    override fun stop() {}
}

/**
 * Single wiring point between persistence, scheduling, and UI state.
 * Every mutation persists first, then reschedules the platform engine,
 * then refreshes the observable list — so UI, DB, and pending
 * notifications can never disagree.
 */
class AppCore(
    private val repository: AlarmRepository,
    engine: AlarmEngine,
    private val ringer: RingerControl = NoopRinger,
    private val now: () -> Instant = { Clock.System.now() },
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private val scheduler = AlarmScheduler(engine, ToneRandomizer(BUNDLED_TONES))

    /** alarmId -> last scheduled request; source of truth for the ringing tone. */
    private var pending: Map<String, FireRequest> = emptyMap()

    val alarms = mutableStateOf<List<Alarm>>(emptyList())

    /** Non-null while an alarm is ringing; drives the full-screen ringing UI. */
    val ringing = mutableStateOf<Alarm?>(null)

    init {
        refreshAndReschedule()
    }

    fun upsert(alarm: Alarm) {
        repository.save(alarm)
        refreshAndReschedule()
    }

    fun setEnabled(id: String, enabled: Boolean) {
        repository.setEnabled(id, enabled)
        refreshAndReschedule()
    }

    fun delete(id: String) {
        repository.delete(id)
        refreshAndReschedule()
    }

    /** Called from the platform when the alarm notification fires. */
    fun onAlarmFired(alarmId: String) {
        val alarm = repository.byId(alarmId) ?: return
        ringing.value = alarm
        ringer.start(pending[alarmId]?.toneFileName ?: BUNDLED_TONES.first().fileName)
    }

    /** Stop button: silence, one-shot alarms disable themselves, reschedule. */
    fun stopRinging() {
        val alarm = ringing.value ?: return
        ringer.stop()
        ringing.value = null
        stopAlarm(alarm)
    }

    /** Snooze button: silence now, re-ring in [Alarm.snoozeMinutes]. */
    fun snoozeRinging() {
        val alarm = ringing.value ?: return
        ringer.stop()
        ringing.value = null
        scheduleSnooze(alarm)
    }

    /**
     * "Snooze" notification action: reschedule without touching the ringing
     * state — the app may be backgrounded and nothing is audibly ringing.
     */
    fun snoozeFromNotification(alarmId: String) {
        val alarm = repository.byId(alarmId) ?: return
        if (!alarm.snoozeEnabled) return
        scheduleSnooze(alarm)
    }

    /** "Stop" notification action: no ringer involved, just settle the alarm. */
    fun stopFromNotification(alarmId: String) {
        val alarm = repository.byId(alarmId) ?: return
        stopAlarm(alarm)
    }

    /** One-shot alarms disable themselves once stopped; then re-sync engine + UI. */
    private fun stopAlarm(alarm: Alarm) {
        if (alarm.repeatDays.isEmpty()) {
            repository.setEnabled(alarm.id, false)
        }
        refreshAndReschedule()
    }

    /** Re-ring in [Alarm.snoozeMinutes] using the pending request's tone. */
    private fun scheduleSnooze(alarm: Alarm) {
        val tone = pending[alarm.id]?.toneFileName ?: BUNDLED_TONES.first().fileName
        val request = scheduler.snooze(alarm, now(), tone)
        pending = pending + (alarm.id to request)
    }

    /** Call on app foreground: re-syncs pending notifications with the DB. */
    fun refreshAndReschedule() {
        val reconciled = reconcileMissed(repository.all())
        pending = scheduler.rescheduleAll(reconciled, now(), zone()).associateBy { it.alarmId }
        reconciled.forEach { alarm ->
            repository.setNextFire(alarm.id, pending[alarm.id]?.fireAt?.toEpochMilliseconds())
        }
        alarms.value = reconciled
    }

    /**
     * A one-shot alarm whose persisted fire time passed while the app was dead
     * already rang as a system notification — disable it instead of silently
     * rescheduling it for tomorrow.
     */
    private fun reconcileMissed(all: List<Alarm>): List<Alarm> {
        val nowMs = now().toEpochMilliseconds()
        return all.map { alarm ->
            val stored = repository.nextFireMs(alarm.id)
            if (alarm.enabled && alarm.repeatDays.isEmpty() && stored != null && stored <= nowMs) {
                repository.setEnabled(alarm.id, false)
                alarm.copy(enabled = false)
            } else {
                alarm
            }
        }
    }
}
