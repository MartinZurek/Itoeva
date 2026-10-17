#!/usr/bin/env python3
"""Prüft die Kartenschmiede ohne echte Daten: Kurve, Stabilität über Monate, nichts geht verloren.

    python tools/card-forge/test_forge.py
"""
from __future__ import annotations

import json
import csv
import random
import re
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

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
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.tmp = Path(tmp.name)
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
        zeilen = erfundene_zeilen(20, self.rnd)
        zeilen[1][15] = zeilen[0][15]
        self.schmiede(zeilen, "2026-10")
        for datei in (self.tmp / "out").iterdir():
            with self.subTest(datei=datei.name):
                text = datei.read_text(encoding="utf-8")
                for z in zeilen:
                    for marker in (z[0], z[13], z[15]):
                        self.assertNotIn(marker, text)
                self.assertNotIn('"herkunft"', text)
                self.assertNotIn('"faktoren"', text)
        bericht = (self.tmp / "out" / "monatswandel-2026-10.md").read_text(encoding="utf-8")
        self.assertIn("Zusammengelegte Gruppen: 1", bericht)

    def test_export_verwirft_unbekannte_felder_auch_bei_ruhenden_karten(self):
        zeilen = erfundene_zeilen(2, self.rnd)
        original = forge.baue_karte

        def mit_privaten_zusatzfeldern(*args):
            k = original(*args)
            k["intern"] = "SYNTHETISCH_GEHEIM"
            if k["faehigkeit"]:
                k["faehigkeit"]["datenquelle"] = "SYNTHETISCH_GEHEIM"
            if k["gabe"]:
                k["gabe"]["datenquelle"] = "SYNTHETISCH_GEHEIM"
            return k

        zeilen[0][12] = 5
        with patch.object(forge, "baue_karte", side_effect=mit_privaten_zusatzfeldern):
            self.schmiede(zeilen, "2026-10")
        for datei in (self.tmp / "out").iterdir():
            self.assertNotIn("SYNTHETISCH_GEHEIM", datei.read_text(encoding="utf-8"))
        # Auch Altdateien duerfen private Felder nicht als ruhende Karte durchreichen.
        pfad = self.tmp / "out" / "karten.json"
        alt = json.loads(pfad.read_text(encoding="utf-8"))
        for k in alt["karten"]:
            k["herkunft"] = {"quality": "SYNTHETISCH_GEHEIM"}
            k["intern"] = "SYNTHETISCH_GEHEIM"
            if k["faehigkeit"]:
                k["faehigkeit"]["datenquelle"] = "SYNTHETISCH_GEHEIM"
            if k["gabe"]:
                k["gabe"]["datenquelle"] = "SYNTHETISCH_GEHEIM"
        pfad.write_text(json.dumps(alt), encoding="utf-8")
        karten = self.schmiede(zeilen[1:], "2026-11")
        self.assertEqual(sum(k["status"] == "ruhend" for k in karten), 1)
        self.assertNotIn("SYNTHETISCH_GEHEIM", json.dumps(karten))
        for datei in (self.tmp / "out").glob("*2026-11*"):
            self.assertNotIn("SYNTHETISCH_GEHEIM", datei.read_text(encoding="utf-8"))
        for name in ("karten.json", "galerie.html"):
            self.assertNotIn("SYNTHETISCH_GEHEIM", (self.tmp / "out" / name).read_text(encoding="utf-8"))

    def test_private_galerie_bleibt_getrennt_und_oeffentliche_wird_aktualisiert(self):
        zeilen = erfundene_zeilen(2, self.rnd)
        zeilen[0][15] = "Erfunden </script><script>PRIVAT_MARKER</script>"
        x = self.tmp / "test.xlsx"
        schreibe_xlsx(x, zeilen)
        forge.schmiede(x, "2026-10", self.tmp / "private", self.tmp / "out", True)
        privat = (self.tmp / "private" / "galerie.html").read_text(encoding="utf-8")
        self.assertIn("PRIVAT_MARKER", privat)
        self.assertIn('"faktoren"', privat)
        self.assertNotIn("</script><script>PRIVAT_MARKER", privat)
        self.assertIn(zeilen[0][13], (self.tmp / "private" / "uebersicht-2026-10.csv").read_text(encoding="utf-8-sig"))
        self.schmiede(zeilen[1:], "2026-11")
        forge.schmiede(x, "2026-12", self.tmp / "private", self.tmp / "out", True)
        public = (self.tmp / "out" / "galerie.html").read_text(encoding="utf-8")
        self.assertNotIn("PRIVAT_MARKER", public)
        self.assertIn('"monat": "2026-12"', public)

    def stand(self):
        return {str(p.relative_to(self.tmp)): p.read_bytes()
                for ordner in ("out", "private")
                for p in (self.tmp / ordner).rglob("*") if p.is_file()}

    def test_fehlendes_register_bricht_vor_jeder_ausgabe_ab(self):
        self.schmiede(erfundene_zeilen(2, self.rnd), "2026-10")
        (self.tmp / "private" / "register.json").unlink()
        vorher = self.stand()
        with self.assertRaisesRegex(ValueError, "Register"):
            self.schmiede(erfundene_zeilen(3, random.Random(9)), "2026-11")
        self.assertEqual(vorher, self.stand())

    def test_defektes_oder_unpassendes_register_schreibt_nichts(self):
        zeilen = erfundene_zeilen(2, self.rnd)
        self.schmiede(zeilen, "2026-10")
        pfad = self.tmp / "private" / "register.json"
        original = pfad.read_text(encoding="utf-8")
        for fall in ("json", "leer", "zaehler", "id_doppelt", "alias_doppelt", "name", "eintrag_fehlt"):
            with self.subTest(fall=fall):
                r = json.loads(original)
                eintraege = list(r["eintraege"].values())
                if fall == "leer":
                    r["eintraege"] = {}
                elif fall == "zaehler":
                    r["naechste_nummer"] = 1
                elif fall == "id_doppelt":
                    eintraege[1]["karte_id"] = eintraege[0]["karte_id"]
                elif fall == "name":
                    eintraege[0]["eigenname"] = "Anderername"
                elif fall == "alias_doppelt":
                    eintraege[0]["aliase"] = [list(r["eintraege"])[1]]
                elif fall == "eintrag_fehlt":
                    del r["eintraege"][next(iter(r["eintraege"]))]
                pfad.write_text("{" if fall == "json" else json.dumps(r), encoding="utf-8")
                vorher = self.stand()
                with self.assertRaises(ValueError):
                    self.schmiede(zeilen, "2026-11")
                self.assertEqual(vorher, self.stand())

    def test_leere_eingabe_schreibt_nichts(self):
        self.schmiede(erfundene_zeilen(2, self.rnd), "2026-10")
        vorher = self.stand()
        with self.assertRaisesRegex(ValueError, "Datenzeilen"):
            self.schmiede([], "2026-11")
        self.assertEqual(vorher, self.stand())

    def test_fehlender_katalog_mit_alten_ausgaben_schreibt_nichts(self):
        zeilen = erfundene_zeilen(2, self.rnd)
        self.schmiede(zeilen, "2026-10")
        (self.tmp / "out" / "karten.json").unlink()
        vorher = self.stand()
        with self.assertRaisesRegex(ValueError, "Kartenkatalog fehlt"):
            self.schmiede(zeilen, "2026-11")
        self.assertEqual(vorher, self.stand())

    def test_legacy_katalog_wird_ohne_identitaetswechsel_versioniert(self):
        zeilen = erfundene_zeilen(2, self.rnd)
        alt = self.schmiede(zeilen, "2026-10")
        pfad = self.tmp / "out" / "karten.json"
        d = json.loads(pfad.read_text(encoding="utf-8"))
        del d["schema_version"]
        pfad.write_text(json.dumps(d), encoding="utf-8")
        neu = self.schmiede(zeilen, "2026-10")
        self.assertEqual(alt, neu)
        self.assertEqual(json.loads(pfad.read_text(encoding="utf-8"))["schema_version"], 1)

    def test_inkompatibler_katalog_wird_nicht_ueberschrieben(self):
        zeilen = erfundene_zeilen(2, self.rnd)
        self.schmiede(zeilen, "2026-10")
        pfad = self.tmp / "out" / "karten.json"
        original = json.loads(pfad.read_text(encoding="utf-8"))
        for fall in ("zukunft", "anzahl", "pflichtfeld", "doppelfelder"):
            with self.subTest(fall=fall):
                d = json.loads(json.dumps(original))
                if fall == "zukunft":
                    d["schema_version"] = 2
                elif fall == "anzahl":
                    d["anzahl"] = 1
                elif fall == "pflichtfeld":
                    del d["karten"][0]["leben"]
                text = json.dumps(d)
                if fall == "doppelfelder":
                    text = text.replace('"schema_version": 1', '"schema_version": 1, "schema_version": 1')
                pfad.write_text(text, encoding="utf-8")
                vorher = self.stand()
                with self.assertRaises(ValueError):
                    self.schmiede(zeilen, "2026-11")
                self.assertEqual(vorher, self.stand())

    def test_abgebrochener_registeraustausch_erhaelt_alle_dateien(self):
        zeilen = erfundene_zeilen(2, self.rnd)
        self.schmiede(zeilen, "2026-10")
        vorher = self.stand()
        with patch.object(forge.os, "replace", side_effect=OSError("Erfundener Schreibfehler")):
            with self.assertRaises(OSError):
                self.schmiede(zeilen, "2026-11")
        self.assertEqual(vorher, self.stand())

    def test_abbruch_nach_registerspeicherung_kann_sicher_wiederholt_werden(self):
        zeilen = erfundene_zeilen(1, self.rnd)
        alt = self.schmiede(zeilen, "2026-10")
        neue = erfundene_zeilen(1, random.Random(99))[0]
        neue[0], neue[13], neue[15] = "SYNTH_NEU", "XX_NEU", "Erfunden neu"
        vorher = self.stand()
        ersetzen = forge.os.replace
        zaehler = 0

        def beim_zweiten_austausch_abbrechen(*args):
            nonlocal zaehler
            zaehler += 1
            if zaehler == 2:
                raise OSError("Erfundener Abbruch nach Registerspeicherung")
            return ersetzen(*args)

        with patch.object(forge.os, "replace", side_effect=beim_zweiten_austausch_abbrechen):
            with self.assertRaises(OSError):
                self.schmiede(zeilen + [neue], "2026-11")
        for p, inhalt in self.stand().items():
            if p != str(Path("private/register.json")):
                self.assertEqual(vorher[p], inhalt)
        pfad = self.tmp / "private" / "register.json"
        register = pfad.read_bytes()
        neu = self.schmiede(zeilen + [neue], "2026-11")
        self.assertEqual(register, pfad.read_bytes())
        self.assertEqual(len(neu), 2)
        self.assertIn((alt[0]["id"], alt[0]["name"]), [(k["id"], k["name"]) for k in neu])
        with open(self.tmp / "private" / "uebersicht-2026-11.csv", newline="", encoding="utf-8-sig") as f:
            self.assertEqual(len(list(csv.reader(f, delimiter=";"))), 3)

    def test_leere_namen_fassen_verschiedene_identitaeten_nicht_zusammen(self):
        zeilen = erfundene_zeilen(2, self.rnd)
        for z in zeilen:
            z[15] = ""
        self.assertEqual(len(self.schmiede(zeilen, "2026-10")), 2)

    def test_doppelte_datenidentitaet_schreibt_nichts(self):
        zeilen = erfundene_zeilen(2, self.rnd)
        zeilen[1][13] = zeilen[0][13]
        with self.assertRaisesRegex(ValueError, "Kartenidentitaet"):
            self.schmiede(zeilen, "2026-10")
        self.assertEqual(self.stand(), {})

    def test_eingecheckte_ausgaben_enthalten_nur_oeffentliche_kartendaten(self):
        out = HIER / "out"
        daten = json.loads((out / "karten.json").read_text(encoding="utf-8"))
        self.assertEqual(set(daten), {"schema_version", "monat", "anzahl", "karten"})
        html = (out / "galerie.html").read_text(encoding="utf-8")
        eingebettet, _ = json.JSONDecoder().raw_decode(html.split("const D = ", 1)[1])
        self.assertEqual(daten["anzahl"], len(daten["karten"]))
        self.assertEqual(eingebettet["karten"], daten["karten"])
        self.assertEqual(eingebettet["monat"], daten["monat"])
        self.assertEqual(daten["schema_version"], 1)
        self.assertEqual(eingebettet["schema_version"], daten["schema_version"])
        self.assertNotIn("firmen", eingebettet)
        self.assertNotIn("faktoren", eingebettet)
        for k in daten["karten"]:
            self.assertNotIn("herkunft", k)
            self.assertEqual(set(k), {"id", "name", "art", "form", "titel", "volk", "wesen",
                                     "farbe", "hex", "rolle", "reihe", "seltenheit", "kosten", "angriff",
                                     "leben", "merkmale", "faehigkeit", "gabe", "glanz", "wertpunkte",
                                     "status", "monat"})
            if k["faehigkeit"]:
                self.assertEqual(set(k["faehigkeit"]), {"name", "x", "text"})
            if k["gabe"]:
                self.assertEqual(set(k["gabe"]), {"name", "text"})
        for bericht in out.glob("monatswandel-*.md"):
            text = bericht.read_text(encoding="utf-8")
            self.assertNotIn("Zusammengelegte Aktiengattungen", text)
            self.assertIsNone(re.search(r"\b[A-Z0-9/]+ (?:US|CN) Equity\b", text))

    def test_wiedererwachen_behaelt_id_und_namen(self):
        zeilen = erfundene_zeilen(2, self.rnd)
        alt = self.schmiede(zeilen, "2026-10")
        self.schmiede(zeilen[1:], "2026-11")
        neu = self.schmiede(zeilen, "2026-12")
        self.assertEqual([(k["id"], k["name"]) for k in alt], [(k["id"], k["name"]) for k in neu])
        self.assertTrue(all(k["status"] == "aktiv" for k in neu))

    def test_private_und_oeffentliche_pfade_duerfen_nicht_ueberlappen(self):
        x = self.tmp / "test.xlsx"
        schreibe_xlsx(x, erfundene_zeilen(2, self.rnd))
        for privat, aus in (("out", "out"), ("out/private", "out"), ("private", "private/out")):
            with self.subTest(privat=privat, aus=aus):
                with self.assertRaises(ValueError):
                    forge.schmiede(x, "2026-10", self.tmp / privat, self.tmp / aus, False)
        self.assertFalse((self.tmp / "out").exists())
        self.assertFalse((self.tmp / "private").exists())

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
