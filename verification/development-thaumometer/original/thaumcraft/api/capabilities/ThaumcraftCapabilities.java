/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraftforge.common.capabilities.Capability
 *  net.minecraftforge.common.capabilities.CapabilityInject
 */
package thaumcraft.api.capabilities;

import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;

public class ThaumcraftCapabilities {
    @CapabilityInject(value=IPlayerKnowledge.class)
    public static final Capability<IPlayerKnowledge> KNOWLEDGE = null;
    @CapabilityInject(value=IPlayerWarp.class)
    public static final Capability<IPlayerWarp> WARP = null;

    public static IPlayerKnowledge getKnowledge(@Nonnull EntityPlayer player) {
        return (IPlayerKnowledge)player.getCapability(KNOWLEDGE, null);
    }

    public static boolean knowsResearch(@Nonnull EntityPlayer player, String ... research) {
        for (String r : research) {
            String[] rr;
            if (r.contains("&&")) {
                rr = r.split("&&");
                if (ThaumcraftCapabilities.knowsResearch(player, rr)) continue;
                return false;
            }
            if (r.contains("||")) {
                for (String str : rr = r.split("||")) {
                    if (!ThaumcraftCapabilities.knowsResearch(player, str)) continue;
                    return true;
                }
                continue;
            }
            if (ThaumcraftCapabilities.getKnowledge(player).isResearchKnown(r)) continue;
            return false;
        }
        return true;
    }

    public static boolean knowsResearchStrict(@Nonnull EntityPlayer player, String ... research) {
        for (String r : research) {
            String[] rr;
            if (r.contains("&&")) {
                rr = r.split("&&");
                if (ThaumcraftCapabilities.knowsResearchStrict(player, rr)) continue;
                return false;
            }
            if (r.contains("||")) {
                for (String str : rr = r.split("||")) {
                    if (!ThaumcraftCapabilities.knowsResearchStrict(player, str)) continue;
                    return true;
                }
                continue;
            }
            if (!(r.contains("@") ? !ThaumcraftCapabilities.getKnowledge(player).isResearchKnown(r) : !ThaumcraftCapabilities.getKnowledge(player).isResearchComplete(r))) continue;
            return false;
        }
        return true;
    }

    public static IPlayerWarp getWarp(@Nonnull EntityPlayer player) {
        return (IPlayerWarp)player.getCapability(WARP, null);
    }
}

