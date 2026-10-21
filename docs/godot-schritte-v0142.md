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
  Schrittweite und Fortschritt teilen die zurueckgelegte Weltstrecke und Bildskalierung.
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

Lokal bestanden: 2352 aktive OpenGL-, 1088 unveraenderte Kontur-, 84 Vollwelt-, 869 Figuren-/Helfer-, 200 Ausdauer-/Anzeige- und 115
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
