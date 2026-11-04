# Ashen Vigil grade (2026-11-04)

## 1. Score: 5/10
One wool value covers about 70% of the model, every box is a flat tube, and Common to Epic look alike.

## 2. Problems, ranked
1. **One material.** Hood, capelet, robe, skirt and mantle are all `WOOL` 4-5 and merge in the in-game back view.
2. **No depth.** Folds are even full-height stripes with no shading under overhangs. The hood looks like a striped cube (contact back). The back fall looks like a door.
3. **Navy-flannel mail.** `mail()` is a 2-step, 1-texel checker across 8 px sleeves, loud in every view.
4. **Legendary hazard stripes.** `WARM[3]` rows over a soot checker on the hem, knee and boots (in-game legs).
5. **Orange-beard face.** The Legendary interior below the mask is solid `WARM` (in-game front).
6. **Bare forearms.** About 6 px of skin below the sleeves. **Cover them.**
7. **Flat rarity steps.** Sash, vials and the 2x3x2 lantern are a few texels each, so contact rows 1-4 look alike.
8. **Legendary shapes.** The peak is a centred pyramid, like a pawn. The mantle is two flat slabs.

## 3. Fixes
**Palette:**
- Bone-wool (`WOOL` 4-6) for cowl and capelet only.
- A new ash-brown, 2 steps darker, for robe and skirt.
- `SOOT` inside the cowl; `LEATH` for belt and straps.
- `GOLD` for the lantern only.
- A 1 px dark border on the stole.

**Painting:**
- Cloth: top row +1, bottom 2 rows -1.
- A 1-row -1 shadow under every overhang.
- Folds as irregular ridge/trough pairs that stop short of the hem.
- Mail as 1-step offset rows.

**Hood:**
- A 9x1x1 brim and 1x6x1 cheek flaps; recess the face 1 px.
- The mask as its own box, 0.5 px proud.
- A tapered back tail (5, 3, 1 wide).
- Lean the Legendary peak back 1 px per tier.
- Legendary light only in the eye holes and 2-3 bottom texels.
- Lantern 3x4x3, hung forward.

**Chest:**
- Capelet 4-5 px deep, 1 px overhang, ragged hem.
- A 5x6x5 bell sleeve, 1-2 mail rows, then `ASH` bracers to y10 (2 px of hand bare).
- Mantle as two-tier flared pauldrons.
- 3-4 embers.

**Legs:**
- Ash-brown panels 0.5-1 px proud, widened inward into one split robe.
- A ragged 1-2 px scorched hem.
- Rarity thread; dim `WARM[1-2]` at Legendary.

**Boots:**
- `LEATH` sole and straps, darker wraps.
- Bells on the outer ankle, a muted cuff.

**Rarity:** a 3 px sash at Rare; lantern, vials and eye holes glow at Epic.

## 4. Keep
- Cowl, mask, team stole and skirt falls.
- Soot soles, vial belt, lantern idea, glow sheet.
- Legendary silhouette growth.
