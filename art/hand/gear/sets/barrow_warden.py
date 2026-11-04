"""Barrow Warden, a House armour set Kort approved on 2026-11-04: authored in ../house/barrow_warden.py (from 5ff7ed4c), drawn in the gear
frame by houselib. Kort approved the set's data 2026-11-04 (#113); its ids sit in ids.json's items and house lists."""
import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import houselib

def pieces(): return houselib.pieces("barrow_warden")
