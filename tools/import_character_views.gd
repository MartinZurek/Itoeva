extends SceneTree

# Einmalige Atlasaufbereitung: gemeinsame Größe und Bodenlinie statt Skalierung je Pose.
func _initialize() -> void:
	for species: String in ["mosslyn", "wyrmling", "nocti", "starlet"]:
		var deer: bool = species == "mosslyn"
		var image := Image.load_from_file("res://assets/characters/source/" + ("mosslyn-views.png" if deer else "companions-views.png"))
		var columns := 4 if deer else 6
		var rows := 4 if deer else 3
		var source_cell := Vector2i(image.get_width() / columns, image.get_height() / rows)
		var row := 0 if deer else ["wyrmling", "nocti", "starlet"].find(species)
		var reference: int = {"mosslyn":168, "wyrmling":118, "nocti":130, "starlet":150}[species]
		var first := image.get_region(Rect2i(Vector2i(0, row * source_cell.y), source_cell))
		var factor := float(reference) / first.get_used_rect().size.y
		var count := 16 if deer else 6
		var atlas := Image.create(1280, 320 * ceili(count / 4.0), false, Image.FORMAT_RGBA8)
		var mask := Image.create(atlas.get_width(), atlas.get_height(), false, Image.FORMAT_R8)
		var bounds: Array = []
		for i in count:
			var source_row: int = i / 4 if deer else row
			var source_column: int = i % 4 if deer else i
			var cell := image.get_region(Rect2i(Vector2i(source_column, source_row) * source_cell, source_cell))
			cell = cell.get_region(cell.get_used_rect())
			cell.resize(maxi(1, roundi(cell.get_width() * factor)), maxi(1, roundi(cell.get_height() * factor)), Image.INTERPOLATE_NEAREST)
			var at := Vector2i((320 - cell.get_width()) / 2, 300 - cell.get_height())
			var origin := Vector2i(i % 4, i / 4) * 320
			atlas.blit_rect(cell, Rect2i(Vector2i.ZERO, cell.get_size()), origin + at)
			mask.fill_rect(Rect2i(origin + at, cell.get_size()), Color(float(i + 1) / 255.0, 0, 0))
			bounds.append([at.x, at.y, cell.get_width(), cell.get_height()])
		var asset := "res://assets/characters/pixel/" + species + "-views-v2.png"
		atlas.save_png(asset)
		var data := {"asset":asset, "cell":320, "columns":4, "rows":ceili(count / 4.0), "frames":count, "ground":300, "reference_height":reference, "fixed_geometry":true, "bounds":bounds, "width":atlas.get_width(), "height":atlas.get_height(), "mask_deflate":Marshalls.raw_to_base64(mask.get_data().compress(FileAccess.COMPRESSION_DEFLATE))}
		FileAccess.open("res://data/" + species + "_views.json", FileAccess.WRITE).store_string(JSON.stringify(data, "  "))
	quit()
