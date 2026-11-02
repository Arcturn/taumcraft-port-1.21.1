package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumcraft.port.crafting.content.*;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ArtificeLampClient {
    private static final ModelLayerLocation CHEST = new ModelLayerLocation(ResourceLocation.parse("thaumcraft:hungry_chest"),"main");
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) { event.registerLayerDefinition(CHEST,ArtificeLampClient::chestLayer); }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerBlockEntityRenderer(CraftingContent.HUNGRY_CHEST_ENTITY.get(),HungryChestRenderer::new); }
    private static LayerDefinition chestLayer() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        root.addOrReplaceChild("lid",CubeListBuilder.create().texOffs(0,0).addBox(0,-5,-14,14,5,14),PartPose.offset(1,7,15));
        root.addOrReplaceChild("base",CubeListBuilder.create().texOffs(0,19).addBox(0,0,0,14,10,14),PartPose.offset(1,6,1));
        root.addOrReplaceChild("knob",CubeListBuilder.create().texOffs(0,0).addBox(-1,-2,-15,2,4,1),PartPose.offset(8,7,15));
        return LayerDefinition.create(mesh,64,64);
    }
    public static final class HungryChestRenderer implements BlockEntityRenderer<HungryChestEntity> {
        private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/chesthungry.png");
        private final ModelPart model,lid,knob;
        public HungryChestRenderer(BlockEntityRendererProvider.Context context) { model=context.bakeLayer(CHEST);lid=model.getChild("lid");knob=model.getChild("knob"); }
        @Override public void render(HungryChestEntity chest,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
            pose.pushPose();pose.translate(0,1,1);pose.scale(1,-1,-1);pose.translate(.5,.5,.5);
            float angle=switch(chest.getBlockState().getValue(CraftingDeviceBlock.FACING)) { case NORTH -> 180;case WEST -> 90;case EAST -> -90;default -> 0; };
            pose.mulPose(Axis.YP.rotationDegrees(angle));pose.translate(-.5,-.5,-.5);
            float openness=1-chest.openness(partialTick); openness=1-openness*openness*openness;
            lid.xRot=knob.xRot=-openness*(float)Math.PI/2;
            model.render(pose,buffers.getBuffer(RenderType.entityCutout(TEXTURE)),light,overlay);pose.popPose();
        }
    }
    private ArtificeLampClient() {}
}
