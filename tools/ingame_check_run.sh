#!/bin/bash
# usage: work-run.sh <root-name> <suite list>
cd "$(dirname "$0")/.."
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot"
ROOT="$PWD/work/ingame-check/$1"; mkdir -p "$ROOT/run" "$ROOT/evidence"
export GRAVES_ROADMAP_TEST_ROOT="$ROOT" GRAVES_PHASE4_CHECKS="$2"
# Loom fixes the launch classpath before compiling and drops build dirs that do not exist yet,
# so a first run in a fresh worktree would start without the main mod. Build them first.
[ -d build/classes/java/main ] && [ -d build/resources/main ] || ./gradlew.bat classes --offline > "$ROOT/gradle-warmup.log" 2>&1
./gradlew.bat runClient -PlocalIntegration --offline -I test-tools/isolated-checks.gradle > "$ROOT/gradle.log" 2>&1
echo "exit $?" >> "$ROOT/gradle.log"
