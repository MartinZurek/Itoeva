shader_type spatial;
render_mode unshaded, cull_disabled, depth_prepass_alpha;
#define PAINTED_WATER
#include "res://shaders/painted_surface.gdshaderinc"
