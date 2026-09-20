@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.devansh.alarm

import androidx.compose.ui.window.ComposeUIViewController
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.devansh.alarm.data.AlarmRepository
import com.devansh.alarm.db.AlarmDb
import com.devansh.alarm.engine.AlarmEngine
import com.devansh.alarm.engine.FireRequest
import com.devansh.alarm.ui.App
import platform.UIKit.UIViewController

/** Fallback when Swift doesn't inject an engine (previews, tests). */
private object NoopEngine : AlarmEngine {
    override fun schedule(request: FireRequest) {}
    override fun cancel(alarmId: String) {}
    override fun cancelAll() {}
}

/**
 * Swift-facing handle to the running AppCore. The notification delegate
 * calls [alarmFired] when an alarm notification arrives in the foreground
 * or is tapped.
 */
object AppBridge {
    internal var core: AppCore? = null

    fun alarmFired(alarmId: String) {
        core?.onAlarmFired(alarmId)
    }

    fun appForegrounded() {
        core?.refreshAndReschedule()
    }
}

fun MainViewController(): UIViewController = MainViewController(NoopEngine, NoopRinger)

/** Entry point for Swift: inject the notification engine and tone ringer. */
fun MainViewController(engine: AlarmEngine, ringer: RingerControl): UIViewController {
    val repository = AlarmRepository(NativeSqliteDriver(AlarmDb.Schema, "alarm.db"))
    val core = AppCore(repository, engine, ringer)
    AppBridge.core = core
    return ComposeUIViewController { App(core) }
}
