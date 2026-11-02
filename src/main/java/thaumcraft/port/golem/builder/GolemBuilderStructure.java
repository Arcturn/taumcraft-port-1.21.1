package thaumcraft.port.golem.builder;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.golem.GolemContent;
/** Exact five-part 2x2x2 ConfigRecipes blueprint, rotated with the original matrix convention. */
public final class GolemBuilderStructure {
 public record Part(BlockPos pos,BlockState source){}public record Match(BlockPos center,Direction facing,List<Part> parts){}
 private static final Set<Level> CHANGING=Collections.newSetFromMap(new IdentityHashMap<>());
 private static Block table(){return BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:table_stone"));}
 public static BlockPos relative(BlockPos center,Direction facing,int x,int y,int z){int rotations=(3-facing.get2DDataValue())&3;for(int i=0;i<rotations;i++){int previous=x;x=z;z=-previous;}return center.offset(x,y,z);}
 public static Match find(Level level,BlockPos clicked){for(int y=-1;y<=0;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var center=clicked.offset(x,y,z);for(Direction facing:Direction.Plane.HORIZONTAL){var match=match(level,center,facing);if(match!=null)return match;}}return null;}
 public static Match match(Level level,BlockPos center,Direction facing){if(!facing.getAxis().isHorizontal()||!level.hasChunkAt(center))return null;var core=level.getBlockState(center);if(!core.is(Blocks.PISTON)||core.getValue(PistonBaseBlock.FACING)!=Direction.UP||core.getValue(PistonBaseBlock.EXTENDED))return null;
  var specs=new int[][]{{0,0,0},{0,1,0},{-1,0,0},{-1,0,1},{0,0,1}};var expected=new Block[]{Blocks.PISTON,Blocks.IRON_BARS,Blocks.CAULDRON,Blocks.ANVIL,table()};var parts=new ArrayList<Part>();
  for(int i=0;i<specs.length;i++){var s=specs[i];var p=relative(center,facing,s[0],s[1],s[2]);if(!level.hasChunkAt(p))return null;var state=level.getBlockState(p);if(!(state.is(expected[i])||i==2&&state.is(Blocks.WATER_CAULDRON)||i==3&&(state.is(Blocks.CHIPPED_ANVIL)||state.is(Blocks.DAMAGED_ANVIL))))return null;parts.add(new Part(p.immutable(),state));}return new Match(center.immutable(),facing,List.copyOf(parts));
 }
 public static boolean form(Level level,BlockPos center,Direction facing){var match=match(level,center,facing);if(match==null||!CHANGING.add(level))return false;var targets=new Block[]{GolemContent.BUILDER.get(),GolemContent.BUILDER_BARS.get(),GolemContent.BUILDER_CAULDRON.get(),GolemContent.BUILDER_ANVIL.get(),GolemContent.BUILDER_TABLE.get()};boolean success=true;
  try{for(int i=0;i<targets.length;i++){var target=targets[i].defaultBlockState();if(i==0)target=target.setValue(GolemBuilderBlock.FACING,facing);if(!level.setBlock(match.parts.get(i).pos,target,Block.UPDATE_CLIENTS)){success=false;break;}}if(!success)for(var part:match.parts)level.setBlock(part.pos,part.source,Block.UPDATE_CLIENTS);}finally{CHANGING.remove(level);}for(var part:match.parts)level.updateNeighborsAt(part.pos,level.getBlockState(part.pos).getBlock());return success;
 }
 public static void shellRemoved(Level level,BlockPos broken){if(level.isClientSide||CHANGING.contains(level))return;for(int y=-1;y<=0;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var center=broken.offset(x,y,z);var state=level.getBlockState(center);if(state.is(GolemContent.BUILDER.get())){dismantle(level,center,state,broken);return;}}}
 public static void dismantle(Level level,BlockPos center,BlockState state,BlockPos broken){if(level.isClientSide||!CHANGING.add(level))return;try{if(level.getBlockEntity(center) instanceof GolemBuilderEntity entity){Containers.dropContents(level,center,entity);entity.clearContent();}
  var facing=state.getValue(GolemBuilderBlock.FACING);for(var spec:new int[][]{{0,1,0},{-1,0,0},{-1,0,1},{0,0,1}}){var pos=relative(center,facing,spec[0],spec[1],spec[2]);if(pos.equals(broken))continue;var part=level.getBlockState(pos);if(part.getBlock() instanceof GolemBuilderShellBlock shell)level.setBlock(pos,shell.original.defaultBlockState(),Block.UPDATE_ALL);}
  if(!center.equals(broken))level.setBlock(center,Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.UP),Block.UPDATE_ALL);
 }finally{CHANGING.remove(level);}}
 private GolemBuilderStructure(){}
}
