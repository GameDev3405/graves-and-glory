import glob,os,shutil,re
from PIL import Image
import cv2
M='C:/Users/kortt/Melty/bannerbound/work/media'
SS='shoot-run/run/screenshots'
scenes=['kingsfield','ironhold','rivergate','hollowwood','frostmarch']
for sc in scenes:
    os.makedirs(f'{M}/stills/{sc}',exist_ok=True)
os.makedirs(f'{M}/stills-4x5',exist_ok=True)
os.makedirs(f'{M}/panoramas/carousel-4x5',exist_ok=True)
n=0
for sc in scenes:
    for f in sorted(glob.glob(f'{SS}/{sc}-s[0-9]-*.png')):
        b=os.path.basename(f)
        shutil.copy(f,f'{M}/stills/{sc}/{b}')
        im=Image.open(f).convert('RGB');w,h=im.size
        cw=int(h*4/5);x=(w-cw)//2
        im.crop((x,0,x+cw,h)).resize((1080,1350),Image.LANCZOS).save(f'{M}/stills-4x5/{b[:-4]}.jpg',quality=93)
        n+=1
print(n,'stills')
src={'kingsfield':'pano/kingsfield-pano.png','ironhold':'pano/ironhold-pano.png','rivergate':'pano/rivergate-pano.png','hollowwood':'pano/hollowwood-pano-v2.png','frostmarch':'pano/frostmarch-pano.png'}
for sc,p in src.items():
    shutil.copy(p,f'{M}/panoramas/{sc}-pano-180.png')
    im=cv2.imread(p);h,w=im.shape[:2]
    cv2.imwrite(f'{M}/panoramas/{sc}-pano-180.jpg',im,[cv2.IMWRITE_JPEG_QUALITY,93])
    tw=int(h*4/5);tiles=4;x0=(w-tw*tiles)//2
    for i in range(tiles):
        t=im[:,x0+i*tw:x0+(i+1)*tw]
        t=cv2.resize(t,(1080,1350),interpolation=cv2.INTER_LANCZOS4)
        cv2.imwrite(f'{M}/panoramas/carousel-4x5/{sc}-{i+1}.jpg',t,[cv2.IMWRITE_JPEG_QUALITY,93])
print('panos done')
