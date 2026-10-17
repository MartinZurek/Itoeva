"""Zweimonatsnachweis fuer Variante A, ausschliesslich mit erfundenen Daten."""
from __future__ import annotations

import json
import sys
import tempfile
import unittest
from dataclasses import FrozenInstanceError
from pathlib import Path

HIER = Path(__file__).resolve().parent
sys.path.insert(0, str(HIER))
from edition import ErworbeneAusgabe, KatalogSnapshot
from katalog import KatalogFehler, lese_katalog


class EditionsTest(unittest.TestCase):
    def setUp(self):
        self.okt = json.loads((HIER / "fixtures/katalog-v1.json").read_text(encoding="utf-8"))
        k = self.okt["karten"][0]
        k["form"], k["seltenheit"], k["glanz"] = "Urform", "Mythisch", True

    def snapshot(self, daten):
        return KatalogSnapshot(json.dumps(daten, ensure_ascii=False).encode("utf-8"))

    def november(self):
        daten = json.loads(json.dumps(self.okt))
        daten["monat"] = "2026-11"
        k = daten["karten"][0]
        k.update(monat="2026-11", form="Jungform", seltenheit="Gewöhnlich", glanz=False,
                 kosten=1, angriff=1, leben=2, wertpunkte=3, faehigkeit=None, gabe=None)
        return daten

    def test_monatswechsel_stuft_erworbene_werte_form_und_glanz_nicht_zurueck(self):
        okt = self.snapshot(self.okt)
        besitz = ErworbeneAusgabe(okt, "K0001")
        alt = besitz.karte
        nov = self.snapshot(self.november())
        aktuell = nov.katalog.karten[0]
        self.assertEqual(alt.id, aktuell.id)
        self.assertNotEqual(alt, aktuell)
        self.assertEqual(besitz.karte, alt)
        self.assertEqual((besitz.karte.form, besitz.karte.seltenheit, besitz.karte.glanz),
                         ("Urform", "Mythisch", True))
        self.assertEqual((besitz.karte.kosten, besitz.karte.angriff, besitz.karte.leben), (2, 1, 3))
        self.assertEqual(besitz.snapshot.katalog.monat, "2026-10")

    def test_weitere_erworbene_ausgabe_bleibt_getrennt(self):
        okt = ErworbeneAusgabe(self.snapshot(self.okt), "K0001")
        nov = ErworbeneAusgabe(self.snapshot(self.november()), "K0001")
        self.assertEqual(okt.karten_id, nov.karten_id)
        self.assertNotEqual(okt.referenz, nov.referenz)
        self.assertEqual(len({okt.referenz, nov.referenz}), 2)
        self.assertTrue(okt.karte.glanz)
        self.assertFalse(nov.karte.glanz)

    def test_ruhend_entfernt_keinen_bereits_gewaehrten_erwerb(self):
        besitz = ErworbeneAusgabe(self.snapshot(self.okt), "K0001")
        nov = json.loads(json.dumps(self.okt))
        nov["monat"] = "2026-11"
        nov["karten"][0]["status"] = "ruhend"
        katalog = self.snapshot(nov).katalog
        self.assertEqual(katalog.karten[0].status, "ruhend")
        self.assertEqual(besitz.karte.status, "aktiv")  # Historischer Status, keine heutige Kampfzulassung.
        self.assertEqual(besitz.karte.monat, "2026-10")

    def test_snapshot_datei_bewahrt_ausgabe_bei_neuem_katalog(self):
        ausgabe = ErworbeneAusgabe(self.snapshot(self.okt), "K0001")
        with tempfile.TemporaryDirectory() as tmp:
            archiv = Path(tmp) / f"{ausgabe.snapshot.sha256}.json"
            archiv.write_bytes(ausgabe.snapshot.inhalt)
            aktuell = Path(tmp) / "aktuell.json"
            aktuell.write_bytes(self.snapshot(self.november()).inhalt)
            self.assertFalse(lese_katalog(aktuell.read_bytes()).karten[0].glanz)
            wiederhergestellt = ErworbeneAusgabe(KatalogSnapshot(archiv.read_bytes()), ausgabe.karten_id)
            self.assertEqual(wiederhergestellt, ausgabe)

    def test_archiv_uebernimmt_keine_unbekannten_privaten_zusatzdaten(self):
        self.okt["privat"] = "NUR_ERFUNDEN_GEHEIM"
        self.okt["karten"][0]["herkunft"] = {"quality": "NUR_ERFUNDEN_GEHEIM"}
        snapshot = self.snapshot(self.okt)
        self.assertNotIn(b"NUR_ERFUNDEN_GEHEIM", snapshot.inhalt)
        self.assertEqual(KatalogSnapshot(snapshot.inhalt), snapshot)

    def test_edition_und_snapshot_sind_unveraenderlich(self):
        ausgabe = ErworbeneAusgabe(self.snapshot(self.okt), "K0001")
        with self.assertRaises(FrozenInstanceError):
            ausgabe.karten_id = "K0002"
        with self.assertRaises(FrozenInstanceError):
            ausgabe.snapshot.inhalt = b"{}"

    def test_fehlende_karte_und_unlesbarer_snapshot_sind_kein_erwerb(self):
        with self.assertRaises(KatalogFehler):
            ErworbeneAusgabe(self.snapshot(self.okt), "K9999")
        with self.assertRaises(KatalogFehler):
            KatalogSnapshot(b"{}")


if __name__ == "__main__":
    unittest.main(verbosity=2)
