# Animation pass 1 spec (M3)

2026-10-28. Draft for Kort's real-game look; nothing here is approved as final art. Branch `anim-pass-1` (from `phase-1-solo-core`). Basis: `docs/ANIMATION-PLAN.md`, the animation section of `docs/SLICE-1-CHECKPOINT.md` (on builder-server), and Kort's hybrid decision in `docs/DECISIONS.md`: code clips for everyday states, Blender-keyed clips only later and only after Kort sees these.

## 1. Scope and class set
- Class set (coordinator decision): the six Order models: knight, archer, mage, sentinel, longbow, gl_marshal.
- Gap, out of pass 1: `ic_hearthguard`, `wm_tusk`, `tc_tideguard` have rows in `sheets/design.json` but no model on any branch (`TroopModels.model` is null; the harness logs "no model, weapon or row"). Their tells and the trident attack in `TroopTells` cannot be seen until models exist. Kort decides when.
- Client-only. No protocol, save, sheet or gameplay change; `stance_v1` stays. Server hit timing code is not touched. No Blender loader (decided after Kort sees code clips). Own animations, no Fresh Animations.

## 2. What exists
- `TroopClips` (18 clips per rig, kinds, lengths, `shotTime`, mirroring), `BipedClips` (the 18 biped clips), `TroopTells` (per-troop overrides and idle tells), `HouseAttacks` (House attack variants), `TroopAnimator` (blends still/move by limb distance, overlays, carries, tell).
- Every biped weapon user plays the same `attack`: one arm swing, 0.5 s. No charge run, no formation march.

## 3. Audit (baseline contact sheets, threshold 9, honest scores)
Harness: `GRAVES_PHASE2_CHECKS=turn GRAVES_PHASE2_POSES=idle,walk,run,attack,hit,death`, 45 and 90 degrees. 72 shots, none missing or blank (runs exit 0). Sheets: scratchpad `sheets-before/` and `sheets-before2/` (not committed; regenerate with the same command).

| class | idle | walk | run | attack | hit | death |
|---|---|---|---|---|---|---|
| knight | 7 | 8 | 8 | 6 | 7 | 8 |
| sentinel | 7 | 8 | 8 | 6 | 7 | 8 |
| gl_marshal | 7 | 8 | 8 | 6 | 7 | 8 |
| archer | 7 | 8 | 8 | 8 (bow_draw) | 7 | 8 |
| longbow | 7 | 8 | 8 | 8 (bow_draw) | 7 | 8 |
| mage | 7 | 8 | 8 | 6 (cast) | 7 | 8 |

Weak (below 9): sword attack (stiff straight arm out front, the same swing for sword, axe and spear), mage cast (staff flung sideways), idle (a 1.5 degree breathe, reads static), hit (small flinch). Walk, run and death are 8: acceptable, polish only if time.
Known harness note: the Turn stage logs "pose false (off by 30)" for knight idle-45 (pose check vs shield overlay); shots are still taken. Left as is.

## 4. Pass 1 build list (code clips only)
1. Sword attack: windup overhead, diagonal cut to the contact at 0.25 s, body and head follow through. Keep contact time 0.25 s (`PEAKS`) and length 0.5 s.
2. Mage cast: staff raised in front, lifted at 0.25 s, thrust forward on release; no sideways fling.
3. Weapon variants for other weapons only where a class needs one (axe, spear thrust, trident, mace): none of the six Order models need them; add the clip hooks, build them when a model exists.
4. Idle: slow weight shift and a head glance; stronger hit: a recoil of body, head and weapon arm.
5. Charge run: leaned forward, weapon forward (client plays it for stance "run"). Limit: the server's "run" stance covers both charge and retreat, so retreat shows the charge pose. Telling them apart needs a protocol change: Kort's later decision, not done here.
6. Formation march walk: upright, shorter synchronized stride. No rank awareness.
Left-handed mirroring and `TroopTells` idle tells must keep working.

## 5. Files
- `TroopClips.java` (extra clip names, `KINDS`/`LENGTHS`), `BipedClips.java` (new and improved clips), `TroopTells.java` (lookup order), `TroopAnimator.java` (charge and march selection), `HouseAttacks.java` only if a variant must change.
- `test-tools/java/gg/gravesandglory/Phase2Checks.java` (updated lookup assertions, pose hook), `test-tools/isolated-checks.gradle`, a clip-data check under `test-tools/` (every required clip present per rig, lengths and contact times as above).
- Generated `Spec.java` and `sheets/preflight.json` come from `tools/preflight.py`; never hand-edit.

## 6. Tests and acceptance
- `python tools/verify.py fast --offline` and `game --offline` pass (needs `pwsh` on PATH; no repo change).
- Clip-data check passes; existing Phase2Checks pass or are updated for the new lookups.
- Server timing unchanged: proved by the diff touching only client animation files and tests (no Combat, Abilities, server or protocol file), and attack contact time kept at 0.25 s.
- After-sheets for the six classes plus charge and march, scored against threshold 9; at most two correction rounds, then record what remains.
- Not verifiable here: Kort's real-game look. Do not call pass 1 final before it.

## 7. Open decisions for Kort
- Models and animation for the three House troops (gap above).
- Protocol change to separate charge from retreat (later).
- Whether any signature attack gets a Blender-keyed clip after seeing the code clips.
