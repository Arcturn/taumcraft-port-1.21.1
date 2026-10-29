package thaumcraft.port.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.port.research.ResearchCatalog;

import static org.junit.jupiter.api.Assertions.*;

class BookBlocksContextTest {
    @BeforeAll static void bootstrapVanillaStates() {
        // NeoForge's vanilla FeatureFlags initialization enumerates the loader
        // mod list even when this unit-test JVM has no mod discovery phase.
        if(LoadingModList.get()==null)LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void variantsMatchActualMinecraft112BytecodeAtBlueprintAndOverflowCoordinates() {
        // Captured by reflection from the unmodified 1.12.2 client rk.c and
        // cgg.getRandomModel, with sixteen equally weighted marker models.
        int[][] positions = {{0,0,0},{0,0,2},{0,1,0},{0,1,2},{2,0,0},{2,0,2},{2,1,0},{2,1,2},
                {1,2,1},{-1,0,-1},{Integer.MIN_VALUE,7,Integer.MAX_VALUE},{1000000,-64,-1000000}};
        long[] seeds = {0L,-5444680982622362366L,42317872L,-5425023526858770730L,
                -2008247555232448674L,-2674758720412006324L,-2007717757406427078L,
                -2655542312059857660L,3923779930808210152L,3942996338948769522L,
                3422402307557659988L,-4107537825190913472L};
        int[] selected = {0,8,5,2,2,10,11,7,15,11,3,3};
        var definitions = ResearchCatalog.read("/thaumcraft-port/book-block-model-index.json");
        for (int i=0;i<positions.length;i++) {
            var p = positions[i]; var pos = new BlockPos(p[0],p[1],p[2]);
            assertEquals(seeds[i],BookBlocks.positionSeed(pos),pos.toString());
            for (String name : new String[]{"stone_arcane_0","stone_ancient_0","stone_eldritch_tile_0"}) {
                var definition=definitions.getAsJsonObject(name);
                var variants=definition.getAsJsonArray("variants");
                assertSame(variants.get(selected[i]%variants.size()).getAsJsonObject(),
                        BookBlocks.variant(definition,pos),name+" "+pos);
            }
        }
    }

    @Test void selectionUsesWeightsRatherThanUniformEntryIndexes() {
        var definition=new JsonObject();var variants=new JsonArray();
        for (int weight:new int[]{3,1}) {var v=new JsonObject();v.addProperty("weight",weight);variants.add(v);}
        definition.add("variants",variants);
        assertSame(variants.get(0),BookBlocks.variant(definition,new BlockPos(0,1,0)));
        assertSame(variants.get(1),BookBlocks.variant(definition,new BlockPos(2,1,0)));
    }

    @Test void originalNonFullStatesKeepCollisionMaterialAndAmbientLightSemantics() {
        var pos=BlockPos.ZERO;
        var view=BookBlocks.view(Map.of(pos,"thaumcraft:pillar_arcane;1;3"));
        var pillar=view.getBlockState(pos);
        assertFalse(pillar.isAir());assertTrue(pillar.isSolid());
        assertFalse(pillar.canOcclude());assertFalse(pillar.isSolidRender(view,pos));
        assertEquals(1,pillar.getShadeBrightness(view,pos));assertEquals(0,pillar.getLightBlock(view,pos));
        assertEquals(.5,pillar.getShape(view,pos).bounds().maxY);
        assertEquals(2,pillar.getCollisionShape(view,pos).bounds().maxY);
        assertFalse(pillar.isFaceSturdy(view,pos,Direction.UP));
        assertTrue(pillar.getFaceOcclusionShape(view,pos,Direction.UP).isEmpty());
        assertNotNull(pillar.getFluidState());assertTrue(pillar.getFluidState().isEmpty());
        for(String name:new String[]{"pedestal_arcane","pedestal_ancient","pedestal_eldritch","table_stone",
                "infusion_matrix","thaumatorium","thaumatorium_top","golem_builder","placeholder_brick","placeholder_obsidian"}) {
            var state=BookBlocks.state("thaumcraft:"+name);
            assertFalse(state.isAir(),name);assertTrue(state.isSolid(),name);
            assertFalse(state.canOcclude(),name);assertEquals(1,state.getShadeBrightness(view,pos),name);
        }
        var stone=BookBlocks.state("thaumcraft:stone_ancient");
        assertTrue(stone.canOcclude());assertEquals(.2f,stone.getShadeBrightness(view,pos));
        var furnace=BookBlocks.state("thaumcraft:infernal_furnace");
        assertTrue(furnace.canOcclude());assertEquals(.5,furnace.getShape(view,pos).bounds().maxY);
        assertEquals(13,furnace.getLightEmission(view,pos));
        assertEquals(13,BookBlocks.state("thaumcraft:placeholder_cauldron").getLightEmission(view,pos));
    }

    @Test void everySourceAndTargetThaumcraftCellHasAnExplicitLegacyContext() {
        var definitions=ResearchCatalog.read("/thaumcraft-port/book-block-states.json");
        var recipes=ResearchCatalog.read("/thaumcraft-port/book-recipes.json").getAsJsonObject("recipes");
        for(var entry:recipes.entrySet()) {
            var recipe=entry.getValue().getAsJsonObject();
            for(String field:new String[]{"layers","target_layers"}) {
                if(!recipe.has(field)) continue;
                for(var layer:recipe.getAsJsonArray(field)) for(var row:layer.getAsJsonArray()) for(var cell:row.getAsJsonArray()) {
                    if(cell.isJsonNull()) continue;
                    String source=cell.getAsString();
                    if(!source.startsWith("thaumcraft:")) continue;
                    assertTrue(definitions.has(source.split(";",2)[0].substring(11)),entry.getKey()+" "+source);
                    assertFalse(BookBlocks.state(source).isAir(),entry.getKey()+" "+source);
                }
            }
        }
    }

    @Test void crucibleRetainsHollowCollisionAndOriginalSolidFaceClassification() {
        var pos=BlockPos.ZERO;var view=BookBlocks.view(Map.of(pos,"thaumcraft:crucible"));
        var crucible=view.getBlockState(pos);var shape=crucible.getCollisionShape(view,pos);
        assertFalse(Shapes.joinIsNotEmpty(shape,Shapes.box(.2,.4,.2,.8,.9,.8),BooleanOp.AND));
        assertTrue(Shapes.joinIsNotEmpty(shape,Shapes.box(0,.5,.3,.1,.6,.7),BooleanOp.AND));
        assertTrue(crucible.isFaceSturdy(view,pos,Direction.EAST));
        assertFalse(crucible.canOcclude());
    }

    @Test void barsUseCompleteVisibleNeighborsAndOriginalBlockFaceShape() {
        var pos=BlockPos.ZERO;
        var view=BookBlocks.view(Map.of(pos,"minecraft:iron_bars",pos.east(),"thaumcraft:crucible",
                pos.west(),"thaumcraft:table_stone",pos.north(),"minecraft:nether_brick"));
        var bars=view.getBlockState(pos);assertTrue(bars.is(Blocks.IRON_BARS));
        assertTrue(bars.getValue(CrossCollisionBlock.EAST));
        assertTrue(bars.getValue(CrossCollisionBlock.NORTH));
        assertFalse(bars.getValue(CrossCollisionBlock.WEST));
        assertFalse(bars.getValue(CrossCollisionBlock.SOUTH));
        var sliced=BookBlocks.view(Map.of(pos,"minecraft:iron_bars"));
        assertFalse(sliced.getBlockState(pos).getValue(CrossCollisionBlock.EAST));
    }
}
