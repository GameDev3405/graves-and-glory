"""Walking distances over built columns. Shared by build (routes in map.json) and check, and mirrored by the
mod's EngineMap.distances: 4-neighbour steps, at most MAX_STEP up or down, water at most MAX_WADE deep."""
from collections import deque

import numpy as np

MAX_STEP = 1   # largest height change of a walkable step
MAX_WADE = 1   # deepest water that can be waded
NONE = -32768


def walkable(solid, water):
    wade = np.where(water == NONE, 0, np.maximum(water - solid, 0))
    return (solid != NONE) & (wade <= MAX_WADE)


def distances(solid, water, start):
    """int32 [z, x] of steps from start (x, z); -1 where unreachable."""
    depth, width = solid.shape
    n = width * depth
    ok = walkable(solid, water).ravel().tolist()
    top = solid.astype(np.int64).ravel().tolist()
    dist = [-1] * n
    sx, sz = start
    s = sz * width + sx
    if ok[s]:
        dist[s] = 0
        q = deque([s])
        while q:
            i = q.popleft()
            x = i % width
            h, nd = top[i], dist[i] + 1
            for j, inside in ((i + 1, x + 1 < width), (i - 1, x > 0), (i + width, i + width < n), (i - width, i >= width)):
                if inside and dist[j] < 0 and ok[j] and abs(top[j] - h) <= MAX_STEP:
                    dist[j] = nd
                    q.append(j)
    return np.array(dist, dtype=np.int32).reshape(depth, width)
