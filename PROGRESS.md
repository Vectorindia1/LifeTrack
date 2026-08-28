# PROGRESS.md — Build Progress Log

Update this at the end of every Claude Code session. Newest entry on top.
Check the box when a milestone is fully working end-to-end (not just started).

## Milestone Checklist (from PRD section 10)
- [x] 1. Project scaffold + Room DB + navigation shell
- [x] 2. Habit tracker (add/log/streak/chart)
- [x] 3. Dashboard v1 (habits only)
- [x] 4. Expense tracker + dashboard integration
- [x] 5. Calorie tracker + dashboard integration
- [x] 6. Water tracker + dashboard integration
- [x] 7. Goal tracker + dashboard integration
- [x] 8. Diary + dashboard integration
- [x] 9. Notification system (WorkManager, consolidated daily check)
- [x] 10. Settings screen
- [ ] 11. Polish (theming, empty states, animations, dark mode)
- [ ] 12. (Stretch) CSV export

---

## Session Log

### Session 12 — 2026-08-29
**Notification diagnostics**, in response to "notifications aren't working."

I have no device access, so rather than guess at a phantom bug, I reviewed the scheduling/worker code (found it structurally sound) and made the actual failure mode — most likely the *complete lack of visibility* into scheduling and permission state — diagnosable and partly fixed:

**Done:**
- Extracted `DigestRunner` — the digest-assembly-and-post logic shared by the scheduled `DailyDigestWorker` and a new manual test action, so they can never drift.
- `DailyDigest.build()` gained an `ignoreTiming` flag: skips the "has this reminder time passed today" gate for manual testing, while still respecting per-feature enable/disable.
- Settings' Reminders card now shows: whether the notification permission is actually granted (with a direct link to the system settings page if not), the next scheduled check's date and time, and a **"Send a test notification now"** button that runs the real pipeline immediately.

**Why I believe this explains most of the complaint:** the default schedule's times are 09:00/14:00/18:00/20:00/20:30/21:30. Install or open the app after 21:30 and the *first* scheduled check is correctly next-day 09:00 — with nothing in the UI saying so, that is indistinguishable from broken. A denied notification permission also fails completely silently by design. Both are now visible in Settings.

**Verified:** `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL, zero warnings, 70/70 tests pass (digest logic behavior unchanged for the default `ignoreTiming = false` path — no test needed to change).

**What I need from you next:** open Settings, check what the notification status row says, and try "Send a test notification now". If a notification appears, the pipeline is proven correct and the original issue was scheduling visibility (now fixed) or plain waiting for the right time. If nothing appears even from the test button, that is a real bug — tell me and I'll dig further with that new information. If the test button works but the *scheduled* check still never fires on its own after a day, that likely points to OEM battery/background-app restrictions, which is a phone-settings problem, not an app-code one.

**Also asked about:** a "period tracking" column/feature. This is a genuinely new tracker beyond the original PRD's six (habit/goal/expense/calorie/water/diary) and its scope varies hugely depending on what's wanted (a simple start-date log vs. a full cycle tracker with predictions and symptoms) — asked the user to clarify before building schema for it, rather than guess and build the wrong thing.

### Session 11 (follow-up) — 2026-08-29
**First real screenshot of the app, and the first real bug it caught.**

The dashboard's water quick-add buttons rendered as huge vertical pills — "+250 ml" wrapped one character per line because three elements (two quick-add buttons + a "Custom" button) were fighting for width in a narrow card column, and the button label had no line limit to stop it from wrapping instead of failing gracefully.

**Fixed:**
- Dropped the "Custom" button from the dashboard's water row (`showCustom = false`) — PRD 7.1 only asks for the two quick-add buttons there, so the third element was never justified.
- Made the dashboard's water card clickable to `Destination.Water`, since removing Custom would otherwise have left no way to reach the full Water screen from the dashboard — a real navigation gap, not just a cosmetic fix.
- Added `maxLines = 1` + ellipsis to both quick-add button labels as a backstop against the same failure recurring elsewhere.

**Verified:** `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL, zero warnings, 70/70 tests pass.

**Still true:** this is the *first* screenshot the app has ever produced, all the way back to milestone 1. Every other screen — Habits, Expenses, Calories, Goals, Diary, Settings — has the identical risk profile (elements crammed into a shared-width row, no line limits) and has never been looked at. Treat any of them rendering oddly as expected-until-proven-otherwise, not a surprise.

**Next up:** keep looking at real screens. Report anything else that renders wrong the same way — a screenshot and a one-line description is enough for me to diagnose.

### Session 11 — 2026-08-28
**Visual redesign** (part of milestone 11 — Polish), prompted by the user sharing a reference UI mockup and saying "the UI is not that good."

**Done this session:**
- **Design token layer**: `Accents` (per-feature light/dark color pairs) and `IconBadge`/`StatTile` shared components in `core/ui/theme/` and `core/ui/`.
- **Dark theme overhauled** — near-black background, a clearly distinct card surface color, rounder corners app-wide.
- **Dashboard rebuilt**: time-of-day greeting (optionally by name), icon-badged cards for every tracker, accent-colored progress bars/rings, **deleted the now-redundant `MoreTrackers` chip row** (Goals/Calories/Water have had real dashboard cards since milestones 6–7; the chips were pure duplicate navigation left over from milestone 3).
- **Habit rows** gained a Mon–Sun week-dot strip showing this week's completions at a glance.
- **Goal cards** gained a deterministic per-goal icon + accent color.
- **Diary** gained a "today's summary" stat-tile row (habits/spent/calories/water), surfacing the same numbers already used for the auto-prefill, just more visually.
- **Charts** gained a gradient area fill under lines and rounded bar tops; every chart and the progress ring now source color from the new accent tokens instead of raw MaterialTheme roles.
- **Settings and Expense/Calorie/Water screens** got icon-badge headers matching the same language.
- **New preference + migration**: `displayName` (nullable, v2→v3 Room migration), editable in Settings, feeding the dashboard greeting.

**Decisions made with the user before starting (not guessed):**
- Kept the current 5-tab bottom nav rather than adopting the reference's center-FAB layout — restyle only, no navigation change.
- Added a `displayName` preference for a personalized greeting, rather than a generic one.

**Verified:**
- `./gradlew assembleDebug testDebugUnitTest` → **BUILD SUCCESSFUL**, **zero warnings**.
- **70/70 unit tests pass** (69 previous + 1 new for the time-of-day greeting bucket function), via `--rerun-tasks`.
- Migration v2→v3 checked against the exported schema: `displayName` is `TEXT`, nullable — exactly what `ALTER TABLE ... ADD COLUMN displayName TEXT` produces.
- String resources audited both ways — clean.
- Caught and fixed three of my own mistakes before this ever reached you: fully-qualifying extension-property icons instead of importing them (Kotlin doesn't resolve those by qualification), a duplicated `@Composable` annotation left over from a batch edit, and a genuinely duplicate `import Icons` line.

**Still not verified — and now more than ever:**
- **None of this has been seen rendered.** Colors, spacing, icon sizing, the week-dot strip, the gradient chart fill — all compile-correct, none confirmed to actually look good on a screen. This entire session's work was "make the UI better" and the UI has not been looked at once.
- **This is the top priority for your next test pass**, more than any previous milestone's caveat.

**Known issues / things to watch:**
- Deliberately did not build a true 2-column dashboard grid (see MEMORY.md) — variable-height cards in a fixed grid risk ugly uneven rows that only a device screen can catch.
- All version pins and toolchain notes from session 1 still apply.

**Next up:**
- **See it on a device first.** After that: finish milestone 11 (remaining polish — animations beyond the ring, empty-state consistency) and milestone 12 (CSV export, stretch).

### Session 10 — 2026-08-28
**Milestone 10 complete.** Settings screen, and the project's first Room migration.

**Done this session:**
- **Room migration v1 → v2** adding `app_preferences` (theme, water quick-add increments). Schema export has been on since milestone 1 precisely so this would be writable rather than guesswork.
- `SettingsScreen` covering all of PRD 7.9 except CSV export (milestone 12): daily targets, quick-add amounts, reminder times with per-category toggles, habit management, expense category management, and a light/dark/system theme picker.
- **Theme preference is applied app-wide** — `MainActivity` collects it and drives `LifeTrackTheme`, so dark mode now works independently of the system setting.
- Water quick-add amounts became configurable, replacing the hardcoded constants everywhere they were used.
- Expense category rename via bulk row update, with the entry count shown so the blast radius is visible.
- Reminder edits re-arm the scheduler immediately.
- **Deleted `PlaceholderScreen`** — every screen in the app is now real.

**Verified:**
- `./gradlew assembleDebug testDebugUnitTest` → **BUILD SUCCESSFUL**, **zero warnings**.
- **69/69 unit tests pass** via `--rerun-tasks`.
- **The migration SQL was checked byte-for-byte against the exported schema.** My first attempt wrote `id INTEGER NOT NULL PRIMARY KEY` where Room expects `PRIMARY KEY(\`id\`)` — a difference that risks a crash on upgrade. It now copies `2.json`'s `createSql` verbatim, verified identical by script.
- String resources audited — everything unused removed.

**Decisions recorded in MEMORY.md:**
- Room over DataStore for preferences, and why.
- Migration SQL must be copied verbatim from the exported schema; the procedure for next time.
- The calorie/water target shortcuts were **kept** rather than removed — fewer taps, same repository, so they cannot disagree.
- Categories are renamed by bulk row update; deletion deliberately not offered.

**Known issues / things to watch:**
- All version pins from session 1 still apply.
- **The migration has never actually run.** It is verified correct against the schema, but no v1 database has been upgraded to v2 on a real device. If you already installed an earlier build, the next install exercises it for real — that is the one thing worth watching when you test.

**Next up — Milestone 11: Polish**
1. Empty states exist for habits, expenses and goals; calories, water and diary could use the same treatment.
2. Dark mode now has a real toggle and has **never been looked at** — the palette was written blind in milestone 1.
3. Animations: the water ring already animates; transitions elsewhere are abrupt.
4. Dashboard card ordering deserves judgement on a real screen now that all six sections exist.
5. Milestone 12 (CSV export) remains a stretch goal.

### Session 9 — 2026-08-28
**Milestone 9 complete.** Consolidated daily notification digest via WorkManager.

**Done this session:**
- `DailyDigest` — the whole decision of *what to say* as pure functions, so notification **content** is unit-testable even though the job fires once a day.
- `DailyDigestWorker` — reads every tracker, builds one digest, posts one notification, then schedules its own successor.
- `DigestScheduler` — a chain of one-shot workers rather than `PeriodicWorkRequest`, because the check times are irregular times of day.
- `Notifier` — one fixed notification id, `setOnlyAlertOnce`, low-importance channel, taps through to the app.
- `NotificationSettingsRepository` over the row set seeded back in milestone 1.
- `POST_NOTIFICATIONS` permission plus a first-launch runtime request.

**Verified:**
- `./gradlew assembleDebug testDebugUnitTest` → **BUILD SUCCESSFUL**, **zero warnings**.
- **69/69 unit tests pass** — 14 new ones covering the digest: nothing unmet produces no notification, features stay silent before their reminder time, a disabled feature contributes nothing, everything unmet consolidates into ONE list of five concerns rather than five notifications, the calorie under-threshold, the water pace ramp, and the scheduling roll-over to tomorrow.
- APK inspected: `POST_NOTIFICATIONS` merged, and **still no `INTERNET`** — the offline guarantee holds.

**How the PRD's own tension was resolved:**
PRD 7.8 demands ONE notification but lists six default check times, while PRD 8 caps at "~3 pushes/day". A single fixed notification id means checks *update* one notification rather than stacking, and `setOnlyAlertOnce` means only the first alerts. Six check times, one notification, one interruption. Reasoning recorded in MEMORY.md.

**Still not verified — and this milestone is the least verifiable yet:**
- Scheduling **cannot** be proven by compiling. The digest content is tested; whether WorkManager actually fires at 20:00 tomorrow is not, and cannot be without running it.
- Also unverified on a device: the runtime permission dialog, the notification's appearance, and whether the chain survives a reboot.
- **To test quickly:** change a reminder time to a couple of minutes ahead once Settings lands (milestone 10), or temporarily shorten the delay in `DigestScheduler`.

**Decisions recorded in MEMORY.md:**
- How the one-notification guarantee is structurally enforced.
- Chained one-shot workers, rescheduling in `finally`, and why the worker never returns `retry`.
- The digest thresholds (calories under 80%, water on an 08:00–22:00 ramp) — chosen, not derived from the PRD, which states triggers but no numbers.
- `POST_NOTIFICATIONS` is the only permission, and nothing is gated on it.

**Known issues / things to watch:**
- All version pins from session 1 still apply.
- Calorie and water target dialogs still duplicate what Settings will own next.

**Next up — Milestone 10: Settings screen**
1. Daily targets for calories and water — **and move the two temporary dialogs there**, deciding whether to keep the shortcuts.
2. Reminder times and per-category enable/disable, writing to `notification_settings`; call `DigestScheduler.scheduleNext` after saving.
3. Manage habits and expense categories.
4. Light/dark theme toggle (currently follows the system).
5. CSV export is milestone 12 and explicitly a stretch goal.

### Session 8 — 2026-08-28
**Milestone 8 complete.** Diary, and with it the PRD 7.1 dashboard is fully populated.

**Done this session:**
- `DiaryStreak` + `DaySummary` as pure, testable logic.
- `DiaryRepository` — the upsert carries the existing row's id, so saving twice in a day updates rather than colliding with the unique-date index.
- `DiaryViewModel` — the one component that reads from **every** other tracker, because PRD 7.7's summary line quotes them.
- `DiaryScreen`: streak header, month calendar, 5 emoji moods, text editor, save and delete.
- `MonthCalendar` — hand-built month grid marking which days have entries. Material 3's `DatePicker` can't do that, and that is the whole point of the view.
- **Auto-prefilled summary line** ("3/4 habits done, ₹450 spent, 1.8L water") seeded into the draft only.
- **Dashboard diary card** with streak and a "write today's entry" prompt.

**Verified:**
- `./gradlew assembleDebug testDebugUnitTest` → **BUILD SUCCESSFUL**, **zero warnings**, clean first attempt.
- **55/55 unit tests pass** (16 habit + 9 expense + 5 calorie + 6 water + 11 goal + 8 new diary), via `--rerun-tasks`.
- String resources audited — three unused strings removed.

**The trap avoided, worth knowing about:**
- Implementing "auto-prefilled" literally — writing the summary into the entry — would have created a **junk diary entry for every day the diary was merely opened**, silently inflating the diary streak and filling the calendar with entries the user never wrote. The prefill lives in the draft only and nothing persists until Save.

**Decisions recorded in MEMORY.md:**
- Prefill never auto-saves; the summary is null when nothing was tracked.
- Diary streak uses the same grace rule as habits — deliberately identical.
- The calendar is hand-built, and why.
- The diary reuses other repositories rather than adding queries, so its numbers can't disagree with the tracker screens.

**Known issues / things to watch:**
- All version pins from session 1 still apply.
- Calorie and water target dialogs still duplicate what Settings will own in milestone 10.
- The dashboard now has six cards. It scrolls, which is fine since you relaxed that constraint, but the ordering has never been judged on a real screen.

**Next up — Milestone 9: Notification system (WorkManager)**
1. **One consolidated daily digest, never per-tracker spam** — this is an explicit product requirement (PRD 7.8, and flagged in MEMORY.md since session 0). Do not add per-event notifications.
2. `notification_settings` is already seeded with the six default reminder rows from PRD 7.8, including water's two times.
3. Reuse `GoalProgress.isDeadlineNear(3)` — the deadline window is already implemented and tested.
4. Needs `POST_NOTIFICATIONS` (API 33+) and a runtime permission request — the first permission this app has asked for.
5. WorkManager is already a dependency and its manifest entries are already merged in.

### Session 7 — 2026-08-28
**Milestone 7 complete.** Goal tracker plus dashboard integration. Also relaxed the no-scrolling constraint.

**Done this session:**
- **Relaxed the no-scrolling rule** on your instruction. PRD 7.1, PRD 8 and CLAUDE.md UX principle 2 amended in place with dated notes. No code was needed — every feature screen was already a `LazyColumn` and the dashboard already scrolled. What changed is the constraint that had been imposing a compaction budget on the dashboard. The first screenful must still carry the essentials and the one-tap actions.
- `GoalProgress` — goal maths as pure functions (fraction, days remaining, overdue, deadline-near, urgency ordering), so the deadline arithmetic is unit-testable.
- `GoalRepository` + `GoalViewModel`.
- `GoalScreen`: Active and Completed sections, per-goal progress bar, days-remaining label that distinguishes due-today / due-tomorrow / overdue, **+1 increment and Set-value** update paths (PRD 7.3), and delete.
- `AddGoalSheet`: name, target value, unit, and an optional deadline via a Material 3 date picker.
- **Dashboard goals card** — top 3 active goals with progress bars and a "See all" that appears only when there are more (PRD 7.1).

**Verified:**
- `./gradlew assembleDebug testDebugUnitTest` → **BUILD SUCCESSFUL**, **zero warnings**, clean on the first attempt.
- **47/47 unit tests pass** (16 habit + 9 expense + 5 calorie + 6 water + 11 new goal), via `--rerun-tasks`.
- String resources audited both ways — clean.

**Decisions recorded in MEMORY.md:**
- A finished goal is never overdue; an undated goal is never overdue or "near".
- `daysRemaining` is intentionally unclamped — negative means overdue and the UI relies on it.
- Completed goals stay visible in their own section rather than vanishing.
- `isDeadlineNear(3)` already implements PRD 7.8's goal reminder window — milestone 9 must reuse it.
- Goals join the dashboard's outer combine (not `DayData`) because they are not day-scoped; diary in milestone 8 *is* day-scoped and belongs in `DayData`.

**Known issues / things to watch:**
- All version pins from session 1 still apply.
- Calorie and water target dialogs still duplicate what Settings will own in milestone 10.
- The date picker converts the selected day via UTC, which is how Material 3 reports it. Worth a look on a device when you test, since date pickers are a classic off-by-one source.

**Next up — Milestone 8: Diary + dashboard integration**
1. `DiaryRepository` + `DiaryViewModel`; the `diary_entries` table has a unique index on date, so one entry per day is already enforced.
2. Entry editor with the 5 emoji moods from PRD 7.7.
3. **Auto-prefilled summary line** ("3/4 habits done, ₹450 spent, 1.8L water") — this needs data from habits, expenses, water and calories, so it reuses the repositories rather than adding queries.
4. Calendar view to browse past entries.
5. Dashboard: diary streak indicator and a "write today's entry" prompt.
6. Diary is day-scoped, so it goes in `DayData`.

### Session 6 — 2026-08-28
**Milestone 6 complete.** Water tracker with a real progress ring, plus dashboard quick-add.

**Done this session:**
- `core/ui/ProgressRing` — a Compose `Canvas` ring with animated fill. Deliberately not a charting component; see MEMORY.md.
- `WaterRepository` + `WaterViewModel`, reusing the local-day helpers.
- `WaterScreen`: progress ring with amount inside, +250/+500/custom quick-add, **Undo** for mis-taps, a 7-day bar chart, today's entries with delete, and target editing.
- **Dashboard water card with +250/+500 inline** — logging a drink is now one tap from the home screen, the strictest case of PRD section 8's rule.
- Restructured `DashboardViewModel` around a private `DayData` holder after hitting `combine`'s five-flow ceiling.

**Verified:**
- `./gradlew assembleDebug testDebugUnitTest` → **BUILD SUCCESSFUL**, **zero warnings**.
- **36/36 unit tests pass** (16 habit + 9 expense + 5 calorie + 6 new water), via `--rerun-tasks`.
- String resources audited both ways — clean.
- One compile error en route (`getValue` import missing for a `by` delegate) — the same slip as in milestone 1, fixed.

**Decisions recorded in MEMORY.md:**
- Progress rings are plain Compose, never a chart library.
- Water is forgiving where calories are strict — water has no "over" state, calories do. Both pinned by tests.
- Dashboard aggregation goes through `DayData`; milestones 7 and 8 must add fields there, not new combine arguments.

**Known issues / things to watch:**
- All version pins from session 1 still apply.
- **The dashboard is now five cards** (habits, water, calories, spend, more-trackers) and PRD 7.1 wants no scrolling. Goals and diary are still to come. This will almost certainly need compacting in milestone 11 — and it has never been seen on a real screen.
- Calorie and water target dialogs both duplicate what Settings will own in milestone 10.

**Next up — Milestone 7: Goal tracker + dashboard integration**
1. `GoalRepository` + `GoalViewModel`; the `goals` table and DAO already exist.
2. Add goal (name, target value, unit, deadline), progress bar per goal, days remaining.
3. Update progress by manual entry or increment.
4. Dashboard: top 2–3 active goals with a "see all", per PRD 7.1.
5. Deadline notifications are milestone 9, not this one.

### Session 5 — 2026-08-28
**Milestone 5 complete.** Calorie tracker plus dashboard progress bar. Also refactored charting.

**Done this session:**
- **Extracted `core/ui/chart/Charts.kt`** — `SimpleBarChart` and `SimpleLineChart`. All Vico imports now live in one file; the habit and expense charts were refactored onto it and calorie's chart is the first built directly on it. This was done before writing a fourth copy of the same boilerplate.
- `CalorieRepository` + `CalorieViewModel`, reusing the local-day helpers written for expenses.
- `CalorieScreen`: eaten-vs-target header with a progress bar that turns red over target, a 7/30-day line chart, today's entries with delete, and a target-editing dialog.
- `AddCalorieSheet`: food name + calories, manual entry only (PRD 3 rules out scanning for v1).
- **Dashboard now has the calorie progress bar** from PRD 7.1, alongside habits and spend.

**Verified:**
- `./gradlew assembleDebug testDebugUnitTest` → **BUILD SUCCESSFUL**, **zero compiler warnings**.
- **30/30 unit tests pass** (16 habit + 9 expense + 5 new calorie), confirmed with `--rerun-tasks`.
- The refactor of the two existing charts is covered by the build plus the untouched habit/expense tests.
- String resources audited both ways — clean.

**Decisions recorded in MEMORY.md:**
- All charting goes through `core/ui/chart`; never import Vico directly in a feature.
- Calorie progress clamps to 1.0; overshoot is carried by a negative `remaining`, and exactly-on-target is not "over".
- The calorie target dialog is a temporary home — PRD 7.9 puts targets in Settings (milestone 10).

**Known issues / things to watch:**
- All version pins from session 1 still apply.
- **The dashboard is now four cards** (habits, calories, spend, more-trackers) with water, goals and diary still to come. PRD 7.1 wants it all visible **without scrolling** — this budget is being spent without ever seeing a real screen, and something will likely have to become more compact.
- The calorie target dialog duplicates what Settings will own in milestone 10.

**Still not verified:**
- The app has not been run. Per your call, testing happens at the end rather than per milestone — noting it here only so the record is accurate. Room has never opened at runtime and four charts have never been drawn.

**Next up — Milestone 6: Water tracker + dashboard integration**
1. `WaterRepository` + `WaterViewModel`; the `water_goal` row is already seeded at 2500 ml.
2. Quick-add +250 ml / +500 ml / custom.
3. Daily target **progress ring** — plain Compose `Canvas`, not a chart library.
4. Weekly bar chart via `SimpleBarChart`.
5. Dashboard ring with the quick-add buttons inline, which is the strictest ≤2-tap case in PRD 7.1.

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
