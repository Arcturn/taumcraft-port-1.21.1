/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockFlower$EnumFlowerType
 *  net.minecraft.block.BlockTallGrass$EnumType
 *  net.minecraft.entity.monster.EntityEnderman
 *  net.minecraft.entity.monster.EntityVex
 *  net.minecraft.entity.monster.EntityWitch
 *  net.minecraft.entity.passive.EntityHorse
 *  net.minecraft.entity.passive.EntityWolf
 *  net.minecraft.init.Blocks
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.Vec3i
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraft.world.biome.Biome
 *  net.minecraft.world.biome.Biome$BiomeProperties
 *  net.minecraft.world.biome.Biome$SpawnListEntry
 *  net.minecraft.world.gen.feature.WorldGenAbstractTree
 *  net.minecraft.world.gen.feature.WorldGenBigMushroom
 *  net.minecraft.world.gen.feature.WorldGenBlockBlob
 *  net.minecraft.world.gen.feature.WorldGenTallGrass
 *  net.minecraft.world.gen.feature.WorldGenerator
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package thaumcraft.common.world.biomes;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityVex;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenBigMushroom;
import net.minecraft.world.gen.feature.WorldGenBlockBlob;
import net.minecraft.world.gen.feature.WorldGenTallGrass;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.entities.monster.EntityWisp;
import thaumcraft.common.lib.utils.Utils;
import thaumcraft.common.world.objects.WorldGenBigMagicTree;
import thaumcraft.common.world.objects.WorldGenGreatwoodTrees;
import thaumcraft.common.world.objects.WorldGenSilverwoodTrees;

public class BiomeGenMagicalForest
extends Biome {
    protected WorldGenBigMagicTree bigTree;
    private static final WorldGenBlockBlob blobs = new WorldGenBlockBlob(Blocks.field_150341_Y, 0);

    public BiomeGenMagicalForest(Biome.BiomeProperties par1) {
        super(par1);
        this.setRegistryName("thaumcraft", "magical_forest");
        this.bigTree = new WorldGenBigMagicTree(false);
        this.field_76762_K.add(new Biome.SpawnListEntry(EntityWolf.class, 2, 1, 3));
        this.field_76762_K.add(new Biome.SpawnListEntry(EntityHorse.class, 2, 1, 3));
        this.field_76761_J.add(new Biome.SpawnListEntry(EntityWitch.class, 3, 1, 1));
        this.field_76761_J.add(new Biome.SpawnListEntry(EntityEnderman.class, 3, 1, 1));
        this.field_76761_J.add(new Biome.SpawnListEntry(EntityVex.class, 1, 1, 1));
        if (ModConfig.CONFIG_WORLD.allowSpawnPech) {
            this.field_76761_J.add(new Biome.SpawnListEntry(EntityPech.class, 20, 1, 2));
        }
        if (ModConfig.CONFIG_WORLD.allowSpawnWisp) {
            this.field_76761_J.add(new Biome.SpawnListEntry(EntityWisp.class, 20, 1, 2));
        }
        this.field_76760_I.field_76832_z = 2;
        this.field_76760_I.field_76802_A = 10;
        this.field_76760_I.field_76803_B = 12;
        this.field_76760_I.field_76833_y = 6;
        this.field_76760_I.field_76798_D = 6;
    }

    public WorldGenAbstractTree func_150567_a(Random par1Random) {
        return par1Random.nextInt(18) == 0 ? new WorldGenSilverwoodTrees(false, 8, 5) : (par1Random.nextInt(12) == 0 ? new WorldGenGreatwoodTrees(false, par1Random.nextInt(8) == 0) : this.bigTree);
    }

    public WorldGenerator func_76730_b(Random par1Random) {
        return par1Random.nextInt(4) == 0 ? new WorldGenTallGrass(BlockTallGrass.EnumType.FERN) : new WorldGenTallGrass(BlockTallGrass.EnumType.GRASS);
    }

    @SideOnly(value=Side.CLIENT)
    public int func_180627_b(BlockPos p_180627_1_) {
        return ModConfig.CONFIG_GRAPHICS.blueBiome ? 0x66AACC : 5635969;
    }

    @SideOnly(value=Side.CLIENT)
    public int func_180625_c(BlockPos p_180625_1_) {
        return ModConfig.CONFIG_GRAPHICS.blueBiome ? 0x77CCEE : 6750149;
    }

    public int getWaterColorMultiplier() {
        return 30702;
    }

    public void func_180624_a(World world, Random random, BlockPos pos) {
        int l;
        for (int a = 0; a < 3; ++a) {
            BlockPos pp = new BlockPos((Vec3i)pos);
            pp = pp.func_177982_a(4 + random.nextInt(8), 0, 4 + random.nextInt(8));
            pp = world.func_175645_m(pp);
            while (pp.func_177956_o() > 30 && world.func_180495_p(pp).func_177230_c() != Blocks.field_150349_c) {
                pp = pp.func_177977_b();
            }
            Block l1 = world.func_180495_p(pp).func_177230_c();
            if (l1 != Blocks.field_150349_c) continue;
            world.func_180501_a(pp, BlocksTC.grassAmbient.func_176223_P(), 2);
            break;
        }
        int k = random.nextInt(3);
        for (l = 0; l < k; ++l) {
            BlockPos p2 = new BlockPos((Vec3i)pos);
            p2 = p2.func_177982_a(random.nextInt(16) + 8, 0, random.nextInt(16) + 8);
            p2 = world.func_175645_m(p2);
            blobs.func_180709_b(world, random, p2);
        }
        for (k = 0; k < 4; ++k) {
            for (l = 0; l < 4; ++l) {
                if (random.nextInt(40) != 0) continue;
                BlockPos p2 = new BlockPos((Vec3i)pos);
                p2 = p2.func_177982_a(k * 4 + 1 + 8 + random.nextInt(3), 0, l * 4 + 1 + 8 + random.nextInt(3));
                p2 = world.func_175645_m(p2);
                WorldGenBigMushroom worldgenbigmushroom = new WorldGenBigMushroom();
                worldgenbigmushroom.func_180709_b(world, random, p2);
            }
        }
        try {
            super.func_180624_a(world, random, pos);
        }
        catch (Exception p2) {
            // empty catch block
        }
        for (int a = 0; a < 8; ++a) {
            BlockPos p2 = new BlockPos((Vec3i)pos);
            p2 = p2.func_177982_a(random.nextInt(16), 0, random.nextInt(16));
            p2 = world.func_175645_m(p2);
            while (p2.func_177956_o() > 50 && world.func_180495_p(p2).func_177230_c() != Blocks.field_150349_c) {
                p2 = p2.func_177977_b();
            }
            Block l2 = world.func_180495_p(p2).func_177230_c();
            if (l2 != Blocks.field_150349_c || !world.func_180495_p(p2.func_177984_a()).func_177230_c().func_176200_f((IBlockAccess)world, p2.func_177984_a()) || !this.isBlockAdjacentToWood((IBlockAccess)world, p2.func_177984_a())) continue;
            world.func_180501_a(p2.func_177984_a(), BlocksTC.vishroom.func_176223_P(), 2);
        }
    }

    private boolean isBlockAdjacentToWood(IBlockAccess world, BlockPos pos) {
        boolean count = false;
        for (int xx = -1; xx <= 1; ++xx) {
            for (int yy = -1; yy <= 1; ++yy) {
                for (int zz = -1; zz <= 1; ++zz) {
                    if (xx == 0 && yy == 0 && zz == 0 || !Utils.isWoodLog(world, pos.func_177982_a(xx, yy, zz))) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public BlockFlower.EnumFlowerType func_180623_a(Random rand, BlockPos pos) {
        double d0 = MathHelper.func_151237_a((double)((1.0 + field_180281_af.func_151601_a((double)pos.func_177958_n() / 48.0, (double)pos.func_177952_p() / 48.0)) / 2.0), (double)0.0, (double)0.9999);
        return BlockFlower.EnumFlowerType.values()[(int)(d0 * (double)BlockFlower.EnumFlowerType.values().length)];
    }
}

