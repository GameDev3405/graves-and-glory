"""Original pixel skin on Minecraft's standard humanoid UV layout; no copied art."""
from PIL import Image,ImageDraw
import pathlib
root=pathlib.Path(__file__).resolve().parents[1]
im=Image.new('RGBA',(64,64),(0,0,0,0));d=ImageDraw.Draw(im)
skin='#c89f7c';shade='#ad7c5e';hair='#49362b';cloth='#2b687c';dark='#203846';trim='#ccb27a'
# Head cube.
d.rectangle((0,0,31,15),fill=skin);d.rectangle((0,0,31,7),fill=hair)
for x in (0,8,16,24):d.rectangle((x,8,x+7,9),fill=hair)
d.rectangle((10,11,11,11),fill='#253848');d.rectangle((13,11,14,11),fill='#253848');d.point((12,13),fill=shade);d.rectangle((11,15,13,15),fill=hair)
# Tunic, sleeves and boots on both limb islands.
d.rectangle((16,16,39,31),fill=cloth);d.rectangle((20,20,27,20),fill=trim);d.rectangle((23,21,24,31),fill=trim)
for x,y in ((40,16),(32,48)):
    d.rectangle((x,y,x+15,y+15),fill=cloth);d.rectangle((x,y+12,x+15,y+15),fill=skin);d.rectangle((x,y+10,x+15,y+11),fill=trim)
for x,y in ((0,16),(16,48)):
    d.rectangle((x,y,x+15,y+15),fill=dark);d.rectangle((x,y+11,x+15,y+15),fill=hair)
path=root/'src/main/resources/assets/gravesandglory/textures/entity/dawn_guard.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
print('Created original Dawn guard skin (64x64).')
