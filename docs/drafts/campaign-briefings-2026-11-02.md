# Campaign briefings and reasons, battles 2 to 6 (DRAFT for Kort's approval)

Task 4D.2. Nothing here is in `sheets/design.json`; copy needs Kort's approval first.

## Limits found

- No hard character limit in code or `tools/preflight.py`. `CampScreen` wraps the briefing with `drawTextWrapped` at the page width (about 210 px at 1x scale to the Start hint at y 239, so roughly 10 lines). Reason has no UI draw yet; it is only sent to the client in `Campaign.java`.
- Self-imposed bounds, matched to battle 1 (briefing 108 chars, reason 52): briefing 160 chars or less, reason 70 chars or less. ASCII, no dashes.
- Lessons: every row below battle 1 has `"lessons": false`, and there is no lessons text field in the sheet. Battle 1 is the only lessons battle, so there is nothing to write. Bonus text (`bonusObjectives`) is already filled in, not a placeholder.
- This draft follows the current `campaignBattles` rows (names, modes, Houses), which differ from the older `docs/copy/chapter-1.md` and `docs/drafts/briefings-2-6.md`. Rows with other placeholder tags (size, difficulty, mode on march-6) are not covered here.

Battle 1 for tone: "Dusk raiders hold the river road. Take the crossings and teach your banner to follow you." / "The road west must open before the march begins." Two short sentences, one place, one verb of intent.

## march-2 The Iron Gate (Ironhold, Siege, Ironclad)
Bonus: lose no more than 10 troops.

- `campaignBattles[1].briefing` (122 chars): The Ironclad hold their gate and will not open it. Break it with the engines, then take the keep behind it.
- `campaignBattles[1].reason` (51 chars): The march cannot go north past a shut iron gate.

## march-3 The Long Night (Hollowwood, Survival, Graveborn)
Bonus: lose no more than 8 troops.

- `campaignBattles[2].briefing` (129 chars): The graves of Hollowwood are opening under the moon. Hold the shrine and keep your banner standing until the dead lie still.
- `campaignBattles[2].reason` (60 chars): The march must camp at the shrine tonight, and so must the dead.

Note: the second half of the reason is a joke-free grim line; swap for "The march camps at the shrine tonight. There is nowhere else." if too much.

## march-4 The Frozen Standard (Frostmarch, Convoy escort, Wildmoot)
Bonus: never let the wagon roll back. The wagon carries the march's standard.

- `campaignBattles[3].briefing` (136 chars): The Wildmoot hunt the pass and the wagon with our standard must cross it. Walk beside it and do not let it be stopped.
- `campaignBattles[3].reason` (57 chars): Without its standard the march is only a crowd of armed men.

## march-5 The Five Crossings (Kingsfield, Domination, Tidecallers)
Bonus: hold all five crossings at the same moment.

- `campaignBattles[4].briefing` (141 chars): Tidecaller raiders have come up out of the water to take Kingsfield. Hold the fords and bridges, and strike where they are thin.
- `campaignBattles[4].reason` (62 chars): Kingsfield is where every road meets, and the march needs them.

## march-6 The Last Watch (Ironhold, Siege, Graveborn, finale)
Bonus: strike down the Hollow King with your own banner. Mode wording stays neutral about the "mixed" finale until Kort picks it.

- `campaignBattles[5].briefing` (148 chars): The Hollow King has taken the keep at Ironhold. The Houses you met stand with you now. Break his gate and strike him down yourself.
- `campaignBattles[5].reason` (55 chars): The king in the keep ends the march, or the march ends.

Note: the last sentence of the reason is intentionally stark; alternative: "Nothing north is safe while he holds the keep." (47 chars).
