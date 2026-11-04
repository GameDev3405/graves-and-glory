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

2026-10-29 animation rework (spec docs/superpowers/specs/2026-10-29-animation-rework-design.md, section 7.2): the Shieldwall formation is removed. Hold builds the shield wall: shield-tagged and spear-tagged troops brace on Hold, and the Hard bot's Shieldwall-against-archers tactic is gone. The formation cycle is Compact, Line, Wedge and Loose. Formation ids are not persisted, so no save migration is needed; the server ignores an unknown formation id. Archived docs keep their historical Shieldwall wording.

2026-10-31 animation rework, Phase 1 closed (branch `work/anim-rework`, not merged; the detailed record is the ledger `.superpowers/sdd/2026-10-29-animation-rework/progress.md` in the `bannerbound-anim-rework` worktree, which is local and not in git, rulings R1 to R49, and the Phase 1 review there approved it with no Critical or Important findings). What now holds in that branch:

- Formation: the Shieldwall removal above stands. On Hold, shield troops close into a wall on a 0.7-block cell (`shieldWallCell`), and shield and spear troops brace. A wall that loses a man closes up by sliding, at most one place per loss, and nobody walks through a neighbour. A braced man faces his Hold direction while he settles and on his last 1.5 blocks to his place; he turns only to strike an enemy in reach.
- Melee lands on the impact frame (Kort's option a, spec section 11.2). Each melee swing picks one of its family's `attackVariants` rows (never the same one twice in a row, drawn from a per-round seed), and its damage lands that row's `impactTicks` later. The blow misses if the attacker dies or is stunned first, or the target is dead or out of reach plus `meleeReachGrace` by then. Kort approved grace 1.0 on 2026-10-30 (it was 0.5). A Charge bonus is judged when the swing starts, so the wind-up never eats the Charge window. The cooldown still runs from the swing's start, so attack rates are unchanged. Preflight now requires each troop's cooldown to be at least its family's largest `impactTicks` + 11, so a swing that comes late on its beat still lands on the frame the client shows. Three new messages tell the client about it: `attack_v1`, `hit_v1` and `stance_v2` (stance_v1 is retired).
- Shields and the crawl: shields go up on Hold and under archer fire, lower briefly to strike, and never go up on Charge or Retreat. "Under fire" means the squad was shot at, or targeted by a ranged enemy in range, within the last `fireMemoryTicks` (60, 3 s). A shield troop that moves under fire slows to `crawlShare` (0.33) through a movement-speed modifier, so it stacks correctly with Howl, Rally and order speeds. A troop walking to its own Hold slot is not slowed.
- Arcs: bow shots lob, with a peak of 0.25 blocks per block of range up to 6 blocks; crossbows and the Ironclad ballista use 0.4 of that. Travel time is unchanged (`arcTravelScale` 1.0) and hit odds are still decided at fire time, so accuracy did not change. Arrows fly nose first and the visible arrow ends on its landing point, lingering 2 ticks.
- Corpses: a dead troop plays vanilla's 20-tick fall, lies still for `corpseLieTicks` 30 (1.5 s), fades out over `corpseFadeTicks` 40 (2 s), and is then removed. The spec asked for about 3 s lying; at 60 ticks the 100-v-100 death-wave load test failed its 15 % frame budget, so the lie time was lowered first, as the spec's own load-test remedy says (frame 1.140, server 1.054 at 30). This is a visible deviation from the spec for Kort's review; re-measure after the Task 13 performance work. A corpse cannot be hit, pushed or counted alive, gives no second death event, and is removed at once on round reset, map switch, unload and server stop; troops are never saved, so no save change was needed. Grave Bloom's Risen uses up the body it rises from. On the client, `BodyCache` draws a lying body from a frozen, cached pose without its features or shadow, and the body fades with an eased translucent fade on both the cached and normal draw paths.
- Pennants: off on all 30 troops (Kort through the coordinator, 2026-10-30). Back gear is fine; only flags, pennants and standards on a troop's back are banned. The same change set the Tidecallers accent to #2E6A8C and the Graveborn accent to #5B4378, with a lightened same-hue tint for text so it stays readable.
- No balance retune in this lane. The House imbalance measured after grace 1.0 was already there before the rework and is backlog HOUSE-BALANCE.

Open carries: Task 13 (wiring the engine) takes the fps-check rewrite, the tabard and mage-knee fixes, ignoring a dead troop's last stance, the attack de-duplication and catch-up window, the ranged loose trigger and the fade polish (overlapping fades, no fade on the Iris path). Task 14 takes the late tracker's restarted death clock. Task 16 takes the 45-degree bend shots and the wall-under-knockback and hop-facing checks. Task 16b takes the shield-rim z-fight that keeps `hl-corpse` red on purpose, and the seven depth checks (relief checks retire; grip, bow-nock and clipping checks are fixed on the new rig).

2026-10-31 corpse fade frame budget (ruling R55, Kort's choice through AskUserQuestion; branch `work/anim-rework`). The `hl-corpse-load` client gate is now an absolute budget: the fading bodies may add at most 0.75 ms to the client frame at the 100-v-100 death wave (lying bodies: mean 51.4, peak 90 at once). The gate is the median of the per-block ABBA differences (each block's B arm mean frame less its A arm's, over the ABBA x8 protocol), at most 0.75 ms (Kort's budget). A run whose mean frame is over 1.5 times its arm's median frame is a stall, and its block is re-run, at most 2 re-runs (R58). Result at 4b905ec: median 0.478 ms, a pass. The client frame ratio B/A is still logged, for information only; the server tick ratio gate (median B/A 1.10 or less) is unchanged. Why: after Task 13 the fade costs B 3.321 ms against A 2.605 ms (B-A 0.715 ms, ratio 1.280, over the old 1.15). Before the rework the same test measured A 3.574 ms and B 4.083 ms, so B is now cheaper than before the rework; the ratio failed because A got about 27% cheaper. The fade pass's overlap groups stay on.

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
  - On disconnect, the army waits 60 s, then a bot takes command.
  - `/warp` is ops only.
  - A stopped or crashed match was void. Superseded 2026-11-02, see below.
- **QA rulings (Kort, 2026-11-02):**
  - Stopped or crashed match (I7, I9): players get a partial reward by time played. It is the participation gold (completion gold, or practice gold in Practice) times the share of the round played, rounded down, with no win bonus. No shards, Renown, XP or match record; armies return to camp. A stop pays at once; a crash pays on the next start from a progress file written every 30 s (so up to 30 s of play can be lost).
  - `/warp` in build mode is ops only, permission level 2 or above (J3). Already true since 2026-11-01.
  - A disconnected player's slot is held for 2 minutes (H8). After that the reservation is released and the army stays with its bot as an ordinary bot seat that any joiner may take, the owner included. The 60 s bot takeover is unchanged.
  - The server kit keeps the Iris and Sodium notices and `iris-source.zip`. No change.
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

2026-11-02 bow loose jitter (ruling Q70, Kort direct; branch `work/anim-rework`). Bow troops' release timing varies a little per troop, so a volley is not lockstep. Each troop that draws ahead (the bow family) gets a fixed offset from its UUID (`MotionMath.seed`): the loose comes 0 to 3 ticks after its beat (`LOOSE_MAX` 3, mean 1.5), and the full-draw hold is stretched by 0 to that many ticks. The server spawns the arrow on the delayed tick and sends the clip start shifted by the same amount, so the arrow still leaves on the visible loose frame; the client derives the hold from the same seed, so no message changed. The cooldown still runs from the beat, so the fire rate, damage, arrow speed and arrow count are unchanged; only each troop's first shot of a fight is up to 3 ticks later. A delayed shot is dropped if the archer dies or is stunned, or the target dies or leaves, in those ticks. Marker times and clip lengths are unchanged.

2026-11-03 slower bow shot (Kort's choice through AskUserQuestion; branch `work/anim-rework`). The bow shot read as too fast, so both changed: the draw is slower and bow troops fire less often. `bow_direct` and `bow_volley` loose at 0.8 s instead of 0.5 s (`impactTicks` 10 to 16) and both clips run 1.8 s; the draw and full-draw markers stretch with them (0.352 and 0.576 s). Bow cooldowns rise by 25%: archer 40 to 50, longbow 45 to 56, bonebow 35 to 44, stalker 35 to 44, wm_hunter 36 to 45. Each shot fits its cooldown with at least 0.4 s of held aim to spare. This lowers bow damage per second by 20%, a deliberate balance change by Kort.
