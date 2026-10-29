/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.item.EntityItem
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.ItemStack
 */
package thaumcraft.common.lib.research;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.research.IScanThing;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ScanningManager;

public class ScanGeneric
implements IScanThing {
    @Override
    public boolean checkThing(EntityPlayer player, Object obj) {
        if (obj == null) {
            return false;
        }
        AspectList al = null;
        if (obj instanceof Entity && !(obj instanceof EntityItem)) {
            al = AspectHelper.getEntityAspects((Entity)obj);
        } else {
            ItemStack is = ScanningManager.getItemFromParms(player, obj);
            if (is != null && !is.func_190926_b()) {
                al = AspectHelper.getObjectAspects(is);
            }
        }
        return al != null && al.size() > 0;
    }

    @Override
    public void onSuccess(EntityPlayer player, Object obj) {
        if (obj == null) {
            return;
        }
        AspectList al = null;
        if (obj instanceof Entity && !(obj instanceof EntityItem)) {
            al = AspectHelper.getEntityAspects((Entity)obj);
        } else {
            ItemStack is = ScanningManager.getItemFromParms(player, obj);
            if (is != null && !is.func_190926_b()) {
                al = AspectHelper.getObjectAspects(is);
            }
        }
        if (al != null) {
            for (ResearchCategory category : ResearchCategories.researchCategories.values()) {
                ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, category, category.applyFormula(al));
            }
        }
    }

    @Override
    public String getResearchKey(EntityPlayer player, Object obj) {
        if (obj instanceof Entity && !(obj instanceof EntityItem)) {
            String s = EntityList.func_75621_b((Entity)((Entity)obj));
            return "!" + s;
        }
        ItemStack is = ScanningManager.getItemFromParms(player, obj);
        if (is != null && !is.func_190926_b()) {
            String s = "!" + is.func_77973_b().getRegistryName();
            if (!is.func_77984_f()) {
                s = s + is.func_77952_i();
            }
            return s;
        }
        return null;
    }
}

