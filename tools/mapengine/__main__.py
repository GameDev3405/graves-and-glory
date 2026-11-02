"""`python tools/mapengine <command>` entry point."""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent.parent))  # tools/: mapengine and mapforge import

from mapengine import cli  # noqa: E402

if __name__ == '__main__':
    sys.exit(cli.main(sys.argv[1:]))
