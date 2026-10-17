

# Graves and Glory 0.3.0

Collect soldiers, arrange a twenty-unit army and lead it across four large fantasy battlefields inside Minecraft: Java Edition. Play 1vAI practice, 1v1, 2v2, 3v3 or 4v4, with optional bots filling empty team slots.

## War council and permanent armies

Play opens a dedicated Graves and Glory world with a physical lobby, recruitment hall, armoury, troop displays and war table. R opens the war council. The host chooses the map, match size and bot fill. Every commander chooses a faction, army and class.

Arrange twenty deployment slots before battle: place any unlocked unit type in any front or rear slot, or leave slots empty. There is no required ratio of troop types. These positions control initial deployment; battlefield orders and formations take over when issued. Losing a soldier never removes its permanent unlock.

Six starting types are available immediately: Dawn Knight, Dawn Ranger, Sunweaver, Dusk Revenant, Bone Archer and Gravecaller. Six additional types can be unlocked: Dawn Sentinel, Dawn Longbow, Dawn Oracle, Dusk Reaper, Dusk Stalker and Dusk Wraith. Unlocking a type lets you field any number within the twenty-troop cap. Practice lets you try all types; trial units stay locked in regular matches.

Earn 80 collection gold for completing a regular match, plus 40 for victory. Practice awards 40. A match must last at least three minutes to award gold; leaving before the result earns none. A roll costs 100 gold and permanently unlocks one remaining type, with equal odds and no duplicates. There are no real-money rolls. Collections and army arrangements save automatically with the Minecraft player profile and travel to friends' hosts; the host also keeps a saved copy. This is a local friends-hosted economy, not a centrally secured competitive service.

Classes boost matching troops: Vanguard grants infantry 20% health and 10% damage; Marksman grants ranged troops 15% damage and 10% attack speed; Arcanist grants support 20% health and 20% spell or healing power. Dawn shield troops guard in close compact combat and support heal wounded allies. Dusk casters mark an area before a damaging slowing spell lands. Ranged attacks require line of sight.

## Four purposeful battlefields

Every playable map is 700 by 500 blocks, with three objectives. Roads connect settlements, bases and river crossings; broad approaches, flanking routes, hills and woods offer different positions.

- Rivergate Valley: a river settlement with mill, farms, market, customs buildings, trade crossing and shrine.
- Ironhold Siege: a breached fortress, curtain walls, supply barn, garrison and flanking military roads.
- Frostmarch Pass: snowy ridges, a mountain river, caravan lodge, sheltered refuge and signal tower.
- Hollowwood Reach: dense woodland, a logging settlement, abandoned chapel, old causeway and moon shrine.

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

World texture choices are Minecraft, Faithful 32x, or medieval Excalibur. All three include Fresh Animations with Entity Model Features and Entity Texture Features. Excalibur is the first-launch default, using the authors' Clarent backport and Fresh Animations compatibility patch on Minecraft 1.20.1. Resource packs reload when changed. Graves and Glory's own faction skin and equipment remain identifiable. Each player's graphics choices are independent in multiplayer.

Confirmed commander hits receive a brief ink-and-gold contact accent and restrained camera recoil. Choose Off, Subtle (default), or Strong. Ambient troop fights never trigger repeated screen flashes; accents do not stop the simulation, change damage or interrupt other players. The camera entrance is sampled every rendered frame instead of stepping its coordinates at the simulation tick rate.

Sodium, Iris, Lithium, FerriteCore, ImmediatelyFast and ModernFix support rendering, simulation and memory use. Presence Footsteps adds material-specific footsteps. These are pinned, unmodified third-party mods, with all required dependencies supplied. No combat overhaul, extra minimap, world-generation mod or progression mod replaces Graves and Glory's rules. Performance depends on hardware and chosen effects; a frame-limit result is not a promise for every PC.

## Required game and multiplayer

Requires an account owning Minecraft: Java Edition. Built for Minecraft 1.20.1, Fabric Loader 0.16.14 and Fabric API 0.92.2+1.20.1. Melty supplies Minecraft, Java and Fabric Loader; the redistributable Fabric API is bundled. Roblox's Command an Army is inspiration only; Roblox is not required. No Minecraft or Roblox content is included.

Up to eight human commanders, four per faction, each with twenty troops (160 total). One client hosts and stays open. Everyone uses the same package; there is no separate server download. Melty passes a joining client's address automatically and clears it for solo/hosting Play. Its native relay supports remote joining on supported networks. Direct Internet reachability and an eight-human Internet match have not been independently tested.

## Verification

Development game runs exercise the physical lobby, paid unlocks, saved profiles, deployment editing, class bonuses, bot objectives, actual combat, client orders, rewards and all team capacities. All twelve map crossings passed with twenty actual pathfinding troops. Two local development clients joined and stayed in a running match with the graphics and animation stack installed. All three shaders ran real 4v4 entrances on this PC. Production installation and final contact-feedback checks are still being verified for this release. Mechanical tests do not establish balance or replace human playtesting. The local development authentication harness is never included in the player package.

## Credits and permissions

Concept and direction: Kort Tjepkema. Implementation, authored environments, animation adjustments, effects and original synthesized sound cues assisted by OpenAI Codex using Melty's modding kit. Original Graves and Glory code and content: MIT; remixes allowed.

Inspired by Command an Army on Roblox; no Roblox code, assets or branding included. Minecraft belongs to Mojang/Microsoft. Minecraft models, equipment, textures, particles and stock sounds are read from each player's own game and are not redistributed.

Fabric API: FabricMC contributors, Apache License 2.0, bundled with its original license. Fabric Loader is installed by Melty from its authors. Third-party components retain their licenses.

Complementary Reimagined / Unbound: EminGTR and [Complementary Development](https://www.complementary.dev/shaders/), License Agreement 1.5 in the supplied r5.5.1 files. Compatibility support for this modpack is Graves and Glory's responsibility. MakeUp Ultra Fast: [javiergcim](https://github.com/javiergcim/MakeUpUltraFast), LGPL-3.0-or-later, with its shader sources and notices.

[Fresh Animations](https://modrinth.com/resourcepack/fresh-animations): FreshLX, included unchanged under its modpack permission. [Excalibur](https://www.curseforge.com/minecraft/texture-packs/excalibur): Matthew (Maffhew) Dillow, CC-BY-NC-ND-3.0 with the included author terms. [Clarent](https://modrinth.com/resourcepack/clarent): GrandPappyJay. [Excalibur Fresh Animations Patch](https://modrinth.com/resourcepack/excalibur-fresh-animations): GrandPappyJay and Ruseki, with FreshLX and Maffhew; unchanged, noncommercial modpack inclusion. [Faithful 32x](https://faithfulpack.net/): Faithful contributors, optional world textures under the [Faithful License](https://faithfulpack.net/license); original archive notices are retained. These packs are not assets owned by Graves and Glory or covered by its MIT grant.

Sodium: CaffeineMC, PolyForm Shield 1.0.0. Iris: IrisShaders contributors, LGPL-3.0. Lithium: CaffeineMC contributors, LGPL-3.0. FerriteCore: malte0811, MIT. ImmediatelyFast: RaphiMC, LGPL-3.0-or-later. ModernFix: embeddedt and contributors, LGPL-3.0. Entity Model Features / Entity Texture Features: Traben and contributors, LGPL-3.0. Presence Footsteps: Ha3, Sollace and contributors, MIT for the supplied 1.10.1 release. Marcellus font: Brian J. Bonislawsky / Astigmatic, SIL OFL 1.1. Original author notices and corresponding library sources are supplied with the release. See THIRD_PARTY.md for links, exact versions and nested libraries.

## Interface and troop detail (0.3.0)

Redesigned battle setup, class selection and barracks with live troop previews, readable Lato body type, and restrained brass selection states. Select a class, then confirm it. Recruitment costs and availability remain server-authoritative.

Armour detail uses Entity Model Features overlays from the built-in armory pack: a tabard, buckle, belt and shoulder pauldrons on player, husk, skeleton and wither skeleton models. Limits: one neutral armour set, so faction colours are not applied to these overlays; the overlays apply to every husk, skeleton and wither skeleton in a Graves and Glory world, not only named troops; and while the armory pack is enabled it replaces the Excalibur and Fresh Animations patch skeleton and wither skeleton models.

Development checks are never in the player archive. They are built from `test-tools/` by the `localTestToolsJar` task and run only with `gradle runClient --offline -PcampaignChecks`.
