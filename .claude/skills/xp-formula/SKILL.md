---
description: Verify or tune Practice Loop's XP reward formula — use whenever the completion-based XP calculation is being discussed, implemented, or changed, since it's not finalized yet
---

## Background

XP is computed from actual completed minutes against a session's planned duration:

floor(recommendedXP / (1 + ((plannedMinutes / completedMinutes) - 1)²))

Full XP at exact completion; shrinks toward zero the further actual time falls
short of planned. This formula is explicitly not finalized (see SPEC.md's
Reward System section) — treat proposed changes to its shape as expected, not
as bugs.

## Known edge case

completedMinutes == 0 divides by zero. Must be guarded with a direct check
that returns 0 XP before the formula runs — never let an exception reach the
user.

## Instructions

When asked to verify, test, or tune this formula:

1. Restate the current formula and what its shape means in practice — compute
   2-3 example XP values at different completion percentages so the shape is
   concrete, not abstract.
2. Confirm the zero-division guard is in place and tested before touching
   anything else.
3. Check behavior against SPEC.md's three XP-related Success Criteria (full
   completion, partial completion, override).
4. If Zac wants to change the formula's shape, explain the mathematical
   effect of the change (does it punish early stopping more or less harshly?)
   before writing any code.
5. After any change: update SPEC.md's Reward System section, the Success
   Criteria section, and the corresponding JUnit tests so all three stay
   consistent with each other.
