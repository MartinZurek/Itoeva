# Itoeva 2 – zusammenhängende gemalte Welt

Stand 08.10.2026. Dieser Schnitt führt die native Kotlin-/Compose-Welt fort.

Vom Wohnzimmer führt der seitliche Ausgang zur Straße. Rechts folgen Park, Wiese,
Wald, Dorfrand mit Obstgarten, Marktplatz, Sportplatz, Ebene, Berge, Bergpass, Lager und Höhle. Links folgen Küstenweg, Dschungel,
Feuchtgebiet, Strand und Teich. Die 17 Außenorte liegen auf einer gemeinsamen,
13440 × 640 großen Fläche mit derselben Bodenprojektion und Kamera. Die sieben gemalten
Panoramen bleiben gemeinsam geladen; Ortsgrenzen lösen keine Bildladung, Blende oder
neue Laufphase aus. Jeder Abschnitt hat denselben Hin- und Rückweg.

Vorwärts/rückwärts bewegt die Figur in der Raumtiefe. Diese Richtungen sind keine
Ausgänge in andere Außenorte. Die elf Innenräume werden über beschriftete Türen,
Doppeltipp oder die Aktionstaste betreten. Ihre Gegentüren führen an denselben
Außenort beziehungsweise in denselben Raum zurück. Das Haus verbindet Wohnzimmer,
Schlafzimmer, Bad, Arbeitszimmer, Lesezimmer, Küche und Werkraum. Laden, Café,
Arbeitsplatz und Spielhalle liegen an ihren Außenorten.

## Darstellung und Bewegung

Alle aktiven Orte verwenden die gemalten Bilder. Das ursprüngliche große Wohnzimmer-
und Schlafzimmerbild bleiben erhalten. Die alten kleinen Szenenbilder samt Animationen,
Materialrastern und Teilatlanten sind aus der Game-Variante entfernt. Der frühere
`GameScenes`-Katalog bleibt als kompatible Geometriemetadaten bestehen; die aktive
Spielwelt benutzt ausschließlich `GameWorld.scene`.

Der begehbare Außenboden reicht von Bildhöhe 450 bis 625. Am Strand ist die hintere
Sandfläche trocken; weiter vorn geht die Figur ins Wasser. Materialkontakte,
verdeckte Beine und eine Wasserlinie teilen dieselbe Ufergeometrie. Die Kamera nähert
sich beim Eintauchen sanft und verschiebt den Ausschnitt zum Wasser. Schnelleres
Laufen öffnet weiterhin die Ansicht. Große Innenbilder werden vollständig skaliert.
Im Wohnzimmer verdecken die Originalkonturen von Sofa und Tisch die Figur dahinter.
Licht folgt der Tageszeit; Lagerfeuer und Höhle haben eigene Lichtquellen.

Die bestehende `GameMovement`-Physik bleibt die einzige Bewegungsquelle. Breitere
Abschnitte normalisieren die horizontale Geh-/Rollstrecke. Orts-IDs, Spielstandcodec,
Inventar, Bewohneridentitäten und Datenbankschema bleiben erhalten. Karte und
Routenvorschau lesen dieselben tatsächlichen Durchgänge wie die Steuerung.

## Prüfung und offene Abnahme

Am 08.10.2026 bestehen lokal **944 Kotlin-Tests**. Die Teststrecke prüft alle 28 Orte mit Hin- und Rückwegen, identische Fußpunkte und
Figurgrößen an sämtlichen Außenrändern, Erhalt von Tempo und Laufphase, Kamera und
Touchprojektion, Tür-Gegenstellen, Materialkontakte und Wasserzoom. Der letzte
vollständige Stand wird im Pull Request festgehalten; Android-Kompilierung und
Geräteprüfung sind zusätzliche, getrennte Prüfungen.

`tools/world-art/preview_geography.kt` zeigt alle 28 Orte. `preview_walk.kt` rendert
mit der echten Kotlin-Physik Hin- und Rückbewegung über die sechs Panoramagrenzen und
vom Strand ins Wasser. Die Vorschau ist eine Desktop-Darstellung, keine APK-Aufnahme.

Die drei bislang abrupten Landschaftssprünge haben eigene Zwischenorte: Küstenweg mit
Bachbrücke, Dorfrand mit Obstgarten und Bergpass zum geschützten Lager. Die Bilder
übernehmen Vegetation, Horizont, Licht und Wegmaterial ihrer jeweiligen Nachbarn.
Sechs nachgemalte Anschlussbilder (`world/seams/0.png` bis `5.png`) verbinden die
beiden Landschaften auf jeweils 960 Weltpixeln. Die äußeren 128 Weltpixel laufen
weich in die Basisbilder aus. Alle Bilder bleiben an ihrer Weltposition verankert;
Türen, Fußanker und Kamera verwenden weiter dieselbe Geometrie.
Das Bodenprofil des Küstenwegs verengt und hebt die Gehfläche über dem Brückenbogen;
Touchprojektion und Füße verwenden dieselbe Geometrie. Die sechs Anschlüsse werden
in der Desktop-Bewegungsvorschau geprüft. Weitere Möbelsilhouetten, präzise Anker aller
kleinen Innenräume und die Touch-/Speicher-/GPU-Abnahme am Telefon stehen aus.
Die durchgehende Bewegung ist bereits unabhängig von diesen Bildnähten organisiert.
Gebäudeeintritte behalten einen bewussten Raumwechsel mit Überblendung.

Reproduzierbar nach `bash tools/reaction-preview/tests.sh`:

```bash
TASK_CACHE=tools/reaction-preview/.work
mkdir -p "$TASK_CACHE/world-preview/classes" "$TASK_CACHE/world-preview/frames"
"$TASK_CACHE/kotlinc/bin/kotlinc" tools/world-art/preview_walk.kt -cp "$TASK_CACHE/tests" -Xfriend-paths="$TASK_CACHE/tests" -d "$TASK_CACHE/world-preview/classes"
java -Djava.awt.headless=true -cp "$TASK_CACHE/world-preview/classes:$TASK_CACHE/tests:$TASK_CACHE/kotlinc/lib/kotlin-stdlib.jar" com.notime.glyphsim.matrix.Preview_walkKt "$PWD" "$TASK_CACHE/world-preview/frames"
ffmpeg -y -framerate 15 -i "$TASK_CACHE/world-preview/frames/frame-%04d.png" -c:v libx264 -crf 23 -pix_fmt yuv420p -movflags +faststart tools/world-art/coherent-world-preview.mp4
```

Rücksetzung: den zusammenhängenden Weltcommit zurücknehmen. Die vorherigen Bilder
werden dabei aus Git wiederhergestellt. Eine Datenmigration ist nicht erforderlich;
alte Clients prüfen gespeicherte Positionen gegen ihre eigene Geometrie.
