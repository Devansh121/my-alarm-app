@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.devansh.alarm

import app.cash.sqldelight.driver.native.inMemoryDriver
import com.devansh.alarm.data.AlarmRepository
import com.devansh.alarm.db.AlarmDb
import com.devansh.alarm.domain.Alarm
import com.devansh.alarm.engine.AlarmEngine
import com.devansh.alarm.engine.FireRequest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class RecordingEngine : AlarmEngine {
    val pending = mutableMapOf<String, FireRequest>()
    override fun schedule(request: FireRequest) { pending[request.alarmId] = request }
    override fun cancel(alarmId: String) { pending.remove(alarmId) }
    override fun cancelAll() { pending.clear() }
}

class AppCoreTest {

    private val zone = TimeZone.of("Asia/Kolkata")
    private val now = LocalDateTime(2026, 9, 20, 10, 0).toInstant(zone)
    private val engine = RecordingEngine()
    private val core = AppCore(
        repository = AlarmRepository(inMemoryDriver(AlarmDb.Schema)),
        engine = engine,
        now = { now },
        zone = { zone },
    )

    private fun alarm(id: String, hour: Int = 7, enabled: Boolean = true) =
        Alarm(id = id, hour = hour, minute = 0, enabled = enabled)

    @Test
    fun startsEmpty() {
        assertEquals(emptyList(), core.alarms.value)
        assertTrue(engine.pending.isEmpty())
    }

    @Test
    fun upsertPersistsSchedulesAndRefreshesState() {
        core.upsert(alarm("a1", hour = 18))
        assertEquals(listOf("a1"), core.alarms.value.map { it.id })
        assertEquals(
            LocalDateTime(2026, 9, 20, 18, 0).toInstant(zone),
            engine.pending.getValue("a1").fireAt,
        )
    }

    @Test
    fun disablingRemovesPendingRequestButKeepsAlarm() {
        core.upsert(alarm("a1"))
        core.setEnabled("a1", false)
        assertEquals(1, core.alarms.value.size)
        assertTrue(engine.pending.isEmpty())
    }

    @Test
    fun deleteRemovesAlarmAndPendingRequest() {
        core.upsert(alarm("a1"))
        core.delete("a1")
        assertEquals(emptyList(), core.alarms.value)
        assertTrue(engine.pending.isEmpty())
    }

    @Test
    fun multipleAlarmsAllScheduled() {
        core.upsert(alarm("a1", hour = 6))
        core.upsert(alarm("a2", hour = 22))
        assertEquals(setOf("a1", "a2"), engine.pending.keys)
    }
}
