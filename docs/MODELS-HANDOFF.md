# House troop models handoff (ic_hearthguard, wm_tusk, tc_tideguard), 2026-10-28

Status: built, wired through index.json (data-driven; no Java change), rendered in game. Branch troop-models-house off phase-1-solo-core; local commits only.
Generator: tools/paint_house_troops.py writes art/parts (14 parts), art/troops (json, _s1-_s3, _veteran, _faces) and art/weapons (hearth_axe, boar_spear, coral_trident, tiers I-III); then `python tools/build_skins.py` and `--verify`.
Sheets: work/troops/turn/turn-*.png (all three, first pass); work/troops/turn-r1/turn-ic_hearthguard.png (after lightening the helm and pauldrons to mid iron).
Render: GRAVES_ROADMAP_TEST_ROOT=<new dir> GRAVES_PHASE2_CHECKS=turn GRAVES_PHASE2_TURN=<ids> gradlew runClient -PlocalIntegration -I test-tools/isolated-checks.gradle, only when no other client of Kort's runs.
Pre-write <root>/run/options.txt with "onboardAccessibility:false\npauseOnLostFocus:false\nguiScale:3\n": Phase2Checks does not dismiss the first-run accessibility screen, and a fresh root stalls on it.
The first run in a fresh worktree loaded without the main mod (NoClassDefFoundError Veterans$Tally); an identical retry loaded it. The cause is unknown.
Deviations: chest_panel stands in for the tideguard sash and the hearthguard tabard; no relief (_r) layers; no outer layer. The hearthguard's folded-arm idle hides the axe (the axe and shield show only in attack).
Unverified: the pennant offsets look right in the turn shots but were not checked against carry or banner code; there are no portrait or face close-up checks and no tier II/III renders (stage 1 only).
verify.py fast needs pwsh: put a pwsh.exe copy of powershell.exe on PATH (in Git Bash use /c/... form, not C:/...).
