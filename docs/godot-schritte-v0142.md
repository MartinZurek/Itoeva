# Laufkorrektur 0.14.2

Martins erneute Rueckmeldung verwirft die optische Qualitaet von 0.14.0/0.14.1.
Die Vierer-Auswahl ist korrekt nach Spezies benannt; eine vertauschte Listenposition
wurde nicht gefunden. Gleichzeitig mit der Auswahl kamen neue Atlanten. Ihre
unabhaengig gezeichneten Phasen aendern auch Koerper, Schal und Proportionen.
Die Registrierung samt optischem Fluss erzeugte daraus keine klaren Fusskontakte.
Eine grosse Zahl gruener Konturchecks hatte diesen Fehler nicht erkannt.

## Aenderung

Alle acht bisherigen Spezies behalten bei Bewegung eine feste Zeichnung je
Blickrichtung. Eng gefuehrte technische Masken bewegen die Gliedmassen getrennt;
es gibt keinen Wechsel oder optischen Fluss zwischen verschiedenen Farbzeichnungen.
Originalbilder werden nicht bearbeitet. Fennec, Gloop, Puffling und Hootlet bleiben
Bewohner; die vier Spielerauswahlen bleiben Mooslyn, Nocti, Starlit und Wyrmling.

- Fuer normale Fuesse gibt es Stand- und Rueckfuehrungsphase mit stetigem Aufsetzen.
  Der Fortschritt folgt der zurueckgelegten Strecke. Die lesbare Mindestzyklus-
  strecke2,15/2,8 bleibt von0.14.1 erhalten. Ein letzter Weltmassstabsabgleich fand
  sonst4-5 Zyklen/Sekunde fuer kleine Wesen; dies wurde vor Lieferung verhindert.
  Damit ist der Fusskontakt stilisiert: Die Sohlen haften nicht exakt in Welt-
  koordinaten. Die mathematische Kontaktpruefung gilt fuer den Zeichenmassstab.
  Der Fuss selbst bleibt ein unverformter Ausschnitt; die Verbindung zur Huefte biegt.
- Wyrmlings Seitenansicht hat vier getrennte Pfoten mit versetztem Schritt, beim
  Joggen diagonal gepaart. Keine unbewegte hintere oder vordere zusaetzliche Pfote.
- Starlits untere Strahlen drehen und uebernehmen abwechselnd Gewicht. Sie werden
  nicht durch die menschliche Beinabbildung zu gelben Saeulen verlaengert. Ein
  Hoehenausgleich haelt die unterste Spitze am Boden. Ein anfaenglicher harter
  Winkelanschlag wurde durch eine stetige Begrenzung ersetzt.
- Noctis Fluegel folgen einem langsamen, stetigen Ausschlag mit fester Ansatzlinie.
  Die gemalte Koerper-/Gesichtszeichnung bleibt stehen. Kein Flussfeld-Morphing.

Die ungueltige punktweise Umkehrung kurzer Gliedmassen erzeugte im ersten Versuch
Gelenkfugen; eine monotone Umkehrung beseitigt sie. Maskenreste an Starlits Spitzen
und eine stehende Wyrmling-Pfote wurden in den echten Renderbildern gefunden und
korrigiert. Der ruhige obere Koerperbereich ueberspringt die Beinumkehrung im Shader.
Die technischen Masken belegen beim Laden aller acht Wesen plus Flug maximal
6,75 MiB GPU-Daten; Telefon-Leistung ist nicht gemessen.

## Pruefung und Grenzen

Neue reine Kontaktpruefung: Standfuss gegen Weltfortschritt, stetige Kontakte,
verschiedene Zeitschritte sowie Geh-/Jogging-Uebergangswerte. Technische
Quellhash-, Masken- und Eindeutigkeitspruefung der Beinabbildung bestanden.
Die Rotationsdarstellung Starlits ist eine eigene visuelle Gewichtsumlagerung;
der mathematische Standfuss-Nachweis der normalen Beine ist keine Messung ihrer
Sternspitzen. Die umfassenden aktiven Renderpruefungen konzentrieren sich auf die
vier Spieler; alle acht Wesen wurden in je drei Ansichten als Bildfolgen geprueft.

Lokal bestanden vor dem abschliessenden Taktabgleich: 2352 aktive OpenGL-, 1088 unveraenderte Kontur-, 84 Vollwelt-, 869 Figuren-/Helfer-, 200 Ausdauer-/Anzeige- und 115
Kampfbewegungs-/Lichtchecks. Die unveraenderten Konturen werden weiterhin separat
mit abgeschalteter Schrittsteuerung geprueft. Die aktive Renderpruefung verwendet
im finalen Lauf das tatsaechliche Geh-/Sprinttempo und 24 Bilder je Sekunde,
auch beim Erstellen der Vorschau. Kopfvergleich folgt Starlits gewollter
Gewichtsverlagerung. Keine optische Freigabe allein aus Testzahlen.

Eine zusaetzlich gestartete aeltere `movement_flow.gd`-Pruefung erwartet fuer die
inzwischen kompakte Hootlet-Luftwende weiterhin Atlasnummern 85/101/108; ihre
uebrigen 123 Checks bestehen. Diese Erwartung ist bereits mit dem unveraenderten
kompakten Atlas (48 Felder) unvereinbar. Kein Eingriff in diesen unabhaengigen
Luftwende-Faden; die aktuelle APK-Regression verwendet die Figurenpruefung.
Bekannte Ressourcenmeldungen beim Beenden der Tests bleiben dokumentiert.

Branch `codex/godot-planted-steps`, auf PR377/0.14.1 aufgebaut. AndroidTest
0.14.2/Code29 verwendet denselben Signier-/Lieferweg und dieselbe Drive-Zieldatei.
Vorbereitung und Liefererfolg sind getrennte Zustaende: erst der erfolgreiche
Baulauf samt Remote-Pruefsummenabgleich belegt eine ausgelieferte APK.
Telefon-GPU/FPS, Daumensteuerung und Martins menschliche Bewegungsabnahme sind offen.

## Lieferung abgeschlossen

Test-APK0.14.2/Code29 ist erfolgreich gebaut, signiert und vollstaendig am
[bisherigen Downloadziel](https://drive.google.com/file/d/148Ls55fl4-I8OUqRlMlFUSy7-PT75wLX/view?usp=drivesdk)
abgelegt. Paketkennung und Zertifikat erlauben das Update zur0.14.1.

- Quellcommit: `0e81c0598cb7b60c0edb2f0298ee593cd296b0ab`.
- [Baulauf39685621858](https://github.com/MartinZurek/Itoeva/actions/runs/39685621858),
  Job119519011559: alle Godot-Regressions-/Render-, Export-, Signatur- und Uploadschritte erfolgreich.
- 192874327 Bytes; SHA-256 `3e31dd6ff6320dec4bb5a260e791bc464bd921d42ec07d21631f0de07a7bca02`.
- Remote-Groesse und MD5 entsprechen der vollstaendigen lokalen Datei.
- APK-Artefakt12387057782, Pruefprotokolle12386358982;14 Tage Aufbewahrung.
- Allgemeiner Verify-Lauf39685630646 ebenfalls erfolgreich.
- Finale aktive OpenGL-Pruefung2352/0; Originalkonturen1088/0; Ausdauer/Takt204/0.
- Zwoelfsekundige Vorschau mit24 Bildern/Sekunde, allen vier Spielern, drei
  Richtungen sowie Gehen/Joggen/Flug fertig und dauerhaft als MP4 gesichert.

Liefernachweis: `docs/validation/godot-v0142-build-report.json`.
Die fruehere Zwischenfassung wurde vor Lieferung abgebrochen. Kein Merge in main.
Weiterhin offen: Martins menschliche Bewegungsabnahme und Telefon-GPU/FPS.
Die stilisierten Sohlen sind nicht exakt in Weltkoordinaten verankert.
