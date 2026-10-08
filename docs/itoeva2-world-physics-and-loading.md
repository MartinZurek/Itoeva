# Itoeva 2: Wasser, Bewegungsgrenzen, Türen und Laden

Stand: 09.10.2026. Martins Auftrag erweitert den bereits gemergten Maßstabs-, Licht- und
Weltkonturschnitt (#346/#347). Merge und Release-APK übernimmt Martin mit Cloud Code.
Dieser Schnitt basiert zusätzlich auf Cloud Codes `b8d442896af19f58d11ada4231065cb31ad9ed13`:
Rohbilder bleiben ohne minutenlange Bodenlichtkorrektur verfügbar.

## Sichtbarer Weltcheck

Alle 28 Spielorte wurden über ihre sieben Landschaftsbilder, sechs Anschlussbilder und elf
Innenbilder betrachtet. Begehbarer Boden, feste Körper und fern gemalte Kulisse haben verschiedene
Folgen. Die bestehenden Maße, Lichtquellen, Pflanz-, Stoff-, Feuer- und Dunstregungen bleiben aktiv.

| Orte / Bild | Plausibilität und umgesetzte Folge |
| --- | --- |
| POND, BEACH, SWAMP, JUNGLE / coast | Vermessene Uferlinie; Stege und Felsen sind trocken und fest. Im Wasser greifen Widerstand, Wasserlinie, Zugpose, Wassertreten, Wellen, Ringe und Spritzer. Rollen/Bodensitzen wird dort zu Wassertreten. Vordergrund und ferner Meerhorizont begrenzen den Bewegungsraum. |
| COAST_PATH / coast-path | Der vermessene Brückenbogen trägt die Figur. Anschlusswege sind schmaler. Flussflächen bewegen sich optisch; fernes Wasser ist keine zusätzliche begehbare Schwimmzone. |
| STREET, PARK, MEADOW, FOREST / street-park-forest | Wege statt frei begehbarem Hintergrund; Anlaufkorridore zu Hause/Laden. Parkbank und Waldstamm behalten tatsächliche Sitz-/Kollisionsanker. Die erreichbare Parkkiste liegt auf dem Weg. |
| VILLAGE_EDGE / village-edge | Gemeinsame hintere und vordere Weggrenze; stetiger Anschluss an Nachbarlandschaften. Häuser, Himmel und Hintergrundbäume bleiben Kulisse. |
| CITY, SPORT, PLAINS, MOUNTAINS / uplands | Begrenzter Boden und echte Anlaufpunkte für Arbeit, Café und Spielhalle. Keine erfundene Stadtbank; der Bergstein besitzt festen Körper. |
| MOUNTAIN_PASS / mountain-pass | Begehbarer Weg zwischen den Bildgrenzen; Felswände und ferner Himmel werden nicht als zusätzlicher Boden behandelt. |
| CAMP, GROTTO / expedition | Lagerstamm und Bodenmaterial korrigiert; keine erfundene Grottenbank. Vorhandenes Feuer/Kristalllicht bleibt aktiv. Ein gemalter ferner Gegenstand wird nicht automatisch zur neuen Aktion. |
| BEDROOM, BATH, DESK, NOOK, LIVING, KITCHEN, CRAFT, SHOP, WORK, CAFE, ARCADE | Bestehende Möbelkörper, Sitze, Lichtblocker und Verdeckungen. Sprünge prüfen den durchlaufenen Weg gegen feste Körper und bleiben unter der Raumhöhe. Türen öffnen am gemalten Durchgang, erst dann erfolgt der Ortswechsel; die Ankunftstür schließt. Küche nutzt für Wohnzimmer/Werkraum dieselbe echte Flurtür mit Zielauswahl. |

Das sind stilisierte 2D-Regeln. Nicht jedes Dekorationspixel besitzt einen Kollisionskörper;
ein vollständiger Flüssigkeits-/Stoff-/3D-Simulator ist nicht gebaut. Die Schwimmbewegung verwendet
vorhandene Richtungs-/Streckframes mit Zugphase, Neigung und artspezifischem Auftrieb. Es gibt
keine neue handgezeichnete Schwimmsprite-Serie. Telefon/GPU und das tatsächliche Bewegungsgefühl
müssen weiterhin in der APK geprüft werden.

## Gemeinsame Modelle

- `GameTerrain` ist der Game-Adapter von `GameMovement`, kein zweiter Bewegungsmotor.
  Projektion und Außennaht bleiben unverändert; Gehen, sichere Speicherpunkte und Anlaufknoten
  verwenden denselben legalen Boden. Druck gegen eine Grenze erzeugt keinen Laufzyklus.
- `GameWater` vermisst Wasserfarben einmal in einer kleinen Maske. Nur diese Ausschnitte
  bewegen sich; Land/Steg bleiben fest. Spieler und Bewohner teilen Zugpose/Wasserlinie;
  echte Bewohnerbewegung erzeugt begrenzte Kontakte. Explizite Übergaben behalten Vorrang.
- `GameDoors` teilt Bildöffnung, Trefferfläche, Anlaufpunkt und Animationsuhr.
  Die Zielauswahl liegt über der Daumensteuerung. Deaktivierte Steuerung hält keinen
  vollflächigen Pointer-Input-Layer fest.
- Sprungkollision läuft nur im Game-Client über zwölf Wegproben; App 1 behält den bisherigen
  Standard. Eine abgebrochene Flugbahn fällt an ihrem letzten freien Punkt und erzeugt
  keinen vorzeitigen Landekontakt am ursprünglich geplanten Ziel.

## Start und Hintergrundarbeit

`GameAssetPlan` ordnet die vorhandenen 24 Welt-/Anschluss-/Innenbilder: aktuellen Ort und
Nachbarn zuerst. `GameAssetLoader` dekodiert außerhalb des UI-Threads und wärmt die sechs
Figurensheets vor. Die deckende Startfläche zeigt vorhandene Weltkunst und den echten Fortschritt
der 30 Vorbereitungen. Das alte Raster wird im Game-Modus nicht aufgebaut. Der erste
Kamerarahmen wird unabhängig von pausierenden Menüs initialisiert. Zurück während Laden
verlässt den Spielbildschirm; ein fehlendes Bild bietet erneuten Versuch statt Endlosschleife.

Nach dem Start folgt zusätzliche Bodenlichtkorrektur mit Zeilenpuffern in 32-Zeilen-Blöcken,
wenn die Spielfigur ruhig ist. Sie verwendet die einmal geschätzten Lichtfelder, gepufferte
Möbelgeometrie und konservative Achter-Pixel-Blöcke. Sichtbare Bitmaps werden nie verändert
oder recycelt. Vollständige korrigierte Kopien ersetzen sie; starke Rohbildreferenzen werden
je Bild freigegeben. Ein weicher Prozesscache kann fertige Bilder beim Wiederbetreten nutzen
und bei Speicherdruck freigeben. Optionale Lichtkopien dürfen ausfallen, ohne das Spiel zu sperren.
Ortswechsel warten auf kein weiteres Asset-IO. Das ist kein gemessener Telefon-Speicher-/FPS-Nachweis.

`GameBreath` hält eine lokale, nicht gespeicherte Ausdauerreserve: ungefähr zwölf Sekunden
tatsächliches Rennen, dann 1,2 Sekunden sichtbares Durchatmen. Stehen/Gehen füllt die Reserve;
Wanddruck und langsames Gehen verbrauchen nichts. Die Pause hängt nicht von einer Ladeaufgabe
ab. Es gibt keinen Hunger-, Gesundheits-, Beziehungs- oder XP-Abzug. Hintergrundarbeit nutzt
auch normales Stehen und benötigt keine absichtlich erzeugte Wartezeit.

Das rechte Aktionspad bleibt sichtbar, auch wenn die Eingabe kurz deaktiviert ist. Dunkler Grund
und stärkere Konturen erleichtern die Erkennung: tippen = springen, wischen = rollen,
halten = hinsetzen. Die genaue Ursache des in Martins APK fehlenden Pads wurde hier nicht am
Telefon reproduziert; Sichtbarkeit, Eingabesemantik und Mehrfingerbedienung werden geprüft.

## Prüfung und Rückbau

- Reine Kotlin-Suite: 1.009 Tests, darunter 20 neue Physik-/Ladeplan-/Ausdauerregressionen.
  Sie prüfen Außenränder, alte Positionsdaten, Stege, Wasser, Sprungkörper, Raumgrenzen,
  alle 22 Türwege, Kontakte, Zugpose, Material, Ausdauer und Vorladeplan.
- Native Compose-Tests prüfen Mehrfinger-/Sprung-/Roll-/Halte-/Abbruchbedienung, sichtbares
  deaktiviertes Pad, deckenden Ladebildschirm, Fortschritt, Retry und echte Pointer-Klicks
  auf beide Flurziele. Ausführung erfolgt mit Verify auf dem PR-Kopf.
- `preview_physics.sh` nutzt produktive Modelle und vorhandene Bilder. `physics-swimming.jpg`
  zeigt sechs Spezies; `physics-boundaries.jpg` alle sieben Landschaftsgrenzen;
  `physics-motion.mp4` Wasserbewegung und Türöffnung. Diese sind keine APK-Aufnahmen.
- Zweite unabhängige Lesekontrolle: Rohbildreferenzen, Lade-Back-Blockade und Touch-Reihenfolge
  wurden korrigiert, ebenso Cue-Vorrang, alte Positionsdaten und echte Landekontakte.

Native Kompilierung, Lint/R8 und Emulatorprüfung müssen am veröffentlichten PR grün sein.
Martins APK-Abnahme: Kaltstart ohne Raster, Zurück/Retry, rechtes Pad mit zwei Daumen,
alle sechs Wesen im Wasser, trockene Stege, Grenzen/Sprünge, alle Türen samt Küchenflur,
zwölf Sekunden Rennen/Erholung und längerer Landschaftslauf ohne späte Ladestopps.

Keine neuen Orts-IDs, Spielstandversionen, Room-Schemas, Preferences oder Workflows.
Der V2-Codec liest alte normalisierte Positionen unverändert; der aktive Spielclient begrenzt
sie beim Gehen/Sichern. Rücknahme dieses Schnitts behält den bereits gemergten V2-Begegnungsreader
und Cloud Codes schnelle Rohbildanzeige. Es ist kein Revert von #347 auf den V1-Spielstand.
