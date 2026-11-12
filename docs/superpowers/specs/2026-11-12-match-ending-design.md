# Match ending: design

Date: 2026-11-12. Author: ending lead (Opus 5.5 high), branch `ui/match-ending` from `main` 3f732f0e. Status: **awaiting Kort's approval**. Kort's answers (2026-11-12) are marked "Kort"; everything marked "my call" is mine and open to correction.

Supersedes section 2 (Match end, F1) of `2026-11-10-match-flow-polish-design.md` (8 s "Ended" + 4 s best-commander orbit + results in camp). Kort's 2026-11-12 answers replace it: a freeze, a top-three podium, a stats screen and a rewards count-up. That spec's other sections (room page, Play again, surrender, leave) are untouched.

## 1. Why

Kort, playtesting `main` on 2026-11-12: "right when the match ends im immediately brought here, idk if i won idk if i lost, there was no end screen, there was no podium ... i want the ending to tie up all loose ends not just cause more." Today `Campaign.finish` pays, plays a sting and calls `returnLobby` in the same tick; the player lands in camp on a report page whose loss headline reads "BATTLE COMPLETE".

Success: a player always knows within one second whether they won, lost or drew; sees who carried the match; can read what they and their army did; sees what they earned; and lands back in their room with nothing left unexplained.

Non-goals: Play again, surrender, round-based (best of five) matches, rebalancing rewards, new currencies (the army rework owns those).

Anti-goals: anything that can lose or double-pay a reward; an ending that takes longer than about 30 s without a way to skip; an ending that differs between players in the same match.

## 2. Today (evidence)

- `Campaign.finish(winner)` (Campaign.java 603-631): per participant pays gold, Forge marks, XP, House shards and Renown, mastery, March and Pacts, saves the profile, builds the `results` JSON report, posts a standing line, plays `Audio.ending`, then `returnLobby()`.
- `returnLobby()` (196-212): teardown, discards units and bots, teleports everyone to camp, saves the world.
- Client: `CampaignClient.accept` opens `CampScreen("results")` (WarTableReport) when a new result id arrives and the round is not running. "Last battle" on the War Table home reopens it (`WarTableHome` 200, `CampScreen` 115).
- Freeze precedent: units, projectiles, commanders, morale, capture and mode ticks already run only while `time >= introUntil` (GravesAndGlory.java 841); `OrderRules` and `Commander` refuse during the intro.
- Tracked per army (`MatchStats.SquadStats`): `goldEarned`, `kills` (troops and commander together), `lost`, `flankHits/flankShots`, `objectiveSeconds` (capture points only), `traitSeconds`, `troopKills` by troop; `Squad.fallen` by troop; per team per point hold seconds (`pointSeconds`); the mode ledger (`reportMode`: score, stages, convoy progress, waves). Capture and order events reach `Campaign.progressEvent` but are only counted into daily contracts.
- MVP today: most kills, then objective seconds (`MatchStats.mvp`).

## 3. The sequence (Kort: auto, with skip; about 20 to 30 s)

| Step | Time | What the player sees |
|---|---|---|
| 1. Result | 0 to 6 s | Everything freezes (Kort): troops, arrows and bot armies stop dead in the pose they were in. A large **VICTORY**, **DEFEAT** or **DRAW** (Kort) lands at the centre of the screen with the sting, the final score under it ("Dawn 300 / Dusk 214" or the mode's own line, e.g. "Keep taken at 2:41"). The player can walk and look (Kort) but cannot strike, order, buy or be hurt. |
| 2. Podium | 6 to 14 s | Camera cuts to the shared podium stage (Kort). The top three commanders of the whole match, both teams (Kort), stand on plinths 1, 2, 3, each in their gear with a few of their troops, in their side's colours, with name and headline stats (Kort). The MVP stands on the tall centre plinth. |
| 3. Stats | 14 to 24 s | Full screen over the dimmed podium: the scoreboard of every commander, then the player's four groups (commander, army, objectives and mode, economy and orders; Kort). |
| 4. Rewards | 24 to 30 s | Each reward appears with its reason and counts up, one at a time, then a total; under it, contracts and chapter tiers ready to claim (Kort). |
| 5. Back | 30 s | Back to the room (Kort): the war council page if the player is in a room, else the War Table home in camp. The report stays reopenable from "Last battle" (Kort). |

Timings live in `sheets/design.json` (`ending` sheet: `resultSeconds` 6, `podiumSeconds` 8, `statsSeconds` 10, `rewardsSeconds` 6; my call on the split of Kort's 20 to 30 s). Rewards extends itself by 0.4 s per row past six rows, capped at 10 s, so a campaign win with a bonus, a Pact and a title is not cut off (my call).

**Skip** (Kort: a key skips ahead). The intro's skip keys, `{key:menu}` or Escape, move this player alone to the next step (my call: same keys as the entrance so players learn one). Hint line at the bottom of every step: "{key:menu} or Escape to skip". Skipping the rewards step returns that player to the room at once. Nobody else's sequence changes. When every human participant has reached step 5 the server ends the sequence early.

**Draw** (Kort): DRAW banner, same sequence. The podium and MVP still count, since the match score does not depend on the winner.

**Player who is out early** (Kort): a commander waiting to respawn (spectator) or one who has dropped and come back stays in the arena as a spectator and gets the full sequence. A participant who disconnects keeps their reward (paid at step 1 as today) and gets the report on return, as today. Players who were never seated (late joiners waiting in camp) do not get the sequence; they see the headline line as today.

## 4. Match score and MVP (Kort: a match score from kills, objective work and troops kept alive)

```
score = 10 x enemy troops killed by your army
      + 30 x enemy commanders you downed yourself
      +  1 x objective seconds
      + survival bonus = round(100 x kept / (kept + lost)), 0 when the army fielded nothing
```

- `kept` = your troops alive at the end (temporary troops excluded); `lost` = your troops that died.
- Objective seconds per mode (my call; new tracking for Siege, Convoy and Survival, see 5): Domination, seconds with a troop or the commander on any point (today's `objectiveSeconds`); Siege, seconds on the keep point or at a breach stage, attackers and defenders alike; Convoy, seconds within the wagon's escort or block radius; Survival, seconds within the shrine radius.
- Weights are my call, sized so one minute of holding (60) is worth six troop kills and a full-match holder in Domination (about 400 s) ranks with a 40-kill fighter. In `sheets/design.json` (`ending.killWeight` 10, `commanderWeight` 30, `objectiveWeight` 1, `survivalMax` 100) so playtesters can tune them.
- MVP = highest score. Ties: more kills, then more objective seconds, then the winning side, then the earlier seat (my call). Replaces `MatchStats.mvp` (the report's MVP mark follows it).
- Bots are scored like players and can be MVP and stand on the podium (Kort: everyone, both teams).

## 5. Stats (Kort: all four groups)

"New" means not tracked today and added by this work. All per round, per army, in `SquadStats` unless said.

| Group | Stat | Source |
|---|---|---|
| Commander | Kills (enemy troops and commanders by your own blows and shots) | **New** `commanderKills`, `commanderDowns` split at the kill credit in `GravesAndGlory` 177-178 |
| | Deaths (times you fell and respawned) | **New** `deaths`, at `respawns.put` (1330) |
| | K/D | Derived; deaths 0 shows kills as is ("12 / 0") |
| | Damage dealt (by you, to enemy troops and commanders) | **New** `damageDealt`, at the commander's hit path (GearCombat / player attack) |
| Army | Kills, losses | `kills`, `lost` (today `kills` includes the commander's; the army line shows troop kills = `kills - commanderKills`) |
| | Per troop type: fielded, kills, losses | `troopKills`, `Squad.fallen`; **new** `fielded` by troop (spawn and refill count) |
| | Best troop | Derived: most kills, ties fewer losses |
| | Kept alive at the end | **New** `kept`, counted at step 1 |
| Objectives and mode | Captures (points you took) | **New** `captures`, at the capture event that feeds `progressEvent(capture)` |
| | Hold time | `objectiveSeconds` (extended per mode as in 4); team hold per point from `pointSeconds` |
| | Stages, convoy progress, waves, final score | Mode ledger (`reportMode`), already sent |
| | Match length | `seconds`, already sent |
| Economy and orders | Gold earned | `goldEarned` |
| | Gold spent | **New** `goldSpent`, at training and Armoury buys |
| | Troops trained | **New** `trained`, at `training.addLast` |
| | Upgrades bought | **New** `upgrades`, at `Veterans.buy` success |
| | Orders given | **New** `orders`, where `OrderRules` calls `progressEvent(order)` |

The scoreboard (top of step 3) shows every commander of both teams, your team first: name, House, score, commander K/D, army kills, losses, objective seconds, MVP mark. The four groups below are your own.

## 6. The podium stage (Kort: in the world, one shared stage, commander with a small squad)

- **Where** (my call): one stage built in the same world at a fixed spot outside every map and the camp: 160 blocks beyond the camp island's far edge (camp `originZ` -310), above the flat world, built once at server start like the camp (`CampWorld`) and rebuilt if missing. No map owns it, so the map redesigns do not touch it. Exact coordinates are fixed in the plan after checking the camp and arena bounds.
- **Look** (my call, inside the parchment / dark wood / brass-frame direction; no brass on table wood): a dark stone dais with three oak plinths (centre tallest), a stone backdrop wall with two hanging banners in the MVP's side colours, torches and lanterns, night-dark sky. Built from vanilla blocks with the existing prop kit where a prop fits.
- **Camera**: at step 2 the server moves each participant to a viewing spot in front of the stage, invisible, as a spectator. The client locks the camera to a fixed shot of the stage with a slow 8 s push-in (my call), letterboxed like the entrance.
- **Figures**: client-side only (my call): no server entities, nothing to clean up, and every client draws the same three from the same data. Each commander is a player figure built from their game profile (their skin) wearing their gear (the same `CommanderGearFeature` draw), in a victory pose for the MVP and a standing pose for 2 and 3. A bot uses a plain commander figure in its side's cloth with its tier's gear. Around each commander stand up to three of their troops (my call: the troop types with the most kills; a commander with fewer kills fills from their banner order; one with an empty banner stands alone), drawn by the troops' own renderers in that side's colours, frozen in their idle pose.
- **Name plates**: drawn on the HUD under each plinth, projected from the plinth's position: rank, name, House crest, score, then the stat that earned it (my call: the highest of kills, objective time, troops kept; e.g. "38 kills" or "4:12 on points").
- **Fewer than three commanders** (practice 1v1, a 1v1 room): only the plinths with a commander show; the empty ones are bare. One commander: the centre plinth alone.

## 7. Multiplayer and sync

- The server owns the clock. At step 1 it records `endingStart`; every client's step is derived from it, so all players see the same step at the same time (within the 0.5 s state interval; the client interpolates).
- Per-player skip: the client sends `ending:skip`; the server raises that player's own step (`max(time step, skipped step)`) and moves only them (to the stage at step 2, to camp at step 5). Skips never affect others.
- The server tears down (`returnLobby`) when the clock reaches step 5 or every human participant has.
- During steps 1 to 4: `running` stays true for the purposes of seats, rejoin and leash, but a new `ending` flag stops every battle tick (as the intro does), refuses orders, abilities, buys and damage, and the round clock stops. Units get `setAiDisabled(true)` and zero velocity; projectiles in flight stop where they are (they are not ticked) and are cleared at teardown.
- Client animation: troop and commander animations pause at the freeze moment (statue look; my call). If a troop's renderer cannot pause cleanly, it falls back to idle and that is reported.
- Rewards are paid and saved at step 1, exactly once, as today. A server stop or crash during steps 1 to 4 loses nothing: the rewards are saved and the report is in `results`.

## 8. Server and protocol changes

- `Campaign.finish`: split into `settle(winner)` (today's pay-and-report, unchanged amounts) and `beginEnding()`; `returnLobby` moves to `endEnding()`. The reward block is not edited beyond the call split; the report gains new fields only.
- New pure class `MatchEnding` (no Minecraft types): step timings, per-player step with skip, the score, MVP, podium pick (top three, troop picks, fewer-than-three), K/D text. Unit-tested in the standalone checks.
- `SquadStats`: the new counters in 5.
- Camp snapshot (`snapshot` JSON, sent every 10 ticks): new `ending` object `{step, ticksLeft, outcome}` while the sequence runs. The `results` report gains `score`, `mvp`, `podium` (three rows: name, uuid, bot, team, House, gear ids, troop ids, score, headline), `you` (the four groups) and `rewards` (an ordered list of `{label, amount, reason}` rows). Older clients ignore unknown fields. The report's loss outcome becomes `DEFEAT`.
- New client to server command `ending:skip` on the existing camp command channel.
- `sheets/design.json`: the `ending` sheet (timings and score weights); `Spec.java` regenerated by `./gradlew compileJava`.
- Snapshot size: the server already drops a snapshot it cannot fit (`{}`); the report's added size is measured in a check against the limit with a full 4v4 match.
- No save schema change: nothing new is stored in profiles.

## 9. Rewards step and the army rework

The army rework (`bb-army`, `army/war-table`) replaces saved collection gold with troop writs and gear shards, renames `writs` to `pacts`, and edits `Campaign.finish`, payouts and `WarTableReport` (near 337, 535). To keep out of its way:

- `finish` builds an ordered `rewards` list in the report, one row per paid item with its reason: e.g. "Troop writs +3 / match complete", "Troop writs +1 / victory", "Gear shards +40 / match complete", "Order shards +12 / House slots", "Renown +5 / Order", "Forge marks +100", "XP +250 / chapter tier 4 in reach", "Bonus objective / +20 Grave shards", "A Pact", "New title: Order Squire Veteran". The rewards step draws whatever rows it gets.
- Built on `main` today, the rows read gold and Forge marks; when the army branch lands, its payout lines add writ and gear-shard rows in the same list. I message the army lead before touching `finish` and keep my change there to the split and the list.
- Under the total: "Ready to claim: 2 contracts / chapter tier 4" with a link to the Contracts page, read from `Progression.claimable`.

## 10. The report afterwards (Kort: back to the room, report reopenable)

- Step 5 opens the war council page (`lobby`) for a player in a room, else the War Table home. (Today's "Return to room" opens the Friends card; this fixes that for the ending.)
- "Last battle" opens the report as today. The report's Battle face shows the new scoreboard with scores and the MVP; a new **You** face shows the four groups; the rewards rows replace the note's rewards block. The stats and rewards drawing is shared code (`EndingPanels`) used by both the ending screens and the report. Edits in `WarTableReport` are kept to adding the face and calling the shared panels, and coordinated with the army lead.

## 11. What it touches

`Campaign` (finish split, ending tick, skip command, snapshot), `GravesAndGlory` (ending gate on the battle tick, stat hooks), `OrderRules`, `Commander`, `Veterans`, `GearCombat` (refusals and counters), `MatchStats` (counters), new `MatchEnding`, `EndingClient` (camera, banner, podium figures, plates), `EndingScreen` (stats and rewards), `EndingPanels`, `PodiumStage` (the build), `WarTableReport` (face and panels), `CampaignClient` (open on step 5 instead of on return), `sheets/design.json`.

Not touched: the battle HUD and troop pages (UI lead), map files, reward amounts.

## 12. Checks

- Standalone checks for `MatchEnding`: score formula, MVP ties, podium with 0 to 8 commanders, bots, empty banners, troop picks, step clock with skips, K/D text, reward list order.
- `tools/verify.py fast` and `game` green.
- Game harness runs (`tools/checkq.py`), one per mode (Domination, Siege, Convoy, Survival), each ended as a win, a loss and a draw where the mode has one: asserts the step order, that units and projectiles do not move during steps 1 to 4, that no damage lands, that rewards are saved once, that the report carries score, podium and rewards, and that everyone is in camp at step 5.
- Two local clients in one room: both see the same step at the same time (logged step changes within 0.5 s); one skips, the other's sequence is unchanged; the skipper lands in the room first.
- A respawning (spectator) commander at match end gets the full sequence.
- In-game screenshots of every step, and a short clip of the whole sequence, for Kort.

## 13. Left to the builder

Exact stage coordinates and block palette; the push-in curve; the victory pose; the text sizes on screen (to the War Table's existing scale rules); the order of reward rows within a reason.
