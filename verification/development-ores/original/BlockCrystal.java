/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.Block
 *  net.minecraft.block.SoundType
 *  net.minecraft.block.material.Material
 *  net.minecraft.block.properties.IProperty
 *  net.minecraft.block.properties.PropertyBool
 *  net.minecraft.block.properties.PropertyInteger
 *  net.minecraft.block.state.BlockFaceShape
 *  net.minecraft.block.state.BlockStateContainer
 *  net.minecraft.block.state.IBlockState
 *  net.minecraft.creativetab.CreativeTabs
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.EnumBlockRenderType
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.NonNullList
 *  net.minecraft.util.math.AxisAlignedBB
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.world.IBlockAccess
 *  net.minecraft.world.World
 *  net.minecraftforge.common.property.ExtendedBlockState
 *  net.minecraftforge.common.property.IExtendedBlockState
 *  net.minecraftforge.common.property.IUnlistedProperty
 *  net.minecraftforge.common.property.Properties$PropertyAdapter
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package thaumcraft.common.blocks.world.ore;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.property.Properties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.world.aura.AuraHandler;

public class BlockCrystal
extends Block {
    public static final PropertyInteger SIZE = PropertyInteger.func_177719_a((String)"size", (int)0, (int)3);
    public static final PropertyInteger GENERATION = PropertyInteger.func_177719_a((String)"gen", (int)1, (int)4);
    public static final IUnlistedProperty<Boolean> NORTH = new Properties.PropertyAdapter((IProperty)PropertyBool.func_177716_a((String)"north"));
    public static final IUnlistedProperty<Boolean> EAST = new Properties.PropertyAdapter((IProperty)PropertyBool.func_177716_a((String)"east"));
    public static final IUnlistedProperty<Boolean> SOUTH = new Properties.PropertyAdapter((IProperty)PropertyBool.func_177716_a((String)"south"));
    public static final IUnlistedProperty<Boolean> WEST = new Properties.PropertyAdapter((IProperty)PropertyBool.func_177716_a((String)"west"));
    public static final IUnlistedProperty<Boolean> UP = new Properties.PropertyAdapter((IProperty)PropertyBool.func_177716_a((String)"up"));
    public static final IUnlistedProperty<Boolean> DOWN = new Properties.PropertyAdapter((IProperty)PropertyBool.func_177716_a((String)"down"));
    public Aspect aspect;

    public BlockCrystal(String name, Aspect aspect) {
        super(Material.field_151592_s);
        this.func_149663_c(name);
        this.setRegistryName("thaumcraft", name);
        this.aspect = aspect;
        this.func_149711_c(0.25f);
        this.func_149672_a(SoundsTC.CRYSTAL);
        this.func_149675_a(true);
        this.func_149647_a(ConfigItems.TABTC);
        this.func_180632_j(this.field_176227_L.func_177621_b().func_177226_a((IProperty)SIZE, (Comparable)Integer.valueOf(0)).func_177226_a((IProperty)GENERATION, (Comparable)Integer.valueOf(1)));
    }

    public SoundType func_185467_w() {
        return SoundsTC.CRYSTAL;
    }

    public BlockFaceShape func_193383_a(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    public EnumBlockRenderType func_149645_b(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    public Item func_180660_a(IBlockState state, Random rand, int fortune) {
        return Item.func_150899_d((int)0);
    }

    protected boolean func_149700_E() {
        return false;
    }

    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        int count = this.getGrowth(state) + 1;
        for (int i = 0; i < count; ++i) {
            ret.add(ThaumcraftApiHelper.makeCrystal(this.aspect));
        }
        return ret;
    }

    public void func_180650_b(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (!worldIn.field_72995_K && rand.nextInt(3 + this.getGeneration(state)) == 0) {
            int threshold = 10;
            int growth = this.getGrowth(state);
            int generation = this.getGeneration(state);
            if (this.aspect != Aspect.FLUX) {
                if (AuraHelper.getVis(worldIn, pos) <= (float)threshold) {
                    if (growth > 0) {
                        worldIn.func_175656_a(pos, state.func_177226_a((IProperty)SIZE, (Comparable)Integer.valueOf(growth - 1)));
                        AuraHelper.addVis(worldIn, pos, threshold);
                    } else if (BlockUtils.isBlockTouching((IBlockAccess)worldIn, pos, this)) {
                        worldIn.func_175698_g(pos);
                        AuraHandler.addVis(worldIn, pos, threshold);
                    }
                } else if (AuraHelper.getVis(worldIn, pos) > (float)(AuraHandler.getAuraBase(worldIn, pos) + threshold)) {
                    BlockPos p2;
                    if (growth < 3 && (long)growth < (long)(5 - generation) + pos.func_177986_g() % 3L) {
                        if (AuraHelper.drainVis(worldIn, pos, threshold, false) > 0.0f) {
                            worldIn.func_175656_a(pos, state.func_177226_a((IProperty)SIZE, (Comparable)Integer.valueOf(growth + 1)));
                        }
                    } else if (generation < 4 && (p2 = BlockCrystal.spreadCrystal(worldIn, pos)) != null && AuraHelper.drainVis(worldIn, pos, threshold, false) > 0.0f) {
                        if (rand.nextInt(6) == 0) {
                            --generation;
                        }
                        worldIn.func_175656_a(p2, this.func_176223_P().func_177226_a((IProperty)GENERATION, (Comparable)Integer.valueOf(generation + 1)));
                    }
                }
            } else if (AuraHelper.getFlux(worldIn, pos) <= (float)threshold) {
                if (growth > 0) {
                    worldIn.func_175656_a(pos, state.func_177226_a((IProperty)SIZE, (Comparable)Integer.valueOf(growth - 1)));
                    AuraHelper.polluteAura(worldIn, pos, threshold, false);
                } else if (BlockUtils.isBlockTouching((IBlockAccess)worldIn, pos, this)) {
                    worldIn.func_175698_g(pos);
                    AuraHelper.polluteAura(worldIn, pos, threshold, false);
                }
            } else if (AuraHelper.getFlux(worldIn, pos) > (float)(AuraHandler.getAuraBase(worldIn, pos) + threshold)) {
                BlockPos p2;
                if (growth < 3 && (long)growth < (long)(5 - generation) + pos.func_177986_g() % 3L) {
                    if (AuraHelper.drainFlux(worldIn, pos, threshold, false) > 0.0f) {
                        worldIn.func_175656_a(pos, state.func_177226_a((IProperty)SIZE, (Comparable)Integer.valueOf(growth + 1)));
                    }
                } else if (generation < 4 && (p2 = BlockCrystal.spreadCrystal(worldIn, pos)) != null && AuraHelper.drainFlux(worldIn, pos, threshold, false) > 0.0f) {
                    if (rand.nextInt(6) == 0) {
                        --generation;
                    }
                    worldIn.func_175656_a(p2, this.func_176223_P().func_177226_a((IProperty)GENERATION, (Comparable)Integer.valueOf(generation + 1)));
                }
            }
        }
    }

    public static BlockPos spreadCrystal(World world, BlockPos pos) {
        int zz;
        int yy;
        int xx = pos.func_177958_n() + world.field_73012_v.nextInt(3) - 1;
        BlockPos t = new BlockPos(xx, yy = pos.func_177956_o() + world.field_73012_v.nextInt(3) - 1, zz = pos.func_177952_p() + world.field_73012_v.nextInt(3) - 1);
        if (t.equals((Object)pos)) {
            return null;
        }
        IBlockState bs = world.func_180495_p(t);
        Material bm = bs.func_185904_a();
        if (!bm.func_76224_d() && (world.func_175623_d(t) || bs.func_177230_c().func_176200_f((IBlockAccess)world, t)) && world.field_73012_v.nextInt(16) == 0 && BlockUtils.isBlockTouching((IBlockAccess)world, t, Material.field_151576_e, true)) {
            return t;
        }
        return null;
    }

    public void func_189540_a(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!BlockUtils.isBlockTouching((IBlockAccess)worldIn, pos, Material.field_151576_e, true)) {
            this.func_176226_b(worldIn, pos, state, 0);
            worldIn.func_175698_g(pos);
        }
    }

    public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing o) {
        return false;
    }

    private boolean drawAt(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        IBlockState fbs = worldIn.func_180495_p(pos);
        return fbs.func_185904_a() == Material.field_151576_e && fbs.func_177230_c().isSideSolid(fbs, worldIn, pos, side.func_176734_d());
    }

    public AxisAlignedBB func_185496_a(IBlockState bs, IBlockAccess iblockaccess, BlockPos pos) {
        IBlockState state = this.getExtendedState(bs, iblockaccess, pos);
        if (state instanceof IExtendedBlockState) {
            IExtendedBlockState es = (IExtendedBlockState)state;
            int c = 0;
            if (((Boolean)es.getValue(UP)).booleanValue()) {
                ++c;
            }
            if (((Boolean)es.getValue(DOWN)).booleanValue()) {
                ++c;
            }
            if (((Boolean)es.getValue(EAST)).booleanValue()) {
                ++c;
            }
            if (((Boolean)es.getValue(WEST)).booleanValue()) {
                ++c;
            }
            if (((Boolean)es.getValue(SOUTH)).booleanValue()) {
                ++c;
            }
            if (((Boolean)es.getValue(NORTH)).booleanValue()) {
                ++c;
            }
            if (c > 1) {
                return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
            }
            if (((Boolean)es.getValue(UP)).booleanValue()) {
                return new AxisAlignedBB(0.0, 0.5, 0.0, 1.0, 1.0, 1.0);
            }
            if (((Boolean)es.getValue(DOWN)).booleanValue()) {
                return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);
            }
            if (((Boolean)es.getValue(EAST)).booleanValue()) {
                return new AxisAlignedBB(0.5, 0.0, 0.0, 1.0, 1.0, 1.0);
            }
            if (((Boolean)es.getValue(WEST)).booleanValue()) {
                return new AxisAlignedBB(0.0, 0.0, 0.0, 0.5, 1.0, 1.0);
            }
            if (((Boolean)es.getValue(SOUTH)).booleanValue()) {
                return new AxisAlignedBB(0.0, 0.0, 0.5, 1.0, 1.0, 1.0);
            }
            if (((Boolean)es.getValue(NORTH)).booleanValue()) {
                return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 0.5);
            }
        }
        return new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    }

    public AxisAlignedBB func_180646_a(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return null;
    }

    public boolean func_149662_c(IBlockState state) {
        return false;
    }

    public boolean func_149686_d(IBlockState state) {
        return false;
    }

    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 1;
    }

    public int func_185484_c(IBlockState state, IBlockAccess source, BlockPos pos) {
        int i = source.func_175626_b(pos, state.getLightValue(source, pos));
        int j = 180;
        int k = i & 0xFF;
        int l = j & 0xFF;
        int i1 = i >> 16 & 0xFF;
        int j1 = j >> 16 & 0xFF;
        return (k > l ? k : l) | (i1 > j1 ? i1 : j1) << 16;
    }

    protected BlockStateContainer func_180661_e() {
        IProperty[] listedProperties = new IProperty[]{SIZE, GENERATION};
        IUnlistedProperty[] unlistedProperties = new IUnlistedProperty[]{UP, DOWN, NORTH, EAST, WEST, SOUTH};
        return new ExtendedBlockState((Block)this, listedProperties, unlistedProperties);
    }

    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (state instanceof IExtendedBlockState) {
            IExtendedBlockState retval = (IExtendedBlockState)state;
            return retval.withProperty(UP, (Object)this.drawAt(world, pos.func_177984_a(), EnumFacing.UP)).withProperty(DOWN, (Object)this.drawAt(world, pos.func_177977_b(), EnumFacing.DOWN)).withProperty(NORTH, (Object)this.drawAt(world, pos.func_177978_c(), EnumFacing.NORTH)).withProperty(EAST, (Object)this.drawAt(world, pos.func_177974_f(), EnumFacing.EAST)).withProperty(SOUTH, (Object)this.drawAt(world, pos.func_177968_d(), EnumFacing.SOUTH)).withProperty(WEST, (Object)this.drawAt(world, pos.func_177976_e(), EnumFacing.WEST));
        }
        return state;
    }

    public IBlockState func_176221_a(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return state;
    }

    public IBlockState func_176203_a(int meta) {
        return this.func_176223_P().func_177226_a((IProperty)SIZE, (Comparable)Integer.valueOf(meta & 3)).func_177226_a((IProperty)GENERATION, (Comparable)Integer.valueOf(1 + (meta >> 2 & 3)));
    }

    public int func_176201_c(IBlockState state) {
        int i = 0;
        i |= ((Integer)state.func_177229_b((IProperty)SIZE)).intValue();
        return i |= (Integer)state.func_177229_b((IProperty)GENERATION) - 1 << 2;
    }

    public int getGrowth(IBlockState state) {
        return this.func_176201_c(state) & 3;
    }

    public int getGeneration(IBlockState state) {
        return 1 + (this.func_176201_c(state) >> 2);
    }

    @SideOnly(value=Side.CLIENT)
    public void func_149666_a(CreativeTabs tab, NonNullList<ItemStack> list) {
        list.add((Object)new ItemStack((Block)this, 1, 0));
    }

    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return false;
    }

    public boolean func_176196_c(World worldIn, BlockPos pos) {
        return BlockUtils.isBlockTouching((IBlockAccess)worldIn, pos, Material.field_151576_e, true) && super.func_176196_c(worldIn, pos);
    }
}

