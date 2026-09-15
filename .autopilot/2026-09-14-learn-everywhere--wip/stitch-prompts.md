# Stitch — запити для НОВОГО проєкту

Статус: перший стислий запит відправлено 2026-09-14. Новий проєкт створено через Blank project: https://stitch.withgoogle.com/projects/13006564547979315790 . Назва Stitch: Learn Everywhere Android UI Mockups. Попередні проєкти не відкривалися і не використовувалися.

## Старт нового проєкту

Create a BRAND NEW native Android design project named "Learn Everywhere — Original concepts 2026-09-14". Do not use any existing project, template, prior conversation, reference screenshot or earlier design. Start from this brief only. These are design mockups for approval, NOT implementation.

Learn Everywhere is a personal audio vocabulary app for Ukrainian speakers learning German and English. It stores multiple dictionaries per language, one default for German and one for English. Default learning language is German; interface defaults to English. Show Ukrainian and German/English vocabulary inside an English UI. All sample words are illustrative, no invented user achievements or personal history.

Design two ORIGINAL, clearly different concepts, EACH in light and dark mode: A Light, A Dark, B Light, B Dark. Keep identical functionality and screen inventory. Mobile width 390 logical pixels, comfortable native Android safe areas and 48dp touch targets. Two bottom navigation destinations with ICONS ONLY: home and dictionaries. Settings via top-right gear, never a third navigation destination. Flag-only dictionary tabs: German and English. Accessibility labels are still required in handoff notes.

Concept A "Resonance": calm audio instrument. Warm ivory background, graphite text, deep teal accent; dark version ink background and pale mint accent. Central microphone with one refined sound-wave ring. Dictionary collection rows have narrow colored book-spine stripes and deliberate typography. Large default dictionary player card above the collection list. No dashboards or gamification.

Concept B "Fieldnotes": warm editorial index cards. Milk background, dark plum text, terracotta accent; dark version deep plum and peach. Asymmetric but restrained headline composition, central prominent microphone without A's concentric rings. Dictionaries as compact index cards with a folded corner marker and word count. This must change composition and visual character, not just colors.

Home: small brand and settings gear, prominent microphone centered, succinct prompt, compact direction caption, text input with adjacent OK button, two bottom icons. Minimal buttons. No unrelated metrics, streaks or onboarding funnel.

Dictionaries: flag tabs; first/selected German by default, English first if selected as main language; same layout in both. Header names the default dictionary with a large play control. Default dictionary first in list with "Default" badge. Every dictionary row has a distinct radio selector for default; tapping row opens that dictionary. Include Create dictionary and JSON import/export actions without overcrowding.

Dictionary detail replaces collection list on the SAME screen. Back, title, four top action icons: delete dictionary, rename dictionary, delete selected word, edit selected word (last two disabled without selection). Each compact but fully readable entry shows Ukrainian word, foreign translation 1, optional translation 2, one foreign example sentence. All text visible, wrapped. Use demo: "яблуко" / "der Apfel" / "Ich esse einen Apfel." Optional second meaning must collapse cleanly. Selection visibly marked.

Add result dialog: Ukrainian, 1–2 foreign meanings, example sentence, destination dictionary, OK/Cancel; nothing is saved before OK. Ukrainian input with main language None opens German/English chooser. Ambiguous source word opens source-language chooser.

Settings: main language None/German/English (German default); interface Ukrainian/English/German (English default); theme System/Light/Dark. Playback checkboxes Loop off, Shuffle off, Include example off, Show card on. Ukrainian repeats 1; pause between Ukrainian repeats 2s; before translation 3s; translation repeats 2; pause between translation repeats 2s; before example 2s; before next word 2s. All durations and repetitions selectable 1–6. Make temporal sequence visually understandable, not a wall of identical dropdowns. Settings can scroll and must show every control, even if split into two design frames.

Playback: current full word card, Ukrainian and translations, example, speaking phase and item position, pause/resume/stop. Optional card visibility does not remove playback controls. Audio keeps playing with the screen locked; show a companion Android media-notification mockup. No false promise of a working prototype.

Splash: original app icon centered on themed solid background, native Android splash constraints, no artificial progress bar. Icon concept: open book whose pages become two sound arcs, small negative-space play triangle; simple enough for small adaptive Android icons and monochrome variant. Do not use flags or tiny letters in the icon.

Required separate screens per concept and theme: Splash, Home idle, Home listening, Translation preview, Dictionaries German, Dictionaries English, Dictionary detail with selected word, Word edit dialog, Settings (all controls), Playback card. Also cover Empty library, New/Rename dialog, Target-language chooser, Ambiguous-language chooser, Import preview/errors and Delete confirmation. Word edit has all four fields and OK/Cancel. Empty play is disabled with meaningful message. Delete prompts name the dictionary and count.

Generate key Home + Dictionaries + Splash + Settings screens first for the four variants as a labeled comparison. Continue remaining screens in this SAME NEW project; preserve concept labels and do not drop any screen from the inventory. No external template imports.

## Наступний пакет після перевірки ключових екранів

Continue this newly created Learn Everywhere project only. Keep A and B distinct and retain Light/Dark pairs. Generate the remaining required screens from the original brief: translation confirmation with destination and OK/Cancel; dictionary detail with all 4 toolbar actions and full compact entries; edit dialog showing every field; current playback card and media notification; empty states; language choosers; create/rename/delete dialogs; JSON import review. If Settings was truncated, create a continuation showing every timing/repetition control and persistence note in handoff metadata. Display every screen separately for approval. Do not generate Kotlin or claim implementation.
