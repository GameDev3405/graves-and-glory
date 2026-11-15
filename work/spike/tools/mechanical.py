"""Cheap 1.20.1 -> 1.21.1 rewrites: Identifier.of, GeckoLib core package merge, a few one-to-one vanilla renames."""
import pathlib, re, sys
root = pathlib.Path(sys.argv[1])
RULES = [
    (r'new (net\.minecraft\.util\.)?Identifier\(', r'\1Identifier.of('),
    # GeckoLib 4.5+ merged geckolib-core into the main packages.
    (r'software\.bernie\.geckolib\.core\.animation\.', 'software.bernie.geckolib.animation.'),
    (r'software\.bernie\.geckolib\.core\.object\.PlayState', 'software.bernie.geckolib.animation.PlayState'),
    (r'software\.bernie\.geckolib\.core\.state\.', 'software.bernie.geckolib.animation.state.'),
    (r'software\.bernie\.geckolib\.core\.keyframe\.', 'software.bernie.geckolib.animation.keyframe.'),
    (r'software\.bernie\.geckolib\.core\.animatable\.instance\.', 'software.bernie.geckolib.animatable.instance.'),
    (r'software\.bernie\.geckolib\.core\.animatable\.GeoAnimatable', 'software.bernie.geckolib.animatable.GeoAnimatable'),
    (r'import software\.bernie\.geckolib\.core\.animatable\.model\.CoreGeoBone;', 'import software.bernie.geckolib.cache.object.GeoBone;'),
    (r'import software\.bernie\.geckolib\.core\.animatable\.model\.CoreGeoModel;', 'import software.bernie.geckolib.model.GeoModel;'),
    (r'\bCoreGeoBone\b', 'GeoBone'),
    (r'\bCoreGeoModel\b', 'GeoModel'),
    # One-to-one vanilla renames.
    (r'\bBlocks\.GRASS\b', 'Blocks.SHORT_GRASS'),
    (r'\bDefaultParticleType\b', 'SimpleParticleType'),
    (r'\.getEntityName\(\)', '.getNameForScoreboard()'),
]
total = {}
for p in root.rglob('*.java'):
    s = o = p.read_text(encoding='utf-8')
    for a, b in RULES:
        s, n = re.subn(a, b, s)
        if n: total[a] = total.get(a, 0) + n
    if s != o: p.write_text(s, encoding='utf-8', newline='')
for a, n in total.items(): print(n, a)
