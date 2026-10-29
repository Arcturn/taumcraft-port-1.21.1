/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.client.Minecraft
 *  net.minecraft.init.Blocks
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Vec3i
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.common.network.simpleimpl.IMessage
 *  net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
 *  net.minecraftforge.fml.common.network.simpleimpl.MessageContext
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 *  net.minecraftforge.oredict.OreDictionary
 */
package thaumcraft.common.lib.network.fx;

import io.netty.buffer.ByteBuf;
import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.fx.particles.FXGeneric;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.lib.utils.Utils;

public class PacketFXScanSource
implements IMessage,
IMessageHandler<PacketFXScanSource, IMessage> {
    private long loc;
    private int size;
    final int C_QUARTZ = 0xE5DED5;
    final int C_IRON = 14200723;
    final int C_LAPIS = 1328572;
    final int C_GOLD = 16576075;
    final int C_DIAMOND = 6155509;
    final int C_EMERALD = 1564002;
    final int C_REDSTONE = 0xFF0000;
    final int C_COAL = 0x101010;
    final int C_SILVER = 14342653;
    final int C_TIN = 0xEFEFFB;
    final int C_COPPER = 16620629;
    final int C_AMBER = 16626469;
    final int C_CINNABAR = 10159368;

    public PacketFXScanSource() {
    }

    public PacketFXScanSource(BlockPos pos, int size) {
        this.loc = pos.func_177986_g();
        this.size = size;
    }

    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(this.loc);
        buffer.writeByte(this.size);
    }

    public void fromBytes(ByteBuf buffer) {
        this.loc = buffer.readLong();
        this.size = buffer.readByte();
    }

    @SideOnly(value=Side.CLIENT)
    public IMessage onMessage(final PacketFXScanSource message, MessageContext ctx) {
        Minecraft.func_71410_x().func_152344_a(new Runnable(){

            @Override
            public void run() {
                PacketFXScanSource.this.startScan(Minecraft.func_71410_x().field_71439_g.field_70170_p, BlockPos.func_177969_a((long)message.loc), message.size);
            }
        });
        return null;
    }

    @SideOnly(value=Side.CLIENT)
    public void startScan(World world, BlockPos pos, int r) {
        int range = 4 + r * 4;
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>();
        for (int xx = -range; xx <= range; ++xx) {
            for (int yy = -range; yy <= range; ++yy) {
                for (int zz = -range; zz <= range; ++zz) {
                    BlockPos p = pos.func_177982_a(xx, yy, zz);
                    if (!Utils.isOreBlock(world, p)) continue;
                    positions.add(p);
                }
            }
        }
        while (!positions.isEmpty()) {
            BlockPos start = (BlockPos)positions.get(0);
            ArrayList<BlockPos> coll = new ArrayList<BlockPos>();
            coll.add(start);
            positions.remove(0);
            this.calcGroup(world, start, coll, positions);
            if (coll.isEmpty()) continue;
            int c = this.getOreColor(world, start);
            double x = 0.0;
            double y = 0.0;
            double z = 0.0;
            for (BlockPos p : coll) {
                x += (double)p.func_177958_n() + 0.5;
                y += (double)p.func_177956_o() + 0.5;
                z += (double)p.func_177952_p() + 0.5;
            }
            double dis = Math.sqrt(pos.func_177957_d(x /= (double)coll.size(), y /= (double)coll.size(), z /= (double)coll.size()));
            FXGeneric fb = new FXGeneric(world, x, y, z, 0.0, 0.0, 0.0);
            fb.func_187114_a(44);
            Color cc = new Color(c);
            fb.func_70538_b((float)cc.getRed() / 255.0f, (float)cc.getGreen() / 255.0f, (float)cc.getBlue() / 255.0f);
            float q = ((float)cc.getRed() / 255.0f + (float)cc.getGreen() / 255.0f + (float)cc.getBlue() / 255.0f) / 3.0f;
            fb.setAlphaF(0.0f, 1.0f, 0.8f, 0.0f);
            fb.setParticles(240, 15, 1);
            fb.setGridSize(16);
            fb.setLoop(true);
            fb.setScale(9.0f);
            fb.setLayer(q < 0.25f ? 3 : 2);
            fb.setRotationSpeed(0.0f);
            ParticleEngine.addEffectWithDelay(world, fb, (int)(dis * 3.0));
        }
    }

    private void calcGroup(World world, BlockPos start, ArrayList<BlockPos> coll, ArrayList<BlockPos> positions) {
        IBlockState bs = world.func_180495_p(start);
        block0: for (int x = -1; x <= 1; ++x) {
            for (int y = -1; y <= 1; ++y) {
                for (int z = -1; z <= 1; ++z) {
                    BlockPos t = new BlockPos((Vec3i)start).func_177982_a(x, y, z);
                    IBlockState ts = world.func_180495_p(t);
                    if (!ts.equals(bs) || !positions.contains(t)) continue;
                    positions.remove(t);
                    coll.add(t);
                    if (positions.isEmpty()) break block0;
                    this.calcGroup(world, t, coll, positions);
                }
            }
        }
    }

    private int getOreColor(World world, BlockPos pos) {
        IBlockState bi = world.func_180495_p(pos);
        if (bi.func_177230_c() != Blocks.field_150350_a && bi.func_177230_c() != Blocks.field_150357_h) {
            ItemStack is = BlockUtils.getSilkTouchDrop(bi);
            if (is == null || is.func_190926_b()) {
                int md = bi.func_177230_c().func_176201_c(bi);
                is = new ItemStack(bi.func_177230_c(), 1, md);
            }
            if (is == null || is.func_190926_b() || is.func_77973_b() == null) {
                return 0xC0C0C0;
            }
            int[] od = OreDictionary.getOreIDs((ItemStack)is);
            if (od != null && od.length > 0) {
                for (int id : od) {
                    if (OreDictionary.getOreName((int)id) == null) continue;
                    if (OreDictionary.getOreName((int)id).toUpperCase().contains("IRON")) {
                        return 14200723;
                    }
                    if (OreDictionary.getOreName((int)id).toUpperCase().contains("COAL")) {
                        return 0x101010;
                    }
                    if (OreDictionary.getOreName((int)id).toUpperCase().contains("REDSTONE")) {
                        return 0xFF0000;
                    }
                    if (OreDictionary.getOreName((int)id).toUpperCase().contains("GOLD")) {
                        return 16576075;
                    }
                    if (OreDictionary.getOreName((int)id).toUpperCase().contains("LAPIS")) {
                        return 1328572;
                    }
                    if (OreDictionary.getOreName((int)id).toUpperCase().contains("DIAMOND")) {
                        return 6155509;
                    }
                    if (OreDictionary.getOreName((int)id).toUpperCase().contains("EMERALD")) {
                        return 1564002;
                    }
                    if (OreDictionary.getOreName((int)id).toUpperCase().contains("QUARTZ")) {
                        return 0xE5DED5;
                    }
                    if (OreDictionary.getOreName((int)id).toUpperCase().contains("SILVER")) {
                        return 14342653;
                    }
                    if (OreDictionary.getOreName((int)id).toUpperCase().contains("TIN")) {
                        return 0xEFEFFB;
                    }
                    if (OreDictionary.getOreName((int)id).toUpperCase().contains("COPPER")) {
                        return 16620629;
                    }
                    if (OreDictionary.getOreName((int)id).toUpperCase().contains("AMBER")) {
                        return 16626469;
                    }
                    if (!OreDictionary.getOreName((int)id).toUpperCase().contains("CINNABAR")) continue;
                    return 10159368;
                }
            }
        }
        return 0xC0C0C0;
    }
}

