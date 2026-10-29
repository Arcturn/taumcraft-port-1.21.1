package thaumcraft.port;

import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.port.research.BookIngredients;
import static org.junit.jupiter.api.Assertions.*;

class BookIngredientsTest {
    @Test void woodAndGlassUseTheOriginalExpandedSetWithoutNewMinecraftSpecies(){
        var wood=BookIngredients.variants("plankWood",true);
        assertEquals(List.of("minecraft:oak_planks;1;0","minecraft:spruce_planks;1;0","minecraft:birch_planks;1;0","minecraft:jungle_planks;1;0","minecraft:acacia_planks;1;0","minecraft:dark_oak_planks;1;0","thaumcraft:plank_greatwood;1;0","thaumcraft:plank_silverwood;1;0"),wood);
        assertEquals(8,BookIngredients.variants("slabWood",true).size());
        assertEquals(17,BookIngredients.variants("blockGlass",true).size());
        assertEquals(17,BookIngredients.variants("paneGlass",true).size());
    }
    @Test void slotOffsetAndOneSecondClockBothSelectAlternatives(){
        assertEquals("minecraft:spruce_planks;1;0",BookIngredients.resolve("oredict:plankWood",1,0,true));
        assertEquals("minecraft:birch_planks;1;0",BookIngredients.resolve("oredict:plankWood",1,1000,true));
        assertEquals("minecraft:oak_planks;1;0",BookIngredients.resolve("oredict:plankWood",1,7000,true));
        assertEquals(0,BookIngredients.index(8,0,999));assertEquals(1,BookIngredients.index(8,0,1000));
    }
    @Test void ColorAndMeatAlternativesStayDistinctAndMissingIntegrationsStayEmpty(){
        assertEquals(16,BookIngredients.variants("nitor",true).stream().distinct().count());
        assertEquals(16,BookIngredients.variants("tallowCandles",true).stream().distinct().count());
        assertEquals(6,BookIngredients.variants("nuggetMeat",true).stream().distinct().count());
        assertEquals("minecraft:air",BookIngredients.resolve("oredict:oreCopper",0,0,true));
        assertEquals("minecraft:diamond;2;0",BookIngredients.resolve("minecraft:diamond;2;0",3,9999,true));
    }
}
