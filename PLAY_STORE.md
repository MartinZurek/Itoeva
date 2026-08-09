# Play-Store-Einreichung

Sammelstelle für alles, was beim Veröffentlichen ausserhalb des Codes zu erledigen ist.

## Exakte Alarme: geprüft und bewusst nicht genutzt

**Für Tama (`:app-sim`) ist hier nichts zu tun.** Die App deklariert weder
`SCHEDULE_EXACT_ALARM` noch `USE_EXACT_ALARM`. Damit entfällt das *Permissions Declaration Form*
samt Video — und mit ihm das Risiko, dass die Veröffentlichung an einer Prüferentscheidung hängt.

Diese Notiz existiert, damit die Frage nicht in ein paar Monaten erneut aufgeworfen wird.

### Warum die App ohne auskommt

Exakte Alarme lösen genau ein Problem: *„das Gerät döst, soll aber trotzdem minutengenau
aufwachen."* Das braucht Tama nicht. Erinnerungen erscheinen rein visuell — auf Dock,
Startbildschirm und Home-Screen-Widget. Alle drei sind nur bei **eingeschaltetem Display**
sichtbar, und solange das Display an ist, greift Doze ohnehin nicht; ungefähre Alarme kommen dann
nah genug am gewünschten Zeitpunkt.

Konsequent zu Ende gedacht heisst das: Bei dunklem Bildschirm löst `ReminderAlarmReceiver` gar
nichts erst aus — keine Animation und **kein Eintrag in der Auswertung**. Eine Auslösung, die
niemand sehen konnte, als „übergangen" zu zählen, würde das Pflegebuch verfälschen und die
Stimmung des Avatars ohne Zutun des Nutzers drücken. Der Alarm wird dabei trotzdem neu geplant,
sonst risse die Kette beim ersten dunklen Bildschirm ab.

`ReminderScheduler` in `:core` brauchte dafür keine Änderung: ohne den Manifest-Eintrag meldet
`canScheduleExact()` ab API 31 von selbst `false`, und der Scheduler nimmt `setAndAllowWhileIdle`.

### Warum nicht `USE_EXACT_ALARM`

Die wird automatisch erteilt und bräuchte kein Formular — Google lässt sie aber nur für Wecker-
und Kalender-Apps zu und blockiert andernfalls die Veröffentlichung **komplett**. Tama wäre ein
Grenzfall (Erinnerungen ja, aber ambient-visuell statt weckend). Das Risiko ist einseitig: bei
`SCHEDULE_EXACT_ALARM` degradiert die App im schlimmsten Fall, bei `USE_EXACT_ALARM` fliegt sie
raus. Da beide ohnehin verzichtbar sind, erübrigt sich die Abwägung.

Dazu käme ein praktisches Hindernis: `minSdk` ist 26, `USE_EXACT_ALARM` gibt es erst ab API 33.
Für API 31–32 bräuchte es weiterhin `SCHEDULE_EXACT_ALARM` mit `android:maxSdkVersion="32"` — die
Berechtigung verschwände also gar nicht aus dem Manifest.

### Was das für die Genauigkeit heisst

| Intervall | Ohne exakte Alarme |
|---|---|
| 1, 5, 10 min | Im Doze nicht einhaltbar (System drosselt auf grob 9–15 min) — bei **eingeschaltetem** Display unkritisch, und nur dann löst die App aus |
| ab 30 min | Praktisch unbeeinträchtigt |

Der minütliche Tick des Widgets (`GlyphClockWidgetProvider.scheduleNextTick`) läuft ebenfalls
ungefähr. Sichtbar ist das Widget nur bei eingeschaltetem Display — dann greift Doze nicht.

### Der `:app`-Nachbar ist davon nicht betroffen

`:app` (Glyph Kalender, `com.notime.glyphkalender`) deklariert `SCHEDULE_EXACT_ALARM`
weiterhin — dort ist sie begründet: Die Glyph-Matrix sitzt auf der Geräterückseite und leuchtet
auch bei dunklem Display. **Wer dieses Modul veröffentlicht, braucht das Declaration Form samt
Video sehr wohl.** Die Argumentation dafür ist die Sichtbarkeit bei ausgeschaltetem Schirm, die
Tama gerade nicht hat.

## Datensicherheits-Formular (Data Safety)

### Die Antworten

| Frage | Antwort |
|---|---|
| Erhebt oder teilt deine App die geforderten Nutzerdatentypen? | **Nein** |
| Werden Daten verschlüsselt übertragen? | entfällt (keine Übertragung) |
| Können Nutzer Löschung ihrer Daten beantragen? | entfällt — alles liegt lokal, Deinstallieren entfernt es vollständig |

Damit sind alle weiteren Abschnitte des Formulars hinfällig.

### Warum das belegbar ist

Geprüft am Code, nicht behauptet:

- **Kein Netzwerkcode.** Keine Verwendung von `HttpURLConnection`, OkHttp, Retrofit, `java.net`,
  `WebView` — nichts.
- **Keine Fremd-SDKs.** Die Abhängigkeiten sind ausschliesslich AndroidX (Compose, Room,
  WorkManager, Lifecycle) und Kotlin-Coroutines. Kein Analytics, kein Crashlytics, keine Werbung.
- **Kein eigener Datenversand**: Erinnerungen und Fütterungen liegen in der Room-Datenbank,
  Einstellungen in SharedPreferences — beides im App-eigenen Speicher. Die App selbst schickt
  nichts davon irgendwohin.

### Die eine Ausnahme: Androids eigene Sicherung

Hier stand früher „alles bleibt lokal". Das war zu einfach — und mit `allowBackup="true"` und
*ohne* Regelwerk sogar falsch: Android sicherte damit **den kompletten App-Speicher** ins
Google-Konto, einschliesslich der aufgenommenen Filme und des Absturzberichts. Genau das
widersprach der Zusage in `CrashLog.kt`, der Bericht verlasse das Gerät nur, wenn der Nutzer ihn
selbst weitergibt.

Seither ist festgelegt, was mitdarf (`res/xml/backup_rules.xml` und
`res/xml/data_extraction_rules.xml` — Positivliste, alles Ungenannte bleibt draussen):

| | In der Sicherung |
|---|---|
| Erinnerungen, Pflegebuch (Room, inkl. WAL-Datei) | **ja** |
| Einstellungen (SharedPreferences) | **ja** |
| Aufgenommene Filme (`clips/`) | nein |
| Standbilder (`shots/`) | nein |
| Absturzbericht (`last-crash.txt`) | nein |

**Zum Absturzbericht im Einzelnen.** Er entsteht nur bei einem Absturz, enthält Version,
Zeitpunkt, Gerätemodell, Android-Version und den Stacktrace, und ist auf 64 KB gekappt (eine
Endlosrekursion erzeugt sonst Tausende gleichförmiger Zeilen). Er verlässt das Gerät **von allein
nie**: nicht über die Sicherung (siehe Tabelle), nicht über das Netz — die App hat keinen
Netzwerkcode. Weitergeben kann ihn nur der Nutzer selbst über „Bericht teilen" in den
Einstellungen; direkt daneben steht „Löschen". Beim Deinstallieren geht er mit dem App-Speicher
weg. Genau das sagt der Hinweistext in den Einstellungen dem Nutzer auch — vorher stand dort nur
der halbe Satz „bleibt auf deinem Gerät".

Warum überhaupt sichern statt `allowBackup="false"`: Ohne Sicherung verliert ein Gerätewechsel
sämtliche Erinnerungen und die komplette Fütterungshistorie. Das ist der Stand, an dem Nutzer
hängen — und der einzige, dessen Verlust wirklich weh tut.

**Was das fürs Formular heisst.** Die Antworten oben bleiben **Nein**: Auto Backup landet in der
Google-Drive-Ablage *des Nutzers*, verschlüsselt, und ist für uns als Entwickler nicht
zugänglich — nach Googles Auslegung ist das keine Erhebung durch die App. *Vor dem Einreichen
gegen die dann gültige Data-Safety-Hilfe gegenprüfen*, das ist eine Auslegungsfrage und keine
Tatsache des Codes.

Ehrlicher formuliert lautet die Aussage an den Nutzer also nicht mehr „nichts verlässt je das
Gerät", sondern: **die App sendet nichts; Erinnerungen und Einstellungen können über Androids
eigene Sicherung im Google-Konto des Nutzers landen; Aufnahmen und Absturzberichte nie.** Wer
Datenschutztexte schreibt, nimmt diesen Satz und nicht den alten.

### Zwei Stellen, die man falsch einschätzen könnte

**Der KI-Import.** Er sieht nach Datenweitergabe aus, ist aber keine: der Nutzer führt das
Gespräch selbst in ChatGPT oder Claude und reicht das Ergebnis über Zwischenablage oder
Teilen-Funktion herüber. **Die App spricht mit keinem Server** — sie liest nur Text entgegen. Genau
deshalb wurde dieser Weg gewählt und nicht eine API-Anbindung (siehe `ReminderImportDialog`): eine
direkte Anbindung würde Nutzereingaben an Dritte senden und wäre hier deklarationspflichtig.

**`ACCESS_NETWORK_STATE`, `WAKE_LOCK` und `FOREGROUND_SERVICE`** tauchen im zusammengeführten
Manifest auf und werden in der Store-Auflistung erscheinen. Alle drei stammen aus **WorkManager**
(dem Watchdog für die Alarmkette), nicht aus eigenem Code — nachprüfbar in
`app-sim/build/intermediates/merged_manifests/debug/`. Sie erlauben keine Datenübertragung; die
Netzwerkstatus-Berechtigung liest lediglich, ob eine Verbindung besteht. Das ändert an den
Antworten oben nichts.

Der vollständige Satz im Release-Manifest ist damit: die drei obigen aus WorkManager,
`RECEIVE_BOOT_COMPLETED` aus eigenem Code und die von Android selbst erzeugte
`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. **Kein `SCHEDULE_EXACT_ALARM`** — siehe oben.

### Sobald sich etwas davon ändert

Das Formular ist **bei jedem Release erneut zu bestätigen**. Es muss angepasst werden, sobald:

- Crash-Reporting dazukommt (siehe offene Punkte weiter oben) — dann werden Diagnosedaten erhoben,
- Werbung eingebunden wird — dann werden in aller Regel Kennungen erhoben und geteilt,
- der KI-Zugang doch direkt in der App läuft — dann verlassen Nutzereingaben das Gerät.

Alle drei standen in der Planung zur Diskussion; wer sie umsetzt, muss hier nachziehen.

## Store-Text (Entwurf)

Entstanden aus der Frage, ob Tama ein Habit Tracker sei. **Ist sie nicht — und soll sie
ausdrücklich nicht sein.** Der Markt ist voll davon, und das Versprechen eines Trackers (abhaken,
Serien, Erfüllungsquote, lückenloses Protokoll) widerspricht dem, was diese App tut.

Das ist keine Meinung, sondern im Code nachweisbar:

- `dailyGoal` steht standardmäßig auf `NO_GOAL` — eine neue Erinnerung ist ein Anstupser ohne Ziel.
- Die Stimmungsmechanik ist standardmäßig **aus**, und Erinnerungen ohne Tagesziel wirken sich nie
  darauf aus.
- Es gibt **keine Serien**, nirgends im Code.
- Bei dunklem Bildschirm löst die App bewusst nicht aus und zählt nichts — ein Tracker mit
  gewollten Lücken wäre kaputt, hier ist es das Merkmal.

**Deshalb: das Wort „Habit Tracker" gehört nicht in den Eintrag.** Wer die App darüber findet,
sucht am zweiten Tag den Abhaken-Knopf, findet ihn nicht und schreibt eine Rezension darüber.

### Kategorie

**Lifestyle**, nicht Produktivität. Produktivität ist das Regal, in dem die Tracker stehen.

### Kurzbeschreibung (max. 80 Zeichen)

> Erinnerungen, die jemand bemerkt. Ohne Zählen, ohne Serien, ohne Druck.

Englisch:

> Reminders someone notices. No counting, no streaks, no pressure.

### Vollständige Beschreibung

> Tama erinnert dich – und merkt, ob du reagierst. Mehr nicht.
>
> Keine Häkchen. Keine Serien. Keine Prozentzahlen am Monatsende. Nichts, das dir vorrechnet, an
> wie vielen Tagen du es nicht geschafft hast.
>
> Stattdessen lebt auf deinem Bildschirm ein kleines Wesen. Es zeigt die Uhrzeit, und wenn etwas
> ansteht – trinken, aufstehen, kurz durchatmen –, wird es sichtbar. Du reagierst, indem du es
> fütterst. Es merkt sich das. Und wenn du gerade nicht kannst, passiert nichts weiter.
>
> **Wo es erscheint**
> Auf dem Startbildschirm, als Vollbild-Anzeige fürs Nachttischchen, und als rundes Widget auf
> deinem Home-Screen. Immer nur dann sichtbar, wenn dein Bildschirm ohnehin an ist.
>
> **Sechs Wesen, sechs Charaktere**
> Puffling, Starlet, Wyrmling, Fennec, Gloop und Hootlet – jedes mit eigener Art, eigener Stimme
> und eigenem Satz Erinnerungen. Der ruhige Beobachter erinnert anders als der kleine Motivator.
>
> **Was du einstellst**
> Bezeichnung, Wochentage, Zeitfenster, Abstand. Optional ein Tagesziel, wenn du eins willst –
> ohne läuft es genauso. Eine kleine Bibliothek an Animationen liegt bei, du kannst eigene
> hinzufügen.
>
> **Was bleibt, wo es ist**
> Alles auf deinem Gerät. Kein Konto, keine Anmeldung, keine Werbung, keine Server. Die App hat
> keinen Netzwerkcode – sie könnte gar nichts senden, selbst wenn sie wollte.
>
> Für alle, die es leid sind, dass ihre Vorsätze eine Tabelle brauchen.

### Was bewusst nicht drinsteht

- **„Habit Tracker", „Streak", „Statistik", „Analyse"** – setzt Erwartungen, die die App nicht bedient.
- **„Produktivität", „Routine optimieren"** – falscher Ton für ein Produkt, dessen Kern das
  Fehlen von Druck ist.
- **Versprechen zur Pünktlichkeit** – ohne exakte Alarme kommen Erinnerungen ungefähr (siehe oben).
  Nichts schreiben, was ein Nutzer als Zusage lesen könnte.

### Vor dem Einreichen noch zu tun

- Screenshots: Startbildschirm mit Wesen, Dock-Modus, Widget auf dem Home-Screen,
  Erinnerungs-Dialog. Der Dock-Modus ist das eigenständigste Bild – gehört nach vorn.
- Feature-Grafik (1024×500).
- Datenschutzerklärung verlinken; Inhalt ergibt sich aus dem Data-Safety-Abschnitt oben.
- Beschreibung auf Englisch spiegeln (die App startet auf Englisch).

## Weitere offene Punkte

- **Upload-Keystore anlegen** — siehe `keystore.properties.example`. Sicher aufbewahren: geht er
  verloren, lässt sich die App nie wieder aktualisieren.
- **Vorsicht beim Umstieg von der Debug-Installation.** Ein per `Tama-debug.apk` installiertes
  Gerät trägt die Debug-Signatur. Android ersetzt eine App nicht durch ein anders signiertes
  Paket — die Play-Version lässt sich also nicht darüber installieren, die Debug-Fassung muss
  vorher **deinstalliert** werden. Damit ist die Room-Datenbank weg: alle Erinnerungen, die
  komplette Fütterungshistorie, die Stimmung des Avatars. Wer seinen Stand behalten will, braucht
  vorher einen Export (bisher nicht gebaut).
- **Migrationstest auf einem Gerät** — `./gradlew :app-sim:connectedDebugAndroidTest`.
- `RECEIVE_BOOT_COMPLETED` ist nicht eingeschränkt und braucht keine Deklaration.
