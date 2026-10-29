package thaumcraft.port.world;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class CultistOrb extends ThrowableProjectile {
    private UUID targetId;private LivingEntity target;
    public CultistOrb(EntityType<? extends CultistOrb> type,Level l){super(type,l);setNoGravity(true);}
    public void target(LivingEntity target){this.target=target;this.targetId=target.getUUID();}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
    @Override public void tick(){
        super.tick();if(tickCount>240){discard();return;}
        if(level() instanceof ServerLevel l){
            if(target==null&&targetId!=null&&l.getEntity(targetId) instanceof LivingEntity living)target=living;
            if(target!=null&&target.isAlive()){
                var d=target.getEyePosition().subtract(position());var v=getDeltaMovement().add(d.scale(.2/Math.max(.01,d.lengthSqr())));
                setDeltaMovement(Mth.clamp(v.x,-.25,.25),Mth.clamp(v.y,-.25,.25),Mth.clamp(v.z,-.25,.25));
            }
        }
    }
    @Override protected void onHit(HitResult hit){if(!level().isClientSide){if(hit instanceof EntityHitResult entity)entity.getEntity().hurt(damageSources().indirectMagic(this,getOwner()),4);discard();}}
    @Override public boolean isPickable(){return true;}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){
        if(source.getEntity()==null)return false;setDeltaMovement(source.getEntity().getLookAngle().scale(.9));return true;
    }
    @Override protected void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);if(targetId!=null)t.putUUID("target",targetId);t.putInt("age",tickCount);}
    @Override protected void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);if(t.hasUUID("target"))targetId=t.getUUID("target");tickCount=t.getInt("age");}
}
