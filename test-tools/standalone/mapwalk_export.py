"""Export an offline Map Forge world's visible surface for the MapWalk harness (test-tools/java/.../MapWalk.java).

Usage: python test-tools/standalone/mapwalk_export.py WORLD.json OUT.txt

Only blocks with at least one open face other than the bottom are written, since the walkthrough judges the top layer.
Line format: 'P index state' defines a palette entry; 'B x y z index' places one block."""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[2] / 'tools'))
from mapforge.world import World  # noqa: E402


def export(world_path, out_path):
    blocks = World.load(world_path).blocks
    palette, lines = {}, []
    for (x, y, z), cell in blocks.items():
        if cell.state in ('minecraft:air', 'minecraft:cave_air'):
            continue
        if all((x + dx, y + dy, z + dz) in blocks for dx, dy, dz in ((0, 1, 0), (1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1))):
            continue
        i = palette.setdefault(cell.state, len(palette))
        lines.append(f'B {x} {y} {z} {i}')
    with open(out_path, 'w', encoding='utf-8', newline='\n') as f:
        for state, i in palette.items():
            f.write(f'P {i} {state}\n')
        f.write('\n'.join(lines) + '\n')
    return len(lines), len(palette)


if __name__ == '__main__':
    n, p = export(sys.argv[1], sys.argv[2])
    print(f'{n} blocks, {p} states')
