package thaumcraft.port.client;

import java.util.*;
import org.junit.jupiter.api.Test;
import thaumcraft.port.theory.TheoryCards;
import thaumcraft.port.theory.TheorySession;
import static org.junit.jupiter.api.Assertions.*;

class TheoryPresentationTest {
    private static TheorySession session(){return new TheorySession(UUID.randomUUID(),5,List.of());}
    private static TheoryCards.Card card(long seed){return new TheoryCards.Card("Calibrate",seed,false,"ARTIFICE","",0,"",List.of());}

    @Test void reopeningTheoryStartsAtSavedTotalsAndOnlyNewProgressAnimatesEveryTwoTicks(){
        var session=session();session.totals.put("ARTIFICE",50);
        var ui=new TheoryPresentation();ui.sync(session,100);
        assertEquals(Map.of("ARTIFICE",50),ui.totals());
        session.totals.put("ARTIFICE",53);
        assertTrue(ui.stepTotals(session,101).isEmpty());
        assertEquals(Set.of("ARTIFICE"),ui.stepTotals(session,102));
        assertEquals(51,ui.totals().get("ARTIFICE"));
        assertTrue(ui.stepTotals(session,102).isEmpty());
        assertTrue(ui.stepTotals(session,103).isEmpty());
        ui.stepTotals(session,104);
        assertEquals(52,ui.totals().get("ARTIFICE"));
        session.totals.clear();ui.stepTotals(session,106);
        assertEquals(51,ui.totals().get("ARTIFICE"));
    }

    @Test void serverAcknowledgmentDoesNotRemoveTheLocallyAnimatingCardsOrPrematurelyReplaceTheStack(){
        var session=session();var old=card(1);session.lastDraw=old;session.choices.addAll(List.of(card(2),card(3)));
        var ui=new TheoryPresentation();assertTrue(ui.sync(session,20).drawn());
        session.selected=1;assertTrue(ui.sync(session,21).selected());
        session.acknowledge();ui.sync(session,22);
        assertEquals(2,ui.cards().size());assertEquals(1,ui.selected());assertEquals(old,ui.lastDraw());
        ui.finishSelection();
        assertEquals(card(3),ui.lastDraw());assertTrue(ui.cards().isEmpty());
        ui.sync(session,23);assertEquals(card(3),ui.lastDraw());
    }

    @Test void delayedServerAcknowledgmentCannotReplayAnAlreadyFinishedSelection(){
        var session=session();session.choices.addAll(List.of(card(2),card(3)));
        var ui=new TheoryPresentation();ui.sync(session,20);session.selected=0;ui.sync(session,21);
        ui.finishSelection();
        var delayed=ui.sync(session,80);assertFalse(delayed.drawn());assertFalse(delayed.selected());
        assertTrue(ui.cards().isEmpty());assertEquals(card(2),ui.lastDraw());
        session.acknowledge();ui.sync(session,81);session.choices.addAll(List.of(card(4),card(5)));
        assertTrue(ui.sync(session,82).drawn());assertEquals(2,ui.cards().size());assertEquals(-1,ui.selected());
    }

    @Test void scrappingOrFinishingClearsTheOldTheoryPresentation(){
        var session=session();session.totals.put("ALCHEMY",100);session.lastDraw=card(1);session.choices.add(card(2));
        var ui=new TheoryPresentation();ui.sync(session,20);ui.sync(null,21);
        assertTrue(ui.totals().isEmpty());assertTrue(ui.cards().isEmpty());assertNull(ui.lastDraw());
        var next=session();next.totals.put("BASICS",15);ui.sync(next,30);
        assertEquals(Map.of("BASICS",15),ui.totals());
    }

    @Test void anotherViewersAcknowledgmentClearsUnselectedCardsEvenIfTheSelectionSnapshotWasMissed(){
        var session=session();session.choices.addAll(List.of(card(2),card(3)));
        var ui=new TheoryPresentation();ui.sync(session,20);
        session.selected=1;session.acknowledge();ui.sync(session,21);
        assertTrue(ui.cards().isEmpty());assertEquals(card(3),ui.lastDraw());
        session.choices.addAll(List.of(card(4),card(5)));
        assertTrue(ui.sync(session,22).drawn());assertEquals(card(4),ui.cards().getFirst());
    }

    @Test void differentDrawReplacesStaleUnselectedCardsAndRejectsClicksBeforeTheNextRenderedFrame(){
        var session=session();session.choices.addAll(List.of(card(2),card(3)));
        var ui=new TheoryPresentation();ui.sync(session,20);assertTrue(ui.canSelect(session,0));
        session.selected=1;session.acknowledge();session.choices.addAll(List.of(card(4),card(5)));
        assertFalse(ui.canSelect(session,0),"Never select a different server card under the previous visible card");
        assertTrue(ui.sync(session,21).drawn());assertEquals(card(4),ui.cards().getFirst());
        assertTrue(ui.canSelect(session,0));
    }
}
