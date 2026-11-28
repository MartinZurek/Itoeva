extends Control

const PlaceArt = preload("res://scripts/place_art.gd")
const Clock = preload("res://scripts/world_clock.gd")
const Ambience = preload("res://scripts/painted_ambience.gd")
const Catalog = preload("res://scripts/world_catalog.gd")
static var visits: Dictionary = {}
var place := ""
var title := ""
var origin_title := ""
var hour := 12.0
var species := ""
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
	picture = PlaceArt.picture(place, species, variation)
	resized.connect(queue_redraw)
	queue_redraw()

func _draw() -> void :
	draw_rect(Rect2(Vector2.ZERO, size), Color("111f24"))
	if picture != null:
		var extent := PlaceArt.fitted_extent(picture, size, get_screen_transform().get_scale())
		draw_texture_rect(picture, Rect2(((size - extent) * 0.5).round(), extent), false, picture_tint())

	for i in 24:
		var y := size.y * (0.72 + 0.28 * float(i) / 24.0)
		draw_rect(Rect2(0, y, size.x, size.y * 0.28 / 24.0 + 1), Color(0.025, 0.04, 0.05, 0.55 * float(i) / 23.0))
	var font := ThemeDB.fallback_font
	var title_size := maxi(22, int(size.y * 0.044))
	var width := font.get_string_size(title, HORIZONTAL_ALIGNMENT_LEFT, -1, title_size).x
	draw_string(font, Vector2((size.x - width) * 0.5, size.y * 0.827), title, HORIZONTAL_ALIGNMENT_LEFT, -1, title_size, Color("f4dfb1"))
	var route := route_caption()
	var subtitle_size := maxi(14, int(size.y * 0.023))
	var route_width := font.get_string_size(route, HORIZONTAL_ALIGNMENT_LEFT, -1, subtitle_size).x
	draw_string(font, Vector2((size.x - route_width) * 0.5, size.y * 0.884), route,
		HORIZONTAL_ALIGNMENT_LEFT, -1, subtitle_size, Color("d4dac9"))
	var bar := Rect2(size.x * 0.34, size.y * 0.92, size.x * 0.32, 3)
	draw_rect(bar, Color("334b4d"))
	bar.size.x *= clampf(progress, 0.0, 1.0)
	draw_rect(bar, Color("c7b483"))

func picture_tint() -> Color:
	# Auf kleineren Displays muss auch das Nachtmotiv lesbar bleiben.
	var indoor: bool = Catalog.data().interiors.has(place) and not Catalog.is_outdoor(place)
	return Color.WHITE.lerp(Ambience.scene_color(hour, indoor), 0.54)

func route_caption() -> String:
	var timing := Clock.phase_label(hour)
	return timing if origin_title.is_empty() else "Von " + origin_title + " · " + timing
