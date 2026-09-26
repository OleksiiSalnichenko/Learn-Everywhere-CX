<!-- autopilot:start -->
# Learn Everywhere

Android-застосунок (`com.learneverywhere.app`, minSdk 26) для накопичення німецьких та англійських слів з українським значенням, JSON import/export, голосового або текстового вводу через Firebase AI Logic і фонового аудіоуроку. Локальні словники, налаштування та playback працюють без Firebase-конфігурації.

## Команди

- Потрібен JDK 21; portable Linux setup без username: `export JAVA_HOME="$HOME/android-studio/jbr"` (або вкажи інший абсолютний JDK 21), потім перевір `"$JAVA_HOME/bin/java" -version`.
- Повна перевірка, виконана успішно: `JAVA_HOME="$HOME/android-studio/jbr" ./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug --no-daemon` — `BUILD SUCCESSFUL`, 47 JVM-тестів, 0 lint errors.
- Один JVM test class: `JAVA_HOME="$HOME/android-studio/jbr" ./gradlew :app:testDebugUnitTest --tests com.learneverywhere.app.intake.WordIntakeTest --no-daemon`.

## Структура і ключові файли

- `app/src/main/java/com/learneverywhere/app/MainActivity.kt` — composition root, Home/Library/Settings навігація, `AppContainer`, locale/theme та `PlayerCard`; `app/src/main/java/com/learneverywhere/app/LearnEverywhereApplication.kt` умовно запускає Firebase App Check.
- `app/src/main/java/com/learneverywhere/app/data/` — Room schema/DAO та єдина межа словників; schema snapshot лежить у `app/schemas/com.learneverywhere.app.data.DictionaryDatabase/1.json`.
- `app/src/main/java/com/learneverywhere/app/intake/WordIntake.kt` + `app/src/main/java/com/learneverywhere/app/translation/` — визначення мови, сувора повторна валідація model output, preview/duplicate flow та commit після підтвердження.
- `app/src/main/java/com/learneverywhere/app/transfer/DictionaryTransfer.kt` + `app/src/main/java/com/learneverywhere/app/ui/library/PendingExportStore.kt` — versioned JSON, preview до атомарного імпорту, узгоджений export snapshot і приватний pending-файл на час SAF picker.
- `app/src/main/java/com/learneverywhere/app/settings/` — Preferences DataStore; `app/src/main/java/com/learneverywhere/app/playback/plan/PlaybackPlan.kt` — чистий детермінований план; `app/src/main/java/com/learneverywhere/app/playback/PlaybackRuntimeCoordinator.kt` — authoritative queue/cache/failure state machine; `app/src/main/java/com/learneverywhere/app/playback/PlaybackService.kt` — Android MediaSession/ExoPlayer/TTS adapter.
- `app/src/main/java/com/learneverywhere/app/ui/` містить Compose UI; локалізовані feature strings дзеркально лежать у `app/src/main/res/values/`, `app/src/main/res/values-uk/`, `app/src/main/res/values-de/`; JVM/device тести — у `app/src/test/` та `app/src/androidTest/`.

## Архітектурні межі й потоки даних

- UI залежить від `DictionaryRepository`, `SettingsRepository`, `TranslationProvider` і `PlaybackController`; Room, DataStore, Firebase та Media3 приховані за цими межами, а очікувані помилки повертаються sealed result/state типами.
- Home: text/`SpeechRecognizer` → `WordIntake.prepare` → `TranslationProvider.translate` → локальна перевірка/preview → `confirm` → `DictionaryRepository.saveWord`; `draftId` робить повторний confirm ідемпотентним, а зміна default destination вимагає другого підтвердження.
- Library: Room `Flow` → Compose; import читається й перевіряється до `importDictionaries` в одній транзакції; export бере dictionary/default/повні ordered words через `getTransferSnapshot` в одній read-транзакції, потім пише SAF stream поза UI thread.
- Settings: `observeSettings()` живить locale/theme/main language/playback; лише `update(transform)` безпечно поєднує швидкі зміни актуального DataStore snapshot; `AppContainer.mainLanguage` — `StateFlow<Language?>`, де `null` означає None.
- Playback: `PlaybackController.play/retry` запускає foreground service, а pause/resume/stop ідуть через `MediaController` з 5 s timeout; service перевіряє default dictionary й знімає immutable words/settings snapshot → `PlaybackPlan` → bounded preparation → `PlaybackRuntimeCoordinator` → ExoPlayer/MediaSession і `PlaybackUiState`.
- `PlaybackRuntimeCoordinator` є єдиним власником append-in-flight, end-of-queue resume, queued/in-flight cache leases, failure cleanup і foreground settlement; `sessionGeneration` у service відкидає результати скасованої або заміненої сесії.
- Audio boundary: TTS запити серіалізовані mutex, мають 60 s timeout і видаляють temp після failure/cancel; WAV commit атомарний, cache обмежений 100 MB, а coordinator захищає queued та ще не передані ExoPlayer файли від eviction.
- Focus/error boundary: play починається лише після `AUDIOFOCUS_GAIN`; denial стає `PlaybackControlFailure.AUDIO_FOCUS_DENIED`, focus loss/transient/duck і noisy route ставлять transport на pause та віддають focus. Start/controller/focus failures зберігають dictionary retry context у `PlaybackUiState.Error.controlFailure`; preparation лишає окремі `MISSING_VOICE`/`SYNTHESIS_FAILED`/`STORAGE_FAILED`.

## Репозиторні конвенції

- Усі зміни dictionary/default/word/import, що мають бути узгодженими, виконуються в Room transaction; default належить мові, а після delete/legacy gap `repairDefaults` обирає найстаріший словник.
- `Language` містить лише DE/EN; українська існує як `SourceLanguage.UK`/`SpeechLanguage.UK`. Автоматичні назви словників локалізує UI й передає repository, data layer не читає resources.
- Модельний JSON вважається недовіреним: Firebase schema, `parseTranslation` і `WordIntake` перевіряють його окремо; `CancellationException` завжди прокидається далі, інші provider/storage помилки мапляться на domain reason.
- Gemini `detectedSource=unsupported` стає неретраєбельним `UNSUPPORTED_LANGUAGE`; Home зберігає review draft як JSON через `rememberSaveable`, а один `SpeechRecognizer` session лишається зайнятим до callback або cancel.
- JSON transfer schemaVersion = 1 не містить Room IDs/timestamps; name collisions локалізовано перейменовуються до commit, чинний default імпорт не замінює.
- Нові UI strings додавай до відповідного feature-файлу в усіх трьох locale-наборах; прості назви, зрозумілі конструкції та коментарі мають пояснювати лише неочевидні інваріанти.

## Оточення та конфігурація

- `JAVA_HOME` — JDK 21; Android SDK знаходиться стандартним `ANDROID_HOME`/`ANDROID_SDK_ROOT` або Android tooling. Не фіксуй user-specific абсолютні шляхи.
- `app/google-services.json` — optional local Firebase Android config і зараз відсутній; Gradle застосовує Google Services plugin лише коли файл існує. Не комітити config, App Check debug token або будь-які секрети; server Gemini key у APK не використовується.
- Debug source set ставить App Check Debug provider, release — Play Integrity; model name заданий у `GeminiTranslationProvider.MODEL`.

## Перевірки й підводні камені

- 47 JVM-тестів покривають repository/intake/transfer/settings/translation/playback/state, включно з runtime coordinator, cache leases, queue underrun, focus denial і controller timeout; `DictionaryRepositoryDeviceTest` перевіряє реальний Room persistence/transactions. Не тестуй private методи замість boundary behavior.
- Потрібні ручні Android-перевірки: SAF import/export, permission/cancel/stop для voice input, notification/lockscreen/audio-focus/noisy-route playback, process recreation, системні EN/UK/DE locale, dark mode й великий шрифт.
- Живий Gemini smoke вимагає правильного `app/google-services.json`, завершеного Firebase AI Logic setup і зареєстрованого debug App Check token; успішна compile без config цього не перевіряє. Enable APIs приймає зовнішні terms/policies, тому натискати можна лише після явного схвалення користувача.
- `saveWord(expectedDictionaryId = null)` може спершу відновити наявний default і повернути `DestinationChanged`; `getTransferSnapshot(null)` означає all, порожній список — none, невідомий ID — `MISSING_DICTIONARY`.

## Autopilot

Вимоги й стан лежать у `.autopilot/`; на продовженні спочатку читай цей файл, потім `.autopilot/state.js` та `.autopilot/2026-09-14-learn-everywhere/interfaces.md`. Лише користувач може скасувати Autopilot; готові коміти надсилати в `develop`, повторного дозволу на продовження не просити.
<!-- autopilot:end -->
