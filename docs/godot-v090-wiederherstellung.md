# Vharok und bewohnte Gegnerreviere – Wiederherstellung 0.9.0

Der lokale Arbeitsstand wurde nach Unterbrechungen der Sitzung zweimal auf 0.8.0 zurückgesetzt. Dieser Commit sichert die vier bereits erzeugten und auf GitHub vorhandenen Bildblobs dauerhaft. Er ist ausdrücklich KEINE fertige APK und noch KEINE spielbare Integration.

## Gesicherte Bilder

Vier eigene gemalte Atlanten: Tangkrabbler (tide-crab.png), Harzsammler (resin-beetle.png), Schlackenträger (cinder-carrier.png), Vharok (vharok.png). Transparente Originaldateien; keine Platzhalter. Die bestehende Boss-Concept-Art liegt unter concept-art/vharok-demon-boss-v080.png.

## Zu rekonstruierender, zuvor lokal ausgeführter Umfang

- Tangkrabbler am POND/Küstensteg: sammelt Muscheln und Treibholz; 120 LP, geschlossener Panzer mindert Treffer um4, angekündigter Zangenschlag14 öffnet Panzer.
- Harzsammler am MILL_BROOK: transportiert gefallene Harztropfen für junge Bäume;130 LP, Heilung12 durch Brennen verhindert, Harzwurf10 bindet nur bei tatsächlichem LP-Schaden.
- Schlackenträger im Aschenstieg: Brennstofftransport zum Siegelhof;185 LP, Aufladung dann22, Wasser/Wanken unterbrechen.
- Vharok im Siegelhof:270 LP, drei Schutzsiegel mit je10 LP und Spott. Kern mit Siegeln25%, nach Siegelbruch150% für zwei Gegnerzüge. Danach ein neues Siegel. Ab90 LP dritte Phase: Vorbereitung, Sturm18 auf beide, erschöpfter Hieb8. Schutz und Unterbrechung wirken. Sieg360 XP und3 Mondsplitter, genau einmal.
- Alle bestehenden Regionalgegner bekommen ebenfalls konkrete Aufgaben, Revierwege und Arbeitsposen. Spielerannäherung stoppt sie; Bewegungsgeschwindigkeit begrenzen, keine Teleports. Fußanker, Kontaktshadow und laufend aktualisierte Kulissenmasken beibehalten. Nach abgeschlossener Begegnung ausblenden.
- Versorgungsroute MIST_WATCH → ASH_ASCENT → SEAL_COURT und zurück. Sie folgt dem Hang; keine Schluchtüberquerung und keine vorgetäuschte Brücke. Weiteres Dämonentor bleibt verschlossen. Atlas23 Gruppen/40 Orte.

## Zuschnitt der Originalatlanten

Reihenfolge kanonisch Ruhe/Vorbereitung/Angriff/Treffer/Fall/Arbeit/Gang.
Normale Gegner pose_map [0,2,3,4,5,2,1], Vharok [0,1,1,2,3,1,0].
Harzsammler Rohrechtecke: [[70,109,336,319],[609,103,347,322],[1159,95,303,331],[74,634,392,280],[640,596,304,323],[1136,674,336,232]].
Vharok: [[229,8,479,489],[863,13,526,486],[189,518,520,490],[885,632,503,374]].
Tangkrabbler: Alpha>30 in sechs Bereichen, oben x[0,520,1066,1536], unten x[0,594,1013,1536], y0/512/1024.
Schlackenträger: Spalten512px; obere/untere Trennzeile pro Spalte495/532/600, NICHT pauschal512; Alpha>30 für genaue Rechtecke.

## Zu beachten

Geerbte Superkräfte in forest_battle schreiben Schaden direkt: auch dort Rüstung/Kernfaktor angleichen und Ereigniswerte anpassen, sonst umgehen Starlet und andere die Bossregel. Brennschaden ebenfalls über Faktor führen. JSON-Zahlen im ecology_state beim Laden zu int wandeln. Siegel als sichtbare aktive Runen rendern, nicht als schlafende Kreaturen. Bossdarstellung250px, Fußpunkt etwa(1110,420), damit Krone nicht im Kopfbereich verschwindet; lange Namenszeile16px.

## Prüfstand und Auslieferung

Im zuletzt verlorenen lokalen Wiederherstellungsversuch bestanden32/32 vollständige Testkämpfe (vier Gegner × acht Helden).89 Dämonenregressionen bestanden.447 neue Prüfungen waren ausgeführt;57 ausschließlich aus Float32-Endpunktvergleichen mussten um eine Toleranz1e-5 ergänzt werden. Deren Wiederholung und weitere Rendering-/Regressionsergebnisse waren beim erneuten Reset nicht mehr verfügbar. Diese Zahlen gelten nicht als Nachweis für diesen reinen Bild-Sicherungscommit.

Letzte dauerhaft nachweisbare und ausgelieferte APK ist0.8.0/Code20, PR365, Commitd8a19b2adc124607b31a2bc262687364e80e5d7d, Drive148Ls55fl4-I8OUqRlMlFUSy7-PT75wLX.0.9.0/Code21 erst nach rekonstruierter Integration, echten Prüfungen und erfolgreichem bestehendem APK-/Drive-Workflow melden. Android-Geräteprüfung bleibt offen.
