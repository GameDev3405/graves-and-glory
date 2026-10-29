# Modeling handoff for a new AI (2026-10-28)

Goal: you are being compared against the AI that built the current House troop models. Kort's verdict on that work: the general design is right, but the units look cheap. He wants high-quality, detailed units like high-end Minecraft knight/armour sets (CurseForge-style). Do a better job, or show clearly where you can't.

Read `docs/HANDOFF-FOR-NEW-AI.md` first for repo and git reality (GitHub is not the true state). This file covers modeling only.

## Where to work
- Repo: `C:/Users/kortt/Melty/` worktrees. Start from branch `troop-models-house` (b9884ff, local only). Do not edit `C:/Users/kortt/Melty/bannerbound-models` itself, which is the other AI's checkout and holds its work. Make your own worktree: `git worktree add ../bannerbound-models2 -b troop-models-house-v2 troop-models-house` (run from `bannerbound`). One writer per checkout.
- Never push main, never publish, never kill processes, do not touch the other worktrees. Push only a feature branch, and only if Kort says so.
- Gameplay, protocol, server, `design.json` stats and hit timing are out of scope.

## What exists (your baseline to beat)
- Three House troops, built by `tools/paint_house_troops.py`: `ic_hearthguard` (Ironclad, broad and short, box helm, braided beard, axe and shield), `wm_tusk` (Wildmoot, hunched, boar snout, kilt, crossbarred boar spear), `tc_tideguard` (Tidecallers, fin cap, coral pauldrons, trident).
- The roster is 30 troops in 5 Houses (`sheets/design.json`, keys `houses`, `troops`): Gilded (knight, archer, sentinel, longbow, mage, gl_marshal), Graveborn, Wildmoot, Ironclad, Tidecallers. Only these three House troops plus the six Gilded/Order troops have finished own art; the rest use stopgap art.
- Self-scored 8/10 each by the previous AI; Kort finds them cheap. Known flaws: Hearthguard folded arms read as one dark bar and hide the front glyph; Tusk kilt floats forward in side views; Tideguard cuirass blends into its mail; shields not accented; only gold side, stage 1 rendered.
- Contact sheets of the current look: `C:/Users/kortt/Melty/bannerbound-models/work/troops/turn-r3/` (`turn-all.png`, `turn-ic_hearthguard.png`, `turn-wm_tusk.png`, `turn-tc_tideguard.png`). Reference pictures Kort likes are in `docs/visual-references/` (02-veteran-units.png) and the CurseForge knight sets he posted: layered plate with rivets and seams, per-plate shading and edge highlights, chainmail under plate, patterned heraldic tabards (checks, bands, emblems), fur/leather trim, ornate crests and plumes, layered pauldrons, belts, buckles, tassets, greaves, 4-5 tone worn metal, a clear dark-to-light read.

## Kort's design rules (new)
1. NO flags or banners on the backs of units. Pennant/standard parts are removed everywhere. Do not bring them back.
2. Team/House identity comes only from accent colors on the shield, weapon, clothing, tabard, trim and plume. Water (Tidecallers) reads blue, Gilded gold, Ironclad ember/orange, Wildmoot ochre/hide, Graveborn pale/violet. Use side cloth (`art/team/team_cloth.png`, glyph masks) for the colors that change by player side, and fixed paint for House color.
3. Quality over speed: no flat skins. Use extra boxes for crests, shoulder layers, tassets, capes, belts, greaves and weapon detail, not just paint.
4. Souls-like game art taste, not historical re-creation and not a plain Minecraft-mod look (Kort, 2026-10-22).
5. Silhouettes must differ clearly between troops at six blocks. Keep the existing character of each design: Hearthguard broad and short with box helm, Tusk hunched boar-snouted spearman, Tideguard finned trident guard.

## Art rules (from `docs/ART-STYLE-GUIDE.md`, read it fully)
- 3 texels per model pixel for NEW art only; simple Minecraft-style faces at 2 with four variants; weapons at 2. Existing art is not remade.
- 4-5 tone ramps per material with hue shifts (shadows toward blue-purple, lights toward warm yellow). Light from the top left. Face tones: up 4, south 3, west 3, north 2, east 1, down 0.
- Team tint rule: no bright saturated texels near any side's hue except on the cloth sheet; `build_skins.py` fails the build otherwise.
- One ramp and one pattern per material; gilt is trim, not a fill.
- Skins pass at score 9, weapons at 8.5 (plus the weapon gate checklist in section 9). Self-score from sheets you view yourself; list observable defects, not just numbers. At most two correction rounds, then record what is under 9.

## Pipeline
- Rigs: `art/rigs/` (biped, colossus, hound, spider). Parts: `art/parts/*.json` (extra boxes). Per troop: `art/troops/<id>.json`, skins `_s1`-`_s3` (tiers), `_veteran`, `_faces`. Weapons: `art/weapons/` (`.bbmodel`, `.json`, `.png`, `_m` map; tiers I-III). Built assets go to `src/main/resources`; index at `index.json`.
- Generate: `python tools/paint_house_troops.py` then `python tools/build_skins.py` then `python tools/build_skins.py --verify`. `--tiers` shows which troops still use stopgap art.
- Data-driven: no Java is needed for new models. Spec.java is generated by `tools/preflight.py`; never hand-edit it.
- Render a turnaround sheet in the real game (only when no Minecraft client is running; never kill one): set `GRAVES_ROADMAP_TEST_ROOT=<new empty dir> GRAVES_PHASE2_CHECKS=turn GRAVES_PHASE2_TURN=<ids>` and run `gradlew runClient -PlocalIntegration -I test-tools/isolated-checks.gradle` (offline Gradle, JDK 17). First write `<root>/run/options.txt` with `onboardAccessibility:false`, `pauseOnLostFocus:false`, `guiScale:3` (each on its own line), or Minecraft stalls on the Narrator welcome screen. A render takes about 15 minutes for three troops. If a first run crashes with `NoClassDefFoundError Veterans$Tally`, retry identically.
- Verify: `python tools/verify.py fast --offline` (needs `pwsh.exe`: copy powershell.exe to a dir on PATH). Game tier needs no client running.
- Windows, line endings: `.jem` files are LF-pinned. Pre-existing dirty `Spec.java` and `sheets/preflight.json` show line-ending-only diffs; leave them out of commits (`git add` only your files).

## Suggested comparison protocol (so Kort can judge fairly)
1. Redo the same three troops (`ic_hearthguard`, `wm_tusk`, `tc_tideguard`), stage 1, gold side, same harness and sheet layout so the sheets are side by side comparable.
2. Then, as far as time allows: tiers II/III, other sides, face close-ups, and the six Order troops.
3. Deliver: sheet PNGs with absolute paths, a short list of what is under 9 and why, `verify.py fast` result path, and your commits. Update a `docs/MODELS-HANDOFF.md` in your own branch.
4. Report: done / worked / broken / left, plus assumptions. Show Kort images in-session.

## Working habits that avoid wasted runs
- Workers cap near 30-40 tool turns; commit WIP after every working step, and never end your turn while a render runs.
- Make large edits, render once per round, view the sheets yourself before scoring.
- Ask Kort when a design call changes gameplay or the look beyond this brief (he decides on UI, new units, protocol, saves).
