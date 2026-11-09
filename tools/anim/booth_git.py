"""Review booth: read-only git helpers. Nothing here switches a branch, stashes, resets or writes to the worktree."""
from __future__ import annotations

import os
import subprocess

from booth_config import BoothError


def _git(repo, *args, check=True):
    r = subprocess.run(["git", "-C", repo, *args], capture_output=True)
    if check and r.returncode != 0:
        raise BoothError("git %s failed: %s" % (" ".join(args), r.stderr.decode("utf-8", "replace").strip()))
    return r


def _text(r):
    return r.stdout.decode("utf-8", "replace")


def resolve(repo, ref):
    r = _git(repo, "rev-parse", "--verify", "--quiet", ref + "^{commit}", check=False)
    sha = _text(r).strip()
    if r.returncode != 0 or len(sha) != 40:
        raise BoothError("unknown branch or commit %r" % ref)
    return sha


def extract(repo, sha, paths, dest):
    """Write each path as it is at sha under dest/<path>. Value is the written file, or None if absent at sha."""
    out = {}
    for p in paths:
        r = _git(repo, "cat-file", "blob", "%s:%s" % (sha, p), check=False)
        if r.returncode != 0:
            out[p] = None
            continue
        target = os.path.join(dest, *p.split("/"))
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(target, "wb") as f:
            f.write(r.stdout)
        out[p] = target
    return out


def _under(path, shown):
    return any(path == s or path.startswith(s.rstrip("/") + "/") for s in shown)


def other_files(repo, target_sha, lane_sha, shown):
    """Paths the lane changed since its merge base with the target, minus those equal to or under a shown entry."""
    base = _text(_git(repo, "merge-base", target_sha, lane_sha)).strip()
    r = _git(repo, "-c", "core.quotepath=false", "diff", "--name-only", "-z", base, lane_sha)
    names = [n for n in _text(r).split("\0") if n]
    return sorted(n for n in names if not _under(n, shown))


def changed_between(repo, a, b, paths):
    """Those files equal to or under paths that differ between commits a and b, sorted."""
    if not paths:
        return []
    r = _git(repo, "-c", "core.quotepath=false", "diff", "--name-only", "-z", a, b, "--", *paths)
    return sorted(n for n in _text(r).split("\0") if n)


def dirty_inputs(repo, paths):
    """Those of paths that are untracked, missing, modified or staged, in the order given."""
    if not paths:
        return []
    tracked = set(n for n in _text(_git(repo, "-c", "core.quotepath=false", "ls-files", "-z", "--", *paths)).split("\0") if n)
    r = _git(repo, "-c", "core.quotepath=false", "status", "--porcelain", "-z", "--untracked-files=all", "--", *paths)
    changed = set(e[3:] for e in _text(r).split("\0") if len(e) > 3)
    return [p for p in paths if p not in tracked or p in changed]
