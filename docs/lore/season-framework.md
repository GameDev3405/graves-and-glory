# Season framework: the campaign after 1.0

Status: Kort's decisions of 2026-11-09 applied (`kort-decisions-2026-11-09.md`): one chapter per season, seasons one month long, story seasons renamed acts, Bound Host and giants as NPC factions in seasons 5 and 6. Names follow `names.md`. Lore lead, 2026-11-09. Builds on the approved reboot design (`docs/superpowers/specs/2026-11-09-lore-reboot-design.md`), the lore bible draft and the chapter 2 to 5 outlines in `docs/drafts/`. Proposals are marked **[call]**. Nothing here changes mechanics, saves or protocol; every new unit, map, mode or House listed is a proposal that needs Kort's yes and its own design.

## 1. What changed today and how this document reconciles it

Kort decided on 2026-11-09: chapter 1 (six battles) ships in 1.0, and chapters 2 to 5 arrive with server seasons. A season can add a campaign chapter, units, maps, modes and now and then a whole House or faction.

The approved design used "season" for a story act: season 1 was all five chapters, season 2 was the giants waking, season 3 the resolution. With one chapter per season, those labels no longer fit. **[call]** This document renames the design's three story seasons to **acts** and keeps everything else:

| Act | Was called | Seasons (one month each) | Job |
|---|---|---|---|
| I. The Ring | season 1 | 1 to 5 | The Houses fight over old wrongs, one House chapter per season. Giants only hinted. Ends with the spire's light. |
| II. The Waking | season 2 | 6 to 8 | Orruvan wakes under the capital. Giants are a non-playable enemy. The Houses split; the Gilded are offered terms. |
| III. The Choice | season 3 | 9 to 11 | The other elders wake, execute the Skeleton King, and their will breaks. Orruvan chooses to live. Giants become a playable House. |
| After | after | 12 on | New arcs on a settled world: Demacrus, new peoples, other giant bodies. |

Every rule of the design still holds with "act" read for "season": nobody names the giants before the end of Act I, no later act contradicts an Act I hint, and the ring stays open at the end of Act I.

**Why one chapter per season [call].** Each chapter is already written as one House's story, ends on that House's own wrong, and hands the next House its Legendary. That is a natural season: the season's House gets the spotlight, the next season's Legendary is the cliffhanger. The cost is pace: the giants stay offstage for five seasons. Section 4 gives each season a visible step up in the giant thread so the wait is felt as rising pressure, not as nothing happening. The alternative (two chapters per season, Act I over three seasons) is listed for Kort in section 8.

## 2. What a season is made of

A season is one story beat plus content that serves it. **[call]** A standard season carries:

1. **One campaign chapter**: six solo battles, one House's side, order-locked after the previous chapter.
2. **One spotlight House**: the chapter's House gets the season's new unit and its season pass art.
3. **One to two new units**, at least one for the spotlight House.
4. **A new map only where the story needs one** the shipped five cannot stand in for. With one-month seasons this is seasons 4, 5 and 6 (the Drowned Coast, Tengervald, Highheart). Seasons 2 and 3 reuse shipped maps with new dressing.
5. **Optionally one mode or mode variant**, only when the chapter's story needs a way of fighting the shipped four cannot give.
6. **One step of the giant thread**: a new relic, hint or world change visible in the hub and on maps (section 4).
7. **Rarely, a House or faction**: at most one new faction per act, at the act's turning point. Splinter groups inside an existing House (section 6 hooks) are new units for that House, not factions.

A season never retcons an earlier one, never removes a House, and never makes an earlier chapter's battle unwinnable or wrong in hindsight.

## 3. The roadmap

Each row is a proposal; content counts follow section 2. "New unit" names a role, not a design. Kort designs maps; the map column gives the place and what it must show.

| Season | Chapter | Spotlight | New units (proposal) | New map (place, what it shows) | Mode (proposal) | Faction | Giant thread step |
|---|---|---|---|---|---|---|---|
| 1 (1.0) | 1 "The Oath" | Gilded | none (30 shipped) | none (5 shipped) | 4 shipped | 5 Houses | Spells sputter; the carved stone shape at the quarry; relics on maps. |
| 2 | 2 "The Long Grass" | Wildmoot | a herd beast (heavy, non-speaking) | none: shipped maps dressed with withering grass | Convoy ambush side, if not built for 1.0 | none | Herds flee uphill in the hub; grass withers on one map. |
| 3 | 3 "The Forge Below" | Ironclad | a redstone engineer or mech | none: Ironhold and Frostmarch dressed with pulsing redstone veins over Kharuun | none needed | none | Redstone in the hub pulses toward the mountains; forges cool. |
| 4 | 4 "The Drowning" | Tidecallers | a tide caster | The Drowned Coast: the eleven towns half under water | Tide variant: points shift as water rises **[call]** | none | The tide pulls inland in the hub; Tidecaller glow fades. |
| 5 | 5 "The Rest" | Graveborn | a freed unit (cut binding) | Tengervald: Halvard's dying realm and its barrows, over Ysmorren | Alliance Survival: two or three Houses against the bound host **[call]** | **The Bound Host** (the Skeleton King's army) as an NPC enemy faction | The spire's light. End of Act I. |
| 6 | 6 (Act II opens) | all Houses | a giant enemy set (non-playable) | Highheart: the Concord's capital on Orruvan's chest, over the Last Breath | Giant siege: co-op defence **[call]** | **Giants** as a non-playable enemy | Orruvan wakes. The giants are named. |
| 7 | 7 | Gilded, then Ironclad | Unsworn units (Gilded who took terms) and Kin-seeker units (Ironclad) **[call]** | Kharuun's mountain | as needed | none (splinters are units of their Houses) | Terms offered; Forge refused as kin. |
| 8 | 8 | Wildmoot, Tidecallers, Graveborn | per House | Ommeru's plains, Thalluun's sea floor | as needed | none | The Houses with no terms fight. The Skeleton King hunts all. End of Act II. |
| 9 to 11 | 9 to 11 (Act III) | all | giant units | elder bodies, then beyond the sea | as needed | **Giants as a playable House** | The elders wake, the king is executed, Orruvan chooses to live. |
| 12 on | new arcs | open | open | Demacrus and other lands | open | new peoples (section 6) | Shared glimpur: a new kind of magic. |

**Act II and III chapters [call].** Act I chapters each belong to one House. From Act II the Houses split under the giants, so a chapter may switch Houses between battles (two to three battles per House). This needs a campaign engine that allows a House per battle rather than per chapter; flagged for code in section 7.

**Chapter 2 to 5 outlines.** The drafts in `docs/drafts/` stand as written, with three changes when each chapter is turned into a battle design: (1) each may use its season's new map and unit, replacing one reused map; (2) each closing hint now reads as that season's giant thread step; (3) chapter 4's last battle hands Aldric to chapter 5's finale rather than to "season 2".

## 4. The giant thread across Act I

Five seasons of hints must build, so each season adds one thing every player sees, not only campaign players **[call]**:

1. Season 1: relics on the five maps; magic troops flicker in chapter 1's later battles (N3 in the chapter 1 design); the carved stone at the Kingsfield quarry.
2. Season 2: the hub's grass withers in one patch; herds of passive mobs drift uphill.
3. Season 3: redstone in the hub pulses toward the mountains; forge light dims.
4. Season 4: the hub's water line pulls back.
5. Season 5: the spire's light over the capital at the end of the chapter, then in the hub sky for the rest of the season.

All are visual or audio only, with no mechanical effect, and need Kort's yes for each hub change.

## 5. Rules for adding a unit

A new unit fits the lore when it passes all of these:

1. **One House, one species.** It belongs to exactly one House and is that House's species. Gilded are human; Wildmoot are animals; Ironclad are forge people; Tidecallers are sea people; Graveborn are the freed dead.
2. **Its magic is glimpur, and the House's kind.** If it uses magic, it draws glimpur the House's way: Gilded mages cast it, Wildmoot grew cunning on it, Ironclad graft redstone (crystallised glimpur), Tidecallers were remade by it, Graveborn are kept by what is left of the binding. A magic unit shows the ebb tell when the story calls for it.
3. **It has a place in the House's grievance.** Its flavour text names what it does in the House's war: what wrong it answers or what wrong it commits. No unit is neutral.
4. **The House's look.** Shipped silhouettes, palettes and gear set rules apply (Oathbound, Thornhide, Forgewall, Tidewrought, Barrow Warden). Wildmoot never use violet. Only giants and their glimpur glow use violet-white.
5. **Speech rule.** Among the Wildmoot only boar-kin, the herd-mother and Old Horn speak (as the chapter 2 outline has it). A non-speaking unit never has lines.
6. **No early giants.** Before Act II no unit is a giant, carries giant technology outside redstone, or names a giant.
7. **No new named characters** unless Kort approves one. The Legendaries are the named cast; new units are roles and titles.
8. **Mechanics are separate.** The lore never sets stats, counters or costs. A unit's design goes through Kort and the balance process.

## 6. Rules for adding a House or faction

A new House or faction fits the lore when it has all of these, written down before any art:

1. **A body.** Which giant's body it lives on, or that it comes from beyond the sea.
2. **A glimpur dependency.** How glimpur made it, changed it or powers it, and so what the cure means for it (extinction, reversion, or only loss of magic).
3. **A wrong done to it and a wrong it did.** It enters the grievance ring with one of each, against Houses already in the game, without erasing their grievances.
4. **A myth of the giants.** A distorted memory of a specific elder.
5. **A voice** in one line, distinct from the five.
6. **A Legendary** with a title.
7. **A season and an act.** When it arrives, and why the story needs it then.
8. **What the giants think of it.** By default it is "an outcome of glimpur used for evil" and gets no terms. Only humans get terms; a new human faction is the only exception.
9. **Playable or not.** NPC factions (the Bound Host, the giants in Act II) can become playable later only through a story turn, as the giants do in Act III.

**Hooks for future Houses and seasons**, left open on purpose:

- **The Charter's gap.** "The Charter does not say who is a person." Any species can claim personhood and enter the ring.
- **Tengervald's living.** The Skeleton King's realm still has living people the confederacy abandoned: a human faction with its own grievance against the Gilded.
- **The Unsworn.** Gilded who take the giants' terms in Act II and give up magic.
- **The Kin-seekers.** Ironclad who hoped the giants would claim them, refused.
- **The eleven towns.** Survivors of the drowned coast.
- **The binding's heir.** The Skeleton King's knowledge survives his execution.
- **Elders beyond the sea.** More giants, more bodies, in Demacrus and other continents.
- **Demacrus.** Where humans and the forge people came from; who stayed behind.
- **Shared glimpur.** After Act III the giants share glimpur, which can make new magic, new units and new peoples.

## 7. What needs code or decisions before season 2 (for planning, not approved)

Already in the engine: `chapter` on campaign rows, the order lock across all rows (`MarchMath.lockedBy`), and a Pact per cleared chapter (`RosterMath.pactChapters`).

Gates before season 2:
- **The chapter's House banner.** The player fields their own collection banner (`Campaign.java:359`), so "commanding the Wildmoot" in chapter 2 needs a fixed or restricted House banner per chapter (chapter 1 design, D1 option b).
- **Season gating.** A chapter's rows unlock with its season, not only with the previous chapter.
- **Late joiners.** A player who starts in season 4 meets the order lock at chapter 1. Decide whether earlier chapters stay open forever (recommended) and whether a later chapter may be played first.
- **Text.** "Battle N of 6" (`MarchText.battleLine`) and the campaign card assume one chapter.
- **Economy.** Each chapter pays a Pact, which buys a Legendary: one free Legendary per season. Check with the economy model.
- **Convoy ambush side** for chapter 2 battle 4, or that battle becomes Domination.

**With one-month seasons, season 2 arrives one month after 1.0.** Every gate above therefore has to ship with 1.0 or in its first month. **[call]** Build the House banner, season gating and the late-joiner rule together with the approved chapter 1 code (N1 to N4).

Later:
- A House per battle within one chapter (Act II).
- An NPC enemy faction with its own roster (the Bound Host in season 5, giants in season 6). Survival's waves are the nearest existing piece.
- Hub world changes per season (section 4), the tide variant and alliance Survival, if Kort wants them.
- **Term clash.** The shipped season pass is called the "Founders chapter" (`2026-10-17-houses-design.md` 4.8). **[call]** Campaign chapters keep "chapter"; player text calls the pass the season pass.

**A lite season [call].** If a season is short on time it still ships its chapter and its giant thread step; the new map, unit and mode can slip to the next season. The chapter is the season's spine.

**Older drafts.** The chapter 2 to 5 outlines and the lore bible say "season 2" and "season 3" for the story parts; read them as Act II and Act III. Names in all documents follow `names.md` (Wakeland, Highheart, the Concord, Merudaal, Halvard the Holdfast, the five elders).

## 8. Decided (Kort, 2026-11-09)

1. One chapter per season; Act I is seasons 1 to 5.
2. The design's story seasons are Acts I to III.
3. A season is one month.
4. The Bound Host is an NPC enemy faction in season 5; the giants an NPC enemy in season 6.
5. New maps only where the story needs them (seasons 4, 5, 6 by default).
6. Season locks (Kort, 2026-11-09): a chapter unlocks when its season starts, set by a server date; earlier chapters stay open forever; the order lock still applies, so a late joiner starts at chapter 1.
