# Gegnerreviere und Vharok – AndroidTest 0.9.0

## Spielerlebnis

Vier neue Gegner ergänzen die fünf bestehenden regionalen Arten. Jede Art besitzt eine Aufgabe, einen örtlich begrenzten Arbeitsweg und Arbeits-/Ruhephasen. Bei Annäherung hält sie an. Nach einem abgeschlossenen Sieg verschwindet sie aus dem aktuellen Profil. Ein Kampf beginnt bewusst über Beobachten und die Schaltfläche „Kartenkampf beginnen“, nicht durch zufällige Berührung. Rückzug, Wiederaufnahme und Neustart verwenden den bestehenden Speicherpfad.

| Art | Revier und Aufgabe | Eigene Antwort im Kampf |
| --- | --- | --- |
| Tangkrabbler | Strand: Muscheln/Treibholz am Spülsaum sammeln | Geschlossener Panzer reduziert Treffer um4; nach angekündigtem Zangenschlag14 öffnet er sich |
| Harzsammler | Mühlenbach: Harz für junge Bäume zusammentragen | Sammelt/heilt12, Brennen verhindert Heilung; Harzwurf10 bindet nur bei tatsächlichem LP-Schaden |
| Schlackenträger | Aschenstieg: Depotbrennstoff zu Grenzfeuern tragen | Lädt vor Schlag22 auf; Wasser oder Wanken löscht die Ladung |
| Vharok | Siegelhof: Grenztor und Schutzsiegel bewachen | Drei Siegel, verwundbarer Kern, angekündigter Aschensturm |

Dornmotten sammeln Pollen; Aschenläufer prüfen Grenzspuren; Rindenwächter versorgen Wurzeln; Glutwichte warten Signalfeuer; Siegelweber erneuern Warnsiegel. Diese Rollen werden im Atlas und in der Beobachten-Ansicht erklärt.

Die neue Route lautet Nebelwacht ↔ Aschenstieg ↔ Siegelhof. Sie verläuft am Hang auf derselben Schluchtseite. Das weitere Dämonentor bleibt versiegelt; Artefakt- und Maschinenwelt werden nicht vorgetäuscht. Der Atlas enthält23 Ortsgruppen/40 Orte plus das technische Waldpfadlabel.

## Boss und Balance

Vharok besitzt270 LP. Drei sichtbare Siegel haben je10 LP und Schutzfunktion. Solange sie bestehen, dringt25% Schaden zum Kern durch. Nach Siegelbruch erleidet der Kern150% für zwei ausgeführte Gegnerzüge, anschließend kehrt ein Siegel zurück. Ab90 LP wechselt Vharok zur finalen Sequenz: Warnung, Sturm42 auf beide, erschöpfter Hieb8. Heilung, Schwächen, Lichtschirm und Schutzhelfer bleiben wirksam. Nach Wanken sind zwei Gegnerzüge lang weitere Unterbrechungen blockiert; danach ist er wieder verwundbar für Wanken.

Die Standfestigkeit verhindert dauerhafte Unterbrechungsketten. Der Sturm wurde nach der Prüfung von18 auf42 erhöht: Ein voller Lichtschirm40 reduziert ihn weiterhin stark, deckt ihn aber nicht vollständig ab. Die Warnung gibt einen vollständigen Gruppenzyklus zur Vorbereitung. Der Boss bleibt ein früher Prototypboss; die Tests ersetzen keine menschliche Langzeitbalance.

Normale Regionalgegner vergeben je120 EP pro Gruppenmitglied, einen Mondsplitter und einen Heiltrank. Vharok vergibt360 EP je Gruppenmitglied, drei Mondsplitter und einen Heiltrank. Die Werte stehen vor Kampfbeginn sichtbar bereit; beanspruchte Beute wird beim Laden und wiederholten Bestätigen nicht dupliziert. Geerbte Heldenkräfte nutzen denselben Rüstungsfaktor vor dem Abzug der LP, einschließlich tödlicher Resttreffer.

## Bewegte Grafik, Licht und Klang

Die sechs bzw. vier gemalten Posen verwenden einzeln vermessene Alpha-Rechtecke. Weltfiguren behalten Fußanker, Kontaktschatten und dynamisch aktualisierte Kulissenmasken. Die begehbaren Bänder der zwei neuen Bilder sind anhand ihrer sichtbaren Straßen festgelegt. Die Depotfeuer und fünf Feuerschalen des Siegelhofs sind als lokale Lichtquellen im Bild verankert; sie wirken auch auf bewegte Figuren. Tag/Nacht wurde in OpenGL gerendert.

Vorbereitung erzeugt einen kleinen lokalen Ring, keinen erfundenen Treffer. Zange, Harzgeschoss, Schlacke, Ketten und Sturm besitzen unterschiedliche kurze Effekte. Ihre Ziele stammen aus dem tatsächlich aufgelösten Ereignis; fängt ein Helfer den Angriff ab, endet er optisch dort. Zehn Zeitpunkte pro Sequenz prüfen Anlauf, Wirkung und Abklingen. Trefferzahlen sitzen unterhalb der oberen Statusanzeigen; der Bossname bleibt in der Rundenanzeige kurz genug.

Vorbereitung und Sturmwarnung verwenden vorhandene dezente Kristall-/Mondklänge mit40% Effektlautstärke. Neue Angriffe nutzen75%; Schutz/Heilung erhalten passende vorhandene Klänge. Vorbereitungen ohne tatsächlichen Treffer spielen keinen Trefferklang. Der bestehende Stimmenpool, Doppelereignisschutz, Lautstärkeschalter, Musikübergang, Ducking sowie Kampf- und Siegthema bleiben erhalten. Keine neue Musikpipeline oder ungeprüfte Musikquelle.

## Prüfung und praktische Grenzen

- `ecology_boss_v090`:1057 Checks,0 Fehler: Reviere, Posen, Rüstung, Kernfenster, Sturm, Standfestigkeit, Konter, Beute, JSON-Spielstand, Neustart, echte Kampfeinstiege, verschwundener besiegter Boss, Bewegung ohne Teleport, lokale Lichtquellen.
- `ecology_playthrough_v090`:128/128 Siege mit acht Helden, vier Gegnern und vier Kartenreihenfolgen. Normale Gegner dauern3–5 Runden, Vharok5–9 Runden. Das ist eine feste sinnvolle Teststrategie, keine Aussage über alle Spielstile oder jede Kombination.
- Türenmatrix:256 Pfade mit allen acht Spezies bestanden. Bestehende Karten-, Spezialkraft-, Einwohner-, Tiefen-, Audio- und Rauchtests im APK-Workflow erneut.
- OpenGL-Aufnahmen: vier Reviere, Nachtlicht, Beobachten/Kampf, Aufladen/Angriff, Kernöffnung, Sturmwarnung/-wirkung, Sieg und Atlas. Linux/Mesa-Rendering, keine Behauptung über geprüfte Telefon-FPS, Adreno/Mali oder menschliche Hörabnahme.

## Art-Nachweis und Übergabe

Bilder und ihre Importdaten liegen in `prototypes/godot-hd2d/assets/expansion/`: `tide-crab.png`, `resin-beetle.png`, `cinder-carrier.png`, `vharok.png`, `ash-ascent.png`, `seal-court.png`. Vharoks ursprünglicher Entwurf bleibt in `concept-art/vharok-demon-boss-v080.png`. Gegneratlanten wurden aus dem vorigen Arbeitsstand gerettet. Die zwei neuen Landschaften wurden mit dem integrierten Imagegen erzeugt.

Promptset der Landschaften: detailreiche gemalte16:9-HD2D-Miniaturwelt, durchgehender freier Steinweg bei70–81% Bildhöhe; Hintergrundarchitektur darüber, Vordergrund unter86%; Aschenstieg mit Birken/Grenzturm/Schlackendepot am diesseitigen Schluchthang; Siegelhof mit geschlossenem Basalt-Bronzetor, drei leeren Ritualsockeln und kleinen Feuerschalen. Keine Figuren, Schrift oder Benutzeroberfläche. Wegband und Lichtquellen anschließend anhand des tatsächlichen Bildes vermessen.

AndroidTest0.9.0/Code21, bestehendes Paket `com.itoeva.godot.hd2d.worldtest`, ARM64. Das Runtime-Manifest dokumentiert den tatsächlichen Quellstand; der bestehende signierte APK-/Drive-Weg prüft Paketversion, Signatur, Dateigröße und Remote-Prüfsumme. Build-/Auslieferungsstatus ist im zugehörigen PR nachzulesen, nicht aus diesem Dokument als abgeschlossen abzuleiten.
