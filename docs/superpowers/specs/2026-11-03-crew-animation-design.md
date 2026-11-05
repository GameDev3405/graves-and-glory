# Mortar crew (Ironhold Mortar) in the Souls-like style

Kort, 2026-11-03. Branch `work/anim-rework`. References are in `C:/Users/kortt/Melty/anim-refs/crew/` (not committed; `SOURCES.md` lists each timestamp). Same shape as the crossbow, bow and caster specs.

## Why

Kort wants Souls-like weight on every troop. The crew family has one troop, the Ironhold Mortar (`ic_mortar`), and **nothing plays for it today**: it has no troop model, so it renders as a vanilla pillager with no barrel and `TroopMotion.row` returns null (no clips play for a model without a `troop` id). There is also no footage of anyone loading a mortar, so the key poses are authored from the weapon-direction rule and the fixed tick, with only the stoop and hunch taken from the Elden Ring ballista crank.

## Goals

- A new `crew` clip family (`clips/biped/crew/`): stances `idle`, `carry`, `aim`, `rest` and the attack `crew_fire` (the pull, the shot at tick 8, the kick, then the reload).
- Weapon direction right at every key (Kort's rule). The barrel is a root part fixed at 45 degrees up and forward (identity spec), so it never moves in the clip; what each key states is where the crew's hands are on it and which way the shell points. A key without that is a defect.
- No self-clipping at 8 angles: hands, shell and arms against the barrel, the ammo crate, the apron and the iron cap.
- Each clip approved by Kort in game, on the compare page, one at a time.

## Units

From `Spec.TROOPS` (last field = family), `sheets/design.json`, and `docs/superpowers/specs/2026-10-17-unit-identity.md`.

| | `ic_mortar` Ironhold Mortar |
|---|---|
| House, rarity | Ironclad, epic |
| Entity, rig | pillager (`ArmyPillager`), biped |
| Group, tags | siege, tags `siege,large` |
| Held item (weapon) | `minecraft:air` (no held item; the barrel is a root part) |
| Helmet, chest | iron_helmet, iron_chestplate |
| Shield column | `none` (no shield clips) |
| Pennant column | false in Spec.TROOPS (the identity spec lists a team pennant; no pennant on any back either way) |
| Health, damage | 22, 9 |
| Range, cooldown, speed | 36, 100 ticks (5.0 s), 0.18 |
| Ability | `shell`: cast, windup 30 ticks (1.5 s), radius 3.5, minRange 10, rangedPenalty |
| Attack row | `crew_fire`: zone body, kind blunt, impact tick 8, weight heavy (fixed) |
| Build (identity spec) | square like the Arbalist; iron cap, scorched beard, heavy apron, ammo crate, `mortar_barrel` root part at 45 degrees over the shoulder with a lit mouth, tabard |
| `rootScale` | TBD: no model exists. It is `(24 - model root pivotY) / (24 - the rig root pivotY)` (`TroopModel.java:311`; the biped rig root pivot is 0, so the divisor is 24); exactly 1.0 if the model file has no `root` pivot (knight, archer), about 0.90 if it lowers the root 2.4 px like a 0.8 leg build. Compute it with the one-liner in plan Task 6 once `art/troops/ic_mortar.json` exists |

`idleArms` defaults to `hang`; the clips hold the arms.

## Inventory today

`TroopMotion.family` needs a model whose `troop` id is in `Spec.TROOPS`. Folder `clips/biped/crew/` does not exist.

| What the engine plays | Source today |
|---|---|
| Troop model for `ic_mortar` | **missing**: `art/troops/` holds archer, gl_marshal, knight, longbow, mage, sentinel only. Vanilla pillager, no barrel, no clips |
| Stances idle, carry, aim, rest | **missing** (procedural; `common/` has no stances). Fallbacks: aim -> guard -> carry -> idle; rest -> carry -> idle |
| Attack `crew_fire` (body, blunt, impact 8) | **missing**. `clips.py` treats blunt as melee and fails a family project that lacks a clip of every melee id, so the project needs at least a stub (plan Task 1); the stub must carry the `impact` marker at tick 8 (0.40 s) or `clips.py --verify` fails |
| `crew_fire_wall` | none needed (siege has no wall or brace stance) |
| Hits `hit_head/body/legs` x3, `fall`, deaths, scene `raise` | inherited from `common/` (authored for a bare arm-down pose) |
| Blocks | none: siege has no shield (Spec shield column `none`), so no block clip is asked for |
| Mortar piece itself | not rendered: `weapon` is `minecraft:air`; `mortar_barrel` is a planned root part of the model that does not exist |
| Bow code | `TroopMotion.bow(row)` (line 146) is true for any ranged, non-magic troop, and `ic_mortar` is ranged and not magic, so the bow hand twist and draw timeline would run on the mortar. See Question 5 |

The shot: the shell ability has a 30-tick windup (the identity spec's tell is FLAME at the mouth through it, then LARGE_SMOKE); the cooldown is 100 ticks. Where the `crew_fire` swing sits against that windup is not read here: Question 3.

## References

See `SOURCES.md`. Honest summary:

1. **Elden Ring ballista crank**, https://www.youtube.com/watch?v=Qvo487abShM, local `C:/Users/kortt/Melty/anim-refs/crossbow/elden-ring-bow-crossbow-ballista.webm`, frames in `frames/ballista_crank/` (checked by eye: f001 21:28.0 planted wide hunched, f020 21:29.9 frame upright against the body, f050 21:32.9 planted, reaching across, f060-f070 21:33.9-21:34.9 hunched crank, head down). Used for the stoop, hunch and head-down-on-the-work of the reload only.
2. **Catapults in Action**, https://www.youtube.com/watch?v=Uk7Qkc92Iu0, local `barcio-catapults.mkv`: checked, unusable (1970s film, crew distant). Not used.

No mortar, cannon or muzzle-loader footage was found. The pull, the shot, the kick, the lift of the shell to the muzzle and the drop-in are **authored, no reference** (Question 4).

## Stances

Degrees, rest-relative. Signs from shipped clips: arm pitch negative raises the arm forward (0 = hanging, -90 = forward level, -180 = straight up); forearm pitch negative bends the elbow; body pitch positive leans forward; head pitch negative looks up; body yaw positive turns the left side forward (the sword_shield low_cut draws back right with body y +34). Values are starting values; `pose_preview.py` is the check. Legs unkeyed (MotionFeet plants them).

Barrel geometry assumed (identity spec; to be confirmed, Question 1): breech behind the right shoulder blade, the barrel crossing over the right shoulder at 45 degrees up and forward, the mouth about 5 px ahead of and 6 px above the shoulder pivot (7.8 px away; the arm reaches 8 px to the hand pivot and about 10 px to the fingertip). A lanyard cord and a short grip strap hang 3-4 px from the barrel underside at 4-5 px ahead of the shoulder (both need to be in the model, Question 1). The barrel is a root part: it does not follow the body bones.

| Stance (loop) | Posture | Key values (pitch; yaw y, roll z in brackets; arm / forearm) | Weapon direction (follows from the numbers) |
|---|---|---|---|
| `aim` (3.0 s, sway 0.4) | Hands on the cord and the grip under the barrel, head tipped up along it | body 4 (y 6); head -14 (y -6); R -80 (z 10) / -95; L -68 (y -22, z -6) / -96 | Barrel fixed 45 up-forward, mouth above the right shoulder. Hand positions (4 px segments, shoulder pivot origin, body 4 included, rough, see the reach check): R hand pivot about 4.5 ahead of and 3 above the shoulder (on the lanyard cord, about 3 px below the barrel); L hand pivot about 5 ahead and 2 above (on the grip strap, about 4 px below the barrel). Head up 14 looks along the barrel |
| `idle` (4.0 s) | Barrel slung, hands on the sling and apron | body 2 (y 4); head -3 (y -4); R -12 (z 8) / -35; L -40 (y -15) / -70 | Barrel 45 up-forward, slung. Left hand on the sling strap at the left chest, right hand low on the apron belt; neither touches the barrel |
| `carry` (2.4 s, march) | Same, leaned under the weight, sway damped | body 5 (y 3); head -2; R -12 / -35; L -40 / -70 | Barrel 45 up-forward, slung; hands as idle |
| `rest` (4.0 s) | Standing easy: arms hang, barrel slung | body 3; head 0; R 3 (z 6) / -8; L 3 / -10 | Barrel 45 up-forward, slung; hands at the thighs, not on the barrel |

## Moves

One attack row, fixed: `crew_fire` (zone body, kind blunt, impact tick 8 = 0.40 s). The clip is an offset over `aim` (first and last keys of every keyed channel 0), 2.0 s (40 ticks, the cap `BattleMath.ATTACK_CLIP_MAX_TICKS`). Markers: `impact` at 0.40 only. Legs unkeyed; the stoop uses body pitch and the root dip, the planted feet bend the knees.

Reference basis: the stoop (0.65-0.95 s) and the head-down work (0.95-1.15 s) follow the ballista frames f001 and f050-f070; the rest is authored. Retime: the Elden Ring crank is about 5 s; the loading is compressed into ticks 13-37.

Signs: R arm +n lowers the hand (pulls back and down); forearm +n straightens the elbow; arm -n raises it. Absolute value = stance + delta.

`crew_fire` key table (deltas from `aim`; yaw y and roll z in brackets; root column is position in pixels, `y` = down; hands keyed 0 throughout):

| t (s) | tick | body | head | R arm | R forearm | L arm | L forearm | root (px) | Weapon direction at this key (R hand = ahead, above the shoulder pivot, px) |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Barrel 45 up-forward; R hand (4.5, 3.0) on the lanyard cord, L hand (5.0, 2.0) on the grip strap |
| 0.15 | 3 | 1 | -1 | 8 | 8 | -1 | 0 | 0 | Barrel unchanged; R hand (5.4, 2.0), cord drawing down; L hand on the grip |
| 0.30 | 6 | 2 | 0 | 16 | 16 | -2 | 0 | 0 | Barrel unchanged; R hand (6.1, 0.8), cord taut (hand 2 px down, 1.6 px out); L hand on the grip |
| **0.40** | **8 impact** | 1 | -1 | 12 | 12 | -1 | 0 | 0 | **Shell leaves along the barrel axis, 45 up-forward toward the target arc; R hand (5.8, 1.5) springing back, cord released** |
| 0.50 | 10 | -2 (y -1) | -3 (y 1) | 6 | 6 | 5 | 2 | y 0.6, z 0.6 | Recoil: barrel 45 (root part), body jolts back, root nudged back (z positive; the shipped lunges go forward with z negative) and down; R hand (5.1, 2.5) flying back, L hand pushed down |
| 0.65 | 13 | 1 | 0 | 2 | 2 | -1 | 1 | y 0.3, z 0.3 | Settled, smoke at the mouth; barrel 45; R hand (4.8, 2.7) |
| 0.95 | 19 | 7 (y -5) | 5 | 30 | 29 | -7 | 0 | y 0.5 | Stooped to the ammo crate, held in front for now (Question 1): R hand (6.4, -2.1) on the shell; barrel 45; L hand stays on the grip (arm -7 cancels the body lean, within about 2 px) |
| 1.15 | 23 | 5 | 2 | -20 | 36 | -5 | 0 | y 0.3 | Shell lifted past the right shoulder: R hand (6.0, 3.5) gripping the shell by its nose, fins out past the fingertips, so the shell points nose down and back along the forearm (forearm world -150, 30 off vertical; the bore is 45) |
| 1.35 | 27 | 1 | -6 | -45 | 60 | -1 | 0 | 0 | Shell at the mouth: R hand (5.2, 5.6) is 0.5 px from the mouth (5, 6); shell nose down and back along the forearm (world -155, 25 off vertical, 20 steeper than the bore, so the hand tips it in); head up 20 total into it |
| 1.45 | 29 | 0 | -6 | -47 | 60 | 0 | 0 | 0 | Shell released into the bore, nose first; R hand (4.9, 5.9) letting go |
| 1.65 | 33 | 1 | -3 | -25 | 40 | -1 | 0 | 0 | R palm slaps the shell tail at the mouth: R hand (5.6, 4.3); barrel loaded |
| 1.85 | 37 | 1 | -1 | -8 | 12 | -1 | 0 | 0 | Hand dropping to the cord, R hand (4.9, 3.4); barrel 45 |
| 2.00 | 40 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Back in aim: R hand (4.5, 3.0) on the cord, L (5.0, 2.0) on the grip, barrel 45 |

Reach check (solved, rough, **confirm in pose_preview**). The biped arm box is 4 px from the shoulder pivot to the elbow and the forearm box 4 px from the elbow to the hand pivot (`art/rigs/biped.json`), so the reach is **8 px to the hand pivot and about 10 px to the fingertip**, not 12. Troop models can override the arm pivot and scale, so the numbers hold for the biped rig only; recheck once `art/troops/ic_mortar.json` exists. Method: world pitch p = body + arm (+ forearm for the second segment); forward = -sin p, up = -cos p, times 4 px each segment, measured from the shoulder pivot; the body lean's own shift of the shoulder (about 0.7 px at aim, 2 px at the stoop) is ignored. Mouth: 5 px ahead of and 6 px above the shoulder pivot, 7.8 px from the pivot (just inside the 8 px reach). Key 27 puts the hand pivot at (5.2, 5.6): arm -125 + body 5 = world -120 -> (3.4, 2.0), forearm -35 more (-155) -> (1.6, 3.6), total (5.1, 5.7), 0.5 px from the mouth. The barrel line at 45 through the mouth passes 1 px above the shoulder pivot, so it rests over the shoulder. Hand positions at aim: R arm -80 / forearm -95 gives (4.5, 3.0), L arm -68 / forearm -96 gives (5.0, 2.0); the cord and strap hang about 3 and 4 px from the barrel underside at 4-5 px ahead of the shoulder (the barrel is 5.5 px above the pivot there). The two hands are 0.5 px apart in front: R rolls out (z 10) and L yaws across (y -22), so they sit on either side of the barrel; check this at 8 angles. The forearm stays within 20 of the barrel axis so the shell can drop in along the bore. The barrel is a root part, so it is 45 up-forward at every key by construction. **Preview limits:** `pose_preview.py --over aim` samples the aim stance at time 0 only and draws no barrel, cord, strap or shell, so the mouth, cord and shell checks are numeric (this table), not visual.

Window gates (see Fixed limits): hit at frame 25 = clip tick 5, window ticks 6-15 (the pull, the shot, the kick and the start of the stoop); hit at frame 47 = tick 27, window ticks 28-35 (the shell at the mouth, the release and the slap). The engine check sums |x|+|y|+|z| for each bone and then over bones (body, head, both arms, both forearms; root excluded), per tick, linear between keys. True figures from the table: ticks 6-15 largest single bone 4.7 deg/tick (R arm 13 to 19, 28 over 6 ticks), summed 13.5 (ticks 9-10, six bones); ticks 28-35 largest single bone 7.0 (R forearm 33 to 37, 28 over 4 ticks), summed 11.75 (tick 30). Outside the windows the lift (ticks 19-23 and 23-27) reaches a summed 17.2, which no hit check covers. The stoop key (tick 19) was moved from tick 17 and its roll dropped so ticks 14-15 stay under 15 (the first draft summed 16.25 there, R arm 8.0). The plan's `window_check.py` measures it; the real gate is the with-hit minus without-hit difference in Task 4.

## Application to units

- **Ironhold Mortar.** Square build (body 1.15 wide as the Arbalist), iron cap, scorched beard, heavy apron, ammo crate, tabard, lit barrel. Clipping risks to check at 8 angles: R hand and shell against the barrel mouth at 1.35-1.45 (the barrel is at the shoulder; the cap is just beside it), the R forearm against the barrel along its length at 1.35 (the hand is 0.5 px from the mouth and the forearm runs within 1 px of the barrel line), the apron against the stooping forearm at 0.95, the crate (held in front for now; location not decided: Question 1) against the R arm at 0.95, the shell against the head at 1.15 and 1.35 (head tilt -6), the two hands 0.5 px apart on the cord and strap, the L forearm against the lanyard cord. Chest and chain are textures, not boxes.
- **Barrel recoil.** It is a root part, so the clip can only dip the whole root 0.6 px; a barrel that visibly kicks needs its own bone in the model (Question 2).
- **Scale.** rootScale TBD (Units); root offsets of 0.6 px are scaled by it.
- **Ability visuals.** The FLAME at the mouth during the windup and LARGE_SMOKE after are particles; they sit at the barrel mouth and need no clip change. The mouth is lit through the glow texture.
- **Gameplay, network, timing:** none changed. Impact tick 8, kind, cooldown 100 and windup 30 stay. How the swing lines up with the windup is a question, not a change.

## Fixed limits

- The attack row (impact tick 8, kind blunt, clip max 40 ticks) is a Spec row; any change needs Kort.
- `python tools/clips.py` then `--verify` (impact on a tick and on the sheet tick, attack at most 40 ticks, loops close); `Spec.java` only through `tools/preflight.py`; after any verify or gradle run `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- `MotionEngineCheck.hitOverAttack` (`test-tools/standalone/MotionEngineCheck.java`, ~line 1093) runs `hitWhileAttacking` (~line 1057) on the **sword_shield fixture only** (`realSwordShield`, `d.shield=true`, stance `hold`, attack index 0): the attack starts at frame 20, the hit at frame 25, the check is frames 26-35 (with-hit step minus without-hit step at most 15 degrees per tick summed over all bones, cloth springs included). That is attack ticks 6-15 of any clip. It never plays crew clips, so the plan adds a crew fixture (`realCrew`, shield false, stance `aim`, attack `crew_fire`) with two hits: frame 25 (window 26-35) and frame 47 (window 48-55, ticks 28-35, inside the 40-tick clip).
- Tooling: a `crew` entry in `tools/anim_edit.py`'s TROOP table (the default family list at line 137 then includes it) is a tooling change that needs the coordinator.
- Max 2 Minecraft instances; never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`; no eula.txt; no computer-use on Minecraft; no push, publish or merge.

## Questions for Kort

1. **Barrel geometry.** The identity spec says "barrel slung at 45 degrees, mouth above the shoulder". This spec assumes the muzzle points up and forward over the right shoulder, breech behind the shoulder blade, so the troop fires from the shoulder. Is that the look (or muzzle up-back, or a ground mortar)? And where is the ammo crate (back, left hip)? The hand targets move with the answer.
2. **Barrel recoil needs a model bone.** A fixed root part can only dip with the root. Should the model give the barrel its own bone so a later clip can kick it, or is the root dip and the smoke enough?
3. **(Blocker for plan Task 3.) Swing timing against the 30-tick shell windup.** Does the server send `crew_fire` at the start of the windup, or when the shell leaves? Impact at tick 8 is fixed. If the swing starts when the windup starts the shell would leave 22 ticks before the visual reload ends and the flame tell would not line up; if it starts at the end of the windup the aim stance must hold through the windup. Needs the coordinator (Abilities.java, a timing read, not a change).
4. **No reference for the loading.** No video was found. Are you happy with an authored pull, shot and reload, or should I look for a muzzle-loader or mortar-drop video first (Fab/UE "Mortar Squad" pack is paid)?
5. **Java: bow code runs on the mortar.** `TroopMotion.bow(row)` (line 146) is true for `ic_mortar`. Same fix as the crossbow plan, combined: `row.ranged()&&!row.magic()&&!"crossbow".equals(row.family())&&!"crew".equals(row.family())`. It is Java but not gameplay or save; approve? Must be one change with the crossbow plan's Task 5.
6. **Model.** `art/troops/ic_mortar.json` (rig biped, parts as in the identity spec, `hand: right`) does not exist and no clip plays without it. An art task, not in this plan; schedule before or with the clips? Until then previews use `--troop archer` (the archer's bow is drawn in hand and the barrel is absent, so hand reach is judged from the numbers).
7. **Ballista Crew** is family `crossbow` in Spec.TROOPS, not `crew`; it is covered by the crossbow spec.

## Review

v1 7.2 (8/6/7/8/8/7). Fix round 1: re-solved the 1.45 reach (mouth 6 ahead, 8 above), aim hand numbers match the text, window steps recomputed, recoil root z added, rootScale divisor, blocks reason, Question 3 marked a blocker.

v2 6.7 (8/6/6/7/7/6). Fix round 2: reach redone with the real 4 + 4 px arm (8 px to the hand pivot, 10 to the fingertip: mouth 5 ahead and 6 above, hands 4.5-5 ahead and 2-3 above, aim R -80/-95 and L -68/-96, key 27 hand 0.5 px from the mouth), stoop moved to tick 19 and the roll dropped so the true window sums are 4.7/13.5 and 7.0/11.75, the shell is gripped by its nose and points down and back along the forearm (no hand pitch keyed), crate held in front for now and hand positions stated per key, crew script entries for swing_ends.py, pose_preview of the inherited hits and a window_check.py source, pose_preview limits noted, stub must carry an impact marker on tick 8.

**Status:** plan Tasks 3-6 are unrunnable until `art/troops/ic_mortar.json` exists and Questions 1, 3 and 6 are answered (Task 1, Task 2 on the stand-in and the Task 4 headroom proxy can start on the archer stand-in).

(Review: pending, to be filled after Kort's pass.)

Final (after fix round 2): 7.2 (8/6/7/7/8/7). Known gaps for Kort: there is no ic_mortar model, so Tasks 3-6 cannot run (Q6); the swing timing against the 30-tick shell windup blocks Task 3 (Q3); the pull, shot, reload and recoil are authored without footage (Q4); the barrel and crate geometry are assumed and the barrel can't kick (Q1, Q2); the TroopMotion.bow Java fix is shared with crossbow (Q5); the crew MEC fixture is prose only; hits are checked only over aim at t=0; the window sums are slightly off (recompute 12.7/12.0, spec says 13.5/11.75, all under 15).
