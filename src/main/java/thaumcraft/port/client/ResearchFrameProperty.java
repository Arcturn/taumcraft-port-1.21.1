package thaumcraft.port.client;

import java.util.function.LongSupplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.research.ResearchIconTiming;

/** Deliberately unclamped: the original crystal sequence has six frames. */
public final class ResearchFrameProperty implements ItemPropertyFunction {
    private final int frames;
    private final LongSupplier clock;
    public ResearchFrameProperty(int frames,LongSupplier clock){this.frames=frames;this.clock=clock;}
    @Override public float call(ItemStack stack,ClientLevel level,LivingEntity entity,int seed){return ResearchIconTiming.frame(frames,clock.getAsLong());}
}
