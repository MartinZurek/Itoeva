extends RefCounted

# Beide Fuesse haben eigene, um einen halben Zyklus versetzte Kontakte.
# Die Standphase laeuft linear gegen die Weltbewegung; nur der freie Fuss hebt ab.
static func sample(cycle: float, half_stride: float, sprint: float, depth: float = 0.0) -> Dictionary:
 var duty := lerpf(.56, .48, sprint)
 var phase := fposmod(cycle, 1.0)
 var travel: float
 var lift := 0.0
 if phase < duty:
  travel = half_stride * (1.0 - 2.0 * phase / duty)
 else:
  var u := (phase - duty) / (1.0 - duty)
  # Hermite-Rueckfuehrung setzt mit derselben Geschwindigkeit ab und auf.
  var tangent := -(1.0 - duty) / duty
  var h := (-2.0*u*u*u + 3.0*u*u) + tangent*(2.0*u*u*u - 3.0*u*u + u)
  travel = half_stride * (2.0*h - 1.0)
  lift = half_stride * .44 * pow(sin(PI*u), 2.0)
 return {"offset": Vector2(travel if depth == 0.0 else 0.0, travel*depth-lift), "contact": phase < duty, "travel": travel, "lift": lift, "duty": duty}

static func stride(half_stride: float, pixel_size: float, sprint: float) -> float:
 return 2.0 * half_stride * pixel_size / lerpf(.56, .48, sprint)
