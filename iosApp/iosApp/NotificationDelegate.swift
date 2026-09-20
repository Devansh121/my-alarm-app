import Foundation
import UserNotifications
import Shared

/// Routes alarm notifications into Kotlin: foreground arrivals start the
/// in-app ringing screen (suppressing the system banner+sound since the
/// ringer takes over); taps from background do the same on open.
final class NotificationDelegate: NSObject, UNUserNotificationCenterDelegate {

    static let shared = NotificationDelegate()

    func install() {
        UNUserNotificationCenter.current().delegate = self
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        AppBridge.shared.alarmFired(alarmId: notification.request.identifier)
        completionHandler([])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let alarmId = response.notification.request.identifier
        switch response.actionIdentifier {
        case NotificationAlarmEngine.snoozeActionId:
            AppBridge.shared.snoozeFromNotification(alarmId: alarmId)
        case NotificationAlarmEngine.stopActionId:
            AppBridge.shared.stopFromNotification(alarmId: alarmId)
        default:
            // Tap on the notification body: open the in-app ringing screen.
            AppBridge.shared.alarmFired(alarmId: alarmId)
        }
        completionHandler()
    }
}
