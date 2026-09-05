# Schlaf & Traeume

Schlaf ist in Itoeva ein exklusiver Nachtzustand: Nach dem Hinlegen bleibt der reale Avatar bis zum Morgen im Bett. Autonome Fidgets, Wanderungen und neue Perform-Aktionen duerfen ihn in dieser Zeit nicht herausloesen. Eine echte Erinnerung oder eine ausdrueckliche Nutzeraktion darf den Schlaf weiterhin unterbrechen.

Traeume liegen als rein visuelle Schicht darueber. Tageshandlungen werden als kleine semantische `AnimationType`-Erinnerungen gespeichert, nicht als Screenshots oder Videos. In seltenen Abstaenden waehlt die Traumlogik daraus ein Erlebnis und spielt eine vorhandene Avatar-Reaktion in einer wachsenden, aufsteigenden Traumblase. Der Avatar in der Blase ist nur Projektion; Weltposition und Bettbelegung des realen Avatars bleiben unveraendert.

`SLEEP` und `MEDICINE` werden nicht als Traumerinnerung gespeichert. Die erste Version merkt sich bis zu zwoelf unterschiedliche Themen **je aktivem Begleiter und simuliertem Tag**. Der Tag folgt dabei derselben `PlayTimeLapse`-Zeit wie die Welt, sodass FAST/TURBO-Testtage ihre Erinnerungen nicht vermischen. Die Darstellung verwendet das bestehende Reaktionsrepertoire, damit neue sichtbare Faehigkeiten spaeter ohne zweites Animationssystem auch in Traeumen wiederverwendbar sind.

## Naechster Ausbau

- Erinnerungen spaeter konkreter als den groben `AnimationType` speichern, zum Beispiel Dribbling statt nur `MOVE`.
- Mehrere kurze Erlebnisse zu einer kleinen Traumsequenz verbinden und staerker verfremden.
- Interaktive Dream-Easter-Eggs an echte Items/Skills anbinden. Der gewuenschte Luftballon-Einstieg wird erst an einen realen Ballon-Node oder Gegenstand angeschlossen; aktuell existiert im Repository noch kein solcher Gegenstand, deshalb wird dafuer kein paralleles Item-System erfunden.