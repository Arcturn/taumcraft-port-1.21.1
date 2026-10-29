/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockSapling
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.monster.EntityCaveSpider
 *  net.minecraft.init.Blocks
 *  net.minecraft.tileentity.TileEntityChest
 *  net.minecraft.tileentity.TileEntityMobSpawner
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraft.world.gen.feature.WorldGenAbstractTree
 *  net.minecraft.world.storage.loot.LootTableList
 *  net.minecraftforge.common.IPlantable
 */
package thaumcraft.common.world.objects;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.IPlantable;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.lib.utils.BlockUtils;

public class WorldGenGreatwoodTrees
extends WorldGenAbstractTree {
    static final byte[] otherCoordPairs = new byte[]{2, 0, 0, 1, 2, 1};
    Random rand = new Random();
    World world;
    int[] basePos = new int[]{0, 0, 0};
    int heightLimit = 0;
    int height;
    double heightAttenuation = 0.618;
    double branchDensity = 1.0;
    double branchSlope = 0.38;
    double scaleWidth = 1.2;
    double leafDensity = 0.9;
    int trunkSize = 2;
    int heightLimitLimit = 11;
    int leafDistanceLimit = 4;
    int[][] leafNodes;
    boolean spiders = false;

    public WorldGenGreatwoodTrees(boolean par1, boolean spiders) {
        super(par1);
        this.spiders = spiders;
    }

    void generateLeafNodeList() {
        int var1;
        this.height = (int)((double)this.heightLimit * this.heightAttenuation);
        if (this.height >= this.heightLimit) {
            this.height = this.heightLimit - 1;
        }
        if ((var1 = (int)(1.382 + Math.pow(this.leafDensity * (double)this.heightLimit / 13.0, 2.0))) < 1) {
            var1 = 1;
        }
        int[][] var2 = new int[var1 * this.heightLimit][4];
        int var3 = this.basePos[1] + this.heightLimit - this.leafDistanceLimit;
        int var4 = 1;
        int var5 = this.basePos[1] + this.height;
        int var6 = var3 - this.basePos[1];
        var2[0][0] = this.basePos[0];
        var2[0][1] = var3--;
        var2[0][2] = this.basePos[2];
        var2[0][3] = var5;
        while (var6 >= 0) {
            float var8 = this.layerSize(var6);
            if (var8 < 0.0f) {
                --var3;
                --var6;
                continue;
            }
            double var9 = 0.5;
            for (int var7 = 0; var7 < var1; ++var7) {
                int[] var18;
                int var16;
                double var13;
                double var11 = this.scaleWidth * (double)var8 * ((double)this.rand.nextFloat() + 0.328);
                int var15 = MathHelper.func_76128_c((double)(var11 * Math.sin(var13 = (double)this.rand.nextFloat() * 2.0 * Math.PI) + (double)this.basePos[0] + var9));
                int[] var17 = new int[]{var15, var3, var16 = MathHelper.func_76128_c((double)(var11 * Math.cos(var13) + (double)this.basePos[2] + var9))};
                if (this.checkBlockLine(var17, var18 = new int[]{var15, var3 + this.leafDistanceLimit, var16}) != -1) continue;
                int[] var19 = new int[]{this.basePos[0], this.basePos[1], this.basePos[2]};
                double var20 = Math.sqrt(Math.pow(Math.abs(this.basePos[0] - var17[0]), 2.0) + Math.pow(Math.abs(this.basePos[2] - var17[2]), 2.0));
                double var22 = var20 * this.branchSlope;
                var19[1] = (double)var17[1] - var22 > (double)var5 ? var5 : (int)((double)var17[1] - var22);
                if (this.checkBlockLine(var19, var17) != -1) continue;
                var2[var4][0] = var15;
                var2[var4][1] = var3;
                var2[var4][2] = var16;
                var2[var4][3] = var19[1];
                ++var4;
            }
            --var3;
            --var6;
        }
        this.leafNodes = new int[var4][4];
        System.arraycopy(var2, 0, this.leafNodes, 0, var4);
    }

    void genTreeLayer(int par1, int par2, int par3, float par4, byte par5, Block par6) {
        int var7 = (int)((double)par4 + 0.618);
        byte var8 = otherCoordPairs[par5];
        byte var9 = otherCoordPairs[par5 + 3];
        int[] var10 = new int[]{par1, par2, par3};
        int[] var11 = new int[]{0, 0, 0};
        int var13 = -var7;
        var11[par5] = var10[par5];
        for (int var12 = -var7; var12 <= var7; ++var12) {
            var11[var8] = var10[var8] + var12;
            var13 = -var7;
            while (var13 <= var7) {
                double var15 = Math.pow((double)Math.abs(var12) + 0.5, 2.0) + Math.pow((double)Math.abs(var13) + 0.5, 2.0);
                if (var15 > (double)(par4 * par4)) {
                    ++var13;
                    continue;
                }
                try {
                    var11[var9] = var10[var9] + var13;
                    IBlockState state = this.world.func_180495_p(new BlockPos(var11[0], var11[1], var11[2]));
                    Block block = state.func_177230_c();
                    if (!(block != Blocks.field_150350_a && block != BlocksTC.leafGreatwood || block != null && !block.canBeReplacedByLeaves(state, (IBlockAccess)this.world, new BlockPos(var11[0], var11[1], var11[2])))) {
                        this.func_175903_a(this.world, new BlockPos(var11[0], var11[1], var11[2]), par6.func_176223_P());
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
                ++var13;
            }
        }
    }

    float layerSize(int par1) {
        if ((double)par1 < (double)this.heightLimit * 0.3) {
            return -1.618f;
        }
        float var2 = (float)this.heightLimit / 2.0f;
        float var3 = (float)this.heightLimit / 2.0f - (float)par1;
        float var4 = var3 == 0.0f ? var2 : (Math.abs(var3) >= var2 ? 0.0f : (float)Math.sqrt(Math.pow(Math.abs(var2), 2.0) - Math.pow(Math.abs(var3), 2.0)));
        return var4 *= 0.5f;
    }

    float leafSize(int par1) {
        return par1 >= 0 && par1 < this.leafDistanceLimit ? (par1 != 0 && par1 != this.leafDistanceLimit - 1 ? 3.0f : 2.0f) : -1.0f;
    }

    void generateLeafNode(int par1, int par2, int par3) {
        int var5 = par2 + this.leafDistanceLimit;
        for (int var4 = par2; var4 < var5; ++var4) {
            float var6 = this.leafSize(var4 - par2);
            this.genTreeLayer(par1, var4, par3, var6, (byte)1, BlocksTC.leafGreatwood);
        }
    }

    void placeBlockLine(int[] par1ArrayOfInteger, int[] par2ArrayOfInteger, Block par3) {
        int[] var4 = new int[]{0, 0, 0};
        int var6 = 0;
        for (int var5 = 0; var5 < 3; var5 = (int)((byte)(var5 + 1))) {
            var4[var5] = par2ArrayOfInteger[var5] - par1ArrayOfInteger[var5];
            if (Math.abs(var4[var5]) <= Math.abs(var4[var6])) continue;
            var6 = var5;
        }
        if (var4[var6] != 0) {
            byte var7 = otherCoordPairs[var6];
            byte var8 = otherCoordPairs[var6 + 3];
            int var9 = var4[var6] > 0 ? 1 : -1;
            double var10 = (double)var4[var7] / (double)var4[var6];
            double var12 = (double)var4[var8] / (double)var4[var6];
            int[] var14 = new int[]{0, 0, 0};
            int var16 = var4[var6] + var9;
            for (int var15 = 0; var15 != var16; var15 += var9) {
                int var19;
                var14[var6] = MathHelper.func_76128_c((double)((double)(par1ArrayOfInteger[var6] + var15) + 0.5));
                var14[var7] = MathHelper.func_76128_c((double)((double)par1ArrayOfInteger[var7] + (double)var15 * var10 + 0.5));
                var14[var8] = MathHelper.func_76128_c((double)((double)par1ArrayOfInteger[var8] + (double)var15 * var12 + 0.5));
                int var17 = 1;
                int var18 = Math.abs(var14[0] - par1ArrayOfInteger[0]);
                int var20 = Math.max(var18, var19 = Math.abs(var14[2] - par1ArrayOfInteger[2]));
                if (var20 > 0) {
                    if (var18 == var20) {
                        var17 = 0;
                    } else if (var19 == var20) {
                        var17 = 2;
                    }
                }
                if (!this.isReplaceable(this.world, new BlockPos(var14[0], var14[1], var14[2]))) continue;
                this.func_175903_a(this.world, new BlockPos(var14[0], var14[1], var14[2]), par3.func_176203_a(var17));
            }
        }
    }

    void generateLeaves() {
        int var2 = this.leafNodes.length;
        for (int var1 = 0; var1 < var2; ++var1) {
            int var3 = this.leafNodes[var1][0];
            int var4 = this.leafNodes[var1][1];
            int var5 = this.leafNodes[var1][2];
            this.generateLeafNode(var3, var4, var5);
        }
    }

    boolean leafNodeNeedsBase(int par1) {
        return (double)par1 >= (double)this.heightLimit * 0.2;
    }

    void generateTrunk() {
        int var1 = this.basePos[0];
        int var2 = this.basePos[1];
        int var3 = this.basePos[1] + this.height;
        int var4 = this.basePos[2];
        int[] var5 = new int[]{var1, var2, var4};
        int[] var6 = new int[]{var1, var3, var4};
        this.placeBlockLine(var5, var6, BlocksTC.logGreatwood);
        if (this.trunkSize == 2) {
            var5[0] = var5[0] + 1;
            var6[0] = var6[0] + 1;
            this.placeBlockLine(var5, var6, BlocksTC.logGreatwood);
            var5[2] = var5[2] + 1;
            var6[2] = var6[2] + 1;
            this.placeBlockLine(var5, var6, BlocksTC.logGreatwood);
            var5[0] = var5[0] + -1;
            var6[0] = var6[0] + -1;
            this.placeBlockLine(var5, var6, BlocksTC.logGreatwood);
        }
    }

    void generateLeafNodeBases() {
        int var2 = this.leafNodes.length;
        int[] var3 = new int[]{this.basePos[0], this.basePos[1], this.basePos[2]};
        for (int var1 = 0; var1 < var2; ++var1) {
            int[] var4 = this.leafNodes[var1];
            int[] var5 = new int[]{var4[0], var4[1], var4[2]};
            var3[1] = var4[3];
            int var6 = var3[1] - this.basePos[1];
            if (!this.leafNodeNeedsBase(var6)) continue;
            this.placeBlockLine(var3, var5, BlocksTC.logGreatwood);
        }
    }

    int checkBlockLine(int[] par1ArrayOfInteger, int[] par2ArrayOfInteger) {
        int var14;
        int[] var3 = new int[]{0, 0, 0};
        int var5 = 0;
        for (int var4 = 0; var4 < 3; var4 = (int)((byte)(var4 + 1))) {
            var3[var4] = par2ArrayOfInteger[var4] - par1ArrayOfInteger[var4];
            if (Math.abs(var3[var4]) <= Math.abs(var3[var5])) continue;
            var5 = var4;
        }
        if (var3[var5] == 0) {
            return -1;
        }
        byte var6 = otherCoordPairs[var5];
        byte var7 = otherCoordPairs[var5 + 3];
        int var8 = var3[var5] > 0 ? 1 : -1;
        double var9 = (double)var3[var6] / (double)var3[var5];
        double var11 = (double)var3[var7] / (double)var3[var5];
        int[] var13 = new int[]{0, 0, 0};
        int var15 = var3[var5] + var8;
        for (var14 = 0; var14 != var15; var14 += var8) {
            var13[var5] = par1ArrayOfInteger[var5] + var14;
            var13[var6] = MathHelper.func_76128_c((double)((double)par1ArrayOfInteger[var6] + (double)var14 * var9));
            var13[var7] = MathHelper.func_76128_c((double)((double)par1ArrayOfInteger[var7] + (double)var14 * var11));
            try {
                Block var16 = this.world.func_180495_p(new BlockPos(var13[0], var13[1], var13[2])).func_177230_c();
                if (var16 == Blocks.field_150350_a || var16 == BlocksTC.leafGreatwood) continue;
                break;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return var14 == var15 ? -1 : Math.abs(var14);
    }

    boolean validTreeLocation(int x, int z) {
        int[] var1 = new int[]{this.basePos[0] + x, this.basePos[1], this.basePos[2] + z};
        int[] var2 = new int[]{this.basePos[0] + x, this.basePos[1] + this.heightLimit - 1, this.basePos[2] + z};
        try {
            IBlockState state = this.world.func_180495_p(new BlockPos(this.basePos[0] + x, this.basePos[1] - 1, this.basePos[2] + z));
            Block var3 = state.func_177230_c();
            boolean isSoil = var3.canSustainPlant(state, (IBlockAccess)this.world, new BlockPos(this.basePos[0] + x, this.basePos[1] - 1, this.basePos[2] + z), EnumFacing.UP, (IPlantable)((BlockSapling)Blocks.field_150345_g));
            if (!isSoil) {
                return false;
            }
            int var4 = this.checkBlockLine(var1, var2);
            if (var4 == -1) {
                return true;
            }
            if (var4 < 6) {
                return false;
            }
            this.heightLimit = var4;
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public void setScale(double par1, double par3, double par5) {
    }

    public boolean func_180709_b(World par1World, Random par2Random, BlockPos pos) {
        this.world = par1World;
        long var6 = par2Random.nextLong();
        this.rand.setSeed(var6);
        this.basePos[0] = pos.func_177958_n();
        this.basePos[1] = pos.func_177956_o();
        this.basePos[2] = pos.func_177952_p();
        if (this.heightLimit == 0) {
            this.heightLimit = this.heightLimitLimit + this.rand.nextInt(this.heightLimitLimit);
        }
        int x = 0;
        int z = 0;
        for (x = 0; x < this.trunkSize; ++x) {
            for (z = 0; z < this.trunkSize; ++z) {
                if (this.validTreeLocation(x, z)) continue;
                this.world = null;
                return false;
            }
        }
        this.world.func_175698_g(pos);
        this.generateLeafNodeList();
        this.generateLeaves();
        this.generateLeafNodeBases();
        this.generateTrunk();
        this.scaleWidth = 1.66;
        this.basePos[0] = pos.func_177958_n();
        this.basePos[1] = pos.func_177956_o() + this.height;
        this.basePos[2] = pos.func_177952_p();
        this.generateLeafNodeList();
        this.generateLeaves();
        this.generateLeafNodeBases();
        this.generateTrunk();
        if (this.spiders) {
            this.world.func_175656_a(pos.func_177977_b(), Blocks.field_150474_ac.func_176223_P());
            TileEntityMobSpawner var14 = (TileEntityMobSpawner)par1World.func_175625_s(pos.func_177977_b());
            if (var14 != null) {
                var14.func_145881_a().func_190894_a(EntityList.func_191306_a(EntityCaveSpider.class));
                for (int a = 0; a < 50; ++a) {
                    int zz;
                    int yy;
                    int xx = pos.func_177958_n() - 7 + par2Random.nextInt(14);
                    if (!par1World.func_175623_d(new BlockPos(xx, yy = pos.func_177956_o() + par2Random.nextInt(10), zz = pos.func_177952_p() - 7 + par2Random.nextInt(14))) || !BlockUtils.isBlockTouching((IBlockAccess)par1World, new BlockPos(xx, yy, zz), BlocksTC.leafGreatwood) && !BlockUtils.isBlockTouching((IBlockAccess)par1World, new BlockPos(xx, yy, zz), BlocksTC.logGreatwood)) continue;
                    this.world.func_175656_a(new BlockPos(xx, yy, zz), Blocks.field_150321_G.func_176223_P());
                }
                par1World.func_175656_a(pos.func_177979_c(2), Blocks.field_150486_ae.func_176223_P());
                TileEntityChest var16 = (TileEntityChest)par1World.func_175625_s(pos.func_177979_c(2));
                if (var16 != null) {
                    var16.func_189404_a(LootTableList.field_186422_d, this.rand.nextLong());
                }
            }
        }
        this.world = null;
        return true;
    }
}

