# Prop kit art (plan 2026-11-08-prop-kit, A1-A5)

**Verdict: approved by Kort, 2026-11-08** (AskUserQuestion, from the contact sheet and in-game shots below).

Branch `feat-prop-kit-art`. Props: forge (hearth, hood, coals), anvil, weapon_rack, shield_rack, quench_trough,
grindstone_bench. Built by `python art/props/build_props.py`; frame contract in `art/props/README.md`.

Evidence:
- `prop-kit-art-sheet.png` - contact sheet, 1x and 4x close-ups beside the Knight (`tools/prop_sheet.py`, density check passes).
- `prop-kit-art-ingame-near.png` - in game near shots, day and lantern-lit night (suite `prop-look`).
- `prop-kit-art-ingame-lineup.png` - the whole set in game.

Notes for SYSTEMS (also sent to the systems session):
- The 1.20.1 block atlas does not stitch `textures/prop`; the pack needs `assets/minecraft/atlases/blocks.json`
  with a directory source `prop` (prefix `prop/`), or the textures must move under `textures/item/`.
- Plan CMD formula gives forge/hearth 7310000, outside the plan's range 7310001..7310999; the preview uses +1.
- Verified spawn: `item_display:"fixed"`, `Rotation:[180f,0f]`, entity at anchor cell + 0.5.
