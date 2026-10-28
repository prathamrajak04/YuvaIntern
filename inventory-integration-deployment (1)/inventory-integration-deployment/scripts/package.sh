#!/usr/bin/env bash
# Assembles the release bundle that is handed to operations:
#
#   dist/inventory-app-<version>.zip          <- the deliverable
#   dist/inventory-app-<version>.zip.sha256   <- checksum so ops can verify the download
#
# Bundle layout (after unzip):
#   bin/run.sh, bin/run.bat   launchers (check Java, set working dir, start the JAR)
#   lib/inventory-app-*.jar   the application
#   conf/application.properties   editable settings
#   data/   logs/             created empty; the app writes here at runtime
#
# Usage: scripts/package.sh [--no-maven]
set -euo pipefail
cd "$(dirname "$0")/.."

bash scripts/build.sh "${1:-}"

VERSION="$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -n 1)"
NAME="inventory-app-${VERSION}"
DIST="dist/${NAME}"

command -v zip >/dev/null 2>&1 || { echo "ERROR: 'zip' is required to create the bundle." >&2; exit 1; }

rm -rf dist
mkdir -p "${DIST}/bin" "${DIST}/lib" "${DIST}/conf" "${DIST}/data" "${DIST}/logs"

cp "target/${NAME}.jar"                "${DIST}/lib/"
cp deploy/bin/run.sh deploy/bin/run.bat "${DIST}/bin/"
cp deploy/conf/application.properties  "${DIST}/conf/"
cp deploy/README.txt                   "${DIST}/README.txt"
touch "${DIST}/data/.gitkeep" "${DIST}/logs/.gitkeep"
chmod +x "${DIST}/bin/run.sh"

( cd dist && zip -qr "${NAME}.zip" "${NAME}" && sha256sum "${NAME}.zip" > "${NAME}.zip.sha256" )

echo "[package] Created dist/${NAME}.zip"
echo "[package] SHA-256: $(cut -d' ' -f1 "dist/${NAME}.zip.sha256")"
