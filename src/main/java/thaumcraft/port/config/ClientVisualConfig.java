package thaumcraft.port.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Original TC6 graphics options used by the aspect displays. Safe to register on either distribution. */
public final class ClientVisualConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_TAGS;
    public static final ModConfigSpec.BooleanValue LARGE_TAG_TEXT;
    public static final ModConfigSpec.BooleanValue NO_STRESS;
    public static final ModConfigSpec.BooleanValue DISABLE_WARP_SHADERS;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("aspects");
        SHOW_TAGS = builder.comment("Display item aspects without holding Shift. Shift hides them when enabled.")
                .define("showTags", false);
        LARGE_TAG_TEXT = builder.comment("Use full-size numbers on aspect icons instead of small, pixel-aligned numbers.")
                .define("largeTagText", false);
        builder.pop();
        builder.push("warp");
        NO_STRESS = builder.comment("Disable Warp heartbeat, phantom creeper hiss and explosion sounds (TC6 noStress). Does not change gameplay.")
                .define("noStress", false);
        DISABLE_WARP_SHADERS = builder.comment("Disable full-screen blur, desaturation, hunger tint and Sun Scorned bloom. Mist and Death Gaze vignette remain, as in TC6.")
                .define("disableShaders", false);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientVisualConfig() {}
}
