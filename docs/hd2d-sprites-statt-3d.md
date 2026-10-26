# Spielfiguren: HD-2D-Sprites statt 3D-Modelle (Entscheidung 26.10.2026)

**Entscheidung von Martin:** Die Spielfiguren bleiben 2D-Sprites (Billboards) in der Godot-Welt,
im Stil von Octopath Traveler und Final Fantasy Resonance. Licht, Tageszeit, örtliche Lichter und
Bodenschatten kommen aus der Welt. Der 3D-Figurenweg ist beendet. Bitte nicht erneut vorschlagen.

## Warum der 3D-Weg verworfen wurde

- A2/A3 (Projektionsvolumen aus Seitenbildern, APK 0.15.4/0.15.5) funktionierten nur aus der
  festen Seitenkamera, Gliedmaßen waren nicht getrennt, Füße rutschten.
- A4 (frei drehbares Moosling-Mesh, Branch `codex/moosling-spatial-rig-20261025`) reichte optisch
  nicht an die freigegebenen Entwürfe heran (Gesicht, Ohren, Geweih, Fell, Tasche); 14 offene
  Kontaktprüfungen. Für vier Spielfiguren und weitere Wesen wäre der Aufwand unverhältnismäßig.
- Die gemalten Sprite-Atlanten mit HD-2D-Pixelpass (Test-APK 0.15.6, Branch
  `claude/hd2d-pixel-sprites`) sehen im Spiel dem Entwurf deutlich näher und wurden abgenommen.

## Was entfernt wurde

`assets/characters/*-3d/`, `scripts/mosslyn_volume.gd`, `scripts/companion_volume.gd`,
`shaders/mosslyn_volume.gdshader`, `data/companion_volumes.json`, die Tests
`mosslyn_volume*`/`companion_volume*`, `tools/mosslyn-3d/`, `tools/companions-3d/`, die Dokumente
A2/A3 sowie die Einstellung „Charaktere 3D“. Alles bleibt in der Git-Historie nachvollziehbar.
Gespeicherte Einstellungen mit dem alten Schlüssel `mosslyn_volume` werden einfach ignoriert.

Nicht betroffen: `tools/character-art/fennec3d.py`, `rig3d.py` und `tools/world-art/room3d.py`
erzeugen 2D-Bildbögen bzw. Kulissen für die Android-App und sind keine Spielfigurenmodelle.

## Wie neue Figurenbewegungen entstehen

1. Gemalte Vorlage (Bildgenerierung) nach `docs/approved-3d-designs-20261025/`.
2. Gemalter Atlas in `prototypes/godot-hd2d/assets/characters/` mit festem Zellraster,
   festem Maßstab und Fußanker auf derselben Bodenlinie.
3. Pixelpass: `python3 tools/pixel-pass/pixel_pass.py prototypes/godot-hd2d/assets/characters`
   → `assets/characters/pixel/`. Metadaten, Masken und Rigs beziehen sich auf die Originale.
4. Vorschau in der echten Welt: `prototypes/godot-hd2d/preview_pixel/play_capture.gd`.
5. Erst nach Martins Freigabe eine Test-APK bauen.
