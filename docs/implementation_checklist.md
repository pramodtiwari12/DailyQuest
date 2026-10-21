# DayQuest Implementation Checklist & Full 7-Phase Completion Report

Target Environment: Native Android (Kotlin 2.2.10 + Jetpack Compose + Room SQLite 2.7.0 + Coroutines/StateFlow)

## Phase Status Tracker (All 7 Phases Completed & Verified)

- [x] **Phase 1: Foundation, Theme, Room DB, Today Screen, Task CRUD, Schedules, Quick-Add, By Time Validation**
  - [x] DayQuest Material 3 Expressive minimalist theme: Indigo `#5B5BD6` default, 8 accent choices, Light/Dark/AMOLED support.
  - [x] 11 Fixed category tokens with color and iconography mapping.
  - [x] Room SQLite Database (`tasks`, `subtasks`, `xp_logs`, `user_stats`) with foreign keys and indexes.
  - [x] Today screen with grouped sections: `By Time` (sorted by due time), `Anytime`, `Completed`, `Overdue` banner.
  - [x] Quick-Add bar with real-time natural language parsing preview (schedule, duration, category guess, XP preview).
  - [x] Mandatory By Time picker: immediate picker launch upon selection, save blocked if time missing, inline validation error.
  - [x] Adaptive custom launcher icon with DayQuest emblem across all density folders.

- [x] **Phase 2: Restrained Gamification, XP Ledger, Levels, Coins, Streaks, Focus Timer, Daily Quests & Routines**
  - [x] Append-only XP ledger (`xp_logs`) with unique idempotency keys and transparent JSON breakdown.
  - [x] Duplicate normalized title detection on same local date (0 XP & 0 coins for duplicate completions).
  - [x] Daily cap of 30 task XP awards per local day.
  - [x] Subtask progression payouts (max 20% per subtask, capped at 80% before parent completion, remainder on parent completion).
  - [x] Daily bonuses (+25 XP before 09:00 AM, +50 XP for completing all planned tasks when >=3, +100 XP for finishing all 3 Daily Quests).
  - [x] Built-in Pomodoro Focus Timer (`TaskDetailSheet` + `FocusSessionEntity`) with timestamp-backed elapsed tracking and +10% Focus Bonus.
  - [x] 3 Deterministic Daily Mini-Quests per date + 1-Tap Routine Templates (`Morning Launchpad`, `Deep Study Block`, `Evening Fitness` + custom routine creator).
  - [x] Streak Freezes (`100 Coins`, max 2 stored) and Level-Up celebration modal.

- [x] **Phase 3: On-Device ML Categorization & Personalization**
  - [x] Bundled `assets/ml/seed.csv` (1,650 rows, 550 semantic families across English, Devanagari Hindi, Hinglish), `assets/ml/time_parser_cases.csv` (51 rows), and `assets/ml/dataset_audit.json`.
  - [x] `OnDeviceTaskClassifier` with Unicode/Hinglish/Devanagari normalization preserving technical tokens (`C++`, `C#`, `SQL`, `ML`, `DSA`, `10k`, `6pm`).
  - [x] Confidence band UI in Quick-Add: `>= 0.70` (`Auto · Category XX%`), `0.40 - 0.699` (best guess + 3 tappable alternatives), `< 0.40` (fallback confirmation).
  - [x] Local personalization store (`ml_corrections`) with `3x` weight on user overrides and live evaluation diagnostics (`family_id` zero-leakage split).

- [x] **Phase 4: Stats, Charts, Activity Heatmap & Weekly Reports**
  - [x] Range control (`Day`, `Week`, `Month`, `Year`, `All`) and Category filter (`All` or any of the 11 categories).
  - [x] Interactive 7-day XP trend line chart with tappable date tooltips and 1-tap drill-down into that date's task list.
  - [x] Per-category mastery progress bars & XP share breakdown.
  - [x] 28-day GitHub-style activity intensity heatmap with date drill-down.
  - [x] Sample-size guarded productivity insights and shareable Weekly Report dialog.

- [x] **Phase 5: Badges & Achievements Catalogue (`assets/badges.json`)**
  - [x] Data-driven 69-badge catalogue in `assets/badges.json` and `BadgeCatalogue.kt` covering Streaks (7), Volume (6), Category Mastery (33 across 11 categories × Levels 5/10/20), Timing (4), Focus (3), Perfect Days (3), League (7), and Secret/Comeback (6).
  - [x] Search, Rarity filter (`Common`, `Rare`, `Epic`, `Legendary`), Group filter, locked secret hints, and Badge Share intent.

- [x] **Phase 6: Leagues, 30-Member Cohort, Friends 1v1 Challenges & AES-256-GCM Encrypted Backup**
  - [x] 7 League tiers (`Bronze -> Silver -> Gold -> Platinum -> Diamond -> Master -> Legend`) on canonical `Asia/Kolkata` Monday 00:00 cycle.
  - [x] 30-member cohort leaderboard where all 29 simulated members are explicitly labelled `Bot` (`Name · Bot`), with Top 7 Promotion and Bottom 5 Demotion zones.
  - [x] Friends & Weekly 1v1 Challenge system with invite codes and +50 Coin victory claims.
  - [x] Client-side `AES-256-GCM` authenticated backup/restore (`BackupCryptoManager.kt`) with `PBKDF2WithHmacSHA256` passphrase derivation, random 12-byte IV, 16-byte salt, and `SHA-256` plaintext integrity verification.

- [x] **Phase 7: Profile, Coin Store, Wired Settings, Localization (`en`, `hi`), Android Home Widget & Tests**
  - [x] Coin Store with atomic purchases for Streak Freezes and Avatar Frames persisted in Room.
  - [x] Full wired Settings (Light/Dark/AMOLED, 8 accents, font scale 80–130%, reduce motion, penalties toggle, private league mode, English/Hindi switch, CSV export, typed `DELETE` data reset).
  - [x] Native Android Home-Screen AppWidget (`DayQuestWidgetProvider`) registered in `AndroidManifest.xml`.
