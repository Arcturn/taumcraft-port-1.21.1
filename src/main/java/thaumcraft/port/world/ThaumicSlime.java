package thaumcraft.port.world;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
public final class ThaumicSlime extends Slime {
    public ThaumicSlime(EntityType<? extends Slime> type,Level level){super(type,level);}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel l))return;
        var target=getTarget();
        if(getSize()>2&&target!=null&&target.isAlive()&&distanceToSqr(target)<256&&hasLineOfSight(target)&&tickCount%100==0){
            var small=WorldEntities.SLIME.get().create(l);if(small!=null){small.setSize(1,true);small.moveTo(getX(),getY()+getBbHeight()/2,getZ(),getYRot(),0);small.setDeltaMovement(target.getEyePosition().subtract(small.position()).normalize().scale(1.5).add(0,.2,0));l.addFreshEntity(small);}
        }
    }
    @Override protected void dropCustomDeathLoot(ServerLevel l,net.minecraft.world.damagesource.DamageSource source,boolean hit){if(getSize()==1&&random.nextBoolean())spawnAtLocation(CrystalItem.stack(Aspect.FLUX));}
}
