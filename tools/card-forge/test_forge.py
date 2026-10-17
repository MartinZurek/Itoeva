#!/usr/bin/env python3
"""Prüft die Kartenschmiede ohne echte Daten: Kurve, Stabilität über Monate, nichts geht verloren.

    python tools/card-forge/test_forge.py
"""
from __future__ import annotations

import json
import random
import sys
import tempfile
import unittest
from pathlib import Path

HIER = Path(__file__).resolve().parent
sys.path.insert(0, str(HIER))
import forge  # noqa: E402

REGELN = json.loads((HIER / "rules.json").read_text(encoding="utf-8"))
KOPF = ["ID", "Quality Rank", "Low Vola Rank", "Momentum Rank", "Value Rank", "Composite Rank", "Size Rank",
        "Quality Qt.", "Low Vola Qt.", "Momentum Qt.", "Value Qt.", "Composite Qt.", "Size Qt.",
        "ISIN", "Industry", "Name", "Sector"]


def sektor_von(industrie: str) -> str:
    """Rückwärts aus den Regeln: jede Industrie wird einem Sektor zugeordnet, der sie enthält."""
    return ZUORDNUNG[industrie]


# Testzuordnung Industrie -> Sektor (nur für erfundene Zeilen; echte Daten bringen den Sektor mit)
ZUORDNUNG = {}
for sek, inds in {
    "Information Technology": ["Software", "Semiconductors & Semiconductor Equipment", "IT Services"],
    "Financials": ["Banks", "Insurance", "Capital Markets"],
    "Utilities": ["Electric Utilities", "Multi-Utilities"],
    "Consumer Staples": ["Food Products", "Beverages"],
    "Health Care": ["Biotechnology", "Pharmaceuticals"],
    "Industrials": ["Machinery", "Aerospace & Defense"],
    "Energy": ["Oil, Gas & Consumable Fuels"],
    "Materials": ["Chemicals", "Metals & Mining"],
    "Real Estate": ["Specialized REITs"],
    "Communication Services": ["Media", "Entertainment"],
    "Consumer Discretionary": ["Specialty Retail", "Automobiles"],
}.items():
    for i in inds:
        ZUORDNUNG[i] = sek


def erfundene_zeilen(n: int, rnd: random.Random) -> list[list]:
    zeilen = []
    for i in range(n):
        ind = rnd.choice(sorted(ZUORDNUNG))
        qs = [rnd.randint(1, 5) for _ in range(6)]
        zeilen.append([f"T{i:03d} US Equity", 0, 0, 0, 0, 0, rnd.randint(1, 600), *qs,
                       f"XX{i:010d}", ind, f"Firma {i}", sektor_von(ind)])
    return zeilen


def schreibe_xlsx(pfad: Path, zeilen: list[list]) -> None:
    import openpyxl
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.append(KOPF)
    for z in zeilen:
        ws.append(z)
    wb.save(pfad)


class KartenTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.rnd = random.Random(7)

    def schmiede(self, zeilen, monat):
        x = self.tmp / f"{monat}.xlsx"
        schreibe_xlsx(x, zeilen)
        return forge.schmiede(x, monat, self.tmp / "private", self.tmp / "out", False)["karten"]

    def test_jede_karte_liegt_auf_der_kurve(self):
        ppk, grund = REGELN["kurve"]["punkte_pro_kristall"], REGELN["kurve"]["grundpunkte"]
        for k in self.schmiede(erfundene_zeilen(300, self.rnd), "2026-10"):
            self.assertLessEqual(abs(k["wertpunkte"] - (ppk * k["kosten"] + grund)), 0.51, k)
            self.assertTrue(1 <= k["kosten"] <= 10)
            self.assertGreaterEqual(k["angriff"], 1)
            self.assertGreaterEqual(k["leben"], 1)

    def test_mehr_momentum_heisst_nie_weniger_angriff(self):
        basis = ["A US Equity", 0, 0, 0, 0, 0, 100, 3, 3, 3, 3, 3, 3, "XA", "Machinery", "A", "Industrials"]
        anteile = []
        for m in (5, 4, 3, 2, 1):
            z = list(basis)
            z[9] = m
            d = forge.entdopple([dict(zip(forge.SPALTEN, self._zeile(z)))])[0][0]
            k = forge.baue_karte(d, {"karte_id": "K", "eigenname": "N"}, REGELN)
            anteile.append(k["angriff"])
        self.assertEqual(anteile, sorted(anteile))

    def _zeile(self, z):
        m = dict(zip(KOPF, z))
        return [m[s] if s not in ("Quality Qt.", "Low Vola Qt.", "Momentum Qt.", "Value Qt.", "Composite Qt.", "Size Qt.")
                else int(m[s]) for s in forge.SPALTEN.values()]

    def test_groesse_wird_seltenheit(self):
        zeilen = erfundene_zeilen(5, self.rnd)
        for i, z in enumerate(zeilen):
            z[12] = i + 1  # Size Qt. 1..5
        karten = self.schmiede(zeilen, "2026-10")
        nach_name = {k["name"]: k for k in karten}
        self.assertEqual(sorted(k["seltenheit"] for k in nach_name.values()),
                         sorted(REGELN["seltenheit"][s]["name"] for s in "12345"))

    def test_folgemonat_namen_stabil_nichts_geht_verloren(self):
        okt = erfundene_zeilen(120, self.rnd)
        k_okt = {k["id"]: k for k in self.schmiede(okt, "2026-10")}

        nov = [list(z) for z in okt[10:]]           # zehn Firmen fallen weg
        for z in nov:                                # Werte wandern
            for spalte in range(7, 13):
                z[spalte] = self.rnd.randint(1, 5)
        nov += erfundene_zeilen(125, random.Random(99))[120:]  # fünf neue, andere ISINs
        for i, z in enumerate(nov[-5:]):
            z[0], z[13], z[15] = f"NEU{i} US Equity", f"NEU{i:09d}", f"Neue Firma {i}"
        k_nov = {k["id"]: k for k in self.schmiede(nov, "2026-11")}

        self.assertEqual(len(k_nov), 125)            # 120 alte (davon 10 ruhend) + 5 neue
        for kid, alt in k_okt.items():
            self.assertIn(kid, k_nov)
            self.assertEqual(k_nov[kid]["name"], alt["name"])
        ruhend = [k for k in k_nov.values() if k["status"] == "ruhend"]
        self.assertEqual(len(ruhend), 10)
        for k in ruhend:                             # eingefroren, nicht gelöscht
            self.assertEqual((k["kosten"], k["angriff"], k["leben"]),
                             (k_okt[k["id"]]["kosten"], k_okt[k["id"]]["angriff"], k_okt[k["id"]]["leben"]))
        bericht = (self.tmp / "out" / "monatswandel-2026-11.md").read_text(encoding="utf-8")
        self.assertIn("Gegenüber 2026-10", bericht)
        self.assertIn("Erwacht", bericht)
        self.assertIn("Ruht jetzt", bericht)

    def test_oeffentliche_daten_ohne_firmen(self):
        self.schmiede(erfundene_zeilen(20, self.rnd), "2026-10")
        text = (self.tmp / "out" / "karten.json").read_text(encoding="utf-8")
        self.assertNotIn("Firma ", text)
        self.assertNotIn("XX0", text)

    def test_namen_ohne_gesperrte_anklaenge_und_eindeutig(self):
        karten = self.schmiede(erfundene_zeilen(400, self.rnd), "2026-10")
        sperre = REGELN["namen"]["sperre"]
        namen = [k["name"].lower() for k in karten]
        self.assertEqual(len(namen), len(set(namen)))
        for n in namen:
            self.assertFalse([w for w in sperre if w in n], n)

    def test_daemonen_und_artefakte_eigene_voelker(self):
        self.assertEqual(REGELN["arten"]["Capital Markets"]["volk"], "Paktvolk")
        self.assertEqual(REGELN["arten"]["Software"]["volk"], "Artefakte")
        for art in REGELN["arten"].values():
            if isinstance(art, dict) and "volk" in art:
                self.assertIn(art["volk"], REGELN["voelker"])

    def test_zwillinge_werden_ein_wesen(self):
        z = erfundene_zeilen(2, self.rnd)
        z[1][15] = z[0][15]  # gleicher Firmenname, andere Gattung
        self.assertEqual(len(self.schmiede(z, "2026-10")), 1)


if __name__ == "__main__":
    unittest.main(verbosity=2)
