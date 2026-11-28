extends RefCounted

# Auch Requisiten ohne Spezialaktion bekommen einen Namen und eine Ortsbeobachtung.
const DETAILS := {
	"park-crate": ["Wetterkiste", "In den Fugen steckt trockener Löwenzahn. Jemand hat die Kiste vom Weg geschoben."],
	"tea-table": ["Teetisch", "Auf dem Holz zeichnen sich helle Ringe von vielen warmen Tassen ab."],
	"side-table": ["Beistelltisch", "Die abgestoßene Kante ist vom vielen Anfassen glatt geworden."],
	"wardrobe": ["Kleiderschrank", "Zwischen den Brettern hängt der Geruch von Zedernholz."],
	"dresser": ["Kommode", "Ein winziger Kerbstreifen markiert jede Schublade."],
	"footbench": ["Fußbank", "Die Mitte ist vom vielen Auftreten heller als die Ränder."],
	"nightstand": ["Nachttisch", "Auf der Platte liegt eine kleine Spur von Kerzenwachs."],
	"basin": ["Waschbecken", "Am Rand trocknen winzige Tropfen."],
	"tub": ["Badewanne", "Der steinerne Rand fühlt sich kühl an."],
	"drawers": ["Schubladen", "Die Griffe sind blank poliert. In einer Fuge steckt ein Papierwinkel."],
	"fridge": ["Kühlschrank", "Hinter der Tür summt es leise."],
	"cabinet": ["Schrank", "Aus dem Holz strömt ein schwacher Duft nach getrockneten Kräutern."],
	"chest": ["Truhe", "Die Messingbeschläge sind an den Ecken grün angelaufen."],
	"crate": ["Holzkiste", "Die Bretter tragen noch die Kerben vom letzten Transport."],
	"counter": ["Tresen", "An dieser Kante haben schon viele Leute gewartet."],
	"table": ["Tisch", "Unebene Holzmaserung zeichnet feine Linien unter dem Licht."],
	"shelves": ["Regale", "Zwischen eng gestellten Dingen ist ein schmaler, staubfreier Streifen."],
	"shelf": ["Regal", "Auf dem obersten Brett ist der Staub nur an einer Stelle verwischt."],
	"machine": ["Spielautomat", "Hinter dem Glas flackert die Anzeige und wartet auf den nächsten Versuch."],
	"pinball": ["Flipper", "Unter der Scheibe glänzen die Rampen vom letzten Spiel."],
}

static func inspect(id: String) -> Array:
	if DETAILS.has(id): return DETAILS[id]
	var furnishing := id.get_slice("-", 1)
	if id.ends_with("-side-table"): furnishing = "side-table"
	if id.ends_with("-tea-table"): furnishing = "tea-table"
	if id.ends_with("-left-table"): furnishing = "table"
	return DETAILS.get(furnishing, ["Einrichtungsstück", "Das Material zeigt Spuren davon, wie dieser Ort genutzt wird."])
