# Itoeva – N5 Siegelweber: verbindliche Fortsetzung (29.10.2026)

## Gesicherte Basis

Repository `MartinZurek/Itoeva`, Branch `codex/npc-enemy-motion-20261027`,
Draft-PR #386. Vollständige Veröffentlichung von N4 und N5-Quellentwurf:
`bfef7dad670dc737fff6ef71b8497ca19238e817`. Dessen Tree `581ed93f7e3cb825209122e1fa1e20e662cb6445`
ist identisch mit dem geprüften lokalen Arbeitsstand vor dem Workspace-Reset.

Martin hat die Veröffentlichung ausdrücklich bestätigt. Der frühere
Push-Blocker ist erledigt. Alle benötigten Quellen, Prompts, Atlanten, Tests,
Filme und Übergaben sind jetzt auf dem Branch. Das private Übergabearchiv
ist nur noch eine Rückfallsicherung; nicht über neuere GitHub-Änderungen spielen.

## Abgeschlossener Stand

Die vier Hauptfiguren aus APK 0.15.8 bleiben abgenommen. N1 Aschenläufer,
N2 Dornmotte und N3 Rindenwächter sind abgeschlossen. N4 Glutwicht ist
vollständig aufgebaut und in der großen Fassung geprüft:

- Gleicher Welt-/Schattenfaktor 1.75, sichtbar 1,59–1,69 Mooslyn-Höhen.
- Acht Geh- und vier Ruhezeichnungen, fester Maßstab und Bodenanker.
- 2.232 Godot-Checks und vier Python-Assetprüfungen bestanden.
- 600 echte Weltbilder / 20 s mit Mooslyn, beiden Richtungen, zwei Stopps
  und Wiederanlaufen; Daten/Logs unter `docs/validation/npc-n4-size-20261029/`.

Die alte kleine Glutwicht-Größe 0.86 ist verworfen. Maßgeblich sind
`docs/ITOEVA_NPC_N4_GROESSE_20261029.md` und dieser Fortsetzungstext.
Die hier genannten Tests wurden vor dem Reset ausgeführt; die erneute
Veröffentlichung hat Dateien geprüft und wiederhergestellt, keine neuen
Testläufe oder Änderungen am Spielcode vorgenommen.

## N5 Siegelweber: Quelle vorhanden, Einbau offen

Original: `prototypes/godot-hd2d/assets/expansion/seal-weaver.png`.
Neue Quelle: `tools/motion-art/sources/seal-weaver-motion-n5.png`.
Vollständiger Prompt daneben als `.txt`; Quellprüfung:
`tools/motion-art/seal-weaver-n5-source-check.json`.

Zwölf Zeichnungen: acht Schwebephasen, vier Ruhehaltungen. Zwölf getrennte
große Alphakomponenten, Transparenz und Quellprüfsumme geprüft. Das ist
ein Quellentwurf, kein fertiger Atlas und keine abgenommene N5-Animation.
Noch keine Registrierung, kein N5-Pixelpass, keine Laufzeitdaten oder Weltprüfung.

## Nächster Arbeitsschritt

1. Zuerst neuere Branchänderungen prüfen. AGENTS.md und die verlangten
   Einstiegsdokumente lesen, außerdem `docs/hd2d-sprites-statt-3d.md` und
   `docs/ITOEVA_NPC_N5_START_20261029.md`.
2. Original und neue N5-Quelle visuell öffnen. Vier Arme, helle Maske,
   cyanfarbenes Auge, violettes Federgewand, rote Siegel und vollständige
   Konturen erhalten. Größen-/Phasenanschlüsse prüfen; nötigenfalls nacharbeiten.
3. Ganze Zeichnungen mit festem Iris-/Rumpfanker registrieren. Flatternder
   Gewandsaum darf nicht den Körper versetzen. Gemeinsamer Maßstab für
   Fortbewegung und Ruhe, keine Gliedmaßenverformung.
4. Zeitgebundene schwebende Ganzkörperbewegung, die beim Anhalten natürlich
   weiterläuft. Keine abwechselnden Gehschritte und kein Phasensprung.
   Bestehenden Flug-/Bewegungspfad bevorzugen, Revierwege und Tempo erhalten.
5. HD-2D-Pixelpass, passende Prüfungen und echte Weltaufnahme mit Mooslyn,
   beiden Richtungen, zwei Stopps und Wiederanlaufen. N1–N4/M4 erhalten.
6. N5 als eigenen vollständigen Figurencheckpoint auf demselben Branch sichern,
   erst danach N6 Tangkrabbler und die Figurenliste im N1-Savespot fortsetzen.

Nur gemalte 2D-Sprites; keine 3D-Figuren. Keine APK und kein Merge.
Druckpunkt-/Schlupffreiheit, zusätzliche Tiefen-/Wendeposen, neue gemalte
Verbindungen und Kampfangriffe nicht als bereits nachgewiesen darstellen.
