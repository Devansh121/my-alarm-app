@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.devansh.alarm

import androidx.compose.runtime.mutableStateOf
import com.devansh.alarm.data.AlarmRepository
import com.devansh.alarm.domain.Alarm
import com.devansh.alarm.domain.BUNDLED_TONES
import com.devansh.alarm.domain.ToneRandomizer
import com.devansh.alarm.engine.AlarmEngine
import com.devansh.alarm.engine.AlarmScheduler
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

/**
 * Single wiring point between persistence, scheduling, and UI state.
 * Every mutation persists first, then reschedules the platform engine,
 * then refreshes the observable list — so UI, DB, and pending
 * notifications can never disagree.
 */
class AppCore(
    private val repository: AlarmRepository,
    engine: AlarmEngine,
    private val now: () -> Instant = { Clock.System.now() },
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private val scheduler = AlarmScheduler(engine, ToneRandomizer(BUNDLED_TONES))

    val alarms = mutableStateOf<List<Alarm>>(emptyList())

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

    /** Call on app foreground: re-syncs pending notifications with the DB. */
    fun refreshAndReschedule() {
        val all = repository.all()
        scheduler.rescheduleAll(all, now(), zone())
        alarms.value = all
    }
}
