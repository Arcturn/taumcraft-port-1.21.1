package thaumcraft.port.world;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Client installs visual callbacks without loading client classes on dedicated servers. */
public final class TaintVisualHooks {
    public static BiConsumer<Level,BlockPos> GOO_BUBBLE=(level,pos)->{};
    private TaintVisualHooks(){}
}
