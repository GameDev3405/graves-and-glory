# Graveborn tier I worker brief (task 4B.3 part 1)

Checkout: C:/Users/kortt/Melty/bannerbound-graveborn, branch art/graveborn-t1. Do not commit, push, merge, launch
Minecraft or run tools/build_skins.py / verify.py (the coordinator does). Never use a Fable model. Another worker
builds a different troop in this checkout at the same time: touch only the files named for you.

## Reference (read only what you need)
- Pipeline: art/hand/common/handlib.py (Troop, Bone, Box, painters, face_painter) and art/hand/common/house_stub.py.
  Worked examples of the builder shape: art/hand/tc_tideguard/build_tc_tideguard.py, art/hand/mage/build_mage.py (robe),
  C:/Users/kortt/Melty/bannerbound-wildmoot/art/hand/wm_seer/build_wm_seer.py (weapon "none", hand prop modelled in,
  glow eyes). Weapons: art/hand/weapons/build_house_weapons.py, build_bows.py, build_swords.py.
- Shared Graveborn ramps: art/hand/common/graveborn.py (VIOLET accent, BONE, MOSS, RUST, GIRON, FROST, SOUL, GILT, ROT,
  GSKIN). Use them; add local ramps only if needed.
- docs/ART-STYLE-GUIDE.md 10.6 (Graveborn) and docs/superpowers/specs/2026-10-17-unit-identity.md (your troop's
  Look, Mark, Build, Portrait lines; ignore every "pennant": flags are banned).

## Rules (Kort)
- 1 texel per model pixel (density 1). Tier I only (tiers=(1,)).
- No flags, pennants or standards on the back. Other back gear is fine.
- Undead: the most distinct silhouettes in the game. Do NOT import or copy any living troop's armour, bones or
  painters (Gilded, Ironclad, Tidecallers, Wildmoot). Build your own parts; handlib's generic painters (plate, mail,
  leather...) with Graveborn ramps are fine. Hunched or gaunt, ragged hems in strips, bare bone, no shields.
- Accent is pale violet (graveborn.VIOLET) on fixed paint: hems, bindings, wraps, grips, trim. Team cloth uses the
  side's cloth sheet as in the examples. `python art/hand/common/graveborn.py art/troops/<tid>_s1.png` must print
  violet >= 15% (the House check needs 10% across all six skins).
- Souls-like game art (Dark Souls / Elden Ring): weathered, layered, strong dark-to-light read, a clear silhouette;
  not historical costume, not a flat Minecraft-mod look.
- Glow (soul-light eyes, lantern, ribcage) goes on the Troop's glow sheet (glow=...), few texels.

## Deliverable per troop <tid>
- art/hand/<tid>/build_<tid>.py, then `python art/hand/<tid>/build_<tid>.py stub` writes art/troops/<tid>.json and
  <tid>_s1.png. meta["weapon"] names your weapon (or "none").
- If your task names a weapon: art/hand/weapons/build_gb_<weapon>.py in the build_house_weapons.py style;
  `python art/hand/weapons/build_gb_<weapon>.py preview <png>` to look, `install` to write art/weapons/<weapon>.*.
- Visual check: `python art/hand/<tid>/build_<tid>.py contact <scratch png> crimson` (and one other side). Also
  `GB_OUT=<scratch png> python work/graveborn_turn_sheet.py crimson <tid>` (5 views, 4x). Look at the images, fix the
  worst issues, at most two polish rounds. Use your own scratchpad for images.
- Report (under 120 words): files written, look summary, violet %, self-score out of 10, known flaws, questions for Kort.
