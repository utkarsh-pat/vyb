# Vybnet Android

Native Android client built with Kotlin and Jetpack Compose.

## Requirements

- Android Studio (Jellyfish or newer)
- JDK 17
- Android SDK 35

## Run

1. Open `apps/mobile` in Android Studio.
2. Let Gradle sync and select an emulator or physical device.
3. Run the `app` configuration.

## Emulator

The local command-line Android environment uses:

- SDK: `C:\Android\sdk`
- AVD: `Vyb_API_35`
- Device profile: Pixel 7
- Image: Android 15 / API 35 / Google Play / x86_64

Start it from PowerShell:

```powershell
$env:ANDROID_HOME = "C:\Android\sdk"
$env:ANDROID_SDK_ROOT = "C:\Android\sdk"
& "C:\Android\sdk\emulator\emulator.exe" -avd Vyb_API_35
```

Build, install, and launch:

```powershell
./gradlew assembleDebug
& "C:\Android\sdk\platform-tools\adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
& "C:\Android\sdk\platform-tools\adb.exe" shell am start -n social.vyb.app/.MainActivity
```

The debug build uses `http://10.0.2.2:4000/` by default, which is the Android
emulator alias for the host machine. Override it without editing source:

```powershell
./gradlew installDebug -PvybApiBaseUrl=https://api.vybnet.app/
```

The default release URL is the Cloud Run custom domain
`https://api.vybnet.app/`. A release override remains available through
`-PvybReleaseApiBaseUrl=https://api.vybnet.app/`.

Create a Play-ready signed App Bundle without committing secrets:

```powershell
./gradlew bundleRelease `
  -PvybReleaseStoreFile=C:\secure\vyb-upload.jks `
  -PvybReleaseStorePassword=YOUR_STORE_PASSWORD `
  -PvybReleaseKeyAlias=vyb-upload `
  -PvybReleaseKeyPassword=YOUR_KEY_PASSWORD
```

The output is `app/build/outputs/bundle/release/app-release.aab`. If those four
properties are omitted, Gradle deliberately creates an unsigned release
artifact for CI verification only.

The app is registered in Firebase project `vybnet` as package
`social.vyb.app`. It uses Firebase Authentication for Email/Password and
Google Sign-in. Canonical profile and tenant data are stored through Data
Connect/Cloud SQL; Firestore is not a production fallback for these entities.

For Google Sign-in, register the local debug and Play App Signing SHA-1/SHA-256
fingerprints on the new `vybnet` Android app before publishing.

`app/google-services.json` is intentionally not committed. Fetch the current
restricted config for the `vybnet` Android app before building:

```powershell
pnpm exec firebase apps:sdkconfig ANDROID 1:850600134378:android:525ba14313609c8f26b993 --project vybnet > app/google-services.json
```

The app includes the native product shell, authenticated feed and social
actions, media Post/Story/Vibe composer, Stories/Vibes playback, encrypted
messages with realtime typing/delivery updates, market, campus hub, events,
resources, games, notifications, FCM device registration, and profile surfaces.
