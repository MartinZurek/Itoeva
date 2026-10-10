extends Node3D

const ATLAS: Texture2D = preload("res://assets/fennec-cast.png")
# Fussanker werden aus den acht Originalzellen abgelesen, ohne Figuren zu stauchen.
const FEET := [420.0, 420.0, 409.0, 409.0, 394.0, 394.0, 375.0, 372.0]
const FOOT_CENTERS := [248.0, 227.0, 257.5, 232.5, 264.5, 241.5, 248.0, 245.0]
var sprite: Sprite3D
var current_pose := -1

func _ready() -> void:
	sprite = Sprite3D.new()
	sprite.texture = ATLAS
	sprite.hframes = 2
	sprite.vframes = 4
	sprite.pixel_size = 0.009
	# Nur um die Hochachse drehen: Kippen zur Kamera wuerde die Fusskante ueber den Boden heben.
	sprite.billboard = BaseMaterial3D.BILLBOARD_FIXED_Y
	sprite.alpha_cut = SpriteBase3D.ALPHA_CUT_DISCARD
	sprite.alpha_scissor_threshold = 0.50
	# Gemaltes Koerperlicht bleibt erhalten; eine flache Sprite-Normale modelliert kein Fellvolumen.
	sprite.shaded = false
	sprite.texture_filter = BaseMaterial3D.TEXTURE_FILTER_LINEAR_WITH_MIPMAPS
	sprite.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	add_child(sprite)
	set_pose(0)

func set_pose(index: int) -> void:
	if current_pose == index:
		return
	current_pose = index
	sprite.frame = index
	var height := float(ATLAS.get_height()) / 4.0
	sprite.position.y = (FEET[index] - height * 0.5) * sprite.pixel_size
	sprite.offset.x = float(ATLAS.get_width()) * 0.25 - FOOT_CENTERS[index]
