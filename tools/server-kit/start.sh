#!/bin/sh
# Start the Graves and Glory dedicated server (port 25580) in the foreground.
#   ./start.sh [--memory-gb N] [--dry-run] [--root DIR]
# Order: Java check, version gate, config, EULA check, world backup, launch with Aikar flags.
# Run it under screen, tmux or systemd to keep it alive. Stop it with ./stop.sh.
# --dry-run runs every check and prints the java command; it writes nothing and starts nothing.
DIR=$(cd "$(dirname "$0")" && pwd); ROOT=; MEM=6; GG_DRY=0
while [ $# -gt 0 ]; do
  case $1 in
    --memory-gb) MEM=$2; shift;; --dry-run) GG_DRY=1;; --root) ROOT=$2; shift;;
    -h|--help) sed -n '2,7p' "$0"; exit 0;;
    *) echo "Unknown option: $1" >&2; exit 2;;
  esac; shift
done
case $MEM in ''|*[!0-9]*) echo "--memory-gb must be a whole number." >&2; exit 2;; esac
[ "$MEM" -ge 2 ] && [ "$MEM" -le 64 ] || { echo "--memory-gb must be 2 to 64." >&2; exit 2; }
export GG_DRY
. "$DIR/lib.sh"; gg_init "$DIR" "$ROOT"
cd "$GG_ROOT" || exit 1

[ -f fabric-server-launch.jar ] || gg_die "fabric-server-launch.jar is missing. See README.md, step 3."
command -v java >/dev/null 2>&1 || gg_die "java not found. Install Java 17 or newer (README.md, step 1)."
JV=$(java -version 2>&1 | head -n 1 | sed -nE 's/.*"([0-9]+)[."].*/\1/p')
case $JV in ''|*[!0-9]*) gg_die "Could not read the Java version.";; esac
[ "$JV" -ge 17 ] || gg_die "Java $JV is too old; need 17 or newer."
if gg_alive; then gg_die "Server already running (pid $(cat server.pid))."; fi

gg_gate

if [ ! -f server.properties ]; then
  [ -f server.properties.default ] || gg_die "server.properties.default is missing from the kit."
  if gg_dry; then gg_say "[dry-run] would create server.properties from server.properties.default"
  else cp server.properties.default server.properties; gg_say "Created server.properties from the kit default."; fi
fi
PORT=$(gg_prop server-port "")
[ -z "$PORT" ] && gg_dry && PORT=$KIT_PORT
[ "$PORT" = "$KIT_PORT" ] || gg_warn "server-port is '$PORT', not $KIT_PORT."

if [ ! -f whitelist.json ]; then
  if gg_dry; then gg_say "[dry-run] would create whitelist.json from whitelist.template.json"
  else cp whitelist.template.json whitelist.json; gg_say "Created empty whitelist.json. Add players: whitelist add <name> in the server console."; fi
fi

# EULA: a human accepts it by editing eula.txt. This script never writes eula=true.
if [ ! -f eula.txt ]; then
  if gg_dry; then gg_say "[dry-run] would create eula.txt with eula=false"
  else
    printf '%s\n' '# Read https://aka.ms/MinecraftEULA. If you agree, change the next line to eula=true yourself.' 'eula=false' > eula.txt
    gg_say "Created eula.txt with eula=false."
  fi
fi
if ! grep -qE '^eula=true[[:space:]]*$' eula.txt 2>/dev/null; then
  if gg_dry; then gg_say "[dry-run] EULA not accepted; a real start would stop here until a person sets eula=true in eula.txt."
  else gg_die "EULA not accepted. Read https://aka.ms/MinecraftEULA and, if you agree, set eula=true in eula.txt yourself. This kit never does it for you."; fi
fi

gg_backup start
FLAGS=$(gg_aikar_flags "$MEM")
CMD="java -Xms${MEM}G -Xmx${MEM}G $FLAGS -jar fabric-server-launch.jar --nogui"
if gg_dry; then
  gg_say "[dry-run] command: $CMD"
  gg_say "[dry-run] checks complete; nothing was started."
  exit 0
fi
echo $$ > server.pid
gg_say "Starting on port $PORT with ${MEM}G heap. pid $$."
# exec keeps this pid, so server.pid is the java process. SIGTERM triggers the server's normal save and shutdown.
# shellcheck disable=SC2086
exec java -Xms${MEM}G -Xmx${MEM}G $FLAGS -jar fabric-server-launch.jar --nogui
