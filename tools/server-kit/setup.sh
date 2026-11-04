#!/bin/sh
# Download the official Fabric server launcher for Minecraft 1.20.1 (Fabric Loader 0.16.14) and check its SHA-256.
# Does not start the server and accepts no terms (the Minecraft EULA is yours to read: start.sh explains).
# Refuses to replace a launcher that is already there.
set -eu
DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$DIR"
URL='https://meta.fabricmc.net/v2/versions/loader/1.20.1/0.16.14/1.1.2/server/jar'
SHA256='856a5f9fe4809ac1972778ff5bac71f8995b7ae0513caa6d0be94288b92cf113'
[ ! -e fabric-server-launch.jar ] || { echo "Launcher already exists; preserve it or move it aside before setup." >&2; exit 1; }
rm -f fabric-server-launch.jar.download
if command -v curl >/dev/null 2>&1; then curl -fL -o fabric-server-launch.jar.download "$URL"
elif command -v wget >/dev/null 2>&1; then wget -O fabric-server-launch.jar.download "$URL"
else echo "Need curl or wget to download the launcher." >&2; exit 1; fi
if command -v sha256sum >/dev/null 2>&1; then GOT=$(sha256sum fabric-server-launch.jar.download | cut -d' ' -f1)
elif command -v shasum >/dev/null 2>&1; then GOT=$(shasum -a 256 fabric-server-launch.jar.download | cut -d' ' -f1)
else echo "Need sha256sum or shasum to verify the launcher." >&2; exit 1; fi
if [ "$GOT" != "$SHA256" ]; then rm -f fabric-server-launch.jar.download; echo "Fabric download hash mismatch. Do not run it." >&2; exit 1; fi
mv fabric-server-launch.jar.download fabric-server-launch.jar
echo "Fabric launcher downloaded and verified. Next: ./gate.sh --pin, then ./start.sh (see README.md)."
