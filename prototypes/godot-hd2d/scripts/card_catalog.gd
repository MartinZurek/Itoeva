extends RefCounted
const MIN_DECK := 40
const MAX_DECK := 40
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
 "mirror":{"name":"Nachtspiegel","cost":4,"kind":"special","effect":"mirror","text":"22 Schaden. Gegner schwach. Spiegelgeist.","detail":"22 Schaden an Nachtwurz, schwächt ihn für zwei Gegnerzüge. Wenn Platz ist, erscheint ein Spiegelgeist mit 4 Angriff, 8 LP und einmaligem Ausweichen.","class":"nocti","element":"moon","rare":true},
 "lantern_snail":{"name":"Laternenschnecke","cost":2,"kind":"minion","text":"2 / 8 · 4 Schild beim Erscheinen.","class":"all","element":"nature","form":"lantern_snail","attack":2,"health":8,"shield":4},
 "leaf_beetle":{"name":"Blattkäfer","cost":2,"kind":"minion","text":"4 / 6 · Nach dem Fallen: 1 Karte.","class":"all","element":"nature","form":"leaf_beetle","attack":4,"health":6,"death_draw":1},
 "mushroom_archivist":{"name":"Pilzarchivar","cost":3,"kind":"minion","text":"2 / 8 · Beim Erscheinen: 1 Karte.","class":"all","element":"nature","form":"mushroom_archivist","attack":2,"health":8,"draw_on_summon":1},
 "thistle_nurse":{"name":"Distelheilerin","cost":3,"kind":"minion","text":"3 / 9 · Eigener Zug: +2 LP.","class":"all","element":"nature","form":"thistle_nurse","attack":3,"health":9,"regen":2},
 "mist_glider":{"name":"Nebelgleiter","cost":3,"kind":"minion","text":"5 / 5 · Heilt dich um verursachte LP.","class":"all","element":"moon","form":"mist_glider","attack":5,"health":5,"lifesteal":true},
 "acorn_sentinel":{"name":"Eichelhüter","cost":4,"kind":"minion","text":"3 / 13 · Schutz und 5 Schild.","class":"all","element":"nature","form":"acorn_sentinel","attack":3,"health":13,"taunt":true,"shield":5},
 "trail_blessing":{"name":"Pfadsegen","cost":2,"kind":"buff","text":"Eigener Helfer: +2 Angriff / +3 LP.","class":"all","element":"nature","value":2,"health":3,"target":"ally_unit"},
 "bark_ward":{"name":"Rindensiegel","cost":1,"kind":"buff","text":"Eigener Helfer erhält 7 Schild.","class":"all","element":"guard","shield":7,"target":"ally_unit"},
 "dew_mend":{"name":"Tauverband","cost":1,"kind":"mend","text":"Eigener Helfer: +8 LP. Ziehe 1 Karte.","class":"all","element":"heal","value":8,"draw":1,"target":"ally_unit"},
 "frost_thread":{"name":"Frostfaden","cost":2,"kind":"freeze","text":"3 Schaden · Gegnerhelfer 1 Zug still.","class":"all","element":"water","value":3,"target":"enemy_unit"},
 "homeward":{"name":"Heimweg","cost":1,"kind":"recall","text":"Eigene Helferkarte zurück auf die Hand.","class":"all","element":"nature","target":"ally_unit"},
 "second_wind":{"name":"Zweiter Wind","cost":2,"kind":"buff","text":"Eigener Helfer bereit. +1 Angriff.","class":"all","element":"wind","value":1,"ready":true,"target":"ally_unit"},
 "leaf_volley":{"name":"Blättersalve","cost":3,"kind":"volley","text":"Allen Gegnerhelfern 6 Schaden.","class":"all","element":"nature","value":6},
 "moon_exchange":{"name":"Mondtausch","cost":2,"kind":"draw","text":"Ziehe 2 Karten und heile dich um 4.","class":"all","element":"moon","value":2,"heal_self":4},
 "root_pact":{"name":"Wurzelpakt","cost":3,"kind":"team_buff","text":"Eigene Helfer: +2 Angriff / +2 LP.","class":"all","element":"nature","value":2,"health":2},
 "fresh_sap":{"name":"Frischer Saft","cost":0,"kind":"energy","text":"+2 Kristalle bis zur Zugobergrenze.","class":"all","element":"nature","value":2},
 "forest_memory":{"name":"Waldgedächtnis","cost":3,"kind":"revive","text":"Zuletzt gefallene Helferkarte beschwören.","class":"all","element":"nature"},
 "shared_rest":{"name":"Gemeinsame Rast","cost":2,"kind":"party_heal","text":"Beide Wesen: +12 LP; löst Fesseln.","class":"all","element":"heal","value":12,"cleanse":true},
 "pack_step":{"name":"Rudelschritt","cost":2,"kind":"buff","text":"Tier bereit. +2 Angriff.","class":"fennec","element":"nature","value":2,"ready":true,"target":"ally_unit"},
 "trail_mark":{"name":"Fährtenzeichen","cost":2,"kind":"damage","text":"8 Schaden +2 je eigenem Helfer.","class":"fennec","element":"nature","value":8,"per_helper":2},
 "wild_cover":{"name":"Wildversteck","cost":1,"kind":"buff","text":"Eigener Helfer weicht 1 Treffer aus.","class":"fennec","element":"nature","evasion":true,"target":"ally_unit"},
 "pack_shelter":{"name":"Rudelschutz","cost":3,"kind":"team_buff","text":"Eigene Helfer: +3 LP und 4 Schild.","class":"fennec","element":"nature","health":3,"shield":4},
 "prism_ward":{"name":"Prismenschild","cost":2,"kind":"buff","text":"Helfer: 10 Schild. Ziehe 1 Karte.","class":"starlet","element":"light","shield":10,"draw":1,"target":"ally_unit"},
 "dawn_arc":{"name":"Morgenbogen","cost":3,"kind":"volley","text":"Allen Gegnerhelfern 8 Lichtschaden.","class":"starlet","element":"light","value":8},
 "constellation":{"name":"Sternverbund","cost":3,"kind":"team_buff","text":"Eigene Helfer: +2 Angriff, 5 Schild.","class":"starlet","element":"light","value":2,"shield":5},
 "sunseed":{"name":"Sonnenkorn","cost":2,"kind":"party_heal","text":"Beide Wesen: +10 LP und 4 Schutz.","class":"starlet","element":"light","value":10,"barrier":4},
 "coal_song":{"name":"Kohlenlied","cost":2,"kind":"buff","text":"Helfer: +3 Angriff / +1 LP.","class":"wyrmling","element":"fire","value":3,"health":1,"target":"ally_unit"},
 "ember_rain":{"name":"Glutregen","cost":3,"kind":"volley","text":"Allen Gegnerhelfern 9 Feuerschaden.","class":"wyrmling","element":"fire","value":9},
 "cinder_bite":{"name":"Aschenbiss","cost":2,"kind":"damage","text":"12 Schaden +2 je eigenem Helfer.","class":"wyrmling","element":"fire","value":12,"per_helper":2},
 "rekindle":{"name":"Wiederentfachen","cost":3,"kind":"revive","text":"Letzte gefallene Helferkarte kehrt zurück.","class":"wyrmling","element":"fire"},
 "river_mend":{"name":"Flussverband","cost":2,"kind":"mend","text":"Eigener Helfer: +14 LP und 4 Schild.","class":"gloop","element":"water","value":14,"shield":4,"target":"ally_unit"},
 "backflow":{"name":"Rückströmung","cost":1,"kind":"recall","text":"Eigene Helferkarte zurück. Ziehe 1.","class":"gloop","element":"water","draw":1,"target":"ally_unit"},
 "ice_reed":{"name":"Eisrohr","cost":2,"kind":"freeze","text":"5 Schaden · Gegnerhelfer 1 Zug still.","class":"gloop","element":"water","value":5,"target":"enemy_unit"},
 "clear_current":{"name":"Klarer Strom","cost":3,"kind":"party_heal","text":"Beide Wesen: +16 LP; löst alle Fesseln.","class":"gloop","element":"water","value":16,"cleanse":true},
 "updraft":{"name":"Aufwind","cost":2,"kind":"buff","text":"Helfer bereit. +2 Angriff.","class":"puffling","element":"wind","value":2,"ready":true,"target":"ally_unit"},
 "seed_flight":{"name":"Samenflug","cost":2,"kind":"draw","text":"Ziehe 3 Karten.","class":"puffling","element":"wind","value":3},
 "wind_lattice":{"name":"Windgitter","cost":1,"kind":"buff","text":"Helfer weicht 1 Treffer aus. +2 LP.","class":"puffling","element":"wind","evasion":true,"health":2,"target":"ally_unit"},
 "crosswind":{"name":"Querwind","cost":3,"kind":"volley","text":"Allen Gegnerhelfern 7 Windschaden.","class":"puffling","element":"wind","value":7},
 "moon_pause":{"name":"Mondpause","cost":2,"kind":"freeze","text":"4 Schaden · Gegnerhelfer 1 Zug still.","class":"hootlet","element":"moon","value":4,"target":"enemy_unit"},
 "owl_script":{"name":"Eulenschrift","cost":2,"kind":"draw","text":"Ziehe 2 Karten; +6 LP.","class":"hootlet","element":"moon","value":2,"heal_self":6},
 "moon_cape":{"name":"Mondumhang","cost":1,"kind":"buff","text":"Helfer: 1× Ausweichen und 3 Schild.","class":"hootlet","element":"moon","evasion":true,"shield":3,"target":"ally_unit"},
 "quiet_circle":{"name":"Stiller Kreis","cost":3,"kind":"weaken","text":"Nachtwurz 2 Züge schwach. Gruppe +8 LP.","class":"hootlet","element":"moon","value":2,"heal_party":8},
 "root_growth":{"name":"Wurzelwachstum","cost":2,"kind":"buff","text":"Helfer: +2 Angriff / +6 LP.","class":"mosslyn","element":"nature","value":2,"health":6,"target":"ally_unit"},
 "green_recall":{"name":"Grüne Erinnerung","cost":3,"kind":"revive","text":"Letzte gefallene Helferkarte kehrt zurück.","class":"mosslyn","element":"nature"},
 "grove_weave":{"name":"Hainflechten","cost":3,"kind":"team_buff","text":"Eigene Helfer: +4 LP und 4 Schild.","class":"mosslyn","element":"nature","health":4,"shield":4},
 "sap_reading":{"name":"Saftlesen","cost":1,"kind":"draw","text":"Ziehe 1 Karte und heile dich um 6.","class":"mosslyn","element":"nature","value":1,"heal_self":6},
 "echo_cloak":{"name":"Echohülle","cost":2,"kind":"buff","text":"Helfer: +1 Angriff, 1× Ausweichen.","class":"nocti","element":"moon","value":1,"evasion":true,"target":"ally_unit"},
 "night_return":{"name":"Nachtkehr","cost":1,"kind":"recall","text":"Eigene Helferkarte zurück. +5 LP.","class":"nocti","element":"moon","heal_self":5,"target":"ally_unit"},
 "shadow_bite":{"name":"Schattenbiss","cost":3,"kind":"damage","text":"18 Schaden; heilt dich um 8 LP.","class":"nocti","element":"moon","value":18,"heal_self":8},
 "hushed_echo":{"name":"Stummes Echo","cost":2,"kind":"weaken","text":"Nachtwurz 1 Zug schwach. Ziehe 1.","class":"nocti","element":"moon","value":1,"draw":1}
}
const ULTIMATES := {"fennec":"wild_rally","starlet":"starfall","wyrmling":"phoenix","gloop":"tide","puffling":"stormflock","hootlet":"eclipse","mosslyn":"ancient","nocti":"mirror"}
const HELPER_FORMS := ["sprigling","moss_guard","echolet","night_guard","phoenix","stormfalcon","ancient","mirror","lightling","star_guard","emberling","coal_guard","dewling","spring_guard","breezeling","wind_guard","moonling","moon_guard","lantern_snail","leaf_beetle","mushroom_archivist","thistle_nurse","mist_glider","acorn_sentinel"]
const SPECIAL := {
 "fennec":["cat","boar","wolf"],"starlet":["lightling","star_guard","radiance"],
 "wyrmling":["emberling","coal_guard","firebolt"],"gloop":["dewling","spring_guard","spring"],
 "puffling":["breezeling","wind_guard","gust"],"hootlet":["moonling","moon_guard","moonbeam"],
 "mosslyn":["sprigling","moss_guard","grove"],"nocti":["echolet","night_guard","echo"],
 "enemy":["thornling","shards","root_rest"]
}
const COMMON := ["spark","leaf_guard","tea","insight","lantern_snail","leaf_beetle","mushroom_archivist","thistle_nurse","mist_glider","acorn_sentinel","trail_blessing","bark_ward","dew_mend","frost_thread","homeward","second_wind","leaf_volley","moon_exchange","root_pact","fresh_sap","forest_memory","shared_rest"]
const CLASS_EXTRA := {"fennec":["pack_step","trail_mark","wild_cover","pack_shelter"],"starlet":["prism_ward","dawn_arc","constellation","sunseed"],"wyrmling":["coal_song","ember_rain","cinder_bite","rekindle"],"gloop":["river_mend","backflow","ice_reed","clear_current"],"puffling":["updraft","seed_flight","wind_lattice","crosswind"],"hootlet":["moon_pause","owl_script","moon_cape","quiet_circle"],"mosslyn":["root_growth","green_recall","grove_weave","sap_reading"],"nocti":["echo_cloak","night_return","shadow_bite","hushed_echo"]}
const START_COMMON := ["spark","leaf_guard","tea","insight","lantern_snail","leaf_beetle","mushroom_archivist","thistle_nurse","mist_glider","acorn_sentinel","trail_blessing","bark_ward","frost_thread"]
const VISUAL_FORMS := {"lightling":"lightling","star_guard":"star_guard","emberling":"emberling","coal_guard":"coal_guard","dewling":"dewling","spring_guard":"spring_guard","breezeling":"breezeling","wind_guard":"wind_guard","moonling":"moonling","moon_guard":"moon_guard"}

static func available(species: String) -> Array[String]:
 var result: Array[String]=[]
 if species!="enemy":result.assign(COMMON)
 result.append_array(SPECIAL.get(species,SPECIAL.fennec))
 if CLASS_EXTRA.has(species):result.append_array(CLASS_EXTRA[species])
 if ULTIMATES.has(species):result.append(ULTIMATES[species])
 return result

static func starter(species: String) -> Array[String]:
 var result: Array[String]=[]
 if species=="enemy":
  for id in SPECIAL.enemy:
   for copy in 4:result.append(id)
  return result
 for id in START_COMMON:
  result.append(id);result.append(id)
 for id in SPECIAL.get(species,SPECIAL.fennec):
  result.append(id);result.append(id)
 var choices: Array=CLASS_EXTRA.get(species,CLASS_EXTRA.fennec)
 for i in choices.size():
  result.append(choices[i])
  if i<3:result.append(choices[i])
 result.append(ULTIMATES.get(species,"wild_rally"))
 return result

static func copy_limit(species: String,id: String) -> int:
 return 4 if species=="enemy" else 1 if CARDS.get(id,{}).get("rare",false) else 2

static func valid(species: String,entries: Array) -> bool:
 if species=="enemy":
  if entries.size()<8 or entries.size()>MAX_DECK:return false
 elif entries.size()!=MAX_DECK:return false
 var counts: Dictionary={}
 for id in entries:
  if not id is String or id not in available(species):return false
  counts[id]=int(counts.get(id,0))+1
  if counts[id]>copy_limit(species,id):return false
 return true

# Kleine gespeicherte Decks behalten ihre Auswahl; nur freie Plaetze werden gefuellt.
# Laufende Kampfzonen werden hiervon nicht angefasst.
static func upgrade(species: String,entries: Array) -> Array[String]:
 var result: Array[String]=[]
 for id in entries:
  if id is String and id in available(species) and result.count(id)<copy_limit(species,id) and result.size()<MAX_DECK:result.append(id)
 if species=="enemy":return result if valid(species,result) else starter(species)
 for id in starter(species):
  if result.size()==MAX_DECK:break
  if result.count(id)<copy_limit(species,id):result.append(id)
 for id in available(species):
  while result.size()<MAX_DECK and result.count(id)<copy_limit(species,id):result.append(id)
 return result

static func get_card(id: String) -> Dictionary:
 var result: Dictionary=CARDS.get(id,{}).duplicate(true)
 result["id"]=id
 if VISUAL_FORMS.has(id):result["form"]=VISUAL_FORMS[id]
 return result
