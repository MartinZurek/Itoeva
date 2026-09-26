#!/usr/bin/env python3
"""Waehlt aus mehreren Takes desselben Stuecks den mit den saubersten Hoehen - Best-of-N.

Warum: Nach der Harmonie-Ueberarbeitung (#243) zeigte die Messung, dass die Takes dieses Modells
(Stable Audio 3 small-music, 8 Schritte, CFG 1) von Seed zu Seed staerker streuen, als der
Prompt-Text sie lenkt: 13 von 26 neuen Takes massen rauer als ihre Vorgaenger, andere deutlich
glatter. Diese Streuung laesst sich nutzen, statt sie hinzunehmen - mehrere Seeds erzeugen, den
besten behalten. Freigegeben am 2026-09-26 (Variante "automatisch": nur der Gewinner geht in die
Hoertest-APK, das Ohr entscheidet danach wie bisher).

Die Regel, in dieser Reihenfolge:

1. Nur Takes, die das Loop-Gate bestanden haben, kommen hier ueberhaupt an (ein gescheiterter
   Lauf legt keinen Branch an).
2. **Dumpf-Sperre**: Ein Take, dessen Schaerfe mehr als `DUMPF_DB` unter dem Mittel seiner
   Geschwister liegt, scheidet aus. Ein Take fast ohne Hoehen misst automatisch "glatt", klingt
   aber leblos - der Wert soll saubere Hoehen belohnen, nicht fehlende.
3. **Gewinner** ist der Take mit der geringsten Rauigkeit oberhalb 900 Hz (`rau_hoch`).
4. Liegen die besten zwei weniger als `KNAPP` (relativ) auseinander, gewinnt der weniger scharfe.

Verglichen wird nur innerhalb eines Stuecks (gleicher Prompt, anderer Seed). Zwischen Stuecken
waere der Vergleich unfair: Eine angezerrte Gitarre misst immer rauer als ein Klavier.

Die Messwerte liefert `harmony_report.py`; dieses Modul rechnet nur und braucht deshalb weder
numpy noch Audiodateien - die Regel ist so in einer Sekunde pruefbar.
"""
from __future__ import annotations

from dataclasses import dataclass

DUMPF_DB = 6.0
KNAPP = 0.05


@dataclass(frozen=True)
class Take:
    name: str
    scharf_db: float
    rau_hoch: float


def pick(takes: list[Take]) -> Take:
    """Der Gewinner nach der Regel oben. Wirft bei leerer Liste."""
    if not takes:
        raise ValueError("keine Takes zur Auswahl")
    mittel = sum(t.scharf_db for t in takes) / len(takes)
    feld = [t for t in takes if t.scharf_db >= mittel - DUMPF_DB] or list(takes)
    feld.sort(key=lambda t: (t.rau_hoch, t.scharf_db))
    if len(feld) >= 2:
        erster, zweiter = feld[0], feld[1]
        if zweiter.rau_hoch - erster.rau_hoch < KNAPP * max(erster.rau_hoch, 1e-9):
            return min(erster, zweiter, key=lambda t: t.scharf_db)
    return feld[0]
