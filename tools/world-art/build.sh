#!/usr/bin/env bash
# Rendert alle Kulissen der gemalten Welt in die Spiel-Variante.
set -euo pipefail
cd "$(dirname "$0")"
OUT=../../app-sim/src/game/assets/scenes
TMP=$(mktemp -d)
python3 park.py "$TMP/park.png"
python3 reading_room.py "$TMP/living.png"
python3 hero.py "$TMP/hero.png"
mkdir -p "$OUT"
cp "$TMP/park.png" "$TMP/living.png" "$TMP/hero.png" "$OUT/"
rm -rf "$TMP"
echo "Kulissen nach $OUT geschrieben."
