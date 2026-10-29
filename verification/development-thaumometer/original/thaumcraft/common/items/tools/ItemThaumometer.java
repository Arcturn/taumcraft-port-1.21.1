/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.item.EnumRarity
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.ActionResult
 *  net.minecraft.util.EnumActionResult
 *  net.minecraft.util.EnumHand
 *  net.minecraft.util.SoundCategory
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.RayTraceResult
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.util.text.TextFormatting
 *  net.minecraft.util.text.translation.I18n
 *  net.minecraft.world.World
 *  net.minecraftforge.fml.common.network.simpleimpl.IMessage
 */
package thaumcraft.common.items.tools;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ScanningManager;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.lib.events.RenderEventHandler;
import thaumcraft.common.items.ItemTCBase;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketAuraToClient;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

public class ItemThaumometer
extends ItemTCBase {
    public ItemThaumometer() {
        super("thaumometer", new String[0]);
        this.func_77625_d(1);
    }

    public EnumRarity func_77613_e(ItemStack itemstack) {
        return EnumRarity.UNCOMMON;
    }

    public ActionResult<ItemStack> func_77659_a(World world, EntityPlayer p, EnumHand hand) {
        if (world.field_72995_K) {
            this.drawFX(world, p);
            p.field_70170_p.func_184134_a(p.field_70165_t, p.field_70163_u, p.field_70161_v, SoundsTC.scan, SoundCategory.PLAYERS, 0.5f, 1.0f, false);
        } else {
            this.doScan(world, p);
        }
        return new ActionResult(EnumActionResult.SUCCESS, (Object)p.func_184586_b(hand));
    }

    public void func_77663_a(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
        boolean held;
        boolean bl = held = isSelected || itemSlot == 0;
        if (held && !world.field_72995_K && entity.field_70173_aa % 20 == 0 && entity instanceof EntityPlayerMP) {
            this.updateAura(stack, world, (EntityPlayerMP)entity);
        }
        if (held && world.field_72995_K && entity.field_70173_aa % 5 == 0 && entity instanceof EntityPlayer) {
            Entity target = EntityUtils.getPointedEntity(world, entity, 1.0, 16.0, 5.0f, true);
            if (target != null && ScanningManager.isThingStillScannable((EntityPlayer)entity, target)) {
                FXDispatcher.INSTANCE.scanHighlight(target);
            }
            RenderEventHandler.thaumTarget = target;
            RayTraceResult mop = this.getRayTraceResultFromPlayerWild(world, (EntityPlayer)entity, true);
            if (mop != null && mop.func_178782_a() != null && ScanningManager.isThingStillScannable((EntityPlayer)entity, mop.func_178782_a())) {
                FXDispatcher.INSTANCE.scanHighlight(mop.func_178782_a());
            }
        }
    }

    protected RayTraceResult getRayTraceResultFromPlayerWild(World worldIn, EntityPlayer playerIn, boolean useLiquids) {
        float f = playerIn.field_70127_C + (playerIn.field_70125_A - playerIn.field_70127_C) + (float)worldIn.field_73012_v.nextInt(25) - (float)worldIn.field_73012_v.nextInt(25);
        float f1 = playerIn.field_70126_B + (playerIn.field_70177_z - playerIn.field_70126_B) + (float)worldIn.field_73012_v.nextInt(25) - (float)worldIn.field_73012_v.nextInt(25);
        double d0 = playerIn.field_70169_q + (playerIn.field_70165_t - playerIn.field_70169_q);
        double d1 = playerIn.field_70167_r + (playerIn.field_70163_u - playerIn.field_70167_r) + (double)playerIn.func_70047_e();
        double d2 = playerIn.field_70166_s + (playerIn.field_70161_v - playerIn.field_70166_s);
        Vec3d vec3 = new Vec3d(d0, d1, d2);
        float f2 = MathHelper.func_76134_b((float)(-f1 * ((float)Math.PI / 180) - (float)Math.PI));
        float f3 = MathHelper.func_76126_a((float)(-f1 * ((float)Math.PI / 180) - (float)Math.PI));
        float f4 = -MathHelper.func_76134_b((float)(-f * ((float)Math.PI / 180)));
        float f5 = MathHelper.func_76126_a((float)(-f * ((float)Math.PI / 180)));
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double d3 = 16.0;
        Vec3d vec31 = vec3.func_72441_c((double)f6 * d3, (double)f5 * d3, (double)f7 * d3);
        return worldIn.func_147447_a(vec3, vec31, useLiquids, !useLiquids, false);
    }

    private void updateAura(ItemStack stack, World world, EntityPlayerMP player) {
        AuraChunk ac = AuraHandler.getAuraChunk(world.field_73011_w.getDimension(), player.func_180425_c().func_177958_n() >> 4, player.func_180425_c().func_177952_p() >> 4);
        if (ac != null) {
            if ((ac.getFlux() > ac.getVis() || ac.getFlux() > (float)(ac.getBase() / 3)) && !ThaumcraftCapabilities.knowsResearch((EntityPlayer)player, "FLUX")) {
                ResearchManager.startResearchWithPopup((EntityPlayer)player, "FLUX");
                player.func_146105_b((ITextComponent)new TextComponentString(TextFormatting.DARK_PURPLE + I18n.func_74838_a((String)"research.FLUX.warn")), true);
            }
            PacketHandler.INSTANCE.sendTo((IMessage)new PacketAuraToClient(ac), player);
        }
    }

    private void drawFX(World worldIn, EntityPlayer playerIn) {
        block3: {
            block2: {
                Entity target = EntityUtils.getPointedEntity(worldIn, (Entity)playerIn, 1.0, 9.0, 0.0f, true);
                if (target == null) break block2;
                for (int a = 0; a < 10; ++a) {
                    FXDispatcher.INSTANCE.blockRunes(target.field_70165_t - 0.5, target.field_70163_u + (double)(target.func_70047_e() / 2.0f), target.field_70161_v - 0.5, 0.3f + worldIn.field_73012_v.nextFloat() * 0.7f, 0.0f, 0.3f + worldIn.field_73012_v.nextFloat() * 0.7f, (int)(target.field_70131_O * 15.0f), 0.03f);
                }
                break block3;
            }
            RayTraceResult mop = this.func_77621_a(worldIn, playerIn, true);
            if (mop == null || mop.func_178782_a() == null) break block3;
            for (int a = 0; a < 10; ++a) {
                FXDispatcher.INSTANCE.blockRunes(mop.func_178782_a().func_177958_n(), (double)mop.func_178782_a().func_177956_o() + 0.25, mop.func_178782_a().func_177952_p(), 0.3f + worldIn.field_73012_v.nextFloat() * 0.7f, 0.0f, 0.3f + worldIn.field_73012_v.nextFloat() * 0.7f, 15, 0.03f);
            }
        }
    }

    public void doScan(World worldIn, EntityPlayer playerIn) {
        if (!worldIn.field_72995_K) {
            Entity target = EntityUtils.getPointedEntity(worldIn, (Entity)playerIn, 1.0, 9.0, 0.0f, true);
            if (target != null) {
                ScanningManager.scanTheThing(playerIn, target);
            } else {
                RayTraceResult mop = this.func_77621_a(worldIn, playerIn, true);
                if (mop != null && mop.func_178782_a() != null) {
                    ScanningManager.scanTheThing(playerIn, mop.func_178782_a());
                } else {
                    ScanningManager.scanTheThing(playerIn, null);
                }
            }
        }
    }
}

