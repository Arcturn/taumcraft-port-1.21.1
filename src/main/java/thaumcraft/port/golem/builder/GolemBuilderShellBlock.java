package thaumcraft.port.golem.builder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
public final class GolemBuilderShellBlock extends Block {
 public final Block original;
 public GolemBuilderShellBlock(Block original,Properties props){super(props.strength(2.5f).sound(SoundType.STONE).lightLevel(state->original==Blocks.CAULDRON?13:0));this.original=original;}
 @Override public String getDescriptionId(){return "tile.golem_builder.name";}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
 @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder context){return List.of(new ItemStack(original));}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  if(level.isClientSide)return InteractionResult.SUCCESS;
  for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)if(level.getBlockEntity(pos.offset(x,y,z)) instanceof GolemBuilderEntity builder){if(player instanceof ServerPlayer server)server.openMenu(builder,builder.getBlockPos());return InteractionResult.CONSUME;}
  return InteractionResult.PASS;
 }
 @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock()))GolemBuilderStructure.shellRemoved(level,pos);super.onRemove(state,level,pos,next,moving);}
}
