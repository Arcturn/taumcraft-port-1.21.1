package thaumcraft.port.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class CentrifugeBlock extends Block implements EntityBlock {
    public CentrifugeBlock(Properties p){super(p);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new CentrifugeEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return t==EssentiaContent.CENTRIFUGE_ENTITY.get()?(w,p,st,e)->((CentrifugeEntity)e).tick():null;}
}
