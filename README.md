# Learn Everywhere

Android vocabulary collections for German and English with Ukrainian translations.

## Build

Requires JDK 21, Android SDK platform 37 and build tools 36. The checked-in wrapper uses Gradle 9.7.1; Android Gradle Plugin 9.3.2 includes Kotlin support. Set `JAVA_HOME` to your JDK and `ANDROID_HOME` to your SDK, or set `sdk.dir` in untracked `local.properties`.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
./gradlew :app:testDebugUnitTest --tests com.learneverywhere.app.data.DictionaryRepositoryTest
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. JVM repository tests run the actual Room database with Robolectric; device testing remains a separate check.

Firebase configuration is optional for local compilation. The account setup instructions are in `docs/FIREBASE_SETUP.md` when supplied. Keep `app/google-services.json` local and never put a Gemini server key in the app. The Gradle Google Services plugin is applied only when that configuration file exists.

Room is the source of dictionary data. Database version 1 is exported under `app/schemas`; later schema changes require explicit migrations. Cloud backup is disabled to keep vocabulary on the device. Names and vocabulary must not be written to logs.
