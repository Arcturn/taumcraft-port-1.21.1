package thaumcraft.port.auromancy.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.auromancy.focus.FocusPackage;
import thaumcraft.port.auromancy.focus.Trajectory;


public final class EntityFocusCloud extends SpellEntity {
    private static final EntityDataAccessor<Float> RADIUS=SynchedEntityData.defineId(EntityFocusCloud.class,EntityDataSerializers.FLOAT);
    private static final Map<ServerLevel,Map<BlockPos,Long>> BLOCK_COOLDOWNS=new WeakHashMap<>();
    private int duration;
    public EntityFocusCloud(EntityType<? extends EntityFocusCloud> type,Level world) {super(type,world);setNoGravity(true);}
    public EntityFocusCloud(FocusPackage pack,Trajectory trajectory,float radius,int duration) {
        this(AuromancyEntities.CLOUD.get(),pack.world);setFocusPackage(pack);setPos(trajectory.source);setRadius(radius);this.duration=duration;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {super.defineSynchedData(builder);builder.define(RADIUS,.5f);}
    public float getRadius() {return entityData.get(RADIUS);}
    public void setRadius(float radius) {entityData.set(RADIUS,net.minecraft.util.Mth.clamp(radius,.1f,16));refreshDimensions();}
    public int getDuration() {return duration;}
    @Override public EntityDimensions getDimensions(Pose pose) {return EntityDimensions.scalable(getRadius()*2,.5f);}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {super.onSyncedDataUpdated(key);if (RADIUS.equals(key)) refreshDimensions();}
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && (tickCount>duration*20 || getCaster()==null)) {discard();return;}
        if (level().isClientSide) {FocusMediaVisuals.entityTick(this);return;}
        if (tickCount%5!=0) return;
        var level=(ServerLevel)level(); var targets=new ArrayList<HitResult>(); var paths=new ArrayList<Trajectory>();
        float radius=getRadius();
        for (var entity:level.getEntities(this,new net.minecraft.world.phys.AABB(position(),position()).inflate(radius))) {
            if (entity instanceof EntityFocusCloud) {
                var push=entity.position().subtract(position()).scale(1.0/50);entity.move(MoverType.SELF,push);
            } else if (entity instanceof LivingEntity living && living.isAlive()) {
                // BETA26 queried a Long-keyed cooldown map with Integer IDs: entities
                // actually received effects every five ticks. Preserve that cadence.
                var midpoint=entity.position().add(0,entity.getBbHeight()/2,0);
                targets.add(new EntityHitResult(entity,midpoint));paths.add(new Trajectory(position(),midpoint.subtract(position())));
            }
        }
        var cooldowns=BLOCK_COOLDOWNS.computeIfAbsent(level,ignored->new HashMap<>());
        cooldowns.entrySet().removeIf(entry->entry.getValue()<=level.getGameTime());
        for (int ray=0;ray<radius;ray++) {
            var direction=new Vec3(random.nextGaussian(),random.nextGaussian(),random.nextGaussian()).normalize();
            var hit=level.clip(new ClipContext(position(),position().add(direction.scale(radius)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
            if (hit.getType()!=HitResult.Type.BLOCK || cooldowns.containsKey(hit.getBlockPos())) continue;
            cooldowns.put(hit.getBlockPos().immutable(),level.getGameTime()+40);
            targets.add(hit);paths.add(new Trajectory(position(),direction));
        }
        if (!targets.isEmpty()) SpellExecution.schedule(getFocusPackage().copy(getCaster()),paths.toArray(Trajectory[]::new),targets.toArray(HitResult[]::new),0);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);tag.putInt("Duration",duration);tag.putFloat("Radius",getRadius());}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);duration=Math.max(0,tag.getInt("Duration"));setRadius(tag.getFloat("Radius"));}
}
