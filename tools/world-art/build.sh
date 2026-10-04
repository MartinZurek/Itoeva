#!/usr/bin/env bash
# Rendert alle Orte der gemalten Welt in die Spiel-Variante und schreibt GameSceneCatalog.kt.
set -euo pipefail
cd "$(dirname "$0")"
python3 build_all.py "$@"
