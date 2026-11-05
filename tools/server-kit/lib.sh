#!/bin/sh
# Shared helpers for the Graves and Glory server kit. Sourced by the other scripts; not run directly.
# Needs: sh, tar, unzip (or python3), sha256sum (or shasum), java 17+ for start.

KIT_MC=1.20.1
KIT_MOD_ID=gravesandglory
KIT_PORT=25580

gg_die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
gg_say() { printf '[server-kit] %s\n' "$*"; }
gg_warn() { printf 'WARNING: %s\n' "$*" >&2; }
gg_dry() { [ "${GG_DRY:-0}" = 1 ]; }
gg_short() { printf '%s' "$1" | cut -c1-12; }

# gg_init <script-dir> <root-override-or-empty>
gg_init() {
  _root=${2:-$1}
  GG_ROOT=$(cd "$_root" 2>/dev/null && pwd) || gg_die "Server directory not found: $_root"
  GG_PROPS=$GG_ROOT/server.properties
  GG_LOCK=$GG_ROOT/mod-lock.json
  GG_PIDFILE=$GG_ROOT/server.pid
  GG_BACKUPS=$GG_ROOT/backups
}

gg_stamp() { date -u +%Y%m%dT%H%M%SZ; }

gg_prop() { # gg_prop <key> [default]
  _v=
  if [ -f "$GG_PROPS" ]; then _v=$(grep -E "^$1=" "$GG_PROPS" | tail -n 1 | cut -d= -f2- | tr -d '\r'); fi
  if [ -n "$_v" ]; then printf '%s' "$_v"; else printf '%s' "${2:-}"; fi
}

gg_world() { gg_prop level-name "Graves and Glory"; }

gg_sha256() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d' ' -f1
  elif command -v shasum >/dev/null 2>&1; then shasum -a 256 "$1" | cut -d' ' -f1
  else gg_die "Need sha256sum or shasum."; fi
}

# Print a file inside a zip/jar to stdout, or nothing.
gg_zip_read() { # <zip> <entry>
  if command -v unzip >/dev/null 2>&1; then unzip -p "$1" "$2" 2>/dev/null
  elif command -v python3 >/dev/null 2>&1; then
    python3 -I -c 'import sys,zipfile
try: sys.stdout.write(zipfile.ZipFile(sys.argv[1]).read(sys.argv[2]).decode("utf8"))
except Exception: pass' "$1" "$2"
  else gg_die "Need unzip or python3 to read the mod jar."; fi
}

# gg_json <json-text> <key>: first string value for "key". Good enough for fabric.mod.json and our lock file.
gg_json() { printf '%s' "$1" | tr -d '\r\n' | grep -oE "\"$2\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" | head -n 1 | sed -E 's/^[^:]*:[[:space:]]*"(.*)"$/\1/'; }

gg_alive() { # true when server.pid names a live java process
  [ -f "$GG_PIDFILE" ] || return 1
  _p=$(tr -d '\r\n ' < "$GG_PIDFILE")
  case $_p in ''|*[!0-9]*) return 1;; esac
  kill -0 "$_p" 2>/dev/null || return 1
  # Guard against a recycled pid: the process must be called java. Linux: /proc (no ps needed, works in
  # minimal containers). Elsewhere: ps, trusted only when it prints exactly one line (some ps builds ignore -o).
  _n=
  if [ -r "/proc/$_p/comm" ]; then _n=$(cat "/proc/$_p/comm" 2>/dev/null)
  elif command -v ps >/dev/null 2>&1; then
    _n=$(ps -p "$_p" -o comm= 2>/dev/null)
    [ "$(printf '%s
' "$_n" | wc -l)" -le 1 ] || _n=
  fi
  if [ -n "$_n" ]; then case $_n in *java*) ;; *) return 1;; esac; fi
  return 0
}

# gg_inspect <jar>: check the jar itself (id, Minecraft version, build record) and set
# GG_JAR, GG_JAR_VERSION, GG_JAR_BUILD, GG_JAR_SHA.
gg_inspect() {
  _j=$1
  [ -f "$_j" ] || gg_die "Mod jar not found: $_j"
  _meta=$(gg_zip_read "$_j" fabric.mod.json)
  [ -n "$_meta" ] || gg_die "$_j has no fabric.mod.json; it is not a Fabric mod jar."
  _id=$(gg_json "$_meta" id)
  [ "$_id" = "$KIT_MOD_ID" ] || gg_die "$_j is mod id '$_id', expected '$KIT_MOD_ID'."
  GG_JAR_VERSION=$(gg_json "$_meta" version)
  case $GG_JAR_VERSION in [0-9]*.[0-9]*.[0-9]*) ;; *) gg_die "$_j has an unusable version '$GG_JAR_VERSION'.";; esac
  _mc=$(gg_json "$_meta" minecraft)
  [ "$_mc" = "$KIT_MC" ] || gg_die "$_j targets Minecraft '$_mc'; this kit runs $KIT_MC."
  _build=$(gg_zip_read "$_j" gravesandglory-build.json)
  [ -n "$_build" ] || gg_die "$_j has no gravesandglory-build.json (not a release build); rebuild with gradlew build."
  GG_JAR_BUILD=$(gg_json "$_build" sourceSha256)
  case $GG_JAR_BUILD in *[!0-9a-f]*|'') gg_die "$_j has an invalid build identity.";; esac
  [ ${#GG_JAR_BUILD} -eq 64 ] || gg_die "$_j has an invalid build identity length."
  [ "$(gg_json "$_build" version)" = "$GG_JAR_VERSION" ] || gg_die "$_j build record version differs from its mod version; rebuild."
  GG_JAR=$_j
  GG_JAR_SHA=$(gg_sha256 "$_j")
}

# gg_pin: record the inspected jar as the approved one. A human decision: only gate.sh --pin and update.sh call it.
gg_pin() {
  if gg_dry; then gg_say "[dry-run] would write mod-lock.json for $GG_JAR_VERSION build $(gg_short "$GG_JAR_BUILD")"; return 0; fi
  if [ -f "$GG_LOCK" ]; then cp "$GG_LOCK" "$GG_LOCK.prev"; fi
  printf '{\n  "modId": "%s",\n  "minecraft": "%s",\n  "version": "%s",\n  "sourceSha256": "%s",\n  "jarSha256": "%s"\n}\n' \
    "$KIT_MOD_ID" "$KIT_MC" "$GG_JAR_VERSION" "$GG_JAR_BUILD" "$GG_JAR_SHA" > "$GG_LOCK.tmp" && mv "$GG_LOCK.tmp" "$GG_LOCK"
  gg_say "Pinned mod $GG_JAR_VERSION (build $(gg_short "$GG_JAR_BUILD"))."
}

# gg_find_jar: set GG_JAR_PATH to the single runtime jar in mods/, or refuse.
gg_find_jar() {
  _mods=$GG_ROOT/mods
  [ -d "$_mods" ] || gg_die "mods/ is missing in $GG_ROOT."
  _n=0; GG_JAR_PATH=
  for _f in "$_mods"/*; do
    [ -f "$_f" ] || continue
    _b=${_f##*/}
    case $_b in
      *DO-NOT-SHIP*) gg_die "Refusing test-tool jar in mods/: $_b";;
      graves-and-glory*-sources.jar|graves-and-glory*-dev.jar) gg_die "Refusing non-runtime jar in mods/: $_b";;
      graves-and-glory*.jar|gravesandglory*.jar|bannerbound*.jar) _n=$((_n+1)); GG_JAR_PATH=$_f;;
    esac
  done
  [ "$_n" -ge 1 ] || gg_die "No Graves and Glory jar in mods/. Copy graves-and-glory-<version>.jar there."
  [ "$_n" -eq 1 ] || gg_die "$_n Graves and Glory jars in mods/. Keep exactly one; move old ones out."
}

# gg_gate: refuse a missing, duplicate, wrong, or unpinned mod jar.
gg_gate() {
  gg_find_jar
  gg_inspect "$GG_JAR_PATH"
  [ -f "$GG_LOCK" ] || gg_die "No mod-lock.json. After confirming ${GG_JAR_PATH##*/} is the approved build, run: ./gate.sh --pin"
  _lock=$(cat "$GG_LOCK")
  _lv=$(gg_json "$_lock" version); _lb=$(gg_json "$_lock" sourceSha256); _lj=$(gg_json "$_lock" jarSha256)
  [ "$_lv" = "$GG_JAR_VERSION" ] || gg_die "Version gate: jar is $GG_JAR_VERSION but the lock says $_lv. Refusing."
  [ "$_lb" = "$GG_JAR_BUILD" ] || gg_die "Version gate: jar build $(gg_short "$GG_JAR_BUILD") differs from the locked build $(gg_short "$_lb"). Refusing."
  [ "$_lj" = "$GG_JAR_SHA" ] || gg_die "Version gate: jar bytes differ from the locked file (rebuilt or altered). Refusing."
  gg_say "Version gate OK: ${GG_JAR_PATH##*/}, version $GG_JAR_VERSION, build $(gg_short "$GG_JAR_BUILD"), Minecraft $KIT_MC."
}

# gg_backup <label>: verified archive of the world. Never overwrites an archive. Sets GG_BACKUP_FILE.
gg_backup() {
  _label=$1; _w=$(gg_world); GG_BACKUP_FILE=
  if [ ! -d "$GG_ROOT/$_w" ]; then gg_say "No world '$_w' yet; nothing to back up."; return 0; fi
  if gg_alive && [ "${GG_ALLOW_RUNNING:-0}" != 1 ]; then gg_die "Server is running. Stop it first (./stop.sh), or pass --allow-running to accept a possibly inconsistent copy."; fi
  _out=$GG_BACKUPS/$(printf '%s' "$_w" | tr -c 'A-Za-z0-9._-' '_')-$(gg_stamp)-$_label.tar.gz
  [ ! -e "$_out" ] || gg_die "Backup already exists, refusing to overwrite: $_out"
  if gg_dry; then gg_say "[dry-run] would archive '$_w' to $_out"; GG_BACKUP_FILE=$_out; return 0; fi
  mkdir -p "$GG_BACKUPS"
  tar -czf "$_out.partial" -C "$GG_ROOT" "$_w" || { rm -f "$_out.partial"; gg_die "Backup failed; nothing was changed."; }
  tar -tzf "$_out.partial" > "$_out.list" || { rm -f "$_out.partial" "$_out.list"; gg_die "Backup could not be read back."; }
  grep -q "^$_w/level.dat\$" "$_out.list" || { rm -f "$_out.partial" "$_out.list"; gg_die "Backup has no level.dat; refusing to trust it."; }
  rm -f "$_out.list"; mv "$_out.partial" "$_out"
  GG_BACKUP_FILE=$_out
  gg_say "Backup written and verified: $_out"
  case $_label in start|auto) gg_prune "$_label" "${GG_KEEP:-20}";; esac
}

# Prune only start/auto archives beyond the newest <keep>. Manual, pre-update and pre-restore archives are never pruned.
gg_prune() {
  _i=0
  # Archive names never contain spaces, but the server folder may, so list from inside backups/.
  for _f in $(cd "$GG_BACKUPS" && ls -1 ./*-"$1".tar.gz 2>/dev/null | sort -r); do
    _i=$((_i+1))
    if [ "$_i" -gt "$2" ]; then rm -f "$GG_BACKUPS/$_f"; gg_say "Pruned old $1 backup: ${_f#./}"; fi
  done
}

gg_jvm_args() { # <inline>; flags from jvm-args.txt (one per line, # comments) then the inline text, space separated; each must start with "-"
  _out=
  if [ -f "$GG_ROOT/jvm-args.txt" ]; then
    _file=$(sed -e 's/#.*$//' -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' "$GG_ROOT/jvm-args.txt" | grep -v '^$' | tr '
' ' ')
    _out="$_file"
  fi
  _out="$_out $1"
  for _f in $_out; do
    case $_f in -*) ;; *) echo "JVM flag '$_f' must start with '-' (jvm-args.txt or --jvm-args)."; return 1;; esac
  done
  printf '%s' "$(echo $_out)"
}
gg_aikar_flags() { # <memory-gb>; Aikar's G1 flags, the large-heap variant from 12 GB up
  if [ "$1" -ge 12 ]; then _new=40; _max=50; _region=16M; _res=15; _ihop=20
  else _new=30; _max=40; _region=8M; _res=20; _ihop=15; fi
  printf '%s' "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=$_new -XX:G1MaxNewSizePercent=$_max -XX:G1HeapRegionSize=$_region -XX:G1ReservePercent=$_res -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=$_ihop -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gg -Daikars.new.flags=true"
}
