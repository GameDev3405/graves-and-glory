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

## Caster fix round 1 (2026-11-10, branch anim/fix-caster)

- **death_right:** the rebuilt fall is deployed, with the wand's world turn capped at 30 deg per tick (`FOCUS_WORLD_TURN`). ang_speed_weapon is 60.7 against a limit of 83.2, and there is no snap on tick 0. The scorecard has 35 clips, 0 failing.

## Batch review 2026-11-11 (branch anim/review-caster)

Kort's four focus points, checked with `tools/anim/focus_check.py`; additive clips also judged on the guard
(`work/review/compose.py`). The family-wide part of the fix (life pass, reaction layers, front blow, stun loop) is in
MAGE-REVIEW.md, same date.

- **Baseline:** the rebuild at the lane commit matched `out/` clip for clip (CRLF only): no drift.
- **Rows:** lead's first run 24 clips with a FAIL (55 rows). Before, composed: 20 clips (40). After, composed: 14 clips
  (15 rows), 13 of them the shoulder tusks (below). Scorecard 35 clips, 0 failing.
- **Changes:**
  - **Life pass** on the cast, howl, every hit, stun, stun_out, knockback, pulled: the feather mantle, shawl, sash tail, the
    robe panels and their fringes swing (all stood still before). The mantle and shawl are short and stiff: soft_motion drove
    them into its clamp and they flipped end to end in a sample (pops 14-21 deg in howl, the side blows, pulled), so on this
    troop their swing may turn at most 2.5 deg a sample (`LIFE_RATE`, run both ways and averaged).
  - **attack_cast (whole_body 0.67, dead_hold):** the hips load back, carry over the gliding right foot and drive into the
    release; the settle dips the body and the wand tip and a breath brings it back up (tick 29 key, tick 32.5 key), it froze
    3.5-5 ticks before.
  - **howl (grip_roll 20.4 at t21.75):** the wand comes back from wide right through a key half way, aimed half way.
  - **Blows:** a blow adds 0.4 of its outward swing to the guard's free arm, already 45 deg out (shoulder 122 in medium back).
  - **Floor:** stun hips up 1.38 px (hoof 1.68 under), knockback 1.02, pulled 1.63 (judged with the pull played on the guard:
    the hoof was 1.2 px under there and not on the bind pose), fallback 0.78 (hoof 1.08 under).
  - **Stances:** brace off arm 25 deg lower and 20 out (cuff 3.1 px in the head), ward wand fist 0.6 px out (cuff in the
    shawl 2.2) and the off forearm 10 deg lower; rally (stances.py layer) wand arm 15 forward and 20 out (cuff 3.3 in the head).
  - **Stun loop:** the first tick eases out of the lap's last pose.
- **Rig difference (proposed exemption):** wm_seer, every upper clip, armour_clip head into left_shoulder_tusks 2.3-2.9.
  The inner tusk cube (x 4..5) is flush against the head box (x -4..4) at bind; head and arm nudges barely move the value.
  Frame `work/review/frames/wm_ready_38`.
- **Proposed exemption:** death_forward shoulder 106 at t20: prone, the arm forward past the head (`frames/wm_df_20`).
- **Open:** stun_out_upper secondary_life 1.35: two skirt fringes (in the stun legs clip) hardly swing over the stun_out
  window, since the stun loop now starts on its lap pose; stun_out's own pieces swing.
- **Looked at:** all ticks, three views: attack_cast, howl (`work/review/frames/all_wm_*.jpg`); the brace, ward, rally
  and tusk ticks at full size.
- **Tests:** unit tests and verify fast as in MAGE-REVIEW.md (the same builder; `test_where` timing under machine load).
  In game: geo-knight suite through checkq, root `wm_seer-review1`: 22 `p1-geo-wm_seer` lines and the suite line all true,
  0 `=false`.

### Re-check 2026-11-12 (tools merged at c0cb68a0: hits and stun_out judged over the stance)

- The new focus check plays every hit over `stance_ready_*` (as `work/review/compose.py` did) and `stun_out_upper` over the
  stun handing over to the stance in 8 ticks. The one row it found that the old tool hid, on all four casters:
  `stun_out_upper` secondary_life (robe panels, keyed by the legs clips, hung still over the handover: mage 1.76,
  hexer 0.95, tc_mist 1.42, wm_seer 1.05). Open, named: the stun_out life pass is now driven by the stun legs blending into the
  stance legs (`_handover_legs`), which swings the upper cloth, but the robe panels belong to `stun_legs` /
  `stance_ready_legs`, and the scorecard plays `stun_out_upper` with `stun_legs` and needs disjoint bones (exit 2 when
  the panels were keyed in `stun_out_upper`). Fixing it needs the panels' swing in the legs clips (shared reactions.py /
  stances.py) or a scorecard that plays stun_out over the handover; both are outside this lane.
- The layer pass (`GG_CAST_REVIEW=layers`, documented in the builder's "Batch review" block) rebuilds every layer fresh
  from reactions.py / stances.py before its own pass: running it twice gives the same bytes (checked on mage).
- `out/` holds the builder's 40 clips; the exported asset holds those 40 byte-identical plus the 17 shared layers
  (reactions.py, stances.py, charge.py), as before this review.
- Rule 6: hold (stances.py, gear `staff`/`focus`) holds the caster's focus forward; brace, wall, ward and shield_up are this
  builder's caster poses. The cast's `impact` stays tick 10, the release key (the bolt leaves).
