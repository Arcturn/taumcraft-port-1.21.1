/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  baubles.api.BaublesApi
 *  net.minecraft.advancements.CriteriaTriggers
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.EntityPlayerMP
 *  net.minecraft.init.Items
 *  net.minecraft.init.MobEffects
 *  net.minecraft.inventory.IInventory
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemFood
 *  net.minecraft.item.ItemStack
 *  net.minecraft.nbt.NBTBase
 *  net.minecraft.nbt.NBTTagCompound
 *  net.minecraft.nbt.NBTTagList
 *  net.minecraft.potion.PotionEffect
 *  net.minecraft.stats.StatList
 *  net.minecraft.util.DamageSource
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.TextComponentString
 *  net.minecraft.util.text.TextFormatting
 *  net.minecraft.util.text.translation.I18n
 *  net.minecraftforge.common.capabilities.ICapabilityProvider
 *  net.minecraftforge.event.AttachCapabilitiesEvent
 *  net.minecraftforge.event.entity.EntityJoinWorldEvent
 *  net.minecraftforge.event.entity.item.ItemTossEvent
 *  net.minecraftforge.event.entity.living.LivingDeathEvent
 *  net.minecraftforge.event.entity.living.LivingEntityUseItemEvent$Finish
 *  net.minecraftforge.event.entity.living.LivingEvent$LivingJumpEvent
 *  net.minecraftforge.event.entity.living.LivingEvent$LivingUpdateEvent
 *  net.minecraftforge.event.entity.living.LivingHurtEvent
 *  net.minecraftforge.event.entity.player.EntityItemPickupEvent
 *  net.minecraftforge.event.entity.player.ItemTooltipEvent
 *  net.minecraftforge.event.entity.player.PlayerEvent$Clone
 *  net.minecraftforge.event.entity.player.PlayerPickupXpEvent
 *  net.minecraftforge.event.entity.player.PlayerWakeUpEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.eventhandler.SubscribeEvent
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 */
package thaumcraft.common.lib.events;

import baubles.api.BaublesApi;
import java.util.HashMap;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.config.ConfigResearch;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.items.curios.ItemThaumonomicon;
import thaumcraft.common.items.resources.ItemCrystalEssence;
import thaumcraft.common.lib.capabilities.PlayerKnowledge;
import thaumcraft.common.lib.capabilities.PlayerWarp;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.lib.events.WarpEvents;
import thaumcraft.common.lib.potions.PotionDeathGaze;
import thaumcraft.common.lib.potions.PotionUnnaturalHunger;
import thaumcraft.common.lib.potions.PotionWarpWard;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.world.aura.AuraHandler;

@Mod.EventBusSubscriber
public class PlayerEvents {
    static HashMap<Integer, Long> nextCycle = new HashMap();
    static HashMap<Integer, Integer> lastCharge = new HashMap();
    static HashMap<Integer, Integer> lastMaxCharge = new HashMap();
    static HashMap<Integer, Integer> runicInfo = new HashMap();
    static HashMap<String, Long> upgradeCooldown = new HashMap();
    public static HashMap<Integer, Float> prevStep = new HashMap();

    @SubscribeEvent
    public static void onFallDamage(LivingHurtEvent event) {
        if (event.getSource() == DamageSource.field_76379_h && event.getEntityLiving() instanceof EntityPlayer) {
            float f;
            if (((ItemStack)((EntityPlayer)event.getEntityLiving()).field_71071_by.field_70460_b.get(0)).func_77973_b() == ItemsTC.travellerBoots) {
                f = Math.max(0.0f, event.getAmount() / 2.0f - 1.0f);
                if (f < 1.0f) {
                    event.setCanceled(true);
                    event.setAmount(0.0f);
                } else {
                    event.setAmount(f);
                }
            }
            if (BaublesApi.isBaubleEquipped((EntityPlayer)((EntityPlayer)event.getEntityLiving()), (Item)ItemsTC.ringCloud) >= 0) {
                f = Math.max(0.0f, event.getAmount() / 3.0f - 2.0f);
                if (f < 1.0f) {
                    event.setCanceled(true);
                    event.setAmount(0.0f);
                } else if (f < event.getAmount()) {
                    event.setAmount(f);
                }
                if (event.getAmount() < 1.0f) {
                    event.setCanceled(true);
                    event.setAmount(0.0f);
                }
            }
        }
    }

    @SubscribeEvent
    public static void livingTick(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)event.getEntity();
            PlayerEvents.handleMisc(player);
            PlayerEvents.handleSpeedMods(player);
            if (!player.field_70170_p.field_72995_K) {
                PlayerEvents.handleRunicArmor(player);
                PlayerEvents.handleWarp(player);
                if (player.field_70173_aa % 20 == 0 && ResearchManager.syncList.remove(player.func_70005_c_()) != null) {
                    IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
                    knowledge.sync((EntityPlayerMP)player);
                }
                if (player.field_70173_aa % 200 == 0) {
                    ConfigResearch.checkPeriodicStuff(player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void pickupItem(EntityItemPickupEvent event) {
        if (event.getEntityPlayer() != null && !event.getEntityPlayer().field_70170_p.field_72995_K && event.getItem() != null && event.getItem().func_92059_d() != null) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(event.getEntityPlayer());
            if (event.getItem().func_92059_d().func_77973_b() instanceof ItemCrystalEssence && !knowledge.isResearchKnown("!gotcrystals")) {
                knowledge.addResearch("!gotcrystals");
                knowledge.sync((EntityPlayerMP)event.getEntityPlayer());
                event.getEntityPlayer().func_145747_a((ITextComponent)new TextComponentString(TextFormatting.DARK_PURPLE + I18n.func_74838_a((String)"got.crystals")));
                if (ModConfig.CONFIG_MISC.noSleep && !knowledge.isResearchKnown("!gotdream")) {
                    PlayerEvents.giveDreamJournal(event.getEntityPlayer());
                }
            }
            if (event.getItem().func_92059_d().func_77973_b() instanceof ItemThaumonomicon && !knowledge.isResearchKnown("!gotthaumonomicon")) {
                knowledge.addResearch("!gotthaumonomicon");
                knowledge.sync((EntityPlayerMP)event.getEntityPlayer());
            }
        }
    }

    @SubscribeEvent
    public static void wakeUp(PlayerWakeUpEvent event) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(event.getEntityPlayer());
        if (event.getEntityPlayer() != null && !event.getEntityPlayer().field_70170_p.field_72995_K && knowledge.isResearchKnown("!gotcrystals") && !knowledge.isResearchKnown("!gotdream")) {
            PlayerEvents.giveDreamJournal(event.getEntityPlayer());
        }
    }

    private static void giveDreamJournal(EntityPlayer player) {
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        knowledge.addResearch("!gotdream");
        knowledge.sync((EntityPlayerMP)player);
        ItemStack book = ConfigItems.startBook.func_77946_l();
        book.func_77978_p().func_74778_a("author", player.func_70005_c_());
        if (!player.field_71071_by.func_70441_a(book)) {
            InventoryUtils.dropItemAtEntity(player.field_70170_p, book, (Entity)player);
        }
        try {
            player.func_145747_a((ITextComponent)new TextComponentString(TextFormatting.DARK_PURPLE + I18n.func_74838_a((String)"got.dream")));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static void handleMisc(EntityPlayer player) {
        if (player.field_70170_p.field_73011_w.getDimension() == ModConfig.CONFIG_WORLD.dimensionOuterId && player.field_70173_aa % 20 == 0 && !player.func_175149_v() && !player.field_71075_bZ.field_75098_d && player.field_71075_bZ.field_75100_b) {
            player.field_71075_bZ.field_75100_b = false;
            player.func_146105_b((ITextComponent)new TextComponentString(TextFormatting.ITALIC + "" + TextFormatting.GRAY + I18n.func_74838_a((String)"tc.break.fly")), true);
        }
    }

    @SideOnly(value=Side.CLIENT)
    @SubscribeEvent
    public static void tooltipEvent(ItemTooltipEvent event) {
        try {
            int al;
            int warp;
            int charge = PlayerEvents.getRunicCharge(event.getItemStack());
            if (charge > 0) {
                event.getToolTip().add(TextFormatting.GOLD + I18n.func_74838_a((String)"item.runic.charge") + " +" + charge);
            }
            if ((warp = PlayerEvents.getFinalWarp(event.getItemStack(), event.getEntityPlayer())) > 0) {
                event.getToolTip().add(TextFormatting.DARK_PURPLE + I18n.func_74838_a((String)"item.warping") + " " + warp);
            }
            if ((al = PlayerEvents.getFinalDiscount(event.getItemStack(), event.getEntityPlayer())) > 0) {
                event.getToolTip().add(TextFormatting.DARK_PURPLE + I18n.func_74838_a((String)"tc.visdiscount") + ": " + al + "%");
            }
            if (event.getItemStack() != null) {
                NBTTagList nbttaglist;
                AspectList aspects;
                int c;
                if (event.getItemStack().func_77973_b() instanceof IRechargable && (c = Math.round(RechargeHelper.getCharge(event.getItemStack()))) >= 0) {
                    event.getToolTip().add(TextFormatting.YELLOW + I18n.func_74838_a((String)"tc.charge") + " " + c);
                }
                if (event.getItemStack().func_77973_b() instanceof IEssentiaContainerItem && (aspects = ((IEssentiaContainerItem)event.getItemStack().func_77973_b()).getAspects(event.getItemStack())) != null && aspects.size() > 0) {
                    for (Aspect tag : aspects.getAspectsSortedByName()) {
                        event.getToolTip().add(tag.getName() + " x" + aspects.getAmount(tag));
                    }
                }
                if ((nbttaglist = EnumInfusionEnchantment.getInfusionEnchantmentTagList(event.getItemStack())) != null) {
                    for (int j = 0; j < nbttaglist.func_74745_c(); ++j) {
                        short k = nbttaglist.func_150305_b(j).func_74765_d("id");
                        short l = nbttaglist.func_150305_b(j).func_74765_d("lvl");
                        if (k < 0 || k >= EnumInfusionEnchantment.values().length) continue;
                        String s = TextFormatting.GOLD + I18n.func_74838_a((String)("enchantment.infusion." + EnumInfusionEnchantment.values()[k].toString()));
                        if (EnumInfusionEnchantment.values()[k].maxLevel > 1) {
                            s = s + " " + I18n.func_74838_a((String)("enchantment.level." + l));
                        }
                        event.getToolTip().add(1, s);
                    }
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static void handleRunicArmor(EntityPlayer player) {
        int charge;
        if (player.field_70173_aa % 20 == 0) {
            int max = 0;
            for (int a = 0; a < 4; ++a) {
                max += PlayerEvents.getRunicCharge((ItemStack)player.field_71071_by.field_70460_b.get(a));
            }
            IInventory baubles = BaublesApi.getBaubles((EntityPlayer)player);
            for (int a = 0; a < baubles.func_70302_i_(); ++a) {
                max += PlayerEvents.getRunicCharge(baubles.func_70301_a(a));
            }
            if (lastMaxCharge.containsKey(player.func_145782_y())) {
                charge = lastMaxCharge.get(player.func_145782_y());
                if (charge > max) {
                    player.func_110149_m(player.func_110139_bj() - (float)(charge - max));
                }
                if (max <= 0) {
                    lastMaxCharge.remove(player.func_145782_y());
                }
            }
            if (max > 0) {
                runicInfo.put(player.func_145782_y(), max);
                lastMaxCharge.put(player.func_145782_y(), max);
            } else {
                runicInfo.remove(player.func_145782_y());
            }
        }
        if (runicInfo.containsKey(player.func_145782_y())) {
            if (!nextCycle.containsKey(player.func_145782_y())) {
                nextCycle.put(player.func_145782_y(), 0L);
            }
            long time = System.currentTimeMillis();
            charge = (int)player.func_110139_bj();
            if (charge == 0 && lastCharge.containsKey(player.func_145782_y()) && lastCharge.get(player.func_145782_y()) > 0) {
                nextCycle.put(player.func_145782_y(), time + (long)ModConfig.CONFIG_MISC.shieldWait);
                lastCharge.put(player.func_145782_y(), 0);
            }
            if (charge < runicInfo.get(player.func_145782_y()) && nextCycle.get(player.func_145782_y()) < time && !AuraHandler.shouldPreserveAura(player.field_70170_p, player, player.func_180425_c())) {
                BlockPos blockPos = new BlockPos((Entity)player);
                if (AuraHelper.getVis(player.field_70170_p, blockPos) >= (float)ModConfig.CONFIG_MISC.shieldCost) {
                    AuraHandler.drainVis(player.field_70170_p, new BlockPos((Entity)player), ModConfig.CONFIG_MISC.shieldCost, false);
                    nextCycle.put(player.func_145782_y(), time + (long)ModConfig.CONFIG_MISC.shieldRecharge);
                    player.func_110149_m((float)(charge + 1));
                    lastCharge.put(player.func_145782_y(), charge + 1);
                }
            }
        }
    }

    public static int getRunicCharge(ItemStack stack) {
        int base = 0;
        if (stack.func_77942_o() && stack.func_77978_p().func_74764_b("TC.RUNIC")) {
            base += stack.func_77978_p().func_74771_c("TC.RUNIC");
        }
        return base;
    }

    public static int getFinalWarp(ItemStack stack, EntityPlayer player) {
        if (stack == null || stack.func_190926_b()) {
            return 0;
        }
        int warp = 0;
        if (stack.func_77973_b() instanceof IWarpingGear) {
            IWarpingGear armor = (IWarpingGear)stack.func_77973_b();
            warp += armor.getWarp(stack, player);
        }
        if (stack.func_77942_o() && stack.func_77978_p().func_74764_b("TC.WARP")) {
            warp += stack.func_77978_p().func_74771_c("TC.WARP");
        }
        return warp;
    }

    public static int getFinalDiscount(ItemStack stack, EntityPlayer player) {
        if (stack == null || stack.func_190926_b() || !(stack.func_77973_b() instanceof IVisDiscountGear)) {
            return 0;
        }
        IVisDiscountGear gear = (IVisDiscountGear)stack.func_77973_b();
        return gear.getVisDiscount(stack, player);
    }

    private static void handleSpeedMods(EntityPlayer player) {
        if (player.field_70170_p.field_72995_K && (player.func_70093_af() || ((ItemStack)player.field_71071_by.field_70460_b.get(0)).func_77973_b() != ItemsTC.travellerBoots) && prevStep.containsKey(player.func_145782_y())) {
            player.field_70138_W = prevStep.get(player.func_145782_y()).floatValue();
            prevStep.remove(player.func_145782_y());
        }
    }

    @SubscribeEvent
    public static void playerJumps(LivingEvent.LivingJumpEvent event) {
        ItemStack is;
        if (event.getEntity() instanceof EntityPlayer && ((ItemStack)((EntityPlayer)event.getEntity()).field_71071_by.field_70460_b.get(0)).func_77973_b() == ItemsTC.travellerBoots && RechargeHelper.getCharge(is = (ItemStack)((EntityPlayer)event.getEntity()).field_71071_by.field_70460_b.get(0)) > 0) {
            event.getEntityLiving().field_70181_x += (double)0.275f;
        }
    }

    private static void handleWarp(EntityPlayer player) {
        if (!ModConfig.CONFIG_MISC.wussMode && player.field_70173_aa > 0 && player.field_70173_aa % 2000 == 0 && !player.func_70644_a(PotionWarpWard.instance)) {
            WarpEvents.checkWarpEvent(player);
        }
        if (player.field_70173_aa % 20 == 0 && player.func_70644_a(PotionDeathGaze.instance)) {
            WarpEvents.checkDeathGaze(player);
        }
    }

    @SubscribeEvent
    public static void droppedItem(ItemTossEvent event) {
        NBTTagCompound itemData = event.getEntityItem().getEntityData();
        itemData.func_74778_a("thrower", event.getPlayer().func_70005_c_());
    }

    @SubscribeEvent
    public static void finishedUsingItem(LivingEntityUseItemEvent.Finish event) {
        if (!event.getEntity().field_70170_p.field_72995_K && event.getEntityLiving().func_70644_a(PotionUnnaturalHunger.instance)) {
            if (event.getItem().func_77969_a(new ItemStack(Items.field_151078_bh)) || event.getItem().func_77969_a(new ItemStack(ItemsTC.brain))) {
                PotionEffect pe = event.getEntityLiving().func_70660_b(PotionUnnaturalHunger.instance);
                int amp = pe.func_76458_c() - 1;
                int duration = pe.func_76459_b() - 600;
                event.getEntityLiving().func_184589_d(PotionUnnaturalHunger.instance);
                if (duration > 0 && amp >= 0) {
                    pe = new PotionEffect(PotionUnnaturalHunger.instance, duration, amp, true, false);
                    pe.getCurativeItems().clear();
                    pe.addCurativeItem(new ItemStack(Items.field_151078_bh));
                    event.getEntityLiving().func_70690_d(pe);
                }
                event.getEntityLiving().func_145747_a((ITextComponent)new TextComponentString("\u00a72\u00a7o" + I18n.func_74838_a((String)"warp.text.hunger.2")));
            } else if (event.getItem().func_77973_b() instanceof ItemFood) {
                event.getEntityLiving().func_145747_a((ITextComponent)new TextComponentString("\u00a74\u00a7o" + I18n.func_74838_a((String)"warp.text.hunger.1")));
            }
        }
    }

    @SubscribeEvent
    public static void attachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(PlayerKnowledge.Provider.NAME, (ICapabilityProvider)new PlayerKnowledge.Provider());
            event.addCapability(PlayerWarp.Provider.NAME, (ICapabilityProvider)new PlayerWarp.Provider());
        }
    }

    @SubscribeEvent
    public static void playerJoin(EntityJoinWorldEvent event) {
        if (!event.getWorld().field_72995_K && event.getEntity() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP)event.getEntity();
            IPlayerKnowledge pk = ThaumcraftCapabilities.getKnowledge((EntityPlayer)player);
            IPlayerWarp pw = ThaumcraftCapabilities.getWarp((EntityPlayer)player);
            if (pk != null) {
                pk.sync(player);
            }
            if (pw != null) {
                pw.sync(player);
            }
        }
    }

    @SubscribeEvent
    public static void cloneCapabilitiesEvent(PlayerEvent.Clone event) {
        try {
            NBTTagCompound nbtKnowledge = (NBTTagCompound)ThaumcraftCapabilities.getKnowledge(event.getOriginal()).serializeNBT();
            ThaumcraftCapabilities.getKnowledge(event.getEntityPlayer()).deserializeNBT((NBTBase)nbtKnowledge);
            NBTTagCompound nbtWarp = (NBTTagCompound)ThaumcraftCapabilities.getWarp(event.getOriginal()).serializeNBT();
            ThaumcraftCapabilities.getWarp(event.getEntityPlayer()).deserializeNBT((NBTBase)nbtWarp);
        }
        catch (Exception e) {
            Thaumcraft.log.error("Could not clone player [" + event.getOriginal().func_70005_c_() + "] knowledge when changing dimensions");
        }
    }

    @SubscribeEvent
    public static void pickupXP(PlayerPickupXpEvent event) {
        if (event.getEntityPlayer() != null && !event.getEntityPlayer().field_70170_p.field_72995_K && BaublesApi.isBaubleEquipped((EntityPlayer)event.getEntityPlayer(), (Item)ItemsTC.bandCuriosity) >= 0 && event.getOrb().func_70526_d() > 1) {
            int d = event.getOrb().field_70530_e / 2;
            event.getOrb().field_70530_e -= d;
            float r = event.getEntityPlayer().func_70681_au().nextFloat();
            if ((double)r < 0.05 * (double)d) {
                String[] s = ResearchCategories.researchCategories.keySet().toArray(new String[0]);
                String cat = s[event.getEntityPlayer().func_70681_au().nextInt(s.length)];
                ThaumcraftApi.internalMethods.addKnowledge(event.getEntityPlayer(), IPlayerKnowledge.EnumKnowledgeType.THEORY, ResearchCategories.getResearchCategory(cat), 1);
            } else if ((double)r < 0.2 * (double)d) {
                String[] s = ResearchCategories.researchCategories.keySet().toArray(new String[0]);
                String cat = s[event.getEntityPlayer().func_70681_au().nextInt(s.length)];
                ThaumcraftApi.internalMethods.addKnowledge(event.getEntityPlayer(), IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory(cat), 1);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        EntityPlayer player;
        int slot;
        if (event.getEntityLiving() instanceof EntityPlayer && (slot = BaublesApi.isBaubleEquipped((EntityPlayer)(player = (EntityPlayer)event.getEntityLiving()), (Item)ItemsTC.charmUndying)) >= 0) {
            if (player instanceof EntityPlayerMP) {
                EntityPlayerMP entityplayermp = (EntityPlayerMP)player;
                entityplayermp.func_71029_a(StatList.func_188057_b((Item)Items.field_190929_cY));
                CriteriaTriggers.field_193130_A.func_193187_a(entityplayermp, BaublesApi.getBaubles((EntityPlayer)player).func_70301_a(slot));
            }
            BaublesApi.getBaublesHandler((EntityPlayer)player).extractItem(slot, 1, false);
            player.func_70606_j(1.0f);
            player.func_70674_bp();
            player.func_70690_d(new PotionEffect(MobEffects.field_76428_l, 900, 1));
            player.func_70690_d(new PotionEffect(MobEffects.field_76444_x, 100, 1));
            player.field_70170_p.func_72960_a((Entity)player, (byte)35);
            event.setCanceled(true);
        }
    }
}

