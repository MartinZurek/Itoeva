# Godot – Feuer und lokale Figurenbeleuchtung, Übergabe 14.10.2026

Martins Auftrag: vor der nächsten APK Feuer und Licht nach der visuellen Referenz
Final Fantasy Resonance verbessern, passend zur eigenen gemalten Welt.
Offizielle Referenz: https://blog.playstation.com/2026/09/03/final-fantasy-resonance-ps5-demo-available-today-sephiroth-revealed/

## Implementierte Darstellung

Verankerte Flammenspitzen, eigene Flackerrhythmen und vier lokale Glutfunken.
Dichter heller Kern und begrenzter weicher Lichtabfall erhalten Pinselstrukturen.
Alle 64 katalogisierten Quellen der Welt, Anschlussbilder und Innenräume sowie
Wald-Fackeln liefern Lichtanker für Figuren. Höchstens vier nahe Quellen
wirken gleichzeitig; die zugewandte Seite erhält mehr warmes Licht.
Ausgeschaltete Quellen werden vor der Auswahl entfernt. Arena-Fackeln verlieren
auch ihr Figurenlicht beim Erlöschen. Die Traglaterne besitzt Glas, vier
Messingstreben, offenen Bügel und einen animierten Docht.

## Belegte Prüfung vor dem Umgebungsausfall

Auf lokalem 40-Karten-Stand 2ca28b5 wurden in Godot 4.5.1 folgende Prüfungen
ausgeführt: ambience_render 9, forest_render 7, firelight_render 9 Checks grün.
Echte Aufnahmen von Lager, Wohnzimmer, Fackelpfad und Laterne an/aus visuell geprüft.
Regressionen: forest_trail 22.027, ambience_coordinates 225, gestures_paths 143,
expanded_cards 441, combat_spectacle 26, full_smoke 78 Checks, alle grün.

Danach wurden Anschlusslichtanker, Ausschluss abgeschalteter Quellen und
Laternenposition ergänzt; die abschließende GL-Wiederholung war beim Ausfall
noch nicht vollständig zurückgelesen. Die neuen Aufnahmen wurden geprüft.

## Tatsächlicher Veröffentlichungsstand

Die Ausführungsumgebung wurde während der Veröffentlichung unavailable.
Dieser Entwurf sichert den Lichtschritt über die GitHub-Verbindung auf Basis
e4983fe / codex/godot-card-arena-v050. Die Änderungen wurden aus den sichtbaren
Patchschritten und dem veröffentlichten Ausgangscode wiederhergestellt.
Der wiederhergestellte Remote-Commit konnte mangels Umgebung noch nicht erneut
ausgeführt werden. Keine Behauptung eines abschließend getesteten Remote-SHA.

Der lokale 40-Karten-/Helferstand 2ca28b5 wird durch diesen Entwurf nicht ersetzt.
Seine vollständige Veröffentlichung und die gemeinsame neue APK stehen aus.
Workflow und Manifest bleiben hier unverändert; dieser Branch startet keine APK.

## Konkrete Fortsetzung

1. Ausführungsumgebung wieder verbinden; lokalen Stand 2ca28b5 wiederfinden.
2. Nur diesen Lichtcommit darauf übernehmen. EVOLUTION/README zusammenführen.
3. Godot importieren, ambience_coordinates, forest_trail, gestures_paths,
   expanded_cards, combat_spectacle und full_smoke wiederholen.
4. Mit echtem OpenGL ambience_render, forest_render, firelight_render und
   firelight_capture ausführen; Spielaufnahmen prüfen.
5. Laufzeitmanifest mit shaders/firelight.gdshaderinc ergänzen (215 Dateien
   im gemeinsamen 0.6.0-Stand) und Hash im bestehenden APK-Workflow aktualisieren.
6. Gemeinsamen Branch veröffentlichen; dann AndroidTest 0.6.0/Code16 mit
   DEBUG_KEYSTORE_BASE64 bauen und resumable in Drive-Datei
   148Ls55fl4-I8OUqRlMlFUSy7-PT75wLX liefern. Lauf und Paketdaten prüfen.

Keine Licht-Spielstandmigration. Gemalte Kulissenschatten bleiben; keine
vollständige neue Möbel-Schattenphysik. Android-GPU/FPS und Telefonstart offen.
