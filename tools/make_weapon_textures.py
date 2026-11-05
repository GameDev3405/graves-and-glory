"""Writes the Graves and Glory weapon textures and models that keep weapons out of Excalibur's look (Kort, 2026-11-05).

Excalibur (CC BY-NC-ND, used unmodified) re-skins the weapon sprites, the entity textures and the block textures our armory 3D weapons sample.
A pack cannot hand Mojang's textures back, so this script paints our own: flat colours with a seeded speckle, in the vanilla layout.
The vanilla 1.20.1 jar is read only as a layout mask (which pixels are used, and a coarse wood / metal / string / light role per pixel);
no vanilla pixel colour is copied. Output is committed; run again only to change the art:  python tools/make_weapon_textures.py [path/to/1.20.1.jar]
"""
import colorsys, io, json, pathlib, random, sys, zipfile
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1] / 'src/main/resources/resourcepacks'
JAR = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path.home() / 'AppData/Roaming/.minecraft/versions/1.20.1/1.20.1.jar'

# own palette: (light, base, dark)
PAL = {
    'wood': ((160, 120, 66), (128, 90, 48), (88, 60, 30)),
    'dark': ((96, 64, 36), (72, 46, 26), (46, 30, 18)),
    'metal': ((214, 216, 220), (170, 174, 182), (110, 114, 124)),
    'string': ((236, 236, 232), (206, 206, 200), (150, 150, 146)),
    'gold': ((252, 238, 110), (234, 190, 48), (170, 120, 24)),
    'stone': ((150, 150, 150), (122, 122, 124), (88, 88, 92)),
    'iron': ((226, 228, 232), (196, 200, 206), (140, 144, 154)),
    'diamond': ((140, 244, 230), (84, 214, 200), (44, 150, 150)),
    'netherite': ((84, 76, 84), (62, 56, 64), (40, 36, 44)),
    'plank': ((190, 150, 90), (160, 120, 66), (118, 84, 44)),
}


def speckle(rng, tone, amount=10):
    return tuple(max(0, min(255, c + rng.randint(-amount, amount))) for c in tone)


def role_of(rgba):
    r, g, b, a = rgba
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    if s < 0.18:
        return 'string' if v > 0.62 else 'metal'
    if h < 0.14 or h > 0.9:
        return 'wood'
    return 'metal'


def paint_from_mask(vanilla, seed, role_map=None):
    """Same used pixels as the vanilla image, our own colours: role by coarse hue, light/dark by edge distance and row."""
    rng = random.Random(seed)
    w, h = vanilla.size
    px = vanilla.load()
    out = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    o = out.load()
    for y in range(h):
        for x in range(w):
            if px[x, y][3] < 40:
                continue
            role = role_of(px[x, y])
            if role_map:
                role = role_map.get(role, role)
            edge = any(not (0 <= x + dx < w and 0 <= y + dy < h) or px[x + dx, y + dy][3] < 40 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            lit = any(0 <= x + dx < w and 0 <= y + dy < h and px[x + dx, y + dy][3] < 40 for dx, dy in ((-1, 0), (0, -1)))
            tone = PAL[role][2] if (edge and not lit) else PAL[role][0] if lit else PAL[role][1]
            o[x, y] = speckle(rng, tone, 6) + (px[x, y][3],)
    return out


def block(seed, name, sheen=0):
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    o = img.load()
    base = PAL[name][1]
    for y in range(16):
        for x in range(16):
            tone = PAL[name][0] if (x + y) % 8 == 0 and sheen else base
            if x in (0, 15) or y in (0, 15):
                tone = PAL[name][2]
            o[x, y] = speckle(rng, tone, 7) + (255,)
    return img


def save(img, rel):
    p = ROOT / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)
    return p


def shield_texture(size, seed, face):
    """64x64 shield layout (plate 26x23 at 0,0; handle at 26,0): wooden face, iron rim, gold boss; patterns tint the face white-ish."""
    rng = random.Random(seed)
    img = Image.new('RGBA', size, (0, 0, 0, 0))
    o = img.load()

    def rect(x0, y0, w, h, tone, amount=6):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                o[x, y] = speckle(rng, tone, amount) + (255,)
    rect(0, 0, 26, 23, PAL['metal'][2])
    for fx in (1, 14):
        rect(fx, 1, 12, 22, face)
        rect(fx, 1, 12, 1, PAL['metal'][1]); rect(fx, 22, 12, 1, PAL['metal'][1])
        rect(fx, 1, 1, 22, PAL['metal'][1]); rect(fx + 11, 1, 1, 22, PAL['metal'][1])
    rect(5, 9, 4, 4, PAL['gold'][1]); rect(18, 9, 4, 4, PAL['gold'][1])
    rect(26, 0, 14, 12, PAL['dark'][1])
    return img


MODELS = {
    'shield': {"parent": "builtin/entity", "gui_light": "front", "textures": {"particle": "block/dark_oak_planks"}, "display": {
        "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [10, 6, -4], "scale": [1, 1, 1]},
        "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [10, 6, 12], "scale": [1, 1, 1]},
        "firstperson_righthand": {"rotation": [0, 180, 5], "translation": [-10, 2, -10], "scale": [1.25, 1.25, 1.25]},
        "firstperson_lefthand": {"rotation": [0, 180, 5], "translation": [10, 0, -10], "scale": [1.25, 1.25, 1.25]},
        "gui": {"rotation": [15, -25, -5], "translation": [2, 3, 0], "scale": [0.65, 0.65, 0.65]},
        "fixed": {"rotation": [0, 180, 0], "translation": [-4.5, 4.5, -5], "scale": [0.55, 0.55, 0.55]},
        "ground": {"rotation": [0, 0, 0], "translation": [2, 4, 2], "scale": [0.25, 0.25, 0.25]}},
        "overrides": [{"predicate": {"blocking": 1}, "model": "item/shield_blocking"}]},
    'shield_blocking': {"parent": "builtin/entity", "gui_light": "front", "textures": {"particle": "block/dark_oak_planks"}, "display": {
        "thirdperson_righthand": {"rotation": [45, 135, 0], "translation": [3.51, 11, -2], "scale": [1, 1, 1]},
        "thirdperson_lefthand": {"rotation": [45, 135, 0], "translation": [13.51, 3, 5], "scale": [1, 1, 1]},
        "firstperson_righthand": {"rotation": [0, 180, -5], "translation": [-15, 5, -11], "scale": [1.25, 1.25, 1.25]},
        "firstperson_lefthand": {"rotation": [0, 180, -5], "translation": [5, 5, -11], "scale": [1.25, 1.25, 1.25]},
        "gui": {"rotation": [15, -25, -5], "translation": [2, 3, 0], "scale": [0.65, 0.65, 0.65]}}},
    'arrow': {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/arrow"}},
    'spectral_arrow': {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/spectral_arrow"}},
    'tipped_arrow': {"parent": "item/generated", "textures": {"layer0": "item/tipped_arrow_head", "layer1": "item/tipped_arrow_base"}},
}

# vanilla path -> (output path in pack, seed, role remap)
MASKED = {}
for n in ('bow', 'bow_pulling_0', 'bow_pulling_1', 'bow_pulling_2', 'crossbow_standby', 'crossbow_pulling_0', 'crossbow_pulling_1',
          'crossbow_pulling_2', 'crossbow_arrow', 'crossbow_firework', 'trident', 'arrow', 'spectral_arrow', 'tipped_arrow_base', 'tipped_arrow_head'):
    MASKED['textures/item/%s.png' % n] = {}
for n in ('trident', 'projectiles/arrow', 'projectiles/spectral_arrow', 'projectiles/tipped_arrow'):
    MASKED['textures/entity/%s.png' % n] = {}


def main():
    armory = 'armory/assets/gravesandglory/textures/item/armory/%s.png'
    for i, name in enumerate(('wood', 'stone', 'iron', 'gold', 'diamond', 'netherite', 'dark')):
        pal = {'wood': 'plank', 'dark': 'dark'}.get(name, name)
        save(block(100 + i, pal, sheen=name in ('iron', 'gold', 'diamond', 'netherite')), armory % name)
    save(block(200, 'gold', sheen=1), armory % 'trim')
    with zipfile.ZipFile(JAR) as jar:
        for rel in MASKED:
            vanilla = Image.open(io.BytesIO(jar.read('assets/minecraft/' + rel))).convert('RGBA')
            out = paint_from_mask(vanilla, seed=sum(map(ord, rel)))
            save(out, 'weapons/assets/minecraft/' + rel)
    for n in ('shield_base', 'shield_base_nopattern'):
        save(shield_texture((64, 64), 7, PAL['plank'][1] if n.endswith('nopattern') else (225, 225, 225)), 'weapons/assets/minecraft/textures/entity/%s.png' % n)
    save(shield_texture((64, 64), 8, PAL['plank'][1]), 'weapons/assets/minecraft/textures/entity/shield/base.png')
    for n, m in MODELS.items():
        p = ROOT / ('weapons/assets/minecraft/models/item/%s.json' % n)
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(json.dumps(m, indent=2) + '\n', encoding='utf8')
    # armory models sample our own textures, not minecraft:block/*, which Excalibur re-skins
    mapping = {'minecraft:block/dark_oak_planks': 'gravesandglory:item/armory/dark', 'minecraft:block/gold_block': 'gravesandglory:item/armory/trim',
               'minecraft:block/iron_block': 'gravesandglory:item/armory/iron', 'minecraft:block/diamond_block': 'gravesandglory:item/armory/diamond',
               'minecraft:block/netherite_block': 'gravesandglory:item/armory/netherite', 'minecraft:block/stone': 'gravesandglory:item/armory/stone',
               'minecraft:block/oak_planks': 'gravesandglory:item/armory/wood'}
    for f in (ROOT / 'armory/assets/minecraft/models/item').glob('*.json'):
        d = json.loads(f.read_text(encoding='utf8'))
        d['textures'] = {k: mapping.get(v, v) for k, v in d['textures'].items()}
        f.write_text(json.dumps(d, indent=2) + '\n', encoding='utf8')


if __name__ == '__main__':
    main()
