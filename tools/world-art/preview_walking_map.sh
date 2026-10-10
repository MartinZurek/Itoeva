#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
CACHE="${WORK:-$ROOT/tools/reaction-preview/.work}"
PREVIEW="$HERE/.work/walking-map"
mkdir -p "$PREVIEW"
"$CACHE/kotlinc/bin/kotlinc" "$HERE/preview_walking_map.kt" -cp "$CACHE/tests" -Xfriend-paths="$CACHE/tests" -d "$PREVIEW"
java -Djava.awt.headless=true -cp "$PREVIEW:$CACHE/tests:$CACHE/kotlinc/lib/kotlin-stdlib.jar" \
  com.notime.glyphsim.matrix.Preview_walking_mapKt "$ROOT" "$HERE"
