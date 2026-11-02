package thaumcraft.port.auromancy.focus.medium;

import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** The source entity ray stops at blocks and inflates hit boxes by the focus's quarter-block margin. */
public final class FocusTargeting {
    public static HitResult ray(Level level,LivingEntity caster,Vec3 start,Vec3 direction,double range) {
        return ray(level,caster,caster,start,direction,range,.25);
    }
    public static HitResult ray(Level level,Entity context,Entity ignored,Vec3 start,Vec3 direction,double range,double padding) {
        Vec3 end=start.add(direction.normalize().scale(range));
        HitResult block=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,context));
        double nearest=block.getType()==HitResult.Type.MISS?range*range:start.distanceToSqr(block.getLocation());
        EntityHitResult entityHit=null;
        for(var entity:level.getEntities(context,new AABB(start,end).inflate(padding),e->e!=ignored&&e.isPickable()&&!e.isSpectator())) {
            var box=entity.getBoundingBox().inflate(entity.getPickRadius()+padding);
            var hit=box.clip(start,end);double distance=box.contains(start)?0:hit.map(start::distanceToSqr).orElse(Double.MAX_VALUE);
            if(distance<=nearest){nearest=distance;entityHit=new EntityHitResult(entity,hit.orElse(start));}
        }
        return entityHit==null?block:entityHit;
    }
    public static boolean friendly(Entity source,Entity target) {
        if(source==null||target==null)return false;
        if(source==target||source.isPassengerOfSameVehicle(target)||source.isAlliedTo(target))return true;
        if(target instanceof OwnableEntity own&&source.getUUID().equals(own.getOwnerUUID()))return true;
        return target instanceof ServerPlayer player&&!player.server.isPvpAllowed();
    }
    public static boolean visible(Entity observer,Entity target,double range,boolean cone) {
        Vec3 delta=target.position().add(0,target.getBbHeight()/2,0).subtract(observer.getEyePosition());
        if(delta.dot(observer.getLookAngle())>=range)return false;
        if(cone&&delta.normalize().dot(observer.getLookAngle())<=Math.cos(1.75/2))return false;
        return observer.level().clip(new ClipContext(observer.getEyePosition(),target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,observer)).getType()==HitResult.Type.MISS;
    }
    public static List<LivingEntity> nearby(Level level,Entity source,double range) {
        return level.getEntitiesOfClass(LivingEntity.class,new AABB(source.position(),source.position()).inflate(range),e->e!=source&&e.isAlive()&&!e.isSpectator())
                .stream().sorted(Comparator.comparingDouble(source::distanceToSqr)).toList();
    }
    private FocusTargeting(){}
}
