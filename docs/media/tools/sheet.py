import sys,glob,os
from PIL import Image,ImageDraw
d,out=sys.argv[1],sys.argv[2]
cols=int(sys.argv[3]) if len(sys.argv)>3 else 3
w=int(sys.argv[4]) if len(sys.argv)>4 else 800
files=sorted(glob.glob(os.path.join(d,'*.png')))
if len(sys.argv)>5: import re;files=[f for f in files if re.search(sys.argv[5],os.path.basename(f))]
ims=[]
for f in files:
    im=Image.open(f).convert('RGB');h=int(im.height*w/im.width);im=im.resize((w,h),Image.LANCZOS)
    ImageDraw.Draw(im).text((8,6),os.path.basename(f)[:-4],fill=(255,255,255))
    ims.append(im)
rows=(len(ims)+cols-1)//cols;H=ims[0].height
sheet=Image.new('RGB',(cols*(w+4),rows*(H+4)),(0,0,0))
for i,im in enumerate(ims):sheet.paste(im,((i%cols)*(w+4),(i//cols)*(H+4)))
sheet.save(out,quality=88);print(out,len(ims))
