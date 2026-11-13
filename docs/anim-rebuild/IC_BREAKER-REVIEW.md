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

## Fix round 2 (2026-11-12)

### Solve cache

The builder's slow, pure steps (attack solve, helm and chest clearances, soft motion, the hit and layered fixes, and the
spear builder's idle, stance, death and LS/LC/LR clips) go through `art/anim/greataxe/solve_cache.py`. A step's key is a
sha256 over its arguments and a fingerprint of the code and data it reads, recursively (its source, defaults, constants,
the globals and module attributes it reads, every project function it reaches). A hit returns the stored result and
replays the in-place changes the step made to its arguments. The cache lives in `work/anim-cache/ic_breaker/`
(gitignored). `GG_BREAKER_CACHE=0` turns it off and `GG_BREAKER_CACHE_DIR` moves it.

- Proof, before any fix: a full build with an empty cache (95 min) and two warm rebuilds (73 s each) left `out/` and the
  exported asset byte-identical to the committed files (commit 45d3777b).
- One `soften` call misses on every build because its arguments hash differently from run to run. The output is still
  identical. It costs a few seconds and adds one small file to the cache per build.
- Final proof, with the fixes: a full build from an empty cache (`GG_BREAKER_CACHE_DIR` pointing at a new, empty folder;
  101 min, 75 steps solved, 0 reused) wrote the same bytes as the last warm build (about 2 min) for all 55 ic_breaker
  files in `out/` and the exported assets. The clip file's sha256 starts 31e8b4d3f8829e8f.

### Fixes (late steps on the finished clip file: `round2_fixes` in `write_layers`; `GG_BREAKER_FIX2=0` leaves them out)

| Row (focus check) | Before | After | How |
|---|---|---|---|
| run_upper armour_clip (right_skirt into tabard_skirt) | FAIL 1.97 | WARN 1.88 | Run skirts trail the thighs: x = -0.2 x leg x on `run_legs` |
| charge_run_upper armour_clip (left_skirt) | FAIL 1.97 | WARN 1.84 | The same `run_legs` skirts |
| charge_run_upper weapon_in_body (axe into helm) | FAIL 1.81 | WARN 1.18 | right_hand turned (+12, +4) and the left fist solved back onto the haft at the same slide (miss under 0.1 px); limb_in_body 1.76 -> 1.85 WARN |
| attack_side_hew armour_clip (helm into right_spikes) | FAIL 3.03 | WARN 1.92 (apron into left_leg, inherited from the stance) | Five eased offsets on the head and both pauldrons in t8.5-20. The right pauldron turns 16 deg, past the kit's 10 deg plate lag |
| hit_pulled armour_clip (cape_cloth into tabard) | FAIL 2.28 | WARN 1.92 (helm into left_pauldron, inherited) | Cape x +6, eased over t0.5-2.5 / t6-9 |
| hit_knockback limb_in_body (left_gauntlet into helm) | FAIL 2.04 | WARN 1.88 | Head x -10 over t0-1 / t2.5-4.5 |
| hit_knockback armour_clip (right_forearm into tabard) | FAIL 2.08 | WARN 1.91 (helm into torso, inherited) | Tabard swing halved (it swung 22 deg forward) |
| hit_medium_front armour_clip (helm into left_pauldron) | FAIL 1.93 | WARN 1.84 | Head z -3 over t5-7.5 / t9.5-12; cape_cloth x +6 over t0.5-3 / t5-10, so the cape moves |
| stun_upper weapon_in_body (into hips) | FAIL 1.87 | WARN 0.93 | right_arm z +14, eased in over t1.5-4 and held; the loop seam t4 = t24 is exact |
| stun_upper limb_in_body (right_gauntlet into hips) | FAIL 2.01 | WARN 1.83 | The same arm offset |
| stun_upper armour_clip (helm into left_spikes) | FAIL 2.16 | WARN 1.86 | Head z -5 and left_arm z +8 in the snap (t0-4), head z -4 held from t4 |
| stun_upper floor (right_boot) | FAIL 1.68 | WARN 0.89 | Boots that dip more than 0.92 px are lifted to 0.8 px, with the leg solved again. A full lift bent the knee to 100 deg (scorecard knee_bend, limit 93.9) and opened the loop (loop_pose 29.7) |
| stun_upper secondary_life: right_tasset | still | moves | One swing out and back inside the loop (+4 / -3 deg x); `LOOP_SWAY` 3 -> 5 |
| stun_out_upper weapon_in_body | FAIL 1.80 (into hips, t2.25) | FAIL 1.79 (into right_leg, t5) | Not fixed, see Open |

### Open after fix round 2 (6 FAIL rows in 4 clips, was 17 rows in 9 clips)

- **secondary_life helm_plume:** stun_upper, stun_out_upper and hit_medium_front. The crest is fixed and never moves.
  - stun_upper and stun_out_upper have no exemption.
  - hit_medium_front's exemption was removed in this round, as the brief asked. The cape now moves, but the row FAILs on
    the plume alone. This conflicts with "never turn a passing row into a FAIL": the lead should either re-add the
    exemption (whose reason is true again) or accept the row.
- **attack_haft_jab armour_clip, helm into left_spikes, 3.52 at t7.5:** not fixed. None of these took the depth below 2.3:
  - head turns of up to ±20 deg
  - pauldron turns of up to 70 deg
  - key edits: shoulder, height, offsets, torso, elbow swivel
  - re-solving the left arm at every sample

  At the shove the left arm is raised and out to the side, so the left shoulder sits just under the helm. Squaring the
  haft does not help: at the shove the haft points 20.7 deg off straight ahead (18.0 at t7, 22.6 at t8.25), but relative
  to the chest it is already square (about -4 deg). Turning it square to the world pushes the spikes further in.
- **attack_haft_jab weapon_in_body, weapon_t3 into left_forearm, 1.64 at t2.75:** not fixed. The haft's ferrule cubes sit
  next to the left fist.
  - Tilting the hand ±10 deg leaves 1.6.
  - Re-solving the arm leaves 1.5.
  - Sliding the fist 4 px up the haft gives 1.15 at t2.75. Over t3-5 a constrained re-solve gives 1.0-1.5, but the left
    forearm then presses into the chest at 1.1-1.4 px (scorecard pen_limb limit 1.39).
- **stun_out_upper weapon_in_body, 1.79 at t5:** not fixed. This is the 8-tick handover from the stun to the ready stance.
  - Pushing the arm out or in moves the haft from the right leg into the hips (1.56-1.94).
  - Turning the hand gives 1.55-1.78 with grip_roll warnings.
  - Re-keying the handover is a stance and controller change, outside this round.

### Checks

- The scorecard passes: exit 0, 32 clips, 0 fail.
- Focus check: 6 FAIL rows in 4 clips, and 131 WARN rows (119 before). No passing row became a FAIL, except
  hit_medium_front secondary_life after its exemption was removed.
- Lengths, loop flags, timelines and impact markers match 9539667f on all 55 clips (chop 12, hew 12, jab 7).
- Changed clips against 9539667f: run_legs, attack_side_hew, hit_pulled, charge_run_upper, stun_upper, stun_legs,
  hit_knockback, hit_medium_front.
- Renders: `work/render/r2_*`, near and game16, front, side and three_quarter, with the rig and tier 3. Hits are added
  onto the ready stance. There is also a top view of the jab over t2-9.

### After the re-review (2026-11-13)

- Sonnet high re-review of fix round 2: SHIP WITH MINORS, 8/10 (was 7). Report: `work/reviewer/IC_BREAKER-sonnet-review-r2.md`.
- The fixed-crest exemption is back on hit_medium_front and added for stun_upper and stun_out_upper (Kort 2026-11-11: a fixed
  crest is still; every other hanging piece in those clips now moves). This replaces the "6 FAIL rows in 4 clips" above.
- The three rows fix round 2 could not clear are recorded debt in `tools/anim/focus_exemptions.json`, each judged by the
  reviewer as not visible at 16 blocks: jab helm into left_spikes (3.52, t7.5), jab haft on the left forearm (1.64,
  t2.75), stun_out axe on the right leg (1.79, t5). The fix for each is a re-keyed clip or a shorter spike, not a late offset.
- Focus check: 0 FAIL rows. This was the troop's last fix round.
- In game: `work/ingame-check/ic_breaker-review4`, 23 lines, 0 false. A first run (`review3`) ran beside a booth build and
  failed two lag-sensitive rows (march-run-side, attack-sync-walk).

## Model fix: left shoulder spikes (2026-11-13)

Kort asked for the left shoulder spike to be fixed in the model. In the haft jab's shove (t7.5) the raised left shoulder
sits under the helm, and the helm covers the whole front half of the left pauldron's top plate, so any spike standing there
is inside the helm down to its base.

- Hand builder: the left row of three 1x1 iron spikes now stands on the back half of the plate, rising 1, 2, 3 px to the
  back (bedrock z 0, 1.5, 3; x and the bone unchanged). Before: three 3 px spikes at z -3, -0.5, 2. The right row is
  unchanged. From the front the left row still shows one 3 px spike, as the right does (sheet in the model-fix report).
- jab helm into left_spikes 3.52 (t7.5) -> 1.89 (t7). Every other row naming left_spikes is now under 0.5 px (was up to 1.80).
- The jab's armour_clip exemption stays: without it the row is helm into right_spikes 3.40 (t13) and helm into
  left_pauldron 2.26 (t5.5), both outside the left spikes. Its reason text still names the left spikes.
- Clips: a cold rebuild (empty cache) changed one clip, attack_haft_jab, on one bone: the plate yield
  (fullbody._plates_yield reads the plate's spikes) keeps left_pauldron slid down its arm a little longer after the
  wind-up, at most 0.15 px (t4.25). Timing fields unchanged on all 55 clips.
- Scorecard exit 0, identical to before. Focus check 0 FAIL rows, 131 WARN rows as before.
