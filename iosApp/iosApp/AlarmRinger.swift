import AVFoundation
import Foundation
import Shared

/// Plays the alarm tone at full effect while the app is foregrounded and
/// guards against a silenced ringer: AVAudioSession .playback ignores the
/// mute switch, and volume below the floor is reported so the UI can warn.
final class AlarmRinger: RingerControl {

    static let shared = AlarmRinger()
    static let minimumVolume: Float = 0.3

    private var player: AVAudioPlayer?

    /// True when the system output volume is below the audible floor.
    var volumeTooLow: Bool {
        AVAudioSession.sharedInstance().outputVolume < Self.minimumVolume
    }

    func start(toneFileName: String) {
        let base = (toneFileName as NSString).deletingPathExtension
        guard let url = Bundle.main.url(forResource: base, withExtension: "caf") else {
            NSLog("alarm: tone \(toneFileName) missing from bundle")
            return
        }
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, options: [])
            try session.setActive(true)
            let player = try AVAudioPlayer(contentsOf: url)
            player.numberOfLoops = -1
            player.volume = 1.0
            player.play()
            self.player = player
        } catch {
            NSLog("alarm: ringer failed \(error)")
        }
    }

    func stop() {
        player?.stop()
        player = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}
