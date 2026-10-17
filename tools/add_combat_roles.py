import json
from pathlib import Path
r=Path(__file__).resolve().parents[1];p=r/'sheets/design.json';d=json.loads(p.read_text())
for t in d['troops']:t['ability']='mend' if t['id']=='mage' else 'blight' if t['id']=='hexer' else 'shield' if t['id']=='knight' else 'none'
c=d['combatDiscipline'][0];c.pop('shieldFrontalScale');c.pop('shieldAngleCos');c.update(healAmount=4.0,healRadius=10.0,abilityCooldownTicks=100)
p.write_text(json.dumps(d,indent=2)+'\n')
p=r/'src/main/java/gg/gravesandglory/GravesAndGlory.java';s=p.read_text()
s=s.replace('return battle.allowDamage(entity,source.getAttacker(),amount);','return battle.allowDamage(entity,source,amount);')
s=s.replace('long nextAttack;', 'long nextAttack;long abilityAt,windupUntil;Vec3d spellCenter;')
s=s.replace('        boolean allowDamage(LivingEntity entity,Entity attacker,float amount){','''        boolean scalingDamage;
        boolean allowDamage(LivingEntity entity,net.minecraft.entity.damage.DamageSource source,float amount){
            Entity attacker=source.getAttacker();''')
s=s.replace('            if(entity instanceof ServerPlayerEntity p&&p.getHealth()<=amount){','''            if(!scalingDamage&&attacker instanceof ServerPlayerEntity&&!(entity instanceof ServerPlayerEntity)){
                scalingDamage=true;try{entity.damage(source,(float)(amount*Spec.COMBATDISCIPLINE[0].damageScale()));}finally{scalingDamage=false;}return false;
            }
            if(entity instanceof ServerPlayerEntity p&&p.getHealth()<=amount){''')
s=s.replace('mob.equipStack(EquipmentSlot.MAINHAND,','if(spec.ability().equals("shield"))mob.equipStack(EquipmentSlot.OFFHAND,unbreakable(Items.SHIELD));\n            mob.equipStack(EquipmentSlot.MAINHAND,')
p.write_text(s)
p=r/'src/main/java/gg/gravesandglory/Tactics.java';s=p.read_text().replace('u.lockUntil=time+40','u.lockUntil=time+Spec.COMBATDISCIPLINE[0].targetMemoryTicks()')
needle='        if(enemy!=null){\n            mob.getLookControl()'
s=s.replace(needle,'''        boolean shield=u.spec.ability().equals("shield")&&a.formation.equals("block")&&!a.order.equals("charge")&&!a.order.equals("retreat")&&enemy!=null&&nearest<36;
        if(shield&&!mob.isUsingItem())mob.setCurrentHand(Hand.OFF_HAND);else if(!shield&&mob.isUsingItem())mob.stopUsingItem();
        if(u.spec.ability().equals("mend")&&time>=u.abilityAt){
            var ally=battle.units.stream().filter(v->v.squad.team==s.team&&v.mob.isAlive()&&v.mob.getHealth()<v.mob.getMaxHealth()&&v.mob.squaredDistanceTo(mob)<Spec.COMBATDISCIPLINE[0].healRadius()*Spec.COMBATDISCIPLINE[0].healRadius()&&mob.canSee(v.mob)).min(java.util.Comparator.comparingDouble(v->v.mob.getHealth()/v.mob.getMaxHealth())).orElse(null);
            if(ally!=null){ally.mob.heal((float)Spec.COMBATDISCIPLINE[0].healAmount());u.abilityAt=time+Spec.COMBATDISCIPLINE[0].abilityCooldownTicks();battle.world.spawnParticles(ParticleTypes.HEART,ally.mob.getX(),ally.mob.getY()+2,ally.mob.getZ(),3,.3,.2,.3,0);}
        }
        if(u.spellCenter!=null){
            double radius=Spec.COMBATDISCIPLINE[0].spellRadius();for(int i=0;i<12;i++){double angle=i*Math.PI/6;battle.world.spawnParticles(ParticleTypes.WITCH,u.spellCenter.x+Math.cos(angle)*radius,u.spellCenter.y+.2,u.spellCenter.z+Math.sin(angle)*radius,1,0,0,0,0);}
            if(time>=u.windupUntil){for(var v:battle.units)if(v.squad.team!=s.team&&v.mob.isAlive()&&v.mob.squaredDistanceTo(u.spellCenter)<radius*radius){v.mob.damage(battle.world.getDamageSources().mobAttack(mob),u.spec.damage());v.mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS,40,0));}u.spellCenter=null;}
        }
        if(enemy!=null){
            mob.getLookControl()''')
s=s.replace('enemy.damage(battle.world.getDamageSources().mobAttack(mob),u.spec.damage());','''if(u.spec.ability().equals("blight")){if(time>=u.abilityAt){u.spellCenter=enemy.getPos();u.windupUntil=time+Spec.COMBATDISCIPLINE[0].spellWindupTicks();u.abilityAt=time+Spec.COMBATDISCIPLINE[0].abilityCooldownTicks();}}
                    else if(!u.spec.ability().equals("mend"))enemy.damage(battle.world.getDamageSources().mobAttack(mob),u.spec.damage());''')
p.write_text(s)
