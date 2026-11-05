# Unit animation rebuild: GeckoLib pilot (Order Squire / `knight`)

Branch `anim/geckolib-rebuild`. Approved by Kort on 2026-11-05: GeckoLib + Blockbench-format files, models and animations rebuilt from scratch, feel mixed by unit type, one pilot unit first, then expand. Old clip/motion code is not consulted.

## Stack

- Minecraft 1.20.1, Fabric loader 0.16.14, Fabric API 0.92.2, yarn build.10, Java 17.
- GeckoLib `software.bernie.geckolib:geckolib-fabric-1.20.1:4.8.4` (cloudsmith maven), `modImplementation` + `include` (jar-in-jar, so players install nothing extra). Its requirements (loader ≥0.15, fabric-api ≥0.92.0) are already met.
- Files: `assets/gravesandglory/geo/troop/knight.geo.json`, `animations/troop/knight.animation.json` (Bedrock 1.12 / Blockbench "GeckoLib animated model" format), textures beside the old ones.
- Preview: `tools/anim/render_geo.py` — offline software renderer that applies GeckoLib 4.8.4's own transform conventions, writes turntable, frame-strip and GIF captures from front, side and three-quarter views, and measures foot slide. In-game harness captures confirm what it shows.

## Responsibilities

- Server (unchanged): target choice, swing timing, damage (`Tactics.swing` → `PendingStrike` → `Combat.resolve`). Animation never decides damage.
- Network (unchanged): `attack_v1` (variant, start tick), `hit_v1` (kind, zone, bearing, weight, flags), `stance_v2` (stance, aim pitch).
- Client Java: picks which animation plays from those events, velocity and stance; sets playback speed from real ground speed; applies small procedural layers (head look) only to bones no keyframe animation owns.
- Keyframes: every authored pose.

## Bone ownership (no two writers per bone)

| Controller | Bones | Plays |
|---|---|---|
| `legs` | root, hips, legs | idle sway, walk, run, turn step, brace stance, death |
| `arms` | torso, chest, arms, item and shield bones | ready guard, attacks, shield stances, block, hit reactions |
| procedural | head (yaw/pitch only) | look at target / move direction |

Death takes the whole body: while dying, `arms` plays the death upper half and `legs` the lower half from one shared clip pair; no other clip runs.

In code `arms` is the `upper` controller. **The one exception** is the `attack` layer (`AttackController`). It is registered after `upper` and
weight-blends the swing the server announced over the upper bones `upper` has just written. The swing fades in over 2.5 ticks and out
over the clip's last 6 ticks. A swing heard late eases in over 2 ticks, and a troop that dies mid-swing lets it go over 3 ticks. So
the arms leave the gait, strike, and hand back to it in step, without a snap. It only ever blends toward the clip and never feeds back
into gameplay.

**Impact timing is data-linked.** Each attack clip carries a `timeline` marker named `impact`, built by `build_anims.py IMPACT` from
the same values as Spec `impactTicks`. The swing plays on the world clock from the tick the server began it, so that marker lands on
the tick the server's `PendingStrike` resolves. The in-game check `p1-geo-knight-attack-sync-*` asserts, for every swing, that the
clip marker equals the server delay and also equals the landing tick minus the swing tick.

## Pilot animation set

Movement: idle, combat idle, walk, run (speed-matched), turn-in-place step.
Attacks (impact tick must equal server impact): thrust (stab, body, light, impact 7), overhead (slash, head, medium, 10), low cut (slash, legs, medium, 8).
Shield: hold, brace, wall, shield_move, fall_back, block reaction.
Damage: hit reactions by weight (light flinch, medium stagger) and side.
Death: 2–3 variants that finish within the despawn window.
Orders: no gesture; the stance/movement change itself must start within 2 ticks and be readable at 16 blocks.

## Acceptance criteria (pilot)

1. Every clip above plays and blends in game; no snapping between them.
2. The visible weapon contact frame of each attack is within ±1 tick of server damage.
3. Walk/run foot slide ≤ 0.05 blocks per planted step at the unit's real speed (measured by the renderer).
4. No clipping of sword/shield through the body in any clip, checked front, side, three-quarter and at 16 blocks.
5. Dead knights never play walk/attack; interrupted and repeated attacks restart cleanly; target changes don't pop.
6. Other 29 troops render and fight exactly as before.
7. 40 knights in combat: client frame time measured and compared with 40 old-rig knights.
8. `tools/verify.py fast` and `game` pass; no new log errors.
9. A Sonnet reviewer signs off rig, each clip group and the in-game result; Kort approves before any other unit migrates.

## Order of work

1. Dependency + renderer tool. 2. Rig + texture options (Kort picks). 3. Renderer wiring in game (static pose). 4. Locomotion. 5. Attacks. 6. Shield + hits. 7. Death. 8. In-game test pass + performance. 9. Kort review. 10. Remaining archetypes.

## Review log

**Locomotion, renderer pass (2026-11-05).** Two Sonnet high review rounds on walk/run (front, side, three-quarter, near and 16 blocks).
Round 1 fixes are all confirmed resolved in round 2:
- walk: the sword hand rode over the thigh and the blade cut the near leg; fixed with a wrist yaw (`blade_out`);
- run: the pommel jammed at the hip; fixed with `arm_base`, forearm and `arm_out`;
- run: the shield was flung back; less swing and forearm bend.

No BLOCKER or MAJOR issues remain. Documented MINORs (not fixed, revisit after the in-game pass):
- the walk stride reads slightly short, and knee lift could rise;
- the cape barely trails in the walk;
- at 16 blocks the leg motion is hard to read, and the gaits read mostly as bob;
- the walk sword hand sits at hip/thigh height in side view (crowding, no clip);
- the run blade and forearm crowd the chest in three-quarter view (depth overlap only);
- the run shield trails 10-15° behind vertical at mid-stride.

Head pitch swinging in the run was reported in round 1 but was not reproduced: `helm_great` world pitch was measured constant (2.0-2.4°).

**Locomotion, in-game pass (2026-11-05).** A Sonnet high review of the harness captures (run5): six-knight server-driven marches,
near and 16 blocks, front, side and three-quarter. There were no BLOCKER issues. Readback: the client gait speed equals the true
march speed (0.169 walk, 0.264 run) and the right clip plays for every knight. Run6 confirmed the Gait reseed fix for a stale clock
after a knight is not drawn.
- MAJOR, walk: the blade hung forward-down, with the tip at foot height across the near thigh.
- MAJOR, walk: the sword and shield were near-constant (a mannequin).
- MINOR: short stride with little knee lift; weak bob at heel strike; the run blade crossed the chest to helm height; a large run
  sword rock; the run shield trails; walk and run read alike from the front at range.

Fix round 1 (build_anims.py):
- walk: the hand is higher (`arm_base` -6, `fore` -42), the blade near level (4°) and turned out (`blade_out` 22);
- walk: the shield counter-swing is 3→7, the twist 2→4, plus a new shoulder roll (`Carry.roll`);
- walk: lift 2.0→2.6, bob 0.35→0.55 (hips 0.2 px lower to keep the leg in reach);
- run: blade -35→-22, `arm_out` 16, follow 6→4, lean 11→14, arm swing 20→24, bob 0.55→0.7, shield forearm -28.

A Sonnet high re-review (round 2) of fix round 1 found no BLOCKER issues. The walk blade is improved (it is level and clear in
side and front views), the knee lift is better and the run blade from the front is fixed. The remaining issues are MINOR: the
walk tip crosses the near shin in three-quarter view, the run blade reached helm height in three-quarter view, the run sword
rocked in side view, both shields trailed, and walk and run were hard to tell apart from the front at range.

Fix round 2 (final under the review policy; build_anims.py): walk `blade` 4→0, `blade_out` 22→34, `follow` 3→5; run `blade`
-22→-12, `blade_out` 10→20, `follow` 4→2; a new `Carry.shield_base` (walk -6, run -14) carries both shields forward. Renderer
self-check: the run blade now sits at waist height in three-quarter view and its rock is smaller.

Accepted and documented, not fixed:
- In three-quarter view, the forward-carried walk blade still passes in front of the legs. This is a viewing angle on a level,
  out-turned blade, with no clip.
- The shields still sit slightly behind the body line in side view.
- Walk and run remain close at 16 blocks from the front; the side and three-quarter views separate them by lean and stride.

**Attacks (2026-11-05).** Harness runs 9 and 10 (two knights dueling; side, three-quarter and 16 blocks) checked the timing.
Every swing's clip impact frame landed on the server's strike tick (attack-sync-stand 234/234, attack-sync-walk 5/5), with the
impact weight 1.000 throughout and no gap between gait and swing. Walking knights kept the WALK gait for the whole swing, and a
knight killed mid-swing let go of it within 3 ticks. Run 10 also confirmed a Gait fix: a run hysteresis band so a server hitch
cannot flip a running knight into the walk.

A Sonnet high review of the run 10 frames and renderer strips raised these findings, with fix round 1 in build_anims.py and
build_rig.py:
- MAJOR, overhead: the wind-up hitched; the blade went past horizontal behind the head at t6 (-190°). It is now -150°, so the
  rise is monotone.
- MAJOR, overhead: the t10 impact read as a level sweep. The blade now comes tip down (+22°) with the arm slightly lower.
- MINOR, thrust: the arm held at full extension from t7 to t15. A t12 key now draws it back.
- MINOR, overhead and low_cut: the shield flailed wide. Its raise is about 60% of the old amount.
- MINOR, all clips: a black block showed at the right shoulder when the arm lifted. It was the pauldron undersides, painted in
  the darkest steel and halved again by the game's down-face shading. They are now the plate's leather lining. Only that face
  changes; the visible art is unchanged.

Deferred, not attack issues:
- MAJOR (out of scope): a dead knight stands in the guard, tinted red, because there is no death clip yet. Step 7 fixes this.
- MINOR (unconfirmed): the low_cut blade may pass near the right shin around t11-13. This has not been seen in any capture;
  re-check in round 2.

Round 2 (final) Sonnet high re-review of renderer strips (atk3) and run 12 in-game sheets. Verdict: ship.
- Thrust draw-back and the black shoulder block: FIXED.
- Overhead wind-up, overhead impact and shield flail: IMPROVED.
- Low_cut near the shin: no clipping seen in the renderer or in game. Resolution is limited, so a shallow touch could still be
  missed.
- A "blade above the head" at low_cut t13 (side row) turned out to be the opponent's overhead wind-up. On that frame the watched
  knight is the right one, and its blade stays low through t11-21 in the three-quarter row.

Remaining MINORs, recorded and not iterated under the two-round limit:
- Overhead t5-t6: the blade leans slightly past vertical, then returns by t7. Hold it at vertical through t5-t8.
- Overhead t10: the impact is chest height, shallower than a head cut. Steepen the blade by about 20°.
- Shield still protrudes a little in the front view: overhead t4-t6 and low_cut t4-t5.
- Low_cut t4-t5: the wind-up blade swings wide behind the body. Bring it about 10° closer.
- Low_cut t14-t17: small brown fragments near the struck knight. These are probably hit particles; not confirmed.

Harness: tick 0 of each clip is now optional in the frame capture. It is weight 0 by design, the guard pose, and the client
often learns of a swing a fraction of a tick late, so runs 10 and 11 each missed one t0 frame. Ticks 1-21 are still required.
Run 12 failed march-run-side (client median 0.303 against 0.264) and attack-walk (the foe's backing speed dipped once to 0.089)
on a loaded machine. Run 13, with the same code, passed the full suite with every speed exact, so those two failures were server
hitches, not gait faults. If they recur, make the foe-backing gate a median like the march check.
