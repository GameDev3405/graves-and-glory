/* Mockup toolkit. Everything is authored at GUI-scale-1 pixels (960x540 page);
   render.py upscales x2 nearest-neighbour, so one CSS px here = one Minecraft GUI px at scale 2.
   <mc-text color="#eee9df" shadow>TEXT</mc-text> draws with Minecraft's real ascii.png glyphs.
   <mc-text font="lato" size="11"> uses the game's Lato body font (rasterised at 1x like the game does). */
(function(){
  const img=new Image();img.src='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAYAAADDPmHLAAAABGdBTUEAALGPC/xhBQAABY1JREFUeNrtnNuS4yAMRP3/P937PpsEXVpI4E6VaiqT2GA4CN3IA+CRvFc0CAJAgyAAJAJAIgAkAkAiACQCQCIAJAJAIgAkAkAiACQCQCIAJK8FAMbPQfrO6v9IXv+Qr4eznW7BRAD+vr61j8D9QXzGJwEZpk0+CwDL9dYJzvQFrEEJaggIAI4GiE5+VBWD/D3vAqi+/mf/J2gAS3vW7QUNGuDZdH1J27s7AOf+b7EDsGEbQOUkTAdgxxaQ9QIyWwDLi+gCIwWvfOGBhllHHIBlhGWMvOeHlwBHGxEbhNX/DhjgtMP+A8CrYr0q2hvIAbg+NorfT9AE4ef/ZESh2FpGwMhbrd7MYFUDwQDYayi7IoFw+Ope9W1V0ZkBzbqY3i0Ag20A9/N/mhgQ1ExmAkFewaxcQ+UWgI7J99gAzwYboFolMwBAgwGIgm3Q5QVYYvlZK9tqhFaHkhHY3pgaY3skU3745X4+qyDEoyHY17M1UGRLQWKldW+JyzgAY4Amu1nZAWYYuZl7Y5HwWhnxrjjAt057VuBTQHhm/1/FOdhGFhL9swTIEPzcFAeAM/vWoeKeJhXaqQE8908B8AiAUgAmuMVLDYBiAHAZALgFAMYe3eUFZL2YrBfAuj+KgEp5AZLLzwVUr2CLm3mSGxrpf9YLAGErfKK5gF35c3Tsgc5MaCSUnU0Hewtj6Rqg2gaYAEBlXr8agFTByi4rujPbyNAAFc/VUZl8JQDWo2Eo1BBVtRLjAEAhIJUArGLplYdPjwdgVXef8kNJ+fodBSvRgg1GNhGVAEjur0f4CujEM29IPmj3sTBMMO4iJWEnAzCp/8+JADBi6dmcPYI2BCMS6Slb7y4IoVZkMYhmZg+RMDKzySzWSaBVwUZVQUg4GxgxJHanOzMVO7tC2VbAjgNgej2AN6HSla9nxRFGagCQbIBMzcAJFTuMKGNJPcCb/ODqNo6rB9Dhid5nZGUhERlPAdD/fCBe79ZCXj9+ZeRkK4JOMEKrvZStC8TrB6PIj97pxkXTydHJZEUCt2UDq9w0NFnBQM4dm1AvsEUDZEOpmf0wA0C2IMQKR8YNHQ9AdbaLWQ8A8vUMvz/Tv9ZkEWsFK45w8LkAycsPhkgEgEQASASARABIBIBEAEgEgKQt4LQ1La2Bn1cvgOI2IADOKhgpbUOD//KaRU2ANIAmQABIxh7frjL+BIA0zFUAWKqNIBDu1wCrej3gfbEE3B4I8hRs4mWTv/z8RkNq9xZQbcSxoogfv6d98I5jbq/WAJYtYOfqhGwAibwAyXkAwKBSPv3Fh/fWg6RMf37VNjPWsHq2SPuM+7vbzRyuxA8rHALgHACmawCrmxWdgNX9sXjOVfu/XlX336oBrKv/BA1gvS6rATIL4ToN4F2B3hXqXYHVGoDRv0z/8VYbILuCsvevtlFCGu5NXoAAKNIAp9gAAmChAXbk1auvrwYk48ZNuF4ACIB3APBpG4oAgMW9I5a7p3+R1xNpXBqg/tknHA1bJoMqXt33z67w6hU6SgNML87sjBROiPXT+ycABIAAEAAz0qm7ijcz7WcmJroNZ7/vNqAEgABwWZQVExDNDEbaj46RNMAhK1AagBi1yvjpIKzwyP2rYw/P5j5QgWFa4TuyXZ3ZwGj/BIAAEAAC4BIAJHMOwAK+A7IC4PDJ97wXAC+YfGQnXwDc8csfqSyvBvgcAGD8vzSAbAABcKsGsLjB2gIkNQAg+Jn3eyvKTy7IRLCtsv5ZkzSWwQdp8q0/duT9DA7LevfvGFqhj6TQ0xrAmw1jaQiQNAAModpuADKApwGYqgEiKwgJSLBBFUcBAGmBjrcBGBB5t4DqvRhJACwLc4sG6PqRRYaKbD19wwradGmAyioeJgCnZPP0O4EJQ0e+O1n+Aa1MJ+LeSEbXAAAAAElFTkSuQmCC';
  let glyphs=null;
  function build(){
    const c=document.createElement('canvas');c.width=128;c.height=128;const g=c.getContext('2d');g.drawImage(img,0,0);
    const d=g.getImageData(0,0,128,128).data;glyphs={};
    for(let code=32;code<127;code++){
      const col=code%16,row=Math.floor(code/16);let right=-1;
      for(let x=0;x<8;x++)for(let y=0;y<8;y++){if(d[((row*8+y)*128+(col*8+x))*4+3]>0)right=Math.max(right,x);}
      glyphs[code]={sx:col*8,sy:row*8,w:code===32?3:right+1};
    }
  }
  function width(text){let w=0;for(const ch of text){const gl=glyphs[ch.charCodeAt(0)]||glyphs[63];w+=gl.w+1;}return w;}
  function draw(text,color,shadow){
    const w=width(text)+1,h=10;const c=document.createElement('canvas');c.width=w;c.height=h;const g=c.getContext('2d');g.imageSmoothingEnabled=false;
    function pass(dx,dy,col){const off=document.createElement('canvas');off.width=w;off.height=h;const o=off.getContext('2d');o.imageSmoothingEnabled=false;let x=dx;
      for(const ch of text){const gl=glyphs[ch.charCodeAt(0)]||glyphs[63];o.drawImage(img,gl.sx,gl.sy,8,8,x,dy,8,8);x+=gl.w+1;}
      o.globalCompositeOperation='source-in';o.fillStyle=col;o.fillRect(0,0,w,h);g.drawImage(off,0,0);}
    if(shadow){const m=color.match(/#(..)(..)(..)/);const sh=m?'#'+[m[1],m[2],m[3]].map(x=>Math.floor(parseInt(x,16)/4).toString(16).padStart(2,'0')).join(''):'#000';pass(1,1,sh);}
    pass(0,0,color);return c;
  }
  function renderAll(){
    document.querySelectorAll('mc-text').forEach(el=>{
      if(el.dataset.done)return;el.dataset.done=1;
      let text=el.textContent;const color=el.getAttribute('color')||'#ffffff';
      /* v2: max="N" truncates to N px (at scale 1) with "..." and keeps the full string in data-full */
      if(el.hasAttribute('max')&&!el.hasAttribute('font')){const max=+el.getAttribute('max');if(width(text)>max){el.setAttribute('data-full',text);let t=text;while(t.length>1&&width(t.trimEnd()+'...')>max)t=t.slice(0,-1);text=t.trimEnd()+'...';}}
      el.dataset.text=text;
      if(el.hasAttribute('font')){el.style.fontFamily=el.getAttribute('font')==='title'?'Marcellus':'Lato';el.style.fontSize=(el.getAttribute('size')||'11')+'px';el.style.color=color;el.style.display='inline-block';el.style.lineHeight='1';el.style.whiteSpace='nowrap';
        if(el.hasAttribute('shadow'))el.style.textShadow='1px 1px 0 rgba(0,0,0,.6)';return;}
      let c=draw(text,color,el.hasAttribute('shadow'));const sc=parseInt(el.getAttribute('scale')||'1',10);if(sc>1){const big=document.createElement('canvas');big.width=c.width*sc;big.height=c.height*sc;const bg=big.getContext('2d');bg.imageSmoothingEnabled=false;bg.drawImage(c,0,0,big.width,big.height);c=big;}c.style.imageRendering='pixelated';c.style.display='inline-block';c.style.verticalAlign='top';el.textContent='';el.appendChild(c);
    });
    document.querySelectorAll('[data-mcbg]').forEach(el=>{el.style.imageRendering='pixelated';});
  }
  function ready(){build();renderAll();document.documentElement.dataset.mcready=1;}
  if(img.complete&&img.naturalWidth)ready();else img.onload=ready;
  window.mcText={width:t=>glyphs?width(t):0,draw};
})();
