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

## 11. Open Questions
- Should habit "frequency" support custom day-of-week schedules in v1, or just daily/weekly?
- Should calorie/expense categories be user-definable from day one, or ship with presets only?
- Local backup (export/import DB file) — v1 or later?
