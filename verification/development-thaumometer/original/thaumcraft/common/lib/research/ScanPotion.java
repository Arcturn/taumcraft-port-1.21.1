/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.EntityLivingBase
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.potion.Potion
 *  net.minecraft.potion.PotionEffect
 *  net.minecraft.potion.PotionUtils
 */
package thaumcraft.common.lib.research;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import thaumcraft.api.research.IScanThing;
import thaumcraft.api.research.ScanningManager;

public class ScanPotion
implements IScanThing {
    Potion potion;

    public ScanPotion(Potion potion) {
        this.potion = potion;
    }

    @Override
    public boolean checkThing(EntityPlayer player, Object obj) {
        return this.getPotionEffect(player, obj) != null;
    }

    private PotionEffect getPotionEffect(EntityPlayer player, Object obj) {
        block4: {
            block3: {
                if (obj == null) {
                    return null;
                }
                if (!(obj instanceof EntityLivingBase)) break block3;
                EntityLivingBase e = (EntityLivingBase)obj;
                for (PotionEffect potioneffect : e.func_70651_bq()) {
                    if (potioneffect.func_188419_a() != this.potion) continue;
                    return potioneffect;
                }
                break block4;
            }
            ItemStack is = ScanningManager.getItemFromParms(player, obj);
            if (is == null || is.func_190926_b()) break block4;
            for (PotionEffect potioneffect : PotionUtils.func_185189_a((ItemStack)is)) {
                if (potioneffect.func_188419_a() != this.potion) continue;
                return potioneffect;
            }
        }
        return null;
    }

    @Override
    public String getResearchKey(EntityPlayer player, Object obj) {
        return "!" + this.potion.func_76393_a();
    }
}

