# Itoeva 2: spielbare Welt und weiterer Ausbau

Stand: 06.10.2026. Martin hat den Worldbuilding-Strategieplan zur Umsetzung freigegeben.
Dieser erste Implementierungsschnitt verbindet Straße, Park, Wald und Lager. Die im Plan
vorgesehene Geräteabnahme ist eine eigene offene Prüfung, keine behauptete erfolgreiche Messung.

## Was jetzt im Spiel vorhanden ist

- Ort, sichere Bodenposition, Blickrichtung, Rucksack, Ablage, Spielzeit, Lichtschalter,
  bedeutende Entdeckungen und bekannte Bewohner bilden einen versionierten Spielstand.
  App-Pause versetzt den Spieler nicht nach den alten Tagesablaufregeln.
- Samen und Holz im Wald sind einmalige Funde. Die Pflanzstelle im Park verbraucht einen
  Samenbestand und zeigt danach eine junge Pflanze. Ein Stück Holz verstärkt wahlweise
  die Parkbank oder den Sitz im Lager. Ein weiteres Stück liegt an der vorhandenen Werkbank.
  Verbrauch und sichtbare Weltfolge werden gemeinsam gespeichert.
- Ein voller alter Rucksack bleibt nutzbar: Gegenstand antippen, „In die Ablage“ wählen,
  später das Symbol in der Ablage zurückholen. Nichts wird dafür gelöscht.
- Die vorhandenen sechs Bewohner werden aus ihrer wirklichen Population dargestellt.
  Sie erhalten Bildkoordinaten, getrennte Tätigkeitsplätze, Laufwege und korrekte Tiefe
  gegenüber der Hauptfigur. Die bisherigen flüchtigen Besuchsfiguren werden im Game-Modus
  nicht parallel erzeugt. Ansprechen ist freiwillig; Schließen gibt die Steuerung sofort zurück.
- Gespräche erinnern sich an das tatsächliche erste Treffen und reagieren auf Pflanzen
  und Reparaturen. Das ist eine Bekanntschaft im Spielstand, noch keine beidseitige
  Beziehung oder Übergabe an die Agenten-Erinnerung.
- Ein alter Wegstein mit Blattzeichen bildet den ersten Hinweis zur Vergangenheit.
  Chronik und Karte helfen bei der Entdeckung. Die Chronik bewahrt die Reihenfolge echter
  Spielerhandlungen. Die neue Pflanze ist die dauerhafte Belohnung dieses kleinen Wegs.
- Licht, Regen, Musik und Atmo verwenden dieselbe gespeicherte Spielzeit und Wetterlage.
  Ein Tag dauert 48 aktive Spielminuten. Menüs, Gespräche und Hintergrund pausieren die Welt.
  Es gibt keine sichtbare Uhr und keinen Fortschritt während der Abwesenheit.
- Der erste strategische Baustein ist eine erkennbare Materialentscheidung: eine Holzressource,
  zwei Reparaturziele und sichtbare Folgen. Weitere Wirtschaft, Begleiter und Kampf folgen
  nach der Qualitätssicherung dieses Ausschnitts.

## Technische Entscheidungen

`GameAdventure` ist reine Kotlin-Logik für Zustand, Aktionen, sichere Anker und den Codec.
`GameSaveStore` speichert `itoeva2/world_snapshot` zusammen mit dem alten Rucksackschlüssel
in einem Commit. Fehlender Snapshot übernimmt den vorhandenen Rucksack. Unlesbare oder neuere
Snapshots werden geschützt; der Ausgang bleibt erreichbar. Speicherfehler werden angezeigt.
Einzelne Weltaktionen werden erst nach erfolgreichem Commit veröffentlicht. Checkpoints erfolgen
alle zwei Sekunden, beim Hintergrundwechsel und beim Verlassen der Composition. Ein harter
Prozessabbruch zwischen zwei Checkpoints kann die letzte Bewegung verlieren, aber keinen bereits
bestätigten Fund teilweise verbrauchen.

Die Kampagne bleibt wie der bisherige Rucksack gemeinsam für die wählbaren Wesen. Ein
Spezieswechsel wechselt die Spielfigur, nicht Welt oder Inventar. Eine spätere Gruppe braucht ein
eigenes ausdrücklich definiertes Besitzmodell. Room-Entities und Datenbankschemas bleiben gleich.

`GameResidents` projiziert die vorhandenen Population-Snapshots über `GameScenes` und
`GameSurfaces`. Es ersetzt weder den Living-Agent-Kern noch seine Entscheidungen. Die
Game-Population verwendet `itoeva2_population`; App 1 und Stream behalten ihren bisherigen Store
und ihre reale Zeit. Eine noch kommende soziale Erinnerung kann daran anschließen.

Objekt- und Stationswege verwenden dieselbe Wegsuche. Abbrechen führt keine Handlung aus;
20 Sekunden aktive Spielzeit begrenzen einen blockierten Weg. Menü und Hintergrund zählen
nicht gegen diese Frist. Das neue Textangebot liegt über der Touch-Steuerfläche.

## Status der freigegebenen Pakete

| Paket | Stand dieses Schnitts | Noch zu prüfen oder auszubauen |
|---|---|---|
| WB-01 Spielstand | Implementiert, Codec-/Wiederholungslogik getestet | Android-Lifecycle und harte Prozessbeendigung am Telefon |
| WB-02 Raumregeln | Gemeinsame Geometrie; neue Objektwege von beiden Seiten getestet | Zwei Bildschirmformate, Ufer/Möbel/Türen und Performance am Telefon |
| WB-03 Weltobjekte | Sechs stabile Objekt-IDs und dauerhafte Folgen | Visuelle Lesbarkeit aller Requisiten am Zielgerät |
| WB-04 Verwendung | Samen/ Holz verbrauchen einmalig; volle Altinventare können Platz schaffen | Touch-Ablauf und Speicherfehler am Telefon |
| WB-05 Bewohner | Wege, Tätigkeitsplätze und Tiefenordnung implementiert | Sitzhaltung, Möbelüberdeckung und Übergänge visuell abnehmen |
| WB-06 Begegnung | Freiwilliges Gespräch, Wiedersehen, Weltreaktion, Kartenweg | Übergabe/Gemeinschaftshandlung und beidseitige Erinnerung |
| WB-07 Umwelt | Gemeinsame Spielzeit; Außenregen und vorhandenes Licht/Klang | Windstärke, Fensterregen und wetterabhängige Bodenwirkung |
| WB-08 Entdeckung | Waldfund → Parkpflanze, Wegstein und geordnete Chronik | Spieldauer und Orientierung mit einer unvorbereiteten Person prüfen |
| WB-09 Innenräume | Eigenes Folgepaket, Bildarbeiten noch nicht ersetzt | Neun Studien und ihre Interaktions-/Tür-/Standflächenanker |
| WB-10 Strategie | Begrenztes Holz mit zwei vorher benannten Reparaturzielen | Zwei spielerisch unterschiedliche Lösungswege, Begleiter und Konsequenzen |

## Die nächsten Bildpakete

Die 16 vorhandenen Illustrationen bleiben Grundlage. Für die neun fehlenden Innenräume werden
passende Studien in derselben Seitenansicht benötigt; die verworfene Zentralperspektive wird
nicht zurückgebracht. Bild, Hitbox, Laufweg, Standfläche, Licht und Vordergrund sind gemeinsam
abzunehmen. `tools/world-art/places/*.py` bleibt die Quelle des generierten Katalogs.

1. **Werkstatt und Laden:** Werkbank/Holz sowie Regal/Samen sichtbar machen. Erstes Folgepaket
   für den Ressourcenkreislauf; Eingang und sämtliche Aktionsanker müssen erreichbar sein.
2. **Küche, Bad und Café:** Kühlschrank/Tisch, Wanne/Waschbecken und öffentlicher Treffpunkt.
   Regeneffekte liegen ausschließlich hinter den Fensterflächen.
3. **Schreibzimmer, Leseecke und Arbeitsstube:** unterscheidbare Tätigkeiten und ruhige Plätze,
   geteilte Möbelgeometrie statt Kopien mit anderen Namen.
4. **Spielhalle:** vorhandene Spielerhandlung und öffentlicher Begegnungsort; Licht und
   Kulisseneffekte müssen die Figur und Wege lesbar lassen.

Nach dem Gerätetest: beidseitige Bewohnererinnerung und eine kleine Übergabe bauen, anschließend
zwei wirklich unterschiedliche Erkundungswege mit derselben Ressource erproben. Erst dann einen
separaten taktischen Kampfprototyp mit höchstens zwei Verbündeten und zwei Gegnern starten.
Raster, Gruppe, Fähigkeiten und Kampfklassen sind mit diesem Schnitt nicht entschieden.

## Abnahme am Telefon

1. Alte Installation mit acht Büchern aktualisieren. Eines in die Ablage legen, Samen sammeln,
   pflanzen, neu starten, Buch zurückholen: Pflanze bleibt, Samen fehlt, alle Bücher bleiben.
2. Von Straße zu Park und Wald laufen. Jede neue Requisite von links und rechts erreichen.
   Auf Möbel springen, absteigen, Tür benutzen und nach App-Pause am selben Ort fortsetzen.
3. Hinweg zur Tür und zum Fund abbrechen: kein Teleport, kein Fund, kein Chronikeintrag.
   Während eines Wegs Menü mindestens 30 Sekunden öffnen: kein Fortschritt, danach fortsetzbar.
4. Zwei Bewohner am selben Ort beobachten. Füße und Größe passen; vordere Bewohner liegen
   vor der Figur, niemand wird zweimal dargestellt. Ansprechen, schließen, erneut ansprechen.
5. Holz zuerst im Park, in separatem Spielstand zuerst im Lager verwenden. Jeweils genau
   ein Verbrauch; anderes Ziel verlangt weiteres Holz und bleibt mit Werkbankholz erreichbar.
6. Über eine Spielphase hinaus spielen: Tag/Nacht und Regen sind in Licht, Bild und Ton
   konsistent. Innenräume werden nicht quer durchregnet. Musikwechsel hat keinen harten Neustart.
7. Mindestens zwei Querformate und eine längere Telefonsitzung prüfen. Bildrate, Speicher und
   Erwärmung messen; die Logiktests ersetzen diese Messung nicht.

## Ausgeführte Prüfung dieses Implementierungsstands

- `bash tools/reaction-preview/tests.sh`: 922 Tests erfolgreich.
- Sauberer Android-Build: `:app-sim:clean :app-sim:compileGameKotlin :app-sim:compileDebugKotlin`
  erfolgreich (Gradle 8.13, JDK 17, Android SDK 35; Kotlin-Inkrementalcache für diesen Lauf deaktiviert).
- Zweiter Agent: mehrfacher Code-Review, letzte Prüfung ohne blockierenden Befund.
- `git diff --check`: sauber. Geräteabnahme bleibt wie oben beschrieben offen.

Der Stand liegt im Branch `codex/worldbuilding-playable`, aufbauend auf dem gemergten
Living-Room-Stand `db7b320b573c1cd7ae5baabd1bf8345110590942`. Zielbranch des Pull Requests
ist `claude/world-concept-places`.
