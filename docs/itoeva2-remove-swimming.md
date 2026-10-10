# Trockene Kueste und Entfernung des Schwimmens

Martins Auftrag vom 10.10.2026 entfernt das Schwimmen einschliesslich des grossen
Vordergrundteichs. Grundlage ist PR #352, Branch
`codex/painted-interiors-walking-map`, Commit `302d83c`.

## Umsetzung

- `world/coast.png` und `world/seams/0.png` ersetzen vorderes flaches Wasser durch
  trockenen Sand, Steine und niedrige Pflanzen. Meer und Insel bleiben im Hintergrund.
  Die Bilder wurden mit dem eingebauten Imagegen-Werkzeug aus den vorhandenen
  Spielassets bearbeitet und visuell geprueft.
- Der Kuestenboden endet wie die angrenzenden Landwege bei y=562. Der ehemalige
  Schwimmbereich bis y=615 gehoert nicht mehr zum begehbaren Territorium. Stege,
  Felsen und der separate Brueckenweg bleiben feste Gegenstaende.
- `GameWater`, Wasser-Texturverformung, Wellen-/Bugwellenzeichnung, Schwimmzoom,
  Wasserwiderstand, Auftrieb, Unterwasserverdeckung und `Motion.SWIM` entfallen.
- Sechs Schwimm-Sheets, ihr Cache und das Vorladen entfallen. 72 Bilder mit
  256 x 256 Pixeln entsprechen bei ARGB_8888 rechnerisch 18 MiB Pixelpuffern;
  GPU-Kopien und temporaere Decoderpuffer sind darin nicht enthalten. Das ist
  eine Speicherrechnung, keine gemessene Beschleunigung auf Martins Telefon.
- Ausschliesslich zum Schwimmen gehoerende Generatoren, Rohatlanten und
  Vorschauen werden entfernt. Historische Dokumentation bleibt als Protokoll.
- Spielstandformat, Orts-IDs, Inventar, Erinnerungen, Musik und Skillbaum bleiben
  erhalten. Alte Wasserpositionen werden erst beim Spieleinstieg auf Land begrenzt.
  Dekoratives Wasser in anderen Darstellungsmodi ist keine Schwimmfunktion.
- Angeln ist ein spaeteres Vorhaben und wird hier nicht implementiert.

## 3D-Pruefung

Die produktive Darstellung ist `AvatarSpriteView` -> `GameCharacterPainter` ->
Android `drawBitmapMesh`. Sie verformt zweidimensionale Bitmapkoordinaten und
multipliziert deren Farben mit Ortslicht. Es gibt dabei keine raeumlichen
Koerperoberflaechen, Tiefenpuffer oder dreidimensionalen Gelenke. Auch
`tools/character-art/rig3d.py` und `fennec3d.py` erzeugen Bildfolgen fuer Sprites;
sie liefern kein live gerendertes, animiertes 3D-Modell in der Android-App.

Echte 3D-Modelle sind in einer Kotlin-App mit einem Renderer wie Filament
moeglich. Godot bietet fuer Modelle, Skelettanimation und Szene bereits
Engine-Werkzeuge und laesst sich auch in Android einbetten:

- https://github.com/google/filament
- https://docs.godotengine.org/en/stable/tutorials/platform/android/android_library.html
- https://docs.godotengine.org/en/stable/tutorials/assets_pipeline/importing_3d_scenes/available_formats.html

Empfehlung: ein Fennec-Modell mit sauberer Geometrie, Textur und Rig in einer
festen 2,5D-Kamera ausprobieren und gegen die gemalten Hintergruende pruefen.
Dabei Proportionen, Drehung, Gehen/Rennen, Schatten, Verdeckung und Android-
Bildrate beurteilen. Ein Engine-Wechsel allein macht vorhandene Sprites nicht
raeumlich. Dieser Auftrag prueft die Architektur; er baut noch keinen 3D-Prototyp
und migriert das Spiel nicht. Assetqualitaet und Geraeteleistung sind offen.

## Bildauftrag

Die beiden Imagegen-Bearbeitungen entfernen ausschliesslich flaches Wasser im
Vordergrund und ersetzen es durch den vorhandenen warmen Sand-/Stein-/Pflanzenstil.
Haus, Baumpositionen, Himmel, entfernte Kueste und Insel sollen erhalten bleiben.
Die zwei kleinen Holzstege werden trockene Holzwege; der horizontale Laufkorridor
in Weltkoordinaten y=495..562 bleibt offen. Das Anschlussbild uebernimmt denselben
trockenen Boden. Keine Figuren, Schrift oder Bedienoberflaechen hinzufuegen.

## Pruefung und Uebergabe

`bash tools/reaction-preview/tests.sh`: 1.049 Tests bestanden am finalen lokalen Stand. Zusaetzliche
Regressionen betreffen die trockene Vordergrenze, alte Schwimmpositionen, Tempo,
Sitz-/Rollbefehle und beide Laufrichtungen an der Kueste. Bestehende Tests fuer
Tueren, Bruecke, Gras, Figurenmassstab und die anderen Modi bleiben erhalten.
Der native Test prueft, dass keine `-swim.png`-Datei mehr ausgeliefert wird und
alle Land-Sheets samt Zusatzposen weiterhin geladen und wiederverwendet werden.

Android-Kompilierung, native Zeichen-/Touchtests und Telefonmessung sind durch
den lokalen Kotlin-Lauf nicht nachgewiesen. CI im zugehoerigen PR verfolgen;
danach APK aus diesem Branch erstellen und Startzeit, Speicher und Kuestenwege
am Telefon pruefen. Keine gemessene Ladezeit behaupten. Kein Merge/APK-Bau in
dieser Sitzung. Ruecksetzung: diesen Schnitt auf `302d83c` zuruecknehmen.
