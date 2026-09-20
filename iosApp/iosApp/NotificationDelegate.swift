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
        AppBridge.shared.alarmFired(alarmId: response.notification.request.identifier)
        completionHandler()
    }
}
