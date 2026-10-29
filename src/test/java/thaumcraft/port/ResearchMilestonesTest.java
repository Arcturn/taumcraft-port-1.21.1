package thaumcraft.port;

import org.junit.jupiter.api.Test;
import thaumcraft.port.research.*;
import static org.junit.jupiter.api.Assertions.*;

class ResearchMilestonesTest {
    @Test void partialResearchDoesNotEarnCompletion(){
        var state=new ResearchState();var entry=ResearchCatalog.get("FIRSTSTEPS");
        for(int stage=0;stage<=entry.stages().size();stage++){
            if(stage>0)state.stage(entry.key(),stage);
            assertFalse(ResearchMilestones.matches(state,entry.key(),""));
            assertFalse(ResearchMilestones.matches(state,"","BASICS"));
        }
        state.stage(entry.key(),entry.stages().size()+1);
        assertTrue(ResearchMilestones.matches(state,entry.key(),""));
        assertTrue(ResearchMilestones.matches(state,"","BASICS"));
        assertFalse(ResearchMilestones.matches(state,"","ALCHEMY"));
    }
    @Test void hiddenScanUsesItsActualDiscoveryFlag(){
        var state=new ResearchState();assertFalse(ResearchMilestones.matches(state,"!Wisp",""));
        state.flag("!Wisp");assertTrue(ResearchMilestones.matches(state,"!Wisp",""));
        assertFalse(ResearchMilestones.matches(state,"!Pech",""));
    }
    @Test void emptyUnknownOrAmbiguousConditionsFailClosed(){
        var state=new ResearchState();state.completeCatalog();state.flag("invented");
        assertFalse(ResearchMilestones.matches(state,"",""));
        assertFalse(ResearchMilestones.matches(state,"invented",""));
        assertFalse(ResearchMilestones.matches(state,"FIRSTSTEPS@1",""));
        assertFalse(ResearchMilestones.matches(state,"","invented"));
        assertFalse(ResearchMilestones.matches(state,"FIRSTSTEPS","BASICS"));
    }
    @Test void backfillReadsSavedProgressWithoutChangingIt(){
        var state=new ResearchState();state.completeCatalog();state.flag("!gotthaumonomicon");state.addKnowledge("THEORY","ALCHEMY",91);state.warp(3,4,5);
        var saved=ResearchCodec.write(state);var restored=ResearchCodec.read(saved);
        for(var entry:ResearchCatalog.all())assertTrue(ResearchMilestones.matches(restored,entry.key(),""));
        for(String category:ResearchCatalog.CATEGORIES)assertTrue(ResearchMilestones.matches(restored,"",category));
        assertTrue(ResearchMilestones.matches(restored,"!gotthaumonomicon",""));
        assertEquals(saved,ResearchCodec.write(restored));
    }
    @Test void openingBookDoesNotCompleteItsResearch(){
        var state=new ResearchState();state.flag("!gotthaumonomicon");
        assertTrue(ResearchMilestones.matches(state,"!gotthaumonomicon",""));
        assertFalse(ResearchMilestones.matches(state,"FIRSTSTEPS",""));
        assertFalse(ResearchMilestones.matches(state,"","BASICS"));
    }
}
