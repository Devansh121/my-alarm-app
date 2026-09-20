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

/** Replaced by the Swift UNUserNotificationCenter engine in Task 13. */
private object NoopEngine : AlarmEngine {
    override fun schedule(request: FireRequest) {}
    override fun cancel(alarmId: String) {}
    override fun cancelAll() {}
}

fun MainViewController(): UIViewController {
    val repository = AlarmRepository(NativeSqliteDriver(AlarmDb.Schema, "alarm.db"))
    val core = AppCore(repository, NoopEngine)
    return ComposeUIViewController { App(core) }
}
