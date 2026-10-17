from pathlib import Path
r=Path(__file__).resolve().parents[1];b=r/'src/main/java/gg/gravesandglory'
p=b/'CampaignLobbyScreen.java';s=p.read_text();s=s.replace('int tab=0;','float uiScale=1;\n    int tab=0;')
s=s.replace('hostControls.clear();key=', 'uiScale=Math.min(1,Math.min(client.getWindow().getScaledWidth()/640f,client.getWindow().getScaledHeight()/360f));width=(int)(client.getWindow().getScaledWidth()/uiScale);height=(int)(client.getWindow().getScaledHeight()/uiScale);\n        start=null;roll=null;hostControls.clear();key=')
s=s.replace('g.fill(0,0,width,height,0xe908111b);', 'g.getMatrices().push();g.getMatrices().scale(uiScale,uiScale,1);mx=(int)(mx/uiScale);my=(int)(my/uiScale);\n        g.fill(0,0,width,height,0xe908111b);')
s=s.replace('super.render(g,mx,my,delta);','super.render(g,mx,my,delta);g.getMatrices().pop();')
s=s.replace('    @Override public void tick(){','    @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked(x/uiScale,y/uiScale,button);}\n    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(x/uiScale,y/uiScale,button);}\n    @Override public void tick(){')
p.write_text(s)
p=b/'Campaign.java';s=p.read_text();s=s.replace('size=requested;practice=mode.equals("ai");balance();','size=requested;practice=mode.equals("ai");if(!practice)for(var entry:profiles.entrySet()){entry.getValue().sanitize();save(entry.getKey());}balance();')
s=s.replace('return s.bot?t.count():profile(s.owner).army.getOrDefault(t.id(),0);','return s.bot?t.count():unlocked(s,t.id())?profile(s.owner).army.getOrDefault(t.id(),0):0;')
s=s.replace('p.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,.7f,1.2f)','p.playSound(SoundEvent.of(new net.minecraft.util.Identifier("gravesandglory","unit_reveal")),.7f,1f)')
s=s.replace('p.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,.8f,1)','p.playSound(SoundEvent.of(new net.minecraft.util.Identifier("gravesandglory","victory")),.8f,1)')
# Chunk generation is cached inside the dedicated save, but never assumed from an old release.
s=s.replace('CampaignMaps.select(0);}', 'try{JsonObject marker=JsonParser.parseString(Files.readString(b.server.getSavePath(WorldSavePath.ROOT).resolve("gravesandglory-layout.json"))).getAsJsonObject();if(marker.get("layout").getAsString().equals("campaign-1"))map=marker.get("map").getAsInt();}catch(Exception ignored){}CampaignMaps.select(map);}')
s=s.replace('    void save(UUID id){','    boolean cached(){try{JsonObject j=JsonParser.parseString(Files.readString(b.server.getSavePath(WorldSavePath.ROOT).resolve("gravesandglory-layout.json"))).getAsJsonObject();return j.get("layout").getAsString().equals("campaign-1")&&j.get("map").getAsInt()==map;}catch(Exception e){return false;}}\n    void built(){try{Files.writeString(b.server.getSavePath(WorldSavePath.ROOT).resolve("gravesandglory-layout.json"),"{\\"layout\\":\\"campaign-1\\",\\"map\\":"+map+"}");}catch(Exception e){System.err.println("[Graves and Glory] Map cache marker could not be saved.");}}\n    void save(UUID id){')
p.write_text(s)
p=b/'GravesAndGlory.java';s=p.read_text().replace('CampaignMaps.lobby(world);','CampaignMaps.lobby(world);ready=campaign.cached();')
s=s.replace('Arena.finish(world);ready=true;', 'Arena.finish(world);campaign.built();ready=true;')
s=s.replace('count()>=R.maxPlayers()||running','count()>=(campaign.practice?1:campaign.size*2)||running')
s=s.replace('if(team<0)return;','if(team<0||campaign.practice&&team!=0)return;',1)
s=s.replace('if(s==null||!ready||!running||respawns', 'if(s==null||!ready||!running||time<campaign.introUntil||respawns')
p.write_text(s)
p=b/'CampaignMaps.java';s=p.read_text();s=s.replace('import net.minecraft.util.math.BlockPos;','import net.minecraft.util.math.BlockPos;\nimport net.minecraft.entity.decoration.ArmorStandEntity;\nimport net.minecraft.item.*;\nimport net.minecraft.entity.EquipmentSlot;\nimport net.minecraft.text.Text;')
needle='        for(int x=new int[]' # markers below use explicit substitution
s=s.replace('        // Open-sided recruitment hall', '''        var old=new ArrayList<net.minecraft.entity.Entity>();for(var e:w.iterateEntities())if(e.getCommandTags().contains("gravesandglory-lobby"))old.add(e);for(var e:old)e.discard();
        int display=0;for(var unit:Spec.TROOPS)if(unit.count()>0){
            int x=-15+display*6;ArmorStandEntity stand=new ArmorStandEntity(w,x,65,LOBBY_Z+6);stand.setNoGravity(true);stand.setInvulnerable(true);stand.setCustomName(Text.literal(unit.name()));stand.setCustomNameVisible(true);stand.addCommandTag("gravesandglory-lobby");
            stand.equipStack(EquipmentSlot.MAINHAND,new ItemStack(net.minecraft.registry.Registries.ITEM.get(new net.minecraft.util.Identifier(unit.weapon()))));
            stand.equipStack(EquipmentSlot.HEAD,new ItemStack(net.minecraft.registry.Registries.ITEM.get(new net.minecraft.util.Identifier(unit.helmet()))));
            stand.equipStack(EquipmentSlot.CHEST,new ItemStack(net.minecraft.registry.Registries.ITEM.get(new net.minecraft.util.Identifier(unit.chest()))));w.spawnEntity(stand);display++;
        }
        // Open-sided recruitment hall''')
p.write_text(s)
print('Refined campaign transitions, presentation, profile rules and map caching')
