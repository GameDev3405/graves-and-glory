"""The locked House palettes for hand builders: ramps(house) and require(house, *names) read art/palettes/<house>.json.

  import palette
  R = palette.require("graveborn", "graveborn.BONE", "graveborn.VIOLET")   # {name: [(r, g, b, 255), ...]} dark to light
  BONE = R["graveborn.BONE"]

house is gilded, graveborn, wildmoot, ironclad, tidecallers or team (the team cloth palette). A name may be given in
full ("graveborn.BONE") or as its last part ("BONE") when that is unambiguous. Colours that sit in no code ramp are in
loose(house). Check a build with: python tools/art/house_palette.py check --dir <preview dir> --house <house>
"""
import json, os

PAL_DIR = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "palettes"))
_cache = {}

def _load(house):
    if house not in _cache:
        path = os.path.join(PAL_DIR, house + ".json")
        if not os.path.exists(path):
            have = sorted(f[:-5] for f in os.listdir(PAL_DIR) if f.endswith(".json")) if os.path.isdir(PAL_DIR) else []
            raise KeyError("no palette for house %r (have: %s)" % (house, ", ".join(have)))
        with open(path, encoding="utf-8") as f: _cache[house] = json.load(f)
    return _cache[house]

def _rgba(h): h = h.lstrip("#"); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)

def ramps(house):
    """{ramp name: [RGBA, ...]} for the house, each ramp dark to light (only the shades the approved textures use)."""
    return {n: [_rgba(e["hex"]) for e in r["colors"]] for n, r in _load(house)["ramps"].items()}

def loose(house):
    """RGBA colours of the house palette that belong to no code ramp."""
    return [_rgba(e["hex"]) for e in _load(house)["loose"]]

def require(house, *names):
    """The named ramps as {name: [RGBA...]}; raises KeyError listing what exists if any is missing or ambiguous."""
    have = ramps(house)
    out = {}
    for n in names:
        if n in have: out[n] = have[n]; continue
        hits = [k for k in have if k.endswith("." + n)]
        if len(hits) == 1: out[n] = have[hits[0]]; continue
        why = "ambiguous (%s)" % ", ".join(hits) if hits else "missing"
        raise KeyError("palette %s: ramp %r is %s; available: %s" % (house, n, why, ", ".join(sorted(have))))
    return out
