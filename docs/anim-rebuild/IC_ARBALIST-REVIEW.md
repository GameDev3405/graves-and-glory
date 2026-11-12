# ic_arbalist review record (2026-11-06)

The first crossbow unit, the Ironclad arbalist (pillager). Its own builder, `python art/anim/crossbow/build_crossbow_anims.py`, solves the crossbow (`bow_t3_*` bones) to the right fist and the left fist to the fore-stock every tick. Its rig comes from `art/anim/common/troop_rig.py ic_arbalist`. The attack, `crossbow_loose` (50 ticks, impact 4), aims, looses, kicks, lowers, spans the string with the foot, hauls, reaches to the hip quiver, places the bolt and settles back to ready.

- **Weapon bones.** The crossbow bones are named `bow_t3_*`, not `weapon*`, so the death floor-fit and the flat-hand pass treated them as body. `is_weapon(bone)` now covers both, which keeps deaths on the ground.
- **Quiver reach.** It blends by joint angle, not by fist position, so the arm cannot fold through the torso on the way to the hip.
- **Butt-anchored keys.** The kick, the lower and the quiver pose pivot about the stock's butt (`about_butt`), so the crossbow tips instead of sliding through the hands.
- **Round 2 changes.** The prod sits lower and further forward through the span and haul (`SPAN`, `HAUL_A`, `HAUL_B`). The quiver pose tips 40° instead of 50°, and the recoil kick pitches 9° instead of 12°.

## Gameplay change (cosmetic, logged as an assumption under WORKFLOW.md)

A crossbow shot's attack_v1 event was only sent at the loose. Its clip therefore arrived late, faded in (weight about 0.08 at impact), and skipped the raise-to-aim.

- **The change.** `Tactics.announcesAhead` now covers non-magic bow and crossbow troops, so a crossbow shot is announced on the beat before it, as a bow's is (R110 Plan B). For a crossbow, `Projectiles.drawAhead` starts the clip at `beat − impactTicks`, the tick `loose` stamps, so the client keeps the first copy and drops loose's duplicate.
- **What does not change.** `drawsAhead`, `looseTicks` and `hold` stay bow-only, so bolt timing, damage and cooldown are unchanged. If the target dies or the troop is stunned in that beat, the aim plays with no bolt, as a bow's draw does.
- **Ballista Crew.** `ic_ballista` (also the crossbow family) gets the same early announcement on its existing renderer. It has not been tested.
- **The harness.** `GeoKnightChecks` duels crossbow troops at range. Its shots are spaced 60 ticks apart (the clip is 50), its frame checks cover the first 30 clip ticks (shot, kick and span), and its gap window sits at 55.

## Review

- **Round 1 (Sonnet high, reel `ic_arbalist_v1`): SHIP WITH MINORS.**
  - After the loose, the crossbow rose upright beside the helm (t306–313).
  - It snapped down to the feet for the quiver reach (t316–325).
- **In-game checks run 1: FAIL, 11 of 51 checks.** The harness duelled the crossbow as melee, and in game the clip faded in late. Both are fixed above.
- **Round 2 (Sonnet high, reel `ic_arbalist_v2`): SHIP WITH MINORS.**
  - The post-loose pose is improved: the weapon is lower and diagonal, and its tip stays below head height.
  - The drop to the span reads as spanning with the foot but is still quick.
  - No new issues, and no regressions on the hit sheet.
  - That was the second fix round, so the remaining items are recorded below.
- **In-game checks run 2: FAIL, 2 of 51.** The walking sync duel saw too few swings. Fix: `GeoKnightChecks.nudgeR` spaces walking crossbow shots 40 ticks apart (60 standing).
- **In-game checks run 3: FAIL, 2 of 51.** Sync passed (stand 9/9, walk 4/4). Only `march-walk-side` failed (median 0.173 vs 0.151), plus the suite total. Heavy rig builds were running alongside and the log shows server hitches; runs 1 and 2 measured 0.151 exactly on the same code.
- **In-game checks run 4: PASS, 22 of 22 checks, 0 false**, with no other heavy work running.
- **Verdict: finished.** Video: `ic_arbalist_v2.mp4`.

## Open minors (polish, not blocking)

- **Crossbow near the face.** In front and three-quarter view at t313, the crossbow passes close to the chin. It does not clip.
- **Drop to the span** (t316.9 → t319.8) is fast, with no gradual lower between.
- **Left forearm** comes near the head during the span (t319.8–322.8).
- **Heavy hit** reads extreme.
- **Death ground contact** is hard to judge at reel size.

## v2 feel (2026-11-07, branch anim/v2-ranged)

The Knight v2 feel (see `ARCHER-REVIEW.md`, "v2 feel"), built by `art/anim/crossbow/build_crossbow_anims.py` with the crossbow's own values. The shot (`attack_loose`, 2.5 s, impact tick 4) and the four deaths are byte-identical to the previous export, so the server sync is unchanged.

Changed clips:
- **Idle:** a contrapposto weight cycle (hips slide over one foot and back, the free hip drops, the chest counter-tilts, the arms trail a beat late). One breath and one full shift per 3 s loop.
- **Brace:** a deeper, wider stance (hips down, feet out, yaw set apart from the ready footing) with the crossbow held out front.
- **Hits:** graded light / medium / heavy. Light is a small flinch at the knees; medium and heavy are pelvis-driven staggers; the crossbow arm opens outward in the front hits.

- **Probe.** Shaft or fist in the body: 0 samples in every clip. Grip error in the shot: max 3.7 px (unchanged). Deaths and shot byte-identical to HEAD.
- **Review.** Sonnet high, 2 rounds.
  - Round 1: FIX on the idle (weak weight shift); stances, hits and reload passed.
  - Round 2: idle amplitude raised and the chest counter-tilt relaxed; still judged FIX for the idle (low-resolution sheets), no new clipping or pops. Two fix rounds spent, so the rest is recorded as a minor.
- **Open minors.**
  - Plain idle weight shift is still subtle from the front on the contact sheets (hip roll about 5 degrees, 2 px slide); check it in-engine.
  - Walk and run show little pelvis sway on the sheets.
  - Light hit is close to idle at t=1-6.
  - Medium hit from behind (t=12) drops the crossbow low; heavy front (t=6) pitches the head close to the chest plate; medium hit from the right splays the arms.
  - Brace and hold-the-line are close from the front.
- **Harness.** geo-knight suite with `GRAVES_GEO_CHECK_TROOP=ic_arbalist`: **22 `[p1] p1-` lines, 0 `=false`** (a first launch hit a transient classpath failure before any check ran; the rerun passed, with two "Can't keep up" log lines and no lag failure).
- Video: `group_d2_v2` (ic_arbalist, then tc_harpooner).

## Batch review 2026-11-11 (branch anim/review-arbalist, from anim/fix-singles)

Fixes made in `art/anim/crossbow/build_crossbow_anims.py` only, each with a comment at the change. The builder now also merges
the shared stance, reaction and charge clips itself (stances, then reactions, then charge) and finishes them for the arbalist,
so one build writes all 55 clips. **Running `stances.py`, `reactions.py` or `charge.py` on the arbalist afterwards would put back
their unfinished versions.**

- **Changed clips (29).** walk_upper, run_legs/upper, attack_loose(+legs), stance_ready/wall/shield_up upper, stance_wall_legs,
  stance_rally_upper, stance_fallback_legs, charge_run_upper, stun_upper/legs, stun_out_upper, hit_knockback(+legs),
  hit_pulled(+legs), the four medium hits and hit_block (+legs). Lengths, loop flags, impact tick 4 and the 50-tick shot unchanged;
  idle, the other stances, light hits and the four deaths are byte-identical.
- **What changed.** The prod and the left forearm clear the helm in the haul (t21) and the bolt reach (t25); the shot and haul
  move the hips 2 px over a stepped-out lead foot; soft motion on the mantle, quiver, skirts and shoulder plates in the shot,
  hits, stun and shake-off (the mantle damped harder than a plume: zeta 0.6, gain 1.0, max 8); leg skirts and the fauld follow the
  run's thighs; carries a little wider in walk/run; wall and shield_up re-keyed so the bevor/helm stay off the bracers and the
  knee off the ground; rally grip carried 2 px out; hips raised where a boot or the crossbow went under the ground (fallback,
  stun, medium hits, knockback, pulled); the charge's support advance uses the arbalist's own hold.
- **focus_check.** 18 clips / 43 FAIL rows before; 6 clips / 9 FAIL rows after (raw). Composed over the stance they are added to
  (`work/review/tools/compose.py`), 5 clips / 6 rows. Remaining:
  - attack_loose limb_in_body / armour_clip 2.36 px at t31.5: the left bracer passes the helm as the bolt comes down from the
    quiver to the rail (joint-angle blend between the two contacts). Head, torso, timing and arm-arc variants did not clear it
    (2.33-2.8). Open; needs a re-pathed bolt carry.
  - stance_wall_upper armour_clip 1.96 at t30 (fauld into left_skirt), stun_upper armour_clip 1.99 at t6 (right forearm into
    tabard, unchanged from before), hit_medium_front composed armour_clip 1.91 (quiver into tabard_skirt): marginal, open.
  - Raw-only, additive clips read over the bind pose (clean when composed): hit_knockback weapon in right_leg 1.88, left forearm
    in fauld 2.04, elbow 148; hit_pulled elbow 162.8; hit_medium_front knee -6.81.
  - Composed-only: stun_out_legs secondary_life (the legs clip carries no soft pieces; the upper clip does).
- **New WARN rows.** attack_loose floor 0.64 (right greave at t21, the haul's deeper sink); hit_medium_back weapon in fauld 0.83;
  hit_pulled right_pauldron pop 9.72 (the plate trailing the pull).
- **Scorecard** exit 0 (0 fail). **Unit tests** (`tools/anim`) 152 OK. **verify fast**: every check passes except python-tests,
  which hit verify's 600 s cap with no failing test on a loaded machine; see below for the untimed run.
- **Rule 6.** shield_up holds the crossbow upright before the face as cover; it reads oddly for a crossbowman. Reported, not
  re-designed.
- **verify fast, untimed python tests.** 727 tests, 726 pass; the one failure is `test_where`'s speed limit (extract 117 s, limit
  30 s) on a machine running many parallel builds. No test touches the arbalist files.
- **In-game reel** (checkq, geo-knight suite, `GRAVES_GEO_CHECK_TROOP=ic_arbalist`). Root `ic_arbalist-review1`: 22 `[p1] p1-`
  check lines, 2 false (`attack-walk`: own unit in IDLE gait during the swing; `hit`: heavy-front far view had 0/4 frames).
  Rerun `ic_arbalist-review2`: `hit` passed; `attack-walk` false again (own and foe IDLE at samples 83-84, foe backing speed
  min 0). The swing's length and impact tick are unchanged, and the same check failed intermittently for archer, knight and
  ic_hearthguard in other lanes' reels today, so it is likely not from these clips. Not proven: no reel was run at HEAD.
- **Score** 7/10. Weakest clip: attack_loose (the left forearm across the helm at t31-32 as the bolt comes to the rail).

## Fix round 1 (2026-11-12, from the Sonnet review FIX 6/10)

- **Changed clips (11, against 28c0f6e0).** attack_loose(+legs), stance_brace_upper, stance_shield_up_upper, stance_wall_legs,
  stun_upper, hit_pulled, hit_medium_front, hit_medium_back, hit_block(+legs). The other 44 clips, the deaths among them, are
  byte-identical; lengths, loops, the 50-tick shot and impact tick 4 kept.
- **attack_loose.** The left arm turns 30 degrees out (+z) over the bolt carry, eased in over t22.5-24 and out over t31.5-33: the
  bracer no longer crosses the helm (worst now 1.72 px at t21, WARN). The kick is stronger: the chest rocks 11 degrees (was 9),
  the stock comes 3.7 px back, the rock carries into the second key, and the hips take more of it. 14 degrees put the left
  forearm 1.47 px into the chest, over the scorecard's 1.39.
- **shield_up (rule 6).** A crossbowman's own cover: side-on and ducked, the left bracer raised before the visor, the loaded
  crossbow lowered to the right hip. It reads apart from ward (crossbow across the chest) in all three views.
- **brace** pitched 16 degrees down (was 6), apart from ready and aim. Ready and aim kept: ready is the shot's first key and the
  guard_up check holds it, and aim keeps its line of fire.
- **wall** left skirt -12 degrees; **stun** crossbow arm 10 degrees forward; **hit_medium_front** quiver swing x0.75.
- **Hanging pieces.** A damped swing (8 degrees, 4 ticks, dying over 2) on the quiver in hit_pulled and hit_medium_back, and on the
  quiver (12) and the skirts in hit_block. Not in stun_out_upper: it plays over stun_legs, which owns the skirts (the scorecard
  needs the two disjoint), so its "still skirts" WARN stays until focus_check holds a pose base.
- **Rig.** Ported af51daad's weapon cube turn `[0, 0, -sc.turn]` in `common/troop_rig.py`; the ic_arbalist geo is byte-identical
  to the bow lane's and the prod limbs render straight.
- **Shared modules.** stances.py, reactions.py and charge.py leave ic_arbalist out of `all` (one comment line each); a warning in
  the crossbow builder's docstring.
- **focus_check** (hits over stance_ready): 4 clips / 5 FAIL rows -> 0 / 0. **Scorecard** 0 fail. **Unit tests** after the last
  builder change: test_scorecard 22, test_focus_check 22, test_render_geo 20 OK, plus the booth tests.
- **In-game reel** `ic_arbalist-review3` at ec7bf989 (checkq exit 0): 23 `[p1]` lines, attack-walk, hit and hit-pulled now true;
  one false, `march-run-side` (units still in the WALK clip at 0.12-0.18 speed; server "Can't keep up" up to 9.9 s). Walk and
  run clips are byte-identical this round, so lag; not rerun. Booth batch `work/booth/ic_arbalist-20261112-ec7bf98` (not
  uploaded); video `status/booth-videos/ic_arbalist_review.mp4`.
- **Open.** Ready and aim are not pulled further apart (above). attack_loose has no aim hold before the release (impact tick 4 is
  the design's; Kort's call). WARN rows: 88 (89 before), worst armour 2.01 on stun_out_upper (inherited from the stance; the
  tool's pose-base change is pending) and its "still skirts", attack floor 0.64 at t21.
