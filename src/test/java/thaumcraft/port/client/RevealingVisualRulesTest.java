package thaumcraft.port.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RevealingVisualRulesTest {
    @Test void airAboveOverridesStruckFaceAndRaisesLabelsByPointFour() {
        var pos=new BlockPos(2,3,4);
        for(var face:Direction.values()) {
            var actual=RevealingVisualRules.anchor(pos,face,true,.25f);
            assertEquals(0,actual.distanceTo(new Vec3(2.5,4.4,4.5)),1e-6);
        }
    }
    @Test void obstructedContainersPlaceTagsOutsideTheTargetedFace() {
        var pos=new BlockPos(2,3,4);var center=Vec3.atCenterOf(pos);
        for(var face:Direction.values()) {
            var offset=RevealingVisualRules.anchor(pos,face,false,.25f).subtract(center);
            assertEquals(new Vec3(face.getStepX()*.5,face.getStepY()*.5,face.getStepZ()*.5),offset);
        }
    }
    @Test void originalGrowthOccursBeforeDrawingAndStopsAfterLosingTarget() {
        assertEquals(.031f,RevealingVisualRules.renderScale(0,true),1e-6);
        assertEquals(.026f,RevealingVisualRules.afterFrame(.031f),1e-6);
        assertEquals(.0544f,RevealingVisualRules.renderScale(.026f,true),1e-6);
        float scale=0;
        for(int i=0;i<200;i++)scale=RevealingVisualRules.afterFrame(RevealingVisualRules.renderScale(scale,true));
        assertEquals(.26f,scale,1e-6);
        for(int i=0;i<100;i++)scale=RevealingVisualRules.afterFrame(RevealingVisualRules.renderScale(scale,false));
        assertEquals(0,scale);
    }
}
