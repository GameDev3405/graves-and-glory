"""Blocks for the title foreground, authored at GUI scale 1 (one pixel here is one GUI pixel).

Three things live here:

- TEX: every block face is a hand-typed 16 x 16 grid of tone letters, drawn the way the game's own textures are
  drawn (per-pixel noise in four or five tones, stones and pebbles with a lit top-left edge). They are our own
  drawings in the game's manner, not copies of the game's files.
- World: a block grid. The scene scripts place every block by hand (typed rows, typed plans, typed lists).
- trace() and friends: draw a world the way the game would at this size: one-point perspective from a camera above
  and to the right of the hill (so every block shows its top, its front and its right side), nearest-texel
  sampling, and light worked out once per texel (pixel-locked): sky light with the game's corner shading (ambient
  occlusion), the low sun with cast shadows, and warm block light from lanterns.

A rendered pixel is a code: the texture tone it shows plus its light level. Day and night are two palettes over the
same codes, so night stays a palette swap of one drawing."""
import numpy as np

# ================================================================================================ tones (unlit)
# the albedo of each tone, before light: sRGB hex
ALBEDO = {
    # stone, cobblestone, andesite
    "s0": "#5a5a5c", "s1": "#69696b", "s2": "#77777a", "s3": "#838386", "s4": "#949497",
    "c0": "#38383a", "c1": "#4f4f52", "c2": "#646467", "c3": "#79797c", "c4": "#919194",
    "a0": "#5f5f5f", "a1": "#727273", "a2": "#808081", "a3": "#8c8c8c", "a4": "#a3a3a2",
    # dirt (and coarse dirt), grass (biome-tinted toward the savanna's olive), gravel (v5: a brown flint)
    "d0": "#4b3222", "d1": "#61412e", "d2": "#775237", "d3": "#886143", "d4": "#9b7250",
    "g0": "#3b561c", "g1": "#4a6823", "g2": "#59792a", "g3": "#688b32", "g4": "#7d9f3d",
    "v0": "#4a4644", "v1": "#625c5a", "v2": "#787270", "v3": "#8e8783", "v4": "#a59d98", "v5": "#76604f",
    # dirt path
    "p0": "#6a5230", "p1": "#7d6339", "p2": "#907442", "p3": "#a1834c", "p4": "#b39459",
    # oak bark, oak rings, stripped oak, oak planks
    "b0": "#2e2315", "b1": "#41311d", "b2": "#544128", "b3": "#675131",
    "r1": "#8b6c40", "r2": "#a1814d", "r3": "#b5965c",
    "w0": "#7f6137", "w1": "#977745", "w2": "#ab8a52", "w3": "#bd9c60",
    "k0": "#57422a", "k1": "#775c36", "k2": "#8f7040", "k3": "#a1804a",
    # wool: white, and the Dawn's teal (the mod's own Dawn teal, #2b687c, as wool)
    "n0": "#a6a6a1", "n1": "#c1c1bb", "n2": "#d5d5cf", "n3": "#e6e6e0",
    "t0": "#1a4a58", "t1": "#215c6c", "t2": "#2b687c", "t3": "#37798e",
    # allium purple (the hill's heather, as the game's allium)
    "l0": "#4a2659", "l1": "#6b3a82", "l2": "#8d52a6", "l3": "#b07ac8",
    # brass and gold (the shield's sun, the finial)
    "e0": "#6e5226", "e1": "#a8843f", "e2": "#d8b664", "e3": "#f6e3a4",
    # iron (lanterns, the spearhead), and flame
    "i0": "#3a3a40", "i1": "#5b5b63", "i2": "#8a8a93", "i3": "#c6c6cc",
    "f0": "#c45a1c", "f1": "#f39235", "f2": "#ffd27a", "f3": "#fff1c4",
    # the commander: skin, eyes, trousers, and the diamond of his sword
    "h0": "#6f4632", "h1": "#946049", "h2": "#b07c63", "h3": "#c89a7e",
    "u0": "#2e2b66", "u1": "#4a4795",
    "j0": "#1f2556", "j1": "#2a3370", "j2": "#35408a", "j3": "#45519e",
    "q0": "#0b2f2c", "q1": "#178273", "q2": "#35d0b8", "q3": "#86f2dd", "q4": "#e2fff8",
}

# ================================================================================================ textures
# Each texture: 16 rows of 16 tone letters (top row first, as the face is seen upright), and the key that turns a
# letter into a tone. '.' is transparent.
TEX = {}


def tex(name, key, rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
    TEX[name] = (rows, key)


def digits(prefix, n=6):
    return {str(i): prefix + str(i) for i in range(n)}


tex("stone", digits("s"), [
    "2223222222122222",
    "2222221102222322",
    "1122222222223422",
    "2222322222222222",
    "2234322212222221",
    "2222222211022222",
    "2212222222223222",
    "2102223222222222",
    "2222222222112222",
    "2232222212222232",
    "2222122222223432",
    "2222110223222222",
    "3222222222222112",
    "2222232221222222",
    "2122222211222322",
    "2222342222222222",
])

# cobblestone: a hand-typed map of stones (letters) and mortar ('.' dark, ',' lighter); each stone has a typed tone,
# and is lit on its top-left edge and shaded on its bottom-right edge (the only rule applied)
COBBLE_MAP = [
    "AAAA..BBBBB.CCCC",
    "AAAAA.BBBBB.CCCC",
    "AAAA,.BBBB,.CCC.",
    ".AA.......DD....",
    "....EEEE.DDDD.FF",
    "GGG.EEEEE.DDD.FF",
    "GGGG.EEEE.DD.FFF",
    "GGGG,.EE.,...FFF",
    "GGG..HHHH.IIII..",
    ".G..HHHHH.IIIII.",
    "....HHHHH,.IIII.",
    "JJJ..HHH.K..II..",
    "JJJJ....KKK....L",
    "JJJJ.MMM.KK.LLLL",
    "JJJ.MMMMM,..LLLL",
    "...,MMMM..AA..LL",
]
COBBLE_TONE = dict(A=3, B=2, C=3, D=2, E=3, F=2, G=2, H=3, I=2, J=3, K=2, L=3, M=2)


def stones(m, tone, mortar, lo=1, hi=4):
    """a stone-and-mortar map to tone digits: each stone its typed tone, +1 where it meets mortar above or to the
    left (the lit edge), -1 where it meets mortar below or to the right"""
    out = []
    for y in range(16):
        row = ""
        for x in range(16):
            ch = m[y][x]
            if ch in ".,":
                row += mortar[ch]
                continue
            t = tone[ch]
            up, left = m[(y - 1) % 16][x], m[y][(x - 1) % 16]
            down, right = m[(y + 1) % 16][x], m[y][(x + 1) % 16]
            lit = up in ".," or left in ".,"
            dark = down in ".," or right in ".,"
            if lit and not dark:
                t += 1
            elif dark and not lit:
                t -= 1
            row += str(max(lo, min(hi, t)))
        out.append(row)
    return out


tex("cobblestone", digits("c"), stones(COBBLE_MAP, COBBLE_TONE, {".": "0", ",": "1"}))

tex("andesite", digits("a"), [
    "3323343233233323",
    "2333223333432332",
    "3343332313333234",
    "3233333432233332",
    "2333423333332333",
    "3312333223343323",
    "3433332334233332",
    "2332343333331423",
    "3333233243323333",
    "3423333332333233",
    "3332342333433312",
    "2333133233223343",
    "3343232334333323",
    "3233333333232333",
    "3332433423313432",
    "2313323333343323",
])

tex("dirt", digits("d"), [
    "2222321222222322",
    "2102222222122222",
    "2222222322222202",
    "3222122222223222",
    "2222222212222242",
    "2212232220221232",
    "2422222222322222",
    "1022212222222102",
    "2223222132222222",
    "2222222222222342",
    "2122223222102222",
    "2222222222222212",
    "2232122223222222",
    "2242222102222232",
    "1022322222212222",
    "2222222232222242",
])

tex("coarse_dirt", digits("d"), [
    "2302213202312032",
    "1223022320221302",
    "2032311220302213",
    "3220123032213022",
    "0213202213022130",
    "2302321302230213",
    "1320203122302302",
    "2013230213020231",
    "3202122302312023",
    "2130302130223102",
    "0223213022103230",
    "2302032213220213",
    "1223120302302321",
    "3032213230213022",
    "2203022102322130",
    "1320230213030223",
])

tex("grass_top", digits("g"), [
    "2324223123241322",
    "3212342232312233",
    "2231213324223142",
    "1343222132332213",
    "2213342221424322",
    "3322123332213231",
    "2143232412323322",
    "3221332223132413",
    "2332412323221232",
    "1223221342343221",
    "3412332213122332",
    "2231223324233413",
    "2322413212312232",
    "3143223231442321",
    "2322341323213232",
    "1233212242332312",
])

GRASS_SIDE_KEY = dict(digits("d"), a="g0", b="g1", c="g2", e="g3", f="g4")
tex("grass_side", GRASS_SIDE_KEY, [
    "cecbcfecbcecfbec",
    "bccbcbecbcbecbcb",
    "abcbabcbbabcbabb",
    "a2ba1ab2aba2bab1",
    "2a12a22a22a12a22",
    "2222212222222222",
    "2122222232222122",
    "2222322222102222",
    "1022222212222322",
    "2232212222222222",
    "2222222223222212",
    "2122232222102222",
    "2222222122222232",
    "3222102222321222",
    "2212222322222212",
    "2222232222102222",
])

GRAVEL_MAP = [
    "AA.BBB.CC.DD.EEE",
    "AAA.BB.CCC.DD.EE",
    ".A.FF..CC.GGG..E",
    "HH.FFF..IIGGG.JJ",
    "HHH.F.KK.II.G.JJ",
    ".H.LL.KKK..MM.J.",
    "NN.LLL.KK.MMMM.O",
    "NNN.LL...P.MM.OO",
    ".N.QQ.RR.PP..OO.",
    "SS.QQQ.RRR.PPTT.",
    "SSS.Q..RR.UU.TTT",
    ".S.VVV...UUU..T.",
    "WW.VVV.XX.UU.YY.",
    "WWW.V.XXX..ZZ.YY",
    ".WW..XXX.ZZZ..Y.",
    "AA.BB.X..ZZ.EE..",
]
GRAVEL_TONE = dict(A=3, B=2, C=4, D=2, E=3, F=2, G=3, H=2, I=4, J=3, K=2, L=3, M=2, N=3, O=2, P=3, Q=2, R=3, S=4,
                   T=2, U=3, V=2, W=3, X=2, Y=4, Z=3)
_gravel = stones(GRAVEL_MAP, GRAVEL_TONE, {".": "1", ",": "1"}, lo=1, hi=4)
_brown = {(x, y) for y, r in enumerate(GRAVEL_MAP) for x, ch in enumerate(r) if ch in "DKU"}  # flint pebbles
tex("gravel", dict(digits("v"), b="v5"),
    ["".join("b" if (x, y) in _brown and _gravel[y][x] in "23" else _gravel[y][x] for x in range(16))
     for y in range(16)])

tex("path_top", digits("p"), [
    "2232222322232222",
    "2222321222222322",
    "3222222222322222",
    "2212222322222212",
    "2222312222132222",
    "2322222222222232",
    "2221222312222222",
    "2222223222212322",
    "3222122222222222",
    "2222222212232222",
    "2132222222222212",
    "2222232213222222",
    "2222222222222322",
    "2312222322122222",
    "2222212222223222",
    "2232222222222212",
])
tex("path_side", dict(digits("d"), P="p2", Q="p1", R="p3"), [
    "................",
    "PRPPQPPRPPPQPRPP",
    "QPPQPPQPPRPPQPPQ",
    "2Q22Q2QQ22Q2Q22Q",
    "2222212222222222",
    "2122222232222122",
    "2222322222102222",
    "1022222212222322",
    "2232212222222222",
    "2222222223222212",
    "2122232222102222",
    "2222222122222232",
    "3222102222321222",
    "2212222322222212",
    "2222232222102222",
    "2122222222222222",
])

tex("oak_log", digits("b", 4), [
    "2132213322312322",
    "2132213322312322",
    "2132213322312321",
    "2132203322312321",
    "2131213322312321",
    "2131213322212321",
    "2231213322212321",
    "2231213302212322",
    "2231213312212322",
    "2132213312312322",
    "2132213312312312",
    "2132213322312312",
    "2132213322302312",
    "2132212322312322",
    "2122212322312322",
    "2132213322312322",
])
tex("oak_log_top", {"b": "b1", "1": "r1", "2": "r2", "3": "r3"}, [
    "bbbbbbbbbbbbbbbb",
    "b33333333333333b",
    "b32222222222223b",
    "b32333333333323b",
    "b32322222222323b",
    "b32321111112323b",
    "b32321222212323b",
    "b32321233212323b",
    "b32321233212323b",
    "b32321222212323b",
    "b32321111112323b",
    "b32322222222323b",
    "b32333333333323b",
    "b32222222222223b",
    "b33333333333333b",
    "bbbbbbbbbbbbbbbb",
])
tex("stripped_oak", digits("w", 4), [
    "2312322232132322",
    "2312322232132322",
    "2312322132132322",
    "2312312132132322",
    "2212312132132322",
    "2212312132132312",
    "2312312132232312",
    "2312322132232312",
    "2312322232232322",
    "2312322232132322",
    "2302322232132322",
    "2302322232132321",
    "2312322232132321",
    "2312322232132321",
    "2312322132132322",
    "2312322132132322",
])
tex("stripped_oak_top", digits("w", 4), [
    "1111111111111111",
    "1333333333333331",
    "1322222222222231",
    "1323333333333231",
    "1323222222223231",
    "1323211111123231",
    "1323212222123231",
    "1323212332123231",
    "1323212332123231",
    "1323212222123231",
    "1323211111123231",
    "1323222222223231",
    "1323333333333231",
    "1322222222222231",
    "1333333333333331",
    "1111111111111111",
])
tex("planks", digits("k", 4), [
    "2223222232222232",
    "2322223222322222",
    "2222322222223222",
    "1111111110111111",
    "2232222222232222",
    "2222232322222322",
    "3222222222222222",
    "1111011111111111",
    "2222223222232222",
    "2322222232222223",
    "2222322222222222",
    "1111111111110111",
    "2232222322222322",
    "2222222222322222",
    "2223223222222232",
    "1110111111111111",
])
WOOL = [
    "2232212232221232",
    "2122232212322212",
    "2321222321212322",
    "1232123212232121",
    "2212322122123232",
    "3221232232212212",
    "2232121222321232",
    "1212232321212322",
    "2321223212232121",
    "2232122121322232",
    "1212323232121212",
    "2321212122232321",
    "2232232321212232",
    "1212121232323212",
    "2321232212121321",
    "2232212321232212",
]
tex("white_wool", digits("n", 4), WOOL)
tex("dawn_wool", digits("t", 4), WOOL)
# a lantern: an iron cage round glass lit from inside. Its two little boxes read this by position, as the game's
# models do: the sides read rows 7 to 15 (the cap, then the cage), the tops read rows 5 to 10
tex("lantern", {"0": "i0", "1": "i1", "2": "i2", "3": "i3", "f": "f1", "g": "f2", "h": "f3"}, [
    "1111111111111111",
    "1111111111111111",
    "1111111111111111",
    "1111111111111111",
    "1111111111111111",
    "1111122222211111",
    "1111123333211111",
    "1111112332111111",
    "1111112222111111",
    "1111100000011111",
    "111111gggg111111",
    "111111ghhg111111",
    "111111ghhg111111",
    "111111gffg111111",
    "111111gggg111111",
    "1111100000011111",
])

# plants: cross models (two planes corner to corner, as the game draws grass and flowers); '.' is see-through
tex("short_grass", dict(a="g0", b="g1", c="g2", e="g3", f="g4"), [
    "................",
    "................",
    "................",
    "......c.........",
    "..b...c....e....",
    "..b..cb...eb....",
    "..cb.cb...c..b..",
    "...b.bc.b.c.bc..",
    ".e.bcb.cb.cbb...",
    ".cb.bb.c..bcb.e.",
    "..cbcb.cbb.b.ec.",
    "..bcbbcb.bbb.cb.",
    "...bbcbbcbb.bb..",
    "...abbbabbbbab..",
    "....abaabaaba...",
    "....aaa.aaaa....",
])
tex("allium", dict(l="l0", m="l1", n="l2", o="l3", a="g0", b="g1", c="g2"), [
    "................",
    "................",
    "......lml.......",
    ".....lnmnl......",
    "....lmnonml.....",
    "....mnoonml.....",
    "....lmnnmml.....",
    ".....lmmml......",
    "......lml.......",
    ".......b........",
    ".......b........",
    "......cb........",
    ".......b..c.....",
    ".....cbb.cb.....",
    "......bb.b......",
    ".......bb.......",
])
tex("dead_bush", {"1": "b1", "2": "b2", "3": "b3"}, [
    "................",
    "................",
    "..2.......3.....",
    "...2.....3......",
    "...2..2..3...3..",
    "....2.2.3...3...",
    "..3..22.3..3....",
    "...3..2.3.3.....",
    "....3.2.33......",
    ".....322........",
    "......22........",
    "......2.........",
    "......2.........",
    "......1.........",
    "................",
    "................",
])

# ================================================================================================ blocks
# faces: top, bottom, side. A model is a list of boxes in 1/16 of a block, each (x0, y0, z0, x1, y1, z1); None is a
# full cube. Full cubes take part in the corner shading. glow: the block light it gives off (0..1).
BLOCKS = [None]                 # id 0 is air
BID = {}


def block(name, top, side=None, bottom=None, model=None, glow=0.0):
    side = side or top
    bottom = bottom or side
    BID[name] = len(BLOCKS)
    BLOCKS.append(dict(name=name, top=top, side=side, bottom=bottom, model=model, glow=glow))


block("stone", "stone")
block("cobblestone", "cobblestone")
block("andesite", "andesite")
block("dirt", "dirt")
block("coarse_dirt", "coarse_dirt")
block("grass", "grass_top", "grass_side", "dirt")
block("gravel", "gravel")
block("path", "path_top", "path_side", "dirt", model=[(0, 0, 0, 16, 15, 16)])
block("oak_log", "oak_log_top", "oak_log")              # the world keeps an axis per block
block("stripped_oak", "stripped_oak_top", "stripped_oak")
block("planks", "planks")
block("white_wool", "white_wool")
block("dawn_wool", "dawn_wool")
block("fence", "planks", model=[(6, 0, 6, 10, 16, 10)])          # rails are added where it meets a fence
block("lantern", "lantern", model=[(5, 0, 5, 11, 7, 11), (6, 7, 6, 10, 9, 10)], glow=1.0)
block("short_grass", "short_grass", model="cross")
block("allium", "allium", model="cross")
block("dead_bush", "dead_bush", model="cross")
CROSS_IDS = [i for i, b in enumerate(BLOCKS) if b and b["model"] == "cross"]
OPAQUE_TEXEL = {name: np.array([[ch != "." for ch in r] for r in rows]) for name, (rows, key) in TEX.items()}

MODEL_IDS = [i for i, b in enumerate(BLOCKS) if b and b["model"] is not None and b["model"] != "cross"]
FULL = np.array([bool(b) and b["model"] is None for b in BLOCKS])
AXIS_Y, AXIS_X, AXIS_Z = 0, 1, 2


class World:
    """a block grid over a box of world coordinates (blocks are unit cubes at integer corners)"""
    def __init__(self, x0, x1, y0, y1, z0, z1):
        self.lo = np.array([x0, y0, z0])
        self.hi = np.array([x1, y1, z1])
        self.shape = tuple(int(v) for v in self.hi - self.lo)
        self.id = np.zeros(self.shape, np.uint8)
        self.axis = np.zeros(self.shape, np.uint8)
        self.rails = {}           # fence (x, y, z) -> its boxes, built by finish()

    def inside(self, x, y, z):
        return all(0 <= v - l < s for v, l, s in zip((x, y, z), self.lo, self.shape))

    def set(self, x, y, z, name, axis=AXIS_Y):
        if self.inside(x, y, z):
            i = (x - self.lo[0], y - self.lo[1], z - self.lo[2])
            self.id[i] = 0 if name is None else BID[name]
            self.axis[i] = axis

    def get(self, x, y, z):
        if self.inside(x, y, z):
            return BLOCKS[self.id[x - self.lo[0], y - self.lo[1], z - self.lo[2]]]
        return None

    def fill(self, x0, x1, y0, y1, z0, z1, name, axis=AXIS_Y):
        """fill the blocks x0 <= x < x1, y0 <= y < y1, z0 <= z < z1"""
        for x in range(x0, x1):
            for y in range(y0, y1):
                for z in range(z0, z1):
                    self.set(x, y, z, name, axis)

    def finish(self):
        """fence rails: a fence joins a fence or a full block beside it along x or z with two rails, as in game"""
        fence = BID["fence"]
        for (i, j, k) in zip(*np.nonzero(self.id == fence)):
            x, y, z = int(i + self.lo[0]), int(j + self.lo[1]), int(k + self.lo[2])
            boxes = [(6, 0, 6, 10, 16, 10)]
            for (dx, dz, b) in ((1, 0, (10, 7, 16, 9)), (-1, 0, (0, 7, 6, 9)),
                                (0, 1, (7, 10, 9, 16)), (0, -1, (7, 0, 9, 6))):
                nb = self.get(x + dx, y, z + dz)
                if nb and (nb["name"] == "fence" or nb["model"] is None):
                    for (y0, y1) in ((6, 9), (12, 15)):
                        if dx:
                            boxes.append((b[0], y0, b[1], b[2], y1, b[3]))
                        else:
                            boxes.append((b[0], y0, b[1], b[2], y1, b[3]))
            self.rails[(x, y, z)] = boxes


# ================================================================================================ the camera
class Camera:
    """one-point perspective with a shifted lens: the camera looks straight along +z, so uprights stay upright and
    every front face is a true square; depth lines run to the vanishing point (xv, yv) in GUI pixels; f is the
    focal length in GUI pixels; eye is the camera's place in world blocks"""
    def __init__(self, f, xv, yv, eye):
        self.f, self.xv, self.yv = f, xv, yv
        self.eye = np.array(eye, float)

    def project(self, p):
        x, y, z = np.asarray(p, float) - self.eye
        return self.xv + self.f * x / z, self.yv - self.f * y / z

    def unproject(self, sx, sy, z):
        """the world point at depth z (world) seen at GUI pixel (sx, sy)"""
        dz = z - self.eye[2]
        return (self.eye[0] + (sx - self.xv) * dz / self.f, self.eye[1] - (sy - self.yv) * dz / self.f, z)

    def rays(self, x0, y0, w, h):
        u = np.arange(w) + x0 + 0.5
        v = np.arange(h) + y0 + 0.5
        U, V = np.meshgrid(u, v)
        d = np.stack([(U - self.xv) / self.f, -(V - self.yv) / self.f, np.ones_like(U)], -1)
        return d.reshape(-1, 3)


# ================================================================================================ ray casting
def _boxes_hit(O, D, boxes, tmin):
    """nearest hit of rays (O, D: (n, 3)) on axis-aligned boxes [(lo (n,3), hi (n,3))] at or beyond tmin:
    t, the axis of the face hit, the box index (-1 for a miss)"""
    n = len(D)
    inv = 1.0 / np.where(np.abs(D) < 1e-12, 1e-12, D)
    best = np.full(n, np.inf)
    bax = np.zeros(n, np.int64)
    bix = np.full(n, -1)
    for bi, (lo, hi) in enumerate(boxes):
        ta = (lo - O) * inv
        tb = (hi - O) * inv
        tn3 = np.minimum(ta, tb)
        tn = tn3.max(1)
        tf = np.maximum(ta, tb).min(1)
        ok = (tn <= tf) & (tn >= tmin - 1e-9) & (tn < best)
        best = np.where(ok, tn, best)
        bax = np.where(ok, tn3.argmax(1), bax)
        bix = np.where(ok, bi, bix)
    return best, bax, bix


def trace(world, O, D, shadow=False, maxsteps=600):
    """walk rays through the block grid (Amanatides and Woo), all rays at once.
    Returns t, normal (n, 3), block id, the block's cell (n, 3); with shadow=True only whether each ray is blocked"""
    n = len(D)
    O = np.broadcast_to(np.asarray(O, float), (n, 3)).copy()
    D = np.asarray(D, float)
    inv = 1.0 / np.where(np.abs(D) < 1e-12, 1e-12, D)
    lo, hi = world.lo.astype(float), world.hi.astype(float)
    ta, tb = (lo - O) * inv, (hi - O) * inv
    tnear3 = np.minimum(ta, tb)
    tnear, tfar = tnear3.max(1), np.maximum(ta, tb).min(1)
    alive = tfar > np.maximum(tnear, 0)
    t = np.maximum(tnear, 0.0)
    eax = np.where(tnear > 0, tnear3.argmax(1), 0)
    P = O + D * (t + 1e-7)[:, None]
    cell = np.clip(np.floor(P).astype(np.int64), world.lo, world.hi - 1)
    step = np.where(D > 0, 1, -1)
    tdel = np.abs(inv)
    tnext = ((cell + (step > 0)) - O) * inv
    out_t = np.full(n, np.inf)
    out_n = np.zeros((n, 3))
    out_b = np.zeros(n, np.int64)
    out_c = np.zeros((n, 3), np.int64)
    blocked = np.zeros(n, bool)
    fence = BID["fence"]
    shape = np.array(world.shape)
    idx = np.nonzero(alive)[0]
    for _ in range(maxsteps):
        if idx.size == 0:
            break
        c = cell[idx]
        rel = c - world.lo
        inb = np.all((rel >= 0) & (rel < shape), 1)
        idx, c, rel = idx[inb], c[inb], rel[inb]
        if idx.size == 0:
            break
        b = world.id[rel[:, 0], rel[:, 1], rel[:, 2]].astype(np.int64)
        done = np.zeros(idx.size, bool)
        full = FULL[b]
        if full.any():
            j = idx[full]
            if shadow:
                blocked[j] = True
            else:
                out_t[j] = t[j]
                nn = np.zeros((j.size, 3))
                ax = eax[j]
                nn[np.arange(j.size), ax] = -step[j, ax]
                out_n[j] = nn
                out_b[j] = b[full]
                out_c[j] = c[full]
            done |= full
        for mid in MODEL_IDS:
            sel = np.nonzero(b == mid)[0]
            if sel.size == 0:
                continue
            j = idx[sel]
            cc = c[sel]
            if mid == fence:
                groups = {}
                for k, key in enumerate(map(tuple, cc.tolist())):
                    groups.setdefault(tuple(world.rails.get(key, BLOCKS[mid]["model"])), []).append(k)
            else:
                groups = {tuple(BLOCKS[mid]["model"]): list(range(j.size))}
            for boxes, ks in groups.items():
                ks = np.array(ks)
                jj = j[ks]
                base = cc[ks].astype(float)
                bl = [(base + np.array(bx[:3]) / 16.0, base + np.array(bx[3:]) / 16.0) for bx in boxes]
                tt, bax, bix = _boxes_hit(O[jj], D[jj], bl, t[jj])
                ok = bix >= 0
                if not ok.any():
                    continue
                jj, tt, bax = jj[ok], tt[ok], bax[ok]
                if shadow:
                    blocked[jj] = True
                else:
                    out_t[jj] = tt
                    nn = np.zeros((jj.size, 3))
                    nn[np.arange(jj.size), bax] = -step[jj, bax]
                    out_n[jj] = nn
                    out_b[jj] = mid
                    out_c[jj] = cc[ks][ok]
                done[sel[ks[ok]]] = True
        if not shadow:
            for cid in CROSS_IDS:
                sel = np.nonzero(b == cid)[0]
                if sel.size == 0:
                    continue
                j = idx[sel]
                cc = c[sel].astype(float)
                texok = OPAQUE_TEXEL[BLOCKS[cid]["top"]]
                texit = tnext[j].min(1)
                best = np.full(j.size, np.inf)
                for plane in (0, 1):
                    if plane == 0:      # x - cx = z - cz
                        den = D[j, 0] - D[j, 2]
                        num = (cc[:, 0] - O[j, 0]) - (cc[:, 2] - O[j, 2])
                    else:               # x - cx = 1 - (z - cz)
                        den = D[j, 0] + D[j, 2]
                        num = 1 + cc[:, 0] + cc[:, 2] - O[j, 0] - O[j, 2]
                    tp = num / np.where(np.abs(den) < 1e-12, 1e-12, den)
                    hp = O[j] + D[j] * tp[:, None]
                    u = hp[:, 0] - cc[:, 0]
                    v = hp[:, 1] - cc[:, 1]
                    ok = (tp >= t[j] - 1e-9) & (tp <= texit + 1e-9) & (u >= 0) & (u < 1) & (v >= 0) & (v < 1)
                    col = np.clip((u * 16).astype(int), 0, 15)
                    row = np.clip(((1 - v) * 16).astype(int), 0, 15)
                    ok &= texok[row, col]
                    ok &= tp < best
                    best = np.where(ok, tp, best)
                okk = np.isfinite(best)
                if okk.any():
                    jj = j[okk]
                    out_t[jj] = best[okk]
                    out_n[jj] = np.array([0.0, 1.0, 0.0])
                    out_b[jj] = cid
                    out_c[jj] = c[sel][okk]
                    done[sel[okk]] = True
        idx = idx[~done]
        if idx.size == 0:
            break
        tn = tnext[idx]
        ax = tn.argmin(1)
        r = np.arange(idx.size)
        t[idx] = tn[r, ax]
        eax[idx] = ax
        cell[idx, ax] += step[idx, ax]
        tnext[idx, ax] += tdel[idx, ax]
        idx = idx[t[idx] < tfar[idx]]
    if shadow:
        return blocked
    return out_t, out_n, out_b, out_c


def obb_hit(O, D, obbs, tmin=1e-6):
    """rays against oriented boxes: each is (centre (3,), axes (3, 3) whose rows are the box's unit axes, half sizes
    (3,)). Returns t, the world normal, the index of the box hit (-1 for a miss) and the hit in the box's frame"""
    n = len(D)
    O = np.broadcast_to(np.asarray(O, float), (n, 3))
    best = np.full(n, np.inf)
    nrm = np.zeros((n, 3))
    bix = np.full(n, -1)
    loc = np.zeros((n, 3))
    r = np.arange(n)
    for i, (c, A, h) in enumerate(obbs):
        A = np.asarray(A, float)
        h = np.asarray(h, float)
        o = (O - np.asarray(c, float)) @ A.T
        d = D @ A.T
        inv = 1.0 / np.where(np.abs(d) < 1e-12, 1e-12, d)
        ta, tb = (-h - o) * inv, (h - o) * inv
        tn3 = np.minimum(ta, tb)
        tn, tf = tn3.max(1), np.maximum(ta, tb).min(1)
        ok = (tn <= tf) & (tn > tmin) & (tn < best)
        if not ok.any():
            continue
        ax = tn3.argmax(1)
        nl = np.zeros((n, 3))
        nl[r, ax] = -np.sign(d[r, ax])
        best = np.where(ok, tn, best)
        nrm = np.where(ok[:, None], nl @ A, nrm)
        bix = np.where(ok, i, bix)
        loc = np.where(ok[:, None], o + d * tn[:, None], loc)
    return best, nrm, bix, loc


# ================================================================================================ light
def corner_ao(world, cell, nrm, fu, fv):
    """the game's corner shading on block faces: each corner of a face is darkened by the full blocks that touch it
    in the layer in front of the face (two sides and the corner between them); read at the texel centre (fu, fv in
    0..1 along the face's u and v axes, as face_uv gives them). Returns 0..1 (1 = open)"""
    n = len(cell)
    r = np.arange(n)
    ax = np.argmax(np.abs(nrm), 1)
    sgn = np.sign(nrm[r, ax]).astype(int)
    front = cell.copy()
    front[r, ax] += sgn
    ua = np.where(ax == 0, 2, 0)          # u runs along z on x faces, along x otherwise
    va = np.where(ax == 1, 2, 1)          # v runs along z on y faces, along y otherwise
    occ = FULL[world.id]
    shape = np.array(world.shape)

    def solid(p):
        rel = p - world.lo
        ok = np.all((rel >= 0) & (rel < shape), 1)
        q = np.clip(rel, 0, shape - 1)
        return ok & occ[q[:, 0], q[:, 1], q[:, 2]]

    # fu, fv here are along +u and +v world directions (0 at the low side)
    vals = {}
    for su in (0, 1):
        for sv in (0, 1):
            du = np.zeros((n, 3), int)
            dv = np.zeros((n, 3), int)
            du[r, ua] = 2 * su - 1
            dv[r, va] = 2 * sv - 1
            s1, s2, cc = solid(front + du), solid(front + dv), solid(front + du + dv)
            v = np.where(s1 & s2, 0, 3 - (s1.astype(int) + s2 + cc))
            vals[(su, sv)] = v / 3.0
    return (vals[(0, 0)] * (1 - fu) * (1 - fv) + vals[(1, 0)] * fu * (1 - fv) +
            vals[(0, 1)] * (1 - fu) * fv + vals[(1, 1)] * fu * fv)


def sky_light(world):
    """the game's sky light per cell: 15 straight down from open sky, then spreading to the six neighbours, one
    level lost per block (opaque blocks hold 0). Outside the world box the air is open (15 at the sides)"""
    occ = FULL[world.id]
    X, Y, Z = world.shape
    L = np.zeros(world.shape, np.int16)
    open_ = np.ones((X, Z), bool)
    for y in range(Y - 1, -1, -1):
        open_ &= ~occ[:, y, :]
        L[:, y, :] = np.where(open_, 15, 0)
    for _ in range(15):
        P = np.pad(L, ((1, 1), (0, 0), (1, 1)), mode="edge")
        P = np.pad(P, ((0, 0), (1, 1), (0, 0)), constant_values=0)
        P[:, -1, :] = 15
        nb = np.maximum.reduce([P[2:, 1:-1, 1:-1], P[:-2, 1:-1, 1:-1], P[1:-1, 2:, 1:-1], P[1:-1, :-2, 1:-1],
                                P[1:-1, 1:-1, 2:], P[1:-1, 1:-1, :-2]])
        L = np.where(occ, 0, np.maximum(L, nb - 1)).astype(np.int16)
    return L


def smooth_sky(world, L, cell, nrm, fu, fv, outside=15):
    """the game's smooth lighting on a full block's face: each corner takes the mean light (sky or block, as L
    holds) of the open cells of the four that touch it in front of the face (the corner cell only counts when a
    side is open), and the face blends its corners at the texel centre (fu, fv as in corner_ao). Returns a level
    0..15 (float); cells outside the world box hold `outside`"""
    n = len(cell)
    r = np.arange(n)
    ax = np.argmax(np.abs(nrm), 1)
    sgn = np.sign(nrm[r, ax]).astype(int)
    front = cell.copy()
    front[r, ax] += sgn
    ua = np.where(ax == 0, 2, 0)
    va = np.where(ax == 1, 2, 1)
    occ = FULL[world.id]
    shape = np.array(world.shape)

    def look(p):
        rel = p - world.lo
        ok = np.all((rel >= 0) & (rel < shape), 1)
        q = np.clip(rel, 0, shape - 1)
        opaque = ok & occ[q[:, 0], q[:, 1], q[:, 2]]
        lv = np.where(ok, L[q[:, 0], q[:, 1], q[:, 2]], outside)
        return ~opaque, lv.astype(float)

    o0, l0 = look(front)
    vals = {}
    for su in (0, 1):
        for sv in (0, 1):
            du = np.zeros((n, 3), int)
            dv = np.zeros((n, 3), int)
            du[r, ua] = 2 * su - 1
            dv[r, va] = 2 * sv - 1
            o1, l1 = look(front + du)
            o2, l2 = look(front + dv)
            o3, l3 = look(front + du + dv)
            o3 = o3 & (o1 | o2)
            num = l0 * o0 + l1 * o1 + l2 * o2 + l3 * o3
            den = o0.astype(int) + o1 + o2 + o3
            vals[(su, sv)] = num / np.maximum(den, 1)
    return (vals[(0, 0)] * (1 - fu) * (1 - fv) + vals[(1, 0)] * fu * (1 - fv) +
            vals[(0, 1)] * (1 - fu) * fv + vals[(1, 1)] * fu * fv)


def cell_sky(world, L, p, outside=15):
    """the light (sky or block, as L holds) in the cell holding each point p (n, 3): for models, plants and
    entities"""
    rel = np.floor(p).astype(np.int64) - world.lo
    shape = np.array(world.shape)
    ok = np.all((rel >= 0) & (rel < shape), 1)
    q = np.clip(rel, 0, shape - 1)
    return np.where(ok, L[q[:, 0], q[:, 1], q[:, 2]], outside).astype(float)


def block_light(world):
    """the game's block light per cell: a lantern holds 15 in its own cell and the light runs out through every
    cell that is not a full block, one level less per block, round corners and through doorways as in the game
    (full blocks hold 0; outside the world box is dark)"""
    occ = FULL[world.id]
    L = np.zeros(world.shape, np.int16)
    for i, b in enumerate(BLOCKS):
        if b and b["glow"] > 0:
            L[world.id == i] = int(round(15 * b["glow"]))
    for _ in range(15):
        P = np.pad(L, 1, constant_values=0)
        nb = np.maximum.reduce([P[2:, 1:-1, 1:-1], P[:-2, 1:-1, 1:-1], P[1:-1, 2:, 1:-1], P[1:-1, :-2, 1:-1],
                                P[1:-1, 1:-1, 2:], P[1:-1, 1:-1, :-2]])
        L = np.where(occ, 0, np.maximum(L, nb - 1)).astype(np.int16)
    return L


def brightness(level):
    """the game's light curve: a level 0..15 to brightness 0..1 (f / (4 - 3 f), the overworld's)"""
    f = np.asarray(level, float) / 15.0
    return f / (4.0 - 3.0 * f)


# ================================================================================================ colour
def _lin(c):
    c = np.asarray(c, float) / 255.0
    return np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)


def _srgb(c):
    c = np.clip(np.asarray(c, float), 0, 1)
    return np.round(255 * np.where(c <= 0.0031308, c * 12.92, 1.055 * c ** (1 / 2.4) - 0.055)).astype(int)


def hexrgb(c):
    c = c.lstrip("#")
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def ramp(stops, n):
    """a light ramp of n levels from hand-set stops [(level, (r, g, b) multipliers)], interpolated between them"""
    out = []
    for k in range(n):
        for (a, ca), (b, cb) in zip(stops, stops[1:]):
            if a <= k <= b:
                u = (k - a) / max(1, b - a)
                out.append(tuple(ca[i] + (cb[i] - ca[i]) * u for i in range(3)))
                break
    return out


def lit_hex(albedo_hex, mult):
    """an albedo under a light (multipliers per channel, applied in linear light)"""
    return "#%02x%02x%02x" % tuple(_srgb(_lin(hexrgb(albedo_hex)) * np.asarray(mult, float)))


# ================================================================================================ entities
# Things that are not blocks (the commander, the dropped shield, the spear) are oriented boxes with a hand-typed
# grid on each face, like the game's entity models. Faces are one-sided, as the game draws them: a ray meets a box
# on the face it enters by, and a see-through texel there ('.') lets it pass on to whatever is inside or behind.
def grid(rows, key):
    """a typed face grid to an array of tones ('' where see-through)"""
    return np.array([[key.get(ch, "") if ch != "." else "" for ch in r] for r in rows], dtype=object)


def solid(tone, w=1, h=1):
    return np.full((h, w), tone, dtype=object)


def rot(yaw=0.0, pitch=0.0, roll=0.0):
    """the rows are the box's own x, y, z axes in the world: turned by yaw (about y, degrees), then pitch (about
    x, positive tips its top away from us), then roll (about z, positive turns it counter-clockwise as we see it)"""
    a, b, c = np.radians([yaw, pitch, roll])
    Ry = np.array([[np.cos(a), 0, -np.sin(a)], [0, 1, 0], [np.sin(a), 0, np.cos(a)]])
    Rx = np.array([[1, 0, 0], [0, np.cos(b), np.sin(b)], [0, -np.sin(b), np.cos(b)]])
    Rz = np.array([[np.cos(c), np.sin(c), 0], [-np.sin(c), np.cos(c), 0], [0, 0, 1]])
    return Rz @ Rx @ Ry


class Box:
    def __init__(self, centre, half, faces, axes=None, part=0):
        """faces: {(axis 0/1/2, sign +1/-1): tone grid} for the box's own axes, with key 'all' as the default"""
        self.c = np.asarray(centre, float)
        self.h = np.asarray(half, float)
        self.A = np.eye(3) if axes is None else np.asarray(axes, float)
        self.faces = faces
        self.part = part

    def face(self, ax, sg):
        return self.faces.get((ax, sg), self.faces.get("all"))


def face_uv(loc, h, ax, sg):
    """where a hit lands on a face, as (u, v) in 0..1: u left to right and v top to bottom as the face is seen
    from outside, upright (tops and bottoms are seen with their far edge up)"""
    x, y, z = (loc[:, k] for k in range(3))
    hx, hy, hz = h
    if ax == 2:
        u = (x + hx) / (2 * hx) if sg < 0 else (hx - x) / (2 * hx)
        v = (hy - y) / (2 * hy)
    elif ax == 0:
        u = (z + hz) / (2 * hz) if sg > 0 else (hz - z) / (2 * hz)
        v = (hy - y) / (2 * hy)
    else:
        u = (x + hx) / (2 * hx)
        v = (hz - z) / (2 * hz) if sg > 0 else (z + hz) / (2 * hz)
    return u, v


def boxes_hit(O, D, boxes, tmin=1e-6, want_tone=True):
    """rays against entity boxes: the nearest entry face with a solid texel.
    Returns t, world normal, box index (-1 for none), tone, and the hit in the box's frame"""
    n = len(D)
    O = np.broadcast_to(np.asarray(O, float), (n, 3))
    best = np.full(n, np.inf)
    nrm = np.zeros((n, 3))
    bix = np.full(n, -1)
    tone = np.full(n, "", dtype=object)
    loc = np.zeros((n, 3))
    r = np.arange(n)
    for i, bx in enumerate(boxes):
        o = (O - bx.c) @ bx.A.T
        d = D @ bx.A.T
        inv = 1.0 / np.where(np.abs(d) < 1e-12, 1e-12, d)
        ta, tb = (-bx.h - o) * inv, (bx.h - o) * inv
        tn3 = np.minimum(ta, tb)
        tn, tf = tn3.max(1), np.maximum(ta, tb).min(1)
        ok = (tn <= tf) & (tn > tmin) & (tn < best)
        if not ok.any():
            continue
        k = np.nonzero(ok)[0]
        ax = tn3[k].argmax(1)
        sg = -np.sign(d[k, ax]).astype(int)
        lp = o[k] + d[k] * tn[k, None]
        tn_k = np.full(k.size, "", dtype=object)
        for a in (0, 1, 2):
            for s in (-1, 1):
                m = (ax == a) & (sg == s)
                if not m.any():
                    continue
                g = bx.face(a, s)
                if g is None:
                    continue
                u, v = face_uv(lp[m], bx.h, a, s)
                R, C = g.shape
                col = np.clip((u * C).astype(int), 0, C - 1)
                row = np.clip((v * R).astype(int), 0, R - 1)
                tn_k[m] = g[row, col]
        keep = tn_k != ""
        k, ax, sg, lp, tn_k = k[keep], ax[keep], sg[keep], lp[keep], tn_k[keep]
        nl = np.zeros((k.size, 3))
        nl[np.arange(k.size), ax] = sg
        best[k] = tn[k]
        nrm[k] = nl @ bx.A
        bix[k] = i
        tone[k] = tn_k
        loc[k] = lp
    return best, nrm, bix, tone, loc
