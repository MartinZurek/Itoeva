# Reiseatlas und Erlenfurt

Die Weltkarte zeigt Küste und Dorf im unteren Teil, den Waldpfad oberhalb des Dorfes
und die Hochlandstrecke im Osten. Die Kartenpositionen folgen den Nachbarschaften.
Die Pfeile in der Ortsliste folgen der tatsächlichen Laufrichtung im Spiel. Küste,
Dorf, Wald, Grenzwege und Hochland lassen sich nach der Entdeckung direkt im Atlas
anwählen. Eine durchgehend begehbare Verbindung erscheint erst nach Entdeckung
beider Enden und wenn der Weg in beide Richtungen offen ist. Sperren stehen bei den
besuchten Nachbarorten mit der jeweiligen Voraussetzung.

Die Erlenfurt liegt zwischen Mühlenbach und Ebene. Der Steinweg auf dem neuen
Landschaftsbild ist die begehbare Fläche. Beide Ausgänge sind beschriftet und
funktionieren per Stick oder Antippen. Die Route verkürzt den Weg vom Mühlenbach
zur Ebene von vier auf zwei Übergänge. Der Rückweg über Ringwehr und alten
Mühlenzulauf bleibt an den Fortgang der Mühlenchronik gebunden.

Ortsbilder im Atlas und in der Ladeansicht stammen aus den Originaltexturen. Bei
gemeinsamen Panoramen wird der Abschnitt des benannten Orts gezeigt. Der Atlas
verkleinert nur diesen Ausschnitt zum Vorschaubild und öffnet beim Antippen oder
Klicken wieder das Original. Bei hochauflösenden Displays begrenzen Bildansicht
und Ladeansicht den Maßstab anhand der physischen Displaygröße. Das ist besonders
bei Innenraumvorlagen mit 480 × 270 Pixeln sichtbar. Weiteres Bilddetail erfordert
dort neue Vorlagen.

`atlas_route_play.gd` prüft Entdeckung, Routen, Bildauswahl, Bedienung und die Reise
durch die Erlenfurt in beide Richtungen. `world_passages_play.gd` prüft die
durchgehenden Laufabschnitte, die 54 gerichteten Durchgänge und den Zugang zu jedem
Durchgang auf Mooslyns Laufgeometrie. Die Sichtprüfung auf einem Android-Gerät
steht noch aus.
