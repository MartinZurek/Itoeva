# Zuschauer-Interaktion im Itoeva-Stream

Status: **umgesetzt (NT-070); Live-Befehl im Kanal fennec_itoeva erfolgreich, Dauerlauf noch offen**
Gilt fuer: den Build-Typ `stream` von `:app-sim`
Letzte Pruefung: 2026-10-02

Dieses Dokument beschreibt, wie Zuschauer kostenlos Einfluss auf die laufende Itoeva-Instanz
nehmen, und vor allem, **warum die Schichten so geschnitten sind**. Der Schnitt ist der eigentliche
Gegenstand: Er entscheidet, ob eine spaetere Bits-Anbindung ein Nachmittag oder ein Umbau wird.

## Die eine Architekturregel

Itoeva bleibt **ein** Spiel. Der Stream-Client ist ein weiterer Client derselben Welt, kein Fork.
Zuschauer-Eingaben sind Eingaben in die bestehende Simulation - keine zweite Spiellogik daneben.

```
Twitch-Chat (kostenlos, anonym)  ─┐
Demo-/Testeingang                ─┼→  StreamInteractionProvider
spaeter: Bits, Subs              ─┘        │
                                           │  ViewerCommand  (wer, was, woher, wann)
                                           ▼
                                    StreamCommandGate        Abstaende, Weltzustand, Protokoll
                                           │
                                           │  StreamInteraction.DropSafeSlot(slotId)
                                           ▼
                                    StreamInteractions.select(...)      ← bestand bereits
                                           │
                                           │  ExternalImpulse → GoalInfluence
                                           ▼
                                    Living Agent, Utility-Auswahl       ← unveraendert
```

**Unterhalb des Tors weiss niemand mehr, woher ein Angebot kam.** Das ist keine Absichtserklaerung,
sondern ein Test: `StreamViewerChainTest.die Herkunft aendert am Ergebnis im Spiel nichts` schickt
dasselbe Angebot aus drei Quellen durch die gesamte Kette und verlangt dasselbe Ergebnis. Wenn
spaeter jemand die Bezahlfrage in die Auswahl oder in den Agenten traegt, wird dieser Test rot.

## Einfluss, nicht Fernsteuerung

Ein angenommenes Angebot setzt **kein Ziel** und fuehrt **keine Handlung** aus. Es wird zu einem
`GoalInfluence` - einem Vorschlag, den dieselbe Utility-Auswahl ueberstimmen darf wie jeden anderen
Anreiz. Wer `A` tippt, waehrend das Wesen kurz vorm Umfallen ist, sieht es schlafen gehen.

Das ist Absicht und der Kern der Sendung: Zuschauer stupsen ein Lebewesen an, sie bedienen keine
Marionette. Erst wenn die Kernhandlung wirklich gelaufen ist (`StreamInteractions.wasHandled`),
wird der Platz geleert - ein abgelehnter Vorschlag laesst ihn liegen.

## Die Befehle

| Befehl | Wirkung |
|---|---|
| `A` | bietet dem Wesen die Erinnerung aus Platz A an |
| `B`, `C`, `D` | dasselbe fuer die uebrigen Plaetze |

Die ganze Nachricht muss genau ein Buchstabe sein; Kleinbuchstaben und Leerraum am Rand sind
erlaubt. `A bitte`, `A!`, `AB`, `E` und einzelne Zahlen loesen nichts aus. Nur belegte,
oeffentlich sichere Plaetze koennen ausgewaehlt werden.

Bei Annahme wandert das Erinnerungssymbol in 450 ms sichtbar vom Speicherplatz
zum Avatar und blendet bei ihm aus. Das Ziel folgt der aktuellen Position, auch wenn der
Avatar gerade laeuft. Der Platzrahmen und sein Buchstabe bleiben rechts stehen; das Symbol
verschwindet dort sofort und bleibt nach der Uebergabe ausgeblendet. Intern bleibt der
Reminder bis zur tatsaechlichen Bearbeitung gespeichert. Wird das Angebot durch einen
anderen Platz ersetzt, kehrt der bisher nicht bearbeitete Reminder in seinen Platz zurueck.
Abgewiesene Befehle
starten keine Bewegung. Es blendet in weiteren 120 ms am Avatar aus. Die gruene Markierung
am Herkunftsplatz gilt nur fuer diese Uebergabe und bleibt nicht bis zum Ende einer Routine.
Demo-Tippen und Chat nehmen denselben Weg.

Die bisherigen Langbefehle bleiben. Nachsicht, weil Leute tippen, wie sie tippen:
Gross-/Kleinschreibung egal, zusaetzlicher Leerraum
egal, `!drop 1` bis `!drop 4` erlaubt, angehaengte Satzzeichen (`!drop a!`) und Text dahinter
(`!drop a bitte`) werden verworfen statt den Befehl zu zerstoeren. Alles Uebrige ist kein Befehl
und loest nichts aus.

Praefix und Schluesselwort stehen in `StreamCommandConfig` - **einmal**. Die Hinweiszeile im Bild
wird aus derselben Quelle gebaut (`StreamCommandConfig.hint()`), und ein Test haelt beides
aneinander. Eine angezeigte Syntax, die der Parser nicht kennt, waere der teuerste Fehler an
dieser Stelle: Niemand meldet ihn, die Zuschauer tippen ihn ab und geben auf.

## Die Schutzschicht

Zentral konfigurierbar in `StreamCommandConfig`, mit diesen Voreinstellungen:

| Stellschraube | Wert | Warum |
|---|---|---|
| `perViewerCooldownMillis` | 1 500 | kurze Spam-Bremse, schneller Wechsel desselben Zuschauers moeglich |
| `globalCooldownMillis` | 750 | keine gleichzeitig fliegenden Angebote |
| `replacePendingImpulse` | true | neue gueltige Auswahl ersetzt das bisher wartende Angebot |
| `logCapacity` | 40 | genug, um im Stream nachzuvollziehen, wer warum abgewiesen wurde |

Dazu ohne Zahl: ungueltige Befehle werden ignoriert. Ein neuer belegter Platz ersetzt den
bisher angebotenen Platz; dessen Reminder bleibt erhalten. Es laeuft weiterhin nur eine
Routine, deren bisheriger Anstoss bei einer neuen Auswahl abgeloest wird. Derselbe bereits
angebotene Platz wird nicht erneut gestartet (`IMPULSE_PENDING`). Wer z. B. A und nach
1,5 Sekunden B schreibt, sieht die Uebergabe von B, auch wenn A noch nicht bearbeitet war.
Leere oder private Ersatzplaetze aendern den bisherigen Impuls nicht.

**Reihenfolge der Pruefungen: erst die Abstaende, dann der Weltzustand.** Wer auf Abstand steht,
wird abgewiesen, egal was in den Plaetzen liegt. Wer dagegen einen leeren Platz nennt, verliert
seinen Abstand **nicht** - er hat nur danebengegriffen, und eine Wartezeit als Strafe fuer
einen Tippfehler waere die falsche Lehre fuer ein neues Publikum.

Die Werte sind bewusst Vermutungen. Was der erste oeffentliche Lauf herausfinden soll, steht in
NT-058: ob Zuschauer die Mechanik verstehen, welche Plaetze sie bevorzugen, ob gespammt wird und
welche Abstaende sich richtig anfuehlen.

## Was im Bild steht

Auf dem eingerichteten Stream-PC sind Hintergrundmusik und Atmo (Regen, Wind, Brandung,
Stadt usw.) seit 2026-10-02 auf Nutzerwunsch um 6 dB abgesenkt. Nur der Stream-Build
wendet diesen Faktor 0,5 an; kurze Avatar-/Aktionsklaenge behalten ihren Pegel. Musik-
Normalisierung, Ueberblendungen und der Musikschalter bleiben wirksam.

Rechts die vier Plaetze, jeder mit seinem Buchstaben - **auch der leere**, damit ein
"Platz C ist leer" im Zusammenhang steht statt wie eine Fehlermeldung zu wirken.

Unten links drei Zeilen (`StreamViewerOverlay`):

```
CHAT · fennec_itoeva
A  B  C  D
lea → Platz B
```

Die dritte Zeile war anfangs nicht vorgesehen und ist die wichtigste. Ohne sie ist eine Ablehnung
im Stream nicht von einem Absturz zu unterscheiden: Man tippt, und nichts geschieht. Mit ihr wird
der Abstand sichtbar und Teil des Spiels statt ein Verdacht.

Auf dem eingerichteten Stream-PC ist dieser untere Textbereich auf Nutzerwunsch in OBS
ausgeschnitten. Die Chat-Verbindung arbeitet trotzdem; der Status ist am Emulator sichtbar.

## Ohne Twitch testen

Der Demo-Eingang ist eingebaut und braucht kein Netz, kein Konto und keine Einrichtung.

1. `./gradlew :app-sim:installStream`, App **Itoeva Stream** starten (sie startet direkt in den
   Spielmodus).
2. Warten, bis Erinnerungen automatisch in die Plaetze wandern (Auto-Save; im Stream-Modus muss
   niemand etwas ziehen).
3. Einen belegten Platz **antippen**.

Das Tippen geht nicht an der Mechanik vorbei: Es schreibt dieselbe Zeile, die ein Zuschauer tippen
wuerde (`!drop B`), und nimmt denselben Weg durch Parser, Tor und Auswahl. Sonst pruefte die
Vorfuehrung am Geraet eine Strecke, die es im Stream gar nicht gibt.

Jedes Tippen zaehlt als **ein anderer** Zuschauer (`demo-1`, `demo-2`, …). Der gemeinsame Abstand
von 750 ms ist damit am Geraet spuerbar, der Einzelabstand nicht - den pruefen die Tests.

Offline, ohne Geraet und ohne Emulator: `tools/reaction-preview/tests.sh`.

## Verbindung zum echten Kanal

Der Kanalname steht ausschliesslich fuer den Stream-Build in
`app-sim/src/stream/res/values/stream_mode.xml`:

```xml
<string name="stream_twitch_channel" translatable="false">fennec_itoeva</string>
```

Danach `./gradlew :app-sim:assembleStream`. Mehr ist nicht noetig, **insbesondere kein Token**:
Twitch laesst Mitlesen ueber einen anonymen `justinfan`-Namen zu (siehe `TwitchIrc`). Daraus folgt
dreierlei, und alles davon ist erwuenscht:

1. In diesem Client existiert **kein Geheimnis**, das im Stream, in einem Protokoll oder in einem
   Commit landen koennte.
2. Der Client kann nichts schreiben und im Chat also auch nichts anrichten.
3. Es kommt keine neue Abhaengigkeit ins Projekt - die Verbindung ist ein TLS-Socket aus dem JDK.

Die Netzberechtigung liegt ausschliesslich in `app-sim/src/stream/AndroidManifest.xml`. Die normale
Itoeva-App bekommt sie **nicht** und kann damit gar keine Verbindung aufbauen - das ist keine
Vereinbarung zwischen Entwicklern, sondern eine Zusicherung des Betriebssystems, und
`StreamBoundaryTest` haelt sie fest.

Am 2026-10-02 wurden TLS-Verbindung, anonyme Anmeldung und Kanalbeitritt vom Stream-PC
bestaetigt. Alle 61 Stream-Tests und der APK-Build sind erfolgreich; die aktualisierte App
ist im Emulator installiert. Ein echter Chatbefehl aus `fennec_itoeva` wurde als
`fennec_itoeva -> slot A` angenommen; Platz A war gruen markiert und wurde nach der
Bearbeitung geleert. OBS uebertrug dabei weiter mit App-Ton. Noch offen ist ein laengerer
Publikumslauf (NT-058). Nach der Rueckmeldung ueber A wurde der schnelle Wechsel A->B im
Emulator aufgezeichnet: beide Angebote angenommen, beide Bewegungen sichtbar, Markierungen
anschliessend verschwunden. Derselbe Zuschauer-Wechsel nach 1500 ms ist automatisiert getestet.

## Stream-Buehne

Der Stream-Build startet im Querformat. Die Uhr links oben zeigt dauerhaft die Ortszeit
in 48 dp; Mond und Traum bleiben Teil der Welt, ersetzen aber nicht die Stream-Uhr.
Die Figur ist unabhaengig von der Uhr 88-132 dp gross, passend zur Bildschirmhoehe.
Die vier Speicherplaetze stehen rechts in 48 dp. Der Chat-/Demo-Fuss ist im Querformat
ausgeblendet, die Chat-Anbindung bleibt aktiv. OBS verwendet das ganze Querformat-Fenster
ohne den bisherigen Hochformat-Zuschnitt.

Nacht-Materie ist fuer die Uebertragung heller; Lichtquellen, leere Zellen und Szenen-
Ausblendungen werden erhalten. Bei angenommenen Angeboten bewegt sich Fennec kurz
abwaerts und wieder aufwaerts, gleichzeitig mit dem fliegenden Reminder. Diese sichtbare
Bestaetigung aendert weder die autonome Auswahl noch den Abschluss der Erinnerung.
Die normale App und ihre gespeicherten Einstellungen sind davon unberuehrt.

### Angebote, Geschichten und Klang

Leere Plaetze werden nach drei Sekunden mit fehlenden Themen aus Trinken, Ruhe, Lesen
und Bewegung ergaenzt. Dazu entsteht eine oeffentliche Spiel-Ausloesung mit echter Room-ID;
sie wird ueber denselben autonomen Bearbeitungsweg abgeschlossen wie andere Angebote.
Gleiche oeffentliche Angebote werden als vollstaendige Abzuege dauerhaft zurueckgestellt, nie als beantwortet markiert. Ein wartendes Angebot bleibt im aktiven Platz. Beim spaeteren Nachfuellen seines Themas wird der Bestand zuerst wiederverwendet. A-D werden dadurch verschieden, ohne den alten Ausloeser zu verlieren. Private Abzuege werden nicht umgeordnet.

Oben zeigen kleine Symbole das echte laufende Projekt mit seinen Arbeitsgaengen und die
Tagesreise bzw. das Tagesvorhaben. Naehe und gelernte Vorliebe erscheinen erst, wenn sie
im Living-Zustand vorhanden sind. Es werden keine Story-Erfolge oder Beziehungen erfunden.
Bei wartenden Angeboten bleibt das aktuelle Absichtssymbol ueber dem Avatar lesbar, auch
nachts. Die Szene nutzt kuehle Aussen- und warme Innenlichtquellen.

Ein angenommener Anstoss und ein veraenderter Projekt-/Reisefortschritt bekommen zwei
leise gerechnete Motivtoene, sofern Ton erlaubt ist. Musik und Wetter sinken waehrend
Uebergaben und Handlungen kurz auf 45 % des schon leiseren Stream-Pegels ab. Normalisierung,
Einspieler, Ueberblendungen und Stummschalter bleiben wirksam. Beim Verlassen wird der
zusaetzliche Faktor zurueckgesetzt. Andere App-Builds behalten ihr bisheriges Klangverhalten.

## Wo spaeter Bits angeschlossen wuerden

An genau einer Stelle: ein neuer `StreamInteractionProvider` neben
`TwitchChatInteractionProvider`, plus ein neuer Eintrag in `ExternalImpulseSource`.

```kotlin
internal class TwitchBitsInteractionProvider(...) : StreamInteractionProvider {
    override val origin = ExternalImpulseSource.TWITCH_BITS
    override val commands: Flow<ViewerCommand> = ...   // 100 Bits -> DropSafeSlot(slotId)
}
```

In `DockScreen` kaeme er in dieselbe `listOfNotNull(...)`-Liste. **Sonst nichts.** Kein Eingriff in
Tor, Auswahl, Ziele, Plaene oder Handlungen.

Was dann noch fehlte, gehoert ausdruecklich nicht zu dieser Runde: OAuth, EventSub und damit zum
ersten Mal ein Geheimnis in diesem Client - das braucht eine eigene Entscheidung darueber, wo es
liegt und wer es sieht. Kostenlose Chat-Interaktionen bleiben davon unberuehrt und sollen es auch:
Die Unterscheidung FREE gegen BITS ist eine Frage der Quelle, nicht des Spiels.

## Bekannte Kante

Zwischen Tor und Auswahl liegt eine Nachpruefung in Room (`selectStreamSlot`). Wurde die
Erinnerung in derselben Sekunde bereits anderweitig beantwortet, hat der Zuschauer seinen Abstand
verbraucht, ohne dass etwas geschieht - die Blase meldet dann Annahme, und im Bild passiert
nichts. Selten und harmlos, aber es steht hier, statt spaeter jemanden zu verwirren.
