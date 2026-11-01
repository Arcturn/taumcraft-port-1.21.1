package thaumcraft.port.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** TC6 q=0 revealing pass: additive color, no depth test and no depth writes. */
final class RiftRenderTypes extends RenderType {
    static final RenderType REVEALED = create("thaumcraft_revealed_rift", DefaultVertexFormat.POSITION,
            VertexFormat.Mode.QUADS, 4096, false, false, CompositeState.builder()
                    .setShaderState(RENDERTYPE_END_GATEWAY_SHADER)
                    .setTextureState(MultiTextureStateShard.builder()
                            .add(ResourceLocation.withDefaultNamespace("textures/environment/end_sky.png"), false, false)
                            .add(ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png"), false, false)
                            .build())
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(new DepthTestStateShard("revealed_rift_no_depth", 519) {
                        @Override public void setupRenderState() {
                            // Vanilla NO_DEPTH_TEST is a no-op and relies on a previous batch's
                            // cleanup. This can also be the first entity batch in the frame.
                            RenderSystem.disableDepthTest();
                        }
                    })
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private RiftRenderTypes() {
        super("unused", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS,
                0, false, false, () -> {}, () -> {});
    }
}
