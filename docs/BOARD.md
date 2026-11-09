# Board: PLAN-TO-1.0

The single visible state for the 1.0 plan. Task states only: main's head, open lanes, leads and open questions are in [STATE.md](STATE.md). Longer evidence prose is in git history (`git log -p docs/BOARD.md`). One row per task in the active phases; later phases get one row until they start. Every row names an owner, a state and the evidence that settles it. If this board and another document disagree, fix both in the same commit.

States: **Done**, **In progress**, **Awaiting Kort** (work finished, visual or merge decision pending), **Awaiting merge** (finished on a branch, not in `main`), **Blocked** (a named task must land first), **Skipped**, **Not started**.

Owners are decisions, not file ownership. `Agent` means implementation; `Kort` means a decision, visual gate or scheduling.

## Phase 0: approve and freeze

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 0.1 | Approve the plan and the seven decisions | Kort | Done 2026-10-28 | `PLAN-TO-1.0.md`, `DECISIONS.md` |
| 0.2 | Freeze the 1.0 scope and exclusions | Kort | Done 2026-10-28 | `PLAN-TO-1.0.md` section 0.2 |
| 0.3 | Confirm the slice map (Ironhold) | Kort | Done 2026-10-28 | `DECISIONS.md`, plan 7.1 |
| 0.4 | Confirm the adoption format and AI build authority | Kort | Done 2026-10-28 | Plan 7.2 and 7.3 |
| 0.5 | Re-run fast and game on `main`; record the baseline | Agent | Done 2026-10-28 | `reports/phase-0-5-baseline-2026-10-28.md` |
| 0.6 | Book the six-player baseline date | Kort | Skipped 2026-10-28 | `SIX-PLAYER-TEST-PLAN.md` section 0; no date held |
| 0.7 | Publish a single visible board | Agent | Done 2026-10-29 | This file |

Phase 0 is done.

## Phase 1: close 0.5.x

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 1.1 | War Table troop sheet | Agent + Kort | Done 2026-10-31 | `c233dbc` after Kort accepted it; six-seat lobby `784249a` |
| 1.2 | Armoury restyle | Agent + Kort | Done 2026-10-31 | `711ae89` merged at `27ddf58` |
| 1.3 | Banner and Field page polish | Agent | Done 2026-10-31 | `cc88186`; polish `a1287c4` |
| 1.4 | Harness Narrator parity | Agent | Done 2026-10-29 | `reports/phase-1-4-harness-narrator-2026-10-29.md` |
| 1.5 | Animation pass 1 | Agent + Kort | Done 2026-11-04 | `b4dfef01`; `37494090`; `work/anim-rework` never merge |
| 1.6 | Merge the House troop models | Agent + Kort | Done 2026-11-02 | `archive/models-rebase-2026-11-02`; `92e8828`; `c26a3d8` |
| 1.7 | Art production line | Agent | Done 2026-11-02 | `art/hand` builders, all five Houses in `main` |
| 1.8 | Six-player network baseline | Kort + friends + Agent | Skipped by Kort 2026-10-28 | Method kept in `SIX-PLAYER-TEST-PLAN.md` |
| 1.9 | Fix blockers and majors from 1.8 | Agent | Blocked on 1.8 | Not started |
| 1.10 | Cut 0.5.1 | Agent + Kort | Done as a candidate 2026-11-04; not published | `c99fe907`, rc2 notes `52fa325a`; record needs refresh |

## Phases 2 to 6

One row per started task or task done in `main`; unlisted tasks in each phase have not started. Each row points at the plan task that defines its work.

### Phase 2: production lines

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 2A.1 | Builder server hardening | Agent | Done; EULA gate not exercised against a real server | `8bad927`; `tools/builder-server/README.md` |
| 2A.2 | `mapforge` command surface in `BuildMode` | Agent | Done | `24398665`, `6e297138` |
| 2A.3 | Map Forge tools | Agent | Done 2026-11-01 | `698b274`; `docs/maps/ironhold/` |
| 2A.4, 2A.5 | Adoption spike and pipeline | Agent + Kort | Done; Ironhold bake superseded | Q15; `14709a2`, `15d8061`; editor files `2a6d2a8` |
| 2A.8 | Server update loop | Agent | Done; EULA scratch rule set | `3fa9dd1`; smoke `4c971e7` (Q79); `e3a26758` |
| 2A.7 | Map standards | Agent | Done 2026-11-02 | `docs/MAP-STANDARDS.md`, merged from `work/map-standards-2a7` |
| 2B.1 | Troop `.bbmodel` round-trip | Agent + Kort | Done 2026-10-29 | `c7578cb`; 118 assets byte-identical |
| 2B.2 | Per-House art bible | Kort + Agent | Done 2026-11-02 | `docs/ART-STYLE-GUIDE.md`, merged from `work/art-bible-2b2` |
| 2B.3 | Ironclad tier I batch plus 1x rebuild of Hearthguard, Tusk and Tideguard | Agent + Kort | Done 2026-11-02 | `art/ironclad-1x`; `6ef1ed1` (Q40) |
| 2B.4, 2B.5 | Ironclad tiers II and III; Ironclad visual gate | Agent, Kort | Done 2026-11-02 | `878c69d`; Q71 |
| 2C.1 | Mode frame | Agent | Done 2026-10-31 | `d24592e`; `reports/mode-frame-merge-2026-10-31.md` |
| 2C.2 | Mode feed lines, ledger, briefing how-to | Agent + Kort | Done; text awaits Kort's read | `a912575`; Kort has not read the text (Q19a) |
| 2C.3 | Campaign schema 3, `MarchMath`, order lock, bonus payout | Agent | Done 2026-10-31 | `8e39637`; `3fdf85e` |
| 2C.4 | Campaign battles as real battles | Agent + Kort | Done 2026-11-02 | `7b5eecd`; rulings `aaae785` |
| 2C.5, 2C.6 | Campaign card and Pact report; retire 1v1 | Agent | Done 2026-11-02 | `b693cca4`, `5be81366`, `5d98429e`; all Kort approved |

### Phase 3: six-player slice

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 3.1 | Adopt camp and Ironhold | Agent + Kort | Waits on Kort's map | `698b274`; Kort signs off layouts (Q36) |
| 3.2 | Siege world pieces | Agent | Done 2026-11-01 | `490c298`, `d1b013b` |
| 3.3 | Siege bots | Agent + Kort | Done 2026-11-02; balance knob with Kort | `4fa1872`; `1f14562` |
| 3.4 | Six-player lobby | Agent | Done 2026-10-31 | `784249a` |
| 3.5 | Dedicated server kit | Agent + Kort | Done 2026-11-03 | `3fa9dd1`; `e3a26758`; `reports/pre-rc-install-2026-11-04.md` |
| 3.6 | Performance baseline | Agent | Done 2026-11-02 | `7ca3221`; `docs/reports/perf-baseline-2026-11-02.md` |
| 3.7 | Six-player slice test | Kort + friends + Agent | Prep done; Q79 answered, live rounds not run | `docs/reports/slice-dryrun-2026-11-02.md`; Q79 |
| 3.8 | Slice fix round | Agent | Done 2026-11-03 | `09d2bac`; live teleport-sync check `4c971e7` |
| 3.9 | Slice visual gate | Kort | Not started | Needs the 1080p, 1440p, 4K and small-window pass (VIS-01) |

### Phase 4: content

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 4A.1 to 4A.6 | Other maps, dressing, camp detail, map art | Kort, then Agent | Waits on Kort's map designs | `2a6d2a8`; build only from signed-off layouts |
| 4B.1 | Wildmoot completion | Agent + Kort | Done 2026-11-02 | `def8e74`; `878c69d` |
| 4B.2 | Tidecallers batch | Agent + Kort | Done 2026-11-02 | Tier I `c45b5843` (Q45); tiers II/III in `878c69d` |
| 4B.3 | Graveborn batch | Agent + Kort | Done 2026-11-02 | Tier I `2998e4a` (Q47); tiers II/III `0b8b3f3` (Q60, Q65) |
| 4B.4 | Gilded completion | Agent + Kort | Done 2026-11-02 | `92e8828` (Q42); Sunweaver and Lord Marshal tiers II/III |
| 4B.5 | House identity set | Agent + Kort | Done 2026-11-03 | `9808fa8` (Q81); `6d619a7` (Q82) |
| 4B.6 | Art QA | Agent | Done; re-run after tiers II/III with no failures | `docs/reports/art-qa-2026-11-02.md`; `5344c9e5` |
| 4B.7 | Final art gate | Kort | Not started | 30 of 30 troops at 9 of 10 |
| 4C.1 | Survival | Agent + Kort | Done 2026-11-02 | `6abb424`; feed lines `e66445b`, `7a0d186` (Q14) |
| 4C.2 | Banner / escort decision | Agent | Done 2026-10-31 | Convoy merged `7838662`; Kort chose Convoy escort over carry Banner |
| 4C.3 | Host picks map and mode | Agent | Done 2026-10-31 | `784249a`; lobby offers Domination, Siege and Convoy |
| 4C.4, 4C.5 | Mode bot runs and gate; HUD, feed, report lines for every mode | Agent + Kort | Done; gate fails on `main`, Kort accepted it; text awaits Kort's read | `770b198`; `docs/reports/mode-gate-timing-2026-11-04.md`; Q118 |
| 4D.1 | Six campaign battles | Agent + Kort | Done 2026-11-03 | All six play and pay on `main`; campaign rulings `aaae785` |
| 4D.2 | Briefings, reasons, lessons, bonus per battle | Agent + Kort | Awaiting Kort's edit | `8e39637`; drafts `df8089d` (Q74) |
| 4D.3 | Order lock, chapter completion, Pact grant, bonus shards | Agent | Done 2026-10-31 | `8e39637`, `3fdf85e`, `649bcb0` (chapter gold and Rare price from the sheet) |
| 4D.4 | Campaign playthrough | Agent + Kort | Agent checks done; Kort's playthrough not done | `docs/reports/ingame-check-2026-11-02.md` |
| 4D.5 | Preflight validation of campaign rows | Agent | Done 2026-10-31 | `tools/preflight.py` validates order, House, bonus and the `placeholder` column |
| 4E.1 to 4E.3 | Animation passes | Agent + Kort | In progress; 4E.1 and the attack clips of 4E.2 merged | `b4dfef01`, `1be70ff9`; knight on `anim/geckolib-rebuild` |
| 4F.1, 4F.6 | War Table pages, 1440p layouts | Agent + Kort | In progress | `30e3f3f`, `b693cca`; VIS-01 windows unchecked by Kort |
| 4F.2 | Report layout | Agent | Done | `7e1e0f0`, `d932922`, `7951be8`; Q18 approved the tabs |
| 4F.3 | Title screen | Agent + Kort | Done 2026-11-04 | `c2dc2cd9` art; wired and merged `ef8d2817` (Q112) |
| 4F.4 | Audio | Agent + Kort | Done; polish open (Q123) | `710e1d5a` (Q107); `docs/reports/audio-checklist-2026-11.md` |
| 4F.5 | Accessibility | Agent | Done | `13d1047` |

### Commander gear (added 2026-11-03; no plan task ids)

Kort approved the design on 2026-11-03 (Q95) and the four sets on 2026-11-04 (Q104). Ids follow the `BACKLOG.md` GEAR names.

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| GEAR-1 | Foundation: data tables, save v4, `gear_v1` sync | Agent | Done 2026-11-03 | `a38e4218` (Kort, Q100) |
| GEAR-3 | Arsenal screen and Blacksmith forge | Agent | Done 2026-11-03; Kort's look at it is not recorded | `9c13ed6d`; `ui-gear` 15 pass |
| GEAR-2 | Gear combat: stats, set bonuses, perks, relics; classes removed | Agent | Done 2026-11-04 | `82ace819`; QA re-run `577daa8b` |
| GEAR-4 | Gear art: 4 sets, weapons, relic and grid icons, clips | Agent + Kort | Done 2026-11-04 | `fc2bad30` (Q108); `docs/reports/gear-art/` |
| GEAR-5 | House armour sets (five) | Agent + Kort | Done 2026-11-04 | `035d9c1f`; `3663087d` |
| GEAR-6 | Command UI (orders, formations, charge) | Agent + Kort | Done 2026-11-05 | `e771d5f1`; Kort's look not recorded |

### Merged 2026-11-05 to 2026-11-07

Kort approved each merge.

| Date | Merge | What it holds |
|---|---|---|
| 2026-11-04 | `37494090` `anim/joints-on-main` | Elbows and knees for the biped troops (row 1.5) |
| 2026-11-04 | `1d3f2edd` `qa/polish-1104` | In-game attack check for all 30 troops, fixes D1 to D6; Kort signed off |
| 2026-11-05 | `6442b1af` `fix/excalibur-weapons` | Excalibur look stays off weapons |
| 2026-11-05 | `e771d5f1` `feat/command-ui` | Command UI (GEAR-6) |
| 2026-11-07 | `1c440dd5` `fix/ui-polish` | Audit UI fixes U4, U5, U10, U18, U20, M1, M5 |
| 2026-11-07 | `2fc30f50` `fix/client-state` | Audit fixes U1, U2, U13, U15, U7/U14; `EntranceOverlayMixin` deleted (`382b6586`) |

### Phase 5: balance and hardening

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 5.1 | Balance design review | Kort + Agent | Done 2026-11-02 | `3b8b58b`, `fa7e4975` |
| 5.2, 5.3 | Bot round robin on adopted maps; tune the sheet | Agent | Not started | Waits on Kort's maps (3.1) and the audit |
| 5.4 | Economy and progression | Agent | Done 2026-11-02 | `649bcb0`; `docs/reports/economy-check-2026-10-30.md` |
| 5.5 | Performance | Agent + Kort | Done; tick 0 under 50 ms | `726a5fb`; `docs/reports/perf-targets-2026-11-02.md` |
| 5.6 | QA matrix | Agent | Run 2026-11-04: 142 PASS, 0 FAIL, per the candidate record; Kort rows and K14 still open | `docs/QA-MATRIX.md`; `577daa8b` |
| 5.7, 5.8 | Release-candidate test; fix rounds | Kort + friends, Agent | Not started |  |

### Phase 6: 1.0 release

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 6.1 | Strict package | Agent | Passes as of `bb2d85c4` | `bb2d85c4` (`c07aff33`) |
| 6.2 | Modrinth, CurseForge and server kit exports | Agent + Kort | Tooling done; CurseForge import is Kort's step | `.mrpack` launcher test; `6f59300`; Q75 |
| 6.3 to 6.6 | Install test, candidate record, final session, publish | Agent, Kort | 6.3 dry run passed 2026-11-04 | `reports/pre-rc-install-2026-11-04.md`; `c07aff33` |

| Phase | State | Task table |
|---|---|---|
| 2. Production lines: Map Forge, art line, mode frame | In progress | Plan section 3, tasks 2A to 2C |
| 3. Six-player slice | In progress | Plan section 3, tasks 3.1 to 3.9 |
| 4. Content completion | In progress | Plan section 3, tasks 4A to 4F |
| 5. Balance, performance, hardening | In progress | Plan section 3, tasks 5.1 to 5.8 |
| 6. 1.0 release | In progress; release blocked, not accepted | Plan section 3, tasks 6.1 to 6.6 |
| 7. Official hosted network | Not started; plan merged into `main` 2026-11-07 | Plan section 3, tasks 7.1 to 7.10 |

### Phase 7: official hosted network

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 7.1 to 7.10 | Hosted network: proxy, matchmaking, shared database, Old Hold hub, moderation, store, deploy, open beta, hub set | Kort + Agent | Not started; plan merged into `main` 2026-11-07 | Plan merged `1c363b08`; no hub work before Old Hold walk |

## What is actually true

- Verified by a run: fast and release verify at `bb2d85c4` (`c07aff33`), game tier at `5344c9e5`, QA matrix 142 PASS on 2026-11-04, pre-RC dry run. The mode gate fails Siege, Convoy and Domination; Kort accepted it (Q118).
- Visually accepted by Kort: troop sheet, Armoury, all House art tiers I to III, identity set, audio picks, gear sets, title screen. Not recorded: his check of animations in game, mode text, the six battles, 1440p and 4K.
- Not done: six networked players on a real baseline, live-server reconnect under players, the 0.5.1 publish, Kort's map designs, a CurseForge import test, the final art gate, the 1.0 version bump.

## Where the authoritative records live

Candidate identity and jar hash: `docs/releases/current-candidate.json`. Scope and decisions: `docs/PLAN-TO-1.0.md`, `docs/DECISIONS.md`. Policy: `docs/WORKFLOW.md`. Setup and verification: `docs/SETUP.md`. Open questions: `docs/QUESTIONS-FOR-KORT.md`. Six-player method: `docs/SIX-PLAYER-TEST-PLAN.md`. Verification run data: `work/verify/<timestamp-pid>/result.json` in each checkout.
