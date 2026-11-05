# In-game attack clip check, 2026-11 (qa/polish-1104)

All 30 troops now have an attack clip. Branch `qa/polish-1104` from main aaabecd7. Real client, the Phase 2 turn suite (`GRAVES_PHASE2_CHECKS=turn`), one client at a time, scratch roots `work/ingame-check/atk-*` (not committed). Shot: stage 1, weapon I, the family's first attack (`attack` alias, a caster's cast, a bow's nock, draw and loose) at windup, impact and recovery, at 0 (front), 45 and 90 (side). All five House runs exited 0 with "all checks passed", and **NO CLIP appeared for any troop**, so the 18 troops left unchecked by `art-qa-2026-11-04.md` are now covered. Four large troops (brute, oldhorn, hollow king, colossus) were re-shot at 5 blocks (`atk-big`).

## Result
**4 troops with defects (6 items, D1 to D6), 5 troops on a watch item (bow hidden at the side view), 21 PASS.** Judged at sheet and zoom resolution only, so each item needs Kort's eye at full size. No art was changed.

## Contact sheets (`docs/reports/art-qa-ingame-attacks-2026-11-evidence/`)
Columns: windup or nock at 45, impact or loose at 0, impact 45, impact 90, recovery 45.
- `attacks-gilded.jpg`, `attacks-graveborn.jpg`, `attacks-ironclad.jpg`, `attacks-tidecallers.jpg`, `attacks-wildmoot.jpg`
- Detail crops: `detail-reaper-hollowking-mortar.jpg`, `detail-colossus-brute-ballista-seer.jpg`, `detail-big-troops-dist5.jpg`
- Raw 1280x720 shots (scratch, not committed): `work/ingame-check/atk-<house>/run/screenshots/p2-turn-<troop>-<pose>-<angle>.png`, poses windup or nock, attack, cast or loose, recovery.

## Per troop
- **knight** (gilded): PASS. Thrust reads level at impact; shield on the threat side. Blade beside the shield edge at impact 45 looks intended at this size.
- **sentinel** (gilded): PASS. As knight.
- **gl_marshal** (gilded): PASS. As knight.
- **archer** (gilded): WATCH. Bow hidden behind the torso at 90 for nock, draw and loose (carried from 11-04); 0 and 45 fine.
- **longbow** (gilded): WATCH. Same as archer.
- **mage** (gilded): PASS. Staff in hand at windup, cast and recovery.
- **revenant** (graveborn): PASS. Sword in hand and leading at impact.
- **reaper** (graveborn): DEFECT. D1 scythe not visible at windup 0 and 45 (`atk-graveborn/run/screenshots/p2-turn-reaper-windup-45.png`, `-0.png`); it only reappears at impact. D2 at impact 90 the long blade rests on the ground line (`p2-turn-reaper-attack-90.png`); may clip the floor.
- **gb_hollowking** (graveborn): PASS. Sword leads at impact (re-shot at 5 blocks, `atk-big`); windup 0 has the weapon crossed behind the shoulders, still attached.
- **hexer** (graveborn): PASS. Lantern in hand, held out at cast.
- **bonebow** (graveborn): WATCH. Bow hidden at 90, as archer.
- **stalker** (graveborn): WATCH. Bow hidden at 90, as archer.
- **ic_hearthguard** (ironclad): PASS. Axe and shield in hand, axe leads at impact.
- **ic_breaker** (ironclad): PASS. Hammer in hand at all keys; head swings past the leg at impact 45 without touching it.
- **ic_arbalist** (ironclad): PASS. Crossbow shouldered and pointed from draw to recovery.
- **ic_ballista** (ironclad): PASS. Crew crouched at the ballista, hands on it. Small pose change (about 10k to 16k px against idle) but not frozen.
- **ic_mortar** (ironclad): DEFECT. D3 the pose barely moves: impact differs from idle by fewer pixels than the windup (12k against 15k) and no weapon is in a hand, only the backpack tube (`atk-ironclad/run/screenshots/p2-turn-ic_mortar-attack-45.png`). Reads frozen, or the clip is too subtle to see.
- **ic_colossus** (ironclad): DEFECT. D4 at recovery 45 the stance is splayed with the left foot and the hammer head both at the ground, knee angle looks off (`atk-ironclad/run/screenshots/p2-turn-ic_colossus-recovery-45.png`, and `atk-big` at 5 blocks). D5 at impact 90 the hammer head dips below the grass line (`atk-big/run/screenshots/p2-turn-ic_colossus-attack-90.png`); fine if the strike is meant to hit the ground.
- **tc_raider** (tidecallers): PASS. Blade in hand; at the front impact it points at the camera.
- **tc_harpooner** (tidecallers): PASS. Harpoon in hand through windup, thrust, recovery.
- **tc_tideguard** (tidecallers): PASS. Spear in hand; shield arm clear.
- **tc_brute** (tidecallers): DEFECT. D6 at recovery 45 the blue claw drops below the ground line (`atk-big/run/screenshots/p2-turn-tc_brute-recovery-45.png`). The 3-block camera was too close for this troop; judged from the 5-block re-shot.
- **tc_admiral** (tidecallers): PASS. Weapon in hand; at the front impact the crossbow end reaches the camera, a perspective effect.
- **tc_mist** (tidecallers): PASS. Casting hand out at cast, small prop in hand.
- **wm_hound** (wildmoot): PASS. Lunge and bite at ground level, no clipping.
- **wm_stalker** (wildmoot): PASS. Crouched leap, legs stay above the ground.
- **wm_tusk** (wildmoot): PASS. Spear in hand through the thrust.
- **wm_oldhorn** (wildmoot): PASS. Charge head-down with antlers clear of the ground (5-block re-shot).
- **wm_hunter** (wildmoot): WATCH. Bow hidden at 90, as archer.
- **wm_seer** (wildmoot): PASS. Casting arm out at cast, bone prop in hand.

## Method notes
- Camera is the suite's default (3 blocks); the sheets crop its centre, so wide beasts are partly cut there.
- Not checked: overhead and low-cut variants (first attack per family only), back views, tiers II and III weapons, House team cloth.
- Backward joints: none seen. Frozen poses: only ic_mortar is suspect.
- Re-run: `GRAVES_PHASE2_CHECKS=turn GRAVES_PHASE2_TURN=<house or ids> GRAVES_PHASE2_TURN_ATTACKS=attack GRAVES_PHASE2_TURN_ANGLES=0,45,90 GRAVES_PHASE2_TURN_DIST=3` through `runClient -PlocalIntegration --offline -I test-tools/isolated-checks.gradle` (`.claude/skills/troop-animation/references/pipeline.md` section 8).

## Fix round (2026-11-04, qa/polish-1104)
Clip-only fixes in `art/clips/build_attacks.py` (regenerated bbmodels and clip JSON; timings, impact ticks and stats unchanged). Re-shot with the turn suite, one client at a time: reaper and ic_mortar at 3 blocks, ic_colossus and tc_brute at 5. Sheets in `art-qa-ingame-attacks-2026-11-evidence/after-fix1-<troop>.jpg` (rows: before, after; columns windup 45, impact 0/45/90, recovery 45). Frames below are under `work/ingame-check/` (local, not committed).
- **D1 reaper: FIXED.** The scythe is cocked high over the right shoulder (wider body coil, arm raised and swung out), so the blade shows at windup 0 and 45 (`fix1-reaper/run/screenshots/p2-turn-reaper-windup-0.png`, `-45.png`).
- **D2 reaper: FIXED.** Less root sink at hit and follow and a flatter wrist; the blade sweeps level about 4 px above the grass at impact 90 (`fix1-reaper/run/screenshots/p2-turn-reaper-attack-90.png`).
- **D3 ic_mortar: FIXED.** `crew_fire` rewritten: a crouched windup with the left hand on the lanyard, a hard lanyard yank and recoil squat at the impact, then a rise back to idle. Windup, impact and recovery now read as three distinct poses (`fix1-mortar/run/screenshots/p2-turn-ic_mortar-windup-45.png`, `-attack-45.png`, `-recovery-45.png`). The impact pitches the tube forward strongly; that is the intended recoil read.
- **D4 ic_colossus: FIXED.** The recovery stands upright, feet under the body, the maul carried up across the chest, nothing at the ground (`fix1-colossus/run/screenshots/p2-turn-ic_colossus-recovery-45.png`). The old "splayed leg" was the 30 px right arm reaching forward with its fist on the grass.
- **D5 ic_colossus: FIXED. Judgement: the ground strike is intended** (a slam). The head should land on the grass, not in it. The impact pose now holds the head's lowest corner about 0.6 px above the ground by FK, level with the soles at impact 90 (`fix1-colossus/run/screenshots/p2-turn-ic_colossus-attack-90.png`); the static frame changed little. The real dip was between the impact and the follow-through (catmull overshoot drove the head 5 to 7 px into the ground around 0.63 to 0.66 s, between the suite's frames). The follow-through is now linear from the impact, so the head rises straight off the grass.
- **D6 tc_brute: FIXED (round 2).** The head stays raised and the jaw shut through the follow-through and settle, so the jaw tentacles sit about 4 px above foot level at recovery (round 1 left them touching the grass; `fix1-brute` and `fix2-brute/run/screenshots/p2-turn-tc_brute-recovery-45.png`; the sheet has before, round 1 and round 2 rows).
- **Leftover (not a clip fault):** the tc_brute model's jaw tentacles and the kelp strand (`kelp_4`) hang about 4 px below foot level in the rest pose, so they touch the grass whenever the head is level (idle, walk). That needs a model change, out of scope here.
- Checks: `tools/clips.py --verify` (91 OK), `build_attacks.py --check`, `test_attack_clips.py`, `tools/verify.py fast` PASS; every turn-suite run "all checks passed". Bow side-view WATCH items left as they were.
