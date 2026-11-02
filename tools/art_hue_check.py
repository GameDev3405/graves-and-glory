"""Client-free House accent hue check (ART-STYLE-GUIDE 10.1 accents, ruled 2026-10-30).
Per House, pools the saturated texels of its built troop skins and reports whether the House's band holds a real share of its saturated texels (the peak hue is printed).
Band centres: gold 42, ochre 32, ember 13, sea blue 210, violet 269 (ramps in the guide), half-width BAND degrees.
Reads textures only; changes nothing. Exit 1 when a House with skins is out of band."""
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

def main():
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
