package thaumcraft.port.world;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

public final class MoundStructure extends Structure {
    public static final MapCodec<MoundStructure> CODEC=simpleCodec(MoundStructure::new);
    public MoundStructure(StructureSettings settings){super(settings);}
    @Override public StructureType<?> type(){return WorldContent.MOUND.get();}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        if(c.chunkGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource||!WorldConfig.STRUCTURES.get())return Optional.empty();
        int x=c.chunkPos().getMinBlockX(),z=c.chunkPos().getMinBlockZ();
        int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
        for(int[] d:new int[][]{{0,0},{18,0},{0,18},{18,18},{9,9}}){
            int y=c.chunkGenerator().getBaseHeight(x+d[0],z+d[1],Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());
            min=Math.min(min,y);max=Math.max(max,y);
            var ground=c.chunkGenerator().getBaseColumn(x+d[0],z+d[1],c.heightAccessor(),c.randomState()).getBlock(y-1);
            if(!(ground.is(Blocks.STONE)||ground.is(Blocks.GRASS_BLOCK)||ground.is(Blocks.DIRT)))return Optional.empty();
        }
        if(max-min>2||min< c.chunkGenerator().getSeaLevel()||min-10<=c.heightAccessor().getMinBuildHeight())return Optional.empty();
        var origin=new BlockPos(x,min-10,z);String template=c.random().nextInt(3)==0?"mound_trapped":"mound";
        return Optional.of(new GenerationStub(origin,b->b.addPiece(new Piece(c.structureTemplateManager(),origin,template))));
    }
    public static final class Piece extends TemplateStructurePiece {
        private static StructurePlaceSettings settings(){return new StructurePlaceSettings().addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK).setIgnoreEntities(true);}
        public Piece(StructureTemplateManager manager,BlockPos pos,String name){super(WorldContent.MOUND_PIECE.get(),0,manager,WorldContent.id(name),"thaumcraft:"+name,settings(),pos);}
        public Piece(StructurePieceSerializationContext c,CompoundTag tag){super(WorldContent.MOUND_PIECE.get(),tag,c.structureTemplateManager(),id->settings());}
        @Override protected void handleDataMarker(String marker,BlockPos pos,ServerLevelAccessor level,RandomSource r,BoundingBox box){
            if(!box.isInside(pos)||!marker.equals("loot"))return;
            float roll=r.nextFloat();String rarity=roll<.1?"rare":roll<.33?"uncommon":"common";
            level.setBlock(pos,WorldContent.block("loot_"+(r.nextFloat()<.3?"crate":"urn")+"_"+rarity).defaultBlockState(),2);
        }
    }
}
