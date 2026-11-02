package thaumcraft.port.client.auromancy;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.*;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import thaumcraft.port.auromancy.item.CasterHand;
import thaumcraft.port.content.CasterItem;

/** The original gauntlet skin follows the actual player arm, including slim skins and left primary arm. */
@EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class GauntletRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("thaumcraft:textures/entity/armor/caster_basic.png");
    @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer) playerRenderer.addLayer(new GloveLayer(playerRenderer));
        }
    }
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        var key = ModelResourceLocation.inventory(ResourceLocation.parse("thaumcraft:caster_basic"));
        var source = event.getModels().get(key); if (source != null) event.getModels().put(key, new WornModel(source));
    }
    /** GUI/ground/frame retain the original item model; held gauntlets are drawn by the arm layer. */
    private static final class WornModel extends BakedModelWrapper<BakedModel> {
        private final BakedModel empty;
        WornModel(BakedModel source) { super(source); empty = new EmptyModel(source); }
        @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean left) {
            if (context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                    || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) return empty;
            return originalModel.applyTransform(context, pose, left);
        }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
    }
    private static final class EmptyModel extends BakedModelWrapper<BakedModel> {
        EmptyModel(BakedModel source) { super(source); }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) { return List.of(); }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) { return List.of(); }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
    }
    private static final class GloveLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        GloveLayer(PlayerRenderer parent) { super(parent); }
        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                float limbSwing, float limbAmount, float partial, float age, float yaw, float pitch) {
            if (player.isInvisible()) return;
            for (var arm : HumanoidArm.values()) {
                var hand = CasterHand.hand(player.getMainArm(), arm); if (!(player.getItemInHand(hand).getItem() instanceof CasterItem)) continue;
                pose.pushPose(); var part = arm == HumanoidArm.RIGHT ? getParentModel().rightArm : getParentModel().leftArm;
                part.translateAndRotate(pose); glove(pose, buffers, light, player.getSkin().model() == PlayerSkin.Model.SLIM, arm); pose.popPose();
            }
        }
    }
    @EventBusSubscriber(modid = "thaumcraft", value = Dist.CLIENT)
    public static final class HandEvents {
        @SubscribeEvent public static void hand(RenderHandEvent event) {
            if (!(event.getItemStack().getItem() instanceof CasterItem)) return;
            var mc = Minecraft.getInstance(); var player = mc.player; if (player == null) return;
            event.setCanceled(true); if (player.isInvisible()) return;
            var arm = CasterHand.arm(player.getMainArm(), event.getHand());
            var renderer = mc.getEntityRenderDispatcher().getRenderer(player);
            if (!(renderer instanceof PlayerRenderer playerRenderer)) return;
            var pose = event.getPoseStack(); pose.pushPose();
            firstPersonArmPose(pose, arm, event.getEquipProgress(), event.getSwingProgress());
            if (arm == HumanoidArm.RIGHT) playerRenderer.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
            else playerRenderer.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
            var part = arm == HumanoidArm.RIGHT ? playerRenderer.getModel().rightArm : playerRenderer.getModel().leftArm;
            pose.pushPose(); part.translateAndRotate(pose);
            glove(pose, event.getMultiBufferSource(), event.getPackedLight(), player.getSkin().model() == PlayerSkin.Model.SLIM, arm);
            pose.popPose(); pose.popPose();
        }
    }
    /** Vanilla empty-hand transform, shared for both arms; the skin and glove use the same posed arm. */
    private static void firstPersonArmPose(PoseStack pose, HumanoidArm arm, float equip, float swing) {
        float side = arm == HumanoidArm.RIGHT ? 1 : -1, root = Mth.sqrt(swing);
        pose.translate(side * (-.3f * Mth.sin(root * Mth.PI) + .64000005f), .4f * Mth.sin(root * Mth.TWO_PI) - .6f - equip * .6f, -.4f * Mth.sin(swing * Mth.PI) - .71999997f);
        pose.mulPose(Axis.YP.rotationDegrees(side * 45));
        pose.mulPose(Axis.YP.rotationDegrees(side * Mth.sin(root * Mth.PI) * 70));
        pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.sin(swing * swing * Mth.PI) * -20));
        pose.translate(side * -1, 3.6, 3.5); pose.mulPose(Axis.ZP.rotationDegrees(side * 120));
        pose.mulPose(Axis.XP.rotationDegrees(200)); pose.mulPose(Axis.YP.rotationDegrees(side * -135)); pose.translate(side * 5.6, 0, 0);
    }
    private static void glove(PoseStack pose, MultiBufferSource buffers, int light, boolean slim, HumanoidArm arm) {
        var vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        // Vanilla sleeves extend .25 px beyond the arm. An extra 1/64 px prevents z fighting without a bulky held-item scale.
        float width = CasterHand.armWidth(slim), center = CasterHand.armCenter(slim, HumanoidArm.RIGHT), inflate = .265625f;
        float x0 = center - width / 2 - inflate, x1 = center + width / 2 + inflate;
        float z0 = -2 - inflate, z1 = 2 + inflate;
        // Source JSON's cuff (y 1.5..7) and hand (y 0..6), remapped to the player's wrist y 10.
        sides(pose, vertices, light, arm, x0 - .04f, x1 + .04f, 3, 8.5f, z0 - .04f, z1 + .04f, true);
        sides(pose, vertices, light, arm, x0, x1, 4, 10 + inflate, z0, z1, false);
        face(pose, vertices, light, arm, new float[][]{{x0,10+inflate,z0},{x1,10+inflate,z0},{x1,10+inflate,z1},{x0,10+inflate,z1}}, 8,0,12,4, 0,1,0);
        // A closed rim avoids seeing through the sleeve where the gauntlet begins.
        face(pose, vertices, light, arm, new float[][]{{x0,3,z1},{x1,3,z1},{x1,3,z0},{x0,3,z0}}, 12,4,16,8, 0,-1,0);
    }
    private static void sides(PoseStack pose, VertexConsumer vertices, int light, HumanoidArm arm, float x0, float x1, float y0, float y1, float z0, float z1, boolean cuff) {
        face(pose,vertices,light,arm,new float[][]{{x0,y0,z1},{x0,y0,z0},{x0,y1,z0},{x0,y1,z1}},cuff?0:16,cuff?9:10,cuff?4:12,cuff?14.5f:16,-1,0,0);
        face(pose,vertices,light,arm,new float[][]{{x1,y0,z0},{x1,y0,z1},{x1,y1,z1},{x1,y1,z0}},cuff?4:12,cuff?9:10,cuff?8:16,cuff?14.5f:16,1,0,0);
        face(pose,vertices,light,arm,new float[][]{{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0}},cuff?4:8,cuff?1:10,cuff?8:12,cuff?6.5f:16,0,0,-1);
        face(pose,vertices,light,arm,new float[][]{{x1,y0,z1},{x0,y0,z1},{x0,y1,z1},{x1,y1,z1}},cuff?0:8,cuff?1:16,cuff?4:12,cuff?6.5f:10,0,0,1);
    }
    private static void face(PoseStack pose, VertexConsumer consumer, int light, HumanoidArm arm, float[][] points, float u0, float v0, float u1, float v1, float nx, float ny, float nz) {
        var entry = pose.last(); float[][] uv = {{u0,v0},{u1,v0},{u1,v1},{u0,v1}};
        // Reflect canonical right-arm geometry around local X, reverse winding and normals, and keep
        // UVs attached to their vertices. The decorated outer surface then stays outside on both arms.
        boolean left = arm == HumanoidArm.LEFT;
        for (int vertex = 0; vertex < 4; vertex++) { int i = left ? 3 - vertex : vertex;
            consumer.addVertex(entry, (left ? -points[i][0] : points[i][0])/16, points[i][1]/16, points[i][2]/16).setColor(255,255,255,255)
                    .setUv(uv[i][0]/16, uv[i][1]/16).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry,left ? -nx : nx,ny,nz);
        }
    }
    private GauntletRenderer() { }
}
