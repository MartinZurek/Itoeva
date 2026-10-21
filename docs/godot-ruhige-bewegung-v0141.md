# Ruhigere Bewegung und dezente Ausdauer – Testfassung0.14.1

Martin lehnt die Gehbewegung aus0.14.0 als zappelnd und schlechter als vorher ab.
Die bisher gruenen Konturtests belegten Vollstaendigkeit, keine natuerliche Bewegung.
Diese menschliche Rueckmeldung ersetzt jede fruehere Annahme visueller Abnahme.

## Korrektur

Die vier Spielfiguren behalten beim Gehen eine konsistente Kopf-/Koerperzeichnung.
Die vorhandenen gezeichneten Stuetzbeinphasen werden daran registriert; weiche
Uebergaenge bleiben unterhalb der Huefte. Wyrmlings Schweif bleibt ausserhalb des
Beinfensters ruhig. Ein kleiner stetiger Atemhub bewegt den Oberkoerper unter einem
Quellpixel; Gesicht, Schal und Proportionen wechseln nicht mehr achtmal pro Zyklus.

Vorbereitete kleine bidirektionale Bewegungsfelder verbinden die vorhandenen Phasen.
Android berechnet keinen optischen Fluss und erzeugt keine neuen Bilder: Der Shader
verwendet technische64×64-Felder je Phasenpaar zur Zwischenzeichnung der Gliedmassen.
Die sechs Felder werden bei Bedarf geladen, zusammen2,36MB unkomprimierte GPU-Daten.
Original-PNGs, Ruhe-/Sitz-/Sprung-/Kampfzeichnungen und Bewohner bleiben erhalten.

Ein voller Gehzyklus entspricht2,15 Welteinheiten, beim Joggen2,8. Die Kameragroesse
beschleunigt nicht mehr die Beine kleiner Wesen. Mooslyns zusaetzlicher Sprunghub
entfaellt. Noctis Kopf/Koerper bleiben beim Flug ruhig, die aeusseren Fluegel bewegen
sich mit1,8 statt3,2 Zyklen/Sekunde; die Hoehenschwankung ist halbiert.
Gehtempo, Sprinttempo, Ausdauerverbrauch, Erholung und Spielstaende bleiben erhalten.

Ausdauer:132×4 statt204×12 Bildpunkte, neutraler graubeiger Ton mit geringer Deckung,
kleine Beschriftung ohne wechselnde Prozentzahl. Bei voller Ausdauer blendet die
Anzeige aus, bei Verbrauch weich ein. Erschoepfung bleibt dezent lesbar.

## Stand der Pruefung

200 Ausdauer-/Anzeige-,869 Figuren-/Helfer-,64 Raum-/Spieler-,84 Vollwelt- und115
Bewegungs-/Lichtpruefungen lokal bestanden. Die technische Datenpruefung verifiziert
Quellbilder, Feld-Pruefsummen und zyklische Zielphasen ohne Bildbibliotheken.
Die aktive OpenGL-Pruefung betrachtet alle vier Spieler in drei Richtungen beim
Gehen/Sprinten, je48 Zeitpunkte. Sie prueft sichtbare Zwischenphasen und eine ruhige
Koerpermitte:2352 aktive OpenGL-Pruefungen bestanden. Die alte Konturmatrix prueft
unveraenderte Originalzeichnungen separat:1088 Checks bestanden. Echte Weltbilder
der neuen Ausdaueranzeige und aller vier Figuren aufgenommen. Fruehe Prueffehler
wegen einer zu grossen Kopf-Messflaeche im Flug wurden korrigiert. Die vollstaendige
erneute CI-Pruefung und APK-Lieferung sind inzwischen abgeschlossen, siehe unten.

Reproduktion der technischen Metadaten: `tools/prepare_gait_registration.py`
(Entwickler: NumPy, SciPy, Pillow, opencv-python-headless). Farbzeichnungen werden
nicht bearbeitet. Laufzeit und CI brauchen diese Entwicklerbibliotheken nicht.

Branch `codex/godot-calm-gait` auf dem ausgelieferten0.14.0-Stand/PR376.
AndroidTest0.14.1/Code28 nutzt den bisherigen signierten GitHub-/Drive-Weg.
Telefon-GPU/FPS und menschliche Bewegungsabnahme bleiben offen.

## Ausgeliefert am21.10.2026

[APK0.14.1 herunterladen](https://drive.google.com/file/d/148Ls55fl4-I8OUqRlMlFUSy7-PT75wLX/view?usp=drivesdk).
AndroidTest0.14.1/Code28 ist als Update zu0.14.0 installierbar; Paket und
Signierzertifikat bleiben gleich. Vollstaendiger Upload und Remote-Pruefsummen
stimmen mit dem lokalen APK ueberein.

- Quellcommit: `89612e3715d10312348e8e0ca2d97a5d22b2f7fc`.
- [Baulauf39633090907](https://github.com/MartinZurek/Itoeva/actions/runs/39633090907), Job119336217261 erfolgreich.
- 192796278 Bytes; SHA-256 `79cfd2840889746199b1550d198566e3a0136b1a87eebceed3a3b92181f059dd`.
- APK-Artefakt12359814658, Pruefprotokolle12360992759 (14 Tage).
- Liefernachweis: `docs/validation/godot-v0141-build-report.json`.
- Allgemeiner Verify-Lauf39633097207 inklusive API26/API35 und Gradle/Lint/R8 gruen.

Eine achtsekundige Vorschau aus tatsaechlichen24-Hz-OpenGL-Aufnahmen zeigt alle
vier Figuren beim Gehen und danach beim Joggen/Flug. Die dargestellten
Zwischenphasen sind dieselbe Laufzeit wie im APK. Technische Pruefungen und die
Bildfolge ersetzen weiterhin nicht Martins menschliche Bewegungsabnahme am Telefon.
