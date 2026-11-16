"""tools/ui_pieces.py (UI editor phase 1): reads UiText calls, refuses duplicate ids and unsupplied slots (design 5.9)."""
import pathlib
import sys
import tempfile
import unittest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[2] / 'tools'))
import ui_pieces  # noqa: E402


def scan(java):
    with tempfile.TemporaryDirectory() as d:
        (pathlib.Path(d) / 'A.java').write_text(java, encoding='utf8')
        return ui_pieces.scan(pathlib.Path(d))


class UiPiecesTest(unittest.TestCase):
    def test_reads_pieces_and_slots(self):
        pieces, findings = scan('class A{void f(){g.draw(UiText.t("hud.wave", "Wave {wave} of {total}", "wave", w(1,2), "total", n));'
                                'x(UiText.t("camp.title", "Camp \\"one\\""));}}')
        self.assertEqual(findings, [])
        by = {p['id']: p for p in pieces}
        self.assertEqual(by['hud.wave']['slots'], ['wave', 'total'])
        self.assertEqual(by['camp.title']['default'], 'Camp "one"')

    def test_duplicate_id_fails(self):
        _, findings = scan('class A{void f(){UiText.t("a.b","x");UiText.t("a.b","y");}}')
        self.assertTrue(any('already declared' in f for f in findings))

    def test_unsupplied_slot_fails(self):
        _, findings = scan('class A{void f(){UiText.t("a.b","Wave {wave}");}}')
        self.assertTrue(any('does not supply' in f for f in findings))

    def test_non_literal_default_fails(self):
        _, findings = scan('class A{void f(){UiText.t("a.b","Wave "+n);}}')
        self.assertTrue(any('one string literal' in f for f in findings))

    def test_key_hint_and_braces(self):
        pieces, findings = scan('class A{void f(){UiText.t("a.b","Press {key:menu} {{x}}");}}')
        self.assertEqual(findings, [])
        self.assertTrue(pieces[0]['keys'])

    def test_lone_brace_fails(self):
        _, findings = scan('class A{void f(){UiText.t("a.b","50% {");}}')
        self.assertTrue(any('opens no slot' in f for f in findings))

    def test_comments_ignored_and_locked(self):
        pieces, findings = scan('class A{/* UiText.t("x.y","z") */ void f(){UiText.locked("a.name", name, "the design sheet");}}')
        self.assertEqual(findings, [])
        self.assertEqual([p['id'] for p in pieces], ['a.name'])
        self.assertTrue(pieces[0]['locked'])

    def test_edit_problem(self):
        piece = {'id': 'a.b', 'default': 'W {n}', 'slots': ['n'], 'keys': False}
        self.assertIsNone(ui_pieces.edit_problem(piece, 'Round {n}'))
        self.assertIn('not a live value', ui_pieces.edit_problem(piece, 'Round {m}'))
        self.assertIn('orphan', ui_pieces.edit_problem(None, 'x'))

    def test_repository_is_clean(self):
        pieces, findings = ui_pieces.scan()
        self.assertEqual(findings + ui_pieces.check_resources(pieces), [])


if __name__ == '__main__':
    unittest.main()
