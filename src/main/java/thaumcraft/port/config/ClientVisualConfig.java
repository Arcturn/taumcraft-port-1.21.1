package thaumcraft.port.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Original TC6 graphics options used by the aspect displays. Safe to register on either distribution. */
public final class ClientVisualConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_TAGS;
    public static final ModConfigSpec.BooleanValue LARGE_TAG_TEXT;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("aspects");
        SHOW_TAGS = builder.comment("Display item aspects without holding Shift. Shift hides them when enabled.")
                .define("showTags", false);
        LARGE_TAG_TEXT = builder.comment("Use full-size numbers on aspect icons instead of small, pixel-aligned numbers.")
                .define("largeTagText", false);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientVisualConfig() {}
}
