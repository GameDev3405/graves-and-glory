# Bots that feel human and play smart: a fresh design (2026-11-10)

Status: proposal for Kort, 2026-11-10 (not approved). Replaces the design sections of docs/reports/ai-audit-and-design-2026-11-09.md, which Kort reopened ("dont limit yourself by any past work"); the audit and research in that report stand.

Read-only proposal. Earlier AI designs are reference only. Kept constraints: the army economy (best of 5, real pick phase, same gold), six orders through the player's path, no hidden information, no stat or gold bonuses, a few ms per tick, content through data.

One finding from the code shapes everything: **a bot commander has no body today.** `Commander.java:110` returns null for a bot's field commander, and the same bot gate shuts bots out of gear perks (`Commander.java:227`, `GearCombat.java:243,301`) and every relic effect (relic effects read the player's position and buff the player, though gear and relic state live on the `Squad`). The approved polish spec plans an order range measured from a bot's army centre; today only the player's crosshair has a range. Every human army has its commander riding with it; the bot's has no one, and that is hard to hide.

## 1. What makes a bot read as human

1. **It is somebody.** A commander rides with its army, looks around, can be killed and goes quiet while dead, like a fallen player. *Player notices:* a leader to hunt; killing him buys silence.
2. **Readable intentions, sometimes misread.** Moves are set up before they land. *Player notices:* "they're going for the Mill", and once a match it was a feint and the blow hits the Ford.
3. **It watches one place at a time.** Orders come in bursts where it looks; other groups keep their last order. *Player notices:* a second front gets a late answer.
4. **Surprise costs it time; the expected does not.** *Player notices:* ambushes work; a charge it saw coming is answered at once.
5. **Mistakes with causes, then learning.** Greed, tunnel vision, revenge. *Player notices:* it bites your bait once, not twice.
6. **It remembers you through the best of 5.** *Player notices:* in round 3 it brings pikes against the cavalry that beat it in round 2 and guards the lane you used twice.
7. **It plays the score and the clock.** *Player notices:* ahead it protects the lead, behind it gambles, it scrambles for a point in the last 20 s, down 0-2 it saves gold for one big round.
8. **It has a style you can name.** *Player notices:* "Captain Hask always rushes the centre and never guards his archers", true across the whole match.
9. **It talks like a teammate.** *Player notices:* "Taking the Mill, need archers", then it really does; it answers your pings.
10. **Never cheats, never superhuman.** No reaction to the unseen, no perfect control of six groups at once, human order rates. *Player notices:* nothing; one impossible reaction ruins the whole match's illusion.

The theme: **"smart" and "human" are separate jobs.** A brain decides what it wants; a human-limits layer decides how fast, how much and where it can act. Difficulty lives mostly in the second, so Easy and Hard want sensible things but act at different speeds.

## 2. The outside-the-box ideas kept

### A. The embodied commander
- **Player sees:** a named commander on a horse or on foot with its banner, its head turning toward what it is watching, fighting when caught, using relics and its commander ability, killable. Its look direction is a readable tell of its attention.
- **How:** a server-side fake player (the trick Carpet mod uses for `/player`) with a small body controller: stay near the army it watches, avoid fights it did not choose, look at its focus. Order range from the body; no orders while dead (existing F7 rule).
- **Cost:** L for the body and controller, plus a separate L for its own combat. **Risk:** Carpet is a mod, not a library, so this is our own fake player: it fires join events, counts against `maxPlayers` (8), loads chunks, shows in the tab list, and looks fake (ping 0, default skin) unless handled. 1.20.1 and 1.21.1 need different code (`ConnectedClientData` from 1.20.2). Its tracking cost against 200+ troops must be measured. Start with a feasibility spike on both versions. **When:** after the port, once the Warlord chain exists. Needs Kort's yes and an amendment to the polish spec, which says a bot has no body and `down` never applies to a bot (question 1).

### B. Hands: an attention and reaction layer between brain and orders
- **Player sees:** orders come in bursts of 2 to 4 at one place, then a lull while it "looks" elsewhere; a group far from its focus keeps holding where it was told; a sudden ambush takes it a second or two to answer.
- **How:** the planner never orders directly; it posts *wanted orders* with a priority. The Hands has one focus spot (where the body looks, or the command map). Moving focus costs time. Orders release only at the focus, at a rate taken from human data, after a reaction delay set by surprise. A multi-place play (a feint at A, the blow at B) is scheduled as one planned episode, the way a person plans it in advance, so its timed steps are not delayed by focus switches; conditional triggers ("when the enemy is within 16, charge") fire with human reaction jitter, never on the exact tick, and respect the charge cooldown. All numbers are difficulty and persona rows. A queue and timers: almost free.
- **Cost:** S to M. **Risk:** too slow looks dumb; tune against recorded human timing. **When:** a stripped version (spread one bot's orders over 1 to 3 s instead of all at once) fits today's fixes; the full layer after the port.

### C. A playbook with tells, feints and bait
- **Player sees:** manoeuvres he can learn to read: *Feint and switch* (a loud group threatens A, the main body takes B 15 s later), *Bait* (exposed archers draw you onto a hidden shield line), *Anvil and hammer*, *Late swing* (reserve held until the clock is low). Setups show: dust and noise before a charge, a pause at a crossing; long tells on Easy, short on Hard.
- **How:** a play is a data row: roles needed (by capability: "a fast group"), steps written as **player orders with conditions** ("Hold at ambush mark until enemy within 16, then Charge"), triggers, an abort rule, its tell, a teammate line. Feints and bait are chosen only when the bot believes the enemy is looking (enemy groups or commander seen near the decoy), which is fair and is what a person does: hit where the enemy commander is not.
- **Cost:** M (engine) plus S per play. **Risk:** plays that fire in the wrong situation look scripted; every play needs an abort rule and a purpose line. **When:** after the port.

### D. The match journal: memory of the opponent
- **Player sees:** the bot adapts across the best of 5: counter-picks what it saw you field, ambushes the lane you used twice, stops biting a bait it fell for, copies a play that beat it, and sometimes holds a grudge (biases toward the commander who killed its Legendary). After the match, a short "after action" note: "Your cavalry kept reaching my archers. Next time I guard them."
- **How:** a small record per enemy commander, written only from what the team saw: army mix per round, first destination, favourite lanes, plays tried and results, whether they guard ranged, how they answer bait. Quartermaster and planner read it; Easy forgets between rounds, Hard keeps it all.
- **Cost:** M, built with the economy. **Risk:** over-adapting to one fluke; weight by how often it happened. **When:** after the port, with the economy.

### E. The voice: pings and lines
- **Player sees:** a bot teammate pings where it is going, says one short line when it commits, asks for help when it needs it, answers your Attack or Defend pings ("On my way, 20 s"). Enemy bots say almost nothing: a rare line at a round's end, in character.
- **How:** listens to plan events and picks a line from its persona's bank (data, by the lore lead). About one line a minute at most, none while it is in a heavy fight. Every line is true to its real plan. Pings use the existing `ping_v1` kinds (no text) through a new `Squad`-based overload, since `Battle.ping` needs a seated player today; words like "On my way" are chat lines.
- **Cost:** S. **Risk:** spam or cringe, the fastest way to look like a bot; few lines, tested by readers. **When:** pings could go into today's code; lines after the port with personas. Needs a tone call (question 3).

### F. A style library from real human play
- **Player sees:** bot openings and habits that feel like a person's because they came from a person: one captain opens like Kort's friend who always splits early, another turtles like a careful player.
- **How:** a recorder logs every commander's orders, timings, picks and what that side could see, one format for humans and bots. Offline tools turn recordings into (1) an **opening book**: the first 30-60 s of real matches per map and mode, replayed with variation and dropped the moment the enemy surprises it; (2) **persona numbers**: order rate, burst size, how far they split, when they pull back, pick-phase pace. Plain statistics plus a weight search over sims; nothing learned runs live.
- **Cost:** recorder S; fitting M after the port. **Risk:** needs dozens of human matches. Recording people needs their consent (question 4): opt-in, local files only, pseudonymised ids, a deletion rule. **When:** a bots-only recorder now (useful for the metrics); recording people only after Kort answers question 4; fitting after the port.

### G. Mistakes with causes, instead of random noise
- **Player sees:** an Easy bot plays like a new player you can learn to punish: chases too far, forgets a group, commits everything to one fight, retreats late. A Hard bot rarely does, and corrects quickly.
- **How:** a catalogue of human mistakes, each with a trigger and a recovery time: *greed*, *tunnel vision*, *forgotten group*, *overcommit*, *revenge*, *late retreat*. Difficulty and persona set how often each fires. Replaces the earlier "judgement noise": random mistakes look random, caused ones look human.
- **Cost:** S to M. **Risk:** an Easy bot that looks broken instead of green; each mistake needs a visible recovery. **When:** after the port.

### Thrown out
- **In-match drama director:** rubber-banding that players feel; breaks "difficulty changes decisions only". Kept only between matches (choosing the next opponent by skill rating).
- **Ghost replays as opponents:** cannot react; only openings survive (F).
- **Language model or learned policy in the live game:** latency, cost, unpredictable, undebuggable, no fast simulator. Fine offline for line banks and the measuring judge.
- **Chat typos, fake lag; memory across separate matches** (later, maybe for campaign captains).

## 3. The architecture

Seven parts. Only the first runs per troop; everything bot-specific is per commander and cheap.

| Part | One per | Runs | Does |
|---|---|---|---|
| 1. **Troop layer** (the approved Sergeant and Soldier) | group / troop | movement every tick, decisions every 5-10 ticks, staggered; far-from-fight troops think less | Carries out any order well: slots, cohesion, crossings, target spreading, ranged standoff, line of sight. **The same code for human and bot troops.** |
| 2. **Perception** | team | every 10 ticks, time-sliced | Belief map: enemy groups the team has seen (place, heading, strength, age; fades over seconds), enemy commanders seen, influence grid of 8x8 cells, HUD facts (score, clock, objective states). Shared by bots and humans on a team, as the minimap is. |
| 3. **Situation** | bot commander | every 1-2 s | Reads score, clock, round, gold, journal; sets posture (press, hold lead, gamble, save) and risk appetite. |
| 4. **Planner** (with council) | bot commander | every 2 s staggered, plus events (contact, flip, group lost, ping) | Scores objectives (mode list) and plays; assigns groups to roles; keeps commitments unless something is clearly better. The council splits objectives between bots on a team and treats human pings as plans. Posts wanted orders, each with a purpose line. |
| 5. **Hands** | bot commander | every tick (timers only) | Focus, focus-switch cost, reaction delay, burst rate, mistake catalogue; releases orders through `Orders.issue`, the player's path. |
| 6. **Body** (if approved) | bot commander | every tick | Moves, looks at the focus, fights, relics. |
| 7. **Journal, Quartermaster, Voice** | bot commander | pick phase / events | Memory of opponents; buys in the pick phase like a person (takes its time, edits picks visibly, fills gaps in human teammates' picks); lines and pings. |

Offline, outside the game: the **recorder** (all commanders, one format), the **style fitter**, the **tournament runner** and the **weight search**.

**Data contracts.** A mode lists objectives (take, hold, escort, intercept, destroy, survive; place, value, wanted strength). A map has editor marks (rally points, lanes, ambush spots, high ground) plus an automatic walk grid. The brain derives each troop's capability (reach, speed, toughness, area damage, healing) from its existing row. Plays, personas, difficulty rows (Hands limits, plan depth, mistake rates, memory) and line banks are rows. New content needs rows, not code.

**Budget.** Bot-only parts (estimate, to be measured with `GRAVES_BOTSIM_PROBE`): about 0.5 ms mean a tick for 8 bot commanders including perception, with planner spikes up to about 1.5 ms (one plan per tick at most); a body adds its own entity cost, measured in the spike. The real cost is the shared troop layer at 200+ troops: a spatial grid instead of nearest-enemy scans, staggered beats, level of detail. Measure 4v4 at 40 slots before the soft-20 prices are tuned.

**What it takes and drops from earlier designs.**
- *Kept:* General and council (now planner and council), fair vision with fading memory, the objectives interface, utility scoring, tells and answers (now produced by plays), purpose lines and overlay, the metrics, the quartermaster, and the no-routing rule unchanged (troops always obey).
- *Dropped:* the bot-only squad plans (form up, wait for stragglers, bot focus sets). Bot squads smarter than a player's are an unfair helper and a tell. Instead every bot squad move is a sequence of player orders (form up = Hold at a rally point, then Attack), and squad smartness goes into the shared troop layer for everyone. The planner owns the triggers a careful player watches for ("8 of 10 gathered at the rally point, now Attack"), so gathering reinforcements (the audit's C3 and D3 fix) is kept, released through the Hands with human delay. Also dropped: judgement noise, the drama director, live learning.
- *Changed:* House doctrines become a House's preferred plays; personality is separate. Bots prefer orders a player has a key for; the internal Move stays for musters and safe crossings, as the polish spec says (section 3.4).

## 4. How we prove it

**Smart.**
- Sim metrics every run (from the audit): troop idle, objective share, trickle index, blind picks (0), stuck, wasted abilities, purpose coverage (100 percent).
- Ladder: Hard beats Normal 65-85 percent, Normal beats Easy 60-80 percent, over whole best-of-5 matches.
- Exploiter bots: a rusher, a turtle, a baiter, a single-lane spammer. Every new brain version must beat all of them and rate at or above the last version.
- Purpose audit: 50 random purpose lines read per build; nonsense is a defect.

**Human.**
- *Machine judge (cheap, repeatable, not Kort; a diagnostic, not a gate):* an offline classifier on recorded order streams (rate, bursts, reaction times, focus switches, pick-phase timing) tries to tell humans from bots, tested on human matches held out from persona fitting, with its accuracy reported with a confidence interval. Its value is the features it leans on: they show what still gives bots away.
- *Blind playtest (BotPrize style):* in friends' playtests, some seats are people, some bots, all with neutral names and bodies. After each match every player marks each other commander "person" or "bot". Humanness = share of votes calling bots human divided by share calling people human; target 0.7 first, 1.0 later. Kort's votes are counted apart and never decide the pass.
- *Outside clip test:* the audit's 20-clip test, judged by three or more people who did not play those matches (fail at 15+ correct, pass at 13 or fewer); every "what gave it away" note is a defect.
- *Budget:* AI share per tick at 4v4, 40 slots, reported every sim.

## 5. Ranked build plan

**Now (approved: measurement and fixes in today's code)**
1. **Measure, explain, record.** Sim metrics, a "why" line per bot order, the dev overlay (being built now), and a bots-only decision recorder; recording people waits for question 4. S.
2. **Fix today's worst moments** in `Campaign.botDecide`: per-group targets, guard a taken point, gather reinforcements, escort clears the wagon, face the target, no Retreat under fire. One small human touch fits here: spread a bot's orders over 1-3 s. M.
3. **Balance gates measured seat-swapped** (Kort's levers). S.

**After the port (with the economy)**
4. **Shared troop layer and one order path** (Sergeant, Soldier, spatial grid, level of detail, `Orders.issue`). Players benefit first. L.
5. **Brain core with the economy:** perception, objectives, situation, planner and council, Hands, quartermaster, journal; sims play whole best-of-5 matches. L.
6. **Plays, tells, feints, bait; the voice.** M.
7. **Personas, mistake catalogue, difficulty rows; style fitting from recordings; opening book.** M.
8. **Embodied commander** (if approved), after the Warlord animation chain. L.
9. **Proof rounds:** machine judge, exploiter ladder, blind playtest, outside clip test; at most two fix rounds, then remaining notes go to Kort. M.

## 6. Questions only Kort can answer

1. Should bot commanders have a body on the field like a player: one you can see, chase and kill, that stops giving orders until it respawns, and that wears the room's gear and uses relics? (This changes the approved controls spec, which says a bot has no body.)
2. Should bots be marked as bots in names and the scoreboard, or look like players (at least for the blind playtests)?
3. May bots type short chat lines to their team, and should they sound in character (medieval captain) or like a normal player?
4. May we record your and your friends' matches (orders, timings and picks only, kept on your machine, names replaced) to give the bots human styles?

Already decided and kept: bots wear gear matched to the room's gear power; no routing; no LAST STAND label.

## 7. Review (one fresh Sonnet 5.5 high review, 2026-11-10)
Fourteen findings, all applied: code line and bot gear/relic gates corrected; order range marked as planned; body flagged as a polish-spec amendment with a feasibility spike and its combat sized separately; false retreat dropped; plays scheduled as one focus episode with jittered triggers; reinforcement gathering kept as a planner trigger; internal Move kept; budget restated as mean plus spike, to be measured; recorder bots-only until consent; machine judge made a diagnostic; pings via a `Squad` overload; the gear question removed as already decided.
