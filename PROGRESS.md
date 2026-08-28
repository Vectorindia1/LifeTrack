# PROGRESS.md — Build Progress Log

Update this at the end of every Claude Code session. Newest entry on top.
Check the box when a milestone is fully working end-to-end (not just started).

## Milestone Checklist (from PRD section 10)
- [x] 1. Project scaffold + Room DB + navigation shell
- [x] 2. Habit tracker (add/log/streak/chart)
- [x] 3. Dashboard v1 (habits only)
- [x] 4. Expense tracker + dashboard integration
- [ ] 5. Calorie tracker + dashboard integration
- [ ] 6. Water tracker + dashboard integration
- [ ] 7. Goal tracker + dashboard integration
- [ ] 8. Diary + dashboard integration
- [ ] 9. Notification system (WorkManager, consolidated daily check)
- [ ] 10. Settings screen
- [ ] 11. Polish (theming, empty states, animations, dark mode)
- [ ] 12. (Stretch) CSV export

---

## Session Log

### Session 4 — 2026-08-28
**Milestone 4 complete.** Expense tracker plus dashboard integration.

**Done this session:**
- `ExpenseRepository` + `ExpenseViewModel` over the milestone-1 DAO.
- `ExpenseScreen`: total card with a **Today / 7 days / This month** toggle, both PRD 7.4 charts, and a recent-expenses list with delete.
- `AddExpenseSheet`: amount + category chips + optional note. Amount input is filtered to digits and one separator, and Save stays disabled until the amount parses and a category is chosen.
- **Two new Vico charts** — a category bar chart and a spend-over-time line chart. These are the second and third charts in the app and the first use of Vico's line layer.
- `core/ui/Money` — locale-aware currency formatting, plus a compact form for chart axes.
- **Dashboard now shows today's spend**, tappable through to the tracker (PRD 7.1).
- Categories: 8 presets plus a "Custom…" chip with free-text entry, resolving PRD section 11's last open question.

**Verified:**
- `./gradlew assembleDebug testDebugUnitTest` → **BUILD SUCCESSFUL**, **zero compiler warnings**.
- **25/25 unit tests pass** (16 habit + 9 new expense), confirmed with `--rerun-tasks`.
- The new tests cover the things most likely to be wrong and least likely to be noticed: local-day boundaries not being sliced on UTC, month-to-date vs rolling-30, custom categories not duplicating presets, and money formatting.
- String resources audited both ways — nothing missing, nothing dead.

**Still not verified:**
- **The app has never been run — four milestones in.** No device or emulator has been available in any session. Room has never opened at runtime, no screen has rendered, and now *three* Vico charts have never been drawn.
- Chart layout in particular is unproven: the category bar chart truncates labels to 4 characters, which is a guess about spacing that only looking at it can settle.

**Decisions recorded in MEMORY.md:**
- Categories are presets + user-defined, with no category table — a category exists because a row uses it.
- Money is locale-formatted; no hardcoded ₹. Amounts stored with no currency code.
- Expense ranges convert to local time; never slice on UTC.
- WEEK is rolling 7 days, MONTH is calendar month-to-date.

**Known issues / things to watch:**
- All version pins from session 1 still apply.
- `ExpenseViewModel` filters expenses in memory rather than in SQL. Fine at personal scale (the DAO's date-range queries exist for when it is not), but revisit if the list ever gets large.
- The dashboard is accumulating cards. PRD 7.1 wants it all visible **without scrolling**, and that constraint has not been checked on a real screen — it needs looking at once calories, water, goals and diary land.

**Next up — Milestone 5: Calorie tracker + dashboard integration**
1. `CalorieRepository` + `CalorieViewModel`; the `calorie_goal` singleton row is already seeded at 2000 kcal.
2. Quick-add food name + calories.
3. Daily total vs target with a progress bar; line chart over 7/30 days.
4. Dashboard calorie progress bar (PRD 7.1).

### Session 3 — 2026-08-28
**Milestone 3 complete.** Dashboard v1, plus the repo is finally under version control.

**Done this session:**
- **`git init`.** Two commits: the original specs, then milestones 1–2 as one commit (the repo did not exist while that work happened). Milestone 3 is its own commit. `local.properties` and build output are correctly ignored.
- `DashboardViewModel` rewritten around `HabitRepository`: today's due habits, done/due counts, and a one-tap toggle.
- `DashboardScreen` replaces the temporary database card with the real PRD 7.1 habit section — date header, per-habit checkbox rows with streak flames, an all-done note, a first-habit prompt when empty, and chips into Goals/Calories/Water.
- **Logging a habit from the dashboard is now one tap**, satisfying PRD section 8's ≤2-tap rule for the first time.
- **Fixed the stale-date bug** flagged last session: `today` is now reactive state in both dashboard and habit ViewModels, refreshed on resume, so an app left open across midnight rolls over.
- Habit checkboxes now carry accessible labels (they were unlabelled controls next to separate text).
- Removed the `db_status_*` strings and two other strings that went dead.

**Verified:**
- `./gradlew assembleDebug testDebugUnitTest` → **BUILD SUCCESSFUL**, 16/16 tests still passing, **zero compiler warnings**.
- Checked for stale string resources in both directions: nothing referenced-but-missing, nothing dead left behind.

**Still not verified:**
- **The app has never been run — three milestones in.** No emulator or device has been available in any session. Room has never opened at runtime, no Compose screen has ever been rendered, and the Vico chart has never been drawn.
- This is now the single biggest risk in the project. Everything is compile-correct and the pure logic is test-correct, but nothing is *runtime*-correct. Worth installing before milestone 4 adds more on top.

**Decisions recorded in MEMORY.md:**
- The dashboard lists only habits *due today*, so its counter is over due habits, not all habits.
- `today` is reactive state, never a captured constant — the pattern to follow for anything date-based later.
- The milestone-1 database card is gone; its runtime-proof role passes to the real dashboard.

**Known issues / things to watch:**
- Version pins from session 1 still apply (API 37 unavailable; JDK path pinned in `gradle.properties`).
- The dashboard's "More trackers" chips exist because Goals/Calories/Water have no bottom-bar tab. When milestones 5–7 give them real dashboard sections, that row should probably go.

**Next up — Milestone 4: Expense tracker + dashboard integration**
1. `ExpenseRepository` + `ExpenseViewModel` over the existing DAO.
2. Quick-add: amount + category, in as few taps as possible.
3. Daily/weekly/monthly totals; bar chart by category and line chart over time (second and third Vico charts).
4. Today's spend total on the dashboard, per PRD 7.1.
5. **Decision needed:** PRD 11 asks whether expense categories are presets-only or user-definable. The schema keeps `category` a plain String so either works — this is the milestone where it has to be decided.

### Session 2 — 2026-08-28
**Milestone 2 complete.** Habit tracker is a full vertical slice: add → log → streak → chart.

**Done this session:**
- `HabitSchedule` — all scheduling, streak and completion-rate maths as pure functions (no Room, no Android), so it is unit-testable on the JVM.
- `HabitRepository` over the existing DAO; added `observeCompletedLogsBetween` so streaks and charts come from one bounded query (400 days) rather than per-habit fetches.
- `HabitViewModel` — combines habits + logs + chart-window into a single `StateFlow<HabitUiState>`.
- `HabitScreen` replaces the placeholder: today's progress header, one card per habit with a **one-tap** checkbox, per-habit streak subtitle, delete, and a FAB.
- `AddHabitSheet` — modal bottom sheet with name + all three frequency types from PRD 7.2 (Every day / Times per week / Certain days, with a day-of-week chip row and a 1–7 slider).
- `HabitCompletionChart` — **first Vico chart in the app**, a plain bar chart of completion rate with a Weeks(8)/Months(6) toggle.
- Empty state for "no habits yet".
- Wired `HabitRepository` + `HabitViewModel` into `AppContainer` / `AppViewModelProvider`.

**Verified:**
- `./gradlew assembleDebug` → **BUILD SUCCESSFUL**, APK 20 MB, no warnings.
- **`./gradlew testDebugUnitTest` → 16/16 passing** (re-run with `--rerun-tasks` to confirm they actually executed). These cover the streak rules specifically: unscheduled days not breaking custom-day streaks, today being a grace period, yesterday not being one, weekly streaks counting weeks, and completion rate ignoring both the future and days before the habit existed.
- The unit tests are the one part of this project verified by *execution* rather than compilation — everything UI-side is still compile-verified only.

**Still not verified:**
- **The app has still not been run.** No emulator, no device attached this session. Nothing has exercised Room at runtime, and no Compose screen has ever been rendered. The Vico chart in particular has never been drawn — it compiles against the right API, which is not the same as laying out correctly.
- Install and check: add a habit, tick it, confirm the streak reads "1 day streak" and the chart renders after a few days of logs.

**Decisions recorded in MEMORY.md:**
- Habit frequency: all three types ship in the UI (resolves PRD 11 at the UI level). `WEEKLY` reads as "N times per week".
- The full streak ruleset, pinned by tests — the tests are the spec.
- `Habit.streakCount` is a write-through cache; logs are the source of truth. Never render the column directly.
- The chart is aggregate across habits, not per-habit (no habit-detail screen in the PRD).
- The verified Vico 3.2.3 API, including `columnSeries` being deprecated in favour of `columnModel`, and the `javap` trick for reading an API off the artifact instead of guessing.

**Known issues / things to watch:**
- Everything from session 1 still applies (API 37 unavailable → version pins; JDK path pinned in `gradle.properties`).
- `HabitViewModel` takes `today` as a constructor default of `LocalDate.now()`. A long-lived process crossing midnight keeps the stale date until the ViewModel is recreated. Fine for now; worth fixing when the dashboard lands in milestone 3.
- **Still not a git repository.** Two milestones of work are now untracked. Worth `git init` before milestone 3.

**Next up — Milestone 3: Dashboard v1 (habits only)**
1. Replace the milestone-1 "Database" status card with the real PRD 7.1 dashboard.
2. Today's habit checkmarks, toggleable **from the dashboard** — this is where the ≤2-taps-from-dashboard rule gets its first real test.
3. Reuse `HabitSchedule`/`HabitRepository` — no new persistence work needed.
4. Decide how the dashboard behaves with zero habits (prompt vs hidden section).

### Session 1 — 2026-08-28
**Milestone 1 complete.** The app compiles to an installable debug APK.

**Done this session:**

*Scaffold*
- Gradle project created from scratch: root + `:app`, Kotlin DSL, version catalog in `gradle/libs.versions.toml`, wrapper on Gradle 8.14.5.
- Toolchain: AGP 8.13.2, Kotlin 2.4.10, KSP 2.3.11, Java 21, **compileSdk/targetSdk 36, minSdk 26**.
- Package structure by feature exactly as CLAUDE.md specifies (`core/`, `habit/`, `goal/`, `expense/`, `calorie/`, `water/`, `diary/`, `notification/`, `settings/`, `dashboard/`), each with its own `data/` / `ui/` / `viewmodel/`.
- All UI text lives in `strings.xml`; no hardcoded strings in composables.
- Material 3 Compose theme with a hand-written light **and** dark palette (`core/ui/theme/`). Dynamic color is wired but off by default.
- Adaptive launcher icon (vector, no PNGs).
- Manual DI via `AppContainer` on `LifeTrackApplication` — no Hilt yet (PRD 5 says optional; reasoning in MEMORY.md).

*Room DB — all 10 entities from PRD section 6*
- `habits`, `habit_logs`, `goals`, `expenses`, `calorie_logs`, `calorie_goal`, `water_logs`, `water_goal`, `diary_entries`, `notification_settings`.
- One DAO per feature, all reads exposed as `Flow`.
- `Converters` for `Instant` ⇄ epoch millis and `LocalDate`/`LocalTime` ⇄ ISO strings (ISO so date columns sort and `BETWEEN`-compare correctly). Enums stored by name.
- Schema export is on — `app/schemas/…/1.json` is committed-ready and makes future migrations writable rather than guesswork. **Verified: 10 tables, version 1.**
- Seeded on first create: the two singleton target rows (2000 kcal, 2500 ml) and six reminder rows at PRD 7.8's default times.
- Indices where they will actually be used: unique `(habitId, date)` so a dashboard habit tap is one upsert, unique `date` on diary (one entry per day), timestamp indices on the log tables.

*Navigation shell*
- Single `NavHost` with all 9 destinations; bottom bar shows 5 (Dashboard, Habits, Expenses, Diary, Settings), with Goals/Calories/Water reached from dashboard cards — a Material 3 nav bar should hold 3–5 items. Rationale in MEMORY.md.
- Tab navigation pops to the start destination with `saveState`/`restoreState`, so the back stack cannot grow one entry per tab tap.
- Every tracker has a placeholder screen naming the milestone that will build it.
- The dashboard is a real MVVM slice (`DashboardViewModel` → `StateFlow` → `collectAsStateWithLifecycle`) that reads live counts and the seeded targets out of Room. It exists to prove the DB stack works end-to-end and **will be replaced wholesale by PRD 7.1's real dashboard in milestone 3.**

*Decisions recorded in MEMORY.md*
- **Charting library: Vico** (not MPAndroidChart) — Compose-native, M3 theme-aware so dark mode is free, actively maintained. Full reasoning + 3.x API gotchas recorded there. Declared in the build now; first chart is milestone 2.
- `Entry` from PRD section 6 is **conceptual, not a table** — confirmed with you.
- Habit frequency schema supports `CUSTOM_DAYS` + a day-of-week bitmask now, so adding custom schedules later needs no migration; v1 UI can still be daily/weekly only.
- Expense `category` stays a plain `String`, so presets-vs-user-defined stays open with no schema cost.

**Verified:**
- `./gradlew assembleDebug` → **BUILD SUCCESSFUL**, `app/build/outputs/apk/debug/app-debug.apk` (19 MB).
- APK inspected, not just built: package `com.lifetrack`, minSdk 26, targetSdk 36, launcher activity present, debug-signed and installable.
- **No `INTERNET` permission** — PRD section 8's offline requirement holds.
- Room schema JSON generated and checked: all 10 tables with the expected columns.

**Not verified — read this before assuming milestone 1 is fully proven:**
- **The app has not been launched.** No emulator/system image is installed on this machine and no device was attached, so this is verified-compiles, not verified-runs. Nothing has executed `LifeTrackDatabase`'s seed callback at runtime.
- First thing next session (or now, on your device): `adb install -r app/build/outputs/apk/debug/app-debug.apk`, open it, and confirm the dashboard's Database card reads "10 tables" with targets `2000 kcal · 2500 ml`. That single card passing is the runtime proof that Room opened, seeded and is readable.

**Known issues / things to watch:**
- **API 37 is not published yet**, so compileSdk is capped at 36. That in turn forced Compose BOM to `2026.06.01` (not the newest `2026.08.00`) and Vico to `3.2.3` (not `3.3.0`) — both newer releases declare `minCompileSdk=37` and hard-fail the AAR metadata check. These five versions must move together when 37 ships; see MEMORY.md for the exact upgrade order.
- System default JDK on this machine is 25, which AGP 8.x rejects. `gradle.properties` pins `org.gradle.java.home` to JDK 21. That path is machine-specific and will need changing on another machine.
- Harmless build warning: "only understands SDK XML versions up to 3 but ... version 4 was encountered", from SDK packages installed by a newer `cmdline-tools`.
- **Not a git repository yet.** CLAUDE.md asks for small, testable commits, but `git init` has not been run — nothing is under version control. Worth doing before milestone 2.

**Next up — Milestone 2: Habit tracker (full vertical slice)**
1. `HabitRepository` over the existing `HabitDao`, plus `HabitViewModel`.
2. Add-habit flow (name + frequency; daily/weekly in the UI — the schema already holds custom days).
3. Daily checklist with a ≤2-tap toggle, using the `(habitId, date)` upsert.
4. Streak calculation. Decide and record: does a missed non-scheduled day break a weekly habit's streak?
5. First Vico chart — weekly/monthly completion rate. Use the pinned 3.2.3 docs; 1.x/2.x examples online will not compile.

**Open question for you before milestone 2:** PRD 7.2 says "daily/weekly/custom days" but PRD 11 leaves custom day-of-week as an open v1 question. The schema supports it; should the milestone-2 **UI** offer custom days, or ship daily/weekly only and revisit later?

---

### Session 0 — [date]
**Status:** Not started. Repo initialized with PRD.md, CLAUDE.md, PROGRESS.md, MEMORY.md.
**Next up:** Milestone 1 — project scaffold.

<!--
Template for future entries:

### Session N — [date]
**Done this session:**
-

**Currently in progress / partially done:**
-

**Known issues / bugs:**
-

**Next up:**
-
-->
