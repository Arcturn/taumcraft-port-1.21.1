package thaumcraft.port.infusion.bauble;

import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.shapes.*;

/** The original invisible field blocks living mobs, but not players, items or ridden mounts. */
public final class BarrierBlock extends Block {
    public BarrierBlock(Properties properties){super(properties);}
    public static boolean active(BlockGetter level,BlockPos pos){
        BlockPos base=pos.below();if(!level.getBlockState(base).is(InfusionBaubles.PAVING_STONE_BARRIER.get()))base=base.below();
        return level.getBlockState(base).is(InfusionBaubles.PAVING_STONE_BARRIER.get())&&(!(level instanceof Level world)||!world.hasNeighborSignal(base));
    }
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        Entity entity=context instanceof EntityCollisionContext entities?entities.getEntity():null;
        return entity instanceof LivingEntity&&!(entity instanceof Player)&&entity.getPassengersAndSelf().noneMatch(Player.class::isInstance)&&active(level,pos)?Shapes.block():Shapes.empty();
    }
    @Override protected VoxelShape getVisualShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override protected float getShadeBrightness(BlockState state,BlockGetter level,BlockPos pos){return 1;}
    @Override protected boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos){return true;}
    @Override protected BlockState updateShape(BlockState state,Direction side,BlockState neighbour,LevelAccessor level,BlockPos pos,BlockPos neighbourPos){
        if(side==Direction.DOWN&&!neighbour.is(InfusionBaubles.PAVING_STONE_BARRIER.get())&&!neighbour.is(this))return Blocks.AIR.defaultBlockState();return state;
    }
    @Override public PathType getBlockPathType(BlockState state,BlockGetter level,BlockPos pos,Mob mob){return active(level,pos)?PathType.BLOCKED:PathType.OPEN;}
}
