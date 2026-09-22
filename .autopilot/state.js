window.STATE =
{
  "slug": "learn-everywhere",
  "dir": "2026-09-14-learn-everywhere--wip",
  "title": "Learn Everywhere",
  "mode": "semi",
  "depth": "normal",
  "polish": null,
  "tier": "T2",
  "briefFile": "2026-09-14-brief.md",
  "memoryFile": "AGENTS.md",
  "skillDir": "/home/osalnichenko/.agents/skills/autopilot",
  "startedAt": "2026-09-14T20:08:30+02:00",
  "updatedAt": "2026-09-23T00:36:13+02:00",
  "finishedAt": null,
  "stages": [
    {
      "id": "preflight",
      "status": "done",
      "startedAt": "2026-09-14T20:08:30+02:00",
      "finishedAt": "2026-09-14T20:09:27+02:00"
    },
    {
      "id": "manifest",
      "status": "done",
      "startedAt": "2026-09-14T20:09:27+02:00",
      "finishedAt": "2026-09-14T20:16:19+02:00"
    },
    {
      "id": "briefing",
      "status": "done",
      "startedAt": "2026-09-14T20:16:19+02:00",
      "note": "A Light/Dark; Gemini/Firebase Spark погоджені.",
      "finishedAt": "2026-09-15T07:52:04+02:00"
    },
    {
      "id": "spec",
      "status": "done",
      "startedAt": "2026-09-15T07:52:04+02:00",
      "finishedAt": "2026-09-15T07:54:13+02:00"
    },
    {
      "id": "plan",
      "status": "done",
      "startedAt": "2026-09-15T07:54:13+02:00",
      "note": "6 тасків, 5 хвиль; 02/03 паралельно після 01.",
      "finishedAt": "2026-09-15T07:54:25+02:00"
    },
    {
      "id": "build",
      "status": "in-progress",
      "startedAt": "2026-09-15T07:54:25+02:00",
      "note": "Wave 4: фоновий аудіоплеєр"
    },
    {
      "id": "review",
      "status": "in-progress",
      "startedAt": "2026-09-22T11:28:16+02:00",
      "note": "Рев’ю таску 05 за Manifest/Spec і Craft"
    },
    {
      "id": "final",
      "status": "pending"
    }
  ],
  "requirements": {
    "total": 65,
    "done": 56,
    "inTicket": 9,
    "inSpec": 0,
    "placeholder": 0,
    "deferred": 0,
    "dropped": 0
  },
  "tickets": [
    {
      "id": "01",
      "title": "Android-основа і локальні словники",
      "requirements": [
        "R02",
        "R03",
        "R04",
        "R05",
        "R07",
        "R09",
        "R11",
        "R12",
        "R13",
        "R22",
        "R23",
        "R40",
        "R41",
        "R42",
        "R43",
        "R47",
        "R54",
        "R55",
        "R56",
        "R59",
        "G02"
      ],
      "blockedBy": [],
      "wave": 1,
      "zone": [
        "app/build.gradle.kts",
        "gradle",
        "app/src/main/java/com/learneverywhere/app/data",
        "app/src/main/java/com/learneverywhere/app/ui/theme",
        "app/src/main/java/com/learneverywhere/app/MainActivity.kt"
      ],
      "status": "done",
      "retries": 1,
      "repairs": 1,
      "handoffs": 0,
      "file": "01-foundation.md",
      "startedAt": "2026-09-15T07:54:25+02:00",
      "finishedAt": "2026-09-15T12:47:06+02:00"
    },
    {
      "id": "02",
      "title": "Керування словами та JSON",
      "requirements": [
        "R10",
        "R48",
        "R49",
        "R50",
        "R51",
        "R52",
        "R53"
      ],
      "blockedBy": [
        "01"
      ],
      "wave": 2,
      "zone": [
        "app/src/main/java/com/learneverywhere/app/ui/library",
        "app/src/main/java/com/learneverywhere/app/transfer",
        "app/src/main/res/values/strings_library.xml"
      ],
      "status": "done",
      "retries": 0,
      "repairs": 1,
      "handoffs": 0,
      "file": "02-library-json.md",
      "startedAt": "2026-09-15T12:49:06+02:00",
      "executor": "/root/w2_library_repair",
      "finishedAt": "2026-09-22T10:49:47+02:00",
      "tests": "Full build green: debug/release APK, unit tests, AndroidTest APK; 7 targeted Ticket02 tests passed",
      "commit": "0193116"
    },
    {
      "id": "03",
      "title": "Додавання слова та Gemini",
      "requirements": [
        "R01",
        "R06",
        "R08",
        "R14",
        "R15",
        "R16",
        "R17",
        "R18",
        "R19",
        "R20",
        "R21",
        "G03"
      ],
      "blockedBy": [
        "01"
      ],
      "wave": 2,
      "zone": [
        "app/src/main/java/com/learneverywhere/app/ui/home",
        "app/src/main/java/com/learneverywhere/app/intake",
        "app/src/main/java/com/learneverywhere/app/translation",
        "app/src/main/res/values/strings_home.xml",
        "app/src/test/java/com/learneverywhere/app/intake"
      ],
      "status": "done",
      "retries": 0,
      "repairs": 1,
      "handoffs": 0,
      "file": "03-intake-gemini.md",
      "startedAt": "2026-09-15T12:49:06+02:00",
      "executor": "/root/w2_intake_repair",
      "finishedAt": "2026-09-22T10:55:00+02:00",
      "tests": "Full build green: debug/release APK, unit tests, AndroidTest APK; 15 targeted Ticket03 tests passed",
      "commit": "d62f69a"
    },
    {
      "id": "04",
      "title": "Налаштування, локалізація та план уроку",
      "requirements": [
        "R24",
        "R25",
        "R26",
        "R27",
        "R28",
        "R29",
        "R30",
        "R31",
        "R32",
        "R33",
        "R34",
        "R35",
        "R36",
        "R37",
        "R38",
        "R39",
        "R44"
      ],
      "blockedBy": [
        "02",
        "03"
      ],
      "wave": 3,
      "zone": [
        "app/src/main/java/com/learneverywhere/app/ui/settings",
        "app/src/main/java/com/learneverywhere/app/settings",
        "app/src/main/java/com/learneverywhere/app/playback/plan",
        "app/src/main/res"
      ],
      "status": "done",
      "retries": 0,
      "repairs": 1,
      "handoffs": 0,
      "file": "04-settings-plan.md",
      "startedAt": "2026-09-22T11:07:01+02:00",
      "executor": "/root/w3_settings_plan",
      "finishedAt": "2026-09-22T11:41:13+02:00",
      "tests": "Full build green: debug/release APK, 37 unit tests, AndroidTest APK; 7 Ticket04 tests passed",
      "commit": "f7850bb"
    },
    {
      "id": "05",
      "title": "Фоновий аудіоплеєр",
      "requirements": [
        "R36",
        "R43",
        "R44",
        "R45",
        "R46"
      ],
      "blockedBy": [
        "04"
      ],
      "wave": 4,
      "zone": [
        "app/src/main/java/com/learneverywhere/app/playback",
        "app/src/main/java/com/learneverywhere/app/ui/player",
        "app/src/main/AndroidManifest.xml"
      ],
      "status": "repair",
      "retries": 1,
      "repairs": 2,
      "handoffs": 0,
      "file": "05-playback.md",
      "startedAt": "2026-09-22T11:42:53+02:00",
      "executor": "/root/w4_playback_resume"
    },
    {
      "id": "06",
      "title": "Приймальні перевірки і документація",
      "requirements": [
        "R01",
        "R02",
        "R03",
        "R04",
        "R05",
        "R06",
        "R07",
        "R08",
        "R09",
        "R10",
        "R11",
        "R12",
        "R13",
        "R14",
        "R15",
        "R16",
        "R17",
        "R18",
        "R19",
        "R20",
        "R21",
        "R22",
        "R23",
        "R24",
        "R25",
        "R26",
        "R27",
        "R28",
        "R29",
        "R30",
        "R31",
        "R32",
        "R33",
        "R34",
        "R35",
        "R36",
        "R37",
        "R38",
        "R39",
        "R40",
        "R41",
        "R42",
        "R43",
        "R44",
        "R45",
        "R46",
        "R47",
        "R48",
        "R49",
        "R50",
        "R51",
        "R52",
        "R53",
        "R54",
        "R55",
        "R56",
        "R57",
        "R58",
        "R59",
        "R60",
        "R61",
        "R62",
        "G01",
        "G02",
        "G03"
      ],
      "blockedBy": [
        "05"
      ],
      "wave": 5,
      "zone": [
        "app/src/androidTest",
        "docs",
        "README.md",
        ".github"
      ],
      "status": "pending",
      "retries": 0,
      "repairs": 0,
      "handoffs": 0,
      "file": "06-acceptance.md"
    }
  ],
  "singlePass": null,
  "tests": null,
  "debt": {
    "placeholders": [],
    "assumptions": [
      "ukrainianRepeats=1; loop=false; shuffle=false",
      "2 секунди перед прикладом; речення повністю",
      "Два концепти, кожен Light/Dark; System theme",
      "minSdk 26 як пропозиція; provider ще не обраний"
    ],
    "emptyEnv": []
  },
  "additions": [
    {
      "quote": "закінчишь Wave 1 - зупинишься",
      "effect": "Wave 1 pause requested; superseded by later explicit Wave 2 continuation"
    },
    {
      "quote": "продовжуй Wave 2",
      "effect": "Complete Waves 1 and 2, then stop before Wave 3"
    },
    {
      "quote": "го",
      "effect": "Resume Wave 2 after temporary subagent usage limit",
      "at": "2026-09-15T17:57:39+02:00"
    },
    {
      "quote": "го",
      "effect": "Resume Wave 2 again after subagent usage reset",
      "at": "2026-09-15T23:13:08+02:00"
    },
    {
      "quote": "продовжуй",
      "effect": "Resume Wave 2 repairs after session interruption",
      "at": "2026-09-22T10:41:45+02:00"
    }
  ],
  "coverage": {
    "status": "passed",
    "reviewer": "/root/spec_coverage",
    "findings": 2,
    "items": [
      {
        "finding": "Translation provider/online-offline undecided",
        "resolution": "Pending user decision; G2 not passed"
      },
      {
        "finding": "Unrequested session restoration after restart",
        "resolution": "Removed; force stop ends session"
      }
    ],
    "finalResult": "Independent G2 PASS; 2 findings resolved"
  },
  "concerns": [
    {
      "ticket": "02",
      "axis": "craft",
      "file": "LibraryScreen.kt:146-165",
      "finding": "Default card remains visible in detail state; keep it list-only"
    },
    {
      "ticket": "02",
      "axis": "craft",
      "file": "PendingExportStore.kt:8-28",
      "finding": "Abandoned SAF pending files have no expiry cleanup"
    },
    {
      "ticket": "02",
      "axis": "craft",
      "file": "DictionaryTransferTest.kt:14-50",
      "finding": "Some negative tests assert location/type rather than the exact TransferProblem"
    },
    {
      "ticket": "03",
      "axis": "spec",
      "file": "HomeScreen.kt:238",
      "finding": "Preview should display destination language together with dictionary name"
    },
    {
      "ticket": "03",
      "axis": "spec",
      "file": "HomeScreen.kt:83-84,239",
      "finding": "Duplicate preview warns but does not show the existing record"
    },
    {
      "ticket": "03",
      "axis": "spec",
      "file": "TranslationProvider.kt:23-24",
      "finding": "Unsupported-language input has no distinct explanatory state"
    },
    {
      "ticket": "03",
      "axis": "spec",
      "file": "GeminiTranslationProvider.kt:43",
      "finding": "Example contract does not explicitly ensure use of the first meaning"
    },
    {
      "ticket": "03",
      "axis": "craft",
      "file": "HomeScreen.kt:130-196",
      "finding": "Speech callbacks are not scoped to a session and control calls need runtime-error guards"
    },
    {
      "ticket": "03",
      "axis": "craft",
      "file": "GeminiTranslationProvider.kt:53-60",
      "finding": "Firebase error classification depends on exception message text"
    },
    {
      "ticket": "03",
      "axis": "craft",
      "file": "GeminiTranslationProviderTest.kt:9-25",
      "finding": "Positive parser test should assert every mapped field"
    },
    {
      "ticket": "03",
      "axis": "craft",
      "file": "HomeStateTest.kt:8-15",
      "finding": "Retry policy tests cover only two of six error categories"
    },
    {
      "ticket": "04",
      "axis": "spec",
      "file": "SettingsScreen.kt:60-62",
      "finding": "Choice dialog options should remain reachable at large font scale by scrolling"
    },
    {
      "ticket": "04",
      "axis": "craft",
      "file": "SettingsRepositoryTest.kt:20-25",
      "finding": "Defaults test should assert independent literal values instead of AppSettings production defaults"
    },
    {
      "ticket": "04",
      "axis": "craft",
      "file": "SettingsRepositoryTest.kt:20-49",
      "finding": "Repository tests do not cover SaveSettingsResult.Failure or coroutine cancellation propagation"
    },
    {
      "ticket": "04",
      "axis": "craft",
      "file": "PlaybackPlanTest.kt:50-58",
      "finding": "Shuffle permutation test should assert count or multiset, not only unique IDs"
    },
    {"ticket":"05","axis":"spec","file":"PlaybackService.kt:94","finding":"STATE_ENDED can finish while bounded prefetch is still preparing later events"},
    {"ticket":"05","axis":"spec","file":"PlaybackService.kt:99","finding":"Direct MediaSession transport can bypass audio-focus and stop lifecycle handlers"},
    {"ticket":"05","axis":"spec","file":"AudioPreparer.kt:37","finding":"Prepared in-flight batch files need protection before enqueue"},
    {"ticket":"05","axis":"spec","file":"PlaybackService.kt:182","finding":"Live phase must come from typed event semantics instead of text equality"},
    {"ticket":"05","axis":"craft","file":"PlaybackQueueTest.kt:18","finding":"Failed-synthesis test needs a post-failure sentinel and explicit returned-problem assertion"},
    {"ticket":"05","axis":"craft","file":"AudioPreparer.kt:96","finding":"Cancellation or thrown synth calls must clean callbacks and temporary files in finally"},
    {"ticket":"05","axis":"craft","file":"AudioPreparer.kt:105","finding":"Every prepared batch file must stay protected until player ownership"},
    {"ticket":"05","axis":"craft","file":"PlaybackService.kt:157","finding":"Consumed items should be removed so long sessions do not pin the full cache"},
    {"ticket":"05","axis":"craft","file":"PlaybackService.kt:89","finding":"All play/pause transitions need one complete audio-focus policy"},
    {"ticket":"05","axis":"craft","file":"PlaybackService.kt:126","finding":"All preparation exits must publish actionable state and settle foreground ownership"},
    {"ticket":"05","axis":"craft","file":"PlaybackService.kt:183","finding":"Prepared segments need explicit phase identity"},
    {"ticket":"05","axis":"craft","file":"MainActivity.kt:97","finding":"Player card visibility must consistently use session snapshot or explicitly update service state"},
    {"ticket":"05","axis":"craft","file":"PlaybackController.kt:35","finding":"Controller connections need timeout, cancellation and surfaced failure"},
    {"ticket":"05","axis":"device","file":"Android runtime","finding":"Screen-off playback, live TTS voices and notification/lockscreen sync require a connected device"}
  ],
  "reviewers": {
    "manifestSpec": "/root/w2_library_review",
    "craft": "/root/w2_craft_review"
  },
  "blind": null,
  "stitch": {
    "projectId": "13006564547979315790",
    "url": "https://stitch.withgoogle.com/projects/13006564547979315790",
    "status": "design-approved",
    "createdNew": true,
    "lastSubmittedBatch": "12 Detail/Edit/Playback: наявність підтверджена до паузи, дизайн погоджений користувачем.",
    "observedScreens": "28 актуальних макетів, включно з останніми 12 Detail/Edit/Playback; 4 початкові чернетки додатково."
  },
  "pause": {
    "requestedByUser": false,
    "at": "2026-09-22T10:55:00+02:00",
    "reason": "Wave 2 завершена; користувач наказав зупинитися перед Wave 3",
    "resumeRequires": "Повідомлення користувача про продовження",
    "resumedAt": "2026-09-22T11:07:01+02:00"
  },
  "designApproval": {
    "status": "approved",
    "at": "2026-09-15T07:29:43+02:00",
    "quote": "виглядає непогано... ок від мене",
    "scope": "Показані макети Stitch. Конкретний вибір A/B не вказаний; Software Design та початок розробки цим повідомленням не підтверджені.",
    "selectedConcept": "A — Resonance",
    "themes": [
      "light",
      "dark"
    ],
    "selectionQuote": "A — Resonance, світла й темна"
  },
  "implementationAuthorization": {
    "at": "2026-09-15T07:37:51+02:00",
    "quote": "го",
    "context": "Команда продовжити після апруву дизайну; не запитувати повторно дозвіл розробляти."
  },
  "translationDecision": {
    "mode": "online",
    "quote": "Онлайн-переклад; провайдера запропонуй",
    "proposedProvider": "Gemini via Firebase AI Logic",
    "providerApproved": true,
    "approvedAt": "2026-09-15T07:52:04+02:00",
    "approvalQuote": "го",
    "plan": "Spark; no billing",
    "model": "gemini-3.5-flash-lite",
    "modelReason": "2.5 Flash-Lite shutdown October 2026; 3.5 Flash-Lite Standard free tier supported without billing as of 2026-09-15"
  },
  "stopAfterWave": null,
  "firebase": {
    "projectId": "learn-everywhere",
    "plan": "Spark",
    "createdNew": true,
    "aiLogicStatus": "wizard-awaits-Gemini-Additional-Terms",
    "observedMessage": "By continuing, you agree to the Gemini API Additional Terms of Service and Additional usage policies",
    "checkedAt": "2026-09-15T23:21:27+02:00",
    "iamAuditStatus": "Google Cloud first-visit Terms of Service modal; no acceptance submitted",
    "iamAuditUrl": "https://console.cloud.google.com/iam-admin/iam?project=learn-everywhere",
    "androidAppPackage": "com.learneverywhere.app",
    "androidAppStatus": "registered",
    "configStatus": "download-clicked-but-no-new-file-in-workspace; old Downloads file discarded without use",
    "billingStatus": "Spark; Gemini Developer API no-cost selected; no Blaze or billing enabled"
  },
  "ticketReviews": {
    "01": {
      "manifestSpecReviewer": "/root/t01_manifest_review",
      "manifestSpecVerdict": "passed-after-repair",
      "craftReviewer": "/root/t01_craft_review",
      "craftVerdict": "passed-after-repair",
      "repairItems": [
        "R41 mainLanguage passed to Library",
        "localized auto dictionary names",
        "repair missing default among existing dictionaries",
        "Library beyond 100 words",
        "honest playback shell"
      ],
      "tests": "assembleDebug, testDebugUnitTest (6 passed), assembleDebugAndroidTest; no attached device for instrumentation"
    },
    "02": {
      "manifestSpecReviewer": "/root/w2_library_review",
      "manifestSpecVerdict": "clean-after-repair",
      "craftReviewer": "/root/w2_craft_review",
      "craftVerdict": "nonblocking-findings-recorded",
      "repairItems": [
        "off-main JSON export",
        "rotation-safe pending SAF payload",
        "clickable non-duplicated default dictionary",
        "localized import suffix",
        "accessible selected semantics",
        "repeated import conflict coverage"
      ],
      "tests": "Full debug/release/unit/AndroidTest APK build passed; 7 targeted tests passed; device SAF unverified"
    },
    "03": {
      "manifestSpecReviewer": "/root/w2_library_review",
      "manifestSpecVerdict": "manifest-clean; nonblocking-spec-findings-recorded",
      "craftReviewer": "/root/w2_craft_review",
      "craftVerdict": "nonblocking-findings-recorded",
      "repairItems": [
        "effective Ukrainian translation target",
        "source-candidate contradiction chooser",
        "repository failure recovery and UI finally",
        "strict Gemini JSON types",
        "rotation-safe review warnings",
        "speech terminal state and listening wave",
        "retry policy split"
      ],
      "tests": "Full debug/release/unit/AndroidTest APK build passed; 15 targeted tests passed; live Gemini and device speech unverified"
    },
    "04": {
      "manifestSpecReviewer": "/root/w2_library_review",
      "manifestSpecVerdict": "passed-after-repair; nonblocking large-font dialog finding recorded",
      "craftReviewer": "/root/w2_craft_review",
      "craftVerdict": "nonblocking-findings-recorded",
      "repairItems": [
        "atomic settings transforms from latest DataStore snapshot",
        "concurrent update and recreation regression test"
      ],
      "tests": "Full debug/release/unit/AndroidTest APK build passed; 37 unit tests total, 7 Ticket04 tests passed; device locale/large-font smoke unverified",
      "commit": "f7850bb",
      "pushedAt": "2026-09-22T11:42:53+02:00"
    }
  },
  "wave1Commit": "3224f03",
  "wave1PushedAt": "2026-09-15T12:49:06+02:00",
  "wave2SharedSeam": {
    "owner": "/root/t01_foundation",
    "status": "done",
    "items": [
      "RECORD_AUDIO + queries",
      "App Check debug/release bootstrap",
      "ImportDictionary.isDefault atomic default handling"
    ],
    "resumedAt": "2026-09-15T17:57:39+02:00",
    "finishedAt": "2026-09-15T18:05:22+02:00",
    "reviewer": "/root/w2_seam_review",
    "review": "passed",
    "tests": "assembleDebug, assembleRelease, seven repository tests passed; live Firebase unconfigured",
    "commit": "a8701d3",
    "pushedAt": "2026-09-15T23:13:08+02:00"
  },
  "wave2ExportSnapshot": {
    "owner": "/root/t01_foundation",
    "status": "done",
    "resumedAt": "2026-09-15T23:13:08+02:00",
    "purpose": "One Room read transaction for one/all JSON export",
    "finishedAt": "2026-09-15T23:21:27+02:00",
    "tests": "Eight repository tests passed; full Wave2 build 18 unit tests, debug/release/test APK passed",
    "review": "ticket02 Manifest+Spec reviewer confirmed coherent one-transaction export"
  }
}
