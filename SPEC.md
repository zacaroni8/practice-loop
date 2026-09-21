# SPEC.md — Practice Loop

## Overview

A Java desktop app that helps someone stick with practicing a tedious-but-worthwhile skill, by tracking practice sessions and nudging the user back in before the habit quietly lapses. Built by Zac as his internship project.

## Problem

Many times I have forgotten to do things or waited too long to. Many things had obvious deadlines that were constantly given to me that I don't finish either way. Practice Loop will help solve that problem, and help me do things in sessions while giving rewards. David can also use this app to help reinforce habits or schedules due to said rewards.

## Users & Use Cases

- **Zac** — piano (improv + classical), specifically the tedious chord-progression drilling he tends to under-practice. Reach-for-it moment: he's already sat down to practice, and chord drilling is the part he'd otherwise skip or shortchange — he plans a session for it in Practice Loop and logs it like any other practice, instead of quietly skipping to the parts he enjoys more.
- **David** (friend) — video editing/making, the repetitive, tedious parts of that process. Reach-for-it moment, from a real recent example: he filmed footage on a Friday and let it sit, unedited, until Sunday. Practice Loop doesn't know he filmed anything — it can't detect that on its own. What it does is hold him to a plan he makes himself: right after filming, he opens the app and schedules an editing session for the next day, and gets a real notification when that time arrives, instead of relying on memory alone to close the gap.

Two different skills, one practice shape — the app must not assume music-specific structure, but v1 does not need to support multiple session shapes to serve both (see Core Loop).

## Core Loop

v1 ships **one session shape: a free-form timer** (start/stop) — a "focus session." Both Zac's chord drilling and David's editing sessions are logged this way; a checklist/discrete-steps shape was considered and cut from v1 (see Non-Goals).

Sessions are **planned, not reactive**: a user schedules a session ahead of time (activity + a start time), and the app fires a real OS notification when that time arrives. There is no "remind me after N idle days" fallback in v1 — if a session isn't scheduled, nothing fires.

The app stores a log of completed sessions per user-defined activity, and a reward is computed from each session's logged minutes (see Reward System below).

## Reward System

Each session has a **recommended base XP, shown as a guide when the session is created** (derived from the activity's default per-minute rate and the planned duration) — the user can freely override that base number at creation time; there is no hard cap or enforcement in v1. At completion, the actual XP awarded is computed from that base against how much of the session was actually completed — more logged minutes relative to the plan yields more XP, up to the full base at exact completion. Claiming is a plain confirmation at that point, not another editing step. **Edge case:** if completed minutes is `0` (stopped immediately), the formula divides by zero — guarded with a direct check that returns `0` XP before the formula runs, no exception needed. A confirmation dialog on "stop early" reduces how often this gets hit by accident, but doesn't replace the code-level guard, since a user can still confirm within the first second. This is a deliberate trust-based design for a two-person tool, not an oversight — see Risks for the reasoning. **No leveling in v1** — XP is just a running total for now; level thresholds and unlocks are a v2 candidate, not designed yet.

**Resolved:** sessions are fixed-duration only (no open-ended session type — considered and cut to keep scope down). The timer **hard-stops automatically** when the planned duration elapses, so completed minutes can never exceed planned minutes. XP is computed from actual completed minutes against the planned duration, using `floor(recommendedXP / (1 + ((plannedMinutes / completedMinutes) - 1)²))` — full XP at exact completion, shrinking toward zero the further actual time falls short. Exact formula flagged as tunable before implementation, not final.

## v1 Features

1. Define one or more practice activities.
2. Schedule a focus session for an activity (pick a start time).
3. Three real OS notifications per session: a **reminder** at a configurable lead time before the scheduled start (shows title, description, scheduled time, time remaining), a **"starting now"** notification exactly at the scheduled start time, and a **completion** notification when the timer hard-stops.
4. The timer does **not** auto-start at the scheduled time — the user presses a "start" button whenever they're actually ready, deliberately, so it doesn't run while they're away. Once started, it runs for the session's planned duration and hard-stops automatically when that elapses, regardless of whether it started exactly on schedule or late. XP is computed from completed vs. planned minutes against the base XP set when the session was created (see Reward System); claiming just confirms it.
5. View session history: one date-ordered list covering all activities, persisted on-device and available after restarting the app, filterable down to a single activity (or a "Misc" filter for sessions with no linked activity).
6. Track accumulated XP total (no levels in v1).

## Non-Goals (v1)

- No checklist/discrete-steps session type — v1 is timer-only. May revisit if timer-only turns out not to fit David's editing workflow well.
- No reactive/idle-based notifications ("remind me after N days with no session") — planned sessions only.
- No calendar-grid scheduling UI — session data is modeled so one could be added later, but v1 shows schedule/history as a list, not a calendar view.
- No hard cap or enforcement on the reward override — the recommendation is a guideline, not a limit, in v1.
- No onboarding/setup-survey flow ("what will you use this for," "how often") — the user just creates an activity directly. May revisit later.
- No focus/attention tracking during a session (e.g. webcam or activity monitoring) — considered and cut; out of reach for this project's time budget and raises privacy questions for David as a second user.
- No level-up unlocks (cosmetic or otherwise) — no concrete unlock content designed yet. Levels are a progress indicator only in v1; unlocks are a real v2 candidate once there's an actual answer to "unlock what."
- No multi-user support or sync — single-user local desktop app only, one instance per person (Zac and David each run their own).
- No mobile app, no web frontend, no server.
- No AI/ML features.

## Data

SQLite via JDBC, two tables, linked by an optional foreign key.

**`activities`** — a reusable category with default values, nothing more.
- `id` (primary key)
- `name`
- `description`
- `default_planned_minutes`
- `default_lead_minutes` (default notification lead time)
- `default_xp_per_minute` (default reward rate used to compute a session's recommended XP)

**`sessions`** — the actual scheduled, timed thing. Always has its own name/description/timing, whether or not it's linked to an activity.
- `id` (primary key)
- `activity_id` (foreign key to `activities`, **nullable** — null for a one-off session with no category; when set, its purpose is grouping for history, not controlling display)
- `name`, `description` (pre-filled from the linked activity's defaults if any, always editable, required if there's no activity)
- `scheduled_time`
- `planned_minutes`, `lead_minutes` (pre-filled from activity defaults if any, editable per-session)
- `completed_minutes` (null until the session actually runs and hard-stops)
- `xp_awarded` (null until claimed)
- `status` (`scheduled` / `completed`)

Accumulated XP is never stored as its own value — always computed with `SELECT SUM(xp_awarded) FROM sessions`.

## Success Criteria

- **Scheduling persists.** Create an activity, schedule a fixed-duration session for it, close the app fully, reopen it: the session still shows as scheduled, at the same time, for the same activity.
- **Pre-session notification fires with the right content.** Schedule a session with a chosen lead time (e.g. 15 minutes). At that lead time before the session's start, a real OS notification appears showing the activity's title, description, the session's scheduled time, and time remaining until it starts.
- **Timer hard-stops on schedule.** Start a session's timer at or after its scheduled time; without manually stopping it, the timer stops on its own exactly when the planned duration elapses, and a completion notification fires at that moment.
- **XP matches the formula at full completion.** Run a session's timer for its entire planned duration (no early stop): the XP awarded equals the recommended amount computed at creation, unreduced.
- **XP matches the formula on early stop.** Manually stop a session's timer partway through (completed minutes < planned minutes): the XP offered equals `floor(recommendedXP / (1 + ((planned/completed) - 1)²))` for those actual numbers, not the full recommended amount.
- **Override works.** When creating a session, change the offered base XP to a different value from the guide's suggestion: completing that session in full awards the overridden value, not the app's original suggestion.
- **XP total persists.** Claim XP on a session, close the app fully, reopen it: the accumulated XP total reflects that claim.

## Milestones

Each one a real vertical slice — something new a user could actually do, end to end, not a layer finished in isolation.

1. **Create and see it.** Create one activity, schedule one fixed-duration session for it (basic time input, not yet a polished picker), and see it sitting in the main list, persisted in real SQLite and still there after restarting the app.
2. **Scheduled, notified, timed.** A proper calendar-style time picker for scheduling. All three real OS notifications firing at the right moments (lead-time reminder, "starting now," completion). A live in-app countdown to the next session's start. A "start" button the user presses themselves — never automatic — that begins the timer, which then hard-stops on its own once the planned duration elapses.
3. **XP, claimed and kept.** The recommended base XP guide shown and overridable at session creation. The completion formula computing actual XP from completed vs. planned minutes when a session hard-stops. A claim button that adds it to the running total, shown in the top-right corner, correct after closing and reopening the app.
4. **Multiple activities, sorted history.** Creating and running sessions across several different activities, plus a standalone session with no activity at all (landing in "Misc"). The history view — the same list, switched to past sessions — filterable down to one activity or Misc.

## Test Plan

**Automated (JUnit), narrow and deliberate:** just the XP formula, because it's pure logic (same input always gives the same output) with a real edge case that's easy to forget to retest by hand after later changes.
- Full completion (`completed == planned`) awards the exact base XP.
- Partial completion matches the formula's expected output for specific numbers.
- `completed == 0` returns `0` without dividing by zero.

**Everything else, by hand:** UI layout and interactions, real OS notifications actually appearing, SQLite persistence surviving an app restart. All low-risk, simple to eyeball, and not worth building test infrastructure for at this project's size.

## Definition of Done

- All 4 Milestones working end to end, matching the Interfaces layout described earlier (left nav, downscroll list furthest-future-to-soonest, activity/session creator popups with the live preview, top-right XP total, history view filterable by activity/Misc).
- All Success Criteria pass — both the automated XP formula tests and the manual UI/notification/persistence checks.
- Concrete polish bar, since "happy with it" isn't checkable by anyone else: no placeholder or debug text visible anywhere, every button/field labeled clearly, consistent spacing, no crashes during normal use of any Milestone's flow.
- README, kept simple: JDK install steps (Eclipse Temurin, mirroring what actually worked this session), then how to run the app. Tested for real by having David follow it on his own machine, not just assumed to work.
- Pushed to a GitHub repo with a clean commit history.
- Both open items from Open Questions actually answered before calling this finished — not necessarily resolved in Zac's favor, just answered on the record: does timer-only fit David's editing workflow, and how does Practice Loop differ from Forest/Flocus. Building all the way to "done" without either answer risks finishing something that turns out not to fit its own second user, or that a mentor conversation kills anyway.

## Interfaces

JavaFX screens, all within one window.

- **Main screen.** Left sidebar (nav menu, mostly empty in v1). Top-right corner shows the running XP total (`SELECT SUM(xp_awarded)`, not stored). Center is one scrollable, date-ordered session list — furthest-in-the-future at the top, counting down toward the soonest session, showing a live countdown to its start. The next session to start shows a "start" button once its scheduled time arrives (the user presses it when actually ready, not automatic). Once started, that session is pinned at the very bottom showing a live timer, a "stop early" button, and a "claim XP" button once it hard-stops. Above the list: an "Add Activity" button and a "Create Session" button, plus a history toggle that switches the same list to show past sessions instead, filterable to one activity or "Misc." (Button names flagged by Zac as likely to change before implementation, for clarity — not final.)
- **Activity creator (popup).** Sign-up-page-style separate fields for an activity's name, description, and defaults (planned minutes, notification lead time, XP-per-minute rate). Includes a live preview of both the session card and the notification, reflecting the fields as they're typed.
- **Session creator (popup).** Same shape as the activity creator — same fields, pre-filled from an activity's defaults if one is selected, fully editable either way, including for a standalone session with no activity. Shows the recommended base XP as a guide (rate × planned duration); the user can override that number here, at creation — this is the only place XP is edited.
- **Claim XP.** No animation/flourish in v1 — just a confirm button; the XP total updates by whatever the completion formula computed from the session's (possibly overridden) base. No editing at this step.

## Architecture

- **Platform:** Java desktop app using **JavaFX** (chosen over Swing for better modern docs/tutorials as Zac's first GUI project).
- **Persistence:** local only (no server, no account system), using an embedded database (SQLite) via **JDBC** with a SQLite driver dependency — chosen over a flat file deliberately as a stretch learning goal (Zac's own Phase 1 self-assessment named databases as an area he's never had reason to touch).
- **Scope constraint:** must run entirely free/local, no paid APIs, consistent with the ~25–35 hour / 2–4 week budget.

### Components

- **UI (JavaFX)** — displays activities, schedules, running timers, and XP; forwards user actions (create activity, schedule session, start/stop timer, override XP) onward. Owns no business logic itself.
- **Session / SessionList** — the core domain object(s). Owns a session's planned duration, its timer (including the automatic hard-stop), and triggers the XP calculator and notification scheduler at the right moments (on creation, on hard-stop).
- **XP calculator** — pure function: given planned and completed minutes plus the recommended base, returns the computed XP. No side effects, no persistence of its own.
- **Notification scheduler** — fires the OS notification before a session (at the configured lead time) and the completion notification at hard-stop. Reads session data to populate the notification's content.
- **SessionStore** — the only component that talks to SQLite. UI, Session/SessionList, and XP all go through it to save or load; nothing else touches JDBC directly.

## Risks & Mitigations

- **Risk: practice falls out of the user's schedule** (Zac's own named pattern — not a motivation failure, a lack of external structure). **Mitigation:** OS-level scheduled notifications on planned sessions, confirmed technically working via a throwaway `java.awt.SystemTray` spike.
- **Risk: reward system repeats Habitica's flaw** (reward tiers too rigid, self-serve claiming too loose, at once). **Mitigation:** the recommended number is computed from real tracked session minutes rather than a flat guess, addressing the rigid-tiers half. The self-serve-override half is accepted, not solved — at this app's scale (Zac and David, two people who opted in on purpose), overriding the reward defeats the user's own reason for using the app, unlike Habitica's public scale where that incentive doesn't hold. A max-per-minute or daily cap is a possible later addition if this turns out to matter in practice.
- **Risk: the project stalls before it's built**, per Zac's own documented pattern of things falling out of his schedule (a missed internship meeting, an abandoned video project). **Mitigation:** Zac's weekly check-ins with his internship mentor are the backstop; his own logged-hours pace (tracked for the internship's required-hours program) is the earlier tripwire — falling behind pace is the cue to get back to it before the check-in has to catch it.
- **Risk: Practice Loop closely resembles existing apps** (Forest, Flocus). **Mitigation:** resolved — checked prior art on all four alternative Phase 2 candidates as the same bar, and two (Show-Up, Concept Tree) turned out to have closer existing competitors than Practice Loop does. Reward system honestly split: the growth/XP mechanic is Zac's own motivation feature (not something David's validated use case requires), while David's actual need — the gap between filming and editing — is served by scheduling + notification alone. Decision: continue with Practice Loop at current v1 scope.

## Open Questions

None currently — the last open item (whether timer-only sessions fit David's editing workflow) was confirmed fine by David during SPEC.md review.

