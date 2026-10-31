#!/bin/sh
# Stop the server cleanly (SIGTERM: the server saves the world and exits). Never uses SIGKILL.
#   ./stop.sh [--timeout SECONDS] [--dry-run] [--root DIR]
DIR=$(cd "$(dirname "$0")" && pwd); ROOT=; TIMEOUT=120; GG_DRY=0
while [ $# -gt 0 ]; do
  case $1 in
    --timeout) TIMEOUT=$2; shift;; --dry-run) GG_DRY=1;; --root) ROOT=$2; shift;;
    -h|--help) sed -n '2,4p' "$0"; exit 0;;
    *) echo "Unknown option: $1" >&2; exit 2;;
  esac; shift
done
case $TIMEOUT in ''|*[!0-9]*) echo "--timeout must be a number." >&2; exit 2;; esac
. "$DIR/lib.sh"; gg_init "$DIR" "$ROOT"
if ! gg_alive; then
  gg_say "Server is not running."
  if [ -f "$GG_PIDFILE" ] && ! gg_dry; then rm -f "$GG_PIDFILE"; fi
  exit 0
fi
PID=$(tr -d '\r\n ' < "$GG_PIDFILE")
if gg_dry; then gg_say "[dry-run] would send SIGTERM to pid $PID and wait up to ${TIMEOUT}s"; exit 0; fi
gg_say "Stopping pid $PID (saving the world)..."
kill -TERM "$PID"
i=0
while kill -0 "$PID" 2>/dev/null; do
  i=$((i+1))
  if [ "$i" -gt "$TIMEOUT" ]; then gg_die "Still running after ${TIMEOUT}s. Not forcing it. Look at the console; as a last resort kill it yourself, then restore from backups/ if the world is damaged."; fi
  sleep 1
done
rm -f "$GG_PIDFILE"
gg_say "Stopped."
