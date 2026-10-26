"""Readable names must survive cluster renumbering without altering graph structure."""
import copy, json, pathlib, sys, tempfile, unittest
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[2]/'tools'))
from graph_labels import names_for, apply_labels

class GraphLabelsCheck(unittest.TestCase):
    def setUp(self):
        self.graph={'nodes':[{'id':'hud','community':4,'source_file':'BattleHud.java'},
                             {'id':'hud_draw','community':4,'source_file':'BattleHud.java'},
                             {'id':'recruit','community':8,'source_file':'RecruitPages.java'}],
                    'links':[{'source':'hud','target':'recruit'}]}
        self.records=[{'name':'Battle HUD','anchors':['hud','hud_draw']},
                      {'name':'Recruitment','anchors':['recruit']}]

    def test_names_follow_symbols_when_cluster_ids_change(self):
        graph=copy.deepcopy(self.graph)
        for node in graph['nodes']: node['community']+=100
        self.assertEqual(names_for(graph,self.records),{104:'Battle HUD',108:'Recruitment'})

    def test_labels_do_not_change_members_or_edges(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=pathlib.Path(tmp); (root/'graph.json').write_text(json.dumps(self.graph))
            records=root/'labels.json'; records.write_text(json.dumps({'labels':self.records}))
            apply_labels(root,records)
            actual=json.loads((root/'graph.json').read_text())
            self.assertEqual(actual['links'],self.graph['links'])
            self.assertEqual([(n['id'],n['community']) for n in actual['nodes']],
                             [(n['id'],n['community']) for n in self.graph['nodes']])
            self.assertEqual(actual['nodes'][0]['community_name'],'Battle HUD')

if __name__=='__main__': unittest.main()
