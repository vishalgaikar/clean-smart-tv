#!/usr/bin/env bash
# restore.sh - Restore Sony Bravia to stock launcher and re-enable packages
set -euo pipefail

TV_HOST="${1:-192.168.1.12}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON="${SCRIPT_DIR}/tools/venv/bin/python"

echo "Connecting to TV at ${TV_HOST}..."

# 1. Re-enable packages disabled by Free My TV mode
echo "Re-enabling packages..."
for pkg in com.google.android.backdrop \
           com.google.android.tvrecommendations \
           com.google.android.tvlauncher \
           com.google.android.youtube.tvmusic \
           com.google.android.play.games; do
    echo "  Enabling $pkg"
    TV_HOST="${TV_HOST}" "${PYTHON}" "${SCRIPT_DIR}/tools/tv.py" "pm enable ${pkg}" || true
done

# 2. Reset Home Activity to stock launcher
echo "Restoring stock Home activity..."
TV_HOST="${TV_HOST}" "${PYTHON}" "${SCRIPT_DIR}/tools/tv.py" "cmd package set-home-activity com.google.android.apps.tv.launcherx/.home.HomeActivity || cmd package set-home-activity com.google.android.tvlauncher/.MainActivity" || true

# 3. Restore stock screensaver
echo "Restoring stock screensaver..."
TV_HOST="${TV_HOST}" "${PYTHON}" "${SCRIPT_DIR}/tools/tv.py" "settings put secure screensaver_components com.google.android.backdrop/.Backdrop" || true

# 4. Reset Private DNS
echo "Resetting Private DNS to default..."
TV_HOST="${TV_HOST}" "${PYTHON}" "${SCRIPT_DIR}/tools/tv.py" "settings put global private_dns_mode opportunistic && settings delete global private_dns_specifier" || true

echo "Restore complete! Returning to Home..."
TV_HOST="${TV_HOST}" "${PYTHON}" "${SCRIPT_DIR}/tools/tv.py" "input keyevent HOME"
