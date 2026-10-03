# Fennec im Twitch-Chat

Stream-only Erweiterung der vorhandenen Runtime. Direkte Ansprache (`hey fennec how are you?`,
`Hallo Fennec`, `@fennec_itoeva`, `How are you, Fennec?`) fuehrt zu vorhandenen Mundframes,
Sprechpunkten und einer kurzen Antwort im Bild. Der eigentliche Weltablauf laeuft weiter.
A-D und Reminder gehen weiterhin ausschliesslich durch das bisherige Impulstor.

## Lokaler Betrieb

1. Ollama mit `qwen3:1.7b` bereitstellen (`ollama pull qwen3:1.7b`).
2. `python fennec_chat.py --state-file PFAD_AUSSERHALB_DES_REPOSITORY/private-auth.json` starten.
3. `adb -s emulator-5554 reverse tcp:18766 tcp:18766` ausfuehren.
4. Stream-APK installieren/starten und `http://localhost:18766` oeffnen.

Der Dienst lauscht nur auf `127.0.0.1`. HTTP ist in der Stream-APK ausschliesslich fuer
Loopback freigegeben, Twitch bleibt TLS. Keine Netzberechtigung fuer die normale App.
Die Webseite prueft Host, Origin, JSON-Inhalt und Nonce; OAuth prueft einen einmaligen State.
Die lokale Vorschau verwendet einen nur durch Androids DUMP-Berechtigung erreichbaren
Broadcast und schreibt niemals an Twitch. Nur der aktuell sichtbare Fennec reagiert.

## Antworten und Anmeldung

Zustandsfragen verwenden belegte Orts-Saetze. Weitere harmlose Fragen gehen an das lokale
Sprachmodell, mit festen Rollen, begrenztem Kontext und maximal 96 Antworttokens. Keine Tools,
Spielzugriffe oder private Daten. Nach 12 Sekunden oder unbrauchbarer Ausgabe folgt eine lokale
Antwort. Links, erkannte Instruktionsangriffe und sensible Themen haben eine ruhige Rueckfallantwort.
Diese einfachen Filter sind keine vollstaendige Moderationsloesung fuer beliebigen Publikumsverkehr.
KI-Ausgaben koennen unpassend sein; der lokale Pilot braucht Beobachtung.

Globale Grenze: eine Antwort pro 10 Sekunden, pro Nutzer 30 Sekunden. Keine Warteschlange,
keine Wiederholung nach Schreibfehlern, keine Logs von Fragen/Antworten und keine Chat-Historie.
Eigene Nachrichten tragen `[Fennec]` und werden als Eingang verworfen.

Der PC liest auf Wunsch die vorhandene OBS-Twitch-Anmeldung. Sie muss noch gueltig sein,
zum Kanal `fennec_itoeva` gehoeren und `chat:read`+`chat:edit` oder `user:write:chat` erlauben.
Keine Refresh-Tokens oder Client-Secrets werden gelesen. Ein abgelaufener OBS-Zugang kann
waehrend des Livebetriebs nicht ueber die dort deaktivierte Kontoseite erneuert werden.
OBS 32.2.2 speichert die erneuerte Kontofreigabe erst beim vollstaendigen Beenden, nicht
bereits mit OK in den Stream-Einstellungen. Danach OBS neu oeffnen und Stream/Aufnahme
wieder starten. Ein gueltiger Zugang nur mit `channel:read:stream_key` reicht nicht fuer
Chat-Antworten; in diesem Fall die eigene Public-App-Verbindung unten verwenden.

Alternativ in der Twitch-Konsole eine eigene **Public**-App mit Weiterleitung
`http://localhost:18766/callback` registrieren. Die lokale Seite nimmt deren oeffentliche
Client-ID und startet Twitchs Anmeldung mit `user:write:chat`. Der Mensch meldet sich an und
bestaetigt die Berechtigung. Die Webseite nimmt das Token aus dem Fragment, entfernt es aus
der URL und speichert es nur im angegebenen privaten State-Pfad. Die Anmeldung ist zeitlich
begrenzt; nach Ablauf neu verbinden. Keine automatische Kontoerstellung oder bezahlte API.

Helix `POST /helix/chat/messages` bestaetigt `is_sent`. Bei bestehenden IRC-Rechten verwendet
der Dienst TLS-IRC; dessen Schreiben hat keine API-Bestaetigung. Die UI behauptet daher
keinen erfolgreichen Versand. `/status` zaehlt Antworten und Schreibversuche ohne Geheimnisse.
Eine fehlende/abgelaufene Anmeldung laesst Antworten im Bild weiterhin zu, schreibt aber nichts.

## Ruecksetzen und Pruefen

PC-Dienst beenden und die vorige Stream-APK installieren; kein Room- oder Spielstandumbau.
Private Anmeldedatei separat entfernen bzw. die Twitch-App-Berechtigung im eigenen Konto widerrufen.

`python -m unittest discover -s tools/fennec-chat -v`

`./gradlew :app-sim:testStreamUnitTest --tests 'com.notime.glyphsim.stream.*' :app-sim:assembleStream`

Quellen: [Twitch Chat-Anmeldung](https://dev.twitch.tv/docs/chat/authenticating/),
[Send Chat Message](https://dev.twitch.tv/docs/api/reference/#send-chat-message),
[Ollama Chat API](https://docs.ollama.com/api/chat).
