# Prop kit: large living props for builders and the game

Status: design, agreed with Kort in chat 2026-11-08. Not yet planned or built.

## Intent

Builders on the Kinetic builder server (and later every map) need props that make G&G places feel crafted: a blacksmith's forge you
stop and look at, not a furnace next to an anvil. Props are not limited to one block. A forge can be about 4 blocks, a stall as big as it
needs. The first batch is the blacksmith set. Success: a builder walks into a smithy built with the kit and it feels alive and
unmistakably G&G, and the same smithy shows up unchanged when that map is played in a battle or the Old Hold hub.

## Decided by Kort

- **Flagship: the blacksmith forge**, about 4 blocks, **alive**: glowing coals that light the area, rising smoke, the odd spark, a
  crackling fire sound.
- **First batch:** the forge plus the **smithy set**: anvil, weapon racks, shield racks, quench trough, grindstone bench.
- **Art style matches the troops:** chunky 16 px-per-block pixel textures, so props blend with vanilla blocks and the G&G troop models.
- **Placement through a prop menu:** open a menu, pick a prop, a ghost preview follows in front of the player, scroll to rotate, click
  to place, hit it to remove.
- **Props appear in the real game too:** they are saved with a map and appear when the map is used in battles and the Old Hold hub.
- **Start now on 1.20.1**; do not wait for the 1.21.1 port.
- **Solid like walls in battles** (Kort 2026-11-08): troops path around props, arrows and projectiles hit them, and map checks
  (`MapDressing.free`, bot routes) treat a prop's footprint as solid blocks.
- **One builder-pack reinstall is accepted** (Kort 2026-11-08): props ship on current `main` (0.5.1+), and the builder server and
  builder pack move to that jar together, once. After that, adding props must need only a server resource-pack update.

## Goals

- Props of any footprint, solid where it matters (players do not walk through the forge).
- Vanilla-client players (planned plain-client support, 1.21.1 plan P2/P4) see the same props, effects and sounds as modpack players.
- Props survive the whole map pipeline: builder world -> mapforge export -> game map load.
- Light enough on the server for a hub copy with dozens of props.

## Non-goals (this design)

- Market stalls, war camp, battlefield and hall props (later batches on the same system).
- Interactive props (a forge that repairs gear, a usable grindstone). Props are scenery for now.
- Custom registered blocks.

## Anti-goals (a failure even if it works)

- Prop content that adds registry entries the client must know. Apart from the one accepted jar update, new or changed props must
  never force a builder-pack reinstall ("unknown registry entries", seen 2026-11-08). So no custom blocks, block-entity types or items.
- Props that look smooth, high-res or "AI-rendered" next to the troops.
- Props that disappear on export or in battles.
- Effects that cause lag spikes or run when no one is near.

## Constraints and facts

- Minecraft 1.20.1 Fabric; display entities (`block_display`, `item_display`) exist since 1.19.4.
- A single item model's elements are limited to the -16..32 range (3 blocks); larger props need display scale or several parts.
- The map pipeline (`tools/mapforge`, `Structures.place`) copies blocks and block entities but **not entities** (mapforge README:
  "entities are not placed"). Props stored only as entities would be lost.
- G&G ships client packs `armory` and `weapons` in the jar and Excalibur via `LookPacks`; the builder and Kinetic servers have no server
  resource pack today.
- The builder server runs G&G 0.5.0 (jar sha1 `3703bd7d`) to match the builder pack. Server-only code changes are fine as long as they add
  no synced registry entries; this must be checked, not assumed.
- 1.21.1 port: resource pack format and item-model override details change (1.21.1 still uses `custom_model_data` overrides); display
  entities and particle/sound commands carry over. The port plan should list the prop pack as a P1.5 resource item.
- One writer per checkout; art and systems run in separate worktrees.

## How it works (Claude's calls, open to the builder to refine)

1. **Model and display.** Each prop is one or more item models in a **server resource pack**, keyed by `custom_model_data` on a plain
   vanilla item (e.g. paper). The game shows a prop as `item_display` entities at an anchor position with a facing (0/90/180/270).
2. **Solidity and light.** Placing a prop also places `barrier` blocks over its solid footprint and vanilla `light` blocks where it
   glows (the forge's coals). Removing the prop removes them.
3. **Saved with the map: anchor block.** Each prop has an anchor that survives block-only copies, so it is stored with the map,
   for example a block entity (a hidden marker that mapforge and `Structures.place` already copy) holding prop id and facing. On map load
   (and on builder-world chunk load) the server spawns any missing display entities for anchors. The builder chooses the exact anchor
   form; the requirement is that blocks-only export carries it and that rotating a structure rotates the prop.
4. **Alive effects.** A server-side ticker runs each living prop's effect list (particles: smoke, flame, lava sparks; sound: fire
   crackle, an occasional anvil ring) only when a player is within about 24 blocks. Vanilla particles and sounds, so vanilla clients get
   them.
5. **Prop menu.** A command or a locked hotbar item opens a vanilla chest menu listing props by category with icons. Picking one gives a
   ghost preview (a translucent or glowing display entity) at the targeted block; scrolling the hotbar rotates it; clicking places it;
   hitting a placed prop removes it (op level 2, the builder permission). Vanilla chest menus work for plain clients too.
6. **Resource pack delivery.** The server sends the pack with a sha1 hash on join (`resource-pack`, `resource-pack-sha1`,
   `require-resource-pack`). Hosting location is the builder's call (the planned proxy pack host later). Modpack clients also get it; it
   must stack above Excalibur/armory without overriding them.

## Reviewer points settled by Claude (2026-11-08)

- **Anchor:** a vanilla block entity, since a custom block-entity type is itself a registry entry. A short spike picks the type. The
  candidate is a jigsaw block placed under the prop's floor: it has no side effects, holds string fields, and survives structure copies.
  Facing is stored in the anchor's own block state or name and re-derived when the map loads, so structure rotation stays correct.
- **Spawning:** a scanner spawns missing display entities from anchors when a game map is built (battles, hub) and when a chunk loads
  on the builder server. Orphan prop entities with no anchor are removed. Props in an unloaded hub area need not exist.
- **Axiom:** the prop menu is the supported way to place props. An Axiom copy that carries anchors is reconciled by the scanner
  (entities respawned, duplicates and orphans removed).
- **Resource pack:** one reserved `custom_model_data` range on one base item, checked for clashes with the armory, weapons and
  Excalibur packs. Hosting and the sha1 come from the pack build step.
- **Plain clients:** the vanilla-client acceptance test runs on the builder server only. Battles and the hub need plain-client support
  (PLAIN-1, after the 1.21.1 port), and the props add no new blocker to it.
- **Baseline:** branch from current `main`.

## Art

- Blockbench models, troop texture density (16 px per block), the troop palette approach; one texture sheet per prop.
- Forge: stone hearth with a hood/chimney, a coal bed with an emissive-looking hot texture, bellows, tools hung on the side.
- Smithy set: anvil (larger than vanilla, on a stump), weapon rack with G&G weapons, shield rack with House-colour shields, quench
  trough with water, grindstone bench.
- Art goes through the usual visual review: contact sheet at 1x and in game, Kort approves.

## Left to the builder

- Exact anchor representation and how it rotates with structures.
- How many display parts per prop; scale versus split.
- Ghost preview look; menu layout and categories.
- Pack hosting for the builder server; pack versioning.
- Effect timings and ranges, within "alive but not noisy".

## Acceptance

- A forge and the full smithy set placed through the menu on the builder server: solid, lit, smoking, sparking, crackling.
- Seen identically by a modpack client and a vanilla 1.20.1 client that accepted the pack (builder server).
- After the one jar and pack update, adding a new prop needs only a new server resource pack; the builder-pack client still joins.
- In a battle, troops path around a placed forge and arrows hit it.
- A smithy exported with mapforge and loaded as a game map shows every prop at the right place and facing, including a rotated placement.
- No measurable tick cost with 50 props loaded and no player near; acceptable cost with a player standing in a smithy.
- Kort approves the art from the contact sheet and in game.

## Builder update bundle (Kort 2026-11-08)

The one builder-pack reinstall also carries:
- **Simple Voice Chat** (client and server): uses one spare Kinetic port (the plan allows 5 allocations); proximity and group chat.
- **Xaero's Minimap and Xaero's World Map** (client only).
- **Builder handbook:** a G&G written book given on first join (and by a command) covering the rules, map briefs, the prop menu, and
  `/map` and `/warp`. Our own code, builder server only.
- **Distant Horizons**, already in the builder pack, gets its first in-game test in this update.

Third-party files are pinned in `tools/builder-server/builder-mods.json` with sha1. Each download needs Kort's yes (file, source, size)
before it is fetched. Litematica was considered and left out.

## Builder handbook content (Kort grilling, 2026-11-08)

- **Reader:** both newcomers and people who know G&G. It opens with a short "What is G&G" page, then the working pages.
- **Voice:** the opening reads in-world, like a decree from the realm. The working pages are plain and clear.
- **Worlds:** the lobby (Old Hold) plus the five maps: Kingsfield, Ironhold, Rivergate, Hollowwood, Frostmarch. Builders choose
  freely what to work on. There are no assignments or claims.
- **Map pages:** one page per map, giving theme, mood, block palette and what it still needs.
- **Writing:** Claude drafts the "What is G&G" page and the map pages from the design docs and `maps/<id>/design.json`. Kort approves
  the text before it ships.
- **Rules:**
  - No leaking: no screenshots or content shared outside the team. The penalty is a warning first, then removal from the whitelist.
  - Never give ops or items to others, and never invite people.
  - There is no rule about painted key points; builders may change layouts freely.
- **Done:** post finished work in the builders' Discord channel. The channel doesn't exist yet; Kort makes it, and the handbook
  carries a placeholder until he names it.
- **Tools page:**
  - Axiom basics: opening it, copy and paste, the blueprint browser.
  - The prop menu: open, pick, scroll to rotate, place, hit to remove.
  - `/map` and `/warp`.
  - Voice chat key and groups, and Xaero minimap waypoints.
- **Camp canvas removed (Kort 2026-11-08):** the camp stations now live in the Old Hold lobby, so the separate `camp` builder canvas
  is dropped from `BuildWorlds.ids()` and from the `gg_build` data pack. Kort didn't recall approving it. Existing `build_camp` data
  on servers is left in place, unused.
