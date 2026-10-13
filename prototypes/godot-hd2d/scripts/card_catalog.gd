extends RefCounted
const MIN_DECK := 8
const MAX_DECK := 12
const CARDS := {
 "spark":{"name":"Funkenstoß","cost":1,"kind":"damage","value":7,"text":"7 Schaden am gewählten Gegner.","class":"all","element":"light"},
 "leaf_guard":{"name":"Blätterschirm","cost":1,"kind":"barrier","value":12,"text":"12 Schutz für dein Wesen.","class":"all","element":"guard"},
 "tea":{"name":"Waldtee","cost":2,"kind":"heal","value":16,"text":"Heilt dein Wesen um 16 LP.","class":"all","element":"heal"},
 "insight":{"name":"Waldwissen","cost":1,"kind":"draw","value":2,"text":"Ziehe 2 Karten.","class":"all","element":"nature"},
 "cat":{"name":"Waldkatze","cost":1,"kind":"minion","attack":3,"health":5,"row":0,"text":"3 Angriff / 5 LP.","class":"fennec","element":"nature"},
 "boar":{"name":"Wildschwein","cost":2,"kind":"minion","attack":4,"health":8,"row":1,"text":"4 Angriff / 8 LP · Schutz.","class":"fennec","element":"nature","taunt":true},
 "wolf":{"name":"Silberwolf","cost":3,"kind":"minion","attack":7,"health":10,"row":2,"text":"7 Angriff / 10 LP.","class":"fennec","element":"nature"},
 "lightling":{"name":"Lichtfunke","cost":1,"kind":"minion","attack":3,"health":5,"species":"starlet","text":"3 Angriff / 5 LP.","class":"starlet","element":"light"},
 "star_guard":{"name":"Sternwächter","cost":3,"kind":"minion","attack":4,"health":12,"species":"starlet","text":"4 Angriff / 12 LP · Schutz.","class":"starlet","element":"light","taunt":true},
 "radiance":{"name":"Lichtbogen","cost":2,"kind":"damage","value":16,"text":"16 Lichtschaden am Ziel.","class":"starlet","element":"light"},
 "emberling":{"name":"Glutling","cost":1,"kind":"minion","attack":4,"health":4,"species":"wyrmling","text":"4 Angriff / 4 LP.","class":"wyrmling","element":"fire"},
 "coal_guard":{"name":"Kohlenwächter","cost":3,"kind":"minion","attack":5,"health":10,"species":"wyrmling","text":"5 Angriff / 10 LP · Schutz.","class":"wyrmling","element":"fire","taunt":true},
 "firebolt":{"name":"Feuerstoß","cost":2,"kind":"damage","value":18,"text":"18 Feuerschaden am Ziel.","class":"wyrmling","element":"fire"},
 "dewling":{"name":"Tautropfen","cost":1,"kind":"minion","attack":2,"health":7,"species":"gloop","text":"2 Angriff / 7 LP.","class":"gloop","element":"water"},
 "spring_guard":{"name":"Quellwächter","cost":3,"kind":"minion","attack":4,"health":13,"species":"gloop","text":"4 Angriff / 13 LP · Schutz.","class":"gloop","element":"water","taunt":true},
 "spring":{"name":"Quellruhe","cost":2,"kind":"heal","value":24,"text":"Heilt dein Wesen um 24 LP.","class":"gloop","element":"heal"},
 "breezeling":{"name":"Windling","cost":1,"kind":"minion","attack":3,"health":5,"species":"puffling","text":"3 Angriff / 5 LP.","class":"puffling","element":"wind"},
 "wind_guard":{"name":"Windwächter","cost":3,"kind":"minion","attack":5,"health":10,"species":"puffling","text":"5 Angriff / 10 LP · Schutz.","class":"puffling","element":"wind","taunt":true},
 "gust":{"name":"Sturmbogen","cost":2,"kind":"damage","value":17,"text":"17 Windschaden am Ziel.","class":"puffling","element":"wind"},
 "moonling":{"name":"Mondling","cost":1,"kind":"minion","attack":3,"health":6,"species":"hootlet","text":"3 Angriff / 6 LP.","class":"hootlet","element":"moon"},
 "moon_guard":{"name":"Mondwächter","cost":3,"kind":"minion","attack":4,"health":12,"species":"hootlet","text":"4 Angriff / 12 LP · Schutz.","class":"hootlet","element":"moon","taunt":true},
 "moonbeam":{"name":"Mondstrahl","cost":2,"kind":"damage","value":16,"text":"16 Mondschaden am Ziel.","class":"hootlet","element":"moon"},
 "thornling":{"name":"Dornenwicht","cost":2,"kind":"minion","attack":5,"health":9,"species":"enemy","text":"5 Angriff / 9 LP · Schutz.","class":"enemy","element":"shadow","taunt":true},
 "shards":{"name":"Splitterregen","cost":1,"kind":"damage","value":5,"text":"5 Schaden am Ziel.","class":"enemy","element":"shadow"},
 "root_rest":{"name":"Wurzelruhe","cost":2,"kind":"heal","value":12,"text":"12 LP für Nachtwurz.","class":"enemy","element":"heal"},
 "sprigling":{"name":"Blattwicht","cost":1,"kind":"minion","attack":2,"health":7,"species":"mosslyn","form":"sprigling","text":"2 Angriff / 7 LP.","class":"mosslyn","element":"nature"},
 "moss_guard":{"name":"Mooswächter","cost":3,"kind":"minion","attack":4,"health":14,"species":"mosslyn","form":"moss_guard","taunt":true,"text":"4 Angriff / 14 LP. Schutz.","class":"mosslyn","element":"nature"},
 "grove":{"name":"Hainruhe","cost":2,"kind":"heal","value":22,"text":"Heilt dein Wesen um 22 LP.","class":"mosslyn","element":"heal"},
 "echolet":{"name":"Echoflügel","cost":1,"kind":"minion","attack":4,"health":4,"species":"nocti","form":"echolet","text":"4 Angriff / 4 LP.","class":"nocti","element":"moon"},
 "night_guard":{"name":"Nachtwache","cost":3,"kind":"minion","attack":4,"health":11,"species":"nocti","form":"night_guard","taunt":true,"text":"4 Angriff / 11 LP. Schutz.","class":"nocti","element":"moon"},
 "echo":{"name":"Echobiss","cost":2,"kind":"damage","value":15,"heal_self":6,"text":"15 Schaden. Heilt dich um 6 LP.","class":"nocti","element":"moon"},
 "wild_rally":{"name":"Rudelruf","cost":4,"kind":"special","effect":"wild_rally","text":"Bis zu 2 Zufallshelfer. Einer bereit.","detail":"Beschwört bis zu zwei zufällige Waldtiere auf freie Plätze. Das erste Tier kann sofort angreifen.","class":"fennec","element":"nature","rare":true},
 "starfall":{"name":"Sternenfall","cost":5,"kind":"special","effect":"starfall","text":"24 / 10 Schaden. Gruppe: 16 Schutz.","detail":"24 Schaden an Nachtwurz, 10 an allen feindlichen Helfern. Beide lebenden Gruppenmitglieder erhalten bis zu 16 zusätzliche Schutzpunkte.","class":"starlet","element":"light","rare":true},
 "phoenix":{"name":"Phönixflamme","cost":5,"kind":"special","effect":"phoenix","text":"30 / 12 Schaden. Ruft einen Phönix.","detail":"30 Schaden an Nachtwurz, 12 an allen feindlichen Helfern. Wenn Platz ist, erscheint ein Phönix mit 6 Angriff und 8 LP.","class":"wyrmling","element":"fire","rare":true},
 "tide":{"name":"Gezeitenwende","cost":4,"kind":"special","effect":"tide","text":"Gruppe: +20 LP. Reinigt und heilt.","detail":"Heilt beide lebenden Mitglieder um 20 LP, eigene Helfer vollständig und entfernt Bindung und Verstummen bei der Gruppe.","class":"gloop","element":"water","rare":true},
 "stormflock":{"name":"Sturmfalken","cost":4,"kind":"special","effect":"stormflock","text":"Bis zu 2 Falken: 5 / 5, sofort bereit.","detail":"Beschwört bis zu zwei Sturmfalken auf freie Plätze. Jeder besitzt 5 Angriff, 5 LP und kann sofort angreifen.","class":"puffling","element":"wind","rare":true},
 "eclipse":{"name":"Mondfinsternis","cost":4,"kind":"special","effect":"eclipse","text":"18 Schaden. Stoppt nächsten Gegnerzug.","detail":"18 Schaden an Nachtwurz. Seine nächste Waldattacke und die Angriffe der aktuellen gegnerischen Helfer fallen aus.","class":"hootlet","element":"moon","rare":true},
 "ancient":{"name":"Urwaldherz","cost":4,"kind":"special","effect":"ancient","text":"Gruppe: 20 Schutz. Ruft Urwaldhüter.","detail":"Beide lebenden Mitglieder erhalten bis zu 20 zusätzliche Schutzpunkte. Wenn Platz ist, erscheint ein schützender Urwaldhüter mit 4 Angriff und 16 LP.","class":"mosslyn","element":"nature","rare":true},
 "mirror":{"name":"Nachtspiegel","cost":4,"kind":"special","effect":"mirror","text":"22 Schaden. Gegner schwach. Spiegelgeist.","detail":"22 Schaden an Nachtwurz, schwächt ihn für zwei Gegnerzüge. Wenn Platz ist, erscheint ein Spiegelgeist mit 4 Angriff, 8 LP und einmaligem Ausweichen.","class":"nocti","element":"moon","rare":true}
}
const ULTIMATES := {"fennec":"wild_rally","starlet":"starfall","wyrmling":"phoenix","gloop":"tide","puffling":"stormflock","hootlet":"eclipse","mosslyn":"ancient","nocti":"mirror"}
const HELPER_FORMS := ["sprigling","moss_guard","echolet","night_guard","phoenix","stormfalcon","ancient","mirror"]
const SPECIAL := {
 "fennec":["cat","boar","wolf"],"starlet":["lightling","star_guard","radiance"],
 "wyrmling":["emberling","coal_guard","firebolt"],"gloop":["dewling","spring_guard","spring"],
 "puffling":["breezeling","wind_guard","gust"],"hootlet":["moonling","moon_guard","moonbeam"],
 "mosslyn":["sprigling","moss_guard","grove"],"nocti":["echolet","night_guard","echo"],
 "enemy":["thornling","shards","root_rest"]
}
static func available(species: String) -> Array[String]:
 var result: Array[String]=[]
 if species!="enemy":result.assign(["spark","leaf_guard","tea","insight"])
 result.append_array(SPECIAL.get(species,SPECIAL.fennec))
 if ULTIMATES.has(species):result.append(ULTIMATES[species])
 return result
static func starter(species: String) -> Array[String]:
 var result: Array[String]=[]
 var ids: Array= SPECIAL.get(species,SPECIAL.fennec)
 for id in ids:
  result.append(id);result.append(id)
 if species=="enemy":
  result.append_array(result.duplicate())
 else:
  result.append(ULTIMATES.get(species,"wild_rally"))
  result.append_array(["spark","leaf_guard","leaf_guard","tea","insight"])
 return result
static func valid(species: String, entries: Array) -> bool:
 if entries.size()<MIN_DECK or entries.size()>MAX_DECK:return false
 var counts: Dictionary={}
 for id in entries:
  if not id is String or id not in available(species):return false
  counts[id]=int(counts.get(id,0))+1
  if counts[id]>(4 if species=="enemy" else 1 if CARDS[id].get("rare",false) else 2):return false
 return true
static func get_card(id: String) -> Dictionary:
 var result: Dictionary=CARDS.get(id,{}).duplicate(true)
 result["id"]=id
 return result
