package thaumcraft.port.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.warp.entity.EldritchMob;
import thaumcraft.port.world.Cultist;

/** The armoured host disappears immediately on death, releasing exactly one shelled crab. */
public final class InhabitedZombie extends Zombie implements EldritchMob {
    private boolean crabReleased;
    public InhabitedZombie(EntityType<? extends InhabitedZombie> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return Zombie.createAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0);}
    @Override protected void registerGoals(){super.registerGoals();targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Cultist.class,true));}
    @Override protected boolean convertsInWater(){return false;}
    @Override protected void randomizeReinforcementsChance(){getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0);}
    @Override public boolean killedEntity(ServerLevel level,LivingEntity victim){return true;}
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group){
        float chance=level.getDifficulty()==Difficulty.HARD?.9f:.6f;
        setItemSlot(EquipmentSlot.HEAD,new ItemStack(MobArmor.HELM.get()));
        if(random.nextFloat()<=chance)setItemSlot(EquipmentSlot.CHEST,new ItemStack(MobArmor.CHEST.get()));
        if(random.nextFloat()<=chance)setItemSlot(EquipmentSlot.LEGS,new ItemStack(MobArmor.LEGS.get()));
        return super.finalizeSpawn(level,difficulty,reason,group);
    }
    public boolean hasHeadArmor(){return getItemBySlot(EquipmentSlot.HEAD).is(MobArmor.HELM.get());}
    public boolean hasChestArmor(){return getItemBySlot(EquipmentSlot.CHEST).is(MobArmor.CHEST.get());}
    public boolean hasLegArmor(){return getItemBySlot(EquipmentSlot.LEGS).is(MobArmor.LEGS.get());}
    // BETA26 skips the normal death hook entirely: no brain roll, equipment drops, or villager conversion.
    @Override public void die(DamageSource source){}
    @Override protected void tickDeath(){
        if(crabReleased)return;crabReleased=true;
        if(level() instanceof ServerLevel server){
            var crab=MobContent.ELDRITCH_CRAB.get().create(server);
            if(crab!=null){crab.moveTo(getX(),getEyeY(),getZ(),getYRot(),getXRot());crab.setHelm(true);server.addFreshEntity(crab);}
            dropExperience();server.sendParticles(ParticleTypes.POOF,getX(),getY()+getBbHeight()/2,getZ(),20,getBbWidth(),getBbHeight()/2,getBbWidth(),.02);discard();
        }
    }
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason){return level().getEntitiesOfClass(InhabitedZombie.class,new AABB(getX(),getY(),getZ(),getX()+1,getY()+1,getZ()+1).inflate(32,16,32),e->e!=this).isEmpty()&&super.checkSpawnRules(level,reason);}
    @Override protected SoundEvent getAmbientSound(){return PortSounds.get("crabtalk");}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.GENERIC_HURT;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("CrabReleased",crabReleased);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);crabReleased=tag.getBoolean("CrabReleased");}
}
