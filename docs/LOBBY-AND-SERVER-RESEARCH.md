# Lobby and server research

Status: research and recommendation only. No code changed. Written 2026-10-16.

Scope: replace the first-joiner host with (a) ready pads and (b) a custom lobby with invites, and collect server optimisation options for the home server.

## 1. Current code

| Piece | Where | Behaviour |
|---|---|---|
| Host set | `Campaign.java:66` | First joiner becomes host when there is none |
| Host hand-over | `Campaign.java:77`, called from `GravesAndGlory.java:270` | When host leaves, next online player becomes host |
| Host-only controls | `Campaign.java:79, 104, 110, 112, 117` | Mode, map, bot fill, start, return to lobby |
| Start | `Campaign.java:116-120` | Host only. Needs bot fill or a full roster, and each human needs one troop. Then `startRound()` |
| Team choice | `GravesAndGlory.java:297-306` (`team:dawn` / `team:dusk`) | Buttons in the lobby screen, `CampaignLobbyScreen.java:28` |
| Join placement | `GravesAndGlory.java:255-265` | Auto-balances to the smaller team. Mid-battle joiner takes a bot's squad |
| Lobby UI | `CampaignLobbyScreen.java:22-29, 54-55, 83` | Host sees enabled controls. Others see "The host chooses the battlefield..." |
| Host flag to client | `CampaignClient.java:18`, `Campaign.java:181` | `host` is sent in the state JSON |
| Arena flag | `Campaign.java:113`, `GravesAndGlory.java:288` | `b.ready` means the arena is built. It does not mean a player is ready. Do not reuse the name |

Two facts that shape the design:

- **One match at a time.** There is one `Battle` object and one `running` flag. Custom lobbies and quick-match pads must feed the same lobby. Two simultaneous matches would need several `Battle` instances, a large change.
- **Host hand-over already exists** (`Campaign.java:77`). The 0.3.0 plan file says the host is never reassigned. That is out of date.

## 2. Ready pads (quick match)

Proposal:

- Two team pads in the lobby, Dawn and Dusk. Standing on a pad picks that side and readies you. This replaces the Join Dawn and Join Dusk buttons.
- Each pad is an axis-aligned box defined in code, with coloured marker blocks placed when the lobby is built.
- Check positions every 10 ticks (0.5 s) from the server `end_tick` hook (`Spec.java:18` lists it), only while `!running`.
- Ready state lives on the server per player. Send it to the client for a "3/8 ready" line.
- Countdown: 10 s proposed. Starts when at least 2 humans are on pads (proposed) and nobody is still walking in. Any player stepping off cancels it.
- At zero, run the same checks as `start` (`Campaign.java:117-119`), then `startRound()`. Bot fill covers empty slots.
- Mode and map for quick match: fixed campaign defaults (`Campaign.java:20`, `size` and `map`). No host needed.

Implementation notes:

- New `LobbyPads` class, ticked from the `GravesAndGlory` tick while not running.
- One new server-to-client packet for ready state.
- No layout marker is needed. The lobby is rebuilt on every server start (`CampaignMaps.lobby`, called from the Battle constructor at `GravesAndGlory.java:193`), so pad changes take effect at the next start. The `gravesandglory-layout.json` marker governs only the arena build.
- The WorldGuard and region-event plugins found in search are Paper or Bukkit plugins. This server runs Fabric, so they cannot be used. The box check goes in the mod.

## 3. Custom lobby (party with invites)

Proposal:

- A custom lobby block in the lobby (for example a lectern). Right-click opens the party screen.
- The party screen lists online players who are not in a party, each with an Invite button. Invites expire after 60 s (proposed).
- The invitee sees Accept and Decline on their lobby screen.
- The player who opens the party is its leader. Only the leader gets mode, map, bot fill and START. These are the current host controls, moved here.
- Team size per side is capped at the chosen mode size.
- One party at a time, because only one match runs.

Wiring:

- Reuse the existing text command channel (`send("...")`, handled in `Campaign.handle`) for invite, accept, decline, kick, mode, map, fill and start.
- Two new packets: open party screen (server to client) and invite received (server to client).

Party and pads: exclusive (proposed). A party member cannot stand on a pad. Leaving the party puts you back on pads.

## 4. Removing the host

| Where | Change |
|---|---|
| `Campaign.java:19` | Remove `host`. Add party leader for custom lobbies only |
| `Campaign.java:66` | `joined` stops assigning a host |
| `Campaign.java:75-77` | Remove `host()` and `left()`. Party leave replaces them: leader leaves, leadership passes to the next member, party closes when empty |
| `Campaign.java:79, 104, 110, 112, 117` | Quick match is open to everyone. Party controls check the leader |
| `GravesAndGlory.java:270` | Call party leave instead of `left` |
| `CampaignClient.java:18`, `Campaign.java:181` | Rename the host flag to party leader |
| `CampaignLobbyScreen.java:28-29, 54-55, 83` | Team choice moves to pads. START moves to the party tab. Rewrite the hint text |

Changed packets mean host and clients must run the same build. Bump the protocol version when this lands.

## 5. Server optimisation

### JVM

- Test used Java 21 (Adoptium 21.0.11). One guide maps Java 17 to 1.18 through 1.20.4 and Java 21 from 1.20.5. Either works for 1.20.1. Keep 21.
- Use G1GC with Aikar's flags from the Paper docs page. Copy the current list from there. The flags are plain JVM options, so they should apply to Fabric too. That is my inference; the sources do not say it.
- Heap: set `-Xms` equal to `-Xmx`. Paper's docs say to use at least 6 to 10 GB even with few players. Start at 6G and confirm against the home PC's RAM.
- Example launch (the jar name depends on the Fabric installer):
  `java -Xms6G -Xmx6G <Aikar flags> -jar fabric-server-launch.jar --nogui`

### Mods (server side, first pass)

| Mod | Why | Note |
|---|---|---|
| Lithium | Game-logic optimisation without changing vanilla rules | Ranked among the most valuable in guides |
| FerriteCore | Lower RAM use | Modest saving |
| ModernFix | Bug fixes and faster startup | |
| Krypton | Network stack optimisation | |
| Chunky | Pre-generate the four battlefield maps | About 1,400 chunks per 700 x 500 map before margin |
| Spark | Profiler to measure before and after | Use it before changing settings |

Skip for now:

- **Starlight.** Archived on Modrinth. Only third-party 1.20.1 builds were found. Guides do not list it for 1.20.1.
- **Alternate Current.** Redstone only. Not used here.
- **Structure Layout Optimizer.** No large modded structures.

Not verified: licences, exact 1.20.1 build dates, and whether these mods conflict with each other or with graves-and-glory. Check each Modrinth page before shipping.

### server.properties

Current values, from `run-server/server.properties`:

- `view-distance=10`, `simulation-distance=10`: keep until Spark shows chunk or tick cost.
- `entity-broadcast-range-percentage=100`: keep. Lowering it cuts traffic but hides distant units, which hurts a battle game.
- `sync-chunk-writes=true`: keep. Safer writes.
- `max-players=20`: fine.

Baseline from the test run: "Done (11.112s)" at startup, and "82 ticks behind" during spawn preparation. Measure a bot-filled 2v2 match with Spark, record tick time, then change one setting at a time.

## 6. Decisions for Kort

1. Team pads replace the Join buttons? (proposed: yes)
2. Start rule: 2 humans minimum, 10 s countdown, cancelled by stepping off? (proposed)
3. Quick-match map: fixed default, or random among the four? (proposed: random)
4. Party invites: any online player? (proposed: yes)
5. Party and pads exclusive? (proposed: yes)
6. One match at a time, accepted? (proposed: yes. Multi-match is a large change)
7. Home server RAM. This sets the heap size.
8. Approve the mod set in section 5 after checking licences.

## Sources

- Aikar's flags, Paper docs: https://docs.papermc.io/paper/aikars-flags/
- Java version by Minecraft version, TopModdedServers: https://topmoddedservers.com/en/guides/arguments-java-serveur-minecraft
- Fabric performance guide, GameHostBros: https://guides.gamehostbros.com/games/minecraft/fabric-performance-guide
- Server optimisation, Modrinth support: https://support.modrinth.com/en/articles/13025809-server-optimization
- Starlight archived note, Mado Hosting: https://www.mado-hosting.com/en/minecraft-mods/starlight
- RegionEvents (Paper 1.21.1+): https://modrinth.com/plugin/regionevents
- WorldGuard Region Events (Bukkit/Spigot): https://www.curseforge.com/minecraft/bukkit-plugins/worldguard-region-events
