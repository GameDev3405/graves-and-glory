# wm_stalker review record (2026-11-09)

The Web Stalker (Wildmoot rare cavalry beast, group F, ability `ensnare`) gets its first GeckoLib model and a full v2 animation set, following the wm_hound template.

**Rig.** `python art/anim/wm_stalker/build_rig.py` re-hangs the hand art (`art/hand/wm_stalker`, 256x128, three tiers) on a 50-bone spider skeleton:

- root > body > thorax > head > palps, fangs (tiers II and III fang sets), mandibles
- abdomen > sacs and sacs_ii
- eight legs of two bones each: femur and tibia

Every source box keeps its absolute place, so proportions and texture UVs are unchanged. The hand art's rest x and z rotations are negated going in. Output: `out/wm_stalker.geo.json`, the rig, 18 tier/colour textures, and Blockbench-compatible preview geos `out/preview/t{1,2,3}.geo.json`.

**Clips.** `python art/anim/wm_stalker/build_wm_stalker_anims.py` writes 46 clips, and `art/anim/common/export.py wm_stalker` exports them.

- Legs are solved by least-squares IK (`stalker_ik.py`) against the combined body pose.
- Each femur solves pitch, yaw and roll and each tibia solves bend (4 DOF), with soft knee-up and anti-gimbal terms.
- `--deaths`, `--fallback` and `--ensnare` re-splice only those clips into the existing output.

| Clips | Length | Notes |
|---|---|---|
| `idle_legs` / `idle_upper` | 60t loop | Abdomen breathing bob and sway, mandible and palp twitches, head flicks. |
| `walk_legs` / `walk_upper` | 6t loop | Alternating tetrapod: L1 R2 L3 R4 against R1 L2 R3 L4, duty 0.52, body low and level. |
| `run_legs` / `run_upper` | 5t loop | Faster, lower skitter (duty 0.42), with more abdomen sway. |
| stance ready / brace / wall `_legs` and `_upper` | 40t loop | Ready raises the forelegs in threat. Brace sinks wide. Wall is a high, square sentry. |
| `stance_fallback_legs` / `_upper` | 6t loop | Backward tetrapod at 0.7x walk speed, with a guard pose. |
| `stance_ward_upper` | 40t loop | Guard pose (upper only). |
| `attack_bite` (+ `_legs`) | 18t | Rears up with forelegs high, slams down, and pins with the fangs. Impact at t8 (`"0.4"`). |
| `attack_lunge` (+ `_legs`) | 18t | Coils, then leaps 5.5 px forward with the forelegs reaching. Impact at t8. |
| `ensnare` (+ `_legs`) | 18t | Rears higher than the bite with both front pairs up and wide, abdomen curled under. It holds that coil, trembling, from t5 to t7 (the spit beat). At t8 the forelegs fling forward and wide like a net, the second pair slams out sideways, and the abdomen whips up. It holds the cast with reeling tugs until t11. Impact at t8. |
| `hit_{light,medium}_{front,back,left,right}` (+ `_legs`) | light 9t, medium 16t | Additive deltas against ready. The controller grades heavy from medium. |
| `hit_knockback` (+ `_legs`) | 20t | Lifted and pitched back, legs scrabbling, then it lands. |
| `stun_legs` / `stun_upper`, `stun_out_upper` | 24t loop, 6t | Legs drawn in, body sagging, slow twitch. Snaps back to ready. |
| `death_{back,forward,left,right}` | 20t, hold on last frame | back: flips onto its back, legs curled over the belly. forward: the legs give and every claw skids out (IK-planted) as it sinks flat onto its belly, fangs and mandibles splayed. left / right: rolls onto that side, legs drawn in (right curls tighter). Each lands, bounces and settles. |

**Server timing.** The spider family's `spider_bite` and `spider_lunge` both have delay 8, and the troop's cooldown is 25.

- Each attack clip's impact marker is at t8, and the harness's attack-sync check measured it at the server's damage tick: 14 of 14 standing and 4 of 4 walking.
- Ensnare is an `onhit_status` ability with an 80-tick cooldown. The server sends no client event for it.

## Code changes (presentation only)

- `GeoTroopRenderer.TROOPS` gains `wm_stalker`.
- `AttackController` gains an ensnare hook, which is client-side and presentation only:
  - It applies to a troop whose ability row is `onhit_status` with a cooldown.
  - The first swing plays the clip named after the ability (`ensnare`) instead of the swing clip. After that, ensnare plays whenever at least the ability's cooldown has passed since the last ensnare shown.
  - This mirrors the server's on-hit status cadence. It is an approximation: a miss, a block, or a troop first seen mid-fight can put it out of step with the real slow.
  - Only the latest swing, or one still inside the clip's length, may claim the ensnare. Older swings a troop keeps when it comes back into view do not decide.
  - The rule is generic: any `onhit_status` row with a cooldown (today also tc_harpooner's `harpoon`) gets its ability clip the same way if its clip file has one.
  - There is no gameplay, server or test-tool change. A server ensnare-ready flag is a separate question for Kort.

## Fixes during authoring

- **Leg reach.** The first pass missed by 2.6 to 7 px because the rest tips sat too close to the body and the femur roll hit gimbal lock. The fixes:
  - wider rest tips
  - the 4-DOF femur solve
  - foreleg targets moved forward
  - less leg-2 spread in ensnare
- **Worst IK misses now.** Every clip is under 0.25 px except `attack_lunge`. Its hind legs miss by 1.7 px mid-leap, at full extension, while airborne.
- **Ground clearance.** In ensnare and knockback the sacs went through the floor (down to -4.4 px). `floor_clear` now lets the abdomen pitch up by up to 60°, and the body lifts more.
  - The lowest body point is now 0.30 px above the floor, and the legs reach -0.07 px.
- **death_forward stood on its legs.** The legs now splay and roll out (rx 6), and the body settles onto the floor.
- **Harness fix.** The stance check expects `stance_fallback_upper` and `_legs` while backing under the ward stance. The fallback clips were added.

## Fix round 1 (Sonnet review: fix first, narrow)

- **Floating corpses (must-fix).**
  - **Symptom:** every corpse's lowest edge sat 0.1 to 0.2 block over its shadow in game, and death_forward stood on stilt legs.
  - **Cause:** `lowest()` counted every cube in the one shared geo, including tier-only gear, so each corpse was lifted until tier III gear touched the floor. Tier I and II draw none of the crest, mandibles or spurs, so they hung 5 to 6 px over the floor (back, right) or 3 to 4.7 px (forward). The offline previews used the tier III geo, which hid it.
  - **Fix (general).** `death_clip` now:
    - lays the corpse on the parts every tier draws (`CORE`, read from the rig's stage rules);
    - lets higher-tier armour sink at most 1 px;
    - turns the head and abdomen just enough to keep tier gear (fangs, mandibles, crest, trophy) off the floor, choosing the direction once from the final pose so the keys stay continuous.
  - The builder prints each death's final lowest point per tier:

    | Death | Tier I | Tier II | Tier III |
    |---|---|---|---|
    | back | 0.00 | -0.26 (ridge plates) | -0.26 |
    | forward | 0.00 | 0.00 | 0.00 |
    | left | 0.00 | 0.00 | 0.00 (spur -1.5) |
    | right | 0.00 | 0.00 | 0.00 |
- **death_forward is now a real slump.** The body sinks onto its belly (0.3 px), and the claws skid out along the floor, IK-planted at 0.0, so the knees splay. The fangs, mandibles and palps fold forward off the dirt, so the body stays level.
- **Corpses lie over their shadow.** A roll turns about the root on the floor and carried the body 3 px sideways, so the root now recentres the body's x/z over where it stood. A fall onto the back keeps 2 px of backward travel.
- **Variety.** death_right now rolls onto its right side (rz -96) with the legs curled tight. It no longer ends on its back like death_back.
- **Ensnare readability.** See the clip table. IK misses are under 0.1 px, and the lowest body point is -0.33 px (the sacs graze the floor at the coil).
- **AttackController.** Swings that are not the latest and are older than the ensnare clip no longer decide it (see Code changes).

## In-game checks

Command: geo-knight phase-1 checks with `GRAVES_GEO_CHECK_TROOP=wm_stalker`.

- **Run r1:** stalled at resource load until the 1800 s timeout, with no `[p1]` lines. This was a launch flake; the rerun used a fresh root.
- **Run r2:** 22 lines. `stance=false` on ward/fall_back because the fallback clips were missing. Fixed above.
- **Run r3 (final clips): PASS, 22 of 22, 0 false.**
  - `attack-frames`: 34 of 34 side and three-quarter, 16 of 16 far
  - `attack-sync-stand` 14 of 14, `attack-sync-walk` 4 of 4
  - march walk and run, stance, hit and death all true
  - `knight-suite` true
- **wm_hound regression run:** 22 lines. Only `stance=false` (ward/fall_back) and the `knight-suite` failure that follows from it.
  - The cause is that wm_hound has no `stance_fallback` clips. It was built before the fallback stance check existed, so this is pre-existing and not touched here.
- **Fix round 1, run f1** (deaths and ensnare fixed, before recentring): PASS, 22 of 22, 0 false. Sync was 14 of 14 standing and 4 of 4 walking.
- **Fix round 1, run f2** (final clips): PASS, 22 of 22, 0 false.
  - `attack-frames` 34 of 34 side and three-quarter, 16 of 16 far
  - sync 13 of 13 standing and 4 of 4 walking
  - stance, hit, death and `knight-suite` all true
  - The final in-game death frames show each corpse on its shadow.
- **Video capture run v1** (`GRAVES_GEO_CAPTURE=video`): frames for idle, stances and aim-sweep.

## Open minors

- The lunge's hind legs miss by 1.7 px in the airborne phase.
- The ensnare hook is approximate (see above).
- There is no `hit_pulled`; the hound does not have one either.
- There is no `hit_block`, because the stalker has no shield.
- The Blockbench MCP was unavailable (ECONNREFUSED), so all work went through the Python builder and render_geo previews.
- Round 1 (Sonnet high) came back fix-first; this record covers its fixes. Round 2 is with the orchestrator.
- Tier III spurs dig 1.5 px into the floor in death_left, and tier II/III ridge plates sink 0.26 px in death_back.

## Port to main (2026-11-09): scorecard loop seams

On main the scorecard failed three looping clips on `loop_vel` (walk_upper 72.8 vs 51.2 deg/tick on left_leg_1_low,
run_upper 178.5 vs 121.6 on left_leg_1, stun_upper 23.7 vs 19.5 on left_leg_3_low). Fixes in the builder, no timing change:
walk and run start a third and a fifth of a stride in (`WALK_T0` 2, `RUN_T0` 1 tick) so the wrap no longer lands on a
claw's touchdown or lift-off; the run's sac bounce phase moves 0.42 -> 0.36 of a stride; the stun snap starts at 0.75
on frame 0, as the knight's does (the controller loops ticks 4..24, so tick 0 plays once). Only walk, run and stun
changed; scorecard 26 clips, 0 failing. Not yet re-rendered or re-reviewed. The old branch's AttackController ensnare
hook was not ported (gameplay code).
