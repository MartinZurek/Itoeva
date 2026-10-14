# Itoeva Godot 0.6.0 – 40-Karten-Decks und individuelle Helfer

## Spielbarer Umfang

Jedes der acht Wesen besitzt ein vollständiges Deck mit **40 Karten**. Normale
Karten sind höchstens zweimal enthalten, die eigene Spezialkarte einmal.
Das Startdeck enthält 21 unterschiedliche Definitionen. Der gesamte Katalog
hat 89 Definitionen; jedes Wesen kann aus 30 passenden Karten wählen.
Der Gegner behält sein eigenes kompaktes Deck und das bisherige Kampfziel.

Der Deckeditor zeigt Kartenzahl, Helfer-/Zauberanteil und Kostenverteilung.
Filter: Alle, Helfer, Zauber, Spezial. Bei 39 Karten ist Speichern gesperrt;
„Auf 40 auffüllen“ ergänzt eine legale Auswahl. Entferne zuerst eine Karte,
bevor du im vollen Deck eine andere hinzufügst. Änderungen gelten im nächsten Kampf.

## Neue Interaktionen

| Werkzeug | Tatsächliche Regel |
|---|---|
| Helferverstärkung | Gezieltes eigenes Tier/Wesen erhält Angriff und zusätzliche maximale/aktuelle LP. |
| Schild | Absorbiert Schaden vor Helfer-LP; maximal 20 Punkte. |
| Tauverband/Flussverband | Heilt ein gewähltes eigenes Wesen auf dem Brett, teils mit Kartenziehen/Schild. |
| Frostfaden/Eisrohr/Mondpause | Nur gegnerischen Helfer treffen; dessen nächste Bereitschaft fällt aus. |
| Heimweg/Rückströmung/Nachtkehr | Eigene Helferkarte zurück auf die Hand; freie Superkraft-Helfer sind keine Karten. |
| Waldgedächtnis/Wiederentfachen | Die zuletzt gefallene eigene Helferkarte auf einen freien Brettplatz zurückholen. |
| Blättersalve/Glutregen/Querwind | Schaden an gegnerischen Helfern statt am Hauptgegner. |
| Rudel-/Hain-/Sternverbund | Mehrere eigene Helfer mit einer Karte stärken. |
| Frischer Saft | Kristalle auffüllen, ohne die Zugobergrenze zu überschreiten. |
| Lebensraub | Nur tatsächlich verursachten LP-Schaden heilen, kein Schildschaden oder Überschuss. |
| Regeneration | Helfer heilt sich zu Beginn seines eigenen Zuges. |
| Pilzarchivar/Blattkäfer | Eine Karte beim Erscheinen bzw. beim Fallen ziehen; maximal fünf Handkarten. |

Zielkarten zuerst antippen, dann eigenen oder gegnerischen Helfer wählen. Falsche
Ziele verbrauchen weder Karte noch Kristalle. Bereits ausgespielte Aktionen werden
vor ihrer Animation gespeichert; ein Neustart führt sie nicht erneut aus.

## Helfer und Kunst

16 neue individuelle Helferdesigns, jeweils sechs echte Zeichnungen:
Lichtmotte, Sternenschuppentier, Glutsalamander, Kohleschildkröte, Tau-Axolotl,
Schilfotter, Löwenzahngeist, Reiherwächter, Monddormaus, Mondwächter-Dachs,
Laternenschnecke, Blattkäfer, Pilzarchivar, Distelheilerin, Nebelgleiter und
Eichelhüter. Die bestehenden Waldtiere und acht Geistformen bleiben erhalten.

Die Kartenbezeichnungen Lichtfunke/Glutling usw. bleiben wiedererkennbar;
ihre Helferzeichnung verwendet nun eine eigene Identität statt eines verkleinerten
Hauptcharakters. Ruhe, Vorbereitung, Schlag, Reaktion und Rückkehr besitzen
unterschiedliche Körper-/Flügel-/Gliedmaßenposen. Bewegungsrhythmus und
Angriffsweg unterscheiden Flieger, Kriecher, schwere Wächter und Springende.
Die bisherigen Waldtiere zeigen in Ruhe keine Trefferpose mehr.

Originale Motive; Fantasy-Kartenspiele dienen als allgemeine Inspiration für
Synergien. Keine Figuren, Kartentexte oder Illustrationen daraus sind übernommen.
[Assetpfade und Produktionsprompts](../prototypes/godot-hd2d/concept-art/helpers-v060.md).

## Spielstände

Gespeicherte kleine Decks werden auf 40 ergänzt; vorhandene legale Karten bleiben.
Bereits laufende Kämpfe behalten Karten, Zug, LP, XP und Belohnungen. Neue 40er-
Decks werden beim nächsten Start/Wiederholen eingesetzt. `card_schema=3` speichert
zusätzlich Schild, Regeneration, Lebensraub, Herkunft und gefallene Helfer.

Karten bleiben zwischen Ziehstapel, Hand, Brett und Ablage erhalten. Rückruf und
Wiederbelebung erzeugen keine zusätzliche Kopie. In älteren gespeicherten Kämpfen
wird die bisherige doppelte Brett-/Ablagenzählung lebender Karten bereinigt.
Native Kotlin-/Room-/LAS-Daten und Reminder sind nicht betroffen.
Vor einem Rücksetzen auf 0.5.0 den Godot-Spielstand sichern: neue Karten werden
von dieser älteren Version nicht verstanden.

## Prüfung und Build

Lokal: 441 neue Modell-/Assetprüfungen, 8 neue Touchprüfungen, vorhandenes
Kartenmodell 95, Kartenregeln 15, Spezialregeln 82, Karten-UI 69, Minion-Touch 24;
acht vollständige Kartenkämpfe gewonnen. Echte Software-OpenGL-Aufnahmen von
Deckeditor, allen 16 Helfern und Angriff wurden visuell geprüft.

Die übrigen bestehenden Regressionen und der APK-Export laufen zusätzlich in
`.github/workflows/godot-apk.yml`. Der Workflow liest das neue Laufzeitmanifest
`docs/godot-v060-runtime-manifest.json` (214 Dateien), verwendet AndroidTest
0.6.0 / Code 16, Paket `com.itoeva.godot.hd2d.worldtest`, ARM64 und dasselbe
Secret `DEBUG_KEYSTORE_BASE64` wie 0.5.0. Drive wird per resumable Upload mit
vollständiger APK aktualisiert; die bisherige Datei-ID und Freigaben bleiben.
APK, Hashes, Paketdaten, Logs und Spielaufnahmen bleiben als Actions-Artefakte.

Telefonstart, Animationseindruck, GPU/FPS und längere Kämpfe auf Android sind
noch separat durch Martin zu prüfen. Der konkrete Actions-Status gehört zur
Auslieferung, nicht zu dieser vorab verfassten Anleitung.
