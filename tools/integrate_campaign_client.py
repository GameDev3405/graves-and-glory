from pathlib import Path
import json
r=Path(__file__).resolve().parents[1];base=r/'src/main/java/gg/gravesandglory'
p=base/'GravesAndGloryClient.java';s=p.read_text().replace('new int[6]','new int[Spec.TROOPS.length]')
s=s.replace('client.setScreen(new BarracksScreen())','client.setScreen(running?new BarracksScreen():new LobbyScreen())')
s=s.replace('client.execute(()->{sightings=', 'String campaignJson=buf.readString(16000);\n            client.execute(()->{CampaignClient.accept(client,campaignJson,run);sightings=',1)
s=s.replace('ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{','ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{CampaignClient.sendProfile(client);',1)
s=s.replace('ready=false;running=false;team=-1;', 'CampaignClient.end(client);ready=false;running=false;team=-1;')
s=s.replace('worldTicks++;prepareMap(client);', 'worldTicks++;prepareMap(client);CampaignClient.tick(client);')
s=s.replace('while(menu.wasPressed()){CommandMode.cancel();', 'while(menu.wasPressed()){CampaignClient.end(client);CommandMode.cancel();')
start=s.index('    public static final class LobbyScreen');end=s.index('    public static final class CommandScreen',start)
s=s[:start]+'''    public static final class LobbyScreen extends CampaignLobbyScreen {
        LobbyScreen(){super();}
    }
'''+s[end:]
# Keep the map texture compact, but sample the complete 700x500 battlefield.
s=s.replace('Arena.mapColor(x-140,z-100)','Arena.mapColor((int)((x-140)*2.5),(int)((z-100)*2.5))')
s=s.replace('+140)*w/280','+350)*w/700').replace('+100)*h/200','+250)*h/500')
s=s.replace('(mx-x)*280/w-140','(mx-x)*700/w-350').replace('(my-y)*200/h-100','(my-y)*500/h-250')
s=s.replace('if(!running)g.drawCenteredTextWithShadow(font,"R: lobby / choose faction and start"','if(!running)g.drawCenteredTextWithShadow(font,"R: war council / collection / maps"')
s=s.replace('map(g,w-110,h-79,96,68,false);g.getMatrices().pop();','map(g,w-110,h-79,96,68,false);CampaignClient.hud(g,w,h);g.getMatrices().pop();')
s=s.replace('    static void prepareMap(MinecraftClient c){','    static void invalidateMap(MinecraftClient c){if(mapTexture!=null){c.getTextureManager().destroyTexture(mapTexture);mapTexture=null;}preparingMap=false;}\n    static void prepareMap(MinecraftClient c){')
# Do not leak old map textures if a selection changes during preparation.
s=s.replace('if(preparingMap)return;preparingMap=true;','if(preparingMap)return;preparingMap=true;final int generation=CampaignMaps.selected;')
s=s.replace('image->c.execute(()->mapTexture=c.getTextureManager().registerDynamicTexture("gravesandglory_map",new NativeImageBackedTexture(image)))','image->c.execute(()->{if(generation!=CampaignMaps.selected){image.close();preparingMap=false;return;}mapTexture=c.getTextureManager().registerDynamicTexture("gravesandglory_map",new NativeImageBackedTexture(image));})')
# Supply purchasing is restricted to unlocked types, including UI explanations.
s=s.replace('c.train().active=nearBarracks&&gold>=t.cost()&&count+queued<20;', 'c.train().active=nearBarracks&&gold>=t.cost()&&count+queued<20&&(CampaignClient.profile.unlocked.contains(t.id())||CampaignClient.mode.equals("ai"));')
# Six types per faction now use compact 22px rows instead of three 42px rows.
s=s.replace('row*42','row*22').replace('int py=y+69+', 'int py=y+69+').replace('int py=y+60+', 'int py=y+60+')
s=s.replace('x+10,py,118','x+10,py+2,118').replace('x+142,py,110','x+142,py+2,110')
# Barracks names and buttons need different rows; expand screen height and rows.
s=s.replace('y=height/2-108','y=height/2-130').replace('row*22','row*27')
s=s.replace('y+151','y+192').replace('y+187','y+230').replace('400,216','400,256').replace('y+181','y+224').replace('y+194','y+236')
# Unit text above each button, compact status on the same line.
p.write_text(s)
p=r/'src/main/resources/gravesandglory.mixins.json';j=json.loads(p.read_text());j['client'].append('BipedAnimationMixin');p.write_text(json.dumps(j,indent=2))
p=base/'Campaign.java';s=p.read_text().replace('portable.matches>stored.matches','portable.matches>=stored.matches');p.write_text(s)
print('Client campaign integrated')
