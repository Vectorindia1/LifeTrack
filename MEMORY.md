# MEMORY.md — Persistent Decisions & Context

This file holds decisions, rationale, and gotchas that must survive across Claude Code sessions. Unlike `PROGRESS.md` (what's done), this file is about *why things are the way they are* — so future sessions don't re-litigate or accidentally contradict earlier choices.

Add a new dated entry whenever a non-obvious decision is made. Never delete old entries — if a decision changes, add a new entry noting the change and why.

---

## Decisions Log

### [date] — Project kickoff
- Chose Kotlin + Jetpack Compose over XML views: faster iteration, less boilerplate, better fit for a solo-built app with a small feature surface.
- Chose Room over any other persistence: fully offline, no backend for v1, well-supported with Compose.
- Chose WorkManager over AlarmManager for notifications: survives device reboot and app kill, better fit for the "one consolidated daily check" pattern.
- Architecture: MVVM + Repository. Feature-based package structure (see CLAUDE.md).
- App name (working title): **LifeTrack**.

### [date] — Charting library
- **Decision pending** — must be made in Milestone 1/2 and recorded here before any chart is built. Options considered: Vico (Compose-native, actively maintained) vs MPAndroidChart (mature but View-based, needs Compose interop).
- Once decided: record the choice, the reason, and any interop gotchas here.
- **RESOLVED 2026-08-28 — see "Charting library — resolved: Vico" below.**

### [date] — Notification consolidation
- Explicit product requirement (see PRD 7.8): notifications must be batched into one daily digest per default schedule, not fired independently per tracker. Any future notification-related work must preserve this — do not add ad-hoc per-event notifications without updating PRD.md first.

### [date] — Data model
- Single `Entry` concept unifies habit/goal/expense/calorie/water/diary logs at a conceptual level (see PRD section 6), but each tracker also has its own dedicated table for type-specific fields. Do not try to force everything into one generic table — the per-feature tables exist for a reason (type-specific queries, indices, and simpler ViewModels).

---

### 2026-08-28 — Charting library — resolved: **Vico**
**Decision:** Vico (`com.patrykandpatrick.vico`), version **3.2.3**, artifacts `vico-compose-m3` (+ `vico-core` transitively). This is now the *only* charting library in the project — per CLAUDE.md, do not mix in another.

**Why Vico over MPAndroidChart:**
- **Compose-native.** MPAndroidChart is a View-based library; every chart would need an `AndroidView` interop wrapper. The whole UI is Compose + Material 3, so interop would be friction on every single chart screen (Milestones 2, 4, 5, 6).
- **Theming / dark mode falls out for free.** Vico's `compose-m3` artifact reads colors from `MaterialTheme`, so charts follow the app's light/dark theme automatically. With MPAndroidChart, dark mode (PRD Milestone 11) means manually bridging theme colors into every chart's paint objects — exactly the kind of busywork this project should avoid.
- **Maintenance.** MPAndroidChart's last real release was v3.1.0 (2019, archived-ish since). Vico is actively released (3.3.0 was current as of this session; we pin 3.2.3 for compileSdk reasons — see the toolchain entry).
- **Scope fit.** PRD only ever asks for bar and line charts (7.2, 7.4, 7.5, 7.6). That's Vico's core competency — no need for the long tail of chart types MPAndroidChart offers.
- **Recomposition behaviour.** Vico charts take Compose state directly, which suits ViewModel `StateFlow` data; the View-based alternative needs manual `invalidate()`/data-set juggling.

**Gotchas to watch:**
- **Vico 3.x renamed and restructured much of its API vs 1.x/2.x.** Most blog posts / StackOverflow answers online are for 1.x and will not compile. Use the official docs for the pinned version only.
- Artifact coordinates changed across major versions too — pin `3.2.3` in `gradle/libs.versions.toml` and change it deliberately, not incidentally.
- **Progress rings are NOT a Vico job.** PRD 7.1/7.6 want a water progress *ring* — that is a plain Compose `Canvas` / `CircularProgressIndicator`, not a chart. Don't pull a charting dependency into the dashboard for it.
- Vico is declared in the version catalog and wired into `app/build.gradle.kts` now, but no chart is drawn until Milestone 2 (habit completion chart).

### 2026-08-28 — Dependency injection: manual, no Hilt (for now)
- PRD 5 lists Hilt as *optional*, "add only if complexity warrants". It does not yet.
- Using a plain `AppContainer` service-locator held by `LifeTrackApplication`, with `viewModelFactory` for ViewModels. Zero annotation processing beyond Room's, faster builds, less magic in a single-user app.
- **Revisit if** the WorkManager milestone (9) needs injected workers, which is the usual tipping point. If Hilt is added later, record it here with the reason.

### 2026-08-28 — `Entry` is a concept, not a table (confirmed with user)
- PRD section 6 lists `Entry (id, type, timestamp, value, note)` alongside the per-feature tables, which read ambiguously — is it a real table?
- **Confirmed with the user this session: it is conceptual only.** The Room schema contains *only* the per-feature tables. Nothing dual-writes to a generic log table.
- If a unified chronological "everything I did today" feed is ever wanted, build it as a read-only Room `@Query` with a `UNION ALL` across the feature tables — do not introduce a mirrored write path and two sources of truth.
- This confirms and sharpens the earlier "[date] — Data model" entry above.

### 2026-08-28 — Habit frequency: schema supports custom day-of-week now, UI later
- Resolves one of PRD section 11's open questions, at the schema level only.
- `Habit` stores `frequencyType` (`DAILY` / `WEEKLY` / `CUSTOM_DAYS`) **plus** `daysOfWeekMask: Int` (bit 0 = Monday … bit 6 = Sunday) and `timesPerWeek: Int?`.
- **Why:** the columns are nearly free now, but adding them later means a Room migration on a table that by then holds real user data. The v1 *UI* can still offer only daily/weekly — `CUSTOM_DAYS` simply won't be selectable yet.
- Decided with the user this session.

### 2026-08-28 — Expense/calorie categories: `category` stays a plain `String`
- PRD section 11 asks whether categories should be user-definable from day one. **Deferred, deliberately, with no schema cost.**
- `Expense.category` is a `String` exactly as PRD section 6 specifies. A preset list can be a Kotlin enum/constant used by the UI, and user-defined categories can be added later as a separate `ExpenseCategory` table *without* migrating `Expense` itself.
- So Milestone 4 can decide the UX without being boxed in. Nothing to re-litigate at schema level.

### 2026-08-28 — Navigation shell shape: 5 bottom tabs, not 7
- There are 9 destinations (dashboard, 6 trackers, settings) but a Material 3 `NavigationBar` should hold 3–5 items, and CLAUDE.md's UX principles push against clutter.
- **Bottom bar:** Dashboard, Habits, Expenses, Diary, Settings.
- **Reached from dashboard cards instead:** Goals, Calories, Water. This is consistent with PRD 7.1, where the dashboard is the jumping-off point for everything anyway.
- All 9 are real destinations in the single `NavHost` — only the bottom bar is a subset. Revisit in Milestone 11 (polish) if it feels wrong in use.

### 2026-08-28 — Toolchain versions pinned
- **AGP 8.13.2 / Gradle 8.14.5 / Kotlin 2.4.10 / KSP 2.3.11 / Java 21.** All pinned in `gradle/libs.versions.toml`.
- **compileSdk / targetSdk = 36, minSdk = 26.**
- **The whole toolchain is pinned by one hard constraint: API 37 is not installable.** The SDK repository currently publishes platforms only up to `android-36` (the older `cmdline-tools` even *lists* `platforms;android-37`, but it does not exist to download — verified this session with an updated `cmdline-tools` 23.0). compileSdk 37 is therefore impossible on this machine right now.
- That single fact cascades into every other version choice, because several current libraries have `minCompileSdk=37` in their AAR metadata and hard-fail `checkDebugAarMetadata`:
  - **Compose BOM pinned to `2026.06.01` (Compose 1.11.4), not the latest `2026.08.00` (Compose 1.12.0).** Compose 1.12 requires compileSdk 37 *and* AGP 9.1+.
  - **Vico pinned to `3.2.3`, not the latest `3.3.0`.** Vico 3.3.0 requires compileSdk 37; 3.2.3 is the newest release with `minCompileSdk=36`. This does not change the Vico-vs-MPAndroidChart decision above, only which Vico version.
- Chose AGP 8.13.2 (last 8.x) over AGP 9.x deliberately: with compileSdk capped at 36 there is nothing to gain from AGP 9, and 9.x is a large breaking release. Not a permanent decision.
- **When API 37 ships, these move together, not one at a time:** compileSdk/targetSdk 37 → AGP 9.1+ → Gradle 9.x → Compose BOM 2026.08.00+ → Vico 3.3.0+. Bumping any one of them alone will fail the AAR metadata check.
- **How to diagnose this class of failure fast:** `checkDebugAarMetadata` failing with "requires ... compile against version N" means read the offending AAR's own metadata rather than guessing versions:
  `unzip -p <artifact>.aar META-INF/com/android/build/gradle/aar-metadata.properties | grep minCompileSdk`
- Local machine specifics: SDK at `~/Android/Sdk`, JDK 21 at `/usr/lib/jvm/java-21-openjdk-amd64` (system default `java` is 25, which AGP 8.x does **not** support — `gradle.properties` pins `org.gradle.java.home`).

### 2026-08-28 (session 2) — Habit frequency: all three types ship in the milestone-2 UI
- Resolves PRD section 11's open question **at the UI level** (the schema was already settled in session 1).
- The add-habit sheet offers **Every day / Times per week / Certain days**. Custom day-of-week cost one chip row because `daysOfWeekMask` already existed, so deferring it would have saved nothing and left PRD 7.2 unimplemented.
- `WEEKLY` is interpreted as **"N times per week"** (slider, 1–7), not "once on some particular day". PRD 7.2 only says "weekly"; this reading is the one that supports a real goal like "gym 3× a week".
- I asked whether the UI should include custom days and did not get an explicit answer before being told to continue, so this is my call — easy to narrow later, since removing a chip does not need a migration.

### 2026-08-28 (session 2) — Streak rules (the part most likely to be argued with later)
Encoded in `HabitSchedule` and pinned by 16 unit tests in `HabitScheduleTest`. The rules:
- **Day-based habits (DAILY, CUSTOM_DAYS):** streak = consecutive *scheduled* days completed. An unscheduled day is skipped entirely — for a Mon/Wed/Fri habit, not doing it on Tuesday is not a miss.
- **Weekly habits:** streak counts consecutive **weeks** that met the target, not days. Weeks are Monday-based, matching `daysOfWeekMask`'s bit order.
- **Today is a grace period.** Not having logged today does not zero the streak — the day is not over. Same for the current week on weekly habits. Missing *yesterday* does break it.
- **Nothing before `createdAt` counts**, even if a log row somehow exists for that date.
- **Completion rate returns null, not zero,** for a window in which the habit was never due. Zero would render as "you failed", which is a lie for a habit that did not exist yet.
- If any of these need to change, change the test first — the tests are the specification.

### 2026-08-28 (session 2) — `streakCount` column is a cache, logs are the truth
- Displayed streaks are always computed from `habit_logs` via `HabitSchedule.currentStreak`.
- `Habit.streakCount` (required by PRD section 6) is maintained as a write-through cache by `HabitRepository.refreshStreak`, so the milestone-9 notification worker can read a streak without replaying history.
- **Never render `habit.streakCount` directly in the UI** — it can lag a write. Compute it.

### 2026-08-28 (session 2) — Habit chart is aggregate, not per-habit
- The completion-rate bar chart on the habit screen averages **all** habits, with a Weeks (8) / Months (6) toggle.
- PRD 7.2 says "weekly/monthly bar chart of completion rate" without specifying scope. Per-habit charts would need a habit-detail screen that the PRD never asks for, and would break the "no screen needs more than one scroll" rule.
- Per-habit streaks still appear on each row, so the per-habit signal is not lost.

### 2026-08-28 (session 2) — Vico 3.2.3 API, as actually verified
The 1.x/2.x examples online do not compile. What works, confirmed by compiling against the artifact:
- `CartesianChartHost(chart = rememberCartesianChart(...), modelProducer = ...)`.
- Layers: `rememberColumnCartesianLayer(columnProvider = ColumnCartesianLayer.ColumnProvider.series(rememberLineComponent(fill = Fill(color), thickness = ..., shape = ...)))`.
- Axes are **companion functions on the axis class**: `VerticalAxis.rememberStart(...)`, `HorizontalAxis.rememberBottom(...)` — not the old free-standing `rememberStartAxis()`.
- Data goes in via `modelProducer.runTransaction { columnModel { series(values) } }`. **`columnSeries` is deprecated in 3.2.3 — use `columnModel`.**
- `Fill(color)` wraps a Compose `Color`; there is no `fill()` helper function.
- Everything lives in the single `compose` artifact (`com.patrykandpatrick.vico.compose.*`); there is no separate `core` module to depend on in 3.x.
- **Tip for future sessions:** rather than guessing the API, read it off the artifact —
  `unzip -p <compose>.aar classes.jar > c.jar && unzip -q c.jar && javap -public <Class>.class`.

### 2026-08-28 (session 3) — Dashboard shows only habits *due today*
- The dashboard lists habits where `HabitSchedule.isScheduledOn(habit, today)` is true, not every habit.
- A Mon/Wed/Fri habit simply is not on Tuesday's dashboard. Showing it greyed out would add noise to the one screen PRD 7.1 insists must be readable at a glance.
- Consequence to remember: the dashboard's "2 of 3" counter is over *due* habits, so it can differ from the total habit count. That is intended.
- Weekly habits are due every day (any day can satisfy the weekly target), so they always appear.

### 2026-08-28 (session 3) — `today` is reactive state in ViewModels, not a captured constant
- Both `DashboardViewModel` and `HabitViewModel` hold `today` in a `MutableStateFlow` and expose `refreshDate()`, which the screens call from `LifecycleResumeEffect`.
- **Why:** a ViewModel that captured `LocalDate.now()` once would keep showing yesterday for an app left open across midnight — and this app is specifically meant to be opened briefly and often, including late at night.
- The completions query is keyed off that flow with `flatMapLatest`, so the whole state recomputes for the new day.
- Anything added later that needs "today" should follow this pattern rather than calling `LocalDate.now()` inline.

### 2026-08-28 (session 3) — The milestone-1 database status card is gone
- `DashboardScreen`'s temporary "Room is live — 10 tables" card and its `db_status_*` strings were deleted, as planned when it was written.
- **It was never seen running.** It was the intended runtime proof that Room opens and seeds, and no device was ever available to display it. That proof is now folded into the real dashboard: if habits appear and a tick persists, Room works.

### 2026-08-28 (session 4) — Expense categories: presets **and** user-defined
- Resolves the last of PRD section 11's open questions. Decided with the user this session.
- 8 presets (`ExpenseCategories.PRESETS`) shown as chips, plus a "Custom…" chip that reveals a free-text field.
- **There is no category table and there should not be one.** `Expense.category` stays a plain `String`; a custom category "exists" precisely because some expense row uses it. `ExpenseCategories.allKnown()` derives the chip list as presets first, then custom ones already used, sorted.
- Presets are kept deliberately short — a long list turns a two-tap action into a scrolling decision, which fights PRD section 8.
- Consequence: renaming a category later means updating rows, not one table row. Acceptable for a single-user app; revisit only if bulk-rename is ever wanted.

### 2026-08-28 (session 4) — Money is locale-formatted, never a hardcoded symbol
- `core/ui/Money` uses `NumberFormat.getCurrencyInstance()` against the device locale.
- PRD 7.7's example reads "₹450", but hardcoding ₹ would be wrong on any other device. An India-locale phone gets ₹ from the locale anyway.
- Amounts are stored as bare `Double` with **no currency code** — this is a single-user, single-currency app. If multi-currency is ever wanted, that is a schema change, not a formatting change.
- Whole amounts drop the ".00"; chart axes use `formatCompact` (450 / 1.5k / 1M) because a repeated currency symbol on every gridline is noise.

### 2026-08-28 (session 4) — Expense day boundaries convert to local time
- Expenses are stored as `Instant`, but "today's spend" is a **local calendar** question.
- `LocalDate.startOfDay()/endOfDay()` in `ExpenseRepository.kt` do the zone conversion; every range query goes through them.
- **Never slice expense ranges on UTC** — for a user at UTC+5:30, an evening expense would land on the wrong day. Covered by `ExpenseLogicTest`.
- `endOfDay` is `next midnight − 1ms`, so consecutive days cannot both match the same instant.

### 2026-08-28 (session 4) — Spend windows: DAY / WEEK / MONTH
- WEEK is a **rolling 7 days** ending today; MONTH is **calendar month-to-date**, not a rolling 30 days.
- Chosen because "this month" is how people actually think about a budget, whereas a rolling 30-day figure answers a question nobody asked. Pinned by tests.

### 2026-08-28 (session 5) — Chart code is centralised in `core/ui/chart/Charts.kt`
- `SimpleBarChart` and `SimpleLineChart` are the only two chart entry points. The habit, expense and calorie charts are thin wrappers that supply data, colour and a y-axis formatter.
- **Why:** CLAUDE.md restricts graphs to bar/line/ring, and Vico's API changes sharply between major versions. Keeping Vico imports in one file means a future Vico upgrade is a one-file change instead of a hunt.
- **Add new charts by wrapping these, not by importing Vico directly.** If a chart genuinely cannot be expressed here, that is a signal it is too complex for the PRD's "simple graphs only" rule.
- Progress rings are deliberately NOT here — they are plain Compose (`LinearProgressIndicator` today, a `Canvas` ring for water in milestone 6), not charting.

### 2026-08-28 (session 5) — Calorie progress clamps, overshoot lives in `remaining`
- `CalorieUiState.progress` is coerced into 0f..1f so a progress bar can never overflow its track.
- Going over target is expressed by `isOverTarget` and a **negative** `remaining`, and rendered in the error colour.
- Eating exactly the target is **not** "over" — pinned by a test, because off-by-one here would nag the user on a day they got it exactly right.
- A zero or missing target yields 0f rather than dividing by zero.

### 2026-08-28 (session 5) — Calorie target editing temporarily lives on the calorie screen
- PRD 7.9 puts daily targets in Settings, which is **milestone 10**.
- A "Change target" dialog sits on the calorie screen for now, because otherwise the target is stuck at the seeded 2000 kcal and the whole eaten-vs-target feature cannot be exercised.
- **Move it to Settings in milestone 10** and decide then whether to keep the shortcut. Same will apply to the water target in milestone 6.

### 2026-08-28 (session 6) — Progress rings are plain Compose, never a chart library
- `core/ui/ProgressRing` is a `Canvas` drawing two arcs, with `animateFloatAsState` for the fill.
- CLAUDE.md permits "bar, line, or progress ring", but a ring shows **one number**, not a data series. Pulling Vico in for it would be the wrong tool and would drag chart machinery onto the dashboard.
- Starts at 12 o'clock and fills clockwise, the way a dial is read. Progress is clamped, so the ring can never wrap past full and read as "barely started" when the user is actually over.

### 2026-08-28 (session 6) — Water is forgiving where calories are not
- Deliberate asymmetry, and it is not a bug:
  - **Calories:** going over matters, so `remaining` goes negative and the bar turns red.
  - **Water:** going over is fine, so `remainingMl` floors at 0 and there is no "over" state at all.
- Both clamp `progress` to 1.0 and both treat exactly-on-target as met/not-over. Pinned by tests in `WaterUiStateTest` and `CalorieUiStateTest`.
- An "Undo" affordance exists on water specifically because +250/+500 are one-tap buttons and a mis-tap is likely. It deletes today's most recent log.

### 2026-08-28 (session 6) — Dashboard aggregation uses a private `DayData` holder
- `combine`'s typed overloads stop at **five** flows; the dashboard already needed six.
- `DashboardViewModel` now groups the per-day sources into a private `DayData` inside one `today.flatMapLatest { ... }`, keeping the outer `combine` to two arguments.
- **Add goals (milestone 7) and diary (milestone 8) as fields on `DayData`**, not as extra arguments to the outer combine — otherwise the same five-flow ceiling gets hit again.

### 2026-08-28 (session 6) — Water quick-add lives on the dashboard itself
- The +250/+500 buttons are inline on the dashboard card, so logging a drink is **one tap from the home screen** — the strictest reading of PRD section 8.
- "Custom" navigates to the water screen rather than opening a dialog on the dashboard, keeping the dashboard free of modals.

### 2026-08-28 (session 7) — The no-scrolling constraint is relaxed
- **Decided by the user.** PRD 7.1 ("all visible without scrolling"), PRD 8 and CLAUDE.md UX principle 2 have been amended in place; each amendment is dated so the change is visible rather than silent.
- Mechanically nothing had to be built: every feature screen was already a `LazyColumn` and the dashboard already scrolled. What changed is the **design constraint**, which had been forcing a compaction budget on the dashboard as trackers were added.
- **What still holds:** the first screenful must carry today's essentials and the one-tap actions. Scrolling is permitted; burying the habit checkboxes or the water quick-add below the fold is not.
- Practical consequence: milestones 7 and 8 can add goal and diary cards to the dashboard without fighting for vertical space, and the milestone-11 polish pass no longer needs a "make it all fit" step.

### 2026-08-28 (session 7) — Goal rules
Encoded in `GoalProgress`, pinned by `GoalProgressTest`:
- **A finished goal is never overdue.** Finishing late is still finishing, and nagging about a completed goal would be actively demoralising.
- **A goal with no deadline is never overdue and never "near"** — undated goals are aspirations, not obligations.
- `daysRemaining` is deliberately **not clamped**: 0 = due today, negative = overdue, null = no deadline. The UI branches on those cases; do not "fix" the negative.
- Reaching the target exactly counts as complete.
- Dashboard ordering is soonest-deadline-first with **undated goals last**, not interleaved.
- **`isDeadlineNear(withinDays = 3)` already implements PRD 7.8's goal reminder window.** Milestone 9's notification worker must reuse it rather than reimplementing the window.

### 2026-08-28 (session 7) — Completed goals stay visible in their own section
- Rather than disappearing when finished, goals move to a "Completed" section.
- Seeing what you finished is a large part of why goal tracking is motivating; deleting the evidence defeats the purpose. Only the dashboard filters to active goals, because that is a today-focused surface.

### 2026-08-28 (session 7) — Goals join the dashboard's outer combine, not `DayData`
- `DayData` exists for per-day queries keyed off `today` via `flatMapLatest`. **Goals are not day-scoped** — the goal list is the same regardless of date — so `goalRepository.observeGoals()` is a third argument to the outer `combine` instead.
- That leaves the outer combine at 3 of its 5 slots, with room for diary in milestone 8. Diary *is* day-scoped, so it belongs in `DayData`.
- The `Goal → GoalItem` mapping is shared: `goal/viewmodel/toItem(today)` is used by both the goal screen and the dashboard, so progress and deadline maths cannot drift between them.

### 2026-08-28 (session 8) — Diary prefill never auto-saves
- PRD 7.7 wants the entry "auto-prefilled" with a summary line. The obvious implementation — write the summary into the entry — would have created a **junk entry for every day the user merely opened the diary**, which would silently inflate the diary streak and pollute the calendar.
- Implementation: the text field is seeded with the summary **in the draft only**, and nothing is persisted until Save is pressed. Opening the diary and walking away stores nothing.
- The summary returns **null when nothing was tracked**, so a genuinely empty day gets a blank page rather than a line of zeroes.
- Save is disabled on blank text, so an entry cannot be created empty.

### 2026-08-28 (session 8) — Diary streak matches the habit grace rule
- Not having written **today** does not break the streak (the day is not over); missing **yesterday** does. Identical to `HabitSchedule`'s rule, deliberately — two different streak rules in one app would be confusing.
- Future-dated entries cannot inflate the streak, and `selectDate` refuses future dates outright.
- Pinned by `DiaryStreakTest`.

### 2026-08-28 (session 8) — The calendar is hand-built, not a DatePicker
- Material 3's `DatePicker` is a *picker*, not a browser: it cannot mark which days have entries, which is the entire point of PRD 7.7's calendar view.
- `diary/ui/MonthCalendar.kt` is a plain month grid — Monday-first (matching habit weeks), entry days tinted, today bold, future days visible but not clickable.

### 2026-08-28 (session 8) — The diary depends on every other repository
- `DiaryViewModel` takes the habit, expense, water and calorie repositories, because PRD 7.7's summary line quotes all of them.
- It **reuses those repositories rather than adding its own queries**, so the numbers in the diary can never disagree with the numbers on the tracker screens.
- This is the one intentional cross-feature dependency in the app. If another feature starts wanting the same aggregate, extract it — do not copy the assembly logic.

### 2026-08-28 (session 8) — Dashboard is now feature-complete for PRD 7.1
- All six sections exist: habits, water (with quick-add), calories, spend, goals, diary.
- `refreshDate` on the diary only follows the clock past midnight **if the user was looking at today** — otherwise it would yank them off a past date they were deliberately reading.

### 2026-08-28 (session 9) — How "one consolidated notification" is actually enforced
PRD 7.8 demands ONE digest, but its own table lists **six** default check times (09:00, 14:00, 18:00, 20:00, 20:30, 21:30) while PRD 8 caps things at "~3 pushes/day". Those pull against each other. Resolved as:
- **One fixed notification id (1001).** Every check updates the same notification instead of stacking, so there is never more than one LifeTrack notification on the device. This is the structural guarantee — even a bug in the scheduling cannot produce a pile of notifications.
- **`setOnlyAlertOnce(true)`.** The first check of the day alerts; later ones update the text silently. That is how six check *times* stay within three felt *interruptions*.
- **`IMPORTANCE_LOW`** channel: no sound by default. This is a gentle nudge app.
- **An empty digest posts nothing.** Silence is the correct output when the user is on top of everything.
- **Do not add per-event notifications.** This has been an explicit product requirement since session 0 and is now structurally enforced by the single id.

### 2026-08-28 (session 9) — Chained one-shot workers, not PeriodicWorkRequest
- The check times are irregular times of day, which a fixed repeat interval cannot express. So each `DailyDigestWorker` run schedules its successor via `DigestScheduler.scheduleNext`.
- Rescheduling happens in a **`finally`**, so a failed run still arms the next one — one bad run can never silently end all future reminders.
- `enqueueUniqueWork(REPLACE)` makes scheduling idempotent, so `LifeTrackApplication.onCreate` re-arming on every launch is safe and also repairs a broken chain.
- **The worker returns `success` even on error, never `retry`.** The `finally` re-enqueues the unique work with REPLACE, which would cancel a retry anyway; missing one check is harmless because the next one re-reads the same state.
- **Settings changes need no extra plumbing** — the next run reads `notification_settings` fresh. Milestone 10 only has to call `DigestScheduler.scheduleNext` after saving.

### 2026-08-28 (session 9) — Digest thresholds, chosen not derived
PRD 7.8 states the triggers but not the numbers. These are in `DailyDigest`, pinned by tests:
- **Calories "under"** = below **80%** of target (`UNDER_TARGET_FRACTION`). Without a threshold, any day not landing exactly on target would nag. "Over" is any amount above target.
- **Water "behind expected pace"** = below a **linear ramp across an 08:00–22:00 waking window**. At the 14:00 check that expects ~43% of the target; at 18:00, ~71%. Water is not expected to be drunk uniformly across 24 hours.
- A feature with **no target set** (0) never nags — no target means no opinion.
- A feature contributes only once one of its reminder times has **already passed today**.

### 2026-08-28 (session 9) — POST_NOTIFICATIONS is the app's only permission
- Requested once on first launch (API 33+). **Nothing is gated on the answer**: refusing means no digest, and every tracker still works exactly as before, so the result is ignored rather than re-prompted.
- `INTERNET` is still absent — verified in the built APK. PRD 8's offline requirement continues to hold.

### 2026-08-28 (session 10) — First Room migration: v1 → v2, `app_preferences`
- Adds a single-row `app_preferences` table (theme + the configurable water quick-add increments), matching the pattern already used by `calorie_goal` and `water_goal`.
- **Chose Room over DataStore** for these preferences: one storage mechanism is simpler to reason about than two, and these values are read alongside other Room data anyway. Recorded because adding DataStore later would now be a contradiction, not a free choice.
- **The migration's `CREATE TABLE` is copied verbatim from the exported schema** (`app/schemas/.../2.json`). Room validates the post-migration schema against that file and crashes on a mismatch, so hand-writing "equivalent" SQL is a real hazard — `id INTEGER NOT NULL PRIMARY KEY` versus `PRIMARY KEY(\`id\`)` is exactly the kind of difference that invites a crash on upgrade.
- **The default row is inserted in both `SeedCallback` and the migration.** A migrating install never runs `onCreate`; without the migration insert, existing users would get an empty table and silently fall back to defaults forever.
- **Procedure for the next migration:** change the entity, bump `version`, build once to regenerate the schema JSON, then copy that file's `createSql` into the migration verbatim.

### 2026-08-28 (session 10) — Settings owns the targets; the shortcuts stayed
- Calorie and water targets are now editable in Settings, as PRD 7.9 intends.
- **The in-place dialogs on the calorie and water screens were kept**, not removed. They were introduced as temporary in milestones 5 and 6, but adjusting a target while looking at the tracker is genuinely fewer taps than navigating to Settings, and both paths write through the same repository, so they cannot disagree.
- Water quick-add amounts (PRD 7.9's "ml increments") moved from constants to `app_preferences`, so `WaterViewModel.QUICK_SMALL_ML` / `QUICK_LARGE_ML` **no longer exist** — read them from state.

### 2026-08-28 (session 10) — Expense categories are renamed by bulk-updating rows
- There is still no category table (see the milestone-4 entry). "Rename" is `UPDATE expenses SET category = ? WHERE category = ?`, and the settings screen shows each category's entry count so the blast radius is visible before confirming.
- Deleting a category is deliberately **not** offered: it would either orphan rows or silently rewrite them. Renaming to an existing name merges the two, which is the useful behaviour anyway.

### 2026-08-28 (session 10) — Changing a reminder re-arms the scheduler immediately
- `SettingsViewModel` calls `DigestScheduler.scheduleNext` after every reminder edit, so a new time takes effect from the next check rather than the next app launch.
- This is why `SettingsViewModel` takes a `Context` — the only ViewModel that does. It is the application context, supplied by the factory, not an Activity.

### 2026-08-28 (session 11) — Visual redesign, inspired by a reference mockup
- The user supplied a reference UI (dark theme, per-feature accent colors, icon badges, gradient charts, a real progress ring, streak-flame motif) and asked for "inspiration", not a pixel clone. Two things in it were architecture decisions, not styling, and were asked about explicitly rather than assumed:
  - **Bottom nav stayed as-is** (Dashboard/Habits/Expenses/Diary/Settings, 5 tabs; Goals/Calories/Water via dashboard cards) rather than adopting the reference's Home/Habits/Goals/[+ FAB]/Diary/Settings layout. The user chose to restyle only. If this is revisited, the FAB in that layout would need a "what are you logging?" destination picker — there is no natural single tracker for a center add button given six trackers.
  - **A `displayName` preference was added** for the dashboard's "Good evening, {name}" greeting. The user chose this over a generic greeting.
- **Design tokens live in `core/ui/theme/AccentColors.kt`**: one `FeatureAccent` (light/dark color pair) per tracker (habit=green, water=blue, calorie=amber, expense=coral, goal=violet, diary=indigo), resolved via the `FeatureAccent.resolved` extension property, which reads `LocalIsDarkTheme` — the app's *effective* theme (from the Settings toggle), not `isSystemInDarkTheme()`. Getting that distinction right matters: a user who force-picks Light while the system is in Dark must see light-mode accents.
- **These accents are a second, decorative color layer on top of MaterialTheme's own roles.** Buttons, selection, and chrome still come from `primary`/`secondary`/`tertiary`. Accents exist only for icon badges, progress rings, and chart series, so each tracker reads as its own color the way the reference does, without touching Material's semantic color contract.
- **`IconBadge`** (`core/ui/IconBadge.kt`) is the recurring colored rounded-square icon container — background is the accent at 16% opacity, not a flat fill, so it never fights the card behind it in light or dark. **`StatTile`** (`core/ui/StatTile.kt`) is the small icon+value+label tile used for the diary's today-summary row.
- **Goals get a deterministic per-item accent and icon** (`goalAccent(id)`, `goalIcon(id)`) via `id % paletteSize` — stable across sessions since it is keyed to the row's primary key, not list position (list position would shift if a goal completes and moves section).
- **Charts got a gradient area fill** under the line (via Vico's `LineCartesianLayer.AreaFill.single(Fill(Brush.verticalGradient(...)))`) and slightly rounded bar tops — confirmed the API by reading it off the AAR (same method as milestone 2), not by guessing. All charts and the progress ring now source their color from the accent tokens, not raw `MaterialTheme.colorScheme.tertiary`.
- **The dashboard's `MoreTrackers` chip row (Goals/Calories/Water) was deleted.** It was left over from milestone 3, when those three had no dashboard card yet. By milestone 7 all three had real cards, so the chip row had become pure duplicate navigation — this was noticed while reviewing the file for the redesign, not requested, and is a straightforward simplification.
- **Habit rows gained a Mon–Sun week-dot strip** (`habit/ui/DayDots.kt`) showing which days this week are done, computed from data the ViewModel already assembles — no new query.
- Dark theme's surface levels were widened (near-black background, a clearly lighter `surfaceVariant` for cards) so a card visibly "sits on" the page, rather than Material's default subtle tonal steps. Card corner radius increased app-wide via a shared `Shapes` override in `Theme.kt`.
- **Not done, on purpose, given effort/verification limits:** a true 2-column card grid like the reference. Mixing a variable-height list card (Habits) with fixed-height stat cards in the same grid row risks uneven, ugly rows that only a real device screen could catch, and nothing has been run on a device all session. Single-column, full-width cards were kept. Worth revisiting once the current pass has actually been seen.

### 2026-08-28 (session 11, follow-up) — Bug found from the first real screenshot: dashboard water buttons
The first screenshot ever taken of this app (session 11, after the redesign) showed the dashboard's water quick-add buttons rendered as absurdly tall vertical pills, each digit of "+250 ml" stacked on its own line.
- **Root cause:** the dashboard's water card packs a ring + title + three row elements (+small, +large, Custom) into a `Column(weight(1f))` that only gets the space left over after a fixed-size ring. That left too little width for three items; `Button`'s label `Text` has no line limit by default, so instead of truncating it wrapped one character per line, and the button's height ballooned to fit.
- **Fix, not a patch:** dropped `showCustom` to false on the dashboard's `WaterQuickAddRow` call — PRD 7.1 only asks for the two quick-add buttons there, not a Custom entry point, so the third element was never earning its keep. The full Water screen keeps Custom (`showCustom` defaults to `true`), where there is a full card's width to work with.
- Because dropping Custom removed the dashboard's only way to reach the full Water screen, the water card itself became clickable (`onOpen(Destination.Water)`), matching how the Calories/Expenses/Diary cards already behave. This closes a real navigation gap, not just cosmetic.
- **Defense in depth added regardless:** both quick-add button labels now carry `maxLines = 1` + `TextOverflow.Ellipsis`. A label that doesn't fit will now truncate with an ellipsis, never balloon the button's height again — this protects against the same failure mode recurring on a narrow phone or with a large accessibility font scale, which nothing in this project can test without a matching device.
- **Lesson for this project specifically:** every card that lays out more than two elements side-by-side in a `weight(1f)`-shared row is a candidate for the same failure, and none of them have been seen rendered. If another one turns up in testing, the fix pattern is the same: shrink to what the PRD actually asks for at that surface, and add a line limit as a backstop.

### 2026-08-29 — "Notifications aren't working" — likely not a bug, and now diagnosable
The user reported the notification system "isn't working." I have no device access to reproduce anything, so rather than guess at a phantom bug, I made the failure mode diagnosable and fixed the specific thing that most likely explains it:
- **The reminder times are the default PRD 7.8 schedule** (09:00, 14:00, 18:00, 20:00, 20:30, 21:30). `LifeTrackApplication.onCreate()` schedules the *next* one of these on every launch. **If the app is installed or opened after 21:30 on a given day, the very first scheduled check is 09:00 the next morning** — correct behavior, but with zero visibility into it, it reads exactly like "broken."
- Denied `POST_NOTIFICATIONS` (API 33+) or the app's notifications disabled in system settings fails **completely silently** by design (`Notifier.canPost` gates `postDigest`) — also indistinguishable from a bug without a status indicator.
- **Fix:** Settings' Reminders card now shows the actual permission status (with a button straight to the app's system notification settings when denied), the next scheduled check's date/time, and a **"Send a test notification now"** button.
- The test button runs through `DigestRunner` — the exact same digest-assembly code the scheduled worker uses, refactored out of `DailyDigestWorker.doWork()` specifically so the two paths cannot drift — with a new `ignoreTiming` flag on `DailyDigest.build()` that skips the "has this feature's reminder time passed yet" gate (a disabled feature is still skipped entirely; only the *time* check is bypassed).
- **What this actually diagnoses:** if the test button produces a notification, the whole pipeline (permission, channel, digest content, posting) is proven correct, and the original complaint was the scheduling-visibility problem above. If the test button produces *nothing*, that is a real, different bug — worth reporting back with a screenshot of what Settings' notification status row now says.
- **Not fixed and can't be, from inside the app:** aggressive OEM battery/background-app killing (common on some Android skins) can still prevent WorkManager's scheduled job from firing even though the code is correct. If the test button works but the 20:00 check never fires on its own, the next thing to check is the phone's battery-optimization/autostart settings for this app — that is outside what any in-app code change can guarantee.

### 2026-08-29 — Period tracker added: scope, wording, and a real lesson about batch edits
Added by user request — not in the original PRD's six trackers. PRD 7.10 and section 6 record it; see there for the feature description. Decisions worth keeping straight:
- **Scope: a simple log, not a cycle-prediction app.** `PeriodLog(id, startDate, endDate?)` only. History and an average cycle length are shown; a *predicted* next date is deliberately not — the user chose the minimal option over the prediction and full-symptom-tracking options when asked.
- **Wording is neutral about whose cycle is being tracked.** The user's own clarification: this app is single-user, and that one user might be logging their own periods or a partner's. No string anywhere says "my period" or assumes who is being tracked — "Period tracker", "Log period start", "Day 12 of cycle". Keep any future copy in this feature to that same neutral register.
- Placement matches every other secondary tracker: own screen (`Destination.Period`), no bottom-nav tab, reached via a dashboard card — consistent with the Goals/Calories/Water precedent and the earlier decision to keep the 5-tab bottom nav fixed.
- **Migration v3 → v4** adds `period_logs` (unique index on `startDate` — a period cannot legitimately start twice on the same day). SQL copied verbatim from the exported schema and checked byte-for-byte, per the established procedure.
- `CycleStats` (average length, current cycle day, per-entry duration) is pure and tested, matching every other piece of business logic in this app (`HabitSchedule`, `GoalProgress`, `DailyDigest`).

**Process note, not product:** two separate edits in this feature (a `DashboardUiState` field addition and its corresponding constructor call) were silently no-ops — a Python `str.replace()` call whose target string didn't match the actual file content (because the file had shifted since I last read it) returned the string unchanged instead of erroring, and the script's own success message printed regardless. The build caught it, but only because it happened to also touch a `.kt` file — the exact same failure mode hit the water quick-add fix session 11→12 in a `.xml` file, twice, before this. **Lesson: prefer the Edit tool (which reads first and errors loudly on a mismatch) over ad hoc `python -c "...replace..."` for any edit whose target text might have moved** — the water and dashboard incidents together are two failures from the same root cause in two sessions.

### 2026-08-29 — Bottom nav is icon-only; screen-reader labels moved to the icon itself
- Removed the text label under each bottom-nav icon, by user request — the icons (calendar/check/wallet/book/gear) are distinct enough without them.
- **The icon's `contentDescription` now carries what the label used to say** (`stringResource(destination.labelRes)`), rather than being null. Removing the visible label without doing this would have silently broken TalkBack for the whole nav bar — a sighted-user request should never cost accessibility.

### 2026-08-29 — Currency is a user preference, not just the device locale
- Added by user request: money was previously always formatted from `Locale.getDefault()` (see the milestone-4 entry) — correct as a *default*, but with no way to override it, someone whose phone is set to one locale but who wants to track a different currency had no path to that.
- `AppPreferences.currencyLocaleTag: String?` (nullable — null means "follow the device locale", preserving the original default exactly). Room migration v4→v5, same ADD-COLUMN shape as `displayName`.
- **`CurrencyOption`** (`core/ui/CurrencyOption.kt`) is a curated list of ~7 currencies + "System default" — not a searchable 150-entry ISO-4217 list, which would be overkill for a personal app. Each option pairs a currency with an *English-language locale in that currency's country* (e.g. EUR → `en-IE`, JPY → `en-JP`) so `Money.format` gets correct symbol **and** grouping from one `Locale`, rather than mixing a currency code with an unrelated locale's number formatting.
- **This resolves through every ViewModel that displays money — Expense, Dashboard, Diary — via `AppPreferences.effectiveCurrencyLocale()`**, a pure function threaded through their `UiState`s exactly like every other preference in this app (water increments, theme). No screen reads `Locale.getDefault()` directly for money anymore; all of them read `uiState.currencyLocale`.
- Diary's `combine` was already at 5 flows before this; `summaryFlow` and `preferencesRepository.preferences` are folded into one via a nested `combine` first, same pattern used for the dashboard's `DayExtras` earlier — see that entry for why this pattern exists.

---

## Known Gotchas / Things to Watch
- **Java 25 is the system default on this machine but AGP 8.13.2 does not support it.** `gradle.properties` sets `org.gradle.java.home` to JDK 21. If builds fail with "Unsupported class file major version" or a Gradle/JVM compatibility error, check that first.
- **Two `cmdline-tools` installs now exist:** the original `cmdline-tools/latest` (rev 12.0) and an updated `cmdline-tools/latest-2` (rev 23.0), because sdkmanager refuses to overwrite `latest` in place. Use `latest-2` for SDK package management; note its `--list` output uses `platforms/android-36` (slash) while the install argument is still `platforms;android-36` (semicolon).
- **AGP 8.13.2 warns "only understands SDK XML versions up to 3 but ... version 4 was encountered"** — this comes from packages the newer `cmdline-tools` installed. It is a warning, not a failure; the build works.
- **Vico 3.x API differs sharply from 1.x/2.x** — online examples will mostly not compile. See the charting entry above.
- **java.time is used directly (no desugaring).** Safe because minSdk is 26. If minSdk is ever lowered, core library desugaring must be enabled or every `LocalDate`/`Instant` breaks at runtime on old devices.
- Room `@TypeConverters` live in `core/data/Converters.kt`. `LocalDate` ⇄ ISO `String`, `Instant` ⇄ epoch millis `Long`, `LocalTime` ⇄ ISO `String`, enums ⇄ `String`. Keep new converters there, not scattered per feature.
- **WorkManager adds four permissions to the merged manifest transitively**, even though `AndroidManifest.xml` declares none: `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`. Verified in the milestone-1 APK. **`INTERNET` is NOT among them**, so PRD section 8's "fully offline" requirement holds — the app cannot reach the network. `ACCESS_NETWORK_STATE` only exists so WorkManager can evaluate network *constraints* the app never sets. If its presence is unwanted on the Play listing later, it can be stripped with `tools:node="remove"` in the manifest; do not do so casually, as WorkManager reads it.
- No emulator or system image is installed on this machine (and no device was attached this session), so Milestone 1 was verified by **compiling a debug APK**, not by launching it. See PROGRESS.md.

## Open Questions Carried From PRD
- ~~Habit frequency: daily/weekly only, or custom day-of-week schedules, for v1?~~ **Resolved 2026-08-28** — schema supports custom days; UI scope for v1 decided in Milestone 2.
- ~~Expense/calorie categories: presets only or user-definable from day one?~~ **Deferred 2026-08-28, no schema cost** — `category` is a `String`; decide the UX in Milestone 4.
- Local backup (DB export/import): v1 or later? — **still open.** Related to PRD Milestone 12 (CSV export, stretch). Not needed before Milestone 10.

Resolve these here once decided, and mirror the resolution into `PRD.md` if it changes scope.
