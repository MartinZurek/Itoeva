# Strategische Karten v1 – 18.10.2026

Auftrag: Karten neu bauen und nach Spielspaß und strategischer Tiefe optimieren.
Basis: Godot-Kampf `6677651` (PR #369). Umsetzung: PR #372, Branch
`codex/godot-strategic-cards-v110`. APK-Auftrag vom 18.10.2026: AndroidTest **0.11.0 / Code23** im bestehenden
signierten GitHub-/Drive-Lieferweg. Export und Auslieferung sind abgeschlossen: Lauf39169406271 bestand einschließlich Signatur und Remote-Prüfsummenabgleich.

## Spielbare Umsetzung

Alle **616 vorhandenen K-IDs** sind im Godot-Kampf und Deckeditor verfügbar.
Die erwähnten rund 620 Karten werden nicht durch erfundene zusätzliche Identitäten
aufgefüllt. Zwei beschworene Spielhelfer sind keine weiteren Sammelkarten.
Der ursprüngliche Faktorkatalog bleibt unverändert; `tools/card-forge/strategy_export.py`
übersetzt ihn deterministisch in `data/strategy_cards.json` (Schema 1, Balance 1).

- 13 Völker, 13 Volksfähigkeiten, 13 Gaben und sechs Merkmale mit tatsächlichen
  Kampfregeln. Alle Völker lassen sich mit allen acht Figuren spielen.
- Kosten 1–7 und Körperbudget auf die vorhandenen Helfer und Gegner abgestimmt.
  Faktorwerte liefern Identität und Körperverhältnis, keine automatische Balancegarantie.
- Glück wird zu **Kunststück**: Eine vorher gespielte Karte schaltet den Bonus frei.
  Flackernd gibt +1 beim ersten eigenen Angriff statt zufälliger Schadensschwankung.
- Drei Helferplätze pro Figur erzwingen die Wahl zwischen Angreifer, Schutz und Motor.
  Wachstum höchstens 30 Angriff/40 Leben; Lerntrigger endlich; Zins zusammen höchstens
  zwei zusätzliche Kristalle; Wiederkehr einmal erschöpft; Zuflucht einmal je aktivem Zug.
- Mythische Karten höchstens einmal pro Deck, andere zweimal. Seltenheit allein
  erhöht nicht die Körperwerte. Die Gaben kosten ebenfalls Körperbudget.
- Heimvolk +1 Leben. Mosslyn passt zusätzlich zu Kräuter-/Erntevolk, Nocti zu
  Artefakten/Irrlichtern. Affinität ist hilfreich, keine Zugangssperre.
- Jeder Deckvorschlag enthält 40 Karten: 26 Helfer, 14 bestehende Zauber, mindestens
  zwei Ein-Kristall-Helfer und zwei mythische Karten. Eine vorhandene Ein-Kristall-Karte
  wird bei Bedarf in die ersten drei Starthandkarten getauscht, ohne Karten zu erzeugen.

| Deckvorschlag | Plan | Entscheidung |
|---|---|---|
| Tempo | Früher Druck, Kunststück, sofortige Angriffe und Glut | Kristalle für eine zweite Karte im Zug aufheben; Schutzhelfer räumen oder Gegner treffen |
| Bollwerk | Schutz, Panzer, Heilung und Lebensaufbau | Angreifen oder für Eingraben stehen bleiben; knappen Brettplatz für Schutz oder Schaden nutzen |
| Motor | Lernen, Kartenziehen, Zins und Aufrüsten | Motor zuerst ausspielen, danach Auslöser; Handlimit und drei Plätze gegen langfristigen Ertrag abwägen |

Gaben besitzen bewusst konkrete Grenzen. Ohne Verbündeten kann etwa ein Baukunst-
oder Blutpakt-Helfer als Körper erscheinen; der nicht erfüllte Nebeneffekt entfällt.
Auren enden mit ihrem Geber und können einen 0/3-Erzsplitter angreifbar machen.
Laufende Zähler, übernommene Fähigkeiten, Wiederkehr und ausstehende Animationen
überstehen Speichern/Laden ohne erneute Auslösung.

## Bedienung und Bilder

Editor: Suche nach Name, Art, Rolle, Fähigkeit und Gabe; 13 Volksfilter, Kartentyp,
„Im Deck“, 18 Karten pro Seite, Kostenkurve und scrollbare vollständige Regeln.
Vorschläge ändern zunächst den Entwurf; erst „Deck speichern“ übernimmt ihn.
Bestehende Startdecks und gespeicherte Decks bleiben gültig und werden nicht ersetzt.

153 verschiedene Arten-/Formillustrationen versorgen die 616 Karten. Sie bestehen
aus reproduzierbaren SVG-Zeichnungen und drei geretteten gemalten Urformen.
Es sind keine 616 einzeln gemalten oder vollständig in Einzelposen animierten Figuren.
Die bestehenden Bewegungs-, Treffer-, Licht- und Schattenabläufe gelten auch für sie.
Beschwörungen erhalten Volksfarben und unterschiedliche mechanische/organische/
magische Motive; mythische Helfer einen 2,6-Sekunden-Auftritt mit zusätzlichem Halo.
Dies ist ein Echtzeiteffekt, kein neues Summoning-Video auf Final-Fantasy-Niveau.
Die drei früheren MP4-Entwürfe sind weiterhin im Recovery-Branch gesichert.

## Prüfung mit Godot 4.5.1

| Prüfung | Ergebnis |
|---|---:|
| `strategy_rules.gd` | 3.152 Checks, 0 Fehler |
| `strategy_ui.gd` headless | 770 Checks, 0 Fehler |
| `strategy_ui.gd -- --capture` mit echtem OpenGL | 771 Checks, 0 Fehler |
| Kartenregeln / Kartenmodell / Erweiterung / Spezialregeln | 15 / 95 / 441 / 82 Checks, jeweils 0 Fehler |
| Kampfklang und Lesbarkeit / Bewegung und Licht | 29 / 115 Checks, jeweils 0 Fehler |
| Ökologie und Boss | 1.089 Checks, 0 Fehler |
| Bisherige komplette Kartenkämpfe | 8/8 Siege |
| Bisherige komplette Ökologiekämpfe | 128/128 Siege |
| Neue Deckvergleiche | 180 vollständige Kämpfe, 0 blockierte Abläufe |

Jede der 616 Karten wurde ausgespielt, als JSON gespeichert und mit ausstehender
Animation wiederhergestellt. Zusätzliche Prüfungen decken Reihenfolge, Aufrüsten,
Zinsgrenzen, Aura-Ende, volles Brett, übernommene Fähigkeiten und Einmal-Wiederkehr ab.
UI-Prüfung umfasst Bilder aller Karten, Filter, Vorschläge, Speicherung, Regelansicht,
Verbündetenziele und Doppeltouch. Renderprüfung bei 1280×720 mit Mesa/OpenGL:
[Editor](images/strategy-deckeditor-v1.png),
[mythische Detailkarte](images/strategy-mythisch-detail-v1.png), Kampfaufstellung und
[Beschwörungsritual](images/strategy-mythisch-ritual-v1.png); die sichtbaren
Helfer werden zusätzlich im echten Zeichenlauf gezählt. Die Testkamera muss nach
angehaltenem Prozess ausdrücklich neu zeichnen; dies ist keine Kampfregeländerung.

Der deterministische Vergleich nutzt drei Figuren, fünf Gegner und drei Seeds mit
vier Decks; dieselbe einfache Steuerung spielt alle Stile. Rohdaten:
[validation/strategy-benchmark-v1.json](validation/strategy-benchmark-v1.json).

| Deck | Siege | Mittlere Runden | Spannweite |
|---|---:|---:|---:|
| Bisheriges Startdeck | 45/45 | 4,27 | 2–7 |
| Tempo | 45/45 | 4,07 | 2–6 |
| Bollwerk | 45/45 | 4,33 | 2–8 |
| Motor | 45/45 | 4,51 | 2–9 |

Die Gegner unterscheiden diese Decks unter dieser Steuerung nur schwach. Die Zahlen
belegen vollständige Abläufe und eine erste Geschwindigkeitsdifferenz, keine Rangliste,
keine PvP-Balance und keinen Nachweis menschlichen Spielspaßes. Menschliche Tests
sollten besonders die Drei-Plätze-Entscheidung und den späten Motorertrag prüfen.

## Grenzen und nächster Schritt

Keine Änderung an Erwerbswirtschaft, Bezahlung, Paketkennung, Signierschlüssel oder
Drive-Ziel. Der bestehende Bauworkflow erhält Version0.11.0, das neue Manifest und
die zusätzlichen Kartenprüfungen.
Der Testkatalog ist wie die bisherigen Karten frei zugänglich. Android-Gerät,
Daumenbedienung, GPU/FPS und Hörabnahme sind ungeprüft. Einzelne bestehende schnelle
Headless-UI-Läufe melden beim Beenden noch laufende Audioressourcen; der echte
OpenGL-Lauf bestand. APK-Bau und Remote-Prüfsummenabgleich sind abgeschlossen; als Nächstes menschliche Decktests.

Reproduktion im Godot-Projekt: `godot --headless --path prototypes/godot-hd2d
--script tests/strategy_rules.gd`; entsprechend `strategy_ui.gd` und
`strategy_benchmark.gd`. Für Bilder OpenGL statt headless und `-- --capture` verwenden.

## APK 0.11.0 – Bauauftrag

- AndroidTest0.11.0 / Code23; gleiches Testpaket und gleicher Zertifikatabdruck.
- Manifest mit488 Laufzeit-/Exportdateien, zusätzlich zur Integritätsprüfung der Quelle.
- Bisherige Regressionen plus Strategieregeln, UI,180 Deckkämpfe und echte
  Renderansichten laufen im Build erneut. Die fertige APK muss den Katalog
  mit616 Helferkarten aus13 Völkern tatsächlich enthalten.
- Vor dem Bau behobener Review-Befund: Zins-Kristalle oberhalb des Grundlimits
  bleiben bei Save/Resume erhalten, einschließlich bereits getätigter Ausgaben.
  Vier neue Checks erhöhen die Strategieregelprüfung auf3152/0. Alte Schemas
  behalten ihre bisherige Grenze; insgesamt bleiben höchstens zehn Kristalle.
- Paket, ARM64, Offline-Berechtigungen, Signatur und Drive-Größe/MD5/SHA-256
  werden wie bisher geprüft. Erst der erfolgreiche Baulauf belegt die Lieferung.

Der erste APK-Lauf39168282907 stoppte im alten `polish_v062.gd`: Nach140Pixel
Scrollen tippte dessen Knopfsuche den ersten gleichnamigen, inzwischen weggeclippten
Eintrag. Die Suche prüft jetzt Sichtbarkeit und alle schneidenden Elternflächen;
eine fehlende Schließen-Schaltfläche erzeugt einen Prüf-Befund statt Nil-Abbruch.
Der unveränderte Bedienvertrag (dreimal Öffnen/Schließen, Entwurf und Scrollposition
erhalten) besteht lokal weiterhin mit36/0. Der Build führt diese Prüfung und die
neuen Strategietests nun zuerst aus; sämtliche bisherigen Prüfungen bleiben erhalten.

## Auslieferung abgeschlossen

[Signierter Baulauf39169406271](https://github.com/MartinZurek/Itoeva/actions/runs/39169406271)
und GitHub Verify für den APK-Quellcommit sind erfolgreich. Alle27 Spielprüfungen
und die Renderprüfungen einschließlich771 Strategie-UI-Checks bestanden.

- Datei: `Itoeva_Godot_Test_v0.11.0.apk`, AndroidTest0.11.0 / Code23.
- APK-Quellcommit: `a4c199cf0f83ac82d7206736c49c00ae1c2e12bf`.
- Größe: 162059141Bytes.
- SHA-256: `883d961ae2dca070fd069eb86750032657d180cccd0cdccf4ad45953580bce70`.
- Bestehendes Testzertifikat: `30dbcf9daa7c26e4f76a1c2da674ee8b5e732186a058081e2dabd34eb82d58dc`.
- Die APK enthält nach der Exportprüfung tatsächlich616 Helferkarten aus13 Völkern.
- [Drive-Download](https://drive.google.com/file/d/148Ls55fl4-I8OUqRlMlFUSy7-PT75wLX/view?usp=drivesdk): bestehende Datei und Ordner beibehalten,
  Name und Größe nach dem Upload nochmals über die Drive-Anbindung bestätigt.
  Der Bauworkflow bestätigt zusätzlich MD5/SHA-256 gegen die hochgeladenen Bytes.
- [Maschinenlesbarer Liefernachweis](validation/godot-v0110-build-report.json).

Auf dem Telefon als Update von0.10.0 installierbar; menschliche Bedienungs-,
GPU-/FPS- und Hörabnahme bleiben offen. Neue Decks über Menü → Deck bauen →
Tempo/Bollwerk/Motor wählen und speichern; die Auswahl gilt für den nächsten Kampf.
