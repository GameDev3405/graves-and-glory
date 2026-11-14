"""Supply Wagon (`supply_wagon`): 3x3x6 cells, anchor cell (1,0,2). Brief: art/props/supply_wagon/brief.md. Shared code and the load: art/props/cartlib.py.
Build:  python art/props/build_props.py supply_wagon      Card:  python tools/prop_card.py supply_wagon
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from cartlib import *   # noqa: F401,F403


def build():
    return wagon_prop("supply_wagon", [])
