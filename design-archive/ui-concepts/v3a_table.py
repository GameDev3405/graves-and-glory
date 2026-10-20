"""v3 War Table furniture: the back rail (the top of the screen is the table, not sky), brass-framed nav plaques
and their icons, portrait frames sized for a live model render, the brass and parchment button families."""
import math, random
from v3lib import Spr, Noise, dith, rgba, mix, emboss, grid_mask
from v3pal import *

ASSETS = {}


def asset(name):
    def deco(f):
        ASSETS[name] = f
        return f
    return deco


# ================================================================ back rail
def rail(W=960, H=66, seed=3):
    """the table's raised back rail across the top of the screen: quartered oak (finer, darker grain than
    the table top), a moulding groove, a brass inlay with studs, and the shadow it casts onto the table"""
    rnd = random.Random(seed)
    s = Spr(W, H)
    body = 52
    idx = [[3] * W for _ in range(body)]
    for k in range(19):
        yb = 2 + k * 2.3 + rnd.random()
        amp = rnd.uniform(0.2, 0.8); L = rnd.uniform(160, 360); ph = rnd.uniform(0, 6.28)
        tone = rnd.choices([2, 4, 1], [60, 30, 10])[0]
        x = rnd.randint(-60, 0); on = True
        while x < W:
            run = rnd.randint(60, 240) if on else rnd.randint(10, 50)
            if on:
                for xx in range(max(0, x), min(W, x + run)):
                    Y = round(yb + amp * math.sin(6.283 * xx / L + ph))
                    if 1 <= Y <= 41:
                        idx[Y][xx] = tone
            on = not on; x += run
    for xx in range(W):
        idx[0][xx] = 2
        idx[42][xx] = 4          # moulding: a lit bead
        idx[43][xx] = 5 if (xx % 37) > 2 else 4
        idx[44][xx] = 1          # groove
        idx[45][xx] = 4          # the lip below the groove catches light
        for yy in range(46, body):
            idx[yy][xx] = 2      # front face of the rail, turned away from the light
        if (xx * 13) % 29 < 3:
            idx[47][xx] = 1
        if (xx * 7) % 23 < 2:
            idx[49][xx] = 1
    for yy in range(body):
        for xx in range(W):
            s.set(xx, yy, OAK[idx[yy][xx]])
    # brass inlay along the lower edge, with round studs every 48 px
    for xx in range(W):
        s.set(xx, body, BRASS[6]); s.set(xx, body + 1, BRASS[4]); s.set(xx, body + 2, BRASS[2])
    for cx in range(24, W, 48):
        stud = ['.hH.', 'hHmd', 'Hmmd', '.dd.']
        s.grid(cx - 2, body - 1, stud, {'h': BRASS[7], 'H': BRASS[6], 'm': BRASS[4], 'd': BRASS[2]})
    # cast shadow on the table: four steps
    for i, a in enumerate((0.62, 0.46, 0.32, 0.2, 0.12, 0.06)):
        for xx in range(W):
            s.set(xx, body + 3 + i, rgba('#0a0505', round(a * 255)))
    return s


ASSETS['rail'] = rail


# ================================================================ nav plaques
def plaque(w, h=28, seed=1):
    """a brass-framed oak plaque screwed to the rail: 2 px brass bevel, recessed dark-oak panel,
    corner screws, and a one-pixel shadow on the rail"""
    rnd = random.Random(seed + w)
    s = Spr(w + 1, h + 2)
    # shadow on the rail
    for x in range(1, w + 1):
        s.set(x, h, rgba('#0a0505', 150)); s.set(x, h + 1, rgba('#0a0505', 70))
    for y in range(1, h):
        s.set(w, y, rgba('#0a0505', 120))
    s.rect(0, 0, w, h, BRASS[0])
    # bevel ring 1 (outer): lit top-left, dark bottom-right
    s.hline(1, w - 2, 1, BRASS[6]); s.vline(1, 1, h - 2, BRASS[6])
    s.hline(1, w - 2, h - 2, BRASS[3]); s.vline(w - 2, 1, h - 2, BRASS[3])
    # ring 2: the inner bevel slopes into the panel, so its top-left faces away (dark) and bottom-right is lit
    s.hline(2, w - 3, 2, BRASS[4]); s.vline(2, 2, h - 3, BRASS[4])
    s.hline(2, w - 3, h - 3, BRASS[5]); s.vline(w - 3, 2, h - 3, BRASS[5])
    s.set(1, 1, BRASS[7]); s.set(2, 1, BRASS[7]); s.set(1, 2, BRASS[7])
    # panel: recessed dark oak, its top-left edge in the frame's shadow
    for y in range(3, h - 3):
        for x in range(3, w - 3):
            s.set(x, y, OAK[2])
    for k in range(int((h - 6) / 2.2)):
        yb = 4 + k * 2.2 + rnd.random() * 0.8
        amp = rnd.uniform(0.2, 0.6); L = rnd.uniform(60, 140); ph = rnd.uniform(0, 6.28)
        tone = rnd.choice([1, 3, 3])
        x = rnd.randint(-20, 4); on = True
        while x < w:
            run = rnd.randint(14, 60) if on else rnd.randint(4, 16)
            if on:
                for xx in range(max(4, x), min(w - 4, x + run)):
                    Y = round(yb + amp * math.sin(6.283 * xx / L + ph))
                    if 4 <= Y <= h - 5:
                        s.set(xx, Y, OAK[tone])
            on = not on; x += run
    s.hline(3, w - 4, 3, OAK[0]); s.vline(3, 3, h - 4, OAK[0])
    s.hline(4, w - 4, h - 4, OAK[3])
    # corner screws (slotted)
    for (cx, cy) in ((1, 1), (w - 4, 1), (1, h - 4), (w - 4, h - 4)):
        s.grid(cx, cy, ['hm.', 'mmd', '.dd'], {'h': BRASS[7], 'm': BRASS[5], 'd': BRASS[2]})
    return s


for _w in (80, 88, 96, 104, 112, 120):
    ASSETS['plaque-%d' % _w] = (lambda w: (lambda: plaque(w)))(_w)


# ---------------------------------------------------------------- nav icons, 14 x 14, hand-placed
ICONS = {
    # the camp: an A-frame tent, canvas lit on the left, its doorway dark, a red pennant on the pole
    'camp': (['......b.......',
              '......brr.....',
              '......brrr....',
              '......brR.....',
              '......b.......',
              '.....kbbk.....',
              '.....kLMk.....',
              '....kLLMSk....',
              '....kLLMSk....',
              '...kLLDDSSk...',
              '...kLLDDSSk...',
              '..kLLDDDDSSk..',
              '..kLLDDDDSSk..',
              '.kkkkkkkkkkkk.'],
             {'b': BRASS[5], 'r': WAX[4], 'R': WAX[2], 'k': OAK[0], 'L': VEL[6], 'M': VEL[4], 'S': VEL[2], 'D': OAK[0]}),
    # recruit: a sealed letter
    'recruit': (['..............',
                 '..............',
                 'kkkkkkkkkkkkkk',
                 'kLLLLLLLLLLLLk',
                 'kSLLLLLLLLLLSk',
                 'kMSLLLLLLLLSMk',
                 'kMMSLLLLLLSMMk',
                 'kMMMSLLLLSMMMk',
                 'kMMMMSwwSMMMMk',
                 'kMMMMwhwwdMMMk',
                 'kMMMMwwwddMMMk',
                 'kNNNNNdddNNNNk',
                 'kkkkkkkkkkkkkk',
                 '..............'],
                {'k': OAK[0], 'L': VEL[7], 'S': VEL[3], 'M': VEL[5], 'N': VEL[4], 'w': WAX[3], 'h': WAX[6], 'd': WAX[1]}),
    # banner: your cloth on a crossbar, swallowtailed, the Dawn sun woven in
    'banner': (['......bb......',
                '..bbbbBBbbbb..',
                '..kddddddddk..',
                '..kTTTTTTTSk..',
                '..ktTTTyTTSk..',
                '..ktTTyyyTSk..',
                '..ktTTTyTTSk..',
                '..ktTTTTTTSk..',
                '..ktTTTTTTSk..',
                '..ktTTTTTTSk..',
                '..ktTTSSTTSk..',
                '..ktTSkkSTSk..',
                '..ktSk..kSSk..',
                '..kkk....kkk..'],
               {'b': BRASS[5], 'B': BRASS[7], 'd': TEAL[1], 'k': OAK[0], 'T': TEAL[3], 't': TEAL[4], 'S': TEAL[2], 'y': VEL[7]}),
    # contracts: a rolled scroll tied with a red cord and sealed
    'contracts': (['..............',
                   '..............',
                   '..............',
                   '..kkkkkkkkkk..',
                   '.kELLLLrrLLEk.',
                   'kEeLLLLrrLLeEk',
                   'kEeMMMMrrMMeEk',
                   'kEeMMMwwwMMeEk',
                   'kEeSSwhwwdSeEk',
                   '.kESSwwwddSEk.',
                   '..kkkkdddkkk..',
                   '..............',
                   '..............',
                   '..............'],
                  {'k': OAK[0], 'E': VEL[7], 'e': VEL[3], 'L': VEL[6], 'M': VEL[5], 'S': VEL[3], 'r': WAX[4],
                   'w': WAX[3], 'h': WAX[6], 'd': WAX[1]}),
    # chapter: an open book with a ribbon marker
    'chapter': (['..............',
                 '..............',
                 '.kkkk....kkkk.',
                 'kPPPPkk.kPPPPk',
                 'kPllPPPkPPllPk',
                 'kPPPPllkPPPPPk',
                 'kPllPPPkPllPPk',
                 'kPPPPllkPPPllk',
                 'kPllPPPkPPPPPk',
                 'kSSSSSSkSSSSSk',
                 'kCCCCCkrkCCCCk',
                 '.kkkkkkrkkkkk.',
                 '.......rr.....',
                 '..............'],
                {'k': OAK[0], 'P': VEL[7], 'l': VEL[3], 'S': VEL[4], 'C': WAX[2], 'r': WAX[4]}),
    # friends: two people, the nearer one lit
    'friends': (['..............',
                 '........mmm...',
                 '.......mmmmm..',
                 '..lll..mmmmm..',
                 '.lllll..mmm...',
                 '.lllll........',
                 '..lll..mmmmm..',
                 '.......mmmmmm.',
                 'lllllll.mmmmm.',
                 'lllllll.mmmmm.',
                 'lllllll.mmmmm.',
                 'lllllll.......',
                 '..............',
                 '..............'],
                {'l': VEL[6], 'm': VEL[3]}),
}


def icon(name):
    rows, pal = ICONS[name]
    s = Spr(14, 14)
    s.grid(0, 0, rows, pal)
    return s


for _n in ICONS:
    ASSETS['icon-' + _n] = (lambda n: (lambda: icon(n)))(_n)


# ================================================================ count badge: a wax drop with room for one digit
def wax_badge():
    """a drop of red wax with room for one digit (the digit is text)"""
    rows = ['...kkkkk...',
            '..kHhfffk..',
            '.kHhffffsk.',
            'khhfffffssk',
            'khffffffssk',
            'kffffffffsk',
            'kfffffffssk',
            'kffffffsssk',
            '.kffssssdk.',
            '..ksssddk..',
            '...kkkkk...']
    s = Spr(11, 11)
    s.grid(0, 0, rows, {'k': WAX[0], 'H': WAX[6], 'h': WAX[5], 'f': WAX[3], 's': WAX[2], 'd': WAX[1]})
    return s


ASSETS['wax-badge'] = wax_badge


# ================================================================ portrait frame for a live model render
def frame_rim(size):
    """brass bezel around a portrait window. The window (size - 8 square, transparent here) is where the game
    draws the unit with InventoryScreen.drawEntity under a scissor; the rim overlaps the window by one pixel
    so the scissored model edge never shows."""
    s = Spr(size + 1, size + 2)
    for x in range(1, size + 1):
        s.set(x, size, rgba('#0a0505', 140)); s.set(x, size + 1, rgba('#0a0505', 60))
    for y in range(1, size):
        s.set(size, y, rgba('#0a0505', 110))
    n = size
    s.frame(0, 0, n, n, OAK[0])
    # ring 1, outer bevel
    s.hline(1, n - 2, 1, BRASS[6]); s.vline(1, 1, n - 2, BRASS[6])
    s.hline(1, n - 2, n - 2, BRASS[3]); s.vline(n - 2, 1, n - 2, BRASS[3])
    # ring 2, face
    s.hline(2, n - 3, 2, BRASS[5]); s.vline(2, 2, n - 3, BRASS[5])
    s.hline(2, n - 3, n - 3, BRASS[4]); s.vline(n - 3, 2, n - 3, BRASS[4])
    # ring 3, inner bevel: slopes toward the window, so top-left is dark and bottom-right lit
    s.hline(3, n - 4, 3, BRASS[2]); s.vline(3, 3, n - 4, BRASS[2])
    s.hline(3, n - 4, n - 4, BRASS[6]); s.vline(n - 4, 3, n - 4, BRASS[6])
    # window lip (one dark pixel, overlaps the render)
    s.frame(4, 4, n - 8, n - 8, BRASS[0])
    s.set(1, 1, BRASS[7]); s.set(2, 1, BRASS[7]); s.set(1, 2, BRASS[7])
    # corner studs
    stud = ['.hm.', 'hmmd', 'mmdd', '.dd.']
    for (cx, cy) in ((0, 0), (n - 4, 0), (0, n - 4), (n - 4, n - 4)):
        s.grid(cx, cy, stud, {'h': BRASS[7], 'm': BRASS[5], 'd': BRASS[2]})
    return s


def frame_back(size, empty=False, seed=5):
    """the window backdrop behind the live render: slate, lit softly behind the head (upper middle) and
    darker toward the shoulders, with organic step edges so it never reads as rings"""
    w = size - 8
    s = Spr(w, w)
    nz = Noise(seed + size, 5)
    tones = ['#141a1d', '#182024', '#1d262a', '#222d31', '#283439']
    if empty:
        tones = ['#120d0b', '#160f0c', '#1a120e', '#1d140f', '#21170f']
    for y in range(w):
        for x in range(w):
            dx = (x - (w - 1) / 2) / (w * 0.55); dy = (y - w * 0.36) / (w * 0.62)
            r = math.sqrt(dx * dx + dy * dy)
            v = (1.0 - r) * 3.6 + (nz(x, y) - 0.5) * 0.9
            t = max(0, min(4, int(v + 0.6)))
            s.set(x, y, tones[t])
    return s


for _sz in (40, 44, 48, 168):
    ASSETS['frame-rim-%d' % _sz] = (lambda z: (lambda: frame_rim(z)))(_sz)
    ASSETS['frame-back-%d' % _sz] = (lambda z: (lambda: frame_back(z)))(_sz)
ASSETS['frame-back-empty-44'] = lambda: frame_back(44, empty=True)


def invite_plus():
    """an engraved brass plus for the empty party seat"""
    rows = ['...hm...', '...hd...', '...hd...', 'hhhhmmmm', 'mmmmdddd', '...hd...', '...hd...', '...md...']
    s = Spr(8, 8)
    s.grid(0, 0, rows, {'h': BRASS[6], 'm': BRASS[4], 'd': BRASS[2]})
    return s


ASSETS['invite-plus'] = invite_plus


def ready_check():
    rows = ['.......kk',
            '......kGk',
            'kk...kGgk',
            'kGk.kGgk.',
            'kgGkGgk..',
            '.kgGgk...',
            '..kgk....',
            '...k.....']
    s = Spr(9, 8)
    s.grid(0, 0, rows, {'k': '#10241a', 'G': '#8fd3a0', 'g': '#4f9a64'})
    return s


ASSETS['ready-check'] = ready_check


# ================================================================ buttons
def brass_button(w, h, seed=2):
    """the primary action: a brushed brass plate with a lit top-left bevel, an engraved border, corner
    rivets and a shadow on the table"""
    rnd = random.Random(seed + w * 3 + h)
    s = Spr(w + 2, h + 3)
    for x in range(2, w + 2):
        for i, a in enumerate((0.55, 0.35, 0.18)):
            s.set(x, h + i, rgba('#0a0505', round(a * 255)))
    for y in range(2, h):
        s.set(w, y, rgba('#0a0505', 130)); s.set(w + 1, y, rgba('#0a0505', 60))
    s.rect(0, 0, w, h, BRASS[0])
    s.rect(1, 1, w - 2, h - 2, BRASS[5])
    # brushing: long, faint, horizontal; kept off the text band in the middle third
    for _ in range(int(w * h / 90)):
        y = rnd.randint(3, h - 4)
        if h * 0.3 < y < h * 0.7:
            continue
        x0 = rnd.randint(3, w - 10); ln = rnd.randint(5, 22)
        t = rnd.choice([BRASS[6], BRASS[4], BRASS[4]])
        for x in range(x0, min(w - 3, x0 + ln)):
            s.set(x, y, t)
    s.hline(1, w - 2, 1, BRASS[7]); s.vline(1, 1, h - 2, BRASS[6]); s.hline(2, w - 3, 2, BRASS[6])
    s.hline(1, w - 2, h - 2, BRASS[2]); s.vline(w - 2, 1, h - 2, BRASS[3]); s.hline(2, w - 3, h - 3, BRASS[3])
    # engraved border: a cut line with its lower-right wall catching the light
    e = 5
    s.hline(e, w - 1 - e, e, BRASS[3]); s.vline(e, e, h - 1 - e, BRASS[3])
    s.hline(e, w - 1 - e, h - 1 - e, BRASS[3]); s.vline(w - 1 - e, e, h - 1 - e, BRASS[3])
    s.hline(e + 1, w - 2 - e, e + 1, BRASS[6]); s.vline(e + 1, e + 1, h - 2 - e, BRASS[6])
    # rivets
    rv = ['hm.', 'mmd', '.dd']
    for (cx, cy) in ((2, 2), (w - 5, 2), (2, h - 5), (w - 5, h - 5)):
        s.grid(cx, cy, rv, {'h': BRASS[7], 'm': BRASS[4], 'd': BRASS[1]})
    return s


ASSETS['btn-brass-240x40'] = lambda: brass_button(240, 40)


def key_engraved(w, h=14):
    """a key cap engraved into brass: cut outline, its lower-right wall lit"""
    s = Spr(w, h)
    s.frame(0, 0, w, h, BRASS[2])
    s.hline(1, w - 2, h - 2, BRASS[6]); s.vline(w - 2, 1, h - 2, BRASS[6])
    s.hline(1, w - 2, 1, BRASS[4]); s.vline(1, 1, h - 2, BRASS[4])
    return s


for _w in range(13, 49):
    ASSETS['key-brass-%d' % _w] = (lambda w: (lambda: key_engraved(w)))(_w)


def vellum_button(w, h=20, seed=4):
    """the secondary action: a cut parchment tag, raised one pixel off the sheet it sits on"""
    rnd = random.Random(seed + w)
    s = Spr(w + 1, h + 2)
    for x in range(1, w + 1):
        s.set(x, h, rgba('#2a1d10', 120)); s.set(x, h + 1, rgba('#2a1d10', 50))
    for y in range(1, h):
        s.set(w, y, rgba('#2a1d10', 90))
    s.rect(0, 0, w, h, VEL[1])
    s.rect(1, 1, w - 2, h - 2, VEL[7])
    s.hline(1, w - 2, 1, VEL[8]); s.vline(1, 1, h - 2, VEL[8])
    s.hline(1, w - 2, h - 2, VEL[5]); s.vline(w - 2, 2, h - 2, VEL[5])
    for _ in range(max(2, w // 18)):
        x = rnd.randint(4, w - 6); y = rnd.choice([3, h - 4])
        s.set(x, y, VEL[6]); s.set(x + 1, y, VEL[6])
    for (x, y) in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        s.set(x, y, None)
    return s


for _w in (64, 72, 80, 88):
    ASSETS['btn-vellum-%dx24' % _w] = (lambda w: (lambda: vellum_button(w, 24)))(_w)


def quill():
    """a goose quill in ink: the vane lit on its upper edge, barbs ticked, the bare shaft and nib"""
    rows = ['.........kk.',
            '........kvLk',
            '.......kvvLk',
            '......kvvLk.',
            '.....kvvLk..',
            '....kvvLk...',
            '...kbvLk....',
            '...ksk.k....',
            '..ksk.......',
            '.ksk........',
            'kk..........']
    s = Spr(12, 11)
    s.grid(0, 0, rows, {'k': INK[1], 'v': INK[4], 'L': VEL[8], 'b': INK[3], 's': INK[3]})
    return s


ASSETS['quill'] = quill
