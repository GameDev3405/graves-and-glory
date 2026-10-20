/* Layout lint for the mockups. Runs only when the page URL has ?check (render.py --check / --all-v2).
   It measures every visible text run and reports, as JSON in <script id="check-out">:
   - clipped: text cut by an ancestor with overflow/clip-path, or spilling past its own box
   - covered: another painted element sits on top of the text (unless marked data-stack)
   - crossed: a painted element that is not the text's own box runs underneath the text
   - tight: text closer than 8 px to the edge of its [data-panel] (4 px inside [data-ctl])
   - edge: any content within 12 px of the 960x540 screen edge (backdrops marked data-bleed are exempt)
   - boxes: [data-panel]/[data-ctl] boxes that intersect without nesting (unless data-stack)
   - sizes: the distinct type sizes on screen (Minecraft 1x = 8, 2x = 16, CSS px for TTF text)
   - subpixel: Minecraft text placed off the whole-pixel grid (it would blur when doubled)
   - words: visible words on screen
   Text rects are ink boxes (canvas measureText), not line boxes, so stacked lines are not false hits. */
(function(){
  if(!/[?&]check\b/.test(location.search))return;
  const W=960,H=540,EDGE=12,PAD=8,CPAD=4;
  const isVisible=el=>{for(let e=el;e&&e.nodeType===1;e=e.parentElement){const cs=getComputedStyle(e);if(cs.display==='none'||cs.visibility==='hidden'||+cs.opacity===0)return false;}return true;};
  const transparent=c=>!c||c==='transparent'||/rgba\([^)]*,\s*0\)$/.test(c);
  /* does el put paint on screen at all (x,y omitted), or at point (x,y)? */
  function painted(el,x,y){
    const at=x!==undefined;
    if(el.tagName==='CANVAS'||el.tagName==='IMG')return true;
    if(el.classList.contains('px')){
      const p=el._px||(el.parentElement&&el.parentElement._px);if(!p||!at)return true;
      const host=el._px?el:el.parentElement;const r=host.getBoundingClientRect();
      const row=p.rows[Math.floor((y-r.top)/p.s)];const ch=row&&row[Math.floor((x-r.left)/p.s)];return !!ch&&ch!=='.';
    }
    if(el.tagName==='B'&&el.parentElement&&el.parentElement._px)return painted(el.parentElement,x,y);
    const cs=getComputedStyle(el);
    if(!transparent(cs.backgroundColor))return true;
    if(cs.backgroundImage&&cs.backgroundImage!=='none')return true;
    const r=el.getBoundingClientRect();
    for(const s of ['Top','Right','Bottom','Left']){
      const bw=parseFloat(cs['border'+s+'Width']);
      if(bw>0&&cs['border'+s+'Style']!=='none'&&!transparent(cs['border'+s+'Color'])){
        if(!at)return true;
        if((s==='Top'&&y<r.top+bw)||(s==='Bottom'&&y>=r.bottom-bw)||(s==='Left'&&x<r.left+bw)||(s==='Right'&&x>=r.right-bw))return true;
      }
    }
    return false;
  }
  const desc=el=>{const t=el.tagName.toLowerCase();const id=el.id?'#'+el.id:'';const cl=el.classList.length?'.'+[...el.classList].join('.'):'';const tx=(el.getAttribute&&el.getAttribute('data-name'))||'';return t+id+cl+(tx?'['+tx+']':'');};
  const R=r=>({x:Math.round(r.left*10)/10,y:Math.round(r.top*10)/10,w:Math.round(r.width*10)/10,h:Math.round(r.height*10)/10});
  const inter=(a,b)=>!(a.right<=b.left||b.right<=a.left||a.bottom<=b.top||b.bottom<=a.top);
  const inside=(a,b,t=0)=>a.left>=b.left-t&&a.right<=b.right+t&&a.top>=b.top-t&&a.bottom<=b.bottom+t;
  const ctx=document.createElement('canvas').getContext('2d');

  function texts(){
    const out=[];
    document.querySelectorAll('mc-text').forEach(el=>{
      if(!isVisible(el))return;const c=el.querySelector('canvas');
      if(!c){ if(el.style.fontFamily){ /* TTF mode of mc-text: handled as DOM text below */ } return; }
      const r=c.getBoundingClientRect();const sc=parseInt(el.getAttribute('scale')||'1',10);
      const txt=el.dataset.text||el.getAttribute('data-text')||'';
      /* canvas = text width + 1 (shadow column), height 10; ink rows 0..7 incl. shadow, row 8 only for descenders */
      const ink={left:r.left,top:r.top,right:r.right,bottom:r.top+(/[gjpqy,;]/.test(txt)?9:8)*sc};
      out.push({el,node:c,kind:'mc',size:8*sc,family:'Minecraft',text:txt,full:txt,color:el.getAttribute('color')||'#ffffff',ink});
    });
    const tw=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);
    while(tw.nextNode()){
      const n=tw.currentNode;const s=n.textContent;if(!s.trim())continue;const p=n.parentElement;
      if(!p||p.closest('script,style,#check-out'))continue;if(p.closest('mc-text')&&p.closest('mc-text').querySelector('canvas'))continue;
      if(!isVisible(p))continue;
      const cs=getComputedStyle(p);const range=document.createRange();range.selectNodeContents(n);
      const rects=[...range.getClientRects()].filter(r=>r.width>0&&r.height>0);
      const fam=cs.fontFamily.split(',')[0].replace(/["']/g,'');const fs=parseFloat(cs.fontSize);
      ctx.font=`${cs.fontStyle} ${cs.fontWeight} ${fs}px ${cs.fontFamily}`;
      const fm=ctx.measureText('Hg');const asc=fm.fontBoundingBoxAscent,descn=fm.fontBoundingBoxDescent;
      const m=ctx.measureText(s.trim());
      rects.forEach((r,i)=>{
        /* range rects are the font's content area (ascent..descent), so the baseline sits asc below the top;
           ink = advance width horizontally, measured glyph ascent/descent vertically */
        const base=r.top+(r.height-(asc+descn))/2+asc;
        const ink={left:r.left,right:r.right,top:base-m.actualBoundingBoxAscent,bottom:base+m.actualBoundingBoxDescent};
        out.push({el:p,node:p,kind:'ttf',size:Math.round(fs*10)/10,family:fam,text:s.trim().slice(0,60),full:i===0?s.trim():'',color:cs.color,ink,line:i});
      });
    }
    return out;
  }

  function nearest(el,sel){for(let e=el;e&&e.nodeType===1;e=e.parentElement)if(e.matches(sel))return e;return null;}

  function run(){
    const issues=[];const T=texts();const sizes={};
    T.forEach(t=>{const k=t.kind==='mc'?(t.size===8?'8 (Minecraft 1x)':t.size+' (Minecraft '+(t.size/8)+'x)'):(t.size+' ('+t.family+')');sizes[k]=(sizes[k]||0)+1;});
    const sizeSteps=new Set(T.map(t=>Math.round(t.size)));
    T.forEach((t,i)=>{
      const a=t.ink;const label=(t.text||'').slice(0,40)||desc(t.el);
      /* 1. clipped by an overflow or clip-path ancestor */
      for(let e=t.el.parentElement||t.el;e&&e!==document.body;e=e.parentElement){
        const cs=getComputedStyle(e);
        if(cs.overflow!=='visible'||cs.clipPath!=='none'){
          const b=e.getBoundingClientRect();
          if(!inside(a,b,0.5)){
            if(cs.textOverflow==='ellipsis'&&e.hasAttribute('data-full'))issues.push({type:'truncated',text:label,by:desc(e),full:e.getAttribute('data-full')});
            else issues.push({type:'clipped',text:label,by:desc(e),ink:R({left:a.left,top:a.top,width:a.right-a.left,height:a.bottom-a.top}),box:R(b)});
          }
          break;
        }
      }
      /* 2. spills past its own painted box or panel */
      const box=nearest(t.el,'[data-panel],[data-ctl]');
      if(box){
        const b=box.getBoundingClientRect();const pad=box.hasAttribute('data-ctl')?CPAD:PAD;
        if(!inside(a,b,0))issues.push({type:'spills',text:label,box:desc(box)});
        else{const d=Math.min(a.left-b.left,b.right-a.right,a.top-b.top,b.bottom-a.bottom);if(d<pad-0.01)issues.push({type:'tight',text:label,box:desc(box),gap:Math.round(d*10)/10,need:pad});}
      }
      /* 3. covered / crossed: sample the ink box */
      const covered=new Map(),crossed=new Map();
      const sx=Math.max(1,(a.right-a.left)/12),sy=Math.max(1,(a.bottom-a.top)/3);
      for(let y=a.top+0.5;y<a.bottom;y+=sy)for(let x=a.left+0.5;x<a.right;x+=sx){
        if(x<0||y<0||x>=W||y>=H)continue;
        const stack=document.elementsFromPoint(x,y);const me=stack.findIndex(e=>e===t.node||e===t.el||t.el.contains(e));
        if(me<0)continue;
        for(let k=0;k<me;k++){const e=stack[k];if(e===document.documentElement||e===document.body)continue;if(t.el.contains(e)||e.contains(t.el))continue;if(nearest(e,'[data-stack]')||nearest(e,'[data-overlay-ok]'))continue;if(painted(e,x,y))covered.set(desc(e),1);}
        const own=nearest(t.el,'[data-panel],[data-ctl]');
        for(let k=me+1;k<stack.length;k++){const e=stack[k];if(e===document.documentElement||e===document.body)break;if(e.contains(t.el))break; if(own&&(e===own))break; if(nearest(e,'[data-under-ok]')||nearest(e,'[data-bleed]'))continue; if(painted(e,x,y)&&!e.contains(t.el))crossed.set(desc(e),1);}
      }
      covered.forEach((_,k)=>issues.push({type:'covered',text:label,by:k}));
      crossed.forEach((_,k)=>issues.push({type:'crossed',text:label,by:k}));
      /* 3b. Minecraft text drawn off the pixel grid turns soft when doubled */
      if(t.kind==='mc'){const r=t.node.getBoundingClientRect();if(Math.abs(r.left-Math.round(r.left))>0.01||Math.abs(r.top-Math.round(r.top))>0.01)issues.push({type:'subpixel',text:label,at:[r.left,r.top]});}
      /* 4. text against text */
      for(let j=i+1;j<T.length;j++){const b=T[j].ink;if(T[j].el===t.el&&T[j].line!==t.line)continue;if(inter({left:a.left+0.5,right:a.right-0.5,top:a.top+0.5,bottom:a.bottom-0.5},b))issues.push({type:'text-overlap',text:label,other:(T[j].text||'').slice(0,40)});}
    });
    /* 5. edge distance for content */
    const content=[...document.querySelectorAll('body *')].filter(e=>!e.closest('[data-bleed]')&&!e.closest('script,style')&&isVisible(e)&&(painted(e)||e.tagName==='MC-TEXT'));
    const edgeHits=new Map();
    content.forEach(e=>{const r=e.getBoundingClientRect();if(r.width===0||r.height===0)return;if(e.closest('[data-vanilla]'))return;
      const d=Math.min(r.left,r.top,W-r.right,H-r.bottom);if(d<EDGE-0.01){const top=e.closest('[data-name]')||e;edgeHits.set(desc(top),Math.min(edgeHits.get(desc(top))??99,Math.round(d*10)/10));}});
    edgeHits.forEach((d,k)=>issues.push({type:'edge',el:k,dist:d}));
    /* 6. boxes intersecting without nesting */
    const boxes=[...document.querySelectorAll('[data-panel],[data-ctl]')].filter(isVisible);
    for(let i=0;i<boxes.length;i++)for(let j=i+1;j<boxes.length;j++){
      const A=boxes[i],B=boxes[j];if(A.contains(B)||B.contains(A))continue;if(A.closest('[data-stack]')||B.closest('[data-stack]'))continue;
      const a=A.getBoundingClientRect(),b=B.getBoundingClientRect();
      if(inter({left:a.left+0.5,right:a.right-0.5,top:a.top+0.5,bottom:a.bottom-0.5},b))issues.push({type:'box-overlap',a:desc(A),b:desc(B)});
    }
    const words=T.reduce((n,t)=>n+(t.full||'').split(/\s+/).filter(w=>/[A-Za-z0-9]/.test(w)).length,0);
    const res={page:location.pathname.split('/').pop(),words,sizes,sizeSteps:[...sizeSteps].sort((x,y)=>x-y),
      texts:T.map(t=>({text:t.text,kind:t.kind,size:t.size,family:t.family,color:t.color,ink:{x:t.ink.left,y:t.ink.top,w:t.ink.right-t.ink.left,h:t.ink.bottom-t.ink.top}})),issues};
    const s=document.createElement('script');s.type='application/json';s.id='check-out';s.textContent=JSON.stringify(res);document.body.appendChild(s);
  }
  function whenReady(){
    const v2=document.documentElement.classList.contains('v2');
    const ready=()=>document.documentElement.dataset.mcready&&(!v2||document.documentElement.dataset.laidout);
    let tries=0;(function wait(){if(ready()||tries++>400){(document.fonts?document.fonts.ready:Promise.resolve()).then(()=>setTimeout(run,50));}else setTimeout(wait,25);})();
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',whenReady);else whenReady();
})();
