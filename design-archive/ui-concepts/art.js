/* Pixel glyphs drawn as box-shadow grids. <i class="px" data-art="horn" data-pal="b=#c8a56b" data-scale="2"></i>
   One letter per pixel; '.' is empty. Palette letters below can be overridden per element. */
(function(){
  const PAL={k:'#101417',b:'#c8a56b',i:'#eee9df',t:'#5fb3ad',v:'#a07cc2',r:'#d9634c',g:'#8a9397',d:'#2a3236',o:'#ffaa66',w:'#ffffff',n:'#2b1d12',p:'#e6d9b8',e:'#4a2f1c',s:'#9cc7c9'};
  const ART={
    mountain:['...k....','..kkk...','.kkikk..','kkkkkkk.','.k..kk..','........'],
    tree:['...kk...','..kkkk..','.kkkkkk.','kkkkkkkk','...kk...','...kk...'],
    keep:['k.k.k.k.','kkkkkkk.','k.....k.','k..k..k.','k..k..k.','kkkkkkk.'],
    mill:['..k.k...','...k....','..k.k...','..kkk...','..kkk...','.kkkkk..'],
    cross:['...k....','...k....','kkkkkkk.','...k....','...k....','...k....'],
    bust:['..iiii..','.iiiiii.','.iiiiii.','..iiii..','.iiiiii.','iiiiiiii','iiiiiiii'],
    shrine:['...k....','..kkk...','...k....','.kkkkk..','.k.k.k..','.kkkkk..'],
    ford:['k.k.k.k.','.k.k.k.k','k.k.k.k.'],
    sword:['......ii','.....ii.','....ii..','.k.ii...','..kk....','.kkk....','kk.k....','k.......'],
    bow:['..ii....','.i..k...','i...k...','i...k...','i...k...','.i..k...','..ii....'],
    staff:['....oo..','...oooo.','....oo..','....i...','...i....','...i....','..i.....','..i.....'],
    horn:['......bb','....bbbb','..bbb.bb','.bb...b.','bb......','bb......','.b......'],
    shield:['.iiiiii.','iibiibii','iibiibii','iiiiiiii','.iiiiii.','..iiii..','...ii...'],
    coin:['..bbbb..','.biibbb.','biibbbbb','bibbbbbb','bbbbbbib','bbbbbiib','.bbbiib.','..bbbb..'],
    sun:['...i....','.i.i.i..','..iii...','iiiiiii.','..iii...','.i.i.i..','...i....'],
    crescent:['..iiii..','.ii.....','ii......','ii......','ii......','.ii.....','..iiii..'],
    hourglass:['bbbbbbb.','.iiiii..','..iii...','...i....','..i.i...','.i...i..','bbbbbbb.'],
    skull:['.iiiii..','iiiiiii.','i.iii.i.','iiiiiii.','.ii.ii..','.i.i.i..'],
    pennant:['k.......','kiiii...','kiiiiii.','kiiii...','k.......','k.......','k.......'],
    north:['...i....','..iii...','.i.i.i..','...i....','...i....','...i....'],
    move:['..i.....','...i....','iiiii...','...i....','..i.....'],
    charge:['i..i....','.i..i...','..i..i..','.i..i...','i..i....'],
    retreat:['...i....','..i.....','.iiiii..','..i.....','...i....'],
    follow:['.i......','i.i.....','.i......','....i...','...i.i..','....i...'],
    hold:['.iiiii.','iibibii','iibibii','iiiiiii','.iiiii.','..iii..','...i...'],
    star:['...i...','..iii..','iiiiiii','.iiiii.','..i.i..','.i...i.'],
    gilded:['...ii...','...ii...','.iiiiii.','...ii...','...ii...','...ii...','..iiii..'],
    graveborn:['.iiiii..','iiiiiii.','i.iii.i.','iiiiiii.','.ii.ii..','.i.i.i..'],
    wildmoot:['i.i..i.i','.ii..ii.','..i..i..','..iiii..','...ii...','...ii...'],
    ironclad:['iiiiiiii','.iiiiii.','...ii...','...ii...','..iiii..','.iiiiii.'],
    tidecallers:['i..i..i.','i..i..i.','.iiiii..','...i....','...i....','...i....'],
    seal:['..bbbb..','.bbbbbb.','bbbibbbb','bbibibbb','bbbibbbb','.bbbbbb.','..bbbb..'],
    pin:['..r.....','.rrr....','.rrr....','..r.....','..k.....','..k.....'],
    tack:['.bb.','bbbb','bbbb','.bb.'],
    rivet:['gg','gk'],
    swords:['i.....i.','.i...i..','..i.i...','...i....','..i.i...','.i...i..','i.....i.'],
    dummy:['..iii...','..iii...','bbbibbb.','...i....','...i....','..i.i...','.i...i..'],
    signpost:['bbbbbb..','biiiib..','bbbbbb..','..bb....','..bb....','..bb....','..bb....'],
    bell:['...i....','..iii...','.iiiii..','.iiiii..','iiiiiii.','...b....'],
    purse:['..bb....','.biib...','biiiib..','biiiib..','biiiib..','.bbbb...'],
    letter:['pppppppp','pkppppkp','ppkppkpp','pppkkppp','pppppppp','pppppppp'],
    candle:['...o....','...i....','..bbb...','..bbb...','..bbb...','..bbb...','.bbbbb..'],
    friend:['..ii....','..ii....','.iiii...','iiiiii..','i.ii.i..','..ii....'],
    check:['.....i..','....i...','i..i....','.ii.....','..i.....'],
    lock:['..ii..','.i..i.','.i..i.','iiiiii','iiiiii','iiiiii'],
    wave:['.ss..ss.','s..ss..s','........','.ss..ss.','s..ss..s'],
    anchor:['...i....','..i.i...','...i....','.iiiii..','...i....','i..i..i.','.iiiii..'],
    key:['.iii..','i...i.','.iii..','..i...','..ii..','..i...','..ii..'],
  };
  /* ---- v2 art (second pass). Portraits are 16x16 Minecraft-style busts of the six Gilded troops;
     each carries its own palette (APAL) so the letters do not fight the shared PAL above. ---- */
  const PP={k:'#101417',H:'#dfe3e4',h:'#aeb6ba',m:'#7c868b',d:'#4c5459',f:'#c99f72',F:'#9a744f',e:'#1d1611',
    g:'#e2bf5e',G:'#a8842f',w:'#ece6d6',W:'#c4bba7',r:'#b8402f',R:'#7d2a1f',n:'#8a6239',N:'#563a22',
    o:'#f2a53a',y:'#ffe08a',c:'#4d7a52',C:'#2f5236',b:'#6b4527',s:'#d9c9a3',S:'#a8977a'};
  const APAL={};
  Object.assign(ART,{
    p_squire:['................','.....kkkkkk.....','....kHHhhhmk....','...kHhhhhhhmk...','.kkkkkkkkkkkkkk.','.kmhhhhhhhhhhmk.','..kkmffffffmkk..','...kmfeffefmk...','...kmffffffmk...','...kmffFFffmk...','...kmmffffmmk...','..kkmmmmmmmmkk..','.kwwwwwggwwwwWk.','kmwwwwggggwwwWmk','kmmwwwwggwwwWmmk','kkkkkkkkkkkkkkkk'],
    p_archer:['................','......kkkk......','....kkcccckk....','...kcccccccCk...','..kcccCkkCcccCk.','..kccCffffCccCk.','..kccfeffefcCk..','..kCcffffffcCk..','...kCfffFffCk...','...kCCffffCCk...','..kCCCCCCCCCCk..','.kNncccccccccnk.','knnNcccccccccnNk','knnnNccccccccNnk','knnnnNcccccccNnk','kkkkkkkkkkkkkkkk'],
    p_templar:['................','...kkkkkkkkkk...','...kHHHggHhmk...','...kHhhgghhmk...','...khhhgghhmk...','...keeeggeeek...','...khhhgghhmk...','...khehgghemk...','...khhhgghhmk...','...kdmmggmmdk...','..kkkkkkkkkkkk..','.kmwwwwggwwwWmk.','kmmwwwggggwwWmmk','kmmwwwwggwwwWmmk','kmmmwwwggwwWmmmk','kkkkkkkkkkkkkkkk'],
    p_longbow:['.............b..','.....kk...r.b.b.','....kcck.rR.b..b','...kcccckR..b..b','..kkkkkkkkk.b..b','...kffffffk.b..b','...kfeffefk.b..b','...kffffffk.b..b','...kffFFffk.b..b','...kNffffNk.b..b','..kNNNNNNNNkb..b','.knncccccccnkb.b','knnNcccccccNnkbb','knnnccccccccNnkb','knnnNcccccccNnnk','kkkkkkkkkkkkkkkk'],
    p_sunweaver:['.......oy.......','......oyyo......','....kksoosskk...','...kssssssssSk..','..ksssSkkkkSsSk.','..kssSkFFFFkSsk.','..ksSkFoFFoFkSk.','..ksSkFFFFFFkSk.','..kssSkFFFFkSsk.','..ksssSkkkkSssk.','..kssssgggssssk.','.kssssgoyogssssk','kSsssssgggsssSSk','kSSssssssssssSSk','kSSSsssssssssSSk','kkkkkkkkkkkkkkkk'],
    p_marshal:['........rR......','.......rrR......','....kkkrRkkkk...','...kHHgggggHmk..','...kHhhhhhhhmk..','...khhhhhhhhmk..','...keeeeeeeeek..','...khhhhhhhhmk..','...khehhhhehmk..','...kdmmgggmmdk..','.kkkkkkkkkkkkkk.','kgGkrrrrrrrrkGgk','kgGkrrrggrrrkGgk','kGGkrrggggrrkGGk','kGGkrrrggrrrkGGk','kkkkkkkkkkkkkkkk'],
    /* 13x13 wax seal: f body, s shade, h highlight; recolour with data-pal */
    seal13:['....kkkkk....','..kkhhhhfkk..','.kkhffffffkk.','.khffffffffk.','khffffffffffk','khfffffffffsk','khfffffffffsk','khfffffffffsk','kffffffffffsk','.kffffffffsk.','.kkffffffskk.','..kkfsssskk..','....kkkkk....'],
    /* 7x7 order and formation glyphs, drawn in k */
    o_follow:['...k...','..kkk..','...k...','.......','...k...','..k.k..','.k...k.'],
    o_move:['.......','....k..','.....k.','kkkkkkk','.....k.','....k..','.......'],
    o_attack:['......k','.....k.','....k..','.k.k...','..k....','.k.k...','k......'],
    o_hold:['kkkkkkk','kkkkkkk','kkkkkkk','.kkkkk.','.kkkkk.','..kkk..','...k...'],
    o_charge:['.......','k..k...','.k..k..','..k..k.','.k..k..','k..k...','.......'],
    o_retreat:['.......','..k....','.k.....','kkkkkkk','.k.....','..k....','.......'],
    f_compact:['.......','.k.k.k.','.......','.k.k.k.','.......','.k.k.k.','.......'],
    f_line:['.......','.......','.......','k.k.k.k','.......','.......','.......'],
    f_wedge:['...k...','.......','..k.k..','.......','.k...k.','.......','k.....k'],
    f_loose:['k....k.','.......','...k...','.......','k.....k','.......','..k.k..'],
    f_shieldwall:['.......','.......','kkkkkkk','kkkkkkk','.k.k.k.','.......','.......'],
  });
  ['p_squire','p_archer','p_templar','p_longbow','p_sunweaver','p_marshal'].forEach(n=>APAL[n]=PP);
  Object.assign(ART,{
    /* point seals: Dawn owns round, Dusk owns diamond, a contested point is a hatched square */
    disc15:['.....kkkkk.....','...kkhhhhhkk...','..kkhfffffhkk..','.kkhfffffffskk.','.khfffffffffsk.','khfffffffffffsk','khfffffffffffsk','khfffffffffffsk','khfffffffffffsk','khfffffffffffsk','.khfffffffffsk.','.kksfffffffskk.','..kksfffffskk..','...kkssssskk...','.....kkkkk.....'],
    diamond15:['.......k.......','......khk......','.....khfhk.....','....khfffhk....','...khfffffhk...','..khfffffffhk..','.khfffffffffhk.','khfffffffffffsk','.ksfffffffffsk.','..ksfffffffsk..','...ksfffffsk...','....ksfffsk....','.....ksfsk.....','......ksk......','.......k.......'],
    hatch13:['kkkkkkkkkkkkk','kccaaccaaccak','kcaaccaaccaak','kaaccaaccaack','kaccaaccaacck','kccaaccaaccak','kcaaccaaccaak','kaaccaaccaack','kaccaaccaacck','kccaaccaaccak','kcaaccaaccaak','kaaccaaccaack','kkkkkkkkkkkkk'],
  });
  ART.horn15=['.........kkkkk.','........kHHHHHk','........kNbbbNk','.........kbbbk.','........kHbbNk.','.......kHbbbbk.','......kHbbNbk..','....kkHbbbbbk..','..kkHHbbbbbk...','kkHHbbbbNkk....','kbbbbbkkk......','kkkkkk.........'];
  APAL.horn15={k:'#101417',b:'#c8a56b',H:'#f0d9a0',N:'#6e5226'};
  APAL.disc15={k:'#101417',f:'#5fb3ad',h:'#9fe0da',s:'#2e6b68'};
  APAL.diamond15={k:'#101417',f:'#a07cc2',h:'#d4bdea',s:'#563a73'};
  APAL.hatch13={k:'#101417',a:'#5fb3ad',c:'#a07cc2'};

  APAL.seal13={k:'#101417',f:'#8a9397',s:'#4c5459',h:'#c9cfd2'};
  /* recruit screens: wax stamp (19), the letter's broken seal (15), the empty-star dot */
  ART.wax19=['.......kkkkk.......','.....kkhhhhhkk.....','...kkhhfffffhfkk...','..khhfsssssssfffk..','..khfssfffffssffk..','.khfsfffffffffsffk.','.khssfffffffffsssk.','khfsfffffffffffsfsk','khfsfffffffffffsfsk','khfsfffffffffffsfsk','khfsfffffffffffsfsk','khfsfffffffffffsfsk','.khssfffffffffsssk.','.kffsfffffffffsfsk.','..kffssfffffssfsk..','..kfffsssssssfssk..','...kkfsfffffsskk...','.....kkssssskk.....','.......kkkkk.......'];
  ART.wax15b=['.....kkkkk.....','...kkhhhhhkk...','..khhfsssfffk..','.khfssfffssffk.','.khsfffffff.fk.','khfsffffff.sfsk','khsfffff..ffssk','khsfff..ffffssk','khsf..ffffffssk','khf.fffffffsfsk','.kfsfffffffssk.','.kffssfffssfsk.','..kfffsssfssk..','...kkssssskk...','.....kkkkk.....'];
  APAL.wax19={k:'#3d0f0a',f:'#8a2b1f',h:'#b8503f',s:'#62190f'};
  APAL.wax15b={k:'#3d0f0a',f:'#8a2b1f',h:'#b8503f',s:'#62190f'};
  ART.dot3=['.i.','iii','.i.'];
  /* a new star's sparkle: four rays and four glints, drawn around a 7x6 star at the same scale */
  ART.spark=['.....i.....','.....i.....','.i.......i.','...........','...........','ii.......ii','...........','...........','.i.......i.','.....i.....','.....i.....'];
  ART.wax15=['.....kkkkk.....','...kkhhhhhkk...','..khhfsssfffk..','.khfssfffssffk.','.khsfffffffsfk.','khfsfffffffsfsk','khsfffffffffssk','khsfffffffffssk','khsfffffffffssk','khfsfffffffsfsk','.kfsfffffffssk.','.kffssfffssfsk.','..kfffsssfssk..','...kkssssskk...','.....kkkkk.....'];
  APAL.wax15={k:'#3d0f0a',f:'#8a2b1f',h:'#b8503f',s:'#62190f'};
  /* envelope flap: a stepped V, 112 wide and 37 deep */
  ART.flap112=(()=>{const W=112,H=37,g=[...Array(H)].map(()=>Array(W).fill('.'));for(let x=0;x<56;x++){const y=Math.round(x*36/55);g[y][x]='k';g[y][W-1-x]='k';}return g.map(r=>r.join(''));})();
  /* Marshal's Glass: the group standard upright and fallen (the pole leans, the cloth hangs from the tilted bar) */
  ART.std13=['......b......','BBBBBBBBBBBBB','.ccccccccccc.','.ccccccccccc.','.ccccccccccc.','.ccccccccccc.','.ccccccccccc.','.ccccccccccc.','.ccccccccccc.','.cccccPccccc.','.cccc.P.cccc.','.ccc..P..ccc.','......P......','......P......','......P......','......P......'];
  ART.std13f=['....BB...........','.....cBB...b.....','.....cccBBb......','.....dccccBB.....','.....dddccccBB...','......d.ddccccBB.','........ddddccccB','........P..dddcc.','.......P....d.dd.','.......P......dd.','......P..........','......P..........','.....P...........','.....P...........','....P............','....P............','...P.............','...P.............'];
  APAL.std13={B:'#c8a56b',P:'#a8916a',c:'#5fb3ad',b:'#dfbd77'};
  APAL.std13f={B:'#8a9397',P:'#8a9397',c:'#d9634c',d:'#8a3a2c',b:'#8a9397'};
  /* point seals drawn as strokes: Dawn a ring, Dusk a diamond, contested a hatched square; dark fill so an ivory letter reads */
  ART.ring15=['.....rrrrr.....','...rrfffffrr...','..rrfffffffrr..','.rrfffffffffrr.','.rfffffffffffr.','rfffffffffffffr','rfffffffffffffr','rfffffffffffffr','rfffffffffffffr','rfffffffffffffr','.rfffffffffffr.','.rrfffffffffrr.','..rrfffffffrr..','...rrfffffrr...','.....rrrrr.....'];
  ART.dia17=['........r........','.......rfr.......','......rfffr......','.....rfffffr.....','....rfffffffr....','...rfffffffffr...','..rfffffffffffr..','.rfffffffffffffr.','rfffffffffffffffr','.rfffffffffffffr.','..rfffffffffffr..','...rfffffffffr...','....rfffffffr....','.....rfffffr.....','......rfffr......','.......rfr.......','........r........'];
  ART.sq15h=['.accaaccaaccaa.','accaaccaaccaacc','ccfffffffffffca','cafffffffffffaa','aafffffffffffac','acfffffffffffcc','ccfffffffffffca','cafffffffffffaa','aafffffffffffac','acfffffffffffcc','ccfffffffffffca','cafffffffffffaa','aafffffffffffac','accaaccaaccaacc','.caaccaaccaacc.'];
  APAL.ring15={r:'#7fd0c9',f:'#24706a'};
  APAL.dia17={r:'#c3a6e0',f:'#563a73'};
  APAL.sq15h={a:'#5fb3ad',c:'#a07cc2',f:'#232a2f'};
  function draw(el){
    const name=el.dataset.art;const rows=ART[name];if(!rows)return;
    const pal=Object.assign({},PAL,APAL[name]||{});(el.dataset.pal||'').split(',').forEach(p=>{const [k,v]=p.split('=');if(k&&v)pal[k.trim()]=v.trim();});
    if(el.dataset.sil)Object.keys(pal).forEach(k=>pal[k]=el.dataset.sil);
    const s=parseInt(el.dataset.scale||'1',10);const sh=[];
    rows.forEach((row,y)=>{[...row].forEach((ch,x)=>{if(ch!=='.'&&pal[ch])sh.push(`${x*s}px ${y*s}px 0 0 ${pal[ch]}`);});});
    /* data-outline="#0d1113": a 1 px ring (8-neighbour) around the glyph, for glyphs drawn straight onto the world */
    if(el.dataset.outline){const on=new Set();rows.forEach((row,y)=>[...row].forEach((ch,x)=>{if(ch!=='.'&&pal[ch])on.add(x+','+y);}));
      const ol=new Set();on.forEach(k=>{const [x,y]=k.split(',').map(Number);for(let dx=-1;dx<=1;dx++)for(let dy=-1;dy<=1;dy++){const kk=(x+dx)+','+(y+dy);if(!on.has(kk))ol.add(kk);}});
      ol.forEach(k=>{const [x,y]=k.split(',').map(Number);sh.push(`${x*s}px ${y*s}px 0 0 ${el.dataset.outline}`);});}
    const w=Math.max(...rows.map(r=>r.length))*s,h=rows.length*s;
    /* v1 forced position:relative unless set inline, which shoved class-positioned glyphs sideways.
       v2 pages (<html class="v2">) keep whatever position the stylesheet gives them. */
    const v2=document.documentElement.classList.contains('v2');
    const pos=v2?(el.style.position||(['absolute','fixed'].includes(getComputedStyle(el).position)?getComputedStyle(el).position:'relative')):(el.style.position||'relative');
    el.style.cssText+=`;display:inline-block;position:${pos};width:${w}px;height:${h}px;vertical-align:top;`;
    const dot=document.createElement('b');dot.style.cssText=`position:absolute;left:0;top:0;width:${s}px;height:${s}px;background:transparent;box-shadow:${sh.join(',')};`;
    el.appendChild(dot);el._px={rows,s};
  }
  /* pips: <span class="pips" data-n="12" data-alive="9" data-col="#5fb3ad" data-dead="#3a4347" data-lost="2"></span> */
  function pips(el){const n=+el.dataset.n,alive=+el.dataset.alive,lost=+(el.dataset.lost||0),w=+(el.dataset.w||3),h=+(el.dataset.h||6),gap=+(el.dataset.gap||1);
    el.style.cssText+=`;display:inline-block;position:relative;height:${h}px;width:${n*(w+gap)}px;vertical-align:top;`;
    for(let i=0;i<n;i++){const d=document.createElement('b');const dead=i>=alive;const fresh=i>=alive&&i<alive+lost;
      d.style.cssText=`position:absolute;left:${i*(w+gap)}px;top:0;width:${w}px;height:${h}px;background:${fresh?(el.dataset.flash||'#ffffff'):dead?(el.dataset.dead||'#3a4347'):(el.dataset.col||'#5fb3ad')};`;el.appendChild(d);}
  }
  /* hatch: diagonal 2px pixel stairs for "contested" surfaces */
  function hatch(el){const c1=el.dataset.c1||'#5fb3ad',c2=el.dataset.c2||'#a07cc2';el.style.backgroundImage=`repeating-linear-gradient(135deg,${c1} 0 3px,${c2} 3px 6px)`;}
  function run(){document.querySelectorAll('.px').forEach(draw);document.querySelectorAll('.pips').forEach(pips);document.querySelectorAll('.hatch').forEach(hatch);}
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run);else run();
  window.pxArt={ART,draw};
})();
