#!/bin/bash
# One headless simulation server for the balance method (docs: .superpowers/sdd/2026-10-17-phase-2-houses/balance-method-report.md).
#   tools/balance_sim.sh <name> <port> <gradle -P arguments...>
# Makes a fresh server root work/method/<name>-<HHMMSS> (eula, server.properties with the port, level "Graves and Glory"), runs
#   gradle runServer --offline <arguments> --init-script test-tools/isolated-server.gradle
# with its output in <root>/gradle-run.txt, and prints the root. Examples:
#   tools/balance_sim.sh p1a 25681 -PbotSim=10 -PsimDawn=normal -PsimDusk=normal -PsimMaps=0,1,2,3,4 -PsimRoundSeconds=300 \
#       -PsimBanner0=ironclad_normal -PsimBanner1=tidecallers_normal -PsimFast=true
#   tools/balance_sim.sh duels 25682 -PduelSim=$PWD/work/method/plans/units.txt -PduelShard=0/2 -PsimFast=true
# SIM_ONLINE_MODE=false lets dev clients and qa bots join (default true).
# SIM_LEVEL_SEED=<n> in the environment fixes the world seed (vanilla level-seed; blank = random, the default).
# Results: <root>/botsim/{results,gold,types}.csv and meta.txt, or <root>/duel/{duels,types}.csv and meta.txt.
set -u
name="$1"; port="$2"; shift 2
here="$(cd "$(dirname "$0")/.." && pwd)"
cd "$here" || exit 9
# Other worktrees run servers too: refuse a port something already listens on.
if netstat -ano 2>/dev/null | grep LISTENING | grep -qE "[:.]$port[[:space:]]"; then echo "port $port is in use; pick another"; exit 8; fi
export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
G=$(ls -d ~/.gradle/wrapper/dists/gradle-8.8-bin/*/gradle-8.8/bin/gradle | head -1)
root="$here/work/method/$name-$(date +%H%M%S)"
mkdir -p "$root"
echo "eula=true" > "$root/eula.txt"
cat > "$root/server.properties" <<EOF
level-name=Graves and Glory
motd=Graves and Glory
server-port=$port
online-mode=${SIM_ONLINE_MODE:-true}
max-tick-time=60000
view-distance=10
simulation-distance=10
sync-chunk-writes=true
spawn-protection=16
difficulty=easy
level-type=minecraft\:normal
level-seed=${SIM_LEVEL_SEED:-}
EOF
echo "$*" > "$root/args.txt"
echo "$root"
export GRAVES_ROADMAP_SERVER_ROOT="$root"
skip=(); [ -f work/method/plans/skip-tools-build ] && skip=(-x compileLocalTestTools -x localTestToolsJar)  # another of my servers holds the jar open
# Fresh worktree: build first so Loom puts the main mod on the launch classpath.
[ -d build/classes/java/main ] && [ -d build/resources/main ] || "$G" classes --offline > "$root/gradle-warmup.txt" 2>&1
"$G" runServer --offline "$@" "${skip[@]}" --init-script test-tools/isolated-server.gradle > "$root/gradle-run.txt" 2>&1
echo "gradle exit $? root $root"
