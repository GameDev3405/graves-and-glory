Graves and Glory 0.3.4 - local dedicated-server candidate

Requires Minecraft Java Edition, Java 17+ (Java 21 used in verification), and
Fabric Loader 0.16.14 for Minecraft 1.20.1. This kit contains the original game
mod and four server dependencies. Rendering, shader and animation mods are absent.
Third-party notices/sources are in docs/. Minecraft/Java/Fabric launcher binaries,
personal saves, profiles, credentials and EULA acceptance are not included.

WINDOWS SETUP
1. Extract into a new dedicated folder. Keep your existing server/saves backed up.
2. Run setup-server.ps1. It downloads the official pinned Fabric launcher and
   checks SHA-256. It does not start the server or accept any terms.
3. Read https://aka.ms/MinecraftEULA yourself. If you agree, supply eula.txt
   containing eula=true. The project never supplies your agreement for you.
4. Run start-server.ps1 (default maximum heap 8 GB, initial heap 2 GB).
   Do not allocate all 96 GB to one process. Measure tick time before scaling.
   The first start downloads Minecraft and Fabric libraries from their providers.
5. Wait for world preparation. The first battlefield is built after Minecraft's
   initial Done message; it can take time and cause startup tick-lag warnings.
6. Join 127.0.0.1:25565 from this PC using the exact same client release/build.
   Use stop in the server console for a normal save and shutdown.

WORLD AND NETWORK
Keep level-name=Graves and Glory. Activation currently checks the saved world's
internal level name (Graves and Glory or legacy Bannerbound), not just its folder.
A random existing world is not automatically converted into this game.
Default configuration binds localhost, uses online-mode=true, allows eight
players and disables RCON. It is not an Internet/home deployment configuration.
Starlink public reachability still needs an outbound relay/public-edge assessment.
No router, firewall, scheduled startup or public endpoint is configured here.

CURRENT LIMITS
One shared battlefield per server process. Local-profile imports are still
trusted; centrally secured official progression and public matchmaking are
unfinished. Do not treat this candidate as a finished public competitive service.
The production headless startup/lobby were checked locally; an authenticated
remote client match and the actual home-server PC still need verification.
