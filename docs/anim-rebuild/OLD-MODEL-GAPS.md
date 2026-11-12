# Old-model gaps (2026-11-12)

Four faults in troops still drawn on the old model (TroopModel, TroopMotion, MotionEngine and the clips under
`clips/biped/`), asked for by Kort on 2026-11-11. Branch `anim/old-model-gaps`, base main f3971eb7. No change to
gameplay, protocol, design.json, Spec.java or any GeckoLib clip or builder.

Evidence comes from the in-game suite `old-gaps` (`test-tools/java/gg/gravesandglory/OldModelGaps.java`,
`GRAVES_PHASE1_CHECKS=old-gaps`). It films one subject at a time on default settings (`geoTroops` true: the knight
on GeckoLib, every other troop on the old model) and saves every client tick of segments 1-4 as
`run/screenshots/p1-og-<slug>-NNN.png`. Run roots are under `work/ingame-check/`:

- `old-gaps-before`: main plus the test files only.
- `old-gaps-after`, `-after2`, `-after3`: with the fixes. after2 and after3 repair only the cast-live segment
  (see gap 3).

Before/after videos (before on the left): `C:/Users/kortt/Melty/status/booth-videos/oldmodel_rise.mp4`,
`oldmodel_cast.mp4` and `oldmodel_sweep.mp4`, built from the before and after frames.

## Which model draws a troop: GeoTroops

The approved and preview sets and the `-Dgravesandglory.geoTroops` flag moved from `GeoTroopRenderer` (client
only) to `GeoTroops`, a plain class with no client imports, so the server can ask too (gaps 2 and 4).
`GeoTroopRenderer.handles` now calls `GeoTroops.drawn`. Its old `APPROVED`, `PREVIEW` and `ALL` lines are deleted,
so a troop lane's one-line edit of the old `APPROVED` line conflicts on merge instead of being lost. After such a
conflict the troop goes in `GeoTroops.APPROVED`. `tools/anim/scorecard.py` (RENDERER), verify's pure-class list,
REVIEW-BOOTH.md, NEW-UNIT.md and the troop-animation skill now point at `GeoTroops.java`.

The server judges by its own JVM's flag. In single player and the harness that is the client's. A dedicated
server on default settings judges by APPROVED alone, which is what a client on default settings draws. A client
that runs `geoTroops=all` or `false` against a dedicated server on default settings gets the default choice
(not covered: no dedicated-server run).

## Gap 1: rising troops idle during their rise

- **Cause.** A rise (Rise Again on any Graveborn troop, the Hollow King's Undying King) stuns the troop for
  its window. The stunned stance chain is stun, then hit, then idle, and no old-model family has a stun or
  hit clip, so the troop stood idle. Only DeathController (GeckoLib) played the fake death and rise.
- **Evidence (before).**
  - Revenant: rise_v1 at tick 780, window 20, no fall overlay on any of the 19 in-window frames.
  - Hollow King: window 70, no overlay on any of the 70 in-window frames.
- **Fix.** `TroopMotion.rise` plays `common/fall.json` forward, holds its last frame, then plays it backwards to
  stand. It blends whole-body over the engine, as a scene clip does, through the shared `over()`.
  - Timing is DeathController's `riseAt`, moved unchanged into `MotionEngine.riseAt`, with DeathController
    delegating. `MotionEngine.riseFall` adds the clip time and the fade weights (2 ticks in, 4 out).
  - A long window (70) falls in 24 ticks, holds, and rises so the troop is up as the window ends. A short one
    (20) quickens both.
  - A scene pose wins over the rise. A dead troop shows no rise.
- **Check.** MotionStanceCheck "rise timing": riseAt at known points for windows 20, 70 and 0. riseFall stays
  inside the clip, never jumps, is at full weight between the fades, lies on the last frame at the turn, and
  ends standing and faded. PASS.
- **In game (after).**
  - Revenant, after3: 15 of 20 in-window frames at full weight. The clip time goes up to 1.19 s at tick 10 and
    back to 0.13 at tick 19, faded to 0.19.
  - Hollow King: 65 of 70 frames at full weight.
  - In the first after-run the 20-tick rise showed no overlay on its first 5 frames: the client heard rise_v1
    late on a loaded machine, and the clip then joined on schedule.
- **Not covered.** A troop killed during its rise on the old model plays its death as the engine gives it, from
  the rise pose; DeathController's collapse rule is GeckoLib-only. Reaper, stalker, bonebow and hexer rises were
  not filmed. They use the same biped fall clip and code path as the revenant.

- **Minors (review 2026-11-12, recorded, not fixed).**
  - A troop killed during its rise pops: `rise()` drops the overlay the moment the troop is dead (TroopMotion.java:284),
    so the body jumps from lying to the engine's pose for a frame, then plays its death. DeathController's `riseDeath`
    handles this on GeckoLib.
  - The stand-up ends on a fade: the last RISE_OUT (4) ticks blend to the engine pose while the reversed clip is
    still a few ticks from its start (about 7 ticks in the quickened 20-tick case). It is continuous, but reads as a
    quick straightening rather than a finished stand.
- **Video.** oldmodel_rise.mp4 shows the revenant (Rise Again, 20 ticks), then the Hollow King (70 ticks).

## Gap 2: caster bolts start in the wrong place

- **Cause.** b6563fd0 (caster v2 tip origin) made `Projectiles.castFrom` start every caster's bolt and mend at
  its cast_tip: the GeckoLib staff tip on the release frame. That applies whatever model draws the troop. On the
  old model there is no prop there.
- **Evidence (before).** The hexer's bolt started 1.554 blocks from its eyes (1.04 up from the feet, 1.12
  forward). The cast-tip log shows the tip used.
- **Fix.** `castFrom` takes the tip only when `GeoTroops.castTip(troop)` holds, which means the troop is drawn
  through GeckoLib. Otherwise it uses the eyes, the origin before b6563fd0. Flight ticks, arc, damage and
  targeting were already worked out from the eyes and are unchanged.
- **Check.** GeoTroopsCheck tests the contract, not today's membership: the sets are disjoint and in the sheet;
  `drawn` follows them under default, `all` and `false`; every troop with a cast_tip resource takes its tip exactly
  when it draws through GeckoLib. A lane moving a troop into APPROVED needs no edit to the check. PASS.
- **In game (after).** p1-old-gaps-bolt-origin passes: the bolt starts 0.000 blocks from the eyes.

## Gap 3: caster cast about 10 ticks out of step (hexer, tc_mist)

- **Finding.** No old-model fault was found. The old cast releases on the tick the bolt leaves, as Kort ruled.
- **Evidence.**
  - `Abilities.attack` (blight or mist) fires the bolt at world tick T and sends the cast event (`loose`)
    with start T - 10, where 10 is caster_cast's impactTicks. Tactics sends the same event a beat ahead
    (`drawAhead`).
  - The old caster_cast clip (1.2 s) has its impact marker at 0.5 s, tick 10, so it releases at T.
  - Before-run, scripted announce and cast: client track caster_cast starts at 948.0 and releases at 958.0.
    The bolt leaves on world tick 958 (release minus bolt = 0.0).
  - The after-runs give the same result (after2: 936.0 against 936).
  - after3, cast-live: the hexer is left to the battle's own Tactics for 400 ticks. Its four bolts left on ticks
    1112, 1212, 1312 and 1452, and each client cast clip released on exactly that tick (4 of 4 matched, worst
    0.0). The first shot came on its first beat with no announce, so the client saw that clip only after its
    release (seen at 1113.06) and the engine joined it mid-clip. The other three were announced a beat ahead
    and seen at the clip's start.
- **Likely source of the 10 ticks: the GeckoLib clip.**
  - AttackController shifts a caster's ability clip by lead = impactTicks + windup - impact(clip). For the
    hexer that is 10 + 20 - 20 = 10.
  - `blight_gravecaller` (30 ticks, impact marker at tick 20) therefore starts on the bolt's tick T and
    releases at T + 20, the burst.
  - So a GeckoLib-drawn hexer starts its cast on the bolt's tick and the old model ends its cast there. Side by
    side they look 10 ticks or more apart.
  - That is GeckoLib behaviour (the release is meant for the burst), so it was left alone and is reported.
- **Check.** MotionStanceCheck "cast release": caster_cast's impact marker equals the sheet's impactTicks, and
  through MotionEngine the release lands within one 60 fps frame of the bolt. Covered for tempo 0.9 to 1.1,
  announced a beat ahead or not. PASS.
- **Commits.** The check is in 40e0d099. That commit also carries the gap 2 change: a failed `git add` on an
  ignored path left gap 2 staged, and the gap 3 commit took it in. The commit text's GeckoLib timing ("T-10 …
  T+10") is wrong; this page has the corrected numbers.

## Gap 4: sweep particles gone

- **Cause.** 6f69045c removed the vanilla SWEEP_ATTACK from `BattleEffects.strike` for every troop, because a
  GeckoLib swing carries the blow (Kort, 2026-11-08). The old clips do not, so old-model melee lost its sweep
  too.
- **Evidence (before).** Revenant swing (impact in 10 ticks): 0 sweep particles in the client's particle
  manager. Knight: 0.
- **Fix.** `strike` spawns the sweep again when `GeoTroops.sweeps(troop)` holds, which means the troop is on the
  old model. Same particle, position and count as before 6f69045c. It adds a harness counter, `sweeps`.
- **Check.** GeoTroopsCheck: no sweep for the knight by default, a sweep for the knight under `false`, a sweep
  for old-model troops by default, and no sweep for a preview troop under `all`. PASS.
- **In game (after).** Revenant: 1 sweep. Knight: 0.

## GeckoLib troops unchanged

- AttackController, DeathController's behaviour (riseAt moved verbatim) and every clip are untouched.
- `GeoTroopRenderer.handles` gives the same answer as before for every troop and setting: GeoTroopsCheck checks
  `drawn` for every APPROVED and PREVIEW troop under default, `all` and `false`.
- The knight shows no sweep in game (sweep-knight). `castTip` holds by default for an approved troop and under
  `all` for the previews, so GeckoLib casters keep the staff tip.
- The rise overlay lives in TroopMotion, which GeckoLib troops do not run.
