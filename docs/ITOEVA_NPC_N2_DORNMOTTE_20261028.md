# N2 – Dornmotte: Flug und Schweben

Basis und Figurenliste: `ITOEVA_NPC_MOTION_SAVESPOT_20261027.md`.
Martins ausdrückliche Freigabe zum Push gilt für **alle** Charakter-Checkpoints.
N1 Aschenläufer ist bereits auf GitHub gesichert; PR #386 sammelt die Arbeit.

## Neue gezeichnete Bewegung

- Acht vollständige Flugzeichnungen: hochgezogene Flügel, Abwärtsschlag bis zu
  tief unter dem Körper liegenden Flügelspitzen, asymmetrische Rückholung und
  erneutes Anheben. Keine Verformung eines einzigen Standbilds.
- Der erste Entwurf enthielt nur zu flache obere Flügelstellungen. Die fehlenden
  vier unteren Schlagphasen wurden ausdrücklich neu gezeichnet und eingebunden.
  Beide Originalquellen und vollständige Prompts sind unter
  `tools/motion-art/sources/thorn-moth-*-n2.*` gesichert.
- Kopf-/Irisanker bleibt bei x=175, y=210 der 448-Pixel-Zelle. Die ganze einzelne
  Zeichnung wird uniform registriert; weder Flügelspitzen noch Füße dienen als
  Größenreferenz. Fester Körperfaktor in Gehen/Flug und Ruhe/Schweben.
- Die Flügel bewegen sich mit 1,4 Zyklen/s, auch wenn die Dornmotte anhält.
  Derselbe Schlag läuft über den Moduswechsel weiter, ohne Phasenneustart.
  Beide Modi verwenden bytegleiche Original- und Pixelzeichnungen.
- Revierweg und Tempo bleiben in der bestehenden Weltsteuerung. Keine neue
  Flugphysik, keine Änderung an Kampf-/Speicherdaten. Die vorhandene Schwebehöhe
  von 0,85 Welt bleibt am physischen Bodenanker erhalten.

## Prüfungen

- `tests/npc_flight.gd`: 68 Checks ohne Fehler; alle Flugphasen in Bewegung und
  Stand, nahtloser Moduswechsel, gleiche Zeitphase bei 30/60/120 Hz, fester
  Körpermaßstab/Anker und tatsächliche Pixelfassung.
- Bestehende N1-Prüfung: 54 Checks ohne Fehler.
- Python-Assetprüfungen N1/N2: 2 Tests bestanden; Quellenprüfsummen, bytegleiche
  Schwebe-/Flugfolge, feste registrierte Iris und echte tiefe Abschlaggeometrie.
- Echte OpenGL-Weltaufnahme im Sonnenhain, Hin-/Rückweg und Schweben mit Mooslyn
  als Größenvergleich: `docs/validation/npc-n2-20261028/`.
- Vollständige neue Tiefen-/Wendeposen und Landen bleiben offen. Kampfangriffe
  weiterhin bestehende Bilder. Kein Telefontest, keine neue APK.

## Reproduzieren

```bash
python3 tools/motion-art/prepare_npc.py thorn-moth --height 1.1 --checkpoint n2 --locomotion flight --wingbeats 1.4
godot --headless --path prototypes/godot-hd2d --editor --import
godot --headless --path prototypes/godot-hd2d --script res://tests/npc_flight.gd
python3 -m unittest discover -s prototypes/godot-hd2d/tests -p 'test_npc*assets.py' -v
MOTION_M4=1 NPC_ID=thorn_moth NPC_CAPTURE_FAST=1 godot \
  --path prototypes/godot-hd2d --rendering-method gl_compatibility --fixed-fps 30 \
  --resolution 960x540 --script res://tests/npc_motion_capture.gd
python3 tools/motion-art/review_npc_capture.py \
  prototypes/godot-hd2d/captures/npc_thorn_moth docs/validation/npc-n2-20261028
```

Rohaufnahmen mit lokaler `captures/.gdignore` vom Editorimport ausschließen;
sie sind Zwischenbilder. Quellen, fertiges Video, Telemetrie und Prüfergebnisse
gehören in den Commit und auf GitHub.

## Nächster Einzelcheckpoint

N3 Rindenwächter. Originalvorlage: unterste Reihe von
`assets/expansion/enemies.png`. Schwerer zweibeiniger Gang mit echten
Gewichtswechseln, großen Wurzelfüßen, Gegenbewegung von Schultern/Becken und
ruhiger Blatt-/Zweigbewegung. Brustkristall, Blüten, Gesicht und Zweigkrone
originalgetreu erhalten. Keine generischen Insektenbeinchen, kein Verzerren
der ganzen Zeichnung. Eigener geprüfter Commit, bevor die nächste Figur beginnt.
