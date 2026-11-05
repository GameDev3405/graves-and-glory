# Map Engine Slice 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Kort can open a local browser app and do the following:
- paint hills (outline, height, steepness), rivers and roads;
- place capture points and spawns;
- flip with Tab to a 3D walk of the same map and move things there;
- undo and redo, and save named versions;
- press Build to get deterministic structure tiles plus a check report against the design.

**Architecture:**
- A Python package, `tools/mapengine`, owns everything:
  - the design file schema;
  - the one terrain implementation (numpy heightfield);
  - the block builder (reuses `tools/mapforge` NBT and Structure);
  - the checker;
  - a small local HTTP server.
- The browser app in `tools/mapengine/web` is plain ES modules plus vendored three.js. It never computes terrain itself. It asks the server for the heightfield after each edit, so what the editor shows and what gets built come from the same code.

**Tech Stack:**
- Python 3.12 (stdlib `http.server`, numpy 2.x already used in `tools/`), `unittest`;
- three.js 0.160.0, vendored;
- plain JS, no Node or build step.

**Spec:** `docs/superpowers/specs/2026-11-01-map-engine-design.md`

**Later plans (not this one):**
- **Plan 2:** a mod reader that builds a battle map from the engine output, then walking it in Minecraft and a game test.
- **Plan 3:** the AI handoff:
  - export with notes and a brief;
  - draft import with tracked-change tint;
  - accept or reject per object;
  - locking and schema rejections.
- **Plan 4:** the Graves and Glory look: signature building library, per-zone palettes beyond the two starters, worn detail, and the fairness panel.
- **Plan 5:** in-game fix notes, which need a dev command Kort must approve.

## Global Constraints

- **Runs locally:** offline, one command (`python tools/mapengine serve`), bound to 127.0.0.1 only. No Node, no install step.
- **Determinism:** the same design file gives the same output bytes. Build output uses `mapforge.nbt.write_file` (gzip mtime 0).
- **Structure format:** vanilla 1.20.1, `DATA_VERSION = 3465` (from `mapforge.structure`), vanilla block states only.
- **Canvas:**
  - the default size is 700 by 500 blocks (x by z), with ground level `baseY = 64`;
  - design coordinates are local block units, 0 ≤ x < width, 0 ≤ z < depth;
  - heights are relative to `baseY`.
- **Gameplay:** no mod Java changes in this plan, so no gameplay or save change.
- **Files and output:**
  - designs live in `maps/<id>/design.json` (committed), and saved versions in `maps/<id>/versions/<name>.json`;
  - build output goes to `work/mapengine/<id>/`, which `work/` already ignores.
- **Tests and verify:**
  - tests go in `test-tools/standalone/test_mapengine_*.py`, so `python tools/verify.py fast --offline` runs them;
  - `tools/verify.py fast --offline` must stay green.
- **"Not fiddly":** the app opens straight into the map view of the last-edited design, or a new blank one. Painting works with no setup dialog.
- **Git:** one writer per checkout, no push or publish, and commits end with the session's attribution trailer.

## Review Focus

1. **Overlaps:** a hill drawn across a river, or a point placed in water. The river must win (carve through the hill), and the checker must flag the point. Tests: `test_river_carves_hill`, `test_point_in_water_flagged`.
2. **Self-crossing or tiny outlines:** a hill outline with fewer than 3 vertices, or one that crosses itself. Validation rejects fewer than 3. A self-crossing outline still builds, using even-odd fill, and never crashes. Test: `test_bad_outlines`.
3. **Objects off the canvas edge:** a road running past the canvas, or a spawn at x = width. Validation rejects points and spawns outside the canvas. Paths and outlines are clipped by the raster. Test: `test_out_of_bounds`.
4. **Missing spawn or points:** a design with no Dusk spawn, or no points, still saves, because drafts are allowed. Build refuses with a clear message. Test: `test_build_needs_spawns_and_points`.
5. **Lost work:** a browser reload or server restart after edits must not lose the last saved state. The app autosaves to `design.json` 1 s after the last edit, and reloads it on open. Test: `test_put_then_get_roundtrip`, plus a browser check in Task 6.

---

### Task 1: Design file schema and validation

**Files:**
- Create: `tools/mapengine/__init__.py`, `tools/mapengine/__main__.py` (dispatches to `cli.main`), `tools/mapengine/cli.py`, `tools/mapengine/design.py`
- Test: `test-tools/standalone/test_mapengine_design.py`

**Interfaces:**
- Produces:
  - `FORMAT = 1`
  - `KINDS = ('hill', 'river', 'road', 'point', 'spawn', 'zone')`
  - `new_design(id: str, name: str, width=700, depth=500) -> dict`
  - `validate(d: dict) -> list[str]`: empty when valid. Messages name the object id.
  - `build_problems(d: dict) -> list[str]`: extra rules for building (both spawns, at least one point).
  - `load(path) -> dict`: raises `DesignError(list[str])` when invalid.
  - `save(path, d: dict) -> None`: JSON, sorted keys, 1-space indent, trailing newline.
  - `next_id(d: dict, kind: str) -> str`: `"<kind>-<n>"`, where n is one more than the highest number in use for that kind.
- Design dict, as the schema:

  ```json
  {"format":1,"id":"ironhold","name":"Ironhold","width":700,"depth":500,"baseY":64,
   "objects":[
    {"id":"hill-1","kind":"hill","outline":[[x,z],...],"height":18,"steepness":0.7,"by":"kort","state":"accepted"},
    {"id":"river-1","kind":"river","path":[[x,z],...],"width":9,"depth":3,"by":"kort","state":"accepted"},
    {"id":"road-1","kind":"road","path":[[x,z],...],"width":5,"by":"kort","state":"accepted"},
    {"id":"point-1","kind":"point","name":"A","x":120,"z":250,"radius":8,"by":"kort","state":"accepted"},
    {"id":"spawn-1","kind":"spawn","team":"dawn","x":20,"z":250,"by":"kort","state":"accepted"},
    {"id":"zone-1","kind":"zone","outline":[[x,z],...],"palette":"ironhold","by":"kort","state":"accepted"}]}
  ```

  - `by` is one of `kort` or `ai`.
  - `state` is one of `accepted` or `proposed`.
  - Plan 3 uses `by` and `state`. This plan always writes `kort` and `accepted`.
- **Validation rules:**
  - Ids are unique.
  - The kind is in `KINDS`.
  - An outline has at least 3 vertices.
  - A path has at least 2 vertices.
  - A hill's height is an integer from -40 to 120.
  - Steepness is a number from 0 to 1.
  - A river's width is 1 to 64 and its depth 1 to 12.
  - A road's width is 1 to 16.
  - A point's radius is 2 to 32.
  - A team is `dawn` or `dusk`.
  - Point and spawn x and z are inside the canvas.
  - A zone's palette is a key in `palettes.json` (Task 3).
  - The format equals 1.

- [ ] **Step 1: Write failing tests**

  The file is `test-tools/standalone/test_mapengine_design.py`. Use the same `sys.path` insert as `test_mapforge.py`.

  - `test_new_design_is_valid`: `validate(new_design('t','T')) == []`, width 700, depth 500, baseY 64, objects empty.
  - `test_bad_outlines`: a hill with 2 vertices gives one message containing `hill-1`. A self-crossing 4-vertex outline is valid.
  - `test_out_of_bounds`: a spawn at x = 700 on a width-700 map is invalid. A road path vertex at x = -50 is valid.
  - `test_duplicate_ids`: two `point-1` objects give a message containing "duplicate".
  - `test_build_needs_spawns_and_points`: `build_problems(new_design(...))` mentions `dawn`, `dusk` and `point`.
  - `test_save_load_roundtrip_stable`: saving the same dict twice gives identical bytes, and `load` returns an equal dict.
  - `test_next_id`: with `hill-1` and `hill-3` present, `next_id(d,'hill') == 'hill-4'`.

- [ ] **Step 2: Run and see it fail.** Run `python -m unittest test-tools/standalone/test_mapengine_design.py -v`. Expected: ImportError for `mapengine`.

- [ ] **Step 3: Implement `design.py` and `cli.py`.**
  - `cli.main(argv)` uses argparse with subcommands `new <id> <name>`, `check <id>`, `build <id>` and `serve [--port 8765] [--no-open]`.
  - `new` writes `maps/<id>/design.json`.
  - The others are wired up in later tasks.

- [ ] **Step 4: Run the tests.** Expected: all PASS.

- [ ] **Step 5: Commit.** "Add the map engine design file and its checks"

### Task 2: Terrain heightfield

**Files:**
- Create: `tools/mapengine/terrain.py`
- Test: `test-tools/standalone/test_mapengine_terrain.py`

**Interfaces:**
- Consumes: the Task 1 design dict.
- Produces:
  - `Field`, a dataclass with these numpy arrays of shape (depth, width), indexed `[z, x]`:
    - `height: int16`: the ground surface height relative to baseY;
    - `water: int16`: the water surface relative to baseY, or `NO_WATER = -32768`;
    - `road: bool`;
    - `zone: int16`: an index into `zones`, or -1.
  - `Field.zones: list[str]`: palette names.
  - `heightfield(d: dict) -> Field`
- **Algorithm.** Pin these exactly, because the editor and the build share them:
  - **Hills** (in object order):
    - Rasterise the outline with an even-odd fill at cell centres (x + 0.5, z + 0.5).
    - Compute `inside_dist`, the Euclidean distance from each inside cell to the nearest outside cell. Use `scipy` only if it is already importable; otherwise a two-pass chamfer (3-4) distance divided by 3.
    - `ramp = 2 + round((1 - steepness) * 30)` blocks.
    - `h = round(height * smoothstep(clip(inside_dist / ramp, 0, 1)))`, where `smoothstep(t) = t*t*(3-2t)`.
    - Combine: positive heights use `max` with the current field. Negative heights (hollows) use `min`.
  - **Rivers** (after all hills):
    - For each cell within `width/2` of the polyline (distance to segments, cell centres), set `height = min(height, -depth)` and `water = 0`.
    - Banks: within `width/2 + 3`, clamp the height to at most `(dist - width/2)`, so the land slopes up 1 block per block from the water.
  - **Roads:** `road = True` within `width/2`. Heights are unchanged in this slice.
  - **Zones:** even-odd raster. A later zone overwrites an earlier one.

- [ ] **Step 1: Write failing tests.** All use small 64×64 designs.
  - `test_flat_when_empty`: all heights 0, no water, no roads, zone -1.
  - `test_hill_peak_and_edge`: a 40×40 square hill with height 20 and steepness 1.0 is 20 at the centre, 0 just outside, and above 0 on the first inside ring. With steepness 0.0, the cell 3 blocks inside the edge is lower than with steepness 1.0.
  - `test_hill_order_independent_for_max`: swapping two overlapping positive hills gives the same field.
  - `test_river_carves_hill`: a river through a hill gives height -depth and water 0 on the centreline, and a height at the bank ring of at most 1.
  - `test_road_mask_width`: a road of width 5 along z = 32 marks z = 30 to 34 and not z = 29 or z = 35, at x = 10.
  - `test_deterministic`: two calls give equal arrays.
  - `test_full_size_speed`: a 700×500 design with 6 hills, 1 river and 2 roads runs `heightfield` in under 0.5 s. This guards the "fast to change" goal.

- [ ] **Step 2: Run and see it fail.**

- [ ] **Step 3: Implement `terrain.py` with numpy** as described above. Vectorise segment distances over the grid, one segment at a time.

- [ ] **Step 4: Run the tests.** Expected: PASS, with the speed test under 0.5 s.

- [ ] **Step 5: Commit.** "Turn map engine shapes into a heightfield"

### Task 3: Block builder (tiles and map data)

**Files:**
- Create: `tools/mapengine/build.py`, `tools/mapengine/palettes.json`
- Test: `test-tools/standalone/test_mapengine_build.py`

**Interfaces:**
- Consumes:
  - `terrain.heightfield`;
  - `design.build_problems`;
  - `mapforge.structure.Structure(size, palette, blocks)` and `.save(path)`;
  - `mapforge.structure.DATA_VERSION`.
- Produces:
  - `TILE = 64`
  - `FLOOR = -16`: the lowest built layer relative to baseY.
  - `build(d: dict, out_dir: Path) -> dict`: returns the manifest it writes to `out_dir/map.json`. Raises `BuildError(list[str])` when `build_problems` is non-empty.
  - The manifest:

    ```json
    {"format":1,"id":..,"baseY":64,"floorY":48,"tiles":[{"file":"tiles/t_0_0.nbt","x":0,"z":0,"size":[64,H,64]}],
     "points":[{"name":"A","x":..,"y":..,"z":..,"radius":8}],"spawns":[{"team":"dawn","x":..,"y":..,"z":..}],"design_sha256":"..."}
    ```

    - `y` is the absolute standing height: `baseY + height + 1`.
    - `H = maxHeight - FLOOR + 1`, where `maxHeight` is the highest ground or water level in the design. It is shared by all tiles.
- `palettes.json` holds palette name → `{surface, under, deep, bed, water, road}` as block-state strings:
  - `default`: grass_block, dirt, stone, gravel, water, dirt_path.
  - `ironhold`: stone, cobblestone, deepslate, gravel, water, cobblestone.
  - `frostmarch`: snow_block, packed_ice, stone, gravel, water, packed_ice.
- **Column fill,** per cell:
  - `deep` from FLOOR up to height - 4;
  - `under` for the next 3 layers;
  - the top layer is `surface`, `road` where the road mask is set, or `bed` under water;
  - `water` from height + 1 up to the water level.

  The palette is chosen by the cell's zone (`default` when -1).
- **Determinism:**
  - The structure palette is ordered by first appearance.
  - Scan order: x outer, then z, then y ascending.
  - Tiles are written in tz then tx order.
  - The manifest is saved with sorted keys.

- [ ] **Step 1: Write failing tests.** All use small designs: 128×64, giving 2 tiles.
  - `test_build_needs_spawns_and_points`: `BuildError` mentions `dusk`.
  - `test_tiles_and_manifest`: 2 tile files exist, and the manifest lists both with `size[0] == 64`. Point A's `y` equals `64 + height + 1` at its cell.
  - `test_column_blocks`: at a hill cell with height 10, local y (10 - FLOOR) is the surface block and the next y up is absent (air is not stored). At a river centre, the bed is gravel with water above it.
  - `test_zone_palette`: a cell inside an `ironhold` zone has a `minecraft:stone` surface.
  - `test_same_design_same_bytes`: building twice into two temporary directories gives byte-identical tile files and manifest.
  - `test_tiles_roundtrip_mapforge`: `Structure.load(tile)` reads back, with `data_version == 3465`.

- [ ] **Step 2: Run and see it fail.**

- [ ] **Step 3: Implement `build.py`.** Build one tile at a time, so memory stays bounded. Wire `cli build <id>`, which writes to `work/mapengine/<id>/` and prints the tile count and output path.

- [ ] **Step 4: Run the tests.** Expected: PASS. Then run `python tools/mapengine build <a 700×500 sample>` (Task 4 commits the sample) in under 90 s.

- [ ] **Step 5: Commit.** "Build map engine designs into structure tiles"

### Task 4: Checker (built output against the design)

**Files:**
- Create: `tools/mapengine/check.py`, `maps/sample/design.json`: a 700×500 sample with 3 hills, 1 river, 1 road, points A to C and both spawns.
- Test: `test-tools/standalone/test_mapengine_check.py`

**Interfaces:**
- Consumes: the Task 3 manifest and tiles, read with `Structure.load`, and the design.
- Produces: `check(d: dict, out_dir: Path) -> list[Issue]`, where `Issue` is a dataclass `(code: str, object_id: str, message: str)`. The codes:
  - `stale`: `design_sha256` doesn't match the current design.
  - `moved`: a manifest point or spawn differs from the design's x and z, or from the built ground y.
  - `in_water`: a point or spawn cell has water.
  - `unreachable`: there is no walkable route from a spawn to a point. Walkable means a 4-neighbour step with a height change of at most 1, through water no deeper than 1.
  - `radius_clipped`: the point's radius disc leaves the canvas.
- The walkability grid is rebuilt from the tiles' top solid block, not from `terrain.py`, so the check covers what was actually built.
- `cli check <id>` builds if needed, then prints one line per issue, plus "OK: matches the design" when there are none. It exits 1 if there are issues.

- [ ] **Step 1: Write failing tests.**
  - `test_sample_builds_clean`: build `maps/sample` into a temporary directory, then `check` gives `[]`.
  - `test_point_in_water_flagged`: a point on the river centreline gives `in_water` for that point's id.
  - `test_cliff_ring_unreachable`: a point on a plateau with height 20 and steepness 1.0 gives `unreachable`.
  - `test_stale_after_edit`: moving a point in the design after building gives `stale`.
  - `test_moved_detects_tampered_manifest`: editing the manifest's point x gives `moved`.

- [ ] **Step 2: Run and see it fail.**

- [ ] **Step 3: Implement `check.py`** (BFS on a numpy grid) and wire up the CLI.

- [ ] **Step 4: Run the tests.** Expected: PASS. Then `python tools/mapengine check sample` prints "OK: matches the design".

- [ ] **Step 5: Commit.** "Check built maps against the design"

### Task 5: Local server, autosave and saved versions

**Files:**
- Create: `tools/mapengine/server.py`
- Test: `test-tools/standalone/test_mapengine_server.py`

**Interfaces:**
- Consumes: `design.load/save/validate/new_design`, `terrain.heightfield`, `build.build`, `check.check`.
- Produces: `make_server(maps_dir: Path, work_dir: Path, port: int) -> ThreadingHTTPServer`, bound to `127.0.0.1`. Routes (JSON unless noted):
  - `GET /`, `/app.js` and similar: static files from `tools/mapengine/web/`. Path traversal is rejected with 404.
  - `GET /api/designs`: `[{"id","name","mtime"}]`, newest first.
  - `GET /api/design/<id>` → the design.
  - `PUT /api/design/<id>` → validates.
    - On validation errors: 400 with `{"errors": [...]}`.
    - Otherwise: saves, and returns `{"ok": true}`.
  - `POST /api/design/<id>/versions` with `{"name"}`: copies the current design to `versions/<name>.json`. The name matches `[A-Za-z0-9 _-]{1,40}`.
  - `GET /api/design/<id>/versions`: the list.
  - `POST /api/design/<id>/restore` with `{"name"}`: puts that version back as `design.json`.
  - `GET /api/design/<id>/field`: `{"width","depth","height": base64 int16 little-endian, "water": base64, "road": base64 uint8, "zone": base64 int16, "zones": [...]}`.
  - `POST /api/design/<id>/build`: `{"manifest": ..., "issues": [{code, object_id, message}]}`. A `BuildError` returns 400 with its messages.
- `cli serve` starts the server and opens `http://127.0.0.1:<port>/` with `webbrowser`, unless `--no-open` is passed.

- [ ] **Step 1: Write failing tests.** Start the server on port 0 in a thread, against temporary directories.
  - `test_put_then_get_roundtrip`
  - `test_put_invalid_400`
  - `test_field_decodes_to_heightfield`: decoding the base64 `height` equals `heightfield(d).height`.
  - `test_versions_save_list_restore`
  - `test_version_name_rejected`: `../x` gives 400.
  - `test_static_traversal_404`: `/../design.py` gives 404.
  - `test_build_endpoint_reports_issues`

- [ ] **Step 2: Run and see it fail.**

- [ ] **Step 3: Implement `server.py`** with stdlib only.

- [ ] **Step 4: Run the tests.** Expected: PASS.

- [ ] **Step 5: Commit.** "Serve the map engine locally with saved versions"

### Task 6: Map view (top-down editor)

**Files:**
- Create in `tools/mapengine/web/`: `index.html`, `style.css`, `app.js` (state, undo and redo, autosave, Tab switch), `api.js` (fetch wrappers for the Task 5 routes), `tools.js`, `map2d.js` and `panel.js`.

**Interfaces:**
- Consumes: the Task 5 routes.
- Produces, for Task 7:
  - `tools.js` exports `TOOLS`: `hill`, `river`, `road`, `point`, `spawn`, `select`.
  - Each tool has `{down(state, x, z, ev), move(state, x, z, ev), up(state, x, z, ev), key(state, ev)}`.
  - Tools receive map block coordinates only. Any view that can turn a pointer into ground (x, z) can drive them.
  - `app.js` exports:
    - `state`: `{design, selectedId, tool, field, view: 'map'|'walk'}`;
    - `commit(mutator)`: applies the change, pushes undo, schedules autosave and a field refresh;
    - `undo()` and `redo()`;
    - `onFieldChange(cb)`.
- **Behaviour:**
  - **Opening:** the app opens the newest design from `GET /api/designs`, or creates `untitled` with no dialog.
  - **Map canvas:**
    - fills the screen between the left tool bar and the right panel;
    - draws the heightfield as a shaded relief (hillshade), water in blue and roads in tan;
    - draws a 16-block grid, with block labels every 64;
    - shows a cursor read-out of `x, z, height`;
    - mouse wheel zooms around the cursor, and middle-drag or space-drag pans.
  - **Tools:**
    - Hill, river and road: click to add vertices; double-click or Enter finishes; Escape cancels.
    - Point and spawn: a single click places. The spawn team alternates dawn and dusk, editable in the panel.
    - Select: click picks the top object. Dragging moves the whole object, or a vertex when the click is within 6 screen px of one. Delete removes the object.
  - **Right panel** shows the selected object's fields:
    - hill: height, steepness;
    - river: width, depth;
    - road: width;
    - point: name, radius;
    - spawn: team.

    Each field is a number input plus a slider, and every change goes through `commit`.
  - **Undo and redo:**
    - Ctrl+Z and Ctrl+Y (Ctrl+Shift+Z also redoes);
    - undo holds 200 design snapshots;
    - a drag counts as one undo step.
  - **Autosave:** a PUT 1 s after the last commit. A 400 shows the errors in a red bar and keeps the local state.
  - **Versions:** a top bar has a "Save version" name prompt and a version list with Restore.
  - **Build:** a Build button in the top bar shows the issues list. Clicking an issue selects its object.
  - **Field refresh:** after each commit, the app fetches the field (with only the latest request applied) and redraws. While dragging, the dragged shape is drawn as an outline right away and the relief updates on release.

- [ ] **Step 1: Verify in the browser pane.**
  1. Add a `mapengine` entry to `.claude/launch.json`: `python tools/mapengine serve --no-open --port 8765`, port 8765.
  2. Start it with `preview_start`.
  3. Draw a hill, a river and a road, then place A, B, C and both spawns, using `computer` clicks.
  4. Confirm that `GET /api/design/<id>` contains 8 objects.
  5. Drag point A and confirm its x changed. Undo, and confirm x is back.
  6. Reload the page and confirm the objects are still there (Review Focus 5).
  7. Press Build and confirm the issues list renders.
  8. Check `read_console_messages` for errors: expected none.

- [ ] **Step 2: Fix until all of Step 1 holds, then take a screenshot for the report.**

- [ ] **Step 3: Commit.** "Add the map engine's top-down editor"

### Task 7: 3D walk view and Tab flip

**Files:**
- Create:
  - `tools/mapengine/web/view3d.js`;
  - `tools/mapengine/web/vendor/three.module.min.js` (three r160 / 0.160.0);
  - `tools/mapengine/web/vendor/PointerLockControls.js` (examples/jsm/controls at 0.160.0, with its import path rewritten to `./three.module.min.js`);
  - `tools/mapengine/web/vendor/README.md`, recording the source URLs, version and SHA-256.
- Modify: `index.html` (import map), `app.js` (Tab handling).

**Interfaces:**
- Consumes: `state`, `onFieldChange`, `commit` and `TOOLS` from Task 6.
- Produces: `View3D`:
  - `mount(el)`;
  - `setField(field, design)`;
  - `show()` and `hide()`;
  - `groundAt(clientX, clientY) -> [x, z] | null` (raycast).
- **Behaviour:**
  - **Tab flip:** Tab toggles `state.view`. The same tool bar and panel stay visible in both views. The 3D camera starts above the map view's centre, looking at the same spot, and flipping back keeps the map view's pan and zoom.
  - **Rendering:**
    - one `InstancedMesh` of unit boxes, one per column;
    - each instance is scaled in y from its lowest 4-neighbour height to its own height;
    - colour comes from the palette's surface colour, with road and bed colours where set;
    - water is a separate translucent plane per water cell group, at the water level.
  - **Movement:**
    - fly mode by default: WASD moves, Space and Shift go up and down, and right-drag looks around;
    - the Walk toggle (F) uses PointerLockControls with gravity at ground height + 1.62 and a 1-block step-up.
  - **Points and spawns** are tall coloured pillars with labels.
  - **Tools in 3D:** pointer events go through `groundAt` into the same `TOOLS` handlers, so placing, selecting and dragging work exactly as in the map view.
  - **Updates:** on a field change, the instance matrices are rebuilt in place, with no new mesh. Target: under 150 ms for 700×500.

- [ ] **Step 1: Verify in the browser pane.**
  1. Load `maps/sample`.
  2. Press Tab and confirm the canvas shows 3D. Use `javascript_tool` to read the instance count, which should be width × depth.
  3. Drag point B in 3D, then press Tab and confirm B moved in the map view, with the same x and z as in `GET /api/design`.
  4. Raise a hill in the panel and confirm the 3D view updates. Time the rebuild with `performance.now()` around `setField`: under 150 ms.
  5. Check that the console has no errors.
  6. Take screenshots of both views.

- [ ] **Step 2: Fix until Step 1 holds.**

- [ ] **Step 3: Run the full fast verify.** Run `python tools/verify.py fast --offline`. Expected: PASS, including the 5 mapengine test files.

- [ ] **Step 4: Commit.** "Add the map engine's 3D walk view with Tab flip"

- [ ] **Step 5: Write a short how-to,** `docs/MAP-ENGINE.md`, for Kort, under 300 words: how to start the app, the tools, the keys, Build and Check, and where the output goes. Then commit.
