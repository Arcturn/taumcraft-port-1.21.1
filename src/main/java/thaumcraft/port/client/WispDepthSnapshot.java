package thaumcraft.port.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

/** One reusable depth texture; Fabulous's final post-process clears the original opaque depth. */
final class WispDepthSnapshot implements AutoCloseable {
    private int texture = -1;
    private int width;
    private int height;
    private boolean valid;

    void invalidate() { valid = false; }
    boolean isValid() { return valid; }
    int textureId() { return texture; }

    void capture(RenderTarget main) {
        RenderSystem.assertOnRenderThread();
        int oldActiveTexture = GlStateManager._getActiveTexture();
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int oldTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int oldReadFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        try {
            if (texture == -1) texture = TextureUtil.generateTextureId();
            RenderSystem.bindTexture(texture);
            if (width != main.width || height != main.height) {
                width = main.width;
                height = main.height;
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24,
                        width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (ByteBuffer) null);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            }
            // Only the READ binding changes; the particle framebuffer remains the draw target.
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
            valid = true;
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldReadFramebuffer);
            RenderSystem.bindTexture(oldTexture);
            RenderSystem.activeTexture(oldActiveTexture);
        }
    }

    @Override public void close() {
        RenderSystem.assertOnRenderThread();
        if (texture != -1) TextureUtil.releaseTextureId(texture);
        texture = -1;
        width = height = 0;
        valid = false;
    }
}
