package thaumcraft.port.warp.entity;

import java.util.function.Consumer;

/** Client hooks keep dedicated servers free of renderer classes. */
public final class WarpEntityVisuals {
    public static Consumer<EldritchGuardian> guardianWisp = entity -> {};
    public static Consumer<EldritchGuardian> sonic = entity -> {};
    private WarpEntityVisuals() {}
}
