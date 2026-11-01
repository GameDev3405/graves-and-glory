# Convoy mode, the supply-wagon escort: design

Status: approved by Kort 2026-10-31 (answers in "Kort's decisions" at the end). Spec only; no code. Plan item 4C.2 ("escort decision if taken"). Protocol, save and custom-entity changes still need Kort's approval case by case; section 9 flags where this design could hit one.

Sources: `docs/VISION-AND-ROADMAP.md` section 6 (Banner: "carry an enemy banner home, or escort a supply wagon along a road while the enemy harasses"); the approved Phase 4 spec `2026-10-18-phase-4-modes-campaign-design.md` sections 4, 5, 7, 10 to 13; `docs/PARKED.md` (escort parked 2026-10-18, "revisit after the carry variant ships"); `docs/PLAN-TO-1.0.md` 4C.2, 4D.1 and the 1.0 campaign table (battle 4, The Frozen Standard, Frostmarch, Banner, Wildmoot); the Map Forge README (`gg-mapforge-map/1`, protect locks) on `work/mapforge-2a3`; the mode frame (`BattleMode`, `SiegeMode`) on `mode/frame`.

Kort's intent, 2026-10-31: battle 4 should be "more like push the cart", a convoy you protect and push. The escort is unparked for 4C.2. Kort 2026-10-31, on this spec: the convoy **replaces** carry-the-banner; carry does not ship. Section 14 lists everything that depends on carry and what each becomes.

Every number invented here is marked "(draft value)". Numbers taken from a sheet or an approved spec are not.

## 1. Intent

The convoy is the one mode where the objective moves. Domination and Siege ask "who holds this ground"; the convoy asks "can you keep a slow thing moving through hostile ground". It gives the Wildmoot, the ambush House, a mode where ambush is the whole defence, and it gives Frostmarch, Kort's "ambush pass", a reason to exist beyond Domination.

Goals:
1. One asymmetric round: the escort side pushes a supply wagon along a fixed road through checkpoints to a destination; the defending side stops it.
2. The wagon moves only while friendly troops or commanders stand by it, stalls when an enemy stands by it, and creeps back when abandoned.
3. Bots escort and attack at all three tiers using the existing `Tactics.walk` and the planned `raidShare` and `chaseRange` columns.
4. Battle 4, The Frozen Standard, becomes a convoy battle on Frostmarch.

Non-goals: wagon combat (it carries no weapon), several wagons, branching roads, a convoy on maps other than Frostmarch in 1.0 (a later map is a `modes.maps` entry and its road rows), side-swap rematches.

Anti-goals: anything worn on a troop's back (no pennants or standards on troops, Kort 2026-10-29); a wagon that a single archer can stop from range; a round that ends in a stalemate at a checkpoint with nobody moving.

## 2. What exists and what the convoy reuses

- **Mode frame** (`BattleMode` on `mode/frame`, not on main yet): per-round object with `setup`, `mark`, `teardown`, `tick`, `fieldBase`, `income`, `timeoutWinner`, `clock`, `ledger`, `max`, `formats`, `howToSide`, `snapshot`. Siege is the closest precedent: asymmetric (`howToSide` attack/defend by team), stage bonuses to the clock, no draws, crash marker. The convoy is a new `BattleMode` subclass, `ConvoyMode`, and needs no new hook.
- **Transport**: STATE (`state_v9`) already writes `campaign.snapshot(p)` every 10 ticks, and that JSON holds the `battleMode` object. Every convoy field rides there (section 9).
- **Troop movement**: troops are `Army*` subclasses of vanilla mobs moved by `Tactics.walk` (navigation, wedge detours, step hop) and `Tactics.route` (nearest bridge across the river or gorge, House gate yards). A moving goal works: `walk` repaths when the goal moves more than 3 blocks or every `repathTicks`. Escorts and attackers therefore need only a goal per group, not new movement code.
- **Wagon movement**: the wagon is not a troop and does not path. It is kinematic: it slides along the road polyline, so its progress is exact, rollback is trivial and it never gets stuck on terrain. Pathfinding a vehicle through `MobNavigation` would bring back every wedge problem the troops had.
- **Structure precedent**: `ArmySupplyBanner` is an invisible armour stand with hp, never saved, carrying the "structure" tag. The wagon follows the same "invisible vanilla entity, never saved, removed by teardown" pattern but carries no hp (section 4).
- **Banner carry** (Phase 4 section 7, now retired): the convoy keeps only the bot columns `raidShare` and `chaseRange` (planned, not yet in the sheet) and `ModeMath.roles` on `mode/frame`; stands, carriers, `exempt` and the carry tests go (section 14).

## 3. Rules

### 3.1 Sides and win/lose

- One round. The escort side (attack) pushes; the other side (defend) stops it. Campaign battle 4: the player escorts. Skirmish: the player picks a side, as in Siege. Lobby: the host picks which team escorts (default Dawn, matching Siege, where Dawn attacks).
- **Escort wins** when the wagon reaches the destination node.
- **Defence wins** when the clock reaches 0 with the wagon short of the destination. No draws.
- Overtime: if the wagon is moving or contested within 6 blocks of a checkpoint or the destination when the clock hits 0, play continues until it is neither for 3 s (draft values), so a push cut short by one tick does not end the round.

### 3.2 Clock and checkpoints

- Start clock 420 s (draft value). Each checkpoint reached adds 180 s (draft value), as Siege's `siegeStageBonus`.
- Two checkpoints and a destination on Frostmarch (draft value). A checkpoint once passed is locked: the wagon never rolls back past it.
- Reaching a checkpoint: feed line, +50 gold to each escort squad (draft value), and the escort's forward anchor moves there.

### 3.3 Pushing, contest, stall, rollback

- **Pusher**: a living escort-side troop or an on-foot commander within 6 blocks (draft value) of the wagon's centre, horizontally, and within 3 blocks vertically. Mounted and dead do not count.
- **Contest**: any defending troop or commander within the same 6 blocks. This is the "uncontested" rule Domination (no enemy within 20) and Siege use, so all three modes read the same way.
- States, evaluated each tick:

| State | When | Wagon does |
|---|---|---|
| Moving | at least one pusher, no defender | moves forward at the speed below |
| Contested | at least one defender in range, whatever the pushers | stops |
| Idle | no pusher, no defender, for under 10 s (draft value) | stops |
| Rolling back | no pusher, no defender, for 10 s or more | moves back at 0.6 blocks/s (draft value), never past the last checkpoint |

- **Speed**: 1.2 blocks/s with one pusher, 1.5 with two, 1.8 with three or more (draft values). A walking troop is faster, so escorts keep up without sprinting. Players and troops count the same; a commander does not push harder.
- **No hp**: the wagon cannot be damaged, so the defence stops it only by standing near it. Arrows and blows pass through or are cancelled. This keeps one way to stop it and keeps archers from stalling it from 30 blocks (an anti-goal).
- **Grade**: on road segments steeper than 1 in 4 (draft value) speed is x0.75 uphill and x1.25 downhill (draft values). Preflight caps road grade (section 5) so this stays a flavour.

### 3.4 Field bases, anchors and income

- Escort: lineup and respawn at its base; bought troops deploy at the last checkpoint reached (or at the start node before the first), when no enemy is within 20 blocks of it, using the existing `Anchor` rule.
- Defence: base, plus the next checkpoint ahead of the wagon as a forward anchor under the same no-enemy rule, so defenders reinforce where they fight.
- Domination points are off (flags down in `setup`, back in `teardown`, as Siege does). `modes` row: `passiveScale` 1, `pointScale` 0 (draft values); checkpoint gold is the escort's objective income.
- Morale: none (no-routing design 9.2).

## 4. The wagon entity

### 4.1 Base: vanilla entity, custom drawing

Recommended: `ArmyWagon`, a server-side subclass of a vanilla entity type, invisible, never saved (`shouldSave` false), no AI, no gravity, positioned each tick by `ConvoyMode.tick`. The client draws the wagon model at that entity's interpolated position; `battleMode.convoy.entity` in the JSON tells the client which entity id is the wagon.

- Why vanilla-based: a new `EntityType` adds a registry entry, a spawn packet and a client registration; with the vanilla base nothing in the registry or protocol changes, and the troop pattern ("subclass a vanilla mob, draw our own model") is already how this mod works.
- Base type: armour stand (precedent, `ArmySupplyBanner`) or boat (client-side collision, smoother interpolation). Builder spike in E2: pick the one whose client motion is smooth at 1.8 blocks/s and which troops path around. Neither changes protocol.
- Solid or not: troops and players should not walk through it. A boat base is solid on the client for free; an armour stand base is not. If neither base is both smooth and solid, the wagon is non-solid in 1.0 and the model sits low enough that clipping reads as "standing at the tailboard" (assumption, logged).
- Hitbox about 3 x 2 x 5 blocks (draft value) for pusher range only; push range is measured from the centre.

### 4.2 Model and look

- A heavy four-wheeled supply wagon with a canvas tilt over hooped ribs, drawn by two oxen in the same model (draft value). Oxen walk and wheels turn only while Moving or Rolling back (rolling back: wheels backwards, oxen led backwards); Idle and Contested hold the last pose.
- House style: Souls-like, grim and weathered, flat faces on boxes, 1 texel per model pixel (art rules 2026-10-30), weathered wood with one-texel grain along the long axis, iron tyres and straps with the house edge-texel rule, frost on the canvas for Frostmarch as a texture variant, not a second model.
- Side colour: the canvas carries a painted side mark (the escort team's sun, moon, star or wave from `DarkUi.sideMark`) on each flank. The Frozen Standard battle adds the march's standard on a pole at the wagon's front: a standard on the wagon, not on any troop's back, so the 2026-10-29 rule holds.
- Built in the house pipeline (bbmodel, `TroopModelLoader`), contact sheet at the art acceptance threshold for player-facing work (9).

## 5. The road: path, checkpoints and how authors mark it

### 5.1 Runtime data (the source of truth)

The road is `modeSites` rows, the Phase 4 sheet, no new sheet:

| Column | Road use |
|---|---|
| kind | `road` for each node |
| mode | `convoy` |
| map | `frostmarch` |
| stage | node order, 0 = start |
| name | blank for a plain node; the checkpoint's name for a checkpoint; `destination` copy for the last node |
| x, y, z | node position at road surface |
| radius | corridor half-width, 3.5 (draft value) |

The wagon moves along straight segments between consecutive nodes, with y interpolated. A node with a name is a checkpoint; the last node is the destination.

Preflight (4D.5 style, a bad row fails the build): nodes of one road have consecutive `stage` from 0; at least 2 named checkpoints before the last node (draft value); consecutive nodes at most 24 blocks apart (draft value); grade at most 1 in 3 (draft value); every node inside the map bounds; no node inside a `structures` footprint; the escort and defence bases are more than 40 blocks from the start and destination respectively (draft values).

### 5.2 Frostmarch road, draft on today's layout

Frostmarch's Map Forge master layout is not drafted yet, so these are placeholders for the layout draft to replace; Kort signs the layout that fixes them.

- Start: the caravan lodge yard on the Dawn side, near (-230, 0) (draft value).
- Checkpoint 1, "Pass Crossing": the far end of the gorge bridge at B, near (riverX(0)+32, 0) (draft value). The bridge is the first choke and the place every escort meets `Tactics.route`.
- Checkpoint 2, "Signal Ridge": the foot of the signal tower, near (150, 60) (draft value), under the tower's sightline.
- Destination, "The Refuge": the cave refuge yard short of the Dusk gate, near (280, 0) (draft value).
- About 560 blocks of road (draft value). At 1.8 blocks/s an unopposed push takes about 5 minutes; with contests a round runs 10 to 13 minutes (draft value), against a maximum clock of 420 + 2 x 180 = 780 s with these draft values.

### 5.3 Map Forge marking

- Map authors place the road nodes as `modeSites` rows and build the road surface with the Map Forge `path` operation using the same node list (`points` [[x,z,y]...], `width` 7, draft value).
- The map's `gg-mapforge-map/1` manifest gets one `protect` lock per node and per 4 blocks of segment (draft value), each an upright cylinder `{"kind": "road", "center": [x,z], "radius": 4, "y": [roadY+1, roadY+4], "source": "sheets/design.json modeSites convoy/frostmarch"}`, generated by a `make_manifest.py` step as Ironhold's capture locks are. A later plan that would put a block in the wagon's way is refused.
- Tool change: `road` joins `PROTECT_KINDS` in `tools/mapforge/manifest.py` (one constant and a test). The manifest format stays `/1`: no new top-level key, only a new value of an existing field. If the builder prefers not to touch the validator, `kind: "other"` with reason "convoy road" works today.
- A Map Forge check (`verify` or a new `road-check`) walks the corridor in the exported world and fails on any non-air block above the surface in the lock, any gap deeper than 1 block, or any step taller than 1 block.

## 6. Bots

Each decision (the existing decision cadence) every bot banner holds one role. Tier logic for retreat, rally, charge and shieldwall stays; roles are orders.

Escort side:
- **Pusher**: infantry groups. Goal: the wagon's position plus a slot offset 2 to 4 blocks ahead and beside it along the road (draft values), so they stand in push range and walk with it. Repath already follows a moving goal.
- **Screen**: the first ranged group, and on Hard also one infantry group. Goal: 20 to 30 blocks ahead on the road (draft values), clearing defenders before the wagon arrives. Never more than 40 blocks from the wagon (draft value).
- Share: `1 - raidShare` of the escort's bot banners push, the rest screen, at least one pusher (reuses `raidShare`: easy 0.34, normal 0.5, hard 0.5).
- **Regroup**: any escort group farther than `chaseRange` (easy 40, normal 80, hard 120) from the wagon walks back to it.
- Humans count as pushers first, as `ModeMath.roles` counts humans first today.

Defence side:
- **Raider**: goes to the wagon and stands in contest range. Share `raidShare` of the defence's bot banners, at least one.
- **Blocker**: holds a stop line on the road 25 to 40 blocks ahead of the wagon (draft values), choosing the next choke (a bridge, a gate or the next checkpoint) when one is within that span.
- **Hard**: raiders approach from the side of the road with fewer escorts (the vision's "flank the least-defended point"); Wildmoot ambush units hold behind the road's cover until the wagon is within 15 blocks (draft value).
- Any defence group within `chaseRange` of a Moving wagon switches to raider for that decision.

Sim gate (bot-vs-bot, 10 matches Normal v Normal, draft values): the escort wins 3 to 7 of 10; no wagon spends more than 60 s Idle or Rolling back while two or more escort groups are alive; mean checkpoints reached at least 1.5; the escort seat's share is logged, as for Siege and carry.

## 7. Scaling: 3v3 and solo campaign

- **3v3 lobby**: up to three commanders per side, bot fill as the lobby does today (War Table lobby, host picks map and mode, 10 s countdown, bot auto-fill). Push speed caps at three pushers, so a full side does not push faster than a small one; the clock does not scale.
- **1v1 and solo skirmish**: the same rules; with one army per side the player is the only pusher besides their troops, which is enough (any troop pushes).
- **Campaign, battle 4 "The Frozen Standard"** (replaces "Raiders on the River" on Rivergate):
  - Map Frostmarch, mode convoy, difficulty Normal, House met Wildmoot (PLAN-TO-1.0 table).
  - Sides (draft values): you and `ironclad_normal` escort; `wildmoot_easy` and `wildmoot_normal` defend. The ally follows the escort bot roles, so with the default share it screens and you push.
  - Reason (draft copy): "The march's standard must cross Frostmarch before the snow closes the pass."
  - Briefing (draft copy): "The Wildmoot watch the pass from the ridges. Push the standard's wagon from the lodge to the refuge, and do not let it stand still."
  - The chapter-1 copy for battle 4 (`docs/copy/chapter-1.md`, `c1_b4_*` scenes and Dunmar's `b4_bonus_kept` line) is written for the carry battle and goes through Kort's copy pass. Battle 4's setup is approved by Kort 2026-10-31.

## 8. HUD and objective UI (War Table style)

All drawing with the existing 4.4 kit (`DarkUi`, brass, iron, wax seals); no new ink drawing.

- **Ledger**: Dawn's and Dusk's tracks keep their place. The escort track shows road progress, `percent` format, max 100; the defence track shows checkpoints still held, `plain` format, max checkpoints + 1. The clock reads as in Siege, with the "+3:00" flash on a checkpoint.
- **Road strip** (new element, under the ledger): a thin brass bar for the whole road, a notch per checkpoint (wax seal of the escort when passed, iron when ahead), and a wagon marker. Marker state: brass and moving tick marks (Moving), the contested seal (Contested), dull iron (Idle), dull iron with a back arrow (Rolling back). Fits in the HUD's small-window size (HudSize) without covering the feed.
- **In world**: a faint ring at push radius drawn on the ground around the wagon for both sides, side-tinted while Moving, contested tint while Contested (draft; dropped if it fails the visual gate).
- **M map**: the road as a line, checkpoints as seals, the wagon as the escort team's emblem.
- **Feed lines** (draft copy): "The wagon rolls" (first move only), "The wagon is stopped" (Contested after 5 s of Moving, rate-limited to once per 20 s, draft values), "The wagon rolls back", "Dawn reaches Pass Crossing", "The convoy is through", "The convoy turns back".
- **Briefing card**: `modes.howTo` "Stay by the wagon to push it. | An enemy beside it stops it. | Reach the refuge before the clock runs out." and `howToDefend` "Stand by the wagon to stop it. | Left alone, it rolls back to its last checkpoint. | Hold it until the clock runs out." (draft copy). Rule line "Convoy: reach {n}", filled "Convoy: reach The Refuge" on Frostmarch (draft copy).
- Visual gate: screenshots of the strip in all four states, the ledger, the map and the wagon in Frostmarch at normal and small window size, against `docs/visual-references`, threshold 9.

## 9. Network and save impact

Approval gates are flagged **GATE**.

- **Protocol: no change intended.** Everything rides in the existing `battleMode` JSON inside STATE's snapshot string: `convoy: {entity, progress (0..1), state, checkpoint (last passed index), next (name), escortTeam, nodes: [[x,z],...] sent once per round at setup}`. Adding keys to a JSON string is not a binary layout change, so no channel bump. **GATE if** the builder needs a binary field in STATE (for example for smoother marker motion): that is `state_v10` and needs Kort's approval first.
- **Entity**: vanilla entity type, so no registry or spawn-packet change. **GATE if** the E2 spike shows a custom `EntityType` is needed: that adds a registry entry both sides must agree on and is a protocol change.
- **World**: the wagon is never saved. The existing `Modes.mark` crash marker records "flags down, wagon placed"; the next start removes any wagon left in a loaded chunk and raises the flags. No new world data.
- **Profile save: no schema change.** Battle 4 keeps its `campaignBattles` id, so `battlesWon` and the chapter rule are unchanged. Its bonus gets a new `bonusObjectives` id (section 11), a new string in `bonusClaimed`; no migration because no released profile holds the old `carry_self` id. Campaign rules: battle 4's map, mode, House and bonus are approved by Kort 2026-10-31 (decisions 6 and 7). If chapter 1 has shipped to any tester profile with the old battle 4 bonus claimed, the builder must stop and ask.
- Data only: new `modes` row `convoy`, `modeSites` road rows, `modeRules` new columns `pushRadius`, `pushSpeeds`, `idleSeconds`, `rollbackSpeed`, `checkpointBonus`, `checkpointGold`, new bonus kinds. Preflight regenerates `Spec.java`; never hand-edited.

### Gates this design hits

- None as drafted: no binary STATE field, no new entity type, no profile schema change.
- Possible, each needing Kort's approval before the builder goes on: (a) `state_v10` if the HUD needs a binary field; (b) a custom `EntityType` if the E2 spike finds no vanilla base that is smooth and usable; (c) a save change if any tester profile already holds a battle 4 bonus id in `bonusClaimed` or relies on `carry_self`.

## 10. Tests

Suite `convoy`, ids `p4-convoy-*`; 4C.2's evidence column changes from `p4-banner-*` to `p4-convoy-*` (section 14). Practice convoy rounds on Frostmarch.

- `p4-convoy-setup`: the wagon stands at node 0 facing node 1; the flags of A, B, C are down; STATE's `battleMode.convoy` reads progress 0, state idle, the node list; clock 420; `howToSide` attack for the escort and defend for the defence.
- `p4-convoy-push`: one escort troop placed within 6 blocks: state moving, the wagon's distance along the road rises 1.2 blocks/s within 5% over 200 ticks; two and three pushers give 1.5 and 1.8; a fourth gives 1.8.
- `p4-convoy-commander`: an on-foot commander alone pushes; mounted, he does not.
- `p4-convoy-contest`: one defender within 6 blocks of a moving wagon with three pushers: state contested, no motion over 100 ticks; the defender at 7 blocks: moving again. Screenshot `p4-convoy-contested`.
- `p4-convoy-rollback`: all pushers removed: idle for 10 s, then rolling back at 0.6 blocks/s, stopping at the last checkpoint and never behind it.
- `p4-convoy-checkpoint`: reaching checkpoint 1 adds 180 s, pays each escort squad 50 gold, moves the escort anchor, feeds "Dawn reaches Pass Crossing"; the wagon cannot go back past it.
- `p4-convoy-nohp`: arrows, blows and a ballista bolt against the wagon change nothing and the wagon survives an explosion.
- `p4-convoy-end`: destination reached wins for the escort; clock 0 short of it wins for the defence; clock 0 with the wagon moving 4 blocks from checkpoint 2 goes to overtime and resolves; no draw at any state.
- `p4-convoy-bots`: a 2 v 2 Normal bot round: roles are pusher and screen for the escort, raider and blocker for the defence; within 2400 ticks the wagon moves 40 blocks or more; a defence group within 80 of the moving wagon targets within 6 of it; logs `[p4] convoy-bots moved=<blocks> checkpoints=<n>`.
- `p4-convoy-hud`: the strip, the ledger formats (`percent`, `plain`), max `[100, 3]` and the map emblem; screenshots `p4-convoy-strip-{moving,contested,idle,rollback}` and `p4-convoy-map`.
- `p4-convoy-teardown`: `returnLobby` mid-push and `switchMap` while contested: no wagon entity remains, flags stand, no protect-corridor block changed, and a Domination round on Frostmarch captures B in its normal seconds.
- `p4-convoy-crash`: a simulated stop with the wagon placed: the next start removes it and raises the flags.
- `p4-convoy-preflight`: each road rule in 5.1 broken once in a fixture fails preflight with the row named.
- `p4-march-4-start` (march suite, changed): Frostmarch, convoy, Normal, the two Wildmoot rows, the ally row, the player on the escort side; the battle 4 bonus driven once to kept and once to lost.
- Map Forge unit tests: `road` accepted as a protect kind; the road check fails a fixture with a block in the corridor.
- The carry suite (`p4-banner-setup` to `p4-banner-json`) is retired, not run (section 14).

## 11. Battle 4 bonus options

House bonus style: one kind per battle, one tracker on the round state, 5 shards to the battle's House (Wildmoot), 40 gold (draft value), paid once on a win.

| Option | Kind | Value | Text (draft copy) | Shards | Gold | Notes |
|---|---|---|---|---|---|---|
| A (approved) | `never_rolled` (new) | 0 s | "Never let the wagon roll back." | 5 Wildmoot | 40 (draft value) | Teaches the mode's core rule: stay with the wagon. Tracker: any tick in Rolling back loses it. Achievable with care at Normal; idle time under 10 s is allowed. |
| B | `stage_by` (exists) | checkpoint 2 by 420 s (draft value) | "Reach Signal Ridge inside seven minutes." | 5 Wildmoot | 40 (draft value) | Reuses Siege's kind with `target` `signal_ridge`; no new tracker. Rewards pushing through the bridge fight fast. |
| C | `lose_at_most` (exists) | 6 troops (draft value) | "Lose no more than 6 troops on the road." | 5 Wildmoot | 40 (draft value) | Reuses battle 2's kind (Long Night in the 1.0 order uses it too, so it would repeat a lesson). |

Kort picked option A on 2026-10-31: bonus `b4_never_rolled`, kind `never_rolled`, value 0, target `none`, 5 Wildmoot shards, 40 gold, text "Never let the wagon roll back." It replaces the `b4_tbd` placeholder on `work/campaign-2c3`. The closed list becomes `hold_all`, `lose_at_most`, `stage_by`, `never_rolled`, `win_within`, `kill_unit`: `carry_self` leaves it, since no mode carries a banner. The economy model sheet is unchanged: 40 gold replaces 40 gold. Options B and C stay above for the record.

## 12. Size and phasing

Depends on: the mode frame (2C.1, `mode/frame`) merged; the bot columns `raidShare` and `chaseRange` added to `botCommand` (planned for carry; they move into E4); Frostmarch's Map Forge master layout signed and its road built (Phase 2A/3 map lane).

| Phase | Work | Size |
|---|---|---|
| E1 | `modes` row, `modeRules` columns, `modeSites` road rows (draft Frostmarch road), preflight road rules, `p4-convoy-preflight` | S |
| E2 | Base-type spike, `ArmyWagon`, kinematic motion, push/contest/idle/rollback, no-hp handling, teardown and crash marker | M |
| E3 | `ConvoyMode` hooks: clock and checkpoint bonus, overtime, anchors, income, end, `battleMode.convoy` JSON | M |
| E4 | Bot roles for both sides and the sim run | M |
| E5 | HUD road strip, ledger formats, ground ring, M map, feed lines, briefing lines; visual gate | M |
| E6 | Wagon and oxen model, Frostmarch canvas variant, wheel and walk clips; contact sheet at 9 | M |
| E7 | Map Forge: `road` protect kind, manifest generation, road check; road surface in the Frostmarch section passes | S (tool) + map lane |
| E8 | Campaign battle 4 rows, bonus kind and tracker, march suite change, copy hand-off to Kort | S |

4C.2 drops its carry half (stands, carriers, drops, `exempt`) and is the convoy alone, still about XL. E1 to E3 are the playable core; E4 and E5 make it a mode; E6 and E7 can run in parallel with E3 to E5 once E1's rows exist.

## 13. Calls made in this draft

- The mode row is `convoy`, name "Convoy", rule line "Convoy: reach {n}" with {n} the destination's name (draft copy). It replaces the Banner mode outright, so the lobby, picker and campaign say "Convoy", not "Banner" (Kort 2026-10-31).
- The wagon is kinematic on a polyline, not pathfinding.
- Contest is "any enemy in range stops it", matching the uncontested rule of Domination and Siege.
- No hp on the wagon.
- Oxen are part of the wagon model, not entities.
- Escort is one asymmetric round with no side swap, as Siege.

## 14. What depends on carry Banner, and what each becomes

Listed only; this spec changes no other file. Branches are named where the item is not on main.

| Item | Where | Becomes |
|---|---|---|
| `campaignBattles` march-4 `mode: "banner"` and its placeholder note | `sheets/design.json`, `work/campaign-2c3` | `mode: "convoy"`, difficulty normal, allies `ironclad_normal`, enemies `wildmoot_easy` and `wildmoot_normal`, bonus `b4_never_rolled`; placeholder kept only for briefing and reason copy |
| `bonusObjectives` `b4_tbd` | same | `b4_never_rolled` (section 11) |
| Preflight `MARCH_MODES` `{domination, siege, survival, banner}` and `BONUS_KINDS` with `carry_self` | `tools/preflight.py` lines 395-396, `work/campaign-2c3` | `banner` becomes `convoy`; `carry_self` becomes `never_rolled` |
| `MarchMath.Tracker`: `carriedSelf`, `carried(boolean)`, `case "carry_self"`, the kinds javadoc at line 89 | `MarchMath.java`, `work/campaign-2c3` | removed; gains a `rolledBack` flag, a `rolled()` call from `ConvoyMode.tick` and `case "never_rolled" -> !rolledBack`; `MarchMathCheck` follows |
| `ModeMath.roles(banners, humans, raidShare)`, its "Banner roles" javadoc and the `split` role | `ModeMath.java`, `mode/frame` | kept as the convoy role split (pusher/screen, raider/blocker), renamed; `split` dropped |
| `modes` rows (domination, siege; no banner row yet) | sheet on `mode/frame` | add `convoy`; no `banner` row is ever added |
| `modeRules` carry columns `carrierSpeed`, `pickupRadius`, `standRadius`, `dropReturnSeconds` (planned) | Phase 4 spec section 11, Phase 4 plan | not added; the convoy columns of section 9 instead |
| `modeSites` kind `stand` (planned, two rows) | Phase 4 spec and plan | not added; kind `road` instead |
| `botCommand` `raidShare`, `chaseRange` (planned) | Phase 4 spec | kept; added by convoy E4 |
| `exempt` and its three call sites, the carrier attack refusal, client sprint stop, `CARRIER_SPEED`, the map carrier emblem (planned) | Phase 4 plan Task 7 | not built; the map shows the wagon emblem instead |
| Phase 4 plan Task 7 "Banner on Rivergate" and suite `banner` (`p4-banner-setup`, `-pickup-troop`, `-pickup-commander`, `-norout`, `-drop`, `-score`, `-end`, `-bots`, `-map`, `-teardown-head`, `-json`) | `docs/superpowers/plans/2026-10-18-phase-4-modes-campaign.md` from line 447 | superseded by E1 to E8 here and suite `convoy` (`p4-convoy-*`) on Frostmarch |
| Phase 4 spec: question 1 (escort parked), section 7 Banner on Rivergate, the Banner feed lines, `ruleLine` "Banner: first to {n}", the harness `banner` row, the sim gate "at least one capture", "Tidecallers ford as their Banner answer" | `2026-10-18-phase-4-modes-campaign-design.md` | superseded by this spec; section 7 to be marked superseded when that spec is next touched |
| No-routing spec Banner bullet (carrier keeps `exempt`, `p4-banner-norout`) | `2026-10-22-no-routing-design.md` lines 401-403 | obsolete; the wagon has no morale |
| PLAN-TO-1.0 4C.2 "Banner: stands, carriers, drops, roles, escort decision if taken", evidence `p4-banner-*`; the battle table's "Banner" | `docs/PLAN-TO-1.0.md` | "Convoy: wagon, road, push and contest, roles", evidence `p4-convoy-*`; battle 4 mode "Convoy" |
| PARKED escort entry | `docs/PARKED.md` line 64 | removed (unparked and approved) |
| Vision Banner line "carry an enemy banner home, or escort a supply wagon" | `docs/VISION-AND-ROADMAP.md` line 155 | "Convoy: escort a supply wagon along a road while the enemy harasses" |
| Chapter-1 copy for "Raiders on the River" (`c1_b4_*`, Dunmar's `b4_bonus_kept` "You carried it yourself") | `docs/copy/chapter-1.md` | rewritten for The Frozen Standard in Kort's copy pass |
| Bonus draft option 4A `carry_self` | `docs/reports/campaign-bonus-draft-2026-10-31.md`, `work/campaign-2c3` | decided: `never_rolled` |
| Lobby mode list `MODES=List.of("domination")` and the field card rule line | `SocialHub.java`, `WarTableHome.java` (main) | gain `convoy` when the frame lands; never `banner` |

## Kort's decisions, 2026-10-31

1. The convoy replaces carry-the-banner and does not ship beside it. The mode row is `convoy`, "Convoy" (section 13); dependencies in section 14.
2. One-sided round; the host or skirmish player picks the escort side: yes.
3. The wagon has no health: yes.
4. Any single enemy within 6 blocks stops the wagon: yes.
5. A wagon left alone for 10 s rolls back to its last checkpoint, and checkpoints lock: yes.
6. Battle 4 as drafted: Frostmarch, convoy, Normal, the player and `ironclad_normal` against `wildmoot_easy` and `wildmoot_normal`: yes.
7. Battle 4 bonus "Never let the wagon roll back", `never_rolled`, 5 Wildmoot shards, 40 gold: yes.

Still gated on Kort case by case: protocol (`state_v10`), save changes and a custom entity type (section 9). As drafted the design hits none of them.
