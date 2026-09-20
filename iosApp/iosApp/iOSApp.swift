import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        NotificationAlarmEngine.shared.requestAuthorization()
        NotificationDelegate.shared.install()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea(.all)
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            engine: NotificationAlarmEngine.shared,
            ringer: AlarmRinger.shared
        )
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
