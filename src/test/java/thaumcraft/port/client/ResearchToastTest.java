package thaumcraft.port.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchToastTest {
    @Test void shortNativeToastKeepsOriginalPositionsAndOneSlot(){
        var layout=new ResearchToast.Layout(1,1,9);
        assertEquals(7,layout.headingY(0));assertEquals(18,layout.titleY(0));
        assertEquals(32,layout.height());assertEquals(1,layout.slots());
        assertEquals(java.util.List.of(new ResearchToast.FrameStrip(0,224,16),new ResearchToast.FrameStrip(16,240,16)),ResearchToast.frame(32));
    }

    @Test void longRussianTitleStaysFullSizeAndReservesTwoSlotsBeforeRendering(){
        // Actual native 1.21.1 at 124px: "Улучшение процесса" / "очистки эссенций" / "(Часть 1)".
        var layout=new ResearchToast.Layout(1,3,9);
        assertEquals(18,layout.titleY(0));assertEquals(27,layout.titleY(1));assertEquals(36,layout.titleY(2));
        assertEquals(50,layout.height());assertEquals(2,layout.slots());
        assertEquals(5,layout.height()-layout.titleY(3),"All native-size title lines need the original bottom margin");
        assertTrue(layout.height()<=layout.slots()*32);
    }

    @Test void wrappedHeadingMovesTheFullTitleWithoutOverlap(){
        var layout=new ResearchToast.Layout(2,3,9);
        assertEquals(16,layout.headingY(1));assertEquals(27,layout.titleY(0));
        assertEquals(59,layout.height());assertEquals(2,layout.slots());
        assertEquals(2,layout.titleY(0)-(layout.headingY(1)+9));
    }

    @Test void expandedFramesCoverEveryPixelWithoutStretchingTheOriginalCorners(){
        for(int height=32;height<=160;height++){
            var strips=ResearchToast.frame(height);int y=0;
            for(var strip:strips){assertEquals(y,strip.y(),"Frame gap or overlap");assertTrue(strip.sourceY()>=224&&strip.sourceY()+strip.height()<=256);y+=strip.height();}
            assertEquals(height,y);
            assertEquals(new ResearchToast.FrameStrip(0,224,16),strips.getFirst());
            assertEquals(new ResearchToast.FrameStrip(height-16,240,16),strips.getLast());
            for(var strip:strips.subList(1,strips.size()-1)){assertEquals(240,strip.sourceY());assertEquals(1,strip.height());}
        }
    }
}
