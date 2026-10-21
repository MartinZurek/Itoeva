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
erneute CI-Pruefung und neue APK-Lieferung stehen noch aus.

Reproduktion der technischen Metadaten: `tools/prepare_gait_registration.py`
(Entwickler: NumPy, SciPy, Pillow, opencv-python-headless). Farbzeichnungen werden
nicht bearbeitet. Laufzeit und CI brauchen diese Entwicklerbibliotheken nicht.

Branch `codex/godot-calm-gait` auf dem ausgelieferten0.14.0-Stand/PR376.
AndroidTest0.14.1/Code28 nutzt den bisherigen signierten GitHub-/Drive-Weg.
Telefon-GPU/FPS und menschliche Bewegungsabnahme bleiben offen.
