# M1 local UI integration, 2026-10-28

Outcome: INCOMPLETE. Integration, fast tier and game tier pass; the War Table/HUD UI suites still have real defects. phase-1-solo-core was NOT fast-forwarded. Nothing pushed.

Branch m1-ui-integration (worktree C:/Users/kortt/Melty/bannerbound-m1), cut from phase-1-solo-core c57727a.
Merges: ui-port-claims 5dcf3da, ui-port-recruit 1e79065 (Phase2Checks conflict resolved: recruit's stage cases kept, claims' case 104 kept, old 104/106 claim fixtures dropped), ui-armoury 92a4e8a (clean), builder-server 3f68caf (clean). HUD lane patch (ui-hud checkout untouched; excluded Spec.java, preflight.json and its older CLAUDE/WORKFLOW/HANDOFF copies) committed as 72cd850.
Source commit 72cd8500ca5bc9ab12fd9ba9634eab47c65023da, clean tree; sourceSha256 d359e44c8603a9ec524560299c39fedae7d3a0c168626bc7bda324ff3f294a2d. Jar preserved at work/candidates/<sha>/graves-and-glory-0.5.0.jar (sha256 51cbc1d3...9d2f).

Checks (tools/verify.py needs `pwsh`, absent here; a copy of powershell.exe named pwsh.exe on PATH was used, outside the repo):
- fast: work/verify/20261028-143943-46556/result.json, exit 0, pass.
- game: work/verify/20261028-144248-4440/result.json, exit 0, pass (Phase 1 combat smoke only).
- Extra UI suites (not part of verify): Phase 1 ui-art,ui-hud,ui-feed,ui-home,ui-recruit,ui-pages,ui-icons,ui-leave: work/m1-visual/p1/evidence/local-checks.txt, 81 pass, 6 fail. Phase 2 screens,commander,armoury,gear: work/m1-visual/p2/evidence/local-checks.txt, 80 pass, 2 fail.
- Pass: all p2-claim-*, p2-screens-suite, p2-armoury-suite, p2-gear-suite, ui-recruit, ui-home.

Defects (all outside the Phase1/Phase2Checks overlap; not fixed, per stop rule):
1. p1-ui-page-banner (+ p1-ui-pages-suite): banner page shows one House, "12 seats" row Add/Remove and null hint wrong; commander class text is clipped behind plaques ("our / deal", "10% more damage."). Plan Task 3 (and Task 2) not built.
2. p2-class-picker (+ p2-commander-suite): two lines cut. Plan Task 2 not built.
3. p1-ui-cards, p1-ui-hud-suite, p1-ui-feed-kinds, p1-ui-feed-last-stand, p1-ui-feed-suite: LAST STAND card/word/stamp not shown for support, feed line truncated to "Infantry make a last...", no last-stand feed line after the state. HUD lane work (plan Tasks 6/7) unfinished or wrong.

Visual (harness screenshots read by Claude; Kort's own look, plan Task 10, is separate): work/m1-visual/p1/run/screenshots and work/m1-visual/p2/run/screenshots. Seen: recruit page 960x540 and 854x480 (p1-ui-recruit.png, p1-ui-recruit-854.png) layout fits, but the "Epic within 40 letters" line is low contrast on wood and the Templar portrait is a tight, soft crop; contracts and chapter pages with the House row (p2-wt-contracts-claim.png, p2-wt-chapter-pick-compact.png) fit with no overlap; Armoury (p2-armoury-tab-854.png) is a plain dark panel, not War Table styled and not compared to a reference; banner page and HUD feed show defects 1 and 3. Not compared side by side with docs/visual-references or greyscale/rubric; VIS-01 (in-party Friends at 1080p, 1440p/4K, Frostmarch supply flag) not checked.

Not verified: six players, dedicated server, release/package tier, Phase 3/House/level regression suites, 1440p/4K, hover/focus/disabled states.

Record: docs/releases/current-candidate.json updated for this identity (path is docs/releases/, older notes cite PROJECT/docs/releases/ or releases/ relative to docs).
Next: finish plan Tasks 2, 3, 6, 7 then rerun; Spec.java/preflight.json regenerate as CRLF-only diffs (restored with git checkout).
