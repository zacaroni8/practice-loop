# JOURNAL.md

Short entries, appended after each milestone: what got built, what broke, what got learned.

## Milestone 1 — Create and see it

Built the first vertical slice: one activity, one scheduled session, persisted in real SQLite, visible in a basic JavaFX list, still there after a restart. Nothing broke technically — compiled clean, ran clean, persistence verified directly against `SessionStore`.

What actually went wrong was process, not code: the whole milestone got generated in one pass instead of in small, checked pieces, which undercut the point of the explain-back rule. Learned (again) that "it works" and "I understand it" aren't the same thing, and that the fix has to happen in how the code gets written, not just in a review pass after it's done. Changing that starting Milestone 2.

Zac manually verified the click-through flow: create an activity, schedule a session, see it listed, close and reopen the app, session still there. Milestone 1 done. His read: "a bit barebones but it works" — expected, matches SPEC.md's own M1 scope (plain text time input, no real picker yet). That polish is Milestone 2, not a gap in this one.

## Milestone 2 — Scheduled, notified, timed

Built in four checkpointed pieces this time, not one dump: a real DatePicker/time picker, `NotificationScheduler` firing real Windows notifications (reminder, starting-now, completion) with dedup so they only fire once each, a live countdown label, and a Start button running a timer that hard-stops automatically at the planned duration.

What broke — all found by Zac actually using it, not by me: the countdown label kept saying "ready to start" for a session that was already running (turns out a session's status stays `scheduled` in the database the whole time it's running — the same fact from an explain-back a few pieces earlier, showing up for real this time), empty names were accepted for activities and sessions, and nothing stopped scheduling two overlapping sessions. All three fixed, the last one as real interval-overlap checking rather than just blocking the exact same start time, per Zac's own call when asked which one he meant.

Learned: the explain-back questions aren't just busywork tacked onto implementation — the "why does the Set exist" one from Piece 2 came back and mattered again a few pieces later, for a real bug, not a hypothetical one.

One known gap left open on purpose: closing the app mid-active-session loses that session's progress, since the data model only has `scheduled`/`completed` status, nothing for "in progress." Discussed and pinned rather than fixed — real scope to do properly, and not actually the failure mode this app is built to prevent.

## Milestone 3 — XP, claimed and kept (in progress)

First real authorship split: `XpCalculator` and its JUnit tests are Zac's own code, not generated. Wrote it wrong twice before it worked — first a plain syntax error (missing return type, no `assertEquals` import, a stray semicolon), then a real logic bug (the whole `recommendedXp * ratio` product was getting raised to the power, not just the ratio) that made every test fail except the trivial zero case. Debugged both from error output and a pointed question, not from being handed the fix.

Bigger thing that came out of this milestone: Zac caught a real flaw in SPEC.md's own original XP formula — it was more XP-efficient per minute spent to stop a session around 75% and restart than to actually finish one. Verified with real numbers (36 XP/15min vs. 40 XP/20min). Replaced it with `recommendedXp * (completedMinutes/plannedMinutes)^1.3`, a shape that guarantees finishing is never worse than stopping early, for any exponent above 1 — not a patch, a structurally different fix. SPEC.md's Reward System, Success Criteria, and Test Plan sections all updated to match, per the `xp-formula` skill's own rule. Bonus: the new formula also deleted the zero-division guard entirely, since it no longer divides by `completedMinutes` at all.

Still to come: `SessionStore` additions (`base_xp` column, `claimXp`, `getTotalXp`, extending `createSession`) — also Zac's to write, then the JavaFX wiring for the XP field, total display, stop-early, and claim button.
