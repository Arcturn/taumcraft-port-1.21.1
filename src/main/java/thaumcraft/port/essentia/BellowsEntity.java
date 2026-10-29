package thaumcraft.port.essentia;

import net.minecraft.core.*;
import net.minecraft.sounds.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BellowsEntity extends BlockEntity {
    public float inflation=1,previousInflation=1;
    private boolean expanding,animationStarted;
    private int pulse;
    public BellowsEntity(BlockPos p,BlockState s){super(EssentiaContent.BELLOWS_ENTITY.get(),p,s);}
    public static int count(Level l,BlockPos p,Direction excluded){
        int count=0;for(Direction d:Direction.values())if(d!=excluded&&(excluded==null||d.getAxis().isHorizontal())){
            var q=p.relative(d);if(!l.hasChunkAt(q))continue;var s=l.getBlockState(q);
            if(s.is(EssentiaContent.BELLOWS.get())&&s.getValue(BellowsBlock.FACING)==d.getOpposite()&&s.getValue(BellowsBlock.ENABLED))count++;
        }return count;
    }
    public void tick(){
        if(level==null)return;
        boolean enabled=getBlockState().getValue(BellowsBlock.ENABLED);
        if(!level.isClientSide){
            if(enabled&&++pulse>=2){pulse=0;BlockPos target=worldPosition.relative(getBlockState().getValue(BellowsBlock.FACING));
                if(level.hasChunkAt(target)&&level.getBlockEntity(target) instanceof net.minecraft.world.level.block.entity.FurnaceBlockEntity furnace)FurnaceBellows.advance(furnace);
            }
            return;
        }
        previousInflation=inflation;if(!enabled)return;
        if(!animationStarted){animationStarted=true;inflation=.35f+level.random.nextFloat()*.55f;previousInflation=inflation;}
        if(inflation>.35f&&!expanding)inflation-=.075f;
        if(inflation<=.35f&&!expanding)expanding=true;
        if(inflation<1&&expanding)inflation+=.025f;
        if(inflation>=1&&expanding){expanding=false;level.playLocalSound(worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),SoundEvents.GHAST_SHOOT,SoundSource.BLOCKS,.01f,.5f+(level.random.nextFloat()-level.random.nextFloat())*.2f,false);}
    }
}
