# Bewohner, Tageslicht und Reisemusik

Die Bewohner haben Tagespläne mit festgelegten Tätigkeiten. Ein Bote trägt seine Karte auf dem Weg, Nella zeichnet an den Windterrassen, und Starlet prüft nachmittags das Licht an der Signalwarte. Puffling macht kurze Aufwärmsprünge. Wer am selben Ort die nächste Aufgabe übernimmt, geht zu seinem neuen Platz. Ein Wechsel in einen anderen Bereich wartet in direkter Nähe von Mooslyn, damit Figuren beim Vorübergehen sichtbar zusammenbleiben.

| Figur | Rolle in der Welt | Hilfe im Gespräch |
| --- | --- | --- |
| Fennec | Botenwege und Küstenroute | Küstenweg einzeichnen |
| Gloop | Becken und Ufer beobachten | Wasserstellen zeigen |
| Puffling | Am Sportweg und Dorfrand trainieren | Mit einer Atemübung Ausdauer erholen |
| Wyrmling | Am Werkplatz arbeiten und Material begutachten | Werkbank und Übungsduelle zeigen |
| Starlet | Licht beobachten und Abendwege begleiten | Ein Weglicht teilen |
| Hootlet | Lesen und Chroniken pflegen | Reisetagebuch öffnen |
| Nocti | Nachtwache halten und am Waldsaum lauschen | Waldpfad einzeichnen |
| Nella | Küstenskizzen prüfen und den Hochweg zeichnen | Signalwarte und Gebirgspass im Atlas verbinden |
| Brumm | Brot backen, am Markt anbieten und heimtragen | Mit einer Brotpause Ausdauer erholen |
| Tilla | Pflanzen und Blätter untersuchen | Blütenstelle im Sonnenhain einzeichnen |
| Miro | Wasserrad und Zulauf prüfen | Den Mühlenbach und seinen Hebel zeigen |

Mooslyn bleibt die spielbare Hauptfigur. Die Porträtrahmen der Bewohner sitzen enger an ihren Silhouetten und halten etwa bei Brumm, Nella und dem Aschenhund Randfragmente aus den Dialogen heraus. Der Atlas unterscheidet eingezeichnete Wegtipps von besuchten Orten. Gesperrte Wege bleiben an die jeweiligen Voraussetzungen gebunden.

## Licht, Ladebilder und Ton

Die Berliner Spieluhr unterscheidet Morgendämmerung von 06 bis 08 Uhr, Tag bis 18 Uhr, Abenddämmerung bis 21 Uhr und Nacht. Spiellandschaft, Wasserreflexe und Ladebilder folgen denselben Stunden. Die Ladekarte zeigt Ortsmotiv, Herkunft und Tagesphase. Beide neuen Hochlandorte verwenden ihre jeweiligen Landschaften.

Die Auswahl der Reisemusik folgt elf Ortsgruppen. Küste, Feuchtgebiete, Wald, Hochland, Grenzland, Siedlungen und die öffentlichen Innenräume haben eigene Reihenfolgen. Elf Ogg-Dateien stammen unverändert aus dem älteren Android-Zweig. Herkunft und SHA-256 stehen im Musikkatalog. Automatische Messungen der integrierten Lautheit und Spitzenpegel liefern die Lautstärkeanpassung pro Stück.

Beim Wechsel der Region mischt das Spiel die Titel etwa 2,35 Sekunden übereinander. Kurze Grenzübertritte lösen den Wechsel erst nach einer stabilen Ankunft aus. Morgenstücke, Abendstücke und das Traumstück ergänzen die Ortsauswahl abhängig von der Spieluhr. Das Tempo fährt stufenlos von 100 Prozent am Tag bis auf 89 Prozent in der Nacht. Ein separater Effekt gleicht die Tonhöhe aus. Vorhandene Musikeinstellungen werden gelesen, und neue Spielstände starten mit aktivierter Weltmusik.

## Prüfung

`godot --headless --path . --script tests/residents_daylight_music_play.gd` prüft Figurenprofile und Porträts, Ortsmusik und ladbare Stücke, Tagesphasen, Überblendung, Ladekarten und die Hochlandroute aus Nellas Gespräch.
