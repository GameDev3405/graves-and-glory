# Independent review: revenant, reaper, hexer texture proposals (2026-11-09)

Reviewed from the three before/after sheets plus revenant/t2_near.png. Rubric: unit-texture-audit.md section 2. Scores are AFTER, 0-10.

## Revenant (Risen) - verdict FIX

1. Overall: better. The mottled olive/charcoal noise is calmer, plates and mail separate more, T2/T3 pauldrons now read as grey plate rather than black noise. At 16 blocks: only marginally better. It is still a dark blob; the red tabard and gold T3 shoulders carry the read exactly as before. Gain is about +1 on E, not the step the audit asked for.
2. Approved look kept: yes. Face, tabard, purple trim, brown T1 pauldrons, gold T3 shoulders all preserved.
3. Defects:
   - Helm crown and back, all tiers: now a blank, flat, noticeably lighter grey slab (reads as a lid or bucket from the back and 3/4). It is lighter than every other mass and has no mail, rivets or shading. It was noisy before, now it is empty and a new material mismatch. This is the blocking item.
   - Legs and boots, T1-T3 (strongest in the back view): boots and the lower legs went near black, and T3 back legs are a black block with two gold bits. Value jump from torso to legs is harsh and the legs lose form at 16 blocks.
   - T1 pauldrons are still brown while T2/T3 are grey, so T1 to T2 reads as a recolour of the shoulder, not a stacked upgrade.
   - Purple trim stripe is still repeated on helm, belt, boots at equal weight.
4. Scores: A 6, B 5, C 6, D 6, E 4, F 5. Mean (A+B+C+D+2E+F)/7 = 5.1 (before 4.3).
5. Fix: give the helm crown/back a light, mid and shadow ramp plus a few rivet or mail texels; lift the boot and lower-leg values off black (one band above the boot); consider one highlight edge on the T1 pauldron.

## Grave Reaper - verdict SHOW

1. Overall: better. The hood back checker patch is gone, replaced by a continuous smooth hood with a purple band, which fixes the audit's seam finding. The robe now has visible vertical fold ramps and a faint edge light. T2 spikes went from black to bone, T3 crown, pauldron edges and hem went gold, so the tier steps are real. At 16 blocks: modestly better. T2 bone spikes and T3 gold crown and shoulders read clearly; T1 is nearly unchanged and the robe is still very dark.
2. Approved look kept: yes. Hood shape, blue eyes, skull face, red sash, purple trim, bone legs all preserved. No restyle.
3. Defects (minor): T3 hood has a thin gold line near the crown base that is slightly noisy; hood back at T1/T2 is a very smooth near-black gradient with little interest (acceptable, it matches the front); T1 to T2 is still mostly grey pauldrons plus spikes at 16 blocks.
4. Scores: A 6.5, B 6, C 7, D 6.5, E 5, F 7. Mean = 6.1 (before 4.4).
5. SHOW. Optional follow-up: slightly lift the robe fold highlights for T1 at 16 blocks.

## Gravecaller (hexer) - verdict SHOW

1. Overall: better. The head and hood gained a purple-tinted dark ramp instead of black; skirt has a clearer band under the belt and bottom hem; T2 shoulder plates have a visible highlight edge; T3 gets a gold sash, gold belt and gold shoulder edges that make the tier unmistakable. Lantern, glow, red tabard and halo are untouched. At 16 blocks: better but small. T3 gold sash reads; T1 and T2 changes are subtle, the head is still a dark cube, and the lantern and glow still carry the read.
2. Approved look kept: yes. Silhouette, lantern, halo, tan face, red panel all preserved.
3. Defects (minor): hood back T1-T3 is a smooth flat slab with little detail; T3 hood back shows a blurred dark circular smudge inside the halo that looks like a soft gradient, off style for the hard-pixel look; hood top is flat; the T3 gold sash across the belt is the only gold on the lower body, so the gold distribution is a bit patchy.
4. Scores: A 6.5, B 6, C 6.5, D 6.5, E 5, F 6. Mean = 5.9 (before 4.7).
5. SHOW. Suggest cleaning the T3 hood-back smudge into a hard-edged shade before final.

## Summary

Reaper and Gravecaller show; Revenant needs the helm crown/back and boot values fixed first. None reaches the 8 threshold at 16 blocks; the gain at 16 blocks is real but modest on all three.

## Revenant re-review (revised proposal) - verdict SHOW

- Helm crown/back: fixed. It is now a dark charcoal with plate seams and a purple band, matching the body instead of a pale lid. Minor: it is now fairly flat and dark, so from behind it reads as a near-black block above the grey shoulders (T2/T3 most).
- Boots: fixed at T1/T2. Boots are a brown-grey with the purple band and no longer near black. Remaining: T3 back lower legs are still a near-black block with two gold bits (unchanged), a harsh value drop.
- New defects: none blocking.
- Scores: A 6.5, B 5.5, C 6, D 6, E 4.5, F 6. Mean = 5.6 (before 4.3).
- Caveat: 16-block gain is still modest; the figure remains dark with the red tabard and T3 gold doing the reading.
