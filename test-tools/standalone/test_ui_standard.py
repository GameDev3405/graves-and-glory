"""The UI standard over src/main/java/gg/gravesandglory/*.java (UI plan Task 3), without launching the game:
- no hint hardcodes a default key: no string literal matches a banned pattern below, unless the match sits inside a {key:...} token;
- the old team colours and factionColor( live only in DarkUi.java;
- the HUD and screen files listed below draw no ARGB literal of their own (colours live in DarkUi, the feed inks in FeedLines),
  except the map terrain fills, the FantasyButton class body and a bit mask (a literal right after &, as in col&0xff00ff00);
- every teamColor( outside DarkUi.java has a teamMark( within three lines, so colour never carries a meaning alone;
- no string literal names a mouse button in English ("Right Button"): chips go by the bound key, not its name (UI Task 8a);
- the battle HUD's files (HUD_CANVAS) never take the screens' canvas scale or size (UiCanvas.k, width, height, begin): the HUD
  draws at its own scale, UiCanvas.hudK (UI Task 8a);
- the methods that write on the darker vellum plates (LO_PLATE_TEXT: the key strip, the lesson note) name only DarkUi inks that
  hold 4.5:1 on those plates' darkest vellum, #bda97c (UI Task 8c fix round 1: sepia gave 3.7:1 there).
Prints 'test_ui_standard: clean', or one 'file: snippet' line per finding and exits 1."""
import pathlib
import re
import sys

SRC = pathlib.Path(__file__).resolve().parents[2] / 'src/main/java/gg/gravesandglory'
BANNED_TEXT = [r'Press [A-Z]\b', r'\b[A-Z] opens', r'[Rr]ight-click', r'Click:', r'"M /', r'cyan ally', r'Gold yours',
               r'\b(Left|Right|Middle) Button\b']
HUD_CANVAS = {'BattleHud', 'ChatPlacement'}
SCREEN_CANVAS = re.compile(r'\bUiCanvas\s*\.\s*(k|width|height|begin)\s*\(')
OLD_COLOURS = ['0xff57d9ee', '0xffc58bff', '0xff70a8a4', '0xff9e77ba', '0xff6ddfe9']
NO_ARGB = {'GravesAndGloryClient', 'CommandMode', 'CampScreen', 'CampaignClient', 'BattleHud', 'EventFeed', 'TitleScene', 'Portrait'}
ARGB_ALLOWED = {'0x66303c39', '0xff34473b'}
ARGB = re.compile(r'\b0[xX][0-9a-fA-F]{8}(?![0-9a-fA-F_])')
BACKSLASH = chr(92)
KEY_TOKEN = re.compile(r'\{key:[^{}]*\}')
LO_PLATE_TEXT = {'BattleHud': ('keyStrip', 'lesson')}
LO_VELLUM_EDGE = 0xbda97c
INK_TOKEN = re.compile(r'\bDarkUi\s*\.\s*([A-Z][A-Z0-9_]*)\b')
INK_DECL = re.compile(r'\b([A-Z][A-Z0-9_]*)\s*=\s*0[xX]([0-9a-fA-F]{8})\b')


def contrast(a, b):
    """WCAG 2 contrast of two 0xRRGGBB colours, as UiMath.contrast."""
    def lum(c):
        ch = [((c >> s) & 255) / 255 for s in (16, 8, 0)]
        ch = [v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4 for v in ch]
        return 0.2126 * ch[0] + 0.7152 * ch[1] + 0.0722 * ch[2]
    la, lb = lum(a), lum(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)


def darkui_inks():
    """DarkUi's ARGB tokens by name, read from its source (an alias such as GOLD=BRASS has no literal and is left out)."""
    code, _ = scan((SRC / 'DarkUi.java').read_text(encoding='utf-8'))
    return {m.group(1): int(m.group(2), 16) & 0xffffff for m in INK_DECL.finditer(code)}


def method_spans(code, name):
    """The body offsets of each static method `name` declares (every overload), by brace matching over the blanked code."""
    spans = []
    for m in re.finditer(r'\bstatic\s+[\w<>\[\],.? ]+?\s+' + name + r'\s*\([^()]*\)\s*\{', code):
        depth = 0
        for k in range(m.end() - 1, len(code)):
            if code[k] == '{':
                depth += 1
            elif code[k] == '}':
                depth -= 1
                if depth == 0:
                    spans.append((m.end() - 1, k + 1))
                    break
    return spans


def scan(text):
    """Splits Java source into code with comments and literals blanked (newlines kept, so offsets and lines still match) and
    the string literals, each as (offset, literal with its quotes). Text blocks count as string literals."""
    code, strings, i, n = list(text), [], 0, len(text)

    def blank(a, b):
        for k in range(a, b):
            if code[k] != '\n':
                code[k] = ' '
    while i < n:
        c = text[i]
        if text.startswith('//', i):
            j = text.find('\n', i)
            j = n if j < 0 else j
            blank(i, j)
            i = j
        elif text.startswith('/*', i):
            j = text.find('*/', i + 2)
            j = n if j < 0 else j + 2
            blank(i, j)
            i = j
        elif text.startswith('"""', i):
            j = text.find('"""', i + 3)
            j = n if j < 0 else j + 3
            strings.append((i, text[i:j]))
            blank(i, j)
            i = j
        elif c == '"' or c == "'":
            j = i + 1
            while j < n and text[j] != c and text[j] != '\n':
                j += 2 if text[j] == BACKSLASH else 1
            j = min(j + 1, n)
            if c == '"':
                strings.append((i, text[i:j]))
            blank(i, j)
            i = j
        else:
            i += 1
    return ''.join(code), strings


def line_of(text, offset):
    return text.count('\n', 0, offset) + 1


def body_span(code, name):
    """The offsets of the class body of `name` (from its opening brace to its closing one), found by brace matching over the
    code with comments and literals blanked; None when the file declares no such class."""
    m = re.search(r'\bclass\s+' + name + r'\b', code)
    if not m:
        return None
    start = code.find('{', m.end())
    if start < 0:
        return None
    depth = 0
    for k in range(start, len(code)):
        if code[k] == '{':
            depth += 1
        elif code[k] == '}':
            depth -= 1
            if depth == 0:
                return start, k + 1
    return start, len(code)


def check_file(path):
    found = []
    text = path.read_text(encoding='utf-8')
    code, strings = scan(text)
    name = path.stem
    for offset, literal in strings:
        tokens = [(t.start(), t.end()) for t in KEY_TOKEN.finditer(literal)]
        for pattern in BANNED_TEXT:
            for m in re.finditer(pattern, literal):
                if not any(a <= m.start() and m.end() <= b for a, b in tokens):
                    found.append(f'{path.name}:{line_of(text, offset)}: {literal}')
    if name != 'DarkUi':
        lowered = text.lower()
        for colour in OLD_COLOURS:
            for m in re.finditer(re.escape(colour), lowered):
                found.append(f'{path.name}:{line_of(text, m.start())}: {text[m.start():m.end()]}')
        for m in re.finditer(r'factionColor\(', text):
            found.append(f'{path.name}:{line_of(text, m.start())}: {text.splitlines()[line_of(text, m.start()) - 1].strip()[:120]}')
    if name in NO_ARGB or name.startswith('WarTable'):
        button = body_span(code, 'FantasyButton')
        for m in ARGB.finditer(code):
            mask = code[max(0, m.start() - 16):m.start()].rstrip().endswith('&')  # keeps channels of a colour: not a colour itself
            if mask or m.group().lower() in ARGB_ALLOWED or button and button[0] <= m.start() < button[1]:
                continue
            found.append(f'{path.name}:{line_of(text, m.start())}: {m.group()}')
    if name in HUD_CANVAS:
        for m in SCREEN_CANVAS.finditer(code):
            found.append(f'{path.name}:{line_of(text, m.start())}: {m.group()} takes the screens\' scale, not the HUD\'s (hudK)')
    if name in LO_PLATE_TEXT:
        inks = darkui_inks()
        for method in LO_PLATE_TEXT[name]:
            spans = method_spans(code, method)
            if not spans:
                found.append(f'{path.name}: no static method {method}( to check the darker vellum\'s inks in')
            for a, b in spans:
                for m in INK_TOKEN.finditer(code, a, b):
                    ink = inks.get(m.group(1))
                    ratio = None if ink is None else contrast(ink, LO_VELLUM_EDGE)
                    if ratio is None or ratio < 4.5:
                        found.append(f'{path.name}:{line_of(text, m.start())}: DarkUi.{m.group(1)} in {method}( holds '
                                     + ('no known ink' if ratio is None else f'{ratio:.2f}:1') + ' on the darker vellum\'s edge #bda97c (wanted 4.5)')
    if name != 'DarkUi':
        lines = code.split('\n')
        for k, line in enumerate(lines):
            if 'teamColor(' in line and not any('teamMark(' in lines[j] for j in range(max(0, k - 3), min(len(lines), k + 4))):
                found.append(f'{path.name}:{k + 1}: teamColor( with no teamMark( within three lines')
    return found


def main():
    files = sorted(SRC.glob('*.java'))
    if not files:
        print(f'test_ui_standard: no sources under {SRC}')
        return 1
    found = list(dict.fromkeys(f for path in files for f in check_file(path)))  # a literal that breaks two rules is listed once
    for f in found:
        print(f)
    if found:
        print(f'test_ui_standard: {len(found)} findings')
        return 1
    print('test_ui_standard: clean')
    return 0


if __name__ == '__main__':
    sys.exit(main())
