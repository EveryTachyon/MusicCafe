# MusicCafe

A simple Android music library app built with Kotlin and Jetpack Compose.

MusicCafe is intended to give users one place to browse a local music collection, organize playlists, and listen to their songs.

## What Works Now

The current version is a UI prototype. It includes:

- A dark Material 3 interface
- Home, Tracks, Artists, Albums, Playlists, Import songs, and Settings screens
- Sidebar navigation between sections
- Responsive Compose layout
- Placeholder states for an empty music library

The app does not import or play audio yet. The library pages currently show sample and placeholder content.

## Development Task

Turn the current UI prototype into a working local music player.

The main work is to:

1. Import audio files or scan a selected music folder.
2. Read track metadata and show real tracks, artists, and albums.
3. Add playback controls, a queue, and a now-playing view.
4. Let users create, edit, and save playlists.
5. Save the library and user preferences on the device.
6. Add tests for navigation, importing, playlists, and playback.

## Built With

- Kotlin
- Jetpack Compose
- Material 3
- Android SDK
- Gradle Kotlin DSL

## Requirements

- Android Studio
- JDK 17 or newer
- Android SDK API 37
- An Android emulator or a physical Android device

## Getting Started

Open the project in Android Studio, allow Gradle to sync, and run the `app` configuration on an emulator or connected device.

To build the debug APK from PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The generated APK is written to `app/build/outputs/apk/debug/`.

## Project Structure

```text
app/src/main/java/com/example/musiccafe/
├── MainActivity.kt       # Main Compose UI and navigation
└── ui/theme/             # App colors, typography, and theme
```

## Status

Early-stage prototype. The layout and navigation are in place; audio importing, playback, persistence, and real playlist management are still to be implemented.
