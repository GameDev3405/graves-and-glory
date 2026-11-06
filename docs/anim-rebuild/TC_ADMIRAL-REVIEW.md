# tc_admiral review record (2026-11-06)

The third spear unit, the Tide Court admiral, with a trident, a cape, coat tails, drifting hair and a kelp hem. It uses the shared spear builder: `GG_SPEAR_TROOP=tc_admiral python art/anim/spear/build_spear_anims.py`. Its rig comes from `art/anim/common/troop_rig.py tc_admiral`, with the kelp hem re-hung on a leg bone through the `HANG` table. It has the same clip set as tc_tideguard: thrust, high thrust and low sweep, each with a `_legs` clip, plus locomotion, stances, hits, block and four deaths.

- **Gear lag:** the knight's cape lag drives the cape (0.35) and both coat tails (0.7). Hair, kelp, plume, tails and sash ends are `soft`: they never hold the falling body up and settle on the ground after it.
- **Fix: kelp hem re-hung and settled.** The hem rides the leg it hangs from, so a stride no longer pulls it through the thigh, and it settles on the ground in the deaths.
- **Fix: rigid back hair.** Only the drifting hair (`hair_adrift`) counts as soft; the back hair stays fixed to the head, so it no longer folds into the skull in a fall.
- **Fix: dropped trident clears the floor.** `body_ground` measures the floor from the hard body only (soft strands hang below it until they settle). `death_step` 0.25 re-keys the fall on every bake sample, so a fast wrist swing cannot dip the trident under the floor between keys. `death_top` 0.15 keeps the trident's top low in a roll rather than standing up off the body.
- Every new per-troop setting defaults to off, so wm_tusk and tc_tideguard clips are unchanged.

## Review

- **Round 1 (Sonnet high, reel `reel_adm_v1`): SHIP WITH MINORS.** The attacks held their loads and settled cleanly; the death poses needed a closer look at the trident against the floor and body. That check, with zoomed death frames, led to the fixes above.
- **Round 2 (Sonnet high, reel `reel_adm_v2`): PASS. No blockers.** The thrusts and the sweep keep the tideguard's load, fast contact, overshoot and exact settle; the deaths lay the trident beside the body with no floor clip.
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false.
- **Verdict: finished.** Video: `tc_admiral_v2.mp4`.

## Open minors (polish, not blocking)

- **Forward death** (t9.8–14.6 of the clip): the trident jerks upward once while the wrist settles it.
- **Wrist turn** at t2.75 of a death: a 76° turn inside one tick reads as a quick flick.
- **Late deaths** (t24+, three-quarter and front views): the trident head sits slightly under the torso silhouette.
- **Kelp hem**: it can sit about 0.4 px into the leg at the extreme of a stride.
- The reel's resolution makes the soft parts (hair, kelp, tails) hard to judge finely.
