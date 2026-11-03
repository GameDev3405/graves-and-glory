# Sword-and-shield troops in the video style

Kort, 2026-11-02. Branch `work/anim-rework`. Reference: "Sword and Shield Animations for Unreal Engine" by RamsterZ (youtu.be/UFFB8xD4j8I), saved locally at `C:/Users/kortt/Melty/anim-refs/sword-shield/` (frames in `frames/<move>/`, 10 fps; not committed).

## Why

Kort dislikes how the troops move and that they clip into themselves. The current sword_shield clips stand upright and stiff-legged, hold the shield at the side and swing from the arm alone. The video's fighters crouch low with bent knees and weight forward, keep the shield up in front of the chest even mid-swing, and turn hips and shoulders into each blow, with big wind-ups and follow-throughs. Kort wants that look.

## Goals

- The sword_shield family (knight and every troop that uses it) matches the video's feel. The aim is the same weight and body mechanics, not a frame-for-frame copy.
- No self-clipping: the sword, shield and arms never pass through the troop's own body or head, at any angle.
- Each move is approved by Kort one at a time, judged in game (front, side, three-quarter) on the comparison page next to the video.

## Two stances (Kort's ruling)

- **Formation** (`wall`, `brace`, and their `*_wall` swings): stays tight and upright, so neighbours on the 0.7-block wall cell do not overlap. Only clipping fixes here.
- **Loose melee** (`guard`, and the swings thrown from it): the video's low, wide, shield-forward pose.
- `idle` (no enemy, still) stays relaxed and upright (Kort: the video's ready pose is used in guard only), so the switch into fight mode is visible.

## Moves, in Kort's order

| Order | Ours | Video (m:ss) | Notes |
|---|---|---|---|
| 1 | `guard` stance (+ `idle` checked for clipping only) | 0:06–0:10 | Low, knees bent, shield forward at chest, sword low by the hip angled forward. Every swing starts and ends here. |
| 2+ | `sword_shield_overhead` | 0:22–0:26 | From high behind the head down across the body, hips turn, a step in. |
| | `sword_shield_low_cut` | 0:15.5–0:19 | Side slash, whole-body turn. |
| | `sword_shield_thrust` | 0:44.5–0:49 | Lunge; fixes the known 2–10° outward blade drift. |
| | `hit_body`, `hit_head`, `hit_legs` (+ variants) | 1:23.5–1:28, 2:41–2:46 | Recoil with the shield staying up. |
| | Guarded advance (walk) | 2:08.5–2:18 | Only with an enemy near (Kort): low, shield up, short steps. The march (`carry`, no enemy) keeps today's walk. |
| | Taunt (new clip) | 1:36.5–1:41 (sword twirl flourish) | After a kill, now and then, when no enemy is in reach (Kort). |

Kort picks which move comes after the stance.

## Fixed limits

- Strike timing is approved: each attack's `impact` marker stays on its `impactTicks` (thrust 7, overhead 10, low cut 8) and clips stay within `BattleMath.ATTACK_CLIP_MAX_TICKS`. Any change to gameplay timing needs Kort first.
- `tools/clips.py --verify` must pass; `Spec.java` only through `tools/preflight.py`.
- The guarded advance and the taunt change client-side visuals only. If the taunt needs the server to tell the client about a kill (a new or changed network message), that is a protocol change and needs Kort's approval before it is built.
- Max 2 Minecraft instances (Kort's own client counts). No computer-use on Minecraft; renders come from the harness motion-grade run. No push, publish or merge to main; the coordinator merges after `verify.py game` passes.

## How each move is done

1. Edit the clip keys in `art/clips/biped/sword_shield.bbmodel` (via `tools/anim_edit.py` or a script), then `python tools/clips.py` and `--verify`.
2. Render in game: `bash test-tools/work/t16r12/live/run.sh <tag> solo_knight` (motion-grade, parts solo and loco). Frame the solo camera tighter than now; the troop is small in frame today.
3. Rebuild the comparison page (scratchpad `build_compare.py`, served at localhost:8771 from `anim-refs/sword-shield/compare/`): video on the left, the game on the right, slowed playback, a frame scrubber, front, side and 3/4 views, key poses clickable.
4. Kort reviews; repeat until approved; commit that move.

## Left to the builder

- Exact angles, key timing inside the approved impact tick, and easing.
- Whether the guarded advance is a new clip, a layer on `guard` while moving, or a tweak to the walk layers in `MotionEngine`.
- How a pose change in `guard` carries into the swing clips' first and last frames so blends do not pop.

## Settled after the builder check (Claude's calls, Kort can overrule)

- **Who uses the family:** the knight (Order Squire), the sentinel (Templar) and Lord Marshal Aldric (`gl_marshal`). Every pose is checked on all three before Kort sees it.
- **Clipping is judged by eye** on the in-game renders at front, side and 3/4. Fixes go in the clip keys only. The shield and sword mounts and the part sizes stay as they are, so wall, brace and idle do not change by accident.
- **The crouch is visual only.** The clip may lower the body, but the hitbox, eye height and gameplay stay the same.
- **Switching stances** stays as `MotionEngine.stanceKey` does it today. Loose swings start and end in the new guard pose; `*_wall` swings start and end in today's wall and brace poses.
- **Taunt comes last.** Before building it, check whether the client can already see a kill. If it can't, stop and ask Kort about the network change; don't build a stand-in.

## Not in scope

- Other weapon families (bow, caster, other melee) and deaths; they come after this, one at a time.
- Formation spacing, gameplay timing or balance.
