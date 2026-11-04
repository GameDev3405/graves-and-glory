# Board: PLAN-TO-1.0

2026-11-03, checked against `main` at `667028f`. The single visible state for the 1.0 plan. One row per task in the active phases; later phases get one row until they start. Every row names an owner, a state and the evidence that settles it. If this board and another document disagree, fix both in the same commit.

States: **Done**, **In progress**, **Awaiting Kort** (work finished, visual or merge decision pending), **Awaiting merge** (finished on a branch, not in `main`), **Blocked** (a named task must land first), **Skipped**, **Not started**.

Owners are decisions, not file ownership. `Agent` means implementation; `Kort` means a decision, visual gate or scheduling.

## Phase 0: approve and freeze

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 0.1 | Approve the plan and the seven decisions | Kort | Done 2026-10-28 | `PLAN-TO-1.0.md`, `DECISIONS.md` |
| 0.2 | Freeze the 1.0 scope and exclusions | Kort | Done 2026-10-28 | `PLAN-TO-1.0.md` section 0.2 |
| 0.3 | Confirm the slice map (Ironhold) | Kort | Done 2026-10-28 | `DECISIONS.md`, plan 7.1 |
| 0.4 | Confirm the adoption format and AI build authority | Kort | Done 2026-10-28 | Plan 7.2 and 7.3 |
| 0.5 | Re-run fast and game on `main`; record the baseline | Agent | Done 2026-10-28 | `reports/phase-0-5-baseline-2026-10-28.md`; fast 22/22, game 23/23 at `f1f6047` |
| 0.6 | Book the six-player baseline date | Kort | Skipped 2026-10-28 | `SIX-PLAYER-TEST-PLAN.md` section 0; no date held |
| 0.7 | Publish a single visible board | Agent | Done 2026-10-29 | This file |

Phase 0 is done.

## Phase 1: close 0.5.x

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 1.1 | War Table troop sheet | Agent + Kort | Done 2026-10-31 | `c233dbc` after Kort accepted it; six-seat lobby `784249a` |
| 1.2 | Armoury restyle | Agent + Kort | Done 2026-10-31 | `711ae89` merged at `27ddf58` |
| 1.3 | Banner and Field page polish | Agent | Done 2026-10-31 | `cc88186` (House-edged portraits, Banner drag and drop, `ui-bannerfield` suite); polish `a1287c4`, `77f1931`, `d6af93f`; drag `81c047f`, `7a5bc40` |
| 1.4 | Harness Narrator parity | Agent | Done 2026-10-29 | `reports/phase-1-4-harness-narrator-2026-10-29.md`; fix `18522ba` |
| 1.5 | Animation pass 1 | Agent + Kort | In progress; merge approved (Q62), not in `main` | `work/anim-rework` (tip `463641a`) replaces the earlier `work/anim-rebase`, archived as tag `archive/anim-rebase-2026-11-02`. Kort approved the merge with the known flaws logged, the 5 to 8 degree thrust angle (Q63), jitter up to 0.15 s (Q70) and the bow cant of 45 to 60 degrees plus a bigger release fling (Q46b, Q57). It conflicts with `main` in 10 files and its session is merging `main` into the branch. Kort closed Q28 on 2026-11-04: no editor pass from him is needed |
| 1.6 | Merge the House troop models | Agent + Kort | Done 2026-11-02 | `work/models-rebase` is archived as tag `archive/models-rebase-2026-11-02`. In `main`: `art/knight-1x`, Gilded flagships (`bd05fa2`), Gilded tiers II/III (`92e8828`, Q42), troop rework `c26a3d8` (six troops keep original art with the grip fix; new Ballista and Mortar), and the Ironclad rows under 2B.3 and 2B.4 |
| 1.7 | Art production line | Agent | Done 2026-11-02 | Hand-authored `art/hand` builders per House, now in `main` for all five Houses (see 2B.3 and 4B.1 to 4B.4); the `.bbmodel` round-trip is 2B.1 |
| 1.8 | Six-player network baseline | Kort + friends + Agent | Skipped by Kort 2026-10-28 | Method kept in `SIX-PLAYER-TEST-PLAN.md` |
| 1.9 | Fix blockers and majors from 1.8 | Agent | Blocked on 1.8 | Not started |
| 1.10 | Cut 0.5.1 | Agent + Kort | Blocked on 1.5 | Kort wants a 0.5.1 candidate cut once the animation merge lands (2026-11-03). 1.6 and 1.7 are done; 1.9 is moot while 1.8 is skipped |

## Phases 2 to 6

One row per started task or task done in `main`; unlisted tasks in each phase have not started. Each row points at the plan task that defines its work.

### Phase 2: production lines

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 2A.1 | Builder server hardening | Agent | Done; EULA gate not exercised against a real server | In `main` (`8bad927`, via the integration merge); `tools/builder-server/README.md`; supervisor, backups, offsite, health check, boot task. No TPS source on 1.20.1 |
| 2A.2 | `mapforge` command surface in `BuildMode` | Agent | Done | Merged into `main` (`24398665`, `6e297138`), with 2A.4 and 2A.5 |
| 2A.3 | Map Forge tools | Agent | Done 2026-11-01 | `698b274` (Ironhold surface, Keep tower, cleanup passes, MapWalk harness); `tools/mapforge/`, `docs/maps/ironhold/` (layout signed 2026-10-30) |
| 2A.4, 2A.5 | Adoption spike and pipeline | Agent + Kort | Done; Ironhold bake superseded | Kort approved `gg-mapforge-runs/1` and the settle rule (Q15); merged. The Ironhold builder round trip is dropped: Kort designs all five maps himself in the map engine editor (2026-11-02, supersedes Q73), and no agent builds a layout. Map engine slice 1 (`14709a2`) and plans 2 to 4 (`6de0b53`, `6552d9c`, `15d8061`) are in `main`, and so are the editor files for the five maps (`2a6d2a8`) |
| 2A.8 | Server update loop | Agent | Done; EULA scratch rule set | Server kit merged `3fa9dd1` (`tools/export_server.py`, `start.ps1`, runbook; EULA never auto-accepted). Start, map build and clean stop passed in a scratch folder (`434e59e`). Kort allows the EULA in scratch server folders for the kit smoke and teleport-sync checks only (Q79). Kit smoke passed (`4c971e7`, merged `4e304ec`, hand-assembled because `export_server` needs the Excalibur zip); console stop, `restore.ps1`, `update.ps1` and client join are not recorded |
| 2A.7 | Map standards | Agent | Done 2026-11-02 | `docs/MAP-STANDARDS.md`, merged from `work/map-standards-2a7` |
| 2B.1 | Troop `.bbmodel` round-trip | Agent + Kort | Done 2026-10-29 | `c7578cb` on `art/troop-bbmodel`, in `main`; 118 assets byte-identical, 18 tests |
| 2B.2 | Per-House art bible | Kort + Agent | Done 2026-11-02 | `docs/ART-STYLE-GUIDE.md`, merged from `work/art-bible-2b2` |
| 2B.3 | Ironclad tier I batch plus 1x rebuild of Hearthguard, Tusk and Tideguard | Agent + Kort | Done 2026-11-02 | `art/ironclad-1x`, `claude/epic-bose-bb1833` and the Tideguard sea-blue recolour (`6ef1ed1`, Q40) are in `main`; `docs/reports/ironclad-art-a-2026-11.md`, `ironclad-art-b-2026-11.md`, `house-troops-1x-2026-11.md` |
| 2B.4, 2B.5 | Ironclad tiers II and III; Ironclad visual gate | Agent, Kort | Done 2026-11-02 | `art/ironclad-t2t3` merged in `878c69d` with Tidecallers and Wildmoot tiers II/III; Kort's approval (Q71) is the visual sign-off |
| 2C.1 | Mode frame | Agent | Done 2026-10-31 | `d24592e` (`BattleMode`, `Modes`, `ModeMath`, `DominationMode`; `modes` sheet); `reports/mode-frame-merge-2026-10-31.md`. `mode/frame` is superseded |
| 2C.2 | Mode feed lines, ledger, briefing how-to | Agent + Kort | Done; text awaits Kort's read | Domination, Siege and Convoy lines in `main` (`a912575`); Kort has not yet read the draft text in game (Q19a). Survival lines are under 4C.4 |
| 2C.3 | Campaign schema 3, `MarchMath`, order lock, bonus payout | Agent | Done 2026-10-31 | `8e39637` (from `9660a4f`); `reports/campaign-2c3-2026-10-31.md`; Siege and Convoy payout `3fdf85e`, `5e48a8d` |
| 2C.4 | Campaign battles as real battles | Agent + Kort | Done 2026-11-02 | Merged `7b5eecd` (harness `march-modes` march-2 to 4, `march-bonus` march-5 and 6, `GRAVES_MARCH_MODES`). Victory gold `173a747` (Q53b). Kort's rulings (Q76 to Q78) are in `main` as `aaae785` (merged from `1a5b7cc`): battle 6 is Siege with allied bots from all five Houses, difficulty ramps (battles 2-3 normal, 4-5 hard, 6 hardest), battle 2's bonus is the gate down by 360 s |
| 2C.5, 2C.6 | Campaign card and Pact report; retire 1v1 | Agent | Done 2026-11-02 | `b693cca4` (2C.5 card and report checks), `5be81366` (campaign seats per row), `5d98429e` (pay by time seated; battles 2-3 2v2, 4-6 3v3), all Kort approved |

### Phase 3: six-player slice

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 3.1 | Adopt camp and Ironhold | Agent + Kort | Waits on Kort's map | Ironhold surface in `main` (`698b274`). Kort fixes Ironhold himself in the map engine after playing it (Q36) and signs off each master layout before anything is built. Camp not started |
| 3.2 | Siege world pieces | Agent | Done 2026-11-01 | `490c298` (three stages, Keep ring, gate rulings); `d1b013b` (sheet-driven rules, wards that seal the yard, `gate_intact`); teardown and `recover` in `d24592e`; checks `ebab3c8`, `66f7e79`; second defend line `6ca063f` |
| 3.3 | Siege bots | Agent + Kort | Done 2026-11-02; balance knob with Kort | `4fa1872` (stage plans, mode sim, `tools/mode_gate.py`); Siege clock 185 s and archer climb fix `1f14562` |
| 3.4 | Six-player lobby | Agent | Done 2026-10-31 | `784249a` |
| 3.5 | Dedicated server kit | Agent + Kort | Done 2026-11-03 | `3fa9dd1` (scripts, version pin, runbook, smoke harness); smoke test passed in a scratch folder (`4c971e7`, merged `4e304ec`, Q79). Deploying to `C:\GGMatch` is Kort's step |
| 3.6 | Performance baseline | Agent | Done 2026-11-02 | `7ca3221`; `docs/reports/perf-baseline-2026-11-02.md` |
| 3.7 | Six-player slice test | Kort + friends + Agent | Prep done; Q79 answered, live rounds not run | `65f2bcc`, `8b1aa68`; `docs/reports/slice-dryrun-2026-11-02.md` (Siege, hold expiry, restart and persistence pass; the crash payout failed, then the QA rules landed in `edf8c38`). Pay by time seated is in (`5d98429`). Q79: the server EULA may be accepted in scratch folders for the teleport-sync check and the server smoke only, so a real six-player round still needs Kort. Lobby builder world for friends: `67fb4ac` (merged `0c72cf9`) |
| 3.8 | Slice fix round | Agent | Done 2026-11-03 | `09d2bac` (teleport sync after relocate and warp, rejoin ready flag). The live teleport-sync check passed (`4c971e7`, merged `4e304ec`) |
| 3.9 | Slice visual gate | Kort | Not started | Needs the 1080p, 1440p, 4K and small-window pass (VIS-01) |

### Phase 4: content

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 4A.1 to 4A.6 | Other maps, dressing, camp detail, map art | Kort, then Agent | Waits on Kort's map designs | Kort designs Rivergate, Ironhold, Frostmarch, Hollowwood and Kingsfield himself in the map engine editor (2026-11-02, sizes as today). Agents build only from his designs, after he signs off each master layout. Editor files are in `main` (`2a6d2a8`) |
| 4B.1 | Wildmoot completion | Agent + Kort | Done 2026-11-02 | Tier I (`def8e74`, `a5fb6f3`), Tusk and trims, tiers II/III in `878c69d` (Q64, Q69, Q72) |
| 4B.2 | Tidecallers batch | Agent + Kort | Done 2026-11-02 | Tier I `c45b5843` (Q45); tiers II/III in `878c69d` |
| 4B.3 | Graveborn batch | Agent + Kort | Done 2026-11-02 | Tier I `2998e4a` (Q47); tiers II/III `0b8b3f3` (Q60, Q65) |
| 4B.4 | Gilded completion | Agent + Kort | Done 2026-11-02 | `92e8828` (Q42); Sunweaver and Lord Marshal tiers II/III |
| 4B.5 | House identity set | Agent + Kort | Done 2026-11-03 | Marks, banners, braziers and card frames merged `9808fa8` (`1ed8557`, Q81). Recolour merged `6d619a7` (`713ecbf`, Q82): Graveborn violet, Tidecallers sea blue, recruit banners regenerated. Kort approved both |
| 4B.6 | Art QA | Agent | Done once; re-run after the tier II/III merges | `docs/reports/art-qa-2026-11-02.md` (`75e4786`, `699ab12`, `a49c9a1`). Release verify of clean `9c2d5f1` passed, package and readback included |
| 4B.7 | Final art gate | Kort | Not started | 30 of 30 troops at 9 of 10 and no stopgap assets; the package no longer refuses for missing tier II/III art |
| 4C.1 | Survival | Agent + Kort | Done 2026-11-02 | `6abb424` in `main`; Kort approved all draft values, allied bots count toward waves, early rise after a clear (Q14). Feed lines `e66445b`, sheet-driven lines `7a0d186` |
| 4C.2 | Banner / escort decision | Agent | Done 2026-10-31 | Kort chose Convoy escort in place of carry Banner; Convoy merged at `7838662` (`5b49aba`, `d102c51`, `257cc3d` wagon, `1ad6a02` harness). Straight draft Frostmarch road accepted for now |
| 4C.3 | Host picks map and mode | Agent | Done 2026-10-31 | `784249a`; lobby offers Domination, Siege and Convoy |
| 4C.4, 4C.5 | Mode bot runs and gate; HUD, feed, report lines for every mode | Agent + Kort | Done; text awaits Kort's read | Mode gate `770b198` (Siege 185 s with stage-0 cap 50%, Convoy wagon slowed, escort 12/20, yard hold 15 s `612a8710`); Survival feed, ledger and report lines `7a0d186` (draft copy); Convoy push check fixed `8aab044`. A gate re-run on current `main` is not recorded |
| 4D.1 | Six campaign battles | Agent + Kort | Done 2026-11-03 | All six play and pay on `main`; campaign rulings `aaae785` |
| 4D.2 | Briefings, reasons, lessons, bonus per battle | Agent + Kort | Awaiting Kort's edit | Bonus picks applied in `8e39637`; battle 2 to 6 briefings and reasons are in `main` (`df8089d`, `docs/drafts/campaign-briefings-2026-11-02.md`), marked as drafts until Kort edits them (Q74). Battle 6's mix and difficulty are settled (`aaae785`); the wording is not |
| 4D.3 | Order lock, chapter completion, Pact grant, bonus shards | Agent | Done 2026-10-31 | `8e39637`, `3fdf85e`, `649bcb0` (chapter gold and Rare price from the sheet) |
| 4D.4 | Campaign playthrough | Agent + Kort | Agent checks done; Kort's playthrough not done | In-game bonus check merged (`docs/reports/ingame-check-2026-11-02.md`); harness suites `march-modes` and `march-bonus`. Kort has not played the six battles end to end; the rulings that shape them are in (`aaae785`) |
| 4D.5 | Preflight validation of campaign rows | Agent | Done 2026-10-31 | `tools/preflight.py` validates order, House, bonus and the `placeholder` column |
| 4E.1 to 4E.3 | Animation passes | Agent + Kort | In progress (1.5 lane) | `work/anim-rework`, not yet in `main`; pass 2 not started |
| 4F.1, 4F.6 | War Table pages, 1440p layouts | Agent + Kort | In progress | 3x scale `30e3f3f`; troop sheet, Armoury, Banner and Field, lobby and campaign card (`b693cca`) merged. Kort has not checked the 1080p, 1440p and 4K windows (VIS-01) |
| 4F.2 | Report layout | Agent | Done | In `main` (`7e1e0f0`, `d932922`, `7951be8`; ui-report suite passed in game); the mode block and the Battle, Troops and Chapter tabs were approved (Q18) |
| 4F.3 | Title screen | Agent + Kort | In progress | Kort's direction (2026-11-03): slow-motion shots of battlefield fights, such as arrow volleys and lines clashing. A painted battle scene option is also being explored. Nothing built or in `main` |
| 4F.4 | Audio | Agent + Kort | Done | Pixabay sounds, music director and combat cues merged `2ee839e` (Kort approved all cues, Q24). Cue timing merged `4fe40e9` (`5e41cb2`; `docs/reports/audio-cue-timing-2026-11-02.md`): cues fire on contact, release and event ticks. Kort approved trimming the silent lead of bow release, arrow impact and armour ring, then re-levelling (Q83); merged `3885d32`. `docs/reports/audio-checklist-2026-11.md` |
| 4F.5 | Accessibility | Agent | Done | `13d1047` in `main`; focus ring and disabled veil approved, Banner key default N (Q16) |

### Phase 5: balance and hardening

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 5.1 | Balance design review | Kort + Agent | Done 2026-11-02 | Set A `3b8b58b`, arrow speed `19c7b4d`, vanilla-trait audit and measurement merged `fa7e4975`. Kort: Houses balanced enough; no rule changes or stat retune; further balance comes from playtesting. |
| 5.2, 5.3 | Bot round robin on adopted maps; tune the sheet | Agent | Not started | Waits on Kort's maps (3.1) and the audit |
| 5.4 | Economy and progression | Agent | Done 2026-11-02 | Wiring merged `649bcb0`; check report `docs/reports/economy-check-2026-10-30.md` merged from `work/economy-check-5-4` |
| 5.5 | Performance | Agent + Kort | Done; tick 0 under 50 ms | Target report `726a5fb` (`db2bc28`, `docs/reports/perf-targets-2026-11-02.md`): server tick and FPS targets met; spawn spike 51 to 118 ms and lobby-return save 0.8 s missed. Kort accepts the 0.8 s save as is (Q84). Spawn-spike fix `4be6e09` merged `667028f` (cold startRound 105 to 29 ms; tick 0 still about 86 to 95 ms under load, to re-measure idle). Idle re-measure 2026-11-03 (`perf/tick0`): tick 0 218 ms cold, 66 ms warm (still over 50 ms); JFR shows startRound cheap, cold cost is vanilla JIT warm-up; probe now logs battle ticks 0-19. Fix merged `f5a9939` (`2cb9cb2`): the slow tick 0 was the bot-sim harness starting the round in the arena-ready tick; it now waits the 200-tick pad countdown like real rounds. Idle tick 0 47.6 ms cold, 22.3 ms warm; shipped code unchanged; CampNetworkChecks fresh-root check not run. Autosave skips battles (`adef601`, Q37) |
| 5.6 | QA matrix | Agent | Run 2026-11-03: 104 PASS, 2 FAIL (C9 Q91, I10 Q93), 30 Kort, 1 GAP (K14), 0 not run | `docs/QA-MATRIX.md` (`6823bcc`); rules merged `edf8c38` (partial pay for a stopped match, ops-only warp, 2 minute reconnect hold). Fresh install and migration rows have no recorded run |
| 5.7, 5.8 | Release-candidate test; fix rounds | Kort + friends, Agent | Not started | |

### Phase 6: 1.0 release

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 6.1 | Strict package | Agent | Passes as of `9c2d5f1` | Release verify of a clean checkout passed, package and readback included, with Excalibur inside. Re-run at `01b0c77` on 2026-11-03: release verify PASS, package and readback included |
| 6.2 | Modrinth, CurseForge and server kit exports | Agent + Kort | Tooling done; CurseForge import is Kort's step | The `.mrpack` installed and booted in a launcher test (`bannerbound-artint/work/launcher-test/RESULT.md`); the CurseForge zip passes at file level, but the mod fetch needs an API key (Q75). Excalibur 1.20 is pinned by sha512 and bundled unmodified with credit (`6f59300`, Kort approved) |
| 6.3 to 6.6 | Install test, candidate record, final session, publish | Agent, Kort | 6.3 dry run passed 2026-11-04 | Pre-RC dry run (`reports/pre-rc-install-2026-11-04.md`): all four downloads build and install, kit server joins; packaging text and portable paths fixed (`cfb8a974`). Open: version bump and Kort's release-note wording (Q111), server kit contents (Q110). Publish only on Kort's explicit go |

| Phase | State | Task table |
|---|---|---|
| 2. Production lines: Map Forge, art line, mode frame | In progress (every 2A, 2B and 2C row is done except the animation merge, 1.5, and Kort's read of the mode text) | Plan section 3, 2A to 2C |
| 3. Six-player slice | In progress (3.2 to 3.6 and 3.8 done, 3.7 prepped; 3.1 and 3.9 wait on Kort, and a real six-player round needs Kort) | Plan section 3, tasks 3.1 to 3.9 |
| 4. Content completion | In progress (4B.1 to 4B.5, 4C, 4D.1, 4D.3, 4D.5, 4F.4 done; maps wait on Kort, title screen in progress) | Plan section 3, tasks 4A to 4F |
| 5. Balance, performance, hardening | In progress (5.1, 5.4 and 5.5 done, 5.6 partly) | Plan section 3, tasks 5.1 to 5.8 |
| 6. 1.0 release | In progress (6.1 and 6.2 tooling done) | Plan section 3, tasks 6.1 to 6.6 |

## Open lanes at 2026-11-04

Every other session branch is merged into `main` or archived.

| Lane | Branch | Plan task | Waits on |
|---|---|---|---|
| Gear art | `claude/elastic-proskuriakova-2b07e1` | gear | Kort's review fixes, then Kort sees the shots (Q108) |
| House gear sets | cloud branch `claude/gifted-ptolemy-j2rv7i` (not pushed yet) | gear | The push, Q113 and the gear art merge |
| Maps | `maps/` in `main` | 3.1, 4A | Kort's own designs and layout sign-off |
| Real six-player round | none | 3.7 | Kort and friends; scratch-folder EULA is allowed for smoke tests only (Q79) |

## What is actually true today

Three kinds of claim, kept apart on purpose.

| Kind | State |
|---|---|
| Verified by a run | Baseline fast 22/22 and game 23/23 at `f1f6047` (old); verify game passed on `aaae785`, `4fe40e9` and `09a65cd` (merge log); release verify of a clean checkout passed at `9c2d5f1`, package and readback included; the `.mrpack` launcher test; the merge log in `work/MERGE-QUEUE.md`. On `main` itself one check, `test_sample_builds_clean`, fails because of Kort's unsaved map edits. QA client rows A2, A3, I1 to I4, D22 and E10 pass (`6c5a4c4`, `3cd82ef`); kit smoke and teleport sync pass (`4c971e7`); perf targets met except spawn spike and lobby save (`726a5fb`). Release verify of a clean checkout passed again at `987f3458` (2026-11-03, offline, package and readback included) |
| Visually accepted | The War Table troop sheet and Armoury (Kort, 2026-10-31); all five Houses' tier I to III art, approved in Kort's 2026-11-02 rounds (Q42, Q45, Q47, Q64, Q65, Q69, Q71, Q72); the House identity set and recolour (Q81, Q82); the Pixabay audio cues (Q24). Not the animation lane. Kort has not read the mode text, played the six battles or checked the 1440p and 4K windows |
| Not done at all | Six networked players on a real baseline, dedicated-server reconnect and restart under real players, the animation merge, the 0.5.1 cut, Kort's five map designs and their adoption, balance retake on those maps, a CurseForge app import test, the final art gate and the title screen. The audio sample trims (Q83) are done (`4f301f0d`) |

## Where the authoritative records live

| Record | File |
|---|---|
| Candidate identity, jar hash, known issues | `docs/releases/current-candidate.json` |
| Approved scope and decisions | `docs/PLAN-TO-1.0.md`, `docs/DECISIONS.md` |
| Policy | `docs/WORKFLOW.md` |
| Setup and verification commands | `docs/SETUP.md` |
| Open questions for Kort | `docs/QUESTIONS-FOR-KORT.md` |
| Six-player method | `docs/SIX-PLAYER-TEST-PLAN.md` |
| Verification run data | `work/verify/<timestamp-pid>/result.json` in each checkout |
