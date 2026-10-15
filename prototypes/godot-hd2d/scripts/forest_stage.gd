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
var lantern_clock:=0.0
var lantern_wick: ShaderMaterial

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
		local_light_sources.append({"position":anchor+Vector3.UP*float(flame[2])*3.0,
			"radius":Vector2(2.64,2.4),"strength":.55,"fire":true,"forest":true,"gain":1.0,
			"seed":float(region.origin)+i*1.9,"arena":region.id=="SHADOW_CLEARING"})
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
	for source in local_light_sources:
		if source.arena: source["gain"] = arena_lights
	for entry in torch_materials:
		if entry.arena: entry.material.set_shader_parameter("gain",0.0)

func set_demon(reveal: float, hit: float = 0.0) -> void:
	demon.visible=reveal>0.0
	demon_material.set_shader_parameter("reveal",clampf(reveal,0,1))
	demon_material.set_shader_parameter("hit",clampf(hit,0,1))

func darkness_at(point: Vector3) -> float:
	return maxf(float(Forest.region_at(point.x).darkness),super.darkness_at(point))

func update_character_light(actor: Node3D, energy: float) -> void:
	if actor.player_controlled:update_player(actor.global_position)

func update_player(point: Vector3) -> void:
	var actor=get_parent().get("actor")
	var star: bool=is_instance_valid(actor) and actor.species=="starlet"
	var center:=point+Vector3(.54,.73,.06)
	var energy:=darkness_at(point) if carried_light else 0.0
	if is_instance_valid(actor):
		var height: float=actor.frame_bounds[actor.sprite.frame].size.y*actor.sprite.pixel_size
		var camera:=get_viewport().get_camera_3d()
		var right:=camera.global_basis.x if camera else Vector3.RIGHT
		var up:=camera.global_basis.y if camera else Vector3.UP
		var side: float=-1.0 if actor.sprite.flip_h else 1.0
		var moving: float=clampf(actor.actual_speed/2.0,0,1)
		center=point+actor.sprite.position+up*height*.45+right*height*.29*side
		lantern.rotation.z=sin(lantern_clock*3.0)*.055*moving
		center+=up*sin(actor.gait*TAU)*.018*moving
		if star:
			center=actor.light_center()
			energy=actor.glow_energy
	lantern.position=center
	lantern.visible=not star and carried_light and energy>.18
	lantern_wick.set_shader_parameter("gain",.85+.15*sin(lantern_clock*2.3))
	for source in local_light_sources:
		if source.get("lantern",false):
			source["position"]=center
			source["gain"]=energy if star or lantern.visible else 0.0
	for material in forest_materials.values():
		material.set_shader_parameter("player",point)
		material.set_shader_parameter("carried_light",star or carried_light)
		material.set_shader_parameter("carried_position",center)
		material.set_shader_parameter("carried_gain",energy if star or lantern.visible else 0.0)
		material.set_shader_parameter("carried_color",Color("ffe5a0") if star else Color("ffbd72"))

func _process(delta: float) -> void:
	lantern_clock+=delta

func set_time(hour: float) -> void:
	light_hour = hour
	var daylight := smoothstep(5.8,8.0,hour)*(1.0-smoothstep(18.0,21.0,hour))
	for material in forest_materials.values(): material.set_shader_parameter("time_tint",Color(.38,.46,.62).lerp(Color.WHITE,daylight))

func _build_lantern() -> void:
	lantern=Node3D.new()
	lantern.name="Traglaterne"
	lantern.scale=Vector3.ONE*1.3
	add_child(lantern)
	local_light_sources.append({"position":Vector3.ZERO,"radius":Vector2(2.9,2.2),
		"strength":.65,"fire":false,"forest":true,"gain":0.0,"seed":0.0,"arena":false,"lantern":true})
	# Kleine Messinglaterne: durchscheinendes Glas, separater Docht und offener Buegel.
	for entry in [[Vector3.ZERO,Vector3(.15,.19,.12),Color("ffcb79")],
		[Vector3(0,.12,0),Vector3(.19,.035,.15),Color("70513a")],
		[Vector3(0,-.12,0),Vector3(.19,.035,.15),Color("70513a")]]:
		var mesh := MeshInstance3D.new()
		var box := BoxMesh.new()
		box.size = entry[1]
		mesh.mesh = box
		mesh.position = entry[0]
		var material := StandardMaterial3D.new()
		material.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
		material.albedo_color = entry[2]
		if entry[0] == Vector3.ZERO:
			material.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
			material.albedo_color.a = .34
		mesh.material_override = material
		lantern.add_child(mesh)
	for x in [-.075,.075]:
		for z in [-.06,.06]:
			var post := MeshInstance3D.new()
			var rod := CylinderMesh.new()
			rod.top_radius = .008; rod.bottom_radius = .008; rod.height = .22
			post.mesh = rod; post.position = Vector3(x,0,z)
			var brass := StandardMaterial3D.new()
			brass.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
			brass.albedo_color = Color("70513a")
			post.material_override = brass; lantern.add_child(post)
	var handle := MeshInstance3D.new()
	var ring := TorusMesh.new()
	ring.inner_radius = .048; ring.outer_radius = .060
	handle.mesh = ring; handle.rotation_degrees.x = 90; handle.position.y = .18
	handle.material_override = lantern.get_child(1).material_override
	lantern.add_child(handle)
	var wick := MeshInstance3D.new()
	var card := QuadMesh.new()
	card.size = Vector2(.08,.15)
	wick.mesh = card; wick.rotation_degrees.x = -35
	var glow := ShaderMaterial.new()
	glow.shader = load("res://shaders/living_flame.gdshader")
	glow.set_shader_parameter("flame_atlas",load("res://assets/combat/flame-atlas.png"))
	glow.render_priority = 5
	lantern_wick=glow
	wick.material_override = glow; lantern.add_child(wick)
	var halo:=MeshInstance3D.new()
	var halo_quad:=QuadMesh.new();halo_quad.size=Vector2(.72,.72);halo.mesh=halo_quad
	var halo_material:=ShaderMaterial.new()
	halo_material.shader=load("res://shaders/star_aura.gdshader")
	halo_material.set_shader_parameter("energy",1.0)
	halo_material.render_priority=4;halo.material_override=halo_material
	halo.cast_shadow=GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	lantern.add_child(halo)

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
