"""Contact sheet for v4 sprites: python v4sheet.py out.png scale bg name[@x,y] ...
bg is 'oak' (a crop of the table), 'world' (fight-v2.png crop) or a hex colour. Sprites are laid left to right."""
import os, sys
from PIL import Image
HERE = os.path.dirname(os.path.abspath(__file__))
out, scale, bg = sys.argv[1], int(sys.argv[2]), sys.argv[3]
items = []
for a in sys.argv[4:]:
    items.append(Image.open(os.path.join(HERE, 'v4', a + '.png')).convert('RGBA'))
W = sum(i.width for i in items) + 8 * (len(items) + 1); H = max(i.height for i in items) + 16
if bg == 'oak':
    base = Image.open(os.path.join(HERE, 'v4', 'oak-table.png')).convert('RGBA').crop((200, 160, 200 + W, 160 + H))
elif bg == 'world':
    base = Image.open(os.path.join(HERE, 'bg', 'fight-v2.png')).convert('RGBA').resize((960, 540)).crop((0, 300, W, 300 + H))
else:
    base = Image.new('RGBA', (W, H), bg)
x = 8
for i in items:
    base.alpha_composite(i, (x, 8)); x += i.width + 8
base.resize((W * scale, H * scale), Image.NEAREST).save(out)
print(out, W * scale, H * scale)
