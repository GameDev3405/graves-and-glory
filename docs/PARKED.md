# Parked ideas

Ideas that were cut, deferred or not yet decided. Nothing here is deleted;
add to it, never remove from it. Date and source on each line.

## Gameplay
- Mounts for commanders (2026-10-17, Roblox comparison; waits for formation code to settle).
- Hit-stop on commander hits (2026-10-17 night session).
- Capture-the-banner mode (night session; now planned as "Banner" in the roadmap).
- Flank/rear/morale (night session; now planned in Phase 1).
- Daily seeds (night session).
- Ping wheel (night session).
- Control groups (night session).
- A fourth commander class (night session; superseded by six classes in the vision).
- Veteran squads keeping rank across a match (audit).
- Medal challenges per map and class as a gold sink (audit).
- Single-player campaign of linked battles (audit; now Phase 4).
- Duplicate rolls levelling units (Roblox comparison; now rank stars).
- Free-for-all with alliances and betrayal; war table with fog of war; land conquest (Kingdom & Army vision, 2026-10-09).
- Player classes Knight/Mage/Ranger/Healer/Tank (Kingdom & Army; replaced by the six-class vision).

## Content
- OGG sound variants (night session).
- More maps beyond five (audit: tune the existing ones first; roadmap: after Siege and Survival exist).
- Spectator mode, passwords on rooms, simultaneous battles per server (Codex 0.3.6 notes; Phase 5/6).

## Platform
- Melty listing 0.2.0: left live for now, not updated (2026-10-17 decision to drop Melty).
- Public matchmaking, Velocity proxy, moderation tools (Codex milestones 5 and 6; Phase 6).
- FancyMenu/Drippy loading screens (Codex research; native screens chosen instead).
- FTB Quests as a contract framework (Codex research; custom contracts chosen).

## Design sheets unused by code
- hooks, landmarkStyle, recruitmentView, terrainStyle, duplicateRefund, starterGold (HOW-TO-EDIT 0.3.x).
- Team-only glow for Mark Prey (Phase 2 plan; needs a client renderer, vanilla Glowing used instead).
- +5 shard campaign bonus objective (Phase 2 plan; no bonus objectives exist yet).
- In-world rendering of renown and banner cosmetics (Phase 2 plan; recorded only, rendering is Phase 3 art).
- Display-entity arrows with interpolation, and arrow arcs (2026-10-17, Phase 1 plan; no-clip arrows chosen as the smallest reliable visual).
- Shield raise range for a shieldwall order (2026-10-17, Phase 1 Task 4): a shieldwall now raises shields whenever the trooper has a target, not only within 6 blocks, so Hard bots can brace against archers; a rule tied to arrow flight time was left out as larger than the task.
- Fast-tick mixin for the bot simulation (run matches faster than real time) (2026-10-17, Phase 1 plan; real-time runs in parallel processes chosen instead).
- Muster lanes for fewer than four armies (2026-10-18, Phase 1 Task 5b): spawn rows are now centred for any army count, so three armies form at z -22, 0 and +22, on the gravel files between MapDressing's lanes; the four trampled lanes and lane standards still sit at (i-1.5)*22. Redraw them per army count when Phase 3 rebuilds the bases.

## Public network brainstorm (2026-10-18, parked by Kort: friends first)

Kort pictures up to a thousand concurrent players a year out and wants the server to pay for itself or better. Parked until Phase 5 (friends) holds. Notes to pick up then:
- Architecture: one Battle per server process means a proxy (Velocity) plus a pool of match servers behind one lobby; a 160-troop match is close to one CPU core. Measure matches-per-machine with the Task 5 bot simulation before sizing anything.
- Before the first stranger: fix the presentation-pack licences in the published build, move profiles to a database with backups, online-mode auth, mute/ban/report with a log and a Discord behind it.
- Hosting: home box sits behind Starlink CGNAT; public play needs a rented front door or rented game hosts; scale the match pool with demand.
- Money (Mojang commercial guidelines): sell access, supporter rank (name colour, title, camp decoration, reserved queue slot) and cosmetics only (alternate skins and overlays per House via the unit identity pipeline, banner frames, horn and impact effects, a paid cosmetic chapter track beside the free one). Never paid gold, troops, shards or random rolls. Store via Tebex-style delivery to the account database; needs an entitlement table. Break-even around several thousand monthly actives at 2-5% conversion.
- Build order: Phase 5 as planned, then a network phase tested with bots as stand-in players, then a small invite-only public opening.
