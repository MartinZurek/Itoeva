"""Artabdeckung, bestehende Werte, sichere Ausgabe und reproduzierbare Gestaltung."""
import json
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

from katalog import lese_katalog
from statkorrektur import korrigiere_stats
from wesen import ARTEN, FORMEN, HIER, bild, buch, entwuerfe, erzeugen


class WesenTest(unittest.TestCase):
    def setUp(self):
        self.regeln = json.loads((HIER / "rules.json").read_text(encoding="utf-8"))
        self.wesen = entwuerfe(self.regeln)
        self.karten = [k.als_dict() for k in lese_katalog(
            (HIER / "out/karten.json").read_bytes()).karten if k.volk == "Leuchtfeuer"]

    def test_alle_fuenf_arten_in_drei_formen_und_zwei_getrennte_helfer(self):
        wesen = [w for w in self.wesen if w["typ"] == "Kartenwesen"]
        self.assertEqual(len(wesen), 15)
        self.assertEqual({(w["name"], w["form"]) for w in wesen}, {
            (name, form) for _, art in ARTEN
            for name, form in zip(self.regeln["arten"][art]["formen"], FORMEN)})
        self.assertEqual(len({w["id"] for w in self.wesen}), 17)
        for k in self.karten:
            self.assertEqual(sum((w["name"], w["form"]) == (k["art"], k["form"])
                                 for w in wesen), 1)
        self.assertEqual(sum(not any(k["art"] == w["name"] for k in self.karten)
                             for w in wesen), 5)

    def test_helfer_folgen_bestehenden_gaben_ohne_kosten_oder_karten_id(self):
        helfer = {w["id"]: w for w in self.wesen if w["typ"] == "Spielhelfer"}
        for ident, sektor, werte, merkmal in (
            ("werkling", "Industrials", (1, 1), []),
            ("erzsplitter", "Materials", (0, 3), ["Wächter"]),
        ):
            w = helfer[ident]
            self.assertEqual((w["angriff"], w["leben"]), werte)
            self.assertEqual(w["merkmale"], merkmal)
            self.assertEqual(w["quelle"]["text"], self.regeln["voelker"][sektor]["gabe"]["text"])
            self.assertIn(f"{werte[0]}/{werte[1]}", w["quelle"]["text"])
            self.assertNotIn("kosten", w)
            self.assertIsNone(w["form"])
            self.assertFalse(w["id"].startswith("K"))

    def test_statkorrekturen_werden_im_kartenbuch_sichtbar(self):
        katalog = lese_katalog((HIER / "out/karten.json").read_bytes())
        karte = next(k for k in katalog.karten if k.volk == "Leuchtfeuer")
        korrigiert = korrigiere_stats(katalog, karte.id, angriff=97, leben=98, kosten=99)
        html = buch(self.wesen, [k.als_dict() for k in korrigiert.karten if k.volk == "Leuchtfeuer"])
        self.assertIn("97 Angriff", html)
        self.assertIn("98 Leben", html)
        self.assertIn("99 Kristalle", html)
        self.assertEqual(next(k for k in korrigiert.karten if k.id == karte.id).form, karte.form)

    def test_namen_und_effekttexte_werden_als_text_behandelt(self):
        self.karten[0]["name"] = '<script>alert("x")</script>'
        self.karten[0]["faehigkeit"] = dict(text='<img src=x onerror="x">')
        html = buch(self.wesen, self.karten)
        self.assertNotIn('<script>alert', html)
        self.assertNotIn('<img src=x', html)
        self.assertIn('&lt;script&gt;', html)

    def test_svg_sind_eigenstaendig_und_ohne_aktive_inhalte(self):
        ns = '{http://www.w3.org/2000/svg}'
        for w in self.wesen:
            svg = bild(w)
            root = ET.fromstring(svg)
            self.assertEqual(root.attrib['viewBox'], '0 0 480 270')
            self.assertEqual(root.find(ns + 'title').text, w['name'] + ' – Vektorentwurf')
            ids = {e.attrib['id'] for e in root.iter() if 'id' in e.attrib}
            self.assertEqual(ids, {'name', 'material', 'feuer', 'aussparung'})
            self.assertFalse(any(e.tag in {ns+'script', ns+'image', ns+'foreignObject'} for e in root.iter()))
            self.assertNotIn('href=', svg)

    def test_ausgaben_reproduzierbar_und_katalog_unveraendert(self):
        vorher = (HIER / 'out/karten.json').read_bytes()
        with tempfile.TemporaryDirectory() as tmp:
            ziel = Path(tmp)
            erzeugen(ziel)
            for datei in ziel.rglob('*'):
                if datei.is_file():
                    self.assertEqual(datei.read_bytes(), (HIER / 'out' / datei.relative_to(ziel)).read_bytes())
            daten = json.loads((ziel / 'minions.json').read_text(encoding='utf-8'))
            referenzen = [ident for w in daten['wesen'] for ident in w['karten_ids']]
            self.assertEqual(set(referenzen), {k['id'] for k in self.karten})
            self.assertEqual(len(referenzen), len(self.karten))
            for w in daten['wesen']:
                self.assertTrue((ziel / w['bild']).is_file())
                self.assertNotIn('familie', w)
                self.assertNotIn('stufe', w)
        self.assertEqual(vorher, (HIER / 'out/karten.json').read_bytes())


if __name__ == '__main__':
    unittest.main()
