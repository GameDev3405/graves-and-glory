# Wildmoot tiers II and III: handoff (task 4B.1, 2026-11-02)

Branch `art/wildmoot-tiers` in `C:/Users/kortt/Melty/bannerbound-wildmoot2`. Not merged, not pushed.

## Done
Tiers II and III are built for five troops: Hound, Hunter, Stalker, Seer and Old Horn. All are hand-built at 1 texel per pixel and follow the Gilded pattern: `per_tier` painters, tier-only bones with `stages=[2,3]` or `[3]`, and no `tiers=(1,)`.

The house rule throughout:
- Tier II adds bone and trophies.
- Tier III spends the ochre and adds one crest-level part.
- No plate and no back flags.

| Troop | Tier II | Tier III |
|---|---|---|
| Hound (Dire Hound / Packleader) | Bone skull-cap, tooth collar, ochre leg wraps, taller spikes | Antler tufts, bone spine plates, ochre flank bars |
| Hunter (Thornstalker / Elder Hunter) | Bone cap plate with a tusk, thorn-spiked pauldron, second bracer, bone quiver rim | Five-feather fan (`feather_fan_5_*`) barred in ochre replaces the three-feather fan, which is now `stages=[1,2]`; grey pelt mantle, tusk necklace, ochre quiver bands |
| Stalker (Broodstalker / Widowmaker) | More silk sacs, thorax ridge plates, cord beads | Bone mandibles, mask crest, ochre hourglass on the abdomen's rear top |
| Seer | Third antler tines, bone plaques on the shawl, feather mantle, ochre wand bands | Taller tine crown, two skulls on the staff, tooth collar, ochre hem fringe. Glow is unchanged. |
| Old Horn | Brow plate, bone pauldrons, greaves, rack bands | Tusk hump crest, trophy skulls, greave tusks, ochre-dipped tines |

Shared tier painters (`wm_plate`, `wm_tusk`, `wm_skull`, `wm_band`) were added to `art/hand/common/wildmoot.py`.

## Bug fixed
Old Horn on main had 66 bones, which is over `TroopModel.MAX_BONES` = 64. Above that limit the client falls back to the vanilla model. Static parts are now merged into their parent bones, so Old Horn has 57 bones at all three tiers.

Bone counts are now:

| Troop | Bones |
|---|---|
| Hound | 28 |
| Hunter | 41 |
| Stalker | 32 |
| Seer | 38 |
| Old Horn | 57 |

The hand path in `build_skins` still has no 64-bone check. It is worth adding one.

## Checks
- The `stub` command was run for all five troops.
- `build_skins` produced 346 outputs, and `--verify` passed.
- `art_hue_check`: Wildmoot PASS, 99% in band, 16 skins.
- `verify.py fast --offline`: PASS (build and all checks).

## Review
- Sheet: `work/wildmoot-tiers-II-III-4x.png`. Tiers I, II and III per troop, five views each.
- Rebuild the sheet with `python work/wildmoot_tiers_sheet.py`.

## Open
- Tusk Spear tiers II and III are not built.
- Veteran trims are not done.
- The Old Horn war-banner question is still open.
- The Seer's `bone_wand` weapon asset is still missing.
- The spec lists the Seer and Old Horn as "no evolution". Their tiers are built anyway, following Gilded's single-stage troops, and need Kort's confirmation.
- The Hound is the weakest troop. It is small, so the tier II changes read faintly at game scale.
