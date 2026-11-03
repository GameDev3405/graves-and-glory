# Map engine how-to

Design a battlefield in the browser, then turn it into structure files. Everything runs on your machine; nothing is installed.

## Start

    python tools/mapengine serve

It opens the editor in your browser at 127.0.0.1. Add `--no-open` to skip the browser and `--port 8766` to pick a port. In the Claude browser pane, the launch entry is called `mapengine` and lives in `.claude/launch.json`, which git ignores, so a fresh clone does not have it.

The editor opens on the design you edited last, or a blank one. Work saves itself one second after each edit.

## Tools and keys

- 1 Hill, 2 River, 3 Road: click to add points, double-click, Enter or Shift (map view) to finish, Escape cancels, Backspace removes the last point.
- 4 Point, 5 Spawn: one click places it.
- 6 Select: click to pick, drag to move, Delete removes.
- 7 Note: one click places a note.
- 8 Building: pick the piece in the panel, R turns it 90 degrees clockwise, one click places it. The footprint and its door cells show under the cursor.
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

## Buildings

Buildings come from the approved structure pieces in `src/main/resources/data/gravesandglory/structures/`. The library index, `tools/mapengine/library.json`, lists each piece's footprint, height, door side and group. It is generated from the .nbt files:

    python tools/mapengine library          # rewrite the index
    python tools/mapengine library --check  # fail if it is stale

The door side is read from door blocks, or else guessed from which side lets you walk under the roof. `open` means no roof (a yard or ward), `none` means no way in (tents, stairs). Fix a wrong guess in `tools/mapengine/library_overrides.json`, keyed by piece id.

The ground under each footprint is levelled to the height at its centre and drained of water. Build stamps the piece's blocks into the tiles, so the mod places it with no mod change. Chest contents, sign text and banner patterns are lost, because tiles hold block states only. map.json also lists each building (piece, position, rotation) for a later mod reader.

Check flags `building_bounds` (off the canvas), `building_overlap`, `door_blocked` (no dry, level, open cell in front of the door) and `building_point` (a point or spawn inside a footprint).

The AI brief lists the library, and the AI may only use those pieces. Buildings follow the usual locking rule: Kort's and accepted ones cannot be changed by an import.

## Where things go

- Designs: `maps/<id>/design.json`, with saved versions in `maps/<id>/versions/`.
- Build output: `work/mapengine/<id>/`.

## Your five maps

One starting file per live map, each 700 by 500 (the arena box, |x| <= 350 and |z| <= 250; canvas x = world x + 350, canvas z = world z + 250): `maps/rivergate`, `maps/ironhold`, `maps/frostmarch`, `maps/hollowwood`, `maps/kingsfield`. Each holds only today's spawns (Dawn at world -283, 0 and Dusk at +283, 0) and today's Domination capture points, plus a note at each Siege (Ironhold), Convoy (Frostmarch) and Survival (Hollowwood) site. No terrain, rivers or buildings: you design those. Ironhold's spawns are the shipped ones, not the signed rebuild layout in `docs/MAP-STANDARDS.md` section 9.

Start the editor, then open a map by its name in the address:

    python tools/mapengine serve

- Rivergate: http://127.0.0.1:8765/#rivergate
- Ironhold: http://127.0.0.1:8765/#ironhold
- Frostmarch: http://127.0.0.1:8765/#frostmarch
- Hollowwood: http://127.0.0.1:8765/#hollowwood
- Kingsfield: http://127.0.0.1:8765/#kingsfield

Without the `#name` the editor opens the design saved last. To check one: `python tools/mapengine check <name>`.
