package thaumcraft.port.world;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Portal encounter combat. Crimson equipment/research are separate, unfinished systems. */
public final class Cultist extends Monster implements RangedAttackMob {
    public Cultist(EntityType<? extends Cultist> type,Level l){super(type,l);xpReward=10;}
    public boolean cleric(){return getType()==WorldEntities.CLERIC.get();}
    public static AttributeSupplier.Builder attributes(boolean cleric){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,cleric?24:30).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.FOLLOW_RANGE,32).add(Attributes.MOVEMENT_SPEED,.3);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        if(cleric())goalSelector.addGoal(2,new RangedAttackGoal(this,1,20,40,24));
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1,false));goalSelector.addGoal(5,new OpenDoorGoal(this,true));
        goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,.8));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(4,new NearestAttackableTargetGoal<>(this,AbstractIllager.class,true));
    }
    @Override public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor l,DifficultyInstance d,MobSpawnType reason,SpawnGroupData data){
        var result=super.finalizeSpawn(l,d,reason,data);
        if(!cleric()){setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));setDropChance(EquipmentSlot.MAINHAND,.05f);}
        if(getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav)nav.setCanOpenDoors(true);
        return result;
    }
    @Override public void performRangedAttack(LivingEntity target,float power){
        if(random.nextFloat()>.66){
            var orb=WorldEntities.ORB.get().create(level());if(orb==null)return;orb.setOwner(this);orb.target(target);
            orb.setPos(getEyePosition());orb.setDeltaMovement(target.getEyePosition().subtract(getEyePosition()).normalize().scale(.66));level().addFreshEntity(orb);
        }else for(int i=0;i<3;i++){
            var delta=target.getEyePosition().subtract(getEyePosition()).add(random.nextGaussian()*Math.sqrt(power)*.5,0,random.nextGaussian()*Math.sqrt(power)*.5);
            var fireball=new SmallFireball(level(),this,delta.normalize());fireball.setPos(getX(),getY()+getBbHeight()*.5+.5,getZ());level().addFreshEntity(fireball);
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }
}
