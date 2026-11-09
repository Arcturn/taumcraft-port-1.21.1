package thaumcraft.port.mob;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.*;

/** Short-lived summoned tentacle: five health, two damage, no experience or loot. */
public final class TaintacleSmall extends Taintacle {
    private int lifetime=200;
    public TaintacleSmall(EntityType<? extends TaintacleSmall> type,Level level){super(type,level);xpReward=0;}
    public static AttributeSupplier.Builder attributes(){return Taintacle.attributes().add(Attributes.MAX_HEALTH,5).add(Attributes.ATTACK_DAMAGE,2);}
    @Override public void tick(){super.tick();if(!level().isClientSide&&lifetime--<=0&&isAlive())hurt(damageSources().magic(),10);}
    public int remainingLifetime(){return lifetime;}
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason){return false;}
    @Override protected boolean shouldDropLoot(){return false;}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean hit){}
    // Preserve the lifetime across chunk unload/reload instead of granting another ten seconds.
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Lifetime",lifetime);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(tag.contains("Lifetime"))lifetime=Math.clamp(tag.getInt("Lifetime"),-1,200);}
}
