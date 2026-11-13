"""Mod key defaults: Play again's default key is unbound in vanilla 1.20.1 and shares no key with another binding the mod registers.
Only one binding per key receives presses, so a clash silently kills one of them (P was vanilla's Social Interactions)."""
import pathlib, re, unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / 'src' / 'main' / 'java' / 'gg' / 'gravesandglory'
# Vanilla 1.20.1 default keyboard bindings, as GLFW names without the prefix (mouse buttons and F-keys of the debug screen left out).
VANILLA = set('W A S D E Q F T L P C X SPACE LEFT_SHIFT LEFT_CONTROL TAB SLASH ESCAPE F1 F2 F3 F5 F11 1 2 3 4 5 6 7 8 9'.split())
KEY_CALL = re.compile(r'\bkey\s*\(\s*"([a-z0-9_]+)"\s*,\s*GLFW\s*\.\s*GLFW_KEY_([A-Z0-9_]+)\s*\)')
RAW = re.compile(r'new\s+KeyBinding\s*\(\s*"key\.gravesandglory\.([a-z0-9_]+)"\s*,\s*InputUtil\.Type\.(?:KEYSYM|MOUSE)\s*,\s*(?:GLFW\s*\.\s*GLFW_KEY_([A-Z0-9_]+)|\d+)')


def defaults():
    out = {}
    for path in SRC.glob('*.java'):
        text = path.read_text(encoding='utf8')
        for name, key in KEY_CALL.findall(text): out[name] = key
        for name, key in RAW.findall(text): out[name] = key or 'MOUSE'
    return out


class KeyDefaults(unittest.TestCase):
    def test_registrations_found(self):
        d = defaults()
        self.assertIn('play_again', d, 'play_again is not registered through key("play_again", GLFW.GLFW_KEY_x)')
        self.assertGreaterEqual(len(d), 15)

    def test_play_again_is_not_vanilla(self):
        key = defaults()['play_again']
        self.assertNotIn(key, VANILLA, f'Play again defaults to {key}, a vanilla 1.20.1 default')
        self.assertNotEqual(key, 'UNKNOWN')

    def test_play_again_shares_no_key_with_the_mod(self):
        d = defaults()
        key = d['play_again']
        clash = sorted(n for n, k in d.items() if n != 'play_again' and k == key)
        self.assertEqual(clash, [], f'Play again ({key}) shares its default with {clash}')

    def test_every_mod_default_is_unique(self):
        d = defaults()
        keys = [k for k in d.values() if k not in ('MOUSE', 'UNKNOWN')]
        dupes = sorted({k for k in keys if keys.count(k) > 1})
        self.assertEqual(dupes, [], f'mod keys defaulting to the same key: {dupes}')


if __name__ == '__main__':
    unittest.main()
