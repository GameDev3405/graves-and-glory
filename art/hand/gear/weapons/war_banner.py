"""Marchwarden's Standard (war_banner): a held standard, never worn on the back. An oak pole with a spear finial, a
crossbar toward +x (backward in the hand, so the flag trails), and a swallow-tailed flag in the side's colour with a gold
sun device. Rare hems it, Epic hangs a tassel with a stone, Legendary sets a lit sun proud on both faces."""
from weaponlib import *

def sun(x, y, w, h):
    cx, cy = w / 2 - 1.3, h / 2 - 0.5
    d = (x - cx) ** 2 + (y - cy) ** 2
    return GOLD[4] if d <= 2.3 else GOLD[3] if d <= 5 and (x + y) % 2 == 0 else None

def tail(x, y, w, h): return x >= w - 2 and abs(y - (h - 1) / 2) < (x - (w - 3)) * 1.2

def make(r, side):
    F = fit_of(r)
    c = [Cube("pole", [-0.8, -14, -0.8], [0.8, 20, 0.8], "wood", grain(OAK, 3)),
         Cube("ferrule", [-1, -15.5, -1], [1, -13.95, 1], "iron", tone(F, 2)),
         Cube("crossbar", [0.75, 17.5, -0.5], [9.5, 18.5, 0.5], "wood", grain(OAK, 3, salt=4)),
         Cube("bar_cap", [9.45, 17.3, -0.7], [10.2, 18.7, 0.7], "iron", tone(F, 3, lead=False)),
         Cube("flag", [0.85, 8, -0.3], [9, 17.55, 0.3], "cloth", cloth(side, 2, device=sun, cut=tail)),
         Cube("socket", [-1, 19.5, -1], [1, 20.5, 1], "iron", tone(F, 3)),
         Cube("finial", [-0.5, 20.45, -0.5], [0.5, 22.5 if r < EPIC else 23.5, 0.5], "steel", tone(F, 4, lead=False))]
    if r >= UNCOMMON: c.append(Cube("hem", [0.85, 7.4, -0.35], [7.2, 8.05, 0.35], "cloth", tone(F, 3, lead=False)))
    c += bead("bead", [-0.4, 19.6, -1.2], [0.4, 20.4, 1.2], r)
    if r >= RARE: c.append(Cube("tassel", [9.6, 13.5, -0.3], [10.1, 17.35, 0.3], "cloth", tone(GOLD if r == LEGEND else LINEN, 3, lead=False)))
    if r >= EPIC:
        c.append(Cube("bar_knob", [10.15, 17.5, -0.5], [11, 18.5, 0.5], "iron", tone(F, 4, lead=False)))
        c += bead("tassel_gem", [9.45, 12.6, -0.4], [10.25, 13.55, 0.4], r)
    if r == LEGEND:
        c += [Cube("wing_l", [-2.2, 20.45, -0.3], [-0.45, 21.4, 0.3], "gilt", tone(GOLD, 4, lead=False)),
              Cube("wing_r", [0.45, 20.45, -0.3], [2.2, 21.4, 0.3], "gilt", tone(GOLD, 4, lead=False)),
              Cube("sun", [3.0, 11.2, -0.45], [5.4, 13.6, 0.45], "gem", stone(SUN), lit=W.FACES)]
    return c

def weapon(): return Weapon("war_banner", make, "staff", show_side=True, note="held standard, swallow-tail flag in the side colour")
