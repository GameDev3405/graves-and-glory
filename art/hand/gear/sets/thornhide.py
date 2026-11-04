"""Thornhide, a House armour set Kort approved on 2026-11-04: authored in ../house/thornhide.py (from 5ff7ed4c), drawn in the gear
frame by houselib. Art only until Kort answers question #113; the ids sit in ids.json's art_only list."""
import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import houselib

def pieces(): return houselib.pieces("thornhide")
