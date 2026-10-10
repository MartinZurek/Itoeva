const assert=require('node:assert/strict');
const fs=require('fs');
const path=require('path');
const {PNG}=require('pngjs');
const {bounds,crop,build}=require('./living_atlases');
const names=['fennec','gloop','puffling','wyrmling','starlet','hootlet'];
const assets=path.resolve(__dirname,'../../app-sim/src/main/assets/creatures');
const source=path.join(__dirname,'source');

async function test() {
  const before=new Map();
  const manifest=JSON.parse(fs.readFileSync(path.join(source,'living-atlas-manifest.json')));
  for(const name of names) {
    const atlas=PNG.sync.read(fs.readFileSync(path.join(source,`${name}-living-atlas.png`)));
    assert.equal(atlas.width,512);assert.equal(atlas.height,1024);
    const legacy=PNG.sync.read(fs.readFileSync(path.join(assets,`${name}.png`)));
    const palette=new Set();
    for(let p=0;p<legacy.data.length;p+=4)if(legacy.data[p+3])palette.add(legacy.data.subarray(p,p+3).toString('hex'));
    const colors=new Set();
    for(let y=0;y<1024;y++)for(let x=0;x<512;x++) {
      const p=(y*512+x)*4,a=atlas.data[p+3];
      assert.ok(a===0 || a===255,`${name}: weicher Alpha`);
      if(a){assert.ok(x%128>=26 && x%128<=101 && y%128>=26 && y%128<=101,`${name}: fehlender 20%-Rand`);colors.add(atlas.data.subarray(p,p+3).toString('hex'));}
    }
    assert.ok([...colors].every(c=>palette.has(c)),`${name}: zweite Farbwelt im kleinen Atlas`);
    const master=PNG.sync.read(fs.readFileSync(path.join(source,`${name}-living-master.png`)));
    assert.equal(master.width,1024);assert.equal(master.height,2048);
    for(let y=0;y<2048;y++)for(let x=0;x<1024;x++)if(master.data[(y*1024+x)*4+3]){
      assert.ok(x%256>=52 && x%256<=203 && y%256>=52 && y%256<=203,`${name}: Master-Rand`);
    }
    const file=path.join(assets,`${name}-living.png`),bytes=fs.readFileSync(file);
    before.set(file,bytes);
    const sheet=PNG.sync.read(bytes);assert.equal(sheet.width,4096);assert.equal(sheet.height,128);
    for(let p=0;p<sheet.data.length;p+=4)if(sheet.data[p+3]){
      assert.equal(sheet.data[p+3],255,`${name}: weicher Alpha im Spiel`);
      assert.ok(palette.has(sheet.data.subarray(p,p+3).toString('hex')),`${name}: Spiel-Farbwert fehlt im vorhandenen Bogen`);
    }
    const frames=Array.from({length:32},(_,i)=>crop(sheet,i*128,0,128,128));
    for(let i=0;i<32;i++) {
      const b=bounds(frames[i]);
      assert.ok(b.x0>=2 && b.x1<=125 && b.y0>=2,`${name}/${i}: angeschnitten`);
      assert.equal(b.y1,125,`${name}/${i}: schwebt`);
      assert.ok(bounds(crop(atlas,i%4*128,Math.floor(i/4)*128,128,128)).width>20);
    }
    for(const start of [0,4]) {
      assert.equal(new Set(frames.slice(start,start+4).map(f=>f.data.toString('base64'))).size,4,`${name}: statische Ruhe`);
      for(let i=start+1;i<start+4;i++)assert.ok(frames[start].data.subarray(124*128*4).equals(frames[i].data.subarray(124*128*4)),`${name}: wandernder Kontakt`);
    }
    assert.ok(bounds(frames[31]).height < bounds(frames[4]).height,`${name}: Sitzen wird vergroessert`);
    const m=manifest.species[name];
    assert.ok(m.samplingFactor<=1,`${name}: verlorene Aufloesung wird hochskaliert`);
    assert.ok(Math.abs(bounds(frames[4]).height*m.renderScale-m.oldHeight)<.01,`${name}: Weltgroesse`);
    const frontHeight=bounds(crop(legacy,27*128,0,128,128)).height;
    assert.ok(Math.abs(bounds(frames[0]).height*m.renderScale-frontHeight)/frontHeight<.08,`${name}: Frontgroesse springt beim Gang-/Ruhewechsel`);
    const kotlin=fs.readFileSync(path.resolve(__dirname,'../../app-sim/src/main/java/com/notime/glyphsim/matrix/CreatureSprites.kt'),'utf8');
    const section=kotlin.slice(kotlin.indexOf('object Living {'));
    const match=section.match(new RegExp('AvatarSpecies\\.'+name.toUpperCase()+' -> ([0-9.]+)f'));
    assert.ok(match && Math.abs(Number(match[1])-m.renderScale)<.000001,`${name}: Laufzeitmassstab`);
  }
  await build();
  for(const [file,bytes] of before)assert.ok(bytes.equals(fs.readFileSync(file)),`${file}: nicht reproduzierbar`);
  console.log('OK: 192 vollstaendige Posen, 20%-Raender, Palette, Kontakte, Weltgroesse und Regeneration');
}
test().catch(e=>{console.error(e);process.exit(1);});
