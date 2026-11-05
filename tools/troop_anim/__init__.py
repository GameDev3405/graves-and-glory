"""Authoring and reviewing troop clips the way the game plays them (docs/ANIMATING-TROOPS.md, tools/animate_troop.py).

model      a troop as TroopModel draws it: bones, boxes, visibility, skin and cloth, the held weapon and shield
pose       a clip on a troop as TroopMotion.applyForced poses it: stance + clip, the feet's ground, limits, carries
solve      the authoring solvers in game space: arm aims, natural grip, Euler continuity, PCHIP, leg IK, head look
clearance  how far the held items pass into the body, and the pushes that take them out
author     chained-pose timelines baked to tick keys, written into art/clips/<rig>/<family>.bbmodel
checks     the quality gates (feet, rotation per frame, jerk, clipping, loops, stance ends, sign and rate lints)
render     a software rasteriser for review sheets, strips, stills and House tiles
recipes    per-family presets and the authored clips

Game model space throughout: pixels, y down, -z forward, the troop's right at -x; rotations in degrees, applied ZYX
(roll, then yaw, then pitch, outermost first) and added to a bone's rest rotation channel by channel.
"""
