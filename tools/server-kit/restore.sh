#!/bin/sh
# Restore the world from a backup archive made by backup.sh.
#   ./restore.sh --backup backups/<file>.tar.gz [--dry-run] [--root DIR]
# The server must be stopped. The current world is backed up first (label prerestore), then moved aside
# to <world>.replaced-<stamp>. Nothing is deleted or overwritten.
DIR=$(cd "$(dirname "$0")" && pwd); ROOT=; ARCH=; GG_DRY=0
while [ $# -gt 0 ]; do
  case $1 in --root|--jar|--backup|--label|--keep|--memory-gb|--jvm-args|--timeout) [ $# -ge 2 ] || { echo "$1 needs a value." >&2; exit 2; };; esac
  case $1 in
    --backup) ARCH=$2; shift;; --dry-run) GG_DRY=1;; --root) ROOT=$2; shift;;
    -h|--help) sed -n '2,/^[^#]/{/^#/p;}' "$0"; exit 0;;
    *) echo "Unknown option: $1" >&2; exit 2;;
  esac; shift
done
[ -n "$ARCH" ] || { echo "--backup FILE is required." >&2; exit 2; }
# A relative --backup that exists where you ran this means that file; otherwise it is relative to the server folder.
case $ARCH in /*) ;; *) [ ! -f "$ARCH" ] || ARCH=$(cd "$(dirname "$ARCH")" && pwd)/$(basename "$ARCH");; esac
export GG_DRY GG_ALLOW_RUNNING=0
. "$DIR/lib.sh"; gg_init "$DIR" "$ROOT"
cd "$GG_ROOT" || exit 1
[ -f "$ARCH" ] || gg_die "Archive not found: $ARCH"
case $ARCH in /*) ;; *) ARCH=$(pwd)/$ARCH;; esac
W=$(gg_world)
if gg_alive; then gg_die "Server is running. Stop it first (./stop.sh)."; fi
tar -tzf "$ARCH" > /dev/null 2>&1 || gg_die "Archive is unreadable: $ARCH"
tar -tzf "$ARCH" | grep -q "^$W/level.dat\$" || gg_die "Archive does not contain '$W/level.dat'. Wrong world name or wrong archive."
if [ -d "$W" ] && [ ! -f "$W/level.dat" ]; then
  # A damaged world cannot pass the backup check, and restoring over it is the likely reason. It is kept anyway.
  gg_warn "Current world '$W' has no level.dat; skipping the prerestore archive. The world itself is kept as $W.replaced-<stamp>."
else
  gg_backup prerestore
fi
if gg_dry; then gg_say "[dry-run] would extract $ARCH, move '$W' to $W.replaced-<stamp>, install the restored world"; exit 0; fi
TMP=.restore-tmp-$$
rm -rf "$TMP"; mkdir "$TMP" && tar -xzf "$ARCH" -C "$TMP" || { rm -rf "$TMP"; gg_die "Extraction failed; the world is untouched."; }
[ -f "$TMP/$W/level.dat" ] || { rm -rf "$TMP"; gg_die "Extracted world has no level.dat; the world is untouched."; }
ASIDE=$W.replaced-$(gg_stamp)
if [ -d "$W" ]; then mv "$W" "$ASIDE" || { rm -rf "$TMP"; gg_die "Could not move the current world aside."; }; fi
mv "$TMP/$W" "$W" || { [ -d "$ASIDE" ] && mv "$ASIDE" "$W"; gg_die "Install failed; the old world was put back."; }
rm -rf "$TMP"
gg_say "Restored '$W' from $ARCH. Previous world kept at ${ASIDE:-<none>} and in the prerestore backup."
