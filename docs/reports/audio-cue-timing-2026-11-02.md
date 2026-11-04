# Audio cue timing audit, 2026-11-02

Scope: every call site of `Audio.` and `AudioCues.` (server and client), checked against the moment each cue should sound. Method: read each call site, read the animation timing data (`HouseAttacks.STRIKE`, `TroopClips.PEAKS`, the projectile landing queue, the gate and wave timers) and measured each sample's envelope to find where its strike or swell sits inside the file. Branch `audio/cue-timing`. Not observed in a running client (static audit plus unit checks only; see Risks).

Ticks are server ticks (20 a second). "Contact" is the tick the blade meets its target in the attack clip: gilded 4, graveborn 6, wildmoot 5, ironclad 7, tidecallers 6, any other troop 5.

## Findings and fixes

| Cue | Trigger | Intended moment | Actual moment (before) | Fix |
| --- | --- | --- | --- | --- |
| combat.sword_swing | `BattleEffects.strike`, as a melee swing starts (every second slot) | Swing start (the whoosh peaks 0.22 to 0.29 s in, matching contact) | Swing start | None needed |
| combat.sword_clash, armor_hit, heavy_metal_hit, shield_block (melee) | `Combat.resolve`, then `Audio.hit` | Contact tick of the attacker's clip | Tick 0 of the swing, 4 to 7 ticks before the blade lands | `Audio.hit` queues melee sounds by `AudioCues.contactTicks` (`Audio.later`, run by `Audio.tick`) |
| shield_block (blocked, zero damage) | Same | Contact tick | Missed: `Combat.resolve` returned before `Audio.hit` when `damage()` answered false, which a fully blocked blow does | Sounds when hurt or when the shield was raised, before the early return; secondary hits (cleave, splash) still silent |
| Shield bash cue | `Abilities.bash` | Contact tick | Tick 0 | Delayed by contact |
| Slam cue | `Abilities.slam` | One thud at contact | Tick 0, plus the primary blow's thud: two heavy hits | Delayed by contact and sent under the blow's own key, so the cooldown folds it into the primary thud |
| combat.armor_hit sample | Sample | Strike at the start | Ring 0.15 s into the file (3 ticks late) | Not fixed: Kort approved the sound as it is. A trim is ready (`LEAD` in `import_pixabay.py`, empty now) but changed the level by about 2 LU |
| combat.bow_shoot | `Audio.shot` after `Abilities.attack`, the tick the arrow leaves (release) | Release | Release tick, but the string twang is 0.26 s into the sample (creak first), and it also sounded when `attack` refused the shot | Refused-shot sound fixed: `Abilities.attack` returns whether it started and `Tactics` plays only then. Sample left as approved (the 0.26 s lead stays) |
| combat.arrow_hit | `Combat.resolve` on the projectile's landing tick | Landing | Landing tick, but the impact is 0.33 s into a flight whistle | Not fixed: sample left as approved |
| magic.cast | `Audio.shot` at cast start | Cast start | Cast start; also played a second time when the Mist Caller's cloud was placed | Cloud now plays magic.impact at placement (its landing), keeps the single cast cue |
| magic.impact | `Abilities.burst` at windup end; `Combat.resolve` for bolts | Landing | Landing | None needed |
| gate_hit | `GateTarget.structureCue` | Contact tick | Tick 0 | Delayed by `Audio.blowDelay` (set by `Combat.resolve` around the damage call; 0 for arrows and bolts, which land when they sound) |
| gate_shut | `SiegeMode.drop` | The crash (0.35 s into the file) as the portcullis drops | Cue began as the gate dropped, the crash 7 ticks late | Queued in `fall` to start `GATE_SHUT_LEAD_TICKS` (7) before `dropAt`; `drop` is silent |
| gate_breach | `SiegeMode.breach`, on the next whole second after the gate died | The felling blow | Up to 20 ticks late | `GateTarget.structureCue(felled)` sounds it at the blow's contact; `breach` plays it only if nothing felled the gate (`breachCued`); the felling blow plays no gate_hit |
| wave_warning | `SurvivalMode.warn` | Warning start | Warning start | None needed |
| wave_rise | `SurvivalMode.rise` | The hit (1.5 s into the file) as the dead rise | Cue began at the rise, hit 30 ticks late | Queued in `warn` to start `WAVE_RISE_LEAD_TICKS` (30) before `riseAt`; `rise` is silent |
| Survival ending | `SurvivalMode.end` | Victory or defeat sting only | A vanilla toast or wither sound at world origin on top of the sting | Removed; the sting comes from `Campaign.finish` |
| capture_start | `GravesAndGlory.captureTick` | Once when a point starts being taken | One shared key for the round: a second point contested within 10 s was silent | Key per point; per-tick cap 1 so two points in one second sound once |
| capture_won, capture_lost | `Audio.captured` | Capture completes | Capture completes | None needed (also used by `SiegeMode.fall`) |
| convoy_roll | `ConvoyMode.feed` | Wagon sets off | Only the first start; set-offs after an announced stop were silent | Plays again when the wagon moves after a stop that sounded (`stopSounded`) |
| convoy_stop, convoy_rollback, convoy_checkpoint | `ConvoyMode.feed`, `checkpoint` | The event | The event | None needed |
| clock.tick | `Audio.clock` | One per second, last ten seconds | Correct (ten ticks, 15-tick cooldown under the 20-tick spacing) | Logic moved to `AudioCues.clockSecond` so it is testable |
| countdown_tick | `Audio.countdown` (Campaign pad and room countdowns) | Last three seconds | Correct | Logic moved to `AudioCues.countdownSecond` |
| victory, defeat | `Campaign.finish` via `Audio.ending`, once per participant | Round end | Correct | `Audio.reset()` in `finish` drops queued cues so none plays over the sting |
| battle_intro, battle.drums | `CampaignClient` on the entrance camera | Camera start, once | Once, together | None needed |
| unit_reveal | `Campaign.reveal` | Card reveal | Once per reveal | None needed |
| ui_confirm, ui_back, ui_deny | `UiSounds` | Click | Click (4 to 6 tick cooldowns) | None needed |
| horns | `BattleEffects.feedback`, commanding player only | The order | Once per order | None needed |
| music transitions | `MusicDirector` | Next track after the ending sting | The epic track could start about 1.5 s into the victory sting | The director waits the sting's length (`VICTORY_STING_TICKS` 64, `DEFEAT_STING_TICKS` 84) before the next track |
| Round start | `Battle.startRound` | Fresh cooldowns, empty queue | Cooldowns kept from the last round | `Audio.reset()` before the first tick |

## Checks added (`test-tools/standalone/AudioCuesCheck.java`, part of `tools/verify.py fast`)

- Contact ticks equal `HouseAttacks.STRIKE` x 20 for every House (parsed from the source), the default equals the rig's attack peak, and no House is missing.
- `AudioCues.Schedule`: zero delay plays now, queued cues play on their tick once and in order, a clock that went back or a clear drops them, a cue may queue another.
- Clock ticks exactly ten times in the last ten seconds and the countdown three, none at 0, and the throttle lets all ten through.
- A thirty-archer volley sounds the bow at most its cap; two points contested in the same tick sound once, in different seconds both sound.
- Sting lengths match the Ogg durations (granule position over sample rate); the gate and wave leads sit inside their files; the bow, arrow and armour samples keep their approved lengths.
- Source-text checks: gate_shut and wave_rise have one caller each, gate_breach two (felling blow, fallback), clock, countdown and capture_start one each, magic.cast one, no world sound at the origin, `Audio.tick` and `Audio.reset` are wired.

## Risks

- Not observed in a client. The contact ticks come from the animation data, not from a recording; a House whose clip changes needs its entry in `AudioCues.CONTACT_TICKS` (the check fails if `HouseAttacks.STRIKE` and the table drift).
- A melee hit sound now lands a few ticks after the damage; if the target dies in between, the sound plays at its last position.
- Three approved samples keep a late strike (bow release 0.26 s, arrow impact 0.33 s, armour ring 0.15 s, i.e. 3 to 7 ticks late). Trimming them is a one-line change (`LEAD` in `import_pixabay.py`, then regenerate) but removes sound and shifts the measured level by about 2 LU, so it waits for Kort's yes.
- Player blows on a structure (vanilla attack) still sound at once.

## Update 2026-11-03

Kort approved the trim. `LEAD` is set for bow_shoot_1 (0.25 s), arrow_hit_1 (0.32 s) and armor_hit_1 (0.14 s); the importer re-levels each to the approved file's integrated loudness (within 0.2 LU). The three Risks/Findings entries above about late strikes are resolved.
