"""Chapter 1 brief N4: the retired words never reach a player. "The Long March" was the campaign's old name and "Founders Chapter" the
season pass's; neither may appear in a Java string literal, a sheet value or the README. Code comments may keep them."""
import json, pathlib, re, unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
RETIRED = re.compile(r'long march|founders chapter|chapter 1 / the founders', re.I)
LITERAL = re.compile(r'"(?:[^"\\\n]|\\.)*"')


def code_only(text):
    """The Java source with block and line comments blanked, string literals kept."""
    out, i, n = [], 0, len(text)
    while i < n:
        if text.startswith('/*', i):
            j = text.find('*/', i + 2); i = n if j < 0 else j + 2
        elif text.startswith('//', i):
            j = text.find('\n', i); i = n if j < 0 else j
        elif text[i] == '"':
            m = LITERAL.match(text, i)
            if not m: out.append(text[i]); i += 1; continue
            out.append(m.group()); i = m.end()
        elif text[i] == "'":
            j = i + 1
            while j < n and text[j] != "'": j += 2 if text[j] == chr(92) else 1
            out.append(text[i:j + 1]); i = j + 1
        else:
            out.append(text[i]); i += 1
    return ''.join(out)


def sheet_strings(node):
    if isinstance(node, str): yield node
    elif isinstance(node, dict):
        for v in node.values(): yield from sheet_strings(v)
    elif isinstance(node, list):
        for v in node: yield from sheet_strings(v)


class CampaignWords(unittest.TestCase):
    def test_java_strings(self):
        hits = [f'{p.name}: {lit}' for p in sorted((ROOT / 'src' / 'main' / 'java').rglob('*.java')) if p.name != 'Spec.java'
                for lit in LITERAL.findall(code_only(p.read_text(encoding='utf8'))) if RETIRED.search(lit)]
        self.assertEqual(hits, [])

    def test_sheet(self):
        data = json.loads((ROOT / 'sheets' / 'design.json').read_text(encoding='utf8'))
        self.assertEqual([s for s in sheet_strings(data) if RETIRED.search(s)], [])

    def test_readme(self):
        self.assertIsNone(RETIRED.search((ROOT / 'README.md').read_text(encoding='utf8')))

    def test_comments_are_ignored(self):
        self.assertEqual(LITERAL.findall(code_only('/** The Long March */ int a; // Long March\nString s = "kept";')), ['"kept"'])


if __name__ == '__main__':
    unittest.main()
