package thaumcraft.port.artifice.tool;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** TC6 hook flight, unique owner rope and accelerating pull. Authority stays on the server. */
public final class GrappleEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Boolean> PULLING=SynchedEntityData.defineId(GrappleEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MAIN_HAND=SynchedEntityData.defineId(GrappleEntity.class,EntityDataSerializers.BOOLEAN);
    private static final Map<UUID,GrappleEntity> ACTIVE=new ConcurrentHashMap<>();
    private UUID activeOwner;
    private boolean boosted;
    public float amplitude;
    public GrappleEntity(EntityType<? extends GrappleEntity> type,Level level){super(type,level);}
    public GrappleEntity(Level level,LivingEntity owner,InteractionHand hand){super(ArtificeTools.GRAPPLE.get(),owner,level);entityData.set(MAIN_HAND,hand==InteractionHand.MAIN_HAND);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){builder.define(PULLING,false);builder.define(MAIN_HAND,true);}
    public InteractionHand hand(){return entityData.get(MAIN_HAND)?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;}
    public boolean pulling(){return entityData.get(PULLING);}
    public static GrappleEntity active(Entity owner){var hook=ACTIVE.get(owner.getUUID());return hook!=null&&hook.isAlive()&&hook.level()==owner.level()?hook:null;}
    public void activate(){if(level().isClientSide||getOwner()==null)return;activeOwner=getOwner().getUUID();var previous=ACTIVE.put(activeOwner,this);if(previous!=null&&previous!=this)previous.discard();}
    @Override protected double getDefaultGravity(){return pulling()?0:.03;}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<4096;}
    @Override public void tick(){
        if(pulling())setDeltaMovement(Vec3.ZERO);
        super.tick();
        if(isRemoved())return;
        var owner=getOwner();
        if(!level().isClientSide){
            if(owner==null||!owner.isAlive()||owner.level()!=level()||(!pulling()&&tickCount>30)){discard();return;}
            if(activeOwner==null)activate();
            if(ACTIVE.get(owner.getUUID())!=this){discard();return;}
            if(pulling()&&owner.isShiftKeyDown()){discard();return;}
        }
        if(owner instanceof LivingEntity living&&pulling()){
            living.fallDistance=0;
            // Both sides simulate the original pull. Velocity sync also covers remote spectators.
            Vec3 change=pullVelocity(position().subtract(living.position()));
            if(!boosted){change=change.add(0,.4,0);boosted=true;}
            living.setDeltaMovement(living.getDeltaMovement().add(change));
            if(living instanceof ServerPlayer server){server.connection.aboveGroundTickCount=0;server.hurtMarked=true;}
        }
        if(level().isClientSide)amplitude=pulling()?amplitude*.66f:amplitude+.02f;
    }
    public static Vec3 pullVelocity(Vec3 delta){double distance=delta.length();double divisor=Math.max(1.0E-9,distance<8?distance*(8-distance):distance)*5;var result=delta.scale(1/divisor);if(result.length()>.25)result=result.normalize().scale(.25);return result.add(0,.033,0);}
    @Override protected void onHit(HitResult hit){if(!level().isClientSide&&hit.getType()!=HitResult.Type.MISS){entityData.set(PULLING,true);setDeltaMovement(Vec3.ZERO);setPos(hit.getLocation());hasImpulse=true;}}
    @Override public void remove(RemovalReason reason){if(activeOwner!=null)ACTIVE.remove(activeOwner,this);super.remove(reason);}
    @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("Pulling",pulling());tag.putBoolean("MainHand",hand()==InteractionHand.MAIN_HAND);tag.putBoolean("Boosted",boosted);tag.putInt("Age",tickCount);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(PULLING,tag.getBoolean("Pulling"));entityData.set(MAIN_HAND,!tag.contains("MainHand")||tag.getBoolean("MainHand"));boosted=tag.getBoolean("Boosted");tickCount=Math.max(0,tag.getInt("Age"));}
}
