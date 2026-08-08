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
- **Alles bleibt lokal**: Erinnerungen und Fütterungen in der Room-Datenbank, Einstellungen in
  SharedPreferences. Beides im App-eigenen Speicher, beides beim Deinstallieren weg.

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
