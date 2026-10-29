package thaumcraft.port.gametest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.world.WorldContent;

/**
 * Real registered vegetation and tree algorithms on isolated, bounded grass plots.
 * These are controlled feature placements, not a density survey of natural terrain.
 */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class VegetationGameTests {
    private static final ResourceKey<PlacedFeature> VEGETATION =
            ResourceKey.create(Registries.PLACED_FEATURE, WorldContent.id("vegetation"));
    private static final ResourceKey<Biome> MAGICAL_FOREST =
            ResourceKey.create(Registries.BIOME, WorldContent.id("magical_forest"));

    @GameTest(template="empty", timeoutTicks=200)
    public static void naturalVegetationIsRegisteredOncePerOverworldBiome(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        int checked = 0;
        for (var biome : biomes.holders().toList()) {
            long vegetation = biome.value().getGenerationSettings().features().stream()
                    .flatMap(set -> set.stream()).filter(feature -> feature.is(VEGETATION)).count();
            helper.assertValueEqual(vegetation, biome.is(BiomeTags.IS_OVERWORLD) ? 1L : 0L,
                    "Wrong natural vegetation registration count in " + biome.key().location());
            boolean sapling = biome.value().getGenerationSettings().features().stream()
                    .flatMap(set -> set.stream()).flatMap(feature -> feature.value().getFeatures())
                    .anyMatch(feature -> feature.feature() == WorldContent.GREATWOOD.get()
                            || feature.feature() == WorldContent.SILVERWOOD.get());
            helper.assertFalse(sapling, "Sapling-only feature leaked into natural biome decoration");
            checked++;
        }
        helper.assertTrue(checked > 50, "Biome registry was not populated");
        System.out.println("WORLDGEN_REGISTRATION checked_biomes=" + checked + " duplicate_vegetation=0");
        helper.succeed();
    }

    @GameTest(template="empty", timeoutTicks=200)
    public static void magicalForestUsesTwoTreesAndOriginalExtraChance(GameTestHelper helper) {
        var sample = sample(helper, MAGICAL_FOREST, 512, true);
        helper.assertTrue(sample.thirdTreeChunks >= 25 && sample.thirdTreeChunks <= 80,
                "Magical forest extra-tree frequency differs from original ten percent: " + sample.thirdTreeChunks);
        helper.assertTrue(sample.oaks > 300 && sample.oaks < 1100,
                "Controlled forest plot produced an implausible number of actual oak trunks: " + sample.oaks);
        helper.assertTrue(sample.greatwoods > 10 && sample.silverwoods > 5,
                "Magical forest stopped placing one of the original special tree types");
        helper.succeed();
    }

    @GameTest(template="empty", timeoutTicks=200)
    public static void ordinaryBiomesKeepOriginalRareTreeRates(GameTestHelper helper) {
        var forest = sample(helper, Biomes.FOREST, 4096, false);
        var plains = sample(helper, Biomes.PLAINS, 4096, false);
        assertRate(helper, forest.greatwoods, 4096.0 / 25, "forest greatwood");
        assertRate(helper, forest.silverwoods, 4096.0 / 160, "forest silverwood");
        assertRate(helper, plains.greatwoods, 4096.0 / 125, "plains greatwood");
        assertRate(helper, plains.silverwoods, 4096.0 / 800, "plains silverwood");
        helper.assertValueEqual(forest.oaks + plains.oaks, 0, "Magic oaks leaked into ordinary biomes");
        helper.succeed();
    }

    @GameTest(template="empty", timeoutTicks=200)
    public static void actualSaplingGrowersStillGrowBothMagicTrees(GameTestHelper helper) {
        var server = helper.getLevel();
        var anchor = helper.absolutePos(BlockPos.ZERO);
        // These elevated plots belong only to the disposable GameTest world.
        // They exercise TreeGrower's real sapling clearing/restoration pipeline.
        var greatRoot = new BlockPos(anchor.getX() + 4, 200, anchor.getZ() + 4);
        var silverRoot = greatRoot.offset(48, 0, 0);
        for (var root : new BlockPos[]{greatRoot, silverRoot}) {
            for (var soil : BlockPos.betweenClosed(root.offset(-3, -1, -3), root.offset(3, -1, 3)))
                server.setBlock(soil, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        }
        var greatSapling = (SaplingBlock)WorldContent.block("sapling_greatwood");
        var greatState = greatSapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        for (int x=0; x<2; x++) for (int z=0; z<2; z++)
            server.setBlock(greatRoot.offset(x, 0, z), greatState, 2);
        greatSapling.performBonemeal(server, RandomSource.create(10), greatRoot, greatState);
        for (int x=0; x<2; x++) for (int z=0; z<2; z++)
            helper.assertTrue(server.getBlockState(greatRoot.offset(x, 0, z)).is(WorldContent.block("log_greatwood")),
                    "Real 2x2 greatwood grower failed to clear saplings before strict trunk validation");
        helper.assertTrue(server.getBlockState(greatRoot.above(5)).is(WorldContent.block("log_greatwood")),
                "Greatwood grower placed an incomplete trunk");
        var silverSapling = (SaplingBlock)WorldContent.block("sapling_silverwood");
        var silverState = silverSapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        server.setBlock(silverRoot, silverState, 2);
        silverSapling.performBonemeal(server, RandomSource.create(10), silverRoot, silverState);
        helper.assertTrue(server.getBlockState(silverRoot).is(WorldContent.block("log_silverwood")),
                "Real single silverwood sapling grower failed");
        helper.assertTrue(server.getBlockState(silverRoot.above(5)).is(WorldContent.block("log_silverwood")),
                "Silverwood grower placed an incomplete trunk");
        helper.succeed();
    }

    private static void assertRate(GameTestHelper helper, int observed, double expected, String label) {
        // Broad deterministic sampling guard: this catches multiplied/missing features,
        // while geometry and collision changes are allowed to vary a few successful trees.
        helper.assertTrue(Math.abs(observed - expected) <= Math.max(8, 6 * Math.sqrt(expected)),
                label + " count " + observed + " is inconsistent with expected " + expected);
    }

    private static Sample sample(GameTestHelper helper, ResourceKey<Biome> key, int chunks, boolean magical) {
        var server = helper.getLevel();
        var biome = server.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
        var vegetation = server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).getHolderOrThrow(VEGETATION).value();
        var generator = new NoiseBasedChunkGenerator(new FixedBiomeSource(biome),
                server.registryAccess().registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD));
        var total = new Sample();
        for (int i=0; i<chunks; i++) {
            var plot = new Plot(server, biome);
            var random = new TrackingRandom(0x6a09e667f3bcc909L + i * 0x9e3779b97f4a7c15L);
            helper.assertTrue(vegetation.place(plot.level, generator, random, BlockPos.ZERO), "Registered vegetation did not run");
            helper.assertValueEqual(random.silverRolls, 1, "Missing or duplicate per-chunk silverwood roll in " + key.location());
            helper.assertValueEqual(random.greatRolls, 1, "Missing or duplicate per-chunk greatwood roll in " + key.location());
            if (magical) {
                helper.assertTrue(random.forestSelections == 2 || random.forestSelections == 3,
                        "Magical forest must select two trees and sometimes a third, got " + random.forestSelections);
                if (random.forestSelections == 3) total.thirdTreeChunks++;
            } else {
                helper.assertValueEqual(random.forestSelections, 0, "Forest decorator ran in an ordinary biome");
            }
            int great = plot.trunkGroups(WorldContent.block("log_greatwood"));
            int silver = plot.trunkGroups(WorldContent.block("log_silverwood"));
            int oak = plot.trunkGroups(Blocks.OAK_LOG);
            if (!magical) {
                helper.assertTrue(great <= 1 && silver <= 1, "Multiple same-species natural trees in one ordinary chunk");
            }
            total.greatwoods += great;
            total.silverwoods += silver;
            total.oaks += oak;
        }
        System.out.println("WORLDGEN_SAMPLE biome=" + key.location() + " chunks=" + chunks
                + " greatwood_trunk_groups=" + total.greatwoods + " silverwood_trunk_groups=" + total.silverwoods
                + " oak_trunk_groups=" + total.oaks + " third_tree_chunks=" + total.thirdTreeChunks
                + " scope=controlled_registered_feature_on_isolated_grass_plots");
        return total;
    }

    private static final class Sample {
        int greatwoods, silverwoods, oaks, thirdTreeChunks;
    }

    /** Count chunk-level selection rolls while preserving every random result. */
    private static final class TrackingRandom implements RandomSource {
        private final RandomSource delegate;
        int silverRolls, greatRolls, forestSelections;
        TrackingRandom(long seed) { delegate = RandomSource.create(seed); }
        @Override public RandomSource fork() { return delegate.fork(); }
        @Override public PositionalRandomFactory forkPositional() { return delegate.forkPositional(); }
        @Override public void setSeed(long seed) { delegate.setSeed(seed); }
        @Override public int nextInt() { return delegate.nextInt(); }
        @Override public int nextInt(int bound) {
            if (bound == 80) silverRolls++;
            if (bound == 25) greatRolls++;
            if (bound == 18) forestSelections++;
            return delegate.nextInt(bound);
        }
        @Override public long nextLong() { return delegate.nextLong(); }
        @Override public boolean nextBoolean() { return delegate.nextBoolean(); }
        @Override public float nextFloat() { return delegate.nextFloat(); }
        @Override public double nextDouble() { return delegate.nextDouble(); }
        @Override public double nextGaussian() { return delegate.nextGaussian(); }
    }

    private static final class Plot implements InvocationHandler {
        private final ServerLevel server;
        private final Holder<Biome> biome;
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();
        private final Map<Long, Integer> tops = new HashMap<>();
        private final WorldGenLevel level = (WorldGenLevel) Proxy.newProxyInstance(
                WorldGenLevel.class.getClassLoader(), new Class<?>[]{WorldGenLevel.class}, this);
        Plot(ServerLevel server, Holder<Biome> biome) { this.server = server; this.biome = biome; }
        private BlockState state(BlockPos p) {
            return blocks.getOrDefault(p, p.getY() == 0 ? Blocks.GRASS_BLOCK.defaultBlockState()
                    : p.getY() < 0 ? Blocks.DIRT.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        private static long column(int x, int z) { return ((long)x << 32) | (z & 0xffffffffL); }
        private static boolean inside(BlockPos p) {
            return p.getY() >= -64 && p.getY() < 320 && p.getX() >= -48 && p.getX() <= 64 && p.getZ() >= -48 && p.getZ() <= 64;
        }
        private void put(BlockPos p, BlockState state) {
            if (!inside(p)) throw new AssertionError("Vegetation wrote outside bounded plot: " + p);
            blocks.put(p.immutable(), state);
            var column = column(p.getX(), p.getZ());
            int top = tops.getOrDefault(column, 0);
            if (state.blocksMotion() && p.getY() >= top) tops.put(column, p.getY());
            else if (!state.blocksMotion() && p.getY() == top) {
                while (top > -64 && !state(new BlockPos(p.getX(), top, p.getZ())).blocksMotion()) top--;
                tops.put(column, top);
            }
        }
        int trunkGroups(Block log) {
            Set<BlockPos> remaining = new HashSet<>();
            blocks.forEach((pos, state) -> { if (pos.getY() == 1 && state.is(log)) remaining.add(pos); });
            int groups=0;
            while (!remaining.isEmpty()) {
                groups++;
                var queue = new ArrayDeque<BlockPos>();
                var root = remaining.iterator().next();
                remaining.remove(root); queue.add(root);
                while (!queue.isEmpty()) {
                    var p = queue.removeFirst();
                    for (var direction : Direction.Plane.HORIZONTAL) {
                        var neighbor = p.relative(direction);
                        if (remaining.remove(neighbor)) queue.addLast(neighbor);
                    }
                }
            }
            return groups;
        }
        @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch(method.getName()) {
                case "getLevel" -> server;
                case "registryAccess" -> server.registryAccess();
                case "getBiome", "getNoiseBiome" -> biome;
                case "getBlockState" -> state((BlockPos)args[0]);
                case "getFluidState" -> state((BlockPos)args[0]).getFluidState();
                case "isEmptyBlock" -> state((BlockPos)args[0]).isAir();
                case "getMinBuildHeight" -> -64;
                case "getMaxBuildHeight" -> 320;
                case "getHeight" -> args == null || args.length == 0 ? 384
                        : tops.getOrDefault(column((int)args[1], (int)args[2]), 0) + 1;
                case "isOutsideBuildHeight" -> args[0] instanceof BlockPos p
                        ? p.getY() < -64 || p.getY() >= 320 : (int)args[0] < -64 || (int)args[0] >= 320;
                case "ensureCanWrite" -> inside((BlockPos)args[0]);
                case "hasChunk", "hasChunkAt", "hasChunksAt" -> true;
                case "setBlock" -> { put((BlockPos)args[0], (BlockState)args[1]); yield true; }
                case "getBlockEntity" -> null;
                case "getSeed" -> 0L;
                case "getRawBrightness", "getMaxLocalRawBrightness" -> 15;
                case "isClientSide" -> false;
                case "toString" -> "Bounded controlled vegetation plot";
                default -> {
                    if (method.isDefault()) yield InvocationHandler.invokeDefault(proxy, method, args);
                    throw new UnsupportedOperationException(method.toString());
                }
            };
        }
    }
}
