# Map engine how-to

Design a battlefield in the browser, then turn it into structure files. Everything runs on your machine; nothing is installed.

## Start

    python tools/mapengine serve

It opens the editor in your browser at 127.0.0.1. Add `--no-open` to skip the browser and `--port 8766` to pick a port. In the Claude browser pane, the launch entry is called `mapengine` and lives in `.claude/launch.json`, which git ignores, so a fresh clone does not have it.

The editor opens on the design you edited last, or a blank one. Work saves itself one second after each edit.

## Tools and keys

- 1 Hill, 2 River, 3 Road: click to add points, double-click or Enter to finish, Escape cancels, Backspace removes the last point.
- 4 Point, 5 Spawn: one click places it.
- 6 Select: click to pick, drag to move, Delete removes.
- Ctrl+Z undo, Ctrl+Y or Ctrl+Shift+Z redo.
- Map view: wheel zooms, middle-drag or Space-drag pans.
- Tab flips between the map and the 3D view. The same tools work in both.
- 3D view: WASD moves, Space goes up, Shift goes down, right-drag looks, the wheel changes speed. F toggles walking with gravity (Space jumps, Shift runs).

The panel on the right edits the selected object. "Save version" keeps a named copy; "Versions" restores one.

## Build and Check

Use the Build button, or the command line:

    python tools/mapengine build <id>
    python tools/mapengine check <id>

Build refuses a design with no Dusk or Dawn spawn or no points. Check lists problems such as a point in water or one a spawn cannot reach.

## Where things go

- Designs: `maps/<id>/design.json`, with saved versions in `maps/<id>/versions/`.
- Build output: `work/mapengine/<id>/`.
