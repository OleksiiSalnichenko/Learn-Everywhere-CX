# 08 — Фінальне зміцнення playback

**Требования:** R43, R44, R45, R46
**Blocked by:** 06
**Зона:** playback runtime and PlaybackQueue tests
**Волна:** 6
**Status:** done

## Що повинно запрацювати

Закрити актуальні high-risk playback findings, відкинувши лише ті, які вже виправлені пізнішими repairs.

## Критерії приймання

- Bounded prefetch не завершує session завчасно; in-flight batch files protected; synth cancellation/throw always clears callback/temp files.
- Consumed items/cache refs bounded; audio-focus policy covers every transition; all preparation failures publish actionable state and settle foreground.
- Phase travels as typed event identity; controller connections have timeout/cancellation/surfaced failure.
- Failed-synthesis test has post-failure sentinel and exact problem assertion; stale MediaSession/showCard findings documented as already addressed, not reimplemented.
- Full build/lint green.

Не змінювати .autopilot або AGENTS; не комітити. Перед edit прочитати executor.md.

**Commit:** c0ac758
**Перевірка:** 12 targeted playback checks; final full build: 47 JVM tests, AndroidTest APK, 0 lint errors.
