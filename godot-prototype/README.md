# Itoeva: gemaltes Haus und begehbare Küste in Godot

Kleiner 3D-Ausschnitt auf dem Android-Prototyp aus PR #341. Ein gemeinsamer Boden verbindet
Wohnzimmer, Haustür, Gartenweg, Strand und flaches Wasser. Man läuft durch dieselbe Tür zurück;
Ortsknöpfe planen einen Weg und versetzen die Figur nicht. Die separate APK behält ihre ID
`com.notime.glyphminderwatch.godotproto` und kann den bisherigen Godot-Prototyp aktualisieren.

## Visuelle Übersetzung

Die gemalten Wohnzimmer- und Küstenbilder geben Farbpalette, Stoffe, Materialien und Einrichtung
vor. Dielen, Putz, Terrakotta, Leinen, Teppich, Kalkstein, Pflaster und Dachziegel sind neu erzeugte,
feine Materialtexturen. Sofa und Sessel haben gerundete Polster und Armrollen; Tisch, Bücher,
Keramik, Fensterrahmen, Vorhänge und Pflanzen besitzen eigene räumliche Formen. Das Küstenbild
hängt als kleines gerahmtes Bild im Zimmer. Ein ganzes Zimmerbild dient nicht als flache Wand.

Der Raum erhält Sonnenlicht durch echte Fensteröffnungen, darunter ein Bogenfenster. Warmes
Lampenlicht und dezentes Fülllicht halten die Möbel lesbar. Kamera und Wände arbeiten zusammen:
Dach, Veranda, nahe Wände und Deckenbalken werden für den Innenblick zu unsichtbaren
Schattenwerfern. Möbel bleiben sichtbar. Der Fußboden kann durch diese unsichtbaren Wände
angetippt werden; ihre physische Kollision bleibt bestehen.

Die Figur bleibt bewusst der vorhandene animierte Fennec als Billboard. Pflanzen verwenden
transparente gemalte Blätter auf gekreuzten 3D-Flächen; Gelände, Haus, Möbel und Wasser sind
räumliche Geometrie. Dies ist eine 3D-/2.5D-Umsetzung, keine automatische Bild-zu-3D-Rekonstruktion.
Volle Renderauflösung mit geglätteten Texturen ersetzt das grobe Pixel-Hochskalieren.

## Bewegung und Kamera

- Tippen: navigiert um Möbel, Baumstämme, Brunnen und Felsen; Doppeltipp: laufen.
- Steuerkreis links: direkte Bewegung; weit nach außen ziehen: laufen.
- Zwei Finger: Kamera drehen und zoomen; Pfeiltasten oben: 45° drehen; Kamera: Ansicht zurücksetzen.
- Orte: zum Wohnzimmer, zur Haustür, in den Garten, zum Strand oder ins Wasser gehen.
- Sitzen: am Boden ruhen; Hüpfen: echter Sprung mit Schwerkraft.
- Am Rechner: WASD/Pfeiltasten, Umschalt zum Laufen, Leertaste zum Hüpfen, Mausrad zum Zoomen.

Innen zeigt die Kamera den gesamten Raum, draußen folgt sie der Figur mit Blick in die
Laufrichtung. Das Ufer fällt kontinuierlich ab. Im Wasser werden Beine verdeckt und die
Bewegung langsamer. Zäune und eine Bojenleine markieren die Grenzen dieses kleinen Ausschnitts.

## Bauen und prüfen

Godot **4.4.1**, Compatibility/OpenGL, Querformat 1280×720 als Basis.

```sh
godot --headless --path godot-prototype --import
godot --headless --path godot-prototype --audio-driver Dummy -- --validate
godot --path godot-prototype --audio-driver Dummy -- --probe
```

`--validate` prüft echte Navigation, alle fünf Zielorte, Hin- und Rückweg durch die Tür,
Bodenkollision, Wassertiefe, Waten, Sprung/Landung, Tippen durch ausgeblendete Wände und
Steuerkreis-Eingaben (Headless: Eingabehandler; mit Fenster: Viewport-Ereignisse). Bei einem Fehler endet der Prozess mit Code 1.
`--probe` führt dieselben Prüfungen mit echten Screenshots (`user://world_*.png`) aus.
`--gallery` ist eine reine Entwickleransicht mit versetzter Figur und ersetzt keinen Wegtest.

`.github/workflows/godot-apk.yml` prüft Import und Navigation vor dem Export, prüft die APK-Signatur
und legt `Itoeva-Godot-Prototyp.apk` am bisherigen Drive-Ziel und als GitHub-Artefakt ab.

[Echte Renderbilder aus Godot](../docs/godot-prototype/README.md).

## Umfang und nächste Arbeit

Dieser Ausschnitt belegt die visuelle und räumliche Umsetzung des Hauses und seiner Küste.
Die übrigen bisherigen Orte, zusätzliche Zimmer, Spielstand, Living Agent, Erinnerungen und
Charakterwahl sind noch nicht in Godot portiert. Die vorhandenen Android-Apps bleiben separat.
Als Nächstes müssen Aussehen, Touchgefühl und Bildrate auf Martins Telefon beurteilt werden;
danach kann derselbe Aufbau um Schlafzimmer, Bad und benachbarte Außenbereiche wachsen.
