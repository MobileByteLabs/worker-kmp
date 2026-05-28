// iosApp/iosApp/iOSApp.swift
//
// SwiftUI entry point. Pre-launcher: would have hand-rolled `startKoin` and
// `ComposeUIViewController { App(...) }` from Swift — impractical. With the
// `cmp-worker-ios` launcher API (`workerKmpMainViewController`), Swift just
// embeds the framework's MainViewController via UIViewControllerRepresentable.

import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
