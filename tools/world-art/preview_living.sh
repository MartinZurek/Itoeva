#!/usr/bin/env bash
# Nach tools/reaction-preview/tests.sh: echte Kotlin-Posen in eine Desktop-Vorschau zeichnen.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
CACHE="$ROOT/tools/reaction-preview/.work"
WORK="${ROOM_PREVIEW_WORK:-$HERE/.work/living}"
mkdir -p "$WORK/classes" "$WORK/frames"
"$CACHE/kotlinc/bin/kotlinc" "$HERE/preview_living.kt" -cp "$CACHE/tests" \
  -Xfriend-paths="$CACHE/tests" -d "$WORK/classes"
java -Djava.awt.headless=true -cp "$WORK/classes:$CACHE/tests:$CACHE/kotlinc/lib/kotlin-stdlib.jar" \
  com.notime.glyphsim.matrix.Preview_livingKt "$ROOT" "$WORK/frames"
ffmpeg -hide_banner -loglevel error -y -framerate 10 -i "$WORK/frames/frame-%03d.png" \
  -c:v libx264 -crf 24 -pix_fmt yuv420p -movflags +faststart "$HERE/living-preview.mp4"
