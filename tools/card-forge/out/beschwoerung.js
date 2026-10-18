/* Reine Zeitvorlage und austauschbare Darsteller; kein Kampfzustand, keine Vergabe. */
(function (root) {
  'use strict';
  const clamp = x => Math.max(0, Math.min(1, x));
  const weich = x => { x = clamp(x); return x * x * (3 - 2 * x); };
  const stufen = ['Jungform', 'Reife Form', 'Urform'];
  function plan(daten) {
    const stufe = stufen.indexOf(daten.form);
    if (daten.volk !== 'Leuchtfeuer' || stufe < 0 ||
        !/^#[0-9a-f]{6}$/i.test(daten.hex) || typeof daten.glanz !== 'boolean' ||
        !/^wesen\/leuchtfeuer-[a-z]+-[123]\.svg$/.test(daten.bild)) {
      throw new Error('Keine gültige Leuchtfeuer-Beschwörung');
    }
    const mythisch = daten.seltenheit === 'Mythisch';
    const dauer = mythisch ? 9600 : 3200 + stufe * 800;
    const phasen = mythisch
      ? [[0, 'Sternenruf'], [.13, 'Lichtfäden'], [.31, 'Lichtkreis'], [.47, 'Leuchtfeuer'], [.67, 'Erscheinen'], [.86, 'Nachglühen']]
      : [[0, 'Funke'], [.2, 'Lichtkreis'], [.43, 'Leuchtfeuer'], [.63, 'Erscheinen'], [.85, 'Nachglühen']];
    return Object.freeze({volk:daten.volk, form:daten.form, stufe, hex:daten.hex,
      glanz:daten.glanz, bild:daten.bild, mythisch, dauer,
      phasen:Object.freeze(phasen.map(([ab, name]) => Object.freeze({ab, name}))),
      schluessel:`leuchtfeuer/${daten.form}/${mythisch ? 'mythisch' : 'normal'}/${daten.glanz ? 'glanz' : 'matt'}`});
  }
  function zustand(vorlage, zeit) {
    if (!Number.isFinite(zeit)) throw new Error('Ungültige Vorschauzeit');
    const t = clamp(zeit / vorlage.dauer);
    const eintritt = vorlage.mythisch ? .67 : .63;
    const phase = vorlage.phasen.filter(p => p.ab <= t).at(-1).name;
    return Object.freeze({t, phase, fertig:t === 1,
      kreis:weich((t - .12) / .24),
      saeule:weich((t - .32) / .18) * (1 - weich((t - .76) / .2)),
      wesen:weich((t - eintritt) / .16),
      funken:12 + vorlage.stufe * 12 + (vorlage.mythisch ? 24 : 0)});
  }
  function abbruch() { return new DOMException('Vorschau abgebrochen', 'AbortError'); }

  // Darstellervertrag: spiele(plan, {signal, reduziert, phase}) -> Promise<void>.
  // Erfüllung bedeutet Ende; Ablehnung gibt Fehler/Abbruch an den gemeinsamen Spieler zurück.
  class CodeDarsteller {
    constructor(canvas, {bildLaden, anfordern, abbrechen, jetzt} = {}) {
      this.canvas = canvas;
      this.bildLaden = bildLaden || (quelle => new Promise((resolve, reject) => {
        const img = new Image(); img.onload = () => resolve(img);
        img.onerror = () => reject(new Error('Wesenbild konnte nicht geladen werden'));
        img.src = quelle;
      }));
      this.anfordern = anfordern || (fn => requestAnimationFrame(fn));
      this.abbrechen = abbrechen || (id => cancelAnimationFrame(id));
      this.jetzt = jetzt || (() => performance.now());
    }
    zeichne(p, z, img, reduziert = false) {
      const c = this.canvas.getContext('2d');
      c.clearRect(0, 0, 480, 270);
      c.fillStyle = '#101820'; c.fillRect(0, 0, 480, 270);
      const licht = c.createRadialGradient(240, 180, 0, 240, 180, 180);
      licht.addColorStop(0, p.hex + '65'); licht.addColorStop(1, p.hex + '00');
      c.fillStyle = licht; c.fillRect(0, 0, 480, 270);
      c.fillStyle = '#080e15'; c.beginPath(); c.ellipse(240, 234, 115, 13, 0, 0, Math.PI*2); c.fill();
      if (!reduziert) {
        c.save(); c.strokeStyle = p.hex; c.lineWidth = 1.5 + p.stufe;
        for (let i = 0; i <= p.stufe; i++) {
          c.globalAlpha = z.kreis * (.5 - i*.1);
          c.beginPath(); c.ellipse(240, 230, (70+i*24)*z.kreis, (10+i*5)*z.kreis, 0, 0, Math.PI*2); c.stroke();
        }
        const strahl = c.createLinearGradient(0, 15, 0, 240);
        strahl.addColorStop(0, p.hex+'00'); strahl.addColorStop(.8, p.hex+'95'); strahl.addColorStop(1, '#fff5c899');
        c.globalAlpha = z.saeule; c.fillStyle = strahl;
        const breite = 30 + p.stufe*22, oben = 80 - p.stufe*31;
        c.beginPath(); c.moveTo(240-breite, 230); c.lineTo(240-breite*.55, oben);
        c.lineTo(240+breite*.55, oben); c.lineTo(240+breite,230); c.fill();
        c.restore();
        // Deterministische Flugbahnen: kein Zufallsflackern, keine weißen Bildschirmblitze.
        c.save();
        for (let i=0; i<z.funken; i++) {
          const winkel=i*2.39996, r=(30+(i%7)*12)*z.kreis;
          const lauf=(z.t*1.4+i/z.funken)%1;
          const x=240+Math.cos(winkel)*r*(1-z.wesen*.35);
          const y=229-lauf*(90+p.stufe*28);
          c.globalAlpha=Math.sin(lauf*Math.PI)*(.65-z.wesen*.25);
          c.fillStyle=p.glanz && i%3===0 ? '#fff1bd' : p.hex;
          c.beginPath(); c.arc(x,y, p.glanz && i%3===0 ? 2 : 1.2,0,Math.PI*2); c.fill();
        }
        c.restore();
      }
      c.save(); c.globalAlpha = reduziert ? 1 : z.wesen;
      c.translate(240, 230); const skalierung = reduziert ? 1 : .86 + z.wesen*.14;
      c.scale(skalierung, skalierung);
      c.drawImage(img, -240, -230-(reduziert ? 0 : (1-z.wesen)*12), 480, 270); c.restore();
      // Glanz bleibt auch in der bewegungsarmen Ansicht sichtbar.
      if (p.glanz && (reduziert || z.wesen > .5)) {
        c.fillStyle='#f3d99b';
        for (const [x,y] of [[125,100],[355,140],[310,65]]) {
          c.beginPath(); c.moveTo(x,y-5); c.lineTo(x+2,y); c.lineTo(x,y+5); c.lineTo(x-2,y); c.fill();
        }
      }
    }
    spiele(p, {signal, reduziert=false, phase=()=>{}}) {
      this.canvas.hidden = false;
      return new Promise((resolve, reject) => {
        let frame, timer, fertig=false;
        const ende = fehler => {
          if (fertig) return; fertig=true;
          if (frame !== undefined) this.abbrechen(frame);
          clearTimeout(timer); signal?.removeEventListener('abort', stop);
          fehler ? reject(fehler) : resolve();
        };
        const stop = () => ende(abbruch());
        if (signal?.aborted) { stop(); return; }
        signal?.addEventListener('abort', stop, {once:true});
        timer=setTimeout(()=>ende(new Error('Wesenbild lädt zu lange')), 5000);
        this.bildLaden(p.bild).then(img => {
          if (fertig) return;
          clearTimeout(timer);
          if (reduziert) {
            this.zeichne(p, zustand(p,p.dauer), img, true); phase('Standbild · Bewegung reduziert'); ende(); return;
          }
          const start=this.jetzt(); let letztePhase;
          const tick = zeit => {
            if (fertig) return;
            try {
              const z=zustand(p, zeit-start); this.zeichne(p,z,img);
              if (z.phase !== letztePhase) { letztePhase=z.phase; phase(z.phase); }
              z.fertig ? ende() : frame=this.anfordern(tick);
            } catch (fehler) { ende(fehler); }
          };
          frame=this.anfordern(tick);
        }).catch(ende);
      });
    }
  }

  class VideoDarsteller {
    constructor(video, quelle, {warten=setTimeout, loeschen=clearTimeout}={}) {
      if (!/^videos\/[a-zA-Z0-9_-]+\.(mp4|webm)$/.test(quelle)) throw new Error('Lokales Video erforderlich');
      this.video=video; this.quelle=quelle;
      this.warten=warten; this.loeschen=loeschen;
    }
    spiele(p, {signal, phase=()=>{}}) {
      return new Promise((resolve,reject) => {
        const v=this.video; let fertig=false, timer, letzteZeit=-1;
        const ende = fehler => {
          if (fertig) return; fertig=true; this.loeschen(timer);
          signal?.removeEventListener('abort', stop);
          v.removeEventListener('ended', abgeschlossen); v.removeEventListener('error', fehlerhaft);
          v.removeEventListener('playing', warten); v.removeEventListener('timeupdate', fortschritt);
          v.pause(); v.hidden=true; v.removeAttribute('src'); v.load();
          fehler ? reject(fehler) : resolve();
        };
        const stop=()=>ende(abbruch()), abgeschlossen=()=>ende();
        const fehlerhaft=()=>ende(new Error('Video nicht verfügbar'));
        // Die Clip-Länge darf später von der Code-Dauer abweichen; nur Stillstand begrenzen.
        const warten=()=>{this.loeschen(timer);timer=this.warten(fehlerhaft,5000);};
        const fortschritt=()=>{if(v.currentTime>letzteZeit){letzteZeit=v.currentTime;warten();}};
        if (signal?.aborted) { stop(); return; }
        signal?.addEventListener('abort', stop, {once:true});
        v.addEventListener('ended', abgeschlossen); v.addEventListener('error', fehlerhaft);
        v.addEventListener('playing', warten); v.addEventListener('timeupdate', fortschritt);
        v.hidden=false; v.muted=true; v.playsInline=true; v.src=this.quelle;
        phase('Video-Beschwörung');
        warten();
        try { Promise.resolve(v.play()).catch(ende); } catch (fehler) { ende(fehler); }
      });
    }
  }

  class Spieler {
    constructor(code, videoFuer=()=>null) { this.code=code; this.videoFuer=videoFuer; this.aktiv=null; }
    stop() { this.aktiv?.abort(); this.aktiv=null; }
    async spiele(p, optionen={}) {
      this.stop(); const controller=new AbortController(); this.aktiv=controller;
      const opts={...optionen, signal:controller.signal};
      try {
        // Bewegungsarme Darstellung muss auch bei einem später registrierten Video greifen.
        const video=opts.reduziert ? null : this.videoFuer(p);
        if (video) {
          try {
            if (this.code.canvas) this.code.canvas.hidden=true;
            await video.spiele(p,opts);
            // Nach dem Clip bleibt dieselbe Kartenillustration stehen wie nach Code.
            await this.code.spiele(p,{...opts,reduziert:true,phase:()=>{}}); return;
          }
          catch (fehler) {
            if (controller.signal.aborted || fehler?.name==='AbortError') throw fehler;
            opts.phase?.('Video nicht verfügbar · Lichtvorschau');
          }
        }
        await this.code.spiele(p,opts);
      } finally { if (this.aktiv===controller) this.aktiv=null; }
    }
  }
  const api=Object.freeze({plan,zustand,CodeDarsteller,VideoDarsteller,Spieler});
  if (typeof module === 'object' && module.exports) module.exports=api;
  else root.Beschwoerung=api;
})(typeof globalThis === 'object' ? globalThis : this);
