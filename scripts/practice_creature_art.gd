extends RefCounted

const SUBJECT := "K0220"
const IMAGE := "res://assets/cinematics/vexzelanz-poses.png"
const REGIONS := [
	Rect2(12, 236, 692, 406),
	Rect2(736, 151, 680, 492),
	Rect2(1460, 52, 700, 594)
]
const GROUND_PIXELS := [401.0, 487.0, 590.0]
const CONTACT_PIXELS := [[56.0, 473.0, 630.0], [66.0, 469.0], [80.0, 391.0, 641.0]]
const PIXEL_SIZE := 0.00325
static var frames: Array[AtlasTexture] = []

static func pose(index: int) -> AtlasTexture:
	if frames.is_empty():
		var sheet: Texture2D = load(IMAGE)
		for region in REGIONS:
			var frame := AtlasTexture.new()
			frame.atlas = sheet
			frame.region = region
			frame.filter_clip = true
			frames.append(frame)
	return frames[index]

static func planted_offset(index: int) -> Vector2:
	return Vector2(0, GROUND_PIXELS[index] - REGIONS[index].size.y * 0.5)

static func contact_offsets(index: int) -> Array[float]:
	var result: Array[float] = []
	for pixel in CONTACT_PIXELS[index]:
		result.append((pixel - REGIONS[index].size.x * 0.5) * PIXEL_SIZE)
	return result
