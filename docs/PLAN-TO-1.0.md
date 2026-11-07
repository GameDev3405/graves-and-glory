# Graves and Glory 1.0: completion plan

2026-10-28. Approved by Kort on 2026-10-28. Status: Phase 0 in progress. This is the execution plan for the full-vision 1.0, built from `ROADMAP-v2.md`, `VISION-AND-ROADMAP.md`, the Phase 4 plan, `PARKED.md`, the six-player test plan, the art pipeline and the current candidate. It does not replace the roadmap; it turns the roadmap into ordered work and closes the open M2 and M5 questions with Kort's answers of 2026-10-28.

**Changed by Kort 2026-11-07.** After an interview Kort fixed the 1.0 feature list (below) and moved the official hosted network, previously post-1.0, into 1.0. Changes: distribution (Modrinth and CurseForge both at launch), public matchmaking, moderation, a rented server and cosmetic monetisation are now in scope (section 0.1, "Hosted network", and the new Phase 7); section 0.2 and decision 7.6 are amended; Phase 6 depends on Phase 7. Scope is a fixed feature list, not a date: 1.0 ships when every item is in and works, with a soft internal target and no public date until a release candidate is clean. No other 1.0 content changes.

Policy still applies: `docs/WORKFLOW.md`. Gameplay, save, protocol and dependency changes need Kort. Nothing is published, pushed or merged without him. `Spec.java` is generated from `sheets/design.json`; never hand-edit it.

## 0. Locked scope

| Decision | Answer |
|---|---|
| Finish line | Full vision 1.0: five Houses, thirty troops, five maps, four modes, six-battle campaign, six-player multiplayer, full tier art, public-ready package and the official hosted network. A fixed feature list: 1.0 ships when every item is in and works |
| Deadline | None public. Dependency-ordered, quality-gated; a soft internal target only, and no public date until a release candidate is clean (Kort 2026-11-07). Estimates are planning ranges, not promises |
| Troop art | All thirty troops with complete tier I to III art, faces, portraits, weapons and PBR maps |
| Story | Six campaign battles with briefing cards, bonus objectives and a Pact. No storybook scenes, named cast, voice or staged sets in 1.0 |
| Modes | Domination, Siege, Survival and Convoy by 1.0. Siege is the first new mode and the slice mode |
| Maps | Two-map slice first, all five by 1.0. The builder server is the canonical editable world |
| Map authoring | AI builds the lobby and maps; Kort and one friend can join the persistent builder server and edit anything, any time |
| Platform | Minecraft 1.20.1, Fabric, Java 17. No loader change |
| Six players | First real 3v3 network baseline within 1 to 3 months; the content slice follows |
| 1.0 distribution | Modrinth and CurseForge both at launch (Kort 2026-11-07; no longer "CurseForge second"), plus Prism import and the self-host server kit (single server, file-based profiles) |
| Public matchmaking | In 1.0 (Kort 2026-11-07): the official hosted network, section 0.1 and Phase 7. Per-mode unranked queues for solo players and parties; ranked is post-1.0 |
| Versioning | 0.5.x until the slice, 0.6.x for content-complete, 1.0.0 at release |

### 0.1 What 1.0 must contain

- Five Houses, thirty troops, four rarities, evolve lines, rank stars, House traits, six commander classes with actives.
- Five authored battle maps plus the rebuilt camp lobby, all adopted from the builder worlds.
- Four modes: Domination, Siege, Survival, Convoy.
- Campaign chapter one: six battles, one per House plus a finale, using all four modes and all five maps, with briefings and one bonus objective per battle.
- The War Table, recruit, Armoury, banner, contracts, chapter and friends pages, restyled and visually accepted.
- Six-player 3v3 on a dedicated server: lobby, match, rewards, return, reconnect, restart and persistence.
- Full tier art for all thirty troops, full animation pass, complete audio cues, strict package gate, clean install.
- Troop art status (re-counted 2026-11-07 on main e771d5f1): all thirty troops have their own model (`models/troop/<id>.json`, listed in `index.json`) and tier I, II and III skins (`textures/entity/troop/<id>_s1..s3.png`), confirming Kort. The section 1 inventory (21 without a model) and the 21-model estimate and risk rows below describe 2026-10-28 and are superseded. Still open for 4B: the visual review (4B.7, 30/30 at 9/10) and the strict `build_skins --verify` gate.
- Distribution: Modrinth and CurseForge both at launch, plus the self-host server kit (single server, file-based profiles).
- The official hosted network (next subsection).

#### Hosted network (Kort 2026-11-07)

Moved from 0.2 into 1.0. Work is Phase 7.

- **Hosting.** Kort rents Kinetic Hosting (12 GB plan, about $16 a month, 4 splits; figures from kinetichosting.com, to confirm at checkout). Bought only after a local prototype (proxy, lobby and one battle server) runs on Kort's PC.
- **Architecture.** A Velocity proxy, one lobby server and N battle servers. Each battle server runs today's one-battle-per-server code (`Campaign.java`: one battle per server); there is no multi-arena refactor. Fabric servers behind Velocity need a forwarding mod (for example FabricProxy-Lite); to verify on 1.20.1 in the prototype.
- **Hub.** The Old Hold is the network hub: spawn, queues, parties and the store. The camp and War Table space live inside it.
- **Matchmaking.** Designed with Kort 2026-11-07 (`DECISIONS.md`, "Matchmaking and parties"). Ranked play is post-1.0.
  - Queues: per-mode and unranked, for Domination, Siege, Convoy (Frostmarch escort) and Survival. PvP queues offer 1v1, 2v2, 3v3 and 4v4. Survival is co-op: queue groups of 2, 3 or 4; solo Survival starts from the hub without queueing, with allied bots. The full 6-battle campaign is solo or party only, never in the public queue.
  - Entry: a Play menu from a hotbar item (mode, then size), a `/queue` command, and walk-up war banners, one per mode, in the Old Hold.
  - Matching: by banner power band, strict at first and wider with waiting (suggested 20 s strict, 40 s wider, then anyone). Bots fill empty seats after 60 s, at a difficulty that matches the match's power band. Queued battles pay full rewards even when most seats are bots.
  - Match found: a 10 s countdown, then an automatic move. Cancelling in the countdown counts as a leave.
  - Parties: up to 4 (`SocialHub.PARTY_LIMIT`). Commands `/party invite`, `accept`, `leave`, `kick` and a Party menu. Only the leader queues. Party power is the average of the members' banner power. A "Fill with strangers" toggle, default on; off, empty seats go to bots. A party may split across sides: each member picks Dawn or Dusk in the party menu, the leader's Queue button works only once each side fits the chosen size, and split friends fight each other. A party larger than the total seats of a size cannot queue that size (orchestrator's call, not yet Kort-approved). Party-versus-party private challenges (any mode, map and size) stay, through the private room and War Table lobby.
  - Leavers: a stand-in bot takes the side; the seat stays reserved until the battle ends and the leaver may rejoin at any time (this replaces the `MatchRules.HOLD_SECONDS` = 120 s release on the network). The queue never sends new players into a running battle. Repeat leavers get a short queue cooldown (about 5 min after 3 leaves in an hour).
  - After a battle: results, then back to the hub with the party kept. The leader gets a "Queue again" button for the same mode and size.
- **Progress.** One shared SQL database (MySQL) for the official network, read and written by all servers. The solo campaign keeps separate local progress and the two never mix. The self-host kit keeps file-based profiles.
- **Version check.** A server refuses a client with a mismatched mod version, with a clear message and the download link. Every player must install the mod (`fabric.mod.json` environment `"*"`).
- **Moderation.** Ban, kick, mute, a chat filter, staff permissions (LuckPerms) and a report command that logs to Discord. Staff: Kort as owner plus one or two trusted friends.
- **Monetisation.** A cosmetic store through Tebex (Kort sets up the account): banner designs, troop skins and colourways, titles and ranks, Old Hold lobby effects. Nothing that changes stats, per the Minecraft EULA and commercial usage guidelines. Excalibur: no store item may use or show Excalibur art (Kort 2026-11-07, instead of re-asking Maffhew; see `DECISIONS.md`).
- **Launch order.** Open beta on the network for the Discord, capped at about 50 players, on a release-candidate build; then 1.0 on Modrinth and CurseForge the same day the server opens, with listings pointing at the server.
- **Work order.** Build the network (proxy, lobby, queues, database) in parallel with Kort's map design; balance after the maps are done.
- **Measured facts.** One 3v3 bot battle: about 500 MB server heap, p95 tick 7 to 9.4 ms, map build about 25 s (about 85 s cold) on the integrated server (`docs/reports/perf-targets-2026-11-02.md`). Dedicated-server MSPT has never been measured, so the network capacity figures are unproven until task 7.2.

### 0.2 What 1.0 does not contain

- Storybook scenes, voice acting, named cast, staged sets, companions and camp story (Phase 4 tasks 13 to 27). These become the 1.1 story update if Kort still wants them.
- Ranked play and ladders, accounts beyond Minecraft sign-in, paid access or anything that changes stats, and more than one hosted region. Public matchmaking, moderation, the rented server and cosmetic monetisation moved into 1.0 on 2026-11-07 (section 0.1, Phase 7).
- Mounts, extra chapters, additional maps, media shoots and a console port.
- A Minecraft version change.

## 1. Baseline: what exists on 2026-10-28

`main` is the 0.5.0 development integration at `f1f6047` (approved-plan docs tip). The Phase 0 baseline re-verified source identity `c41a0e1` at `f1f6047` with the fast tier (22 checks) and the game tier (23 checks). The earlier `v0.5.0-dev.20261028` tag marks `c4f8771`. No release is published.

| Area | Current state | The gap to 1.0 |
|---|---|---|
| Troops and Houses | 30 troops, 5 Houses, rarities, stars, evolve data, House traits, 6 classes and actives, in-match levels and gear | Balance and art, not code |
| Troop art | On `main`, 6 Gilded troops have own models; 4 have full tier I to III skins, mage and Lord Marshal have tier I only; the other 24 have no own model. `work/troop-models-house` adds three more at stage 1 (self-scored 8/10, not gate-accepted), not merged | 21 troops still without their own model after the branch merges; every troop at tier II and III, with faces, portraits, PBR maps, weapons and veteran trims |
| Animation | 18 code clips per rig; pass 1 on six Order troops is WIP; 1 to 3.7 px clipping remains; no clip reached 9; bow revert unrendered | Pass 1 sign-off with recorded exceptions, pass 2 signature attacks and charge, formation march |
| Maps | Five generated maps plus generated camp; Phase 3 built world merged but parked at its screenshot gate; builder server runs six saved worlds | Adoption pipeline, AI build tool, camp redesign, final map detail, all five adopted |
| Modes | Domination only | Mode frame, Siege, Survival, Convoy, mode bots and gates |
| Campaign | One battle (`march-1`), seated as 1v1 practice; `size`, `difficulty` and `map` are not fully validated | Six battles, real sizes, bonus objectives, order lock, briefings, Pact |
| Multiplayer | Lobby, pads, parties, host hand-over, six-player rules (8 max, 4 per team) in code | Real 3v3 baseline, reconnect and restart proof |
| Balance | Gilded dominant; Graveborn 17.5 percent vs Gilded; Wildmoot and Tidecallers near zero; Dawn seat bias 65 to 85 percent | Structural fixes, then a full round robin on adopted maps and modes |
| Packaging | Candidate jar exists; strict gate refuses 26 troops with incomplete tier art; Excalibur not bundled | Full art, Excalibur, strict package, Modrinth and Prism, server kit |
| Testing | Fast, game smoke, Phase 1 and Phase 2 suites, bot sims | Mode, campaign, map-adoption and six-player suites |

The hard numbers are in `docs/releases/current-candidate.json`, `docs/PHASE-2-PLAYTEST.md` and `docs/reports/cleanup-2026-10-28.md`.

## 2. Critical path

Two things decide the 1.0 date: **art throughput** and **map adoption**. Everything else is code that can proceed in parallel.

```
Art pipeline + 21 new models, 5 tier upgrades ........ 5 to 7 months
Map Forge + camp + five map adoptions ................ 3 to 5 months
Mode frame -> Siege -> Survival -> Convoy ............ 3 to 4 months
Campaign data + six battles .......................... 6 to 8 weeks
Six-player baseline -> slice -> release .............. 3 sessions across the plan
Balance and hardening ................................ 2 to 3 months
```

The six-player baseline is deliberately early: it runs on the current candidate's generated Rivergate and Domination, with stopgap art, before the War Table, House-model and animation merges. It is task 1.8 and depends only on the Phase 0 baseline and the booked date, so it does not wait for art, animation or map work. That gives Kort the 1 to 3 month network answer before the content is finished.

## 3. Phases

Effort labels: S is under a week of focused work, M is one to two weeks, L is three to five weeks, XL is six or more weeks. "Agent" means an implementation agent; "Kort" means a decision or visual gate.

### Phase 0: Approve and freeze (1 week)

Goal: one agreed scope, one baseline, no moving target.

| ID | Task | State | Owner | Evidence |
|---|---|---|---|---|
| 0.1 | Approve this plan and the decisions in section 7 | Done 2026-10-28 | Kort | This file marked approved |
| 0.2 | Freeze the 1.0 scope and exclusions; no new feature enters without removing another | Done 2026-10-28 | Kort | `docs/BACKLOG.md` updated |
| 0.3 | Confirm the slice battle map (Ironhold, section 7.1) | Done 2026-10-28 | Kort | Decision recorded |
| 0.4 | Confirm the map adoption format and AI build authority (sections 7.2 and 7.3) | Done 2026-10-28 | Kort | Decision recorded |
| 0.5 | Re-run fast and game on `main`; record the baseline candidate | Done 2026-10-28 | Agent | `work/verify/20261028-231020-35520`, `work/verify/20261028-231257-41152`; candidate record |
| 0.6 | Book the six-player baseline date with five friends | Skipped 2026-10-28 | Kort | Kort: skip the six-player game for now; continue the roadmap |
| 0.7 | Make a single visible board: one row per phase task, owner, state, evidence link | Done 2026-10-29 | Agent | `docs/BOARD.md` |

Phase 0 status: 0.1 to 0.5 are complete. The plan and the seven decisions are approved and recorded in `docs/DECISIONS.md`; the baseline is fast 22/22 and game 23/23 at `f1f6047`, source identity `c41a0e1`. 0.6 is skipped for now by Kort, who directed work to continue on the roadmap. 0.7 is done (`docs/BOARD.md`); Phase 1.1 is green on `work/war-table-rebase`, and Phase 1.4 (harness Narrator parity) is done.

Exit: scope signed, baseline green, slice map and adoption format chosen. The six-player date (0.6) is skipped for now; the board (0.7) is done.

### Phase 1: Close 0.5.x and prove the network (6 to 10 weeks)

Goal: no stranded WIP, a clean 0.5.1 candidate, and a real six-player network baseline before content work.

| ID | Task | Effort | Owner | Depends on |
|---|---|---|---|---|
| 1.1 | War Table troop sheet: rebase `work/war-table-troop` (36 commits behind `main`, 1 ahead), resolve conflicts, compile, run layout checks, visual review, merge or park | L | Agent + Kort | 0.1 |
| 1.2 | Armoury restyle: War Table Task 10 look, implemented and accepted | M | Agent + Kort | 1.1 |
| 1.3 | Banner page and Field page polish: House chips, seats, map art, empty and error states | M | Agent | 1.1 |
| 1.4 | Harness parity: make `Phase2Checks` write or dismiss the Narrator screen (`onboardAccessibility:false`); carry the fix from `tools/verify.py` and `tools/launch_playtest_clients.py` into every worktree used for render runs | S | Agent | 0.1 |
| 1.5 | Animation pass 1: rebase `work/anim-pass-1` (14 behind, 6 ahead), finish the six Order troops, record the current 1 to 3.7 px clipping and the unrendered bow revert, re-render contact sheets, accept or document exceptions. Windup, bow carry and staff grip are Blender-keyed work in 4E.2, not code fixes | L | Agent + Kort | 0.1, 1.4 |
| 1.6 | Merge `work/troop-models-house`: rebase (14 behind, 5 ahead), expect conflicts in `TroopModel` and `Phase2Checks`, record the stage 1 baseline (8/10, known flaws), fix the cull and chest-panel checks, update Order notes, run turn and death suites | L | Agent | 0.1, 1.4 |
| 1.7 | Art production line: generalise `paint_house_troops.py` into a per-House generator; add tier II and III, faces, portraits, weapons and PBR verification | L | Agent | 1.6 |
| 1.8 | Six-player network baseline: current candidate, generated Rivergate, Domination, 3v3, three matches, rewards, return, reconnect, restart, persistence. Run before or alongside 1.1 to 1.7 | L | Kort + friends + Agent | 0.5, 0.6 |
| 1.9 | Fix blockers and majors from 1.8; no balance or art work in this fix round | M | Agent | 1.8 |
| 1.10 | Cut 0.5.1: fast, game, record, tag. No release | S | Agent | 1.1 to 1.7, 1.9 |

Branch facts at 2026-10-28: `work/war-table-troop` is 36 commits behind `main` and 1 ahead; `work/troop-models-house` and `work/anim-pass-1` are 14 behind, 5 and 6 ahead. The fast and game tiers have run on `main`'s candidate only, not on any lane tip. A lane's old report is not certification: rebase, compile and re-run the relevant tiers before merge.

Phase 1 status at 2026-10-29: 1.1 is rebased and green at `a423cc8` on `work/war-table-rebase` and awaits Kort's visual review; 1.4 is done and checked (22/22 Phase 2 checks in a fresh root with no `options.txt`, `docs/reports/phase-1-4-harness-narrator-2026-10-29.md`); 1.6 is rebased and green at `bf86e26` on `work/models-rebase` (fast 22/22, Gilded art 7/7, turn suite) and awaits Kort's visual review; 1.5 is rebased and green at `b023fe1` on `work/anim-rebase` (12/12 animation checks, 196 turn shots, clearance 0.6 to 3.7 px recorded); 1.8 is skipped by Kort for now. All three lanes now await Kort's review. Next: the 0.7 board, then 1.3.

Exit: all 0.5.x WIP is merged or explicitly parked; every lane is rebased and re-verified; 0.5.1 is green; six humans have played three matches; blockers and majors are closed; the network baseline report exists.

### Phase 2: Build the production lines (4 to 6 weeks)

Goal: make art and maps repeatable, and put the mode and campaign frame under the game. Three workstreams run in parallel.

#### 2A. Map Forge and the live builder server

| ID | Task | Effort | Owner | Evidence |
|---|---|---|---|---|
| 2A.1 | Harden the builder server: boot task, auto-restart, whitelist Kort plus one friend, ops, offsite backup copy, log rotation, health check, version stamp | M | Agent | `tools/builder-server`, runbook |
| 2A.2 | Add a build-only `mapforge` command surface to `BuildMode`: plan, apply, export, diff, verify, undo, backup. Shipped jar never contains it | L | Agent | Commands, unit checks |
| 2A.3 | Build `tools/mapforge/`: JSON and NBT operation format, structure placement, terrain stamping, region locks, dry-run and rollback | L | Agent | Generator, sample batch |
| 2A.4 | Adoption spike: choose generated base plus edit overlay versus full snapshot; prototype on a 64x64 camp region, measure file size, apply time and determinism | L | Agent + Kort | Spike report, decision |
| 2A.5 | Adoption pipeline: `export` from builder world, validate, bake into the mod, version stamp, mismatch refusal, load after generation, walk and route checks on the final blocks | XL | Agent | Round-trip test |
| 2A.6 | Rebuild the camp lobby: The Last Watch layout, hearth court, war table, House tents, armoury and training cluster, assembly bays, graveyard, War Gates; preserve every station function | XL | Agent + Kort | Blockout, walkthrough, screenshots |
| 2A.7 | Map standards: palette, road widths, capture circles, cover, sight lines, landmark rules, walk and route suites, bot path checks | M | Agent | `docs/MAP-STANDARDS.md` |
| 2A.8 | Server update loop: after each candidate, stop, back up, swap jar and map artifacts, restart, smoke join; never overwrite a world without a backup | M | Agent | Update script, runbook |

Exit: Kort can edit the camp on the live server; an agent can place a reviewed test structure; export and adoption round-trip on the camp; the camp blockout is accepted; the builder server survives a restart and a jar swap.

#### 2B. Art line at production speed

| ID | Task | Effort | Owner | Evidence |
|---|---|---|---|---|
| 2B.1 | Finish or explicitly defer the troop `.bbmodel` round-trip. Production uses the generator either way; do not block art on the editor | M | Agent + Kort | Decision, one converted troop |
| 2B.2 | Write the per-House art bible: Kort's design rules first (no back flags; identity only through shield, weapon, clothing, tabard, trim and plume accents; side cloth for player colour), then silhouette, palette, materials, parts, tier language, faces, portraits, weapon family and the 8/10 baseline with its known flaws | M | Kort + Agent | `docs/ART-STYLE-GUIDE.md` update |
| 2B.3 | Ironclad batch: Hearthguard is built; build Shieldbreaker, Arbalist, Ballista Crew, Mortar and Colossus at tier I. Run up to three art agents in separate worktrees inside the House; one visual gate per House | XL | Agent | Turn sheets, verify |
| 2B.4 | Ironclad tiers II and III, veteran trims, faces, portraits, weapons, PBR maps | L | Agent | Contact sheets, strict asset check |
| 2B.5 | Ironclad visual gate: 9 of 10 at 36 px, close-up, portrait and in-game turn | M | Kort | Accepted or fix list |

**Art bible baseline (2B.2).** The three House troops are stage 1, self-scored 8/10 against a 9 gate, and Kort judged them cheap next to high-detail CurseForge knight sets. The known flaws go in the guide, not only in a handoff: Hearthguard's folded arms hide the front glyph, Tusk's kilt floats in side views, Tideguard's cuirass blends into its mail, and shields are not accented. Only the gold side and stage 1 were rendered. The bar is layered plate with rivets and seams, chainmail under plate, patterned heraldic cloth, fur and leather trim, ornate crests and plumes, four to five tone worn metal, and a clear dark-to-light read.

**Kort's rules (2026-10-28).** No flags or banners on the back; pennant and standard parts stay deleted. Team and House identity comes only from accent colour on shield, weapon, clothing, tabard, trim and plume: ember for Ironclad, ochre for Wildmoot, sea blue for Tidecallers, gold for Gilded, pale violet for Graveborn. Side cloth carries the player-side colour; fixed paint carries the House colour.

Exit: one full House is art-complete; a troop goes from design to accepted in under two weeks; the generator, not hand-edited JSON, is the production source.

#### 2C. Mode and campaign frame

| ID | Task | Effort | Owner | Evidence |
|---|---|---|---|---|
| 2C.1 | `ModeMath`, `BattleMode`, `Modes`, `DominationMode`: move Domination behind the mode frame without changing its rules | L | Agent | `ModeMathCheck`, all Phase 1 suites |
| 2C.2 | Mode feed lines, ledger and briefing how-to lines | M | Agent | `p4-feed-fit` |
| 2C.3 | Campaign schema 3, `MarchMath`, battle order, cleared state, bonus trackers | L | Agent | `MarchMathCheck`, migration check |
| 2C.4 | Campaign battles as real battles: `size`, `difficulty` and `map` honoured and preflight-validated; fixed banners; no practice seating | L | Agent | `p4-march-*`, bot runs |
| 2C.5 | War Table campaign card, briefing bonus line and Pact report | M | Agent | UI suites |
| 2C.6 | Retire the 1v1 assumption: campaign sizes, bot fill, rewards per seat | M | Agent | Campaign tests |

Exit: Domination runs through the mode frame unchanged; campaign data supports six battles with sizes; profile schema 3 migrates schema 1 and 2; all existing suites are green.

### Phase 3: Six-player slice (8 to 12 weeks)

Goal: two adopted maps, Siege, a real 3v3 loop and a dedicated server that survives a session. The slice is the first content milestone, not 1.0.

| ID | Task | Effort | Owner | Evidence |
|---|---|---|---|---|
| 3.1 | Adopt the camp and the slice battle map (Ironhold recommended; see 7.1) through the 2A pipeline | L | Agent | Walk, route, bot, capture checks |
| 3.2 | Siege world pieces: intact gate, ward, mounds, gate target, wards, stages, teardown after crash or map switch | XL | Agent | `p4-siege-*` |
| 3.3 | Siege bots: stage plans, gate priority, defend and attack plans; mode bot runs | L | Agent | `tools/mode_gate.py` report |
| 3.4 | Lobby for six: 3v3 seats, pads, party and invite, House and class choice, bot fill marked, ready countdown, host hand-over | L | Agent | `CampNetworkChecks` |
| 3.5 | Dedicated server kit: export, EULA, whitelist, port 25580, Aikar flags, backups, start and stop scripts, version gate | M | Agent + Kort | Kit on `tjepkema-server` |
| 3.6 | Performance baseline: six clients, two maps, MSPT, client fps, memory, bandwidth, map build time | M | Agent + Kort | Baseline report |
| 3.7 | Six-player slice test: Domination and Siege, rewards, return, reconnect, mid-match crash, restart, persistence | L | Kort + friends + Agent | Test report, defect list |
| 3.8 | Fix round: blockers and majors only; maximum two correction rounds | L | Agent | Green defect list |
| 3.9 | Visual gate at 1080p, 1440p, 4K and small window; no accepted exception without Kort | M | Kort | Screenshots, rubric |

Exit: six humans play Domination and Siege on two adopted maps; no blockers or majors; rewards and profiles persist across reconnect and restart; the dedicated server kit is reproducible; slice candidate is recorded and tagged.

### Phase 4: Content completion (20 to 30 weeks; optimistic at the low end)

Goal: all five maps, all four modes, six campaign battles, all thirty troops at full tier art, animation pass 2. Workstreams overlap; this is the long middle. The low end assumes two to three art agents in parallel, an adoption pipeline that already works, and a review queue that does not idle; otherwise this phase moves first.

#### 4A. Maps

| ID | Task | Effort | Owner | Evidence |
|---|---|---|---|---|
| 4A.1 | Adopt Rivergate, Frostmarch, Hollowwood and Kingsfield one at a time; each map passes walk, route, bot path, capture and visual checks before the next | XL | Agent | Per-map gate |
| 4A.2 | Finish landmarks, supply houses, roads, cover, sight lines and map dressing on every map; fix the parked clipping and road blockers | XL | Agent + Kort | Contact sheets |
| 4A.3 | Re-derive spawns, capture circles, routes and landmark rows after edits; no stale generated coordinates | L | Agent | Scan report, `LayoutMarker` |
| 4A.4 | Camp detail pass: House districts, banners, braziers, graveyard record, training yard, guide and Archivist | L | Agent | Walkthrough |
| 4A.5 | Map art: War Table field cards for all five maps, minimap colours, title stills | M | Agent | UI assets |
| 4A.6 | Map visual gate: built, not generated; nothing floats, clips or blocks a road, with the accepted exception list | M | Kort | Signed sheets |

#### 4B. Art

| ID | Task | Effort | Owner | Evidence |
|---|---|---|---|---|
| 4B.1 | Wildmoot completion: Tusk is built; build Hound, Hunter, Stalker, Seer and Old Horn at tier I, then all six at tiers II and III, with veteran trims. Ironclad is finished in Phase 2 | XL | Agent | Turn sheets |
| 4B.2 | Tidecallers batch: Tideguard is built; build Raider, Harpooner, Mist, Brute and Admiral, all tiers and faces | XL | Agent | Turn sheets |
| 4B.3 | Graveborn batch: six models, three tiers, faces, portraits, weapons, PBR | XL | Agent | Turn sheets |
| 4B.4 | Gilded completion: mage and Lord Marshal tier II and III, all six veteran and portrait checks | M | Agent | Turn sheets |
| 4B.5 | House identity set: recruiter tent banners, House marks, camp displays, card frames | L | Agent | Screenshots |
| 4B.6 | Art QA: `build_skins --verify`, PBR, glow, texture budget, team hue rule, 36 px readability | M | Agent | Verify log |
| 4B.7 | Final art gate: 30 of 30 troops, 9 of 10, no stopgap assets | L | Kort | Signed matrix |

#### 4C. Modes

| ID | Task | Effort | Owner | Evidence |
|---|---|---|---|---|
| 4C.1 | Survival: waves, shrine, director squad, wave economy, solo and co-op | XL | Agent | `p4-survival-*` |
| 4C.2 | Convoy (Frostmarch escort): supply wagon, road path, escort and attack roles, drops | XL | Agent | `p4-convoy-*` |
| 4C.3 | Skirmish and Practice picker, mode and map selection, bot fill per mode | M | Agent | UI suites |
| 4C.4 | Mode bot runs and mode gate; mode teardown, crash and map switch | L | Agent | `mode_gate.py` |
| 4C.5 | Mode HUD, feed, briefing and report lines for every mode | M | Agent | Visual gate |

#### 4D. Campaign

| ID | Task | Effort | Owner | Evidence |
|---|---|---|---|---|
| 4D.1 | Six campaign battles: Training Field (Rivergate, Domination), Iron Gate (Ironhold, Siege), Long Night (Hollowwood, Survival), Frozen Standard (Frostmarch, Convoy), Five Crossings (Kingsfield, Domination), Last Watch (Ironhold, mixed finale) | L | Agent + Kort | `campaignBattles` rows |
| 4D.2 | Briefing cards, reasons, lessons and one bonus objective per battle | M | Agent | Copy, UI |
| 4D.3 | Order lock, chapter completion, Pact grant, bonus shards and Renown | M | Agent | Campaign tests |
| 4D.4 | Campaign playthrough, rewards and persistence; no practice seating, no 1v1 default | L | Agent + Kort | Full chapter run |
| 4D.5 | Difficulty and map validation in preflight; a bad row fails the build, not the player | S | Agent | Preflight |

#### 4E. Animation

| ID | Task | Effort | Owner | Evidence |
|---|---|---|---|---|
| 4E.1 | Pass 1 for every rig, not only Order: biped, hound, spider, colossus | L | Agent | Contact sheets |
| 4E.2 | Pass 2: signature attacks per class, charge, formation march, no server timing change. This is the Blender-keyed work that fixes the sword windup arc, bow carry axes and staff grip offset recorded in 1.5; code clips alone cannot reach 9 | L | Agent + Kort | Contact sheets, protocol check |
| 4E.3 | Scene poses and death and hit polish; no regression in tells or left-hand mirroring | M | Agent | Phase 2 animation suite |

#### 4F. UI, audio and polish

| ID | Task | Effort | Owner | Evidence |
|---|---|---|---|---|
| 4F.1 | War Table pages complete: troop sheet, Armoury, banner, contracts, chapter, friends, field | M | Agent | UI gate |
| 4F.2 | Battle report and mode report final layout | M | Agent | Screenshots |
| 4F.3 | Title screen still, loading and menu polish | S | Kort + Agent | Screenshots |
| 4F.4 | Audio pass: mode cues, victory and defeat, UI confirms, mix check | M | Agent | Audio checklist |
| 4F.5 | Accessibility: colour-blind glyphs, text scale, key rebinding, focus and disabled states | M | Agent | UI checks |
| 4F.6 | 1440p and 4K layouts; no tiling seam or cropped page | M | Agent + Kort | Screenshots |

Exit: every 1.0 content item exists; feature-complete candidate; no known blocker or major; all art at full tiers; all maps adopted; all modes and six campaign battles playable.

### Phase 5: Balance, performance and hardening (8 to 12 weeks)

Goal: make the complete game fair, readable and stable, not just present.

| ID | Task | Effort | Owner | Evidence |
|---|---|---|---|---|
| 5.1 | Balance design review: Wildmoot and Tidecallers mechanics, harpoon, mist, hounds, brace, mixed-banner trait slots, Dawn seat bias. Fix design before numbers | L | Kort + Agent | Decision note |
| 5.2 | Bot round robin on adopted maps and all modes; 40 to 60 percent band per House; mixed banners report; seat symmetry | XL | Agent | Balance report |
| 5.3 | Tune values in `sheets/design.json` only; no inline numbers | L | Agent | Sheet diff, preflight |
| 5.4 | Economy and progression: 29 hour median to full roster, +12 percent cap, campaign rewards, Pact pace | M | Agent | `economy_model.py` |
| 5.5 | Performance: server MSPT with six players and 160 troops, map build time, client 60 fps target, memory, network | L | Agent + Kort | Baseline and target report |
| 5.6 | QA matrix: fresh install, server kit, migration v1 and v2 to v3, reconnect, restart, permissions, edge cases | L | Agent | QA checklist |
| 5.7 | Six-player release-candidate test: full content, three matches, all modes, defect triage | L | Kort + friends | Test report |
| 5.8 | Fix rounds; no bypass of the strict package or visual gates | L | Agent | Green list |

Exit: balance accepted, performance targets met, QA matrix green, six players have played the full content, no blockers or majors.

### Phase 6: 1.0 release (3 to 4 weeks)

| ID | Task | Effort | Owner | Evidence |
|---|---|---|---|---|
| 6.1 | Strict package: all art, no harness, dependency pins, Excalibur bundled unmodified with credit, licence checks | M | Agent | `verify.py release` |
| 6.2 | Modrinth `.mrpack`, Prism import, CurseForge export (both listings prepared for the same-day launch), server kit | M | Agent + Kort | Artifacts |
| 6.3 | Fresh-machine install test with no dev environment; a friend installs from the pack alone | M | Kort + friend | Install report |
| 6.4 | Release candidate record, `v1.0.0` tag, release notes, README, HANDOFF and BACKLOG final | S | Agent | Candidate record |
| 6.5 | Final six-player session on the release build; Kort's sign-off | M | Kort + friends | Signed checklist |
| 6.6 | Publish on Modrinth and CurseForge the same day the network opens (7.9), only on Kort's explicit go; listings point at the server | S | Kort | Release URLs |

Exit: a downloadable 1.0 that a new player can install, a server kit that a friend can run, an open official network, and a recorded acceptance run. Phase 6 starts after Phase 5 and after Phase 7 reaches the beta exit.

### Phase 7: Official hosted network (added by Kort 2026-11-07)

Goal: an official network where strangers queue for unranked matches, with progress in a shared database, moderation and a cosmetic store. Phase 7 runs in parallel with Kort's map design and Phases 2 to 4 (starting after the Phase 1 close), and finishes before task 6.6. Order is fixed: local prototype, then Kinetic, then beta, then launch. Nothing is bought, opened, published or charged without Kort's explicit go; gameplay, save and protocol changes still need Kort. Hosting facts are from kinetichosting.com and are to be confirmed at checkout.

| ID | Task | Effort | Owner | Depends on |
|---|---|---|---|---|
| 7.1 | Local prototype: Velocity proxy, lobby server and one battle server on Kort's PC; test the Fabric forwarding mod (FabricProxy-Lite or equivalent) on 1.20.1; a client joins the lobby, is sent to the battle server and returns | L | Agent | 1.4 |
| 7.2 | Dedicated-server measurement: MSPT, heap and bandwidth of one battle server and of the lobby with players on a real dedicated server (the EULA is Kort's call, Q79); decide battle servers per 12 GB plan and the capacity cap | M | Agent + Kort | 7.1 |
| 7.3 | Matchmaking and parties (design in section 0.1 and `DECISIONS.md` 2026-11-07): per-mode unranked size queues (PvP 1v1 to 4v4, Survival groups of 2 to 4), Play menu, `/queue` and war banners; banner-power bands and 60 s bot fill; parties up to 4 with `/party` commands, a Party menu and Dawn/Dusk side picks; 10 s match-found countdown; allocate a free battle server; stand-in bot and reserved seat for leavers, leave cooldown; party kept on return to the hub with a Queue again button; private party challenges | L | Agent | 7.1, 3.4 |
| 7.4 | Shared database: MySQL schema and access layer for the official network profiles, rewards and cosmetics, read and written by every server; migrations and backups; the solo campaign stays on separate local progress and never mixes; the self-host kit keeps file profiles | L | Agent | 7.1 |
| 7.5 | Old Hold as hub: spawn, queue pads, party and store areas; the camp and War Table space inside it; version check with a clear message and the download link | L | Agent + Kort | 7.3, 2A.6 |
| 7.6 | Moderation: ban, kick, mute, chat filter, LuckPerms staff roles (Kort plus one or two friends), a report command logging to Discord, an audit log | M | Agent + Kort | 7.1 |
| 7.7 | Cosmetic store: Tebex account (Kort), delivery of banner designs, troop skins and colourways, titles and ranks, Old Hold lobby effects; cosmetics change no stats; EULA and commercial-guideline review; no store item uses or shows Excalibur art | L | Agent + Kort | 7.4 |
| 7.8 | Rent Kinetic Hosting (12 GB, 4 splits), deploy the network, backups, restart and health checks, an operations runbook | M | Kort + Agent | 7.1 to 7.4, 7.6 |
| 7.9 | Open beta for the Discord, capped at about 50 players, on a release-candidate build; collect defects; then launch: 1.0 on Modrinth and CurseForge the same day the server opens | M | Kort + friends | 7.8, Phase 5, 6.1 to 6.5 |

Acceptance checks:

- 7.1: on Kort's PC, a real client logs in through the proxy, appears in the lobby, is moved to the battle server, plays a bot match to the end and returns to the lobby; the forwarding mod works or an alternative is recorded; no multi-arena change was needed.
- 7.2: a report with dedicated-server MSPT p95 under 50 ms at six players, heap per battle server, bandwidth per player and the measured number of battle servers one 12 GB plan holds; the 4-split plan is confirmed or the plan size changes. This gate precedes any purchase.
- 7.3: real clients queue solo and as a party of up to 4 in each of the four modes at the offered sizes (1v1 to 4v4; Survival 2 to 4); a party split across Dawn and Dusk queues only when each side fits the size and its members fight each other; a party larger than a size's total seats cannot queue it; a match found shows the 10 s countdown and moves players automatically, and cancelling counts as a leave; an empty seat fills with a bot after 60 s at the match's power band difficulty and the match pays full rewards; with "Fill with strangers" off, empty seats go to bots; a leaver is replaced by a stand-in bot, can rejoin after more than 120 s while the battle runs, and the queue never sends a new player into that battle; 3 leaves in an hour starts a queue cooldown of about 5 min; after the battle the party returns to the hub intact and the leader's Queue again repeats the mode and size; a challenge between two parties starts a private match; two battles run at once on two servers; a crashed battle server returns its players to the hub.
- 7.4: two servers read and write the same profile; a restart and a reconnect keep it; a restore from backup is tested; the local solo campaign file is untouched by network play and the reverse.
- 7.5: a client with a mismatched mod version is refused with the message and the download link; every existing camp station works in the hub.
- 7.6: each moderation command works from a staff account and is refused for a player; a report reaches Discord with the reporter, target and time; a muted player cannot chat on any server.
- 7.7: a Tebex test-mode purchase grants a cosmetic on every server; no cosmetic changes a stat (checked against the sheet); the store text passes the commercial-guideline review.
- 7.8: a fresh deploy from the runbook works; backups restore; the network survives a proxy restart and a battle-server crash.
- 7.9: a beta of about 50 players over several sessions with no blocker or major open; the version-matched release candidate is the build on Modrinth and CurseForge at launch; Kort's explicit go is recorded.

Exit: the official network runs on Kinetic with queues, shared progress, moderation and the store; the beta has no open blocker or major; Modrinth and CurseForge listings are ready to go live the same day the server opens.

## 4. Map Forge: how AI builds and how the server stays editable

This is the least proven part of the plan, so it gets its own design.

**What this changes.** `ROADMAP-v2.md` section 3 said a human builder owns maps and Claude no longer builds them. Kort's 2026-10-28 answer puts map building back with the AI. The builder server stays the human editing surface, and Kort plus one friend keep direct edit access at all times. This plan supersedes that roadmap line.

**Source of truth.** The builder server world is canonical. Kort and one friend can join at any time, in creative, and edit any block. The server keeps one saved world per map and per camp, already does 30-minute backups and `/backup`, and switches worlds with `/map`. No agent overwrites a world file while the server is running.

**How an agent builds.** Agents do not play the game with a bot. They write a `mapforge` plan: a JSON or NBT list of operations (place structure, fill, stamp terrain, set block, clear region) plus a region lock and a dry-run. A build-only command in `BuildMode` applies the plan in batches, reports changed blocks, and writes an undo snapshot. This keeps builds reviewable and reversible, and keeps the shipped jar free of build code.

**Surface-only maps (Kort, 2026-10-31).** "We only need the top layer for maps, blocks aren't breakable so what's underneath doesn't matter, just make sure the top layer looks good and go from there." Map plans build the visible surface only: the top layer plus a shallow fill wherever a gap or cliff face would show, never solid interiors. Details and the `gaps` check are in `tools/mapforge/README.md`. Already-built sections are not rebuilt to hollow them.

**How edits become the game.** A `/forge export` reads the difference between the builder world and the deterministic generated base, writes a versioned overlay with a hash, and records the author and time. The mod generates the base as it does today, then applies the overlay before the battle is marked ready. Walk, route, capture and bot checks run on the final blocks. The client map texture is built from the final colours. If the generator or the base hash changes, the overlay refuses to apply instead of corrupting a map.

**Why an overlay, not a full world.** The current generator already produces the terrain, roads and structures deterministically, and the gameplay tables depend on it. An overlay keeps that machinery, keeps file size bounded and makes AI edits reviewable as a diff. A full snapshot is the fallback if an overlay cannot carry a redesigned map; the 2A.4 spike decides with measurements.

**The edit loop.**

1. Kort or the friend edits on the builder server.
2. Before an AI batch, `/forge export` and `/backup`.
3. The agent's plan runs in a locked region, with dry-run and undo.
4. Kort reviews the changed region in game or by screenshots.
5. `/forge export` again; the overlay is committed to the repo with a map id and version.
6. The next candidate bakes the overlay; the server is updated to the same build.
7. If the server is behind the build, joining warns; adoption refuses a mismatched base.

**Server operations.** The scheduled task starts the builder at boot; the PC must be on for "always available". Add a health check and a restart-on-crash loop, an offsite copy of backups, and an update script that stops, backs up, swaps the jar and overlays, restarts and smoke-joins. The builder and the match server stay on separate ports and separate worlds.

## 5. Art production: the 30 troop matrix

The current production source is `tools/paint_house_troops.py`, which generated three House troops, their three skins, veteran trim, faces and nine weapon tiers. That is the fastest proven path. The `.bbmodel` round-trip is approved in the pipeline v2 design but is not done for troops; it is useful for hand editing, not required to paint.

Per troop, "complete" means:

- own rig and silhouette parts, with a readable 36 px shape and a House silhouette;
- tier I, II and III skins with a visible gear progression, not only a recolour;
- veteran trim, face patch and portrait;
- weapon family at tiers I to III, with PBR `_n` and `_s` maps and glow where designed;
- team accent parts that obey the team hue rule;
- turn, close-up, portrait and cull renders; 9 of 10 at Kort's gate.

The three built House troops are stage 1, self-scored 8/10 against a 9 gate, and not accepted. Kort's verdict is that the general design is right but the units look cheap next to high-detail CurseForge knight sets. The known flaws are Hearthguard's folded arms hiding the front glyph, Tusk's kilt floating in side views, Tideguard's cuirass blending into its mail, and unaccented shields. Only the gold side and stage 1 were rendered. Record these as the baseline in 2B.2; do not treat the branch as finished art.

Batch order, because three Houses already have one built troop:

1. Ironclad: Hearthguard built; build Shieldbreaker, Arbalist, Ballista Crew, Mortar, Colossus, then all tiers.
2. Wildmoot: Tusk built; build Hound, Hunter, Stalker, Seer, Old Horn, then all tiers.
3. Tidecallers: Tideguard built; build Raider, Harpooner, Mist, Brute, Admiral, then all tiers.
4. Graveborn: six from scratch; the undead silhouettes are the most distinct and should not reuse living armour.
5. Gilded: finish Mage and Lord Marshal tiers II and III, then re-check the four complete troops.

Each House batch ends with a visual gate. No batch starts before the previous House's gate, so a systematic art fault is caught once, not thirty times.

## 6. Mode and campaign mapping

The Phase 4 plan is 28 tasks. 1.0 needs tasks 1 to 12 and 28. Tasks 13 to 27 are the storybook, cast, sets, companions and camp story, and are post-1.0.

| 1.0 need | Phase 4 tasks |
|---|---|
| Mode rules, frame, Domination | 1, 2 |
| Mode feed and briefing lines | 3 |
| Siege world pieces and mode | 4, 5 |
| Survival | 6 |
| Convoy | 7 |
| Skirmish and Practice picker | 8 |
| Mode bots and gate | 9 |
| Campaign schema, order, bonuses, real sizes | 10, 11 |
| Campaign card, briefing, Pact report | 12 |
| Final regression | 28 |

Campaign battle table for 1.0, using all five maps and all four modes:

| # | Battle | Map | Mode | House introduced |
|---|---|---|---|---|
| 1 | The Training Field | Rivergate | Domination | Gilded Order |
| 2 | The Iron Gate | Ironhold | Siege | Ironclad |
| 3 | The Long Night | Hollowwood | Survival | Graveborn |
| 4 | The Frozen Standard | Frostmarch | Convoy | Wildmoot |
| 5 | The Five Crossings | Kingsfield | Domination | Tidecallers |
| 6 | The Last Watch | Ironhold | Siege, mixed | All Houses |

Each battle has a briefing card, one lesson line per new system, one bonus objective worth shards, and a fixed enemy banner. The campaign is solo in 1.0; co-op campaign is post-1.0. It is only ever played solo or as a party, never from the public queue (Kort 2026-11-07).

## 7. Approved decisions

Approved by Kort on 2026-10-28. These are decisions, not recommendations. The 2A.4 spike validates the adoption mechanics and can trigger the snapshot fallback; it does not reopen the overlay-first decision. The 7.7 numbers are working targets confirmed after the Phase 3 baseline.

### 7.1 Slice battle map

Decision: **Ironhold**, not Rivergate, for the first content slice. Siege is the must-have second mode, and Ironhold is its map. A slice on Ironhold tests Domination and Siege with one adoption, one route pass and one bot pass. Rivergate becomes the second map in Phase 4 and gives the campaign its opening battle.

### 7.2 Adoption format

Decision: **generated base plus edit overlay**, with a full-snapshot fallback. The 2A.4 spike measures both on the camp and can trigger the fallback; it does not reopen the overlay-first approach.

### 7.3 AI build authority

Decision: agents may propose and apply build plans in a locked region after a dry-run, but Kort approves the House, map or landmark before it is exported. Agents may not delete a region, change a station, move a spawn or capture point, or overwrite a live world without a backup and a recorded undo.

### 7.4 Tier II and III treatment

Decision: gear and trim progression on the same silhouette, plus a distinct helmet or weapon where the design calls for it. Full new silhouettes for every tier would multiply the art cost without adding much at 36 px.

### 7.5 Campaign co-op

Decision: solo only for 1.0. Co-op campaign needs separate reward, seat and save design, and the six-player slice already carries the multiplayer risk.

### 7.6 1.0 distribution

Decision: downloadable Modrinth and Prism release plus the self-hosted server kit. No public matchmaking, accounts, moderation or paid service in 1.0. Those are the public-service project, and they need their own security and hosting plan.

**Amended by Kort 2026-11-07:** superseded. 1.0 ships on Modrinth and CurseForge at once, the self-host kit stays, and the official hosted network (matchmaking, moderation, a rented server, cosmetic store) is in 1.0 as Phase 7. Ranked play stays post-1.0.

### 7.7 Performance target

Decision: working targets of 60 fps on each six-player test machine, server MSPT p95 under 50 ms and map build under 60 seconds. Confirm after the Phase 3 baseline; do not tune to a number before measuring.

## 8. Estimates

| Phase | Effort | Can start | Gate |
|---|---|---|---|
| 0. Approve and freeze | 1 week | now | Scope signed |
| 1. Close 0.5.x and network baseline | 6 to 10 weeks | now | Six humans, three matches, no blockers |
| 2. Production lines | 4 to 6 weeks | after 0 | Art line, map round-trip, mode frame |
| 3. Six-player slice | 8 to 12 weeks | after 2 | Two maps, Siege, 3v3, dedicated server |
| 4. Content completion | 20 to 30 weeks | during 3 | All maps, modes, campaign, art |
| 5. Balance and hardening | 8 to 12 weeks | after 4 | Balance, performance, QA green |
| 6. 1.0 release | 3 to 4 weeks | after 5 and 7 | Strict package, six-player sign-off |
| 7. Official hosted network | To be set after 7.2 | after 1; parallel with 2 to 4 | Local prototype, Kinetic, beta at about 50 players |

Phase 7 runs in parallel, so it adds no sequential time unless the beta defects do; set its range after the 7.1 prototype and the 7.2 measurement. The sum of phases 0 to 6 is roughly 50 to 75 weeks if every phase is sequential. With art, maps and modes running in parallel, the realistic range is **10 to 14 months of steady work**. The six-player baseline is the near-term deliverable: **1 to 2 months**. The slice is roughly **4 to 6 months**. No calendar date is promised; review bandwidth and art quality are the real limits.

## 9. Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| 21 new troop models plus tier II and III for 5 existing troops take longer than planned | Slips 1.0; blocks strict package | Start the Ironclad batch in Phase 2; reuse the generator; gate one House at a time; allow two to three art agents in parallel; stopgap art allowed only for the network baseline |
| Map adoption is harder than expected | Blocks slice and all maps | 2A.4 spike on the camp first; overlay fallback; keep generated maps playable until adoption passes |
| Builder server is a single home PC | Kort and friend cannot edit; work stops | Boot task, health check, automatic restart, offsite backups, documented manual start; treat it as a service |
| AI map edits corrupt a world or overwrite Kort's work | Lost work, distrust | Region locks, dry-run, undo snapshot, backup before every batch, no live file edits, review before export |
| Six-player test slips because friends are unavailable | No network proof | Book the date in Phase 0; use the current candidate and generated map; three sessions across the plan |
| Balance is structural, not numeric | 1.0 feels unfair | Design review before tuning; fix Wildmoot and Tidecallers mechanics; seat symmetry; human data at the slice |
| Scope creep returns (storybook, public service, extra maps) | 1.0 never ships | The exclusions in 0.2 are frozen; new ideas go to `PARKED.md` |
| Profile or protocol change breaks old camps | Player loss | Schema 3 migration tests for v1 and v2; version gate; keep the original file |
| Performance collapses with 160 troops and six players | Unplayable | Measure at the slice and at content-complete; Aikar flags; performance mods only after a measurement |
| Kort becomes the review bottleneck | Queues stall | Batch visual gates per House or map; threshold 9; agents fix before review |
| Fabric servers behind Velocity do not forward player identity on 1.20.1 | The network design fails | Test FabricProxy-Lite or an alternative in the 7.1 prototype before buying anything |
| Dedicated-server MSPT was never measured and a 12 GB plan holds fewer battle servers than hoped | Queues back up; cap lower than 50 | 7.2 measures before purchase; cap the beta; bots fill seats; upgrade the plan only after data |
| Strangers online: abuse, griefing, chat risk | Reputation, Discord load | 7.6 moderation, report command, chat filter, staff roster, beta capped at about 50 |
| Cosmetic store breaks the Minecraft EULA or the Excalibur licence | Takedown or dispute | Cosmetics only, no stat effects, 7.7 review, no Excalibur art in the store (open risk: the pack is CC BY-NC-ND and ships in the modpack of a monetised server) |
| Shared database loses or mixes profiles | Player loss | Separate solo and network progress, backups and restore test, migration checks (7.4) |

## 10. First ten actions

1. Approve this plan and the seven decisions in section 7.
2. Six-player baseline skipped for now; continue Phase 1.1 review and Phase 1.4 harness parity.
3. Run the six-player baseline on the current candidate and generated Rivergate.
4. Fix the harness Narrator screen, then rebase and merge the War Table troop sheet.
5. Rebase and merge the three House models; record their 8/10 baseline and known flaws.
6. Rebase and finish animation pass 1; record the clipping exceptions for 4E.2.
7. Harden the builder server and add the `/forge` spike.
8. Run the adoption spike on the camp and choose overlay or snapshot.
9. Start the Ironclad art batch with the existing generator.
10. Implement the mode frame and add campaign schema 3 with real battle sizes.

## Appendix A: Refresh the inventory

These commands produce the numbers this plan uses:

```bash
cd C:/Users/kortt/Melty/bannerbound
python - <<'PY'
import json, os, collections
d = json.load(open('sheets/design.json', encoding='utf-8'))
print('troops', len(d['troops']))
print(collections.Counter(t['house'] for t in d['troops']))
print('campaign battles', len(d['campaignBattles']))
print('maps', len(d['maps']))
PY
find art/troops -maxdepth 1 -name '*_s3.png' | wc -l
find src/main/resources/assets/gravesandglory/models/troop -maxdepth 1 -type f | wc -l
git ls-remote --heads origin
```

## Appendix B: Release checklist

- Fast tier passes on the release commit with a clean tree.
- Game tier passes, including Phase 1, 2, 3 and 4 suites.
- Strict package passes with no stopgap art and no harness classes.
- Excalibur bundled unmodified, credited, licence recorded.
- Modrinth `.mrpack` and Prism import tested on a clean machine.
- Server kit starts, accepts six players, backs up and restarts.
- Official network: Phase 7 acceptance checks pass; the beta has no open blocker or major; Modrinth and CurseForge listings point at the server and go live the same day it opens.
- Profile migration from schema 1 and 2 passes.
- Six-player release test passes with no blockers or majors.
- Candidate record, tag, release notes, README and HANDOFF agree.
- Kort's explicit publish approval is recorded.

## Appendix C: What moves after 1.0

- Storybook: Phase 4 tasks 13 to 27, voice, cast, sets, companions, camp story.
- Public service beyond the 1.0 network (matchmaking, moderation, hosting and the cosmetic store moved into 1.0 on 2026-11-07): ranked play and ladders, more regions, larger capacity, paid access.
- Extra chapters, more maps, mounts, media and a platform port.
