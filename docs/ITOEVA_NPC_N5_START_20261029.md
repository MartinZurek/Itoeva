# N5 Siegelweber – Quellenstart und Übergabe (29.10.2026)

## Verbindlicher Stand

Martin hat nach der vergrößerten Glutwicht-Vorschau geantwortet:
„Ok mach weiter mit Den nächsten Charakter und kannst mir schon mal einen
neuen Prompt schicken, sodass ich einen neuen Chat öffnen kann“.
N4 ist lokal vollständig geprüft; Glutwichts große Fassung ist beizubehalten.
Welt-/Schattenfaktor 1.75, sichtbar 1,59–1,69 Mooslyn-Höhen.
N4-Implementierung und Größe: lokale Commits `cf4013a`, `1d58fa4`, `d654756`.
Details: `docs/ITOEVA_NPC_N4_GROESSE_20261029.md`.

Der erneute Push wurde trotz dieser Antwort automatisch abgelehnt: Das Gate
wertet sie als Fortsetzungsauftrag, nicht als eindeutige Freigabe zur
Veröffentlichung des vorgelegten Commits im öffentlichen Repository.
Keinen anderen Publikationsweg als Umgehung nutzen. Remote zuletzt `c7c90bd`;
PR #386 ist deshalb noch nicht auf dem vollständigen N4-Stand.
Ein privates Übergabearchiv sichert ausschließlich die noch unveröffentlichten
Änderungen einschließlich N5-Quelle; es dupliziert keinen synchronisierten
Repository-Checkout. Für den nächsten Chat dieses Archiv zuerst wiederherstellen.

## N5 wurde begonnen, noch nicht implementiert

- Originalvorlage visuell geprüft: `assets/expansion/seal-weaver.png`.
- Siegelweber (`seal_weaver`) steht am `MIST_WATCH`, bisherige Weltgröße 1.13.
- Identität: vier Arme, violett-burgunderfarbenes Federgewand, helle
  schnabelartige Maske und cyanfarbenes Auge, rote Siegel und goldene Bänder.
- Neue Bildgenerierungsquelle: `tools/motion-art/sources/seal-weaver-motion-n5.png`.
  Vollständiger Prompt direkt daneben als `.txt`. Acht Schwebezeichnungen
  in den ersten zwei Reihen, vier Ruhezeichnungen in Reihe drei, alle links.
- Technische Quellprüfung: zwölf getrennte große Alphakomponenten,
  Transparenz und Originalprüfsumme; Ergebnis in
  `tools/motion-art/seal-weaver-n5-source-check.json`.
- Das ist ein **Quellentwurf**, kein fertiger Atlas und keine abgenommene
  Animation. Noch keine Iris-/Rumpfregistrierung, kein N5-Pixelpass, keine
  N5-Laufzeitdaten, keine N5-Zustandsprüfung und keine N5-Weltaufnahme.

## Nächster konkreter Arbeitsschritt

1. AGENTS.md und die verlangten Einstiegsdokumente lesen; lokalen und
   neueren Remote-Stand prüfen. N1–N4 und die vier Hauptfiguren beibehalten.
2. Originalvorlage und neue N5-Quelle visuell öffnen. Identität, vier Arme,
   vollständige Konturen, konstante Kopfgröße, Phasenanschlüsse und
   glaubwürdige Schwebehaltung prüfen; ungeeignete Zeichnungen nacharbeiten.
3. Ganze Zeichnungen mit festem Iris-/Rumpfanker registrieren. Der unterste
   flatternde Gewandsaum darf nicht den Körper hoch/runter verschieben.
   Kein per Phase wechselnder Maßstab, keine Gliedmaßenverformung.
4. Artgerechter zeitgebundener Schwebetakt: beim Anhalten keine eingefrorene
   Figur und kein Phasensprung. Kein Gehen mit abwechselnden Fußkontakten.
   Bestehende Flug-/Bewegungslogik bevorzugen, keine neue Wegsimulation.
5. HD-2D-Pixelpass, Metadaten, passende Prüfungen und echte Weltaufnahme mit
   Mooslyn, beiden Richtungen, zwei Stopps und Wiederanfahren. Bisherige
   N1–N4-/M4-Prüfungen erhalten. Neue Prüfresultate vollständig protokollieren.
6. Erst einen vollständigen N5-Einzelcheckpoint mit Quellen, Prompts,
   Atlanten, Tests, Welt-/Detailfilm und Übergabe sichern. Danach N6
   Tangkrabbler; vollständige Figurenliste steht im N1-Savespot.

Keine APK und kein Merge. Nur gemalte 2D-Sprites mit HD-2D-Pixelpass.
Hauptfiguren aus APK 0.15.8 sind abgenommen. Größenkorrektur des Glutwichts
nicht zurücknehmen. Konkrete Publikationsbestätigung bleibt beim Gate offen;
die private Arbeit und Prüfung können fortgesetzt werden.
