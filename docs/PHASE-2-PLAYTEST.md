# Phase 2 playtest gate (Kort)

Branch `phase-2-houses`. Four sessions, about 20 minutes each. Tick `[x] yes` or `[x] no` on each line; the free-text answers are the ones that matter most. Nothing here is published; the build stays in the local `dist/`.

**Read first.** Phase 2 has no levels and no gear yet. The in-match progression plan (levels and gear) has not run, so Phase 2 sells no evolution in a match and a troop's tier I to III art exists only for the six Gilded troops. Version 0.5.0 ships only after that plan has run, its suites join the regression, and every troop has its tier I to III art. This gate tests what exists: Houses, rolls, the chase, the camp screens, migration, and the no-routing fights.

## Setup

1. Back up your camp first: your client copy `gravesandglory/collections/<your-uuid>.json` and the world's `saves/Graves and Glory/gravesandglory-profiles/<your-uuid>.json`.
2. Install the 0.5.0 build from the local `dist/` (`graves-and-glory-0.5.0.zip`) over your existing camp, so you see the migration. On first load your file becomes schema 2 and the original stays beside it as `<your-uuid>.v1.json`.
3. Expect: the 10 Commons, Sentinel, Longbow, Reaper and Stalker if you had them; the Sunweaver and Gravecaller (and the Oracle and Wraith if you had unlocked them) refunded as 300 gold each; your class mapped (Vanguard to Champion, Marksman to Warden, Arcanist to Warlock); your banner kept where it was valid.

## Where Phase 2 stands

- **Built:** 30 troops in five Houses (Gilded Order, Graveborn, Wildmoot, Ironclad, Tidecallers); the combat engine and the no-routing rule (a group never breaks; the LAST STAND stamp); six commander classes on key H; profile schema 2 with migration; rolls, chosen buys, stars, shards, Renown, Mastery and the Founders chapter; the recruit page, troop card and roll card; your own troop models and animation clips.
- **Not built:** levels and gear; art for 24 of the 30 troops (they draw as their bare rigs; the six Gilded skins are painted, their Mastery II trim set is graded 8.5 and provisional); each House's art half; EMF, ETF and Fresh Animations still sit in the modpack until the release step.
- **Known weak Houses (structural, report-only, not tuned):** Tidecallers and Wildmoot lose almost every bot match (see the appendix). Judge them as "unplayable until the balance pass", not as the design.
- **Mixed banners (Task 13 sims, outside 40 to 60, flagged):** a Gilded+Graveborn mix wins 1 of 20 against pure Gilded (the fights are level, the points are lost: mixing halves each House's trait slots); an Ironclad+Tidecallers mix wins 18 of 20 against pure Tidecallers (Tidecallers are the weak half). Session 3 is where you tell us whether a mix feels as weak or as strong as these numbers say.
- **Known test flakes (not game faults):** `p2-blight-gravecaller` is timing-flaky (burst tick off by one); the animation suite's platform light can flake and is bounded.

## Session 1: collection and rolls

- [ ] yes / [ ] no: your migrated collection shows on the recruit page, every House tab opens, and nothing you owned is missing.
- [ ] yes / [ ] no: your gold is your old gold plus 300 for each refunded caster.
- [ ] yes / [ ] no: a few rolls in different Houses show the roll card, with stars filling on a duplicate.
- [ ] yes / [ ] no: the troop page for a Rare reads clearly (stars, Mastery bars, three ability lines, beats and beaten by).
- [ ] yes / [ ] no: a chosen buy (300 gold) works and the card line "full stars" refuses a further buy.

## Session 2: a pure Gilded skirmish on Normal

- [ ] yes / [ ] no: infantry on Hold brace their shields, the Bulwark tile shows in the trait strip, and Aldric's banner (Lord Marshal) gives its aura. A migrated camp owns no Legendary and holds no Pact yet: clear every battle of a Long March chapter first (a Pact is earned only for a fully cleared chapter, so the first Pact takes the whole of chapter 1), then spend the Pact on Lord Marshal Aldric in the Gilded tab. Without a Pact, skip the Aldric part of this line and say so.
- [ ] yes / [ ] no: a line of yours holds to its last stand without breaking, and the LAST STAND stamp shows.
- [ ] yes / [ ] no: you pull a group out with Retreat, and its horn answers to you alone.
- [ ] yes / [ ] no: the battle report shows a House row with trait seconds, shards and Renown.

## Session 3: a mixed banner against Hard bots of a new House

- [ ] yes / [ ] no: you can build a banner from two Houses in the editor, and the trait tiers show for both.
- [ ] yes / [ ] no: the fight against Hard Wildmoot, Ironclad or Tidecallers bots is winnable with your mix, and you can tell why you won or lost.
- [ ] yes / [ ] no: a Legendary or Epic troop reads as special on the field.

## Session 4: the report and the chase

- [ ] yes / [ ] no: the report's House rows (tier, trait seconds, shards, Renown) match what you did.
- [ ] yes / [ ] no: Mastery and Renown move after a match of 3 minutes or more, and not for a shorter one.
- [ ] yes / [ ] no: the contracts and the chapter page claim to the House you chose.
- Levels and gear: not testable until their plan has run; once it has, play a match for levels and gear here.

## Questions

1. Did it feel good?
2. Which House read worst on the field?
3. Is the chase pace right (about 29 hours of play to own everything, median)?
4. Anything that surprised you in the migration?

## Appendix A: every balance gate line

All are bot-vs-bot, Normal bots, maps 0 to 4, 300 s rounds, 20 matches (10 a seat) unless noted. The band is 40 to 60 percent. Newest first.

```
gate mix_gilded_graveborn_normal vs gilded_normal            win=5%    fail  (Task 13, log only, OUTSIDE 40-60)
gate mix_ironclad_tidecallers_normal vs tidecallers_normal win=90%   fail  (Task 13, log only, OUTSIDE 40-60)
gate tidecallers_normal vs ironclad_normal   win=0%    fail  (Task 9, report-only, no-routing game)
gate tidecallers_normal vs gilded_normal     win=0%    fail  (Task 9, report-only, no-routing game)
gate wildmoot_normal vs ironclad_normal      win=10%   fail  (Task 10, report-only)
gate wildmoot_normal vs gilded_normal        win=0%    fail  (Task 10, report-only)
gate graveborn_normal vs gilded_normal       win=17.5% fail  (Task 8b, no-routing game)
gate graveborn_normal vs gilded_normal       win=32.5% fail  (Task 8b, its one tuning round: NOT committed, so the live sheet is the 17.5% line above; kept here for the record)
gate ironclad_normal vs gilded_normal        win=60%   pass  (Task 8b)
gate graveborn_normal vs gilded_normal       win=47.5% pass  (balance fix: Squire 28, Templar 40; with routing)
gate ironclad_normal vs gilded_normal        win=75%   fail  (same fix, report-only)
gate ironclad_normal vs gilded_normal        win=45%   pass  (Task 8, report-only, spec numbers)
gate graveborn_normal vs gilded_normal       win=15%, 0%, 0% fail  (Task 7: spec numbers, then tuning rounds 1 and 2)
gate gilded_normal vs graveborn_normal       win=100% fail  (Task 6, report-only)
gate gilded_normal vs graveborn_normal       win=92.5% fail (Task 3 baseline after fix round 1; the first run read 95%)
```

Round robin of the four pure Houses (120 matches, no client): Bradley-Terry shares Gilded 82%, Graveborn 58%, Ironclad 53%, Tidecallers 7%. Clash order Ironclad, Graveborn, Gilded, Tidecallers. Tidecallers and Wildmoot are structural items for the end balance pass (does the bot AI use the harpoon pull, Mist, Undertow, Brace and the hounds at all; range and a shield answer are Kort's design call).

## Appendix B: economy model

`python tools/economy_model.py` (20000 runs, seed 4909, roll 100 gold, chosen buy 300, Epic pity 40). Income about 492 gold an hour (matches 400, daily contracts 67, weekly 25); chapter gold and the tier-20 Rare are one-off.

| Player | Gold needed | Hours |
|---|---|---|
| Lucky (10th percentile) | 10,400 | 21 |
| Median | 14,100 | 29 |
| Mean | 14,100 | 29 |
| Unlucky (90th percentile) | 17,600 | 36 |

| Hour | What just happened | Next goal on screen |
|---|---|---|
| 0 to 2 | campaign chapter 1, 10 Commons owned, Pact spent | first Rare (300 gold or rolls) |
| 2 to 6 | 2 to 4 Rares, first stars on Commons | first Epic (pity counter shown) |
| 6 to 12 | first House complete except its Legendary, Renown III | Renown V in that House |
| 12 to 20 | second and third Houses, Legendary #2 | star 3 on a favourite |
| 20 to 29 | last Epics, Legendaries #3 to #5 | Mastery II veteran trim, star 3 on favourites |
| 29+ | roster complete | star 3 across a House, Mastery II and III |
