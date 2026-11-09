"""Review booth command line. See docs/superpowers/specs/2026-11-09-review-booth-design.md.

  python tools/anim/booth.py build --config art/anim/<family>/booth.<troop>.json --target <branch> [--batch <id>]
  python tools/anim/booth.py decide --batch-dir work/booth/<id>

build renders a batch for Kort's review page into work/booth/<batch-id>/: one sheet per clip, manifest.json, the
database documents to write (db/) and the list of sheets to upload (upload.txt). It never changes the worktree
outside work/booth, never switches a branch and never talks to the network.
"""
from __future__ import annotations

import argparse
import datetime
import json
import os
import shutil
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
if HERE not in sys.path:
    sys.path.insert(0, HERE)
import booth_git as bg  # noqa: E402
import booth_pack as bp  # noqa: E402
import booth_render as br  # noqa: E402
import booth_verdict as bv  # noqa: E402
import scorecard as sc  # noqa: E402
from booth_config import BoothError, load_config  # noqa: E402

BYTES_PER_CLIP = 40 * 1024 * 1024 // 20      # 20 clips stay under 40 MB


def _read_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def _write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=1)
        f.write("\n")


class _Sources:
    """Builds render sources for the worktree and for files as they were at a commit."""

    def __init__(self, repo, cfg, tmp):
        self.repo, self.cfg, self.tmp = repo, cfg, tmp
        self._cache = {}

    def _abs(self, rel):
        return os.path.join(self.repo, *rel.split("/"))

    def new(self, clip):
        s = self.cfg.source
        anim = self._abs(s.anim)
        return br.Source(self._abs(s.geo), [self._abs(t) for t in s.textures],
                         [(anim, b) for b in clip.base] + [(anim, clip.name)], clip.speed)

    def _at(self, sha, spec):
        key = (sha, spec.geo, tuple(spec.textures), spec.anim)
        if key not in self._cache:
            got = bg.extract(self.repo, sha, [spec.geo, *spec.textures, spec.anim],
                             os.path.join(self.tmp, "%s-%d" % (sha[:12], len(self._cache))))
            names = set()
            if got[spec.anim] is not None:
                try:
                    names = set(_read_json(got[spec.anim]).get("animations", {}))
                except (ValueError, AttributeError):
                    names = set()
            self._cache[key] = (got, names)
        return self._cache[key]

    def _old(self, sha, spec, name, base, speed):
        got, names = self._at(sha, spec)
        if got[spec.geo] is None or got[spec.anim] is None or name not in names:
            return None
        anim = got[spec.anim]
        textures = [got[t] for t in spec.textures if got[t] is not None]
        return br.Source(got[spec.geo], textures, [(anim, b) for b in base if b in names] + [(anim, name)], speed)

    def previous(self, clip, sha):
        return self._old(sha, self.cfg.source, clip.name, clip.base, clip.speed)

    def reference(self, clip, sha):
        if clip.reference is None:
            return None
        src = self._old(sha, self.cfg.reference, clip.reference, clip.reference_base, clip.speed)
        if src is None:
            raise BoothError("clip %r: reference clip %r is not on the target branch at %s"
                             % (clip.name, clip.reference, sha[:7]))
        return src


def scorecard_gate(repo, cfg, names):
    """Score the batch's clips (tools/anim/scorecard.py); a batch with a failing clip is not built."""
    try:
        cards = sc.score_booth(repo, cfg, names)
    except (sc.ScoreError, sc.rg.GeoError, OSError, ValueError) as e:
        raise BoothError("cannot score the clips: %s" % e)
    bad = sc.failures([cards[n] for n in names])
    if bad:
        raise BoothError("%d metric(s) fail the animation scorecard (limits from the approved knight, "
                         "tools/anim/scorecard_limits.json); fix the clips before building:\n  %s"
                         % (len(bad), "\n  ".join(bad)))
    return {n: {"pass": c["pass"], "kind": c["kind"], "metrics": c["metrics"]} for n, c in cards.items()}


def build(repo, config_path, target, out_root="work/booth", batch=None, clips=None, ss=2, budget_bytes=None):
    repo = os.path.abspath(repo)
    cfg_abs = config_path if os.path.isabs(config_path) else os.path.join(repo, config_path)
    cfg_rel = os.path.relpath(cfg_abs, repo).replace("\\", "/")
    out_abs = out_root if os.path.isabs(out_root) else os.path.join(repo, out_root)

    dirty = bg.dirty_inputs(repo, [cfg_rel])
    if dirty:
        raise BoothError("commit the booth config first: %s" % cfg_rel)
    cfg = load_config(cfg_abs)
    dirty = bg.dirty_inputs(repo, cfg.input_paths())
    if dirty:
        raise BoothError("these inputs are not committed, so the batch would not match the lane commit: %s"
                         % ", ".join(dirty))

    if target == "master":
        raise BoothError("the booth never targets master; use main (decide then says to land the lane with tools/land.py)")
    lane_sha = bg.resolve(repo, "HEAD")
    lane = bg._text(bg._git(repo, "rev-parse", "--abbrev-ref", "HEAD")).strip()
    if lane == "HEAD":
        raise BoothError("check out the lane branch first: the worktree is on a detached commit")
    target_sha = bg.resolve(repo, target)
    by_name = {c.name: c for c in cfg.clips}

    if batch is None:
        rnd, old, partial = 1, {}, clips is not None
        batch = "%s-%s-%s" % (cfg.troop, datetime.date.today().strftime("%Y%m%d"), lane_sha[:7])
        batch_dir = os.path.join(out_abs, batch)
        if os.path.exists(batch_dir):
            raise BoothError("batch %s already exists; pass --batch %s to add a round, or delete %s"
                             % (batch, batch, batch_dir))
        previous_sha = target_sha
        created_at = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
        if clips is not None:
            unknown = [n for n in clips if n not in by_name]
            if unknown:
                raise BoothError("--clips names clips the config does not have: %s" % ", ".join(unknown))
            names = [c.name for c in cfg.clips if c.name in set(clips)]
        else:
            names = [c.name for c in cfg.clips]
    else:
        batch_dir = os.path.join(out_abs, batch)
        mpath = os.path.join(batch_dir, "manifest.json")
        if not os.path.isfile(mpath):
            raise BoothError("no batch %r under %s" % (batch, out_abs))
        if clips is not None:
            raise BoothError("--clips only applies to a batch's first round; later rounds re-check every clip")
        prev = _read_json(mpath)
        if prev["target"] != target:
            raise BoothError("batch %s targets %s, not %s" % (batch, prev["target"], target))
        if prev["troop"] != cfg.troop:
            raise BoothError("batch %s is for troop %s, not %s" % (batch, prev["troop"], cfg.troop))
        rnd = prev["round"] + 1
        old = {c["name"]: c for c in prev["clips"]}
        gone = [n for n in old if n not in by_name]
        if gone:
            raise BoothError("the config no longer lists clips of batch %s: %s" % (batch, ", ".join(gone)))
        previous_sha = prev["submitted_sha"]
        created_at = prev["created_at"]
        partial = bool(prev.get("partial"))
        # A batch limited with --clips keeps its clips. A full batch also takes clips added to the config since.
        names = [c.name for c in cfg.clips if not partial or c.name in old]

    cards = scorecard_gate(repo, cfg, names)
    shown = ["art/anim/%s" % cfg.family, *cfg.input_paths(), cfg_rel]
    others = bg.other_files(repo, target_sha, lane_sha, shown)

    entries = []
    with tempfile.TemporaryDirectory(prefix="booth-") as tmp:
        srcs = _Sources(repo, cfg, os.path.join(tmp, "git"))
        stage = os.path.join(tmp, "round")
        for name in names:
            clip = by_name[name]
            was = old.get(name)
            render = None
            if was is not None:
                same = {"new": srcs.new(clip), "previous": srcs.previous(clip, was["previous_sha"]),
                        "reference": srcs.reference(clip, was["reference_sha"]) if was["reference_sha"] else None}
                render = br.render_clip(same, cfg.foot_bones, ss=ss)
                if render.frame_hash == was["frame_hash"]:
                    entries.append(dict(was, changed=False))
                    continue
                if (was["previous_sha"], was["reference_sha"] or target_sha) != (previous_sha, target_sha):
                    render = None
            ref = srcs.reference(clip, target_sha)
            # A clip that was in no earlier round was never rejected: its Previous is the target branch.
            before = previous_sha if was is not None else target_sha
            if render is None:
                render = br.render_clip({"new": srcs.new(clip), "previous": srcs.previous(clip, before),
                                         "reference": ref}, cfg.foot_bones, ss=ss)
            sheet = "round-%d/%s.webp" % (rnd, name)
            sections = bp.pack_sheet(render, os.path.join(stage, name + ".webp"))
            entries.append({"name": name, "ticks": render.ticks, "impact_tick": clip.impact_tick,
                            "frame_hash": render.frame_hash, "sheet": sheet, "changed": True, "round": rnd,
                            "sections": sections, "numbers": render.numbers, "flags": render.flags,
                            "scorecard": cards[name],
                            "previous_sha": before, "reference_sha": target_sha if ref is not None else None})

        def sheet_file(e):
            if e["changed"]:
                return os.path.join(stage, e["name"] + ".webp")
            return os.path.join(batch_dir, *e["sheet"].split("/"))
        total = bp.total_bytes([sheet_file(e) for e in entries])
        budget = BYTES_PER_CLIP * len(entries) if budget_bytes is None else budget_bytes
        if total > budget:
            raise BoothError("sheets total %.1f MB, above the size budget of %.1f MB for %d clip(s)"
                             % (total / 1048576.0, budget / 1048576.0, len(entries)))
        round_dir = os.path.join(batch_dir, "round-%d" % rnd)
        os.makedirs(round_dir, exist_ok=True)
        for e in entries:
            if e["changed"]:
                shutil.copyfile(os.path.join(stage, e["name"] + ".webp"), os.path.join(round_dir, e["name"] + ".webp"))

    manifest = {"batch": batch, "round": rnd, "troop": cfg.troop, "lane": lane, "submitted_sha": lane_sha,
                "target": target, "target_sha": target_sha, "previous_sha": previous_sha, "created_at": created_at,
                "partial": partial, "other_files": others, "inputs": shown, "clips": entries}
    db = os.path.join(batch_dir, "db")
    shutil.rmtree(db, ignore_errors=True)
    doc = {k: v for k, v in manifest.items() if k != "clips"}
    doc["clip_order"] = [e["name"] for e in entries]
    _write_json(os.path.join(db, "batch.json"), doc)
    uploads = []
    for e in entries:
        if e["changed"]:
            _write_json(os.path.join(db, "clips", e["name"] + ".json"), dict(e, sheet_asset=None))
            uploads.append(os.path.join(batch_dir, *e["sheet"].split("/")))
    os.makedirs(os.path.join(db, "clips"), exist_ok=True)
    with open(os.path.join(batch_dir, "upload.txt"), "w", encoding="utf-8", newline="\n") as f:
        f.write("".join(p + "\n" for p in uploads))
    _write_json(os.path.join(batch_dir, "manifest.json"), manifest)
    return manifest


def build_parser():
    p = argparse.ArgumentParser(description="Review booth: build a batch for Kort's review page and act on his verdict.")
    sub = p.add_subparsers(dest="cmd", required=True)
    b = sub.add_parser("build", help="render a batch into work/booth/<batch-id>/")
    b.add_argument("--repo", default=".", help="lane worktree (default: current directory)")
    b.add_argument("--config", required=True, help="art/anim/<family>/booth.<troop>.json")
    b.add_argument("--target", required=True, help="branch the lane will merge into; main means the lane lands with tools/land.py")
    b.add_argument("--batch", default=None, help="existing batch id: add a round after fixes")
    b.add_argument("--clips", default=None, help="comma list: limit a first round to these clips")
    b.add_argument("--ss", type=int, default=2, help="supersampling factor (default 2)")
    d = sub.add_parser("decide", help="read verdict.json in a batch folder and print what to do next")
    d.add_argument("--repo", default=".", help="lane worktree (default: current directory)")
    d.add_argument("--batch-dir", required=True, help="work/booth/<batch-id>")
    return p


def decide_dir(repo, batch_dir):
    """The decision for a batch folder: its manifest.json, its verdict.json if present, and the lane branch's tip.
    The tip is read from the lane branch itself, so the answer is the same from any checkout of the repository."""
    mpath = os.path.join(batch_dir, "manifest.json")
    if not os.path.isfile(mpath):
        raise BoothError("no manifest.json in %s" % batch_dir)
    manifest = _read_json(mpath)
    lane = manifest.get("lane") if isinstance(manifest, dict) else None
    if not isinstance(lane, str) or not lane or lane == "HEAD":
        raise BoothError("this batch names no lane branch; build it again from the lane branch")
    try:
        tip = bg.resolve(os.path.abspath(repo), "refs/heads/" + lane)
    except BoothError:
        raise BoothError("the lane branch of this batch is not in this repository")
    vpath = os.path.join(batch_dir, "verdict.json")
    verdict = None
    if os.path.isfile(vpath):
        try:
            verdict = _read_json(vpath)
        except ValueError:
            return {"action": "invalid", "problems": ["verdict.json is not valid JSON"]}
        if verdict is None:
            return {"action": "invalid", "problems": ["verdict is not an object"]}
    result = bv.decide(manifest, verdict, tip)
    if result["action"] == "land":
        # land.py rebases the lane on main: if main changed what Kort was shown since the batch was built, what lands
        # would differ from what he approved, so the batch is built again on the new main first.
        moved = bg.changed_between(os.path.abspath(repo), manifest["target_sha"], bg.resolve(os.path.abspath(repo), "refs/heads/main"),
                                   result["inputs"])
        if moved:
            return {"action": "resubmit", "reason": "main changed reviewed inputs since the batch was built: %s" % ", ".join(moved)}
    return result


def main(argv=None):
    args = build_parser().parse_args(argv)
    try:
        if args.cmd == "build":
            clips = [c.strip() for c in args.clips.split(",") if c.strip()] if args.clips else None
            m = build(args.repo, args.config, args.target, batch=args.batch, clips=clips, ss=args.ss)
            folder = os.path.join(os.path.abspath(args.repo), "work", "booth", m["batch"])
            changed = [c for c in m["clips"] if c["changed"]]
            size = bp.total_bytes([os.path.join(folder, *c["sheet"].split("/")) for c in m["clips"]])
            print("batch %s round %d: %d clip(s), %d to upload, sheets %.1f MB, %d other changed file(s)"
                  % (m["batch"], m["round"], len(m["clips"]), len(changed), size / 1048576.0, len(m["other_files"])))
            print(folder)
            return 0
        if args.cmd == "decide":
            result = decide_dir(args.repo, args.batch_dir)
            print(json.dumps(result, indent=1))
            return 2 if result["action"] == "invalid" else 0
    except BoothError as e:
        print("error: %s" % e, file=sys.stderr)
        return 2
    return 2


if __name__ == "__main__":
    sys.exit(main())
