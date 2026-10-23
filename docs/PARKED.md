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

## Bot balance leftovers (2026-10-18, Phase 1 Tasks 5b-5e)
- Blight borrows vanilla indirect_magic to pass shields, so it also bypasses armour; the Phase 2 gravesandglory:troop damage type is where blight gets bypasses_shield without bypasses_armor.
- Spawn-row centring and the Dusk mirror (GravesAndGlory.reinforce) are inline; a pure BattleMath row-offset helper would let BattleMathCheck pin the half-turn mirror.
- Tier ladder (2026-10-18): Hard ships without flanking (9/20 vs Normal, level); Easy's edge over Normal (13/20) is unexplained by charge ratio or retreat/rally (decisionTicks 160 untested); the Dawn seat wins 75-85% of every batch. Next steps when the Phase 3 maps land: test decisionTicks, then re-run the ladder on the rebuilt maps; consider a Hard-only point picker that holds but does not flank. Data in docs/QUESTIONS-FOR-KORT.md problem 5 follow-up.

## The Long March leftovers (2026-10-18, Phase 1 Task 6 review)
- campaignBattles.size is not honoured: a Long March battle is seated as practice, so it is one commander against one bot whatever the row says.
- campaignBattles.difficulty is not checked by preflight; a tier that is not a botCommand id plays as Normal without a word.
- campaignBattles.map is not checked by preflight; a bad index only stops the battle in game, with a notice to the player.
- Three primary buttons on the camp home (THE LONG MARCH / BATTLE 1, OPEN RECRUITMENT, PARTY WITH FRIENDS); whether the other two step down while battle 1 is unwon is left for the UI pass.
- MVP exact tie on equal non-zero kills and seconds crowns each viewer's own first row (2026-10-18, Phase 1 Task 7); a viewer-independent tiebreak (owner id) if it is ever noticed.
- Banner mode: supply-wagon escort variant (2026-10-18, Phase 4 spec question 1). Kort: parked for now, to be revisited after the carry variant ships.

## War Table home leftovers (2026-10-18, UI Task 5)
- A canvas smaller than the 960x540 page (the default 854x480 window, whose canvas origin is -53,-30) crops the War Table page at the right: nothing is cropped at the left (the party column and the Camp plaque sit 3 px inside the edge), but the field card at page x 752 lands on canvas x 699 to 866, so its right 13 columns, its 4 px shadow and its right tack are off the canvas (ui-home pins the 13). The candle and the inkwell are left off there rather than cut. Accepted for UI Task 5 (controller, 2026-10-19).
- (Done in UI Task 8b-2.) UI Task 8 polish list: a compact layout for canvases under 960 wide: the right column (field card, Change, Ride, Last battle) shifts left and the sheets squeeze to 8 px gaps, so the first-launch window shows the whole card. When it lands, the 854 step of p1-ui-home-scales changes its assertion on purpose.
- (Done in UI Task 8b-2.) UI Task 8 polish list: canvases larger than 1280x720 (a maximized 1920x974 window at GUI 1, or 1920x1080 at GUI 1) show the 1280x720 table on a dark floor with a faint 4 px edge shadow (fix round 1 of Task 5; controller's ruling, 2026-10-19). Polish: export each page's oak field at 1920x1080 and centre-crop it on the canvas so the planks run to the window edge at every size, with the fittings staying in the 1280x720 layout; then the floor and the ring go.
- The home has no Visuals & sound entry; the camp guide and the side list of the camp pages not yet on the War Table keep it. Where it lives on the table is left for the page tasks.
- Suite order: ui-hud reads the camp chat lift at its tick 20 without first going back to camp, so a suite that ends mid-round (bots) run straight before it fails p1-ui-chat-lift (run ui-home-235258: "in camp the chat moved 44 GUI px"). The registered order never does that; making ui-hud return to camp first is left for the harness.
- The field card paints only Rivergate's map; any other battlefield (Kingsfield Crossroads in run ui-home-002147) shows its point seals on bare vellum, as UI Task 5 pins it. A painted table map per battlefield is left for an art pass.
- The bare field card has seals only when its map is the one built (CampaignMaps.select fills Spec.POINTS for the built map alone). On the Campaign sheet the card shows the next battle's map, so a next battle on another map without art would show bare vellum with no seals at all. Latent: the only campaign battle is on Rivergate, which has its painted map. Settled with the painted map per battlefield (or by working a map's points out without building it).
- ui-hud sets its window once before the five-point build and not again before p1-ui-seals-five, so a window maximized from outside during the build fails it (run ui-home-001049: the shot is 1920x974 and the seals sit on a 1920-wide canvas). Setting the window again before that check is left for the harness.

## War Table recruit leftovers (2026-10-19, UI Task 6)
- At 854x480 (canvas origin -53,-30) the recruit page crops at both sides, as home's does under the same ruling (controller, 2026-10-19): the muster roll's art (270 wide, drawn 15 px left of the roll's box at page x 56) starts at canvas x -12, so the left 12 columns of its rods' knobs are off the canvas, and the row mark's art (4 px left of the row) loses its left column; the back plaque and the roll's rows sit 3 px inside the left edge, and Enlist, the sealed letters and the Banner plaque end 3 px inside the right (canvas 851 of 854). The columns' foot (page 492) lands at canvas 462, 18 px above the window's foot. p1-ui-recruit-scales pins these numbers.
- At 854x480 the notice line (page 504 to 528, under the ladder's foot on its axis) lands at canvas 474 to 498, past the canvas's foot at 480: a refusal shows only the top 6 px of its first line until UI Task 8's compact layout lifts it (for example over the purse and rung, or the columns shortened by a row).
- At 1920x974 GUI 1 (origin 480,217) the rod and the House banners hang from the rail at canvas y 12 to 83 (the chosen banner's cloth is 70 tall) over the dark floor, since the planks start at y 127 (rail items take only the origin's x). With UI Task 8's per-page oak field run to the window edge they hang over planks; until then this is the floor ruling's look (controller, 2026-10-19).

## War Table compact page and big oak leftovers (2026-10-23, UI Task 8b-2)
- Under 854x480 the page crops again, now with no floor to mark the cut: a window snapped to half a 1366x768 screen (683x728) puts compactOrigin at x -85, so the back plaque and both side columns are cut. The compact page is 854x480; a narrower layout is left for a later pass (Task 8b-2 review, Minor 5).
- A GUI 1 canvas larger than 1920x1080 (2560x1440, 2560x1080, 4K) tiles the 1920x1080 oak: at 2560x1440 the seams fall at x 320 and 2240 and y 180 and 1260, where plank rows break and the vignette restarts. No check or shot covers it (the suite cannot open a window larger than the screen); it is on Kort's eye list for 1440p and 4K.
