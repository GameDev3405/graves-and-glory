# Scythe troop: animation spec

Kort, 2026-11-03 (request: find good game animation videos, break the moves down, apply them to the units properly). Branch `work/anim-rework`. References saved at `C:/Users/kortt/Melty/anim-refs/scythe/` (SOURCES.md, `frames/<move>/` at 10 fps, `sheets/`; not committed).

## Why

Kort wants Souls-like weight, not historical or Minecraft-mod looks, and the weapon must always face the way the move needs (`docs/ANIMATION-EDITING.md`, "Weapon direction"). The scythe family is one troop, the Grave Reaper, and today it has no clips and no model at all: it fights on the vanilla wither skeleton renderer with a vanilla iron hoe. This spec writes down what the Reaper should do once it has a model, and the one blocker that comes first (Questions 1).

## Goals

- The Reaper swings a two-handed scythe with a long gather and a snap: a wide low reap across the shins and an overhead hook onto the head, both heavy (impact at tick 12), both edge-first with the blade on the move's line.
- No self-clipping: the haft, blade and arms never pass through the head, helmet, chest or legs at any angle, and the wide sweep stays out of neighbours in formation.
- Each move is approved by Kort one at a time, judged in game on the compare page next to the video.

## Units

Troops with `family = scythe` (Spec.TROOPS): one.

| id | name | house | entity | own TroopModel | held item | helmet | chest | shield | HP / dmg | range | cooldown | ability |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| reaper | Grave Reaper (L2 Harvester, L3 Lord of Scythes) | Graveborn | wither_skeleton | no (`art/troops/` has no reaper; `models/troop/index.json` lists archer, gl_marshal, knight, longbow, mage, sentinel) | `minecraft:iron_hoe` | netherite helmet | iron chestplate | none | 30 / 7 | 3.0 | 26 ticks | `cleave_reaper`: each hit also strikes one other enemy within 2 blocks for 57 percent (L2 77 percent; L3 `reaper_l3`: a killing blow heals a quarter of his health) |

Other facts: speed 0.33, slotWeight 1, no pennant, rarity rare, cost 40, training 5.2 s. `rootScale()` does not exist yet (no TroopModel). Once a model exists, `rootK = (24 - its root pivot y) / (24 - the biped rig's root pivot y)` (`TroopModel.rig()`, `TroopModel.java:312-315`); a wither-skeleton-height model (vanilla 2.4 blocks against the husk's 2.0) would scale every root offset in this spec by about 1.2, so 1.8 px forward becomes about 2.2.

## Inventory today

Rig: biped. Folder `clips/biped/scythe/` does not exist. `TroopMotion.build` takes `common/` only.

| Engine slot | Clip | Owner | Notes |
|---|---|---|---|
| stances `idle`, `carry`, `guard`, `brace`, `wall`, `shield`, `aim`, `rest` | none | missing | `common/` holds no stance clips; procedural fallback only |
| attack `scythe_reap` (legs, slash, impact 12) | none | missing | `Fam.attackIndex` is -1, `TroopMotion.java:193` drops the swing |
| attack `scythe_overhead_hook` (head, slash, impact 12) | none | missing | same |
| `*_wall` attacks | none | missing | see Application to units |
| hits `hit_head/body/legs` (+ `_2`, `_3`) | `common/` | inherited | shared with every biped family |
| blocks | none | missing | shield none; the Reaper never blocks |
| deaths (`death*`), `fall`, `raise` | `common/` | inherited | shared |
| scenes | `common/` | inherited | none family-owned |

**Blocker (Questions 1).** `TroopMotion.row(m)` returns null for a model with no troop id (`TroopMotion.java:143`), and the Reaper has no TroopModel, so even a finished clip folder plays nothing in game today. The vanilla wither skeleton renders with the vanilla arm swing. Also, `tools/anim_edit.py` line 29 has `TROOP = {'sword_shield': 'knight', 'bow': 'archer', 'common': 'knight'}`; a `scythe` family needs a `TROOP` entry (`'scythe': 'reaper'`) and a bbmodel copy `art/clips/biped/scythe.bbmodel`. That is a tooling change and needs the coordinator. `test-tools/work/t16r12/live/run.sh` has solo cases for knight, sentinel and marshal only; a `solo_reaper` case is added in the plan.

Action and hit clips are additive offsets over the stance (MotionStack.action); the first and last key of every keyed channel is 0.

## References

1. Reaper Animations for Unreal Engine, Wolff's Studio (https://www.youtube.com/watch?v=uT3M0MZxwS0, 5:39), `uT3M0MZxwS0.mkv`. The only clean side-on scythe set found. Two mannequins fight on a plain stage. Frames on disk: `frames/attack_a` (4.0-6.0 s), `frames/attack_c` (9.3-10.5 s), `frames/idle_stand` (118-121 s), `frames/guard_hold` (123-126 s), `frames/pose_carry` (113-117 s).
2. Elden Ring Weapon Showcase: Scythe, MrIwont4get (https://www.youtube.com/watch?v=CBb-5zS2WBA, 8:23), `CBb-5zS2WBA.webm`. Mood only: heavy wide arcs and long recovery. No timestamp cited (camera and effects hide the poses).

**Kort's picks (2026-11-03) from video 2, these lead over video 1 for the swing:**

| Move | Time | Frames |
|---|---|---|
| normal left swing into right swing | 0:16-0:17 | `frames/kort_swing` (16.0-17.9 s, 19) |
| right swing, second view | 0:34-0:35 | `frames/kort_right_swing` (34.0-35.9 s, 19) |

f_n = start + 0.1 (n-1) s. The moves table is re-keyed from these when the scythe family is built.

Mannequins are slim and quick (about 0.8 s per reap cycle), so timing is retimed to heavy troops. The coordinator list (ANIMATION-REFERENCES.md) names no scythe pack and suggests the Souls greatsword and halberd references for the sweep and slow recovery; use the greatsword family's `side_cut` (GameDevHero / RamsterZ frames, see its spec) as the cross-check for body mechanics.

Not found: a clean overhead hook with a camera that does not pan (9.3-10.5 pans), a march (carry) and any hit reaction on a scythe holder.

## Stances

Grip: the weapon bone is `right_hand` (`hand: right` in the future troop JSON). The haft is about 2.2 m with the blade at the upper end; the **right hand is the upper grip** and the **left hand holds the lower part**, 0.5 m toward the butt. The engine has no two-hand hold, so the left hand is posed onto the haft by clip keys on **every guard and attack key**. Exception: in `idle` and `carry` the left hand is free (idle: hanging relaxed at the hip, left arm [0, 0, 12]; carry: swinging with the procedural march), because the right hand alone holds the haft grounded or on the shoulder. Blade: concave edge on the inside of the curve, tip leading in a swing.

**Conventions.** Rotation degrees x, y, z; stance values are absolute, attack values are deltas over guard. Root position in pixels: forward is -z and **y positive is down** (shipped `guard.json` crouches at root y +1.2..+1.35; shipped `sword_shield_overhead` rises to y -0.8 and lunges to z -1.8 at impact; shipped `sword_shield_low_cut` is y +0.3, z -1.2..-1.4). Arm x negative raises the arm.

**Weapon pitch rule.** The haft is assumed to lie along the forearm line of the right arm (as the sword does in the sword_shield mount; the real mount comes with the weapon part in Task 0, and every hand x below shifts by any mount offset). With `S = body x + right_arm x + right_forearm x + right_hand x` (stance plus delta, absolute), the haft points `S + 90` degrees **below horizontal** along the forearm line (S = -90 is level, S = -180 is straight up, below -180 it leans back). Every Scythe direction below carries its sum. `pose_preview.py` is the final check; the sums are what the numbers give.

**`guard`** (loose melee, loop 3.0 s, catmull-rom, tiny sway). Video: 2:03-2:06 (`frames/guard_hold`: knees bent, both hands on the haft, haft diagonal across the body, blade up past a shoulder; checked at 123, 124, 124.5 s).

| t (s) | body | head | right_arm | right_forearm | left_arm | left_forearm | hands (R / L) | root pos | Scythe direction |
|---|---|---|---|---|---|---|---|---|---|
| 0.0 | 6, 22, 0 | -4, -20, 0 | -62, -10, -14 | -50, 0, 0 | -38, 26, 8 | -70, 0, 0 | -15,0,0 / 0,0,0 | 0, 1.2, 0 | Haft about 30 deg above horizontal, pointing forward, turned left of the target line by the torso twist (+y turns the right shoulder back, so body y +22 faces the torso left; arm y -10 brings it back about 10, net about 12 deg left of the target; sign to verify in `pose_preview.py`, acceptance: blade end forward and within 20 deg of the target line). Butt low near the left hand, blade end up and forward of the head, tip curved toward the target, never across the face. Sum 6 - 62 - 50 - 15 = -121, so 31 above horizontal |
| 1.5 | 7, 23, 0 | -4, -21, 0 | -64, -10, -14 | same | -40, 26, 8 | same | same | 0, 1.4, 0 | Same line. Sum 7 - 64 - 50 - 15 = -122, 32 above horizontal |
| 3.0 | = 0.0 | | | | | | | | same |

Departure from the video: the mannequin keeps the blade over one shoulder behind its head; ours keeps the blade end forward of the head so the swing starts from where the target is and the blade never passes behind the helmet.

**`idle`** (no enemy, still; loop 4.0 s). Video: 1:58-2:01 (`frames/idle_stand`: upright, one hand on the hip, scythe in the other hand at the side with the blade trailing low and back; checked on the zoomed frames at 118, 119, 120 s). Departure: the video's low trailing blade would sink into the ground on slopes and stairs. Ours rests the butt on the ground beside the right foot with the haft near vertical, blade end up and out.

| t (s) | body | head | right_arm | right_forearm | right_hand | left_arm | left_forearm | root pos | Scythe direction |
|---|---|---|---|---|---|---|---|---|---|
| 0.0 | 0, 0, 0 | 0, 0, 0 | -45, 4, -10 | -105, 0, 0 | -30, 0, 0 | 0, 0, 12 | -8, 0, 0 | 0, 0, 0 | Haft vertical, butt on the ground beside the right foot, blade end up and out to the right, tip curved forward. Sum 0 - 45 - 105 - 30 = -180, so straight up (90 above horizontal) |
| 2.0 | 1, 0, 0 | 0, 2, 0 | -47, 4, -10 | -105, 0, 0 | -30, 0, 0 | 0, 0, 12 | -8, 0, 0 | 0, 0.3, 0 | Sum 1 - 47 - 105 - 30 = -181, 1 deg past vertical, still grounded |
| 4.0 | = 0.0 | | | | | | | | |

**`carry`** (marching, no enemy; loop 1.2 s). No video (the checked POSE frames show a display pose). Composed: haft over the right shoulder, blade end up and **behind** so it never reaches the trooper behind. Left arm free (procedural swing; the left-hand exception above). Needs Kort to say yes (Questions 4).

| t (s) | body | head | right_arm | right_forearm | right_hand | root pos | Scythe direction |
|---|---|---|---|---|---|---|---|
| 0.0 | 0, 0, 0 | 0, 0, 0 | -70, 8, -25 | -105, 0, 0 | -25, 0, 0 | 0, 0, 0 | Haft on the right shoulder slanting back and up, blade end behind the head, tip away from the neck. Sum 0 - 70 - 105 - 25 = -200, so the haft is 110 above horizontal measured from forward, i.e. pointing back and up 70 deg above horizontal |
| 0.6 | 3, 0, 0 | 0, 0, 0 | -72, 8, -25 | -105, 0, 0 | -25, 0, 0 | 0, 0.3, 0 | Sum 3 - 72 - 105 - 25 = -199, 109 from forward, back and up 71 above horizontal |
| 1.2 | = 0.0 | | | | | | |

`brace`, `wall`, `shield`, `aim`, `rest`: not authored. `wall` and `brace` are only reached by formation holds; see Application.

## Moves

Both attacks are heavy (impact tick 12, marker 0.60) and thrown from `guard` (key tables are deltas over guard). The clip length is **1.3 s = 26 ticks**, not the 40-tick cap: the Reaper's cooldown is 26 ticks, so the next swing can start the tick the clip ends and a longer clip would be cut into by the next swing (Review Focus 4 in the plan).

Video retime (Wolff 4.0-4.9 s cycle, `frames/attack_a`, `f_001` = 4.0 s):

| Phase | Video | Ours |
|---|---|---|
| Hauled up across the body | 4.6-4.9 (0.3 s) | 0.00-0.50 gather and full wind-up (stretched to show weight) |
| Flat overhead hold | 4.8-4.9, repeated at 4.0-4.1 | 0.50 hold, one key |
| Strike | 4.1-4.3 (0.15-0.2 s) | 0.50-0.60 (0.10 s, the snap) |
| Sweep ends, haft vertical | 4.4-4.5 | 0.80 |
| Settle | 4.6 | 1.05-1.30 |

| Engine clip | Video move (m:ss) | Start / end | Weapon at each key | Body mechanics |
|---|---|---|---|---|
| `scythe_reap` (legs, slash, impact 0.60) | Wolff 0:04.0-0:04.9, the low sweep (checked on `attack_a` f_001-f_009 and the zoom sheet) | guard / guard | wind-up: haft behind the right hip pitched about 15 deg down, blade end out to the right and back; impact: haft 30 deg down, blade tip about 0.3 m above floor; follow-through: haft rising to about 60 deg above horizontal on the left; recovery: back to the guard diagonal | hips and shoulders wind 52 deg back then whip -34; weight back at the gather, root 1.8 px forward at impact, dip y +0.6; legs unkeyed (foot planting) |
| `scythe_overhead_hook` (head, slash, impact 0.60) | Wolff 0:09.8-0:10.2 (`attack_c`, f_006-f_009), camera pans | guard / guard | wind-up: haft tilted back about 35 deg past vertical above the head, blade end up and behind; impact: haft 10 deg below horizontal, blade tip at about 1.5 m, head height; follow-through: 65 deg below horizontal, blade end at knee height | trunk folds forward 16-20 deg, root 1.8 px forward and 0.8 px up (shipped overhead signs) |

Not in the video: a heavier, slower gather, the zone-height end positions (chosen for the Spec zone: legs and head), and the follow-through to guard.

### Key tables (deltas over `guard`; guard sum S0 = 6 - 62 - 50 - 15 = -121)

Absolute pitch at each key is S0 plus the delta sum (body + right_arm + right_forearm + right_hand deltas); the haft is then S + 90 below horizontal.

**`scythe_reap`** (1.3 s, marker `impact` 0.60). Forward is -z; weight shifts back at wind-up (+z). Root z at impact -1.8 equals the shipped maximum (shipped -1.2..-1.8); justified as a heavy swing, flagged for the render.

| t (s) | body | head | right_arm | right_forearm | left_arm | left_forearm | hands (R / L) | root pos | Scythe direction and pitch sum |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0 | 0, 0, 0 | In guard: S = -121, 31 above horizontal |
| 0.20 | 2, 14, 0 | 0, -10, 0 | 8, 8, -10 | -5, 0, 0 | 6, -6, 6 | 10, 0, 0 | 0 | 0, 0.4, 0.5 | Haft rolling back, blade end swinging out right and back. Delta 2 + 8 - 5 = 5, S = -116, 26 above |
| 0.50 | 8, 52, 0 | 4, -44, 0 | 18, 40, -52 | 20, 0, 0 | 14, -30, 10 | 18, 0, 0 | R 0,0,-15 / L 0,0,10 | 0, 0.8, 0.8 | Wind-up: haft behind and right of the body, blade end pointing away from the target, concave edge turned forward. Delta 8 + 18 + 20 = 46, S = -75, so 15 deg **below** horizontal (not level) |
| 0.60 | 20, -34, 2 | -2, 28, 0 | 10, -30, -30 | 16, 0, 0 | -10, -50, 18 | -6, 0, 0 | R 15,0,10 / L 0,0,-10 | 0, 0.6, -1.8 | Impact: haft across the front pointing forward and down, blade tip across the target's shins, concave edge and tip leading. Delta 20 + 10 + 16 + 15 = 61, S = -60, so 30 deg below horizontal. Height calc: right hand at 0.89 m (shoulder 1.45, minus lean 0.05 and dip 0.04 = 1.35; upper arm 0.35 at 26 from down drops 0.31; forearm 0.30 at 60 from down drops 0.15), tip 1.2 m along the haft from the right hand (right hand 1.0 m from the butt of a 2.2 m haft), so tip height 0.89 - 1.2 sin30 = 0.29 m, inside the shin zone 0.2-0.5. Forward: 0.41 (arm) + 1.2 cos30 = 1.45 m from the shoulder axis |
| 0.80 | 6, -52, 2 | 0, 40, 0 | -44, -48, -16 | 8, 0, 0 | -30, -60, 20 | 10, 0, 0 | 0 | 0, 0.4, -1.4 | Follow-through: haft rising on the left, blade end up and left, tip away from the body. Delta 6 - 44 + 8 = -30, S = -151, 61 above horizontal |
| 1.05 | 4, -18, 0 | 0, 12, 0 | -10, -16, -4 | 4, 0, 0 | -10, -16, 4 | 4, 0, 0 | 0 | 0, 0.2, -0.5 | Haft returning toward the guard line. Delta 4 - 10 + 4 = -2, S = -123, 33 above |
| 1.30 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0 | 0, 0, 0 | In guard |

Peak step 0.50 to 0.60: body about 86 deg in 2 ticks (y goes 52 to -34), so up to about 43 deg/tick on the body alone. That is the attack's own speed; the hit-over-attack gate only measures what a hit adds.

**`scythe_overhead_hook`** (1.3 s, marker `impact` 0.60). Root signs copy shipped `sword_shield_overhead`: sinks (y positive, z back) in the wind-up, rises and lunges (y -0.8, z -1.8) at impact.

| t (s) | body | head | right_arm | right_forearm | left_arm | left_forearm | hands (R / L) | root pos | Scythe direction and pitch sum |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0 | 0, 0, 0 | In guard: S = -121, 31 above |
| 0.20 | -4, 8, 0 | 0, -4, 0 | -35, 6, -10 | -8, 0, 0 | -25, 0, 8 | -5, 0, 0 | 0 | 0, 0.2, 0.4 | Haft rising toward vertical in front of the chest, blade end up. Delta -4 - 35 - 8 = -47, S = -168, 78 above |
| 0.50 | -14, 14, 0 | 8, -12, 0 | -50, 10, -18 | -20, 0, 0 | -60, -6, 14 | -15, 0, 0 | R -10,0,0 / L -5,0,0 | 0, 0.4, 0.8 | Wind-up: haft above the head tilted back about 35 deg past vertical, blade end up and behind, tip hooked down behind the head (4 px clear of the nape). Delta -14 - 50 - 20 - 10 = -94, S = -215, so 125 from forward (35 past vertical) |
| 0.60 | 4, -8, 0 | -6, 4, 0 | -50, -4, -10 | 15, 0, 0 | -10, -6, 10 | 30, 0, 0 | R 72,0,0 / L 20,0,0 | 0, -0.8, -1.8 | Impact: haft pointing forward and slightly down, blade tip at head height of the target, tip hooked down and forward, concave edge facing the target's head. Delta 4 - 50 + 15 + 72 = 41, S = -80, so 10 deg below horizontal (the haft is not along the forearm here: the hand x +72 flicks the wrist, 87 deg from the wind-up in 2 ticks, which is the hook). Height calc: absolute chain body 10, arm -112, forearm -35 gives S_forearm = -137 so the forearm is 47 above horizontal; shoulder 1.43 (lean), upper arm 0.35 at 12 above horizontal adds 0.07, forearm 0.30 at 47 above adds 0.22, right hand at 1.72 m; tip 1.2 m along the haft: 1.72 - 1.2 sin10 = 1.51 m, inside the head zone 1.4-1.9 (husk height assumed). Forward: 0.55 (arm) + 1.2 cos10 = 1.7 m from the shoulder axis |
| 0.80 | 20, -6, 0 | -8, 4, 0 | 30, -4, -6 | 40, 0, 0 | 0, -6, 6 | 34, 0, 0 | R 6,0,0 / L 25,0,0 | 0, -0.3, -0.8 | Follow-through: blade end at knee height ahead, butt up near the chest. Delta 20 + 30 + 40 + 6 = 96, S = -25, so 65 below horizontal |
| 1.05 | 8, -3, 0 | -3, 2, 0 | 10, 0, -2 | 14, 0, 0 | 4, 0, 2 | 12, 0, 0 | 0 | 0, 0, -0.1 | Haft returning toward the guard line. Delta 8 + 10 + 14 = 32, S = -89, about level (1 below) |
| 1.30 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0, 0, 0 | 0 | 0, 0, 0 | In guard |

The left hand follows the haft: at each key its arm and forearm values put it 0.5 m below the right hand on the same line (checked in `pose_preview.py`; the left chain is not summed here because its shoulder is on the other side).

**`scythe_reap_wall` and `scythe_overhead_hook_wall`** (BLOCKED on Kort's answer to Questions 3; no key table until the `wall`/`brace` holds exist, because the deltas depend on that start pose). Thrown from `wall` or `brace`, so no formation hold exists to start from (those two stances are not authored; formation uses the procedural pose or `guard`). The proposed shape: the same keys with body yaw (y) and arm abduction (z) scaled by 0.35 and the blade end kept within 0.9 m of the body line, so the reap becomes a short forward hook (blade end swings in front of the troop's own cell, not across the neighbours) and the hook stays vertical.

**`hit_*`, `block*`, deaths.** Not authored here; inherited from `common/` (Questions 2).

## Application to units

- **Reaper (the only unit).**
  - Today it has no model, so no clip plays (Blocker above). A TroopModel is an art task (`art/troops/reaper.bbmodel` plus `reaper.json`, a `scythe` weapon part and `scythe_t2`, `scythe_t3` tier variants like `arming_sword_t2`, a `models/troop/index.json` entry, `anim_edit.py` and `run.sh` entries). `docs/ART-STYLE-GUIDE.md` 7.3 puts the scythe among weapons the artist invents; `PARKED.md` records Graveborn art as deferred. Needs Kort.
  - Loadout from Spec: netherite helmet, iron chestplate, no shield, no pennant. Helmet bulk raises two clipping risks: the wind-up of `scythe_overhead_hook` (haft above and behind the head, tip hooked down behind it; keep 4 px off the nape and helmet back) and the guard (blade end forward of the head, never across the face).
  - Wither skeleton height (2.4 blocks vs the husk's 2.0) lengthens the haft's reach on screen; scale root offsets by `rootScale()` once it exists. Hands and haft sizes come from the weapon model; the 2.2 m haft and 0.5 m blade here are proposals.
  - Reach: range is 3.0 blocks. The height calcs above put the blade tip about 1.5 m (reap) and 1.7 m (hook) from the shoulder axis at impact, short of 3.0 by about 1.3-1.5 m even with the right hand 1.0 m from the butt of a 2.2 m haft. The clip cannot close that gap (root lunge is under 2 px); only a longer weapon model, or Kort accepting a visibly short reach, can. Pass threshold for the render: blade tip within 0.3 m of 3.0 at impact (Questions 6). Both numbers are calculations from assumed rig sizes (shoulder 1.45 m, arm 0.35 m, forearm 0.30 m), not measurements.
  - Ability visuals: `cleave_reaper` hits a second enemy within 2 blocks. The wide reap (the blade end sweeping about 180 deg) shows that read. Visuals only; no gameplay, network or timing change is needed.
- **Formation.** A 2 m horizontal sweep clips the neighbours on the 0.7-block wall cell. The Reaper (group infantry) may stand in lines; `wall`/`brace` stances are not authored, so it keeps `guard` or the procedural pose. Needs Kort (Questions 3).
- **Cooldown.** 26 ticks equals the clip length (1.3 s). Chained swings must start from guard with every channel at 0 (Review Focus 4).
- **Needs Kort (gameplay, network, timing).** None of this spec changes them. A model, a weapon part and the optional `_wall` variants are art or scope decisions.

## Fixed limits

- Attack rows are fixed: `scythe_reap` (legs, slash, impact 12, heavy) and `scythe_overhead_hook` (head, slash, impact 12, heavy); clip at most 40 ticks. Any change needs Kort. Marker `impact` stays at 0.60.
- First and last keys of every keyed channel are 0 (attacks) and loops close (stances).
- `hit over attack` (`MotionEngineCheck.hitOverAttack`, line 1093, helper `hitWhileAttacking`, line 1057): the fixture is `realSwordShield` only (family sword_shield, stance `hold`, shield on, variant 0 = the sword_shield thrust, starting at tick 20 with a body hit at tick 25). It never plays this family's clips, so it does not cover the scythe. Its 26-35 tick window would, for a scythe attack started at tick 20, cover attack ticks 6-15 (0.30-0.75 s), which includes the impact at tick 12, so the window is the right place to look; the plan adds a scythe fixture rather than relying on the existing gate. Cloth springs (cape, straps, tabard) count in that gate; the Reaper's model may add some.
- `python tools/clips.py` then `--verify`; `Spec.java` only through `tools/preflight.py`; after any verify or gradle run `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- Max 2 Minecraft instances; never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`; no eula.txt; no computer-use on Minecraft; no push, publish or merge.

## Questions for Kort

1. Build a reaper TroopModel with a scythe weapon part (art task) so the clips can play at all? Everything else depends on it. Until then these are paper specs only.
2. Should the Reaper get its own `hit_*` clips (keep the haft in both hands through a hit), or stay on the shared `common/` hits? Recommended: check first on the render, add only if the haft is flung off the hands.
3. Add `scythe_reap_wall` and `scythe_overhead_hook_wall` plus a tight `wall`/`brace` hold, so the sweep never crosses neighbours in formation (recommended), or keep the plain swings?
4. Is the composed `carry` (haft on the right shoulder, blade end up and behind) OK without a video, or look for a march reference?
5. The Reaper's Elden-Ring-style hook has no clean video; accept the composed hook and judge it in game?
6. Reach: the calc puts the blade tip about 1.5-1.7 m out at impact against range 3.0. Lengthen the scythe model (about 3.3 m total would be needed), or accept a visibly short reach?

## Review

v1 7.0 (8/7/7/6/8/6). Fix round 1: root y sign corrected (y positive is down), every direction now has its stance+delta pitch sum with hook and reap numbers changed to match, left-hand exception for idle/carry stated, root z held to the shipped range, carry table added.

v2 7.3 (8/7/7/7/8/7). Fix round 2: hook impact raised to 10 deg below with a wrist flick and hand-height plus haft-length calcs for hook and reap (reap impact now 30 below), guard direction reworded, reach threshold and reach shortfall added, wall variants marked blocked on Question 3, plan fixes for Task 0, 1, 4, 6.

Final (after fix round 2): 7.5 (8/7/8/7/8/7). Known gaps for Kort: reach is about 1.5-1.7 m against range 3.0, so pick a ~3.3 m scythe or a short-reach look (Q6); there is no reaper TroopModel, so nothing plays until Q1; the `_wall` variants and wall/brace holds have no keys (Q3); the left hand on the haft is unverified; the hook and carry have no clean video; the reap lands at shin height while the video sweeps at hip height; the Task 6 Expected git status is incomplete.
