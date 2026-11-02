"""Rebuilds the mod's original sound cues (everything in AudioCues that is not the six older files made by tools/create_battle_audio.py).

Pure numpy synthesis, nothing downloaded or borrowed, so the files carry no third-party licence. Mono 44.1 kHz Ogg Vorbis through ffmpeg.
Usage: python tools/audio/make_cues.py [cue_id ...]      (no arguments rebuilds all; needs numpy and ffmpeg on PATH)
Tone: medieval and Souls-like: low war horns, deep drums, iron and bell strikes, muted wood thunks, little treble.
Each cue is normalised to its own peak (CUES); the in-game level is the volume column of AudioCues, so change it there.
"""
import pathlib, subprocess, sys
import numpy as np

SR = 44100
OUT = pathlib.Path(__file__).resolve().parents[2] / 'src/main/resources/assets/gravesandglory/sounds'


def rng(name): return np.random.default_rng(sum(map(ord, name)) * 7919)
def tt(d): return np.arange(int(d * SR)) / SR


def filt(x, lo=None, hi=None, order=2):
    """Smooth high-pass at lo and/or low-pass at hi (Butterworth-like magnitude, applied in the frequency domain)."""
    X = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1 / SR) + 1e-9
    if hi: X = X / np.sqrt(1 + (f / hi) ** (2 * order))
    if lo: X = X * (1 / np.sqrt(1 + (lo / f) ** (2 * order)))
    return np.fft.irfft(X, len(x))


def add(buf, x, at=0.0, gain=1.0):
    i = int(at * SR); n = min(len(x), len(buf) - i)
    if n > 0: buf[i:i + n] += x[:n] * gain
    return buf


def drum(f0, d, r, sweep=1.2, body=1.0):
    t = tt(d); f = f0 * (1 + sweep * np.exp(-t / .035))
    ph = 2 * np.pi * np.cumsum(f) / SR
    tone = np.sin(ph) * np.exp(-t / (d / 4.5)) * body
    skin = filt(r.standard_normal(len(t)), hi=420) * np.exp(-t / .03) * 1.4
    return tone + skin


def horn(f, d, r, attack=.12, release=.25, glide=0.0, vib=4.5, depth=.004, dark=1900, partials=14):
    t = tt(d); fr = f * (1 + glide * (t / d)) * (1 + depth * np.sin(2 * np.pi * vib * t) * np.minimum(1, t / .4))
    ph = 2 * np.pi * np.cumsum(fr) / SR
    x = sum(np.sin(ph * k + k) / k ** 1.15 for k in range(1, partials + 1))
    x = x + filt(r.standard_normal(len(t)), lo=300, hi=1400) * .12   # breath
    env = np.minimum(1, t / attack) ** 1.3 * np.minimum(1, (d - t) / release) ** 1.5
    return filt(x, hi=dark) * env


def bell(f, d, r):
    t = tt(d); x = np.zeros_like(t)
    for ratio, a, dec in [(.5, .5, 1.0), (1, 1, 1), (1.19, .7, .75), (1.5, .35, .55), (2.0, .55, .5), (2.74, .35, .3), (4.17, .2, .18)]:
        x += a * np.sin(2 * np.pi * f * ratio * t + r.uniform(0, 6.28)) * np.exp(-t / (d * dec / 3.2))
    return x * np.minimum(1, t / .003) * .55 + filt(r.standard_normal(len(t)), lo=1500, hi=5000) * np.exp(-t / .006) * .25


def clank(f, d, r, bright=1.0):
    t = tt(d); x = np.zeros_like(t)
    for ratio, a in [(1, 1), (2.32, .8), (4.25, .6), (6.63, .35), (9.38, .2)]:
        x += a * np.sin(2 * np.pi * f * ratio * t + r.uniform(0, 6.28)) * np.exp(-t / (d * .22 / (1 + .25 * ratio)))
    click = filt(r.standard_normal(len(t)), lo=1800, hi=7000 * bright) * np.exp(-t / .004)
    return x * .5 + click * .7


def thunk(f, d, r, wood=1.0):
    t = tt(d); fr = f * (.55 + .45 * np.exp(-t / .02)); ph = 2 * np.pi * np.cumsum(fr) / SR
    x = np.sin(ph) * np.exp(-t / (d / 5)) + .35 * np.sin(ph * 2.3) * np.exp(-t / (d / 9))
    return x + filt(r.standard_normal(len(t)), lo=200, hi=900) * np.exp(-t / .012) * wood


def rumble(d, r, hi=140, decay=None):
    t = tt(d); return filt(r.standard_normal(len(t)), hi=hi, order=3) * np.exp(-t / (decay or d / 3))


def creak(d, r, f=190, wobble=14):
    t = tt(d); fr = f * (1 + .35 * np.sin(2 * np.pi * 2.3 * t) + .15 * np.sin(2 * np.pi * 5.1 * t))
    ph = 2 * np.pi * np.cumsum(fr) / SR
    saw = sum(np.sin(ph * k) / k for k in range(1, 9))
    am = .5 + .5 * np.sin(2 * np.pi * wobble * t + 1)
    return filt(saw, lo=120, hi=1100) * am * np.minimum(1, t / .05) * np.minimum(1, (d - t) / .12)


def reverb(x, tail, mix, r):
    n = int(tail * SR); ir = filt(r.standard_normal(n), hi=3200) * np.exp(-np.arange(n) / (tail * SR / 5.5))
    ir[0] = 0; L = len(x) + n; wet = np.fft.irfft(np.fft.rfft(x, L) * np.fft.rfft(ir, L), L)
    wet *= np.max(np.abs(x)) / (np.max(np.abs(wet)) + 1e-9)
    out = np.zeros(L); out[:len(x)] += x * (1 - mix * .4); out += wet * mix
    return out


# ---- the cues ------------------------------------------------------------------------------------------------------------------------------

def ui_confirm(r):    # a latch of iron settling: soft low bell tick, short
    b = np.zeros(int(.34 * SR)); add(b, bell(520, .34, r), 0, .6); add(b, clank(1500, .05, r, .5), 0, .35); return b

def ui_back(r):       # a lower, duller tick, going the other way
    b = np.zeros(int(.2 * SR)); add(b, thunk(210, .2, r, .8), 0, .8); add(b, clank(700, .06, r, .4), 0, .25); return b

def ui_deny(r):       # a muted wooden thunk, twice, the second shorter
    b = np.zeros(int(.34 * SR)); add(b, thunk(130, .22, r, 1.1), 0, 1); add(b, thunk(105, .16, r, 1.0), .11, .7); return b

def countdown_tick(r):  # one low drum stroke with a wood knock on top
    b = np.zeros(int(.4 * SR)); add(b, drum(78, .4, r), 0, 1); add(b, thunk(260, .06, r), 0, .35); return b

def defeat(r):        # a toll, a falling dirge horn and a sinking drum
    b = np.zeros(int(3.0 * SR)); add(b, bell(147, 3.0, r), 0, .55)
    add(b, horn(110, 2.2, r, attack=.25, release=.9, glide=-.16, dark=1100), .15, .5)
    add(b, horn(116.5, 2.2, r, attack=.3, release=.9, glide=-.16, dark=1000), .2, .3)
    add(b, drum(52, 1.2, r, 1.8), .05, 1); return reverb(b, 1.2, .35, r)

def capture_start(r):  # chain mail dragged and an iron clank: someone is at the point
    b = np.zeros(int(.9 * SR))
    for i in range(9): add(b, clank(r.uniform(2200, 3600), .09, r, 1.0), i * .045 + r.uniform(0, .02), r.uniform(.15, .3))
    add(b, clank(240, .5, r), .4, .9); return reverb(b, .5, .2, r)

def capture_won(r):    # two rising horn notes over a bell: the banner goes up
    b = np.zeros(int(2.0 * SR))
    add(b, horn(196, .6, r, .08, .2), 0, .6); add(b, horn(293.66, 1.3, r, .08, .6), .5, .75); add(b, horn(392, 1.2, r, .1, .6), .55, .3)
    add(b, bell(587, 1.4, r), .6, .35); add(b, drum(70, .5, r), 0, .8); return reverb(b, 1.0, .3, r)

def capture_lost(r):   # a low falling note and a heavy stroke: the banner comes down
    b = np.zeros(int(1.9 * SR))
    add(b, horn(165, 1.4, r, .1, .7, glide=-.2, dark=1200), 0, .7); add(b, horn(123, 1.4, r, .1, .7, glide=-.2, dark=900), .05, .4)
    add(b, drum(55, 1.0, r, 1.6), .3, 1); add(b, bell(110, 1.5, r), .3, .25); return reverb(b, .9, .3, r)

def gate_hit(r):       # a ram on iron-banded oak
    b = np.zeros(int(1.0 * SR)); add(b, thunk(70, .6, r, 1.6), 0, 1); add(b, clank(190, .7, r), 0, .55)
    add(b, rumble(.9, r, 160), 0, .5); return reverb(b, .6, .25, r)

def gate_shut(r):      # a portcullis falling: chain run, then an iron slam with a long tail
    b = np.zeros(int(1.6 * SR))
    for i in range(12): add(b, clank(r.uniform(1800, 3000), .06, r), i * .03, .13)
    add(b, clank(130, .9, r), .38, 1); add(b, thunk(60, .5, r), .38, 1); add(b, rumble(.9, r, 120), .38, .6); return reverb(b, 1.0, .4, r)

def gate_breach(r):    # oak splitting, a deep boom, stones falling
    b = np.zeros(int(2.4 * SR)); add(b, filt(r.standard_normal(int(.3 * SR)), lo=500, hi=3500) * np.exp(-tt(.3) / .08), 0, .9)
    add(b, thunk(48, 1.2, r, 1.5), .02, 1.1); add(b, rumble(2.2, r, 150, 1.0), .05, 1.0)
    for i in range(10): add(b, thunk(r.uniform(90, 220), .12, r), .5 + i * r.uniform(.05, .14), r.uniform(.15, .4))
    return reverb(b, 1.4, .3, r)

def convoy_roll(r):    # wheels groan into motion, cart-ruts thudding
    b = np.zeros(int(1.7 * SR)); add(b, creak(1.7, r, 175, 11), 0, .7)
    for i in range(7): add(b, thunk(80, .1, r, .6), .1 + i * .22, .5)
    add(b, rumble(1.7, r, 200, 3), 0, .25); return b

def convoy_stop(r):    # the creak dies, a wooden thunk as the wheels settle
    b = np.zeros(int(.8 * SR)); add(b, creak(.45, r, 150, 8), 0, .5); add(b, thunk(100, .35, r, 1.2), .4, 1); return b

def convoy_rollback(r):  # the cart slips backward: a falling creak and ruts clattering
    b = np.zeros(int(1.4 * SR)); add(b, creak(1.3, r, 135, 9)[::-1].copy(), 0, .55)
    for i in range(6): add(b, thunk(95, .1, r, .6), .1 + i * .17, .45)
    add(b, thunk(70, .3, r), 1.0, .7); return b

def convoy_checkpoint(r):  # two bell strokes on a low hum
    b = np.zeros(int(2.4 * SR)); add(b, bell(392, 2.0, r), 0, .8); add(b, bell(523, 2.0, r), .5, .65)
    add(b, horn(196, 1.2, r, .2, .6, dark=900), .1, .25); return reverb(b, 1.2, .3, r)

def wave_warning(r):   # one long, cold horn, a flat second voice below it
    b = np.zeros(int(2.6 * SR))
    add(b, horn(110, 2.4, r, .5, .8, glide=.03, dark=1500), 0, .8); add(b, horn(116.5, 2.4, r, .6, .8, glide=.03, dark=1300), .05, .5)
    add(b, horn(220, 2.2, r, .6, .8, dark=1700), .1, .25); add(b, drum(48, 1.0, r), 1.3, .7); return reverb(b, 1.1, .3, r)

def wave_rise(r):      # drums swell, then the whole horn line breaks
    b = np.zeros(int(3.0 * SR))
    for i, tm in enumerate([0, .35, .65, .9, 1.1, 1.25, 1.4]): add(b, drum(64 + i * 3, .45, r), tm, .55 + .07 * i)
    add(b, horn(146.83, 1.3, r, .06, .5), 1.45, .7); add(b, horn(220, 1.3, r, .06, .5), 1.45, .35)
    add(b, horn(110, 1.6, r, .08, .8), 1.45, .55); add(b, drum(45, 1.3, r, 1.6), 1.45, 1); return reverb(b, 1.2, .3, r)


CUES = {  # id: (builder, peak)
    'ui_confirm': (ui_confirm, .55), 'ui_back': (ui_back, .5), 'ui_deny': (ui_deny, .6), 'countdown_tick': (countdown_tick, .7),
    'defeat': (defeat, .8), 'capture_start': (capture_start, .75), 'capture_won': (capture_won, .8), 'capture_lost': (capture_lost, .8),
    'gate_hit': (gate_hit, .85), 'gate_shut': (gate_shut, .85), 'gate_breach': (gate_breach, .9), 'convoy_roll': (convoy_roll, .6),
    'convoy_stop': (convoy_stop, .7), 'convoy_rollback': (convoy_rollback, .6), 'convoy_checkpoint': (convoy_checkpoint, .75),
    'wave_warning': (wave_warning, .85), 'wave_rise': (wave_rise, .9),
}


def finish(x, peak):
    x = filt(x, lo=28)                       # no inaudible DC / sub rumble
    x = x / (np.max(np.abs(x)) + 1e-9) * peak
    n = int(.012 * SR); x[-n:] *= np.linspace(1, 0, n); x[:64] *= np.linspace(0, 1, 64)
    return x


def export(name, x):
    pcm = (np.clip(x, -1, 1) * 32767).astype('<i2').tobytes()
    OUT.mkdir(parents=True, exist_ok=True)
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 's16le', '-ar', str(SR), '-ac', '1', '-i', '-', '-map_metadata', '-1', '-fflags', '+bitexact',
                    '-c:a', 'libvorbis', '-q:a', '4', str(OUT / (name + '.ogg'))], input=pcm, check=True)


if __name__ == '__main__':
    names = sys.argv[1:] or list(CUES)
    for n in names:
        build, peak = CUES[n]; x = finish(build(rng(n)), peak); export(n, x)
        print(f'{n}.ogg  {len(x) / SR:.2f}s  peak {peak}')
