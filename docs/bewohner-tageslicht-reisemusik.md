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

Morgenstücke, Abendstücke und das Traumstück ergänzen die Ortsauswahl abhängig von der Spieluhr. Das Tempo fährt stufenlos von 100 Prozent am Tag bis auf 89 Prozent in der Nacht. Ein separater Effekt gleicht die Tonhöhe aus. Vorhandene Musikeinstellungen werden gelesen, und neue Spielstände starten mit aktivierter Weltmusik.

## Prüfung

`godot --headless --path . --script tests/residents_daylight_music_play.gd` prüft Figurenprofile und Porträts, Ortsmusik und ladbare Stücke, Tagesphasen, Überblendung, Ladekarten und die Hochlandroute aus Nellas Gespräch.

## Wechsel auf der Reise

Ein Raumwechsel lässt den laufenden Titel über das Ladebild weiterklingen. In der durchgehenden Welt muss Mooslyn den Grenzbereich erst hinter sich lassen. Ein neuer Musikbereich gilt nach einer kurzen bestätigten Ankunft. Kehrt Mooslyn vor dem musikalischen Ausstieg in den Grenzbereich zurück, wird der vorgemerkte Wechsel verworfen.

Die Ausstiegspunkte liegen in vergleichsweise ruhigen Abschnitten der tatsächlichen Aufnahmen. Die Analyse berücksichtigt den mittleren Pegel und neue Klangimpulse. Das Spiel sucht den nächsten passenden Punkt innerhalb weniger Sekunden und hält bei fehlenden Markierungen den Wechsel vor dem Ende des Titels. Ruhige Stücke gehen fließend ineinander über. Bei ausgeprägten Rhythmen klingt das erste Stück früher ab, bevor das zweite hörbar in den Vordergrund kommt. Die beiden Mischkurven dauern rund 2,8 beziehungsweise 3,2 Sekunden.

`python tools/build_music_exit_points.py` erstellt die Markierungen mit FFmpeg und NumPy neu. Die 21 Weltmusikstücke haben insgesamt 693 Ausstiegspunkte. Eine Godot-Aufnahme der Folge Küste, Ortsgrenze, Stadt prüft, dass der Mix während des Wechsels durchgehend hörbar bleibt.

Als Referenz dienten die [Erfahrungen zu Übergängen in It Takes Two](https://www.youtube.com/watch?v=Ep0JO-m7G30) und die [Wwise-Dokumentation zu musikalischen Ausstiegspunkten](https://www.audiokinetic.com/download/documents/WwiseProjectAdventure_en.pdf).
