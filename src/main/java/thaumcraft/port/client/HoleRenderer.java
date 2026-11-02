package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import thaumcraft.port.auromancy.effect.*;

/** Original 16-layer ender.frag sphere projection, on the tunnel's solid interior faces. */
@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HoleRenderer implements BlockEntityRenderer<HoleBlockEntity> {
    private static ShaderInstance shader;
    public HoleRenderer(BlockEntityRendererProvider.Context context){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(FocusEffectContent.HOLE_ENTITY.get(),HoleRenderer::new);}
    @SubscribeEvent public static void shader(RegisterShadersEvent event){
        shader=null;
        try{event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumcraft:portable_hole"),DefaultVertexFormat.POSITION),loaded->shader=loaded);}
        catch(IOException error){LogUtils.getLogger().warn("Cannot load portable-hole shader; using vanilla portal",error);}
    }
    private static ShaderInstance activeShader(){
        var client=Minecraft.getInstance();var camera=client.getCameraEntity();
        if(shader!=null&&camera!=null){
            // ShaderHelper uses the render-view entity's age; TileHoleRenderer's
            // callback uses the local player's angles, even for a detached view.
            var orientation=client.player==null?camera:client.player;
            shader.safeGetUniform("Time").set(camera.tickCount);
            shader.safeGetUniform("Yaw").set((float)Math.toRadians(orientation.getYRot()));
            shader.safeGetUniform("Pitch").set((float)-Math.toRadians(orientation.getXRot()));
        }
        return shader;
    }
    @Override public void render(HoleBlockEntity hole,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var level=hole.getLevel();if(level==null)return;var buffer=buffers.getBuffer(shader==null?RenderType.endPortal():Layer.PORTAL);var matrix=pose.last().pose();
        for(Direction face:Direction.values()){
            var neighbor=hole.getBlockPos().relative(face);var state=level.getBlockState(neighbor);
            if(state.is(FocusEffectContent.HOLE.get())||!state.isSolidRender(level,neighbor))continue;
            PortableHoleGeometry.emit(face,matrix,buffer);
        }
    }
    private static final class Layer extends RenderType {
        static final RenderType PORTAL=create("thaumcraft_portable_hole",DefaultVertexFormat.POSITION,VertexFormat.Mode.QUADS,1536,false,false,
                CompositeState.builder().setShaderState(new ShaderStateShard(HoleRenderer::activeShader)).setTextureState(new TextureStateShard(ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png"),false,false)).setCullState(CULL).createCompositeState(false));
        private Layer(){super("unused",DefaultVertexFormat.POSITION,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    }
}
