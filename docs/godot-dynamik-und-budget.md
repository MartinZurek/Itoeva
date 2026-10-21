# Dynamik, Ladeaufwand und vier Spielerfiguren

Stand:21.10.2026. Arbeitsbranch `codex/godot-focused-environment-budget` auf
`codex/godot-visible-route-connections`/PR375, ausgelieferte Testfassung0.13.1.

Martin möchte Spielerprüfungen nur für die vier tatsächlich auswählbaren Wesen,
ruhige Wasser-/Wolkenbewegung sowie weniger unsichtbaren Detail- und Ladeaufwand.

## Umsetzung – noch in Prüfung

- Umfangreiche Spieler-, Tür-, Bewegungs-, Framing- und Kampfmatrizen lesen die
  verbindliche Auswahl `Catalog.STARTERS`. NPC-Dialoge/Tagespläne und Helferidentität
  bleiben gezielt geprüft. Der vollständige Acht-Spezies-Katalog bleibt erhalten.
- Ebene Bildböden brauchen nur eine statt acht Tiefenreihen. Hintergrundreliefs
  behalten zwölf statt32 Reihen; Laufwege und Kollisionskonturen bleiben vollständig.
  Die Außenwelt hat75264 statt230784 Geometriepunkte (67,4% weniger).
- Wasserrauschen wird nur auf Wasser berechnet; entfernte Lichtkreise werden vor
  teuren Lichtfunktionen ausgeschlossen. Opaque Bilder verwenden keinen zusätzlichen
  Alpha-Vorlauf; transparente Bildanschlüsse behalten ihren bisherigen Renderpfad.
- Wasser und weich begrenzte Wolken teilen eine winzige vorberechnete RG-Maske.
  Keine pixelweise Konturrasterung bei Szenenwechseln. Die originalen gemalten
  Landschaftsbilder behalten ihre Auflösung und werden nicht verändert.
- Dezente Wolkenbewegung in ausgewählten Himmelsfenstern von Küste, Küstenpfad,
  Parkpanorama und Hochland. Zusätzliche Wasserfenster am Meer und Sonnenhain;
  Wege, Ufer, Holz und Felsen bleiben ausgespart. Bestehende Mühlen-/Schattenbäche
  behalten ihre Fließbewegung.
- Acht reine Kampfatlanten werden beim Start nicht dauerhaft vorgehalten. Sie
  bleiben verfügbar und werden bei Bedarf geladen.

## Evidenz und offener Stand

Vor dem Umgebungsreset gemessen (Godot4.5.1, OpenGL/Mesa llvmpipe,1280×720):
74 vorbereitete Texturen/103402448 Pixel, Bereitschaft6585ms, erster Weltaufbau2077ms;
Außenwelt230784 Punkte. Die erste Optimierung ergab66 Texturen/90817964 Pixel,
75264 Punkte und1401ms Weltaufbau. Renderzeit Küste157→89ms, Park232→151ms.
Dies sind Desktop-Einzelmessungen, keine Telefon-FPS. Die zweite Bereitschaft war
mit10156ms langsamer (neue Shader); Ladezeitgewinn deshalb noch nicht belegt.

Die lokale Messumgebung wurde bei einer Fortsetzung zurückgesetzt. Quelländerungen
sind aus dem dokumentierten Ablauf rekonstruiert; abschließende Messung, Prüfung
und visuelle Abnahme stehen noch aus. Eine neue APK wurde noch nicht ausgeliefert.

Masken neu erzeugen: `python3 tools/bake_environment_masks.py` im Godot-Projekt.
Abschließend Quellen-/Prüfsummen und Wasser-/Trockenanker prüfen. Nächster Schritt:
Vergleich von Bildqualität und Laufzeiten, fokussierte Regressionen, dann PR/APK.
