#!/usr/bin/env bash
# Enforces the layer rule: a package may only import the packages listed for it below.
# Fails (exit 1) and prints the offending file if any import points the wrong way.
# Runs in CI; run it locally before opening a pull request.
#
#   app -> everything        cli -> service, model, exception
#   service -> repository, model, exception, util
#   repository -> model, exception, util      model -> util
#   config -> exception      util, exception -> nothing
set -euo pipefail
cd "$(dirname "$0")/.."
SRC="${1:-src/main/java/com/inventory}"

declare -A ALLOWED=(
  [app]="cli config exception model repository service util"
  [cli]="service model exception"
  [service]="repository model exception util"
  [repository]="model exception util"
  [model]="util"
  [config]="exception"
  [util]=""
  [exception]=""
)

violations=0
for dir in "$SRC"/*/; do
  pkg="$(basename "$dir")"
  if [[ -z "${ALLOWED[$pkg]+set}" ]]; then
    echo "UNKNOWN PACKAGE: $pkg (add it to the table in scripts/check-layers.sh)" >&2
    violations=$((violations + 1))
    continue
  fi
  while IFS= read -r line; do
    file="${line%%:*}"
    imported="$(sed -E 's/.*import com\.inventory\.([a-z]+)\..*/\1/' <<<"${line#*:}")"
    [[ "$imported" == "$pkg" ]] && continue
    if [[ " ${ALLOWED[$pkg]} " != *" $imported "* ]]; then
      echo "VIOLATION: $file imports package '$imported' (not allowed from '$pkg')" >&2
      violations=$((violations + 1))
    fi
  done < <(grep -rn --include='*.java' -E '^import com\.inventory\.[a-z]+\.' "$dir" || true)
done

if [ "$violations" -gt 0 ]; then
  echo "Layer check FAILED: $violations violation(s)" >&2
  exit 1
fi
echo "Layer check passed: no illegal package dependencies"
