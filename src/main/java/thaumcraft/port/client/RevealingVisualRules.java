package thaumcraft.port.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** BETA26 container labels grow before drawing and decay in the world-last pass. */
final class RevealingVisualRules {
    static float renderScale(float previous,boolean visible) {
        return visible&&previous<.3f?previous+.031f-previous/10f:previous;
    }
    static float afterFrame(float scale) {return Math.max(0,scale-.005f);}
    static Vec3 anchor(BlockPos pos,Direction struckFace,boolean airAbove,float scale) {
        Direction face=airAbove?Direction.UP:struckFace;
        return Vec3.atCenterOf(pos).add(face.getStepX()*scale*2,
                (airAbove?.4:0)+face.getStepY()*scale*2,face.getStepZ()*scale*2);
    }
    private RevealingVisualRules() {}
}
