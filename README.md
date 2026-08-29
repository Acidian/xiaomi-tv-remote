# Xiaomi TV Remote

An Android phone remote for Xiaomi / Android TV devices on the same local network.

## Current milestone

V0.1 establishes a native Kotlin + Jetpack Compose Android app and a GitHub Actions pipeline that produces a debug APK.

The visible remote UI includes navigation, Home/Back/Power, media and volume controls. Network discovery, pairing, and Android TV Remote Protocol communication are the next implementation milestone; the V0.1 buttons are intentionally UI-only until that transport is connected.

## Download the APK

1. Open the repository's **Actions** tab.
2. Open the latest successful **Build Android APK** workflow run.
3. Under **Artifacts**, download `xiaomi-tv-remote-debug`.
4. Extract the ZIP and install `app-debug.apk` on your Android phone.

Android may ask you to allow installation from your browser/file manager.

## Build

The CI build uses Java 17, Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, and `:app:assembleDebug`.

## Roadmap

- LAN Android TV discovery
- Pairing-code flow
- Persistent paired-device credentials
- Android TV Remote Protocol commands
- D-pad, OK, Back, Home, media and volume
- Text/keyboard input
- App-launch shortcuts
- Touchpad mode
