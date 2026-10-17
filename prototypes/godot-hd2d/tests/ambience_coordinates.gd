# Coordinate, mask and time regression; supports --headless.
extends SceneTree
const Ambience=preload("res://scripts/painted_ambience.gd")
const Catalog=preload("res://scripts/world_catalog.gd")
var checks=0
var failures=0
func _initialize(): call_deferred("run")
func check(ok,label):
 checks+=1
 if not ok:
  failures+=1
  push_error(label)
func run():
 var data=Ambience.data()
 for key in data.assets:
  var entry=data.assets[key]
  var path="res://assets/"+("world/seams/" if key in ["0.png","1.png","2.png","3.png","4.png","5.png"] else "world/" if entry.size[0]==2172 else "interiors/")+key
  path=str(entry.get("asset",path))
  var texture=load(path)
  check(texture!=null,"Originaltextur vorhanden "+key)
  if texture==null:continue
  var mat=Ambience.create_material(path,texture)
  check(mat.get_shader_parameter("light_count")==entry.get("lights",[]).size(),"Alle Lampen "+key)
  for light in entry.get("lights",[]):
   check(light.at[0]>=0 and light.at[0]<entry.size[0] and light.at[1]>=0 and light.at[1]<entry.size[1] and light.radius[0]>0 and light.radius[1]>0,"Licht in Quellpixeln "+key)
  var mask=mat.get_shader_parameter("water_mask").get_image()
  for point in entry.get("audit_water",[]):
   check(mask.get_pixel(int(point[0]/entry.size[0]*mask.get_width()),int(point[1]/entry.size[1]*mask.get_height())).r>.9,"Handpruefung Wasser "+key+str(point))
  for point in entry.get("audit_dry",[]):
   check(mask.get_pixel(int(point[0]/entry.size[0]*mask.get_width()),int(point[1]/entry.size[1]*mask.get_height())).r<.1,"Handpruefung trockener Steg/Pfad "+key+str(point))
  if key in ["coast.png","coast-path.png"]:
   var region=Catalog.region_for("POND" if key=="coast.png" else "COAST_PATH")
   var overlap=0
   for y in mask.get_height():
    for x in mask.get_width():
     if mask.get_pixel(x,y).r>.5:
      var u=(x+.5)/mask.get_width()
      var v=(y+.5)/mask.get_height()*640
      var band=Catalog.band(region,u,true)
      if v>=band.x and v<=band.y: overlap+=1
   check(overlap==0,"Kein animiertes Wasser auf begehbarem Bildpfad "+key+" count="+str(overlap))
  Ambience.set_time(mat,12,false)
  var noon=mat.get_shader_parameter("lamp_gain")
  check(mat.get_shader_parameter("scene_tint")==Color.WHITE,"Tag ohne Palettewechsel "+key)
  Ambience.set_time(mat,23,false)
  check(mat.get_shader_parameter("lamp_gain")>noon,"Leuchten staerker in Nacht "+key)
  Ambience.set_time(mat,7.0,false)
  var dawn=mat.get_shader_parameter("scene_tint")
  Ambience.set_time(mat,19.5,false)
  var dusk=mat.get_shader_parameter("scene_tint")
  check(dawn.r>dawn.b and dusk.r>dusk.b and dawn!=dusk,"Morgen und Abend eigenstaendig warm "+key)
  Ambience.set_time(mat,NAN,true)
  check(mat.get_shader_parameter("scene_tint")==Color.WHITE,"NaN Stunde sichere Tagesansicht "+key)
 print("Ambience coordinates/masks: ",checks," Checks, ",failures," Fehler")
 quit(1 if failures else 0)
