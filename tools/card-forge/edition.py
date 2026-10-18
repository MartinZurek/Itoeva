"""Referenzmodell fuer beschlossene Variante A; kein Erwerbsweg und kein App-Spielstand."""
from __future__ import annotations

import hashlib
from dataclasses import dataclass, field

from katalog import Karte, Katalog, KatalogFehler, katalog_json, lese_katalog
from statkorrektur import korrigiere_stats


@dataclass(frozen=True)
class KatalogSnapshot:
    inhalt: bytes
    katalog: Katalog = field(init=False)
    sha256: str = field(init=False)

    def __post_init__(self):
        if type(self.inhalt) is not bytes:
            raise KatalogFehler("Snapshot muss aus UTF-8-Bytes bestehen.")
        katalog = lese_katalog(self.inhalt)
        # Ein Archiv darf unbekannte/private Zusatzfelder nicht als Originalbytes bewahren.
        public = katalog_json(katalog).encode("utf-8")
        object.__setattr__(self, "inhalt", public)
        object.__setattr__(self, "katalog", katalog)
        object.__setattr__(self, "sha256", hashlib.sha256(public).hexdigest())


@dataclass(frozen=True)
class ErworbeneAusgabe:
    """Ein bereits gewaehrter Erwerb verweist auf bewahrte Daten, nie auf den Live-Katalog."""

    snapshot: KatalogSnapshot
    karten_id: str
    karte: Karte = field(init=False)

    def __post_init__(self):
        if not isinstance(self.snapshot, KatalogSnapshot):
            raise KatalogFehler("Erworbene Ausgabe benoetigt einen bewahrten Katalog-Snapshot.")
        karte = next((k for k in self.snapshot.katalog.karten if k.id == self.karten_id), None)
        if karte is None:
            raise KatalogFehler("Erworbene Karte fehlt im referenzierten Snapshot.")
        object.__setattr__(self, "karte", karte)

    @property
    def referenz(self) -> tuple[str, str]:
        return self.snapshot.sha256, self.karten_id

    def mit_statkorrektur(self, *, angriff: int | None = None, leben: int | None = None,
                         kosten: int | None = None) -> ErworbeneAusgabe:
        """Bewusste Korrektur derselben Karte; kein neuer Erwerb und kein Monatsimport."""
        katalog = korrigiere_stats(self.snapshot.katalog, self.karten_id,
                                  angriff=angriff, leben=leben, kosten=kosten)
        snapshot = KatalogSnapshot(katalog_json(katalog).encode("utf-8"))
        return ErworbeneAusgabe(snapshot, self.karten_id)
