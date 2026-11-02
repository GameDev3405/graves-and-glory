# Graveborn tier I handoff (task 4B.3 part 1)

Branch `art/graveborn-t1` in worktree C:/Users/kortt/Melty/bannerbound-graveborn (from main a49c9a1). Nothing pushed or merged.

## Done
- `art/hand/common/house_stub.py`: ported the sisters' `meta["rig"]` and `stub` command (same diff as Wildmoot/Tidecallers).
- `art/hand/common/graveborn.py`: shared ramps (VIOLET accent, BONE, MOSS, RUST, GIRON, FROST, SOUL, GILT, ROT, GSKIN) and `violet_share(png)`.
- Builders (Sonnet workers, tier I, 1 texel/px, no back flags, no living armour reused; brief `work/GRAVEBORN-BRIEF.md`):
  revenant (Risen, notched_sword, 6.5), reaper (scythe, 7), bonebow (bone_bow, 7), stalker (frost_bow, 7),
  hexer (weapon none, modelled lantern, 7), gb_hollowking (grave_sword, 7). Weapons in art/hand/weapons/build_gb_*.py.
- `work/graveborn_turn_sheet.py [side] [ids]` (GB_OUT overrides the path) -> `work/graveborn-t1-turn-4x-crimson.png`.

## In progress
build_skins fails on weapon geometry (bone_bow spur fixed; frost_bow riser_bind z-fight next). Then --verify,
art_hue_check (graveborn NO DATA until build_skins writes the skins), verify.py fast --offline, CRLF revert, commit.

## Polish left
Risen boxy, stoop subtle; Reaper hood boxy, violet 69% (heavy); Bone Archer pauldron dark; Stalker fur blotchy;
Gravecaller robe dark, mask flat, 54 glow texels; Hollow King ribs ladder-like, jaw wire reads as beard.
Weapons not seen in hand; scythe blade direction unverified.

## Questions for Kort
- Hollow King: spend some of the glow budget on the grave sword, at the cost of fewer lit ribs?
