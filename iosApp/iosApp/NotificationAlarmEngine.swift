import Foundation
import UserNotifications
import Shared

/// Kotlin AlarmEngine backed by UNUserNotificationCenter.
/// Each FireRequest becomes one local notification whose sound is the
/// pre-resolved bundled tone.
final class NotificationAlarmEngine: AlarmEngine {

    static let shared = NotificationAlarmEngine()

    func requestAuthorization() {
        UNUserNotificationCenter.current().requestAuthorization(
            options: [.alert, .sound, .badge]
        ) { granted, _ in
            NSLog("alarm: notification permission granted=\(granted)")
        }
    }

    func schedule(request: FireRequest) {
        let content = UNMutableNotificationContent()
        content.title = request.label
        content.body = request.isSnooze ? "Snoozed alarm" : "Alarm"
        content.sound = UNNotificationSound(
            named: UNNotificationSoundName(request.toneFileName)
        )

        let fireDate = Date(
            timeIntervalSince1970: Double(request.fireAt.toEpochMilliseconds()) / 1000.0
        )
        let interval = max(fireDate.timeIntervalSinceNow, 1)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)

        UNUserNotificationCenter.current().add(
            UNNotificationRequest(
                identifier: request.alarmId,
                content: content,
                trigger: trigger
            )
        ) { error in
            if let error { NSLog("alarm: schedule failed \(error)") }
        }
    }

    func cancel(alarmId: String) {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: [alarmId])
    }

    func cancelAll() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
    }
}
