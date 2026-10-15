shader_type spatial;
render_mode unshaded, cull_disabled, depth_test_disabled, depth_draw_never;
uniform float energy=0.0;
void vertex() {
 vec3 scale=vec3(length(MODEL_MATRIX[0].xyz),length(MODEL_MATRIX[1].xyz),length(MODEL_MATRIX[2].xyz));
 mat4 facing=mat4(INV_VIEW_MATRIX[0]*scale.x,INV_VIEW_MATRIX[1]*scale.y,INV_VIEW_MATRIX[2]*scale.z,MODEL_MATRIX[3]);
 MODELVIEW_MATRIX=VIEW_MATRIX*facing;
}
void fragment() {
 vec2 p=(UV-.5)*2.0;
 float radius=dot(p,p);
 float halo=exp(-radius*5.5)*(1.0-smoothstep(.4,1.0,radius));
 ALBEDO=vec3(1.0,.78,.32);
 ALPHA=halo*energy*.40*(.97+.03*sin(TIME*1.7));
}
