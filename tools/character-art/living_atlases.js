#!/usr/bin/env node
// ImageGen zeichnet die Posen; dieser Import registriert sie im festen Spielraster.
// Aufruf: node living_atlases.js --import RAW_DIRECTORY, danach ohne Argument regenerieren.
const fs = require('fs');
const path = require('path');
const sharp = require('sharp');
const {PNG} = require('pngjs');
const HERE = __dirname;
const NAMES = ['fennec','gloop','puffling','wyrmling','starlet','hootlet'];
const SOURCE = path.join(HERE,'source');
const ASSETS = path.resolve(HERE,'../../app-sim/src/main/assets/creatures');
const FRAME=128, COUNT=32, GROUND=125;

function figures(file,count=COUNT,columns=4) {
  const im=PNG.sync.read(fs.readFileSync(file));
  const {width:w,height:h,data}=im, labels=new Int32Array(w*h), stack=new Int32Array(w*h);
  let id=0; const found=[];
  for(let p=0;p<w*h;p++) {
    if(labels[p] || data[p*4+3]<230) continue;
    ++id; let n=1, area=0, x0=w,y0=h,x1=0,y1=0,sx=0,sy=0;
    stack[0]=p;labels[p]=id;
    while(n) {
      const q=stack[--n],x=q%w,y=Math.floor(q/w);
      area++;sx+=x;sy+=y;x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);
      for(const v of [x>0?q-1:-1,x<w-1?q+1:-1,y>0?q-w:-1,y<h-1?q+w:-1]) {
        if(v>=0 && !labels[v] && data[v*4+3]>=230){labels[v]=id;stack[n++]=v;}
      }
    }
    if(area>1200)found.push({id,area,x0,y0,x1,y1,cx:sx/area,cy:sy/area});
  }
  if(found.length!==count)throw Error(`${file}: ${found.length} statt ${count} Figuren`);
  found.sort((a,b)=>a.cy-b.cy);
  const ordered=[];
  for(let row=0;row<count/columns;row++) {
    const group=found.slice(row*columns,row*columns+columns).sort((a,b)=>a.cx-b.cx);
    if(Math.max(...group.map(a=>a.cy))-Math.min(...group.map(a=>a.cy))>h/(count/columns*1.5))throw Error('Unklare Zeilenordnung');
    ordered.push(...group);
  }
  return ordered.map(a=>{
    if(a.x0<2 || a.y0<2 || a.x1>w-3 || a.y1>h-3)throw Error(`${file}: angeschnittene Figur`);
    const width=a.x1-a.x0+1,height=a.y1-a.y0+1,out=Buffer.alloc(width*height*4);
    for(let y=0;y<height;y++)for(let x=0;x<width;x++) {
      const p=(y+a.y0)*w+x+a.x0,q=(y*width+x)*4;
      if(labels[p]===a.id){data.copy(out,q,p*4,p*4+3);out[q+3]=255;}
    }
    return {data:out,width,height};
  });
}

function bounds(im) {
  let x0=im.width,y0=im.height,x1=-1,y1=-1;
  for(let y=0;y<im.height;y++)for(let x=0;x<im.width;x++)if(im.data[(y*im.width+x)*4+3]){
    x0=Math.min(x0,x);x1=Math.max(x1,x);y0=Math.min(y0,y);y1=Math.max(y1,y);
  }
  if(x1<0)throw Error('Leere Pose');
  return {x0,y0,x1,y1,width:x1-x0+1,height:y1-y0+1};
}

function crop(im,x,y,w,h) {
  const out=Buffer.alloc(w*h*4);
  for(let j=0;j<h;j++)im.data.copy(out,j*w*4,((y+j)*im.width+x)*4,((y+j)*im.width+x+w)*4);
  return {data:out,width:w,height:h};
}

function trim(im) { const b=bounds(im);return crop(im,b.x0,b.y0,b.width,b.height); }

async function place(art,factor,baseline,axis=64,contact=false) {
  const width=Math.max(1,Math.round(art.width*factor)),height=Math.max(1,Math.round(art.height*factor));
  const data=await sharp(art.data,{raw:{width:art.width,height:art.height,channels:4}})
    .resize(width,height,{kernel:'nearest'}).raw().toBuffer();
  let cx=(width-1)/2;
  if(contact) {
    let min=width,max=-1;
    for(let y=Math.max(0,height-3);y<height;y++)for(let x=0;x<width;x++)if(data[(y*width+x)*4+3]){min=Math.min(min,x);max=Math.max(max,x);}
    if(max>=0)cx=(min+max)/2;
  }
  // Breite Fluegel haben Vorrang vor einer idealen Achse; nie am Bildrand abschneiden.
  const dx=Math.max(2,Math.min(126-width,Math.round(axis-cx))),dy=baseline-height+1;
  if(dx<2 || dx+width>126 || dy<2 || baseline>125)throw Error('Pose passt nicht ins Spielraster');
  const out=Buffer.alloc(FRAME*FRAME*4);
  for(let y=0;y<height;y++)data.copy(out,((y+dy)*FRAME+dx)*4,y*width*4,(y+1)*width*4);
  return {data:out,width:FRAME,height:FRAME};
}

function join(frames,columns) {
  const width=columns*FRAME,height=frames.length/columns*FRAME,data=Buffer.alloc(width*height*4);
  frames.forEach((fr,i)=>{
    const x=i%columns*FRAME,y=Math.floor(i/columns)*FRAME;
    for(let j=0;j<FRAME;j++)fr.data.copy(data,((y+j)*width+x)*4,j*FRAME*4,(j+1)*FRAME*4);
  });
  return {data,width,height};
}

async function save(im,file) {
  const colors=new Set();
  for(let p=0;p<im.data.length;p+=4)if(im.data[p+3])colors.add(im.data.readUIntBE(p,3));
  if(colors.size>63) {
    const hist=new Map();
    for(let p=0;p<im.data.length;p+=4)if(im.data[p+3]){
      const r=im.data[p],g=im.data[p+1],b=im.data[p+2],key=(r>>4)*256+(g>>4)*16+(b>>4);
      const v=hist.get(key)||{r:0,g:0,b:0,n:0};v.r+=r;v.g+=g;v.b+=b;v.n++;hist.set(key,v);
    }
    const points=[...hist.values()].map(v=>({r:v.r/v.n,g:v.g/v.n,b:v.b/v.n,n:v.n}));
    function spread(group) {
      const ranges=['r','g','b'].map(c=>({c,d:Math.max(...group.map(v=>v[c]))-Math.min(...group.map(v=>v[c]))}));
      return ranges.sort((a,b)=>b.d-a.d)[0];
    }
    const groups=[points];
    while(groups.length<63) {
      let index=-1,score=-1;
      groups.forEach((g,i)=>{const s=g.length>1?spread(g).d*Math.sqrt(g.reduce((n,v)=>n+v.n,0)):-1;if(s>score){score=s;index=i;}});
      if(index<0)break;
      const group=groups[index],axis=spread(group).c;group.sort((a,b)=>a[axis]-b[axis]);
      const half=group.reduce((n,v)=>n+v.n,0)/2;let total=0,split=1;
      for(let i=0;i<group.length-1;i++){total+=group[i].n;split=i+1;if(total>=half)break;}
      groups.splice(index,1,group.slice(0,split),group.slice(split));
    }
    const palette=groups.map(g=>{const n=g.reduce((n,v)=>n+v.n,0);return ['r','g','b'].map(c=>Math.round(g.reduce((s,v)=>s+v[c]*v.n,0)/n));});
    const mapped=new Map();
    for(let p=0;p<im.data.length;p+=4)if(im.data[p+3]){
      const key=im.data.readUIntBE(p,3);let rgb=mapped.get(key);
      if(!rgb){const r=im.data[p],g=im.data[p+1],b=im.data[p+2];let best=Infinity;
        for(const col of palette){const d=(r-col[0])**2+(g-col[1])**2+(b-col[2])**2;if(d<best){best=d;rgb=col;}}mapped.set(key,rgb);}
      im.data[p]=rgb[0];im.data[p+1]=rgb[1];im.data[p+2]=rgb[2];
    }
  }
  // RGBA wie die bestehende Pipeline, ohne PNG-Palette-Abhaengigkeit auf Android.
  fs.writeFileSync(file,PNG.sync.write(im,{colorType:6,filterType:4,deflateLevel:9}));
}

async function build(rawDirectory,postureFile) {
  const postures=postureFile?figures(postureFile,12):null;
  const manifest={version:1,frame:128,columns:4,rows:8,gutter:26,ground:125,species:{}};
  for(const name of NAMES) {
    const source=path.join(SOURCE,`${name}-living-atlas.png`);
    if(rawDirectory) {
      const art=figures(path.join(rawDirectory,`${name}.png`));
      if(postures && ['fennec','puffling','wyrmling'].includes(name)) {
        const row=['fennec','puffling','wyrmling'].indexOf(name)*4;
        const seated=postures[row+3],factor=art[4].height/postures[row].height;
        const width=Math.round(seated.width*factor),height=Math.round(seated.height*factor);
        art[31]={width,height,data:await sharp(seated.data,{raw:{width:seated.width,height:seated.height,channels:4}})
          .resize(width,height,{kernel:'nearest'}).raw().toBuffer()};
      }
      const factor=74/Math.max(...art.map(a=>Math.max(a.width,a.height)));
      const packed=await Promise.all(art.map(a=>place(a,factor,101)));
      await save(join(packed,4),source);
    }
    const packed=PNG.sync.read(fs.readFileSync(source));
    if(packed.width!==512 || packed.height!==1024)throw Error('Atlas muss exakt 512 x 1024 sein');
    const art=Array.from({length:32},(_,i)=>trim(crop(packed,i%4*128,Math.floor(i/4)*128,128,128)));
    const old=PNG.sync.read(fs.readFileSync(path.join(ASSETS,`${name}.png`)));
    const stand=crop(old,0,0,128,128),oldHeight=bounds(stand).height;
    const factor=Math.min(oldHeight/art[4].height,112/Math.max(...art.map(a=>a.width)),122/Math.max(...art.map(a=>a.height)));
    let supportMin=128,supportMax=-1;
    for(let y=123;y<=125;y++)for(let x=0;x<128;x++)if(stand.data[(y*128+x)*4+3]){supportMin=Math.min(supportMin,x);supportMax=Math.max(supportMax,x);}
    const sideAxis=(supportMin+supportMax)/2;
    const frames=[];
    for(let i=0;i<32;i++) {
      const front=i<4 || (i>=16 && i<24) || i===10 || i===30;
      // Alle Posen teilen den Massstab. Nur Ruhe braucht eine feste Stuetzachse;
      // aktive Schritte duerfen ihre Kontaktflaeche wirklich verlagern.
      frames.push(await place(art[i],factor,GROUND,front?64:sideAxis,i<8));
    }
    // Atmen bleibt auf denselben Pfoten. Die gezeichnete Brust/Fleecebewegung
    // bleibt unveraendert; die untersten zwei Kontaktzeilen werden registriert.
    for(const start of [0,4])for(let i=start+1;i<start+4;i++)
      frames[start].data.copy(frames[i].data,124*128*4,124*128*4);
    await save(join(frames,32),path.join(ASSETS,`${name}-living.png`));
    const nativeHeight=bounds(frames[4]).height;
    manifest.species[name]={oldHeight,nativeHeight,renderScale:Number((oldHeight/nativeHeight).toFixed(6))};
    console.log(name,manifest.species[name]);
  }
  fs.writeFileSync(path.join(SOURCE,'living-atlas-manifest.json'),JSON.stringify(manifest,null,2)+'\n');
}
if(require.main===module)build(process.argv[2]==='--import'?path.resolve(process.argv[3]):null,
  process.argv[4]==='--posture'?path.resolve(process.argv[5]):null).catch(e=>{console.error(e);process.exit(1);});
module.exports={figures,bounds,crop,trim,build};
