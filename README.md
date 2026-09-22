# Xiaomi TV Remote

An Android phone remote for Xiaomi / Android TV devices on the same local network.

## Current status

A native Kotlin + Jetpack Compose Android app with a working Android TV Remote Protocol v2 client: LAN discovery (NSD), the mTLS pairing handshake with on-TV code entry, and persisted per-device client credentials. Once paired, the app sends real key events, volume/media commands, text input (IME batch edit), and app deep links over the protocol, it isn't a UI mockup.

A GitHub Actions pipeline builds a debug APK on every push.

## Download the APK

1. Open the repository's **Actions** tab.
2. Open the latest successful **Build Android APK** workflow run.
3. Under **Artifacts**, download `xiaomi-tv-remote-debug`.
4. Extract the ZIP and install `app-debug.apk` on your Android phone.

Android may ask you to allow installation from your browser/file manager.

## Build

The CI build uses Java 17, Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, and `:app:assembleDebug`.

## Roadmap

Shipped: LAN discovery, pairing-code flow, persistent per-device credentials, D-pad/power/home/back, volume and media transport keys, text input, app-launch links.

Remaining:

- UI/UX redesign (theming, connect-vs-remote screen split, accessibility labels)
- Touchpad mode (swipe-to-D-pad gesture translation — the protocol has no continuous pointer message, so this emulates a trackpad via accelerated key repeats, it isn't literal cursor injection)
- ViewModel-based state management and unit tests for the protocol framing
- Multi-device favorites
