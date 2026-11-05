#!/bin/sh
# Verified world backup to backups/<world>-<UTC stamp>-<label>.tar.gz. Never overwrites an archive.
#   ./backup.sh [--label NAME] [--keep N] [--allow-running] [--dry-run] [--root DIR]
# Labels start and auto are pruned to the newest N (default 20). Any other label is kept forever.
DIR=$(cd "$(dirname "$0")" && pwd); ROOT=; LABEL=manual; GG_DRY=0; GG_ALLOW_RUNNING=0; GG_KEEP=20
while [ $# -gt 0 ]; do
  case $1 in --root|--jar|--backup|--label|--keep|--memory-gb|--jvm-args|--timeout) [ $# -ge 2 ] || { echo "$1 needs a value." >&2; exit 2; };; esac
  case $1 in
    --label) LABEL=$2; shift;; --keep) GG_KEEP=$2; shift;;
    --allow-running) GG_ALLOW_RUNNING=1;; --dry-run) GG_DRY=1;; --root) ROOT=$2; shift;;
    -h|--help) sed -n '2,/^[^#]/{/^#/p;}' "$0"; exit 0;;
    *) echo "Unknown option: $1" >&2; exit 2;;
  esac; shift
done
case $LABEL in ''|*[!A-Za-z0-9]*) echo "Label must be letters and digits only." >&2; exit 2;; esac
case $GG_KEEP in ''|*[!0-9]*) echo "--keep must be a number." >&2; exit 2;; esac
export GG_KEEP GG_ALLOW_RUNNING
. "$DIR/lib.sh"; gg_init "$DIR" "$ROOT"
gg_backup "$LABEL"
