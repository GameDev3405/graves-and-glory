# Build brief: campaign chapter 1 "The Oath" and the season gates

Status: all of A to C approved by Kort, 2026-11-09 (`kort-decisions-2026-11-09.md`, items 4, 5, 8 and 12). Revised after a second Sonnet-high review. Written by the lore lead for whichever session the coordinator assigns; the lore lead does not edit code or `sheets/design.json`. Design source: `chapter-1-battles.md`. Base: `main` at `54e5d339` or later.

Outcome: the six chapter 1 battles play as designed, with the right enemies, after-battle text and the glimpur ebb; "The Long March" is gone from player text and "chapter" means a campaign chapter only; old campaign progress resets safely; the campaign is ready for chapter 2 one month after 1.0.

## A. Sheet (`sheets/design.json`, then `tools/preflight.py`; never hand-edit `Spec.java`)

1. **Campaign rows.** Replace `march-1` to `march-6` with `oath-1` to `oath-6` as in `chapter-1-battles.md` section 5 (name, map, size, difficulty, mode, lessons on battle 1 only, briefing, reason, bonus, allies, house `gilded`, chapter 1). Briefings and reasons are in section 3 of that document.
2. **Ally rows.** Add four distinct rows: `ally_gilded_easy`, `ally_gilded_normal`, `ally_gilded_normal_b`, `ally_ironclad_normal`, copies of the matching House rows. Preflight's botBanner id rule (`tools/preflight.py`, about line 563) accepts only `<house>_<difficulty>` and `mix_...` ids, so extend it to accept `ally_<house>_<difficulty>[_b]`; allies must stay distinct per battle (about line 607). Each ally row must only ever be named in `allies`, so no skirmish pool loses or gains a row.
3. **Retired ally rows.** `mix_gilded_ironclad_wildmoot_normal` and `mix_tidecallers_graveborn_normal` were reserved only by `march-6`. Delete them, or they rejoin the normal skirmish pool when `march-6` goes.
4. **Bonus rows.** A new `bonusObjectives` id for every oath battle, using shipped kinds (`hold_all`, `lose_at_most`, `stage_by`, `never_rolled`). None reuses a `march-*` bonus id. oath-6 is `hold_all` on five points, never `kill_unit`.

## B. Code

| Id | What | Where | Size |
|---|---|---|---|
| N1 | Row field `enemies`: botBanner ids by lane, comma separated. Team 1 seats use it when present, at the battle's own tier (`botCommand`); tier and lane picking stay the fallback. Survival ignores it. Preflight checks each id exists and the count matches `size`. | `Campaign.java` about 341 (seating), 370 (`botBanner`); preflight | S to M |
| N2 | Row field `after`: one line on the result screen after a win. Chapter field `chapterEnd`: shown once, the first time the last battle of the chapter is won (store a flag in the profile, or reuse `writChapters` as the trigger). Preflight enforces 160 characters or fewer, ASCII, no dashes as punctuation, no exclamation marks, and allows `{key:name}`. | result screen, `WarTableReport` campaign block; preflight | M |
| N3 | Row flag `ebb`: in those battles, caster troops (`magic: true`: Sunweaver, Gravecaller, Bloodhowl Seer, Mist Caller) on both sides dim and flicker now and then: their own glow and particles fade for a moment, with a faint sound. **No violet**: the Wildmoot must never show violet, and violet-white belongs to the giants. Visual and audio only, no mechanical effect. Set on oath-4, oath-5, oath-6. Needs a visual check against the art standards. | troop render or particle layer | M |
| N4 | Retire the old words in player text. "The Long March": `CampaignClient.java:176` header becomes "THE OATH"; `Campaign.java:309,524`; `WarTableReport.java:510,513,515` ("The Long March" becomes "The Oath", the chapter's name); `Progression.java:128`; `README.md:138`. "Chapter" for the season pass: `Progression.java:128` "Founders chapter", `WarTablePages.java:33` page title "Founders Chapter" and the "Chapter 1 / The Founders" block (`WarTablePages.java:1783-1785`) become "Founders season pass" or "Season pass". These are hard-coded Java strings, not sheet text. Code comments may stay. | as listed | S |
| M1 | Profile step for the reset. `Profile.sanitize` already drops unknown `battlesWon` and `bonusClaimed` ids, so the `march-*` entries vanish on load. Add one step that clears chapter 1 from `writChapters`, so clearing the oath chapter pays its Pact; Pacts already held (`writs`) and owned troops stay. Bump the schema only if the migration code requires it. | `ProfileMigration.java`, `Profile.java`, `RosterMath.pactChapters` | S |
| G1 | **House banner per chapter.** A chapter setting `banner: own | house`. With `house`, the player fields a fixed banner of the chapter's House (one sheet row per chapter) instead of their collection. Chapter 1 stays `own`. Needed for chapter 2 (Wildmoot), one month after 1.0. | `Campaign.java:359` | M |
| G2 | **Season locks (approved).** A chapter field `season`; a chapter's rows unlock when the server's current season reaches it, set from a server date config (there is no season clock today; `Progression.SEASON` is only the pass id). Earlier chapters stay open forever; the order lock still applies, so a late joiner starts at chapter 1. Solo campaigns read the same date. | `MarchMath.lockedBy`, server config | M |
| G3 | **Chapter-aware text.** "Battle N of 6" (`MarchText.battleLine`) and the War Table campaign card name the chapter and count within it. | `MarchText`, `WarTableHome.java:416-441` | S |

G1 to G3 were judged "before season 2"; with one-month seasons they ship with the 1.0 campaign code or within its first month. Chapter 2's Convoy ambush battle plays as Domination on Frostmarch unless an ambush side is built; no code is asked for it here.

## C. Plan amendment

`PLAN-TO-1.0.md:16` (Story row) becomes: "Six campaign battles with briefing cards, after-battle lines, bonus objectives and a Pact. Aldric, a shipped Legendary, speaks in briefings and after lines. No storybook scenes, new named cast, voice or staged sets in 1.0." Approved by Kort on 2026-11-09. The 4D.1 battle table (about line 430) is replaced by the oath rows.

## D. Acceptance

- Fast verify green; preflight regenerates `Spec.java`; before and after hashes saved.
- Every check that names `march-*` ids is rewritten for `oath-*`: `Phase1Checks`, `Phase3Checks`, `SurvivalChecks`, `SiegePlayChecks`, `MarchBonusChecks` (drop its `kill_unit` case on march-6), `MarchModesChecks`, the `p4-march-*` suites, and the standalone `MarchMathCheck`, `HousesCheck` and `ProfileMigrationCheck`. No test is dropped without a replacement.
- Each oath battle fields exactly the rows in its `enemies` field at its tier (harness check per battle); no off-chapter Legendary appears (no Hollow King, Brassjaw or Vael).
- Skirmish and practice bot pools are identical before and after (list compared).
- Sim gate and bot-quality checks re-run.
- A profile with old `march-*` clears and a Pact claimed for chapter 1 loads with chapter 1 at 0 of 6, its `writs` and troops intact, and earns one Pact on clearing the oath chapter.
- One full chapter run in a harness: each after line appears once per win, the chapter-end line shows once, the ebb shows on casters in battles 4 to 6 with no violet on the Wildmoot, and no "Long March" or "Founders Chapter" string appears on any screen.
- Bonus tuning: `lose_at_most 12` (oath-5) and the 360 second gate (oath-3) checked in the sim and adjusted if no bot run clears them.
- G1: a chapter with `banner: house` fields its House row, never the player's collection. G2: a chapter whose season has not started is locked and says when it opens.
- `docs/WORKFLOW.md` rules apply; nothing is pushed or published.

## E. Not in this brief

Map relics (Kort's map designs); storybook pages and scenes (a later story layer); the Bound Host and giant factions (seasons 5 and 6); a Convoy ambush side.
