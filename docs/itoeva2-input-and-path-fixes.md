# Itoeva 2: Bedienung, Eingänge und Brückenweg

Stand: 09.10.2026. Fortsetzung von PR #349 (`8908a0f`). Dieser Korrekturbranch
setzt dessen Physik-/Ladefunktionen voraus. Merge und APK übernimmt Martin mit Cloud Code.

## Korrekturen

- Rechtes Sprung-/Roll-/Sitzpad: eigener Pointerhandler, eigener Fingerzustand,
  aktuelle Callbacks bei Neukomposition. Darstellung transparent wie der linke Stick.
  Linkes Steuern und rechte Aktion bleiben gleichzeitig möglich. Die genaue Ursache
  des Ausfalls in Martins Telefon-APK ist hier nicht reproduziert; die Eingabekette
  wurde robuster gemacht; native Android-Pointertests sichern die Kette ab.
- Türen und deren Namen: ein Tipp genügt. Die Trefferprüfung umfasst alle sichtbaren
  Weltabschnitte, nicht nur `currentPlace`. Der Avatar geht zum sichtbaren Eingang
  auch über Abschnittsgrenzen. Ein laufender Weg bleibt abbrechbar.
  Die Figur behält Tipp-Priorität vor einer überlappenden Tür; ihr Menü bleibt erreichbar.
- Shop-/Aktions- und Abbruchknöpfe liegen in der Komposition über dem Touchlayer;
  ihr bestehender `zIndex(2f)` bleibt. Gegenstände und Stationen reagieren ebenfalls
  auf einen Tipp. Bodensitzen blockiert einen Türdurchgang nicht mehr; Sprünge,
  Rollen und erhöhte Standflächen werden weiterhin nicht mitten im Ablauf verlassen.
- Brücke im Küstenpfad: neu vermessener Weg, an der Krone y=370–396 im 1920×640-Bild.
  Die vorherige Fläche y=408–449 lag auf der Steinbogenfront. Bodenprojektion,
  manuelles Gehen/Rennen und automatischer Anlauf benutzen dieselbe korrigierte Fläche.
- Wasser: vordere Flächen bewegen sich binnen etwa einer Viertelsekunde sichtbar.
  Bestehende Wasser-/Steg-/Felsmasken und unveränderte Bilder werden weiterverwendet.

## Prüfstand

- Kotlin-Gesamtsuite: 1.015 Tests grün.
- Neue Regressionen: Brückenkrone, Gehen/Rennen in beiden Richtungen, benachbarter
  Shop und seine Beschriftung, schnelleres Vorderwasser und Türeingabe im Bodensitzen.
- Native Compose: bestehende Mehrfinger-, Roll-, Halte-, Abbruch- und Sperrtests;
  zusätzlich Pointerklick auf einen Aktionsknopf und rechter Pad-Klick → produktiver
  Bewegungsmotor → sichtbare Sprunghöhe → zweiter Sprung nach Neukomposition.
- Android-CI am ersten Kopf `fb2e925`: beide Emulatorläufe grün, einschließlich
  111 Tests auf API 26 mit den neuen Pad-/Sprungtests. Ein automatischer P2-Befund
  zur Menüpriorität vor einer Tür wurde danach korrigiert; der finale Kopf wird erneut geprüft. Lokale reine Kotlin-Tests
  ersetzen weder Compose noch Telefon-/GPU-Abnahme. Hier keine APK erstellt.

## Nächster Schritt

Android-Prüfung des Korrekturkopfs abwarten. PR #349 und diesen darauf aufbauenden
Korrekturstand gemeinsam in Cloud Code übernehmen; erst dann die APK bauen. Am Telefon
rechts tippen/wischen/halten, gleichzeitig links steuern, die Brücke aus beiden
Richtungen überqueren und Shop/Nachbartür/Beschriftung einfach antippen.

Keine Änderungen an Saveformat, Room, Ort-IDs, Signierung oder Workflows.
