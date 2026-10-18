"""Vertragstests mit handgeschriebenen, erfundenen Karten; unabhaengig von Faktordaten."""
from __future__ import annotations

import copy
import json
import sys
import unittest
from dataclasses import FrozenInstanceError
from pathlib import Path

HIER = Path(__file__).resolve().parent
sys.path.insert(0, str(HIER))
from katalog import (KatalogFehler, altbestand_uebernehmen, katalog_aus_daten,
                     katalog_json, katalog_sha256, lese_katalog)


class KatalogTest(unittest.TestCase):
    def setUp(self):
        self.text = (HIER / "fixtures/katalog-v1.json").read_text(encoding="utf-8")
        self.daten = json.loads(self.text)

    def test_aktive_und_ruhende_karten_inklusive_monaten_lesen(self):
        k = lese_katalog(self.text)
        self.assertEqual((k.schema_version, k.monat), (1, "2026-10"))
        self.assertEqual([c.status for c in k.karten], ["aktiv", "ruhend"])
        self.assertEqual([c.monat for c in k.karten], ["2026-10", "2026-09"])
        self.assertEqual(k.karten[1].faehigkeit.x, 1)
        self.assertEqual(k.karten[1].gabe.name, "Ewiges Feuer")
        self.assertTrue(k.karten[1].glanz)

    def test_modelle_sind_unveraenderlich(self):
        k = lese_katalog(self.text)
        with self.assertRaises(FrozenInstanceError):
            k.karten[0].leben = 99
        with self.assertRaises(FrozenInstanceError):
            k.karten[0].faehigkeit.x = 99
        self.assertIsInstance(k.karten[1].merkmale, tuple)

    def test_unbekannte_felder_werden_auf_jeder_ebene_ignoriert(self):
        d = copy.deepcopy(self.daten)
        d["zukuenftig"] = {"intern": "ERFUNDEN_GEHEIM"}
        for c in d["karten"]:
            c["herkunft"] = {"quality": "ERFUNDEN_GEHEIM"}
            c["faehigkeit"]["intern"] = "ERFUNDEN_GEHEIM"
            if c["gabe"]:
                c["gabe"]["intern"] = "ERFUNDEN_GEHEIM"
        parsed = lese_katalog(json.dumps(d))
        self.assertEqual(parsed, lese_katalog(self.text))
        self.assertNotIn("ERFUNDEN_GEHEIM", katalog_json(parsed))

    def test_zukunftsversionen_und_falsche_typen_nicht_als_v1_lesen(self):
        for version in (None, 0, 2, -1, True, 1.0, "1"):
            with self.subTest(version=version):
                d = copy.deepcopy(self.daten)
                d["schema_version"] = version
                with self.assertRaises(KatalogFehler):
                    katalog_aus_daten(d)

    def test_legacy_uebernahme_erfordert_expliziten_aufruf(self):
        d = copy.deepcopy(self.daten)
        del d["schema_version"]
        with self.assertRaises(KatalogFehler):
            katalog_aus_daten(d)
        self.assertEqual(altbestand_uebernehmen(d), lese_katalog(self.text))
        self.assertEqual(lese_katalog(json.dumps(d), erlaube_altbestand=True), lese_katalog(self.text))
        with self.assertRaises(KatalogFehler):
            altbestand_uebernehmen(self.daten)
        d["schema_version"] = 999
        with self.assertRaises(KatalogFehler):
            altbestand_uebernehmen(d)
        with self.assertRaises(KatalogFehler):
            lese_katalog(json.dumps(d), erlaube_altbestand=True)

    def test_doppelte_ids_namen_und_falsche_anzahl_ablehnen(self):
        for feld in ("id", "name", "anzahl"):
            with self.subTest(feld=feld):
                d = copy.deepcopy(self.daten)
                if feld == "anzahl":
                    d["anzahl"] = 1
                else:
                    d["karten"][1][feld] = d["karten"][0][feld]
                with self.assertRaises(KatalogFehler):
                    katalog_aus_daten(d)

    def test_falsche_monate_ablehnen(self):
        for monat in ("2026-00", "2026-13", "0000-01", "2026-1", "2026-10/../privat", None):
            with self.subTest(monat=monat):
                d = copy.deepcopy(self.daten)
                d["monat"] = monat
                with self.assertRaises(KatalogFehler):
                    katalog_aus_daten(d)
        for index, monat in ((0, "2026-09"), (1, "2026-11")):
            d = copy.deepcopy(self.daten)
            d["karten"][index]["monat"] = monat
            with self.assertRaises(KatalogFehler):
                katalog_aus_daten(d)

    def test_kartentypen_und_pflichtfelder_ablehnen(self):
        faelle = (("kosten", True), ("angriff", 0), ("leben", 1.5), ("kosten", 2**31),
                  ("hex", "rot"), ("glanz", 1), ("merkmale", "Wächter"), ("status", "weg"),
                  ("form", "Vierte Form"), ("wertpunkte", float("nan")), ("wertpunkte", float("inf")),
                  ("faehigkeit", {"name": "Test", "x": True, "text": "Test"}), ("gabe", {}))
        for feld, wert in faelle:
            with self.subTest(feld=feld, wert=wert):
                d = copy.deepcopy(self.daten)
                d["karten"][0][feld] = wert
                with self.assertRaises(KatalogFehler):
                    katalog_aus_daten(d)
        d = copy.deepcopy(self.daten)
        del d["karten"][0]["volk"]
        with self.assertRaises(KatalogFehler):
            katalog_aus_daten(d)

    def test_mehrdeutiges_oder_unlesbares_json_ablehnen(self):
        for text in ('{"schema_version":1,"schema_version":2}', '{"intern":NaN}',
                     '{"intern":Infinity}', '{"intern":-Infinity}', '{', '[]', b'\xff',
                     self.text.encode("utf-16")):
            with self.subTest(text=text):
                with self.assertRaises(KatalogFehler):
                    lese_katalog(text)

    def test_fehlertexte_enthalten_keine_eingabewerte(self):
        self.daten["karten"][0]["hex"] = "ERFUNDEN_GEHEIM"
        with self.assertRaises(KatalogFehler) as fehler:
            katalog_aus_daten(self.daten)
        self.assertNotIn("ERFUNDEN_GEHEIM", str(fehler.exception))

    def test_unpaariges_unicode_ist_kein_exportierbarer_text(self):
        self.daten["karten"][0]["name"] = "\ud800"
        with self.assertRaises(KatalogFehler):
            lese_katalog(json.dumps(self.daten))

    def test_roundtrip_und_bytegenaue_dateireferenz(self):
        k = lese_katalog(self.text)
        raw = katalog_json(k).encode("utf-8")
        self.assertEqual(lese_katalog(raw), k)
        self.assertEqual(katalog_json(lese_katalog(raw)).encode("utf-8"), raw)
        self.assertEqual(len(katalog_sha256(raw)), 64)
        self.assertNotEqual(katalog_sha256(raw), katalog_sha256(raw + b"\n"))

    def test_monatswechsel_aendert_dateireferenz_aber_keine_identitaet(self):
        vorher = lese_katalog(self.text)
        self.daten["monat"] = "2026-11"
        self.daten["karten"][0]["monat"] = "2026-11"
        self.daten["karten"][0]["leben"] = 2
        nachher = katalog_aus_daten(self.daten)
        self.assertEqual([(c.id, c.name) for c in vorher.karten], [(c.id, c.name) for c in nachher.karten])
        self.assertNotEqual(katalog_sha256(katalog_json(vorher).encode()),
                            katalog_sha256(katalog_json(nachher).encode()))
        self.assertEqual(vorher.karten[0].leben, 3)

    def test_aktueller_oeffentlicher_bestand_ist_lesbar(self):
        raw = (HIER / "out/karten.json").read_bytes()
        katalog = lese_katalog(raw)
        self.assertEqual(katalog.schema_version, 1)
        self.assertGreater(len(katalog.karten), 0)
        self.assertEqual(raw, katalog_json(katalog).encode("utf-8"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
