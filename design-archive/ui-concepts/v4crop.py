"""Zoom crops of a rendered PNG into the scratchpad: python v3crop.py page.png name x y w h [scale] ..."""
import os, sys
from PIL import Image
P = 'C:/Users/kortt/AppData/Local/Temp/claude/C--Users-kortt-AppData-Roaming-Claude-scratch-workspaces-1c3c77e8-4660-43cb-a286-c186fd439d66-74d4ed7b-bc16-482f-8989-7260b2d0abc1-scratch-2026-10-17-e9b69f/1ca98052-31e1-4db2-9d69-3bda301536c1/scratchpad/v4prev'
im = Image.open(sys.argv[1])
a = sys.argv[2:]
while a:
    name, x, y, w, h = a[0], *map(int, a[1:5]); a = a[5:]
    sc = 1
    if a and a[0].isdigit():
        sc = int(a[0]); a = a[1:]
    im.crop((x, y, x + w, y + h)).resize((w * sc, h * sc), Image.NEAREST).save(os.path.join(P, name + '.png'))
    print(name)
