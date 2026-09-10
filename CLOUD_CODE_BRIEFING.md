# Cloud-Code-Briefing: aktueller Auftrag und Produktkontext

Diese Datei ist der verbindliche, kurze Übergabeprompt für Claude Code und andere KI-Agenten.
Sie wird über `AGENTS.md` und `CLAUDE.md` in jeder neuen Sitzung sichtbar. Bei Widersprüchen gilt
`EVOLUTION.md`. Dieses Briefing beschreibt Richtung und Status; es ist keine selbstständige
Freigabe, alle genannten Zukunftsfunktionen sofort zu implementieren.

## Startprompt für jede neue Sitzung

Arbeite am Repository Itoeva als an einer fortlaufend evolvierenden Pixelwelt. Prüfe zuerst den
Git-Status und arbeite nie direkt auf `main`. Lies `AgentGuide.md`, `CLAUDE.md` und dieses Briefing.
Wenn die Aufgabe Produkterlebnis, Charaktere, Geschichten, Tagesablauf, öffentliche Beobachtung
oder Zuschauerinteraktion berührt, lies zusätzlich die relevanten Abschnitte in `Vision.md` und
`EVOLUTION.md`. Unterscheide immer ausdrücklich zwischen aktuellem Verhalten, beschlossener
Langfristvision, getesteter Evidenz und `OPEN DECISION`. Setze nur den kleinsten freigegebenen,
rücknehmbaren Schritt um, sichere Verhaltensänderungen mit Tests ab und dokumentiere die Übergabe
für den nächsten Agenten. Erfinde keine Freigabe für Netzwerk, Konten, Bezahlung, Twitch-/YouTube-
Integration, private Nutzerdaten oder medizinische Reminder.

## Aktueller Hauptauftrag: Living Agent System

Die Charakter-Musikvorbereitung ist abgeschlossen; die Audiodateien bleiben bis zur manuellen
Einzel-Erzeugung und Hoerfreigabe aussen vor. Der naechste freigegebene Architektur-Meilenstein
ist das [Itoeva Living Agent System](LIVING_AGENT.md).

Baue es ausschliesslich in den dort beschriebenen kleinen PR-Schnitten. Die Simulation waehlt aus
Beduerfnissen, Weltzustand, Persoenlichkeit, Erinnerung, Beziehungen und Kosten; sie schreibt
keine Handlung vor. Kein `StoryManager`, kein freier Textdialog, keine zweite Weltpipeline.
Zuerst reiner deterministischer Kern, dann profilbezogene Persistenz, dann bestehende
`PlayRoutine`-Anbindung und zuletzt der read-only Stream-Vertrag.

## Was gerade geändert wurde

- `AGENTS.md` ist seit PR #61 der verbindliche Einstiegspunkt für alle KI-Agenten.
- PR #62 hat den täglichen Evolutionslauf neu ausgerichtet: Offene `ITO-*`-Aufgaben haben Vorrang;
  bei leerem Backlog gilt `evolutions/DAILY_LIFE_TASK.md` als kontrollierter Dauerauftrag.
- Der Dauerauftrag priorisiert beobachtbaren Tagesablauf, Charakterunterschiede, natürliche
  Übergänge und die sanfte sichtbare Wirkung persönlicher Reminder statt leicht zählbarem
  Füllmaterial.
- `evolutions/DAILY_LIFE_LEARNING.md` übergibt Nutzerfeedback, belegte Erkenntnisse und den nächsten
  sinnvollen Hebel zwischen angenommenen Evolutionen.
- Workflow, Runner, Auftrag, Rechte-, Review- und Merge-Gates dürfen sich aus diesem Lern-Overlay
  nicht selbst verändern. Jede Evolution bleibt ein eigener Branch mit Tests, zweiter Prüfung und
  Pull Request.
- **Seit 2026-09-03 darf eine beauftragte Agentensitzung mergen** — bei grüner CI, ohne
  Merge-Konflikt und ohne offene Review-Anmerkung. Der unbeaufsichtigte Lauf mergt weiterhin nie
  sein eigenes Ergebnis; `claude-primary-run.yml` und `runner/` haben kein Merge-Recht. Der
  Revert bleibt beim Menschen. Einzelheiten in EVOLUTION.md, Evolution History.

## Neue übergreifende Produktvision

Itoeva soll sichtbar machen, welche Möglichkeiten und Grenzen automatisierte Gestaltung von
Geschichten und Pixel-Avatar-Leben hat. Die Evolution ist nicht nur der Entwicklungsprozess hinter
der App, sondern ein Teil des erlebbaren Werks: Die Welt verändert sich schrittweise, die Wesen
leben mit diesen Veränderungen und angenommene, verworfene oder gescheiterte Versuche bleiben
nachvollziehbar.

Die Vision hat drei miteinander verbundene Perspektiven:

1. **Persönliche App:** Nutzer erleben eine eigene lokale Instanz, beobachten ihr Wesen und
   beeinflussen dessen Alltag sanft durch ihre persönlichen Reminder.
2. **Öffentliche Charakter-Streams:** Jedes Wesen soll langfristig eine eigene echte
   24/7-Itoeva-Instanz erhalten, die gleichzeitig auf YouTube Live und Twitch ausgespielt werden
   kann. Sie lebt fortlaufend mit Schlaf, Essen, Sport, Ausflügen und Musikwechseln statt als
   Videoschleife. Der spätere Betrieb soll serverseitig und unabhängig von Martins PC oder
   Smartphone möglich sein.
3. **YouTube-Evolutionstagebuch:** Zusätzlich zu den YouTube-Livestreams sollen die Avatare
   möglichst selbst als Protagonisten erzählen, was sich Tag für Tag in ihrer Welt verändert hat,
   was sie erlebt haben und wo die Evolution an Grenzen gestoßen ist. Aus demselben belegten
   Material können später auch kurze Highlights entstehen.

## Nächster Validierungsschritt: Emulator plus OBS

Vor jeder Cloud-, Multi-Instanz- oder Plattformintegration kommt ein begrenzter Proof-of-Concept:
genau eine bestehende `:app-sim`-Instanz läuft in einem Android-Emulator und wird mit OBS über
einen längeren Zeitraum aufgenommen beziehungsweise privat getestet. Gemessen werden
Stabilität, sichtbare Aktivitätsvielfalt, Musik, Bildformat, Ressourcenverbrauch und
Wiederanlauf. Dieser PoC darf keine Produktions-Cloud, keine neue Headless-Engine und keine
Zuschauerintegration vorwegnehmen. Das priorisierte Arbeitspaket steht als NT-058 in
`NextTasks.md`; die technische Einordnung in `Architecture.md`.

Dezente App-Hinweise gehören zunächst in die Stream- beziehungsweise Kanalbeschreibung. Sichtbare
Werbeeinblendungen, zusammengeführte Plattform-Chats und automatische Cross-Promotion bleiben bis
zur aktuellen Regelprüfung unentschieden.

## Zuschauer beeinflussen, aber steuern nicht direkt

Für die öffentliche Welt ist als Richtung entschieden, dass Zuschauer über klar begrenzte
Berechtigungen aus Abos, Donations oder vergleichbaren Mechanismen Impulse geben können. Ein
Impuls kann beispielsweise eine **öffentliche, erfundene** Routine zeitlich verschieben, eine
erlaubte Aktivität oder einen Ort gewichten oder einem Wesen eine begrenzte Wahl eröffnen.

Dabei gelten folgende Grenzen:

- Die öffentliche Streaming-Welt ist eine eigene Instanz. Sie greift nie auf private App-Reminder,
  Nutzerhistorien, Konten oder lokale Daten zu.
- Zuschauer verschieben niemals persönliche oder medizinische Reminder; `MEDICINE` ist vollständig
  ausgeschlossen.
- Einfluss bleibt begrenzt, nachvollziehbar und von der Eigenlogik des Avatars gerahmt. Zuschauer
  geben Impulse, keine beliebigen Befehle.
- Geld, Abos oder häufige Interaktion dürfen keine Schuld-, Verlust- oder Strafmechanik erzeugen.
- Ungeprüfter Freitext wird nicht unmittelbar als Dialog, Lore oder ausführbarer Auftrag in die
  Welt übernommen.

## Noch nicht entschieden oder gebaut

Entschieden sind die getrennten echten 24/7-Instanzen je Charakter, die Zielausspielung auf
YouTube Live und Twitch sowie langfristig ein serverseitiger Betrieb ohne Martins Geräte. Noch
nicht entschieden sind Cloud-Anbieter, Laufzeitmodell, Encoder-/Relay-Topologie, technische
Twitch-/YouTube-Anbindung, Video-, Sprach-, Backend-, Konto- oder Zahlungsarchitektur. Ebenfalls
offen sind konkrete Interaktionskontingente, Preise, Cooldowns, Moderation, Plattformregeln,
Rechte an generierten Medien, tatsächlich erreichbare Verfügbarkeit und der genaue
Produktionsweg für Avatarstimmen und Videos. Jede solche Umsetzung
braucht ein eigenes kleines Arbeitspaket sowie Sicherheits-, Datenschutz-, Kosten- und
Rechtsprüfung.

Der endgültige Produktname bleibt `OPEN DECISION`: Der Nutzer verwendet aktuell auch „Toeva“, das
Repository heißt weiterhin „Itoeva“. Kein Agent darf daraus allein eine Umbenennung ableiten.

## Maßstab für kommende Arbeit

Eine gute Evolution macht mindestens eines davon erkennbar besser: Freude am Zuschauen,
Plausibilität des Tagesablaufs, Individualität eines Wesens, nachvollziehbare Wirkung eines
zulässigen Impulses oder die Fähigkeit der Welt, ihre eigene Veränderung wahrheitsgetreu zu
erzählen. Technische Menge, neue Dateien, mehr Lore oder mehr Animationen sind für sich kein
Erfolgskriterium.
