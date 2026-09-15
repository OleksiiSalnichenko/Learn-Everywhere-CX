# 02 — Керування словами та JSON

**Требования:** R10, R48, R49, R50, R51, R52, R53
**Blocked by:** 01
**Зона:** app/src/main/java/com/learneverywhere/app/ui/library, app/src/main/java/com/learneverywhere/app/transfer, app/src/main/res/values/strings_library.xml
**Волна:** 2
**Status:** ready

## Что должно заработать

Керування словами та JSON end-to-end у спільному Android app.

## Из брифа, дословно

> «Додати можливість імпортувати і експортувати словники з JSON.»
> «По натиску на словник, показується цей словник в цьому ж екрані на місці попереднього списку:»
> «тут відображається повна інформація по повному айтему. Сгрупируй якось, щоб займіло мало місця і було читабельно.»

## Разделы спецификации

spec.md §4,7,8,9 (пізніші уточнення пріоритетні).

## Критерии приёмки

- [ ] Відкриття словника в тому самому екрані; усі поля компактно і повністю, nullable second translation прихований.
- [ ] Чотири toolbar дії delete dictionary / rename / delete selected / edit selected; selection одного слова; disabled якщо відсутня; confirm destructive, OK/Cancel редагування всіх полів.
- [ ] Create/rename валідація 1–60 символів і конфліктів; після delete default repository коректно вибирає іншого; callback зупинити активний словник при видаленні.
- [ ] SAF OpenDocument/CreateDocument; JSON version1, full validation, 10MiB/100 dictionaries/10000 words, preview+confirm, no partial writes, копії при конфліктах, точний word order; export one/all.
- [ ] Тести roundtrip/null optional, malformed/oversized/unknown-version, mixed language defaults і rollback; всі строки resource-based у strings_library.xml.

## Правила виконання

Не змінювати .autopilot або AGENTS; не комітити. Встановлення необхідних стандартних build-dependencies з офіційних реєстрів у workspace або /tmp дозволене для виконання користувацького запиту. Не встановлювати глобальні невідомі пакети. При блокуванні tools — повідомити конкретний blocker, продовжити незалежне. Не читати попередні проєкти. Перед редагуванням прочитати skill prompts/executor.md.
