# Itoeva – Nebenfiguren und Gegner, Savespot N1

## Verbindlicher Auftrag und Basis

Martin hat am 27.10.2026 die vier Hauptfiguren der APK **0.15.8 / Code 38**
visuell akzeptiert. Dieselbe Qualität ist nun für die umherlaufenden Nebenfiguren
und besonders die Gegner beauftragt: vollständige Körperbewegung, lesbare Größe,
kein Zweibild-Flackern und kein Wachstum/Schrumpfen. **Jede Figur einzeln prüfen
und absichern, erst danach die nächste.** Rechtzeitig eine vollständige Übergabe
schreiben, falls ein Chatwechsel nötig wird.

- Basis: `codex/hd2d-motion-phases-20261026`, Commit
  `85705a97d918ab4a5b10a21008f50ef5c14e4645`, PR #385.
- Arbeitsbranch: `codex/npc-enemy-motion-20261027`.
- Weiter ausschließlich gemalte 2D-Sprites mit HD-2D-Pixelpass. Einstieg wie immer
  über `AGENTS.md` und `docs/hd2d-sprites-statt-3d.md`.
- Hauptfiguren, Spieltempo, Revierwege, Decks und Spielstände beibehalten.

## Stand N1: Aschenläufer (`ash_hound`)

- Acht neue gemalte Vierbeiner-Gehphasen und vier ruhige Standzeichnungen.
  Originalvorlage: mittlere Reihe von `assets/expansion/enemies.png`.
- Originalbild und Bildgenerierungs-Prompt unter `tools/motion-art/sources/`.
- Irisregistrierung als feste Größenreferenz, ausschließlich uniforme
  Transformation ganzer Originalzeichnungen; fester Zell-/Bodenanker.
- Registrierte Originalatlanten und Pixelfassungen: `assets/npc_motion/`.
  Einzelne Metadatei: `data/npc_motion/ash_hound.json`.
- Sichtbarer Körpermaßstab `world_height=1.3`, Referenzhöhe 185 Pixel;
  Gehen und Ruhe verwenden denselben Faktor. Schwanz zählt nicht als Körperhöhe.
- Schrittfortschritt folgt ausschließlich dem echten zurückgelegten Weg.
  Nach 0,14 s Stillstand beginnt die ruhige Standfolge (2 Zeichnungen/s).
- Weltintegration in `world_app.gd`; noch nicht bearbeitete Gegner verwenden
  weiterhin ihre bisherigen Bilder. Die Bildfolgen für Kampfangriffe wurden
  noch nicht ersetzt. Es gibt keinen Eingriff in Kampfdaten oder Speicherformat.

## Nachweis und Grenzen

- `tests/npc_locomotion.gd`: 54 Checks, 0 Fehler, einschließlich aller zwölf
  Zeichnungen, festem Maßstab/Bodenanker, Stillstand und 30/60/120-Hz-Wegvergleich.
- `tests/test_npc_motion_assets.py`: Quell-/Atlasprüfsummen, Transparenz,
  getrennte eigene Zeichnungen, Zellränder und Originalbodenlinie bestanden.
- Bestehender Reviertest `tests/enemy_roaming.gd`: 47 Checks, 0 Fehler.
  Bekannte Ressourcenreste beim Beenden weiterhin vorhanden.
- Bestehende M4-Hauptfigurenprüfung: 1.798 Checks, 0 Fehler.
- Echte OpenGL-Weltaufnahme mit Software-GPU: 360 Bilder / 12 Sekunden,
  alle acht Gehzeichnungen in beiden Richtungen und alle vier Ruhezeichnungen.
  Durchgehend Pixelmaßstab 0,0126207005232573 und Bodenoffset 136; während Ruhe
  keine Ortsbewegung. Vergleichsvideo, Screenshot, Bericht und Telemetrie sind
  unter `docs/validation/npc-n1-20261027/` gesichert.
- Noch kein Telefontest, keine neue APK. Vollständige Pfoten-Druckpunkte und
  Schlupffreiheit sind nicht nachgewiesen. Neue Tiefen-/Wendeposen und gemalte
  Verbindungen von jeder möglichen Gehphase zur Ruhe bleiben offen.

## Absicherung / GitHub

Martin hat im Anschluss ausdrücklich geantwortet: **„Ja mach! Erlaube es für alle“**.
Diese Freigabe gilt für den N1-Push und alle folgenden Charakter-Checkpoints in
`MartinZurek/Itoeva`; nicht erneut nachfragen. N1 wurde unter Commit
`aa22446a5466c06084cfc7ba85bb19a09195b540` gesichert. Der Remote-Git-Tree
`d8bc7be581e49f6abca7ee0d171902ccc04224be` stimmt exakt mit dem lokalen N1-Tree
überein. Die anfängliche automatische Ablehnung ist damit durch ausdrückliche
Nutzerfreigabe erledigt. Kein Merge beauftragt.

## Reihenfolge der nächsten Einzel-Checkpoints

| Schritt | Figur | Bewegung | Stand |
|---|---|---|---|
| N1 | Aschenläufer | Vierbeiner gehen / ruhig stehen | Implementiert, Prüfungen oben |
| N2 | Dornmotte (`thorn_moth`) | Flug, Flügelschlag, Schweben | Implementiert, siehe N2-Nachtrag |
| N3 | Rindenwächter (`bark_guardian`) | Schweres Gehen, Gewichtsverlagerung | Implementiert, siehe N3-Nachtrag |
| N4 | Glutwicht (`glut_imp`) | Lebhaftes Gehen | Lokal geprüft; Wiederherstellung unvollständig |
| N5 | Siegelweber (`seal_weaver`) | Artgemäße schwebende Fortbewegung | Offen |
| N6 | Tangkrabbler (`tide_crab`) | Seitlicher Mehrbeinergang | Offen |
| N7 | Harzsammler (`resin_beetle`) | Mehrbeinergang, Lastbewegung | Offen |
| N8 | Schlackenträger (`cinder_carrier`) | Schwerer Lastgang | Offen |
| N9 | Vharok | Bossbewegung und feste Größenreferenz | Offen |
| N10 | Nachtwurz | Bestehenden Wald-/Kampfboss separat prüfen | Offen |
| N11–N18 | Fennec, Gloop, Puffling, Hootlet, Nella, Brumm, Tilla, Miro | Jeweils eigene artgerechte Folge | Offen |

Die vier Hauptfiguren werden nicht erneut umgestaltet. Sie bleiben auch bei
ihren Auftritten als Bewohner auf ihrem aktuellen Stand.

## Reproduktion

```bash
python3 tools/motion-art/prepare_npc.py ash-hound --height 1.3
python3 -m unittest discover -s prototypes/godot-hd2d/tests -p test_npc_motion_assets.py -v
godot --headless --path prototypes/godot-hd2d --editor --import
godot --headless --path prototypes/godot-hd2d --script res://tests/npc_locomotion.gd
godot --headless --path prototypes/godot-hd2d --fixed-fps 120 --script res://tests/enemy_roaming.gd
MOTION_M4=1 godot --path prototypes/godot-hd2d --rendering-method gl_compatibility \
  --fixed-fps 30 --resolution 1280x720 --script res://tests/npc_motion_capture.gd
```

Die Vorschau verwendet echte Revierpfade. Bei Bild 150 wird ausschließlich die
Revieruhr zum Rückweg gesetzt, bei Bild 285 tritt die Hauptfigur in die
Beobachtungsreichweite. Kein Retiming und keine interpolierten Zwischenbilder.
Die Aufnahmekamera entspricht der normalen Spielkamera. Die Telemetrie hält
Weg, Phase, Maßstab und Bodenanker pro Bild fest.

```bash
python3 tools/motion-art/review_npc_capture.py \
  prototypes/godot-hd2d/captures/npc_ash_hound docs/validation/npc-n1-20261027
```

## Fortsetzungsprompt

> Setze Itoeva fort: Nebenfiguren und Gegner, Savespot N1 vom 27.10.2026.
> Lies AGENTS.md und docs/ITOEVA_NPC_MOTION_SAVESPOT_20261027.md im Branch
> codex/npc-enemy-motion-20261027. Die vier Hauptfiguren aus APK 0.15.8 sind
> abgenommen. Aschenläufer N1 ist der erste neue Einzelcheckpoint. Prüfe zuerst,
> ob der aktuelle Branch mit GitHub übereinstimmt. Die Freigabe zum Push aller
> Charakter-Checkpoints liegt vor. Danach N2 Dornmotte und die Tabelle abarbeiten. Je Figur
> Originalquellen, registrierte HD-2D-Atlanten, echte Weltaufnahme und Tests
> sichern; nicht alle Figuren in einem ungesicherten Arbeitsschritt bearbeiten.
> Artgerechte vollständige Körperbewegung, konstante Größenreferenz und
> Bodenanker. Welt-/Spieltempo und Hauptfiguren erhalten. Keine 3D-Figuren.

## Nachtrag N2 (28.10.2026)

Dornmotte umgesetzt; Details, Reproduktion und nächster Schritt stehen in
`docs/ITOEVA_NPC_N2_DORNMOTTE_20261028.md`. Nächste Figur ist N3 Rindenwächter.

## Nachtrag N3 (28.10.2026)

Rindenwächter umgesetzt, nach Workspace-Reset neu reproduziert und geprüft.
Maßgeblicher aktueller Einstieg: `docs/ITOEVA_NPC_N3_RINDENWAECHTER_20261028.md`.
Nächste Figur ist N4 Glutwicht (`glut_imp`), erst nach GitHub-Sicherung von N3.

## Nachtrag N4 (28.10.2026)

Glutwicht war lokal umgesetzt und geprüft; nach Workspace-Reset ist die Sicherung unvollständig. Aktuell zuerst `docs/ITOEVA_NPC_N4_RECOVERY_20261028.md` lesen. Frühere Übergabe:
`docs/ITOEVA_NPC_N4_GLUTWICHT_20261028.md`. Nächste Figur nach N4-Sicherung:
N5 Siegelweber (`seal_weaver`).
