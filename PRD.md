# PRD — LifeTrack (All-in-One Habit/Goal/Expense/Calorie/Water/Diary Tracker)

## 1. Problem Statement
The user has low motivation and a shrinking attention span. Existing tracker apps are single-purpose, require too many taps, and app-switching itself becomes a barrier to consistency. The goal is ONE Android app that consolidates habit tracking, goal tracking, expense tracking, calorie tracking, water tracking, and a daily diary — with minimal friction per log and simple, non-overwhelming visualizations.

## 2. Goals
- Reduce logging to 1–2 taps wherever possible.
- Single dashboard shows the whole day at a glance — no scrolling, no digging.
- Gentle, consolidated notifications — never notification spam.
- Simple graphs only (bar/line/progress ring) — no complex analytics dashboards.
- Fully offline, local-first (no backend, no login) for v1.

## 3. Non-Goals (v1)
- No social/sharing features.
- No cloud sync / multi-device support.
- No AI-based food recognition or receipt scanning.
- No monetization/ads.

## 4. Target Platform
- Android only, min SDK 26 (Android 8.0+), target latest stable SDK.
- Single user, single device, local storage only.

## 5. Tech Stack
| Layer | Choice | Why |
|---|---|---|
| Language | Kotlin | Standard modern Android |
| UI | Jetpack Compose | Less boilerplate, easier iteration |
| Local DB | Room (SQLite) | Offline-first, simple relations |
| Charts | Compose-native charting (e.g. Vico) or MPAndroidChart | Simple bar/line/ring charts |
| Background jobs | WorkManager | Survives reboots/app kill, reliable daily scheduling |
| Architecture | MVVM + Repository pattern | Standard, testable, keeps UI dumb |
| DI | Hilt (optional, add only if complexity warrants) | Simplifies ViewModel/Repo wiring |

## 6. Core Data Model

```
Entry (id, type, timestamp, value, note)
  type = "habit" | "goal" | "expense" | "calorie" | "water" | "diary"

Habit (id, name, target_frequency, streak_count, created_at)
HabitLog (id, habit_id, date, completed)

Goal (id, name, target_value, current_value, unit, deadline, created_at)

Expense (id, category, amount, note, timestamp)

CalorieLog (id, food_name, calories, timestamp)
CalorieGoal (id, daily_target)

WaterLog (id, ml_amount, timestamp)
WaterGoal (id, daily_target_ml)

DiaryEntry (id, date, text, mood)  -- one per day

NotificationSettings (id, feature_type, enabled, reminder_time)

PeriodLog (id, start_date, end_date)  -- added 2026-08-29, see section 7.10
```

## 7. Features & Screens

### 7.1 Dashboard (Home)
- Today's habit checkmarks (tap to toggle)
- Calorie progress bar (eaten / target)
- Water progress ring (drunk / target, +250ml / +500ml quick buttons)
- Today's spend total
- Active goals with progress bars (top 2–3, "see all" for rest)
- Diary streak indicator + "write today's entry" prompt
- Scrolling is allowed (revised 2026-08-28); the first screenful must still lead with today's essentials — habit checkmarks and the quick-add actions

### 7.2 Habit Tracker
- Add habit (name, frequency: daily/weekly/custom days)
- Daily checklist view
- Streak counter per habit
- Weekly/monthly bar chart of completion rate

### 7.3 Goal Tracker
- Add goal (name, target value, unit, deadline)
- Update progress (manual value entry or increment button)
- Progress bar per goal, days remaining
- Notification as deadline approaches

### 7.4 Expense Tracker
- Quick-add: amount + category (preset categories + custom)
- Daily/weekly/monthly total
- Bar chart by category, line chart of spend over time

### 7.5 Calorie Tracker
- Quick-add: food name + calorie count (manual entry, no scanning in v1)
- Daily total vs target, progress bar
- Line chart of daily calories over past 7/30 days

### 7.6 Water Tracker
- Quick-add buttons (+250ml, +500ml, custom)
- Daily target progress ring
- Weekly bar chart

### 7.7 Diary
- One text entry per day, optional mood tag (emoji-based, 5 options)
- Auto-prefilled summary line at top of entry (e.g. "3/4 habits done, ₹450 spent, 1.8L water") to reduce blank-page friction
- Calendar view to browse past entries

### 7.8 Notifications (via WorkManager)
Single daily consolidation job checks all trackers and sends ONE notification covering unmet items, not one notification per tracker.

| Trigger | Default Timing |
|---|---|
| Habit(s) not logged today | 8:00 PM |
| Goal deadline within 3 days | Once/day, 9:00 AM |
| Calorie target under/over by evening | 8:30 PM |
| Water intake behind expected pace | 2:00 PM and 6:00 PM (only if behind) |
| Diary not written | 9:30 PM |

All reminder times are user-configurable in Settings. Users can disable any category independently.

### 7.10 Period Tracker (added 2026-08-29, post-v1)
Added by user request, not part of the original v1 scope. Deliberately minimal:
- Log a period's start date. One tap for "today"; a date picker for any other date.
- Optionally set an end date once it's over.
- History list of past periods, with duration where an end date is set.
- Average cycle length shown once two or more periods are logged. **No prediction of a future date** — this is a log with a computed average, not a forecast.
- Dashboard card shows the current cycle day (days since the last logged start) and taps through to the full tracker, consistent with Goals/Calories/Water.
- **Wording throughout is neutral about whose cycle is being tracked.** This app is single-user; that one user might be logging their own periods, or someone else's (e.g. a partner's) on their behalf. Copy avoids "my period" / gendered phrasing for exactly that reason — see MEMORY.md.
- No symptom or flow-intensity tracking in this version.

### 7.11 Home Screen Widget (added 2026-08-29, post-v1)
Added by user request. A "Today" widget showable on the Android home screen:
- Shows today's habit count (done/due) and water progress (drunk/target).
- One-tap **+250ml** button, logging water without opening the app.
- Tapping the rest of the widget opens the app.
- Refreshes immediately after a habit toggle or water quick-add made from the dashboard, plus a 30-minute background refresh floor (the OS-enforced minimum for widget provider updates) as a fallback for changes made elsewhere.
- Built with Jetpack Glance, not classic RemoteViews, for consistency with the rest of the app's declarative-UI style — see MEMORY.md for the exact API surface this was verified against and why compileSdk 35 (Glance's floor) still fits under this project's compileSdk 36 ceiling.

### 7.12 Interval Water Reminder (added 2026-08-29, post-v1)
Added by user request: a recurring "drink water" nudge every N minutes, **separate from and in addition to** the water check already in the consolidated daily digest (7.8).
- **Off by default.** Turned on in Settings, with a configurable interval (15/30/60/90/120 min).
- Fires only during waking hours (reuses 7.8's 08:00–22:00 window) and stops for the day once the water target is met.
- Uses a distinct, audible notification channel — unlike the digest's deliberately quiet channel — so it can actually get attention, per the user's request. Notification includes +250ml/+500ml actions that log water directly.
- **This is a deliberate, acknowledged exception to section 8's "never more than ~3 pushes/day" rule and to 7.8's "one consolidated notification" principle** — made because the user explicitly asked for a repeating alert, not a daily summary. It does not change the digest's behavior; the two systems are independent. See MEMORY.md.
- Implemented as a `PeriodicWorkRequest`, not an `AlarmManager` exact alarm — the latter needs `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`, both subject to Play Store policy review intended for actual alarm-clock apps. The trade-off is the OS may shift the exact firing minute by a few minutes around Doze; acceptable for a "roughly every N minutes" reminder.

### 7.13 Backup and Restore (added 2026-08-29, post-v1)
Added by user request — a safety net against losing data across an app update, reinstall, or device change. Resolves PRD section 9's stretch "CSV export" goal with something more complete: a full-database export, not one tracker's worth of rows.
- **Export**: one JSON file covering every table (habits, habit logs, goals, expenses, calorie logs and target, water logs and target, diary entries, notification settings, app preferences, period logs). Saved wherever the user picks via the standard Android file picker.
- **Import**: reads that same file back and **replaces** everything currently in the app with it. This is explicitly destructive and requires confirmation — it is a restore, not a merge.
- Nothing is changed if the file is unreadable, damaged, or from an incompatible export format — the whole file is parsed and validated before any existing data is touched.
- No account, cloud storage, or network involved — the exported file is just a file, kept wherever the user's phone lets them save one. Consistent with section 8's fully-offline requirement.

### 7.9 Settings
- Set daily targets (calories, water, ml increments)
- Configure notification times / toggle categories
- Manage habit list, expense categories
- Data export (CSV) — stretch goal
- Theme: light/dark

## 8. Non-Functional Requirements
- App cold start under 1.5s on mid-range devices.
- All logging actions (habit check, quick-add expense/calorie/water) complete in ≤2 taps from dashboard.
- Screens may scroll (revised 2026-08-28). Primary content and quick actions must still appear in the first screenful, without the user hunting.
- Local DB only; no network permissions except none needed for v1 (fully offline).
- Notifications must be batchable/consolidated, never more than ~3 pushes/day by default.

## 9. Success Metrics (self-tracked, informal)
- Daily app open rate
- Habit completion streak length
- Diary entries per week
- Subjective: "does opening this app feel effortless?"

## 10. Build Milestones
1. Project scaffold + Room DB + navigation shell
2. Habit tracker (full vertical slice: add/log/streak/chart)
3. Dashboard v1 (habits only)
4. Expense tracker + dashboard integration
5. Calorie tracker + dashboard integration
6. Water tracker + dashboard integration
7. Goal tracker + dashboard integration
8. Diary + dashboard integration
9. Notification system (WorkManager, consolidated daily check)
10. Settings screen
11. Polish: theming, empty states, animations, dark mode
12. (Stretch) CSV export
13. Period tracker (added 2026-08-29, post-v1 — see section 7.10)
14. Home screen widget (added 2026-08-29, post-v1 — see section 7.11)
15. Interval water reminder (added 2026-08-29, post-v1 — see section 7.12)
16. Backup and restore (added 2026-08-29, post-v1 — see section 7.13)

## 11. Open Questions
- Should habit "frequency" support custom day-of-week schedules in v1, or just daily/weekly?
- Should calorie/expense categories be user-definable from day one, or ship with presets only?
- Local backup (export/import DB file) — v1 or later?
