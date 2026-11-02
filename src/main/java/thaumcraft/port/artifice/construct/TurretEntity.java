package thaumcraft.port.artifice.construct;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.crafting.content.CraftingContent;

/** TC6 arrow magazine, dispenser feed, source firing intervals and target selectors. */
public final class TurretEntity extends OwnedConstruct implements RangedAttackMob {
    private static final EntityDataAccessor<Byte> FLAGS=SynchedEntityData.defineId(TurretEntity.class,EntityDataSerializers.BYTE);
    private final boolean advanced;
    private int loadTicks,shotTicks;
    private boolean loading,shooting;
    private float previousLoad,load,previousShot,shot;
    public TurretEntity(EntityType<? extends TurretEntity> type,Level level,boolean advanced){
        super(type,level);this.advanced=advanced;
        goalSelector.addGoal(1,new RangedAttackGoal(this,0,20,advanced?40:60,24));
        goalSelector.addGoal(2,new WatchTargetGoal());
        targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new FindTargetGoal());
    }
    public static AttributeSupplier.Builder attributes(boolean advanced){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,advanced?40:30).add(Attributes.FOLLOW_RANGE,24).add(Attributes.ARMOR,advanced?8:2);}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(FLAGS,(byte)2);}
    @Override protected net.minecraft.network.chat.Component getTypeName(){return net.minecraft.network.chat.Component.translatable(advanced?"entity.TurretAdvanced.name":"entity.TurretBasic.name");}
    public boolean isAdvanced(){return advanced;}
    public boolean targetFlag(int bit){return (entityData.get(FLAGS)&1<<bit)!=0;}
    public void setTargetFlag(int bit,boolean value){if(bit<0||bit>3)return;int flags=entityData.get(FLAGS);entityData.set(FLAGS,(byte)(value?flags|1<<bit:flags&~(1<<bit)));setTarget(null);}
    @Override public boolean accepts(ItemStack stack){return stack.getItem() instanceof ArrowItem;}
    @Override public String placerId(){return advanced?"turret_placer_advanced":"turret_placer_basic";}
    @Override public void performRangedAttack(LivingEntity target,float range){
        if(level().isClientSide||!accepts(getMainHandItem())||getMainHandItem().isEmpty())return;
        var arrow=new Arrow(level(),this,getMainHandItem().copyWithCount(1),null);
        arrow.setBaseDamage(2.25+range*2+random.nextGaussian()*.25);
        Vec3 offset=getLookAngle().scale(isPassenger()?1.75:-.9f);arrow.setPos(arrow.position().add(offset));arrow.pickup=AbstractArrow.Pickup.DISALLOWED;
        arrow.shoot(target.getX()-getX(),target.getY()+target.getEyeHeight()+range*range*3-arrow.getY(),target.getZ()-getZ(),2,2);
        level().addFreshEntity(arrow);level().broadcastEntityEvent(this,(byte)16);playSound(SoundEvents.ARROW_SHOOT,1,1/(random.nextFloat()*.4f+.8f));getMainHandItem().shrink(1);
        if(getMainHandItem().isEmpty())setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
    }
    public boolean reloadFromDispenser(){
        if(level().isClientSide||!getMainHandItem().isEmpty())return false;
        BlockPos below=blockPosition().below();var state=level().getBlockState(below);
        if(!(level().getBlockEntity(below) instanceof DispenserBlockEntity dispenser)||!state.hasProperty(DispenserBlock.FACING)||state.getValue(DispenserBlock.FACING)!=net.minecraft.core.Direction.UP)return false;
        for(int slot=0;slot<dispenser.getContainerSize();slot++)if(accepts(dispenser.getItem(slot))&&!dispenser.getItem(slot).isEmpty()){
            setItemInHand(InteractionHand.MAIN_HAND,dispenser.removeItem(slot,dispenser.getItem(slot).getCount()));dispenser.setChanged();playSound(PortSounds.get("ticks"),1,1);level().broadcastEntityEvent(this,(byte)17);return true;
        }
        return false;
    }
    @Override public void tick(){
        if(!level().isClientSide)reloadFromDispenser();previousLoad=load;previousShot=shot;
        super.tick();
        if(level().isClientSide){if(loading){if(++loadTicks>=10){loadTicks=0;loading=false;}}else loadTicks=0;if(shooting){if(++shotTicks>=6){shotTicks=0;shooting=false;}}else shotTicks=0;load=loadTicks/10f;shot=shotTicks/6f;}
        else{
            setYRot(yHeadRot);if(tickCount%80==0)heal(1);
            var pos=blockPosition();if(BaseRailBlock.isRail(level().getBlockState(pos.below())))pos=pos.below();var state=level().getBlockState(pos);
            if(state.is(CraftingContent.ACTIVATOR_RAIL.get()))setNoAi(state.getValue(PoweredRailBlock.POWERED));
            if(getTarget()!=null&&(!getTarget().isAlive()||isAlliedTo(getTarget())||(advanced&&getTarget() instanceof Player&&level().getServer()!=null&&!level().getServer().isPvpAllowed()&&!isOwner(getTarget()))))setTarget(null);
        }
    }
    @Override public void handleEntityEvent(byte event){if(event==16){if(!shooting){shotTicks=-1;shooting=true;}}else if(event==17){if(!loading){loadTicks=-1;loading=true;}}else super.handleEntityEvent(event);}
    public float loadProgress(float partial){float delta=load-previousLoad;if(delta<0)delta++;return previousLoad+delta*partial;}
    public float shotProgress(float partial){float delta=shot-previousShot;if(delta<0)delta++;return previousShot+delta*partial;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putByte("targets",entityData.get(FLAGS));}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(tag.contains("targets"))entityData.set(FLAGS,tag.getByte("targets"));}
    @Override public void move(MoverType type,Vec3 movement){super.move(type,movement.multiply(advanced?1.0/300:1.0/20,1,advanced?1.0/300:1.0/20));}
    @Override public boolean hurt(DamageSource source,float amount){setYRot(getYRot()+(float)random.nextGaussian()*45);setXRot(getXRot()+(float)random.nextGaussian()*20);return super.hurt(source,amount);}
    @Override public int getMaxHeadXRot(){return 20;}
    /** Source's "friendly" option selects same-team/owned candidates rather than indiscriminate friendly fire. */
    public boolean validTarget(LivingEntity target,boolean requireSight){
        if(target==this||!target.isAlive()||target.isSpectator()||target instanceof Player p&&p.isCreative())return false;
        if(!advanced){if(!(target instanceof Enemy)||isAlliedTo(target))return false;}
        else{
            boolean category=target instanceof Enemy?targetFlag(1):target instanceof Player?targetFlag(2):(target instanceof Animal||target instanceof net.minecraft.world.entity.animal.AbstractGolem||target instanceof net.minecraft.world.entity.animal.WaterAnimal||target instanceof net.minecraft.world.entity.ambient.AmbientCreature)&&targetFlag(0);if(!category)return false;
            if(target instanceof Player&&level().getServer()!=null&&!level().getServer().isPvpAllowed()&&!targetFlag(3)){setTargetFlag(2,false);return false;}
            var team=getTeam();if(team!=null&&((target.getTeam()==team)!=targetFlag(3)))return false;
            if(getOwnerUUID()!=null){
                if(target instanceof OwnableEntity ownable){boolean same=Objects.equals(getOwnerUUID(),ownable.getOwnerUUID());if(same!=targetFlag(3))return false;}
                else if(!(target instanceof Player)&&targetFlag(3))return false;
                if(isOwner(target)&&!targetFlag(3))return false;
            }
        }
        double range=24;if(target instanceof Player player){if(player.isCrouching())range*=.8;if(player.isInvisible())range*=.7*Math.max(.1,player.getArmorCoverPercentage());}
        return distanceToSqr(target)<=range*range&&(!requireSight||getSensing().hasLineOfSight(target));
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean hit){
        super.dropCustomDeathLoot(level,source,hit);int looting=looting(source);float bonus=looting*.15f;
        if(random.nextFloat()<.2f+bonus)spawnAtLocation(item(advanced?"mind_biothaumic":"mind_clockwork"),.5f);
        if(random.nextFloat()<.5f+bonus)spawnAtLocation(item("mechanism_simple"),.5f);
        for(int i=0;i<2;i++)if(random.nextFloat()<.5f+bonus)spawnAtLocation(item("plank_greatwood"),.5f);
        if(advanced){if(random.nextFloat()<.3f+bonus)spawnAtLocation(item("plate_brass"),.5f);for(int i=0;i<2;i++)if(random.nextFloat()<.4f+bonus)spawnAtLocation(item("plate_iron"),.5f);}
    }
    private final class FindTargetGoal extends Goal {
        private LivingEntity next;private int unseen;
        FindTargetGoal(){setFlags(EnumSet.of(Flag.TARGET));}
        @Override public boolean canUse(){if(random.nextInt(5)!=0)return false;next=level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(24,4,24),e->validTarget(e,true)).stream().min(Comparator.comparingDouble(TurretEntity.this::distanceToSqr)).orElse(null);return next!=null;}
        @Override public void start(){setTarget(next);unseen=0;}
        @Override public boolean canContinueToUse(){var target=getTarget();if(target==null||!validTarget(target,false))return false;if(getSensing().hasLineOfSight(target))unseen=0;return ++unseen<=60;}
        @Override public void stop(){setTarget(null);next=null;}
    }
    private final class WatchTargetGoal extends Goal {
        WatchTargetGoal(){setFlags(EnumSet.of(Flag.LOOK));}
        @Override public boolean canUse(){return getTarget()!=null;}
        @Override public void tick(){var target=getTarget();if(target!=null)getLookControl().setLookAt(target.getX(),target.getEyeY(),target.getZ(),10,getMaxHeadXRot());}
    }
}
