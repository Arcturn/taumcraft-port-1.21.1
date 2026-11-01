package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumcraft.port.infusion.matrix.InfusionMatrixEntity;
import thaumcraft.port.infusion.matrix.MatrixContent;
import thaumcraft.port.infusion.structure.StructureContent;

/** Original eight ModelCube meshes, pulsating UV overlay, startup transforms and deterministic halo. */
@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class InfusionMatrixRenderer implements BlockEntityRenderer<InfusionMatrixEntity> {
    private static final ModelPart.Cube CUBE=cube(0),OVERLAY=cube(32);
    private static final ResourceLocation NORMAL=texture("normal"),ANCIENT=texture("ancient"),ELDRITCH=texture("eldritch");
    public InfusionMatrixRenderer(BlockEntityRendererProvider.Context context){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(MatrixContent.ENTITY.get(),InfusionMatrixRenderer::new);}
    // TC6 expands the tile's render bounds for its moving pieces; NeoForge owns this hook on the renderer.
    @Override public AABB getRenderBoundingBox(InfusionMatrixEntity matrix){return new AABB(matrix.getBlockPos()).inflate(.1);}
    private static ModelPart.Cube cube(int v){return new ModelPart.Cube(0,v,-8,-8,-8,16,16,16,0,0,0,false,64,64,EnumSet.allOf(Direction.class));}
    private static ResourceLocation texture(String variant){return ResourceLocation.parse("thaumcraft:textures/blocks/infuser_"+variant+".png");}
    @Override public void render(InfusionMatrixEntity matrix,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var camera=Minecraft.getInstance().getCameraEntity();float ticks=(camera==null?0:camera.tickCount)+partial;
        ResourceLocation texture=NORMAL;
        if(matrix.getLevel()!=null){var pillar=matrix.getLevel().getBlockState(matrix.getBlockPos().offset(-1,-2,-1));if(pillar.is(StructureContent.PILLAR_ANCIENT.get()))texture=ANCIENT;if(pillar.is(StructureContent.PILLAR_ELDRITCH.get()))texture=ELDRITCH;}
        pose.pushPose();pose.translate(.5,.5,.5);
        pose.mulPose(Axis.YP.rotationDegrees(ticks%360*matrix.startUp));pose.mulPose(Axis.XP.rotationDegrees(35*matrix.startUp));pose.mulPose(Axis.ZP.rotationDegrees(45*matrix.startUp));
        float instability=Math.min(6,1+(matrix.stability<0?-matrix.stability*.66f:1)*(Math.min(matrix.craftCount,50)/50f));
        for(int pass=0;pass<(matrix.active?2:1);pass++){
            VertexConsumer buffer=buffers.getBuffer(pass==0?RenderType.entitySolid(texture):Layers.GLOW.get(texture));
            for(int a=0;a<2;a++)for(int b=0;b<2;b++)for(int c=0;c<2;c++){
                pose.pushPose();float jitter=matrix.active?.01f*matrix.startUp*instability:0;
                pose.translate((a==0?-.25f:.25f)+Mth.sin((ticks+a*10)/15)*jitter,(b==0?-.25f:.25f)+Mth.sin((ticks+b*10)/14)*jitter,(c==0?-.25f:.25f)+Mth.sin((ticks+c*10)/13)*jitter);
                if(a>0)pose.mulPose(Axis.XP.rotationDegrees(90));if(b>0)pose.mulPose(Axis.YP.rotationDegrees(90));if(c>0)pose.mulPose(Axis.ZP.rotationDegrees(90));pose.scale(.45f,.45f,.45f);
                int color=pass==0?-1:((int)((Mth.sin((ticks+a*2+b*3+c*4)/4)*.1f+.2f)*matrix.startUp*255)<<24)|0xCC19FF;
                (pass==0?CUBE:OVERLAY).compile(pose.last(),buffer,pass==0?light:0xF000F0,overlay,color);pose.popPose();
            }
        }
        pose.popPose();
        if(matrix.crafting&&matrix.craftCount>0)halo(pose,buffers.getBuffer(Layers.HALO),matrix.craftCount);
    }
    static void halo(PoseStack pose,VertexConsumer buffer,int count){
        emitHalo(pose,buffer,count,Minecraft.useFancyGraphics()?20:10);
    }
    /** TC6's POSITION_COLOR buffer writes an unsigned byte, even after the first 500 ticks. */
    static int haloAlpha(int count){return ((int)(255*(1-count/500f)))&255;}
    static void emitHalo(PoseStack pose,VertexConsumer buffer,int count,int quality){
        pose.pushPose();pose.translate(.5,.5,.5);Random random=new Random(245);
        float fraction=count/500f,scale=Math.min(count,50)/1000f;
        for(int i=0;i<quality;i++){
            pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat()*360));pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat()*360));pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat()*360));
            pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat()*360));pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat()*360));pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat()*360+fraction*360));
            float length=(random.nextFloat()*20+5)*scale,width=(random.nextFloat()*2+1)*scale;
            float[][] corners={{-.866f*width,length,-.5f*width},{.866f*width,length,-.5f*width},{0,length,width}};
            for(int side=0;side<3;side++){
                buffer.addVertex(pose.last().pose(),0,0,0).setColor(255,255,255,haloAlpha(count));
                for(int vertex:new int[]{side,(side+1)%3})buffer.addVertex(pose.last().pose(),corners[vertex][0],corners[vertex][1],corners[vertex][2]).setColor(255,0,255,0);
            }
        }
        pose.popPose();
    }
    private static final class Layers extends RenderType {
        // TC6 blends SRC_ALPHA, ONE. Vanilla ADDITIVE_TRANSPARENCY uses ONE, ONE and loses pulses and ray gradients.
        static final Map<ResourceLocation,RenderType> GLOW=Map.of(NORMAL,glow(NORMAL),ANCIENT,glow(ANCIENT),ELDRITCH,glow(ELDRITCH));
        static final RenderType HALO=create("thaumcraft_matrix_halo",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.TRIANGLES,1536,false,false,
                CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        private static RenderType glow(ResourceLocation texture){return create("thaumcraft_matrix_runes",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,false,false,
                CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER).setTextureState(new TextureStateShard(texture,false,false)).setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setOverlayState(OVERLAY).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
        private Layers(){super("unused",DefaultVertexFormat.POSITION,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    }
}
