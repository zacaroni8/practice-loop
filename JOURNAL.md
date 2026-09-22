# JOURNAL.md

Short entries, appended after each milestone: what got built, what broke, what got learned.

## Milestone 1 — Create and see it

Built the first vertical slice: one activity, one scheduled session, persisted in real SQLite, visible in a basic JavaFX list, still there after a restart. Nothing broke technically — compiled clean, ran clean, persistence verified directly against `SessionStore`.

What actually went wrong was process, not code: the whole milestone got generated in one pass instead of in small, checked pieces, which undercut the point of the explain-back rule. Learned (again) that "it works" and "I understand it" aren't the same thing, and that the fix has to happen in how the code gets written, not just in a review pass after it's done. Changing that starting Milestone 2.
