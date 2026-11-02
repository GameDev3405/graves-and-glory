"""Graveborn shared ramps (ART-STYLE-GUIDE 10.6), darkest first, derived from the spec's Look bases; plus a per-skin
violet check. Import: sys.path.insert(0, <art/hand/common>); import graveborn as GB.

The House accent is VIOLET (pale violet, guide 10.1). tools/art_hue_check.py counts only texels with saturation >= 0.30
and value >= 0.12; of VIOLET only tones 0 to 3 count (tone 4 and 5 are too grey), so put tones 1 to 3 where they show:
hems, bindings, wraps, cords, grips, mask trim. Moss and rust are saturated too and compete with it.
"""
import colorsys
from PIL import Image
from handlib import ramp

VIOLET = ramp("#1E1528", "#362645", "#54406A", "#7A6494", "#A28FBA", "#D2C6D8")   # House accent (fixed paint)
BONE   = ramp("#3A3328", "#5E5544", "#857B66", "#A89F87", "#C9C1A8", "#E4DECB")   # bare bone, skulls
MOSS   = ramp("#161A12", "#262E1E", "#36402A", "#4F5A3C", "#6A7552", "#87906A")   # grave-moss rags
RUST   = ramp("#1E130D", "#34221A", "#4E3324", "#6B4A33", "#8A6444", "#A8805A")   # rusted mail and coif
GIRON  = ramp("#0C0B0C", "#171516", "#221F1E", "#2B2826", "#3D3936", "#55504B", "#746D66")   # black grave-iron
FROST  = ramp("#26323A", "#3E4E57", "#5C6F78", "#7A8E96", "#93A7AE", "#B8C8CC", "#DCE3E2")   # frost grey to rime
SOUL   = ramp("#3E5A6E", "#7FA8C4", "#CFEFFF", "#F2FCFF")                             # soul-light (glow only)
GILT   = ramp("#2E1E0E", "#482E16", "#6F5026", "#96783A", "#BDAA6A")                  # funeral gilt (mask, jaw wire)
ROT    = ramp("#141210", "#221E1A", "#332C24", "#4A4134", "#625746", "#7A6E5A")      # rotted burial cloth
GSKIN  = ramp("#1E231D", "#2A3028", "#3E463A", "#56604F", "#6F7A66", "#8A9480")      # Risen grey-green flesh

def violet_share(png, centre=269, band=22):
    """Share of a skin's saturated weight inside the violet band (art_hue_check counts >= 0.10 over the House)."""
    tot = inb = 0.0
    for r, g, b, a in Image.open(png).convert("RGBA").getdata():
        if a < 200: continue
        h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
        if s < 0.30 or v < 0.12: continue
        w = s * v; tot += w
        d = abs(int(h * 360) - centre) % 360
        if min(d, 360 - d) <= band: inb += w
    return inb / tot if tot else 0.0

if __name__ == "__main__":
    import sys
    for p in sys.argv[1:]: print("%-40s violet %3d%%" % (p, violet_share(p) * 100))
