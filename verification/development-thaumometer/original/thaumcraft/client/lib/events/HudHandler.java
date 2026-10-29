/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.ScaledResolution
 *  net.minecraft.client.gui.inventory.GuiContainer
 *  net.minecraft.client.renderer.GlStateManager
 *  net.minecraft.client.renderer.OpenGlHelper
 *  net.minecraft.client.renderer.RenderHelper
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.inventory.Slot
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.MathHelper
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 *  org.lwjgl.opengl.GL11
 */
package thaumcraft.client.lib.events;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.items.tools.ItemSanityChecker;
import thaumcraft.common.items.tools.ItemThaumometer;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.world.aura.AuraChunk;

public class HudHandler {
    final ResourceLocation HUD = new ResourceLocation("thaumcraft", "textures/gui/hud.png");
    public LinkedBlockingQueue<KnowledgeGainTracker> knowledgeGainTrackers = new LinkedBlockingQueue();
    public static final ResourceLocation BOOK = new ResourceLocation("thaumcraft", "textures/items/thaumonomicon.png");
    public static final ResourceLocation[] KNOW_TYPE = new ResourceLocation[]{new ResourceLocation("thaumcraft", "textures/research/knowledge_theory.png"), new ResourceLocation("thaumcraft", "textures/research/knowledge_observation.png")};
    float kgFade = 0.0f;
    public static AuraChunk currentAura = new AuraChunk(null, 0, 0.0f, 0.0f);
    private final float VISCON = 525.0f;
    long nextsync = 0L;
    DecimalFormat secondsFormatter = new DecimalFormat("#######.#");
    ItemStack lastItem = null;
    int lastCount = 0;
    final ResourceLocation TAGBACK = new ResourceLocation("thaumcraft", "textures/aspects/_back.png");

    @SideOnly(value=Side.CLIENT)
    void renderHuds(Minecraft mc, float renderTickTime, EntityPlayer player, long time) {
        GL11.glPushMatrix();
        ScaledResolution sr = new ScaledResolution(Minecraft.func_71410_x());
        GL11.glClear((int)256);
        GL11.glMatrixMode((int)5889);
        GL11.glLoadIdentity();
        GL11.glOrtho((double)0.0, (double)sr.func_78327_c(), (double)sr.func_78324_d(), (double)0.0, (double)1000.0, (double)3000.0);
        GL11.glMatrixMode((int)5888);
        GL11.glLoadIdentity();
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-2000.0f);
        int ww = sr.func_78326_a();
        int hh = sr.func_78328_b();
        GL11.glEnable((int)3042);
        GL11.glBlendFunc((int)770, (int)771);
        this.renderHudsInGUI(mc, renderTickTime, player, time, ww, hh);
        if (mc.field_71415_G) {
            if (mc.func_71382_s()) {
                mc.field_71446_o.func_110577_a(this.HUD);
                ItemStack handStack = player.func_184614_ca();
                boolean rC = false;
                boolean rT = false;
                boolean rS = false;
                int start = 0;
                for (int a = 0; a < 2; ++a) {
                    if (handStack != null && !handStack.func_190926_b()) {
                        if (!rC && handStack.func_77973_b() instanceof ICaster) {
                            this.renderCastingWandHud(mc, renderTickTime, player, time, handStack, start);
                            rC = true;
                            if (!ModConfig.CONFIG_GRAPHICS.dialBottom) {
                                start += 33;
                            }
                        } else if (!rT && handStack.func_77973_b() instanceof ItemThaumometer) {
                            this.renderThaumometerHud(mc, renderTickTime, player, time, ww, hh, start);
                            rT = true;
                            start += 80;
                        } else if (!rS && handStack.func_77973_b() instanceof ItemSanityChecker) {
                            this.renderSanityHud(mc, Float.valueOf(renderTickTime), player, time, start);
                            rS = true;
                            start += 75;
                        }
                    }
                    handStack = player.func_184592_cb();
                }
            }
        }
        GL11.glDisable((int)3042);
        GL11.glPopMatrix();
    }

    @SideOnly(value=Side.CLIENT)
    void renderHudsInGUI(Minecraft mc, float renderTickTime, EntityPlayer player, long time, int ww, int hh) {
        if (this.kgFade > 0.0f) {
            this.renderKnowledgeGains(mc, renderTickTime, player, time, ww, hh);
        }
    }

    @SideOnly(value=Side.CLIENT)
    void renderKnowledgeGains(Minecraft mc, float renderTickTime, EntityPlayer player, long time, int ww, int hh) {
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)(this.kgFade / 40.0f));
        mc.field_71446_o.func_110577_a(BOOK);
        UtilsFX.drawTexturedQuadFull(ww - 17, hh - 17, -90.0);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        LinkedBlockingQueue<KnowledgeGainTracker> temp = new LinkedBlockingQueue<KnowledgeGainTracker>();
        int a = 0;
        while (!this.knowledgeGainTrackers.isEmpty()) {
            KnowledgeGainTracker current = this.knowledgeGainTrackers.poll();
            if (current != null) {
                float b;
                float g;
                float r;
                float size;
                float m2;
                float wot3;
                float m;
                mc.field_71446_o.func_110577_a(KNOW_TYPE[current.type.ordinal()]);
                Random rand = new Random(current.seed);
                GL11.glPushMatrix();
                float s = 16.0f;
                float x = ww / 4 + rand.nextInt(32);
                float y = hh / 3 + rand.nextInt(32);
                float wot = 0.0f;
                if ((float)current.progress < (float)current.max * 0.66f) {
                    float q = ((float)current.progress - renderTickTime) / ((float)current.max * 0.66f);
                    s *= q;
                    m = (float)Math.sin((double)q * Math.PI - 1.5707963267948966) * 0.5f + 0.5f;
                    y *= m;
                    float d = (float)Math.sin((double)m * Math.PI * 0.5);
                    x *= d;
                } else {
                    wot = (float)(current.max - current.progress) + renderTickTime;
                    float wot2 = wot / ((float)current.max * 0.33f);
                    m = (float)Math.sin((double)wot2 * Math.PI * 2.0 - 1.5707963267948966) * 0.5f + 1.5f;
                    if ((double)wot2 < 0.5) {
                        s *= wot2 * 2.0f;
                    }
                    s *= m;
                }
                float xx = (float)(ww - 12 + rand.nextInt(8)) - x;
                float yy = (float)(hh - 12 + rand.nextInt(8)) - y;
                if (current.sparks && player.func_70681_au().nextInt((int)(1.0f + (float)current.progress / (float)current.max * 10.0f)) == 0) {
                    float r2 = (float)MathHelper.func_76136_a((Random)player.field_70170_p.field_73012_v, (int)255, (int)255) / 255.0f;
                    float g2 = (float)MathHelper.func_76136_a((Random)player.field_70170_p.field_73012_v, (int)189, (int)255) / 255.0f;
                    float b2 = (float)MathHelper.func_76136_a((Random)player.field_70170_p.field_73012_v, (int)64, (int)255) / 255.0f;
                    FXDispatcher.INSTANCE.drawSimpleSparkleGui(player.field_70170_p.field_73012_v, (double)xx + player.field_70170_p.field_73012_v.nextGaussian() * 5.0, (double)yy + player.field_70170_p.field_73012_v.nextGaussian() * 5.0, player.field_70170_p.field_73012_v.nextGaussian(), player.field_70170_p.field_73012_v.nextGaussian(), 24.0f, r2, g2, b2, player.field_70170_p.field_73012_v.nextInt(5), 0.9f, -1.0f);
                }
                GL11.glTranslatef((float)xx, (float)yy, (float)(-80 + a));
                GL11.glRotatef((float)(84 + rand.nextInt(12)), (float)0.0f, (float)0.0f, (float)-1.0f);
                UtilsFX.renderQuadCentered(1, 1, 0, s, 1.0f, 1.0f, 1.0f, 200, 771, 1.0f);
                if (current.category != null) {
                    mc.field_71446_o.func_110577_a(current.category.icon);
                    GL11.glTranslatef((float)0.0f, (float)0.0f, (float)1.0f);
                    UtilsFX.renderQuadCentered(1, 1, 0, s * 0.75f, 1.0f, 1.0f, 1.0f, 200, 771, 1.0f);
                }
                if ((float)current.progress > (float)current.max * 0.9f) {
                    wot3 = wot / ((float)current.max * 0.1f);
                    m2 = (float)Math.sin((double)wot3 * Math.PI * 2.0 - 1.5707963267948966) * 0.25f + 0.25f;
                    size = 64.0f * m2;
                    GL11.glRotatef((float)rand.nextInt(360), (float)0.0f, (float)0.0f, (float)-1.0f);
                    mc.field_71446_o.func_110577_a(ParticleEngine.particleTexture);
                    r = (float)MathHelper.func_76136_a((Random)rand, (int)255, (int)255) / 255.0f;
                    g = (float)MathHelper.func_76136_a((Random)rand, (int)189, (int)255) / 255.0f;
                    b = (float)MathHelper.func_76136_a((Random)rand, (int)64, (int)255) / 255.0f;
                    UtilsFX.renderQuadCentered(64, 64, 320 + rand.nextInt(16), size, r, g, b, 200, 1, 1.0f);
                }
                if ((float)current.progress < (float)current.max * 0.1f) {
                    wot3 = 1.0f - ((float)current.progress - renderTickTime) / ((float)current.max * 0.1f);
                    m2 = (float)Math.sin((double)wot3 * Math.PI * 2.0 - 1.5707963267948966) * 0.25f + 0.25f;
                    size = 32.0f * m2;
                    GL11.glRotatef((float)rand.nextInt(360), (float)0.0f, (float)0.0f, (float)-1.0f);
                    mc.field_71446_o.func_110577_a(ParticleEngine.particleTexture);
                    r = (float)MathHelper.func_76136_a((Random)rand, (int)255, (int)255) / 255.0f;
                    g = (float)MathHelper.func_76136_a((Random)rand, (int)189, (int)255) / 255.0f;
                    b = (float)MathHelper.func_76136_a((Random)rand, (int)64, (int)255) / 255.0f;
                    UtilsFX.renderQuadCentered(64, 64, 320 + rand.nextInt(16), size, r, g, b, 200, 1, 1.0f);
                }
                temp.offer(current);
                GL11.glPopMatrix();
            }
            ++a;
        }
        while (!temp.isEmpty()) {
            this.knowledgeGainTrackers.offer((KnowledgeGainTracker)temp.poll());
        }
    }

    @SideOnly(value=Side.CLIENT)
    void renderThaumometerHud(Minecraft mc, float partialTicks, EntityPlayer player, long time, int ww, int hh, int shifty) {
        String msg;
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        float base = MathHelper.func_76131_a((float)((float)currentAura.getBase() / 525.0f), (float)0.0f, (float)1.0f);
        float vis = MathHelper.func_76131_a((float)(currentAura.getVis() / 525.0f), (float)0.0f, (float)1.0f);
        float flux = MathHelper.func_76131_a((float)(currentAura.getFlux() / 525.0f), (float)0.0f, (float)1.0f);
        float count = (float)Minecraft.func_71410_x().func_175606_aa().field_70173_aa + partialTicks;
        float count2 = (float)Minecraft.func_71410_x().func_175606_aa().field_70173_aa / 3.0f + partialTicks;
        if (flux + vis > 1.0f) {
            float m = 1.0f / (flux + vis);
            base *= m;
            vis *= m;
            flux *= m;
        }
        float start = 10.0f + (1.0f - vis) * 64.0f;
        GL11.glPushMatrix();
        GL11.glEnable((int)3042);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glTranslated((double)2.0, (double)shifty, (double)0.0);
        if (vis > 0.0f) {
            GL11.glPushMatrix();
            GL11.glColor4f((float)0.7f, (float)0.4f, (float)0.9f, (float)1.0f);
            GL11.glTranslated((double)5.0, (double)start, (double)0.0);
            GL11.glScaled((double)1.0, (double)vis, (double)1.0);
            UtilsFX.drawTexturedQuad(0.0f, 0.0f, 88.0f, 56.0f, 8.0f, 64.0f, -90.0);
            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glBlendFunc((int)770, (int)1);
            GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)0.5f);
            GL11.glTranslated((double)5.0, (double)start, (double)0.0);
            UtilsFX.drawTexturedQuad(0.0f, 0.0f, 96.0f, 56.0f + count % 64.0f, 8.0f, vis * 64.0f, -90.0);
            GL11.glBlendFunc((int)770, (int)771);
            GL11.glPopMatrix();
            if (player.func_70093_af()) {
                GL11.glPushMatrix();
                GL11.glTranslated((double)16.0, (double)start, (double)0.0);
                GL11.glScaled((double)0.5, (double)0.5, (double)0.5);
                msg = this.secondsFormatter.format(currentAura.getVis());
                mc.field_71456_v.func_73731_b(mc.field_71466_p, msg, 0, 0, 0xEEAAFF);
                GL11.glPopMatrix();
                mc.field_71446_o.func_110577_a(this.HUD);
            }
        }
        if (flux > 0.0f) {
            start = 10.0f + (1.0f - flux - vis) * 64.0f;
            GL11.glPushMatrix();
            GL11.glColor4f((float)0.25f, (float)0.1f, (float)0.3f, (float)1.0f);
            GL11.glTranslated((double)5.0, (double)start, (double)0.0);
            GL11.glScaled((double)1.0, (double)flux, (double)1.0);
            UtilsFX.drawTexturedQuad(0.0f, 0.0f, 88.0f, 56.0f, 8.0f, 64.0f, -90.0);
            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glBlendFunc((int)770, (int)1);
            GL11.glColor4f((float)0.7f, (float)0.4f, (float)1.0f, (float)0.5f);
            GL11.glTranslated((double)5.0, (double)start, (double)0.0);
            UtilsFX.drawTexturedQuad(0.0f, 0.0f, 104.0f, 120.0f - count2 % 64.0f, 8.0f, flux * 64.0f, -90.0);
            GL11.glBlendFunc((int)770, (int)771);
            GL11.glPopMatrix();
            if (player.func_70093_af()) {
                GL11.glPushMatrix();
                GL11.glTranslated((double)16.0, (double)(start - 4.0f), (double)0.0);
                GL11.glScaled((double)0.5, (double)0.5, (double)0.5);
                msg = this.secondsFormatter.format(currentAura.getFlux());
                mc.field_71456_v.func_73731_b(mc.field_71466_p, msg, 0, 0, 0xAA11BB);
                GL11.glPopMatrix();
                mc.field_71446_o.func_110577_a(this.HUD);
            }
        }
        GL11.glPushMatrix();
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        UtilsFX.drawTexturedQuad(1.0f, 1.0f, 72.0f, 48.0f, 16.0f, 80.0f, -90.0);
        GL11.glPopMatrix();
        start = 8.0f + (1.0f - base) * 64.0f;
        GL11.glPushMatrix();
        UtilsFX.drawTexturedQuad(2.0f, start, 117.0f, 61.0f, 14.0f, 5.0f, -90.0);
        GL11.glPopMatrix();
        GL11.glPopMatrix();
    }

    @SideOnly(value=Side.CLIENT)
    void renderSanityHud(Minecraft mc, Float partialTicks, EntityPlayer player, long time, int shifty) {
        GL11.glPushMatrix();
        GL11.glEnable((int)3042);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glTranslated((double)0.0, (double)shifty, (double)0.0);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        UtilsFX.drawTexturedQuad(1.0f, 1.0f, 152.0f, 0.0f, 20.0f, 76.0f, -90.0);
        int p = ThaumcraftCapabilities.getWarp(player).get(IPlayerWarp.EnumWarpType.PERMANENT);
        int s = ThaumcraftCapabilities.getWarp(player).get(IPlayerWarp.EnumWarpType.NORMAL);
        int t = ThaumcraftCapabilities.getWarp(player).get(IPlayerWarp.EnumWarpType.TEMPORARY);
        float tw = p + s + t;
        float mod = 1.0f;
        if (tw > 100.0f) {
            mod = 100.0f / tw;
            tw = 100.0f;
        }
        int gap = (int)((100.0f - tw) / 100.0f * 48.0f);
        int wt = (int)((float)t / 100.0f * 48.0f * mod);
        int ws = (int)((float)s / 100.0f * 48.0f * mod);
        if (t > 0) {
            GL11.glPushMatrix();
            GL11.glColor4f((float)1.0f, (float)0.5f, (float)1.0f, (float)1.0f);
            UtilsFX.drawTexturedQuad(7.0f, 21 + gap, 200.0f, gap, 8.0f, wt + gap, -90.0);
            GL11.glPopMatrix();
        }
        if (s > 0) {
            GL11.glPushMatrix();
            GL11.glColor4f((float)0.75f, (float)0.0f, (float)0.75f, (float)1.0f);
            UtilsFX.drawTexturedQuad(7.0f, 21 + wt + gap, 200.0f, wt + gap, 8.0f, wt + ws + gap, -90.0);
            GL11.glPopMatrix();
        }
        if (p > 0) {
            GL11.glPushMatrix();
            GL11.glColor4f((float)0.5f, (float)0.0f, (float)0.5f, (float)1.0f);
            UtilsFX.drawTexturedQuad(7.0f, 21 + wt + ws + gap, 200.0f, wt + ws + gap, 8.0f, 48.0f, -90.0);
            GL11.glPopMatrix();
        }
        GL11.glPushMatrix();
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        UtilsFX.drawTexturedQuad(1.0f, 1.0f, 176.0f, 0.0f, 20.0f, 76.0f, -90.0);
        GL11.glPopMatrix();
        if (tw >= 100.0f) {
            GL11.glPushMatrix();
            GL11.glScaled((double)0.75, (double)0.75, (double)1.0);
            GL11.glTranslated((double)mc.field_71439_g.func_70681_au().nextInt(2), (double)mc.field_71439_g.func_70681_au().nextInt(2), (double)0.0);
            UtilsFX.drawTexturedQuad(3.0f, 3.0f, 216.0f, 0.0f, 20.0f, 16.0f, -90.0);
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
    }

    @SideOnly(value=Side.CLIENT)
    void renderChargeMeters(Minecraft mc, float renderTickTime, EntityPlayer player, long time, int ww, int hh) {
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        boolean start = false;
        boolean total = false;
    }

    @SideOnly(value=Side.CLIENT)
    void renderCastingWandHud(Minecraft mc, float partialTicks, EntityPlayer player, long time, ItemStack wandstack, int shifty) {
        ICaster wand = (ICaster)wandstack.func_77973_b();
        int short1 = 240;
        int short2 = 240;
        OpenGlHelper.func_77475_a((int)OpenGlHelper.field_77476_b, (float)((float)short1 / 1.0f), (float)((float)short2 / 1.0f));
        GlStateManager.func_179131_c((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)0.0f, (float)shifty, (float)0.0f);
        ScaledResolution sr = new ScaledResolution(Minecraft.func_71410_x());
        GL11.glClear((int)256);
        GL11.glMatrixMode((int)5889);
        GL11.glLoadIdentity();
        GL11.glOrtho((double)0.0, (double)sr.func_78327_c(), (double)sr.func_78324_d(), (double)0.0, (double)1000.0, (double)3000.0);
        GL11.glMatrixMode((int)5888);
        GL11.glLoadIdentity();
        int l = sr.func_78328_b();
        int dailLocation = ModConfig.CONFIG_GRAPHICS.dialBottom ? l - 32 : 0;
        GL11.glTranslatef((float)0.0f, (float)dailLocation, (float)-2000.0f);
        GL11.glEnable((int)3042);
        GL11.glBlendFunc((int)770, (int)771);
        mc.field_71446_o.func_110577_a(this.HUD);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glPushMatrix();
        GL11.glScaled((double)0.5, (double)0.5, (double)0.5);
        UtilsFX.drawTexturedQuad(0.0f, 0.0f, 0.0f, 0.0f, 64.0f, 64.0f, -90.0);
        GL11.glPopMatrix();
        GL11.glTranslatef((float)16.0f, (float)16.0f, (float)0.0f);
        short max = currentAura.getBase();
        int amt = (int)currentAura.getVis();
        ItemFocus focus = (ItemFocus)wand.getFocus(wandstack);
        ItemStack focusStack = wand.getFocusStack(wandstack);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)16.0f, (float)-10.0f, (float)0.0f);
        GL11.glScaled((double)0.5, (double)0.5, (double)0.5);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        int loc = (int)(30.0f * (float)amt / (float)max);
        GL11.glPushMatrix();
        Color ac = new Color(Aspect.ENERGY.getColor());
        GL11.glColor4f((float)((float)ac.getRed() / 255.0f), (float)((float)ac.getGreen() / 255.0f), (float)((float)ac.getBlue() / 255.0f), (float)0.8f);
        UtilsFX.drawTexturedQuad(-4.0f, 35 - loc, 104.0f, 0.0f, 8.0f, loc, -90.0);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        UtilsFX.drawTexturedQuad(-8.0f, -3.0f, 72.0f, 0.0f, 16.0f, 42.0f, -90.0);
        GL11.glPopMatrix();
        boolean sh = false;
        if (player.func_70093_af()) {
            GL11.glPushMatrix();
            GL11.glRotatef((float)-90.0f, (float)0.0f, (float)0.0f, (float)1.0f);
            String msg = this.secondsFormatter.format(amt);
            mc.field_71456_v.func_73731_b(mc.field_71466_p, msg, -32, -4, 0xFFFFFF);
            GL11.glPopMatrix();
            if (focus != null && focus.getVisCost(focusStack) > 0.0f) {
                float mod = wand.getConsumptionModifier(wandstack, player, false);
                GL11.glPushMatrix();
                msg = this.secondsFormatter.format(focus.getVisCost(focusStack) * mod);
                mc.field_71456_v.func_73731_b(mc.field_71466_p, msg, -32 - mc.field_71456_v.func_175179_f().func_78256_a(msg) / 2, 32, 0xFFFFFF);
                GL11.glPopMatrix();
            }
            mc.field_71446_o.func_110577_a(this.HUD);
        }
        GL11.glPopMatrix();
        if (focus != null) {
            ItemStack pickedStack = wand.getPickedBlock(player.field_71071_by.func_70448_g());
            if (pickedStack != null && !pickedStack.func_190926_b()) {
                this.renderWandTradeHud(partialTicks, player, time, pickedStack);
            } else {
                GL11.glPushMatrix();
                GL11.glTranslatef((float)-24.0f, (float)-24.0f, (float)90.0f);
                RenderHelper.func_74520_c();
                GL11.glDisable((int)2896);
                GL11.glEnable((int)32826);
                GL11.glEnable((int)2903);
                GL11.glEnable((int)2896);
                try {
                    mc.func_175599_af().func_180450_b(wand.getFocusStack(wandstack), 16, 16);
                }
                catch (Exception exception) {
                    // empty catch block
                }
                GL11.glDisable((int)2896);
                GL11.glPopMatrix();
            }
        }
        GL11.glDisable((int)3042);
        GL11.glPopMatrix();
    }

    @SideOnly(value=Side.CLIENT)
    public void renderWandTradeHud(float partialTicks, EntityPlayer player, long time, ItemStack picked) {
        if (picked == null) {
            return;
        }
        Minecraft mc = Minecraft.func_71410_x();
        int amount = this.lastCount;
        if (this.lastItem == null || this.lastItem.func_190926_b() || player.field_71071_by.func_194015_p() > 0 || !picked.func_77969_a(this.lastItem)) {
            amount = 0;
            for (ItemStack is : player.field_71071_by.field_70462_a) {
                if (is == null || is.func_190926_b() || !is.func_77969_a(picked)) continue;
                amount += is.func_190916_E();
            }
            this.lastItem = picked;
            player.field_71071_by.func_70296_d();
        }
        this.lastCount = amount;
        GL11.glPushMatrix();
        RenderHelper.func_74520_c();
        GL11.glDisable((int)2896);
        GL11.glEnable((int)32826);
        GL11.glEnable((int)2903);
        GL11.glEnable((int)2896);
        try {
            mc.func_175599_af().func_180450_b(picked, -8, -8);
        }
        catch (Exception exception) {
            // empty catch block
        }
        GL11.glDisable((int)2896);
        GL11.glPushMatrix();
        String am = "" + amount;
        int sw = mc.field_71466_p.func_78256_a(am);
        GL11.glTranslatef((float)0.0f, (float)(-mc.field_71466_p.field_78288_b), (float)500.0f);
        GL11.glScalef((float)0.5f, (float)0.5f, (float)0.5f);
        for (int a = -1; a <= 1; ++a) {
            for (int b = -1; b <= 1; ++b) {
                if (a != 0 && b != 0 || a == 0 && b == 0) continue;
                mc.field_71466_p.func_78276_b(am, a + 16 - sw, b + 24, 0);
            }
        }
        mc.field_71466_p.func_78276_b(am, 16 - sw, 24, 0xFFFFFF);
        GL11.glPopMatrix();
        GL11.glPopMatrix();
    }

    public void renderAspectsInGui(GuiContainer gui, EntityPlayer player, ItemStack stack, int sd, int sx, int sy) {
        AspectList tags = ThaumcraftCraftingManager.getObjectTags(stack);
        if (tags == null) {
            return;
        }
        GL11.glPushMatrix();
        int x = 0;
        int y = 0;
        int index = 0;
        if (tags.size() > 0) {
            for (Aspect tag : tags.getAspectsSortedByAmount()) {
                if (tag == null) continue;
                x = sx + index * 18;
                y = sy + sd - 16;
                UtilsFX.drawTag(x, y, tag, tags.getAmount(tag), 0, gui.field_73735_i);
                ++index;
            }
        }
        GL11.glPopMatrix();
    }

    private boolean isMouseOverSlot(Slot par1Slot, int par2, int par3, int par4, int par5) {
        int var4 = par4;
        int var5 = par5;
        return (par2 -= var4) >= par1Slot.field_75223_e - 1 && par2 < par1Slot.field_75223_e + 16 + 1 && (par3 -= var5) >= par1Slot.field_75221_f - 1 && par3 < par1Slot.field_75221_f + 16 + 1;
    }

    public static class KnowledgeGainTracker {
        IPlayerKnowledge.EnumKnowledgeType type;
        ResearchCategory category;
        int progress;
        int max;
        long seed;
        boolean sparks = false;

        public KnowledgeGainTracker(IPlayerKnowledge.EnumKnowledgeType type, ResearchCategory category, int progress, long seed) {
            this.type = type;
            this.category = category;
            if (type == IPlayerKnowledge.EnumKnowledgeType.THEORY) {
                progress += 10;
            }
            this.progress = progress;
            this.max = progress;
            this.seed = seed;
        }
    }
}

