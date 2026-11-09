package thaumcraft.port.boss.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

final class BossRenderTypes extends RenderType {
    private static final java.util.Map<ResourceLocation,RenderType> TRANSLUCENT=new java.util.HashMap<>();
    /** Boss renderers in TC6 keep the Warden texture's faint cloth pixels down to 1/255 alpha. */
    static RenderType translucent(ResourceLocation texture){
        return TRANSLUCENT.computeIfAbsent(texture,key->create("thaumcraft_boss_translucent",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,true,true,CompositeState.builder()
                .setShaderState(new ShaderStateShard(()->{var shader=net.minecraft.client.Minecraft.getInstance().gameRenderer.getShader("thaumcraft:golem_transparent");return shader==null?net.minecraft.client.renderer.GameRenderer.getRendertypeEntityTranslucentShader():shader;}))
                .setTextureState(new TextureStateShard(key,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(true)));
    }

    static RenderType billboard(ResourceLocation texture,boolean additive){
        return create("thaumcraft_boss_billboard",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,4096,false,true,CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER).setTextureState(new TextureStateShard(texture,false,false))
                .setTransparencyState(additive?LIGHTNING_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
    private BossRenderTypes(){super("unused",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.TRIANGLES,0,false,false,()->{},()->{});}
}
