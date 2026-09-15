# 06 — Приймальні перевірки і документація

**Требования:** R01, R02, R03, R04, R05, R06, R07, R08, R09, R10, R11, R12, R13, R14, R15, R16, R17, R18, R19, R20, R21, R22, R23, R24, R25, R26, R27, R28, R29, R30, R31, R32, R33, R34, R35, R36, R37, R38, R39, R40, R41, R42, R43, R44, R45, R46, R47, R48, R49, R50, R51, R52, R53, R54, R55, R56, R57, R58, R59, R60, R61, R62, G01, G02, G03
**Blocked by:** 05
**Зона:** app/src/androidTest, docs, README.md, .github
**Волна:** 5
**Status:** ready

## Что должно заработать

Приймальні перевірки і документація end-to-end у спільному Android app.

## Из брифа, дословно

> «Юзер може ввести слово українська, англійською або німецькою - система повинна розпізнати в який саме словник додати слово -> намецько-українськи або англійсько-український:»
> «відповідно у нас буде в відповідному скріні 2 таби - зі словами німецькою або англійською.»
> «у кожної мови є дефолтовий словник. у юзара повинна бути можливість міняти цей флаг. якщо словника не має - система створює новий словник, та ставить його дефолтовим.»

## Разделы спецификации

spec.md §1–18 (пізніші уточнення пріоритетні).

## Критерии приёмки

- [ ] Full acceptance §11 across input/library/transfer/settings/playback/localization; fix integration defects using review evidence, preserve approved A look.
- [ ] Build debug APK, assemble/test/lint, Room migrations no destructive fallback; verify readable small-screen/large-font/dialog keyboards and accessibility.
- [ ] Actual live Firebase translation smoke if configured; missing setup never reported as pass; no paid billing. Document exact remaining manual steps.
- [ ] Clear run/build/test docs, comments in important rules, APK path. No user data in source/logs; no API secrets; parent handles commits/push develop.
- [ ] No regressions from system-back/default selection/foreground playback; inventory UI and all requirements; fixes targeted not new features.

## Правила виконання

Не змінювати .autopilot або AGENTS; не комітити. Встановлення необхідних стандартних build-dependencies з офіційних реєстрів у workspace або /tmp дозволене для виконання користувацького запиту. Не встановлювати глобальні невідомі пакети. При блокуванні tools — повідомити конкретний blocker, продовжити незалежне. Не читати попередні проєкти. Перед редагуванням прочитати skill prompts/executor.md.
