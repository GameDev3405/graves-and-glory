"""Weapons stay out of Excalibur's look: our packs carry their own art and are ordered above Excalibur (Kort, 2026-11-05)."""
import json
import pathlib
import re
import unittest

REPO = pathlib.Path(__file__).resolve().parents[2]
PACKS = REPO / 'src/main/resources/resourcepacks'
JAVA = REPO / 'src/main/java/gg/gravesandglory'
TIERS = ('wooden', 'stone', 'iron', 'golden', 'diamond', 'netherite')
TOOLS = ('sword', 'axe', 'pickaxe', 'shovel', 'hoe')


def tex_path(pack, ref):
    ns, _, rel = ref.partition(':')
    return PACKS / pack / 'assets' / ns / 'textures' / (rel + '.png')


class WeaponOverride(unittest.TestCase):
    def test_armory_models_use_only_our_textures(self):
        for tier in TIERS:
            for tool in TOOLS:
                f = PACKS / ('armory/assets/minecraft/models/item/%s_%s.json' % (tier, tool))
                self.assertTrue(f.exists(), f.name)
                for key, ref in json.loads(f.read_text(encoding='utf8'))['textures'].items():
                    self.assertTrue(ref.startswith('gravesandglory:item/armory/'), '%s %s -> %s (Excalibur re-skins minecraft:block/*)' % (f.name, key, ref))
                    self.assertTrue(tex_path('armory', ref).exists(), ref)

    def test_weapons_pack_covers_ranged_shield_trident_arrow(self):
        root = PACKS / 'weapons'
        self.assertEqual(json.loads((root / 'pack.mcmeta').read_text())['pack']['pack_format'], 34)  # 1.21.1 resource pack format (was 15 on 1.20.1)
        need = ['textures/item/%s.png' % n for n in ('bow', 'bow_pulling_0', 'bow_pulling_1', 'bow_pulling_2', 'crossbow_standby', 'crossbow_pulling_0',
                'crossbow_pulling_1', 'crossbow_pulling_2', 'crossbow_arrow', 'crossbow_firework', 'trident', 'arrow', 'spectral_arrow')]
        need += ['textures/entity/%s.png' % n for n in ('trident', 'shield_base', 'shield_base_nopattern', 'shield/base', 'projectiles/arrow')]
        need += ['models/item/%s.json' % n for n in ('shield', 'shield_blocking', 'arrow', 'spectral_arrow', 'tipped_arrow')]
        for n in need:
            self.assertTrue((root / 'assets/minecraft' / n).exists(), n)

    def test_packs_registered_and_ordered_above_excalibur(self):
        client = (JAVA / 'GravesAndGloryClient.java').read_text(encoding='utf8')
        for name in ('armory', 'weapons'):
            self.assertRegex(client, r'registerBuiltinResourcePack\((?:new Identifier|Identifier\.of)\("gravesandglory","%s"\).*DEFAULT_ENABLED' % name)
        look = (JAVA / 'LookPacks.java').read_text(encoding='utf8')
        adds = re.findall(r'out\.add\((EXCALIBUR|ARMORY|WEAPONS)\)', look)
        self.assertEqual(adds, ['EXCALIBUR', 'ARMORY', 'WEAPONS'], 'ours must come after (above) Excalibur')
        self.assertIn('Excalibur_V1.21.1.zip', look)
        self.assertIn('CLIENT_STARTED.register(LookPacks::apply)', client)

    def test_excalibur_zip_name_matches_pin(self):
        dep = next(d for d in json.loads((REPO / 'dependencies/manifest.json').read_text(encoding='utf8')) if d['slug'] == 'excalibur')
        self.assertIn('file/' + dep['fileName'], (JAVA / 'LookPacks.java').read_text(encoding='utf8'))


if __name__ == '__main__':
    unittest.main()
