#!/usr/bin/env bash
# Builds the executable JAR:  target/inventory-app-<version>.jar
#
# Usage:  scripts/build.sh              use Maven if installed, otherwise javac + jar
#         scripts/build.sh --no-maven   force the javac + jar path (needs only a JDK 17+)
#
# Why two paths? Maven is the normal route (and what CI uses). The fallback lets an operations
# machine that has only a JDK still build the application from source.
set -euo pipefail
cd "$(dirname "$0")/.."

# Single source of truth for the version: the first <version> element in pom.xml.
VERSION="$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -n 1)"
JAR="target/inventory-app-${VERSION}.jar"

if [[ "${1:-}" == "--no-maven" ]] || ! command -v mvn >/dev/null 2>&1; then
  echo "[build] Compiling with javac/jar (Maven not used)"
  command -v javac >/dev/null 2>&1 || { echo "ERROR: javac not found. Install a JDK 17 or newer." >&2; exit 1; }
  command -v jar   >/dev/null 2>&1 || { echo "ERROR: jar tool not found. Install a JDK 17 or newer." >&2; exit 1; }

  rm -rf target/classes && mkdir -p target/classes
  find src/main/java -name '*.java' > target/sources.txt
  javac --release 17 -Xlint:all -d target/classes @target/sources.txt
  cp -r src/main/resources/. target/classes/
  jar --create --file "$JAR" --main-class com.inventory.app.Main -C target/classes .
else
  echo "[build] Building with Maven"
  mvn -B -q -DskipTests package
fi

echo "[build] Created $JAR"
