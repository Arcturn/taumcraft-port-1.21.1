package thaumcraft.port.auromancy.client.media;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpellBatRenderingTest {
    @Test
    void visibleSpellBatKeepsItsFocusTintAndHurtOverlay(){
        var capture=new Capture(0x3399cc,127,OverlayTexture.pack(0,3));
        FocusMediaRenderers.renderBatModel(FocusMediaRenderers.batLayer().bakeRoot(),new PoseStack(),capture,
                0xF000F0,capture.overlay,-1,0x3399cc);
        assertTrue(capture.vertices>0);
    }
    @Test
    void alliedInvisibleSpellBatUsesLegacyWhiteFifteenPercentAlpha(){
        var capture=new Capture(0xffffff,38,OverlayTexture.NO_OVERLAY);
        FocusMediaRenderers.renderBatModel(FocusMediaRenderers.batLayer().bakeRoot(),new PoseStack(),capture,
                0xF000F0,capture.overlay,0x26ffffff,0x3399cc);
        assertTrue(capture.vertices>0);
        assertTrue(capture.alpha/255f>.1f,"Multiplying the invisible alpha by .5 makes the vanilla translucent-cull shader discard the entire bat");
    }
    private static final class Capture implements VertexConsumer {
        final int rgb,alpha,overlay;int vertices;
        Capture(int rgb,int alpha,int overlay){this.rgb=rgb;this.alpha=alpha;this.overlay=overlay;}
        @Override public VertexConsumer addVertex(float x,float y,float z){vertices++;return this;}
        @Override public VertexConsumer setColor(int r,int g,int b,int a){assertEquals(rgb,r<<16|g<<8|b);assertEquals(alpha,a);return this;}
        @Override public VertexConsumer setUv(float u,float v){return this;}
        @Override public VertexConsumer setUv1(int u,int v){assertEquals(overlay,u|v<<16);return this;}
        @Override public VertexConsumer setUv2(int u,int v){assertEquals(0xF000F0,u|v<<16);return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){return this;}
    }
}
