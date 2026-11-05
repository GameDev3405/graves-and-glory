# Painted title key art (board 4F.3)

Kort approved `key-art.png` on 2026-11-03. It is 2560x1440, stormy dusk: Admiral Vael (Tidecallers, from the left) locks weapons with the Hollow King (Graveborn, from the right). Each figure is the in-game model and skin, rendered and then lit. No brush filter is used; Kort rejected the procedural brush-filter drafts.

`key-art-menu-mock.png` is a mock of the art behind the title UI from `design-archive/ui-concepts/title/TITLE.md`: the lockup, the tagline, the menu column, the marker and the version. The art is not wired into the game.

Rebuild from `src/` (Python with numpy, scipy, opencv-python and Pillow):

    python key_art.py ../key-art.png            # about 4 minutes; add --scale 0.5 for a quick preview
    python mock_title.py ../key-art.png ../key-art-menu-mock.png

What each file does:
- `troop_render.py` is a numpy software renderer for `models/troop/*.json`: bones, boxes, skins, team cloth, weapons and the glow sheet.
- `pose.json` holds the heroes' clash pose. `pose_solve2.py` solved it; `pose_probe.py` holds the base poses and `pose_view.py` checks a pose from three views.
- `key_art.py` sets the camera, the crowds, the lighting (backlit sun, fills, rim, AO, spark-burst light and depth haze) and the composite. The sky, plain, dust, rain, embers and grade come from `paint_title.py`.
