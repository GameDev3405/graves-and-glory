# Animation pass 1 handoff (2026-10-28, uncommitted)
- Class set (coordinator decision): knight, archer, mage, sentinel, longbow, gl_marshal. House troops ic_hearthguard, wm_tusk, tc_tideguard have no model on any branch (gap; Kort decides).
- Run 1 (knight/archer/mage) done: scratchpad run-before, sheets in scratchpad/sheets-before. Scores: attack knight 6, mage cast 6, idle 7 all, hit 7, walk/run/death 8.
- Run 2 (sentinel,longbow,gl_marshal) running: scratchpad/run-before2 (gradle.log ends "exit N"). Composite with scratchpad/sheetmk.py RUN OUT idle,walk,run,attack,hit,death.
- Next: write docs/ANIMATION-SPEC.md (<=100 lines), commit Phase A (spec, harness hook GRAVES_PHASE2_POSES, ANIMATION-PLAN, preflight-regenerated files), then Phase B: knight sword attack + mage cast first (extras class, client only; charge vs retreat needs protocol change = Kort later).
- Turn stage logs "pose false (off by 30)" for knight idle-45: shots still taken; left as is.

## Phase B status (Sonnet, stopped on Kort's change of plan; UNVERIFIED)
Done (only src/main/java/gg/gravesandglory/BipedClips.java changed; client-only, no server/protocol/sheet/TroopTells change):
- idle: breath, weight shift, head turn, small bob (loop 3 s, start = end).
- attack: sword cut, windup 0.12 s, contact 0.25 s (unchanged), follow-through, recover 0.5 s; round 2 lowered the contact arm to pitch -45 yaw -22.
- hit: stronger recoil (body, head, arms, knees, root dip), 0.3 s.
- cast: arm forward -75 with right_hand 80 (staff upright attempt), off hand open, sway. NOTE: several rotation tracks on one bone SUM in Animation (hold + wave add up), so waves use base 0.
Sheets: round 1 (idle, hit, attack) rendered: hit clearly stronger (about 8.5-9), idle fine (about 8, snapshot), knight attack still read as a stiff arm (about 6.5) and mage cast was a T-pose reach (about 6); round 2 (attack only, with the changes above) rendered at scratchpad run-after2 / sheets-after2 but NOT looked at, so knight attack and mage cast scores are unknown.
Not done: verify.py fast/game (never run), clip-presence check, Phase2Checks lookup updates (may need changes), after-sheets for sentinel/longbow/gl_marshal, charge/march variants, server-timing diff proof (expected: diff only touches BipedClips and test tools). HouseAttacks/TroopTells overrides for biped attack are not affected.
Sheet commands: scratchpad sheet.sh NAME POSES (six-troop list includes House ids with no model), sheet2.sh for sentinel/longbow/gl_marshal, sheetmk.py RUN OUT POSES.
