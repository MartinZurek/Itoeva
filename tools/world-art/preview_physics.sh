#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
CACHE="$ROOT/tools/reaction-preview/.work"
WORK="$HERE/.work/physics"
mkdir -p "$WORK/classes" "$WORK/frames"
"$CACHE/kotlinc/bin/kotlinc" "$HERE/preview_physics.kt" -cp "$CACHE/tests" -Xfriend-paths="$CACHE/tests" -d "$WORK/classes"
java -Djava.awt.headless=true -Xmx1200m -cp "$WORK/classes:$CACHE/tests:$CACHE/kotlinc/lib/kotlin-stdlib.jar" \
 com.notime.glyphsim.matrix.Preview_physicsKt "$ROOT" "$WORK/frames"
python3 - "$WORK/frames" "$HERE" <<'PY'
from PIL import Image
from pathlib import Path
import sys
for name in ('physics-swimming','physics-boundaries'):
 Image.open(Path(sys.argv[1])/(name+'.png')).save(Path(sys.argv[2])/(name+'.jpg'),quality=90,optimize=True)
PY
VIDEO_TMP="$(mktemp /tmp/itoeva-physics-XXXXXXXX.mp4)"
trap 'rm -f "$VIDEO_TMP"' EXIT
ffmpeg -hide_banner -loglevel error -y -threads 2 -filter_threads 1 -framerate 10 -i "$WORK/frames/physics-motion-%03d.png" \
 -c:v libx264 -threads 2 -crf 22 -pix_fmt yuv420p -movflags +faststart "$VIDEO_TMP"
python3 - "$VIDEO_TMP" "$HERE/physics-motion.mp4" <<'PY'
from pathlib import Path
import sys
Path(sys.argv[2]).write_bytes(Path(sys.argv[1]).read_bytes())
PY
