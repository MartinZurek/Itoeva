extends SceneTree

func visible_bounds(image: Image, region: Rect2i) -> Rect2i:
	var cell := image.get_region(region)
	for y in cell.get_height():
		for x in cell.get_width():
			if cell.get_pixel(x,y).a < 0.31: cell.set_pixel(x,y,Color(0,0,0,0))
	return cell.get_used_rect()

func _initialize() -> void:
	var checks := 0
	for species: String in ["mosslyn","wyrmling","nocti","starlet"]:
		var views: Dictionary = JSON.parse_string(FileAccess.get_file_as_string("res://data/"+species+"_views.json"))
		var cycles: Dictionary = JSON.parse_string(FileAccess.get_file_as_string("res://data/"+species+("_m4_cycles.json" if species=="mosslyn" else "_authored_cycles.json")))
		var texture: Texture2D = load(views.asset)
		var image := texture.get_image()
		if image.is_compressed(): image.decompress()
		var target: float = float(cycles.rest.bounds[0][3]) / float(cycles.rest.reference_height)
		for i in (8 if species=="mosslyn" else 4):
			var bounds := visible_bounds(image,Rect2i(i%4*320,i/4*320,320,320))
			var ratio: float = bounds.size.y / float(views.reference_height) / target
			assert(ratio>.90 and ratio<1.18,"View changes body height: "+species+" "+str(i)+" "+str(ratio))
			assert(abs(bounds.end.y-300)<=1,"Unplanted view: "+species)
			assert(abs(bounds.size.y-int(views.bounds[i][3]))<=1,"Transparent fringe counted")
			checks+=3
	print("CHARACTER_PROPORTIONS_OK ",checks)
	quit()
