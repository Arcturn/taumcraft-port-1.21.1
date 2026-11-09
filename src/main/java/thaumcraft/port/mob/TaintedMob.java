package thaumcraft.port.mob;

import net.minecraft.world.entity.EntityType;
import thaumcraft.port.world.WorldEntities;

/** Shared faction and flux-taint healing marker from ITaintedMob in BETA26. */
public interface TaintedMob {
    static boolean isTaintedType(EntityType<?> type){
        return type==thaumcraft.port.boss.BossContent.GIANT_TAINTACLE.get()||type==MobContent.CRAWLER.get()||type==MobContent.SWARM.get()||type==MobContent.TAINTACLE.get()||type==MobContent.TAINTACLE_SMALL.get()||type==WorldEntities.SEED.get()||type==WorldEntities.PRIME.get()||type==WorldEntities.SLIME.get();
    }
}
