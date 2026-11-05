# Graves and Glory: vision and roadmap

Version 1, 2026-10-17. Written from Kort's answers in the orchestrator session.
Replaces the Codex six-milestone checklist as the forward plan; that checklist
stays as a reference for packaging and hosting detail. Current state of the
project is in `HANDOFF.md`.

## 1. The one-paragraph pitch

You are a commander in a war-scarred fantasy land where the dead do not stay
buried. Build a banner of troops from five rival Houses, fight through a solo
campaign of hand-built battles, then bring that banner to your friends. Every
battle is loud and readable: charges, arrows in the air, lines that
hold to the last. Progress is a long, earned chase: recruit, rank up, evolve,
collect. Grim world, heroic moments. Minecraft 1.20.1, Fabric.

## 2. Decisions Kort made today

| Topic | Decision |
|---|---|
| Next goal | Make it feel great solo. Then bring friends in. Public later. |
| Visual bar | Hand-built world (camp, landmarks) plus custom UI art. Claude builds structures and draws pixel UI/portraits. |
| Commander | Both: hero classes that fight and general classes that command. Player chooses. |
| Progression | Long chase: ~30 troops, rarity tiers, evolve lines, duplicates rank units up. Earned only, no real money. |
| Factions | Drop Dawn vs Dusk. Houses you mix freely with set bonuses. Teams are just banner colours. |
| Tone | Grim but heroic. |
| Modes | Domination, Siege (attack/defend), Survival waves (solo/co-op), Capture the banner / escort. |
| Solo | Campaign and skirmish from the start; campaign reuses skirmish maps. |
| Workflow | Claude orchestrates alone, owns the repo. Codex is out unless handed a task doc. |
| Melty | Dropped. Modrinth/Prism first, CurseForge second. Leave the 0.2.0 listing alone for now. |

Follow-up answers (same day, after the builder check):
- Progression power applies everywhere but stays small: rank stars and
  evolutions together cap at about +12% over base. Tactics beat grind.
- Standing permission: tester subagents may launch isolated test clients any
  time Kort is not playing (check for his game process first). Build from the
  proposal tables; Kort reviews at phase gates and can redirect.
- Friends connect over Tailscale node sharing (Phase 5). No relay for now.

Builder's calls made by the orchestrator (flagged, not Kort's words):
- An evolve stage is a state of one troop id (name, stats, overlay change),
  not a new troop. Epic and Legendary units do not evolve. "~30 troops" means
  30 base ids.
- A match has two sides. The four banner colours are per-side picks, not
  four-team play.
- A new profile starts with every House's Commons. Rares come from
  recruitment, the Legendary of each House from campaign chapter completion or
  mastery. Migration: Dawn troops map to Gilded Order equivalents, Dusk to
  Graveborn, rank and gold carried over.
- Phase 4 modes retrofit existing maps (Siege on Ironhold, Survival on
  Hollowwood, Banner on Rivergate). Purpose-built fortress and shrine maps are
  Phase 6 content, not Phase 4.
- All troop skins and House readability must work on vanilla textures with the
  modpack's own assets; it ships no third-party resource packs (Kort,
  2026-10-18). History: this line used to add Faithful as a second test case and
  keep Excalibur optional and by-reference.

Standing rules that still apply: never publish without Kort's go; never hand-
edit `Spec.java`; harness stays in `test-tools/`; no game launches while Kort
is gaming; park cut ideas, never delete them.

## 3. Design pillars (how to decide anything)

1. **Readable drama.** If a player cannot tell who is winning a fight by
   looking, the fight is wrong. Arrows fly, lines hold or fall, and every order
   is given from the screen.
2. **Your banner is yours.** Army composition is the main expression. Houses,
   ranks, evolutions and cosmetics all show on the field.
3. **Built, not generated.** The camp and every landmark are authored
   structures with a reason to exist. Procedural fill is allowed for terrain
   and forests only.
4. **Solo is a real game.** Bots and campaign must be worth playing alone.
   Multiplayer is the same game with friends, not a different game.
5. **Honest numbers.** Nothing is claimed done until seen in a running game by
   screenshot. Balance claims need bot-vs-bot data.

## 4. The Houses (replacing Dawn and Dusk)

Five Houses, about six troops each, four rarity tiers. Any troop in any banner.
Fielding 3, 5 or 7 troops of one House turns on that House's trait at rising
strength. Teams in a match are banner colours (gold, violet, crimson, teal),
not Houses. Names and traits below are the proposal; the first implementation
task is a roster sheet Kort signs off.

| House | Fantasy | Playstyle | Trait (3 / 5 / 7) | Base mobs |
|---|---|---|---|---|
| **Graveborn** | the risen dead, grave wardens, bone casters | cheap, numerous, slow, fearless | *Rise Again*: fallen Graveborn stand back up once at 30% HP (10% / 25% / 40% chance) | zombie, husk, skeleton, stray, phantom |
| **Gilded Order** | knights, templars, crossbowmen, oathbound paladins | expensive, armoured, shields, holds ground | *Bulwark*: shield troops take less ranged damage and troops near a Gilded unit cannot be flanked (15% / 30% / 45%) | zombie/husk with player-skin armour, skeleton crossbow, iron golem (paladin) |
| **Wildmoot** | beast-folk hunters, wolves, hidebound archers, a horned alpha | fast, ambush from woods, weak in the open | *Pack Hunters*: bonus damage when three or more Wildmoot attack one target or strike from the flank | wolf, stray (hunter), spider (stalker), ravager (alpha) |
| **Ironclad** | dwarven sappers, shield-breakers, ballistae, a mortar | slow, heavy, siege, strongest when still | *Entrench*: troops holding on an objective gain armour and the ballista range grows | piglin brute, zombified piglin, iron golem variant, custom ballista entity |
| **Tidecallers** | drowned raiders, trident guards, mist callers, a kraken-touched brute | mobile along water, slow enemies, mist | *Riptide*: Tidecallers move faster in water and rain; enemies hit by them are slowed | drowned, guardian (as a caster), tridents |

Rarity: Common (starter), Rare, Epic, Legendary. One Legendary per House is a
named champion with a unique ability (for example the Gilded *Lord Marshal*
plants a banner that holds the line). Rarity sets cost and training time, not raw
power per gold. Legendaries are strong but take a lot of a banner's budget.

Evolve lines: three stages for Common and Rare units (Squire -> Knight ->
Knight-Captain). Evolving costs House shards earned from matches and
contracts. Duplicates from recruitment give rank stars (max 3). Stars and evolutions
together never exceed about +12% over base stats, in every mode. This is the
"long chase" with a visible and small ceiling.

Why this fits the game: Houses let the collection be large without making
sides unbalanced, and set bonuses give a reason to collect deep in one House
and a reason to mix. Every House has a readable silhouette on the field.

## 5. The commander

Two tracks, six classes at launch, one active ability each on a long cooldown
plus one passive. The player picks a class in camp.

Hero track (you fight):
- **Champion**: sword and shield. Active *Stand Fast*: nearby troops take
  less damage for 8 s.
- **Reaver**: two-hander, fast. Active *Bloodrush*: speed and lifesteal, and
  your kills refund a reinforcement.
- **Warden**: spear and bow hybrid. Active *Mark Prey*: a target takes extra
  damage from all your troops.

General track (you command):
- **Marshal**: Active *Muster*: all groups march to you,
  faster; passive faster training.
- **Warlock**: Active *Grave Bloom*: raises fallen troops in an area as
  temporary Graveborn; passive casters cost less.
- **Quartermaster**: Active *Forward Supply*: drops a supply banner that acts
  as a reinforcement point; passive more gold from objectives.

Mounts are parked for a later phase (they interact with the formation code).

## 6. Battles

Keep the command layer (R council, orders, formations, M map). Add the things
that make fights readable and dramatic:

- Visible projectiles with short travel, team-coloured impact flashes.
- No routing: troops never break. A group fights where it is sent until it
  falls or its commander pulls it out. Flank and rear blows land harder. A
  group under a quarter strength shows LAST STAND. Every command is given from
  the screen; no horns sound in the field, and a command you give may answer
  with a horn sound to you.
- Clear counters shown on troop cards: shields vs arrows, charge vs archers,
  spears vs beasts, siege vs structures.
- End-of-match report: kills, time on objectives, gold, troops lost, House
  trait uptime.
- Forward reinforcement from held objectives (fixes the one-minute walk).

Modes:
- **Domination** (exists): hold objectives to 300. Tune.
- **Siege**: defenders hold a fortress with gates and walls that Ironclad can
  break; attackers take the outer yard, then the gate, then the keep. Uses
  Ironhold Siege first.
- **Survival**: 1 to 4 players hold a shrine against timed waves of Graveborn
  that scale; between waves you spend wave gold. The campaign's horror beat.
- **Banner**: carry an enemy banner home, or escort a supply wagon along a
  road while the enemy harasses.

Bots: three difficulties. Hard bots flank the least-defended point, hold
braced shields against archers, and time their charges.

## 7. Solo: campaign and skirmish

Skirmish: any map, any mode, any difficulty, any banner. Rewards as normal.

Campaign "The Long March": chapter 1 is six battles across the existing maps,
each introducing one House and one mode, with a short briefing card and a
reason to be there (a road to open, a shrine to hold, a fortress to retake).
Battle 1 is the tutorial. Each battle has one bonus objective that pays a
shard. Finishing a chapter unlocks a Legendary recruitment. Chapters 2 and 3
are content for later, not promises.

## 8. Progression economy (replaces the 600-gold collection)

- **Gold** from matches (completion, win, objectives, bonus objectives) and
  contracts. Spent on recruitment.
- **Recruitment** by House: 100 gold rolls one troop from that House's pool
  weighted by rarity; duplicates become rank stars. 300 gold buys a chosen
  Common or Rare outright. Legendaries come from chapter completion and
  mastery, never from rolls alone.
- **House shards** from contracts, campaign bonus objectives and dismantling
  duplicates past three stars. Spent on evolutions.
- **Chapters** (the existing Founders chapter machinery): keep the server-
  owned ledger and claims; re-author the 30 tiers around shards, titles,
  banner cosmetics and one Rare troop. Keep "never expires".
- **Mastery** per troop: kills and matches survived unlock a cosmetic variant
  and a title. This is the long tail after the roster is complete.

Model it in a sheet before shipping numbers: target 25 to 40 hours to a full
roster at one star, with a visible next goal at every point.

## 9. The world: built, not generated

- **Camp**: rebuild the courtyard as a hand-built war camp: a ridge with the
  commander's tent and war table, the Houses' recruiter tents in a half-ring
  with their banners and a live display of that House's troops, a graveyard
  shrine for the Graveborn, a training yard that is also the tutorial, the War
  Gates facing the map. Built once as a structure file, placed by the mod.
- **Battlefields**: keep the five layouts and the dressing. Replace the
  generic structures with authored landmark structures per map (mill, gate,
  lodge, chapel, outposts), each with cover and sight lines meant for the
  fights that happen there. Fix the known road blockers (railings, Ironhold
  gate, Hollowwood log pile).
- **New maps** (a purpose-built fortress, a shrine in the woods) come after
  Phase 4 proves the modes on retrofitted existing maps.
- **UI**: original pixel icons for Houses, troops, orders and formations;
  troop portraits; a painted-looking but pixel-authored frame set in the
  existing charcoal and brass language. Marcellus headings stay.
- **Skins**: each troop gets an ETF/EMF skin and overlay so Houses read at a
  distance. The armory pack already proves the pipeline.

## 10. Platform and release

- Stay on Minecraft 1.20.1 and Fabric. No loader change.
- Drop the Melty recipe from the build (keep `melty.json` only in git history).
- Release channel: Modrinth `.mrpack` and Prism import first (already works),
  CurseForge export second. The modpack ships no third-party resource packs
  (Kort, 2026-10-18), so no pack needs referencing by platform ID. History:
  Excalibur, Clarent and the Fresh Animations patch were the known cases.
- Dedicated server kit stays and is the friends-phase host on tjepkema-server.
- Versioning: 0.4.x for the solo rework, 0.5.x when friends join, 1.0 at
  public.

## 11. Roadmap

Phases are ordered by Kort's priorities: solo feel, then friends, then
public. Each phase ends with a Kort playtest and a screenshot-verified build.

### Phase 0: hygiene (this week)
- Checkpoint commit done (cea7968).
- Retire the two-copies setup: `bannerbound-test` becomes a git worktree.
- Make the self-test and route harness match current code (intro delay,
  camp lobby), so tests mean something again.
- Remove the Melty recipe from packaging; keep Modrinth export.
- Verify 0.3.7 in a real dev client: camp, pads, party, room, practice.

### Phase 1: the solo core feels right (3 to 4 weeks)
- Combat readability: projectiles, impact flashes (morale, rout and battle horns were built in Phase 1 and removed by the no-routing design).
- Forward reinforcement from objectives; measure the walk.
- Bot difficulty tiers; bot-vs-bot simulation harness for balance numbers.
- Tutorial verified in game and rewritten as campaign battle 1.
- End-of-match report screen.
- HUD and camp UI cleanup to the charcoal/brass standard; colour-blind glyphs.
- Playtest gate: Kort plays three skirmishes and says it feels good.

### Phase 2: Houses and the long chase (4 to 6 weeks)
- Roster sheet (30 troops, stats, costs, mobs, abilities) for sign-off.
- Data model: Houses, rarity, evolve lines, rank stars, shards; migration from
  Dawn/Dusk profiles.
- Implement troops in batches of one House at a time, each with skins,
  portraits, cards and a bot test.
- House traits and counters on the field.
- Recruitment, evolve and mastery screens; chapter re-authored.
- Commander classes with actives (six).
- Playtest gate: a full roster can be collected and a mixed banner feels
  different from a pure one.

### Phase 3: the built world (3 to 5 weeks, overlaps Phase 2)
- Hand-built camp structure and placement.
- Landmark structures for the five maps; road blocker fixes.
- Pixel UI icon set and frames.
- Playtest gate: screenshots of camp and each map match the tone.

### Phase 4: modes and campaign chapter 1 (4 weeks)
- Siege on Ironhold, Survival on Hollowwood, Banner on Rivergate.
- Campaign chapter 1: six battles with briefings and bonus objectives.
- Playtest gate: Kort finishes chapter 1 in one sitting and wants chapter 2.

### Phase 5: friends (2 to 3 weeks)
- Home server deployment on tjepkema-server over Tailscale; Starlink
  reachability answered (relay if needed).
- Parties, rooms, pads, disconnect handover verified with real humans.
- Modrinth/Prism package friends can install in one step.
- Playtest gate: a 3v3 with friends, nobody needs Kort's help to join.

### Phase 6: public (later)
- CurseForge listing, public server, matchmaking, moderation. The Codex
  checklist milestones 5 and 6 cover this and stay parked until Phase 5 holds.

## 12. How work gets done

- One orchestrator session (this one) owns the plan, the repo and the gates.
- Each task goes to a subagent sized to it: Opus 5.5 medium for design-heavy
  code, Sonnet 5.5 extra-high for ordinary implementation, Haiku 5.5 max for
  runs, screenshots and checks.
- Every feature task ends with a screenshot or log from a running game, taken
  by a tester subagent, before it is called done.
- Kort plays at phase gates and whenever he wants; nothing launches while he
  is gaming.
- Cut ideas go to `docs/PARKED.md`, never deleted.

## 13. Left to the builder

Exact stat numbers, mob choice per troop where the table says "variant",
structure dimensions, ability cooldowns, shader of the camp lighting, and the
order of troops inside a House batch. These are the builder's call within the
pillars above.
