# Board: PLAN-TO-1.0

2026-11-02, checked against `main` at `5056c56`. The single visible state for the 1.0 plan. One row per task in the active phases; later phases get one row until they start. Every row names an owner, a state and the evidence that settles it. If this board and another document disagree, fix both in the same commit.

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
| 1.5 | Animation pass 1 | Agent + Kort | In progress, waiting on Kort's merge decision | `work/anim-rework` (tip `8dac133`, 115 commits, 121 behind `main`) replaces the earlier `work/anim-rebase`, archived as tag `archive/anim-rebase-2026-11-02` (`b023fe1`, 12/12 checks, 196 turn shots). The animation editor (`work/anim-edit`, `bannerbound-anim-edit`) also awaits Kort |
| 1.6 | Merge the House troop models | Agent + Kort | In progress | `work/models-rebase` (`bf86e26`, 8/10 baseline) not merged. Gilded 1x art on `art/knight-1x`, merge pending. Troop rework merged `c26a3d8` (six troops keep original art with the grip fix; new Ballista and Mortar). Ironclad lane under 2B.3 |
| 1.7 | Art production line | Agent | In progress | Gilded sun staff `7bfe24e`, `595cd16` on `art/knight-1x`; `tools/paint_house_troops.py` is not yet a per-House generator |
| 1.8 | Six-player network baseline | Kort + friends + Agent | Skipped by Kort 2026-10-28 | Method kept in `SIX-PLAYER-TEST-PLAN.md` |
| 1.9 | Fix blockers and majors from 1.8 | Agent | Blocked on 1.8 | Not started |
| 1.10 | Cut 0.5.1 | Agent | Blocked on 1.5 to 1.7 and 1.9 | Not started |

## Phases 2 to 6

One row per started task or task done in `main`; unlisted tasks in each phase have not started. Each row points at the plan task that defines its work.

### Phase 2: production lines

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 2A.1 | Builder server hardening | Agent | Awaiting merge (EULA gate not exercised against a real server; no TPS source on 1.20.1) | `worktree-agent-a67738406dd158482` (`8bad927`); `tools/builder-server/README.md`; supervisor, backups, offsite, health check, boot task |
| 2A.2 | `mapforge` command surface in `BuildMode` | Agent | Done | Merged into `main` (`24398665`, `6e297138`), with 2A.4 and 2A.5 |
| 2A.3 | Map Forge tools | Agent | Done 2026-11-01 | `698b274` (Ironhold surface, Keep tower, cleanup passes, MapWalk harness); `tools/mapforge/`, `docs/maps/ironhold/` (layout signed 2026-10-30) |
| 2A.4, 2A.5 | Adoption spike and pipeline | Agent + Kort | Done; release bake blocked | Kort approved `gg-mapforge-runs/1`, the settle rule and a first release bake (Q15). Merged. The release bake needs a fresh builder round trip: the dev overlay is stale after the D1a spawn move `c6a6d0fc` (Kort question pending) |
| 2A.7 | Map standards | Agent | Done 2026-11-02 | `docs/MAP-STANDARDS.md`, merged from `work/map-standards-2a7` |
| 2B.1 | Troop `.bbmodel` round-trip | Agent + Kort | Done 2026-10-29 | `c7578cb` on `art/troop-bbmodel`, in `main`; 118 assets byte-identical, 18 tests |
| 2B.2 | Per-House art bible | Kort + Agent | Done 2026-11-02 | `docs/ART-STYLE-GUIDE.md`, merged from `work/art-bible-2b2` |
| 2B.3 | Ironclad tier I batch plus 1x rebuild of Hearthguard, Tusk and Tideguard | Agent + Kort | Awaiting Kort's art review | Ironclad A and B on ic-1x-a and ic-1x-b; House troops 1x on `claude/epic-bose-bb1833` (in-game check passed) |
| 2B.4, 2B.5 | Ironclad tiers II and III; Ironclad visual gate | Agent, Kort | Blocked on 2B.3 | Not started |
| 2C.1 | Mode frame | Agent | Done 2026-10-31 | `d24592e` (`BattleMode`, `Modes`, `ModeMath`, `DominationMode`; `modes` sheet); `reports/mode-frame-merge-2026-10-31.md`. `mode/frame` is superseded |
| 2C.2 | Mode feed lines, ledger, briefing how-to | Agent | In progress (subagent, 2026-11-01) | Convoy HUD strip and track `70b61e4`; Domination, Siege and Convoy lines on a worktree branch |
| 2C.3 | Campaign schema 3, `MarchMath`, order lock, bonus payout | Agent | Done 2026-10-31 | `8e39637` (from `9660a4f`); `reports/campaign-2c3-2026-10-31.md`; Siege and Convoy payout `3fdf85e`, `5e48a8d` |
| 2C.4 | Campaign battles as real battles | Agent | In progress | All six battles start from the campaign command in their row's mode and map, win and pay their bonus (harness `march-modes` march-2 to 4, `march-bonus` march-5 and 6, run 2026-11-02 on `work/campaign-battles-356`). Open: difficulty cells for battles 2 to 6, and battle 6's mixed finale (plays plain Siege until Kort picks the mix) |
| 2C.5, 2C.6 | Campaign card and Pact report; retire 1v1 | Agent | Done 2026-11-02 | `b693cca4` (2C.5 card and report checks), `5be81366` (campaign seats per row), `5d98429e` (pay by time seated; battles 2-3 2v2, 4-6 3v3), all Kort approved |

### Phase 3: six-player slice

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 3.1 | Adopt camp and Ironhold | Agent | In progress | Ironhold surface in `main` (`698b274`); adoption needs 2A.5. Camp not started |
| 3.2 | Siege world pieces | Agent | Done 2026-11-01 | `490c298` (three stages, Keep ring, gate rulings); `d1b013b` (sheet-driven rules, wards that seal the yard, `gate_intact`); teardown and `recover` in `d24592e`; checks `ebab3c8`, `66f7e79`; second defend line `6ca063f` |
| 3.3 | Siege bots | Agent + Kort | Done 2026-11-02; balance knob with Kort | `4fa1872` (stage plans, mode sim, `tools/mode_gate.py`); Siege clock 185 s and archer climb fix `1f14562` |
| 3.4 | Six-player lobby | Agent | Done 2026-10-31 | `784249a` |
| 3.5 | Dedicated server kit | Agent + Kort | In progress | `claude/server-kit-4fed75` (`83538f6`, 2 commits); `tools/export_server.py` and the server-rules merge in `main` |
| 3.6 to 3.9 | Performance baseline, slice test, fix round, visual gate | Agent, Kort | Not started | Need the server kit (3.5) and Siege bots (3.3) |

### Phase 4: content

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 4A.1 to 4A.6 | Other maps, dressing, camp detail, map art | Agent + Kort | Not started | Only Ironhold (see 3.1) |
| 4B.1 to 4B.7 | House art completion and final art gate | Agent + Kort | Not started beyond 1.6, 1.7 and 2B.3 | |
| 4C.1 | Survival | Agent + Kort | Awaiting Kort's values | `worktree-agent-a99e20eda71f140df` (`6abb424`, 14 suites green) |
| 4C.2 | Banner / escort decision | Agent | Done 2026-10-31 | Kort chose Convoy escort in place of carry Banner; Convoy merged at `7838662` (`5b49aba`, `d102c51`, `257cc3d` wagon, `1ad6a02` harness). Straight draft Frostmarch road accepted for now |
| 4C.3 | Host picks map and mode | Agent | Done 2026-10-31 | `784249a`; lobby offers Domination, Siege and Convoy |
| 4C.4, 4C.5 | Mode bot runs and gate; HUD, feed, report lines for every mode | Agent | In progress | `tools/mode_gate.py` exists (`4fa1872`); harness suites `42e727e`, `1ad6a02`; feed and report lines for every mode not finished |
| 4D.1 | Six campaign battles | Agent + Kort | In progress | See 2C.4 |
| 4D.2 | Briefings, reasons, lessons, bonus per battle | Agent | In progress | Bonus picks applied in `8e39637`; battle 2 to 6 briefings and reasons are in `sheets/design.json` (`df8089d`), marked as drafts in each row's placeholder cell until Kort edits them (Q74) |
| 4D.3 | Order lock, chapter completion, Pact grant, bonus shards | Agent | Done 2026-10-31 | `8e39637`, `3fdf85e`, `649bcb0` (chapter gold and Rare price from the sheet) |
| 4D.4 | Campaign playthrough | Agent + Kort | In progress (session lane, 2026-11-01) | In-game check of the campaign bonuses on `claude/ingame-check-557622` at `19c7b4d`; harness suite `42e727e` |
| 4D.5 | Preflight validation of campaign rows | Agent | Done 2026-10-31 | `tools/preflight.py` validates order, House, bonus and the `placeholder` column |
| 4E.1 to 4E.3 | Animation passes | Agent + Kort | In progress (1.5 lane) | `work/anim-rework`; pass 2 not started |
| 4F.1, 4F.6 | War Table pages, 1440p layouts | Agent | In progress | 3x scale `30e3f3f`; troop sheet, Armoury, Banner and Field, lobby merged |
| 4F.2 | Report layout | Agent + Kort | Awaiting Kort | `worktree-agent-aeaa474f0437e717f` (`7e1e0f0`, `d932922`, `7951be8`; ui-report suite passed in game) |
| 4F.3 | Title screen | Agent + Kort | Not started | Needs Kort's direction |
| 4F.4 | Audio | Agent + Kort | Cues merged (`audio/cues`); Pixabay set awaiting Kort | `audio/pixabay` in `bannerbound-audio`; `reports/audio-checklist-2026-11.md` |
| 4F.5 | Accessibility | Agent + Kort | Awaiting Kort | `worktree-agent-a3dc6bac1ab4219e7` (`13d1047`) |

### Phase 5: balance and hardening

| ID | Task | Owner | State | Evidence |
|---|---|---|---|---|
| 5.1 | Balance design review | Kort + Agent | In progress | Set A (ten troop stat and cost changes) merged `3b8b58b`, Kort approved 2026-11-01; `reports/house-balance-*`. House balance retune (`967e980`) on `work/house-audit` awaits Kort. Vanilla-trait audit plus its measurement is a session lane (`claude/trait-audit-573141`, `claude/audit-measure-5b5fc8`; `work/house-audit` holds the 10-31 audit at `278a03d`). Arrow speed merged `19c7b4d` (lobbed-volley pace, Kort 2026-11-01) |
| 5.2, 5.3 | Bot round robin on adopted maps; tune the sheet | Agent | Not started | Waits on map adoption (3.1) and the audit |
| 5.4 | Economy and progression | Agent | Done 2026-11-02 | Wiring merged `649bcb0`; check report `docs/reports/economy-check-2026-10-30.md` merged from `work/economy-check-5-4` |
| 5.5 to 5.8 | Performance, QA matrix, release-candidate test, fix rounds | Agent, Kort | Not started | |

### Phase 6: 1.0 release

Not started. Tasks 6.1 to 6.6 wait on Phase 5.

| Phase | State | Task table |
|---|---|---|
| 2. Production lines: Map Forge, art line, mode frame | In progress (2A.3, 2A.7, 2B.1, 2B.2, 2C.1, 2C.3 done) | Plan section 3, 2A to 2C |
| 3. Six-player slice | In progress (3.2, 3.3, 3.4 done) | Plan section 3, tasks 3.1 to 3.9 |
| 4. Content completion | In progress (4C.2, 4C.3, 4D.3, 4D.5 done) | Plan section 3, tasks 4A to 4F |
| 5. Balance, performance, hardening | In progress (5.1 Set A merged, 5.4 done) | Plan section 3, tasks 5.1 to 5.8 |
| 6. 1.0 release | Not started | Plan section 3, tasks 6.1 to 6.6 |

## Session lanes at 2026-11-02

| Lane | Branch | Plan task |
|---|---|---|
| Ironclad 1x tier I art; 1x rebuild of Hearthguard, Tusk, Tideguard | `art/ironclad-1x` | 2B.3 |
| Gilded 1x art, merge pending | `art/knight-1x` | 1.6, 1.7 |
| Archer bow rework | in progress | 1.7 |
| Map engine slice 1, awaiting Kort | `claude/map-engine-slice-1-65134c` | 2A.4 |
| Dedicated server kit | `claude/server-kit-4fed75` | 3.5 |
| In-game check of the campaign bonuses | `claude/ingame-check-557622` | 4D.4 |
| Vanilla-trait audit and its measurement | `claude/trait-audit-573141`, `claude/audit-measure-5b5fc8` | 5.1 prep |
| Animation rework, waiting on Kort's merge decision | `work/anim-rework` | 1.5 |

## What is actually true today

Three kinds of claim, kept apart on purpose.

| Kind | State |
|---|---|
| Verified by a run | Baseline fast 22/22 and game 23/23 at `f1f6047` (not re-run on current `main`); Phase 2 screens 22/22 on `work/war-table-rebase`; House-model fast 22/22, Gilded art 7/7 and turn suite at `bf86e26`; animation 12/12 and 196 turn shots at `b023fe1` |
| Visually accepted | The War Table troop sheet and Armoury (Kort, 2026-10-31). Not the rebased animation or House-model lanes; the House troop art is 8/10 against a 9 gate. Siege and Convoy rules are merged; Kort accepted the draft Frostmarch road for now |
| Not done at all | Six networked players on a real baseline, dedicated-server reconnect and restart, strict package, balance retake on adopted maps, Survival, all six campaign battles, map adoption, Excalibur in the pack |

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
