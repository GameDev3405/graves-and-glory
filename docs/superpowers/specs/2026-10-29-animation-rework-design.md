# Troop animation rework: design

Date: 2026-10-29. Status: draft for Kort's review. Branch `work/anim-rework`, checkout `C:/Users/kortt/Melty/bannerbound-anim-rework`.

## 1. Why

Kort, 2026-10-29: troop animations are "really stiff and stagnate, and ugly in general". In battle:

- a swing moves only the swinging arm;
- getting hit doesn't look like getting hit;
- every unit attacks and reacts the same way at the same moment, so the army reads as clones ("i dont want a clone feeling").

He pointed at Fresh Animations (FA) as the standard to reach. The quality bar is his: nothing below a perfect 10/10. If something looks bad, rethink, replan, rework and grade again. Never label work good to get past it.

### Root causes in the current system (found 2026-10-29)

- Hand-coded clips with linear keyframes (`BipedClips`, `HoundClips`, `SpiderClips`, `ColossusClips`) make motion robotic. The clips are short and loop in lockstep, so every unit moves at the same moment.
- Motion is rotation only: no bob, weight shift or translation.
- Limbs are single segments with no elbows or knees.
- Attacks and hits are arm-only overlays that fade back to rest. The rest of the body never reacts.
- Weapon and shield carries are fixed angles. The knight's shield bone rotation is derived from a hard-coded 45° shield-arm carry.

## 2. What this replaces

- **Start fresh** (Kort): reuse nothing built for animation. That covers:
  - `TroopAnimator`, `TroopClips`, `BipedClips`, `HoundClips`, `SpiderClips`, `ColossusClips`, `TroopTells`, `HouseAttacks`
  - the ANIM-1 pass (`work/anim-pass-1`, `work/anim-rebase`)
  - `docs/ANIMATION-PLAN.md`

  The external API other code calls (§9) is replaced with new equivalents, not kept for its own sake.
- **Supersedes** the 2026-10-28 "Animation approach: hybrid" entry in `docs/DECISIONS.md`.
- **Fresh Animations itself is not used.** The 2026-10-28 note "Fresh Animations is not wanted" still holds: the pack is not shipped or required. We study its techniques only. No expressions, models or textures are copied.

## 3. Goals

1. **Alive at rest.** Every troop breathes, shifts its weight and looks around. Each unit has its own timing, so a line of 20 troops reads as 20 people.
2. **Whole-body action.** Attacks, hits and deaths involve the hips, torso, head, both arms and the legs, with anticipation, follow-through and settle.
3. **Readable at battle distance.** Silhouettes show who is attacking, blocking, hit or dying from 30+ blocks away.
4. **Weapons and shields belong to the body.** Carries, guards and the shield wall look deliberate, and nothing clips through the wearer or a neighbour.
5. **Gameplay changes approved by Kort land with the animation**, and nothing that depends on them breaks (§7).

### Non-goals

- Hit zones change only the visuals, never damage (Kort).
- No physics ragdoll. Every fall is authored by hand.
- No new mod dependencies such as GeckoLib or EMF (my call: dependencies need Kort's decision, and none is needed).

### Anti-goals: these count as failure even if they "work"

- Clone sync: visible lockstep between neighbours.
- Linear, robotic motion.
- Pops: a visible jump on any blend.
- Foot sliding while standing.
- A weapon through the body, or a shield through the forearm or a neighbour.
- Arm-only attacks.
- Deaths that pop out.
- Work labelled 10/10 that isn't.

## 4. Decisions Kort made (2026-10-29)

1. **Fresh, FA-grade, procedural-plus-authored animation system** for troops, weapons and shields, including hold and carry poses.
2. **Whole-body attacks** in three phases:
   - wind-up: the torso twists back and weight goes onto the back foot, eyes on the target;
   - strike: step in, hips and shoulders snap through, the off-arm or shield pulls in;
   - follow-through and recovery back to guard.

   Several attacks per weapon type, picked at random, never the same one twice in a row.
3. **Hit reactions** depend on where and how the blow landed:
   - head: the head snaps back or aside, a stagger step, the free hand comes up;
   - body: fold at the waist, shoulders curl, weight rocks back;
   - legs: a knee buckles into a dip, the arms swing out for balance, then recover.

   Direction and weight change the reaction. A blocked hit is different again: the shield jolts and the unit is shoved back.

   Hit zones are visual only. Melee zones come from the attacker's chosen swing (overhead → high, side cut → body, low sweep → legs). Projectile zones come from the real impact height.

   **The server sends the client a per-hit message, a protocol change Kort approved.** It carries:
   - attack kind: stab, slash, blunt, arrow/bolt, magic
   - zone
   - direction
   - weight
   - whether it was blocked
   - whether it was lethal
4. **No clones.** Each unit has its own:
   - attack timing and speed, about ±10%
   - swing size and posture
   - breathing rate
   - idle stance
5. **Elbows and knees on the biped rig.** Every biped model is split at the elbow and knee. The Freebuff session rebuilds the knight (§8).
6. **Hold, units without shields:** a low, ready stance with knees bent and the weapon raised toward the enemy. Kort's reference shows swordsmen in a tight line, crouched, blades levelled forward. Each weapon family has its own guard (§6).
7. **Hold, shield units:** they always stand shoulder to shoulder with shields up, whatever the formation. Shields overlap edge to edge and "link" into a shield wall. Kort's reference shows hoplites with spears overhand above overlapping round shields.

   **Spacing for shield troops on Hold becomes about 0.7 blocks** (today's Shieldwall is 1.5; the knight's shield is 12 px = 0.75 blocks wide). When an enemy comes into reach, that unit dips or opens its shield, leans in and strikes, then closes up again. Its neighbours stay linked.
8. **The Shieldwall formation is removed.** Its behaviour is now the default for every shield troop. The Hard bot's "Shieldwall against archers" tactic goes with it.
9. **Shields under archer fire while moving.** When moving or attacking, not holding, shield troops raise their shields under archer fire and **slow to a crawl**. A raised-shield walk is its own braced shuffle, not a slowed normal walk.
10. **Ranged troops on Hold** keep whatever formation the player chose; their spacing doesn't change. Bows are lowered and idle. Once enemies are in range, they raise their bows toward the sky and loose volleys.

    **The real arrow flight changes to match:** high lobbed volleys at long range, flatter direct shots up close. The bow's aim angle always matches the actual launch angle.
11. **Deaths:**
    - stab: drop to the knees, then faceplant;
    - slash: fall in the direction the blade travelled. Ragdoll-like, but hand-crafted "with extreme quality".

    The body lies on the ground, then slowly fades away.
12. **Make the game-code changes** these decisions need, trace everything they affect, and verify that nothing breaks.
13. **Write and revise a plan before implementing anything.**

## 5. My calls (marked for Kort's review)

- **Crawl speed:** about a third of walk speed. It is one tunable number in the design sheet.
- **What counts as archer fire:** an enemy bow, crossbow or thrower troop is in range and targeting a unit in the squad, or the squad was hit by a projectile in the last 3 s. Shields lower when that clears, or when a melee enemy is within reach (so the unit can fight).
- **Charge and Retreat never raise shields.** That matches today's rule.
- **Crossbows** use the same volley logic with a flatter arc.
- **Death variety:** 2–3 versions per death kind, varied further by zone.
  - **Arrow or bolt:** a head or chest shot knocks the unit backward; a shot elsewhere makes it stagger, then crumple.
  - **Blunt** (hammer, golem, ravager): thrown back off its feet, landing hard with a small bounce.
  - **Magic or fire:** clutch and collapse where it stands.
  - **Beasts and the golem** have their own deaths: a hound rolls onto its side, the golem topples like a statue.
- **Corpse timing:** the body lies about 3 s, then fades over about 2 s, replacing vanilla's 1 s removal. While it lies there it cannot be targeted, has no collision and gives no loot or XP twice.
- **Novelty idles** in the FA style: look around, roll a shoulder, adjust grip, shift weight, a big breath. They are suppressed in combat and while moving.
- **Secondary motion:** cape, tabard and crest lag behind the body like a spring. They are driven by body velocity and turn rate.
- **The crawl follows archer fire, not the raised shield.** The modifier is on only while the squad is under archer fire (above) and moving. A shield troop on Hold that walks or StepHops to its slot moves at normal speed, with its shield up and the braced shuffle gait.
- **Hold keeps the formation's shape.** Shield troops keep the chosen shape (line, wedge, loose, block). Only their cell shrinks to the shield-wall cell. Cells are per unit, so other units in the same group keep their own cell. A large shield troop (gl_marshal) gets its width plus a margin. "Linked" comes from the shield pose: shields angle edge-over-edge, authored per cell, rather than from the shield's flat width.
- **Arc travel time is unchanged by default.** The arc is shaped to fit the current `travelTicks`, so damage timing and archer lethality do not change. A sheet value allows longer lobs later, but it starts at "no change".
- **Hits from other sources** (player attacks, abilities, blight, slam, fire, fall) send `hit_v1` too:
  - The kind maps from the damage type: player weapon → slash or stab by item, slam → blunt, blight/fire/magic → magic, fall → blunt to the legs.
  - The direction comes from the damage source's position, or "front" when there is none.
  - The weight class is light unless the damage tops a sheet threshold.
- **Freebuff sequencing.** The engine and clips are built against a stub jointed rig: the current biped plus forearm and shin bones at the §8 pivots. The bone names, pivots, hierarchy and hand placement under the forearm are the contract, which I send Freebuff before work starts. Freebuff's real knight and sword models drop in on that contract. Any stub geometry is throwaway, but no clip is. Archer, longbow, mage, sentinel and gl_marshal get the same split, by whichever session owns those models when the engine lands. I coordinate this through Freebuff.
- **Saves.** Formation ids are not persisted. They live only in the running battle's squad state (Campaign.java:575-577; GravesAndGlory.java:804), so removing Shieldwall needs no save migration.
- **Grading.**
  - The reviewer is a fresh Sonnet subagent at high effort, per WORKFLOW.
  - It grades one weapon family per pass: idle, guard, walk, attack variants, hits and deaths together, with contact sheets and a 20-unit crowd clip.
  - Two correction rounds per family, then escalate.
- **Corpse load test.** The 100-v-100 death wave passes if server tick time stays within 10 % of the same battle with vanilla removal, and client frame time within 15 %. If it fails, the lie time drops before anything else changes.
- **Authoring tool:** Blockbench, which is installed and has an MCP bridge. Hand-keyed action curves (§6.3) are authored on the jointed rig and exported to a JSON clip format the mod loads. Blender is not required. The builder may use it for reference passes if it raises quality.

## 6. How it works

### 6.1 The motion engine (client)

There is one engine per rig. Each frame, every bone's six channels (rotation x/y/z, translation x/y/z) are the **sum of layers**, after smoothing. The layers, in order:

1. **Locomotion**, driven by gait phase from `limbAngle` and speed from `limbDistance`.
   - Gaits: walk, run, braced shield shuffle, backpedal, turn-in-place step.
   - Distorted waveforms give snappy footfalls (FA's `sin(x + sin(2x)/3)` family, triangle waves for the arms).
   - The body bobs at twice the gait frequency and leans into speed. Hips counter-twist against the shoulders.
   - Arms counter-swing a beat behind (follow-through) and roll outward to clear the body.
   - Knees bend on the passing leg and lift the foot. Elbows trail.
   - Walk and run cross-fade on speed.
2. **Stance:** the current carry or guard pose for the unit's weapon family and state (§6.2). Stances blend with cosine-eased, time-smoothed weights, never instant switches.
3. **Life:**
   - breathing, with a per-unit rate and a phase offset between chest, shoulders and arms;
   - weight shift onto one leg;
   - micro sway;
   - novelty idles on per-unit random windows.
4. **Action:** authored clips (§6.3) for attacks, shots, casts, hit reactions, deaths and scene poses. They are applied as offsets from the stance, scaled per unit, with eased in/out and no pops.
5. **Look:** head yaw and pitch toward the target or the camera, smoothed. Part of large yaws passes into body twist.
6. **Secondary:** spring-lag on capes, tabards, crests and loose straps.

**Per-unit identity.** A seed from the entity UUID sets:

- phase offsets for every cyclic layer;
- tempo, ±10%;
- amplitude scales;
- breathing rate;
- the preferred idle leg;
- novelty timing;
- the attack-variant bias.

Two neighbours never share timing.

**State smoothing.** Every state weight (moving, running, in combat, guard up, aiming, shield up) eases toward its target at its own rate and is shaped by `0.5 - 0.5*cos(πx)`. Everything runs on render time (partial ticks), so motion is smooth at any frame rate.

### 6.2 Weapon families: carries, guards and actions

Each troop maps to one family. A family defines:

- **march carry:** moving, no enemy
- **ready guard:** Hold, or an enemy near
- **wall pose:** shield families only
- **action clips:** attack variants, each with its zone and kind
- **hit and death set**

| Family | Troops (today) | Hold / guard | Attacks (zone, kind) |
|---|---|---|---|
| Sword + shield | knight, sentinel, gl_marshal (netherite sword, large) | wall: shield up and overlapping, sword cocked at the shoulder behind the shield edge | thrust over or around the shield (body, stab); overhead (head, slash); low cut (legs, slash) |
| Axe + shield | ic_hearthguard | wall: shield up, axe raised behind the shield edge | overhead chop (head, slash); hook-and-hew over the shield (body, slash) |
| One-hand sword | revenant, tc_raider | low ready guard, point toward the enemy, off-hand forward for balance | thrust (body, stab); overhead (head, slash); low cut (legs, slash) |
| Axe | ic_breaker | high two-hand guard | overhead chop (head, slash); side hew (body, slash); haft jab (body, blunt) |
| Two-hand sword | gb_hollowking | high guard, point toward the enemy | overhead cleave (head, slash); wide side cut (body, slash); thrust (body, stab) |
| Spear / trident | wm_tusk, tc_tideguard, tc_admiral | levelled spear, braced; overhand above the shield when shielded | thrust (body, stab); high thrust (head, stab); low sweep (legs, blunt) |
| Scythe / hoe | reaper | blade low and back, ready to reap | reap (legs, slash); overhead hook (head, slash) |
| Bow | archer, longbow, bonebow, stalker, wm_hunter | bow lowered, at ease | volley: draw high toward the sky, loose; direct: level draw, loose |
| Crossbow | ic_arbalist, ic_ballista | crossbow at port arms | aim (flatter arc), loose, reload cycle |
| Thrower | tc_harpooner | harpoon at shoulder | throw (arc matches flight) |
| Caster | mage, hexer, wm_seer, tc_mist | staff or focus held at the chest | cast windup → release (magic); a variant per caster |
| Crew | ic_mortar | crew stance by the piece | load, fire, recoil |
| Beasts | wm_hound, wm_stalker, wm_oldhorn, tc_brute | rig-specific alert crouch | bite or lunge, maul, charge (rig-specific) |
| Colossus | ic_colossus (iron golem) | planted, arms low | slam (blunt), backhand (blunt) |

The shield troops, meaning those with the `shield` tag in `sheets/design.json`, are knight, sentinel, gl_marshal and ic_hearthguard. The spear troops (`spear` tag) are wm_tusk, tc_tideguard and tc_admiral.

The builder fills in and grades every cell. The table's attack list is a starting point, not a cap.

**Ranged aim** follows the real launch pitch from the server (§7.4), so the bow angle matches the arrow. **Bow pull frames** follow the draw clip's progress.

**The shield wall.** The shield pose is authored so that at 0.7-block spacing the shield edge overlaps the neighbour's, the way Kort's fourth reference shows. Row ends look complete by themselves. Striking from the wall is an action clip: the shield dips or opens, the unit leans and strikes, the shield returns. The shield never passes through a neighbour's model.

### 6.3 Authored action clips

- Hand-keyed per rig in Blockbench on the jointed rig, with per-key easing (bezier or ease in/out, never linear by default).
- The builder defines the loader format and the converter. Clips are data, so a re-key needs no code change.
- Attack clips declare their impact frame, zone and kind. How the server's damage lines up with the impact frame is open question §11.2.
- Death clips end in a resting pose that lies flat on the ground without sinking. Directional deaths are authored for the blow's direction relative to the victim's facing (at least front, back, left, right), with blending between them.

### 6.4 Hit and attack events (new network messages, approved)

- **Attack start**, server → clients: troop id, attack variant id, start tick.
  - The server picks the variant, so the attacker's swing, the zone and the victim's reaction agree.
  - This replaces the client-side random House attack choice (`HouseAttacks`).
- **Hit**, server → clients: victim id, attack kind, zone, direction (yaw of the blow relative to the victim), weight (light / medium / heavy from damage and weapon class), blocked, lethal.
  - Projectile zone comes from impact height on the victim's box.
  - Melee zone comes from the attack variant.
- **Stance:** extended or replaced to carry the new states:
  - hold-ready
  - shield wall
  - shield-up walking
  - ranged lowered or aiming
  - aim pitch

### 6.5 Rendering

- **Corpse fade** needs a translucent render path for troop models during the fade.
- **Culling** (`TroopModel.cullBox`) must still contain every pose, including deaths and lunges. The current version samples clips, so the new engine must provide equivalent bounds.

## 7. Gameplay changes and everything they touch

Code survey, 2026-10-29, on `main` at bd05fa2. Paths are relative to `src/main/java/gg/gravesandglory` unless they say otherwise. Each change lists its touchpoints, and the plan must verify every one.

The design data lives in `sheets/design.json`. `tools/preflight.py` turns it into `Spec.java`, which is never hand-edited. Ignore the old `tools/implement_*` and `revise_*` patch scripts.

### 7.1 Shield troops hold the line at 0.7-block spacing

**How slots work today.** `GravesAndGlory.Battle.formation(Unit)` (GravesAndGlory.java:826-836) lays out slots on one group-wide cell:

```
cell = max(formation spacing, max over the group of BattleMath.spacing(width))
BattleMath.spacing(width) = max(1.3, width + 0.3)   // BattleMath.java:47
```

So 0.7 is below the current 1.3 floor.

**The change.** On Hold, shield-tagged units take a shield-wall cell: a new sheet value, about 0.7, on a rule row. The cell is per unit or per tag rather than group-wide.

**Rules to settle:**

- Shield and non-shield units in the same group lay out without overlapping.
- gl_marshal is `large`, so the cell must be at least its width plus a margin. The shield overlap is then authored for that cell.
- Mob collision boxes must not push neighbours apart. A husk's width is 0.6, so a 0.7 cell leaves a 0.1 gap.

**Mirrors and users to update:**

- client formation preview: `CommandMode.cell` (CommandMode.java:83-113)
- slot centre, goal and bridge offset: Tactics.java:48, 150, 173
- `StepHop` probes
- test expectations:
  - BattleMathCheck.java:31
  - Phase2Checks p2-ironclad-spacing (2577-2593)
  - the many `b.formation(u)` placement helpers in Phase1Checks and Phase2Checks
  - VisualChecks.java:59

**Shields on Hold.** On Hold, shield troops always raise their shields: `BattleMath.raiseShield` (BattleMath.java:127-134), used at Tactics.java:122. Today shields only go up in Shieldwall, or in Block on the approach. The new rule:

- Shields go up on Hold, and under archer fire (§7.3).
- They lower briefly to strike when an enemy is in reach.
- Charge and Retreat never raise them.

Blocking is not vanilla blocking. `Combat.java:70` counts a shield as raised when the unit is shield-tagged and `isUsingItem()` with a shield. `COUNTERS` then scales frontal damage: C1 arrows ×0.35, C3 melee ×0.6, C2 lowered ×0.75, C1g Gilded ×0.30. These rows are unchanged, but how often a shield is raised changes, so this is a balance shift.

### 7.2 Remove the Shieldwall formation

| Where | What changes |
|---|---|
| `sheets/design.json` | Remove: the formation row (line ~950), `shieldwall` from the UI formation list (~1834), the bots' `shieldwallVsArchers` flags (~2002/2018/2034). Rewrite: the tutorial lesson text (~3006) and the map beat text (~8067, ~8127). Then regenerate `Spec.java`. |
| Tactics.java:51 | Stance: "brace" now applies to shield-tagged or spear-tagged units on Hold. Spear units without shields keep the brace. |
| BattleMath.java | `raiseShield`: new rule (§7.1). `shieldwallVsArchers` (BattleMath.java:124): removed. |
| Campaign.java:542-596 | The Hard bot's Shieldwall-against-archers tactic is removed. Bots get the behaviour for free (§7.3). |
| GravesAndGloryClient.java | The formation cycle (138), command-screen cards and the hard-coded column counts (353-360), and the lesson text (257). |
| BattleHud.java | The label (290-291, 397) and the glyph cases (383, 390). |
| Glyph art | `textures/gui/atlas.json` entries (26-27), `tools/ui_art/manifest.py:50`, `tools/ui_art/v4a_hud.py:481`: remove the glyphs. |
| Saves and state | Saved or persisted formation ids such as squad assignments must map `shieldwall` to `block` on load. The builder confirms whether any are persisted. |
| Tests | Rewrite: BattleMathCheck (57, 62-64), LevelMathCheck:21, UiMathCheck (176-177), Phase1Checks p1-bots-shieldwall (929-974) and line 1523, Phase2Checks p2-gilded-showcase (1591-1616), lines 2578 and 3888, and the stance step "hold in shieldwall" (5316). |
| Docs | Note the change in DECISIONS.md and the current docs (PHASE-2-PLAYTEST, VISION-AND-ROADMAP, the SHOTLIST). Archived docs are left as they are. |

### 7.3 Shields up under archer fire, at a crawl

- **Trigger** (§5). The server tracks per squad when it was last shot at or targeted by a ranged enemy in range.
- **Raising:** `setCurrentHand(OFF_HAND)` and `stopUsingItem()` at Tactics.java:122-123, as today.
- **Slowdown:** while the shield is up and the unit is moving, add a movement-speed attribute modifier with a new sheet value, crawl ≈ 0.33. Add and remove it alongside raising and lowering, the way HOWL does (Abilities.java:689-709). Don't multiply the walk calls (Tactics.java:145, 151), because the modifier survives every path.
- **Must not stack wrongly** with HOWL, RALLY (Commander.java:106) or order `moveMultiplier`s.
- **Charge and Retreat** never raise shields.
- **Client:** sees the shield up through `isUsingItem()` and the active hand, and moving speed through the limb values. That drives the braced shuffle gait.

### 7.4 Volley arcs for bows and crossbows

**How shots work today.**

- Fire is custom. `Abilities.attack` → `Projectiles.fire` (Abilities.java:267-271; Projectiles.java:30-47).
- Arrows and tridents have no gravity and no clip.
- Each one is steered every tick to land exactly on `s.lands` (Projectiles.tick, 54-70).
- **The hit is decided at fire time**, and damage applies on landing from the queue.
- Travel time is `BattleMath.travelTicks(dist, speed, minTicks, maxTicks)` (BattleMath.java:7), using `Spec.COMBATDISCIPLINE[0]`.
- Bolts lerp from start to target (Projectiles.java:66-70).

**The change.**

- Add a scripted vertical arc `h(t)` to both the launch velocity (line 41) and the per-tick aim point (62-65). The peak height grows with distance: high lobs at long range, near-flat close in.
- Crossbows use a lower peak. Harpoon throws get a modest arc.
- If the visible path should match a longer lob, travel time may grow with the arc's path length. That is a balance change, set by sheet values the builder exposes.
- **Hit odds stay as the existing fire-time rules decide**, so accuracy against moving targets does not change unless Kort asks.
- **Aim sync:** the server sends the launch pitch (§6.4), so the client's bow angle equals the arrow's initial direction.
- **Range:** `range` values don't change. Mortar and mist bolts already arc or have their own path (Abilities.java:259-264) and are untouched unless they look wrong.

### 7.5 Death timing: the corpse lies, then fades

**Today:**

- Vanilla removes a dead mob after 20 ticks of `deathTime`. There is no custom death code on the server beyond the events at GravesAndGlory.java:122-134: Rise Again via `Houses.allowDeath`, kill credit, `Veterans.onDeath`.
- The client death clip ends by 0.9 s (p2-anim-death).

**The change:** extend dead troops to about 100 ticks (3 s lying + 2 s fade) through a troop-only mixin or override of the post-death removal.

**Verify:**

- No second death event, loot or XP.
- The corpse is not targetable, collides with nothing, and is not counted alive by any battle tally (squad counts, objectives, end-of-round checks, Rise Again).
- Round reset, map switch and unload still remove corpses at once.
- Corpses must not inflate the entity count enough to hurt performance in big battles. The builder measures this with a 100-v-100 death wave.

### 7.6 Hit and attack events

**Today:**

- Melee is `Combat.resolve(..., "melee", false)` → `target.damage(...)` (Combat.java:91), right after `mob.swingHand` (Tactics.java:133-135). **Damage lands on the same tick the swing starts.**
- The attack kind is only melee, arrow, bolt, blight or slam (CounterMath.kindTags, CounterMath.java:161).
- The blow's direction already exists as `bearing` (Combat.java:69) but is never sent.
- The client sees swings only through `handSwinging` and hits only through `hurtTime`.
- `HouseAttacks` picks a variant per House on the client.

**New** (approved protocol change), modelled on `stance_v1`:

- **Registration:** id in GravesAndGlory.java:50; send at Tactics.java:57-62 plus START_TRACKING (GravesAndGlory.java:86-98); receive at GravesAndGloryClient.java:86-88.
- **attack_v1:** troop id, variant id, start tick. The server picks the variant from the troop's family, with per-unit bias and no immediate repeat.
- **hit_v1:** victim id, kind (stab, slash, blunt, arrow, bolt, magic), zone, direction from `bearing`, weight class, blocked (from the counters' raised-shield result), lethal.
  - Projectile zone comes from where `s.lands` sits on the victim's box.
  - Melee zone comes from the variant.
- **stance:** gains hold-ready, shield wall, shield-up moving, ranged lowered or aiming, and aim pitch.

**Timing (open question §11.2).** A whole-body swing has a 0.25–0.45 s wind-up before the visible impact, but damage lands when the swing starts. Either damage is delayed to the clip's impact frame (a gameplay timing change), or the wind-up plays first and the server's attack is scheduled to land on the impact frame.

## 8. Rig changes (elbows and knees)

Proposed and agreed in principle with the Freebuff session (2026-10-29). Final numbers go in the spec sent to it.

- **New bones:**
  - `right_forearm` / `left_forearm`: children of the arms, pivot at the elbow, arm-frame y 4.
  - `right_shin` / `left_shin`: children of the legs, pivot at the knee, leg-frame y 6.
- **Hands:** `right_hand` / `left_hand` move under the forearms, keeping their offsets relative to them. Weapons and the shield follow the forearm.
- **Seams:** a lower-part cop or cuff about 0.5 px proud covers each seam when bent. The knight's couter moves onto the forearm and grows about 0.5 px up. The knee cop already sits on the shin.
- **Shield:** the knight's shield bone is re-derived against the new guard and wall poses. Its rotation no longer assumes the 45° carry. I send Freebuff the exact carry angles.
- **Folded arms:** `TroopModel`'s arms-folded swap must include the forearms.
- **Other modelled troops:** the other modelled biped troops (archer, longbow, mage, sentinel, gl_marshal) also need the split. Freebuff owns the builders, so this is coordinated with it.

## 9. Code the new engine replaces or must serve

- **TroopModel.setAngles → `TroopAnimator.pose(...)`:** the per-frame entry point.
- **TroopFeatures:** bow pull frame from `TroopAnimator.drawSeconds`, which becomes draw-clip progress. Weapon and shield placement on the hand bones.
- **TroopModel.cullBox / cullReach:** samples every clip for bounds.
- **TroopRenderer.prepare:** folded arms (`TroopAnimator.folds`) and helm-off (`helmOff`).
- **GravesAndGloryClient:** registers the stance receiver into `TroopAnimator.STANCES`, and `forget` on entity unload.
- **Harness hooks:** `TroopAnimator.forced`, `reference`, `simulate`, `setScenePose`, `View`. They are used by `test-tools/java/gg/gravesandglory/Phase2Checks.java`, and the checks are rewritten for the new engine (§10).
- **Tactics.java:** stance selection (hold / brace / fall_back / run). `StepHop` stays, since it is movement, not animation.
- **Troops without a model** (24 of 30 today) draw through vanilla renderers with vanilla animation. See §11, question 1.

## 10. Quality process: how 10/10 is judged

- **Reference board:** Kort's four hold references, FA captures of a vindicator and a skeleton, and real HEMA and shield-wall footage for timing.
- **Evidence for every clip and state:**
  - in-game contact sheets at fixed frames;
  - a turnaround;
  - a 20-unit crowd shot at battle distance (the clone check);
  - a short in-game capture.

  All of it is rendered through the harness in isolated run roots, never Kort's saves, and never while Kort's game is running.
- **Rubric.** Each item is scored 1–10 and the clip passes only at 10:
  - weight and timing
  - arcs
  - anticipation and follow-through
  - silhouette at distance
  - variation (no clones)
  - contact (no foot slide, no ground sinking)
  - no clipping (body, weapon, shield, neighbour)
  - blend continuity (no pops)
- **Grading:** a fresh reviewer grades against the rubric, and Kort has the final say.
- **Correction rounds:** WORKFLOW.md allows two automatic correction rounds. After that the work goes to Kort with the honest grade, never labelled 10 when it isn't. That honours both WORKFLOW and Kort's "rethink, replan, rework" instruction.
- **Tests:** real game tests for every gameplay change:
  - spacing
  - shield-up crawl speed
  - arc and range parity
  - death timing and no double loot
  - packet sync
  - bots no longer using Shieldwall

  The animation harness checks are rewritten for the new engine. `tools/verify.py fast` and `game` must pass.

## 11. Open questions for Kort

1. **Troops without models.** 24 of the 30 troops still draw as vanilla Minecraft mobs, with vanilla animation. The new system can only drive troops with a mod model. Options:
   - (a) they stay vanilla until their models arrive;
   - (b) give every humanoid troop a temporary jointed placeholder model wearing its vanilla skin, so they all get the new animations now.

   Beasts and the golem have rigs (hound, spider, colossus), but polar bear and ravager have none.
2. **Melee damage timing.** Today damage lands the instant a swing starts. A whole-body swing has a visible wind-up of about 0.25–0.45 s, so to make the hit land on the impact frame:
   - (a) damage is delayed to the impact frame. A target that steps out of reach, or an attacker that dies, makes the blow miss. This is a gameplay timing change.
   - (b) the swing is started early while the cooldown finishes, so damage keeps its current timing.

   (b) preserves balance but can show swings at targets that walk away.

## 12. Animation tests today

These are all in-game checks in `test-tools/java/gg/gravesandglory/Phase2Checks.java`. They are not run by `tools/verify.py fast`. The builder confirms whether `verify.py game` (`test-tools/isolated-checks.gradle`) includes them.

- The p2-anim-* checks (4946-5419): timeout, cover, change, return, cull, choose, loose, blend, keys, bow, carries, stance, scene, death, fold.
- The turn suite: p2-turn-shots and `[p2-turn-fact]` (5444-5812).

They test the old engine's internals, so they are replaced, not patched. Keep the turn suite's shot conventions where they still fit.

## 13. Left to the builder

- The exact engine structure and class split, the clip data format and converter, and the spring and easing constants.
- Specific clip choreography inside the family rules above, judged by the rubric.
- Test names and the harness scenes.
- Any extra variation that raises quality without changing gameplay.

## 14. Constraints

- Follow docs/WORKFLOW.md:
  - one coordinator plus at most two helpers
  - one writer per file
  - no push, publish or merge
  - never stop Kort's game
  - at most four harness clients
  - no compiling in a checkout with a running client
- Don't hand-edit `Spec.java`. Change `sheets/design.json` and regenerate with `tools/preflight.py`.
- The Freebuff session owns `TroopModel`, `TroopRenderer`, `TroopFeatures`, `TroopModelLoader`, `TroopModels` and the knight and sword builders until its work lands. Coordinate before touching them.
- Freebuff's knight and sword work is uncommitted on `art/knight-1x` in the main checkout. This branch starts from `main` and is rebased onto that work once it lands.
- No Fable models.
