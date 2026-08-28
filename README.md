# LifeTrack

A single Android app for habits, goals, expenses, calories, water, a daily diary, and period tracking — built because switching between five different tracker apps is its own source of friction, and low motivation doesn't need more of that.

Fully offline. No account, no cloud sync, no ads, no analytics. Everything lives in a local database on your phone, and you can export all of it to a file at any time.

## Download

Grab the latest APK from **[Releases](../../releases/latest)** and sideload it (`adb install app-debug.apk`, or copy it to your phone and open it — Android will ask you to allow installs from that source once).

Requires Android 8.0 (API 26) or newer.

## What it does

**Dashboard** — everything that matters today, on one screen: today's habits with one-tap checkboxes, a water ring with quick +250ml/+500ml buttons, calorie progress, today's spend, your top active goals, and a diary streak — all scrollable, all reachable without hunting.

**Habits** — daily, weekly ("N times a week"), or specific-days-of-the-week schedules; streaks that give today a grace period but don't forgive a missed yesterday; a weekly/monthly completion-rate chart.

**Goals** — a target value and a deadline, updated by a quick +1 or a direct value entry; a days-remaining label that knows the difference between "due tomorrow" and "12 days overdue"; finished goals stay visible instead of vanishing.

**Expenses** — quick-add with preset or custom categories, day/week/month totals, a breakdown by category, and a spend-over-time chart.

**Calories & Water** — manual logging against a daily target, with history charts; water gets a progress ring and configurable quick-add amounts.

**Diary** — one entry a day with a mood tag, a month calendar to browse past entries, and a same-day summary line ("3/4 habits done, ₹450 spent, 1.8L water") pre-filled into a blank entry so the page is never empty when you open it — without ever writing anything until you actually hit Save.

**Period tracker** — a simple, neutrally-worded log of start dates, with history and an average cycle length. No prediction, no symptom tracking — just a record.

**Notifications** — one consolidated daily digest covering whatever's still unmet, not a notification per tracker. A separate, off-by-default interval water reminder is available for anyone who wants a more frequent nudge.

**Home screen widget** — today's habit count and water progress, with a one-tap +250ml button that logs water without opening the app.

**Backup & restore** — export everything to a single file from Settings, and restore it later. The one thing that should survive a reinstall, an update, or a new phone.

**Settings** — daily targets, reminder times (each independently toggleable), habit and category management, light/dark/system theme, and a currency picker (not tied to your phone's locale).

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3) for the UI
- **Room** for local persistence — 12 tables, versioned with real migrations from day one
- **WorkManager** for the notification digest and the interval water reminder
- **Jetpack Glance** for the home screen widget
- **Vico** for charts (bar and line only — no dense multi-axis dashboards)
- **MVVM + Repository**, feature-first package structure, manual dependency injection (no Hilt — this app isn't complex enough to need it)
- Min SDK 26, no backend, no network permission at all

## Architecture, briefly

Each tracker is its own package (`habit/`, `goal/`, `expense/`, `calorie/`, `water/`, `diary/`, `period/`) with its own `data/`, `ui/`, and `viewmodel/`. Business logic that isn't trivial — streak calculation, goal deadline math, the notification digest's rules, cycle-length averaging — lives as pure, unit-tested functions with no Android dependencies, so the parts most likely to have a subtle bug are the parts actually covered by tests.

`core/` holds what's shared: the Room database and its migrations, the app-wide theme and design tokens, navigation, and a small manual DI container.

## Building from source

```bash
git clone https://github.com/Vectorindia1/LifeTrack.git
cd LifeTrack
./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`. No signing setup, API keys, or backend config needed — the whole app builds from a clean checkout.

## Project history

This was built incrementally, milestone by milestone, with a working app kept buildable at every step — see [`PROGRESS.md`](PROGRESS.md) for the full session-by-session log, [`PRD.md`](PRD.md) for the product spec (including everything added after the original plan), and [`MEMORY.md`](MEMORY.md) for the reasoning behind the less-obvious decisions along the way.

## What's not here

No CSV export yet (a stretch goal from the original spec — full JSON backup covers the "don't lose my data" need in the meantime). No cloud sync, by design. No food-photo or receipt scanning. No social features. This is a personal tool, not a platform.

## License

[MIT](LICENSE)
