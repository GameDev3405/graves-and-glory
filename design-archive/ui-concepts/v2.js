/* v2 layout helper. Runs once Minecraft text and the web fonts are ready, so every measured
   width is final, then snaps positions to whole pixels and marks <html data-laidout>.
   data-cx="480"  centre the element's box on x = 480
   data-rx="560"  put the element's right edge on x = 560
   data-cy="20"   centre the element's box on y = 20
   window.v2after = [fn, ...] runs after placement (for anything that depends on measured sizes). */
(function(){
  function place(){
    document.querySelectorAll('[data-cx],[data-rx],[data-cy]').forEach(el=>{
      const r=el.getBoundingClientRect();
      if(el.dataset.cx)el.style.left=Math.round(+el.dataset.cx-r.width/2)+'px';
      if(el.dataset.rx)el.style.left=Math.round(+el.dataset.rx-r.width)+'px';
      if(el.dataset.cy)el.style.top=Math.round(+el.dataset.cy-r.height/2)+'px';
    });
    (window.v2after||[]).forEach(f=>f());
    document.documentElement.dataset.laidout=1;
  }
  function go(){
    if(!document.documentElement.dataset.mcready){setTimeout(go,10);return;}
    (document.fonts?document.fonts.ready:Promise.resolve()).then(()=>setTimeout(place,0));
  }
  go();
})();
