# 01 — Android-основа і локальні словники

**Требования:** R02, R03, R04, R05, R07, R09, R11, R12, R13, R22, R23, R40, R41, R42, R43, R47, R54, R55, R56, R59, G02
**Blocked by:** —
**Зона:** app/build.gradle.kts, gradle, app/src/main/java/com/learneverywhere/app/data, app/src/main/java/com/learneverywhere/app/ui/theme, app/src/main/java/com/learneverywhere/app/MainActivity.kt
**Волна:** 1
**Status:** ready

## Что должно заработать

Android-основа і локальні словники end-to-end у спільному Android app.

## Из брифа, дословно

> «відповідно у нас буде в відповідному скріні 2 таби - зі словами німецькою або англійською.»
> «у кожної мови є дефолтовий словник. у юзара повинна бути можливість міняти цей флаг. якщо словника не має - система створює новий словник, та ставить його дефолтовим.»
> «словник - це список слів наступного формату:»

## Разделы спецификации

spec.md §2,3.2,4,6,7,9,10,16,18 (пізніші уточнення пріоритетні).

## Критерии приёмки

- [ ] Відтворювана Gradle Android debug збірка + unit tests; Kotlin/Compose/Room; minSdk26, перевірені сумісні SDK/версії. Не використовувати інші проєкти як джерело.
- [ ] Погоджений A Resonance: ivory #F7F5EF / graphite #172C2D / teal #0D7377; dark #0E1415 / mint #4ECCA3. Простий книжка-звук adaptive icon і splash; два нижні icon-only таби Home/Dictionary; gear Settings.
- [ ] Room Dictionary/WordEntry/LanguageDefault; composite FK для default language, cascade, індекси, stable order, транзакції. Усі методи repository create/rename/setDefault/save/update/delete доступні для наступних тасків.
- [ ] Після першого підтвердженого збереження автоматично створений словник має локалізовану назву «My German words» / «My English words» у поточній мові UI. За відсутності дефолту серед наявних словників відновити дефолт, не створювати зайву колекцію.
- [ ] Library з прапорними табами, правильним mainLanguage order, першим default, radio та окремою click-зоною; create dictionary і порожній стан. Локальне збереження реально працює.
- [ ] MainActivity передає поточну app-level mainLanguage в LibraryScreen (None відкриває German першим); Library не ховає слова після сотого запису.
- [ ] Визначити public composable boundaries HomeScreen, LibraryScreen, SettingsScreen і playback hooks, щоб наступні таски змінювали свої зони; transient shells чесні, не fake translations.
- [ ] Реальні unit/instrumented тести на default invariants, rollback/missing dictionary і persistence; мінімум компіляція debug, тести що можливо; не називати непроведений тест успішним.
- [ ] Підготувати залежності для наступних тасків (DataStore, Media3, Firebase AI Logic/AppCheck, serialization, coroutines) у спільному Gradle конфігу. Firebase setup optional, absent configuration must not break build.

## Правила виконання

Не змінювати .autopilot або AGENTS; не комітити. Встановлення необхідних стандартних build-dependencies з офіційних реєстрів у workspace або /tmp дозволене для виконання користувацького запиту. Не встановлювати глобальні невідомі пакети. При блокуванні tools — повідомити конкретний blocker, продовжити незалежне. Не читати попередні проєкти. Перед редагуванням прочитати skill prompts/executor.md.
