shader_type spatial;
render_mode unshaded, cull_disabled;
#define PAINTED_OPAQUE
#define PAINTED_WATER
#define PAINTED_SUN_GLADE
#include "res://shaders/painted_surface.gdshaderinc"
