# UI plan Task 1 report: art exporter and UI kit

**Status: DONE_WITH_CONCERNS.** The concerns are small, mostly about art, and none blocks a later task.

## Commit

- `2352586` UI Task 1: art exporter, atlas and UI kit (UiCanvas, UiTex, UiButton, UiMath)
- It is on branch `ui-plan` in worktree `C:\Users\kortt\Melty\bannerbound-ui`, with parent `a119e5c`. Not pushed or merged. 210 files: 5 modified, the rest new. The message file is `bannerbound-ui/work/p1/ui-task-1-commit.txt` (git-ignored).
- The working tree is clean after the commit. Preflight's CRLF rewrite of `Spec.java` and `sheets/preflight.json` only changed line endings, so I restored both with `git checkout --` and committed neither.

## What was built

- **Exporter**:
  - `tools/build_ui_art.py` refuses any Python but 3.12. It re-executes with `PYTHONHASHSEED=0` and writes no bytecode.
  - A bare run paints every manifest entry into `textures/gui/<folder>/<name>.png` plus `atlas.json`, and deletes stale PNGs.
  - `--verify` repaints into a temp dir. It exits 1, naming the first problem, on any of: an RGBA difference, a PNG missing from or extra to the atlas, an atlas entry that differs, or a texture name used in Java (the brief's regex over `src/main/java` and `test-tools/java`) that the atlas lacks.
  - `--import DIR` copies the 12 painters.
- **Painter copies**: `tools/ui_art/{v4lib,v4pal,v4a_table,v4a_paper,v4a_hud,v4a_recruit,v4a_ink,v4a_map,v4a_furn,v4a_portrait,v4a_recruit2,v4art}.py`. Each is byte-identical to `work/ui-concepts` once CRLF becomes LF (checked again just before the commit). `v4a_map.py` is the v4.2 painter from 10:32.
- **Manifest and extras**:
  - `tools/ui_art/manifest.py` holds the pinned export list: 182 entries (hud 75, table 80, recruit 27), checked name by name against the brief.
  - `tools/ui_art/kit_extra.py` paints the textures no mock drew:
    - glyphs `g-move/charge/retreat/wedge/loose` and their `-faded` versions;
    - `seal-neutral`, `hp-full-dusk`, `fgv-moon` and `btn-iron-240x40`.
- **Generated assets**: 182 PNGs under `textures/gui/{hud,table,recruit}` and `atlas.json` (schema as in the brief, `"python":"3.12"`). `font/focal.json` is `title.json` with size 16.
- **Java**:
  - `UiMath`: pure, the five functions.
  - `UiCanvas`: `k`, `width`, `height`, `begin`, `end`, `toCanvas`, `guiBox`, `scissor`, `endScissor`.
  - `UiTex`: `Entry` record; `entry`, `has`, `draw`, `frame`, `nine`, `three`, `part`, `bar`. The atlas is read by the `gravesandglory:ui_atlas` `SimpleSynchronousResourceReloadListener`, registered in `GravesAndGloryClient.onInitializeClient`.
  - `UiButton`, with `Painter`, `canvasBox` and `relayout`.
  - `DarkUi`: all 23 tokens. I checked their values against the brief by script, with `PAPER` and `GOLD` kept as aliases. Also `focal`, `heading`, `teamColor` and `teamMark` (sun, crescent, hollow diamond).
- **Harness** (all in `test-tools/`; `UiMathCheck` and `UiKitSheet` are added to `release_config.forbidden`):
  - `test-tools/standalone/UiMathCheck.java`.
  - `UiKitSheet`, a Screen.
  - In `Phase1Checks`: suite `ui-art` and `window(c,w,h,gui)`. The helper sets the window size, sets the GUI Scale option, calls `onResolutionChanged`, then holds the suite for 5 client ticks through a new `hold` counter in the runner.
- `.gitignore` gains `tools/ui_art/v4/`.

## Checks

| Check | Result |
| --- | --- |
| `UiMathCheck` | Red first: it failed to compile before `UiMath` existed. Then `UiMathCheck: 8 checks passed`. |
| `python tools/build_ui_art.py` | `182 textures painted` |
| `python tools/build_ui_art.py --verify` | `build_ui_art: 182 textures verified`, exit 0. 182 is the manifest length. |
| `compileJava compileLocalTestTools --offline` | OK. The only warning is the pre-existing one on the `ModelAngleCompatibilityMixin` target. |
| `python tools/preflight.py` | `Preflight clean: 1261 filled cells` |

### Suite `ui-art`

- **Root**: `C:/Users/kortt/Melty/bannerbound/work/p1/ui-art-124241`, launched at 12:42. Before launch, the Kort-not-playing check found no java or javaw process at all, so no Minecraft window was open.
- **`evidence/local-checks.txt`**: `pass p1-ui-atlas`, `pass p1-ui-canvas`, `pass p1-ui-kit-sheet`, `pass p1-ui-art-suite`. Also `pass mod-loads`, `pass client-compatibility` and `pass arena-ready`.

The log lines:

- `p1-ui-atlas`: "182 of 182 atlas.json entries open through the resource manager at their listed size and read alike by UiTex; hud/card-sel 181x68 at 1, 1 (181x68 at 1, 1 wanted); no pt- names and no PNG outside the atlas".
- `p1-ui-canvas`, one reading per window:

  | Window | Framebuffer | GUI Scale | k | Canvas | 100 GUI px |
  | --- | --- | --- | --- | --- | --- |
  | 960x540 GUI 1 | 960x540 | 1 | 1 | 960x540 | 100 canvas px |
  | 1280x720 GUI 3 | 1280x720 | option 3, effective 3 | 1 | 1280x720 | 300 canvas px |
  | 1280x720 GUI Auto | 1280x720 | option 0, effective 3 | 1 | 1280x720 | 300 canvas px |

- `p1-ui-kit-sheet`:
  - Layout: "182 atlas textures on 7 pages of 960x540 at GUI 1". That is 171 drawn once; 7 nine and 4 three slices drawn twice, at native size and at 1.5x; and 7 bar cuts of lengths [1, 68, 102, 27, 54, 27, 54].
  - Draws: every texture was placed, and all 200 of the 200 draws were made.
  - Pixels: "1747157 opaque pixels of the native-size draws and bar cuts compared with their PNGs, 0 wrong; 0 missing-texture magenta pixels; 7 screenshots".

Screenshots are in `run/screenshots/`. I opened each one:

- `p1-ui-kit-sheet.png`: the hud page. It shows:
  - the ledger cards: plain, the red-framed flash, brass-framed selected, grey rout, plus the 62- and 48-tall versions;
  - the iron spine at native size and 1.5x;
  - the score card, feed slip, lesson card, notice slip, respawn card and the seals;
  - the order-glyph row with its faded versions, and the feed glyphs;
  - the health channels and score tracks with their cuts;
  - then the field card, the ornate brass frame and order-hi.
- `p1-ui-kit-sheet-2.png`: table pieces. The dark 160 frame back, the two order sheets, the soft prop glow, the Rivergate map with its wax seals, the map roll, the ink skirmish and practice marks, the oak rail at native width and at 1.5x (centred, running off both edges), the castle mark, inkwell, candle, the small brass frame rim and the four plaques at 1.5x. (Corrected in fix round 1: this note first put the native plaques and the quill here.)
- `p1-ui-kit-sheet-3.png`: brass and iron 240x40 buttons, the 48 frame backs, the three vellum buttons at 1.5x and at native size, the crown wax, the four plaques at native size, the teal ribbon, the map seal, the seven icons, the 36 engraved brass key outlines (13 to 48 wide), the wax badge, quill, invite plus, ready check and tack. (Corrected in fix round 1: this note first put the 1.5x plaques here.)
- `p1-ui-kit-sheet-4.png`: recruit pieces. The large vellum letter, the muster roll on its rods, the bundle of letters under red wax, and eight house banners: the five `-on` versions, then gilded, graveborn and wildmoot.
- `p1-ui-kit-sheet-5.png`: the ironclad and tidecallers banners, the pale row-mark with its brass tab, the star and broken wax seals, the pouch, the sparkle, stars and padlock, the banner rod at native size and 1.5x, the die, the coin, and the 208 px rule.
- `p1-ui-kit-sheet-6.png`: the middle 960x540 of `oak-table`, nailed oak planks shown at 1 px per art pixel, darker toward the right edge (see concern 1, fixed in fix round 1).
- `p1-ui-kit-sheet-7.png`: the same view of `oak-table-recruit`, with a different joint layout from its own avoid box.

I also checked by eye, on 4x to 8x enlargements of the screenshots:

- Every bar cut ends in its dark edge. The health cut of length 1 is a single dark teal column at x 237. The 68 cut ends in a dark column at x 416 with the channel showing after it. Dawn's track cuts end dark at the right, and Dusk's start dark at the left.
- The 1.5x plaques keep their screws only in the corners, with continuous brass edges. The first enlargement was of the native plaques on sheet 3; fix round 1 checked the 1.5x ones on sheet 2, and they look the same.
- The 1.5x vellum buttons show tiled middles with no stretched pixels.
- Everything is crisp.

## Deviations, and why

1. **Title export left out** by the orchestrator's ruling 1. There is no `tools/ui_art/title/`, no title scripts, and no `title/*` PNGs or atlas entries.
2. **Slice insets are stored from the PNG edges.**
   - The values are plaques `l6 t6 r7 b8` and vellum buttons `l4 t4 r5 b6`, against the brief's flat "insets 6" and "insets 4".
   - The brief's values are measured from the plate. These PNGs carry a 1 px shadow on the right and 2 px at the bottom, so r and b include it.
   - A preview of the literal values, measured from the PNG edge, showed screw slivers and a ruled line through the middle of the 1.5x slices. The spine, key chip, rail and rod use the brief's values unchanged.
3. **`UiTex.blit` turns blending on** (`RenderSystem.enableBlend(); defaultBlendFunc()`) before each `drawTexture`.
   - `javap` shows that `DrawContext.drawTexturedQuad` leaves blending as it finds it, while fills and text switch it off after they draw.
   - Without this, the art's translucent shadows would draw as solid dark blocks after any `fill` or text, and the kit sheet starts with a `fill`.
   - It does not change the brief's draw call.
4. `build_ui_art.py` re-executes whenever `PYTHONHASHSEED` is not `0`, not only when it is unset. It is stricter, for the same determinism.
5. The painter copies are stored with LF line endings, because this worktree's `.gitattributes` makes `*.py` LF. Their content is otherwise byte-identical to the sources, and `--import` does the same normalising.
6. **The kit sheet needs 7 pages**, not 1 or 2: 200 draws, two of which are the 1280x720 tables, each centred on a page of its own. So the screenshots are `ui-kit-sheet` and `ui-kit-sheet-2` to `-7`.
7. **Step 6 red took a different form.**
   - `build_ui_art.py --verify` failed with `ModuleNotFoundError: No module named 'manifest'`, because the script had been written before the manifest, not "script missing".
   - The in-game "`p1-ui-atlas` fails with no atlas" run was not done. The suite cannot compile before the kit classes exist, so the red there was the compile failure.
   - From the code, not from a run: on a missing atlas `p1-ui-atlas` reports "no gravesandglory:textures/gui/atlas.json" and fails.
8. **The suite checks more than the brief asks.**
   - `p1-ui-atlas` also checks that `UiTex` parsed every field of every entry as in the JSON, and that no PNG under `textures/gui` is missing from the atlas.
   - `p1-ui-kit-sheet` also compares every opaque pixel of the native-size draws and bar cuts with the PNGs, and counts magenta (`0xf800f8`) pixels.
   - The 1.5x slices are judged by eye, as the brief says.
   - `UiKitSheet.shownPage` lets the suite shoot a page only after a frame has drawn it.
9. **Meanings later tasks must follow:**
   - `nine(w,h)` sizes are PNG extents, shadow included: the atlas `w,h` reproduce the PNG.
   - All draw positions are box origins: the PNG goes at `x-ox, y-oy`. `part` has no offset.
   - `bar`'s `(x,y)` is the full bar's box. A cut from the right sits flush with the full bar's right end.

## Concerns

1. **The oak tables are lit off-centre.** `v4art.oak_table` hard-codes its scratch centre and its vignette at (480, 300), with radii 600 and 380. Those fit its default 960x540 table, but the brief's `W=1280,H=720` calls do not move them. On the exports the light sits about 160 px left of and 60 px above the middle (640, 360), and the right side of `ui-kit-sheet-6` and `-7` reads darker. I kept the brief's call. Correction from the review: the fix belongs in the copy in `tools/ui_art`, where Global Constraints line 30 says painters are edited after Task 1, not in `work/ui-concepts`. Fixed in fix round 1.
2. **Some textures have offset 0 even though their art is inset in the PNG.** This follows the brief's listing. `hud/notice-slip`'s paper starts at (1, 1). The four seals are 27x27 PNGs with the plate at (4, 4). Later tasks must place them by the PNG origin, or the atlas offsets should change.
3. **Line endings.** This worktree's `.gitattributes` gives LF working copies for `*.java`, `*.py` and `*.json`, against the "working copy CRLF" rule. I kept each file's existing endings: Java, Python and JSON LF; `.gitignore` CRLF.
4. **Preflight dirties two files.** `tools/preflight.py`, run by every `compileJava`, rewrites `Spec.java` and `sheets/preflight.json` with CRLF. The content is identical, but git shows them modified until they are checked out again.
5. **Two launch rules disagree.** The plan's Global Constraints (line 21) say to stop if a Minecraft window is open, while the orchestrator said to wait. It did not matter here: no java process was running at launch.

## Fix round 1

**Status: DONE.** Review: `task-1-review.md`. Work is in the same worktree and on the same branch.

### Commit

- `598e57f` UI Task 1 fix round 1: neutral mark is a square, oak tables lit from their middle, safer export, reload, verify and hit box.
- Its parent is `2352586`. Not pushed. The message file is `bannerbound-ui/work/p1/ui-task-1-fix1-commit.txt`.

### What changed

1. **`DarkUi.teamMark`** draws a hollow 7x7 square, not a diamond, for anyone but Dawn (sun) and Dusk (crescent). This follows spec 6.1: the diamond is Dusk's, the bare square is neutral.
2. **Oak tables.**
   - In `tools/ui_art/v4art.py`, `oak_table` fixed its light and its scratch centre at (480, 300) at the old lines 205 and 223. That is the mock's spot on its 960x540 table, 30 px below the middle.
   - Both now use `lx, ly = 480 + (W-960)/2, 300 + (H-540)/2`. That keeps the mock's spot relative to the middle of a larger table, so the 1280x720 exports are lit from (640, 390).
   - The mock lit both tables from that same spot, not from an avoid box, so the recruit table gets it too.
   - The 960x540 table paints the same pixels as before: same sha1 from the HEAD and the fixed painter.
   - Re-exporting changed only `table/oak-table.png` and `table/oak-table-recruit.png`, and `--verify` passes. The `tools/ui_art` copy of `v4art.py` now differs from `work/ui-concepts`, so a later `--import` would undo this.
   - Mean light of the left, centre and right thirds of sheets 6 and 7:

     | Run | Sheet 6 (L / C / R) | Sheet 7 (L / C / R) |
     | --- | --- | --- |
     | Before (`ui-art-124241`) | 34.0 / 33.8 / 26.1 | 34.1 / 33.8 / 25.9 |
     | After (`ui-fix1-132034`) | 31.1 / 34.8 / 31.1 | 31.2 / 34.8 / 30.9 |

3. **`build_ui_art.py` export** now removes only PNGs that the previous `atlas.json` listed and the manifest no longer paints.
   - A PNG that no atlas listed is kept. The export names it, and `--verify` fails until it goes. An unreadable previous atlas removes nothing.
   - Checked on a copy of the gui folder:
     - A listed-then-dropped PNG was removed.
     - A stray PNG was kept and failed `--verify` ("hud/zz-stray.png is not in atlas.json").
     - With an unreadable previous atlas, both PNGs were kept.
4. **`UiTex.load`** no longer empties the map when a reload finds no atlas, or finds one it cannot read. It logs the problem and keeps the textures it read before.
   - When nothing has been read yet, the log line says that `UiTex.entry` throws for every name, and no UI texture draws, until a reload finds the atlas.
   - `load` now takes a `ResourceFactory`, and there is a package-private `UiTex.count()`.
5. **`--verify` texture-name rule**: `UiTex\.\w+\(\s*(?:[\w.]+\s*,\s*)?"([^"\\\n]+)"`.
   - It also matches one-argument `entry("...")` and `has("...")` calls, a name on the line after the `(`, and any string literal, not only `[a-z0-9/-]`. Names held in variables are still not checked.
   - Tested on 11 sample calls: all right. A wrapped `UiTex.has(\n "hud/nope")` failed `--verify` as it should.
   - It now finds the `"hud/card-sel"` literals in `Phase1Checks`.
6. **`UiButton` hit box.**
   - `isMouseOver` and `clicked` test the canvas box itself, through `UiCanvas.toCanvas` of the double mouse position.
   - `renderButton` sets `hovered` the same way, from the exact mouse position, so hover and the tooltip match too.
   - `guiBox` still sets the widget bounds, which only focus and narration use.
   - There is no in-game check for this. Button coverage is left to the task that first uses a button.
7. **`kit_extra.py`** comments name the `v4a_hud.py` lines that the extras mirror:
   - `seal-neutral` mirrors `seal('B')`, lines 396 to 469. It keeps the plate, shadow, `iron_shade` and bead ring, and leaves out the swords, split, glint and coloured field.
   - `hp-full-dusk` mirrors `hp_fill`, lines 561 to 575.
8. **Report**: the plaque notes for sheets 2 and 3, the 1.5x-plaque check and concern 1 are corrected above.
9. **New check `p1-ui-atlas-reload`** in suite `ui-art`, added as evidence for item 4.
   - It reloads `UiTex` from a factory with no atlas, then from a cut-off `atlas.json`, and checks that the same 182 entries are still held. Then it reads the real atlas again.

### Checks

| Check | Result |
| --- | --- |
| `UiMathCheck` | 8 checks passed |
| `python tools/build_ui_art.py` | 182 textures painted; only the two oak tables changed |
| `python tools/build_ui_art.py --verify` | `build_ui_art: 182 textures verified`, exit 0 |
| `compileJava compileLocalTestTools --offline` | OK, the same one pre-existing mixin warning |
| `python tools/preflight.py` | clean |

### In-game run

- **Root**: `C:/Users/kortt/Melty/bannerbound/work/p1/ui-fix1-132034`, launched at 13:20. The Kort-not-playing check found no java or javaw process.
- **Results**: `pass p1-ui-atlas`, `pass p1-ui-atlas-reload`, `pass p1-ui-canvas`, `pass p1-ui-kit-sheet` and `pass p1-ui-art-suite`. The kit sheet compared 1,747,157 opaque pixels with the PNGs, with 0 wrong and 0 magenta.
- **Log lines** behind `p1-ui-atlas-reload`:
  - "The UI atlas gravesandglory:textures/gui/atlas.json is missing; keeping the 182 UI textures read before."
  - "... could not be read (com.google.gson.JsonSyntaxException: java.io.EOFException: End of input at line 1 column 22 path $.textures.hud/x); keeping the 182 UI textures read before."
- **Screenshots**: `run/screenshots/p1-ui-kit-sheet.png` and `-2` to `-7`. Sheets 1 to 5 are byte-identical to the first run.
  - `p1-ui-kit-sheet-6.png` and `p1-ui-kit-sheet-7.png` show the middle 960x540 of `oak-table` and `oak-table-recruit`. Each is now lit evenly left to right, with the brightest planks in the middle and the corners darker on both sides.
  - On sheet 2, the 1.5x plaques have screws only in their four corners, unbroken brass rims and a tiled wood middle with no stretched pixels.

### Still open (out of scope this round)

- `hp-channel-grey` is identical to `hp-channel`, because the mock uses an identity lambda. This is a note for Task 2.
- There are no in-game checks for `UiButton`, `scissor`, `frame` or `teamMark`. They go to the tasks that first use them.
