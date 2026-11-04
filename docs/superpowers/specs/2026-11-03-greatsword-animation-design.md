# Greatsword animation for The Hollow King

Kort, 2026-11-02/03 (research brief). Branch `work/anim-rework`. Reference: GameDevHero "Great Sword Animation V1 Demo" (youtu.be/65oeK8PbyEk), saved at `C:/Users/kortt/Melty/anim-refs/greatsword/` (frames in `frames/<move>/`, GameDevHero at 10 fps, RamsterZ carry and idle at 2 fps; not committed; details in `SOURCES.md`). Docs only; no clip, Java or sheet was touched.

## Why

The `greatsword` family has no clips at all. Its one troop, the Hollow King, would fall back to the procedural swing today. Kort likes Souls-like weight (Dark Souls ultra greatsword): slow, heavy, planted, big wind-ups, the blade doing the work. The Hollow King is the Graveborn legendary "fear engine", so his swings should read as dread: slow, unstoppable weight, not a quick fencer.

## Goals

- Three loose attacks that match a two-handed greatsword's weight: `greatsword_overhead_cleave`, `greatsword_side_cut`, `greatsword_thrust`.
- A high-guard stance (the family's `guard`) that every swing starts and ends in.
- The weapon faces the move at every key (rule: `docs/ANIMATION-EDITING.md`, "Weapon direction"). Each key below names the blade direction.
- No self-clipping: the long blade never passes through his head, legs, cloak or the ground.
- Each move approved by Kort in game (front, side, 3/4) before it is committed.

## Units

| Troop | Model | Held and worn items | Scale and build | Ability |
|---|---|---|---|---|
| `gb_hollowking` The Hollow King (Graveborn, legendary, infantry, tag `large`) | `wither_skeleton` on the biped rig; skin `minecraft:textures/entity/skeleton/wither_skeleton.png` today (art plan: `gb_hollowking_s1.png`) | Weapon `minecraft:netherite_sword` (stats; art plan: `grave_sword`). Helmet `minecraft:golden_helmet` (unit-identity spec: chainmail coif; stats only). Chest `minecraft:netherite_chestplate`. Shield `none`. | Body scale [1.2, 1.15, 1.15], arms 1.1, legs [1.1, 1.15, 1.1] (the 1.2 / 1.15 figures): source is the design text in `docs/superpowers/specs/2026-10-17-unit-identity.md` line 168 only; no model, Spec row or code carries it (`gb_hollowking.json` does not exist), so it is **unverified until the model lands**. Sword hand: `hand: right` is read from `models/troop/knight.json`, `sentinel.json` and `gl_marshal.json` (line 14 of each); `gb_hollowking.json` does not exist, so his hand is **unverified** and assumed right until it does. **TroopModel `rootScale()`: unknown until the model exists** (`TroopModelLoader` sets it from the rig as (24 - his root pivot y) / (24 - the rig's); about 1.15 if his root sits 15 percent lower, but not measured). Crown and cloak sizes: **not authored yet** (no part JSON); planning budget below. | `dread`: passive aura, radius 10, enemies deal 15% less. No attack visual. |

Planning budget for the long-blade clearance checks (to be replaced by real numbers when the model exists): crown tops at most 4 px above the skull and the cloak hangs at most 14 px below the shoulders, 3 px behind the back. So the blade tip must clear head plus crown by at least 6 px, and any key that swings the blade across the back (the side-cut wind-up) must clear the cloak by 3 px, or the cloak moves. Cloth springs count against the hit-over-attack gate (see Fixed limits).

Stats that matter here: range 3.0, cooldown 30 ticks (1.5 s), damage 10, speed 0.26 (slowest infantry), health 70, slot weight 4.

## Inventory today

Engine view (`TroopMotion.build`: `clips/biped/common/`, then `clips/biped/greatsword/` overriding by name). `clips/biped/greatsword/` does not exist.

| Clip the engine would play | Source today |
|---|---|
| Stances `idle`, `carry`, `guard`, `brace`, `wall`, `shield`, `aim`, `rest` | `common/` has none of them (it holds only death, fall, raise, hit_*). Missing: procedural fallback for all eight. |
| `greatsword_overhead_cleave`, `greatsword_side_cut`, `greatsword_thrust` | Missing: no clip (attackIndex -1), procedural swing. |
| `<id>_wall` | None. Not needed: no shield, no formation hold. |
| `hit_head/body/legs` (+ `_2`, `_3`) | Inherited from `common/`; decided: no greatsword-specific hits in this plan (backlog item, Question 4). Fine for a standing skeleton. |
| `block`, `block_head/legs` | Missing (only sword_shield has them): procedural. Decided: no block in this plan (backlog item); not needed for the fixed attack set. |
| Deaths (`death`, `death_<dir>`, `death_<kind>`, `death_<kind>_<dir>`), `fall`, `raise` | Inherited from `common/` (full set). |
| Scenes, bow draw, shield carry | Not applicable. |

Art dependency (needs Kort): `models/troop/` holds six Gilded troop models only. There is no `gb_hollowking.json`, no `grave_sword` weapon model and no Graveborn skin yet, so the family clips have no model to render on today. Clips can be built and verified without it (`tools/clips.py --verify` is model-free), but the in-game grade render needs the Hollow King model or a stand-in. The stand-in render command is in the plan (Task 0): `solo_marshal` with `GRAVES_GRADE_TROOP=gl_marshal` (the Marshal is the only legendary-size biped model). Without Kort's yes on that, the in-game grade is **blocked until the model exists**. See Questions.

## References

See `anim-refs/greatsword/SOURCES.md`. One video is used for pose reading: GameDevHero's room duel, 0:00 to 0:03 (640x360; the fighter is about 35 px tall, so poses are read for blade direction and high/low, not degrees). RamsterZ "Greatsword Animations for Unreal Engine" (youtu.be/54zN63HgMq8, 3:42, local `ramsterz-greatsword.webm`) is used for the carry and idle only: its attacks (0:14-0:22) are far from the camera with several characters, too small to read. RamsterZ carry and idle frames were extracted this round: `frames/carry` (6-14 s, 2 fps, sheet `frames/carry_sheet.png`) and `frames/idle` (14-17 s, sheet `frames/idle_sheet.png`). The sword-and-shield thrust sheet (`anim-refs/sword-shield/frames/stab_sheet.png`) was opened: the shield hides the sword arm in every frame and the sword is a pale blur, so it gives no usable arm pose and is **not cited**. Two others were checked and not used for timestamps: Mythforge's greatsword montage (staggered fighters, cannot be isolated) and AniBearlee's overview (dozens of labelled characters). The timestamps below come from frames I looked at. The thrust is **unverified against any video**: it is composed from the weapon rule and the Dark Souls greatsword running thrust, and Kort's compare page will show only the game.

## Stances

Conventions for every table below (first-pass numbers, signs as in `clips/biped/sword_shield/*.json`, tuned in Blockbench; **the blade column is the acceptance, the degrees are a start**): rotations in degrees [x, y, z]; right_arm x negative lifts the arm forward and up (knight overhead: -132 at the top), body x positive leans forward; root position in pixels [x, y, z], z negative is forward (knight thrust: -2.7 at impact). Stances are absolute poses; attack tables are deltas over `guard`.

- **`guard` (loose melee; every swing starts and ends here), departing from v1:** the video frames (`frames/shoulder_ready`, GameDevHero 2.2-2.7 s) show the blade **up and back over a shoulder** (which shoulder cannot be read at 35 px; the right shoulder is our choice, matching the sword hand), slanting about 50 deg above horizontal, grip near the right ear, knees bent. v1 said "up and forward over the left shoulder, point toward the enemy"; that came from `2026-10-29-animation-rework-design.md` ("high guard, point toward the enemy") and is dropped because a blade held point-forward at head height crosses his own face on this rig, and the shoulder guard is what the video shows and what the cleave wind-up grows from (shorter lift, clean silhouette). Question 6 asks Kort to confirm. Both hands on the grip, right hand high, left hand a hand-width below.
- **`idle`:** upright. Reference RamsterZ 14-17 s (`frames/idle`, tiles 5-6 at about 16 s): the blade **point-down in front of him, both hands stacked on the pommel at the belt**, point resting near the floor a step ahead of the right foot (not at the thigh, v1 was wrong). The point stays 3 px off the floor and clear of both feet.
- **`carry` (marching, no enemy):** reference RamsterZ 6-10 s (`frames/carry` tiles 1-9): blade resting on the right shoulder, grip hand at the shoulder; at 10-14 s (tiles 10-16, seen from behind and the side) the same carry with the tip up and forward. Departure: in 6-10 s the tip dips past the left shoulder at chest height; ours holds the tip **up and forward about 30 deg above horizontal** to keep it out of his face, the crown and the cloak. The left hand in the video swings free; ours steadies the blade (left hand near the grip) so the carry reads as one weight. The 14-17 s point-down reference is the idle, not the carry (v1 mixed them).
- `brace`, `wall`, `shield`, `aim`, `rest`: not built. No shield and no wall cell (slot weight 4, large). Procedural fallback if the engine asks.

### Key tables (stances, absolute rotations)

| Clip | t (s) | body | head | R arm | R forearm | L arm | L forearm | hands R / L | root | Blade direction |
|---|---|---|---|---|---|---|---|---|---|---|
| `guard` (2.4 s loop) | 0.00 and 2.40 | [8, -8, 0] | [-6, 8, 0] | [-125, -10, -20] | [-95, 0, 0] | [-60, 25, 10] | [-70, 0, 0] | [0,0,0] / [0,0,0] | [0, 1.0, 0] | Up and back over the right shoulder, about 50 deg above horizontal, tip behind and above the shoulder |
| | 1.20 (breath) | [9.5, -8, 0] | [-7.5, 8, 0] | [-127, -10, -20] | [-96, 0, 0] | [-61, 25, 10] | [-71, 0, 0] | same | [0, 1.1, 0] | same, tip bobs about 1 deg |
| `idle` (3.0 s loop) | 0.00 and 3.00 | [2, 0, 0] | [0, 0, 0] | [-30, -4, 6] | [-30, 0, 0] | [-34, 6, -4] | [-36, 0, 0] | [0,0,0] / [0,0,0] | [0, 0, 0] | Point down, a step ahead of the right foot, 3 px off the floor, hands stacked on the pommel at the belt |
| | 1.50 (breath) | [3, 0, 0] | [-1, 0, 0] | [-31, -4, 6] | [-31, 0, 0] | [-35, 6, -4] | [-37, 0, 0] | same | [0, 0.1, 0] | same |
| `carry` (2.0 s loop) | 0.00 and 2.00 | [4, -6, 0] | [-3, 6, 0] | [-100, -5, -15] | [-100, 0, 0] | [-45, 15, 5] | [-40, 0, 0] | [0,0,0] / [0,0,0] | [0, 0.4, 0] | Resting on the right shoulder, edge up, tip up and forward about 30 deg above horizontal |
| | 1.00 (sway) | [4, -3, 0] | [-3, 3, 0] | [-102, -5, -15] | [-100, 0, 0] | [-47, 15, 5] | [-40, 0, 0] | same | [0, 0.5, 0] | same, tip sways about 2 deg |

Legs in `guard`: copy the knight stagger (left_leg [-40,-6,-7], left_shin 41, right_leg [30,8,2], right_shin 14), widened 4 deg; `idle` and `carry` leave legs to the walk and foot planting.

## Moves

Fixed rows (`Spec.ATTACKVARIANTS`, changes need Kort): overhead cleave head slash, impact tick 12 (0.60 s); side cut body slash, impact 12 (0.60 s); thrust body stab, impact 7 (0.35 s). `BattleMath.ATTACK_CLIP_MAX_TICKS` = 40 (2.0 s). Cooldown is 30 ticks, so a 1.7-1.8 s heavy clip is the weight we want, within the limit.

The video's swings take about 0.2-0.3 s from wind-up to impact. Ours get 0.6 s (cleave and side cut): a slow lift, a short hang at the top, then a fast drop (the Souls feel). The thrust keeps 0.35 s.

Attack tables are **deltas over `guard`** (guard: blade up and back over the right shoulder). First and last key are all zero. Notation as in Stances; "R hand / L hand" is the x rotation of the hands (y, z 0).

### `greatsword_overhead_cleave` (1.8 s, impact 0.60). Video: GameDevHero 0:00.7-0:01.0 (`frames/overhead_cleave`: blade high behind the right shoulder at 0.7-0.8, lowest near the front hip at 0.9-1.0)

| t (s) | body | head | R arm | R forearm | L arm | L forearm | R hand / L hand | root | Blade direction |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | 0 / 0 | [0,0,0] | Guard: up and back over the right shoulder |
| 0.20 lift | [-6,0,0] | [3,0,0] | [-25,0,0] | [-10,0,0] | [-20,0,0] | [-10,0,0] | -10 / 0 | [0,0.2,0.4] | Rising toward vertical, tip behind the head |
| 0.36 top (hang) | [-15,4,0] | [8,0,0] | [-50,0,-6] | [-20,0,0] | [-45,0,0] | [-25,0,0] | -30 / 0 | [0,0.4,0.8] | Vertical to tilted back behind the head, edge toward the enemy, tip above and behind the skull and crown (at least 6 px clear) |
| 0.48 mid-drop | [6.5,0,0] | [-2,0,0] | [2.5,0,2] | [5,0,0] | [0,0,0] | [2,0,0] | -35 / -10 | [0,-0.2,-0.8] | Halfway down, blade about 45 deg from vertical, edge forward, tip in front of the face line |
| 0.60 impact | [28,-4,0] | [-12,0,0] | [55,0,10] | [30,0,0] | [45,0,0] | [30,0,0] | -40 / -20 | [0,-0.8,-2.4] | Down, edge first, about 45 deg forward and down, tip at chest height in front of the target, on the front line, both hands at the belt |
| 0.75 follow | [20,-2,0] | [-8,0,0] | [70,0,8] | [34,0,0] | [55,0,0] | [30,0,0] | -30 / -10 | [0,-1.0,-2.8] | Low in front, point at least 3 px above the floor, hands at the thigh |
| 1.20 recover | [6,0,0] | [-2,0,0] | [20,0,0] | [8,0,0] | [15,0,0] | [8,0,0] | -8 / 0 | [0,-0.3,-0.8] | Rising back toward guard, tip coming up and back |
| 1.80 | zeros | zeros | zeros | zeros | zeros | zeros | 0 / 0 | [0,0,0] | Guard |

Body mechanics: lean back about 15 deg for the lift, fold forward 28 deg at impact, head follows the target, root forward 2.4 px (root only; legs unkeyed so foot planting bends the knees). Elbows stay slightly bent so the blade stays in front of the face line. Step check (fix round 2): v2 had the fast part over 3.6 ticks (R arm 105 deg, about 29 deg per tick). The hang now tops out at 0.36 and a 0.48 key splits the drop into two 2.4-tick legs: R arm 52 deg per leg, about 22 per tick, under the one cap below.

### `greatsword_side_cut` (1.7 s, impact 0.60). Video: GameDevHero 0:01.0-0:01.5 (`frames/side_cut`, sheet `frames/side_sheet.png`)

**Direction.** Reading the five frames: the fighter faces screen-right; the blade starts low (1.0), travels level at waist height toward the opponent (1.1-1.2) and ends rising to the vertical on the far side (1.3-1.4). That is a low-to-high cut ending raised, and its handedness cannot be told at 35 px. Ours **departs**: a flat level sweep at chest height, from his right (where the guard blade rests) across the front to his left, edge leading, because the fixed row is a body slash and the weapon rule demands flat, sideways, edge leading. The video's rising ending is kept only as the recovery (the blade rises back to guard). Kort confirms the direction on the compare page (Question 7).

| t (s) | body | head | R arm | R forearm | L arm | L forearm | R hand / L hand | root | Blade direction |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | 0 / 0 | [0,0,0] | Guard: up and back over the right shoulder |
| 0.15 | [2,-12,0] | [0,10,0] | [20,-15,-20] | [10,0,0] | [15,-10,-10] | [8,0,0] | -10 / 0 | [0,0.2,0.3] | Dropping to level on his right, point behind, edge toward the enemy |
| 0.36 top (hang) | [4,-45,0] | [0,38,0] | [45,-25,-40] | [25,0,0] | [40,-20,-25] | [20,0,0] | -25 / -10 | [0,0.4,0.9] | Flat and level at chest height, point behind him (3 px clear of the cloak), edge toward the enemy |
| 0.48 mid-sweep | [6,5,0] | [-1,-3,0] | [27,0,-12] | [42,0,0] | [30,-2,-7] | [35,0,0] | -17 / -5 | [0.5,-0.1,-0.6] | Level, crossing the body line at chest height, edge leading |
| 0.60 impact | [8,55,0] | [-2,-45,0] | [10,25,15] | [60,0,0] | [20,15,10] | [50,0,0] | -10 / 0 | [1.0,-0.6,-1.5] | Level across the front at chest height, **edge leading**, sweeping toward his left past the target, arms extended, hips lead |
| 0.80 follow | [6,60,0] | [-2,-50,0] | [8,20,12] | [50,0,0] | [16,12,8] | [40,0,0] | -8 / 0 | [1.2,-0.8,-1.2] | Level, point past his left side and slightly up, head looking back at the target |
| 1.20 recover | [2,15,0] | [0,-12,0] | [3,5,3] | [12,0,0] | [4,3,2] | [10,0,0] | -2 / 0 | [0.3,-0.2,-0.3] | Rising over the left shoulder and drifting back toward guard |
| 1.70 | zeros | zeros | zeros | zeros | zeros | zeros | 0 / 0 | [0,0,0] | Guard |

Step check (fix round 2): v2 had body y -45 to +55 in 3.6 ticks (about 28 per tick). The top is now at 0.36 and a 0.48 key splits it: 50 deg per 2.4-tick leg, about 21 per tick; the lead-in 0.15 to 0.36 (4.2 ticks) is 33 deg, about 8 per tick.

### `greatsword_thrust` (1.2 s, impact 0.35). Video: **unverified** (none readable)

**Status: reference-free.** One more search this round (ultra greatsword and Dark Souls greatsword thrust showcases) found only wiki move lists and full-game showcase videos with no isolated, readable thrust; none was downloaded. The thrust is composed from the weapon rule with a two-hand grip, and **Kort signs it off in game before it is committed** (plan Task 5 gate). The sword-and-shield thrust frames (`anim-refs/sword-shield/frames/stab_sheet.png`) were opened and cannot be cited (the shield covers the sword arm). The departure from the guard is large: from blade up-and-back to level and pointing forward, about 130 deg, done mostly by the right hand and body twist.

| t (s) | body | head | R arm | R forearm | L arm | L forearm | R hand / L hand | root | Blade direction |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | [0,0,0] | 0 / 0 | [0,0,0] | Guard: up and back over the right shoulder |
| 0.05 | [0.8,-3,0] | [0,2,0] | [6,0,-1] | [4,0,0] | [4,-2,0] | [3,0,0] | -12 / -3 | [0,0.1,0.2] | Starting to swing down, tip still behind |
| 0.10 | [2.4,-9,0] | [-1,7,0] | [18,0,-3] | [12,0,0] | [12,-6,0] | [9,0,0] | -36 / -9 | [0,0.2,0.5] | About halfway: tip swinging down and forward |
| 0.15 draw | [4,-15,0] | [-2,12,0] | [30,0,-5] | [20,0,0] | [20,-10,0] | [15,0,0] | -60 / -15 | [0,0.3,0.8] | Level at the right hip, point forward at the target, elbows tucked, forearms level with the blade |
| 0.35 impact | [12,6,0] | [-8,-4,0] | [35,0,0] | [95,0,0] | [-20,0,0] | [45,0,0] | -15 / -5 | [0,0.7,-3.5] | Level, in line with both forearms, point at the target's chest height, within about 2 deg of the body's forward line, hands at chest |
| 0.50 follow | [10,5,0] | [-6,-3,0] | [30,0,0] | [80,0,0] | [-17,0,0] | [38,0,0] | -13 / -4 | [0,0.6,-3.0] | Extension held a beat, point forward and level |
| 0.80 recover | [4,2,0] | [-2,-1,0] | [10,0,0] | [30,0,0] | [-6,0,0] | [14,0,0] | -4 / -1 | [0,0.2,-1.0] | Pulling back, point rising |
| 1.20 | zeros | zeros | zeros | zeros | zeros | zeros | 0 / 0 | [0,0,0] | Guard |

**Per-tick step check for the 0.15 s draw** (3 ticks; keys at 0.05, 0.10, 0.15 above): R hand steps -12, -24, -24 deg per tick; R arm 6, 12, 12; R forearm 4, 8, 8; body y -3, -6, -6; L arm 4, 8, 8; L forearm 3, 6, 6; L hand -3, -6, -6. Largest single-bone step 24 deg, under the cap. The draw is kept off the full 130 deg by splitting it over hand, body and arms. If Blockbench tuning pushes a bone over 25 deg per tick, add a 0.20 key or ask Kort to move the impact (needs Kort). The 0.15 to 0.35 drive (4 ticks) moves the right forearm 75 deg, about 19 per tick.

**One cap for all three attacks: no bone may move more than 25 deg in one tick (0.05 s)**, measured on the catmullrom-sampled clip at 20 ticks per second (plan Task 1 Step 5 `step_check.py`), not from the keys alone, because the spline can overshoot the key-to-key average. Largest authored legs after round 2: cleave R arm about 22 per tick, side cut body about 21, thrust R hand 24. Any key that breaks the cap gets a split key or a longer leg; moving an impact tick needs Kort.

### Blade direction per key (summary)

| Clip | Wind-up | Impact | Follow-through | Recovery |
|---|---|---|---|---|
| cleave | Vertical to tilted back behind the head, edge toward the enemy | Down, edge first, tip at chest height in front | Low in front, point 3 px above the ground | Rising to guard, tip up and back |
| side cut | Level at chest height on his right, point behind | Level across the front, edge leading | Level, point past his left side | Rising over the left shoulder to guard |
| thrust | Level at the right hip, point at the target | Level, point at the target, in line with both forearms | Level, point still forward | Point rising to guard |

Moves the video lacks: the thrust (composed, unverified). Not planned: taunt, block.

## Application to units

- **The Hollow King (only troop).** The sword is held in the right hand (`hand: right` in troop JSON). The two-hand grip needs the left hand on the grip, and the engine has no two-hand hold, so the left arm and forearm must be keyed to the grip in every key (the guard stance sets it; swings keep it). The grip point is the weapon model's grip transform; `grave_sword` does not exist yet (the Gilded `marshal_sword` has grip offset [0,0,0], rotation [90,45,90] as a model).
- **Proportions.** Arms are 1.1x, so the hands reach a little farther than the rig default; clip angles are degrees, so they port, but the hand distance changes. Judge on the real model.
- **Held item and orientation.** Edge (not flat) leading in the cleave and side cut: the model needs a clear edge and flat. Blade in line with the forearms for the thrust.
- **Wall or formation variants:** none.
- **Clipping risks (check at all 8 angles):**
  - Blade through the head at the cleave wind-up top (the broken crown adds height).
  - Blade into the ground at the cleave follow-through (tip stays above the floor).
  - Blade through the right thigh at the side cut (level at chest height; keep the point out of the leg at follow-through).
  - Blade and the tattered cloak or pennant on the side-cut turn.
  - Blade through the left forearm in the thrust when the grip hands are close.
- **Ability visuals.** `dread` is a passive aura with no attack link; nothing to animate. A pulse on swing would be a later, client-only effect.
- **Gameplay, network, timing:** none needed. Anything that would be is "needs Kort".

## Fixed limits

- Impact ticks stay on `impactTicks` (cleave 12, side cut 12, thrust 7); clips stay within 40 ticks; any timing change needs Kort.
- Every action clip is an offset over the stance (`MotionStack.action`, `Clip.add`): first and last key of every keyed channel is 0, so a swing starts and ends in `guard`.
- `tools/clips.py --verify` must pass. MotionEngineCheck "hit over attack" (`hitOverAttack`, about line 1093; ticks 26-35, step with hit minus step without at most 15 deg per tick; cloth springs count) **runs only the sword_shield fixture** (`realSwordShield` loads `common` and `sword_shield`, and `hitWhileAttacking` always plays attack index 0, the thrust), so a green run says nothing about the greatsword; plan Task 6A adds a greatsword-specific check. The knight's hits had to be shrunk to about 0.4 size to pass; expect the same fight with his cloak and pennant.
- `Spec.java` only through `tools/preflight.py`; after any verify or gradle run, `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- At most 2 Minecraft instances; never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`; no eula.txt; no computer-use on Minecraft. No push, publish or merge.

## Questions for Kort

1. The Hollow King has no troop model, skin or `grave_sword` yet. Is Graveborn model art coming first, or should the clips be built now and graded on a stand-in (the Marshal model with a long sword)? Needs Kort.
2. May `idle` be "blade point down at the ground" (a Souls rest), unlike the knight's relaxed upright idle? Recommend yes.
3. The thrust has no video source. Accept a composed two-hand thrust, or look for a better video?
4. Decided in the plan (Task 6, fix round 2): hits are inherited from `common/`, no block is built, and greatsword-specific hits and a block are a separate backlog item. Kort may overrule.
5. Confirm `brace` and `wall` stay procedural: he is not meant to hold a formation line.
6. `guard` now has the blade up and back over the right shoulder (as the video), not point toward the enemy (`2026-10-29` design). Accept?
7. Side cut sweeps flat from his right across to his left; the video rises to vertical instead. Accept the flat sweep, or mirror it?

## Review

Review v1 (coordinator): **6.2/10**. Coverage 8, reference 5, weapon direction 8, per-unit 5, limits 7, execution 4.

Fix round 1 (docs only), what changed:

1. Key tables for guard, idle, carry, cleave, side cut, thrust: time, bone deltas (body, head, both arms and forearms, hands, root) and the blade direction in the same row.
2. Limits now say hit-over-attack runs only the sword_shield fixture; plan Task 6A adds a greatsword check (15 deg per tick).
3. Units: golden_helmet, netherite_chestplate, rootScale (unknown until the model exists), the 1.2 / 1.15 scale, crown and cloak clearance budget in px.
4. Guard text now matches the video frame (blade up and back over the right shoulder); the departure from the 10-29 design is stated and asked (Question 6). RamsterZ carry (6-14 s) and idle (14-17 s) frames extracted and split; idle corrected to point-down in front; carry gets its own departure note.
5. Thrust: sword-shield frames opened, unusable (shield covers the arm), marked unverified; per-tick step check for the 0.15 s draw added.
6. Side cut direction stated, with why it departs from the video (Question 7).
7. Plan scripts use the absolute scratchpad path with a recreate note, exact `anim_edit.py open`, `run.sh` and `build_compare.py` commands with Expected checklists.
8. Task 0 and Task 6 name the stand-in render command; the in-game grade is blocked until the model exists unless Kort accepts the stand-in.

9. **Fix round 2 (final):** one 25 deg per tick cap for all three attacks (cleave and side cut re-keyed with top at 0.36 and a 0.48 key); thrust marked reference-free with a Kort sign-off gate; `hand: right` and the 1.2 / 1.15 scale cited or marked unverified; guard shoulder softened; Task 6A gets a hit window per clip; stand-in Expected line; thrust compare row written out; hits and blocks decided (inherited, no block, backlog).

v2 7.5 (9/7/8/7/7/7). Fixes in round 2: single 25 deg per tick cap with re-keyed cleave and side cut, thrust reference-free plus Kort gate, hand and scale sources, softer guard shoulder, per-clip hit windows, stand-in Expected line, thrust compare row, hits and blocks decided.

Open: Kort's answers to the seven questions.
