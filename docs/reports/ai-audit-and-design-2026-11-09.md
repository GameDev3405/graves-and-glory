# Bot and AI: audit, research and design (ai-lead milestone 1, 2026-11-09)

Branch `ai/lead` (worktree `bb-ai`, cut from `main` 2a1e26b4). No game code changed. One harness-only change: `test-tools/.../BotSimulation.java` gains a Domination trace (`domination.csv`, `domination-units.csv`) like the existing Convoy trace.

Kort's goal: bots and units that always have a purpose, so a player cannot tell a bot from a person.

Labels used below: **(data)** seen in a sim run or CSV; **(code)** read from source, not yet watched; **[n]** a numbered source in Appendix C.

## 1. Summary

1. **Today's bot is one decision for the whole army.** Every 3 to 8 seconds `Campaign.botDecide` picks one capture point and gives every group the same order and spot (`Campaign.java:771-819`). Ranged troops stand 5 blocks behind the infantry's spot instead of where they can shoot, an army that holds a point does so by accident of a distance score rather than to guard it, facing is fixed toward the enemy base, and the bot never uses relics, commander abilities, custom groups or formations other than block/loose.
2. **Each troop is a nearest-enemy follower.** Every 10 ticks a troop takes the nearest enemy inside a radius, with no line-of-sight check outside a player's Attack, no focus fire, no kiting, no screening and no self-preservation (`Tactics.java:243-343`). Archers on Hold only engage inside 10 blocks though they reach 22 to 30.
3. **Difficulty is only decision speed, retreat thresholds and banner strength.** No stat cheats (good), but Hard's flanking is switched off since 2026-10-18 so Hard differs from Normal only by a faster clock, a lower retreat share, the fair 18-block charge and `defenderWeight` (which is never reached), and Easy/Normal read the whole map's enemy count for their charge rule.
4. **The two failing gates.** Domination's "Dawn wins 8 of 10" is mainly the House pairing: the default sim always puts Gilded on Dawn and Graveborn on Dusk, and Gilded wins whichever seat it takes; the same bots pass in a Gilded mirror (section 3). Convoy is mode timing and bot escort play together: the first checkpoint is 262 road blocks away and pushing runs at about 0.75 blocks/s on the flat (faster downhill), so the escort must keep the wagon uncontested for most of a 420 s round, while today's escort leaves it contested 46 percent of the time.
5. **Design:** it **builds on the unit-AI spec Kort approved on 2026-10-18** (General per bot banner, council per team, doctrines, flow fields, `Orders.issue`; not built yet) and on the **no-routing ruling of 2026-10-22** (no group of any side ever breaks). This report adds what that spec lacks for "can't tell it from a person": a three-layer brain (commander, squad, troop), the shape used by Killzone 2/3 bots, Paragon and Days Gone [9][13][14], filled with utility scoring [1][3] and a short ordered plan per squad (HTN-lite) [8][9]; fair vision (only what the team saw, with memory) [9][27]; difficulty by reaction time, attention and judgement, never stats [28][32]; every unit carries a one-line purpose that a debug overlay shows. Modes and maps feed it data (objectives, lanes, rally points), so new units, Houses, maps and modes get competent play without new bot code.
6. **No machine learning in the decision core.** It is not realistic for this mod (cost, no fast simulator, explainability) [32][33]. The useful lessons from AlphaStar and OpenAI Five are human limits (reaction time, attention, action rate) and league-style testing with exploiter bots, and those are in the plan.
7. **Plan:** measure first (small), then fix the worst bot-commander behaviours inside today's code (medium, needs Kort's yes as gameplay change), then build the approved spec's General and council with this report's additions (large), then personalities, difficulty and a blind "bot or person" test. All of that fits 1.0; heavy automatic tuning, off-thread path planning and adaptive difficulty wait for after the 1.21.1 port.

## 2. What the bots actually do (sims, 2026-11-09)

Runs (all `-PsimFast=true`, Normal vs Normal, 3v3, 10 matches each, Domination maps 0-4 in turn, 300 s clock; roots under `work/method/` in this worktree, not committed):

| Run | Dawn banner | Dusk banner | Result |
|---|---|---|---|
| `ai-dom-191818` (default gate) | gilded_normal | graveborn_normal | Dawn 8, Dusk 2; gate fail |
| `ai3-domtrace-195503` (same, with trace) | gilded_normal | graveborn_normal | Dawn 8, Dusk 2 (reproduced) |
| `ai2-domswap-193720` (seats swapped) | graveborn_normal | gilded_normal | Dawn 0, Dusk 10 |
| `ai2-dommirror-194706` (same House) | gilded_normal | gilded_normal | Dawn 4, Dusk 4, 2 draws; gate pass |
| `bb-campaign-base/work/method/base-convoy-190225` (campaign session's run on pre-landing main, Frostmarch) | native | native | Dusk 10 of 10, 0 checkpoints |
| `ai3-convoy-200447` (rerun on this branch, Frostmarch) | gilded_normal | graveborn_normal | Dusk 10 of 10, 0 checkpoints (gate fail); best push 231 of 262 blocks, median about 150; wagon moving 50%, contested 46%, idle 3% of samples; while contested the escort infantry is 37 blocks from the wagon on average (more than 6 away 97%). Confirms the earlier run on current code. |

### 2.1 Moments a person would never play (reproducible: same command, then read `botsim/domination.csv` at the given match and second)

| # | Where | What happens | Why (code) |
|---|---|---|---|
| D1 | Domination, Kingsfield (map 4), `ai3-domtrace` match 5, s 5 to 60 | Each team sends **all three armies to the same first point** (two armies switch only at s 65 and s 80) ((-130,100) for Dawn, (130,-100) for Dusk); nobody goes for the centre or the other three points. | Lane choice is overridden by the distance score; all armies start in one spot and score the same (`Campaign.java:791-794`). |
| D2 | Same match, s 65 to 295 | Dusk army 4 stands on its first point for **235 of 300 s** with no enemy within 150 blocks; Dawn army 1 does the same at (-130,100) from s 75. A third of all troops do nothing for four minutes while the centre is fought over, and point 5 is never taken by anyone. | Staying is not a decision to guard: an owned point costs +140, and from a far corner every other point is more than 140 blocks away, so the army "chooses" to hold forever. |
| D3 | Ironhold (map 1), match 2, s 90 to 150 | Dusk owns all three points at s 90 because it spawns about 150 blocks closer. Dawn army 1's front line fights at the centre (x about 0) while its refills, spawned at x -250 to -310, walk in one at a time; the group's centre therefore sits 100+ blocks back (living count 10 to 7 by s 125). | Trickled reinforcements (same cause as C3). Lesson for the metrics: measure idle and objective share per troop, not per group centre. |
| D4 | Ironhold, match 2, s 295 | Dawn kills 106 to Dusk's 26 yet loses 75 to 107: it wins fights but reaches the points late, and Dusk's broken armies of 3 and 4 troops keep walking one by one into points Dawn holds. | No regroup before re-entry; a broken army walks to the same point (`Campaign.java:780-799`); retreat only below 25% of the full banner. |
| D5 | All Domination runs | **Every group of an army always has the same target** (every sample in all four runs, e.g. 3,533 of 3,533 in `ai3-domtrace`). Ranged troops stand 5 blocks behind the infantry's spot, not where they can shoot best. Each army changes its target on average **1.9 times per 300 s match**. | One order per army (`Campaign.java:799-815`); slot depth fixed by role (`BattleMath.java:558`). |
| D6 | All 40 Domination matches (4 runs) | **0 retreats** by any bot army. | Retreat only below 25% of the banner's full count; armies are usually refilled first. |
| C1 | Convoy, Frostmarch, `base-convoy-190225` match 1, s 180 to 240 | The wagon is stopped ("contested", 4 to 10 defenders within 6 blocks) while the escort's infantry stands 40 to 45 blocks **behind** it. | One defender within 6 blocks stops the wagon (`ModeMath.java:104-105`); escort groups on Attack chase off the road (engage radius 24). |
| C2 | Same run, all matches | The wagon is contested **44 percent** of the time; while contested, the escort infantry's centre is on average **34 blocks** from the wagon (more than 6 blocks away 96 percent of the time). Nobody clears the defenders off the wagon. | No "clear the wagon" task; groups go to their role spot, not to the threat. |
| C3 | Same run, match 1, s 230 to 250 | Dusk reinforcements walk 100+ blocks along the road **in single file**, one troop every few blocks, into a fight they lose one at a time. | Refills spawn at base or the next checkpoint and walk straight to the army's target; no gathering (`GravesAndGlory.java:1139-1146`). |
| C4 | Same run, match 1, s 5 to 90 | The Dusk defence starts 470 blocks from the wagon and needs about 90 s to reach it. | Spawn layout for Convoy on Frostmarch. |

Stuck troops: none in the Convoy traces; in Domination a handful of troop samples hit the stuck counter's cap (4 to 46 per run), e.g. one ranged troop pinned at (-115,-138) from s 130 to 270. Pathing is a minor cause next to the decisions above.

## 3. The failing gates: bots, balance, or both

- **Domination: House balance first.** Gilded beat Graveborn in all 16 matches off Ironhold, from either seat; with the same House on both sides (mirror run) the same bots split 4-4 with 2 draws and pass. Caveats: `ai-dom` and `ai3-domtrace` are near-repeats, so the seat-swapped evidence is about 20 independent matches, and the sim stops at 300 s of a 900 s round, which exaggerates early leads. **Ironhold** is a seat problem on its own: Dusk spawns about 150 blocks closer to every point and won all 8 Ironhold matches across the four runs, including mirror matches where Dawn out-killed it 66-27 and 61-34. The bots are not shown to cause the gate failure; they magnify spawn and early leads (no garrison by intent, no regroup, trickling refills, D1-D6). The default gate always fields Gilded on Dawn against Graveborn on Dusk, so it cannot separate seat from House; `tools/balance_gate.py` with swapped seats should be the Domination check.
- **Convoy: timing and bots together.** The gate needs the escort to win 30-70 percent, mean checkpoints at least 1.5 and no stall over 60 s (`tools/mode_gate.py`). On flat road the first checkpoint needs about 349 s of pushing in a 420 s round, so it is only reachable if the wagon is contested well under a fifth of the time; downhill pushing is faster (grade factor 1.25). The best match got to 231 of 262 blocks; 5 of 10 passed 150. History matters: on 2026-11-02 the escort won 10 of 10 before the push speed cut and defence-bot tuning (`BLOCK_AHEAD`, no retreat), so both the mode numbers and the bots move this gate. Changing a mode lever before the escort bots improve risks another overshoot.
- **Order:** balance and mode levers are Kort's (section 9). Any balance fix must be re-checked after each bot milestone, because better bots will shift it.

## 4. Audit: every decision point

The full tables with `file:line` for each point are in Appendix A (troops) and Appendix B (commander, modes, campaign, economy, harness). This section is the short version.

### 4.1 Commander (one per bot seat)
| Decision | Where | Runs | Information | Problem |
|---|---|---|---|---|
| Buy troops toward banner | `Campaign.java:745-755` | every 5 s | own gold | fixed banner order; no reaction to what the enemy fields; a wiped army waits 12 s |
| Buy gear | `Veterans.java:399-419` | every 5 s | own state | fine |
| Retreat / rally | `Campaign.java:780-782` | each decision | own living count only | whole army walks to base; no partial fall-back; 0 retreats seen in 40 Domination matches (data) |
| Point choice | `Campaign.java:791-794` | 8/5/3 s (Easy/Normal/Hard) | point owner and contested flag | after a capture all armies move on; nobody guards; armies converge on one point |
| Flank / garrison | `Campaign.java:783-790, 824-837` | never | team sightings | dead code: no tier enables it |
| Order and formation | `Campaign.java:801-802` | each decision | point owner, army size | one order and one spot for every group; only block/loose |
| Charge (Easy/Normal) | `Campaign.java:809` | each decision | **every living enemy on the map** (cheat) | almost never fires; reads hidden information |
| Charge (Hard) | `Campaign.java:803-808` | each decision | team sightings (fair) | counts enemies within 36 but needs one within 18 |
| Facing | `Campaign.java:815` | each decision | team | always faces the enemy base, even on flank points |
| Mode hooks | `BattleMode.java:63,107,110,113`; Convoy/Siege/Survival | each decision | mode state | Domination has none; Siege's `howToSide` and `botPlan` fight over the order |
| Takeover of a dropped player | `GravesAndGlory.java:918` | once | lobby tier | uses lobby tier and lane 0 even in a campaign battle |
| Relics, abilities, custom groups | `Commander.java:110,126`; `CustomGroups.java:37` | never | - | bots never use them |

Unused sheet columns: `archerThreatCount`, `archerThreatRange` (never read), `defenderWeight` (unreached), `enemyKnowledge: visible_only` (not enforced), `orders.hunt`, `navigation.unreachableNoticeSeconds`.

### 4.2 Squad layer
There is none. A group is only a slot layout around the army's one target (`GravesAndGlory.java:1024-1040`); ranged sit 5 blocks behind, support 9, regardless of terrain. Siege's `botPlan` and Convoy's role split (`ModeMath.java:52`) are the only per-group decisions.

### 4.3 Troop (every 10 ticks per troop, `Tactics.java:243`)
Decisions: beat gate, arrival latch, engage radius, leash, target keep/lock, target scan (nearest), shield raise and crawl, ranged back-off, fire gate, melee approach, ranged standoff, idle-to-slot, bridge and gate routing, waypoint walking, repath, stuck detection, step-hop, sidestep and detour, automatic abilities, morale (cosmetic). Worst problems (code; the sims in section 3 confirm several):
1. Archers on Hold or Follow engage only inside 10 blocks (reach 22-30) (`Tactics.java:264`; engage radii from `Spec.ORDERS`).
2. Target choice ignores walls and hills; archers then circle a building; melee walk into walls (`Tactics.java:274-279, 318, 331-334`).
3. Troops on Retreat or Move ignore enemies, even when hit, and bot retreats keep shields down (`Tactics.java:35-43`, `BattleMath.java:657`). For player troops this is the documented order ("ignore enemies"); the defect is the bot choosing Retreat under fire.
4. Pure nearest-enemy targeting: no focus fire, no health or counter weighting, no reachability (rivers send melee to far bridges), and a lock that is not renewed so troops dither between two enemies (`Tactics.java:274, 301, 404-424`).
5. Melee crowd the nearest enemy (the spread at `Tactics.java:335` only offsets around one target), then crowd-hop and step-hop when blocked by friends (`Tactics.java:450-466`).
6. One arrow slows a whole shield column to a third for 3 s, far from the fight (`Tactics.java:155-158`).
7. Abilities fire on timers: Howl with no enemy near, Blight on the nearest enemy rather than a cluster, Oath Banner only on a change to Hold (`Abilities.java:713-756, 280-292`).
8. Morale's "last stand" is information only (`Morale.java:53` on); that is by design under the approved no-routing ruling, not a defect.
9. Arrows are homing guaranteed hits that pass through blocks after release (`Projectiles.java:178-181, 245-251`), so movement never avoids damage. (A combat rule, not AI; listed because it removes kiting as a skill.)

### 4.4 Campaign enemies
Chapter 1 (oath-1 to oath-6) sets map, mode, size, tier and banners by lane; there is no per-battle AI, no scripted events, no enemy "captain". The `kill_unit` boss path is dead with the current sheet (Appendix B section 3).

### 4.5 Harness
`-PbotSim` (`test-tools/.../BotSimulation.java`) writes results, gold, levels, types, and per-group traces for Siege and Convoy (now Domination too). Missing: idle time, objective share, formation hold, a "why" log for each decision, a tier ladder, Survival and Banner checkers. Domination's sim clock is forced to 300 s, so every sim match is a timeout decided on score lead (Appendix B section 4).

### 4.6 Cost today
Measured once: `p1-tick-cost` gave a 3.2 ms mean server tick at 160 troops (quoted in the 2026-10-18 unit-AI spec); AI's share was not split out. From code: every troop runs `Battle.unitTick` every tick; target scans are O(n²)/10 per tick; `Battle.formation` scans every unit and is called many times per troop, with an O(g²·n) arrival check for retreating groups; worst case about 16 vanilla path searches per tick at 160 troops (Appendix A section 3).

## 5. Research (short)

Appendix C has the full notes and 50+ sources. What matters for this game:
- **Layered chain of command** (commander -> squad -> individual) is the best-documented fit for a few dozen units: Killzone 3 bots use mode-specific objectives with weights and wanted sizes, squads with their own planner, and individuals that may break orders to survive [9]; Paragon's commander loops over an objective graph and keeps each team's plans secret from the other [14]; Days Gone squads pick goal, roles and area and drive press/hold/retreat from a "confidence" value [13].
- **Utility scoring** for "which objective, which target, which spot" [1][2][3][24]; a **priority list / HTN** for short sequences (form up, advance, engage, reform) [7][8][9]; a commitment bonus stops flip-flopping [7]. GOAP is overkill here [6][8].
- **Influence/threat maps** at coarse resolution and **position scoring** with cheap candidates first and raycasts last [9][15][16].
- **Fair vision:** players rarely notice real cheating but do complain about AI that seems to know too much; perceived realism tracks perceived competence [27]. So: no hidden knowledge, but sharp play.
- **Human limits** from AlphaStar and OpenAI Five: reaction around 0.2 s, an action-rate cap, one camera/attention focus [32][33]. Mistakes must be the kind a person makes [27].
- **Testing:** bot-vs-bot tournaments with TrueSkill ratings [36], exploiter bots that try to break the main bot [32], automated scenario tests [35], and BotPrize-style blind judging [31].
- **Learned bots:** not realistic for the decision core here; tuning utility weights by search (CMA-ES or a genetic algorithm over headless sims) is the realistic middle ground [35][38].

## 6. Minecraft factors

- **Tick budget:** everything on the server shares 50 ms per tick [40]. Kort's working target is server p95 under 50 ms (SLICE-03). The AI should own a fixed slice: **under 4 ms mean and 8 ms p99 for all bot and troop AI at 160 troops** (the approved spec's numbers), enforced by a time-sliced scheduler.
- **Pathing:** troops are vanilla mobs using `MobNavigation` (weighted A* with a node cap tied to follow range, partial paths, 1-block step, fences and closed doors impassable) [41][42][45]; each mob paths alone, so groups jam. Today's code already wraps it with waypoints at most 16 blocks ahead, bridge and gate routing, and stuck recovery. Plan (as in the approved unit-AI spec's flow fields and group paths): one route per group on a coarse arena grid, troops steer to formation slots and use vanilla A* only for the last few blocks; cap path searches per tick; keep follow range modest [44].
- **Chunks:** troops only tick in entity-ticking chunks [40]; the mod already forces a 3x3 area around each troop every 40 ticks (`GravesAndGlory.java:1241-1247`).
- **Server side:** all decisions run on the server. A debug overlay needs a dev-only packet with each troop's purpose string.
- **Threads:** world reads off the main thread are unsafe; if any planning moves off-thread it must copy a compact snapshot first and apply results next tick [49b]. For this army size, time-slicing on the main thread is enough for 1.0.
- **1.21.1:** relevant changes are API renames, not behaviour: `GoalSelector.setNewGoalRate` removed, path type renames, attribute modifiers keyed by id, `step_height` attribute, Java 21 [50][51][52]; the `generic.` attribute prefix only goes in 1.21.2 [43]. Yarn ends at 26.1, so new AI code should keep Minecraft calls behind a thin adapter [53]. Our AI is mostly our own code over `Tactics`, so the port mostly touches the adapter, not the brain.

## 7. Design

### 7.0 Relation to approved specs
The **unit-AI spec (`docs/superpowers/specs/2026-10-18-unit-ai-design.md`, approved, not built)** already defines the General per bot banner, the council per bot team, doctrines per House, answers, tells, flow fields, group paths, `Orders.issue` (bots order like players) and a budget; its build order (steps 1-10) stays. The **no-routing spec (`2026-10-22-no-routing-design.md`, approved)**: no group of any side breaks; troops do exactly what they are told. This design extends those two: everything below either fills a gap they leave (purpose lines, fair-vision memory, squad step plans, the objectives interface, human limits, personalities, metrics and the blind test) or restates them. Where this report says "commander" read "General and council"; "squad" is the spec's Sergeant plus a step plan. Nothing here changes how a human's troops obey.

### 7.1 Principles
1. Every troop always has a purpose, written as one line: intent / squad step / troop action, for example `Take Mill / form up / waiting for 3`. A debug overlay (`/gg ai purpose`, dev only) shows it above every troop, and the sim logs it.
2. The brain sees only what a person on that side could see: own army, the map, objective states shown on the HUD, and enemies the team has seen, with memory that fades.
3. Difficulty and personality change judgement, speed and attention, never troop stats.
4. Data, not code, for content: new troops, Houses, maps and modes plug in through rows and declarations.
5. Fixed time budget, measured every sim run.

### 7.2 Layer 1: Commander (per bot seat; every 20-40 ticks, staggered between seats)
- **Inputs:** belief map (last-seen enemy groups: position, velocity, strength, time seen; confidence decays; expires after about 30 s), influence grid (8x8-block cells over the arena: own strength, believed enemy strength, ranged threat, objective value; updated every 10 ticks), the mode's objective list, own groups and their state.
- **Objectives come from the mode** through one interface, replacing today's `botGoal`/`botTarget`/`botPlan` special cases: `Objective{id, kind (take, hold, escort, intercept, destroy, survive), position, radius, value now, value trend, side, wanted strength}`. Domination lists points; Convoy lists the wagon, the road ahead and ambush spots; Siege lists stage targets and defend spots; Survival lists the shrine and rise sites; a campaign battle can add scripted objectives.
- **Intents** scored by utility per group: Take(o), Hold(o), Escort(o), Intercept(enemy group), Flank(o via lane), Screen(friendly group), Regroup(rally point), Reserve(trigger). Considerations: objective value, distance/time to reach, believed enemy strength there vs our strength (Lanchester estimate [20b]), threat on the route, group role fit, current commitment.
- **Assignment** (Killzone/Paragon style [9][14]): highest objective first, give it the smallest set of groups that reaches its wanted strength, lower its priority, repeat; keep groups on their current intent unless a new one beats it by a margin (hysteresis). Every group always ends with an intent; a reserve group's intent names its trigger.
- **Economy:** buy to fill the plan's needs and counter what has been seen (counters table `CounterMath`), within the banner; personality biases the mix.
- **Commander actions** a person has: relics and commander abilities scored like intents (value, cooldown, a target cluster from the belief map).

### 7.3 Layer 2: Squad (per group; every 10 ticks)
- Turns an intent into an ordered plan (HTN-lite: preconditions -> steps), chosen by group role (melee line, shield line, ranged, cavalry, support, siege):
  - Approach: route on the influence grid avoiding known threat; formation by terrain width.
  - Form up: stop out of enemy reach and wait for stragglers (cap the wait).
  - Engage by role: melee holds a frontline between the enemy and own ranged [13]; ranged stands at weapon range behind and shoots what the line fights (focus set); cavalry waits on a flank and charges an engaged or exposed ranged group; support stays with the group it serves; siege stands off.
  - Consolidate: on a won fight, reform on the objective.
  - Fall back: the General may order a group to the nearest friends or a rally point (a Move order, as a player would), never to base by reflex. Troops never break on their own.
- **Confidence** (Days Gone [13]): own vs seen enemy strength, losses, kills, decaying. It lives only in the bot's head: four levels bias which order the General gives (charge / attack / hold / fall back). Troops never change behaviour from it (no-routing ruling).
- **No trickling:** new troops gather at a rally point and move as a body once N have gathered or a timer ends.
- **Charge** only when the target is engaged or exposed, in range, and confidence is at least confident; uses the existing cooldown.
- **Focus set:** the squad chooses 1-3 target enemies; troops pick within it.

### 7.4 Layer 3: Troop (movement every tick; decisions every 5-10 ticks, staggered)
- Keep slot unless engaging; target by score: in focus set, can see it (line of sight), can reach it (same bank or bridge cost), threat to friends, low health, already attacked by enough friends (spread), distance.
- Ranged: keep standoff; step back along a free cell when a melee foe closes; never on Hold walk into its own melee.
- Inside its order only: no wounded step-back or other self-willed move (no-routing ruling). Whether troops on Retreat or Move may strike back or raise shields when hit is a rule change for player troops too, so it is a question for Kort, not part of the bot work.
- Abilities by score: Howl only with enemies near; area spells at the densest seen cluster; banners when holding.
- Unreachable target: give up after `unreachableNoticeSeconds` and pick another.

### 7.5 Data-driven contract (what new content provides)
- **Troop row** already gives group, range, speed, health, damage, tags, ability. The brain derives a capability vector (role, reach, mobility, toughness, area damage, heal) from it. New tags only if a troop does something new.
- **Ability row** adds three fields: target kind (self, ally, enemy, area, point), area radius, effect class (damage, heal, buff, debuff, control). The brain scores any ability from these.
- **Mode** implements one method returning its objectives. Nothing else.
- **Map** gets a small annotation list (rally points, lanes, high-ground spots, ambush spots), placed in the map engine editor by Kort when he builds the map, plus automatic data at build: a walkable/cost grid and height per 8x8 cell. Hand-placed points beat automatic chokepoint detection for authored maps [9].
- **House / personality** is a row of weights.

### 7.6 Personalities and difficulty
- **Personalities** (weight rows over the same considerations): Steady (balanced), Aggressor (early fights, charges), Warden (holds what it takes, counter-attacks), Raider (flanks, hits ranged and weak points), Opportunist (punishes mistakes, retreats early). Campaign enemies get named captains with a personality and a line or two of flavour, which the lore lead can write.
- **Difficulty** (same brain, different limits):

| Knob | Easy | Normal | Hard |
|---|---|---|---|
| Commander decision period | 2.0 s | 1.5 s | 1.0 s |
| Reaction delay to a new sighting | 1.0-1.6 s | 0.6-1.0 s | 0.3-0.6 s |
| Groups it can re-order per second (attention) | 1 | 2 | 3 |
| Plan depth (looks ahead to next objective, flanks) | no | partial | yes |
| Judgement noise (picks a near-best option) | high | medium | low |
| Uses relics and commander abilities | rarely | yes | well timed |
| Scouting and memory | short | normal | long |

Today's banner strength per tier (`*_easy`, `*_normal`, `*_hard`) can stay as a separate campaign choice.

### 7.7 Tick budget (estimates to be measured; the approved spec's targets are under 4 ms mean and 8 ms p99 at 160 troops)
| Part (160 troops, the 8-seat maximum) | Cadence | Estimate per tick |
|---|---|---|
| Troop movement and slot steering | every tick, 160 troops | 0.5 ms |
| Troop decisions (target and position scoring with a spatial grid instead of O(n²) scans) | 1/5 of troops per tick | 0.5 ms |
| Squads (about 18 groups) | each every 10 ticks | 0.3 ms |
| Commanders (6) | each every 20-40 ticks | 0.3 ms |
| Influence grid and belief map | every 10 ticks, time-sliced | 0.2 ms |
| Group paths and last-few-block searches | shared node budget (spec: 4,000 nodes per tick, 1 ms cap) | up to 1.0 ms |
| **Total, our AI only** | | **about 3 ms average; 8 ms p99** (vanilla mob ticking is separate and inside the 3.2 ms baseline) |

The scheduler carries unfinished work to the next tick rather than overrun. The sim's existing probe (`GRAVES_BOTSIM_PROBE=1`, per-tick nanoseconds) measures it on every run.

### 7.8 "Can't tell it from a person": the test
1. **Blind clip test (Kort).** 20 clips of 60 s, spectator view of one commander's army in a real match, half driven by a person other than Kort, half by the bot, on the same maps and modes, shuffled. Kort marks each "person" or "bot" and writes one line on what gave it away. Guessing gives 10 of 20. **Fail** at 15 or more correct (a coin does that well only about 2 percent of the time: bots clearly detectable). **Pass** at 13 or fewer. 14 means 20 more clips. Twenty clips cannot prove the bots are indistinguishable, only catch clear tells, so every "what gave it away" note becomes a defect. Human clips come from friends' matches (the 3v3 playtest after the port gives the best supply).
2. **Sim metrics, every run** (targets start from `docs/drafts/bot-quality-targets.md`):

| Metric | Target |
|---|---|
| Troop idle (no order progress, no fight) | no group idle over 20 s; mean under 5 percent |
| Purpose coverage | 100 percent of troop-seconds have a purpose line |
| Objective share | at least 70 percent of army time on or moving to an objective |
| Blind picks | 0 targets chosen without line of sight |
| Trickle index | reinforcements arriving alone under 10 percent |
| Wasted abilities | under 10 percent of casts hit nothing |
| Stuck | 0 troops stuck over 30 s |
| Orders per minute and reaction time | inside the range measured from Kort's own play |
| Win spread | 40-60 percent per House pair, seat-swapped |
| Difficulty ladder | Hard beats Normal 65-85 percent; Normal beats Easy 60-80 percent (proposal; today's Hard-with-flank won only 9 of 20 against Normal, so the reaction and judgement knobs must be shown to open the gap) |
| Version ladder | new bot rated at or above the previous one (TrueSkill) and beats every exploiter bot |

### 7.9 Army size and match economy redesign (Kort, 2026-11-09; not specced yet)
Source: `status/ARMY-ECONOMY-REDESIGN-2026-11-09.md`. A gold pick phase before every round (teammates see each other's picks, enemy picks hidden), gold carried between rounds with a wanted snowball, a soft 20-slot threshold with rising price and slower training past it, mid-round training kept, the same system for bots and the campaign. Today's bot does none of this: it refills a fixed banner in fixed order (`Campaign.java:745-755`).

**How the architecture covers it.** The commander layer gains a **quartermaster** with three decisions, all using the same belief map and utility scoring as the rest:
1. **Pick phase (once per round): what army to buy.** Generate a handful of candidate armies within the rarity caps (doctrine-led: swarm, balanced, elite, counter-pick), score each with a cheap fight estimate (Lanchester-style strength per gold, `CounterMath` counters) against what the bot **believes** the enemy will field: last round's seen army plus its gold estimate, never the hidden picks. It runs once per round outside battle ticks, so it can afford a few milliseconds. With a human teammate, the council reads the teammate's visible picks and fills the gaps (no doubled cavalry, someone brings ranged).
2. **Save or spend across rounds.** A round-value estimate (rounds left, score, gold lead) decides how much to bank; personalities differ (a saver, a spender). This is new: today's bot has no match-level plan.
3. **Mid-round reinforcement.** Train only when the squad plan needs it (a gap in the line, a counter to what was just seen), weigh the rising price past 20 against holding gold for the next round, and send new troops to a rally point as a group (already in milestone 2).
Every purchase writes a purpose line too ("bought 6 pikes: they fielded 8 horse last round").

**Milestones that change** (the plan in section 8 is otherwise kept until Kort answers):
- 1: the metrics add gold left unspent, gold per kill, and buy-versus-enemy fit.
- 2: "reinforcements gather" stays; bot buying stays as today until the economy is specced.
- 3: balance sims add swarm versus elite and the snowball; costs become the main balance lever, so the Domination balance verdict must be re-run on the new economy.
- 4: the commander gains the quartermaster (about +1-2 M tokens).
- 7: personalities and campaign captains get an economic style; campaign difficulty moves from fixed banners to enemy gold budgets.
- 8: the blind test should include pick-phase choices, which players will compare across rounds.

**Risks to raise now.**
- **Server budget is the real risk.** The redesign survey measured 5 to 6 ms per tick for 3v3 bot battles today (more than this report's 3.2 ms baseline, from a different run). Six commanders at the practical ceiling of about 30 slots is roughly 180 troops, and cheap troops push the troop count higher than the slot count. Today's troop target scans are O(n²) and grow with the square of army size; vanilla pathfinding and entity collisions grow at least linearly and are outside our AI's control. The design's spatial grid and capped path budget keep our AI roughly linear, but its 4 ms/8 ms target must be re-set for the hidden safety limit, and **that limit must come from a measured large-army 3v3 on a dedicated server before the price curve is tuned**, not after. Suggested: troops far from any fight think less often (level of detail).
- **The snowball makes good bots harder to measure, not to build.** A small early edge compounds, so win rates swing more and the difficulty ladder and balance gates need more matches per verdict (and the sim must play full multi-round matches, not today's single 300 s round). It also makes a losing bot's comeback logic (when to save, when to all-in) a visible test of "plays like a person".
- **Hidden enemy picks fit the fair-vision rule** and need no cheat; bots must not read the hidden picks during the pick phase.
- **No other blocker.** Bigger armies suit the per-group design better than today's one-order-per-army bot.

**Approved spec (Kort, 2026-11-10):** `docs/superpowers/specs/2026-11-09-army-economy-design.md` on branch `docs/army-economy-spec` (d40f2cfd, not in main), section 5 and the rules it uses. Built after the 1.21.1 port (its build step 6 is the bot work); nothing here is built before then. What it fixes for the bot, and how the design above takes it:
- **Best of 5, survivors carry over** at full health with their levels and Armoury tiers; dead units are bought again. So from round 2 the quartermaster mostly decides what to replace and what to add, and must value a living veteran above a fresh buy. A unit's position in the army order sets its formation row, so the quartermaster also orders the army (shields first, ranged behind).
- **Same gold, same prices, no hidden gold; difficulty changes decision quality only.** Matches section 7.6: Easy buys with more noise and a shorter memory of the enemy's army, Hard counter-picks and plans the save across rounds.
- **45 s pick phase, enemy picks hidden, teammates visible.** The quartermaster decides in well under a second and waits a human-like time before Ready; with a human teammate it reads the live side panel and fills gaps.
- **Slots past 20 queue at round start and train slowly** (`overTrainBase` 1.15 per slot past 20). Buying past 20 is a bet that arrives late, so the squad layer must plan those troops as a reserve rather than as part of the opening line.
- **Disconnect:** a bot takes a missing commander's seat with their gold and army; the takeover must keep the human's army order and not re-buy (today's takeover uses the lobby tier and lane 0, section 4.1).
- **Roles swap each round** in Siege and Convoy, so each bot needs both attack and defence plans per map; already covered by the mode's objective list.
- **Budget note, sharper than above:** teams go up to 4, so up to 8 commanders, and the safety limit starts at 40 slots each. The worst case is far past this report's 160-troop budget. The spec's performance test (20, 30 and 40 slots, 3v3 and 4v4) must measure the AI's share as well, and `hardSlots` should be set with the AI's level-of-detail scheduler running, not without it.
- **Sim harness:** `-PbotSim` must play whole best-of-5 matches with carried gold and armies; the spec's targets (round-1 winner takes the match 60 to 70 percent, a side down 0-2 can still field 16 slots) become bot-sim gates.

## 8. Ranked build plan

Costs are rough estimates for agent work (tokens include one review per change and sims). "Kort sees" is what changes for him.

| # | Milestone | Cost | Kort sees | Release |
|---|---|---|---|---|
| 1 | **Measure and show purpose.** Sim metrics above (idle, objective share, blind picks, trickle, wasted abilities, stuck, cohesion), a "why" log per decision, the dev overlay with purpose lines, a gate script. No gameplay change. | S: about 1-2 M tokens | A page with today's numbers per behaviour; an overlay he can switch on in game | 1.0 |
| 2 | **Fix the worst bot-commander behaviours in today's code.** Per-group targets in Domination (archers stand off, one group guards a taken point); no Retreat order under fire; face the target; no map-wide enemy count for Easy/Normal charge; bot reinforcements gather before moving; Convoy escort clears the wagon. Troop-rule fixes that also touch player troops (archer engage reach on Hold, line of sight, target lock and spread, abilities only with enemies near, strike back on Retreat) go to Kort one by one. Gameplay change: needs Kort's yes. | M: about 3-5 M tokens | The same sims with visibly fewer dumb moments; numbers from milestone 1 drop | 1.0 |
| 3 | **Gate fixes that are balance, not AI** (Kort decides): Gilded vs Graveborn army balance and the Ironhold spawn distance now; Convoy timing only after milestone 2's escort fixes are measured. Re-check after each bot milestone. | S: about 1 M tokens of sims | Domination gate measured seat-swapped; Convoy tuned once | 1.0 |
| 4 | **Commander layer = the approved spec's steps 1-3 and 7-8** (core, grid and fields, movement, General, council) plus this report's objectives interface for all modes, belief map, influence grid, utility intents, group assignment, economy by need, relic use. | L: about 6-10 M tokens | Bots that split up, guard, flank, react to what they saw, and use relics | 1.0 |
| 5 | **Squad layer = the spec's Sergeant (step 4)** plus role plans (approach, form up, engage, consolidate), frontline, focus sets, the General's confidence, timed charges, rally points. | L: about 6-10 M tokens | Battles that look like formed armies: lines, archers behind, cavalry on flanks, orderly retreats | 1.0 |
| 6 | **Troop polish = the spec's Soldier (step 5)** plus position scoring, ranged standoff inside the order, ability scoring, unreachable give-up, spatial grid for scans. | M: about 3-5 M tokens | Fights that look deliberate up close | 1.0 |
| 7 | **Personalities, difficulty knobs, campaign captains.** | M: about 2-4 M tokens | Easy, Normal and Hard feel different for the right reasons; named enemy captains | 1.0 |
| 8 | **Blind test and tournament.** Version ladder, exploiter bots, the 20-clip blind test, fixes from its notes (two rounds). | M: about 2-4 M tokens plus 1 hour of Kort | The "bot or person" score | 1.0 (test after friends' clips exist) |
| 9 | **After the port:** weight tuning by search (CMA-ES over headless sims), group path planning on a snapshot off the main thread or flow fields, adaptive difficulty, calibrating from recorded human commanders, scouting units. | L | Stronger, smoother bots on bigger maps | after 1.21.1 |

Order logic: 1 before anything, because every later step is judged by its numbers. Steps 4-6 follow the approved spec's own build order (core, grid, movement, Sergeant, Soldier, General, council), so movement and troops come before the General, with Kort's checkpoint after the spec's step 6. 2 gives the biggest visible gain for the least work and stays useful as the troop layer of the new design. 3 is cheap and unblocks honest gates. 4 to 6 are the real rebuild; 4 and 5 should be designed together but built in that order.

## 9. Questions for Kort
1. **Start milestone 1 (measure and show purpose)?** No gameplay change; about 1-2 M tokens. (prio 1, 2 min)
2. **Approve milestone 2's bot-commander fixes as gameplay changes** (groups get their own targets, one guards a taken point, no retreat under fire, refills gather, escort clears the wagon)? Troop-rule changes that also affect Kort's own troops come as a separate short list. (prio 2, 5 min)
3. **Convoy:** OK to leave push speed, clock and checkpoint distance alone until the bot escort fixes are measured, then pick one lever? (prio 3, 1 min)
4. **Domination balance:** who owns the Gilded vs Graveborn army balance and the Ironhold spawn distance: the balance lead, or me? (prio 2, 1 min)
5. **Maps:** will you place rally, lane and high-ground markers in the map editor when you build a map (a few minutes per map), or should the bot work them out alone? (prio 3, 2 min)
6. **Design base:** OK that the new bot brain builds on the approved unit-AI spec of 18 October (General, council, doctrines) and keeps the no-routing rule, adding purpose lines, fair memory, squad plans and the bot-or-person test? (prio 2, 3 min)
7. **Blind test:** about one hour later in 1.0 to judge 20 clips "bot or person". OK to plan for it? (prio 3, 1 min)

## 10. Review
One fresh Sonnet 5.5 (high) review of sections 1-9 against the code and the CSVs, 2026-11-09. Findings and what changed:

| # | Finding | Resolution |
|---|---|---|
| 1 | Design clashed with two approved specs: unit AI (2026-10-18: General, council, doctrines) and no routing (2026-10-22); proposed rout, wounded step-back and strike-back on Retreat | Fixed: section 7.0 added; rout, step-back removed; confidence only biases the General's orders; troop-rule changes that touch player troops moved to a separate list for Kort; plan steps 4-6 mapped to the spec's build order |
| 2 | Convoy "arithmetic, no bot can pass" overstated (downhill 1.25, best match 231/262, escort won 10/10 before the 11-02 cut and bot tuning) | Fixed: section 1 and 3 now say timing and bots together; Convoy lever deferred until after milestone 2 |
| 3 | D3 misread: group centre dragged back by trickling refills | Fixed: D3 rewritten; metrics must be per troop |
| 4 | C1 "two archers alone" contradicted by `defenders` 4-10 | Fixed |
| 5 | Domination verdict: bot contribution unsupported; 300 s sim cap; near-duplicate runs; Ironhold is 8 of 8 | Fixed in section 3 |
| 6 | "0 stuck" false for Domination | Fixed |
| 7 | Sim numbers (D1, D2, D5) | Fixed; 1.9 is per army per match as stated |
| 8 | Wrong citations (OrderRules:113, Morale:34-51, GravesAndGlory:1239, Tactics:335/543, [flow]) | Fixed |
| 9 | "Hard plays like Normal" overstated | Fixed |
| 10 | Tick budget used 115 troops, ignored the 3.2 ms baseline and the spec's budget | Fixed: 160 troops, spec's 4 ms mean / 8 ms p99, baseline cited |
| 11 | Blind test: Kort would know his own clips; pass/fail band | Fixed: clips from other people; fail at 15+, pass at 13 or fewer, 14 reruns |
| 12 | Hard-over-Normal 65-85% unsupported | Marked as proposal with today's 9-of-20 evidence |

Not re-reviewed after fixes (one review round, per policy). Appendices A-C are the working notes behind sections 4-6 and were not corrected line by line; where they disagree with sections 1-9, sections 1-9 win.

---

## Appendix A. Unit (troop) AI audit

## Per-unit (troop) AI audit: Graves and Glory, branch ai/lead

Read-only audit of `C:\Users\kortt\Melty\bb-ai\src\main\java\gg\gravesandglory\`. Paths are relative to that folder. Nothing here was run in game.
Every "dumb" note is **from code, unverified** unless it says otherwise. "Confirmed" means the code path is unambiguous. It does not mean the behaviour was watched.

### 0. Frame: how a troop is driven

- Troops are vanilla mobs (husk, skeleton, pillager, wolf, ...). Their only vanilla goal is `SwimGoal` (GravesAndGlory.java:453-524). They have no vanilla targeting or melee goals. All behaviour comes from `Battle.unitTick` = `Abilities.unitTick` -> `Tactics.tick` -> `FormationRules.tick` (GravesAndGlory.java:1249). This runs for **every unit every tick** (GravesAndGlory.java:838).
- `Tactics.tick` does a few cheap things every tick: hop, stun stance, shield brace and the wall "stretch". The full decision runs only on the unit's **10-tick beat**: `time%10 == slot%10` (Tactics.java:243).
- The order a troop obeys is `Battle.assignment(u)` (GravesAndGlory.java:989). That is a custom-group order if one is newer, else the group's stored order. A bot's orders are squad-level (`s.order/s.target`). They arrive as a **fresh** `Assignment` built on every call (GravesAndGlory.java:993), so for bots `hunt=false`, `chargeDir=null`, `enemy=-1` and `holdSlots=false` always.
- Army size: `rules.squadSize` = 20 slot weight per army. Up to 8 players means about 160 ticking troops; a 1v1 bot sim has about 40. `automaticReinforcements` = false (Spec RulesRow "domination").

Spec values read by unit AI (Spec.java, generated from `sheets/design.json`):
- NAVIGATION: waterPenalty 16, searchRange (follow range) 40, waypointDistance 16, stuckSeconds 3, repathTicks 20, rangedRetreatDistance 4, rangedStandoff 0.65, bridgeEntryInset 2, unreachableNoticeSeconds 10 (unused by Tactics), arrivalTolerance 0.35, preciseRange 2.
- ORDERS (engage, engageRadius, moveMultiplier):
  - follow: true, 10, 1.0
  - move: false, 0, 1.0
  - attack: true, 24, 1.0
  - hold: true, 10, 1.0
  - charge: true, 32, 1.25
  - retreat: false, 0, 1.15
  - The sheet's `hunt` column is not read anywhere. `Assignment.hunt` comes from OrderRules.java:110.
- FORMATIONS (columns, spacing, wedge, moveMul, arrowBlock, shove):
  - block: 5, 1.8, no wedge, 0.85, 0.75, 0
  - line: 10, 1.8, no wedge, 1, 0, 0
  - wedge: 5, 1.8, wedge, 1, 0, 0.6
  - loose: 5, 3.2, no wedge, 1, 0, 0
- COMBATDISCIPLINE: targetMemoryTicks 40, rangedObstructionStep 4, spellWindupTicks 20, spellRadius 2.5, enemyKnowledge "visible_only" (**not enforced by Tactics**), healRadius 10, abilityCooldownTicks 100, lastStandShare 0.25 / radius 16, arriveRadius 4, retreatArriveRadius 4, shieldWallCell 0.7, crawlShare 0.33, fireMemoryTicks 60, meleeReachGrace 1.0, chargeRunBlocks 32, chargeCooldownTicks 400, chargeBonusMultiplier 1.3, chargeKnockback 0.8, flankArcDegrees 110.
- SCOUTING: range 36.
- Troop reach (TroopsRow.range):
  - melee 2.2-3.0
  - archers 20-30 (Archer 22, Longbow 30, Bone Archer 25, Stalker 29)
  - Harpooner 16
  - mages 14-20
  - Ballista 34, Mortar 36
- Role depth behind the anchor (BattleMath.java:558): ranged 5, siege 7, support 9, other groups 0.

### 1. Decision points

Freq: "beat" = every 10 ticks per unit; "tick" = every tick per unit.

| Decision point | file:line | What it decides | Inputs / knowledge | Freq | Where it looks dumb (from code, unverified) |
|---|---|---|---|---|---|
| Beat gate | Tactics.java:243 | Full AI only on slot's beat; off-beat runs brace, shieldStance, stretch | slot%10 | tick | Up to a 0.5 s reaction lag. Neighbours with different slots react on different ticks, so a line starts moving ragged. |
| Down / stun freeze | Tactics.java:242, 322 | Undying King down: no move or act. Stunned: no attack (still moves) | downUntil, stunnedUntil | tick | A stunned troop keeps walking and turning. Only its attack waits. |
| Arrival latch | Tactics.java:35-43, 248 | Retreat/Move group "arrived" once its centre is within 4 of its slot centre, then fights as Hold | group centre vs centre of all slots | beat until latched | Until it arrives, a Retreat/Move troop **ignores enemies even when struck** (doc at Tactics.java:32). Stragglers are latched by the group centre, so one troop can be far off and still "arrived". |
| Player Retreat -> Follow | Tactics.java:250-252 | Retreat target tracks the commander live; becomes Follow within 4 | commander pos | beat | - |
| Player Charge -> Attack | Tactics.java:253, 662 | Charge turns into hunting Attack at the end point or on the first landed blow | group centre, landed blow | beat / on hit | One troop's first hit flips the **whole group** to map-wide hunt (`become` at Tactics.java:61-64). The formation dissolves at once. |
| Order row / hold flag | Tactics.java:255-266 | Order speed, engage radius, hold, holdSiege, hunt, dash/volley/lane, slotted | assignment | beat | - |
| **Engage radius** | Tactics.java:264, 235 | How far from **itself** a troop picks targets: order.engageRadius (Hold/Follow 10, Attack 24, Charge 32). Siege gets at least its reach | order row | beat | **Archers (reach 22-30) on Hold or Follow only take enemies within 10 blocks of themselves.** Bot Hold never sets `holdSlots` (OrderRules.java:113 is player-only), so the reach widening at Tactics.java:268 never applies to bots. Bot archers stand idle while enemy archers (reach 22-30) shoot them from 11-25 blocks. Confirmed in code. |
| Leash | Tactics.java:134-137 | Hold: target must be within engage of the **anchor** (a.target). Other orders: no anchor leash. Player Formation: melee only within reach of own slot | anchor, slot | beat, per candidate | Hold melee chase an enemy inside 10 of the anchor, drop it the moment it steps out, and walk back. That gives yo-yo at the leash edge. Follow has no leash to the commander, so troops chase up to 10 (lock to ~11.8) from themselves. |
| Locked-target keep | Tactics.java:270, 301 | Keep the last target while lock (40 ticks) is live, it is inside 1.18x engage, outside minRange, inside the leash and (hunt/volley) visible | lockedTarget, lockUntil | beat | `lockUntil` is only renewed when the target **changes** (Tactics.java:301). After 40 ticks on one target the unit re-picks the nearest every beat. It flips whenever another enemy is marginally nearer, so melee units walking between two enemies can dither. |
| Explicit enemy | Tactics.java:271 | Player-clicked enemy overrides, if `canSee` | a.enemy, LOS | beat | Also overwrites `a.target` (the order anchor) with the enemy position every beat. Once LOS breaks the unit silently reverts to nearest-target. Bots never use it. |
| **Target scan** | Tactics.java:274-286 | Nearest living enemy troop, then commander, within engage and leash. LOS is required **only** for player hunt or volley | all units' positions, all players | beat, O(n) | **Wall-hack:** melee and archers on bot Attack/Hold/Follow/Charge pick through walls, hills and buildings. Pure nearest: no weighting by health, threat, counter tags, reachability (river) or what allies already target. |
| Taunt | Tactics.java:287 | GearCombat.taunter commander overrides | gear | beat | - |
| Structures | Tactics.java:288-294 | Enemy supply banner only when no troop or commander qualifies | structures | beat | Fine. |
| Hunt fallback | Tactics.java:295-300 | Player Attack with nothing visible within 36: nearest enemy **anywhere on the map** | global positions | beat | Map-wide omniscience (no fog). The comment at 333 admits it "walks at it, not around the obstruction". |
| Shield raise | Tactics.java:305-309, 163-170, BattleMath.java:656-660 | Hold: up with enemy picked or sighted. Others: up only under archer fire. Never on Charge or Retreat. Down when enemy is in reach | underFire (squad memory 60 t), sighted (nearest only + 1 LOS) | beat + tick | **Bot Retreat never raises shields** (raiseShield false for "retreat"; only a player's Retreat maps to follow at Tactics.java:140). It retreats under arrow fire with shields down. Shield drops exactly when the enemy is in reach (to swing), so a wall man in melee is never braced. |
| Crawl | Tactics.java:155-158 | Shield up + under fire + not Hold: speed x0.33 | underFire | tick | A whole Attack column crawls at a third of its speed for 3 s after any archer shot, even far from the archers. |
| Stance / aim | Tactics.java:83-90, 311-312 | Visual stance and aim pitch | LOS | beat | Cosmetic. |
| Abilities beat | Tactics.java:313, Abilities.java:164-177 | mend / blight burst / aura / mist / undertow on the beat | see Abilities rows below | beat | - |
| Ranged back-off | Tactics.java:314, 319, 340, 399-402 | Ranged troop backs 5 blocks straight away if any enemy within max(4, minRange). Not on Hold or perch | nearest enemy | beat | Straight-line flee: no check for walls, river or allies behind. It goes into corners, which feeds stuck recovery. It backs and shoots in the same beat. Never on Hold, so bot-held archers stand and shoot point-blank. A Mortar on Hold with enemies inside its min range just stands (Tactics.java:340). |
| Attack fire gate | Tactics.java:318, 322-328 | In reach + `canSee` -> stop and attack when cooldown is ready and no blow is pending | LOS at fire time | beat | Arrows are **guaranteed homing hits** (no-clip, steered to the target: Projectiles.java:178-181, 245-251). LOS is checked only at release, so a shot passes through walls or allies afterwards and cannot be dodged. |
| Melee in reach but no LOS | Tactics.java:318 -> 331-337 | Falls through to walking at the enemy | - | beat | Melee whose target is behind a 1-block wall walk into the wall until stuck recovery fires. |
| Ranged unseen target | Tactics.java:334 | Steps 4 blocks perpendicular (side by slot parity) every beat | own pos, enemy dir | beat | Because target choice ignores LOS (non-hunt), an archer locked on an enemy behind a house **circle-strafes** round it indefinitely. It ignores visible enemies that are farther away. |
| Melee approach | Tactics.java:335 | Goal = enemy + 1.6-block golden-angle offset by slot | slot | beat | 1.6 blocks is less than melee reach (2.8-3). Many melee converge on the same nearest enemy and jam, which triggers stuck sidesteps and jumps. |
| Ranged standoff | Tactics.java:336 | Walk to 0.65x reach from the target | reach | beat | Fine. |
| Post / volley | Tactics.java:320, 329-330 | Perched or Charging ranged walk to slot, never chase | perch | beat | - |
| Idle -> slot | Tactics.java:341-343, 347-352 | No enemy: walk to the formation slot, face the order yaw, settle | Battle.formation | beat | - |
| Wall last stretch | Tactics.java:185-203 | Shield wall on Hold slides to its slot without turning, with stuck release after 20 t | velocity set directly | tick | Fine (designed). It is a direct velocity write. |
| Settle / hop | Tactics.java:363-381 | Velocity push or jump onto a slot 1 block up | StepHop.onRise | beat / tick | - |
| Wedge shove | FormationRules.java:139-160, Tactics.java:344 | Wedge point/flank push enemies sideways at 0.6 b/t | O(n) units | beat (walking Hold) | Only while **walking** on Hold. A static wedge pushes nothing. |
| Shield square | FormationRules.java:68-87, 172-192 | Block on Hold with 4+ shields and enemies in 3 or more quadrants within 36 (no LOS) -> square | all enemies, no LOS | cached 10 t | Can trigger from enemies unseen behind terrain up to 36 away. The wall reshapes for a threat it cannot know about. |
| Formation slot | GravesAndGlory.java:1024-1040 | Slot = layout around a.target by group/role depth, held places | all squad units | per call (many) | Slots ignore terrain. Ranged sit 5 behind the anchor regardless of hills, LOS or river. |
| Route: bridge | Tactics.java:404-424 | Different river side -> nearest bridge by entry+exit distance | Spec STRUCTURES | every walk | Target choice ignores the river, so the nearest straight-line enemy across water makes a melee troop walk to a far bridge while a reachable enemy stands near. |
| Route: house gate | Tactics.java:426-432 | Goals in walled yards route via the gate | HouseRoute | every walk | - |
| Walk: precise | Tactics.java:538-540 | Within 2 blocks on open, level ground: MoveControl.moveTo, no path | block states | beat | - |
| Walk: waypoint | Tactics.java:541-542, 568-577 | Straight-line waypoint at most 16 ahead, snapped out of river (+-11) and solid blocks (search r<=5) | Arena.ground | beat | A waypoint straight through a building gets snapped to some open cell within 5. That can be on the wrong side, so the unit wobbles. |
| Repath | Tactics.java:579-581 | `startMovingTo` when nextPath passed (20 t), goal moved >3, or nav idle | - | beat | Fine. |
| Stuck detect | Tactics.java:543-546, 515-521 | stuckTicks +10 per walking beat that moved <0.17; >60 -> stuck. Pinned: 100 t in a 1-block patch | own pos | beat | Counts crowd blocking by friends as stuck. |
| Step hop | Tactics.java:450-466, StepHop.java:16 | Teleports the unit onto an adjacent 1-up cell | block probes | when stuck | A **visible teleport** (refreshPositionAndAngles). |
| Sidestep / detour | Tactics.java:548-564, 491-510 | Sidestep 4 fwd + 3 side + jump, or full A* detour (follow range 160, 1 search per tick server-wide, 300 t retry) | Wedge | when stuck | Hopping sidestep in a crowd. |
| Swing / land | Tactics.java:627-663 | Melee blow queued to the impact frame; lands if within reach + 1.0 | pending strikes | beat / tick | Fine. |
| Morale | Morale.java:34-51 | "Last stand" when a group is <=25% health with an enemy within 16 | group health | 20 t | **Cosmetic only.** Read by the HUD/feed (GravesAndGlory.java:1372, BattleHud.java:285). No behaviour change: no rout, no retreat, no bonus. |
| Mend target | Abilities.java:190-212 | Lowest health-fraction ally in LOS within 10, on cooldown | ally health, LOS | beat | Heals a 19/20 troop at the back over a 5/40 one if fractions say so. Ignores danger. Fine otherwise. |
| Blight / mist cast | Abilities.java:280-292, 229-258 | Spell at the **current target's position**, bursts 20 t later | target pos | beat (via attack) | Aims at the nearest enemy, not the densest cluster. The centre is fixed, so a moving target walks out of a 2.5 radius in 20 t. |
| Howl | Abilities.java:740-756 | Fires whenever off cooldown | none | beat | Fires with no enemy anywhere (spawn, marching). It wastes the buff. |
| Oath Banner | Abilities.java:713-725 | Plants when the troop's order **changes to** Hold | order transition | beat | A round that starts on Hold never plants. A bot that stays on Hold never re-plants after cooldown. |
| On-hit rows | Abilities.java:352-385 | pierce, cleave, slows, bash, slam, pounce, mark: automatic on hit | - | on hit | No timing choice. |
| Commander relics | Commander.java:110, 131-145, GravesAndGlory.java:947 | Only a human's `relic:<slot>` command activates one | - | - | **Bot commanders never use relics** (`player()` returns null for bots). Confirmed. |

### 2. Ten likely "a person would never do that" behaviours

How to reproduce: `tools/balance_sim.sh <name> <port> -PbotSim=N -PsimMaps=... -PsimDawn=normal -PsimDusk=normal -PsimBanner0=... -PsimBanner1=...`, plus a spectating client to watch. The sim also prints `Tactics.sidesteps/detours/stepHops` (Tactics.java:468).

1. **Archers holding fire on Hold or Follow.** Engage radius 10 from self (Tactics.java:264) vs reach 22-30. Bot Hold never sets holdSlots (OrderRules.java:113). Repro: Gilded (Archer/Longbow) vs Graveborn bot. Watch a bot on Hold: its archers take arrows from Bone Archers at 15-25 blocks and do not shoot back.
2. **Shooting at or walking toward enemies they cannot see.** No LOS in non-hunt target choice (Tactics.java:274-279). Ranged then circle-strafe 4 blocks per beat (Tactics.java:334). Repro: a map with houses or chapel walls. Look for archers orbiting a building while visible enemies are farther away.
3. **Melee jamming against walls.** Melee in reach but no LOS fall through to walk (Tactics.java:318, 331). Repro: fights at supply-house yard walls. Look for troops pressed on a wall, then sidestep jumps or step-hop teleports.
4. **Retreating or moving straight through enemies without fighting back.** Engage false until arrival (Tactics.java:35-43). Repro: a bot Retreat to base or Muster Move. Look for troops hit in the back for the whole walk, never turning.
5. **Bot Retreat with shields down under arrow fire.** BattleMath.java:657, Tactics.java:140 (player-only fix). Repro: a shield infantry bot (Gilded Squire) retreating from archers.
6. **Pointless river detours.** Nearest straight-line target across the river (Tactics.java:274) plus a bridge route (Tactics.java:404-421). Repro: a river map, melee near the bank. Look for melee marching to a bridge while enemies on their own bank are 25+ blocks away but nearer by path.
7. **Melee dog-pile and crowd hopping.** Everyone takes the nearest enemy (no spread or assignment). The 1.6-block offset ring (Tactics.java:335) is jammed by bodies, which are counted as stuck (Tactics.java:543), so they sidestep with a jump (Tactics.java:561-563). Repro: any 20v20 brawl. Look for jumping troops at the back of a crowd and rising `sidesteps`.
8. **Yo-yo at the Hold leash.** Chase while the enemy is within 10 of the anchor, return when it steps out (Tactics.java:136, 331, 342). Repro: a bot Hold on a capture point vs a kiting or patrolling enemy. Look for melee shuttling out and back.
9. **Target dithering after 40 ticks.** The lock is not renewed for the same target (Tactics.java:301). The nearest-only re-pick each beat flips between near-equidistant enemies. Repro: a melee unit between two enemies about equally far away. Watch the look direction and path flips.
10. **Whole-army slowdown from one arrow.** Crawl x0.33 for 60 ticks on any non-Hold order once the group is shot (Tactics.java:121-124, 155-158). Repro: Ironclad/Gilded shield infantry on Attack vs any archer bot. The column slows to a third far from the fight.

Honourable mentions:
- Howl fired with no enemy around (Abilities.java:740).
- Bot never uses relics (Commander.java:110).
- A Mortar on Hold stands helpless inside its min range (Tactics.java:340).
- A spell lands on empty ground after a 20-tick windup (Abilities.java:285).

### 3. Per-tick cost notes (from code, unverified; nothing profiled)

- **Every tick, every unit** (GravesAndGlory.java:838 -> 1249):
  - `Abilities.unitTick` (cheap).
  - The Tactics.tick prefix: hop, stunStance, plus on off-beats `brace` (2 map lookups, plus a fresh `Assignment` allocation per call for bots at GravesAndGlory.java:993) and `stretch` (for Hold wall troops without an enemy: `clear` samples a 0.25-block line, 2 getBlockState per cell, plus `route` -> `houseGate` per house).
  - `FormationRules.tick`.
  - `assignment(u)` is called several times per tick per unit and allocates for squad-level orders.
- **Battle.formation(u) is O(n + g log g)** (GravesAndGlory.java:1029-1031: scans all units, sorts the group, holdLayout, heldPlace). It is called by:
  - FormationRules.tick every 20 t per unit, which gives O(n^2)/20 per tick
  - toSlot every idle beat
  - show() for wall troops every beat
  - stretch when place is null
  - **slotCentre**, which calls it for every group member (Tactics.java:45). `arrived()` (unlatched Retreat/Move, every beat) and `groupWithin()` (player Retreat/Charge, every beat) are therefore O(g·n) per unit and **O(g^2·n) per group per beat**. A 20-troop Retreat in a 160-unit battle is about 64k inner iterations every 10 ticks. This is the most likely hotspot.
- **O(n^2) scans:**
  - target scan (Tactics.java:274) over all units on each unit's beat: n^2/10 per tick
  - hunt fallback (Tactics.java:297)
  - `sighted` (Tactics.java:147, shield troops on Hold)
  - Wedge `shove` (FormationRules.java:145)
  - `surrounded` (cached 10 t per group)
  - Morale.tick every 20 t (squads x 3 groups x 2 scans)
  - mend `mendTarget` (all units, with a `canSee` raycast **per hurt ally in radius** because the filter runs before the min, Abilities.java:210)
  - howl, lend and burst loops
  - `pending()` scans all pending strikes (Tactics.java:619)
- **Raycasts per beat:** up to about 3 `canSee` per unit (Tactics.java:271, 311, 317). Hunt and volley add one per nearer candidate (worst case O(n)).
- **Pathfinding:** vanilla `MobNavigation.startMovingTo` toward a waypoint at most 16 ahead, with follow range 40 (about 640-node A* cap, Tactics.java:488 doc). It is only called from `walk` on beats, so at most once per 10 t per unit. Normally every 20 t (repathTicks), sooner if the waypoint moved >3 blocks or nav is idle (Tactics.java:579). Climb paths to tower tops go up to every 20 t inside 24 blocks (Tactics.java:526-529). Stuck detours use a separate A* with follow range 160, **max 1 per tick server-wide** (Wedge.java:28), with a 300 t retry on a miss. Worst case at 160 units: about 16 vanilla path searches per tick.
- Chunk forcing of a 3x3 area per unit every 40 t (GravesAndGlory.java:1239-1247): a set rebuild of O(n).

### 4. Gaps: what a good tactics AI would need and this code lacks

- **Focus fire / target allocation:** none. Pure nearest per unit. Only player-clicked targets (Tactics.java:271) and the Pack Hunters count (`Houses.countTargeters`, read only for damage) exist. There is no low-health or high-value preference, no counter-aware choice (CounterMath tags are known but unused in targeting), and no spreading across enemies.
- **Perception model:** `enemyKnowledge: visible_only` exists in the sheet but Tactics ignores it. Troops know every enemy position within 10-36 (map-wide on hunt). There is no memory or last-seen position at the unit level (team `sightings` exist, GravesAndGlory.java:1328, but Tactics does not read them).
- **Kiting:** only a straight 5-block back-off (Tactics.java:399), never on Hold, with no terrain check and no "shoot then step" rhythm. Arrows auto-hit, so movement never avoids damage.
- **Protecting ranged / screening:** slots put ranged 5 behind (BattleMath.java:559), but no melee unit intercepts enemies diving the archers. Melee just target their own nearest.
- **Unit-level flanking:** flank damage exists (CounterMath C16, flankArc 110) but no troop ever moves to a flank. The melee offset is a golden-angle ring (Tactics.java:335).
- **Wounded retreat / morale behaviour:** none. Last stand is cosmetic (Morale.java:42-48). There is no rout, no pulling back of low-health units, and menders do not escort.
- **Terrain / high ground / cover:** slots and targets ignore elevation, cover props (PropGameplay cover affects damage only) and LOS lanes. The ambush stance (noSky) is passive.
- **Ability timing:** all troop abilities are automatic on a timer or trigger. Howl fires with no enemies, blight targets the nearest instead of a cluster, and Oath Banner only fires on a Hold transition. Bot commanders never use relics.
- **Reachability-aware targeting:** the river and bridges are only considered after the target is chosen.
- **Group cohesion while engaging:** once an engaging order is active, each troop chases independently with no rally to its formation. The Charge -> Attack flip dissolves the whole group on one hit.
- **Self-defence on non-engaging orders:** Retreat/Move troops never strike back, even when struck.
- **Unreachable-target give-up:** `unreachableNoticeSeconds` is unused. A troop keeps one target it cannot reach (only the 40 t lock expiry and the nearest re-pick change it).

---

## Appendix B. Commander, modes, tiers, campaign and harness audit

## Bot AI audit: modes, helpers, tiers, campaign, harness (read-only, 2026-11-09)

Checkout `C:\Users\kortt\Melty\bb-ai` (ai/lead). Paths are relative to `src/main/java/gg/gravesandglory/` unless they start with `sheets/`, `tools/`, `test-tools/`, `docs/` or `work/`.
"(code)" means it was read from the code and not observed in a run. "(data)" means it was seen in a CSV or report.
Only one file has local changes: `test-tools/.../BotSimulation.java`, which adds the Domination trace (see section 4). Nothing else in the checkout is edited.

### 1. Decision points

Every bot army runs the same pipeline in `Campaign.tick` (Campaign.java:740-758). It skips the army while a dropped player's grace period runs (:742, MatchRules.java:23). It decides every `decisionTicks` (:743) and buys every 100 ticks (:739). It then calls `botDecide` (:771-819). Mode hooks are consulted in this order: `botRetreats` (:780), `botGoal` (:801), `botTarget` (:812), `botPlan` (:816).

| Decision point | file:line | Decides | Information used (cheating?) | Cadence | Looks dumb because |
|---|---|---|---|---|---|
| Retreat / rally | Campaign.java:780-782 | The whole army retreats to its field base when living <= retreatShare x banner units, and rallies at >= rallyShare | Its own living count only | Each decision | The whole army walks to base (:799). Rallying needs bought troops, and while it retreats the deploy target is the base, so refills spawn at the barracks (GravesAndGlory.java:1139-1144). (code) In ai-dom-191818 there were 0 retreats in 10 matches (data). |
| Point choice, Easy/Normal/Hard | Campaign.java:791-794 | The lane point (`laneTarget`), then the lowest of: distance + 140 if owned + 90 if off-lane - 55 if contested | Point owner and contested flag, which the HUD also shows. No enemy positions | Each decision | Hard now picks exactly like Normal (flank off, sheets/design.json botCommand hard `flankLeastDefended:false`, commit e8da4e68). No army ever garrisons, because hold only happens when every point is owned (:802). After a capture, the +140 pushes armies to the nearest unowned point, so armies tend to converge on the same point and leave their captures empty. (code) |
| Flank / garrison (dead) | Campaign.java:783-790, 824-837; BattleMath.java:588, 607 | Least-defended point, keeping a guard on held points | `b.sightings` (fair: line of sight) | - | No tier turns it on. `defenderWeight` 25 (Hard) is never read. |
| Order and formation | Campaign.java:801-802 | `attack` unless the chosen point is owned (then `hold`). `block` if living >= 8, else `loose` | - | Each decision | Never line, wedge, shieldwall or custom groups. `CustomGroups` refuses bots (CustomGroups.java:37). All groups (ranged, support, siege) get one target, so archers walk onto the point with the infantry. Domination has no `botPlan`. (code) |
| Timed charge, Hard | Campaign.java:803-808; BattleMath.java:649 | Charge when living >= seen-near x chargeRatio and the nearest seen enemy is <= chargeDistance (18) | Team sightings (fair) | Each decision, gated by the Charge cooldown (:814, OrderRules) | Counts enemies within 36 blocks but needs one within 18. (code) |
| Charge, Easy/Normal | Campaign.java:809 | Charge when living >= ALL living enemies on the map x chargeRatio | Omniscient enemy count | Each decision | With 3 armies a side (about 18 vs 54) this almost never fires until the enemy is nearly wiped out. (code) |
| Facing | Campaign.java:815 | yaw -90 for Dawn, 90 for Dusk | - | Each decision | The army always faces the enemy's base, even toward a flank point, while retreating, or in Convoy. (code) |
| Economy: troops | Campaign.java:745-755 | Re-buys toward its banner's counts, capped by squadSize | Own gold | 5 s | A wiped army waits 240 ticks (12 s) before it buys again (:747). (code) |
| Economy: gear | Veterans.java:399-419; LevelMath.java:97-117 | Buys gear tiers up to gearMaxTier, keeping gearReserveTroops x the cheapest troop's price; priority banner, role or open | Own state | 5 s | Fine. |
| Takeover of a dropped player | GravesAndGlory.java:918; Campaign.java:742 | After disconnectGraceSeconds a bot takes the army with `attack` | - | - | It takes the lobby tier `campaign.difficulty`, even in a campaign battle, and keeps lane 0 because a human's `lane` is never set (GravesAndGlory.java:215). (code) |
| Domination `botGoal`, `botTarget`, `botPlan` | BattleMode.java:63, 110, 113 (defaults) | Nothing: the point plan above runs | - | - | `DominationMode` (DominationMode.java:8-25) only scores. |
| Convoy `botGoal` | ConvoyMode.java:337-354 | Roles from `ModeMath.convoyRoles` (ModeMath.java:52): escort pushers aim 3 blocks ahead of the wagon, screens 20 ahead. Defence raiders go to the wagon, blockers min(d+10, next stop) ahead. Beyond chaseRange (40/80/120) a group regroups on the wagon | Wagon position (public) | Each decision | 3v3, raidShare 0.5 (:321): escort 1 pusher and 2 screens, defence 2 raiders and 1 blocker. Pushers are on Attack (engage radius 24, sheets `orders`), so they chase off the road. A single defender within 6 blocks stops the wagon (ModeMath.java:104-105). The defence never retreats (:330) and forward-deploys at the next checkpoint (:296-301), right in the wagon's path. (code) |
| Convoy retreat | ConvoyMode.java:330 | Only the escort may fall back, to (-283,0) | - | - | As the wagon advances, the escort's walk home grows to 500+ blocks. (code) |
| Siege `botTarget` | SiegeMode.java:212-221 | Stage target: yard, gate face or behind the passage, keep | Stage, gate HP | Each decision | See docs/reports/siege-bots-2026-11.md. Wards are never targeted. |
| Siege `botPlan` | SiegeMode.java:265-289 | Per-group orders at the gate: infantry and cavalry hit the gate, siege troops stand off 25, ranged stand off 12; defender ranged go to tower posts (Normal and Hard, :89) | Stage | Each decision | Defenders' howToSide is "defend", so botDecide forces `hold` (Campaign.java:813) and botPlan then flips it to `attack` (:269-271). Two hooks fight over the order. (code) |
| Siege `botRetreats` | SiegeMode.java:292 | Defenders never retreat after stage 0 | - | - | - |
| Survival `botTarget`/`botGoal` | SurvivalMode.java:131-132, 109 | Allies stand on the shrine on Hold (10-block leash) | - | Each decision | They never go to rise sites. The director army (:165) has a fixed order (attack the shrine) and no decisions. (code) |
| `howToSide` | BattleMode.java:107; ConvoyMode.java:137; SiegeMode.java:140; SurvivalMode.java:109 | attack or defend. "defend" turns a botTarget into `hold` (Campaign.java:813) | - | - | - |
| Abilities and relics | Commander.java:110, 126 | Relics and commander abilities are player-only. A bot squad has no commander | - | - | Bots use no active abilities. Troop abilities fire automatically per unit (`Abilities.unitTick`, GravesAndGlory.java:1249). |

Sightings: `Battle.updateSight` (GravesAndGlory.java:1328-1334) runs inside `sendState` every 10 ticks (:858, :1346), even when no player is online. An enemy is "seen" when any living friendly troop or seated player is within 36 blocks with `canSee` (line of sight). Memory lasts 10 s (sheets `scouting`: range 36, memorySeconds 10).
- Hard reads sightings only for its timed charge.
- Easy and Normal never read them. Their only enemy information is the omniscient count used for charge.
- No tier sees more than the HUD for point choice.
- The Campaign.java:770 doc ("Hard reads only what its team has seen ... holds the points it owns") is stale since flank was turned off.

BattleMath helpers:
- `laneTarget` (BattleMath.java:644-647): Dawn's lane i goes to point i, Dusk's to points-1-i.
- `leastDefended` (:588-599): minimises distance + defenders x weight over points the team does not own.
- `garrison` (:607-627): pairs each owned point with its nearest army, guards first.
- `timedCharge` (:649-651): living >= near x ratio, and the nearest enemy within distance when distance > 0.

### 2. Difficulty tiers (sheets/design.json `botCommand`, Spec.java:46)

| Column | easy | normal | hard | Read at |
|---|---|---|---|---|
| retreatShare | 0.35 | 0.25 | 0.20 | Campaign.java:781 |
| rallyShare | 0.70 | 0.60 | 0.55 | :782 |
| chargeRatio | 2.5 | 1.5 | 1.3 | :808-809 |
| decisionTicks | 160 (8 s) | 100 (5 s) | 60 (3 s) | :743 |
| flankLeastDefended | false | false | false | :783 (dead for all tiers) |
| defenderWeight | 0 | 0 | 25 | :790 only (unreached) |
| archerThreatCount | 3 | 3 | 3 | never read (grep finds no use outside Spec.java) |
| archerThreatRange | 30 | 30 | 30 | never read |
| chargeDistance | 0 | 0 | 18 | :803 (0 means omniscient charge) |
| gearMaxTier | 2 | 3 | 3 | LevelMath.java:101 |
| gearReserveTroops | 8 | 5 | 2 | LevelMath.java:100 |
| gearPriority | banner | role | open | LevelMath.java:99, 104 |
| waveScale | 0.75 | 1.0 | 1.25 | SurvivalMode.java:276 |
| waveTraitTier | 0 | 1 | 2 | SurvivalMode.java:137 |

What actually differs between tiers:
- Decision speed.
- Retreat and rally thresholds.
- Charge rule.
- Gear buying.
- Survival wave size.
- The banner row: botBanners `*_easy` has no Epic, `*_normal` has a 2-slot Epic (mage, hexer, seer, mortar), `*_hard` has a 4-slot Legendary plus an Epic (sheets `botBanners`; Spec.java:132).

There are no stat multipliers: bots fight at progression 1 (Campaign.java:411-412). Bot banners are chosen by tier and lane over the fieldable Houses (Campaign.java:381-388, 403-407).

History: Hard flank was turned off by Kort on 2026-10-18 after it lost to Normal (docs/archive/QUESTIONS-FOR-KORT-before-optimization-2026-10-25.md:149). The tier ladder is parked.

### 3. Campaign enemy setup (chapter 1, "The Oath", sheets `campaignBattles`)

Only oath-1 to oath-6 exist. The March rows were retired (commit c7b35aaf). The `kill_unit` boss path (Campaign.java:390-397) is dead with the current sheet.

How seating works:
- The player is always team 0 (Campaign.java:330).
- Bots fill each side to `size` with `lane = i % 3` (:351). Every bot takes the battle's difficulty, except allies, who take their ally row's tier (:351).
- Enemy banners come from `campaignBattles.enemies` by lane (:383, MarchMath.java:139).
- Ally banners come from `allies` by lane (:398-401).

| Battle | Map | Mode | Size | Tier | Allies (team 0 bots) | Enemies by lane | Bonus | Ebb |
|---|---|---|---|---|---|---|---|---|
| oath-1 | 0 Rivergate | domination | 1 | easy | none | tidecallers_easy | o1_hold_all | no |
| oath-2 | 3 Hollowwood | survival | 2 | easy | ally_gilded_easy | director waves | o2_lose8 | no |
| oath-3 | 1 Ironhold | siege (player attacks) | 2 | normal | ally_ironclad_normal | tidecallers_normal x2 | o3_gate_by 360 s | no |
| oath-4 | 2 Frostmarch | convoy (player escorts) | 2 | normal | ally_gilded_normal | wildmoot_normal x2 | o4_never_rolled | yes |
| oath-5 | 3 Hollowwood | domination | 3 | normal | ally_gilded_normal, _b | wildmoot_normal x3 | o5_lose12 | yes |
| oath-6 | 4 Kingsfield | domination | 3 | hard | ally_gilded_normal, _b | wildmoot_hard, wildmoot_normal x2 (all at tier hard) | o6_hold_all | yes |

There is no per-battle AI script or scripted event. The only battle-specific behaviour is:
- The glimpur ebb: a cosmetic flicker on casters (MarchMath.java:194-231).
- The bonus trackers.
- Survival's wave set, scaled by the battle tier (SurvivalMode.java:73-77).

In oath-4 (2v2) the roles work out as: human pusher, ally bot screen; defence 1 raider and 1 blocker (ModeMath.java:52-58) (code).

### 4. Harness

`test-tools/java/gg/gravesandglory/BotSimulation.java` runs with `-PbotSim=N`. Options: `simDawn`/`simDusk` tiers, `simMaps`, `simSize` (3), `simKit=native` (which makes Dawn gilded and Dusk graveborn, :154), `simBanner0`/`simBanner1`, `simMode`, `simOverride`, `simProgression`.

Domination's clock is forced to 300 s (:143), but the sheet has 900 s and winTarget 300 at 1 point per held point every 4 s. So no sim match ever reaches the target, and every match is a timeout decided by the score lead.

Files it writes:
- `results.csv`, one row per match (:91, :215-234): winner, scores, seconds, banners, kills, wiped groups, retreats, fights, mean fight length, flank hits and shots, score300, the Siege stage columns, and `checkpoints,progress,idleMax`.
- `gold.csv` (every 30 s), `levels.csv`, `types.csv` (TypeLedger) and `meta.txt`.
- `siege.csv`: per army and group every 5 s (:285-297).
- `convoy.csv` every 5 s, plus `convoy-units.csv` every 10 s with per-troop ticking, nav-idle and stuck data (:268-283).
- `probe.csv`, only with GRAVES_BOTSIM_PROBE=1 (:104-111).
- A console health line every 30 s with the stuck count (:162-172).

`-PsimFast=true` (testing/SimSpeed.java, FastTickMixin) only removes the 50 ms tick wait. Game logic is unchanged.

`tools/mode_gate.py` checks:
- siege: 30-70% wins and the stage-0 rule.
- domination: dawn 30-70% with draws counting half, and no match where both scores are 0 (:55-68).
- convoy: escort 30-70%, mean checkpoints >= 1.5, no stall over 60 s (:71-92).

`tools/balance_gate.py` checks a 40-60 band over two seat-swapped runs.

Domination per-group trace: there is none in the committed code. The local edit adds `dominationTrace` (BotSimulation.java:250-267). It writes `domination.csv` every 5 s (owners string, per army and group: centre, order, target, retreating, nearest foe) and `domination-units.csv` every 10 s. Nothing is per second. Its javadoc was inserted so that convoyTrace's comment now sits above it (:249-250).

The running run `work/method/ai-dom-191818` (started 19:18) has no `domination.csv`. The edit is dated 19:20, so that run predates it. A per-second trace means lowering `SIEGE_TICKS` (100) or adding a separate cadence.

Gaps:
- No idleSeconds, objectiveShare or formationHold columns (docs/drafts/bot-quality-targets.md "Not built yet").
- No per-army point-choice log, no capture timeline, no record of who reached which point first.
- No survival or banner checker.
- No tier-ladder gate.
- The Convoy "stall" metric counts only Idle and Rolling back (BotSimulation.java:238-243), so a wagon that is contested all match never registers as stalled.
- A domination row records `storedAssignment`, which gives no hint why a point was chosen.

### 5. Hypotheses

#### A. Domination: Dawn about 8 of 10

1. **It is mostly the House pairing, not the seat. (data)**
   - The native kit is always Gilded on Dawn against Graveborn on Dusk (BotSimulation.java:154).
   - With seats swapped, Gilded won 10 of 10 as Dusk, 18 of 20 overall (docs/reports/mode-gate-2026-11-02.md, Domination row).
   - Run ai-dom-191818 (10 matches, normal, native): Dawn kills Dusk 2-2.5 times as often as the reverse on every map: 71/44, 91/37, 74/33, 75/42, 39/25, 78/35, 88/37, 75/54, 78/35, 31/21. Dusk loses 2-6 groups wiped per match against Dawn's 1-3, and there were 0 retreats.
   - This happens even though 1:1 duels favour Graveborn: Gilded 40.2 against Graveborn 58.5 (work/HOUSE-BALANCE-RETUNE-2026-11-02.md, with Set A, which is in the gate's base).
   - So the edge is army-level. Candidates (code, unverified):
     - Gilded fields 10 shield troops (6 knights, 4 sentinels; sheets `troops` tag `shield`). Graveborn fields none.
     - Bots always form `block` (Campaign.java:802), so the shields rise under archer fire. That blunts Graveborn's 8 ranged (bonebow, stalker), while Gilded's 8 ranged hit unshielded troops.
     - The Gilded mage heals (combatDiscipline healAmount 4, radius 10).
     - Total health: Gilded 504 against Graveborn 451 (gilded_normal against graveborn_normal).
     - The same shield mechanic was the confirmed Dawn bias in October (docs/archive/QUESTIONS-FOR-KORT-before-optimization-2026-10-25.md, item 4).
2. **The only real seat asymmetry favours Dusk, on map 1.**
   - Ironhold spawns: Dawn at -314, Dusk at 128 (CampaignMaps.java:113), with points at x = -29, 0, 29 (:38-40).
   - So Dusk is about 150 blocks closer to every point. It wins both map-1 matches (76-108, 78-105) while losing fights 91 to 37 (data). That matches "only map 1 favours Graveborn" in the gate report.
   - Maps 0, 2, 3 and 4 have half-turn-symmetric points, fan roads and barracks (:38-47, 117, 120), and laneTarget mirrors them (BattleMath.java:644).
3. **Minor seat leanings. (code, unverified, likely small)**
   - Terrain is mirrored in x, not half-turned. Ridge at (±138, -80), hill at (±130, 100); on map 2 the ridge is 28 high against a 12-high hill (CampaignMaps.java:66-67). So Dawn's lane-0 army crosses a ridge while Dusk's lane-0 army crosses a hill.
   - The A-side village (mill, farm, houses, :48-52) sits on Dawn's approach. The shrine (:57) sits on Dusk's.
   - Dawn squads are inserted first (Campaign.java:349-351), so they decide and buy first, and their units tick first (GravesAndGlory.java:838).
4. **Why it snowballs. (code)**
   - The 300 s clock decides by score lead.
   - Held points become forward reinforcement anchors (BattleMode.java:59; GravesAndGlory.java:1139-1146).
   - Nobody garrisons, and Easy/Normal almost never charge. So the side that wins the first lane clashes keeps its points and reinforces forward. Kort's October note: "first clash decides".
5. **Test to separate these:**
   - Run `-PsimKit=gilded` and `-PsimKit=graveborn` (same House on both seats), excluding map 1. That isolates the seat.
   - Run `-PsimOverride` to remove the shield tag or force `loose` formation. That tests the shield hypothesis.
   - Run with the new domination.csv to see who reaches which point first.

#### B. Convoy: 0 checkpoints

1. **Timing arithmetic. (code plus sheet)**
   - The road on map 2 (sheets `modeSites` convoy_frostmarch_road_0-6) puts the first checkpoint, Pass Crossing, at 262 road blocks: 202 + 60, from (-230,0) to (-28,0) to (32,0). Signal Ridge is at about 395 and The Refuge at about 538.
   - Commit 9ee1c36e cut push speed from 1.2/1.5/1.8 to 0.5/0.63/0.75 blocks per second (sheets `modeRules` convoy).
   - Even with 3+ pushers uncontested, the first checkpoint takes 262 / 0.75 = 349 s, or 524 s with one pusher. Uphill segments are x0.75 (ModeMath.java:135-139).
   - The clock is 420 s (sheets `modes` convoy), and the first checkpoint is what adds +180 s.
   - So the escort has about 70 s in total for walking from (-283,0) to the road and for all contests. Any defence presence beyond that means 0 checkpoints.
   - Before the cut it took 146 s. The commit says the escort won 4 of 10 right after the cut; whatever changed since (bot charge cooldown 6cb8b1fb, Attack/Charge rework 0b0a696b) only adds contest time. (unverified)
2. **The defence can contest almost all match. (code)**
   - Any single defender within 6 blocks stops the wagon (ModeMath.java:104-105; ConvoyMode.java:199-212).
   - 2 of 3 defence banners are raiders sitting on the wagon (:321, :351-353).
   - Since 4ec05c02 the defence never retreats (:330) and its blockers hold 10 ahead (:327).
   - It also forward-deploys at Pass Crossing, the next checkpoint (:296-301). Its refills appear 60 blocks in front of the wagon, while the escort's refills appear at the road start, 262 blocks back, or at its base when retreating.
3. **Escort pushers drift off the wagon. (code)** Pushers and screens are on `attack` with a 24-block engage radius (sheets `orders`) and chase raiders. Pushing counts any escort troop within 6 blocks (ConvoyMode.java:199-204), so a fight more than 6 blocks from the wagon pushes nothing. `idleMax` cannot see this (section 4).
4. **Check:** read `convoy.csv` columns state, pushers, defenders and wagonDist for the share of time CONTESTED. Then try `-PsimOverride=modeRules.convoy.speedOne=...` or a longer clock. A pre-existing chunk freeze (fixed in d14fe789, ConvoyMode.java:185-191) is no longer a candidate: the unforce guard is present.

---

## Appendix C. Research notes and sources

## Human-like, purposeful tactics bots for a Fabric Minecraft battle mod: research

Date: 2026-11-09. Scope: Graves and Glory (1.20.1 now, 1.21.1 port later). Server-side AI, 20 TPS, 1-4 commanders per side, ~10-20 troop mobs per commander.

Citation tags: `[n]` = numbered source at the end. Source strength: S = primary/peer-reviewed/official/read in full, M = reputable secondary or read only partly, W = forum/blog/press/student paper. Anything marked **(unverified)** is from my own knowledge and was NOT confirmed against a source in this pass. Anything marked **Recommendation** is my inference, not a cited claim.

---------------------------------------------------------------------

### 0. One-paragraph verdict

For a 10-20 unit-per-commander real-time tactics game the best-evidenced pattern is the **Killzone 2/3 chain of command**: a Commander layer that turns game-mode state into weighted *objectives* and assigns squads to them, a Squad layer that turns objectives into orders, roles and routes, and an Individual layer that is free to choose *how* (target, position, ability) and may override orders to survive [9][10]. Fill the layers with **utility scoring** (cheap to tune, good at "which objective / which target / which position") and a small **priority-list behaviour tree or HTN** for the per-unit action sequence [7][8][9]. Add a **coarse threat/influence map** and **last-seen memory with decay** for fog-of-war honesty [9][16][26], a **Days-Gone-style confidence/morale value** to drive charge/hold/retreat [13], and **human limits** (reaction delay 0.2-0.4 s with jitter, one attention focus per commander, command-rate cap) [28][32][33]. Do not train a neural net; use headless bot-vs-bot tournaments plus evolutionary/CMA-style tuning of utility weights instead [35][36][38]. In Minecraft the dominant risks are vanilla per-mob A* cost and group jamming, so path **per group, not per unit**, snapshot-then-compute any off-thread work, and keep a hard per-tick AI time budget [40-48].

---------------------------------------------------------------------

### A. Architectures for purposeful, human-feeling opponents

#### A1. Utility AI (Dave Mark's IAUS, Guild Wars 2, XCOM, Dragon Age Inquisition)
- **What:** every candidate action/target/position gets a score from several "considerations", each a response curve normalised to 0-1, combined (multiplied/averaged); pick the best, optionally with weighted-random among top options. Mark's Infinite Axis Utility System is the data-driven version used for agents, squads and armies [1].
- **Shipped:** Guild Wars 2 Heart of Thorns (Mark + Mike Lewis, GDC 2015; modular AI plus influence-map engine) [2][1]; XCOM Enemy Unknown scored every action and every reachable tile (distance, flanking, cover, proximity to other aliens so they stop grenading each other), with ~17 behaviour sets from per-species weights [24] (M, press coverage of GDC 2013); Dragon Age Inquisition has a utility scoring architecture chapter [4] (listing only, not read).
- **Good at:** target choice, position choice, objective choice, "personality" by changing weights, graceful degradation, designers can read why something won [3][15]. Mike Lewis's chapter is specifically about picking *effective* considerations for tactical movement and skill selection [3].
- **Bad at:** long multi-step sequences (needs a plan/BT under it); dithering if scores flip each tick; weight tuning grows unmanageable with many options (Isla notes float tuning is unmanageable at ~20 options, which is why Halo used prioritised lists) [7].
- **Mitigation:** commitment bonus for last tick's winner, as Halo did [7]; hysteresis and minimum durations (Recommendation).
- **Use here:** commander objective choice, squad target/area choice, per-troop target and position choice. **Don't use** as the only layer for scripted-looking sequences (volley, charge-then-reform).

#### A2. Behaviour trees / hierarchical FSMs (Halo 2/3)
- **What:** Isla's Halo 2 brain is a DAG of ~50 behaviours; parents pick children via simple schemes (prioritised list most common), children report binary relevance not floats; "impulses" and stimulus behaviours inserted for 1-2 s on events (e.g. ally died) instead of polling; previous winner gets a bonus to prevent dithering; bit-tag locking of whole branches; memory pooled by tree depth (~4) not behaviour count [7].
- **Squad/orders in Halo 2:** orders group firing positions and are assigned to squads; triggers like squad losses or player sighting switch orders; "styles" allow-list behaviours per order [7].
- **Good at:** deterministic, debuggable, designer-authored, cheap per tick, coherent commitment [7][3].
- **Bad at:** combinatorial explosion in tactical cases; static priorities; Mike Lewis/Dill/Merrill literature exists precisely to graft utility into BTs [4][listing: Merrill, GAP1 ch10 in 19].
- **Use here:** the per-troop "execute order" tree (move-to-slot, engage, kite, guard, retreat). **Don't use** for army-level decisions.

#### A3. GOAP (F.E.A.R.)
- **What:** goals and actions with preconditions/effects; A* over world-state to find an action sequence; F.E.A.R.'s FSM had only three states (Goto, Animate, UseSmartObject) with planning supplying the rest [6] (S).
- **Good at:** emergent sequencing, squad flanking via shared goals; **Bad at:** CPU (a Transformers team reported their HTN planner was considerably faster than their earlier GOAP) [8]; hard to guarantee priorities; design reads as opaque.
- **Use here:** no. A shooter-scale planner is overkill for ~20 mobs with vanilla pathing; the HTN/priority-list gives the same designer control at lower cost.

#### A4. HTN planners (Killzone 2/3, Transformers: Fall of Cybertron, Horizon Zero Dawn / Decima)
- **What:** a domain of methods with ordered branches (precondition -> list of tasks); the first fully decomposable branch wins; tasks are primitive or compound. Branch *order* encodes priority (self-preservation before heal, before attack). Re-plan at a fixed rate and replace the current plan only if a higher-priority branch now applies; "continue" branches keep a valid plan alive [9] (S, read in full).
- **Shipped:** Killzone 2 was the first commercial game built on this style of HTN (secondary) [11]; Killzone 3 bots (individual *and* squad planners) [9]; Horizon Zero Dawn and Decima ("HTN Planning in Decima", Guerrilla) [12]; Transformers FoC [8].
- **Good at:** designer control of priorities, readable decomposition trees for debugging, no cost-tuning [8][9]. **Bad at:** a wrong branch order is a silent bug; needs a compiler/tooling to stay manageable (Killzone compiled the domain to C++) [9].
- **Use here:** worthwhile at the *squad* level (formation advance -> regroup -> assault -> reform). A hand-rolled "ordered list of (precondition, task list)" with replan every N ticks gets 90% of it without a framework (Recommendation).

#### A5. Layered chain-of-command (the main fit)
- **Killzone 3 multiplayer bots (best single reference):** three layers, orders down, information up [9] (S).
  - Commander: game-mode-specific code generates **objectives** (AdvanceToWaypoint, DefendMarker, AttackEntity, EscortEntity), each with a *weight* and an *optimal bot count*; a six-step assignment algorithm distributes squads then bots, prefers keeping squads on their current objective, otherwise nearest [9]. The commanders were custom code from designer/QA input; keeping the commander small by feeding it a list of objectives [9].
  - Squad: its own HTN planner; translates objectives to per-member commands via a **command queue** (orders overwrite unless part of a sequence); reasons on a hierarchical "strategic graph" plus a faction **influence map**; adds a penalty to areas used by other friendly squads' paths so simultaneous attacks spread out [9].
  - Individual: HTN planner with perception-limited world database ("the bot's idea of the world may be believably wrong"), position picking over waypoints+cover, uses the same virtual controller as players so capabilities are identical, may ignore orders to survive [9].
  - Honest lessons from the authors: squad assignment was *harder* with more objectives and fewer bots (exactly the 10-20 unit situation); they later tried an HTN commander and reinforcement learning to adapt branch order from bot-vs-bot outcomes [9].
- **Verweij's earlier thesis**: one AI commander per faction -> group leaders -> bots [10] (S).
- **Paragon bots (Epic):** an **Objective Graph** (nodes = towers, wells, camps with path-length edges), per-node enemy influence updated periodically, objectives with priority + required resources; Commander loop: pick highest-priority objective, let it pick a *minimum* resource set, reduce its priority by MaxPriority/TeamSize per agent, repeat. Each team's objective pool is separate so bots do not know the other team's plans ("that would be cheating") [14] (S, read). Directly portable to Domination points, convoy waypoints, siege stages.
- **Days Gone squads:** squad is a full AI entity that picks goal, role per member, and area; roles are packaged sub-behaviours slotted into the individual hierarchy at a fixed priority so survival still overrides; decisions driven by a **Confidence** value (panicked/worried/neutral/confident/heroic) [13] (S, read). See C4.
- **Total War:** unit AI under battle AI under campaign AI; battle layer picks formations, movement and targets and sends them to unit layer; early Shogun used ~220 rules from the Art of War [23] (M/W, early-title description, reading is of Shogun-era). No primary Creative Assembly technical paper on modern battle AI was found. Rome II coverage is press-level only (W).
- **StarCraft bots:** the research survey divides bots into strategy / tactics / reactive control, with terrain analysis and intelligence gathering as support; top bots at the time mostly hard-coded strategies, UAlbertaBot used modular managers plus search for build order and combat [19][21] (S/M). PurpleWave (threat-aware A* retreats, potential fields for flyers) and Steamhammer (per-opponent opponent model that picks openings) are described in competition reports and the author's blog [21][22b] (M/W).
- **Supreme Commander 2:** platoons, an AI overlord that coordinates multiple AIs, influence-map decay tuning appear only as patch notes (W) [49]. Company of Heroes 2 and XCOM 2: no primary AI source found in this pass (XCOM 2 alert-priority scoring is community reverse engineering, W) [24].
- **AoE2 Definitive Edition:** developers said the hardest AI uses high-level-play tactics without the old resource cheating, and both old and new AI ship [29] (M); player reports say no resource cheat in skirmish but some dispute research-time/campaign handicaps (W).

#### A6. Which combination for a small-army RTS (Recommendation, built from the above)
1. **Commander** (per commander/bot): objective list with weights and desired force sizes (Paragon + Killzone), assignment loop, re-plan every 1-2 s, with commitment bonus. Utility scoring for objective priority. 
2. **Group controller** (melee, ranged, cavalry, ability): ordered-branch planner/priority list (HTN-lite): form up -> advance -> engage -> reform/retreat; owns the formation, the route, and role slots. Emits a *small set of orders* via a command queue.
3. **Troop** (the mob): a priority BT/ordered Goals: survive > obey order > opportunistic action (focus, kite, brace) > idle behaviours; with utility choosing target/position. The troop may refuse an order for a stated reason (low health, flanked) and that reason is visible (animation/voice/banner), which is what makes it read as purposeful.
4. **Cross-cutting services:** shared blackboard (Section C), threat/influence grid + last-seen memory (B, E), confidence/morale (C4), human limits (F).
- **Why not full GOAP/MCTS:** budget (Section I) and tuning cost; combat-outcome prediction via a closed-form Lanchester model is available when a "should we engage" estimate is needed [20b].

---------------------------------------------------------------------

### B. Spatial reasoning

#### B1. Influence / threat maps
- **What:** grid or graph where each source (unit, tower, last-seen enemy) adds a value that propagates and decays; consumers read it as "danger here / who controls this". Mark's chapter is a modular architecture (propagation, population, retrieval), with examples: place tank-style defenders between enemies and vulnerable casters, relative threat per agent, spell targeting. The map gives information only; the agent's decision layer decides [16] (S).
- **Shipped:** Guild Wars 2 [2]; Killzone 3 faction influence map per *area* (not per cell): counts friendly/enemy bots, turrets and drones, recent deaths shift values, smoothed spatially and temporally, used for squad pathing and picking regroup markers [9]; Paragon objective-graph influence limited to the lane the minion wave is on (don't leak influence through walls) [14]; Dishonored 2 chase flooding (GAP online ed. 2021, listed) [17].
- **Cost:** small if coarse. Killzone and Paragon both work at **area/graph** resolution rather than per-voxel [9][14]. Mike Lewis's "Escaping the Grid" gives infinite-resolution variant [17] (listing).
- **Use here:** a 2D (x,z) grid of e.g. 4x4- or 8x8-block cells over the arena, only for the battle bounding box, updated at 2-4 Hz and time-sliced. Cells store: friendly strength, enemy strength (visible), enemy last-seen strength with decay, and static terrain tags. **Don't** use per-block resolution or 3D.

#### B2. Tactical position scoring
- **Killzone:** score candidate waypoints with a weighted sum (cover from known threats, distance from threats/friends, line of fire, travel distance, etc.), different weights per behaviour; also *threat prediction*: likely hiding waypoints for a hidden threat [9][11] (S/M).
- **CryEngine Tactical Point System (Crysis 2):** query language to generate candidate points then filter and weight them; **cost is dominated by raycasts**, so generate candidates cheaply and defer expensive tests; scheduler amortises queries over frames; group techniques for avoiding two agents choosing the same point [15] (S, read).
- **Taming/auto-generated spatial queries, Paragon "Where to go":** listed in GAP [15][14][17].
- **Use here:** "position" = a point within +-R blocks of the unit's formation slot or objective. Score by: distance to slot, enemy range band (ranged), melee reach to chosen target, height advantage, line-of-sight (cached coarse raycast), crowding (distance to friends), threat-map value. Evaluate K<=8 candidates per unit per 10-20 ticks (Recommendation).

#### B3. Terrain analysis / chokepoints / high ground / cover
- **StarCraft:** BWTA (Perkins) finds choke points by Voronoi diagram of walkable tiles and splits regions at them; known to be order-dependent when merging regions; BWTA2 is >=10x faster; BWEM provides areas, chokes and bases (no polygons) [22] (S/M).
- **Killzone 3:** "regroup" and "sniping" locations, assassination hiding spots and defend locations were *hand-placed* because automatic generation "would require complex terrain reasoning"; strategic graph itself auto-clustered from the waypoint graph [9] (S).
- **Dill (GAP2 ch31)** "Spatial Reasoning for Strategic Decision Making": region-based decomposition (homogeneous regions) with influence layered on top [17] (listing + skim, M).
- **Use here:** Minecraft arenas are authored; hand-annotate per map (siege gate, keep, convoy route, capture points, flank lanes, high-ground markers) and generate the rest. Auto-chokepoint detection is a **don't** for a hobby mod: marker entities/NBT per map cost less than writing BWTA (Recommendation, supported by Killzone's choice [9]).
- **Flanking/high ground** as *score terms* (angle between target's facing and attacker, relative Y), not special-case code (XCOM did flanking as a tile utility term [24]).

---------------------------------------------------------------------

### C. Squad and formation coordination

#### C1. Shared blackboard and roles
- Halo/Killzone/Days Gone all communicate by *messages and orders up/down plus a shared world state*, not by units reading each other's internals [7][9][13]. Killzone squads build their world state *from member messages*, not perception [9].
- **Roles as slotted packages** (Days Gone): role is a sub-behaviour injected at a fixed priority so higher-priority reactions (self-preservation) pre-empt it; adding a role does not change existing behaviours [13] (S).
- **Use here:** one `GroupBlackboard` per group (objective, formation, anchor, target list, token holders, confidence, last-seen-enemy snapshot) and one `ArmyBlackboard` per commander. Troops read-only except for reports.

#### C2. Formation movement
- **AoE2 (Pottinger):** hierarchical coordinated movement; formation speed set by slowest unit; units ordered outside a formation drop the rules and path individually (from reverse-engineering docs citing Pottinger) [18] (M; the Game Developer articles are the primary).
- **Group navigation (Mars & Chanut, GAP2 ch20):** hierarchical architecture for group navigation, group-to-member communication, emergent vs choreographed formations, group collision [listing, 17] (not read in full).
- **Supreme Commander 2** replaced per-unit fixed A* paths (units collide and stall, esp. in formation, "grinding to a halt" across a thousand units) with **flow-field tiles** (sectors, integrator, cache), explicitly to remove unit-babysitting [flow] (S, read). 
- **Use here (Recommendation):** one path/anchor per *group*; each troop steers to its formation **slot** (anchor + offset rotated by heading), using vanilla `MoveControl` for straight runs and vanilla navigation only when blocked. Formation speed = min member speed, capped; reform when stuck > N ticks.

#### C3. Focus fire, kiting, combined arms, timing
- **Focus fire / kiting** are standard reactive-control scripts; Churchill & Buro's Portfolio Greedy Search searches over a *portfolio of such scripts* per unit group (50v50, 40 ms limit), i.e. "pick a script per group" is the cheap, proven decomposition [20] (S). **Recommendation:** implement scripts (attack-weakest-in-range, attack-nearest, kite, hold, charge) and let utility pick one per group, rather than searching.
- **Combat outcome prediction:** Lanchester-model with parameters fitted by logistic regression from recorded battles; cheap alternative to simulation [20b] (S, intro read). Use for engage/avoid, retreat, and "wait for reinforcements".
- **Hierarchical plan-space planning for multi-unit combat maneuvers** (van der Sterren, GAP1 ch13): coordinated simultaneous platoon manoeuvres (two-pronged attacks) [listing + skim] (M). Over-scoped here, but the *plan template* ("pin with A, flank with B, then regroup at Z") is a good HTN method shape for cavalry flanks.
- **Melee screens ranged:** Killzone-style role/slot semantics: Days Gone's Frontline (direction of combat, line closest members may stand to the enemy, neutral buffer area, enemy-controlled area) keeps the sides separate and legible; the chapter argues intermixing makes it hard for the *player* to read intent and erodes the impression of AI [13] (S). **Use here:** compute a frontline from friendly vs visible-enemy centroids; melee slots on/just behind the line, ranged slots at weapon range behind, cavalry on flank lanes.
- **Attack tokens / throttling simultaneous attackers:** Ocio's chapter (accuracy tokens, "how many agents can hit the player simultaneously") shows the believability cost of purposely missing; prefers fewer, visible attackers [28]. Relevant if bots fight *players* (survival/campaign).
- **Charge timing:** trigger on confidence + engagement window + formation-ready flag, not a fixed timer (Recommendation). 

#### C4. Retreat / regroup / morale
- **Days Gone Confidence** (S, read): per-AI quotient of perceived friendly vs enemy strength (units, kills, losses; strength modified by armour, weapon, wounded, suppressed, and by each unit's own confidence); five levels; squad confidence = average of members rounded toward neutral; panic is contagious (a panicked unit's strength is credited to the *enemy* side); kills and casualties decay over time; the formula converges in a few updates; toward the player it deliberately lets the player take initiative (flank them and the AI yields) [13]. Squad uses it for press/hold/retreat.
- **Killzone:** squads report completion or *imminent failure* (decimated) to the commander, which reassigns [9].
- **Use here:** exactly this, with five levels mapping to existing orders: heroic=charge, confident=attack, neutral=hold/advance, worried=fall back to regroup marker, panicked=retreat/rout. Regroup markers: hand-placed safe points near objectives (Killzone) [9].

---------------------------------------------------------------------

### D. Army composition, build choice, counter-picking, economy timing

(Your game has no economy per se; the transferable ideas are *pre-battle composition choice* and *adapting to what is seen*.)

- **StarCraft bots:** the survey documents bots committing to hard-coded openings; a bot can win a tournament just by having a strategy nobody has a counter for; build-order planning with timing constraints exists (Churchill & Buro) [19] (S).
- **Steamhammer:** keeps a file per opponent, models whether the opponent repeats or varies, prefers openings that previously worked against that opponent, uses "counter" openings mainly to test responses; author says strategy *reactions* were much harder than the opponent model [22b] (W, but a first-hand author source).
- **UAlbertaBot:** modular managers + search for build order and combat [21] (M).
- **AoE2 AI scripts:** rule-based scripting with personalities/strategies; DE "Extreme" described as playing without resource cheats [29] (M). Primary script docs not fetched.
- **Use here (Recommendation):** 
  - Composition = utility over a template pool (e.g. "shield wall + archers", "cavalry heavy", "balanced") using map/mode tags and *last-match scouting memory* (what the opponent fielded), with a personality bias. Counter-pick lightly (+/- weights), never perfectly.
  - Mode-specific: Domination -> mobile cavalry share up; Convoy defence -> ranged + blockers near route; Siege attackers -> shield/ram support, defenders -> ranged on walls; Survival -> no composition choice (director spawns).
  - In-battle "composition" = group assignment to objectives (A5). Reserve groups kept back for reaction (no purposeless idle: every reserve has a named trigger).

---------------------------------------------------------------------

### E. Scouting and imperfect information

#### E1. Why no-cheat matters and how players perceive it
- The StarCraft survey says most commercial RTS AIs cheat with full map view, and that some games *simulate scouting* so players believe the AI does not cheat (Bob Fitch's AIIDE 2011 keynote on the WarCraft/StarCraft AI); it lists believability (scouting instead of cheating) as an industry challenge [19] (S).
- **Perception study (Wetzel & Anderson, GAP3):** players asked to rate two dozen AIs were bad at estimating actual difficulty, bad at inferring strategy, and *no player* commented on AIs that really cheated while several complained about cheating from AIs that did not; perceived difficulty correlated highly with perceived realism, and a random-action AI was rated among the hardest [27] (S, read, but a turn-based game and a small study; generalise carefully). **Implication:** fairness is mostly about *no visible super-knowledge*, e.g. units turning straight to the unseen flanker or the commander reacting instantly to a hidden force. Do the cheap honest thing: give the bot only what a human would see.
- **Paragon:** each team's objective pool is separate so bots cannot use the other team's plans [14].
- **Killzone 3:** bots' world databases hold only perceived or messaged facts, with sensors limited by view cones and distance, so they are "believably wrong" [9].

#### E2. Techniques
- **Last-known-position memory with decay (Recommendation, simplest):** per observed enemy group, store (pos, velocity, strength, t_seen). Estimate position = pos + vel * dt (clamped to walkable region); confidence decays with dt. Feed the threat map's "last-seen" layer. Expire after 20-40 s or when the area is re-observed empty.
- **Occupancy maps / particle filters:** Isla's occupancy maps are an influence field radiating out from last-known position into unseen cells (described by forum posters; Isla's papers are the primary) [26] (W for the forum, S for Third Eye Crime: "Building a Stealth Game Around Occupancy Maps", AIIDE) [26b]; Weber et al. used a particle model with linear trajectory update to track enemy units under fog of war in StarCraft [19]; Darken & Anderegg particle filters in Game AI Programming Wisdom 4 [26] (W). Cost: grid diffusion is cheap; particles scale with count. Not needed for 10-20 units: last-known + decay is enough, with a diffusion step only for searching.
- **Splinter Cell Blacklist:** awareness is a set of discrete levels raised by perception over time, with investigation at a threshold of a progress bar; vision cones + extra tests; a Tactical Environment Awareness System; a note that perception can't be perfectly fair to the player [26c] (S, partial read). **Use here:** per-unit awareness meter (unaware -> suspicious -> alerted) with delays gives readable, human-like reactions and a natural reaction time.
- **Alien: Isolation:** a macro "director" knows where the player is but never tells the creature exactly; it nudges it to the *general area* and uses a "menace gauge" to back the alien off; the creature's behaviour tree unlocks nodes over time so it appears to learn [25] (M, secondary summaries of GDC). Direct analogue: **commander knowledge != unit knowledge**; the commander dispatches scouts/groups toward a *region*, units discover details themselves.
- **Scouting actions:** assign a cheap fast unit (cavalry) a scout objective with a utility value that rises with "staleness" of knowledge of a region (Recommendation; the survey notes bots that scout well performed better [19]).

---------------------------------------------------------------------

### F. Difficulty by better decisions, plus human-like limits

#### F1. Examples of difficulty without stat cheats
- **AoE2 DE Extreme:** developer says tactics from high-level play, no cheating, unlike the old AI; players mostly agree on resources, dispute research time [29] (M/W).
- **StarCraft II:** top levels are explicitly "Cheater 1 (Vision), 2 (Resources), 3 (Insane)" above Very Hard and Elite; Cheater levels get full vision and mining boosts (TStarBots paper; forum anecdotes on exact numbers) [30] (M/W). Tencent's TStarBots beat the cheating built-in AI [30]. Lesson: the *ladder of non-cheating levels is short*; a no-cheat top difficulty is a differentiator.
- **Civ:** commonly handicapped via bonuses (no source fetched here, drop).
- **Ocio:** manage difficulty through accuracy/tokens/aggression, not damage; purposeful misses must look natural [28] (S).
- **Days Gone:** AI confidence is modulated by player behaviour so AI yields to aggressive players [13].
- **Left 4 Dead director:** monitors player intensity and paces spawns, one of its stated goals is dramatic pacing [37] (M). Useful for Survival waves and campaign encounters, not for PvP bots.

#### F2. Human-like limits (implementation menu, Recommendation unless cited)
| Limit | Value | Basis |
|---|---|---|
| Reaction time to new stimulus | 4-8 ticks base (0.2-0.4 s) + jitter per unit; commander decisions every 10-20 ticks | Humans ~0.2-0.4 s; simultaneous reactions look robotic; randomise timings [28]. OpenAI Five reacted in 217 ms on average and was given restrictions to be comparable [33]. |
| Command/APM cap | e.g. <= 1-2 meaningful orders/s per commander, with an extra delay for large reassignment | AlphaStar capped at 22 agent actions per 5 s, used a camera interface, restrictions approved by a pro [32] (S). |
| Attention | One "focus region" per commander at a time; attention switches cost ticks; unattended groups run their last order | AlphaStar's camera = attention limit [32]. Halo's stimulus windows [7] |
| Perception | view cones/ranges, occlusion by blocks, memory decay | [9][26c] |
| Deliberate imperfection | formation sloppiness, late reaction to flanks, occasional wrong target, misjudged engage | Wetzel: avoid errors "no human would make"; *type* of mistake matters (a billiards AI that makes hard shots then misses easy ones broke the illusion) [27] |
| Personalities | aggressive / defensive / harasser as *weight sets* on the same utilities (plus a per-match random seed) | XCOM species biases and ~17 behaviour sets [24]; Mark's IAUS data-driven weights [1] |
| Adaptive difficulty | adjust decision-quality parameters (reaction time, scouting accuracy, commit threshold) from recent results, not stats | [28][13][37] |
- **Important caveat on "humanising":** Wetzel found that adding revenge/persistence traits did not raise perceived realism; perceived realism tracked *perceived competence* [27]. Spend effort on purposeful, legible tactics first, quirks second.

#### F3. The "believable bots" literature
- **BotPrize** (Hingston; Turing-test-style contest in Unreal Tournament 2004): 2008 winner AMIS fooled 2 of 5 judges, humanness 2.4/4, all humans judged more human [31] (M); 2012 grand prize won by MirrorBot (52.2%) and UT^2 (51.9%) vs average human 40% [31] (M, from poster/press). Mirror Bot copied other players' recorded movement in real time; UT^2 replays nearest-neighbour *human traces* to get unstuck and evolved combat with multiobjective neuroevolution under constraints to prevent superhuman aim (rapid movements and long distances reduce accuracy) [31] (M, UT news and slides listing). Published as "Humanlike Combat Behavior via Multiobjective Neuroevolution" in *Believable Bots* (Springer 2012) [31].
- **Lessons that transfer without ML:** (a) impose human physical limits as hard constraints (aim noise, turn rate, reaction) [31]; (b) replaying/mimicking *human-looking motion traces* beat hand-built logic at stuck recovery; for us, "formation sloppiness" and recorded-style idle micro-behaviours are the analogue; (c) judges detected bots through repetitive or unnatural behaviour under stress (summary-level; verify in Hingston papers before relying).

---------------------------------------------------------------------

### G. Measuring bot quality

#### G1. What to measure (Recommendation, with supporting evidence)
- **Purposefulness / idle metrics:** % of ticks each troop spends in no-order or idle; "orphan" units (no objective > N s); mean distance of unit from its role's expected location; time-to-first-contact after order; fraction of combat time with focus-fired targets; friendly-fire/stuck counts; path recomputations per unit-minute; formation break count.
- **Objective metrics per mode:** capture-point time share, convoy progress rate, siege stage times, wave survival time.
- **Human-comparison metrics:** orders per second, reaction latency distribution, scouting coverage; compare bot logs against recorded human commanders (the BotPrize approach in spirit) [31].
- **Strength metrics:** win-rate spread between bot versions and between personalities; **Elo or TrueSkill** ladder with draws and multi-party rating [36] (S: TrueSkill handles more than two competitors, draws and team results, tracks uncertainty and converges faster than Elo). Use as regression gates: a new version must beat the previous baseline by margin or equal it with lower idle time.
- **Blind tests:** BotPrize-style judging (human judges watch replays or play, label "bot or human") [31]; at hobby scale, 6-10 judges x 4-6 games is enough for coarse signal (Recommendation). Remember Wetzel: players misjudge strategy and cheating, so ask *what they saw*, not just the label [27].

#### G2. How studios test
- **Automated AI tests:** reproduce most AI bugs with two characters; write sequential test scenarios; most complex failures have simple underlying causes tested in isolation (Skarupke, GAP online 2021) [35] (S, intro read).
- **Autoplay agents for prelaunch tuning:** simple agents are enough for balance and exploit discovery; start design with agents (Borovikov, EA) [35] (S, intro read).
- **GA to generate player agents for balance QA** (Square Enix, Grimms Notes Repage): only the fitness equation and genome decoding are game-specific; a full search run reported on the order of 10 hours [35] (S, partial read).
- **StarCraft AI competitions** (AIIDE, CIG, SSCAIT): round-robin tournaments of bots enforcing fog of war; the survey documents the format and results [19][21] (S/M). 
- **Bot-vs-bot tournaments inside the game process:** Killzone used bot-vs-bot outcomes to adapt HTN branch ordering with reinforcement learning (Huizinga, cited in [9]).
- **Debug tooling:** Guerrilla's HTN decomposition logging [9][12]; Halo logs impulses and "hacks" globally [7]; Paragon/Killzone provide visible tree traces. **Recommendation:** log "why" per decision (top 3 utilities) to a ring buffer and a replay file.

---------------------------------------------------------------------

### H. Learning-based bots: what transfers, and what does not

#### H1. Reference systems
- **AlphaStar (DeepMind):** imitation learning from human replays gave an initial policy better than 84% of active players; then a multi-agent **league** with *main agents* and *exploiter agents* that expose weaknesses ("playing to win is insufficient"); final agent above 99.8% of ranked players; interface restricted to a camera view and max 22 agent actions per 5 s [32] (S, DeepMind blog read).
- **OpenAI Five:** average reaction time 217 ms; only 17 of 117 heroes; some mechanics hand-scripted (item order, courier); "surgery" about once per two weeks over 10 months; ~770-820 PFlops/s-days; thousands of GPUs/CPUs [33] (S, paper read via ar5iv). Team-spirit reward blending 0.3 -> 1.0 [33].
- **Lux AI / MicroRTS competitions:** learned agents won Lux Season 1 (Toad Brigade; imitation-learning details come from a competitor write-up, W) while a microRTS paper notes rule-based agents won Halide/Kore/Lux Season 2-style contests [34] (M/W; verify before quoting).
- **Hobby Minecraft ML mods exist** (ReinforceMC with DJL on Forge 1.16.5; Adaptive Mob AI, a DQN mod for Fabric 1.20.1) but I found no evidence of a shipped, robust tactical battle bot on ONNX/DJL [39] (W).

#### H2. Lessons that transfer *without* training a network
1. **Hard human limits** (APM cap, camera/attention, reaction time) from AlphaStar/OpenAI Five [32][33] -> implement as the F2 table.
2. **League of exploiters as a test method:** keep an archive of past bot versions and personalities, and a few "exploiter" scripted bots tuned to beat the current main bot (e.g. kiting-only, rush, turtle). Regression = main bot must beat all exploiters [32] (Recommendation).
3. **Reward shaping -> utility terms:** OpenAI Five's shaped signals (deaths, resources) [33] correspond to utility considerations (own losses, enemy losses, objective progress, tempo). Use the shaped-reward list as a checklist of considerations (Recommendation).
4. **Self-play tournaments as tuning:** Killzone's RL over HTN branch order [9]; GA agents for balance [35]; evolutionary search of utility weights below.
5. **Imitation from humans:** record real human commander sessions on your own server (orders, timings) to calibrate reaction/APM distributions and personality weights (Recommendation; AlphaStar used replays to seed [32]).

#### H3. Is a learned component realistic for a hobby mod? Plain verdict
- **No for the decision core.** Reasons: (1) training cost (OpenAI Five ~10 months on thousands of accelerators; AlphaStar league) [32][33]; (2) needs a fast headless simulator; a Minecraft server is not one (20 TPS wall-clock unless you build an offline sim); (3) non-determinism and unexplainable failures; (4) server CPU budget: 50 ms/tick for everything (Tick page [40]); (5) maintenance across 1.20.1 -> 1.21.1.
- **Java inference is technically feasible:** ONNX Runtime has an official Java API on Maven Central (CPU, Win/Linux/macOS x64), Java 8+ [39] (S). DJL exists (ReinforceMC uses it) [39]. A tiny MLP (< 1 ms) for one narrow scoring function (e.g. "engage win probability") is the only place I would consider one. Even there, Lanchester + logistic regression [20b] is simpler and explainable.
- **Middle ground (recommended):** optimise utility weights and thresholds with search:
  - **CMA-ES / evolution** over ~20-60 parameters; fitness = win-rate/Elo vs an opponent pool from headless bot-vs-bot sims. Literature notes offline tuning needs many samples (each sample = a full match), and self-play fitness needs an opponent pool; CMA-ME (CMA-ES + MAP-Elites) outperformed plain CMA-ES on Hearthstone deck/strategy search and gives *diverse personalities* for free [38] (M).
  - Square Enix's GA-generated agents and EA's autoplay show the game-specific part is only the fitness and genome decoding [35].
  - **Prerequisite:** a headless, faster-than-real-time battle simulator for the combat model (not the Minecraft server). That is a real cost; budget it explicitly. A cheap start: tune against a **simplified abstract model** (SparCraft-style, which supports 50v50 under 40 ms [20]) then validate in-game.

---------------------------------------------------------------------

### I. Minecraft specifics

#### I1. Server tick budget
- TPS stays at 20 only if MSPT <= 50 ms; the entire server tick (all dimensions, all entities, chunk work, network, autosave every 6000 ticks) shares that budget; entities (non-passenger) tick after chunk work each tick; if the loop falls 2 s behind it resets [40] (S, wiki Tick page). Treat AI as getting a fraction of it: **Recommendation:** hard cap of ~5-10 ms/tick *total* for all bot AI (commander + groups + troops + pathing) on a typical host, enforced by a time-slicing scheduler that carries unfinished work to the next tick.
- Only entity-ticking chunks tick entities; these come mainly from simulation distance, and (Java 1.21.5+) all fully loaded chunks get random ticks [40]. Keep the arena fully inside player simulation distance or force-load it (ticket) so troops do not freeze (Recommendation; chunk-ticket API specifics not verified in this pass).
- Server-side only: AI, damage, pathing, spawning; client gets standard entity sync. (Obvious from the model, nothing to cite.)

#### I2. Cost of vanilla mob AI
- **Goal selector / target selector:** mobs have `goalSelector` and `targetSelector`; goals have priorities and flags [41]. The wiki states navigation updates every tick while AI is active and that "navigation can check for stuck mobs, recalculate paths, test direct movement" [41] (M). **(unverified)** In 1.20.1 `GoalSelector` re-evaluated `canUse` for non-running goals on a throttled interval (setNewGoalRate/`newGoalRate`); the NeoForge 1.20.5 primer lists "getRunningGoals" and "setNewGoalRate" as removed, which confirms the API existed in 1.20.1 and was removed by 1.20.5 [51].
- **Pathfinding is A\*:** weighted A\* with heuristic x1.5 (`neighbor.h = getBestH(...) * 1.5F`), hard node-expansion limit (`maxVisitedNodes`), diagonal corner-cutting rules, partial path to the best node when the limit is hit; zombie follow range 35; `MeleeAttackGoal` re-paths every 4-10 ticks (`4 + random(7)`) (decompiled-source analysis in a student paper, MC 1.21.5) [42] (W/M: student paper, but it quotes code). **(unverified, from memory)** the node limit is derived from follow range (~ `followRange * 16`); the paper says only that it "correlates with" follow range [42]. Verify in decompiled source before depending on it.
- **Follow range** attribute: default 32 on the wiki attribute page; most mobs 16, evoker 12, ghast 100; max 2048 [43] (S). Mojira reports extreme follow range causes extreme lag, crashes at 1000, and modified follow range may affect wandering [44] (M).
- **Recalculation:** `EntityNavigation` (Yarn name; Mojang `PathNavigation`) has `RECALCULATE_COOLDOWN`, `recalculatePath()`, `shouldRecalculatePath(pos)`, `rangeMultiplier`, `createPathNodeNavigator(int range)`, `findPathTo`/`findPathToAny` overloads, `checkTimeouts` [45] (S, Yarn 1.21.1 javadoc). Block updates near a path trigger re-evaluation (farm guides, W) [44].
- **Node evaluator / maluses:** open doors/air/trapdoors 0; breach water 4; water, danger-fire, honey 8; damage-fire (lava, magma, fire) 16; blocked/fences/powder snow/closed doors -1; some mobs override [41] (S/M, wiki). Mobs pick lowest-penalty paths.
- **Known limits:** 1-block step (wiki: step height; `step_height` attribute exists from 1.20.5/23w51a [43]); fences and walls are impassable (-1) [41]; large groups jam because each mob paths independently with no group coordination; no built-in formation or flow field (Recommendation, consistent with SupCom 2 account of per-unit A* gridlock [flow]). Paths longer than the node cap return a *partial* path [42].
- **Entity count/cost:** no hard per-entity ms figure found in sources. Paper/Pufferfish-style DAB, NeoForge "Path finder" (48+ block mobs run AI every 4th tick, caps path computations per tick) and SmartAI (round-robin tick budget) exist because mob AI is a known cost [46] (W/M). Lithium rewrote mob brain selection, entity/pathfinding caching with a no-behaviour-change rule [47] (M, listing). **Recommendation:** assume path searches are the spikes; measure with Spark/`/tick` profiling (not verified here).
- **Fabric specifics:** DAB is Paper/Pufferfish-only, not applicable to Fabric; Lithium and Entity Tick Optimizer-style mods are common in Fabric packs and may change timings, test with and without [46][47].

#### I3. What mods do instead
- **Baritone:** its own A\* (segmented calculation because terrain is only known to render distance, incremental cost back-off, picks best node >= 5 blocks away, ignores < 0.01-tick improvements) [48] (M). Client-side bot pathing, not group tactics.
- **MineColonies / PerViamInvenire:** multi-threaded pathfinder: the default navigator caches the chunks between source and target, then submits the A\* to a **thread pool**; derivative of MineColonies' pathfinder; ground mobs only, alpha, no flying/climbing; block weights tunable per entity type [49b] (M, mod page). The page does not detail thread-safety guarantees.
- **Flow fields (SupCom 2 / Planetary-style):** three field types, integrator, cache; scales to hundreds/thousands of agents with a small CPU footprint [flow] (S, GAP1 ch23). Best fit for many units sharing a destination (convoy, capture point). Chunk/section snapshot cost is the Minecraft-specific problem.
- **Navigation graph over arena (Paragon, Killzone strategic graph):** hand-annotated waypoint/area graph for group-level routing, vanilla pathing only inside an area [9][14].

#### I4. Off-thread work safely
- World/chunk reads from a non-server thread are unsafe in vanilla; MineColonies-style cache-then-compute is the shared solution [49b]. Vanilla itself builds a region snapshot for pathfinding (Yarn `ChunkCache`, constructor `(World, minPos, maxPos)`, holds a `Chunk[][]`) [45] (S for existence; the javadoc does not state the purpose; **(unverified)** it is used by `EntityNavigation` for path calculation). 
- **Recommendation (safe pattern):** (1) on the server thread, copy a *compact snapshot* (e.g. a `byte[]` walkability/cost grid for the group's bounding box, plus unit positions as arrays) into a plain object; (2) compute commander/group planning, influence maps, A\* on that grid in a worker; (3) return an immutable result (order list, route as int[] cells, ids not entity refs); (4) apply on the server thread at the start of the next tick (e.g. `server.execute(...)`); (5) validate result still applies (target alive, version stamp). Never touch `Entity`, `ServerWorld`, `BlockState` from the worker. Fabric networking docs do not discuss thread rules, so rely on the above pattern and Minecraft's single main thread tick loop [40][45].
- **Is it worth it?** For 20-80 troops and cell-level planning, main-thread time-sliced work is likely sufficient; use a worker only for A\* over a big snapshot or influence-map diffusion (Recommendation). Complexity cost is real.
- Mobs created via normal entity types are ticked by vanilla; avoid per-tick allocations; use arrays and IDs.

#### I5. Version differences 1.20.1 -> 1.21.1 relevant to mob AI (citations only)
Cited from NeoForge primers (vanilla-code diffs, Mojang names) and wiki; **the Fabric porting page confirms that the move is mostly a build-script update plus code changes and links to those primers** [50][51][52].
- **1.20.5 primer [51]:** `GoalSelector#getRunningGoals` and `setNewGoalRate` removed; `BlockPathTypes -> PathType`; `NodeEvaluator#getGoal -> getTarget`, `getPathType(Mob, BlockPos)` added; `PathNavigation#moveTo` overload with close-enough distance; `Mob#getTargetFromBrain` added; `AttributeModifier` becomes a record; operation names `ADDITION -> ADD_VALUE`, `MULTIPLY_BASE -> ADD_MULTIPLIED_BASE`, `MULTIPLY_TOTAL -> ADD_MULTIPLIED_TOTAL`; `Entity#setMaxUpStep` removed (replaced by the `step_height` attribute, see below); `ItemStack` uses data components (`DataComponentHolder`, `DataComponentPatch`), no mutable NBT; `Entity#defineSynchedData` takes a `SynchedEntityData$Builder`; `EntityType#spawn/create` take a Consumer not a CompoundTag; `Mob#finalizeSpawn` loses the CompoundTag.
- **Attributes (wiki Attribute page) [43]:** `generic.scale` and `generic.step_height` added in 1.20.5 (snapshot 23w51a); `gravity` and `safe_fall_distance` added in 24w06a (1.20.5 cycle); `movement_efficiency` and `water_movement_efficiency` in 1.21 (24w18a); in 1.21 (24w21a) attribute modifiers use a single `id` instead of `uuid` and `name`; the **`generic.` prefix (and `player.`/`zombie.`) was removed in 1.21.2 (24w33a)**, so `generic.follow_range` becomes `follow_range` in 1.21.2, **not** in 1.21.1. So in 1.21.1 you still use `generic.follow_range` / `Attributes.FOLLOW_RANGE` (Mojang constant name unchanged) but modifier IDs are `ResourceLocation`s (1.21 primer) [43][50].
- **1.21 primer [50]:** "Attribute Modifiers, now with ResourceLocations" (AttributeInstance/AttributeMap/MobEffect take ResourceLocation instead of String/UUID); `AttributeInstance#removePermanentModifier` removed, `addOrReplacePermanentModifier` added; `FollowOwnerGoal` no longer takes the can-fly boolean; `PanicGoal` takes a function of PathfinderMob -> damage-type tag; `Mob` leash methods moved to `Leashable`. The primer text did **not** mention Brain or PathNavigation changes.
- **Not found / could not cite:** a concrete change list to `Brain`/Sensor/Activity APIs, to pathfinding node limits or to `GoalSelector` tick rates between 1.20.1 and 1.21.1. **Action:** diff Mojang-mapped sources of `GoalSelector`, `PathNavigation`, `PathFinder`, `WalkNodeEvaluator` between the two versions before the port.
- **Mappings:** Fabric 1.21.1 builds can still use Yarn (example project: Yarn 1.20.1+build.10 vs 1.21.1+build.3; Java 17 vs 21; Fabric API 0.92.2+1.20.1 vs 0.104.0+1.21.1 from one repo diff, W) [52]. **Yarn is not maintained from 26.1 on**: Fabric says 26.1 is the first unobfuscated release, tells developers to migrate to Mojang mappings *before* bumping Minecraft, and Fabric API names move to the official names [53] (S). For a codebase that will live beyond 1.21.1, **write new AI code against Mojang names (or keep Minecraft-facing code behind a thin adapter)** (Recommendation).
- **Java:** 1.21.1 requires Java 21 per the repo diff above [52] (W; verify at the Fabric Develop site).

#### I6. Cheap Minecraft-specific design rules (Recommendation)
- Reduce per-mob vanilla AI to the minimum (disable wander, panic, idle look goals for troops); the troop should have 3-5 custom goals driven by the blackboard, not a vanilla zombie pile.
- One path request per group anchor; stagger recomputes (4-10 vanilla re-path interval [42] suggests the scale; use 10-20 ticks for group anchors); retry-with-backoff; cap queries/tick.
- Keep follow range modest (< 48) and `rangeMultiplier` default; **don't** raise follow range to "see" across the arena (lag and wander side-effects [44]); provide vision via your own perception queries instead.
- Door/fence/gate handling for Siege: author *gate open/closed state* into your own waypoint graph and use bridges (e.g. ladders/ramps) as explicit edges; vanilla treats closed doors as -1 (wiki) [41].
- Water, boats: avoid; mark as impassable in your snapshot cost grid.

---------------------------------------------------------------------

### J. Per-mode notes (Recommendation, synthesising above)
- **Domination:** objective per capture point; commander distributes to maintain majority on N-1 points while pressing one; reserve group reacts to enemy influence spike (Paragon-style priority + enemy presence factor) [14].
- **Convoy:** attackers = Escort objective (EscortEntity, Killzone) with screen slots around wagon; defenders = Defend markers along route + ambush objective at choke; "harass" objective for cavalry [9].
- **Siege:** per-stage objective sets (yard/gate/keep); defenders get hand-placed Defend locations and sniping spots on walls; attackers' regroup markers before gate (Killzone annotations) [9].
- **Survival:** director-style pacing (menace/intensity) for wave spawn and retreat/back-off beats [25][37]; troops are the *player's* side, so human-like limits apply to enemy AI only.
- **Campaign:** scripted objectives feed the same commander interface.

---------------------------------------------------------------------

### K. Minimal build order (Recommendation)
1. Blackboards + order/command queue + objective list + assignment loop (no spatial yet). Log decisions.
2. Group formation + slot steering + single group anchor path; measure idle %.
3. Utility target/position scoring per troop; focus-fire + kite scripts.
4. Confidence/morale -> charge/hold/retreat.
5. Perception + last-seen memory + threat grid.
6. Human limits (reaction, APM cap, attention).
7. Headless bot-vs-bot harness (abstract sim) + Elo/TrueSkill + exploiter bots; then weight tuning by CMA-ES/GA.
8. Blind human judging; iterate.

---------------------------------------------------------------------

### L. Known gaps in this research (be honest)
- No primary CA/Relic/Firaxis technical talks for Total War battle AI, Company of Heroes, XCOM 2 (only press/mod-community).
- Could not read: Dragon Age utility chapter, Mars/Chanut group nav (skim only), Dill spatial (skim only), Pottinger articles, Isla occupancy map papers, Hingston's BotPrize papers, AlphaStar Nature text (paywall; used DeepMind blog), Lithium/Paper source.
- Vanilla `maxVisitedNodes` formula, GoalSelector exact re-evaluation interval, and Brain/Goal API diffs 1.20.1 -> 1.21.1 are **not** verified; do a decompiled-source diff.
- Numbers in the F2 table are design defaults, not literature values (except reaction 0.2-0.4 s [28], 22 actions/5 s [32], 217 ms [33]).

---------------------------------------------------------------------

### Source list

URLs fetched/read in this pass are marked (read); others from search results/listings (listing/search).

1. Dave Mark, Infinite Axis Utility System, gameai.com — https://gameai.com/iaus.php (search) [M]
2. GDC Vault, "Building a Better Centaur: AI at Massive Scale" (Mark and Lewis, 2015) — https://www.gdcvault.com/play/1021848/ (search, abstract only) [M]
3. Mike Lewis, "Choosing Effective Utility-Based Considerations", Game AI Pro 3 ch13 — http://www.gameaipro.com/GameAIPro3/GameAIPro3_Chapter13_Choosing_Effective_Utility-Based_Considerations.pdf (intro read) [S]
4. Hanlon and Watts, Dragon Age Inquisition utility scoring, Game AI Pro 3 ch31 — http://www.gameaipro.com/GameAIPro3/GameAIPro3_Chapter31_Behavior_Decision_System_Dragon_Age_Inquisition's_Utility_Scoring_Architecture.pdf (listing) [S, not read]
5. (reserved)
6. Jeff Orkin, "Three States and a Plan: The AI of F.E.A.R." GDC 2006 — https://gdcvault.com/play/1013282/Three-States-and-a-Plan ; paper https://pages.cs.wisc.edu/~dyer/cs540/handouts/gdc2006_orkin_jeff_fear.pdf (search) [S]
7. Damian Isla, "Handling Complexity in the Halo 2 AI" GDC 2005 — https://www.gamedeveloper.com/programming/gdc-2005-proceeding-handling-complexity-in-the-i-halo-2-i-ai (read) [S]
8. Troy Humphreys, "Exploring HTN Planners through Example", Game AI Pro 1 ch12 (Transformers: Fall of Cybertron HTN faster than their GOAP) — http://www.gameaipro.com/GameAIPro/GameAIPro_Chapter12_Exploring_HTN_Planners_through_Example.pdf (skim) [S]
9. Straatman, Verweij, Champandard, Morcus, Kleve, "Hierarchical AI for Multiplayer Bots in Killzone 3", Game AI Pro 1 ch29 — http://www.gameaipro.com/GameAIPro/GameAIPro_Chapter29_Hierarchical_AI_for_Multiplayer_Bots_in_Killzone_3.pdf (read in full) [S]
10. Tim Verweij, "A Hierarchically-Layered Multiplayer Bot System for a First-Person Shooter" (2007 thesis, Killzone 2) — https://www.guerrilla-games.com/media/News/Files/VUA07_Verweij_Hierarchically-Layered-MP-Bot_System.pdf ; summary page https://www.guerrilla-games.com/read/a-hierarchically-layered-multiplayer-bot-system-for-a-first-person-shooter (search) [S]
11. Straatman, van der Sterren, Beij, "Killzone's AI: dynamic procedural combat tactics" — https://www.guerrilla-games.com/read/killzones-ai-dynamic-procedural-tactics ; GDC slides https://www.guerrilla-games.com/media/News/Files/gdce05_killzone_ai.pdf (search) [S/M]
12. Guerrilla, "HTN Planning in Decima" — https://guerrilla-games.com/read/htn-planning-in-decima ; Game Developer, "Behind the AI of Horizon Zero Dawn (Part 1)" — https://gamedeveloper.com/design/behind-the-ai-of-horizon-zero-dawn-part-1- (search) [M]
13. Tobias Karlsson, "Squad Coordination in Days Gone", Game AI Pro Online Edition 2021 ch12 — http://www.gameaipro.com/GameAIProOnlineEdition2021/GameAIProOnlineEdition2021_Chapter12_Squad_Coordination_in_Days_Gone.pdf (read) [S]
14. Mieszko Zielinski, "Being Where It Counts: Telling Paragon Bots Where to Go", Game AI Pro 3 ch24 — http://www.gameaipro.com/GameAIPro3/GameAIPro3_Chapter24_Being_Where_It_Counts_Telling_Paragon_Bots_Where_to_Go.pdf (read) [S]
15. Matthew Jack, "Tactical Position Selection: An Architecture and Query Language" (CryEngine TPS), Game AI Pro 1 ch26 — http://www.gameaipro.com/GameAIPro/GameAIPro_Chapter26_Tactical_Position_Selection.pdf (read parts) [S]
16. Dave Mark, "Modular Tactical Influence Maps", Game AI Pro 2 ch30 — http://www.gameaipro.com/GameAIPro2/GameAIPro2_Chapter30_Modular_Tactical_Influence_Maps.pdf (read intro) [S]
17. Game AI Pro listing and related chapters (not read in full): Lewis "Escaping the Grid" http://www.gameaipro.com/GameAIPro2/GameAIPro2_Chapter29_Escaping_the_Grid_Infinite-Resolution_Influence_Mapping.pdf ; Dill "Spatial Reasoning for Strategic Decision Making" http://www.gameaipro.com/GameAIPro2/GameAIPro2_Chapter31_Spatial_Reasoning_for_Strategic_Decision_Making.pdf ; Couvidou "Flooding the Influence Map for Chase in Dishonored 2" http://www.gameaipro.com/GameAIProOnlineEdition2021/GameAIProOnlineEdition2021_Chapter06_Flooding_the_Influence_Map_for_Chase_in_Dishonored_2.pdf ; Mars and Chanut "Hierarchical Architecture for Group Navigation Behaviors" http://www.gameaipro.com/GameAIPro2/GameAIPro2_Chapter20_Hierarchical_Architecture_for_Group_Navigation_Behaviors.pdf ; Karlsson/Siemonsmeier "Gears Tactics" http://www.gameaipro.com/GameAIProOnlineEdition2021/GameAIProOnlineEdition2021_Chapter03_Gearing_the_Tactics_Genre_Simultaneous_AI_Actions_in_Gears_Tactics.pdf ; index https://www.gameaipro.com/ (read) [S]
18. Dave Pottinger, "Implementing Coordinated Movement", Game Developer — https://www.gamedeveloper.com/programming/implementing-coordinated-movement (search only); openage reverse-engineering notes on AoE2 formations https://simonsan.github.io/openage-webdocs/sphinx/doc/reverse_engineering/game_mechanics/formations.html (search) [M/W]
19. Ontanon, Synnaeve, Uriarte, Richoux, Churchill, Preuss, "A Survey of Real-Time Strategy Game AI Research and Competition in StarCraft", IEEE TCIAIG 2013 — https://hal.science/hal-00871001/document (read parts) [S]
20. Churchill and Buro, "Portfolio Greedy Search and Simulation for Large-Scale Combat in StarCraft" — https://www.cs.mun.ca/~dchurchill/publications/pdf/combat13.pdf (search) ; 20b. Stanescu, Barriga, Buro, "Combat Outcome Prediction for RTS Games" (Lanchester), Game AI Pro 3 ch25 — http://www.gameaipro.com/GameAIPro3/GameAIPro3_Chapter25_Combat_Outcome_Prediction_for_Real-Time_Strategy_Games.pdf (intro read) [S]
21. Churchill and Buro, UAlbertaBot — https://ojs.aaai.org/index.php/AIIDE/article/view/12548 ; AIIDE 2019 StarCraft AI Competition report (PurpleWave, Steamhammer) https://www.cs.mun.ca/~dchurchill/starcraftaicomp/2019/AIIDE2019_StarcraftAICompetition (search) [M]
22. Perkins, "Terrain Analysis in RTS Games: Choke Point Detection and Region Decomposition" (BWTA), AIIDE 2010 — https://ojs.aaai.org/index.php/AIIDE/article/view/12405 ; BWEM http://bwem.sourceforge.net/ (search) [S/M]. 22b. Jay Scott, Steamhammer opponent-model posts https://www.satirist.org/ai/starcraft/blog/archives/474-the-opponent-model-in-Steamhammer-1.4.html and https://www.satirist.org/ai/starcraft/blog/archives/562-Steamhammer-1.4.2-change-list.html (search) [W, first-hand author]
23. Game Developer, "The Road to War: The AI of Total War (Part 1)" — https://www.gamedeveloper.com/programming/the-road-to-war-the-ai-of-total-war-part-1- (read; Shogun era) ; Game Developer "War Hammer: The AI of Total War (Part 5)" https://www.gamedeveloper.com/design/war-hammer-the-ai-of-total-war-part-5- ; Rome II press e.g. https://games.softpedia.com/blog/Rome-II-Hannibal-at-the-Gates-Diary-The-AI-Succeeds-Tactically-Fails-Strategically-437447.shtml (search) [M/W]
24. PC Gamer, "GDC 2013: The AI tricks behind XCOM, Assassins Creed 3 and Warframe" — https://www.pcgamer.com/gdc-2013-the-ai-tricks-behind-xcom-assassins-creed-3-and-warframe (search) ; XCOM 2 alert-scoring mod changelog (community) https://steamcommunity.com/sharedfiles/filedetails/changelog/1492899729?p=2 [M/W]
25. Game Developer, "The Perfect Organism: The AI of Alien: Isolation" — https://www.gamedeveloper.com/design/the-perfect-organism-the-ai-of-alien-isolation ; "Revisiting the AI of Alien: Isolation" https://www.gamedeveloper.com/design/revisiting-the-ai-of-alien-isolation (search) [M]
26. Occupancy maps/particle filters: forum summary https://gamedev.net/forums/topic/594674-ai-searching-for-the-player-intelligently/?page=1 (W); 26b. Third Eye Crime, AIIDE — https://ojs.aaai.org/index.php/AIIDE/article/view/12663 (search; page blocked on fetch) [S]; 26c. Martin Walsh, "Modeling Perception and Awareness in Splinter Cell Blacklist", Game AI Pro 2 ch28 — http://www.gameaipro.com/GameAIPro2/GameAIPro2_Chapter28_Modeling_Perception_and_Awareness_in_Tom_Clancy's_Splinter_Cell_Blacklist.pdf (partial read) [S]; Crytek Target Tracks http://www.gameaipro.com/GameAIPro/GameAIPro_Chapter31_Crytek's_Target_Tracks_Perception_System.pdf (listing)
27. Wetzel and Anderson, "What You See Is Not What You Get: Player Perception of AI Opponents", Game AI Pro 3 ch4 — http://www.gameaipro.com/GameAIPro3/GameAIPro3_Chapter04_Player_Perception_of_AI_Opponents.pdf (read conclusion) [S]
28. Sergio Ocio Barriales, "Using Your Combat AI Accuracy to Balance Difficulty", Game AI Pro 3 ch33 — http://www.gameaipro.com/GameAIPro3/GameAIPro3_Chapter33_Using_Your_Combat_AI_Accuracy_to_Balance_Difficulty.pdf ; "You had me at AAAAHHH: reactions in game AI" (reaction time 0.2-0.4 s, randomise) http://www.gameaipro.com/GameAIProOnlineEdition2021/GameAIProOnlineEdition2021_Chapter11_You_had_me_at_AAAAHHH_On_the_importance_of_reactions_in_game_AI.pdf (both read in part) [S]
29. AoE2 DE AI: PCWorld preview https://www.pcworld.com/article/3402536/age-of-empires-ii-definitive-edition-preview.html ; Intel coverage https://game.info.intel.com/gaming-access/age-of-empires-ii-definitive-edition-brings-new-life-to-a-classic ; forum https://forums.ageofempires.com/t/does-the-ai-cheat/119262 (search only) [M/W]
30. StarCraft II AI levels: Liquipedia https://liquipedia.net/starcraft2/Artificial_Intelligence ; TStarBots https://paperswithcode.com/paper/tstarbots-defeating-the-cheating-level (search) [M/W]
31. BotPrize: UT Austin news https://news.utexas.edu/2012/09/26/artificially-intelligent-game-bots-pass-the-turing-test-on-turings-centenary/ ; UT^2 slides https://www.cs.utexas.edu/ftp/neural-nets/slides/schrum.wcci12-humanlike.pdf ; poster https://people.southwestern.edu/~schrum2/AAAI-BotPrizePoster.pdf ; MIT Tech Review 2010 https://www.technologyreview.com/s/415274/a-turing-test-for-computer-game-bots/ ; PopSci https://www.popsci.com/technology/article/2012-09/artificially-intelligent-gamer-bots-convince-judges-theyre-more-human-humans/ (search) [M]
32. DeepMind, "AlphaStar: Grandmaster level in StarCraft II using multi-agent reinforcement learning" — https://deepmind.google/discover/blog/alphastar-grandmaster-level-in-starcraft-ii-using-multi-agent-reinforcement-learning/ (read) ; Nature paper https://www.nature.com/articles/s41586-019-1724-z (paywalled, not read) [S]
33. OpenAI et al., "Dota 2 with Large Scale Deep Reinforcement Learning", arXiv 1912.06680 — https://ar5iv.labs.arxiv.org/html/1912.06680 (read first 100k chars) ; OpenAI Five Benchmark https://openai.com/index/openai-five-benchmark/ (search) [S]
34. Lux AI / MicroRTS: https://arxiv.org/pdf/2301.01609v2.pdf ; https://speakerdeck.com/kuto5046/lux-ai-34th-place-solution (W) [M/W]
35. Manabe and Miyake, "Game Balancing using Genetic Algorithms to Generate Player Agents" http://www.gameaipro.com/GameAIProOnlineEdition2021/GameAIProOnlineEdition2021_Chapter17_Game_Balancing_using_Genetic_Algorithms_to_Generate_Player_Agents.pdf ; Borovikov, "AI-Driven Autoplay Agents for Prelaunch Game Tuning" http://www.gameaipro.com/GameAIProOnlineEdition2021/GameAIProOnlineEdition2021_Chapter10_AI-Driven_Autoplay_Agents_for_Prelaunch_Game_Tuning.pdf ; Skarupke, "Automated AI Testing" http://www.gameaipro.com/GameAIProOnlineEdition2021/GameAIProOnlineEdition2021_Chapter01_Automated_AI_Testing_Simple_tests_will_save_you_time.pdf (intros read) [S]
36. Herbrich, Minka, Graepel, "TrueSkill: A Bayesian Skill Rating System" — https://www.microsoft.com/en-us/research/publication/trueskilltm-a-bayesian-skill-rating-system/ ; NIPS PDF https://papers.nips.cc/paper_files/paper/2006/file/f44ee263952e65b3610b8ba51229d1f9-Paper.pdf (search) [S]
37. Michael Booth, "The AI Systems of Left 4 Dead" (GDC slides mirror) — https://www.readkong.com/page/the-ai-systems-of-left-4-dead-michael-booth-valve-9664541 (search) [M]
38. CMA-ES/evolution for game AI parameters: adaptive MCTS tuning with CMA-ES https://dke.maastrichtuniversity.nl/m.winands/documents/New-AdaptiveMCTSforGGP.pdf ; CMA-ME on Hearthstone https://arxiv.org/pdf/1912.02400v1.pdf (search) [M]
39. ONNX Runtime Java — https://onnxruntime.ai/docs/get-started/with-java.html (read) [S]; ReinforceMC (DJL, Forge) https://modrinth.com/mod/reinforcemc ; Adaptive Mob AI (Fabric 1.20.1 DQN) https://www.curseforge.com/minecraft/mc-mods/adaptive-mob-ai/files/7386128 (search) [W]
40. Minecraft Wiki, Tick — https://minecraft.wiki/w/Tick (read) [S/M]
41. Minecraft Wiki, Mob AI (pathfinding maluses, navigation) — https://minecraft.wiki/w/Mob_AI (read first 100k chars; page says pathfinding section "needs expansion") [M]
42. Santosa, "Analysis of A*-Based Mob Pathfinding Behavior in Minecraft" (ITB student paper, MC 1.21.5, quotes decompiled code) — https://informatika.stei.itb.ac.id/~rinaldi.munir/Stmik/2025-2026/Makalah2026/13524041_Nathan%20Adhika%20Santosa_Analysis%20of%20AStar-Based%20Mob%20Pathfinding%20Behavior%20in%20Minecraft.pdf (read) [W]
43. Minecraft Wiki, Attribute (follow_range defaults; history of renames) — https://minecraft.wiki/w/Attribute (read, history from offset 100000) [S/M]
44. Mojira MC-95727 (high follow range lag) https://mojira.dev/MC-95727 ; MC-195247 https://bugs-legacy.mojang.com/browse/MC-195247 ; mob-farm guide (block updates retrigger paths) (search) [M/W]
45. Yarn 1.21.1 javadoc: EntityNavigation https://maven.fabricmc.net/docs/yarn-1.21.1+build.3/net/minecraft/entity/ai/pathing/EntityNavigation.html ; ChunkCache https://maven.fabricmc.net/docs/yarn-1.21.1+build.3/net/minecraft/world/chunk/ChunkCache.html (read) [S]
46. NeoForge "Path finder" mod (48+ blocks every 4th tick, path cap) https://www.modpackindex.com/mod/162249/path-finder ; SmartAI https://www.curseforge.com/minecraft/mc-mods/smartai ; Entity Tick Optimizer; Paper/Pufferfish DAB (search) [W]
47. Lithium (CaffeineMC) — https://modrinth.com/mod/lithium ; https://www.curseforge.com/minecraft/mc-mods/lithium (search) [M]
48. Baritone docs — https://raw.githubusercontent.com/cabaletta/baritone/1.19.4/FEATURES.md (search) [M]
49. Supreme Commander 2 patch notes (AI overlord, platoons, influence decay) — https://www.pcgamer.com/supreme-commander-2-ai-updated-new-dlc-units-revealed ; 49b. PerViamInvenire (MineColonies-derived async pathfinder) — https://www.curseforge.com/minecraft/mc-mods/perviaminvenire (read) [M/W]
50. NeoForge primer 1.20.6 -> 1.21 — https://github.com/neoforged/.github/blob/main/primers/1.21/index.md (read) [M]
51. NeoForge primer 1.20.4 -> 1.20.5 — https://github.com/neoforged/.github/blob/main/primers/1.20.5/index.md (read) [M]
52. Fabric docs, Porting to 1.21.1 — https://docs.fabricmc.net/1.21.1/develop/porting/ (read; says 1.21.1 is a hotfix over 1.21 and links the primers and https://fabricmc.net/2024/05/31/121.html) ; one repo's 1.20.1 vs 1.21.1 build diff https://remotebranch.eu/ChaoticByte/hardboiled/compare/1.0.0-1.21.1..1.21.1 (search, W)
53. Fabric, 26.1 announcement and mappings migration — https://fabricmc.net/2026/03/14/261.html ; https://docs.fabricmc.net/26.1.2/develop/porting/mappings/ (search) [S]
54. "flow" = Elijah Emerson, "Crowd Pathfinding and Steering Using Flow Field Tiles" (Supreme Commander 2), Game AI Pro 1 ch23 — http://www.gameaipro.com/GameAIPro/GameAIPro_Chapter23_Crowd_Pathfinding_and_Steering_Using_Flow_Field_Tiles.pdf (read intro) [S]
