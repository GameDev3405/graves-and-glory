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
- Four morale/bot numbers live in code, not sheets: rout home radius 10 (Morale.java), MELEE_THREAT_RANGE 8 and NEAR_RADIUS_FACTOR 2 (Campaign.java), and the +20 in Projectiles.CREDIT_WINDOW_TICKS. Move to `morale`/`botCommand` columns when tier tuning reopens in Phase 3 (final review 2026-10-18).

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
- Tier ladder (2026-10-18): Hard ships without flanking (9/20 vs Normal, level); Easy's edge over Normal (13/20) is unexplained by charge ratio or retreat/rally (decisionTicks 160 untested); the Dawn seat wins 65-85% of every batch (13 to 17 of 20). Next steps when the Phase 3 maps land: test decisionTicks, then re-run the ladder on the rebuilt maps; consider a Hard-only point picker that holds but does not flank. Data in docs/QUESTIONS-FOR-KORT.md problem 5 follow-up.

## The Long March leftovers (2026-10-18, Phase 1 Task 6 review)
- campaignBattles.size is not honoured: a Long March battle is seated as practice, so it is one commander against one bot whatever the row says.
- campaignBattles.difficulty is not checked by preflight; a tier that is not a botCommand id plays as Normal without a word.
- campaignBattles.map is not checked by preflight; a bad index only stops the battle in game, with a notice to the player.
- Three primary buttons on the camp home (THE LONG MARCH / BATTLE 1, OPEN RECRUITMENT, PARTY WITH FRIENDS); whether the other two step down while battle 1 is unwon is left for the UI pass.
- MVP exact tie on equal non-zero kills and seconds crowns each viewer's own first row (2026-10-18, Phase 1 Task 7); a viewer-independent tiebreak (owner id) if it is ever noticed.
- Banner mode: supply-wagon escort variant (2026-10-18, Phase 4 spec question 1). Kort: parked for now, to be revisited after the carry variant ships.

## Media shoot: stills, trailer, gameplay footage (2026-10-18, parked by Kort)

- Full shot list in `docs/media/SHOTLIST.md` (sets A to E: Steve watching with empty hands, face close-ups near and far, walking away, ten scenarios, trailer clips, gameplay footage, intro and outro plates). Kort: hold until units and maps are designed and polished. Resume after Phase 2 Task 5 (skins) and Phase 3 (built world). The title-screen pick from the six demos is deferred to the same point; the staging harness (TitleDemos + mixins) is committed.

## Phase 2 Task 1 leftovers (2026-10-18)
- houses.glyph is checked for its path format only, because the House glyph PNGs do not exist yet; once they land (the DarkUi.houseMark step), preflight should check each file exists, as it does for troop textures.
- Two rule numbers stay in CounterMath: the Entrench reduction doubles (x2) inside an owned capture radius, and a hit is frontal at 90 degrees or less (the vanilla shield half-plane). houseTraits has no column for the x2; one could be added if Kort wants it tunable.

## Phase 2 Task 2 leftovers (2026-10-18)
- House glyph art still missing: the four side marks (sun, moon, star, wave) are 7x7 pixel patterns drawn in DarkUi.sideMark until real glyph textures exist.
- Team cloth on the maps (MapDressing.cloth), the capture flags (Arena.flag: cyan wool for team 0, purple for team 1, so a crimson team's captured point flies cyan) and the camp wording still say Dawn and Dusk: the pad line "Dawn n / 4 | Dusk n / 4", the room buttons "Move party to Dawn/Dusk" and the battle report's point lines ("Dawn Ns / Dusk Ns"). They follow the side colours with the Task 3 HUD and the Phase 3 built world.
- Chosen recruits (recruitment.chosenCost, rarities.chosenPrice) are not offered yet: `unlock:` and `army:` are refused with a notice, and the camp recruit page keeps its layout with the House roll only until the Task 3 shrine.
- Classes are inert until Task 4: the six Task 4 class rows resolve and can be chosen, but no class bonus or active applies (the Phase 1 class bonuses left with the old rows); bots still take Spec.CLASSES[i%3].
- Wildmoot, Ironclad and Tidecallers troops name entity types the server cannot spawn yet (wolf, spider, polar bear, vindicator, pillager, iron golem, drowned, evoker, ravager, zombified piglin); the editor and the server keep them out of every banner until their Houses are fieldable.
- The tactical bar offers six groups (commandInterface.selection gained cavalry and siege); those two buttons select nothing until a fieldable House has cavalry or siege troops, and STATE still carries the three morale groups (layout decided at Task 3).
- Two "Can't keep up!" warnings at the start of the isolated client runs: the server falls 2.0 to 5.6 s (40 to 112 ticks) behind 5 to 10 s, and again 17 to 28 s, after the first player joins a fresh world, before the first check line. They were in all nine Phase 2 Task 2 runs and in the earlier `work/p1/ui-fix1-160849` and `ui-fix1-161500` runs, so they predate the Houses work, and no check runs inside them. Not investigated: the likely cause is the first build of the battlefield and the camp on a new world; a profile of the first minute (which tick work takes the 2 to 5 s) would show whether that build should spread over more ticks.

## Phase 2 Task 3 leftovers (2026-10-18)
- Done here: `tools/preflight.py` now checks that each House glyph names a PNG that exists under `textures/gui/house/` (the Task 1 leftover above); the five 16x16 icons are in.
- The recruit page still opens the Phase 1 reveal panel ("RECRUITMENT CONFIRMED") at a fixed place over the card grid for a few seconds after a roll, so it covers the cards it announces; a later pass could light the rolled card instead. The panel now draws in front of the card portraits (a model is drawn at z 50 and writes depth, so the panel sits at z 300, a locked card's dim at z 100 and the card text at z 150); before fix round 1 the models drew over it and a locked card's model was never dimmed. Chosen recruits (chosenCost, chosenPrice) are still not offered: the page has the House roll only.
- Combat.resolve multiplies by factors that are 1 until their tasks: Houses.traitFactor (the House tasks), Abilities.auraFactor and inMist (Oath Banner, Howl, Dread, Mist), Combat.commanderFactor (Task 4). Abilities.onHit does nothing, and only shield, mend, blight and the stances act: the stances reach the counters table through the troop's tags, so the Order Archer's `steady` multiplies by 1.2 while its group holds (counters row C18a; four archers in `gilded_normal`). Graveborn's `fearless` has no code path yet. The state_v8 tail is written in full: the commander id, ready and active-for ticks go out as "", 0 and 0 until Task 4.
- Blight is now an ordinary troop blow: armour reduces it (Phase 1's indirect-magic type ignored armour and shields; an iron-armoured Order Squire now takes about 5.7 of the Gravecaller's 7), and counters row C4 keeps the shield out of it.
- BattleEffects.strike and the biped pose now ask `ranged`, so the seven troops with range exactly 3.0 (Hollow King, Tusk Spear, Old Horn, Brassjaw, Trident Guard, Kraken-touched Brute, Admiral Vael) get the melee sweep; `range() < 3` sent them to the ranged flash.
- The recruit cards wrap a long name onto a second line and draw a wide model smaller; the Wildmoot Web Stalker is the widest portrait. A nicer fit would crop each portrait to a per-troop box in the sheet.
- A real-time balance run is 10 matches of about 6 minutes in each of two processes, an hour for the pair; the fast-tick mixin is still parked (Phase 1 Task 5).
- The banner editor and barracks troop cards (`ArmyCards.Recruit.renderButton`) dim an inactive card with a fill drawn after the model at the same z 0, so the model itself is not dimmed; the camp recruit page had the same fault and fix round 1 cured it there (`CampScreen.card`: the dim at z 100). The same cure would fit here.
