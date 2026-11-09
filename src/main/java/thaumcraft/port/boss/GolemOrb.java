package thaumcraft.port.boss;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.port.content.PortSounds;

/** The golem/leader's slow homing orb. It can be batted aside but retains its original target. */
public final class GolemOrb extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> TARGET=SynchedEntityData.defineId(GolemOrb.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RED=SynchedEntityData.defineId(GolemOrb.class,EntityDataSerializers.BOOLEAN);
    private UUID targetUuid;
    public GolemOrb(EntityType<? extends GolemOrb> type,Level level){super(type,level);setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder data){data.define(TARGET,-1);data.define(RED,false);}
    public void setTarget(LivingEntity target){targetUuid=target==null?null:target.getUUID();entityData.set(TARGET,target==null?-1:target.getId());}
    public LivingEntity getTarget(){
        var entity=level().getEntity(entityData.get(TARGET));
        if(entity instanceof LivingEntity living&&living.isAlive())return living;
        if(level() instanceof ServerLevel server&&targetUuid!=null&&server.getEntity(targetUuid) instanceof LivingEntity living&&living.isAlive()){
            entityData.set(TARGET,living.getId());return living;
        }
        return null;
    }
    public boolean isRed(){return entityData.get(RED);}
    public void setRed(boolean value){entityData.set(RED,value);}
    @Override protected double getDefaultGravity(){return 0;}
    @Override public void tick(){
        super.tick();if(isRemoved())return;
        if(tickCount>(isRed()?240:160)){if(!level().isClientSide)discard();return;}
        var target=getTarget();
        if(target!=null){
            // BETA26 intentionally divides by squared distance, making close turns much sharper.
            double squared=Math.max(.0001,distanceToSqr(target));
            var delta=target.position().add(0,target.getBbHeight()*.6,0).subtract(position()).scale(.2/squared).add(getDeltaMovement());
            setDeltaMovement(Mth.clamp(delta.x,-.25,.25),Mth.clamp(delta.y,-.25,.25),Mth.clamp(delta.z,-.25,.25));
        }
        if(level().isClientSide)BossVisuals.orbTrail.accept(this);
    }
    @Override protected void onHit(HitResult hit){
        super.onHit(hit);if(level().isClientSide)return;
        if(hit instanceof EntityHitResult entityHit&&getOwner() instanceof LivingEntity owner){
            entityHit.getEntity().hurt(damageSources().indirectMagic(this,owner),(float)owner.getAttributeValue(Attributes.ATTACK_DAMAGE)*(isRed()?1:.6f));
        }
        playSound(PortSounds.get("shock"),1,1+(random.nextFloat()-random.nextFloat())*.2f);
        level().broadcastEntityEvent(this,(byte)3);discard();
    }
    @Override public void handleEntityEvent(byte event){if(event==3)BossVisuals.impact.accept(this);else super.handleEntityEvent(event);}
    @Override public boolean isPickable(){return isAlive();}
    @Override public boolean hurt(DamageSource source,float damage){
        if(isInvulnerableTo(source)||source.getEntity()==null)return false;
        setDeltaMovement(source.getEntity().getLookAngle().scale(.9));hasImpulse=true;
        playSound(PortSounds.get("zap"),1,1+(random.nextFloat()-random.nextFloat())*.2f);return true;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("Red",isRed());tag.putInt("Age",tickCount);if(targetUuid!=null)tag.putUUID("Target",targetUuid);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setRed(tag.getBoolean("Red"));tickCount=Math.max(0,tag.getInt("Age"));targetUuid=tag.hasUUID("Target")?tag.getUUID("Target"):null;entityData.set(TARGET,-1);}
}
