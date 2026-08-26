# Tagesablauf

Produkt- und Planungsrahmen für das Leben der Avatare im Spielmodus. Dieses Dokument beschreibt,
wie ein glaubwürdiger Tagesablauf wachsen soll. Konkrete Implementierung gehört in den Code,
Arbeitspakete in [NextTasks.md](NextTasks.md), übergeordnete Prinzipien in
[Vision.md](Vision.md).

## Zielbild

Der Avatar wartet nicht auf den Nutzer und beginnt beim App-Start nicht immer am selben Ort. Er
führt ein eigenes, sanftes Leben. Beim Öffnen sieht der Nutzer eine plausible Momentaufnahme:
abhängig von Uhrzeit, Entwicklung, Gewohnheiten und Weltzustand.

Der Ablauf darf nie wie eine Aufgabenliste oder Strafe wirken. Abwesenheit senkt keine Beziehung,
erzeugt keine Schuld und wird nicht vollständig nachgespielt. Nach längerer Abwesenheit wird nur
der gegenwärtige plausible Zustand bestimmt.

## Entscheidungsreihenfolge

Wenn mehrere Einflüsse konkurrieren, gilt zunächst:

1. Grundbedürfnis und Sicherheit
2. Tageszeit und Schlafrhythmus
3. Jahreszeit, Wetter und Wochentag
4. verfügbare Orte, Gegenstände und Vorräte
5. Skills, Level und Persönlichkeit des Avatars
6. offene Gewohnheiten und Beziehung zum Nutzer
7. kontrollierte Variation

Ein Einfluss gewichtet den Tagesablauf, ohne stärkere Ebenen beliebig zu überschreiben. Ein hoher
Kreativitäts-Skill darf beispielsweise die Freizeit prägen, aber nicht regelmäßig den Schlaf
verdrängen.

## Grundrhythmus

Der heutige Ablauf arbeitet mit Morgen, Mittag, Abend und Nacht. Typische Tätigkeiten sind:

- morgens: aufstehen, trinken, bewegen, orientieren;
- tagsüber: arbeiten, fokussieren, Besorgungen, Bewegung, Kreativität;
- abends: Spaziergang, Nähe, Lesen, Kreativität, Erholung;
- nachts: schlafen, ruhen, seltene leise Regungen.

Das sind Gewichtungen, kein minutengenauer Stundenplan. Übergänge sollen natürlich variieren,
solange Ort und Handlung zusammenpassen.

## Wiedereinstieg

- Kurze Abwesenheit: Ort und Absicht fortsetzen; keine neue Situation nur wegen eines
  Bildschirmwechsels.
- Längere Abwesenheit: direkt in den aktuellen Tageszustand springen; verpasste Tätigkeiten nicht
  nacheinander abspielen.
- App-Neustart: gespeicherten Zustand und Zeitabstand auswerten, nicht auf einen festen Startort
  zurückfallen.
- Zeitreise vermeiden: Das Ergebnis beschreibt, was jetzt geschieht, nicht alles, was seit dem
  letzten Besuch geschehen sein könnte.

Die heutige Grenze zwischen kurzer und langer Abwesenheit beträgt zehn Minuten. Sie ist ein
Balancing-Wert und darf nach Beobachtung angepasst werden.

## Evolution des Tagesablaufs

### Skills und Level

Skills verändern Qualität und Auswahl einer Tätigkeit, nicht nur ihre Häufigkeit. Ein Anfänger
nutzt eine kurze einfache Handlung; ein entwickelter Avatar kann Werkzeuge, mehrstufige Abläufe,
andere Orte oder langfristige Vorhaben verwenden. Level schalten Möglichkeiten frei, ersetzen
aber nicht Persönlichkeit und Tageszeit.

### Persönlichkeit und Beziehung

Verschiedene Avatare interpretieren denselben Tagesabschnitt unterschiedlich. Neigungen geben
passenden Tätigkeiten zusätzliches Gewicht. Gemeinsame Erfahrungen dürfen neue Varianten oder
soziale Situationen öffnen, aber Abwesenheit darf nichts Erreichtes abbauen.

### Jahreszeit, Wetter und Kalender

Jahreszeiten sollen Tageslänge, Kulisse, Pflanzen, Kleidung, verfügbare Außenaktivitäten und
seltene Ereignisse beeinflussen. Wetter entscheidet eher über die Ausführung oder den Ort einer
Absicht als über die Persönlichkeit: Bewegung kann bei Regen drinnen stattfinden, statt ersatzlos
zu verschwinden. Wochentage und besondere Tage dürfen den Rhythmus lockern.

### Weltzustand

Vorräte, Geld, erworbene Gegenstände, Besucher und freigeschaltete Orte geben Handlungen Ursachen
und Folgen. Zufall darf auswählen, wenn mehrere plausible Möglichkeiten bestehen; ein klarer
Weltzustand hat Vorrang, beispielsweise einkaufen oder arbeiten bei fehlenden Vorräten.

## Leitplanken

- Kein Teleportieren ohne sichtbaren oder erklärbaren Übergang.
- Avatar, Requisite und Boden teilen bei jeder Tätigkeit dieselbe räumliche Ebene.
- Keine parallelen Routinen, die denselben Avatar gleichzeitig steuern.
- Keine Medikamentenhandlung als zufällige autonome Tätigkeit.
- Keine negative Beziehungsauswirkung durch Abwesenheit.
- Neue Einflussfaktoren zuerst als Gewichtung ergänzen; harte Sonderregeln brauchen einen klaren
  Grund und Tests.
- Persistiert wird der kleinste semantische Zustand: Absicht, Ort und Zeitpunkt statt einzelner
  Animationsframes.

## Offene Entscheidungen

- Welche Skills existieren und wie werden sie erworben?
- Welche Level schalten welche Ablaufvarianten, Orte oder Gegenstände frei?
- Reale Jahreszeit des Geräts oder eine eigene Spieljahreszeit?
- Wie stark dürfen Nutzergewohnheiten den autonomen Ablauf beeinflussen?
- Welche Tätigkeiten dauern über mehrere Tagesabschnitte oder Besuche hinweg?
- Welche seltenen Ereignisse sind wiederholbar und welche werden Teil der Biografie?

Diese Punkte werden erst spezifiziert und validiert, bevor ein großes Skill-, Level- oder
Jahreszeitensystem gebaut wird.
