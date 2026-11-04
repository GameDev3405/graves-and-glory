# Tidecallers tier I worker brief (task 4B.2 part 1)

Checkout: C:/Users/kortt/Melty/bannerbound-tidecallers, branch art/tidecallers-t1. Do not commit, push, merge, launch
Minecraft or run tools/build_skins.py / verify.py (the coordinator does). Never use a Fable model.

## Reference (read only what you need)
- Style and pipeline: art/hand/tc_tideguard/build_tc_tideguard.py (Troop, bones, painters, cloth_painter, NOTE,
  house_stub.main). Import its ramps/painters instead of copying: `sys.path.insert(0, os.path.join(HERE, "..", "tc_tideguard"));
  import build_tc_tideguard as TG` (VERD there is now the sea-blue ramp; BRONZE, CORAL, FLESH, BARN, bronze_scales,
  barnacled_mail, plate_b, face painter). art/hand/common/handlib.py is the library.
- docs/ART-STYLE-GUIDE.md section 10.5 (Tidecallers: silhouettes, signature parts, palette, faces, portraits, weapons)
  and 10.1 (sea-blue ramp `#0A1426 #11264A #1A3F6A #2A5E8A #5E88A6 #9DB8C4`).
- Sister batch for a worked example: C:/Users/kortt/Melty/bannerbound-wildmoot/art/hand/wm_*/build_wm_*.py
  (wm_oldhorn and wm_hound use the hound rig via meta["rig"]="hound"; wm_seer uses weapon "none").

## Rules (Kort)
- 1 texel per model pixel (density 1). Tier I only (tiers=(1,)).
- No flags, pennants or standards on the back. Other back gear is fine.
- Accent is sea blue (the ramp above) on fixed paint: trim, wraps, plume, weapon grip, rims. Side cloth uses TEAM[side]
  like the Tideguard. The House hue check needs saturated sea-blue texels (sat >= 0.30) to stay a real share of the
  skin's saturated texels, so put visible sea blue on the skin, not only bronze/brown.
- Souls-like game art: weathered, layered, strong silhouette and dark-to-light read; not historical costume, not a
  flat Minecraft-mod look. Drowned, dripping: kelp, barnacles, bleached coral (coral is the Trident Guard's; others
  use shell, kelp, barnacle).

## Deliverable per troop <tid>
- art/hand/<tid>/build_<tid>.py (your only new source file), then `python art/hand/<tid>/build_<tid>.py stub`
  writes art/troops/<tid>.json and <tid>_s1.png.
- Visual check: `python art/hand/<tid>/build_<tid>.py contact <png> teal` (and one other side) into the scratchpad;
  look at it, fix the worst issues, at most two polish rounds. Also run
  `python work/tidecallers_turn_sheet.py teal tc_tideguard <tid>` and view work/tidecallers-t1-turn-4x-teal.png to
  compare with the Tideguard.
- Touch no other files unless your task says so.
- Report (under 120 words): files written, weight/look summary, self-score out of 10, known flaws, questions for Kort.
