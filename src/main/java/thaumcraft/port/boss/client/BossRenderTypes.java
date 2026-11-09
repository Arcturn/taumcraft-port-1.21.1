package thaumcraft.port.boss.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

final class BossRenderTypes extends RenderType {
    static RenderType billboard(ResourceLocation texture,boolean additive){
        return create("thaumcraft_boss_billboard",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,4096,false,true,CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER).setTextureState(new TextureStateShard(texture,false,false))
                .setTransparencyState(additive?LIGHTNING_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
    private BossRenderTypes(){super("unused",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.TRIANGLES,0,false,false,()->{},()->{});}
}
