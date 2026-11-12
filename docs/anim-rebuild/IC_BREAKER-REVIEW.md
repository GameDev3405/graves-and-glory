# ic_breaker review record (2026-11-06)

The Iron Clans Shieldbreaker, the first greataxe unit: a long two-handed axe (`greataxe`, sunder). Its builder reuses the spear builder's two-handed solve, footing, carry, hits and falls, and adds the greataxe's own guard, stances and swings: `python art/anim/greataxe/build_greataxe_anims.py` (`GG_GREATAXE_TROOP` picks the troop; the default is ic_breaker). Its rig comes from `art/anim/common/troop_rig.py ic_breaker`.

- **Guard:** the axe is held across the body at the high port. The right fist is low before the belly, the head is up by the right shoulder with the bit toward the foe, and the left fist is up the haft. From here the chop needs only a heave and the hew a coil.
- **Attacks:** inside a 28-tick clip under the 32-tick cooldown, each with an `_legs` clip:
  - overhead_chop (impact 12)
  - side_hew (impact 12)
  - haft_jab (impact 7)

  The timeline `impact` marker sits on the server's impact tick.
- **haft_jab is a cross-check:** the haft is driven level across the chest into the foe. It reads in the front and three-quarter views. In the side view it is seen end-on.
- **Bit:** every key names the bit direction, so the bit leads each swing. The chop swings in the sagittal plane and the hew level.
- **Grip:**
  - `GREATAXE_PROBE=1` prints each swing's grip error, axe lowest point and shaft-in-body samples.
  - Results: chop 1.95 px (none in body), hew 3.17 px (one sample), jab 1.44 px (none). The axe stays at least 2 px above the soles.
- **Round 1 fix: haft_jab recovery.** After the check the haft swung back out forward, which read as a second, spear-like thrust. The recovery now pulls in and lifts back toward the carry.
- **Round 1 fix: overhead_chop.** A mid-arc key at atk 11.5 splits the drop across the impact tick.

## Review

- **Round 1 (Sonnet high, reel `reel_brk_v1`): FIX NEEDED.** The haft_jab recovery read as a second thrust. The fixes above.
- **Round 2 (Sonnet high, reel `reel_brk_v2`): SHIP WITH MINORS.** The cross-check reads as one strike; the body lunges and drops on impact, then the axe returns to the carry with no second thrust. No new clipping.
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false. All 22 standing swings and 4 walking swings matched the server impact.
- **Verdict: finished.** Video: `ic_breaker_v2.mp4`.

## Open minors (polish, not blocking)

- **overhead_chop** (t305→t306): the drop still reads as a one-frame snap at 20 Hz sampling. The atk 11.5 key falls between sampled ticks; in game, the renderer's partial-tick interpolation shows it.
- **overhead_chop** follow-through (atk 20): a slight lift before the settle.
- **side_hew:**
  - The overshoot is hidden in the side view.
  - The butt brushes the right flank at t11.25.
  - The grip error is 3.2 px at t11.
  - The gather turns the bit between t18 and t20.
- Walk and stances read weakly at reel scale.
- Hit folds are deep.
- Death floor contact was judged at low confidence.
- The reel crops the head at the top of frame during the chop wind-up.

## v2 feel (2026-11-07)

The knight's approved v2 style is ported to the greataxe builder (`art/anim/greataxe/build_greataxe_anims.py`, which reuses the spear builder's v2 tables). The breaker keeps its heavy two-handed axe and slow, committed swings. `GG_V2=0` builds the old style. Deaths are unchanged.

- **Pelvis-driven blows:** the overhead chop, side hew and haft jab each load into the back hip. The pelvis then turns and drops into the blow, and the axe follows the hips. Both fists stay on the haft.
- **Side hew recovery fixed:** the right hand's rotation channel ended more than 180 deg from its start. HEAD had this too: the end key was at z -356.9. During the in-game fade back to the stance, the axe spun a full turn. Now the recovery lifts the axe up in front on the left (t21). A geodesic key at t19 rolls it evenly back to the guard, at no more than about 13 deg per quarter tick. Any attack rotation channel whose end is more than 180 deg from its start is re-branched to an equivalent euler that is visually continuous. The swing's fastest step fell from 377.7 deg/tick (HEAD, in the recovery) to 252.9 (t11.5, the strike itself).
- **Graded hits:** a light blow is a knee flinch at 0.4 of the peak. A medium blow throws the upper body further, holds it a beat, shifts the pelvis and catches it with a recovery step. A heavy blow plays the medium clip at 1.55 times its size, so it reads as a stagger.
- **Idle:** the knight's weight cycle: breath, then a slow shift from foot to foot.
- **Stances:** ready, brace, hold the line and ward follow the spear family's v2 poses, carried by the axe.
- **Probe:** no new shaft or fist in the body, and no new pops. The counts below were already present at HEAD and the review judged them as the haft resting against the torso and arm:
  - stance_wall: 81 shaft-in-body samples (HEAD 81);
  - hit_medium_front: 3 (HEAD 5);
  - hit_medium_right: 10 (HEAD 9).
- **Byte identity:** knight, archer and gl_marshal build byte-identical to HEAD. The greataxe builder feeds only ic_breaker.
- **Review (Sonnet high, sheets, compared with knight v2): PASS WITH MINORS.** One fix round re-reviewed the side hew recovery: PASS WITH MINORS, no new blockers.
- **Harness:** `geo-knight` with ic_breaker, run twice: 22 `[p1]` checks each run. Each run failed only attack-sync-walk and the suite roll-up. Every sampled swing matched the server: walk 3/3 in both runs, stand 27/27 and 24/24. Only 3 walking swings landed in the window, one below the check's floor of 4. Run 1 logged 3 "Can't keep up" warnings, run 2 none. The original breaker rollout sampled 4. Clip lengths and impact markers are unchanged from HEAD, and the server's swing cadence does not read clip data, so the shortfall is in the duel's sample size, not the animation.
- **Minors:**
  - The axe head comes close to the helmet in the chop (t13/t15), the side hew wind-up (t13/t15), the jab (t18) and the medium right hit (t6). It does not clip.
  - The light front hit is a very small flinch.
  - Ready and brace read close. The walk is near-identical in side view. The idle weight shift is subtle next to the knight's.
  - The side hew's grip error peaks at 3.2 px.

## Batch review 2026-11-11

Branch `anim/review-breaker` (from `anim/fix-spear`, base a6f9c90f). Builder `art/anim/greataxe/build_greataxe_anims.py`.
Recipe (one command; it rebuilds `out/` from the builders each time, and `out/` equals the exported asset byte for byte):
`python art/anim/greataxe/build_greataxe_anims.py && python art/anim/common/export.py ic_breaker`.
The builder's `write_layers` step (layered clips, hang_swing keying, and keeping the shipped death clips) refuses to run on
an `out/` that already holds layered clips, so it cannot be applied twice.

### Drift (baseline rebuild, no edit)

The lane's builder at this commit rebuilds 16 clips that differ from the committed `out/`: idle_upper, the three attacks, the five
stance uppers, hit_light_front, hit_medium_front, hit_block and the four deaths. The three hits differ by under 1 deg.
The focus check on the drift build gives the same rows as on the committed asset. Two causes:
- the committed `out/` was never re-keyed by hang_swing, which the asset was: the new `write_layers` step now does both, so
  idle, ready, ward, shield_up, fallback and death_back come out byte-identical to the committed asset;
- death_forward/left/right in the asset are heldprop.py's data-level polish of 2026-11-07 (POLISH-DEATHS.md: non-D3 troops
  were not rebuilt), which no builder reproduces. A rebuild gives deaths that fail the scorecard (ang_speed_weapon 87-100
  against 83.2). The build now keeps the shipped death clips, so all four deaths are byte-identical to the lane.

### Rows before -> after (focus check, FAIL rows per clip)

32 clips: 18 with a FAIL, 53 FAIL rows -> 14 with a FAIL, 28 FAIL rows.

| clip | before | after |
|---|---|---|
| run_upper | 1 | 1 |
| attack_overhead_chop | 7 | 5 |
| attack_side_hew | 5 | 2 |
| attack_haft_jab | 7 | 3 |
| stance_brace_upper | 1 | 0 |
| stance_wall_upper | 2 | 0 |
| stance_rally_upper | 3 | 0 |
| stance_hold_upper | 3 | 0 |
| hit_pulled | 3 | 2 |
| charge_run_upper | 4 | 2 |
| stun_upper | 5 | 5 |
| stun_out_upper | 2 | 2 |
| hit_knockback | 2 | 1 |
| hit_medium_front / back / right / left | 1 / 3 / 2 / 1 | 1 / 1 / 1 / 1 |
| hit_block | 1 | 1 |

Ten of the 28 rows are `secondary_life: helm_plume` (the crest, which must not swing).

### What changed and why

- **All attacks, hits, stun, knockback, pulled:** `fullbody.soft_motion` on the pieces (cape, cape_cloth, fur_collar, skirts,
  tassets, pauldrons); pauldrons may trail up to 10 deg to keep their spikes off the helm (`plate_clear`); the cape swing is
  scaled (attacks 0.6, hits 0.45) so the cloth stays out of the tabard. Hits whose plates barely move get a small decaying jolt.
- **Attacks, helm clearance (`clear_helm`):** where a left cuff/gauntlet enters the helm or the left elbow passes 125 deg, the
  left arm is re-solved with the fist kept on the haft, eased in and out over a tick, first and last tick untouched; the head
  turns a few degrees away from plates (smoothed). The jab's re-solve is windowed to t10+ (earlier it popped the wind-up).
- **overhead_chop:** the roll of the bit through t12.5-13.5 is even (roll_spin 170 and a right-hand pop); the follow-through
  is levelled up to 9 deg (axe into the ground 2.7 px at t14); the heave is held a hand in front of the brow (O z +1.2..3.4)
  so both forearms clear the helm.
- **haft_jab:** wind-up keys keep the guard's roll (grip_roll 83 at t1.5); the shove drives on past the impact (phases);
  hands a little closer on the haft through the recovery (left spikes into the helm).
- **side_hew, haft_jab, overhead_chop:** the pelvis sinks less at the deepest drive (right_knee_wing under the ground) and the
  haft rises with it, so the arms keep their pose.
- **stance_brace / stance_wall:** pelvis 0.9 / 2.2 px higher and the rear foot 0.7 / 1.5 px further back (knee plate under
  the ground).
- **stance_rally / stance_hold / charge_run:** helm clearance; stance_hold's legs are raised to clear the floor.
- **stun:** arms lifted 18 deg (right) and 16 deg (left) off the tassets, legs raised where the boot sank, soft motion on the
  loop plus a small sway on still pieces. The helm-clearance pass was tried and dropped: it popped the left arm 36 deg at t1.75
  and broke the loop (scorecard loop_pose 33).
- Lengths, impact markers and timeline markers are unchanged on every clip.

### Rig differences (frame proof)

- `secondary_life` helm_plume on every action clip: the crest is fixed to the helm's ridge and must not swing (Kort, knight
  round 3). Frame `work/render/ready0`.

### Open (row, tick, frame)

- attack_side_hew: helm into right_spikes 2.75 at t12.5 (real; the right plate's spikes touch the helm side in the follow-through,
  `work/render/final_side_hew/frame_0013_t13.png`).
- attack_haft_jab: helm into left_spikes 3.38 at t8.5; weapon into left_arm 1.88 at t4.5 (the haft against the upper arm in the load).
- attack_overhead_chop: weapon into helm 1.83 at t10.5; left_gauntlet into helm 2.26 at t8.5 (the heave over the crown); left_arm
  pop 20.7 at t3.75 (an arm IK branch change at key t4, already in the lane's re-solve; easing did not remove it).
- charge_run_upper: weapon into helm 1.82 at t0.5; left_skirt into tabard_skirt 1.97 at t2 (run gait base, same row as run_upper).
- run_upper: right_skirt into tabard_skirt 1.97 at t5.5 (gait clip, unchanged).
- hit_pulled: cape_cloth into tabard 2.14 at t3 (2.27 at the lane).
- stun_upper: weapon into hips 1.87 at t20.25, right_gauntlet into hips 2.01 at t7.75, right_boot under the ground 1.68 at t0.75,
  helm into left_spikes 2.16 at t1, right_tasset still. stun_out_upper: right_boot floor 1.68 at t0.75 (tool pairing with
  stun_legs under change by the lead), right_skirt/right_tasset/left_pauldron still.
- Scorecard (lane: exit 0; now exit 1, 4 clips): pen_limb attack_overhead_chop 1.51, attack_side_hew 1.49, attack_haft_jab 1.82
  (limit 1.39; the left forearm against the chest, jab at t5.5 and t13, after the left arm was moved off the helm);
  charge_run_upper loop_pose 3.58 (limit 1; the helm re-solve does not close the loop seam).

### Tests

- `python -m unittest` on test_scorecard, test_focus_check, test_render_geo and the six test_booth* files: all OK.
- `python tools/verify.py fast`: PASS (`work/review/verify_fast2.log`). The first run failed one checkq timing test
  (test_cancel_running_stops_its_tree), which passes alone and on the rerun; no file of this pass touches checkq.
- In-game reel (checkq 20261112-081504-306-4018, root `ic_breaker-review1`, geo-knight): 22 `[p1] p1-` lines, 20 true.
  `attack-sync-walk=false` with 3/3 walking swings matched (one under the check's floor of 4, as in both lane runs of
  2026-11-07) and the suite roll-up. Stand sync 26/26, attack frames 81/81, stances, hits, pulls and deaths all true.
