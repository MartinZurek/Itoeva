extends SceneTree

var checks := 0
var failures: Array[String] = []
var max_slip := 0.0
var max_contact_height_error := 0.0

func check(condition: bool, message: String) -> void:
 checks += 1
 if not condition: failures.append(message)

func _initialize() -> void:
 call_deferred("_run")

func _run() -> void:
 var document := GLTFDocument.new()
 var state := GLTFState.new()
 var error := document.append_from_file("res://output/moosling-a4.glb", state)
 check(error == OK, "GLB-Import")
 if error != OK:
  push_error("GLB konnte nicht importiert werden")
  quit(1)
  return
 var model := document.generate_scene(state)
 root.add_child(model)
 var skeleton: Skeleton3D = model.find_child("Skeleton3D", true, false)
 var player: AnimationPlayer = model.find_child("AnimationPlayer", true, false)
 check(skeleton != null, "Skelett vorhanden")
 check(player != null, "AnimationPlayer vorhanden")
 if not skeleton or not player:
  print(model.get_tree_string_pretty())
  quit(1)
  return
 check(skeleton.get_bone_count() == 20, "20 Gelenke")
 for name in ["walk", "run"]:
  check(player.has_animation(name), "Clip " + name)
  if not player.has_animation(name): continue
  var animation := player.get_animation(name)
  player.play(name)
  player.advance(0)
  var head_index := skeleton.find_bone("head")
  var head_rest := skeleton.get_bone_global_rest(head_index)
  var previous_contacts: Dictionary = {}
  var stride := 1.05 if name == "run" else .48
  var duty := .32 if name == "run" else .64
  for frame in 49:
   var cycle := float(frame) / 48.0
   player.seek(animation.length * cycle, true)
   skeleton.force_update_all_bone_transforms()
   for bone in skeleton.get_bone_count():
    var pose := skeleton.get_bone_global_pose(bone)
    check(pose.origin.is_finite(), "Endliche Gelenkposition")
    check(absf(pose.basis.determinant() - 1.0) < .001, "Keine Groessen-/Volumenaenderung")
   var head := skeleton.get_bone_global_pose(head_index)
   check(head.basis.get_scale().is_equal_approx(head_rest.basis.get_scale()), "Kopf bleibt formstabil")
   for kind in (["hind", "fore"] if name == "run" else ["hind"]):
    for side in [-1, 1]:
     var key: String = kind + str(side)
     var offset := (0.0 if side == 1 else .5) if name == "walk" else (0.0 if side == 1 else .09) + (0.0 if kind == "fore" else .52)
     var phase := fposmod(cycle + offset, 1.0)
     var paw := skeleton.get_bone_global_pose(skeleton.find_bone(key + "_paw_2")).origin
     check(paw.y >= .119, "Pfotenanker oberhalb Boden")
     if phase < duty:
      var world_x := paw.x + stride * cycle
      var height_error := absf(paw.y - .12)
      max_contact_height_error = maxf(max_contact_height_error, height_error)
      check(height_error < .001, "%s/%s/%d Standhoehenfehler %.6f" % [name,key,frame,height_error])
      if previous_contacts.has(key) and phase > previous_contacts[key].phase:
       var slip: float = absf(world_x - previous_contacts[key].x)
       max_slip = maxf(max_slip,slip)
       check(slip < .001, "%s/%s/%d Standfussversatz %.6f" % [name,key,frame,slip])
      previous_contacts[key] = {"phase":phase,"x":world_x}
     else:
      previous_contacts.erase(key)
  player.stop()
 var report := {"checks":checks,"failures":failures,"engine":Engine.get_version_info().string,"max_stance_slip_model_m":max_slip,"max_contact_height_error_model_m":max_contact_height_error,"scope":"GLB-Import, Animation, Skalierung und Standfuesse im Modellmassstab; keine Android-/Weltabnahme"}
 var file := FileAccess.open("res://output/godot-validation.json", FileAccess.WRITE)
 file.store_string(JSON.stringify(report,"  "));file.close()
 print(JSON.stringify(report))
 model.queue_free()
 quit(0 if failures.is_empty() else 1)
