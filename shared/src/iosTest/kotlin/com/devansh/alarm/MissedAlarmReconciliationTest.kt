@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.devansh.alarm

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import com.devansh.alarm.data.AlarmRepository
import com.devansh.alarm.db.AlarmDb
import com.devansh.alarm.domain.Alarm
import com.devansh.alarm.engine.AlarmEngine
import com.devansh.alarm.engine.FireRequest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class ReconTestEngine : AlarmEngine {
    val pending = mutableMapOf<String, FireRequest>()
    override fun schedule(request: FireRequest) { pending[request.alarmId] = request }
    override fun cancel(alarmId: String) { pending.remove(alarmId) }
    override fun cancelAll() { pending.clear() }
}

/**
 * Simulates an app relaunch: two AppCore instances share one database.
 * The first schedules; the "relaunched" second must reconcile alarms
 * that fired while no app was running.
 */
class MissedAlarmReconciliationTest {

    private val zone = TimeZone.of("Asia/Kolkata")
    private val driver: SqlDriver = inMemoryDriver(AlarmDb.Schema)
    private val repo = AlarmRepository(driver)

    // Sat 2026-09-20; alarm at 18:00 same day
    private val beforeFire = LocalDateTime(2026, 9, 20, 10, 0).toInstant(zone)
    private val afterFire = LocalDateTime(2026, 9, 21, 9, 0).toInstant(zone)

    @AfterTest
    fun tearDown() = driver.close()

    private fun core(at: kotlinx.datetime.Instant, engine: AlarmEngine = ReconTestEngine()) =
        AppCore(repo, engine, now = { at }, zone = { zone })

    @Test
    fun missedOneShotDisablesOnRelaunch() {
        core(beforeFire).upsert(Alarm(id = "a1", hour = 18, minute = 0))

        val relaunched = core(afterFire)
        assertFalse(relaunched.alarms.value.single().enabled)
    }

    @Test
    fun futureOneShotStaysEnabledOnRelaunch() {
        core(beforeFire).upsert(Alarm(id = "a1", hour = 18, minute = 0))

        val relaunchedBeforeFire = core(LocalDateTime(2026, 9, 20, 12, 0).toInstant(zone))
        assertTrue(relaunchedBeforeFire.alarms.value.single().enabled)
    }

    @Test
    fun missedRepeatingAlarmStaysEnabledAndReschedules() {
        core(beforeFire).upsert(
            Alarm(id = "a2", hour = 18, minute = 0, repeatDays = DayOfWeek.entries.toSet()),
        )

        val engine = ReconTestEngine()
        val relaunched = core(afterFire, engine)
        assertTrue(relaunched.alarms.value.single().enabled)
        assertEquals(
            LocalDateTime(2026, 9, 21, 18, 0).toInstant(zone),
            engine.pending.getValue("a2").fireAt,
        )
    }

    @Test
    fun disabledAlarmUntouchedByReconciliation() {
        core(beforeFire).upsert(Alarm(id = "a3", hour = 18, minute = 0, enabled = false))

        val relaunched = core(afterFire)
        assertFalse(relaunched.alarms.value.single().enabled)
        assertEquals(null, repo.nextFireMs("a3"))
    }

    @Test
    fun nextFirePersistedForScheduledAlarms() {
        core(beforeFire).upsert(Alarm(id = "a4", hour = 18, minute = 0))
        assertEquals(
            LocalDateTime(2026, 9, 20, 18, 0).toInstant(zone).toEpochMilliseconds(),
            repo.nextFireMs("a4"),
        )
    }
}
