# App-spezifische R8-Regeln. Was der gemeinsame Kern braucht, kommt automatisch ueber
# core/consumer-rules.pro dazu (Enums der Datenschicht, Watchdog-Worker, Room-Entities).

# ---------------------------------------------------------------------------------------------
# Glyph Matrix SDK (Nothing)
# ---------------------------------------------------------------------------------------------
# Das SDK kommt als fertige AAR ohne eigene Keep-Regeln ins Projekt (libs/glyphsdk.aar, siehe
# README). Wie es intern arbeitet, ist nicht einsehbar - GlyphMatrixManager bindet sich per
# ServiceConnection an einen Systemdienst, und solche Bruecken laufen typischerweise ueber
# feste Klassen-/Methodennamen, die R8 nicht als benutzt erkennt. Wuerde hier etwas
# wegoptimiert, bliebe die Glyph-Matrix im Release-Build einfach dunkel, waehrend im
# Debug-Build alles funktioniert.
#
# Bewusst grosszuegig gefasst: das SDK ist klein (rund 60 KB), die eingesparten Bytes stuenden
# in keinem Verhaeltnis zum Risiko, es kaputtzuoptimieren.
-keep class com.nothing.ketchum.** { *; }
-dontwarn com.nothing.ketchum.**

# Der Glyph-Toy-Service wird vom System ueber das Manifest gestartet; AGP behaelt im Manifest
# deklarierte Komponenten zwar automatisch, die Callback-Methoden ruft aber das SDK auf.
-keep class com.notime.glyphkalender.glyph.** { *; }
