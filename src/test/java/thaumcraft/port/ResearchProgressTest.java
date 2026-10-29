package thaumcraft.port;
import java.util.Map;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import thaumcraft.port.aspect.*;
import thaumcraft.port.research.*;
import static org.junit.jupiter.api.Assertions.*;

class ResearchProgressTest {
    @Test void allOriginalEntriesAndFormulaArePresent(){
        assertEquals(148,ResearchCatalog.all().size());assertEquals(3,ResearchCatalog.get("FIRSTSTEPS").stages().size());
        assertEquals(2,ResearchCatalog.observation("BASICS",Map.of(Aspect.AIR,5)));
        assertEquals(6,ResearchCatalog.observation("ALCHEMY",Map.of(Aspect.ALCHEMY,10)));
    }
    @Test void hiddenPrerequisitesAndStageReferencesRemainDistinct(){
        var s=new ResearchState();var first=ResearchCatalog.get("FIRSTSTEPS");assertFalse(s.available(first));
        s.flag("!gotthaumonomicon");assertTrue(s.available(first));s.stage("FIRSTSTEPS",2);
        assertTrue(s.knows("FIRSTSTEPS@2"));assertFalse(s.knows("FIRSTSTEPS"));assertFalse(s.knows("FIRSTSTEPS@3"));
        s.stage("FIRSTSTEPS",4);assertTrue(s.knows("FIRSTSTEPS"));
        assertFalse(s.available(ResearchCatalog.get("WARP")));s.flag("WARP");assertTrue(s.available(ResearchCatalog.get("WARP")));
    }
    @Test void andOrExpressionsUseRealBooleanSemantics(){
        var s=new ResearchState();s.flag("a");assertTrue(s.knows("a||b"));assertFalse(s.knows("b||c"));
        assertFalse(s.knows("a&&b"));s.flag("b");assertTrue(s.knows("a&&b"));assertFalse(s.knows("FIRSTSTEPS@oops"));
    }
    @Test void knowledgeSpendingRejectsUnderflowAndUnknownCategory(){
        var s=new ResearchState();assertTrue(s.addKnowledge("THEORY","ALCHEMY",32));
        assertFalse(s.addKnowledge("THEORY","ALCHEMY",-33));assertEquals(32,s.knowledge("THEORY","ALCHEMY"));
        assertTrue(s.addKnowledge("THEORY","ALCHEMY",-32));assertFalse(s.addKnowledge("THEORY","UNKNOWN",5));
        assertFalse(s.addKnowledge("THEORY","ALCHEMY",Integer.MAX_VALUE));
    }
    @Test void schemaOneMigrationPreservesScansAndGrantsAspectDiscoveries(){
        var tag=new CompoundTag();var scanned=new ListTag();scanned.add(StringTag.valueOf("item:minecraft:stone"));tag.put("scanned",scanned);
        var discovered=new ListTag();discovered.add(StringTag.valueOf("terra"));tag.put("discovered",discovered);
        var s=ResearchCodec.read(tag);assertTrue(s.knows("!terra"));assertEquals(1,s.scanned().size());assertEquals(0,s.stage("FIRSTSTEPS"));
    }
    @Test void completeStateRoundTripsIncludingWarpAndCrafts(){
        var s=new ResearchState();s.stage("FIRSTSTEPS",4);s.addKnowledge("OBSERVATION","BASICS",31);s.craft("thaumcraft:arcane_workbench");s.warp(4,8,12);
        var copy=ResearchCodec.read(ResearchCodec.write(s));assertTrue(copy.knows("FIRSTSTEPS"));assertEquals(31,copy.knowledge("OBSERVATION","BASICS"));
        assertTrue(copy.crafted().contains("thaumcraft:arcane_workbench"));assertEquals(4,copy.permanentWarp());assertEquals(8,copy.normalWarp());assertEquals(12,copy.temporaryWarp());
    }
    @Test void completedLegacyScanFlagsAlsoSatisfyStagedRequirements(){
        var s=new ResearchState();s.flag("!CrimsonCultist");assertTrue(s.knows("!CrimsonCultist@2"));assertFalse(s.knows("!CrimsonCultist@3"));
        var copy=ResearchCodec.read(ResearchCodec.write(s));assertTrue(copy.knows("!CrimsonCultist@2"));assertEquals(0,copy.stage("!CrimsonCultist"));
    }
    @Test void originalRecipeAspectRoundingIsRetained(){
        assertEquals(0,RecipeAspects.scaled(1,1));assertEquals(1,RecipeAspects.scaled(2,1));assertEquals(1,RecipeAspects.scaled(5,4));
        assertEquals(3,RecipeAspects.scaled(4,1));assertEquals(0,RecipeAspects.scaled(-1,1));
    }
}
