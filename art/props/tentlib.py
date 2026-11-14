"""Shared ground for the tent colour variants (`tent_pavilion_large_*`, `tent_soldier_large_*`).

The two source models (`tent_pavilion_large/model.py`, `tent_soldier_large/model.py`) take a palette; a variant's
model.py is a thin wrapper that loads the source module and calls its `build(pid, palette)`. Geometry, collision and
solid cells therefore stay the same as the source's; only ramps and weathering change.

Team colours (Kort, 2026-11-13): Dawn teal UI #5fb3ad / ink #1b4a47, Dusk purple UI #a07cc2 / ink #52376d.
"""
import importlib.util, os
from proplib import hx, make_ramp

HERE = os.path.dirname(os.path.abspath(__file__))


def load_model(pid):
    """Import art/props/<pid>/model.py by path (the model folders are not packages)."""
    spec = importlib.util.spec_from_file_location("prop_model_" + pid, os.path.join(HERE, pid, "model.py"))
    mod = importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)
    return mod


def _mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


def team_ramp(ui, ink):
    """5 tones: 0 = the team's deep ink, 3 = its UI colour, 2 the working base between them, 4 a pale highlight."""
    ui, ink = hx(ui), hx(ink)
    return [ink, _mix(ink, ui, 0.45), _mix(ink, ui, 0.78), ui, _mix(ui, (244, 240, 222, 255), 0.45)]


DAWN = team_ramp("5fb3ad", "1b4a47")
DUSK = team_ramp("a07cc2", "52376d")

# neutral ramps shared by the variants
LINEN = make_ramp("D8CCA6", 0.15)          # unbleached linen (a touch duller than the original cream)
MUD = make_ramp("6A5841", 0.18)            # dried camp mud, a hem splash
PATCH_NEW = make_ramp("CDC3A2", 0.14)      # a patch of newer, paler canvas
PATCH_TAR = make_ramp("5F594A", 0.16)      # a tarred, dark patch
