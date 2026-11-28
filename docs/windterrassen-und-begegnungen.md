# Windterrassen und Begegnungen

Die Erlenfurt führt über die Windterrassen und die Signalwarte zum Gebirgspass. Alle drei Übergänge sind in beiden Richtungen begehbar. Beide neuen Orte haben eigene gemalte Kulissen, Ortsbilder im Atlas und Hochlandmusik. Auf den Terrassen lässt sich Mooslyn den eingeritzten Weg am Windstein zeigen. An der Warte kann er die Signalglocke mit einem kurzen sichtbaren und hörbaren Echo läuten.

Möbel und andere im Spiel platzierte Requisiten haben eine beschriftete Aktion und eine Trefferfläche am Objekt. Wo es keine Spezialaktion gibt, kann Mooslyn das Objekt untersuchen und einen zum Gegenstand passenden Satz finden. Der Fokus zeichnet goldene Ecken am Objekt. Figuren, Hebel, Kartenfunde und Erzählobjekte lassen sich ebenfalls über ihre sichtbare Fläche auswählen.

Bewohner gehen mit verschiedenen Geschwindigkeiten und bremsen beim Ankommen ab. Für Tätigkeiten suchen sie ihren gewohnten Platz auf. Je nach Tagesplan halten sie zum Beispiel eine Karte, Brot oder ein Blatt in den Händen.

| Gegner | Tätigkeit am Ort | Begegnung |
| --- | --- | --- |
| Schlackenträger | Hockt am Depot und sortiert glühende Schlacke. | Fragt nach Mooslyns Anliegen am Siegelhof. |
| Vharok | Kniet am Tor und zieht sein Schutzsiegel nach. | Fragt, ob Mooslyn wissen will, was er hinter dem Tor hütet. |
| Relaiswächter | Stimmt die drei Ringe ab. Dabei flackern türkise Impulse. | Bietet Mooslyn die Prüfung des Relais an. |

Die drei Gegner wenden sich Mooslyn zu, wenn er ihnen nahe kommt. Bleibt er mit freier Sicht kurz stehen, sprechen sie ihn pro Besuch einmal an. Das Gespräch bietet eine Einladung zum bestehenden Kartenkampf und die Möglichkeit, gleich weiterzugehen. Das [Konzeptblatt](../assets/expansion/guardian-concepts.webp) wird als Portraitquelle in diesen Gesprächen verwendet. Die Arbeitshaltungen auf der Karte verwenden vorhandene gezeichnete Posen, Gewichtsverlagerung und kleine Effekte am Arbeitsplatz.

`godot --headless --path . --script tests/wind_terraces_play.gd` prüft Einrichtung und direkte Objektklicks, die Hochlandroute in beide Richtungen, Wegstein und Glocke sowie die Gegnertätigkeiten und das Gespräch.
