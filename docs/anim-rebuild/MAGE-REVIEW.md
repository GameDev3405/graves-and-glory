# mage review record (2026-11-07, caster v2)

The Sunweaver Mage, the first caster: a 35 px sun staff held in the right fist, robes and a cowl. Its clips come from the caster builder (`art/anim/caster/build_caster_anims.py`, `GG_CAST_TROOP=mage`). Its rig comes from `art/anim/common/troop_rig.py mage`. It was redone on 2026-11-07 in caster style v2, following the wm_seer pilot (`WM_SEER-REVIEW.md`) and the mist caller (`TC_MIST-REVIEW.md`). The wm_seer, tc_mist and hexer built output is byte-identical; every mage change branches on the troop (`ROBED` now covers the mage too).

## v2 (2026-11-07)

- **Robe:** one hanging skirt rebuilt as four panels under the hips (front, back and two sides), each in three stepped tiers (`troop_rig.ROBE["mage"]`).
  - The mage's source robe was built per leg. The right side's tiers paint the whole skirt round both legs, and the left side's are dropped (`drop`). Each tier keeps its own paint (per-tier painter in `robe_tiers`).
  - The panels pivot at the waist (`top` 11.5). The hem sits at geo 1.5, the same bell as the mist caller's. The legs keep their bones but lose their boxes, so only the boots show.
  - Each panel swings with the leg behind it (the builder's `PANELS` solver, `HEM_Y` 1.5, `HEM_CLEAR` 0.15, `LEG_R` 0.75). The walk and run glide; side-panel roll is about 11 degrees in both.
- **Attack (the staff heal), 36 ticks, release on tick 10 (`impact`), `drawAhead` unchanged:**
  - **Ticks 0-1.5:** the hips load back first (Knight-style weight shift), before the arms move.
  - **Ticks 2.8-4.5:** both hands come onto the staff (the left fist solved onto the shaft below the right) and raise it. The body arches back and the gaze follows the sun up.
  - **Ticks 4.5-8.4:** the light is held with a small tremor and a deepening arch.
  - **Tick 9:** the hips drive forward over the lead foot and the torso follows. The left hand comes off and the staff starts to tip.
  - **Tick 10:** the staff is tipped at the ally and the heal leaves the sun. The left hand is open forward after it, as a blessing.
  - **After:** overshoot at 11.8, a drift at 13.6 (the heal carries on), recover at 16, settled on guard by 28-30.
  - Lead-foot and rear-foot glides under the robe (`attack_cast_legs`). Fist error at most 0.29 px.
  - The head glow is the existing emissive (art), not the clip.
- **Stances:**
  - **Brace (Hold position):** upright, both arms up, the staff crosswise as a guard bar at the brow, the sun out to the left. No crouch. The 8 px arms cannot lift it over the tall cowl, so it sits in front of it.
  - **Wall (Hold the line):** the staff levelled at the foe in both hands like a pike, the sun at chest height, the butt by the right hip, the shoulders turned to bring the left hand forward.
  - **Ward (fall back):** both hands on the staff slanted across the body as a bar, low on the right and the sun high left.
- **Locomotion:** the walk is the seer-style short glide. The run is a 4-tick scurry under the robe (v2 round 1 had spider legs at the longer stride). The staff butt carries 2.6 px (walk) and 7.0 px (run) above the ground.
- **Hits:** the shared graded set (light and medium per bearing, plus block), with the robe panels following the legs.
- **Deaths:** the seer's flat-armed falls except `death_forward` (`DEATH_TWEAK["mage"]`, `_MAGE_FALLS`), with straighter legs in the back, left and right falls so the robe lies flat. The staff is laid flat beside the hand. A dead mage's robe panels fold inward in 2-degree steps then bisect, down to 0.1 px off the leg line for front and back (the legs have no boxes), so the hem does not gape. Rest heights (min-y, px):

  | Fall | Rest heights |
  |---|---|
  | Back | hips 0.79, feet 1.66, torso 2.39, arms 3.07 |
  | Forward | arms 0.0, hips 0.28, feet 2.25, torso 2.39 |
  | Left | arms 0.0, torso 3.62, hips 4.85, feet 6.13 |
  | Right | arms 0.0, torso 3.62, hips 5.04, feet 6.42 |

- **Robe probe** (the seer's poke-through probe): live clips have a worst leg-line margin of 0.75 px, the deepest boot poke is 0.03 px (under 0.25), and the lowest live hem is 0.15 px above the floor; fails: none. The deaths have 0.09-0.10 px leg margins by design and the hems stay above the floor (lowest 0.07 px).
- **Tip origin (cosmetic only, approved):** `data/gravesandglory/cast_tip/mage.json` has offset (-0.077, 2.134, 2.215) blocks for the t2 and t3 staff, and t1 (-0.124, 1.978, 1.949). Damage, healing, ticks and targeting are unchanged.
- **Review (Sonnet high):**
  - **Round 1: FIX NEEDED.** Run spider legs; wall duplicated the guard; the death robe was not flat. Fixed by the 4-tick run lap, the two-handed pike wall, and straighter-legged falls with the inward robe fold.
  - **Round 2: SHIP WITH MINORS except deaths (FIX NEEDED).** Run, wall and cast PASS. Deaths: a dark hollow read at the hem end in the back and left falls.
  - **Fix round 2 (the last allowed, verified by the animator only, not re-reviewed):** the front and back panels of a dead mage now close to 0.1 px off the leg line. The death reel (`mage_v3/reel3d`) shows the panels tight round the boots. The remaining dark squares at the hem end are the two boot soles (4x4, brown rimmed) seen end-on, which is the boot texture, not an opening in the robe.
- **In-game checks (final v2 clips): PASS, 22 of 22, 0 false** (`harness3.log`).
  - The pre-fix-round-2 clips also passed 22 of 22 (`harness1.log`).
  - The first run on the final clips failed one check: march walk side, client speed 0.089-0.222 against 0.175, under "Can't keep up" server lag while other workers' builds ran. That check reads entity motion (`Gait.update`), not the clips. The rerun passed.
  - Cast frames: 35 of 35 in the side and three-quarter views, and 17 of 17 far.
  - 9 of 9 standing and 4 of 4 walking casts matched the server release.
  - The tip log reads `cast tip mage: from (-281.285, 67.134, 7.577) eye (-283.5, 66.74, 7.5) body yaw -90`, so the heal leaves the sun head, about 0.4 blocks above the eyes.

## v1 record (2026-11-06, superseded)

The Sunweaver Mage, the first caster: a sun staff held in the right fist, robes and a cowl. Its clips come from the new caster builder (`art/anim/caster/build_caster_anims.py`). Its rig comes from `art/anim/common/troop_rig.py mage` (staff `build_sun_staff`, held at (-90, 0, 0)).

- **Clips:** guard, idle, walk and run carry, stances, hits and deaths, plus `attack_cast` (36 ticks) and `attack_cast_legs`. The timeline `impact` marker sits on the server's release tick (10).
- **Cast:** gather and raise the staff, a held coil, then a whole-body drive (torso lean and twist of about 26 and -20 degrees) with a hip drop and a lead step. The release is at tick 10, with the staff about 63 degrees from vertical, and an overshoot at 11.8. The root is back on guard by tick 30.
- **Stances:** brace (Hold position) is a deep crouch with the staff planted upright. Wall (Hold the line) is tall, with the staff pushed forward and the off hand raised.
- **Deaths:** hard robe panels, with the robes lifted off the ground over ticks 0-4.
- **Gameplay (Kort approved, 2026-11-06, cosmetic only):** the mage's cast is its heal (`Abilities.mend`), not an enemy attack beat. On the beat before a heal, `mend()` announces the cast 10 ticks early (`Projectiles.drawAhead`), so the wind-up plays before the release. The later `loose` sends the same event, and the client drops the repeat. `Tactics.announcesAhead` now admits a magic caster only when its attack shoots a bolt (`Abilities.shoots`): wm_seer yes, hexer and tc_mist no. Damage, healing and release ticks are unchanged.
- **Checks:** `GeoKnightChecks` has a `CAST` flag (the mage). It runs the ranged duel with frames over the 36-tick cast clip, and releases a 0-heal mend at the foe as a stand-in.
- **Review round 1 (Sonnet high, reel `reel_mage_v1`): FIX NEEDED.** The cast was weak, wall looked like brace, and the deaths had floor offsets.
- **Review round 2 (Sonnet high, reel `reel_mage_v2`): SHIP WITH MINORS.** The cast has a full-body drive, the stances are distinct, and the deaths rest on the floor. Probe: cast fist error at most 0.54 px. Lowest legs in death_forward went from -2.22 to 0.95 px.
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false. 35 of 35 cast frames caught side and three-quarter (17 of 17 far). All 9 standing and 4 walking casts matched the server release. Weight at the release is 1.0, and 0 between casts.
- **Verdict: finished** (superseded by v2). Video: `mage_v2.mp4`.

## Staff grip (2026-11-09)

Kort: the mage held its staff at the bottom like a wand. It now holds it about 40% up the shaft, like a walking staff, and the casts are two-handed.

- **Rig:** `troop_rig.GRIP_SLIDE["mage"]` slides the staff cubes down through the right fist: t1 3.0, t2 4.5, t3 5.5 px. The fist is now 40% up the shaft on every tier. Mage only; the hexer, wm_seer, tc_mist and knight build output is byte-identical.
- **Clips (`build_caster_anims.py` mage):**
  - Guard, idle, walk and run carry the staff upright, with the butt 1.2 to 4.3 px off the floor. The fist is higher (`CARRY["R"]`, `"RF"`).
  - The cast and `mend_sunweaver` raise the staff in both hands, with the left fist 4.5 px above the right (`ls`). The left hand comes off at the drive, as before.
  - Release and impact ticks are unchanged.
- **Downstream:**
  - The rest stance drops the staff arm 6 degrees, not 16. The stun drops it 8 degrees with the head tipped forward (`STUN_ARMS["staff"]`). Without this the longer butt went into the floor.
  - Stances, charge and reactions were rerun, and heldprop was rerun on the deaths.
  - The cast tip moved (offset y 2.13 to 1.99, z 2.22 to 1.98 blocks; cosmetic).
- **Probe (staff axis into the body or robe):** at most 0.55 px on every changed clip, except deaths and aim_up, which are unchanged from before (cowl 0.85 to 1.33 px). The `death_right` butt dips 0.8 px while the staff falls.
- **In game:** 22 of 22 checks, 0 false.
- **Minor:** the grip wrap texture now shows below the fist. It needs a model or texture move (out of animation scope).

## Review

- **v2 verdict: SHIP WITH MINORS, pending one look at the death hem.** Run, wall and cast passed review round 2. The death hem fix (round 2) was checked by the animator on the reel and the probe, not by the reviewer. Video: `mage_v3c.mp4`.

## Open minors (polish, not blocking)

- **Death hem end:** the boot soles are dark and read as a hollow when a fall is seen end-on (back and left falls). A lighter sole texture would fix it (art, out of animation scope).
- **Wall:** in the side and three-quarter views it resembles the cast release. Dropping the front fist 2 px or rolling the staff would separate it.
- **Cast:** the frames around tick 4 (f608 and f610 in the reel) are nearly identical, a short hold.
- **Cast, round 1 minors not reopened:** a small hip dip; the brace bar sits at mouth level rather than above the brow.
- **Hits:** the shaft crosses the face in the medium hits.
- **Side falls:** the hips rest about 5 px up, hidden by the robe.
- The early announce needs a hurt ally in sight on the beat before the heal. The harness checks the announce and release timing, but not `Abilities.mend` itself.

## Caster fix round 1 (2026-11-10, branch anim/fix-caster)

- **Stances:** shield_up has the staff centred and lifted. Hold has the staff tipped at the foe (`HOLD_TROOP` row; it also feeds the rally ending and charge_run_upper).
- **Gait loops:** walk and run are sampled 0.28 of a cycle later (`GAIT_PHASE`), and charge_run_upper LP is moved half a tick earlier (`LP_SHIFT`). Clip lengths are unchanged.
  - Before: run torso 16 vs 10.9, right foot 59.8 vs 41.0.
  - After: worst run_legs left_leg 55.4 vs 55.7.
  - The scorecard has 35 clips, 0 failing.

## Batch review 2026-11-11 (branch anim/review-caster)

Kort's four focus points, checked with `tools/anim/focus_check.py`. Additive clips (hits, knockback, pulled, stun_out) are
also judged as the game plays them, added onto the guard (`work/review/compose.py`; stun_out onto the stun loop); the raw
check plays them on the bind pose.

- **Baseline:** the rebuild at the lane commit matched `out/` clip for clip (CRLF only): no drift.
- **Rows:** lead's first run 15 clips with a FAIL (26 rows). Before, composed: 15 clips (23 rows). After: raw 6 clips
  (10 rows), composed 3 clips (6 rows). Scorecard 35 clips, 0 failing.
- **Changes (builder: section "Batch review 2026-11-11" in `build_caster_anims.py`, plus key edits marked in place):**
  - **Life pass on every attack, ability and hit** (`alive_clips`, `life_pass`): `fullbody.soft_motion` springs the
    shoulder cape and the four robe panels (panels clamped to swing out only, a still piece scaled up to 5 deg), cloth
    eased back where it sank into its parent. Before, the cape and the panels stood still in every hit and stun.
  - **Reaction layers** (`GG_CAST_REVIEW=layers`, run after the reactions merge): the same life pass on stun (the steady
    middle lap of three, the hem flaring on the knee snap), stun_out, knockback and pulled; the floor pass raises the hips
    where a foot went under (stun 1.46 px at tick 0, knockback 1.08 at 5, pulled 0.92 at 8.25). hit_block gets its own
    copy (it was the same object as hit_light_front).
  - **Front blow:** the free arm swings 22, not 32, deg (the sleeve sank 1.9 px into the vestment at 5.5 over the guard).
  - **Left blow (mage only):** the staff arm swings 6 deg less forward; the sun head went 1.6 px into the face at tick 2.
  - **Stances:** brace fist 0.6 px further out and lower (bell in the cowl 2.3); wall fist 0.8 px out (bell in the brooch
    2.0); shield_up staff tipped 12 deg at the foe, fists 5.5 px apart (left bell on the face 2.4).
  - **attack_cast:** the off arm swings out on the drive; the fists 5.5 px apart on the shaft in the raise.
  - **mend_sunweaver:** gather and up fists out of the cowl and the vestment (bell in the vestment 2.1, face in the bell
    1.9); the fists 5.5 px apart.
  - **aim_up (stances.py layer):** the left arm 18 deg lower (`LAYER_NUDGE`): the left bell was 2.8 px in the cowl.
  - **Stun loop (family):** the stun's first tick eases out of its lap's last pose, so the loop has no jump at the wrap
    (tc_mist's kelp hood stood 23 deg apart there: scorecard loop_pose).
- **Remaining rows:**
  - **Bind-pose artefacts (pass composed):** knockback elbow 150.9 (t9.5), pulled elbow 164.6 (t5.5), hit_medium_back
    staff under the ground 2.62 (t2.25) and knee -6.8 (t11.5). Played on the guard none of them shows.
  - **Two fists on one staff in bell sleeves (open, proposed exemption):** attack_cast right forearm in the left bell
    2.1 (t8.5, the last tick both fists hold the staff in the raise), mend_sunweaver 2.07 (t8.75-9, the peak),
    shield_up left bell in the right bell 2.31 (t18.5). The bell is wider than the 5.5 px between the fists the 8 px arms
    reach; frames `work/review/frames/m2_att_8.5`, `m2_sh_18.5`. Wider fists would need a longer arm or a narrower bell.
- **New WARN (composed):** hit_block skirt_left 3.5 deg (WARN, was still), hit_block cape settle 0.7, knockback cowl into
  the swinging cape 1.81, pulled foot 0.73 under after the lift, aim_up right bell in the vestment 1.62 (the aim follows the
  re-keyed cast), left blow staff tip on the sun crown 0.8 (was 1.6 in the face).
- **Looked at:** every tick in three views of attack_cast, mend, brace, wall, shield_up, aim_up, stun, knockback,
  pulled, hit_medium_front and hit_medium_left (composed), contact sheets in `work/review/frames/all_mage_*.jpg`.
- **Tests:** `python -m unittest discover -s tools/anim` OK (152 tests, `work/review/mage.unittest.log`). `python tools/verify.py
  fast`: every step PASS but python-tests, where the one failure is `test_where` timing: `where_extract.py` took 82-107 s
  against a 30 s limit while other sessions loaded the machine (it skips `work/`; no file of this review is in its scope;
  `work/review/mage.verify.log`, `test_where.rerun.log`). In game: geo-knight suite through checkq, root `mage-review1`,
  22 `p1-geo-mage` lines and the suite line all true, 0 `=false`.
