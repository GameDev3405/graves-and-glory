"""Review booth: check a verdict document read back from the page, and decide what the lane does next.

The verdict is data written by a web page. It has a fixed vocabulary and no free text; anything outside that shape
makes the whole document invalid. Problems are described in this module's own words and never quote stored values,
so text planted in a verdict cannot reach an agent through an error message.
"""
from __future__ import annotations

import re

TAGS = ("too_stiff", "too_slow", "too_fast", "clipping", "wrong_weapon_angle", "no_weight", "feet_slide",
        "hard_to_read")
HASH_RE = re.compile(r"^[0-9a-f]{64}$")
TOP_KEYS = {"sent_round", "clips"}
CLIP_KEYS = {"verdict", "tags", "pins", "frame_hash"}
PIN_KEYS = {"tick", "tags"}


def _int(v):
    return isinstance(v, int) and not isinstance(v, bool)


def _tags_ok(v):
    return (isinstance(v, list) and all(isinstance(t, str) and t in TAGS for t in v)
            and len(set(v)) == len(v))


def validate(verdict, manifest):
    """Problems with the verdict's shape; an empty list means it is well-formed."""
    if not isinstance(verdict, dict):
        return ["verdict is not an object"]
    problems = []
    if set(verdict) != TOP_KEYS:
        problems.append("verdict must have exactly the keys sent_round and clips")
    if not _int(verdict.get("sent_round")) or verdict.get("sent_round") < 0:
        problems.append("sent_round must be a whole number, 0 or more")
    clips = verdict.get("clips")
    if not isinstance(clips, dict):
        problems.append("clips must be an object")
        return problems
    ticks = {c["name"]: c["ticks"] for c in manifest["clips"]}
    unknown = sum(1 for k in clips if k not in ticks)
    if unknown:
        problems.append("%d clip entr%s not in this batch" % (unknown, "y is" if unknown == 1 else "ies are"))
    for name in ticks:
        if name not in clips:
            continue
        c = clips[name]
        where = "clip %s: " % name
        if not isinstance(c, dict):
            problems.append(where + "entry is not an object")
            continue
        if set(c) != CLIP_KEYS:
            problems.append(where + "entry must have exactly the keys verdict, tags, pins and frame_hash")
        if c.get("verdict") not in ("approved", "rejected"):
            problems.append(where + "verdict must be approved or rejected")
        if not _tags_ok(c.get("tags")):
            problems.append(where + "tags must be distinct tags from the fixed list")
        if not isinstance(c.get("frame_hash"), str) or not HASH_RE.match(c["frame_hash"]):
            problems.append(where + "frame_hash must be 64 lowercase hex characters")
        pins = c.get("pins")
        if not isinstance(pins, list):
            problems.append(where + "pins must be a list")
            continue
        for i, p in enumerate(pins):
            if (not isinstance(p, dict) or set(p) != PIN_KEYS or not _int(p.get("tick"))
                    or not 0 <= p["tick"] < ticks[name] or not _tags_ok(p.get("tags"))):
                problems.append(where + "pin %d must be a tick inside the clip with tags from the fixed list" % (i + 1))
        if c.get("verdict") == "approved" and (c.get("tags") or pins):
            problems.append(where + "an approved clip cannot carry tags or pins")
    return problems


def decide(manifest, verdict, lane_tip):
    """What the lane does next: invalid, wait, resubmit, fix or merge (checked in that order)."""
    names = [c["name"] for c in manifest["clips"]]
    if verdict is None:
        return {"action": "wait", "undecided": names}
    problems = validate(verdict, manifest)
    if problems:
        return {"action": "invalid", "problems": problems}
    given = verdict["clips"]
    undecided = [c["name"] for c in manifest["clips"]
                 if c["name"] not in given or given[c["name"]]["frame_hash"] != c["frame_hash"]]
    if verdict["sent_round"] != manifest["round"] or undecided:
        return {"action": "wait", "undecided": undecided}
    if lane_tip != manifest["submitted_sha"]:
        return {"action": "resubmit", "reason": "lane tip moved"}
    rejected = [{"name": n, "tags": list(given[n]["tags"]),
                 "pins": [{"tick": p["tick"], "tags": list(p["tags"])} for p in given[n]["pins"]]}
                for n in names if given[n]["verdict"] == "rejected"]
    if rejected:
        return {"action": "fix", "clips": rejected}
    return {"action": "merge", "sha": manifest["submitted_sha"], "message": "Kort approved in review booth, batch %s" % manifest["batch"]}
