package thaumcraft.port.world;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
public final class Wisp extends FlyingMob {
    private static final EntityDataAccessor<String> ASPECT=SynchedEntityData.defineId(Wisp.class,EntityDataSerializers.STRING);
    private Vec3 wander;
    private int aggression, attackCounter;
    public Wisp(EntityType<? extends Wisp> type,Level level){super(type,level);xpReward=5;setNoGravity(true);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,22).add(Attributes.FOLLOW_RANGE,16).add(Attributes.MOVEMENT_SPEED,.25);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ASPECT,"");}
    public Aspect aspect(){return Aspect.byId(entityData.get(ASPECT)).orElse(Aspect.AIR);}
    public void aspect(Aspect a){entityData.set(ASPECT,a.id());}
    @Override protected void registerGoals(){}
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return thaumcraft.port.content.PortSounds.get("wisplive");}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return thaumcraft.port.content.PortSounds.get("wispdead");}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){
        boolean hit=super.hurt(source,amount);
        if(hit&&source.getEntity() instanceof LivingEntity attacker){setTarget(attacker);aggression=200;}
        return hit;
    }
    public static boolean canSpawn(EntityType<Wisp> type,net.minecraft.world.level.ServerLevelAccessor l,MobSpawnType reason,net.minecraft.core.BlockPos p,net.minecraft.util.RandomSource r){
        return l.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL
            &&l.getBrightness(net.minecraft.world.level.LightLayer.SKY,p)<=r.nextInt(32)
            &&l.getMaxLocalRawBrightness(p)<=r.nextInt(8)
            &&l.getLevel().getEntitiesOfClass(Wisp.class,new net.minecraft.world.phys.AABB(p).inflate(16)).size()<8;
    }
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel l))return;
        if(l.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){discard();return;}
        if(entityData.get(ASPECT).isEmpty()){
            var pool=java.util.Arrays.stream(Aspect.values()).filter(a->a.isPrimal()).toList();
            if(random.nextInt(10)==0)pool=java.util.Arrays.stream(Aspect.values()).filter(a->!a.isPrimal()).toList();
            aspect(pool.get(random.nextInt(pool.size())));
        }
        if(tickCount%4==0)thaumcraft.port.network.WorldFxPayload.send(l,2,getEyePosition(),getEyePosition(),aspect().color());
        if(aggression>0)aggression--;else setTarget(null);
        if(getTarget()==null&&random.nextInt(1000)==0){var p=l.players().stream().filter(player->player.isAlive()&&!player.isCreative()&&!player.isSpectator()&&distanceToSqr(player)<=16*16).min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);if(p!=null){setTarget(p);aggression=50;}}
        var target=getTarget();Vec3 destination;
        if(target!=null&&target.isAlive()){
            destination=distanceToSqr(target)>128?target.getEyePosition():position();
            if(distanceToSqr(target)<256&&hasLineOfSight(target)){
                if(++attackCounter>=20){
                    var v=target.getDeltaMovement();boolean moving=Math.abs(v.x)>.1||Math.abs(v.y)>.1||Math.abs(v.z)>.1;
                    if(random.nextFloat()<(moving?.4f:.66f))target.hurt(damageSources().indirectMagic(this,this),moving?3:4);
                    thaumcraft.port.network.WorldFxPayload.send(l,1,getEyePosition(),target.getEyePosition(),aspect().color());
                    playSound(thaumcraft.port.content.PortSounds.get("zap"),1,1.1f);
                    attackCounter=-20+random.nextInt(20);
                }
            }else if(attackCounter>0)attackCounter--;
        }else{
            if(wander==null||tickCount%60==0||position().distanceToSqr(wander)<1){
                wander=position().add(random.nextInt(13)-6,random.nextInt(6)-2,random.nextInt(13)-6);
                int ceiling=l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(int)Math.floor(wander.x),(int)Math.floor(wander.z))+8;
                wander=new Vec3(wander.x,Math.min(wander.y,ceiling),wander.z);
            }
            destination=wander;
        }
        var d=destination.subtract(position());var v=getDeltaMovement().multiply(1,.6,1);
        setDeltaMovement(v.add((Math.signum(d.x)*.5-v.x)*.1,(Math.signum(d.y)*.7-v.y)*.1,(Math.signum(d.z)*.5-v.z)*.1));
    }
    @Override protected void dropCustomDeathLoot(ServerLevel l,net.minecraft.world.damagesource.DamageSource d,boolean recentlyHit){super.dropCustomDeathLoot(l,d,recentlyHit);spawnAtLocation(CrystalItem.stack(aspect()));}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);t.putString("aspect",aspect().id());}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);if(t.contains("aspect"))Aspect.byId(t.getString("aspect")).ifPresent(this::aspect);}
}
