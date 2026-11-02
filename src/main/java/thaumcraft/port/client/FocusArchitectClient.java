package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import thaumcraft.port.auromancy.focus.*;
import thaumcraft.port.auromancy.focus.medium.FocusMediumPlan;
import thaumcraft.port.auromancy.item.CasterManager;
import thaumcraft.port.content.CasterItem;

/** PLAN's independent sixteen-block ray, connected faces and original selected-axis arrows. */
@EventBusSubscriber(modid="thaumcraft", value=Dist.CLIENT)
public final class FocusArchitectClient extends RenderType {
    private static final RenderType ARROWS = create("thaumcraft_focus_architect_arrows", DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                    .setTextureState(new TextureStateShard(ResourceLocation.parse("thaumcraft:textures/misc/architect_arrows.png"),false,false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    private record Preview(net.minecraft.client.multiplayer.ClientLevel level, net.minecraft.core.BlockPos origin, Direction face, int components, int period) { }
    private static Preview previous;
    private static java.util.List<net.minecraft.core.BlockPos> cachedBlocks = java.util.List.of();
    private static FocusMediumPlan find(FocusPackage pack) {
        if (pack == null) return null;
        for (var element : pack.nodes) {
            if (element instanceof FocusMediumPlan plan) return plan;
            if (element instanceof FocusPackage child) { var found = find(child); if (found != null) return found; }
            if (element instanceof FocusModSplit split) for (var child : split.getSplitPackages()) { var found = find(child); if (found != null) return found; }
        }
        return null;
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        var mc = Minecraft.getInstance(); if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        var hand = CasterManager.heldCasterHand(mc.player); if (hand == null) return;
        var stack = mc.player.getItemInHand(hand); var plan = find(CasterItem.getFocus(stack)); if (plan == null) return;
        var target = plan.getArchitectMOP(stack, mc.level, mc.player); if (target.getType() != HitResult.Type.BLOCK) return;
        var preview = new Preview(mc.level,target.getBlockPos(),target.getDirection(),stack.getComponents().hashCode(),mc.player.tickCount/5);
        if (!preview.equals(previous)) { previous = preview; cachedBlocks = plan.getArchitectBlocks(stack,mc.level,target.getBlockPos(),target.getDirection(),mc.player); }
        var blocks = cachedBlocks; if (blocks.isEmpty()) return;
        var buffers = mc.renderBuffers().bufferSource(); var pose = event.getPoseStack(); var camera = event.getCamera().getPosition();
        ArchitectOverlay.render(pose, buffers, camera, blocks);
        pose.pushPose(); var pos = target.getBlockPos(); pose.translate(pos.getX()+.5-camera.x,pos.getY()+.5-camera.y,pos.getZ()+.5-camera.z);
        float r = Mth.sin(mc.player.tickCount/4f+pos.getX())*.2f+.3f;
        float g = Mth.sin(mc.player.tickCount/3f+pos.getY())*.2f+.3f;
        float b = Mth.sin(mc.player.tickCount/2f+pos.getZ())*.2f+.8f;
        pose.mulPose(Axis.XP.rotationDegrees(90));
        for (var axis : Direction.Axis.values()) if (plan.showAxis(stack, mc.level, mc.player, target.getDirection(), axis)) {
            pose.pushPose(); if (axis == Direction.Axis.X) pose.mulPose(Axis.ZP.rotationDegrees(90)); else if (axis == Direction.Axis.Y) pose.mulPose(Axis.XP.rotationDegrees(90));
            arrow(pose, buffers.getBuffer(ARROWS), r,g,b); pose.mulPose(Axis.YP.rotationDegrees(90)); arrow(pose,buffers.getBuffer(ARROWS),r,g,b); pose.popPose();
        }
        pose.popPose(); ArchitectOverlay.flush(buffers); buffers.endBatch(ARROWS);
    }
    private static void arrow(PoseStack pose, VertexConsumer vertex, float r, float g, float b) {
        var matrix = pose.last().pose();
        vertex.addVertex(matrix,-.5f,-.5f,0).setUv(0,1).setColor(r,g,b,1);
        vertex.addVertex(matrix,.5f,-.5f,0).setUv(1,1).setColor(r,g,b,1);
        vertex.addVertex(matrix,.5f,.5f,0).setUv(1,0).setColor(r,g,b,1);
        vertex.addVertex(matrix,-.5f,.5f,0).setUv(0,0).setColor(r,g,b,1);
    }
    private FocusArchitectClient() { super("unused",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,0,false,false,()->{},()->{}); }
}
