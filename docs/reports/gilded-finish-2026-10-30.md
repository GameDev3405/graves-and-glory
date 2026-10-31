# Gilded finish: sun staff and bows (2026-10-30)

Contact sheet: docs/reports/gilded-finish-2026-10-30.png

## Outcome
Sun staff I–III are hand-authored and installed. Each tier has 2 lit texels and 87/99/121 quads, and keeps the old grip and thickness 2. The sunweaver, longbowman, archer and all bow tiers were checked in game: front, side and back, normal size and card size.

Round 1 found a defect: every sun clipped the cowl and face. It is fixed, and round 2 confirmed it clean. The bows had no concrete defects.

## Commits
7bfe24e (staff installed), 8e1eacc (suns raised), 595cd16 (report).

## Changed files
- art/hand/weapons/build_sun_staff.py
- art/weapons/sun_staff{,_t2,_t3}.*
- models/troop/weapon/sun_staff*.json
- textures/entity/troop/weapon/sun_staff*

## Checks
- build_skins --verify: PASS
- 160 unittests: PASS
- Client gf-1 (stage 3) and gf-2 (stage 1), suites hands/turn/t3: all PASS
- verify fast: DEFERRED, because the bannerbound-bisect client is running and game_guard waits for it.
- Spec.java: ff45ae12 before. runClient rewrote it with CRLF only (d25ef5d1); I restored it to ff45ae12.

## Evidence
work/art-units/gf-1 and gf-2: run/screenshots and log.txt (gitignored).

## Unresolved, for Task 16b (anim lane)
- In the cast pose (hands-mage-t3-cast-0) the staff hangs upside down, with the sun at the belly.
- At idle the shaft passes inside the right_bell sleeve.
- The cowl reads boxy from the side.
- At front card size a bow hanging at idle is nearly invisible, edge-on.

## Assumptions
- I lengthened the staves to 35/40/39 px to clear the cowl.
- I dropped tier III's cross-bar and hang one chain on the outer side, which keeps the staff 2 px deep.
- Card size comes from downscaled shots.
- The GRAVES_PHASE2_WEAPON override picks the tier.

## Status
- Implemented: yes.
- Tested: yes, except verify fast, which is deferred.
- Visually inspected: yes.
