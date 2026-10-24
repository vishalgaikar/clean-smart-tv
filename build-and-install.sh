#!/usr/bin/env bash
# build-and-install.sh - Rebuild Free My TV and deploy to TV
set -euo pipefail

TV_HOST="${1:-192.168.1.12}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON="${SCRIPT_DIR}/tools/venv/bin/python"

export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"

echo "Building Free My TV..."
(cd "${SCRIPT_DIR}/free-my-tv" && ./build.sh)

echo "Pushing FreeMyTV.apk to TV at ${TV_HOST}..."
TV_HOST="${TV_HOST}" "${PYTHON}" "${SCRIPT_DIR}/tools/b64push.py" "${SCRIPT_DIR}/free-my-tv/FreeMyTV.apk" "/data/local/tmp/FreeMyTV.apk"

echo "Installing APK..."
TV_HOST="${TV_HOST}" "${PYTHON}" "${SCRIPT_DIR}/tools/tv.py" "pm install -r /data/local/tmp/FreeMyTV.apk"
TV_HOST="${TV_HOST}" "${PYTHON}" "${SCRIPT_DIR}/tools/tv.py" "rm -f /data/local/tmp/FreeMyTV.apk"

echo "Free My TV updated successfully!"
