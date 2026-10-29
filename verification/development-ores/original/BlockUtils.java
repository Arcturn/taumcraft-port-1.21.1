/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.BlockCommandBlock
 *  net.minecraft.block.BlockLog$EnumAxis
 *  net.minecraft.block.BlockStructure
 *  net.minecraft.block.material.Material
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.enchantment.EnchantmentHelper
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.init.Enchantments
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.network.Packet
 *  net.minecraft.network.play.server.SPacketBlockBreakAnim
 *  net.minecraft.network.play.server.SPacketBlockChange
 *  net.minecraft.tileentity.TileEntity
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.EnumFacing$Axis
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.RayTraceResult
 *  net.minecraft.util.math.RayTraceResult$Type
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.util.math.Vec3i
 *  net.minecraft.world.GameType
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraftforge.common.ForgeHooks
 *  net.minecraftforge.fml.common.FMLCommonHandler
 *  net.minecraftforge.fml.relauncher.ReflectionHelper
 *  net.minecraftforge.oredict.OreDictionary
 */
package thaumcraft.common.lib.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCommandBlock;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockStructure;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketBlockBreakAnim;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.GameType;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.utils.Utils;

public class BlockUtils {
    static BlockPos lastPos = BlockPos.field_177992_a;
    static int lasty = 0;
    static int lastz = 0;
    static double lastdistance = 0.0;
    public static ArrayList<String> portableHoleBlackList = new ArrayList();

    private static boolean removeBlock(EntityPlayer player, BlockPos pos) {
        return BlockUtils.removeBlock(player, pos, false);
    }

    private static boolean removeBlock(EntityPlayer player, BlockPos pos, boolean canHarvest) {
        IBlockState iblockstate = player.field_70170_p.func_180495_p(pos);
        boolean flag = iblockstate.func_177230_c().removedByPlayer(iblockstate, player.field_70170_p, pos, player, canHarvest);
        if (flag) {
            try {
                iblockstate.func_177230_c().func_176206_d(player.field_70170_p, pos, iblockstate);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return flag;
    }

    public static boolean harvestBlockSkipCheck(World world, EntityPlayer player, BlockPos pos) {
        return BlockUtils.harvestBlock(world, player, pos, false, false, 0, true);
    }

    public static boolean harvestBlock(World world, EntityPlayer player, BlockPos pos) {
        return BlockUtils.harvestBlock(world, player, pos, false, false, 0, false);
    }

    public static boolean harvestBlock(World world, EntityPlayer p, BlockPos pos, boolean alwaysDrop, boolean silkOverride, int fortuneOverride, boolean skipEvent) {
        int exp;
        if (world.field_72995_K || !(p instanceof EntityPlayerMP)) {
            return false;
        }
        EntityPlayerMP player = (EntityPlayerMP)p;
        int n = exp = skipEvent ? 0 : ForgeHooks.onBlockBreakEvent((World)world, (GameType)player.field_71134_c.func_73081_b(), (EntityPlayerMP)player, (BlockPos)pos);
        if (exp == -1) {
            return false;
        }
        IBlockState iblockstate = world.func_180495_p(pos);
        TileEntity tileentity = world.func_175625_s(pos);
        Block block = iblockstate.func_177230_c();
        if ((block instanceof BlockCommandBlock || block instanceof BlockStructure) && !player.func_189808_dh()) {
            world.func_184138_a(pos, iblockstate, iblockstate, 3);
            return false;
        }
        world.func_180498_a(null, 2001, pos, Block.func_176210_f((IBlockState)iblockstate));
        boolean flag1 = false;
        if (player.field_71134_c.func_73083_d()) {
            flag1 = BlockUtils.removeBlock((EntityPlayer)player, pos);
            player.field_71134_c.field_73090_b.field_71135_a.func_147359_a((Packet)new SPacketBlockChange(world, pos));
        } else {
            ItemStack itemstack1 = player.func_184614_ca();
            boolean flag = alwaysDrop || iblockstate.func_177230_c().canHarvestBlock((IBlockAccess)world, pos, (EntityPlayer)player);
            flag1 = BlockUtils.removeBlock((EntityPlayer)player, pos, flag);
            if (flag1 && flag) {
                ItemStack fakeStack = itemstack1.func_77946_l();
                if (silkOverride || fortuneOverride > EnchantmentHelper.func_185284_a((Enchantment)Enchantments.field_185308_t, (EntityLivingBase)player)) {
                    int fort;
                    if (alwaysDrop || fakeStack.func_190926_b()) {
                        fakeStack = new ItemStack(ItemsTC.enchantedPlaceholder);
                    }
                    Map enchMap = EnchantmentHelper.func_82781_a((ItemStack)itemstack1);
                    if (silkOverride) {
                        enchMap.put(Enchantments.field_185306_r, 1);
                    }
                    if ((fort = Math.max(fortuneOverride, enchMap.get(Enchantments.field_185308_t) != null ? (Integer)enchMap.get(Enchantments.field_185308_t) : 0)) > 0) {
                        enchMap.put(Enchantments.field_185308_t, fort);
                    }
                    EnchantmentHelper.func_82782_a((Map)enchMap, (ItemStack)fakeStack);
                }
                iblockstate.func_177230_c().func_180657_a(world, (EntityPlayer)player, pos, iblockstate, tileentity, fakeStack);
            }
        }
        if (!player.field_71134_c.func_73083_d() && flag1 && exp > 0) {
            iblockstate.func_177230_c().func_180637_b(world, pos, exp);
        }
        return flag1;
    }

    public static void destroyBlockPartially(World world, int par1, BlockPos pos, int par5) {
        for (EntityPlayerMP entityplayermp : world.field_73010_i) {
            double d2;
            double d1;
            double d0;
            if (entityplayermp == null || entityplayermp.field_70170_p != FMLCommonHandler.instance().getMinecraftServerInstance().func_130014_f_() || entityplayermp.func_145782_y() == par1 || !((d0 = (double)pos.func_177958_n() - entityplayermp.field_70165_t) * d0 + (d1 = (double)pos.func_177956_o() - entityplayermp.field_70163_u) * d1 + (d2 = (double)pos.func_177952_p() - entityplayermp.field_70161_v) * d2 < 1024.0)) continue;
            entityplayermp.field_71135_a.func_147359_a((Packet)new SPacketBlockBreakAnim(par1, pos, par5));
        }
    }

    public static void findBlocks(World world, BlockPos pos, IBlockState block, int reach) {
        for (int xx = -reach; xx <= reach; ++xx) {
            for (int yy = reach; yy >= -reach; --yy) {
                for (int zz = -reach; zz <= reach; ++zz) {
                    double zd;
                    double yd;
                    double xd;
                    double d;
                    boolean same;
                    if (Math.abs(lastPos.func_177958_n() + xx - pos.func_177958_n()) > 24) {
                        return;
                    }
                    if (Math.abs(lastPos.func_177956_o() + yy - pos.func_177956_o()) > 48) {
                        return;
                    }
                    if (Math.abs(lastPos.func_177952_p() + zz - pos.func_177952_p()) > 24) {
                        return;
                    }
                    IBlockState bs = world.func_180495_p(lastPos.func_177982_a(xx, yy, zz));
                    boolean bl = same = bs.func_177230_c() == block.func_177230_c() && bs.func_177230_c().func_180651_a(bs) == block.func_177230_c().func_180651_a(block);
                    if (!same || !(bs.func_177230_c().func_176195_g(bs, world, lastPos.func_177982_a(xx, yy, zz)) >= 0.0f) || !((d = (xd = (double)(lastPos.func_177958_n() + xx - pos.func_177958_n())) * xd + (yd = (double)(lastPos.func_177956_o() + yy - pos.func_177956_o())) * yd + (zd = (double)(lastPos.func_177952_p() + zz - pos.func_177952_p())) * zd) > lastdistance)) continue;
                    lastdistance = d;
                    lastPos = lastPos.func_177982_a(xx, yy, zz);
                    BlockUtils.findBlocks(world, pos, block, reach);
                    return;
                }
            }
        }
    }

    public static boolean breakFurthestBlock(World world, BlockPos pos, IBlockState block, EntityPlayer player) {
        lastPos = new BlockPos((Vec3i)pos);
        lastdistance = 0.0;
        int reach = Utils.isWoodLog((IBlockAccess)world, pos) ? 2 : 1;
        BlockUtils.findBlocks(world, pos, block, reach);
        boolean worked = BlockUtils.harvestBlockSkipCheck(world, player, lastPos);
        world.markAndNotifyBlock(pos, world.func_175726_f(pos), block, block, 3);
        if (worked && Utils.isWoodLog((IBlockAccess)world, pos)) {
            world.markAndNotifyBlock(pos, world.func_175726_f(pos), block, block, 3);
            for (int xx = -3; xx <= 3; ++xx) {
                for (int yy = -3; yy <= 3; ++yy) {
                    for (int zz = -3; zz <= 3; ++zz) {
                        world.func_175684_a(lastPos.func_177982_a(xx, yy, zz), world.func_180495_p(lastPos.func_177982_a(xx, yy, zz)).func_177230_c(), 50 + world.field_73012_v.nextInt(75));
                    }
                }
            }
        }
        return worked;
    }

    public static RayTraceResult getTargetBlock(World world, Entity entity, boolean par3) {
        return BlockUtils.getTargetBlock(world, entity, par3, par3, 10.0);
    }

    public static RayTraceResult getTargetBlock(World world, Entity entity, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, double range) {
        float var4 = 1.0f;
        float var5 = entity.field_70127_C + (entity.field_70125_A - entity.field_70127_C) * var4;
        float var6 = entity.field_70126_B + (entity.field_70177_z - entity.field_70126_B) * var4;
        double var7 = entity.field_70169_q + (entity.field_70165_t - entity.field_70169_q) * (double)var4;
        double var9 = entity.field_70167_r + (entity.field_70163_u - entity.field_70167_r) * (double)var4 + (double)entity.func_70047_e();
        double var11 = entity.field_70166_s + (entity.field_70161_v - entity.field_70166_s) * (double)var4;
        Vec3d var13 = new Vec3d(var7, var9, var11);
        float var14 = MathHelper.func_76134_b((float)(-var6 * ((float)Math.PI / 180) - (float)Math.PI));
        float var15 = MathHelper.func_76126_a((float)(-var6 * ((float)Math.PI / 180) - (float)Math.PI));
        float var16 = -MathHelper.func_76134_b((float)(-var5 * ((float)Math.PI / 180)));
        float var17 = MathHelper.func_76126_a((float)(-var5 * ((float)Math.PI / 180)));
        float var18 = var15 * var16;
        float var20 = var14 * var16;
        Vec3d var23 = var13.func_72441_c((double)var18 * range, (double)var17 * range, (double)var20 * range);
        return world.func_147447_a(var13, var23, stopOnLiquid, !ignoreBlockWithoutBoundingBox, false);
    }

    public static int countExposedSides(World world, BlockPos pos) {
        int count = 0;
        for (EnumFacing dir : EnumFacing.field_82609_l) {
            if (!world.func_175623_d(pos.func_177972_a(dir))) continue;
            ++count;
        }
        return count;
    }

    public static boolean isBlockExposed(World world, BlockPos pos) {
        for (EnumFacing face : EnumFacing.values()) {
            if (world.func_180495_p(pos.func_177972_a(face)).func_185914_p()) continue;
            return true;
        }
        return false;
    }

    public static boolean isAdjacentToSolidBlock(World world, BlockPos pos) {
        for (EnumFacing face : EnumFacing.values()) {
            if (!world.isSideSolid(pos.func_177972_a(face), face.func_176734_d())) continue;
            return true;
        }
        return false;
    }

    public static boolean isBlockTouching(IBlockAccess world, BlockPos pos, IBlockState bs) {
        for (EnumFacing face : EnumFacing.values()) {
            if (world.func_180495_p(pos.func_177972_a(face)) != bs) continue;
            return true;
        }
        return false;
    }

    public static boolean isBlockTouching(IBlockAccess world, BlockPos pos, Block bs) {
        for (EnumFacing face : EnumFacing.values()) {
            if (world.func_180495_p(pos.func_177972_a(face)).func_177230_c() != bs) continue;
            return true;
        }
        return false;
    }

    public static boolean isBlockTouching(IBlockAccess world, BlockPos pos, Material mat, boolean solid) {
        for (EnumFacing face : EnumFacing.values()) {
            if (world.func_180495_p(pos.func_177972_a(face)).func_185904_a() != mat || solid && !world.func_180495_p(pos.func_177972_a(face)).isSideSolid(world, pos.func_177972_a(face), face.func_176734_d())) continue;
            return true;
        }
        return false;
    }

    public static EnumFacing getFaceBlockTouching(IBlockAccess world, BlockPos pos, Block bs) {
        for (EnumFacing face : EnumFacing.values()) {
            if (world.func_180495_p(pos.func_177972_a(face)).func_177230_c() != bs) continue;
            return face;
        }
        return null;
    }

    public static boolean isPortableHoleBlackListed(IBlockState blockstate) {
        return BlockUtils.isBlockListed(blockstate, portableHoleBlackList);
    }

    public static boolean isBlockListed(IBlockState blockstate, List<String> list) {
        String stateString = blockstate.toString();
        for (String key : list) {
            String[] splitString = key.split(";");
            if (splitString[0].contains(":")) {
                if (!((ResourceLocation)Block.field_149771_c.func_177774_c((Object)blockstate.func_177230_c())).toString().equals(splitString[0])) continue;
                if (splitString.length > 1) {
                    int matches = 0;
                    for (int a = 1; a < splitString.length; ++a) {
                        if (!stateString.contains(splitString[a])) continue;
                        ++matches;
                    }
                    if (matches != splitString.length - 1) continue;
                    return true;
                }
                return true;
            }
            ItemStack bs = new ItemStack(Item.func_150898_a((Block)blockstate.func_177230_c()), 1, blockstate.func_177230_c().func_176201_c(blockstate));
            for (ItemStack stack : OreDictionary.getOres((String)splitString[0], (boolean)false)) {
                if (!OreDictionary.itemMatches((ItemStack)stack, (ItemStack)bs, (boolean)false)) continue;
                return true;
            }
        }
        return false;
    }

    public static double distance(BlockPos b1, BlockPos b2) {
        double d3 = b1.func_177958_n() - b2.func_177958_n();
        double d4 = b1.func_177956_o() - b2.func_177956_o();
        double d5 = b1.func_177952_p() - b2.func_177952_p();
        return d3 * d3 + d4 * d4 + d5 * d5;
    }

    public static EnumFacing.Axis getBlockAxis(World world, BlockPos pos) {
        IBlockState state = world.func_180495_p(pos);
        EnumFacing.Axis ax = EnumFacing.Axis.Y;
        for (IProperty prop : state.func_177228_b().keySet()) {
            if (!prop.func_177701_a().equals("axis")) continue;
            if (state.func_177229_b(prop) instanceof BlockLog.EnumAxis) {
                ax = (BlockLog.EnumAxis)state.func_177229_b(prop) == BlockLog.EnumAxis.X ? EnumFacing.Axis.X : ((BlockLog.EnumAxis)state.func_177229_b(prop) == BlockLog.EnumAxis.Y ? EnumFacing.Axis.Y : ((BlockLog.EnumAxis)state.func_177229_b(prop) == BlockLog.EnumAxis.Z ? EnumFacing.Axis.Z : EnumFacing.Axis.Y));
                break;
            }
            if (!(state.func_177229_b(prop) instanceof EnumFacing.Axis)) continue;
            ax = (EnumFacing.Axis)state.func_177229_b(prop);
            break;
        }
        if (ax == null) {
            ax = EnumFacing.Axis.Y;
        }
        return ax;
    }

    public static boolean hasLOS(World world, BlockPos source, BlockPos target) {
        RayTraceResult mop = ThaumcraftApiHelper.rayTraceIgnoringSource(world, new Vec3d((double)source.func_177958_n() + 0.5, (double)source.func_177956_o() + 0.5, (double)source.func_177952_p() + 0.5), new Vec3d((double)target.func_177958_n() + 0.5, (double)target.func_177956_o() + 0.5, (double)target.func_177952_p() + 0.5), false, true, false);
        return mop == null || mop.field_72313_a == RayTraceResult.Type.BLOCK && mop.func_178782_a().func_177958_n() == target.func_177958_n() && mop.func_178782_a().func_177956_o() == target.func_177956_o() && mop.func_178782_a().func_177952_p() == target.func_177952_p();
    }

    public static ItemStack getSilkTouchDrop(IBlockState bs) {
        ItemStack dropped = ItemStack.field_190927_a;
        try {
            Method m = ReflectionHelper.findMethod(Block.class, (String)"getSilkTouchDrop", (String)"func_180643_i", (Class[])new Class[]{IBlockState.class});
            dropped = (ItemStack)m.invoke((Object)bs.func_177230_c(), bs);
        }
        catch (Exception e) {
            Thaumcraft.log.warn("Could not invoke net.minecraft.block.Block method getSilkTouchDrop");
        }
        return dropped;
    }

    public static class BlockPosComparator
    implements Comparator<BlockPos> {
        private BlockPos source;

        public BlockPosComparator(BlockPos source) {
            this.source = source;
        }

        @Override
        public int compare(BlockPos a, BlockPos b) {
            double db;
            if (a.equals((Object)b)) {
                return 0;
            }
            double da = this.source.func_177951_i((Vec3i)a);
            return da < (db = this.source.func_177951_i((Vec3i)b)) ? -1 : (da > db ? 1 : 0);
        }
    }
}

