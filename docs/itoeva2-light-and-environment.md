# Itoeva 2: Licht, Schatten und Umgebungsbewegung

Stand 08.10.2026; Erweiterung von PR #346 auf dessen geprueftem Kopf
`a73c5568be9f07d5f67a64bfa6dcc814f50ae6fa`. Martin beauftragt die Reihenfolge
Landschaft/Massstab, Licht/Schatten, Figurenbeleuchtung und Umgebungsbewegung.
Merge und APK erstellt Martin anschliessend mit Cloud Code.

## Abgearbeitete Reihenfolge

1. **Landschaft und Massstab:** Die 17 Aussenorte, drei Zwischenorte und sechs
   gemalten Bildanschluesse sind in der Basis enthalten. Gemeinsame Fussprojektion,
   Charaktergroessen, alle sechs Spezies und alle Hin-/Rueckwege sind geprueft.
   Der Massstabs-/Moebelteil von #346 bleibt enthalten; siehe
   [Massstab](itoeva2-character-scale.md) und [Landschaft](itoeva2-continuous-world.md).
2. **Licht und Schatten:** GameLightingCatalog vermisst Fenster und Leuchtkoerper
   aller elf Innenbilder. Quellen besitzen Bildposition, Bodenanker und Montagehoehe.
   Sonnenrichtung wandert ueber den Tag. Aussenlicht verwendet absolute Weltanker;
   benachbarte Ortsnamen erzeugen weder doppelte Quellen noch einen neuen Lichttakt.
   Der Hoehleneingang filtert die Sonne raeumlich. Fenster erhellen tagsueber die
   Bodenflaeche; nachts werden ihre Scheiben kuehl abgedunkelt. Die schaltbare
   Wohnzimmerlampe erlischt tatsaechlich; andere gemalte Raumleuchten bleiben eigene
   Quellen. Feuer flackert warm, Kristalle leuchten kuehl.
3. **Figurenbeleuchtung:** Spieler und Bewohner verwenden dieselbe Berechnung aus
   Umgebung, Quellen, Abstand und vier Farbwerten fuer die Koerperseiten. Fell und
   Kleidung behalten ihre Farben und ihre Alpha-Silhouette. Die physische Lichtseite
   bleibt beim Umdrehen an derselben Bildseite. Licht wird nicht als Aura oder als
   undurchsichtige Flaeche ueber die Figur gelegt. Farbfilter im Stand werden gecacht.
4. **Umgebungsbewegung:** Kleine Bereiche der bestehenden Pflanzen-/Stoffmalerei
   werden zeilenweise um hoechstens rund 1.4 Weltpixel versetzt, mit festem Wurzel-
   beziehungsweise Aufhaengepunkt. Bildausschnitte an den Landschaftsnahtstellen
   stammen aus dem nachgemalten Anschluss; im Mischrand wird nichts uebermalt. Derselbe stetige Wind bewegt obere Spritebereiche;
   Fuesse bleiben fest. Die vorhandenen Sprite-Kleidungsdetails werden mitbewegt,
   es werden keine neuen Umhaenge oder Kostueme erfunden. Laubschatten bewegen sich
   dezent am Waldboden. Staub ist an wirksames Fenster-/Feuer-/Kristalllicht gebunden;
   Fensterstaub wird auf den Strahl begrenzt. Kuestenreflexe/Wellen lesen dieselbe
   Wasser-/Ufermaske wie die Figur. Dunst bleibt auf Teich, Feuchtgebiet, Bergpass
   und Hoehle beschraenkt. Regen verteilt sich ueber die gesamte Aussenwelt.

## Schatten und Verdeckung

Spieler und Bewohner haben einen weichen Kontaktschatten und bis zu drei nach
Lichtstaerke gewichtete gerichtete Wurfschatten. Abstand, Quellenhoehe und
Koerperhoehe beeinflussen die Projektion. Beim Springen bleibt der Empfaenger auf
dem Boden; der Schatten wird breiter und schwaecher. Bei einer stehenden Figur auf
Bank/Tisch liegt er auf der vermessenen Plattformoberkante.

Die bereits vermessenen Moebel projizieren zusaetzliche weiche Bodenschatten.
Ihre Konturen schuetzen Moebelfronten vor dem Uebermalen. Ein niedriger lokaler
Lichtstrahl hinter einem Tisch wird abgeschattet; ein hoher Strahl kann den Kopf
weiter erreichen. Die Verdeckung der Figur aus dem ersten #346-Schnitt bleibt
unveraendert. Keine neue Kollision, keine Aenderung an Inventar, Zeitfortschritt,
Raum-IDs, Spielstandschema, App-1-/Stream-Darstellung oder Signierungsinfrastruktur.

## Validierung und Vorschau

**970 lokale Kotlin-Tests gruen**, darunter 16 neue Licht-/Umgebungspruefungen:
alle elf Raumanker, Lampenschalter, gleiche Quellen ueber alle Ortskoordinaten,
alle Aussenraender mit allen sechs Spezies zu vier Tageszeiten, Hoehleneintritt,
warme/kuehle Farbseiten, Schattenrichtung, Sprung, Plattformkontakt,
Tischabschattung, raeumlich und zeitlich stetiger Wind, begrenzte Pflanzenbewegung,
Bildgrenzen samt korrektem Anschlussbild, Lichtstaub und ortsgebundener Dunst. Die bisherigen Massstabs-,
Bewegungs-, Kamera-, Speicher- und Verhaltenstests bleiben gruen.

- [Licht vorher/nachher](../tools/world-art/lighting-comparison.png): Wohnzimmer,
  Lager, Hoehle und Wald, gleicher Fussstandort und korrigierter Massstab auf
  beiden Seiten; links Lichtrechnung vor dieser Erweiterung, rechts neue Rechnung.
- [Alle sechs Figuren](../tools/world-art/lighting-family.png): derselbe Standort
  und dasselbe lokale Lagerfeuerlicht.
- [Bewegungsvorschau](../tools/world-art/lighting-environment.mp4): Tageszeit mit
  Sonnenwurf, Lampe an/aus samt Blickwechsel, Winddetails und Kuestenreflexe.

Dies sind Desktop-Darstellungen mit den echten Kotlin-Modellen und Bildassets,
keine Android-Aufnahmen. Die Java2D-Komposition approximiert die nativen
Canvas-Ebenen; die Bilder belegen keine GPU-/Telefonleistung. Android/Compose,
Lint, R8 und API-26-/35-Instrumentierung werden am letzten PR-Kopf separat
geprueft; der aktuelle Status steht im Pull Request.

### Vorschau reproduzieren

Nach `bash tools/reaction-preview/tests.sh` die alte Lichtrechnung fuer den
Vorher-Vergleich aus dem unveraenderten Basiskopf lesen:

```bash
git show a73c5568be9f07d5f67a64bfa6dcc814f50ae6fa:app-sim/src/main/java/com/notime/glyphsim/matrix/GameSceneLighting.kt > tools/reaction-preview/.work/OldGameSceneLighting.kt
sed -i 's/object GameSceneLighting/object OldGameSceneLighting/' tools/reaction-preview/.work/OldGameSceneLighting.kt
tools/reaction-preview/.work/kotlinc/bin/kotlinc tools/world-art/preview_lighting.kt \
  tools/reaction-preview/.work/OldGameSceneLighting.kt \
  -cp tools/reaction-preview/.work/tests -Xfriend-paths=tools/reaction-preview/.work/tests \
  -d tools/world-art/.work/lighting-classes
java -Djava.awt.headless=true \
  -cp tools/world-art/.work/lighting-classes:tools/reaction-preview/.work/tests:tools/reaction-preview/.work/kotlinc/lib/kotlin-stdlib.jar \
  com.notime.glyphsim.matrix.Preview_lightingKt . tools/world-art/.work/lighting
ffmpeg -y -threads 2 -filter_threads 1 -framerate 6 -i tools/world-art/.work/lighting/motion-%03d.png \
  -c:v libx264 -threads 2 -crf 22 -pix_fmt yuv420p tools/world-art/.work/lighting/lighting-environment.mp4
```

## Grenzen und Cloud-Code-Uebergabe

Es bleibt eine stilisierte 2D-Beleuchtung. Gemalte Schatten und Sonnenflecken sind
Teil der Originalillustrationen und werden nicht physikalisch neu gebacken.
Abschattung verwendet die bereits vermessenen Moebel; weitere Gegenstaende haben
noch keine vollstaendigen dynamischen Lichtblocker. Pflanzen bewegen sich lokal,
kein kompletter Baum-/Stoffsimulator. Keine neuen Hintergrundtexturen oder
zusatzlichen grossen Sprite-Boegen; Farbfilter und Bildausschnitte sind begrenzt.

Martin mergt selbst. Cloud Code prueft danach den tatsaechlichen main-Kopf, baut
ueber den vorhandenen APK-Weg und uebernimmt keine Workflow-/Keystore-Aenderungen.
Auf dem Telefon pruefen: Tageslicht/Nacht in Wohnzimmer und Wald; Lampe an/aus;
Feuer-/Kristalllicht beim Vorbeigehen und Umdrehen; Spieler und Bewohner zusammen;
Bank-/Tischlandung; Wind ohne wackelnde Fuesse; Ufer, Wasser und Ortsrueckweg;
Zoom/Antippen und Bildrate. PR #345 ist in #346 enthalten, kein separater neuer Fix.
Ruecknahme dieses Erweiterungscommits stellt die vorherige Lichtrechnung wieder
her und behaelt den ersten Massstabs-/Moebelschnitt.
