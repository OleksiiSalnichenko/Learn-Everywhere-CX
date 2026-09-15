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
  "updatedAt": "2026-09-15T18:05:22+02:00",
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
      "status": "active",
      "startedAt": "2026-09-15T07:54:25+02:00",
      "note": "Wave 2: 02 Library/JSON і 03 Intake/Gemini паралельно"
    },
    {
      "id": "review",
      "status": "pending"
    },
    {
      "id": "final",
      "status": "pending"
    }
  ],
  "requirements": {
    "total": 65,
    "done": 21,
    "inTicket": 44,
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
      "status": "in-progress",
      "retries": 0,
      "repairs": 0,
      "handoffs": 0,
      "file": "02-library-json.md",
      "startedAt": "2026-09-15T12:49:06+02:00",
      "executor": "/root/t02_library_json"
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
      "status": "in-progress",
      "retries": 0,
      "repairs": 0,
      "handoffs": 0,
      "file": "03-intake-gemini.md",
      "startedAt": "2026-09-15T12:49:06+02:00",
      "executor": "/root/t03_intake_gemini"
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
      "status": "pending",
      "retries": 0,
      "repairs": 0,
      "handoffs": 0,
      "file": "04-settings-plan.md"
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
      "status": "pending",
      "retries": 0,
      "repairs": 0,
      "handoffs": 0,
      "file": "05-playback.md"
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
  "concerns": [],
  "reviewers": {
    "manifestSpec": "/root/spec_coverage",
    "craft": null
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
    "at": "2026-09-15T07:17:46+02:00",
    "reason": "Пауза після опрацювання брифу; чернетка готова, питання перекладу ще відкрите",
    "resumeRequires": "Повідомлення користувача про продовження",
    "resumedAt": "2026-09-15T07:37:51+02:00"
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
  "stopAfterWave": 2,
  "firebase": {
    "projectId": "learn-everywhere",
    "plan": "Spark",
    "createdNew": true,
    "aiLogicStatus": "console-reports-missing-permissions",
    "observedMessage": "To manage Firebase AI Logic, ask a project owner for the necessary permissions",
    "checkedAt": "2026-09-15T12:35:13+02:00",
    "iamAuditStatus": "Google Cloud first-visit Terms of Service modal; no acceptance submitted",
    "iamAuditUrl": "https://console.cloud.google.com/iam-admin/iam?project=learn-everywhere"
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
    "tests": "assembleDebug, assembleRelease, seven repository tests passed; live Firebase unconfigured"
  }
}
