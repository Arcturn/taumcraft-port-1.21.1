package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import thaumcraft.port.aspect.Aspect;

/** BETA26 entity-aspect labels: yaw-facing world quads, five per row, no scan gate. */
final class ScannerWorldAspects {
    private static float tagScale;
    static void reset() { tagScale=0; }
    static void hide() { tagScale=ScannerVisualRules.nextTagScale(tagScale,false); }

    static void render(RenderLevelStageEvent event,Entity entity,Map<Aspect,Integer> aspects) {
        if(aspects.isEmpty()) {hide();return;}
        var mc=Minecraft.getInstance();
        if(mc.player==null) return;
        tagScale=ScannerVisualRules.nextTagScale(tagScale,true);
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double x=Mth.lerp(partial,entity.xo,entity.getX()),y=Mth.lerp(partial,entity.yo,entity.getY())+entity.getBbHeight()+.5;
        double z=Mth.lerp(partial,entity.zo,entity.getZ());
        var camera=event.getCamera().getPosition();
        float yaw=(float)Math.toDegrees(Math.atan2(camera.x-x,camera.z-z))+180;
        var pose=new PoseStack();
        // AFTER_LEVEL is dispatched after vanilla has popped its model-view matrix.
        pose.mulPose(event.getModelViewMatrix());
        pose.translate(x-camera.x,y-camera.y,z-camera.z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        var buffers=mc.renderBuffers().bufferSource();
        int index=0;
        for(var entry:aspects.entrySet()) {
            var aspect=entry.getKey();
            pose.pushPose();
            pose.translate(ScannerVisualRules.tagHorizontalOffset(index,aspects.size(),tagScale),
                    ScannerVisualRules.tagVerticalOffset(index,tagScale),0);
            pose.scale(tagScale,tagScale,tagScale);
            // Original Z=90 rotation combined with UtilsFX's rotated UV convention.
            var type=RenderType.textSeeThrough(BookAssets.id("textures/aspects/"+aspect.id()+".png"));
            var buffer=buffers.getBuffer(type);
            int color=aspect.color();
            float[][] quad={{-.5f,-.5f,1,1},{-.5f,.5f,1,0},{.5f,.5f,0,0},{.5f,-.5f,0,1}};
            for(var vertex:quad) buffer.addVertex(pose.last().pose(),vertex[0],vertex[1],0)
                    .setColor((color>>16)&255,(color>>8)&255,color&255,191).setUv(vertex[2],vertex[3]).setLight(0x00DC0000);
            buffers.endBatch(type);
            if(entry.getValue()>=0) {
                pose.mulPose(Axis.ZP.rotationDegrees(180));
                pose.scale(.04f,.04f,.04f);
                pose.translate(0,6,-.1);
                String amount=Integer.toString(entry.getValue());
                int width=mc.font.width(amount);
                mc.font.drawInBatch(amount,14-width,1,0xFF111111,false,pose.last().pose(),buffers,Font.DisplayMode.SEE_THROUGH,0,0xF000F0);
                pose.translate(0,0,-.1);
                mc.font.drawInBatch(amount,13-width,0,0xFFFFFFFF,false,pose.last().pose(),buffers,Font.DisplayMode.SEE_THROUGH,0,0xF000F0);
            }
            pose.popPose();index++;
        }
        buffers.endBatch();
    }
    private ScannerWorldAspects() {}
}
