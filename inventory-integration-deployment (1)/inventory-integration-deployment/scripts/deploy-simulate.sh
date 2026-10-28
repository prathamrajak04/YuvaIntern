#!/usr/bin/env bash
# Simulates handing the release bundle to an operations team and running their acceptance checks.
# Run scripts/package.sh first.   Usage: scripts/deploy-simulate.sh
#
# It does what an ops engineer would do on a fresh server:
#   verify checksum -> unpack -> smoke test -> use the app -> RESTART -> confirm data survived
#   -> confirm a corrupt data file is refused with a clear error.
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION="$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -n 1)"
NAME="inventory-app-${VERSION}"
ZIP="dist/${NAME}.zip"
[ -f "$ZIP" ] || { echo "ERROR: $ZIP not found. Run scripts/package.sh first." >&2; exit 1; }

SERVER="$(mktemp -d)"
trap 'rm -rf "$SERVER"' EXIT
APP="${SERVER}/${NAME}"
PASS=0
FAIL=0

check() { # check "description" command...
  local description="$1"; shift
  if "$@" >/dev/null 2>&1; then echo "  PASS  ${description}"; PASS=$((PASS + 1))
  else echo "  FAIL  ${description}"; FAIL=$((FAIL + 1)); fi
}

echo "[1/7] Verify checksum"
( cd dist && sha256sum -c "${NAME}.zip.sha256" )

echo "[2/7] Unpack on the 'server': ${SERVER}"
unzip -q "$ZIP" -d "$SERVER"
check "launcher is executable"            test -x "${APP}/bin/run.sh"
check "JAR present in lib/"               test -f "${APP}/lib/${NAME}.jar"
check "config present in conf/"           test -f "${APP}/conf/application.properties"

echo "[3/7] Smoke test: --version and --help"
VERSION_OUT="$("${APP}/bin/run.sh" --version)"
check "version banner is printed"         grep -q "Inventory Manager ${VERSION}" <<<"${VERSION_OUT}"
check "--help exits 0"                    "${APP}/bin/run.sh" --help

echo "[4/7] Session 1: add two items (first run, no data file yet)"
set +e
printf '%s\n' 1 A1 Laptop 10 50000 1 A2 "Cable, USB-C" 3 199.5 2 0 | "${APP}/bin/run.sh" > "${SERVER}/session1.txt" 2>&1
CODE=$?
set -e
check "session 1 exit code is 0"          test "${CODE}" -eq 0
check "session 1 reported 0 items loaded" grep -q "Loaded 0 item(s)." "${SERVER}/session1.txt"
check "data file was created"             test -f "${APP}/data/inventory.csv"
check "data file has header + 2 rows"     test "$(wc -l < "${APP}/data/inventory.csv")" -eq 3
check "comma in name was quoted in CSV"   grep -q '"Cable, USB-C"' "${APP}/data/inventory.csv"

echo "[5/7] Session 2: RESTART - data must still be there"
set +e
printf '%s\n' 2 10 0 | "${APP}/bin/run.sh" > "${SERVER}/session2.txt" 2>&1
CODE=$?
set -e
check "session 2 exit code is 0"          test "${CODE}" -eq 0
check "session 2 loaded 2 items"          grep -q "Loaded 2 item(s)." "${SERVER}/session2.txt"
check "total value is 500598.50"          grep -q "Total inventory value: 500598.50" "${SERVER}/session2.txt"
check "log file records the additions"    grep -q "Added item A1" "${APP}/logs/inventory.log"

echo "[6/7] Corrupt data file must be refused with a clear message"
printf 'id,name,quantity,price\nA1,Laptop,oops,5\n' > "${SERVER}/bad.csv"
set +e
BAD_OUT="$("${APP}/bin/run.sh" --data="${SERVER}/bad.csv" </dev/null 2>&1)"
BAD_CODE=$?
set -e
check "corrupt file gives exit code 1"    test "${BAD_CODE}" -eq 1
check "message names the line number"     grep -q "Startup failed: Corrupt data file.* at line 2" <<<"${BAD_OUT}"

echo "[7/7] Bad command line must be refused"
set +e
"${APP}/bin/run.sh" --bogus >/dev/null 2>&1
BOGUS_CODE=$?
set -e
check "unknown flag gives exit code 2"    test "${BOGUS_CODE}" -eq 2

echo
echo "Deployment simulation: ${PASS} passed, ${FAIL} failed"
[ "${FAIL}" -eq 0 ]
