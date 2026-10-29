/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Predicate
 *  net.minecraft.block.Block
 *  net.minecraft.block.material.Material
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.block.state.pattern.BlockMatcher
 *  net.minecraft.entity.Entity
 *  net.minecraft.init.Blocks
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraft.world.biome.Biome
 *  net.minecraft.world.chunk.IChunkProvider
 *  net.minecraft.world.gen.IChunkGenerator
 *  net.minecraft.world.gen.structure.MapGenScatteredFeature
 *  net.minecraftforge.common.BiomeDictionary
 *  net.minecraftforge.common.BiomeDictionary$Type
 *  net.minecraftforge.fml.common.IWorldGenerator
 */
package thaumcraft.common.world;

import com.google.common.base.Predicate;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.IWorldGenerator;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.blocks.world.ore.ShardType;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.monster.cult.EntityCultistPortalLesser;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.world.aura.AuraHandler;
import thaumcraft.common.world.biomes.BiomeHandler;
import thaumcraft.common.world.objects.WorldGenCustomFlowers;
import thaumcraft.common.world.objects.WorldGenGreatwoodTrees;
import thaumcraft.common.world.objects.WorldGenMound;
import thaumcraft.common.world.objects.WorldGenSilverwoodTrees;

public class ThaumcraftWorldGenerator
implements IWorldGenerator {
    public static ThaumcraftWorldGenerator INSTANCE = new ThaumcraftWorldGenerator();
    private final Predicate<IBlockState> predicate = BlockMatcher.func_177642_a((Block)Blocks.field_150348_b);

    public void initialize() {
    }

    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        this.worldGeneration(random, chunkX, chunkZ, world, true);
        AuraHandler.generateAura(chunkProvider.func_186025_d(chunkX, chunkZ), random);
    }

    public void worldGeneration(Random random, int chunkX, int chunkZ, World world, boolean newGen) {
        if (world.field_73011_w.getDimension() == ModConfig.CONFIG_WORLD.dimensionOuterId) {
            world.func_72964_e(chunkX, chunkZ).func_76630_e();
        } else {
            this.generateAll(world, random, chunkX, chunkZ, newGen);
            if (world.field_73011_w.getDimension() == -1) {
                this.generateNether(world, random, chunkX, chunkZ, newGen);
            } else if (world.field_73011_w.getDimension() == ModConfig.CONFIG_WORLD.overworldDim) {
                this.generateSurface(world, random, chunkX, chunkZ, newGen);
            }
            if (!newGen) {
                world.func_72964_e(chunkX, chunkZ).func_76630_e();
            }
        }
    }

    private void generateSurface(World world, Random random, int chunkX, int chunkZ, boolean newGen) {
        int randPosZ;
        int randPosX;
        BlockPos p;
        int blacklist = BiomeHandler.getDimBlacklist(world.field_73011_w.getDimension());
        if (blacklist == -1 && ModConfig.CONFIG_WORLD.generateStructure && world.field_73011_w.getDimension() == ModConfig.CONFIG_WORLD.overworldDim && !world.func_72912_H().func_76067_t().func_77127_a().startsWith("flat") && (newGen || ModConfig.CONFIG_WORLD.regenStructure) && (p = world.func_175725_q(new BlockPos(randPosX = chunkX * 16 + 8 + MathHelper.func_76136_a((Random)random, (int)-4, (int)4), 0, randPosZ = chunkZ * 16 + 8 + MathHelper.func_76136_a((Random)random, (int)-4, (int)4))).func_177979_c(9)).func_177956_o() < world.func_72940_L()) {
            BlockPos p2;
            IBlockState bs;
            if (random.nextInt(100) == 0) {
                WorldGenMound mound = new WorldGenMound();
                mound.func_180709_b(world, random, p);
            } else if (random.nextInt(500) == 0 && ((bs = world.func_180495_p(p2 = p.func_177981_b(8))).func_185904_a() == Material.field_151578_c || bs.func_185904_a() == Material.field_151576_e || bs.func_185904_a() == Material.field_151595_p || bs.func_185904_a() == Material.field_151597_y)) {
                EntityCultistPortalLesser eg = new EntityCultistPortalLesser(world);
                eg.func_70107_b((double)p2.func_177958_n() + 0.5, p2.func_177956_o() + 1, (double)p2.func_177952_p() + 0.5);
                eg.func_180482_a(world.func_175649_E(new BlockPos((Entity)eg)), null);
                world.func_72838_d((Entity)eg);
            }
        }
    }

    private void generateNodes(World world, Random random, int chunkX, int chunkZ, boolean newGen, int blacklist) {
        if (blacklist != 0 && blacklist != 2 && ModConfig.CONFIG_WORLD.generateAura && (newGen || ModConfig.CONFIG_WORLD.regenAura)) {
            BlockPos var7 = null;
            try {
                var7 = new MapGenScatteredFeature().func_180706_b(world, world.func_175645_m(new BlockPos(chunkX * 16 + 8, 64, chunkZ * 16 + 8)), true);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private void generateVegetation(World world, Random random, int chunkX, int chunkZ, boolean newGen) {
        Biome bgb = world.func_180494_b(new BlockPos(chunkX * 16 + 8, 50, chunkZ * 16 + 8));
        if (BiomeHandler.getBiomeBlacklist(Biome.func_185362_a((Biome)bgb)) != -1) {
            return;
        }
        if (random.nextInt(80) == 3) {
            ThaumcraftWorldGenerator.generateSilverwood(world, random, chunkX, chunkZ);
        }
        if (random.nextInt(25) == 7) {
            ThaumcraftWorldGenerator.generateGreatwood(world, random, chunkX, chunkZ);
        }
        int randPosX = chunkX * 16 + 8;
        int randPosZ = chunkZ * 16 + 8;
        BlockPos bp = world.func_175645_m(new BlockPos(randPosX, 0, randPosZ));
        if (world.func_180494_b((BlockPos)bp).field_76752_A.func_177230_c() == Blocks.field_150354_m && world.func_180494_b(bp).func_180626_a(bp) > 1.0f && random.nextInt(30) == 0) {
            ThaumcraftWorldGenerator.generateFlowers(world, random, bp, BlocksTC.cinderpearl, 0);
        }
    }

    private void generateOres(World world, Random random, int chunkX, int chunkZ, boolean newGen) {
        IBlockState block;
        BlockPos pos;
        int randPosZ;
        int randPosY;
        int randPosX;
        int i;
        Biome bgb = world.func_180494_b(new BlockPos(chunkX * 16 + 8, 50, chunkZ * 16 + 8));
        if (BiomeHandler.getBiomeBlacklist(Biome.func_185362_a((Biome)bgb)) == 0 || BiomeHandler.getBiomeBlacklist(Biome.func_185362_a((Biome)bgb)) == 2) {
            return;
        }
        float density = (float)ModConfig.CONFIG_WORLD.oreDensity / 100.0f;
        if (world.field_73011_w.getDimension() == -1) {
            return;
        }
        if (ModConfig.CONFIG_WORLD.generateCinnabar && (newGen || ModConfig.CONFIG_WORLD.regenCinnabar)) {
            for (i = 0; i < Math.round(18.0f * density); ++i) {
                randPosX = chunkX * 16 + 8 + MathHelper.func_76136_a((Random)random, (int)-6, (int)6);
                pos = new BlockPos(randPosX, randPosY = random.nextInt(world.func_72800_K() / 5), randPosZ = chunkZ * 16 + 8 + MathHelper.func_76136_a((Random)random, (int)-6, (int)6));
                block = world.func_180495_p(pos);
                if (!block.func_177230_c().isReplaceableOreGen(block, (IBlockAccess)world, pos, this.predicate)) continue;
                world.func_180501_a(pos, BlocksTC.oreCinnabar.func_176223_P(), 2);
            }
        }
        if (ModConfig.CONFIG_WORLD.generateQuartz && (newGen || ModConfig.CONFIG_WORLD.regenQuartz)) {
            for (i = 0; i < Math.round(18.0f * density); ++i) {
                randPosX = chunkX * 16 + 8 + MathHelper.func_76136_a((Random)random, (int)-6, (int)6);
                pos = new BlockPos(randPosX, randPosY = random.nextInt(world.func_72800_K() / 4), randPosZ = chunkZ * 16 + 8 + MathHelper.func_76136_a((Random)random, (int)-6, (int)6));
                block = world.func_180495_p(pos);
                if (!block.func_177230_c().isReplaceableOreGen(block, (IBlockAccess)world, pos, this.predicate)) continue;
                world.func_180501_a(pos, BlocksTC.oreQuartz.func_176223_P(), 2);
            }
        }
        if (ModConfig.CONFIG_WORLD.generateAmber && (newGen || ModConfig.CONFIG_WORLD.regenAmber)) {
            for (i = 0; i < Math.round(20.0f * density); ++i) {
                int randPosZ2;
                int randPosY2;
                randPosX = chunkX * 16 + 8 + MathHelper.func_76136_a((Random)random, (int)-6, (int)6);
                pos = new BlockPos(randPosX, randPosY2 = world.func_175645_m(new BlockPos(randPosX, 0, randPosZ2 = chunkZ * 16 + 8 + MathHelper.func_76136_a((Random)random, (int)-6, (int)6))).func_177956_o() - random.nextInt(25), randPosZ2);
                block = world.func_180495_p(pos);
                if (!block.func_177230_c().isReplaceableOreGen(block, (IBlockAccess)world, pos, this.predicate)) continue;
                world.func_180501_a(pos, BlocksTC.oreAmber.func_176223_P(), 2);
            }
        }
        if (ModConfig.CONFIG_WORLD.generateCrystals && (newGen || ModConfig.CONFIG_WORLD.regenCrystals)) {
            int t = 8;
            int maxCrystals = Math.round(64.0f * density);
            int cc = 0;
            if (world.field_73011_w.getDimension() == -1) {
                t = 1;
            }
            for (int i2 = 0; i2 < Math.round((float)t * density); ++i2) {
                int randPosX2 = chunkX * 16 + 8 + MathHelper.func_76136_a((Random)random, (int)-6, (int)6);
                int randPosZ3 = chunkZ * 16 + 8 + MathHelper.func_76136_a((Random)random, (int)-6, (int)6);
                int randPosY3 = random.nextInt(Math.max(5, world.func_175645_m(new BlockPos(randPosX2, 0, randPosZ3)).func_177956_o() - 5));
                BlockPos bp = new BlockPos(randPosX2, randPosY3, randPosZ3);
                int md = random.nextInt(6);
                if (random.nextInt(3) == 0) {
                    Aspect tag = BiomeHandler.getRandomBiomeTag(Biome.func_185362_a((Biome)world.func_180494_b(bp)), random);
                    md = tag == null ? random.nextInt(6) : ShardType.getMetaByAspect(tag);
                }
                Block oreBlock = ShardType.byMetadata(md).getOre();
                for (int xx = -1; xx <= 1; ++xx) {
                    for (int yy = -1; yy <= 1; ++yy) {
                        for (int zz = -1; zz <= 1; ++zz) {
                            IBlockState bs;
                            Material bm;
                            if (random.nextInt(3) == 0 || (bm = (bs = world.func_180495_p(bp.func_177982_a(xx, yy, zz))).func_185904_a()).func_76224_d() || !world.func_175623_d(bp.func_177982_a(xx, yy, zz)) && !bs.func_177230_c().func_176200_f((IBlockAccess)world, bp.func_177982_a(xx, yy, zz)) || !BlockUtils.isBlockTouching((IBlockAccess)world, bp.func_177982_a(xx, yy, zz), Material.field_151576_e, true)) continue;
                            int amt = 1 + random.nextInt(3);
                            world.func_180501_a(bp.func_177982_a(xx, yy, zz), oreBlock.func_176203_a(amt), 0);
                            cc += amt;
                        }
                    }
                }
                if (cc > maxCrystals) break;
            }
        }
    }

    private void generateAll(World world, Random random, int chunkX, int chunkZ, boolean newGen) {
        boolean auraGen = false;
        int blacklist = BiomeHandler.getDimBlacklist(world.field_73011_w.getDimension());
        if (blacklist == -1 && ModConfig.CONFIG_WORLD.generateTrees && !world.func_72912_H().func_76067_t().func_77127_a().startsWith("flat") && (newGen || ModConfig.CONFIG_WORLD.regenTrees)) {
            this.generateVegetation(world, random, chunkX, chunkZ, newGen);
        }
        if (blacklist != 0 && blacklist != 2) {
            this.generateOres(world, random, chunkX, chunkZ, newGen);
        }
    }

    private void generateNether(World world, Random random, int chunkX, int chunkZ, boolean newGen) {
        boolean auraGen = false;
    }

    public static boolean generateFlowers(World world, Random random, BlockPos pos, Block block, int md) {
        WorldGenCustomFlowers flowers = new WorldGenCustomFlowers(block, md);
        return flowers.func_180709_b(world, random, pos);
    }

    public static boolean generateGreatwood(World world, Random random, int chunkX, int chunkZ) {
        int z;
        int x = chunkX * 16 + 8 + MathHelper.func_76136_a((Random)random, (int)-4, (int)4);
        BlockPos bp = world.func_175725_q(new BlockPos(x, 0, z = chunkZ * 16 + 8 + MathHelper.func_76136_a((Random)random, (int)-4, (int)4)));
        int bio = Biome.func_185362_a((Biome)world.func_180494_b(bp));
        if (BiomeHandler.getBiomeSupportsGreatwood(bio) > random.nextFloat()) {
            boolean t = new WorldGenGreatwoodTrees(false, random.nextInt(8) == 0).func_180709_b(world, random, bp);
            return t;
        }
        return false;
    }

    public static boolean generateSilverwood(World world, Random random, int chunkX, int chunkZ) {
        int x = chunkX * 16 + 8 + MathHelper.func_76136_a((Random)random, (int)-4, (int)4);
        int z = chunkZ * 16 + 8 + MathHelper.func_76136_a((Random)random, (int)-4, (int)4);
        BlockPos bp = world.func_175725_q(new BlockPos(x, 0, z));
        Biome bio = world.func_180494_b(bp);
        int bi = Biome.func_185362_a((Biome)world.func_180494_b(bp));
        if (BiomeHandler.getBiomeSupportsGreatwood(bi) / 2.0f > random.nextFloat() || !bio.equals(BiomeHandler.MAGICAL_FOREST) && BiomeDictionary.hasType((Biome)bio, (BiomeDictionary.Type)BiomeDictionary.Type.MAGICAL) || bio == Biome.func_150568_d((int)18) || bio == Biome.func_150568_d((int)28)) {
            boolean t = new WorldGenSilverwoodTrees(false, 7, 4).func_180709_b(world, random, bp);
            return t;
        }
        return false;
    }
}

