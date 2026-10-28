# Animation pass 1 handoff (2026-10-28, uncommitted)
- Class set (coordinator decision): knight, archer, mage, sentinel, longbow, gl_marshal. House troops ic_hearthguard, wm_tusk, tc_tideguard have no model on any branch (gap; Kort decides).
- Run 1 (knight/archer/mage) done: scratchpad run-before, sheets in scratchpad/sheets-before. Scores: attack knight 6, mage cast 6, idle 7 all, hit 7, walk/run/death 8.
- Run 2 (sentinel,longbow,gl_marshal) running: scratchpad/run-before2 (gradle.log ends "exit N"). Composite with scratchpad/sheetmk.py RUN OUT idle,walk,run,attack,hit,death.
- Next: write docs/ANIMATION-SPEC.md (<=100 lines), commit Phase A (spec, harness hook GRAVES_PHASE2_POSES, ANIMATION-PLAN, preflight-regenerated files), then Phase B: knight sword attack + mage cast first (extras class, client only; charge vs retreat needs protocol change = Kort later).
- Turn stage logs "pose false (off by 30)" for knight idle-45: shots still taken; left as is.
