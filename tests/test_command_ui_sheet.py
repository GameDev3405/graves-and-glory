"""Command UI sheet rows (Task 1): order, formation, group, charge and panel numbers read by the generated Spec.java."""
import json
import pathlib
import subprocess
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent


def sheet():
    return json.loads((ROOT / 'sheets' / 'design.json').read_text(encoding='utf-8'))


class CommandUiSheet(unittest.TestCase):
    def test_command_ui_rows(self):
        data = sheet()
        orders = {o['id']: o for o in data['orders']}
        self.assertEqual(orders['follow']['label'], 'Follow me')
        self.assertIs(orders['attack']['hunt'], True)
        for oid in ('follow', 'move', 'hold', 'charge', 'retreat'):
            self.assertIs(orders[oid]['hunt'], False, oid)
        self.assertIs(orders['move']['playerFacing'], False)
        self.assertIs(orders['hold']['playerFacing'], False)
        self.assertEqual([o['id'] for o in data['orders'] if o['playerFacing']], ['follow', 'attack', 'charge', 'retreat'])
        self.assertEqual({k: orders[k]['hotkey'] for k in ('attack', 'charge', 'follow', 'retreat')},
                         {'attack': 'Z', 'charge': 'X', 'follow': 'C', 'retreat': 'V'})

        formations = {f['id']: f for f in data['formations']}
        self.assertEqual([f['id'] for f in data['formations']], ['block', 'line', 'wedge', 'loose'])
        self.assertEqual(formations['block']['label'], 'Shield wall')
        self.assertEqual((formations['block']['moveMultiplier'], formations['block']['arrowBlock'], formations['block']['shove']), (0.85, 0.75, 0))
        self.assertEqual(formations['wedge']['shove'], 0.6)
        self.assertEqual(formations['loose']['spacing'], 3.2)
        for fid in ('line', 'wedge', 'loose'):
            self.assertEqual(formations[fid]['moveMultiplier'], 1.0, fid)
            self.assertEqual(formations[fid]['arrowBlock'], 0, fid)
        for fid in ('block', 'line', 'loose'):
            self.assertEqual(formations[fid]['shove'], 0, fid)

        groups = {g['id']: g for g in data['groups']}
        self.assertEqual(groups['ranged']['label'], 'Archers')
        self.assertEqual(groups['cavalry']['label'], 'Riders')
        self.assertEqual({k: g['pill'] for k, g in groups.items()},
                         {'infantry': 1, 'ranged': 2, 'cavalry': 3, 'all': 4, 'support': 5, 'siege': 6})

        cd = data['combatDiscipline'][0]
        self.assertEqual({k: cd[k] for k in ('chargeCooldownTicks', 'chargeRunBlocks', 'chargeBonusMultiplier', 'chargeKnockback',
                                             'outlineShareTicks', 'outlineLitTicks', 'retreatArriveRadius')},
                         {'chargeCooldownTicks': 400, 'chargeRunBlocks': 32, 'chargeBonusMultiplier': 1.3, 'chargeKnockback': 0.8,
                          'outlineShareTicks': 200, 'outlineLitTicks': 20, 'retreatArriveRadius': 4.0})

        ci = data['commandInterface'][0]
        self.assertEqual(ci['pennantAlpha'], 0.55)
        self.assertEqual(ci['panelSlideTicks'], 6)
        self.assertEqual(ci['orders'], 'attack,charge,follow,retreat,hold')

        result = subprocess.run([sys.executable, 'tools/preflight.py'], cwd=ROOT, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)


if __name__ == '__main__':
    unittest.main()
