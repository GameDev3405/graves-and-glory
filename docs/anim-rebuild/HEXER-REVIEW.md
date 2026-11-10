# hexer review record (2026-11-06, caster v2 2026-11-07)

The Gravecaller (hexer), the second caster: a grave lantern hung from a short pole in the right fist, a burial robe and a hood. Its clips come from the caster builder (`art/anim/caster/build_caster_anims.py`, `GG_CAST_TROOP=hexer`). The mage's built output is byte-identical; every hexer change branches on the troop.

## v2 (2026-11-07)

Redone in caster style v2 (`CASTER-V2-DESIGN.md`), the last of the four casters after the wm_seer pilot, the mist caller and the mage. Every hexer change branches on the troop; the wm_seer, tc_mist and mage outputs rebuild byte-identical (md5 of all their `out/` files). Blockbench MCP was not used (the clips are Python-built).

- **Robe:** the burial robe rebuilt as four panels under the hips (front, back and two sides), each in three stepped tiers (`troop_rig.ROBE["hexer"]`, `burial_robe`, `top` 11.5). The vestment strips hang from the front and back panels (`troop_rig.HANG["hexer"]`). The bell is a pixel wider and deeper than the mage's (front and back 2.5/4.0 deep, 11 wide; sides 4.5/6.0 out, 7 deep), with the hem at geo 1.5.
  - Each panel swings with the leg behind it (`PANELS` solver, `LEG_R` 0.75).
  - The hexer's side panels hang straight: seen edge-on from the front, a splayed side panel read as a spindly leg under the hem (review round 1). Hexer only: `SIDE_PITCH` 0.2 (of the stride), `SIDE_FOLLOW` 0.25 (of the thigh splay) and `SIDE_HEM_CLEAR` -0.2. Side-panel roll is about 5 degrees at idle and about 2 degrees in the walk and run (round 1: 14-16).
- **Attack (the lantern thrust, the blight curse), 36 ticks, release on tick 10 (`impact`), the server cast tick unchanged:**
  - **Tick 1.5:** the hips load back onto the rear foot and turn the right side away before the arm moves.
  - **Ticks 3.0-4.6:** the lantern is hauled in to the chest on its pole, the pole tipped up, the shoulders turned away (the coil).
  - **Tick 6.4:** the arm drives the lantern out at the foe at chest height, heavy, the shoulder coming round.
  - **Ticks 7.2-8.8:** held at arm's length while the light gathers, the arm locked, a heavy tremor and the lantern swaying on its pole.
  - **Tick 9.4:** the hips drive forward over the lead foot first; the shoulder holds back a beat and the lantern lifts on its pole (the heave).
  - **Tick 10:** the shoulder follows the hips through (torso twist 26 degrees, lean 14) and the lantern is shoved at the foe; the blight leaves the lantern glass.
  - **After:** the lantern kicks up on its pole at 11.6, the arm gives back through the shoulder (14), recover at 17, near at 22, settled on guard by 29.
  - The off hand never joins the cast: it hangs and swings back as the right side drives. The lead foot glides forward under the robe and the rear foot drags after (`attack_cast_legs`). Fist error at most 0.058 px; 0 samples with the lantern in the body.
- **Stances:**
  - **Brace (Hold position):** upright, both arms up, the lantern lifted high over the head on its pole as a warding light, the off hand raised flat beside it. No crouch.
  - **Wall (Hold the line):** leaning in, the lantern held out at the foe at arm's length and high, the off hand pushed forward open below it.
  - **Ward (fall back):** shrinking away, the right side turned back, the lantern hugged in at the chest, the off hand up beside the face to fend.
- **Locomotion:** the walk is the seer-style short glide and the run is the mage's 4-tick scurry under the robe (`GAITS["hexer"]`, the same bell). The lantern's lowest point carries 16.8 px (walk) and 16.9 px (run) above the ground, 0 samples in the body.
- **Hits:** the shared graded set (light and medium per bearing, plus block), with the robe panels following the legs.
- **Deaths:** the mage's falls (`_MAGE_FALLS`) and the mage's robe fold (a dead hexer's panels fold in onto the legs). The lantern lies beside the hand. Rest heights (min-y, px):

  | Fall | Rest heights |
  |---|---|
  | Back | hips 0.02, head 1.55, torso 2.24, feet 2.59, arms 3.76 |
  | Forward | arms 0.0, hips 1.24, torso 3.67, head 4.46, feet 5.09 |
  | Left | arms 0.0, torso 3.25, head 4.99, hips 6.09, feet 8.52 |
  | Right | torso 0.69, arms 0.22, head 2.43, hips 3.51, feet 6.19 |

- **Robe probe** (the seer's poke-through probe): live clips have a worst leg-line margin of 0.73 px and the deepest boot poke is 0.03 px (under 0.25). The probe flags a live hem of -0.20 px (FAIL by its above-the-floor rule) in `attack_cast_legs`, `hit_block_legs`, the medium hits, `idle_legs` and `stance_ready_legs`: that is a side-panel corner grazing the floor by up to 0.2 px, the deliberate price of the straight side panels (`SIDE_HEM_CLEAR` -0.2). The deaths have 0.07-0.10 px leg margins by design.
- **Tip origin (cosmetic only):** `data/gravesandglory/cast_tip/hexer.json` has offset (-0.216, 1.044, 1.119) blocks for all three lantern tiers (the lantern glass at the release). Damage, the release tick and targeting are unchanged.
- **Review (Sonnet high):**
  - **Round 1: FIX NEEDED.** Blocker: spider legs (the side panels splayed out as sticks under the hem in idle, walk and run). Minors: the release was low and lacked twist, the kick-up was small, the settle was late, and the back death props the head and halo up.
  - **Round 2: PASS, no blockers.** The sticks are gone in every gait; the cast beats read (hips lead, release at the server tick, kick-up, early settle); no clipping, no hem sinking; hits graded, deaths grounded with the lantern beside the body.
- **In-game checks (final v2 clips): PASS, 22 of 22, 0 false** (`hexer_v2/harness2.log`, 2 "Can't keep up" lines).
  - The first run crashed at client start (`NoClassDefFoundError: gg/gravesandglory/BuildMode`) while the class directory was being rewritten by a concurrent compile; nothing ran. The rerun passed.
  - Cast frames: 35 of 35 in the side and three-quarter views, and 17 of 17 far.
  - 9 of 9 standing and 4 of 4 walking casts matched the server release; weight 1.0 at the release and 0 between casts.
  - The tip log reads `cast tip hexer: from (-287.284, 66.044, 6.381) eye (-287.5, 67.1, 7.5) body yaw 180`, so the blight leaves the lantern glass in front of the chest, about 1.1 blocks ahead of the eyes.**

## Open minors (v2)

- **Back death:** the head and halo are propped up about 30 degrees (the fall is shared with the mage, `_MAGE_FALLS`; not changed here). In the front view the foreshortened hem tiers can read as small legs.
- **Cast thrust from the front:** the lantern is already held forward on guard, so the thrust reads mainly from the side and three-quarter views; it could take more forward reach.
- **Hem graze:** faint 1 px hem-edge lines at the right side in idle and stance frames (the 0.2 px side-panel graze). Cosmetic.
- Not seen in game beyond the harness frames.

## v1 record (2026-11-06, superseded)

- **Clips:** guard, idle, walk and run carry, stances, hits and deaths, plus `attack_cast` (36 ticks) and `attack_cast_legs`. The timeline `impact` marker sits on the server's release tick (10).
- **Cast:** an underhand censer swing, not the mage's overhead gather. From the guard (lantern at the right hip) the body coils back and sinks over the rear foot while the lantern sweeps low and back, wide of the right hip, and the off hand reaches at the foe. The coil keeps winding through the held load (ticks 4.5-8.4). The hips then drive forward over the lead foot, and the lantern swings up and is thrust at chest height on tick 10 (torso lean about 29 degrees on top of the hip pitch), with an overshoot at 11.8 and a supported recovery. The feet step out and come back; the root is on guard by tick 30. Probe: fist error at most 0.064 px, 0 samples with the lantern in the body.
- **Stances:** brace (Hold position) is a deep hunch with the lantern low before the lead knee. Wall (Hold the line) is upright, with the lantern raised high at arm's length as a warding light and the off hand up. Ward (fall back) holds the lantern forward at the chest with the off hand up to fend.
- **Lantern probes:** the hexer focus is checked at its own points (the pole runs along local -z from the grip, the light hangs below it), as well as along the shaft.
- **Deaths:** the shared caster falls. The left fall takes the mage's arm and cowl tuck; the right fall lies lower without it. Rest heights (min-y, px): left torso 1.17, hips 4.13; right torso 0.70, hips 3.66.
- **Gameplay (Kort approved, 2026-11-06, cosmetic only):** the hexer's attack is its blight curse (`Abilities` blight case), which used to send no attack event. A caster whose ability mechanic is blight (`Abilities.castsAhead`, so the hexer only) now announces the cast 10 ticks early from `Tactics` (`Projectiles.drawAhead`) on the beat before a cast is due, and `Abilities` sends the matching `loose` when the curse is released; the client drops the repeat. Damage, the release tick and the cooldowns are unchanged. The hexer is not in `Tactics.announcesAhead`.
- **Checks:** `GeoKnightChecks` has a `BLIGHT` flag (ranged, magic caster, blight). It runs the ranged duel with frames over the 36-tick cast clip and releases through `Tactics.shoot` with the ability cooldown cleared.

### Review

- **Round 1 (Sonnet high, reel `hexer/reel`, video `hexer_v1.mp4`): SHIP WITH MINORS.** No blockers. Minors: the lantern vanished behind the torso in side view at the drive, the load parked then snapped, the release lean (about 15-20 degrees) was below the mage bar, brace and wall were hard to tell apart, the ward lantern may have been hidden, and the left fall lay about 0.2 block off the floor.
- **Round 2 (Sonnet high, reel `hexer/reel2`, video `hexer_v2.mp4`): SHIP WITH MINORS.** Fixed: the swing comes through wide of the hip and stays visible; brace, wall and ready are three distinct silhouettes; the ward lantern reads at the chest; the side falls lie on the floor. Improved: the load keeps winding, and the release lean is now about 25-30 degrees.
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false. 35 of 35 cast frames caught in side and three-quarter views (17 of 17 far). All 9 standing and 4 walking casts matched the server release. Weight at the release is 1.0, and 0 between casts. Stances, hits and deaths pass in 3 views.
- **Verdict: finished.** Video: `hexer_v2.mp4`.

### Open minors (polish, not blocking)

- **Cast load:** a short near-hold remains around clip ticks 6-8, before the drive.
- **Cast release:** the lean holds only 2-3 ticks. It is still a little short of the mage's drive. A deeper hip pitch made the lead foot unreachable, so the extra lean is in the torso.
- **Cast, front view:** the swing reads mostly in the side and three-quarter views. The lantern stays close behind the body from the front.
- **Brace:** very little breathing motion in the hold.
- **Left fall:** the rest pose is a tight heap, and the lantern may overlap the torso at the end.
- **Right fall:** the lantern swings out to the side during the tumble (plausible, not broken).
- **Shared with the mage, not reopened:** the heavy front hit folds about 80 degrees with a rebound (knight hit keys); the deaths have a long static end with no separate lantern settle; there is a walk and run seam pop; the run has little forward lean; the ward walk's back foot flicks.
- The early announce needs the blight ability to be off cooldown on the beat before the cast. The harness checks the announce and release timing through `Tactics.shoot`, not the live blight targeting itself.

## Caster fix round 1 (2026-11-10, branch anim/fix-caster)

- **F1 stances:** ready, ward, shield_up and hold now read apart at game16 three-quarter (`work/caster_sheet/casters_stances_game16_3q.png`): ready has the lantern at the belly, ward lifts it before the face at shoulder height, shield_up holds it out beside the hood, and hold carries it low near the lead knee (`HOLD_TROOP` row in `stances.py`).
- **F2 deaths:** the rebuilt falls are deployed, with no old clip left, so nothing snaps on tick 0. The focus is capped per fall (`FOCUS_WORLD_TURN`, `WRIST_TURN`). ang_speed_weapon readings: back 35.1, forward 62.7, left 67.0, right 33.4 (limit 83.2).
- **F3 gait loops:** walk and run are sampled 0.28 of a cycle later (`GAIT_PHASE`), and charge_run_upper LP is moved half a tick earlier (`charge.py LP_SHIFT`). Clip lengths are unchanged. The scorecard has 35 clips, 0 failing.
  - Why the right foot: the old wrap fell on a stride extreme of the 4-tick run_legs (the base clip), so the 1-tick speed reversed there.
- **Open minors:** at game16, rally, brace, wall and rest are still close to ready (not in this round's scope). The run_legs left_leg wrap sits at 1.355 against a limit of 1.362, so the margin is thin.
