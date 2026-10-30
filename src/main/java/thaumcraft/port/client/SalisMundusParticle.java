package thaumcraft.port.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The Salis Mundus presets of BETA26 FXGeneric, on the unmodified original atlas. */
public final class SalisMundusParticle extends TextureSheetParticle {
    private static final ResourceLocation ORIGINAL_SHEET = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/particles.png");
    private static final DustRenderType[] LAYERS = createLayers();
    private static final ParticleRenderType SMOKE = LAYERS[0], GLOW = LAYERS[1];
    private static ShaderInstance dustShader;

    private static DustRenderType[] createLayers() {
        var first = new DustRenderType();
        var second = new DustRenderType();
        // NeoForge 21.1 orders non-vanilla particle layers by identity hash. Assign
        // our modes in that order so alpha smoke is always followed by additive glow.
        var smoke = System.identityHashCode(first) < System.identityHashCode(second) ? first : second;
        var glow = smoke == first ? second : first;
        glow.additive = true;
        return new DustRenderType[]{smoke, glow};
    }

    public static void registerShader(RegisterShadersEvent event) {
        dustShader = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath("thaumcraft", "salis_particle"), DefaultVertexFormat.PARTICLE),
                    loaded -> dustShader = loaded);
        } catch (IOException error) {
            LogUtils.getLogger().warn("Cannot load Salis Mundus shader; using vanilla particle shader", error);
        }
    }

    private static final class DustRenderType implements ParticleRenderType {
        private boolean additive;
        @Override public BufferBuilder begin(Tesselator tessellator, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    additive ? GlStateManager.DestFactor.ONE : GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShader(() -> dustShader != null ? dustShader : GameRenderer.getParticleShader());
            // TC6 samples its own sheet with texture.blur=true. Stitching the whole
            // sheet into vanilla's nearest-filtered atlas loses that metadata.
            textures.getTexture(ORIGINAL_SHEET).setFilter(true, false);
            RenderSystem.setShaderTexture(0, ORIGINAL_SHEET);
            return tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }
        @Override public String toString() { return additive ? "THAUMCRAFT_SALIS_GLOW" : "THAUMCRAFT_SALIS_SMOKE"; }
    }

    private int delay, grid = 64, firstFrame = 512, frameCount = 16;
    private int[] finalFrames;
    private boolean loop = true, additive = true, angled;
    private float startSize = .05f, endSize = .1f, rotationStep, angleYaw, anglePitch;
    private float startRed = 1, startGreen = 1, startBlue = 1;
    private float endRed = 1, endGreen = 1, endBlue = 1;
    private float[] alphaKeys = {0, 1, 0};
    private double slowdown = .9800000190734863, randomX, randomY, randomZ, windX, windZ;

    private SalisMundusParticle(ClientLevel level, double x, double y, double z,
                               double vx, double vy, double vz) {
        super(level, x, y, z);
        setSize(.1f, .1f);
        xd = vx; yd = vy; zd = vz;
        lifetime = 64;
        // FXGeneric retains ordinary particle collision and ambient lighting.
        hasPhysics = true;
        alpha = 0;
        quadSize = startSize;
    }

    public void sparkle(float size, float red, float green, float blue,
                        int delayTicks, float decay, float gravityValue) {
        var r = level.random;
        lifetime = 64 + r.nextInt(16);
        firstFrame = r.nextFloat() < .2f ? 320 : 512;
        alphaKeys = new float[6 + r.nextInt(lifetime / 3)];
        for (int i = 1; i < alphaKeys.length - 1; i++) alphaKeys[i] = r.nextFloat();
        startSize = size * .1f;
        endSize = startSize * 2;
        colours(red, green, blue, red, green, blue);
        delay = Math.max(0, delayTicks);
        slowdown = decay;
        gravity = gravityValue;
        randomX = randomZ = .0005;
        randomY = .001;
        wind(.0005);
        alpha = 0;
    }

    /** FXDispatcher.drawSimpleSparkle with the scanner's original baseAge=4. */
    public void scannerSparkle(float size, float red, float green, float blue, int delayTicks) {
        var preset = ScannerVisualRules.sparkle(level.random, size);
        lifetime = preset.lifetime();
        firstFrame = preset.firstFrame();
        alphaKeys = preset.alphaKeys();
        startSize = preset.startHalfSize();
        endSize = preset.endHalfSize();
        colours(red, green, blue, red, green, blue);
        delay = Math.max(0, delayTicks);
        slowdown = 1;
        gravity = 0;
        randomX = randomZ = .0005;
        randomY = .001;
        wind(.0005);
        alpha = 0;
    }

    static ParticleRenderType scannerGlowLayer() { return GLOW; }

    public void crucible(CrucibleVisualRules.Bubble preset, float red, float green, float blue) {
        lifetime = preset.lifetime();
        grid = 64; firstFrame = preset.firstFrame(); frameCount = 1; loop = false;
        finalFrames = preset.finalFrames();
        startSize = endSize = preset.halfSize();
        colours(red, green, blue, red, green, blue);
        gravity = preset.gravity();
        randomX = randomY = randomZ = preset.movement();
        additive = preset.additive();
        // FXGeneric.setNoClip(false) writes canCollide=false: overflow must pass
        // through the rim and fall down the sides instead of accumulating on top.
        hasPhysics = preset.collision();
        alphaKeys = new float[]{preset.alpha()};
        alpha = preset.alpha();
        quadSize = startSize;
    }

    public void smoke(float red, float green, float blue) {
        var r = level.random;
        lifetime = 20 + r.nextInt(15);
        grid = 16; firstFrame = 123; frameCount = 5; loop = false; additive = false;
        colours(red, green, blue, red, green, blue);
        alphaKeys = new float[]{1, .1f};
        startSize = .3f; endSize = (4 + r.nextFloat() * 3) * .1f;
        slowdown = .7;
        rotation(r.nextFloat(), r.nextBoolean() ? -1 : 1);
        alpha = 1;
    }

    /** Long-lived, silent pollution smoke; this is not the short BAMF craft explosion. */
    public void pollution(CrucibleVisualRules.Pollution preset) {
        lifetime = preset.lifetime();
        grid = CrucibleVisualRules.POLLUTION_GRID;
        firstFrame = CrucibleVisualRules.POLLUTION_FRAME;
        frameCount = 1; loop = false; additive = false; hasPhysics = true;
        var colour = CrucibleVisualRules.POLLUTION_COLOUR;
        colours(colour.red(), colour.green(), colour.blue(), colour.red(), colour.green(), colour.blue());
        alphaKeys = new float[]{CrucibleVisualRules.POLLUTION_ALPHA, 0};
        alpha = CrucibleVisualRules.POLLUTION_ALPHA;
        startSize = CrucibleVisualRules.POLLUTION_START_SIZE;
        endSize = CrucibleVisualRules.POLLUTION_END_SIZE;
        quadSize = startSize;
        slowdown = CrucibleVisualRules.POLLUTION_SLOWDOWN;
        wind(CrucibleVisualRules.POLLUTION_WIND);
        rotation(level.random.nextFloat(), level.random.nextBoolean() ? -1 : 1);
    }

    static ParticleRenderType legacyLayer(boolean additive) { return additive ? GLOW : SMOKE; }

    public void wispMote(int colour) {
        lifetime=10+level.random.nextInt(5); firstFrame=264; frameCount=8; loop=true;
        startSize=(1+level.random.nextFloat()*.25f)*.1f; endSize=.005f;
        colours(((colour>>16)&255)/255f,((colour>>8)&255)/255f,(colour&255)/255f,
                ((colour>>16)&255)/255f,((colour>>8)&255)/255f,(colour&255)/255f);
        alphaKeys=new float[]{.5f}; alpha=.5f; quadSize=startSize;
        randomX=randomZ=.0025; wind(.00025);
    }

    public void wispBurst(float size) {
        lifetime=31; grid=16; firstFrame=208; frameCount=31; loop=false;
        startSize=endSize=size*.1f; quadSize=startSize; alphaKeys=new float[]{1}; alpha=1;
    }

    public void flash() {
        var r = level.random;
        lifetime = 10 + r.nextInt(5);
        grid = 16; firstFrame = 77; frameCount = 1; loop = false;
        colours(1, .9f, 1, 1, .9f, 1);
        alphaKeys = new float[]{1, 0};
        startSize = (10 + r.nextFloat() * 2) * .1f; endSize = 0;
        rotation(r.nextFloat(), (float) r.nextGaussian());
        alpha = 1;
    }

    public void mote(int baseAge) {
        var r = level.random;
        float red = .25f + r.nextFloat() * .75f;
        float green = .25f + r.nextFloat() * .75f;
        float blue = .25f + r.nextFloat() * .75f;
        colours(red, green, blue, red, green, blue);
        lifetime = (int) (baseAge + (baseAge / 2) * r.nextFloat());
        alphaKeys = new float[]{0, .6f, .6f, 0};
        startSize = .1f; endSize = .05f;
        gravity = -.01f;
        randomX = randomZ = .0025;
        wind(.001);
    }

    public void wisp(int seed, float red, float green, float blue) {
        var r = level.random;
        if (seed > 0 && r.nextBoolean()) {
            angled = true;
            angleYaw = 90 * (float) r.nextGaussian();
            anglePitch = 90 * (float) r.nextGaussian();
        }
        lifetime = 25 + r.nextInt(20 + 20 * seed);
        grid = 16; firstFrame = 60 + r.nextInt(4); frameCount = 1; loop = false;
        colours(red, green, blue, .1f, 0, .1f);
        alphaKeys = new float[]{.75f, 0};
        startSize = .5f; endSize = (10 + r.nextFloat() * 4) * .1f;
        rotation(r.nextFloat(), r.nextBoolean() ? -2 - r.nextFloat() * 2 : 2 + r.nextFloat() * 2);
        alpha = .75f;
    }

    private void colours(float r, float g, float b, float r2, float g2, float b2) {
        startRed = r; startGreen = g; startBlue = b;
        endRed = r2; endGreen = g2; endBlue = b2;
        setColor(r, g, b);
    }

    private void rotation(float start, float speed) {
        roll = oRoll = start * Mth.TWO_PI;
        // Preserve FXGeneric's radians conversion followed by its extra 2*pi step.
        rotationStep = speed * Mth.DEG_TO_RAD * Mth.TWO_PI;
    }

    private void wind(double amount) {
        float angle = level.getMoonPhase() * (40 + level.random.nextInt(10)) * Mth.DEG_TO_RAD;
        windX = Mth.cos(angle) * .1 * amount;
        windZ = -Mth.sin(angle) * .1 * amount;
    }

    @Override public void tick() {
        // Original ParticleEngine starts the lifetime only when its delay expires.
        if (delay > 0 && --delay > 0) return;
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) { remove(); return; }
        oRoll = roll;
        roll += rotationStep;
        yd -= .04 * gravity;
        move(xd, yd, zd);
        xd *= slowdown; yd *= slowdown; zd *= slowdown;
        xd += level.random.nextGaussian() * randomX + windX;
        yd += level.random.nextGaussian() * randomY;
        zd += level.random.nextGaussian() * randomZ + windZ;
        if (onGround && slowdown != 1) { xd *= .7; zd *= .7; }
    }

    private int frame() {
        if (finalFrames != null) return CrucibleVisualRules.frame(firstFrame, finalFrames, age, lifetime);
        return firstFrame + (loop ? age % frameCount
                : Math.min(frameCount - 1, age * frameCount / Math.max(1, lifetime)));
    }
    @Override protected float getU0() { return (frame() % grid) / (float) grid; }
    @Override protected float getU1() { return (frame() % grid + 1) / (float) grid; }
    @Override protected float getV0() { return (frame() / grid) / (float) grid; }
    @Override protected float getV1() { return (frame() / grid + 1) / (float) grid; }

    @Override public float getQuadSize(float partialTick) {
        // SingleQuadParticle also uses this for the frustum bounds. Preserve the
        // original age-based scale while keeping large smoke/flash quads visible.
        return Mth.lerp(Mth.clamp(age / (float) lifetime, 0, 1), startSize, endSize);
    }

    @Override public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        if (delay > 0) return;
        float tickProgress = Mth.clamp(age / (float) lifetime, 0, 1);
        float key = tickProgress * (alphaKeys.length - 1);
        int index = Math.min(alphaKeys.length - 1, (int) key);
        alpha = Mth.lerp(key - index, alphaKeys[index], alphaKeys[Math.min(index + 1, alphaKeys.length - 1)]);
        float size = getQuadSize(partialTick);
        float progress = Mth.clamp((age + partialTick) / lifetime, 0, 1);
        rCol = Mth.lerp(progress, startRed, endRed);
        gCol = Mth.lerp(progress, startGreen, endGreen);
        bCol = Mth.lerp(progress, startBlue, endBlue);
        var position = camera.getPosition();
        float px = (float) (Mth.lerp(partialTick, xo, x) - position.x);
        float py = (float) (Mth.lerp(partialTick, yo, y) - position.y);
        float pz = (float) (Mth.lerp(partialTick, zo, z) - position.z);
        Quaternionf orientation = angled
                ? new Quaternionf().rotationY((-angleYaw + 90) * Mth.DEG_TO_RAD).rotateX((anglePitch + 90) * Mth.DEG_TO_RAD)
                : new Quaternionf(camera.rotation());
        orientation.rotateZ(roll + (roll - oRoll) * partialTick);
        int light = getLightColor(partialTick);
        renderQuad(buffer, orientation, px, py, pz, size, getU0(), getU1(), getV0(), getV1(),
                rCol, gCol, bCol, alpha, light);
    }

    static void renderQuad(VertexConsumer buffer, Quaternionf orientation, float px, float py, float pz,
                           float size, float u0, float u1, float v0, float v1,
                           float red, float green, float blue, float alpha, int light) {
        // Match SingleQuadParticle's front-facing winding and UVs. Reversing X
        // without reversing the vertex order makes the entire effect back-face culled.
        for (int corner = 0; corner < 4; corner++) {
            float cx = corner < 2 ? 1 : -1;
            float cy = corner == 0 || corner == 3 ? -1 : 1;
            var point = new Vector3f(cx, cy, 0).rotate(orientation).mul(size).add(px, py, pz);
            buffer.addVertex(point.x, point.y, point.z).setUv(cx > 0 ? u1 : u0, cy > 0 ? v0 : v1)
                    .setColor(red, green, blue, alpha).setLight(light);
        }
    }

    @Override public ParticleRenderType getRenderType() {
        return additive ? GLOW : SMOKE;
    }

    public static final class Provider implements ParticleProvider.Sprite<SimpleParticleType> {
        @Override public TextureSheetParticle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double vx, double vy, double vz) {
            return new SalisMundusParticle(level, x, y, z, vx, vy, vz);
        }
    }
}
