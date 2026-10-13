# Itoeva v0.5.0 – Spezialkräfte und Figuren in der Arena

Diese Fassung erweitert den Kartenkampf v0.4.0. Karten bleiben in der Hand;
gespielte Minions werden zu echten gemalten Figuren in der Arena. Sie stehen
vor den Gruppenmitgliedern, besitzen eigene Ruhe-, Angriffs- und Trefferposen
und bleiben bis zu ihrer Niederlage auf dem Spielfeld.

## Bedienung

Ein Minion antippen zeigt Name, Besitzer, Angriff, aktuelle/maximale
Verteidigungspunkte (LP) und Zustände. Ein eigener bereiter Minion wird zugleich
als Angreifer ausgewählt. Danach einen gegnerischen Minion oder Nachtwurz
antippen. Schutzhelfer blockieren direkte Minionangriffe auf die übrige Seite.
Zauber besitzen die bisherige freie Zielwahl. Beim Minionkampf tragen beide
Figuren gleichzeitig Schaden davon; ein Treffer gegen Nachtwurz löst keinen
Gegenschaden aus. Die LP sind die Verteidigungsreserve, kein zusätzliches
separates Rüstungsattribut.

Neue Helfer warten auf ihren nächsten eigenen Zug. Rudelruf macht das erste
beschworene Tier sofort bereit; Sturmfalken sind ebenfalls sofort bereit.
Pufflings Rückenwind macht eigene Helfer wieder bereit und kann dadurch einen
weiteren Angriff erlauben. Andere Angriffe erschöpfen den Helfer bis zum nächsten
eigenen Zug. Bis zu drei Helfer je Besitzer; Hauptfigur und Begleiter behalten
getrennte Decks, Hände, Kristalle und Züge.

Im Kampf öffnet „Karten“ eine blätterbare Detailansicht der Hand. Im Deckeditor
zeigt „Ansehen“ das Motiv und den vollständigen Effekt. Das Betrachten verbraucht
keine Karte und keine Kristalle; die Modalansicht sperrt Aktionen dahinter.
Kosten stehen blau oben links, Klassenfarben unterscheiden Kartentypen und
Spezialkarten besitzen einen stärkeren Rahmen. Decks: 8–12 Karten, normale Karte
höchstens zweimal, Spezialkarte höchstens einmal.

## Neue spielbare Wesen

**Mooslyn**, ein kleiner Waldhüter mit Zweiggeweih, Blattmantel und Eichelbrosche,
besitzt 86 LP. Rankenstoß verursacht 14 Schaden. Lebenshain heilt beide lebenden
Gruppenmitglieder um 12 LP und eigene Helfer um bis zu 6 LP. Eigene Karten:
Blattwicht (2/7), Mooswächter (4/14, Schutz), Hainruhe (+22 LP) und Urwaldherz.

**Nocti**, ein Fledermausgeist mit violettem Fell und Mondbrosche, besitzt 66 LP.
Echoruf verursacht 15 Schaden und zieht eine Karte. Schattenhaut gibt dem Wesen
und eigenen Helfern einmaliges Ausweichen. Eigene Karten: Echoflügel (4/4),
Nachtwache (4/11, Schutz), Echobiss (15 Schaden und +6 eigene LP) und Nachtspiegel.

Beide erscheinen im Auswahlmenü, als Bewohner in der Außenwelt, in Gesprächen
und im Kampf. Jeder Originalatlas enthält 32 Welt- und 16 Kampfposen; die Welt
nutzt die passenden Geh-, Lauf-, Blick-, Sprung-, Roll- und Sitzfolgen.
Die bisherigen sechs Wesen und ihre Originalbilder bleiben erhalten.

## Spezialkräfte und Spezialkarten

Eine bezahlte Karte, eine normale Superkraft oder ein eigener Helferangriff lädt
einen Punkt. Bei fünf Punkten ist die kostenlose Spezialkraft bereit, höchstens
einmal pro Kampf und pro Gruppenmitglied. Bindung/Verstummen sperren sie. Menü,
Speichern und Neustart erhalten Ladung und Nutzung; Wiederholen setzt beides
zurück. Die bisherige kostenlose Superkraft bleibt einmal pro eigenem Zug
verfügbar und nutzt einen eigenen Zähler.

Der gleiche besondere Effekt steht als bezahlte Spezialkarte zur Verfügung.
Diese ist unabhängig von der einmaligen kostenlosen Spezialkraft und darf
höchstens einmal im Deck liegen. Dadurch kann ihr Effekt im Kampf erneut
auftreten, wenn sowohl Spezialkarte als auch geladene Kraft eingesetzt werden.

| Wesen | Spezialkraft / Karte | Kartenkosten | Wirkung |
|---|---|---:|---|
| Fennec | Rudelruf | 4 | Bis zu zwei zufällige Waldtiere auf freie Plätze; das erste bereit. |
| Starlet | Sternenfall | 5 | 24 Schaden an Nachtwurz, 10 an feindlichen Helfern; Gruppe +16 Schutz. |
| Wyrmling | Phönixflamme | 5 | 30 Schaden an Nachtwurz, 12 an feindlichen Helfern; Phönix (6/8), falls Platz. |
| Gloop | Gezeitenwende | 4 | Gruppe +20 LP, eigene Helfer voll heilen, Gruppenfesseln und Verstummen lösen. |
| Puffling | Sturmfalken | 4 | Bis zu zwei Sturmfalken (5/5) auf freie Plätze; sofort bereit. |
| Hootlet | Mondfinsternis | 4 | 18 Schaden; nächste Waldattacke und nächste Angriffe aktueller Gegnerhelfer entfallen. |
| Mooslyn | Urwaldherz | 4 | Gruppe +20 Schutz; Urwaldhüter (4/16, Schutz), falls Platz. |
| Nocti | Nachtspiegel | 4 | 22 Schaden, Nachtwurz zwei Züge schwächen; Spiegelgeist (4/8, einmal ausweichen), falls Platz. |

## Darstellung und Speichern

Die neuen Helfer besitzen jeweils vier Zeichnungen: zwei Ruheposen, Angriff
und Trefferreaktion. Wildtiere verwenden ihre vier vorhandenen Körperposen;
Helfer der Originalwesen ihre Kampfzeichnungen. Angreifer bewegen sich zum
Ziel und kehren zurück. Ein fester Größenfaktor pro Figur verhindert eine
Normalisierung jeder einzelnen Pose; Körpergrößen ändern sich durch gezeichnete
Haltung statt durch Aufblasen. Die Regionen werden aus den transparenten
Rinnen gelesen, Originalpixel werden nicht verändert.

`card_schema=2` ergänzt Spezialladung, Nutzung, Helferform, Ausweichen und
Einfrieren. Frühere abgeschlossene Kämpfe und XP/Beute bleiben erhalten.
Frühere gültige Decks werden weiterverwendet; eine neue Spezialkarte wird dort
erst durch Deckbearbeitung aufgenommen. Der bekannte Siegesabschluss bleibt:
Trefferanimation, je 120 XP, Heiltrank/Mondsplitter, Charakterreaktion, Rückkehr
zur frei steuerbaren Welt.

## Abnahme und Grenzen

778 erfolgreiche Checks: 95 Kartenmodell, 15 Kartenregeln, 82 Spezialregeln,
69 Karten-UI, 96 Siegesablauf, 24 Minion-Touch und 397 visuelle Feld-/Kartenchecks.
Zusätzlich 22027 Waldchecks und acht gewonnene vollständige Kartenkämpfe.
53 gerenderte Ansichten decken alle acht Hände, Decks, Detailansichten,
Spezialkräfte und Arenasituationen sowie Menü, Welt und Gespräche der neuen
Wesen ab. Bildkontrolle korrigierte Felder, Kostenziffern, Namen und Abstände.
Die Filmvorschau zeigt gezielt vorbereitete Situationen im Desktop-Godot.

APK: Godot 4.5.1, AndroidTest 0.5.0 / Code 15, arm64-v8a,
`com.itoeva.godot.hd2d.worldtest`, bestehendes Testzertifikat, v2/v3 verifiziert.
Keine Installation oder Finger-/GPU-/FPS-Prüfung auf einem echten Telefon.
Balance, Kartenökonomie und Gegner-KI bleiben Prototypen. In einzelnen
Desktop-Testshutdowns besteht die bekannte ObjectDB-Aufräumwarnung.

Quellbranch: `feature/specials-v050`. Belege in `abnahme/v050`; Grafikprompts
in `prototypes/godot-hd2d/concept-art/specials-v050.md`. Die neuen Bitmapassets
liegen vollständig im Projekt; Importcache und privater Signierschlüssel sind
nicht Bestandteil des Quellarchivs.
