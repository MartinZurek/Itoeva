# Zuschauer-Interaktion im Itoeva-Stream

Status: **umgesetzt (NT-070); echte Twitch-Verbindung noch nicht an einem Kanal erprobt**
Gilt fuer: den Build-Typ `stream` von `:app-sim`
Letzte Pruefung: 2026-09-12

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
Anreiz. Wer `!drop A` tippt, waehrend das Wesen kurz vorm Umfallen ist, sieht es schlafen gehen.

Das ist Absicht und der Kern der Sendung: Zuschauer stupsen ein Lebewesen an, sie bedienen keine
Marionette. Erst wenn die Kernhandlung wirklich gelaufen ist (`StreamInteractions.wasHandled`),
wird der Platz geleert - ein abgelehnter Vorschlag laesst ihn liegen.

## Die Befehle

| Befehl | Wirkung |
|---|---|
| `!drop A` | wirft dem Wesen hin, was in Platz A liegt |
| `!drop B` `!drop C` `!drop D` | dasselbe fuer die uebrigen Plaetze |

Nachsicht, weil Leute tippen, wie sie tippen: Gross-/Kleinschreibung egal, zusaetzlicher Leerraum
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
| `perViewerCooldownMillis` | 60 000 | damit der knappe Durchsatz nicht einem schnellen Tipper gehoert |
| `globalCooldownMillis` | 8 000 | hoechstens ~7 Anstoesse pro Minute; eine Handlung laeuft ueber mehrere simulierte Minuten |
| `logCapacity` | 40 | genug, um im Stream nachzuvollziehen, wer warum abgewiesen wurde |

Dazu ohne Zahl: ungueltige Befehle werden ignoriert, und waehrend ein Anstoss laeuft kommt kein
zweiter durch (`IMPULSE_PENDING`) - zwei gleichzeitige Ablaeufe wuerden dieselbe Figur an zwei Orte
schieben.

**Reihenfolge der Pruefungen: erst die Abstaende, dann der Weltzustand.** Wer auf Abstand steht,
wird abgewiesen, egal was in den Plaetzen liegt. Wer dagegen einen leeren Platz nennt, verliert
seinen Abstand **nicht** - er hat nur danebengegriffen, und eine Minute Schweigen als Strafe fuer
einen Tippfehler waere die falsche Lehre fuer ein neues Publikum.

Die Werte sind bewusst Vermutungen. Was der erste oeffentliche Lauf herausfinden soll, steht in
NT-058: ob Zuschauer die Mechanik verstehen, welche Plaetze sie bevorzugen, ob gespammt wird und
welche Abstaende sich richtig anfuehlen.

## Was im Bild steht

Rechts die vier Plaetze, jeder mit seinem Buchstaben - **auch der leere**, damit ein
"Platz C ist leer" im Zusammenhang steht statt wie eine Fehlermeldung zu wirken.

Unten links drei Zeilen (`StreamViewerOverlay`):

```
CHAT · itoeva
!drop A  !drop B  !drop C  !drop D
lea → Platz B
```

Die dritte Zeile war anfangs nicht vorgesehen und ist die wichtigste. Ohne sie ist eine Ablehnung
im Stream nicht von einem Absturz zu unterscheiden: Man tippt, und nichts geschieht. Mit ihr wird
der Abstand sichtbar und Teil des Spiels statt ein Verdacht.

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
von acht Sekunden ist damit am Geraet spuerbar, der Einzelabstand nicht - den pruefen die Tests.

Offline, ohne Geraet und ohne Emulator: `tools/reaction-preview/tests.sh`.

## Was fuer echte Zuschauer noch fehlt

Genau ein Eintrag - der Kanalname in
`app-sim/src/stream/res/values/stream_mode.xml`:

```xml
<string name="stream_twitch_channel" translatable="false">deinkanal</string>
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

Noch nicht erprobt ist der Lauf an einem echten Kanal mit echtem Publikum. Das ist Teil von
NT-058 und braucht einen Menschen an einem Rechner mit Android Studio.

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
