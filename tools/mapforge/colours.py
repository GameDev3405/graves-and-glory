"""Block colours and shapes for `render`. The table colours.json is generated from the Minecraft 1.20.1 client jar
(the one Loom caches for build.gradle): each block's first model is resolved through its parents, and the top and side
colours are the alpha-weighted average of the textures on its up and south/east faces, biome-tinted where the model
asks for a tint (plains grass and foliage, default water). `box` is the union of the model's elements in 1/16 block
units. Only average colours are kept, no texture data. Regenerate with `python tools/mapforge colours --jar JAR`.

Reading the table needs only the stdlib; generating it needs Pillow."""
import json
import pathlib
import zipfile

TABLE = pathlib.Path(__file__).with_name('colours.json')
TABLE_FORMAT = 'gg-mapforge-colours/1'
INVISIBLE = frozenset({'minecraft:air', 'minecraft:cave_air', 'minecraft:void_air', 'minecraft:structure_void',
                       'minecraft:barrier', 'minecraft:light'})
UNKNOWN = (255, 0, 255)  # magenta: a block the table does not name
FULL = (0, 0, 0, 16, 16, 16)

# Default biome tints (plains grass and foliage, the default water colour; fixed spruce and birch leaf colours).
GRASS, FOLIAGE, WATER = (0x91, 0xBD, 0x59), (0x77, 0xAB, 0x2F), (0x3F, 0x76, 0xE4)
_FIXED_TINT = {'spruce_leaves': (0x61, 0x99, 0x61), 'birch_leaves': (0x80, 0xA7, 0x55), 'lily_pad': (0x20, 0x80, 0x30),
               'redstone_wire': (0xB4, 0x00, 0x00), 'attached_melon_stem': (0xE0, 0xC7, 0x1C),
               'attached_pumpkin_stem': (0xE0, 0xC7, 0x1C), 'melon_stem': (0x7F, 0xBF, 0x1F),
               'pumpkin_stem': (0x7F, 0xBF, 0x1F), 'water': WATER, 'water_cauldron': WATER, 'bubble_column': WATER}
_GRASS_TINTED = {'grass_block', 'grass', 'tall_grass', 'fern', 'large_fern', 'potted_fern', 'sugar_cane'}
_DYES = ('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple',
         'blue', 'brown', 'green', 'red', 'black')
# Blocks drawn by a block-entity renderer have no model elements; their shapes, in 1/16 block units.
_ENTITY_BOX = {'chest': (1, 0, 1, 15, 14, 15), 'trapped_chest': (1, 0, 1, 15, 14, 15), 'ender_chest': (1, 0, 1, 15, 14, 15),
               'bed': (0, 0, 0, 16, 9, 16), 'banner': (7, 0, 7, 9, 16, 9), 'wall_banner': (1, 0, 14, 15, 16, 16),
               'sign': (7, 0, 7, 9, 16, 9), 'wall_sign': (1, 4, 14, 15, 12, 16), 'hanging_sign': (1, 0, 7, 15, 10, 9),
               'wall_hanging_sign': (1, 0, 7, 15, 10, 9), 'skull': (4, 0, 4, 12, 8, 12), 'head': (4, 0, 4, 12, 8, 12),
               'wall_skull': (4, 4, 8, 12, 12, 16), 'wall_head': (4, 4, 8, 12, 12, 16), 'bell': (4, 0, 4, 12, 16, 12),
               'decorated_pot': (1, 0, 1, 15, 16, 15), 'conduit': (5, 5, 5, 11, 11, 11)}

# Models whose element union misreads the shape (torch planes span the whole cell; rail planes are rotated).
_BOX_OVERRIDE = {'torch': (7, 0, 7, 9, 10, 9), 'soul_torch': (7, 0, 7, 9, 10, 9), 'redstone_torch': (7, 0, 7, 9, 10, 9),
                 'wall_torch': (7, 3, 11, 9, 13, 16), 'soul_wall_torch': (7, 3, 11, 9, 13, 16),
                 'redstone_wall_torch': (7, 3, 11, 9, 13, 16), 'rail': (0, 0, 0, 16, 1, 16),
                 'powered_rail': (0, 0, 0, 16, 1, 16), 'detector_rail': (0, 0, 0, 16, 1, 16),
                 'activator_rail': (0, 0, 0, 16, 1, 16)}


def tint_for(name):
    short = name.split(':', 1)[1]
    if short in _FIXED_TINT:
        return _FIXED_TINT[short]
    if short in _GRASS_TINTED:
        return GRASS
    return FOLIAGE


def entity_box(short):
    for suffix in sorted(_ENTITY_BOX, key=len, reverse=True):
        if short == suffix or short.endswith('_' + suffix):
            return _ENTITY_BOX[suffix]
    return None


def _hex(rgb):
    return '#%02x%02x%02x' % tuple(rgb)


def _rgb(text):
    return tuple(int(text[i:i + 2], 16) for i in (1, 3, 5))


class Palette:
    """Lookup of a block name to (top rgb, side rgb, box). Unknown names give magenta and are remembered."""

    def __init__(self, data=None):
        data = data if data is not None else json.loads(TABLE.read_text(encoding='utf-8'))
        if data.get('format') != TABLE_FORMAT:
            raise ValueError(f'colour table format {data.get("format")!r}, expected {TABLE_FORMAT}')
        self.source = data.get('source', '')
        self.blocks = {name: (_rgb(e[0]), _rgb(e[1]), tuple(e[2])) for name, e in data['blocks'].items()}
        self.unknown = {}

    def __contains__(self, name):
        return name in self.blocks

    def get(self, name):
        entry = self.blocks.get(name)
        if entry is None:
            self.unknown[name] = self.unknown.get(name, 0) + 1
            box = entity_box(name.split(':', 1)[-1]) or FULL
            return UNKNOWN, UNKNOWN, box
        return entry


# --- generation from the client jar (Pillow) ---

class _Jar:
    def __init__(self, path):
        self.zip = zipfile.ZipFile(path)
        self.names = set(self.zip.namelist())
        self._models, self._tex = {}, {}

    def json(self, path):
        return json.loads(self.zip.read(path))

    def model(self, ref):
        """(textures, elements) with the parent chain applied."""
        ref = ref.split(':', 1)[-1]
        if ref not in self._models:
            path = f'assets/minecraft/models/{ref}.json'
            data = self.json(path) if path in self.names else {}
            textures, elements = {}, None
            if 'parent' in data and not data['parent'].startswith('builtin/'):
                textures, elements = self.model(data['parent'])
                textures = dict(textures)
            textures.update(data.get('textures', {}))
            self._models[ref] = (textures, data.get('elements', elements))
        return self._models[ref]

    def texture(self, ref):
        """RGBA image of a texture reference (first frame of an animated strip), or None."""
        from PIL import Image
        ref = ref.split(':', 1)[-1]
        if ref not in self._tex:
            path = f'assets/minecraft/textures/{ref}.png'
            img = None
            if path in self.names:
                import io
                img = Image.open(io.BytesIO(self.zip.read(path))).convert('RGBA')
                if img.height > img.width:
                    img = img.crop((0, 0, img.width, img.width))
            self._tex[ref] = img
        return self._tex[ref]


def _resolve(textures, key, depth=0):
    value = textures.get(key.lstrip('#'))
    if value is None or depth > 8:
        return None
    return _resolve(textures, value, depth + 1) if value.startswith('#') else value


def _first_model(state):
    if 'variants' in state:
        v = next(iter(state['variants'].values()))
        return (v[0] if isinstance(v, list) else v)['model']
    parts = state.get('multipart', [])
    part = next((p for p in parts if 'when' not in p), parts[0] if parts else None)
    if part is None:
        return None
    a = part['apply']
    return (a[0] if isinstance(a, list) else a)['model']


def _face_colour(jar, textures, faces, tint):
    """Alpha-weighted mean colour of the given element faces; faces sharing a box are composited in order, so an
    overlay (the grass block side) lands on its base."""
    from PIL import Image
    layers = {}
    for box, face in faces:
        img = _resolve(textures, face.get('texture', ''))
        img = jar.texture(img) if img else None
        if img is None:
            continue
        uv = face.get('uv')
        if uv:
            s = img.width / 16
            x0, x1 = sorted((uv[0], uv[2]))
            y0, y1 = sorted((uv[1], uv[3]))
            if x1 > x0 and y1 > y0:
                img = img.crop((int(x0 * s), int(y0 * s), max(int(x1 * s), int(x0 * s) + 1), max(int(y1 * s), int(y0 * s) + 1)))
        img = img.resize((16, 16), Image.NEAREST)
        if 'tintindex' in face:
            r, g, b, a = img.split()
            r = r.point(lambda v: v * tint[0] // 255)
            g = g.point(lambda v: v * tint[1] // 255)
            b = b.point(lambda v: v * tint[2] // 255)
            img = Image.merge('RGBA', (r, g, b, a))
        if box in layers:
            layers[box] = Image.alpha_composite(layers[box], img)
        else:
            layers[box] = img
    tot = [0.0, 0.0, 0.0]
    weight = 0.0
    for box, img in layers.items():
        area = max(1, (box[3] - box[0]) * (box[5] - box[2]) + (box[4] - box[1]) * 16)
        for r, g, b, a in img.getdata():
            w = a / 255 * area
            tot[0] += r * w
            tot[1] += g * w
            tot[2] += b * w
            weight += w
    if weight == 0:
        return None
    return tuple(round(c / weight) for c in tot)


def block_entry(jar, short):
    state = jar.json(f'assets/minecraft/blockstates/{short}.json')
    model = _first_model(state)
    textures, elements = jar.model(model) if model else ({}, None)
    name = 'minecraft:' + short
    tint = tint_for(name)
    for dye in _DYES:  # banners and beds are drawn by the entity renderer in their dye colour
        if short.startswith(dye + '_') and short.endswith(('_banner', '_bed')):
            textures, elements = {'all': f'block/{dye}_wool'}, None
    up, side = [], []
    if elements:
        for el in elements:
            box = tuple(el['from']) + tuple(el['to'])
            for direction, face in el.get('faces', {}).items():
                if direction == 'up':
                    up.append((box, face))
                elif direction in ('south', 'east'):
                    side.append((box, face))
    tinted = short in _FIXED_TINT or any('tintindex' in f for _, f in up + side)
    fallback = []
    for key in ('all', 'particle', 'texture'):
        if key in textures:
            fallback = [(FULL, {'texture': '#' + key, **({'tintindex': 0} if tinted else {})})]
            break
    sid = _face_colour(jar, textures, side, tint)
    top = _face_colour(jar, textures, up, tint) or sid or _face_colour(jar, textures, fallback, tint)
    sid = sid or top
    if top is None:
        return None
    if elements:
        solid = [e for e in elements if all(e['to'][i] > e['from'][i] for i in range(3))]
        lo = [min(e['from'][i] for e in solid or elements) for i in range(3)]
        hi = [max(e['to'][i] for e in solid or elements) for i in range(3)]
        box = [max(0, min(16, round(v))) for v in (*lo, *hi)]
        if not solid:  # crossed or flat planes (plants, rails, vines): a thin centred post of the same height
            box = [4, box[1], 4, 12, max(box[4], box[1] + 1), 12]
        box = tuple(box)
    else:
        box = entity_box(short) or FULL
    box = _BOX_OVERRIDE.get(short, box)
    return _hex(top), _hex(sid or top), list(box)


def generate(jar_path, out=TABLE):
    jar = _Jar(jar_path)
    prefix = 'assets/minecraft/blockstates/'
    blocks, missing = {}, []
    for path in sorted(n for n in jar.names if n.startswith(prefix) and n.endswith('.json')):
        short = path[len(prefix):-5]
        name = 'minecraft:' + short
        if name in INVISIBLE:
            continue
        entry = block_entry(jar, short)
        if entry is None:
            missing.append(name)
        else:
            blocks[name] = entry
    data = {'format': TABLE_FORMAT, 'source': f'average texture colours from {pathlib.Path(jar_path).name} (Minecraft 1.20.1)',
            'blocks': blocks}
    text = '{\n "format": %s,\n "source": %s,\n "blocks": {\n' % (json.dumps(data['format']), json.dumps(data['source']))
    text += ',\n'.join(f'  {json.dumps(k)}: {json.dumps(v)}' for k, v in blocks.items())
    text += '\n }\n}\n'
    pathlib.Path(out).write_text(text, encoding='utf-8')
    return len(blocks), missing
