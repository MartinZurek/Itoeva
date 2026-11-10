# Itoeva Kartenschmiede

Spielbarer Godot-Quellstand für die Kartenschmiede 0.19.6 (Build 68).
Die Basis wurde aus der Itoeva-APK 0.19.1 wiederhergestellt.
Das Projekt liegt in diesem Branch direkt im Hauptverzeichnis.

## Auf einem anderen PC starten

```sh
git clone --branch codex/kartenschmiede-0.19.6 --single-branch https://github.com/MartinZurek/Itoeva.git Itoeva-Kartenschmiede
```

In Godot 4.5.1 die Datei `project.godot` importieren. Nach dem Import mit F5 starten.
An der Werkbank stehen drei Solo-Übungsduelle zur Auswahl.
Auf dem Desktop kann `MOTION_M4=1` als Umgebungsvariable gesetzt werden, um Mooslyns gemalte seitliche Galoppanimation im Editor zu prüfen.

Für Android die passenden Exportvorlagen und das Android SDK in Godot einrichten und das Profil **Kartenschmiede** verwenden. Das Profil aktiviert `itoeva_motion_m4`.
Ein Update einer bereits installierten APK braucht denselben Signaturschlüssel wie die installierte Version.

## Enthaltene Änderungen

- [Solo-Übungsduelle](docs/kartenschmiede-uebungsduelle.md)
- [Kampfinszenierung und tatsächliche Treffer](docs/kartenschmiede-kampfinszenierung.md)
- [Bodenkontakt in der Beschwörungsszene](docs/kartenschmiede-bodenkontakt.md)
- [Zehnsekündige Einspieler](docs/kartenschmiede-zehnsekunden-einspieler.md)
- Build 68 aktiviert die bereits vorhandene gemalte M4-Figurenanimation wieder im Android-Export. Geprüft wurden der seitliche Galopp in Godot, die Kampfeinspieler und die signierte APK. Ein Test auf einem physischen Android-Gerät steht aus.
