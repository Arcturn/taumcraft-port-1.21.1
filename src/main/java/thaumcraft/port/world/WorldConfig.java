package thaumcraft.port.world;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class WorldConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ORES, CRYSTALS, TREES, STRUCTURES, PORTALS, AURA, MAGIC_FOREST, RIFTS, TAINT;
    public static final ModConfigSpec.BooleanValue NO_SLEEP;
    public static final ModConfigSpec.BooleanValue BRAINY_ZOMBIES;
    public static final ModConfigSpec.IntValue DENSITY, FOREST_WEIGHT;
    static {
        var b = new ModConfigSpec.Builder();
        b.comment("New terrain only. No automatic retrogen or replacement of existing blocks.").push("worldgen");
        ORES=b.define("ores", true); CRYSTALS=b.define("crystals", true);
        TREES=b.define("treesAndPlants", true); STRUCTURES=b.define("mounds", true);
        PORTALS=b.define("lesserCultistPortals",true);
        MAGIC_FOREST=b.define("magicalForest", true);
        DENSITY=b.defineInRange("oreDensityPercent",100,1,500);
        FOREST_WEIGHT=b.comment("TerraBlender region weight. Restart required.").defineInRange("magicalForestWeight",5,1,100);
        b.pop().push("aura"); AURA=b.define("enabled",true);
        RIFTS=b.define("fluxRifts",true); TAINT=b.define("taintSpread",true);
        b.pop().push("research");
        NO_SLEEP=b.comment("Give the dream journal on first crystal pickup instead of requiring sleep, as in TC6.").define("noSleep",false);
        b.pop().push("mobs");
        BRAINY_ZOMBIES=b.comment("Allow angry zombies to spawn naturally in eligible Overworld biomes, as in TC6.").define("brainyZombies",true);
        b.pop(); SPEC=b.build();
    }
    private WorldConfig() {}
}
