# Editing troop animations

You edit in Blockbench; the game uses exactly what you save, after one command.

1. **Open.** `python tools/anim_edit.py open sword_shield` (families: `sword_shield` on the knight, `bow` on the archer, `common` for hits, deaths and scenes; `--troop <name>` picks another troop; `--launch` starts Blockbench). It writes a file under `work/anim-edit/` with the troop's real model and skins plus the clips. Open that file. Running `open` again keeps your unapplied edits; `--fresh` throws them away.
2. **Scrub.** Switch to Animate mode, pick a clip on the left, and drag the playhead (or press play). The timeline snaps to 20 per second, which is one game tick.
3. **Edit.** Move keyframes, change their numbers, or switch a key's interpolation (linear, smooth, bezier, step). Rotation is degrees and position is pixels, added to the bone's rest pose. Markers are lines in the Effects row (`impact`, for example). An `impact` marker must sit exactly on a tick (a multiple of 0.05 s) and, for attacks, on the tick the balance sheet names. Do not rename clips or bones, and do not move or resize cubes.
4. **Save** with Ctrl+S, over the same file.
5. **Apply.** `python tools/anim_edit.py apply` copies your clips into `art/clips/biped/`, rebuilds the game's clip files and verifies them. It stops with a message if a check fails (impact off a tick, attack too long, death not on the ground, loop not closing). Then commit.
6. **Preview.** `python tools/anim_edit.py preview sword_shield sword_slash` prints the command that renders the clip in the real game. It has no single-clip filter, so find your clip in the solo sheet.

## Limits

- Blockbench shows the troop's body, but not the weapon, the shield board or the scale and hunch the game adds, so a pose can look different in game.
- The game then adds layers on top: walking, running and turning, the idle breath and fidgets, hit reactions, head look, foot planting, cloth and strap springs, and the blends between stances. The shield and bow carries are added too. Judge the final look in game.
- `python art/clips/build_clips.py` is the old generator. Do not run it any more; it would overwrite your edits.
- Do not edit the committed `art/clips/biped/*.bbmodel` files directly in Blockbench (they hold only the bare rig); always use `open`.
- A real in-game scrub screen is not built.
