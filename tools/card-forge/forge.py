#!/usr/bin/env python3
"""Kartenschmiede: übersetzt monatliche Faktordaten in Itoeva-Karten.

Aufruf (Beispiel):
    python tools/card-forge/forge.py --eingabe tools/card-forge/private/card_stats.xlsx --monat 2026-10

Ergebnis:
    out/karten.json          öffentliche Kartendaten (keine Firmennamen, keine ISINs)
    out/monatswandel-<m>.md  was sich gegenüber dem Vormonat geändert hat + Balancebericht
    out/galerie.html         durchblätterbare Kartengalerie
    private/register.json    stabile Zuordnung Datenzeile -> Karte (Eigenname, Karten-Id)
    private/uebersicht-<m>.csv  Firma -> Karte, nur zur eigenen Kontrolle

Alle Spielwerte stehen in rules.json. Dieses Skript enthält nur Mechanik.
Nur Standardbibliothek plus openpyxl.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

HIER = Path(__file__).resolve().parent

SPALTEN = {
    "id": "ID",
    "isin": "ISIN",
    "name": "Name",
    "sektor": "Sector",
    "industrie": "Industry",
    "quality": "Quality Qt.",
    "lowvola": "Low Vola Qt.",
    "momentum": "Momentum Qt.",
    "value": "Value Qt.",
    "composite": "Composite Qt.",
    "size": "Size Qt.",
    "size_rang": "Size Rank",
}
FAKTOREN = ("quality", "lowvola", "momentum", "value", "composite", "size")


# --------------------------------------------------------------------------- Einlesen

def lies_tabelle(pfad: Path) -> list[dict]:
    import openpyxl

    wb = openpyxl.load_workbook(pfad, read_only=True, data_only=True)
    ws = wb.worksheets[0]
    zeilen = ws.iter_rows(values_only=True)
    kopf = [str(k).strip() if k is not None else "" for k in next(zeilen)]
    fehlend = [s for s in SPALTEN.values() if s not in kopf]
    if fehlend:
        sys.exit(f"Spalten fehlen in {pfad.name}: {', '.join(fehlend)}")
    idx = {k: kopf.index(s) for k, s in SPALTEN.items()}
    daten = []
    for z in zeilen:
        if z is None or z[idx["id"]] in (None, ""):
            continue
        d = {k: z[i] for k, i in idx.items()}
        for k in ("id", "isin", "name", "sektor", "industrie"):
            d[k] = str(d[k]).strip() if d[k] not in (None, "") else ""
        for k in FAKTOREN:
            d[k] = int(d[k] or 0)
        d["size_rang"] = float(d["size_rang"] or 0)
        daten.append(d)
    return daten


def vollstaendigkeit(d: dict) -> tuple:
    return (bool(d["isin"]), sum(1 for k in FAKTOREN if d[k] > 0))


def entdopple(daten: list[dict]) -> tuple[list[dict], list[tuple[str, list[str]]]]:
    """Mehrere Aktiengattungen derselben Firma (z. B. GOOG/GOOGL) werden ein Wesen."""
    nach_name = defaultdict(list)
    for d in daten:
        nach_name[d["name"]].append(d)
    eins, zwillinge = [], []
    for name, gruppe in nach_name.items():
        gruppe.sort(key=vollstaendigkeit, reverse=True)
        haupt = dict(gruppe[0])
        haupt["aliase"] = [g["id"] for g in gruppe[1:]] + [g["isin"] for g in gruppe[1:] if g["isin"]]
        eins.append(haupt)
        if len(gruppe) > 1:
            zwillinge.append((name, [g["id"] for g in gruppe]))
    return eins, zwillinge


# --------------------------------------------------------------------------- Register

def lade_json(pfad: Path, standard):
    return json.loads(pfad.read_text(encoding="utf-8")) if pfad.exists() else standard


def finde_im_register(d: dict, register: dict) -> str | None:
    """Schlüssel bevorzugt ISIN; Ticker und Zweitgattungen sind Aliase."""
    kandidaten = [d["isin"], d["id"], *d.get("aliase", [])]
    for k in kandidaten:
        if k and k in register["eintraege"]:
            return k
    for schluessel, e in register["eintraege"].items():
        if any(k and k in e.get("aliase", []) for k in kandidaten):
            return schluessel
    return None


def saat(text: str) -> int:
    return int(hashlib.sha256(text.encode("utf-8")).hexdigest(), 16)


VERBOTEN = {"puffling", "starlet", "wyrmling", "fennec", "gloop", "hootlet"}


VOKALE = "aeiouyäöü"


def _fuge(teile: list[str]) -> str | None:
    """Setzt Teile zusammen; None, wenn es an einer Fuge holpert ("Oro"+"ar")."""
    name = teile[0]
    for t in teile[1:]:
        if name[-1].lower() == t[0]:
            t = t[1:]  # "Lux"+"xa" -> "Luxa", keine Doppelbuchstaben an der Fuge
        if not t or (name[-1].lower() in VOKALE and t[0] in VOKALE):
            return None
        name += t
    return name


def erfinde_namen(schluessel: str, volk: dict, mythisch: bool, vergeben: set[str],
                  nutzung: Counter, sperre: list[str]) -> tuple[str, list[str]]:
    """Name aus dem Klang des Volkes. Hängt nur am Datenschlüssel, bleibt also über alle Monate.

    Drei Bauformen für Abwechslung: Anfang+Endung ("Galgor"), Anfang+Bindevokal+Endung
    ("Bramaber"), Anfang+Anfang+Endung ("Vexkalon", Mythische bevorzugt). Aus mehreren
    gültigen Kandidaten gewinnt der, dessen Teile im Volk bisher am seltensten vorkommen -
    so häufen sich keine gleichen Endungen. `sperre` verhindert ungewollte Anklänge.
    """
    anf, end = volk["anfaenge"], volk["endungen"]
    kandidaten = []
    for versuch in range(4000):
        s = saat(f"{schluessel}#{versuch}")
        a, e = anf[s % len(anf)], end[(s >> 32) % len(end)]
        form = (s >> 64) % 10
        if mythisch or versuch >= 1500:
            form = 9 if mythisch and form < 7 else form
        if form >= 8:
            b = anf[(s >> 16) % len(anf)]
            if b == a:
                continue
            teile = [a, b.lower(), e]
        elif form >= 5 and a[-1] not in VOKALE and e[0] not in VOKALE:
            teile = [a, "aeiou"[(s >> 80) % 5] if a[-1] != "r" else "i", e]
        else:
            teile = [a, e]
        name = _fuge(teile)
        if name is None:
            continue
        klein = name.lower()
        if (not 4 <= len(name) <= 11 or klein in vergeben or klein in VERBOTEN
                or any(w in klein for w in sperre)
                or any(c * 3 in klein for c in "abcdefghijklmnopqrstuvwxyz")
                or re.search(r"[^aeiouyäöü]{3,}", klein.replace("sch", "s").replace("ch", "c").replace("ck", "k"))
                or (len(teile) == 3 and len(name) > 10)):
            continue
        last = sum(nutzung[(volk["volk"], t.lower())] for t in (teile[0], teile[-1]))
        kandidaten.append((last, versuch, name, teile))
        if len(kandidaten) >= 6:
            break
    if not kandidaten:
        raise RuntimeError(f"Kein freier Name für {schluessel}")
    _, _, name, teile = min(kandidaten)
    for t in (teile[0], teile[-1]):
        nutzung[(volk["volk"], t.lower())] += 1
    return name, teile


# --------------------------------------------------------------------------- Übersetzung

def runde(x: float) -> int:
    return math.floor(x + 0.5)


def q(wert: int) -> str:
    """Quintil 0 bedeutet: Wert fehlt - neutral behandeln."""
    return str(wert if 1 <= wert <= 5 else 3)


def monatstitel(d: dict, regeln: dict) -> str:
    kandidaten = [(int(q(d[k])), rang, k) for rang, k in enumerate(("momentum", "quality", "lowvola", "value"))]
    bester, _, faktor = min(kandidaten)
    return regeln["titel"][faktor if bester <= 2 else "neutral"]


def baue_karte(d: dict, eintrag: dict, regeln: dict) -> dict:
    kurve = regeln["kurve"]
    fak = regeln["faktoren"]
    art = regeln["arten"][d["industrie"]]
    volk = regeln["voelker"][art.get("volk", d["sektor"])]
    rolle = regeln["rollen"][art["rolle"]]
    selten = regeln["seltenheit"][q(d["size"])]

    # 1) Rohwerte aus den Faktoren
    angriff_roh = fak["momentum_angriff"][q(d["momentum"])]
    leben_roh = fak["quality_leben"][q(d["quality"])]
    bonus = selten["bonus"]
    angriff_roh += bonus * rolle["anteil_angriff"]
    leben_roh += bonus * (1 - rolle["anteil_angriff"])

    # 2) Merkmale und Fähigkeiten kosten Wertpunkte
    merkmale = []
    for m in (fak["lowvola_wesen"][q(d["lowvola"])], fak["value_kurve"][q(d["value"])]["merkmal"]):
        if m:
            merkmale.append(m)
    sonder = sum(regeln["merkmale"][m]["punkte"] for m in merkmale)

    faehigkeit = None
    x = selten["stufe"]
    if x > 0:
        f = volk["faehigkeit"]
        faehigkeit = {"name": f["name"], "x": x, "text": f["text"].format(x=x)}
        sonder += f["punkte_pro_x"] * x
    gabe = None
    if selten.get("gabe"):
        gabe = {"name": volk["gabe"]["name"], "text": volk["gabe"]["text"]}
        sonder += volk["gabe"]["punkte"]

    # 3) Kristallkosten: Gesamtwert auf die Kurve, Value verschiebt
    ppk, grund = kurve["punkte_pro_kristall"], kurve["grundpunkte"]
    gesamt = angriff_roh + leben_roh + sonder
    kosten = runde((gesamt - grund) / ppk + fak["value_kurve"][q(d["value"])]["verschiebung"])
    kosten = max(kurve["min_kosten"], min(kurve["max_kosten"], kosten))
    min_werte = kurve["min_angriff"] + kurve["min_leben"]
    while ppk * kosten + grund - sonder < min_werte and kosten < kurve["max_kosten"]:
        kosten += 1

    # 4) Exakt auf die Kurve legen, Form (Angriff:Leben) aus den Faktoren behalten
    budget = ppk * kosten + grund - sonder
    werte = max(min_werte, runde(budget))
    angriff = runde(werte * angriff_roh / (angriff_roh + leben_roh))
    angriff = max(kurve["min_angriff"], min(werte - kurve["min_leben"], angriff))
    leben = werte - angriff

    form = selten["form"]
    return {
        "id": eintrag["karte_id"],
        "name": eintrag["eigenname"],
        "art": art["formen"][form],
        "form": ["Jungform", "Reife Form", "Urform"][form],
        "titel": monatstitel(d, regeln),
        "volk": volk["volk"],
        "wesen": volk["wesen"],
        "farbe": regeln["wesen"][volk["wesen"]]["farbe"],
        "hex": regeln["wesen"][volk["wesen"]]["hex"],
        "rolle": art["rolle"],
        "reihe": rolle["reihe"],
        "seltenheit": selten["name"],
        "kosten": kosten,
        "angriff": angriff,
        "leben": leben,
        "merkmale": merkmale,
        "faehigkeit": faehigkeit,
        "gabe": gabe,
        "glanz": bool(fak["composite_glanz"].get(q(d["composite"]), False)),
        "herkunft": {k: d[k] for k in ("quality", "lowvola", "momentum", "value", "size", "composite")},
        "wertpunkte": round(angriff + leben + sonder, 2),
        "status": "aktiv",
    }


# --------------------------------------------------------------------------- Berichte

def vergleiche(alt: dict, neu: dict) -> list[str]:
    zeilen = []
    for kid, k in sorted(neu.items()):
        a = alt.get(kid)
        nm = f"**{k['name']}** ({k['art']})"
        if a is None:
            zeilen.append(f"- Erwacht: {nm}, {k['seltenheit']}, {k['kosten']} Kristalle {k['angriff']}/{k['leben']}")
            continue
        if k["status"] == "ruhend" and a.get("status") != "ruhend":
            zeilen.append(f"- Ruht jetzt: {nm} - bleibt in jeder Sammlung, Werte eingefroren")
            continue
        if k["status"] == "aktiv" and a.get("status") == "ruhend":
            zeilen.append(f"- Ist zurück: {nm}")
        teile = []
        if a["art"] != k["art"]:
            teile.append(f"entwickelt sich: {a['art']} -> {k['art']}")
        if a["seltenheit"] != k["seltenheit"]:
            teile.append(f"{a['seltenheit']} -> {k['seltenheit']}")
        if (a["kosten"], a["angriff"], a["leben"]) != (k["kosten"], k["angriff"], k["leben"]):
            teile.append(f"{a['kosten']}K {a['angriff']}/{a['leben']} -> {k['kosten']}K {k['angriff']}/{k['leben']}")
        if a["merkmale"] != k["merkmale"]:
            teile.append(f"Merkmale {', '.join(a['merkmale']) or '-'} -> {', '.join(k['merkmale']) or '-'}")
        if teile:
            zeilen.append(f"- {nm}: " + "; ".join(teile))
    return zeilen


def balancebericht(karten: list[dict], regeln: dict) -> list[str]:
    aktiv = [k for k in karten if k["status"] == "aktiv"]
    ppk, grund = regeln["kurve"]["punkte_pro_kristall"], regeln["kurve"]["grundpunkte"]
    abweichung = [k for k in aktiv if abs(k["wertpunkte"] - (ppk * k["kosten"] + grund)) > 0.51]
    raritaeten = [regeln["seltenheit"][s]["name"] for s in "12345"]
    z = [f"Aktive Karten: {len(aktiv)}, ruhende: {len(karten) - len(aktiv)}",
         f"Karten neben der Kurve (>0,5 Punkte): {len(abweichung)}", "",
         "**Kristallkurve je Seltenheit**", "",
         "| Seltenheit | " + " | ".join(str(i) for i in range(1, 11)) + " | Ø |",
         "|---|" + "---|" * 11]
    for r in raritaeten:
        ks = [k["kosten"] for k in aktiv if k["seltenheit"] == r]
        c = Counter(ks)
        z.append(f"| {r} | " + " | ".join(str(c.get(i, "")) for i in range(1, 11))
                 + f" | {sum(ks) / max(1, len(ks)):.1f} |")
    z += ["", "**Heimvölker je Wesen**", "", "| Wesen | Völker | Karten | Ø Kosten | Ø Angriff | Ø Leben | Mythisch |", "|---|---|---|---|---|---|---|"]
    for w in (w for w in regeln["wesen"] if not w.startswith("_")):
        ks = [k for k in aktiv if k["wesen"] == w]
        voelker = sorted({k["volk"] for k in ks})
        n = max(1, len(ks))
        z.append(f"| {w.title()} | {', '.join(voelker)} | {len(ks)} | {sum(k['kosten'] for k in ks) / n:.1f} | "
                 f"{sum(k['angriff'] for k in ks) / n:.1f} | {sum(k['leben'] for k in ks) / n:.1f} | "
                 f"{sum(k['seltenheit'] == 'Mythisch' for k in ks)} |")
    mc = Counter(m for k in aktiv for m in k["merkmale"])
    z += ["", "**Merkmale**: " + ", ".join(f"{m} {n}" for m, n in mc.most_common()),
          "**Rollen**: " + ", ".join(f"{r} {n}" for r, n in Counter(k["rolle"] for k in aktiv).most_common()),
          f"**Glanz (Composite Q1)**: {sum(k['glanz'] for k in aktiv)}"]
    return z


# --------------------------------------------------------------------------- Hauptlauf

def schmiede(eingabe: Path, monat: str, privat: Path, aus: Path, galerie_mit_firmen: bool) -> dict:
    regeln = lade_json(HIER / "rules.json", None)
    roh = lies_tabelle(eingabe)
    daten, zwillinge = entdopple(roh)

    unbekannt = sorted({d["sektor"] for d in daten if d["sektor"] not in regeln["voelker"]}
                       | {d["industrie"] for d in daten if d["industrie"] not in regeln["arten"]})
    if unbekannt:
        sys.exit("Neue Sektoren/Industrien - bitte in rules.json eintragen: " + "; ".join(unbekannt))

    privat.mkdir(parents=True, exist_ok=True)
    aus.mkdir(parents=True, exist_ok=True)
    register = lade_json(privat / "register.json", {"naechste_nummer": 1, "eintraege": {}})
    vergeben = {e["eigenname"].lower() for e in register["eintraege"].values()}
    nutzung = Counter((e.get("volk"), t.lower()) for e in register["eintraege"].values()
                      for t in (e.get("teile") or [])[:1] + (e.get("teile") or [])[-1:])
    sperre = [w.lower() for w in regeln.get("namen", {}).get("sperre", [])]

    karten, gesehen, uebersicht = {}, set(), []
    # Feste Reihenfolge -> neue Nummern und Namen sind reproduzierbar
    for d in sorted(daten, key=lambda d: (d["isin"] or d["id"])):
        schluessel = finde_im_register(d, register)
        if schluessel is None:
            schluessel = d["isin"] or d["id"]
            volk = regeln["voelker"][regeln["arten"][d["industrie"]].get("volk", d["sektor"])]
            name, teile = erfinde_namen(schluessel, volk, q(d["size"]) == "5", vergeben, nutzung, sperre)
            vergeben.add(name.lower())
            register["eintraege"][schluessel] = {
                "karte_id": f"K{register['naechste_nummer']:04d}",
                "eigenname": name,
                "teile": teile,
                "volk": volk["volk"],
                "erstellt": monat,
                "aliase": [],
            }
            register["naechste_nummer"] += 1
        e = register["eintraege"][schluessel]
        e["aliase"] = sorted(set(e["aliase"]) | {d["id"], d["isin"], *d.get("aliase", [])} - {schluessel, ""})
        e["zuletzt"] = monat
        k = baue_karte(d, e, regeln)
        k["monat"] = monat
        karten[k["id"]] = k
        gesehen.add(schluessel)
        uebersicht.append({"karte": k["id"], "firma": d["name"], "id": d["id"], "isin": d["isin"],
                           "sektor": d["sektor"], "industrie": d["industrie"], "wesen": f"{k['name']} – {k['art']}",
                           "seltenheit": k["seltenheit"], "kosten": k["kosten"], "angriff": k["angriff"],
                           "leben": k["leben"]})

    # Vormonat: nichts geht verloren. Fehlt eine Firma, ruht ihre Karte mit den letzten Werten.
    vorher_pfad = aus / "karten.json"
    vorher = {k["id"]: k for k in lade_json(vorher_pfad, {"karten": []})["karten"]}
    vorher_monat = lade_json(vorher_pfad, {}).get("monat")
    for kid, alt in vorher.items():
        if kid not in karten:
            ruhend = dict(alt)
            ruhend["status"] = "ruhend"
            karten[kid] = ruhend

    liste = [karten[k] for k in sorted(karten)]
    (aus / "karten.json").write_text(json.dumps({"monat": monat, "anzahl": len(liste), "karten": liste},
                                                ensure_ascii=False, indent=1), encoding="utf-8")
    (privat / "register.json").write_text(json.dumps(register, ensure_ascii=False, indent=1), encoding="utf-8")
    with open(privat / f"uebersicht-{monat}.csv", "w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=list(uebersicht[0]), delimiter=";")
        w.writeheader()
        w.writerows(sorted(uebersicht, key=lambda r: r["karte"]))

    wandel = [f"# Monatswandel {monat}", ""]
    if vorher_monat and vorher_monat != monat:
        wandel += [f"Gegenüber {vorher_monat}:", ""] + (vergleiche(vorher, karten) or ["- keine Änderungen"])
    else:
        wandel += ["Erste Schmiede dieses Monats - kein Vormonat zum Vergleich."]
    if zwillinge:
        wandel += ["", "Zusammengelegte Aktiengattungen (ein Wesen je Firma): "
                   + "; ".join(f"{', '.join(ids)}" for _, ids in zwillinge)]
    wandel += ["", "## Balance", ""] + balancebericht(liste, regeln)
    (aus / f"monatswandel-{monat}.md").write_text("\n".join(wandel) + "\n", encoding="utf-8")

    vorlage = HIER / "galerie_vorlage.html"
    if vorlage.exists():
        daten_galerie = {"monat": monat, "karten": liste, "wesen": regeln["wesen"], "voelker": {
            v["volk"]: {"wesen": v["wesen"], "bild": v["bild"]} for v in regeln["voelker"].values()}}
        if galerie_mit_firmen:  # Firmennamen nur in der privaten Galerie
            daten_galerie["firmen"] = {r["karte"]: r["firma"] for r in uebersicht}
        html = vorlage.read_text(encoding="utf-8").replace(
            "/*__DATEN__*/null", json.dumps(daten_galerie, ensure_ascii=False))
        ziel = (privat if galerie_mit_firmen else aus) / "galerie.html"
        ziel.write_text(html, encoding="utf-8")

    return {"karten": liste, "zwillinge": zwillinge}


def main() -> None:
    p = argparse.ArgumentParser(description="Faktordaten -> Itoeva-Karten")
    p.add_argument("--eingabe", required=True, type=Path, help="Excel mit den Faktor-Quintilen")
    p.add_argument("--monat", required=True, help="Datenstand, z. B. 2026-10")
    p.add_argument("--privat", type=Path, default=HIER / "private", help="Register und Firmenübersicht (nicht einchecken)")
    p.add_argument("--aus", type=Path, default=HIER / "out", help="Öffentliche Kartendaten")
    p.add_argument("--galerie-mit-firmen", action="store_true",
                   help="Galerie mit Firmennamen nach --privat schreiben (zur eigenen Kontrolle)")
    a = p.parse_args()
    ergebnis = schmiede(a.eingabe, a.monat, a.privat, a.aus, a.galerie_mit_firmen)
    print(f"{len(ergebnis['karten'])} Karten geschmiedet -> {a.aus / 'karten.json'}")
    print(f"Bericht: {a.aus / f'monatswandel-{a.monat}.md'}")


if __name__ == "__main__":
    main()
