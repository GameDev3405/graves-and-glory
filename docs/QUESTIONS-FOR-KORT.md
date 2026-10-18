# Questions and problems for Kort

Written while working autonomously on 2026-10-17. Newest at the bottom.
Each item says what was decided in the meantime so work could continue.

## Open questions

(none; the 2026-10-17 items are answered below)

## Answered 2026-10-17

- Phase gates: tester subagent reviews screenshots; Kort playtests the builds listed below when he wants.
- Houses: trait tiers by banner slots 6/10/14; migrated Sunweaver/Gravecaller (and Oracle/Wraith if unlocked) refunded 300 gold each, not kept; spiders keep climbing in Siege; Legendary names approved; boar-kin Wildmoot approved; 29-hour chase accepted; commander active on H; no retro Founders tier-20 Rare; Mark Prey uses the vanilla glow.
- Phase 2 planner's other calls (classes split by House batch, later Houses hidden until their batch, Ballista evolve trimmed to the cap) stand.

## Problems hit

1. **Kingsfield (map 4) crashes the client when the lobby switches to it**
   (`ArrayIndexOutOfBoundsException` in `GravesAndGloryClient.map`: the minimap
   draws 5 points while the contested/progress arrays still hold 3 until the
   next state packet). Found 2026-10-17 by the harness-fix run. Decision: fix it
   inside Phase 1 (regression task) and add a check for it; until then do not
   pick Kingsfield from the lobby.
   Fixed on phase-1-solo-core (commit 6ecbfb0); in-game verification in the regression task.
2. **Two campaign-suite checks were already failing before Phase 1:**
   `solo-pad-3v3` and `roadmap-suite`. Left as-is for now; Phase 1 Task 9
   either fixes them or records why they are stale.

3. **Bot balance misses two of three acceptance targets** (Task 5 sim, 40 matches,
   2026-10-17 23:00). Normal beats Easy 9/10 (pass). Hard beats Normal only 10/20
   (needs 60%). Normal vs Normal: Dawn wins 8/10 (needs 35-65%), and across every
   pairing Dawn won 30/32 on the 3-point maps while Dusk won 5/8 on Kingsfield.
   So the seat decides more than the tier. Decision: a follow-up task (5b) first
   finds the seat bias (suspects: Dawn's base holds the world spawn so its chunks
   always tick; 3-squad spawn rows are centred for 4; Dawn shields vs Dusk kit),
   then makes Hard's flank picker leave a garrison on held points, then reruns.
   Question for you: if Hard still cannot reach 60% with flanking on, is it fine
   to turn `hard.flankLeastDefended` off (Hard keeps faster decisions, timed
   charges and shieldwalls, but picks points like Normal)? My recommendation:
   yes, flanking is worth less than a Hard that actually wins.

4. **Bot balance, round two (Task 5b, 2026-10-18 03:00).** The seat bias is now
   explained: Dawn knights raise shields in block formation and stop frontal
   hits, arrows and blight; revenants had no shield, and husks carry 2 natural
   armour where skeletons carry 0. With equal kits the Dawn seat won 9/20. The
   agent gave revenants a shield to even it, which contradicts the unit identity
   spec you approved (Graveborn carry no shields), so I reverted that and used
   the invisible fix instead: Dusk troop health +10% (revenant 33, bone archer
   22, gravecaller 20), measured at 50/50 in a 10-round test. Also fixed: the
   3-army spawn rows gave one side a 10-13 block head start; Normal's lane rule
   did not mirror on Kingsfield (Dusk won 16/18 there). Reruns are in progress
   (Task 5c; later the offset went to 20% in 5d and was halved in 5e to 32/21/19;
   the Health number on recruit and army cards shows it, the model does not).
   Hard still sits at 45% against Normal; the flanking question in
   item 3 stands.
   Update 03:40: the health offset did nothing (Dawn 10/10, knights killing
   revenants 3.3 to 1, same as before). The real cause is a mechanic: knights
   in block formation raise the shield whenever an enemy is within 6 blocks and
   keep swinging while blocking, which a player can never do, so every melee is
   a free full frontal block. Task 5d: in block formation the shield is up only
   on the approach and drops while the knight is in reach and swinging
   (Shieldwall keeps the full brace); and blight now ignores shields as your
   unit identity spec says. Reruns follow. Note for Phase 2: when the Risen
   becomes a zombie it gains 2 natural armour, so that health offset must go then (it is +5-7% now: revenant 32, bone ranger 21, gravecaller 19, after Task 5e).

5. **Bot tiers come out upside down (Tasks 5d/5e, 2026-10-18 08:00).** After the
   shield fix the sims say: Easy beats Normal 13/20, Normal beats Hard 13/20, and
   the Dawn seat wins 15/20 Normal mirrors (Dusk loses whichever tier it plays).
   Hard wins 4/4 on Kingsfield but 3/16 on the three-point maps, so its
   flank-least-defended picker loses to plain lanes when there are only three
   points; one Hard round ended 153-0 with its army never reaching a fight.
   Every tier knob moves the same way (faster decisions, earlier charges, later
   retreats from Easy to Hard), so the sims cannot say which one is backwards.
   Decision: no more kit or mechanic tuning in Phase 1; two sheet-only
   single-knob experiments are running from the runs worktree (Hard with
   flanking off; Normal with Easy's charge ratio) and their numbers will be
   appended here. My recommendation when you are back: turn Hard's flanking
   off (your item 3), make Normal charge like Easy if the experiment says
   charging loses, and accept a 60-70% Dawn seat until the maps are rebuilt in
   Phase 3 (the village around point A sits on Dawn's side on every map).

## Builds to try

- 0.3.7 (master e78b81b): camp, pads, parties, rooms, contracts verified by
  automated checks in a real client. Import dist/graves-and-glory-0.3.7.mrpack
  (rebuilt 2026-10-17 22:18, Melty removed, archive verified) into Prism.
  Pre-Phase-1 game: no projectiles, morale or bot tiers yet.

## Decisions from Kort 2026-10-17 (UI/HUD round 2)

- Direction chosen: the War Table screens (UI A) and the Iron Ledger group cards (HUD B). Polish only that family; no more three-way reworks.
- Unit portraits show the actual soldier (an in-game model render), not pixel art. Placeholder pixel busts stay until the unit identity pass below delivers each unit's look.
- New track: a unit identity pass. An agent examines every unit and designs each one deliberately with a distinct look and a distinct purpose; the agreed look then feeds the UI portraits. Spec: docs/superpowers/specs/2026-10-17-unit-identity.md (in progress).
- Hunger is removed: no hunger bar and no eating mid-battle. Something else takes that slot later. Goes into the UI implementation plan (hide the bar client-side, pin food level server-side).
- Pixel art detail goes way up on the chosen screens; no corner cutting.

## Unit identity spec questions (2026-10-17, docs/superpowers/specs/2026-10-17-unit-identity.md)

Answered by Kort 2026-10-18: all five as recommended (1 yes, 2 allow, 3 brace on Charge, 4 yes, 5 yes).
1. Troops never show gold or diamond armour (gold reads as the gold side, diamond as teal); golden helmets become chainmail, diamond weapons iron, Aldric's diamond chest netherite. [Yes]
2. Painted shields and CustomModelData weapons (longbow, scythe, boar spear, sun staff): one sheet column and one line each in Battle.spawn and ArmyCards.portrait, or EMF-only fallbacks? [Allow the column; it is the cheaper path to distinct silhouettes]
3. Tusk Spear and Trident Guard share the same brace. Split by job only, or make Tusk Spear brace on Charge instead of Hold? [Brace on Charge; a stat-identical pair is what the pass exists to remove]
4. One large team-colour cloth per Legendary (Aldric's standard, Hollow King's cloak, Old Horn's antler banner, Brassjaw's belt banner, Vael's coat lining)? [Yes]
5. Vanilla posing overrides the aim mixin, so husks and boar-kin raise zombie arms in combat. Fix the pose before the Gilded art is made? [Yes, in Phase 2 Task 5 (skin pipeline spike)]

## UI round 3 feedback from Kort (2026-10-18)

- v3 War Table and Iron Ledger: "getting better but not quite there yet." Double or triple the pixel art detail (map card named as the example). v4 in progress.
- Mode cards get a small one-line subtitle: Practice (train against bots), Skirmish (one battle, your rules), Campaign (the story, battle by battle).
- HUD gets a battle event feed on the right: troop losses, a fellow commander down, points taken or lost, routs and regroups, reinforcements; colour-coded by kind with a glyph per kind so colour is never alone. Replaces the notice slip above the group cards.
- Title screen: current one is "absolutely depressing". Direction chosen: "The Hill" (real shaders-on battlefield panorama, pixel-art cliff and commander with banner in the foreground, text-column buttons bottom-left, "Ride to camp" primary). Mockups in progress.

## Phase 3 built-world spec questions (2026-10-18, docs/superpowers/specs/2026-10-17-built-world-design.md)

Recommended answer in brackets (planner and controller agree). Work follows the recommendation until Kort says otherwise; nothing in Phase 3 is built before Phase 1 merges.
1. Graveborn recruit at the graveyard shrine, not a fifth tent. [Yes]
2. All six troops of each House stand as live displays in camp, showing LOCKED or the player's stars. [Yes]
3. The training yard teaches with lesson boards plus a START for campaign battle 1, not a separate in-camp drill. [Yes]
4. Replace every generic battlefield building except bases and bridges, with a shared kit plus signature pieces per map. [Yes]
5. Existing worlds rebuild the camp and the maps by themselves on first load of the new build. [Yes]
6. Leave the bases' barracks and store road clips for Phase 4, recorded in PARKED. [Yes]
Risk the planner named: agent-built pieces can look boxy; per-piece studio screenshots are the early catch, and the final gate is Kort's screenshot review.

## UI spec questions (2026-10-18, docs/superpowers/specs/2026-10-18-ui-design.md)

Recommended answer in brackets. The plan (docs/superpowers/plans/2026-10-18-ui-plan.md, 8 tasks) starts only after you have graded v4.1 and the title and after Phase 1 Tasks 6 and 7 land; it follows the recommendations until you say otherwise.
1. Our screens size themselves from the window (2x at 1080p) and ignore a larger GUI Scale so the pixel art stays sharp; hearts and hotbar keep your GUI Scale. [Yes; forcing GUI Scale 2 on everything would shrink vanilla text for people who need it bigger]
2. Night title by the computer's clock (19:00 to 06:00), or day only for now? [By the clock; the night painting exists and it costs one capture]
3. Recruit banners say "Dawn Covenant" and "Dusk Legion" until Phase 2, or Gilded Order and Graveborn now? [Keep the faction names until the Houses exist, so nothing on screen promises what is not there]
4. The minimap leaves the battle HUD; M opens the full map. [Yes; it is what the approved Iron Ledger mock shows]
5. With food pinned full, players heal slowly (1 heart every 8 s). Keep, or no natural healing? [Keep; without it a commander who survives a fight never recovers, and Mend is Dawn-only]

## Kort's answers 2026-10-18 (decision board, morning)

- Mockups: War Table home map is too cluttered after the detail pass ("you can't tell what's what"); keep the detail but zoom it out. Recruit table approved. Title "The Hill" day and night: the pixel art is good but is not Minecraft; make it blocky, and put a Steve-style commander with a diamond sword at the top of the hill. HUD Iron Ledger v4.1 not objected to. Home map and title go back to the designer.
- Bot tiers: Hard flanking off (open question 3: yes). Tier ladder parked until the Phase 3 maps; no more sim hours on it in Phase 1.
- UI spec questions 1 to 5 and the 8-task UI plan: all recommendations approved.
- Phase 3 built-world questions 1 to 6: all recommendations approved.

### Problem 5 follow-up: tier diagnostics finished (2026-10-18, sheet-only experiments, 20 matches each, 3v3, maps 0-4, 300 s)

| Variant vs Normal | Wins Dawn / Dusk / total of 20 | Per map 0-4 (of 4) | Score margin, seat-averaged |
|---|---|---|---|
| Baseline Hard (5e) | 5 / 2 / 7 | 1 2 0 0 4 | -18.7 |
| A: Hard, flankLeastDefended=false (now shipped) | 7 / 2 / 9 | 2 2 3 2 0 | +6.2 |
| Baseline Easy (5e) | 9 / 4 / 13 (+1 draw) | 2 4 3 2 2 | +18.0 |
| B: Easy = Normal + chargeRatio 2.5 | 8 / 1 / 9 | 2 0 3 2 2 | -3.6 |
| C: Easy = Normal + retreat 0.35, rally 0.7 | 7 / 1 / 8 | 2 1 2 2 1 | -16.0 |

Reading: the flank/garrison picker caused Hard's deficit on the three-point maps (3/16 -> 9/16 without it) and also its Kingsfield wins (4/4 -> 0/4); without it Hard is level with Normal, and the rest of Hard's row shows no measurable edge. No tested knob explains Easy beating Normal; decisionTicks 160 is the only Easy knob untested, and 13/20 sits inside noise around parity. The Dawn-seat bias (13 to 17 of 20 across the six batches) is larger than any tier effect. Knight-to-revenant kill ratio ~1.5 in every batch, so the effects come from where armies go, not melee. Parked until the Phase 3 maps, per Kort. Run roots: work/p1/botsim-5f-{A,B,C}-*.

## Phase 4 spec questions (2026-10-18, docs/superpowers/specs/2026-10-18-phase-4-modes-campaign-design.md, draft)

Recommended answer in brackets. Nothing in Phase 4 is planned or built until you have read the draft; the builder check may add a question or two.
1. Banner mode is "carry the enemy banner home"; the supply-wagon escort variant goes to PARKED. [Yes; the carry reuses touch pickup and the rout override, a wagon needs a moving vehicle and a road path]
2. Campaign battles 3 to 6 bring allied bot banners (Ironclad from battle 3, Wildmoot too in battle 6), so the chapter grows from 1v1 to 3v3. [Yes; battle 3 needs ballistas a new player does not own, and the growing host is the chapter's arc]
3. Each campaign battle has one fixed difficulty, no picker. [Yes; a picker triples tuning and bonus checks]
4. Skirmish "any banner" means you choose the enemy's House and always field your own banner. [Yes; Practice is where you try troops you do not own]
5. The Hollow King in battle 6 is a normal Legendary troop in the enemy banner, no boss rules. [Yes; boss rules can wait for chapter 2]
6. A profile that already got the chapter 1 Writ from the one-battle Phase 2 chapter keeps it and gets no second Writ for finishing all six. [Yes; nothing is taken back]
7. (builder check) A solo Skirmish starts straight from the picker, not through the war-gates pads; pads stay for parties and Phase 5; the Ride plate keeps its name. [Yes; otherwise pad team, pad size and map votes fight the picker]
Call made in the draft after the builder check: Practice fields unowned troops through an uncapped practice barracks (own banner as the starting lineup, every troop buyable at star 1, nothing saved); unowned troops are never written into the saved banner.
Code facts the vision did not account for (from the draft): campaign battles currently run as practice (one bot, 40 practice gold, every troop unlocked, size ignored, any battle startable in any order), so "your own banner" and normal rewards need new seating code; the Phase 2 plan grants the chapter 1 Writ once every chapter 1 row is cleared, which until Phase 4 is battle 1 alone; mode changes (bases, barracks, respawn, rout targets, morale row, bot targets) are per-team or global constants today; Ironhold's gate wall can be walked round at both ends, so a yard-then-gate Siege needs Siege-only ward walls and a gate that starts open; and the tier ladder is not monotonic, so a campaign curve cannot rely on tier labels alone.

## Kort's answers 2026-10-18 (afternoon)

- War Table home v4.2 approved. The focused order sheet is the enlarged sealed one and focus follows hover (Practice hovered: Skirmish drops to normal size with no seal, Practice grows and takes the seal); the menu gets animation and effects so it feels alive. Written into the UI spec and plan Task 5.
- Title v3 (blocky) rejected: "Steve on a hill facing the battle, overlooking it, sword in the air; one unified hill, no close-up/far-away split". Next step is quick demos to choose from, staged in the game itself, before any long build.
- Phase 4 spec questions: 1 yes (the escort variant is parked for now and will be revisited), 2 to 7 yes. Spec status moves from draft to approved; the Phase 4 plan is written once Phase 2's names exist in code.

## Title demos ready (2026-10-18, afternoon)

Six real in-game captures (no AI art) at `work/ui-concepts/title/demos/` and on the decision board: classic Steve on one built hill, diamond sword raised, a live 120-troop battle on the Rivergate bridge below, shaders on. Demos 1 to 5 raise the arm with a ten-line render hook (vanilla has no overhead pose); demo 6 is the best hookless swing frame. Pick one, with any notes on time of day, camera and how much battle. The real title adds the v2 lockup, menu, slow drift and the day/night clock rule on top of the chosen scene. UI plan Task 4 is re-planned around the pick.

## Media shoot (2026-10-18, afternoon): PARKED by Kort the same day

Kort liked all six title demos but asked for a reshoot with no sword: Steve standing and watching the battle, face close-ups near and far, walking-away shots, brainstormed scenarios, trailer film, gameplay footage, intro and outro plates. Then: "write all this down and put a hold on it until all the units are designed and polished, maps are designed and polished". Shot list kept at `docs/media/SHOTLIST.md`; the shoot agent was stopped before it produced anything. Resume after Phase 2 Task 5 and Phase 3. The title-screen pick is deferred to the same point.
