"""Shared helpers for the scratch painters: box faces on a sheet (ModelPart.Cuboid layout) and flat fills."""
from PIL import Image


def faces(uv, size):
    (u, v), (w, h, d) = uv, size
    return {'top': (u + d, v, u + d + w, v + d), 'bottom': (u + d + w, v, u + d + 2 * w, v + d),
            'west': (u, v + d, u + d, v + d + h), 'north': (u + d, v + d, u + d + w, v + d + h),
            'east': (u + d + w, v + d, u + 2 * d + w, v + d + h), 'south': (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)}


def hexc(s, a=255):
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def fill(px, rect, colour):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = colour if len(colour) == 4 else colour + (255,)


def paint_box(px, uv, size, colours):
    """colours: {face: colour or callable(px, rect)}; a missing face takes colours['side']."""
    for face, rect in faces(uv, size).items():
        c = colours.get(face, colours.get('side'))
        if c is None:
            continue
        if callable(c):
            c(px, rect)
        else:
            fill(px, rect, c)


def rows(px, rect, pattern, legend):
    """Paints a face from text rows (one char per texel); '.' leaves the texel as it is."""
    x0, y0, x1, y1 = rect
    for j, line in enumerate(pattern):
        for i, ch in enumerate(line):
            if ch != '.' and x0 + i < x1 and y0 + j < y1:
                c = legend[ch]
                px[x0 + i, y0 + j] = c if len(c) == 4 else c + (255,)
