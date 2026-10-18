# Strategische Karten – Wiederaufbau 18.10.2026

Status: Umsetzung läuft. Basis: Godot-Kampf 6677651 (PR #369).
Auftrag von Martin: Karten neu bauen und nach Spielspaß/strategischer Tiefe optimieren.

Ziel: 616 bestehende Karten-IDs in den vorhandenen Kampf/Deckeditor integrieren;
13 Völker mit ausgespielten Effekten, drei Helferplätze pro Figur, Gegenstrategien,
nutzbare frühe Karten und begrenzte mythische Motoren. Die Quell-Faktordaten bleiben
unverändert. Spielwerte bekommen eine getrennte, versionierte Laufzeitübersetzung.
Die bisherige pauschale 2K+1-Kurve ist kein Beleg für Kampf-Balance.

Vorhandene Decks und die bisherigen Zauber bleiben lesbar. Neue Deckvorschläge
ersetzen bestehende Decks nur nach bewusster Auswahl im Editor. Keine neue
Kartenbezahlung, Verlustmechanik oder Cloud-Anbindung.

Arbeitsschritte: Daten/Regeln; Effektkern + Speichern; Bilder/UI; deterministische
Kampf- und Decktests; Godot-Renderprüfung. Erst tatsächlich ausgeführte Prüfungen
werden als bestanden dokumentiert. Zwischenstände werden direkt auf diesem
Branch gesichert; kein direkter Push auf main.
