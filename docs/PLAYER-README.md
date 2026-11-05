# Graves and Glory

Graves and Glory is a Fabric mod for Minecraft: Java Edition 1.20.1. You collect troops, arrange an army of twenty, command it in real time and fight over objectives on large fantasy battlefields, alone or with friends.

## What you need

- Minecraft: Java Edition 1.20.1.
- Java 17 or newer.
- Fabric Loader 0.16.14 and Fabric API 0.92.2. The `.mrpack` and the plain zip both include Fabric API. A launcher sets up Fabric Loader when it imports the `.mrpack`; with the plain zip you install Fabric Loader yourself.
- Everyone in the same game must run the same Graves and Glory build. A client with a different build is told to update before it can enter.

## Install

Pick one.

1. **`.mrpack` (recommended).** Import the `.mrpack` file into a launcher that supports it (Prism Launcher, the Modrinth app and others). The launcher creates a Minecraft 1.20.1 Fabric instance with the mod, Fabric API, the shaders and the Excalibur resource pack. Start that instance.
2. **Plain zip.** Install Fabric Loader 0.16.14 for Minecraft 1.20.1 yourself, then copy the zip's `mods`, `shaderpacks` and `resourcepacks` folders into your instance's game folder.
3. **Dedicated server.** Use the server kit zip. It does not accept the Minecraft server EULA for you; read it and supply `eula.txt` yourself.

The shaders and Excalibur are optional. Excalibur is in `resourcepacks` and is switched on for you the first time you start the game. The game's own armory and weapons packs sit above it, so weapons keep their own look. Turn Excalibur off in Options, Resource Packs if you prefer.

## Start a battle

1. Start the game and choose Enter camp on the title menu. You arrive in a walled courtyard.
2. Open the War Table to manage your troops, gear, banner and Pact. Press I in camp for the Arsenal, where you equip gear and relics.
3. Arrange your army, then start a match from the War Table lobby. The host picks the map and the mode; empty seats fill with Easy, Normal or Hard bots; a 10-second countdown starts the round.
4. For a first match, pick the Campaign (battle 1 teaches command) or Practice.

You can play solo against bots or with friends on a LAN or a server.

## Saves and updates

Your collection and camp save with your Minecraft player profile. Older saves migrate on first load, and the original file is kept beside the new one. Keep a backup before upgrading, and do not go back to an older build after playing with gear: older builds drop the gear fields from your commander save.

## More information

- `RELEASE-<version>.txt` in this folder: what is in this release and what is not.
- `THIRD_PARTY.md`: credits, authors and licences for everything bundled (mods, shaders, fonts and the Excalibur resource pack).
- `AUDIO-CREDITS.md`: the author of each sound file.
- `third-party/`: the licence notices and library sources.
- `LICENSE`: the licence for the game's own code.

This free Minecraft modpack is not endorsed by Mojang, Microsoft or any author credited in `THIRD_PARTY.md`.
