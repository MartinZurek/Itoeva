# Itoeva Godot 0.6.1 – Kampfklang und erkennbare Aktionen

## Bedienung

| Anzeige | Bedeutung |
|---|---|
| Helle blaue Kristalle | Aktuell verfügbarer Vorrat; Zahl nennt Vorrat/Kapazität. |
| Dunkle Kristalle | Bereits verbraucht; ganz dunkle Umrisse sind noch gesperrte Kapazität. |
| Goldene Kristalle | Kosten der ausgewählten Zielkarte. |
| Grüner Kartenrand | Nach den tatsächlichen Modellregeln jetzt spielbar. |
| Goldener Kartenrand und Zielringe | Auswahl aktiv; hervorgehoben sind nur regelkonforme Ziele. |
| Schlafzeichen, ruhige Pose und „Schläft“ | Helfer wartet auf seinen nächsten eigenen Zug. |
| Bereitschaftsring, gekreuzte Klingen und „Bereit“ | Helfer kann im aktuellen eigenen Zug angreifen. |
| „Erschöpft“ | Helfer hat bereits angegriffen. |
| „Festgehalten“ | Frost/übersprungener Zug verhindert den Angriff. |

Angriff und Lebenspunkte stehen dauerhaft direkt am Helfer. Spielfähige
Superkräfte erhalten ebenfalls einen helleren Rand. Die Zusammenfassung nennt
spielbare Karten, bereite Helfer und verfügbare Kräfte. Regeln und Kosten
bleiben unverändert; die Anzeige fragt CardBattle statt eigener Ersatzregeln.

## Klang und Musik

Die Weltpause des rundenbasierten Kampfes schaltet Kampfmusik nicht mehr ab.
BattleAudio mischt die vorhandene Weltmusik und einen eigenen Kampfkanal mit
1,2 Sekunden Übergang. Bestätigte Aktionen senken Musik kurz ab; die Wirkung
wird am tatsächlichen Trefferzeitpunkt hörbar. Zugwechsel und Sieg/Niederlage
besitzen eigene kurze Motive; Sieg erst nach der letzten Angriffsanimation.
Ein Pool von sechs Stimmen und kurze Wiederholungssperren begrenzen Klangstapel.
Menü/App-Hintergrund stoppen Effekte und pausieren Musik.

Kampfmusik ist die bereits im Repository vorhandene Itoeva-Komposition
**Continue?** (itoeva_arcade_02.ogg), bytegleich als battle-arena.ogg übernommen.
Kein neu komponierter Lang-Soundtrack, keine Musik/Samples von Final Fantasy
oder Hearthstone. 17 eigene kurze Quarz-, Holz-, Luft- und Elementklänge werden
reproduzierbar durch tools/audio/generate_battle_sfx.py erzeugt. Herkunft,
Dateihashes und Längen stehen in data/battle_audio.json.

„Ton“ im Kampf öffnet getrennte Schalter für Kampfmusik und Spielsounds sowie
Lautstärken. Ab Werk Kampfmusik an (35 %), Effekte an (55 %). Die vorhandene
Einstellung für Weltmusik bleibt erhalten; sie ist separat bezeichnet.

## Referenzen und Prüfung

Die offizielle Hearthstone-Fehlerliste bestätigt den Leuchtrahmen für spielbare
Karten als wesentliche Rückmeldung:
https://news.blizzard.com/en-us/article/24187196/32-0-patch-notes
Square Enix beschreibt den eigenständigen Stellenwert des Sounddesigns:
https://www.square-enix-games.com/en_US/news/final-fantasy-vii-rebirth-developer-comments
Diese Referenzen dienen Rückmeldung und Atmosphäre; alle eigenen Figuren,
Kartenregeln und vorhandenen Illustrationen bleiben erhalten.

Lokale Integration: 29 neue Prüfungen für Zustände, legales Highlighting,
Kristallkosten, Laden, Musik trotz Weltpause, Stummschaltung, Trefferzeitpunkt,
Siegeszeitpunkt und Menürückkehr. Vorhandene Regressionen: Kartenmodell 95,
Kartenregeln 15, Spezialregeln 82, Erweiterung 441, erweiterter Touch 8,
Minion-Touch 24, Karten-UI 69, Full Smoke 78. PCM-Prüfung: 17 unterschiedliche,
nicht stumme Stereo-Effekte mit 44,1 kHz, begrenzten Pegeln und weichen Enden.
Echte OpenGL-Aufnahmen von Übersicht, Zielwahl, Angriff und Tonmenü geprüft.

## Spielstand und Übergabe

Zwei optionale Helfer-Anzeigefelder spent/skipped werden mitgespeichert; die
bestehende Kartenschema-Version und alle Kampfregeln bleiben lesbar. Alte
Spielstände ergänzen diese Anzeigefelder neutral; nach dem nächsten eigenen
Zug gelten die vollständig aktuellen Anzeigen. Drei zusätzliche Audioeinstellungen
liegen nur im bestehenden lokalen Godot-Spielstand. Keine native Room-/LAS-Änderung.

AndroidTest 0.6.1/Code17, bestehende Test-Paketkennung und Signaturschlüssel.
Neue Runtime-Dateien im 0.6.1-Manifest. Workflow prüft zusätzlich die Audio-/UI-
Integration und erzeugt echte Zustandsbilder vor Export. Telefon-Lautstärke,
Bluetooth/Unterbrechungen und der subjektive Klangeindruck sind Geräteabnahme.
