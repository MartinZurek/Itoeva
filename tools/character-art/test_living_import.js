// Der echte CLI-Pfad muss dieselben v2-Korrekturen wie der Erstimport anwenden.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const os=require('node:os');
const path=require('node:path');
const {execFileSync}=require('node:child_process');
const {PNG}=require('pngjs');
const {bounds,crop}=require('./living_atlases');
const names=['fennec','gloop','puffling','wyrmling','starlet','hootlet'];
const temp=fs.mkdtempSync(path.join(os.tmpdir(),'itoeva-living-import-'));
try {
  const tool=path.join(temp,'tools/character-art'),raw=path.join(temp,'raw');
  const assets=path.join(temp,'app-sim/src/main/assets/creatures');
  for(const p of [path.join(tool,'source'),raw,assets])fs.mkdirSync(p,{recursive:true});
  fs.copyFileSync(path.join(__dirname,'living_atlases.js'),path.join(tool,'living_atlases.js'));
  function fixture(rows,posture=false) {
    const im=new PNG({width:512,height:128*rows});
    for(let i=0;i<rows*4;i++) {
      const w=i===10?70:50,h=posture && i%4>=2?48:80;
      for(let y=0;y<h;y++)for(let x=0;x<w;x++) {
        const p=((Math.floor(i/4)*128+24+y)*512+i%4*128+24+x)*4;
        im.data[p]=200;im.data[p+1]=150;im.data[p+2]=80;im.data[p+3]=255;
      }
    }
    return PNG.sync.write(im);
  }
  for(const name of names) {
    fs.writeFileSync(path.join(raw,name+'.png'),fixture(8));
    fs.copyFileSync(path.resolve(__dirname,'../../app-sim/src/main/assets/creatures',name+'.png'),path.join(assets,name+'.png'));
  }
  const posture=path.join(temp,'posture.png');fs.writeFileSync(posture,fixture(3,true));
  execFileSync(process.execPath,[path.join(tool,'living_atlases.js'),'--import',raw,'--posture',posture],
    {env:{...process.env,NODE_PATH:[...require.resolve.paths('sharp'),...require.resolve.paths('pngjs')].join(path.delimiter)},stdio:'pipe'});
  const frame=(name,i)=>crop(PNG.sync.read(fs.readFileSync(path.join(tool,'source',name+'-living-master.png'))),i%4*256,Math.floor(i/4)*256,256,256);
  assert.ok(bounds(frame('wyrmling',9)).width>bounds(frame('wyrmling',10)).width,'CLI vertauscht Wyrmling-Wendebilder nicht');
  for(const name of ['fennec','wyrmling'])assert.equal(bounds(frame(name,31)).height,152,name+': neue Sitzzeichnung wurde ersetzt');
  assert.ok(bounds(frame('puffling',31)).height<120,'Puffling-Sitzkorrektur fehlt');
  console.log('OK: CLI sortiert Wyrmling und korrigiert ausschliesslich Pufflings Sitzbild');
} finally {fs.rmSync(temp,{recursive:true,force:true});}
