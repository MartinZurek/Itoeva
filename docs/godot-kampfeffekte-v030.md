# Itoeva Godot v0.3.0 – Wildruf, Lichtschirm und Wurzelbann

Die Fähigkeiten haben eigene Auflade-, Bewegungs-, Treffer- und Abklingphasen. Ihr Modell verbucht Schaden und MP wie bisher vor der Darstellung. Die Anzeige wechselt erst beim gespeicherten Trefferzeitpunkt. Aktionssperren richten sich nach der jeweiligen Animation.

| Fähigkeit | Darstellung und Wirkung |
|---|---|
| Wildruf | Gemaltes Tier erscheint, läuft/springt zum Gegner, greift an und zieht sich zurück. Katze 35 % / 12 Schaden, Wildschwein 30 % / 22, Wolf 25 % / 36, Gorilla 10 % / 48. Weiterhin 6 MP und bis zu 5 eigene LP Heilung. |
| Waldchor | Verstärkter Tierangriff: 42 Grundschaden plus halbe Tierstärke. Bestehender Herzverbrauch und Wanken. |
| Sternenlicht / Nova | Aufladegesten, breiter Lichtstrahl, Trefferstern und Partikel; Nova stärker inszeniert. Bestehender Lichtschaden und Lichtschwäche. |
| Lichtschirm | Zwei transparente Kuppeln mit Sternrunen, Rissen und Scherben beim Bruch. Je 40 Schutzpunkte für lebende Mitglieder, 8 MP. Schaden trifft zuerst den Schirm; überlaufender Schaden trifft LP. |
| Holzsplitter | Neun Holzprojektile und Splitter am Treffer; ein Ziel in geraden Gegnerzügen. Nebelwoge hat in jedem dritten Zug Vorrang. |
| Wurzelbann | Vorbereitungssiegel, ausbreitende Wurzeln und Ranken um Körper/Arme. Jeden vierten Gegnerzug: 12 Schaden pro lebendem Ziel, 1 eigener Zug gefesselt und 2 eigene Züge verstummt. |

Ein ungebrochener Lichtschirm hält auch Wurzelfesseln und Verstummen ab. Fesseln sperren Angriff und Magie; Verstummen sperrt Fähigkeiten, Unterstützung und Spezialaktion. **Fesseln lösen** entfernt beide Zustände und verbraucht einen Zug. Verteidigen, Gegenstände und Rückzug bleiben möglich. Verteidigen kann den Status auch ausklingen lassen. Kein dauerhafter Eingabestillstand.

Der Wildtierwurf geschieht genau einmal beim Aktionsbeginn. Tier, Schaden, Trefferzeitpunkt und Animationsdauer werden im Ereignis gespeichert. Der Zufallszustand wird als Dezimalstring gespeichert, damit JSON die 64-Bit-Zahl nicht rundet. Ein Neustart würfelt weder das laufende Tier noch seine Wirkung neu.

Vier Tiere mit je vier gezeichneten Posen im vorhandenen Gouache-/Bleistiftstil. Der transparente Atlas bleibt vollständig erhalten; wildlife_atlas.json beschreibt über Alphagrenzen vermessene Ausschnitte. Kein Tier wird durch nominale Rasterzellen abgeschnitten. Größe und Bodenkontakt bleiben über die Posen stabil.

Licht, Strahlen, Kuppeln, Splitter und Ranken sind native Godot-Canvas-Effekte. Vier Tierposen plus Bewegungskurven sind eine ausgearbeitete erste Sequenz, noch kein vollständiges Skelett oder AAA-Cutscene. Eigene Itoeva-Entwürfe mit klassischer JRPG-Kampfinszenierung als Richtung. Die Vorschau ist eine echte Desktop-Godot-Aufnahme, keine Android-Geräteaufnahme.

Schema 4 ergänzt Party-Mitglieder um barrier, bind und silence, das Ereignis um visual, duration, impact, summon und before_status und den Encounter um rng_state. Erfahrung, Beute und Siegesabschluss aus v0.2.9 bleiben erhalten. Alte Spielstände erhalten neutrale neue Statusfelder; alte laufende Aktionen verwenden ihr gespeichertes Ergebnis.

AndroidTest: 0.3.0 / Code 13, gleiche Paketkennung und Testsignatur wie v0.2.9. Direkt über die bestehende Test-App installieren. Kein Eingriff in native Room-Datenbanken, Reminder oder Agenten. Rücksetzung: v0.2.9-Quelle mit höherem Versionscode und gleichem Schlüssel bauen; vorher Spielstand sichern, da alte Versionen Zusatzfelder beim Speichern verwerfen können.

Die Tests prüfen Modell, Zufallsverteilung (1.000 feste Testwürfe), Neustart, Barrierenbruch, Überlaufschaden, Statusantworten, alte Spielstände, Touchmenüs, Sieg und Weltrückkehr. Alle vier Tiere, Strahl, Kuppel, Splitter und Wurzelbann werden zusätzlich mit echtem OpenGL gerendert. Ergebnisse stehen im Prüfbericht.

Geräteleistung, Touchgefühl und der zuvor gemeldete Android-Absturz nach dem Kampf bleiben am Telefon zu prüfen. Nächster Schritt: APK über die Test-App installieren, Wildruf mehrfach nutzen, Lichtschirm setzen, bis zum angekündigten Wurzelbann spielen, mit Fesseln lösen reagieren und bis zur freien Welt weitergehen.
