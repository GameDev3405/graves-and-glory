> **Current state (2026-11-07):** `main` is the 0.5.0 development integration heading for 1.0.0 (a draft of the player notes is in [docs/releases/1.0.0-notes-DRAFT.md](docs/releases/1.0.0-notes-DRAFT.md)), at `2fc30f50`. It has five Houses, thirty troops with their own models and jointed limbs, in-match levels, commander gear (four sets plus the five House armour sets, relics on keys 1 to 3, Arsenal screen, Forge; the commander classes are removed), the command UI (R panel with groups, formations and charge), the War Table with a six-seat lobby, Domination, Siege, Convoy and Survival, the six-battle campaign, bots, the dedicated server kit, audio cues and the 2026-11-07 audit fixes. Pending, not in `main`: the Old Hold lobby (`feat/oldhold-wiring`, awaiting Kort's in-game walk), Kort's own map designs and the hosted network plan (`docs/plan-1.0-network`, awaiting Kort's review). It is not a published release. Start with [docs/BOARD.md](docs/BOARD.md), then [docs/HANDOFF.md](docs/HANDOFF.md); [docs/WORKFLOW.md](docs/WORKFLOW.md), [docs/BACKLOG.md](docs/BACKLOG.md), [docs/DECISIONS.md](docs/DECISIONS.md) and [docs/SETUP.md](docs/SETUP.md) are the active records. Historical lanes are preserved under `archive/2026-10/*` tags.



# Graves and Glory 0.5.0 (Houses development integration)

This is the current development integration, not a published release. Isolated game checks do not constitute a
production launcher installation or a six-player networked playtest.
Public Internet matchmaking and the home-hosted official service remain planned
work. This candidate reworks the solo game: shots that fly before they land,
lines that fight on to a last stand (nothing routs in 0.5.0), recruits deploying from held points, Easy, Normal and
Hard bots, a guided first campaign battle and a report after every match (see
"0.4.0 solo rework candidate" below). Version 0.5.0 adds five Houses (thirty troops), schema 2 collections that migrate
from older camps, commander gear and relics, in-match levels and gear tiers, the Arsenal and Forge, the War Table pages, the banner page, and the live builder server; it
remains a local candidate for Kort's playtest only
(docs/PHASE-2-PLAYTEST.md). Nothing in this update has been published
automatically.

Clients must match the server's game version and build identity. A missing or
incompatible pack receives an update instruction before entering the world.
This compatibility check does not authenticate progression or prevent cheating.

This modpack's look is its own built-in assets plus the shaders credited below, and no third-party resource pack.

Upgrades copy legacy Bannerbound collections, deployment data and visual
preferences into the current folders without overwriting current files. When
only a legacy Bannerbound world exists, it is copied before opening; the original
is retained. Close that world normally before upgrading, keep a backup, and allow
disk space for the copy. If both world folders exist, the current world wins.

Collect soldiers, arrange an army of up to 40 troops and lead it across five large fantasy battlefields inside Minecraft: Java Edition. Play 1vAI practice, 1v1, 2v2, 3v3 or 4v4, with optional bots filling empty team slots.

## Camp, friends and permanent armies

The branded title menu waits for your choice. Enter camp opens a walled courtyard with a Quartermaster, Recruiter, Archivist and Steward. Use their stations or press R for army equipment, recruitment, contracts, chapter rewards and friends. Camp hides battle information and combat controls. War Gates offer ready pads, map voting and a countdown; a party must stand together on the same side. Practice remains available from the guide.

Play with friends offers local-network hosting and the normal server browser. Hosting announces its LAN address in chat. Friend parties hold up to four commanders and persist in that host's world across matches and restarts. Invite an online commander, accept, then create or join a private room by code. Each member confirms readiness; the host selects 1v1–4v4, map and bot fill. Parties move teams together and never silently split. Results retain the room for a rematch. Rooms are temporary and end at server restart; parties persist. One battle runs at a time while other rooms wait in camp. Private rooms are code-based, without passwords or spectator mode.

The free, nonexpiring Founders season pass ends at tier 30 (7,500 XP). Eligible matches grant 100 XP, plus 50 for victory. Daily and weekly contracts add claimable troop writs, gear shards and XP. Twenty-four tiers grant 25 gear shards and a quarter of a troop writ; every fifth tier grants a title, ending with Bannerlord. The current season pass has a clear endpoint; future seasons, troops and cosmetic rewards can extend it. Rewards never raise combat stats.

Lay out your army on the war table banner before battle: a grid of 9 columns by 6 rows, with row 0 at the front. Place any unlocked unit type in any cell, or leave cells empty. A banner holds up to 40 troops costing at most 640 in total (each troop's muster cost), with at most one Legendary and no limit on Epics. A troop takes one cell whatever its size. You can save up to five banners with names of 1 to 16 characters and pick one as active. There is no required ratio of troop types. Troops deploy at their cell and a refill returns to its own cell; battlefield orders and formations take over when issued. Losing a soldier never removes its permanent unlock.

The ten Commons of the five Houses are yours from the start. The other twenty troops (Rares, Epics and Legendaries) come from rolls, chosen buys, Pacts and chapter rewards. Owning a type lets you field any number within the banner's 40-troop and 640-cost limits, and a banner can hold troops of more than one House. Practice lets you try all types; trial units stay locked in regular matches.

Completing a regular match pays 0.8 troop writs and 80 gear shards, plus 0.4 writs and 40 shards for victory (the fraction of a writ carries over to the next match). Practice pays 0.4 writs and 40 shards. A match must last at least three minutes to pay; leaving before the result earns nothing. Troop writs buy troops: a roll costs 1 writ and draws from the House you pick, by rarity: a new troop is unlocked, and a duplicate raises its stars (a duplicate of a full-star troop pays shards). A chosen buy costs 3 writs and Epics are guaranteed by pity. Gear shards pay for the Forge: rolls, repairs and extra rows. Pacts, which used to be called writs, are the Legendary currency. Houses also pay shards, Renown and Mastery after matches of three minutes or more. There are no real-money rolls. Collections and army arrangements save automatically with the Minecraft player profile and travel to friends' hosts; the host also keeps a saved copy. This is a local friends-hosted economy, not a centrally secured competitive service.

Your commander fights in his gear; the old commander classes and the H key are gone. What you wear sets your role, health, attack and armour, set bonuses and perks. Up to three relics give active abilities on keys 1 to 3. The old class actives live on as relics: Stand Fast, Bloodrush, Mark Prey, Rally Horn, Grave Bloom and Forward Supply, with six more. In camp, press I for the Arsenal (stats, worn gear, relics, filters, compare and equip), and visit the Blacksmith to roll, break down and repair gear at the Forge. Pending: gear art, gear animations and gear sounds. Dawn shield troops guard in close compact combat and support heal wounded allies. Dusk casters mark an area before a damaging slowing spell lands. Ranged attacks require line of sight.

## Five purposeful battlefields

Every playable map is 700 by 500 blocks, with three objectives (Kingsfield Crossroads has five). Roads connect settlements, bases and river crossings; broad approaches, flanking routes, hills and woods offer different positions.

- Rivergate Valley: a river settlement with mill, farms, market, customs buildings, trade crossing and shrine.
- Ironhold Siege: a breached fortress, curtain walls, supply barn, garrison and flanking military roads.
- Frostmarch Pass: snowy ridges, a mountain river, caravan lodge, sheltered refuge and signal tower.
- Hollowwood Reach: dense woodland, a logging settlement, abandoned chapel, old causeway and moon shrine.
- Kingsfield Crossroads: open plains with five objectives (two fords, a central bridge and a raised outpost on each flank).

The first visit prepares the selected map and caches its completed layout. Selecting a different map prepares that battlefield. Collection saves are separate from map generation.

## Battlefield commands and combat

R selects All, Infantry, Ranged or Support. Choose Move, Attack, Charge, Hold, Follow or Retreat, aim in the world, adjust facing with bracket keys and confirm with attack or Enter. Use or Backspace cancels. Visible formation outlines preview the destination. Block, Banner, Wedge and Loose change frontage and spacing; Banner stands the army as the war table lays it out. G aims an attack, V recalls the whole army and B changes formation. Minecraft Controls can rebind these keys.

M opens the tactical map: select groups, right-click destinations and send Attack, Defend, Help or Moving-here pings. Gold marks your troops, cyan allies, red currently sighted enemies and gray their last known positions, which expire after ten seconds.

Hold objectives to reach 300 points. Rounds last at most fifteen minutes; higher score wins at timeout and equal scores draw. Commanders respawn after twelve seconds. Friendly fire is off. AI commanders attack and defend objectives using the same routes, and train replacements using the same supplies and training times as players, regrouping after a wipe. Bot fill supports larger battles with fewer friends. A joining friend can take over an available AI army during a round.

Right-click your own faction's barracks lectern to buy, train or disband battlefield soldiers. Barracks gold is separate from your saved troop writs and gear shards and resets each round. Match supplies arrive over time, with a limited bonus for objectives. Training is sequential; living plus queued troops cannot exceed 40, and each troop type cannot pass its count in your banner. Cancelling refunds queued purchases; disbanding gives no refund. Replacements receive their group's orders.

Six-second skippable camera entrances feature the commander and army with original battle horns. Swings, bow and spell poses, projectile trails, impact particles and spatial sounds accompany combat. Original unlock and victory cues mark their events. R or Escape skips an entrance.

## Dark fantasy presentation

The war council, collection, gear role, command controls and battlefield panels share original dark ironwork, muted gold trim and readable faction colors. Collection portraits render the actual equipped troop models. Numbered deployment slots show troop roles and weapons; tooltips identify the full unit.

Visuals & sound in the lobby lets each player choose one shader: Complementary Reimagined r5.5.1, Complementary Unbound r5.5.1, MakeUp Ultra Fast 9.5g, or shaders off. Shader changes are saved locally. Shaders begin off for reliable battle performance on a range of PCs; selecting one loads it inside the game. Complementary's pinned releases have an explicit modpack distribution grant; the latest shader versions have different distribution terms and are not silently substituted.

The look is the modpack's own built-in assets. Minecraft's textures come from each player's own game. Each player's graphics choices are independent in multiplayer.

Confirmed commander hits receive a brief ink-and-gold contact accent and restrained camera recoil. Choose Off, Subtle (default), or Strong. Ambient troop fights never trigger repeated screen flashes; accents do not stop the simulation, change damage or interrupt other players. The camera entrance is sampled every rendered frame instead of stepping its coordinates at the simulation tick rate.

Sodium, Iris, Lithium, FerriteCore, ImmediatelyFast and ModernFix support rendering, simulation and memory use. Presence Footsteps adds material-specific footsteps. These are pinned, unmodified third-party mods, with all required dependencies supplied. No combat overhaul, extra minimap, world-generation mod or progression mod replaces Graves and Glory's rules. Performance depends on hardware and chosen effects; a frame-limit result is not a promise for every PC.

## Required game and multiplayer

Requires an account owning Minecraft: Java Edition. Built for Minecraft 1.21.1, Fabric Loader 0.19.5 and Fabric API 0.116.18+1.21.1. Install the Modrinth .mrpack in Prism Launcher or another launcher that imports .mrpack files; it sets up Minecraft and Fabric Loader for the instance and downloads the pinned mods, including Fabric API. Java 21 or newer is required. The plain zip bundles the redistributable Fabric API and expects you to install Fabric Loader yourself. Roblox's Command an Army is inspiration only; Roblox is not required. No Minecraft or Roblox content is included.

Three pack formats and a server kit are prepared locally, none published: a Modrinth .mrpack (for Prism Launcher or any launcher that imports .mrpack), a CurseForge modpack zip (`tools/export_curseforge.py`, with `tools/verify_curseforge.py` as its readback check), and a plain zip. The dedicated-server zip is the one way to run a server: it is `tools/server-kit/` (see docs/SERVER-RUNBOOK.md) plus the mod jars, with setup, start, stop, backup, restore and update scripts, a version gate and a whitelist-on configuration on port 25580; it never writes the EULA for you, so you set `eula=true` yourself. The server kit booted, saved and stopped cleanly in a scratch test on 2026-11-02 with no clients. The title menu works with no launcher-specific runtime files. The Modrinth .mrpack install test in a launcher passed. Dedicated-server and real launcher checks are tracked in the work log; they do not establish Internet reachability. It is not yet a published Modrinth or CurseForge release.

Up to eight human commanders, four per faction, each with up to 40 troops (320 total). A client can host a local-network camp and stay open, or players can join a separately configured dedicated server. Practice stays private. If a launcher supplies an invitation file, it adds an optional action under Play with friends and never starts a connection automatically; an ordinary Prism or Modrinth install has no such file. Remote friends need a reachable server. Direct Internet reachability and an eight-human Internet match have not been independently tested.

## Verification

Development game runs exercise the physical lobby, paid unlocks, saved profiles, deployment editing, gear bonuses, bot objectives, actual combat, client orders, rewards and all team capacities. All twelve map crossings passed with twenty actual pathfinding troops. Two local development clients joined and stayed in a running match with the graphics and animation stack installed. All three shaders ran real 4v4 entrances on this PC. Production installation and final contact-feedback checks are still being verified for this release. Mechanical tests do not establish balance or replace human playtesting. The local development authentication harness is never included in the player package.

## Credits and permissions

Concept and direction: Kort Tjepkema. Implementation, authored environments, animation adjustments, effects assisted by OpenAI Codex using Melty's modding kit. Sound effects are Pixabay samples (Pixabay Content License). Original Graves and Glory code and content: MIT; remixes allowed.



Inspired by Command an Army on Roblox; no Roblox code, assets or branding included. Minecraft belongs to Mojang/Microsoft. Minecraft models, equipment, textures, particles and stock sounds are read from each player's own game and are not redistributed.

Fabric API: FabricMC contributors, Apache License 2.0, bundled with its original license. Fabric Loader is installed from its authors by the launcher (or the server setup script). Third-party components retain their licenses.

Complementary Reimagined / Unbound: EminGTR and [Complementary Development](https://www.complementary.dev/shaders/), License Agreement 1.5 in the supplied r5.5.1 files. Compatibility support for this modpack is Graves and Glory's responsibility. MakeUp Ultra Fast: [javiergcim](https://github.com/javiergcim/MakeUpUltraFast), LGPL-3.0-or-later, with its shader sources and notices.

Sodium: CaffeineMC, PolyForm Shield 1.0.0. Iris: IrisShaders contributors, LGPL-3.0. Lithium: CaffeineMC contributors, LGPL-3.0. FerriteCore: malte0811, MIT. ImmediatelyFast: RaphiMC, LGPL-3.0-or-later. ModernFix: embeddedt and contributors, LGPL-3.0. Entity Model Features / Entity Texture Features: Traben and contributors, LGPL-3.0. Presence Footsteps: Ha3, Sollace and contributors, MIT for the supplied 1.10.1 release. Marcellus font: Brian J. Bonislawsky / Astigmatic, SIL OFL 1.1. Original author notices and corresponding library sources are supplied with the release. See THIRD_PARTY.md for links, exact versions and nested libraries.

## Interface and troop detail (0.3.0)

Redesigned battle setup, gear role and barracks with live troop previews, readable Lato body type, and restrained brass selection states. Recruitment costs and availability remain server-authoritative.

Armour detail uses Entity Model Features overlays from the built-in armory pack: a tabard, buckle, belt and shoulder pauldrons on player, husk, skeleton and wither skeleton models. Limits: one neutral armour set, so faction colours are not applied to these overlays; the overlays apply to every husk, skeleton and wither skeleton in a Graves and Glory world, (named troops no longer use them: in 0.5.0 troops draw their own models).

Development checks are never in the player archive. They are built from `test-tools/` by the `localTestToolsJar` task and run only with `gradle runClient --offline -PcampaignChecks`.

The war council uses R by default. The bundled Iris shader-reload default moves to F8 when it conflicts with the unchanged war-council default (or becomes unbound if F8 is occupied). Custom key bindings remain configurable in Minecraft Controls.

## 0.3.7 camp waiting candidate

Full matches now admit visitors to camp without granting a battle seat. R opens
the war council while waiting. New server templates allow 16 connections, with
a maximum of eight battle commanders; existing servers need `max-players`
raised to leave room for visitors. Admission policy checks pass; live full-match
join, camp movement, reconnect and next-round verification remain pending.

## 0.4.0 solo rework candidate

*History: this is the 0.4.0 text. In 0.5.0 nothing routs; a losing group makes a last stand.*

Arrows, bolts and spells fly before they land, with team-coloured impact puffs,
and a group card flashes when its troops take hits. A shattered group, or one
taken from behind, routs with a horn and rallies; orders given during the rout
apply on regroup. Barracks recruits march out from a point your side has held
for ten seconds near the fight. Bots come in Easy, Normal and Hard (Bots button
in the camp guide). The Oath, the first campaign chapter, opens with The Drowned Crossings, which teaches
command with lessons that name your own keys, and every match ends with a
battle report of kills, losses, gold and time on points, with an MVP.
Verified by automated checks in an isolated development client; bot tier
balance is parked until the rebuilt maps.
