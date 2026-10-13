# Itoeva v0.2.8 – Rundenkampf, Charakterposen und lebendige Effekte

## Ergebnis

Die erste Schattenwaldbegegnung hat eine eigene, gemalte Kampfansicht. Der gewählte Charakter und ein Begleiter kämpfen gegen Nachtwurz. Die Zugfolge steht oben; Aktionen warten auf die Entscheidung des Spielers. Es gibt Angriff, zwei eigene Fähigkeiten, eine aufladbare Spezialaktion, Verteidigen, Gegenstände und Rückzug. Treffer werden mit Körperposen, Licht, Flugbahnen, Schaum-/Windlinien und Schadenszahlen gezeigt. Lebensbalken ändern sich am Trefferzeitpunkt. Der Kampf bleibt bei Menüwechsel und Neustart erhalten.

Klassische Final-Fantasy-Rundenkämpfe sind die Referenz für Ablauf und Bedienung; Figuren, Fähigkeiten, Zeichnungen und Oberfläche sind eigene Itoeva-Entwürfe.

## Fähigkeiten

Alle normalen Spezialfähigkeiten kosten 6 MP, Unterstützungsfähigkeiten 8 MP. Verteidigen regeneriert 4 MP und reduziert den nächsten gegnerischen Schaden um 65 %. Normale Treffer geben 12 Herzpunkte, eingehende Treffer 24; die große Spezialaktion benötigt 100 Herzpunkte.

| Charakter | Fähigkeit (6 MP) | Unterstützung (8 MP) | Spezialaktion (100 Herz) |
|---|---|---|---|
| Fennec | Wildruf: 18 Schaden, bis zu 5 eigene LP; gemalte Rufpose und Tierflug | Fährtenlist: weicht dem nächsten Treffer aus; Doppelbild und Spuren | Waldchor: 42 Schaden, Gegner wankt |
| Starlet | Sternenlicht: 20 Lichtschaden, gegen Nachtwurz 30; Sternprojektil | Lichtschirm: schützt beide Gruppenmitglieder bis zu ihrem nächsten eigenen Zug | Nova: 42 Lichtschaden, Gegner wankt |
| Wyrmling | Glutfunken: 18 Schaden, Brennen für 2 Gegnerzüge; Atempose und Flammenbahn | Glutspur: 10 Schaden, Brennen für 3 Gegnerzüge | Sonnenatem: 42 Schaden, 3 Gegnerzüge Brennen, Gegner wankt |
| Gloop | Quellwelle: 18 Schaden, bis zu 6 LP für jedes lebende Gruppenmitglied; Wasserbahn und Tropfen | Tauquelle: bis zu 22 LP für jedes lebende Gruppenmitglied | Lebensquell: 42 Schaden, bis zu 24 LP für beide Mitglieder, Gegner wankt |
| Puffling | Windfeder: 18 Schaden; nächster Gegnerzug entfällt, Windbögen und flatternde Mantelpose | Rückenwind: nächster normaler Angriff beider Mitglieder +6 Schaden | Sturmkreis: 42 Schaden, Gegner wankt |
| Hootlet | Mondruf: 20 Lichtschaden, gegen Nachtwurz 30; 2 Gegnerzüge 35 % weniger Schaden, Flügelpose und Mondlicht | Dämmerschleier: 3 Gegnerzüge 35 % weniger Schaden | Mondfinsternis: 42 Lichtschaden, Gegner wankt |

Brennen verursacht 6 Schaden am Ende eines Gegnerzuges. Schutz/Täuschung/Rückenwind sowie Brennen, Wanken und Schwächung sind echte Modellzustände und werden gespeichert. Heiltränke (3 pro Versuch) heilen 36 LP und können ein erschöpftes Gruppenmitglied zurückholen; Äther (1) regeneriert 16 MP. Der erste Kampf stellt bewusst die Startgruppe; freie Gruppenzusammenstellung, Erfahrung/Level und eine große Gegnersammlung sind spätere Ausbauschritte.

## Charakterart

Die Konzepttafel zeigt Nachtwurz (Wald), Aschenwicht (Glut) und Schleierseher (Schatten). Nachtwurz ist der integrierte erste Gegner, die anderen beiden sind Designstudien. Nachtwurz besitzt 16 gezeichnete Posen: vier Ruhe-, vier Angriffs-, vier Zauber- und vier Treffer/Niederlagebilder.

Jeder der sechs Spielercharaktere besitzt einen eigenen 16-Bilder-Kampfatlas: vier Angriff-, vier Zauber-, zwei Schutz-, zwei Treffer- und vier Ruhe/Sieg/Niederlagebilder. Referenz waren die bestehenden Originalatlanten. Die Weltbewegung benutzt weiterhin ihre ursprünglichen Bilder; die neuen Kampfposen werden in der Kampfansicht verwendet. Die Bildausschnitte sind einzeln über Alpharänder vermessen, Standhöhe und Maßstab bleiben fest statt pro Bild die Körperhöhe neu zu normieren.

## Feuer und Wasser

Gemalter 16-Bilder-Flammenatlas mit 12 Bildern pro Sekunde und Überblendung. Die gemalten Flammenkerne bewegen sich lokal; der neue Flammenatlas ergänzt die Form statt eine rote Dreiecksfläche zu verwenden. Torchkörbe und Pfosten bleiben fest. Die neuen Phasen gelten für den Wald sowie Lagerfeuer/Kamin im bisherigen Weltteil. Beim ersten Kampf gehen Flammen und Licht aus; Restrauch bleibt kurz sichtbar.

Wasser hat gerichtete Strömung, unregelmäßige Reflexe und feine gebrochene Schaumlinien. Brückendeck, Geländer, Ufer und ausgesparte Felsen bewegen sich nicht. Fließshader und Wasserpolygone werden in Wald und bisheriger Welt verwendet. Die Variante bleibt eine stilisierte Animation der gemalten Kulisse, keine physikalische Wassersimulation.

## Bedienung und Speichern

- Touch und Maus verwenden dieselben nativen Aktionsbuttons.
- Eingaben sind während der Trefferfolge gesperrt; ein Doppeltipp löst nur einen Zug aus.
- Auch beim Wechsel vom Befehls- ins Fähigkeitenmenü gibt es eine kurze Eingabesperre gegen Durchtippen.
- Ergebnisse und MP/Gegenstandskosten werden vor der Animation atomar gespeichert. Nach Neustart wird nur die Animation wiederholt, kein zweiter Treffer berechnet.
- V0.2.7-Spielstände werden übernommen; bisherige Siege bleiben erhalten. Auf der Lichtung erscheint danach „Kampf wiederholen“.
- Niederlage: Wiederholen oder Rückkehr zum Waldsaum. Sieg: freie Bewegung in den tiefen Wald.

## Prüfung und Grenzen

Prüfergebnisse werden im beigefügten Prüfbericht festgehalten. Die Android-APK ist ein ARM64-Testbuild ab Android 7 / API 24. Paketkennung und Testzertifikat entsprechen v0.2.7. Der konkrete Telefon-, Leistungs- und Spielgefühltest steht noch aus. Die älteren Stühle/Bänke sind nicht Teil dieses Kampfumbaus.
