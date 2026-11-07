> Historical (superseded 2026-11-07): see PLAN-TO-1.0.md / WORKFLOW.md

# DRAFT: lobby and map screen looks (War Table), for Kort to pick

Status: DRAFT, 2026-11-01. Nothing here is built. Each look must still pass the 9 of 10 bar in `docs/UI-GATE.md` and `work/ui-concepts/GRADING-RUBRIC.md`.

Fixed by existing standards, all looks keep: the approved War Table object (oak table, brass, parchment) and Iron Ledger; the 960x540 page centred under a full-width rail on a 1280x720 table; everything fits at 854x480; ink contrast at least 4.5:1 on slips (Dawn and Dusk team inks as in the gate); no pure black or white; Graveborn violet and Tidecallers sea blue as House accents; 1 texel per pixel. Palette anchors from `docs/ART-STYLE-GUIDE.md`: wood `#6E5440`, stained wood `#563C2E`, gilt `#96783A`, steel `#7C8591`, linen `#BFB59A`, leather `#5A3E2C`.

Code touchpoints common to all: palette in `tools/ui_art/v4pal.py`, table and furniture in `v4a_table.py` and `v4a_furn.py`, parchment in `v4a_paper.py`, map frame in `v4a_map.py`; rebuilt with `tools/build_ui_art.py`; textures swapped under the mod's gui assets. No Java layout change unless noted.

## Look 1: Lantern Table (closest to approved)
- Palette: stained wood `#563C2E` table, wood `#6E5440` rail, gilt `#96783A` trim, linen `#BFB59A` parchment, one warm candle glow.
- Materials: oak planks, brass corner caps, parchment, wax seal for Ready.
- Frame: map in a brass-cornered oak frame; seats as carved name plates.
- Mood: a campaign tent at night, candle lit.
- Code: palette tweak plus candle glow overlay in `v4a_table.py`; no Java.

## Look 2: Iron Map Room
- Palette: steel `#7C8591` and iron tones 1 to 3 for the frame, leather `#5A3E2C` map mat, linen map, gilt only on the Ready plate.
- Materials: riveted iron plate, leather straps, pinned vellum.
- Frame: plate with rivets and strap corners; seats as iron tags on a rail.
- Mood: a siege command post, colder and harder; matches the Iron Ledger HUD so lobby and battle read as one object.
- Code: new frame and tag textures in `v4a_map.py` and `v4a_furn.py`; small Java change (map and seat panels use a new nine-slice id).

## Look 3: Banner Hall
- Palette: linen `#BFB59A` ground, Dawn and Dusk inks as two hanging cloth strips, wood `#6E5440` posts, gilt `#96783A` finials.
- Materials: cloth, wood posts, painted map scroll.
- Frame: the map is a scroll hung between two banner poles; each team's seats hang under its strip.
- Mood: a muster hall before the march, ceremonial and bright.
- Code: largest. Team columns re-skinned in `v4a_paper.py`; Java: seat layout needs the two strips and a 3v3 spacing check (plan item 3.4); the 854x480 compact layout needs a re-pass.

Suggestion: Look 1 for lowest risk to the 9 of 10 score; Look 2 if you want lobby and battle to match; Look 3 only if you accept a new gate run.
