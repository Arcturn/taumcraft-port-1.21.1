package thaumcraft.port.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thaumcraft.port.content.PortParticles;

public final class AlumentumProjectile extends ThrowableItemProjectile {
    public AlumentumProjectile(EntityType<? extends AlumentumProjectile> type,Level level){super(type,level);}
    @Override protected Item getDefaultItem(){return BuiltInRegistries.ITEM.get(WorldContent.id("alumentum"));}
    @Override protected void onHit(HitResult result){
        super.onHit(result);if(!level().isClientSide){level().explode(this,getX(),getY(),getZ(),1.1f,Level.ExplosionInteraction.TNT);discard();}
    }
    @Override public void tick(){
        super.tick();if(level().isClientSide)for(int i=0;i<3;i++)level().addParticle(PortParticles.get("alumentum"),getX()+random.nextGaussian()*.2,getY()+random.nextGaussian()*.2,getZ()+random.nextGaussian()*.2,0,0,0);
        if(!level().isClientSide&&tickCount>1200)discard();
    }
}
