# Troop clips: per-troop folders, attack picks, idle variants, gait clips

Status: **code for Kort's review before merge.** Branch `claude/gifted-ptolemy-j2rv7i`. Gameplay timing is unchanged.

## What it lets an animator do

| Need | How | Where |
|---|---|---|
| A troop moves unlike the rest of its family | Its own project `art/clips/<rig>/<troop>.bbmodel` writes `clips/<rig>/<troop>/`. The game layers that folder over the family folder, and the family over `common`, clip by clip. | `TroopMotion.build`, `tools/clips.py` |
| A troop has its own attacks | One `troopAttacks` row per clip: `{id: <troop>_<clip>, troop, clip, slot}`. The slot is one of the family's `attackVariants` rows. The clip keeps that row's impact tick, zone, kind, weight and the length cap. When the server swings that variant, the client picks one of the troop's clips for the slot, by the unit's seed and the swing's tick. | `sheets/design.json`, `tools/preflight.py`, `TroopMotion.Fam.attack` |
| Several idles | `idle_alt_1`, `idle_alt_2`, ... play once in place of the code's novelty idles: on a calm unit, every 6 to 14 s, eased in and out over 0.3 s, and let go when the calm breaks. A family without them keeps the code's six novelties. | `MotionBody.life` |
| Own reactions | A troop folder with any hit clip of its own replaces every inherited hit; the same for blocks and for deaths, each on its own. | `TroopMotion.build` |
| Specials | `victory`, `war_cry`, `summon`, `spawn_rise` and any other named clip in the troop, family or `common` folder can be played by `setScenePose(<name>)`. **What triggers them in a match is a gameplay call and is not wired.** | `TroopMotion.scene` |
| A character walk and run | Looping `walk` and `run` clips play on the stride's own phase, so by distance travelled (time 0 is the right heel striking, half the loop the left). In their share they replace the code's upper-body gait (bob, sway, lean, twist, arm swing, elbows). A `stride_<tenths>` timeline marker (for example `stride_16`, 1.6 blocks a loop) sets the stride length, held to 0.6 to 1.25 times the code's so the legs can reach. The legs stay the code's solved stride, planted by `MotionFeet`; what a gait clip keys on the legs is added as an offset. | `MotionBody.stride`, `Clip.stride` |

Bones: a troop project may key any bone of its troop model (props such as `bolt_t2`), as well as the rig's. A bone in neither
is still refused.

## The timing rule

New attacks reuse their family's timing slots. A troop clip in the `sword_shield_thrust` slot must hit on tick 7, and a clip in
the `bow_volley` slot loosing on its row's tick. `impactTicks`, cooldowns and clip length caps do not change. The server never
sees which clip played. Changing any of those still needs your decision and a mode-gate sim.

## Checks

- `tools/preflight.py`: a troopAttacks row needs a real troop, a slot of that troop's family, a name that is not an
  attackVariants id, and its clip file. **A missing clip fails the build.**
- `tools/clips.py`: a troop project's swings hit on their slot's tick; walk and run loop; idle variants play once.
- `test-tools/standalone/MotionTroopClipsCheck.java`: the gait clip follows the stride phase and replaces the code arm swing;
  the stride marker lengthens the stride within the cap; idle variants play on calm units and let go when an enemy comes near.
- The turn suite and motion grade (client) are what show the troop folders and attack picks in game; run them per House branch.

## For Kort

1. Approve the per-troop layering and the `troopAttacks` sheet (the slot rule keeps gameplay timing).
2. Approve gait clips replacing the code's upper-body gait, with the legs left to the solved stride. The alternative (a clip
   that owns the legs and switches `MotionFeet` off) skates at any speed but the one it was keyed for.
3. Decide what triggers the specials in a match (victory at a won round, war cry on a Rally order, summon for casters,
   spawn rise for Graveborn raised units).
