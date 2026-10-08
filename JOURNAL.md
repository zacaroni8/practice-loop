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

## Milestone 3 — XP, claimed and kept

First real authorship split: `XpCalculator` and its JUnit tests are Zac's own code, not generated. Wrote it wrong twice before it worked — first a plain syntax error (missing return type, no `assertEquals` import, a stray semicolon), then a real logic bug (the whole `recommendedXp * ratio` product was getting raised to the power, not just the ratio) that made every test fail except the trivial zero case. Debugged both from error output and a pointed question, not from being handed the fix.

Bigger thing that came out of this milestone: Zac caught a real flaw in SPEC.md's own original XP formula — it was more XP-efficient per minute spent to stop a session around 75% and restart than to actually finish one. Verified with real numbers (36 XP/15min vs. 40 XP/20min). Replaced it with `recommendedXp * (completedMinutes/plannedMinutes)^1.3`, a shape that guarantees finishing is never worse than stopping early, for any exponent above 1 — not a patch, a structurally different fix. SPEC.md's Reward System, Success Criteria, and Test Plan sections all updated to match, per the `xp-formula` skill's own rule. Bonus: the new formula also deleted the zero-division guard entirely, since it no longer divides by `completedMinutes` at all.

`SessionStore` additions (`base_xp` column, `claimXp`, `getTotalXp`, extending `createSession`) were Zac's to write too, pattern-matched off the existing methods. Found and self-fixed several real bugs along the way: broken string concatenation in the new column DDL, a missing semicolon, `Session`'s constructor never updated to accept the new `baseXp` field (caught by the compiler), `listSessions()` reading `base_xp` from the result set but never actually passing it into the `Session` constructor (also caught by the compiler). One bug was mine, not his: I assumed `rs.getInt("xp_awarded")` would work on `SELECT SUM(xp_awarded) FROM sessions` without testing it — it throws, because an unaliased aggregate expression doesn't inherit the source column's name. Confirmed with a real throwaway test instead of continuing to guess, then fixed by reading the sum by position (`rs.getInt(1)`) instead of by name.

JavaFX UI wiring (recommended/base XP field, top-right XP total, Stop Early button, claim-XP flow) was mine to write, checkpointed with Zac afterward rather than handed over silently — walked through `Optional.ifPresent(...)` vs. the `.isEmpty()`/`.get()` pattern he already knew, and the `java.time.Duration` vs. `javafx.util.Duration` naming collision.

Added `run.bat` so compiling and launching the app is one command instead of Zac re-asking for the javac/java invocation every time — this also surfaced that `.claude/hooks/compile-check.sh`'s plain `-cp "lib/*"` never actually reaches the JavaFX jars nested under `lib/javafx-sdk/lib/`, since compiling against JavaFX needs `--module-path`/`--add-modules`, not just a classpath entry.

Three more real bugs, found by Zac using the app rather than by test: the hour/minute dropdowns showed single digits (`5` instead of `05`) — fixed with a `ListCell` cell factory, genuinely new JavaFX for Zac, explained rather than handed over cold. The minute dropdown's 5-minute step made fast manual testing painful (had to wait up to 5 minutes between test sessions) — changed to a 1-minute step, Zac's own one-line fix. And notification reminder text was consistently a minute low — root cause was `Duration.toMinutes()` truncating instead of rounding; took Zac two attempted fixes to land (first attempt rounded a value that had already been truncated to a whole number before the rounding ever ran, same category of bug as the XP-formula exponent-grouping issue from earlier — order of operations losing precision that can't be recovered afterward) before correctly pulling from `toSeconds()` first. Separately, a scare where notifications stopped firing entirely turned out to be Windows itself (likely Focus Assist or notification throttling), not the code — confirmed via an isolated throwaway test using only `SystemTray`/`TrayIcon`, no Practice Loop code involved, before concluding the Java side was fine.

Small naming correction, Zac's own catch: the XP field was still labeled/named "recommended" even though the user can freely override it before the session starts and that overridden number becomes the actual scoring base — not just a suggestion. Renamed to match.

Milestone 3 manually verified end-to-end by Zac: create a session with the XP field shown and overridable, run one to full completion and claim it (total goes up by the full amount), stop one early and claim the reduced amount, close and reopen the app with the total still correct.

## Milestone 4 — Multiple activities, sorted history

Multiple activities and standalone ("Misc") sessions already worked end to end from earlier milestones — nothing new to build there, just verified. The real gap: the main list had no status filter at all, so completed sessions piled up permanently mixed in with upcoming ones.

Authorship split held the same shape as Milestone 3: the filtering logic (`upcomingSessions()`, `historySessions(activityFilter, miscOnly)`) was Zac's, pattern-matched off `soonestScheduled()`/`overlapsExistingSession()` already in the file; the history-toggle UI (new button, activity filter `ComboBox`, "Misc only" checkbox, and making `refreshSessionList()` mode-aware) was mine, explained afterward.

Real bugs, found the normal way — by compiling and by Zac working through the itemized list rather than being handed fixes: a stray unrelated typo (`totalXpLabel.setText(...)` missing its method name entirely, blocking the whole build before his new code could even be checked), a plural typo (`List<Sessions>`), a capitalization mismatch calling `store.ListSessions()`, a raw `Stream` used without its import, and a wrong field name (`Activity` doesn't have `.activityId`, just `.id`). One real logic bug survived compiling clean: filtering completed sessions down to one activity unboxes `Session.activityId` (nullable) against `Activity.id` (a primitive `int`) — for a Misc session (`activityId == null`), that throws `NullPointerException` at runtime, confirmed with a real throwaway test before pointing Zac at it rather than assuming. Zac's own fix was the correct one: a `s.activityId != null &&` short-circuit guard before the comparison.

Separately, Zac caught a real, longer-standing gap while testing: non-numeric input in any minutes/XP field (`Integer.parseInt`/`Double.parseDouble`) threw an exception that only ever surfaced in the console, never to the actual user — unlike the scheduled-time picker, which structurally can't hold an invalid value. Fixed with `TextFormatter` (new to Zac, explained rather than handed over cold): a per-keystroke filter that rejects any edit that wouldn't leave the field as a valid whole number or decimal, the same way `DatePicker` already refuses garbage input — plus extending both dialogs' OK-button `disableProperty` bindings so an emptied field can't be submitted either.

Milestone 4 manually verified by Zac: history toggle works, filtering by activity and by "Misc" both behave, upcoming view is no longer polluted by completed sessions.

## Post-v1.0.0 polish

After tagging v1.0.0, four real refinements came out of actually using the app with David nearby: default lead time (15 min) was annoyingly long for quick testing, nothing capped an absurd planned-minutes typo, the overlap check hard-blocked with no way to override, and the session creator only showed whole-session XP once, auto-filled from an activity and never updated again.

Authorship split: the default-value change and converting two checks (overlap, 240-minute max) from a hard `Alert.AlertType.ERROR` block into `stopEarly()`'s existing overridable-confirmation shape were Zac's — pattern-matched off code already in the file, no new concepts. One real bug along the way, missed by the compiler: after adding the two confirmation blocks inside `okButton`'s event filter, the lambda itself (and the `addEventFilter(...)` call around it) never got closed — `dialog.setResultConverter(...)` ended up nested inside it instead of after it. Found by counting braces from the lambda's opening `{` down, not by being handed the fix.

The XP-per-minute ↔ whole-session-XP linkage was mine, genuinely new: two `TextField`s whose listeners recompute each other, guarded by a one-element `boolean[]` reentrancy flag so a programmatic write from one listener doesn't re-trigger the other in an infinite loop (walked through why a lambda needs that array wrapper instead of a plain `boolean` — effectively-final capture). Resolved with Zac beforehand: planned-minutes changes rescale whole-session XP, treating the per-minute rate as the stable quantity (matching how an activity's default rate already worked); editing whole-session XP directly instead recomputes the rate.
