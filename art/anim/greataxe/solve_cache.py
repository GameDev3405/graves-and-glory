"""An on-disk memo for the greataxe builder's slow solves (fix round 2, 2026-11-12).

A full ic_breaker build solves every sample of every clip again, about two hours. The builder wraps its slow, pure steps
(the attack solve, the helm and chest clearances, the hit and layered fixes, the spear builder's idle, stance, hit and
death clips) with memo(): a call whose inputs are the same as a call made before returns the stored result instead of
solving again. It never changes what is built:

  key     sha256 over the step's name, the call's arguments and a fingerprint of the code: the step's own source and
          defaults, then every name its code (and the code of every function, nested or called, that it reaches) reads,
          resolved where the code would resolve it. A function is fingerprinted the same way, recursively; a project
          module's names are resolved as attributes; data (tables, keys, limits, arrays, the rig read from the geo) is
          hashed by value. A change to any key, table or helper a step reads gives a new key, so the step is solved again.
  value   the step's result (pickled), plus the arguments it changed in place (replayed on a hit, so a caller that relies
          on the change sees it).

    work/anim-cache/<troop>/<step>/<key[:2]>/<key>.pkl     (work/ is gitignored)
    GG_BREAKER_CACHE=0   builds without the cache (no reads, no writes)
    GG_BREAKER_CACHE_DIR=<dir>   another cache folder

Proof that it is exact: an empty cache gives a full solve; the build with a warm cache and the one from an empty cache
must write the same bytes (IC_BREAKER-REVIEW.md, fix round 2).
"""
from __future__ import annotations

import functools
import hashlib
import inspect
import os
import pickle
import re
import struct
import types
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[3]
ENABLED = os.environ.get("GG_BREAKER_CACHE", "1") != "0"
SKIP_NAMES = {"_SOFT", "_MEMO_STATS"}   # lazily filled caches of derived data: not inputs
STATS = {"hit": 0, "miss": 0}
_ADDR = re.compile(r" at 0x[0-9a-fA-F]+")


def cache_dir(troop):
    base = os.environ.get("GG_BREAKER_CACHE_DIR")
    return Path(base) if base else ROOT / "work" / "anim-cache" / troop


def _project(mod):
    f = getattr(mod, "__file__", None)
    if not f:
        return False
    p = Path(f).resolve()
    try:
        p.relative_to(ROOT)
    except ValueError:
        return False
    return "site-packages" not in p.parts


def _codes(code):
    yield code
    for c in code.co_consts:
        if isinstance(c, types.CodeType):
            yield from _codes(c)


class _Hasher:
    def __init__(self):
        self.h = hashlib.sha256()
        self.seen = {}

    def tag(self, s):
        b = s.encode("utf-8")
        self.h.update(struct.pack("<I", len(b)))
        self.h.update(b)

    def feed(self, o):
        t = type(o)
        if o is None or t in (bool, int, str):
            self.tag("%s:%r" % (t.__name__, o))
        elif t is float:
            self.tag("f:" + float.hex(o))
        elif t is bytes:
            self.tag("b:%d" % len(o))
            self.h.update(o)
        elif isinstance(o, np.ndarray):
            a = np.ascontiguousarray(o)
            self.tag("nd:%s:%s" % (a.dtype.str, a.shape))
            if a.dtype == object:
                for x in a.ravel():
                    self.feed(x)
            else:
                self.h.update(a.tobytes())
        elif isinstance(o, np.generic):
            self.tag("ng:%s" % o.dtype.str)
            self.h.update(o.tobytes())
        elif t in (tuple, list):
            self.tag("%s:%d" % (t.__name__, len(o)))
            for x in o:
                self.feed(x)
        elif isinstance(o, dict):
            self.tag("dict:%s:%d" % (t.__name__, len(o)))
            for k, v in o.items():
                self.feed(k)
                self.feed(v)
        elif isinstance(o, (set, frozenset)):
            ds = sorted(_digest(x) for x in o)
            self.tag("set:%d:%s" % (len(ds), ",".join(ds)))
        elif isinstance(o, Path):
            self.tag("path:" + o.as_posix())
        elif isinstance(o, (types.FunctionType, types.MethodType, functools.partial)):
            self.function(o)
        elif isinstance(o, types.ModuleType):
            self.tag("module:" + o.__name__)
        elif isinstance(o, type):
            self.klass(o)
        elif isinstance(o, (types.BuiltinFunctionType, np.ufunc)):
            self.tag("builtin:%s" % getattr(o, "__qualname__", getattr(o, "__name__", repr(o))))
        elif hasattr(o, "__dict__") and not isinstance(o, type):
            self.tag("obj:%s.%s" % (t.__module__, t.__qualname__))
            self.feed({k: v for k, v in vars(o).items()})
            if _project(inspect.getmodule(t) or types):
                self.klass(t)
        else:
            self.tag("repr:%s:%s" % (t.__qualname__, _ADDR.sub("", repr(o))))

    def klass(self, c):
        mod = inspect.getmodule(c)
        if mod is None or not _project(mod):
            self.tag("class:%s.%s" % (c.__module__, c.__qualname__))
            return
        key = ("class", id(c))
        if key in self.seen:
            self.tag("seen:" + c.__qualname__)
            return
        self.seen[key] = True
        self.tag("class:%s.%s" % (c.__module__, c.__qualname__))
        try:
            self.tag(inspect.getsource(c))
        except (OSError, TypeError):
            pass
        for v in vars(c).values():
            if isinstance(v, (types.FunctionType, staticmethod, classmethod)):
                self.function(getattr(v, "__func__", v))

    def function(self, fn):
        if isinstance(fn, functools.partial):
            self.tag("partial")
            self.function(fn.func)
            self.feed(fn.args)
            self.feed(fn.keywords)
            return
        if isinstance(fn, types.MethodType):
            self.tag("method")
            self.feed(fn.__self__)
            fn = fn.__func__
        fn = inspect.unwrap(fn)
        mod = inspect.getmodule(fn)
        name = "%s.%s" % (getattr(fn, "__module__", "?"), fn.__qualname__)
        if mod is None or not _project(mod):
            self.tag("ext:" + name)
            return
        key = ("fn", id(fn))
        if key in self.seen:
            self.tag("seen:" + name)
            return
        self.seen[key] = True
        self.tag("fn:" + name)
        code = fn.__code__
        try:
            self.tag(inspect.getsource(fn))
        except (OSError, TypeError):
            for c in _codes(code):
                self.h.update(c.co_code)
        self.feed(fn.__defaults__)
        self.feed(fn.__kwdefaults__)
        for c in _codes(code):
            self.feed(tuple(x for x in c.co_consts if not isinstance(x, types.CodeType)))
        if fn.__closure__:
            for cell in fn.__closure__:
                try:
                    self.feed(cell.cell_contents)
                except ValueError:
                    self.tag("empty-cell")
        names = sorted({n for c in _codes(code) for n in c.co_names})
        g = fn.__globals__
        mods = [g[n] for n in names if isinstance(g.get(n), types.ModuleType) and _project(g[n])]
        for n in names:
            if n in SKIP_NAMES:
                continue
            if n in g:
                v = g[n]
                if isinstance(v, types.ModuleType):
                    self.tag("mod:" + v.__name__)
                    continue
                self.tag("g:" + n)
                self.feed(v)
            for m in mods:   # SP.foo, K.bar: the attribute the code reads on a project module
                if n in vars(m) and n not in SKIP_NAMES and not isinstance(vars(m)[n], types.ModuleType):
                    self.tag("a:%s.%s" % (m.__name__, n))
                    self.feed(vars(m)[n])

    def hexdigest(self):
        return self.h.hexdigest()


def _digest(o):
    h = _Hasher()
    h.feed(o)
    return h.hexdigest()


def memo(fn, troop, name=None):
    """fn wrapped with the on-disk memo (see the module docstring); fn must be deterministic in its inputs."""
    step = name or fn.__qualname__

    @functools.wraps(fn)
    def wrapper(*a, **k):
        if not ENABLED:
            return fn(*a, **k)
        h = _Hasher()
        h.tag("step:" + step)
        h.feed(sorted((k_, v_) for k_, v_ in os.environ.items() if k_.startswith(("GG_", "GREATAXE", "SPEAR"))
                      and not k_.startswith("GG_BREAKER_CACHE")))
        h.function(fn)
        h.tag("args")
        h.feed(a)
        h.feed(k)
        key = h.hexdigest()
        path = cache_dir(troop) / step / key[:2] / (key + ".pkl")
        if path.is_file():
            with open(path, "rb") as fh:
                rec = pickle.load(fh)
            for i, new in rec["muts"]:
                _replay(a[i], new)
            STATS["hit"] += 1
            return a[rec["res_arg"]] if rec["res_arg"] is not None else rec["res"]
        before = [(_digest(x) if isinstance(x, (list, dict)) else None) for x in a]
        res = fn(*a, **k)
        muts = [(i, x) for i, x in enumerate(a) if before[i] is not None and _digest(x) != before[i]]
        res_arg = next((i for i, x in enumerate(a) if res is x), None)
        rec = {"res": None if res_arg is not None else res, "res_arg": res_arg, "muts": muts}
        path.parent.mkdir(parents=True, exist_ok=True)
        tmp = path.with_suffix(".tmp%d" % os.getpid())
        with open(tmp, "wb") as fh:
            pickle.dump(rec, fh, protocol=pickle.HIGHEST_PROTOCOL)
        os.replace(tmp, path)
        STATS["miss"] += 1
        return res
    return wrapper


def _replay(dst, new):
    if isinstance(dst, list):
        dst[:] = new
    elif isinstance(dst, dict):
        dst.clear()
        dst.update(new)
