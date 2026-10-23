> **Project navigation:** [Start here — branches, plans, artwork and work in progress](docs/migration/START-HERE.md). This private repository preserves parallel development; `main` is the 0.4.0 base, not a merge of every feature.



# Graves and Glory 0.4.0

This is a local release candidate. Isolated game checks do not constitute a
production launcher installation or an eight-human Internet multiplayer playtest.
Public Internet matchmaking and the home-hosted official service remain planned
work. This candidate reworks the solo game: shots that fly before they land,
lines that rout and rally, recruits deploying from held points, Easy, Normal and
Hard bots, a guided first campaign battle and a report after every match (see
"0.4.0 solo rework candidate" below). Nothing in this update has been published
automatically.

Clients must match the server's game version and build identity. A missing or
incompatible pack receives an update instruction before entering the world.
This compatibility check does not authenticate progression or prevent cheating.

This modpack ships no third-party resource packs (Kort, 2026-10-18). Its look is
its own built-in assets plus the shaders credited below.

Upgrades copy legacy Bannerbound collections, deployment data and visual
preferences into the current folders without overwriting current files. When
only a legacy Bannerbound world exists, it is copied before opening; the original
is retained. Close that world normally before upgrading, keep a backup, and allow
disk space for the copy. If both world folders exist, the current world wins.

Collect soldiers, arrange a twenty-unit army and lead it across five large fantasy battlefields inside Minecraft: Java Edition. Play 1vAI practice, 1v1, 2v2, 3v3 or 4v4, with optional bots filling empty team slots.

## Camp, friends and permanent armies

The branded title menu waits for your choice. Enter camp opens a walled courtyard with a Quartermaster, Recruiter, Archivist and Steward. Use their stations or press R for army equipment, recruitment, contracts, chapter rewards and friends. Camp hides battle information and combat controls. War Gates offer ready pads, map voting and a countdown; a party must stand together on the same side. Practice remains available from the guide.

Play with friends offers local-network hosting and the normal server browser. Hosting announces its LAN address in chat. Friend parties hold up to four commanders and persist in that host's world across matches and restarts. Invite an online commander, accept, then create or join a private room by code. Each member confirms readiness; the host selects 1v1–4v4, map and bot fill. Parties move teams together and never silently split. Results retain the room for a rematch. Rooms are temporary and end at server restart; parties persist. One battle runs at a time while other rooms wait in camp. Private rooms are code-based, without passwords or spectator mode.

The free, nonexpiring Founders chapter ends at tier 30 (7,500 XP). Eligible matches grant 100 XP, plus 50 for victory. Daily and weekly contracts add claimable gold and XP. Twenty-four tiers grant 25 gold; every fifth tier grants a title, ending with Bannerlord. The current chapter has a clear endpoint; future chapters, troops and cosmetic rewards can extend it. Rewards never raise combat stats.

Arrange twenty deployment slots before battle: place any unlocked unit type in any front or rear slot, or leave slots empty. There is no required ratio of troop types. These positions control initial deployment; battlefield orders and formations take over when issued. Losing a soldier never removes its permanent unlock.

Six starting types are available immediately: Dawn Knight, Dawn Ranger, Sunweaver, Dusk Revenant, Bone Archer and Gravecaller. Six additional types can be unlocked: Dawn Sentinel, Dawn Longbow, Dawn Oracle, Dusk Reaper, Dusk Stalker and Dusk Wraith. Unlocking a type lets you field any number within the twenty-troop cap. Practice lets you try all types; trial units stay locked in regular matches.

Earn 80 collection gold for completing a regular match, plus 40 for victory. Practice awards 40. A match must last at least three minutes to award gold; leaving before the result earns none. A roll costs 100 gold and permanently unlocks one remaining type, with equal odds and no duplicates. There are no real-money rolls. Collections and army arrangements save automatically with the Minecraft player profile and travel to friends' hosts; the host also keeps a saved copy. This is a local friends-hosted economy, not a centrally secured competitive service.

Pick one of six commander classes in the war council. Heroes lead from the front: the Champion has 4 more health and his infantry within 10 blocks deal 10% more damage; the Reaver deals 2 more attack damage and respawns in 9 seconds instead of 12; the Warden's ranged troops deal 10% more damage and reach 2 blocks further. Generals run the army: the Marshal's troops train in 20% less time; the Warlock's magic troops cost 20% less gold in battle; each point the Quartermaster holds pays 2 more gold every income tick. Each class also has an active ability on the ability key (H). Dawn shield troops guard in close compact combat and support heal wounded allies. Dusk casters mark an area before a damaging slowing spell lands. Ranged attacks require line of sight.

## Five purposeful battlefields

Every playable map is 700 by 500 blocks, with three objectives (Kingsfield Crossroads has five). Roads connect settlements, bases and river crossings; broad approaches, flanking routes, hills and woods offer different positions.

- Rivergate Valley: a river settlement with mill, farms, market, customs buildings, trade crossing and shrine.
- Ironhold Siege: a breached fortress, curtain walls, supply barn, garrison and flanking military roads.
- Frostmarch Pass: snowy ridges, a mountain river, caravan lodge, sheltered refuge and signal tower.
- Hollowwood Reach: dense woodland, a logging settlement, abandoned chapel, old causeway and moon shrine.
- Kingsfield Crossroads: open plains with five objectives (two fords, a central bridge and a raised outpost on each flank).

The first visit prepares the selected map and caches its completed layout. Selecting a different map prepares that battlefield. Collection saves are separate from map generation.

## Battlefield commands and combat

R selects All, Infantry, Ranged or Support. Choose Move, Attack, Charge, Hold, Follow or Retreat, aim in the world, adjust facing with bracket keys and confirm with attack or Enter. Use or Backspace cancels. Visible formation outlines preview the destination. Compact, Line, Wedge and Loose change frontage and spacing. G aims an attack, V recalls the whole army and B changes formation. Minecraft Controls can rebind these keys.

M opens the tactical map: select groups, right-click destinations and send Attack, Defend, Help or Moving-here pings. Gold marks your troops, cyan allies, red currently sighted enemies and gray their last known positions, which expire after ten seconds.

Hold objectives to reach 300 points. Rounds last at most fifteen minutes; higher score wins at timeout and equal scores draw. Commanders respawn after twelve seconds. Friendly fire is off. AI commanders attack and defend objectives using the same routes, and train replacements using the same supplies and training times as players, regrouping after a wipe. Bot fill supports larger battles with fewer friends. A joining friend can take over an available AI army during a round.

Right-click your own faction's barracks lectern to buy, train or disband battlefield soldiers. Barracks gold is separate from permanent collection gold and resets each round. Match supplies arrive over time, with a limited bonus for objectives. Training is sequential; living plus queued troops cannot exceed twenty. Cancelling refunds queued purchases; disbanding gives no refund. Replacements receive their group's orders.

Six-second skippable camera entrances feature the commander and army with original battle horns. Swings, bow and spell poses, projectile trails, impact particles and spatial sounds accompany combat. Original unlock and victory cues mark their events. R or Escape skips an entrance.

## Dark fantasy presentation

The war council, collection, class selection, command controls and battlefield panels share original dark ironwork, muted gold trim and readable faction colors. Collection portraits render the actual equipped troop models. Numbered deployment slots show troop roles and weapons; tooltips identify the full unit.

Visuals & sound in the lobby lets each player choose one shader: Complementary Reimagined r5.5.1, Complementary Unbound r5.5.1, MakeUp Ultra Fast 9.5g, or shaders off. Shader changes are saved locally. Shaders begin off for reliable battle performance on a range of PCs; selecting one loads it inside the game. Complementary's pinned releases have an explicit modpack distribution grant; the latest shader versions have different distribution terms and are not silently substituted.

The look is the modpack's own. It ships no third-party resource packs, so there is no world texture choice: Minecraft's textures come from each player's own game, and the built-in armory pack adds the weapon and armour overlays. Each player's graphics choices are independent in multiplayer.

Confirmed commander hits receive a brief ink-and-gold contact accent and restrained camera recoil. Choose Off, Subtle (default), or Strong. Ambient troop fights never trigger repeated screen flashes; accents do not stop the simulation, change damage or interrupt other players. The camera entrance is sampled every rendered frame instead of stepping its coordinates at the simulation tick rate.

Sodium, Iris, Lithium, FerriteCore, ImmediatelyFast and ModernFix support rendering, simulation and memory use. Presence Footsteps adds material-specific footsteps. These are pinned, unmodified third-party mods, with all required dependencies supplied. No combat overhaul, extra minimap, world-generation mod or progression mod replaces Graves and Glory's rules. Performance depends on hardware and chosen effects; a frame-limit result is not a promise for every PC.

## Required game and multiplayer

Requires an account owning Minecraft: Java Edition. Built for Minecraft 1.20.1, Fabric Loader 0.16.14 and Fabric API 0.92.2+1.20.1. Install the Modrinth .mrpack in Prism Launcher or another launcher that imports .mrpack files; it sets up Minecraft and Fabric Loader for the instance and downloads the pinned mods, including Fabric API. Java 17 or newer is required. The plain zip bundles the redistributable Fabric API and expects you to install Fabric Loader yourself. Roblox's Command an Army is inspiration only; Roblox is not required. No Minecraft or Roblox content is included.

A local Modrinth .mrpack candidate, a plain zip and a dedicated-server kit are prepared, with pinned official downloads and a localhost-only dedicated-server configuration template. The title menu works with no launcher-specific runtime files. A Prism Launcher import and start of the 0.3.3 .mrpack was checked on this PC; 0.3.7 has not been installed that way. Dedicated-server and real launcher checks are tracked in the work log; they do not establish Internet reachability. It is not yet a published Modrinth or CurseForge release.

Up to eight human commanders, four per faction, each with twenty troops (160 total). A client can host a local-network camp and stay open, or players can join a separately configured dedicated server. Practice stays private. If a launcher supplies an invitation file, it adds an optional action under Play with friends and never starts a connection automatically; an ordinary Prism or Modrinth install has no such file. Remote friends need a reachable server. Direct Internet reachability and an eight-human Internet match have not been independently tested.

## Verification

Development game runs exercise the physical lobby, paid unlocks, saved profiles, deployment editing, class bonuses, bot objectives, actual combat, client orders, rewards and all team capacities. All twelve map crossings passed with twenty actual pathfinding troops. Two local development clients joined and stayed in a running match with the graphics and animation stack installed. All three shaders ran real 4v4 entrances on this PC. Production installation and final contact-feedback checks are still being verified for this release. Mechanical tests do not establish balance or replace human playtesting. The local development authentication harness is never included in the player package.

## Credits and permissions

Concept and direction: Kort Tjepkema. Implementation, authored environments, animation adjustments, effects and original synthesized sound cues assisted by OpenAI Codex using Melty's modding kit. Original Graves and Glory code and content: MIT; remixes allowed.

Inspired by Command an Army on Roblox; no Roblox code, assets or branding included. Minecraft belongs to Mojang/Microsoft. Minecraft models, equipment, textures, particles and stock sounds are read from each player's own game and are not redistributed.

Fabric API: FabricMC contributors, Apache License 2.0, bundled with its original license. Fabric Loader is installed from its authors by the launcher (or the server setup script). Third-party components retain their licenses.

Complementary Reimagined / Unbound: EminGTR and [Complementary Development](https://www.complementary.dev/shaders/), License Agreement 1.5 in the supplied r5.5.1 files. Compatibility support for this modpack is Graves and Glory's responsibility. MakeUp Ultra Fast: [javiergcim](https://github.com/javiergcim/MakeUpUltraFast), LGPL-3.0-or-later, with its shader sources and notices.

Sodium: CaffeineMC, PolyForm Shield 1.0.0. Iris: IrisShaders contributors, LGPL-3.0. Lithium: CaffeineMC contributors, LGPL-3.0. FerriteCore: malte0811, MIT. ImmediatelyFast: RaphiMC, LGPL-3.0-or-later. ModernFix: embeddedt and contributors, LGPL-3.0. Entity Model Features / Entity Texture Features: Traben and contributors, LGPL-3.0. Presence Footsteps: Ha3, Sollace and contributors, MIT for the supplied 1.10.1 release. Marcellus font: Brian J. Bonislawsky / Astigmatic, SIL OFL 1.1. Original author notices and corresponding library sources are supplied with the release. See THIRD_PARTY.md for links, exact versions and nested libraries.

## Interface and troop detail (0.3.0)

Redesigned battle setup, class selection and barracks with live troop previews, readable Lato body type, and restrained brass selection states. Select a class, then confirm it. Recruitment costs and availability remain server-authoritative.

Armour detail uses Entity Model Features overlays from the built-in armory pack: a tabard, buckle, belt and shoulder pauldrons on player, husk, skeleton and wither skeleton models. Limits: one neutral armour set, so faction colours are not applied to these overlays; the overlays apply to every husk, skeleton and wither skeleton in a Graves and Glory world, not only named troops.

Development checks are never in the player archive. They are built from `test-tools/` by the `localTestToolsJar` task and run only with `gradle runClient --offline -PcampaignChecks`.

The war council uses R by default. The bundled Iris shader-reload default moves to F8 when it conflicts with the unchanged war-council default (or becomes unbound if F8 is occupied). Custom key bindings remain configurable in Minecraft Controls.

## 0.3.7 camp waiting candidate

Full matches now admit visitors to camp without granting a battle seat. R opens
the war council while waiting. New server templates allow 16 connections, with
a maximum of eight battle commanders; existing servers need `max-players`
raised to leave room for visitors. Admission policy checks pass; live full-match
join, camp movement, reconnect and next-round verification remain pending.

## 0.4.0 solo rework candidate

Arrows, bolts and spells fly before they land, with team-coloured impact puffs,
and a group card flashes when its troops take hits. A shattered group, or one
taken from behind, routs with a horn and rallies; orders given during the rout
apply on regroup. Barracks recruits march out from a point your side has held
for ten seconds near the fight. Bots come in Easy, Normal and Hard (Bots button
in the camp guide). The Long March battle 1, The Training Field, teaches
command with lessons that name your own keys, and every match ends with a
battle report of kills, losses, gold and time on points, with an MVP.
Verified by automated checks in an isolated development client; bot tier
balance is parked until the rebuilt maps.
