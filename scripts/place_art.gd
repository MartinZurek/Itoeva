extends RefCounted

const Catalog = preload("res://scripts/world_catalog.gd")
const Forest = preload("res://scripts/forest_catalog.gd")

const ILLUSTRATED := ["SUN_GLADE", "MIST_WATCH", "MILL_BROOK", "ASH_ASCENT", "SEAL_COURT", "RELAY_COURT", "BEACH"]

static func picture(place: String, species: String, variation: int = 0, panorama: bool = false) -> Texture2D:
	if place in ILLUSTRATED:
		return load("res://assets/travel/%s_%d.png" % [place.to_lower(), variation])
	var region := Catalog.region_for(place)
	if not region.is_empty():
		var texture: Texture2D = load(region.asset)
		if panorama or region.places.size() == 1:
			return texture
		var atlas := AtlasTexture.new()
		atlas.atlas = texture
		var width := texture.get_width() / float(region.places.size())
		atlas.region = Rect2(region.places.find(place) * width, 0, width, texture.get_height())
		atlas.filter_clip = true
		return atlas
	if Catalog.data().interiors.has(place):
		return load(Catalog.interior_for(place, species).asset)
	for segment in Forest.data().segments:
		if segment.id == place:
			return load(segment.asset)
	return null

static func thumbnail(place: String, species: String, cache: Dictionary) -> Texture2D:
	var key := place + ":" + str(Catalog.personal_variant(place, species).get("asset", ""))
	if cache.has(key):
		return cache[key]
	var original := picture(place, species)
	if original == null:
		return null
	var image := original.get_image()
	if image == null or (image.is_compressed() and image.decompress() != OK):
		return original
	var extent := image.get_size()
	var ratio := minf(192.0 / extent.x, 108.0 / extent.y)
	image.resize(maxi(1, roundi(extent.x * ratio)), maxi(1, roundi(extent.y * ratio)), Image.INTERPOLATE_LANCZOS)
	var texture := ImageTexture.create_from_image(image)
	cache[key] = texture
	return texture
