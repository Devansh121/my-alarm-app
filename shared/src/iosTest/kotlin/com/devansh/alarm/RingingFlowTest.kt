@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.devansh.alarm

import app.cash.sqldelight.driver.native.inMemoryDriver
import com.devansh.alarm.data.AlarmRepository
import com.devansh.alarm.db.AlarmDb
import com.devansh.alarm.domain.Alarm
import com.devansh.alarm.domain.ToneSelection
import com.devansh.alarm.engine.AlarmEngine
import com.devansh.alarm.engine.FireRequest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class RingingTestEngine : AlarmEngine {
    val pending = mutableMapOf<String, FireRequest>()
    override fun schedule(request: FireRequest) { pending[request.alarmId] = request }
    override fun cancel(alarmId: String) { pending.remove(alarmId) }
    override fun cancelAll() { pending.clear() }
}

private class RingingTestRinger : RingerControl {
    var playing: String? = null
    val started = mutableListOf<String>()
    override fun start(toneFileName: String) { playing = toneFileName; started += toneFileName }
    override fun stop() { playing = null }
}

class RingingFlowTest {

    private val zone = TimeZone.of("Asia/Kolkata")
    private val now = LocalDateTime(2026, 9, 20, 10, 0).toInstant(zone)
    private val engine = RingingTestEngine()
    private val ringer = RingingTestRinger()
    private val core = AppCore(
        repository = AlarmRepository(inMemoryDriver(AlarmDb.Schema)),
        engine = engine,
        ringer = ringer,
        now = { now },
        zone = { zone },
    )

    private val oneShot = Alarm(
        id = "a1", hour = 18, minute = 0,
        tone = ToneSelection.Pinned("waves"), snoozeMinutes = 9,
    )

    @Test
    fun firedAlarmStartsRingerWithScheduledTone() {
        core.upsert(oneShot)
        core.onAlarmFired("a1")
        assertEquals(oneShot, core.ringing.value)
        assertEquals("tone_waves.caf", ringer.playing)
    }

    @Test
    fun unknownAlarmIdIsIgnored() {
        core.onAlarmFired("ghost")
        assertNull(core.ringing.value)
        assertNull(ringer.playing)
    }

    @Test
    fun stopSilencesAndDisablesOneShot() {
        core.upsert(oneShot)
        core.onAlarmFired("a1")
        core.stopRinging()
        assertNull(core.ringing.value)
        assertNull(ringer.playing)
        assertFalse(core.alarms.value.single().enabled)
        assertTrue(engine.pending.isEmpty())
    }

    @Test
    fun stopKeepsRepeatingAlarmEnabledAndRescheduled() {
        val daily = oneShot.copy(id = "a2", repeatDays = DayOfWeek.entries.toSet())
        core.upsert(daily)
        core.onAlarmFired("a2")
        core.stopRinging()
        assertTrue(core.alarms.value.single().enabled)
        assertTrue("a2" in engine.pending)
    }

    @Test
    fun snoozeSchedulesSameToneNinMinutesOut() {
        core.upsert(oneShot)
        core.onAlarmFired("a1")
        core.snoozeRinging()
        assertNull(core.ringing.value)
        assertNull(ringer.playing)
        val request = engine.pending.getValue("a1")
        assertTrue(request.isSnooze)
        assertEquals("tone_waves.caf", request.toneFileName)
        assertEquals(
            LocalDateTime(2026, 9, 20, 10, 9).toInstant(zone),
            request.fireAt,
        )
    }

    @Test
    fun snoozedAlarmRingsAgainWithSameTone() {
        core.upsert(oneShot)
        core.onAlarmFired("a1")
        core.snoozeRinging()
        core.onAlarmFired("a1")
        assertEquals(listOf("tone_waves.caf", "tone_waves.caf"), ringer.started)
    }
}
