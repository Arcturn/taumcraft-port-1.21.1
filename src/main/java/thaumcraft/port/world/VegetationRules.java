package thaumcraft.port.world;

import net.minecraft.util.RandomSource;

/** TC6's tree selection, before terrain and biome suitability can reject a placement. */
public final class VegetationRules {
    public enum ForestTree { SILVERWOOD, GREATWOOD, BIG_MAGIC }

    private VegetationRules() {}

    public static int magicalForestTreeAttempts(RandomSource random) {
        // BiomeGenMagicalForest sets treesPerChunk=2; 6 belongs to waterlilyPerChunk.
        // Vanilla 1.12.2 BiomeDecorator adds one tree with extraTreeChance=0.1f.
        return 2 + (random.nextFloat() < .1f ? 1 : 0);
    }

    public static ForestTree magicalForestTree(RandomSource random) {
        if (random.nextInt(18) == 0) return ForestTree.SILVERWOOD;
        return random.nextInt(12) == 0 ? ForestTree.GREATWOOD : ForestTree.BIG_MAGIC;
    }

    public static boolean rollSilverwood(RandomSource random) {
        return random.nextInt(80) == 3;
    }

    public static boolean rollGreatwood(RandomSource random) {
        return random.nextInt(25) == 7;
    }

    public static int rareTreeCoordinate(int chunkMin, RandomSource random) {
        // Original chunk*16 + 8 + nextInt(-4, 4), both bounds inclusive.
        return chunkMin + 4 + random.nextInt(9);
    }
}
