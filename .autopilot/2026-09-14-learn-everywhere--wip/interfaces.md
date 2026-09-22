# Границы и швы

## Правила

Kotlin/Jetpack Compose/Room; package com.learneverywhere.app; minSdk26. Концепція A Resonance, Light/Dark, погоджена. Gemini Firebase AI Logic Spark, gemini-3.5-flash-lite Standard, no billing. Backend secret ніколи у APK. Початок коду авторизовано. Не відкривати інші проєкти.

Build/test: task01 establishes ./gradlew :app:assembleDebug :app:testDebugUnitTest; single test ./gradlew :app:testDebugUnitTest --tests CLASS. Toolchain cached: /home/osalnichenko/Android/Sdk (platforms36.1/37.0, build-tools36); ~/.gradle wrapper9.7.1, AGP9.3.2/9.4.0, Compose BOM2026.08.00. Java absent PATH; locate/install official JDK into /tmp as needed. Version compatibility must be verified, not presumed. Do not reuse source from previous projects.

Shared Gradle/Manifest/resources contracts owned01, all later resource names unique per feature; 02/03 own disjoint strings_library.xml/strings_home.xml and code zones. 04 integrates/translates resources. Parent owns git/.autopilot/AGENTS.

## Границы, решённые в спецификации


Це проєктні сигнатури, не створений код.

| Компонент | Володіє | Публічний контракт | Приховує |
|---|---|---|---|
| WordIntake | маршрутизація та чернетка | prepare(text, sourceHint?, targetLanguage?) → NeedSource / NeedTarget / Draft / Failure; confirm(draft) → Saved / DestinationChanged / Failure | normalisation, узгодження результатів, повторний submit |
| TranslationProvider | інтеграція перекладу | translate(text, source, target) → ukrainian, translation1, translation2?, example | провайдер, HTTP, parsing, auth |
| DictionaryRepository | Room і default | observeDictionaries(language); observeWords(dictionaryId); create; rename; setDefault; saveWord; updateWord; deleteWord; deleteDictionary | DAO, транзакції, FK, міграції |
| DictionaryTransfer | формат JSON | previewImport(stream) → ValidatedImport / Errors; commitImport(validated) → Summary; export(ids, stream) | version parser, ліміти, naming conflicts |
| SettingsRepository | налаштування | observeSettings() → Flow; update(settings) → Saved / Failure | DataStore schema і defaults |
| PlaybackPlan | порядок вимови | create(words, settings, randomSeed) → список Speak(text, language, wordId) / Silence(durationMs, wordId) | repetitions, shuffle, loop boundary |
| AudioPreparer | синтез і кеш | prepare(phrase, voice) → LocalAudio / MissingVoice / Failure | TextToSpeech callback, temp files, кеш |
| PlaybackController | керування сесією | play(defaultDictionaryId); pause; resume; stop; observeState | MediaController transport |
| PlaybackService | життєвий цикл звуку | MediaSession commands + session state | Player, audio focus, notification, знімок черги |

## Реалізований шов після Wave 1

`DictionaryRepository.saveWord(language: Language, expectedDictionaryId: String?, content: WordContent, draftId: String, automaticDictionaryName: String): SaveWordResult` — локалізовану назву передає UI. `SaveWordResult` є `Saved` або `DestinationChanged`. `expectedDictionaryId == null` означає, що під час preview у мові не було дефолту; якщо дефолт з'явився або був відновлений, користувач має підтвердити призначення повторно. Один `draftId` повертає раніше збережений результат.

`HomeScreen(repository, mainLanguage: Language?, automaticDictionaryName: (Language) -> String)` є boundary для task03. `LibraryScreen(repository, mainLanguage: Language? = DE, onPlay: (String) -> Unit = {})` — для task02. `AppContainer.mainLanguage: StateFlow<Language?>`, `setMainLanguage(language)` — пізніше підключає task04. `observeWords(dictionaryId, limit, offset)` має сторінки, `getWords(dictionaryId)` повертає повний впорядкований snapshot для JSON і playback. `importDictionaries(List<ImportDictionary>)` — повна транзакція; task02 розв'язує конфлікти назв до commit.

Після спільного шва Wave 2 `ImportDictionary(language, name, words, isDefault: Boolean = false)`. `importDictionaries` зберігає існуючий дефолт; якщо його немає, бере позначений `isDefault` імпортований словник або перший новий цієї мови. Під час startup `LearnEverywhereApplication` налаштовує App Check лише за наявності Firebase-конфігурації; debug/release фабрики рознесені в source sets. AndroidManifest містить RECORD_AUDIO і query RecognitionService.

`DictionaryRepository.getTransferSnapshot(ids: List<String>? = null): List<DictionaryExportSnapshot>` читає dictionary/default/ordered words в одній Room транзакції. `DictionaryExportSnapshot(dictionary: Dictionary, words: List<WordEntry>, isDefault: Boolean)`. `null` — all, порожній список — none, дублікати ID прибираються, невідомий ID дає `RepositoryException(MISSING_DICTIONARY)`; порядок DE→EN, потім createdAt/id і insertionOrder/id. Ticket02 використовує цей метод для узгодженого JSON експорту.

Ticket 02: `DictionaryTransfer` імпортує та експортує версійований JSON із перевіркою лімітів, показом preview і атомарним записом через repository. Експорт виконується поза UI-потоком; `PendingExportStore` тримає payload у приватному cache-файлі, а через `rememberSaveable` переживає recreation лише його ім’я. Library показує default окремою клікабельною карткою, не дублює його у звичайному списку, має локалізовані конфліктні назви та доступну семантику вибору.

Ticket 03: `WordIntake.prepare(input, targetLanguage, mainLanguage)` визначає uk/de/en, передає effective target провайдеру, повертає chooser при неоднозначності й готує draft без запису в Room. `confirm(draft)` зберігає через idempotent `draftId` та вимагає повторного підтвердження, якщо default destination змінився. `TranslationProvider.translate(TranslationRequest)` ізолює Firebase AI Logic; production implementation використовує JSON schema, timeout і строгий parser, а відсутня конфігурація повертає керовану помилку. Home підтримує текст і `SpeechRecognizer`, permission/stop/cancel, preview з OK-only commit та збереження review warnings через recreation.

Ticket 04: `SettingsRepository.observeSettings(): Flow<AppSettings>` і `update(transform: (AppSettings) -> AppSettings): SaveSettingsResult` атомарно змінюють актуальний знімок налаштувань у DataStore. `PlaybackPlan.create(words, settings, randomSeed, previousWordId): List<PlaybackEvent>` створює типізовану послідовність `Speak` / `Silence`; `order(...)` повертає порядок слів із детермінованим shuffle. `AppContainer.settings(context): SettingsRepository` є спільною точкою доступу, а `AppContainer.mainLanguage: StateFlow<Language?>` відображає збережене налаштування для Home і Library.

Перевірки концентруємо на WordIntake + DictionaryRepository для бізнес-правил і PlaybackPlan для таймінгів. Не тестуємо приватні методи заради покриття. Окремі платформні сценарії — Room instrumentation, MediaSession і Compose UI.

Обрано Gemini через Firebase AI Logic. Нижче наведено обґрунтування ізоляції провайдера; актуальна інтеграція описана у §18. Для онлайн-варіанта клієнт не повинен містити сервісний секрет у APK; якщо потрібен секретний ключ, необхідний погоджений backend/proxy з авторизацією і лімітами. Для публічного мобільного API допустимий лише його документований mobile-auth механізм. Ціну, акаунт і хостинг не вигадуємо; не реалізовуємо фальшивий production-переклад локальною таблицею. Для повністю офлайн-варіанта окремо перевіряються мовні моделі, ліцензії, розмір і якість двох значень та речень; це суттєва архітектурна розвилка.


## Остаточне рішення

spec.md §18 є чинним контрактом інтеграції, всі попередні provider/pause-open notes застаріли.
