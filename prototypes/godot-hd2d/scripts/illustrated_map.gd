extends Control
# Decorative terrain is painted; every route and clickable marker follows game data.
const Catalog = preload("res://scripts/world_catalog.gd")
const Residents = preload("res://scripts/world_residents.gd")
const Enemies=preload("res://scripts/regional_enemies.gd")
var app: Node3D
var background: Texture2D
var nodes: Array[Dictionary] = []
var links: Array = []
var selected := ""
var map_rect := Rect2()
var detail: RichTextLabel
var heading: Label
var close: Button
var chips: Array[Button] = []
var legend: Label
var clock := 0.0

func _ready() -> void:
 background=load("res://assets/expansion/world-atlas.png")
 mouse_filter=Control.MOUSE_FILTER_STOP
 nodes=[
  _node("SHORES","Küste & Teich",Vector2(.09,.79),["POND","BEACH","SWAMP","JUNGLE"]),
  _node("COAST_PATH","Küstenpfad",Vector2(.23,.79),["COAST_PATH"]),
  _node("STREET","Dorf & Zuhause",Vector2(.38,.79),["STREET","LIVING","BEDROOM","BATH","DESK","NOOK","KITCHEN","CRAFT","SHOP"]),
  _node("PARK","Park",Vector2(.39,.65),["PARK"]),
  _node("MEADOW","Wiese",Vector2(.45,.51),["MEADOW"]),
  _node("FOREST","Waldrand",Vector2(.36,.39),["FOREST"]),
  _node("VILLAGE_EDGE","Dorfrand",Vector2(.54,.79),["VILLAGE_EDGE"]),
  _node("CITY","Markt & Ebene",Vector2(.69,.79),["CITY","SPORT","PLAINS","WORK","CAFE","ARCADE"]),
  _node("MOUNTAINS","Berge",Vector2(.84,.79),["MOUNTAINS"]),
  _node("MOUNTAIN_PASS","Bergpass",Vector2(.86,.64),["MOUNTAIN_PASS"]),
  _node("CAMP","Lager & Grotte",Vector2(.87,.49),["CAMP","GROTTO"]),
  _node("FOREST_ENTRANCE","Waldsaum",Vector2(.36,.27),["FOREST_ENTRANCE"]),
  _node("GREENWOOD","Grüner Wald",Vector2(.23,.20),["GREENWOOD"]),
  _node("TORCH_PATH","Letzte Fackeln",Vector2(.39,.10),["TORCH_PATH"]),
  _node("SHADOW_GATE","Schattenwald",Vector2(.55,.10),["SHADOW_GATE"]),
  _node("SHADOW_BROOK","Schattenbach",Vector2(.60,.27),["SHADOW_BROOK"]),
  _node("SHADOW_CLEARING","Nachtlichtung",Vector2(.72,.15),["SHADOW_CLEARING"]),
  _node("SHADOW_DEPTHS","Tiefer Wald",Vector2(.82,.28),["SHADOW_DEPTHS"]),
  _node("SUN_GLADE","Sonnenhain",Vector2(.12,.43),["SUN_GLADE"]),
  _node("MIST_WATCH","Nebelwacht",Vector2(.70,.41),["MIST_WATCH"]),
  _node("MILL_BROOK","Mühlenbach",Vector2(.12,.59),["MILL_BROOK"]),
  _node("ASH_ASCENT","Aschenstieg",Vector2(.73,.53),["ASH_ASCENT"]),
  _node("SEAL_COURT","Siegelhof",Vector2(.70,.63),["SEAL_COURT"])]
 # A continuous outdoor route, seven forest sections, then two converging branches.
 links=[[0,1],[1,2],[2,3],[3,4],[4,5],[5,6],[6,7],[7,8],[8,9],[9,10],
  [5,11],[11,12],[12,13],[13,14],[14,15],[15,16],[16,17],[5,18],[12,18],[18,19],[17,19],[18,20],[19,21],[21,22]]
 heading=Label.new();heading.text="ITOЕVA  ·  Atlas der bekannten Wege";heading.add_theme_font_size_override("font_size",27);add_child(heading)
 close=Button.new();close.text="Zurück zur Welt";close.pressed.connect(app._resume);add_child(close)
 app.menu_buttons.append(close)
 detail=RichTextLabel.new();detail.bbcode_enabled=true;detail.scroll_active=true;detail.add_theme_font_size_override("normal_font_size",19);detail.add_theme_color_override("default_color",Color("f4e4c4"));add_child(detail)
 legend=Label.new();legend.text="● Hier   ◉ Besucht   ○ Unentdeckt   ·   Orte antippen: Bewohner, Wege und Entdeckungen";legend.add_theme_font_size_override("font_size",17);add_child(legend)
 for node in nodes:
  var chip:=Button.new();chip.text=node.label;chip.add_theme_font_size_override("font_size",16)
  var box:=StyleBoxFlat.new();box.bg_color=Color("f5e5bf");box.border_color=Color("73583a");box.set_border_width_all(1);box.set_corner_radius_all(10)
  chip.add_theme_stylebox_override("normal",box);chip.add_theme_color_override("font_color",Color("342b25"));chip.add_theme_color_override("font_hover_color",Color("382512"))
  chip.pressed.connect(func():_select(node.id));add_child(chip);chips.append(chip);app.menu_buttons.append(chip)
  if app.current_place() in node.places:selected=node.id
 resized.connect(_layout)
 _layout();_select(selected if selected!="" else "STREET")

func _node(id: String,title: String,uv: Vector2,places: Array) -> Dictionary:
 return {"id":id,"label":title,"uv":uv,"places":places}

func _layout() -> void:
 var scale_factor:=minf(size.x/1280.0,size.y/720.0)
 var board:=Vector2(1280,720)*scale_factor
 var origin:=(size-board)*.5
 map_rect=Rect2(origin+Vector2(16,72)*scale_factor,Vector2(940,580)*scale_factor)
 heading.position=origin+Vector2(24,17)*scale_factor;heading.scale=Vector2.ONE*scale_factor
 close.position=origin+Vector2(1050,14)*scale_factor;close.size=Vector2(206,48);close.scale=Vector2.ONE*scale_factor
 detail.position=origin+Vector2(976,88)*scale_factor;detail.size=Vector2(286,558);detail.scale=Vector2.ONE*scale_factor
 legend.position=origin+Vector2(24,675)*scale_factor;legend.scale=Vector2.ONE*scale_factor
 for i in chips.size():
  var chip:=chips[i];chip.size=Vector2(126,35);chip.scale=Vector2.ONE*scale_factor
  chip.position=_point(i)-Vector2(63,17.5)*scale_factor
 queue_redraw()

func _point(index: int) -> Vector2:
 return map_rect.position+Vector2(nodes[index].uv)*map_rect.size

func _select(id: String) -> void:
 selected=id
 var node: Dictionary={}
 var index:=0
 for i in nodes.size():
  if nodes[i].id==id:node=nodes[i];index=i;break
 if node.is_empty():return
 var visited: Array=app.save_data.get("discovered",[])
 var lines:=PackedStringArray(["[font_size=25][color=#eac886]"+str(node.label)+"[/color][/font_size]",""])
 for place in node.places:
  lines.append(("[color=#aee1c5]● Hier[/color] " if place==app.current_place() else "◉ " if place in visited else "○ ")+str(Catalog.data().labels.get(place,place)))
 lines.append("\n[color=#eac886]Verbundene Wege[/color]")
 for link in links:
  if index in link:lines.append("• "+str(nodes[link[1] if link[0]==index else link[0]].label))
 lines.append("\n[color=#eac886]Bewohner · "+app.world_clock.local_datetime().get("zone","")+"[/color]")
 var inhabitants:=0
 for resident in Residents.data():
  if resident==app.selected:continue
  var appointment: Dictionary=app.resident_appointments[resident]
  if appointment.place in node.places:
   inhabitants+=1;lines.append(str(Residents.data()[resident].name)+" · "+str(Residents.data()[resident].role))
 if inhabitants==0:lines.append("Zurzeit keine Bewohner hier.")
 if id in ["SHADOW_GATE","SHADOW_BROOK","SHADOW_CLEARING","SHADOW_DEPTHS"]:lines.append("\n[color=#e5b3a1]Dunkelwald: keine Bewohner. Vorsicht vor Gegnern.[/color]")
 if id=="MILL_BROOK":lines.append("\nEin heller Bachweg an der alten Wassermühle. Miro wartet tagsüber das Wasserrad.")
 if id=="SUN_GLADE":lines.append("\nEin heller Birkenhain mit Kräutern und alten Grenzsteinen. Dornmotten bewachen den Weg.")
 if id=="MIST_WATCH":lines.append("\nDie Waldwege treffen an der alten Grenzwacht zusammen. Der Versorgungsweg führt am Hang zum Aschenstieg und zum Siegelhof. Die Schlucht wird nicht überquert.")
 if id=="ASH_ASCENT":lines.append("\nAlter Versorgungsweg zwischen Nebelwacht und Siegelhof. Schlackenträger beliefern die Grenzfeuer.")
 if id=="SEAL_COURT":lines.append("\nVharok bewacht das versiegelte Grenztor. Das Tor zur weiteren Dämonenwelt bleibt geschlossen.")
 lines.append("\n[color=#eac886]Wesen und Aufgaben[/color]")
 for enemy in Enemies.data().values():
  if enemy.place in node.places:lines.append(str(enemy.name)+" · "+str(enemy.get("purpose","")))
 detail.text="\n".join(lines)
 for i in chips.size():chips[i].modulate=Color("ffe7a1") if nodes[i].id==id else Color.WHITE
 queue_redraw()

func _process(delta: float) -> void:
 clock+=delta;queue_redraw()

func _draw() -> void:
 draw_rect(Rect2(Vector2.ZERO,size),Color("142624"))
 if not is_instance_valid(background) or nodes.is_empty():return
 draw_texture_rect(background,map_rect,false)
 for link in links:
  var a:=_point(link[0]);var b:=_point(link[1])
  draw_line(a,b,Color(.18,.15,.12,.75),7,true);draw_line(a,b,Color("d6b976"),3,true)
 for i in nodes.size():
  var point:=_point(i)+Vector2(0,-22)*map_rect.size.x/940
  var current: bool=app.current_place() in nodes[i].places
  var visited:=false
  for place in nodes[i].places:
   if place in app.save_data.get("discovered",[]):visited=true
  draw_circle(point,7,Color("3b352c"))
  draw_circle(point,4,Color("bce9cc") if current else Color("e7bd6e") if visited else Color("eee5d3"))
  if current:draw_arc(point,11+sin(clock*3)*1.5,0,TAU,40,Color("fff5c4"),2,true)
 # Der Versorgungsweg folgt dem Hang; keine Route überquert die Schlucht.
