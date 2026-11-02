# Accepted decisions and durable evidence

2026-10-25: Kort authorized optimization proposal v2, including task-based model routing, one coordinator/two-helper trial, bounded correction rounds, autonomous reversible decisions, selected references and six-player target. This replaces older blanket model bans and mandatory xhigh art effort; quality and gameplay approval gates remain. Claude was confirmed idle for this optimization.

The complete approved proposal and original detailed checklist are in archive/optimization-proposal-v2.md and archive/optimization-original-checklist.md. Conflicts are resolved in favor of v2.

Visual references: docs/visual-references/reference-manifest.json identifies exact copies. Preserve warm parchment, dark wood, brass/gold frames, stitched faction banners, wax accents, deliberate layouts, distinct troop silhouettes and rank equipment. Do not add brass fittings to table wood. Small reference text is a defect to check, not a target. HUD reference is a baseline, not final approval.

Accepted prior decisions (full context in archived questions): troop skins and relief at 1 texel per model pixel, weapons at 2, hand-authored with the `art/hand` builders (Kort 2026-10-30, replacing the old 3-texel rule; the density-3 `tools/paint_house_troops.py` output is retired); simple Minecraft-style faces at 2 with four variants; existing art remains. Weapon stage 1 accepted as built; staff I head bigger. Tower wedges and Frostmarch snow/night glow accepted. HUD battle shots at 1280/1366 accepted at 8.5 with no new scale rule. Held groups never rout; Detach once/group; allied bot intentions and aid/watch requests approved in AI spec. Voice casting remains deferred. The unapproved growth/0.6.0 plan was withdrawn.

Historical evidence copied from local-only ledgers is under archive/local-decisions with original path/hash manifest. These reports preserve decisions and exact historical checks, but their old dispatch/model instructions are superseded and their tests do not certify a new source fingerprint.

Excalibur resource pack (Kort, 2026-10-28): the author, Maffhew, answered "Yes that's ok" in Discord to Kort's request to use it in the modpack (CC BY-NC-ND 3.0; Kort may add paid features later and the author's reply did not address that, so re-ask him before charging). This supersedes the 2026-10-18 no-third-party-resource-packs decision for Excalibur only. Fresh Animations is not wanted; own animations are planned. Credit the author in the pack listing.

Animation approach (Kort, 2026-10-28): hybrid. Code-authored clips for everyday states (mostly built: see docs/ANIMATION-PLAN.md), hand-keyed Blender clips only for signature attacks and the charge. First pass all six classes, core states; formation and charge second; build after SLICE-1 scoping.

SLICE-1 scoping answers (Kort, 2026-10-28; partly superseded by the approval entry below: slice map, adoption and AI authority are now decided; bot quality, art direction and world completion remain open): 3v3 humans on two teams with bots filling empty seats; one House per player, six classes, existing tiered troops, no new units. Campaign: first chapter cut to one short run. Competitive: one standard capture scenario on the slice battle map. Performance: measure a six-client baseline first, then set targets (working guess 60 fps, server tick keeps up). Test clients: Kort's PC plus friends' machines against tjepkema-server; headless bots never count as players. Animation: "all 6 classes" means the six troop classes (client-side code clips first); who keys the Blender clips is decided after the code clips are seen; pass-2 protocol data is asked again later. Excalibur: bundle it unmodified in the modpack (CC BY-NC-ND; credit Maffhew; get his written confirmation of redistribution if practical, and re-ask before any paid feature). Friends' build may ship with stopgap tier art for the 26 troops; the strict package gate stays for any public release. Version stays 0.5.x.

2026-10-28 repository cleanup (Kort approved): `main` is the current 0.5.0 development integration, promoted by fast-forward from `phase-1-solo-core`; `v0.4.0` tags the old `main`. Active lanes are `work/anim-pass-1`, `work/troop-models-house` and `work/war-table-troop`; merged and historical branches are removed from GitHub and their unique tips preserved under `archive/2026-10/*` tags. The War Table troop sheet is preserved as WIP, not merged. All 30 local worktrees and their run evidence are retained; no worktree or bulk run data was deleted. No GitHub Release was created.

2026-10-28: Kort skipped the six-player baseline (Phase 0.6) with "skip the 6 player game" and directed work to continue on the roadmap. The earlier 2026-11-07 target is withdrawn; no date is held. Phase 0.1 to 0.5 are complete; 0.7 remains open. Phase 1.1 is green on `work/war-table-rebase`; Phase 1.4 (harness Narrator parity) is done and checked.

2026-10-28: Kort approved docs/PLAN-TO-1.0.md and the seven Phase 0 decisions. (1) Slice battle map: Ironhold; Rivergate is the second map in Phase 4. (2) Map adoption: generated base plus edit overlay, with a full-snapshot fallback; the 2A.4 spike validates the mechanics but does not reopen the overlay-first approach. (3) AI build authority: agents may propose and apply plans in a locked region after a dry run; Kort approves the House, map or landmark before export; no region deletion, station change, spawn or capture-point move, or live-world overwrite without a backup and recorded undo. (4) Tier II and III: gear and trim progression on the same silhouette, plus a distinct helmet or weapon where designed; no full new silhouette per tier. (5) Campaign co-op: solo only in 1.0; co-op is post-1.0. (6) 1.0 distribution: Modrinth and Prism download plus the self-hosted server kit; no public matchmaking, accounts, moderation or paid service in 1.0. (7) Performance working targets: 60 fps on each six-player test machine, server MSPT p95 under 50 ms and map build under 60 seconds; confirm after the Phase 3 baseline. Also approved: the full-vision 1.0 scope and exclusions in section 0.2; the six-player network baseline runs before the War Table, House-model and animation merges; versioning is 0.5.x until the slice, 0.6.x for content-complete and 1.0.0 at release.

2026-11-01 answers (Kort, through AskUserQuestion; applied and tested on `integration/candidate-2026-11-01`, then merged into main by Kort's Q12 approval):

- **EULA (Q11):** accepted. Agents may write `eula.txt` (eula=true) in dev server run folders for bot-sim battles only. It is never committed.
- **Map adoption (Q15):**
  - `gg-mapforge-runs/1` and a first release bake are approved, and so is the settle rule.
  - Ironhold spawns move to the signed D1a values, Dawn -314 and Dusk 128, with the barracks moved to match.
- **Siege (Q13):**
  - Kort chose a shorter clock as the first knob.
  - Gate towers get stairs and bots climb them, so defender archers can man the towers.
  - Sims put the clock at 180 s, giving a 30% attacker win rate (6/20). On 2026-11-01 Kort approved trying 200 s to centre it.
- **Survival (Q14):**
  - The draft values are approved.
  - Allied bot banners count toward wave size.
  - The next wave rises early after a clear.
- **UI (Q16, Q18, Q19):**
  - The mode block and the Battle/Troops/Chapter tabs are approved.
  - A crowded Troops page pages at 10 rows instead of truncating.
  - How-to lines are capped at two parts per side.
  - The focus ring and disabled veil are fine.
  - The banner key default is N.
- **Audio (Q17):**
  - Use synthesised cues first, falling back to permissively licensed and credited free sounds.
  - Refusals play a deny cue.
  - The doubled back click is dropped.
- **QA (Q20):**
  - A stopped or crashed match is void: no rewards, no losses, and armies return to camp.
  - On disconnect, the army waits 60 s, then a bot takes command.
  - `/warp` is ops only.
- **Saves (Q21):**
  - All writes are atomic.
  - A bad file falls back to `.bak`, then to defaults, with a warning, and the bad file is kept as `.corrupt`.
- **Package (Q22):**
  - Keep minimum version ranges, plus a Fabric API floor (>=0.92.2).
  - Excalibur gets this credit: "Excalibur resource pack by Maffhew, used with permission, unmodified", plus its licence.
- **Animation (Q23):**
  - There will be a third round including the art fixes.
  - On 2026-11-01 Kort said "Show me first", so the round-3 merge waits until he has seen it.
  - Kort wants a frame-by-frame editor so he can edit animations himself.
- **Art (Q9):**
  - Agents push every troop to grade 9, then Kort reviews.
  - After four passes the grades were 8.5–8.7, and on 2026-11-01 Kort chose to review them now.
- **Other answers:**
  - Traits (Q2): B–F are decided after measurement A.
  - Briefings for battles 2–6 are agent-drafted and Kort edits them.
  - SLICE-02: agents propose 2–3 lobby and map looks.
  - SLICE-03: agents propose bot quality targets for Kort's sign-off.
- **Map engine:** Kort approved the design (`docs/superpowers/specs/2026-11-01-map-engine-design.md`, branch `docs/map-engine-design`) and chose subagent-driven building of slice 1.
