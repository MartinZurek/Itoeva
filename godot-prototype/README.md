# Itoeva: gemalte 2D-Welt mit begehbarer Tiefe

Dieser kleine Godot-Ausschnitt verwendet **die originalen Itoeva-Bilder unverändert**:
Küstenpanorama, Wohnzimmer und animierter Fennec. Haus, Strand, Stege, Waldweg und Wasser
sind begehbar. Beide Bilder und sämtliche Vordergrundstücke werden einmal beim Start geladen.
Das Wohnzimmer öffnet sich als Innenansicht an der gemeinsamen Haustür. Beim Betreten und
Verlassen bleiben die Koordinaten der Figur unverändert; es gibt keinen Szenenwechsel.

Die ursprüngliche Beleuchtung der Bilder bleibt erhalten. Vordergrundstücke verwenden dieselben
Bildpixel, stehen aber abhängig von der Fußposition vor oder hinter der Figur. Begehbare
Bodenflächen, Möbelgrundrisse, Sprunghöhe und perspektivische Figurengröße geben dem flachen
Bild räumliche Tiefe. Feine Wasserbewegung, Eintauchen und schwingende Pflanzenspitzen beleben
es. Die Figur läuft nicht über die gesamte Bildfläche: Wände, Möbel, Felsen und Bildhorizont
begrenzen diesen Ausschnitt. Berge und die ferne Ortschaft sind hier Hintergrund.

## Bedienung

- Tippen auf den Boden: gehen; Doppeltipp: laufen; Figur antippen: Freude.
- Steuerkreis links: freie Bewegung nach links/rechts und nach hinten/vorne; außen: laufen.
- Zwei Finger auseinanderziehen: näher ansehen; **Ansicht**: Überblick wiederherstellen.
- **Orte**: einen Weg zum Wohnzimmer, zur Tür, zum Strand, Steg, Wald oder Wasser planen.
- **Ins Haus / Zur Küste**: durch dieselbe Tür hinein- oder hinausgehen.
- **Sitzen**, **Hüpfen**; am Rechner WASD/Pfeile, Umschalt, Leertaste und Mausrad.

Die Kamera folgt draußen auf dem Panorama. Innen zeigt sie in der Grundansicht das ganze
Zimmer, auch bei breiteren Telefonformaten. Beim Zoomen folgt sie innerhalb dessen Grenzen.
Im Wasser wird die Figur langsamer, ihre unteren Körperteile werden entsprechend der Tiefe
verdeckt. Wege laufen um Hindernisse; die Ortsauswahl teleportiert die Figur nicht.

## Lizenz

Godot ist unter der MIT-Lizenz veröffentlicht: kommerzielle Spiele sind erlaubt, ohne
Lizenzgebühr oder Umsatzbeteiligung; der eigene Spielcode muss nicht offengelegt werden.
Die Engine-Lizenz und Hinweise zu mitgelieferten Komponenten müssen verfügbar sein.
Die Schaltfläche **Lizenzen** enthält die Texte aus der tatsächlich verwendeten Engine,
inklusive Copyrightangaben und Drittanbieter-Lizenztexten, offline in der APK.
Bilder, Musik und zusätzlich eingebaute Assets haben jeweils eigene Rechtebedingungen.

Offizielle Quellen: https://godotengine.org/license/ und
https://docs.godotengine.org/en/stable/about/complying_with_licenses.html

## Bauen und prüfen

Godot **4.4.1**, Compatibility/OpenGL, Querformat, 1280×720 als Basis:

```sh
godot --headless --path godot-prototype --import
godot --headless --path godot-prototype --audio-driver Dummy -- --validate
godot --path godot-prototype --audio-driver Dummy -- --probe
```

`--validate` prüft tatsächliche Hin-/Rückwege zu den Orten, Ein-/Austritt ohne Versetzen,
Wassertiefe, perspektivische Größe, Weg hinter eine Vordergrundpflanze, Touch-Joystick,
Sprunghöhe und gleichbleibende Bildidentitäten/Anzahl der Nodes. Bei Fehlern endet es mit Code 1.
Mit Fenster führt `--probe` dieselben Prüfungen über den Viewport aus und speichert echte
Renderbilder unter `user://2d_*.png`. Headless-Eingaben benutzen den identischen Eingabehandler.
Die APK-Pipeline prüft Import und Weltverhalten vor Export und Signaturprüfung.

[Echte Godot-Ansichten](../docs/godot-prototype/README.md).

## Umfang

Dieser Prototyp ersetzt den 3D-Versuch. Die 3D-Szenen und zugehörigen Material-/Pflanzenassets
sind daraus entfernt. Er ist ein erster begehbarer 2D-Ausschnitt, keine vollständige Portierung
aller bisherigen Räume. Weitere Innenräume, angrenzende Panoramen, Spielstand, Living Agent
und Reminder sind noch nicht portiert. Die übrigen Android-Apps bleiben separat.

Die APK verwendet weiterhin `com.notime.glyphminderwatch.godotproto` und aktualisiert den
Godot-Prototyp. Bildrate und physische Touchbedienung auf Martins Telefon müssen dort geprüft
werden. Der nächste Ausbau kann dieselbe residente Welt und eindeutig zugeordnete Türen nutzen.
