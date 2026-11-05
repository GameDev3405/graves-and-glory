# Caster troops: staff and palm casting in the Souls-like style

Kort's request, 2026-11-03 (overnight, for the morning review). Branch `work/anim-rework`. References: space.bar. anim "Essential Magic Staff Animation Pack - Update 2" (A), AbilityPreview "Dark Souls III - All Sorceries" (B), and Riko's "Mage Staff Animset" (C, the source for the idle, the overhead raise, hits and deaths). Saved at `C:/Users/kortt/Melty/anim-refs/caster/` with `SOURCES.md` (not committed). Docs only: no clip, Java, sheet or Spec.java was touched.

## Why

Kort wants the animations to follow good game footage in a Souls-like feel, applied to each unit properly. The casters have one clip today (`caster_cast`, 1.2 s) and no stance clips, so the stances are the procedural fallback. From the keys, the current cast's staff arm only rises 45 degrees while the free arm rises 68, so the staff is not clearly aimed at the target. Sorcery in the references reads as a deliberate gather and a clear release, with the focus pointing at the target.

## Goals

- The mage's staff points at the target at the release (within 15 degrees of level, see Key tables). No video shows a level staff on a target (A's palm cast has the staff low and behind; B releases overhead), so that pose is an engine rule from the weapon-direction rule, with no reference, and a viewer can read gather, release, hold, recover.
- A caster that is shooting at range (`aim`) holds the staff pointed at the enemy; near an enemy on hold or running it is braced (`guard`); otherwise it stands calm with the staff low like Riko's (`idle`, which also serves `rest`).
- No self-clipping (staff through head, cowl, robes, legs), at any angle.
- Each move is approved by Kort one at a time in game, as for the knight.

## Units

All four have `family = caster` (Spec.TROOPS). Range is in blocks.

| Troop | Name | Model | Held item (Spec) | Ability | Range | Own troop model? |
|---|---|---|---|---|---|---|
| `mage` | Sunweaver (Gilded), entity `husk`, support row | biped rig | `minecraft:blaze_rod` in Spec, but the model draws the custom `sun_staff` weapon in the right hand; Spec armour `minecraft:golden_helmet` and `minecraft:leather_chestplate` is not drawn (a troop never draws vanilla armour, TroopRenderer header, unit identity rule 3); the model's own cowl, mantle and vestment stand in | `mend_sunweaver` (heal, damage 0) | 18 | yes (`models/troop/mage.json`) |
| `hexer` | Gravecaller (Graveborn) | wither_skeleton (vanilla look) | `minecraft:amethyst_shard` | `blight_gravecaller`, damage 8 | 20 | no |
| `wm_seer` | Bloodhowl Seer (Wildmoot) | zombified_piglin (vanilla look) | `minecraft:bone` | `howl`, damage 3 | 14 | no |
| `tc_mist` | Mist Caller (Tidecallers) | evoker (vanilla look) | none (empty hands) | `mist`, damage 0 | 18 | no |

Mage model (`mage.json`): head box 8 x 8 x 8 px with a 6 x 4 x 1 face plate; cowl 10 x 10 x 10 px on the head; mantle 10 x 4 x 6; body 8 x 12 x 4; staff arm and forearm 6 px each. `rootScale` is `(24 - own root pivot y) / (24 - rig root pivot y)` (`TroopModel.java:311`); the mage's root pivot is [0,0,0], so it is 1 if the biped rig root is also at y 0, to be confirmed from the first render log (not read from the rig here).

### Hexer, wm_seer and tc_mist cannot play clips (code checked)

- `TroopMotion.java:143` in `row(...)`: `if(m.troop.isEmpty())return null;`. A mob with no troop model gets no clip row, so the engine never runs for it.
- `TroopRenderer.java` header comment: it "draws a troop with its own model ... and every other mob of the entity type through the vanilla renderer it was registered over".
- `models/troop/index.json` lists only archer, gl_marshal, knight, longbow, mage, sentinel.

So no per-unit key notes are written for the three. When they get models, the same tables apply with the focus swapped (see Application to units). I did not run the game to confirm. See Questions 1.

## Which stance a ranged caster plays (code checked)

The mage is `ranged=true, magic=true`, so `bow(row)` is false and it takes no bow carry. Stance comes from `BattleMath.stance` (BattleMath.java 190-209) and `MotionEngine.stanceKey`:

| Situation | Stance | Clip key | Fallback chain |
|---|---|---|---|
| ranged unit with an enemy in reach and in sight (Tactics.java:232), not running or falling back | `ranged_aim` | `aim` | aim, guard, carry, idle |
| ranged unit with an empty order | `ranged_rest` | `rest` | rest, carry, idle |
| order hold, or run or fall_back with an enemy | guard | `guard` | guard, carry, idle |
| no enemy, moving | carry | `carry` | carry, idle |
| no enemy, still (rare for a ranged unit) | idle | `idle` | procedural |

So when a caster is actually casting at range it is in `aim`, not `guard`. `MotionStack` adds `aimPitch` (positive-down, clamped to 60 degrees) to the RX of both arms in `aim`, so an aim pose is authored for aimPitch 0 and tilts down for a lower enemy by itself. Decision: author `idle` (it serves `rest` through the chain rest, carry, idle), `aim` and `guard`. `caster_cast` therefore plays over `aim` in the normal case and over `guard` on a hold order; the cast is checked over both bases.

## Inventory today

Folder `clips/biped/caster/` holds one clip. Everything else comes from `clips/biped/common/` (hits, deaths, fall, raise) or is missing.

| Engine clip | Played when | Source today |
|---|---|---|
| `idle`, `rest` | no enemy, still; ranged empty order | missing: procedural |
| `carry` | no enemy, moving | missing: falls back to `idle`, then procedural |
| `aim` | ranged unit, enemy in reach and sight | missing: falls back guard, carry, idle |
| `guard` | hold, or run/fall_back with an enemy | missing: falls back guard, carry, idle |
| `brace`, `wall`, `shield` | formation and shield stances | not used by casters (no shield) |
| `caster_cast` | the family's only attack row (body, magic, impact tick 10, medium) | family-owned |
| `hit_head/body/legs` (+ `_2`, `_3`) | taking a hit | inherited from common |
| deaths | by kind and direction; `death_magic[_2]` exist | inherited from common |

The current `caster_cast` keys body, head, both arms and forearms, the right hand and the root; first and last keys are 0, impact marker 0.5 s, length 1.2 s. At the impact key the right (staff) arm is -45 degrees, the right hand +50 and the left arm -68, root (0, 0.5, -1.7) px.

## References

Checked timestamps are listed in `SOURCES.md`; every one has extracted frames in `anim-refs/caster/frames/<move>/` and a contact sheet in `sheets/`.

- **A** staff pack: battle-mage stance 0:15.75-0:19.0 (`frames/battle_stance`, the source of `guard`); palm cast 0:30.5-0:32.5 (`frames/palm_cast`, the source of the free open palm only: the staff is low and behind in that clip, so A does not support a level staff). The standing guard at 0:35.75-0:37.75 has no extracted frames and is not used.
- **B** Dark Souls III: Soul Arrow cast 0:12.9-0:14.3 and 0:15.7-0:16.9 (`frames/soul_arrow`), staff raised overhead, release forward. Camera is behind a small figure, so broad poses and rhythm only.
- **C** Riko, Mage Staff Animset (youtu.be/96GEiO0qu7k), a wizard in a red coat with a long staff, one animation at a time on an open floor:
  - Idle, 0:08-0:28 (`frames/riko_idle`, `sheets/riko_idle.png`, frames 1-3 at 8-10 s): staff held low in the right hand at the hip, pointing down and forward at roughly 45 degrees, free hand open at the thigh, a slow weight shift. `idle` matches it (head forward and 45 degrees down).
  - Overhead raise, 3:49-3:53 (229-233 s, `frames/riko_raise`, `sheets/riko_raise_frames.png`, 4 fps): at 230.5-231.0 s the right hand sweeps the staff up from the hip to overhead, the staff tip ending up about 20 to 30 degrees off vertical, head tilted up, the free arm thrown out wide to the side at shoulder height. At 231.25-231.5 s it is held there with the elbow bent. This is the source of the wind-up key.
  - Hits, 2:28-2:48 (148-168 s, `frames/riko_hit`, `sheets/riko_hit_frames.png`, 2 fps): at 156.5 s a body hit folds the torso forward about 25 degrees with both arms trailing; at 159.0 s a head hit rocks the head and shoulders back and the figure steps back; at 160.5 s a hit from the side turns the torso away; at 163.0 s a leg hit drops into a stumbling crouch. The staff stays in the hand in every one.
  - Deaths, 5:04-5:30 (304-330 s, `frames/riko_death`, `sheets/riko_death_frames.png`, 1.5 fps): the figure doubles over with the staff planted (313.3-314.0 s), drops to the knees (316.0 s), then falls onto its side or back with the staff let go beside it (316.7 s on). A second death at 319-321 s falls straight back, and at 326.7 s the free arm is thrown up as it falls.
- D Excalibur Studios "Mage with Staff Animations" was downloaded and checked: the footage is an unlabelled mannequin crowd with no staff, so it is not used.

Weapon direction sources: the gather (0.28) is from C's raise and B; the free palm is from A's palm cast; the staff head level on the target at 0.5 and in `aim` is an engine rule (weapon-faces-the-move), no reference.

Decisions from C for hits and deaths: the common hits (`hit_head/body/legs`) already fold, rock and stumble as C does, so the caster keeps them unchanged. The only caster-specific point is that the staff stays held through a hit and a death starts with the staff arm releasing. A caster-owned death is not proposed (the staff cannot be dropped by clip data; `death_magic` stays); Kort may ask for one.

## Weapon direction, and the staff-axis maths

The `sun_staff` weapon grips at the hand origin with zero rotation; in the hand frame it spans x +/-3.5, y -24..+11, z +/-1.5 px (about 35 px long). The staff axis is the hand's local Y. I could not read from the JSON which end is the head; I assume the short end (+Y, 11 px) is the sun head and Task 1 confirms it on a render. **Every angle table below is provisional until that render confirms the head end.** Fallback: if the head is the other end (-Y), subtract 180 from every `right_hand` x in every table (idle, guard, aim, caster_cast) and re-check the render.

All the staff pitch is rotation about X, so the pitches add up. Define the chain pitch

`phi = body.x + arm.x + forearm.x + hand.x` (degrees, a negative arm RX raises the arm forward, as the existing cast does)

With head = +Y end the head direction is (forward, up) = (sin phi, cos phi). So the head elevation above horizontal, pointing forward, is `e = 90 - phi` for phi between 0 and 180:

| phi | head points |
|---|---|
| 0 | straight up |
| -10 | up, 10 degrees back |
| 90 | level, at the target |
| 98 | forward, 8 degrees down |
| 135 | forward, 45 degrees down |

Y and Z rotations only swing the staff sideways (they are the "outward 5 degrees" style notes) and are ignored in `phi`. The hand x values of 100 to 170 are expected: the staff is modelled along the arm, so pointing its head forward needs the wrist turned most of the way round. If a hand over 130 degrees looks wrong in the preview, move some into the forearm in the preview, keeping `phi`. `aimPitch` adds the same angle to arm.x of both arms in `aim`, so a positive aimPitch raises `phi` and lowers the head by exactly that angle, which is the wanted behaviour for a lower enemy. All numbers below are derived from this maths and the existing cast keys, not yet rendered; the first plan step compares them with a render and corrects them.

## Key tables

Units: rotation in degrees (x, y, z), root in pixels (x, y, z; negative z is forward, as in the existing cast). Stance clips (`idle`, `guard`, `aim`) hold absolute values for the keyed bones; the cast holds deltas. Unlisted bones are not keyed (legs stay free so foot planting works). The new idle, guard and aim step with the root only: no leg keys, so the step cycle and foot planting stay procedural and the crouch is the root offset. Body yaw sign: negative y turns the staff (right) side toward the target, as in the existing cast where impact is -6 and the wind-up +14; Task 1 confirms the sign on a render.

### idle (loop 4.0 s; also serves `rest`)

| t (s) | bone | x | y | z | Staff direction |
|---|---|---|---|---|---|
| 0.0 | body | 0 | 0 | 0 | |
| 0.0 | head | 0 | 0 | 0 | |
| 0.0 | right_arm | -8 | 0 | -6 | phi = 135 (-8 -12 +155), as C: head forward and 45 degrees down, staff low at the hip |
| 0.0 | right_forearm | -12 | 0 | 0 | |
| 0.0 | right_hand | 155 | 0 | -5 | the staff hangs diagonally forward of the right hip, clear of the leg |
| 0.0 | left_arm | -4 | 0 | 6 | open hand at the thigh |
| 0.0 | left_forearm | -8 | 0 | 0 | |
| 0.0 | root | 0 | 0 | 0 | |
| 2.0 | body | 1 | 0 | 0 | breath (phi = 136) |
| 2.0 | head | -1 | 0 | 0 | |
| 2.0 | root | 0 | 0.15 | 0 | |
| 4.0 | all above | as 0.0 | | | loop closes |

### guard (loop 3.0 s)

| t (s) | bone | x | y | z | Staff direction |
|---|---|---|---|---|---|
| 0.0 | root | 0 | -2.0 | 0 | crouch (visual only) |
| 0.0 | body | 8 | -25 | 0 | staff side leads |
| 0.0 | head | -8 | 20 | 0 | eyes on the enemy |
| 0.0 | right_arm | -20 | 0 | -10 | phi = 98 (8 -20 -50 +160): head forward, 8 degrees down, at hip height across the body |
| 0.0 | right_forearm | -50 | 0 | 0 | |
| 0.0 | right_hand | 160 | 0 | 0 | |
| 0.0 | left_arm | -45 | -10 | 8 | free hand at the chest, palm toward the enemy |
| 0.0 | left_forearm | -50 | 0 | 0 | |
| 1.5 | body | 9 | -25 | 0 | settle |
| 1.5 | root | 0 | -2.3 | 0 | phi = 99 |
| 3.0 | all above | as 0.0 | | | loop closes |

### aim (loop 3.0 s, authored for aimPitch 0)

| t (s) | bone | x | y | z | Staff direction |
|---|---|---|---|---|---|
| 0.0 | root | 0 | -1.0 | 0 | |
| 0.0 | body | 0 | -20 | 0 | turned toward the enemy |
| 0.0 | head | 0 | 15 | 0 | |
| 0.0 | right_arm | -55 | 0 | -5 | phi = 90 (0 -55 -20 +165): head level, pointing at the enemy |
| 0.0 | right_forearm | -20 | 0 | 0 | |
| 0.0 | right_hand | 165 | 0 | 0 | |
| 0.0 | left_arm | -70 | -10 | 8 | open palm toward the enemy |
| 0.0 | left_forearm | -15 | 0 | 0 | |
| 1.5 | body | 1 | -20 | 0 | breath; phi = 91 |
| 1.5 | root | 0 | -1.15 | 0 | |
| 3.0 | all above | as 0.0 | | | loop closes |

### caster_cast (deltas, length 1.2 s, impact 0.50 s, over `aim`; also valid over `guard`)

| t (s) | bone | x | y | z | Staff direction |
|---|---|---|---|---|---|
| 0.00 | all | 0 | 0 | 0 | the stance (level at the enemy in aim; 8 degrees down in guard) |
| 0.28 | root | 0 | -0.5 | 0.8 | gather dip, a hair back |
| 0.28 | body | -4 | 12 | 0 | turned 12 degrees away from the target |
| 0.28 | head | -3 | -12 | 0 | still looks at the target |
| 0.28 | right_arm | -35 | 0 | -25 | phi = 1 over aim (90 -4 -35 -20 -30) and 9 over guard: head up, elevation 81 to 89 degrees. C's raise holds the tip 20-30 degrees off vertical; this is nearer vertical to clear the cowl (see Clearance) |
| 0.28 | right_forearm | -20 | 0 | 0 | |
| 0.28 | right_hand | -30 | 0 | 15 | phi = 1 over aim: head up. Hand z +15 cancels most of the arm's outward swing so the head end leans out, not in |
| 0.28 | left_arm | 60 | 10 | 55 | free arm flung wide at shoulder height, as C at 230.75 s |
| 0.28 | left_forearm | -5 | 0 | 0 | |
| 0.50 | root | 0 | 0.5 | -1.7 | forward lunge |
| 0.50 | body | 4 | -6 | 0 | turned into the target |
| 0.50 | head | 0 | 6 | 0 | |
| 0.50 | right_arm / forearm / hand | 0 | 0 | 0 | phi = 90 +4 = 94 over aim (head 4 degrees down); 102 over guard (12 down). Both within 15 of level |
| 0.50 | left_arm | -8 | -6 | 0 | palm pushes toward the target |
| 0.50 | left_forearm | 0 | 0 | 0 | |
| 0.62 | root | 0 | 0.5 | -1.7 | hold |
| 0.62 | body | 4.2 | -6.4 | 0 | |
| 0.62 | head | 0 | 6.4 | 0 | |
| 0.62 | right_arm | 3 | 0 | 0 | head drifts down 3 more degrees, 5 at most |
| 0.62 | left_arm | -8 | -6 | 0 | |
| 0.85 | root | 0 | 0.15 | -0.5 | |
| 0.85 | body | 1.2 | -1.8 | 0 | |
| 0.85 | head | 0 | 1.5 | 0 | |
| 0.85 | right_arm | 1.5 | 0 | 0 | head back toward the stance angle |
| 0.85 | left_arm | 20 | 0 | 0 | |
| 0.85 | left_forearm | -10 | 0 | 0 | |
| 1.20 | all | 0 | 0 | 0 | the stance |

At 0.28 the staff is raised, free arm wide, as in C's raise at 230.5-231.0 s; at 0.50 the staff head is level on the target (engine rule, no reference) and the open palm points at it (A's palm cast). **Clearance at 0.28 (estimate from `mage.json`, to be measured in Task 1).** The cowl is a 10 x 10 x 10 px box on the head (origin -5,-9,-5): 5 px to each side and 5 px forward of the head centre, y -21 to -11 in body space. The right shoulder pivots at x -5, y -10; with arm -35 and forearm -20 forward and 25 degrees out, the hand is about 4 px outside the shoulder (x about -9), 6 px below it and 8 px forward. The head end stands 11 px above the grip: with hand z +15 (net outward swing 10 degrees) it sits at x about -7, 2 px outside the cowl edge, and at z about -6 against the cowl front at -5. So lateral clearance is about 2 px and forward clearance under 1 px. Without the hand z +15 the head end would be at x -3, inside the cowl. If the render shows contact, add outward arm z in 5 degree steps, not change the cowl.

The body at 0.28 and 0.50 differs from the stance by the 12 degrees away and 6 into the target of the existing cast, so the turn is not new.

## Stances (summary and videos)

| Stance | Pose (weapon direction) | Video |
|---|---|---|
| `idle` (also `rest`) | Staff held low at the right hip, head forward and 45 degrees down, free hand open at the thigh, slow breath. | C idle 0:08-0:28 (frames/riko_idle) |
| `aim` | Body turned toward the enemy, staff arm raised with the head level on the enemy, open free palm toward the enemy. | Staff level on the target: engine rule, no reference. Open palm: A 0:30.5-0:32.5 (frames/palm_cast) |
| `guard` | Battle-mage crouch, body turned 25 degrees, staff across the body at the hip with the head forward and 8 degrees down, free hand at the chest. | A 0:15.75-0:19.0 (frames/battle_stance) |
| `carry` | No clip; falls back to `idle`. Add one only if the idle staff angle fails in game (Task 3). | none |

The crouch is visual only (hitbox and eye height unchanged), as ruled for the knight.

## Moves

One attack row exists for the family (`caster_cast`). Timing is fixed: impact marker at 10 ticks = 0.5 s, clip at most 40 ticks. The existing 1.2 s length is kept. The cast gathers the staff overhead like C and B (B releases overhead, not level), then releases with the staff head level on the target (engine rule, no reference) and the free palm open toward it like A's palm cast. Missing from the videos: a kill flourish or taunt, not proposed. Per-troop flavours are the same clip with the focus swapped; a second cast variant would need a new `Spec.ATTACKVARIANTS` row, which needs Kort.

## Application to units

- **Mage (Sunweaver).** The staff is about 35 px long, 7 px across. Clipping risks: the staff beside the cowl at the wind-up, its lower end through the robe skirt and shin in guard and idle, the free hand through the mantle at the chest. Wall and brace are not used. Whether the heal plays `caster_cast` is not checked (the mage has damage 0); a gameplay-side check, and a new variant row would need Kort.
- **Hexer, Seer, Mist Caller.** Not playable until modelled (code quote above). When they are: the shard and bone are tiny, so "staff head aims at the target" becomes "tip points at the target, held in the fist, forearm level, the tip a hand-length past the fist"; use the `aim` and `caster_cast` tables with the hand x set so the forearm points at the target (right_hand x about 0 to 20 instead of 165). The mist caller has empty hands on an evoker (folded arms is its default): the focus is the open right palm toward the target at the release, the left hand tracing a short circle at the gather; the engine opens the folded arms while a clip holds an arm (confirm in the render). Wither skeleton and evoker are taller than a player; check scale before judging clipping.

## Fixed limits

- `caster_cast` impact stays at 10 ticks (marker 0.5 s), clip at most `BattleMath.ATTACK_CLIP_MAX_TICKS` = 40 ticks. Any change needs Kort.
- Attack rows and variants are fixed. First and last keys of every keyed channel of an action or hit clip are 0.
- `MotionEngineCheck` "hit over attack" (test-tools/standalone/MotionEngineCheck.java:1093, `hitOverAttack`) calls `hitWhileAttacking`, which builds the family with `realSwordShield(...)`, a hold stance with a shield, and attack index 0. It therefore tests the sword-and-shield fixture only and says nothing about the caster. The 15 degrees per tick limit (ticks 26-35, cloth springs included) is still the bar, so the plan adds a caster-specific check (Task 5) rather than relying on that gate.
- `python tools/clips.py` then `--verify` must pass. After any verify or gradle run: `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- Max 2 Minecraft instances. Never touch PIDs 29360, 4580, 6048 or C:/GGMatch. No eula.txt. No computer-use on Minecraft. No push, publish or merge.

## Questions for Kort

1. **Models for hexer, wm_seer and tc_mist.** Only the mage has a troop model, so only the mage can show these animations. Should those three be modelled next, or is the vanilla look intended for now?
2. **Guard for casters.** I propose a low battle-mage crouch in guard (used on hold, run, fall back). Should the guard stay upright like the idle?
3. **Staff head.** In the sun-staff model, is the glowing head the short end (up when held beside the hip)? I assumed yes; Task 1 checks it on a render.
4. **Heal casts.** Should the mage's heal use the same cast pose as an attack? I assumed yes.
5. **Deaths.** Hits and deaths: common clips kept, no Kort needed. A caster-owned death (staff dropped) only if Kort asks.

## Review

Review v1 graded this spec 6.8 out of 10: coverage 8, references 6, weapon direction 8, per-unit 5, limits 9, execution 5.

Round 2 (final): v2 7.0 (8/6/7/6/8/7). Fixes: aim and release labelled engine rule, no reference (A supports only the palm); idle matched to Riko (staff down-forward 45 degrees); guard cites frames/battle_stance only; 0.28 key and plan Expected agree (phi 1, elevation 81-89); mage row with entity, armour and cowl clearance; all angle tables provisional with the -180 hand fallback; hits/deaths: common kept, no Kort needed.

Fixed in round 1:
- Key tables with numbers for `idle`, `guard`, `aim` and `caster_cast`: time, bone, x/y/z, staff direction; the staff-axis maths is now a formula (`phi`) with a head-elevation table, and the idle, guard and aim step with the root only.
- Ranged stance: checked in BattleMath, Tactics and MotionEngine. A ranged caster plays `aim` and `rest`, so `aim` has its own pose, `idle` serves `rest`, and the cast is checked over `aim` and `guard`.
- Overhead raise, hits and deaths sourced from Riko (video C) with frames on disk; Excalibur downloaded and rejected.
- Other three troops: code lines quoted showing they cannot play clips.
- Hit-over-attack gate: stated that it tests the sword-and-shield fixture only; the plan adds a caster check.
- Plan has absolute scratchpad script paths and numeric Expected lines.

Still pending: Kort's review of this spec and the plan.
