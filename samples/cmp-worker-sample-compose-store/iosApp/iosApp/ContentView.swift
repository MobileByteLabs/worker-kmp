// iosApp/iosApp/ContentView.swift
//
// Bridge between SwiftUI and the Compose Multiplatform UIViewController exposed
// by `cmp-worker-sample-compose-store`'s iosMain source set.

import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // `MainViewController` is the top-level fun in iosMain/MainViewController.kt.
        // Kotlin/Native exposes it to Swift as `MainViewControllerKt.MainViewController()`.
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Composable state is driven from inside Compose; no SwiftUI-side updates required.
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea()
    }
}
