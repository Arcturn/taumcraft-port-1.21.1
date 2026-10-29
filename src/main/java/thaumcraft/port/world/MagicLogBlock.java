package thaumcraft.port.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class MagicLogBlock extends RotatedPillarBlock {
    private final boolean log;
    public MagicLogBlock(Properties p){this(p,true);}
    public MagicLogBlock(Properties p,boolean log){super(p);this.log=log;}
    public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction face){return log?5:20;}
    public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction face){return 5;}
}
