# Graves and Glory roadmap v2

2026-10-28. Proposal written from Kort's statements this session and the current docs. It replaces the sequencing in `VISION-AND-ROADMAP.md` section 11 once Kort accepts it. The vision's pitch, pillars, Houses, commander, battle and economy sections still stand except where section 3 below says otherwise. Policy and approval gates are in `WORKFLOW.md`: gameplay, save, protocol and dependency changes need Kort's decision; nothing is pushed, published or merged on a gameplay branch without him.

## 1. Where we stand (checked against docs and git on 2026-10-28)

| Area | State | Evidence |
|---|---|---|
| Tooling and workflow (old Phase 0) | Done. Fast and game verify tiers PASS on source `859a2a93…` | `reports/optimization-2026-10-25.md` |
| Solo core, Houses, in-match progression (old Phases 1-2) | Merged into `phase-1-solo-core`. Progression gate passed headless (`5bad9a3`). No Kort playtest gate is recorded | `git merge-base`: `phase-2-houses`, `progression-gate` are ancestors |
| Built world (old Phase 3) | Merged (`f172667`), then parked at the screenshot gate | `PARKED.md`, 2026-10-24 |
| UI | `ui-plan` and `ui-icons` merged. Claims (`5dcf3da`), recruit (`1e79065`) and Armoury (`92a4e8a`) are each one commit ahead and not merged. HUD lane `bannerbound-ui-hud` has 35 dirty paths | `git`; `HANDOFF.md` |
| Modes and campaign (old Phase 4) | Spec approved 2026-10-18 and a 28-task plan written. No implementation commits | `a119e5c`, `2d86d60` |
| Friends and public (old Phases 5-6) | Not started | — |
| Builder server | Live on tjepkema-server, `100.72.202.38:25570`, six worlds. Code is on branch `builder-server` (`88a01ba`, `b8df429`), not merged | `BUILDER-SERVER.md` |
| Animations | A code-authored keyframe system already exists (`TroopAnimator`, `TroopClips`, `BipedClips`, `TroopTells`; Phase 2 Task 5b). It has idle, walk, run, attack, hit, death, brace, bow_draw, shield_up and cast clips | source |
| Tier art | Strict package gate fails: 26 troops lack complete tier art of their own | optimization report |
| Balance | The levels-on round robin retake is pending. The Graveborn-vs-Gilded gate failure (PARKED, Task 7) has no recorded resolution | `BACKLOG.md` BALANCE |
| Candidate record | Exists at `docs/releases/current-candidate.json`, but it is stale. It describes `da669ee` (dirty) as "development-build; release blocked" | file |
| Media | Parked by Kort until maps and units are final | `BACKLOG.md` |

The main checkout (`builder-server`) has uncommitted edits to `PARKED.md`, two plans, `preflight.json` and `Spec.java`. Their owner is unknown, so they are preserved.

## 2. Milestones, in order

**M1. Local UI integration.** Claude can start now; Kort delegated the decision.
Work in a new integration checkout cut from `phase-1-solo-core`. Merge claims, then recruit, then Armoury. Claims and recruit overlap only in `Phase1Checks` and `Phase2Checks`, and no lane touches save, protocol or sheet files. Export the HUD lane's dirty diff as a patch and apply it there; leave the lane checkout untouched. Merge `builder-server` too: BuildMode stays inert without `-Dgg.build`.
*Exit:* fast and game tiers PASS on the new source identity; War Table pages pass a visual check at the reference sizes (including VIS-01); the candidate record is updated for that identity; `phase-1-solo-core` is fast-forwarded locally; nothing is pushed. Kort's look is War Table plan Task 10.

**M2. SLICE-1 scoping.** Planning checkpoint; blocked on Kort.
Decide SLICE-01 to SLICE-03 together: team split, content counts, slice maps, campaign segment (from the Phase 4 plan), competitive scenario, performance and bot targets, and six-client hardware.
*Exit:* a written slice spec Kort has approved. M5 and M6 depend on it.

**M3. Animation spec.** Planning now; building after M2.
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

- **Maps and lobby:** Claude no longer builds structures or maps; a human builder does, on the builder server. The Claude-built Phase 3 landmarks are now only the builder's starting worlds. The "Last Watch" camp redesign moves to the builder, and Claude's map work becomes tooling and M5 adoption. Old section 13 used "builder" to mean Claude; "builder" now means the human.
- **Resource packs:** the 2026-10-18 rule of no third-party resource packs is lifted for Excalibur only. Clarent and Fresh Animations stay out.
- **Animations:** a new workstream, M3.
- **Sequencing:** milestones now centre on the six-player slice (optimization v2, 2026-10-25) instead of one Kort playtest per phase.
- **Superseded:** old section 12 model routing gives way to `WORKFLOW.md`, plus Kort's 2026-10-28 ban on Fable models. Versioning is stale: the mod is already 0.5.0 without the friends phase.
- **Dropped or parked, not deleted:** Claude-authored camp and landmark structures, media, the withdrawn growth/0.6.0 plan, mounts, the escort variant and public play.

## 4. Recommended next 3 actions

1. **Record the 2026-10-28 decisions.** Put Excalibur, the human builder and the animation plan in `DECISIONS.md`, `BACKLOG.md` and `HANDOFF.md`. Correct the candidate path to `docs/releases/current-candidate.json`. This is small and needs no approval.
2. **Run M1, the local UI integration.** One Sonnet worker in a fresh checkout. Acceptance is the M1 exit criteria. No push.
3. **Hold the M2 SLICE-1 checkpoint with Kort.** Bring one-page drafts of the M3 animation spec and the M5 map-adoption spec, so the slice decision also fixes animation scope and slice maps.

## 5. Decisions only Kort can make

1. SLICE-1 scope: team split, content counts, slice maps, campaign segment and competitive scenario (SLICE-01).
2. Performance and bot-quality targets, and the six-client test hardware (SLICE-03).
3. When each builder world is "done", and approval of the M5 adoption design: replacing generation, how saves ship, and whether moved spawns or capture points are acceptable.
4. Excalibur: bundled or by reference, and written confirmation from Maffhew of what "Yes that's ok" covers. Under CC BY-NC-ND, any later paid feature that includes the pack needs permission for commercial use.
5. The animation author and approach (hybrid recommended), and what "all 6 classes" means. If it means the six commander classes, player-model animation likely needs a new mixin or dependency.
6. Whether animation pass 2 may add stance or formation data to the protocol.
7. Whether packaging for friends may use stopgap tier art, or the strict gate stays, and who paints the 26 troops.
8. Whether status effects also hit player commanders (round 34, question 3).
9. The version number of the next candidate.
10. Disposal of inactive worktrees (OWN-01). This blocks deletion only.
