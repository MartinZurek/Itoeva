# Glyphminder

Grundgerüst für eine App fürs Nothing Phone (4a) Pro, die einen eigenen Glyph
Toy registriert (Uhrzeit-Anzeige) und dazwischen wiederkehrende visuelle
Erinnerungen ("Trink was", "Beweg dich", "Bleib fokussiert", ...) auf der Glyph
Matrix aufblinken lässt.

## Setup

1. **Glyph Matrix SDK besorgen**: `app/libs/README.txt` folgen, AAR als
   `app/libs/glyphsdk.aar` ablegen (ohne diese Datei schlägt der Gradle-Sync fehl).
2. Projekt in Android Studio öffnen, Gradle-Sync abwarten – `gradlew`/Wrapper sind
   bereits vorhanden (Gradle 8.13, siehe "Build & Installation" unten).
3. **NothingKey**: Hängt am Build-Typ und steht **nicht mehr im Manifest**. Debug- und
   `releaseCheck`-Builds bekommen automatisch den öffentlichen Test-Key (`"test"`) aus dem
   Demoprojekt — für die Entwicklung ist damit nichts einzurichten. Ein echter Release
   braucht einen eigenen Key über das Nothing Developer Programme; er gehört als
   `nothingKey=…` in die (nicht eingecheckte) `keystore.properties` oder in die
   Umgebungsvariable `NOTHING_KEY`. Fehlt er, bricht `assembleRelease` ab, statt eine App
   mit dunkler Matrix auszuliefern. Begründung in `app/build.gradle.kts`.
4. **Geräte-Konstante**: `glyph/GlyphMatrixService.kt` registriert
   `Glyph.DEVICE_25111p` (Phone (4a) Pro, Glyph Matrix). Per Bytecode-Inspektion
   der AAR verifiziert – kleines "p" am Ende, nicht zu verwechseln mit
   `Glyph.DEVICE_25111` (das ist die Glyph-Bar-Variante des einfachen (4a) ohne Matrix).
   Erfordert `glyph-matrix-sdk-2.0.aar` aus dem GlyphMatrix-Developer-Kit-Repo
   (siehe `app/libs/README.txt`) – die ältere Example-Project-AAR kennt diese
   Konstante nicht.
5. App per USB auf dem (4a) Pro installieren, in den Glyph-Einstellungen den
   Toy "Glyphminder" auswählen.

## Konzept

Statt Terminen/Kalender gibt es "Erinnerungen" (`GlyphReminder`): eine
Erinnerung ist an bestimmten Wochentagen innerhalb eines Zeitfensters (z.B.
9:00–18:00) aktiv und blinkt darin in einem festen Intervall (z.B. alle 15 Min.)
kurz mit einer gewählten Animation auf - dazwischen und außerhalb des Fensters
läuft die normale Uhrzeit. Kein Kalender-Sync, keine Serientermine, keine
Vorlaufzeit-Logik - jede Erinnerung ist vollständig durch Bezeichnung,
Animationstyp, Wochentage, Start-/Endzeit und Intervall definiert.

## Module

Drei Gradle-Module:

- **`:core`** (`com.notime.glyphcore`) – gemeinsame Datenschicht und Alarmplanung: Room-Entities
  (`GlyphReminder`, `LibraryAnimation`, …), DAOs, Repositories, `FrameCodec`/`FrameCrossfade`,
  `ReminderScheduler`. **Bewusst ohne UI und ohne Darstellung.**
- **`:app`** (`com.notime.glyphkalender`) – die echte Hardware-App: registriert den Glyph Toy und
  gibt Erinnerungen auf der Glyph Matrix des Nothing Phone (4a) Pro aus.
- **`:app-sim`** (`com.notime.glyphsim`) – Simulator ohne Nothing-Hardware, inzwischen mit
  Tamagotchi-Avatar, Dock-Modus, Home-Screen-Widget und Play Mode (siehe unten).

Die beiden Apps hatten bis dahin ~28 dateigleiche Kopien der Datenschicht, die zunehmend
auseinanderliefen (Änderungen landeten nur in `:app-sim`). `:core` beendet das.

**Der Übergabepunkt ist `core/reminder/ReminderHost.kt`.** Drei Dinge lassen sich nicht teilen
und werden dort von jeder App hereingereicht:

1. **Die Datenbank** – `:app-sim` führt zusätzlich Tabellen für Avatar-Fütterungen und
   Play-Mode-Fortschritt. Eine gemeinsame `@Database`-Klasse würde `:app` diese Tabellen
   aufzwingen, deshalb behält jedes Modul seine eigene und reicht nur den DAO-Zugriff herein.
2. **Der Alarm-Empfänger** – pro Modul im Manifest registriert, stellt eine Erinnerung völlig
   unterschiedlich dar (Hardware-Matrix vs. Widget + simulierte Matrix).
3. **Das Play-Mode-Würfeln** (`playModeReroll`) – nur `:app-sim` kennt Avatare und damit den
   Charakter, nach dem gewürfelt wird (siehe "Play Mode" unten). Der Parameter hat als
   Standardwert die Identität, `:app` ruft `install(...)` deshalb unverändert auf.

Beides wird in `Application.onCreate()` gesetzt (`GlyphKalenderApp` / `GlyphSimApp`), **nicht** in
einer Activity: Alarm- und Boot-Empfänger laufen auch in Prozessen, in denen nie eine Activity
existiert hat.

**Profile statt Avatare im Kern:** In `:app-sim` hat jeder Avatar seinen eigenen Satz
Erinnerungen. Im Kern heißt das neutral „Profil" (`GlyphReminder.profileId`,
`ActiveProfilePrefs`) – `:app` steuert die echte Matrix an, kennt keine Avatare und bleibt beim
Standardprofil. `AvatarSpeciesPrefs` in `:app-sim` hält beide Seiten synchron.

## Aufbau

- `glyph/GlyphMatrixService.kt` – Basisklasse (Lifecycle, Touch-Events)
- `glyph/ClockGlyphToyService.kt` – konkreter Toy, zeigt die Uhrzeit
- `glyph/ClockFrame.kt` – zeichnet die Uhrzeit (zweizeilig Std./Min., siehe unten);
  gemeinsam genutzt von Uhr-Toy und Reminder-Service
- `glyph/GlyphMatrixConnection.kt` – trackt, ob die (Prozess-Singleton-)Verbindung von
  `GlyphMatrixManager` gerade besteht (siehe "Bekannte Einschränkung" unten)
- `data/` – Room-DB: `GlyphReminder` (Entity: Bezeichnung, `AnimationType`,
  Wochentags-Bitmaske `daysOfWeekMask`, `startMinuteOfDay`/`endMinuteOfDay`,
  `intervalMinutes`, `enabled`, `nextTriggerEpochMillis` - letzteres merkt sich den
  geplanten nächsten Zeitpunkt und ist die Grundlage für den Watchdog und die
  Auslöse-Reihenfolge, siehe "Erinnerungs-Pipeline" unten),
  `AnimationType` (Enum, 13 Typen - siehe "Erinnerungs-Pipeline" unten fuer die volle
  Liste), `DaysOfWeekMask` (Bitmaske <-> `Set<DayOfWeek>`-Konvertierung),
  `GlyphReminderDao`, `GlyphReminderRepository`, `AppDatabase`
- `ui/GlyphReminderViewModel.kt` – lädt/speichert Erinnerungen (StateFlow aus der
  DB), stößt bei jeder Änderung `ReminderScheduler.schedule`/`cancel` an
- `ui/ReminderScreen.kt` – Compose-UI: Liste aller Erinnerungen (Bezeichnung,
  Animationsname, aktive Wochentage, Zeitfenster, Intervall, Ein/Aus-Switch),
  FAB zum Anlegen, Tap auf eine Karte öffnet den Bearbeiten-Dialog (Bezeichnung,
  Animationstyp mit Test-Button je Typ, Wochentags-Chips, Start-/Endzeit-Picker,
  Intervall-Dropdown, Löschen mit Rückfrage). Kein Einstellungen-Dialog mehr — er enthielt
  nur das Kollisionsverhalten und ist mit ihm entfallen (siehe unten).
- `MainActivity.kt` – zeigt `ReminderScreen`, ruft einmalig
  `ReminderScheduler.rescheduleAll` als Sicherheitsnetz auf

Room-Codegen läuft über KSP automatisch beim Build mit, keine weiteren manuellen
Schritte nötig (nur Gradle-Sync abwarten).

**App-Icon**: `res/drawable/ic_launcher_foreground.xml` zeichnet eine Dot-Matrix-Uhr
(Ring aus 12 Punkten als Stundenmarken + zwei Zeiger aus Punktreihen in 10:10-Stellung,
angelehnt an die echten LED-Punkte der Glyph Matrix) mit einem farbigen Punkt in der
Mitte als Akzent, auf dunklem Hintergrund (`ic_launcher_background.xml`,
`colors.xml`). `ic_launcher_monochrome.xml` liefert dieselbe Punktform als einfarbige
Maske fürs themed Icon (Android 13+, "Icon an Systemfarbe anpassen") - beide sind reine
Vektor-Drawables, keine Raster-Mipmaps nötig (minSdk 34 liegt weit über der fürs
Adaptive-Icon-Format erforderlichen API 26).

**Exakte Alarme**: Ab Android 14 muss die App-Berechtigung "Alarme & Erinnerungen"
manuell erlaubt werden (Systemeinstellung, kein normaler Laufzeit-Dialog). Die App
zeigt dafür oben einen Hinweis-Banner, wenn die Berechtigung fehlt - antippen öffnet
die passende Systemseite.

## Erinnerungs-Pipeline (`reminder/` + Teile von `glyph/`)

- `reminder/ReminderScheduler.kt` – Kernstück des Scheduling: statt eines
  Repeating-Alarms (der weder Wochentagsmuster noch Fenster+Intervall kennt)
  bildet jede Erinnerung eine **Kette** exakter `AlarmManager`-Alarme. Die
  Funktion `nextTrigger(reminder, afterEpochMillis)` durchsucht Fenster-Start-
  Tage von "gestern" bis "in 8 Tagen" (der Vortag fängt den Fall ab, dass ein
  über Mitternacht gehendes Fenster - `endMinuteOfDay < startMinuteOfDay` - noch
  in den heutigen Morgen hineinreicht; der 8-Tage-Puffer garantiert einen
  Treffer auch bei nur einem aktiven Wochentag), generiert pro Tag die Slots
  `windowStart + k*intervalMinutes` und gibt den kleinsten Slot nach
  `afterEpochMillis` zurück. `schedule()` setzt darauf den nächsten Alarm
  (`setExactAndAllowWhileIdle`, PendingIntent-Requestcode = `reminder.id`),
  `cancel()` entfernt ihn wieder, `rescheduleAll()` plant alle aktivierten
  Erinnerungen neu (nach Reboot bzw. als Sicherheitsnetz beim App-Start).

  **Kollisionsverhalten: der Slot wird nie verschoben.** Fallen mehrere Erinnerungen auf
  denselben Moment (z.B. mehrere mit 5-Minuten-Intervall), feuern sie alle zu ihrer echten
  Zeit — nur nacheinander. Die Reihenfolge bestimmt das Intervall: kurze zuerst, längere
  direkt danach, bei Gleichstand die ID (siehe `ReminderTrigger.firePending` in `:app-sim`,
  bzw. die Warteschlange in `ReminderGlyphService` bei `:app`).

  Es gab dafür einmal eine Einstellung („Spread out", `CollisionPrefs`), die kollidierende
  Erinnerungen zeitlich auseinanderzog. **Beides ist gelöscht.** Das Verschieben löste die
  Kollision zwar auf, aber um den Preis, dass eine Erinnerung nicht mehr dann kam, wofür sie
  gestellt war — aus „alle fünf Minuten" wurde faktisch „alle acht". Der Schalter blieb danach
  noch eine Weile in der Oberfläche stehen und schrieb Werte, die niemand mehr las: er sprang
  sichtbar um und bewirkte nichts. Genau deshalb ist er weg — ein Schalter, der lügt, ist
  schlimmer als kein Schalter.
- `reminder/ReminderAlarmReceiver.kt` – vom `AlarmManager` geweckt, lädt die
  aktuelle `GlyphReminder`-Definition frisch aus der DB (damit eine
  zwischenzeitliche Bearbeitung berücksichtigt wird), startet
  `ReminderGlyphService` und plant sofort den nächsten Slot derselben
  Erinnerung neu - das ist die eigentliche Kette.
- `reminder/BootReceiver.kt` – ruft nach einem Neustart `rescheduleAll()` auf
  (AlarmManager-Alarme überleben keinen Reboot)
- `glyph/ReminderGlyphService.kt` – kurzlebiger Foreground-Service (Typ `shortService`,
  max. 3 Min.), verbindet sich unabhängig vom aktuell aktiven Glyph Toy direkt mit der
  Glyph Matrix und spielt die passende Animation fuer eine feste Gesamtdauer
  (`ANIMATION_DURATION_MS`, 6,4 s). Danach automatisch zurueck zur Uhrzeit; letzter
  Frame ist bereits die aktuelle Uhrzeit (kein Schwarzbild-Übergang, siehe unten).

  **`playAnimation()` ist zeitbasiert, nicht zaehlbasiert**: der zu zeigende Frame wird
  aus der tatsaechlich vergangenen Realzeit (`SystemClock.elapsedRealtime()`) berechnet
  (`frameIndex = (elapsed / FRAME_DELAY_MS) % frames.size`, alle `TICK_MS` = 100 ms
  geprueft), nicht aus der Anzahl bereits gemachter `delay()`-Aufrufe. Das hat zwei
  Vorteile: die Anzeigedauer bleibt unabhaengig von der Frameanzahl eines Typs immer
  gleich lang, und falls der Thread einmal kurz ins Stocken geraet (GC-Pause, CPU-
  Drosselung im Doze-Modus o.ae.), "haengt" das Bild dadurch nicht auf einem alten Frame
  fest, bis die Schleife wieder aufholt - der naechste Tick zeigt sofort den fuer die
  aktuelle Zeit richtigen Frame, statt sequenziell nachzuholen.

  **Wichtiger Bugfix - Uhr-Tick ueberschrieb Animationsframes**: `ClockGlyphToyService`
  hat einen eigenen Sekundentakt, der unabhaengig von `ReminderGlyphService` ueber
  denselben `GlyphMatrixManager`-Prozess-Singleton auf dieselbe Matrix zeichnet. Ohne
  Koordination hat dieser Tick spaetestens nach einer Sekunde jeden gerade gesetzten
  Animationsframe wieder mit der Uhrzeit ueberschrieben - dadurch wirkte jede Animation
  so, als wuerde sie sich nur ganz am Anfang bewegen und danach einfrieren/flackern.
  `GlyphMatrixConnection` traegt jetzt zusaetzlich ein `isAnimationPlaying()`-Flag
  (`markAnimationPlaying()`/`markAnimationStopped()`, um die gesamte `drainQueue()`-
  Laufzeit gesetzt): der Uhr-Tick in `ClockGlyphToyService` laesst einen Durchlauf
  einfach aus, solange das Flag gesetzt ist, statt die Matrix zu ueberschreiben.

  **Kollisionen mehrerer Erinnerungen**: Die Glyph Matrix kann nur eine Animation
  gleichzeitig zeigen, aber mehrere Erinnerungen koennen auf denselben oder einen sehr
  nah beieinanderliegenden Slot fallen (z.B. mehrere mit 5-Minuten-Intervall). Statt eine
  noch laufende Animation beim naechsten Trigger abzubrechen (dann waere immer nur die
  zuletzt eingetroffene Erinnerung ueberhaupt sichtbar), landet jede eingehende
  Erinnerung in einer `ConcurrentLinkedQueue` und wird der Reihe nach abgespielt -
  `onStartCommand()` reiht nur ein, `drainQueue()` spielt sie nacheinander ab und
  beendet den Service erst, wenn die Warteschlange leer ist. Direkt aufeinanderfolgende
  Erinnerungen mit demselben Animationstyp werden dabei zusammengefasst (dieselbe
  Animation mehrfach identisch hintereinander zu zeigen braechte keinen zusaetzlichen
  Informationsgehalt) - die Foreground-Notification zeigt dabei die Titel aller
  zusammengefassten Erinnerungen sowie, falls noch mehr warten, "(+N in Warteschlange)".
  Vorzeitiges Beenden ueber einen **"Beenden"-Button in der Foreground-Notification**
  (`ACTION_DISMISS`, `PendingIntent.getService()` zurueck auf denselben Service - Android
  startet dabei nicht neu, sondern ruft `onStartCommand()` der bereits laufenden Instanz
  erneut auf) bricht die aktuell laufende Animation ab (`dismissRequested`-Flag, von
  `playAnimation()` pro Frame geprueft) UND verwirft die restliche Warteschlange.

  **Der Notification-Button ist nicht mehr der einzige Weg dorthin.** Seit Android 13 ist
  `POST_NOTIFICATIONS` eine Laufzeitberechtigung, und ohne sie zeigt Android auch die
  Benachrichtigung eines Vordergrunddienstes nicht an — der Dienst läuft dann, ist aber unsichtbar
  und nicht zu beenden. `glyph/ReminderPlayback.kt` (Prozess-Singleton wie
  `GlyphMatrixConnection` daneben) veröffentlicht deshalb, *was gerade läuft*; `ReminderScreen`
  zeigt daraufhin ein Banner mit demselben „Stop" und schickt dasselbe `ACTION_DISMISS`.
  Dazu erklärt ein zweites Banner die fehlende Berechtigung, **bevor** der Systemdialog kommt
  (er selbst erklärt nichts, und beim zweiten Ablehnen fragt Android nie wieder) — inklusive des
  wichtigen Teils: die Erinnerungen laufen über die Matrix und erscheinen weiterhin, fehlen tut
  nur der Beenden-Knopf. Wer endgültig abgelehnt hat, wird auf Antippen in die
  Systemeinstellungen geführt, statt auf einen Knopf zu drücken, der nichts mehr tut.

  `runCatchingPlayAnimation()` faengt unerwartete Fehler in `playAnimation()` ab
  und springt im Fehlerfall trotzdem zur Uhrzeit, statt die Matrix haengen zu lassen.
  `onTimeout()`-Override als Backstop fuer das harte 3-Minuten-Limit von `shortService`
  (bei realistischer Anzahl Erinnerungen - 12 Animationstypen à ca. 6,4 s sind selbst im
  Extremfall aller dreizehn gleichzeitig nur ~83 s - sollte das nie greifen, ist aber best
  practice bei diesem Service-Typ). Diagnose-Logging (`Log.d`/`Log.w`/`Log.e`, Tags
  `ReminderScheduler`, `ReminderAlarmReceiver`, `ReminderGlyphService`) vorhanden, um
  Probleme per `adb logcat` einzugrenzen.
  **Zwei verworfene Ansaetze** (fuer vorzeitiges Beenden): (1) Handbewegung ueber dem
  `Sensor.TYPE_PROXIMITY`-Naeherungssensor - drei gefundene Bugs in Folge (SensorManager-
  Aufruf ohne Looper crashte die Coroutine, erster Sensor-Messwert ist oft ein veralteter
  Cache-Wert und wurde faelschlich als Geste gewertet, Fix dafuer war dann zu streng und
  erkannte gar keine Gesten mehr je nach Lage des Handys) sowie grundsaetzliche Sensor-/
  Lage-Abhaengigkeit machten das zu fehleranfaellig. (2) Antippen der Glyph-Matrix selbst
  ueber den Toy-Touch-Mechanismus (`onTouchPointPressed()`, offizieller Bestandteil des
  Nothing-SDKs) - kam beim Test nie an, vermutlich weil die Matrix waehrend
  `ReminderGlyphService` laeuft nicht mehr als "Toy-UI" gilt und der System-Touch-Handler
  deshalb gar nicht erst ausgeloest wird. Der Notification-Button umgeht dieses Problem
  komplett, da er auf normaler, gut dokumentierter Android-API beruht statt auf unklarem
  Geraete-/SDK-Verhalten.
- `glyph/ReminderAnimations.kt` – liefert rohe Pixel-Frames (`IntArray`) je `AnimationType`,
  direkt für das 13x13-Raster entworfen (siehe "Matrix-Auflösung" unten). Jeder Typ hat
  **4-6 Frames statt nur 2** (siehe Bugfix-Hinweis oben zu `playAnimation()`) - mit nur 2
  Frames waere es ein reines An/Aus-Blinken, mit mehreren Zwischenstufen ergibt sich
  durchgehend wahrgenommene Bewegung ueber die komplette Anzeigedauer.

  Alle Frames wurden mit einem kleinen Python/Pillow-Skript (nicht Teil des Repos, nur
  zur Entwurfspruefung) als vergroesserte Bitmaps gerendert und visuell durchgesehen - in
  drei Ueberarbeitungsrunden, nachdem erste (und teils sogar zweite) Entwuerfe am
  Bildschirm nicht erkennbar waren: das Glas sah wie eine Fensterscheibe aus, ein Auge
  wie eine Fliege/ein Kreisel/ein Kreuz mit Punkten, die Teetasse mit zwei Dampf-Straengen
  wie ein Hasenkopf, die Meditationsfigur mit schwebendem Kopf wie ein UFO, und das Buch
  brauchte drei Anlaeufe (Domino-Steine → Klammern/Geraet → "V"-Kontur, die immer noch
  zu sehr wie ein generisches Rechteck/eine Box wirkte, kaum von `WORK` unterscheidbar).
  Eine dritte Runde ging dann bewusst auch die bereits als "gut genug" abgehakten Typen
  nochmal einzeln und vergroessert durch (nicht nur die zuvor auffaelligen) und fand dabei
  noch reale Schwaechen: `MOVE`s Zwischenschritt war eine reine T-Pose mit zwei parallelen,
  geraden Beinen (sah aus wie Stillstehen, nicht wie Laufen) - jetzt eine geduckte
  "Flugphase" mit angezogenen Fuessen fuer echten Kontrast zu den ausgestreckten
  Kontakt-Posen; `REST`s Henkel war ein spitzes Dreieck (wirkte wie ein Schnabel/eine
  Pfeilspitze) - jetzt eine offene Schlaufe; `WORK`s Henkel klebte direkt auf der
  Deckelkante (wirkte wie ein Zierstreifen, nicht wie ein Griff) - jetzt eine freischwebende
  Leiste mit sichtbarem Abstand zum Koffer.

  Eine vierte Runde stellte dann drei weitere Typen komplett infrage, nachdem sie beim
  Nutzer trotz technisch funktionierender Animation nicht ueberzeugten: `CREATIVITY` war
  nur eine vage diagonale Punktreihe ohne erkennbare Pinselform; `WORK` war ein
  Aktenkoffer, der sich neben REST/BOOK/etc. kaum als eigenes Motiv abhob (nur ein
  weiteres Rechteck); `SLEEP`s Mond stand die ganze Animation ueber unveraendert still,
  nur die Sterne bewegten sich - wirkte "eindimensional".

  Insgesamt wurden dadurch fuenf Typen grundlegend neu konzipiert statt nur nachjustiert:
  - `FOCUS` wechselte von "blinzelndes Auge" (bei 13x13 nicht ueberzeugend von einem
    Kreis/Kreuz zu unterscheiden) zu einem **Kamera-Fokus-Sucher**: vier feste Eckwinkel
    (Sucherrahmen, bleiben immer an derselben Stelle) umrahmen einen Zielring, der
    durchgehend zwischen drei Groessen pulsiert (Autofokus-Eindruck) - der Rahmen macht
    die "Fokus"-Bedeutung eindeutig, wo ein bloßer Kreis/eine Iris das nicht schaffte.
  - `BOOK` wechselte von "Buch-Silhouette" (egal ob Rechteck, Klammernform oder
    "V"-Kontur, immer zu generisch/verwechselbar) zu einer **stillstehenden Buchseite mit
    zwei sichtbaren Textzeilen** (bleibt die ganze Animation über unveraendert, klar als
    beschriebene Seite erkennbar) plus einem Lese-Merker im linken Rand, der durchgehend
    zwischen den beiden Zeilen hin- und herwandert - liest sich als "wird gerade gelesen".
  - `CREATIVITY` bekam einen tatsaechlich erkennbaren **Pinsel** (fransige Borsten-Spitze
    breiter als der duenne diagonale Stiel, statt gleichfoermiger Punkte) statt einer
    abstrakten Diagonale - der Pinsel gleitet ueber die Leinwand und hinterlaesst einen
    wachsenden Farbstrich direkt unter den Borsten.
  - `WORK` wechselte vom Aktenkoffer zu einem **Laptop** (Bildschirm auf flacher
    Tastatur-Basis - Emoji/Farbe in der UI entsprechend auf 💻/Blaugrau angepasst).
  - `SLEEP`s Mond ist jetzt selbst die Animation: er pulsiert durchgehend zwischen einer
    duennen und einer voll gefuellten Sichel (wie ein Mondphasen-Wechsel), waehrend
    zusaetzlich zwei Sterne daneben "twinkeln" - vorher stand der Mond nur als starres
    Requisit da.

  Eine fuenfte Runde feilte dann an konkreten Feinheiten, die trotz der Neukonzeption noch
  nicht ganz saßen: `WORK`s Bildschirminhalt (eine schlicht wachsende Zeile) wirkte wenig
  kreativ - jetzt zaehlt eine kleine Ziffernanzeige im Bildschirm sichtbar 1 → 2 → 3 → 1
  hoch. `SLEEP`s Sichel war von Hand mit einer linear verjuengten Kante gezeichnet, was
  auf der linken/aeusseren Seite eckig statt rund wirkte - jetzt per Kreisformel berechnet
  (Hauptkreis minus einem versetzten "Biss"-Kreis fuer jede der drei Phasen), was eine
  gleichmaeßig runde Kontur ergibt. `MINDFULNESS`s Schneidersitz war ein massives Dreieck
  ohne erkennbare Beine - jetzt ein sichtbares Beinpaar (Huefte → Knie außen → Fuesse
  zur Mitte gekreuzt) mit einer dunklen Luecke im Schoss dazwischen, dazu ruecken die
  Knie synchron zur Kopf-Aura leicht aus-/einwaerts, sodass die ganze Figur mitatmet statt
  nur der Kopf.

  Eine sechste Runde ging dann noch mal gezielt an drei Typen, die trotz allem beim Nutzer
  nicht ankamen - diesmal mit deutlich mehr Frames und bewusst der vollen Rastergroesse
  statt nur der Mitte: `BOOK` war als stillstehende Seite ueberhaupt nicht als "Lesen"
  erkennbar - komplett neu als **8-Frame-Sequenz**: geschlossenes Buch (schmaler Block,
  Ruecken-Ansicht) → oeffnet sich → voll aufgeklappte Doppelseite mit Falz in der Mitte
  (fast volle Rasterbreite, Spalten 1-11) → Textzeilen auf beiden Seiten "flackern"
  (erscheinen/verschwinden im Wechsel, simuliert Lesefortschritt) → klappt wieder zu →
  Schleife. `WORK`s Bildschirm war nur 4 Zeilen hoch, wodurch sich "2" und "3" nicht
  unterscheiden ließen - Laptop um 1-2 Zeilen nach unten verschoben und vergroeßert
  (Bildschirm jetzt 5 Zeilen hoch, zweistufige Tastatur-Basis mit breiterer Vorderkante bis
  Spalte 11), wodurch alle drei Ziffern eindeutig unterscheidbar werden. `CREATIVITY`s
  gerade Diagonale wirkte beliebig - der Pinsel zeichnet jetzt einen **vollen Kreis** ueber
  **12 Frames** (Radius ~4,6 um die Mitte, nutzt fast das komplette 13x13-Raster, Punkte
  per Kreisformel berechnet), mit einem groesseren pluspoermigen Pinselkopf an der
  aktuellen Position vor der duennen bereits gezeichneten Kontur.

  Alle dreizehn Typen im Detail: `FOCUS` (Kamera-Sucher, Zielring pulsiert klein → mittel →
  groß → mittel), `DRINK` (offenes Glas mit sichtbarem Wasserstand, Tropfen fällt sichtbar
  hinein und erzeugt eine kurze Welle, die sich wieder legt), `MOVE`
  (Strichmännchen-Laufzyklus: ausgestreckte Kontaktpose links → geduckte Flugphase →
  ausgestreckte Kontaktpose rechts → Flugphase), `GENERAL` (Glocke schwingt wie ein
  Pendel: links → Mitte → rechts → Mitte), `REST` (Tasse mit offener Henkel-Schlaufe und
  einer einzelnen, ueber sechs Phasen sanft schlaengelnden Dampfschwade - bewusst nur ein
  Strang statt zwei symmetrischer, die sich sonst wie Hasenohren lesen), `WORK` (großer
  Laptop - Bildschirm 5 Zeilen hoch auf zweistufiger Tastatur-Basis -, eine Ziffer im
  Bildschirm zaehlt sichtbar 1 → 2 → 3 → 1 hoch), `MINDFULNESS` (sitzende
  Meditationsfigur mit sichtbarem Schneidersitz - Knie außen, Fuesse zur Mitte gekreuzt -
  bleibt an derselben Stelle, Knie und eine kleine Aura ueber dem Kopf pulsieren synchron
  durchgehend mit), `LOVE` (Herz im "lub-dub"-Herzschlag-Rhythmus: groß → klein → mittel
  → klein), `SLEEP` (Mond pulsiert durchgehend zwischen duenner und voll gefuellter
  Sichel wie ein Mondphasen-Wechsel, zwei Sterne "twinkeln" daneben durch vier
  verschiedene Positionen), `MEDICINE` (medizinisches Kreuz pulsiert winzig → klein →
  groß → klein), `BOOK` (8 Frames: geschlossenes Buch → oeffnet sich → volle Doppelseite
  → Textzeilen auf beiden Seiten flackern → schließt sich wieder) und `CREATIVITY`
  (12 Frames: ein Pinsel zeichnet einen vollen Kreis ueber fast das ganze Raster, mit
  groesserem Pinselkopf an der aktuellen Position vor der duennen bereits gezeichneten
  Kontur).

**Matrix-Auflösung**: Laut offizieller Spezifikation (`GlyphMatrix-Developer-Kit`-Repo,
`image/25111_specification.svg`) hat das (4a) Pro nur ein **13x13-Raster**
(`Common.getDeviceMatrixLength()`), das Phone (3) dagegen 25x25. Die ersten Animationen
waren als 108dp-Vektor-Icons gebaut und über `GlyphMatrixUtils.drawableToBitmap()` skaliert
- auf 13x13 heruntergerechnet wurden feine Linien praktisch unlesbar ("hässlich" im Test).
`ReminderAnimations.kt` baut die Frames jetzt direkt als grobe Pixel-Koordinaten fürs
13x13-Raster statt als skalierte Bitmaps.

**Bekannte Einschränkung (gelöst)**: `GlyphMatrixManager.getInstance()` ist laut Bytecode
ein Prozess-Singleton mit nur einer `ServiceConnection`. Ein zweiter `init()`-Aufruf auf
eine bereits verbundene Connection löst `onServiceConnected` nicht erneut aus (Android
bindet dann nur den Ref-Count hoch) - ohne Gegenmaßnahme wäre z. B. der zweite Testklick
auf "Animation testen" wirkungslos verpufft, weil `ReminderGlyphService` auf einen
Callback wartet, der nie mehr kommt. `glyph/GlyphMatrixConnection.kt` trackt den
Verbindungsstatus jetzt geteilt zwischen `ClockGlyphToyService`/`GlyphMatrixService` und
`ReminderGlyphService`: ist schon verbunden, wird direkt registriert/animiert statt auf
den Callback zu warten.

**Helligkeit**: `setMatrixFrame(int[])` reicht Werte direkt an die Hardware durch, deren
Skala geht aber 0..4095, nicht 0..255 (per Bytecode verifiziert: `GlyphMatrixUtils`
rechnet `nativeWert = 4095 * brightness(0..255) / 255`). `ReminderAnimations.kt` setzt
aktive Pixel deshalb auf 4095, nicht 255.

**DB-Migration**: Datenmodell wurde komplett von terminbasiert (`Termin`,
`RecurringTermin`, `AnimationRule`) auf das einfache `GlyphReminder`-Schema umgestellt -
`AppDatabase` ist deshalb auf Version 8 (zuletzt: `nextTriggerEpochMillis`-Spalte fürs
Kollisions-Scheduling) mit `fallbackToDestructiveMigration(true)` gestiegen. Für dieses
Nebenprojekt im aktiven Aufbau ok (bisherige lokale Daten gehen beim ersten Start nach
dem Update verloren), für einen Produktiv-Einsatz mit echten Nutzerdaten bräuchte es
eine echte `Migration`.

## Play Mode (`:app-sim`)

Ein Spielmodus je Avatar: **der Nutzer richtet hier nichts ein.** Statt selbst gestellter
Erinnerungen feuert eine einzelne, von der App verwaltete Erinnerung zu zufälligen Zeitpunkten,
deren Thema und Intervall zum Charakter des jeweiligen Avatars passen. Gefüttert wird sie wie
jede andere (Uhr auf den Avatar ziehen); jede Fütterung zahlt auf XP/Level ein, der Avatar
entwickelt sich also über die Zeit.

**Der Spielmodus ist kein eigener Bildschirm, sondern ein globaler Schalter** – das ist der
entscheidende Punkt. Ein Textbutton in der oberen Leiste des Startbildschirms, direkt neben
„Erinnerungen", schaltet ihn an und aus. Sonst ändert sich **nichts**: Startbildschirm, Dock-Modus
und Home-Screen-Widget sehen aus und funktionieren wie immer, es kommt nur etwas anderes an. Man
spielt also überall dort, wo man die App ohnehin benutzt.

`PlayModeState` (im Kern) hält den Schalter, `PlayModePrefs` (in `:app-sim`) legt den beobachtbaren
Zustand darüber – dieselbe Aufteilung wie bei `ActiveProfilePrefs`/`AvatarSpeciesPrefs` und aus
demselben Grund: Scheduler und Alarm-Empfänger fragen ihn auch aus kalt gestarteten Prozessen ab.

**Beide Modi schließen sich aus.** Genau eine Frage entscheidet das: welche Erinnerungen überhaupt
eingeplant und ausgelöst werden. Gefiltert wird an vier Stellen über
`PlayModeState.matchesCurrentMode` – `ReminderScheduler.schedule()`, `ReminderTrigger.firePending()`
und `.fireFromAlarm()` sowie im `ReminderWatchdogWorker`. Liefen beide Sätze gleichzeitig,
überlagerten sich die sorgfältig eingestellten echten Erinnerungen mit einem Spiel-Takt von wenigen
Minuten; die eigentliche Aufgabe der App ginge im Spiel unter und das Pflegebuch wäre nicht mehr zu
lesen. Der Filter im Watchdog ist dabei kein Beiwerk: ohne ihn fiele der jeweils ruhende Satz bei
jedem Lauf als „kein Alarm hinterlegt" auf und würde endlos vergeblich nachgeplant.

Die eigenen Erinnerungen werden dabei **nicht angefasst** – sie behalten ihr `enabled`, sind also
in dem Moment wieder da, in dem man das Spiel ausschaltet. Genau das sagt die Erklärung beim ersten
Einschalten auch ausdrücklich (`PlayModeIntroDialog`, in derselben Sprechblasen-Form wie der
Avatar-Assistent): Der Schalter ändert lautlos die Grundlage der App, und ohne ein Wort dazu wäre
der erste Eindruck, die eigenen Erinnerungen seien verschwunden.

**Die App startet immer im Normalbetrieb** – `GlyphSimApp.onCreate()` setzt den Schalter beim
Prozessstart zurück. Der Spielmodus ist eine Sitzung, die man bewusst beginnt, kein Zustand, in dem
man das Gerät aus der Hand legt; bliebe er über Nacht an, fühlten sich die eigenen Erinnerungen am
nächsten Tag grundlos verschwunden an. Der Fortschritt hängt nicht daran und bleibt erhalten.

**Kein zweites Timer-System.** Play Mode nutzt die bestehende Pipeline (`ReminderScheduler` →
`AlarmManager`/In-Prozess-Takt → `ReminderTrigger` → `AvatarFeedEvent`) unverändert weiter. Es
ändert sich nur, WAS als Nächstes gilt, nicht WIE es feuert oder beantwortet wird – Nachhol-Fenster,
Mindestabstand, Kollisions-Reihenfolge und Widget-Anzeige gelten damit automatisch mit. Dass die
Spiel-Animationen auch im Widget und im Dock-Modus erscheinen und dort gefüttert XP geben, ist
deshalb kein Zusatzaufwand, sondern ergibt sich von selbst.

**Wo die Spiel-Erinnerung bewusst ausgeblendet wird**, weil sie keine Einstellung des Nutzers ist:
in der bearbeitbaren Liste (`GlyphReminderViewModel.reminders`), bei den Zählungen für den
Kopieren-Dialog und beim Kopieren selbst (`GlyphReminderRepository.copyProfile` überspringt sie in
beide Richtungen – mitkopiert hätte das Zielprofil sonst zwei davon, die unabhängig voneinander
weiterwürfeln).

**Getrennte Buchführung.** `AvatarFeedEvent.isPlayMode` trennt Spiel- von Alltags-Fütterungen, und
die Auswertungen fragen gezielt nach einem der beiden Sätze:

- Die Symbolleiste oben im Startbildschirm zeigt immer **den Modus, in dem man gerade ist**, und
  wechselt mit ihm. Vorher stand beides vermischt da – eigene Tagesziele neben zufälligen
  Spiel-Fütterungen –, eine Zeile, die dadurch über keines von beidem Auskunft gab.
- Im Spielmodus wird nach **Thema** gruppiert (`observePlayFedPerTypeSince`), nicht nach
  Erinnerung: Es gibt dort nur eine einzige Erinnerungs-Zeile, deren Thema ständig wechselt – nach
  `reminderId` gruppiert ergäbe das einen einzigen Eintrag mit dem gerade zufälligen Symbol.
- Tagesziele und der Rhythmus-Vorschlag im Pflegebuch lesen ausschließlich `isPlayMode = false`.
  Ein Spielstand darf ein echtes Tagesziel weder erfüllen noch beschönigen, und der Rat zu den
  eigenen Erinnerungen darf nicht auf Spielzahlen beruhen.

Level und XP stehen **nur im Dock-Modus** (unten mittig – am oberen Rand lagen sie auf Geräten mit
Kamera-Ausschnitt teils darunter). Im Startbildschirm stünden sie nur im Weg; er ist der Ort zum
Einrichten und Nachsehen, nicht die Spielfläche.

Die Umsetzung besteht aus drei Teilen:

- **Markierung:** `GlyphReminder.isPlayMode` kennzeichnet die eine Zeile je Avatar-Profil. Sie
  läuft immer mit `dailyGoal = NO_GOAL` und geht dadurch nie in die Stimmungsberechnung ein
  (`AvatarMoodSnapshot` filtert ohnehin auf `dailyGoal > 0`) – der Spielmodus kann den Avatar
  also nicht trüben. `GlyphReminderDao.countForProfile` schließt sie aus, sonst hielte
  `seedIfEmpty` einen frischen Avatar für schon eingerichtet und legte nie die
  Standard-Erinnerungen an.
- **Würfeln nach Spielplan:** `matrix/PlayGamePlan.kt` gibt je Avatar vor, was überhaupt in Frage
  kommt und wie wahrscheinlich es ist; `matrix/PlayModeRoll.kt` würfelt darin. Eingehängt über
  `ReminderHost.reroll` und von `ReminderScheduler.schedule()` für jede Play-Mode-Zeile aufgerufen –
  also genau dann, wenn die Erinnerung gerade gefeuert hat und den nächsten Slot braucht.

  **Warum ein Plan statt reinem Zufall:** Vorher waren es 70 % Signature-Thema und sonst
  Gleichverteilung. Das ist abwechslungsreich, erzählt aber nichts – jeder Avatar fühlte sich nach
  denselben zwölf Themen an, nur mit anderem Schwerpunkt. Der Plan macht daraus eine Handschrift:
  Hootlet setzt Ruhe und Bücher vor, Wyrmling treibt an, Gloop entschleunigt. Der Zufall bleibt, er
  bekommt nur einen Rahmen – es ist ein Wahrscheinlichkeitsraum, kein Drehbuch.

  **Stufen nach Level** (`PlayStage.fromLevel`, aktuell 1 / 3 / 5) sind die Stelle, an der sich die
  Entwicklung tatsächlich zeigt, ohne dass dafür neue Pixel-Art nötig wäre: Anfangs ist ein Avatar
  auf sein eigenes Thema konzentriert, mit steigendem Level öffnet er sich – Wyrmling lernt zur
  Bewegung die Erholung dazu, Starlet neben der Achtsamkeit das Lesen. Der Fortschritt ändert also,
  WAS man erlebt, nicht nur eine Zahl. Weil das Level dafür bekannt sein muss, ist der
  Reroll-Hook `suspend` (er liest `AvatarPlayState`).

  `MEDICINE` kommt in **keinem** Plan vor – eine zufällige Spiel-Auslösung darf nie wie eine echte
  Medikamenten-Erinnerung wirken.

  **Das Intervall ist Spieltempo, nicht Alltagstempo** (`PlayStage.intervalMinutes`): 2–16 Minuten
  statt der 15–180 aus `ReminderRhythm`, je Avatar und Stufe verschieden (Fennec hat bewusst die
  engste Spanne – „erinnert ohne je zu nerven" heißt vorhersehbar; Gloop die weiteste und
  langsamste). `ReminderRhythm` beantwortet „wie oft soll mich das im Alltag anstupsen" – wer den
  Spielmodus startet, sitzt davor und will spielen. Eine Dreiviertelstunde bis zum ersten Ereignis
  liest sich nicht als seltener Charakter, sondern als kaputte Funktion; genau das war der erste
  Eindruck. Aus demselben Grund zieht `PlayModeViewModel.pullFirstTriggerForward` den allerersten
  Slot auf die nächste volle Minute vor, statt ihn auf dem regulären Raster liegen zu lassen.
- **Fortschritt:** `AvatarPlayState` (eine Zeile je Profil: `xp`, `startedAtMillis`,
  `lastSeenLevel`). `ReminderTrigger.show()` schreibt `isPlayMode` ins `AvatarFeedEvent` mit,
  `AvatarFeeding.logFeedEvent()` vergibt daraufhin XP (`ui/PlayModeXp.kt`). Das Level wird
  **immer** aus `xp` abgeleitet und nie gespeichert, damit beide nicht auseinanderlaufen können;
  `lastSeenLevel` merkt sich nur, bis wohin der Avatar schon gratuliert hat.

`ui/PlayModeViewModel.kt` hält den Zustand je gerade gewähltem Avatar (`started`, `xp`, `level`,
`showLevelUp`); `setActive(Boolean)` ist der eine Umschalter: Es legt beim Einschalten Erinnerung
und Fortschrittszeile an (falls noch nicht vorhanden), setzt den Modus und ruft
`ReminderScheduler.rescheduleAll` – das bestellt alle stehenden Alarme ab und plant nur noch den
Satz des neuen Modus. Der Level-Stand steht während des Spiels über dem Avatar im Startbildschirm,
bei einem Aufstieg für einige Sekunden als Glückwunsch.

### Signatur-Animationen und -Reaktionen

Je fünf Animationen pro Avatar, aus dem Charakter abgeleitet – `data/AvatarSignatureAnimations.kt`
(die Zeichnungen, 13×13, als Punktdaten in der Bibliothek) und
`matrix/AvatarSignatureReactions.kt` (die Antwort des Avatars darauf, 16×16).

Die 26 allgemeinen Animationen sind bewusst neutral – Stern, Welle, Rakete passen zu jeder
Erinnerung und zu keinem Avatar besonders. Dieser Satz gehört dagegen jeweils **einer** Figur:
Fennecs Leuchtturm und Anker stehen für den verlässlichen Beschützer, Gloops Schildkröte und Wolke
für den Entschleuniger, Hootlets Sanduhr und Schlüssel für Geduld und Erkenntnis.

Dazu je eine **eigene Reaktion** statt der gemeinsamen Freuden-Choreografie: Wyrmling zuckt beim
Blitz zusammen und geht dann erst recht hoch, Gloop schwappt der Wolke hinterher, Hootlet blinzelt
mit dem fremden Auge im Takt, Fennec legt sich schützend über das Nest (die einzige Reaktion, die
nach unten endet statt nach oben). Der Reiz liegt darin, dass die Figur auf *dieses* Motiv
antwortet und nicht generisch jubelt.

Beides ist **gestalterisch** zugeordnet, nicht technisch gesperrt: Jede Animation lässt sich für
jeden Avatar auswählen. Deshalb verlässt sich keine Choreografie auf Schwanz oder Füße (die haben
nicht alle sechs) – gearbeitet wird mit Verschiebung, `AvatarBody.accent`, Blick, Mund und
Requisiten. `AvatarAnimationsTest` fährt alle 30 Reaktionen gegen **alle sechs** Spezies, genau
darum.

Drei Fallen, in die das Zeichnen gelaufen ist und die die Tests jetzt abfangen:

- **Die Matrix ist rund.** Nur der einbeschriebene Kreis wird angezeigt; ein bis in die Ecken
  gezogener Berghang verlor dort 18 % seiner Punkte und franste aus (`LibraryAnimationFitTest`).
- **Punkte auf einer Linie verschwinden in ihr.** Der Kletterer am Summit lag exakt auf der
  Hangkante – die Frames waren dadurch identisch, die Figur unsichtbar.
- **Gefüllte Flächen verschmelzen.** Puzzleteil und Rahmen wurden zu einem Klotz, in dem die Naht –
  also das Motiv – nicht mehr zu sehen war; Umrisse statt Füllung lösen das.

`AvatarSignatureAnimationsTest.renderSample` gibt die Raster als ASCII aus – Schnecke, Schildkröte
und Auge haben genau darüber ihre jetzige Form gefunden, nachdem sie als unlesbare Klumpen
begonnen hatten.

**Was bewusst noch fehlt** (Grundstein, mehr nicht): Die Entwicklung ist weiterhin nur Datenmodell
plus Level-Anzeige. Es gibt **keine „Specials"** – ein Levelaufstieg schaltet keine zusätzliche
Animation, keine neue Silhouette und keine neue Bewegung frei; die Signatur-Animationen stehen von
Anfang an alle zur Verfügung. Die Avatare selbst sind fest gezeichnete Pixel-Art
(`matrix/AvatarBody.kt`); zusätzliche Körperformen sind Gestaltungsarbeit und kein Nebenprodukt.
Ebenso wenig schaltet der Spielfortschritt echte Erinnerungen frei.

**Datenbank:** `:app-sim` steht dadurch auf Version 19 (16 → 17 `glyph_reminders.isPlayMode`,
17 → 18 `avatar_feed_events.isPlayMode`, 18 → 19 neue Tabelle `avatar_play_state`) – alle drei als
echte `Migration` in `data/AppDatabaseMigrations.kt`, bestehende Daten bleiben erhalten. Geprüft
wird das in `AppDatabaseMigrationTest` (instrumentiert, braucht ein Gerät:
`./gradlew :app-sim:connectedDebugAndroidTest`).

## Werkzeugkette

| | Wert | Anmerkung |
|---|---|---|
| **JDK für den Build** | **mindestens 17** | AGP 8.13 lädt Klassen im Format 61. Der Wurzel-`build.gradle.kts` prüft das und bricht sonst mit Klartext ab statt mit einer `UnsupportedClassVersionError` aus dem Plugin-Ladevorgang. Hier tatsächlich benutzt: JDK 21, Android Studios gebündeltes JBR. |
| **Bytecode-Ziel** | **Java 11** | `compileOptions`/`jvmTarget` in allen drei Modulen. Das ist etwas völlig anderes als die Zeile darüber: es richtet sich nach `minSdk` (26) und dem, was ART ohne aufwendiges Desugaring versteht — nicht nach dem JDK des Entwicklers. Die beiden Zahlen dürfen und sollen auseinanderliegen. |
| Gradle | 8.13 | Wrapper ist eingecheckt, nichts zu installieren |
| AGP | 8.13.1 | |
| Kotlin / KSP | 2.2.20 | |
| Compose BOM | 2026.06.01 | |
| Room | 2.8.4 | |
| `compileSdk` / `targetSdk` | 35 | |
| `minSdk` | 26 (`:core`, `:app-sim`) · 34 (`:app`) | `:app` folgt dem Glyph Matrix SDK, das API 34 voraussetzt |

`local.properties` (mit `sdk.dir`) ist bewusst nicht eingecheckt (steht in `.gitignore`) – beim
ersten Öffnen in Android Studio legt die IDE sie automatisch an.

Unter Windows/PowerShell, falls `JAVA_HOME` nicht gesetzt ist:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

## Persistenz-Landkarte

Aufgenommen in Phase 1d des Produktumbaus, weil jede der geplanten Änderungen (gemeinsamer
Routinen-Satz, Pausen statt Modi, Kapitel statt Level) an genau diesen Ablagen hängt. **Datei- und
Schlüsselnamen sind der einzige Bezug zu dem, was auf Geräten liegt** — eine Umbenennung setzt die
Einstellung stillschweigend zurück. `SettingsCatalogTest` hält jeden Namen fest.

### Room

Zwei getrennte Datenbanken, beide auf Version 19. Der gemeinsame Kern liefert die Entities, die
`@Database`-Klasse bleibt pro App (siehe „Module").

| Tabelle | In | Schlüssel | Bemerkung |
|---|---|---|---|
| `glyph_reminders` | beide | `id` (auto) | `profileId` bindet an den Avatar, `isPlayMode` markiert die eine vom Spielmodus verwaltete Zeile |
| `library_animations` | beide | `id` (auto) | Abgleich der mitgelieferten über `label` |
| `builtin_animation_selection` | beide | `animationType` | An/Aus je eingebautem Typ |
| `avatar_feed_events` | nur `:app-sim` | `id` (auto) | Entsteht beim **Auslösen**; `fedAtMillis` erst bei Reaktion |
| `avatar_play_state` | nur `:app-sim` | `profileId` | `xp`, `lastSeenLevel`, `startedAtMillis` |

Beziehungen bestehen **ohne Fremdschlüssel**: `avatar_feed_events.reminderId` zeigt auf
`glyph_reminders.id`, `profileId` verbindet Erinnerungen, Ereignisse und Spielstand. Eine gelöschte
Erinnerung lässt ihre Ereignisse also stehen — gewollt, das Pflegebuch soll nicht rückwirkend
schrumpfen.

### SharedPreferences

Sechzehn Einträge in dreizehn Dateien, vollständig in
`app-sim/.../settings/SettingsCatalog.kt`. Acht laufen bereits über `SettingsStore`, die übrigen
lesen noch direkt — der Katalog dokumentiert sie trotzdem.

Zwei Besonderheiten:

- **`play_mode_state`** liegt als einzige im gemeinsamen Kern. Alarm-Empfänger und Planer müssen
  sie aus kalt gestarteten Prozessen lesen, in denen die App-Oberfläche nie lief.
- **`rhythm_suggestion_prefs`** steht *nicht* im Katalog: sie legt je Erinnerung und Art einen
  eigenen Schlüssel an (`key(reminderId, kind)`). Eine Familie dynamischer Schlüssel lässt sich
  nicht als Liste führen.

### Prozessweite Zwischenspeicher — Vorsicht

Drei Objekte halten ihren Wert zusätzlich in einem `MutableStateFlow`, der beim ersten Lesen
befüllt und **nie wieder aufgefrischt** wird: `PlayModePrefs`, `WatchModePrefs`,
`AvatarSpeciesPrefs`.

Wer am Objekt vorbei direkt in die Ablage schreibt — etwa über `PlayModeState` aus dem Kern —,
ändert die Datei, aber nicht das, was die Oberfläche anzeigt. Heute fällt das nicht auf, weil nur
ein Weg schreibt; beim Einführen des Pausen-Zustands wäre es ein Fehler, den niemand sucht.
Festgehalten in `ModeTransitionCharacterizationTest`.

### Dateien im App-Speicher

| Pfad | Inhalt | In der Sicherung? |
|---|---|---|
| `files/clips/` | Aufgenommene Filme (`.mp4`), unfertige als `.part` | nein |
| `files/shots/` | Standbilder (`.png`) | nein |
| `files/last-crash.txt` | Letzter Absturzbericht, auf 64 KB gekappt | nein |

## Prüfen

Ein Befehl deckt alles ab, was ohne angeschlossenes Gerät prüfbar ist:

```
.\gradlew.bat verify
```

Dahinter stecken Unit-Tests, Lint und ein minifizierter Probe-Bau beider Apps (alle drei Module).
**Die CI ruft genau diesen Task auf** — damit sind „was prüfe ich vor dem Einchecken" und „was
prüft die CI" per Konstruktion dasselbe, statt zwei Listen, die auseinanderlaufen.

Was dort bewusst *nicht* drin ist, weil es ein Gerät braucht:

```
.\gradlew.bat :app-sim:connectedDebugAndroidTest   # Room-Migrationen + DAO-Transaktionen
.\gradlew.bat :app:connectedDebugAndroidTest       # Room-Migrationen
```

**Emulator dafür anlegen** (einmalig; `:app` braucht API ≥ 34 wegen des Glyph Matrix SDK, `aosp_atd`
ist das schlanke, für Testläufe gedachte Abbild):

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" "system-images;android-35;aosp_atd;x86_64"
& "$sdk\cmdline-tools\latest\bin\avdmanager.bat" create avd -n tama-test `
    -k "system-images;android-35;aosp_atd;x86_64" -d pixel_6
& "$sdk\emulator\emulator.exe" -avd tama-test -no-window -no-audio -no-boot-anim
```

Zwei Stolpersteine, beide schon behoben, aber gut zu wissen:

- **`--offline` funktioniert hier nicht.** Die Unified Test Platform lädt beim ersten Lauf eigene
  Artefakte nach; mit `--offline` bricht der Task ab, bevor irgendetwas läuft.
- **Schlägt der Gradle-Task mit „Failed to receive the UTP test results" fehl**, ohne dass ein Test
  gelaufen ist, führt der direkte Weg schneller zum Ergebnis — und zu einer brauchbaren
  Fehlermeldung:

  ```
  adb install -r -t app-sim\build\outputs\apk\debug\app-sim-debug.apk
  adb install -r -t app-sim\build\outputs\apk\androidTest\debug\app-sim-debug-androidTest.apk
  adb shell am instrument -w com.notime.glyphminderwatch.test/androidx.test.runner.AndroidJUnitRunner
  ```

### Die drei Release-Varianten auseinanderhalten

| Aufruf | Ergebnis | Wofür |
|---|---|---|
| `assembleDebug` | Debug-signiert, nicht minifiziert | Entwickeln |
| `assembleReleaseCheck` | **Debug**-signiert, **minifiziert** | R8 prüfen und auf dem Gerät durchspielen, ohne Upload-Schlüssel. Eigene `applicationId` (`…​.releasecheck`), lässt sich also parallel zur Debug-Fassung installieren. |
| `assembleRelease` / `bundleRelease` | Release-signiert, minifiziert | Das echte Paket. **Scheitert absichtlich**, wenn Signaturdaten, Room-Schema-Export, Versionsangaben oder (bei `:app`) der NothingKey nicht stimmen — siehe `validateRelease`. |

Früher lieferte `assembleRelease` ohne `keystore.properties` stillschweigend ein *unsigniertes*
Paket: gleicher Ordner, gleicher Dateiname, wertlos, und man sieht es ihm nicht an. Genau das
trennt `releaseCheck` jetzt sauber ab.

Die Prüfung einzeln, ohne etwas zu bauen:

```
.\gradlew.bat :app-sim:validateRelease
```

### Vor dem Veröffentlichen

`versionCode` und `versionName` stehen als benannte Werte oben in
`app-sim/build.gradle.kts`. **`versionCode` muss bei jedem Upload höher sein als beim
vorherigen** — der Play Store lehnt eine bereits verwendete Nummer ab, und nachträglich ändern
lässt sie sich nicht. Der Rest der Store-Checkliste steht in `PLAY_STORE.md`.

## Build & Installation

Debug-APK selbst bauen (PowerShell, aus dem Projektordner):

```
.\gradlew.bat assembleDebug
```

Ergebnis liegt danach unter `app\build\outputs\apk\debug\app-debug.apk` (Debug-signiert,
sofort installierbar, kein Release-Key nötig).

**Installieren:**
- Per USB mit aktiviertem USB-Debugging: `adb install -r app\build\outputs\apk\debug\app-debug.apk`
  (adb liegt im Android SDK unter `platform-tools`)
- Oder die APK-Datei aufs Handy kopieren (z. B. per Kabel, Google Drive, E-Mail) und dort
  antippen – dafür einmalig "Installation aus unbekannten Quellen" für den jeweiligen
  Dateimanager/Browser erlauben.

Beim ersten Durchlauf hier mussten drei reale Fehler behoben werden (nicht nur
Versions-Wünsche, sondern echte Kompatibilitätsprobleme):
- Room 2.6.1 + aktuelles Kotlin/KSP: `unexpected jvm signature V` beim Annotation-Processing
  → auf Room 2.8.4 aktualisiert.
- `import androidx.compose.foundation.layout.weight` war falsch/überflüssig und hat eine
  interne, gleichnamige Property verdeckt → Import entfernt (Row/Column-Scope löst
  `.weight()` automatisch über den impliziten Receiver auf, kein Import nötig).
- `Icons.Default.*` war nicht auflösbar, weil `material-icons-core` entgegen meiner
  ursprünglichen Annahme nicht transitiv über `material3` mitkommt → explizit als
  Abhängigkeit ergänzt.

## Nächste Schritte

- Vorzeitiges Beenden ist jetzt als "Beenden"-Button in der Foreground-Notification
  umgesetzt (siehe "Erinnerungs-Pipeline" oben), nach zwei verworfenen Ansätzen
  (Näherungssensor-Geste, Antippen der Glyph-Matrix). Auf dem echten Gerät noch testen.
- Erinnerungen bearbeiten ist umgesetzt (Bezeichnung, Animationstyp, Wochentage,
  Zeitfenster, Intervall alles nachträglich änderbar) - noch auf dem echten Gerät
  testen, insbesondere Erinnerungen mit über Mitternacht gehendem Fenster.
- Echte Room-`Migration` statt `fallbackToDestructiveMigration`, sobald echte
  Nutzerdaten erhalten bleiben sollen.
- Kollisionen mehrerer gleichzeitig fälliger Erinnerungen werden jetzt über eine
  Warteschlange in `ReminderGlyphService` sequenziell abgespielt (siehe
  "Erinnerungs-Pipeline" oben) statt sich gegenseitig abzubrechen - auf dem echten
  Gerät noch mit mehreren eng getakteten Erinnerungen (z.B. alle 5 Minuten) testen.
- **Play Mode ist bisher nur kompiliert, nicht gelaufen** (siehe "Play Mode" oben). Auf einem
  Gerät/Emulator noch durchspielen: umschalten, auf die erste Auslösung warten (soll innerhalb
  einer Minute kommen), füttern, Level/XP über dem Avatar nachsehen. Vor allem aber den
  **Moduswechsel in beide Richtungen** prüfen - dass im Spiel wirklich keine eigene Erinnerung mehr
  kommt und dass nach dem Ausschalten alle eigenen wieder eingeplant sind (`adb logcat -s
  ReminderScheduler`). Ebenso, dass die Spiel-Animation im Widget und im Dock-Modus ankommt und
  dort gefüttert XP gibt. Die drei neuen Migrationen laufen im selben Zug mit
  (`./gradlew :app-sim:connectedDebugAndroidTest`).
- **Noch ungeprüft: was ein Moduswechsel mit einer gerade offenen Erinnerung macht.** Wird während
  einer laufenden Animation umgeschaltet, ist deren `AvatarFeedEvent` bereits geschrieben; ob sie
  sich danach noch sinnvoll füttern lässt (und ob das XP geben soll), ist nicht durchdacht.
