package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.RevealingEquipment;
import thaumcraft.port.content.RevealingReadout;

/** Targeted container and note readouts provided by TC6's Goggles of Revealing. */
@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class RevealingHud {
    private static float tagScale;

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){tagScale=0;}

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();
        // TC6 keeps world tags behind an open container/inventory screen.
        if(mc.level!=null&&mc.player!=null&&mc.getCameraEntity() instanceof Player&&!mc.options.hideGui
                &&RevealingEquipment.hasGoggles(mc.player)&&mc.hitResult instanceof BlockHitResult hit&&hit.getType()==HitResult.Type.BLOCK) {
            var pos=hit.getBlockPos();
            var aspects=RevealingReadout.aspects(mc.level.getBlockEntity(pos));
            if(!aspects.isEmpty()) {
                tagScale=RevealingVisualRules.renderScale(tagScale,true);
                drawAspects(event,RevealingVisualRules.anchor(pos,hit.getDirection(),mc.level.isEmptyBlock(pos.above()),tagScale),
                        Vec3.atCenterOf(pos),aspects);
            }
            var state=mc.level.getBlockState(pos);
            if(state.getBlock() instanceof NoteBlock)drawText(event,Vec3.atCenterOf(pos).add(0,1,0),"Note: "+state.getValue(NoteBlock.NOTE));
        }
        tagScale=RevealingVisualRules.afterFrame(tagScale);
    }

    private static PoseStack facing(RenderLevelStageEvent event,Vec3 anchor,Vec3 facingOrigin) {
        var mc=Minecraft.getInstance();
        var entity=mc.getCameraEntity();
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double x=Mth.lerp(partial,entity.xo,entity.getX()),z=Mth.lerp(partial,entity.zo,entity.getZ());
        var camera=event.getCamera().getPosition();
        float yaw=(float)Math.toDegrees(Math.atan2(x-facingOrigin.x,z-facingOrigin.z))+180;
        var pose=new PoseStack();
        pose.mulPose(event.getModelViewMatrix());
        pose.translate(anchor.x-camera.x,anchor.y-camera.y,anchor.z-camera.z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        return pose;
    }

    private static void drawAspects(RenderLevelStageEvent event,Vec3 anchor,Vec3 facingOrigin,Map<Aspect,Integer> aspects) {
        var mc=Minecraft.getInstance();
        var pose=facing(event,anchor,facingOrigin);
        var buffers=mc.renderBuffers().bufferSource();
        int index=0;
        for(var entry:aspects.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            var aspect=entry.getKey();
            pose.pushPose();
            pose.translate(ScannerVisualRules.tagHorizontalOffset(index,aspects.size(),tagScale),
                    ScannerVisualRules.tagVerticalOffset(index,tagScale),0);
            pose.scale(tagScale,tagScale,tagScale);
            var type=RenderType.textSeeThrough(BookAssets.id("textures/aspects/"+aspect.id()+".png"));
            var buffer=buffers.getBuffer(type);
            int color=aspect.color();
            float[][] quad={{-.5f,-.5f,1,1},{-.5f,.5f,1,0},{.5f,.5f,0,0},{.5f,-.5f,0,1}};
            for(var vertex:quad)buffer.addVertex(pose.last().pose(),vertex[0],vertex[1],0)
                    .setColor((color>>16)&255,(color>>8)&255,color&255,191).setUv(vertex[2],vertex[3]).setLight(0x00DC0000);
            buffers.endBatch(type);
            if(entry.getValue()>=0) {
                pose.mulPose(Axis.ZP.rotationDegrees(180));
                pose.scale(.04f,.04f,.04f);
                pose.translate(0,6,-.1);
                String amount=Integer.toString(entry.getValue());int width=mc.font.width(amount);
                mc.font.drawInBatch(amount,14-width,1,0xFF111111,false,pose.last().pose(),buffers,Font.DisplayMode.SEE_THROUGH,0,0xF000F0);
                pose.translate(0,0,-.1);
                mc.font.drawInBatch(amount,13-width,0,0xFFFFFFFF,false,pose.last().pose(),buffers,Font.DisplayMode.SEE_THROUGH,0,0xF000F0);
            }
            pose.popPose();index++;
        }
        buffers.endBatch();
    }

    private static void drawText(RenderLevelStageEvent event,Vec3 anchor,String text) {
        var mc=Minecraft.getInstance();var pose=facing(event,anchor,anchor);
        pose.mulPose(Axis.ZP.rotationDegrees(180));pose.scale(.0125f,.0125f,.0125f);
        var buffers=mc.renderBuffers().bufferSource();
        mc.font.drawInBatch(text,1-mc.font.width(text)/2,1,0xFFFFFFFF,true,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0,0xF000F0);
        buffers.endBatch();
    }
    private RevealingHud() {}
}
