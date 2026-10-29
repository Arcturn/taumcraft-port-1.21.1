package thaumcraft.port.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

public final class MagicSaplingBlock extends SaplingBlock {
    public MagicSaplingBlock(TreeGrower g,Properties p){super(g,p);}
    @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
        if(l.getMaxLocalRawBrightness(p.above())>=9&&r.nextInt(7)==0)advanceTree(l,p,s,r);
    }
    @Override public boolean isBonemealSuccess(Level l,RandomSource r,BlockPos p,BlockState s){return r.nextFloat()<.25f;}
}
