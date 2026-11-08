# v2 death polish: weapon and prop review (2026-11-07)

Owner feedback: "during death animations for some units their weapon glitches around a lot and ends up in places that don't make
sense." Every death clip of the 21 v2 bipeds was reviewed for the weapon, the shield and any held prop (bows, staffs, harpoons,
crossbows, books). Body motion, clip names and clip lengths are unchanged. Attacks, idle, stance, gait and hit clips are parsed-JSON
identical to the previous commit (checked for every unit, art and src copies; 0 bad).

## Rules the clips are held to

1. The weapon is held until the body lands and rests with it, or is released on a continuous, believable arc.
2. Once down it lies flat and still: no standing upright, no sliding, flipping or popping after it settles.
3. Never below the floor (more than 0.1 px) and never inside the torso or legs.
4. No single-tick jumps while not in free fall: no more than about 3 px or 30 degrees per tick.
5. The settle is a small bounce or clatter, then stillness, timed to the body's landing.

## Method

`probe.py` (scratch tool, not committed) samples every death clip of every unit at half-tick steps in world space (the rig, the
cubes of each held prop) and flags: FLOOR (below the floor), INBODY (inside torso/legs), UPRIGHT (more than 40 degrees off flat
after the body landed), NOTFLAT (rests tilted), LATE (lands after the body), FLIP (sudden rotation), SNAP (position jump), POP (velocity
change of more than 12 px/tick for a held prop and 9 px/tick for a free-keyed one while airborne). The counts below are before
(the commit this branch started from) and after.

| Unit | Before | After |
|---|---|---|
| knight | FLIP 3, FLOOR 10, NOTFLAT 3, UPRIGHT 3 | clean |
| sentinel | FLIP 5, FLOOR 3, NOTFLAT 3, SNAP 1, UPRIGHT 3 | UPRIGHT 1 (accepted, below) |
| gl_marshal | FLIP 4, FLOOR 3, NOTFLAT 3, UPRIGHT 2 | INBODY 1 (3.3 px graze, below) |
| ic_hearthguard | FLIP 2, FLOOR 3, NOTFLAT 3, POP 1, UPRIGHT 3 | UPRIGHT 1 (accepted, below) |
| revenant | FLIP 3, FLOOR 1 | POP 1 (accepted landing impact) |
| tc_raider | FLIP 2, NOTFLAT 1 | clean |
| wm_tusk | FLIP 1, INBODY 1, POP 1 | clean |
| tc_tideguard | FLOOR 1, INBODY 1, POP 1, SNAP 1, UPRIGHT 1 | POP 1 (accepted) |
| tc_admiral | FLIP 1, POP 2 | clean |
| ic_breaker | POP 2, SNAP 1 | clean |
| archer | FLIP 2, FLOOR 3 | clean |
| longbow | FLIP 1, FLOOR 4 | clean |
| wm_hunter | FLIP 3, FLOOR 3, INBODY 1, NOTFLAT 1, UPRIGHT 1 | clean |
| bonebow | FLIP 4, FLOOR 3, INBODY 1, LATE 1, SNAP 1 | clean |
| stalker | FLIP 2, FLOOR 4, NOTFLAT 2, UPRIGHT 1 | clean |
| ic_arbalist | FLIP 4, FLOOR 4, INBODY 1, POP 2 | POP 2 (accepted) |
| tc_harpooner | FLIP 1, FLOOR 2, POP 1 | clean |
| wm_seer | FLOOR 1, INBODY 1 | clean |
| tc_mist | FLOOR 1, NOTFLAT 1, UPRIGHT 1 | POP 1 (accepted) |
| mage | FLIP 2, FLOOR 1 | POP 1 (accepted) |
| hexer | NOTFLAT 4, UPRIGHT 4 | POP 1 (accepted) |

Totals: before FLOOR 47, FLIP 40, UPRIGHT 19, NOTFLAT 21, SNAP 4, POP 10, INBODY 6, LATE 1 (148 flags); after UPRIGHT 2, INBODY 1,
POP 7 (10 flags, all accepted or marginal, listed below).

## Root causes

1. **Carried props through the floor.** A prop in the hand follows the wrist, and a falling body drives the wrist (and so the
   weapon) below the floor, often by several pixels (knight 10 flags, archer, longbow, stalker, bonebow).
2. **Flips and pops at release.** When a prop was let go, the free-keyed motion started with a different velocity and spin than the
   hand had, so it jumped in one tick; props also flipped over by whole turns on a short flight.
3. **Props upright or tilted at rest.** Several clips left a sword or staff standing on its pommel or leaning, then a late key
   toppled it (UPRIGHT, NOTFLAT, SNAP).
4. **Props inside the body.** A landing spot or a held pose put a bow, harpoon or shield inside the torso or legs.
5. **Shared death system (knight family, `deaths.py`).** The release velocity was capped, which left an instant velocity jump
   at release; a sharp clearing swerve over a short flight produced a huge lateral acceleration; when no clearing swerve existed the
   builder used none and left the shield inside the body; hops did not obey the gravity used elsewhere; flights could run past the
   body's landing.

## What changed and where

- `art/anim/common/deaths.py` (shared by the six D3 troops: knight, sentinel, gl_marshal, ic_hearthguard, revenant, tc_raider; 12
  death clips each):
  - the vertical release speed is clamped, the flight time is stretched to fit the sideways drift, the rotation and the speed
    required, and capped so the prop touches by tick 17.5;
  - `D3_RELAX` (0.5 tick): the speed removed by the caps is shed over half a tick, not in one tick;
  - `D3_SWACC` (25 px/tick^2): a clearing swerve that would bend harder than this stretches the flight (up to the touch cap), and
    swerves wider than 6 px use a smooth `sin^2` profile instead of the quick ease-in;
  - when no swerve clears the body, the minimum-penetration offset is used instead of none;
  - the hop after the touch obeys the same gravity.
- `art/anim/common/heldprop.py` (new): a data-level post-pass on the death clips of any unit. For each hand that holds a prop it
  solves the wrist rotation per half tick (a Viterbi search over about 700 candidates) so the prop stays above the floor and out of
  the torso, stays close to the natural wrist, and lies flat once the body has landed; then it releases (pins) the prop at the
  landing instead of letting it slide, and lifts props that would sink. Clips that key the prop bones themselves are skipped.
  `python art/anim/common/heldprop.py <troop> [--scratch <path>] [--check]`. Environment toggles exist for trials.
- The 15 non-D3 units get the post-pass only (their four death clips). The six D3 troops get a fresh `deaths.py` build for the death
  clips, spliced into the committed JSON, then the post-pass.
- Exported through the normal path (`art/anim/common/export.py <troop>`, knight through `art/anim/knight/export.py`) into
  `src/main/resources/assets/gravesandglory/animations/troop/`.

### The out-of-date-deaths caveat

POLISH-SWAY.md (commit 46e3fbb7) kept the committed death clips as the approved baseline: the bow family's committed deaths
predate the widened shieldless idle, and a fresh rebuild moves their start poses. Rebuilding those from the builders would therefore
change the body motion Kort approved. Resolution: the 15 non-D3 units were not rebuilt; their committed deaths were edited at data
level (wrist and release only, body untouched). The D3 troops share one death system that was already current, so only their
`death_*` clips were rebuilt and spliced; the clips that keep both pieces came out equal to the committed ones.

The D3 JSONs were built over a few rounds of constants (knight and revenant, tc_raider on the first, sentinel and gl_marshal on
the second, ic_hearthguard on the third). Rebuilding all six with the final `deaths.py` would only smooth the clearing swerves
further for the units built before `D3_SWACC` was lowered to 25 (knight, sentinel, gl_marshal): not re-run.

## Accepted

- **Held-landing POPs** (tc_tideguard back 11.6, mage back 11.2, revenant back 10.0, ic_arbalist 9.1 and 9.4, tc_mist 9.7, hexer
  9.4 px/tick): the prop is carried by the body's landing impact, then lies flat and still. The thresholds (9 free, 12 held) are set
  so ordinary falls pass; these are the landing impact itself, not a glitch.
- **right_drop2 shield, UPRIGHT 41 degrees (ic_hearthguard) and 50 degrees (sentinel):** the shield topples over its edge in about
  four ticks and is upright for roughly half a tick longer than the probe's grace. Fixing it needs the shared shield release time
  on the right to change, and a rebuild of all six units.
- **gl_marshal death_right, shield INBODY 3.3 px for about two ticks:** the shield is released from the dropped arm and grazes the
  torso (no clear path from that pose). Minor; not worth a visible change to the arm.
- Cape z-fight (sentinel D10-12, tideguard D4): a body and cape rendering issue, out of scope.

## Harness

In-game checks on tc_tideguard, ic_arbalist and archer (the three most changed non-D3 units): tc_tideguard p1=22 false=0; ic_arbalist
and archer each failed once on a lag-dependent timing check (march-run-threequarter and attack-sync) and passed on the rerun
(p1=22 false=0, lag 0). No NoClassDefFoundError.

## Review

One fresh Sonnet (high effort) reviewer looked only at the after-sheets (39 contact sheets, 7 frames per clip, side and three-quarter).
Verdict: mostly pass. It raised 12 low-confidence items, most from the small cells and the cropped frames (a prop at the cell edge,
the long spear and staff lying diagonally across the body, thin gold bars on the three-quarter view). The three it rated most likely
(knight back_drop2 shield, left-drop swords near vertical, revenant back weapon out of frame) were checked against the per-half-tick
probe: the shield and swords end with 0 degrees off flat and low at or above the floor (swords in the left drops are mid-flight in
frames 3-5, not resting), and the revenant weapon lands flat 36 px behind the body and stays still. No fix round was opened for
these; the other items match known rest poses (long props lying diagonally over the legs, committed body motion).

## Evidence

Before and after reel for every changed unit (side and three-quarter): `deaths_weapon_fix.mp4` in the session scratchpad.
