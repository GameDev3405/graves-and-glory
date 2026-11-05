# Waist and ankle rig: handoff (2026-11-05)

Branch `art/anim-redo-rig` (from `art/anim-redo` at 74316ed7), checkout `bannerbound-animredo-rig`. Not pushed or merged.

## What changed

Every biped troop (25) gains three bones:

- `chest` under `body`, pivot (0,-6,0), the waist. head, both arms and `arms_folded` now hang from it. The joint limit is pitch
  [-35, 45].
- `right_foot` and `left_foot` under the shins, pivot (0,4,0), the ankles. The joint limit is pitch [-45, 45].

The five non-bipeds (wm_hound, tc_brute, wm_oldhorn, wm_stalker, ic_colossus) are unchanged.

### Art pipeline (art/hand/common/handlib.py)

- `WAIST`, `ANKLE` and `JOINT_LIMITS` add the joints; `rig_bones` and the rig file (`art/rigs/biped.json`, 17 bones) carry them.
- `waist_and_ankles` runs on every biped as it builds. It cuts the torso at the waist and the shin at the ankle (`cut_box`):
  - The rows above the waist go to `chest`, the rows below stay on `body`. A shin's rows below the ankle go to its foot.
  - The lower piece starts JOINT_OVERLAP (2) rows above the cut, with the same texels, as `limb()` does at the elbow and knee.
- Gear on the torso is cut at the waist in the same way. A gear bone that sits wholly above the waist rides `chest` (RIDE_CHEST),
  and one wholly below rides `body` (RIDE_BODY). Boots ride the foot.
- A builder can name riders explicitly with `waist={bone: "up"|"down"}`. gb_hollowking (rib rails) and tc_admiral (anchor) do.
- `merge={bone: into}` folds a static gear bone into another, to stay at or under 64 bones (`merge_bones`).
- The joint faces of a cut are caps (`joint_cap`), which show only once the joint bends.
- Two rest-pose fixes came out of the depth-buffered check (see "Rest pose"):
  - A dilated box's cut pieces (a garment worn over the torso or shin) get no caps. Their caps only filled the dilation ring,
    which showed at rest through an open hem or neck or a see-through texel.
  - The glow layer skips a lower piece's overlap rows (`Box.dup`, `paint_box(dup=False)` in `build_glow`). The eyes pass is
    additive with no depth write, so a texel lit on both pieces would have glowed twice at rest. Only gb_hollowking had glow
    there.
- `tools/build_skins.py` changes BIPED_BONES, LEGS and JOINTS. It regenerated 25 troop models, `rig/biped.json` and the textures.
- Sheet growth: ic_mortar's SKIN grew to 128x144 and mage's to 128x96, to fit the cut pieces.

### Clips

- The new bones are legal clip channels.
- `art/clips/build_clips.py` has the ARMOUR pivot.
- The 13 `art/clips/biped/*.bbmodel` skeletons and the 4 legacy `art/troops/*.bbmodel` files (archer, gl_marshal, longbow, mage)
  now carry the new bones.
- The arm pivot y in `art/troops/knight.json` and `sentinel.json` is rebased from -10.6 to -4.6, relative to chest.
- `tools/clips.py --verify` reports 91 clips unchanged.
- `test_attack_clips.py` has the chest and foot LIMITS plus `test_waist_and_ankles_stay_in_limits`.

### Runtime (src/main/java)

- `Pose.find` reads a row without making one.
- `MotionStack`: CROUCH_BONES and keysLegs include the feet.
- `MotionBody`: the cape and tabard lag includes the chest.
- `MotionFeet`:
  - HULL is foot-aware. Corners before FOOT_TO (24), the boot and lower shin, turn about the ankle by the foot's channels
    (`corner`, `footOf`). At rest this is identical to before.
  - `torsoClear` walks body, then chest (WAIST 6), then head (NECK 6). The head offset changed from -12 to -6-6, which is an
    ulp-level difference.
- TroopModel's hand world position (`handPath`) and the cull reach follow the hierarchy as they are, through the parent chain.

### Harness (test-tools/java/.../Phase2Checks.java)

`GRAVES_PHASE2_TURN_BEND=1` makes the Turn suite shoot only each troop at rest and bent, at TURN_ANGLES. It uses two
harness-only clips laid into the family map (`rigClips`), not art:

- `rig_rest` keys nothing.
- `rig_bend` sets chest pitch 25 and yaw 30, right_foot -30, left_foot +30, and both shins 60.

`facts()` lays the clips in too. In the first runs it did not, because the facts are logged before the first shot, so the bent
`[p2-turn-fact]` lines showed the rest pose. The shots were never affected, since `shoot()` lays the clips in before each one.

## Bone counts (before -> after)

| Troop | Before | After |
|---|---|---|
| archer | 39 | 45 |
| bonebow | 43 | 45 |
| gb_hollowking | 61 | 62 |
| gl_marshal | 38 | 42 |
| hexer | 41 | 45 |
| ic_arbalist | 38 | 42 |
| ic_ballista | 63 | 63 |
| ic_breaker | 45 | 49 |
| ic_hearthguard | 46 | 49 |
| ic_mortar | 54 | 58 |
| knight | 53 | 59 |
| longbow | 39 | 45 |
| mage | 34 | 38 |
| reaper | 40 | 42 |
| revenant | 62 | 62 |
| sentinel | 51 | 56 |
| stalker | 41 | 43 |
| tc_admiral | 64 | 58 |
| tc_harpooner | 48 | 49 |
| tc_mist | 40 | 43 |
| tc_raider | 43 | 44 |
| tc_tideguard | 46 | 49 |
| wm_hunter | 45 | 49 |
| wm_seer | 51 | 54 |
| wm_tusk | 49 | 52 |

Merges made to stay at or under the 64-bone cap:

- **tc_admiral:** hat_pearl1-7 merge into hat_pearl0.
- **revenant:** right_boot and left_boot merge into the feet, brow_plate into helm_plates, and cape_clasp into gilt_collar.
- **ic_ballista:** right_boot and left_boot merge into the feet, bevor into cap_band, and coif into cap.

## Rest pose

The rest pose is identical at every tier, but the evidence changed during the work. An earlier claim of "pixel-identical" from
render.py sheets was wrong. My diff script used `getbbox()` on RGBA images, which tests only alpha. Comparing every pixel showed
4037 changed px.

render.py is a painter's-algorithm preview: one outlined quad per texel, sorted by mean depth. Its shared-edge pixels depend on
draw order, so splitting a box, or adding the 2-row overlap duplicates, moves 1-px seams with no change in geometry. It is not
how the game draws.

The rest pose was therefore checked two ways:

1. **Quad sets.** For each sheet I took every drawn texel quad in world space (corners, normal and colour). The sheets cover
   every biped at tiers 1-3, the veteran skin and the all-bones sheet, 150 sheets in all. Every old quad survives unchanged:
   lost 0 on all 150. The gained quads are the internal joint caps and the overlap rows.
2. **A depth-buffered, back-face-culled render** of those quads, matching the game's `getEntityCutout`, in render_all's 6 views.
   The first pass found 123 px of cap rings in 9 troops; the cap fix removed them. Now 0 changed pixels and 0 new coplanar
   colour ties.

For the glow layer, no lit glow quad is gained or lost on any glowing biped after the dup fix.

The image is `docs/review/anim-redo/rig-waist-ankle-rest-diff.png`: before | after | diff for each of the 150 sheets, with 0
changed pixels. It is palette-quantised to 909 KB; the counts come from the exact renders.

## Checks

All on the working tree at HEAD 74316ed7 plus these changes, the same tree as the commit.

- `python tools/clips.py --verify`: 91 clips verified, unchanged.
- `python tools/build_skins.py --verify`: 568 outputs verified.
- `python -m unittest` in `test-tools/standalone`: 513 tests, OK (4 skipped).
- `python tools/verify.py fast`: PASS, every step (`work/verify/20261105-022938-47412`).
- `python tools/verify.py game`: PASS, every step, including the fast tier (`work/verify/20261105-030106-46288`). That run is on
  the final tree, after the one-line facts fix in Phase2Checks. The earlier run, before the fix, also passed
  (`work/verify/20261105-023948-47964`). The game step is the Phase 1 combat smoke.
- The in-game Turn runs (`work/ingame-check/rigA`, `rigB`, `rigC`, and `rigF` after the facts fix): mod-loads, arena-ready,
  p2-turn-shots and p2-turn-suite pass in each.
- The rest-pose evidence is under "Rest pose" above.

## In-game contact sheet

`docs/review/anim-redo/rig-waist-ankle-contact.png`: 1018x8284, a 256-colour palette, 1.8 MB.

- **Layout:** one row per biped (25). The columns are rest at 0, 45 and 90 degrees, then bent at 0, 45 and 90.
- **Shots:** stage 3, gold, face 0, daylight. Each cell is cropped from a 1280x720 Turn shot (`GRAVES_PHASE2_TURN_BEND=1`).
- **Bent pose:** the harness clip `rig_bend` above.

What it shows:

- **Waist:** every biped's chest, head, arms, shoulder gear and held weapon turn and lean together about the waist. The hips,
  belt, skirt and legs stay put. No gear is left behind at the waist or comes loose.
- **Knees and ankles:** they bend, and the cut boots follow the foot. Joint faces show as caps only on the bent joints.
- **Robes:** a long robe that rides `body` hides the knee bend: hexer, tc_mist and the mage's lower robe. That is the robe, not
  a missing joint.
- **Floating:** troops float in the bent pose, because the clip bends the knees and nothing plants the feet (the flat sole was
  skipped). That is expected for a forced test clip, which is not a gait.
- **Hand position (rigF, knight and mage):**
  - Under `rig_bend`, the right hand pivot moves from (R 6.00, U 14.02, F 0.00) to (R 4.35, U 14.39, F -4.46) px from the
    feet.
  - That is its rest offset from the waist, (6, -3.98, 0), turned by pitch 25 and then yaw 30: (5.196 - 0.841,
    18 - 3.607, -(3 + 1.457)).
  - The mage's off hand turns from (-12, 0, 0) to (-10.39, 0, 6.00) relative to the weapon hand.
  - So `handPath`, and the weapon placed by it, follows the chest.

## Judgement calls

- **The flat planted sole was skipped as too risky.** It would:
  - change every standing troop's ground lift, with no review;
  - make MotionEngineCheck's frozen-fixture SOLE probe disagree;
  - risk pushing the crouch sink past -0.5 px.

  A follow-up sketch, in `plant()`: `d = (R_root R_leg R_shin)^-1 (0,1,0)`, `rx = asin(dz)`, `rz = atan2(-dx, dy)`, weighted by
  pw, applied to the foot row.
- **Cut garments have no joint caps.** A dilated cut garment shows no cap when bent, only a hairline ring at most. This keeps
  the rest pose identical.
- **The glow lights overlap rows once,** on the upper piece only. The elbow and knee overlaps from `limb()` still double their
  glow, as they did at HEAD; that is pre-existing and was not touched.
- **The relief 20% rule now applies per torso half.**
- **The template's cap hue wraps onto the chest's.**
- **14-px legs use the same ANKLE (4).**
- **The rig file has no box overlap.**
- **build_clips was already stale at HEAD.** I rebuilt only the skeletons.
- **anim_edit still uses the legacy projects.** tools/anim_edit.py was left alone, because another agent may be editing it.
- **The in-game sheet uses stage 3,** the heaviest gear, with face 0 forced. The brief's bent pose bends "knee 60", which I
  applied to both knees.

## Unresolved

Nothing blocking. Open items:

- **The flat planted sole** was not done; see "Judgement calls".
- **Elbow and knee overlap rows** from `limb()` still glow twice, as at HEAD. This is pre-existing and was not touched.
- **`tools/anim_edit.py`** still opens the legacy projects, which lack the new bones. It was left to the agent working on it.
- **Visual sign-off:** the contact sheet is evidence, not a visual sign-off. Kort's visual review remains a separate gate.
- **Uncommitted preflight output:** `Spec.java` and `sheets/preflight.json` have CRLF-only working-tree changes from
  `tools/preflight.py`. They were left out of the commit.
