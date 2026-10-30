package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;

/** BETA26 table transforms and UtilsFX.renderTextureIn3D's 16-pixel quill extrusion. */
final class ResearchTableGeometry {
    static void tablePose(PoseStack pose, Direction facing) {
        pose.translate(.5, 1, .5);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        int angle = switch (facing) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
        pose.mulPose(Axis.YP.rotationDegrees(angle));
    }

    static void quillPose(PoseStack pose) {
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.translate(-.5, .1, .125);
        pose.mulPose(Axis.YP.rotationDegrees(60));
        pose.scale(.5f, .5f, .5f);
    }

    static void quill(PoseStack pose, VertexConsumer out, float minU, float maxU, float minV, float maxV,
                      int light, int overlay) {
        float back = -.0625f;
        quad(pose,out,light,overlay,0,0,1,new float[][]{
                {0,0,0,maxU,maxV},{1,0,0,minU,maxV},{1,1,0,minU,minV},{0,1,0,maxU,minV}});
        quad(pose,out,light,overlay,0,0,-1,new float[][]{
                {0,1,back,maxU,minV},{1,1,back,minU,minV},{1,0,back,minU,maxV},{0,0,back,maxU,maxV}});
        float halfU = (maxU-minU)/32, halfV = (maxV-minV)/32;
        for (int pixel = 0; pixel < 16; pixel++) {
            float edge = pixel/16f, next = edge+1/16f;
            float u = maxU+(minU-maxU)*edge-halfU;
            quad(pose,out,light,overlay,-1,0,0,new float[][]{
                    {edge,0,back,u,maxV},{edge,0,0,u,maxV},{edge,1,0,u,minV},{edge,1,back,u,minV}});
            quad(pose,out,light,overlay,1,0,0,new float[][]{
                    {next,1,back,u,minV},{next,1,0,u,minV},{next,0,0,u,maxV},{next,0,back,u,maxV}});
            float v = maxV+(minV-maxV)*edge-halfV;
            quad(pose,out,light,overlay,0,1,0,new float[][]{
                    {0,next,0,maxU,v},{1,next,0,minU,v},{1,next,back,minU,v},{0,next,back,maxU,v}});
            quad(pose,out,light,overlay,0,-1,0,new float[][]{
                    {1,edge,0,minU,v},{0,edge,0,maxU,v},{0,edge,back,maxU,v},{1,edge,back,minU,v}});
        }
    }

    private static void quad(PoseStack pose, VertexConsumer out, int light, int overlay,
                             float nx, float ny, float nz, float[][] vertices) {
        for (var v : vertices) out.addVertex(pose.last().pose(),v[0],v[1],v[2])
                .setColor(1f,1f,1f,1f).setUv(v[3],v[4]).setOverlay(overlay).setLight(light)
                .setNormal(pose.last(),nx,ny,nz);
    }
    private ResearchTableGeometry() {}
}
