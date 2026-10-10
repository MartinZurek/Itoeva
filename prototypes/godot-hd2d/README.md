# Itoeva: gemalter HD-2D-Ausschnitt in Godot

Ein separat startbarer Prototyp fuer Martins Auftrag vom 10.10.2026:
**bestehende gemalte Welt im 3D-Raum, bestehender Fennec als 2D-Figur**.
Basis: `main` auf `124186c77f20954321008b0bf373a1527562f11a`.

## Starten

In **Godot 4.5.1** `project.godot` importieren und mit F6/F5 starten.
Die benoetigten Bilder liegen bereits im Projekt; Python wird zum Spielen nicht gebraucht.
Desktop: `godot --path prototypes/godot-hd2d` aus dem Repository.
Der Compatibility-Renderer funktioniert ohne Vulkan. Die Test-APK unterstuetzt
ARM64-Geraete ab Android 7 und startet als separate App **Itoeva Godot**.

## Android-APK v0.1.2

Godot 4.5.1 mit Android-Exportvorlagen, Java 17 und Android SDK Build-Tools 35.0.1
verwenden. Java-/SDK-Pfade und den eigenen Debug-Keystore in den Editor-Einstellungen
eintragen; keine Schluessel ins Repository kopieren.

```bash
godot --headless --path prototypes/godot-hd2d --export-debug Android /absoluter/pfad/Itoeva_Godot_HD2D_v0.1.2.apk
```

Das Exportpreset baut eine signierte Debug-APK mit Paket `com.itoeva.godot.hd2d`,
Version 0.1.2 / Code 2, ARM64, Querformat und ohne Internetberechtigung.
Tests, Vorschauen und Capture-Dateien werden ausgeschlossen. Die ausgelieferte
APK ist 31.024.552 Bytes gross; ZIP-Integritaet und APK-Signatur v2/v3 sind geprueft.
Ein Start auf einem Android-Telefon ist noch nicht geprueft.

| Eingabe | Wirkung |
|---|---|
| WASD / Pfeiltasten | In der Breite und Tiefe bewegen |
| Shift | Laufen |
| Leertaste | Physischer Sprung mit vorhandenen gezeichneten Sprungposen |
| Q / E | Kleiner Kameraschwenk, maximal acht Grad |
| Z / X oder Mausrad | Zoom |
| L / Abend | Tages-/Abendlicht vergleichen |
| Linker Daumen | Analog gehen, am aeusseren Rand laufen |
| Zweiter Daumen auf Sprung | Gleichzeitig bewegen und springen |

## Was uebertragen ist

- Originalpanorama `street-park-forest.png`: Strasse, Park, Wiese und Waldrand.
  Referenzkamera und UV-Projektion erhalten die Komposition. Rueckwand als
  verbundenes Relief mit Tiefenstaffelung, horizontaler 3D-Boden, vertikale Steinkante.
- Original-Fennec, **alle 138 Bilder pixel-identisch**, nur von einer sehr breiten
  Zeile in einen 12x12-Atlas umgepackt. 1536px statt 17664px Breite vermeiden mobile
  Texturlimits. Fussanker 126, Richtungen und Rollen aus `CreatureSprites.Rich`.
  Naechste-Nachbar-Filter, aufrechte 2D-Karte (`Sprite3D`), keine 3D-Figur und kein Rig.
  Pixelmassstab 0.020 beruecksichtigt die 35-Grad-Kamera: die Kopf-/Fuss-Spanne
  entspricht rund 87 Bildpixeln in der nativen 640px-Kulisse, innerhalb ihrer
  65..108px-Tiefenskalierung. Kollisionskoerper und Bodenschatten sind angepasst.
- Echter `CharacterBody3D`, begehbarer Boden, Kartenraender, Bank und Lampensockel;
  Bank kann als erhoehte Standflaeche genutzt werden. Schatten folgt der Standflaeche.
- Sanfte orthografische Folge-Kamera, Zoom und begrenzter Schwenk. Die Original-
  Blickrichtung ist verbindlich: ein Rundumblick wuerde ungesehene Bildseiten erfordern.
- Warmes Umgebungs-/Lampenlicht, erhaltener gemalter Tageslook und umschaltbare
  Abendtoenung. Die Kulisse traegt bereits gemaltes Licht; sie wird deshalb nicht
  voll mit PBR-Licht neu beleuchtet. Ein schwacher real beleuchteter Boden liegt darueber.

## Pruefen

```bash
python prototypes/godot-hd2d/prepare_assets.py --check
godot --headless --path prototypes/godot-hd2d --editor --import --quit
godot --headless --path prototypes/godot-hd2d --script res://tests/smoke.gd
godot --path prototypes/godot-hd2d --script res://tests/capture.gd
```

`smoke.gd` prueft in 43 Checks den projizierten Figurenmassstab und mit echter Godot-Physik Boden, Bewegung, Bank, Sprung/Landung,
Standflaechenschatten, Kartenraender und die ganze Spritekarte an beiden Raendern
bei beiden Zoomgrenzen, drei Schwenkwinkeln und vorderem/hinterem Laufstreifen,
Lichtwechsel sowie zwei gleichzeitig eingespeiste ScreenTouch-Ereignisse.
`capture.gd` schreibt echte Godot-Renderings nach `captures/` (nicht eingecheckt).
Die ausgewaehlten Renderings stehen in `preview/`. Sie sind Desktop-Aufnahmen mit
Software-OpenGL, keine Konzeptbilder und keine APK-Aufnahmen.

`prepare_assets.py` regeneriert die beiden Godot-Assets aus den aktuellen Originalen
(Python mit Pillow). `--check` prueft die vorhandenen Dateien ohne Schreiben.
Bei geaendertem Rollenformat erst den Godot-Adapter anpassen.

## Grenzen und naechster Schritt

Dieser Ausschnitt ist eine gemalte 3D-Buehne mit schmalem Laufstreifen. Gebaeude und
Baeume sind Reliefgeometrie, keine rundum ausgearbeiteten 3D-Modelle. Licht/Schatten
der Originalmalerei bleiben eingebrannt; Bankverdeckung und ihr Sitzanker sind
naeherungsweise, eine allgemeine Tiefenmaske fuer jedes Blatt/Moebel fehlt noch.
Blatt-/Wasseranimation, Innenraeume, Tueren, Bewohner, Living-Agent-System, Kampfsystem
und Android-Spielstaende sind nicht angebunden. Die Android-App wird durch diesen
Prototyp nicht umgestellt; eine separate Debug-APK ist exportiert, eine Telefonmessung fehlt.

Zuerst diesen Ausschnitt in Godot bzw. auf dem Telefon visuell abnehmen. Danach
Bank-/Baum-Silhouetten einzeln auf Tiefenkarten legen und den Eingang zum Haus
mit einem bestehenden gemalten Innenraum verbinden. Erst nach dieser Stilabnahme
weitere Regionen und bestehende Spielsysteme uebertragen.

Ruecksetzen: den Prototype-PR zuruecknehmen bzw. `prototypes/godot-hd2d` entfernen.
Keine Datenmigration, keine Aenderung an den Android-Persistenzvertraegen.
