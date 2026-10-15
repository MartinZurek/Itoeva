# Godot 0.6.2 – Kartenansicht, Eigenlicht und Kampfabschluss

Basis: ausgelieferte 0.6.1, Commit `cb2d0d6f35f70044da9da229d2685e9d92c2eb8e`, PR #361.
AndroidTest wird 0.6.2 / Code 18; bestehendes Paket und bestehender GitHub-Debugschluessel.

## Nutzerwirkung und Ursachen

- Deck bauen → Ansehen → Schliessen funktioniert wieder. Der bisher nur lokal erzeugte
  RefCounted-Editor verlor auf der Detailseite seine letzte starke Referenz; am
  Schliessen-Knopf war danach kein Signal mehr verbunden. Die App haelt nun den Editor.
  Entwurf, Filter und Listenposition bleiben beim Rueckweg erhalten.
- Starlet (Martins „Starlit“) traegt keine Laterne. Ihr gemalter Koerper und ein weicher
  Halo werden bei Dunkelheit allmaehlich heller. Bei Tageslicht geht das Eigenlicht aus.
  Tageszeit und Schattenwaldabschnitt bestimmen die Dunkelheit. Das gilt auch fuer
  den Parkbewohner; der Spieler beleuchtet zusaetzlich die benachbarte Kulisse.
- Traglaternen folgen Richtung, Figurengroesse und Bewegung statt einem festen
  Weltversatz. Docht, Pendelbewegung, Glaslicht und lokaler Schein sind gemeinsam
  verankert; Sprung und Kamerabewegung nehmen dieselbe Weltposition mit.
- Neun Flammenanker an den gezeichneten Feuerkoerben nachgemessen. Vor allem das
  rechte Bachfeuer lag deutlich zu tief. Flammenbreite folgt nun der Flammenhoehe;
  der alte harte Ausschnitt am unteren Flammenrand entfaellt. Intro und Welt
  verwenden dieselben Feuerkoordinaten. Licht und Flamme bleiben am Quellbild.
- Nachtwurz behaelt eine ruhige Grundzeichnung mit langsamer Gewichtsverlagerung.
  Die vier eigenstaendig gezeichneten Ruheposen werden nicht mehr 5,5-mal pro
  Sekunde durchgeschaltet. Wirkung → LP-Verlust/Trefferpose → 0,40 Sekunden fuer
  den LP-Balken → drei Fallposen → Ergebnis. Kein Umfallen beim bloss berechneten Sieg.
- Sieger stehen gut sichtbar links, nehmen ihre gezeichnete Jubelpose ein und
  springen beziehungsweise schweben zeitversetzt. XP zaehlen vom gespeicherten
  Vorher- zum Nachherwert; Fortschrittsbalken und hinzugewonnene XP folgen.
  Heiltrank und Mondsplitter erscheinen gestaffelt mit eigenen animierten Symbolen
  und dem echten Vorratsuebergang. Dezente aufsteigende Lichtfunken begleiten den Jubel.
- Ein originales 12-Sekunden-Thema ersetzt nach der letzten Attacke die Kampfmusik.
  Vier Takte C-Dur, 80 BPM, Glocken, gezupfte Toene und Pad, deterministisch erzeugt
  durch `tools/audio/generate_victory_music.py`. Keine Fremdsamples. Der kurze
  vorhandene Siegesklang bleibt ein getrennt schaltbarer Effekt. Musikpause und
  Lautstaerke verwenden weiter die vorhandenen Einstellungen.

## Pruefung und Uebergabe

`tests/polish_v062.gd` prueft den echten Menue-/Touchpfad ohne lokale Editorreferenz,
dreimaliges Oeffnen/Schliessen, Entwurf und Scrollposition, LP-/Treffer-/Fallreihenfolge,
Siegesmusik erst nach Ende, echte XP-Zwischenwerte, gestaffelte Beute und Eigenlicht
im Wald, am Tag sowie nachts im Park. `polish_capture_v062.gd` rendert diese Phasen
in echtem Software-OpenGL. Die vorhandenen Karten-, Spezial-, Helfer-, Wald-,
Raum-, Tuer-, Audio- und Lichtregressionen bleiben Bestandteil des APK-Workflows.

Lokal bestaetigt: 36 neue Ablaufchecks, 96 bestehende Siegeschecks, 29 Audiochecks,
9 Ambience-, 7 Wald- und 9 Figurenlicht-Renderchecks ohne funktionale Fehler.
Weitere vorhandene Regressionen und der konkrete Actions-Abschluss werden im PR
mit den tatsaechlichen Ergebnissen ergaenzt. Godot meldet in einigen Testprozessen
beim Beenden noch RefCounted-/Resource-Restverweise; keine Parser-/Laufzeitfehler
im getesteten Spielablauf. Diese Meldungen sind kein Messbeleg fuer Telefon-Speicher.

Keine Spielregeln, Belohnungsmengen, Decks, Save-Schluessel, Room- oder LAS-Vertraege
geaendert. Neuer Blick auf bereits gespeicherte Ergebnisse, keine neue Belohnungsbuchung.
Ruecksetzen: den isolierten Aenderungscommit revertieren und APK 0.6.1 verwenden.
Android kann fuer ein Downgrade eine Neuinstallation verlangen; lokalen Godot-Stand
vorher sichern. main bleibt unveraendert. Telefon-GPU/FPS, subjektiver Klangeindruck
und das konkrete Tragen in allen gemalten Posen bleiben Geraeteabnahme.
