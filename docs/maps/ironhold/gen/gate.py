"""Ironhold I1, the main gatehouse, in map coordinates: wall centre x -72, passage z -4..4, towers z -13..-5 and 5..13.
Ground climbs from 64 (x <= -88) to 70 (x >= -76) on the glacis; the ward inside is at 70. Curtain stubs run to z +-30.
build(b) adds it to a Build. Usage: python gate.py OUT.json"""
import sys, os
sys.path.append(os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'kingsfield', 'gen')))
from kit import Build

DEEP = [('deepslate_bricks', 52), ('cracked_deepslate_bricks', 12), ('deepslate_tiles', 20), ('cobbled_deepslate', 16)]
FOOT = [('polished_blackstone_bricks', 50), ('blackstone', 30), ('cracked_polished_blackstone_bricks', 20)]
ROAD = [('gravel', 42), ('cobblestone', 30), ('cobbled_deepslate', 23), ('coarse_dirt', 5)]
GLACIS = [('cobbled_deepslate', 36), ('gravel', 30), ('tuff', 28), ('coarse_dirt', 6)]
WARD = [('cobbled_deepslate', 40), ('cobblestone', 30), ('gravel', 20), ('tuff', 10)]
WX = -72                                                    # wall centre line
W0, W1 = WX - 3, WX + 3                                     # curtain faces: west (field) x -75, east (ward) x -69
T0, T1 = WX - 6, WX + 6                                     # towers x -78..-66
WALK, TOP = 80, 88                                          # curtain wall-walk y, tower roof y


def ground(x): return 64 + max(0, min(6, (x + 88) // 2))


def build(b, flag='purple'):
    S = lambda f, top=False, shape='straight': 'deepslate_brick_stairs[facing=%s,half=%s,shape=%s]' % (f, 'top' if top else 'bottom', shape)
    # the curtain stubs: a battered foot, a string course, a walk with merlons to the field and a low wall to the ward
    for z in list(range(-30, -13)) + list(range(14, 31)):
        for x in range(W0, W1 + 1):
            for y in range(ground(x) + 1, WALK + 1):
                b.set(x, y, z, b.pick(FOOT) if y <= ground(x) + 2 else 'polished_deepslate' if y == WALK - 3 and x in (W0, W1) else b.pick(DEEP))
        b.set(W0 - 1, ground(W0 - 1) + 1, z, b.pick(FOOT)); b.set(W0 - 1, ground(W0 - 1) + 2, z, 'polished_blackstone_brick_stairs[facing=east,half=bottom,shape=straight]')
        b.set(W0 - 1, WALK, z, S('east', True)); b.set(W0 - 1, WALK + 1, z, b.pick(DEEP))                      # corbelled parapet
        if z % 2 == 0: b.set(W0 - 1, WALK + 2, z, b.pick(DEEP))
        if z % 4 == 0: b.set(W0 - 1, WALK + 3, z, 'iron_bars') if z % 8 == 0 else None
        b.set(W1, WALK + 1, z, 'deepslate_brick_wall')
        if abs(z) in (18, 24, 30):                                                                              # buttresses on the field face
            for y in range(ground(W0 - 1) + 1, WALK - 3): b.set(W0 - 1, y, z, b.pick(FOOT) if y <= ground(W0 - 1) + 3 else b.pick(DEEP))
            b.set(W0 - 1, WALK - 3, z, S('east')); b.set(W0 - 2, ground(W0 - 2) + 1, z, b.pick(FOOT)); b.set(W0 - 2, ground(W0 - 2) + 2, z, b.pick(FOOT)); b.set(W0 - 2, ground(W0 - 2) + 3, z, 'polished_blackstone_brick_stairs[facing=east,half=bottom,shape=straight]')
        if abs(z) in (21, 27): b.set(W0, WALK - 5, z, 'iron_bars'); b.set(W0, WALK - 6, z, 'iron_bars')            # loops
    # the two towers: hollow, chamfered corners, a floor at the walk level, a corbelled fighting top
    for s in (-1, 1):
        za, zb = (-13, -5) if s < 0 else (5, 13)
        for x in range(T0, T1 + 1):
            for z in range(za, zb + 1):
                edge = x in (T0, T1) or z in (za, zb); corner = x in (T0, T1) and z in (za, zb)
                for y in range(ground(x) + 1, TOP + 1):
                    if corner and y > ground(x) + 3: continue
                    if edge: b.set(x, y, z, b.pick(FOOT) if y <= ground(x) + 3 else 'polished_deepslate' if y in (WALK - 3, TOP - 1) else b.pick(DEEP))
                    elif y in (WALK, TOP): b.set(x, y, z, 'dark_oak_planks' if y == WALK else b.pick(DEEP))
                    elif y <= 70: b.set(x, y, z, b.pick(DEEP))
                    else: b.set(x, y, z, 'air')
        for x in range(T0 - 1, T1 + 2):                                                                           # the top: one out on corbels
            for z in range(za - 1, zb + 2):
                ring = x in (T0 - 1, T1 + 1) or z in (za - 1, zb + 1)
                if not ring: continue
                corner = x in (T0 - 1, T1 + 1) and z in (za - 1, zb + 1)
                if z == (zb + 1 if s < 0 else za - 1) and W0 <= x <= W1: continue                                # the side that faces the gate block
                f = 'east' if x == T0 - 1 else 'west' if x == T1 + 1 else 'south' if z == za - 1 else 'north'
                b.set(x, TOP, z, 'polished_deepslate' if corner else S(f, True)); b.set(x, TOP + 1, z, b.pick(DEEP))
                if (x + z) % 2 == 0 or corner: b.set(x, TOP + 2, z, b.pick(DEEP))
                if corner: b.set(x, TOP + 3, z, 'deepslate_brick_wall'); b.set(x, TOP + 4, z, 'lightning_rod[facing=up]')
        zc = (za + zb) // 2
        for y in (73, 74, 78, 79, 84, 85): b.set(T0, y, zc, 'iron_bars')                                          # loops to the field
        for y in (78, 79): b.set(T0, y, zc - 2 * s, 'iron_bars')
        for x in (WX - 2, WX + 2):
            for y in (84, 85): b.set(x, y, za if s < 0 else zb, 'iron_bars')
        b.set(T1, 71, zc, 'air'); b.set(T1, 72, zc, 'air'); b.set(T1, 73, zc, S('west', True))                    # door from the ward
        b.set(T1 + 1, 72, zc - 1, 'lantern[hanging=false]') if False else None
        for y in range(71, TOP + 1): b.set(T0 + 1, y, zc + 2 * s, 'ladder[facing=east]')
        b.set(T0 + 1, WALK, zc + 2 * s, 'ladder[facing=east]'); b.set(T0 + 1, TOP, zc + 2 * s, 'ladder[facing=east]')
        for y in (WALK + 1, WALK + 2): b.set(WX, y, zb if s < 0 else za, 'air') if False else None
        b.set(WX, TOP - 3, zc, 'lantern[hanging=true]'); b.set(WX, TOP - 2, zc, 'chain[axis=y]'); b.set(WX, TOP - 1, zc, 'chain[axis=y]')
        for dz in (0,):                                                                                         # a door from each tower onto the curtain walk
            zz = za if s < 0 else zb
            for y in (WALK + 1, WALK + 2): b.set(WX, y, zz, 'air')
        px, pz = WX, zc                                                                                         # the standard
        for y in range(TOP + 1, TOP + 9): b.set(px, y, pz, 'dark_oak_fence')
        b.set(px, TOP + 1, pz, 'polished_blackstone')
        for i in range(1, 5):
            for y in (TOP + 6, TOP + 7, TOP + 8):
                if not (i == 4 and y == TOP + 7): b.set(px + i, y, pz, 'black_wool' if y == TOP + 7 else flag + '_wool')
    # the gate block between the towers: a vaulted passage 9 wide and 6 high, a chamber over it, a hoarding to the field
    for x in range(W0 - 1, W1 + 2):
        for z in range(-4, 5):
            for y in range(71, 77): b.set(x, y, z, 'air')
            for y in range(77, 84): b.set(x, y, z, 'polished_deepslate' if y in (77, 83) and x in (W0 - 1, W1 + 1) else b.pick(DEEP))
            b.set(x, 70, z, b.pick(ROAD))
        for z, f in ((-4, 'south'), (4, 'north')):
            b.set(x, 76, z, S(f, True)); b.set(x, 75, z, b.pick(DEEP)) if x in (W0 - 1, W1 + 1) else None
        for z in (-3, 3): b.set(x, 76, z, S('south' if z < 0 else 'north', True)) if x in (W0 - 1, W1 + 1) else None
    for x in (W0 - 1, W1 + 1):                                                                                   # arch rings on both faces
        for z in (-4, 4):
            for y in range(71, 76): b.set(x, y, z, 'polished_blackstone_bricks' if y % 2 else 'chiseled_polished_blackstone')
    for z in range(-4, 5):                                                                                      # the portcullis, drawn up, and its chains
        for y in (75, 76): b.set(W0 + 1, y, z, 'iron_bars[north=true,south=true]')
        b.set(W0 + 1, 74, z, 'iron_bars[north=true,south=true]') if z % 2 == 0 else None
    for z in (-2, 2):
        for x in (WX - 1, WX + 2): b.set(x, 76, z, 'chain[axis=y]'); b.set(x, 75, z, 'lantern[hanging=true]')
    for z in range(-5, 6):                                                                                      # hoarding: a timber gallery on corbels over the gate
        b.set(W0 - 2, 81, z, 'dark_oak_stairs[facing=east,half=top,shape=straight]'); b.set(W0 - 2, 82, z, 'dark_oak_planks' if z % 3 else 'air'); b.set(W0 - 2, 83, z, 'dark_oak_planks')
        b.set(W0 - 2, 84, z, 'dark_oak_slab[type=bottom]'); b.set(W0 - 1, 84, z, 'dark_oak_slab[type=top]')
    for z in range(-4, 5):                                                                                      # roof walk and parapets of the gate block
        for x in (W0 - 1, W1 + 1): b.set(x, 84, z, b.pick(DEEP)) if x == W1 + 1 else None
        if z % 2 == 0: b.set(W1 + 1, 85, z, b.pick(DEEP))
    for z in (-2, 2):                                                                                           # banners and an iron crest to the field
        b.set(W0 - 2, 79, z, flag + '_wall_banner[facing=west]')
        b.set(W1 + 2, 80, z, flag + '_wall_banner[facing=east]')
    b.set(W0 - 2, 78, 0, 'chiseled_polished_blackstone') if False else None
    for z in (-1, 0, 1): b.set(W0 - 1, 79, z, 'chiseled_deepslate'); b.set(W0 - 1, 80, z, 'iron_bars[north=true,south=true]')
    for z in (-6, 6):                                                                                           # braziers in iron cages either side of the gate, clear of the road
        b.set(W0 - 5, ground(W0 - 5) + 1, z, 'polished_blackstone_brick_wall'); b.set(W0 - 5, ground(W0 - 5) + 2, z, 'polished_blackstone'); b.set(W0 - 5, ground(W0 - 5) + 3, z, 'lantern[hanging=false]')
        b.set(W1 + 5, 71, z, 'polished_blackstone_brick_wall'); b.set(W1 + 5, 72, z, 'polished_blackstone'); b.set(W1 + 5, 73, z, 'lantern[hanging=false]')


def site(b):
    for x in range(-98, -51):
        for z in range(-36, 37):
            g = ground(x)
            for y in range(60, g): b.set(x, y, z, 'stone')
            b.set(x, g, z, b.pick(ROAD) if abs(z) <= 4 else b.pick(GLACIS) if x < W0 else b.pick(WARD))
            for y in range(g + 1, 72): b.set(x, y, z, 'air')


if __name__ == '__main__':
    b = Build(seed=91)
    site(b); build(b)
    if len(sys.argv) > 2:                                   # picture of the field face: the whole build turned about x
        t = {'facing=east': 'facing=west', 'facing=west': 'facing=east'}
        b.c = {(-149 - x, y, z): (v.replace('facing=east', '#').replace('facing=west', 'facing=east').replace('#', 'facing=west')) for (x, y, z), v in b.c.items()}
    b.box(-90, 65, 0, -90, 66, 0, 'red_wool'); b.box(-60, 71, 2, -60, 72, 2, 'red_wool')
    print(b.emit(sys.argv[1], 'ih-i1-gate', 'I1 main gatehouse, workshop build', pad=((-100, 60, -38), (-50, 110, 38))), 'ops')
    if len(sys.argv) == 2:                                  # export boxes (each side under 48): name x0 y0 z0 x1 y1 z1
        print('ih_gate_n -98 60 -36 -52 100 0'); print('ih_gate_s -98 60 1 -52 100 36')
