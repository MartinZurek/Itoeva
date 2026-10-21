extends RefCounted

# Ein zusammenhaengendes Hautnetz traegt die Zeichnung. Knie, Huefte und
# Oberkoerper bewegen echte Geometrie; kein Bildfluss und keine ausgeschnittenen
# Fussaufkleber. Die gemalten Gesichtszuege bleiben dabei dieselben.
const Steps = preload("res://scripts/step_gait.gd")
static var meshes: Dictionary = {}

static func surface(bounds: Rect2i) -> ArrayMesh:
 var key := str(bounds)
 if meshes.has(key): return meshes[key]
 var vertices := PackedVector3Array()
 var colors := PackedColorArray()
 var indices := PackedInt32Array()
 var columns := ceili(bounds.size.x / 3.0) + 1
 var rows := ceili(bounds.size.y / 3.0) + 1
 for y in rows:
  for x in columns:
   var point := Vector2(bounds.position) + Vector2(bounds.size) * Vector2(float(x)/(columns-1),float(y)/(rows-1))
   vertices.append(Vector3(point.x-128,128-point.y,0))
   # Kennzeichnet nur dieses Netz, nicht die von Sprite3D gelieferte Quadflaeche.
   colors.append(Color(0,1,1,1))
 for y in rows-1:
  for x in columns-1:
   var a := y*columns+x
   indices.append_array(PackedInt32Array([a,a+columns,a+1,a+1,a+columns,a+columns+1]))
 var arrays := []
 arrays.resize(Mesh.ARRAY_MAX)
 arrays[Mesh.ARRAY_VERTEX]=vertices
 arrays[Mesh.ARRAY_COLOR]=colors
 arrays[Mesh.ARRAY_INDEX]=indices
 var mesh := ArrayMesh.new()
 mesh.add_surface_from_arrays(Mesh.PRIMITIVE_TRIANGLES,arrays)
 meshes[key]=mesh
 return mesh

static func point(value: Array) -> Vector2:
 return Vector2(value[0],value[1])

static func rotate_about(p: Vector2,pivot: Vector2,angle: float) -> Vector2:
 return pivot+(p-pivot).rotated(angle)

static func torso(p: Vector2,pose: Dictionary) -> Vector2:
 return rotate_about(p,pose.root,pose.angle)+pose.offset

static func solve_knee(hip: Vector2,target: Vector2,upper: float,lower: float,bend: float) -> Vector2:
 var axis := target-hip
 var distance := clampf(axis.length(),absf(upper-lower)+.01,upper+lower-.01)
 axis=axis.normalized()
 var along := (upper*upper-lower*lower+distance*distance)/(2*distance)
 var across := sqrt(maxf(0,upper*upper-along*along))
 return hip+axis*along+Vector2(-axis.y,axis.x)*across*bend

static func sample(species: String,row: int,cycle: float,sprint: float,rig: Dictionary,profile: Dictionary,flight: bool=false) -> Dictionary:
 var phase := cycle*TAU
 var depth := row!=0
 var root := point(profile.root)
 var two_steps := cos(phase*2)
 var bob := -lerpf(4.5,7.5,sprint)*(1.0+two_steps)
 var shift := (1.6 if depth else .8)*sin(phase)
 var angle := (.025 if depth else -.035-.045*sprint)+(.030 if depth else .018)*sin(phase)
 if species=="wyrmling":
  bob=-lerpf(3.5,6.0,sprint)*(1.0+two_steps)
  angle=(-.025-.045*sprint if not depth else 0.0)+.025*sin(phase)
 var lift := 0.0
 var squash := 0.0
 if species=="starlet":
  # Ein kurzer Kontakt geht in einen langen weichen Schwebebogen ueber.
  # Beide unteren Strahlen bleiben Strahlen und wechseln nie als Beine ab.
  var beat := fposmod(cycle,1.0)
  if beat<.18:
   squash=.045*pow(sin(PI*beat/.18),2)
  else:
   lift=lerpf(19.0,32.0,sprint)*pow(sin(PI*(beat-.18)/.82),1.3)
  bob=0
  angle=lerpf(.065,.10,sprint)*sin(phase-.35)
  shift=1.5*sin(phase)
 if flight:
  bob=4.2*sin(phase-.5)
  angle=(-.04 if not depth else 0.0)+.035*sin(phase-.7)
 var pose := {"root":root,"offset":Vector2(shift,-bob),"angle":angle,"lift":lift,"squash":squash,
  "head_angle":-angle*.72+.012*sin(phase*2-.65),
  "ear_angle":.035*sin(phase*2-1.0)*(1.0+sprint*.5),
  "secondary":lerpf(.075,.13,sprint)*sin(phase-1.0),
  "forearms":Vector2(1,-1)*lerpf(.09,.17,sprint)*sin(phase-.35),
  "arms":Vector2(1,-1)*lerpf(.13,.25,sprint)*sin(phase+.2),
  "upper":PackedVector4Array(),"lower":PackedVector4Array(),"toes":PackedVector4Array(),
  "knees":PackedVector2Array(),"feet":PackedVector2Array(),"hips":PackedVector2Array()}
 if flight:
  pose.arms=Vector2(1,-1)*(.28*sin(phase)+.06*sin(phase*2-.6))
 if species=="starlet":
  var lowest := -INF
  for value in rig.feet:
   var tip := point(value)-root
   tip*=Vector2(1.0+squash*.35,1.0-squash)
   lowest=maxf(lowest,(root+tip.rotated(angle)+pose.offset).y)
  pose.lift+=maxf(0,lowest-float(rig.ground))
  return pose
 for leg in rig.feet.size():
  var hip := point(rig.hips[leg])
  var foot := point(rig.feet[leg])
  var knee := hip.lerp(foot,.55)+Vector2(-5.0 if not depth else (3.0 if leg%2==0 else -3.0),0)
  var stagger: float= lerpf([0.0,.5,.75,.25][leg],[0.0,.5,.5,0.0][leg],sprint) if rig.feet.size()==4 else leg*.5
  var half_stride := lerpf(18.0,26.0,sprint) if not depth else lerpf(10.0,16.0,sprint)
  if species=="nocti":half_stride*=.64
  var step := Steps.sample(cycle+stagger,half_stride,sprint,0.0 if row==0 else .38 if row==1 else -.38)
  var hip_target := torso(hip,pose)
  var foot_target: Vector2= foot+step.offset
  # Die Referenzpfoten liegen in perspektivischer Tiefe, nicht alle auf y=256.
  # Die Auftrittshoehe jeder Pfote bleibt deshalb ihre eigene Referenzhoehe.
  var upper := hip.distance_to(knee)
  var lower := knee.distance_to(foot)
  var bend := -1.0 if (foot-hip).cross(knee-hip)<0 else 1.0
  var knee_target := solve_knee(hip_target,foot_target,upper,lower,bend)
  pose.hips.append(hip);pose.knees.append(knee);pose.feet.append(foot)
  pose.upper.append(Vector4((knee_target-hip_target).angle()-(knee-hip).angle(),hip_target.x,hip_target.y,hip_target.distance_to(knee_target)/upper))
  pose.lower.append(Vector4((foot_target-knee_target).angle()-(foot-knee).angle(),knee_target.x,knee_target.y,knee_target.distance_to(foot_target)/lower))
  pose.toes.append(Vector4(0,foot_target.x,foot_target.y,1))
 return pose
