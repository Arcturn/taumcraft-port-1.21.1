package thaumcraft.port.world;

import java.util.function.Consumer;

/** Client callbacks keep render classes out of the dedicated server. */
public final class WispVisualBridge {
    public static Consumer<Wisp> tick=wisp->{};
    public static Consumer<Wisp> death=wisp->{};
    private WispVisualBridge() {}
}
