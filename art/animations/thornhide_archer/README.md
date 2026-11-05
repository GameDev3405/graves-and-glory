# Thornhide archer: the Antlerbow

The Thornhide hunt-leader (`art/armor/thornhide`) with the Antlerbow (`art/player_weapons/thornhide`), an arrow, and a
new hip quiver, on the articulated rig used by the Oathbound combat pack (`../oathbound_combat/`). Status: **for Kort's
look; not wired into the game.** It needs the same articulated troop model as the combat pack.

Watch `archer_reel.mp4` (22.6 s, 60 fps, 1280x720): the idle, the backpedal, the shot twice at full speed and once at
0.35x, then the hit reaction at full speed and at half.

## The clips

| clip | length | what happens |
|---|---|---|
| `archer_idle` | 3.0 s, loop | Ready: the bow carried at the side by its grip, limbs fore and aft, the draw hand resting by the quiver. Two breaths, the weight rocking onto the lead foot and back, the bow swaying a little, a glance to each flank. The cloak, the feather fan and the loincloth sway on springs. |
| `archer_kite` | 1.2 s, loop | Backing away at 0.28 block/s: the back foot reaches back first and lands on its toe, the lead foot drags after it, the root dips at each landing and sways over the planted foot. The bow stays carried, the eyes stay on the threat. |
| `archer_shoot` | 2.35 s, once | Reach to the hip quiver (0.12 s), grab an arrow (0.22 s) and draw it clear along the quiver (0.38 s), swing the bow up onto the line with the arm still bent (0.44 to 0.60 s), lay the arrow on the string at the cheek (0.70 s), push the bow out while drawing (push-pull, 0.82 s) to full draw (1.00 s), settle and aim. At 1.35 s the loose: the string snaps past brace and shivers, the arrow flies, and the draw hand follows through past the jaw. Hold the finish, roll the bow flat while it is still high, drop it out to the side and lower it to the carry. |
| `archer_hit` | 0.9 s, once | Struck: the head snaps back, both arms fly wide, a stumbling step back, a crouch to catch it, then back up to ready. |

## How it is built

The rig, solvers and checks are the combat pack's (see `docs/ANIMATING-TROOPS.md`). What is new for a bow:

- **Placed hands.** The bow's grip is placed (`bow_at`, turned by `aim`: yaw, elevation and cant), and so is the draw
  hand: either on the string's nock (`on_string` 1) or wherever the clip sends it (`hand_at`: the quiver, the
  follow-through). Both arms reach their hands with position IK (`combat_rig.reach_arm`), and both wrists are solved
  so the bow and the arrow face where they should.
- **A natural grip.** The bow's riser crosses the fist with the forearm pointing down the shot, and the arrow leaves the
  draw fingers along the forearm. The IK is guided to keep both wrists nearly straight.
- **The string follows the hand.** The bow is drawn with its own generator at any string position. The draw for the
  game maps onto the bow's four real frames (`rest`, `pull_0` to `pull_2`), as a vanilla bow swaps its model. The
  per-tick draw and its frame are in `archer_clips.json`. After the loose a damped vibration snaps the string past
  brace and settles it.
- **The arrow** is a prop in the draw hand, hidden from the loose until a new one is grabbed. The loosed arrow flies at
  160 px/s. In the game that flight is the projectile entity.
- **A hip quiver**, new: a laced hide tube with a fur mouth and a bone foot, four fletched arrows showing. It hangs on
  the right hip tilted back and out, so an arrow drawn out along it rises clear of the head.
- **Springs** for the cloak (from the chest), the feather fan (from the head) and the loincloth (from the hips, kept
  in front of the thighs).

## The hard part, and what it cost

At Minecraft proportions there is little room for an archer:

- **Head and shoulders:** a 10 px head and a fur ruff 12 px wide that stands 4.5 px out from the chest.
- **Reach:** arms reach about 8 px from the shoulder.
- **Draw:** the bow's full draw is 6 px.

A real anchor at the jaw is impossible: the draw hand cannot reach it. A nocked arrow held low across the belly puts
the draw forearm through the chest. The first two designs did exactly that, by up to 300 px³. What works, found by
search over thousands of poses:

- **Carried bow.** Ready carries the bow at the side, so neither arm crosses the body.
- **Side-on full draw.** The bow arm is out toward the target, the string at the right cheek, and the draw elbow out
  to the side.
- **Nocking on the line.** The arrow is nocked on the shooting line with the bow arm still bent, then pushed out while
  drawing: push-pull, as archers do. Holding the bow in front of the belly to nock is what doesn't fit.

Beats that still clipped were fixed by `tools/unit_anim/pose_search.py`. It searches each failing beat for the nearest
clear pose, judged either as a still or over the motion between its neighbours. The fixes are stored as data in
`tools/unit_anim/archer_poses.json`.

Measured on every 60 fps frame (`python tools/animate_archer.py --check`):

| clip | worst turn in a frame | items in the body | forearm in hard armour | hands off target | feet |
|---|---|---|---|---|---|
| idle | 3.3 degrees | 1.4 px³ (the quiver's foot on the boot cuff) | 0.8 px³ | 0.29 px | still |
| kite | 6.7 degrees | 0.5 px³ | 1.9 px³ | 0.26 px | slide 3.8 px/s |
| shoot | 34.7 degrees | arrow 1.6, **bow 7.0** px³ | 11.8 px³ | 0.26 px | still |
| hit | 30 degrees | 1.9 px³ | 0.8 px³ | 0.07 px | slide 1.0 px/s |

What remains, honestly:

- **The bow coming up.** As the bow swings up onto the line (about 0.45 to 0.5 s of the shot), its lower antler tip
  brushes the lead shin for a few frames, up to 1.8 px deep.
- **The draw forearm in the jerkin.** While nocking and drawing, the draw forearm sits up to 1.5 px into the edge of
  the jerkin. It also presses into the fur ruff at full draw: the check treats the ruff as soft, as fur gives.
- **Allowed contacts.** These are counted as how a bow is shot, not as clipping:
  - the bowstring touching the cheek at the anchor
  - the string through the drawing fingers
  - the string on the bracer
  - the riser through the closed fist
  - the nocked arrow resting on the bow hand
  - the hand among the quiver's fletchings

The tests (`test-tools/standalone/test_archer_anim.py`) hold all of this. Feet within 0.15 px, no bone over 50
degrees a frame, loops closed, both one-shot clips ending on ready, hands within 0.35 px, items within 7.5 px³ and
forearms within 12.5 px³ on every 60 fps frame, the string drawing to full and snapping past brace, the arrow in
flight, and the exports matching.

## Files

| file | what |
|---|---|
| `archer_reel.mp4` | every clip, two views (three-quarter from the bow side, profile from the draw side), the shot at 0.35x, floor studs moving on the backpedal |
| `archer_reel.gif` | the full-speed parts, 30 fps, 640x360 |
| `idle_beats.png`, `kite_beats.png`, `shoot_beats.png`, `hit_beats.png` | each clip's beats from both views |
| `archer_clips.java` | the clips in the TroopClips builder (they need the articulated model's bones; the bow swaps frames by the draw in the JSON) |
| `archer_clips.json` | the keys, the rig, the foot placements, the beats, the string draw per tick with the bow frame to show, when the arrow is in the hand, the loose |
| `archer_thornhide.bbmodel` | the archer cut at the joints, the bow (rest frame), the arrow and the quiver, with every clip as an animation |

Build with `python tools/animate_archer.py`. Use `--check` for the numbers, `--sheet shoot` and `--strip shoot 1.3 1.5`
to review, and `python tools/unit_anim/pose_search.py archer` to re-run the beat search.

## For Kort

- **Model and wiring:** as for the combat pack. The articulated troop model, and which clips become the ranged attack,
  idle, retreat and hit tells.
- **The bow in game.** The bow needs its pull frames driven by `string_draw` in `archer_clips.json`, as vanilla does
  with its pulling predicate.
- **The arrow and the quiver** are new animation props, not items. Should the quiver become part of the Thornhide
  armour?
- **Kite pace.** The backpedal assumes 0.28 block/s.
- **Not checked in Blockbench:** like the others, the `.bbmodel` has not been opened in Blockbench yet.
