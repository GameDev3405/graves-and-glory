#!/bin/bash
# usage: work-run.sh <root-name> <suite list>
cd "$(dirname "$0")/.."
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot"
ROOT="$PWD/work/ingame-check/$1"; mkdir -p "$ROOT/run" "$ROOT/evidence"
export GRAVES_ROADMAP_TEST_ROOT="$ROOT" GRAVES_PHASE4_CHECKS="$2"
./gradlew.bat runClient -PlocalIntegration --offline -I test-tools/isolated-checks.gradle > "$ROOT/gradle.log" 2>&1
echo "exit $?" >> "$ROOT/gradle.log"
