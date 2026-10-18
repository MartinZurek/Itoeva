extends Panel
const Art=preload("res://scripts/card_assets.gd")
var app: Node
var card: Dictionary={}
var close_callback: Callable
var previous_callback: Callable
var next_callback: Callable
var registered: Array[Button]=[]

func _ready() -> void:
 var box:=StyleBoxFlat.new();box.bg_color=Color("10202c");box.border_color=Art.accent(card)
 box.set_border_width_all(2);box.set_corner_radius_all(16);add_theme_stylebox_override("panel",box)
 _label(card.name,Vector2(22,15),Vector2(550,38),25,Art.accent(card))
 var image:=TextureRect.new();image.texture=Art.texture(card)
 image.expand_mode=TextureRect.EXPAND_IGNORE_SIZE;image.stretch_mode=TextureRect.STRETCH_KEEP_ASPECT_CENTERED
 image.position=Vector2(22,61);image.size=Vector2(178,170);image.mouse_filter=Control.MOUSE_FILTER_IGNORE;add_child(image)
 _label("%s Kristalle · %s" % [card.cost,Art.kind_label(card)],Vector2(220,57),Vector2(355,34),15,Art.accent(card))
 var scroll:=ScrollContainer.new();scroll.position=Vector2(220,99);scroll.size=Vector2(355,136);scroll.horizontal_scroll_mode=ScrollContainer.SCROLL_MODE_DISABLED;add_child(scroll)
 var text:=Label.new();text.text=card.get("detail",card.text);text.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART;text.custom_minimum_size=Vector2(330,0);text.size_flags_horizontal=Control.SIZE_EXPAND_FILL
 text.add_theme_font_size_override("font_size",16);text.add_theme_color_override("font_color",Color("eee5d0"));scroll.add_child(text)

 if previous_callback.is_valid():_button("←",Vector2(22,248),Vector2(75,43),previous_callback)
 if next_callback.is_valid():_button("→",Vector2(107,248),Vector2(75,43),next_callback)
 _button("Schließen",Vector2(418,248),Vector2(157,43),close_callback)

func _label(text: String,at: Vector2,dimensions: Vector2,font_size: int,color: Color) -> Label:
 var label:=Label.new();label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART;label.text=text
 label.position=at;label.size=dimensions;label.mouse_filter=Control.MOUSE_FILTER_IGNORE
 label.add_theme_font_size_override("font_size",font_size);label.add_theme_color_override("font_color",color);add_child(label);return label

func _button(text: String,at: Vector2,dimensions: Vector2,callback: Callable) -> void:
 var b: Button=app._button(text,callback);b.custom_minimum_size=Vector2.ZERO;b.position=at;b.size=dimensions
 b.set_meta("battle_action","card_info_close" if text=="Schließen" else "card_info_previous" if text=="←" else "card_info_next")
 registered.append(b);add_child(b)

func detach() -> void:
 for b in registered:app.menu_buttons.erase(b)
 registered.clear();queue_free()
