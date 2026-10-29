/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.init.Blocks
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.World
 *  net.minecraft.world.gen.feature.WorldGenerator
 */
package thaumcraft.common.world.objects;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

public class WorldGenCustomFlowers
extends WorldGenerator {
    private Block plantBlock;
    private int plantBlockMeta;

    public WorldGenCustomFlowers(Block bi, int md) {
        this.plantBlock = bi;
        this.plantBlockMeta = md;
    }

    public boolean func_180709_b(World world, Random par2Random, BlockPos pos) {
        for (int var6 = 0; var6 < 18; ++var6) {
            int var9;
            int var8;
            int var7 = pos.func_177958_n() + par2Random.nextInt(8) - par2Random.nextInt(8);
            BlockPos bp = new BlockPos(var7, var8 = pos.func_177956_o() + par2Random.nextInt(4) - par2Random.nextInt(4), var9 = pos.func_177952_p() + par2Random.nextInt(8) - par2Random.nextInt(8));
            if (!world.func_175623_d(bp) || world.func_180495_p(bp.func_177977_b()).func_177230_c() != Blocks.field_150349_c && world.func_180495_p(bp.func_177977_b()).func_177230_c() != Blocks.field_150354_m) continue;
            world.func_180501_a(bp, this.plantBlock.func_176203_a(this.plantBlockMeta), 3);
        }
        return true;
    }
}

