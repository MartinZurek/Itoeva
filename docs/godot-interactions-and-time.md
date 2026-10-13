# Godot v0.2.6: Entdecken und Begegnen

Stand: 2026-10-11. Die vorhandenen 28 Orte, 22 gerichteten Tueren und sechs
gezeichneten Wesen bilden weiter dieselbe gemalte 3D-Buehne. Das neue System
verbindet freie Daumenbewegung mit gezieltem Antippen und sichtbaren Aktionen.

## Verhalten

- Ein Tipp auf Figur/Sprechblase, Eingang, Fund oder Moebel geht zum Ziel und
  fuehrt anschliessend dessen Aktion aus. Manuelle Bewegung oder eine Geste
  bricht den Weg ab. Der Zielname bleibt festgelegt; niemand wird versehentlich
  durch eine andere nahe Interaktion ersetzt.
- Die Wegsuche verwendet exakt dieselben trockenen Grenzen und Moebelkollisionen
  wie die Physik. Schmale Stege erhalten eine zusaetzliche Mittellinie mit
  0,10 Einheiten Abtastung; die begehbare Flaeche wird nicht erweitert.
- Gespraeche zeigen Originalportraet und Originalleitsatz plus eine neue kurze,
  zur Erkundung passende Antwort. Weiter/Schliessen funktionieren mit echten
  ScreenTouch-Ereignissen, Maus und Tastatur/Gamepad-Menueabbruch.
- Alle vorhandenen Sitze haben groessere Tippflaechen und einen freien
  Annaeherungsanker. Die sitzende Figur wird vom eigenen Moebel nicht verdeckt;
  andere Vordergrundobjekte behalten ihre Tiefenverdeckung. Aufstehen erfolgt
  am freien Ausgangsanker. Im belegten Sitz liegt kein Symbol ueber dem Gesicht.
- Parkbewohner patrouillieren mit Wendepausen und weichen seitlich aus.
  Figuren kollidieren physikalisch mit Welt/Moebeln, Nachbarn steuern weich vorbei.
  Ein abgebrochener Gespraechsweg laesst den Nachbarn wieder weitergehen.
- Berliner Uhrzeit basiert auf UTC plus EU-Sommerzeitregel. Ein Weltbesuch bzw.
  App-Fokus setzt auf Echtzeit; Tuerwechsel erhalten den Sitzungsoffset.
  Lesen bestaetigt +2 Stunden, Schlafen den naechsten lokalen Morgen um 08:00.
  In Einstellungen ist die echte Ortszeit wiederherstellbar.
- Gemalte Kulissen wechseln kontinuierlich zwischen Nacht, warmem Morgen,
  Tag und warmem Abend. 24 Bilder enthalten 64 vermessene Lichtquellen und
  acht begrenzte Wasserpolygone. Lampen flackern subtil mit eigenen Phasen,
  das Lagerfeuer staerker. Wasser verschiebt sich unter einem Quellpixel;
  Holzstege, Himmel, Pfade und Figuren werden nicht verzogen.
- Vordergrundmeer der ersten Kuestenkulisse wird ab UV .75 bis .78 durch ein
  neues trockenes Ufer ersetzt. Oberhalb .75 bleiben die Originalpixel erhalten,
  das Hintergrundmeer bleibt sichtbar und bewegt sich. Originaldatei unveraendert.

## Validierung

| Pruefung | Checks | Fehler |
|---|---:|---:|
| Bewegung, Spielablauf, Tuer-/Raummatrix, Proportionen, Gesten, Grenzen, Ufer, Geometrie (9 bestehende Suiten) | 3807 | 0 |
| Berliner Uhr inkl. Sommerzeitgrenzen, Lesen, Schlafen | 71 | 0 |
| Interaktionen / freie Annaeherung / Moebel / Zeit | 58 | 0 |
| Android-Touch, 22 ferne Tuerwege, sechs Sitzfiguren, Abbrueche | 122 | 0 |
| Spaziergaenger / Gegenverkehr / schmale Bruecke | 21 | 0 |
| Fuenf echte Brueckenstrecken, Boden und Kapsel je Physikschritt | 4905 | 0 |
| Entfernte Figurentipps ueber drei Bruecken bis Gespraech | 15 | 0 |
| Lichtkoordinaten / Wassermasken / Tagesfarben | 225 | 0 |
| Echte Software-GL-Renderregressionen | 9 | 0 |
| **Gesamt** | **9233** | **0** |

Die fuenf Brueckenlaeufe brauchen 3,95–12,35 Sekunden simulierte Spielzeit,
Endpunktabweichung maximal 0,022 Welteinheiten. Wegberechnung im Test 42–165 ms
auf diesem Rechner; das ist keine Leistungszusage fuer Android.
17 finale 1280x720-PNGs zeigen Menue, Gespräche, alle sechs Sitzfiguren,
Lesen/Schlafen, Morgen/Nacht und Einstellungen. Eine 16,6-sekuendige echte
Desktopaufnahme zeigt Hinlaufen, Gespraech, Lesen, Lagerfeuer und Kueste.

Alle urspruenglichen PNGs sind bytegleich mit v0.2.5, ebenso Wegkatalog und
Geometrie. Nur eine zusaetzliche Uferdatei kam hinzu. Der Bildeditor-Prompt
steht in `UFER_EDIT.md` im Quellarchiv. Built-in ImageGen wurde fuer den
lokalen Objektedit genutzt; Shader mischt nur den geprueften Vordergrund.

Einige Testprozesse melden beim Beenden weiterhin RefCounted/ObjectDB-Warnungen;
`full_smoke` meldet wie zuvor zwei noch referenzierte Ressourcen. Keine
fehlgeschlagene funktionale Assertion und keine GDScript-Laufzeitfehler im
finalen Spiel-/Renderlauf. Installation, Fingerbedienung, GPU und FPS auf einem
echten Android-Telefon bleiben ungeprueft.

## Paket und Ruecksetzung

AndroidTest: `com.itoeva.godot.hd2d.worldtest`, v0.2.6 / Code 9, ARM64,
API 24 bis Ziel-API 35, Querformat, ohne Internetberechtigung.
Bestehender Testschluessel unveraendert; v2/v3-Signatur und 16-KB-ZIP-Alignment
geprueft. Kein nativer Spielstand, kein LAS-/Reminder-Vertrag wurde migriert.
Das urspruengliche Android-Preset bleibt v0.2.4 / Code 7.

APK: `Itoeva_Godot_HD2D_v0.2.6_Test.apk`, **81.475.722 Bytes**.
SHA-256: `4d664b7875b5e5ac346401894d875432cc61be525b3e1b9e11d664221978bf22`.
Zertifikat-SHA-256: `fe124bbf6e3682280f62d0ba7e0619c0fc3c266adfa74b6e99c77d4ed6318582`.
Gepackte drei JSON-Dateien und Ambience-Shader mit dem finalen Quellstand
byteweise abgeglichen; Tests, Captures, Vorschauen und Schluessel nicht im APK.

[APK in Drive Notime](https://drive.google.com/file/d/1nH1hYj4xYkB8-x_lgXPOTUxMAdXDBTrL/view?usp=drivesdk).
Metadaten bestaetigen Namen, Ordner und exakte Dateigroesse. Kein Remote-Hash
wurde vom Connector geliefert.

Die Aenderungen liegen als lokaler Commit und vollstaendiger Godot-Quellstand
vor. Es erfolgte kein GitHub-Push. Ein separater Patch stellt den Unterschied
zum wiederhergestellten gelieferten v0.2.5-Stand dar. Revert/alter Quellstand
setzt v0.2.5 wieder her; optionale Spieldaten bleiben kompatibel.
