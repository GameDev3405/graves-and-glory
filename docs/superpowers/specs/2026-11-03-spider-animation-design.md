# Spider (Web Stalker) animation

Kort, 2026-11-03. Branch `work/anim-rework`. Reference: "Skyrim - Giant Frostbite Spider" by Invictus8511 (youtu.be/ZArGBAsVHlI), saved at `C:/Users/kortt/Melty/anim-refs/spider/` (frames in `frames/<move>/`, 10 fps; `SOURCES.md` lists what was checked and what was rejected; not committed).

## Why

Kort asked for the animations each unit needs, taken from good game footage and applied to the units properly, in a Souls-like feel. The Web Stalker has no animation work at all today (see Inventory): it is drawn by the vanilla spider renderer, so `spider_bite` and `spider_lunge` show nothing but vanilla's leg sway. Before any clip can matter, the spider needs the same pipeline the knight has. This spec is the clips; the pipeline gaps are listed first because they gate everything (Questions 1 to 3).

## Goals

- The Web Stalker bites and lunges with real weight: a visible coil before each, the body and front legs and fangs pointing at the target on the impact tick, a clean recovery. Style: heavy, predatory, a giant cave spider; not cartoon scuttle.
- The lunge's front legs and body aim at the target's chest line (about 15 to 20 deg above level) on the impact tick. The bite is an exaggerated rear-up (front legs 31 deg above level, steeper than the chest line, stated as such) with the fangs leading forward. Each key below states where.
- No clipping of legs through the body, head or each other, no foot-slide that reads as a glitch.
- Each move is approved by Kort one at a time in game (front, side, 3/4) on the comparison page.

## Units

One troop uses this family.

| Troop | Row (Spec.TROOPS) | Notes |
|---|---|---|
| `wm_stalker` Web Stalker (Wildmoot, rare) | entity `spider`, group cavalry, tags beast/cavalry/small, hp 28, damage 6, range 2.6, speed 0.34, cooldown 25 ticks, cost 40, training 6 s; weapon, helmet, chest all `minecraft:air`; shield none; no pennant; texture `minecraft:textures/entity/spider/spider.png`; family `spider`; ability `ensnare` | Ability: on hit, Slowness II for 1.5 s, at most every 4 s; it climbs walls (vanilla `SpiderNavigation`, `GravesAndGlory.ArmySpider`). Holds nothing: the "weapon" is the fangs bone. |

Rig `art/rigs/spider.json` (64x64 skin, outer 0.25, hands `fangs`): `root` (pivot [0,0,0]) > `thorax` (pivot 0,15,0; 6x6x6) > `head` (pivot 0,0,-3; 8x8x8) > `fangs` (pivot 0,3,-8; 4x2x1, hangs under the face); `thorax` > `abdomen` (pivot 0,0,9; 10x8x12); `thorax` > `left_leg_1..4` and `right_leg_1..4`, each one 16x2x2 box with no joint. Leg 1 is the front pair. Rest rotations (pitch, yaw, roll): left 1 (0,45,45), 2 (0,22.5,33.3), 3 (0,-22.5,33.3), 4 (0,-45,45); right legs are the mirror (yaw and roll negated). Model y is down. `TroopModel.rotation` is Rz·Ry·Rx, so the lowest point of each leg box is not the simple 15 + 15 sin(roll): `clips.points(rig, {})` gives 23.71 for legs 1 and 4 and 23.65 for legs 2 and 3. The rest sole (the ground `clips.py` uses) is 23.71, and legs 2 and 3 are 0.06 px above it, not 2. The thorax pivot (y 15) is 8.7 px (0.54 block) above the sole. The model is vanilla spider size, so the troop has rootScale 1; there is no per-troop scale.

## Inventory today

- **No custom model.** `models/troop/index.json` lists the rig `rig/spider.json` but no troop model; `TroopRenderer` (line 95) draws a custom model only when `TroopModels.modelOf(troop id)` exists, and the six models today are knight, sentinel, gl_marshal, archer, longbow, mage (all biped). `TroopModels.rig("spider")` has no caller. The Web Stalker therefore renders as the vanilla spider.
- **No clips.** `clips/` has only `biped/{common,bow,caster,sword_shield}`; no `spider` folder. `TroopMotion.pose` returns at once for a model with no troop row, and `family(m)` would read `clips/spider/common` plus `clips/spider/spider`, both absent.
- Clip ids the engine would look up for this family once a model exists: stances `idle`, `carry`, `guard` (`brace`, `wall`, `shield`, `aim`, `rest` fall back to these and are not used); attacks `spider_bite`, `spider_lunge` (no `_wall` variant: not a formation unit); hits `hit_head`, `hit_body`, `hit_legs` (the engine also reads `_n` variants; v1 authors none); blocks none (shield none); deaths via `deathKey`; no scenes.
- Family-owned today: none. Inherited from `clips/spider/common`: none (the folder does not exist). Missing: everything, so the engine would use only its procedural layers, which are written for the biped (see Engine gaps).
- `spider_bite` is stab/body, impact tick 8, light; `spider_lunge` is blunt/body, impact tick 8, medium (Spec.ATTACKVARIANTS, fixed).

## Engine gaps (pipeline, needs coordinator or Kort)

1. **Troop model.** source `art/troops/wm_stalker.json` (and its `wm_stalker.bbmodel` and faces png, like `art/troops/archer.*`), then `python tools/build_skins.py` generates `models/troop/wm_stalker.json` and the `index.json` entry; `python tools/preflight.py` checks that the index troops equal the `art/troops` ids (preflight.py line 216), so the renderer uses `TroopModel` for the Web Stalker. This also changes how the stalker is drawn (custom renderer instead of the vanilla spider renderer: shadow, texture, outer layer, team cloth, culling). Needs Kort: "is the Web Stalker moving off the vanilla renderer".
2. **Walking.** The engine's gait is a biped foot solve (`MotionFeet`: `right_leg`, `left_leg`, shins, driven by limb angle). A spider has eight legs and none of those bones, so on `TroopModel` the legs would stand still while the spider moves, and the vanilla leg sway is gone. Options: (a) a hand-keyed `carry` loop of the eight legs that plays at a fixed speed, enough for a 0.34-speed crawler and a Kort-approved cheat; (b) a spider gait layer in `MotionEngine` (Java; the plan does not build it). Needs Kort or the coordinator.
3. **Biped assumptions elsewhere.** Procedural layers (`MotionFeet`, arm shrug, head look, hit pulse) name `right_arm`, `right_leg` and so on; `TroopMotion.apply` skips a bone the model lacks, so they should be inert on the spider, but nothing has run the engine on a spider Family. The plan includes a scratch smoke test before any clip work.
4. **Tooling.** `tools/anim_edit.py` knows only `sword_shield`, `bow`, `common` (`TROOP` dict, line 29), hard-codes `RIG = 'biped'` and `SRC` (lines 26-27), loads `biped.json` (line 90) and copies applied clips to `art/clips/biped`, so `RIG` and `SRC` must be derived from the family before `open spider` works (coordinator work); `tools/clips.py` expects `art/clips/<rig>/<family>.bbmodel`, one project per family, and fails if the family lacks a clip for either MELEE attack id (`spider_bite`, `spider_lunge` are both stab/blunt). The bbmodel must be made from the spider rig (groups per spider bone), not copied from the biped project.
5. **Render harness.** `test-tools/work/t16r12/live/run.sh` has no spider case; a `solo_stalker` line like `solo_knight` is needed (troop `wm_stalker`, family `spider`).

## References

- **Skyrim - Giant Frostbite Spider** (Invictus8511, youtu.be/ZArGBAsVHlI, 1:37, `frostbite_a.mp4`). First-person gameplay: the spider is seen head-on, so we get its silhouette, the rear-up and the charge, never the fangs. This is the best clean creature material found: three more searches (Unreal Marketplace/Fab creature packs, Elden Ring spider bosses, Mega Arachnid UE4) gave a distant swarm, a dark boss fight and screen-recorded video, all rejected (SOURCES.md). The coordinator's reference list has no beast section that was verified.
- No Elden Ring or Dark Souls source gives a readable spider bite. The Souls feel comes from the timing the stalker is given: a long, readable coil (anticipation), a hard fast strike on the impact tick, a held beat, a slow recovery.
- Because the fangs are not visible in any footage, the fang snap is **our design**, stated as such. The video supplies the rear-up (front legs overhead), the leg strike and the charge.
- **Silhouette only.** `frames/rear_bite/f_012` (24.1 s, both front legs overhead), `frames/rear_bite/f_016` (24.5 s, one leg sweeping across low) and `frames/lunge/f_004` (19.9 s, charge with motion blur) give an outline and nothing else: no leg joints, no fangs, no timing between poses. They are used for the rear-up shape and the charge silhouette only, never for angles. Question 3 asks Kort for better footage (a side view of a spider bite).

## Stances

Starting pose of every attack is `guard` (an enemy is near, `MotionEngine.stanceKey`), the moving pose is `carry`, the idle is `idle`. All values are deltas on the rig's rest pose; leg yaw and roll add to the rest rotations above.

Sign conventions, derived from the model, to be confirmed with the pose preview on the first clip (Task 1 Step 3 fails the plan if they differ):
- Root y positive is down (shipped `guard.json` crouches with root y +1.2); root z negative is forward (shipped swings lunge with root z -1.4 to -1.8).
- Thorax or head pitch positive is nose down; negative is nose up (rears). Body y positive turns to the spider's right (shipped `sword_shield_low_cut`: right draw back is +34).
- Left leg: yaw up sweeps the tip forward, roll down (toward 0 and below) lifts it. Right leg: the opposite signs (yaw down is forward, roll up lifts). Leg pitch spins the leg about its own length and does nothing visible: leave it 0. "Mirror" below means the right leg gets the left's yaw and roll with both signs flipped.
- Fangs: pitch 0 hangs straight down (and a little back); pitch negative snaps the tips forward, positive tucks them back. Abdomen pitch positive lifts its rear.

**Ground clearance (no foot planting on this rig).** Planted legs are 2, 3, 4 on each side; legs 1 are raised and exempt, but must not go below the sole. The rest sole (the ground `clips.py` uses) is the lowest rest point, y **23.71** (`clips.points(rig, {})`: leg lows 23.71, 23.65, 23.65, 23.71 for legs 1 to 4, so all four sit within 0.06 px of it). `TroopModel.rotation` is Rz·Ry·Rx (roll is applied last), so a leg's lowest point is about 15·cos(yaw)·sin(roll) plus the pitch and root terms, not 15·sin(roll), and no hand formula is used: every planted tip is solved numerically with `clips.points` (bisection on the leg roll per key; left side solved, right the mirror; scratchpad `sp_v4.py`) and the tables hold the solved numbers. Result: legs 2, 3, 4 end every key within 0.02 px of the sole (the limit is 0.5 px; a thorax sway of 1 deg adds 0.28 px, 1.5 deg 0.42 px); legs 1 end at or above it. Guard's own rolls are L1 -14, L2 -5.3, L3 -10.2, L4 -17.6 (deltas in the clip tables are over these). Death is exempt (see Hits and death). The Task 1 pose preview re-measures the lowest leg point per key against the rest pose; a key off by more than 0.5 px is solved again.

| Stance | Loop | Pose and direction |
|---|---|---|
| `idle` | 2.6 s | Low crouched wait (absolute values, not over guard): root y +1.0, thorax pitch +2, leg 1 roll -12.4 (yaw 0, held 0.4 px above the sole), leg 2 roll -5.4, leg 3 roll -3.6, leg 4 roll -4.4 (planted: solved with `clips.points`, tips within 0.01 px of the sole). Breath: abdomen pitch +-2, thorax yaw +-1 (no thorax roll: +-1.5 deg of roll moves tips 0.42 px). Fangs hang (chain +2 -6 +0 = -4, nearly straight down), the head faces forward. A feeler twitch (yaw +-4, one leg 1 at a time) every 1.3 s. |
| `carry` (move) | 0.6 s | Alternating-tetrapod gait: legs L1, R2, L3, R4 swing together, then R1, L2, R3, L4; each swinging leg yaws +-14 and lifts roll -10 at mid-swing (a swinging leg is exempt); planted legs hold idle's rolls; no root bob (a 0.8 px bob would sink the planted tips 0.8 px); thorax roll +-1.5 following the planted side (0.42 px at most). Fangs hang. Pending Question 2. |
| `guard` | 2.0 s | Alert crouch, the loose-melee pose every attack starts from: root y +1.8, thorax pitch -4 (chest lifted), head pitch -6 (face up toward the target), fangs pitch -10 (chain -4 -6 -10 = -20: tips hang 20 deg forward of straight down, 70 deg below level), legs 1 forward and raised (left yaw +14, roll -14; mirror right; 2.6 px above the sole), legs 2 yaw +5 roll -5.3, leg 3 roll -10.2 (yaw 0), leg 4 roll -17.6 (yaw 0); planted tips solved, within 0.02 px of the sole (the crouch and the thorax -4 alone would sink them 1 to 3 px); abdomen pitch +6 (raised). Sway +-1 deg (0.28 px). |

## Moves (breakdown)

Clip ids and impact ticks are fixed (Spec.ATTACKVARIANTS). Video times are `frostbite_a.mp4` (first-person camera, the spider seen from the front). `Spider` here is the Web Stalker. Pitches are the sum of thorax and head pitch added to the rest.

| Order | Ours | Video (m:ss) | Notes |
|---|---|---|---|
| 1 | stances `idle`, `guard`, `carry` | 0:19.6 (`frames/lunge/f_001`, waiting low and square) | Gated on Questions 1 and 2. |
| 2 | `spider_bite` (1.0 s, impact 0.40 s, tick 8) | 0:23.9 to 0:24.5 (`frames/rear_bite/f_010` to `f_016`) | Rear up, front legs strike, fangs snap. |
| 3 | `spider_lunge` (1.1 s, impact 0.40 s, tick 8) | 0:19.6 to 0:20.4 (`frames/lunge/f_001` to `f_008`) | Coil low, pounce, body slam. |
| 4 | `hit_body`, `hit_head`, `hit_legs` (no `_2` in v1) | none found | Recoils authored on the rig; see below. |
| 5 | `death` | none found | Flip onto the back, legs curl. |

### `spider_bite`: body, stab, impact 0.40 s (tick 8)

Time mapping (one, piecewise; frames at 10 fps, `f_010` = 23.9 s): video 23.9 to 24.1 s (f_010 to f_012, body low then both front legs overhead) = clip 0.00 to 0.20 s at 1x; video 24.1 to 24.3 s (f_012 to f_014, legs still high) = clip 0.20 to 0.30 s at 2x; video 24.3 to 24.5 s (f_014 to f_016, a front leg sweeping across low) = clip 0.30 to 0.40 s at 2x. So the video's strike lands on our tick 8. The video shows the rear-up and the leg strike; the fang snap is ours. Nothing after 0.40 s comes from the video.

Pitch chain = thorax + head + fangs absolute (0 = fangs straight down; positive = tips tucked back; negative = tips forward; -90 = level; -120 = 30 deg above level). Keys are deltas over `guard` (guard abs: thorax -4, head -6, fangs -10, chain -20); the first and last of every keyed channel are 0. Leg rolls are the solved ones (see Ground clearance): legs 2, 3, 4 end every key within 0.02 px of the rest sole (y 23.71), L1 is raised 4 to 6 px above it. Rear yaws are not used (yawing the rear legs back would need rolls of 39 to 70 deg to stay on the sole); the rear tips slide with the root step instead.

**Chest line.** The target is 1.5 to 2 blocks away and its chest is 1.2 to 1.4 blocks up; the spider's thorax is about 0.54 block high, so the line to the chest is about 15 to 20 deg above level. **The bite does not aim at the chest.** It is an exaggerated rear-up from the footage: at impact the face is nose-up 52 deg, the fangs 30 deg above level and the front legs 31 deg above level, 10 to 15 deg steeper than the chest line, striking down over the target from above. Question 3 asks Kort whether to flatten it. (The lunge below is the move that aims at the chest.)

**Judging the bite.** Not by fang angles: the fangs are a 4x2x1 box and unreadable in game. By the head and thorax heading (face nose-up 32 deg at 0.20, 42 at 0.30, 52 at 0.40) and by the fang-box travel between 0.30 and 0.40, computed from the keys with the rig's rotation: 4.5 px forward and 3.6 px up in the world (the 5 px root step carries it forward; the snap adds the rise and swings the tips from down-back to forward-up). Both must be more than 3 px in the render.

| Time (s) | Key | Bones, rotation x,y,z deg (position px) | Where the fangs and front legs point |
|---|---|---|---|
| 0.00 | start | all 0 | guard: fangs 20 deg forward of straight down, front legs forward |
| 0.20 | coil and rear-up (video 24.1, silhouette only) | root pos (0, +0.5, +2.0 back); thorax x -16 (abs -20); head x -6 (abs -12); fangs x +60 (abs +50), chain +18; L1 yaw +4 roll -40 (total yaw 63, roll -9: leg up and out); L2 yaw +4 roll +12.2; L3 roll -10.0; L4 roll -21.7; mirror right; abdomen x +8 | Face lifted 32 deg (thorax -20, head -12); fangs cocked 18 deg **back** of straight down, under the chin (chain +18, not forward); front legs raised overhead and splayed; the spider rears at the target, nothing strikes yet |
| 0.30 | peak (video 24.3) | root pos (0, 0, +1.0); thorax x -22 (abs -26); head x -10 (abs -16); fangs x +62 (abs +52), chain +10; L1 yaw +4 roll -50 (total roll -19); L2 roll +18.2 (yaw 0); L3 roll -10.4; L4 roll -26.3; mirror right | Held for 0.1 s: face lifted 42 deg, fangs cocked 10 deg back of straight down (loaded: it snaps 120 deg in the next 0.1 s), front legs up high; the stillest beat, the telegraph |
| 0.40 | **impact, fang snap** (video 24.5, a leg sweep only; silhouette only) | root pos (0, +0.5, -4.0 forward); thorax x -28 (abs -32); head x -14 (abs -20); fangs x -58 (abs -68), chain -120; L1 yaw +6 roll -30 (total yaw 65, roll 1); L2 roll +23.0 (yaw 0); L3 roll -15.5; L4 roll -37.6; mirror right; abdomen x +10 | Face nose-up 52 deg. Fangs 120 deg forward of straight down: tips forward and **30 deg above level**. Front legs reach forward **31 deg above level** (thorax nose-up 32 minus roll 1), each tip **25 deg outside the facing** (yaw 65 from the sideways axis), the pair forming a 50 deg V. This is the rear-up display, 10 to 15 deg above the chest line (stated above). Tolerance: front legs within 8 deg of 31 deg elevation, lateral within 30 deg per leg |
| 0.55 | hold and bite closes | root pos (0, +0.8, -2.5); thorax x -20 (abs -24); head x -8 (abs -14); fangs x -22 (abs -32), chain -70; L1 yaw +2 roll -14; L2 roll +10.6; L3 roll -13.3; L4 roll -28.8; mirror right | Fangs 70 deg forward of down (20 deg below level), easing down; front legs land forward of the body |
| 0.75 | recover | root pos (0, +0.4, -0.8); thorax x -6 (abs -10); head x -2 (abs -8); fangs x -4 (abs -14), chain -32; L1 roll -4; L2 roll +1.6; L3 roll -4.5; L4 roll -9.1; mirror right | Fangs 58 deg below level, falling toward guard (chain -20), face dropping |
| 1.00 | end | all 0 | guard |

Mechanics: the body rises on the rear legs (the thorax pivot is 8.7 px above the sole); the step is root only (root z +1.0 at 0.30 to -4.0 at 0.40, 5 px). The rear tips stay within 0.02 px of the sole in height and slide up to 5 px along it between 0.30 and 0.40, hidden under the reared body; no foot planting layer exists for this rig.

### `spider_lunge`: body, blunt, impact 0.40 s (tick 8)

Time mapping (one, piecewise; frames at 10 fps, `f_001` = 19.6 s): video 19.6 to 19.8 s (f_001 to f_003, low and square) = clip 0.00 to 0.20 s at 1x; video 19.8 to 19.9 s (f_003 to f_004, the launch begins) = clip 0.20 to 0.30 s at 1x; video 19.9 to 20.2 s (f_004 charging with blur, to f_007 body filling the frame) = clip 0.30 to 0.40 s at 3x (the charge is compressed to hit tick 8). The charge itself is a root move; the video supplies its silhouette (low body, legs flung wide). Nothing after 0.40 s comes from the video.

Pitch chain = thorax + head + fangs absolute (0 = fangs straight down, negative = tips forward, -90 = level, -120 = 30 deg above level). Leg rolls are solved (see Ground clearance): legs 2, 3, 4 end within 0.02 px of the rest sole at every key except 0.30 (airborne); L1 at or above it. Deltas over guard.

**Chest line.** The target is 1.5 to 2 blocks away with its chest 1.2 to 1.4 blocks up, so from the thorax (0.54 block high) the chest line is about 15 to 20 deg above level. The lunge aims at it: at impact the front legs reach 17 deg above level, inside that line.

| Time (s) | Key | Bones (deltas over guard; position px) | Where the front legs and fangs point |
|---|---|---|---|
| 0.00 | start | all 0 | guard |
| 0.20 | coil (video 19.6 to 19.8) | root pos (0, +3.0, +3.0 back); thorax x +8 (abs +4); head x +4 (abs -2); fangs x +20 (abs +10), chain +12; L1 yaw -8 roll -21.5 (pulled in; 0.4 px above the sole); L2 yaw -5 roll -17.5; L3 roll -9.5; L4 roll -7.7; abdomen x +14; mirror right | Fangs tucked 12 deg back of straight down, head low, front legs folded back under the chin. A spring, not a threat yet |
| 0.30 | launch (video 19.9) | root pos (0, -3.5, -7.0 forward); thorax x -8 (abs -12); head x -4 (abs -10); fangs x -30 (abs -40), chain -62; L1 yaw +6 roll -20 (total yaw 65, roll 11); L2 yaw +14 roll -22; L3 yaw -10 roll -12; L4 yaw -14 roll -12; abdomen x +6; mirror right | Airborne (`clips.points`: the lowest tips are 4.3 px above the sole): front legs forward and about level (body nose-up 12 minus roll 11 = 1 deg above level), fangs 28 deg below level, head level |
| 0.40 | **impact** (video 20.2, charge silhouette only) | root pos (0, +1.0, -12.0 forward, about 0.75 block); thorax x -14 (abs -18); head x -6 (abs -12); fangs x -40 (abs -50), chain -80; L1 yaw +6 roll -30 (total yaw 65, roll 1); L2 yaw +18 roll +24.0; L3 roll -11.4; L4 roll -22.2; abdomen x +12; mirror right | Front legs forward, **17 deg above level** (thorax nose-up 18 minus roll 1), inside the 15 to 20 deg chest line; each tip is 25 deg outside the facing (yaw 65 from the sideways axis), the pair forming a 50 deg V around the chest. Tolerance: elevation within 15 deg of the chest line, lateral within 30 deg per leg. Fangs forward, 10 deg below level (80 deg forward of down). The thorax slams the target (the blunt hit is the body) |
| 0.55 | skid | root pos (0, +1.8, -9.0); thorax x -4 (abs -8); head x -2 (abs -8); fangs x -10 (abs -20), chain -36; L1 yaw +2 roll -8; L2 yaw +10 roll -2.3; L3 roll -9.8; L4 roll -14.6; mirror right | Front legs braking forward, fangs 54 deg below level and easing down |
| 0.80 | settle | root pos (0, +1.0, -3.0); thorax x +2 (abs -2); L1 yaw -14 roll -4.8 (0.4 px above the sole); L2 roll -5.8; L3 roll -3.6; L4 roll -3.5; mirror right | Rocking back to guard, face toward the target, fangs about 18 deg forward of down (chain -18) |
| 1.10 | end | all 0 | guard |

Mechanics: the pounce is a root move of 15 px in all (3 px back, then 12 forward). The legs are airborne between 0.25 and 0.40 s, so the feet land at their new place at 0.40 s and do not slide; after the landing the body skids 3 px back (the rear tips slide up to 3 px, hidden by the low body).

Gameplay note: the range is 2.6 and the lunge's visible reach is 12 px (0.75 block) plus about 0.7 block of body and legs. The clip is a cosmetic step; the server position does not move (Question 4).

### Hits and death

Decision on `_2`: v1 authors **no** `_2` or `_n` variants (one clip per zone: `hit_body`, `hit_head`, `hit_legs`); the engine picks `hit_<zone>` for every hit. A mirrored `_2` is added only if Kort asks. Hits are deltas over `guard` (guard abs: thorax -4, head -6, fangs -10, chain -20), authored at about 0.4 of the knight's amplitude so the hit-over-attack check passes. First and last keys are 0. No video; authored on the rig. Leg rolls are solved (Ground clearance): at each key legs 2, 3, 4 are within 0.02 px of the rest sole (y 23.71) and leg 1 is at or above it (L1 clearance listed).

`hit_body` (0.5 s, shove back):

| Time (s) | Bones (deltas, position px) | Abs pitch (thorax / head / fangs, chain) | Where the fangs and front legs point |
|---|---|---|---|
| 0.00 | all 0 | -4 / -6 / -10, -20 | guard |
| 0.10 | root z +2.0; thorax x +10; fangs x +20; L1 yaw -8 roll -5.6; L2 roll -4.9; L3 roll +4.8; L4 roll +13.7 (the nose-down thorax lifts the rear tips; the roll puts them back on the sole); mirror right | +6 / -6 / +10, +10 | Body flinched back, fangs tucked 10 deg back of straight down, forward legs recoiling (L1 0.4 px above the sole); still facing the target |
| 0.25 | root z +0.8; thorax x +4; fangs x +8; L1 yaw -3 roll 0; L2 roll -2.1; L3 roll +1.8; L4 roll +5.0 | 0 / -6 / -2, -8 | Recovering, fangs 8 deg forward of down (L1 1.3 px above the sole) |
| 0.50 | all 0 | -4 / -6 / -10, -20 | guard, fangs and legs toward the target |

`hit_head` (0.5 s, face snaps up):

| Time (s) | Bones (deltas, position px) | Abs pitch (thorax / head / fangs, chain) | Where the fangs and front legs point |
|---|---|---|---|
| 0.00 | all 0 | -4 / -6 / -10, -20 | guard |
| 0.08 | thorax x -8; head x -26; fangs x +30; L2 roll +5.0; L3 roll -3.6; L4 roll -8.9; abdomen x +4; mirror right | -12 / -32 / +20, -24 | Face thrown up 44 deg, fangs tucked relative to the head but still 24 deg forward of down; front legs unchanged (L1 4.5 px above the sole) |
| 0.25 | thorax x -3; head x -10; fangs x +12; L2 roll +1.8; L3 roll -1.2; L4 roll -3.5 | -7 / -16 / +2, -21 | Face dropping back to the target |
| 0.50 | all 0 | -4 / -6 / -10, -20 | guard |

`hit_legs` (0.5 s, buckle):

| Time (s) | Bones (deltas, position px) | Abs pitch (thorax / head / fangs, chain) | Where the fangs and front legs point |
|---|---|---|---|
| 0.00 | all 0 | -4 / -6 / -10, -20 | guard |
| 0.10 | root y +2.5; thorax x +6; L1 yaw -4 roll -15.0 (the forward legs fold in and stay 0.4 px above the sole; a +4 sag would put them 3 px under it); L2 roll -14.4; L3 roll -8.3; L4 roll -7.3; fangs x 0; mirror right | +2 / -6 / -10, -14 | Body dropped 2.5 px, forward legs folded in, fangs 14 deg forward of down, facing the target |
| 0.25 | root y +1.0; L1 roll +1.0; L2 roll -4.8; L3 roll -4.4; L4 roll -5.8 | -4 / -6 / -10, -20 | Rising (L1 1.5 px above the sole) |
| 0.50 | all 0 | -4 / -6 / -10, -20 | guard |

`death` (1.4 s): staggers, then the body rolls onto its back and the legs curl in. Not scored for weapon direction; the fangs tuck (chain stays between -10 and +10). **Death is exempt from the planted-tip rule** (the body tips over): each key's root y is solved so the lowest point of the whole model sits on the rest sole (23.71), the way `clips.py --verify` checks it (within 0.1 px).

| Time (s) | Bones (deltas over guard, position px) | Abs pitch (thorax / head / fangs, chain) | Where the fangs and front legs point |
|---|---|---|---|
| 0.00 | all 0 | -4 / -6 / -10, -20 | guard |
| 0.30 | thorax roll +12; root y -3.3 (solved: the body leans onto the left legs, which touch the sole; the right legs lift 6 to 7 px, so they are not planted); legs at guard; fangs x +10 | -4 / -6 / 0, -10 | Staggering, fangs back toward hanging |
| 0.70 | thorax roll +90 (on its side); root y -6.4 (solved); all legs to roll +20 total (deltas L1 +34, L2 +25.3, L3 +30.2, L4 +37.6; mirror right); fangs x +20 | -4 / -6 / +10, 0 | On its side, fangs hanging straight down, front legs flailing up |
| 1.10 | thorax roll +180 (belly up); root y +2.1 (solved: back on the ground); all legs to roll +50 total (deltas L1 +64, L2 +55.3, L3 +60.2, L4 +67.6; mirror right: curled in); fangs x +30 | -4 / -6 / +20, +10 | Belly up, fangs 10 deg back of straight down (pointing at the sky), legs curled |
| 1.40 | hold the 1.10 pose | same | same |

`clips.py --verify` places the lowest point on the ground; if it reports more than 0.1 px at a key, move that key's root y in 0.1 steps (the numbers above were found with `clips.points`, the same function).

## Application to units

- **wm_stalker** (hp 28, damage 6, range 2.6, ability ensnare, no worn items). Rig is vanilla spider size, rootScale 1, so clip pixels are exact; no scaling to compute. Nothing is held: the fangs bone is the weapon, the front legs 1 are the reach.
- **Clipping risks:** front legs through the head or fangs when raised and yawed forward (the leg bases are at x +-4, z -1; the head is 8 wide, so a total yaw above about 65 with roll near 0 runs the leg along the head's side: keep total yaw at most 65 and check front and top views); abdomen through the thorax at abdomen x +14 (the abdomen pivot is at z 9, box z -6 to 6: at +14 deg it swings only ~1.5 px at its tip into the thorax at the start of its box, check); legs through the ground on `hit_legs` and the lunge landing (every leg's lowest point is within 0.06 px of the sole at rest, so any root drop or pitch sinks tips unless the roll is solved per key, as in the tables; see Ground clearance; `MotionFeet.ground` does nothing for this rig).
- **Ability visual:** `ensnare` has no animation. A web tell on hit would be a new visual: needs Kort.
- **Wall/formation:** none; not a formation unit, so no `_wall` or `brace`.
- **Needs Kort:** Questions 1, 2 and 4 below (renderer swap, gait, whether the lunge moves the entity).

## Fixed limits

- Impact markers stay on tick 8 (0.40 s) for both attacks; clips stay at most 40 ticks (planned 1.0 s and 1.1 s). Any gameplay timing change needs Kort.
- `python tools/clips.py` then `--verify` must pass; `Spec.java` changes only through `tools/preflight.py`. No gameplay, save or network change.
- Max 2 Minecraft instances (Kort's client counts). No computer-use on Minecraft. No push, publish or merge.

## Questions for Kort

1. Switch the Web Stalker from the vanilla spider renderer to the troop model (needed for any clip)? This also needs `wm_stalker.json` and its skin (the vanilla texture fits the rig's UVs).
2. Walking: author a hand-keyed 8-leg `carry` loop at a fixed speed (Kort-approved cheat), or have a spider gait layer written in the engine (Java)?
3. Is the weak footage acceptable (first-person, fangs never shown, rear_bite f_012 and f_016 and lunge f_004 silhouette only), and is the bite as an exaggerated rear-up (front legs 31 deg above level, steeper than the 15 to 20 deg chest line) what he wants, or should he supply a spider bite clip from the side?
4. Should the lunge visibly cover 12 px (0.75 block) as a root offset only (the entity does not move), or stay in place?
5. `ensnare` tell: a web puff on hit, or none?

## Review

Pending: Kort's review of the comparison pages, one move at a time.

v1 6.2 (8/5/5/6/8/5). Fix round 1: (1) bite 0.20 and 0.30 fang chain re-derived (fangs cocked back: chain +18 and +10) and 0.55 / 0.75 re-derived; (2) front-leg aim stated with the real angles (31 / 17 deg elevation, 25 deg outside the facing per leg) and tolerances instead of "within 5 deg"; (3) guard and idle crouch compensated with leg roll so planted tips stay within 0.3 px of the rest sole, ground clearance rule and per-key rear-leg rolls added; (4) units fixed (root pivot [0,0,0], rest sole y 26.3, thorax 11 px above); (5) pipeline gaps now name the `anim_edit.py` RIG/SRC generalisation and the `art/troops/wm_stalker.json` source; (6) per-key tables for hits and death, no `_2` variants in v1; (7) one video-to-clip time mapping per move; (8) hit-over-attack fixture named `test-tools/standalone/SpiderHitCheck.java`.

v2 6.2 (8/5/6/6/6/6), no gain on v1. Fix round 2: (1) rest sole recomputed with `clips.points`: y 23.71 (not 26.3), legs 2 and 3 are 0.06 px above it (not 2), because `TroopModel.rotation` is Rz·Ry·Rx; (2) every planted-leg roll, the guard, idle, bite, lunge and hit keys and death 0.30 to 0.70 solved numerically (bisection on `clips.points`, scratchpad `sp_v4.py`), tips within 0.02 px of the sole, solved values in the tables; (3) chest line restated as 15 to 20 deg: the lunge aims at it (17 deg), the bite is stated as an exaggerated rear-up (31 deg) and no longer "aims at the chest"; (4) the bite is judged by head and thorax heading and fang-box travel (4.5 px forward, 3.6 px up from 0.30 to 0.40), not fang angles; (5) rear_bite f_012 and f_016 and lunge f_004 marked silhouette only (also in SOURCES.md), Question 3 kept; (6) SpiderHitCheck specified in the plan (package, loader, stance, command, Expected); (7) smoke test moved to the end of Task 1, stub clips and `spider.bbmodel` marked needs coordinator; (8) `hit_legs` and `death` numbers and Expected match the solved values.

Final (after fix round 2): 7.2 (coverage 8, references 6, weapon direction 7, per-unit 7, fixed limits 8, executability 7). Open minor items:
- The plan's Global Constraints say the fangs lead at the chest on the bite. The bite is actually an exaggerated rear-up; only the lunge aims at the chest.
- In Task 1, the engine smoke test (Step 6) should come before the render (Step 4).
- Tasks 2, 5, 6 and 8 leave out the `bash run.sh <tag> solo_stalker` tag.
- In Task 0, the solo_stalker case must honour `${GRAVES_GRADE_CLIPS:-...}`.
- Task 7 could call the MotionEngineCheck helpers directly instead of copying them.
- The hit amplitude of 0.4 is unmeasured.

Known gaps for Kort:
- Q1: move the Web Stalker to the troop model. This gates every clip.
- Q2: the 8-leg gait.
- Q3: approve the silhouette-only footage and the exaggerated bite, or find a side-view spider bite.
- Q4: the lunge root offset. A render is also needed to confirm the 15°/tick gate and the clipping risks.
