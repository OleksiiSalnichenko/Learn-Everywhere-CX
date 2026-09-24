# Learn Everywhere

Learn Everywhere is an Android vocabulary app for German and English words with Ukrainian translations. It supports text and speech intake, local dictionaries, reviewed Gemini suggestions, JSON import/export, persistent lesson settings, and background audio lessons. The approved visual direction is Resonance A in light and dark themes.

## Requirements

- JDK 21 (the Android Studio JBR at `$HOME/android-studio/jbr` is a common option)
- Android SDK platform 37 and build tools 36
- Android 8.0 / API 26 or newer for the app
- An Android device or emulator for instrumentation and manual checks

Firebase configuration is optional for local builds. Without it, the Home screen returns a localized setup error and local dictionaries, transfer, settings, and playback remain usable.

## Build and test

From the repository root:

```sh
JAVA_HOME="$HOME/android-studio/jbr" ./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug --no-daemon
```

Run one JVM test class:

```sh
JAVA_HOME="$HOME/android-studio/jbr" ./gradlew :app:testDebugUnitTest --tests com.learneverywhere.app.data.DictionaryRepositoryTest --no-daemon
```

Artifacts:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- Lint report: `app/build/reports/lint-results-debug.html`
- Room schema: `app/schemas/com.learneverywhere.app.data.DictionaryDatabase/1.json`

The build does not use a destructive Room migration fallback. Any future schema change must export a new schema and provide an explicit migration.

## Optional Firebase AI Logic setup

The Firebase project and Android registration must match package `com.learneverywhere.app`. Do not put a Gemini server key, App Check debug token, user vocabulary, or any other secret in source control.

The current Firebase console wizard is paused at **Enable APIs** because that action accepts Gemini API Additional Terms and usage policies. Only the product owner can approve those terms. Do not enable billing; the selected design uses Firebase AI Logic on Spark with `gemini-3.5-flash-lite`.

After the product owner explicitly accepts the legal terms:

1. In Firebase Console, finish AI Logic setup for the existing `learn-everywhere` project without enabling billing.
2. Download a fresh Android configuration for `com.learneverywhere.app` and save it locally as `app/google-services.json`.
3. For a debug build, obtain the App Check debug token from local device logs and register it in Firebase Console. Keep the token out of files and commits.
4. Rebuild the debug APK, install it on the device, enter one Ukrainian, German, and English word, review the destination and translated fields, and press OK once.
5. Confirm each saved item is in the expected default dictionary. Check that Cancel creates neither a word nor an automatic dictionary.
6. Remove `app/google-services.json` before preparing a source archive. Never commit it.

A successful compile without `google-services.json` is not a live Firebase smoke test.

## Device acceptance

With a device shown by `adb devices`, install the debug APK:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Perform the manual scenarios in [Software Design](docs/software-design.md#manual-device-validation). They cover speech permission and cancellation, SAF JSON transfer, locale switching, large text, keyboard resizing, TalkBack labels, TTS voices, notification controls, screen lock, audio focus, and loop/shuffle behavior.

## Documentation

[Software Design](docs/software-design.md) describes architecture, data, intake, transfer, settings, playback, navigation, validation boundaries, and the remaining manual checks.
