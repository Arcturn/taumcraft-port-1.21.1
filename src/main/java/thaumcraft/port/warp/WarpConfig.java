package thaumcraft.port.warp;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side control of periodic warp events; visual comfort options are client-only. */
public final class WarpConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue WUSS_MODE;
    static {
        var builder = new ModConfigSpec.Builder();
        WUSS_MODE = builder.comment("Disable periodic warp events and their temporary-warp decay. Existing warp and active effects are preserved.")
                .define("disableEvents", false);
        SPEC = builder.build();
    }
    private WarpConfig() {}
}
