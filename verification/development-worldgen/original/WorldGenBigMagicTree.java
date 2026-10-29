/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockLeaves
 *  net.minecraft.block.BlockLog
 *  net.minecraft.block.BlockLog$EnumAxis
 *  net.minecraft.block.BlockSapling
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.init.Blocks
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraft.world.gen.feature.WorldGenAbstractTree
 *  net.minecraftforge.common.IPlantable
 */
package thaumcraft.common.world.objects;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraftforge.common.IPlantable;

public class WorldGenBigMagicTree
extends WorldGenAbstractTree {
    private Random field_175949_k;
    private World world;
    private BlockPos basePos = BlockPos.field_177992_a;
    int heightLimit;
    int height;
    double heightAttenuation = 0.6618;
    double branchSlope = 0.381;
    double scaleWidth = 1.25;
    double leafDensity = 0.9;
    int trunkSize = 1;
    int heightLimitLimit = 11;
    int leafDistanceLimit = 4;
    List field_175948_j;
    private static final String __OBFID = "CL_00000400";

    public WorldGenBigMagicTree(boolean p_i2008_1_) {
        super(p_i2008_1_);
    }

    void generateLeafNodeList() {
        int k;
        int i;
        this.height = (int)((double)this.heightLimit * this.heightAttenuation);
        if (this.height >= this.heightLimit) {
            this.height = this.heightLimit - 1;
        }
        if ((i = (int)(1.382 + Math.pow(this.leafDensity * (double)this.heightLimit / 13.0, 2.0))) < 1) {
            i = 1;
        }
        int j = this.basePos.func_177956_o() + this.height;
        this.field_175948_j = Lists.newArrayList();
        this.field_175948_j.add(new FoliageCoordinates(this.basePos.func_177981_b(k), j));
        for (k = this.heightLimit - this.leafDistanceLimit; k >= 0; --k) {
            float f = this.layerSize(k);
            if (!(f >= 0.0f)) continue;
            for (int l = 0; l < i; ++l) {
                BlockPos blockpos1;
                double d3;
                double d1;
                double d0 = this.scaleWidth * (double)f * ((double)this.field_175949_k.nextFloat() + 0.328);
                double d2 = d0 * Math.sin(d1 = (double)(this.field_175949_k.nextFloat() * 2.0f) * Math.PI) + 0.5;
                BlockPos blockpos = this.basePos.func_177963_a(d2, (double)(k - 1), d3 = d0 * Math.cos(d1) + 0.5);
                if (this.func_175936_a(blockpos, blockpos1 = blockpos.func_177981_b(this.leafDistanceLimit)) != -1) continue;
                int i1 = this.basePos.func_177958_n() - blockpos.func_177958_n();
                int j1 = this.basePos.func_177952_p() - blockpos.func_177952_p();
                double d4 = (double)blockpos.func_177956_o() - Math.sqrt(i1 * i1 + j1 * j1) * this.branchSlope;
                int k1 = d4 > (double)j ? j : (int)d4;
                BlockPos blockpos2 = new BlockPos(this.basePos.func_177958_n(), k1, this.basePos.func_177952_p());
                if (this.func_175936_a(blockpos2, blockpos) != -1) continue;
                this.field_175948_j.add(new FoliageCoordinates(blockpos, blockpos2.func_177956_o()));
            }
        }
    }

    void func_181631_a(BlockPos p_181631_1_, float p_181631_2_, IBlockState p_181631_3_) {
        int i = (int)((double)p_181631_2_ + 0.618);
        for (int j = -i; j <= i; ++j) {
            for (int k = -i; k <= i; ++k) {
                BlockPos blockpos;
                IBlockState state;
                if (!(Math.pow((double)Math.abs(j) + 0.5, 2.0) + Math.pow((double)Math.abs(k) + 0.5, 2.0) <= (double)(p_181631_2_ * p_181631_2_)) || !(state = this.world.func_180495_p(blockpos = p_181631_1_.func_177982_a(j, 0, k))).func_177230_c().isAir(state, (IBlockAccess)this.world, blockpos) && !state.func_177230_c().isLeaves(state, (IBlockAccess)this.world, blockpos)) continue;
                this.func_175903_a(this.world, blockpos, p_181631_3_);
            }
        }
    }

    float layerSize(int p_76490_1_) {
        if ((float)p_76490_1_ < (float)this.heightLimit * 0.3f) {
            return -1.0f;
        }
        float f = (float)this.heightLimit / 2.0f;
        float f1 = f - (float)p_76490_1_;
        float f2 = MathHelper.func_76129_c((float)(f * f - f1 * f1));
        if (f1 == 0.0f) {
            f2 = f;
        } else if (Math.abs(f1) >= f) {
            return 0.0f;
        }
        return f2 * 0.5f;
    }

    float leafSize(int p_76495_1_) {
        return p_76495_1_ >= 0 && p_76495_1_ < this.leafDistanceLimit ? (p_76495_1_ != 0 && p_76495_1_ != this.leafDistanceLimit - 1 ? 3.0f : 2.0f) : -1.0f;
    }

    void generateLeafNode(BlockPos pos) {
        for (int i = 0; i < this.leafDistanceLimit; ++i) {
            this.func_181631_a(pos.func_177981_b(i), this.leafSize(i), Blocks.field_150362_t.func_176223_P().func_177226_a((IProperty)BlockLeaves.field_176236_b, (Comparable)Boolean.valueOf(false)));
        }
    }

    void func_175937_a(BlockPos p_175937_1_, BlockPos p_175937_2_, Block p_175937_3_) {
        BlockPos blockpos2 = p_175937_2_.func_177982_a(-p_175937_1_.func_177958_n(), -p_175937_1_.func_177956_o(), -p_175937_1_.func_177952_p());
        int i = this.func_175935_b(blockpos2);
        float f = (float)blockpos2.func_177958_n() / (float)i;
        float f1 = (float)blockpos2.func_177956_o() / (float)i;
        float f2 = (float)blockpos2.func_177952_p() / (float)i;
        for (int j = 0; j <= i; ++j) {
            BlockPos blockpos3 = p_175937_1_.func_177963_a((double)(0.5f + (float)j * f), (double)(0.5f + (float)j * f1), (double)(0.5f + (float)j * f2));
            BlockLog.EnumAxis enumaxis = this.func_175938_b(p_175937_1_, blockpos3);
            this.func_175903_a(this.world, blockpos3, p_175937_3_.func_176223_P().func_177226_a((IProperty)BlockLog.field_176299_a, (Comparable)enumaxis));
        }
    }

    private int func_175935_b(BlockPos p_175935_1_) {
        int i = MathHelper.func_76130_a((int)p_175935_1_.func_177958_n());
        int j = MathHelper.func_76130_a((int)p_175935_1_.func_177956_o());
        int k = MathHelper.func_76130_a((int)p_175935_1_.func_177952_p());
        return k > i && k > j ? k : (j > i ? j : i);
    }

    private BlockLog.EnumAxis func_175938_b(BlockPos p_175938_1_, BlockPos p_175938_2_) {
        int j;
        BlockLog.EnumAxis enumaxis = BlockLog.EnumAxis.Y;
        int i = Math.abs(p_175938_2_.func_177958_n() - p_175938_1_.func_177958_n());
        int k = Math.max(i, j = Math.abs(p_175938_2_.func_177952_p() - p_175938_1_.func_177952_p()));
        if (k > 0) {
            if (i == k) {
                enumaxis = BlockLog.EnumAxis.X;
            } else if (j == k) {
                enumaxis = BlockLog.EnumAxis.Z;
            }
        }
        return enumaxis;
    }

    void func_175941_b() {
        for (FoliageCoordinates foliagecoordinates : this.field_175948_j) {
            this.generateLeafNode(foliagecoordinates);
        }
    }

    boolean leafNodeNeedsBase(int p_76493_1_) {
        return (double)p_76493_1_ >= (double)this.heightLimit * 0.2;
    }

    void func_175942_c() {
        BlockPos blockpos = this.basePos;
        BlockPos blockpos1 = this.basePos.func_177981_b(this.height);
        Block block = Blocks.field_150364_r;
        this.func_175937_a(blockpos, blockpos1, block);
        if (this.trunkSize == 2) {
            this.func_175937_a(blockpos.func_177974_f(), blockpos1.func_177974_f(), block);
            this.func_175937_a(blockpos.func_177974_f().func_177968_d(), blockpos1.func_177974_f().func_177968_d(), block);
            this.func_175937_a(blockpos.func_177968_d(), blockpos1.func_177968_d(), block);
        }
    }

    void func_175939_d() {
        for (FoliageCoordinates foliagecoordinates : this.field_175948_j) {
            int i = foliagecoordinates.func_177999_q();
            BlockPos blockpos = new BlockPos(this.basePos.func_177958_n(), i, this.basePos.func_177952_p());
            if (!this.leafNodeNeedsBase(i - this.basePos.func_177956_o())) continue;
            this.func_175937_a(blockpos, foliagecoordinates, Blocks.field_150364_r);
        }
    }

    int func_175936_a(BlockPos p_175936_1_, BlockPos p_175936_2_) {
        BlockPos blockpos2 = p_175936_2_.func_177982_a(-p_175936_1_.func_177958_n(), -p_175936_1_.func_177956_o(), -p_175936_1_.func_177952_p());
        int i = this.func_175935_b(blockpos2);
        float f = (float)blockpos2.func_177958_n() / (float)i;
        float f1 = (float)blockpos2.func_177956_o() / (float)i;
        float f2 = (float)blockpos2.func_177952_p() / (float)i;
        if (i == 0) {
            return -1;
        }
        for (int j = 0; j <= i; ++j) {
            BlockPos blockpos3 = p_175936_1_.func_177963_a((double)(0.5f + (float)j * f), (double)(0.5f + (float)j * f1), (double)(0.5f + (float)j * f2));
            if (this.isReplaceable(this.world, blockpos3)) continue;
            return j;
        }
        return -1;
    }

    public void func_175904_e() {
        this.leafDistanceLimit = 4;
    }

    public boolean func_180709_b(World worldIn, Random p_180709_2_, BlockPos p_180709_3_) {
        this.world = worldIn;
        this.basePos = p_180709_3_;
        this.field_175949_k = new Random(p_180709_2_.nextLong());
        if (this.heightLimit == 0) {
            this.heightLimit = 11 + this.field_175949_k.nextInt(this.heightLimitLimit);
        }
        if (!this.validTreeLocation()) {
            this.world = null;
            return false;
        }
        this.generateLeafNodeList();
        this.func_175941_b();
        this.func_175942_c();
        this.func_175939_d();
        this.world = null;
        return true;
    }

    private boolean validTreeLocation() {
        BlockPos down = this.basePos.func_177977_b();
        IBlockState state = this.world.func_180495_p(down);
        boolean isSoil = state.func_177230_c().canSustainPlant(state, (IBlockAccess)this.world, down, EnumFacing.UP, (IPlantable)((BlockSapling)Blocks.field_150345_g));
        if (!isSoil) {
            return false;
        }
        int i = this.func_175936_a(this.basePos, this.basePos.func_177981_b(this.heightLimit - 1));
        if (i == -1) {
            return true;
        }
        if (i < 6) {
            return false;
        }
        this.heightLimit = i;
        return true;
    }

    static class FoliageCoordinates
    extends BlockPos {
        private final int field_178000_b;
        private static final String __OBFID = "CL_00002001";

        public FoliageCoordinates(BlockPos p_i45635_1_, int p_i45635_2_) {
            super(p_i45635_1_.func_177958_n(), p_i45635_1_.func_177956_o(), p_i45635_1_.func_177952_p());
            this.field_178000_b = p_i45635_2_;
        }

        public int func_177999_q() {
            return this.field_178000_b;
        }
    }
}

