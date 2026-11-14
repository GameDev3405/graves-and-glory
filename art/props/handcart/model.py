"""Handcart (`handcart`): 2x2x3 cells, anchor cell (0,0,1). Brief: art/props/handcart/brief.md. Shared code and the load: art/props/cartlib.py.
Build:  python art/props/build_props.py handcart      Card:  python tools/prop_card.py handcart
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from cartlib import *   # noqa: F401,F403


def build():
    return cart_prop("handcart", [])
