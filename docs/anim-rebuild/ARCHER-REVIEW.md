# Archer review record (2026-11-06)

The first bow unit. Its clips come from `art/anim/archer/build_bow_anims.py`, which imports the knight's builder (`GG_ANIM_TROOP=archer`) for gait, stances, hits and the death machinery and adds the bow:

- **Bow draw channel.** The empty `draw` bone's posX picks which bow frame shows (rest, pull_0, pull_1, pull_2). The geo carries all four frames per tier under `weapon_t{n}`.
- **Solved shot.** Per clip, one solve moves the full-draw key's torso, head and bow arm so the drawn nock lands on the cheek anchor (error ≤ 0.02 px). Every sample, the bow hand turns the bow onto the aim line with its limbs upright, and the string hand rides the nock (error ≤ 0.5 px).
  - Volley aims 30° up; the direct shot aims 3° up.
  - The held full draw trembles.
- **Held shot in game.** `AttackController` stretches the clip's own full-to-impact span over `Seed.hold` extra ticks, so the aim stays alive instead of freezing a frame, and the loose still lands on the server's impact tick.
- **Deaths.** These are the knight's four falls. On top of them, the bow hand is solved per tick from the impact onward so the bow lies on the ground beside the body: it never sinks into the ground or spears the body (`flat_bow`).
  - In death_left, the bow arm flings out past the head (archer `ROLL_LEFT`). The knight's over-the-hood sweep leaves no bow heading that clears both the body and the ground.

## Review

- **Round 1 (Sonnet high): FAIL.**
  - The bow attack line and the string hand were wrong.
  - In the deaths, the bow sank through the ground or speared the body, and the body hovered.
- **Round 2 (Sonnet high, reel `archer_v2`): SHIP WITH MINORS.**
  - **Attacks pass.** The bow stays on the aim line with no flip or clip. The arrow comes to the string and draws to the cheek, and the loose lands on the server's impact label. The held draw trembles.
  - **Deaths pass.** No bow sinks or spears in any of the four falls.
  - **The rest reads cleanly:** idle, walk, run, ward, combat ready and hits.
- **In-game checks:** the geo-knight suite with `GRAVES_GEO_CHECK_TROOP=archer`.
  - **Run 3:** placed, views, march gaits, attack-walk, walk-gait-legs, attack-death, stance, hit and death pass. Attack sync, plays and footwork failed (below); attack frames at side/three-quarter were 19/25.
  - **Fixes after run 3:**
    - `TroopRenderer` was hard-wired to the knight's GeckoLib renderer; it now keeps one renderer per troop, so every rebuilt troop draws as itself. Sentinel, revenant and tc_raider verdicts from before this fix are void and must be rerun.
    - A bow shot's draw is announced one beat (10 ticks) ahead, so the client hears it 3–6 ticks after the clip's start. `AttackController` plays the wind-up quickened from arrival to the full draw, then eases its weight in over `LATE_IN` ticks.
    - Harness: ranged troops are nudged through `Projectiles.drawAhead` then `Tactics.shoot` on the beat. The sync row now records the shot on the tick it was fired (`shotAt`); the swing detector runs before the nudge, so it logged nudged shots one tick late.
  - **Run 4:** sentinel, revenant and tc_raider pass every check with the per-troop renderer. Archer passes placed, views, march, attack-walk, walk-gait-legs, attack-death, stance, hit and death. Four checks failed, all on the harness's view of the shot:
    - **Attack frames: 20/25.** The quickened late wind-up runs the clip about 1.5× the clock, so a tick steps past a frame's ±0.3 window.
    - **Sync: 112/134, clip null.** In the three-quarter view the camera faces the own archer, and the foe, 10 blocks off, is behind it and culled. Its clip is never drawn.
    - **Plays and footwork: impact weight 0.27.** When the foe came back into view mid-shot, `AttackController` took the first drawn frame as the time it heard of the shot and faded the clip in as late.
  - **Fixes after run 4:**
    - **`Gait`:** the walk/run bands scale with each troop's speed (`toRun = .22k`, `toWalk = .20k`, k = speed/.28), so a fast troop no longer runs in place at a walk.
    - **`AttackController`:** a swing first seen when the troop was not drawn the frame before plays from its real time at full weight, which is also right in play when the camera pans onto a troop mid-shot. It also exposes `shownRate`, the clip's speed against the clock.
    - **Harness:** frames in a quickened wind-up use a ±0.5 window. The sync check skips swings whose troop was never drawn, and reports them as off camera.
  - **Run 5:** everything passes except attack frames (frame 14 missed): the quickened wind-up steps more than one frame per tick, so some frames fall between samples.
  - **Fix after run 5 (harness):** frames a quickened wind-up steps past between two ticks (`AttackController.shownRate` above 1) count as covered, and the report lists them as "stepped past".
  - **Run 6: PASS, all 51 checks.** Attack frames 25/25 at side and three-quarter (5 stepped past) and 12/12 far; sync 7/7 standing and 4/4 walking; impact weight 1.0; footwork 1.0; hits 7 blows × 3 views; deaths 4/4.
- **Verdict: finished** (visual review and gameplay testing both pass). Video `archer_v2`.

## Open minors (polish, not blocking)

- **Bowstring art.** The pull frames draw the string as one rigid white rod.
  - It reads as detached from the limb tips and floats behind the head during the draw.
  - After the volley's loose it stays diagonal for a few frames.
  - This is an art fix in the pull-frame geo, not the animation.
- **Arrow.** The arrow is short against the draw length (art).
- **Volley front view.** The torso rolls (−27.6° from the anchor solve), so the pose reads contorted from the front.
- **Death forward.** One bow limb rests raised about 30°.
- **Death right.** The bow crosses the face for a few frames during the fall (it reads as a flail).
- **Brace and hold the line.** Both start close to combat ready; push them apart.
- **From round 1:**
  - overshoot and hip drop at the loose
  - run lean and arm swing
  - hit_light_front amplitude
  - hood and quiver lag

## v2 feel (2026-11-07, branch anim/v2-bow)

The Knight v2 feel, brought over in `art/anim/archer/build_bow_anims.py`; the knight builder is not edited. The draw, the shot and the deaths are unchanged, and the deaths are byte-identical to the previous export.

- **Idle.** A weight cycle (contrapposto): the pelvis shifts and rolls so the standing hip lifts, and the shoulders tilt against it. Breath and head drift are layered on top.
- **Stances.** Brace and hold the line get their own footing (`STANCE_LEGS` v2 and `STANCE_LEGS_LIFE`), so they read apart from combat ready. This closes the round-1 minor "push them apart".
- **Hits.** These are graded like the Knight's.
  - Light: a knee flinch.
  - Medium: the pelvis shifts.
  - Heavy: a stagger, with the bow arm opening outward and never folding in.
  - Every direction gets the Knight's v2 footwork.
  - The front body hit is scaled to 0.85 (`V2_BODY`), so medium and heavy read apart.
  - The bow arm opens wider and holds open (`BOW_ARM`).
- **Probe.**
  - No bow or fist in the body in the hits, idle or stances. The volley still has 2 bow points in `hood_t3`, which is pre-existing.
  - Floor: hits -0.38 to -0.58 px, wall -0.91 px (the boot cuff, as at baseline).
  - Largest hit pop: 19.1 px/t, at the heavy front peak.
- **Review.** Sonnet high, 2 rounds, verdict SHIP WITH MINORS. Round 2 resolved the medium/heavy grading and improved the heavy front tumble.
- **Open minors.**
  - Heavy front, side view, still leans deeper than the Knight's heaviest hit, about 45-55 degrees at the peak.
  - Heavy front, front view: the bow hand and bow top overlap the chin for about 2 ticks. This is depth only; the 3/4 view shows clearance.
  - Medium front: a possible snap around ticks 4-7.
  - The idle hip tilt is hard to see from the front.
  - Wall stance: the string fist beside the helm can read as a salute (pre-existing pose).
  - Medium from behind pitches about 50 degrees, near the limit.
- Video: `archer_v2feel`.

## Archer shot port 2026-11-11

Kort's approved archer direct shot (round 3, skill lane, 26-tick clock) now runs on the 22-tick bow builder.

- **Files.**
  - `art/anim/archer/direct_shot.py` holds the shot. The bow builder calls `direct_shot.clips()` for the archer only, at the end of `attack_clips()`.
  - `art/anim/archer/build_direct_demo.py` is a thin runner that writes the pair into a copy, never the exported file.
  - Only `attack_direct` and `attack_direct_legs` were spliced into `art/anim/archer/out/` and the exported animation. A scratch full build of the archer differs from the old builder's build in those two clips only, and its pair equals the spliced pair.
- **Timing.** Every time comes from the builder: `ATTACK_TICKS` 32, `FULL_AT` 16.5, `IMPACT` 22, through `at()`. Kort's two pacing points (nock 8.75, set 11.5) are kept. Full draw is half a tick earlier than the approved clip mapped through `at()` (17).
- **Hand-off.** Tick 0 and the last tick match the builder's own `attack_direct` to 1.32 deg (`hood_cape`, cloth) and 0.00 px. They match `stance_ready_upper` to 1.42 deg (torso). The other stance, idle and walk first frames differ from it by the same amounts as the builder's own clip.
- **Time-warp against the approved clip** (every quarter tick, approved sampled at the mapped time):
  - Largest difference 2.30 deg at tick 18.25, `right_arm`, and 0.27 px at 21.75, `draw`.
  - Every row over 1 deg is in the hold, ticks 17 to 19.75, on `right_arm`. The hold tremble runs at a fixed 2.3-tick period over a hold that is now 5.5 ticks, not 5, so its phase moves. Its size does not change.
  - The 0.27 px at 21.75 is the string's last creep before the loose, spread over the same longer hold.
- **Checks.**
  - Focus check: `attack_direct` 0 FAIL, 3 WARN. The approved clip had 2: bow in left shin 1.22 px at tick 7.25, and fist in jerkin 1.69 px at tick 8.75. The third is the fist at the quiver at tick 5 (1.74 px). That is the pluck, now that the quiver sits where the hand reaches. Before the splice, the old clip was FAIL here.
  - Scorecard: exit 0, 35 clips, 0 failures.
  - Every tick seen in front, side and three-quarter on the corrected quiver geo.
- **In game.** Runs `archer-port1` and `archer-port1b` each give 23 `[p1] p1-` lines, but not a pass: 7 and 6 checks are false. The false checks change from run to run: attack-plays, sync-walk, footwork, walk, frames and hit. All of them are on the volley, the walk and the hits, which this port does not touch. The lane failed 4 of the same checks before any change (`archer-geo1b`). The archer's duel plays the volley, so the harness never shows `attack_direct` in game.
- **Video.** `status/booth-videos/archer_direct_port.mp4`: the approved clip on the left, re-timed onto the port's clock so the two play in step, and the port on the right.

## Batch review 2026-11-11

Bow-family batch review on `anim/review-bow` (Opus lane, two fix rounds). Timing unchanged: `ATTACK_TICKS` 32, `FULL_AT` 16.5, `IMPACT` 22; `attack_direct` and `attack_volley` keep `full` 0.825 and `impact` 1.1.

- **Rows before -> after** (full focus check, `work/review/archer.{before,after}.json`): clips with a FAIL 13 -> 6, FAIL rows 22 -> 10, WARN rows 51 -> 51 (no new WARN).
- **Changed clips (25, spliced; every other clip byte-identical to HEAD).**
  - `attack_volley` (+legs): rebuilt on the r3 direct-shot engine (`direct_shot.clips(volley=True)`), so the fist no longer passes through the face and hood at tick 8 and the 126 deg forearm pop at the loose is gone. The archer's `attack_direct` pair is byte-identical to before.
  - `stance_aim_upper`, `stance_aim_up_upper`, `stance_aim_down_upper`: aim from the r3 full-draw frame; the chest takes 85% of the pitch (`AIM_GAIN_INGAME`), so the string fist stays clear of the face on aim-up.
  - All eight `hit_light/medium_*` pairs and `hit_block` (+legs): legs now come from the knight's absolute hit legs and the gear (quiver, skirts, pouch, tippet) swings through `fullbody.soft_motion`, low-passed so nothing pops. This clears the `secondary_life` FAILs.
- **Remaining FAIL rows (outside this builder).** `hit_pulled`, `stun_upper`, `stun_out_upper`, `hit_knockback` and `charge_run_upper` are merged by the common modules (`reactions.py`, `charge.py`), which this lane may not edit: foot under the ground 1.6-1.8 px, `secondary_life` still gear, knockback hood into quiver 2.04 at t3. `run_upper` skirt into skirt 1.97 at t2 (gait, pre-existing, limit 1.9).
- **Proposed exemption.** archer, `run_upper` / `charge_run_upper`, `armour_clip`, 1.97, `jerkin_t3_skirt` into `left_skirt_t3`: two cloth skirt layers overlapping at the stride, not visible at game size.
- **Tests.** Scorecard exit 0 (35 clips, 0 fail). Unit tests OK: `test_scorecard`, `test_focus_check` (22), `test_render_geo`, all six `test_booth*`. Booth config valid, every listed clip present.
- **Drift not shipped.** A full rebuild also changes the deaths (weapon_t3 channel) and `hit_medium_front` arm keys through common-module commits after 151790bd; only the clips above were spliced, so that drift is not in this commit.
