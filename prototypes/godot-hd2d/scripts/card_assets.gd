extends RefCounted
const ART := {"spark":0,"leaf_guard":1,"tea":2,"insight":3,"radiance":4,"firebolt":5,"spring":6,"gust":7,"moonbeam":8,"shards":9,"root_rest":10,"grove":11,"echo":12,"wild_rally":13,"starfall":14,"phoenix":15,"tide":16,"stormflock":17,"eclipse":18,"ancient":19,"mirror":20}
const COLORS := {"all":"8ab9ca","fennec":"b6d394","starlet":"f4ce85","wyrmling":"e39b77","gloop":"80d1d5","puffling":"a8cce5","hootlet":"bcafe3","mosslyn":"8ccc9b","nocti":"c2a2e7","enemy":"bc8fa3"}
static var combat: Dictionary={}
static var spirits: Dictionary={}
static var wildlife: Dictionary={}

static func accent(card: Dictionary) -> Color:return Color(COLORS.get(card.get("class","all"),"8ab9ca"))

static func form_texture(form: String,frame: int=0) -> AtlasTexture:
 if spirits.is_empty():spirits=JSON.parse_string(FileAccess.get_file_as_string("res://data/spirit_animations.json"))
 var atlas:=AtlasTexture.new();atlas.atlas=load(spirits.asset)
 var r: Array=spirits.regions[form][clampi(frame,0,3)];atlas.region=Rect2(r[0],r[1],r[2],r[3]);return atlas

static func form_height(form: String) -> float:
 if spirits.is_empty():spirits=JSON.parse_string(FileAccess.get_file_as_string("res://data/spirit_animations.json"))
 return float(spirits.reference_height[form])

static func texture(card: Dictionary) -> AtlasTexture:
 var atlas:=AtlasTexture.new()
 if card.get("form","")!="":return form_texture(card.form)
 if ART.has(card.id):
  atlas.atlas=load("res://assets/combat/card-art-v050.png")
  var dimensions:=Vector2(atlas.atlas.get_width()/6.0,atlas.atlas.get_height()/4.0)
  var index: int=ART[card.id];atlas.region=Rect2(Vector2(index%6,index/6)*dimensions,dimensions);return atlas
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
