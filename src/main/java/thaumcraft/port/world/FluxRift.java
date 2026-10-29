package thaumcraft.port.world;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import thaumcraft.port.aspect.Aspect;

/** Original seeded rift geometry, flux growth, stability loss and weighted events. */
public final class FluxRift extends Entity {
    private static final EntityDataAccessor<Integer> SIZE=SynchedEntityData.defineId(FluxRift.class,EntityDataSerializers.INT),SEED=SynchedEntityData.defineId(FluxRift.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> STABILITY=SynchedEntityData.defineId(FluxRift.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> COLLAPSE=SynchedEntityData.defineId(FluxRift.class,EntityDataSerializers.BOOLEAN);
    private int maxSize=6,lastSize=-1,lastSeed;
    private final List<Vec3> points=new ArrayList<>();
    private final List<Float> widths=new ArrayList<>();
    public FluxRift(EntityType<? extends FluxRift> t,Level l){super(t,l);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(SIZE,6);b.define(SEED,1);b.define(STABILITY,0f);b.define(COLLAPSE,false);}
    public int size(){return entityData.get(SIZE);}
    public float stability(){return entityData.get(STABILITY);}
    public boolean collapsing(){return entityData.get(COLLAPSE);}
    public void addStability(float amount){if(Float.isFinite(amount))entityData.set(STABILITY,Math.clamp(stability()+amount,-100,100));}
    public void collapse(){entityData.set(COLLAPSE,true);}
    private void size(int n){n=Math.clamp(n,0,400);entityData.set(SIZE,n);maxSize=Math.max(maxSize,n);}
    public List<Vec3> points(){geometry();return Collections.unmodifiableList(points);}
    public List<Float> widths(){geometry();return Collections.unmodifiableList(widths);}
    private void geometry(){
        int seed=entityData.get(SEED);if(lastSize==size()&&lastSeed==seed)return;lastSize=size();lastSeed=seed;
        points.clear();widths.clear();Random r=new Random(seed);
        Vec3 right=new Vec3(r.nextGaussian(),r.nextGaussian(),r.nextGaussian()).normalize(),left=right.scale(-1),lr=Vec3.ZERO,ll=Vec3.ZERO;
        int steps=Math.max(1,(int)Math.ceil(size()/3f));float girth=size()/300f,dec=girth/steps;
        for(int i=0;i<steps;i++){
            girth-=dec;right=right.xRot((float)(r.nextGaussian()*.33)).yRot((float)(r.nextGaussian()*.33));lr=lr.add(right.scale(.2));points.add(lr);widths.add(Math.max(0,girth));
            left=left.xRot((float)(r.nextGaussian()*.33)).yRot((float)(r.nextGaussian()*.33));ll=ll.add(left.scale(.2));points.addFirst(ll);widths.addFirst(Math.max(0,girth));
        }
        points.add(lr.add(right.scale(.1)));widths.add(0f);points.addFirst(ll.add(left.scale(.1)));widths.addFirst(0f);
        AABB box=new AABB(position(),position());for(var p:points)box=box.minmax(new AABB(position().add(p),position().add(p)));setBoundingBox(box.inflate(1));
    }
    public static void tryCreate(ServerLevel l,ChunkPos chunk){
        if(!WorldConfig.RIFTS.get())return;
        int x=chunk.getMinBlockX()+l.random.nextInt(16),z=chunk.getMinBlockZ()+l.random.nextInt(16);
        var p=new BlockPos(x,l.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z),z);
        if(!l.dimensionType().hasSkyLight()){
            p=new BlockPos(x,l.getMinBuildHeight()+10,z);
            while(!l.isEmptyBlock(p)&&p.getY()<l.getMaxBuildHeight()-4)p=p.above(1+l.random.nextInt(5));
        }
        if(p.getY()>=l.getMaxBuildHeight()-4||!l.getEntitiesOfClass(FluxRift.class,new AABB(p).inflate(32)).isEmpty())return;
        var a=AuraSystem.get(l,p);if(a==null)return;int size=(int)Math.sqrt(a.flux()*3);
        if(size<=5)return;
        var r=WorldEntities.RIFT.get().create(l);if(r==null)return;r.moveTo(x+.5,p.getY()+.5,z+.5,0,0);r.entityData.set(SEED,l.random.nextInt());r.size(size);
        if(l.addFreshEntity(r))AuraSystem.drain(l,p,size,true,false);
    }
    @Override public void tick(){
        super.tick();geometry();if(!(level() instanceof ServerLevel l))return;
        if(!WorldConfig.RIFTS.get())return;
        if(points.size()>1){
            int i=random.nextInt(points.size()-1);Vec3 a=position().add(points.get(i)),b=position().add(points.get(i+1));
            if(l.hasChunkAt(BlockPos.containing(a))&&l.hasChunkAt(BlockPos.containing(b))){
                var hit=l.clip(new ClipContext(a,b,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
                if(hit.getType()==HitResult.Type.BLOCK&&l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)){
                    var p=hit.getBlockPos();var s=l.getBlockState(p);
                    if(!s.isAir()&&s.getDestroySpeed(l,p)>=0){l.levelEvent(2001,p,net.minecraft.world.level.block.Block.getId(s));l.removeBlock(p,false);}
                }
                for(var e:l.getEntities(this,new AABB(a,a).inflate(.5))){if(e instanceof Player p&&p.isCreative())continue;e.hurt(damageSources().fellOutOfWorld(),2);if(e instanceof ItemEntity)e.discard();}
            }
        }
        if(collapsing()){
            size(size()-1);AuraSystem.add(l,blockPosition(),1,random.nextBoolean());
            if(size()<=1){finish(l);return;}
        }
        if(tickCount%120==0)addStability(-.2f);
        if(tickCount%600==Math.floorMod(getId(),600)){
            float amount=(float)Math.sqrt(size()*2);
            if(size()<100&&stability()<=50&&AuraSystem.drain(l,blockPosition(),amount,true,true)>=amount){AuraSystem.drain(l,blockPosition(),amount,true,false);size(size()+1);}
            if(stability()<0&&random.nextInt(1000)<Math.abs(stability())+size())event(l);
        }
        if(tickCount%300==0)playSound(thaumcraft.port.content.PortSounds.get("evilportal"),.15f,.75f);
    }
    private void event(ServerLevel l){
        int choice=random.nextInt(101);
        if(choice<50){
            var w=WorldEntities.WISP.get().create(l);if(w!=null){w.moveTo(getX()+random.nextGaussian()*5,getY()+random.nextGaussian()*5,getZ()+random.nextGaussian()*5,0,0);
                if(random.nextInt(5)==0)w.aspect(Aspect.FLUX);else w.aspect(Aspect.values()[random.nextInt(6)]);
                if(l.hasChunkAt(w.blockPosition())&&l.noCollision(w)&&l.addFreshEntity(w))addStability(5);}
        }else if(choice<60){
            if(!WorldConfig.TAINT.get()||TaintSystem.nearby(l,blockPosition()))return;
            int x=(int)(getX()+random.nextGaussian()*5),z=(int)(getZ()+random.nextGaussian()*5);if(!l.hasChunk(x>>4,z>>4))return;
            var seed=WorldEntities.PRIME.get().create(l);if(seed!=null){seed.moveTo(x+.5,l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z+.5,0,0);seed.boost=size();
                if(l.noCollision(seed)&&l.addFreshEntity(seed)){AuraSystem.add(l,blockPosition(),size()/2f,true);discard();}}
        }else if(choice<80){
            var targets=l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(16));
            for(var target:targets)target.addEffect(new MobEffectInstance(WorldEntities.INFECTIOUS,3000,2));if(!targets.isEmpty())addStability(10);
        }else if(choice<100){
            var target=l.getNearestPlayer(this,16);if(target==null)return;
            var cloud=new AreaEffectCloud(l,target.getX(),target.getY(),target.getZ());cloud.setRadius(1+random.nextInt(3));cloud.setDuration(Math.max(20,Math.min(size(),120)*20));cloud.setParticle(ParticleTypes.WITCH);cloud.addEffect(new MobEffectInstance(WorldEntities.TAINT,100));l.addFreshEntity(cloud);addStability(10);
        }else collapse();
    }
    private void finish(ServerLevel l){
        int count=(int)Math.sqrt(maxSize);
        for(int i=0;i<count;i++)spawnAtLocation(new ItemStack(BuiltInRegistries.ITEM.get(WorldContent.id("void_seed"))));
        if(random.nextInt(100)<count){var pearl=new ItemStack(BuiltInRegistries.ITEM.get(WorldContent.id("primordial_pearl")));pearl.setDamageValue(4+random.nextInt(4));spawnAtLocation(pearl);}
        l.sendParticles(ParticleTypes.EXPLOSION,getX(),getY(),getZ(),8,.5,.5,.5,0);
        for(var e:l.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(32))){
            double strength=1-distanceTo(e)/32;if(strength<=0)continue;
            if(stability()<-25)e.addEffect(new MobEffectInstance(WorldEntities.TAINT,(int)(strength*2400)));
            if(stability()<0)e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,(int)(strength*6000)));
        }
        discard();
    }
    @Override protected void addAdditionalSaveData(CompoundTag t){t.putInt("size",size());t.putInt("seed",entityData.get(SEED));t.putInt("maxSize",maxSize);t.putFloat("stability",stability());t.putBoolean("collapse",collapsing());}
    @Override protected void readAdditionalSaveData(CompoundTag t){if(t.contains("size"))size(t.getInt("size"));if(t.contains("seed"))entityData.set(SEED,t.getInt("seed"));maxSize=t.contains("maxSize")?Math.clamp(t.getInt("maxSize"),size(),400):size();float stability=t.getFloat("stability");entityData.set(STABILITY,Float.isFinite(stability)?Math.clamp(stability,-100,100):0);entityData.set(COLLAPSE,t.getBoolean("collapse"));}
}
