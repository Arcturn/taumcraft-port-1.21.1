package thaumcraft.port.artifice.furnace;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** ConfigRecipes' 3x3x3 blueprint: twelve bricks, twelve obsidian, lava and the mouth bars. */
public final class InfernalFurnaceStructure {
    public record Part(BlockPos pos,BlockState source){}
    public record Match(BlockPos center,Direction output,List<Part> parts){}
    private static final Set<Level> CHANGING=Collections.newSetFromMap(new IdentityHashMap<>());
    public static Match find(Level level,BlockPos clicked){
        for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            BlockPos center=clicked.offset(dx,dy,dz);
            for(Direction output:Direction.Plane.HORIZONTAL){var match=match(level,center,output);if(match!=null)return match;}
        }
        return null;
    }
    public static Match match(Level level,BlockPos center,Direction output){
        if(!output.getAxis().isHorizontal()||!level.hasChunkAt(center)||!level.getFluidState(center).is(FluidTags.LAVA))return null;
        var parts=new ArrayList<Part>();parts.add(new Part(center.immutable(),level.getBlockState(center)));
        for(int y=1;y>=-1;y--)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            if(x==0&&z==0&&y>=0)continue; // The top opening is explicitly null in the original blueprint.
            BlockPos pos=center.offset(x,y,z);if(!level.hasChunkAt(pos))return null;
            Block expected=y==0&&x==output.getStepX()&&z==output.getStepZ()?Blocks.IRON_BARS
                    :x!=0&&z!=0?Blocks.NETHER_BRICKS:Blocks.OBSIDIAN;
            var state=level.getBlockState(pos);if(!state.is(expected))return null;
            parts.add(new Part(pos.immutable(),state));
        }
        return new Match(center.immutable(),output,List.copyOf(parts));
    }
    /** All writes are made before neighbor notifications; a failed conversion restores its paid inputs. */
    public static boolean form(Level level,BlockPos center,Direction output){
        var match=match(level,center,output);if(match==null||!CHANGING.add(level))return false;
        boolean success=true;
        try {
            for(var part:match.parts()){
                BlockState replacement=part.pos().equals(center)?InfernalFurnaceContent.FURNACE.get().defaultBlockState().setValue(InfernalFurnaceBlock.FACING,output.getOpposite())
                        :part.source().is(Blocks.IRON_BARS)?Blocks.AIR.defaultBlockState()
                        :part.source().is(Blocks.NETHER_BRICKS)?InfernalFurnaceContent.BRICK.get().defaultBlockState():InfernalFurnaceContent.OBSIDIAN.get().defaultBlockState();
                if(!level.setBlock(part.pos(),replacement,Block.UPDATE_CLIENTS)){success=false;break;}
            }
            if(!success)for(var part:match.parts())level.setBlock(part.pos(),part.source(),Block.UPDATE_CLIENTS);
        }finally{CHANGING.remove(level);}
        for(var part:match.parts())level.updateNeighborsAt(part.pos(),level.getBlockState(part.pos()).getBlock());
        return success;
    }
    public static void shellRemoved(Level level,BlockPos broken){
        if(level.isClientSide||CHANGING.contains(level))return;
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
            // Reversion is a bounded 3x3x3 transaction, including parts across a chunk boundary.
            var center=broken.offset(x,y,z);var state=level.getBlockState(center);
            if(state.is(InfernalFurnaceContent.FURNACE.get())){dismantle(level,center,state,broken);return;}
        }
    }
    public static void dismantle(Level level,BlockPos center,BlockState furnace,BlockPos broken){
        if(level.isClientSide||!CHANGING.add(level))return;
        try {
            // Drop the queue once before replacing its block entity. Never turn the broken shell
            // block back into a second harvestable copy (the old reference-equality check did).
            if(level.getBlockEntity(center) instanceof InfernalFurnaceEntity entity){Containers.dropContents(level,center,entity);entity.clearContent();}
            for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
                var pos=center.offset(x,y,z);if(pos.equals(broken))continue;
                var state=level.getBlockState(pos);
                if(state.getBlock() instanceof FurnaceShellBlock shell)level.setBlock(pos,shell.original.defaultBlockState(),Block.UPDATE_ALL);
            }
            var mouth=center.relative(furnace.getValue(InfernalFurnaceBlock.FACING).getOpposite());
            if(level.isEmptyBlock(mouth))level.setBlock(mouth,Blocks.IRON_BARS.defaultBlockState(),Block.UPDATE_ALL);
            level.setBlock(center,Blocks.LAVA.defaultBlockState(),Block.UPDATE_ALL);
        }finally{CHANGING.remove(level);}
    }
    public static boolean changing(Level level){return CHANGING.contains(level);}
    private InfernalFurnaceStructure(){}
}
