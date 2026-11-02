"""Offline world: a sparse map of block cells, saved as JSON. It stands in for a builder-server world so plans can be
checked, applied and rolled back without Minecraft. Absent cells are air."""
import base64
import hashlib
import json
import os
import pathlib

from . import nbt
from .structure import AIR, Structure, normalize

WORLD_FORMAT = 'gg-mapforge-world/1'


class Cell(tuple):
    """(state, block-entity bytes or None); the bytes are uncompressed NBT of the block-entity compound."""
    __slots__ = ()

    def __new__(cls, state, be=None):
        return tuple.__new__(cls, (state, be))

    @property
    def state(self):
        return self[0]

    @property
    def be(self):
        return self[1]

    def to_json(self):
        return [self.state] if self.be is None else [self.state, base64.b64encode(self.be).decode('ascii')]

    @classmethod
    def from_json(cls, data):
        return cls(normalize(data[0]), base64.b64decode(data[1]) if len(data) > 1 and data[1] is not None else None)

    @classmethod
    def from_tag(cls, state, tag):
        return cls(state, nbt.encode(tag) if tag is not None else None)

    def be_tag(self):
        return nbt.decode(self.be)[1] if self.be is not None else None


AIR_CELL = Cell(AIR)


class World:
    def __init__(self, name='offline', blocks=None):
        self.name = name
        self.blocks = {}
        for pos, cell in (blocks or {}).items():
            self.set(pos, cell)

    def get(self, pos):
        return self.blocks.get(pos, AIR_CELL)

    def set(self, pos, cell):
        pos = tuple(pos)
        if cell == AIR_CELL:
            self.blocks.pop(pos, None)
        else:
            self.blocks[pos] = cell

    def copy(self):
        w = World(self.name)
        w.blocks = dict(self.blocks)
        return w

    def digest(self):
        """sha256 over every non-air cell, sorted by position; independent of insertion order."""
        h = hashlib.sha256()
        for pos in sorted(self.blocks):
            cell = self.blocks[pos]
            h.update(('%d,%d,%d|%s|' % (*pos, cell.state)).encode())
            h.update(cell.be or b'-')
            h.update(b'\n')
        return h.hexdigest()

    def region_digest(self, lo, hi):
        """sha256 over the non-air cells inside the inclusive box lo..hi, encoded as digest() encodes them: a section
        digest, so an edit elsewhere in the world leaves it unchanged."""
        return region_digests(self, {'box': (lo, hi)})['box']

    def bounds(self):
        if not self.blocks:
            return None
        xs, ys, zs = zip(*self.blocks)
        return (min(xs), min(ys), min(zs)), (max(xs), max(ys), max(zs))

    # --- JSON file ---
    def to_json(self):
        return {'format': WORLD_FORMAT, 'name': self.name, 'digest': self.digest(),
                'blocks': [[*pos, *self.blocks[pos].to_json()] for pos in sorted(self.blocks)]}

    @classmethod
    def from_json(cls, data):
        if data.get('format') != WORLD_FORMAT:
            raise ValueError(f'world format {data.get("format")!r}, expected {WORLD_FORMAT}')
        w = cls(data.get('name', 'offline'))
        for row in data['blocks']:
            w.set(tuple(row[:3]), Cell.from_json(row[3:]))
        if 'digest' in data and data['digest'] != w.digest():
            raise ValueError('world digest does not match its blocks; the file was edited by hand or is damaged')
        return w

    def save(self, path):
        write_json_atomic(path, self.to_json())

    @classmethod
    def load(cls, path):
        return cls.from_json(json.loads(pathlib.Path(path).read_text(encoding='utf-8')))

    # --- structure files ---
    @classmethod
    def from_structure(cls, struct, origin=(0, 0, 0), name='offline'):
        """A world holding the structure with local (0,0,0) at origin, unrotated."""
        w = cls(name)
        for pos, state, be in struct.cells():
            w.set((origin[0] + pos[0], origin[1] + pos[1], origin[2] + pos[2]), Cell.from_tag(state, be))
        return w

    def export_structure(self, lo, hi, include_air=True):
        """The box lo..hi (inclusive) as a Structure. Air cells are written as air when include_air, so placing the
        file clears; otherwise they are left out and placing keeps what is there."""
        size = tuple(hi[i] - lo[i] + 1 for i in range(3))
        if min(size) <= 0:
            raise ValueError('export box is empty')
        palette, index, blocks = [], {}, []
        for y in range(lo[1], hi[1] + 1):
            for z in range(lo[2], hi[2] + 1):
                for x in range(lo[0], hi[0] + 1):
                    cell = self.get((x, y, z))
                    if cell == AIR_CELL and not include_air:
                        continue
                    if cell.state not in index:
                        index[cell.state] = len(palette)
                        palette.append(cell.state)
                    blocks.append(((x - lo[0], y - lo[1], z - lo[2]), index[cell.state], cell.be_tag()))
        return Structure(size, palette, blocks)


def _line(pos, cell):
    return ('%d,%d,%d|%s|' % (*pos, cell.state)).encode() + (cell.be or b'-') + b'\n'


def region_digests(world, boxes):
    """{key: sha256} for {key: (lo, hi)} inclusive boxes, in one pass over the world (boxes may overlap). Each value
    equals hashing the box's cells alone, sorted by position."""
    hashes = {k: hashlib.sha256() for k in boxes}
    by_column = {}
    items = list(boxes.items())
    for pos in sorted(world.blocks):
        col = (pos[0], pos[2])
        hits = by_column.get(col)
        if hits is None:
            hits = by_column[col] = [(k, lo[1], hi[1]) for k, (lo, hi) in items
                                     if lo[0] <= pos[0] <= hi[0] and lo[2] <= pos[2] <= hi[2]]
        for k, ylo, yhi in hits:
            if ylo <= pos[1] <= yhi:
                hashes[k].update(_line(pos, world.blocks[pos]))
    return {k: h.hexdigest() for k, h in hashes.items()}


def write_json_atomic(path, data):
    path = pathlib.Path(path)
    tmp = path.with_name(path.name + '.tmp')
    tmp.write_text(json.dumps(data, indent=1, sort_keys=False) + '\n', encoding='utf-8')
    os.replace(tmp, path)
