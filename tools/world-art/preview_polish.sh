#!/usr/bin/env bash
# Erst: bash tools/reaction-preview/tests.sh; danach Modell-/Asset-QA, keine APK-Aufnahme.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
CACHE="$ROOT/tools/reaction-preview/.work"
WORK="${POLISH_PREVIEW_WORK:-$HERE/.work/polish}"
mkdir -p "$WORK/classes" "$WORK/frames"
"$CACHE/kotlinc/bin/kotlinc" "$HERE/preview_polish.kt" -cp "$CACHE/tests" \
  -Xfriend-paths="$CACHE/tests" -d "$WORK/classes"
java -Djava.awt.headless=true -Xmx1200m -cp "$WORK/classes:$CACHE/tests:$CACHE/kotlinc/lib/kotlin-stdlib.jar" \
  com.notime.glyphsim.matrix.Preview_polishKt "$ROOT" "$WORK/frames"
cp "$WORK/frames/polish-furniture.png" "$WORK/frames/polish-seating.png" "$WORK/frames/polish-shadow-comparison.png" "$HERE/"
python3 - "$HERE" <<'PY'
import pathlib, sys
from PIL import Image
root = pathlib.Path(sys.argv[1])
for name in ('polish-furniture', 'polish-seating', 'polish-shadow-comparison'):
    with Image.open(root / (name + '.png')) as image:
        image.save(root / (name + '.jpg'), quality=90, subsampling=0, optimize=True)
PY
VIDEO_TMP="$(mktemp /tmp/itoeva-polish-XXXXXXXX.mp4)"
trap 'rm -f "$VIDEO_TMP"' EXIT
ffmpeg -hide_banner -loglevel error -y -threads 2 -filter_threads 1 -framerate 10 -i "$WORK/frames/polish-motion-%03d.png" \
  -c:v libx264 -threads 2 -crf 22 -pix_fmt yuv420p -movflags +faststart "$VIDEO_TMP"
# Erst die vollstaendige MP4 schreiben; einige Workspace-Dateisysteme unterstuetzen Seek nicht.
python3 - "$VIDEO_TMP" "$HERE/polish-motion.mp4" <<'PY'
import pathlib, sys
pathlib.Path(sys.argv[2]).write_bytes(pathlib.Path(sys.argv[1]).read_bytes())
PY
