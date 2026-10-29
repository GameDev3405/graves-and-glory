#!/usr/bin/env bash
# Serves the lobby preview on http://localhost:8080 (needs Python 3, or falls back to Node's npx http-server).
cd "$(dirname "$0")"
PORT="${1:-8080}"
echo "Banner Camp lobby preview -> http://localhost:$PORT"
if command -v python3 >/dev/null 2>&1; then exec python3 -m http.server "$PORT"; fi
if command -v python >/dev/null 2>&1; then exec python -m http.server "$PORT"; fi
exec npx --yes http-server -p "$PORT" -c-1 .
