# v2 sway polish: variant B (Kort, 2026-11-07)

Kort's feedback on the v2 bipeds: "way too much sway", "heavy hits break their back", and the ready stance "has too much sway, bring it
down to like 25%". After the A/B sway trial he chose **variant B**. This roll-out applies it to every v2 biped.

## Values

| Constant | Value | What it scales |
|---|---|---|
| `V2_SWAY_SCALE` | 0.4 | idle and walk/run sway; the non-ready stances' weight rock (torso/head/pelvis/arm trail, shield lag, pelvis roll, hip shift, shoulder roll). Breathing, gait yaw and arm swing are not sway and are untouched. |
| `V2_HIT_BACK_SCALE` | 0.35 | how far a blow throws the spine, head and pelvis backward (negative pitch on the hit clips). Forward pitch, twist, the stagger step and the knee give are unchanged. |
| `V2_READY_SWAY_SCALE` | 0.25 | everything the ready stance and the combat ready idle do over their loop: breath, weight rock, fore-aft lean, yaw drift, shield lag, head travel, weapon drift. Replaces the 0.4 for the ready stance only. |

All three live in `art/anim/common/feel.py` and every builder imports them from there (knight, spear, axe, greataxe, archer,
crossbow, thrower, caster). Env overrides for trials: `GG_V2_SWAY`, `GG_V2_HIT_BACK`, `GG_V2_READY_SWAY`; setting any scale to 1.0
rebuilds the previous v2 motion. `feel.unscaled()` gives the previous motion inside a `with` block; the death falls use it for their
start pose.

## Front hits

Front medium and heavy hits (heavy = medium x 1.55 at runtime) now ramp in over 2 ticks instead of snapping in one: a key at tick 1
holds 38% of the tick-2 key. Clip lengths, other directions, light hits and the legs clips are unchanged. The torso offset of the
front hit is also capped (`HEAVY_BACK_MAX_DEG` = 17.5 degrees, heavy gain included) so a heavy front hit never bends a torso back past
about 20 degrees in the world (casters, which carry the biggest authored peak, are the only units the cap touches).

World torso pitch on a heavy front hit (worst case over the ready phases, degrees back, before to after):

| Family | Before | After |
|---|---|---|
| knight, sentinel, gl_marshal, ic_hearthguard | 45.8 | 12.6 |
| revenant, tc_raider | 49.8 | 11.3 |
| wm_tusk, tc_tideguard, tc_admiral (spear) | 63.6 | 10.5 |
| ic_breaker (greataxe) | 65.0 | 12.4 |
| archer, longbow, wm_hunter, bonebow, stalker | 39.0 | 9.4 |
| ic_arbalist (crossbow) | 60.6 | 15.0 |
| tc_harpooner (thrower) | 64.6 | 17.6 |
| mage, hexer, tc_mist, wm_seer (casters) | 68.7 | 19.3 |

Heavy still reads heavier than medium on every unit (for example knight 12.6 vs 7.9, mage 19.3 vs 12.7).

Ready-stance yaw drift (torso, peak to peak over the ready loop): knight 14.1 to 3.5 degrees; mage, hexer, tc_mist and wm_seer
20.8 to 5.2 degrees.

## What changed in the exports

Only `idle_*`, `stance_*`, `walk_*`, `run_*` and `hit_*` clips changed. Attack clips, impact keys, clip names and lengths are
identical. Death clips are byte-identical to the committed ones (see below).

Two pre-existing build problems had to be fixed so the rebuild could run at all, neither changes any shipped clip:

- `K.idle_start()` called `idle_clips(1.0)` but every family builder replaces `K.idle_clips` with a zero-argument function, so the
  death step raised `TypeError`. It now calls `idle_clips()` inside `feel.unscaled()`.
- The hexer and bonebow builders failed at import (`fist unreachable`) from the knight low-cut free-fist check; the tolerance is
  now 3.0 for those two troops (those attack keys are unused for them).

The bow family's committed death clips were built before the shieldless idle was widened, so a fresh rebuild of the HEAD builders
reproduces different death start poses (and the caster hit clips were also stale against HEAD). To keep deaths exactly as approved,
the exports keep the committed death clips and take only the idle, stance, walk, run and hit clips from the rebuild.

## Floor contact and slide

Probes on idle, ready, brace, wall, walk, run and light/medium/heavy hits in the front, right and back directions, all 21 units
(315 probes). No probe sinks more than 0.01 px further than before, and the deepest sink of any probe drops from 0.64 px to 0.10 px; the worst
per-tick slide rises by at most 0.06 px. The only regressions: the cumulative grounded slide on the caster
medium hits (front 4.0 to 7.4 px, back 3.0 to 6.3 px, 16 ticks) now matches the knight's 7.0, because the smaller backward lean
keeps the feet on the floor through the stagger step instead of lifting them (knight medium front rises 7.0 to 7.4 px).
