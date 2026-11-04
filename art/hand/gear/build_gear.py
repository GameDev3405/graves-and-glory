"""Build commander gear art. Usage:
  python art/hand/gear/build_gear.py contact <set> <out.png> [rarities, e.g. 1,5]   worn on the mannequin
  python art/hand/gear/build_gear.py close <set> <out.png> [rarities]              each piece alone, large
  python art/hand/gear/build_gear.py export <set>                                  sheets and models into the assets
Sets are the modules in sets/ (last_watch, ...). Export only once Kort has approved the set's contact sheet."""
import importlib, json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE); sys.path.insert(0, os.path.join(HERE, "sets"))
import gearlib

ASSETS = os.path.join(HERE, "..", "..", "..", "src", "main", "resources", "assets", "gravesandglory")

def set_pieces(name): return importlib.import_module(name).pieces()

def export(pieces, root=ASSETS):
    for p in pieces:
        for rel, data in p.outputs().items():
            path = os.path.join(root, rel); os.makedirs(os.path.dirname(path), exist_ok=True)
            if isinstance(data, dict):
                with open(path, "w", newline="\n") as f: json.dump(data, f, indent=1); f.write("\n")
            else: data.save(path)
        print("exported", p.item_id)

def main(argv):
    if len(argv) < 2: raise SystemExit(__doc__)
    cmd, name = argv[0], argv[1]
    pieces = set_pieces(name)
    rar = tuple(int(x) for x in argv[3].split(",")) if len(argv) > 3 else gearlib.RARITIES
    if cmd == "contact": gearlib.contact(pieces, argv[2], rar)
    elif cmd == "close": gearlib.pieces_close(pieces, argv[2], rar)
    elif cmd == "export": export(pieces)
    else: raise SystemExit(__doc__)

if __name__ == "__main__": main(sys.argv[1:])
