# Supply houses: capturable forward bases on every map

Status: **approved by Kort as written, 2026-10-21** (after the builder check). Kort's answers from the brainstorm are
marked **(Kort)**. Calls the
designer made where Kort did not rule are marked **(designer's call)**; Kort may overturn any of them. Nothing here is
built.

This design fills the place the in-match progression design left for it (`2026-10-22-in-match-progression-design.md`
sections 5.2 and 5.3). It changes three approved things: held control points stop being forward deploy spots (Phase 1
forward reinforcement), the gear shop's stopgap location rule (progression 5.3) ends, and Banner mode's point B stops
deploying (Phase 4 spec section 7). Everything else in the approved specs stands. It does not revive the deleted growth
design: nothing here persists between matches.

Sources: Kort's direction of 2026-10-21 and his click-through answers in this brainstorm; the in-match progression
design; the built-world spec and the Phase 3 plan (`2026-10-17-built-world-design.md`,
`plans/2026-10-17-phase-3-built-world.md`); the Houses spec section 5 (Forward Supply, the `Anchor` record); the unit
AI spec; the Phase 4 modes spec; the UI spec (`2026-10-18-ui-design.md`); `sheets/design.json` (`economy`, `points`,
`rules`, `morale`, `botCommand`, `orders`, `classes`); and the code read through the graphify graphs:
`Battle.reinforcementAnchors`, `Battle.deploy`, `Battle.deployCentre`, `Battle.captureTick`, `Battle.atBarracks`,
`Anchor`, `ArmySupplyBanner`, `Morale.tick`, `Morale.rally` and `Campaign.botDecide` on `phase-2-houses`;
`CampaignMaps.select`, `Arena.height`, `Arena.roadLevel` and `Arena.flag` on `phase-3-built-world`; `FeedEvents` and
`GravesAndGloryClient.MapScreen` on `ui-plan`.

**Routing.** During this brainstorm Kort ruled that routing goes away for everyone's troops ("if I send my troops
somewhere they stay there and continue the job without question"). That change gets its own design,
`2026-10-22-no-routing-design.md`, being written in a parallel session **(Kort)**. This design is written for a game
with no routing: houses do not take routing troops, and "regroup" is an order (section 5.2).

---

## 1. Intent

Kort, 2026-10-21: "there's going to be buildings called 'supply house' which will be placed around the map/control
points. these houses can be used to deploy new troops, regroup, level up troops, heal injured troops, etc. but the
catch is that these houses only work for the person whose team controls the house. so each house is also a side quest
domination point to control, which could change the tide of the war if one player stole their enemy's supply house and
vice versa." He confirmed that "level up troops" means buying gear tiers at the house; levels still come instantly from
kills.

Why this is better for the game:
- Points win the match; houses keep the army going. Splitting those jobs gives every match two fights: the point
  fight in the middle, and the fight over the supply line on the flanks. A player who reads the map better can win the
  second while holding the first.
- A stolen house is a forward base deep in enemy ground and one fewer for the enemy. That is a real swing ("change the
  tide of the war"), but it is close to the owner's base and far from the thief's, so it is easy to win back for a side
  that notices. The warning line gives the owner that chance.
- Today the side that holds a point also reinforces at it, so winning the middle snowballs into faster reinforcements
  at the front. With houses, deploying no longer follows the point fight.

### Goals
1. Four supply houses on each three-point map and two on Kingsfield, two (or one) in each half **(Kort)**.
2. Every house starts neutral; any side may take any house **(Kort)**.
3. A held house gives its team four services: deploy, regroup, healing and the shop (troops and gear) **(Kort)**.
4. Houses give no score and no gold **(Kort)**.
5. A house is harder to take than a point, and its owner is warned when it is being taken **(Kort)**.
6. Bots claim, defend and raid houses, more cleverly at higher difficulty **(Kort)**.
7. Every house reads at a glance: one shape on every map, its holder shown in the world, on the M map and in the feed
   **(Kort)**.

### Non-goals
- No score, no gold income, no win condition tied to houses.
- No guards, garrisons or NPC mobs at a house. A house is defended by troops you send there.
- Houses cannot be damaged or destroyed. They are not structure targets (Phase 2's C10 and C11 do not apply).
- No commander respawn at a house: commanders respawn at the base as today **(designer's call)**.
- No house upgrades, building or tiers.
- No routing changes in this design (the no-routing design owns them).
- No new maps, and no change to points, bases, rivers or bridges.

### Anti-goals (a failure even if it works)
- A house on a through-road, a deck or a point ring (the built-world rule).
- Houses that decide the match by themselves: a side that holds every house wins the point fight automatically.
- A house taken in seconds by one soldier while the owner never knew.
- The player has to stand in a menu in the middle of a melee to use a house (the shop works only in a safe house).
- A battle HUD crowded by house state (the v2 audit took the minimap and the busy strips out on purpose).
- A house layout that favours the Dawn or Dusk seat.

---

## 2. Constraints

- Minecraft 1.20.1, Fabric. Every number lives in `sheets/design.json`; `Spec.java` is generated by
  `tools/preflight.py` and never hand-edited; preflight must print `Preflight clean`.
- Houses are built-world pieces: vanilla structure files authored as `.mcfunction` scripts and exported by the
  Phase 3 `StructureStudio`, placed through the landmark pipeline (`landmarks` rows, one piece per tick, at most 100 ms
  each, at most 1.5 s per map). The built-world rules bind: no landmark on a through-road, a deck or a ring it does not
  enclose; odd width and depth, each at most 48, height at most 32; a piece whose footprint changes gets a new name.
- **Walkthrough gate:** every map with houses passes Phase 3's first-person walkthrough at 9 or better
  (`GRADING-RUBRIC.md` adapted to world pieces). Nothing under 9 ships.
- **Art:** player-facing art (the M map house glyph, the feed glyphs) reaches 9 on `work/ui-concepts/GRADING-RUBRIC.md`,
  in the approved War Table and Iron Ledger family. `ui-ux-pro-max` is used only for UX checks (contrast, hit targets).
- Colour-blind rules of the UI spec (section 6): every team colour travels with a shape.
- The arena keeps its coordinates: 700 by 500, Y 64, every point, base, bridge, river and road endpoint.
- Harness and checks live in `test-tools/`; their class names join the forbidden tuple in `tools/release_config.py`.
  Client and commit rules of `docs/HANDOFF.md` apply.
- A packet layout change bumps its channel id.

---

## 3. Where the houses stand

### 3.1 Count and layout **(Kort)**

Each half of the map (west of the river, east of the river) has two houses on the three-point maps: one on the north
flank and one on the south flank, between the base and the river, off the lanes. Kingsfield has one per half, because its
raised outposts (points B and D) already hold the other flank of each half.

Every house starts neutral **(Kort)**, so the opening is a race. Each side's near houses are its own half's, about 175
blocks from its base and 410 from the enemy's, so a side normally claims its own two first, and the match turns on who
raids whose.

### 3.2 Starting positions **(designer's call)**

Checked against the current geometry (`CampaignMaps.select` on `phase-3-built-world` and the landmark rows of Phase 3
Tasks 6 to 8): with a footprint of 31 by 37, every position below is clear of every road, deck, point ring (radius 17
plus 2 plus 6 blocks of margin), landmark footprint (4 blocks of margin), base piece, watchtower and the river (20 blocks).
Heights are the terrain at the centre today.

| Map | nw | sw | ne | se |
|---|---|---|---|---|
| 0 Rivergate | (-125,-72), h 76 | (-125,72), h 73 | (125,-72), h 75 | (125,72), h 72 |
| 1 Ironhold | (-125,-72), h 76 | (-125,72), h 78 | (125,-72), h 75 | (125,72), h 76 |
| 2 Frostmarch | (-125,-72), h 90 (see 3.3) | (-125,72), h 73 | (125,-72), h 88 (see 3.3) | (125,72), h 72 |
| 3 Hollowwood | (-125,-72), h 76 | (-125,72), h 73 | (125,-72), h 75 | (125,72), h 72 |
| 4 Kingsfield | (-125,-72), h 76 | none (outpost B) | none (outpost D) | (125,72), h 76 |

On maps 0 to 3 the north houses stand on the ridges (`<side>_ridge` terrain rows at x ±138, z -80) and the south houses
on the hills (`<side>_hill` at x ±130, z 100), so each side has one ridge house and one hill house. On Kingsfield both
houses stand on the ridges (x ±138, z ±80).

For scale: a north-west house is about 113 blocks from point A and 144 from B; a south-west house about 144 from B.

### 3.3 Placement rules for the builder

- **Half-turn symmetry.** Houses come in pairs that map onto each other by (x, z) to (-x, -z), as the points do:
  nw with se, sw with ne. Each pair has the same elevation (the mean of the two terrain heights, rounded). The
  builder may move a pair by up to 20 blocks to fit the ground, keeping the symmetry.
- **Frostmarch.** The ridge crest at the north spots is about 90 (its ridges are 28 high). Starting value: the nw/se
  pair and the sw/ne pair each sit at elevation 80, with the north houses set into the ridge as a terrace and the south
  houses on a raised plateau on the hill. If the walkthrough grades the terrace below 9, the builder moves the pair
  down-slope within the 20 blocks instead.
- Clearances as in 3.2. A spur road, 4 wide, runs from each house's front gate to the nearest lane or connector road,
  laid by `CampaignMaps.select` from the house's row. A spur that ends at a raised house climbs to it, as the Kingsfield
  outpost roads do (`Arena.roadLevel` treats a raised house like a raised point).
- The house's yard and its capture circle (radius 10, section 4) are flat, open and walkable from both gates.

### 3.4 Names **(designer's call)**

Each house has a name for the feed, the M map and deploy notices: unique on its map, saying west or east, in the map's
theme, at most 20 characters. Starting names (Kort may rename any):

| Map | nw | sw | ne | se |
|---|---|---|---|---|
| Rivergate | Westmill Granary | Westhill Barn | Eastmill Granary | Easthill Barn |
| Ironhold | West Armoury | West Quarters | East Armoury | East Quarters |
| Frostmarch | West Ridge Hut | West Fur Store | East Ridge Hut | East Fur Store |
| Hollowwood | West Woodstore | West Herb House | East Woodstore | East Herb House |
| Kingsfield | West Tithe Barn | none | none | East Tithe Barn |

Row ids are `<map prefix>-supply-<nw|sw|ne|se>` (prefixes `rg`, `ih`, `fm`, `hw`, `kf`, as Phase 3's landmark rows),
so they never clash with the hamlet's `rg-house-0` rows.

---

## 4. Capture **(Kort: slower than a point)**

- The capture circle is the yard: radius 10 around the yard centre.
- Presence counts as it does at points: each living troop of a team inside the circle counts 1; a seated,
  non-spectating commander counts `rules.capturePlayersWeight` (3). A bot's commander body counts as a commander.
- One team alone in the circle, not the owner: progress rises by 1 a second. At `supply.captureSeconds` (20) the house
  is theirs. This holds for a neutral house and an owned one alike.
- Both teams in the circle: the house is contested and progress freezes.
- The owner alone in the circle: progress resets to 0 (as at points).
- Nobody in the circle: progress falls by `supply.decayPerSecond` (1) a second **(designer's call: unlike points,
  so a raider cannot chip a house over several visits)**.
- A new challenger restarts progress from 0.
- **Warning.** When progress against an owned house first rises from 0, the owner's team gets the feed line
  "Dusk is taking West Armoury" (section 7.4), at most once per house per `supply.alertSeconds` (30).
- **Taken.** The house changes owner, its flag and braziers change (7.2), and everyone gets "Dusk took West Armoury".

The code shape is the builder's. `Capture` already holds owner, challenger, progress, contested and `ownedSince`; a
house can reuse it in a list of its own (`Battle.houses`), not in `Battle.points`, so the score loop, point income and
the ledger seals never see a house **(designer's call)**.

---

## 5. Services

A house serves the team that holds it, and only while it is **safe**: held, and no living enemy troop or commander
inside its circle. Every service below checks that, except where it says otherwise.

### 5.1 Deploy **(Kort: houses, not points)**

- New troops deploy at the barracks, at a held house, or at a standing Forward Supply banner. **Held control points stop
  being deploy spots.** `Battle.reinforcementAnchors(team)` lists the team's houses and its Forward Supply banners;
  the loop over `points` goes.
- A house is a deploy spot when it is safe, has been held for `economy.forwardHoldSeconds` (10) since it was taken, and
  no enemy is within `economy.forwardSafeRadius` (20) of its deploy centre. Those two columns now govern houses.
- The deploy centre is the yard centre; each slot stands in its ring place around it, as today. `forwardSpawnOffset`
  (the step toward the base at a point) no longer applies; the builder drops the column if nothing reads it.
- **Which spot.** A trained troop deploys at the spot nearest the current target of **its own group** (today the rule
  reads the infantry group's target for every troop). So a group that is regrouping at a house gets its new troops at
  that house **(designer's call, needed by 5.2)**. Where it was bought does not matter: training takes time, and the
  troop appears where its group needs it.
- Deploy notices name the house ("Dawn Knights deploying at West Armoury"), through the anchor's name.

### 5.2 Regroup **(Kort: a Regroup order)**

Regroup is a new order, not a morale state. It is the player's way to pull a beaten group out of a fight and rebuild it.

- **Giving it.** In the R council (`CommandMode`), for one group or the whole army, like the other orders. No new
  default key (the unit AI spec's key ruling). Aim: a house the team holds, picked on the field; with no aim, the
  nearest safe held house by straight distance from the group's centre, else the base. An aim at a house the team does
  not hold is refused with a notice ("You do not hold West Armoury"). While Regroup is aimed, the aim prompt names the
  house under the cursor, as the HUD names an aimed point under its seal **(designer's call)**.
- **Marching.** The group walks to the house's yard and fights nothing on the way (like Retreat: speed x1.15, loose
  cohesion, engage none).
- **Arriving.** Once the group's centre is inside the circle it holds there, and its order becomes a Hold at the yard. It
  heals by the yard rule (5.3), and its new troops deploy there (5.1). It waits for the next order.
- **If the house falls** while the group marches, the group carries on and holds there anyway; the player sees it on the
  card and the M map **(designer's call: no unasked change of order, which is the point of no routing)**.
- **Card.** The group card shows the state word "Regrouping" while it marches, then its Hold state.
- **When it lands.** Regroup lands with this design's rules plan, not with the no-routing change; until then Retreat
  pulls a group out **(Kort, in the no-routing brainstorm)**. In a mode with houses idle, or when the team holds no safe
  house, Regroup with no aim goes to the base.
- A new `orders` row `regroup` (section 10). The unit AI plan carries it into its new orders table with aim `house` or
  none, stance march, engage none, speed 1.15, cohesion loose, arrive stay.

### 5.3 Healing **(Kort: steady heal, base too)**

- Every soldier of the holding team inside a safe house's circle, and its commander there, heals
  `supply.healPct` (4%) of max health every second, once it has taken no damage for `supply.healDelaySeconds` (5).
  A full heal from near death takes about 25 s.
- The base heals the same way, within `supply.baseRadius` (40) of the team's base (`Battle.base(team)`, the muster and
  spawn; 40 covers the barracks and stores) while no enemy is within it **(Kort; the radius is the designer's call)**.
- Temporary troops (Grave Bloom's Risen) heal like the rest **(designer's call)**. A level-up heal and the armour-tier
  health of the progression design are unaffected; healing never raises health above max.
- Heal ticks every 20 ticks, with a small particle on each healed soldier every 2 s (heart, as the Sunweaver's mend),
  so a healing group reads from the field.

### 5.4 The shop: troops and gear **(Kort: "level up troops" means buying gear; designer's call: training too)**

- The barracks screen (training and the Armoury tab) works where the commander stands within `economy.accessRadius`
  (18) of his barracks, **or within the circle of a safe house his team holds**. `Battle.atBarracks(p, s)` widens to
  that rule; the gear command `gear:<troop>:<weapon|armour>:<tier>` and the training command check it on the server.
- The screen still opens anywhere by key (progression 5.1); elsewhere its buttons are greyed with "Buy at your barracks
  or a supply house you hold".
- The progression design's stopgap rule (5.3: buy near any reinforcement anchor) ends when this lands. Forward Supply
  banners are not shops (section 6).
- Bots ignore the location rule, as they do today for training and in the progression design.

---

## 6. The Forward Supply banner **(Kort: stays as it is)**

The Quartermaster's banner keeps every rule of the Houses spec section 5: placed at least 30 from the base, 40 health,
60 s, a deploy spot while no enemy is within 12. It does not heal, take Regroup orders or sell. Because points no longer
deploy, it is the only deploy spot that is not a house or the barracks, so the class gains value without any change.
`Anchor` keeps its shape; a house's anchor is `new Anchor(yardX, yardZ, name, owner)`.

---

## 7. Look and display

### 7.1 The building **(Kort: one shape, five dressings)**

One layout on every map, so a player knows a house at a glance; each map builds it in its own materials. Starting brief
for the piece scripts **(designer's call; dimensions and detail are the builder's within the built-world rules)**:

- Footprint about 31 wide and 37 deep, height at most 16. The storehouse fills the back third; a walled yard (a wall 2
  high) the front two thirds, with a 7-wide front gate facing the spur road and a 5-wide side gate, so the yard is never
  a dead end.
- The capture circle (radius 10) lies in the yard, paved as a ring so its edge reads on the ground. Its centre column is
  structure void up to 8 for the flag (as Kingsfield's outposts leave their point's flag column).
- In the yard: crates and barrels, a supply cart, a forge corner (anvil, grindstone, smithing table, a weapon rack) for
  the gear shop, and a covered corner of cots for the wounded. Two braziers (campfires) by the front gate.
- The flag pole rises above the roof line, so the holder's colour reads from about 60 blocks.
- A lookout on the storehouse gives the house a sight line over its spur road; the yard wall gives a cover line. Each is
  a `landmarkLines` row checked by raycast, as the other landmarks are.
- Dressings: Rivergate oak frame on a cobble plinth with a hay roof; Ironhold stone brick, iron bars, deepslate roof;
  Frostmarch dark spruce and stone with snowy roofs; Hollowwood mossy cobble and spruce logs; Kingsfield fieldstone and
  oak with wheat sheaves. Piece ids `supply/<map>`, all five with the same footprint.

### 7.2 In the world

- The flag over the yard is the points' flag (`Arena.flag`): white neutral, cyan Dawn, purple Dusk.
- The braziers are lit while a team holds the house and out while it is neutral.
- **Capture progress shows as a rising flag.** While a challenger takes the house, the pole fills from the bottom in the
  challenger's colour, one block for each sixth of `captureSeconds`, and drains back as progress decays or resets; at
  20 s the whole flag is theirs. So a raider in the yard and a defender arriving see how far it has gone without
  opening M, and nothing is added to the HUD **(designer's call: today capture progress shows only on the M map; points
  keep their flag as it is)**.

### 7.3 The M map **(Kort)**

`MapScreen` (the M map) draws each house as a house glyph (a small gabled outline), with the holder's shape inside: the
round sun for Dawn, the diamond and crescent for Dusk, the split square with swords when contested, a bare square when
neutral (the UI spec's colour-blind shapes). Capture progress shows around the glyph, as it does for points on the M map.
The house's name shows on hover. The glyph is pixel-authored at scale 2 and graded 9.

### 7.4 The feed **(Kort)**

Two new kinds in the UI plan's feed (`FeedEvents`, `FeedLines`), following its rule that the seal means ours and the
struck seal means theirs:

| Event | Who sees it | Glyph | Ink | Copy |
|---|---|---|---|---|
| A house is taken | everyone | house seal (ours) or struck house seal (theirs) | the taker's team ink | "Dusk took West Armoury" |
| Your house is being taken | the owner's team | struck house seal | the attacker's team ink | "Dusk is taking West Armoury" |

The first capture of a neutral house uses the first line. A new kind changes the feed packet's layout only if the
builder adds fields; then it bumps.

### 7.5 The HUD **(Kort: nothing new)**

No house seals, pips or strips on the battle HUD. The ledger bar, the point seals and the group cards stay as they are,
except the group card's new state word "Regrouping" (5.2).

### 7.6 Network

The client needs each house's owner, challenger, progress and contested flag (for the M map and the world), as STATE
already sends for points. Houses ride in the state packet as their own list; the channel takes the next free id. The
builder picks the layout **(designer's call)**.

---

## 8. Bots **(Kort: scaled by difficulty)**

Bots use the services as players do: they deploy at held houses through `reinforcementAnchors`, heal in yards, and shop
without the location rule. What changes is what they decide, in `Campaign.botDecide` today (per-group assignments
already exist there) and in the unit AI General later.

| | Easy | Normal | Hard |
|---|---|---|---|
| Claim | At the start, sends one group to claim its own half's neutral houses, one after another, then that group rejoins. | Same. | Same. |
| Defend | Takes a lost own house back only when its army's centre is within `houseDefendRange` (60) of it: the house becomes its point choice. | When its house is being taken and lies within 120 of its army, sends its nearest group. | Answers every attack on its houses within 250: sends the nearest group, and a second if the attackers it has seen there outnumber the first. |
| Raid | Never. | When it holds at least one point and has a spare group (four or more living troops, not the claim group), sends it to the nearest enemy-held house with no enemy seen within 30 of it. One raid at a time. | Raids when the enemy army's seen centre is at least `raidEnemyDistance` (100) from the target house, with its best raiding group (cavalry first). |
| Regroup | Never (retreats to base, as today). | Never. | A group under `regroupShare` (35%) of its full strength regroups at the nearest safe held house when that is nearer than its base; a whole-army retreat goes to a house the same way. |

Further rules **(designer's calls)**:
- The claim group is the group with the fewest living troops among infantry, ranged and cavalry, cavalry first on a
  tie (fast and cheap to spare). Two bot banners on one team never claim or raid the same house: one a teammate is
  already heading for is skipped.
- House decisions (defend, raid, regroup) read only what the bot's team has seen (the `sightings` memory) at every tier,
  even though Easy and Normal read the whole field for their other choices today; a raid is never aimed by knowledge a
  player would not have.
- A claim, defend or raid mission belongs to its group until it ends: the house is taken, the house is lost, the group
  falls under four living troops, or a claim makes no progress for 60 s. Today `botDecide` clears every assignment each
  time it runs (every 60 to 160 ticks); it keeps a per-squad mission list and re-applies each live mission as that
  group's assignment instead. The shape is the builder's.
- New `botCommand` columns: `houseClaim` (true, true, true), `houseDefend` (false, true, true), `houseDefendRange`
  (60, 120, 250), `houseRaid` (false, true, true), `raidEnemyDistance` (0, 0, 100), `regroupShare` (0, 0, 0.35).
- When the unit AI lands, houses become General objectives (claim, defend, raid: the "raids" objective kind it already
  names), and these columns become its tier rows. Doctrines may use houses (a Tidecallers raid may target the least-held
  house a ford reaches).

---

## 9. Modes **(Kort: Domination and Banner)**

| Mode | Houses |
|---|---|
| Domination (Practice, Skirmish, campaign battles) | Work as this design says. |
| Banner on Rivergate (Phase 4) | Work as in Domination. Point B keeps its point income but stops deploying (the Phase 4 spec named it "the only forward anchor"; houses take that job). |
| Siege on Ironhold (Phase 4) | Idle: the buildings stand, flags down, braziers out, no capture, no services. Siege's stages and field bases carry deploy. |
| Survival on Hollowwood (Phase 4) | Idle, as in Siege. The shrine court is the defenders' field base. Rise sites keep out of house footprints. |

The Phase 4 `modes` sheet gains a `supplyHouses` column (true for domination and banner). Until Phase 4 exists, only
Domination runs, so houses are always on. A mode with idle houses lets Regroup with no aim go to the field base.

---

## 10. Data

Column names and sheet shapes are the builder's, within these rules. Values are starting values.

- **`supply`** (new, one row): `enabled` (bool, default true), `radius` 10.0, `captureSeconds` 20, `decayPerSecond` 1, `alertSeconds` 30, `healPct` 0.04,
  `healDelaySeconds` 5, `baseRadius` 40.0.
  `enabled` false switches off every part of this design, for the gate's baseline: no house capture (flags stay white,
  braziers out), no house deploys, heals or shops, no base heal, Regroup only to the base, bots ignore houses; held
  points deploy again (Phase 1 forward deploy) and the shop follows the progression stopgap (5.3). The buildings stand,
  as Phase 3 leaves them.
- **`supplyHouses`** (new, 18 rows): `id`, `map`, `name`, `landmark` (its `landmarks` row id), `yardX`, `yardZ` (world
  coordinates of the yard centre), `elevation`.
- **Phase 3 sheets:** `pieces` gains `supply/rivergate`, `supply/ironhold`, `supply/frostmarch`, `supply/hollowwood`,
  `supply/kingsfield`; `landmarks` gains the 18 house rows (`replaces` and `encloses` `-`); `landmarkLines` gains a
  cover and a sight row per piece.
- **`economy`:** `forwardHoldSeconds` and `forwardSafeRadius` now govern houses; `forwardSpawnOffset` drops if nothing
  reads it.
- **`orders`:** new row `regroup`.
- **`botCommand`:** the six columns of section 8.
- **Phase 4 `modes`:** `supplyHouses` (bool).

Preflight rules: every `supplyHouses` row names a `landmarks` row on the same map whose template starts `supply/`; its
yard centre lies inside that footprint; names are unique per map and at most 20 characters; maps 0 to 3 have four rows
and Kingsfield two; every row has a partner on its map at (-yardX, -yardZ) with the same elevation; the `supply` numbers
are positive and `healPct` is at most 0.25.

---

## 11. Checks and the balance gate

### 11.1 Checks **(designer's call)**

A harness suite (`SupplyChecks`, ids `sh-*`, in `test-tools/`, on the forbidden list) proves each rule in a running game,
by log line and screenshot as pillar 5 asks:
- capture: 20 s alone, freeze when contested, reset by the owner, decay when empty, the commander's weight, the warning
  line once per 30 s, the taken line;
- deploy: a held point no longer deploys; a safe house does after 10 s; a contested or unsafe one does not; a troop
  deploys at the spot nearest its own group's target;
- healing: 4% a second after 5 s undamaged, stopped by an enemy in the circle, at the base within 40;
- shop: training and gear only at the barracks or a safe held house, greyed elsewhere with the reason;
- Regroup: marches without fighting, holds on arrival, refills there, refuses an unheld house, goes to base with no house;
- bots: claim at the start on every tier; Normal defends and raids; Hard raids while the enemy army is away and
  regroups at a house;
- display: screenshots of a neutral, a Dawn and a Dusk house (flag and braziers), a house half taken (the rising flag),
  the M map glyph in each state, and both feed lines.

### 11.2 Balance gate **(Kort: bot-sim gate)**

The rules plan ends with bot sims at full 900 s rounds on the merged base, levels on in both conditions: **A**, houses
off (`supply.enabled` false, section 10), and **B**, houses on. If the progression re-gate (progression 9.2) has not run
by then, it runs on the same base and adds **C**, levels off with houses off; A is then shared, as the progression
gate's levels-on condition and this gate's baseline. So the shared design runs three conditions, never two
**(designer's call, from the builder check)**.

Noise sets the checks. At 20 matches a win share wobbles by about 11 points, and the shift between two runs by about 16,
so a single 20-match run cannot tell a 10-point shift from luck. Each check therefore re-runs before it fails
**(designer's call, from the builder check)**:
- **House shares.** Each of the five pure normal banners against the current best pure banner (whose own row is a
  mirror), 20 matches (10 per seat), each match on the next map in rotation (4 per map). A House passes when its share
  with houses on is within 10 points of its own baseline share. A House outside that gets 20 more matches in both
  conditions and passes when its 40-match shift is within 15 points (noise about 11); only a House that still misses
  fails. As in the progression gate, absolute House balance (40 to 60%) belongs to Phase 2's balance pass; a House
  outside it in both conditions is reported, not tuned here.
- **Seat.** Mirror matches of the current best pure banner on both seats, 20 per map (10 per seat), 100 per condition.
  Pooled over the five maps (noise about 5 points), the Dawn seat wins 40 to 60% with houses on, or is no further from
  50% than the pooled baseline (the sims already show a Dawn-seat lean; houses must not add to it). Per map the share is
  reported; a map outside 30 to 70% with houses on but not in the baseline gets 20 more matches, and if it is still
  outside, its house positions are a tuning item.
- **Houses change hands.** The sims log every capture. Pass: at least one steal (an owned house taken by the other
  side) in at least half of the matches, and at most 3 steals per house per match on average **(designer's call on
  both numbers)**.
- **Comebacks.** The share of decided matches won by the side that trailed at 300 s, pooled over all matches of a
  condition. Pass: with houses on it is at least the baseline minus 5 points **(designer's call on the margin)**.
- **Cost.** 400 matches for A and B (200 each), about 60 hours of machine time at the progression gate's rate, plus the
  re-runs above, run in the background two headless servers at a time (they do not count toward the client limit).
  Sharing A with the progression gate saves its 200 matches there **(Kort: "runs alongside the progression re-gate
  where the timing lines up")**.
- **Stop rule.** Two tuning rounds. A tuning round changes only house numbers, so the baseline runs are kept and a round
  re-runs condition B for the failed checks only. Whatever still fails goes to Kort in the questions file with the
  numbers.
- **Tuning order:** `captureSeconds`, `healPct`, `forwardHoldSeconds`, the bot house columns, then house positions
  (within the 20 blocks of 3.3, which sends the moved pieces back through the walkthrough).

---

## 12. Timing, branches and plan changes **(Kort: buildings now, rules in 0.5.0)**

### 12.1 Buildings: Phase 3, now

A new **Phase 3 Task 8b, "Supply houses"**, after Task 8 (Frostmarch and Hollowwood) and before Task 9 (regression and
Kort's gate), on `phase-3-built-world` **(designer's call on the task's place; the plan writer may fold it into Tasks 6
to 8)**:
- five scripts `supply/<map>` to the brief of 7.1, exported and copied as the other pieces;
- the `pieces`, `landmarks` and `landmarkLines` rows, and the `supplyHouses` sheet (data only);
- the spur roads in `CampaignMaps.select` and the raised-spur climb in `Arena.roadLevel`;
- each house's flag raised white (neutral) at its yard centre, so the maps look finished before the rules exist; the
  braziers stay out;
- the landmark checks for every row (`p3-lm-<id>-placed`, `-fits`, `-clear`, `-roads`, `-ring`, `-cover`, `-sight`,
  and the per-map budget), plus `p3-house-<id>-yard` (the circle flat, open, reachable from both gates) and
  `p3-house-routes-map<N>` (20 knights reach every yard from their base and from the nearest point);
- the `routes` suite still 20/20 on all 15 crossings, the walk scan at zero blocked road cells;
- the first-person walkthrough of each map's houses (spur road, gate, yard, lookout, and from the nearest point), graded
  9 or better.

Task 9's full run, release count of structure files (33 becomes 38) and contact sheets include the houses.

### 12.2 Rules: their own plan, before 0.5.0 ships

The supply house rules (sections 4 to 9 and 11) are their own plan, run on the merged base after the base merge
(Phase 2 through Task 13, Phase 3 through Task 9 with 8b, the UI plan through Task 9, and the progression plan's server
parts already on `phase-2-houses`) and before 0.5.0 ships. 0.5.0 already waits for every troop's gear tier art
(progression 4.4), so this plan runs in that wait. If it is not done when the art is, Kort decides whether 0.5.0 waits;
the progression stopgap (5.3) keeps the game whole without it.

**When each plan is written (designer's call, from the builder check).** Task 8b goes into the Phase 3 plan now, at
file-and-line level: its code names exist on `phase-3-built-world`. The rules plan is written after the base merge,
when the merged code exists (`Battle`, `Capture`, the gear command, the feed kinds and the landmark pipeline live on
different branches today), at file-and-line level, as the Phase 4 plan waited for Phase 2's code names.

### 12.3 Changes to other plans (a list; this design does not edit them)

- **Phase 3 plan:** Task 8b above; Task 9's counts and sheets.
- **In-match progression plan:** none to its tasks. Its 5.3 stopgap ends when the rules plan lands; its 9.2 gate may
  share runs with 11.2.
- **UI plan:** none to its tasks. The rules plan adds the M map glyph and the feed kinds on the merged UI code, and the
  "Regrouping" card word.
- **Phase 4 plan:** the `modes.supplyHouses` column; idle houses in Siege and Survival (flags down, braziers out, no
  capture); Banner's B no longer deploys; Survival rise sites outside house footprints; Regroup with no aim goes to the
  mode's field base.
- **Unit AI plan:** house yards join the flow-field sites (four more fields, about 2.8 MB more at 2 bytes a cell); houses
  join the General's objectives; the `regroup` order joins its orders table (5.2); the bot house columns become tier
  rows.
- **No-routing design** (`2026-10-22-no-routing-design.md`): it needs nothing from this one; Retreat covers pulling a
  group out until Regroup lands here. Until no routing lands, routing groups still run to the base as today; houses
  never take routing troops.

---

## 13. Left to the builder

- Sheet and column names, within section 10.
- The piece scripts' exact dimensions, materials and detail, within 7.1 and the built-world rules; spur road routes.
- House positions within 20 blocks of 3.2, keeping half-turn symmetry and the walkthrough at 9.
- The code shape: houses as `Capture` objects in their own list, the anchor list, the per-group deploy rule.
- The state packet layout for houses, the M map glyph's pixels, the feed glyph pixels, within the rubric.
- Particle and sound choices for healing, within the readable-drama pillar.
- Bot group choice details within section 8.
- Tuning of every number in sections 4, 5, 8 and 11 by the sims, inside the gate.

---

## 14. Decisions in this document

**Kort's answers (brainstorm, 2026-10-21):**
1. Two houses in each half; Kingsfield one per half beside its outposts.
2. Every house starts neutral.
3. Troops deploy at houses, the barracks and Forward Supply banners; control points no longer deploy.
4. Houses give services only: no score, no gold.
5. Capture is slower than a point: a 10-block yard, 20 s, troops 1 and a commander 3, frozen when contested, a warning
   to the owner.
6. Routing goes away for everyone, in its own design and session, started now; this design assumes it.
7. Regroup at a house is an order: march, hold in the yard, heal, refill.
8. Healing 4% of max health a second after 5 s undamaged, at houses and at the base.
9. The Forward Supply banner stays as it is.
10. One house shape on every map in five dressings.
11. The holder shows in the world (flag, braziers), on the M map and in the feed; nothing new on the HUD.
12. Bots scale by difficulty: all claim; Normal defends and raids; Hard raids while you are busy, answers every attack and
    regroups.
13. Houses work in Domination and Banner, idle in Siege and Survival.
14. Buildings in Phase 3 now; the rules in their own plan after the base merge, before 0.5.0 ships.
15. A bot-sim gate, houses on against off.

**Designer's calls (Kort may overturn):** commanders respawn at the base, not at houses; houses are not structure
targets; the training shop at houses (with gear); the starting positions and the Frostmarch terrace (3.2, 3.3); the house
names (3.4); progress decay of 1 a second when nobody is in the circle; houses in their own list, outside the score loop;
the deploy rule reads each troop's own group's target; Regroup holds on arrival even if the house fell on the way; the
base heal radius of 40; temporary troops heal; the heal particle; the piece brief (7.1); the state packet carries houses
as their own list; the bot claim group and the six `botCommand` values; the check list (11.1); the steal-count and
comeback margins of the gate; Phase 3 Task 8b as the buildings' task. From the builder check: the rising flag shows
capture progress in the world (no HUD element); the Regroup aim prompt names the house; bot house decisions read
sightings only at every tier, and bot house missions persist across decisions; `supply.enabled` false switches off
the whole design for the baseline; the gate's conditions (A, B, and C when shared) and its re-run-before-fail rule; Task
8b is planned now, the rules plan after the base merge.
