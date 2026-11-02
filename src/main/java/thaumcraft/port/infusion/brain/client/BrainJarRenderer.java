package thaumcraft.port.infusion.brain.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.EnumSet;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumcraft.port.infusion.brain.BrainJarContent;
import thaumcraft.port.infusion.brain.BrainJarEntity;

/** ModelBrain's original three meshes and 128x64 UVs, followed by ModelJar's translucent brine. */
@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BrainJarRenderer implements BlockEntityRenderer<BrainJarEntity> {
    private static final ResourceLocation BRAIN=ResourceLocation.parse("thaumcraft:textures/models/brain2.png"),BRINE=ResourceLocation.parse("thaumcraft:textures/models/jarbrine.png");
    // The legacy ModelRenderer sets mirror after addBox: the baked source cubes are not mirrored.
    private static final ModelPart.Cube CEREBRUM=cube(0,0,0,0,0,12,10,16,128,64);
    private static final ModelPart.Cube CEREBELLUM=cube(64,0,0,0,0,8,3,7,128,64);
    private static final ModelPart.Cube STEM=cube(0,32,0,0,0,2,6,2,128,64);
    private static final ModelPart.Cube LIQUID=cube(0,0,-4,-11,-4,8,10,8,64,32);
    public BrainJarRenderer(BlockEntityRendererProvider.Context context){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(BrainJarContent.ENTITY.get(),BrainJarRenderer::new);}
    private static ModelPart.Cube cube(int u,int v,float x,float y,float z,float width,float height,float depth,float textureWidth,float textureHeight){
        return new ModelPart.Cube(u,v,x,y,z,width,height,depth,0,0,0,false,textureWidth,textureHeight,EnumSet.allOf(Direction.class));
    }
    @Override public void render(BrainJarEntity jar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var player=Minecraft.getInstance().player;float bob=Mth.sin((player==null?0:player.tickCount)/14f)*.03f+.03f;
        emit(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(BRAIN)),()->buffers.getBuffer(RenderType.entityTranslucent(BRINE)),jar.interpolatedRotation(partial),bob,light,overlay);
    }
    static void emit(PoseStack pose,VertexConsumer brain,Supplier<VertexConsumer> liquid,float rotation,float bob,int light,int overlay){
        pose.pushPose();pose.translate(.5,.01,.5);pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.pushPose();pose.translate(0,-.8f+bob,0);
        pose.mulPose(Axis.YP.rotationDegrees(rotation*180/3.141593f));pose.mulPose(Axis.YP.rotationDegrees(-90));pose.scale(.4f,.4f,.4f);
        pose.pushPose();pose.translate(-6/16f,8/16f,-8/16f);CEREBRUM.compile(pose.last(),brain,light,overlay,-1);pose.popPose();
        pose.pushPose();pose.translate(-4/16f,18/16f,0);CEREBELLUM.compile(pose.last(),brain,light,overlay,-1);pose.popPose();
        pose.pushPose();pose.translate(-1/16f,18/16f,-2/16f);pose.mulPose(Axis.XP.rotation(.4089647f));STEM.compile(pose.last(),brain,light,overlay,-1);pose.popPose();
        pose.popPose();
        // Requesting a different dynamic buffer may flush the first one; acquire brine after brain emission.
        LIQUID.compile(pose.last(),liquid.get(),light,overlay,-1);
        pose.popPose();
    }
}
