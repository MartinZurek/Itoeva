# Godot 0.6.0 – Feuer und lokale Lichtquellen

Martins Zusatzauftrag vom 14.10.2026: Vor dem APK-Build Feuer und Licht nach
der visuellen Referenz Final Fantasy Resonance verbessern. Die offizielle
Square-Enix-Präsentation beschreibt HD-2D und filmische Pixelgestaltung:
https://blog.playstation.com/2026/09/03/final-fantasy-resonance-ps5-demo-available-today-sephiroth-revealed/

## Umsetzung

Die eigenen gemalten Feuerbilder behalten ihre Identität. Verankerte
Flammenspitzen bewegen sich, vier sparsame Glutfunken steigen lokal auf.
Jede Feuerstelle hat einen eigenen Rhythmus; Flamme, Kulisse und Figur
teilen denselben Takt. Lampen bleiben ruhig. Ein dichter heller Kern und
weicher begrenzter Lichtabfall erhalten Pinselstriche. Tageslicht reduziert
künstliches Licht wie bisher.

Alle 64 Quellen der Originalpanoramen, Anschlussbilder und Innenräume sowie
Wald-Fackeln liefern Lichtanker für Figuren. Höchstens vier nahe Quellen
beleuchten eine Figur gleichzeitig. Der zugewandte Rand erhält mehr warmes
Licht. Blickrichtung, Sprite-Alpha und Möbelverdeckung bleiben unabhängig.
Ausgeschaltete Quellen werden vor der Auswahl entfernt. Arena-Fackeln
verlieren beim Erlöschen auch ihre Wirkung auf Figuren.

Die Traglaterne hat durchscheinendes Glas, vier Messingstreben, einen offenen
Bügel und einen animierten Docht. Sie erhellt einen lokalen Bereich im dunklen
Wald. Ohne Traglicht bleibt die Figur entsprechend der Waldregion abgedunkelt;
die vorherige pauschale warme Aufhellung des ganzen Wesens entfällt.

## Prüfung

Godot 4.5.1, echter Software-OpenGL-Renderer: Ambience 9, Wald 7 und neue
Figurenlicht-Prüfung 9 Checks ohne Fehler. Die neue Prüfung misst die
quellenzugewandte Lichtseite, Flackern, transparente Silhouette, Überbelichtung,
pixelgleiche Rückkehr ohne Quellen sowie Arena- und Laternenabschaltung.
Echte Spielaufnahmen von Lager, Wohnzimmer, Fackelpfad und Traglaterne an/aus
wurden visuell geprüft. Keine Concept Art als Spielaufnahme ausgegeben.

Regressionen: Wald 22.027; Ambience-Koordinaten 225; Gesten/Wege 143;
40-Karten-Erweiterung 441; Kampfspektakel 26; vollständiger Smoke 78 Checks,
alle ohne Fehler. Der APK-Workflow prüft zusätzlich die Karten-/Türregressionen
und rendert die Helfer sowie Feuer-/Lichtprüfungen vor dem Android-Export.

## Grenzen und Übergabe

Stilisierte Beleuchtung gemalter Reliefkulissen, keine Rekonstruktion von
Final Fantasys proprietärer Grafiktechnik. Kulissenschatten sind weiterhin
Teil der Bilder; neue physikalische Schatten für sämtliche Möbel sind nicht
enthalten. Android-GPU/FPS und Telefonstart bleiben Geräteabnahme.

Basis: 2ca28b5 (40-Karten-/Helferstand), davor e4983fe (0.5.0-Build).
Der integrierte Branch codex/godot-firelight-polish enthält Karten und Licht.
Runtime-Manifest: 215 Dateien. AndroidTest bleibt 0.6.0/Code 16 mit vorhandenem
DEBUG_KEYSTORE_BASE64 und derselben Drive-Datei-ID. Erst der geprüfte finale
Branch-Push startet den neuen Build. Native Daten und Godot-Spielstände
ändern sich durch diesen Lichtschritt nicht. Nur den Lichtcommit zurücknehmen,
um die bisherige Darstellung mit 40-Karten-Stand wiederherzustellen.
