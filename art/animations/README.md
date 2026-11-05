# Unit animations

## Oathbound knight: `oath_combo` and `oath_guard`

The Oathbound knight (`art/armor/`) with the Oathblade in the right hand and the Sunward shield in the left
(`art/player_weapons/`). Status: **for Kort's look; not wired into the game.** The clips are written for the game's
animation system and can be pasted into a rig's clip set once approved.

### The clips

**`oath_combo`** lasts 1.9 s, once. It is the signature attack, four beats with a recovery:

| time | beat | what happens |
|---|---|---|
| 0.00 | guard | Lead foot forward, shield across the chest, point up at the throat, shield shoulder leading. |
| 0.10 to 0.25 | load, wind-up, hold | The body twists away (35 degrees) and leans back. The blade cocks behind the shoulder, the shield thrusts out to cover, and the weight goes onto the back foot. The pose holds a beat (anticipation). |
| 0.30 to 0.35 | cut, contact | The hips turn first, then the shoulders, then the blade. The overhead diagonal cut lands in 0.10 s, about 20 degrees a frame at 60 fps, as the right foot steps in and the root lunges 2 px. |
| 0.44 to 0.56 | follow-through, settle | The blade carries on low across the body, overshoots, and settles. |
| 0.70 to 0.97 | gather, rising cut, high guard | The blade drops low left, then rips up and across to the right into a high guard. |
| 1.10 to 1.30 | bash load, bash, hold | The body twists back, then the shield punches straight out with a 2.6 px lunge, the sword held high behind. |
| 1.55 to 1.90 | recover, guard | Back to the guard with a small overshoot. |

**`oath_guard`** lasts 2.4 s and loops. It is an idle in the guard: the chest rises and falls on each breath, the sword's
point drifts in a small circle, the shield settles, and the head checks left and right.

The cape and the plume are not hand-keyed. A damped-spring simulation of the body's motion drives them: they lag on the
wind-up, fly out on the lunge and settle after. They cannot swing into the body or through a leg swung back. The
simulation is baked into keys every tick. The root's height is solved, not keyed: it keeps the lowest point of either
sabaton on the floor, baked every tick. With the back leg tilted in the guard, that lifts the root about 0.9 px, since
legs in this rig are rigid.

### Files

| file | what |
|---|---|
| `oathbound/oath_combo.mp4` | 13.7 s, 60 fps, 1280x720: a guard breath, the combo twice at full speed, once at 35% speed, then the guard loop. Two views: three-quarter from the shield side, and profile from the sword side. A timeline marks the beats. |
| `oathbound/oath_combo.gif` | the same at full speed, 30 fps, 640x360 |
| `oathbound/oath_combo_beats.png` | contact sheet of every beat from both views |
| `oathbound/oath_clips.java` | both clips in the TroopClips `Clip` builder that `BipedClips` uses, ready to paste |
| `oathbound/oath_clips.json` | the same keys as data, with the rig and the conventions |
| `oathbound/oath_knight.bbmodel` | the knight, sword and shield on the rig (nested bone groups at the rig's pivots, three textures), with both clips as Blockbench animations |

### How it is made

`python tools/animate_unit.py` builds everything; `--still 0.35` renders one frame; `--check` prints the smoothness
report.

- `tools/unit_anim/rig.py` is the troop biped rig from `art/rigs/biped.json`: root, body, head, arms, hands and legs at
  their pivots, plus the cape and plume follow bones. It poses the rig exactly as the game's `AnimationHelper` does:
  the same angle conventions as `BipedClips` (an arm pitch of -90 points forward) and the same rotation order. It
  interpolates with Minecraft's own Catmull-Rom (`Transformation.Interpolations.CUBIC`), so the preview moves the way the
  game would.
- `tools/unit_anim/oathbound_clips.py` holds the poses and timing, the shield hand that keeps the board facing the
  enemy, the ground solver and the springs.
- The frames are drawn by the armour renderer with bone matrices. Each frame is supersampled 2x and filtered for clean
  edges in motion, and shading follows each face's direction as it turns. A soft contact shadow sits under the feet.

Smoothness (`--check`): no channel jumps between frames. The largest change in a frame is the strike itself, about 20
degrees on the sword arm and wrist, and the frame-to-frame change in speed stays under 11 degrees.

### For Kort

- **Wiring.** Wiring the clips in means adding the combo as a per-class attack variant (ANIMATION-PLAN gap 1) and the
  guard as an idle tell. Both are gameplay-visible, so they wait for your call. The cape and plume keys need a troop
  model that gives those parts their own bones.
- **Item placement.** The held items sit on the rig's hand bones by a grip transform written in `animate_unit.py`, not
  by the game's held-item transforms. A weapon's in-game grip may sit slightly differently.
- **Blockbench keys.** They are converted to Blockbench's space (rotation as -pitch, -yaw, roll; position as -x, y,
  z) but have not been checked by opening the file in Blockbench.
