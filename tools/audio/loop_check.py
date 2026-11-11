#!/usr/bin/env python
"""loop_check.py - standalone checker for loopable music files (ogg or wav).

For every file it reports:
  * duration and sample count (as decoded);
  * whole-bar check, given --bpm and --beats (beats per bar): samples must equal bars * beats * 60 / bpm * sr
    exactly (a 1-sample tolerance can be set with --bar-tol);
  * the loop seam: the step from the last sample to the first, the level jump between the last 10 ms and the first
    10 ms, each compared with how much the file itself changes elsewhere;
  * integrated loudness (LUFS) and true peak (dBTP) via ffmpeg's ebur128 filter.

SEAM METRICS (the file is treated as endlessly repeating: sample N-1 is followed by sample 0)
  peak step ratio   max over channels of |x[0]-x[N-1]| divided by the 99.9th percentile of |x[n]-x[n-1]| over the file.
                    ~1 or less: the junction is as smooth as ordinary audio. Fails above --seam-peak-max (default 2.0).
  rms jump          |20*log10(rms of first 10 ms / rms of last 10 ms)| in dB, compared with the typical change between
                    consecutive 10 ms blocks elsewhere (95th and 99th percentile of the dB change, blocks above -70 dBFS)
                    and, when --bpm is given, with the same last-10 ms / next-10 ms jump at every other bar line (a downbeat
                    accent after a quiet bar end is normal music; a dropout or hole at the seam is not). Fails when the jump
                    exceeds max(99th percentile typical change, largest other bar-line jump, --seam-rms-floor dB (default 3)).
  hf step ratio     the same junction step measured on the signal after a 1st-order high-pass (about 2 kHz); this is
                    what a click is made of and it ignores steep low-frequency slopes. Reported, and fails above
                    --seam-peak-max like the peak ratio. (If the file has no content above 2 kHz this is reported as n/a.)

LOUDNESS RULE   |LUFS - target| <= --lufs-tol (default target -24, tol 1.0); true peak <= --max-tp (default -1.0 dBTP).

TIER MODE   --tiers: the files given are tiers 1..n in order; additionally requires that every tier has the same sample
            count, loudness never falls from one tier to the next, and (max LUFS - min LUFS) <= --tier-spread (default 2.0).

USAGE
    python loop_check.py FILE[@BPM] [FILE[@BPM] ...] [--bpm 100] [--beats 4] [--bars N] [--tiers] [--json]
    FILE@BPM overrides --bpm for that file. --bars N additionally demands exactly N bars.
EXIT CODE  0 all pass, 1 any seam / loudness / whole-bar (when a BPM is given) / tier rule fails, 2 usage or decode error.
Needs numpy, scipy and soundfile (or falls back to ffmpeg for decoding) and ffmpeg on PATH (or FFMPEG env var).
"""
import argparse, json, os, re, shutil, subprocess, sys
import numpy as np

FFMPEG = os.environ.get("FFMPEG") or shutil.which("ffmpeg") or r"C:/ffmpeg/ffmpeg-8.1-full_build/bin/ffmpeg.exe"


def load(path):
    """-> (float64 array [n, ch], sr). soundfile first, ffmpeg as fallback."""
    try:
        import soundfile as sf
        x, sr = sf.read(path, dtype="float64", always_2d=True)
        return x, sr
    except Exception:
        r = subprocess.run([FFMPEG, "-v", "error", "-i", path, "-f", "f32le", "-acodec", "pcm_f32le", "-"], capture_output=True)
        if r.returncode:
            raise RuntimeError(r.stderr.decode(errors="replace")[-400:])
        info = subprocess.run([FFMPEG, "-hide_banner", "-i", path], capture_output=True, text=True).stderr
        sr = int(re.search(r"(\d+) Hz", info).group(1))
        ch = 1 if "mono" in info else 2
        return np.frombuffer(r.stdout, dtype="<f4").astype(np.float64).reshape(-1, ch), sr


def ebur128(path):
    """-> (integrated LUFS, true peak dBTP) from ffmpeg's ebur128 summary."""
    r = subprocess.run([FFMPEG, "-hide_banner", "-nostats", "-i", path, "-af", "ebur128=peak=true", "-f", "null", "-"],
                       capture_output=True, text=True)
    t = r.stderr
    t = t[t.rfind("Summary:"):]
    i = re.search(r"I:\s+(-?[\d.]+|-inf)\s+LUFS", t)
    p = re.search(r"Peak:\s+(-?[\d.]+|-inf)\s+dBFS", t)
    if not i or not p:
        raise RuntimeError("could not parse ebur128 output: " + t[-300:])
    return float(i.group(1)), float(p.group(1))


def seam_metrics(x, sr, spb=None):
    n = len(x)
    w = max(1, int(round(0.010 * sr)))
    d = np.abs(np.diff(x, axis=0))
    junction = float(np.max(np.abs(x[0] - x[-1])))
    p999 = float(np.percentile(d, 99.9))
    local = np.concatenate([d[: w], d[-w:]]) if n > 2 * w else d
    local_max = float(local.max())
    # high-passed version (click detector)
    try:
        from scipy.signal import butter, sosfilt
        sos = butter(1, 2000.0 / (sr / 2), btype="high", output="sos")
        # filter the file tiled 3x and keep the middle copy so the junction sees steady-state filter memory
        xt = np.concatenate([x, x, x], axis=0)
        h = sosfilt(sos, xt, axis=0)[n: 2 * n]
        dh = np.abs(np.diff(h, axis=0))
        hj = float(np.max(np.abs(h[0] - h[-1])))
        hp999 = float(np.percentile(dh, 99.9))
        hf_ratio = hj / hp999 if hp999 > 1e-9 else None
    except Exception:
        hf_ratio = None
    # 10 ms RMS blocks
    mono_sq = np.mean(x ** 2, axis=1)
    nb = n // w
    blocks = np.sqrt(mono_sq[: nb * w].reshape(nb, w).mean(axis=1) + 1e-20)
    db = 20 * np.log10(blocks + 1e-12)
    first = 20 * np.log10(np.sqrt(mono_sq[:w].mean()) + 1e-12)
    last = 20 * np.log10(np.sqrt(mono_sq[-w:].mean()) + 1e-12)
    ok = (db[1:] > -70) & (db[:-1] > -70)
    dd = np.abs(np.diff(db))[ok]
    p95, p99 = (float(np.percentile(dd, 95)), float(np.percentile(dd, 99))) if len(dd) else (0.0, 0.0)
    # level jump (last 10 ms before / first 10 ms after) at every OTHER bar line, when the bar length is known:
    # a downbeat accent after a quiet bar end is normal music, so the seam may jump as much as the bar lines do
    bl = []
    if spb and spb > 4 * w:
        k = 1
        while k * spb + w <= n:
            b = int(round(k * spb))
            if b + w <= n:
                bl.append(abs(10 * np.log10(mono_sq[b: b + w].mean() + 1e-20) - 10 * np.log10(mono_sq[b - w: b].mean() + 1e-20)))
            k += 1
    return {
        "bar_line_jump_max_db": float(max(bl)) if bl else None,
        "junction_step": junction, "p99_9_step": p999, "peak_ratio": junction / p999 if p999 > 0 else float("inf"),
        "local_max_step_20ms": local_max, "hf_ratio": hf_ratio,
        "rms_first10ms_db": float(first), "rms_last10ms_db": float(last), "rms_jump_db": float(abs(first - last)),
        "typical_block_change_p95_db": p95, "typical_block_change_p99_db": p99,
    }


def check_file(path, bpm=None, beats=4, bars=None, args=None):
    a = args
    x, sr = load(path)
    n, ch = x.shape
    res = {"file": path, "sr": sr, "channels": ch, "samples": n, "seconds": n / sr, "fails": []}
    # whole bars
    if bpm:
        spb = beats * 60.0 * sr / bpm
        nb = n / spb
        res.update({"bpm": bpm, "beats_per_bar": beats, "samples_per_bar": spb, "bars_exact": nb})
        whole = abs(nb - round(nb)) * spb <= a.bar_tol
        res["bars"] = int(round(nb)) if whole else None
        res["whole_bars"] = bool(whole)
        if not whole:
            res["fails"].append(f"not whole bars ({nb:.4f} bars; {round(nb) * spb - n:+.1f} samples from {round(nb)} bars)")
        if bars is not None and res["bars"] != bars:
            res["fails"].append(f"expected {bars} bars, found {nb:.3f}")
    # seam
    s = seam_metrics(x, sr, (beats * 60.0 * sr / bpm) if bpm else None)
    res["seam"] = s
    if s["peak_ratio"] > a.seam_peak_max:
        res["fails"].append(f"seam peak step {s['peak_ratio']:.2f}x the p99.9 step (max {a.seam_peak_max})")
    if s["hf_ratio"] is not None and s["hf_ratio"] > a.seam_peak_max:
        res["fails"].append(f"seam high-passed step {s['hf_ratio']:.2f}x p99.9 (max {a.seam_peak_max})")
    allowed = max(s["typical_block_change_p99_db"], a.seam_rms_floor, s["bar_line_jump_max_db"] or 0.0)
    if s["rms_jump_db"] > allowed:
        res["fails"].append(f"seam RMS jump {s['rms_jump_db']:.1f} dB over allowed {allowed:.1f} dB")
    res["seam_pass"] = not any(f.startswith("seam") for f in res["fails"])
    # loudness
    lufs, tp = ebur128(path)
    res["lufs"], res["true_peak_dbtp"] = lufs, tp
    if abs(lufs - a.target_lufs) > a.lufs_tol:
        res["fails"].append(f"loudness {lufs:.1f} LUFS outside {a.target_lufs:+.1f} +/- {a.lufs_tol}")
    if tp > a.max_tp:
        res["fails"].append(f"true peak {tp:.1f} dBTP above {a.max_tp:+.1f}")
    res["pass"] = not res["fails"]
    return res


def main(argv=None):
    ap = argparse.ArgumentParser(description="loop / loudness checker for ogg and wav music loops")
    ap.add_argument("files", nargs="+", help="FILE or FILE@BPM")
    ap.add_argument("--bpm", type=float, help="tempo; enables the whole-bar check")
    ap.add_argument("--beats", type=int, default=4, help="beats per bar (default 4)")
    ap.add_argument("--bars", type=int, help="demand exactly this many bars")
    ap.add_argument("--bar-tol", type=float, default=0.5, help="allowed distance from a whole bar, samples (default 0.5)")
    ap.add_argument("--target-lufs", type=float, default=-24.0)
    ap.add_argument("--lufs-tol", type=float, default=1.0)
    ap.add_argument("--max-tp", type=float, default=-1.0)
    ap.add_argument("--seam-peak-max", type=float, default=2.0)
    ap.add_argument("--seam-rms-floor", type=float, default=3.0)
    ap.add_argument("--tiers", action="store_true", help="files are tiers 1..n: equal length, rising loudness, spread limit")
    ap.add_argument("--tier-spread", type=float, default=2.0)
    ap.add_argument("--json", action="store_true", help="print JSON instead of text")
    a = ap.parse_args(argv)
    results = []
    for f in a.files:
        bpm = a.bpm
        if "@" in f and not os.path.exists(f):
            f, b = f.rsplit("@", 1)
            bpm = float(b)
        try:
            results.append(check_file(f, bpm, a.beats, a.bars, a))
        except Exception as e:
            print(f"ERROR {f}: {e}", file=sys.stderr)
            return 2
    group = []
    if a.tiers and len(results) > 1:
        if len({r["samples"] for r in results}) > 1:
            group.append("tiers differ in sample count: " + ", ".join(str(r["samples"]) for r in results))
        l = [r["lufs"] for r in results]
        if any(l[i + 1] < l[i] for i in range(len(l) - 1)):
            group.append("tier loudness not rising: " + ", ".join(f"{v:.1f}" for v in l))
        if max(l) - min(l) > a.tier_spread:
            group.append(f"tier loudness spread {max(l) - min(l):.1f} LU over {a.tier_spread}")
    if a.json:
        print(json.dumps({"results": results, "group_fails": group}, indent=1))
    else:
        for r in results:
            s = r["seam"]
            print(f"{'PASS' if r['pass'] else 'FAIL'}  {os.path.basename(r['file'])}")
            bar = ""
            if "bpm" in r:
                bar = f"  bars {r['bars'] if r['bars'] is not None else 'NOT WHOLE (%.3f)' % r['bars_exact']} @ {r['bpm']:g} BPM x{r['beats_per_bar']}"
            print(f"      {r['samples']} samples, {r['seconds']:.3f} s, {r['sr']} Hz, {r['channels']} ch{bar}")
            hf = "n/a" if s["hf_ratio"] is None else f"{s['hf_ratio']:.2f}x"
            print(f"      seam: step {s['junction_step']:.5f} = {s['peak_ratio']:.2f}x p99.9 step ({s['p99_9_step']:.5f}); high-passed {hf};"
                  f" RMS first/last 10 ms {s['rms_first10ms_db']:.1f}/{s['rms_last10ms_db']:.1f} dBFS, jump {s['rms_jump_db']:.1f} dB"
                  f"{'' if s['bar_line_jump_max_db'] is None else ' [max other bar line ' + format(s['bar_line_jump_max_db'], '.1f') + ' dB]'}"
                  f" (typical block change p95 {s['typical_block_change_p95_db']:.1f}, p99 {s['typical_block_change_p99_db']:.1f} dB)")
            print(f"      loudness {r['lufs']:.1f} LUFS, true peak {r['true_peak_dbtp']:.1f} dBTP")
            for f in r["fails"]:
                print(f"      FAIL: {f}")
        for g in group:
            print(f"TIER FAIL: {g}")
    return 1 if (any(not r["pass"] for r in results) or group) else 0


if __name__ == "__main__":
    sys.exit(main())
