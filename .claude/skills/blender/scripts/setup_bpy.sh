#!/usr/bin/env bash
# Install Blender as a Python module (bpy) into a private venv, plus the system libraries EEVEE
# needs on headless Linux. Safe to re-run.
#
#   BPY_VENV=/path/to/venv   where to put the venv (default: $HOME/.cache/blender-bpy)
#   BPY_VERSION=5.2.3        bpy version to install (default matches the API notes in SKILL.md)
#
# Afterwards run scripts with:  "$BPY_VENV/bin/python" path/to/script.py
set -euo pipefail

VENV="${BPY_VENV:-$HOME/.cache/blender-bpy}"
BPY_VERSION="${BPY_VERSION:-5.2.3}"
SUDO=""
if [ "$(id -u)" -ne 0 ] && command -v sudo >/dev/null 2>&1; then
  SUDO="sudo"
fi

EGL_PACKAGES="libegl1 libgl1 libxkbcommon0 libsm6 libxi6 libxrender1 libxxf86vm1 libglu1-mesa"

# EEVEE needs libEGL even when run in software. Cycles does not. Skip this on macOS and Windows.
if [ "$(uname -s)" = "Linux" ] && command -v apt-get >/dev/null 2>&1; then
  if ! ldconfig -p 2>/dev/null | grep -q "libEGL.so.1"; then
    echo "Installing EGL and GL libraries for EEVEE..."
    $SUDO apt-get install -y -q $EGL_PACKAGES >/dev/null 2>&1 \
      || { $SUDO apt-get update -q >/dev/null 2>&1 && $SUDO apt-get install -y -q $EGL_PACKAGES >/dev/null; }
  fi
fi

python3 -m venv "$VENV"
"$VENV/bin/pip" install -q "bpy==$BPY_VERSION"
"$VENV/bin/python" -c "import bpy; print('bpy', bpy.app.version_string, 'ready')"
echo "Run scripts with: $VENV/bin/python path/to/script.py"
