# Props (smithy kit)

Large props shown by `item_display` entities from a server resource pack (plan `docs/superpowers/plans/2026-11-08-prop-kit.md`).
Built by script: `python art/props/build_props.py [id ...]` writes, per prop, `<id>/<part>.json` (Java item model),
`<id>/<id>.png` (one sheet) and `<id>/<id>.bbmodel` (Blockbench project of all parts; it opens and edits, but the
script is the source). Shared ground: `proplib.py`. Sheet: `python tools/prop_sheet.py --out <png>` (exit 1 on a face
that is not 1 texel per model px).

## Frame (contract with SYSTEMS)
- Model px, 16 = 1 block. Origin (0,0,0) = min corner (bottom, north, west) of the prop's **anchor (pivot) cell**
  `((sx-1)/2, 0, (sz-1)/2)`; x east, y up, z south. The front faces **north (-z)**.
- Every part of a prop shares the frame. Elements stay inside -16..32. A part that cannot (forge `hood`, y 32..64) is
  stored shifted, and its `display.fixed.translation` (px) shifts it back. Every part carries `display.fixed`, so parts
  are spawned with `item_display: "fixed"`, all with the same entity transform. `display.gui` fits the part in a slot.
- Calibration used for the art shots (1.20.1): entity at anchor cell + (0.5, 0.5, 0.5), `Rotation:[180f,0f]`,
  `item_display:"fixed"` puts model (0,0,0) on the anchor cell's min corner with the front facing north.
- Texture `gravesandglory:prop/<id>`; glow parts have `shade:false` elements (entity brightness 15 from the def).

| id | cells sx×sy×sz | pivot | parts (CMD order) | frame extent (px) |
|---|---|---|---|---|
| `forge` | 3×4×2 | (1,0,0) | `hearth`, `hood`, `coals` (glow) | x -16..32, y 0..64, z -1..32 |
| `anvil` | 1×1×1 | (0,0,0) | `anvil` | x -2..18 (2 px overhang), y 0..18 |
| `weapon_rack` | 2×2×1 | (0,0,0) | `weapon_rack` | x 0..32, y 0..32 |
| `shield_rack` | 2×2×1 | (0,0,0) | `shield_rack` | x 0..32, y 0..31 |
| `quench_trough` | 2×1×1 | (0,0,0) | `quench_trough` | x 0..32, y 0..15 |
| `grindstone_bench` | 2×2×1 | (0,0,0) | `grindstone_bench` | x 0..32, y 0..30, z -2..15 |

CMD = `7310001 + 10*propIndex + partIndex` (SYSTEMS `PropDef`, agreed 2026-11-08): forge hearth/hood/coals
7310001..7310003, anvil 7310011, weapon_rack 7310021, shield_rack 7310031, quench_trough 7310041, grindstone_bench
7310051. The pack builder assigns CMDs from the defs; the models carry no numbers.

## Look
- Chunky: 1 texel per model px (16 px a block), like the troops and vanilla blocks. One sheet per prop, shelf-packed.
- Ramps: 5 tones, 0 shadow .. 4 highlight, made the style guide's way (`proplib.make_ramp`: value ±22%, shadows toward
  violet-blue, lights toward warm yellow). Style-guide ramps reused as is: steel, gilt, wood, stained wood, linen, leather.
  House shield fields and charges from `art/house/<house>/banner.png` and `mark16.png`.
- Light rule (handlib `LIT`): top +1, front (north) and the viewer's left (east) at base, back and west -1, underside -2;
  side faces get a lit top row on the lit faces and a dark bottom seam. Patterns are world-aligned (stone courses line up
  across boxes), one pattern a material, marks placed sparsely, no noise or gradients.
- Stone is close to vanilla stone bricks (8×4 courses) so a forge sits in a vanilla wall without a seam in style.
