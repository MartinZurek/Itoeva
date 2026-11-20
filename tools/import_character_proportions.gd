extends SceneTree

# Technical atlas registration: transparent export fringe must not count as body.
func clean_cell(source: Image, columns: int, rows: int, index: int) -> Image:
	var size := Vector2i(source.get_width() / columns, source.get_height() / rows)
	var rect := Rect2i(Vector2i(index % columns, index / columns) * size, size)
	# Generated rows are regular; use measured empty gutters between columns.
	if source.get_width() == 1448 and columns == 4:
		var gutters := [0, 400, 720, 1075, 1448]
		rect.position.x = gutters[index % columns]
		rect.size.x = gutters[index % columns + 1] - rect.position.x
	var cell := source.get_region(rect)
	cell.convert(Image.FORMAT_RGBA8)
	for y in cell.get_height():
		for x in cell.get_width():
			if cell.get_pixel(x, y).a < 0.31: cell.set_pixel(x, y, Color(0, 0, 0, 0))
	return cell.get_region(cell.get_used_rect())

func write_atlas(cells: Array[Image], scales: Array[float], asset: String, reference: float) -> Dictionary:
	var rows := ceili(cells.size() / 4.0)
	var atlas := Image.create(1280, rows * 320, false, Image.FORMAT_RGBA8)
	var mask := Image.create(1280, rows * 320, false, Image.FORMAT_R8)
	var bounds: Array = []
	for i in cells.size():
		var cell := cells[i]
		cell.resize(roundi(cell.get_width() * scales[i]), roundi(cell.get_height() * scales[i]), Image.INTERPOLATE_NEAREST)
		var at := Vector2i((320 - cell.get_width()) / 2, 300 - cell.get_height())
		var origin := Vector2i(i % 4, i / 4) * 320
		atlas.blit_rect(cell, Rect2i(Vector2i.ZERO, cell.get_size()), origin + at)
		mask.fill_rect(Rect2i(origin + at, cell.get_size()), Color(float(i + 1) / 255.0, 0, 0))
		bounds.append([at.x, at.y, cell.get_width(), cell.get_height()])
	atlas.save_png(asset)
	return {"asset":asset, "cell":320, "columns":4, "rows":rows, "frames":cells.size(), "ground":300, "reference_height":reference, "fixed_geometry":true, "bounds":bounds, "width":1280, "height":rows*320, "alpha_cutoff":0.31, "mask_deflate":Marshalls.raw_to_base64(mask.get_data().compress(FileAccess.COMPRESSION_DEFLATE))}

func _initialize() -> void:
	var new_companions := Image.load_from_file("res://assets/characters/source/companions-front-back-v3.png")
	var old_companions := Image.load_from_file("res://assets/characters/source/companions-views.png")
	var deer := Image.load_from_file("res://assets/characters/source/mosslyn-views.png")
	for species: String in ["mosslyn", "wyrmling", "nocti", "starlet"]:
		var reference: float = {"mosslyn":168.0, "wyrmling":118.0, "nocti":130.0, "starlet":150.0}[species]
		var cells: Array[Image] = []
		var scales: Array[float] = []
		var row := ["wyrmling", "nocti", "starlet"].find(species)
		for i in (16 if species == "mosslyn" else 6):
			cells.append(clean_cell(deer, 4, 4, i) if species == "mosslyn" else clean_cell(new_companions, 4, 3, row * 4 + i) if i < 4 else clean_cell(old_companions, 6, 3, row * 6 + i))
		for i in cells.size():
			var anchor := 4 if species == "mosslyn" and i in range(4,8) else 2 if species != "mosslyn" and i in [2,3] else 0
			var target := 163.0 if species == "mosslyn" else reference
			var height := cells[anchor].get_height()
			if species != "mosslyn" and i >= 4:
				height = clean_cell(old_companions, 6, 3, row * 6).get_height()
			scales.append(target / height)
		var data := write_atlas(cells, scales, "res://assets/characters/pixel/" + species + "-views-v3.png", reference)
		FileAccess.open("res://data/" + species + "_views.json", FileAccess.WRITE).store_string(JSON.stringify(data, "  "))
	var walk_source := Image.load_from_file("res://assets/characters/source/mosslyn-walk-compact-v3.png")
	var walk_cells: Array[Image] = []
	var walk_scales: Array[float] = []
	var scale := 150.0 / clean_cell(walk_source, 4, 2, 0).get_height()
	for i in 8:
		walk_cells.append(clean_cell(walk_source, 4, 2, i))
		walk_scales.append(scale)
	var walk := write_atlas(walk_cells, walk_scales, "res://assets/characters/pixel/mosslyn-walk-compact-v3.png", 168.0)
	walk["cycle_distance_pixels"] = 140.0
	var cycles: Dictionary = JSON.parse_string(FileAccess.get_file_as_string("res://data/mosslyn_m4_cycles.json"))
	cycles.walk = walk
	FileAccess.open("res://data/mosslyn_m4_cycles.json", FileAccess.WRITE).store_string(JSON.stringify(cycles, "  "))
	quit()
