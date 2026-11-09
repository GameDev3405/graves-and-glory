# Animation style reference (Kort, 2026-11-05)

Kort's reference reel, "Oathbound knight · combat pack", is the 10/10 bar for
every unit. Copy its style and flow, not its exact moves.

- Video: `C:/Users/kortt/Melty/reference/combat_reel.mp4` (24.5 s, 60 fps; not
  in git). It shows two views: three-quarter from the shield side, and profile
  from the sword side.
- Sheets: `art/anim/reference/` (combo, block and hit at 5 fps from the 0.35x
  slow-motion parts, plus the phase-label strip).

The reel names each phase, which shows how its flow is built:

- **idle:** breath, weight centre, weight forward, weight back
- **advance:** lead foot lifts, heel strike, weight forward, back foot drags,
  settle
- **combo:** load, wind-up, contact, follow-through, gather, rising contact,
  bash load, bash, lead foot back, back foot back, plant, settle
- **block:** skid, guard, absorb, shove, plant, settle
- **hit:** catch, guard, gather, plant

## Rules taken from the reel

1. **The guard is a ready pose, not a rest pose.**
   - The blade is held forward and angled up, crossing in front of the body.
   - The shield is held forward to cover the torso.
   - The knees are bent and the feet staggered, lead foot forward.
   - Weapon and shield never hang.
2. **Idle is alive but stays in guard.** Slow breathing and small weight shifts
   (centre, then forward, then back). The guard silhouette never breaks.
3. **Every strike uses the whole body.**
   - The torso twists and leans into the blow.
   - The hips drop on heavy strikes.
   - The head follows the blade.
   - The arms are the end of the chain, not the whole motion.
4. **Footwork is part of every action.**
   - Each strike plants or steps: in on contact, then back to recover ("lead
     foot back", "back foot back", "plant").
   - At contact the stance is wide: feet well apart, deep knee bend.
5. **Each move has clear phases with contrasting speeds.**
   - Load or wind-up is held long enough to read.
   - Contact is fast.
   - Follow-through overshoots past the target, and the blade ends low and
     across the body after a downward cut.
   - Settle eases back into the exact guard pose.
6. **Combos chain through a "gather"** in place of a full return to guard. The
   end of one hit is the load of the next (cut, rising cut, shield bash).
7. **The shield is a weapon too.** It pulls in to the chest on the load and
   drives out on the bash. During a block it pushes forward, the body skids
   back a little, the knees absorb, then it shoves out.
8. **Hit reactions are big and readable.**
   - The torso throws back and sideways, about 30 degrees, and the sword
     flies wide.
   - Then a quick "catch" to recover and a gather back to guard.
9. **Secondary motion.**
   - The cape and plume lag and swing through each strike.
   - The shield trails the body slightly. It is never glued in place.
10. **The silhouette reads from both views.** Each key pose reads clearly as a
    silhouette in profile and in three-quarter view.

## Mapping to our rig

- Attack clips keep their upper-body channels, so attacks still work while
  walking.
- Each attack also gets a matching `attack_<name>_legs` clip (hips, legs,
  feet) for the standing case: plant, step and hip drop timed to the same
  contact tick. Walking keeps the gait legs.
- Stance "ready" is the guard from rule 1.
- Server damage timing stays the contact tick.
