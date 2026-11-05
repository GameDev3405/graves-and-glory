#!/bin/sh
# Version gate. Refuses a missing, duplicate, wrong or unpinned Graves and Glory jar.
#   ./gate.sh           check mods/ against mod-lock.json (exit 1 on mismatch)
#   ./gate.sh --pin     record the jar now in mods/ as the approved build (a human decision)
#   --root DIR          use DIR instead of this script's folder
#   --dry-run           with --pin: report, write nothing
DIR=$(cd "$(dirname "$0")" && pwd); ROOT=; PIN=0; GG_DRY=0
while [ $# -gt 0 ]; do
  case $1 in --root|--jar|--backup|--label|--keep|--memory-gb|--jvm-args|--timeout) [ $# -ge 2 ] || { echo "$1 needs a value." >&2; exit 2; };; esac
  case $1 in
    --pin) PIN=1;; --dry-run) GG_DRY=1;; --root) ROOT=$2; shift;;
    -h|--help) sed -n '2,/^[^#]/{/^#/p;}' "$0"; exit 0;;
    *) echo "Unknown option: $1" >&2; exit 2;;
  esac; shift
done
. "$DIR/lib.sh"; gg_init "$DIR" "$ROOT"
if [ "$PIN" = 1 ]; then
  gg_find_jar; gg_inspect "$GG_JAR_PATH"
  gg_say "Jar ${GG_JAR_PATH##*/}: version $GG_JAR_VERSION, build $GG_JAR_BUILD"
  gg_pin
else
  gg_gate
fi
