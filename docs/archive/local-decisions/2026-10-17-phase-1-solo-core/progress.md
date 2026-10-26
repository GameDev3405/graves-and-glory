# SDD ledger — plan: docs/superpowers/plans/2026-10-17-phase-1-solo-core.md
Spec: docs/VISION-AND-ROADMAP.md (binding). Branch: phase-1-solo-core (from master after Phase 0 commits).

## Preflight scan (2026-10-17)
| Pair / task | Produces vs consumes | Finding |
|---|---|---|
| T1/T2 | T1 creates BattleMath + Battle.tick hook for Projectiles; T2 adds BattleMath methods + Morale.tick in Battle.tick; T2 bumps state_v5 | consistent; both hooks in the running/after-intro block |
| T1/T7 | T1 Unit.lastHitBy; T7 p1-report-late-kill consumes | consistent |
| T2/T4 | Morale overrides Battle.assignment without touching s.assignments; T4 Hard bots write s.assignments shieldwall | no clash: rout wins while routing, bot order applies on regroup |
| T3/T6 | T3 deployPoint; T6 lesson 4 copy | consistent |
| T4/T5 | Campaign.difficulty, Squad.tier set in prepareRound; sim overrides team-1 tier after startRound | consistent |
| T4/T6 | T6 campaign row difficulty "easy" needs tier(); order T4 before T6 | ok |
| T5/T7 | MatchStats.SquadStats goldEarned (T5) then kills/lost/objectiveSeconds (T7) | consistent |
| T7/T8 | T7 results table drawn before DarkUi.teamMark exists; T8 sweeps it | ok, T8 brief carries it |
| T1 self | tests vs code: travelTicks/tookLoss values agree; p1-impact-team-colour covers melee via strike | ok |
| T2 self | morale sheet columns match Rules; checks reference 12 knights (default lineup 12/6/2) | ok |
| T9 self | melty.json alignment "if exists": Phase 0 removed the check; harmless | ok |
| Global | "commit on feature branch, never master": Kort allowed master commits, but plan isolates | Ruling: use branch phase-1-solo-core — keeps master = last verified 0.3.7 — cost if wrong: one merge later |

Ruling: in-game suites run one at a time, in the bannerbound folder on the branch; test roots under work/p1/. — avoids two clients on one GPU — cost if wrong: slower.
Ruling: implementer models: T1,T2 Opus medium; T3-T9 Sonnet xhigh; reviewers Sonnet xhigh (T1,T2 Opus medium); final review Opus medium.
Update (Kort, evening): Opus and Sonnet only (no Haiku), both at max effort from Task 5 on; Fable subagents at high. — per Kort's tiering — cost if wrong: a fix round.

Note for Task 6: 0.3.7 verification screenshot camp-battle-hud.png shows lesson text overflowing the panel ("...to send th"); treat as a genuine defect in Step 1.

## Phase 0 close-out (2026-10-17)
- Harness fix landed on master as e78b81b (self-test 8/8 pass, routes maps 0-3 20/20). Branch phase-1-solo-core created from e78b81b.
- Known game bug found by the harness agent: switching the lobby to Kingsfield (map 4) crashes the client in GravesAndGloryClient.map (minimap indexes 5 points while contested/progress arrays still hold 3). Logged in docs/QUESTIONS-FOR-KORT.md; Task 9 (regression) must fix it or earlier if a task touches that code. Ruling: fix in Task 1 only if trivially adjacent; otherwise Task 9 — keeps tasks scoped — cost if wrong: one extra fix commit.
- Pre-existing suite failures untouched: solo-pad-3v3, roadmap-suite.

## Task 1
BASE=c8c9189 (controller docs commits; implementer dispatched a7b70d7, Opus medium)
Note: Phase 2 plan committed on this branch as 8d66654 (docs only, 13 tasks). Task 1 BASE stays c8c9189; review package will include 8d66654 docs diff — harmless.
Task 1 implementer DONE_WITH_CONCERNS: commit 19880d2; BattleMathCheck 5/5; p1 combat suite pass; screenshots verified. Report task-1-report.md.
Ruling: tookLoss threshold 0.01 cannot flash a single 4-damage arrow on a full 12-knight group (0.0093). Lower threshold to 0.008 and restore the brief's 4-damage fixture; the "noise" check (0.005 drop) must still not flash — single hits must read on the HUD (spec: readable drama) — cost if wrong: rare extra flashes.
Ruling: isolated test roots go under <repo>/work/p1/ (git-ignored), not the scratchpad — Windows CreateProcess rejects the long scratchpad path — cost if wrong: none (work/ is ignored).
Review package: review-c8c9189..19880d2.diff (includes 8d66654 docs commit). Reviewer: Opus medium.
Task 1 review (ac486eb): Needs fixes. Important: ruling-1 threshold not applied. Minor: spectator-target chase, bolt aimed at enemy not spellCenter and cap no-op, lastHitBy never cleared, static C cache, no small-drop check.
Ruling: fix round 1 = Important 1 + minors 1,2,5. Bolt: aim at spellCenter and land at u.windupUntil (lands = max(fired+minTicks, windupUntil)) so bolt and burst coincide — plan intent — cost if wrong: a visual nit. Minors 3,4,6 parked (harmless).
Fix round 1: resume implementer a7b70d7.
Re-review round 1 (a8027a1, Sonnet): all 4 findings fixed, no new issues. Out-of-scope: bolt keeps flying if caster dies/spell cancelled (pre-existing); boltSpeed now only used by mend.
Task 1: complete — commits 19880d2, 34b73ef.

## Task 2
BASE=34b73ef
Implementer aadbdea (Opus medium) dispatched.
Task 2 implementer DONE_WITH_CONCERNS: commit 1e82c5b; BattleMathCheck 12/12; morale suite 7/7 + suite pass; screenshots checked.
Ruling: BattleEffects.horn(Battle,Squad,Vec3d,String) with per-squad-per-kind cooldown accepted over the brief's 3-arg form — needs squad for cooldown and team for pitch; rout horn must not silence the rally — cost if wrong: Phase 2 Marshal call site uses 4 args.
Ruling: test roots seed run/options.txt with pauseOnLostFocus:false and onboardAccessibility:false (added to global-constraints) — client otherwise idles on the title screen — cost: none.
Review package review-34b73ef..1e82c5b.diff; reviewer Opus medium.
Task 2 review (a9024de, Opus): Approved, 0 Important, 9 Minor. Ruling: carry minor 1 (trained troops outside fullHealth -> group never routs) into Task 3 since it touches the deploy path; minor 2 (weak group re-routs each immunity) to Task 5 balance data; minors 3-9 parked.
Task 2: complete — commit 1e82c5b.

## Task 3
BASE=1e82c5b
Implementer a74567a (Sonnet xhigh) dispatched; carries Task 2 minor 1 (fullHealth for trained troops).
Task 3 implementer DONE_WITH_CONCERNS: commit 3e1c540; BattleMathCheck 15/15; reinforce suite 9/9; walk 83.8s -> 13.4s.
Ruling: Morale.fielded = max(old, living group health) accepted over "add trained troop health" — a plain add inflates the ceiling when a casualty is replaced — cost if wrong: none observed.
Ruling: bots' reinforcements also deploy forward (same trainingTick path) — symmetric rules, measured by Task 5 bot sim — cost if wrong: a bot tier tweak.
Review package review-1e82c5b..3e1c540.diff; reviewer Sonnet xhigh.
Task 3 review (ac33de9, Sonnet): Approved, 0 Important, 6 Minor. Carry to Task 9: minor 3 (safe-radius check at actual spawn spot, not anchor centre); minor 1 (sharpen p1-trained-fullhealth with a 2-lose-2-train-1 case). Minor 5 lesson width -> Task 6 (already noted). Others parked.
Task 3: complete — commit 3e1c540.

## Task 4
BASE=3e1c540
Implementer ad646d2 (Sonnet xhigh) dispatched.
Side track (Kort, 2026-10-17 evening): HUD/UI concept designer a1ea645 (Fable high) producing mockups under work/ui-concepts/ (read-only on src). Kort wants UI/HUD pulled forward; Task 8 scope will be replaced by a UI plan once he picks a direction. Kort's Houses answers committed 5170127.
Task 4 implementer DONE_WITH_CONCERNS: commit ebd31d8; BattleMathCheck 20/20; bots suite 6/6; old campaign suite 35 pass (solo-pad-3v3, roadmap-suite pre-existing fails).
Ruling: shieldwall formation now raises shields whenever the trooper has a target (not only within 6 blocks), for players too — a shieldwall should read as shields up (readable drama) — cost if wrong: slightly slower shieldwall advance; Task 5 sim will show it.
Note: Hard bots rely on Battle.updateSight(); the Task 5 headless sim must keep it ticking. Commit trailer on ebd31d8 says Sonnet 5.5; left as is.
Review package review-3e1c540..ebd31d8.diff (includes 5170127 docs commit); reviewer Sonnet max.
Task 4 review (a954a59, Sonnet max): Needs fixes. Important: s.yaw moved into non-retreat branch (Normal not unchanged). Minor: dead sightings counted in defenders/nearest, near-radius 2*chargeDistance unnamed, test gaps (28-block charge case, timedCharge false case), Hard semantics for Task 5/7, report nit.
Ruling: fix round 1 = Important 1 + minors 1, 2 and the two named test gaps; minors 4-6 parked (Hard convergence to be judged with Task 5 sim data).
Fix round 1 commit e3a56da (yaw restored, live sightings, NEAR_RADIUS_FACTOR, 2 checks + p1-bots-retreat-facing; bots suite 7/7). Re-review: Sonnet max.
Re-review round 1 (a92036c, Sonnet max): all 4 fixed, none new. Note for Task 9: re-run old -PcampaignChecks suite before merge.
Task 4: complete — commits ebd31d8, e3a56da.

## Task 5
BASE=e3a56da
Implementer aa88691 (Opus max) dispatched; balance runs are long (~2 h); servers only, no client.
Side fix: Kingsfield minimap/strip crash guarded in GravesAndGloryClient (6ecbfb0, docs 5377313; Sonnet max; compile ok; in-game verify in Task 9). Task 5 BASE stays e3a56da; its review package will include these two commits (skip in review).
Side: 0.3.7 mrpack rebuilt from master e78b81b in bannerbound-runs (Melty-free, verified) and copied to dist/. Phase 3 built-world spec+plan being written (ae54a0f, Opus max, docs only).
Task 5 implementer DONE_WITH_CONCERNS: commit 0150a0d; BattleMathCheck 23/23; compile ok; smoke 2 matches; balance 40 matches. Acceptance: Normal>Easy 9/10 pass; Hard 10/20 = 50% MISS; Normal v Normal Dawn 8/10 MISS (seat bias: Dawn 30/32 on maps 0-3, Dusk 5/8 on Kingsfield). No tuning done. Full report transcribed to task-5-report.md (subagent could not write files).
Ruling: harness code is reviewed now as Task 5; the two acceptance misses are not the harness's fault and become Task 5b (bot balance) after review — garrison-keeping flank picker for Hard, seat-bias diagnosis (spawn-chunk/world-spawn candidate first, then spawn-row centring, then faction kit), rerun hard-normal, normal-hard, normal-normal and easy-normal — cost if wrong: one more ~1 h sim batch.
Ruling: commit trailers naming the working model stay as they are — cost: none.
Review package review-0ac77b9..0150a0d.diff (0150a0d only; 6ecbfb0/5377313/f329a7c/0ac77b9 are controller docs/side-fix commits, not in the package); reviewer Opus max.
Task 5 review (ac4f01a, Opus max): Needs fixes. Important: botSim gradle block does not pin -Dgravesandglory.localChecks, so a sim server can append false passes to a real Melty checks file. Minors: no ready watchdog; blank/non-numeric -PbotSim; lastWinner stale if returnLobby called outside finish; simMaps default hard-coded; APPEND without CREATE; 136 WARN lines in sim logs (startup lag, DUMMY block entities: pre-existing); earned-gold paired adds twice; global-constraints standalone command missing MatchStats (fixed by controller).
Ruling: fix round 1 = Important 1 + minors 1-5 and 7 (all small); minor 6 noted for Task 5b; minor 8 done in global-constraints.md — cost: one short implementer round.
Fix round 1 commit 10f3793 (all 7 items; aborted winner; createDirectories; failed CSV write logged). Re-review: Sonnet max.
Re-review round 1 (a9f9194, Sonnet max): all 7 fixed, no Important new. Minors parked: recreated CSV lacks header; write failures not counted; dawnWin% divides by all matches incl. aborted; lastWinner sentinel -2 documented (future readers test <0).
Task 5: complete — commits 0150a0d, 10f3793.

## Task 5b (controller-added: bot balance)
BASE=10f3793
Brief task-5b-brief.md. Implementer Opus max dispatched (sims up to 4 h).
Side: Phase 3 built-world spec + 9-task plan committed b9aadfc (planner ae54a0f, Opus max); six questions logged, all recommended yes; plan's model rule corrected to Opus/Sonnet max only.
Side: UI v4.1 mockups done (designer a7810ef), graded 9.5 x3; UI spec+plan being written (a492605, Opus max); title mockups v1 graded 8.5/9, detail round running (aa7a3c0).
Task 5b implementer DONE_WITH_CONCERNS: commit c53d843; BattleMathCheck 26/26; bots suite pass. Targets still missed: normal-v-normal Dawn 70%, Hard 45%, Normal over Easy 55%. Root cause found: knights' shields vs shieldless revenants (equal kits -> 9/20), plus husk 2 natural armour vs skeleton 0, spawn-row head start (fixed), Kingsfield lane rule not mirrored.
Ruling: the revenant shield is REVERTED (the approved unit identity spec says Graveborn carry no shields; a shield shows on cards and in play). Replacement: Dusk sheet health +10% (revenant 33, bonebow 22, hexer 20) to offset husk natural armour, as the agent's experiment measured 50%; plus mirror Normal's lane rule for team 1 on Kingsfield. This is Task 5c on the same implementer; review covers 5b+5c as one diff — cost if wrong: one more ~130 min sim wave.
Ruling: garrison rule's deviation (an army already on an owned point stays as its guard) accepted — it is what the brief meant.
Ruling: Hard flankLeastDefended stays on until Kort answers the logged question.
Side: title "The Hill" v2 delivered (aa7a3c0); controller grades day 9 / night 9.5; sent to Kort. No UI code until Kort grades v4.1 and the title.
Task 5c implementer DONE_WITH_CONCERNS: commit 584480b (shield reverted, Dusk health +10%, laneTarget mirror incl. Dusk spawn rows, BattleMathCheck 27/27, bots suite pass). Normal-v-Normal gate FAILED: Dawn 10/10. Melee duel ratio knights:revenants 3.3:1, same as the original kit; the +10% health shows nothing. wt-5b-armor removed.
Ruling (5d): the gap is a mechanic, not a number. Tactics raises the shield in block formation whenever the enemy is within 6 blocks and the knight keeps swinging while blocking, which vanilla never allows a player; so the knight gets a free 100% frontal block in every melee. Fix: in block formation the shield comes up only while the enemy is outside sword reach (approach under fire) and drops while the knight is in reach and swinging; shieldwall keeps the full brace (its counters are Shieldbreakers and blight). Plus the unit identity spec's own rule: blight ignores shields (spec line 140) — the burst uses a damage source a raised shield does not block. Health +10% stays for now as the husk natural-armour offset; revert it if Dawn drops under 35%. Spawn-row mirror deviation accepted (equal walking distance is the point). Same implementer, gate normal-v-normal first, then the three pairings — cost if wrong: one more ~2 h sim wave.
Minors carried to review: Phase1Checks.laneRule needs a Dusk fixture; lesson banner text clipped at the right edge (UI plan territory, PARKED); Phase 2 ArmyZombie must drop the +10% when the Risen becomes a zombie (2 natural armour).
Side: UI spec + 8-task plan delivered (a492605, Opus max; report ui-plan-report.md). Models correct (Opus/Sonnet max). Five questions logged in QUESTIONS-FOR-KORT.md with recommendations. Task 8 brief marked superseded; Task 9 brief drops the `ui` suite. Plan starts after Kort's grades and Phase 1 Tasks 6/7.
Task 5d dispatched (same implementer a65b27d): shield down while swinging in block, blight ignores shields, Dusk laneRule fixture; gate then pairings.
Task 5d implementer DONE_WITH_CONCERNS: commit 6523eab (raiseShield in BattleMath, blight indirectMagic ignores shields and armour, Dusk laneRule fixture, p1-bots-blight stage; BattleMathCheck 32/32; bots suite pass twice). Gate: 20% Dawn with the +10% Dusk health, 70% with it reverted (committed). Duel ratio 3.3 -> 1.8. Pairings not run.
Ruling (5e): the even point sits between the two settings, so take half the offset (revenant 32, bone ranger 21, gravecaller 19) and gate on 20 rounds, not 10 (one standard error at 10 rounds is 15 points). If 35-65%, run the four pairings. Same implementer — cost if wrong: one more sim wave; if 20 rounds still miss, the remaining lever is a Hard/Normal design call for Kort, not more tuning.
Ruling (5e amendment, 05:30): gate at 8/10 Dawn mid-run; pairings run regardless of the gate, since the tier targets are what players feel and are unmeasured since 5d. No more kit changes this wave; if the gate misses, the seat bias goes to Kort as a design call (first clash decides, offset flips it from 70% to 20%).
Task 5e implementer: sheet commit 50121f5; 20-round gate Dawn 15/20 (75%, miss); Hard 6/20 (30%) vs Normal (as Dawn 4/10, as Dusk 2/10); Easy pairings running. Reports 5c/5d transcribed; review package review-10f3793..50121f5.diff (12 commits); reviewer Opus max dispatched for 5b-5e as one diff while the Easy sims finish.
Task 5b-5e review (a5a9f1a, Opus max): Needs fixes. Important: the garrison rule that runs (garrisonPoint's guard pre-pass in Campaign) is not the tested one; BattleMath.garrison's guarded test is dead on the only call path and check (a) encodes the literal brief. Minors: laneRule hand copy redundant (keep absolute expectations); missing edge cases (garrison tie, 6-block limit, lane>=points); spawn rows untested; shield decided on the 10-tick beat (up to 9 ticks of free block after the enemy enters reach); armour bypass note for Phase 2; human armies never count as guards; spec tell drift; stale numbers in QUESTIONS.
Ruling: fix round 1 = Important 1 + minors 1, 2, 4, 6 (code); minors 5, 7, 8 done by the controller in docs/PARKED, the unit identity spec and QUESTIONS; minor 3 parked. Minor 4 is a correctness fix of the 5d ruling (the shield must drop the tick the enemy enters reach), so the 5e numbers are pre-fix; no rerun of the pairings in this round. Starts after the Easy sims end (shared build dir).
Task 5e implementer DONE_WITH_CONCERNS (final): all three tier targets missed on 20-round pairings at 50121f5: NvN Dawn 15/20 (75%); Hard 7/20 vs Normal (5 as Dawn, 2 as Dusk; 3/16 on three-point maps, 4/4 Kingsfield; one degenerate round 153-0 where Hard as Dusk never fought); Normal 6/20 vs Easy (Easy 13/20). The tier ladder is upside down and every knob moves monotonically Easy->Hard. Dawn 42/60 overall; deaths even in mirrors, so Dawn wins on points.
Ruling: balance tuning stops here for Phase 1 code; the seat edge and the tier ladder go to Kort as problem 5 with data. Two sheet-only single-knob diagnostics run in parallel from the bannerbound-runs worktree (Task 5f, Sonnet max, brief task-5f-diag-brief.md): Hard with flankLeastDefended=false, and Normal-with-Easy's-chargeRatio; retreat/rally third if time. They do not block Phase 1 Tasks 6-9 — cost if wrong: ~2 h of wall clock, few tokens.
Fix round 1 for the 5b-5e review dispatched to a65b27d (Important 1 + minors 1, 2, 4, 6); review file task-5b-5e-review.md. Then scoped re-review (Sonnet max), then Task 6.
Fix round 1 commit 186bdaa (guard pass inside BattleMath.garrison, check (a) {0,2,1}; laneRule copy dropped; tie/6-block/lane-wrap checks; Tactics.brace decides the shield every off-beat tick; human armies guard but are never assigned). BattleMathCheck 37/37; bots suite 12/12 (work/p1/bots-r1-080723). Re-review: Sonnet max on d4a8f65..186bdaa.
Re-review round 1 (a0e7f3a, Sonnet max): all five addressed, no new Important; six mutations each tripped its check. Gaps parked: no suite asserts garrisonPoint with an owned point plus a seated human army; no in-game timing of the per-tick shield drop; balance numbers in QUESTIONS 4/5 were measured with the beat-stale shield.
Task 5b/5c/5d/5e: complete — commits c53d843, 584480b, 6523eab, 50121f5, 186bdaa. Balance targets open (QUESTIONS problem 5); diagnostics Task 5f running in the runs worktree.

## Task 6
BASE=186bdaa
Brief task-6-brief.md. Implementer Opus max.
Task 6 implementer DONE_WITH_CONCERNS: commits 8d304c4 (F-17 lesson clipping fix, red/green roots) and 0882526 (reframe). Suite tutorial 6/6 in work/p1/tutorial-090732; controller viewed briefing and lesson-1 screenshots: match the report. Concerns noted: camp home copy still mentions practice lessons (Progression outside brief); state channel now v6 (later plans use v7); campaign battles reuse practice rules and size works only as 1; preflight does not validate campaignBattles refs; vanilla advancement chat over the panels (pre-existing, hunger/UI plan territory). Review: Sonnet max on 186bdaa..0882526.
Task 5f experiment A: Hard with flankLeastDefended=false won 9/20 vs Normal (7 as Dawn, 2 as Dusk), up from 7/20. Experiment B (Normal with Easy's chargeRatio) running.
Task 6 review (a71e472, Sonnet max): Approved, Minor only; six named risks all checked. Controller: plans renumbered (UI v7, Phase 2 v8, lesson card wraps) in 4d245a1. Ruling: one polish fix round on the same implementer for minors 2-7 (cheap, keeps the camp copy honest and the pending start guarded), then a scoped re-review; font-licence ERROR lines carried to Task 9 — cost: ~20 min.
Task 6 fix round 1 commit 7c42258 (soloStart/soloRefusal shared; ready guard; bad-map notice; PARKED lines; camp copy and Practice/skirmish label; use-key assert; copy nits). Suite tutorial 6/6 (work/p1/tutorial-093258). Re-review: Sonnet max on 4d245a1..7c42258.
Re-review round 1 (ac3057f, Sonnet max): all six addressed, no new Important. Notes: practice is refused for the whole map-build window (better than the old unbuilt-map start); the ready guard is redundant but harmless; Campaign.java:40 comment omits custom lobbies (cosmetic).
Task 6: complete — commits 8d304c4, 0882526, 7c42258.

## Task 7
BASE=7c42258
Brief task-7-brief.md. Implementer Opus max. Resolution: the brief's "expect 21 checks passed" is stale; BattleMathCheck is at 37, so expect 38.
- 2026-10-18 morning, Kort's decisions (docs/QUESTIONS-FOR-KORT.md, "Kort's answers 2026-10-18"): Hard flankLeastDefended -> false (sheets/design.json, regenerate Spec.java via tools/preflight.py), queued as its own small commit AFTER Task 7 lands so it does not collide with the implementer's working tree. Tier ladder parked until Phase 3 maps; diagnostics agent told to stop after its current batch. UI plan and Phase 3 recommendations approved. Home map and title mockups sent back to designers (work/ui-concepts, git-ignored).
- Task 7 implementer DONE_WITH_CONCERNS, commit 98ca38e (report task-7-report.md). Ruling: concern 1 (snapshot can exceed readString(16000) after ~140 eligible matches; pre-existing) goes to Task 9 as a required fix: compact JSON for the snapshot or matchGrants dropped from it, with a standalone size check — a crash that arrives after 140 matches is a Phase 1 bug, not a parked idea — cost if wrong: one small extra item in Task 9. Concern 2 (teamPointSeconds helper without its own standalone check) is for the reviewer. Review package review-7c42258..98ca38e.diff, reviewer Sonnet max.
- Flank flip committed e8da4e6; bots suite green at C:/Users/kortt/Melty/bannerbound/work/p1/bots-flankoff-101638.
- Task 7 review: Approved (task-7-review.md). Ruling: Important 1 stays a required Task 9 item, now with the reviewer's fix (drop matchGrants from both snapshot copies, keep a size check) — it is a 140-match crash, Phase 1 owns it — cost if wrong: Task 9 grows by one small change. Ruling: minor 3 (human taking over a bot inherits the army's tallies) is intended: the row is the army's, not the person's. Minor 5 accepted. Fix round 1 (resume implementer): minor 1 (no MVP on a 0/0 tie), minor 4 (tier-30 line fits at 760 wide), minor 2c (standalone case for teamPointSeconds guards, count 39).
- Fix round 1 commit ad7e4bc (39 checks). Ruling: a non-zero exact tie (same kills and same seconds) crowning each viewer's own row is accepted and parked — rare, cosmetic — cost if wrong: two players see different MVPs once in a long while. Scoped re-review e8da4e6..ad7e4bc, Sonnet max.
Task 7: complete — commits 98ca38e, ad7e4bc, d193f2e (re-review: all findings addressed; nit fixed by the orchestrator).
## Task 9
BASE=d193f2e
Brief task-9-brief.md. Implementer Opus max. Added by ruling: snapshot size fix (drop matchGrants from both snapshot copies), expected log noise recorded, Hard flanking off in the playtest copy.
- Diagnostics agent done (A 9/20, B 9/20, C 8/20; Dawn seat 15-17/20); recorded in QUESTIONS problem 5 follow-up and PARKED; commit d11d07e. Note: Task 9 implementer dispatched at BASE d193f2e; this docs commit sits between, harmless for its review range (use BASE d193f2e..HEAD, the docs diff is small).
- Task 9 implementer DONE_WITH_CONCERNS, commits fee70bf (snapshot guard) and 2017cbe (regression, 0.4.0, playtest doc); report task-9-report.md. Rulings: (1) Hard-without-flanking losing Kingsfield is stated in the playtest appendix; Kort's Skirmish 3 answer decides what Phase 3 tuning targets. (2) importProfile replacing the whole profile is harmless (import only when the server has no record); recorded, no change. (3) the two Task 3 minors carried to Task 9 (deploy safe-radius at the real spawn spot; sharper 2-lose-2-train-1 case) go into Task 9's fix round. (4) vanilla advancement lines in battle chat: fix round (announceAdvancements off on the battle world); lobby map button name clipping is replaced by the UI plan's field picker (Task 7), noted there. (5) sims predating per-tick shields: noted in QUESTIONS problem 5. Review package review-d193f2e..2017cbe.diff (includes the orchestrator's docs commits d11d07e, c8cf969, ffc924f), reviewer Opus max.
- Task 9 review: Needs fixes (task-9-review.md): Important 1 plus minors. Minor 11 fixed by the orchestrator (docs commit). Ruling: minor 4 accepted (unreachable fallbacks). Fix round 1 (resume implementer): Important 1; minors 1, 2, 3, 5, 6, 7, 8, 9, 10; plus the two Task 3 minors carried to Task 9 (deploy safe radius measured at the real spawn spot; sharper 2-lose-2-train-1 case) and announceAdvancements off on the battle world (advancement lines seen in battle chat). In-game re-runs limited to the suites the harness changes touch (combat, reinforce, report, tutorial), after the client is free.
- Fix round 1 commits 4073113, 48194af (37/37 focused re-run, package rebuilt). README map count fixed by the orchestrator (8a359fb). Scoped re-review d7bb870..48194af, Sonnet max. Note: title-demos harness files are uncommitted in this tree; decide (commit as harness or discard) when that tester reports, before the whole-branch review.
Task 9: complete — commits fee70bf, 2017cbe, 4073113, 48194af (+ orchestrator docs 8a359fb, 981c6d4); re-review: all findings addressed. Nit carried to the final review fix dispatch: Campaign.java:491 should call forClient(p.progression) instead of the literal "progression". dist/ was built from 48194af (README differs by the map-count fix only); rebuild after the final review.
Ruling: whole-branch review dispatched on e78b81b..981c6d4 while the title-demos tester's uncommitted harness files (TitleDemos.java, testing/mixin/, mixins json, fabric.mod.json, release_config.py) stay out of the range — they are harness-only, not part of the plan, and the demos agent still owns them; whichever way they resolve (commit as harness or discard) they will ride in the fix-dispatch re-review package. Cost if wrong: one small extra review of harness code.
Title demos: six in-game captures at work/ui-concepts/title/demos (DEMOS.md); harness committed as 83f987f (TitleDemos + two test-tools mixins, TitleDemo in the forbidden tuple). 83f987f lands after the final-review range and rides in the fix-dispatch re-review package. Decision board republished with the six for Kort's pick; UI plan Task 4 re-planned after the pick.
Final review (final-review.md): Ready with fixes — 0 Critical, 0 Important, 4 Minor. Ruling: the one fix dispatch takes all four minors (ENTITY_LOAD discard of Projectiles.TAG arrows; lastHitBy credit window; README 'Four purposeful battlefields' heading and missing Kingsfield bullet; playtest flank wording 'from behind, >110 degrees') plus the two queued nits (Campaign.java:491 literal; dist rebuild after). Plan-level note (routHomeRadius 10, MELEE_THREAT_RANGE 8, NEAR_RADIUS_FACTOR 2 live in code, not sheets) is parked to PARKED.md for the Phase 3 tuning pass. Every 'Declined to judge' line is a ruling already made or pre-existing behaviour; none reopens. Cost if wrong: one more minor fix round before 0.4.0.
Final fix dispatch: Sonnet max, BASE 4be5921, items: 4 minors + Campaign literal + PARKED note + dist rebuild; in-game combat/report suites required. UI Task 2 held until this client run ends (one client at a time).
Final fix round: b3fc295 (code) + 8cbe972 (docs); BattleMathCheck 41; in-game 56/56 across combat,morale,reinforce,bots,tutorial,report plus old campaign 39/39; dist 0.4.0 rebuilt (zip sha256 1e7bcd35…, mrpack 0d0db884…, jar 6d8d0ee9…). Ruling: finding 1's deviation accepted — ENTITY_LOAD fires for fresh spawns too, so the discard applies only to Projectiles.TAG arrows no queued Shot owns (Projectiles.owns), and fire() queues the Shot before spawning; a red run proved the literal fix deleted every arrow at birth. Cost if wrong: a stray arrow after a reload in the rare ownerless case. Ruling: 34-tick kill-credit window (projectileMaxTicks 14 + 20) accepted; the mod applies no damage over time. PROJECT-STATE count bumped by the orchestrator. Recommendations 3-5 of the final review stay recommendations (not findings).
Final re-review: all findings addressed, no new Critical/Important (two wording nits fixed by the orchestrator in b50dcb4 together with the shipped "four battlefields" strings the re-reviewer noticed out of scope). Phase 1 plan: COMPLETE at b50dcb4. 0.4.0 rebuilt from b50dcb4: zip 68,685,381 B sha256 7e2f34d4…, mrpack 22,103,722 B sha256 63b8e711…, jar sha256 bf919a43…; verify_package 77 entries, no harness names in the jar. Ruling: this workspace is kept (not deleted) until Kort's playtest verdict and the local merge to main, because a playtest fix round would reuse its briefs and reports. Cost if wrong: a stale scratch folder.
