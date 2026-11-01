"""Entry point for `python tools/mapforge ...` (and `python -m mapforge` from tools/)."""
import pathlib
import sys

if __package__ in (None, ''):
    sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))
    from mapforge.cli import main
else:
    from .cli import main

sys.exit(main())
