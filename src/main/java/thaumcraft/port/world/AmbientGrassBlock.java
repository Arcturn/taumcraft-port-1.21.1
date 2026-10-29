package thaumcraft.port.world;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
public final class AmbientGrassBlock extends GrassBlock {
    public AmbientGrassBlock(Properties p){super(p);}
    @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){
        if(l.getMaxLocalRawBrightness(p.above())<5&&r.nextInt(13)>4){
            var q=p.offset(r.nextInt(17)-8,5,r.nextInt(17)-8);for(int i=0;i<10&&l.isEmptyBlock(q.below());i++)q=q.below();
            if(l.isEmptyBlock(q))l.addParticle(ParticleTypes.END_ROD,q.getX()+.5,q.getY()+.3,q.getZ()+.5,0,.01,0);
        }
    }
}
