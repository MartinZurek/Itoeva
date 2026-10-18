"""Leuchtfeuer-Entwurfsbuch mit Vektorwesen; ohne Engine, neue Kartenwerte oder Vergabe.

    python tools/card-forge/wesen.py
"""
from __future__ import annotations

import json
from html import escape
from pathlib import Path

from katalog import lese_katalog

HIER = Path(__file__).resolve().parent
FORMEN = ("Jungform", "Reife Form", "Urform")
# Stabile Gestaltungs-IDs sind unabhängig von Karten-ID und Monatsausgabe.
ARTEN = (
    ("licht", "Electric Utilities"),
    ("kessel", "Gas Utilities"),
    ("wind", "Independent Power and Renewable Electricity Producers"),
    ("rohr", "Multi-Utilities"),
    ("quelle", "Water Utilities"),
)


def entwuerfe(regeln: dict) -> list[dict]:
    volk = regeln["voelker"]["Utilities"]
    ergebnis = []
    for familie, schluessel in ARTEN:
        art = regeln["arten"][schluessel]
        for stufe, (form, name) in enumerate(zip(FORMEN, art["formen"], strict=True)):
            ergebnis.append(dict(id=f"leuchtfeuer-{familie}-{stufe + 1}", name=name,
                typ="Kartenwesen", volk=volk["volk"], form=form, rolle=art["rolle"],
                familie=familie, stufe=stufe, hex="#e0683a"))
    # Zahlen/Merkmale sind ausschließlich die bestehenden Gaben, keine neue Balance.
    for ident, name, sektor, angriff, leben, merkmale in (
        ("werkling", "Werkling", "Industrials", 1, 1, []),
        ("erzsplitter", "Erzsplitter", "Materials", 0, 3, ["Wächter"]),
    ):
        quelle = regeln["voelker"][sektor]
        ergebnis.append(dict(id=ident, name=name, typ="Spielhelfer", volk=quelle["volk"],
            form=None, rolle=None, familie=ident, stufe=0,
            hex="#d8a65b" if ident == "werkling" else "#83b8b0",
            angriff=angriff, leben=leben, merkmale=merkmale,
            quelle=dict(gabe=quelle["gabe"]["name"], text=quelle["gabe"]["text"])))
    return ergebnis


def bild(wesen: dict) -> str:
    """480×270-Entwurf: dunkle Aussparung, Materialkante links oben, Licht im Inneren.

    Bewusst eigenständige SVG-Silhouetten statt vermeintlich fertiger Pixel-/3D-Assets.
    Keine externen Ressourcen oder eingebetteten Skripte.
    """
    f, s = wesen["familie"], wesen["stufe"]
    koerper = []

    def pfad(d, fill="url(#material)", stroke="#f1c59b", width=3):
        koerper.append(f'<path d="{d}" fill="{fill}" stroke="{stroke}" stroke-width="{width}" stroke-linejoin="round"/>')

    def ellipse(x, y, rx, ry, fill="url(#material)"):
        koerper.append(f'<ellipse cx="{x}" cy="{y}" rx="{rx}" ry="{ry}" fill="{fill}" stroke="#e9bb8a" stroke-width="3"/>')

    def augen(x=240, y=139, abstand=15):
        for dx in (-abstand, abstand):
            ellipse(x + dx, y, 4, 6, "#fff4cc")

    if f == "licht":
        if s == 0:
            ellipse(208, 123, 26, 14, "#accac4"); ellipse(270, 123, 26, 14, "#accac4")
            ellipse(240, 156, 24, 34); ellipse(240, 174, 14, 16, "url(#feuer)")
            pfad("M230 124 Q215 92 207 107 M249 124 Q265 92 273 107", "none")
            augen(y=144, abstand=10)
        else:
            pfad("M205 112 L275 112 L284 184 L267 205 L213 205 L196 184 Z")
            pfad("M213 119 H267 V175 H213 Z", "url(#feuer)")
            pfad("M193 111 L240 77 L287 111 Z")
            pfad("M227 84 Q215 48 241 51 Q266 53 253 84", "none")
            pfad("M208 203 L198 224 H220 L225 205 M261 203 L269 224 H291 L276 203")
            augen(y=148)
            if s == 2:
                pfad("M197 125 Q156 113 169 174 L199 190 M282 125 Q325 113 310 174 L282 190")
                pfad("M213 77 L203 48 L227 58 L240 31 L253 58 L277 48 L267 77", "url(#feuer)")
    elif f == "kessel":
        if s == 0:
            pfad("M240 204 Q188 196 209 153 Q217 134 213 116 Q229 126 237 94 Q274 137 269 155 Q297 193 240 204 Z", "url(#feuer)")
            augen(y=169)
        else:
            pfad("M190 137 Q174 205 219 211 H269 Q309 198 289 137 Z")
            pfad("M198 139 Q155 98 187 96 M280 139 Q325 98 297 96", "none", width=8)
            ellipse(240, 137, 50, 12, "#3a2e31")
            pfad("M216 129 Q204 99 232 72 Q226 103 244 108 Q247 76 263 60 Q289 114 264 130", "url(#feuer)")
            pfad("M210 209 L201 225 H220 L225 211 M260 211 L268 225 H287 L276 209")
            augen(y=174)
            if s == 2:
                pfad("M190 154 L160 148 L144 178 L163 191 L184 181 M293 154 L320 148 L338 178 L319 191 L298 181")
                pfad("M224 74 L209 53 L230 57 L241 35 L253 58 L276 51 L263 72", "url(#feuer)")
    elif f == "wind":
        if s == 0:
            ellipse(235, 166, 42, 26)
            pfad("M248 156 L257 111 L283 106 L296 128 L274 144 L271 174 Z")
            pfad("M257 112 L252 92 L269 105 M285 109 L291 94 L293 117")
            pfad("M210 181 L204 214 H218 L225 187 M254 186 L264 214 H278 L272 177 M195 161 Q162 130 160 159", "none", width=7)
            ellipse(280, 124, 3, 4, "#fff4cc")
        elif s == 1:
            pfad("M216 128 L259 128 L276 210 H205 Z")
            pfad("M213 166 L187 191 M266 166 L293 190", "none", width=13)
            for d in ("M240 102 L219 48 L245 45 Z", "M240 102 L296 80 L302 107 Z", "M240 102 L258 157 L230 159 Z", "M240 102 L185 126 L178 101 Z"):
                pfad(d, "#abc9ba")
            ellipse(240, 102, 12, 12, "url(#feuer)"); augen(y=177, abstand=12)
        else:
            pfad("M234 160 Q179 167 130 99 L196 120 L159 66 Q214 85 241 130 Q269 85 321 66 L284 120 L350 99 Q299 169 249 160", "url(#feuer)")
            pfad("M236 172 L206 213 L239 199 L270 216 L251 171", "#dda659")
            ellipse(242, 145, 20, 33); ellipse(242, 115, 17, 17)
            pfad("M259 113 L279 121 L257 127", "#ffe7a0")
            ellipse(248, 111, 3, 4, "#fff4cc")
    elif f == "rohr":
        if s == 0:
            pfad("M201 163 Q190 200 217 210 H266 Q288 199 278 163 Z")
            ellipse(240, 162, 40, 11, "#82bec1")
            pfad("M221 159 Q208 125 228 102 Q237 122 247 109 Q272 138 260 157", "#90d1cd")
            pfad("M208 207 L195 219 M273 207 L287 219", "none", width=7); augen(y=140, abstand=10)
        else:
            pfad("M164 202 Q181 124 219 181 Q241 220 260 159 Q276 113 305 157", "none", "#111820", 44)
            pfad("M164 202 Q181 124 219 181 Q241 220 260 159 Q276 113 305 157", "none", "#bc8a5d", 31)
            ellipse(305, 152, 26, 22); augen(x=306, y=147, abstand=10)
            pfad("M178 176 L192 185 M215 177 L205 191 M248 185 L232 178 M267 151 L282 160", "none", "#eed0a1", 5)
            if s == 2:
                pfad("M291 132 L281 101 L302 111 L315 83 L324 117 L341 111 L326 139", "#8cb9ac")
                pfad("M151 194 L128 177 M183 156 L177 128 M251 166 L243 139", "none", width=8)
    elif f == "quelle":
        if s == 0:
            pfad("M216 174 L185 166 L175 188 M218 187 L194 199 L187 216 M265 174 L294 166 L308 187 M264 187 L291 200 L297 215", "none", width=7)
            ellipse(240, 175, 32, 25, "#73aeb3")
            ellipse(181, 151, 14, 17, "#8fc8c5"); ellipse(301, 151, 14, 17, "#8fc8c5")
            pfad("M216 160 L225 144 M260 160 L252 144", "none", width=6); augen(y=149)
        else:
            pfad("M219 141 Q187 153 189 194 L209 192 L210 218 H230 L236 196 H249 L254 218 H277 L273 192 L294 194 Q291 155 265 142 Z", "#638e84")
            ellipse(241, 134, 29, 32, "#80b5a5"); augen(y=131)
            pfad("M217 108 Q213 77 240 60 Q267 80 266 107", "#79c3cf")
            pfad("M238 75 Q226 98 243 102", "none", "#e5fff1")
            if s == 2:
                pfad("M211 113 L186 77 L216 90 L225 54 L243 76 L264 50 L268 90 L299 78 L270 116", "#9cbf96")
                pfad("M287 182 L306 221 M301 214 L320 153 Q326 134 308 127", "none", width=7)
    elif f == "werkling":
        pfad("M209 125 H270 V187 H209 Z")
        ellipse(239, 108, 26, 24); pfad("M207 105 H272 L262 85 H218 Z", "#dbad64")
        pfad("M208 143 L180 174 L168 157 M271 143 L297 165 M221 188 L213 216 H231 M258 188 L267 216 H249", "none", width=10)
        pfad("M291 144 L307 137 L321 155 L310 166 L313 189 L298 192 L294 169 L282 161 Z", "#8fa5a4")
        ellipse(240, 154, 11, 11, "url(#feuer)"); augen(y=107, abstand=10)
    elif f == "erzsplitter":
        pfad("M195 199 L208 115 L244 68 L279 117 L289 200 L242 222 Z", "#537f86")
        pfad("M244 68 L236 172 L208 115 Z", "#b4e0d2")
        pfad("M244 68 L279 117 L260 172 L236 172 Z", "#82babb")
        pfad("M195 199 L236 172 L242 222 Z", "#699f99")
        pfad("M289 200 L260 172 L242 222 Z", "#34565f")
        pfad("M223 183 L241 174 L258 183 L254 198 L241 207 L226 198 Z", "url(#feuer)")
        augen(y=153, abstand=12)
    else:
        raise ValueError("Unbekannte Gestaltungsfamilie")

    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 480 270" role="img" aria-labelledby="name">
<title id="name">{escape(wesen['name'])} – Vektorentwurf</title>
<defs><linearGradient id="material" x2="0.8" y2="1"><stop stop-color="#bd9a75"/><stop offset=".45" stop-color="#755b4e"/><stop offset="1" stop-color="#322d32"/></linearGradient>
<radialGradient id="feuer"><stop stop-color="#fff7b7"/><stop offset=".5" stop-color="#f5bd68"/><stop offset="1" stop-color="{wesen['hex']}"/></radialGradient>
<filter id="aussparung" x="-20%" y="-20%" width="140%" height="140%"><feMorphology in="SourceAlpha" operator="dilate" radius="4" result="rand"/><feFlood flood-color="#101820"/><feComposite in2="rand" operator="in"/><feMerge><feMergeNode/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs>
<ellipse cx="240" cy="226" rx="89" ry="11" fill="#080e15" opacity=".8"/>
<g filter="url(#aussparung)" paint-order="stroke fill">{''.join(koerper)}</g>
</svg>'''


def buch(wesen: list[dict], karten: list[dict]) -> str:
    def illustration(w, karte=None):
        img = f'<img src="wesen/{w["id"]}.svg" alt="{escape(w["name"])}" width="480" height="270"/>'
        if w["typ"] == "Spielhelfer":
            return img
        daten = dict(volk=w["volk"], form=w["form"], hex=w["hex"],
                     bild=f'wesen/{w["id"]}.svg', name=w["name"], seltenheit="", glanz="false")
        if karte:
            daten.update(name=karte["name"], hex=karte["hex"], seltenheit=karte["seltenheit"],
                         glanz=str(karte["glanz"]).lower())
        attrs = ' '.join(f'data-{k}="{escape(v, quote=True)}"' for k, v in daten.items())
        return (f'<button type="button" class="beschwoeren" {attrs} '
                f'aria-label="{escape(daten["name"], quote=True)} beschwören">{img}'
                '<span>Beschwören ✧</span></button>')

    felder = []
    for w in wesen:
        verbunden = [k for k in karten if (k["art"], k["form"]) == (w["name"], w["form"])]
        werte = (f'{w["angriff"]} Angriff · {w["leben"]} Leben'
                 if w["typ"] == "Spielhelfer"
                 else f'{len(verbunden)} {"Karte" if len(verbunden) == 1 else "Karten"} im Katalog')
        quelle = w.get("quelle", {}).get("text", "Artentwurf · keine zusätzliche Monatskarte")
        felder.append(f'''<article data-typ="{w['typ']}" data-form="{w['form'] or ''}">
<p class="eyebrow">{escape(w['volk'])} · {w['form'] or 'Spielhelfer'}</p>
{illustration(w)}
<h3>{escape(w['name'])}</h3><p>{werte}</p><p class="muted">{escape(quelle)}</p></article>''')
    for k in karten:
        w = next(w for w in wesen if (w["name"], w["form"]) == (k["art"], k["form"]))
        texte = [escape(t["text"]) for t in (k["faehigkeit"], k["gabe"]) if t]
        merkmale = ' · '.join(map(escape, k["merkmale"]))
        felder.append(f'''<article data-typ="Monatskarte" data-form="{k['form']}" class="{'glanz' if k['glanz'] else ''}">
<p class="eyebrow">{escape(k['seltenheit'])} · {k['kosten']} Kristalle</p>
{illustration(w, k)}
<h3>{escape(k['name'])}</h3><p>{escape(k['art'])} · {k['form']}</p>
<p class="stats">{k['angriff']} Angriff <span>{k['leben']} Leben</span></p>
<p>{merkmale}</p><p class="muted">{'<br/>'.join(texte)}</p>
<small>{escape(k['id'])} · {k['monat']} · {escape(k['status'])}{' · Glanz' if k['glanz'] else ''}</small></article>''')
    vorlage = (HIER / "wesenbuch_vorlage.html").read_text(encoding="utf-8")
    return vorlage.replace("<!-- WESEN -->", '\n'.join(felder))


def erzeugen(ziel: Path = HIER / "out") -> None:
    regeln = json.loads((HIER / "rules.json").read_text(encoding="utf-8"))
    katalog = lese_katalog((HIER / "out/karten.json").read_bytes())
    wesen = entwuerfe(regeln)
    karten = [k.als_dict() for k in katalog.karten if k.volk == "Leuchtfeuer"]
    bilder = ziel / "wesen"
    bilder.mkdir(parents=True, exist_ok=True)
    for w in wesen:
        (bilder / f'{w["id"]}.svg').write_text(bild(w), encoding="utf-8", newline="\n")
    # Nur explizite Gestaltungsdaten exportieren, keine Faktor-/Industriezuordnung.
    oeffentlich = []
    for w in wesen:
        daten = {k: v for k, v in w.items() if k not in ("familie", "stufe")}
        daten["bild"] = f'wesen/{w["id"]}.svg'
        daten["karten_ids"] = [k["id"] for k in karten if (k["art"], k["form"]) == (w["name"], w["form"])]
        oeffentlich.append(daten)
    (ziel / "minions.json").write_text(json.dumps(dict(schema_version=1,
        zweck="Gestaltungsentwürfe und bestehende Regelvorschläge, keine Kampf-Runtime",
        wesen=oeffentlich), ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    (ziel / "wesenbuch.html").write_text(buch(wesen, karten), encoding="utf-8", newline="\n")
    (ziel / "beschwoerung.js").write_text((HIER / "beschwoerung.js").read_text(
        encoding="utf-8"), encoding="utf-8", newline="\n")


if __name__ == "__main__":
    erzeugen()
