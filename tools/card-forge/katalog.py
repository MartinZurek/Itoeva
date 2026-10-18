"""Engineunabhaengiger JSON-Katalogvertrag; keine Kampf- oder Besitzlogik.

    python tools/card-forge/katalog.py tools/card-forge/out/karten.json
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
from dataclasses import asdict, dataclass, fields
from datetime import date
from pathlib import Path

SCHEMA_VERSION = 1


class KatalogFehler(ValueError):
    """Absichtlich ohne Datenwerte: Fehlermeldungen duerfen private Altinhalte nicht ausgeben."""


@dataclass(frozen=True)
class Faehigkeit:
    name: str
    x: int
    text: str


@dataclass(frozen=True)
class Gabe:
    name: str
    text: str


@dataclass(frozen=True)
class Karte:
    id: str
    name: str
    art: str
    form: str
    titel: str
    volk: str
    wesen: str
    farbe: str
    hex: str
    rolle: str
    reihe: str
    seltenheit: str
    kosten: int
    angriff: int
    leben: int
    merkmale: tuple[str, ...]
    faehigkeit: Faehigkeit | None
    gabe: Gabe | None
    glanz: bool
    wertpunkte: int | float
    status: str
    monat: str

    def als_dict(self) -> dict:
        daten = asdict(self)
        daten["merkmale"] = list(self.merkmale)
        return daten


KARTENFELDER = tuple(f.name for f in fields(Karte))


@dataclass(frozen=True)
class Katalog:
    schema_version: int
    monat: str
    karten: tuple[Karte, ...]

    def als_dict(self) -> dict:
        return {"schema_version": self.schema_version, "monat": self.monat,
                "anzahl": len(self.karten), "karten": [k.als_dict() for k in self.karten]}


def _objekt(wert, pfad: str) -> dict:
    if not isinstance(wert, dict):
        raise KatalogFehler(f"Objekt erforderlich: {pfad}")
    return wert


def _text(wert, pfad: str) -> str:
    if not isinstance(wert, str) or not wert.strip():
        raise KatalogFehler(f"Nichtleerer Text erforderlich: {pfad}")
    try:
        wert.encode("utf-8")
    except UnicodeError as exc:
        raise KatalogFehler(f"Ungueltiger Unicode-Text: {pfad}") from exc
    return wert


def _ganzzahl(wert, pfad: str, minimum: int = 1) -> int:
    if type(wert) is not int or not minimum <= wert <= 2_147_483_647:
        raise KatalogFehler(f"Ganzzahl ausserhalb des Vertrags: {pfad}")
    return wert


def _monat(wert, pfad: str) -> str:
    if not isinstance(wert, str) or not re.fullmatch(r"[0-9]{4}-(0[1-9]|1[0-2])", wert):
        raise KatalogFehler(f"Monat im Format JJJJ-MM erforderlich: {pfad}")
    try:
        date.fromisoformat(wert + "-01")
    except ValueError as exc:
        raise KatalogFehler(f"Ungueltiger Kalendermonat: {pfad}") from exc
    return wert


def _auswahl(wert, erlaubt: tuple[str, ...], pfad: str) -> str:
    if not isinstance(wert, str) or wert not in erlaubt:
        raise KatalogFehler(f"Nicht unterstuetzter Wert: {pfad}")
    return wert


def _karte(daten, monat: str, pfad: str) -> Karte:
    d = _objekt(daten, pfad)
    if not set(KARTENFELDER) <= d.keys():
        raise KatalogFehler(f"Pflichtfelder fehlen: {pfad}")
    werte = {f: d[f] for f in KARTENFELDER}
    for f in ("id", "name", "art", "titel", "volk", "wesen", "farbe", "rolle", "reihe"):
        _text(d[f], f"{pfad}.{f}")
    if not re.fullmatch(r"K[0-9]{4,}", d["id"]) or int(d["id"][1:]) < 1:
        raise KatalogFehler(f"Ungueltige Karten-ID: {pfad}.id")
    if not isinstance(d["hex"], str) or not re.fullmatch(r"#[0-9a-fA-F]{6}", d["hex"]):
        raise KatalogFehler(f"RGB-Farbe erforderlich: {pfad}.hex")
    _auswahl(d["form"], ("Jungform", "Reife Form", "Urform"), f"{pfad}.form")
    _auswahl(d["seltenheit"], ("Gewöhnlich", "Ungewöhnlich", "Selten", "Episch", "Mythisch"), f"{pfad}.seltenheit")
    _auswahl(d["status"], ("aktiv", "ruhend"), f"{pfad}.status")
    for f in ("kosten", "angriff", "leben"):
        _ganzzahl(d[f], f"{pfad}.{f}")
    punkte = d["wertpunkte"]
    if type(punkte) not in (int, float) or not 0 <= punkte <= 2_147_483_647 or not math.isfinite(punkte):
        raise KatalogFehler(f"Endliche nichtnegative Zahl erforderlich: {pfad}.wertpunkte")
    if type(d["glanz"]) is not bool:
        raise KatalogFehler(f"Boolescher Wert erforderlich: {pfad}.glanz")
    if not isinstance(d["merkmale"], list):
        raise KatalogFehler(f"Liste erforderlich: {pfad}.merkmale")
    werte["merkmale"] = tuple(_text(m, f"{pfad}.merkmale") for m in d["merkmale"])
    for feld, modell in (("faehigkeit", Faehigkeit), ("gabe", Gabe)):
        if d[feld] is not None:
            effekt = _objekt(d[feld], f"{pfad}.{feld}")
            argumente = {}
            for f in fields(modell):
                if f.name not in effekt:
                    raise KatalogFehler(f"Pflichtfeld fehlt: {pfad}.{feld}.{f.name}")
                argumente[f.name] = (_ganzzahl(effekt[f.name], f"{pfad}.{feld}.x") if f.name == "x"
                                     else _text(effekt[f.name], f"{pfad}.{feld}.{f.name}"))
            werte[feld] = modell(**argumente)
    stand = _monat(d["monat"], f"{pfad}.monat")
    if stand > monat or (d["status"] == "aktiv" and stand != monat):
        raise KatalogFehler(f"Kartenmonat widerspricht dem Katalog: {pfad}.monat")
    return Karte(**werte)


def katalog_aus_daten(daten: dict) -> Katalog:
    d = _objekt(daten, "Katalog")
    version = d.get("schema_version")
    if type(version) is not int or version != SCHEMA_VERSION:
        raise KatalogFehler("Fehlende oder nicht unterstuetzte schema_version.")
    monat = _monat(d.get("monat"), "monat")
    anzahl = _ganzzahl(d.get("anzahl"), "anzahl", minimum=0)
    if not isinstance(d.get("karten"), list) or anzahl != len(d["karten"]):
        raise KatalogFehler("Kartenliste und anzahl stimmen nicht ueberein.")
    karten = tuple(_karte(k, monat, f"karten[{i}]") for i, k in enumerate(d["karten"]))
    if len({k.id for k in karten}) != anzahl or len({k.name.casefold() for k in karten}) != anzahl:
        raise KatalogFehler("Karten-IDs oder Namen sind nicht eindeutig.")
    return Katalog(version, monat, karten)


def altbestand_uebernehmen(daten: dict) -> Katalog:
    """Nur explizit fuer das bisherige unversionierte Format; kein automatischer Zukunftsfallback."""
    if not isinstance(daten, dict) or "schema_version" in daten:
        raise KatalogFehler("Altbestand darf keine schema_version enthalten.")
    return katalog_aus_daten({**daten, "schema_version": SCHEMA_VERSION})


def _eindeutige_felder(paare: list[tuple]) -> dict:
    ergebnis = {}
    for k, v in paare:
        if k in ergebnis:
            raise KatalogFehler("Doppelte JSON-Felder sind nicht erlaubt.")
        ergebnis[k] = v
    return ergebnis


def _keine_sonderzahl(_wert):
    raise KatalogFehler("JSON darf kein NaN oder Infinity enthalten.")


def lese_katalog(text: str | bytes, *, erlaube_altbestand: bool = False) -> Katalog:
    try:
        if isinstance(text, bytes):
            text = text.decode("utf-8")
        daten = json.loads(text, object_pairs_hook=_eindeutige_felder, parse_constant=_keine_sonderzahl)
    except (json.JSONDecodeError, UnicodeError, RecursionError) as exc:
        raise KatalogFehler("Katalog ist kein lesbares JSON.") from exc
    if erlaube_altbestand and isinstance(daten, dict) and "schema_version" not in daten:
        return altbestand_uebernehmen(daten)
    return katalog_aus_daten(daten)


def katalog_json(katalog: Katalog) -> str:
    return json.dumps(katalog.als_dict(), ensure_ascii=False, indent=1, allow_nan=False) + "\n"


def katalog_sha256(text: bytes) -> str:
    """Referenz auf genau diese UTF-8-Datei, nicht auf eine vermutete Spiele-Edition."""
    lese_katalog(text)
    return hashlib.sha256(text).hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description="Oeffentlichen Katalogvertrag pruefen, ohne Dateien zu aendern")
    parser.add_argument("datei", type=Path)
    args = parser.parse_args()
    try:
        raw = args.datei.read_bytes()
        katalog = lese_katalog(raw)
        fingerprint = katalog_sha256(raw)
    except (ValueError, OSError) as exc:
        parser.exit(2, f"Katalogpruefung abgebrochen: {exc}\n")
    print(f"Schema {katalog.schema_version}, Monat {katalog.monat}, {len(katalog.karten)} Karten")
    print(f"Datei-SHA256: {fingerprint}")


if __name__ == "__main__":
    main()
