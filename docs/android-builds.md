# Itoeva Android-Builds

| App | Version | Versionscode | Android-Paket | Einstiegsszene | Symbol |
| --- | --- | --- | --- | --- | --- |
| Itoeva Gameplay | 0.19.21 | 83 | `com.itoeva.gameplay.preview` | `scenes/gameplay.tscn` | Grünes Blatt |
| Itoeva Kampfarena | 1.0.5 | 6 | `com.itoeva.kampfarena.preview` | `scenes/duel_lab.tscn` | Kupferfarbene Karten mit Blitz |

Der gemeinsame Quellstand enthält die Kartenhand aus PR #394. Das Hauptspiel basiert auf dem letzten im Repository verfügbaren Gameplay-Quellstand. Die Kampfarena baut auf der Git-Historie der veröffentlichten 1.0.3 auf. Beide Exporte sind für ARM64 vorgesehen.

## Export reproduzieren

Godot 4.6.3 und das dazugehörige Android-Exporttemplate installieren. In Godot die Pfade zum JDK und Android SDK eintragen. Die Befehle im Repository-Verzeichnis ausführen. `OUT` muss auf ein neues, absolutes Ausgabeverzeichnis zeigen.

```bash
mkdir -p "$OUT"
python3 tools/stage_gameplay.py "$OUT/main"
python3 tools/stage_duel_lab.py "$OUT/arena"

godot --headless --editor --path "$OUT/main" --import
godot --headless --editor --path "$OUT/arena" --import
godot --headless --path "$OUT/main" --export-release Gameplay "$OUT/main-unsigned.apk"
godot --headless --path "$OUT/arena" --export-release Kartenschmiede "$OUT/arena-unsigned.apk"
```

Die beiden Exporte über `tools/fix_duel_lab_android.py` mit dem originalen Godot-Android-Template korrigieren. Anschließend mit `zipalign -P 16 -f 4` ausrichten, mit dem vorhandenen Schlüssel signieren und mit `apksigner verify` prüfen. Jede signierte APK in ein eigenes ZIP legen. Die Kampfarena kann zusätzlich über den Workflow `Itoeva Kampfarena APK` auf dem gewünschten Quellbranch gebaut werden.

Für ein direktes Update muss Android denselben Signaturschlüssel erkennen. Den Schlüssel außerhalb des Repositorys aufbewahren. Bei späteren Builds die Versionswerte in den beiden Staging-Skripten aktualisieren und den Commit der APK festhalten.
