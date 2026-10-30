package thaumcraft.port.client;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NitorVisualRulesTest {
    @Test void flameLoopsEightOriginalFramesWhileCoreKeepsItsOwnCell(){
        int[] expected={264,265,266,267,268,269,270,271,264,265,266,267,268,269,270};
        for(int age=0;age<expected.length;age++){
            assertEquals(expected[age],NitorVisualRules.frame(false,age));
            assertEquals(457,NitorVisualRules.frame(true,age));
        }
    }
    @Test void atlasCoordinatesCoverWholeOriginalCellsWithoutInsetOrVerticalFlip(){
        assertEquals(8/64f,NitorVisualRules.u0(264));assertEquals(9/64f,NitorVisualRules.u1(264));
        assertEquals(4/64f,NitorVisualRules.v0(264));assertEquals(5/64f,NitorVisualRules.v1(264));
        assertEquals(9/64f,NitorVisualRules.u0(457));assertEquals(10/64f,NitorVisualRules.u1(457));
        assertEquals(7/64f,NitorVisualRules.v0(457));assertEquals(8/64f,NitorVisualRules.v1(457));
    }
    @Test void flameShrinksAcrossWholeTicksToTheOriginalTinyFinalSize(){
        var frames=NitorVisualRules.scaleFrames(10,3,.05f);
        assertEquals(.3f,frames[0],.000001);
        assertEquals(0,NitorVisualRules.size(frames,0));
        assertEquals(.2705f,NitorVisualRules.size(frames,1),.000001);
        assertEquals(.1525f,NitorVisualRules.size(frames,5),.000001);
        assertEquals(.005f,NitorVisualRules.size(frames,10),.000001);
        assertEquals(frames[10],NitorVisualRules.size(frames,11));
        for(int age=1;age<frames.length;age++)assertTrue(frames[age]<frames[age-1]);
    }
    @Test void corePulseHasOriginalThreeKeysAndCanGrowOrShrink(){
        var grow=NitorVisualRules.scaleFrames(10,1,1.2f,1);
        var shrink=NitorVisualRules.scaleFrames(10,1,.8f,1);
        assertEquals(.1f,grow[0],.000001);assertEquals(.12f,grow[5],.000001);assertEquals(.1f,grow[10],.000001);
        assertEquals(.1f,shrink[0],.000001);assertEquals(.08f,shrink[5],.000001);assertEquals(.1f,shrink[10],.000001);
    }
    @Test void factoryKeepsDistinctFlameAndCoreLifetimeOpacityAndBlend(){
        var random=RandomSource.create(918);
        for(int i=0;i<100;i++){
            var flame=NitorVisualRules.preset(random,false);
            assertTrue(flame.lifetime()>=10&&flame.lifetime()<=14);
            assertTrue(flame.scaleFrames()[0]>=.3f&&flame.scaleFrames()[0]<.4f);
            assertEquals(.005f,flame.scaleFrames()[flame.lifetime()],.000001);
            assertEquals(.66f,flame.alpha());assertTrue(flame.additive());
            var core=NitorVisualRules.preset(random,true);
            assertEquals(10,core.lifetime());assertEquals(1,core.alpha());assertFalse(core.additive());
            assertEquals(.1f,core.scaleFrames()[0]);assertEquals(.1f,core.scaleFrames()[10],.000001);
        }
    }
    @Test void flamePresetDoesNotConsumeAnUnusedCoreGaussian(){
        var actual=RandomSource.create(26);var reference=RandomSource.create(26);
        var flame=NitorVisualRules.preset(actual,false);
        assertEquals(10+reference.nextInt(5),flame.lifetime());
        assertEquals(.1f*(3+reference.nextFloat()),flame.scaleFrames()[0]);
        assertEquals(reference.nextLong(),actual.nextLong());
    }
}
