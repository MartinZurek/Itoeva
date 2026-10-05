# Fennec: Kopfstudien fuer Animationsframes

Die beiden transparenten Atlanten wurden mit ImageGen aus dem vorhandenen
Fennec-Key-Design entwickelt. Folgende Arbeitsbeschreibung rekonstruiert die
Gestaltungsanforderungen; sie ist kein wortgetreues Protokoll der Generierungsaufrufe.

## Hauptatlas (4 Spalten, 2 Zeilen)

Zeichne acht separat ausgearbeitete Kopfansichten desselben orangefarbenen
Fantasy-Fennecs: sehr grosse Ohren, cremefarbene Schnauze, dunkle Nase, markante
Augen, kleine Fellsträhne. Behalte die Identitaet der Referenzfigur mit rostfarbenem
Reisemantel und tuerkisem Schmuck. Feine handgezeichnete Fantasy-Konzeptkunst,
klare Silhouetten und ausdrucksstarke Formen, geeignet zur spaeteren Pixelreduktion.
Transparenter Hintergrund, keine Schrift, keine Requisiten, ausreichend Zellabstand.
Zeile 1: neutraler Dreiviertelblick, derselbe Blick mit geschlossenen Augen,
froehlicher Dreiviertelblick, neugieriger Blick. Zeile 2: echtes rechtes Profil,
Vorderansicht, echte Hinterkopfansicht, konzentrierter Dreiviertelblick.
Keine Kopfansicht durch Stauchen einer anderen Ansicht vortaeuschen.

## Mimik-Ergaenzung (4 Spalten, 1 Zeile)

Dieselbe Figur und Malweise, transparente einzelne Kopfzeichnungen:
Vorderansicht blinzelnd, Vorderansicht froehlich, rechtes Profil blinzelnd,
rechtes Profil froehlich. Einheitliche anatomische Proportionen und Halsansatz.

## Weiterverarbeitung

`fennec_faces.py` entfernt Zellfragmente, registriert am Hals und gleicht
Mimikvarianten an ihre Grundansicht an. `fennec_key.py` kombiniert Kopfzeichnung
und Koerperpose; `rich_sheets.py` reduziert auf die Spielpalette und 128-Pixel-Frames.
Die zwei Profil-Mimikvarianten sind Quellen fuer einen spaeteren vollstaendigen
Profilgang; sie sind noch nicht Teil der aktiven 68-Frame-Auswahl.
