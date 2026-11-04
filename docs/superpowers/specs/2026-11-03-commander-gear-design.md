# Commander gear: design

Date: 2026-11-03. Status: draft for Kort's approval. Kort's answers are marked as **Kort**. My calls are marked as **call**, and Kort can overrule them.

## Why

Today the commander does little in battle besides give orders. They have low damage and low health and contribute nothing on the front line. Every commander gets the same kit (iron sword, bow, 64 arrows and iron armour, from `GravesAndGlory.java:812` `equipPlayer`), plus a small class modifier (`Commander.applyPlayer`, `Commander.java:481`). **Kort:** the moment the commander feels useless is fighting on the front line.

The goal is that the commander becomes a **hero**, worth roughly 3 to 5 troops, who only goes down when surrounded (**Kort**). The player picks a role through gear (bruiser, healer, ranged or commander), earns gear by playing, and builds a collection over time.

## Goals
- Gear that makes the commander a real fighter in one of 4 roles.
- A post-match loop: earn gold plus **Forge marks**, then roll gear at a blacksmith.
- A Minecraft Dungeons-style gear screen showing the character, the worn slots, the relics and the inventory.
- Fair matches despite real power growth, handled by ranked and unranked matchmaking. That's a separate project, but it ships in 1.0 (see Dependencies).

## Non-goals
- Trading gear between players, a gear market, or paid gear.
- Crafting individual stats (beyond picking which slot to roll).
- Gear for troops. Troop gear stays in the Armoury.
- Using gear mid-battle beyond relic activations. There's no swapping during a match.

## Anti-goals (what would make this fail even if it works)
- Gear that turns the commander into a solo army, so troops stop mattering. Hero scale is capped at about 3 to 5 troops.
- Clients editing their own gear. All gear must be server-authoritative.
- Grind that feels like a chore. About one roll per match is the target pace.
- Generic-looking gear. Kort's art bar is Souls-like game art, with each piece unique.

## Decided by Kort

**Loadout**
- Full Minecraft slots: helmet, chestplate, leggings and boots, plus main hand and off-hand.
- **3 relic slots.** Relics are **active abilities** on keys 1, 2 and 3, each with a cooldown.
- **Any relic works in any build.**

**Classes**
- **Gear replaces class.** The 6 commander classes (`Commander.java:54-61`) are removed, and their actives are replaced by relics.

**Roles and sets**
- **Sets define the role.** There is 1 set per role at 1.0: bruiser, healer, ranged and commander, so 4 sets of 4 armour pieces each.
- Wearing 2 or 4 pieces of a set unlocks its bonuses. Your role is your strongest set.

**Weapons at 1.0**
| Weapon | Hands | Role fit |
|---|---|---|
| Sword + shield | Main hand + off-hand | Bruiser and all-rounder |
| Greatsword or great-axe | Two-handed, fills both slots | Bruiser |
| Bow or crossbow | Main hand, with a quiver or dagger off-hand | Ranged |
| Healing staff | Main hand | Healer |
| War banner | Main hand | Commander |

**Rarity**
- Five tiers: Common, Uncommon, Rare, Epic and Legendary. Each tier has a border colour: grey, green, blue, purple and orange.
- Higher rarity means bigger stats **and** a look upgrade (trim, glow, extra detail).
- Epic and Legendary pieces also carry one perk (**Kort:** "mix of stats+perks and looks").

**Looks**
- **Every piece is unique.**
- The worn gear shows on the commander in battle.

**Currency**
- The new currency is **Forge marks**. The name "shards" is already taken by an existing currency (`Profile.shards`).

**Earn rate**
- About **one roll's worth per match**. A loss pays about half.
- Gold rewards are unchanged.

**Items are a named catalogue (Kort)**
- Every armour piece and weapon is a hand-made named item, such as "Warden's Greathelm", with its own art.
- A roll picks an item from that slot's list, then its rarity, then its stat numbers within that rarity's range.
- A **duplicate** is the same item at the same rarity.
- **Call:** duplicates are not destroyed automatically. They show a "duplicate" badge, and the player breaks them down by hand. A one-click "break down all duplicates" button sits on the blacksmith screen.

**Weapons and relics (Kort)**
- Weapons roll like armour (rarity, stats, duplicates). They belong to no set; only the 4 armour pieces count toward set bonuses.
- **Relics unlock once.** They have no rarity and no stat roll. Rolling a relic you already own turns it into Forge marks.
- Relics are rolled through a "Relic" slot choice at the blacksmith.

**Rolling**
- **Pick the slot, the rest is random:** which item, its rarity and its stats.
- **Pity:** after N rolls with no Epic or better, the next roll is guaranteed one. Pity doesn't apply to relic rolls.
- **Duplicates break down into Forge marks.**

**Blacksmith**
- A **new camp NPC station** next to the quartermaster, archivist and steward (`sheets/design.json` `campStations`).
- It handles rolling, repair and breakdown.

**Inventory**
- **60 slots to start, expandable** with gold in rows.

**Wear**
- Gear wears down each match. The blacksmith **repairs it for gold**, and a piece is **never lost**.

**Bots**
- A bot gets a full rolled loadout at about the **average gear power of the humans in the lobby**.

**Where gear counts**
- **Everywhere:** the campaign, 4v4 and all other modes.

**Fairness**
- Real power growth is allowed. **Ranked and unranked matchmaking** keeps matches fair: ranked matches gear power strictly, and unranked loosely.
- Matchmaking is in scope for 1.0, as its own project.

## The gear screen (layout A, Kort's drawing)
Mockup: `docs/superpowers/specs/2026-11-03-commander-gear-mockup.html`, option A. It uses the War Table style: dark wood, a gold rope divider, parchment cards and serif text.

**Left card**
- A stats column: Health, Damage, Armour, Speed, Ranged, Healing and Command.
- Beside it, the commander in a gold frame, wearing the equipped gear.
- Below that, a "Worn" row of 6 slots with the set progress shown (for example "Warden 2 of 4").
- Below that, a "Relics" row of 3 slots labelled 1, 2 and 3.

**Right card**
- An inventory counter ("37 of 60") and the Forge marks and gold balances.
- Filter tabs: All, Helms, Chests, Legs, Boots, Weapons and Relics.
- A sort dropdown with Rarity, Newest, Slot and Set.
- A grid with rarity-coloured borders.
- **Hover** shows a tooltip: name, rarity, set, stats, perk, ▲ or ▼ against the worn piece, and durability.
- **Click or right-click** equips the piece. **Shift-click** breaks it down, with a confirmation for Epic and above.

**Call:**
- The screen opens from the blacksmith and with a key anywhere in camp. It never opens during a battle.
- The blacksmith's own screen uses the same frame with a Forge tab: pick a slot, see the cost, roll, and then a reveal.

## Calls (mine, for Kort to check)

**Catalogue size at 1.0**
- One named item per armour slot per set: 16 armour items.
- About 2 named items per weapon family: about 10 weapons, 2 of them off-hands.
- About 12 relics.
- Each armour item and weapon has 5 rarity looks.

**Weapon behaviour**
- **Sword and shield:** vanilla-feel melee. The shield blocks on right-click with a short cooldown after a block.
- **Greatsword or great-axe:** slower and heavier hits with a small sweep. It can't be used with an off-hand.
- **Bow or crossbow:** vanilla draw and fire. Crossbow is slower and hits harder; bow draws faster.
- **Healing staff:** right-click fires a short-range healing bolt that heals the first friendly troop or commander hit. Left-click is a weak melee.
- **War banner:** left-click is a medium melee. While held, it doubles the radius of the commander set's morale aura and of relic auras.
- Every family needs its commander attack clips, and the weapon faces the move. The plan lists the clips per family.

**Relics and the old class actives**
- The six class actives (Stand Fast, Muster, Grave Bloom and the rest) become relics, keeping their troop-affecting effects at today's scale.
- About 6 new relics are added, mostly self-focused: a dash, a shield bash, a heal pulse and a volley.
- Any relic works in any build (Kort).

**Hero target, measured**
- "Worth 3 to 5 troops" means a full Legendary commander beats 3 troops of the same house in an open-field harness duel at least 80% of the time, and loses to 5 at least 80% of the time.
- A full Common commander beats 1 troop and loses to 3.
- These tests live in the harness and run at the balance step.

**Matchmaking boundary**
- This project builds the **gear power** number and stores it per profile, plus a stub that the matchmaking project consumes. It doesn't block on matchmaking.
- The campaign isn't rescaled. Campaign enemies are tuned at the balance step for a mid-geared (Rare) commander.
- A bot-only lobby's bots use the host's gear power.

**Existing profiles**
- Each old class maps to its closest role's starter set. The plan lists the mapping; for example the Champion and Reaver classes become the bruiser set.
- Class-unlock refunds are the gold actually paid.
- The plan audits every reader of `profile.commander`: Houses, stats, QA and text.

**Reward maths**
- Forge marks = base × (1 for a win, 0.5 for a loss) × (0.5 if every opponent was a bot).
- The same base applies in campaign, 4v4 and custom matches.
- One roll price for every slot except relics, which cost 1.5×.

**Hero tuning**
- A full Legendary set reaches the "3 to 5 troops" target. A full Common set sits at about 1.5 to 2 troops, against today's stock commander at about 1.
- All tuning lives in sheet rows (`sheets/*.json` through `tools/preflight.py`, which generates `Spec.java`), so tuning needs no code changes. Balance has been closed since 2026-11-02, so this reopens it, and the numbers go to Kort before release.

**Set bonuses**
- Each set's 2-piece and 4-piece bonuses:

| Set | 2 pieces | 4 pieces |
|---|---|---|
| Bruiser | Damage reduction while surrounded | Taunt aura |
| Healer | Heal-over-time on nearby troops | Revive-speed or cleanse |
| Ranged | Draw speed | Piercing or volley |
| Commander | Morale and order radius | A command bonus such as faster formation and charge |

- Set names, perks and relic list: the builder drafts them and Kort approves.

**Relics at 1.0**
- About 12 relics, roughly 3 suited to each role, all usable in any build.
- At least one relic replaces each removed class active.

**Roll cost, breakdown and pity**
- A roll costs about one average match's Forge marks.
- Breakdown returns 20, 35, 50, 70 or 100% of a roll's cost by rarity, Common to Legendary.
- Pity N = 10. Final numbers live in the sheet.

**Wear and repair**
- Each match costs 10% durability.
- At 0%, the piece still works at half stats until repaired.
- Repair is gold, scaled by rarity.

**Expanding the inventory**
- Each extra row of 10 slots costs a rising amount of gold, up to 120 slots.

**Starting the system**
- Every existing and new profile starts with a free Common set and weapon matching its old class's closest role.
- The old class-unlock purchases are refunded in gold.

**Gear power**
- Gear power = the sum of piece rarity weights. Bots and matchmaking use it.

**Death**
- No gear loss on death; only normal wear.

## What it touches (checklist for the plan)

**Save**
- Profile schema 3 → 4 in `Profile.java` and `ProfileMigration`.
- New fields:
  - Forge marks
  - gear inventory (compact IDs plus rolls)
  - equipped slots
  - relics
  - durability
  - pity counter
  - inventory size
- Migration grants the starter set and refunds classes.
- Writes go through `SafeFiles.writeWithBackup`.

**Network**
- The 16,000-character snapshot cap (`Campaign.SNAPSHOT_LIMIT`, importProfile and the PROFILE receiver) can't hold a 120-item inventory. It needs a separate paged gear sync packet or a compact encoding.
- New or bumped channels (protocol gate):
  - gear sync
  - equip, roll, repair and breakdown requests (all validated on the server)
  - relic activation
- Bumps to the profile_v1 and state_v9 versions as needed.

**Rewards**
- `Campaign.java:557-574` and `MatchRules.grossGold` (`MatchRules.java:50`) add a Forge marks payout: a win pays the full amount and a loss about half.
- Rows go in `Spec.CAMPAIGN` and `Spec.ECONOMY`.
- Bot-only matches pay less. **Call:** half, so they can't be farmed.

**Combat**
- Replace `equipPlayer`'s fixed kit with the equipped gear.
- Replace `Commander.applyPlayer` class modifiers with gear stats, set bonuses and perks.
- Relics on keys 1, 2 and 3. This reuses the class-active key path, and bots get a simple relic AI (bots never fired actives before).
- `Battle.allowDamage` is unchanged.
- The "only goes down if surrounded" feel comes from tuning (damage reduction while surrounded) and is tested.

**Classes removed**
- Class selection UI, `Commander` registry, and sheet rows.
- Campaign text that names classes.
- QA rows and docs.

**Camp**
- A new blacksmith station in `campStations` and `CampWorld.npc`.
- A `CampScreen` page.
- A new gear screen.
- `ArmouryTab` stays troop-only.

**Bots**
- Loadout generator at the lobby's average human gear power.
- Relic use.

**Art**
- 4 sets × 4 pieces × 5 rarity looks.
- 5 weapon families.
- About 12 relic icons.
- Item icons for the grid.
- Commander render on the screen and in battle.
- Every piece is unique, to the 1 texel per pixel standard and Kort's Souls-like taste. There are no back flags or pennants (Kort, 2026-10-29).
- Large cost: plan art as its own track with its own reviews.

**Animation**
- Greatweapon, staff and banner attack clips for the commander. The weapon must face the move (Kort, 2026-11-03).

**Text**
- Item names, perks, relic text, blacksmith lines, tooltips, the how-to-play page and the Discord roadmap post.

**QA and tests**
- Roll distribution and pity tests.
- Migration tests (schema 3 → 4, with refund).
- Packet validation tests: a client cannot equip an item it doesn't own.
- Snapshot and gear sync size tests at 120 items.
- In-game tests through the harness: a hero vs troops duel against the 3-to-5 target, and the screen at 1080p, 1440p and 4K.

## Dependencies and order
1. **This design → plan.** It builds in parts: (a) data, save and sync; (b) rewards and blacksmith; (c) gear screen; (d) combat, relics and class removal; (e) bots; (f) art track in parallel; (g) balance pass with Kort.
2. **Matchmaking (ranked and unranked)** is its own brainstorm and spec. It uses the gear power number from this spec.
3. PLAN-TO-1.0.md lines 20, 36 and 392 say public matchmaking is post-1.0. Kort moved it into 1.0 on 2026-11-03, so update the plan to match.

## Approval gates this needs
- The save schema (v4).
- Protocol (new and bumped channels).
- Gameplay (class removal and hero power).
- Balance reopen.
- Art reviews.

Kort approving this doc covers the direction. The numbers come back to him at the balance step.
