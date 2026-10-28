#!/usr/bin/env bash
# Launcher for Linux / macOS.  Usage: bin/run.sh [--data=PATH] [--config=PATH] [--version] [--help]
#
# What it does for you:
#   1. finds the install folder no matter where you call it from
#   2. checks that Java 17+ is installed and explains the problem if not
#   3. switches to the install folder, so the relative paths in conf/application.properties
#      (data/inventory.csv, logs/inventory.log) always point inside the install
#   4. starts the JAR with the bundled config; your own flags are passed through
#
# Optional: export JAVA_OPTS="-Xmx256m" before calling to pass JVM options.
set -euo pipefail

APP_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Java was not found on PATH. Install a JRE or JDK 17 or newer." >&2
  exit 1
fi

# Read the major version from the 'version "17.0.9"' line. The grep skips any extra lines the
# JVM may print first (for example JAVA_TOOL_OPTIONS notices). Java 8 prints "1.8" -> major 1.
JAVA_MAJOR="$(java -version 2>&1 | grep -E 'version "' | head -n 1 | sed -E 's/.*version "([0-9]+).*/\1/')"
if ! [[ "${JAVA_MAJOR}" =~ ^[0-9]+$ ]] || [ "${JAVA_MAJOR}" -lt 17 ]; then
  echo "ERROR: Java 17 or newer is required (found: ${JAVA_MAJOR:-unknown})." >&2
  exit 1
fi

JAR="$(ls "${APP_HOME}"/lib/inventory-app-*.jar 2>/dev/null | head -n 1 || true)"
if [ -z "${JAR}" ]; then
  echo "ERROR: application JAR not found in ${APP_HOME}/lib" >&2
  exit 1
fi

cd "${APP_HOME}"
# JAVA_OPTS is intentionally unquoted so several options can be passed.
# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} -jar "${JAR}" --config="${APP_HOME}/conf/application.properties" "$@"
