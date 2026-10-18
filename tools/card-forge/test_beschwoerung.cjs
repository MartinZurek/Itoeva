// node --test tools/card-forge/test_beschwoerung.cjs
const {test} = require('node:test');
const assert = require('node:assert/strict');
const {plan,zustand,CodeDarsteller,VideoDarsteller,Spieler} = require('./beschwoerung.js');
const daten = {volk:'Leuchtfeuer',form:'Jungform',hex:'#e0683a',glanz:false,
  bild:'wesen/leuchtfeuer-licht-1.svg',seltenheit:'Ungewöhnlich'};
const normal = plan(daten);

test('drei Formstufen, Glanz und längere mythische Sequenz ohne Stats', () => {
  const dauern=[];
  for (const [stufe,form] of ['Jungform','Reife Form','Urform'].entries()) {
    const p=plan({...daten,form}); dauern.push(p.dauer);
    const m=plan({...daten,form,seltenheit:'Mythisch',glanz:true});
    assert.equal(p.stufe,stufe); assert.ok(m.dauer>p.dauer);
    assert.ok(m.phasen.length>p.phasen.length); assert.notEqual(m.schluessel,p.schluessel);
    assert.equal(Object.isFrozen(m.phasen[0]),true);
    assert.equal(m.angriff,undefined); assert.equal(m.kosten,undefined);
  }
  assert.deepEqual(dauern,[3200,4000,4800]);
  assert.equal(plan({...daten,seltenheit:'Mythisch'}).dauer,9600);
});

test('ungültige Formen, Farben, Völker und Bildpfade werden zurückgewiesen', () => {
  for (const defekt of [{form:'Ei'},{volk:'Erzvolk'},{hex:'red'},{glanz:1},
    {bild:'../private/x.svg'},{bild:'https://example.com/x.svg'}]) {
    assert.throws(()=>plan({...daten,...defekt}),/gültige/);
  }
});

test('Zeitleiste ist endlich, begrenzt und vor dem Erscheinen ohne Wesen', () => {
  for (const form of ['Jungform','Reife Form','Urform']) {
    for (const seltenheit of ['Selten','Mythisch']) {
      const p=plan({...daten,form,seltenheit});
      assert.equal(zustand(p,-100).t,0); assert.equal(zustand(p,p.dauer+1).fertig,true);
      assert.equal(zustand(p,p.dauer*.6).wesen,0);
      assert.equal(zustand(p,p.dauer).wesen,1);
      assert.equal(zustand(p,p.dauer).saeule,0);
      for (const phase of p.phasen) assert.equal(zustand(p,phase.ab*p.dauer).phase,phase.name);
      for (let ms=0;ms<=p.dauer;ms+=20) {
        for (const [key,value] of Object.entries(zustand(p,ms))) {
          if (typeof value==='number') assert.ok(Number.isFinite(value),key);
        }
      }
    }
  }
  assert.throws(()=>zustand(normal,NaN));
});

function code(bildLaden=()=>Promise.resolve({})) {
  const frames=new Map(), zeichnungen=[]; let id=0;
  const c=new CodeDarsteller({hidden:true},{bildLaden,jetzt:()=>0,
    anfordern:fn=>{frames.set(++id,fn);return id;},abbrechen:id=>frames.delete(id)});
  c.zeichne=(p,z,img,reduziert)=>zeichnungen.push({z,reduziert});
  return {c,frames,zeichnungen,tick:ms=>{const eintrag=frames.entries().next().value;
    assert.ok(eintrag,'geplanter Frame'); frames.delete(eintrag[0]);eintrag[1](ms);}};
}
const laden=()=>new Promise(resolve=>setImmediate(resolve));

test('Code läuft einmal bis zum Ende und hinterlässt keine Frames', async () => {
  const {c,frames,zeichnungen,tick}=code(), phasen=[];
  const fertig=c.spiele(normal,{phase:p=>phasen.push(p)}); await laden();
  tick(0); tick(1800); tick(normal.dauer); await fertig;
  assert.equal(zeichnungen.at(-1).z.wesen,1); assert.equal(frames.size,0);
  assert.deepEqual(phasen,['Funke','Leuchtfeuer','Nachglühen']);
});

test('Bewegungsarm zeigt sofort ein Standbild ohne Animationsframe', async () => {
  const {c,frames,zeichnungen}=code();
  await c.spiele(normal,{reduziert:true});
  assert.equal(frames.size,0); assert.equal(zeichnungen.length,1);
  assert.equal(zeichnungen[0].reduziert,true);
});

test('Abbruch vor/nach Laden und während Animation stoppt auch späte Antworten', async () => {
  let liefere; const {c,frames,zeichnungen}=code(()=>new Promise(resolve=>liefere=resolve));
  const controller=new AbortController();
  const fertig=c.spiele(normal,{signal:controller.signal});
  const abgewiesen=assert.rejects(fertig,{name:'AbortError'});
  controller.abort(); await abgewiesen; liefere({}); await laden();
  assert.equal(frames.size,0); assert.equal(zeichnungen.length,0);
  const zweiter=code(), ctrl=new AbortController();
  const lauf=zweiter.c.spiele(normal,{signal:ctrl.signal}); await laden(); zweiter.tick(0);
  const stopp=assert.rejects(lauf,{name:'AbortError'});ctrl.abort();await stopp;
  assert.equal(zweiter.frames.size,0);
  await assert.rejects(zweiter.c.spiele(normal,{signal:ctrl.signal}),{name:'AbortError'});
});

test('Bildfehler lehnt ab und plant keine Frames', async () => {
  const {c,frames}=code(()=>Promise.reject(new Error('kein Bild')));
  await assert.rejects(c.spiele(normal,{}),/kein Bild/); assert.equal(frames.size,0);
});

class Film extends EventTarget {
  constructor(){super();this.hidden=true;this.pausen=0;this.ladevorgaenge=0;}
  play(){return Promise.resolve();} pause(){this.pausen++;}
  removeAttribute(name){delete this[name];} load(){this.ladevorgaenge++;}
}
test('Video-Darsteller räumt bei Ende, Fehler und Abbruch auf', async () => {
  for (const ereignis of ['ended','error','abort']) {
    const video=new Film(),controller=new AbortController();
    const lauf=new VideoDarsteller(video,'videos/licht.webm').spiele(normal,{signal:controller.signal});
    assert.equal(video.hidden,false);assert.equal(video.muted,true);assert.equal(video.playsInline,true);
    const abschluss=ereignis==='ended'?lauf:assert.rejects(lauf);
    if(ereignis==='abort')controller.abort();else video.dispatchEvent(new Event(ereignis));
    await abschluss;assert.equal(video.hidden,true);assert.equal(video.src,undefined);
    assert.equal(video.pausen,1);assert.equal(video.ladevorgaenge,1);
    video.dispatchEvent(new Event('ended'));assert.equal(video.pausen,1);
  }
  assert.throws(()=>new VideoDarsteller(new Film(),'https://example.com/video.mp4'),/Lokales/);
});

test('Abgelehnte Videowiedergabe wird bereinigt', async () => {
  for (const synchron of [false,true]) {
    const video=new Film();video.play=()=>{if(synchron)throw new Error('Codec fehlt');return Promise.reject(new Error('Codec fehlt'));};
    await assert.rejects(new VideoDarsteller(video,'videos/licht.mp4').spiele(normal,{}),/Codec/);
    assert.equal(video.hidden,true);assert.equal(video.src,undefined);
  }
});

test('Lange Videos dürfen weiterlaufen, Stillstand löst begrenzten Fallback aus', async () => {
  const video=new Film(),timer=new Map();let id=0;
  const darsteller=new VideoDarsteller(video,'videos/lang.webm',{
    warten:(fn,ms)=>{assert.equal(ms,5000);timer.set(++id,fn);return id;},
    loeschen:id=>timer.delete(id),
  });
  const lauf=darsteller.spiele(normal,{}), abschluss=assert.rejects(lauf,/Video nicht verfügbar/);
  video.dispatchEvent(new Event('playing'));
  for (const t of [1,5,15,30,90]) {
    video.currentTime=t;video.dispatchEvent(new Event('timeupdate'));
    assert.equal(timer.size,1);assert.equal(video.hidden,false);
  }
  const vorher=id;video.dispatchEvent(new Event('timeupdate'));assert.equal(id,vorher);
  [...timer.values()][0]();await abschluss;
  assert.equal(timer.size,0);assert.equal(video.hidden,true);
});

test('Austausch, Fallback und bewegungsarme Auswahl verwenden denselben Plan', async () => {
  const aufrufe=[];
  const c={spiele:async(p,o)=>aufrufe.push(['code',p,o.reduziert])};
  const v={spiele:async p=>aufrufe.push(['video',p])};
  const s=new Spieler(c,()=>v);
  await s.spiele(normal);assert.deepEqual(aufrufe.map(a=>a[0]),['video','code']);
  assert.equal(aufrufe[1][2],true);assert.equal(aufrufe[0][1],normal);
  aufrufe.length=0;await s.spiele(normal,{reduziert:true});
  assert.deepEqual(aufrufe.map(a=>a[0]),['code']);
  aufrufe.length=0;const fallback=new Spieler(c,()=>({spiele:async()=>{throw new Error('Codec');}}));
  await fallback.spiele(normal);assert.deepEqual(aufrufe.map(a=>a[0]),['code']);
});

test('Erneuter Start bricht den vorherigen Lauf ab; alter Abschluss löscht keinen neuen', async () => {
  const laeufe=[];
  const s=new Spieler({spiele:(p,{signal})=>new Promise((resolve,reject)=>{
    laeufe.push({signal,resolve});signal.addEventListener('abort',()=>reject(new DOMException('Stop','AbortError')));
  })});
  const alt=s.spiele(normal);const abgebrochen=assert.rejects(alt,{name:'AbortError'});
  const neu=s.spiele(normal);await abgebrochen;
  assert.equal(laeufe[0].signal.aborted,true);assert.equal(s.aktiv.signal,laeufe[1].signal);
  laeufe[1].resolve();await neu;assert.equal(s.aktiv,null);
});

test('Video-Abbruch wechselt niemals in eine neue Code-Animation', async () => {
  let codeLaeufe=0;
  const s=new Spieler({spiele:async()=>{codeLaeufe++;}},()=>({spiele:(p,{signal})=>new Promise((resolve,reject)=>{
    signal.addEventListener('abort',()=>reject(new DOMException('Stop','AbortError')));
  })}));
  const lauf=s.spiele(normal);const fertig=assert.rejects(lauf,{name:'AbortError'});
  s.stop();await fertig;assert.equal(codeLaeufe,0);
});
