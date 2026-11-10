uniform bool rig_active = false;
uniform bool rig_star = false;
uniform float rig_pixel_size;
uniform float rig_ground_offset;
uniform float rig_flip = 1.0;
uniform sampler2D rig_mask : filter_linear, repeat_disable;
uniform vec2 rig_root;
uniform vec2 rig_offset;
uniform float rig_angle;
uniform vec2 rig_neck;
uniform float rig_head_angle;
uniform float rig_ear_angle;
uniform float rig_squash;
uniform float rig_secondary;
uniform vec2 rig_tail;
uniform vec2 rig_cape;
uniform vec2 rig_shoulders[2];
uniform vec2 rig_arms;
uniform vec2 rig_elbows[2];
uniform vec2 rig_forearms;
uniform vec2 rig_hips[4];
uniform vec2 rig_knees[4];
uniform vec2 rig_feet[4];
uniform vec4 rig_upper[4];
uniform vec4 rig_lower[4];
uniform vec4 rig_toes[4];
varying float skin_surface;

vec2 turn(vec2 p,float angle) {
 return vec2(cos(angle)*p.x-sin(angle)*p.y,sin(angle)*p.x+cos(angle)*p.y);
}

vec2 body_point(vec2 p) {
 vec2 local=p-rig_root;
 local.y*=1.0-rig_squash;
 local.x*=1.0+rig_squash*.35;
 return rig_root+turn(local,rig_angle)+rig_offset;
}

vec2 bone_point(vec2 p,vec2 rest,vec4 pose) {
 return pose.yz+turn((p-rest)*pose.w,pose.x);
}

vec2 skin_point(vec2 p) {
 vec4 weights=textureLod(rig_mask,(p+vec2(0.0,float(step_row)*256.0))/vec2(256.0,768.0),0.0);
 vec2 body=body_point(p);
 // Kopf dreht am Hals gegen den Rumpf; Gesicht und Proportionen bleiben erhalten.
 vec2 neck=body_point(rig_neck);
 vec2 head=neck+turn(p-rig_neck,rig_angle+rig_head_angle);
 body=mix(body,head,weights.r);
 float ear=(1.0-smoothstep(rig_neck.y-105.0,rig_neck.y-45.0,p.y))*weights.r;
 body+=(turn(vec2(0.0,p.y-(rig_neck.y-45.0)),rig_ear_angle)-vec2(0.0,p.y-(rig_neck.y-45.0)))*ear;
 vec2 pivot=mix(rig_cape,rig_tail,smoothstep(rig_root.y-12.0,rig_root.y+8.0,p.y));
 float chain=clamp(length(p-pivot)/85.0,0.0,1.0);
 vec2 trailing=body_point(pivot)+turn(p-pivot,rig_angle+rig_secondary*(.3+.7*chain));
 body=mix(body,trailing,weights.g*(1.0-weights.r));
 vec2 arms[2];
 for(int i=0;i<2;i++) {
  vec2 shoulder=rig_shoulders[i];
  vec2 elbow=rig_elbows[i];
  float swing=rig_arms[i];
  vec2 upper=body_point(shoulder)+turn(p-shoulder,rig_angle+swing);
  vec2 joint=body_point(shoulder)+turn(elbow-shoulder,rig_angle+swing);
  vec2 lower=joint+turn(p-elbow,rig_angle+swing+rig_forearms[i]);
  vec2 axis=elbow-shoulder;
  float along=dot(p-shoulder,axis)/max(1.0,dot(axis,axis));
  arms[i]=mix(upper,lower,smoothstep(.65,1.15,along));
 }
 float arm_total=min(1.0,weights.b+weights.a);
 body=body*(1.0-arm_total)+(arms[0]*weights.b+arms[1]*weights.a)/max(1.0,weights.b+weights.a);
 if (rig_star) { return body; }
 vec4 legs=textureLod(step_mask,(p+vec2(0.0,float(step_row)*256.0))/vec2(256.0,768.0),0.0);
 float total=0.0;
 vec2 result=vec2(0.0);
 for(int i=0;i<4;i++) {
  if(i>=step_leg_count) { break; }
  float w=legs[i];
  vec2 upper=bone_point(p,rig_hips[i],rig_upper[i]);
  vec2 lower=bone_point(p,rig_knees[i],rig_lower[i]);
  vec2 toe=bone_point(p,rig_feet[i],rig_toes[i]);
  vec2 limb=mix(upper,lower,smoothstep(rig_knees[i].y-6.0,rig_knees[i].y+6.0,p.y));
  limb=mix(limb,toe,smoothstep(rig_feet[i].y-12.0,rig_feet[i].y-3.0,p.y));
  result+=w*limb;
  total+=w;
 }
 return body*(1.0-min(1.0,total))+result/max(1.0,total);
}
