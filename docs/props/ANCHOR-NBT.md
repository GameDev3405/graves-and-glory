# Prop anchors in structure NBT

For map generators (Python tools that write `.nbt` structure templates directly, never through the builder world). A prop is one
hidden **anchor** command block plus things the mod can rebuild from it. A generator writes only the anchor; when the template is
placed (`Structures.place`) or its chunk loads, `Props.scanBox` restores the prop's barrier cells and light blocks (into air only,
never over another block) and spawns its `item_display` parts. Class javadoc: `Props.java`; geometry: `PropGeometry.java`.

Helper: `tools/prop_anchor.py` (`anchor(id, anchor_cell, facing, floor_state, depth)`). Example template:
`docs/props/examples/prop_facings.nbt` (built by `python tools/prop_anchor.py --example`).

## 1. Block state

```
minecraft:command_block[conditional=false,facing=<dir>]
```

`facing` is the prop facing, turned clockwise seen from above. The front of every prop model is north.

| prop facing | `facing=` |
|---|---|
| 0 | `north` |
| 90 | `east` |
| 180 | `south` |
| 270 | `west` |

Any other value, or a block that is not a `command_block`, is not an anchor. `Structures.place` rotates the template (WorldEdit and
mapforge turn `facing` too), so a template authored at facing 0 can be placed at any rotation.

## 2. Block-entity NBT

The `nbt` of the block in the template (no `x`, `y`, `z`):

| field | value |
|---|---|
| `id` | `"minecraft:command_block"` |
| `Command` | `gravesandglory:prop/<id> depth=<n> floor=<block state>` (string, one line) |
| `auto` | `0b` (never run; the text is not a valid command anyway) |
| `powered`, `conditionMet` | `0b` |
| `SuccessCount` | `0` |
| `TrackOutput` | `0b` |
| `UpdateLastExecution` | `1b` |
| `LastOutput` | `""` |

Only `Command` matters to the mod; the rest are the vanilla defaults of an idle block, so the block loads without noise.

`Command` fields, space separated, in this order:

- `gravesandglory:prop/<id>`: `<id>` is the def id (`gg_props/<id>.json`). An unknown id makes the block a plain command block.
- `depth=<n>`: blocks between the anchor block and the anchor cell (section 3). Missing means 1 (an old anchor); the scanner moves an
  old anchor down to a hidden cell when it can.
- `floor=<block state>`: the block the anchor replaced, as `namespace:name[prop=value,...]` with properties sorted by name
  (`minecraft:stone`, `minecraft:grass_block[snowy=false]`). Removing the prop puts this block back. It must be the block you
  would have written in that cell without the anchor.

## 3. Depth rule

The anchor block sits **directly below the anchor cell**, `depth` blocks down: its cell is `(ax, ay - depth, az)`.

- **depth 2 to 12**: the preferred, hidden anchor. The anchor cell must be *buried*: all 6 neighbours of the anchor block are opaque
  full cubes (stone, dirt, planks, grass block, ...; not slabs, stairs, glass, leaves, air), or out of the world. The floor block
  directly under the anchor cell (depth 1) stays. The game picks the smallest depth that works; a generator may pick any depth
  in range that satisfies the rule. Because the anchor block itself replaces one block, write the `floor=` of the block you replace.
- **depth 1**: the fallback when no buried cell exists (thin floor, floor over air). The anchor replaces the floor block directly
  under the anchor cell and shows. The game falls back to it too, with a warning in the log.

So a floor made of 3 layers of full cubes under the prop (anchor at depth 2 with a full layer above and below it, and full cubes
around it) hides it. A single slab-thick floor cannot.

## 4. The anchor cell and rotation

The **anchor cell** is the prop's pivot cell on its ground layer: `((sx-1)//2, 0, (sz-1)//2)` of the def's `size` (integer
division), the same pivot `Structures.place` uses. A 3x3x6 `supply_wagon` has its anchor cell at (1, 0, 2): the middle column of
the 3 cells wide, the third of the 6 deep. Cells of the def (`solid`, `lights`) are `(x east, y up, z south)` counted from the
footprint's min corner at facing 0.

Cell offsets turn about the anchor cell. With `(dx, dz) = (x - px, z - pz)` and the anchor cell at `(ax, ay, az)`:

| facing | world cell |
|---|---|
| 0 | `(ax + dx, ay + y, az + dz)` |
| 90 | `(ax - dz, ay + y, az + dx)` |
| 180 | `(ax - dx, ay + y, az - dz)` |
| 270 | `(ax + dz, ay + y, az - dx)` |

`y` is never turned. East turns to south at 90 degrees. The anchor cell stays where it is at every facing.

`solid` cells become `minecraft:barrier`, `lights` cells become `minecraft:light[level=<level>]`. The generator may leave them out (the
scanner places them into air) or write them itself; if it writes something else there, the scanner leaves it alone. A prop whose cell
holds another block is not given a barrier there. Wall-hung and overhead props can have an empty `solid`.

## 5. Writing one

```python
import sys; sys.path.insert(0, 'tools')
from prop_anchor import anchor

a = anchor('supply_wagon', anchor_cell=(10, 4, 7), facing=90, floor_state='minecraft:stone', depth=3)
a.pos      # (10, 1, 7)
a.state    # 'minecraft:command_block[conditional=false,facing=east]'
a.nbt      # the block-entity dict of section 2
a.solid    # rotated solid cells, to keep clear or to check against your own blocks
```

Put `a.state` in the template palette and `a.nbt` in the block's `nbt` (mapforge `Structure.blocks` takes `(pos, palette index, nbt Tag)`).
Template cells are relative to the template's min corner, so subtract it from `a.pos` and the cells. Keep every solid and light cell
air (or structure void) in the template so the scanner can fill it. The template box must contain the anchor block: `Structures.place`
scans the footprint box of the whole piece.
