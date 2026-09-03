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
  nicht selbst verändern. Jede Evolution bleibt ein eigener Branch mit Tests, zweiter Prüfung,
  Pull Request und menschlichem Merge.

## Neue übergreifende Produktvision

Itoeva soll sichtbar machen, welche Möglichkeiten und Grenzen automatisierte Gestaltung von
Geschichten und Pixel-Avatar-Leben hat. Die Evolution ist nicht nur der Entwicklungsprozess hinter
der App, sondern ein Teil des erlebbaren Werks: Die Welt verändert sich schrittweise, die Wesen
leben mit diesen Veränderungen und angenommene, verworfene oder gescheiterte Versuche bleiben
nachvollziehbar.

Die Vision hat drei miteinander verbundene Perspektiven:

1. **Persönliche App:** Nutzer erleben eine eigene lokale Instanz, beobachten ihr Wesen und
   beeinflussen dessen Alltag sanft durch ihre persönlichen Reminder.
2. **Öffentliche Twitch-Welt:** Eine eigene, von privaten App-Instanzen getrennte Welt kann
   dauerhaft und ohne Sprecher als beobachtbares Pixel-Avatar-Leben gestreamt werden.
3. **YouTube-Evolutionstagebuch:** Die Avatare sollen möglichst selbst als Protagonisten erzählen,
   was sich Tag für Tag in ihrer Welt verändert hat, was sie erlebt haben und wo die Evolution an
   Grenzen gestoßen ist. Aus demselben Material können später auch kurze Highlights entstehen.

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

Die Vision beschließt noch keine technische Twitch-, YouTube-, Streaming-, Video-, Sprach-,
Backend-, Konto- oder Zahlungsarchitektur. Ebenfalls offen sind konkrete Interaktionskontingente,
Preise, Cooldowns, Moderation, Plattformregeln, Rechte an generierten Medien, Verfügbarkeit des
Dauerstreams und der genaue Produktionsweg für Avatarstimmen und Videos. Jede solche Umsetzung
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
