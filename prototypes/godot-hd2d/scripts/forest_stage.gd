extends "res://scripts/world_stage.gd"

const Forest = preload("res://scripts/forest_catalog.gd")
var forest_materials: Dictionary = {}
var seam_chunks: Array[Node3D] = []
var torch_materials: Array[Dictionary] = []
var arena_lights := 1.0
var carried_light := true
var lantern: Node3D
var bridge_mask: Texture2D
var bench_mask: Texture2D
var demon: MeshInstance3D
var demon_material: ShaderMaterial

func build() -> void:
	place = "FOREST_TRAIL"
	width = float(Forest.data().width)
	for region in Forest.data().segments:
		var chunk := Node3D.new()
		chunk.name = region.id
		add_child(chunk)
		chunks.append(chunk)
		_make_panorama(region, chunk)
		_make_path(region, chunk)
		_add_flames(region, chunk)
	_build_seams()
	_make_end_wall(1.9)
	_make_end_wall(width - 1.9)
	var position := Forest.position_for(0, 0.10)
	doors.append({"from": "FOREST_TRAIL", "to": "FOREST", "at": Vector2(0.10,0.5),
		"arrival": Vector2(Forest.data().entrance.at[0],Forest.data().entrance.at[1]),
		"position": position, "marker_position": position+Vector3.UP})
	_build_demon()
	_build_lantern()
	_build_bridge_mask()
	_build_bench()

func fit_character(_pixel_size: float) -> void:
	pass

func _band(region: Dictionary, u: float, dry: bool = false) -> Vector2:
	var raw := Forest.raw_band(region,u)
	if dry:
		return Forest.limits(float(region.origin)+u*float(region.width))
	return Vector2(raw.x-8.0, 640.0)

func _point(region: Dictionary, uv: Vector2, ground: bool) -> Vector3:
	var x: float = region.origin + uv.x * region.width
	var band := _band(region, uv.x)
	var raw := Forest.raw_band(region,uv.x)
	var shared := Forest.limits(x)
	var shift := (shared.x+shared.y-raw.x-raw.y)*0.5
	var y := uv.y*640.0+shift
	if ground:
		return Vector3(x,0,Catalog.image_z(y))
	var z := Catalog.image_z(band.x+shift)-smoothstep(band.x/640.0,.20,uv.y)*2.2
	return Vector3(x,Catalog.image_y(y,z,640.0),z)

func _make_path(region: Dictionary, parent: Node3D) -> void:
	var floor_vertices := PackedVector3Array()
	var edges := PackedVector3Array()
	var samples: Array[float] = []
	for i in range(221): samples.append(i/220.0)
	for anchor in region.path: samples.append(float(anchor[0]))
	# Die Profilknicke beider Nachbarn werden exakt in die Physik aufgenommen.
	for neighbor in Forest.data().segments:
		for anchor in neighbor.path:
			var u := (float(neighbor.origin)+float(anchor[0])*22.0-float(region.origin))/22.0
			if u>0.0 and u<1.0: samples.append(u)
	samples.sort()
	for i in range(samples.size()-1):
		var x0: float = region.origin+samples[i]*region.width
		var x1: float = region.origin+samples[i+1]*region.width
		if x1<=x0: continue
		var a := Forest.limits(x0)
		var b := Forest.limits(x1)
		var p := Vector3(x0,0,Catalog.image_z(a.x))
		var q := Vector3(x1,0,Catalog.image_z(b.x))
		var r := Vector3(x1,0,Catalog.image_z(b.y))
		var s := Vector3(x0,0,Catalog.image_z(a.y))
		_quad(floor_vertices,p,q,r,s)
		_quad(edges,p,q,q+Vector3.UP*8,p+Vector3.UP*8)
		_quad(edges,r,s,s+Vector3.UP*8,r+Vector3.UP*8)
	_collision("Waldpfad",floor_vertices,parent)
	_collision("WurzelnUndUfer",edges,parent)

func _limits_at(x: float) -> Vector2:
	return Forest.limits(x)

func is_walkable(point: Vector3, radius: float = 0.0, _allow_platforms: bool = false) -> bool:
	for offset in [Vector2.ZERO,Vector2(radius,0),Vector2(-radius,0),Vector2(0,radius),Vector2(0,-radius)]:
		var x: float = point.x+offset.x
		if x<1.9 or x>width-1.9: return false
		var limits := Forest.limits(x)
		var z: float = point.z+offset.y
		if z<Catalog.image_z(limits.x) or z>Catalog.image_z(limits.y): return false
	for item in furniture:
		var box: BoxShape3D=item.body.get_child(0).shape
		if absf(point.x-item.body.position.x)<box.size.x*.5+radius and absf(point.z-item.body.position.z)<box.size.z*.5+radius: return false
	return true

func safe_position(raw: Vector3, radius: float = 0.25) -> Vector3:
	var point := Vector3(clampf(raw.x,2.3,width-2.3),maxf(raw.y,0.03),raw.z)
	var lower := -INF
	var upper := INF
	for x in [point.x-radius,point.x,point.x+radius]:
		var limits := Forest.limits(x)
		lower=maxf(lower,Catalog.image_z(limits.x)+radius+0.04)
		upper=minf(upper,Catalog.image_z(limits.y)-radius-0.04)
	point.z=clampf(point.z,lower,maxf(lower,upper))
	return point

func actor_occlusion(position: Vector3) -> Dictionary:
	var region := Forest.region_at(position.x)
	return {"mask":bridge_mask if region.id=="SHADOW_BROOK" else bench_mask if region.id=="SHADOW_GATE" else null,"origin":region.origin,"span":region.width}

func location_at(x: float) -> String:
	return Forest.region_at(x).id

func update_visible(player_x: float) -> void:
	for i in chunks.size(): chunks[i].visible=absf(float(i)*20.0+11.0-player_x)<37.0
	for i in seam_chunks.size(): seam_chunks[i].visible=absf(float(i+1)*20.0-player_x)<37.0

func _painted_material(asset: String, _transparent: bool = false) -> ShaderMaterial:
	var region: Dictionary
	for entry in Forest.data().segments:
		if entry.asset==asset: region=entry
	var material := ShaderMaterial.new()
	material.shader=load("res://shaders/forest_panorama.gdshader")
	material.set_shader_parameter("painted_texture",textures.get(asset,load(asset)))
	material.set_shader_parameter("flame_atlas",load("res://assets/combat/flame-atlas.png"))
	material.set_shader_parameter("origin",region.origin)
	material.set_shader_parameter("span",region.width)
	material.set_shader_parameter("darkness",region.darkness)
	material.set_shader_parameter("blend_left",float(region.origin)>0)
	material.render_priority = int(float(region.origin)/20.0)-8
	var flame_data := PackedVector4Array()
	for flame in region.flames: flame_data.append(Vector4(flame[0],flame[1],flame[2],0))
	while flame_data.size()<4: flame_data.append(Vector4.ZERO)
	material.set_shader_parameter("flames",flame_data)
	material.set_shader_parameter("flame_count",region.flames.size())
	material.set_shader_parameter("water_mask",_water_mask(region.id=="SHADOW_BROOK"))
	forest_materials[region.id]=material
	return material

func _water_mask(brook: bool) -> Texture2D:
	var image := Image.create(416,235,false,Image.FORMAT_R8)
	if brook:
		for contour in Forest.data().water:
			var polygon := PackedVector2Array()
			for p in contour: polygon.append(Vector2(p[0]*416,p[1]*235))
			for y in range(235):
				for x in range(416):
					if Geometry2D.is_point_in_polygon(Vector2(x+.5,y+.5),polygon): image.set_pixel(x,y,Color.WHITE)
		for contour in Forest.data().get("water_exclude",[]):
			var polygon := PackedVector2Array()
			for p in contour: polygon.append(Vector2(p[0]*416,p[1]*235))
			for y in range(235):
				for x in range(416):
					if Geometry2D.is_point_in_polygon(Vector2(x+.5,y+.5),polygon): image.set_pixel(x,y,Color.BLACK)
	return ImageTexture.create_from_image(image)

func _add_flames(region: Dictionary, parent: Node3D) -> void:
	for i in region.flames.size():
		var flame: Array=region.flames[i]
		var uv := Vector2(flame[0],flame[1])
		var anchor := _point(region,uv,uv.y>=_band(region,uv.x).x/640.0)
		var size := Vector2(.62,float(flame[2])*10.0)
		var surface := SurfaceTool.new()
		surface.begin(Mesh.PRIMITIVE_TRIANGLES)
		# Karte liegt parallel zur festen gemalten Kameraachse.
		for corner in [Vector2(0,0),Vector2(1,1),Vector2(1,0),Vector2(0,0),Vector2(0,1),Vector2(1,1)]:
			surface.set_uv(corner)
			var up: float = (1.0-corner.y)*size.y
			surface.add_vertex(anchor+Vector3((corner.x-.5)*size.x,up*cos(deg_to_rad(35)), -up*sin(deg_to_rad(35)))+Vector3(0,0,.015))
		var material := ShaderMaterial.new()
		material.shader=load("res://shaders/living_flame.gdshader")
		material.set_shader_parameter("seed",float(region.origin)+i*1.9)
		material.set_shader_parameter("flame_atlas",load("res://assets/combat/flame-atlas.png"))
		material.set_shader_parameter("gain",0.0)
		material.render_priority=3
		_mesh("LoderndeFlamme",surface,material,parent)
		torch_materials.append({"material":material,"arena":region.id=="SHADOW_CLEARING"})

func _build_demon() -> void:
	demon_material=ShaderMaterial.new()
	demon_material.shader=load("res://shaders/shadow_demon.gdshader")
	demon_material.set_shader_parameter("demon_texture",load("res://assets/combat/nachtwurz-atlas.png"))
	demon_material.render_priority=20
	var quad := QuadMesh.new()
	quad.size=Vector2(2.5,3.2)
	demon=MeshInstance3D.new()
	demon.name="Schattenwesen"
	demon.mesh=quad
	demon.material_override=demon_material
	demon.rotation_degrees.x=-35.0
	demon.position=safe_position(Vector3(float(Forest.data().encounter.demon_x),0.03,-2.0)) + Vector3(0,1.6*cos(deg_to_rad(35)),-1.6*sin(deg_to_rad(35)))
	demon.visible=false
	add_child(demon)

func set_arena_lights(value: float) -> void:
	arena_lights=clampf(value,0,1)
	forest_materials.SHADOW_CLEARING.set_shader_parameter("torch_gain",arena_lights)
	for entry in torch_materials:
		if entry.arena: entry.material.set_shader_parameter("gain",0.0)

func set_demon(reveal: float, hit: float = 0.0) -> void:
	demon.visible=reveal>0.0
	demon_material.set_shader_parameter("reveal",clampf(reveal,0,1))
	demon_material.set_shader_parameter("hit",clampf(hit,0,1))

func update_player(point: Vector3) -> void:
	lantern.position=point+Vector3(.35,.63,.06)
	lantern.visible=carried_light and float(Forest.region_at(point.x).darkness)>.18
	for material in forest_materials.values():
		material.set_shader_parameter("player",point)
		material.set_shader_parameter("carried_light",carried_light)

func set_time(hour: float) -> void:
	var daylight := smoothstep(5.8,8.0,hour)*(1.0-smoothstep(18.0,21.0,hour))
	for material in forest_materials.values(): material.set_shader_parameter("time_tint",Color(.38,.46,.62).lerp(Color.WHITE,daylight))

func _build_lantern() -> void:
	lantern=Node3D.new()
	lantern.name="Traglaterne"
	add_child(lantern)
	for entry in [[Vector3.ZERO,Vector3(.15,.19,.12),Color("f8ba5c")],[Vector3(0,.12,0),Vector3(.19,.035,.15),Color("3f2e24")],[Vector3(0,-.12,0),Vector3(.19,.035,.15),Color("3f2e24")],[Vector3(0,.19,0),Vector3(.025,.12,.025),Color("514132")]]:
		var mesh:=MeshInstance3D.new()
		var box:=BoxMesh.new()
		box.size=entry[1]
		mesh.mesh=box
		mesh.position=entry[0]
		var material:=StandardMaterial3D.new()
		material.shading_mode=BaseMaterial3D.SHADING_MODE_UNSHADED
		material.albedo_color=entry[2]
		mesh.material_override=material
		lantern.add_child(mesh)

func _build_bridge_mask() -> void:
	# Vorderes Gelaender und seine Pfosten verdecken die Figur nur hinter der Brüstung.
	var image:=Image.create(836,470,false,Image.FORMAT_RG8)
	var contours: Array=[[[.362,.441],[.650,.477],[.650,.490],[.362,.455]],[[.362,.484],[.650,.520],[.650,.531],[.362,.496]],[[.359,.404],[.374,.401],[.374,.546],[.359,.550]],[[.465,.430],[.479,.429],[.479,.545],[.465,.546]],[[.644,.440],[.658,.438],[.658,.570],[.644,.572]]]
	for contour in contours:
		var polygon:=PackedVector2Array()
		for p in contour: polygon.append(Vector2(p[0]*836.0,p[1]*470.0))
		for y in range(185,272):
			for x in range(297,552):
				if Geometry2D.is_point_in_polygon(Vector2(x+.5,y+.5),polygon): image.set_pixel(x,y,Color(.57,1,0,1))
	bridge_mask=ImageTexture.create_from_image(image)

func set_arena_smoke(value: float) -> void:
	for entry in torch_materials:
		if entry.arena: entry.material.set_shader_parameter("smoke_gain",clampf(value,0,1))

func _build_bench() -> void:
	var region: Dictionary=Forest.data().segments[3]
	var ground_z:=Catalog.image_z(.546*640.0)
	var anchor:=Vector3(float(region.origin)+.294*22.0,Catalog.image_y(.497*640.0,ground_z,640.0),ground_z)
	var body:=StaticBody3D.new()
	body.name="Waldruhebank"
	body.position=Vector3(anchor.x,.35,ground_z)
	var collision:=CollisionShape3D.new()
	var box:=BoxShape3D.new()
	box.size=Vector3(2.65,.7,.30)
	collision.shape=box
	body.add_child(collision)
	add_child(body)
	var points:=PackedVector3Array()
	for uv in [Vector2(.232,.437),Vector2(.360,.437),Vector2(.360,.550),Vector2(.232,.550)]: points.append(_point(region,uv,false))
	furniture.append({"id":"forest-rest-bench","seat":{"point":[.294,.497]},"seat_position":anchor,"seat_offsets":{"fennec":32.0,"wyrmling":32.0,"puffling":18.0,"gloop":10.0,"starlet":12.0,"hootlet":10.0},
		"body":body,"world_center":anchor,"visual_points":points,
		"approach_position":safe_position(Vector3(anchor.x,.03,ground_z+.9))})
	var image:=Image.create(836,470,false,Image.FORMAT_RG8)
	var contours: Array=[[[.241,.445],[.337,.445],[.337,.483],[.241,.483]],[[.232,.490],[.360,.490],[.360,.505],[.232,.505]],[[.241,.499],[.251,.499],[.251,.544],[.241,.544]],[[.343,.500],[.355,.500],[.355,.547],[.343,.547]]]
	for contour in contours:
		var polygon:=PackedVector2Array()
		for uv in contour: polygon.append(Vector2(uv[0]*836,uv[1]*470))
		for y in range(205,260):
			for x in range(190,305):
				if Geometry2D.is_point_in_polygon(Vector2(x+.5,y+.5),polygon): image.set_pixel(x,y,Color(.551,1,0,1))
	bench_mask=ImageTexture.create_from_image(image)

func _build_seams() -> void:
	var atlas: Texture2D=load("res://assets/forest/greenwood.png")
	for i in range(6):
		var chunk:=Node3D.new()
		chunk.name="GemalterWalduebergang%s" % i
		add_child(chunk)
		seam_chunks.append(chunk)
		var region: Dictionary={"origin":float(i+1)*20.0-3.0,"width":6.0,"path":Forest.data().segments[1].path}
		var material:=ShaderMaterial.new()
		material.shader=load("res://shaders/forest_panorama.gdshader")
		material.set_shader_parameter("painted_texture",atlas)
		var tint: Array=Forest.data().seam_tints[i]
		material.set_shader_parameter("path_tint",Color(tint[0],tint[1],tint[2]))
		material.set_shader_parameter("seam_piece",true)
		material.set_shader_parameter("water_mask",_water_mask(false))
		var a: float=Forest.data().segments[i].darkness
		var b: float=Forest.data().segments[i+1].darkness
		material.set_shader_parameter("darkness",(a+b)*.5)
		material.render_priority=1
		forest_materials["seam_%s" % i]=material
		for ground in [false,true]:
			var surface:=SurfaceTool.new()
			surface.begin(Mesh.PRIMITIVE_TRIANGLES)
			var rows:=8 if ground else 24
			for row in rows:
				for col in 60:
					for corner in [Vector2(0,0),Vector2(1,1),Vector2(1,0),Vector2(0,0),Vector2(0,1),Vector2(1,1)]:
						var u: float=(col+corner.x)/60.0
						var edge:=_band(region,u).x/640.0
						var v:=lerpf(edge,1.0,(row+corner.y)/rows) if ground else lerpf(0.0,edge,(row+corner.y)/rows)
						var uv:=Vector2(u,v)
						surface.set_uv(uv)
						surface.set_uv2(Forest.raw_band(region,u)/640.0)
						surface.add_vertex(_point(region,uv,ground)+Vector3(0,.006,.006))
			_mesh("Waldnaht",surface,material,chunk)
