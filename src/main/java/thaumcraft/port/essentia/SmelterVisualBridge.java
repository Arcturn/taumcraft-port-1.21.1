package thaumcraft.port.essentia;

import net.minecraft.core.Direction;

/** Client-only vent presentation is installed without linking render classes on the server. */
public final class SmelterVisualBridge {
    public static VentHandler vent=(smelter,direction)->false;
    @FunctionalInterface public interface VentHandler {
        boolean handle(SmelterEntity smelter,Direction direction);
    }
    private SmelterVisualBridge(){}
}
