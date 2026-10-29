package thaumcraft.port.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.content.PortParticles;

/** TC6 emits flames every client tick and refreshes its white core every ten ticks. */
public final class NitorEntity extends BlockEntity {
    private int ticks;
    public NitorEntity(BlockPos pos,BlockState state){super(MachineContent.NITOR_ENTITY.get(),pos,state);}
    public void tick(){
        if(level==null||!level.isClientSide||!(getBlockState().getBlock() instanceof NitorBlock block))return;
        var r=level.random;double x=worldPosition.getX()+.5,y=worldPosition.getY()+.45,z=worldPosition.getZ()+.5;
        level.addParticle(PortParticles.get("nitor_"+block.color.getName()),x+r.nextGaussian()*.025,y+r.nextGaussian()*.025,z+r.nextGaussian()*.025,r.nextGaussian()*.0025,r.nextFloat()*.06,r.nextGaussian()*.0025);
        if(ticks++%10==0)level.addParticle(PortParticles.get("nitor_core"),x,worldPosition.getY()+.49,z,0,0,0);
    }
}
