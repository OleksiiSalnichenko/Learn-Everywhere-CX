# 07 — Фінальна якість UI та тестів

**Требования:** R01, R03, R06, R08, R10, R18, R24, R37, R39, R44
**Blocked by:** 06
**Зона:** intake/translation/settings/transfer/library UI and tests, docs, lint config
**Волна:** 6
**Status:** done

## Що повинно запрацювати

Закрити фінальний triage неблокуючих findings: user-visible preview/accessibility/settings issues, delete-session scoping, portable docs/lint opt-in, а також повторюваний assertion-quality борг у tickets 02–04.

## Критерії приймання

- Default card лише у list state; preview показує destination language й existing duplicate; unsupported language має distinct state; example contract вимагає first meaning; speech callbacks scoped/guarded.
- Choice dialogs scroll at large font; dictionary delete stops only matching active session; global Media3 lint suppression removed; Mermaid and JDK commands accurate/portable.
- Transfer/Gemini/HomeState/Settings/PlaybackPlan tests assert exact public results, all retry categories, literal defaults, failure/cancellation and shuffle multiset.
- Pending export TTL можна лишити як report-only concern; нових features не додавати.
- Full build/lint green.

Не змінювати .autopilot або AGENTS; не комітити. Перед edit прочитати executor.md.

**Commit:** 63c0703
**Перевірка:** 17 targeted checks; full build was green and later final suite reached 47 JVM tests.
