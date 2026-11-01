package thaumcraft.port.content;

import java.util.Map;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.essentia.*;
import thaumcraft.port.machine.CrucibleEntity;

/** The BETA26 IAspectContainer view, distinct from transport amounts and suction. */
public final class RevealingReadout {
    public static Map<Aspect,Integer> aspects(BlockEntity entity) {
        if(entity instanceof JarEntity jar)return single(jar.tank.aspect(),jar.tank.amount());
        if(entity instanceof BufferEntity buffer)return Map.copyOf(buffer.contents.values());
        if(entity instanceof TubeEntity tube&&tube.kind==TubeBlock.Kind.FILTER)return single(tube.tank.filter(),-1);
        if(entity instanceof CentrifugeEntity centrifuge)return single(centrifuge.outgoing,1);
        if(entity instanceof ThaumatoriumEntity machine) {
            var main=machine.main();
            return main==null?Map.of():Map.copyOf(main.contents.values());
        }
        if(entity instanceof thaumcraft.port.alchemy.devices.PotionSprayerEntity sprayer)return Map.copyOf(sprayer.store.progress());
        if(entity instanceof CrucibleEntity crucible)return Map.copyOf(crucible.state.aspects());
        return Map.of();
    }
    private static Map<Aspect,Integer> single(Aspect aspect,int amount) {
        return aspect==null||amount==0?Map.of():Map.of(aspect,amount);
    }
    private RevealingReadout() {}
}
