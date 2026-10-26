# Task 4 report (lane A): kill credit and levels

- **Commit:** `c3282e1` on `phase-2-houses` (worktree `bannerbound-p2`), on top of `b578827`. 4 files, +607/-17. The tree is clean. Committed by path with the message in `bannerbound-p2/work/p2/t4/commit-msg.txt` and the trailer `Co-Authored-By: Claude Opus 5.5`. Not pushed, not merged.
- **Executor:** Opus (lane A). Two harness clients, one after the other, under `work/p2/t4/`. The slot check passed each time: Kort was not playing, and at most one other work client was running.
- **Files:** only lane A files. `Veterans.java`, `GravesAndGlory.java`, `Campaign.java` and `test-tools/.../LevelChecks.java`, (`graphify-out/` is git-ignored in this worktree, so the rebuild is not in the commit). Nothing frozen was touched (sheets, `Spec.java`, `preflight.py`, `LevelMath`, `CreditLog`, `RankSync`).

## What changed

| File | Change |
|---|---|
| `Veterans.java` | **`onDeath(b, victim, attacker, winner)`** runs from AFTER_DEATH, ABOVE `if(victim.temporary)continue;`. Through a try/finally it always forgets the victim's log. It does nothing when the winner is missing or on the victim's own team. It finds the killer among `b.units` with `Combat.unitOf(b, attacker)`: he must be alive, not temporary, and on the winner's team; if not, the killer is null. The bounty is `LevelMath.bounty(victim.level, false, victim.temporary)`.<br>**`onCommanderDeath(b, commander, team, attacker)`** is called from the `allowDamage` respawn branch. It finds the striker among `b.units`. The side is the striker's team, or else `b.teamOf(attacker)`, or else the commander's enemy. The bounty is `bounty(1, true, false)` = `progression.commanderBounty`. It always forgets the commander's log; with no attacker (a fall, lava) it credits nobody.<br>**`award`** credits every id that `CreditLog.credited` returns, but only a unit found in a copy of `b.units` that is alive, not temporary and on the credited team. It then calls `Abilities.onKill(b, killer)`.<br>**`credit`** adds the credits and applies every level reached (9 at level 1 gives two level ups).<br>**`levelUp`**, per level: `healthStep` scales the max-health base value by `healthFactor(level)/healthFactor(level-1)` (stars stay on top) and raises current health by only the added amount. It then sets `u.level` and sends `RankSync.changed` (rank_v1). Once per level up: the custom name becomes `b.troopName(...)` (level name plus Veteran suffix), the flash plays, `Abilities.rowsChanged(u)` runs, and one `Feed(team, levelName)` line is queued in `pendingFeed` when the troop reaches `progression.feedLevel` (Task 12's HUD reads it; `clear` empties it).<br>**The flash** is server-side side-coloured dust: a 16-point ring at the feet and a 30-particle column. With it plays `entity.player.levelup` at volume 1.5 (it carries 24 blocks), pitch 0.9, category HOSTILE. No new packet.<br>**`enabled()`** reads a volatile `override` (harness, and the Task 5 sim switch), else `progression.enabled`. `helped` and `supported` are now guarded by it too.<br>**A bounded `LOG`** (256 events: kill, credit, level, rank, feed) for the harness. It prints `[levels]` only under Diagnostics. |
| `GravesAndGlory.java` | The AFTER_DEATH loop iterates a copy of `battle.units`. The winner and troop lookup moved above the temporary skip, then comes `Veterans.onDeath(...)`. Gold, kills, Mastery, lost and fallen are unchanged and still skip a temporary victim. Mastery still counts the killing blow only. The `allowDamage` player-death branch calls `Veterans.onCommanderDeath(this, p, team, attacker)` before `return false`. The new `Battle.troopName(squad, spec, level)` is shared by `spawn` (level 1) and `levelUp`; the name-colour rule moved into its javadoc. |
| `Campaign.java` | Notices expire server-side after 10 s with no protocol change: `noticeAt` holds the time `notice()` posted each one. `expireNotices()` runs from `tick()` once a second. A notice put without `notice()` is timed from the first pass that sees it. The same words posted again start a new 10 s. |
| `LevelChecks.java` (harness) | The `levels` suite (replaces the Task 3 NotBuilt skeleton). It runs on the Phase 2 Staged sky fixture with forced Rise Again off by default. It has 21 checks plus `p2-levels-suite`; ids are listed below. The `gear` and `armoury` suites stay NotBuilt (Task 5). |

## Verification

| Command | Result |
|---|---|
| `python tools/preflight.py` | clean, 4893 cells |
| standalone checks (`test-tools/standalone/`): BattleMath, Houses, ProfileMigration, LevelMath | 51, 268, 31, 136 passed, 0 failed |
| **red first:** compiling `LevelChecks.java` against the Task 3 main sources (the three main files stashed) | 36 `cannot find symbol` errors in LevelChecks; stash popped clean. Before Task 4 the suite reported `fail p2-levels-not-built`. |
| `gradle compileJava compileLocalTestTools --offline` | BUILD SUCCESSFUL |
| client A: `GRAVES_PHASE2_CHECKS=levels,chase,graveborn` (`-PcampaignChecks`; root `work/p2/t4/suite-A-142713`) | **55 pass, 0 fail**: every levels check, `p2-levels-suite`, `p2-chase-suite`, `p2-graveborn-suite` |
| client B: `GRAVES_PHASE2_CHECKS=commander,screens,migration` (root `work/p2/t4/suite-B-143158`), run because of the commander-death branch and the notice expiry | **37 pass, 0 fail**, including `p2-notice-clears`, `p2-notice-changes`, `p2-notice-repeats`, `p2-commander-suite`, `p2-screens-suite`, `p2-migration-suite` |
| `gradle build --offline -x test` | BUILD SUCCESSFUL |
| `GRAPHIFY_BACKEND=none graphify update .` | rebuilt: 3731 nodes, 12041 edges, 152 communities |

Levels ids (all pass in client A):
- `p2-levels-sheet`;
- `p2-credit-killer` (also covers an arrow landing after its archer fell);
- `p2-credit-assist` (also covers 11 s falling outside the window and a dead helper getting nothing);
- `p2-credit-support` (mends through `Abilities.tick`; the bystander's mender gets 0);
- `p2-mastery-unchanged`;
- `p2-notice-expires`;
- `p2-credit-slow` and `p2-credit-zone` (a Mist Caller cloud);
- `p2-credit-allied` (a second Dawn army);
- `p2-bounty` (a level-3 victim gives 3 to each);
- `p2-level-skip` (9 credits: 28 to 30.80 max, 20 to 22.80 health, one feed line);
- `p2-rank-sent` (+2 rank_v1 packets; the client holds Rank[3,1,1]);
- `p2-level-up` (10/28 to 11.40/29.40, "Knight");
- `p2-level-name-veteran` ("Knight-Captain (Veteran)");
- `p2-level-flash`;
- `p2-rise-keeps-level` (the same Revenant, 3/2, with no credit and no rank packet);
- `p2-temporary-no-credit` (a Risen earns nothing; a temporary victim counts 1);
- `p2-structure-no-kill`;
- `p2-commander-bounty` (3 each; a second death at once does not re-credit the first death's archer);
- `p2-commander-never-levels`;
- `p2-rebuy-level-1`;
- `p2-progression-off`.

**What `levels-flash` shows** (`suite-A-142713/run/screenshots/p2-levels-flash.png`): a Dawn Squire standing 8 blocks ahead on the grey sky platform, wrapped in a column of gold (Dawn) dust with a gold ring at his feet, plainly readable against sky and stone. The action-bar line "Levels check: this notice expires" in it comes from the notice check 2 s earlier, not from the flash.

## Deviations

1. **There is no bot commander body to kill.** Bots have no entity, so `p2-commander-bounty` uses the harness's player commander, struck down by Dusk troops. `onCommanderDeath` is generic, so a future bot body would call it the same way.
2. **`onDeath` sits after the winner lookup and before the temporary skip,** that is, before the gold, kills and Mastery lines. It reads the same `winner`, so a shot that lands after its archer fell credits that army's helpers. The dead archer gets nothing because he is no longer in `b.units`.
3. **AFTER_DEATH iterates a copy of `battle.units`,** so lane B's `onKill` and `rowsChanged` seams can change the list safely.
4. **The progression-off switch also stops `helped` and `supported`,** so the off baseline does no progression work at all (the Task 11 sim compares against it).
5. **Two ids the plan does not list:** `p2-levels-sheet` (a sheet sanity line) and `p2-notice-expires` (the binding notice note).
6. **The flash lives in `Veterans`,** not `BattleEffects`: no lane owns `BattleEffects`. It also holds the harness counters `flashes` and `lastFlash`.
7. **Notice expiry trade-off:** expiry is checked once a second, so a notice lasts between 10 and 11 s. The same words posted again start a new 10 s and show again, which `p2-notice-expires` and `p2-notice-repeats` check. There is no serial on the wire, so there is no protocol change.
8. **The `levels-flash` picture uses the M8 stopgap `_s2` skin** (Blockbench is down this session).
9. **The plan's Step 4 javac list names `HintText`,** which does not exist. I compiled through gradle instead.

## Bookkeeping (review note M2)

The `rank_v1` CLEAR packet (entity id -1, which clears the client store at round boundaries) and the `Mist.caster` and `Banner.by` components are **Task 3 additions** that the plan did not list. Record them against Task 3.

## In-game checks owed

- **Two-client LAN rank sync:** a second player tracking a troop sees his rank arrive at a level up. The harness checks only the local commander's store. The late-join resend stays owed from Task 3 / Task 10.
- **Flash readability at about 24 blocks in a real fight:** dust and sound in Dusk's violet and Dawn's gold on the real maps. The harness checks it at 8 blocks on the sky platform.
- **The 10 s notice expiry feels right in the camp:** a refused buy's line goes away and does not come back with the next snapshot.
