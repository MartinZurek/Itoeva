// Flamme, Kulisse und Figur teilen denselben Rhythmus; keine globale Blitzhelligkeit.
float firelight_flicker(float clock, float seed) {
 return 0.90 + 0.055*sin(clock*4.7+seed) + 0.03*sin(clock*7.9+seed*1.31) + 0.015*sin(clock*13.1+seed*2.17);
}
float firelight_pool(vec2 distance) {
 float r2=dot(distance,distance);
 return exp(-r2*3.6)*(1.0-smoothstep(0.72,1.0,sqrt(r2)));
}
float firelight_embers(vec2 uv, float clock, float seed) {
 float sparks=0.0;
 for(int i=0;i<4;i++) {
  float id=float(i);
  float rise=fract(clock*(0.31+id*0.037)+id*0.263+seed*0.13);
  float x=0.5+sin(seed+id*4.1)*0.18+sin(rise*7.0+id)*0.09*rise;
  vec2 delta=(uv-vec2(x,0.83-rise*0.76))/vec2(0.009,0.022);
  sparks+=exp(-dot(delta,delta)*2.0)*smoothstep(0.0,0.15,rise)*(1.0-smoothstep(0.60,0.98,rise));
 }
 return min(sparks,1.0);
}
