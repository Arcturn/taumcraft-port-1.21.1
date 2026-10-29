/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.particle.Particle
 *  net.minecraft.client.particle.ParticleLava$Factory
 *  net.minecraft.client.renderer.RenderGlobal
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.init.Items
 *  net.minecraft.init.SoundEvents
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumParticleTypes
 *  net.minecraft.util.SoundCategory
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.client.FMLClientHandler
 */
package thaumcraft.client.fx;

import java.awt.Color;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleLava;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.fx.beams.FXArc;
import thaumcraft.client.fx.beams.FXBeamBore;
import thaumcraft.client.fx.beams.FXBeamWand;
import thaumcraft.client.fx.beams.FXBolt;
import thaumcraft.client.fx.other.FXBlockWard;
import thaumcraft.client.fx.other.FXBoreStream;
import thaumcraft.client.fx.other.FXEssentiaStream;
import thaumcraft.client.fx.other.FXShieldRunes;
import thaumcraft.client.fx.other.FXVoidStream;
import thaumcraft.client.fx.particles.FXBlockRunes;
import thaumcraft.client.fx.particles.FXBoreParticles;
import thaumcraft.client.fx.particles.FXBoreSparkle;
import thaumcraft.client.fx.particles.FXBreakingFade;
import thaumcraft.client.fx.particles.FXFireMote;
import thaumcraft.client.fx.particles.FXGeneric;
import thaumcraft.client.fx.particles.FXGenericGui;
import thaumcraft.client.fx.particles.FXGenericP2E;
import thaumcraft.client.fx.particles.FXPlane;
import thaumcraft.client.fx.particles.FXSmokeSpiral;
import thaumcraft.client.fx.particles.FXSwarm;
import thaumcraft.client.fx.particles.FXVent;
import thaumcraft.client.fx.particles.FXVent2;
import thaumcraft.client.fx.particles.FXVisSparkle;
import thaumcraft.client.fx.particles.FXWispEG;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.tiles.crafting.TileCrucible;

public class FXDispatcher {
    public static FXDispatcher INSTANCE = new FXDispatcher();
    static int q = 0;

    public World getWorld() {
        return FMLClientHandler.instance().getClient().field_71441_e;
    }

    public void drawFireMote(float x, float y, float z, float vx, float vy, float vz, float r, float g, float b, float alpha, float scale) {
        boolean bb = this.getWorld().field_73012_v.nextBoolean();
        FXFireMote glow = new FXFireMote(this.getWorld(), x, y, z, vx, vy, vz, r, g, b, bb ? scale / 3.0f : scale, bb ? 1 : 0);
        glow.func_82338_g(alpha);
        ParticleEngine.addEffect(this.getWorld(), (Particle)glow);
    }

    public void drawAlumentum(float x, float y, float z, float vx, float vy, float vz, float r, float g, float b, float alpha, float scale) {
        FXFireMote glow = new FXFireMote(this.getWorld(), x, y, z, vx, vy, vz, r, g, b, scale, 1);
        glow.func_82338_g(alpha);
        ParticleEngine.addEffect(this.getWorld(), (Particle)glow);
    }

    public void drawTaintParticles(float x, float y, float z, float vx, float vy, float vz, float scale) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, vx, vy, vz);
        fb.func_187114_a(80 + this.getWorld().field_73012_v.nextInt(20));
        fb.func_70538_b(0.4f + this.getWorld().field_73012_v.nextFloat() * 0.2f, 0.1f + this.getWorld().field_73012_v.nextFloat() * 0.3f, 0.5f + this.getWorld().field_73012_v.nextFloat() * 0.2f);
        fb.setAlphaF(0.75f, 0.0f);
        fb.setGridSize(16);
        fb.setParticles(57 + this.getWorld().field_73012_v.nextInt(3), 1, 1);
        fb.setScale(scale, scale / 4.0f);
        fb.setLayer(1);
        fb.setSlowDown(0.975f);
        fb.setGravity(0.2f);
        fb.setRotationSpeed(this.getWorld().field_73012_v.nextFloat(), this.getWorld().field_73012_v.nextBoolean() ? -1.0f : 1.0f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawLightningFlash(double x, double y, double z, float r, float g, float b, float alpha, float scale) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, 0.0, 0.0, 0.0);
        fb.func_187114_a(5 + this.getWorld().field_73012_v.nextInt(5));
        fb.setGridSize(16);
        fb.func_70538_b(r, g, b);
        fb.setAlphaF(alpha, 0.0f);
        fb.setParticles(108 + this.getWorld().field_73012_v.nextInt(4), 1, 1);
        fb.setScale(scale);
        fb.setLayer(0);
        fb.setRotationSpeed(this.getWorld().field_73012_v.nextFloat(), 0.0f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawGenericParticles(double x, double y, double z, double mx, double my, double mz, GenPart part) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, mx, my, mz);
        fb.func_187114_a(part.age);
        fb.setRBGColorF(part.redStart, part.greenStart, part.blueStart, part.redEnd, part.greenEnd, part.blueEnd);
        fb.setAlphaF(part.alpha);
        fb.setLoop(part.loop);
        fb.setParticles(part.partStart, part.partNum, part.partInc);
        fb.setScale(part.scale);
        fb.setLayer(part.layer);
        fb.setRotationSpeed(part.rotstart, part.rot);
        fb.setSlowDown(part.slowDown);
        fb.setGravity(part.grav);
        fb.setGridSize(part.grid);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, part.delay);
    }

    public void drawGenericParticles(double x, double y, double z, double x2, double y2, double z2, float r, float g, float b, float alpha, boolean loop, int start, int num, int inc, int age, int delay, float scale, float rot, int layer) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        fb.func_187114_a(age);
        fb.func_70538_b(r, g, b);
        fb.func_82338_g(alpha);
        fb.setLoop(loop);
        fb.setParticles(start, num, inc);
        fb.setScale(scale);
        fb.setLayer(layer);
        fb.setRotationSpeed(rot);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, delay);
    }

    public void drawGenericParticles16(double x, double y, double z, double x2, double y2, double z2, float r, float g, float b, float alpha, boolean loop, int start, int num, int inc, int age, int delay, float scale, float rot, int layer) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        fb.setGridSize(16);
        fb.func_187114_a(age);
        fb.func_70538_b(r, g, b);
        fb.func_82338_g(alpha);
        fb.setLoop(loop);
        fb.setParticles(start, num, inc);
        fb.setScale(scale);
        fb.setLayer(layer);
        fb.setRotationSpeed(rot);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, delay);
    }

    public void drawLevitatorParticles(double x, double y, double z, double x2, double y2, double z2) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        fb.func_187114_a(200 + this.getWorld().field_73012_v.nextInt(100));
        fb.func_70538_b(0.5f, 0.5f, 0.2f);
        fb.setAlphaF(0.3f, 0.0f);
        fb.setGridSize(16);
        fb.setParticles(56, 1, 1);
        fb.setScale(2.0f, 5.0f);
        fb.setLayer(0);
        fb.setSlowDown(1.0);
        fb.setRotationSpeed(this.getWorld().field_73012_v.nextFloat(), this.getWorld().field_73012_v.nextBoolean() ? -1.0f : 1.0f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawStabilizerParticles(double x, double y, double z, double x2, double y2, double z2, int life) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        fb.func_187114_a(life + this.getWorld().field_73012_v.nextInt(life));
        fb.func_70538_b(0.5f, 0.2f, 0.5f);
        fb.setAlphaF(0.3f, 0.0f);
        fb.setGridSize(16);
        fb.setParticles(72 + this.getWorld().field_73012_v.nextInt(4), 1, 1);
        fb.setScale(1.0f, 10.0f);
        fb.setLayer(0);
        fb.setSlowDown(1.01);
        fb.setRotationSpeed(this.getWorld().field_73012_v.nextFloat(), this.getWorld().field_73012_v.nextBoolean() ? -1.0f : 1.0f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawGolemFlyParticles(double x, double y, double z, double x2, double y2, double z2) {
        try {
            FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
            fb.func_187114_a(20 + this.getWorld().field_73012_v.nextInt(5));
            fb.setAlphaF(0.3f, 0.0f);
            fb.setGridSize(16);
            fb.setParticles(56, 1, 1);
            fb.setScale(1.5f, 3.0f, 8.0f);
            fb.setLayer(0);
            fb.setSlowDown(1.0);
            fb.setWind(0.001);
            fb.setRotationSpeed(this.getWorld().field_73012_v.nextFloat(), this.getWorld().field_73012_v.nextBoolean() ? -1.0f : 1.0f);
            ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void drawPollutionParticles(BlockPos p) {
        float x = (float)p.func_177958_n() + 0.2f + this.getWorld().field_73012_v.nextFloat() * 0.6f;
        float y = (float)p.func_177956_o() + 0.2f + this.getWorld().field_73012_v.nextFloat() * 0.6f;
        float z = (float)p.func_177952_p() + 0.2f + this.getWorld().field_73012_v.nextFloat() * 0.6f;
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, (double)(this.getWorld().field_73012_v.nextFloat() - this.getWorld().field_73012_v.nextFloat()) * 0.005, 0.02, (double)(this.getWorld().field_73012_v.nextFloat() - this.getWorld().field_73012_v.nextFloat()) * 0.005);
        fb.func_187114_a(100 + this.getWorld().field_73012_v.nextInt(60));
        fb.func_70538_b(1.0f, 0.3f, 0.9f);
        fb.setAlphaF(0.5f, 0.0f);
        fb.setGridSize(16);
        fb.setParticles(56, 1, 1);
        fb.setScale(2.0f, 5.0f);
        fb.setLayer(1);
        fb.setSlowDown(1.0);
        fb.setWind(0.001);
        fb.setRotationSpeed(this.getWorld().field_73012_v.nextFloat(), this.getWorld().field_73012_v.nextBoolean() ? -1.0f : 1.0f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawBlockSparkles(BlockPos p, Vec3d start) {
        AxisAlignedBB bs = this.getWorld().func_180495_p(p).func_185900_c((IBlockAccess)this.getWorld(), p);
        bs.func_72314_b(0.1, 0.1, 0.1);
        int num = (int)(bs.func_72320_b() * 20.0);
        for (EnumFacing face : EnumFacing.values()) {
            IBlockState state = this.getWorld().func_180495_p(p.func_177972_a(face));
            if (state.func_185914_p() || state.isSideSolid((IBlockAccess)this.getWorld(), p.func_177972_a(face), face.func_176734_d())) continue;
            boolean rx = face.func_82601_c() == 0;
            boolean ry = face.func_96559_d() == 0;
            boolean rz = face.func_82599_e() == 0;
            double mx = 0.5 + (double)face.func_82601_c() * 0.51;
            double my = 0.5 + (double)face.func_96559_d() * 0.51;
            double mz = 0.5 + (double)face.func_82599_e() * 0.51;
            for (int a = 0; a < num * 2; ++a) {
                double x = mx;
                double y = my;
                double z = mz;
                if (rx) {
                    x += this.getWorld().field_73012_v.nextGaussian() * 0.6;
                }
                if (ry) {
                    y += this.getWorld().field_73012_v.nextGaussian() * 0.6;
                }
                if (rz) {
                    z += this.getWorld().field_73012_v.nextGaussian() * 0.6;
                }
                x = MathHelper.func_151237_a((double)x, (double)bs.field_72340_a, (double)bs.field_72336_d);
                y = MathHelper.func_151237_a((double)y, (double)bs.field_72338_b, (double)bs.field_72337_e);
                z = MathHelper.func_151237_a((double)z, (double)bs.field_72339_c, (double)bs.field_72334_f);
                float r = (float)MathHelper.func_76136_a((Random)this.getWorld().field_73012_v, (int)255, (int)255) / 255.0f;
                float g = (float)MathHelper.func_76136_a((Random)this.getWorld().field_73012_v, (int)189, (int)255) / 255.0f;
                float b = (float)MathHelper.func_76136_a((Random)this.getWorld().field_73012_v, (int)64, (int)255) / 255.0f;
                Vec3d v1 = new Vec3d((double)p.func_177958_n() + x, (double)p.func_177956_o() + y, (double)p.func_177952_p() + z);
                double delay = (double)this.getWorld().field_73012_v.nextInt(5) + v1.func_72438_d(start) * 16.0;
                this.drawSimpleSparkle(this.getWorld().field_73012_v, (double)p.func_177958_n() + x, (double)p.func_177956_o() + y, (double)p.func_177952_p() + z, 0.0, 0.0025, 0.0, 0.4f + (float)this.getWorld().field_73012_v.nextGaussian() * 0.1f, r, g, b, (int)delay, 1.0f, 0.01f, 16);
            }
        }
    }

    public void drawLineSparkle(Random rand, double x, double y, double z, double x2, double y2, double z2, float scale, float r, float g, float b, int delay, float decay, float grav, int baseAge) {
        boolean sp = (double)rand.nextFloat() < 0.2;
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        int age = baseAge * 4 + this.getWorld().field_73012_v.nextInt(baseAge);
        fb.func_187114_a(age);
        fb.func_70538_b(r, g, b);
        fb.setAlphaF(0.0f, 1.0f, 0.0f);
        fb.setParticles(sp ? 320 : 512, 16, 1);
        fb.setLoop(true);
        fb.setGravity(grav);
        fb.setScale(scale, scale * 2.0f, scale);
        fb.setLayer(0);
        fb.setSlowDown(decay);
        fb.setRandomMovementScale(5.0E-5f, 0.0f, 5.0E-5f);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, delay);
    }

    public void drawSimpleSparkle(Random rand, double x, double y, double z, double x2, double y2, double z2, float scale, float r, float g, float b, int delay, float decay, float grav, int baseAge) {
        boolean sp = (double)rand.nextFloat() < 0.2;
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        int age = baseAge * 4 + this.getWorld().field_73012_v.nextInt(baseAge);
        fb.func_187114_a(age);
        fb.func_70538_b(r, g, b);
        float[] alphas = new float[6 + rand.nextInt(age / 3)];
        for (int a = 1; a < alphas.length - 1; ++a) {
            alphas[a] = rand.nextFloat();
        }
        fb.setAlphaF(alphas);
        fb.setParticles(sp ? 320 : 512, 16, 1);
        fb.setLoop(true);
        fb.setGravity(grav);
        fb.setScale(scale, scale * 2.0f);
        fb.setLayer(0);
        fb.setSlowDown(decay);
        fb.setRandomMovementScale(5.0E-4f, 0.001f, 5.0E-4f);
        fb.setWind(5.0E-4);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, delay);
    }

    public void drawSimpleSparkleGui(Random rand, double x, double y, double x2, double y2, float scale, float r, float g, float b, int delay, float decay, float grav) {
        boolean sp = (double)rand.nextFloat() < 0.2;
        FXGenericGui fb = new FXGenericGui(this.getWorld(), x, y, 0.0, x2, y2, 0.0);
        fb.func_187114_a(32 + this.getWorld().field_73012_v.nextInt(8));
        fb.func_70538_b(r, g, b);
        fb.setAlphaF(0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f);
        fb.setParticles(sp ? 320 : 512, 16, 1);
        fb.setLoop(true);
        fb.setGravity(grav);
        fb.setScale(scale, scale * 2.0f);
        fb.setNoClip(false);
        fb.setLayer(4);
        fb.setSlowDown(decay);
        fb.setRandomMovementScale(0.025f, 0.025f, 0.0f);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, delay);
    }

    public void drawBlockMistParticles(BlockPos p, int c) {
        AxisAlignedBB bs = this.getWorld().func_180495_p(p).func_185900_c((IBlockAccess)this.getWorld(), p);
        Color color = new Color(c);
        for (int a = 0; a < 8; ++a) {
            double x = (double)p.func_177958_n() + bs.field_72340_a + (double)this.getWorld().field_73012_v.nextFloat() * (bs.field_72336_d - bs.field_72340_a);
            double y = (double)p.func_177956_o() + bs.field_72338_b + (double)this.getWorld().field_73012_v.nextFloat() * (bs.field_72337_e - bs.field_72338_b);
            double z = (double)p.func_177952_p() + bs.field_72339_c + (double)this.getWorld().field_73012_v.nextFloat() * (bs.field_72334_f - bs.field_72339_c);
            FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, this.getWorld().field_73012_v.nextGaussian() * 0.01, (double)this.getWorld().field_73012_v.nextFloat() * 0.075, this.getWorld().field_73012_v.nextGaussian() * 0.01);
            fb.func_187114_a(50 + this.getWorld().field_73012_v.nextInt(25));
            fb.func_70538_b((float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f);
            fb.setAlphaF(0.0f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f, 0.0f);
            fb.setGridSize(16);
            fb.setParticles(56, 1, 1);
            fb.setScale(5.0f, 1.0f);
            fb.setLayer(0);
            fb.setSlowDown(1.0);
            fb.setGravity(0.1f);
            fb.setWind(0.001);
            fb.setRotationSpeed(this.getWorld().field_73012_v.nextFloat(), this.getWorld().field_73012_v.nextBoolean() ? -1.0f : 1.0f);
            ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
        }
    }

    public void drawFocusCloudParticle(double x, double y, double z, double mx, double my, double mz, int c) {
        Color color = new Color(c);
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, mx, my, mz);
        fb.func_187114_a(20 + this.getWorld().field_73012_v.nextInt(10));
        fb.func_70538_b((float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f);
        fb.setAlphaF(0.0f, 0.66f, 0.0f);
        fb.setGridSize(16);
        fb.setParticles(56 + this.getWorld().field_73012_v.nextInt(4), 1, 1);
        fb.setScale(5.0f + this.getWorld().field_73012_v.nextFloat(), 10.0f + this.getWorld().field_73012_v.nextFloat());
        fb.setLayer(0);
        fb.setSlowDown(0.99);
        fb.setWind(0.001);
        fb.setRotationSpeed(this.getWorld().field_73012_v.nextFloat(), this.getWorld().field_73012_v.nextBoolean() ? -0.25f : 0.25f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawWispyMotesOnBlock(BlockPos pp, int age, float grav) {
        this.drawWispyMotes((float)pp.func_177958_n() + this.getWorld().field_73012_v.nextFloat(), pp.func_177956_o(), (float)pp.func_177952_p() + this.getWorld().field_73012_v.nextFloat(), 0.0, 0.0, 0.0, age, 0.4f + this.getWorld().field_73012_v.nextFloat() * 0.6f, 0.6f + this.getWorld().field_73012_v.nextFloat() * 0.4f, 0.6f + this.getWorld().field_73012_v.nextFloat() * 0.4f, grav);
    }

    public void drawWispyMotes(double d, double e, double f, double vx, double vy, double vz, int age, float grav) {
        this.drawWispyMotes(d, e, f, vx, vy, vz, age, 0.25f + this.getWorld().field_73012_v.nextFloat() * 0.75f, 0.25f + this.getWorld().field_73012_v.nextFloat() * 0.75f, 0.25f + this.getWorld().field_73012_v.nextFloat() * 0.75f, grav);
    }

    public void drawWispyMotes(double d, double e, double f, double vx, double vy, double vz, int age, float r, float g, float b, float grav) {
        FXGeneric fb = new FXGeneric(this.getWorld(), d, e, f, vx, vy, vz);
        fb.func_187114_a((int)((float)age + (float)(age / 2) * this.getWorld().field_73012_v.nextFloat()));
        fb.func_70538_b(r, g, b);
        fb.setAlphaF(0.0f, 0.6f, 0.6f, 0.0f);
        fb.setGridSize(64);
        fb.setParticles(512, 16, 1);
        fb.setScale(1.0f, 0.5f);
        fb.setLoop(true);
        fb.setWind(0.001);
        fb.setGravity(grav);
        fb.setRandomMovementScale(0.0025f, 0.0f, 0.0025f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawBlockMistParticlesFlat(BlockPos p, int c) {
        Block bs = this.getWorld().func_180495_p(p).func_177230_c();
        Color color = new Color(c);
        for (int a = 0; a < 6; ++a) {
            double x = (float)p.func_177958_n() + this.getWorld().field_73012_v.nextFloat();
            double y = (float)p.func_177956_o() + this.getWorld().field_73012_v.nextFloat() * 0.125f;
            double z = (float)p.func_177952_p() + this.getWorld().field_73012_v.nextFloat();
            FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, (double)(this.getWorld().field_73012_v.nextFloat() - this.getWorld().field_73012_v.nextFloat()) * 0.005, 0.005, (double)(this.getWorld().field_73012_v.nextFloat() - this.getWorld().field_73012_v.nextFloat()) * 0.005);
            fb.func_187114_a(400 + this.getWorld().field_73012_v.nextInt(100));
            fb.func_70538_b((float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f);
            fb.setAlphaF(1.0f, 0.0f);
            fb.setGridSize(8);
            fb.setParticles(24, 1, 1);
            fb.setScale(2.0f, 5.0f);
            fb.setLayer(0);
            fb.setSlowDown(1.0);
            fb.setWind(0.001);
            fb.setRotationSpeed(this.getWorld().field_73012_v.nextFloat(), this.getWorld().field_73012_v.nextBoolean() ? -1.0f : 1.0f);
            ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
        }
    }

    public void crucibleBubble(float x, float y, float z, float cr, float cg, float cb) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, 0.0, 0.0, 0.0);
        fb.func_187114_a(15 + this.getWorld().field_73012_v.nextInt(10));
        fb.setScale(this.getWorld().field_73012_v.nextFloat() * 0.3f + 0.3f);
        fb.func_70538_b(cr, cg, cb);
        fb.setRandomMovementScale(0.002f, 0.002f, 0.002f);
        fb.setGravity(-0.001f);
        fb.setParticle(64);
        fb.setFinalFrames(65, 66, 66);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void crucibleBoil(BlockPos pos, TileCrucible tile, int j) {
        for (int a = 0; a < 2; ++a) {
            FXGeneric fb = new FXGeneric(this.getWorld(), (float)pos.func_177958_n() + 0.2f + this.getWorld().field_73012_v.nextFloat() * 0.6f, (float)pos.func_177956_o() + 0.1f + tile.getFluidHeight(), (float)pos.func_177952_p() + 0.2f + this.getWorld().field_73012_v.nextFloat() * 0.6f, 0.0, 0.002, 0.0);
            fb.func_187114_a((int)(7.0 + 8.0 / (Math.random() * 0.8 + 0.2)));
            fb.setScale(this.getWorld().field_73012_v.nextFloat() * 0.3f + 0.2f);
            if (tile.aspects.size() == 0) {
                fb.func_70538_b(1.0f, 1.0f, 1.0f);
            } else {
                Color color = new Color(tile.aspects.getAspects()[this.getWorld().field_73012_v.nextInt(tile.aspects.getAspects().length)].getColor());
                fb.func_70538_b((float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f);
            }
            fb.setRandomMovementScale(0.001f, 0.001f, 0.001f);
            fb.setGravity(-0.025f * (float)j);
            fb.setParticle(64);
            fb.setFinalFrames(65, 66);
            ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
        }
    }

    public void crucibleFroth(float x, float y, float z) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, 0.0, 0.0, 0.0);
        fb.func_187114_a(4 + this.getWorld().field_73012_v.nextInt(3));
        fb.setScale(this.getWorld().field_73012_v.nextFloat() * 0.2f + 0.2f);
        fb.func_70538_b(0.5f, 0.5f, 0.7f);
        fb.setRandomMovementScale(0.001f, 0.001f, 0.001f);
        fb.setGravity(0.1f);
        fb.setParticle(64);
        fb.setFinalFrames(65, 66);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void crucibleFrothDown(float x, float y, float z) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, 0.0, 0.0, 0.0);
        fb.func_187114_a(12 + this.getWorld().field_73012_v.nextInt(12));
        fb.setScale(this.getWorld().field_73012_v.nextFloat() * 0.2f + 0.4f);
        fb.func_70538_b(0.25f, 0.0f, 0.75f);
        fb.func_82338_g(0.8f);
        fb.setRandomMovementScale(0.001f, 0.001f, 0.001f);
        fb.setGravity(0.05f);
        fb.setNoClip(false);
        fb.setParticle(73);
        fb.setFinalFrames(65, 66);
        fb.setLayer(1);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawBamf(BlockPos p, boolean sound, boolean flair, EnumFacing side) {
        this.drawBamf((double)p.func_177958_n() + 0.5, (double)p.func_177956_o() + 0.5, (double)p.func_177952_p() + 0.5, sound, flair, side);
    }

    public void drawPedestalShield(BlockPos pos) {
        FXShieldRunes fb = new FXShieldRunes(this.getWorld(), (double)pos.func_177958_n() + 0.5, pos.func_177956_o() + 1, (double)pos.func_177952_p() + 0.5, null, 8, 0.0f, 90.0f);
        FMLClientHandler.instance().getClient().field_71452_i.func_78873_a((Particle)fb);
    }

    public void drawBamf(BlockPos p, float r, float g, float b, boolean sound, boolean flair, EnumFacing side) {
        this.drawBamf((double)p.func_177958_n() + 0.5, (double)p.func_177956_o() + 0.5, (double)p.func_177952_p() + 0.5, r, g, b, sound, flair, side);
    }

    public void drawBamf(BlockPos p, int color, boolean sound, boolean flair, EnumFacing side) {
        this.drawBamf((double)p.func_177958_n() + 0.5, (double)p.func_177956_o() + 0.5, (double)p.func_177952_p() + 0.5, color, sound, flair, side);
    }

    public void drawBamf(double x, double y, double z, int color, boolean sound, boolean flair, EnumFacing side) {
        Color c = new Color(color);
        float r = (float)c.getRed() / 255.0f;
        float g = (float)c.getGreen() / 255.0f;
        float b = (float)c.getBlue() / 255.0f;
        this.drawBamf(x, y, z, r, g, b, sound, flair, side);
    }

    public void drawBamf(double x, double y, double z, boolean sound, boolean flair, EnumFacing side) {
        this.drawBamf(x, y, z, 0.5f, 0.1f, 0.6f, sound, flair, side);
    }

    public void drawBamf(double x, double y, double z, float r, float g, float b, boolean sound, boolean flair, EnumFacing side) {
        double vz;
        double vy;
        double vx;
        int a;
        if (sound) {
            this.getWorld().func_184134_a(x, y, z, SoundsTC.poof, SoundCategory.BLOCKS, 0.4f, 1.0f + (float)this.getWorld().field_73012_v.nextGaussian() * 0.05f, false);
        }
        for (a = 0; a < 6 + this.getWorld().field_73012_v.nextInt(3) + 2; ++a) {
            vx = (0.05f + this.getWorld().field_73012_v.nextFloat() * 0.05f) * (float)(this.getWorld().field_73012_v.nextBoolean() ? -1 : 1);
            vy = (0.05f + this.getWorld().field_73012_v.nextFloat() * 0.05f) * (float)(this.getWorld().field_73012_v.nextBoolean() ? -1 : 1);
            vz = (0.05f + this.getWorld().field_73012_v.nextFloat() * 0.05f) * (float)(this.getWorld().field_73012_v.nextBoolean() ? -1 : 1);
            if (side != null) {
                vx += (double)((float)side.func_82601_c() * 0.1f);
                vy += (double)((float)side.func_96559_d() * 0.1f);
                vz += (double)((float)side.func_82599_e() * 0.1f);
            }
            FXGeneric fb2 = new FXGeneric(this.getWorld(), x + vx * 2.0, y + vy * 2.0, z + vz * 2.0, vx / 2.0, vy / 2.0, vz / 2.0);
            fb2.func_187114_a(20 + this.getWorld().field_73012_v.nextInt(15));
            fb2.func_70538_b(MathHelper.func_76131_a((float)(r * (1.0f + (float)this.getWorld().field_73012_v.nextGaussian() * 0.1f)), (float)0.0f, (float)1.0f), MathHelper.func_76131_a((float)(g * (1.0f + (float)this.getWorld().field_73012_v.nextGaussian() * 0.1f)), (float)0.0f, (float)1.0f), MathHelper.func_76131_a((float)(b * (1.0f + (float)this.getWorld().field_73012_v.nextGaussian() * 0.1f)), (float)0.0f, (float)1.0f));
            fb2.setAlphaF(1.0f, 0.1f);
            fb2.setGridSize(16);
            fb2.setParticles(123, 5, 1);
            fb2.setScale(3.0f, 4.0f + this.getWorld().field_73012_v.nextFloat() * 3.0f);
            fb2.setLayer(1);
            fb2.setSlowDown(0.7);
            fb2.setRotationSpeed(this.getWorld().field_73012_v.nextFloat(), this.getWorld().field_73012_v.nextBoolean() ? -1.0f : 1.0f);
            ParticleEngine.addEffect(this.getWorld(), (Particle)fb2);
        }
        if (flair) {
            for (a = 0; a < 2 + this.getWorld().field_73012_v.nextInt(3); ++a) {
                vx = (0.025f + this.getWorld().field_73012_v.nextFloat() * 0.025f) * (float)(this.getWorld().field_73012_v.nextBoolean() ? -1 : 1);
                vy = (0.025f + this.getWorld().field_73012_v.nextFloat() * 0.025f) * (float)(this.getWorld().field_73012_v.nextBoolean() ? -1 : 1);
                vz = (0.025f + this.getWorld().field_73012_v.nextFloat() * 0.025f) * (float)(this.getWorld().field_73012_v.nextBoolean() ? -1 : 1);
                this.drawWispyMotes(x + vx * 2.0, y + vy * 2.0, z + vz * 2.0, vx, vy, vz, 15 + this.getWorld().field_73012_v.nextInt(10), -0.01f);
            }
            FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, 0.0, 0.0, 0.0);
            fb.func_187114_a(10 + this.getWorld().field_73012_v.nextInt(5));
            fb.func_70538_b(1.0f, 0.9f, 1.0f);
            fb.setAlphaF(1.0f, 0.0f);
            fb.setGridSize(16);
            fb.setParticles(77, 1, 1);
            fb.setScale(10.0f + this.getWorld().field_73012_v.nextFloat() * 2.0f, 0.0f);
            fb.setLayer(0);
            fb.setRotationSpeed(this.getWorld().field_73012_v.nextFloat(), (float)this.getWorld().field_73012_v.nextGaussian());
            ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
        }
        for (a = 0; a < (flair ? 2 : 0) + this.getWorld().field_73012_v.nextInt(3); ++a) {
            this.drawCurlyWisp(x, y, z, 0.0, 0.0, 0.0, 1.0f, (0.9f + this.getWorld().field_73012_v.nextFloat() * 0.1f + r) / 2.0f, (0.1f + g) / 2.0f, (0.5f + this.getWorld().field_73012_v.nextFloat() * 0.1f + b) / 2.0f, 0.75f, side, a, 0, 0);
        }
    }

    public void drawCurlyWisp(double x, double y, double z, double vx, double vy, double vz, float scale, float r, float g, float b, float a, EnumFacing side, int seed, int layer, int delay) {
        if (this.getWorld() == null) {
            return;
        }
        vx += (double)((0.0025f + this.getWorld().field_73012_v.nextFloat() * 0.005f) * (float)(this.getWorld().field_73012_v.nextBoolean() ? -1 : 1));
        vy += (double)((0.0025f + this.getWorld().field_73012_v.nextFloat() * 0.005f) * (float)(this.getWorld().field_73012_v.nextBoolean() ? -1 : 1));
        vz += (double)((0.0025f + this.getWorld().field_73012_v.nextFloat() * 0.005f) * (float)(this.getWorld().field_73012_v.nextBoolean() ? -1 : 1));
        if (side != null) {
            vx += (double)((float)side.func_82601_c() * 0.025f);
            vy += (double)((float)side.func_96559_d() * 0.025f);
            vz += (double)((float)side.func_82599_e() * 0.025f);
        }
        FXGeneric fb2 = new FXGeneric(this.getWorld(), x + vx * 5.0, y + vy * 5.0, z + vz * 5.0, vx, vy, vz);
        if (seed > 0 && this.getWorld().field_73012_v.nextBoolean()) {
            fb2.setAngles(90.0f * (float)this.getWorld().field_73012_v.nextGaussian(), 90.0f * (float)this.getWorld().field_73012_v.nextGaussian());
        }
        fb2.func_187114_a(25 + this.getWorld().field_73012_v.nextInt(20 + 20 * seed));
        fb2.setRBGColorF(r, g, b, 0.1f, 0.0f, 0.1f);
        fb2.setAlphaF(a, 0.0f);
        fb2.setGridSize(16);
        fb2.setParticles(60 + this.getWorld().field_73012_v.nextInt(4), 1, 1);
        fb2.setScale(5.0f * scale, (10.0f + this.getWorld().field_73012_v.nextFloat() * 4.0f) * scale);
        fb2.setLayer(layer);
        fb2.setRotationSpeed(this.getWorld().field_73012_v.nextFloat(), this.getWorld().field_73012_v.nextBoolean() ? -2.0f - this.getWorld().field_73012_v.nextFloat() * 2.0f : 2.0f + this.getWorld().field_73012_v.nextFloat() * 2.0f);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb2, delay);
    }

    public void pechsCurseTick(double posX, double posY, double posZ) {
        FXGeneric fb2 = new FXGeneric(this.getWorld(), posX, posY, posZ, 0.0, 0.0, 0.0);
        fb2.setAngles(90.0f * (float)this.getWorld().field_73012_v.nextGaussian(), 90.0f * (float)this.getWorld().field_73012_v.nextGaussian());
        fb2.func_187114_a(50 + this.getWorld().field_73012_v.nextInt(50));
        fb2.setRBGColorF(0.9f, 0.1f, 0.5f, 0.1f + this.getWorld().field_73012_v.nextFloat() * 0.1f, 0.0f, 0.5f + this.getWorld().field_73012_v.nextFloat() * 0.1f);
        fb2.setAlphaF(0.75f, 0.0f);
        fb2.setGridSize(8);
        fb2.setParticles(28 + this.getWorld().field_73012_v.nextInt(4), 1, 1);
        fb2.setScale(3.0f, 5.0f + this.getWorld().field_73012_v.nextFloat() * 2.0f);
        fb2.setLayer(0);
        fb2.setRotationSpeed(this.getWorld().field_73012_v.nextFloat(), this.getWorld().field_73012_v.nextBoolean() ? -3.0f - this.getWorld().field_73012_v.nextFloat() * 3.0f : 3.0f + this.getWorld().field_73012_v.nextFloat() * 3.0f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb2);
        this.drawWispyMotes(posX, posY, posZ, 0.0, 0.0, 0.0, 10 + this.getWorld().field_73012_v.nextInt(10), -0.01f);
    }

    public void scanHighlight(BlockPos p) {
        AxisAlignedBB bb = this.getWorld().func_180495_p(p).func_185900_c((IBlockAccess)this.getWorld(), p);
        bb = bb.func_186670_a(p);
        this.scanHighlight(bb);
    }

    public void scanHighlight(Entity e) {
        AxisAlignedBB bb = e.func_174813_aQ();
        this.scanHighlight(bb);
    }

    public void scanHighlight(AxisAlignedBB bb) {
        int num = MathHelper.func_76143_f((double)(bb.func_72320_b() * 2.0));
        double ax = (bb.field_72340_a + bb.field_72336_d) / 2.0;
        double ay = (bb.field_72338_b + bb.field_72337_e) / 2.0;
        double az = (bb.field_72339_c + bb.field_72334_f) / 2.0;
        for (EnumFacing face : EnumFacing.values()) {
            double mx = 0.5 + (double)face.func_82601_c() * 0.51;
            double my = 0.5 + (double)face.func_96559_d() * 0.51;
            double mz = 0.5 + (double)face.func_82599_e() * 0.51;
            for (int a = 0; a < num * 2; ++a) {
                double x = mx;
                double y = my;
                double z = mz;
                x += this.getWorld().field_73012_v.nextGaussian() * (bb.field_72336_d - bb.field_72340_a);
                y += this.getWorld().field_73012_v.nextGaussian() * (bb.field_72337_e - bb.field_72338_b);
                z += this.getWorld().field_73012_v.nextGaussian() * (bb.field_72334_f - bb.field_72339_c);
                x = MathHelper.func_151237_a((double)x, (double)(bb.field_72340_a - ax), (double)(bb.field_72336_d - ax));
                y = MathHelper.func_151237_a((double)y, (double)(bb.field_72338_b - ay), (double)(bb.field_72337_e - ay));
                z = MathHelper.func_151237_a((double)z, (double)(bb.field_72339_c - az), (double)(bb.field_72334_f - az));
                float r = (float)MathHelper.func_76136_a((Random)this.getWorld().field_73012_v, (int)16, (int)32) / 255.0f;
                float g = (float)MathHelper.func_76136_a((Random)this.getWorld().field_73012_v, (int)132, (int)165) / 255.0f;
                float b = (float)MathHelper.func_76136_a((Random)this.getWorld().field_73012_v, (int)223, (int)239) / 255.0f;
                this.drawSimpleSparkle(this.getWorld().field_73012_v, ax + x, ay + y, az + z, 0.0, 0.0, 0.0, 0.4f + (float)this.getWorld().field_73012_v.nextGaussian() * 0.1f, r, g, b, this.getWorld().field_73012_v.nextInt(10), 1.0f, 0.0f, 4);
            }
        }
    }

    public void sparkle(float x, float y, float z, float r, float g, float b) {
        if (this.getWorld().field_73012_v.nextInt(6) < 4) {
            this.drawGenericParticles(x, y, z, 0.0, 0.0, 0.0, r, g, b, 0.9f, true, 320, 16, 1, 6 + this.getWorld().field_73012_v.nextInt(4), 0, 0.6f + this.getWorld().field_73012_v.nextFloat() * 0.2f, 0.0f, 0);
        }
    }

    public void visSparkle(int x, int y, int z, int x2, int y2, int z2, int color) {
        FXVisSparkle fb = new FXVisSparkle(this.getWorld(), (float)x + this.getWorld().field_73012_v.nextFloat(), (float)y + this.getWorld().field_73012_v.nextFloat(), (float)z + this.getWorld().field_73012_v.nextFloat(), (double)x2 + 0.4 + (double)(this.getWorld().field_73012_v.nextFloat() * 0.2f), (double)y2 + 0.4 + (double)(this.getWorld().field_73012_v.nextFloat() * 0.2f), (double)z2 + 0.4 + (double)(this.getWorld().field_73012_v.nextFloat() * 0.2f));
        Color c = new Color(color);
        fb.func_70538_b((float)c.getRed() / 255.0f, (float)c.getGreen() / 255.0f, (float)c.getBlue() / 255.0f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void voidStreak(double x, double y, double z, double x2, double y2, double z2, int seed, float scale) {
        FXVoidStream fb = new FXVoidStream(this.getWorld(), x, y, z, x2, y2, z2, seed, scale);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void splooshFX(Entity e) {
        float f = this.getWorld().field_73012_v.nextFloat() * (float)Math.PI * 2.0f;
        float f1 = this.getWorld().field_73012_v.nextFloat() * 0.5f + 0.5f;
        float f2 = MathHelper.func_76126_a((float)f) * 2.0f * 0.5f * f1;
        float f3 = MathHelper.func_76134_b((float)f) * 2.0f * 0.5f * f1;
        FXBreakingFade fx = new FXBreakingFade(this.getWorld(), e.field_70165_t + (double)f2, e.field_70163_u + (double)(this.getWorld().field_73012_v.nextFloat() * e.field_70131_O), e.field_70161_v + (double)f3, Items.field_151123_aH, 0);
        if (this.getWorld().field_73012_v.nextBoolean()) {
            fx.func_70538_b(0.6f, 0.0f, 0.3f);
            fx.func_82338_g(0.4f);
        } else {
            fx.func_70538_b(0.3f, 0.0f, 0.3f);
            fx.func_82338_g(0.6f);
        }
        fx.setParticleMaxAge((int)(66.0f / (this.getWorld().field_73012_v.nextFloat() * 0.9f + 0.1f)));
        FMLClientHandler.instance().getClient().field_71452_i.func_78873_a((Particle)fx);
    }

    public void taintsplosionFX(Entity e) {
        FXBreakingFade fx = new FXBreakingFade(this.getWorld(), e.field_70165_t, e.field_70163_u + (double)(this.getWorld().field_73012_v.nextFloat() * e.field_70131_O), e.field_70161_v, Items.field_151123_aH);
        if (this.getWorld().field_73012_v.nextBoolean()) {
            fx.func_70538_b(0.6f, 0.0f, 0.3f);
            fx.func_82338_g(0.4f);
        } else {
            fx.func_70538_b(0.3f, 0.0f, 0.3f);
            fx.func_82338_g(0.6f);
        }
        fx.setSpeed(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
        fx.boom();
        fx.setParticleMaxAge((int)(66.0f / (this.getWorld().field_73012_v.nextFloat() * 0.9f + 0.1f)));
        FMLClientHandler.instance().getClient().field_71452_i.func_78873_a((Particle)fx);
    }

    public void tentacleAriseFX(Entity e) {
        int j = 0;
        while ((float)j < 2.0f * e.field_70131_O) {
            float f = this.getWorld().field_73012_v.nextFloat() * (float)Math.PI * e.field_70131_O;
            float f1 = this.getWorld().field_73012_v.nextFloat() * 0.5f + 0.5f;
            float f2 = MathHelper.func_76126_a((float)f) * e.field_70131_O * 0.25f * f1;
            float f3 = MathHelper.func_76134_b((float)f) * e.field_70131_O * 0.25f * f1;
            FXBreakingFade fx = new FXBreakingFade(this.getWorld(), e.field_70165_t + (double)f2, e.field_70163_u, e.field_70161_v + (double)f3, Items.field_151123_aH);
            fx.func_70538_b(0.4f, 0.0f, 0.4f);
            fx.func_82338_g(0.5f);
            fx.setParticleMaxAge((int)(66.0f / (this.getWorld().field_73012_v.nextFloat() * 0.9f + 0.1f)));
            FMLClientHandler.instance().getClient().field_71452_i.func_78873_a((Particle)fx);
            if (!this.getWorld().func_175623_d(e.func_180425_c().func_177977_b())) {
                f = this.getWorld().field_73012_v.nextFloat() * (float)Math.PI * e.field_70131_O;
                f1 = this.getWorld().field_73012_v.nextFloat() * 0.5f + 0.5f;
                f2 = MathHelper.func_76126_a((float)f) * e.field_70131_O * 0.25f * f1;
                f3 = MathHelper.func_76134_b((float)f) * e.field_70131_O * 0.25f * f1;
                this.getWorld().func_175688_a(EnumParticleTypes.BLOCK_CRACK, e.field_70165_t + (double)f2, e.field_70163_u, e.field_70161_v + (double)f3, 0.0, 0.0, 0.0, new int[]{Block.func_176210_f((IBlockState)this.getWorld().func_180495_p(e.func_180425_c().func_177977_b()))});
            }
            ++j;
        }
    }

    public void slimeJumpFX(Entity e, int i) {
        float f = this.getWorld().field_73012_v.nextFloat() * (float)Math.PI * 2.0f;
        float f1 = this.getWorld().field_73012_v.nextFloat() * 0.5f + 0.5f;
        float f2 = MathHelper.func_76126_a((float)f) * (float)i * 0.5f * f1;
        float f3 = MathHelper.func_76134_b((float)f) * (float)i * 0.5f * f1;
        FXBreakingFade fx = new FXBreakingFade(this.getWorld(), e.field_70165_t + (double)f2, (e.func_174813_aQ().field_72338_b + e.func_174813_aQ().field_72337_e) / 2.0, e.field_70161_v + (double)f3, Items.field_151123_aH, 0);
        fx.func_70538_b(0.7f, 0.0f, 1.0f);
        fx.func_82338_g(0.4f);
        fx.setParticleMaxAge((int)(66.0f / (this.getWorld().field_73012_v.nextFloat() * 0.9f + 0.1f)));
        FMLClientHandler.instance().getClient().field_71452_i.func_78873_a((Particle)fx);
    }

    public void taintLandFX(Entity e) {
        float f = this.getWorld().field_73012_v.nextFloat() * (float)Math.PI * 2.0f;
        float f1 = this.getWorld().field_73012_v.nextFloat() * 0.5f + 0.5f;
        float f2 = MathHelper.func_76126_a((float)f) * 2.0f * 0.5f * f1;
        float f3 = MathHelper.func_76134_b((float)f) * 2.0f * 0.5f * f1;
        if (this.getWorld().field_72995_K) {
            FXBreakingFade fx = new FXBreakingFade(this.getWorld(), e.field_70165_t + (double)f2, (e.func_174813_aQ().field_72338_b + e.func_174813_aQ().field_72337_e) / 2.0, e.field_70161_v + (double)f3, Items.field_151123_aH);
            fx.func_70538_b(0.1f, 0.0f, 0.1f);
            fx.func_82338_g(0.4f);
            fx.setParticleMaxAge((int)(66.0f / (this.getWorld().field_73012_v.nextFloat() * 0.9f + 0.1f)));
            FMLClientHandler.instance().getClient().field_71452_i.func_78873_a((Particle)fx);
        }
    }

    public void drawInfusionParticles1(double x, double y, double z, BlockPos pos, ItemStack stack) {
        FXBoreParticles fb = new FXBoreParticles(this.getWorld(), x, y, z, (double)pos.func_177958_n() + 0.5, (double)pos.func_177956_o() - 0.5, (double)pos.func_177952_p() + 0.5, (float)this.getWorld().field_73012_v.nextGaussian() * 0.03f, (float)this.getWorld().field_73012_v.nextGaussian() * 0.03f, (float)this.getWorld().field_73012_v.nextGaussian() * 0.03f, stack).getObjectColor(pos);
        fb.func_82338_g(0.3f);
        FMLClientHandler.instance().getClient().field_71452_i.func_78873_a((Particle)fb);
    }

    public void drawInfusionParticles2(double x, double y, double z, BlockPos pos, IBlockState id, int md) {
        FXBoreParticles fb = new FXBoreParticles(this.getWorld(), x, y, z, (double)pos.func_177958_n() + 0.5, (double)pos.func_177956_o() - 0.5, (double)pos.func_177952_p() + 0.5, id, md).getObjectColor(pos);
        fb.func_82338_g(0.3f);
        FMLClientHandler.instance().getClient().field_71452_i.func_78873_a((Particle)fb);
    }

    public void drawInfusionParticles3(double x, double y, double z, int x2, int y2, int z2) {
        FXBoreSparkle fb = new FXBoreSparkle(this.getWorld(), x, y, z, (double)x2 + 0.5, (double)y2 - 0.5, (double)z2 + 0.5);
        fb.func_70538_b(0.4f + this.getWorld().field_73012_v.nextFloat() * 0.2f, 0.2f, 0.6f + this.getWorld().field_73012_v.nextFloat() * 0.3f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawInfusionParticles4(double x, double y, double z, int x2, int y2, int z2) {
        FXBoreSparkle fb = new FXBoreSparkle(this.getWorld(), x, y, z, (double)x2 + 0.5, (double)y2 - 0.5, (double)z2 + 0.5);
        fb.func_70538_b(0.2f, 0.6f + this.getWorld().field_73012_v.nextFloat() * 0.3f, 0.3f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawVentParticles(double x, double y, double z, double x2, double y2, double z2, int color) {
        FXVent fb = new FXVent(this.getWorld(), x, y, z, x2, y2, z2, color);
        fb.func_82338_g(0.4f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawVentParticles(double x, double y, double z, double x2, double y2, double z2, int color, float scale) {
        FXVent fb = new FXVent(this.getWorld(), x, y, z, x2, y2, z2, color);
        fb.func_82338_g(0.4f);
        fb.setScale(scale);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawVentParticles2(double x, double y, double z, double x2, double y2, double z2, int color, float scale) {
        FXVent2 fb = new FXVent2(this.getWorld(), x, y, z, x2, y2, z2, color);
        fb.func_82338_g(0.4f);
        fb.setScale(scale);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
        if (this.getWorld().field_73012_v.nextInt(6) < 2) {
            this.drawGenericParticles(x, y, z, x2 / 2.0, y2 / 2.0, z2 / 2.0, 1.0f, 0.7f, 0.2f, 0.9f, true, 320, 16, 1, 10 + this.getWorld().field_73012_v.nextInt(4), 0, 0.25f + this.getWorld().field_73012_v.nextFloat() * 0.1f, 0.0f, 0);
        }
    }

    public void spark(double d, double e, double f, float size, float r, float g, float b, float a) {
        FXGeneric fb = new FXGeneric(this.getWorld(), d, e, f, 0.0, 0.0, 0.0);
        fb.func_187114_a(5 + this.getWorld().field_73012_v.nextInt(5));
        fb.func_82338_g(a);
        fb.func_70538_b(r, g, b);
        fb.setGridSize(16);
        fb.setParticles(8 + this.getWorld().field_73012_v.nextInt(3) * 16, 8, 1);
        fb.setScale(size);
        fb.setFlipped(this.getWorld().field_73012_v.nextBoolean());
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void smokeSpiral(double x, double y, double z, float rad, int start, int miny, int color) {
        FXSmokeSpiral fx = new FXSmokeSpiral(this.getWorld(), x, y, z, rad, start, miny);
        Color c = new Color(color);
        fx.func_70538_b((float)c.getRed() / 255.0f, (float)c.getGreen() / 255.0f, (float)c.getBlue() / 255.0f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fx);
    }

    public void wispFXEG(double posX, double posY, double posZ, Entity target) {
        for (int a = 0; a < 2; ++a) {
            FXWispEG ef = new FXWispEG(this.getWorld(), posX, posY, posZ, target);
            ParticleEngine.addEffect(this.getWorld(), (Particle)ef);
        }
    }

    public void burst(double sx, double sy, double sz, float size) {
        FXGeneric fb = new FXGeneric(this.getWorld(), sx, sy, sz, 0.0, 0.0, 0.0);
        fb.func_187114_a(31);
        fb.setGridSize(16);
        fb.setParticles(208, 31, 1);
        fb.setScale(size);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void excavateFX(BlockPos pos, EntityLivingBase p, int progress) {
        RenderGlobal rg = Minecraft.func_71410_x().field_71438_f;
        rg.func_180441_b(p.func_145782_y(), pos, progress);
    }

    public Object beamCont(EntityLivingBase p, double tx, double ty, double tz, int type, int color, boolean reverse, float endmod, Object input, int impact) {
        FXBeamWand beamcon = null;
        Color c = new Color(color);
        if (input instanceof FXBeamWand) {
            beamcon = (FXBeamWand)((Object)input);
        }
        if (beamcon == null || !beamcon.func_187113_k()) {
            beamcon = new FXBeamWand(this.getWorld(), p, tx, ty, tz, (float)c.getRed() / 255.0f, (float)c.getGreen() / 255.0f, (float)c.getBlue() / 255.0f, 8);
            beamcon.setType(type);
            beamcon.setEndMod(endmod);
            beamcon.setReverse(reverse);
            FMLClientHandler.instance().getClient().field_71452_i.func_78873_a((Particle)beamcon);
        } else {
            beamcon.updateBeam(tx, ty, tz);
            beamcon.setEndMod(endmod);
            beamcon.impact = impact;
        }
        return beamcon;
    }

    public Object beamBore(double px, double py, double pz, double tx, double ty, double tz, int type, int color, boolean reverse, float endmod, Object input, int impact) {
        FXBeamBore beamcon = null;
        Color c = new Color(color);
        if (input instanceof FXBeamBore) {
            beamcon = (FXBeamBore)((Object)input);
        }
        if (beamcon == null || !beamcon.func_187113_k()) {
            beamcon = new FXBeamBore(this.getWorld(), px, py, pz, tx, ty, tz, (float)c.getRed() / 255.0f, (float)c.getGreen() / 255.0f, (float)c.getBlue() / 255.0f, 8);
            beamcon.setType(type);
            beamcon.setEndMod(endmod);
            beamcon.setReverse(reverse);
            FMLClientHandler.instance().getClient().field_71452_i.func_78873_a((Particle)beamcon);
        } else {
            beamcon.updateBeam(px, py, pz, tx, ty, tz);
            beamcon.setEndMod(endmod);
            beamcon.impact = impact;
        }
        return beamcon;
    }

    public void boreDigFx(int x, int y, int z, Entity e, IBlockState bi, int md, int delay) {
        float p = 50.0f;
        int a = 0;
        while ((float)a < p / (float)delay) {
            Particle fb;
            if (this.getWorld().field_73012_v.nextInt(4) == 0) {
                fb = new FXBoreSparkle(this.getWorld(), (float)x + this.getWorld().field_73012_v.nextFloat(), (float)y + this.getWorld().field_73012_v.nextFloat(), (float)z + this.getWorld().field_73012_v.nextFloat(), e);
                ParticleEngine.addEffect(this.getWorld(), fb);
            } else {
                fb = new FXBoreParticles(this.getWorld(), (float)x + this.getWorld().field_73012_v.nextFloat(), (float)y + this.getWorld().field_73012_v.nextFloat(), (float)z + this.getWorld().field_73012_v.nextFloat(), e.field_70165_t, e.field_70163_u, e.field_70161_v, bi, md);
                fb.setTarget(e);
                FMLClientHandler.instance().getClient().field_71452_i.func_78873_a(fb);
            }
            ++a;
        }
    }

    public void essentiaTrailFx(BlockPos p1, BlockPos p2, int count, int color, float scale, int ext) {
        FXEssentiaStream fb = new FXEssentiaStream(this.getWorld(), (double)p1.func_177958_n() + 0.5, (double)p1.func_177956_o() + 0.5, (double)p1.func_177952_p() + 0.5, (double)p2.func_177958_n() + 0.5, (double)p2.func_177956_o() + 0.5, (double)p2.func_177952_p() + 0.5, count, color, scale, ext, 0.0);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void boreTrailFx(BlockPos p1, Entity e, int count, int color, float scale, int ext) {
        FXBoreStream fb = new FXBoreStream(this.getWorld(), (double)p1.func_177958_n() + 0.5, (double)p1.func_177956_o() + 0.5, (double)p1.func_177952_p() + 0.5, e, count, color, scale, ext, 0.0);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void essentiaDropFx(double x, double y, double z, float r, float g, float b, float alpha) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, this.getWorld().field_73012_v.nextGaussian() * (double)0.005f, this.getWorld().field_73012_v.nextGaussian() * (double)0.005f, this.getWorld().field_73012_v.nextGaussian() * (double)0.005f);
        fb.func_187114_a(20 + this.getWorld().field_73012_v.nextInt(10));
        fb.func_70538_b(r, g, b);
        fb.func_82338_g(alpha);
        fb.setLoop(false);
        fb.setParticles(25, 1, 1);
        fb.setScale(0.4f + this.getWorld().field_73012_v.nextFloat() * 0.2f, 0.2f);
        fb.setLayer(1);
        fb.setGravity(0.01f);
        fb.setRotationSpeed(0.0f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void jarSplashFx(double x, double y, double z) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x + this.getWorld().field_73012_v.nextGaussian() * (double)0.075f, y, z + this.getWorld().field_73012_v.nextGaussian() * (double)0.075f, this.getWorld().field_73012_v.nextGaussian() * (double)0.015f, 0.075f + this.getWorld().field_73012_v.nextFloat() * 0.05f, this.getWorld().field_73012_v.nextGaussian() * (double)0.015f);
        fb.func_187114_a(20 + this.getWorld().field_73012_v.nextInt(10));
        Color c = new Color(2650102);
        fb.func_70538_b((float)c.getRed() / 255.0f, (float)c.getGreen() / 255.0f, (float)c.getBlue() / 255.0f);
        fb.func_82338_g(0.5f);
        fb.setLoop(false);
        fb.setParticles(73, 1, 1);
        fb.setScale(0.4f + this.getWorld().field_73012_v.nextFloat() * 0.3f, 0.0f);
        fb.setLayer(1);
        fb.setGravity(0.3f);
        fb.setRotationSpeed(0.0f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void waterTrailFx(BlockPos p1, BlockPos p2, int count, int color, float scale) {
        FXEssentiaStream fb = new FXEssentiaStream(this.getWorld(), (double)p1.func_177958_n() + 0.5, (double)p1.func_177956_o() + 0.66, (double)p1.func_177952_p() + 0.5, (double)p2.func_177958_n() + 0.5, (double)p2.func_177956_o() + 0.5, (double)p2.func_177952_p() + 0.5, count, color, scale, 0, 0.2);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void furnaceLavaFx(int x, int y, int z, int facingX, int facingZ) {
        float qx = facingX == 0 ? (this.getWorld().field_73012_v.nextFloat() - this.getWorld().field_73012_v.nextFloat()) * 0.5f : (float)facingX * this.getWorld().field_73012_v.nextFloat();
        float qz = facingZ == 0 ? (this.getWorld().field_73012_v.nextFloat() - this.getWorld().field_73012_v.nextFloat()) * 0.5f : (float)facingZ * this.getWorld().field_73012_v.nextFloat();
        Particle fb = new ParticleLava.Factory().func_178902_a(0, this.getWorld(), (double)((float)x + 0.5f + (this.getWorld().field_73012_v.nextFloat() - this.getWorld().field_73012_v.nextFloat()) * 0.3f + (float)facingX * 1.0f), (double)((float)y + 0.3f), (double)((float)z + 0.5f + (this.getWorld().field_73012_v.nextFloat() - this.getWorld().field_73012_v.nextFloat()) * 0.3f + (float)facingZ * 1.0f), (double)(0.15f * qx), (double)(0.2f * this.getWorld().field_73012_v.nextFloat()), (double)(0.15f * qz), new int[0]);
        FMLClientHandler.instance().getClient().field_71452_i.func_78873_a(fb);
    }

    public void blockRunes(double x, double y, double z, float r, float g, float b, int dur, float grav) {
        FXBlockRunes fb = new FXBlockRunes(this.getWorld(), x + 0.5, y + 0.5, z + 0.5, r, g, b, dur);
        fb.setGravity(grav);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void blockRunes2(double x, double y, double z, float r, float g, float b, int dur, float grav) {
        FXBlockRunes fb = new FXBlockRunes(this.getWorld(), x + 0.5, y + 0.5, z + 0.5, r, g, b, dur);
        fb.setGravity(grav);
        fb.setScale((float)(0.5 + this.getWorld().field_73012_v.nextGaussian() * (double)0.1f));
        fb.setOffsetX(0.0);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawSlash(double x, double y, double z, double x2, double y2, double z2, int dur) {
        FXPlane fb = new FXPlane(this.getWorld(), x, y, z, x2, y2, z2, dur);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void blockWard(double x, double y, double z, EnumFacing side, float f, float f1, float f2) {
        FXBlockWard fb = new FXBlockWard(this.getWorld(), x + 0.5, y + 0.5, z + 0.5, side, f, f1, f2);
        FMLClientHandler.instance().getClient().field_71452_i.func_78873_a((Particle)fb);
    }

    public Object swarmParticleFX(Entity targetedEntity, float f1, float f2, float pg) {
        FXSwarm fx = new FXSwarm(this.getWorld(), targetedEntity.field_70165_t + (double)((this.getWorld().field_73012_v.nextFloat() - this.getWorld().field_73012_v.nextFloat()) * 2.0f), targetedEntity.field_70163_u + (double)((this.getWorld().field_73012_v.nextFloat() - this.getWorld().field_73012_v.nextFloat()) * 2.0f), targetedEntity.field_70161_v + (double)((this.getWorld().field_73012_v.nextFloat() - this.getWorld().field_73012_v.nextFloat()) * 2.0f), targetedEntity, 0.8f + this.getWorld().field_73012_v.nextFloat() * 0.2f, this.getWorld().field_73012_v.nextFloat() * 0.4f, 1.0f - this.getWorld().field_73012_v.nextFloat() * 0.2f, f1, f2, pg);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fx);
        return fx;
    }

    public void bottleTaintBreak(double x, double y, double z) {
        for (int k1 = 0; k1 < 8; ++k1) {
            this.getWorld().func_175688_a(EnumParticleTypes.ITEM_CRACK, x, y, z, this.getWorld().field_73012_v.nextGaussian() * 0.15, this.getWorld().field_73012_v.nextDouble() * 0.2, this.getWorld().field_73012_v.nextGaussian() * 0.15, new int[]{Item.func_150891_b((Item)ItemsTC.bottleTaint)});
        }
        this.getWorld().func_184134_a(x, y, z, SoundEvents.field_187825_fO, SoundCategory.NEUTRAL, 1.0f, this.getWorld().field_73012_v.nextFloat() * 0.1f + 0.9f, false);
    }

    public void arcLightning(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float h) {
        if (h <= 0.0f) {
            h = 0.1f;
        }
        FXArc efa = new FXArc(this.getWorld(), x, y, z, tx, ty, tz, r, g, b, h);
        FMLClientHandler.instance().getClient().field_71452_i.func_78873_a((Particle)efa);
    }

    public void arcBolt(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float width) {
        FXBolt efa = new FXBolt(this.getWorld(), x, y, z, tx, ty, tz, r, g, b, width);
        FMLClientHandler.instance().getClient().field_71452_i.func_78873_a((Particle)efa);
    }

    public void cultistSpawn(double x, double y, double z, double a, double b, double c) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, a, b, c);
        fb.func_187114_a(10 + this.getWorld().field_73012_v.nextInt(10));
        fb.setRBGColorF(1.0f, 1.0f, 1.0f, 0.6f, 0.0f, 0.0f);
        fb.func_82338_g(0.8f);
        fb.setGridSize(16);
        fb.setParticles(160, 6, 1);
        fb.setScale(3.0f + this.getWorld().field_73012_v.nextFloat() * 2.0f);
        fb.setLayer(1);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawWispyMotesEntity(double x, double y, double z, Entity e, float r, float g, float b) {
        FXGenericP2E fb = new FXGenericP2E(this.getWorld(), x, y, z, e);
        fb.func_70538_b(r, g, b);
        fb.func_82338_g(0.6f);
        fb.setParticles(512, 16, 1);
        fb.setLoop(true);
        fb.setWind(0.001);
        fb.setRandomMovementScale(0.0025f, 0.0f, 0.0025f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawWispParticles(double x, double y, double z, double x2, double y2, double z2, int color, int a) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        fb.func_187114_a(10 + this.getWorld().field_73012_v.nextInt(5));
        Color c = new Color(color);
        fb.func_70538_b((float)c.getRed() / 255.0f, (float)c.getGreen() / 255.0f, (float)c.getBlue() / 255.0f);
        fb.func_82338_g(0.5f);
        fb.setLoop(true);
        fb.setGridSize(64);
        fb.setParticles(264, 8, 1);
        fb.setScale(1.0f + this.getWorld().field_73012_v.nextFloat() * 0.25f, 0.05f);
        fb.setWind(2.5E-4);
        fb.setRandomMovementScale(0.0025f, 0.0f, 0.0025f);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, a);
    }

    public void drawNitorCore(double x, double y, double z, double x2, double y2, double z2) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        fb.func_187114_a(10);
        fb.func_70538_b(1.0f, 1.0f, 1.0f);
        fb.func_82338_g(1.0f);
        fb.setParticles(457, 1, 1);
        fb.setScale(1.0f, 1.0f + (float)this.getWorld().field_73012_v.nextGaussian() * 0.1f, 1.0f);
        fb.setLayer(1);
        fb.setRandomMovementScale(2.0E-4f, 2.0E-4f, 2.0E-4f);
        ParticleEngine.addEffect(this.getWorld(), (Particle)fb);
    }

    public void drawNitorFlames(double x, double y, double z, double x2, double y2, double z2, int color, int a) {
        FXGeneric fb = new FXGeneric(this.getWorld(), x, y, z, x2, y2, z2);
        fb.func_187114_a(10 + this.getWorld().field_73012_v.nextInt(5));
        Color c = new Color(color);
        fb.func_70538_b((float)c.getRed() / 255.0f, (float)c.getGreen() / 255.0f, (float)c.getBlue() / 255.0f);
        fb.func_82338_g(0.66f);
        fb.setLoop(true);
        fb.setGridSize(64);
        fb.setParticles(264, 8, 1);
        fb.setScale(3.0f + this.getWorld().field_73012_v.nextFloat(), 0.05f);
        fb.setRandomMovementScale(0.0025f, 0.0f, 0.0025f);
        ParticleEngine.addEffectWithDelay(this.getWorld(), fb, a);
    }

    public static class GenPart {
        public int grid = 64;
        public int age = 0;
        public float redStart = 1.0f;
        public float greenStart = 1.0f;
        public float blueStart = 1.0f;
        public float redEnd = 1.0f;
        public float greenEnd = 1.0f;
        public float blueEnd = 1.0f;
        public float[] alpha = new float[]{1.0f};
        public float[] scale = new float[]{1.0f};
        public float rot;
        public float rotstart = 0.0f;
        public boolean loop = false;
        public int partStart = 0;
        public int partNum = 1;
        public int partInc = 1;
        public int layer = 0;
        public double slowDown = 0.98f;
        public float grav = 0.0f;
        public int delay = 0;
    }
}

