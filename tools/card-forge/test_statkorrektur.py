"""Bewusste Statkorrekturen duerfen Karten aendern, ohne erworbene Darstellung zu verlieren."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

HIER = Path(__file__).resolve().parent
sys.path.insert(0, str(HIER))
from edition import ErworbeneAusgabe, KatalogSnapshot
from katalog import KatalogFehler, katalog_json, lese_katalog
from statkorrektur import korrigiere_stats


class StatkorrekturTest(unittest.TestCase):
    def setUp(self):
        self.raw = (HIER / "fixtures/katalog-v1.json").read_bytes()
        self.katalog = lese_katalog(self.raw)

    def test_stats_aendern_sich_ohne_karte_neu_anzulegen(self):
        neu = korrigiere_stats(self.katalog, "K0001", angriff=4, leben=5, kosten=3)
        altkarte, neuekarte = self.katalog.karten[0], neu.karten[0]
        self.assertEqual((neuekarte.angriff, neuekarte.leben, neuekarte.kosten), (4, 5, 3))
        self.assertEqual((neuekarte.id, neuekarte.name), (altkarte.id, altkarte.name))
        self.assertEqual(neu.monat, self.katalog.monat)
        self.assertEqual(len(neu.karten), len(self.katalog.karten))
        self.assertEqual(neu.karten[1], self.katalog.karten[1])
        self.assertEqual((altkarte.angriff, altkarte.leben, altkarte.kosten), (1, 3, 2))
        for f, v in altkarte.als_dict().items():
            if f not in ("angriff", "leben", "kosten", "wertpunkte"):
                self.assertEqual(neuekarte.als_dict()[f], v)

    def test_wertpunkte_folgen_den_stats_ohne_versteckte_neuverteilung(self):
        neu = korrigiere_stats(self.katalog, "K0001", angriff=4)
        self.assertEqual(neu.karten[0].leben, 3)
        self.assertEqual(neu.karten[0].kosten, 2)
        self.assertEqual(neu.karten[0].wertpunkte, 8)
        nur_kosten = korrigiere_stats(neu, "K0001", kosten=4)
        self.assertEqual(nur_kosten.karten[0].wertpunkte, 8)
        self.assertEqual(nur_kosten.karten[0].angriff, 4)

    def test_erworbene_karte_uebernimmt_bewusste_korrektur_auch_nach_wiederherstellung(self):
        alt = ErworbeneAusgabe(KatalogSnapshot(self.raw), "K0002")
        neu = alt.mit_statkorrektur(angriff=5, leben=6, kosten=7)
        self.assertEqual((neu.karte.angriff, neu.karte.leben, neu.karte.kosten), (5, 6, 7))
        self.assertEqual((neu.karten_id, neu.karte.name), (alt.karten_id, alt.karte.name))
        self.assertEqual((neu.karte.form, neu.karte.glanz, neu.karte.seltenheit), ("Urform", True, "Mythisch"))
        self.assertEqual(neu.karte.monat, alt.karte.monat)
        self.assertEqual(neu.snapshot.katalog.karten[0], alt.snapshot.katalog.karten[0])
        self.assertNotEqual(neu.referenz, alt.referenz)
        self.assertEqual(ErworbeneAusgabe(KatalogSnapshot(neu.snapshot.inhalt), neu.karten_id), neu)
        self.assertEqual(alt.karte.angriff, 2)

    def test_monatsimport_ueberschreibt_eine_korrigierte_ausgabe_nicht(self):
        ausgabe = ErworbeneAusgabe(KatalogSnapshot(self.raw), "K0002").mit_statkorrektur(angriff=5)
        november = json.loads(self.raw)
        november["monat"] = "2026-11"
        for k in november["karten"]:
            if k["status"] == "aktiv":
                k["monat"] = "2026-11"
        november["karten"][1].update(angriff=1, leben=1, form="Jungform", glanz=False)
        aktueller_katalog = lese_katalog(json.dumps(november))
        self.assertEqual(aktueller_katalog.karten[1].angriff, 1)
        self.assertEqual((ausgabe.karte.angriff, ausgabe.karte.form, ausgabe.karte.glanz), (5, "Urform", True))

    def test_mehrere_erworbene_ausgaben_derselben_karte_sind_korrigierbar(self):
        okt = ErworbeneAusgabe(KatalogSnapshot(self.raw), "K0001")
        daten = json.loads(self.raw)
        daten["karten"][0]["form"] = "Reife Form"
        weitere = ErworbeneAusgabe(KatalogSnapshot(json.dumps(daten).encode()), "K0001")
        korrigiert = [a.mit_statkorrektur(leben=7) for a in (okt, weitere)]
        self.assertTrue(all(a.karte.leben == 7 for a in korrigiert))
        self.assertEqual([a.karte.form for a in korrigiert], ["Jungform", "Reife Form"])

    def test_leere_ungueltige_und_unbekannte_korrekturen_veraendern_nichts(self):
        vorher = katalog_json(self.katalog)
        for kwargs in ({}, {"angriff": 0}, {"leben": True}, {"kosten": 1.5}, {"leben": 2**31}):
            with self.subTest(kwargs=kwargs):
                with self.assertRaises(KatalogFehler):
                    korrigiere_stats(self.katalog, "K0001", **kwargs)
                self.assertEqual(katalog_json(self.katalog), vorher)
        with self.assertRaises(KatalogFehler):
            korrigiere_stats(self.katalog, "K9999", angriff=4)
        self.assertEqual(katalog_json(self.katalog), vorher)

    def test_gleiche_statkorrektur_ist_idempotent(self):
        einmal = ErworbeneAusgabe(KatalogSnapshot(self.raw), "K0001").mit_statkorrektur(angriff=4)
        zweimal = einmal.mit_statkorrektur(angriff=4)
        self.assertEqual(einmal, zweimal)
        self.assertEqual(einmal.referenz, zweimal.referenz)


if __name__ == "__main__":
    unittest.main(verbosity=2)
