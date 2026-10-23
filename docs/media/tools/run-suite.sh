#!/bin/bash
# usage: run-suite.sh <suite> [extra env as KEY=VALUE ...]   Runs one harness suite in the isolated shoot-run folder, in the foreground.
SUITE="$1"; shift
for kv in "$@"; do export "$kv"; done
export JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
G=$(ls -d ~/.gradle/wrapper/dists/gradle-8.8-bin/*/gradle-8.8/bin/gradle)
ROOT="C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-Melty-bannerbound/c7170aca-b331-4cc8-a7cf-3a21e5c511b4/scratchpad/shoot-run"
export GRAVES_ROADMAP_TEST_ROOT="$ROOT"
export GRAVES_PHASE1_CHECKS="$SUITE"
cd C:/Users/kortt/Melty/bannerbound-shoot || exit 1
"$G" runClient --offline -PcampaignChecks --init-script test-tools/isolated-checks.gradle --args="--gameDir $ROOT/run" > "$ROOT/gradle-$SUITE.log" 2>&1
echo "exit $?" >> "$ROOT/gradle-$SUITE.log"
