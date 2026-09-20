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

fun MainViewController(): UIViewController = MainViewController(NoopEngine)

/** Entry point for Swift: inject the UNUserNotificationCenter-backed engine. */
fun MainViewController(engine: AlarmEngine): UIViewController {
    val repository = AlarmRepository(NativeSqliteDriver(AlarmDb.Schema, "alarm.db"))
    val core = AppCore(repository, engine)
    return ComposeUIViewController { App(core) }
}
