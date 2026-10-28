# House troop models handoff (ic_hearthguard, wm_tusk, tc_tideguard), 2026-10-28

Status: knight-armour pass built and rendered in game (stage 1, gold). Branch troop-models-house; local commits only.
Flags: the pennant and the standard are gone from every troop (art/parts deleted, troop jsons, TroopModel.FLAGS, Phase2Checks). Team colour reads through accents instead.
Team accents (side cloth): Hearthguard chequy tabard and horsehair crest; Tusk barry kilt, headband and forearm wraps; Tideguard barry-wavy tabard, dorsal fin and wraps. The side glyph sits on each tabard or kilt.
House paint (fixed): ember (Ironclad), ochre (Wildmoot) and sea blue (Tidecallers) on bordures, cuffs, stitching and weapon grips.
Generator: tools/paint_house_troops.py writes art/parts, art/troops (json, _s1-_s3, _veteran, _faces), art/weapons (tiers I-III), and regenerates art/team/team_cloth.png and the glyph masks (it keeps the Order team parts' pixels). Then run `python tools/build_skins.py` and `--verify`.
Plate: five-tone ramps with edge highlights, rivets, lame seams and scuffs over mail; layered pauldrons, belts, buckles, tassets, knee cops, sabatons and bracers are extra boxes. build_skins GREYS gained the 192 and 64 field greys.
Sheets: work/troops/turn-r2 (first pass of this design), work/troops/turn-r3 (after the kilt and crest fix).
Render: GRAVES_ROADMAP_TEST_ROOT=<new dir> GRAVES_PHASE2_CHECKS=turn GRAVES_PHASE2_TURN=<ids> gradlew runClient -PlocalIntegration -I test-tools/isolated-checks.gradle, only when no other client of Kort's runs.
Pre-write <root>/run/options.txt with "onboardAccessibility:false\npauseOnLostFocus:false\nguiScale:3\n": Phase2Checks does not dismiss the first-run accessibility screen, and a fresh root stalls on it.
The first run in a fresh worktree once loaded without the main mod (NoClassDefFoundError Veterans$Tally); an identical retry loaded it.
Open: Phase2Checks death, spawn and cull checks now read chest_panel instead of the standard. The cull camera aim was built around the tall standard and needs a game run to re-check.
Open: the hearthguard's folded arms hide most of the front glyph; the back glyph shows. Shields are the server's offhand item and were not changed. The Order troops (knight, archer, longbow, mage, sentinel, gl_marshal) were not upgraded, and their notes still mention the pennant.
Unverified: tier II/III and other-side renders, portrait and face close-ups.
verify.py fast needs pwsh: put a pwsh.exe copy of powershell.exe on PATH (in Git Bash use /c/... form, not C:/...).
