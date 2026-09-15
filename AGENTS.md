<!-- autopilot:start -->
# Learn Everywhere

Android-застосунок для вивчення німецьких та англійських слів через український переклад і аудіосписки.

Стек за брифом: Kotlin, Jetpack Compose, Room. Wave 1 реалізована: Android-основа, локальні словники і 6 Room unit-тестів. Home/Settings і аудіо ще розвиваються у наступних хвилях.

## Обмеження користувача

- Користувач погодив показаний дизайн і 2026-09-15 сказав «го» після паузи: продовження авторизоване. Повторно дозвіл розробляти не запитувати. Уточнення A/B і режиму перекладу зберігаються у state.js.
- Не використовувати ідеї попереднього проєкту. У Stitch створити новий проєкт; існуючі не використовувати.
- Готові коміти надсилати в develop.
- Прості назви, зрозумілі конструкції, пояснювальні коментарі.
- Після обмеження «закінчишь Wave 1 - зупинишься» користувач дозволив продовжити Wave 2. Зупинитися після Wave 2 до наступної команди.

## Autopilot

Вимоги і стан у `.autopilot/`; продовжувати зі `state.js`. Лише користувач може зняти вимогу.

## Поточні технічні шви

- `DictionaryRepository.saveWord(language, expectedDictionaryId, content, draftId, automaticDictionaryName)` приймає назву автоматичного словника, локалізовану в UI. Якщо словники є, але дефолт відсутній, repository відновлює дефолт і вимагає повторного підтвердження призначення.
- `AppContainer.mainLanguage` є `StateFlow<Language?>`; `None` ставить German вкладку бібліотеки першою. Постійні налаштування належать Wave 3.
- Новий Firebase-проєкт `learn-everywhere` на Spark та Android app `com.learneverywhere.app` зареєстровано. У консолі AI Logic відкрито wizard, але кнопка Enable APIs означає прийняття Gemini API Additional Terms та usage policies; чекаємо явного схвалення користувача. Новий `app/google-services.json` ще не збережено в workspace; старий файл іншого проєкту відкинуто. Config і debug токени не комітити. Для Gemini обрано 3.5 Flash-Lite, бо 2.5 вимикають у жовтні 2026.
- Wave 2 shared seam: `ImportDictionary.isDefault` зберігається атомарно; за наявності чинного дефолту він лишається, інакше обирається позначений імпортом або перший. Firebase App Check ініціалізується при старті лише за наявності конфігурації: debug провайдер окремий від release Play Integrity. Manifest містить RECORD_AUDIO і query RecognitionService.
- Для JSON-експорту `DictionaryRepository.getTransferSnapshot(ids: List<String>? = null)` повертає словники, повні впорядковані слова й прапор дефолту в одній Room read-транзакції. `null` означає всі словники; невідомий ID — явна помилка.
<!-- autopilot:end -->
