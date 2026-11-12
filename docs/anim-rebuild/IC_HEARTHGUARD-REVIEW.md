# ic_hearthguard review record (2026-11-06)

The first axe unit, the Iron Clans hearthguard: a hand axe and a round shield (`axe_shield`). Its builder reuses the knight's shield guard, gait, stances, hits, block and falls, and replaces only the attacks: `GG_AXE_TROOP=ic_hearthguard python art/anim/axe/build_axe_anims.py`. Its rig comes from `art/anim/common/troop_rig.py ic_hearthguard`, with the axe rolled so the bit faces the foe in guard and leads every chop.

- **Attacks:** overhead_chop (impact 10, head) and hook_hew (impact 8, body), each with an `_legs` clip, inside a 24-tick clip under the variant's 28-tick cooldown. The timeline `impact` marker sits on the server's impact tick.
- **Cloth:** the knight's cape lag is turned onto the tabard (1.0) and the beard braid (0.6); the flare is dropped so the tabard never swings into the thighs.
- **Knight builder:** `SHIELD` now includes ic_hearthguard, so the shield guard, block and shield-arm falls apply. Other troops are unchanged.
- **Round 1 fix: the axe stayed above the floor.** In the chop's follow-through the axe dipped under the soles. The bite now stops above them; `AXE_PROBE=1` prints the axe's lowest point per swing.
- **Round 1 fix: overhead_chop.** The drop now reads over two frames (t9 mid-arc), the bit lands at chest height on the impact tick, the follow-through carries it down to the bite, and the gather lifts it rather than letting it dangle.
- **Round 1 fix: hook_hew.** The hew comes across from high right so the bit is crossing the body on the impact tick, carries past the left hip, then the elbow folds and the bit hooks back toward the belly.
- **Round 1 fix: left death.** Rolling face down over the shield arm, the round shield stood up off the back. A dedicated `ROLL_LEFT` wrist turns it the other way, so it lies flat over the back.

## Review

- **Round 1 (Sonnet high): SHIP WITH MINORS.** The fixes above.
- **Round 2 (Sonnet high, reel `reel_hg_v2`): PASS. No blockers.**
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false. The first run failed only on two harness assumptions (3 attack clips of 22 ticks); `GeoKnightChecks` now reads the troop's attack variant count and clip length.
- **Verdict: finished.** Video: `ic_hearthguard_v2.mp4`.

## Open minors (polish, not blocking)

- **overhead_chop** (t303→t304): the drop is a one-frame jump with no smear.
- **hook_hew** (t338–344, side view): the bit stays near chest height.
- **Shield:** no secondary lag in either attack.
- The first and last hit reactions are weaker than the middle ones.
- The ready idle's weight shift is small.
- The death framing crops part of the body in the reel.

# ic_hearthguard: v2 style + v4 deaths (2026-11-07)

The hearthguard now builds in the v2 shield style (Kort approved v2 as the default for shield units), so its two attacks, upper stances, hits and block changed, not only the deaths. It also has the knight's 12 v4 deaths: `death_{back,forward,left,right}`, each plus `_drop1` (drops the axe) and `_drop2` (drops the axe and the shield). The export goes from 42 to 50 clips. It is the first rig without a cape to take the D3 deaths, so `_d3_tracks` in `art/anim/knight/build_anims.py` now skips keyed bones that are not in the troop's `DEATH_BONES` (without it the axe build fails with KeyError 'cape'). The guard cannot change a troop that built before, since any skipped key would have raised.

- **Supersedes:** the old "death framing crops part of the body" minor above; the v4 reel frames the whole fall.
- **Byte-identical (parsed JSON):** knight, revenant, tc_raider, archer, ic_arbalist, wm_tusk, tc_harpooner, ic_breaker and tc_mist build the same with and without the guard; gl_marshal with the guard matches its committed export. D3 stays gated on `D3_TROOPS`.
- **Review (Sonnet, high effort, reel `ihg_v2`): SHIP WITH MINORS.** It covered the whole set (gait, stances, idle, 2 attacks, 7 hits, 12 deaths) from contact sheets, 12 death sheets and the full reel. No blockers: every death settles on one floor line, the shield always lands flat, and all four falls read in their direction. No fix round was needed.
- **Harness (geo-knight, `GRAVES_GEO_CHECK_TROOP=ic_hearthguard`):**
  - Run 1: 22 p1 and 1 `=false`. That failure was the march run-side check under lag (3 "Can't keep up"). The suite rollup line follows from it.
  - Rerun: 22 p1 and 0 `=false`.
  - The death clips that played included `death_back_drop1`, `death_back_drop2`, `death_forward_drop1`, `death_forward_drop2`, `death_left`, `death_left_drop2`, `death_right` and `death_right_drop2`.

## Open minors (v2 + v4, polish, not blocking)

The shared v2 and v4 minors in `SENTINEL-REVIEW.md` and `GL_MARSHAL-REVIEW.md` apply here as well.

- **death_left and death_right (all variants):** they end as a propped, hunched slump with the torso at about half height, not lying flat. The pattern is the marshal's, but the stocky body and wide pauldrons make it more obvious.
- **death_left (shield kept):** the shield tucks under the torso, so the direction reads from the body, not the gear.
- **death_back (kept gear):** at t=13-15 the axe ends past the head and the shield past the feet; the silhouette is wide and busy.
- **death_back_drop1 and drop2:** the axe lands slightly above the head line at t=13-15 and settles flat by t=18. It is a soft arc, not a pop.
- **death_forward drop variants:** the dropped axe ends partly under the forearm and the shield edge, crowded near the body.
- **death_right_drop2:** the shield lands about a body-width away, with the axe beside it.
- **Not seen in game:** only the harness and the offline reel were run.

## Batch review 2026-11-11

Branch `anim/review-hearthguard` (lane `anim/stance-hearthguard` c73ad676 with the skill lane merged). Notes, scripts and frames are in `work/review/` (not committed).

**Baseline.** The full build ran out of memory twice in `deaths.py` (machine load), so the deaths were not rebuilt. Every other clip (38), built by the committed builder, matches the committed export byte for byte, so there is no drift. The committed `out/` copy had stale attack clips: an earlier chop splice went into the export only. This commit splices the same new attacks into both files. `stances.py all` (to scratch) leaves every other troop byte-identical except two drifts that HEAD's `stances.py` reproduces as well: hexer (known) and stalker (its committed export lacks the batch-1 stance clips).

**Focus check.** Before: 40 clips, 19 with a FAIL (38 rows). After: 13 with a FAIL (26 rows). Every remaining row is either the tool's edge sign or a shared hit/reaction clip (below). Scorecard before: exit 0. After: exit 1 on edge_lead only. Under the corrected sign it passes.

**Changed clips (11), in the builder only:**
- **stance_brace / brace_up:** the axe arm comes in from 30 to 14 deg of abduction. The right pauldron boss was in the helm on the breath (2.56 / 2.36). The rows are cleared; a floor WARN 0.54 remains.
- **stance_ward:** the arm comes in from 26 to 18 for the same boss (2.29). Its row is cleared.
- **stance_wall (+ wall_up through stance_wall_legs):** the shield fist rides 1.8 px higher (the rim was 2.73 px under the ground). The footing sinks 2.5 px instead of 3.9, and the rear foot comes 1.8 px nearer (knee guard 2.07). A floor WARN 0.91 remains, and the wall still sits lowest.
- **stance_fallback_legs:** the keyed row in `stances.py` loses its extra 0.6 px hips drop, which put the boot 1.33 px under the ground at t0. A WARN 0.73 remains (the family's value).
- **attack_overhead_chop / hook_hew (+ legs):**
  - **Edge side:** the axe's bit is on the weapon bone's local -x. In the exported geo the bit is at x 0-0.31 and the back spike at 0.44-0.75, the same layout as ic_breaker, which uses -1. The knight table's +1 turned the head half round in the load, so the spike led the chop (`work/review/render/cmp_head.png`, t11) and the axe spun in the hand (grip_roll 59.6 / 41.6). The builder now sets `K.EDGE_SIGN = -1` and both swings turn at 15 deg per sample from a tick before the drive. Result: grip_roll is clean, edge_lead is 15.5 / 9.8 deg under -1, and roll_spin is 50 / 78.
  - **Floor:** the hips ride 1.2 px higher around the impact (smooth, ±3.5 ticks). The floor rows (1.56 / 1.59) are now WARN 0.81 / 0.97.
  - **Soft motion:** `fullbody.soft_motion` now moves the braid, the tasset plates and the pauldrons (all secondary_life rows cleared). The tabard keeps cloth_lag.
  - **Tabard:** it swings 14 deg forward off the leading thigh in both follow-throughs, and 8 deg in the hew's drive, where the lag had thrown it into the coat.
  - **Boss in the hew:** during the load the right pauldron slides 2 px out along the arm (t3-9). Its boss was 2.5 px in the breastplate.
  - Length, impact markers and keys are unchanged. Rule 6 and the facing were checked by eye: the bit leads both cuts in all three views.

**Proposed tool change (not made; outside the boundary):** set `EDGE_SIGN` for ic_hearthguard to -1.0 in `tools/anim/scorecard.py` (line 123) and in `scorecard_limits.json` "edge_sign". Under +1, the clips in which the bit visibly leads score 178 / 179 deg.

**Shared rows left open (identical on sentinel, from the knight's hit code and `reactions.py`):**
- **Additive hits:** hit_block elbow 176.9; hit_medium_back knee -13.0 and tabard 1.95; hit_medium_right helm/boss spike 1.99; secondary_life on every medium hit.
- **Reaction clips:** hit_pulled floor 1.39, elbow 162.8 and secondary_life; hit_knockback 5 rows; stun / stun_out floor 1.91, head into boss spike 3.57 and secondary_life; charge_run braid 1.93.
- **run_upper:** limb_in_body 2.03 (family gait).

The hits are measured over their `_legs` only. In the game they are added on top of the stance. Looked at by eye, hit_pulled, stun, knockback, block and charge_run read for an axe-and-shield dwarf.

**Open minors:**
- shield_up reads close to ready at 16 blocks.
- The attacks start and end on the knight GUARD, not this unit's stance_ready, so the hand-over is a crossfade between two different poses.
- Both attacks keep the phases WARN (they stop at the impact).
- The stance clips were looked at every second tick (40-tick breathing loops). The attacks were looked at every tick.

**Tests:**
- `python -m unittest discover -s tools/anim`: 152 tests OK.
- `python tools/verify.py fast`: every check passes except python-tests. That suite timed out under machine load (exit 124). Rerun alone, 725 of 727 tests pass. The two failures are speed limits in `test_where` (65 s against a 30 s limit, and 3.2 s against 3 s), not functional failures.

**In game (geo-knight suite through checkq):**
- `ic_hearthguard-review1`: 22 p1 lines and 2 `=false`: march-run-side (wrong clip) and attack-walk (gait while swinging). The server was 21 s behind ("Can't keep up" 4 times).
- Rerun `ic_hearthguard-review1b`: 22 p1 lines and 1 `=false`: march-run-threequarter (speed samples 0.010 under lag, "Can't keep up" 3 times).
- Every attack, stance, hit, hit_pulled and death check passed in both runs. Each failing check passed in the other run.

## Fix round 1 (2026-11-12, Sonnet review FIX 6/10)

Focus check (hits and stun_out played over stance_ready): 12 clips / 22 FAIL rows before, 3 clips / 3 FAIL rows after. Scorecard 40 clips, 0 fail.

- **Hits and stuns, this troop only:** `art/anim/axe/hearthguard_hits.py` rebuilds hit_light_*, hit_medium_*, hit_block (knight builder, through build_axe_anims) and stun / stun_out / knockback / pulled (`reactions.py`) sample for sample (checked: with no fixes the core bones match the old export exactly), then adds the per-clip fixes and runs `fullbody.soft_motion` over each clip added onto stance_ready. Braid, tabard, tassets and pauldrons now trail in every hit and stun clip; no secondary_life FAIL is left. Knight and sentinel files are not read or written.
  - hit_pulled: axe arm flung 25 deg less (haft in helm 2.41 -> WARN 1.5); hips up to 2.5 px higher through the drag and the catch (boot 2.29 -> WARN 0.88).
  - stun_upper: snap flares the axe arm 25 deg less, head thrown back 14 deg less and turned 10 deg off the boss (3.57 -> WARN 1.81); shield arm 8 deg higher; hips 2.2 px up through the snap, 0.6 px through the loop (floor 1.91 -> WARN 0.81). The trailing pieces are run over three loop passes and the middle one kept, so the loop closes exactly.
  - hit_knockback: wrist flick 18 deg less (haft on helm 1.64 -> WARN 1.07); hips raised through both stumbling steps (1.9 -> WARN 0.68); tabard swung off the thigh.
  - hit_medium_right / stun: the tabard swings forward off the thigh where the soft lag put it on the leg.
- **Attack hand-off:** both swings start and end on stance_ready's own first sample (upper exact; legs within 1.1 deg), the feet start from and walk back to the ready footing, and the knight-guard recovery key is eased halfway onto the stance. Length and impact unchanged.
- **Follow-through:** the chop's arm drives 8 deg further down and the chest 8 deg further over, the hew carries 12 deg further across (chest 8), peaking 2.5 ticks after the impact. Both phases WARNs are gone. The hew's hips ride 0.6 px higher at t11 (knee guard).
- **Stances:** brace_up axe arm 20 deg higher; ward shield fist 3 px higher, axe level. Focus check clean (`work/review/render/stances_cmp.png`, old row top). Brace and brace_up still share the footing and read close at 16 blocks.

**Still open:**
- stun_out_upper floor 1.87 (t6). The check adds the ABSOLUTE stun_legs (its booth base) on top of stance_ready_legs, which doubles the knee bend; stun_legs alone is WARN 0.81. The knight has an exemption for this row. Needs a tool or booth-base decision, not a clip change.
- run_upper limb_in_body 2.03 and charge_run_upper braid 1.93 (review F6): not in this round's list, not touched.
- The hit file grew (trailing pieces keyed every sample).

**Checks:** `python -m unittest discover -s tools/anim` 153 OK. Booth batch `ic_hearthguard-20261112-b9c6d2f` (40 clips, built at b9c6d2fb, not uploaded); phone video `status/booth-videos/ic_hearthguard_review.mp4` (3:08, previous beside new). In game, `ic_hearthguard-review2` (geo-knight suite through checkq): 23 p1 checks, all true, 0 `=false` ("Can't keep up" 3 times).
