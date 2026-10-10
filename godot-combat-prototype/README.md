# Itoeva: gemalter Fennec in einer Godot-Kampfstudie

Martin hat nach dem weiterhin puppenhaften echten 3D-Modell einen neuen Godot-Versuch
beauftragt. Dieser **getrennte Prototyp** testet gemalte Figuren, eine raeumliche Buehne
und kurze zeitgesteuerte Kampfaktionen. Kein vollständiger Engine-Wechsel und kein Ersatz
der Kotlin-App. Der frühere Godot-Versuch aus PR #343 dient als Referenz, wird nicht reaktiviert.

## Spielen

Godot **4.5.1 Standard**, Compatibility/OpenGL, GDScript, keine Plugins:

```sh
godot --headless --path godot-combat-prototype --import
godot --path godot-combat-prototype
```

**Waldwind**, **Vorstoß**, **Ausweichen** starten einzelne Abläufe. **Demo starten** wiederholt
diese drei Aktionen. **Pause / Weiter** hält die vollständige Kampfzeit an. Tastatur:
1 / 2 / 3, Leertaste. Alle Aktionen haben ausreichend große Touch-Schaltflächen.
Die Android-App heißt **Itoeva Kampfstudie**, Paket `com.notime.itoeva.godotcombat`;
sie installiert sich neben den vorhandenen Apps. Android 7/API 24 oder neuer, arm64.
Sie nutzt weder Netzwerkrechte noch persönliche Daten oder Spielstände.

## Was hier räumlich ist

- Echter perspektivischer 3D-Viewport mit Boden, Steinen, Trainingsstein, Tiefentest,
  Kontaktfläche und bewegten 3D-Blättern. Die Landschaft ist eine gemalte Hintergrundfläche.
- Fennec ist ein Sprite3D mit echter Transparenz und gemalten Körperformen. Acht gezeichnete
  Posen bilden einen neuen Zauberablauf, mit eigenen Fußankern. Keine Stauchung eines Standbilds.
  Die letzte Ruhepose verwendet wieder exakt Frame 0, statt das leicht abweichende Frame 7.
- Die Zeichnungsfolge spielt mit etwa acht Bildern pro Sekunde; Körperposition, Kamera,
  Geschosse und Trefferzeit laufen unabhängig davon pro Renderbild. Mehr Render-FPS erzeugen
  keine zusätzlichen Zeichnungen. Die finale Spriteanimation braucht weitere Zwischenbilder.
- Vorstoß und Ausweichen verwenden diese begrenzten Posen mit räumlicher Bewegung.
  Es sind noch keine vollständigen eigenen Lauf-/Nahkampf-/Ausweichserien.
- Gemaltes Figurenlicht bleibt erhalten: ein flaches Sprite erhält hier keine vorgetäuschte
  Körperbeleuchtung über seine flache Normale. Bodenschatten ist eine Kontaktfläche;
  keine dynamische Figurensilhouette, kein normalgemapptes Fell, kein weiches 3D-Skinning.
- Der Trainingsstein ist ein technischer Trefferempfänger, kein fertig gestalteter Gegner.
  Keine Gegner-KI, Trefferpunkte, Inventar, Kampfbalance oder Persistenz.

Die ursprüngliche Stadtrand-/Waldmalerei stammt aus
`app-sim/src/game/assets/world/street-park-forest.png` auf main `124186c`.
Die neue Bodenmaterial-Textur und Fennec-Posen verwenden denselben gemalten Stil;
Prompts und Herkunft: [assets/PROMPTS.md](assets/PROMPTS.md).
Kein Teich, Schwimmgebiet oder Schwimmablauf. Angeln ist nicht Bestandteil dieses Versuchs.

## Leistung prüfen

Die App zeigt tatsächliche FPS, p95 der letzten 600 Bildintervalle, Anzahl der Intervalle
über 33,34 ms und das erste gerenderte Bild seit Engine-Start. Diese Startzeit schließt
den Engine-Start vor dessen Zeitgeber und spätere Betriebssystemdarstellung nicht ein.
Alle drei Bildressourcen werden einmal geladen. 36 Blatt-Meshes bleiben in einem festen Pool;
kein Nachladen oder Erzeugen von Effekt-Nodes während der Aktionen. Ziel: bis zu 60 FPS.
3D-Auflösung 80 %, 2x MSAA, Oberfläche in voller Auflösung. Keine teure Schattenkarte.

```sh
godot --headless --path godot-combat-prototype --audio-driver Dummy -- --validate
python godot-combat-prototype/tests/check_assets.py
godot --path godot-combat-prototype --audio-driver Dummy -- --benchmark
godot --path godot-combat-prototype --audio-driver Dummy -- --probe
```

`--validate` prüft Abläufe bei 15/30/60/120 Hz, doppelte Aktionsstarts, große Zeitsprünge,
genau einen Zeitpunkt pro Aktion, Rückkehr zum Ausgangspunkt, Pool-/Texturidentität,
Tastatureingabe und echtes Einfrieren/Fortsetzen. Fehler ergeben Exit 1.
`--benchmark` beendet nach 20 Sekunden eine reale Render-Schleife und schreibt
`user://benchmark.json`. Nur die letzten 600 Bilder gehen in die Quantile ein.
`--probe` zeichnet definierte Ansichten und schreibt `user://probe.json`.

[Renderbilder, Film und belegte Prüfergebnisse](../docs/godot-combat/README.md).
**Die Filmaufnahme verwendet einen festen 60-Hz-Zeitschritt und beweist keine Geräte-FPS.**
Software-OpenGL in dieser Umgebung ist keine Android-GPU; die Handyprüfung bleibt offen.

## Android bauen

Offizielle Export-Templates **4.5.1.stable**, OpenJDK 17, Android SDK Platform 35,
Build Tools 35.0.0 und Platform Tools. In den Godot-Editor-Einstellungen Java-/SDK-Pfad
eintragen. Keystore lokal in Godot konfigurieren oder alle drei
`GODOT_ANDROID_KEYSTORE_DEBUG_PATH`, `GODOT_ANDROID_KEYSTORE_DEBUG_USER`,
`GODOT_ANDROID_KEYSTORE_DEBUG_PASSWORD` über die Umgebung setzen; keinen Schlüssel einchecken.

```sh
mkdir -p godot-combat-prototype/build
touch godot-combat-prototype/build/.gdignore
godot --headless --path godot-combat-prototype --export-debug Android build/Itoeva-Godot-Kampfstudie.apk
```

Die bereitgestellte APK wurde lokal mit einem separaten Prototyp-Debugschlüssel exportiert;
keine vorhandene Signier-/Drive-/Release-Pipeline wurde verändert. Falls ein späterer Build
einen anderen Schlüssel nutzt, muss nur diese separate, ungespeicherte Test-App deinstalliert
werden. APK-Signatur und 16-KiB-ZIP-Ausrichtung werden separat geprüft.
Die Schaltfläche **Lizenzen** zeigt Engine-Lizenz und die von der verwendeten Engine
gelieferten Drittanbietertexte offline.

## Nächster Schritt

Martins Handyprüfung: zuerst Stil und Fußkontakt, dann Demo und jede Einzelaktion, Pause,
Rückkehr aus dem Hintergrund sowie Bildzeiten beurteilen. Erst nach überzeugender
Figurenanimation und gemessener Telefonleistung einen größeren Welt-/Kampfport entscheiden.
Reminder, Living Agent, Spielstand und die bisherigen Kotlin-Apps sind hier nicht angebunden.
