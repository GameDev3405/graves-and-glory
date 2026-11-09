# Lore status: decided, draft, and what contradicts the game

Status: lore lead, 2026-11-09. Sources: the approved design `docs/superpowers/specs/2026-11-09-lore-reboot-design.md`, the drafts `docs/drafts/lore-bible-2026-11-09.md` and `chapter-1..5-outline-2026-11-09.md`, `PLAN-TO-1.0.md`, and `main` at `54e5d339` (`sheets/design.json`, `Campaign.java`, `MarchMath.java`, `Modes.java`, `CampaignClient.java`). The design and the drafts are uncommitted files in `bannerbound-mainmerge`; copies live on branch `docs/lore-lead` in `bannerbound-lore`.

## 1. Decided by Kort

- Full reboot; the Long March, Barrowlands, Horn, Edric Vane and the old cast are retired with no cameos. The Graveborn virus origin is dropped.
- The land is the earthen giants' bodies. Their drained mana, glimpur, is the magic in every unit. The spire woke; the giants want all glimpur destroyed. Calm, courteous, sympathetic; machine and crystal.
- Five Houses are species, chosen per match: Gilded human, Wildmoot animals, Ironclad forge people, Tidecallers sea people, Graveborn rebels from the Skeleton King's army. Shipped House names, Legendaries and map names stay. Mechanics do not change.
- The Skeleton King: a human king who bound the dead to protect his dying realm after the confederacy looked away. His binding tripped the spire. He survives season 1, keeps hunting the Houses when the giants arrive, and the giants execute him once all elders are awake.
- Terms only for the Gilded. The leader elder is the weary one; persuading it to live is the saga's ending.
- Five House chapters in ring order, Graveborn last; the ring stays open; the commander is silent; the Hollow King is a mocking title; fixed chapter order.
- Seeds in 1.0: giant relics on maps and a cosmetic glimpur ebb.
- New today: chapter 1 (six battles) ships in 1.0; chapters 2 to 5 arrive with server seasons; seasons can add chapters, units, maps, modes and sometimes a House; everything must leave room to grow for years.

## 2. Draft, waiting for Kort

- The lore bible in full, the five chapter outlines and every **[call]** in the design.
- Names: Eldmark, Tengervald as the Skeleton King's realm, the five elder names and translations. Unnamed: the Skeleton King, the spire, the capital, the confederacy.
- How long the Skeleton King lives: the design reads it as acts I and II with his death in act III, a call awaiting Kort's confirmation.
- Aldric as the likeliest to accept the giants' terms (the writer's framing).
- Glimpur's violet-white look; five elders on Eldmark; the timeline dates.
- The Charter's clause and its gap line; titled-only supporting roles.
- The Wildmoot's glimpur grass; the Tidecallers' origin wording; the Graveborn's wrongs against Wildmoot and Tidecallers; the Forge kin refusal.

## 3. Contradictions with the game as built or planned

| # | Contradiction | Where | Proposed fix |
|---|---|---|---|
| C1 | The design puts all five chapters (30 battles) in 1.0 and calls the giants' arrival "season 2". Kort now ships chapter 1 only, with one chapter per season after. | design sections 8 and 9 | Rename the design's story seasons to acts (season framework section 1). |
| C2 | The chapter 1 outline is built on storybook pages, stills, stage scenes, card items and the archive. None exist, and `PLAN-TO-1.0.md:16` keeps storybook, cast, scenes and sets out of 1.0. | chapter 1 outline | 1.0 text lives in briefings, reasons and bonus lines, plus a proposed after-battle line; the pages wait for a later story layer (chapter 1 design section 6). |
| C3 | Chapters are told "from the House's side", but the campaign fields the player's own collection banner, any House. | `Campaign.java:359` | Decision D1: own banner with House allies in 1.0, fixed House banner before season 2. |
| C4 | The enemy House is not chosen per battle: bots come from tier rows in sheet order. Today's briefings already name enemies the code does not field. | `Campaign.java:341,370` | New sheet field naming enemy rows (N1). |
| C5 | The shipped finale (`march-6`) makes the Hollow King the villain to kill. In the reboot he leads the rebel House. | `design.json` march-6 briefing and bonus | Replaced by the new chapter 1 rows. |
| C6 | "The Long March" is shipped player text. | `CampaignClient.java:176`, `Campaign.java:309,524`, `WarTableReport.java:510,513,515`, `Progression.java:128`, `README.md:138` | Rename to the chapter title (N4). |
| C7 | The Skeleton King's army and the Graveborn rebels use the same troops; Survival's waves are Graveborn troop ids. | `waveGroups` | Text never calls the bound dead Graveborn in chapter 1; later an art tell (intact sigils against cut cords). |
| C8 | The Bound Host as an enemy army with commanders (chapter 5) needs a third faction or a Graveborn bot banner relabelled; the engine has two teams only. | `Campaign.java:411-423` | Two teams suffice (Houses plus allies against the host) if the host uses Graveborn rows; a distinct faction needs code. |
| C9 | Chapter 2 battle 4 needs the player on the ambush side of Convoy; Convoy is Dawn-escorts only. | `Modes.java`, convoy spec | Code before season 2, or Domination on Frostmarch. |
| C10 | The plan's campaign introduces one House per battle; the reboot chapter is Gilded throughout. | `PLAN-TO-1.0.md:430-439` | The new chapter still meets all five Houses as enemies or allies. |
| C11 | Ironclad have no casters, so "the ebb" cannot show on Ironclad magic troops. | `design.json` troops | Show the Ironclad hint on redstone parts and forges instead. |
| C12 | Graveborn's trait `rise_again` makes fallen troops rise, though the rebels broke the binding. | `houseTraits` | Flavour: what is left of the binding still holds them for a moment. No mechanic change. |
| C13 | The season pass is called the "Founders chapter", clashing with campaign chapters. | `2026-10-17-houses-design.md` 4.8 | Call it the season pass in player text. |
| C14 | Old Horn is a bear in the sheet; the design names boar-kin as the Wildmoot speakers, but no troop is named boar-kin (Tusk Spear is the nearest). | `design.json` Wildmoot | Treat Tusk Spears as the boar-kin; keep Old Horn a great antlered beast. |
| C15 | Map relics have no system; maps are Kort's to design. | `MapDressing.java` | Ask Kort to include relics in his map designs. |
| C17 | `PLAN-TO-1.0.md:16` says no named cast in 1.0; the chapter's after lines have Aldric speak. | plan | Aldric is a shipped Legendary; ask Kort to amend the plan line. |
| C18 | Placeholder names (Tengervald, the Charter's wording) would enter player text in chapter 1. | chapter 1 design | Kort confirms or renames before the text ships. |
| C19 | Keeping the battle ids `march-1` to `march-6` would mark new battles cleared in old profiles and keep a paid Pact. | `Profile`, `MarchMath` | New ids plus a reset or migration decision (save change, Kort). |
| C16 | House flavour text in the sheet and the archive still carries the old lore. | `design.json` houses, troop text | A flavour-text proposal after Kort's review of the bible (design step 3). |

"Barrow Stalker" and the "Barrow Warden" gear set keep their names: a barrow is a grave mound, not the retired Barrowlands.
