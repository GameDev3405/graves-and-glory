"""Smoke Vent (thick) (`smoke_vent_thick`): an effect-only prop (1x1x1 cells, anchor cell (0,0,0)): nothing to see, no solid cell,
only the def's `effects` (vanilla particles and sounds). Brief: art/props/smoke_vent_thick/brief.md. Card:  python tools/prop_card.py smoke_vent_thick
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from proplib import Prop   # noqa: F401


def build():
    return Prop("smoke_vent_thick", {"smoke_vent_thick": []})
