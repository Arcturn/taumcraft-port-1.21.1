package thaumcraft.port.infusion.armor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.port.content.PortParticles;

/** Original accelerating collector item; ordinary pickup and inventory insertion stay server-owned. */
public final class FollowingItemEntity extends ItemEntity {
    private static final EntityDataAccessor<Integer> TARGET=SynchedEntityData.defineId(FollowingItemEntity.class,EntityDataSerializers.INT);
    private int countdown=20;
    public FollowingItemEntity(EntityType<? extends ItemEntity> type,Level level){super(type,level);}
    public FollowingItemEntity(ItemEntity original,Entity target){this(InfusionEnchantments.FOLLOWING_ITEM.get(),original.level());setPos(original.position());setItem(original.getItem().copy());setDeltaMovement(original.getDeltaMovement());setPickUpDelay(10);lifespan=original.lifespan;entityData.set(TARGET,target.getId());}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(TARGET,-1);}
    @Override public void tick(){
        var target=level().getEntity(entityData.get(TARGET));
        if(target!=null&&target.isAlive()){
            var delta=target.position().add(0,target.getBbHeight()*.5,0).subtract(position());double distance=delta.length();
            if(countdown>1)countdown--;
            if(distance>.5){noPhysics=true;setNoGravity(true);setDeltaMovement(delta.scale(1/(distance*countdown)));}
            else {entityData.set(TARGET,-1);noPhysics=false;setNoGravity(false);setDeltaMovement(getDeltaMovement().scale(.1));}
            if(level().isClientSide)InfusionCollectorVisuals.trail.accept(this);
        }else{noPhysics=false;setNoGravity(false);}
        super.tick();
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(TARGET,-1);}
}
