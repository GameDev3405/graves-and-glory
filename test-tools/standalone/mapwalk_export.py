"""Export an offline Map Forge world's visible surface for the MapWalk harness (test-tools/java/.../MapWalk.java).

Usage: python test-tools/standalone/mapwalk_export.py WORLD.json OUT.txt

Only blocks with at least one open face other than the bottom are written, since the walkthrough judges the top layer,
plus whatever holds up a written falling block (gravel, sand, concrete powder): the column under it down to the first
block that does not fall, so nothing drops through the unwritten core when the harness places it.
Line format: 'P index state' defines a palette entry; 'B x y z index' places one block."""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[2] / 'tools'))
from mapforge.world import World  # noqa: E402


AIR = ('minecraft:air', 'minecraft:cave_air', 'minecraft:void_air')


def falls(state):
    name = state.split('[')[0]
    return name in ('minecraft:gravel', 'minecraft:sand', 'minecraft:red_sand', 'minecraft:suspicious_gravel',
                    'minecraft:suspicious_sand') or name.endswith('_concrete_powder')


def export(world_path, out_path):
    blocks = World.load(world_path).blocks

    def solid(p):
        c = blocks.get(p)
        return c is not None and c.state not in AIR

    picked = set()
    for (x, y, z), cell in blocks.items():
        if cell.state in AIR:
            continue
        if all(solid((x + dx, y + dy, z + dz)) for dx, dy, dz in ((0, 1, 0), (1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1))):
            continue
        picked.add((x, y, z))
        if falls(cell.state):
            p = (x, y - 1, z)
            while solid(p) and p not in picked:
                picked.add(p)
                if not falls(blocks[p].state):
                    break
                p = (p[0], p[1] - 1, p[2])
    palette, lines = {}, []
    for (x, y, z) in sorted(picked, key=lambda p: (p[1], p[0], p[2])):
        i = palette.setdefault(blocks[(x, y, z)].state, len(palette))
        lines.append(f'B {x} {y} {z} {i}')
    with open(out_path, 'w', encoding='utf-8', newline='\n') as f:
        for state, i in palette.items():
            f.write(f'P {i} {state}\n')
        f.write('\n'.join(lines) + '\n')
    return len(lines), len(palette)


if __name__ == '__main__':
    n, p = export(sys.argv[1], sys.argv[2])
    print(f'{n} blocks, {p} states')
