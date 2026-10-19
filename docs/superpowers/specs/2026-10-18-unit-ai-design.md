# The soldiers think: unit AI and enemy AI, design

Draft v1, 2026-10-18, for Kort to correct; nothing here is planned or built. Invented numbers carry (proposal); unmarked numbers come from a sheet or an approved document. The controller's open calls are marked **controller's call**, drafting calls (call N); all are listed at the end for Kort to overturn.

## 1. Intent and why

Kort on his troops: "When commanding my troops I want them to do exactly what I say. I don't want them to stray away from my commands. If I ask them to hold they hold and defend, if I tell them to attack they attack, if I tell them to follow they follow. But I want them to do it without getting stuck, without messing up; I want it to feel smooth."

Kort on the enemy: "I want them to have a thinking commander, so they actually have someone who's thinking and working, not just mindless bodies throwing themselves at my troops to die. He plans and executes. If there are multiple bots on a team they collaborate, think, plan and execute based on what's in front of them and what's available."

Today falls short:
- **Movement is greedy.** `Tactics.walk` aims vanilla path search 16 blocks down the straight line and, stuck for 3 s, sidesteps and jumps; `Tactics.route` lets each soldier pick his own bridge.
- **Formations churn.** `Battle.formation(u)` renumbers slots at every death, and Follow turns with the commander's view.
- **Soldiers stray.** On Attack each takes the nearest enemy within 24 of himself; Hold chases anything within 10 of its anchor; ranged kite unasked.
- **The enemy has no plan.** `Campaign.botDecide` gives a bot's whole army one order every 3 to 8 s, faces it due east or west, measures straight across the river and, on Easy and Normal, counts every enemy on the map. In the sims Hard wins 9 of 20 against Normal, Easy 13 of 20, and the Dawn seat 13 to 17 of every 20.

## 2. Goals, non-goals, anti-goals

Goals: obedience (zero violations); smooth movement (nobody stuck on the 15 crossings or the Ironhold gate); unity (each group, and an All order's whole army, moves as one body); a thinking enemy (a General per bot banner with a plan, a House doctrine and tells; a council per bot team that follows a human's pings); a tier ladder built on knowledge and reaction alone; player commands for everything the enemy does; proof (a scorecard and harness suites).

Non-goals: combat numbers (damage, Phase 2's `counters`, traits, abilities, rout shares); Phase 4's mode plans (this phase gives them gates, sites and hooks); the bot economy; bot commander classes; mounts.

Anti-goals: difficulty that touches stats (health, damage, speed, gold, training, sight); a bot order the player cannot give, an order to one troop, or a faster order rate; omniscience at any tier; teleporting a stuck troop; a new dependency.

## 3. Constraints and what changes

- Minecraft 1.20.1, Fabric, Java 17; a 50 ms server tick (the AI's share is under Budget in section 6).
- Tests: one client per agent, at most two harness clients on the machine, never while Kort plays; isolated roots, `--offline`. Bot sims run on a dedicated server.
- Numbers live in `sheets/design.json`; `Spec.java` is generated. One `Battle` per server.
- After Phase 2 Task 3 (`ArmyMobs`, `Combat.resolve`, `Unit.tags`) and before Phase 4, in worktree `C:\Users\kortt\Melty\bannerbound-ai`, branch `phase-ai` (call 1), cut from `phase-2-houses`. It merges `phase-3-built-world`, whose landmarks shape the grid, and later Phase 2 tasks at four seams: `Battle.spacing` becomes clearance, structure targets join the fight set, `Houses.fords` becomes a cost profile, spiders climb off the grid. `p2-ironclad-bridge`, `p2-tidecallers-bridge`, `p2-riptide-ford` and `p2-spider-wall` must still pass.

| File, class | Today | This phase |
|---|---|---|
| `Tactics` | Targeting, kiting, `route`, `walk`; shields every tick. | Keeps strikes and `brace`; targeting to the Soldier, movement to `Steering`. |
| `Battle.formation(u)` | Slots renumbered among the living; turned by the order's yaw. | Stable Sergeant slots on walkable cells, facing the path; pure `FormationMath`, shared with `CommandMode`'s preview. |
| `Battle.tactical`, `command` | Two order paths; a river spot refused or pushed 12 blocks. | One `Orders.issue` for players, Generals and the harness; a river spot is moved, not refused. |
| `Assignment`, `Unit`, `Squad` | Three stuck fields on `Unit`. | Stance and source; a stable slot and a progress window; Sergeants and, for bots, a General. |
| `Army*` | `SwimGoal`; vanilla navigation. | `ArmyNavigation`; step height 1.0 (call 2). |
| `Morale` | Three groups and home radius 10 in code; flank against the ordered yaw. | Groups and radius from sheets; flank against the real facing; routs walk the base field. |
| `Campaign.botDecide` | One order per army; two constants in code. | `General` and `Council`; constants to sheets; bot buying unchanged. |
| `Combat.resolve` | `holding` follows the Hold order. | It follows the Hold or Brace stance. |
| Packets | `tactical_v1`; `state_v8` after Phase 2. | `tactical_v1` unchanged (its target may be a friend); the state packet takes the next id for a state word; new `path_v1`. |

## 4. The three brains

**Soldier**, one per troop. Data: a stable slot, a carrot, a progress window and a target. Every tick, steering and the shield; on his 10-tick beat, a target from his Sergeant's fight set, a step out and back within his stance, and facing the threat. He answers a new threat after his tier's `soldierReactTicks`. Cost: 5 µs a tick, 40 µs a beat (proposal).

**Sergeant**, one per group, Phase 2's cavalry and siege included: the body that decides. Data: stance, formation, anchor, slot table, path and fight set. Cost: a 5-tick beat at 60 µs (proposal). It decides:
- Cohesion: the anchor moves at the slowest member's pace (call 3), slows to 60% while a quarter of the group is 4 blocks off its slots, and waits at 8; nobody engages without two friends within 4, unless struck (proposal).
- Places: on an All order the Sergeants share one anchor and path: infantry front, support middle, ranged at the infantry's shoulders, cavalry on a flank, siege behind.
- Slots: set per order, a Legendary at the front centre, then `shield` and `spear` troops, then nearest first so paths do not cross; a death is filled from the rank behind (call 4).
- Fight: under Attack, one seen cluster, the one closing fastest; ranged shoot what their infantry fights; support mends the most hurt.

**General**, one per bot banner. From its team's sightings, the council's assignment and the fields, it decides objectives, who goes where, formation, crossing, timing and the tell before each blow. It acts only through `Orders.issue`, with the player's validation and 4-tick cooldown, aims only at enemies its team sees, and never orders Follow (call 5). Data: its picture, objectives and plan (section 7). Cost: a react beat at 0.3 ms and a plan at 1.5 ms, one plan a tick at most (proposal).

## 5. Orders and stances

An order sets a stance, kept until the next order; a soldier leaves his slot only within it. Only two things happen unasked: a group routs (Phase 1 morale; it resumes its order on regroup; question 1), and shields rise and menders heal.

`orders`, changed: `id S, label S, aim S, stance S, engage S, engageRadius I, moveMultiplier D, cohesion S, arrive S, meaning S`. `aim` (none, ground, enemy, friend, self) replaces `targeted`. `engage` replaces the boolean: `none` fights nothing, even when struck; `reach` fights from the slot; `guard` also meets enemies within `engageRadius` of the guarded troop or spot; `free` lets the Sergeant take a cluster within `engageRadius` of the anchor (call 6). `arrive` hold means the group holds where the order ends (call 7).

| id | aim | stance | engage | radius | speed | cohesion | arrive |
|---|---|---|---|---|---|---|---|
| move | ground | march | none | 0 | 1.0 | keep | hold |
| attack | ground, enemy | advance | free | 24 | 1.0 | keep | hold |
| charge | enemy | advance | free | 32 | 1.25 | broken | hold |
| hold | ground | hold; brace in Shieldwall | reach | 0 (was 10) | 1.0 | keep | stay |
| follow | self | guard | guard | 10 | 1.0 | keep | stay |
| retreat | ground | march | none | 0 | 1.15 | loose | hold |
| flank_left, flank_right | enemy | advance | free | 24 | 1.0 | keep | hold |
| screen | friend, ground | guard | guard | 12 | 1.0 | keep | stay |
| fall_back | ground | march | reach | 0 | 0.8 | keep | hold |
| focus | enemy | hold | reach | 0 | 1.0 | keep | stay |
| guard | friend | guard | guard | 10 | 1.0 | keep | stay |
| skirmish | ground, enemy | skirmish | reach | 0 | 1.0 | loose | stay |

New rows and cells, and the distances below, are proposals.
- **Controller's call:** a held troop struck from outside its circle turns to the attacker, shield first; ranged shoot back; nobody chases.
- **Controller's call:** a charge breaks cohesion on purpose; the Sergeant re-forms when the cluster is beaten.
- Follow faces your walking heading, not your view, re-anchoring after 3 blocks or a 30-degree turn held 1 s (call 8).
- Flank swings 12 blocks wide and strikes the aimed group in morale's rear arc.
- Screen stands 6 blocks before the aimed friend or spot, facing the nearest enemy within 30.
- Fall back walks there at 0.8 pace, striking only what reaches it, then holds.
- Focus: ranged shoot only the aimed troop, then its cluster.
- Guard forms on an aimed troop of yours and goes where it goes.
- Skirmish: ranged keep 75% of range, stepping back from an enemy within 4; melee strike and withdraw 8 blocks.

`stances`, new: `id S, stepOut D, shields S, standoff D, kite D, text S`.

| id | stepOut | shields | standoff | kite |
|---|---|---|---|---|
| march | 0.0 | approach | 0.0 | 0.0 |
| advance | 1.5 | approach | 0.65 | 0.0 |
| hold | 1.5 | approach | 0.0 | 0.0 |
| brace | 0.0 | braced | 0.0 | 0.0 |
| skirmish | 0.0 | approach | 0.75 | 4.0 |
| guard | 1.5 | approach | 0.0 | 0.0 |

`approach` and `braced` are today's Compact and Shieldwall shield rules; 0.65 and 4.0 are today's `rangedStandoff` and `rangedRetreatDistance`; the other cells are proposals. Ranged kite only on Skirmish (call 9); Brace is Hold in Shieldwall, needing no key (call 10). `formations` gain `pace D` (Shieldwall 0.75) and `turnRate D` (30 degrees a second, Shieldwall 20); `groups` gain `place S`, `maxColumns I` (12, 6 and 2 as today; cavalry 5, siege 4) and `cohesionRadius D` 4.0 (proposals).

## 6. Movement

**Grid.** `NavGrid` keeps one cell per block column of the arena (about 351,000): feet height, flags (deck, road, water, gate, and cover: no sky, as Phase 2's ambush reads it), clearance and bank, about 1 MB (estimate). A cell is open when its feet and head blocks have no collision shape (Phase 3's walk test, moved into `NavGrid`); a step may rise 1 or drop 2. Costs in `navigation` (proposal): straight 10, diagonal 14, +6 a block up, +2 down, road -2, shallow +30, deep closed except +5 for troops `Houses.fords` lets wade. It is computed from the placed world after `Arena.finish` and the landmark queue, before `campaign.built()`, and cached beside the layout marker by landmark hash; it is not shipped, because the pieces still change (call 11). Gates are flagged cells switched by `NavGrid.setGate(id, open)`.

**Flow fields.** Dijkstra from every capture point, crossing mouth, base and Phase 4 site: about 12 fields at 2 bytes a cell (8 MB, estimate). They give the General true travel times and lead groups to sites without search; a gate change rebuilds only the fields through it, inside Phase 4's 10 s portcullis warning.

**Group path.** To a site the path is read off its field; to any other spot, the cheapest crossing, then A* on the far bank under a shared budget (4,000 nodes a tick, proposal), string-pulled to keep half the frontage clear. One group, one crossing. **Controller's call:** an order across the river with no crossing on its line takes the cheapest crossing, and the feed names it; a spot no troop can stand on moves to the nearest open cell within 12 (proposal). The anchor slides along the path at the group's pace, turning no faster than `turnRate`. Where clearance is narrower than the frontage, columns drop (at 1.8 spacing: 8 abreast on a 16-wide deck, 3 on a ford's 7-wide causeway, 5 in the 11-wide Ironhold breach) and the group re-forms beyond.

**Steering.** Each tick a soldier seeks a carrot 2.5 blocks ahead toward his slot and keeps 1.2 from friends (a spatial hash of 4-block cells, rebuilt each tick). He passes a stopped friend by a 0.8 sidestep instead of shoving, and a soldier in his slot gives half a block to one passing. Closed and deep cells are walls, so deck edges hold. Under 25% of expected progress for 2 s, he sidesteps along the clearance, then paths alone on the grid, then falls back to vanilla. All proposals.

**Living with `MobNavigation`.** Each `Army*` returns `ArmyNavigation extends MobNavigation` from `createNavigation`. Minecraft ticks navigation just before the move control, so `ArmyNavigation.tick` steers and calls `moveControl.moveTo` every tick (the move control drops its target after one). No vanilla search runs on the grid. Step height 1.0, as the iron golem has, walks one-block rises, while fences and walls (1.5) still stop troops (call 2). Still vanilla: `MoveControl`, `JumpControl`, `LookControl`, `SwimGoal`, and entity pushing as a last resort. Off the grid (a closed cell, or feet over 1.5 above it: roofs, stairs, Phase 4's stations at feet 7 and 13), it calls `super.tick()` with a vanilla path to the nearest open cell within 8, one search per troop per 20 ticks and four a server tick (proposal). After 5 s (proposal) his Sergeant stops waiting; nobody is teleported (call 12). Spiders get `ArmySpiderNavigation extends SpiderNavigation` and climb only where the grid has no way; `ArmyDrowned` overrides `updateSwimming`, so the drowned's water navigation never replaces ours.

**Budget.** `p1-tick-cost` measured a 3.2 ms mean server tick at 160 troops. Proposed for all AI: at 60 troops, under 1.5 ms mean and 4 ms p99 (steering 0.3, Soldier beats 0.24, Sergeants 0.1, paths 0.2 with a 1 ms cap, Generals and council 0.1); at 160, under 4 ms mean and 8 ms p99. The 40 ms gate (`p2-tick-cost`) stays.

## 7. The General

Its planning loop:
1. **Picture:** its team's sightings within its tier's memory (`Battle.sightings` keeps the longest), clustered into enemy groups with kinds, facing, speed and a formation read from positions, never from orders (call 13); plus its own forces, the score and the clock.
2. **Objectives:** points, crossings, a reserve spot, raids, ambushes and Phase 4 sites, scored for value, field time, seen risk (`defenderWeight`), the answers, the doctrine and the council.
3. **Plan:** a short chain such as "to the ford mouth, attack C, hold C until a stronger cluster comes within 30, fall back over the ford". Before ground unseen beyond its memory, Normal and Hard scout first (cavalry, else ranged on Skirmish) while the body waits up to 15 s (proposal); scouts and reserves are whole groups (call 14).
4. **Execute:** each order through `Orders.issue`, after its tell.
5. **Watch:** contact, a threatened point, a rout or rally, a formation change, a flip, a ping, a step overdue by half: adjust or plan again.

It garrisons a point it takes (`BattleMath.garrison`, the picker PARKED asks for) and gives ground group by group.

`doctrines`, new, one row per House (a mixed banner takes its larger House): `id S, line S, advance S, reserve S, fallBackShare D, rallyShare D, chargeRatio D, chargeDistance D, defenderWeight D, crossing S, haltEvery I, haltTicks I, ambushRange I, fadeTicks I, text S`. Judgement moves here from `botCommand`: it is a House's character, not a difficulty (call 15). Proposals:

| House | Line, advance | Gives ground | Crossing | Plan, Legendary |
|---|---|---|---|---|
| Gilded Order | Shieldwall, Compact; one group in reserve | at 35%, onto the reserve | any | holds a line across the enemy's road; the reserve meets a flank; Aldric's oath banner at the centre |
| Graveborn | Compact, Wedge | never | any | presses the same objective after every rally, charges at 1.2 to 1, hunts the weakest cluster; the Hollow King's dread at the centre |
| Ironclad | Shieldwall at 0.75 pace | at 25% | any | 12-block bounds, then a 3 s halt for Entrench and the engines; Brassjaw at the centre |
| Tidecallers | Loose, Wedge | at 40%, back over the water | fords, open water from tier II, never a bridge | raids the least-held point a ford reaches and leaves when a stronger cluster comes within 30; Vael against the enemy ranged |
| Wildmoot | Loose | fades at 50% | any | waits in cover off the enemy's road; charges a cluster passing flank-on within 16; fades to cover after 8 s; Old Horn leads |

A Gilded banner without a spare group keeps no reserve (question 2).

## 8. The council

One per bot team. It gives each General an objective by field time and fit (siege to gates, cavalry to raids, shield lines to holds), nearest pair first, and plans again on a flip, a rout, an emptied banner, a ping, or every 10 s (proposal). On Hard it runs plays: pincer (two crossings, timed to arrive within 5 s, proposal), anvil (one pins, one flanks), relief (to a garrison outnumbered two to one) and screen (shields before a siege or ranged banner). With a human on the team, his pings are the plan: Attack draws the nearest bot to him and another to his far flank; Defend, a bot holds there; Help, the nearest bot sends its best group; Moving claims the objective, so no bot doubles him. Bots never order a human's troops.

## 9. Difficulty

`botCommand` keeps knowledge and reaction: `decisionTicks` becomes `planTicks`, `flankLeastDefended` goes, the shieldwall columns become answer A1, judgement goes to `doctrines`, and Phase 4's columns (`useStations` and the rest) join it. Proposals:

| Column | Easy | Normal | Hard |
|---|---|---|---|
| planTicks | 400 | 240 | 160 |
| reactTicks | 80 | 40 | 20 |
| soldierReactTicks | 16 | 10 | 6 |
| memorySeconds | 5 | 10 | 20 |
| predict (unseen clusters keep heading) | false | false | true |
| scouts | false | true | true |
| readFormations | none | infantry | all |
| council | objectives | objectives | plays |
| tellScale | 1.5 | 1.0 | 0.6 |

No tier sees beyond its team's line of sight (call 16). Your troops react at Hard's 6 ticks (question 5).

## 10. Tells and answers

`tells`, new: `id S, trigger S, cue S, leadTicks I, radius D, feed S`. Leads scale by `tellScale`; cues carry 64 blocks, the ambush's 32. Proposals:

| id | Trigger | Cue | Lead (ticks) |
|---|---|---|---|
| T1 | a charge | `horn_charge` (exists) | 40 |
| T2 | a commitment | the scouts, seen first, then `horn_commit` | 200 |
| T3 | the Gilded reserve commits | `cue_reserve` | 40 |
| T4 | an Ironclad bound | `cue_advance` | 20 |
| T5 | a Tidecallers raid | `cue_raid` | 60 |
| T6 | a Wildmoot ambush | `cue_ambush` | 30 |
| T7 | a fall back | `cue_fallback` | 20 |

`answers`, new (Phase 2 owns `counters`; call 17): `id S, seen S, range D, minCount I, answer S, groups S, text S`. The General answers only what it has seen and its tier reads. Proposals except A1, today's Hard rule:

| id | Seen | Range, count | Answer, by |
|---|---|---|---|
| A1 | archers, no enemy melee within 8 | 30, 3 | Shieldwall, infantry |
| A2 | blight or mist casters | 24, 1 | Loose (wider than blight's 2.5), all |
| A3 | an infantry line two deep or less | 40, 6 | Wedge and charge its centre, infantry |
| A4 | cavalry | 24, 2 | Compact on Hold, infantry |
| A5 | a Compact block | 40, 6 | Line, ranged |
| A6 | archers with no infantry within 8 | 40, 3 | flank or charge them, cavalry else infantry |
| A7 | a Shieldwall advancing | 40, 6 | Skirmish, ranged; the infantry fall back |
| A8 | cavalry within 12 of our support | 12, 2 | Guard the support, infantry |

## 11. What the player sees

Named here, designed with the UI work: card state words beside the UI spec's Fighting, Steady, Fallen and ROUTING (Marching, Holding, Braced, Skirmishing, Guarding, Re-forming, Blocked); aim prompts for the new aims; a path ribbon for 3 s (proposal) after confirm, from `path_v1` (call 18), naming the crossing ("Infantry / Attack / by the south ford"); feed lines for moved orders, blocked troops and heard tells; the new cues; a report line per enemy General; a harness-only `AiDebug` overlay.

## 12. Harness and scorecard

`AiMathCheck`, standalone javac like `BattleMathCheck`: costs, A*, fields, string pulling, slots, the formation read, objective and council scoring. `AiChecks` client suites (`ai-*` ids; the class joins the release forbidden list; fixture numbers are proposals):
- `grid`: closed road, deck and ring cells match `Walkability.scan` (road 0, through 0); a pinned hash and picture per map.
- `movement`: Phase 1's river and hill checks grown to all 15 crossings both ways (20 knights in Line, 20 of 20 within 1.3 times the field estimate, nobody blocked 3 s) and the Ironhold gate; a turn in place; a troop knocked into the river; the widest troops (ravager 1.95, golem 1.4) on every causeway.
- `cohesion`: a march with one slowed troop; an All order's places; deaths moving only the ranks behind.
- `obedience`: a fixture per order, including Hold shot from outside its circle, Follow while the commander looks around, 20 orders in 2 s and 120 s of provocation.
- `general`: fixed sightings on Rivergate and Kingsfield: field-time choices, garrisons, answers only when seen, tells on time, each doctrine, the pincer, a followed ping, valid rows only.
- `cost`: the budget at 60 and 160 troops.

The `p1-bots-*` checks move to the General (shieldwall to A1, charge timing to T1; least-defended retires). `BotSimulation` adds `ai.csv` per match and banner. Targets (proposals); movement's are the same at every tier:

| Metric | Definition | Target |
|---|---|---|
| Stuck rate | moving troop-seconds under 25% of expected progress for over 3 s | at most 0.5% |
| Cohesion index | share of a moving group within `cohesionRadius` of its slots | at least 0.85 |
| Obedience violations | a troop doing what its stance forbids, or a bot order outside `orders` | 0 |
| Time to objective | arrival time over the field estimate | at most 1.25 |
| Friendly blocking | moving troop-ticks held up by a friend | at most 3% |
| Formation integrity | mean slot error at rest | at most 0.8 blocks |
| Reaction | seen trigger to order | Easy 6 s, Normal 3 s, Hard 1.5 s |
| Ladder | wins over the tier below, equal banners (call 19), both seats, 20 matches | Normal and Hard 60% |

The Normal mirror's Dawn seat must win 40 to 60%. One obedience violation fails the suite.

## 13. Order of work

Each task ends with its suite, screenshots a tester has opened, and the tick gate.
1. Pure core and sheets; no behaviour change.
2. Grid, fields and cache; `grid`.
3. Movement, with `route` and `walk` deleted; `movement`, no stuck troop on any crossing.
4. Sergeant; `cohesion`.
5. Soldier, engage rules, `Orders.issue`; `obedience` for today's six orders.
6. New commands, aims, `path_v1`; `obedience` in full. Kort checkpoint: do his troops obey and move smoothly against today's bots?
7. General, doctrines, answers, tells; `botDecide` retired; `general`.
8. Council and pings.
9. Ladder: scorecard sims on the Phase 3 maps, sheet tuning only.
10. Regression (Phase 1 to 3 suites, `routes`, tick cost) and Kort's gate: a 1v2 on Hard and a 3v3 with bot allies.

## 14. Questions for Kort

Recommended answers in brackets.
1. **Rout on Hold.** Kort, 2026-10-18: **hold means never break.** A held group is exempt from the rout rules (the quarter-strength break and the strike-from-behind break) for as long as the order stands; when a member falls, the survivors close the gap in the formation as best they can (re-slot toward the centre of the line, the front rank refilled from the rear) and keep holding. The group card still shows morale as information, never as a break. The cost is that Hold is strong, which is the point: a line that holds because it was told to is the player's reward for choosing where to stand.
2. **Detach.** Kort: **yes, once per group.** Detach splits a group into two halves that take orders separately; a second Detach on either half is refused. Rejoin is the reverse and is free. The General has the same order.
3. **Tier and banner.** Controller's ruling: keep Phase 2's stronger Hard banners (an approved spec), and the AI scorecard measures the tier ladder with equal banners, so the brain's contribution is read on its own. The "never stats" rule binds the brain, not the roster.
4. **How Hard should feel:** Kort: **a fight the player loses until they learn its tells and strategies.** The scorecard's target for Hard against a scripted "first-time" player is a loss; against the scripted "learned" player (one who answers the tells) it is an even fight.
5. **Your troops' reaction:** controller's ruling: always Hard's 6 ticks. The player's troops obey at full speed whatever the enemy's tier.
6. **A visible general:** controller's ruling: a brain only in this phase. A banner-bearer is a later chapter's unit, not an AI feature.
7. **Allied bots:** Kort: **announce their plans, and the player can ask them for aid and tell them where to look.** Each allied bot posts its current objective as a map marker and a feed line. The player gets two requests on the ping wheel aimed at a place: "Aid here" (the council sends the nearest allied bot with a spare group, or answers "Cannot, holding B" in the feed) and "Watch here" (the council adds the place to its objectives at the player's priority). The council treats a request as an order when it can be met and says why when it cannot.
8. **Keys:** controller's ruling: the new commands live in the R council only until Kort has played them; no new default keys in this phase.

## Calls made in this draft

Controller's calls: Hold answers in place and never chases; an order with no crossing on its line takes the cheapest crossing; a charge breaks cohesion.

Drafting calls:
1. Worktree `bannerbound-ai`, branch `phase-ai`.
2. Step height 1.0.
3. Groups move at the slowest member's pace, except on Charge.
4. Stable slots; a Legendary, then shield and spear troops, in front.
5. Generals have no class and never order Follow.
6. `engageRadius` counts from the anchor; Hold's falls to reach.
7. Move never fights; an ended order holds.
8. Follow faces your walking heading.
9. Ranged kite only on Skirmish.
10. Brace is Hold in Shieldwall.
11. The grid is computed and cached, not shipped.
12. Nobody is teleported.
13. Formations are read from sightings only.
14. Scouts and reserves are whole groups.
15. Judgement numbers move to `doctrines`.
16. No tier sees beyond its team's sight.
17. The formation sheet is `answers`.
18. The path ribbon shows after confirm.
19. The ladder uses equal banners.
