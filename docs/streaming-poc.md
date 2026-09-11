# Vorbereitung fuer den Streaming-PoC: Emulator, OBS und Twitch

Status: **Runbook und leerer Messbogen; NT-058 ist weiterhin offen und nicht durchgefuehrt**  
Einordnung: Voraussetzung fuer das Arbeitspaket NT-058, kein NT-058-Ergebnis  
Letzte Regelpruefung: 2026-09-10  
Letzter Abgleich mit dem Stream-Client: 2026-09-11 (nach PR #133)

Dieses Dokument ist Runbook und leerer Messbogen fuer den ersten begrenzten End-to-End-Test
einer oeffentlichen Itoeva-Instanz. Dieser PR liefert nur die Vorbereitung. NT-058 ist erst
erledigt, wenn ein tatsaechlicher Zwei-Stunden-Lauf samt Messwerten, Screenshots,
Wiederanlaufbefund und begruendeter GO/CHANGE/STOP-Empfehlung dokumentiert ist. Leere
`TODO`-Felder sind kein positives Testergebnis.

## Warum NT-058 weiterhin offen ist

Am 2026-09-11 hat eine Agenten-Sitzung versucht, diesen Lauf durchzufuehren. Sie konnte es
nicht, und zwar nicht aus Zeitmangel, sondern weil in ihrer Umgebung nichts davon existiert:

| gebraucht | vorhanden | Befund |
|---|---|---|
| Android SDK | nein | `ANDROID_HOME` und `ANDROID_SDK_ROOT` nicht gesetzt, kein SDK auf der Platte |
| Android Gradle Plugin | nein | `./gradlew :app-sim:assembleStream` bricht ab: `Plugin [id: 'com.android.application', version: '8.13.1'] was not found` - der Egress-Proxy erreicht das Google-Repository nicht |
| Emulator / `adb` | nein | keines der Werkzeuge installiert |
| Hardwarebeschleunigung | nein | kein `/dev/kvm`; ein Emulator waere selbst nach Installation nicht ernsthaft messbar |
| OBS oder `ffmpeg` | nein | keine Bild- oder Tonaufzeichnung moeglich |

Damit sind **alle** Kennzahlen dieses Dokuments unerreichbar: kein Bild, kein Ton, keine CPU-,
RAM- oder Frame-Messung, kein Wiederanlauf, keine Slot- oder Impulsbeobachtung. Es waere leicht
gewesen, hier plausible Zahlen einzutragen; sie waeren erfunden. Die Felder bleiben deshalb
`TODO`.

Was die Sitzung stattdessen getan hat - und was den naechsten Lauf schneller macht:

- Die beiden rein lokalen Teststrecken ausgefuehrt: `tools/reaction-preview/tests.sh` mit
  **299 gruenen Tests**, `tools/music` mit **15 gruenen Tests**.
- Den Bau-Abschnitt dieses Runbooks gegen `app-sim/build.gradle.kts` geprueft und korrigiert.
  **Er war falsch:** Er baute `installDebug` und startete `com.notime.glyphminderwatch` - also
  die normale App. Wer ihm gefolgt waere, haette zwei Stunden lang den falschen Build gemessen,
  ohne Direktstart, ohne automatische Slots und ohne Viewer-Simulator.
- Die seit PR #133 hinzugekommenen Beobachtungen als eigenen Messbogen ergaenzt
  (Abschnitt 4b): Slot-Belegung, Viewer-Impulse, Einfluss statt Steuerung, Erklaerbarkeit,
  Datenschutz.
- Einen Test ergaenzt, der Runbook und Build-Konfiguration aneinander bindet
  (`StreamRunbookTest`), damit dieselbe Abweichung nicht ein zweites Mal unbemerkt entsteht.

**NT-058 bleibt damit vollstaendig offen.** Es braucht einen Menschen an einem Rechner mit
Android Studio und OBS. Der Rest dieses Dokuments ist der Zettel, den dieser Mensch abarbeitet.

## Ziel und feste Grenze

Die **Stream-Variante** von `:app-sim` laeuft in einem Android-Emulator. Der Spielmodus bleibt
mindestens zwei Stunden sichtbar und OBS zeichnet Bild und ausschliesslich den App-Ton lokal auf.
Erst danach darf optional ein Twitch-Bandbreitentest folgen.

Seit PR #133 ist der Gegenstand dieses Laufs nicht mehr die normale App, sondern der
Stream-Client: dieselbe Runtime, derselbe Code, dieselbe Pixelwelt - nur mit
`stream_mode = true`, eigener `applicationId` und Direktstart in den Spielmodus. Alle Befehle
weiter unten beziehen sich auf diese Variante. Wer versehentlich die normale App misst, misst
weder den Direktstart noch die automatische Slot-Belegung noch den Viewer-Simulator.

Der PoC prueft:

- ob das Avatarleben ueber zwei Stunden sichtbar interessant und technisch stabil bleibt;
- ob Musik, Aktivitaeten und Szenenwechsel fuer Zuschauer zusammenpassen;
- welches Seitenverhaeltnis den runden Pixelbildschirm lesbar zeigt;
- ob Emulator und Encoder auf dem Testrechner genuegend Reserven haben;
- ob sich App, Emulator und Aufnahme kontrolliert wieder starten lassen;
- **ob Erinnerungen von selbst in hoechstens vier Stream-Slots landen** und dabei nichts
  Privates oder Medizinisches oeffentlich wird;
- **ob ein simulierter Zuschauerimpuls beeinflusst statt steuert** - also abgelehnt wird oder
  liegen bleibt, wenn ein dringenderes Ziel laeuft;
- **ob Ziel, Grund, Plan und Hindernis des Wesens waehrend des Laufs nachvollziehbar bleiben.**

Nicht enthalten sind Cloud-Betrieb, sechs parallele Instanzen, eine neue Weltengine,
Zuschauerinteraktion, Monetarisierung oder ein oeffentlicher Simulcast. Der PoC verwendet eine
frische, rein erfundene Instanz und keine privaten Reminder, Nutzerhistorien, Konten oder
medizinischen Inhalte.

## Geheimnisse und Konten

- Niemals Twitch-Stream-Key, Zugangsdaten oder ausgefuellte OBS-Profile committen, fotografieren
  oder in Logs beziehungsweise PR-Beschreibungen kopieren.
- Fuer den lokalen Zwei-Stunden-Lauf ist kein Twitch-Konto erforderlich.
- Twitch bietet fuer diesen Zweck keinen mit YouTube vergleichbaren nicht gelisteten Livestream.
  Fuer einen unsichtbaren Verbindungstest den offiziellen
  [Twitch Inspector](https://inspector.twitch.tv/) verwenden und lokal
  `?bandwidthtest=true` an den Stream-Key anhaengen. Laut Inspector erscheint der Kanal damit
  nicht online und es werden keine Benachrichtigungen gesendet.
- Vor jedem spaeteren Simulcast die dann aktuellen
  [Twitch-Simulcasting-Regeln](https://help.twitch.tv/s/article/simulcasting-guidelines) und
  [Twitch-Nutzungsbedingungen](https://legal.twitch.com/legal/terms-of-service/) erneut pruefen.
- Der erste oeffentliche Test bekommt noch keinen zusammengefuehrten Chat, keine Donations und
  keine Zuschauerbefehle.

## 1. Saubere Testinstanz vorbereiten

### Rechner und Emulator erfassen

| Feld | Wert |
|---|---|
| Datum und lokale Uhrzeit | TODO |
| Betriebssystem | TODO |
| CPU / GPU | TODO |
| RAM gesamt | TODO |
| OBS-Version | TODO |
| Android-Studio-Version | TODO |
| Emulator / API-Level | TODO |
| getesteter Git-Commit | TODO |
| getestete Spezies | TODO |

Eine neue AVD ohne Google-Konto verwenden. Keine persoenlichen Reminder oder echten Namen
anlegen. Netzwerkbenachrichtigungen anderer Programme vor der Aufnahme deaktivieren.

### Die Stream-Variante bauen und installieren

**Nicht `installDebug`.** Der Stream-Client ist ein eigener Build-Typ `stream` derselben App
(siehe `app-sim/build.gradle.kts`), debug-signiert, mit `applicationIdSuffix = ".stream"`. Er
laesst sich deshalb neben der normalen App installieren - und genau das soll er, damit beide
Zustaende getrennt bleiben.

| | |
|---|---|
| Build-Aufgabe | `:app-sim:assembleStream` (bauen) bzw. `:app-sim:installStream` (bauen und installieren) |
| APK-Pfad | `app-sim/build/outputs/apk/stream/app-sim-stream.apk` |
| `applicationId` | `com.notime.glyphminderwatch.stream` |
| Startklasse | `com.notime.glyphsim.ui.MainActivity` |
| Name im Launcher | **Itoeva Stream** (die normale App heisst dort `Tama`) |

**Bauen muss man sie seit dem 11.09. nicht mehr selbst.** `deliver-apk.yml` erzeugt bei jedem
Merge nach `main` beide Pakete und legt sie als Artefakt des Laufs ab (`itoeva-stream-*.apk`,
14 Tage). Ist die Variable `GDRIVE_STREAM_APK_FILE_ID` eingerichtet, liegt die Stream-APK
zusaetzlich als eigene Datei in Drive - neben der gewohnten `Tama-debug.apk` und nicht statt
ihrer. Der lokale Bau unten bleibt der Weg fuer einen Stand, der noch nicht gemergt ist.

Unter Windows vorher `JAVA_HOME` gemaess `CLAUDE.md` setzen.

PowerShell unter Windows:

```powershell
.\gradlew.bat :app-sim:installStream
adb.exe shell am start -n com.notime.glyphminderwatch.stream/com.notime.glyphsim.ui.MainActivity
```

Bash unter macOS oder Linux:

```bash
./gradlew :app-sim:installStream
adb shell am start -n com.notime.glyphminderwatch.stream/com.notime.glyphsim.ui.MainActivity
```

Vor dem Lauf einmal pruefen, dass wirklich die Stream-Variante laeuft - sonst misst der ganze
Nachmittag die falsche App:

```bash
adb shell pm list packages | grep glyphminderwatch
# erwartet: ...glyphminderwatch UND ...glyphminderwatch.stream
adb shell dumpsys activity activities | grep mResumedActivity
# erwartet: com.notime.glyphminderwatch.stream/...MainActivity
```

Zwei sichtbare Unterschiede, an denen die Stream-Variante ohne Werkzeuge zu erkennen ist: Sie
heisst im Launcher **Itoeva Stream**, und sie startet **unmittelbar in den Spielmodus** statt in
die Uhr. Bleibt die Uhr stehen, laeuft die falsche App.

Im Emulator den Bildschirm nur fuer den Test wach halten; die Einstellung danach wieder
zuruecksetzen. Unter PowerShell `adb.exe`, unter Bash `adb` verwenden:

```text
adb shell svc power stayon true
# Nach dem Test:
adb shell svc power stayon false
```

In der App:

1. eine einzige Spezies fuer den gesamten Lauf waehlen und oben dokumentieren;
2. Musik im Spielmodus einschalten;
3. **keine persoenlichen Erinnerungen anlegen** - der Lauf braucht nur die Erinnerungen, die
   die App selbst erzeugt. Schritt "Spielmodus oeffnen" entfaellt: Die Stream-Variante startet
   von sich aus dort;
4. Systemlautstaerke und App-Ton vor OBS einmal mit Kopfhoerern pruefen;
5. Benachrichtigungsleiste, Emulator-Bedienelemente und Mauszeiger aus dem Bild halten.

**Ein Datenschutzbefund gehoert in diesen Lauf, nicht in eine spaetere Pruefung.** Der
Stream-Client legt Ausloesungen automatisch in die Slots. Wer zum Ausprobieren doch eine eigene
Erinnerung anlegt, nimmt sie mit ins Bild. Falls das passiert: Lauf abbrechen, App-Daten
loeschen, neu beginnen - und den Vorfall unten unter "Datenschutz" vermerken.

## 2. Bildformat vor dem Langzeittest entscheiden

Der erste PoC nutzt eine OBS-Leinwand mit **1920 x 1080 bei 30 fps**. Das ist ein bewusst
konservatives Testprofil und keine Behauptung ueber eine dauerhafte Plattformvorgabe.

Vor dem Zwei-Stunden-Lauf zwei je zehnminuetige Probeaufnahmen vergleichen:

| Variante | Aufbau | Lesbarkeit | Avatar korrekt | ungenutzte Flaeche | Entscheidung |
|---|---|---:|---:|---:|---|
| A | App im Hochformat, auf volle Hoehe skaliert und mittig auf 16:9 | TODO | TODO | TODO | TODO |
| B | App-interner Querformatmodus, ohne nachtraegliches Strecken | TODO | TODO | TODO | TODO |

Die Quelle niemals ungleichmaessig strecken. Wenn Variante B den bekannten Fehler ausloest, bei
dem der Avatar nach einer Drehung falsch oder schwebend steht, Variante A verwenden und den
Fehler als Produktbefund dokumentieren. Die Zwei-Stunden-Messung soll nicht durch wiederholte
Orientierungswechsel verfremdet werden.

Im ersten PoC keine Werbe-, Chat- oder Spenden-Overlays einblenden. Freie Seitenflaechen bleiben
neutral. Ein spaeterer sachlicher App-Link gehoert zunaechst nur in die Kanalbeschreibung.

## 3. OBS-Profil

Eine Szene `Itoeva PoC` mit genau diesen Quellen anlegen:

1. Fensteraufnahme des Emulators, auf den App-Inhalt zugeschnitten;
2. App-/Fenster-Audio des Emulators;
3. keine Mikrofonquelle und kein uebriger Desktop-Ton.

Ausgangsprofil fuer vergleichbare Messungen:

| Einstellung | Startwert |
|---|---|
| Basis- und Ausgabeaufloesung | 1920 x 1080 |
| Bildrate | 30 fps |
| Videoencoder | Hardware-H.264, falls stabil; sonst Software-H.264 |
| Bitratensteuerung | CBR |
| Videobitrate | 4.500 kbit/s |
| Keyframe-Intervall | 2 Sekunden |
| Audio | AAC, 48 kHz, Stereo, 160 kbit/s |
| Lokales Aufnahmeformat | MKV |
| Automatisches Remuxen | optional nach MP4, Original-MKV behalten |

Diese Werte sind ein reproduzierbarer Ausgangspunkt fuer den PoC. Vor einem oeffentlichen Stream
die aktuellen [Twitch-Broadcasting-Hinweise](https://help.twitch.tv/s/article/broadcasting-guidelines)
erneut gegen das dann verwendete OBS-Profil pruefen.

Vor Start zehn Sekunden aufnehmen und kontrollieren:

- Bild scharf, vollstaendig und ohne Emulatorrahmen;
- App-Musik auf beiden Kanaelen, kein Mikrofon und kein Systemton;
- keine rote Pegelanzeige beziehungsweise hoerbares Clipping;
- OBS-Statistik zeigt keine Render- oder Encoding-Ueberlastung;
- Dateipfad hat mindestens 20 GB freien Speicher.

## 4. Zwei-Stunden-Lauf

Zuerst **120 Minuten lokal aufnehmen**, ohne Twitch-Verbindung. Die Anwendung waehrenddessen
nicht bedienen, ausser ein klar definierter Wiederanlauftest folgt nach der abgeschlossenen
Aufnahme.

Alle 15 Minuten einen Messpunkt erfassen. Zusaetzlich jedes Ereignis protokollieren, das laenger
als fuenf Sekunden Bild oder Ton sichtbar beeintraechtigt.

| Minute | Ort / sichtbare Aktivitaet | Musik / Wechsel | CPU gesamt | RAM gesamt | OBS-Ausfaelle | Auffaelligkeit |
|---:|---|---|---:|---:|---|---|
| 0 | TODO | TODO | TODO | TODO | TODO | TODO |
| 15 | TODO | TODO | TODO | TODO | TODO | TODO |
| 30 | TODO | TODO | TODO | TODO | TODO | TODO |
| 45 | TODO | TODO | TODO | TODO | TODO | TODO |
| 60 | TODO | TODO | TODO | TODO | TODO | TODO |
| 75 | TODO | TODO | TODO | TODO | TODO | TODO |
| 90 | TODO | TODO | TODO | TODO | TODO | TODO |
| 105 | TODO | TODO | TODO | TODO | TODO | TODO |
| 120 | TODO | TODO | TODO | TODO | TODO | TODO |

Nach dem Lauf aus Aufnahme und Notizen ermitteln:

| Kennzahl | Ergebnis |
|---|---|
| App-Abstuerze | TODO |
| laengster Bildstillstand ohne beabsichtigte Ruhe | TODO |
| Audioausfaelle ueber 5 Sekunden | TODO |
| OBS: verlorene Frames | TODO |
| OBS: wegen Rendering verpasste Frames | TODO |
| OBS: wegen Encoding verpasste Frames | TODO |
| CPU Durchschnitt / Spitze | TODO |
| RAM Start / Ende / Spitze | TODO |
| verschiedene Orte | TODO |
| verschiedene Aktivitaeten | TODO |
| sichtbare Reaktionen / Animationen | TODO |
| Musikstuecke und Wechselzeitpunkte | TODO |
| laengste Phase ohne sichtbare Veraenderung | TODO |
| harte oder unpassende Musikwechsel | TODO |
| Avatar- oder Szenenfehler | TODO |

Nicht nur Mengen zaehlen. Zu jedem problematischen Abschnitt den genauen Zeitstempel der
Aufnahme notieren. Besonders pruefen:

- Bleibt die Figur zu lange nur drinnen oder sichtbar untatig?
- Wirkt ein Aussenaufenthalt wie ein echter Aufenthalt statt wie ein kurzer Durchgang?
- Aendert sich die Musik innerhalb einer stabilen Rolle nach etwa drei bis fuenf Minuten?
- Passt der Musikwechsel zur sichtbaren Situation und ist die Ueberblendung weich?
- Sind Aktivitaeten, Text und wichtige Pixel auf einem normalen Laptopbildschirm erkennbar?
- Gibt es innerhalb von zehn Minuten mindestens einen Grund, weiter zuzusehen?

## 4b. Was seit PR #133 zusaetzlich zu messen ist

Der Zwei-Stunden-Lauf oben misst Stabilitaet und Vielfalt. Seit dem Stream-Client kommen drei
Dinge dazu, die man **nur im Lauf** sehen kann - im Test ist jedes davon schon belegt, aber nur
gegen eine erfundene Welt.

### Automatische Slot-Belegung

Der Stream-Client legt jede oeffentlich zeigbare Ausloesung selbst in den ersten freien der
**vier** vorhandenen Slots (`ACTION_SLOT_COUNT`, `ui/ActionSlotState.kt`). Zu beobachten:

| Frage | Ergebnis |
|---|---|
| Wie viele Slots waren nach 30 / 60 / 120 Minuten belegt? | TODO |
| Gab es je einen fuenften Platz oder ein Ueberschreiben? | TODO |
| Tauchte dieselbe Ausloesung zweimal auf? | TODO |
| Wie lange dauerte es bis zur ersten Belegung? | TODO |
| Blieben alle vier dauerhaft belegt (Stau) oder leerten sie sich wieder? | TODO |

Ein Stau ist kein Absturz, aber ein Produktbefund: Vier dauerhaft volle Slots heissen, dass
Zuschauer nichts Neues mehr zu waehlen bekommen.

### Lokaler Viewer-Simulator

Ein Tippen auf einen belegten Slot bedeutet «ein Zuschauer waehlt diesen Impuls». Mindestens
sechs Versuche ueber den Lauf verteilen, davon bewusst je einer auf einen leeren Slot und einen
auf einen Slot, dessen Vorkommen laengst vorbei ist.

| # | Minute | Slot | erwartet | tatsaechlich | Slot danach geleert? |
|---:|---:|---:|---|---|---|
| 1 | TODO | TODO | TODO | TODO | TODO |
| 2 | TODO | TODO | TODO | TODO | TODO |
| 3 | TODO | TODO | TODO | TODO | TODO |
| 4 | TODO | leer | wirkungslos | TODO | - |
| 5 | TODO | veraltet | wirkungslos | TODO | - |
| 6 | TODO | TODO | TODO | TODO | TODO |

### Einfluss ist keine Steuerung

Das ist die wichtigste Beobachtung des ganzen Laufs, und sie ist eine ueber ABLEHNUNG. Ein
Impuls geht als fluechtiger `GoalInfluence` in die vorhandene Utility-Wahl; sein Gewicht
(`GoalInfluence.MAX_WEIGHT` = 0,15) liegt unter `UtilitySelector.MIN_PRESSURE` (0,2). Ein
dringendes Beduerfnis oder ein laufender Mehrschrittplan gewinnt also weiterhin.

Deshalb ausdruecklich **einen Impuls setzen, waehrend das Wesen sichtbar beschaeftigt ist** -
etwa mitten in Arbeit oder auf dem Weg zum Laden:

| Frage | Ergebnis |
|---|---|
| Hat das Wesen den Impuls sofort befolgt? (erwartet: nein) | TODO |
| Blieb der Slot belegt, bis der Impuls wirklich angenommen wurde? | TODO |
| Wurde der Slot erst nach der VOLLSTAENDIG sichtbaren Routine geleert? | TODO |
| Gab es einen Sprung, ein Teleport oder einen abgebrochenen Ablauf? | TODO |
| Wurde ein Impuls dauerhaft nie angenommen? Welcher, und warum vermutlich? | TODO |

Ein sofort befolgter Impuls waere kein Erfolg, sondern ein Fehler: Dann waere aus Einfluss
Fernsteuerung geworden, und die Figur haette aufgehoert, eine eigene zu sein.

### Erklaerbarkeit waehrend des Laufs

Zu drei selbst gewaehlten Zeitpunkten festhalten, was das Wesen gerade will, warum, welcher Plan
laeuft und was ihn gegebenenfalls blockiert (Quelle: `stream/LivingObservationSource.kt`).

| Minute | Ziel | Grund | Plan | Hindernis | passt das zum sichtbaren Bild? |
|---:|---|---|---|---|---|
| TODO | TODO | TODO | TODO | TODO | TODO |
| TODO | TODO | TODO | TODO | TODO | TODO |
| TODO | TODO | TODO | TODO | TODO | TODO |

### Datenschutz

| Frage | Ergebnis |
|---|---|
| War je ein medizinischer Inhalt in einem Slot sichtbar? (erwartet: nie) | TODO |
| War je ein frei beschrifteter/privater Text sichtbar? (erwartet: nie) | TODO |
| Sonstige personenbezogene Anzeige im Bild? | TODO |

Ein einziger Treffer hier ist ein **STOP** in Abschnitt 8, unabhaengig von allen anderen Werten.

## 5. Wiederanlauf separat pruefen

Nach der vollstaendigen Zwei-Stunden-Aufnahme:

1. App per Android-Einstellungen beenden und wieder oeffnen;
2. OBS-Aufnahme stoppen und neu starten;
3. Emulator kontrolliert neu starten;
4. App erneut in den Spielmodus bringen.

| Schritt | Zeit bis wieder sichtbares Leben | Zustand plausibel fortgesetzt | manueller Eingriff | Befund |
|---|---:|---:|---|---|
| App-Neustart | TODO | TODO | TODO | TODO |
| OBS-Neustart | TODO | TODO | TODO | TODO |
| Emulator-Neustart | TODO | TODO | TODO | TODO |

Beim Stream-Client zusaetzlich pruefen, weil beides erst seit PR #130 und #133 existiert:

| Frage | Ergebnis |
|---|---|
| Startet die App wieder direkt im Spielmodus, ohne Bedienung? | TODO |
| Sind die belegten Slots nach dem Neustart noch da? (erwartet: ja, `ActionSlotStore`) | TODO |
| Setzt der Living Agent seinen Zustand plausibel fort oder faengt er bei null an? | TODO |
| Springt die Simulationszeit sichtbar oder holt sie sinnvoll auf? | TODO |

Ein zufaelliger oder sichtbar widerspruechlicher Neustart ist kein Cloud-Problem, sondern ein
Befund fuer NT-059. Noch keinen neuen Checkpoint-Dienst bauen.

## 6. Optionaler Twitch-Inspector-Test

Nur nach erfolgreicher lokaler Aufnahme:

1. Stream-Key ausschliesslich lokal in OBS eintragen.
2. Gemaess Twitch Inspector `?bandwidthtest=true` anhaengen.
3. 30 Minuten mit demselben Profil senden.
4. Inspector-Sitzung auf Stabilitaet, Bitrate und Verbindungsabbrueche pruefen.
5. Stream-Key nie in Screenshot, Export oder Repository aufnehmen und danach aus dem OBS-Profil
   entfernen.

| Kennzahl | Ergebnis |
|---|---|
| Ingest-Region | TODO |
| Testdauer | TODO |
| durchschnittliche / schwankende Bitrate | TODO |
| Verbindungsabbrueche | TODO |
| Inspector-Warnungen | TODO |

## 7. Screenshots und Nachweise

Nur Bilder ohne Kontodaten oder Stream-Key ablegen:

- `docs/streaming-poc/01-portrait.png`
- `docs/streaming-poc/02-landscape.png`
- `docs/streaming-poc/03-obs-stats-start.png`
- `docs/streaming-poc/04-obs-stats-end.png`
- `docs/streaming-poc/05-twitch-inspector.png` nur beim optionalen Test

Die zweistuendige Aufnahme ist zu gross fuer Git und bleibt lokal. Im Ergebnis nur Dateiname,
Dauer, Groesse und SHA-256 notieren.

## 8. Entscheidung fuer NT-059

Dieser Abschnitt wird erst im tatsaechlichen NT-058-Lauf ausgefuellt. Vorher darf dieses
Vorbereitungsartefakt keine Entscheidung fuer NT-059 ausloesen.

Nach dem Lauf genau eine Empfehlung markieren und mit Messwerten begruenden:

- **GO:** technisch stabil, Bild und Ton tragfaehig, Aktivitaeten und Musik bieten ueber zwei
  Stunden genuegend beobachtbare Veraenderung. NT-059 darf den emulatorbasierten Einzelbetrieb
  weiter bewerten.
- **CHANGE:** technisch grundsaetzlich stabil, aber ein klar benannter Produkt- oder
  Darstellungshebel muss zuerst verbessert werden, etwa lange Leerlaufphasen, Bildformat,
  Musikmonotonie oder falsche Avatarposition.
- **STOP:** Abstuerze, Datenschutzrisiko, anhaltende Bild-/Audioausfaelle oder Ressourcenlast
  machen selbst den Einzelbetrieb unbrauchbar. Vor weiterer Streaming-Infrastruktur zuerst diese
  Ursache beheben.

**Auswahl:** TODO  
**Begruendung mit Messwerten und Zeitstempeln:** TODO

NT-060, Cloud-Host und mehrere Charakterinstanzen bleiben gesperrt, bis NT-059 die Ergebnisse
dieses Dokuments ausgewertet hat.
