package thaumcraft.port.artifice.siphon.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Quaternionf;
import thaumcraft.port.artifice.siphon.VoidSiphonBlock;
import thaumcraft.port.artifice.siphon.VoidSiphonEntity;

/** Source star-field apertures: six small top faces and six tall base faces. */
public final class VoidSiphonRenderer implements BlockEntityRenderer<VoidSiphonEntity> {
    static final ResourceLocation STARS=ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    private static ShaderInstance shader;
    public VoidSiphonRenderer(BlockEntityRendererProvider.Context context){}
    static void registerShader(RegisterShadersEvent event){
        shader=null;
        try{event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumcraft:void_siphon"),DefaultVertexFormat.POSITION),loaded->shader=loaded);}
        catch(IOException error){LogUtils.getLogger().warn("Cannot load void-siphon star shader",error);}
    }
    static ShaderInstance activeShader(){
        var client=Minecraft.getInstance();var camera=client.getCameraEntity();
        if(shader!=null&&camera!=null){var orientation=client.player==null?camera:client.player;
            shader.safeGetUniform("Time").set(camera.tickCount);
            shader.safeGetUniform("Yaw").set((float)Math.toRadians(orientation.getYRot()));
            shader.safeGetUniform("Pitch").set((float)-Math.toRadians(orientation.getXRot()));
        }
        return shader==null?GameRenderer.getPositionShader():shader;
    }
    @Override public void render(VoidSiphonEntity siphon,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!siphon.getBlockState().getValue(VoidSiphonBlock.ENABLED))return;
        faces(pose,buffers.getBuffer(Layers.TOP),.875,.126,true);
        faces(pose,buffers.getBuffer(Layers.BASE),.3125,.26,false);
    }
    private static void faces(PoseStack pose,VertexConsumer out,double height,double offset,boolean top){
        pose.pushPose();pose.translate(.5,height,.5);
        for(var face:Direction.values()){
            pose.pushPose();pose.mulPose(new Quaternionf().rotationAxis((float)Math.PI/2,-face.getStepY(),face.getStepX(),-face.getStepZ()));
            if(face.getStepZ()<0){pose.translate(0,0,offset);pose.mulPose(Axis.YP.rotationDegrees(180));}else pose.translate(0,0,-offset);
            if(top||face.getStepZ()!=0)pose.mulPose(Axis.ZN.rotationDegrees(90));
            if(top)pose.scale(.2f,.2f,.2f);else pose.scale(.25f,.5f,.25f);
            var matrix=pose.last().pose();
            out.addVertex(matrix,-.5f,-.5f,0);out.addVertex(matrix,.5f,-.5f,0);out.addVertex(matrix,.5f,.5f,0);out.addVertex(matrix,-.5f,.5f,0);
            pose.popPose();
        }
        pose.popPose();
    }
    private static final class Layers extends RenderType {
        static final RenderType TOP=layer("top",true),BASE=layer("base",false);
        private static RenderType layer(String name,boolean additive){return create("thaumcraft_siphon_"+name,DefaultVertexFormat.POSITION,VertexFormat.Mode.QUADS,1536,false,false,
                CompositeState.builder().setShaderState(new ShaderStateShard(VoidSiphonRenderer::activeShader)).setTextureState(new TextureStateShard(STARS,false,false))
                        .setTransparencyState(additive?LIGHTNING_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
        private Layers(){super("unused",DefaultVertexFormat.POSITION,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    }
}
