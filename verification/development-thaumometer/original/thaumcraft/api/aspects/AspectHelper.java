/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityList
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTTagCompound
 */
package thaumcraft.api.aspects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.internal.CommonInternals;

public class AspectHelper {
    public static AspectList cullTags(AspectList temp) {
        return AspectHelper.cullTags(temp, 7);
    }

    public static AspectList cullTags(AspectList temp, int cap) {
        AspectList temp2 = new AspectList();
        for (Aspect tag : temp.getAspects()) {
            if (tag == null) continue;
            temp2.add(tag, temp.getAmount(tag));
        }
        while (temp2 != null && temp2.size() > cap) {
            Aspect lowest = null;
            float low = 32767.0f;
            for (Aspect tag : temp2.getAspects()) {
                if (tag == null) continue;
                float ta = temp2.getAmount(tag);
                if (tag.isPrimal()) {
                    ta *= 0.9f;
                } else {
                    if (!tag.getComponents()[0].isPrimal()) {
                        ta *= 1.1f;
                        if (!tag.getComponents()[0].getComponents()[0].isPrimal()) {
                            ta *= 1.05f;
                        }
                        if (!tag.getComponents()[0].getComponents()[1].isPrimal()) {
                            ta *= 1.05f;
                        }
                    }
                    if (!tag.getComponents()[1].isPrimal()) {
                        ta *= 1.1f;
                        if (!tag.getComponents()[1].getComponents()[0].isPrimal()) {
                            ta *= 1.05f;
                        }
                        if (!tag.getComponents()[1].getComponents()[1].isPrimal()) {
                            ta *= 1.05f;
                        }
                    }
                }
                if (!(ta < low)) continue;
                low = ta;
                lowest = tag;
            }
            temp2.aspects.remove(lowest);
        }
        return temp2;
    }

    public static AspectList getObjectAspects(ItemStack is) {
        return ThaumcraftApi.internalMethods.getObjectAspects(is);
    }

    public static AspectList generateTags(ItemStack is) {
        return ThaumcraftApi.internalMethods.generateTags(is);
    }

    public static AspectList getEntityAspects(Entity entity) {
        AspectList tags = null;
        String entityString = EntityList.func_75621_b((Entity)entity);
        if (entity instanceof EntityPlayer) {
            tags = new AspectList();
            tags.add(Aspect.MAN, 4);
            Random rand = new Random(((EntityPlayer)entity).func_70005_c_().hashCode());
            Aspect[] posa = Aspect.aspects.values().toArray(new Aspect[0]);
            tags.add(posa[rand.nextInt(posa.length)], 15);
            tags.add(posa[rand.nextInt(posa.length)], 15);
            tags.add(posa[rand.nextInt(posa.length)], 15);
        } else {
            block0: for (ThaumcraftApi.EntityTags et : CommonInternals.scanEntities) {
                if (!et.entityName.equals(entityString)) continue;
                if (et.nbts == null || et.nbts.length == 0) {
                    tags = et.aspects;
                    continue;
                }
                NBTTagCompound tc = new NBTTagCompound();
                entity.func_189511_e(tc);
                for (ThaumcraftApi.EntityTagsNBT nbt : et.nbts) {
                    if (!tc.func_74764_b(nbt.name) || !ThaumcraftApiHelper.getNBTDataFromId(tc, tc.func_150299_b(nbt.name), nbt.name).equals(nbt.value)) continue block0;
                }
                tags = et.aspects;
            }
        }
        return tags;
    }

    public static Aspect getCombinationResult(Aspect aspect1, Aspect aspect2) {
        Collection<Aspect> aspects = Aspect.aspects.values();
        for (Aspect aspect : aspects) {
            if (aspect.getComponents() == null || (aspect.getComponents()[0] != aspect1 || aspect.getComponents()[1] != aspect2) && (aspect.getComponents()[0] != aspect2 || aspect.getComponents()[1] != aspect1)) continue;
            return aspect;
        }
        return null;
    }

    public static Aspect getRandomPrimal(Random rand, Aspect aspect) {
        ArrayList<Aspect> list = new ArrayList<Aspect>();
        if (aspect != null) {
            AspectList temp = new AspectList();
            temp.add(aspect, 1);
            AspectList temp2 = AspectHelper.reduceToPrimals(temp);
            for (Aspect a : temp2.getAspects()) {
                for (int b = 0; b < temp2.getAmount(a); ++b) {
                    list.add(a);
                }
            }
        }
        return list.size() > 0 ? (Aspect)list.get(rand.nextInt(list.size())) : null;
    }

    public static AspectList reduceToPrimals(AspectList in) {
        AspectList out = new AspectList();
        for (Aspect aspect : in.getAspects()) {
            if (aspect == null) continue;
            if (aspect.isPrimal()) {
                out.add(aspect, in.getAmount(aspect));
                continue;
            }
            AspectList temp = new AspectList();
            temp.add(aspect.getComponents()[0], in.getAmount(aspect));
            temp.add(aspect.getComponents()[1], in.getAmount(aspect));
            AspectList temp2 = AspectHelper.reduceToPrimals(temp);
            for (Aspect a : temp2.getAspects()) {
                out.add(a, temp2.getAmount(a));
            }
        }
        return out;
    }

    public static AspectList getPrimalAspects(AspectList in) {
        AspectList t = new AspectList();
        t.add(Aspect.AIR, in.getAmount(Aspect.AIR));
        t.add(Aspect.FIRE, in.getAmount(Aspect.FIRE));
        t.add(Aspect.WATER, in.getAmount(Aspect.WATER));
        t.add(Aspect.EARTH, in.getAmount(Aspect.EARTH));
        t.add(Aspect.ORDER, in.getAmount(Aspect.ORDER));
        t.add(Aspect.ENTROPY, in.getAmount(Aspect.ENTROPY));
        return t;
    }

    public static AspectList getAuraAspects(AspectList in) {
        AspectList t = new AspectList();
        t.add(Aspect.AIR, in.getAmount(Aspect.AIR));
        t.add(Aspect.FIRE, in.getAmount(Aspect.FIRE));
        t.add(Aspect.WATER, in.getAmount(Aspect.WATER));
        t.add(Aspect.EARTH, in.getAmount(Aspect.EARTH));
        t.add(Aspect.ORDER, in.getAmount(Aspect.ORDER));
        t.add(Aspect.ENTROPY, in.getAmount(Aspect.ENTROPY));
        t.add(Aspect.FLUX, in.getAmount(Aspect.FLUX));
        return t;
    }
}

