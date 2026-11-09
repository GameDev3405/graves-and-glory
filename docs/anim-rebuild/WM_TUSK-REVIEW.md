# wm_tusk review record (2026-11-06)

The first spear unit, the Warmonger tusk. Its builder is new and shared by the spear family: `GG_SPEAR_TROOP=wm_tusk python art/anim/spear/build_spear_anims.py`. It holds the spear left-lead, with the right fist at the hip and the left fist up the shaft, and solves both fists to the shaft every tick. Its rig comes from `art/anim/common/troop_rig.py wm_tusk`. The three attacks each run 22 ticks with their own `_legs` clip:

- `thrust`, impact 7.
- `high_thrust`, impact 7, which dips and drives up at the face.
- `low_sweep`, impact 8, which swings the shaft flat across the shins.

- **Death ground guard.** In the deaths, the spear dug into the floor and spun as the wrist flattened it. `flat_weapon` now rekeys every half tick and `ground_hand` turns the wrist about the shaft's tilt axis until the weapon clears the ground. The left arm, which grips the shaft, is left out of the body-clip cost. The wrist turn is capped at 45° per tick, and `near_euler` picks the Euler form nearest the last key. The probe shows weapon min y 0.00 in all four deaths.
- **Carry clearance.** In walk and run the shaft stays out of the body and the butt stays above the ground: 1.2 px at the walk's lowest, 5.3 px at the run's.
- **Round 1 fix: whole-body thrusts.** The thrust and high thrust draw the shaft about 7 px back with the torso coiled 32° and hold the load (t2.5–5.5). They then lunge off a longer lead-foot step (1.8 ticks, 4.4 px) with the hips dropping and driving forward, then overshoot. The high thrust drives up from the legs. Its rear fist sits 0.8 px further out, which keeps the butt out of the torso at contact. All three attacks have 0 shaft-in-body samples.

## Review

- **Round 1 (Sonnet high, reel `wm_tusk_v2`): SHIP WITH MINORS.** The thrust and high thrust read arms-only. The load was barely visible, the lunge small and the overshoot weak. The low sweep, hits, stances and deaths passed. Fixed above.
- **In-game checks run 1** (round 1 clips): PASS, 22 of 22 checks, 0 false.
- **Round 2 (Sonnet high, reel `wm_tusk_v3`, plus dense side strips of each attack): SHIP WITH MINORS.**
  - The thrust now has a visible held load, then a whole-body lunge with a step and a hip drop, then a settle.
  - The high thrust is distinct and drives up.
  - No new clipping.
  - That was the second fix round, so the remaining items are recorded below.
- **In-game checks run 2** (round 2 clips): PASS, 22 of 22 checks, 0 false.
- **Verdict: finished.** Video: `wm_tusk_v3.mp4`.

## Open minors (polish, not blocking)

- **Thrust load to lunge** (t301→t302) is a one-tick pop. One in-between pose would soften it.
- **Thrust overshoot** is mostly the body sinking. The tip carries only a little past contact.
- **High-thrust load** looks almost like the flat thrust's. The dip is slight, and the upward drive shows mainly from t336.
- **Left death** (t660–677): the spear turns quickly while the body rolls. It is continuous, with no spin.
- **Behind death** (t614): the spear tip reaches floor level at the feet. It touches but does not go below.
- **Run carry:** the butt passes close to the ground (5.3 px at the lowest).

## v2 feel (2026-11-07)

The knight's approved v2 style is ported to the spear builder (`art/anim/spear/build_spear_anims.py`), adapted to a two-handed spear. It is on for wm_tusk, tc_tideguard and tc_admiral (and the ic_breaker greataxe builder); `GG_V2=0` builds the old style. Deaths are unchanged.

- **Pelvis-driven blows:** each attack loads into the back hip, then the pelvis turns and drops into the lunge, and the spear follows the hips instead of leading with the arms. Both fists stay on the shaft.
- **Graded hits:** a light blow is a knee flinch at 0.4 of the peak. A medium blow throws the upper body further and holds it a beat, shifts the pelvis off the footing, and catches it with a recovery step (the knight's hit legs). A heavy blow plays the medium clip at 1.55 times its size, so it reads as a stagger. A blow from behind throws the chest forward with both arms lifted, so the spear point stays off the ground.
- **Idle:** the knight's weight cycle (breath, then a slow shift from foot to foot).
- **Stances:** ready holds the spear level at guard. Brace sets the point steeply, like a pike grounded against a charge. Hold the line crouches with the spear low and level. Ward, for walking backward, angles the point up. All four read apart at gameplay scale.
- **Probe:** no shaft or fist in the body, nothing below the floor, no new pops.
- **Byte identity:** knight, archer and gl_marshal build byte-identical to HEAD.
- **Review (Sonnet high, sheets and the knight v2 comparison): PASS WITH MINORS.** One fix round: brace was too close to ready, so its point was raised (aim 20 to 32). The new stances sheet was checked by eye; there was no second review.
- **Harness:** `geo-knight` check on wm_tusk passed with 22 `[p1] p1-` lines and 0 `=false`. An earlier run under heavy machine load ("Can't keep up") missed one stance-blend timing check (weight 0.98 instead of 1 at k30); the rerun passed.
- **New minors:**
  - The light hit is barely readable from the front (t1–6). It matches the approved knight's light flinch.
  - Medium from the right is subtle, and its recovery step is hard to see.
  - The thrust pop and the low run carry (both listed above) are no worse.
