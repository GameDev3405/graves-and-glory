# Heraldic units: the grade sheet

Graded against `HX-RUBRIC.md`, the ten lines, on the renders in
`work/p2b/hx_ab.png` (baseline against mine, four yaws, 36 px read strip) and
the close-ups. Every line is 1 point; half points allowed. Below 9 is a fix
round, not a pass.

Grade: **the set's average** across the six troops.

## Where the baseline stood

| Line | Baseline | Why |
|---|---|---|
| 1 Silhouette | 5.0 | Six bipeds sharing one body plan; only the headgear differed |
| 2 Read at 36 px | 5.0 | The gold pale was the loudest thing on every unit and said nothing |
| 3 Form and volume | 3.5 | Flat fills: one value of cream for a torso, one for a leg |
| 4 Material separation | 4.0 | Mail, cloth and plate were all pale greys |
| 5 Light and shadow | 3.0 | No occlusion anywhere: armpits, under the chin, skirt over leg |
| 6 Face and expression | 4.5 | A face existed, but flat, with no brow or nose shadow |
| 7 Edge and material noise | 4.0 | Edges had no line; big fields had no texture |
| 8 Heraldic identity | 2.0 | A flat field with a belt. No device at all |
| 9 Hierarchy | 4.0 | The pale shouted; the face and the mark did not |
| 10 Unity with the game | 6.0 | The house palette was held; the craft was thin |
| **Baseline average** | **4.1** | |

## The heraldic version

| Line | Grade | Why |
|---|---|---|
| 1 Silhouette | **8.5** | Iron cap, pointed hood, flat great helm, peaked cap, deep cowl, laurel helm. The pennant or the standard and the weapon carry the rest. On top of that the three armoured troops now wear real shoulder pauldrons, new geometry that caps the shoulder and overhangs outward, so an armoured troop is wider across the shoulders than a cloth one. Not 9: the six still share one biped body plan, so the torso and legs are the same shape on all of them. |
| 2 Read at 36 px | **8.5** | The device is on the chest and the weapon is in the hand, so the job reads at a glance where the baseline needed a label, and the pauldrons separate the armoured from the cloth at the same size. Not 9: at 36 px the pale and the mail are close in value on the archer, and the mage and the squire still share a pale rectangle. |
| 3 Form and volume | **9.0** | A weighted cylinder falloff gives every limb a lit edge, a field and a shadow; the surcoat has a crease and the skirt has folds. Measured, not guessed: the arm goes S0 -> S1 -> S2 -> S3 across four texels. |
| 4 Material separation | **9.0** | Mail is a dark mottle, cloth a pale fold, plate a specular gradient, leather a warm grain, gilt a lit rim, wood a grain. The Templar's plate and his tabard's cloth are two different things at a glance. |
| 5 Light and shadow | **9.0** | Occlusion at the collar, the armpit, the hem, under the helm brim and where the skirt falls over the leg. One light direction, top front left, matching the renderer. The only miss is under the chin, which the head box does not give room for. |
| 6 Face and expression | **8.5** | Brow, lit sclera, dark pupil, nose shadow, mouth and a jaw; the hood and the cowl are cut open again so the faces show in shadow. Not 9: at 8x8 the eye is two texels and the expression is a mood, not a look. |
| 7 Edge and material noise | **8.5** | Every material carries texture and every edge a line: dark on the shadow side, light on the lit side. The pauldron's gilt rim is a lit roll, so the new geometry carries the same edge rule as the rest. Not 9: the mail mottle is deliberately near-flat, so at a close-up it is a little empty where the baseline's checker had (false) texture. |
| 8 Heraldic identity | **9.5** | A real device: the side's glyph stamped in white over a graded field, on the pale, the jerkin and the vestment, plus the pennant and the standard. The Templar keeps his own white cross, which is what keeps him apart from the Squire. The device reads at 36 px and the glyph carries the side for a colour-blind player. |
| 9 Hierarchy | **9.0** | The face and the mark are the brightest things; the mail sits a value below the cloth; the gilt is an accent, never a field. The eye lands on the sun on the chest, then the face, then the weapon. |
| 10 Unity with the game | **9.0** | The house palette is held exactly (enamel, steel, gilt, sun-white), the team colour never touches a skin, the copy of the spec is followed, and the units belong to this game rather than to a generic fantasy. |
| **Average** | **8.9** | |

## The honest read

The set averages **8.9**. By the project's own rule that is still a fix round,
not a pass, and I am not going to dress it up.

**What is genuinely better than the baseline:** volume, material, light, the
heraldic device and the weapons. Those are not opinions; they are measured in
the pixels and they are visible in the A/B sheet. The baseline scored 4.1 on
the same rubric, so the craft roughly doubles.

**What is still short, and why:**

1. **Silhouette (8.5) is the real ceiling.** All six troops are the same biped
   skeleton. The headgear differentiates them, the pauldrons widen the armoured
   ones, and the pennant or the standard finishes the job, but the torso and
   legs are still one shape across the whole House. The fix is more geometry:
   a proper shield on the arm instead of the vanilla item, a cloak on the
   Marshal, a wider stance for the Templar. That is a modelling job, not a
   painting job, and it is where the next 9+ lives.
2. **The face is an 8x8 mood, not an expression.** Two texels of eye cannot
   carry more than that at this scale.
3. **Read at 36 px (8.5) still leans on the device.** The pale and the mail sit
   close in value on the archer, so the mark is doing more work than the shape
   on that one troop.

**Fixed since the first grade, and worth saying out loud:**

- **The archer's fletching was a regression and is now fixed.** The spec makes
  the fletching over the shoulder the archer's mark. My first repaint of the
  quiver used a dim bone tone and it vanished against the linen; the baseline
  had it in bright sun-white. The fletching is now sun-white again and reads
  over the shoulder from behind, better than the baseline's.
- **The pauldrons are new geometry, not a repaint.** `art/parts/pauldron.json`:
  a cap over the top of the arm and a rolled rim along its top edge, hung from
  the arm bone so it moves with the shoulder, in the troop's own metal (white
  plate on the Templar, blackened on the Marshal) with a gilt rim. The
  left side is the same part on the left arm with an x offset, because the
  build refuses a negative scale.

## The grade that is real

The offline renderer is the project's own (`work/p2b/preview.py`), and it
mirrors ModelPart.Cuboid exactly, so these renders are the models the game
draws. What is **not** done: the in-game harness render. The project's rule is
that nothing is called done until a running game shows it by screenshot, and
that needs a game launch, which needs Kort's go. So the 8.75 is a grade on the
renders, and the in-game grade is unclaimed until it is run.
