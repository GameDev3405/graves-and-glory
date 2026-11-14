"""The season pass's gear-shard tier icon (War Table review item 15, Kort 2026-11-14: "Gear / House + new icon"):
a broken sword tip of cool steel over a darker iron cog with a wedge broken out, so a gear tier no longer shows the
gold coins. 35 x 31 as chapter/ic-coins is; light from the top left, a 1 px near-black outline and the siblings'
half-clear shadow one pixel down and right. Drawn by an Opus art worker, 2026-11-14."""
import math
from PIL import Image

from v4lib import Spr

W,H=35,31
OL=(14,16,21); SH=(10,5,5,115)
P={'hi':(236,241,244),'l1':(190,200,208),'l2':(150,162,172),'m':(112,124,135),'d1':(80,90,101),'d2':(56,64,74),'d3':(38,44,52),'raw':(214,221,226),
   'i1':(140,147,154),'i2':(104,111,119),'i3':(74,80,88),'i4':(44,48,55)}
def blade():
    px={}
    A=(4.5,27.5); B=(31.5,2.5)
    L=math.hypot(B[0]-A[0],B[1]-A[1]); ux=((B[0]-A[0])/L,(B[1]-A[1])/L); vx=(-ux[1],ux[0])
    TAP=11
    def hw(u):
        b=4.4-0.025*u
        return b if u<L-TAP else max(0,b*(L-u)/TAP)
    pts=[(-5,1),(-3.4,4.5),(-1.8,2),(-0.3,5.5),(1.2,3),(2.8,6.5),(5,3.5)]
    def brk(v):
        for (a,ua),(b,ub) in zip(pts,pts[1:]):
            if a<=v<=b: return ua+(ub-ua)*(v-a)/(b-a)
        return 3
    for y in range(H):
        for x in range(W):
            dx,dy=x+0.5-A[0],y+0.5-A[1]
            u=dx*ux[0]+dy*ux[1]; v=dx*vx[0]+dy*vx[1]
            h=hw(u)
            if u>L or abs(v)>h or u<brk(v): continue
            e=abs(v)/max(h,0.01)
            if u<brk(v)+1.3: k='raw' if v<0.5 else 'l2'
            elif abs(v)<0.75 and u<L-TAP: k='d3'                       # fuller groove
            elif v<0: k='hi' if e>0.72 else ('l1' if u>L*0.4 else 'l2')
            else: k='m' if e>0.72 else ('d1' if u>L*0.4 else 'd2')
            if u>=L-TAP and abs(v)<0.6: k='l2'
            px[(x,y)]=k
    for q in [(22,9),(23,8)]:  # nick in the upper edge
        px.pop(q,None)
    return px
def cog():
    px={}
    C=(26.0,22.0); R0,R1,R2=1.6,5.2,7.6
    for y in range(H):
        for x in range(W):
            dx,dy=x+0.5-C[0],y+0.5-C[1]; r=math.hypot(dx,dy); a=math.degrees(math.atan2(dy,dx))%360
            if 200<a<250: continue                     # a wedge broken away (under the blade)
            tooth=((a+15)%51.4)<22
            if r<R0 or r>(R2 if tooth else R1): continue
            lit=-(dx+dy)/max(r,0.01)
            if r>R1-0.6: k='i1' if lit>-0.3 else ('i2' if lit>-0.8 else 'i3')
            elif r<R0+1.1: k='i1' if lit<-0.2 else 'i4'
            else: k='i2' if lit>0 else 'i3'
            px[(x,y)]=k
    return px
def layer(px):
    img=Image.new('RGBA',(W,H),(0,0,0,0)); fill=set(px); out=set()
    for (x,y) in fill:
        for ddx,ddy in ((1,0),(-1,0),(0,1),(0,-1)):
            q=(x+ddx,y+ddy)
            if q not in fill and 0<=q[0]<W and 0<=q[1]<H: out.add(q)
    for q in out: img.putpixel(q,OL+(255,))
    for q,k in px.items(): img.putpixel(q,P[k]+(255,))
    return img
def shadow(img):
    s=Image.new('RGBA',(W,H),(0,0,0,0))
    for y in range(H):
        for x in range(W):
            if img.getpixel((x,y))[3] and x+1<W and y+1<H and not img.getpixel((x+1,y+1))[3]: s.putpixel((x+1,y+1),SH)
    s.alpha_composite(img); return s
ic=layer(cog()); ic.alpha_composite(layer(blade())); icon=shadow(ic)


def gear_shards():
    ic = layer(cog())
    ic.alpha_composite(layer(blade()))
    S = Spr(W, H)
    S.im = shadow(ic)  # the exporter reads .im (a bare PIL Image's .im is its core, not the picture)
    return S
