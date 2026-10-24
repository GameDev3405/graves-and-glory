# Phase 2 balance log

Every House batch of Phase 2 ends with a bot-vs-bot balance gate. This file holds the gate rules, the run recipe, the result of every run and the tuning log. The plan is `docs/superpowers/plans/2026-10-17-phase-2-houses.md` (section "Bot balance run").

## The gate

- A House's `<house>_normal` bot banner (`botBanners` sheet) plays the current best pure normal banner (below). Each batch compares two pure banners.
- 20 matches, 10 per seat: process 1 puts the House's banner on team 0 and process 2 on team 1, so the seat (Dawn's or Dusk's base) cancels out. Normal bots on both seats (`-PsimDawn=normal -PsimDusk=normal`), maps 0 to 4 (`-PsimMaps=0,1,2,3,4`), 300 s rounds.
- P is the House banner's share of the decided rounds it won over the 20 matches; a draw counts half and an aborted round is left out (`tools/balance_gate.py` names how many). Pass: 40 <= P <= 60.
- On a fail, tune only that House's rows in `sheets/design.json`, keeping each number within 15% of its spec value, run `python tools/preflight.py`, rerun both processes and log every change in the tuning log below.
- The current best pure banner is the pure normal banner with the highest gate win rate logged so far.
- Phase 2 Task 3 has no gate: it logs the baseline only.

## Run recipe

Two background processes at once, on ports 25601 and 25602, each with its own fresh server root (a copy of `eula.txt` and `server.properties`, `level-name=Graves and Glory`, with the process's `server-port`). A run takes about an hour in real time (ten matches of 5 minutes of round time plus the arena build, per process).

```bash
G=$(ls -d ~/.gradle/wrapper/dists/gradle-8.8-bin/*/gradle-8.8/bin/gradle | head -1)
export GRAVES_ROADMAP_SERVER_ROOT=<root1>   # then <root2>, with server-port 25602
"$G" runServer --offline -PbotSim=10 -PsimDawn=normal -PsimDusk=normal -PsimMaps=0,1,2,3,4 -PsimRoundSeconds=300 \
  -PsimBanner0=<a> -PsimBanner1=<b> --init-script test-tools/isolated-server.gradle   # process 2: -PsimBanner0=<b> -PsimBanner1=<a>
python tools/balance_gate.py <root1>/botsim/results.csv <root2>/botsim/results.csv <a> [--detail]
```

The gate prints `gate <a> vs <b> win=<P>% pass|fail` (exit status 0 pass, 1 fail, 2 unusable input), preceded by a line `aborted=<N>` of its own when any round was aborted; `--detail` adds the per-run and per-map tallies. Each run's `botsim/results.csv` names the banners fielded in its `banner0` and `banner1` columns, and the `[botsim]` summary line ends with `banner0=<id> banner1=<id>`.

## Current best pure banner

`gilded_normal` (82.5% against `graveborn_normal` in the Task 8b gate on the no-routing game, the baseline from here on; 52.5% in the balance fix of 2026-10-22 with routing; 85% in the Task 7 gate at the spec numbers; 100% in the Task 6 gate, report-only, before the Graveborn mechanics; 92.5% in the Task 3 baseline after fix round 1). The first Task 3 baseline (`gilded_normal` 95%) was measured with the husk's +2 armour on the Gilded side and is superseded by it. Update this line whenever a logged gate gives another pure normal banner a higher win rate. (`ironclad_normal` read 60% against `gilded_normal` in the Task 8b gate, 75% in the balance fix with routing and 45% in the Task 8 gate, so Gilded stays the bar.)

## Results

### In-match progression gate (2026-10-24; PASS at the as-designed 3 and 8, at 4 and 12 and at 5 and 14; shipped at 5 and 14; Task 11 of the in-match progression plan)

The question (design 9.2 of `docs/superpowers/specs/2026-10-22-in-match-progression-design.md`): do levels, the Armoury gear and the veteran bonuses shift the Houses' balance or the comeback rate? Measured with Kort's round-robin method (round 34 answer 1) on fast ticks, 200 rounds per condition, plus one real-time pair. Rules: every House within 10 points of its own levels-off share, and the pooled comeback rate (rounds won by the side trailing at 300 s) with levels on no more than 10 points below off. Result: both rules pass at every threshold tried (largest share move 4.9 points, comeback -4.5 points at worst). Kort's round-35 answer raised the level kill thresholds in two steps (3 and 8, then 4 and 12, then 5 and 14) because the levels arrived early in the sim; at 5 and 14 the first level 2 (120 s) and the winner's level-2 share (35.5%) are inside their aims, the level 3 count (27 a round) is not and is a question for Kort. Fast ticks do not reproduce real time on the Gilded and Graveborn pair (below). Houses outside the 40 to 60% band in both runs (Graveborn, Ironclad, Tidecallers, Wildmoot) go to the Phase 2 end balance pass.

#### Code, tools and method

- Branch `progression-gate` in the worktree `bannerbound-gate-prog`, cut from `cdea2c4` (the whole-plan review fix round 1 of the in-match progression plan, Tasks 1 to 10 merged). Commits on it, in order: `1e708db` `tools/balance_method.py` and `tools/balance_sim.sh` (copied unchanged from the untracked working tree of `bannerbound-method`, base `5a88026`; their own commit, as the plan asks); `74bb8a8` the sim speed and balance-method test tools from the same tree (`-PsimFast`, `-PsimOverride`, the duel simulation, the type ledger; `build.gradle`, `BotSimulation`, `DuelSimulation`, `SheetOverride`, `TypeLedger`, `SimSpeed`, `FastTickMixin`); `d8bb4c4` `tools/progression_gate.py` and `test-tools/standalone/test_progression_gate.py` (red first: tool missing, then 13 tests OK); `a59efab` `SIM_LEVEL_SEED` in `balance_sim.sh` (a fixed world seed per run, vanilla `level-seed`); `ceba5bd` the gate prints the raw seat result; `522c856` the 4 and 12 thresholds below.
- Method (round 34 answer 1, Kort's round 32 round robin): every ordered pair of the five `<house>_normal` banners (20 pairs), 10 matches per pair, maps 0 to 4 twice (`-PsimMaps=0,1,2,3,4`), normal bots on both seats, `-PsimRoundSeconds=900 -PsimFast=true` (with levels on the bots buy gear on their own: `botbuy` lines in the log), once with `-PsimProgression=off` (baseline) and once with `-PsimProgression=on`. A job is `tools/balance_sim.sh <name> <port> -PbotSim=10 -PsimDawn=normal -PsimDusk=normal -PsimMaps=0,1,2,3,4 -PsimRoundSeconds=900 -PsimFast=true -PsimProgression=<off|on> -PsimBanner0=<a>_normal -PsimBanner1=<b>_normal`; it makes a fresh server root `work/method/<name>-<HHMMSS>` and prints it. The queue runner (`work/gate/runq.py`, not committed) ran two jobs at once on ports 25741 and 25742, off and on of a pair side by side, kill-safe on a 5400 s budget, retrying a job once when gradle did not exit 0.
- Seeds: the world seed of each ordered pair is `9000 + k` (`SIM_LEVEL_SEED`, k = 1 to 20 in the order gilded-graveborn, gilded-ironclad, gilded-tidecallers, gilded-wildmoot, graveborn-gilded, and so on, houses alphabetical), the same in the off job, the on job and every threshold setting, so a pair's maps are the same terrain in every condition. The real-time pair uses 9901. The bots, the combat and the vanilla entity AI are not seeded, so a seed fixes the world and nothing else (see Determinism).
- Interval method (`tools/balance_method.py rr`, used by `tools/progression_gate.py`): Bradley-Terry with a seat term, logit P(team 0 wins) = s_i - s_j + h, the ratings s summing to 0 (weak normal prior, SD 3), a draw counting half, an aborted round left out; mirror pairs (a House against itself) only inform the seat term h. A House's share is its expected share against the other four Houses (equal weights). The 95% intervals come from the information matrix; the interval of a difference on - off uses the delta method on the two independent fits. A House's rule line reads "interval reaches past the limit" when the point difference is inside 10 points but the interval is not, so the pass is a point estimate. The comeback rate is a pooled proportion with a Wilson interval; the difference interval is the normal approximation.
- Comeback pool rule (decided here, documented in the tool): a round counts when it was decided (a winner, not a draw, not aborted), lasted at least 300 s, and the scores at 300 s (`score300_0`, `score300_1` in `results.csv`) are not tied. Its numerator is a round won by the side that trailed at 300 s. A tie at 300 s is left out (nobody trails, so there is nothing to come back from or to hold); each exclusion is counted and printed. Rounds under 300 s still count in the shares (a quick win is a win).
- Rules (design 9.2): pass = every House's share with levels on within 10 points of its own baseline share AND the pooled comeback rate on no more than 10 points below off. A House inside 40 to 60% in both runs is balanced; outside it in both goes to the Phase 2 end balance pass. The level aims (first level 2 at 2 to 3 minutes; about a third of the winner's living soldiers at level 2 or higher at the end; a few level 3s per round) are a tuning aim, not pass or fail. Tuning, only on a fail: `sheets/design.json`, the levers of design 9.2 in order (thresholds, gear prices, gear percentages, level bonuses), at most two rounds, each followed by `python tools/preflight.py` and the standalone checks.

#### Smoke (900 s, fast, one match each, map 0, random world seed; `work/gate/smoke-table.txt`)

Each House's `<house>_normal` on `-PsimProgression=on` against `gilded_normal` (Gilded against itself for the first row), one off baseline, and one repeat. All gradle exits 0, no exception in any log, `levels.csv` filled with levels on and blank with levels off (the off baseline reports 0 for every column, 0 botbuy lines), `gradle-run.txt` has 28 to 41 `botbuy` lines per match with levels on.

```
job                      wall  winner  seconds  score     botbuy  first L2 / first L3 / end share L2+ / level 3s   (dawn ; dusk)
smoke-on-gilded          301   dusk    705      139-302   34      107/115/0.442/30 ; 106/134/0.326/35
smoke-on-graveborn       176   dawn    645      300-123   28      97/120/0.560/30  ; 101/110/0.080/33
smoke-on-ironclad        177   dawn    645      301-111   39      124/140/0.421/27 ; 131/155/0.037/17
smoke-on-wildmoot        192   dusk    529      43-300    41      214/218/0.263/5  ; 87/98/0.825/45
smoke-on-tidecallers     315   dusk    529      17-302    33      98/142/0.484/15  ; 97/106/0.360/45
smoke-off-graveborn      205   dawn    649      301-125   0       -/-/0/0          ; -/-/0/0
smoke-on-graveborn-rep   232   dawn    589      301-81    28      99/111/0.296/34  ; 97/106/0.348/26
```

The first level 2 lands at 87 to 131 s in every match except the Wildmoot side that lost (214 s); the level 3 follows 8 to 50 s later. Wall time 176 to 315 s per match for 529 to 705 s of game time (about 3 to 4 times real time on a loaded machine; 6 to 7 times when the machine is quiet).

#### Determinism (two identical jobs, `SIM_LEVEL_SEED=424242`, `graveborn_normal` vs `gilded_normal`, maps 0 and 1, levels on; `det-a`, `det-b`)

Same world seed, same arguments, same code: the winners agreed (dawn, dusk both times), everything else differed.

```
                 det-a                           det-b
match 1 (map 0)  dawn 685 s, 301-139, kills 122/120, 14 fights    dawn 677 s, 301-145, kills 143/129, 18 fights
match 2 (map 1)  dusk 565 s, 46-301, kills 107/125, 7 fights      dusk 753 s, 183-301, kills 137/158, 11 fights
first level 2   99, 102 | 92, 99 (dawn, dusk; match 1 | match 2)   105, 98 | 96, 99   within 6 s per side
first level 3   195, 113 | 112, 106                               128, 108 | 128, 109   one side differs by 67 s
```

The runs are not tick-deterministic. The sources: the combat and the bots draw on `world.random` and vanilla entity AI (pathing, target choice, knockback), none of it seeded from the world seed; a grep of the server game logic found no wall-clock use (`System.nanoTime`, `currentTimeMillis`) in anything that decides a fight. Consequence for the gate: a single pair cannot be replayed, so the gate pools 200 rounds per condition and judges with intervals, and a fixed seed only holds the terrain constant.


#### Run 1, thresholds as designed (levels.l2.kills 3, levels.l3.kills 8): PASS

Both conditions on `cdea2c4` plus the sim tools, 20 jobs each, started 19:46 and finished 22:56 (one pair redone, below), 200 rounds per condition. Ports 25741 and 25742, two jobs at a time. Output of `bash work/gate/gate2.sh rr rr work/gate/queue-rr.log work/gate/queue-redo.log` (off = the `rr-off-*` jobs, on = the `rr-on-*` jobs), saved as `work/gate/rr-gate-final.txt`:

```
progression gate: off 200 rounds in 20 runs, on 200 rounds in 20 runs (aborted rounds left out); limit 10 points; comeback pool drops rounds under 300 s
seat: team 0 (Dawn) won 98 of 200 decided rounds off and 93 of 200 on; fitted seat term, team 0 wins an even pairing: off 45%, on 32%

shares (Bradley-Terry expected share against the other Houses, 95% interval; raw = plain win rate over non-mirror rounds)
  house          off %          (95%)    on %          (95%)   delta            (95%)  raw off  raw on  rule (|delta| <= 10)
  gilded          57.4 (51.4 to 63.3)    59.7 (53.5 to 65.8)    +2.3  (-6.3 to +10.9)     57.5    60.0  PASS (interval reaches past the limit)
  graveborn       67.3 (61.4 to 73.2)    65.1 (58.9 to 71.2)    -2.3  (-10.8 to +6.3)     67.5    65.0  PASS (interval reaches past the limit)
  ironclad        98.2 (95.4 to 100.0)    98.5 (96.1 to 100.0)    +0.3   (-3.3 to +4.0)     98.8    98.8  PASS
  tidecallers     24.1 (19.6 to 28.5)    24.2 (20.3 to 28.0)    +0.1   (-5.8 to +6.0)     23.8    23.8  PASS
  wildmoot         3.1   (0.0 to 6.6)     2.6   (0.0 to 5.8)    -0.5   (-5.2 to +4.3)      2.5     2.5  PASS
rule 1, every House within 10 points of its own baseline: PASS

comeback (decided rounds won by the side trailing at 300 s; ties at 300 s, draws, aborted and rounds under 300 s are left out)
  off  44 of 198 = 22.2% (Wilson 95% 17.0 to 28.5); left out: 0 under 300 s, 2 tied at 300 s, 0 draws, 0 aborted, of 200 rounds
  on   35 of 198 = 17.7% (Wilson 95% 13.0 to 23.6); left out: 0 under 300 s, 2 tied at 300 s, 0 draws, 0 aborted, of 200 rounds
  difference on - off: -4.5 points (95% -12.4 to +3.3)
rule 2, comeback rate on no more than 10 points below off: PASS

band (design 9.2: a House inside 40 to 60% is balanced; outside it in both conditions goes to the Phase 2 end balance pass, not this plan)
  gilded       off  57.4% in, on  59.7% in
  graveborn    off  67.3% OUT, on  65.1% OUT   <- outside in both: report
  ironclad     off  98.2% OUT, on  98.5% OUT   <- outside in both: report
  tidecallers  off  24.1% OUT, on  24.2% OUT   <- outside in both: report
  wildmoot     off   3.1% OUT, on   2.6% OUT   <- outside in both: report

level timings with levels on (tuning aim, not a rule; 200 rounds, 400 sides)
  first level 2: median 115 s (aim 120 to 180: outside); 0 sides never reached it
  first level 3: median 132 s; 21 sides never reached it
  end share at level 2 or higher: winner 43.9% (aim about a third, 27 to 40: outside), loser 12.4%
  level 3s per side per round: 24.8 (aim a few, not none and not a dozen: outside); both sides 49.5 a round
  house        sides  1st lvl 2  1st lvl 3  end share  lvl 3/side
  gilded          80        101        110      31.3%        32.5
  graveborn       80         97        120      26.2%        25.0
  ironclad        80        121      135.5      54.6%        31.2
  tidecallers     80      100.5      117.5      14.3%        25.9
  wildmoot        80        120        204      14.5%         9.2
  baseline sanity (levels off): 0 sides reached level 2, level 3s per side 0.0

gate progression shares=pass comeback=pass -> pass
```

- Pair matrix (wins of team 0 (Dawn), team 1 (Dusk), draws; mean seconds; mean kills of Dawn/Dusk), off against on; the first House is Dawn's (`work/gate/pairs-rr.txt`):

```
gilded      graveborn   off 3-7-0 688s k154/144 | on 2-8-0 675s k120/115
gilded      ironclad    off 0-10-0 629s k52/123 | on 0-10-0 597s k42/107
gilded      tidecallers off 10-0-0 581s k129/95 | on 10-0-0 621s k137/103
gilded      wildmoot    off 10-0-0 548s k139/43 | on 10-0-0 562s k153/35
graveborn   gilded      off 7-3-0 783s k144/166 | on 5-5-0 697s k130/162
graveborn   ironclad    off 0-10-0 675s k79/178 | on 0-10-0 621s k48/148
graveborn   tidecallers off 9-1-0 593s k136/126 | on 9-1-0 603s k128/96
graveborn   wildmoot    off 10-0-0 546s k141/88 | on 10-0-0 546s k138/82
ironclad    gilded      off 10-0-0 667s k133/67 | on 9-1-0 648s k123/60
ironclad    graveborn   off 9-1-0 650s k169/72 | on 10-0-0 633s k150/46
ironclad    tidecallers off 10-0-0 604s k129/46 | on 10-0-0 586s k129/28
ironclad    wildmoot    off 10-0-0 571s k135/21 | on 10-0-0 569s k140/18
tidecallers gilded      off 0-10-0 585s k104/133 | on 0-10-0 623s k105/136
tidecallers graveborn   off 0-10-0 594s k129/129 | on 0-10-0 583s k109/119
tidecallers ironclad    off 0-10-0 580s k32/122 | on 0-10-0 581s k28/118
tidecallers wildmoot    off 9-1-0 634s k160/123 | on 8-2-0 648s k165/109
wildmoot    gilded      off 0-10-0 576s k65/151 | on 0-10-0 546s k37/146
wildmoot    graveborn   off 0-10-0 552s k99/150 | on 0-10-0 545s k88/140
wildmoot    ironclad    off 0-10-0 563s k24/138 | on 0-10-0 561s k13/133
wildmoot    tidecallers off 1-9-0 626s k111/161 | on 0-10-0 623s k114/149
```

- Reading: the rules pass with room. Every share moved by 2.3 points or less (the widest interval, Gilded, still reaches +10.9 at the upper end, so the pass is a point estimate with a 200-round pool). The comeback rate is 22.2% off and 17.7% on: 4.5 points lower, inside the 10-point limit, interval -12.4 to +3.3, so a small loss of comebacks cannot be ruled out.
- The shares are saturated at both ends: Ironclad (98%) and Wildmoot (3%) win or lose nearly every round whatever the levels do, so the gate cannot move them; the movable part of the field is Gilded, Graveborn and Tidecallers. Houses outside 40 to 60% in both runs: Graveborn (67.3 / 65.1), Ironclad (98.2 / 98.5), Tidecallers (24.1 / 24.2) and Wildmoot (3.1 / 2.6): all four go to the Phase 2 end balance pass, nothing tuned here. Only Gilded sits inside the band (57.4 / 59.7).
- Level aims, not a rule, all missed on the early side: the first level 2 median is 115 s (aim 120 to 180), 0 sides never reached it; the first level 3 median is 132 s, the median gap from a side's first level 2 to its first level 3 only 13 s (mean 30 s); 43.9% of the winner's living soldiers are level 2 or higher at the end (aim about a third), the loser's 12.4%; 24.8 level 3s per side per round, 49.5 a round (aim a few). Per House the first level 2 comes at 97 s (Graveborn) to 121 s (Ironclad).
- Why they come early (`work/gate/extra-rr.txt`): a kill credits the killer, every helper of the killing team who touched the victim inside `progression.assistSeconds`, and every supporter of the killer or of a helper (`CreditLog.credited`, design 3.2), 4.7 soldiers a kill on average (median 4, p90 8, max 27; 5.3% of the 40831 logged kills credit 10 soldiers or more), and a victim of level N is worth N credits. With about 140 kills a side a round, a soldier near the fighting gets its first credits within the first fight or two, and the bounty rule (a victim is worth its own level in credits) feeds the climb. Seat check: team 0 won 49.0% off and 46.5% on (z -0.50), so the levels did not move the seat.
- One pair was redone. `rr-off-graveborn-gilded` and `rr-on-graveborn-gilded` (seed 9005) crashed 195 and 199 s in with the Server Watchdog ("A single server tick took 60.00 seconds", main thread parked in `ServerChunkManager.getChunk` through `World.getChunk`); the queue runner had logged exit 0 because the shell script itself exited 0. Rerun with the same seed and arguments (`work/gate/queue-redo.log`, 22:56 to 23:14), they ran clean; the crashed roots are not in any number. The runner now retries a job once when gradle did not exit 0, and the collector takes only jobs with "gradle exit 0". The same crash hit `r412-on-graveborn-gilded` once (03:46, same stack) and the retry ran clean. It is a flake of the sim, not of the levels (the baseline job crashed too), and it has only ever hit this pair (seed 9005, three of its four first attempts so far), so the world at that seed may be the trigger; not investigated, nothing in this task touches it.


#### Tuning round 1, thresholds 4 and 12 (levels.l2.kills 3 -> 4, levels.l3.kills 8 -> 12): PASS, level aims still early

Why: the as-designed gate passed its two rules, but the level aims missed on the early side (first level 2 at 115 s, 49.5 level 3s a round). Kort ruled in round 35 (question "Levels arrive early in the headless sim") to raise the kill thresholds, so lever 1 of design 9.2 (thresholds) moved first and alone: `levels.l2.kills 3 -> 4` and `levels.l3.kills 8 -> 12`. Gear prices, gear percentages and level bonuses stay. `commanderBounty` (3) is a different cell and did not change. Counts as tuning round 1 of the stop rule.

- Checks, red first. `python tools/preflight.py`: Preflight clean (4895 filled cells); `Spec.java` regenerated, never hand-edited. Standalone `HousesCheck` failed on the pinned 0, 3 and 8 before the pins moved, then all standalone checks passed (BattleMathCheck 64, HousesCheck 293, ProfileMigrationCheck 31, LevelMathCheck 141, and the Admission, CampRules, Compatibility and LegacyDataMigration checks, migration 13). The pinned values moved to 4 and 12 in `LevelMathCheck` (`lm-thresholds`, `lm-carry`) and `HousesCheck`; the harness checks that credited a literal 3 or 8 (`LevelChecks`, `RankNetworkChecks`, 27 check ids) now read the thresholds from `Spec` through `L2` and `L3`, so a later tuning round needs no edit there. Harness client, first with the sheet changed and the checks not (red: 176 pass, 29 fail: 27 check ids plus `p2-levels-suite` and `p2-gear-suite`), then with the checks fixed (green: 205 pass, 0 fail; suites `levels,gear,statuses,ranks,banners,engine,levels-gilded,levels-graveborn,levels-wildmoot,levels-ironclad,levels-tidecallers`); the two-client LAN check: host 9 pass 0 fail, guest 6 pass 0 fail.
- Rerun: the `on` condition only (the baseline is unchanged: same code, same sheet rows except the thresholds, which `off` never reads), 20 jobs `r412-on-*`, same ordered pairs, seeds and arguments as `rr-on-*`; 03:23 to 05:25. One job (`r412-on-graveborn-gilded`) hit the sim's Watchdog flake and ran clean on the automatic retry. Gate = `bash work/gate/gate2.sh rr r412 work/gate/queue-rr.log work/gate/queue-redo.log work/gate/queue-r412.log` (off = the Run 1 baseline), saved as `work/gate/r412-gate-final.txt`:

```
progression gate: off 200 rounds in 20 runs, on 200 rounds in 20 runs (aborted rounds left out); limit 10 points; comeback pool drops rounds under 300 s
seat: team 0 (Dawn) won 98 of 200 decided rounds off and 99 of 200 on; fitted seat term, team 0 wins an even pairing: off 45%, on 48%

shares (Bradley-Terry expected share against the other Houses, 95% interval; raw = plain win rate over non-mirror rounds)
  house          off %          (95%)    on %          (95%)   delta            (95%)  raw off  raw on  rule (|delta| <= 10)
  gilded          57.4 (51.4 to 63.3)    56.2 (49.4 to 63.1)    -1.1  (-10.2 to +7.9)     57.5    56.2  PASS (interval reaches past the limit)
  graveborn       67.3 (61.4 to 73.2)    62.4 (55.9 to 69.0)    -4.9  (-13.7 to +3.9)     67.5    62.5  PASS (interval reaches past the limit)
  ironclad        98.2 (95.4 to 100.0)    99.4 (97.7 to 100.0)    +1.2   (-2.1 to +4.4)     98.8   100.0  PASS
  tidecallers     24.1 (19.6 to 28.5)    25.3 (18.8 to 31.8)    +1.2   (-6.7 to +9.1)     23.8    25.0  PASS
  wildmoot         3.1   (0.0 to 6.6)     6.7  (1.6 to 11.8)    +3.6   (-2.6 to +9.9)      2.5     6.2  PASS
rule 1, every House within 10 points of its own baseline: PASS

comeback (decided rounds won by the side trailing at 300 s; ties at 300 s, draws, aborted and rounds under 300 s are left out)
  off  44 of 198 = 22.2% (Wilson 95% 17.0 to 28.5); left out: 0 under 300 s, 2 tied at 300 s, 0 draws, 0 aborted, of 200 rounds
  on   44 of 200 = 22.0% (Wilson 95% 16.8 to 28.2); left out: 0 under 300 s, 0 tied at 300 s, 0 draws, 0 aborted, of 200 rounds
  difference on - off: -0.2 points (95% -8.4 to +7.9)
rule 2, comeback rate on no more than 10 points below off: PASS

band (design 9.2: a House inside 40 to 60% is balanced; outside it in both conditions goes to the Phase 2 end balance pass, not this plan)
  gilded       off  57.4% in, on  56.2% in
  graveborn    off  67.3% OUT, on  62.4% OUT   <- outside in both: report
  ironclad     off  98.2% OUT, on  99.4% OUT   <- outside in both: report
  tidecallers  off  24.1% OUT, on  25.3% OUT   <- outside in both: report
  wildmoot     off   3.1% OUT, on   6.7% OUT   <- outside in both: report

level timings with levels on (tuning aim, not a rule; 200 rounds, 400 sides)
  first level 2: median 118 s (aim 120 to 180: outside); 7 sides never reached it
  first level 3: median 148 s; 51 sides never reached it
  end share at level 2 or higher: winner 41.2% (aim about a third, 27 to 40: outside), loser 11.0%
  level 3s per side per round: 16.0 (aim a few, not none and not a dozen: outside); both sides 32.0 a round
  house        sides  1st lvl 2  1st lvl 3  end share  lvl 3/side
  gilded          80        104        121      29.2%        22.8
  graveborn       80       98.5      151.5      24.4%        14.7
  ironclad        80        125        152      50.4%        18.5
  tidecallers     80        107        144      16.9%        16.7
  wildmoot        80        132      215.5       9.6%         7.5
  baseline sanity (levels off): 0 sides reached level 2, level 3s per side 0.0

gate progression shares=pass comeback=pass -> pass
```

- Pair matrix, off against on at 4 and 12 (`work/gate/pairs-r412.txt`):

```
gilded      graveborn   off 3-7-0 688s k154/144 | on 5-5-0 729s k152/125
gilded      ironclad    off 0-10-0 629s k52/123 | on 0-10-0 605s k41/113
gilded      tidecallers off 10-0-0 581s k129/95 | on 9-1-0 602s k131/94
gilded      wildmoot    off 10-0-0 548s k139/43 | on 9-1-0 545s k143/26
graveborn   gilded      off 7-3-0 783s k144/166 | on 6-4-0 672s k124/146
graveborn   ironclad    off 0-10-0 675s k79/178 | on 0-10-0 641s k49/155
graveborn   tidecallers off 9-1-0 593s k136/126 | on 9-1-0 580s k125/99
graveborn   wildmoot    off 10-0-0 546s k141/88 | on 10-0-0 543s k139/70
ironclad    gilded      off 10-0-0 667s k133/67 | on 10-0-0 617s k119/51
ironclad    graveborn   off 9-1-0 650s k169/72 | on 10-0-0 653s k154/58
ironclad    tidecallers off 10-0-0 604s k129/46 | on 10-0-0 586s k126/29
ironclad    wildmoot    off 10-0-0 571s k135/21 | on 10-0-0 571s k134/18
tidecallers gilded      off 0-10-0 585s k104/133 | on 1-9-0 585s k100/131
tidecallers graveborn   off 0-10-0 594s k129/129 | on 0-10-0 584s k100/114
tidecallers ironclad    off 0-10-0 580s k32/122 | on 0-10-0 573s k21/123
tidecallers wildmoot    off 9-1-0 634s k160/123 | on 8-2-0 659s k173/130
wildmoot    gilded      off 0-10-0 576s k65/151 | on 1-9-0 554s k47/146
wildmoot    graveborn   off 0-10-0 552s k99/150 | on 0-10-0 548s k80/141
wildmoot    ironclad    off 0-10-0 563s k24/138 | on 0-10-0 563s k15/135
wildmoot    tidecallers off 1-9-0 626s k111/161 | on 1-9-0 630s k118/156
```

- Reading: both rules pass with more room than Run 1 (shares within 4.9 points, comeback -0.2). The aims moved the right way but not far: the first level 2 is 118 s (115 before; it is the earliest soldier of a side, which a higher threshold barely delays), the winner's end share 41.2% (43.9), level 3s 16.0 a side and 32.0 a round (24.8 and 49.5), the median gap from a side's first level 2 to its first level 3 31 s (13 s). Kill credit sharing is unchanged (4.7 soldiers a kill, median 4, p90 8; `work/gate/extra-r412.txt`).
- By the coordinator's rule (Kort's round-35 follow-up): the first level 2 is still before about 120 s and the level 3s a round are still above about 8, so one further step, 5 and 14, was allowed.


#### Tuning round 2, thresholds 5 and 14 (levels.l2.kills 4 -> 5, levels.l3.kills 12 -> 14): PASS, two of three aims met

Why: at 4 and 12 the first level 2 still landed before about 120 s (118 s) and the level 3s a round were still far above 8 (32.0). Kort's round-35 answer allowed one further step, 5 and 14, before asking again: `levels.l2.kills 4 -> 5` and `levels.l3.kills 12 -> 14`, nothing else. Tuning round 2 of the stop rule (the last).

- Checks, red first, same recipe as round 1: `python tools/preflight.py` Preflight clean; standalone `HousesCheck` failed (`levels: 0, 4 and 12 kills ...`) before the pins moved, then every standalone check passed; the pins in `LevelMathCheck` and `HousesCheck` now read 5 and 14 (the harness checks follow `Spec` through `L2` and `L3`, no edit); `gradle build --offline -x test` BUILD SUCCESSFUL; harness client suites `levels,gear,statuses,ranks,banners,engine,levels-*` 205 pass, 0 fail; LAN check host 9 pass 0 fail, guest 6 pass 0 fail (run on the scratch worktree `bannerbound-gate-tune`, whose six changed files are byte-identical to the committed ones).
- Rerun: `on` only, 20 jobs `r514-on-*` (same pairs, seeds and arguments; 05:26 to 07:29). `r514-on-graveborn-gilded` hit the Watchdog flake and ran clean on the automatic retry. Gate = `bash work/gate/gate2.sh rr r514 work/gate/queue-rr.log work/gate/queue-redo.log work/gate/queue-r514.log` against the Run 1 baseline, saved as `work/gate/r514-gate-final.txt`:

```
progression gate: off 200 rounds in 20 runs, on 200 rounds in 20 runs (aborted rounds left out); limit 10 points; comeback pool drops rounds under 300 s
seat: team 0 (Dawn) won 98 of 200 decided rounds off and 94 of 200 on; fitted seat term, team 0 wins an even pairing: off 45%, on 38%

shares (Bradley-Terry expected share against the other Houses, 95% interval; raw = plain win rate over non-mirror rounds)
  house          off %          (95%)    on %          (95%)   delta            (95%)  raw off  raw on  rule (|delta| <= 10)
  gilded          57.4 (51.4 to 63.3)    60.9 (54.3 to 67.4)    +3.5  (-5.3 to +12.4)     57.5    61.3  PASS (interval reaches past the limit)
  graveborn       67.3 (61.4 to 73.2)    67.3 (60.6 to 74.0)    -0.1   (-9.0 to +8.9)     67.5    67.5  PASS
  ironclad        98.2 (95.4 to 100.0)    94.9 (90.3 to 99.5)    -3.2   (-8.6 to +2.1)     98.8    95.0  PASS
  tidecallers     24.1 (19.6 to 28.5)    22.9 (18.3 to 27.5)    -1.2   (-7.6 to +5.2)     23.8    22.5  PASS
  wildmoot         3.1   (0.0 to 6.6)     4.0   (0.0 to 8.0)    +1.0   (-4.4 to +6.3)      2.5     3.8  PASS
rule 1, every House within 10 points of its own baseline: PASS

comeback (decided rounds won by the side trailing at 300 s; ties at 300 s, draws, aborted and rounds under 300 s are left out)
  off  44 of 198 = 22.2% (Wilson 95% 17.0 to 28.5); left out: 0 under 300 s, 2 tied at 300 s, 0 draws, 0 aborted, of 200 rounds
  on   42 of 194 = 21.6% (Wilson 95% 16.4 to 28.0); left out: 0 under 300 s, 6 tied at 300 s, 0 draws, 0 aborted, of 200 rounds
  difference on - off: -0.6 points (95% -8.8 to +7.6)
rule 2, comeback rate on no more than 10 points below off: PASS

band (design 9.2: a House inside 40 to 60% is balanced; outside it in both conditions goes to the Phase 2 end balance pass, not this plan)
  gilded       off  57.4% in, on  60.9% OUT
  graveborn    off  67.3% OUT, on  67.3% OUT   <- outside in both: report
  ironclad     off  98.2% OUT, on  94.9% OUT   <- outside in both: report
  tidecallers  off  24.1% OUT, on  22.9% OUT   <- outside in both: report
  wildmoot     off   3.1% OUT, on   4.0% OUT   <- outside in both: report

level timings with levels on (tuning aim, not a rule; 200 rounds, 400 sides)
  first level 2: median 120 s (aim 120 to 180: inside); 19 sides never reached it
  first level 3: median 166 s; 63 sides never reached it
  end share at level 2 or higher: winner 35.5% (aim about a third, 27 to 40: inside), loser 10.8%
  level 3s per side per round: 13.5 (aim a few, not none and not a dozen: outside); both sides 27.0 a round
  house        sides  1st lvl 2  1st lvl 3  end share  lvl 3/side
  gilded          80      107.5      129.5      25.2%        19.3
  graveborn       80        100        207      18.4%        11.9
  ironclad        80      127.5      168.5      46.3%        14.7
  tidecallers     80        106        163      15.6%        15.1
  wildmoot        80      133.5        214      10.0%         6.6
  baseline sanity (levels off): 0 sides reached level 2, level 3s per side 0.0

gate progression shares=pass comeback=pass -> pass
```

- Pair matrix, off against on at 5 and 14 (`work/gate/pairs-r514.txt`):

```
gilded      graveborn   off 3-7-0 688s k154/144 | on 3-7-0 732s k150/136
gilded      ironclad    off 0-10-0 629s k52/123 | on 0-10-0 615s k44/113
gilded      tidecallers off 10-0-0 581s k129/95 | on 10-0-0 592s k127/103
gilded      wildmoot    off 10-0-0 548s k139/43 | on 10-0-0 534s k143/22
graveborn   gilded      off 7-3-0 783s k144/166 | on 7-3-0 720s k134/169
graveborn   ironclad    off 0-10-0 675s k79/178 | on 1-9-0 647s k61/162
graveborn   tidecallers off 9-1-0 593s k136/126 | on 9-1-0 609s k138/123
graveborn   wildmoot    off 10-0-0 546s k141/88 | on 10-0-0 545s k137/80
ironclad    gilded      off 10-0-0 667s k133/67 | on 7-3-0 684s k139/74
ironclad    graveborn   off 9-1-0 650s k169/72 | on 10-0-0 633s k155/58
ironclad    tidecallers off 10-0-0 604s k129/46 | on 10-0-0 572s k122/27
ironclad    wildmoot    off 10-0-0 571s k135/21 | on 10-0-0 565s k134/14
tidecallers gilded      off 0-10-0 585s k104/133 | on 0-10-0 597s k106/130
tidecallers graveborn   off 0-10-0 594s k129/129 | on 0-10-0 595s k134/128
tidecallers ironclad    off 0-10-0 580s k32/122 | on 0-10-0 572s k21/126
tidecallers wildmoot    off 9-1-0 634s k160/123 | on 7-3-0 714s k188/140
wildmoot    gilded      off 0-10-0 576s k65/151 | on 0-10-0 603s k69/164
wildmoot    graveborn   off 0-10-0 552s k99/150 | on 0-10-0 548s k84/139
wildmoot    ironclad    off 0-10-0 563s k24/138 | on 0-10-0 562s k16/134
wildmoot    tidecallers off 1-9-0 626s k111/161 | on 0-10-0 620s k108/150
```

- Reading: both rules pass (largest share move: Ironclad -3.2, Gilded +3.5; comeback -0.6 points, interval -8.8 to +7.6). The Ironclad and Wildmoot saturation is unchanged. Gilded reads 60.9% on (57.4% off): inside the rule, a hair over the 60% edge of the band in this one run, so by the band rule (outside in both runs) it is not reported. The seat is steady (team 0 won 47.0% on, 49.0% off, z -0.40).
- Aims at 5 and 14: first level 2 median 120 s (inside 120 to 180, on the edge; 19 of 400 sides never reached level 2); winner's end share at level 2 or higher 35.5% (inside 27 to 40, about a third); level 3s 13.5 a side and 27.0 a round (outside: a few), first level 3 median 166 s, the median gap from a side's first level 2 to its first level 3 43 s (mean 67.5).
- The one aim still missed is the number of level 3s. Estimate for a higher level-3 threshold (`work/gate/l3_curve.py r514 work/gate/queue-r514.log`; first order: it counts the soldiers whose running credits, as logged in the 20 on jobs at 5 and 14, reached T; at T = 14 it returns exactly the 13.5 a side the gate measured, and a higher threshold would also slow the level-ups that feed the bounty, so the real counts at a higher T would be a little lower than these):

```
r514: 400 sides; levels.csv level3Count per side 13.5
  soldiers that reached  8 credits:  23.0 per side per round,  46.0 a round
  soldiers that reached 10 credits:  18.9 per side per round,  37.7 a round
  soldiers that reached 12 credits:  15.8 per side per round,  31.7 a round
  soldiers that reached 14 credits:  13.5 per side per round,  27.0 a round
  soldiers that reached 16 credits:  11.7 per side per round,  23.4 a round
  soldiers that reached 18 credits:   9.9 per side per round,  19.8 a round
  soldiers that reached 20 credits:   8.3 per side per round,  16.6 a round
  soldiers that reached 22 credits:   7.1 per side per round,  14.1 a round
  soldiers that reached 24 credits:   6.0 per side per round,  12.0 a round
  soldiers that reached 26 credits:   5.1 per side per round,  10.3 a round
  soldiers that reached 28 credits:   4.5 per side per round,   8.9 a round
  soldiers that reached 30 credits:   3.8 per side per round,   7.7 a round
  soldiers that reached 35 credits:   2.6 per side per round,   5.1 a round
  soldiers that reached 40 credits:   1.8 per side per round,   3.6 a round
```

- Summary of the three settings (200 rounds each, levels on, same ordered pairs and seeds; all three pass both rules):

```
thresholds   first lvl 2   first lvl 3   winner end share L2+   lvl 3 per side (a round)   median gap L2->L3   largest share move   comeback on-off
3 and 8      115 s          132 s         43.9%                  24.8 (49.5)                13 s                +2.3 (Gilded)        -4.5 points
4 and 12     118 s          148 s         41.2%                  16.0 (32.0)                31 s                -4.9 (Graveborn)     -0.2 points
5 and 14     120 s          166 s         35.5%                  13.5 (27.0)                43 s                +3.5 (Gilded)        -0.6 points
aim          120 to 180     -             about a third          a few                      -                   within 10 points     within 10 points
```

- Stop: two tuning rounds are used. The level-3 count still misses its aim, so it goes to `docs/QUESTIONS-FOR-KORT.md` (section "Progression gate: the level 3 count") with the numbers; nothing more is tuned here. The recommended answer there is one cell, `levels.l3.kills 14 -> 30`.


#### Real-time check: fast ticks against real time, `gilded_normal` vs `graveborn_normal` (the current best pair), 900 s

Both seats, 10 matches each, maps 0 to 4 twice, world seed 9901, no `-PsimFast`: `rt-off-*` (levels off), `rt-on-*` (levels on at the as-designed 3 and 8) and `rt514-on-*` (levels on at the final 5 and 14). Real time is slow: one job took 1.9 to 2.2 hours (6836 to 7844 s), two at a time. Comparisons are `python tools/balance_method.py compare-matches --a <A results> --b <B results> --focus gilded_normal` (B minus A, by map and seat; `work/gate/compare_rt.py` builds the argument lists; output `work/gate/compare-rt-338.txt`, `work/gate/compare-rt-final.txt`). Win = a `gilded_normal` win.

```
### Levels on at the as-designed 3 and 8 (jobs rr-*, rt-*)
=== off: fast (A) against real time (B)
gilded_normal: run A 20 matches, run B 20 matches
  win      A    0.30 (SD  0.47)  B    0.70 (SD  0.47)  B-A by map and seat  +0.40 (95% +0.16 to +0.64, 10 cells)  Welch p=0.011
  gap      A  -35.20 (SD 100.80)  B   50.40 (SD 110.11)  B-A by map and seat +85.60 (95% +36.57 to +134.63, 10 cells)  Welch p=0.014
  kills    A  159.75 (SD 52.16)  B  172.15 (SD 52.26)  B-A by map and seat +12.40 (95% -6.03 to +30.83, 10 cells)  Welch p=0.457
  lost     A  144.45 (SD 47.74)  B  131.85 (SD 53.16)  B-A by map and seat -12.60 (95% -31.31 to +6.11, 10 cells)  Welch p=0.435
  seconds  A  735.25 (SD 106.21)  B  714.45 (SD 99.44)  B-A by map and seat -20.80 (95% -60.57 to +18.97, 10 cells)  Welch p=0.526
=== on: fast (A) against real time (B)
gilded_normal: run A 20 matches, run B 20 matches
  win      A    0.35 (SD  0.49)  B    0.50 (SD  0.51)  B-A by map and seat  +0.15 (95% -0.20 to +0.50, 10 cells)  Welch p=0.350
  gap      A  -44.00 (SD 129.61)  B   14.30 (SD 135.75)  B-A by map and seat +58.30 (95% -33.70 to +150.30, 10 cells)  Welch p=0.173
  kills    A  140.80 (SD 54.96)  B  166.70 (SD 54.49)  B-A by map and seat +25.90 (95% +4.72 to +47.08, 10 cells)  Welch p=0.143
  lost     A  122.50 (SD 39.27)  B  118.30 (SD 42.03)  B-A by map and seat  -4.20 (95% -18.65 to +10.25, 10 cells)  Welch p=0.746
  seconds  A  685.80 (SD 75.17)  B  700.40 (SD 97.94)  B-A by map and seat +14.60 (95% -37.85 to +67.05, 10 cells)  Welch p=0.600
=== real time: off (A) against on (B)
gilded_normal: run A 20 matches, run B 20 matches
  win      A    0.70 (SD  0.47)  B    0.50 (SD  0.51)  B-A by map and seat  -0.20 (95% -0.54 to +0.14, 10 cells)  Welch p=0.207
  gap      A   50.40 (SD 110.11)  B   14.30 (SD 135.75)  B-A by map and seat -36.10 (95% -117.91 to +45.71, 10 cells)  Welch p=0.362
  kills    A  172.15 (SD 52.26)  B  166.70 (SD 54.49)  B-A by map and seat  -5.45 (95% -27.92 to +17.02, 10 cells)  Welch p=0.749
  lost     A  131.85 (SD 53.16)  B  118.30 (SD 42.03)  B-A by map and seat -13.55 (95% -35.57 to +8.47, 10 cells)  Welch p=0.377
  seconds  A  714.45 (SD 99.44)  B  700.40 (SD 97.94)  B-A by map and seat -14.05 (95% -70.15 to +42.05, 10 cells)  Welch p=0.655
=== fast: off (A) against on (B)
gilded_normal: run A 20 matches, run B 20 matches
  win      A    0.30 (SD  0.47)  B    0.35 (SD  0.49)  B-A by map and seat  +0.05 (95% -0.21 to +0.31, 10 cells)  Welch p=0.744
  gap      A  -35.20 (SD 100.80)  B  -44.00 (SD 129.61)  B-A by map and seat  -8.80 (95% -73.41 to +55.81, 10 cells)  Welch p=0.812
  kills    A  159.75 (SD 52.16)  B  140.80 (SD 54.96)  B-A by map and seat -18.95 (95% -35.78 to -2.12, 10 cells)  Welch p=0.270
  lost     A  144.45 (SD 47.74)  B  122.50 (SD 39.27)  B-A by map and seat -21.95 (95% -30.55 to -13.35, 10 cells)  Welch p=0.121
  seconds  A  735.25 (SD 106.21)  B  685.80 (SD 75.17)  B-A by map and seat -49.45 (95% -83.88 to -15.02, 10 cells)  Welch p=0.098

### Levels on at the final 5 and 14 (jobs r514-*, rt514-*; the off lines above are the same runs)
=== on: fast (A) against real time (B)
gilded_normal: run A 20 matches, run B 20 matches
  win      A    0.30 (SD  0.47)  B    0.60 (SD  0.50)  B-A by map and seat  +0.30 (95% -0.04 to +0.64, 10 cells)  Welch p=0.059
  gap      A  -58.95 (SD 93.31)  B   34.55 (SD 138.20)  B-A by map and seat +93.50 (95% +26.08 to +160.92, 10 cells)  Welch p=0.017
  kills    A  159.50 (SD 52.35)  B  160.80 (SD 47.48)  B-A by map and seat  +1.30 (95% -20.74 to +23.34, 10 cells)  Welch p=0.935
  lost     A  135.30 (SD 38.22)  B  115.60 (SD 44.37)  B-A by map and seat -19.70 (95% -33.97 to -5.43, 10 cells)  Welch p=0.141
  seconds  A  725.80 (SD 87.52)  B  700.60 (SD 108.37)  B-A by map and seat -25.20 (95% -71.03 to +20.63, 10 cells)  Welch p=0.424
=== real time: off (A) against on (B)
gilded_normal: run A 20 matches, run B 20 matches
  win      A    0.70 (SD  0.47)  B    0.60 (SD  0.50)  B-A by map and seat  -0.10 (95% -0.41 to +0.21, 10 cells)  Welch p=0.520
  gap      A   50.40 (SD 110.11)  B   34.55 (SD 138.20)  B-A by map and seat -15.85 (95% -83.89 to +52.19, 10 cells)  Welch p=0.691
  kills    A  172.15 (SD 52.26)  B  160.80 (SD 47.48)  B-A by map and seat -11.35 (95% -31.37 to +8.67, 10 cells)  Welch p=0.477
  lost     A  131.85 (SD 53.16)  B  115.60 (SD 44.37)  B-A by map and seat -16.25 (95% -37.27 to +4.77, 10 cells)  Welch p=0.301
  seconds  A  714.45 (SD 99.44)  B  700.60 (SD 108.37)  B-A by map and seat -13.85 (95% -63.53 to +35.83, 10 cells)  Welch p=0.676
=== fast: off (A) against on (B)
gilded_normal: run A 20 matches, run B 20 matches
  win      A    0.30 (SD  0.47)  B    0.30 (SD  0.47)  B-A by map and seat  +0.00 (95% -0.28 to +0.28, 10 cells)  Welch p=1.000
  gap      A  -35.20 (SD 100.80)  B  -58.95 (SD 93.31)  B-A by map and seat -23.75 (95% -71.92 to +24.42, 10 cells)  Welch p=0.444
  kills    A  159.75 (SD 52.16)  B  159.50 (SD 52.35)  B-A by map and seat  -0.25 (95% -20.86 to +20.36, 10 cells)  Welch p=0.988
  lost     A  144.45 (SD 47.74)  B  135.30 (SD 38.22)  B-A by map and seat  -9.15 (95% -19.74 to +1.44, 10 cells)  Welch p=0.508
  seconds  A  735.25 (SD 106.21)  B  725.80 (SD 87.52)  B-A by map and seat  -9.45 (95% -44.29 to +25.39, 10 cells)  Welch p=0.761
```

- Fast ticks are not the same game as real time on this pair. With levels off, `gilded_normal` won 6 of 20 at fast ticks and 14 of 20 in real time (B-A +0.40, 95% +0.16 to +0.64, Welch p = 0.011; mean score gap +85.6 points, p = 0.014); kills, kills lost and round length do not differ significantly. In the same 20 fast matches the seats looked alike (Gilded 3 of 10 from each seat); in real time Gilded won 7 of 10 from each seat. So the shares of the fast-tick round robin above are right for the fast-tick game and may be shifted for the real one, by up to about 40 points on a pair like this. The cause is not known. What fast mode changes is only the wait between ticks (`FastTickMixin`: the next tick starts when the task queues are drained, instead of in the next 50 ms slot), so the chunk, light and worldgen threads and anything else off the server thread get less wall time per tick; every game rule counts ticks. One guess is a unit or a projectile meeting a chunk that is not ready; it was not tested. For the Phase 2 end balance pass: use the round robin to find which Houses and pairs move, and confirm every tuning step on a real-time pair before trusting a fast-tick share.
- Levels on against levels off, in real time, as-designed thresholds: `gilded_normal` 14 of 20 off and 10 of 20 on (B-A -0.20, 95% -0.54 to +0.14, p = 0.207): a drop of the size the intervals allow, not shown to be real, on 20 matches. At fast ticks the same pair went 6 of 20 off to 7 of 20 on (+0.05, p = 0.744). The round robin's rule-1 move for Gilded is within 2.3 points, so the real-time pair does not contradict the gate, but it is a single pair with 20 matches and cannot overturn it.
- At the final 5 and 14 (`rt514-on-*`, 07:30 to 09:41, 6836 and 7844 s a job): `gilded_normal` won 12 of 20 in real time (5 of 10 from Dawn's seat, 7 of 10 from Dusk's), against 14 of 20 with levels off (B-A -0.10, 95% -0.41 to +0.21, p = 0.52) and against 6 of 20 at fast ticks with levels on (+0.30, 95% -0.04 to +0.64, p = 0.059; score gap +93.5, p = 0.017). Fast ticks: 6 of 20 off and 6 of 20 on (0.00, p = 1.00). So on this pair levels move Gilded by no more than the intervals allow in either mode (fast 0.00, real time -0.10), and the difference between fast and real time is about the same size with levels off (+0.40), as designed (+0.15) and at 5 and 14 (+0.30): it belongs to the sim mode, not to the levels.
- The level timings do carry to real time on this pair. First level 2 median: 101.5 s real time against 100.0 and 100.5 s fast at 3 and 8; 108 s against 110 and 108 s fast at 5 and 14. Level 3s a side: 32.4 real time against 32.6 and 29.1 fast at 3 and 8; 18.3 against 21.5 and 18.9 fast at 5 and 14. First level 3 median at 5 and 14: 170.5 s real time against 159.5 and 151 s fast. No side failed to reach level 2 in real time.

#### Verdict, what went where

- Gate result: PASS at the as-designed thresholds (3 and 8), PASS at 4 and 12, PASS at the final 5 and 14: every House within 10 points of its own baseline (largest move 3.5 points at the final setting, 4.9 at 4 and 12, 2.3 as designed) and the pooled comeback rate within 0.6 points of the baseline at the final setting (-4.5 as designed, -0.2 at 4 and 12).
- Tuning applied (two rounds, both lever 1, both by Kort's round-35 answer): `levels.l2.kills 3 -> 4 -> 5` and `levels.l3.kills 8 -> 12 -> 14`; shipped state is 5 and 14. Nothing else in `sheets/design.json` changed (gear prices, gear percentages, level bonuses, `commanderBounty`, the banners and the 15% banner rule untouched).
- Houses outside 40 to 60% in both conditions, reported to the Phase 2 end balance pass (not tuned here): Graveborn (67.3% off, 67.3% on at the final setting), Ironclad (98.2 / 94.9), Tidecallers (24.1 / 22.9), Wildmoot (3.1 / 4.0). Gilded is inside off (57.4%) and just over on (60.9%), so it is not reported. The Ironclad and Wildmoot shares are saturated, so the gate is blind to levels for them; the end pass should look at them with a pair that is not decided in the first fight.
- Open for Kort (in `docs/QUESTIONS-FOR-KORT.md`): the level 3 count (27 a round at 5 and 14, aim a few) and the fast-tick against real-time difference.
- Not run: the difficulty ladder (Easy, Normal, Hard with gear buying on), which the plan marks report-only; the gate and its tuning took the time and the sim hours. `-PsimDawn=hard -PsimDusk=normal` on the same banners would do it.
- Reproduce: `python tools/progression_gate.py --off <rr-off roots> --on <rr-on roots> [--json OUT]` over the server roots (each holds `botsim/results.csv` and `levels.csv`); fixture test `python test-tools/standalone/test_progression_gate.py` (13 tests).

### Phase 2 Task 13 gates: mixed banners against their pure Houses (2026-10-23; `mix_gilded_graveborn_normal` vs `gilded_normal` 5%, `mix_ironclad_tidecallers_normal` vs `tidecallers_normal` 90%; log only, both outside the band)

Log-only by the plan (no gate, nothing tuned, nothing changed in the sheets). Code: `phase-2-houses` at `efff4cc` (Task 12 fix round 1 plus its N1/N2 fix) in the detached worktree `bannerbound-gate13`, built with `gradle localTestToolsJar --offline`; the Task 13 commits after it change no game rule (a card-layout probe, a test suite, a preflight rule). Recipe as the Task 9 gate (`work/p2/t8b/{gate.sh,sim.sh,fightstats.py}` with the paths changed): per pairing two bot-simulation servers at once, 10 matches each, `-PsimDawn=normal -PsimDusk=normal -PsimMaps=0,1,2,3,4 -PsimRoundSeconds=300`, seats swapped. Raw logs in `work/p2/t13/gates/`. The two pairings ran one after the other (07:45 to 08:41 and 09:00 to 09:56); every round ran its full 300 s and none aborted.

```
gate mix_gilded_graveborn_normal vs gilded_normal win=5% fail   (rounds=20 decided=20 won=1 lost=19 drawn=0)
gate mix_ironclad_tidecallers_normal vs tidecallers_normal win=90% fail   (rounds=20 decided=20 won=18 lost=2 drawn=0)
```

- `mix_gilded_graveborn_normal` (seat-pooled, 20 matches): won 1 (on Dawn, map 1), lost 19; kills 57.2, lost 59.5; groups wiped 2.80 own and 2.35 enemy; 6.05 fights of 37.2 s. The fights are level, the points are not: `gilded_normal` takes the match on points. Both seats lose (Dawn 1 of 10, Dusk 0 of 10). Reading: mixing two Houses halves each House's trait slots, so neither trait reaches its tier the way a pure banner's does, and the Gilded Bulwark and shieldwall edge goes to the pure banner.
- `mix_ironclad_tidecallers_normal`: won 18, lost 2 (Dawn 9 of 10, Dusk 9 of 10); kills 81.2, lost 52.5; groups wiped 2.40 own and 4.10 enemy; 7.55 fights of 31.4 s. The Ironclad half carries the mix: it beats pure Tidecallers about as `ironclad_normal` does (Task 9: 0 won of 20 for Tidecallers). This says Tidecallers are weak (structural, see the Task 9 gate and the round robin), not that mixing is strong.
- Both are far outside 40 to 60. Neither is a defect to fix in Task 13 (report-only by ruling); they go to the end balance pass with the Tidecallers and Wildmoot items, and they are flagged in `docs/PHASE-2-PLAYTEST.md`.

### Phase 2 Task 10 gates: Wildmoot (2026-10-23; `wildmoot_normal` vs `ironclad_normal` 10%, vs `gilded_normal` 0%; both report-only, both outside the band)

Code: `phase-2-houses` at `dc62e65` (Task 10 fix round 1: the Moot Hound takes a troop's blow in full, Bloodrush lifesteal and refunds by the commander's hand only, the trample once per fresh Charge), on the no-routing game of Task 8b at the design's own numbers. By the controller's ruling both gates are report-only: no sheet number was changed and tuning is deferred to the end balance pass, which takes Wildmoot as a structural item next to Tidecallers (check first whether the bots use the hound and pack mechanics at all).

Both gates use the recipe unchanged (two processes of 10 matches, `-PsimDawn=normal -PsimDusk=normal -PsimMaps=0,1,2,3,4 -PsimRoundSeconds=300`, a fresh server root each), one after the other: ports 25681 and 25682 for gate 1 (roots `work/p2/t11-sim1-005751` and `work/p2/t11-sim2-005811`, 00:57 to 01:53), 25683 and 25684 for gate 2 (`work/p2/t11-sim3-015328` and `work/p2/t11-sim4-015348`, 01:53 to 02:49). All 40 rounds ran their full 300 s; none aborted. An earlier gate 1 on `d5ccb0a` (3 of 20) is invalid: on that code vanilla's wolf halved every troop blow on a Moot Hound.

#### Gate 1: `wildmoot_normal` vs `ironclad_normal` (report-only)

```
work/p2/t11-sim1-005751/botsim/results.csv: wildmoot_normal on dawn: won=0 lost=10 drawn=0 aborted=0
work/p2/t11-sim2-005811/botsim/results.csv: wildmoot_normal on dusk: won=2 lost=8 drawn=0 aborted=0
rounds=20 decided=20 won=2 lost=18 drawn=0
gate wildmoot_normal vs ironclad_normal win=10% fail
```

- `wildmoot_normal` won 2 of 20 (both on map 4 from Dusk's seat): P = 10%. Kills 18.9 a match against 97.0; 9.9 of its groups wiped a match against 0.1 of Ironclad's; fights 16.1 s mean (11.6 to 32.0).

#### Gate 2: `wildmoot_normal` vs `gilded_normal` (report-only)

```
work/p2/t11-sim3-015328/botsim/results.csv: wildmoot_normal on dawn: won=0 lost=10 drawn=0 aborted=0
work/p2/t11-sim4-015348/botsim/results.csv: wildmoot_normal on dusk: won=0 lost=10 drawn=0 aborted=0
rounds=20 decided=20 won=0 lost=20 drawn=0
gate wildmoot_normal vs gilded_normal win=0% fail
```

- `wildmoot_normal` won 0 of 20: P = 0%. Kills 21.8 a match against 96.7; 10.1 of its groups wiped a match against 0.4 of Gilded's; fights 17.9 s mean (13.8 to 22.7).

Result: Wildmoot loses both pairings by a wide margin, on kills, not on points. Nothing is tuned now; the end balance pass owns it.

### Phase 2 Task 8b gates: the no-routing game (2026-10-22; `graveborn_normal` vs `gilded_normal` 17.5%, fail, one tuning round below; `ironclad_normal` vs `gilded_normal` 60%, report-only, inside the band)

Code: the Task 8b tree on `cdcf095` (the commits of this task), at the design's own numbers (`docs/superpowers/specs/2026-10-22-no-routing-design.md`, section 8). The controller's ruling for 8b: the `graveborn_normal` pairing must pass (40 <= P <= 60 over 20 decided matches; if not, one tuning round on the design's own numbers; if that still fails, stop and report), and the `ironclad_normal` pairing is report-only. (The design's section 10 names the gates the other way round; the ruling was followed.) Both reruns are the new baselines; the routing-game results stay below as the routing reference: `graveborn_normal` vs `gilded_normal` 47.5% (the balance fix of 2026-10-22, at the same troop rows as here) and `ironclad_normal` vs `gilded_normal` 75% (the same entry; 45% in the Task 8 gate before the Squire and Templar cuts).

What reaches these fights that the routing game did not have: no group ever breaks (a beaten group fights on; a bot army still leaves a fight only by its own Retreat order at `retreatShare`, and now arrives at its base and holds there until `rallyShare`); flank blows x1.30 (C16, was 1.15) and flank shots x1.15 (C16s, new); Bulwark's rout guard became covered flanks (no flank row against a troop within 5, 8 or 12 of a Gilded troop of another group); `fearless` is +20% damage at or below 30% health (was a 12% rout share); `dread` keeps its damage cut only. No horn sounds in the field and nothing a bot does makes a sound.

Both gates use the recipe unchanged (two processes of 10 matches, `-PsimDawn=normal -PsimDusk=normal -PsimMaps=0,1,2,3,4 -PsimRoundSeconds=300`, a fresh server root each, nothing else running). Ports 25651 and 25652 for gate 1 (roots `work/p2/t8b-sim1-143548` and `work/p2/t8b-sim2-143608`, servers up 14:36 to 15:31), 25653 and 25654 for gate 2 (`work/p2/t8b-sim3-153227` and `work/p2/t8b-sim4-153247`, 15:32 to 16:28); every round ran its full 300 s and none aborted. The `[botsim] health` lines (90 a process) show no burning troop in any of the 360 samples and at most 2 troops stuck for 30 s in 355 of them (gate 1: two samples with 4, both match 1 at 270 s; gate 2: three samples above 2).

`BotSimulation` now writes the design's fight stats to `results.csv` (columns `dawnKills, duskKills, dawnWiped, duskWiped, dawnRetreats, duskRetreats, fights, meanFightSeconds`; `work/p2/t8b/fightstats.py` sums them): kills per side are the other side's `stats.lost` (a death by any cause); a group is wiped out when a bot army's group goes from living regular troops to none (temporaries excluded; a group refilled from the barracks can be wiped again); a retreat is the bot's own Retreat order (`Campaign.botRetreating`); a fight opens at the first blow logged while none is open and closes at the next wipe or retreat (or the round's end), so the mean fight length runs from first contact to one side's group wiped or retreating.

#### Gate 1: `graveborn_normal` vs `gilded_normal` (must pass, 40 <= P <= 60)

```
work/p2/t8b-sim1-143548/botsim/results.csv: graveborn_normal on dawn: won=1 lost=9 drawn=0 aborted=0
work/p2/t8b-sim2-143608/botsim/results.csv: graveborn_normal on dusk: won=2 lost=7 drawn=1 aborted=0
  map 0 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 1 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 2 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 3 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 4 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 0 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 1 (dusk): won=0 lost=1 drawn=1 aborted=0
  map 2 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 3 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 4 (dusk): won=1 lost=1 drawn=0 aborted=0
rounds=20 decided=20 won=3 lost=16 drawn=1
gate graveborn_normal vs gilded_normal win=17.5% fail
```

- `graveborn_normal` won 3 of 20 (1 of 10 from Dawn's seat, 2 of 10 from Dusk's), lost 16 and drew 1: P = 17.5%, fail. Against the routing baseline (47.5%) it moved 30 points down, back to where the Task 7 gate stood with routing at the spec numbers (15%) before the balance fix.
- Mean final score 69.8 to 90.8 for Graveborn (a gap of -21.0; the balance fix read +7.0).
- Fight stats (means over the 20 matches, seat-pooled): Graveborn killed 61.2 troops a match and lost 69.5; 3.65 of its groups were wiped out a match and it wiped 3.40 of Gilded's; the bots ordered 0.05 (Graveborn) and 0.50 (Gilded) retreats a match; 8.35 fights a match of 26.5 s mean length (the per-match means run 17.8 to 45.2 s). Kingsfield (map 4) fights are half the size (about 30 kills a side).
- Why (a diagnosis from the mechanics, not an ablation): in a bot melee most blows land on someone's flank or rear. `gilded_normal` is tier III, so with its archers and longbows within 12 of the line nearly every Gilded troop is covered and takes no flank row, while every Graveborn troop takes x1.30 from a blow and x1.15 from a shot behind it. In the routing game C16 (1.15) applied to both sides alike and Bulwark's guard only stopped routs, which `gilded_normal` rarely did anyway; so the design hands Gilded a one-sided flank multiplier that the fearless damage bonus (a Risen at 30% is one or two blows from death) does not pay back.

#### Gate 2: `ironclad_normal` vs `gilded_normal` (report-only)

```
work/p2/t8b-sim3-153227/botsim/results.csv: ironclad_normal on dawn: won=6 lost=4 drawn=0 aborted=0
work/p2/t8b-sim4-153247/botsim/results.csv: ironclad_normal on dusk: won=6 lost=4 drawn=0 aborted=0
  map 0 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 1 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 2 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 3 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 4 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 0 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 1 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 2 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 3 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 4 (dusk): won=2 lost=0 drawn=0 aborted=0
rounds=20 decided=20 won=12 lost=8 drawn=0
gate ironclad_normal vs gilded_normal win=60% pass
```

- `ironclad_normal` won 12 of 20 (6 of 10 from each seat), lost 8, no draws: P = 60%, inside the band on its upper edge. Against the routing baseline (75%) it moved 15 points down; the Task 8 gate with routing at the spec Gilded rows read 45%.
- Mean final score 75.2 to 76.5 (a gap of -1.3).
- Fight stats: Ironclad killed 75.6 a match and lost 39.1; it wiped 4.35 Gilded groups a match and lost 0.60 of its own; retreats 0.00 (Ironclad) and 0.15 (Gilded); 6.0 fights a match of 32.2 s mean length (15.3 to 70.3). Ironclad wins the killing two to one and the points by one match in twenty: the Gilded groups die on the point and refill from the barracks while the Ironclad line is slow to move onto it. With routing they ran from the point at 25% and Ironclad took it; fighting to the last stand is what moved this number down.

#### Tuning round (ruling 1: the design's own numbers only, each within 15% of its value)

Round 1 (2026-10-22), the one round the ruling allows, on top of the design's numbers. The levers that bear on this pairing moved and no other (the bots have no commander, so Stand Fast and Muster cannot matter; `graveborn_normal` fields no Hollow King, so dread cannot): C16 1.30 to 1.105 (-15%), C16s 1.15 to 1.05 (-8.7%: a shot into a flank must still land harder for its text to hold), the Bulwark cover radii 5, 8 and 12 to 4.25, 6.8 and 10.2 (-15%; the `gilded_1..3` texts said the new numbers), `fearless` amount 0.20 to 0.23 (+15%) and amount2 0.30 to 0.345 (+15%; text "Below 34.5% health he deals 23% more damage."). The direction: the flank rows are the one-sided edge (a tier III Gilded troop is covered, a Graveborn troop never), so both come down as far as the rule allows, the cover shrinks, and the dead hit harder and sooner. Ports 25655 and 25656, roots `work/p2/t8b-sim5-163529` and `work/p2/t8b-sim6-163549`, servers up 16:35 to 17:31; all 20 rounds ran their 300 s; no burning troop and no sample with more than 2 stuck troops in the 180.

```
work/p2/t8b-sim5-163529/botsim/results.csv: graveborn_normal on dawn: won=3 lost=7 drawn=0 aborted=0
work/p2/t8b-sim6-163549/botsim/results.csv: graveborn_normal on dusk: won=3 lost=6 drawn=1 aborted=0
  map 0 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 1 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 2 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 3 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 4 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 0 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 1 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 2 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 3 (dusk): won=0 lost=1 drawn=1 aborted=0
  map 4 (dusk): won=1 lost=1 drawn=0 aborted=0
rounds=20 decided=20 won=6 lost=13 drawn=1
gate graveborn_normal vs gilded_normal win=32.5% fail
```

- `graveborn_normal` won 6 of 20 (3 from each seat), lost 13 and drew 1: P = 32.5%, fail. The round moved it 15 points up from 17.5% and left it 7.5 under the band. Mean final score 73.7 to 86.8 (a gap of -13.1, from -21.0).
- Fight stats: Graveborn killed 64.3 a match and lost 66.5 (from 61.2 and 69.5); 3.55 of its groups were wiped out a match and it wiped 3.35 (from 3.65 and 3.40); retreats 0.00 (Graveborn) and 0.40 (Gilded); 8.15 fights a match of 26.9 s mean length (16.7 to 35.8).
- Result: the tuning round fails. By the ruling it is not committed: the sheet stays at the design's numbers (`Spec.java` with it), and the pairing goes to Kort in `docs/QUESTIONS-FOR-KORT.md` with these numbers while Task 9 starts anyway (the design's stop rule). What the round says: the four levers together moved it 15 points (within noise of about +/-10 at n = 20), so no single lever can be credited; the covered-flank mechanism the diagnosis names is plausible but this data does not rank it. The balance pass's first step is a one-lever ablation (C16 alone at 1.105, 20 rounds, then the cover radii alone), and `results.csv` now carries per-side C16 and C16s counts (`dawnFlankHits`, `duskFlankHits`, `dawnFlankShots`, `duskFlankShots`, from `MatchStats.SquadStats`, counted in `Combat.resolve`) so the asymmetry is measured rather than inferred. With C16 at 1.105 Gilded still wins two of three, so a part of the gap is the no-routing game itself, where `fearless` was worth more as a 12% rout share against Gilded's 25% than as +20% at the edge of death, on top of the Bulwark ranged cut the balance fix already paid for. Closing it stays with Phase 2's balance pass, which may move the troop rows 8b's rule did not allow.

How much longer and bloodier fights got against the routing game: the old logs cannot say. The routing-game `results.csv` had none of the fight-stat columns and `gold.csv` samples living troops per team every 30 s only (losses are not kills: reinforcements refill between samples), so the kill, wipe, retreat and fight-length numbers above are the first baseline for the next gates; Tasks 9 and 10 compare against them.

### Phase 2 balance fix: Gilded Squire 28 and Templar 40, `graveborn_normal` vs `gilded_normal` passes (2026-10-22, 47.5%; `ironclad_normal` vs `gilded_normal` reads 75%, report-only)

Code: `phase-2-houses` at `bed3fec` with the two Gilded rows of the tuning log below changed (the commit that carries this entry). The Task 7 gate had failed at 0 of 28 with the Graveborn numbers within 15% of spec, and the options went to the controller. The controller ruled that Gilded's rows may move, on the diagnosis of 2026-10-21 (`.superpowers/sdd/2026-10-17-phase-2-houses/balance-diagnosis.md` in the main checkout). Its cause, in short: a ranged troop shoots the nearest enemy, which is the enemy front, and the Gilded front is ten shield troops under Bulwark III, so 85% of Graveborn's arrows land on a Squire or a Templar at about 28% of their base damage (counters C1 or C2 times Bulwark x0.55, clamped at 0.25), while no Gilded arrow meets a shield. Graveborn loses the first clash and never recovers the points. Cutting the two Gilded infantry rows to the lowest whole numbers within the 15% limit is the smallest change that brought a 20-match gate into the band (46% pooled over 40 matches in the diagnosis); the Graveborn rows stay at the round 2 numbers.

Both gates use the plan's recipe unchanged (two processes of 10 matches, `-PsimDawn=normal -PsimDusk=normal -PsimMaps=0,1,2,3,4 -PsimRoundSeconds=300`, a fresh server root each, nothing else running). Ports 25641 and 25642 for gate 1, 25643 and 25644 for gate 2; every round ran its full 300 s.

#### Gate 1: `graveborn_normal` vs `gilded_normal` (blocking, 40 <= P <= 60)

Roots `work/p2/bf/g1a-004759` (`graveborn_normal` on team 0) and `work/p2/bf/g1b-004810` (`gilded_normal` on team 0).

```
[botsim] matches=10 dawn(normal)=7 dusk(normal)=3 draws=0 dawnWin=70% meanGoldEarnedAtEnd dawn=1592 dusk=1445 banner0=graveborn_normal banner1=gilded_normal
[botsim] matches=10 dawn(normal)=7 dusk(normal)=2 draws=1 dawnWin=70% meanGoldEarnedAtEnd dawn=1558 dusk=1512 banner0=gilded_normal banner1=graveborn_normal
```

```
work/p2/bf/g1a-004759/botsim/results.csv: graveborn_normal on dawn: won=7 lost=3 drawn=0 aborted=0
work/p2/bf/g1b-004810/botsim/results.csv: graveborn_normal on dusk: won=2 lost=7 drawn=1 aborted=0
  map 0 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 1 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 2 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 3 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 4 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 0 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 1 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 2 (dusk): won=0 lost=1 drawn=1 aborted=0
  map 3 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 4 (dusk): won=0 lost=2 drawn=0 aborted=0
rounds=20 decided=20 won=9 lost=10 drawn=1
gate graveborn_normal vs gilded_normal win=47.5% pass
```

- Mean final score 84.8 to 77.8 for Graveborn (a gap of +7.0): 90.9 to 69.6 with Graveborn on Dawn, 78.7 to 86.1 on Dusk. Graveborn won no map-4 round from either seat; the Dawn seat won 14 of the 20 rounds whichever House held it (the diagnosis' own confirming gate showed the Dusk seat winning 15 of 20), so the seat edge swings from run to run and only the seat-swapped total means anything.
- The diagnosis' confirming gate of the same two rows read 47.5% as well (9 won, 10 lost, 1 drawn, gap -2); the two runs pool to 18 won, 20 lost, 2 drawn of 40.

#### Gate 2: `ironclad_normal` vs `gilded_normal` (report-only)

Roots `work/p2/bf/g2a-014428` (`ironclad_normal` on team 0) and `work/p2/bf/g2b-014429` (`gilded_normal` on team 0).

```
[botsim] matches=10 dawn(normal)=7 dusk(normal)=3 draws=0 dawnWin=70% meanGoldEarnedAtEnd dawn=1489 dusk=1431 banner0=ironclad_normal banner1=gilded_normal
[botsim] matches=10 dawn(normal)=2 dusk(normal)=8 draws=0 dawnWin=20% meanGoldEarnedAtEnd dawn=1428 dusk=1553 banner0=gilded_normal banner1=ironclad_normal
```

```
work/p2/bf/g2a-014428/botsim/results.csv: ironclad_normal on dawn: won=7 lost=3 drawn=0 aborted=0
work/p2/bf/g2b-014429/botsim/results.csv: ironclad_normal on dusk: won=8 lost=2 drawn=0 aborted=0
rounds=20 decided=20 won=15 lost=5 drawn=0
gate ironclad_normal vs gilded_normal win=75% fail
```

- Mean final score 79.3 to 71.2 for Ironclad (a gap of +8.1): 74.8 to 71.4 on Dawn, 83.8 to 71.0 on Dusk. No tuning was done (report-only by ruling).
- The Task 8 gate read 45% at the spec Gilded numbers. The same pairing now reads 75%: the Squire and Templar cuts (-12.5% and -13%) soften the Gilded front, which is what the Ironclad line (sunder x1.4 and armour bolt x0.7 against a raised shield, siege) attacks; that reading was not isolated by an ablation. With 20 matches the standard error is about 10 points, so the swing is real in direction and rough in size. The Ironclad batch's own balance pass has to start from this number, not from 45%.
- Because `ironclad_normal` now beats `gilded_normal` by more than `gilded_normal` beats `graveborn_normal`, the "current best pure banner" above needs a decision from the controller; it is left as it stood.

### Phase 2 Task 8 gate: `ironclad_normal` vs `gilded_normal` (2026-10-21, report-only, 45% at the spec numbers: inside the band)

Code: the Task 8 tree (committed as `f0febad`), on `842d2c7`. The gate is report-only by ruling (the gates of Tasks 8 to 10 do not block until the balance pass), so nothing was tuned; it would have passed anyway. What reaches this fight:

- The `ironclad_normal` banner: an Ironhold Mortar (2 slots), 6 Hearthguards, 4 Arbalists, 4 Shieldbreakers and 4 Ballista Crew, 20 slots, which is tier III of Entrench (14 slots).
- Entrench: a troop that has stood still for 60 ticks takes 20% less damage (40% less on a point its team holds), and a still Mortar or Ballista Crew shoots 30% further (the Ballista Crew's 34 becomes 44.2, the Mortar's 36 becomes 46.8).
- The siege troops engage at their reach rather than at the order's radius (24 on attack, 10 on hold), and the Mortar casts nothing at an enemy within 10 blocks and walks away from one.
- The counters of the siege and the Ironclad weapons against the Gilded line: the Shieldbreaker's sunder x1.4 against a raised shield (C14), the Arbalist's armour bolt x0.7 against one (C15), siege against large x1.3 and against small x0.6 (C12, C13).
- Wide bodies in formation (Brassjaw's cell) and the siege troops standing behind the archers.

What does not reach it: Brassjaw (the hard banner only), the Quartermaster's Forward Supply (bots never use actives) and the structure counters (no structure stands in a bot match).

The gate rule is 20 matches and 40 <= P <= 60; all 20 ran. Process 1 (port 25621, `ironclad_normal` on team 0) and process 2 (port 25622, `gilded_normal` on team 0) each held its own server root; every round was a full 300 s; servers up 20:59 to 21:55. The `[botsim] health` lines (90 samples a process) show no burning troop in any of the 180, and at most 2 troops that stood still for 30 s with no target, no rout and no Hold order in 178 of them; two samples had more (process 1: match 2 at 210 s with 5 and match 7 at 210 s with 4), each followed 30 s later by one with none or fewer.

```
[botsim] matches=10 dawn(normal)=3 dusk(normal)=7 draws=0 dawnWin=30% meanGoldEarnedAtEnd dawn=1424 dusk=1512 banner0=ironclad_normal banner1=gilded_normal
[botsim] matches=10 dawn(normal)=4 dusk(normal)=6 draws=0 dawnWin=40% meanGoldEarnedAtEnd dawn=1481 dusk=1500 banner0=gilded_normal banner1=ironclad_normal
```

Gate (`python tools/balance_gate.py <root1>/botsim/results.csv <root2>/botsim/results.csv ironclad_normal --detail`; the roots are `work/p2/t8-sim1-205946` and `work/p2/t8-sim2-210007`):

```
work/p2/t8-sim1-205946/botsim/results.csv: ironclad_normal on dawn: won=3 lost=7 drawn=0 aborted=0
work/p2/t8-sim2-210007/botsim/results.csv: ironclad_normal on dusk: won=6 lost=4 drawn=0 aborted=0
  map 0 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 1 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 2 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 3 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 4 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 0 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 1 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 2 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 3 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 4 (dusk): won=2 lost=0 drawn=0 aborted=0
rounds=20 decided=20 won=9 lost=11 drawn=0
gate ironclad_normal vs gilded_normal win=45% pass
```

What the numbers say (the last gold sample of a round is at 270 s):

- `ironclad_normal` won 9 of 20 (3 of 10 from Dawn's seat, 6 of 10 from Dusk's). Across both processes the Dusk seat won 13 of the 20 matches whichever House sat in it, so in this pairing the seat matters more than the banner; the gate's ten matches per seat cancel it.
- Mean final score 70.8 to 80.2; mean gold earned 1225 to 1366; troops alive at 270 s 55.2 to 44.5 of 57. Ironclad loses the objective race and wins the attrition: it ends each round with 10 more troops than Gilded and the lower score.
- By the second, the mean of the 20 rounds (gold earned, troops alive; Ironclad first):

```
30 gold IC/GL 72 72 living IC/GL 57.0 57.0 n 20
60 gold IC/GL 162 162 living IC/GL 57.0 57.0 n 20
90 gold IC/GL 257 273 living IC/GL 57.0 57.0 n 20
120 gold IC/GL 360 464 living IC/GL 56.8 55.6 n 20
150 gold IC/GL 475 692 living IC/GL 51.8 48.6 n 20
180 gold IC/GL 617 907 living IC/GL 50.8 42.7 n 20
210 gold IC/GL 788 1095 living IC/GL 54.2 42.1 n 20
240 gold IC/GL 994 1241 living IC/GL 56.4 44.7 n 20
270 gold IC/GL 1225 1366 living IC/GL 55.2 44.5 n 20
```

  Gold is level to 90 s (257 to 273) and Gilded leads from 120 s (360 to 464, 475 to 692 at 150 s, 617 to 907 at 180 s); Ironclad is ahead on troops from 150 s (51.8 to 48.6) and the lead widens to 50.8 against 42.7 at 180 s and 54.2 against 42.1 at 210 s. Why Ironclad earns less gold while losing fewer troops (slower troops reaching the points, or fewer kills paid) is not measured here.
- By count the banners are close (`ironclad_normal` is 20 slots with the Mortar's two; `gilded_normal` 19 troops with 552 hit points and 10 shield bearers).

Result: the gate passes at the spec numbers (P = 45%). `gilded_normal` stays the current best pure banner (85% against `graveborn_normal` in Task 7).

### Phase 2 Task 7 gate: `graveborn_normal` vs `gilded_normal` (2026-10-21, fails; two tuning rounds, stopped)

Code: the Task 7 tree (committed as `3324fa3`), on `f52c15a`. The commit carries the round 2 numbers; round 0 ran the same mechanics at the spec numbers (the sheet rows differ only as the tuning log lists, and the soul-fire particles and a rally-horn centre that ignores temporary troops came after it; neither touches a fight). What reaches this fight that the Task 6 gate (before Task 7) did not have:

- Rise Again. The 20 slots of `graveborn_normal` reach tier III (14 slots): each Graveborn troop that is not Legendary rises once, at 30% health and stunned for 20 ticks, with a 40% chance at spec (45% from round 2).
- Fearless. The Risen and the Bonebow carry it: a group at least half fearless breaks at 12% strength instead of 25% (the infantry group is 6 Risen of 10, the ranged group 4 Bonebow of 8).
- The Grave Reaper's cleave (half the blow to the nearest other enemy within 2) and the Barrow Stalker's frostshot (Slowness I for 2 s). The Gravecaller's blight was in already.
- The Risen is a zombie and the Barrow Stalker a stray (Army mobs: no daylight burn, no water conversion).

What does not reach it: the Hollow King's dread (the hard banner only) and Grave Bloom (bots never use actives).

The gate rule is 20 matches and 40 <= P <= 60. Round 0 ran all 20 at the spec numbers. Rounds 1 and 2 were stopped after 14 decided rounds each (7 per seat, all lost): with 6 rounds left the best possible share is 6 of 20 = 30%, under the band, so the rest could not change the verdict. Each process held its own server root and port (25601, 25602); every round was a full 300 s. The `[botsim] health` lines (a sample every 30 s) show no burning troop in any of the 455 samples (180, 141 and 134 per round) and, in 454 of them, at most 2 troops that stood still for the 30 s with no target, no rout and no Hold order; one sample had 15 (round 1, match 5, 180 s, map 4) and the next one, 30 s later, had none.

#### Round 0: the spec numbers (all 20 matches)

Process 1 (port 25601, `graveborn_normal` on team 0) and process 2 (port 25602, `gilded_normal` on team 0), servers up 16:10 to 17:05:

```
[botsim] matches=10 dawn(normal)=3 dusk(normal)=7 draws=0 dawnWin=30% meanGoldEarnedAtEnd dawn=1306 dusk=1639 banner0=graveborn_normal banner1=gilded_normal
[botsim] matches=10 dawn(normal)=10 dusk(normal)=0 draws=0 dawnWin=100% meanGoldEarnedAtEnd dawn=1686 dusk=1251 banner0=gilded_normal banner1=graveborn_normal
```

Gate (`python tools/balance_gate.py <root1>/botsim/results.csv <root2>/botsim/results.csv graveborn_normal --detail`; the roots are `work/p2/t7-sim1-160953` and `work/p2/t7-sim2-161013`):

```
work/p2/t7-sim1-160953/botsim/results.csv: graveborn_normal on dawn: won=3 lost=7 drawn=0 aborted=0
work/p2/t7-sim2-161013/botsim/results.csv: graveborn_normal on dusk: won=0 lost=10 drawn=0 aborted=0
  map 0 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 1 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 2 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 3 (dawn): won=1 lost=1 drawn=0 aborted=0
  map 4 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 0 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 1 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 2 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 3 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 4 (dusk): won=0 lost=2 drawn=0 aborted=0
rounds=20 decided=20 won=3 lost=17 drawn=0
gate graveborn_normal vs gilded_normal win=15% fail
```

What the numbers say (the last gold sample of a round is at 270 s):

- `graveborn_normal` won 3 of 20 (3 of 10 from Dawn's seat, 0 of 10 from Dusk's), so Gilded still leads from either seat; in the Task 6 gate Graveborn won none of 20, so the Graveborn mechanics moved it from 0% to 15%.
- Mean final score 47.8 to 99.8 (Task 6: 45.7 to 105.0); mean gold earned 1107 to 1466; troops alive at 270 s 48.9 to 45.6 of 57.
- By the second, the mean of the 20 rounds: gold is level to 120 s (402 to 409); at 150 s Graveborn has 46.7 troops left against Gilded's 52.5 and at 180 s 48.7 against 53.4; gold is 690 to 802 at 180 s and 957 to 1244 at 240 s. Gilded's losses pass Graveborn's only from 210 s (48.2 to 52.0 alive), when the points are already Gilded's. The gap is the first clash and the points it decides.
- By count and hit points the two banners are close (`graveborn_normal` is 466 hit points in 19 troops, `gilded_normal` 552 hit points in 19 troops with 10 shield bearers). Which part of the first clash Graveborn loses is not measured here; the Gilded shield bearers (x0.55 from arrows and blight, counters C1 to C3 on the front) face 9 of Graveborn's 19 troops that fire arrows or blight, and the Gilded longbow (range 30) outranges every Graveborn archer. That is a reading of the sheet, not a result of the run.

#### Round 1 (tuned, stopped at 14 decided rounds)

The changes are in the tuning log. Roots `work/p2/t7-sim1-171824` and `work/p2/t7-sim2-171844`.

```
work/p2/t7-sim1-171824/botsim/results.csv: graveborn_normal on dawn: won=0 lost=7 drawn=0 aborted=0
work/p2/t7-sim2-171844/botsim/results.csv: graveborn_normal on dusk: won=0 lost=7 drawn=0 aborted=0
  map 0 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 1 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 2 (dawn): won=0 lost=1 drawn=0 aborted=0
  map 3 (dawn): won=0 lost=1 drawn=0 aborted=0
  map 4 (dawn): won=0 lost=1 drawn=0 aborted=0
  map 0 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 1 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 2 (dusk): won=0 lost=1 drawn=0 aborted=0
  map 3 (dusk): won=0 lost=1 drawn=0 aborted=0
  map 4 (dusk): won=0 lost=1 drawn=0 aborted=0
rounds=14 decided=14 won=0 lost=14 drawn=0
gate graveborn_normal vs gilded_normal win=0% fail
```

- Mean final score 68.4 to 90.8; mean gold earned 1234 to 1435; troops alive at 270 s 52.0 to 43.0. At 120 s Graveborn is ahead on gold (503 to 384) and at 150 s it has 45.3 troops left against 55.1: it reaches the points first and still loses the first clash.

#### Round 2 (tuned further, stopped at 14 decided rounds)

Roots `work/p2/t7-sim1-181133` and `work/p2/t7-sim2-181153`.

```
work/p2/t7-sim1-181133/botsim/results.csv: graveborn_normal on dawn: won=0 lost=7 drawn=0 aborted=0
work/p2/t7-sim2-181153/botsim/results.csv: graveborn_normal on dusk: won=0 lost=7 drawn=0 aborted=0
  map 0 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 1 (dawn): won=0 lost=2 drawn=0 aborted=0
  map 2 (dawn): won=0 lost=1 drawn=0 aborted=0
  map 3 (dawn): won=0 lost=1 drawn=0 aborted=0
  map 4 (dawn): won=0 lost=1 drawn=0 aborted=0
  map 0 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 1 (dusk): won=0 lost=2 drawn=0 aborted=0
  map 2 (dusk): won=0 lost=1 drawn=0 aborted=0
  map 3 (dusk): won=0 lost=1 drawn=0 aborted=0
  map 4 (dusk): won=0 lost=1 drawn=0 aborted=0
rounds=14 decided=14 won=0 lost=14 drawn=0
gate graveborn_normal vs gilded_normal win=0% fail
```

- Mean final score 71.8 to 87.8; mean gold earned 1255 to 1423; troops alive at 270 s 51.6 to 39.5; at 150 s 46.5 against 54.1 alive.
- The gap in final score fell from 52 points (round 0) to 22 (round 1) and 16 (round 2), and Graveborn ends each round with more troops than Gilded, but it won no round of the 28 tuned ones. The closest rounds were on map 4: 97 to 101 with Graveborn on Dawn and 89 to 105 with it on Dusk.

Result: the gate fails. Two tuning rounds within 15% of the spec numbers did not bring `graveborn_normal` into the band, so the tuning stops here (the plan's rule), and the Graveborn numbers stay as round 2 left them (the tuning log lists every spec value, so a revert is mechanical). The Task 7 report puts the options to Kort.

### Phase 2 Task 6 gate: `gilded_normal` vs `graveborn_normal` (2026-10-19, report-only)

Code: the Task 6 tree (committed as `6264f6a`), on `786bb2b`. What reaches this fight:

- The Order Longbow's pierce: each arrow also hits the nearest enemy within 2 blocks behind its target for half the shot.
- Gilded Bulwark. `gilded_normal`'s 20 Gilded slots reach tier III:
  - its 10 shield bearers take x0.55 from arrows, bolts and blight;
  - a group within 12 blocks of a Gilded troop of another group of its army does not rout.
- The Stand Fast and Oath Banner rout guards are registered too, but they act only while their active or banner stands.

What does not reach it:

- The Oath Banner: `gilded_normal` has no Lord Marshal.
- The Champion's and the Marshal's actives: bots never use actives.

Report-only, by the controller's ruling in progress.md ("Task 6 gates after Tasks 4 and 7 land"): nothing was tuned.

Process 1 (port 25601, `gilded_normal` on team 0, `graveborn_normal` on team 1), server up 07:59:41 to 08:54:56. Every round ran its full 300 s.

```
[botsim] matches=10 dawn(normal)=10 dusk(normal)=0 draws=0 dawnWin=100% meanGoldEarnedAtEnd dawn=1755 dusk=1238 banner0=gilded_normal banner1=graveborn_normal
```

Process 2 (port 25602, `graveborn_normal` on team 0, `gilded_normal` on team 1), server up 08:00:08 to 08:56:14.

```
[botsim] matches=10 dawn(normal)=0 dusk(normal)=10 draws=0 dawnWin=0% meanGoldEarnedAtEnd dawn=1240 dusk=1763 banner0=graveborn_normal banner1=gilded_normal
```

Gate (`python tools/balance_gate.py <root1>/botsim/results.csv <root2>/botsim/results.csv gilded_normal --detail`; the roots are `work/p2/task6-sim1-075914` and `work/p2/task6-sim2-075935`):

```
work/p2/task6-sim1-075914/botsim/results.csv: gilded_normal on dawn: won=10 lost=0 drawn=0 aborted=0
work/p2/task6-sim2-075935/botsim/results.csv: gilded_normal on dusk: won=10 lost=0 drawn=0 aborted=0
  map 0 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 1 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 2 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 3 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 4 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 0 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 1 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 2 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 3 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 4 (dusk): won=2 lost=0 drawn=0 aborted=0
rounds=20 decided=20 won=20 lost=0 drawn=0
gate gilded_normal vs graveborn_normal win=100% fail
```

The gate fails at 100%, outside the 40 to 60 band. That is 7.5 points above the Task 3 baseline after fix round 1 (92.5%): that baseline's one loss and one draw are wins here, one and a half rounds of 20. It stays report-only until the Graveborn batch (Task 7) lands; Graveborn still fights without its own mechanics.

What the numbers say (from the two runs' `results.csv` and `gold.csv`; the last gold sample of a round is at 270 s):

- `gilded_normal` won 20 of 20, 10 from each seat. Every close round was on map 4: 100 to 95 and 108 to 83 from Dawn's seat, 100 to 93 and 101 to 100 from Dusk's. On maps 0 to 3 the margin was 58 to 103 points.
- Mean final score 114.0 to 42.6 (Task 3 after fix round 1: 100.8 to 47.8). Mean gold earned 1549 to 1071 (1470 to 1118).
- Troops still alive at the 270 s sample, out of 57: Gilded 48.5 (from 47.1), Graveborn 50.0 (from 49.9). Pierce and Bulwark barely moved who survives, so the gap is still about holding points, not kills.
- Graveborn's fearless, cleave, frostshot, Rise Again and Dread arrive with Task 7. Its live damage here is the Bone Archers' and Barrow Stalkers' arrows and the Gravecaller's blight, which is exactly what Bulwark cuts on the ten Gilded shield bearers.

### Phase 2 Task 3 baseline after fix round 1: `gilded_normal` vs `graveborn_normal` (2026-10-19, no gate)

Code: `0533219` plus fix round 1 (`418797c`). The one change that reaches the fight is that a troop's armour is now its worn gear only: no Gilded troop carries the husk's built-in armour of 2 any more, and no troop of any House takes armour from its mob type. The other ten review fixes touch the sheet (one new `combatDiscipline` column, the charge-impact window, with the same value), the camp UI, the harness and the docs, not the rules. Report only: nothing was tuned.

Process 1 (port 25601, `gilded_normal` on team 0, `graveborn_normal` on team 1), server up 00:19:39 to 01:14:34. Every round ran its full 300 s.

```
[botsim] matches=10 dawn(normal)=9 dusk(normal)=0 draws=1 dawnWin=90% meanGoldEarnedAtEnd dawn=1677 dusk=1278 banner0=gilded_normal banner1=graveborn_normal
```

Process 2 (port 25602, `graveborn_normal` on team 0, `gilded_normal` on team 1), server up 00:20:00 to 01:14:56.

```
[botsim] matches=10 dawn(normal)=1 dusk(normal)=9 draws=0 dawnWin=10% meanGoldEarnedAtEnd dawn=1292 dusk=1666 banner0=graveborn_normal banner1=gilded_normal
```

Gate (`python tools/balance_gate.py <root1>/botsim/results.csv <root2>/botsim/results.csv gilded_normal --detail`; the roots are `work/p2/task3-fix1-sim1-001855` and `work/p2/task3-fix1-sim2-001916`):

```
work/p2/task3-fix1-sim1-001855/botsim/results.csv: gilded_normal on dawn: won=9 lost=0 drawn=1 aborted=0
work/p2/task3-fix1-sim2-001916/botsim/results.csv: gilded_normal on dusk: won=9 lost=1 drawn=0 aborted=0
  map 0 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 1 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 2 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 3 (dawn): won=1 lost=0 drawn=1 aborted=0
  map 4 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 0 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 1 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 2 (dusk): won=1 lost=1 drawn=0 aborted=0
  map 3 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 4 (dusk): won=2 lost=0 drawn=0 aborted=0
rounds=20 decided=20 won=18 lost=1 drawn=1
gate gilded_normal vs graveborn_normal win=92.5% fail
```

The baseline is outside the 40 to 60 band. Task 3 has no gate, so nothing was tuned; the Gilded batch (plan Task 6, Step 8) gates `gilded_normal` against `graveborn_normal` again, and the Graveborn batch (Task 7, Step 9) gates `graveborn_normal` against the current best. It is 2.5 points below the first baseline's 95%, which was measured with the husk's +2 armour on the Gilded side. The difference is one win turned into a draw (half a round of 20), well inside the noise of 20 rounds, so removing that armour did not by itself close the gap.

What the numbers say (from the two runs' `results.csv` and `gold.csv`; the last gold sample of a round is at 270 s):

- `gilded_normal` won 18 of 20 (9 of 10 from Dawn's seat, 9 of 10 from Dusk's), 1 drawn. The rounds it lost: map 2 (graveborn_normal on Dawn, 75 to 71).
- Mean final score 100.8 to 47.8 (first baseline 105.0 to 45.7); mean gold earned 1470 to 1118 (1487 to 1106); troops still alive at the 270 s sample 47.1 to 49.9 of 57 (49.9 to 48.5 in the first baseline): Gilded had lost more troops than in the first baseline and Graveborn fewer, and the outcome did not move with it.

### Phase 2 Task 3 baseline, first run: `gilded_normal` vs `graveborn_normal` (2026-10-18, no gate; measured with the husk's +2 armour on every Gilded troop)

Code: base `01f9f5c` plus the Task 3 combat engine (ArmyMobs, Combat.resolve with the troop damage type, abilities shell, Houses, state_v8). In this build the counters table (the shield rows C1 to C3, the flank row and the stance rows), mend and blight act; every trait, aura and commander factor is 1 and every other ability is a no-op until its House batch. The Order Archer's `steady` is live through counters row C18a (x1.2 on its own hits while its group holds; `gilded_normal` fields four archers). Every Gilded troop was a husk and carried the husk's built-in armour of 2 on top of its sheet gear, which no sheet holds and no Graveborn troop (skeletons have none) carried: this baseline was measured with that +2 on the Gilded side. Fix round 1 removed it (a troop's armour is its worn gear only); the corrected baseline is the section above.

Process 1 (port 25601, `gilded_normal` on team 0, `graveborn_normal` on team 1), server up 21:57:44 to 22:52:10. Every round ran its full 300 s.

```
[botsim] matches=10 dawn(normal)=10 dusk(normal)=0 draws=0 dawnWin=100% meanGoldEarnedAtEnd dawn=1698 dusk=1262 banner0=gilded_normal banner1=graveborn_normal
```

Process 2 (port 25602, `graveborn_normal` on team 0, `gilded_normal` on team 1), server up 21:59:29 to 22:53:58.

```
[botsim] matches=10 dawn(normal)=1 dusk(normal)=9 draws=0 dawnWin=10% meanGoldEarnedAtEnd dawn=1278 dusk=1702 banner0=graveborn_normal banner1=gilded_normal
```

Gate (`python tools/balance_gate.py <root1>/botsim/results.csv <root2>/botsim/results.csv gilded_normal --detail`; the roots are `work/p2/task3-sim1-215714` and `work/p2/task3-sim2-215859`):

```
work/p2/task3-sim1-215714/botsim/results.csv: gilded_normal on dawn: won=10 lost=0 drawn=0 aborted=0
work/p2/task3-sim2-215859/botsim/results.csv: gilded_normal on dusk: won=9 lost=1 drawn=0 aborted=0
  map 0 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 1 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 2 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 3 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 4 (dawn): won=2 lost=0 drawn=0 aborted=0
  map 0 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 1 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 2 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 3 (dusk): won=2 lost=0 drawn=0 aborted=0
  map 4 (dusk): won=1 lost=1 drawn=0 aborted=0
rounds=20 decided=20 won=19 lost=1 drawn=0
gate gilded_normal vs graveborn_normal win=95% fail
```

The baseline is outside the 40 to 60 band. Task 3 has no gate, so nothing was tuned; the Gilded batch (plan Task 6, Step 8) gates `gilded_normal` against `graveborn_normal` again, and the Graveborn batch (Task 7, Step 9) gates `graveborn_normal` against the current best.

What the numbers say (from the two runs' `results.csv` and `gold.csv`; the last gold sample of a round is at 270 s):

- `gilded_normal` won 19 of 20 (10 of 10 from Dawn's seat, 9 of 10 from Dusk's), so the seat does not explain it. The rounds it lost: map 4 (graveborn_normal on Dawn, 104 to 96).
- Mean final score 105.0 to 45.7; mean gold earned 1487 to 1106; troops still alive at the 270 s sample 49.9 to 48.5 of 57 (three armies of 19 on a side). The fights barely thin either side, so the gap is objective score, not kills.
- The two banners as the sheet has them: `gilded_normal` is 19 troops with 552 hit points and 10 shield bearers (counters C1 to C3 cut arrows and melee from the front); `graveborn_normal` is 19 troops with 466 hit points and 0 shield bearers. Graveborn's fearless, cleave, frostshot, Rise Again and Dread are not built until the Graveborn batch, so its one live mechanic is the Gravecaller's blight; Gilded's live ones are the shield rows, the Order Archer's `steady` stance (C18a) and the Sunweaver's mend. The Gilded armour was also 2 higher than its gear says (above), roughly 9 to 11% less damage taken from every troop blow.

## Tuning log

Phase 2 Tasks 3, 6 and 8 changed no sheet numbers (the Task 6 and Task 8 gates are report-only by ruling; the Task 8 gate passed at the spec numbers, 45%). Task 8b changed the rows its design names (section 8 of the no-routing design; its first commit lists them) and nothing else; its one tuning round is logged under its gate entry and was not committed (it read 32.5%, still under the band). Task 7 changed these Graveborn rows in `sheets/design.json`, each within 15% of its spec value; the percentages are against the spec value, and a whole-number column (health, damage, cooldown) moves in whole numbers.

Round 1 (2026-10-21), the five troops of `graveborn_normal`:

- Risen (revenant): health 26 to 29 (+11.5%), cooldown 25 to 22 ticks (-12%), speed 0.25 to 0.28 (+12%).
- Bonebow: health 18 to 20 (+11.1%), cooldown 40 to 35 (-12.5%), speed 0.27 to 0.30 (+11.1%).
- Grave Reaper: health 34 to 39 (+14.7%), damage 8 to 9 (+12.5%), cooldown 30 to 26 (-13.3%), speed 0.30 to 0.33 (+10%).
- Barrow Stalker: health 20 to 22 (+10%), cooldown 40 to 35 (-12.5%), speed 0.30 to 0.33 (+10%).
- Gravecaller (hexer): health 22 to 25 (+13.6%), damage 7 to 8 (+14.3%), cooldown 55 to 48 (-12.7%), speed 0.27 to 0.30 (+11.1%).
- Result: 0 of 14, mean final score 68.4 to 90.8 (round 0: 47.8 to 99.8).

Round 2 (2026-10-21), on top of round 1:

- Reach: Risen and Grave Reaper 2.8 to 3.0 (+7.1%; preflight keeps a melee reach at 3 or less), Bonebow 22 to 25 (+13.6%), Barrow Stalker 26 to 29 (+11.5%), Gravecaller 18 to 20 (+11.1%).
- Training time: Risen and Bonebow 3.0 to 2.6 s (-13.3%), Grave Reaper and Barrow Stalker 6.0 to 5.2 s (-13.3%), Gravecaller 8.0 to 7.0 s (-12.5%).
- Rise Again chance (`graveborn_1`, `graveborn_2`, `graveborn_3`): 10% to 11% (+10%), 25% to 28% (+12%), 40% to 45% (+12.5%); the row texts say the new numbers.
- Cleave (`cleave_reaper`): 50% to 57% of the blow (+14%); the row text says it.
- Result: 0 of 14, mean final score 71.8 to 87.8.

Not changed: the Hollow King (not in the normal banner), costs, the fearless, dread, frostshot and blight rows, and the Gilded rows. The Task 7 tests that read a Graveborn number (the Rise Again chances and the cleave share) follow the tuned sheet.

Round 3 (2026-10-22), the Gilded rows, on top of round 2. Controller ruling: Gilded's rows may move. Cause: a ranged troop shoots the nearest enemy, which is the shield front, and Bulwark III (x0.55) on top of the shield counters leaves Graveborn's arrows about 28% of their base damage (diagnosis section 2, `.superpowers/sdd/2026-10-17-phase-2-houses/balance-diagnosis.md`). Each is a health cut to the lowest whole number within the 15% limit (27.2 and 39.1 are the floors):

- Order Squire (`knight`): health 32 to 28 (-12.5%).
- Templar (`sentinel`): health 46 to 40 (-13.0%).
- Result: gate 1, `graveborn_normal` vs `gilded_normal`, 9 won, 10 lost, 1 drawn of 20, P = 47.5%, pass (mean final score 84.8 to 77.8). Gate 2, `ironclad_normal` vs `gilded_normal`, report-only, 15 of 20, P = 75% (45% before the cut).

Not changed in round 3: every Graveborn row (they stay at the round 2 numbers), the other Gilded rows (the Order Archer, Order Longbow and Sunweaver health stay at spec; the diagnosis' five-row set that also cut them is the fallback if a later gate drifts under 40%), costs, counters and traits. No check hardcodes the Squire's or the Templar's health: `HousesCheck`, `BattleMathCheck` and `Phase2Checks` read the rows through `Campaign.troop(...)` and `Spec`, so none was changed.

In-match progression Task 11 (2026-10-24): two tuning rounds, both lever 1 of design 9.2 (thresholds), both Kort's round-35 answer to "levels arrive early in the headless sim". Each is checked by `python tools/preflight.py` (Preflight clean; `Spec.java` regenerated, never hand-edited), the standalone checks and a rerun of the `on` condition (details under "In-match progression gate" in Results).

Round 1 (commit `522c856`):

- `levels.l2.kills` 3 -> 4 and `levels.l3.kills` 8 -> 12. Reason: the as-designed gate passed both rules, but the first level 2 landed at 115 s (aim 120 to 180 s) and a round saw 49.5 level 3s (aim a few).
- Result: gate pass; first level 2 118 s, winner end share 41.2%, level 3s 32.0 a round.

Round 2 (commit `a4900fb`), on top of round 1:

- `levels.l2.kills` 4 -> 5 and `levels.l3.kills` 12 -> 14. Reason: the first level 2 was still under about 120 s and the level 3s a round still far above 8.
- Result: gate pass; first level 2 120 s, winner end share 35.5%, level 3s 27.0 a round (aim not met; asked in `docs/QUESTIONS-FOR-KORT.md`).

Not changed: gear prices, gear percentages, level bonuses (`levels.l2.healthPct` and the others), `progression.commanderBounty`, every banner and the 15% banner rule. The standalone checks that pin the thresholds (`LevelMathCheck` `lm-thresholds` and `lm-carry`, `HousesCheck`) follow the sheet; the harness checks that need "just enough credits for level N" read `Spec.LEVELS` through `LevelChecks.L2` and `L3`.
