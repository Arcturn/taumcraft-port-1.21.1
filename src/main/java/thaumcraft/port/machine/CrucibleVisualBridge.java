package thaumcraft.port.machine;

import java.util.function.Consumer;

/** Client callbacks are installed at client setup; dedicated servers never load render classes. */
public final class CrucibleVisualBridge {
    public static Consumer<CrucibleEntity> tick = crucible -> {};
    public static EventHandler event = (crucible, id, parameter) -> false;
    @FunctionalInterface public interface EventHandler {
        boolean handle(CrucibleEntity crucible, int id, int parameter);
    }
    private CrucibleVisualBridge() {}
}
