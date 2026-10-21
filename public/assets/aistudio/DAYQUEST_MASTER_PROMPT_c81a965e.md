# DAYQUEST — Production Build Master Prompt

**Role:** Act as a principal Flutter engineer, mobile product designer, applied on-device ML engineer, data engineer, QA lead, and Firebase security engineer. Build **DayQuest**, a production-ready, offline-first daily task and personal-progress tracker with carefully restrained gamification. Do not ask clarifying questions. Use the defaults below, document any unavoidable constraint, and implement rather than merely describe.

**Primary target:** Android first, iOS compatible. **Build policy:** work in the repository, run commands, create real files, run tests, fix failures, and keep the app runnable after each phase. Do not return only snippets or a plan. Do not claim a phase passes unless you ran its acceptance checks and provide the command/output summary. Never replace a requested feature with a TODO, fake button, hard-coded success, placeholder chart, fake model, or simulated production service. Simulated league members are the one explicit exception and must be labelled “Bot”.

---

## 0. Product definition and research-informed defaults

DayQuest should feel like a premium, calm, fast productivity app with an optional RPG progression layer—not a noisy game dashboard. Its core promise is: **capture a task quickly, complete it, understand your progress, and build sustainable routines without requiring an account or network**.

### UI direction (current Android-first recommendation)

Use **Material 3 Expressive principles implemented in Flutter Material 3**, restrained to a minimalist productivity interface: adaptive components, expressive but controlled typography, responsive surfaces, personalization, natural motion, and clear state feedback. Apply these principles selectively; do not use oversized hero cards, gratuitous gradients, neon everywhere, or a separate card for every number. The default screen must prioritize the task list and quick-add action.

- Default accent: Indigo `#5B5BD6`; use one neutral surface system, category colors only where they encode meaning, and the accent for primary actions/progress.
- Light: warm/neutral white surfaces, subtle borders. Dark: deep charcoal. AMOLED: true black option. System follows OS theme. Do not assume dynamic system color is the default; offer it only as an optional theme mode.
- Cards: 12 dp radius, thin border, no heavy shadows. Use elevation only to distinguish transient/modal elements. No gradients except a very subtle XP progress glow.
- Information hierarchy: small title, clear section labels, compact task rows, tabular numerals for XP/time, excellent contrast. Preserve the specified compact typography but never truncate critical accessible text just to fit more content.
- Motion should communicate hierarchy and reward; avoid continuous motion except a subtle, pausable streak pulse. Respect Reduce Motion globally.
- Use responsive/adaptive navigation: bottom navigation on compact phones, navigation rail on wider layouts, and sensible list/detail layouts on tablet/foldables. Keep phone UI the priority.
- Use a fixed category icon/color mapping, not random colors. Icons should be consistent line icons (Lucide/Phosphor-like package with a maintained license); have a built-in Material icon fallback where required.
- Use Inter for Latin text and Noto Sans Devanagari for Hindi. Bundle fonts with their licenses and configure font fallback explicitly.
- Accessibility is part of design, not a final polish: semantic labels, screen-reader announcements for XP/badge/level events, minimum recommended tap targets (48 dp Android, 44 pt iOS), contrast checks, keyboard support where relevant, dynamic type/system scaling, and a usable layout at large accessibility font sizes. “9 visible tasks” is a compact-mode target at 360×640 and default font scale; do not violate accessibility or clip controls to force it.

### Research snapshot as of 20 October 2026

- Flutter documentation lists **Flutter 3.47** as the August 2026 stable release. The current package-page snapshot also surfaced `flutter_riverpod 3.4.3`, `go_router 18.1.0`, `drift 2.35.2`, and `tflite_flutter 0.12.1`; treat those only as research-time observations, confirm all SDK/platform constraints before pinning, and lock the compatible versions. At implementation time, re-check official stable release notes and pub.dev. Do not use beta/dev packages in production without documenting why. <https://docs.flutter.dev/release/whats-new> <https://pub.dev/packages/flutter_riverpod> <https://pub.dev/packages/go_router> <https://pub.dev/packages/drift> <https://pub.dev/packages/tflite_flutter>
- Google describes Material 3 Expressive as emphasizing personalization, glanceable information, responsive components, and natural motion. Use the principles, not every decorative treatment. <https://blog.google/products-and-platforms/platforms/android/material-3-expressive-android-wearos-launch/>
- Accessibility testing must include tap-target, semantics, and text-contrast audits; Flutter documents test guidelines for these checks. <https://docs.flutter.dev/ui/accessibility/accessibility-testing> <https://docs.flutter.dev/ui/accessibility/ui-design-and-styling>
- `tflite_flutter` supports TFLite/LiteRT inference, delegates, and isolate-based execution, but it is not itself a pretrained model. Bundle the actual model/tokenizer/labels and test them. <https://pub.dev/packages/tflite_flutter>
- Android exact alarms are not automatically available in many Android 14+ install scenarios. Request special alarm access only when needed, handle denial/revocation, and gracefully fall back to inexact reminders. <https://developer.android.com/about/versions/14/changes/schedule-exact-alarms> <https://pub.dev/packages/flutter_local_notifications>
- Firestore has offline persistence, but DayQuest's **Drift database remains the local source of truth**. Only aggregate league data and explicitly opted-in, client-encrypted backup may sync. <https://firebase.google.com/docs/firestore/manage-data/enable-offline>
- Candidate multilingual encoders must be evaluated for size, license, tokenizer compatibility, conversion, latency, and real Hindi/Hinglish quality. `multilingual-e5-small` is a multilingual research baseline, not a drop-in TFLite asset. Never treat the model card alone as proof of mobile readiness. <https://huggingface.co/intfloat/multilingual-e5-small>

---

## 1. Fixed technology stack

- Flutter: latest stable at build time; Dart stable. Android first, iOS compatible.
- State: Riverpod (stable). Navigation: `go_router`.
- Offline-first DB/source of truth: Drift + SQLite, reactive queries, explicit migrations, indexes, foreign keys where practical, transactional writes, and background/isolate DB access where supported.
- Charts: `fl_chart`; motion: `flutter_animate`, Flutter implicit/explicit animations, and Lottie for badge/level celebrations only when the actual licensed animation asset is bundled. If an external Lottie asset cannot be licensed/bundled, implement a real local particle/confetti animation; do not leave a missing asset or static placeholder.
- Notifications: `flutter_local_notifications`, timezone-aware scheduling, boot rescheduling, notification actions, and permission-state handling.
- On-device ML: `tflite_flutter`/LiteRT, compact multilingual encoder + classifier heads when a genuinely compatible model is available; local keyword/character n-gram baseline as a tested fallback. Model inference and tokenization must run off the UI isolate.
- Online features only: Firebase Auth (anonymous, optional Google linking/sign-in), Firestore, Cloud Functions, App Check, and explicit opt-in encrypted cloud backup. All task CRUD, task text, timer, XP, categories, statistics, quests, badges, and settings work offline.
- Localization: Flutter ARB, English and Hindi (`en`, `hi`), no hard-coded user-facing strings.
- Android widgets: implement real Android home-screen widgets through a maintained Flutter/native bridge with documented platform-specific code. iOS widgets, if included, must use a WidgetKit extension; do not pretend a Flutter screen is a widget.

At build start, check current stable versions on official release pages/pub.dev, resolve compatibility, run `flutter pub get`, commit/ship `pubspec.lock`, and record the tested versions in README. Do not blindly paste stale versions from this prompt.

## 2. Architecture and repository structure

Use feature-first boundaries and a clean separation between presentation, application/use cases, domain rules, and infrastructure. Required starting structure (additional files are expected):

```text
lib/
  app/                   # app bootstrap, router, localization, lifecycle
  core/
    db/                  # Drift database, tables, DAO, migrations
    ml/                  # model interface, tokenizer, inference, fallback
    notifications/       # permissions, schedule/reconcile/cancel
    time/                # timezone, day boundary, natural-language parser
    theme/               # tokens, theme modes, scale, density
    security/            # encryption, biometric/PIN lock
    export/              # JSON/CSV backup and share-image pipeline
    sync/                # outbox, sync service, conflict/retry policy
    widgets/             # shared controls and accessibility utilities
  features/
    onboarding/ tasks/ xp/ badges/ quests/ leagues/ stats/ profile/ settings/
assets/
  fonts/
  lottie/
  badges.json
  ml/seed.csv
  ml/time_parser_cases.csv
  ml/model/              # real .tflite model, tokenizer/vocab, labels, metadata
  l10n/                  # app_en.arb, app_hi.arb
functions/               # Firebase Cloud Functions, rules/indexes
android/ ios/
test/ integration_test/ docs/
```

Use repository interfaces so task/XP rules can be unit tested without Flutter widgets or Firebase. Prefer immutable domain entities and explicit result/error types. Riverpod providers must not hide business logic inside widget build methods. Use UTC instants for timestamps, a stored IANA timezone for wall-clock interpretation, and local date keys for planning/history.

### Offline/network boundary

- Drift is authoritative for all local state. UI never waits for a network call to create/edit/complete a task.
- Use an idempotent outbox for league XP events and sync status; batch/debounce sync by 30 seconds when online. Retry with bounded exponential backoff and jitter. Preserve queued events through process death.
- Do not upload task titles, notes, subtasks, embeddings, or ML corrections for leagues. Only send the minimum pseudonymous user ID, weekly XP event/aggregate, date bucket, league metadata, and anti-cheat fields.
- Cloud backup is opt-in and is separate from league sync. If enabled, encrypt the complete export client-side before upload (AES-GCM or a well-supported authenticated encryption construction, unique nonce, versioned envelope, key derivation via a user-controlled passphrase or documented cross-device recovery key). Never store plaintext task data or the encryption key in Firestore. Explain recovery implications. Task text is never sent in plaintext.
- Add Firebase App Check, restrictive Firestore/Storage/Functions rules, callable/function input validation, server timestamps, rate limiting, and secrets in server configuration. Never put admin credentials in the client.
- Add a visible connection/sync status. Offline League displays cached ranking and “Offline · last synced …”; the rest of the app does not present offline as an error.

## 3. Design system and motion requirements

- Base screen horizontal padding 14 dp; 4 dp spacing grid; compact card padding 10 dp; default radius 12 dp; 1 dp hairline border; avoid heavy shadows. Font defaults: Title 16 sp semibold, section 13 sp semibold, body 12 sp, caption 10.5 sp, XP/numbers 14 sp tabular. Base in-app font-size scale is 90%, adjustable 80–130%, but all controls and system text scaling must still be tested at larger system scales.
- 8 accent choices: Indigo `#5B5BD6` default, Blue `#2673D9`, Teal `#0F9D91`, Green `#2E9D62`, Amber `#C58A13`, Orange `#D96B27`, Rose `#D94F70`, Violet `#805AD5`. Verify contrast in both themes; alter foreground/surface pairings as needed.
- Category tokens (fixed): Study blue; Work/Career slate; Health & Fitness green; Skill/Coding violet; Finance gold; Family & Social pink; Mindfulness/Self-care teal; Home & Chores brown; Creativity/Hobby orange; Rest/Entertainment grey; Other neutral. Icons must be consistent and accessible; category meaning cannot rely on color alone.
- Route transitions: shared-axis horizontal slide + fade, 280 ms, `easeOutCubic`. Task add: slide down/fade in 220 ms; list reflow 200 ms spring. Complete: checkbox check stroke 180 ms, title strike 150 ms, row fades to 60% opacity, category-colored floating `+XP` rises 24 dp and fades over 700 ms, XP bar fills in 500 ms `easeOutQuart`; coin count-up runs at the same time without layout jitter. Swipe right completes. Swipe left opens a clear action choice for delete/postpone with a 4-second undo snackbar; avoid accidental destructive gestures.
- Level-up overlay: confetti 1.6 s, level number scales 0.6→1.0 with elastic curve, tap/accessible action to dismiss. Queue multiple level-up events rather than stacking overlays.
- Badge unlock: bottom sheet slides up; 3D Y-axis flip 500 ms with shimmer. League promotion/demotion: directional arrow plus rank count. Bar charts grow from zero 600 ms, 40 ms stagger; line charts draw left-to-right over 800 ms. Streak flame gentle 2 s 1.0→1.08 loop. Loading skeleton shimmer; no infinite shimmer after loading resolves.
- Reduce motion: disable translations, scaling, looping motion, shimmer, and confetti; use brief fades only. Sound and haptics are independently configurable. Haptic events only on meaningful completion/level/badge transitions and must not fire twice on duplicate streams.
- Every destructive operation supports undo where it can be reversed. Destructive snackbars are accessible to screen readers.

## 4. Core tasks and planning

### Task fields and persistence

Required title max 120 Unicode grapheme clusters (not just code units); optional notes; category ID; source auto/user; ML confidence and model version; difficulty Easy/Medium/Hard/Epic; estimated minutes; actual minutes; schedule type; due/start/end timestamps; local date key; repeat rule; priority; pending/done/missed status; created/completed/updated timestamps; XP awarded and breakdown JSON; whether timer was used; occurrence/series ID; notification IDs/version. Add a task-series/occurrence strategy so recurring tasks create separate trackable occurrences without duplicating or overwriting history. Keep IDs stable and recurrence idempotent.

### Schedule types

1. **Anytime Today:** no due time; task date still exists.
2. **By Time:** selecting this immediately opens and shows the time picker. Cannot save while time is missing. Store a local wall time plus resolved instant/timezone. Show e.g. `by 6:30 PM` on the row's right. Schedule configurable reminders 15 minutes before (default, selectable 5/10/15/30) and at due time.
3. **At Time Range:** choose start and end in the same local date; end must be after start. Show the range. Use `endAt` as deadline for overdue/XP semantics.

Default date is today; future planning is supported. Repeating tasks: none/daily/weekdays/weekly (weekday selector)/monthly; handle month-end dates deterministically, skipped occurrences, app restarts, timezone changes, and DST. Repeating task edits must ask internally through the UI whether to edit this occurrence or the whole series; never modify past completed occurrences.

### Add/edit, list, swipe, timer

- Add/Edit uses a compact bottom sheet with basic fields visible without scrolling on a 360×640 viewport where feasible; scroll only the advanced section. By Time picker opens immediately on switching the segmented control.
- Today screen groups rows as `By Time` (sorted by deadline), `Anytime`, `Completed`; missed items have a clear section/banner. Compact rows show checkbox, title, category dot/icon, due time/range, priority, XP chip. Overdue rows are amber but maintain accessible text/icon status. Avoid color-only meaning.
- Quick-add at bottom: typing instantly shows ML category and XP preview; Enter adds when valid. Support parsing explicit intent such as `gym by 6pm`, `study physics 2 hours`, `shaam 6 baje tak`, `2 ghante`. Parse temporal/duration/routine tokens with a deterministic local parser **before** ML categorization. Never turn an ambiguous `by 6` into a silently guessed time; apply a documented locale/current-time policy and surface a confirmation chip when AM/PM is ambiguous. Preserve the original title after extracting tokens, unless the user chooses to clean it.
- Difficulty/duration chips: 15/30/45/60/90/120 minutes. AI suggestions never overwrite a user edit silently.
- Subtasks are an editable checklist. Their progression XP is paid exactly once and is capped by the task's final XP; see XP rules below. Do not pay again when a task is completed or restored from undo.
- Optional built-in focus timer: Pomodoro defaults 25/5; session durations editable. Timer survives app backgrounding/process death using stored start/end instants and lifecycle recovery; elapsed time is calculated using timestamps, not a fragile in-memory counter. Pause/resume/reset is explicit. If used to complete a task, measured focused duration (rounded to documented minutes policy) replaces estimate for XP.
- Repeated duplicate titles on the same local task date must be detected after Unicode normalization/whitespace folding; allow the user to keep the duplicate, but apply anti-cheat XP rule below.
- At configured day reset (default 00:00, optional 04:00), reconcile overdue/incomplete tasks. At 23:59 local day, incomplete today's tasks move to Missed if auto-move is enabled. Respect custom day reset rather than hard-coding midnight. A missed task can be rescheduled to tomorrow with one tap; never silently lose history.
- Search/filter by title/notes locally, category, difficulty, status, priority, date range, and schedule type. Keep all content local.

### Notifications and Android OS behavior

- Request notifications contextually, with a clear onboarding explanation and a Settings route to OS permissions. Exact alarm permission is not guaranteed on Android 14+. Check permission state before scheduling, respond to revocation, explain the tradeoff, and fall back to inexact reminders when exact alarms are unavailable. Never claim a reminder will be exact if permission was denied.
- Reconcile pending notifications after task edit/delete/complete, time zone change, reboot, app update, and day-boundary changes. Avoid duplicate notifications using deterministic IDs and a notification-version key. If the OS limits pending alarms, schedule the nearest eligible events and reconcile later.
- Morning planning reminder default 08:00; evening review reminder default 21:30. All notification types have toggles and quiet hours. Weekly report notification Sunday 20:00 in the selected display timezone; weekly league cycle is canonical IST as specified below.

## 5. On-device ML: real artifact, measurable performance, private by default

### ML responsibilities

1. Normalize Unicode, whitespace, punctuation, case, common Hinglish spellings, and Devanagari/Romanized variants without destroying the original task title. Preserve technical tokens (`C++`, `C#`, `SQL`, `ML`, `DSA`, `10k`, `6pm`). Keep a test suite for transliteration collisions.
2. Use a compact multilingual sentence/text encoder and a small multi-head classifier for category (11 classes), difficulty (4 classes), and duration bucket/value. If a converted, licensed, sufficiently small pre-trained multilingual TFLite/LiteRT encoder does not meet size/latency/quality targets, train a compact supervised multilingual encoder/classifier that really runs on-device. Candidate models are candidates, not assumptions. Benchmark at least one compact transformer candidate against a lightweight character n-gram/TF-IDF or fastText-style baseline.
3. Bundle the actual `.tflite`/`.litert` artifact, tokenizer/vocabulary, fixed label map, model metadata, license/notices, model version, SHA-256 checksum, training scripts, evaluation output, and conversion script. The app must load the asset and run real inference. Do not download task text or model requests to a server at runtime. Inference must work in airplane mode.
4. ML input is task title only by default; don't send title/notes/embeddings/corrections off-device. Schedule parsing is a separate deterministic extraction layer so the ML classifier cannot hallucinate due times.
5. If confidence ≥0.70: assign top class and label e.g. `Auto · Study 92%`. If confidence 0.40–0.699: show best guess highlighted plus three alternatives and ask for confirmation. If confidence <0.40: attempt local keyword/character baseline; otherwise assign `Other` and ask the user to pick. Calibrate confidence using held-out data; softmax probability must not be advertised as calibrated confidence without evaluation.
6. The local correction store records normalized local features/embedding, confirmed label, model version, timestamp, and user-corrected flag. Keep it device-only. User corrections count 3× for personalization. Maintain bounded kNN/local memory and perform safe on-device classifier-head updating or periodic personalized calibration after 20 corrections. If on-device gradient fine-tuning is infeasible in the selected runtime, use a real working weighted kNN/personalization layer and document the distinction; do not label kNN as neural fine-tuning.
7. Learn difficulty/duration corrections as separate labels using the same local learning protocol. Explicit user category is the ground truth. A user override must stay respected for the current task and can update future predictions.
8. Model accuracy in Settings uses a clearly defined measure (e.g. agreement with later user-confirmed/corrected labels over last 100 eligible predictions), shows sample size and confidence/limitations, and never counts unverified predictions as confirmed-correct. Show fallback usage and class-wise metrics in developer diagnostics.
9. Inference target: under 50 ms p95 on a documented mid-range Android reference device, or report measured results and optimize. Target compact quantized asset; report actual model size and cold/warm latency. Inference must run off the UI isolate and not block quick-add typing.

### Seed dataset and training quality gates

Use the included `assets/ml/seed.csv` (1,650 rows: 11 categories × 50 semantic task families × English/Hindi/Hinglish variants) as a **synthetic starter dataset**, not as proof of production accuracy. Use `assets/ml/time_parser_cases.csv` as parser acceptance data. `family_id` identifies translated versions of the same intent and must be used to keep all language variants of a family in the same train/validation/test split; otherwise evaluation leakage will make scores look falsely high.

- Validate label names, language counts, category balance, duplicate titles, nulls, duration bounds, and invalid rows before training. Produce `dataset_audit.json` with row counts, unique family counts, class/language balance, dedupe report, split hashes, and known quality limitations.
- Seed is balanced and template-generated; paraphrase breadth and real-user diversity are limited. Add a documented way to import additional reviewed, consented, privacy-safe examples. Never scrape private task lists or imply that synthetic data represents users. No user task text is included in cloud analytics.
- Augment with safe transformations: spelling variants, punctuation/case/noise, short/long task phrasings, code-mixed Hindi/Hinglish, Devanagari, and Romanized Hindi. Do not create augmentation that changes the actual label. Keep a manually reviewed “hard negatives” file for confusable classes (e.g. coding a budget spreadsheet vs personal finance; watching a coding tutorial vs entertainment; work email vs family message).
- Add an independently authored, manually reviewed evaluation set from different semantic families and phrasing templates. Do not use synthetic paraphrases of training rows as the only test set. Evaluate category macro-F1, per-class precision/recall, overall accuracy, abstention/fallback rate, calibration (ECE or reliability bins), and confusion matrix; measure Hindi and Hinglish separately. Set a target macro-F1 ≥0.85 on the reviewed test set before describing categorization as production-grade. If it misses target, disclose the metric and keep the low-confidence confirmation UX; do not fake a passing score.
- Difficulty/duration labels in the starter dataset are weak synthetic heuristics. Include `difficulty_duration_review.csv` or a report describing that limitation. Treat duration as a suggested bucket, never a factual prediction, and always let the user override it.
- Time parser has rule-based ambiguity behavior. Run every row in `time_parser_cases.csv` using a fixed test anchor date/timezone. Add fuzz and regression tests for Hindi/English/Hinglish time formats, locale (12/24 h), AM/PM ambiguity, tomorrow/day after tomorrow, weekday expressions, time ranges, duration-vs-due-time distinctions, and invalid times. An input like “in 20 minutes” must not silently become a 20-minute task duration or deadline without a confidence/intent decision.

## 6. XP, coins, levels, streaks, and anti-cheat — exact rules

Store every event in an append-only XP ledger with an idempotency key. Recalculations or undo actions must reverse/compensate prior ledger entries exactly once; never double-award due to stream rebuild, restart, retry, or sync.

### Task XP

`base = 10 + (durationMinutes × 0.5)`.

Difficulty multiplier: Easy 1.0, Medium 1.5, Hard 2.0, Epic 3.0.

Timing multiplier (unambiguous definitions):
- No deadline: 1.0.
- Early: completion is more than 5 minutes before `dueAt`/`endAt`: 1.2.
- On time: completion is from `dueAt - 5 minutes` through `dueAt` inclusive: 1.0.
- Late by 1 second–60 minutes: 0.7.
- More than 60 minutes late: 0.5.

Streak multiplier: `1 + min(currentStreakDays, 30) × 0.01` (maximum 1.30). Snapshot the streak multiplier at the completion event.

Focus bonus: 10% if the built-in timer/focus session was genuinely used for this task; otherwise 0%. Do not grant it just because timer was opened. `finalTaskXP = max(5, round(base × difficultyMultiplier × timingMultiplier × streakMultiplier × (1 + focusBonus)))`. Use a single documented rounding rule (`round-half-up` in the domain layer) shared by app and tests; do not rely on platform-specific rounding implicitly.

- If the timer is used, actual focused duration replaces estimated duration for XP, subject to reasonable safeguards (minimum 1 minute; preserve total timer segments; prevent negative/overflow/paused time from counting).
- Duplicate normalized titles on the same local task date: only the first can earn task XP. Duplicate tasks remain usable; later duplicate task XP/coins are 0.
- Maximum 30 task XP awards per local day. Additional tasks can be completed and tracked but earn 0 task XP/coins. This cap is independent of daily bonuses and badge rewards.
- Tasks completed within 5 seconds of creation earn 25% of their normal XP, rounded using the same rule; document whether this applies before/after the minimum-5 rule and implement consistently. Required default: apply the 25% anti-cheat factor after calculating XP and before the final minimum, so final award is `max(5, roundHalfUp(normalXP × 0.25))`, unless the duplicate/day cap sets it to 0.
- Coins from a task: `floor(awardedTaskXP / 10)`, after anti-cheat/caps; 0-XP tasks earn 0 coins. Explicit badge/store/friend-challenge rewards are separate ledger reasons.
- Daily bonuses: +50 XP for completing all planned tasks when there are at least 3 planned task occurrences; +25 XP for completing the first task before 09:00 local time; +100 XP for finishing that day's Daily Quest. Each can be awarded at most once per date; task/quest edits must reconcile the bonus ledger idempotently.
- Penalty: −10 XP for each missed By Time task when penalties are enabled. A day's net XP cannot fall below 0; penalties may offset positive XP from that same day only and must never claw back XP earned on previous days. No duplicate penalty for the same occurrence. Penalties can be disabled in Settings.
- Subtasks: prevent XP farming. On each completed subtask, pay at most 20% of the final task XP, with all pre-completion subtask payouts capped at 80% of that task’s final XP. On parent completion, award only the remaining XP to bring total task-related XP to the calculated task XP; if there are no subtasks, award all task XP on completion. More than four subtasks can improve progress but cannot increase total task XP. Undo/reopen must use compensating ledger entries.
- Daily bonuses/penalties/badges must have separate `reason` values and transparent details in the XP history/breakdown UI.

### Levels

Overall and category levels use the same curve, and total XP for category levels is based on task XP attributed to that category (not unrelated global bonuses). Define the boundary without off-by-one ambiguity: level 1 begins at 0 XP; the cumulative XP threshold to enter level `L` (`L ≥ 2`) is `roundHalfUp(100 × (L−1)^1.5)`. Therefore moving from level `n` to `n+1` requires `100 × n^1.5` total cumulative XP. Show progress between thresholds `T(L)` and `T(L+1)`, not merely a percentage of lifetime XP.

Titles: levels 1–4 Beginner, 5–9 Explorer, 10–19 Achiever, 20–29 Pro, 30–49 Master, 50+ Legend. Display level, XP earned toward next level, streak flame, and coins in a slim header XP bar. Prevent rapid UI jumps when one reward crosses multiple levels; queue celebrations one at a time.

### Streaks and freezes

- A streak day counts when at least one eligible task is completed according to the user's configured day boundary. Current streak rolls forward; longest streak is historical max.
- A streak freeze costs 100 coins, storage cap 2, and protects one missed day only. Spend/restore is ledgered atomically. No use of more than one freeze per missing day. Clarify whether a freeze preserves the streak without marking the day as active; it must not award task XP or count as a completed task.
- Test consecutive days, gaps, timezone/day-reset changes, leap days, daylight transitions, freeze use, freeze cap, and current-vs-longest streak.

## 7. Daily quests, templates, routines, smart suggestions

- Create 3 deterministic, achievable daily mini-quests from current settings and recent history (e.g. complete two Health tasks, finish one Hard task, focus 45 minutes). Persist quests by local date; never reroll on every app launch. Provide alternative quests if a category is disabled or unavailable.
- Quest completion updates reactively and awards +100 XP once for finishing all 3. Show per-quest progress and claim/reward state.
- Routines/templates: Morning, Study, Workout defaults; user can edit, create, reorder, and add a routine in one tap. Template insertion must assign separate occurrence IDs and never duplicate existing routine instances accidentally.
- Smart suggestions start after 14 days with enough history. Make suggestions explainable (`You often schedule this on weekday mornings`), optional, dismissible, based only on on-device trends, and not auto-add tasks without consent.

## 8. Badges / achievements

Use `assets/badges.json` as a data-driven catalogue with at least 40 unique badges, validated against a schema. Required coverage:
- Streaks: 3, 7, 14, 30, 60, 100, 365 days.
- Volume: first task, 10, 50, 100, 500, 1,000 completed tasks.
- Category mastery: levels 5/10/20 for all 11 categories (generate entries from one template; these must total correctly).
- Timing: 10/50 early/before-deadline completions; Early Bird (5 tasks before 08:00 across 7 days); Night Owl.
- Focus: 10/50/100 total timer hours.
- Perfect days: 1/7/30.
- Comeback after at least 7 days away.
- League: reach each league; promotion 3 consecutive weeks.
- At least 5 secret badges hidden until unlocked.

Each badge: id, name, description, icon identifier, group/category, rarity (Common/Rare/Epic/Legendary), condition DSL/version, reward XP, reward coins, current progress, target, unlocked timestamp, hidden flag, locked hint, and localization keys. Conditions are evaluated from real local/event data, not hard-coded counters. Avoid duplicate unlock/reward events. Badges screen: search/filter by rarity/category, grid with rings/progress (`7/10 tasks`), locked hints, detail sheet, and share image export using a real image-capture pipeline. Use an original, licensed icon set.

## 9. Online leagues, friends, and challenge

Leagues in order: Bronze → Silver → Gold → Platinum → Diamond → Master → Legend. Canonical league period is Monday 00:00 to Sunday 23:59 **Asia/Kolkata** for cohort fairness; show times in the user's local timezone while explaining the reset. The user's configured task-day boundary must not shift the shared league reset. Weekly scores reset idempotently server-side.

- Cohort size 30. If there are fewer real users, add clearly labelled simulated `Bot` entries. Deterministic bot scores should be bounded/reproducible and must never be presented as real users.
- Rank by validated weekly XP, stable tie-breaker: earlier server-recorded time reaching the score, then stable anonymized ID. Top 7 promoted; bottom 5 demoted; others stay. Define boundary behavior for new users and top/bottom leagues (no promotion above Legend; no demotion below Bronze). Store previous and new rank so the promotion/demotion animation and report are data-backed.
- Leaderboard row: rank, avatar, pseudonymous display name, weekly XP, promotion/demotion zone, clear “You” row pinned/highlighted. Private mode hides user from other members while keeping all local features; decide whether hidden users are omitted from cohort/rank calculations and document consistently.
- Default profile: anonymous name/avatar. Optional Google linking must safely link the anonymous account without losing local data. App should work if Firebase is unconfigured during local development.
- Sync XP on completion through queued events, debounced to 30 seconds, with idempotency IDs and offline replay. Cloud Function validates timestamp, event uniqueness, plausible XP, max 3,000 XP/day, and rejects or flags malicious payloads. Because task title remains local, the backend validates event metadata/limits, not task text. Treat client-side totals as untrusted.
- Weekly promotion/demotion must be run in a transaction/idempotent scheduled Cloud Function with explicit timezone. Add Firestore security rules, indexes, App Check, test emulator configuration, and tests for race/retry conditions.
- Friend system: invite code, add/remove friend, friend-only leaderboard, opt-in weekly 1v1 challenge (most validated XP wins; bonus coins). Prevent challenge duplicate claims and self-challenges.
- If disabled, leagues are hidden/disabled without damaging local XP/quests. If offline, show cached ranking and last-sync timestamp, distinguish “stale cache” from “no data”.

## 10. Analytics, charts, and weekly reports

Stats ranges: Day / Week / Month / Year / All. Support overall and category-specific (each individual category or All).

- Overall XP line chart; planned vs completed task bars; completion rate; total focus hours; streak history.
- Per-category XP trend, time spent, task count, level progress, average difficulty. XP-share donut and horizontal ranking bars.
- GitHub-style annual activity heatmap with intensity based on daily XP, accessible legend and keyboard/screen-reader descriptions.
- Hour-of-day productivity chart; best weekday; rule-based insights with sample-size guards, e.g. “Your completion rate was 18% higher on Tuesdays over the last 4 weeks.” Do not infer causal conclusions and do not show fabricated precision from tiny samples.
- Every chart point is tappable, opens a tooltip, and drills into that date’s task list. Handle empty, one-point, all-zero, large-data, rotated and resized charts.
- Animated charts meet motion rules; no animation on every provider rebuild. Use stable chart keys and animation only on data/range changes.
- Weekly report notification Sundays 20:00 (display timezone) and in-app report containing XP gained, best category, streak, badges earned, league rank movement, focus time, tasks planned/completed. Report and its shareable image must be generated from the same report DTO.
- Stats only calculate from actual data and account for deleted/undo events consistently. Keep aggregations efficient; add DB indexes and test query plans with realistic data volume.

## 11. Screens and navigation

Five primary destinations: **Today · Stats · Quests/Badges · League · Profile/Settings**. Use an adaptive bottom bar/rail according to window size. Deep links should open task detail, badge detail, report, and invitation appropriately.

1. Onboarding, ≤3 pages and skippable: goal/categories of interest; theme/accessibility defaults; notification explanation and optional sign-in. Do not block app use on account creation or notification permission.
2. Today: date, slim level/XP bar, streak, coins, week strip, `By Time`, `Anytime`, `Completed`, missed/reschedule state, compact task rows, bottom quick-add. Show a useful empty state with real suggestion chips.
3. Add/Edit task sheet, one main sheet; time picker mandatory/immediate for By Time; validation inline.
4. Task detail: edit, notes, subtasks, timer, XP breakdown, occurrence history, reschedule/repeat options, destructive actions with undo.
5. Stats with range control, category selector, chart drill-down and weekly report.
6. Quests/Badges: daily quest list, XP rewards and badges grid/progress/detail/share.
7. League: league tier, weekly position, cached/offline indicator, ranks/zones, friends/challenges, promotion history.
8. Profile: avatar/name, overall level/title, total XP, category level cards, badge preview, stats summary, coin store. Store themes, avatar frames and streak freezes; owned/locked/purchased state must persist in Drift and spending must be atomic.
9. Settings: grouped, searchable if useful, every toggle wired to actual behavior.
10. Onboarding, permission-denied, empty, loading, error, restore-conflict, backup-failure, ML-unavailable and offline states for every relevant screen. Include retry/recovery rather than decorative dead ends.

## 12. Settings: fully wired

**Appearance:** Light/Dark/AMOLED/System; 8 accent colors; font-size slider 80–130% default 90% with live preview; Density Compact/Comfortable/Spacious; font family Inter/System/Serif; radius slider/steps; Reduce Motion; Haptics; Sounds; optional dynamic color.

**Tasks:** default duration; reminder offset 5/10/15/30 min; start of week; day reset 00:00 default with 04:00 option; auto-move missed tasks; 12h/24h time format; duplicate title warning; default category/difficulty; recurring-task behavior.

**Gamification:** penalties on/off; XP breakdown visible; leagues on/off; privacy mode; streak freeze; level celebration toggle.

**ML:** on-device auto-categorization on/off (off means ask user; deterministic time parsing remains available); personal model export/reset; model version; accuracy with sample count and limitation notes; correction-history management; show local inference status.

**Notifications:** per-type toggles, quiet hours, morning/evening reminders, weekly report, due reminders, exact-alarm permission state and fallback mode.

**Data/security:** JSON export/import; CSV export; encrypted cloud backup/restore opt-in; last backup, checksum and format version; conflict review; delete all data with typed confirmation; biometric/PIN lock and recovery policy; privacy policy.

**Language/about:** English/Hindi full ARB localization; version/build number; privacy policy; licenses/open-source notices; diagnostics/export logs without task text by default.

Every setting change must affect app behavior and persist across restart. No inert switches.

## 13. Drift data model, migrations, and consistency

Required tables and fields (extend carefully as needed):

- `Tasks`: id, seriesId/occurrenceId, title, notes, categoryId, categorySource (`auto|user`), mlConfidence, mlModelVersion, difficulty, estMinutes, actualMinutes, scheduleType (`anytime|byTime|range`), dueAt, startAt, endAt, localDateKey, timezoneId, repeatRule, priority, status (`pending|done|missed`), createdAt, updatedAt, completedAt, xpAwarded, xpBreakdownJson, timerUsed, timerSeconds, notificationVersion, deletion/undo metadata.
- `Subtasks`: id, taskId, title, done, createdAt, completedAt, awardedXp; foreign key/cascade behavior documented.
- `XpLog`: id, idempotencyKey, taskId nullable, categoryId nullable, xp delta, coins delta, reason, localDateKey, createdAt, breakdownJson, sync status. Append-only; use compensating events instead of dangerous history rewrites.
- `CategoryProgress`: categoryId, attributedXp, level, updatedAt.
- `Badges`: id, unlockedAt, progress, rewardedAt.
- `Streaks`: current, longest, freezes, lastActiveDate, protectedDates.
- `MlCorrections`: id, normalizedLocalFeature/embedding, originalLabel if needed, correctLabel, correctedDifficulty, correctedDuration, modelVersion, createdAt, weight, private/local-only.
- `Settings`: key, typed/serialized value, updatedAt.
- `Quests`: id, localDateKey, type, target, progress, done, rewardedAt, configJson.
- Add `TaskSeries`, `FocusSessions`, `NotificationSchedules`, `DailyAggregates` (or equivalent), `SyncOutbox`, `StorePurchases/Inventory`, `Friends/Challenges` local cache, and `WeeklyReports` where required.

Required indexes at minimum: `(localDateKey, status)`, `(localDateKey, categoryId)`, `(dueAt, status)`, `(seriesId, occurrenceId)`, `(createdAt)`, XP `(localDateKey, reason)`, idempotency keys unique. Add foreign keys and cascading behavior, migration tests from every released schema version, data validation and sensible defaults. Use transactions when completing task and recording XP/coins/quest/streak/badge progression. Avoid two sources of truth for aggregate values; derive them from the ledger or transactionally maintained aggregates with reconciliation tests.

## 14. Quality, testing, security, and release acceptance

### Unit tests (minimum)
- XP formula for every difficulty/timing/streak/focus combination, round-half-up, minimum XP, duplicate title, 5-second anti-cheat factor, daily cap, subtasks, bonus idempotency, penalty floor.
- Overall/category level thresholds including 0 XP and exact boundary values.
- Streak calculation, timezone/day-reset transitions, freezes and one-time spend.
- Recurrence generation, month ends, range validation, DST, time parsing, locale/12-hour/24-hour handling and every included parser CSV case.
- League tie breaks, 7-up/5-down, minimum/maximum tier behavior, bot generation, server retries and idempotency.
- ML data audit, group split by `family_id`, model asset load, label map, confidence thresholds, fallback behavior, category/difficulty/duration output, correction persistence/weight, accuracy-window calculation.
- Import/export schema validation, encrypted backup integrity, wrong passphrase, corrupt file, schema upgrades, conflict handling.

### Widget/integration tests (minimum)
- By Time picker appears immediately when selected and Save is blocked with no time.
- Quick-add -> parse -> preview -> save -> row displays correct date/time/category/XP.
- Complete task -> only one XP award, correct bonus/level transition, undo/reopen ledger compensation.
- Delete/postpone swipe -> undo snackbar restores correct state and reminder.
- Daily quest completion/reward cannot be double-claimed.
- Settings toggles immediately alter UI/behavior and persist after restart.
- Empty/loading/error/offline states for core screens.
- 360×640 compact-mode layout and large system font layout; no overflow. Accessibility guideline tests for Android 48 dp/iOS 44 pt touch targets and labeled controls. Contrast verification for both themes/category tokens.
- Firebase Emulator Suite tests for rules, App Check configuration path, cloud function validation, sync retries, league reset idempotency, friends/challenges and backup ciphertext.

### Performance/reliability gates
- Cold start target <2 seconds on a documented reference device after first install (report measurement and test conditions; don't assert without measuring).
- ML inference target <50 ms p95 on the documented Android device; report model file size and warm/cold timings.
- No synchronous DB or ML inference on UI isolate. No layout thrash while typing/scrolling/charts animate. Use profile builds for frame measurements.
- Test app lifecycle, force-stop/restart, reboot reschedule, daylight/zone change, low storage, permission denied/revoked, no network, data restore, app upgrade and migration.
- Add CI workflow: format, analyzer, unit tests, widget tests, ML data audit/tests, build debug APK, and Firebase function tests. Build release APK/AAB and iOS simulator build where toolchain permits; explicitly state blockers if CI environment lacks Apple tooling.
- No secrets committed. Provide Firestore rules/indexes and minimum-required IAM. Add privacy-policy text reflecting on-device ML and optional encrypted backups.

## 15. README and developer documentation

README must include: exact Flutter/Dart/dependency versions tested; prerequisites; clone/setup; Android run/build commands; iOS notes; Firebase project creation/FlutterFire setup, Auth providers, App Check, Firestore rules/indexes, Cloud Functions deployment and emulator testing; notification/channel/exact alarm setup and Android permission behavior; ML model download/license/tokenizer/model build+conversion instructions; training/reproducible data split and evaluation commands; how to retrain/replace the TFLite model without changing app code; dataset provenance/limitations; backup encryption/recovery; test commands; feature matrix; known platform gaps; release checklist. Add architecture/data-flow diagrams in Markdown or Mermaid.

## 16. Required build order — execute each phase before moving on

For each phase: implement real code, run the phase's tests, fix failures, write a short `docs/phase-N-report.md` describing files changed, actual verification commands/results, known limitations, and next phase. Do not mark unchecked acceptance criteria as complete. Keep `main` runnable at each gate.

1. **Phase 1 — Foundation, theme, DB, Today, CRUD, schedules, notifications.** Deliver onboarding, theme tokens, migrations, CRUD, repeat occurrence model, quick-add, By Time validation/time picker, list grouping, undo, notifications/permission fallback. Gate: core unit/widget tests pass; Android build succeeds.
2. **Phase 2 — XP, levels, coins, ledger, animations, streaks, quests.** Gate: formula/streak/subtask/anti-cheat tests pass; exactly-once award behavior verified.
3. **Phase 3 — ML categorization + personalization.** Ship dataset audit/training/conversion scripts, actual model/tokenizer assets, parser, confidence UI, corrections/kNN personalization, model accuracy view. Gate: offline inference works, metrics are emitted honestly, dataset leak test passes.
4. **Phase 4 — Stats/charts/weekly report.** Gate: chart values match DB/ledger fixtures, tap drill-down works, empty/large data and weekly report tests pass.
5. **Phase 5 — Badges.** Gate: catalogue schema validates, required groups and generated category badges exist, progression/reward idempotency tests pass, sharing exports a real image.
6. **Phase 6 — Firebase, leagues, friends, challenges, encrypted backup/restore.** Gate: Emulator Suite rules/function tests pass, offline outbox replay verified, privacy rules documented, weekly promotions idempotent.
7. **Phase 7 — Settings polish, localization, widgets, app lock, export/import, accessibility, performance, CI, README.** Gate: full tests and Android release build; iOS build where available; accessibility audit; migration and restore tests; honest final feature matrix.

## 17. Final deliverables and completion report

Deliver the complete source repository, lockfile, assets, SQL/Drift migrations, generated model + tokenizer + label map, reproducible ML scripts/metrics, seed dataset, parser regression dataset, `badges.json`, localization ARBs, Firebase rules/functions/config templates, tests, CI workflow, README, privacy/licence notices, and a final phase-by-phase feature matrix. Include Android APK/AAB only if the environment actually built it.

At the end, report:
1. What actually works end-to-end and was verified per phase.
2. Test commands and pass/fail counts.
3. Measured model size, macro-F1/per-language results, p95 latency, and reference device; if not measured, say so.
4. App build outputs with verified paths.
5. Firebase setup that still requires the project owner's credentials.
6. Any real remaining limitation, never disguised as a completed feature.

**Final non-negotiable rule:** functionality beats appearance-only scaffolding. If a requirement cannot be achieved with the chosen dependency or environment, implement the closest real, local, tested alternative, describe exactly what changed and why, and never claim the original requirement is complete.
