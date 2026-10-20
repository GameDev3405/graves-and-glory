"""Write the three Chapter plaque mockups (static HTML) from one description of the Founders chapter, so
the three states share every coordinate. Run from work/ui-concepts:  python track/pages.py
Then render each through render.py (see NOTES.md)."""
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))

# ---------------------------------------------------------------- the chapter (Houses spec 4.8, re-authored)
GOLD = {1, 3, 7, 9, 11, 13, 17, 19, 23, 27}
SHARDS = {2, 4, 6, 8, 12, 14, 16, 18, 22, 24, 26, 28}
TITLES = {5: 'Scout', 10: 'Pathfinder', 15: 'Captain', 25: 'Veteran', 30: 'Bannerlord'}
# the Oath row (round 2): the sworn House's things for the player's own commander and banner, never a troop's
# (the veteran livery and brass frames per troop belong to mastery, Houses spec 4.7). A plaque under every
# tier: doubled shards on the shard tiers, the House portrait frame at 10, the cloak at 20, the title at 30,
# and on the odd tiers the tabard, pennant patterns and banner cloths. Here for the Gilded Order.
OATH_ODD = {1: ('pennant-fess', 'Pennant', 'Fess'), 3: ('pennant-chief', 'Pennant', 'Chief'),
            5: ('pennant-bordure', 'Pennant', 'Bordure'), 7: ('pennant-saltire', 'Pennant', 'Saltire'),
            9: ('pennant-quarterly', 'Pennant', 'Quarterly'), 11: ('tabard', 'Tabard', 'livery'),
            13: ('pennant-pale', 'Pennant', 'pattern'), 15: ('cloth', 'Banner', 'cloth'),
            17: ('pennant-bend', 'Pennant', 'pattern'), 19: ('pennant-cross', 'Pennant', 'pattern'),
            21: ('cloth-gold', 'Banner', 'cloth'), 23: ('pennant-gyronny', 'Pennant', 'pattern'),
            25: ('cloth-crowns', 'Banner', 'cloth'), 27: ('pennant-pily', 'Pennant', 'pattern'),
            29: ('cloth-night', 'Banner', 'cloth')}


def oath(t):
    """(icon stem, line 1, line 2) for the Oath's reward at tier t"""
    if t in SHARDS:
        return 'shards', 'Shards', '+10'
    if t == 10:
        return 'frame', 'Portrait', 'frame'
    if t == 20:
        return 'cloak', 'Cloak', 'livery'
    if t == 30:
        return 'title', 'Oathsworn', 'title'
    return OATH_ODD[t]


def roll(t, sworn):
    """(icon, line 1, line 2) for the Roll's reward at tier t"""
    if t in GOLD:
        return 'ic-coins', 'Gold', '30'
    if t in SHARDS:
        return ('ic-shards' if sworn else 'ic-shards-plain'), 'Shards', '10'
    if t in TITLES:
        return 'ic-title', TITLES[t], 'title'
    if t == 20:
        return 'ic-letter', 'Rare troop', 'your pick'
    if t == 21:
        return 'ic-pennant', 'Pennant', 'pattern'
    return 'ic-banner-frame', 'Banner frame', None


# ---------------------------------------------------------------- geometry
# plaques 68 x 80 on a 76 pitch: 8 px between them, as every group on the table. The current tier's pair
# hangs lifted (the Roll's 8 px, the Oath's 4) and 8 px taller (68 x 88), its brass polished and its shadow
# deeper; it keeps the same width, so it clears its neighbours by the same 8 px
X0, PITCH, PW, PH = 136, 76, 68, 80
RW, RH = 68, 88
ROD_Y, TAG_Y, ROLL_Y, OATH_Y = 296, 294, 320, 408

STATES = {
    'v4-track-01-sworn': dict(first=10, current=11, claimed=10, sworn=True, hero=20, claim=True, fresh=False,
                              title='a Gilded-sworn player at tier 11, tier 11 to claim'),
    'v4-track-02-unsworn': dict(first=10, current=11, claimed=10, sworn=False, hero=20, claim=True, fresh=False,
                                title='the same player, sworn to no House: the Oath row in sepia'),
    'v4-track-03-claim': dict(first=11, current=11, claimed=11, sworn=True, hero=21, claim=False, fresh=True,
                              title='the moment after Claim: tier 11 stamped, the plate dull, the hero moved on'),
}

HEROES = {
    20: dict(kicker='Tier 20', name='Order Longbow', kind='Rare ranged, your pick', seal='seal-rare',
             line='One arrow, two men. The Order wastes nothing.', art='pt-longbow-160', change=True,
             what='the chosen Rare troop: the live model at 8 px per model px, as recruit draws it'),
    21: dict(kicker='Tier 21', name='Cloth of Gold', kind='Banner cloth, Gilded Order', seal='seal-sword',
             line='Your banner in the Order\'s gold. Only the sworn fly it.', art='cloth-gold-160', change=False,
             what='the Oath\'s banner cloth on its pole, as the banner builder will show it'),
}


def esc(s):
    return s.replace('&', '&amp;').replace('<', '&lt;')


def plaque_html(t, row, st, k):
    """one hanging plaque: row is 'roll' or 'oath'"""
    sworn = st['sworn']
    x = X0 + PITCH * k
    sep = row == 'oath' and not sworn
    # the current tier's plaques hang lifted and larger, pulled toward the viewer; an Oath plaque the player
    # cannot claim (not sworn) stays in the row
    cur = t == st['current'] and not sep
    w, h = (RW, RH) if cur else (PW, PH)
    if cur:
        x -= (RW - PW) // 2
    y = (ROLL_Y - 8) if (cur and row == 'roll') else ((OATH_Y - 4) if cur else (ROLL_Y if row == 'roll' else OATH_Y))
    cx = w // 2
    claimed = t <= st['claimed']
    locked = t > st['current']
    if row == 'roll':
        icon, l1, l2 = roll(t, sworn)
    else:
        stem, l1, l2 = oath(t)
        icon = 'ic-title' if stem == 'title' else 'ic-o-' + stem
        if sep:
            icon += '-sepia'
    if sep:
        pl = 'plaque-tier-sepia'
    elif cur:
        pl = 'plaque-tier-raised'
    elif locked:
        pl = 'plaque-tier-dull' if (t + (row == 'oath')) % 2 else 'plaque-tier-dull-b'
    else:
        pl = 'plaque-tier' if t % 2 else 'plaque-tier-b'
    state = 'sepia, not sworn' if sep else ('claimed' if claimed else ('to claim' if cur else 'not reached'))
    out = []
    # chains: from the rod to the Roll plaque, from the Roll plaque to the Oath plaque
    ch = 'chain-sepia-10' if sep else ('chain-dull-%d' if locked else 'chain-%d')
    if row == 'roll':
        n = 10 if cur else 18
        for chx in (x + 8, x + w - 14):
            out.append('<img class="s" src="art/%s.png" style="left:%dpx;top:%dpx" alt="">' % (ch % n, chx, ROD_Y + 6))
    else:
        for chx in (x + 8, x + w - 14):
            src = ch if sep else ch % (6 if cur else 10)
            out.append('<img class="s" src="art/%s.png" style="left:%dpx;top:%dpx" alt="">' % (src, chx, y - (7 if cur else 11)))
    out.append('<div class="tp" data-ctl data-name="tier %d %s, %s" style="left:%dpx;top:%dpx;width:%dpx;height:%dpx">' % (t, row, state, x, y, w, h))
    out.append('  <img class="s" src="art/%s.png" style="left:0;top:0" data-under-ok alt="">' % pl)
    # one label line across each row (47 px under the row's top); the icon centred in the space above it
    ly = 47 + (ROLL_Y if row == 'roll' else OATH_Y) - y
    iw, ih = Image.open(os.path.join(HERE, 'art', icon + '.png')).size
    out.append('  <img class="s" src="art/%s.png" style="left:%dpx;top:%dpx" data-under-ok alt="">' % (icon, round(cx - iw / 2), round((ly + 3) / 2 - ih / 2)))
    if locked and not sep:
        out.append('  <img class="s" src="art/hasp.png" style="left:-1px;top:%dpx" data-under-ok alt="" data-name="iron hasp">' % (ly - 12))
    tone = 'sepia-l' if sep else 'ivory'
    out.append('  <span class="a h11 %s" data-cx="%d" style="top:%dpx">%s</span>' % (tone, cx, ly, esc(l1)))
    if l2:
        out.append('  <mc-text class="a" data-cx="%d" style="top:%dpx" color="%s">%s</mc-text>' % (cx, ly + 15, '#b9a77f' if sep else '#afb9bc', esc(l2)))
    if claimed and not sep:
        stamp = 'stamp-hollow-fresh' if (st['fresh'] and t == st['claimed']) else ('stamp-hollow' if t % 2 else 'stamp-hollow-b')
        out.append('  <img class="s" src="art/%s.png" style="left:%dpx;top:-3px" alt="" data-name="the Hollow\'s mark">' % (stamp, w - 29))
    out.append('</div>')
    return out


def page(name, st):
    hero = HEROES[st['hero']]
    first = st['first']
    L = []
    A = L.append
    A('<!doctype html>')
    A('<html class="v2 v3"><head><meta charset="utf-8"><title>Chapter plaque: %s</title>' % st['title'])
    A('<link rel="stylesheet" href="../common.css"><link rel="stylesheet" href="../v3.css">')
    A('<style>')
    A('/* The Chapter plaque (the Founders reward track), round 2. Same rail, plaques and plates as the accepted')
    A('   home (v4.2) and recruit (v4.1) screens, on this page\'s own stretch of the table (the kit\'s oak with its')
    A('   own seed, and no brass fittings on it). Top left, the hero: the next big reward the track does')
    A('   not yet show, on a letter as recruit shows a troop. Top right, the chapter: its name, the tier on a wax')
    A('   seal, the XP as brass poured along a rail, one Claim plate. Below, the track: tiers hang from a brass')
    A('   rod, a red wax pin on the rod over the current tier; the Roll above (everyone), the Oath below (the')
    A('   sworn House\'s things for the player\'s own commander and banner); claimed tiers carry the Hollow\'s mark')
    A('   in red wax, the current pair hangs lifted, tiers not reached sit under an iron hasp. No table props')
    A('   on this page (Kort, approving round 2): the candle and the ink pot stand on other pages.')
    A('   Generated by track/pages.py; edit there. */')
    A('.plq{position:absolute;top:12px;height:28px}')
    A('.plq .ic{left:8px;top:7px}')
    A('.plq .h11{position:absolute;left:28px;top:8px}')
    A('.plq .bd{top:9px}')
    A('.tp{position:absolute;width:68px;height:80px}')
    A('.v3 .sepia-l{color:#c4b28a}')
    A('.v3 .brass-hi{color:#f0d9a0}')
    A('</style></head>')
    A('<body>')
    A('<img class="s" src="art/oak-table-track.png" style="left:0;top:0" data-bleed alt="">')
    A('<img class="s" src="../v4/rail.png" style="left:0;top:0" data-bleed alt="">')
    A('')
    A('<!-- the rail, as home has it: the way back (Esc) on the left, the camp\'s plaques in their places; the')
    A('     Chapter plaque lit, the page you are on -->')
    A('<div class="plq" data-ctl data-name="back" style="left:56px;width:120px">')
    A('  <img class="s" src="../v4/plaque-120.png" style="left:0;top:0" data-under-ok alt="">')
    A('  <img class="s ic" src="../v4/icon-table.png" alt="">')
    A('  <span class="h11 ivory">War table</span>')
    A('  <img class="s" src="../v4/key-brass-27.png" style="left:85px;top:7px" data-under-ok alt="">')
    A('  <mc-text class="a" style="left:89px;top:10px" color="#f0d9a0">Esc</mc-text>')
    A('</div>')
    for (nm, x, icon, label, badge) in (('recruit', 184, 'icon-recruit', 'Recruit', None),
                                        ('banner', 304, 'icon-banner', 'Banner', None),
                                        ('contracts', 424, 'icon-contracts', 'Contracts', '2'),
                                        ('chapter', 544, 'icon-chapter', 'Chapter', None),
                                        ('friends', 664, 'icon-friends', 'Friends', '1')):
        here = nm == 'chapter'
        A('<div class="plq" data-ctl data-name="%s%s" style="left:%dpx;width:112px">' % (nm, ' (open)' if here else '', x))
        A('  <img class="s" src="%s" style="left:0;top:0" data-under-ok alt="">' % ('art/plaque-112-here.png' if here else '../v4/plaque-112.png'))
        A('  <img class="s ic" src="../v4/%s.png" alt=""><span class="h11 %s">%s</span>' % (icon, ('brass-hi' if here else 'ivory'), label))
        if badge:
            A('  <img class="s bd" src="../v4/wax-badge.png" style="left:93px;top:8px" data-under-ok alt=""><mc-text class="a" style="left:96px;top:10px" color="#f1e7cb">%s</mc-text>' % badge)
        A('</div>')
    A('')
    # ------------------------------------------------ the hero
    A('<!-- the hero: the next big reward the track does not yet show (%s) -->' % hero['what'])
    A('<img class="s" src="art/hero-letter.png" style="left:56px;top:72px" data-under-ok alt="">')
    A('<div class="box" data-panel data-name="hero letter" style="left:56px;top:72px;width:400px;height:200px">')
    A('  <img class="s" src="../v4/frame-back-160.png" style="left:20px;top:20px" alt="">')
    A('  <img class="s" src="art/%s.png" style="left:20px;top:20px" alt="" data-name="live render">' % hero['art'])
    A('  <img class="s" src="../v4/frame-ornate-184.png" style="left:8px;top:8px" alt="" data-name="reveal frame">')
    A('  <mc-text class="a" data-cx="300" style="top:34px" color="#5c4a32">%s</mc-text>' % hero['kicker'])
    A('  <span class="a t16 ink" data-cx="300" style="top:48px">%s</span>' % esc(hero['name']))
    A('  <span class="a b11 nw sepia" data-cx="300" style="top:70px">%s</span>' % esc(hero['kind']))
    A('  <span class="a b11 ink" style="left:220px;top:92px;width:160px;text-align:center">%s</span>' % esc(hero['line']))
    if hero['change']:
        A('  <div class="box" data-ctl data-name="change pick" style="left:260px;top:160px;width:80px;height:24px">')
        A('    <img class="s" src="../v4/btn-vellum-80x24.png" style="left:0;top:0" data-under-ok alt="">')
        A('    <img class="s" src="../v4/quill.png" style="left:8px;top:6px" data-under-ok alt="">')
        A('    <span class="a h11 ink" style="left:24px;top:6px">Change</span>')
        A('  </div>')
    A('</div>')
    A('<img class="s" src="art/%s.png" style="left:340px;top:60px" alt="" data-name="kind seal">' % hero['seal'])
    A('')
    # ------------------------------------------------ the chapter: name, tier seal, XP rail, Claim
    A('<!-- the chapter: no end date (the Founders never close); the row under the name is kept for the clock')
    A('     later chapters carry -->')
    A('<mc-text class="a" style="left:480px;top:80px" color="#afb9bc">Chapter 1</mc-text>')
    A('<span class="a h11 brass" style="left:480px;top:92px">The Founders</span>')
    A('<img class="s" src="art/seal-tier-big.png" style="left:480px;top:144px" alt="" data-name="tier seal">')
    A('<mc-text class="a" data-cx="502" style="top:162px" color="#f1e7cb">11</mc-text>')
    A('<img class="s" src="art/xp-rail.png" style="left:536px;top:156px" alt="" data-name="XP rail">')
    A('<mc-text class="a" style="left:536px;top:184px" color="#eee9df">2,870 / 7,500 XP</mc-text>')
    A('<mc-text class="a" data-rx="904" style="top:184px" color="#afb9bc">130 XP to tier 12</mc-text>')
    if st['claim']:
        A('<div class="box" data-ctl data-name="claim" style="left:664px;top:232px;width:240px;height:40px">')
        A('  <img class="s" src="../v4/btn-brass-240x40.png" style="left:0;top:0" data-under-ok alt="">')
        A('  <img class="s" src="../v4/key-brass-37.png" style="left:14px;top:13px" data-under-ok alt="">')
        A('  <mc-text class="a" style="left:18px;top:16px" color="#3a2a12">Enter</mc-text>')
        A('  <span class="a h11" data-cx="120" style="top:14px;color:#1e1509">Claim</span>')
        A('</div>')
    else:
        A('<div class="box" data-ctl data-name="claim (nothing to claim)" style="left:664px;top:232px;width:240px;height:40px">')
        A('  <img class="s" src="art/btn-dull-240x40.png" style="left:0;top:0" data-under-ok alt="">')
        A('  <img class="s" src="art/key-dull-37.png" style="left:14px;top:13px" data-under-ok alt="">')
        A('  <mc-text class="a" style="left:18px;top:16px" color="#b3ad9f">Enter</mc-text>')
        A('  <span class="a h11 muted" data-cx="120" style="top:14px">Claim</span>')
        A('</div>')
    A('')
    # ------------------------------------------------ the track
    A('<!-- the track: ten tiers on the table, the rest off either end of the rod; the row names on the left -->')
    A('<mc-text class="a" style="left:56px;top:%dpx" color="#afb9bc">Tier</mc-text>' % (ROD_Y + 2))
    A('<span class="a h11 brass" style="left:56px;top:%dpx">The Roll</span>' % (ROLL_Y + 28))
    A('<mc-text class="a" style="left:56px;top:%dpx" color="#afb9bc">for all</mc-text>' % (ROLL_Y + 44))
    if st['sworn']:
        A('<span class="a h11 brass" style="left:56px;top:%dpx">The Oath</span>' % (OATH_Y + 28))
        A('<mc-text class="a" style="left:56px;top:%dpx" color="#afb9bc">Gilded Order</mc-text>' % (OATH_Y + 44))
    else:
        A('<span class="a h11 sepia-l" style="left:56px;top:%dpx">The Oath</span>' % (OATH_Y + 28))
        A('<mc-text class="a" style="left:56px;top:%dpx" color="#afb9bc">not sworn</mc-text>' % (OATH_Y + 44))
    A('<img class="s" src="art/track-rod.png" style="left:130px;top:%dpx" alt="" data-name="rod">' % ROD_Y)
    A('<div class="box" data-ctl data-name="earlier tiers" style="left:116px;top:%dpx;width:15px;height:15px">' % (ROD_Y - 3))
    A('  <img class="s" src="art/arrow-left.png" style="left:0;top:0" alt=""></div>')
    A('<div class="box" data-ctl data-name="later tiers" style="left:889px;top:%dpx;width:15px;height:15px">' % (ROD_Y - 3))
    A('  <img class="s" src="art/arrow-right.png" style="left:0;top:0" alt=""></div>')
    # the current tier's column last, so its lifted plaques cast their shadow over their neighbours
    for k in sorted(range(10), key=lambda k: first + k == st['current']):
        t = first + k
        for line in plaque_html(t, 'roll', st, k):
            A(line)
        for line in plaque_html(t, 'oath', st, k):
            A(line)
    # the tier tags last, so they sit over the chains' tops on the rod; the current tier carries a plain
    # red wax pin instead of its number (the chapter's seal carries the number)
    for k in range(10):
        t = first + k
        x = X0 + PITCH * k
        if t == st['current']:
            A('<img class="s" src="art/pin-tier.png" style="left:%dpx;top:%dpx" alt="" data-name="wax pin, current tier">' % (x + 25, ROD_Y - 4))
        else:
            A('<img class="s" src="art/tag-tier.png" style="left:%dpx;top:%dpx" alt="" data-name="tier tag">' % (x + 24, TAG_Y))
            A('<mc-text class="a" data-cx="%d" style="top:%dpx" color="#1e1509">%d</mc-text>' % (x + 35, TAG_Y + 4, t))
    if not st['sworn']:
        A('<span class="a b11 nw muted" style="left:136px;top:%dpx">Swear to a House at the recruit table</span>' % (OATH_Y + 92))
    A('')
    A('<script src="../mc.js"></script><script src="../art.js"></script><script src="../v2.js"></script><script src="../check.js"></script>')
    A('</body></html>')
    return '\n'.join(L) + '\n'


if __name__ == '__main__':
    for name, st in STATES.items():
        with open(os.path.join(HERE, name + '.html'), 'w', encoding='utf-8', newline='\n') as f:
            f.write(page(name, st))
        print('wrote track/' + name + '.html')
