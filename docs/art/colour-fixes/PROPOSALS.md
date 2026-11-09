# Colour-fix proposals (not applied)

Small palette swaps on the shipped skins. Kort asked to keep the existing art, so every swap stays inside the troop's House palette (`art/palettes/<house>.json`). Each one touches under 9% of a tier's opaque skin texels. Nothing in any repo was changed. The fixes are stored as data in `fixes.json`, which holds the exact hex pairs, the bones and faces, the bounding box for each tier and the % for each tier. `run.py` rebuilds every fix and before/after sheet from copies of the audit previews.

Region notation: the bones listed are the UV faces of those bones in the skin area. `upper half` means the top 50% of the side faces plus the top face. Team cloth is never touched. The % is the largest across tiers.

## Gilded (1 fix)

| Troop | Fix | Region | From → to | % texels | Why |
|---|---|---|---|---|---|
| mage | mg-shins | right_shin, left_shin, right_shin_robe, left_shin_robe (side faces); T1/2/3 | `#d9d5c6` WHITE[3] → `#b9b4a4` WHITE[2]<br>`#b9b4a4` WHITE[2] → `#8e897c` WHITE[1] | 2.9 | White robe is one value top to bottom; one step darker on the lower legs gives a shadow band so cowl and chest read as the lit mass. |

## Graveborn (6 fixes)

| Troop | Fix | Region | From → to | % texels | Why |
|---|---|---|---|---|---|
| revenant | rev-legs | right_leg, left_leg, right_shin, left_shin; T1/2/3 | `#1e231d` GSKIN[0] → `#3e463a` GSKIN[2]<br>`#2a3028` GSKIN[1] → `#56604f` GSKIN[3]<br>`#161a12` MOSS[0] → `#36402a` MOSS[2]<br>`#262e1e` MOSS[1] → `#4f5a3c` MOSS[3] | 8.6 | Legs sit on the two near-black GSKIN/MOSS shades and fuse into one dark column at 16 blocks; two steps up separates legs from boots and gives the lower body a mid band. |
| revenant | rev-boots | right_boot, left_boot; T1/2/3 | `#54406a` VIOLET[2] → `#332c24` ROT[2]<br>`#7a6494` VIOLET[3] → `#625746` ROT[4] | 1.1 | The purple trim stripe repeats at equal weight on helm, belt and boots; swapping the boot stripe to the House rot-leather keeps purple on helm and belt only, so the trim reads as a deliberate accent. |
| bonebow | bb-pauldrons | skull_pauldron, right_pauldron; T2/3 | `#171516` GIRON[1] → `#2b2826` GIRON[3]<br>`#221f1e` GIRON[2] → `#3d3936` GIRON[4] | 1.9 | T2/T3 pauldrons read as solid black blocks; lifting the two darkest iron shades restores a plate edge while staying darker than the bone. |
| reaper | rp-robe-light | shroud, rags (upper half); T1/2/3 | `#120f13` SHROUD[1] → `#272227` SHROUD[3]<br>`#1c181d` SHROUD[2] → `#35303a` SHROUD[4] | 8.0 | Robe is one near-black fill; lifting the upper half two steps gives top-to-bottom light so the cloth reads as hanging fabric, not a void. |
| reaper | rp-drape | hood_drape; T1/2/3 | `#08070a` SHROUD[0] → `#1c181d` SHROUD[2]<br>`#120f13` SHROUD[1] → `#272227` SHROUD[3] | 2.1 | Hood drape uses the darkest shroud shades so the hood back blends into the robe; two steps up separates the drape. |
| hexer | hx-hood | burial_hood, hood_tail; T1/2/3 | `#0c0b0c` GIRON[0] → `#221f1e` GIRON[2]<br>`#171516` GIRON[1] → `#2b2826` GIRON[3] | 5.6 | Hood is a black cube that swallows the head silhouette; lifting the two darkest iron shades shows the hood form against the robe. |

## Ironclad (0 fixes)

No fixes proposed. See the end of this file.

## Tidecallers (3 fixes)

| Troop | Fix | Region | From → to | % texels | Why |
|---|---|---|---|---|---|
| tc_raider | rd-helm-gaps | helm, helm_back; T1/2/3 | `#0d1a10` SHELL[0] → `#1c3322` SHELL[1] | 8.9 | Shell helm is about 45% the near-black shade, so it reads as a flat dark cap; one step up keeps the scute lines but lets the shell green read. |
| tc_raider | rd-helm-top | helm (top face); T1/2/3 | `#325238` SHELL[2] → `#4d7448` SHELL[3]<br>`#4d7448` SHELL[3] → `#6f9560` SHELL[4]<br>`#6f9560` SHELL[4] → `#9bb883` SHELL[5] | 2.9 | No shell highlight; lifting the top face gives the helm a lit crown at game distance. |
| tc_mist | mi-hood | hood, hood_peak (upper half); T1/2/3 | `#0a1426` SEA[0] → `#11264a` SEA[1]<br>`#11264a` SEA[1] → `#1a3f6a` SEA[2]<br>`#1a3f6a` SEA[2] → `#2a5e8a` SEA[3] | 4.0 | Hood is a dark navy slab around a small face; lifting its upper half one step puts light on the head so the caster reads at 16 blocks. |

## Wildmoot (0 fixes)

No fixes proposed. See the end of this file.

## Troops with no fix

- **Gilded:** knight, archer, sentinel, longbow, gl_marshal. These are already strong. The flat red legs and the archer's red T3 hood are team cloth, so they are outside the skin.
- **Graveborn:** stalker and gb_hollowking. The stalker's problem is frost speckle noise, which a swap cannot fix, and its pauldrons already have GIRON[6] edges. The Hollow King is strong, and its red scrap is cloth.
- **Ironclad:** all six. This is the strongest House. The red legs are team cloth. A mortar chest-board lift was tried, but it changed only 0.5% of texels and did not show at 16 blocks, so it was dropped.
- **Tidecallers:** tc_harpooner, tc_tideguard, tc_brute and tc_admiral. Their issues are noise and missing form, which need a repaint rather than a swap.
- **Wildmoot:** all six. A knee shadow band on the hunter and tusk legs was tried, but it was hidden by the boots and tunic in the renders, so it was dropped. The other Wildmoot issues are noise or need a repaint.

## Notes

- The Graveborn lifts work inside very dark ramps (SHROUD tops out at `#484250`). They are visible close up, but at 16 blocks they are subtle by design: the House stays dark.
- The clearest wins at game distance are the Sunweaver shin band, the Gravecaller hood, the Drowned Raider helm and the Mist Caller hood light.
- Sheets: `house_graveborn_before_after.png`, `house_gilded_before_after.png` and `house_tidecallers_before_after.png`. Each row is one troop tier: before near | after near | before 16 blocks x2 | after 16 blocks x2.
