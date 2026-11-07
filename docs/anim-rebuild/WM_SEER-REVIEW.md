# wm_seer review record (2026-11-06)

The Bloodhowl Seer (wm_seer), the Wildmoot caster and the pilot for caster style v2 (`docs/anim-rebuild/CASTER-V2-DESIGN.md`). It holds a 6 px bone wand with a skull finial in the right fist, wears a hanging robe, and keeps its totem staff strapped to its back. Its clips come from the caster builder (`art/anim/caster/build_caster_anims.py`, `GG_CAST_TROOP=wm_seer`). The mage's, hexer's and tc_mist's built output is byte-identical; every wm_seer change branches on the troop.

- **Robe:** the source skirt and hem fringe are rebuilt as four hanging panels under the hips (front, back and two sides; `troop_rig.ROBE`, `robe_panels`). Each panel swings with the leg behind it, and the hem rides above the floor. The legs keep their bones but lose their boxes, so only the hooves show (they ride the feet). The robe art is reused: a panel's outer face is the old skirt paint, and its inner face is the side paint darkened as a lining.
- **Clips:** guard, idle, short gliding walk (6-tick steps) and run (5-tick steps), stances, hits and deaths, plus `attack_cast` (36 ticks) and `attack_cast_legs`. The timeline `impact` marker sits on the server's release tick (10).
- **Cast (point and shoot):** the off hand rests at the hip by the belt throughout.
  - **Ticks 0-4:** the shoulders turn side-on and the wand hand comes up past the chest. The feet glide under the robe.
  - **Ticks 4-9:** the arm extends straight at the foe with the elbow locked and the wand level along it. The wand holds steady through the charge.
  - **Tick 9.4:** the wrist cocks the tip up 22 degrees.
  - **Tick 10:** the wrist flicks it 8 degrees down at the foe (a 30-degree flick), and the bolt leaves the tip.
  - **After the release:** a small recoil runs back through the shoulder (the fist kicks up and back, the torso rocks back), then the seer returns to guard.
  - **Probe:** fist error at most 0.030 px, 0 samples with the wand in the body.
- **Stances:**
  - **Brace (Hold position):** upright, both arms up, the wand held overhead crosswise as a guard bar, the off hand raised flat to the other end. No crouch.
  - **Wall (Hold the line):** leaning in, the wand pointed up and out at the foe at arm's length, the open off hand pushed forward below it.
  - **Ward (fall back):** turned away, the wand slanted low across the body, the off hand up beside the face.
- **Carry:** walk and run carry the wand butt 13.6 px and 12.7 px above the ground, with 0 samples in the body. The totem staff stays strapped to the back.
- **Deaths:** the shared caster falls, with per-troop key tweaks (`DEATH_TWEAK["wm_seer"]`) so no limb props the body up and the robe folds flat with the body.
  - **Back:** the arms settle wide and flat.
  - **Forward:** the head turns aside onto the cheek, so the snout does not prop it.
  - **Side rolls:** the under arm lies straight out past the head instead of bent beneath.
  - **Rest heights (min-y, px):**

    | Fall | Rest heights |
    |---|---|
    | Back | torso 0.29, hips 0.94, head 2.04, arms 2.03 |
    | Forward | head 0.03, hips 0.0, torso 2.41 |
    | Left | hips 0.14, arms 0.0, torso 4.91 |
    | Right | hips 0.14, arms 0.0, torso 4.91 |

    The side-fall torso is held up by the wider robe and mantle.
- **Gameplay (Kort approved, 2026-11-06, cosmetic only):** the tip origin. A caster's drawn shot (a bolt or a mend) now starts at the tip of its prop on the release frame.
  - The builder samples the tip at the release (`tip_json`, `CAST_TIP`). `export.py` copies it to `data/gravesandglory/cast_tip/<troop>.json` as a body-frame offset in blocks.
  - wm_seer's offset is (-0.213, 1.28, 1.227) for the t2 wand, with t1 z 1.104 and t3 z 1.351.
  - `Projectiles.castFrom` turns the offset by the body yaw and sets it on the feet. A troop with no cast_tip keeps its eyes, which was the old origin.
  - Arrows, flight ticks, arc, damage, targeting and cooldowns are unchanged.
- **Checks:**
  - `GeoKnightChecks` adds a `SHOT` flag: a caster whose attack beat shoots a bolt and whose ability is not a spell. Its duel is ranged, released through `Tactics.shoot` and announced a beat ahead.
  - `GeoTroopRenderer` lists wm_seer.
  - **Robe probe** (every clip, deaths included): worst leg-line margin 0.75 px, deepest hoof poke 0.00 px, lowest live hem 0.15 px above the floor, 0 failures.
  - **Identity check:** the mage, hexer and tc_mist outputs are byte-identical to the pre-change builder.

## Review

- **Round 1 (Sonnet high, reel `wm_seer_v2/reel2`): FIX.**
  - **Claimed blocker:** "the wand stays vertical in the fist (thumbs-up)". A full-resolution side zoom showed this was a misread: the shaft is level along the forearm, and the vertical shape is the t3 skull finial's two upright horn prongs overlapping in side view. The tip json confirms a level, forward tip.
  - **Real finding:** the wrist flick was barely readable. It was widened to a 30-degree cock and flick, and the tip was regenerated.
  - **Minors:** a wide run stride; a small brace wand at sheet scale; a strong ward head roll in the front view; a lumpy hem on the hit from behind in three-quarter view; the side-fall torso resting at 4.9 px.
  - **Confirmed to spec:** the attack phases, the off hand at the hip, the recoil, an upright arms-up brace, a distinct wall and ward with no shield language, a robe that shows only the feet, the totem strapped on, and all four deaths flat with nothing floating.
- **Round 2 (Sonnet high, reel `wm_seer_v2/reel3`, video `wm_seer_v2.mp4`, side zoom strip): SHIP WITH MINORS.**
  - The reviewer withdrew the wand blocker: the wand reads as pointing at the target, with the arm locked and the shaft horizontal from tick 8 to 16.
  - The flick reads in close-up (the cock at 9.0-9.5, the drop on tick 10), easing to guard with no pop.
  - All round-1 spec checks still hold.
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false.
  - 35 of 35 cast frames were caught in the side and three-quarter views, and 17 of 17 far.
  - All 9 standing and 4 walking casts matched the server release.
  - Weight at the release is 1.0, and 0 between casts.
  - Stances, hits and deaths pass in 3 views. The walk and run march at the true speeds (0.187 and 0.292).
  - The first run failed 3 march checks. The client's measured speed jittered 0.099-0.376 under "Can't keep up" server lag on a loaded machine. That check reads only entity motion (`Gait.update`), not the clips. The clean re-run (no lag) passed all 22.
  - **Tip origin in game:** the server logged `cast tip wm_seer: from (-287.287, 66.28, 6.273) eye (-287.5, 66.79, 7.5) body yaw 180`. Facing north, the bolt starts 1.227 blocks forward and 0.213 to the seer's right (the wand hand), 1.28 above the feet, 0.51 below the eye.
- **Verdict: finished.** Video: `wm_seer_v2.mp4`.

## Open minors (polish, not blocking)

- **Wand silhouette:** the skull finial's horn prongs read as a thumbs-up at small size. This is reused art, per the spec. A few degrees of forward wand roll would break up the silhouette.
- **Flick at game distance:** it reads mostly as a brief tip twitch. A 10-degree higher cock, or a 1-2 tick hold at the cock, would carry it further.
- **Run:** the stride is about 15 percent wide for "short gliding steps". It is tied to the movement speed, and shortening it would make the feet slide.
- **Brace:** the 6 px wand is hard to read as a crosswise overhead bar at sheet scale.
- **Ward:** the head roll is strong in the front view.
- **Hit from behind:** the hem looks lumpy in the three-quarter view. There is no poke-through.
- **Side falls:** the torso rests about 4.9 px up on the wider robe and mantle. Check it against the ground shadow in game.
