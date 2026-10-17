from pathlib import Path
import json
r=Path(__file__).resolve().parents[1]
p=r/'src/main/resources/gravesandglory.mixins.json';d=json.loads(p.read_text());d['client'].append('EntranceOverlayMixin');p.write_text(json.dumps(d,indent=2))
p=r/'src/main/java/gg/gravesandglory/CampaignMaps.java';s=p.read_text();s=s.replace('Spec.STRUCTURES=structures.toArray', '''if(selected==1){
            structures.removeIf(st->st.kind().equals("farm")||st.kind().equals("mill")||st.kind().equals("shrine"));
            structures.add(s("supply_barn","house",-105,-160,24,18,11,"wood",64));
            structures.add(s("garrison","barracks",65,135,30,22,13,"stone",64));
            for(int side:new int[]{-1,1})for(int z:new int[]{-62,62})structures.add(s("curtain_"+side+"_"+z,"breach",side*72,z,5,68,13,"stone",64));
        }else if(selected==2){
            structures.removeIf(st->st.kind().equals("farm")||st.kind().equals("mill")||st.kind().equals("market"));
            structures.add(s("caravan_lodge","house",-80,-178,26,18,12,"wood",64));
            structures.add(s("pass_refuge","house",62,34,20,16,12,"wood",64));
            structures.add(s("signal_tower","watchtower",57,166,14,14,19,"stone",64));
            road(-80,-178,-20,-155,4);road(62,34,25,0,4);
        }else if(selected==3){
            structures.removeIf(st->st.kind().equals("farm")||st.kind().equals("mill"));
            structures.add(s("sawmill","house",-85,-176,24,18,10,"wood",64));
            structures.add(s("broken_chapel","ruin",70,100,28,22,12,"dusk",64));
            road(-85,-176,-35,-145,4);road(70,100,40,145,3);
        }
        Spec.STRUCTURES=structures.toArray''');p.write_text(s)
p=r/'src/main/java/gg/gravesandglory/Arena.java';s=p.read_text().replace('    public static void finish(ServerWorld w){','    public static void finish(ServerWorld w){\n        w.setTimeOfDay(CampaignMaps.selected==3?11500:CampaignMaps.selected==2?6000:3000);');s=s.replace('        for(int x=-260;', '''        if(CampaignMaps.selected==3)for(int row=0;row<3;row++)box(w,-90+row*3,65,-164,2,2,8,Blocks.SPRUCE_LOG);
        if(CampaignMaps.selected==1)for(int row=0;row<4;row++)box(w,-109+row*4,65,-152,3,2,3,Blocks.HAY_BLOCK);
        for(int x=-260;''');p.write_text(s)
# Bump the cache whenever the authored layout changes.
p=r/'src/main/java/gg/gravesandglory/Campaign.java';p.write_text(p.read_text().replace('campaign-1','campaign-2'))
print('Distinct fortifications, winter refuges and logging settlement authored')
