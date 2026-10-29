/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.util.EnumFacing
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.math.MathHelper
 *  net.minecraftforge.common.capabilities.Capability
 *  net.minecraftforge.common.capabilities.Capability$IStorage
 *  net.minecraftforge.common.capabilities.CapabilityManager
 *  net.minecraftforge.common.capabilities.ICapabilitySerializable
 *  net.minecraftforge.fml.common.network.simpleimpl.IMessage
 */
package thaumcraft.common.lib.capabilities;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketSyncKnowledge;

public class PlayerKnowledge {
    public static void preInit() {
        CapabilityManager.INSTANCE.register(IPlayerKnowledge.class, (Capability.IStorage)new Capability.IStorage<IPlayerKnowledge>(){

            public NBTTagCompound writeNBT(Capability<IPlayerKnowledge> capability, IPlayerKnowledge instance, EnumFacing side) {
                return (NBTTagCompound)instance.serializeNBT();
            }

            public void readNBT(Capability<IPlayerKnowledge> capability, IPlayerKnowledge instance, EnumFacing side, NBTBase nbt) {
                if (nbt instanceof NBTTagCompound) {
                    instance.deserializeNBT((NBTBase)((NBTTagCompound)nbt));
                }
            }
        }, () -> new DefaultImpl());
    }

    public static class Provider
    implements ICapabilitySerializable<NBTTagCompound> {
        public static final ResourceLocation NAME = new ResourceLocation("thaumcraft", "knowledge");
        private final DefaultImpl knowledge = new DefaultImpl();

        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
            return capability == ThaumcraftCapabilities.KNOWLEDGE;
        }

        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            if (capability == ThaumcraftCapabilities.KNOWLEDGE) {
                return (T)ThaumcraftCapabilities.KNOWLEDGE.cast((Object)this.knowledge);
            }
            return null;
        }

        public NBTTagCompound serializeNBT() {
            return this.knowledge.serializeNBT();
        }

        public void deserializeNBT(NBTTagCompound nbt) {
            this.knowledge.deserializeNBT(nbt);
        }
    }

    private static class DefaultImpl
    implements IPlayerKnowledge {
        private final HashSet<String> research = new HashSet();
        private final Map<String, Integer> stages = new HashMap<String, Integer>();
        private final Map<String, HashSet<IPlayerKnowledge.EnumResearchFlag>> flags = new HashMap<String, HashSet<IPlayerKnowledge.EnumResearchFlag>>();
        private final Map<String, Integer> knowledge = new HashMap<String, Integer>();

        private DefaultImpl() {
        }

        @Override
        public void clear() {
            this.research.clear();
            this.flags.clear();
            this.stages.clear();
            this.knowledge.clear();
        }

        @Override
        public IPlayerKnowledge.EnumResearchStatus getResearchStatus(@Nonnull String res) {
            if (!this.isResearchKnown(res)) {
                return IPlayerKnowledge.EnumResearchStatus.UNKNOWN;
            }
            ResearchEntry entry = ResearchCategories.getResearch(res);
            if (entry == null || entry.getStages() == null || this.getResearchStage(res) > entry.getStages().length) {
                return IPlayerKnowledge.EnumResearchStatus.COMPLETE;
            }
            return IPlayerKnowledge.EnumResearchStatus.IN_PROGRESS;
        }

        @Override
        public boolean isResearchKnown(String res) {
            if (res == null) {
                return false;
            }
            if (res.equals("")) {
                return true;
            }
            String[] ss = res.split("@");
            if (ss.length > 1 && this.getResearchStage(ss[0]) < MathHelper.func_82715_a((String)ss[1], (int)0)) {
                return false;
            }
            return this.research.contains(ss[0]);
        }

        @Override
        public boolean isResearchComplete(String res) {
            return this.getResearchStatus(res) == IPlayerKnowledge.EnumResearchStatus.COMPLETE;
        }

        @Override
        public int getResearchStage(String res) {
            if (res == null || !this.research.contains(res)) {
                return -1;
            }
            Integer stage = this.stages.get(res);
            return stage == null ? 0 : stage;
        }

        @Override
        public boolean setResearchStage(String res, int stage) {
            if (res == null || !this.research.contains(res) || stage <= 0) {
                return false;
            }
            this.stages.put(res, stage);
            return true;
        }

        @Override
        public boolean addResearch(@Nonnull String res) {
            if (!this.isResearchKnown(res)) {
                this.research.add(res);
                return true;
            }
            return false;
        }

        @Override
        public boolean removeResearch(@Nonnull String res) {
            if (this.isResearchKnown(res)) {
                this.research.remove(res);
                return true;
            }
            return false;
        }

        @Override
        @Nonnull
        public Set<String> getResearchList() {
            return Collections.unmodifiableSet(this.research);
        }

        @Override
        public boolean setResearchFlag(@Nonnull String res, @Nonnull IPlayerKnowledge.EnumResearchFlag flag) {
            HashSet<IPlayerKnowledge.EnumResearchFlag> list = this.flags.get(res);
            if (list == null) {
                list = new HashSet();
                this.flags.put(res, list);
            }
            if (list.contains((Object)flag)) {
                return false;
            }
            list.add(flag);
            return true;
        }

        @Override
        public boolean clearResearchFlag(@Nonnull String res, @Nonnull IPlayerKnowledge.EnumResearchFlag flag) {
            HashSet<IPlayerKnowledge.EnumResearchFlag> list = this.flags.get(res);
            if (list != null) {
                boolean b = list.remove((Object)flag);
                if (list.isEmpty()) {
                    this.flags.remove(this.research);
                }
                return b;
            }
            return false;
        }

        @Override
        public boolean hasResearchFlag(@Nonnull String res, @Nonnull IPlayerKnowledge.EnumResearchFlag flag) {
            if (this.flags.get(res) != null) {
                return this.flags.get(res).contains((Object)flag);
            }
            return false;
        }

        private String getKey(IPlayerKnowledge.EnumKnowledgeType type, ResearchCategory category) {
            return type.getAbbreviation() + "_" + (category == null ? "" : category.key);
        }

        @Override
        public boolean addKnowledge(IPlayerKnowledge.EnumKnowledgeType type, ResearchCategory category, int amount) {
            String key = this.getKey(type, category);
            int c = this.getKnowledgeRaw(type, category);
            if (c + amount < 0) {
                return false;
            }
            this.knowledge.put(key, c += amount);
            return true;
        }

        @Override
        public int getKnowledge(IPlayerKnowledge.EnumKnowledgeType type, ResearchCategory category) {
            String key = this.getKey(type, category);
            int c = !this.knowledge.containsKey(key) ? 0 : this.knowledge.get(key);
            return (int)Math.floor((double)c / (double)type.getProgression());
        }

        @Override
        public int getKnowledgeRaw(IPlayerKnowledge.EnumKnowledgeType type, ResearchCategory category) {
            String key = this.getKey(type, category);
            return !this.knowledge.containsKey(key) ? 0 : this.knowledge.get(key);
        }

        @Override
        public void sync(@Nonnull EntityPlayerMP player) {
            PacketHandler.INSTANCE.sendTo((IMessage)new PacketSyncKnowledge((EntityPlayer)player), player);
        }

        public NBTTagCompound serializeNBT() {
            NBTTagCompound rootTag = new NBTTagCompound();
            NBTTagList researchList = new NBTTagList();
            for (String resKey : this.research) {
                HashSet<IPlayerKnowledge.EnumResearchFlag> list;
                NBTTagCompound tag = new NBTTagCompound();
                tag.func_74778_a("key", resKey);
                if (this.stages.containsKey(resKey)) {
                    tag.func_74768_a("stage", this.stages.get(resKey).intValue());
                }
                if (this.flags.containsKey(resKey) && (list = this.flags.get(resKey)) != null) {
                    String fs = "";
                    for (IPlayerKnowledge.EnumResearchFlag flag : list) {
                        if (fs.length() > 0) {
                            fs = fs + ",";
                        }
                        fs = fs + flag.name();
                    }
                    tag.func_74778_a("flags", fs);
                }
                researchList.func_74742_a((NBTBase)tag);
            }
            rootTag.func_74782_a("research", (NBTBase)researchList);
            NBTTagList knowledgeList = new NBTTagList();
            for (String key : this.knowledge.keySet()) {
                Integer c = this.knowledge.get(key);
                if (c == null || c <= 0 || key == null || key.isEmpty()) continue;
                NBTTagCompound tag = new NBTTagCompound();
                tag.func_74778_a("key", key);
                tag.func_74768_a("amount", c.intValue());
                knowledgeList.func_74742_a((NBTBase)tag);
            }
            rootTag.func_74782_a("knowledge", (NBTBase)knowledgeList);
            return rootTag;
        }

        public void deserializeNBT(NBTTagCompound rootTag) {
            if (rootTag == null) {
                return;
            }
            this.clear();
            NBTTagList researchList = rootTag.func_150295_c("research", 10);
            for (int i = 0; i < researchList.func_74745_c(); ++i) {
                String[] ss;
                String fs;
                NBTTagCompound tag = researchList.func_150305_b(i);
                String know = tag.func_74779_i("key");
                if (know == null || this.isResearchKnown(know)) continue;
                this.research.add(know);
                int stage = tag.func_74762_e("stage");
                if (stage > 0) {
                    this.stages.put(know, stage);
                }
                if ((fs = tag.func_74779_i("flags")).length() <= 0) continue;
                for (String s : ss = fs.split(",")) {
                    IPlayerKnowledge.EnumResearchFlag flag = null;
                    try {
                        flag = IPlayerKnowledge.EnumResearchFlag.valueOf(s);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                    if (flag == null) continue;
                    this.setResearchFlag(know, flag);
                }
            }
            NBTTagList knowledgeList = rootTag.func_150295_c("knowledge", 10);
            for (int i = 0; i < knowledgeList.func_74745_c(); ++i) {
                NBTTagCompound tag = knowledgeList.func_150305_b(i);
                String key = tag.func_74779_i("key");
                int amount = tag.func_74762_e("amount");
                this.knowledge.put(key, amount);
            }
            this.addAutoUnlockResearch();
        }

        private void addAutoUnlockResearch() {
            for (ResearchCategory cat : ResearchCategories.researchCategories.values()) {
                for (ResearchEntry ri : cat.research.values()) {
                    if (!ri.hasMeta(ResearchEntry.EnumResearchMeta.AUTOUNLOCK)) continue;
                    this.addResearch(ri.getKey());
                }
            }
        }
    }
}

