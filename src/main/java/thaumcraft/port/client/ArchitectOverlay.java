package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.joml.Quaternionf;

/** Original connected architect faces and corner frames, including their depth-independent overlay. */
final class ArchitectOverlay extends RenderType {
    private static final int[][] CORNERS={{4,5,6,7},{0,1,2,3},{0,1,4,5},{2,3,6,7},{0,2,4,6},{1,3,5,7}};
    private static final int[][] ROTATIONS={{0,90,270,180},{270,180,0,90},{180,90,270,0},{0,270,90,180},{270,180,0,90},{180,270,90,0}};
    private static final RenderType SIDE=layer("side"),CORNER=layer("corner");
    private static RenderType layer(String name) {
        return create("thaumcraft_architect_"+name,DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,2048,false,true,
                CompositeState.builder().setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                        .setTextureState(new TextureStateShard(ResourceLocation.parse("thaumcraft:textures/misc/frame_"+name+".png"),false,false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(NO_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
    static void render(RenderHighlightEvent.Block event,List<BlockPos> positions) {
        render(event.getPoseStack(), event.getMultiBufferSource(), event.getCamera().getPosition(), positions);
    }
    static void render(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, net.minecraft.world.phys.Vec3 eye, List<BlockPos> positions) {
        var blocks=new HashSet<>(positions);
        pose.pushPose();pose.translate(-eye.x,-eye.y,-eye.z);
        for (var pos:positions) {
            boolean[] corners=new boolean[8];
            for (int i=0;i<8;i++) corners[i]=!blocks.contains(pos.offset((i&1)==0?-1:1,0,0))
                    && !blocks.contains(pos.offset(0,0,(i&2)==0?-1:1)) && !blocks.contains(pos.offset(0,i<4?1:-1,0));
            pose.pushPose();pose.translate(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
            for (Direction face:Direction.values()) {
                if (blocks.contains(pos.relative(face))) continue;
                pose.pushPose();pose.mulPose(new Quaternionf().rotationAxis((float)Math.PI/2,-face.getStepY(),face.getStepX(),-face.getStepZ()));
                pose.translate(0,0,face.getStepZ()<0?-.5:.5);pose.mulPose(new Quaternionf().rotationZ(-(float)Math.PI/2));
                quad(pose,buffers.getBuffer(SIDE),.1f);
                for (int corner=0;corner<4;corner++) if (corners[CORNERS[face.ordinal()][corner]]) {
                    pose.pushPose();pose.mulPose(new Quaternionf().rotationZ(ROTATIONS[face.ordinal()][corner]*(float)Math.PI/180));
                    quad(pose,buffers.getBuffer(CORNER),.66f);pose.popPose();
                }
                pose.popPose();
            }
            pose.popPose();
        }
        pose.popPose();
    }
    static void flush(net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers) { buffers.endBatch(SIDE); buffers.endBatch(CORNER); }
    private static void quad(PoseStack pose,VertexConsumer vertices,float alpha) {
        var matrix=pose.last().pose();
        vertices.addVertex(matrix,-.5f,-.5f,0).setUv(0,1).setColor(1,1,1,alpha);
        vertices.addVertex(matrix,.5f,-.5f,0).setUv(1,1).setColor(1,1,1,alpha);
        vertices.addVertex(matrix,.5f,.5f,0).setUv(1,0).setColor(1,1,1,alpha);
        vertices.addVertex(matrix,-.5f,.5f,0).setUv(0,0).setColor(1,1,1,alpha);
    }
    private ArchitectOverlay() { super("unused",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,0,false,false,()->{},()->{}); }
}
