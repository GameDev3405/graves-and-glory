# Historical snapshot — not current instructions

Preserved 2026-10-25. Use docs/WORKFLOW.md and docs/HANDOFF.md for current directions.

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
6. A profile that already got the chapter 1 Pact from the one-battle Phase 2 chapter keeps it and gets no second Pact for finishing all six. [Yes; nothing is taken back]
7. (builder check) A solo Skirmish starts straight from the picker, not through the war-gates pads; pads stay for parties and Phase 5; the Ride plate keeps its name. [Yes; otherwise pad team, pad size and map votes fight the picker]
Call made in the draft after the builder check: Practice fields unowned troops through an uncapped practice barracks (own banner as the starting lineup, every troop buyable at star 1, nothing saved); unowned troops are never written into the saved banner.
Code facts the vision did not account for (from the draft): campaign battles currently run as practice (one bot, 40 practice gold, every troop unlocked, size ignored, any battle startable in any order), so "your own banner" and normal rewards need new seating code; the Phase 2 plan grants the chapter 1 Pact once every chapter 1 row is cleared, which until Phase 4 is battle 1 alone; mode changes (bases, barracks, respawn, rout targets, morale row, bot targets) are per-team or global constants today; Ironhold's gate wall can be walked round at both ends, so a yard-then-gate Siege needs Siege-only ward walls and a gate that starts open; and the tier ladder is not monotonic, so a campaign curve cannot rely on tier labels alone.

## Kort's answers 2026-10-18 (afternoon)

- War Table home v4.2 approved. The focused order sheet is the enlarged sealed one and focus follows hover (Practice hovered: Skirmish drops to normal size with no seal, Practice grows and takes the seal); the menu gets animation and effects so it feels alive. Written into the UI spec and plan Task 5.
- Title v3 (blocky) rejected: "Steve on a hill facing the battle, overlooking it, sword in the air; one unified hill, no close-up/far-away split". Next step is quick demos to choose from, staged in the game itself, before any long build.
- Phase 4 spec questions: 1 yes (the escort variant is parked for now and will be revisited), 2 to 7 yes. Spec status moves from draft to approved; the Phase 4 plan is written once Phase 2's names exist in code.

## Title demos ready (2026-10-18, afternoon)

Six real in-game captures (no AI art) at `work/ui-concepts/title/demos/` and on the decision board: classic Steve on one built hill, diamond sword raised, a live 120-troop battle on the Rivergate bridge below, shaders on. Demos 1 to 5 raise the arm with a ten-line render hook (vanilla has no overhead pose); demo 6 is the best hookless swing frame. Pick one, with any notes on time of day, camera and how much battle. The real title adds the v2 lockup, menu, slow drift and the day/night clock rule on top of the chosen scene. UI plan Task 4 is re-planned around the pick.

## Media shoot (2026-10-18, afternoon): PARKED by Kort the same day

Kort liked all six title demos but asked for a reshoot with no sword: Steve standing and watching the battle, face close-ups near and far, walking-away shots, brainstormed scenarios, trailer film, gameplay footage, intro and outro plates. Then: "write all this down and put a hold on it until all the units are designed and polished, maps are designed and polished". Shot list kept at `docs/media/SHOTLIST.md`; the shoot agent was stopped before it produced anything. Resume after Phase 2 Task 5 and Phase 3. The title-screen pick is deferred to the same point.

## UI Task 2 (Iron Ledger in game) raised two things for you (2026-10-18, afternoon)

1. **Chat over the group cards.** Vanilla chat (and advancement lines) draws at the bottom left, over the group cards. Neither the spec nor the mock says where chat goes. Recommendation: move the chat origin up so it sits above the top card (the cards never move), and let a later task shrink chat width in battle. UI Task 3 does this unless you say otherwise.
2. **The ledger looks small at 1280x720.** The spec's scale rule gives the HUD 1x at that window while the vanilla hotbar is 2x or 3x, so the hotbar looks bigger than the ledger. Options: (a) keep the rule (the mock's intent: the HUD is sized to the canvas, not to GUI Scale); (b) let the ledger follow GUI Scale up to 2x on windows under 1600 wide. Recommendation: (b), one line in UiMath.pixelScale, re-checked by the existing canvas checks. Parked until you answer; nothing changes meanwhile.

## Chapter 1 copy draft ready to read (2026-10-18, afternoon)

`docs/copy/chapter-1.md`: the six Road North briefings (spec text kept verbatim), new victory/defeat lines, bonus tags, Survival and Banner how-to lines, mode feed lines, the Pact card and a 40-word camp blurb, with a table mapping every string to its sheet column. Read it when you like; the spec's words are approved, the new lines are not until you say so. Twelve choices where the spec was silent are listed at the end of the file (new columns campaignBattles.victory/defeat, bonusObjectives.tag, chapters.writText/blurb; five section 8 lines reworded to fit the 140 px feed slip).

**Kort, 2026-10-18 evening:** chat goes above the group cards, kept small but readable. Applied in UI Task 3. The ledger-scale question is still open. Playtest of 0.4.0 waits until Kort has reviewed more.

## 0.4.0 soak verdict and one defect (2026-10-18 evening)

Forty bot-only 3v3 rounds on the shipped commit (20 on a normal world, 20 on the shipped flat world): every round ended with a winner, 0 exceptions, tick mean at most 7 ms, heap flat. Verdict: safe for your playtest. Full report: bannerbound-runs/work/p1/soak-145424/SOAK-REPORT.md.

Defect found, not blocking: every arena rebuild over an existing arena spills hundreds of loose items (poppies, seeds, lanterns) because the build clears columns bottom to top without suppressing neighbour updates. Worlds that switch maps many times fill with items and slow by about 1 ms of tick per 1,000. Fix folded into Phase 3 Task 2 (Arena is its file): write with flags 2|16 and discard items inside the arena box at finish. A second, smaller note: a one-second freeze when a round starts on a fresh server, from the arena finish meeting the autosave. Left as is.

**Kort, 2026-10-18 late:** story spec answers. 1 Hollow King is Osric: NO (identity to be reworked as the plot twist). 2 Wren blew the Horn: yes. 3 Companions join one by one: yes. 4 Tomas lives: yes. 5 "Bannerbound" rejected; wants name options. 6 (voice) not answered yet. 7 Scenes unskippable the first time, free after. Wants: love interests, sorrow and despair, a plot twist, every chapter ends on a cliffhanger, depth. Storybook framing approved: book on the table, wind flips it open to the current page, 2 to 4 scenes per page, X pages per chapter, click a scene to play it, then the match.

**Kort, 2026-10-18 late, second round:** Hollow King is Edric Vane (the second name on the pact, Maud's lover): yes. Company name: the Writborne. Voice: some speakers Kort, some friends, maybe some AI, undecided; write dialog and captions now, voice later. The storybook must be given real art and detail (the demo was a flow mockup).

## Kort directions, 2026-10-18 late

1. **Unit AI deep dive.** Kort wants a dedicated pass on troop AI: pathfinding, stances, unit cohesion (staying together as a unit), commands, decision-making. Not a task in any current plan. Recorded here and on the roadmap as its own phase, "The soldiers think", to be brainstormed and specced after Phase 3 lands (pathfinding depends on the built maps). Recommendation: run it between Phase 3 and Phase 4, before the campaign battles are tuned.
2. **Media after polish.** Title scene, announcement video and snapshots start once maps, lobby and units are done and polished; the shot list is in docs/media/SHOTLIST.md (parked earlier today). Confirmed.
3. **Map and lobby quality.** Do not rush the Phase 3 work; add help rather than cut time; approve nothing below a 9. Review method: a controlled character walks each map and the camp, screenshots are taken from the walker's eye, graded, then edited. Applied as a walkthrough gate in the Phase 3 plan (Global Constraints) and ledger.

**Kort, 2026-10-18 late, third round (story v2 section 13):** 1 Ashka is the heir and the love interest: both. 2 Maud leaves with Tomas: yes. 3 End the saga in chapter 5 on the player's choice, complete but open to a sixth: yes. 4 Choice chits: fine. 5 Stone readable from chapter 1: fine. Story design v2 is approved; next is the chapter 1 copy rewrite, then the Phase 4 plan with the storybook in it.

**Kort, 2026-10-18 late, fourth round:** approved the familial pass (one new name per page, roles not names, no realm names in dialog, fire scene after every battle with fixed nicknames) — in the spec as section 4.8 and the `story-names` check, commit 254dd88. Wants the company name changed from the Writborne to "Danta Hollow"; asked whether the Writborne was the player character. Answered: it is the company, not the player; the player has no fixed name. **Open:** is "Danta Hollow" the company name, the player's name, or a place? Not applied until he says.

**Kort, 2026-10-18 late, fifth round:** the player character is Danta Hollow; the company is "the Hollow". Applied to the story spec (section 4.8 and every mention) and sent to the copy writer. Note raised: the enemy is "the Hollow King", so the names echo. Recommended binding: the title was always "king of the Hollow", the old company's name, which Danta's line carries; Edric took it with the pact; chapter 1 notices, chapter 5 pays off. **Open:** bind the echo (recommended) or rename the enemy.

**Kort, 2026-10-18 late, sixth round:** the Hollow echo is the saga's hidden meaning: "Hollow" is the pact's name, every captain takes it, the Hollow King is a title for the captain who takes the crown, the mounds hold them, Danta is the first to refuse. Approved; written into the spec as section 4.9 and one beat per chapter in section 9. Chapter 1 plants only Wren's line and the Pact card's form "Danta, of the Hollow".

**Kort, 2026-10-18 late, seventh round:** two game clients may run at once for automated checks, never while he plays. Client rule from now on: a java process with `--gameDir` outside `C:\Users\kortt\Melty\*\work\` is Kort's: wait. Otherwise count harness clients (gameDir under a `work\` run root); at two, wait. Each agent still runs one client at a time.

**Chapter 1 copy, 2026-10-18 late:** rewritten from story v2 with the familial rule, Danta Hollow and the Hollow: 238 dialog lines, one new name per page (Maud, the Hollow King, Wren, Hesper, Dunmar, none, Ashka, Edric), nicknames fixed (Third, Grey, Tidy, Banner, Bird, Honest, Quick). Rulings written into the spec (section 8): the echo lands on page 3 at the first fire Wren shares; Tomas is "her brother" all chapter; item cards; storyPages columns; Wren's memories advance per return to camp; grave roll uses bynames. For Kort to read: `docs/copy/chapter-1.md`, and the twelve silence choices at its end.

**Reward track, 2026-10-18 late:** Kort sent a live-service battle-pass reference and asked whether we have a design. We have the system (Founders chapter, 30 tiers, Houses spec 4.8) and only a plain list for its screen (UI spec 4.4). Mockup round 1 dispatched on two recommendations, both open for Kort: (1) no paid tier: two rows, "The Roll" for everyone and "The Oath" for House-sworn players, earned not bought; (2) no clock on the Founders chapter, which never expires; timed chapters later get one.

**Kort, 2026-10-18 late, eighth round:** reward track: no paid tier; two earned rows, The Roll (everyone) and The Oath (House-sworn); no timer on the Founders chapter. Approved as recommended; the mockup round runs on it, and the UI spec's section 4.4 treatment of the Chapter page is superseded by the track design once the mockup passes.

**Unit AI design draft v1, 2026-10-18 late** (`docs/superpowers/specs/2026-10-18-unit-ai-design.md`, commit 1f3cb1f; builder check running). Ruled by me: Hard keeps Phase 2's stronger banners and the scorecard measures tiers with equal banners; the player's troops always react at Hard's speed; a brain only, no banner-bearer; new commands in the R council only. **Open for Kort:** (1) rout on Hold: keep morale breaks on a held line, with a "Wavering" warning on the card, or does hold mean they never break? (2) a Detach order that splits a group in two? (3) should Hard be a fight you usually lose until you learn its tells? (4) should allied bots announce their plan on the map and feed?

**Kort, 2026-10-18 late, ninth round (unit AI):** hold means never break: a held group is exempt from rout, closes gaps when a member falls, keeps holding. Detach: yes, once per group. Hard: a fight the player loses until they learn the tells and strategies. Allied bots: announce plans, and the player can ask them for aid and tell them where to look ("Aid here", "Watch here" on the ping wheel; the council answers in the feed). Written into the spec, section 14.

**Kort, 2026-10-18 late, tenth round (UI):** the candle and ink pot read too small on every table screen; scale props up (about 2.5x) with light and shadow. Wood placement varies per screen (planks, knots, nails, fittings), never the same backdrop. Added to DESIGN-RULES.md and the UI plan's Global Constraints; sent to both artists running now.

**Kort, 2026-10-18 late, eleventh round (characters):** the troops are new characters, not recolours: new geometry (horns, hoods, pauldrons, bone), body proportions per House and troop, hand-drawn skins per stage, own weapon models; only the skeleton is shared. Written into the unit identity spec's rules; Phase 2 Task 5's brief carries it (shared part library, proportions, rubric gate) when Task 4 finishes.

**Kort, 2026-10-18 late, twelfth round (animations):** own animations per skeleton (biped, hound, spider, colossus, boar-kin) plus a tell per troop, in Minecraft's keyframe system; own models loaded by the mod; no Entity Model Features, Entity Texture Features or Fresh Animations in the modpack. Approved, "make sure not to break anything". Phase 2 Task 5 rewritten accordingly with two animation tasks after it; the shipped 0.4.0 is untouched.

**Reward track round 2, 2026-10-18 late:** controller grade 9, sent to Kort (work/ui-concepts/track/). For Kort: grade it; the Oath pennant and cloth names are the artist's proposals (Fess, Chief, Bordure, Saltire, Quarterly, Gyronny, Pily; Crowned field, Night cloth), keep or rename; the wood variety on this page is a different seed of the same planks, say if plank direction or a bound edge should change per page too. When approved, the UI plan gets a task for the screen in place of the plain list (after Task 7).

**Kort, 2026-10-18 late, thirteenth round:** (1) Reward track round 2 approved; the candle and ink pot come off that page (kept for other pages); the artist's Oath item names stay. Re-rendered without props, same grade; the three PNGs are at `work/ui-concepts/track/`. The UI plan's Task 7 now builds the Chapter page as the track. (2) The 720p ledger size was left to me: the Iron Ledger follows GUI Scale up to 2x on windows under 1600 px wide, and stays at kit scale from 1600 up, so 1366x768 and 1280x720 at GUI 2 get a readable ledger without the 1080p layout growing. Queued as a polish item for the UI plan's Task 8. (3) The twelve silence choices of the chapter 1 copy, answered click-through: 1 to 5, 7, 8, 10 to 12 kept; 6 confirmed as the spec's rule (Wren's memory advances every return to camp after a battle); 9 overturned: Tomas is named on page 8, by Maud, after Edric, the one page with two new names. Applied to the copy and the spec. (4) Standing rule: questions are written here and only presented when Kort asks what needs his attention, and then as click-to-answer multiple choice, one question after the next.

**Kort, 2026-10-18 late, fourteenth round:** (1) "Writ" is renamed "Pact" everywhere a player reads it (the paper of command and the recruit item); the story spec, chapter 1 copy, Houses spec and Phase 4 design spec are renamed; the Phase 2 plan, the Phase 4 plan and the Phase 2 code's strings follow through the agents editing them; internal ids and saved-profile keys keep their names. (2) Unit AI design spec approved as written; the plan is written once Phase 3's maps are far enough along. (3) Chapter 1 copy approved for the Phase 4 build, voice later. (4) The formation key's short chip stays "Form". (5) The 0.4.0 playtest waits for 0.5.0 (Houses, new characters, new UI).

**Storybook mockups, round 1 (2026-10-18 late):** five screens at `work/ui-concepts/storybook/` (closed book, open spread, a Fall panel, a stage scene, the march card); controller grades 9.5, 8.5, 9.5, 9, 8.5; one fix round running (page 2 shows Maud alone, as the copy has it). For Kort once the fix lands: grade the set; and one decision: the Hollow's company look on the cards and seal, a navy tabard and the artist's own seal mark, is a proposal, keep or change.

**Kort, 2026-10-18 late, fifteenth round:** the storybook's scenes are real in-game footage, set up to look however the scene needs; no painted panels or traced block scenes. Written into the story spec (section 7: book scenes are staged stills in the engine; `panel <set>`; thumbnails are captured frames; portraits rendered from the live model, my call) and DESIGN-RULES.md; sent to the Phase 4 plan writer (stagecraft: set templates, fog override, posed actors, camera hold and drift, a build-and-grade task per set) and to the storybook artist, whose world shots are placeholders until a staging agent's harness screenshots replace them.

**Kort, 2026-10-18 late, sixteenth round:** (1) the Hollow's look is the storybook artist's: a navy tabard and its own seal mark, now the company's colours and mark everywhere (the book cover, the cards, the standard). (2) Portraits are rendered from the live character model, not hand-drawn; confirmed. Nothing else is open.

**Kort, 2026-10-18 late, seventeenth round:** the brass butterfly key (the bowtie-shaped plank joint) is removed from every table page; wood variety uses plank layout, knots, nails, straps and mending plates only. Sent to the track artist, the storybook artist and the UI Task 5 implementer.

**Kort, 2026-10-18 late, eighteenth round:** drop Excalibur and Clarent, "so we are not dependent on anything but ourselves; the modpack is 100% ours". Applied as: every third-party resource pack leaves the modpack (Excalibur, Clarent, the Excalibur Fresh Animations patch, Fresh Animations, Faithful 32x); the game ships its own look. Mods and shaders stay for now (Fabric API, the performance mods, Iris and Sodium are open-licensed libraries the game needs; Complementary and MakeUp are shader packs that permit modpack use; EMF and ETF leave when the own models ship). Footage and stills use no resource pack. Also: no brass fittings of any kind on table wood (keys, plates, straps, tacks); the kit's rail, plaques, frames and seals stay.

**Kort, 2026-10-19:** the storybook stills now being staged are pipeline placeholders (no resource packs, shaders on, old troop look); they settle the staging pipeline and the layout so the real set shoot is easy once the own characters (Phase 2 Task 5) and the built maps (Phase 3) exist. Final storybook art and all media wait for that.

**Phase 4 plan, 2026-10-19:** written (commit 1bcbcef, 27 tasks), then extended with Task 19, the named cast (own faces and skins on the troop rigs; Pell on the camp librarian villager; the King's helm comes off to show Edric), 28 tasks. The camp guide blurb is Maud's one-time card (round nineteen), placed by the planner at the first return to camp after a campaign battle, since the copy fixes Maud's first line at the start of camp; controller accepted. Rulings made by me on the plan's other 36 assumptions are in the plan.

**Kort, 2026-10-19, nineteenth round:** the in-game War Table home passes as the look the other pages copy. He asked why the Kingsfield Crossroads card had no painted map: only Rivergate's map was ever painted (the mock's), other maps showed point seals on bare vellum. Ruled into UI plan Task 7: painted map cards for all five maps, graded. Camp guide blurb: Maud says it, one short card at the war table on the first camp arrival, never again (Phase 4 plan).

**Kort, 2026-10-19, twentieth round (token economy):** agents unlimited; everything else optimised: no messages to parked agents except work, art loops capped (render, lint, one self-review, report; the controller grades), one contact sheet per round instead of full frames, Sonnet for mechanical work and small reviews, effort below max for mechanical agents, whole briefs up front. Tool load: the Vercel, computer-use and Playwright connectors are being turned off for this session.

**Storybook round 2, 2026-10-19 (controller):** all five pages rebuilt with every ruling and graded 9.5, 9.5, 9.5, 9, 9.5; the set passes; the contact sheet went to Kort. Two things the artist raised were ruled, not asked: Maud's in-game look is the company's navy tabard on her own rig (unit identity rule 8, Phase 4 Task 19), so the placeholder Gilded Maud in the stills is reshot once the cast exists; the Hollow standard needs the mod's own banner art (vanilla has no navy), added to Task 19.

**Troop models, 2026-10-19 (round 21):** the six Gilded troops now have our own models, rigs and stage art (Phase 2 Task 5), graded 9 by the controller. Two calls for Kort: (1) the spec makes stage 2 of the archer, Templar and longbow a trim repaint, so at 36 px those stage 2s differ from stage 1 by a few gilt pixels, while the knight's stage 2 changes material (polished plate) and reads on its own; keep the spec (recommended, the stage shows in the card and the close-up) or give every stage 2 a material change. (2) The four side colours (gold, violet, crimson, teal) tell apart in colour, but gold and teal are near-equal in greyscale; keep as is (recommended, the pennant glyph also differs) or darken teal one step for colour-blind play. Parked on Phase 2 Task 13 until answered.

**Kort, 2026-10-19 morning, twenty-first round:** (1) stage 2 art keeps the spec: trim repaints for the archer, Templar and longbow, material change only where the spec says so. (2) Side colours stay as they are; gold and teal are told apart by hue and the pennant glyph. Both recorded; nothing is parked on Task 13 for these.

**Kort, 2026-10-19 morning:** no branch is merged yet, so nothing playable holds all the work; the 0.5.0 build (Houses, own troop models, the five built maps, the War Table) follows the base merge once the running tasks land. Kort wants to be told the moment 0.5.0 is ready; the controller sends the build with SendUserFile and a one-line note, unasked.

**Kort, 2026-10-21 evening:** the growth design (`docs/superpowers/specs/2026-10-19-growth-design.md`) and the 0.6.0 "The Field" plan were written and committed by another session (a273522, 2026-10-20 22:47, under Kort's git name), which recorded them here as approved. Kort did not approve them and asked for them to be deleted; commit a273522 is reverted (its PARKED.md and VISION-AND-ROADMAP.md blocks and these lines go with it). The six calls it queued are withdrawn.

## graphify, round 22 (2026-10-21)

**Kort, 2026-10-21:** asked to install and use graphify to organise the project. Done: the code of the base checkout and the four branch worktrees is mapped locally, at no token cost (see HANDOFF, "graphify"). One call for Kort:
(1) The docs (101 specs, plans, ledgers and notes) are not in the graph. Mapping them, and naming the graph's groups ("Community 12" becomes "Battle HUD"), runs Claude over every doc, a one-off token cost of roughly one large review. Options: keep the graph code-only for now and add docs after the 0.5.0 merge (recommended: the plans move fast and would need a rebuild after every task); map the docs now; never map docs.

## Gilded models sign-off, round 23 (2026-10-21)

**Kort, 2026-10-21 evening:** asked to see the units so far and whether they are done. Sent `contact-art5.png` (the six Gilded troops, lineup, portraits, cards) and `contact-art6.png` (stages 1 to 3, close-ups, four side colours). Also ruled: animation is judged on a bare biped, with a short geared pass for clipping only.
(1) The Gilded six and their stage art were graded 9 by the controller and passed two reviews, but Kort's own yes on them was never recorded. Options: they pass as they are (recommended: the House tasks reuse their part library and proportions, so a late change costs four Houses of rework); name the troops to change; hold until the 0.5.0 playtest.

## Troop depth, round 24 (2026-10-21)

**Kort, 2026-10-21 evening:** wants troops and weapons with 3D depth, all our own work. Design: `docs/superpowers/specs/2026-10-21-troop-depth-design.md` (Task 5d): weapons become solid pixel sprites (bows with three pull frames), and each troop gets a relief layer whose pixels stand one pixel proud within 24 blocks. Three calls, each already built the recommended way:
(1) How the bow hangs at rest: at 45 degrees so its curve reads from the front, side and portrait (recommended); string to the back (edge-on from the front); string toward the leg (full curve from the front, edge-on from the side).
(2) Bow thickness: 2 px so it still reads edge-on (recommended); 1 px like vanilla items.
(3) A Visuals on/off switch for troop depth: none for now, parked, because a near cap and a frame budget hold the cost (recommended); add the switch now.

**Kort's answers, 2026-10-21 evening:** round 22: the graph stays code-only until the 0.5.0 merge. Round 23: the Gilded six and their stage art pass as they are. Round 24: (1) bows rest at 45 degrees; (2) bows 2 px thick; (3) add the Visuals on/off switch for troop depth now (in Task 5d, default On). Growth design: not approved by Kort, deleted at his word.

## Art pipeline v2, round 25 (2026-10-21)

**Kort, 2026-10-21 evening:** wants the troops and weapons to look better, Soulrend-style, all in-house; approved three directions: Blockbench as the workshop, a Minecraft 3D texture rulebook, and LabPBR maps. Blockbench 5.2.2 and its MCP plugin are set up for agents. The spike remade the Order bow and the arming sword in Blockbench (`bannerbound-p2b/work/p2b/spike-bb/side-by-side.png`, sent to Kort): sword 66 quads, bow 72 to 78, inside the 128 cap. Design draft: `docs/superpowers/specs/2026-10-21-art-pipeline-v2-design.md`; rulebook: `docs/ART-STYLE-GUIDE.md`. Three calls for Kort:
(1) Approve the design: weapons and troops become Blockbench models, painted to the style guide, with LabPBR maps, starting in Task 5d's art step and carried through the House art halves (recommended); approve weapons only, troops stay as they are; not now.
(2) The Gilded six: repaint them to the new style guide in Task 5d's art step, about 28 sheets (recommended: you said the units could look better, and every House after them is painted to the guide); keep them as signed off and start the guide with the Ironclad.
(3) Complementary only shows our normal and specular maps in its labPBR mode, which turns off its built-in PBR for every vanilla block. Ship the maps and leave Complementary on its default, so only players who switch see them (recommended: cheap, and the world keeps its look); set the modpack's Complementary to labPBR; skip the maps.

**Kort's answers, 2026-10-21 night, round 25:** (1) the art pipeline v2 design is approved: weapons and troops become Blockbench models painted to the style guide, with LabPBR maps, from Task 5d's art step through the House art halves. (2) The Gilded six are repainted to the style guide in Task 5d's art step. (3) The LabPBR maps ship; the modpack's Complementary stays on its default mode.

**Kort, 2026-10-21 night (art references):** before any more weapons are modelled, Haiku scouts (extra high effort, Kort's explicit choice over the "no Haiku" rule) gather reference images for every weapon, shield and armour piece, from 3D Minecraft weapon mods, fantasy game art and real historical pieces, at least 8 per item group; Kort reviews them on a review page and keeps or rejects each. Every troop's weapon and armour must look visibly stronger and cooler at each of its three stages (this supersedes round 21's "stage 2 is a trim repaint" for weapons and armour). Body armour is in scope.

**Kort, 2026-10-21 night (troop progression):** troops should level up through combat, not gold. Answers: (1) levels are earned per soldier during a match, from kills; a newly bought or rebought soldier starts at level 1. (2) Better weapons and armour are bought by the player during the match. (3) A level gives a stat bonus, unlocks the next gear tier for purchase, and unlocks or improves an ability. (4) Three weapon tiers and three armour tiers, bought separately; a soldier who levels up keeps the gear he had until the player buys the next tier. This replaces the approved between-match evolutions (stages bought with gold and shards, Mastery I gating stage 3) and needs its own design for Kort's approval; Phase 2 work continues meanwhile.

**Kort, 2026-10-21 night, round 26 (in-match progression):** design approved as written: `docs/superpowers/specs/2026-10-22-in-match-progression-design.md` (his 13 answers in its section 13; supply houses get their own design next).

**Kort, 2026-10-21 night, round 27 (supply houses):** design approved as written: `docs/superpowers/specs/2026-10-22-supply-houses-design.md` (his 15 answers in its section 14). He also asked that the in-match progression plan give its balance gate (progression 9.2) the same re-run-before-fail rule as supply houses 11.2, because 20 matches cannot tell a 10-point shift from chance; the progression spec itself is unchanged.

## Weapon look round, round 28 (2026-10-22)

**The look round:** the Order Squire's arming sword and the Order Archer's bow were remade at weapon tiers I to III from Kort's kept references, with a silhouette pass first and an independent grader (no self-score). It took two fix rounds:
- round 0 scored 6.5;
- fix round 1 scored 7;
- fix round 2 scored 7.5 (swords 7.5, bows 7.5).

Sheet: `bannerbound-p2b/work/p2b/look-round/look-round-sheet.png`, with the earlier rounds as `-r0` and `-r1` (sent to Kort). Grades: `.superpowers/sdd/2026-10-17-phase-2-houses/look-round-grade*.md`.
- **What is clean now:** tiers I and II of both weapons read as clean Minecraft-mod weapons, and the tiers differ at a glance in the hand.
- **What the grader found:** nothing yet has the Souls character of Kort's picks. At 1 texel per model pixel, a 2 px blade has room for only two colours across its width, so it cannot carry a sheen, an engraved fuller and edge wear. More rounds under today's rules top out at about 8 to 8.5.

Five calls for Kort:
(1) **Texel density for weapons.**
- One trial round at 2 texels per model pixel on these six weapons, then decide on that sheet (recommended: the grader says it is the only route to 9; it costs nothing in frame time, and the risk is texels half the size of the skin's at the hand, which the trial will show).
- Stay at 1:1 and sign weapons off at about 8, as an exception to the 9 bar.

(2) **Sword III's sun and bow III's badge.**
- Drop the bow badge, and shrink the sword sun to a boss no wider than the blade plus 1 px a side (recommended: at game size both read as a grey box, not a sun).
- Keep both as they are.

(3) **Weapon scale.**
- One-handed swords about 1.5 blocks long, 24.5 px at most, as on the sheet (recommended: closer to Simply Swords and the Souls refs).
- Back to about 1 block.

(4) **A Blockbench check** before the other 28 troops are modelled. The agents' attempt to load the files in Blockbench was blocked by the permission system, so nobody has opened these six there. Kort opens `bannerbound-p2b/work/p2b/look-round/order_bow_t3.bbmodel` in Blockbench and checks that it shows `rest` and `pull_0` to `pull_2` with one 32 x 64 texture. Kort may also close the 17 old look-round and spike tabs without saving.

(5) **The Marshal's idle gesture.** His Tell line is "horn to the lips", but his model carries a shield and no horn, so the gesture (Task 5c) reads as "shield raised". Options:
- Give the Marshal a war horn on his belt that he lifts in the gesture, added in Task 5d's Gilded repaint (recommended: it keeps the Tell line, and a horn is his Rally Horn ability made visible).
- Change the gesture to a raised sword arm.
- Accept "shield raised".

Controller rulings, recorded rather than asked:
- The style guide's weapon section becomes the look-round recipe once the look is approved.
- The pipeline gains two checks: no coplanar faces between materials, and no air gaps between parts.
- Tiers I and II are capped at 16 cubes and tier III at 18, with the frame-cost check rerun on these six.

**Kort, 2026-10-22, round 29 (no routing):** design approved as written: `docs/superpowers/specs/2026-10-22-no-routing-design.md` (his 11 answers in its section 15, including mid-review: no horns in the field, every command and task given from the screen, never by an item, weapon or horn; a Retreat he clicks may play a horn sound to him; Rally Horn is renamed Muster). Note: round 28 call (5) recommends a belt war horn for the Marshal as "his Rally Horn ability made visible"; that reason no longer holds under this ruling.

**Kort's answers, 2026-10-22, round 28:**
1. A trial round at 2 texels per model pixel runs on the six weapons, and Kort decides on that sheet.
2. Bow III's badge is dropped, and sword III's sun shrinks to a boss no wider than the blade plus 1 px a side.
3. One-handed swords are about 1.5 blocks long, 24.5 px at most.
4. The Blockbench check is still open for Kort.
5. The Marshal gets a war horn on his belt, which he lifts in his idle gesture. It is added in Task 5d's Gilded repaint.

## Weapon texel trial, round 30 (2026-10-22)

**The trial:** the six look-round weapons at 2 texels per model pixel, graded 8 out of 10, up from 7.5 (`look-round-grade-trial.md`).
- Bow III's badge was dropped, and sword III's sun became a painted boss.
- The bow files now open with only `rest` visible. Kort found all four frames showing at once in Blockbench.
- No coplanar faces remain in any frame.
- The grader now puts 9 out of reach for weapons at Minecraft scale; about 8.5 is the ceiling.

**Kort's answers, 2026-10-22, round 30:**
1. All 30 troops' weapons use 2 texels per model pixel. Three rules come with it: parts within 1 px of the hand are painted in whole pixels, one-texel patterns sit only one tone step from their face, and a joint check makes every frame one connected piece.
2. Weapons pass at 8.5 from the grader, and Kort sees each House's weapon sheet before it ships. All other player-facing art keeps the 9 bar.

**Kort, 2026-10-22, round 30 (the Marshal's horn under no routing):** the Marshal wears the belt war horn and lifts it in his idle gesture as dress only. It never plays a sound and gives no command, in line with the no-routing design's "no horns in the field" (round 29). The Task 5d repaint adds it.

## Map identity, round 31 (2026-10-22)

**Kort's answers, 2026-10-22, round 31** (21 questions asked in chat; full list in `.superpowers/sdd/2026-10-17-phase-3-built-world/kort-map-vision-2026-10-22.md`):
1. Each map is picked for different tactics; layouts, looks and design should all differ, "a brand new map from each other"; fast and slow maps; grim and weathered tone; Rivergate is the first and default map.
2. Rivergate: bridge brawls on a great river, pastoral valley with a mill. Ironhold: a real siege, walled fortress on one side, intact looming keep. Frostmarch: ambush pass, narrow centre and open flanks, signal tower beacon plus cave refuge. Hollowwood: eerie haunted dense forest with giant sequoias, tight and brutal, dark spruce, mist, glowing shrines, dense edges and open lanes. Kingsfield: the big war map, royal estate country, fortified high-ground outposts and a watchtower network, fireworks as flares.
3. Phase 3 finishes on today's layouts with these looks and roles; the layout redesign becomes its own spec afterwards (Kort's choice).

**Kort, 2026-10-22 (later, in chat):** no map is made fast or slow on purpose; match length is whatever the terrain and layout give (this replaces the pace answer above).

**Open for Kort:** whether the map layout redesign gets its own spec next (recommended: yes, after Phase 3's walkthrough gate).
## Task 8b (no routing) gate: `graveborn_normal` vs `gilded_normal` fails (2026-10-22)

The no-routing design's gate re-run (`docs/PHASE-2-BALANCE.md`, "Phase 2 Task 8b gates") reads 17.5% for `graveborn_normal` against `gilded_normal` at the design's numbers (3 won, 16 lost, 1 drawn of 20; 47.5% with routing after the balance fix, at the same troop rows) and 32.5% after the one tuning round the ruling allows (C16 1.105, C16s 1.05, the Bulwark cover radii 4.25/6.8/10.2, fearless +23% at or below 34.5% health: 6 won, 13 lost, 1 drawn). The tuning is not committed; the sheet stays at the design's numbers. `ironclad_normal` vs `gilded_normal` reads 60% (report-only; 75% with routing), inside the band. The cause, as far as the numbers say: covered flanks make nearly every troop of a tier III Gilded army immune to the flank rows while every Graveborn troop takes x1.30 from a blow behind it, a one-sided multiplier the routing game did not have; the tuning round's four levers together moved it 15 points (within noise of about +/-10 at n = 20), so no single lever can be credited; a one-lever ablation is the balance pass's first step, and a part of the gap is the no-routing game itself (fearless as a 12% rout share was worth more against Gilded than +20% at the edge of death).

Decision in the meantime (the design's stop rule): Task 9 starts on the design's numbers, and Phase 2's balance pass owns the pairing. Options, the recommended one first:

1. **(recommended)** Leave the design's numbers and hand the pairing to the balance pass with this diagnosis. It may move the Graveborn troop rows (the Task 7 route, within 15% of spec) or the Gilded rows again; 8b's rule allowed neither.
2. Narrow covered flanks: cover against shots only (C16s), so a Gilded line can still be flanked in melee; or cover only shield troops. A design change, so yours to call; 8b built the design as approved.
3. Give the Commons the level-2 fearless now (+30% at or below 40% health). The progression design keeps those numbers for level 2, so it would need a new level-2 step.

Answered by Kort 2026-10-22: option 1. The pairing is left to the end balance pass at the design's numbers (no design change). The Oath Banner card keeps its 85-character line (the 90-character limit stands).

## Balance method and map layout, round 32 (2026-10-22)

Answers in click-through, recommended option each time:
- House balance is set by the match round robin (fast ticks, every pair, Bradley-Terry with intervals). Duels and clashes are diagnostics. One real-time gate per release catches fast-tick drift.
- Units are compared per gold from duels; Houses are compared as fielded in matches, so opening gold counts toward the House.
- Loose bands: unit value per gold 0.87 to 1.15; each House 40 to 60% share; no pair wholly outside 35 to 65%. A ranged or siege type is not raised on duels alone.
- Points beating fights (Graveborn over Ironclad, 18 of 20): inspect the Normal bot's point play per House before tuning any number.
- No-damage supports (Mist Caller, Sunweaver): substitution tests before any change.
- The 20-match gate is replaced by the band, measured on the round robin.
- The four queued Tidecallers match pairs and the clash rerun stay held until Kort says gaming is over.
- Map layout redesign: yes, its own spec after Phase 3's walkthrough gate. Not started; Phase 3 is still on its camp base task.

Source: `.superpowers/sdd/2026-10-17-phase-2-houses/balance-method-report.md` section 7.

## Overnight orchestrator, round 33 (2026-10-23)

Recommended option first. Nothing here blocks the night's work.
1. Commit the 2026-10-23 file cleanup in the main checkout (8 docs and PROJECT-STATE.md moved to `docs/archive/`, references repointed to HANDOFF.md, `.gitignore` additions, client cap raised to four)? Recommended: yes, one docs commit by path, your CLAUDE.md and this file's other edits left out.
2. `docs/LOBBY-AND-SERVER-RESEARCH.md` (2026-10-16, nothing links to it): keep for the lobby phase (recommended) or archive?
3. The one-shot generator scripts in `tools/` (add_*.py, implement_*.py, refine_*.py, revise_*.py): leave (recommended) or move to `tools/archive/`?
4. Big untracked folders (`work/p1` 3.4 GB, `work/refs` 1.2 GB, `work/sb`, `work/title-demos`, `work/media` 8.3 GB, `dist/` 981 MB): you decide what to clear. `run*/` and `build/` (about 1.5 GB) are approved and get cleared once no run agent is active.
5. Phase 2 Task 11 (Pacts): should a Pact be earned on any finished match (as built, so players who cleared chapter 1 get theirs at their next match) or only in Long March battles? Recommended: any finish, as built (cheaper, and the Long March is Phase 4).
6. Phase 2 Task 11 (full-star buy): buying a troop already at full stars is refused (as built) or turns into shards (dismantle)? Recommended: refuse, as built; dismantle can come with a shard shop later.
7. Phase 3 Task 3 (camp base) is complete at grades stairs 10, tent/gates/archive 9.5, parade 9.5, terrain 9.0 (contact sheet sent to you). The grader's one remaining complaint: the north side between the stairs still reads as a grey stone wall rather than a hillside. Walk it and decide: accept as is (recommended; Task 4's graveyard is planned for this bank), or spend a round re-cutting the ridge stair as a longer, shallower flight (new export, moves the Recruiter, risks the stairs' 10).
8. Phase 2 Task 12, veteran trim (Mastery II look): graded 8.5, under the 9 bar. An overlay on the armour cannot change a troop's outline, and on the gold side the brass competes with the cloth. Only the six Gilded trims exist (the other 24 troops have no skins yet). Options: accept the trim as a name-plus-trim marker until each House's art half (recommended; no rework now), or add a crest/plume model part keyed to Mastery (needs model geometry, Opus rig work).

**Kort's answers, 2026-10-23, round 33 (click-through, all recommended):** 1. commit the cleanup as one docs commit; 5. Pacts on any finished match, as built; 6. a full-star buy is refused, as built; 7. accept the camp's grey north wall for now (look again after Task 4's graveyard); 8. accept the veteran trim at 8.5 until each House's art half. Items 2 to 4 keep their recommended defaults (keep the lobby research, leave the tools scripts, big folders stay yours except run*/ and build/ after the runs).

## In-match progression plan, round 34 (2026-10-23)

The plan is at `docs/superpowers/plans/2026-10-23-in-match-progression.md`. It has 13 tasks. Until you answer, each task builds the recommended option, and an answer changes only the task named in its question. None of these blocks the start (Task 1).

1. **How should the end-of-plan balance gate be measured?** The design (9.2) asks for 20 real-time matches per House against the current best banner, at 900 s rounds, once with levels off and once on. That is about 30 hours. In round 32 you replaced the 20-match gate with the round robin.
   - Use the round robin (recommended). Every pair of Houses plays at fast ticks and 900 s rounds, once with levels off and once on, and each House's share is measured with intervals. One real-time pair per condition checks the fast-tick result. The pass rules stay the design's: each House within 10 points of its own share with levels off, and a comeback rate no more than 10 points lower. This needs the balance-method tools committed to `phase-2-houses` first. (Task 11)
   - Run the gate as the design states it.
2. **Do you approve the save and network changes?**
   - The save change: profiles stop storing an evolution stage. No player ever evolved, so nothing is lost. The design already approved this; it is listed for completeness.
   - The network changes: the battle state packet goes from version 8 to version 9, to carry the Armoury and the veteran counts. A new per-soldier packet carries level, weapon tier and armour tier, so other clients can draw the tiers and chevrons. Host and players must run the same build, so a 0.5.0-preview1 player cannot join.
   - Approve both (recommended). (Task 3)
   - Ask for a different design.
3. **Do the new battle statuses (weaken, bleed, Hunter's mark) also hit player commanders?**
   - Yes, as Slowness already does today (recommended). A weakened commander's own blows also deal 15% less. (Task 6)
   - Troops only.
4. **What should the Armoury tab look like?**
   - Build it in the style of today's in-battle barracks screen, on the Phase 2 branch now, graded 9 (recommended). That style uses the dark UI colours and Marcellus headings.
   - Paint it in the Iron Ledger kit after the branches merge. That makes it a later, bigger task. (Task 8)
5. **Should Phase 2's end balance pass tune with levels on?** That pass is the one that fixes Gilded, Tidecallers and Wildmoot.
   - Yes (recommended). It runs after this plan's gate, so it tunes the game that ships. This plan's "within 10 points of baseline" check is then re-run once.
   - No, it tunes with levels off.
6. **How are builds made while some troops still lack their tier II and III art?** The design says the release build must fail while any troop still borrows a lower tier's art.
   - Refuse a release package, but allow a marked test build with `-stopgap` in the file name, so numbered builds can still reach you (recommended). (Task 9)
   - Refuse every package until all the art exists.

**Kort's answers, 2026-10-23, round 34:** 1. gate by the round robin per round 32; 2. the save and protocol changes are approved; 3. level statuses also hit commanders; 4. Armoury tab in today's barracks style now, restyled after the base merge. Items 5 (the Phase 2 end balance pass tunes with levels on) and 6 (marked -stopgap test builds allowed while tier art is missing) keep their recommended defaults.

## Orchestrator, round 35 (2026-10-23)
1. **HUD at 8.5 (UI Task 8 leftovers).** `p1-ui-hud-1280-battle` and `p1-ui-hud-1366-battle` grade 8.5: the HUD draws at 1x beside a 3x/2x chat, so legibility and hierarchy each lose half a point. Fixing means bringing back a HUD scale rule you dropped. Options: (a) accept 8.5 on those two sizes, recommended; (b) scale the HUD with the chat.
2. **Eyes needed in game:** the in-party Friends page at 1080p, and real 854x480, 1440p and 4K windows.
3. **Yard lesson 4 wording.** The yard board says "Trained troops join your army at the front." The battle 1 HUD and `docs/copy/chapter-1.md` say "march out from the held point nearest the fight." Options: (a) board follows the HUD, recommended (it is how reinforcements actually arrive); (b) change the HUD to the board's wording. I applied (a) provisionally.

Kort's answers (round 35, 2026-10-23): 1 accept 8.5 on the two HUD sizes; 2 yard lesson 4 board follows the HUD wording. The Friends page at 1080p and the real 854x480, 1440p and 4K windows still need his eyes in game.

## Phase 2 Task 13 done (2026-10-23)
1. **Mixed-banner results (log only).** Gilded+Graveborn mix beats pure Gilded 5% of the time; Ironclad+Tidecallers mix beats pure Tidecallers 90%. Both are outside 40 to 60. The end balance pass (levels on) owns the tuning, so nothing changes now. A pure-Tidecallers army losing 90% to a mix is the structural Tidecallers weakness already diagnosed. Say if you want a different rule for mixed banners.
2. **Four playtest sessions** are written up in `docs/PHASE-2-PLAYTEST.md` for you to run when you have time. 0.5.0 stays unreleased until the levels-and-gear plan and tier I to III art are done.

## Trial merge (integration-0.5), 2026-10-23
Kort's answer: the War Table gap closes by porting Phase 2's missing pieces into the War Table (House-picker claims, troop-shop entry, class icons for the six Phase 2 classes, the full 20-slot Banner page), as one UI task after the base merge; Phase 2's old screen checks move to the War Table. Controller rulings (no action needed): Quartermaster keeps opening the Banner page; rerere stays on; state_v9 fields go after ui's `strength`; the Armoury tab (progression Task 8) is built on the merged base. Report: `.superpowers/sdd/2026-10-23-integration/report.md`.


## FYI from lane B Task 7 (2026-10-23, no answer needed unless you disagree)
A player commander is knocked back by Shield bash and the other level-3 stuns but is never stunned himself (your round 34 answer 3 covered weaken, bleed and Hunter's mark, which do hit commanders). The reason is the same as today's rule: a stunned commander cannot command. Say so if you want commanders stunnable, it is one cell per row.

## Weapon art, stage 1 result (2026-10-23 night, Sonnet 5.5 xhigh made all 18; for your eye)
All 18 weapons (arming sword, order bow, longsword, longbow, sun staff, Marshal sword; tiers I to III) are built in Blockbench on branch `phase-2-art` (worktree `bannerbound-art-sonnet`) after two fix rounds (the workflow cap). Honest grades: roster mean 8.0 by the independent grader, 8.2 by the maker; only Marshal III, order bow III and sun staff III reach the 8.5 bar. Contact sheet: `bannerbound-art-sonnet/work/art/sheet-all.png` (sent to you in chat); per-weapon sheets beside it. The misses are Feel (clean dark steel where your picks are worn and engraved; the picks' fine tracery is below 2 texels per px), not build errors. Weapons are not in game yet: the converter is being written on `phase-2-pipeline`.
1. **Accept stage 1 as built, or push for 8.5?** (a) Accept as built and show you the in-game look once the converter lands (recommended: the in-game check may change the verdict more than more paint). (b) Allow a little wear and ornament on the plain tier I pieces (arming I, longsword I, ceiling about 8.0 today). (c) Try finer texels (3 per px) for the swords: costs a new sheet size and a re-plan.
2. **Staff I bigger head?** Staff I has 5 cubes spare; its head is small on a 38 to 44 px shaft. Yes (recommended) / no.
3. **Longbow III and staff III height** (43.7 px and 48.4 px against the 32 px troop; old 38 and 41): keep as modelled (recommended until you see them in hand) / scale down in code.
Stage 2 (the Gilded six: armour-tier skins, relief layers, troop bbmodels, the Marshal horn) waits on the converter's troop direction and on your answer to 1.

## Phase 3 Task 6 landmarks: tower wedges (2026-10-23 night, non-blocking)
Task 6 passed at 9.0 or higher on every piece (independent grade, shaders on; sheet sent to you). About nine dirt triangles remain in the tower foregrounds: they are ordinary one-block contour steps on a gentle slope, so no filter removes them. Options: (a) accept as is, 9.0 (recommended; they read as terrain); (b) lower relief amplitude with a longer period on towers (the outposts would change too, so they get regraded); (c) wider tower shoulder. The optional roof dormer on the houses was skipped (houses stay at 9.0).

## Levels arrive early in the headless sim (2026-10-23 night; whole-plan review I2, for the balance gate)
In the 900 s bot sim (gilded vs graveborn, progression on) the first level 2 lands at about 95 to 106 s (design aim: 2 to 3 minutes) and there are 32 to 35 level-3 soldiers a side (aim: a few). The cause is the credit rules you approved: every striker is logged, status refreshes log helpers, and supports earn credit on every beat. Task 11 runs the balance gate as designed, records the timings and does not retune them on its own. Options: (a) raise the kill thresholds (for example 4 and 12 instead of 3 and 8) after the gate has run (recommended: it is the plan's first lever and one cell pair); (b) throttle support and assist credit (one credit per support per few seconds); (c) leave it, since the sim bots are blunter than players. Say which, and I rerun the gate once on your choice.

## Kort's answers, 2026-10-24 (weapons, early levels, towers, snow, signal fire)
1. **Weapon art stage 1:** "I like these models as is, but going forward try doing 3 texels per px for everything and see how it looks." Accepted as built at 2 texels per px. From now on everything is painted at 3 texels per px, starting with a trial (scoping plan: `.superpowers/sdd/2026-10-17-phase-2-houses/texel-3-plan.md`).
2. **Staff I head and sizes:** bigger staff I head, keep the other sizes (recommended).
3. **Early levels:** raise the kill thresholds (recommended). The gate agent sets levels.kills to 4 and 12 after the as-designed gate and reruns, with at most one more step (5 and 14) before it asks you again.
4. **Tower wedges:** accept at 9.0.
5. **Frostmarch snow white (overview graded 8.5):** accept the white. **Signal fire:** accept the night glow.

## Phase 3 Task 8: the refuge is a timber hut, not a carved cave
Your map vision (Frostmarch) calls the refuge a carved shelter in the rock. The plan said "a stone hut" and the build is stone-footed timber under a dark oak roof (graded 9.0, reads well). Options: (a) keep the hut (recommended: it passed, a cave needs terrain carving that the box-diff rule forbids outside the zone); (b) redo it as a rock-cut shelter inside its zone, about one more fix round. Plan note added.

## Faces: three small choices I made (reversible, tell me to change)
Variants are chosen from the unit's ID, client-side, so every player sees the same face with no save or protocol change. (1) Roster cards and the reveal portrait show a fixed face (variant 0), not a random one. (2) A soldier's face re-rolls when its mob is created again (between battles); keeping it would need a stable synced id, which is a save and protocol change, so it is yours to decide. (3) Odds are uniform, so 1 in 4 is the designed canonical face; the Marshal is pinned to one face. Source: `.superpowers/sdd/2026-10-17-phase-2-houses/task-5d-faces-design.md`.

## Faces: the Templar and the weak faces
The Templar's great helm hides his head from every angle, so his four faces only show with the helm off. Recommended: wire one face for him now and keep the other three banked in art/ until a helm-off scene exists (cost: none). Self-grades of the canonical faces against the 9 bar: knight 8.5, longbow 8.5, mage 8, marshal 7.5, templar 7.5, archer 7. The archer's face is a small dark patch at 6 blocks; the marshal's variants 0 and 2 read alike. Say whether to spend a fix round on the archer, templar and marshal (recommended) or accept as is.

## Progression gate result: the level 3 count (2026-10-24, Task 11; the gate passed, one aim still open)
The in-match progression balance gate (design 9.2) passes both rules at every threshold tried, so it does not block 0.5.0. Per your round-35 answer I set the kill thresholds to 4 and 12, then took the one further step to 5 and 14, and stopped. Branch `progression-gate` (worktree `bannerbound-gate-prog`, tip `a4900fb`): `levels.l2.kills` 5 and `levels.l3.kills` 14 (were 3 and 8). Full log: `docs/PHASE-2-BALANCE.md`, section "In-match progression gate". 200 rounds per condition, levels on against the same rounds with levels off:

| thresholds | first level 2 | winner's soldiers at level 2+ at the end | level 3s a round | every House within 10 points | comeback on minus off |
|---|---|---|---|---|---|
| 3 and 8 (as designed) | 115 s | 43.9% | 49.5 | yes (largest move 2.3) | -4.5 points |
| 4 and 12 | 118 s | 41.2% | 32.0 | yes (4.9) | -0.2 points |
| 5 and 14 | 120 s | 35.5% | 27.0 | yes (3.5) | -0.6 points |
| aim | 120 to 180 s | about a third | a few | within 10 | within 10 |

1. **Level 3s are still 27 a round (13.5 a side) at 5 and 14.** The first level 2 and the winner's share are on the aim now; the level 3 count is the one miss. The cause is credit sharing: a kill credits 4.6 soldiers on average (the killer, helpers, supporters; 5% of kills credit 10 or more), so credits pile up fast. I can estimate higher level 3 thresholds from the credit log (soldiers whose running credits reached T, first order; it reproduces the measured 13.5 at 14 exactly): T 20 gives 16.6 a round, 24 gives 12.0, 28 gives 8.9, 30 gives 7.7, 35 gives 5.1, 40 gives 3.6.
   - Raise only the level 3 threshold: `levels.l3.kills` 14 to 30, level 2 stays 5 (recommended: one cell, the plan's first lever, about 7.7 level 3s a round). I would rerun the levels-on round robin once and the gate, about 2 hours; the rules should hold, since a higher threshold only moves the sim toward the levels-off baseline.
   - Accept 5 and 14 as they are (the gate passes, level 3 is the +10% bonus, 27 a round).
   - Throttle assist and support credit instead (one credit per support per few seconds; round 35 option b). That is a rule change and the root cause, so it is yours.
2. **FYI, no change made: fast ticks are not the same game as real time on one pair.** `gilded_normal` against `graveborn_normal`, levels off, 20 matches each: Gilded won 6 at fast ticks and 14 in real time (+0.40, 95% +0.16 to +0.64, p = 0.011). Kills and round length do not differ. The cause is unknown (fast mode only skips the wait between ticks). The round robin shares are therefore right for fast ticks and may be off for the real game by up to that much on a pair like this one. With levels on at 5 and 14 it is 6 against 12 of 20, the same gap, and the level timings in real time match the fast-tick ones (first level 2 108 s against 108 to 110 s). The gate's rule results hold either way, because on and off ran the same way.
   - Keep the round robin for finding which Houses and pairs move, and confirm every tuning step of the Phase 2 end balance pass on a real-time pair (recommended; costs 2 hours a pair).
   - Spend a session first on finding why fast ticks differ, before the end balance pass tunes on them.

Kort's answer (2026-10-24): yes to `levels.l3.kills` 14 -> 30 (level 2 stays 5). Done and measured on the gate branch (commit `fa59470`): both gate rules pass (largest share move 4.9 points, Gilded -4.9 and Graveborn +4.9; comeback +3.3 points), first level 2 121 s, winner's soldiers at level 2+ 37.0%, level 3s 5.9 a round (3.0 a side; the credit-log estimate was 7.7). All three aims are now inside their bands. Item 2 (fast ticks against real time) stays open. The baseline and all level settings ran on `cdea2c4`, before the Phase 3 gate routing became global (`155f2cf`): retake the levels-off baseline, and rerun the levels-on condition once, at the base merge.

## Phase 3 Task 8b: the supply-house flag cannot be seen from the lane
The house's white neutral flag is hidden by the spur climb on every map (only the lantern speck shows on Rivergate and Hollowwood), so "readable from the lane" in the plan is not met. I ruled: a taller house-only flag (pole about 11, cloth about y+9..10, lantern about y+12) and a lane ray check that can fail. Alternative: drop the sentence from the plan and keep the low flag. Also for you: the pinned step-hop (stops units jumping forever at a one-block step under leaves) is global AI and stays in (reviewed: no-op outside yards, needed for 2 of the house marches); Ironhold nw/se stand at elevation 77, not 76 (plan amended). Look in game at the lane end of one house per map.

## Answered 2026-10-24 (in chat): refuge stays a timber hut; taller supply-house flag; 3 texels for new art only; faces become simple default-Minecraft faces at 2 texels per px. Open follow-up: do you still want the four variants per troop (beards, mustaches, freckles, scars) in that simpler style? I kept them, chunkier.

(Answered 2026-10-24: keep the four face variants per troop, painted at 2 texels per px.)

## Supply-house flag heights
The lane check needs the flag to clear the spur crest, so pole height follows the house's elevation: Rivergate and Hollowwood 11, Kingsfield 12, Ironhold 13, Frostmarch 17 (beside a 9-high house). Alternative: one height for all houses, which has to be 17. Recommended: keep the formula. Check Frostmarch in game.
