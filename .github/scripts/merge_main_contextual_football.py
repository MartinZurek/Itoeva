from pathlib import Path

path = Path('SKILLBAUM.md')
s = path.read_text()

old_repertoire = '''      **gezeichneten** Nachkommen; der Wirtsknoten selbst fehlt bewusst (seine Reaktion IST die
      Handlung, die gerade lief — sie zu wiederholen wäre ein Echo, keine Fähigkeit).
'''
new_repertoire = '''      **gezeichneten** Nachkommen; der Wirtsknoten selbst fehlt bewusst (seine Reaktion IST die
      Handlung, die gerade lief — sie zu wiederholen wäre ein Echo, keine Fähigkeit). Seit dem
      kontextuellen Fussball-Schnitt bleiben auch dessen Stufe-3-Knoten hier enthalten: Der autonome
      PERFORM-Pfad hat noch keinen exakten Skill-Intent und wuerde sie sonst komplett verlieren.
'''
if new_repertoire not in s:
    if old_repertoire not in s:
        raise SystemExit('SKILLBAUM repertoire anchor missing')
    s = s.replace(old_repertoire, new_repertoire, 1)

old_bus = '''- `AvatarActivityBus` bleibt unbenutzt. Die Einlage hier braucht keinen mitgeführten Zustand, und
  ein Schreiber ohne Leser wäre nur eine zweite Karteileiche.
'''
new_bus = '''- `AvatarActivityBus` war in P15 noch unbenutzt. Seit dem kontextuellen Fussball-Schnitt vom
  2026-09-04 hat er einen echten Leser und Schreiber: Ein exakter Ballsport-/Dribbling-/Schuss-Intent
  setzt die laufende Ballsport-Aktivitaet, damit eine folgende Stufe-3-Faehigkeit an dieselbe
  Beschaeftigung anschliessen kann. Der Zustand bleibt bewusst sitzungsgebunden und laeuft ab.
'''
if new_bus not in s:
    if old_bus not in s:
        raise SystemExit('SKILLBAUM AvatarActivityBus anchor missing')
    s = s.replace(old_bus, new_bus, 1)

journal_header = '''| Datum | Paket | Ergebnis / Hinweis für die nächste Sitzung |
|---|---|---|
'''
new_row = '''| 2026-09-04 | Kontextueller Fussball-Schnitt | **Reminder/Skills werden zu Absichten in der vorhandenen Welt.** `AvatarActivityPlans` loest Ballsport/Dribbling/Schuss anhand von Ort und echten Unlocks in bestehende `PlayRoutine`-Schritte auf; `DockScreen` behaelt dafuer den exakten `ReactionTrigger.Node`, und `AvatarActivityBus` ist jetzt tatsaechlich aktiv. Park/Wiese bleiben lokal, ungeeignete Innenraeume nutzen den sichtbaren `GoToPlace(SPORT)`-Weg. `TOUCH` ist die Anfaenger-Basis; `DRIBBLE` sowie `AIM`/`KICK` erscheinen nur nach echter Freischaltung. **Keine Level-Schwellen erfunden:** Welche Level Varianten freischalten, bleibt laut `Tagesablauf.md` offen. Im autonomen PERFORM-Pfad bleiben dieselben Fussball-Unlocks weiterhin als `SkillRepertoire`-Einlagen sichtbar, bis auch dieser Pfad einen exakten kontextuellen Intent besitzt. Fuer die naechste Sitzung: dieselbe Architektur nur vertikal fuer weitere Skills ausbauen, nicht daneben ein zweites Handlungssystem beginnen. |
'''
if new_row not in s:
    if journal_header not in s:
        raise SystemExit('SKILLBAUM journal header missing')
    s = s.replace(journal_header, journal_header + new_row, 1)

path.write_text(s)
