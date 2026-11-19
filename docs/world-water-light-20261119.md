# Sichtbare Weltbewegung – 19. November 2026

Basis: Gameplay 0.19.14, Commit 3a9c009. Separate Weiterentwicklung der gemeinsamen Welteffekte; keine neue APK in diesem Schritt.

Die bestehenden Wasser- und Wolkenmasken bleiben erhalten. Das Strömungsfeld wandert schneller, breite Reflexe haben mehr Kontrast. In gemalten Kulissen bleibt der Versatz auf maximal ein Quellpixel pro Achse begrenzt. Wolken driften innerhalb ihrer vorhandenen Fenster deutlicher. Die Bewegung ist weiterhin eine langsame periodische Verformung der gemalten Wolken, keine vollständige Wolkensimulation. Nicht maskierte Himmelsbereiche bleiben statisch.

Die registrierten Lampen und Feuer bleiben an ihren vorhandenen Koordinaten. Der bestehende Lichtpool erzeugt auf nassen Pixeln jetzt einen bewegten Reflex aus dem ohnehin berechneten Strömungsfeld. Das vorhandene Feuerflackern bleibt erhalten. Wasserreflexe in gemalten Kulissen übernehmen jetzt die Tageslichttönung, damit ihre erhöhte Intensität nachts nicht unverändert hell bleibt.

Keine neuen Texturen, Nodes, Partikel, Renderziele oder Zeichenpassagen. Die Lichtreflexe kosten zusätzliche einfache Rechenoperationen ausschließlich innerhalb bestehender Wasser-/Lichtbereiche. Keine gemessene Zusicherung unveränderter Handy-Frametimes.

Validierung: Godot 4.6.3 Compatibility/OpenGL ES 3.2 (Software-Renderer), 12 echte Renderbilder für Küste, Sonnenhain und Aschenstieg zu zwei Uhrzeiten und zwei Animationszeitpunkten; Shader ohne Kompilierfehler. Küste nachts und Sonnenhain tagsüber visuell geprüft. Mühlenchronik: 23 Checks, 0 Fehler. Gameplay-Test einschließlich sieben Arenen, Gegnern und gültigen Kartenablagen bestanden. Die bestehenden Ressourcenhinweise beim Beenden der Gameplay-/Mühlentests bleiben bestehen.

Reproduzierbare Bildprüfung:
`godot --path . --rendering-driver opengl3_es --script res://tests/world_ambience_render.gd -- /absolute/output`

Offen: längere Bewegungsabnahme in echter Spielkamera, physischer Android-Test mit Frametimes und Wärmeentwicklung; keine Garantie von Ruckelfreiheit aufgrund des Desktop-Renderers.
