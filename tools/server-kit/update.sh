#!/bin/sh
# Update loop (task 2A.8): stop, back up, swap the mod jar, pin it, optionally restart.
#   ./update.sh --jar PATH [--restart] [--memory-gb N] [--dry-run] [--root DIR]
# The new jar is checked first (mod id, Minecraft 1.20.1, build record). The old jar is moved to
# mods-retired/, never deleted. Running this script is the human approval that re-pins mod-lock.json.
# After a restart, join 25580 yourself as the smoke test. Maps and structures live in the world,
# so a map change is a restore from a world archive (./restore.sh), not part of this script.
DIR=$(cd "$(dirname "$0")" && pwd); ROOT=; NEW=; RESTART=0; MEM=6; GG_DRY=0
while [ $# -gt 0 ]; do
  case $1 in --root|--jar|--backup|--label|--keep|--memory-gb|--jvm-args|--timeout) [ $# -ge 2 ] || { echo "$1 needs a value." >&2; exit 2; };; esac
  case $1 in
    --jar) NEW=$2; shift;; --restart) RESTART=1;; --memory-gb) MEM=$2; shift;;
    --dry-run) GG_DRY=1;; --root) ROOT=$2; shift;;
    -h|--help) sed -n '2,/^[^#]/{/^#/p;}' "$0"; exit 0;;
    *) echo "Unknown option: $1" >&2; exit 2;;
  esac; shift
done
[ -n "$NEW" ] || { echo "--jar PATH is required." >&2; exit 2; }
# Resolve a relative --jar against where you ran this, before moving into the server folder.
case $NEW in /*) ;; *) [ ! -f "$NEW" ] || NEW=$(cd "$(dirname "$NEW")" && pwd)/$(basename "$NEW");; esac
export GG_DRY GG_ALLOW_RUNNING=0
. "$DIR/lib.sh"; gg_init "$DIR" "$ROOT"
cd "$GG_ROOT" || exit 1
gg_inspect "$NEW"; NEWJAR=$GG_JAR; NEWVER=$GG_JAR_VERSION; NEWBUILD=$GG_JAR_BUILD; NEWSHA=$GG_JAR_SHA
gg_say "New jar OK: $(basename "$NEWJAR") version $NEWVER build $(gg_short "$NEWBUILD")"
mkdir -p mods
OLD=
gg_find_jar_quiet() { _c=0; for _f in mods/graves-and-glory*.jar mods/gravesandglory*.jar mods/bannerbound*.jar; do [ -f "$_f" ] && { _c=$((_c+1)); OLD=$_f; }; done; [ "$_c" -le 1 ] || gg_die "More than one Graves and Glory jar in mods/; fix by hand first."; }
gg_find_jar_quiet
if [ -n "$OLD" ]; then
  _oldsha=$(gg_sha256 "$OLD")
  [ "$_oldsha" != "$NEWSHA" ] || { gg_say "mods/ already holds this exact jar. Nothing to do."; exit 0; }
fi

if gg_alive; then
  if gg_dry; then gg_say "[dry-run] would run ./stop.sh"; else "$DIR/stop.sh" --root "$GG_ROOT" || gg_die "Could not stop the server; nothing was changed."; fi
fi
# The server is stopped, so a missing or failed backup aborts before any file moves.
if gg_dry; then GG_ALLOW_RUNNING=1; fi
gg_backup preupdate
if gg_dry; then
  gg_say "[dry-run] would move ${OLD:-no old jar} to mods-retired/, copy $(basename "$NEWJAR") into mods/, pin it"
  [ "$RESTART" = 1 ] && gg_say "[dry-run] would restart with ./start.sh --memory-gb $MEM"
  gg_say "[dry-run] nothing was changed."; exit 0
fi

STAMP=$(gg_stamp); mkdir -p mods-retired
RETIRED=
if [ -n "$OLD" ]; then RETIRED=mods-retired/$(basename "$OLD").$STAMP; mv "$OLD" "$RETIRED" || gg_die "Could not retire the old jar."; fi
DEST=mods/graves-and-glory-$NEWVER.jar
if ! cp "$NEWJAR" "$DEST.tmp" || ! mv "$DEST.tmp" "$DEST"; then
  rm -f "$DEST.tmp"; [ -n "$RETIRED" ] && mv "$RETIRED" "$OLD"
  gg_die "Copy failed; the old jar was put back."
fi
gg_inspect "$DEST"; gg_pin
gg_gate
gg_say "Updated to $NEWVER. Old jar kept at ${RETIRED:-<none>}."
if [ "$RESTART" = 1 ]; then
  mkdir -p logs
  nohup "$DIR/start.sh" --root "$GG_ROOT" --memory-gb "$MEM" >> logs/console.log 2>&1 &
  gg_say "Restart launched in the background (console log: logs/console.log). Now join 25580 as the smoke test."
else
  gg_say "Start it with ./start.sh, then join 25580 as the smoke test."
fi
