# Base merge dry run: report

Implementer: Sonnet 5.5 xhigh. Date: 2026-10-24. Worktree `C:\Users\kortt\Melty\bannerbound-merge`, branch `merge-prep`, head `8266638`.

## 1. Status

**merge-prep is ready to fast-forward into `phase-1-solo-core` (merge base is 74c3a0d, so `--ff-only` works).** It builds, preflight is clean, every standalone and python check is green, the player-jar leak guard passes, and the client suites are green except for the reds in section 6. Nothing was pushed, `phase-1-solo-core` is still 74c3a0d, no source branch was touched or deleted, and none of the four controller-owned files was edited.

What is not clean, in order of weight:

1. **The War Table (UI branch) and Phase 2's House pages do not fit each other.** The War Table chapter and contract claims send no House, so the server refuses every shard reward ("Choose a House for tier 2's 10 shards"): 12 of the 30 chapter tiers, the tier 20 Rare and the daily contract's 3 shards cannot be claimed from the War Table. Recruit tabs, roll card and full-roster screens of Phase 2 are not reachable. This is the "Kort decides" item 1 from the earlier trial and it is still open (section 7).
2. **The class screen overflows with the UI font** (QUARTERMASTER draws over ALL TROOPS, "FORWARD SUPPL" is cut, the Quartermaster passive loses its last line). Visible in `p2-commander-classes.png`. Needs a layout pass, not done here.
3. **20 Phase 1 UI suite reds** (ui-hud fixture, ui-feed, ui-recruit, ui-pages banner, contracts and chapter): their fixtures and strings are Phase 1 content. UI plan Task 9 owns them.
4. Two real defects were found by the client suites and fixed in the merge (section 5): a client crash on the Founders chapter page and a missing HUD slot for the commander class ability.
5. `python tools/package.py` refuses without `--allow-stopgap`: 26 troops still draw a stopgap tier (0.5.0 waits for tier art). This is the release gate working as designed, not a merge fault.
6. The levels-on balance retake has not been run, as ordered. Its command is in section 10.

## 2. What was merged, in what order, and why

Start: `integration-0.5` (1d6cc75), the Opus trial integration (ui-plan 8fa52f8 + phase-2-houses 5270e86 + phase-3 e0da9cf, plus its fixes). Its 14 UI conflicts and rulings are in `.superpowers/sdd/2026-10-23-integration/report.md`; I reused all of that work instead of redoing the UI merge, then brought in only what moved after it.

| Order | Merge commit | Brings | Conflicts |
|---|---|---|---|
| 1 | e052a31 phase-2-houses 5bad9a3 | progression Tasks 1 to 11 (levels, gear, ranks, statuses, the gate, 5 and 30), Phase 2 Task 13, phase-2-models, the `progression-*` branches | 12 files |
| 2 | c93cfa4 texel3-trial c86c6db | weapon pipeline (phase-2-pipeline 605f0dc is inside it), texel density, face system, tier I to III weapon art, knight density-3 skin | 6 files |
| 3 | df24e82 phase-2-art c83a5e8 | the one commit texel3-trial lacked: sun staff I (`art/weapons/sun_staff.*`) | none |
| 4 | 3265e55 phase-3-built-world f172667 | landmarks and supply houses of all five maps, neutral flags, walkways, stuck-unit hop, supply gate routing, bounded wedge detour, screenshot-gate tools | 7 files |
| 5 | 95a92b5, 8266638 | fix commits (section 5) | |

Why this order: phase-2-houses first because every later line (renderer, Phase 3 AI) edits files it also changed (state packet, Campaign, CampScreen); texel3-trial second because its renderer conflicts are with progression Task 9 (tier looks), which arrived in step 1; phase-2-art third and alone because it is a pure art commit; Phase 3 last because it touches the fewest shared files and its generated `Spec.java` and `sheets/preflight.json` are regenerated anyway.

Not merged, on purpose:
- `texel3-art` (ccbc6fd, 4 commits). Its final face art, arming sword III and knight s3 are blob-identical to what texel3-trial carries (checked file by file); its `art/troops/knight.json` lacks `faces.density`; the rest of its tree diff is older-base noise. Safe to retire once Kort agrees.
- `preview-0.5`, `heraldic-units`, `media-shoot-harness`, `art-bakeoff-opus`: outside the brief.
- Contained already (verified with `merge-base --is-ancestor`): phase-2-houses, phase-2-pipeline, texel3-trial, phase-2-art, phase-2-models e4df826, phase-3-built-world, ui-plan, progression-abilities, progression-gate, progression-ranks, integration-0.5.

Constants checked against the Phase 3 branch: `Structures.PASS = "landmark-pass-3"`, `CampTerrain.REVISION = "terrain-6"`, `LayoutMarker.LAYOUT = "campaign-4"`. `mod_version` is 0.5.0 (from phase-2-houses Task 13).

## 3. Conflicts and how each was resolved

Each hunk was read on both sides; a helper (`work/merge/res.py`, git-ignored) keeps line endings. rerere is on repo-wide (shared `.git/config`), so the resolutions are recorded.

### Merge 1: phase-2-houses 5bad9a3 (12 files)
- `src/main/java/gg/gravesandglory/GravesAndGlory.java`, `CommandMode.java`, `GravesAndGloryClient.java`: **state_v9 group record** is `lastStand`, then the UI branch's `full` and `strength`, then progression's `level2`, `level3`, `armouryOpen`; the Armoury block follows the commander ability. Writer, reader and `CommandMode.Group` agree; the UI testAbility triple stays after the groups.
- `Campaign.java`: Phase 2's server notice expiry (`noticeAt`, `NOTICE_TICKS`, standing lines) together with the UI's `noticeSerials` and key-named welcome lines; the wait line is `WELCOME_WAIT`; `switchMap` clears progression and the feed.
- `CampaignClient.java`, `CampScreen.java`: War Table pages kept; p2's dismissal (`acceptNotice`, `dismissed`, `shownNotice`) and the levels rows of the battle report added (in DarkUi tokens).
- `test-tools/java/.../BotSimulation.java`: progression's `levels.csv` and `score300` beside the soak probe. The two `startedAt` fields collided (long vs String): the probe's was renamed `probeStartedAt`.
- `tools/release_config.py` (forbidden tuple), `test-tools/resources/fabric.mod.json`, `HOW-TO-EDIT.md`, `README.md`: unions.
- `sheets/preflight.json` (and `Spec.java`): regenerated with `python tools/preflight.py`, never hand-merged.

### Merge 2: texel3-trial c86c6db (6 files)
Both sides kept, because progression Task 9 (tier art, rank-keyed skins, weapon tiers) and the trial edited the same renderer lines.
- `TroopModel.java`: `Bone` gains `variants` beside `weaponTiers`; `prepare(look, folded, helmOff, variant)` uses the tier-aware stage and weapon-part lookups plus the face filter; the rest box skips non-zero variants.
- `TroopModels.java`: skin, trim and glow take the far flag (twin textures) and keep the armour-tier stopgap; `weapon(id, tier)` and `weapon(id)` both stay.
- `TroopRenderer.java`: `Draw` carries `weaponFar` and `weaponId`; the face variant is set before `prepare`.
- `TroopFeatures.java`: tiered weapon lookup with the cube and far draw path.
- `TroopModelLoader.java`: `checkFaces` beside the armour-tier wording and `tierPoints`.
- `tools/build_skins.py`: density, faces and beards beside tiers, `weaponTiers`, chevron and trim; the glow count divides by density squared and adds the brightest weapon tier; `clean_bone` emits `weaponTiers` and `variants`. A first draft had a 6-space indent error and dropped a docstring word; both fixed; `--verify` clean (118 outputs).
- Non-textual break: `RankChecks.java` built `TroopModels.Weapon` with the old constructor; it now passes `w.far()`.

### Merge 3: phase-2-art c83a5e8
No conflicts. `sun_staff` art regenerated through `python tools/build_skins.py` (model json and textures).

### Merge 4: phase-3-built-world f172667 (7 files)
- `Tactics.java` `walk`: the wedge detour (`onDetour`, a detour point is used as it is) and `Houses.fords` (wade) both apply: the river snap runs only when the unit is neither on a detour nor fording; the waypoint search the same.
- `Campaign.java` `switchMap`: p2's progression and feed clears plus p3's `Structures.clearQueue()` and `placing=false`.
- `GravesAndGlory.java` build tick: p3's landmark placing (one a tick, ready after the last) with the key-named "Press {key:menu}" line.
- `CampWorld.java` `spawn(Profile)`: the top-level `Profile`, p3's doc (first arrival looks west, yaw 90).
- `MapRouteChecks.java`: `Profile` and p3's houses constructor.
- `sheets/preflight.json`, `Spec.java`: regenerated (Preflight clean, 6636 cells).

### Auto-merged but worth knowing
`docs/PARKED.md` (+93 lines) and `docs/QUESTIONS-FOR-KORT.md` (+11) changed cleanly from the branches. Both are also edited, uncommitted, in the main checkout: that matters for landing (section 9).

## 4. Static verification (final tree, head 8266638)

| Command | Result |
|---|---|
| `python tools/preflight.py` | Preflight clean: 6636 filled cells |
| `python tools/build_skins.py --verify` | 118 outputs verified |
| `python tools/build_ui_art.py --verify` | 260 textures verified |
| `work/merge/standalone.sh` (javac + run of every Java check in `test-tools/standalone`) | 15 of 15 pass, 0 FAIL lines: AdmissionPolicy 746, BattleMath 64, Houses 293, LevelMath 141, ProfileMigration 31, CampRules 49, CampTerrain 31, CompatibilityPolicy, HouseRoute 30, StepHop 13, Wedge 42, LayoutMarker 26, LegacyDataMigration 13, TroopFaces 161717, UiMath 179 |
| python tests | test_build_skins 105 OK, test_bbmodel 15 OK, test_ui_standard clean; unittests modrinth_export 5, release_integrity 3, preflight_houses 6, preflight_rules 6, progression_gate 13, all OK |
| `gradle build --offline -x test` | BUILD SUCCESSFUL |
| `gradle compileJava compileLocalTestTools --offline` | clean |
| `python tools/package.py` | refused: 26 troops still draw a stopgap tier (no model, or armour/weapon tier draws a lower one) |
| `python tools/package.py --allow-stopgap` | OK, `validate_player_jar` passes, zip `dist/graves-and-glory-0.5.0-stopgap.zip` (40,252,315 bytes); `tools/verify_package.py` reads it back OK; the stale `dependencies/manifest.json` problem is gone |
| `graphify update .` (once, at the end) | OK: 7324 nodes, 23816 edges, 241 communities (`graphify-out` is git-excluded) |

The first standalone run after the merges had three reds, all fixed in 95a92b5: `test_release_integrity` (22 harness classes the branches added were missing from the forbidden list; 11 names added), and `test_preflight_rules` and `test_preflight_houses` (their scratch-copy fixtures lacked files the newer preflight rules read).

## 5. Fixes made on top of the merges (commit 8266638, by path)

1. **Crash, `WarTablePages` Founders chapter.** The page assumed a title on every 5th tier (`Progression.TITLES[t / 5 - 1]`). Phase 2's chapter has five titles at `TITLE_TIERS` 5, 10, 15, 25, 30 (Vanguard dropped), so tier 30 read `TITLES[5]` and the client died with `ArrayIndexOutOfBoundsException` (`WarTablePages.hero:1062`, found because the ui-pages suite crashed there at tier 30; tiers 20 and 25 also named the wrong title). New `titleAt` and `titleFrom` follow `TITLE_TIERS`. Not fixed, because it is design: the page still labels non-title tiers "Gold / 25" where Phase 2 pays 30 gold, 10 shards, a Rare or a cosmetic (`Progression.reward(tier)` has the right words), and its Claim sends no House.
2. **HUD slot for the commander class ability.** The UI branch's `BattleHud.ability` reads only the test-ability triple of the state packet, which is "none" in play; Phase 2's own tile (`abilityDrawn`) vanished with the UI merge. So the H ability had no HUD feedback. `GravesAndGloryClient.classAbility()` now feeds the slot from the packet's class active: its seconds while it runs, then its cooldown; an active this build lacks (Quartermaster's Forward Supply, ready -1) leaves the slot empty. The UI plan already says "every ability shows the horn until Phase 2 gives each its picture". Seen working in `p2-commander-tile-active.png` (key H, horn, "8"). Per-class pictures are not drawn.
3. **Checks that pinned the pre-merge state**, adapted: `Phase1Checks` atlas check skips `house/` glyphs (as `tools/build_ui_art.py` `FOREIGN` does); `Phase2Checks` spike-loader fixtures say format 3 (`TroopModelLoader.FORMAT` became 3 with the weapon pipeline: this red also exists on texel3-trial and phase-2-pipeline, it was just not run there); `slotTile()` reads the slot the HUD draws for p2-ability-key, -ability-cycle, -champion-standfast, -marshal-muster; p2-hud-hearts reads the Ledger column against vanilla's hearts (p2's panels and `CommandMode.groupsRight` are gone); the unbound-key hint is "Not Bound" (Minecraft's own wording); `RankChecks` expects the tier art that now exists (arming_sword_t2 and _t3, sun_staff_t3) instead of the stopgap.

## 6. Client results

Harness: one isolated root per launch under `work/merge/r-<tag>-<time>/` (git-ignored; evidence in `evidence/local-checks.txt`, log in `client.log`), at most 30 minutes per launch, at most 4 harness clients across worktrees, gate polled each minute. Kort's client was never up during any launch. Summary file: `work/merge/results.txt`. Two clients (campaign, selftest) do not close themselves after their last check (the Phase 3 report notes the same): I stopped each by hand after its last check had passed.

Final state per suite (the last run of each, after the fixes where fixes applied):

| Family | Suites | Result | Baseline / note |
|---|---|---|---|
| Phase 1 core | combat, morale, reinforce, bots, tutorial, report | 57 pass, 0 fail | Phase 3 Task 9: 56/0 |
| Phase 1 UI | ui-art, ui-home, ui-leave, ui-pages (friends, field, scales, cards, focus, held) | green | ui-plan own run: 81/0 |
| Phase 1 UI | ui-hud, ui-feed, ui-recruit, ui-pages banner, contracts, chapter | 20 red (section 6.1) | expected: Task 9 |
| Phase 2 houses | banners, chase, graveborn, ironclad, tidecallers, wildmoot | green | |
| Phase 2 houses | engine, commander, gilded (ability key, cycle, hearts, champion Stand Fast, marshal Muster) | green on two parallel clients after the fixes; 2 red (6.2) | |
| Phase 2 screens | screens, migration, spike, gilded-art | 36 pass, 6 red (4 checks + 2 suite roll-ups, 6.2) | spike loader fixed |
| Phase 2 render | anim, tells, depth | 38 / 0 | |
| Phase 2 t3, faces | t3 (needs `GRAVES_PHASE2_TURN=knight`), faces | t3 6/0, faces 8/0 | my first t3 run lacked that variable: p2-t3-farskin red, my invocation error, not a merge fault |
| Phase 2 turn, hands | turn, hands | turn pass; hands 1 of 198 shots red in 3 of 4 runs (6.3) | known platform-light flake, higher rate here |
| Progression | levels, gear, statuses; levels-gilded, -graveborn, -wildmoot, -ironclad, -tidecallers; ranks | levels 105 pass with the rank reds since fixed; five House suites 73/0; ranks 20/0 after the fix | `armoury` suite is a skeleton (`NotBuilt`: "Task 8: the Armoury tab"): red by design |
| Phase 3 | pipeline, studio, camp, houses, yard | 135 / 0 | baseline 135/0, identical |
| Phase 3 | landmarks + supply, maps 0 to 4 (one launch each) | 149, 202, 156, 164, 139, all 0 fail | baseline landmarks 115/168/122/127/117 + supply 37/37/37/?/25, minus the 3 header checks each: matches exactly (map 3 supply 40) |
| Phase 3 | routes | 49 / 0 | baseline 49/0 |
| Campaign (old suite, `-PcampaignChecks -PautoHost`) | | 39 / 0 | baseline 39/0 |
| Self test (`-PselfTest`) | | 11 / 0 | |

### 6.1 The 20 Phase 1 UI reds (all merge-induced, all fixtures and strings of Phase 1 content)
- `p1-ui-hud-fixture`, `-hud-suite`, `-alt-layer` (3): the fixture expects Phase 1's 12/6/2 army (9 infantry, 6 ranged, 1 support at the checkpoint); Phase 2's starter army is 12/8/0 (support group empty), so the plates and the hint-layer chips differ. Same finding as the trial.
- `p1-ui-feed-point`, `-loss-kill`, `-rout`, `-deploy`, `-feed-suite` (5): feed words are Phase 1's ("Dawn took Mill Hamlet", "3 Dawn Knights lost", "Dusk Revenants"); the merged game prints "Gold took Mill Hamlet", "3 Order Squires lost", "2 Risen lost"; the rout and deploy lines depend on the same fixture.
- `p1-ui-recruit-*` (8): the Phase 1 recruit letter flow (Enlist, Choose, Dusk banner, broke) against Phase 2's roll rules ("roll:gilded", "Epic within 39 letters", House locks); `-exception` is a null rect in the test itself (NullPointerException at `Phase1Checks.java:2960`, a rect the page no longer draws), not game code.
- `p1-ui-page-banner`, `-contracts`, `-chapter`, `-pages-suite` (4): Phase 1 numbers ("+25 gold", claims without a House, the hero tier) against Phase 2's chapter and contract rules ("+30 gold", shards need a House).
UI plan Task 9 (Phase 2 UI) is the planned owner. I did not rewrite them.

### 6.2 Remaining Phase 2 reds (merge-induced, design)
- `p2-recruit-tabs`, `p2-full-roster`, `p2-roll-card`, `p2-notice-clears`, `p2-claim-house` (+ `p2-engine-suite`, `p2-screens-suite`, `p2-migration-suite` roll-ups): they drive Phase 2's camp recruit page (five House tabs, ROLL button, per-House Claim). The War Table replaced it: no tabs, no roll button, War Table claims send `progress:daily:2` with no House (server stored nothing, shards 0 to 0). Same four the trial listed, plus `p2-recruit-tabs` and `p2-full-roster`, which share the cause.
- `p2-class-picker` (+ `p2-commander-suite` roll-up): "2 lines cut". Not a test fault: `p2-commander-classes.png` shows QUARTERMASTER over ALL TROOPS, "FORWARD SUPPL", and the Quartermaster passive ending mid-sentence. Cause: the UI branch's wider title and body fonts in Phase 2's fixed-size class cards (`CampaignLobbyScreen.ClassCard`). Needs card sizing or a War Table class page.

### 6.3 `p2-hands-shots`
One shot of 198, the first of the suite (`hands-knight-t1-body-0`), is "taken anyway after 300 ticks (pose false)". The platform's sky light stayed 0, the harness's light recovery fired ("[p2-light] ... shot recovery 1"), and the shot was taken with the pose check unmet; the picture is lit and correct (checked). It failed in 3 of 4 runs on the merged tree (the 4th, with no light trouble, was 5/0), and passed 1 of 1 on texel3-trial c86c6db run the same way. This is the known "anim platform light" flake, hit more often here (the t3 suite also needed a recovery once). Likely cause, not proved: the first load of the Phase 3 map keeps the light engine busy. A cheap harness fix would be to reset `held` after a recovery.

Not run: `walkthrough` and `campwalk` (Phase 3 screenshot gates: Kort passed that gate on f172667, and the walkthrough takes a launch per map), the media and screenshot-shoot suites, the two-client network check.

## 7. What Kort or the controller must decide

1. **War Table against Phase 2's House pages.** Pick one owner for: shard claims (daily, weekly, chapter), the tier 20 Rare choice, House tabs, rolling, troop shop and star-up, the full-roster view. Today the War Table covers none of them and the Phase 2 pages are not reachable. Until then 12 chapter tiers plus contract shards are unclaimable in the merged game.
2. **Class screen.** Fix card sizes for the UI font, or move classes onto the War Table. Class icons exist only for the three Phase 1 classes (arcanist, marksman, vanguard); the six Phase 2 classes draw a name only (`WarTablePages.paintClass` skips a missing icon).
3. **Commander ability slot.** Accept the interim (horn for every class; Quartermaster's Forward Supply hides the slot as not built), or schedule per-class pictures. The Armoury tab (progression Task 8) is still a skeleton.
4. **Phase 1 UI suites.** Confirm Task 9 rewrites the 20 reds against Phase 2 content, rather than keeping them as Phase 1 pins.
5. **Chapter page labels**: use `Progression.reward(tier)` for plaque text ("Gold / 25" is wrong for 22 of 30 tiers).
6. **Release.** `mod_version` is already 0.5.0 on this branch; `package.py` refuses until the 26 stopgap troop tiers have art. Banner page is still a stopgap (one House, no wide-troop markers); Quartermaster opens the Banner page via the UI alias; the deploy feed has no "Forward Supply" wording (trial notes, not re-checked).
7. **texel3-art** may be retired (section 2). I deleted nothing.
8. **rerere** is enabled repo-wide (shared `.git/config`); say so if that is unwanted.

## 8. Docs the merge needs (controller-owned, not touched)
- `docs/HANDOFF.md`: current state (merge-prep head, the lines it contains, baselines of section 6), the open items of section 7, the balance retake status. It is unchanged by the merge.
- `docs/PARKED.md` and `docs/QUESTIONS-FOR-KORT.md`: the merge adds 93 and 11 lines from the branches; add the decisions of section 7 there (recommended option first).
- `CLAUDE.md`: no change needed.

## 9. How to land it (exact commands, in the main checkout)

The merge base is 74c3a0d, so a fast-forward works. The merge changes `docs/PARKED.md` and `docs/QUESTIONS-FOR-KORT.md`, which have uncommitted edits in the main checkout, so git will refuse until those two are out of the way. `docs/HANDOFF.md`, `CLAUDE.md` and the two plan files are not touched by the merge and may stay dirty.

```
cd C:\Users\kortt\Melty\bannerbound
git status --short
git stash push -m "controller docs before the base merge" -- docs/PARKED.md docs/QUESTIONS-FOR-KORT.md
git merge --ff-only merge-prep
git stash pop
```
If `git stash pop` reports a conflict in either file, keep both sides (the branches added parked items and questions; the controller's edit adds more) and run `git add` on the two files. Then:

```
python tools/preflight.py
gradle build --offline -x test        (invocation as in docs/HANDOFF.md)
git log --oneline -1                  (expect 8266638)
```
The untracked `docs/superpowers/plans/2026-10-23-in-match-progression.md` is not in merge-prep and does not block anything. To undo before anything else is committed: `git reset --hard 74c3a0d` is not needed, since the fast-forward only moves the branch pointer and `git branch -f phase-1-solo-core 74c3a0d` restores it. Do not remove the worktree `C:\Users\kortt\Melty\bannerbound-merge` until the evidence under its `work\merge\` is no longer needed (git-ignored, not in the branch).

## 10. Balance retake (levels-on only, not started)

Kort's ruling (2026-10-24, in HANDOFF): at the base merge run the levels-on 5/30 round robin only. The merged tree has all three AI changes the baseline needs (stuck-unit hop 155f2cf, supply-house gate routing, bounded wedge detour f54c49c/f172667) and `build.gradle` has the `-PsimProgression`, `-PsimFast`, `-PsimOverride` flags. Method as in `docs/PHASE-2-BALANCE.md` (progression gate section): 20 ordered pairs of the five `<house>_normal` banners (houses in alphabetical order gilded, graveborn, ironclad, tidecallers, wildmoot; seed 9000 + k, k = 1 to 20 in that order), 10 matches per pair, maps 0 to 4 twice, 900 s rounds, fast ticks. One job, from the merged checkout:

```
SIM_LEVEL_SEED=9001 bash tools/balance_sim.sh r30m-on-gilded-graveborn 25741 -PbotSim=10 -PsimDawn=normal -PsimDusk=normal -PsimMaps=0,1,2,3,4 -PsimRoundSeconds=900 -PsimFast=true -PsimProgression=on -PsimBanner0=gilded_normal -PsimBanner1=graveborn_normal
```
The other 19 jobs differ only in the name, the seed (9002 gilded-ironclad, 9003 gilded-tidecallers, 9004 gilded-wildmoot, 9005 graveborn-gilded, and so on in that order) and the two banners. Each job makes `work/method/<name>-<HHMMSS>`. The earlier run used a queue runner that is not committed: `C:\Users\kortt\Melty\bannerbound-gate-prog\work\gate\runq.py` (copy it with `collect.py` to `work/gate/` of the merged checkout; job file lines are `name|seed=N -PbotSim=10 ...`; `python work/gate/runq.py JOBFILE 25741,25742 5400 retake` ran two jobs at a time, about 1.7 h for the 20). The earlier job file is `bannerbound-gate-prog\work\gate\r30-jobs.txt`. Analysis, against the old levels-off baseline roots `bannerbound-gate-prog\work\method\rr-off-*` (cdea2c4, before the AI changes: that caveat stays):

```
python tools/progression_gate.py --off <the 20 rr-off-* roots> --on <the 20 new roots>
```
It prints `gate progression shares=... comeback=... -> pass|fail` and exits 0 on pass. Kort's earlier result to compare against: largest share move 4.9 points (Gilded -4.9, Graveborn +4.9), comeback +3.3.

## 11. Where the evidence is
`C:\Users\kortt\Melty\bannerbound-merge\work\merge\`: `results.txt` (one line per launch), `r-<tag>-<time>\evidence\local-checks.txt` and `client.log` per launch, `standalone-final.txt`, `build-final.out`, `package-strict.out`, `package-allow.out`, `graphify.out`, the commit messages, and the helper scripts (`run.sh`, `standalone.sh`, `compile.sh`, `apply-fixes.py`, lane scripts). Screenshots worth a look: `r-p2houses2-184654\run\screenshots\p2-commander-classes.png` (class overflow) and `r-p2abila-191420\run\screenshots\p2-commander-tile-active.png` (ability slot).
