package thaumcraft.port.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** FXSlimyBubble and the slime-item fragments of FXBreakingFade, with their original alpha fade. */
final class AlchemyLiquidParticle extends TextureSheetParticle {
    private static final ResourceLocation SHEET = ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
    private static final ParticleRenderType BUBBLES = layer(SHEET, true), TAINT = layer(TextureAtlas.LOCATION_BLOCKS, false);
    private final boolean bubble;
    private final float baseAlpha, uOffset, vOffset;
    private int frame = 144;
    private AlchemyLiquidParticle(ClientLevel level, double x, double y, double z, boolean bubble) {
        super(level, x, y, z);
        this.bubble = bubble;
        var r = level.random;
        uOffset = r.nextFloat() * 3; vOffset = r.nextFloat() * 3;
        if (bubble) {
            lifetime = 15 + r.nextInt(5); quadSize = .075f + r.nextFloat() * .075f;
            xd = yd = zd = 0; hasPhysics = false; setSize(.01f, .01f);
            baseAlpha = .8f; setColor(.3f - r.nextFloat() * .1f, 0, .4f + r.nextFloat() * .1f);
        } else {
            setSprite(Minecraft.getInstance().getItemRenderer().getModel(new ItemStack(Items.SLIME_BALL), level, null, 0).getParticleIcon());
            lifetime = (int)(66 / (r.nextFloat() * .9f + .1f));
            boolean bright = r.nextBoolean(); baseAlpha = bright ? .4f : .6f;
            setColor(bright ? .6f : .3f, 0, .3f); quadSize *= .5f; gravity = 1;
            double vx = r.nextDouble() * 2 - 1, vy = r.nextDouble() * 2 - 1, vz = r.nextDouble() * 2 - 1;
            double scale = (r.nextDouble() + r.nextDouble() + 1) * .15 * .964 / Math.max(.0001, Math.sqrt(vx*vx + vy*vy + vz*vz));
            xd = vx * scale; yd = vy * scale + .1; zd = vz * scale;
        }
        alpha = baseAlpha;
    }
    static AlchemyLiquidParticle bubble(ClientLevel level, double x, double y, double z) { return new AlchemyLiquidParticle(level, x, y, z, true); }
    static AlchemyLiquidParticle taint(ClientLevel level, double x, double y, double z) { return new AlchemyLiquidParticle(level, x, y, z, false); }
    @Override public void tick() {
        if (!bubble) { super.tick(); alpha = baseAlpha * Math.max(0, 1 - (float)age / lifetime); return; }
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) remove();
        if (age - 1 < 6) { frame = 144 + age / 2; if (age == 5) y += .1; }
        else if (age < lifetime - 4) { yd += .005; frame = 147 + age % 4 / 2; }
        else { yd /= 2; frame = 150 - (lifetime - age) / 2; }
        y += yd;
    }
    @Override protected float getU0() { return bubble ? frame % 16 / 64f : sprite.getU(uOffset / 4); }
    @Override protected float getU1() { return bubble ? getU0() + 1/64f : sprite.getU((uOffset + 1) / 4); }
    @Override protected float getV0() { return bubble ? frame / 16 / 64f : sprite.getV(vOffset / 4); }
    @Override protected float getV1() { return bubble ? getV0() + 1/64f : sprite.getV((vOffset + 1) / 4); }
    @Override public ParticleRenderType getRenderType() { return bubble ? BUBBLES : TAINT; }
    private static ParticleRenderType layer(ResourceLocation texture, boolean blur) {
        return new ParticleRenderType() {
            @Override public BufferBuilder begin(Tesselator tessellator, TextureManager textures) {
                RenderSystem.depthMask(false); RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
                RenderSystem.setShader(GameRenderer::getParticleShader);
                if (blur) textures.getTexture(texture).setFilter(true, false);
                RenderSystem.setShaderTexture(0, texture);
                return tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            }
        };
    }
}
