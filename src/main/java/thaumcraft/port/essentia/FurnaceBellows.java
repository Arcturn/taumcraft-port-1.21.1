package thaumcraft.port.essentia;

import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;

/** Vanilla furnace acceleration and the original infernal furnace's separate two-block bellows range. */
public final class FurnaceBellows {
    public static int infernal(Level level,BlockPos center){
        int count=0;
        for(Direction direction:Direction.values()){
            if(direction==Direction.UP)continue;
            var pos=center.relative(direction,2);if(!level.hasChunkAt(pos))continue;
            var state=level.getBlockState(pos);
            if(level.getBlockEntity(pos) instanceof BellowsEntity&&state.getValue(BellowsBlock.FACING)==direction.getOpposite()
                    &&!level.hasNeighborSignal(pos))count++;
        }
        return Math.min(4,count);
    }
    public static int infernalCookTime(boolean boosted,int bellows){int b=Math.min(4,Math.max(0,bellows));return Math.max(10,(boosted?80:140)-(20-(b-1))*b);}
    public static boolean advance(FurnaceBlockEntity furnace){
        if(furnace.litTime<=0||furnace.cookingProgress<=0||furnace.cookingProgress>=furnace.cookingTotalTime-1)return false;
        furnace.cookingProgress++;
        furnace.setChanged();
        return true;
    }
    private FurnaceBellows(){}
}
