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
  hold 4.5:1 on those plates' darkest vellum, #bda97c (UI Task 8c fix round 1: sepia gave 3.7:1 there);
- the methods that draw player names (NAME_METHODS: the party's seats on home and Friends, the Invite tags) set only fixed words
  in the heading face: every heading( there takes one string literal, so a name, and the measure that trims one, is in
  Minecraft's face, whose 1 is no I (UI Task 8b, the Task 7 review's out-of-scope 1);
- what a method of the battle HUD must draw by name, and must never (MUST_DRAW, NEVER_DRAW; War Table port Tasks 6 and 7): a card and a
  chip at their last stand draw the stamp and its words LAST STAND, never a colour alone, and the ability slot never draws the title's
  horn (each active has its own picture);
- the back rail's camp plaques (RAIL, RAIL_X, RAIL_LABELS, RAIL_W) are declared once, in WarTableHome, and nowhere else (UI Task
  8b, the Task 7 review's Minor 8), and the lit plaque WarTablePages.here( draws for the open page ("table/plaque-N-here",
  written whole for build_ui_art --verify) names WarTableHome's RAIL_W as its N, so a new rail width cannot leave the lit plaque
  the old size (8b review Minor 4);
- the accessibility pass (plan 4F.5): every mod key is a KeyBinding made inside KeyBindingHelper.registerKeyBinding( (so Controls
  lists and rebinds it) and has a key.gravesandglory.<name> line in en_us.json, as has the category; no screen file names a
  letter or digit key (GLFW_KEY_A..Z, 0..9) of its own, only GravesAndGloryClient's registrations do, because the player's keys
  go through matchesKey( (arrows, Tab, Enter, Space, Esc and the Page keys of navigation stay); every ButtonWidget subclass either
  extends UiButton or calls DarkUi.focus( so it shows a keyboard focus ring (Tab works on every custom screen).
Prints 'test_ui_standard: clean', or one 'file: snippet' line per finding and exits 1."""
import json
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
LANG = SRC.parents[2] / 'resources/assets/gravesandglory/lang/en_us.json'
KEY_CALL = re.compile(r'\bkey\s*\(\s*"([a-z0-9_]+)"')
KEY_NAME = re.compile(r'"(key\.gravesandglory\.[a-z0-9_]+)"')
BARE_KEYBINDING = re.compile(r'\bnew\s+KeyBinding\s*\(')
REGISTERED_KEYBINDING = re.compile(r'\bregisterKeyBinding\s*\(\s*new\s+KeyBinding\s*\(')
LETTER_KEY = re.compile(r'\bGLFW_KEY_[A-Z0-9]\b')
KEY_OWNER = 'GravesAndGloryClient'
BUTTON_SUBCLASS = re.compile(r'\bclass\s+(\w+)\s+extends\s+(?:[\w.]+\.)?ButtonWidget\b')
FOCUS_CALL = re.compile(r'(?:\bDarkUi\s*\.\s*|(?<![\w.]))(focus|focusRing|keyboardFocus)\s*\(')  # DarkUi's own MapChoice calls it bare
ARGB = re.compile(r'\b0[xX][0-9a-fA-F]{8}(?![0-9a-fA-F_])')
BACKSLASH = chr(92)
KEY_TOKEN = re.compile(r'\{key:[^{}]*\}')
LO_PLATE_TEXT = {'BattleHud': ('keyStrip', 'lesson')}
# the literals a method must hold (a texture name or a word, quotes left off) and the ones it must not
MUST_DRAW = {'BattleHud': {'card': ('hud/stamp-last-card', 'LAST STAND'), 'chip': ('hud/stamp-last-chip', 'LAST STAND')}}
NEVER_DRAW = {'BattleHud': {'ability': ('hud/horn',)}}
LO_VELLUM_EDGE = 0xbda97c
INK_TOKEN = re.compile(r'\bDarkUi\s*\.\s*([A-Z][A-Z0-9_]*)\b')
INK_DECL = re.compile(r'\b([A-Z][A-Z0-9_]*)\s*=\s*0[xX]([0-9a-fA-F]{8})\b')
NAME_METHODS = {'WarTablePages': ('party', 'paintInvite'), 'WarTableHome': ('party',)}
HEADING_CALL = re.compile(r'\bheading\s*\(')
RAIL_HOME = 'WarTableHome'
RAIL_DECL = re.compile(r'\b(RAIL|RAIL_X|RAIL_LABELS|RAIL_W)\s*=(?!=)')
RAIL_W_DECL = re.compile(r'\bRAIL_W\s*=\s*(\d+)\s*;')
LIT_PLAQUE = re.compile(r'"table/plaque-(\d+)-here"')
RAIL_HERE = ('WarTablePages', 'here')  # the method that draws the open page's rail plaque lit (the class plaques' 120 is not the rail)


def rail_width():
    """WarTableHome's RAIL_W, read from its source; None when it declares no number."""
    code, _ = scan((SRC / (RAIL_HOME + '.java')).read_text(encoding='utf-8'))
    m = RAIL_W_DECL.search(code)
    return int(m.group(1)) if m else None


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


def any_method_spans(code, name):
    """The body offsets of each method `name` declares, static or not (every overload), by brace matching over the blanked
    code; a call, which has no return type before it and no body after it, does not count."""
    spans = []
    head = r'(?m)^[ \t]*(?:(?:static|final|private|public|protected)\s+)*[\w<>\[\],.?]+\s+' + name + r'\s*\([^()]*\)\s*\{'
    for m in re.finditer(head, code):
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


def call_argument(code, open_paren):
    """The text between the ( at open_paren and its matching )."""
    depth = 0
    for k in range(open_paren, len(code)):
        if code[k] == '(':
            depth += 1
        elif code[k] == ')':
            depth -= 1
            if depth == 0:
                return code[open_paren + 1:k]
    return code[open_paren + 1:]


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
    for table, must in ((MUST_DRAW, True), (NEVER_DRAW, False)):
        for method, names in table.get(name, {}).items():
            spans = method_spans(code, method)
            if not spans:
                found.append(f'{path.name}: no static method {method}( to check what it draws')
            for a, b in spans:
                held = {literal.strip('"') for offset, literal in strings if a <= offset < b}
                for wanted in names:
                    if must and wanted not in held:
                        found.append(f'{path.name}: {method}( never names "{wanted}" (a state carries its shape and its words, spec 6.1)')
                    if not must and wanted in held:
                        found.append(f'{path.name}:{line_of(text, a)}: {method}( names "{wanted}", which it must never draw')
    if name in NAME_METHODS:
        for method in NAME_METHODS[name]:
            spans = any_method_spans(code, method)
            if not spans:
                found.append(f'{path.name}: no method {method}( to check the player names\' face in')
            for a, b in spans:
                for m in HEADING_CALL.finditer(code, a, b):
                    if call_argument(code, m.end() - 1).strip():  # a literal is blanked to spaces: anything left is not one
                        found.append(f'{path.name}:{line_of(text, m.start())}: heading( in {method}( takes more than fixed words; '
                                     'a player name goes in Minecraft\'s face, whose 1 is no I')
    if name != RAIL_HOME:
        for m in RAIL_DECL.finditer(code):
            found.append(f'{path.name}:{line_of(text, m.start())}: {m.group(1)} declared again; the rail is WarTableHome\'s alone')
    here = any_method_spans(code, RAIL_HERE[1]) if name == RAIL_HERE[0] else []
    if name == RAIL_HERE[0] and not here:
        found.append(f'{path.name}: no method {RAIL_HERE[1]}( to check the lit rail plaque in')
    lit = [(offset, m) for offset, literal in strings if any(a <= offset < b for a, b in here) for m in LIT_PLAQUE.finditer(literal)]
    if here and not lit:
        found.append(f'{path.name}: {RAIL_HERE[1]}( writes no "table/plaque-N-here" to check against the rail\'s width')
    if lit:
        width = rail_width()
        for offset, m in lit:
            if width is None or int(m.group(1)) != width:
                found.append(f'{path.name}:{line_of(text, offset)}: "table/plaque-{m.group(1)}-here" is not the rail\'s width '
                             f'(WarTableHome.RAIL_W {width})')
    registered = [r.start() for r in REGISTERED_KEYBINDING.finditer(code)]
    for m in BARE_KEYBINDING.finditer(code):
        if not any(a < m.start() <= a + 60 for a in registered):
            found.append(f'{path.name}:{line_of(text, m.start())}: new KeyBinding( outside KeyBindingHelper.registerKeyBinding(, '
                         'so Controls would not list it')
    if name != KEY_OWNER:
        for m in LETTER_KEY.finditer(code):
            found.append(f'{path.name}:{line_of(text, m.start())}: {m.group()} hardcodes a letter/digit key; read the bound key '
                         '(binding.matchesKey) so the player can rebind it')
    for m in BUTTON_SUBCLASS.finditer(code):
        body = body_span(code, m.group(1))
        if body is None or not FOCUS_CALL.search(code, body[0], body[1]):
            found.append(f'{path.name}:{line_of(text, m.start())}: {m.group(1)} extends ButtonWidget but never calls DarkUi.focus(; '
                         'it needs a keyboard focus ring (extend UiButton or call DarkUi.focus(g, this))')
    if name != 'DarkUi':
        lines = code.split('\n')
        for k, line in enumerate(lines):
            if 'teamColor(' in line and not any('teamMark(' in lines[j] for j in range(max(0, k - 3), min(len(lines), k + 4))):
                found.append(f'{path.name}:{k + 1}: teamColor( with no teamMark( within three lines')
    return found


def check_lang():
    """Every key the mod registers and its category have a line in en_us.json (Controls shows the line, not the id)."""
    try:
        lang = json.loads(LANG.read_text(encoding='utf-8'))
    except (OSError, ValueError) as e:
        return [f'{LANG.name}: unreadable ({e})']
    raw = (SRC / (KEY_OWNER + '.java')).read_text(encoding='utf-8')
    names = {'key.gravesandglory.' + m.group(1) for m in KEY_CALL.finditer(raw)} | {m.group(1) for m in KEY_NAME.finditer(raw)}
    found = [] if 'category.gravesandglory' in lang else [f'{LANG.name}: no "category.gravesandglory" line for the Controls category']
    if not names:
        found.append(f'{KEY_OWNER}.java: registers no key.gravesandglory.* binding to check against {LANG.name}')
    found += [f'{LANG.name}: no line for {n}' for n in sorted(names) if not str(lang.get(n, '')).strip()]
    return found


def main():
    files = sorted(SRC.glob('*.java'))
    if not files:
        print(f'test_ui_standard: no sources under {SRC}')
        return 1
    found = list(dict.fromkeys([f for path in files for f in check_file(path)] + check_lang()))  # a literal that breaks two rules is listed once
    for f in found:
        print(f)
    if found:
        print(f'test_ui_standard: {len(found)} findings')
        return 1
    print('test_ui_standard: clean')
    return 0


if __name__ == '__main__':
    sys.exit(main())
