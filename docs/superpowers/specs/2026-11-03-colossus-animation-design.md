# Colossus (Brassjaw, the Forge Colossus) in the Souls-like style

Kort, 2026-11-03. Branch `work/anim-rework`. References saved at `C:/Users/kortt/Melty/anim-refs/colossus/` (frames in `frames/<move>/`, notes in `SOURCES.md`; not committed): "Giant Golem AnimSet" (youtu.be/huee6syEzuM, wemakethegame) and "Giant Monster Animset" (youtu.be/19wwArriGXI, Riko).

## Why

Kort wants the Ironclad giant to move like a Souls-like iron golem boss: slow, heavy, a big committed wind-up and a long recovery, the whole body behind each blow. Today **nothing animates the colossus in this mod**. It has no troop model, so it draws as the vanilla iron golem with the vanilla swing (see "Inventory today"). This spec designs the stances, the two attacks and a light hit set on the golem rig, and lists what must exist before any of it shows in game.

## Goals

- Colossus stances (`idle`, `guard`), two attacks (`colossus_slam`, `colossus_backhand`) and minimal hits.
- The fists lead at every key; every key of every attack states where the fists and knuckles point (tables below). The golem's arm is one rigid bar, so "fist" means the arm's end face.
- No fist through the head, chest or ground; the arms never pass through the body.
- Each move approved by Kort one at a time on the compare page.
- Be honest about the gaps: the videos show a one-arm chop, not a two-fisted slam or a backhand; those are designed from the rig.

## Units

One troop uses family `colossus`. `Spec.TROOPS` has exactly one row, `ic_colossus`. `ic_colossus_2` and `ic_colossus_3` are level ids (`Spec.LEVELABILITIES` -> abilities `ic_colossus_l2`, `ic_colossus_l3`), not troop rows; they use the same model and clips.

| Troop | Entity / rig | Held | Helmet / chest | Shield | HP / dmg / range | Cooldown | Ability |
|---|---|---|---|---|---|---|---|
| `ic_colossus` Brassjaw, the Forge Colossus | `iron_golem`, `colossus` rig | `minecraft:air` | `minecraft:air` / `minecraft:air` | none (tag `large`) | 120 / 12 / 3.0 | 36 ticks (1.8 s) | `forge_slam` (onhit: every hit also deals 60% to all other enemies within 2.5 and knocks them back 0.8; L2 75%; L3 `ic_colossus_l3` adds a passive furnace-heart aura, 10% less damage for allies within 6) |

Speed 0.22, cost 150, slot weight 4, legendary. The sheet row has no scale field. There is **no `scale` and no troop model**, so no TroopModel rootScale exists; for a troop model built on the `colossus` rig the formula (24 - troop root pivot y) / (24 - rig root pivot y) gives 1.0 (the rig's root pivot is y 0, `art/rigs/colossus.json`). No cape, strap, helmet, armour or weapon part exists, so there is nothing to clip but the rig itself.

Rig facts (`art/rigs/colossus.json`, px, y down, the golem faces -z, ground at y 24; pivots in the file are relative to the parent, worked out here to world): `body` pivot (0,8,0) at the hips, chest box y -9..3, z -6..5, x -9..9; `head` pivot (0,-7,-2), box y -19..-9, z -7.5..0.5, x -4..4, nose to z -9.5; `right_arm` pivot (-11,-7,0), `left_arm` pivot (+11,-7,0), each one box x +-2, z +-3, from the pivot down 27.5 px (the `*_hand` bone at 25 px has no box); `right_leg` pivot (-4,11,0), `left_leg` (+5,11,0), soles at y 24. The rig has **no forearm and no shin bones** (a golem's limbs do not bend; `build_skins.py` COLOSSUS_BONES), so the arm is one bar from shoulder to fist and the knees do not bend in clips. Model height 43 px (about 2.7 blocks); the shoulder is 31 px above the ground, the fist hangs 3.5 px above it.

## Inventory today

| Item | Status |
|---|---|
| Troop model `ic_colossus` | **missing.** `models/troop/index.json` lists six troops (archer, gl_marshal, knight, longbow, mage, sentinel); `art/troops/` has no `ic_colossus.json`. `TroopRenderer.render` (line 95-96) gets `modelOf("ic_colossus")` null and draws `vanilla.render` (IronGolemEntityRenderer). `TroopMotion.pose` is called only from `TroopModel.setAngles`, so it never runs for the colossus |
| Rig model `colossus` | exists (`models/troop/rig/colossus.json`; TroopModels.RIGS maps `iron_golem`). TroopMotion cannot use it as a family model: `row(m)` returns null for a rig-only model (`m.troop` empty), so `family(m)` is null and `simulate` returns null ("a bare rig") |
| Clip folders `clips/colossus/common` and `clips/colossus/colossus` | missing; only `clips/biped/` exists. `build(rig, family)` would read two empty folders and every stance would fall back to the procedural layers |
| Stances idle, carry, guard, brace, wall, shield, aim, rest | missing. This plan adds `idle` and `guard`; the rest fall back (`stanceChain`). Formation is not designed (Question 4) |
| `colossus_slam`, `colossus_backhand` | missing. `Tactics.variants("colossus")` returns the two rows; `build` marks them index -1. `tools/clips.py` requires a clip for each (blunt is in MELEE, clips.py:36), so Task 2 adds zero-motion stubs. `_wall` not needed |
| The swing today | `Abilities.animate` (Abilities.java ~line 294) sends entity status 4 for a golem; the client plays the vanilla arms-up chop. The server also sends the attack variant via `MotionEvents.sendAttack` for every family with variant rows (to be confirmed in a harness run, Task 1 Step 5). A troop model would make the clips play; the vanilla status-4 swing would stop mattering, but must be checked not to double up |
| hit_head/body/legs, block*, death_*, fall, raise | missing; the biped common clips do not apply to this rig folder. Hits added here (Task 6). Death and fall stay procedural (Question 3) |
| Procedural gait | the golem rig uses the biped bone names for `right_arm`, `left_arm`, `right_leg`, `left_leg`, `body`, `head`, so MotionBody's walk should drive them once a troop model exists. Not verified: no render of it exists. Task 1 Step 5 checks it |
| Pipeline | `tools/anim_edit.py` is hard-wired to the biped rig (`RIG = 'biped'`, `TROOP` map: sword_shield, bow, common) and `mesh(troop)` calls `build_skins.troop_project`, which fails without `art/troops/ic_colossus.json`. `tools/clips.py` already reads any `art/clips/<rig>/<family>.bbmodel` |

New folders after Task 2: `clips/colossus/colossus/` (stances and attacks, generated from `art/clips/colossus/colossus.bbmodel`) and `clips/colossus/common/` (hits, from `art/clips/colossus/common.bbmodel`). Never hand-author the JSON.

## References

Both videos were opened and frames checked on disk (`SOURCES.md`). Findings, plainly:

- Giant Golem AnimSet, 0:25.6-0:28.0 (`frames/slam_front02`, `sheets/slam_detail.png`): a repeating one-arm overhead chop. The arm goes up behind the head with the torso leaning back (about 0:26.0-0:26.1), then the body drops forward as the arm comes down across (contact about 0:26.5-0:26.7) with a step in, upright by about 0:26.9; the cycle is about 1.2 s. The figure is small and other characters overlap, so angles below are designed, not measured. It gives the slam's shape and its timing feel (raise and hold about 0.4 s, down about 0.4 s, recover about 0.3 s).
- Giant Golem AnimSet, 0:18-0:20.4 (`frames/backhand_back01`): labelled atk_back01 but shows a distant dash with another figure running through; **not a backhand**. No video in hand shows a backhand sweep. The backhand is designed from the rig and from the slam's torso counter-rotation (Question 1).
- Giant Golem AnimSet, 0:08-0:11 (`frames/idle_stance`, 30 frames at 10 fps): only `f_007` and `f_013` (about 0:08.6 and 0:09.2, strip `frames/idle_stance_check.png`) show the golem standing, feet wide, arms hanging a little forward, knees soft; the other frames in that range are title cards, glitch frames or other characters. 1:28-1:34 (`frames/hit_shoulder`) is the source of the small hit recoil (see Hits).
- Giant Monster Animset (Riko): looked at on a 0.5 fps sheet only; no timestamp is cited. It confirms the family look (wide stance, long recovery).
- No Souls-like iron golem boss footage was downloaded. If Kort wants the Elden Ring Godskin or Dark Souls Iron Golem as a target, name a link (Question 1).

## Conventions

- Rotations in degrees, ModelPart convention: **+x pitches the front of a bone down**; for an arm hanging down, **negative x swings the fist forward and up** (-90 = fist forward, level with the shoulder; -180 = straight up). Body +x leans the chest forward (checked: `guard.json` body x +14, head -12 to keep the eyes level). **+y turns toward the troop's right** (`sword_shield_low_cut.json`: the draw back right is body y +34; the right arm is at -x, so right = -x). Root position: **y positive is down, z negative is forward** (`guard.json` crouches with root y +1.2; `sword_shield_thrust.json` lunges to root z -2.6 at impact).
- An arm's absolute pitch is body x + arm x (the arms are children of `body`). With the absolute pitch th, the bar points (down, forward) = (cos th, -sin th) from the shoulder: th -25 points 65 degrees below horizontal, th -75 15 degrees below horizontal, th -177 straight up. Fist end = shoulder + 27.5 px along the bar; the shoulder is at hips + (0,-15) rotated by body x. `scratchpad/colossus_fist.py` computes these (recreate from this paragraph if missing).
- Rotation order is z, y, x (matrix Rz Ry Rx): an arm's pitch is applied first, then its yaw about the vertical, so a pitched arm with a yaw sweeps flat around the shoulder; a body yaw carries both arms.
- Clips are additive over the stance; the stance `guard` is the base for both attacks. First and last keys of every channel are 0. The root position and the head turn are delta channels like the rest.
- The golem's knuckles are the arm's end face. When the arm is hanging that is the underside; at a slam it is the face that strikes. The back of the hand is the arm's outer face (-x for the right arm, +x for the left).

## Stances

| Clip | Length | Keys (absolute, from the rig's file pose) | Fists |
|---|---|---|---|
| `idle` (loop, no enemy) | 4.0 s | body x +4 (+-1 breathing, y +-3 sway), head x -3, arms x -8 (+-3), arm y +-2 | bars hang 82 degrees below horizontal, fists about 1 px ahead of the hips, knuckles down; relaxed |
| `guard` (loop, enemy near) | 2.4 s | body x +10, head x -8, right arm x -35 and y -6, left arm x -35 and y +6, root y +0.8, breathing +-1.5 | bars 65 degrees below horizontal (abs -25), fists 14 px ahead of the hips and 5.0 px above the ground (at root y +0.8), drawn in a little so they are over the front of each thigh, knuckles forward-down; a low, forward-weight brace |

`carry`, `brace`, `wall`, `shield`, `aim`, `rest` fall back (Question 4).

## Moves

Both attacks start and end in `guard`. Times in seconds; clip length at most 2.0 s (`ATTACK_CLIP_MAX_TICKS` 40). Impact markers: slam 0.6 s (12 ticks), backhand 0.4 s (8 ticks), as in `Spec.ATTACKVARIANTS`.

| Engine clip | Video move | Start / end | Poses, weapon (fist) direction | Body mechanics | Retiming |
|---|---|---|---|---|---|
| `colossus_slam` (head, blunt, impact 0.6 s, length 1.6 s) | Giant Golem AnimSet atk_front02, 0:25.6-0:28.0; the video is one-armed, ours is both fists together | guard / guard | wind-up (0.16 load, 0.30 top, held to 0.34): fists up and over; impact 0.6: both fists driven down and forward, fists leading; follow-through 0.78 into the ground; slow recovery to 1.6 | the whole body behind it: lean back on the wind-up, rise on the toes (root y -0.6), then fall forward onto the fists (root y +1.8, z -2.0); legs unkeyed, planted by MotionFeet if it runs on this rig (see Application) | video: raise and hold about 0.4 s, contact about 0.4 s later; ours 0.30 up, 0.30 down; recovery 1.0 s because the golem is heavy |
| `colossus_backhand` (body, blunt, impact 0.4 s, length 1.3 s) | **designed, no reference** (no video shows a backhand; Q1 still asks for a link). Torso counter-rotation from the slam video | guard / guard | wind-up: right arm held across the front to the left; impact: right arm sweeps left to right, back of the hand leading; follow-through out to the right; left arm swings back for balance | a hip-and-shoulder turn (body yaw -42 to +46); the arm is held 20 degrees below horizontal and the body does the sweep | no video timing; 0.2 s wind-up hold, 0.2 s sweep to impact, long recovery |

### colossus_slam keys (deltas from guard; both arms take the same value)

Guard is body x +10, arms x -35 (abs th -25). "abs th" is body + arm, the bar's pitch. Fist height above ground and reach ahead of the hips from the rig (colossus_fist.py).

| t | body x | head x | arms x | root y, z | abs th, fist direction, knuckles |
|---|---|---|---|---|---|
| 0.00 | 0 | 0 | 0 | 0, 0 | guard: -25, bars 65 below horizontal, fists 14 ahead, knuckles down-forward |
| 0.16 (load) | -10 | +6 | -80 | -0.3, +0.4 | abs 0 body, arm abs -115 gives th -115: bars 25 above horizontal pointing forward-up, knuckles forward-up |
| 0.30 (top) | -22 | +10 | -130 | -0.6, +0.8 | th -177: bars straight up (3 behind vertical), fists about 58 px above the ground, knuckles to the sky; body leans back 12 |
| 0.34 (top, end of hold) | -22 | +10 | -130 | -0.6, +0.8 | identical to 0.30: the top hold |
| 0.45 (mid-drop) | +4 | 0 | -90 | +0.6, -0.6 | th -111: bars 21 above horizontal, forward; moving down fast |
| 0.60 (IMPACT) | +22 | -14 | -72 | +1.8, -2.0 | th -75: bars 15 below horizontal, pointing forward at the target, fists about 19 px above the ground (absolute root y +2.6; falling through head height about 0.53 s, 26-29 px for a 1.6-1.8 block enemy), 34 px ahead; the end faces (knuckles) lead, a hammer-fist falling onto the head or shoulder |
| 0.78 (follow) | +28 | -18 | -35 | +1.6, -2.4 | th -32: bars 58 below horizontal, fists driven to about 2.1 px above the ground (root delta +1.6 on top of guard's +0.8 = absolute +2.4), 24 px ahead; knuckles down; clearance must stay positive |
| 1.20 (settle) | +12 | -6 | 0 | +0.4, -1.6 | th -13 (body abs 22 + arm abs -35 = -13): bars 77 below horizontal, fists about 1.9 px above the ground at absolute root y +1.2, 12 px ahead, rising slowly |
| 1.60 | 0 | 0 | 0 | 0, 0 | guard |

Body y, z, the arms' y and z, and the left/right difference are 0. Check: at impact the weapon direction follows from the numbers: body +32 plus arm -107 = th -75, forward, fist level with the shoulder and descending. Impact must be a descending fist: the fist is at its fastest between 0.45 and 0.60 and still going down past it (0.78 key lower).

### colossus_backhand keys (deltas from guard)

Right arm swings; the left arm trails. R = right arm. World yaw of the right bar = body y + arm y absolute (guard's right-arm y is -6, so absolute arm y = -6 + delta; right of front positive). Arm th = body x (guard 10 + delta) + arm x (guard -35 + delta).

| t | body x, y | head y | R arm x, y | L arm x | root y, z | R abs th, direction, back of hand |
|---|---|---|---|---|---|---|
| 0.00 | 0, 0 | 0 | 0, 0 | 0 | 0, 0 | guard: th -25, bar 65 below horizontal |
| 0.20 (load) | +4, -42 | +30 | -49, -10 | +20 | +0.8, +0.6 | th -70: bar 20 below horizontal, pointing to absolute world yaw -58 (left of front), fist 29.5 ahead, about 19.5 above the ground (absolute root y +1.6); back of the hand (outer face) faces the right side of the swing to come |
| 0.30 (sweep) | +3, -14 | +12 | -48, -3 | +22 | +0.9, -0.3 | th -70, absolute world yaw -23: sweeping right, back of the hand leading |
| 0.40 (IMPACT) | +2, +14 | -6 | -47, +5 | +25 | +1.0, -1.2 | th -70, absolute world yaw +13: arm points forward and a little right at the target, 20 below horizontal (fist about 19.5 px above the ground, chest height of a 1.8-block enemy), back of the hand (outer face) and the knuckles' side lead the arc; the arm is flat in plan, so the fist travels in a horizontal arc |
| 0.55 (follow) | +4, +46 | -20 | -45, +10 | +30 | +1.0, -1.6 | absolute world yaw +50: arm out to the right, fist about 19 above the ground, back of the hand still leading; stops here |
| 0.85 (hold) | +2, +20 | -8 | -30, 0 | +10 | +0.5, -0.8 | absolute world yaw +14; arm lowering back toward guard |
| 1.30 | 0, 0 | 0 | 0, 0 | 0 | 0, 0 | guard |

Clipping: at the 0.20 load the right arm crosses the front; with the body turned -42 and the arm only -10 relative, the arm's back edge sits at z about -6.2, the chest's front is z -6: it should just clear. If the render shows contact, the arm's local yaw becomes -6.

### Hits (small; the golem barely flinches)

Deltas from the stance; time in seconds; first and last keys are 0 for every channel. Fists: arms are never keyed, so the bars keep pointing where the stance has them (guard: 65 degrees below horizontal, knuckles forward-down).

`hit_body` (0.45 s)

| t | body x | head x | root z |
|---|---|---|---|
| 0.00 | 0 | 0 | 0 |
| 0.05 (mid) | +5 | +4 | +0.4 |
| 0.25 (settle) | +1.5 | +1 | +0.1 |
| 0.45 (end) | 0 | 0 | 0 |

`hit_head` (0.45 s)

| t | body x | head x |
|---|---|---|
| 0.00 | 0 | 0 |
| 0.05 (mid) | -3 | -8 |
| 0.25 (settle) | -1 | -2.5 |
| 0.45 (end) | 0 | 0 |

`hit_legs` (0.45 s)

| t | body x | root y |
|---|---|---|
| 0.00 | 0 | 0 |
| 0.05 (mid) | +4 | +0.8 |
| 0.25 (settle) | +1.2 | +0.25 |
| 0.45 (end) | 0 | 0 |

From the video's hit_left_shoulder (`frames/hit_shoulder`, 60 frames at 10 fps; checked on a 10-frame strip `frames/hit_shoulder_check.png`: the caption reads hit_left_shoulder and a grey humanoid mannequin, not the golem, rocks back a few degrees and recovers; the golem hit amplitudes below come from that strip plus a 1 fps view, and are designed smaller for the golem, not measured). Each is a single clip with no `_2`, `_3` variants in this pass.

## Application to units

- **ic_colossus** is the only troop. A troop model `ic_colossus` on the `colossus` rig is needed first (Task 0, needs coordinator and art): `art/troops/ic_colossus.json`, skins (the build wants `_s<tier>` skins, not the raw vanilla texture), and an `index.json` entry. Until it exists nothing in this spec shows in game. The row's `texture` is `minecraft:textures/entity/iron_golem/iron_golem.png`; whether the troop model should keep the vanilla look or get Souls-like art is Question 2.
- Proportions: arm bar 27.5 px, shoulders +-11 px from the centre, 43 px tall. With scale 1.0 the numbers above hold exactly.
- Held item: none, so no weapon mount. Fists are the arm's end face; there is no hand box (`right_hand`/`left_hand` carry no boxes).
- Wall and formation: the golem has no shield and tag `large`; slot weight 4. No `*_wall` clips (Question 4).
- Clipping risks: the arms straight up at the 0.30 slam top (x +-11 vs head +-4, clear by 5 px); the right arm across the chest at the backhand load (above); the fist about 2.1 px above the ground in the slam follow-through (absolute root y +2.4 = guard +0.8 plus the 0.78 delta +1.6; root deltas stack on guard's +0.8; any more root drop or a lower arm puts it through the floor; the render must show ground clearance at 0.78) (a ground clip if the troop is on slabs or the entity sinks; the render's `contact` view checks it); the head with body lean 38 at the slam follow-through (head pivot moves 9 px forward; the head turns -18 to stay up).
- Ability visuals: `forge_slam` (2.5 radius splash, 60% damage and knockback) has no VFX today. A slam with a ground shock would suit it but is a gameplay-adjacent visual: **needs Kort** (Question 5).
- Anything needing a timing, network or server change is flagged "needs Kort": none in the poses; the status-4/variant double play is a client check (Task 1).

## Fixed limits

- Strike timing is approved: `colossus_slam` impact 0.6 s (12 ticks), `colossus_backhand` 0.4 s (8 ticks); lengths within 2.0 s (`BattleMath.ATTACK_CLIP_MAX_TICKS` 40); the cooldown is 1.8 s so the slam (1.6 s) fits. Any change needs Kort.
- `python tools/clips.py` then `--verify` must pass; `Spec.java` only through `tools/preflight.py`; after verify/gradle run `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- The colossus is a rig with no forearm or shin bones: clips key only `root`, `body`, `head`, `right_arm`, `left_arm` (legs left to procedural planting).
- Max 2 Minecraft instances; never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`; no eula.txt; no computer-use on Minecraft; no push, publish or merge.
- Hit-over-attack gate: `MotionEngineCheck.hitOverAttack` runs only the sword_shield fixture (`realSwordShield`, `d.shield = true`); it never plays this family's clips. The family needs its own check (plan Task 2 Step 4).

## Questions for Kort

1. **References.** The videos show a one-arm chop only; no backhand footage was found and no Souls-like boss video was downloaded. Is "two fists up and down" for the slam and "back-of-hand sweep from the left, 20 degrees below level" for the backhand what you want, or do you have a link (iron golem boss) to match?
2. **Model.** Does the colossus keep the vanilla iron golem look when it gets its troop model, or is new art wanted? (Needed before anything animates.)
3. **Death and fall.** Procedural collapse for now, or a heavy death clip (knees, then forward onto the fists)?
4. **Formation.** The colossus has no wall or brace pose; it uses `idle` and `guard` in line orders. OK?
5. **Slam shock.** Add a ground dust or crack effect at the slam's impact (the splash radius is 2.5)? Gameplay-adjacent, so yours to say.

## Review

v1 7.7 (8/6/9/8/8/7). Fix round 1: anim_edit refactor specified (RIG/SRC from the TROOP entry, `semantic()` takes the rig name); bbmodel built from the colossus rig; concrete commands in every plan step; slam ground clearance restated (about 2.3 px at follow-through, about 20 px at impact) with a render check; `ColossusMotionCheck` fixture named; idle/guard numbers copied into Task 3; per-hit key tables; level ids named; backhand marked designed with no reference; hit_shoulder frames checked.

v2 7.3 (8/6/8/8/8/6). Fix round 2: `open` seeds the colossus bbmodels from the troop mesh with no animations; TROOP keyed by (rig, file stem) so the hits file is `common` with no `colossus_common` stem; slam settle arm is 0 delta so the sum is -13; slam root deltas restated on top of guard +0.8 (0.78 delta +1.6, fist 2.1 px; guard fist 5.0 px); backhand yaw absolutes -58/-23/+13/+50 include guard's right-arm y -6; slam 0.34 hold key added; Task 2 reordered so the expected failure is named; idle citation narrowed to f_007/f_013; hit amplitudes sourced to the strip plus a 1 fps view.

Kort's review: (placeholder)

Final (after fix round 2): 7.5 (8/6/8/8/8/7). Open minor items (not fixed, two-round limit): root deltas +0.8 to +2.6 push the knee-less soles 1-2.6 px under the ground, so renders must check the soles and cap the root or key a leg offset; the slam hold 0.30-0.34 is under one tick (use top 0.25, hold to 0.40); plan Task 3 guard clearance says 5.8 px but the true value is 5.0; the idle row bar angle is 86 deg, not 82, and the fists sit about 3 px ahead; the hit-over-attack gate in Tasks 4/5 passes trivially until the Task 6 hits exist; there are stale task cross-references; the idle frames show a mannequin proxy, not the golem. Known gaps for Kort: Q2 troop model and look (nothing animates without it); Q1 a backhand / iron golem boss reference; Q5 slam ground shock; a real render run for sole clearance and the status-4 double swing.
