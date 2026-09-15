# 03 — Додавання слова та Gemini

**Требования:** R01, R06, R08, R14, R15, R16, R17, R18, R19, R20, R21, G03
**Blocked by:** 01
**Зона:** app/src/main/java/com/learneverywhere/app/ui/home, app/src/main/java/com/learneverywhere/app/intake, app/src/main/java/com/learneverywhere/app/translation, app/src/main/res/values/strings_home.xml, app/src/test/java/com/learneverywhere/app/intake
**Волна:** 2
**Status:** ready

## Что должно заработать

Додавання слова та Gemini end-to-end у спільному Android app.

## Из брифа, дословно

> «Юзер може ввести слово українська, англійською або німецькою - система повинна розпізнати в який саме словник додати слово -> намецько-українськи або англійсько-український:»
> «система повинна визначити 2 найкращих перекладів і поставити його до <німецьке або англійське - значення 1> та <намецьке або англійське - значення 2>»
> «для кожного слова треба додати одне речення відповідною мовою - або німецькою англійською - <Речення з прикладом>»

## Разделы спецификации

spec.md §3,9,16,17,18 (пізніші уточнення пріоритетні).

## Критерии приёмки

- [ ] Home capture з погодженого A, microphone + text adjacent OK; підтримати SpeechRecognizer runtime permission, stop/cancel/no-speech, unavailable-service fallback без блокування тексту.
- [ ] Єдина WordIntake для голосу/тексту: sourceCandidates uk/de/en, Gift ambiguous explicit chooser; Ukrainian+None target chooser; German/English disregard mainLanguage для destination.
- [ ] Firebase AI Logic Google AI backend, gemini-3.5-flash-lite Standard, schema JSON + independent validation, no service keys APK, AppCheck debug/release; use constructors not invented APIs, official docs.
- [ ] Preview з українським, до2 foreign meanings, example, destination; тільки OK commits atomic default creation+word; Cancel no DB; duplicate submission/idempotence, destination changed reconfirm.
- [ ] NotConfigured/Quota/network/invalid content localized and preserve input. No fake production translations. Tests use explicit fake provider, tests route/duplicate/Cancel/missing fields.
- [ ] Provider credentials/config user-managed; do not read/log secrets. Parent coordinates Firebase project setup; only write configuration instructions/empty example if needed, no external project creation by executor.

## Правила виконання

Не змінювати .autopilot або AGENTS; не комітити. Встановлення необхідних стандартних build-dependencies з офіційних реєстрів у workspace або /tmp дозволене для виконання користувацького запиту. Не встановлювати глобальні невідомі пакети. При блокуванні tools — повідомити конкретний blocker, продовжити незалежне. Не читати попередні проєкти. Перед редагуванням прочитати skill prompts/executor.md.

## Стан Firebase на початку Wave 2

Батьківський агент створив новий Firebase-проєкт `learn-everywhere` на тарифі Spark. У Project Settings поки немає Android app, а AI Logic показує «To manage Firebase AI Logic, ask a project owner for the necessary permissions». Написати реальну SDK-інтеграцію та поведінку NotConfigured; не підкладати фальшивий production-відповідь. Консоль і реєстрацію Android app координує батьківський агент. Поточна модель `gemini-3.5-flash-lite` замінила 2.5 через дату вимкнення у жовтні 2026 року.
