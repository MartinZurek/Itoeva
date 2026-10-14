extends RefCounted
const ART := {"spark":0,"leaf_guard":1,"tea":2,"insight":3,"radiance":4,"firebolt":5,"spring":6,"gust":7,"moonbeam":8,"shards":9,"root_rest":10,"grove":11,"echo":12,"wild_rally":13,"starfall":14,"phoenix":15,"tide":16,"stormflock":17,"eclipse":18,"ancient":19,"mirror":20}
const COLORS := {"all":"8ab9ca","fennec":"b6d394","starlet":"f4ce85","wyrmling":"e39b77","gloop":"80d1d5","puffling":"a8cce5","hootlet":"bcafe3","mosslyn":"8ccc9b","nocti":"c2a2e7","enemy":"bc8fa3"}
static var combat: Dictionary={}
static var spirits: Dictionary={}
static var wildlife: Dictionary={}
static var helpers: Dictionary={}

static func accent(card: Dictionary) -> Color:return Color(COLORS.get(card.get("class","all"),"8ab9ca"))

static func form_info(form: String) -> Dictionary:
 if helpers.is_empty():helpers=JSON.parse_string(FileAccess.get_file_as_string("res://data/helper_animations_v060.json"))
 if helpers.forms.has(form):return helpers.forms[form]
 if spirits.is_empty():spirits=JSON.parse_string(FileAccess.get_file_as_string("res://data/spirit_animations.json"))
 return {"asset":spirits.asset,"regions":spirits.regions[form],"reference_height":spirits.reference_height[form],
  "idle_sequence":[0,0,0,1,0,0],"attack_sequence":[0,2,2,0],"hit_sequence":[3,3,0],"tempo":1.5,
  "style":"flier" if form in ["echolet","night_guard","phoenix","stormfalcon","mirror"] else "guard" if form in ["moss_guard","ancient"] else "pounce"}

static func form_texture(form: String,frame: int=0) -> AtlasTexture:
 var info:=form_info(form)
 var atlas:=AtlasTexture.new();atlas.atlas=load(info.asset)
 var r: Array=info.regions[clampi(frame,0,info.regions.size()-1)]
 atlas.region=Rect2(r[0],r[1],r[2],r[3]);return atlas

static func form_height(form: String) -> float:return float(form_info(form).reference_height)

static func animation_frame(form: String,clock: float,action: String="idle",progress: float=0.0) -> int:
 var info:=form_info(form)
 var sequence: Array=info.get(action+"_sequence",info.idle_sequence)
 if action=="idle":return int(sequence[posmod(int(clock/float(info.tempo)*sequence.size()),sequence.size())])
 return int(sequence[clampi(int(progress*sequence.size()),0,sequence.size()-1)])

static func movement_style(form: String) -> String:return str(form_info(form).style)

static func texture(card: Dictionary) -> AtlasTexture:
 var atlas:=AtlasTexture.new()
 if card.get("form","")!="":return form_texture(card.form)
 if ART.has(card.id):
  atlas.atlas=load("res://assets/combat/card-art-v050.png")
  var dimensions:=Vector2(atlas.atlas.get_width()/6.0,atlas.atlas.get_height()/4.0)
  var index: int=ART[card.id];atlas.region=Rect2(Vector2(index%6,index/6)*dimensions,dimensions);return atlas
 if card.id not in ART and card.kind!="minion":
  var representative: String={"nature":"insight","guard":"leaf_guard","heal":"tea","water":"spring","wind":"gust","fire":"firebolt","moon":"moonbeam","light":"radiance"}.get(card.element,"insight")
  var index: int=ART[representative]
  atlas.atlas=load("res://assets/combat/card-art-v050.png")
  var dimensions:=Vector2(atlas.atlas.get_width()/6.0,atlas.atlas.get_height()/4.0)
  atlas.region=Rect2(Vector2(index%6,index/6)*dimensions,dimensions);return atlas
 if card.has("row"):
  if wildlife.is_empty():wildlife=JSON.parse_string(FileAccess.get_file_as_string("res://data/wildlife_atlas.json"))
  var r: Array=wildlife.regions[int(card.row)][0];atlas.atlas=load(wildlife.asset);atlas.region=Rect2(r[0],r[1],r[2],r[3]);return atlas
 if card.get("species","")=="enemy":
  atlas.atlas=load("res://assets/combat/nachtwurz-atlas.png");atlas.region=Rect2(0,0,328,304);return atlas
 if combat.is_empty():
  combat=JSON.parse_string(FileAccess.get_file_as_string("res://data/combat_atlases.json"))
  combat.merge(JSON.parse_string(FileAccess.get_file_as_string("res://data/new_combat_atlases.json")),true)
 var species: String=card.get("species",card.get("class","starlet"))
 var info: Dictionary=combat.get(species,combat.starlet)
 var r: Array=info.regions[12];atlas.atlas=load(info.asset);atlas.region=Rect2(r[0],r[1],r[2],r[3]);return atlas

static func kind_label(card: Dictionary) -> String:
 return "SPEZIAL · EINMAL IM DECK" if card.get("rare",false) else "HELFER · SCHUTZ" if card.get("taunt",false) else "HELFER" if card.kind=="minion" else "ZAUBER"
