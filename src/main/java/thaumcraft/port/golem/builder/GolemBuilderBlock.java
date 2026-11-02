package thaumcraft.port.golem.builder;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.port.golem.GolemContent;
public final class GolemBuilderBlock extends Block implements EntityBlock {
 public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
 public GolemBuilderBlock(Properties props){super(props);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
 @Override public String getDescriptionId(){return "tile.golem_builder.name";}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new GolemBuilderEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return type==GolemContent.BUILDER_ENTITY.get()?(l,p,s,e)->((GolemBuilderEntity)e).tick():null;}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){if(level.getBlockEntity(pos) instanceof GolemBuilderEntity entity){if(player instanceof ServerPlayer server)server.openMenu(entity,pos);return InteractionResult.sidedSuccess(level.isClientSide);}return InteractionResult.PASS;}
 @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder context){return List.of();}
 @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock()))GolemBuilderStructure.dismantle(level,pos,state,pos);super.onRemove(state,level,pos,next,moving);}
}
