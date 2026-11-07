# Caster style v2 design (Kort approved, 2026-11-06)

**Why:** the casters were still animated like sword-and-shield troops. They used the off hand in the attack, crouched shield-style to brace, and their legs, knees and feet clipped through their robes. Casters now cast through the prop they hold, and their robes hang as cloth.

## Attack styles by prop and spell

| Caster | Prop | Spell | Attack |
|---|---|---|---|
| wm_seer | short bone wand (6 px), right hand | bolt at an enemy | Point and shoot |
| tc_mist | short driftwood rod (9 px) | mist burst at an enemy | Point and shoot |
| mage | big sun staff (35 px) | heal shot at an ally (mend) | Staff heal |
| hexer | lantern | blight burst at an enemy | Lantern thrust |

The Seer's totem staff stays strapped to its back as costume. Future big-staff bolt casters use the two-handed staff shot.

- **Point and shoot (short wand, one hand).**
  - Ticks 0-4: turn side-on; the wand hand comes up past the chest.
  - Ticks 4-9: the arm extends straight at the target with the elbow locked; the wand holds steady and charges.
  - Release tick: the wrist flicks; the bolt leaves the tip, with a small recoil back through the shoulder; then back to guard.
  - The off hand stays relaxed at the hip or belt and never joins the cast.
- **Two-handed staff shot (big staff, bolt spells).** Plant the feet and bring the staff level at waist or chest height, back hand at the butt and front hand mid-shaft, like aiming a rifle. A short charge hold, a firm push forward, the bolt fires from the head, and the staff kicks back.
- **Staff heal (big staff, heal).** Both hands raise the staff overhead and the head glows. The staff tips toward the ally and the heal leaves the head.
- **Lantern thrust.** The lantern is held out at arm's length toward the target. The light flares and the blight leaves the lantern. It is the point and shoot made heavy and held.
- **Raise and slam** is reserved for real area spells; no current caster uses it.

## Stances

- **Brace (Hold position):** arms up. The wand or staff is held overhead crosswise as a guard. The body stays upright, with no shield-style crouch.
- **Wall (Hold the line)** and **ward:** caster-shaped, no shield language. Each must read as distinct from guard and from the others.

## Robe (robed casters)

- One hanging skirt: front, back and side panels hung from the hips. Each panel swings with the leg behind it, and the hem stays above the floor.
- The legs move inside the skirt and only the feet show below the hem. No knee, shin or thigh pokes through in any clip, deaths included.
- Locomotion uses shorter, gliding steps.
- Deaths fold the robe flat with the body.
- The current robe art is reused. Only the structure changes.

## Projectile origin (cosmetic, Kort approved)

Every caster spell (`Projectiles.fire` for mend and bolts, `Projectiles.bolt` for blight and mist) currently starts at `getEyePos()`. A caster's shot now starts at its prop tip as it stands on the release frame. That is a per-troop offset in the body frame, rotated by body yaw, with the eye position as a fallback. Damage, ticks, targeting and cooldowns are unchanged.

## Rollout

The Seer is the pilot: wand, robe fix, tip origin. Kort gets a video. Then tc_mist, mage and hexer are redone the same way.
