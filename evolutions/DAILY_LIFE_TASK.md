# Dauerauftrag: Das beobachtbare Leben der Avatare verbessern

Dies ist genau eine Evolution. Lies zuerst `Tagesablauf.md` und danach
`evolutions/DAILY_LIFE_LEARNING.md`. Waehle selbst den kleinsten Aenderungssatz mit dem groessten
erwartbaren Effekt darauf, wie lebendig, individuell und nachvollziehbar sich ein normaler Besuch
im Spielmodus anfuehlt.

## Auswahlreihenfolge

1. Bearbeite zuerst den obersten passenden `[open]`-Eintrag im Feedback-Eingang von
   `evolutions/DAILY_LIFE_LEARNING.md`, wenn er innerhalb der bestehenden Leitplanken sicher und
   in einer Evolution vollstaendig umsetzbar ist.
2. Sonst behebe eine dort belegte Wiederholung, Sackgasse oder sichtbar unplausible Situation.
3. Sonst waehle einen Hebel aus dem bestehenden Tagesablauf: Aktivitaetsauswahl, natuerlicher
   Uebergang, Wiedereinstieg, Charakterunterschied, Weltzustand, kontextgebundener Dialog oder die
   sichtbare sanfte Wirkung einer persoenlichen Erinnerung.

## Mindestnutzen statt Fuellmaterial

Die Aenderung muss einen konkreten Vorher-/Nachher-Effekt fuer Spieler haben, der waehrend eines
normalen Besuchs sichtbar oder spuerbar werden kann. Sie soll mindestens zwei bereits vorhandene
Einfluesse sinnvoll verbinden, zum Beispiel Tageszeit und Persoenlichkeit, Ort und Handlung,
Wiedereinstieg und Absicht oder Erinnerungsthema und naechste Ambient-Aktivitaet. Eine belegte
Blockade in nur einem dieser Einfluesse darf ebenfalls allein behoben werden.

Nicht ausreichend sind fuer sich allein:

- ein weiteres dekoratives Requisit;
- eine isolierte Bibliotheksanimation;
- reine Lore oder eine weitere Beziehung;
- reine Pixelpolitur ohne Einfluss auf Ablauf oder Verstaendlichkeit;
- ein generisches Refactoring ohne beobachtbaren Spieler- oder nachgewiesenen Zuverlaessigkeitsnutzen.

## Erlaubter Gestaltungsraum

- Bestehende Ambient-Aktivitaeten, Routinen, Orte, Requisiten und Reaktionen besser kombinieren.
- Gewichtungen und Auswahl innerhalb des dokumentierten Grundrhythmus begruendet verbessern.
- Die sechs vorhandenen Wesen mit ihren belegten Neigungen unterschiedlich auf denselben Kontext
  reagieren lassen, ohne ihre geschuetzte Persoenlichkeit umzudeuten.
- Persoenliche Erinnerungen als sanften Einfluss auf Absicht, Aktivitaet, Ort, Reaktion oder
  Gespraech sichtbar machen. Reminder-Semantik, Tagesziel, XP-Regeln und Besitzverhaeltnisse
  bleiben unveraendert.
- Kontinuitaet beim Wechsel zwischen Oberflaechen, bei kurzer Abwesenheit und beim Wiedereinstieg
  verbessern, ohne verpasste Zeit nachzuspielen.
- Vorhandene lokale Daten fuer nachvollziehbare Dialoge oder sichtbare Ursache-Wirkung nutzen.

## Harte Grenzen

- Keine neue grosse Spielstruktur, Quest, Wirtschaft, Cloud-, Netzwerk-, Telemetrie- oder
  Sprachmodellfunktion einfuehren.
- Keine `OPEN DECISION` selbst entscheiden und keinen geschuetzten Punkt aus `EVOLUTION.md`
  semantisch veraendern.
- `MEDICINE` bleibt aus zufaelligen autonomen Aktivitaeten ausgeschlossen.
- Keine negative Beziehung, Schuld oder Strafe aus Abwesenheit ableiten.
- `.github/`, `runner/` und diese Datei nicht veraendern. Vorschlaege dazu gehoeren ausschliesslich
  als offene Meta-Idee in `evolutions/DAILY_LIFE_LEARNING.md` und brauchen einen eigenen,
  ausdruecklich menschlich freigegebenen Prozess-PR.

## Umsetzung und lernende Uebergabe

- Bestehende Mechanik und Muster wiederverwenden; keine parallele Ablaufsteuerung erfinden.
- Eine Verhaltensaenderung mit einem gezielten Test absichern und in der Evolution History von
  `EVOLUTION.md` dokumentieren.
- `evolutions/DAILY_LIFE_LEARNING.md` im selben Aenderungssatz aktualisieren: verwendetes Feedback
  nur bei vollstaendiger Erledigung auf `[done]` setzen und im Journal Problem, Spielerwirkung,
  Evidenz, Ergebnis sowie den sinnvollsten naechsten Hebel festhalten.
- Eine neue gelernte Heuristik nur eintragen, wenn sie durch einen Test, einen reproduzierbaren
  Fehler oder mindestens zwei unabhaengige Journaleintraege belegt ist. Vermutungen bleiben als
  Hypothese im Journal.
- Wenn keine sichere Spielerlebnis-Aenderung moeglich ist, keine Kleinigkeit als Ersatz bauen.
  Dokumentiere stattdessen genau eine begrenzte, belegte Hypothese oder Meta-Idee im Lernjournal,
  die eine spaetere Evolution entscheidungsfaehig macht.

Am Ende muessen Diff, Tests und Dokumentation gemeinsam erklaeren, warum diese eine Evolution das
Leben der Avatare glaubwuerdiger, individueller oder interessanter zu beobachten macht.
