# Graves and Glory roadmap v2

2026-10-28. Working roadmap written from Kort's statements and the current docs. It supersedes the sequencing in `VISION-AND-ROADMAP.md` section 11; the approved execution plan is `PLAN-TO-1.0.md`. The vision's pitch, pillars, Houses, commander, battle and economy sections still stand except where section 3 below says otherwise. Policy and approval gates are in `WORKFLOW.md`: gameplay, save, protocol and dependency changes need Kort's decision; nothing is pushed, published or merged on a gameplay branch without him.

**Approved execution plan (2026-10-28):** [PLAN-TO-1.0.md](PLAN-TO-1.0.md) is the ordered work and the Phase 0 decision record. Where this roadmap conflicts with it — map authoring, adoption, the slice map, mode/campaign sequencing and the decision list — the plan and `DECISIONS.md` win. This file remains the vision-to-milestones map.

## 1. Where we stand (checked against docs and git on 2026-10-28)

| Area | State | Evidence |
|---|---|---|
| Tooling and workflow (old Phase 0) | Done. Fast and game tiers pass on the 2026-10-28 integration; exact identity and evidence are in `docs/releases/current-candidate.json` | `reports/optimization-2026-10-25.md`; `reports/cleanup-2026-10-28.md` |
| Solo core, Houses, in-match progression (old Phases 1-2) | Merged into `main`; progression gate passed headless (`5bad9a3`). No Kort playtest gate is recorded | `git merge-base`; `HANDOFF.md` |
| Built world (old Phase 3) | Merged (`f172667`), then parked at the screenshot gate | `PARKED.md`, 2026-10-24 |
| UI | Claims, recruit, Armoury, class cards, banner House chips and HUD LAST STAND/feed are merged into `main`. The War Table troop sheet (Task 5) is WIP on `work/war-table-troop`; the Armoury restyle waits for Kort | `git`; `HANDOFF.md`; `BACKLOG.md` |
| Modes and campaign (old Phase 4) | Spec approved 2026-10-18 and a 28-task plan written. No implementation commits | `a119e5c`, `2d86d60` |
| Friends and public (old Phases 5-6) | Not started | — |
| Builder server | Live on tjepkema-server, `100.72.202.38:25570`, six worlds. The code is merged into `main`; the docs live in the main checkout | `BUILDER-SERVER.md` |
| Animations | Pass 1 is WIP on `work/anim-pass-1`; weapon clipping is improved but no clip reached 9. The code-authored keyframe system and harness checks are in `main` | `HANDOFF.md`; `ANIMATION-PLAN.md` |
| Tier art | Strict package gate fails: 26 troops lack complete tier art of their own | optimization report |
| Balance | The levels-on round robin retake is pending. The Graveborn-vs-Gilded gate failure (PARKED, Task 7) has no recorded resolution | `BACKLOG.md` BALANCE |
| Candidate record | Current at `docs/releases/current-candidate.json` for the 2026-10-28 integration; release remains blocked by the six-player, visual and package gates | file |
| Media | Parked by Kort until maps and units are final | `BACKLOG.md` |

The 2026-10-28 cleanup promoted `main`, committed the preserved plans and handoffs, and kept all 30 worktrees and run evidence. See `reports/cleanup-2026-10-28.md`.

## 2. Milestones, in order

**Superseded by PLAN-TO-1.0.md (2026-10-28):** the phase plan replaces these milestones for execution. M1 is done; M2 is complete (scope and plan approved, slice map Ironhold); M3 to M8 remain the capability areas, sequenced by the plan.

**M1. Local UI integration (done 2026-10-28).** Claims, recruit, Armoury, class cards, banner and HUD are merged; the War Table troop sheet remains WIP on `work/war-table-troop`.
The work merged claims, recruit, Armoury, the HUD patch, the banner page and the builder-server docs; BuildMode stays inert without `-Dgg.build`. The War Table troop sheet was built after the recruit commit but never committed, so it remains WIP on `work/war-table-troop`.
*Exit:* fast and game tiers pass on the new source identity; War Table pages pass a visual check at the reference sizes (including VIS-01); the candidate record is updated for that identity; `main` is fast-forwarded. Kort's look is War Table plan Task 10.

**M2. SLICE-1 scoping (done 2026-10-28).** Kort approved the scope and PLAN-TO-1.0.md; the slice map is Ironhold, map adoption is overlay-first and AI build authority is recorded. Remaining detail: bot-quality targets, lobby/map art direction and when each builder world is done.
*Exit:* met for the plan. M5 and M6 are sequenced by the plan.

**M3. Animation spec.** Pass 1 is in progress on `work/anim-pass-1`; signature/Blender work and pass 2 follow M2.
Pass 1 covers all six classes in their core states: idle, walk, run, hit, death, and an attack per weapon (sword, axe, spear, trident, bow and so on). Pass 2 adds formation hold with teammates and the charge. Recommendation: extend the existing `TroopAnimator` with a hybrid. The procedural base (walk, run and idle blending already exist) stays in code, and signature attacks are hand-keyed clips. Kort or the builder may supply files, so the spec fixes one import format.
Animation is client-only and must not change server hit timing; a change there is a gameplay change. If pass 2 needs new stance data, that is a protocol change needing approval.
*Exit:* approved spec. Then a pass-1 contact sheet per class and state at threshold 9, with no `TroopTells` regression.

**M4. Excalibur in the pack.** Dependency change, approved in principle 2026-10-28.
Add Excalibur (Maffhew, CC BY-NC-ND 3.0) to the modpack with credit and licence notice. Ship it bundled or by reference, as Kort decides. Do not modify its files: the licence forbids derivatives. Fresh Animations is not added.
*Exit:* troop readability, side colours and HUD checked with Excalibur on and off; the ETF emissive double-glow from PARKED Task 5 checked.

**M5. Adopt the built maps and lobby.** Blocked until Kort and the builder say the worlds are done.
This needs its own spec, from PARKED "Hand-built live maps". It covers bypassing generation, re-deriving spawns, capture circles, routes, landmark rows and `CampTerrain.REVISION`, and deciding how saves ship (jar structure files versus world data). It is a world, save and gameplay change, so it needs Kort's approval.
*Exit:* each adopted world passes the walk and route suites, bots path and capture, camp stations are reachable, and screenshots are approved.

**M6. Build SLICE-1.** Needs M1, M2 and M5 for the slice maps; M3 pass 1 if in scope.
*Exit:* the lobby, match, rewards and return loop works on a dedicated server with six networked players (bots do not count), with persistence, reconnect and restart tests and visual acceptance at threshold 9.

**M7. Balance retake and tier art.** Needs M5, because new layouts invalidate old balance data.
Run the levels-on round robin on the adopted maps. Paint tier art for the 26 troops.
*Exit:* the balance report is accepted and the strict package gate passes with no bypass.

**M8. Friends candidate (old Phase 5).** Package, candidate record and a Kort playtest. Publishing happens only on Kort's explicit go.

**Later:** the rest of the Phase 4 modes and chapter 1 after the slice, media when maps and units are final, and public play (old Phase 6) stays parked.

## 3. What changed from the old roadmap

- **Maps and lobby (superseded 2026-10-28):** the approved plan puts map building back with the AI, with Kort and one friend editing the persistent builder server. The builder worlds remain the editable source; the Claude-built Phase 3 landmarks are the starting worlds. Old section 13 used "builder" to mean Claude; "builder" now means the human-editable server. See PLAN-TO-1.0.md sections 4 and 7.3.
- **Resource packs:** the 2026-10-18 rule of no third-party resource packs is lifted for Excalibur only. Clarent and Fresh Animations stay out.
- **Animations:** a new workstream, M3.
- **Sequencing:** milestones now centre on the six-player slice (optimization v2, 2026-10-25) instead of one Kort playtest per phase.
- **Superseded:** old section 12 model routing gives way to `WORKFLOW.md`, plus Kort's 2026-10-28 ban on Fable models. Versioning is stale: the mod is already 0.5.0 without the friends phase.
- **Dropped or parked, not deleted:** Claude-authored camp and landmark structures, media, the withdrawn growth/0.6.0 plan, mounts, the escort variant and public play.

## 4. Next actions (approved plan)

1. **Kort's review.** The three rebased lanes (War Table, House models, animation pass 1) await visual review and a merge or park decision.
2. **Board.** `docs/BOARD.md` is the single task board; keep it current as tasks move.
3. **Phase 1.3.** Banner and Field page polish once 1.1 is decided; 1.7 follows the House models' acceptance.

The six-player date (0.6) is skipped for now by Kort; the 0.7 board is published at `docs/BOARD.md`.

Then start Phase 1: rebase the War Table, House-model and animation lanes, run the six-player network baseline, and cut 0.5.1. The full sequence is in PLAN-TO-1.0.md.

## 5. Decisions only Kort can make

**Status 2026-10-28:** items 1, 4, 7, 9 and 10 are fully answered in PLAN-TO-1.0.md and DECISIONS.md; items 2, 3 and 5 are partly answered (performance targets, adoption and AI authority, hybrid animation approach). Still open: bot-quality targets, when each builder world is done, who keys the Blender clips, the pass-2 protocol change (item 6) and status effects on commanders (item 8).

1. SLICE-1 scope: team split, content counts, slice maps, campaign segment and competitive scenario (SLICE-01).
2. Performance and bot-quality targets, and the six-client test hardware (SLICE-03).
3. When each builder world is "done", and approval of the M5 adoption design: replacing generation, how saves ship, and whether moved spawns or capture points are acceptable.
4. Excalibur: bundled or by reference, and written confirmation from Maffhew of what "Yes that's ok" covers. Under CC BY-NC-ND, any later paid feature that includes the pack needs permission for commercial use.
5. The animation author and approach (hybrid recommended), and what "all 6 classes" means. If it means the six commander classes, player-model animation likely needs a new mixin or dependency.
6. Whether animation pass 2 may add stance or formation data to the protocol.
7. Whether packaging for friends may use stopgap tier art, or the strict gate stays, and who paints the 26 troops.
8. Whether status effects also hit player commanders (round 34, question 3).
9. The version number of the next candidate.
10. Disposal of inactive worktrees (OWN-01). Kort chose on 2026-10-28 to keep all 30 worktrees and their evidence; deletion remains a future decision.
