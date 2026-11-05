# Oathbound knight: combat pack

A Fresh Animations-style set of combat clips for the Oathbound knight with the Oathblade and the Sunward shield.
It doesn't copy Fresh Animations; it gives troops that kind of mobility, aimed at fighting. Status: **for Kort's look;
not wired into the game.** The clips need the articulated troop model described below.

Watch `combat_reel.mp4` (24.5 s, 60 fps, 1280x720). It plays each clip at full speed, then the strikes again in slow
motion, from two views.

## What changed from `oath_combo`

The earlier clips (`../oathbound/`) run on the game's troop rig, where an arm is one rigid cube from shoulder to wrist,
a leg one cube from hip to foot, and the torso one block. That rig limits what an animation can say: no bent elbows,
no bent knees, no crouch, and the body has to lift off the floor whenever a leg tilts.

This pack adds joints, as Fresh Animations does for mobs:

| joint | bone | pivot (Blockbench) | moves |
|---|---|---|---|
| waist | `chest` | (0, 17.5, 0) | the torso bends and twists; the head and arms ride on it |
| elbows | `right_forearm`, `left_forearm` | (±6, 17.5, 2.5), at the back of the arm | a hinge, 0 to 145 degrees |
| knees | `right_shin`, `left_shin` | (±1.9, 7.5, -2.5), at the front of the leg | a hinge, 0 to 150 degrees |
| ankles | `right_foot`, `left_foot` | (±1.9, 2.5, 0) | the foot stays flat or rolls onto its toe |

The hinges sit at the back of the elbow and the front of the knee, so the point of the elbow and the knee cop stay
closed when they bend. The armour is cut at the joints: every box that crosses one becomes two boxes whose faces take
the matching rows of the original texture, so the set needs no repainting. Inside each cut sits a hidden **joint
sleeve**, a plate a hair smaller than the limb, which fills the gap when the joint bends so neither a seam nor the body
beneath shows. The couter, poleyn, skirt, sabaton and pauldrons each ride one segment whole.

## The clips

| clip | length | what happens |
|---|---|---|
| `combat_idle` | 3.2 s, loop | The guard, alive. The weight rocks onto the lead foot and back, with the hips rolling over it. Two breaths lift the chest. The point hunts in a slow circle and the shield settles. The eyes check the left flank, then the right. At 2.0 s the wrist rolls the blade and resets the grip. It starts and ends on the guard every strike returns to. |
| `combat_advance` | 1.2 s, loop | Moving in guard at 0.5 block/s with a step and drag. The lead foot lands heel first, the root dips on each landing, and the back foot drags up with its heel peeling. The hips sway over the planted foot while the guard rides steady above. |
| `combat_combo` | 2.1 s, once | The three-hit string. First a crouched, loaded wind-up: the hips twist 60 degrees away and the blade cocks behind the shoulder. Then an overhead cut that steps in and drives off the back foot onto its toe. A rising backhand ends in a high guard, the point at the enemy's face. Then a shield bash with a second step and the elbow behind it. Last, a two-step recovery, back foot first. |
| `combat_block` | 1.0 s, once | Brace behind the shield and the blow lands. The shield is driven back into the shoulder, the knees soak it, and the lead foot skids half a step. A shove, then a step back in to the guard. |
| `combat_hit` | 0.9 s, once | Struck unguarded. The head snaps back, the chest folds open and the arms fly wide. A stumbling step back, a catch, the guard gathered again, and a step up to the line. |

## How it moves

- **Feet are placed, not keyed.** A clip says where each foot stands, how far its toe turns out, how high it lifts and
  how far its heel rises. A solver bends the hip, knee and ankle to put it there, so a planted foot holds while the
  body lunges and twists over it. A planted foot never sinks more than 0.07 px into the floor and slides less than
  7 px/s at 60 fps.
- **Arms are aimed.** A pose gives where the upper arm, the forearm and the blade point. A solver turns that into
  shoulder, elbow and wrist angles. Each direction is interpolated with Minecraft's own Catmull-Rom, so limbs travel
  in arcs. The grip is natural: the flats stay square to the forearm, as a hand holds a sword. That also keeps the
  wrist clear of gimbal lock.
- **The shield and the eyes are aimed.** The shield's wrist is solved so the board faces where the clip says,
  whatever the arm does. The head is solved to keep the eyes on the target and only turns away to glance.
- **Overlap.** The forearm and blade trail the upper arm by 15 to 100 ms, so the body breaks in order: hips, chest,
  arm, forearm, blade.
- **Secondary motion.** The cape and the plume are damped springs off the chest and the head, baked into keys.
- **Baking.** Everything solved is baked into keys every 25 ms (twice a tick), so the game plays it as previewed.

Checks (`test-tools/standalone/test_combat_anim.py`) measure each bone's true rotation between 60 fps frames, not
its Euler numbers. No bone turns more than 50 degrees in a frame (only the strikes come close), and no change in speed
is larger than 30 degrees. The loops close within 0.02 degrees, and every strike ends exactly on the guard. Python
writes the exports, and the test checks the committed copies match.

## Files

| file | what |
|---|---|
| `combat_reel.mp4` | every clip at full speed, the combo at 0.35x, the block and the hit at 0.5x; three-quarter view from the shield side and profile from the sword side; scrolling floor studs on the advance |
| `combat_reel.gif` | the full-speed parts, 30 fps, 640x360 |
| `idle_beats.png`, `advance_beats.png`, `combo_beats.png`, `block_beats.png`, `hit_beats.png` | every beat of each clip from both views |
| `combat_clips.java` | the clips in the TroopClips `Clip` builder |
| `combat_clips.json` | the same keys as data, with the rig, the foot placements and the beats |
| `combat_knight.bbmodel` | the knight cut at the joints on the articulated rig, with the sword and shield in the fists and every clip as a Blockbench animation |

Build with `python tools/animate_combat.py`. Use `--still combo 0.4` for one frame and `--check` for the foot and
smoothness report. The rig is `tools/unit_anim/combat_rig.py` and the clips are `tools/unit_anim/combat_clips.py`.

## For Kort

- **The model.** These clips need a troop model with the seven new bones and the armour cut at those joints, as in
  `combat_knight.bbmodel`. That changes how every troop looks in game, so it is your call. The pivots and cuts are
  listed above and in `combat_clips.json`. A troop on the old model can't play these clips; it keeps `oath_combo`.
- **Wiring.** The combo would be the knight's attack, the block and the hit would play on those events, and the idle
  and advance would be in-combat tells. All of that is gameplay-visible, so it waits for you. The advance assumes
  0.5 block/s and needs scaling to the AI's approach speed.
- **Blockbench.** The file has not been opened in Blockbench yet. The keys are converted to Blockbench's space
  (rotation -pitch, -yaw, roll; position -x, y, z), and the sword and shield groups carry their grip rotation.
- **Older file.** The earlier `oath_knight.bbmodel` left the sword and shield at the origin instead of in the fists. That
  is fixed and the file regenerated.
