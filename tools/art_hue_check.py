"""Client-free House accent hue check (ART-STYLE-GUIDE 10.1 accents, ruled 2026-10-30).
Per House, pools the saturated texels of its built troop skins and reports whether the House's band holds a real share of its saturated texels (the peak hue is printed).
Band centres: gold 42, ochre 32, ember 13, sea blue 210, violet 269 (ramps in the guide), half-width BAND degrees.
Reads textures only; changes nothing. Exit 1 when a House with skins is out of band.

  python tools/art_hue_check.py                                  every House, from the installed troop skins
  python tools/art_hue_check.py --dir <preview dir> --house <h>  one builder's preview skins (skin_s*.png) against House h
A new House adds its accent hue to CENTRES (docs/art/TEXTURE-RECIPE.md, part B)."""
import colorsys, json, math, pathlib, sys
from PIL import Image
ROOT = pathlib.Path(__file__).resolve().parents[1]
TROOPS = ROOT / 'src/main/resources/assets/gravesandglory/textures/entity/troop'
BAND = 22
MIN_SHARE = 0.10   # leather and skin browns share the warm hues, so the peak alone cannot decide; the band must hold this share
CENTRES = {'gilded': 42, 'graveborn': 269, 'wildmoot': 32, 'ironclad': 13, 'tidecallers': 210}
MIN_SAT, MIN_VAL, MIN_TEXELS = 0.30, 0.12, 20   # below these a texel is grey/metal/shadow, not accent

def dist(a, b):
    d = abs(a - b) % 360
    return min(d, 360 - d)

def dominant(files, CENTRE):
    bins = [0.0] * 360; n = 0
    for f in files:
        im = Image.open(f).convert('RGBA')
        for r, g, b, a in im.get_flattened_data():
            if a < 200: continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s < MIN_SAT or v < MIN_VAL: continue
            bins[int(h * 360) % 360] += s * v; n += 1
    if n < MIN_TEXELS: return None, n, 0
    # smoothed peak (+-10 degrees) so one tone step does not decide it
    share = sum(bins[(c + k) % 360] for c in [CENTRE] for k in range(-BAND, BAND + 1)) / sum(bins)
    best = max(range(360), key=lambda c: sum(bins[(c + k) % 360] for k in range(-10, 11)))
    return best, n, share

def check_dir(d, house):
    """The preview skins of one builder (skin_s*.png in d) against the House's band; 0 pass, 1 fail."""
    if house not in CENTRES: print('no accent band for House %r (have %s)' % (house, ', '.join(sorted(CENTRES)))); return 1
    files = sorted(pathlib.Path(d).glob('skin_s*.png'))
    if not files: print('no skin_s*.png in %s (run the builder\'s preview first)' % d); return 1
    peak, n, share = dominant(files, CENTRES[house])
    if peak is None:
        print('%-12s FAIL     only %d accent texels in %d skins (need %d)' % (house, n, len(files), MIN_TEXELS)); return 1
    ok = share >= MIN_SHARE
    print('%-12s %-8s accent in band %2d%%, peak hue %3d, band %d+-%d, %d skins, %d texels (%s)' % (house, 'PASS' if ok else 'FAIL', share * 100, peak, CENTRES[house], BAND, len(files), n, d))
    return 0 if ok else 1

def main():
    if '--dir' in sys.argv:
        a = sys.argv
        if '--house' not in a: print('--dir needs --house'); return 2
        return check_dir(a[a.index('--dir') + 1], a[a.index('--house') + 1])
    houses = json.loads((ROOT / 'sheets/design.json').read_text(encoding='utf8'))['houses']
    bad = 0
    for h in houses:
        files = []
        for t in h['troops'].split(','):
            files += [p for p in (TROOPS / t).glob(t + '_*.png') if not p.stem.endswith(('_n', '_s', '_e', '_o'))]
        c = CENTRES[h['id']]
        peak, n, share = dominant(files, c)
        if peak is None:
            print('%-12s NO DATA   %d skins, %d accent texels (band %d+-%d)' % (h['id'], len(files), n, c, BAND)); continue
        ok = share >= MIN_SHARE
        bad += not ok
        print('%-12s %-8s accent in band %2d%%, peak hue %3d, band %d+-%d, %d skins, %d texels' % (h['id'], 'PASS' if ok else 'FAIL', share * 100, peak, c, BAND, len(files), n))
    return 1 if bad else 0
if __name__ == '__main__': sys.exit(main())
