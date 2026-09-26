# 05 — Фоновий аудіоплеєр

**Требования:** R36, R43, R44, R45, R46
**Blocked by:** 04
**Зона:** app/src/main/java/com/learneverywhere/app/playback, app/src/main/java/com/learneverywhere/app/ui/player, app/src/main/AndroidManifest.xml
**Волна:** 4
**Status:** ready

## Что должно заработать

Фоновий аудіоплеєр end-to-end у спільному Android app.

## Из брифа, дословно

> «Показати картку - чекбокс. По замовчуванні ввімкнено.»
> «Зверху, щось типу хедера - де юзер бачить дефалтовий плейлист та поруч велику кнопку плей.»
> «Плей програє слова зі дефалтового словника по порядку, згідно правил вказанних в налаштуванні - 3.3, або програвання випадковим порядком, якщо 3.3.2.»

## Разделы спецификации

spec.md §5,6,9,11 (пізніші уточнення пріоритетні).

## Критерии приёмки

- [ ] MediaSessionService+ExoPlayer, proper foreground mediaPlayback permissions/session/notification; Activity lifecycle independent, start user action only.
- [ ] TTS synthesizeToFile callbacks + language/voice check; bounded cache100MB, atomic files, window prefetch; silence media segments pause/resume accurate, no UI delay timers owning service.
- [ ] Play DEFAULT dictionary; immutable snapshot, showCard toggle full current word, phase/index, pause/resume/stop; zero words disabled and errors actionable.
- [ ] Audio focus/noisy pause, lockscreen controls, Stop releases resources; force stop ends session, no unrequested auto resume.
- [ ] Device/emulator verification when possible screen off + notification; unit tests service collaborators failing synth/empty queue/order; report physical device limitation honestly.

## Правила виконання

Не змінювати .autopilot або AGENTS; не комітити. Встановлення необхідних стандартних build-dependencies з офіційних реєстрів у workspace або /tmp дозволене для виконання користувацького запиту. Не встановлювати глобальні невідомі пакети. При блокуванні tools — повідомити конкретний blocker, продовжити незалежне. Не читати попередні проєкти. Перед редагуванням прочитати skill prompts/executor.md.
