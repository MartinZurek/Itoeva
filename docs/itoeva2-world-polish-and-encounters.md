# Weltkonturen, Sitzen und echte Begegnungen

Stand: 08.10.2026, `world-polish-encounters-v1`; Basis `be25d5d503c62007fdae71c653255ac3adb4a605` nach Merge von #346.

Martin hat die offenen Verbesserungen beauftragt. Dieses Paket erweitert die vorhandene
Game-Welt um Konturen, Sitzabläufe, Sonnenabschattung, Stoffdetails und gemeinsame Handlungen.
Merge und APK-Bau übernimmt Martin über Cloud Code.

## Umgesetzt

- Alle elf Innenbilder besitzen zusätzliche Konturen für ihre großen Schränke, Arbeitsflächen,
  Stühle und Ablagen. Kollisionshöhe, Figurenverdeckung und Lichtblocker lesen `GameFurniture`.
  Hohe Schränke sind nicht bespringbar. Kleine Dekorationen sind weiterhin Teil der Malerei.
- `GameSeating` führt Spieler und Bewohner zum verfügbaren Sitz, bewegt sie in 420 ms auf die
  Sitzfläche und in 360 ms zurück. Sitzplätze werden reserviert. Lesen/Schlafen behalten ihre
  Aktivitätsanimation in der Haltephase. NPC-Trefferbox und Tiefensortierung folgen der Renderpose.
- `GameWorldShadows` liefert 19 ortsfeste große Baum-/Gebäude-/Felsanker. Schattenrichtung und
  Figurenabschattung folgen derselben Sonne; Baumspitzen reagieren auf Wind.
- `GameGroundLight` dämpft beim einmaligen Laden großflächige gemalte Bodenbeleuchtung über ein
  kleines Helligkeitsfeld. Möbel, Wasser, Höhle und Hintergrund werden ausgespart. Das ist eine
  begrenzte 2D-Näherung, keine vollständige Trennung von Material und gebackenem Licht.
  Laden/Korrektur laufen auf `Dispatchers.Default`; ein Ladehinweis pausiert Zeit und Steuerung.
- `GameFabric` bewegt vorhandene Schals, Blatt-/Federdetails und Vorhänge mit fixierten
  Befestigungen. Inverses Sprite-Sampling vermeidet Lücken zwischen den bewegten Kacheln.
- Begegnungen erfordern tatsächliche Nähe. Eine Gegenstandsübergabe entfernt genau einen
  Gegenstand beim Spieler und legt ihn in den Bestand des Bewohners. Beide Agenten erinnern
  dieselbe typisierte Handlung. Volle Bestände, fehlende Gegenstände und stark müde/hungrige
  Bewohner lehnen den Transfer ohne Besitzänderung ab. Der Dialog weicht während der Geste.
- Holz hat eine weitere bewusste Verwendung: direkte Bankreparatur oder Übergabe an einen
  Parkbewohner und gemeinsames Bauen sichtbarer Wegweiser im Park und Wald. Kein Material
  entsteht aus Wiederholen; kein XP-Bonus und kein Beziehungsabzug bei Ablehnung.

## Speicherung und Grenzen

`GameAdventure` liest V1 und schreibt V2 mit zwei zusätzlichen Zeilen: begrenzte, validierte
Agenten-Snapshots und Bewohnerbestände. V1-Inventar, Ablage, Ereignisse, Schalter und bekannte
Bewohner bleiben erhalten. Die alte weltweite Bekanntschaft wird als neutrale Relation für
die sechs Spielerspezies übernommen, ohne Episoden, Vertrauen oder Nähe zu erfinden.

Spielerbesitz, Bewohnerbesitz und beide Erinnerungen liegen in **einem** `GameSaveStore`-Commit.
Erst bei Erfolg veröffentlicht die UI den neuen Stand. Der soziale Bewohneranteil wird idempotent
in die vorhandene Population eingeblendet; deren Bedürfnisse, autonome Entscheidungen,
Wirtschaft und Uhr werden dabei nicht ersetzt. `GAME_*` sind Abschlusswirkungen tatsächlicher
Spielerhandlungen und keine neuen autonomen Kandidaten. Die Game-Host-Profile sind fiktionale
Wesenprofile; kein persönliches Nutzermodell und keine neue Gesprächs-KI.

V2-Spielstände sind für ältere APKs nicht lesbar. Ein reines Revert auf V1 ist deshalb **kein**
verlustfreier Rücksetzweg. Für ein visuelles Revert müssen V2-Reader und neue Enumwerte bleiben;
Sitzen/Schatten/Stoff/Begegnungs-UI können separat zurückgenommen werden. Ein älterer Reader
bewahrt den unbekannten Snapshot und verweigert Überschreiben. Room und Workflow bleiben gleich.

## Nachweise

- `bash tools/reaction-preview/tests.sh`: **989 Tests bestanden**. Neu: Sitzkontinuität,
  belegte Sitze, tatsächliche Trefferposition, alle Innenraumwege, Stoffbefestigungen,
  Schattenanker, begrenzte Bodenkorrektur, reale beidseitige Erinnerung, Transfer/Repetition,
  Neustart, volle Bestände, Holzalternativen und V1-Migration.
- `GameEncounterSaveTest`: drei zusätzliche Android-Tests für echten Preference-Neustart,
  verweigerten Editor-Commit und Schutz unbekannter Versionen. Ausführung erfolgt in Verify/API 26.
  Der simuliert verweigerte Editor beweist keine Rücknahme eines echten Android-Plattenfehlers:
  SharedPreferences kann seinen Prozessspeicher bereits vor einem fehlgeschlagenen Disk-Commit ändern.
- Zweite Codeprüfung fand Sitzanimation/Trefferfläche/V1-Kenntnis; die drei Befunde wurden
  korrigiert und mit Regressionstests versehen.
- `bash tools/world-art/preview_polish.sh` erzeugt die drei PNGs und `polish-motion.mp4`
  aus denselben Modellen/Assets. Das sind Desktop-Vorschauen, keine Android-Aufnahmen.
  Die einmalige Bodenkorrektur der 24 Assets dauerte hier rund 1,8 s; keine Telefonmessung.

## Cloud-Code-Abnahme

1. PR-Kopf und vollständigen Verify-Lauf prüfen; dann APK aus genau diesem Kopf bauen.
2. Alten Spielstand behalten: bekannte Bewohner, Rucksack/Ablage, Schalter und Ereignisse prüfen.
3. Mit allen sechs Figuren Sofa, Café, Küche, Bett und Nische: Hinsetzen, Aktivität, Aufstehen,
   antippbaren Oberkörper und Verdeckung prüfen; kein doppelter Sitz mit NPC.
4. Morgen/Abend an Bäumen, Haus und Außennaht: feste Schattenfüße, geänderte Richtung, ruhige
   Farbseiten; Fenster/Möbelfronten dürfen nicht durch eine Vollbildkorrektur verändert werden.
5. Einen Gegenstand übergeben, App schließen/neustarten: einmaliger Besitz und beide Erinnerungen.
   Acht Gegenstände beim Bewohner; weitere Übergabe darf den Spielerbesitz nicht verändern.
6. Holz einmal selbst zur Bankreparatur und in getrenntem Stand gemeinsam zu Wegweisern verwenden.
7. Ladedauer, Speicher, Bildrate mit mehreren NPCs, Vorhänge und Stoffbewegung auf Telefon prüfen.

## Nächster ausdrücklich beauftragter Schnitt

Martin meldet am 08.10. zusätzlich stehende Wasserflächen, unlesbares Schwimmen und fehlende
physikalische Weltreaktionen. Nach Abschluss dieses Pakets folgt ein systematischer Scan von
Wasser/Schwimmen, Türen, sichtbaren Hindernissen, begehbaren Bildflächen, Sprung-/Weltgrenzen und
sinnvollen Kontaktreaktionen. Diese Punkte sind durch die oben genannten Vorschauen **nicht**
abgenommen. Außenbänke außerhalb des Parks haben noch generische Geometrie; kleine Dekorationen
und vollständig dynamisches Kulissenlicht bleiben offen. Keine Begleiter- oder Kampflogik.
