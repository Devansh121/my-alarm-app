package com.devansh.alarm.data

import app.cash.sqldelight.db.SqlDriver
import com.devansh.alarm.db.AlarmDb
import com.devansh.alarm.db.AlarmRow
import com.devansh.alarm.domain.Alarm
import com.devansh.alarm.domain.ToneSelection
import com.devansh.alarm.domain.repeatDaysFromCsv
import com.devansh.alarm.domain.repeatDaysToCsv

private const val TONE_MODE_RANDOM = "random"
private const val TONE_MODE_PINNED = "pinned"

class AlarmRepository(driver: SqlDriver) {

    private val queries = AlarmDb(driver).alarmQueries

    fun all(): List<Alarm> = queries.selectAll().executeAsList().map { it.toDomain() }

    fun byId(id: String): Alarm? = queries.selectById(id).executeAsOneOrNull()?.toDomain()

    fun save(alarm: Alarm) {
        queries.upsert(
            id = alarm.id,
            hour = alarm.hour.toLong(),
            minute = alarm.minute.toLong(),
            repeat_days = repeatDaysToCsv(alarm.repeatDays),
            label = alarm.label,
            tone_mode = when (alarm.tone) {
                is ToneSelection.Random -> TONE_MODE_RANDOM
                is ToneSelection.Pinned -> TONE_MODE_PINNED
            },
            tone_id = (alarm.tone as? ToneSelection.Pinned)?.toneId,
            snooze_enabled = if (alarm.snoozeEnabled) 1L else 0L,
            snooze_minutes = alarm.snoozeMinutes.toLong(),
            enabled = if (alarm.enabled) 1L else 0L,
            next_fire_epoch_ms = null,
        )
    }

    fun setEnabled(id: String, enabled: Boolean) {
        queries.setEnabled(if (enabled) 1L else 0L, id)
    }

    fun delete(id: String) {
        queries.deleteById(id)
    }

    fun count(): Long = queries.countAll().executeAsOne()

    /** Epoch ms of the last scheduled fire, persisted for missed-alarm reconciliation. */
    fun nextFireMs(id: String): Long? =
        queries.selectById(id).executeAsOneOrNull()?.next_fire_epoch_ms

    fun setNextFire(id: String, epochMs: Long?) {
        queries.setNextFire(epochMs, id)
    }

    private fun AlarmRow.toDomain(): Alarm = Alarm(
        id = id,
        hour = hour.toInt(),
        minute = minute.toInt(),
        repeatDays = repeatDaysFromCsv(repeat_days),
        label = label,
        tone = when (tone_mode) {
            TONE_MODE_PINNED -> tone_id?.let { ToneSelection.Pinned(it) } ?: ToneSelection.Random
            else -> ToneSelection.Random
        },
        snoozeEnabled = snooze_enabled != 0L,
        snoozeMinutes = snooze_minutes.toInt(),
        enabled = enabled != 0L,
    )
}
