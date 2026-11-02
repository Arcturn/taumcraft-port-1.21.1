package thaumcraft.port.golem.client;

import com.mojang.blaze3d.vertex.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import thaumcraft.port.client.SalisMundusParticle;

final class GolemRenderLayers extends RenderType {
    private static ShaderInstance transparentShader;
    private record LayerKey(ResourceLocation texture,boolean xray) {}
    private static final Map<LayerKey,RenderType> TRANSPARENT = new HashMap<>();
    static void shaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),ResourceLocation.parse("thaumcraft:golem_transparent"),DefaultVertexFormat.NEW_ENTITY),shader->transparentShader=shader);
    }
    static RenderType entity(ResourceLocation texture,int color,boolean xray) {
        if(!xray&&(color>>>24)==255)return RenderType.entityTranslucent(texture);
        return TRANSPARENT.computeIfAbsent(new LayerKey(texture,xray),key->create("thaumcraft_golem_transparent",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,true,true,
                CompositeState.builder().setShaderState(new ShaderStateShard(()->transparentShader==null?GameRenderer.getRendertypeEntityTranslucentShader():transparentShader))
                .setTextureState(new TextureStateShard(texture,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(xray?NO_DEPTH_TEST:LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false)));
    }
    static final RenderType ORB=create("thaumcraft_golem_orb",DefaultVertexFormat.PARTICLE,VertexFormat.Mode.QUADS,1536,false,false,
            CompositeState.builder().setShaderState(new ShaderStateShard(SalisMundusParticle::legacyShader))
                    .setTextureState(new TextureStateShard(ResourceLocation.parse("thaumcraft:textures/misc/particles.png"),true,false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).setLightmapState(LIGHTMAP).createCompositeState(false));
    private GolemRenderLayers(){super("unused",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,0,false,false,()->{},()->{});}
}
