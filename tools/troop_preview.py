"""Engine-true animation previews of a troop: the game's MotionEngine (test-tools MotionPreview) drives the pose, troop_render
draws it on the troop's model.

    python tools/troop_preview.py <troop> <scene.json> --out <dir> [--tier N] [--sheet] [--reel]

A scene is MotionPreview's JSON (see that class). The engine classes compile once into work/troop_anim/classes (javac and the
Gson jar from the Gradle cache, as tools/verify.py finds it)."""
import argparse
import json
import os
import pathlib
import subprocess
import sys
import tempfile

from PIL import Image, ImageDraw

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import troop_render as R  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parents[1]
WORK = ROOT / 'work/troop_anim'
ENGINE = ['Spec', 'MotionMath', 'Clip', 'MotionInput', 'Pose', 'MotionBody', 'MotionFeet', 'MotionStack', 'MotionEngine']
BG = (38, 42, 50, 255)


def gson():
    jars = sorted((pathlib.Path.home() / '.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/2.10.1').glob('*/*.jar'))
    if not jars:
        raise SystemExit('Gson 2.10.1 missing from the Gradle cache (see docs/SETUP.md)')
    return jars[0]


def classes():
    """Compiles the engine and MotionPreview when any source is newer than the classes."""
    out = WORK / 'classes'
    srcs = [ROOT / 'src/main/java/gg/gravesandglory' / (n + '.java') for n in ENGINE] + [ROOT / 'test-tools/java/gg/gravesandglory/MotionPreview.java']
    stamp = out / 'gg/gravesandglory/MotionPreview.class'
    if not stamp.is_file() or max(s.stat().st_mtime for s in srcs) > stamp.stat().st_mtime:
        out.mkdir(parents=True, exist_ok=True)
        subprocess.run(['javac', '--release', '17', '-nowarn', '-cp', str(gson()), '-d', str(out), *map(str, srcs)], check=True,
                       stderr=subprocess.DEVNULL if os.environ.get('QUIET') else None)
    return out


def run(scene):
    """MotionPreview's output for a scene dict."""
    cp = os.pathsep.join([str(classes()), str(gson())])
    with tempfile.TemporaryDirectory() as tmp:
        sp, op = pathlib.Path(tmp) / 'scene.json', pathlib.Path(tmp) / 'out.json'
        sp.write_text(json.dumps(scene))
        subprocess.run(['java', '-cp', cp, 'gg.gravesandglory.MotionPreview', str(sp), str(op)], check=True, cwd=ROOT,
                       stderr=subprocess.PIPE)
        return json.loads(op.read_text())


def draw(troop, pose, yaw, pitch=10, scale=7, size=(240, 300), ground=None, bow=None):
    items, _ = troop.quads(pose, bow)
    img = Image.new('RGBA', size, BG)
    # keep the hips in the middle of the tile: a lunge or a step carries the root several px
    if ground is not None:  # floor studs that move with the ground, so travel reads
        d = ImageDraw.Draw(img)
        gx, gz = ground
        import math
        cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
        for i in range(-6, 7):
            for j in range(-6, 7):
                x, z = i * 8 - (gx * 16) % 8, j * 8 - (-gz * 16) % 8
                sx = size[0] / 2 + (x * cy - z * sy) * scale
                sy_ = size[1] * 0.92 + (x * sy + z * cy) * scale * math.sin(math.radians(pitch))
                d.rectangle([sx - 1, sy_ - 1, sx + 1, sy_ + 1], fill=(70, 76, 88, 255))
    import math as _m
    rt = pose.get('root', [0] * 6)
    cy_, sy_ = _m.cos(_m.radians(yaw)), _m.sin(_m.radians(yaw))
    shift = (rt[3] * cy_ - rt[5] * sy_) * scale * 0.5
    img.alpha_composite(R.render(items, yaw, pitch, scale, size, anchor=(size[0] / 2 - shift, size[1] * 0.92)))
    return img


def sheet(troop, frames, times, views=(0, 40, 90), label=None, scale=5, size=(250, 230)):
    """A contact sheet: one column a time, one row a view (front, three-quarter from the right, side from the right), the clip
    named in a header when label is given."""
    W, H = size
    top = 24 if label else 0
    out = Image.new('RGBA', (W * len(times), H * len(views) + top + 16), BG)
    d = ImageDraw.Draw(out)
    if label:
        d.text((6, 6), label, fill=(240, 220, 150, 255))
    by_t = {round(f['t'], 4): f for f in frames}
    ts = sorted(by_t)
    for c, t in enumerate(times):
        f = by_t[min(ts, key=lambda x: abs(x - t))]
        for r, yaw in enumerate(views):
            out.alpha_composite(draw(troop, f['pose'], yaw, scale=scale, size=size, bow=f.get('draw')), (c * W, top + r * H))
        d.text((c * W + 4, top + H * len(views) + 2), f'{t:.2f}s', fill=(220, 220, 220, 255))
    return out


def reel(troop, frames, path, views=(20, 90), scale=6, size=(320, 360), title='', fps=None):
    """An mp4 of the frames from the given views side by side, with the ground studs."""
    fps = fps or 30
    tmp = pathlib.Path(tempfile.mkdtemp(dir=WORK))
    for i, f in enumerate(frames):
        img = Image.new('RGBA', (size[0] * len(views), size[1] + 24), BG)
        for k, yaw in enumerate(views):
            img.alpha_composite(draw(troop, f['pose'], yaw, scale=scale, size=size, ground=(f.get('gx', 0), f.get('gz', 0)), bow=f.get('draw')), (k * size[0], 0))
        txt = title(f) if callable(title) else f"{title}  {f['t']:.2f}s"
        ImageDraw.Draw(img).text((6, size[1] + 5), txt, fill=(225, 225, 225, 255))
        img.convert('RGB').save(tmp / f'{i:05d}.png')
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-framerate', str(fps), '-i', str(tmp / '%05d.png'), '-pix_fmt', 'yuv420p',
                    '-vf', 'pad=ceil(iw/2)*2:ceil(ih/2)*2', str(path)], check=True)
    for p in tmp.iterdir():
        p.unlink()
    tmp.rmdir()


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('troop')
    ap.add_argument('scene')
    ap.add_argument('--out', required=True)
    ap.add_argument('--tier', type=int, default=1)
    ap.add_argument('--times', default='')
    a = ap.parse_args(argv)
    scene = json.loads(pathlib.Path(a.scene).read_text())
    scene['troop'] = a.troop
    res = run(scene)
    troop = R.Troop(a.troop, a.tier)
    out = pathlib.Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    times = [float(x) for x in a.times.split(',')] if a.times else [i * scene['length'] / 7 for i in range(8)]
    sheet(troop, res['frames'], times).save(out / f'{a.troop}_sheet.png')
    reel(troop, res['frames'], out / f'{a.troop}_reel.mp4', title=a.troop, fps=scene.get('fps', 30))
    print(out)


if __name__ == '__main__':
    main()
