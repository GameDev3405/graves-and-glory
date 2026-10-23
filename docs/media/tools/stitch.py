"""Stitch the per-yaw panorama stills of MediaShoot into one cylindrical panorama.
usage: python stitch.py <screenshots dir> <scene>-pano <out.png> [vfov=50] [span=180]
Frames are named <prefix>-+015.png (yaw offset in degrees from the facing). The camera only rotates about its own centre,
so each frame maps straight onto the cylinder with no feature matching; overlaps are feather-blended."""
import sys, glob, os, re
import numpy as np
import cv2

d, prefix, out = sys.argv[1:4]
vfov = float(sys.argv[4]) if len(sys.argv) > 4 else 50.0
span = float(sys.argv[5]) if len(sys.argv) > 5 else 180.0

frames = []
for f in glob.glob(os.path.join(d, prefix + '-*.png')):
    m = re.search(r'-([+-]\d+)\.png$', f)
    if m:
        frames.append((int(m.group(1)), f))
frames.sort()
if not frames:
    sys.exit('no frames for ' + prefix)

first = cv2.imread(frames[0][1])
H, W = first.shape[:2]
f = (H / 2) / np.tan(np.radians(vfov / 2))          # focal length in pixels
PW = int(round(np.radians(span) * f))                # panorama width for the span
band = 0.97 * np.tan(np.radians(vfov / 2))           # usable cylinder height, in focal lengths either side
PH = int(round(2 * band * f))

phi = (np.arange(PW) - PW / 2) / f                    # azimuth of each column, radians from the centre
h = (np.arange(PH) - PH / 2) / f                      # height of each row on the unit cylinder
acc = np.zeros((PH, PW, 3), np.float64)
wsum = np.zeros((PH, PW), np.float64)

for off, path in frames:
    img = cv2.imread(path).astype(np.float64)
    alpha = phi[None, :] - np.radians(off)
    alpha = (alpha + np.pi) % (2 * np.pi) - np.pi
    valid = np.abs(alpha) < np.radians(80)
    cosa = np.cos(alpha)
    xs = f * np.tan(alpha) + W / 2
    ys = f * h[:, None] / np.maximum(cosa, 1e-6) + H / 2
    xs = np.broadcast_to(xs, (PH, PW)).astype(np.float32)
    ys = ys.astype(np.float32)
    ys = np.broadcast_to(ys, (PH, PW)).astype(np.float32)
    warped = cv2.remap(img.astype(np.float32), xs, ys, cv2.INTER_CUBIC, borderMode=cv2.BORDER_CONSTANT, borderValue=0)
    inside = (xs >= 0) & (xs <= W - 1) & (ys >= 0) & (ys <= H - 1) & valid
    wx = 1 - np.abs(xs - W / 2) / (W / 2)
    wy = 1 - np.abs(ys - H / 2) / (H / 2)
    w = np.where(inside, np.clip(wx, 0, 1) ** 2 * np.clip(wy, 0, 1) ** 0.5, 0)
    acc += warped * w[..., None]
    wsum += w

pano = (acc / np.maximum(wsum, 1e-6)[..., None])
pano[wsum < 1e-6] = 0
cv2.imwrite(out, np.clip(pano, 0, 255).astype(np.uint8), [cv2.IMWRITE_PNG_COMPRESSION, 3])
print(out, PW, 'x', PH, 'from', len(frames), 'frames')
