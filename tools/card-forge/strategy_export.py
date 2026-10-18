"""Deterministische Spielübersetzung v1. Faktorkatalog und Kartenidentitäten bleiben erhalten."""
from __future__ import annotations
import json, math, hashlib, re, unicodedata, shutil
from pathlib import Path
from html import escape
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[1]/'prototypes/godot-hd2d'
PALETTE={'Werkvolk':'#d3b482','Glimmerwerk':'#94bcc6','Artefakte':'#aaa9de','Irrlichter':'#c5b8de','Gauklervolk':'#e2ad86','Moorglut':'#d58e70','Hortbrut':'#d5c383','Paktvolk':'#c49baa','Kräutervolk':'#a6cfa1','Erntevolk':'#d8ce9a','Leuchtfeuer':'#efc18a','Bauvolk':'#b4b99b','Erzvolk':'#9dc7c5'}
ELEMENT={'Werkvolk':'physical','Glimmerwerk':'light','Artefakte':'moon','Irrlichter':'wind','Gauklervolk':'physical','Moorglut':'fire','Hortbrut':'guard','Paktvolk':'fire','Kräutervolk':'heal','Erntevolk':'nature','Leuchtfeuer':'light','Bauvolk':'nature','Erzvolk':'guard'}
ABILITY={
 'Aufrüsten':'Der nächste eigene Glimmerwerk-/Artefakt-Helfer erhält +{v} Angriff, solange dieser Helfer lebt (einmal).',
 'Widerhall':'Beim Erscheinen: Ziehe {x} Karte(n).',
 'Glück':'Kunststück: Hast du in diesem Zug schon eine andere Karte gespielt, erhält dieser Helfer +{v}/+{v}.',
 'Glut':'Beim Erscheinen: {v} Schaden an einem gewählten Gegner.',
 'Zins':'Zu Beginn der nächsten {x} eigenen Züge: +1 Kristall (insgesamt höchstens 2 zusätzliche Kristalle pro Zug).',
 'Baukunst':'Beim Erscheinen: Ein anderer eigener Helfer erhält +{v} Leben.',
 'Heilung':'Eigener Zugbeginn: Heile den am stärksten verletzten eigenen Helfer um {v}.',
 'Nähren':'Beim Erscheinen: Andere eigene Helfer erhalten +{v} Leben.',
 'Schutzlicht':'Beim Erscheinen: Ein eigener Helfer erhält {v} Schild.',
 'Eingraben':'Eigenes Zugende: Hat dieser Helfer nicht angegriffen, erhält er +{v} Leben.',
 'Panzer':'Erleidet pro Treffer {v} Schaden weniger (mindestens 1).',
 'Blutpakt':'Beim Erscheinen: Füge einem gewählten anderen eigenen Helfer {v} Schaden zu; ziehe dann {x} Karte(n). Ohne Helfer kein Pakt.',
 'Lernen':'Jede andere gespielte Karte gibt +1 Angriff, noch {x}-mal.',
}
GIFT={
 'Sternennetz':'Andere eigene Glimmerwerk-/Artefakt-Helfer haben +1 Angriff, solange dieses Wesen lebt.',
 'Tausend Stimmen':'Ziehst du eine Karte auf die Hand: +1 Angriff (höchstens 2-mal pro Zug).',
 'Volksfest':'Andere eigene Helfer haben +1 Angriff, solange dieses Wesen lebt.',
 'Flächenbrand':'Beim Erscheinen: 3 Schaden an allen gegnerischen Helfern.',
 'Schatzkammer':'Eigenes Zugende: +1 Leben pro ungenutztem Kristall (höchstens +3).',
 'Fließband':'Eigener Zugbeginn: Beschwöre einen 1/1-Werkling, falls Platz ist.',
 'Wiedererblühen':'Stirbt dieses Wesen zum ersten Mal: Es kehrt erschöpft mit 1 Leben zurück.',
 'Sauerteig':'Eigener Zugbeginn: Eigene Helfer erhalten +1 Leben.',
 'Ewiges Feuer':'Eigenes Zugende: Heile eigene Helfer um 1.',
 'Zuflucht':'Ein anderer eigener Helfer überlebt einen tödlichen Treffer mit 1 Leben (einmal pro Zug).',
 'Kristallherz':'Beim Sterben: Beschwöre einen Erzsplitter 0/3 mit Schutz, falls Platz ist.',
 'Höllenpforte':'Stirbt ein anderer eigener Helfer: +1/+1 (höchstens 2-mal pro Zug).',
 'Erwachen':'Beim Erscheinen: Übernimm die Volksfähigkeit eines gewählten anderen eigenen Helfers.',
}
TRAIT={'Wächter':'Schutz: Gegnerische Angriffe müssen zuerst Schutzhelfer treffen.','Flackernd':'Flackernd: Der erste eigene Angriff pro Zug verursacht +1 Schaden.','Ungestüm':'Ungestüm: Kann sofort angreifen.','Knospend':'Knospend: Nächster eigener Zugbeginn +1 Angriff.','Wachstum':'Wachstum: Nächste zwei eigene Zugbeginne jeweils +1/+1.','Schnäppchen':'Schnäppchen: Kostenrabatt ist bereits eingerechnet.'}

def slug(text):return re.sub(r'[^a-z0-9]+','-',unicodedata.normalize('NFKD',text).encode('ascii','ignore').decode().lower()).strip('-')

def convert(c,r):
    cost=max(1,min(7,round(1+(c['kosten']-1)*2/3)))
    ability=c['faehigkeit'] or {};gift=c['gabe'] or {};x=int(ability.get('x',0))
    a=ability.get('name','');g=gift.get('name','')
    if a and a not in ABILITY:raise ValueError(a)
    if g and g not in GIFT:raise ValueError(g)
    ap=next((v['faehigkeit']['punkte_pro_x']*x for v in r['voelker'].values() if v['faehigkeit']['name']==a),0)
    gp=next((v['gabe']['punkte'] for v in r['voelker'].values() if v['gabe']['name']==g),0)
    tp=sum(r['merkmale'][t]['punkte'] for t in c['merkmale'])
    budget=max(4,4*cost+3-math.ceil((ap+gp)*1.25)-tp)
    ratio=c['angriff']/max(1,c['angriff']+c['leben'])
    attack=max(1,min(budget-2,round(budget*ratio)))
    health=budget-attack
    v=3*x if a=='Glut' else min(2,x) if a=='Panzer' else 2*x
    details=[f"{attack} Angriff / {health} Leben · {c['art']} · {c['rolle']}."]
    details += [TRAIT[t] for t in c['merkmale']]
    if a:details.append(f'{a} {x}: '+ABILITY[a].format(x=x,v=v))
    if g:details.append(g+': '+GIFT[g])
    details.append('Heimvolk: +1 Leben für '+c['wesen'].title()+'.')
    out=dict(id=c['id'],name=c['name'],kind='minion',cost=cost,attack=attack,health=health,
       volk=c['volk'],role=c['rolle'],art=c['art'],form=c['form'],rarity=c['seltenheit'],
       affinity=c['wesen'].lower(),element=ELEMENT[c['volk']],rare=c['seltenheit']=='Mythisch',
       glanz=c['glanz'],hex=c['hex'],strategy=True,ability=a,ability_x=x,ability_value=v,gift=g,
       traits=c['merkmale'],taunt='Wächter' in c['merkmale'],ready='Ungestüm' in c['merkmale'],
       text=f"{attack} / {health} · {c['rolle']}\n"+(a+' '+str(x) if a else 'Ohne Volksfähigkeit')+((' · '+g) if g else ''),detail='\n\n'.join(details),source_month=c['monat'],source_cost=c['kosten'],
       art_id=slug(c['volk'])+'-'+slug(c['art']),status=c['status'])
    if a=='Glut':out['strategy_target']='enemy'
    elif a in ('Baukunst','Schutzlicht','Blutpakt') or g=='Erwachen':out['strategy_target']='ally'
    return out

def motif(name):
    n=name.lower()
    for terms,kind in [(['drache','wyvern','greif','hydra'],'dragon'),(['motte','falter','libelle','fee'],'wing'),(['rabe','krähe','elster','falke','kranich','möwe','taube','vogel'],'bird'),(['schnecke'],'snail'),(['käfer','spinne','ameise','krebs','krabbe','schildkröte','panzerchen'],'shell'),(['wurm','wyrm','schlange','molch','salamander'],'serpent'),(['pilz','spor','myzel'],'mushroom'),(['baum','ent','dryade','moos','wurzel'],'tree'),(['spiegel','orakel','foliant','glyph','datenweber'],'oracle'),(['haus','burg','turm','halle'],'tower'),(['flämm','flamme','licht','geist','sylphe','nymphe','quell'],'spirit'),(['dämon','fürst','teufel','pakt'],'demon'),(['biber','bär','dachs','hamster','maus','marder','hase','igel','hirsch','ochse','ross','fohlen','mammut','maulwurf'],'animal')]:
        if any(t in n for t in terms):return kind
    return 'golem'

def svg(c):
    color=PALETTE[c['volk']];kind=motif(c['art']);s={'Jungform':0,'Reife Form':1,'Urform':2}[c['form']]
    seed=int(hashlib.sha256(c['art'].encode()).hexdigest()[:4],16)
    p=[]
    def path(d,fill='url(#body)',width=3):p.append(f'<path d="{d}" fill="{fill}" stroke="#14232c" stroke-width="{width}" stroke-linejoin="round" stroke-linecap="round"/>')
    def e(x,y,rx,ry,fill='url(#body)'):p.append(f'<ellipse cx="{x}" cy="{y}" rx="{rx}" ry="{ry}" fill="{fill}" stroke="#17232d" stroke-width="2"/>')
    def line(d,col=color,w=3):p.append(f'<path d="{d}" fill="none" stroke="{col}" stroke-width="{w}" stroke-linecap="round"/>')
    if s==2:e(128,123,94,91,'none')
    scale=[.73,.9,1.0][s];p.append(f'<g transform="translate({128*(1-scale)} {231*(1-scale)}) scale({scale})">')
    if kind in ('dragon','wing','bird'):
        for side in [-1,1]:
            path(f'M128 153 Q{128+side*49} 52 {128+side*106} 77 L{128+side*84} 114 L{128+side*61} 106 L{128+side*43} 157 Z','url(#wing)')
            line(f'M128 153 L{128+side*84} 88 M{128+side*62} 109 L{128+side*25} 159', '#dfd2ac',1)
        e(128,168,29 if kind=='dragon' else 18,42);e(136,112,24,24)
        if kind in ('dragon','bird'):path('M153 110 L181 124 L153 131 Z',color)
        if kind=='dragon':
            line('M103 185 Q58 224 35 184',color,10);path('M116 94 L109 69 L129 91 M145 94 L163 69 L158 109',color)
        else:line('M114 196 L104 224 H119 M140 196 L151 224 H137',color,6)
    elif kind=='shell':
        legs=4 if 'spinn' in c['art'].lower() else 2 if any(x in c['art'].lower() for x in ['schildkröte','panzerchen']) else 3
        for side in [-1,1]:
            for j in range(legs):line(f'M{128+side*25} {135+j*17} Q{128+side*57} {124+j*19} {128+side*74} {164+j*16}',color,6)
        e(128,164,43,43);path('M95 156 L128 130 L161 155 L153 189 L128 204 L103 189 Z','url(#wing)');line('M128 134 V201','#eadbb5',2);e(128,110,23,24)
    elif kind in ('snail','serpent'):
        if kind=='snail':
            path('M52 218 Q65 191 101 191 H172 L181 148 L200 142 L211 157 L201 212 Q128 239 52 224 Z',color);e(114,162,48,48)
            line('M111 186 Q78 164 99 144 Q128 128 138 158 Q140 181 117 173',color,5);line('M184 150 L178 126 M201 148 L212 126',color,4)
        else:
            line('M42 220 Q69 129 108 185 Q153 245 167 148 Q177 96 211 134','#17232d',38);line('M42 220 Q69 129 108 185 Q153 245 167 148 Q177 96 211 134','url(#body)',31);e(204,133,25,21)
    elif kind in ('tree','mushroom'):
        path('M104 141 L99 198 L75 229 L107 217 L127 189 L148 216 L181 231 L159 193 L152 133 Z');line('M105 161 L65 172 L43 146 M153 155 L193 173 L213 144',color,10)
        if kind=='mushroom':
            path('M52 137 Q67 61 127 62 Q194 61 204 137 Q125 168 52 137 Z','url(#wing)')
            for x,y,rx in [(93,108,11),(139,85,13),(168,120,8)]:e(x,y,rx,rx*.7,'#e4d7b9')
        else:
            for x,y,rx in [(83,117,34),(121,79,36),(164,110,36),(128,127,40)]:e(x,y,rx,24,'url(#wing)')
    elif kind=='oracle':
        path('M88 210 L76 230 H106 L128 201 L149 231 H178 L161 208 Z');e(128,132,56,76,color);e(128,132,43,62,'url(#wing)')
        for j in range(5):
            a=j*math.tau/5;x=128+math.cos(a)*73;y=130+math.sin(a)*79;path(f'M{x} {y-9} l7 9 l-7 9 l-7 -9 Z',color,1)
    elif kind=='tower':
        for x,y in [(70,124),(106,86),(147,114)]:path(f'M{x} 225 V{y} L{x+20} {y-8} L{x+39} {y+5} V225 Z');path(f'M{x-5} {y} L{x+19} {y-32} L{x+44} {y+5} Z',color)
        path('M117 224 V190 Q130 172 143 190 V224 Z','#1a2934')
    elif kind=='spirit':
        path('M128 227 Q77 230 96 187 Q66 151 106 127 Q92 94 122 63 Q128 96 149 88 Q181 123 157 158 Q194 197 160 209 L173 232 L144 221 L128 240 Z','url(#wing)');e(128,143,25,30);line('M106 164 Q66 171 57 144 M151 164 Q191 178 204 143',color,7)
    elif kind=='animal':
        e(105,221,25,10,color);e(153,221,25,10,color);e(128,180,42,42);e(128,127,39,35)
        if 'hase' in c['art'].lower():e(108,78,12,41);e(146,77,12,41)
        else:e(100,104,15,15,color);e(157,104,15,15,color)
        e(128,142,18,13,color);line('M93 167 Q64 186 88 200 M164 168 Q193 186 171 199',color,14)
        if any(x in c['art'].lower() for x in ('dachs','waschbär')):path('M94 121 L126 134 L163 120 L159 143 L128 141 L99 144 Z','#283c43')
    else:
        path('M102 187 L92 226 H117 L125 193 M140 189 L145 226 H171 L159 186 Z')
        path('M96 136 L118 117 L154 124 L170 160 L159 198 L103 198 L87 164 Z');e(128,105,30,29)
        line('M94 145 L66 155 L58 188 M166 144 L193 157 L204 185',color,16)
        if kind=='demon':path('M105 88 Q79 62 98 49 L117 81 M143 79 L161 50 Q181 68 153 92 Z',color)
        else:path('M94 87 L118 70 L152 78 L166 96 Z',color)
    # Artmerkmal bleibt über drei Formen stabil, Stufe und Ornament sind gut erkennbar.
    ex,ey=(204,133) if kind=='serpent' else (194,150) if kind=='snail' else (128,153) if kind in ('tree','mushroom') else (128,136) if kind=='oracle' else (128,105) if kind in ('shell','golem','demon') else (128,127)
    for dx in [-9,9]:e(ex+dx,ey,3.5,4.5,'#fff1bd')
    for j in range(2+s):
        x=80+(seed+j*29)%96;y=168+(seed//7+j*13)%34;path(f'M{x} {y} l5 -7 l5 7 l-5 7 Z',color,1)
    if s==2:
        path('M115 195 L128 183 L142 195 L128 207 Z','url(#light)',1)
        for j in range(9):
            a=j*math.tau/9;x=128+math.cos(a)*91;y=121+math.sin(a)*90;line(f'M{x} {y} l{math.cos(a)*6} {math.sin(a)*6}',color,1)
    p.append('</g>')
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><title>{escape(c['art'])}</title><defs><linearGradient id="body" x2=".8" y2="1"><stop stop-color="{color}"/><stop offset=".45" stop-color="#657076"/><stop offset="1" stop-color="#273c4b"/></linearGradient><radialGradient id="wing"><stop stop-color="{color}"/><stop offset="1" stop-color="#3a465b"/></radialGradient><radialGradient id="light"><stop stop-color="#fff3c9"/><stop offset="1" stop-color="{color}"/></radialGradient></defs><ellipse cx="128" cy="237" rx="66" ry="9" fill="#0b1821" opacity=".6"/>{''.join(p)}</svg>'''

def export():
    source=json.loads((HERE/'out/karten.json').read_text());rules=json.loads((HERE/'rules.json').read_text())
    cards=[convert(c,rules) for c in source['karten']]
    if len({c['id'] for c in cards})!=616:raise ValueError('616 eindeutige Identitäten erwartet')
    directory=ROOT/'assets/cards';directory.mkdir(parents=True,exist_ok=True)
    painted={'Hortdrache':'gemalt-hortdrache.png','Leuchtfeuer-Hüter':'gemalt-leuchtfeuer-hueter.png','Orakel-Konstrukt':'gemalt-orakel-konstrukt.png'}
    for c in cards:
        image=c['art_id']+'.svg';(directory/image).write_text(svg(c),encoding='utf8')
        if c['art'] in painted:
            image=painted[c['art']];shutil.copyfile(HERE/'art'/image,directory/image)
        c['art_asset']='res://assets/cards/'+image
    # Zwei Spielhelfer sind keine zusätzlichen Sammelkarten.
    tokens={}
    for id,name,volk,attack,health,guard in [('forge_werkling','Werkling','Werkvolk',1,1,False),('forge_erzsplitter','Erzsplitter','Erzvolk',0,3,True)]:
        c=dict(cards[0],id=id,name=name,art=name,volk=volk,form='Jungform',rare=False,gift='',ability='',ability_x=0,ability_value=0,attack=attack,health=health,taunt=guard,traits=['Wächter'] if guard else [],ready=False,strategy=True,affinity='',rarity='Gewöhnlich')
        c['art_asset']='res://assets/cards/'+id+'.svg';(directory/(id+'.svg')).write_text(svg(c),encoding='utf8');tokens[id]=c
    output=dict(schema_version=1,balance_version=1,source_sha256=hashlib.sha256((HERE/'out/karten.json').read_bytes()).hexdigest(),cards={c['id']:c for c in cards},tokens=tokens)
    (ROOT/'data/strategy_cards.json').write_text(json.dumps(output,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    from collections import Counter
    print('Karten:',len(cards),'Artenbilder:',len({c['art_asset'] for c in cards}),'Kosten:',dict(sorted(Counter(c['cost'] for c in cards).items())))

if __name__=='__main__':export()
