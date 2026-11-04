# Animation roadmap (2026-11-03)

## 1. Intro

This is the overnight result: one spec and one plan for each of 16 unit families. Nothing is built yet; no clip has been authored from these. The order is most troops first (bow, caster, spear, sword), then the single-unit families, then the animals and the golem.

Grading: a Sonnet reviewer graded each spec out of 10. Each spec got at most two fix rounds, and the final grade is the one after round 2. Caster and greatsword stopped after round 1 (their v2 is final). Final scores run 6.5 to 7.8, so none is "done"; each spec lists its known gaps.

## 2. Table

Moves = clips planned (attacks plus stances, plus walls, hits or death where the spec plans them). Grades are v1 / v2 / final.

| family | units | reference videos | moves | grades | blocked on |
|---|---|---|---|---|---|
| bow | archer, longbow, bonebow, stalker, wm_hunter | Essential Bow & Arrow Pack (space.bar) https://www.youtube.com/watch?v=FEWzL31g3Eo ; Horizon Archery Test (mood) https://www.youtube.com/watch?v=6MW9t4hZkik | 3 (bow_direct, bow_volley, aim) +3 hits if you say yes | 6.5 / 7.3 / 7.8 | Only archer and longbow have models; other 3 are vanilla. Arm sign check gates the tables. |
| caster | mage, hexer, wm_seer, tc_mist | Essential Magic Staff Pack https://www.youtube.com/watch?v=abzQNrqeExk ; DS3 Sorceries https://www.youtube.com/watch?v=0Vyx-WZ8bRQ ; Riko Mage Staff Animset https://www.youtube.com/watch?v=96GEiO0qu7k | 4 (caster_cast, idle, guard, aim) | 6.8 / 7.0 (final) | Only mage has a model; hexer, wm_seer, tc_mist cannot play clips. |
| spear | wm_tusk, tc_tideguard, tc_admiral | Thunder Cloud Spear Animation Demo https://youtu.be/Yk4NFoRhw1g | 10 (3 thrusts, 3 wall versions, brace, guard, carry, idle) | 6.5 / 6.2 / 6.5 | No models on this branch (they are on origin/work/troop-models-house; Admiral has none). Stub clips. Brace numbers need re-deriving. |
| sword | revenant, tc_raider | Sharp Accent sword example https://www.youtube.com/watch?v=NwHzWyKyIs4 | 6 (overhead, low cut, thrust, idle, carry, guard) | 6.2 / 7.3 / 7.3 | Models are on `main` (merge needed). The forward lean (+15 deg) re-sums the key tables. anim_edit entry. Stub clips. Root y sign table is flipped. |
| crossbow | ic_arbalist, ic_ballista | Elden Ring Bow/Crossbow/Ballista https://www.youtube.com/watch?v=Qvo487abShM ; Ironbelly Arbalest https://www.youtube.com/watch?v=4YlwCnuqFBM | 5 (crossbow_loose, idle, carry, aim, rest) | 6.2 / 6.2 / 6.5 | No troop models. TroopMotion.bow fix (Java). anim_edit entry. Reload and window numbers need fixes. |
| axe_shield | ic_hearthguard | RBStudio 650+ Axe+Shield https://youtu.be/8jiz-5jf15U ; Hammerhead Axe and Shield https://youtu.be/KB-SWLjRgzs ; Mythforge axe https://www.youtube.com/watch?v=y8Qez-K8VhE ; Graves Shield pack https://www.youtube.com/watch?v=MJkHBwryr7Q | 6 (overhead chop, hook hew, 2 wall versions, 2 stances) | 6.3 / 7.2 / 7.0 | No troop model. anim_edit entry (coordinator). Stub clips. Hew yaw signs inverted. |
| axe | ic_breaker | Elden Ring Full Greataxe Moveset (Dare To Game) https://www.youtube.com/watch?v=KVRG_kIZlfs | 6 (chop, side hew, haft jab, guard, idle, carry) | 6.3 / 6.3 / 6.5 | No troop model. anim_edit entry and an axe.bbmodel. Stub clips. Haft pitch numbers contradict the "level" claim. Jab has no video. |
| greatsword | gb_hollowking | GameDevHero Great Sword V1 Demo https://youtu.be/65oeK8PbyEk ; RamsterZ Greatsword Animations https://youtu.be/54zN63HgMq8 | 6 (cleave, side cut, thrust, guard, idle, carry) | 6.2 / 7.5 (final) | No Hollow King model, skin or grave_sword. Thrust has no video. Needs a stand-in (Marshal) to grade. |
| scythe | reaper | Wolff's Reaper Animations https://www.youtube.com/watch?v=uT3M0MZxwS0 ; Elden Ring Scythe Showcase (mood) https://www.youtube.com/watch?v=CBb-5zS2WBA | 5 (reap, overhead hook, guard, idle, carry) +2 wall versions if you say yes | 7.0 / 7.3 / 7.5 | No reaper model; nothing plays without one. Reach is short (1.5-1.7 m vs range 3.0). |
| thrower | tc_harpooner | Awakening Spear Throw https://youtu.be/5VTQCulDktA ; Thunder Cloud Spear Demo https://youtu.be/Yk4NFoRhw1g | 4 (thrower_throw, idle, carry, aim) | 6.5 / 7.3 / 7.5 | Model is on branch art/tidecallers-t1, not here. TroopMotion.bow (Java) blocks every render. |
| crew | ic_mortar | Elden Ring ballista crank https://www.youtube.com/watch?v=Qvo487abShM | 5 (crew_fire, idle, carry, aim, rest) | 7.2 / 6.7 / 7.2 | No ic_mortar model. Swing timing vs the 30-tick windup. TroopMotion.bow (Java). Fire, reload, recoil have no footage. |
| hound | wm_hound (also levels 2, 3) | ROG Creatures Wolf https://www.youtube.com/watch?v=eZ6g7BfuyRo ; Gray Wolf preview https://www.youtube.com/watch?v=9ZgLlGkoNo4 | 7 (bite, lunge, idle, guard, 3 hits) | 6.5 / 7.0 / 7.5 | No model. anim_edit needs a rig change. No quadruped gait. Bite and lunge wind-up have no video. |
| spider | wm_stalker | Skyrim Giant Frostbite Spider https://youtu.be/ZArGBAsVHlI | 9 (bite, lunge, idle, guard, carry, 3 hits, death) | 6.2 / 6.2 / 7.2 | No model. anim_edit needs a rig change. No 8-leg gait. No run.sh case. Fangs never shown in footage. |
| bear | wm_oldhorn | Elden Ring Runebear https://youtu.be/yE3Nf2XMuNU ; Rageclaw Grizzly https://youtu.be/938Px9NRK5Q | 11 (maul, bite, rear, death, 3 hits, rest, idle, guard, carry) | 6.5 / 6.7 / 7.5 | No bear rig or model (rig change in TroopModels). anim_edit rig change. No quadruped gait. |
| ravager | tc_brute | gim studio Rhinoceros preview https://www.youtube.com/watch?v=vJAlnB0O3KM (no real Souls-like video found) | 6 (charge, bite, idle, carry, guard, rest) +hits later | 6.0 / 6.5 / 6.5 | No model (hound rig base unconfirmed). anim_edit rig change. No quadruped gait. |
| colossus | ic_colossus (levels 2, 3 share it) | Giant Golem AnimSet https://www.youtube.com/watch?v=huee6syEzuM ; Riko Giant Monster Animset https://www.youtube.com/watch?v=19wwArriGXI | 7 (slam, backhand, idle, guard, 3 hits) | 7.7 / 7.3 / 7.5 | No model. anim_edit rig change. Stub clips. Backhand has no video. Soles go 1-2.6 px under ground. |

## Correction (2026-11-03, coordinator)

The specs were written on `work/anim-rework`, which is 351 commits behind `main`. On `main`, `models/troop/index.json` lists all 30 troops. So every "no model" blocker in this roadmap and in the family specs is stale: the units have models, skins and weapons on `main`.

| family | model on `main` | weapon | rig on `main` |
|---|---|---|---|
| sword | revenant, tc_raider | notched_sword, cutlass | biped |
| spear | wm_tusk, tc_tideguard, tc_admiral | boar_spear, coral_trident, admiral_trident | biped |
| crossbow | ic_arbalist, ic_ballista | ic_crossbow, none | biped |
| axe_shield, axe | ic_hearthguard, ic_breaker | hearth_axe, ic_axe | biped |
| greatsword, scythe | gb_hollowking, reaper | grave_sword, scythe | biped |
| thrower, crew | tc_harpooner, ic_mortar | harpoon, none | biped |
| hound, bear, ravager | wm_hound, wm_oldhorn, tc_brute | none | **hound** (bear and ravager share the hound rig; the bear spec's new bear rig is not needed) |
| spider | wm_stalker | none | spider |
| colossus | ic_colossus | none | colossus |

The sword spec and plan are re-checked against the real models; Task 0 is now "merge `main`". The other families still need the same check after the merge: rest lean, idleArms, weapon model, cloth parts. Several models lean forward at rest: tusk +20, raider +16, revenant +15, reaper +7. Their key-table chain sums must include that lean.

## 3. Questions for Kort

"(blocks build)" means the family cannot be built or seen in game until you answer.

**bow**
1. Own hit clips for archers that keep the bow on the target, or stay on the shared ones?
2. Should the volley angle follow the server's real launch pitch? It is a protocol change and needs your approval.
3. Add a `carry` clip (bow low, tip down), or keep the procedural march?
4. A bow-specific death, or the common set?
5. Build models for bonebow, stalker and wm_hunter? Today they never play bow clips.

**caster**
1. Model hexer, wm_seer and tc_mist next, or keep the vanilla look?
2. Low battle-mage crouch for `guard`, or keep it upright like idle?
3. Is the glowing end of the sun staff the short end (up when held at the hip)? Checked on a render.
4. Should the heal use the same cast pose as an attack?
5. Deaths stay common unless you ask for a caster death (staff dropped).

**spear**
1. Wait for the troop-models-house branch to merge, or author blind on the biped rig and render later? (blocks build)
2. Shared clips for all three, or heavier ones for Admiral later? Default: shared.
3. Undertow has no cast pose; leave it as the plain thrust? Default: leave.

**sword**
0. May Risen and Drowned Raider get troop models? This changes how they look. (blocks build)
1. Is one 480p watermarked video enough, or fetch the Sebastian Graves sword video?
2. Should Risen move differently from the Raider (needs a second clip set)?
3. Keep the low knee-to-hip cut (default) or a chest-high side slash?
4. Taunts, guarded advance and hits for swords later, as for the knight?

**crossbow**
1. Ballista Crew shares the Arbalist's clip, or gets its own? Answer first.
2. Narrow `TroopMotion.bow` to the bow family (Java)? It also changes the thrower. (blocks build)
3. Schedule the Arbalist and Ballista models before or with the clips? (blocks build)
4. Keep the split: shouldered for aim and loose, chest-level for idle and carry?
5. Fix hits only if the shared ones clip with a crossbow in hand. OK?
6. Aim and loose have no reference. Happy with authored ones?
7. Ballista frame stock cannot follow the arm drop; settle after Q1.

**axe_shield**
1. Two 640x360 mannequin videos are the only refs. OK, or do you have a favourite?
2. Should the hook only look like a pull, or do you want a gameplay pull (needs approval)?
3. Idle: keep folded arms, or open it like the knight?
4. Add `_wall` versions of both attacks?
5. Copy the knight's hit and block clips once they land?

**axe**
1. Build a custom two-handed great-axe model, or keep the vanilla netherite axe? The jab depends on it.
2. The video has no haft jab. Supply one, or keep the designed jab?
3. Wall or brace poses for the Shieldbreaker?
4. Accept a shallower crouch (3 px) than the video?

**greatsword**
1. No Hollow King model yet. Model first, or build now and grade on a Marshal stand-in? (blocks build)
2. Idle with the blade point down (Souls rest). Yes? Recommended.
3. Accept a composed thrust (no video), or find one?
4. Hits stay common, no block. Overrule?
5. Brace and wall stay procedural (he does not hold a line)?
6. Guard has the blade up and back over the shoulder, as the video, not toward the enemy. Accept?
7. Side cut sweeps flat right to left; the video rises to vertical. Accept flat, or mirror?

**scythe**
1. Build a reaper model with a scythe part? Nothing plays without it. (blocks build)
2. Own hit clips (haft kept in both hands), or common? Recommend: check on render first.
3. Add `_wall` swings and a tight wall/brace hold (recommended)?
4. Composed carry (haft on right shoulder) OK without a video?
5. Accept the composed overhead hook, judged in game?
6. Reach: lengthen the scythe to about 3.3 m, or accept a visibly short reach?

**thrower**
1. Merge the Tidecallers models (branch art/tidecallers-t1)? Nothing can be seen without it. (blocks build)
2. Start the throw a beat early (as the bow does) so the wind-up shows?
3. Hide the held harpoon from release to about 0.9 s, or show a spare from the quiver?
4. Restrict `TroopMotion.bow` to the bow family (Java)? Blocks every render, answer first. (blocks build)
5. Thrower-owned hit clips so a hit during aim does not drag the harpoon?
6. Level over-the-shoulder javelin throw, or a heavier two-hand overhead hurl?

**crew**
1. Barrel look: muzzle up and forward over the right shoulder (assumed), or up-back, or on the ground?
2. Give the barrel its own bone so it can kick, or is root dip plus smoke enough?
3. Does the server send `crew_fire` at the start of the 30-tick windup or when the shell leaves? (blocks build)
4. Happy with an authored pull, shot and reload (no footage), or find a muzzle-loader video?
5. Narrow `TroopMotion.bow` for crew too (same Java change as the crossbow). Approve?
6. Schedule the `ic_mortar` model before or with the clips? (blocks build)
7. Ballista Crew is covered by the crossbow spec.

**hound**
1. Accept the designed bite (refs show a crouch and a leap only), or supply a reference?
2. Build the hound model? And for walking: hand-keyed leg loop on `carry`, or an engine gait? (blocks build)
3. Let `anim_edit.py` take a rig argument (tool change, no game change)? (blocks build)
4. Brace or wall poses for hounds? Default: no.
5. Small hit set now, death later?

**spider**
1. Switch the Web Stalker from the vanilla renderer to a troop model? Needed for any clip. (blocks build)
2. Walking: hand-keyed 8-leg `carry` loop at fixed speed, or an engine spider gait (Java)? (blocks build)
3. Weak footage (first-person, fangs never shown) acceptable, or supply a side bite clip?
4. Should the lunge visibly cover 12 px as a root offset?
5. Web puff on hit for `ensnare`, or none?

**bear**
1. Build a bear rig and `wm_oldhorn` art now (recommended), or keep the vanilla bear and skip clips? (blocks build)
2. The maul telegraph shrinks from 2.3 s (video) to 0.35 s (impact tick is fixed). Accept, or let the server start earlier (gameplay timing)?
3. Quadruped gait: clips for the bear only, or a layer for all hound-rig troops? (blocks build)
4. Is a purchased pack (Fab polar bear, Unity Wild Bear) OK as a clearer jaw reference?

**ravager**
1. Real model on the hound rig plus a horn part (recommended), or its own rig? (blocks build)
2. Is a Java four-legged gait in scope? (blocks build)
3. Charge style: head-down ram and toss, or shoulder barge?
4. Hit and death clips in this pass?

**colossus**
1. Is "two fists up and down" for the slam and "back-of-hand sweep, 20 degrees below level" for the backhand right, or do you have a link?
2. Vanilla iron golem look for the model, or new art? (blocks build)
3. Procedural collapse death, or a heavy death clip?
4. No wall or brace pose (uses idle and guard in line orders). OK?
5. Add dust or crack at the slam impact? Gameplay-adjacent.

## 4. Shared blockers

- **anim_edit knows only the biped families.** `tools/anim_edit.py` line 29 lists `sword_shield`, `bow`, `common`; it fixes `RIG = 'biped'`. Biped families need a new TROOP entry (and a .bbmodel): spear, sword, crossbow, axe_shield, axe, greatsword, scythe, thrower, crew (caster has a bbmodel, check its entry). Animal and golem families need a rig change: hound, spider, bear, ravager, colossus. The coordinator must do this.
- **No troop model on this branch** for: bonebow, stalker, wm_hunter, hexer, wm_seer, tc_mist, wm_tusk, tc_tideguard, tc_admiral, revenant, tc_raider, ic_arbalist, ic_ballista, ic_hearthguard, ic_breaker, gb_hollowking, reaper, tc_harpooner, ic_mortar, wm_hound, wm_stalker, wm_oldhorn, tc_brute, ic_colossus. Only archer, longbow and mage have models (plus knight, gl_marshal, sentinel, not in these families). Spear and thrower models exist on other branches.
- **No quadruped gait** (procedural gait is biped only): hound, spider (8 legs), bear, ravager. Options are hand-keyed `carry` loops or a Java gait layer.
- **Stub clips.** `tools/clips.py` (about line 396) fails until every attack id has a clip, so each family needs zero-motion stubs first: spear, sword, axe_shield, axe, hound, spider, colossus, crew. Crossbow and scythe plans touch this too; the crossbow loose is a bolt, so it may not need stubs.
- **Hit-over-attack check is hard-wired to sword_shield** (`MotionEngineCheck.hitOverAttack`, 15 degrees per tick). Every family needs its own copy: all 16 specs name it.
- **`TroopMotion.bow(row)` (line 146) is true for every ranged non-magic troop**, so the bow hand twist runs on the crossbow, thrower and mortar crew. One Java change fixes all three (narrow it to the bow family). Crossbow, thrower and crew each propose it; do it once.
- **`run.sh` needs a tag as its first argument** (e.g. `CLIPS=... bash run.sh oldhorn solo_oldhorn`), or it runs nothing. Spider also has no `solo_stalker` case; spear brace pair render has none either.
- **Bear spec supersedes hound spec line 33.** Hound line 33 says `TroopModels.RIGS` maps wolf, polar_bear and ravager to the hound rig. The bear plan changes `polar_bear` to a new `bear` rig (wolf and ravager stay on hound).
- **Invented numbers with no footage.** Hound bite, spider fangs, crew fire and reload, spear `_wall` and low sweep wind-up, greatsword thrust, scythe hook, colossus backhand are designed from the rig. Plan to review these first in game.

## 5. Suggested build order

1. **bow.** Archer and longbow have models, the bow bbmodel exists and anim_edit already knows it. No Java change is needed to start. Begin with the arm sign check (Task 3).
2. **caster.** Mage has a model and a bbmodel. Same tooling path as bow.
3. **Biped families once a model exists, in this order:** axe_shield (one troop, single-unit tooling change, spec 7.0), sword (7.3, two common troops, needs models), spear and thrower (models already drawn on other branches; merge first), scythe (7.5, needs a model), greatsword (can grade on a Marshal stand-in), axe.
4. **crossbow and crew,** after the one Java `TroopMotion.bow` change, and after the models and the crew timing answer (Q3).
5. **colossus.** Best-graded of the non-biped set (7.5) and no gait is needed (it is a walking golem), but it needs the rig change in anim_edit and a model.
6. **hound, then bear, ravager and spider last.** All need the anim_edit rig change, a model and a quadruped or 8-leg gait. Do the shared gait and rig tooling once for all four, hound first (the others reuse its rig). Spider needs its review to finish first.

Why: families with models and existing tooling give real clips with no coordinator work. Families blocked on models or the gait cannot be seen in game, so their work would stay on paper.

## 6. Not yet approved

The knight hits v2 is uncommitted and waits for your approval: 9 hit clips (`hit_body`, `hit_head`, `hit_legs`, each with `_2` and `_3`) in `clips/biped/sword_shield/`, with the worst step 14.7 degrees per tick against the 15 limit. The sword and axe_shield specs both depend on these files landing.

## 7. Kort's rulings (2026-11-04)

- Bow is built (b711c5f3: slower shot, face clearance, hips settle into the draw).
- Merge `main` into `work/anim-rework` now, so every troop model is here. This answers every "no model" blocker above.
- Narrow `TroopMotion.bow(row)` to the bow family, so crossbow, thrower and crew get their own carry. Visual only.
- `tools/anim_edit.py` takes a rig argument (hound, spider, colossus rigs) and gets entries for the biped families. Tool change only.
- Beasts walk with one shared Java gait layer (ruled 2026-11-03).
