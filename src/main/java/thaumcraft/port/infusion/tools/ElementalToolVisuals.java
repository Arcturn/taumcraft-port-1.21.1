package thaumcraft.port.infusion.tools;

import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

/** Dedicated servers never load the client particle implementations. */
public final class ElementalToolVisuals {
    public static Consumer<ItemEntity> attraction = item -> {};
    public static Consumer<LivingEntity> vortex = entity -> {};
    public static Consumer<net.minecraft.core.BlockPos> growth = pos -> {};
    public static java.util.function.BiConsumer<net.minecraft.core.BlockPos,Boolean> till = (pos,center) -> {};
    private ElementalToolVisuals() {}
}
