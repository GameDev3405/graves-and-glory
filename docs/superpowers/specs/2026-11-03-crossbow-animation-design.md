# Crossbow troops (Arbalist, Ballista Crew) in the Souls-like style

Kort, 2026-11-03. Branch `work/anim-rework`. References are saved locally in `C:/Users/kortt/Melty/anim-refs/crossbow/` (frames and sheets, not committed; `SOURCES.md` lists each timestamp).

## Why

Kort wants animations with Souls-like weight for every troop, applied correctly to the unit. The crossbow family has none today: no clip folder, so every stance is procedural, and the Ironclad crossbow troops have no troop model at all (see Inventory). The family needs a level, shouldered aim, a hard, fast loose (the bolt leaves at tick 4), and a visible reload cycle after it, in the same readable, heavy language as the new sword-and-shield guard.

## Goals

- A new `crossbow` clip family (`clips/biped/crossbow/`): stances `idle`, `carry`, `aim`, `rest`, and the attack `crossbow_loose` with its reload.
- Weapon direction is right at every key (Kort's rule): the stock sits at the shoulder and the bolt is level at the target at the loose; during the reload the crossbow points down and forward, never across the head or the troop's own body. Each key in the Moves section states it.
- No self-clipping at 8 angles on both units, including the bolt case, goggles and coat skirt.
- Each clip approved by Kort in game, on the compare page beside the video, one at a time.

## Units

Read from `Spec.TROOPS` (last field = family) and `sheets/design.json`; looks from `docs/superpowers/specs/2026-10-17-unit-identity.md`.

| | `ic_arbalist` Arbalist | `ic_ballista` Ballista Crew |
|---|---|---|
| House, rarity | Ironclad, common | Ironclad, rare |
| Entity, rig | pillager (`ArmyPillager`), biped | pillager (`ArmyPillager`), biped |
| Group, tags | ranged | siege, tag `siege` |
| Held item (stats) | `minecraft:crossbow` | `minecraft:crossbow` (the identity spec: no weapon model, the frame is the engine) |
| Helmet, chest | chainmail_helmet, leather_chestplate | iron_helmet, leather_chestplate |
| Shield column (Spec.TROOPS) | `none` | `siege` (a siege tag, not a shield: no shield model, no shield clips) |
| Health, damage | 20, 6 | 24, 10 |
| Range, cooldown | 24, 60 ticks (3.0 s) | 34, 80 ticks (4.0 s) |
| Speed | 0.25 | 0.20 |
| Ability | `armour_bolt` (passive, stance mechanic) | `pierce_ballista` (onhit pierce, radius 4, amount 0.6) |
| Build (identity spec) | square: body scale 1.15/0.95/1.1, legs 1.1/0.8/1.1; goggles, short beard, leather coat skirt, bolt case on the back, tabard | same build; iron cap, `ballista_frame` and `ballista_wheels` root parts (oak frame, 16 px bow arms, loaded bolt) |
| Weapon model | `arbalest`, held level at the chest | none (the frame) |
| `rootScale` | about 0.90 (estimate only, valid if the model lowers the root pivot about 2.4 px for its 0.8 legs, so (24 - 2.4) / 24; if the model json has no `root` bone entry, as for knight, archer and sentinel, it is exactly 1.0; compute it from the files with the one-liner in plan Task 6) | same |

Both use `idleArms` hang unless the model file sets otherwise (the archer sets `hang`; `forward` would pitch both arms 80 degrees forward whenever no clip holds them, so the crossbow model should set `hang` and let the clips hold the arms).

## Inventory today

`TroopMotion.family` finds a family only for a model whose `troop` id is in `Spec.TROOPS`. Folder `clips/biped/crossbow/` does not exist.

| What the engine plays | Source today |
|---|---|
| Troop model for `ic_arbalist`, `ic_ballista` | **missing**: `art/troops/` holds only archer, gl_marshal, knight, mage, sentinel. Both render as a vanilla pillager and `TroopMotion.row` returns null, so no clip plays |
| Stances idle, carry, aim, rest | **missing** (procedural). `common/` has no stance clips. Fallback chains: aim -> guard -> carry -> idle; rest -> carry -> idle; idle -> carry |
| Attack `crossbow_loose` (body, bolt, impact 4, medium) | **missing** (no clip, `attackIndex` -1) |
| `crossbow_loose_wall` | none, none needed: ranged troops have no wall or brace stance |
| Hits `hit_head/body/legs` x3 | inherited from `common/` (generic, authored for a bare arm-down pose) |
| Blocks | none (shield-only; archers raise nothing) |
| Deaths (`death_*`, fall), scene `raise` | inherited from `common/` |
| Bow draw carry | `bow(row)` is true for any ranged, non-magic troop, so the **bow hand twist and draw** (`TroopMotion.twist`, `draw`, `drawSeconds`) would also run on a crossbow: see Questions 2 |

The shot itself: the crossbow family has no draw-ahead (`Tactics.drawsAhead` is bow only), so the bolt is released at impact tick 4 of the attack, and the cooldown (60 or 80 ticks) then covers the reload. The clip is at most 40 ticks, so the troop is back in the aim stance, loaded, for the rest of the cooldown.

## References

Full list in `C:/Users/kortt/Melty/bannerbound/work/ANIMATION-REFERENCES.md` (crossbow section: Revolt Studios, Tod's Workshop). Used:

1. **Elden Ring Attack Animations [Bow, Crossbow, & Ballista Attacks]**, Video Game Animation References, https://www.youtube.com/watch?v=Qvo487abShM, local `elden-ring-bow-crossbow-ballista.webm`. Souls-like, third person. Crossbow idle 13:37, light attack and reload 13:58-14:50 (reload frames used: 14:01.7-14:02.3, 14:06.7-14:07.3; aim hold 14:03.3-14:05.7 is chest height, forward-right), ballista 21:23-22:15 (crank 21:28-21:36). The torso follows the camera, so the view is never straight on.
2. **The Ironbelly Arbalest Crossbow - UE4**, Ironbelly Studios, https://www.youtube.com/watch?v=4YlwCnuqFBM, local `ironbelly-arbalest.mkv` (35 s, first person: the hand work of the reload).

Not downloaded (Kort's list, unchecked): Revolt Studios Hunting Pack https://www.youtube.com/watch?v=wH_w4QR5Xhk, Tod's Workshop spanning methods https://www.youtube.com/watch?v=V-8eOBOffi4.

## Stances

Rotation is degrees, position pixels, added to the bone's rest pose. Signs (from the bow and knight clips): arm pitch negative raises the arm forward; forearm pitch negative bends the elbow; body pitch positive leans forward; head pitch negative looks up; body yaw positive turns the left side to the front (the bow clip turns +90 with the bow in the left hand). All values are starting values to tune with `pose_preview.py`, which is the check. Legs are left unkeyed so `MotionFeet` plants them.

The weapon is mounted in the right hand (the trigger hand, `hand: right` in the troop model); the left hand rides the forestock by pose. Muzzle estimate for a stance (absolute, rough): muzzle above level = -(body x + R arm x + R forearm x + R hand x) - 90, with the mount defined as level at a pitch sum of -90 (see Moves). The hand rotations below are derived from it, not guessed.

| Stance (loop) | Posture | Key values (pitch x unless shown; R/L = right/left; arm / forearm / hand) | Muzzle estimate and weapon direction |
|---|---|---|---|
| `aim` (3.0 s, sway 0.5 deg) | Crossbow shouldered, left side forward, cheek on the stock, 3/4 to the target | body 4, y 25; head -2, y -25, z -6; R 55 up (-55, y 10, z 12) / -85 / hand 46; L -85 (y -10, z -6) / -20 / hand 11; root 0 | Sum 4-55-85+46 = -90: level. Stock at the right shoulder, bolt level at target chest height |
| `idle` (4.0 s) | Port arms: crossbow across the chest, relaxed | body 3, y 8; head y -8; R -25 (z 12) / -70 / hand -33; L -45 (y -10, z -6) / -60 / hand -23 | Sum 3-25-70-33 = -125: 35 above level. Muzzle forward and up about 35, left of the face line, stock at the right ribs |
| `carry` (2.4 s, march) | Same hold, body leaned 4, sway damped | body 4, y 6; head -2, y -6; R -25 / -70 / hand -19; L -45 / -60 / hand -9 | Sum 4-25-70-19 = -110: 20 above level. Muzzle forward and up about 20 |
| `rest` (4.0 s, `ranged_rest`) | Crossbow lowered at the right hip, left arm hanging | body 0; head 0; R +5 (z 8) / -20 / hand -35; L 0 / -5 / hand 0 | Sum 5-20-35 = -50: 40 below level. Hand at the hip (arm hangs, forearm only 20 forward), muzzle down-forward about 40, never across the legs |

(The first draft's rest pose, R arm -15 and forearm -30, raised the hand toward the chest; this one keeps it at the hip, matching Elden Ring 13:58-14:01, crossbow held low at the hip.)

## Moves

Only one attack row exists for the family, and it is fixed: `crossbow_loose` (zone body, kind bolt, impact tick 4 = 0.20 s, weight medium). The clip is an offset over `aim` (first and last keys of every keyed channel 0), 1.8 s (36 ticks) long, near-even key spacing, within `BattleMath.ATTACK_CLIP_MAX_TICKS` = 40.

Reference basis, stated honestly. **The shouldered aim and the loose are authored, no reference**: the Elden Ring crossbow is held at chest height and pointed forward-right (SOURCES.md, 14:03.3-14:05.7), and neither video shows the loose frame. Only the reload has footage: Elden Ring 14:01.7-14:02.3 and 14:06.7-14:07.3 (crossbow at the belly, roughly level, torso upright to slightly hunched, knees slightly bent: `frames/xb_reload/f_015`; the raise is `frames/xb_raise`) and Ironbelly 2.0-7.7 s (hand work: string hauled to the latch, bolt laid in the groove, crossbow levelled; `frames/arb_reload1`). See Questions 6. Retime: the video's reload (about 5 s) is compressed into ticks 7-34; the lowering gets 14 ticks so no bone exceeds 6 degrees per tick.

Muzzle estimate (rough, a pre-pose guess only): muzzle below level, in degrees, is about `body x + R arm x + R forearm x + R hand x` of the deltas from `aim` (positive = muzzle down). It ignores yaw, roll, the left hand and the shoulder offset, so **`pose_preview.py` is the check**: render each key from the side and top and read the muzzle off the picture; if they disagree, the picture wins and the table is changed. Weapon mount (needed for the estimate to hold): the held crossbow is mounted on `right_hand` with its muzzle (+Z) level when body + arm + forearm + hand pitch sums to -90; this is set in the troop model's weapon transform (the archer stand-in draws the archer's bow with its own mount, so previews on the stand-in show the bow's orientation, not the crossbow's, until the mount is set).

Hands at aim (absolute, degrees): R hand (46, 0, 0) lifts the wrist so the muzzle comes level (4 - 55 - 85 + 46 = -90); L hand (11, 0, 0) cradles the forestock (4 - 85 - 20 + 11 = -90). The crossbow is oriented muzzle +Z, stock end at the right hand's grip, held on the right shoulder; its net yaw must be 0 to the target line in the top view (body y 25 is cancelled by the arm and hand yaw; tune R arm y and R hand y, not the table, if the top view is off). In `crossbow_loose` both hands are keyed at 0 delta throughout (the wrist keeps its aim angle relative to the forearm), so the kick comes from the arms and body alone.

`crossbow_loose` key table (deltas from `aim`; degrees pitch, yaw `y` and roll `z` in brackets; **root column is position in pixels** (`y` = down), 0 where blank; R hand and L hand pitch deltas are 0 at every key):

| t (s) | tick | body | head | R arm | R forearm | L arm | L forearm | root (px) | Muzzle below level (estimate) / weapon direction |
|---|---|---|---|---|---|---|---|---|---|
| 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0: stock at the shoulder, bolt level at the target |
| 0.10 | 2 | 0.5 | 0.5 | 1 | 0 | 1 | 0 | 0 | 1.5: cheek weld, bolt still level |
| **0.20** | **4 impact** | 0 | 0 | 0 | 0 | 0 | 0 | 0 | **0: bolt level at the target chest, stock at the shoulder** |
| 0.25 | 5 kick | -3 (y -2) | -2 (y 2) | -6 | 4 | -5 | 3 | z -0.4 | -5: muzzle up about 5, stock punched into the shoulder |
| 0.35 | 7 settle | 1 | 1 | 2 | 0 | 2 | 0 | 0 | 3: level again, start of the lowering |
| 0.55 | 11 | 3 (y -2) | 3 (y 1) | 11 (z -2) | -13 | 13 | 7 | y 0.2 | 1: elbows dropping, crossbow leaving the shoulder, still level |
| 0.80 | 16 | 7 (y -4) | 6 (y 3) | 26 (z -4) | -29 | 29 | 16 | y 0.5 | 4: crossbow at the chest dropping to the belly, nearly level |
| 1.05 | 21 haul | 10 (y -6) | 10 (y 5) | 40 (z -6) | -45 | 45 | 25 | y 0.8 | 5: stock at the belly, forearms near horizontal, muzzle level to slightly down, free hand hauls the string, torso hunched about 10 |
| 1.25 | 25 lay | 10 (y -4) | 10 (y 4) | 28 (z -4) | -33 | 34 | 16 | y 0.5 | 5: head over the groove, bolt laid, muzzle slightly down-forward, never across the head |
| 1.55 | 31 lift | 6 (y -2) | 4 (y 2) | 14 | -16 | 16 | 8 | y 0.2 | 4: crossbow rising toward the shoulder, muzzle toward the target |
| 1.70 | 34 | 2 (y -1) | 2 (y 1) | 6 | -7 | 7 | 3 | y 0.1 | 1: nearly level, stock near the shoulder |
| 1.80 | 36 end | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0: back in aim, stock at the shoulder, bolt level at the target |

What the reload frames show (so the keys match them): Elden Ring `xb_reload` f_015 (about 14:07.4): crossbow held at the belly, roughly level (muzzle a few degrees down, forward-right), forearms near horizontal, torso upright to slightly hunched, knees slightly bent. Ironbelly `arb_reload1` f_040 (6.0 s, bolt being laid): crossbow level, bolt in the groove. So the reload is a drop of the whole crossbow from shoulder to belly with the muzzle staying near level, not a muzzle-down dip; the forearm flex (-45) cancels the arm drop (+40) to keep it level.

Step caps (checked by the plan's `window_check.py`, Task 4): no bone moves more than 6 degrees per tick (summing x, y and z); the largest is R forearm, 45 over 14 ticks (about 3.2 per tick). Summed over all bones the lowering is about 13.5 degrees per tick (haul totals about 190 over 14 ticks) and the lift 12.5-14.5. Keys are near-evenly spaced, not eased, for that reason.

Hit windows (the check's fixture starts the attack at frame 20, so clip tick = frame - 20): a hit at frame 25 is clip tick 5 (the kick); its window, frames 26-35, is clip ticks 6-15: the settle and the **lowering**. A second hit at frame 47 (clip tick 27) has its window at frames 48-57, but the clip ends at tick 36, so the window is **clamped to ticks 28-35**: the **lift**.

Hands are keyed 0 throughout the loose (see above); the crossbow must not roll in the hand during the kick. Markers: `impact` 0.20 only (no `draw` / `full`, so `drawSeconds` has no bow pull to show).

Legs: unkeyed. The hunch at ticks 11-26 uses body pitch and the root dip; planted feet bend the knees (knight plan, Review Focus 2).

### Ballista and the shared clip

One family means one `crossbow_loose` for both troops. For the Ballista Crew the same aim and loose read as a crewman holding a heavy two-hand piece at the shoulder; the frame's stock sits where the Arbalist's crossbow stock is, and the reload reads as cranking the winch (Elden Ring 21:30-21:33 is a wide, hunched crank at the frame). See Questions 1.

## Application to units

- **Arbalist.** Chainmail helmet, leather chestplate, goggles, short beard, leather coat skirt, bolt case on the back (back gear is allowed; no pennant on the back), tabard. Square build: the body is 1.15 wide, so shoulders sit about 0.6 px further out and the aim yaw of 25 degrees keeps the stock clear of the arm. Clipping risks to check at 8 angles: the stock end against the cheek or the goggles at `aim` (head roll -6 keeps the cheek on the stock; reduce it if the stock enters the head), the coat skirt against the lowered crossbow at the haul key (body pitch 10 swings the skirt forward), the tabard against the muzzle at 0.55-1.05, the left hand against the forestock at the kick. Chainmail and chest are skin textures, not boxes, so they add no clipping.
- **Ballista Crew.** Iron cap, leather chestplate, tabard, `ballista_frame` (root part) with 16 px bow arms and wheels. The frame stays at root, so the clip cannot lift it: the lowering and haul poses must not carry the arms away from the frame's stock. This needs Kort's answer on how the frame is built (Question 1). Risks: arms through the frame at the haul key, wheels vs the legs as the root dips.
- **Scale.** Both are square builds (rootScale about 0.90, estimated). Root offsets are scaled by it, so the 0.8 px dip becomes about 0.7 px.
- **Ability visuals.** `armour_bolt` is a passive stance mechanic and `pierce_ballista` an onhit effect; neither has an animation. The identity spec's Ballista tell (a puff at the frame on each shot) is a particle at impact and needs no clip change. The Arbalist's tell, "the crossbow-charge pose while reloading", is the reload in this clip.
- **Gameplay, network, timing:** no change. Impact tick 4, cooldowns 60 and 80, kinds and weights stay. The reload runs in the cooldown, so nothing changes if the clip is shorter than 60 ticks.

## Fixed limits

- Impact tick, clip max 40 ticks and the attack row are Spec rows; any change needs Kort.
- `tools/clips.py --verify` must pass; `Spec.java` only through `tools/preflight.py`; after any verify or gradle run `git checkout -- sheets/preflight.json src/main/java/gg/gravesandglory/Spec.java`.
- `MotionEngineCheck` "hit over attack" (`test-tools/standalone/MotionEngineCheck.java`, `hitOverAttack`, ~line 1093): it drives the **real sword_shield family attack 0 (the thrust)**: attack starts at frame 20, hit at frame 25, and it checks frames 26-35 (with-hit step minus without-hit step at most 15 degrees per tick, summed over all bones; cloth springs count). It does not load the crossbow family, so as written the gate does not cover this family. For `crossbow_loose` those frames are clip ticks 6-15, the **lowering** (impact is tick 4, so a hit at frame 25 lands one tick after the kick). The plan adds a crossbow case with two hits: frame 25 (window 26-35, the lowering) and frame 47 (window 48-57, clamped to clip ticks 28-35, the lift). The lowering and lift are authored with near-even spacing and per-bone steps of at most 6 degrees per tick, and a `window_check.py` pre-check (written in the plan, Task 4) runs before the check.
- Max 2 Minecraft instances; never touch PIDs 29360, 4580, 6048 or `C:/GGMatch`; no eula.txt; no computer-use on Minecraft; no push, publish or merge.

## Questions for Kort

1. **Ballista Crew shares the clip.** (Its Spec.TROOPS shield column is `siege`, a siege tag; it holds no shield, so the shield clips do not apply.) The Ballista is a crewman with a wheeled frame (a root part), and in Elden Ring the ballista is cranked from a planted lunge. One `crossbow_loose` serves both. Options: (a) shared clip, and the frame model is built with its stock at the shouldered position (default, no gameplay change); (b) move the Ballista to the `crew` family with its own clip, which changes its attack kind (blunt, impact 8) and needs your approval; (c) Ballista keeps (a) now and gets a crank-specific clip later.
2. **Java fix: bow code runs on the crossbow.** `TroopMotion.bow(row)` is true for any ranged non-magic troop, so the bow's hand twist, draw timeline and `drawSeconds` would act on a crossbow. `bow(row)` is at `TroopMotion.java:146`. Narrowing it to `"bow".equals(row.family())` would also change the thrower (`tc_harpooner`, family `thrower`, ranged non-magic), which today gets the bow twist and draw for its trident throw. Proposed instead: `row.ranged()&&!row.magic()&&!"crossbow".equals(row.family())`, which changes only the crossbow and leaves the thrower exactly as it is. It is Java but not gameplay or save; approve?
3. **Troop models are missing.** `art/troops/ic_arbalist.json` and `ic_ballista.json` (with the `arbalest` weapon and the frame parts) do not exist, and no clip plays without them. They are an art task, not part of this plan. Should it be scheduled before or with the clips? Until then the clips are built and previewed with `--troop archer` as the stand-in (the archer model exists; the stand-in crossbow cube is the held item).
4. **Hold look.** The identity spec says "crossbow held level at the chest"; this spec shoulders it for the aim and loose (stock at the cheek) and keeps the chest-level hold for idle and carry. Keep this split?
5. **Hits.** Inherited `common` hits are authored for a bare pose; with a crossbow in both hands they may clip. The plan checks them and only adds family hits if needed.

6. **Aim and loose have no reference.** Neither video shows a shouldered crossbow or the moment of release (Elden Ring aims at chest height, forward-right). The shouldered aim and the loose are authored from the fixed tick 4 and the weapon-direction rule. Are you happy with an authored shoulder aim, or do you want me to find a shouldered-crossbow video first (Revolt Studios hunting pack, Tod's spanning methods are on your list, not downloaded)?
7. **Ballista frame stock.** The shared clip drops the right arm about 40 degrees (with the forearm flexing 45 to keep the muzzle level) and brings the stock to the belly; a frame with a fixed stock cannot follow. Blocker for the Ballista render (see the plan's Global Constraints); answer Question 1 first.

## Review

v1 6.2 (7/5/8/6/5/6)
v2 6.2 (7/5/6/6/7/6)

Fix round 1: hit windows restated (frames 26-35 = ticks 6-15 lowering, second hit at frame 47 for the lift); lowering and lift slowed to at most 6 degrees per tick per bone with a hit_keys.py pre-check; loose labelled "authored, no reference" (Question 6) and reload ranges matched to SOURCES.md; archer stand-in and mandatory anim_edit.py TROOP step; bow() at line 146 with the thrower decision; rootScale code path and render checklists, report and wait for the Ballista; Ballista frame-stock blocker; muzzle angle formula tied to the arm values per key.

Fix round 2: muzzle estimate now includes the hands and is labelled rough with pose_preview.py as the check; hand rotations and mount given for aim and loose; reload matched to the frames (near level at the belly); idle, carry and rest muzzles derived and rest hand lowered; second hit window clamped to ticks 28-35; root column labelled and the 0.10 muzzle fixed to 1.5; Ballista siege column reconciled.

(Review: pending, to be filled after Kort's pass.)

Final (after fix round 2): 6.5 (8/6/6/7/6/6). Known gaps for Kort:
- **Reload haul key (1.05):** the arm numbers put the right hand at the chin, not the belly the frame shows. Recompute the haul, lay and lift keys from hand height, with the right forearm near -90 absolute.
- **Window check:** the table gives about 16.5°/tick in ticks 6-15, over the 15 limit. Reshape the lowering or correct the Expected and the spec figure.
- **Task 1 Step 4:** `crossbow_loose` is kind `bolt`, not melee (`clips.py:36`), so no missing-clip failure happens and no stubs are needed; expect a pass. Whether `clips.py` accepts a bbmodel with zero animations is unchecked.
- **MEC case:** the plan must set `shield` false and `stance` `aim`, state what `mec.sh` runs, and wire in the frame-47 hit.
- **rootScale:** about 0.90 is an estimate. Mark it TBD until the models exist.
- **Small fixes:**
  - The reload frames live in `xb_raise`.
  - The hand-pitch sign wording is wrong.
  - Adding `crossbow` to the `anim_edit` TROOP table changes the default family list (line 137).
