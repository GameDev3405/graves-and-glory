# Heraldic units: my grading rubric for unit art (Kort's 9/10 bar)

The project's UI rubric (work/ui-concepts/GRADING-RUBRIC.md) grades screens. This
grades a *unit*: a 36 px biped that has to read in a battle of 160, from 30 blocks,
in four yaws, in four team colours. Same bar, adapted.

Each line is 1 point; half points allowed. Below 9 is a fix round, not a pass.

1. **Silhouette.** At 36 px, in solid black, the troop is identifiable as *this*
   unit and not its line-mates. Headgear, shoulders, hem and weapon are all
   legible. Two troops that share a job never share a silhouette.
2. **Read at 36 px.** The unit's job is legible in the 36 px colour strip without
   a label: archer reads as an archer, shield troop as a shield troop, caster as a
   caster. Shape and value, not detail.
3. **Form and volume.** The body reads as a solid with light on it: a lit plane,
   a shadow side, a dark core, a bounce. No flat single-value fields on any
   material that faces the camera. Round forms get a rim, flat forms get an edge.
4. **Material separation.** Skin, mail, plate, cloth, leather, wood and metal are
   each distinguishable at 36 px, by value and texture, not only by hue.
5. **Light and shadow consistency.** One light direction, top-front-left, on every
   unit and every part. Ambient occlusion where forms meet: under the chin, in
   the armpit, where a skirt meets a leg, where an arm crosses the body, under
   the brow. The unit sits on the ground, not floats.
6. **Face and expression.** Where a face shows, it has a brow, eyes with a
   highlight, a nose shadow and a mouth. Faces carry the most contrast on the
   unit; a face is never a flat pink rectangle.
7. **Edge and material noise.** Edges are read with a darker line on the shadow
   side and a lighter line on the lit side. Cloth has weave or fold lines, mail
   has a ring pattern, leather has grain, wood has grain, metal has a gradient
   and a specular. No flat fills on any material over 4x4 texels.
8. **Heraldic identity.** The team cloth is the loudest read and it is a *device*,
   not a colour field: a recognisable charge, correctly proportioned, with the
   team's colour the loudest thing on the unit and the glyph carrying it for
   colour-blind players. The device is not noise and not a smudge.
9. **Hierarchy.** The eye lands on the face or the mark first, then the weapon,
   then the silhouette, then the cloth. Value contrast does the ordering; the
   brightest thing on the unit is the thing that matters.
10. **Unity with the game.** The unit belongs to Graves and Glory: grim but
    heroic, hand-authored pixel art, the house palette held, no generic fantasy
    filler, no vanilla mob left showing through, nothing that could be mistaken
    for a recolour of another troop.

**Grade sheet.** Every unit is graded on all ten lines, with the number and a
one-sentence reason per line, and the total. The average across the set is the
set's grade. A unit under 9 is a fix round.

**A 10/10 reference set** is kept in `work/p2b/hx_reference/` with a note on why
each is there.
