# 04 — Налаштування, локалізація та план уроку

**Требования:** R24, R25, R26, R27, R28, R29, R30, R31, R32, R33, R34, R35, R36, R37, R38, R39, R44
**Blocked by:** 02, 03
**Зона:** app/src/main/java/com/learneverywhere/app/ui/settings, app/src/main/java/com/learneverywhere/app/settings, app/src/main/java/com/learneverywhere/app/playback/plan, app/src/main/res
**Волна:** 3
**Status:** ready

## Что должно заработать

Налаштування, локалізація та план уроку end-to-end у спільному Android app.

## Из брифа, дословно

> «опція - головна мова - 3 стани: ніяка - німецька - англійська. Німецька - дефалтова.»
> «Блок налаштування програвання:»
> «зациклювання - чекбокс»

## Разделы спецификации

spec.md §5,6,9 (пізніші уточнення пріоритетні).

## Критерии приёмки

- [ ] Усі settings §5 через DataStore із коректними defaults і restart persistence; None/German/English main; UI en default/uk/de незалежна.
- [ ] Доступні всі числові 1–6 pickers durations and repeats, checkbox loop/shuffle/includeExample/showCard, Theme System/Light/Dark; no omitted settings.
- [ ] PlaybackPlan typed Speak/Silence ms with exact sentence full length, repetitions 1/2 meanings, no extra trailing gaps, shuffle permutation + deterministic seed tests, loop boundary.
- [ ] Повні переклади всіх UI/dialog/errors/accessibility strings en/uk/de; large-font scrolling; current audio settings apply next session, showCard/theme immediately.
- [ ] Test DataStore roundtrip/defaults and plan orders/durations at boundary cases. Integrate existing Home/Library no duplicated repository.

## Правила виконання

Не змінювати .autopilot або AGENTS; не комітити. Встановлення необхідних стандартних build-dependencies з офіційних реєстрів у workspace або /tmp дозволене для виконання користувацького запиту. Не встановлювати глобальні невідомі пакети. При блокуванні tools — повідомити конкретний blocker, продовжити незалежне. Не читати попередні проєкти. Перед редагуванням прочитати skill prompts/executor.md.
