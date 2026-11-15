"""Parse javac errors from a Gradle log, dedupe, categorize. Usage: categorize.py <log> [--dump]"""
import re, sys, collections
log = open(sys.argv[1], encoding='utf-8', errors='replace').read().splitlines()
pat = re.compile(r'^(?:\s*)(\S.*?\.java):(\d+): error: (.*)$')
errs = []; seen = set()
i = 0
while i < len(log):
    m = pat.match(log[i])
    if m:
        f, ln, msg = m.groups()
        ctx = []
        j = i + 1
        while j < len(log) and not pat.match(log[j]) and j < i + 8:
            ctx.append(log[j].strip()); j += 1
        key = (f, ln, msg)
        if key not in seen:
            seen.add(key); errs.append((f, int(ln), msg, ctx))
        i = j; continue
    i += 1

def cat(f, msg, ctx):
    t = msg + ' ' + ' '.join(ctx)
    src = ctx[0] if ctx else ''
    name = f.replace(chr(92), '/').split('/')[-1]
    if 'Identifier' in msg and ('private access' in msg or 'constructor Identifier' in msg): return 'Identifier constructor'
    if 'geckolib' in t or re.search(r'\b(GeoModel|GeoEntity|GeoRenderer|AnimationState|AnimationController|RawAnimation|AnimatableManager|GeoBone|CoreGeoBone|AnimatableInstanceCache|PlayState|GeoAnimatable|Keyframe|EasingType|IValue|MolangParser|DataTicket|BakedGeoModel|BoneSnapshot|Animation|BoneAnimation|animatable|transitionLength|currentAnimation|shouldResetTick|getAnimationState|AnimationStateHandler|CoreGeoModel|GeoReplacedEntityRenderer)\b', t) or re.search(r'(Gait|Attack|Stance|Hit|Death)Controller|GeoTroop', name): return 'GeckoLib API'
    if '/mixin/' in f.replace(chr(92),'/') : return 'mixin targets'
    if re.search(r'ServerPlayNetworking|ClientPlayNetworking|PacketByteBuf|PacketSender|ServerLoginNetworking|ClientLoginNetworking|CustomPayload|PacketByteBufs|LoginPacket', t): return 'networking'
    if re.search(r'NbtIo|readCompressed|writeCompressed|NbtSizeTracker', t): return 'NbtIo'
    if re.search(r'getNbt|setNbt|getOrCreateNbt|hasNbt|getSubNbt|removeSubNbt|setCustomName|NbtCompound|var nbt|ItemStack|HideFlags|Unbreakable|DataComponent|getEnchantments|EnchantmentHelper|Enchantment|DyeableItem|getColorComponents|toJson\(|createNbt', t): return 'item NBT -> components'
    if re.search(r'DrawContext|VertexConsumer|MatrixStack|RenderLayer|RenderSystem|BufferBuilder|Tessellator|ModelPart|vertex\(|Matrix3f|Matrix4f|\bEntry\b|ShaderProgram|VertexFormat|FeatureRenderer|EntityRenderer|render\(|BufferRenderer|InGameHud|RenderTickCounter|WorldRenderContext|Camera|tickDelta|getTickDelta|drawTexture|Screen|mouseScrolled|TextRenderer|OverlayTexture|ItemRenderer|HeldItem|BakedModel|Sprite|TexturedRenderLayers|getSkinTexture|DefaultParticleType', t): return 'rendering / UI'
    if re.search(r'EntityAttribute|Attribute|removeModifier|addTemporaryModifier|StatusEffect|getAttribute|EntityAttributes|damage|DamageSource|PersistentProjectile|setPierceLevel|LivingEntity|playSound|EntityType|Brain|Navigation|PathNode|Goal|initDataTracker|getEntityName|Wagon', t): return 'entity / attributes / sound'
    if re.search(r'Registry|Registries|RegistryKey|RegistryEntry|Registration|TagKey', t): return 'registries'
    if re.search(r'toPath|getEnabledNames|getNames|sendResourcePackUrl|Session|getTickTime|removePlayerFromTeam|GRASS', t): return 'renamed vanilla API (small)'
    return 'other'

cats = collections.Counter(); byfile = collections.Counter(); filecat = collections.defaultdict(collections.Counter)
for f, ln, msg, ctx in errs:
    c = cat(f, msg, ctx); cats[c] += 1
    n = f.replace(chr(92),'/').split('src/main/java/gg/gravesandglory/')[-1]
    byfile[n] += 1; filecat[n][c] += 1
print('TOTAL unique errors:', len(errs), ' files with errors:', len(byfile))
for c, n in cats.most_common(): print(f'  {n:5d}  {c}')
print('TOP FILES')
for f, n in byfile.most_common(int(sys.argv[2]) if len(sys.argv) > 2 and sys.argv[2].isdigit() else 25):
    print(f'  {n:4d}  {f}   ' + ', '.join(f'{c}:{k}' for c, k in filecat[f].most_common(3)))
if '--dump' in sys.argv:
    for f, ln, msg, ctx in errs:
        if cat(f, msg, ctx) == 'other':
            print('OTHER', f.split('gravesandglory')[-1], ln, msg, '|', ' | '.join(x for x in ctx if x.startswith(('symbol','location')))[:200])
