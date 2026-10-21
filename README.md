# DayQuest — Offline-First Daily Task & Quest Tracker (Android)

DayQuest is an offline-first daily task and personal-progress tracker with restrained RPG gamification, built for native Android using **Kotlin 2.2.10**, **Jetpack Compose (Material 3 Expressive)**, and **Room SQLite 2.7.0**.

## Runtime & Stack Disclosure
- **AI Studio Target Runtime:** Native Android (AGP 9.1.1, Kotlin 2.2.10, compileSdk 36, minSdk 24, Jetpack Compose BOM 2024.09.00, Room 2.7.0 with KSP).
- **Architecture Mapping:**
  - Local Source of Truth: **Room SQLite Database** (`dayquest.db`, 13 tables) replacing Drift/SQLite.
  - Reactive State: **Kotlin Coroutines + `StateFlow` + `ViewModel`** replacing Riverpod.
  - Navigation: **5 Primary Destinations** (`Today · Stats · Quests/Badges · League · Profile/Settings`) with `BackHandler` support.
  - On-Device ML: **`OnDeviceTaskClassifier`** (multilingual character 3-gram + token TF-IDF + weighted kNN personalization layer) loaded from bundled `assets/ml/seed.csv` (1,650 rows).

## Key Features Across All 7 Phases

1. **Phase 1 — Foundation, Theme, Today & Task CRUD**
   - Default Indigo (`#5B5BD6`) accent + 7 optional accents; Light, Dark Charcoal, and true-black AMOLED modes.
   - 11 Fixed Categories (`Study`, `Work/Career`, `Health & Fitness`, `Skill/Coding`, `Finance`, `Family & Social`, `Mindfulness/Self-care`, `Home & Chores`, `Creativity/Hobby`, `Rest/Entertainment`, `Other`).
   - Mandatory **By Time** validation: selecting `By Time` immediately opens the Material 3 TimePicker and blocks saving until a due time is set.

2. **Phase 2 — XP Ledger, Anti-Cheat, Focus Timer, Daily Quests & Routines**
   - Exact XP formula: `base = 10 + 0.5 * minutes`, difficulty (`1.0x–3.0x`), timing (`1.2x` early, `1.0x` on-time, `0.7x` late <=60m, `0.5x` >60m), streak (`1 + min(streak, 30) * 0.01`), focus timer bonus (`+10%`), and 5-second creation-to-completion anti-cheat (`25%` factor, min 5 XP).
   - Duplicate normalized titles on the same local date earn `0 XP`, and daily task XP awards are capped at `30/day`.
   - Subtasks pay up to `20%` per item, capped at `80%` pre-completion.
   - Built-in Pomodoro Focus Timer (`15m`/`25m`/`45m`) and 3 deterministic Daily Mini-Quests (+100 XP Grand Bonus).

3. **Phase 3 — On-Device Multilingual ML & Personalization**
   - Uses `assets/ml/seed.csv` (1,650 rows across 550 semantic families in English, Devanagari Hindi, and Hinglish) and `assets/ml/time_parser_cases.csv` (51 cases).
   - Evaluates with zero translation leakage by grouping on `family_id`.
   - Confidence UI: `>= 70%` auto-assigns, `40%–69.9%` highlights best guess + 3 tappable alternatives, `< 40%` falls back to confirmation. User overrides carry `3x` weight in local kNN memory.

4. **Phase 4 — Analytics, Charts, Heatmap & Weekly Report**
   - Interactive 7-day XP line chart with tappable points that drill into that date's task list.
   - Category mastery progress bars, 28-day GitHub-style intensity heatmap, sample-size guarded insights, and shareable Weekly Report.

5. **Phase 5 — 69-Badge Data-Driven Catalogue (`assets/badges.json`)**
   - 69 unique badges across Streaks, Volume, Category Mastery (33 badges: Levels 5/10/20 × 11 categories), Timing, Focus, Perfect Days, League, and 6 Secret badges.

6. **Phase 6 — Leagues, 30-Member Cohort, Friends 1v1 & AES-256-GCM Backup**
   - 7 League tiers (`Bronze` to `Legend`) on canonical `Asia/Kolkata` Monday 00:00 cycle.
   - 30-member cohort where all 29 simulated members are explicitly labelled `Bot`.
   - Client-side `AES-256-GCM` authenticated backup with `PBKDF2WithHmacSHA256` key derivation and `SHA-256` plaintext checksum verification.

7. **Phase 7 — Coin Store, Wired Settings, Bilingual (`en`/`hi`) & Android Home Widget**
   - Atomic Coin Store purchases (`Streak Freeze` 100 coins max 2, Avatar Frames), English/Hindi UI toggle, CSV export, typed `DELETE` data reset, and native `DayQuestWidgetProvider` home-screen widget.

## Verification & Test Commands

```bash
# Run all local unit and Robolectric tests
gradle :app:testDebugUnitTest

# Assemble debug APK
gradle :app:assembleDebug
```
