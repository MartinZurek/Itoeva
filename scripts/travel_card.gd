extends Control

const PLACES = ["SUN_GLADE", "MIST_WATCH", "MILL_BROOK", "ASH_ASCENT", "SEAL_COURT", "RELAY_COURT", "BEACH"]
const ART = preload("res://assets/travel/places-concepts.png")
const Catalog = preload("res://scripts/world_catalog.gd")
const Scenery = preload("res://scripts/scenery_data.gd")
static var visits: Dictionary = {}
var place := ""
var title := ""
var progress := 0.1
var variation := 0
var picture: Texture2D

func _ready() -> void :
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	mouse_filter = Control.MOUSE_FILTER_STOP
	z_index = 100
	texture_filter = CanvasItem.TEXTURE_FILTER_LINEAR
	clip_contents = true
	variation = int(visits.get(place, 0)) % 2
	visits[place] = variation + 1
	var row := PLACES.find(place)
	if row >= 0:

		picture = load("res://assets/travel/%s_%d.png" % [place.to_lower(), variation])
	else:
		var region := Catalog.region_for(place)
		var asset: String = region.get("asset", Catalog.data().interiors.get(place, {}).get("asset", ""))
		if asset.is_empty():
			for segment in preload("res://scripts/forest_catalog.gd").data().segments:
				if segment.id == place: asset = segment.asset;break
		if Scenery.data().has(asset): picture = Scenery.fallback(asset)
		elif not asset.is_empty() and ResourceLoader.exists(asset): picture = load(asset)
	resized.connect(queue_redraw)
	queue_redraw()

func _draw() -> void :
	draw_rect(Rect2(Vector2.ZERO, size), Color("111f24"))
	if picture != null:

		var factor := maxf(size.x / picture.get_width(), size.y / picture.get_height())
		var extent := picture.get_size() * factor
		draw_texture_rect(picture, Rect2((size - extent) * 0.5, extent), false)

	for i in 24:
		var y := size.y * (0.72 + 0.28 * float(i) / 24.0)
		draw_rect(Rect2(0, y, size.x, size.y * 0.28 / 24.0 + 1), Color(0.025, 0.04, 0.05, 0.55 * float(i) / 23.0))
	var font := ThemeDB.fallback_font
	var title_size := maxi(22, int(size.y * 0.044))
	var width := font.get_string_size(title, HORIZONTAL_ALIGNMENT_LEFT, -1, title_size).x
	draw_string(font, Vector2((size.x - width) * 0.5, size.y * 0.865), title, HORIZONTAL_ALIGNMENT_LEFT, -1, title_size, Color("f4dfb1"))
	var bar := Rect2(size.x * 0.34, size.y * 0.92, size.x * 0.32, 3)
	draw_rect(bar, Color("334b4d"))
	bar.size.x *= clampf(progress, 0.0, 1.0)
	draw_rect(bar, Color("c7b483"))
