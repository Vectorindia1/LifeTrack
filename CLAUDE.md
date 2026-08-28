# CLAUDE.md — Instructions for Claude Code on this project

This file tells Claude Code how to work on this repo. Read this first, then `PRD.md` for what to build, `PROGRESS.md` for what's already done, and `MEMORY.md` for decisions/context that must persist across sessions.

## Project
**LifeTrack** — Android app combining habit, goal, expense, calorie, and water tracking plus a daily diary, in one minimal app. Full spec in `PRD.md`.

## Tech Stack (do not deviate without updating this file)
- Kotlin, Jetpack Compose (Material 3)
- Room for local persistence
- WorkManager for scheduled notifications
- MVVM + Repository pattern
- Min SDK 26, target latest stable
- Charting library: decide once in Phase 1 and record the choice in `MEMORY.md` — do not mix libraries later

## Working Style
- **Build in vertical slices**, per the milestone order in `PRD.md` section 10. Each milestone should be a working, runnable app — never leave the build in a broken state between sessions.
- **Update `PROGRESS.md` at the end of every session** — mark what's done, what's in progress, what's next. This is the first thing to check when resuming work.
- **Update `MEMORY.md` whenever a non-obvious decision is made** — library choices, schema changes, naming conventions, anything a future session would otherwise have to re-derive or might contradict.
- Prefer small, testable commits over large ones.
- Keep the codebase simple: this is a personal single-user app, not an enterprise product. Avoid over-engineering (no unnecessary abstraction layers, no premature optimization).

## Code Conventions
- Package structure by feature, not by layer:
  ```
  com.lifetrack/
    core/           // shared: theme, navigation, DB setup, common composables
    habit/
    goal/
    expense/
    calorie/
    water/
    diary/
    notification/
    settings/
    dashboard/
  ```
  Each feature package contains its own `data/`, `ui/`, `viewmodel/` as needed.
- Composables: small and single-purpose; prefer composition over large monolithic screens.
- ViewModels expose `StateFlow`, not `LiveData`.
- All strings in `strings.xml` — no hardcoded UI text (supports future localization, and keeps things clean).
- Follow standard Kotlin style (4-space indent, trailing commas in multi-line params).

## UX Principles to Enforce in Every Feature
1. Logging any single data point (habit check, expense, calorie, water) must take ≤2 taps from the dashboard.
2. Screens may scroll (revised 2026-08-28, was "no more than one scroll"). The first screenful must still carry the primary content and the quick actions — don't bury logging behind a scroll.
3. Notifications must be consolidated — one daily digest, not per-tracker spam (see PRD 7.8).
4. Graphs stay simple: bar, line, or progress ring only. No multi-axis or dense dashboards.

## What NOT to Do
- Don't add a backend, login, or cloud sync — this is local-only for v1.
- Don't add ads or analytics SDKs.
- Don't introduce a new charting or DI library without recording the reason in `MEMORY.md`.
- Don't build all six trackers in parallel — follow the milestone order so there's always a working app.

## Session Checklist
At the **start** of a session: read `PROGRESS.md` and `MEMORY.md`.
At the **end** of a session:
1. Update `PROGRESS.md` (what shipped, what's next).
2. Update `MEMORY.md` if any new decisions were made.
3. Leave the app in a buildable, runnable state.
