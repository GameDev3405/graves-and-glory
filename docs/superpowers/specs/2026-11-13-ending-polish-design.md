# Match ending polish (2026-11-13)

Follow-up to `2026-11-12-match-ending-design.md` (landed on main). Kort picked all four bundles on 2026-11-13: "Fixes + awards", "Sound", "Reveal + progress bars", "Graph, bests, play again". Branch `ui/ending-polish` in `bb-ending`. Calls marked *(lead's call)* are mine and open to Kort's correction.

## Goals

The ending should feel finished: it reacts on the exact tick the match ends, people on the podium are celebrated rather than standing still, the screens have no empty space, rewards show progress, and the sequence leads naturally into the next match.

Non-goals: a replay or kill cam, new animation clips (the animation lead owns clips), new art beyond UI drawing, any balance change to rewards.

## 1. Fixes

- **Instant freeze.** The server sends a small `ending_begin` S2C packet in `Campaign.beginEnding` with outcome and line. The client freezes animation and shows the banner on receipt and no longer waits for the next snapshot. The snapshot stays the source of truth for later steps. Protocol addition: approved by Kort's pick.
- **Podium poses** *(lead's call)*. Player figures: rank 1 raises the weapon arm high, ranks 2 and 3 hold the weapon arm across the chest, a 4th or later rank stands. All poses are made with arm and head angles on the existing figures. Troops play their existing idle or stance clip, never frozen mid-step. No new clips.
- **Stats screen fill.** Below the four groups: "Your army" (troop rows with Fielded, Kills and Lost, the same table as the report's You page) on the left, and "Highlights" on the right (the awards earned this match, up to three lines).
- **War Table rewards box.** It lists each reward row in two compact columns and falls back to the wrapped total only when the rows don't fit.

## 2. Awards *(lead's call on names and rules)*

Computed on the server from the per-squad stats, once, at `finish`. Each player gets at most one award: the first in this order that they win outright, with ties going to the higher match score.

| Award | Won by | Floor |
|---|---|---|
| Kingslayer | most enemy commanders downed by your army | at least 1 |
| Duelist | most commander kills by your own hand | at least 1 |
| Banner Breaker | most captures | at least 1 |
| Holdfast | most objective seconds | at least 30 s |
| Butcher | most army kills | at least 5 |
| Iron Wall | best kept ratio | at least 10 fielded |

Report: `squads[i].award` is set when the player has an award, and the podium rows carry the same award. Shown as a third line on the podium plate, a chip on the scoreboard row and a line in Highlights. A pure `EndingAwards` class has a standalone check.

## 3. Sound

There are seven cues:
- `ENDING_DRAW`
- `PODIUM_DRUM`
- `PODIUM_REVEAL`
- `REWARD_TICK`
- `REWARD_TOTAL`
- `LEVEL_UP`
- `AWARD`

The existing `VICTORY` and `DEFEAT` cues play with the banner. The audio lead makes the files and adds the cues to `AudioCues` on its own branch. This branch plays the cues by name through `EndingSounds`, which plays nothing for a cue that doesn't exist yet. The audio lead's landing then switches them on without touching our files.

## 4. Reveal and progress bars

- **Podium reveal** *(lead's call)*. The 8 s podium step opens with the stage dim and the figures hidden:
  - 3rd place appears at 1.5 s, 2nd at 3.0 s and the MVP at 4.5 s.
  - Each appears with its plate and `PODIUM_REVEAL`; the MVP also gets `AWARD`.
  - The camera dollies 2 blocks in and drifts 6° of yaw over the step.
  - With fewer than three people, the reveal still ends with the MVP at 4.5 s.
  - Skip shows everything at once.
- **Progress bars.** The report gains `progress`: a list of {label, before, after, next, tier}.
  - The bars are XP to the next level, Renown to the next tier for each House that earned Renown, and contract progress.
  - Bars fill after their reward row has counted up. When one crosses its threshold it flashes, reads "Level up!" or "Tier N!", and plays `LEVEL_UP`.
  - The REWARDS step stays within its existing maximum length.

## 5. Graph, bests, play again, left tag

- **Score graph.** The server samples each team's mode score every 5 s of battle time (at most 120 samples: past that every second sample is dropped and the interval doubles). The report gains `timeline` {interval, label, teams: [[...],[...]]}. It is drawn on STATS as a small two-line chart in team colours with the final score at the right edge. A match shorter than 15 s shows no chart.
- **Personal bests.** The profile gains `bests` (kills, armyKills, score, keptPct, captures, commanderDowns), with a default of empty, so no migration is needed beyond the field default. A value counts as a best only when an earlier record exists, so the first match sets records quietly. The report gains `bests`: [{label, value, previous}], shown on REWARDS above the claim strip ("New best: 14 kills (was 11)"). A history list is not stored. *(Lead's call: bests only, no history, to keep the save small.)* This is a save format addition, approved by Kort's pick, and it gets a ProfileMigrationCheck case.
- **Play again.** The DONE step already returns the player to the room. The rewards screen gets a "Play again" button (or its rebindable key, J by default: P is vanilla's Social Interactions; unbound, the button shows no key) that skips to DONE and marks the player Ready in the room. The room's existing all-ready ten-second countdown does the rest. No new vote system.
- **Left the battle.** Participants who were offline at `finish` get `squads[i].left = true`. The scoreboard row and the podium plate read "Left the battle", and such a player cannot win an award or MVP.

## Constraints

These are carried over from the ending work:
- Never hand-edit `Spec.java`.
- Never write `eula.txt`.
- Never kill a running client; at most five clients on the machine.
- Run game suites only through `tools/checkq.py`.
- Do not touch the battle HUD or troop pages.
- Keep edits to `WarTableReport.java` and `Campaign.finish` small, because the army rework edits them too.
- No push, merge or publish.
- Worker routing follows `WORKFLOW.md`: Opus medium for screens and the podium, Sonnet high for the review.

## Passes

1. **Fixes and awards:** the ending_begin packet, poses, the stats fill, the War Table box, `EndingAwards` with its check, the plate, chip and Highlights, the left tag, and the `EndingSounds` hooks.
2. **Reveal and progress bars:** the podium reveal and camera, `progress` in the report, and the bars and flashes on REWARDS.
3. **Graph, bests and play again:** `timeline` sampling and the chart, profile `bests` with its migration check, and the Play again button.
4. **Finish:** one Sonnet-high review (at most two fix rounds), `verify fast`, the `ending` and `ending-sync` suites, and screenshots of every new element at normal and small sizes.
