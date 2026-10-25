# Itoeva: freigegebene 3D-Modellvorlagen – 25.10.2026

Martin Zurek hat genau diese vier Bildentwuerfe im Chat ausdruecklich freigegeben.
Sie sind die verbindlichen Gestaltungsreferenzen fuer die Umsetzung der vier
spielbaren Charaktere. **Es sind visuelle Entwuerfe, noch keine implementierten
3D-Modelle und keine Spielrender.** Keine Ersatzdesigns erfinden.

Die vier Original-PNGs liegen direkt in diesem Ordner. Alle vier herunterladen und
visuell oeffnen; nicht nur diesen Text lesen. Dateigroessen und SHA-256 stehen
in [manifest.json](manifest.json).

## Verbindliche Auswahl

1. [Moosling – Bewegungsentwurf](Moosling_Bewegungsentwurf.png): oben ruhiges
   Gehen auf zwei Hinterbeinen, unten schnelles Laufen auf allen vieren.
2. [Starlight – korrigierte freigegebene Fassung](Starlight_3D_Entwurf_FREIGEGEBEN.png):
   Sternform bleibt beim Abstoessen und Landen erhalten; nur geringe Stauchung.
   Keine zusaetzlichen Beine/Fuesse. Abstoessen, laenger schweben, sanft landen.
   **Diese letzte Korrektur ist verbindlich; die zuvor stark abgeflachte Fassung
   ist verworfen und bewusst nicht enthalten.**
3. [Wyrmling – 3D-Entwurf](Wyrmling_3D_Entwurf.png): raeumlicher Vierfuesser,
   glaubwuerdige Gelenke und Gewichtsverlagerung, Schwanz/Fluegel dezent mitbewegen.
4. [Nocti – 3D-Entwurf](Nocti_3D_Entwurf.png): Gehen, Abheben und tiefer Flug;
   kompakte Originalproportionen, grosse Ohren und violette Fledermausfluegel.

## Umsetzung und erteilte Freigabe

Aussehen, Gesicht, Proportionen, Materialfarben, Fell/Schuppen, Kleidung und
Schmuck so nah wie moeglich an diesen Bildern halten. Moosling ist der Massstab.
Natuerliche Ganzkoerperbewegung, stabile Groesse und feste Standpfoten; kein
hektisches Beinkrabbeln oder gummiartige Verformungen. Schrittfrequenz an den
wirklichen Weltfortschritt koppeln, das bewaehrte Fortbewegungstempo erhalten.

Martin hat Modellierung, Texturen, Rigging, Animation, Codeaenderungen auf eigenem
Branch, Push, Entwurfs-PR und nach bestandenen Pruefungen eine neue signierte
Test-APK ueber den bestehenden Actions-/Drive-Weg freigegeben. Echte Modellrender
und kurze Bewegungsclips vor dem naechsten APK-Einbau zur Qualitaetskontrolle zeigen.
Die Bilderfreigabe erlaubt nicht, Konzeptbilder als fertig gebaute Modelle auszugeben.

## Technischer Einstieg

Repository: MartinZurek/Itoeva; Godot-Projekt `prototypes/godot-hd2d`.
Letzter hier bestaetigter Lieferstand: APK 0.15.5 / Code 35,
Commit `66dd18814c7fb6d157e6f581c4cbdc47391ec48d`,
Branch `codex/companions-original-volume`, PR #383.

Vor Beginn aktuelle Remote-Staende vergleichen und AGENTS.md lesen.
Bestehende Dokumentation: `docs/godot-companions-volume-a3.md`,
`docs/godot-mooslyn-projection-rig-a2.md`, `tools/mosslyn-3d/` und
`tools/companions-3d/`.

Die bisherigen Prototypen sind ansichtsgebundene Originalprojektions-Volumen,
keine fertig frei betrachtbaren Figuren. In 0.15.5 ist die 3D-Option standardmaessig
aus; Moosling faellt beim Sprint auf die alte zweibeinige Bildfolge zurueck.
Diese Grenzen beim Fortsetzen bewusst beheben und die neue Darstellung klar
zugaenglich machen. Keine neue APK allein zum Hochladen dieser Referenzbilder bauen.
