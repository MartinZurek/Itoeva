"""Gezielte Statkorrektur, ausdruecklich getrennt vom automatischen Monatswechsel."""
from __future__ import annotations

from katalog import Katalog, KatalogFehler, katalog_aus_daten


def korrigiere_stats(katalog: Katalog, karten_id: str, *, angriff: int | None = None,
                    leben: int | None = None, kosten: int | None = None) -> Katalog:
    korrektur = {f: v for f, v in (("angriff", angriff), ("leben", leben), ("kosten", kosten)) if v is not None}
    if not korrektur:
        raise KatalogFehler("Statkorrektur benoetigt mindestens einen neuen Statwert.")
    for wert in korrektur.values():
        if type(wert) is not int or not 1 <= wert <= 2_147_483_647:
            raise KatalogFehler("Statkorrektur benoetigt positive Int32-Ganzzahlen.")
    daten = katalog.als_dict()
    karte = next((k for k in daten["karten"] if k["id"] == karten_id), None)
    if karte is None:
        raise KatalogFehler("Zu korrigierende Karte fehlt im Katalog.")
    alter_koerper = karte["angriff"] + karte["leben"]
    karte.update(korrektur)
    # Effektpreise bleiben gleich. Kein stilles Skalieren anderer Stats auf die 2K+1-Kurve.
    karte["wertpunkte"] += karte["angriff"] + karte["leben"] - alter_koerper
    return katalog_aus_daten(daten)
