# Group 1: the Gilded Order

Six troops, each with its own clips, built fresh from the approved list (`KN`, `AR`, `SE`, `LB`, `MA`, `GM`). Nothing is reused from
the earlier family clips except what a troop does not key (formation stances wall and shield, blocks). Status: **for Kort's look;
needs the troop clip system (ANIM-04) to merge.**

Watch the reels: `<troop>_reel.mp4`, two views (three-quarter and side), drawn from the game's own motion engine on the troop's
real model and skin, with floor studs so travel reads. Each runs idle, the idle variants, walk, run, guard, every attack, the
three hits, the specials, then the death.

## What each troop has

| troop | stances | attacks (slot) | idle variants | gait | hits | specials and death |
|---|---|---|---|---|---|---|
| Order Squire `knight` | idle (KN-1 textbook guard), guard, carry (sword shouldered) | KN-7 lunge (thrust), KN-9 shoulder barge (thrust), KN-6 drill cut (overhead), KN-8 cut then low backhand (low cut) | KN-2 grip and gauntlet, KN-3 glance and nod | KN-4 march, KN-5 charge | KN-10 stagger, shake it off | KN-12 `victory`, KN-11 kneel on the planted sword, slump |
| Order Archer `archer` | idle (AR-1 bow low, arrow nocked), carry, guard, aim (side-on) | AR-6 quick shot, AR-7 aimed shot (direct), AR-8 retreating shot (volley); every shot ends with a reload from the quiver | AR-2 tests the string, AR-3 shades the eyes and scans | AR-4 crouched walk, AR-5 light run | AR-10 duck behind the bow | AR-9 `reload`, AR-11 spin and fall, bow flung out |
| Templar `sentinel` | idle (SE-1 point planted, hands on pommel), guard (two-handed plough), carry (sword upright, processional), brace (SE-9 knee down, sword planted) | SE-8 pommel strike then cut (thrust), SE-6 cleave (overhead), SE-7 hip-turning sweep (low cut) | SE-2 prayer, SE-3 shoulder roll | SE-4 heavy walk, SE-5 lumbering charge, sword high | SE-10 a shudder in the plate | SE-11 falls back stiff like a tree, arms wide |
| Order Longbow `longbow` | idle (LB-1 leaning on the grounded bow), carry (bow over the shoulder), guard, aim | LB-7 heavy shot, LB-8 kneeling shot (direct), LB-6 high volley (volley) | LB-2 tests the wind, LB-3 counts arrows | LB-4 long strides, LB-5 awkward jog | LB-10 steps back, guards the bow | LB-9 `reload` from the tall quiver, LB-11 bow arm drops, then he crumples |
| Sunweaver `mage` | idle (MA-1 staff upright, floating sway), guard, carry | MA-6 sunbolt, MA-7 sunburst, MA-8 staff swing (all caster cast) | MA-2 traces a sun sigil, MA-3 twirls the staff | MA-4 glide, MA-5 robe gathered | MA-10 ward | MA-9 `bless`, MA-11 sinks at the hips (her robe holds her knees), settles |
| Lord Marshal `gl_marshal` | idle (GM-1 hands on the pommel, chin up), guard (shield up, sword cocked high), carry (at the slope) | GM-7 riposte (thrust), GM-6 executioner's cut (overhead), GM-8 three-hit flourish into a pose (low cut) | GM-2 points the order, GM-3 rallies | GM-4 stately stride, GM-5 charge, sword raised | GM-10 unimpressed, straightens the cape | GM-9 `war_cry`, GM-11 one knee, raises the sword a last time, falls |

## Rules kept

- **Timing unchanged.** Every attack sits in its family's attackVariants slot and hits on that slot's tick (thrust 7, low cut 8,
  overhead 10, caster 10, bows loose on 16). Bow shots set their own draw and full markers, so each draws differently but
  looses on the same tick. Lengths stay within 40 ticks.
- **The blade points the logical way.** Weapons are aimed by solving the wrist for a direction, within ±40 degrees of each
  weapon's natural hold through every strike. A point-down hold (pommel stances) and a turned-over staff for casts use their own
  holds, outside strikes only.
- **Lint on every clip** (`tools/troop_anim/kit.py`): no backward elbow or knee on the net pose, joint limits, no bone over 60
  degrees in any 100 ms, keys on the 0.05 s tick, loops closed, impacts on the slot tick, no weapon tip under the floor.
- **Feet** stay the engine's: the stride and planting solve the legs; walk and run clips key rhythm (bob, sway, shoulders, arms)
  so they sit on either the carry or the guard.

## How it is built

`python tools/troop_anim/run.py <troop>` writes `art/clips/biped/<troop>.bbmodel` and the troop's `troopAttacks` rows, runs
`tools/clips.py` and `tools/preflight.py`, and lints. `--sheet [clip ...]` draws contact sheets from the engine, `--reel` the
reel. The poses are in `tools/troop_anim/gilded.py`, one function per troop. The `.bbmodel` files open in Blockbench through
`tools/anim_edit.py open <family> --troop <troop>`.

## Not checked here

- **In game.** These are drawn from the game's motion engine compiled offline (`MotionPreview`), not shot in the client. The
  turn suite and the motion grade still need to run on this branch (Windows only here).
- **Not drawn in the preview:** the outer layer, glow, the vanilla shield item and cape physics' cloth (the engine's springs are
  in the pose).
- **Ranged base stance.** Bow and caster shots are keyed as offsets over `aim` (bows) and `guard` (the Sunweaver). If the server
  shoots from another stance the arms land off; the turn suite will show it.
