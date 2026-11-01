package thaumcraft.port.alchemy.material;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Always-lit, single TC6 candle; unlike the vanilla candle it does not require flint and steel. */
public final class TallowCandleBlock extends Block {
    public TallowCandleBlock(Properties p) { super(p.noCollission().noOcclusion().strength(.1f).sound(SoundType.WOOL).lightLevel(s->14)); }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return box(6,0,6,10,8,10); }
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p) { return l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP); }
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos q) { return d==Direction.DOWN&&!canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,d,n,l,p,q); }
    @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r) {
        l.addParticle(ParticleTypes.SMOKE,p.getX()+.5,p.getY()+.7,p.getZ()+.5,0,0,0);
        l.addParticle(ParticleTypes.FLAME,p.getX()+.5,p.getY()+.7,p.getZ()+.5,0,0,0);
    }
    public float infusionStability() { return .1f; }
}
