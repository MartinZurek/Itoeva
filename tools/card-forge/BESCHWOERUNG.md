# Leuchtfeuer-Beschwörungsvorschau v1

Stand 18.10.2026. Engineunabhängige Gestaltung im offline nutzbaren Wesenbuch,
kein Kampfablauf oder neuer Erwerb. Mit `python tools/card-forge/wesen.py` erzeugen,
`out/wesenbuch.html` öffnen und bei einem Leuchtfeuer-Wesen auf **Beschwören** drücken.
Die beiden Spielhelfer haben noch keine eigene Beschwörungsvorlage.

## Eine Vorlage, drei Formen

| Form | Normale Dauer | Darstellung |
|---|---|---|
| Jungform | 3,2 s | Ein Lichtkreis, kleiner Strahl, 12 Funken |
| Reife Form | 4,0 s | Zwei Lichtkreise, breiterer Strahl, 24 Funken |
| Urform | 4,8 s | Drei Lichtkreise, hoher Lichtaufbau, 36 Funken |
| Mythisch, jede Form | 9,6 s | Zusätzlicher Sternenruf und Lichtfäden, 24 weitere Funken |

Die Lichtsequenz baut von Funken und Kreisen über eine Lichtsäule zur jeweiligen
Wesenillustration auf. Die längere mythische Sequenz ist eine eigene Umsetzung
der gewünschten ausgedehnten Beschwörungsdramaturgie; keine FF8-Assets oder Tonspur.
`hex` färbt Licht, Kreise und Funken nach Karte; Glanz ergänzt goldene Lichtpunkte.
Das Wesen bleibt der zugehörige SVG-Entwurf. Bildbasis 480×270, dunkle Aussparung,
Bodenschatten und Materialkante der Illustrationen bleiben erhalten. Kein Kamerablitz.

Alle 15 Artenformen und 36 aktuellen Leuchtfeuer-Monatskarten benutzen denselben
Darsteller. Beim Monatskatalog werden Form, Seltenheit, Farbe und Glanz aus der
Karte übernommen. Artenentwürfe haben normale Vorschausequenzen. Die aktuellen
36 Karten enthalten keine Glanzkarte; Glanz wird daher im synthetischen Test geprüft.
Die Vorschau ändert weder Stats noch Form, Besitz oder Glanz einer Karte.

## Schnittstelle für spätere Videos

`beschwoerung.js` enthält `plan`, die reine Zeitfunktion `zustand` sowie drei Klassen:

- `CodeDarsteller(canvas)` zeichnet anhand der verstrichenen Zeit. Kein Kampftakt.
- `VideoDarsteller(video, quelle)` spielt eine lokale Datei `videos/<name>.mp4`
  oder `.webm` stumm und inline. Noch keine solchen Dateien ausgeliefert.
- `Spieler(code, videoFuer)` wählt für einen Plan einen registrierten Videodarsteller
  oder die Code-Vorlage. Ein neuer Start bricht den vorherigen Lauf ab.

Der gemeinsame Vertrag lautet:

```js
darsteller.spiele(plan, {
  signal,          // AbortSignal: Schließen, Wiederholung oder Seitenwechsel
  reduziert,       // Bewegungsarme Ansicht; der Spieler benutzt dafür immer Code
  phase: text => {} // Status nur bei Phasenwechseln, keine Frame-Texte
}); // Promise<void>; Ende = erfüllt, Fehler/Abbruch = abgelehnt
```

Der Plan ist eingefroren und enthält ausschließlich Gestaltung: Volk, Form/Stufe,
Farbe, Glanz, Bildpfad, mythische Kennung, Dauer, Phasengrenzen und Vorlagenschlüssel.
Beispielschlüssel: `leuchtfeuer/Urform/mythisch/matt`. Keine Angriffswerte,
Schadensereignisse oder Bestandsänderungen. Ein späterer nativer Darsteller kann
diesen Vertrag portieren; die Browserklassen sind kein Kotlin-/Godot-API.

Die lokale `videos`-Map der HTML-Vorlage ist der Anschluss für spätere Clips:

```js
videos.set('leuchtfeuer/Urform/mythisch/matt', 'videos/leuchtfeuer-urform.webm');
```

Die Registrierung ist vorerst eine Entwicklerkonfiguration. Ein Clip muss Form,
Farbe und Glanz des Plans gestalterisch abdecken; der Schlüssel allein garantiert
das nicht. Es gibt keinen Downloader, Fernkatalog oder Dateiauswahldialog.
Bei Lade-/Codec-/Abspielfehlern oder Zeitüberschreitung übernimmt Code. Nach einem
erfolgreichen Clip bleibt die zugehörige Kartenillustration als Standbild stehen.
Abbruch löst keinen Fallback aus. Fehlende Wesenbilder führen zu einer verständlichen
Fehlermeldung und können erneut versucht werden. Videoelement und Frame-Callbacks
werden bei Ende/Abbruch bereinigt; Bildladen sowie Videostart/-stillstand haben eine
Zeitgrenze von fünf Sekunden. Ein voranschreitender Clip darf länger sein als
die Code-Sequenz. Es gibt kein Abschneiden nach 9,6 Sekunden.

## Bedienung und Grenzen

Native modale Dialogbox mit beschrifteten Buttons, Fokus-Rückgabe und Escape.
Wiederholung ist auch während einer Sequenz möglich. `prefers-reduced-motion`
aktiviert ein unmittelbares Standbild ohne bewegte Effekte; die Checkbox erlaubt
eine bewusste Änderung. Ein Wechsel der Systemeinstellung wird übernommen.
Beim Verbergen der Seite wird eine laufende Sequenz gestoppt; kein automatischer
Neustart beim Zurückkehren. Die Vorschau hat keine Audioausgabe.

```text
python -m unittest discover -s tools/card-forge -p "test_*.py"
node --test tools/card-forge/test_beschwoerung.cjs
node tools/card-forge/test_galerie.cjs
```

Die Node-Tests prüfen Zeitgrenzen, Formstufen, Glanz, mythische Länge, austauschbare
Darsteller, Video-Fehler und Abbruch/Rennen mit kontrollierten Frames/Medienstubs.
Sie beweisen keinen echten Codec oder Android-GPU-Ablauf. Browserabnahme ergänzt
die Darstellung und Bedienung über localhost. Direkte `file:`-Navigation lässt
der App-Browser nicht zu; die Ordnerkopie mit relativen Assets ist vollständig,
der Start per lokaler Datei wurde dort deshalb nicht ausgeführt.
Es gibt noch keine echten Videos und keine
Android-/Godot-Integration. Kampf, Gruppe, Raster und Engine bleiben offen.
Nächster begrenzter Schnitt: eine weitere Volksvorlage mit denselben Ablauf-/
Abbruchregeln und einem eigenen Motiv, zunächst Werkvolk samt Werkling.
