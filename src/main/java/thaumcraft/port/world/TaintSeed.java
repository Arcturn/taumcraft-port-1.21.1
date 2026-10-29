package thaumcraft.port.world;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.aspect.Aspect;
public final class TaintSeed extends Monster {
    public int boost;
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s){return thaumcraft.port.content.PortSounds.get("tentacle");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return thaumcraft.port.content.PortSounds.get("tentacle");}
    public TaintSeed(EntityType<? extends TaintSeed> t,Level l){super(t,l);xpReward=8;setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes(boolean prime){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,prime?150:75).add(Attributes.ATTACK_DAMAGE,prime?7:4).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.KNOCKBACK_RESISTANCE,1);}
    public boolean prime(){return getType()==WorldEntities.PRIME.get();}
    @Override protected void registerGoals(){goalSelector.addGoal(1,new MeleeAttackGoal(this,1,false));targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel l)||!isAlive())return;
        int area=prime()?2:1;
        if(WorldConfig.TAINT.get()&&(boost>0||tickCount%20==0)){
            var aura=AuraSystem.get(l,blockPosition());float mod=boost>0?1:aura==null||aura.base()==0?0:aura.flux()/aura.base();
            if(boost>0)boost--;
            if(mod<=0){hurt(damageSources().starve(),.5f);AuraSystem.add(l,blockPosition(),.1f,true);}
            else TaintSystem.spread(l,blockPosition().offset(random.nextInt(area*6+1)-area*3,random.nextInt(area*2+1)-area,random.nextInt(area*6+1)-area*3),true);
        }
        if(tickCount%20==0)for(var e:l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(area*4),e->e!=this))e.addEffect(new MobEffectInstance(WorldEntities.TAINT,100,area-1));
    }
    @Override protected void dropCustomDeathLoot(ServerLevel l,net.minecraft.world.damagesource.DamageSource d,boolean hit){super.dropCustomDeathLoot(l,d,hit);int n=prime()?1+random.nextInt(3):1;for(int i=0;i<n;i++)spawnAtLocation(CrystalItem.stack(Aspect.FLUX));}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putInt("boost",boost);}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);boost=Math.max(0,Math.min(10000,t.getInt("boost")));}
}
