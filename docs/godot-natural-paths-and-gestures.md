# Godot v0.2.3: natuerliche Grenzen, Gestenkreis und Aktionen in der Welt

Stand: 2026-10-11. Martins Auftrag: goldene Weglinien entfernen, Wege ueber die
Kulisse erkennbar machen, Bruecken an ihre Decks abstimmen, einen einzelnen
rechten Gestenkreis wiederherstellen und Figuren innen/aussen angleichen.

## Verhalten

- Goldener Linienmesh, Shader und Einstellung entfallen. Alte lokale
  `paths_visible`-Werte werden ignoriert, beim naechsten Einstellungscommit entfernt.
- Kleine gemalte Steingruppen sitzen ausserhalb ausgewaehlter unklarer Wegraender.
  Ein niedriges Holzgeländer begrenzt die Rueckseite des Sumpfstegs. Die Kulissen
  behalten ihre Erd-, Sand- und Steinwege; kein flächendeckender Pflasterersatz.
- Der Holzsteg war unterhalb seiner Planken begehbar: die Kontur folgt nun
  y=400..422 der 1920x640-Referenz. Die Steinbruecke lag zu weit vor dem Weg:
  die Mitte folgt jetzt y=365..390, hinter ihrer Bruestung. Sicherheitsabstand
  und Figur-Radius gelten weiterhin; Wasser und Bruestung sind gesperrt.
- Die Bildflaeche aller begehbaren Pixel liegt auf der physischen Bodenebene.
  Der vorherige Reliefbeginn bei y=450 verschob hoehere Brueckenbilder gegen die
  flache Physik. An Kueste/Bruecken bleibt die gezeichnete Kamerablickachse fest;
  Zoomen und Folgen funktionieren weiterhin. Andere Aussenregionen behalten
  den begrenzten Schwenk.
- Rechte Hand: **Tippen = Sprung; Wischen = Rolle in jede Richtung;
  500 ms Halten = Sitzen/Aufstehen**. Links bleibt der analoge Geh-/Rennstick.
  Abbruch, Pause und Fokusverlust verwerfen gehaltene Aktionen. Seitliche und
  Tiefenrollen werden nicht durch die linke Hand ersetzt. Oben bleiben nur
  Menue und Karte; Touch-Zoom steht in den Einstellungen.
- Sprechblasen, Tuerpfeile und Sitzsymbole werden mit der aktuellen Kamera
  projiziert. Sie sind bei wirklicher Reichweite antippbar und zeigen dann ihren
  Namen. Die gleiche Kandidatenpruefung loest die Interaktion aus. Ein Tipp auf
  Hootlets Blase spricht Hootlet an, nicht einen zufaellig anderen Nachbarn.
  Dezente kleine Pfeile weisen am Bildrand auf begehbare Fortsetzungen hin.
  Tuerhinweise folgen dem Originalanker, auch wenn die sichere Standposition
  vor einem Sofa liegt; sie werden nicht zusammen mit dem Spieler versetzt.
- Figuren verwenden in jedem Raum, draussen und bei Abendlicht `Color.WHITE`
  und die unveraenderte Atlaspalette. Zusätzliche Entsaettigung und Raumtoenung
  entfallen. Kameraangepasster Innenmassstab liefert dieselbe Referenzhoehe wie
  draussen (Fennec rund 119 Bildpixel bei 720p und Standardzoom). Relative
  Artgroessen, Fussanker, Originalbewegungen und lineare Filterung bleiben.

## Nachweise

Godot 4.5.1: 117 neue Gesten-/Kulissenchecks, 291 Aktionsgrenzchecks,
2048 Geometriechecks mit echter Bewegung durch 411 Wegpunkte je Richtung,
76 Laufzeitchecks, 132 gerichtete Spezies/Tuerpfade und 66 Raum/Spezies-Bildrahmen.
100 Renderpaare pruefen Vollueberdeckung; sechs weitere Bilder zeigen die
aktuelle Bedienung, beide Bruecken und Innen-/Aussenfiguren mit Bodenkontakt.
Alle Tests sind ohne Fehler abgeschlossen. Die Originale sind weiterhin
25-mal byte-identisch und 6x170-mal frame-identisch.

Beim Durchlaufen aller Tueren war die Wiederholung identischer Bildspalten
unnötig teuer. Der Buehnenaufbau speichert ihre exakt ausgewerteten Profile
lokal zwischen; die Physik rundet oder caches keine Bewegungspunkte.
Der komplette Tuerlauf ist danach wieder ohne Timeout abgeschlossen.

Desktop-Beispiele: `prototypes/godot-hd2d/preview/natural-*.jpg`.
Keine dieser Aufnahmen stammt von einer Android-Installation. Telefon-GPU,
FPS/Temperatur und echte Android-Beruehrungen bleiben noch abzupruefen.
Die Welt bleibt eine gerichtete Reliefbuehne; Pflanzenkonturen sind weiterhin
polygonale Naeherungen, Innenbilder behalten ihre originale Aufloesung.

## Bildasset und Prompt

Built-in Imagegen wurde fuer die transparente Steingruppe verwendet.
Referenz: `prototypes/godot-hd2d/assets/world/coast-path.png`.
Finales Projektasset: `prototypes/godot-hd2d/assets/world/path-stones.png`.
Das Originalpanorama wird nicht ersetzt. Der RGBA-Output bleibt unveraendert.
Das Holzgeländer und die UI-Symbole werden direkt in Godot gezeichnet.

Verwendeter Prompt:

> Use case: background-extraction / game environment prop. Extract and adapt the small warm gray and ochre foreground stones from this painted landscape into ONE isolated low cluster of five small pebbles and two tiny sparse grass tufts, for a natural path edge in the same game. This is a transparent prop, not a landscape edit. Preserve the hand painted pencil/brush grain, the soft cream sunlit facets and warm grey-brown shadows of the supplied scene. View from the landscape's slightly elevated fixed camera, visible front and top of stones, not an overhead plan. Composition: horizontally wide low silhouette, roughly 3:1 aspect, generously padded with true transparent background on ALL sides; nothing touches the canvas edges. Stones are modest, irregular, varied sizes with spaces, not a continuous paving strip, no fence. Only this small natural cluster, no landscape, no water, no characters, no lettering, no colored outline or golden line, no external shadow extending far from the cluster. Intended for placement just outside a walkable game path. High quality clean RGBA prop.

## Build und Ruecknahme

APK 0.2.3 / Code 6, Paket `com.itoeva.godot.hd2d.world`, ARM64, API 24/35,
Querformat, ohne Internetberechtigung. Derselbe Debug-Schluessel wie zuvor.
Keine neue native Migration, Orts-ID oder Godot-Spielstandversion. Ein Revert
stellt v0.2.2 wieder her und erhaelt bestehende Wesen-/Raumdaten.
78.759.206 Bytes; SHA-256:
`fdb243f11a40a8c771d779c258aaf70559ac3b68871d3cf5ada29ba49756aee8`.
ZIP/Manifest und aktuelle Geometrie/Shader sind geprueft; der alte Linien-Shader
ist nicht exportiert. Signaturen v2/v3 und 16-KiB-ZIP-Ausrichtung sind bestanden.
Auch die kompilierten .gdc-Spielskripte aus genau dieser APK wurden unter
Godot 4.5.1 ausgefuehrt: dieselben 117 Gesten-/Kulissenchecks ohne Fehler.
Das ersetzt keinen Test der Android-Touch-Ereignisse auf einem Telefon.

Quellen fuer die technische Referenz:
[Godot Touch-Ereignisse](https://docs.godotengine.org/en/4.5/classes/class_inputeventscreentouch.html),
[native Itoeva-Gesten](../app-sim/src/main/java/com/notime/glyphsim/ui/GameControls.kt),
[HD-2D-Entwicklerinterview](https://www.unrealengine.com/developer-interviews/octopath-traveler-ii-builds-a-bigger-bolder-world-in-its-stunning-hd-2d-style?lang=en-US).
