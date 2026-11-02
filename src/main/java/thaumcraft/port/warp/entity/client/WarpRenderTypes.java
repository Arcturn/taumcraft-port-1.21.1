package thaumcraft.port.warp.entity.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

final class WarpRenderTypes extends RenderType {
    static RenderType ghost(ResourceLocation texture) {
        return create("thaumcraft_warp_ghost", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 4096, false, true,
                CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
    }
    static final RenderType RAYS = create("thaumcraft_eldritch_rays", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES, 1536, false, false, CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER).setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    private WarpRenderTypes() { super("unused", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES, 0, false, false, ()->{}, ()->{}); }
}
